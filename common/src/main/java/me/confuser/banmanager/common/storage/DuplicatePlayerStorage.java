package me.confuser.banmanager.common.storage;

import lombok.Getter;
import me.confuser.banmanager.common.BanManagerPlugin;
import me.confuser.banmanager.common.data.DuplicatePlayerData;
import me.confuser.banmanager.common.data.PlayerData;
import me.confuser.banmanager.common.ipaddr.IPAddress;
import me.confuser.banmanager.common.ormlite.dao.BaseDaoImpl;
import me.confuser.banmanager.common.ormlite.dao.CloseableIterator;
import me.confuser.banmanager.common.ormlite.field.DataType;
import me.confuser.banmanager.common.ormlite.stmt.SelectArg;
import me.confuser.banmanager.common.ormlite.support.ConnectionSource;
import me.confuser.banmanager.common.ormlite.table.DatabaseTableConfig;
import me.confuser.banmanager.common.ormlite.table.TableUtils;
import me.confuser.banmanager.common.util.UUIDUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DuplicatePlayerStorage extends BaseDaoImpl<DuplicatePlayerData, byte[]> {

    @Getter
    private BanManagerPlugin plugin;

    public DuplicatePlayerStorage(BanManagerPlugin plugin) throws SQLException {
        super(plugin.getLocalConn(), (DatabaseTableConfig<DuplicatePlayerData>) plugin.getConfig().getLocalDb()
                .getTable("duplicatePlayers"));

        this.plugin = plugin;

        if (!isTableExists()) {
            TableUtils.createTable(connectionSource, tableConfig);
        }
    }

    public DuplicatePlayerStorage(ConnectionSource connection, DatabaseTableConfig<?> table) throws SQLException {
        super(connection, (DatabaseTableConfig<DuplicatePlayerData>) table);
    }

    public List<DuplicatePlayerData> retrieve(String name) {
        try {
            return queryForEq("name", new SelectArg(name));
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public List<PlayerData> getDuplicatesInTime(IPAddress address, long timeDiff) {
        List<PlayerData> players = new ArrayList<>();

        if (plugin.getConfig().getBypassPlayerIps().contains(address.toString())) {
            return players;
        }

        try {
            CloseableIterator<Object[]> itr = null;
            try {
                String playerTableName = getTableName();
                String banTableName = plugin.getPlayerBanStorage().getTableName();
                itr = queryRaw("SELECT *\n" +
                        "FROM " + playerTableName + " \n" +
                        "LEFT JOIN " + banTableName + " ON " + playerTableName + ".id = " + banTableName + ".player_id\n" +
                        "WHERE " + playerTableName + ".ip = UNHEX('" + address.toHexString(false) + "') \n" +
                        "LIMIT 300;", new DataType[]{DataType.BYTE_ARRAY, DataType.STRING, DataType.BYTE_ARRAY, DataType.LONG}).closeableIterator();

                while (itr.hasNext()) {
                    Object[] data = itr.next();
                    PlayerData player = new PlayerData(UUIDUtils.fromBytes((byte[]) data[0]), (String) data[1], address, (Long) data[3]);

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
}
