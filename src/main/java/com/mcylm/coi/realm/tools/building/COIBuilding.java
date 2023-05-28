package com.mcylm.coi.realm.tools.building;

import com.mcylm.coi.realm.Entry;
import com.mcylm.coi.realm.enums.COIBuildingType;
import com.mcylm.coi.realm.model.COIBlock;
import com.mcylm.coi.realm.model.COINpc;
import com.mcylm.coi.realm.model.COIPaster;
import com.mcylm.coi.realm.model.COIStructure;
import com.mcylm.coi.realm.tools.team.impl.COITeam;
import com.mcylm.coi.realm.utils.ItemUtils;
import com.mcylm.coi.realm.utils.LocationUtils;
import com.mcylm.coi.realm.utils.LoggerUtils;
import com.mcylm.coi.realm.utils.TeamUtils;
import com.mcylm.coi.realm.utils.rotation.Rotation;
import lombok.Getter;
import lombok.Setter;
import me.filoghost.holographicdisplays.api.HolographicDisplaysAPI;
import me.filoghost.holographicdisplays.api.hologram.Hologram;
import me.filoghost.holographicdisplays.api.hologram.VisibilitySettings;
import me.filoghost.holographicdisplays.api.hologram.line.TextHologramLine;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 建筑物结构
 */
@Setter
@Getter
public class COIBuilding implements Serializable {

    // 是否可建造
    private boolean available = false;

    // 是否建造完成
    private boolean complete = false;

    // 是否"活"着
    private boolean alive = true;

    // 建筑类型
    private COIBuildingType type;

    // 所需消耗的材料
    private int consume = 0;

    // 建筑的全部方块
    private List<COIBlock> remainingBlocks;

    // 地图中建筑的所有方块
    private Set<Block> blocks = new HashSet<>();

    // 建筑所替换的原方块数据
    private Map<Location, BlockData> originalBlockData = new ConcurrentHashMap<>();
    private Map<Location, Material> originalBlocks = new ConcurrentHashMap<>();

    // 放置物品的箱子位置
    private List<Location> chestsLocation = new ArrayList<>();

    // 所在世界名称
    private String world;

    // 建筑基点
    private Location location;

    // 总方块数量
    private Integer totalBlocks;

    // 建筑等级
    private int level = 1;

    // 最高等级
    private Integer maxLevel = 1;

    // 建筑等级对照建筑结构表
    // key为等级，value是建筑结构文件名称
    private HashMap<Integer, String> buildingLevelStructure = new HashMap<>();

    // 建筑生成的NPC创建器，不生成NPC就设置NULL
    private List<COINpc> npcCreators = new ArrayList<>();

    // 建筑所属的队伍
    private COITeam team;

    // 建筑血量
    private AtomicInteger health = new AtomicInteger(getMaxHealth());

    // 悬浮字相关
    private Map<Player, Hologram> holograms = new HashMap<>();
    private Map<Player, AtomicInteger> hologramVisitors = new HashMap<>();

    private static String getHealthBarText(double max, double current, int length) {
        double percent = current / max;
        StringBuilder text = new StringBuilder("§a建筑血量: ");
        int healthLength = Math.toIntExact(Math.round(length * percent));
        text.append("§e|".repeat(Math.max(0, healthLength)));
        text.append("§7|".repeat(Math.max(0, length - healthLength)));
        return text.toString();
    }

    /**
     * 首次建造建筑
     */
    public void build(Location location, Player player) {

        if (!isAvailable()) {
            return;
        }

        // 扣除玩家背包里的资源
        boolean b = deductionResources(player);

        if (!b) {
            LoggerUtils.sendMessage("背包里的资源不够，请去收集资源", player);
            return;
        }

        // 建筑开始就记录位置
        setLocation(location.clone());
        setWorld(location.getWorld().getName());

        String structureName = getStructureByLevel();

        if (structureName == null) {
            return;
        }
        // 实例化建筑结构
        COIStructure structure = Entry.getBuilder().getStructureByFile(structureName);


        // 设置名称
        structure.setName(getType().getName());

        structure = prepareStructure(structure, location.clone());

        // 预先计算建筑的方块位置，及总方块数量
        List<COIBlock> allBlocks = getAllBlocksByStructure(structure);
        setRemainingBlocks(allBlocks);
        setTotalBlocks(allBlocks.size());

        // 设置NPC所属小队
        getNpcCreators().forEach(npcCreator -> {
            npcCreator.setTeam(TeamUtils.getTeamByPlayer(player));
            npcCreator.setBuilding(this);
        });

        COIBuilding building = this;
        // 构造一个建造器
        COIPaster coiPaster = new COIPaster(false, getType().getUnit(), getType().getInterval()
                , location.getWorld().getName(), location
                , structure, false, TeamUtils.getTeamByPlayer(player).getType().getBlockColor()
                , getNpcCreators(), ((block, blockToPlace, type) -> {
            blocks.add(block);
            // block.setMetadata("building", new BuildData(building));
            if (ItemUtils.SUITABLE_CONTAINER_TYPES.contains(type)) {
                chestsLocation.add(block.getLocation());
            }
            originalBlockData.put(block.getLocation(), block.getBlockData().clone());
            originalBlocks.put(block.getLocation(), block.getType());
            return type;
        }));

        // 开始建造
        Entry.getBuilder().pasteStructure(coiPaster, player, building);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (coiPaster.isComplete()) {
                    // 监听建造状态
                    complete = coiPaster.isComplete();
                    Bukkit.getScheduler().runTask(Entry.getInstance(), () -> {
                        buildSuccess(location, player);
                    });
                    this.cancel();

                }
            }
        }.runTaskTimerAsynchronously(Entry.getInstance(), 0L, 20L);
    }

    public void buildSuccess(Location location, Player player) {
        // 建筑成功可以放个烟花
    }

    public void upgradeBuild(Player player) {

        for (Block b : getBlocks()) {
            b.removeMetadata("building", Entry.getInstance());

        }
        Set<Map.Entry<Location, Material>> blocks = getOriginalBlocks().entrySet();
        Set<Map.Entry<Location, BlockData>> blockData = getOriginalBlockData().entrySet();
        for (Map.Entry<Location, Material> entry : blocks) {
            Block block = entry.getKey().getBlock();
            if (block.getState() instanceof Container container) {
                for (ItemStack item : container.getInventory().getContents()) {
                    if (item != null) block.getWorld().dropItemNaturally(block.getLocation(), item);
                }
            }
            block.setType(entry.getValue());
        }
        for (Map.Entry<Location, BlockData> entry : blockData) {
            entry.getKey().getBlock().setBlockData(entry.getValue());
        }

        blocks.clear();
        originalBlocks.clear();
        originalBlockData.clear();
        remainingBlocks.clear();
        chestsLocation.clear();

        String structureName = getStructureByLevel();

        if (structureName == null) {
            return;
        }
        // 实例化建筑结构
        COIStructure structure = Entry.getBuilder().getStructureByFile(structureName);


        // 设置名称
        structure.setName(getType().getName());

        structure = prepareStructure(structure, location.clone());

        // 预先计算建筑的方块位置，及总方块数量
        List<COIBlock> allBlocks = getAllBlocksByStructure(structure);
        setRemainingBlocks(allBlocks);
        setTotalBlocks(allBlocks.size());

        COIBuilding building = this;
        // 构造一个建造器
        COIPaster coiPaster = new COIPaster(false, getType().getUnit(), getType().getInterval()
                , location.getWorld().getName(), location
                , structure, false, getTeam().getType().getBlockColor()
                , npcCreators, ((block, blockToPlace, type) -> {
            getBlocks().add(block);
            // block.setMetadata("building", new BuildData(building));
            if (ItemUtils.SUITABLE_CONTAINER_TYPES.contains(type)) {
                chestsLocation.add(block.getLocation());
            }
            originalBlockData.put(block.getLocation(), block.getBlockData().clone());
            originalBlocks.put(block.getLocation(), block.getType());
            return type;
        }));

        // 开始建造
        Entry.getBuilder().pasteStructure(coiPaster, player, building);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (coiPaster.isComplete()) {
                    // 监听建造状态
                    complete = true;
                    Bukkit.getScheduler().runTask(Entry.getInstance(), () -> {
                        upgradeBuildSuccess();
                    });
                    this.cancel();

                }
            }
        }.runTaskTimerAsynchronously(Entry.getInstance(), 0, 20L);

    }

    public void upgradeBuildSuccess() {

        getNpcCreators().forEach(COINpc::upgrade);
        getHealth().set(getMaxHealth());
    }

    /**
     * 通过等级获取建筑文件名称
     *
     * @return
     */
    public String getStructureByLevel() {
        return getBuildingLevelStructure().get(getLevel());
    }

    /**
     * 通过建筑结构文件获取所有方块
     *
     * @param structure
     * @return
     */
    public List<COIBlock> getAllBlocksByStructure(COIStructure structure) {
        // 全部待建造的方块
        List<COIBlock> allBlocks = structure.getBlocks();

        // 建筑基点
        Location basicLocation = getLocation();

        List<COIBlock> needBuildBlocks = new ArrayList<>();

        // 根据建筑基点设置每个方块的真实坐标
        for (COIBlock coiBlock : allBlocks) {

            COIBlock newBlock = new COIBlock();
            newBlock.setX(coiBlock.getX() + basicLocation.getBlockX());
            newBlock.setY(coiBlock.getY() + basicLocation.getBlockY());
            newBlock.setZ(coiBlock.getZ() + basicLocation.getBlockZ());
            newBlock.setBlockData(coiBlock.getBlockData());
            newBlock.setMaterial(coiBlock.getMaterial());

            if ("AIR".equals(newBlock.getMaterial())) {
                //删除掉空气方块
            } else
                needBuildBlocks.add(newBlock);
        }

        return needBuildBlocks;
    }

    // 找到箱子的位置
    private List<Location> getChestsLocation(List<COIBlock> blocks) {

        List<Location> chestsLocations = new ArrayList<>();
        for (COIBlock block : blocks) {

            Material material = Material.getMaterial(block.getMaterial());

            if (material != null) {
                if (material.equals(Material.CHEST)) {
                    Location location = new Location(Bukkit.getWorld(getWorld()), block.getX(), block.getY(), block.getZ());
                    chestsLocations.add(location);
                }
            }


        }

        return chestsLocations;
    }

    /**
     * 根据建筑所需资源，扣除玩家背包的物品
     *
     * @param player
     * @return
     */
    public boolean deductionResources(Player player) {
        return deductionResources(player, getConsume());

    }

    public boolean deductionResources(Player player, int amount) {
        int playerHadResource = getPlayerHadResource(player);

        // 如果玩家手里的资源数量足够
        if (playerHadResource >= amount) {

            // 扣减物品
            ItemStack[] contents =
                    player.getInventory().getContents();

            // 剩余所需扣减资源数量
            int deductionCount = amount;

            // 资源类型
            Material material = getResourceType();
            for (ItemStack itemStack : contents) {

                if (itemStack == null) {
                    continue;
                }

                // 是资源物品才扣减
                if (itemStack.getType().equals(material)) {
                    // 如果当前物品的堆叠数量大于所需资源，就只扣减数量
                    if (itemStack.getAmount() > deductionCount) {
                        itemStack.setAmount(itemStack.getAmount() - deductionCount);
                        return true;
                    }

                    // 如果当前物品的堆叠数量等于所需资源，就删物品
                    if (itemStack.getAmount() == deductionCount) {
                        player.getInventory().removeItem(itemStack);
                        player.updateInventory();
                        return true;
                    }

                    // 如果物品的堆叠数量小于所需资源，就删物品，同时计数
                    if (itemStack.getAmount() < deductionCount) {
                        // 减去当前物品的库存
                        deductionCount = deductionCount - itemStack.getAmount();
                        player.getInventory().removeItem(itemStack);
                        player.updateInventory();
                    }
                }


            }

        } else
            return false;

        return false;
    }

    /**
     * 获取玩家背包里的资源
     *
     * @return
     */
    public int getPlayerHadResource(Player player) {

        @NonNull ItemStack[] contents =
                player.getInventory().getContents();

        Material material = getResourceType();
        if (material == null) {
            return 0;
        }

        int num = 0;

        for (ItemStack itemStack : contents) {

            if (itemStack == null) {
                continue;
            }

            if (itemStack.getType().equals(material)) {
                num = num + itemStack.getAmount();
            }
        }

        return num;

    }

    public Material getResourceType() {
        String materialName = Entry.getInstance().getConfig().getString("game.building.material");

        return Material.getMaterial(materialName);

    }

    public int getMaxHealth() {
        return 100;
    }

    public void damage(Entity attacker, int damage, Block attackBlock) {
        if (!isComplete()) {
            return;
        }
        if (damage >= getHealth().get()) {
            getHealth().set(0);
            destroy(true);
        } else {
            getHealth().addAndGet(-damage);
        }
        for (Entity e : location.getNearbyEntities(30, 20, 20)) {
            if (e instanceof Player p) {
                displayHealth(p);
            }
        }
    }

    public COIStructure prepareStructure(COIStructure structure, Location loc) {
        loc.setYaw(loc.getYaw() + 90);
        structure.rotate(Rotation.fromDegrees(Math.round(loc.getYaw() / 90) * 90));
        return structure;
    }

    public void displayHealth(Player p) {
        if (hologramVisitors.containsKey(p)) {
            hologramVisitors.get(p).set(5);
        } else {
            hologramVisitors.put(p, new AtomicInteger(5));
        }
        if (!holograms.containsKey(p)) {
            Hologram hologram = HolographicDisplaysAPI.get(Entry.getInstance()).createHologram(location);
            hologram.getVisibilitySettings().setGlobalVisibility(VisibilitySettings.Visibility.HIDDEN);
            hologram.getVisibilitySettings().setIndividualVisibility(p, VisibilitySettings.Visibility.VISIBLE);
            hologram.getLines().appendText(String.format(LoggerUtils.replaceColor(team.getType().getColor() + "%s Lv. %s"), type.getName(), getLevel()));
            @NotNull TextHologramLine line = hologram.getLines().appendText(getHealthBarText(getMaxHealth(), getHealth().get(), getHealthBarLength()));

            holograms.put(p, hologram);
            new BukkitRunnable() {
                int tick = 0;

                @Override
                public void run() {
                    if (!holograms.containsKey(p)) {
                        Entry.runSync(hologram::delete);
                        this.cancel();
                    } else {
                        if (tick++ == 20) {
                            tick = 0;
                            if (hologramVisitors.get(p).decrementAndGet() == 0) {
                                holograms.remove(p);
                                hologramVisitors.remove(p);
                            }

                        }
                        int maxDistance = 12;
                        List<Location> loc = LocationUtils.line(getHologramPoint(), p.getEyeLocation(), 1);
                        int distance = loc.size();
                        if (maxDistance < distance) {
                            distance = maxDistance;
                        }
                        int finalDistance = loc.size() >= 3 ? distance - 3 : loc.size() ;

                        Entry.runSync(() -> {
                            if (hologram.isDeleted()) return;
                            line.setText(getHealthBarText(getMaxHealth(), getHealth().get(), getHealthBarLength()));
                            hologram.setPosition(loc.get(finalDistance).add(0,0.5,0));
                        });
                    }
                }
            }.runTaskTimerAsynchronously(Entry.getInstance(), 1, 1);

        }
    }

    public int getUpgradeRequiredConsume() {
        return consume + level * 80;
    }

    public int getDestroyReturn() {
        return Math.toIntExact(Math.round(consume + (level - 1) * 80 * 0.8));
    }

    public void destroy(boolean effect) {
        if (!isComplete()) {
            return;
        }
        for (Hologram value : holograms.values()) {
            value.delete();
        }
        holograms.clear();

        Set<Map.Entry<Location, Material>> blocks = getOriginalBlocks().entrySet();
        Set<Map.Entry<Location, BlockData>> blockData = getOriginalBlockData().entrySet();
        for (Block b : getBlocks()) {
            b.removeMetadata("building", Entry.getInstance());
            if (Math.random() > 0.8 && effect) {
                b.getWorld().spawnParticle(Particle.EXPLOSION_NORMAL, b.getLocation(), 1);
            }
            FallingBlock fallingBlock = b.getWorld().spawnFallingBlock(b.getLocation().add(0.5, 0.5, 0.5), b.getBlockData());
            fallingBlock.setDropItem(false);
            fallingBlock.setHurtEntities(false);
            fallingBlock.setMetadata("break_falling_block", new FixedMetadataValue(Entry.getInstance(), "fake_block"));
        }
        for (Map.Entry<Location, Material> entry : blocks) {
            Block block = entry.getKey().getBlock();
            if (block.getState() instanceof Container container) {
                for (ItemStack item : container.getInventory().getContents()) {
                    if (item != null) block.getWorld().dropItemNaturally(block.getLocation(), item);
                }
            }
            block.setType(entry.getValue());
        }

        for (Map.Entry<Location, BlockData> entry : blockData) {
            entry.getKey().getBlock().setBlockData(entry.getValue());
        }
        complete = false;
        team.getFinishedBuildings().remove(this);
        npcCreators.forEach(COINpc::remove);

        setAlive(false);
        team.getFoodChests().removeAll(getChestsLocation());
    }

    public Location getHologramPoint() {
        return getLocation();
    }

    protected int getHealthBarLength() {
        return 20;
    }

    public void upgrade(Player player) {
        if (level + 1 > maxLevel || !isComplete()) {
            return;
        }
        if (getPlayerHadResource(player) >= getUpgradeRequiredConsume()) {
            deductionResources(player, getUpgradeRequiredConsume());
            level++;
            upgradeBuild(player);
        }
    }

    public Block getNearestBlock(Location location) {
        List<Block> blocks = new ArrayList<>(this.blocks);
        blocks.sort(Comparator.comparingDouble(b -> location.distance(b.getLocation())));
        return blocks.get(0);
    }
}
