package me.confuser.banmanager.bungee.api.events;

import lombok.Getter;
import me.confuser.banmanager.common.data.PlayerBanData;


public class PlayerABanEvent extends SilentCancellableEvent {

  @Getter
  private PlayerBanData ban;

  public PlayerABanEvent(PlayerBanData ban, boolean isSilent) {
    super(isSilent);
    this.ban = ban;
  }

}
