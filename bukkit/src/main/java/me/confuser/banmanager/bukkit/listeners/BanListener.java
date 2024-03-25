package me.confuser.banmanager.bukkit.listeners;

import me.confuser.banmanager.bukkit.api.events.*;
import me.confuser.banmanager.common.BanManagerPlugin;
import me.confuser.banmanager.common.listeners.CommonBanListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class BanListener implements Listener {

  private final CommonBanListener listener;

  public BanListener(BanManagerPlugin plugin) {
    this.listener = new CommonBanListener(plugin);
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void notifyOnBan(PlayerBannedEvent event) {
    listener.notifyOnBan(event.getBan(), event.isSilent());
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void notifyOnABan(PlayerABannedEvent event) {
    listener.notifyOnABan(event.getBan(), event.isSilent());
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void notifyOnIpBan(IpBannedEvent event) {
    listener.notifyOnBan(event.getBan(), event.isSilent());
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void notifyOnIpRangeBan(IpRangeBannedEvent event) {
    listener.notifyOnBan(event.getBan(), event.isSilent());
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void notifyOnNameBan(NameBannedEvent event) {
    listener.notifyOnBan(event.getBan(), event.isSilent());
  }
}
