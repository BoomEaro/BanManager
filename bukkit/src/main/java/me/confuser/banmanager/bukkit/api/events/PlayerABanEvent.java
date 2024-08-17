package me.confuser.banmanager.bukkit.api.events;

import lombok.Getter;
import me.confuser.banmanager.common.data.PlayerABanData;


public class PlayerABanEvent extends SilentCancellableEvent {

  @Getter
  private PlayerABanData ban;

  public PlayerABanEvent(PlayerABanData ban, boolean isSilent) {
    super(isSilent, true);
    this.ban = ban;
  }

}
