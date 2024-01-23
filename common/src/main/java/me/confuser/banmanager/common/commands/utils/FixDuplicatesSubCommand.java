package me.confuser.banmanager.common.commands.utils;

import me.confuser.banmanager.common.BanManagerPlugin;
import me.confuser.banmanager.common.commands.CommandParser;
import me.confuser.banmanager.common.commands.CommonSender;
import me.confuser.banmanager.common.commands.CommonSubCommand;
import me.confuser.banmanager.common.data.PlayerData;

import java.util.List;

public class FixDuplicatesSubCommand extends CommonSubCommand {
  public FixDuplicatesSubCommand(BanManagerPlugin plugin) {
    super(plugin, "fixduplicates");
  }

  @Override
  public boolean onCommand(CommonSender sender, CommandParser parser) {
    if (parser.getArgs().length != 1) {
      return false;
    }

    getPlugin().getScheduler().runAsync(() -> {
      try {
        String name = parser.getArgs()[0];

        List<PlayerData> players = getPlugin().getPlayerStorage().retrieve(name);
        for (PlayerData player : players) {
          if (player.getName().equals(name)) {
            continue;
          }
          getPlugin().getPlayerStorage().deleteById(player.getId());
        }

        sender.sendMessage("Successfully cleared all duplicate players except '" + name + "'");
      }
      catch (Exception e) {
        e.printStackTrace();
      }
    });

    return true;
  }

  @Override
  public String getHelp() {
    return "fixduplicates [name]";
  }

  @Override
  public String getPermission() {
    return "command.bmutils.fixduplicates";
  }
}
