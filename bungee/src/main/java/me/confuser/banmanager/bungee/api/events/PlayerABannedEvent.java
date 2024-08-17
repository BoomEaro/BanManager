package me.confuser.banmanager.bungee.api.events;

import lombok.Getter;
import me.confuser.banmanager.common.data.PlayerABanData;


public class PlayerABannedEvent extends SilentEvent {

  @Getter
  private PlayerABanData ban;

  public PlayerABannedEvent(PlayerABanData ban, boolean isSilent) {
    super(isSilent);
    this.ban = ban;
  }

}
