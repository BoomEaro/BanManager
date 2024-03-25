package me.confuser.banmanager.bukkit.api.events;

import lombok.Getter;
import me.confuser.banmanager.common.data.PlayerBanData;


public class PlayerABannedEvent extends SilentEvent {

  @Getter
  private PlayerBanData ban;

  public PlayerABannedEvent(PlayerBanData ban, boolean isSilent) {
    super(isSilent, true);
    this.ban = ban;
  }

}
