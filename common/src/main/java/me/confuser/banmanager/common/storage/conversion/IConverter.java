package me.confuser.banmanager.common.storage.conversion;

public interface IConverter {

  void importPlayerBans();

  void importPlayerABans();

  void importPlayerMutes();

  void importPlayerWarnings();

  void importIpBans();

  void importIpRangeBans();
}
