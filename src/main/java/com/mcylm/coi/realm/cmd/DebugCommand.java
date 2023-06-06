package com.mcylm.coi.realm.cmd;

import com.mcylm.coi.realm.gui.ChooseTeamGUI;
import com.mcylm.coi.realm.utils.LoggerUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class DebugCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if(!(commandSender instanceof Player)){
            // 这个指令只能让玩家使用
            // This command only player can use
            LoggerUtils.sendMessage("这个指令只能让玩家使用。",commandSender);
            return false;
        }
        Player player = ((Player) commandSender);
        if (args[0].equalsIgnoreCase("team")) {
            new ChooseTeamGUI(player).open();
        }
        if (args[0].equalsIgnoreCase("monster")) {

        }

        return true;
    }
}
