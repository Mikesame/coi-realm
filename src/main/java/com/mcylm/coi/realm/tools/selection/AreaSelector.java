package com.mcylm.coi.realm.tools.selection;

import com.destroystokyo.paper.ParticleBuilder;
import com.mcylm.coi.realm.Entry;
import com.mcylm.coi.realm.model.COIBlock;
import com.mcylm.coi.realm.model.COIStructure;
import com.mcylm.coi.realm.tools.building.COIBuilding;
import com.mcylm.coi.realm.tools.building.FloatableBuild;
import com.mcylm.coi.realm.tools.building.data.BuildData;
import com.mcylm.coi.realm.utils.particle.ParticleRect;
import com.mcylm.coi.realm.utils.region.Region;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@Setter
public class AreaSelector {

    public static Map<Player, AreaSelector> areaSelectors = new ConcurrentHashMap<>();

    private Player player;
    private boolean canPlace = false;
    private Location selectedLocation;
    private COIBuilding building;
    private boolean stop;

    public AreaSelector(Player p, COIBuilding building, Location location) {
        this.player = p;
        this.selectedLocation = location;
        this.stop = false;
        this.building = building;
        areaSelectors.put(p, this);

        String structureName = building.getStructureByLevel();

        if (structureName == null) {
            return;
        }
        // 实例化建筑结构
        COIStructure structure = building.prepareStructure(Entry.getBuilder().getStructureByFile(structureName), player);

        new BukkitRunnable() {
            @Override
            public void run() {

                if (stop) {
                    this.cancel();
                } else {
                    if (!player.isOnline()) {
                        stop(false);
                    }
                    select(selectedLocation, structure);
                }
            }
        }.runTaskTimerAsynchronously(Entry.getInstance(), 15, 20);
    }



    public void select(Location basicLocation, COIStructure structure) {

        List<COIBlock> allBlocks = structure.getBlocks();

        Vector maxXYZ = new Vector();

        Vector minXYZ = new Vector();
        boolean first = true;
        for (COIBlock coiBlock : allBlocks) {
            if (first) {
                maxXYZ.setX(coiBlock.getX());
                maxXYZ.setY(coiBlock.getY());
                maxXYZ.setZ(coiBlock.getZ());

                minXYZ = maxXYZ.clone();
                first = false;
            }

            if (maxXYZ.getX() < coiBlock.getX()) {
                maxXYZ.setX(coiBlock.getX());
            }
            if (maxXYZ.getY() < coiBlock.getY()) {
                maxXYZ.setY(coiBlock.getY());
            }
            if (maxXYZ.getZ() < coiBlock.getZ()) {
                maxXYZ.setZ(coiBlock.getZ());
            }


            if (minXYZ.getX() > coiBlock.getX()) {
                maxXYZ.setX(coiBlock.getX());
            }
            if (minXYZ.getY() > coiBlock.getY()) {
                minXYZ.setY(coiBlock.getY());
            }
            if (minXYZ.getZ() > coiBlock.getZ()) {
                maxXYZ.setZ(coiBlock.getZ());
            }
        }

        Location start = basicLocation.clone().add(minXYZ);
        Location end = basicLocation.clone().add(maxXYZ);

        Region region = new Region(start, end);

        canPlace = true;

        for (Block block : region.getBlocks()) {
            if (BuildData.getBuildingByBlock(block) != null) {
                canPlace = false;
            }
        }

        String state = canPlace ? "§a可放置" : "§c不可放置";

        player.sendActionBar("§a潜行进行放置 右键选择新点 §c切换物品取消 §e当前状态: " + state);

        ParticleRect rect = new ParticleRect(start, end.getZ() - start.getZ(), end.getX() - start.getX(), end.getY() - start.getY());

        if (player.isSneaking() && canPlace) {
            Bukkit.getScheduler().runTask(Entry.getInstance(), () -> place(structure));
        }

        rect.draw(canPlace ? new ParticleBuilder(Particle.REDSTONE).color(Color.LIME).receivers(player) : new ParticleBuilder(Particle.REDSTONE).color(Color.RED).receivers(player), player);
    }

    public void place(COIStructure structure) {
        List<COIBlock> allBlocks = structure.getBlocks();

        Vector maxXYZ = new Vector();

        Vector minXYZ = new Vector();
        boolean first = true;
        for (COIBlock coiBlock : allBlocks) {
            if (first) {
                maxXYZ.setX(coiBlock.getX());
                maxXYZ.setY(coiBlock.getY());
                maxXYZ.setZ(coiBlock.getZ());

                minXYZ = maxXYZ.clone();
                first = false;
            }

            if (maxXYZ.getX() < coiBlock.getX()) {
                maxXYZ.setX(coiBlock.getX());
            }
            if (maxXYZ.getY() < coiBlock.getY()) {
                maxXYZ.setY(coiBlock.getY());
            }
            if (maxXYZ.getZ() < coiBlock.getZ()) {
                maxXYZ.setZ(coiBlock.getZ());
            }


            if (minXYZ.getX() > coiBlock.getX()) {
                maxXYZ.setX(coiBlock.getX());
            }
            if (minXYZ.getY() > coiBlock.getY()) {
                minXYZ.setY(coiBlock.getY());
            }
            if (minXYZ.getZ() > coiBlock.getZ()) {
                maxXYZ.setZ(coiBlock.getZ());
            }
        }

        Location start = selectedLocation.clone().add(minXYZ);
        Location end = selectedLocation.clone().add(maxXYZ);

        Region region = new Region(start, end);

        canPlace = true;

        for (Block block : region.getBlocks()) {
            if (BuildData.getBuildingByBlock(block) != null) {
                canPlace = false;
            }
        }


        Region regionFloor = new Region(start.clone().subtract(0,1,0), end.set(end.getX(), start.getY() - 1, end.getY()));
        Set<Block> blocks = regionFloor.getBlocks();
        float emptyCount = 0;
        for (Block block : blocks) {
            if (!block.isSolid()) {
                emptyCount++;
            }
        }
        if (!(building instanceof FloatableBuild)) {
            if (emptyCount / blocks.size() >= 0.4) {
                canPlace = false;
            }
        }
        if (canPlace) {
            stop(false);
            building.build(selectedLocation, player);
        }
    }

    public void stop(boolean sendMsg) {
        setStop(true);
        if (sendMsg) player.sendActionBar("§c已取消");

        areaSelectors.remove(player);
    }
}