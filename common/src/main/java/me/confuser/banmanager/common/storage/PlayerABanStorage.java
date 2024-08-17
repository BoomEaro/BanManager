package me.confuser.banmanager.common.storage;

import me.confuser.banmanager.common.BanManagerPlugin;
import me.confuser.banmanager.common.api.events.CommonEvent;
import me.confuser.banmanager.common.data.PlayerABanData;
import me.confuser.banmanager.common.data.PlayerData;
import me.confuser.banmanager.common.ipaddr.AddressValueException;
import me.confuser.banmanager.common.ipaddr.IPAddress;
import me.confuser.banmanager.common.ormlite.dao.BaseDaoImpl;
import me.confuser.banmanager.common.ormlite.dao.CloseableIterator;
import me.confuser.banmanager.common.ormlite.field.DataType;
import me.confuser.banmanager.common.ormlite.stmt.QueryBuilder;
import me.confuser.banmanager.common.ormlite.stmt.StatementBuilder;
import me.confuser.banmanager.common.ormlite.stmt.Where;
import me.confuser.banmanager.common.ormlite.support.CompiledStatement;
import me.confuser.banmanager.common.ormlite.support.ConnectionSource;
import me.confuser.banmanager.common.ormlite.support.DatabaseConnection;
import me.confuser.banmanager.common.ormlite.support.DatabaseResults;
import me.confuser.banmanager.common.ormlite.table.DatabaseTableConfig;
import me.confuser.banmanager.common.ormlite.table.TableUtils;
import me.confuser.banmanager.common.util.DateUtils;
import me.confuser.banmanager.common.util.IPUtils;
import me.confuser.banmanager.common.util.UUIDUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerABanStorage extends BaseDaoImpl<PlayerABanData, Integer> {

  private BanManagerPlugin plugin;
  private ConcurrentHashMap<UUID, PlayerABanData> bans = new ConcurrentHashMap<>();

  public PlayerABanStorage(BanManagerPlugin plugin) throws SQLException {
    super(plugin.getLocalConn(), (DatabaseTableConfig<PlayerABanData>) plugin.getConfig().getLocalDb()
        .getTable("playerABans"));

    this.plugin = plugin;

    DatabaseTableConfig<PlayerABanData> test = (DatabaseTableConfig<PlayerABanData>) plugin.getConfig().getLocalDb()
            .getTable("playerABans");

    test.getTableName();

    if (!this.isTableExists()) {
      TableUtils.createTable(connectionSource, tableConfig);
      return;
    } else {
      try {
        executeRawNoArgs("ALTER TABLE " + tableConfig.getTableName() + " ADD COLUMN `silent` TINYINT(1)");
      } catch (SQLException e) {
      }

      try {
        executeRawNoArgs("ALTER TABLE " + tableConfig.getTableName()
          + " CHANGE `created` `created` BIGINT UNSIGNED,"
          + " CHANGE `updated` `updated` BIGINT UNSIGNED,"
          + " CHANGE `expires` `expires` BIGINT UNSIGNED"
        );
      } catch (SQLException e) {
      }
    }

    loadAll();

    plugin.getLogger().info("Loaded " + bans.size() + " abans into memory");
  }

  public PlayerABanStorage(ConnectionSource connection, DatabaseTableConfig<?> table) throws SQLException {
    super(connection, (DatabaseTableConfig<PlayerABanData>) table);
  }

  private void loadAll() throws SQLException {
    DatabaseConnection connection;

    try {
      connection = this.getConnectionSource().getReadOnlyConnection(getTableName());
    } catch (SQLException e) {
      e.printStackTrace();
      plugin.getLogger().warning("Failed to retrieve abans into memory");
      return;
    }
    StringBuilder sql = new StringBuilder();

    sql.append("SELECT t.id, p.id, p.name, p.ip, p.lastSeen, a.id, a.name, a.ip, a.lastSeen, t.reason,");
    sql.append(" t.expires, t.created, t.updated, t.silent");
    sql.append(" FROM ");
    sql.append(this.getTableInfo().getTableName());
    sql.append(" t LEFT JOIN ");
    sql.append(plugin.getPlayerStorage().getTableInfo().getTableName());
    sql.append(" p ON player_id = p.id");
    sql.append(" LEFT JOIN ");
    sql.append(plugin.getPlayerStorage().getTableInfo().getTableName());
    sql.append(" a ON actor_id = a.id");

    CompiledStatement statement;

    try {
      statement = connection.compileStatement(sql.toString(), StatementBuilder.StatementType.SELECT, null,
          DatabaseConnection.DEFAULT_RESULT_FLAGS, false);
    } catch (SQLException e) {
      e.printStackTrace();
      this.getConnectionSource().releaseConnection(connection);

      plugin.getLogger().warning("Failed to retrieve abans into memory");
      return;
    }

    DatabaseResults results = null;

    try {
      results = statement.runQuery(null);

      while (results.next()) {
        PlayerData player;

        try {
          player = new PlayerData(UUIDUtils.fromBytes(results.getBytes(1)), results.getString(2),
              IPUtils.toIPAddress(results.getBytes(3)),
              results.getLong(4));
        } catch (NullPointerException | AddressValueException e) {
          plugin.getLogger().warning("Missing or invalid player for ban " + results.getInt(0) + ", ignored");
          continue;
        }

        PlayerData actor;

        try {
          actor = new PlayerData(UUIDUtils.fromBytes(results.getBytes(5)), results.getString(6),
              IPUtils.toIPAddress(results.getBytes(7)),
              results.getLong(8));
        } catch (NullPointerException | AddressValueException e) {
          plugin.getLogger().warning("Missing or invalid actor for ban " + results.getInt(0) + ", ignored");
          continue;
        }

        PlayerABanData ban = new PlayerABanData(results.getInt(0), player, actor,
            results.getString(9),
            results.getBoolean(13),
            results.getLong(10),
            results.getLong(11),
            results.getLong(12));

        bans.put(ban.getPlayer().getUUID(), ban);
      }
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      if (results != null) results.closeQuietly();

      this.getConnectionSource().releaseConnection(connection);
    }
  }

  public ConcurrentHashMap<UUID, PlayerABanData> getBans() {
    return bans;
  }

  public boolean isBanned(UUID uuid) {
    return bans.get(uuid) != null;
  }

  public boolean isBanned(String playerName) {
    return getBan(playerName) != null;
  }

  public PlayerABanData retrieveBan(UUID uuid) throws SQLException {
    List<PlayerABanData> bans = queryForEq("player_id", UUIDUtils.toBytes(uuid));

    if (bans.isEmpty()) return null;

    return bans.get(0);
  }

  public PlayerABanData getBan(UUID uuid) {
    return bans.get(uuid);
  }

  public void addBan(PlayerABanData ban) {
    bans.put(ban.getPlayer().getUUID(), ban);

    plugin.getServer().callEvent("PlayerABannedEvent", ban, ban.isSilent() || !plugin.getConfig().isBroadcastOnSync());
  }

  public void removeBan(PlayerABanData ban) {
    removeBan(ban.getPlayer().getUUID());
  }

  public void removeBan(UUID uuid) {
    bans.remove(uuid);
  }

  public PlayerABanData getBan(String playerName) {
    for (PlayerABanData ban : bans.values()) {
      if (ban.getPlayer().getName().equalsIgnoreCase(playerName)) {
        return ban;
      }
    }

    return null;
  }

  public boolean ban(PlayerABanData ban) throws SQLException {
    CommonEvent event = plugin.getServer().callEvent("PlayerABanEvent", ban, ban.isSilent());

    if (event.isCancelled()) {
      return false;
    }

    create(ban);
    bans.put(ban.getPlayer().getUUID(), ban);

    plugin.getServer().callEvent("PlayerABannedEvent", ban, event.isSilent());

    return true;
  }

  public boolean unban(PlayerABanData ban, PlayerData actor) throws SQLException {
    return unban(ban, actor, "");
  }

  public boolean unban(PlayerABanData ban, PlayerData actor, String reason) throws SQLException {
    return unban(ban, actor, reason, false);
  }

  public boolean unban(PlayerABanData ban, PlayerData actor, String reason, boolean delete) throws SQLException {
    CommonEvent event = plugin.getServer().callEvent("PlayerAUnbanEvent", ban, actor, reason);

    if (event.isCancelled()) {
      return false;
    }

    delete(ban);
    bans.remove(ban.getPlayer().getUUID());

    if (!delete) plugin.getPlayerABanRecordStorage().addRecord(ban, actor, reason);

    return true;
  }

  public CloseableIterator<PlayerABanData> findBans(long fromTime) throws SQLException {
    if (fromTime == 0) {
      return iterator();
    }

    long checkTime = fromTime + DateUtils.getTimeDiff();

    QueryBuilder<PlayerABanData, Integer> query = queryBuilder();
    Where<PlayerABanData, Integer> where = query.where();
    where
        .ge("created", checkTime)
        .or()
        .ge("updated", checkTime);

    query.setWhere(where);

    return query.iterator();

  }

  public List<PlayerData> getDuplicates(IPAddress address) {
    ArrayList<PlayerData> players = new ArrayList<>();

    if (plugin.getConfig().getBypassPlayerIps().contains(address.toString())) {
      return players;
    }

    try {
      CloseableIterator<Object[]> itr = null;
      try {
        String banTableName = getTableName();
        String playerTableName = plugin.getDuplicatePlayerStorage().getTableName();
        itr = queryRaw("SELECT *\n" +
                "FROM " + banTableName + " \n" +
                "LEFT JOIN " + playerTableName + " ON " + playerTableName + ".id = " + banTableName + ".player_id\n" +
                "WHERE " + playerTableName + ".ip = UNHEX('" + address.toHexString(false) + "') \n" +
                ";", new DataType[]{
                DataType.INTEGER,
                DataType.BYTE_ARRAY,
                DataType.STRING,
                DataType.BYTE_ARRAY,
                DataType.LONG,
                DataType.LONG,
                DataType.LONG,
                DataType.BOOLEAN,
                DataType.BYTE_ARRAY,
                DataType.STRING,
                DataType.BYTE_ARRAY,
                DataType.LONG
        }).closeableIterator();

        while (itr.hasNext()) {
          Object[] data = itr.next();

          PlayerData player = new PlayerData(UUIDUtils.fromBytes((byte[]) data[9]), (String) data[9], address, (Long) data[11]);

          if (!plugin.getExemptionsConfig().isExempt(player, "alts")) {
            players.add(player);
          }
        }

      } catch (SQLException e) {
        e.printStackTrace();
      } finally {
        if (itr != null) itr.closeQuietly();
      }

    } catch (Exception e) {
      e.printStackTrace();
      return players;
    }

    return players;
  }

  public boolean isRecentlyBanned(PlayerData player, long cooldown) throws SQLException {
    if (cooldown == 0) {
      return false;
    }

    return queryBuilder().where()
        .eq("player_id", player).and()
        .ge("created", (System.currentTimeMillis() / 1000L) - cooldown)
        .countOf() > 0;
  }
}
