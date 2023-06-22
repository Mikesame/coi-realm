package com.mcylm.coi.realm.gui;

import com.mcylm.coi.realm.Entry;
import com.mcylm.coi.realm.enums.COIBuildingType;
import com.mcylm.coi.realm.enums.COIGameStatus;
import com.mcylm.coi.realm.enums.COIHeadType;
import com.mcylm.coi.realm.enums.COIPropType;
import com.mcylm.coi.realm.gui.GuiBuilder.COIPageGuiBuilder;
import com.mcylm.coi.realm.model.COISkin;
import com.mcylm.coi.realm.player.COIPlayer;
import com.mcylm.coi.realm.tools.building.COIBuilding;
import com.mcylm.coi.realm.tools.team.impl.COITeam;
import com.mcylm.coi.realm.utils.*;
import me.lucko.helper.item.ItemStackBuilder;
import me.lucko.helper.menu.Item;
import me.lucko.helper.menu.paginated.PaginatedGuiBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class SkinGUI {

    /**
     * 更换皮肤GUI
     * @param p 需要打开GUI的玩家
     */
    public SkinGUI(Player p) {

        COIPlayer coiPlayer = Entry.getGame().getCOIPlayer(p);

        COIPageGuiBuilder builder = COIPageGuiBuilder.create();

        builder.title("&9&l选择你要装备的皮肤");
        builder.previousPageSlot(48);
        builder.customSlot(49);
        builder.nextPageSlot(50);
        builder.nextPageItem((pageInfo) -> ItemStackBuilder.of(Material.COMPARATOR).name("&a下一页").build());
        builder.previousPageItem((pageInfo) -> ItemStackBuilder.of(Material.REPEATER).name("&a上一页").build());
        builder.customItem((pageInfo) -> ItemStackBuilder.of(Material.ENDER_CHEST).name("&b返回").build());

        builder.build(p, paginatedGui -> {
            List<Item> items = new ArrayList<>();

            for (COISkin coiSkin : Entry.getSkinData().getSkins()) {

                // 物品模型
                ItemStack item = new ItemStack(Material.getMaterial(coiSkin.getMaterial()));

                // 判断是否达到解锁条件
                if(p.hasPermission(coiSkin.getPermission())){

                    COIBuildingType buildingTypeByCode = COIBuildingType.getBuildingTypeByCode(coiSkin.getBuildingTypeCode());

                    boolean selectedBoolean = false;
                    String selected = "&c未装备";
                    String button = "&a装备";

                    COISkin selectedSkin = coiPlayer.getSelectedSkins().get(coiSkin.getBuildingTypeCode());
                    if(selectedSkin != null
                        && selectedSkin.getCode().equals(coiSkin.getCode())){
                        selectedBoolean = true;
                        selected = "&a已装备";
                        button = "&c取消装备";
                    }

                    boolean finalSelectedBoolean = selectedBoolean;
                    items.add(ItemStackBuilder.of(item.clone())
                            .name(coiSkin.getName())
                            .amount(1)
                            .lore("")
                            .lore("&f> &a所属建筑: &a" + buildingTypeByCode.getName())
                            .lore("&f> &a是否已装备: " + selected)
                            .lore("&f> &c同一个建筑只能装备一个皮肤")
                            .lore("")
                            .lore("&f> &a&l点击进行"+button)
                            .build(() -> {
                                // 点击时触发下面的方法
                                if(finalSelectedBoolean){
                                    // 装备的情况下，取消装备
                                    coiPlayer.getSelectedSkins().remove(coiSkin.getBuildingTypeCode());
                                    LoggerUtils.sendMessage("&a已取消装备皮肤："+coiSkin.getName(), p);
                                }else{
                                    coiPlayer.getSelectedSkins().put(coiSkin.getBuildingTypeCode(),coiSkin);
                                    LoggerUtils.sendMessage("&a已装备皮肤："+coiSkin.getName(), p);
                                }

                            }));
                }else{
                    // 不满足解锁条件

                    ItemStack itemType = new ItemStack(Material.CHEST);

                    items.add(ItemStackBuilder.of(itemType.clone())
                            .name(coiSkin.getName()+" &c尚未解锁")
                            .amount(1)
                            .lore("")
                            .lore("&f> &a解锁条件：")
                            .lore(GUIUtils.autoLineFeed("使用积分兑换"))
                            .lore("")
                            .build(paginatedGui::close));

                }
            }
            return items;
        }).open();
    }

    public SkinGUI(Player p,COIBuildingType type) {

        COIPlayer coiPlayer = Entry.getGame().getCOIPlayer(p);

        COIPageGuiBuilder builder = COIPageGuiBuilder.create();

        builder.title("&9&l选择你要装备的"+type.getName()+"皮肤");
        builder.previousPageSlot(48);
        builder.customSlot(49);
        builder.nextPageSlot(50);
        builder.nextPageItem((pageInfo) -> ItemStackBuilder.of(Material.COMPARATOR).name("&a下一页").build());
        builder.previousPageItem((pageInfo) -> ItemStackBuilder.of(Material.REPEATER).name("&a上一页").build());
        builder.customItem((pageInfo) -> ItemStackBuilder.of(Material.ENDER_CHEST).name("&b返回").build());

        builder.build(p, paginatedGui -> {
            List<Item> items = new ArrayList<>();

            COISkin selectedSkinByType = coiPlayer.getSelectedSkins().get(type.getCode());

            boolean defaultSkin = false;
            String defaultSkinSelected = "&c未装备";
            String defaultButton = "&f> &a&l点击进行装备";
            if(selectedSkinByType == null){
                defaultSkin = true;
                defaultSkinSelected = "&a已装备";
                defaultButton = "";
            }

            // 默认皮肤
            boolean finalDefaultSkin = defaultSkin;
            items.add(ItemStackBuilder.of(type.getItemType().clone())
                    .name(type.getName()+"默认皮肤")
                    .amount(1)
                    .lore("")
                    .lore("&f> &a所属建筑: &6" + type.getName())
                    .lore("&f> &a状态: " + defaultSkinSelected)
                    .lore("")
                    .lore(defaultButton)
                    .build(() -> {
                        // 点击时触发下面的方法
                        if(finalDefaultSkin){
                            // 装备的情况下，取消装备
                        }else{
                            coiPlayer.getSelectedSkins().remove(type.getCode());
                            LoggerUtils.sendMessage("&a已装备皮肤："+type.getName()+"默认皮肤", p);
                        }

                    }));

            for (COISkin coiSkin : Entry.getSkinData().getSkins()) {

                if(coiSkin.getBuildingTypeCode().equals(type.getCode())){
                    // 物品模型
                    ItemStack item = new ItemStack(Material.getMaterial(coiSkin.getMaterial()));

                    // 判断是否达到解锁条件
                    if(p.hasPermission(coiSkin.getPermission())){

                        COIBuildingType buildingTypeByCode = COIBuildingType.getBuildingTypeByCode(coiSkin.getBuildingTypeCode());

                        boolean selectedBoolean = false;
                        String selected = "&c未装备";
                        String button = "&a&l装备";

                        COISkin selectedSkin = coiPlayer.getSelectedSkins().get(coiSkin.getBuildingTypeCode());
                        if(selectedSkin != null
                                && selectedSkin.getCode().equals(coiSkin.getCode())){
                            selectedBoolean = true;
                            selected = "&a已装备";
                            button = "&c&l取消装备";
                        }

                        boolean finalSelectedBoolean = selectedBoolean;
                        items.add(ItemStackBuilder.of(item.clone())
                                .name(coiSkin.getName())
                                .amount(1)
                                .lore("")
                                .lore("&f> &a所属建筑: &6" + buildingTypeByCode.getName())
                                .lore("&f> &a状态: " + selected)
                                .lore("&f> &c同一个建筑只能装备一个皮肤")
                                .lore("")
                                .lore("&f> &a&l点击进行"+button)
                                .build(() -> {
                                    // 点击时触发下面的方法
                                    if(finalSelectedBoolean){
                                        // 装备的情况下，取消装备
                                        coiPlayer.getSelectedSkins().remove(coiSkin.getBuildingTypeCode());
                                        LoggerUtils.sendMessage("&a已取消装备皮肤："+coiSkin.getName(), p);
                                    }else{
                                        coiPlayer.getSelectedSkins().put(coiSkin.getBuildingTypeCode(),coiSkin);
                                        LoggerUtils.sendMessage("&a已装备皮肤："+coiSkin.getName(), p);
                                    }

                                }));
                    }else{
                        // 不满足解锁条件

                        ItemStack itemType = new ItemStack(Material.CHEST);

                        items.add(ItemStackBuilder.of(itemType.clone())
                                .name(coiSkin.getName()+" &c尚未解锁")
                                .amount(1)
                                .lore("")
                                .lore("&f> &a解锁条件：")
                                .lore(GUIUtils.autoLineFeed("使用积分兑换"))
                                .lore("")
                                .build(paginatedGui::close));

                    }
                }

            }
            return items;
        }).open();
    }
}
