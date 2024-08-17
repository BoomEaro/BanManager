package me.confuser.banmanager.common.storage;

import me.confuser.banmanager.common.BanManagerPlugin;
import me.confuser.banmanager.common.configs.CleanUp;
import me.confuser.banmanager.common.data.PlayerABanData;
import me.confuser.banmanager.common.data.PlayerABanRecord;
import me.confuser.banmanager.common.data.PlayerData;
import me.confuser.banmanager.common.ormlite.dao.BaseDaoImpl;
import me.confuser.banmanager.common.ormlite.dao.CloseableIterator;
import me.confuser.banmanager.common.ormlite.stmt.DeleteBuilder;
import me.confuser.banmanager.common.ormlite.stmt.QueryBuilder;
import me.confuser.banmanager.common.ormlite.stmt.Where;
import me.confuser.banmanager.common.ormlite.support.ConnectionSource;
import me.confuser.banmanager.common.ormlite.table.DatabaseTableConfig;
import me.confuser.banmanager.common.ormlite.table.TableUtils;
import me.confuser.banmanager.common.util.DateUtils;

import java.sql.SQLException;

public class PlayerABanRecordStorage extends BaseDaoImpl<PlayerABanRecord, Integer> {

  public PlayerABanRecordStorage(BanManagerPlugin plugin) throws SQLException {
    super(plugin.getLocalConn(), (DatabaseTableConfig<PlayerABanRecord>) plugin.getConfig()
        .getLocalDb()
        .getTable("playerABanRecords"));

    if (!this.isTableExists()) {
      TableUtils.createTable(connectionSource, tableConfig);
      return;
    } else {
      // Attempt to add new columns
      try {
        executeRawNoArgs("ALTER TABLE " + tableConfig.getTableName() + " ADD COLUMN `createdReason` VARCHAR(255)");
      } catch (SQLException e) {
      }
      try {
        executeRawNoArgs("ALTER TABLE " + tableConfig.getTableName() + " ADD COLUMN `silent` TINYINT(1)");
      } catch (SQLException e) {
      }

      try {
        executeRawNoArgs("ALTER TABLE " + tableConfig.getTableName()
          + " CHANGE `created` `created` BIGINT UNSIGNED,"
          + " CHANGE `pastCreated` `pastCreated` BIGINT UNSIGNED,"
          + " CHANGE `expired` `expired` BIGINT UNSIGNED"
        );
      } catch (SQLException e) {
      }
    }
  }

  public PlayerABanRecordStorage(ConnectionSource connection, DatabaseTableConfig<?> table) throws SQLException {
    super(connection, (DatabaseTableConfig<PlayerABanRecord>) table);
  }

  public void addRecord(PlayerABanData ban, PlayerData actor, String reason) throws SQLException {
    create(new PlayerABanRecord(ban, actor, reason));
  }

  public CloseableIterator<PlayerABanRecord> findUnbans(long fromTime) throws SQLException {
    if (fromTime == 0) {
      return iterator();
    }

    long checkTime = fromTime + DateUtils.getTimeDiff();

    QueryBuilder<PlayerABanRecord, Integer> query = queryBuilder();
    Where<PlayerABanRecord, Integer> where = query.where();

    where.ge("created", checkTime);

    query.setWhere(where);


    return query.iterator();

  }

  public long getCount(PlayerData player) throws SQLException {
    return queryBuilder().where().eq("player_id", player).countOf();
  }

  public CloseableIterator<PlayerABanRecord> getRecords(PlayerData player) throws SQLException {
    return queryBuilder().where().eq("player_id", player).iterator();
  }

  public void purge(CleanUp cleanup) throws SQLException {
    if (cleanup.getDays() == 0) return;

    updateRaw("DELETE FROM " + getTableInfo().getTableName() + " WHERE created < UNIX_TIMESTAMP(CURRENT_TIMESTAMP - INTERVAL '" + cleanup.getDays() + "' DAY)");
  }

  public int deleteAll(PlayerData player) throws SQLException {
    DeleteBuilder<PlayerABanRecord, Integer> builder = deleteBuilder();

    builder.where().eq("player_id", player);

    return builder.delete();
  }
}
