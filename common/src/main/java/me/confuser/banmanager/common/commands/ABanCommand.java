package me.confuser.banmanager.common.commands;

import me.confuser.banmanager.common.BanManagerPlugin;
import me.confuser.banmanager.common.CommonPlayer;
import me.confuser.banmanager.common.data.PlayerABanData;
import me.confuser.banmanager.common.data.PlayerData;
import me.confuser.banmanager.common.util.DateUtils;
import me.confuser.banmanager.common.util.Message;

import java.sql.SQLException;
import java.util.UUID;

public class ABanCommand extends CommonCommand {

  public ABanCommand(BanManagerPlugin plugin) {
    super(plugin, "aban", true, 1);
  }

  @Override
  public boolean onCommand(CommonSender sender, CommandParser parser) {
    final boolean isSilent = parser.isSilent();

    if (isSilent && !sender.hasPermission(getPermission() + ".silent")) {
      sender.sendMessage(Message.getString("sender.error.noPermission"));
      return true;
    }

    if (parser.args.length < 2) {
      return false;
    }

    if (parser.isInvalidReason()) {
      Message.get("sender.error.invalidReason")
              .set("reason", parser.getReason().getMessage())
              .sendTo(sender);
      return true;
    }

    if (parser.args[0].equalsIgnoreCase(sender.getName())) {
      sender.sendMessage(Message.getString("sender.error.noSelf"));
      return true;
    }

    // Check if UUID vs name
    final String playerName = parser.args[0];
    final boolean isUUID = isUUID(playerName);
    final boolean isBanned;

    if (isUUID) {
      try {
        isBanned = getPlugin().getPlayerABanStorage().isBanned(UUID.fromString(playerName));
      } catch (IllegalArgumentException e) {
        sender.sendMessage(Message.get("sender.error.notFound").set("player", playerName).toString());
        return true;
      }
    } else {
      isBanned = getPlugin().getPlayerABanStorage().isBanned(playerName);
    }

    if (isBanned && !sender.hasPermission("bm.command.aban.override")) {
      Message message = Message.get("aban.error.exists");
      message.set("player", playerName);

      sender.sendMessage(message.toString());
      return true;
    }

    final CommonPlayer onlinePlayer;

    if (isUUID) {
      onlinePlayer = getPlugin().getServer().getPlayer(UUID.fromString(playerName));
    } else {
      onlinePlayer = getPlugin().getServer().getPlayer(playerName);
    }

    if (onlinePlayer == null) {
      if (!sender.hasPermission("bm.command.aban.offline")) {
        sender.sendMessage(Message.getString("sender.error.offlinePermission"));
        return true;
      }
    } else if (!sender.hasPermission("bm.exempt.override.aban") && onlinePlayer.hasPermission("bm.exempt.aban")) {
      Message.get("sender.error.exempt").set("player", onlinePlayer.getName()).sendTo(sender);
      return true;
    }

    getPlugin().getScheduler().runAsync(() -> {
      final PlayerData player = getPlayer(sender, playerName, true);

      if (player == null) {
        sender.sendMessage(Message.get("sender.error.notFound").set("player", playerName).toString());
        return;
      }

      if (getPlugin().getExemptionsConfig().isExempt(player, "ban")) {
        sender.sendMessage(Message.get("sender.error.exempt").set("player", playerName).toString());
        return;
      }

      try {
        if (getPlugin().getPlayerABanStorage().isRecentlyBanned(player, getCooldown())) {
          Message.get("aban.error.cooldown").sendTo(sender);
          return;
        }
      } catch (SQLException e) {
        sender.sendMessage(Message.get("sender.error.exception").toString());
        e.printStackTrace();
        return;
      }

      final PlayerData actor = sender.getData();

      if (actor == null) return;

      if (isBanned) {
        PlayerABanData ban;

        if (isUUID) {
          ban = getPlugin().getPlayerABanStorage().getBan(UUID.fromString(playerName));
        } else {
          ban = getPlugin().getPlayerABanStorage().getBan(playerName);
        }

        if (ban != null) {
          try {
            getPlugin().getPlayerABanStorage().unban(ban, actor);
          } catch (SQLException e) {
            sender.sendMessage(Message.get("sender.error.exception").toString());
            e.printStackTrace();
            return;
          }
        }
      }

      final PlayerABanData ban = new PlayerABanData(player, actor, parser.getReason().getMessage(), isSilent);
      boolean created;

      try {
        created = getPlugin().getPlayerABanStorage().ban(ban);
      } catch (SQLException e) {
        handlePunishmentCreateException(e, sender, Message.get("aban.error.exists").set("player",
                playerName));
        return;
      }

      if (!created) {
        return;
      }

      if (sender.isTrueNotConsole()) {
          Message message;

          if (ban.getExpires() == 0) {
              message = Message.get("aban.notify");
          } else {
              message = Message.get("atempban.notify");
              message.set("expires", DateUtils.getDifferenceFormat(ban.getExpires()));
          }

          message
                  .set("id", ban.getId())
                  .set("player", ban.getPlayer().getName())
                  .set("playerId", ban.getPlayer().getUUID().toString())
                  .set("actor", ban.getActor().getName())
                  .set("reason", ban.getReason());

          sender.sendMessage(message);
      }

      handlePrivateNotes(player, actor, parser.getReason());

      getPlugin().getScheduler().runSync(() -> {
        if (onlinePlayer == null) return;

        Message kickMessage = Message.get("aban.player.kick")
                                     .set("displayName", onlinePlayer.getDisplayName())
                                     .set("player", player.getName())
                                     .set("playerId", player.getUUID().toString())
                                     .set("reason", ban.getReason())
                                     .set("id", ban.getId())
                                     .set("actor", actor.getName());

        onlinePlayer.kick(kickMessage.toString());
      });
    });

    return true;
  }
}
