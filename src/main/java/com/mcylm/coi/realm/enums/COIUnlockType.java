package com.mcylm.coi.realm.enums;

import com.mcylm.coi.realm.tools.building.COIBuilding;
import com.mcylm.coi.realm.tools.team.impl.COITeam;
import com.mcylm.coi.realm.utils.SkullUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * 解锁类型
 */
@Getter
@AllArgsConstructor
public enum COIUnlockType {

    // 磨坊解锁判断
    LOCK_MILL(
        "LOCK_MILL",
        "&c尚未解锁 &b磨坊",
        SkullUtils.createPlayerHead(COIHeadType.LOCK_CHEST.getTextures()),
        """
        建造1个矿场后解锁""",
        COIBuildingType.MILL,
        COIBuildingType.STOPE,
        1,
        1
        ),

    // 兵营解锁
    LOCK_CAMP(
            "LOCK_CAMP",
            "&c尚未解锁 &b军营",
            SkullUtils.createPlayerHead(COIHeadType.LOCK_CHEST.getTextures()),
            """
            建造5个矿场后解锁""",
            COIBuildingType.MILITARY_CAMP,
            COIBuildingType.STOPE,
            5,
            1
    ),

    // 城墙解锁
    LOCK_WALL(
            "LOCK_WALL",
            "&c尚未解锁 &b城墙",
            SkullUtils.createPlayerHead(COIHeadType.LOCK_CHEST.getTextures()),
            """
            升级基地到2级解锁""",
            COIBuildingType.WALL_NORMAL,
            COIBuildingType.BASE,
            1,
            2
    ),

    // 城门解锁
    LOCK_DOOR(
            "LOCK_DOOR",
            "&c尚未解锁 &b城门",
            SkullUtils.createPlayerHead(COIHeadType.LOCK_CHEST.getTextures()),
            """
            升级基地到2级解锁""",
            COIBuildingType.DOOR_NORMAL,
            COIBuildingType.BASE,
            1,
            2
    ),

    // 防御塔解锁
    LOCK_TURRET(
            "LOCK_TURRET",
            "&c尚未解锁 &b基础防御塔",
            SkullUtils.createPlayerHead(COIHeadType.LOCK_CHEST.getTextures()),
            """
            任意1个矿场升级到2级,并且拥有3个矿场后解锁""",
            COIBuildingType.TURRET_NORMAL,
            COIBuildingType.STOPE,
            3,
            2
    ),

    // 修复塔解锁
    LOCK_REPAIR(
            "LOCK_REPAIR",
            "&c尚未解锁 &b修复塔",
            SkullUtils.createPlayerHead(COIHeadType.LOCK_CHEST.getTextures()),
            """
            任意1个矿场升级到3级,并且拥有5个矿场后解锁""",
            COIBuildingType.TURRET_REPAIR,
            COIBuildingType.STOPE,
            5,
            3
    ),

    // 铁匠铺
    LOCK_FORGE(
            "LOCK_REPAIR",
            "&c尚未解锁 &b铁匠铺",
            SkullUtils.createPlayerHead(COIHeadType.LOCK_CHEST.getTextures()),
            """
            升级基地到5级解锁""",
            COIBuildingType.FORGE,
            COIBuildingType.BASE,
            1,
            5
    ),

    ;


    // CODE
    private String code;
    // 未解锁时显示的名称 name
    private String name;
    // GUI显示的材质
    private ItemStack itemType;
    // 解锁条件介绍
    private String introduce;
    // 待解锁的建筑类型
    private COIBuildingType objBuildingType;

    // 下面这三个是解锁建筑的条件
    // 解锁逻辑如下：
    // 1.如果前置建筑类型为null，则直接解锁
    // 2.如果前置建筑不为null，则先判断当前小队是否已经解锁前置建筑，
    //   此时如果已经建造，同时建筑数量条件和等级条件都是0，则直接解锁
    // 3.如果前置建筑不为null，判断建筑数量和等级是否满足要求，满足就解锁

    // 前置建筑类型
    private COIBuildingType preBuildingType;
    // 建筑数量
    private int buildingsNum;
    // 前置建筑满足的等级
    private int buildingLevel;

    /**
     * 自动检查当前小队是否满足解锁条件
     * @param team
     * @param type
     * @return
     */
    public static boolean checkUnlock(COITeam team,COIBuildingType type){
        COIUnlockType[] values = COIUnlockType.values();

        for(COIUnlockType unlockType : values){

            // 匹配建筑类型
            if(unlockType.getObjBuildingType().equals(type)){
                // 匹配到了
                // 1.检查前置建筑类型是否为null
                if(unlockType.getPreBuildingType() == null){
                    // 直接解锁
                    return true;
                }

                // 没有限制，直接解锁
                if(unlockType.getBuildingsNum() == 0
                    && unlockType.getBuildingLevel() == 0){
                    return true;
                }

                // 2.判断当前小队是否已经解锁前置建筑
                // 前置建筑建造的数量
                int perBuildingCount = 0;
                int maxLevel = 0;
                List<COIBuilding> finishedBuildings = team.getFinishedBuildings();

                for(COIBuilding building : finishedBuildings){
                    // 如果匹配到前置建筑
                    if(building.getType().equals(unlockType.getPreBuildingType())){
                        perBuildingCount ++;
                        if(building.getLevel() > maxLevel){
                            maxLevel = building.getLevel();
                        }
                    }
                }

                // 如果两个条件同时满足
                if(perBuildingCount >= unlockType.getBuildingsNum()
                    && maxLevel >= unlockType.getBuildingLevel()){
                    return true;
                }

                // 未在上面满足条件，未解锁
                return false;
            }
        }

        // 在本枚举类里没有配置，则默认解锁
        return true;
    }

    /**
     * 根据建筑类型获取未解锁物品
     * @param type
     * @return
     */
    public static COIUnlockType getUnlockItem(COIBuildingType type){
        COIUnlockType[] values = COIUnlockType.values();

        for(COIUnlockType unlockType : values){

            // 匹配建筑类型
            if(unlockType.getObjBuildingType().equals(type)){
                return unlockType;
            }
        }

        return null;
    }

}