package me.confuser.banmanager.bungee.listeners;

import lombok.RequiredArgsConstructor;
import me.confuser.banmanager.bungee.BMBungeePlugin;
import me.confuser.banmanager.bungee.BungeePlayer;
import me.confuser.banmanager.bungee.BungeeServer;
import me.confuser.banmanager.common.BanManagerPlugin;
import me.confuser.banmanager.common.data.PlayerData;
import me.confuser.banmanager.common.listeners.CommonJoinHandler;
import me.confuser.banmanager.common.listeners.CommonJoinListener;
import me.confuser.banmanager.common.util.IPUtils;
import me.confuser.banmanager.common.util.Message;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class JoinListener implements Listener {
    private final CommonJoinListener listener;
    private final BMBungeePlugin plugin;

    public JoinListener(BMBungeePlugin plugin) {
        this.plugin = plugin;
        this.listener = new CommonJoinListener(plugin.getPlugin());
    }

    @EventHandler
    public void banCheck(PostLoginEvent event) {
        ProxiedPlayer player = event.getPlayer();
        event.registerIntent(plugin);

        plugin.getPlugin().getScheduler().runAsync(() -> {
            try {
                if (!player.isConnected()) {
                    return;
                }

                listener.banCheck(player.getUniqueId(), player.getName(), IPUtils.toIPAddress(player.getAddress().getAddress()), new BanJoinHandler(plugin.getPlugin(), event));

                if (!player.isConnected()) {
                    return;
                }

                listener.onPreJoin(player.getUniqueId(), player.getName(), IPUtils.toIPAddress(player.getAddress().getAddress()));

                if (!player.isConnected()) {
                    return;
                }

                listener.onPlayerLogin(new BungeePlayer(event.getPlayer(), plugin.getPlugin().getConfig().isOnlineMode()), new LoginHandler(event));

                if (!player.isConnected()) {
                    return;
                }

                listener.onJoin(new BungeePlayer(event.getPlayer(), plugin.getPlugin().getConfig().isOnlineMode()));
            }
            finally {
                event.completeIntent(plugin);
            }
        });
    }

    @RequiredArgsConstructor
    private static class BanJoinHandler implements CommonJoinHandler {
        private final BanManagerPlugin plugin;
        private final PostLoginEvent event;

        @Override
        public void handlePlayerDeny(PlayerData player, Message message) {
            plugin.getServer().callEvent("PlayerDeniedEvent", player, message);

            handleDeny(message);
        }

        @Override
        public void handleDeny(Message message) {
            event.getPlayer().disconnect(BungeeServer.formatMessage(message.toString()));
        }
    }

    @RequiredArgsConstructor
    private static class LoginHandler implements CommonJoinHandler {
        private final PostLoginEvent event;

        @Override
        public void handlePlayerDeny(PlayerData player, Message message) {
            handleDeny(message);
        }

        @Override
        public void handleDeny(Message message) {
            event.getPlayer().disconnect(BungeeServer.formatMessage(message.toString()));
        }
    }
}
