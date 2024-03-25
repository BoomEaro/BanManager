package me.confuser.banmanager.bukkit.api.events;

import lombok.Getter;
import me.confuser.banmanager.common.data.PlayerBanData;
import me.confuser.banmanager.common.data.PlayerData;


public class PlayerAUnbanEvent extends CustomCancellableEvent {

  @Getter
  private PlayerBanData ban;
  @Getter
  private PlayerData actor;
  @Getter
  private String reason;

  public PlayerAUnbanEvent(PlayerBanData ban, PlayerData actor, String reason) {
    super(true);

    this.ban = ban;
    this.actor = actor;
    this.reason = reason;
  }

}
