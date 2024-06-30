package me.confuser.banmanager.common.storage;

import lombok.Getter;
import me.confuser.banmanager.common.BanManagerPlugin;
import me.confuser.banmanager.common.data.PlayerData;
import me.confuser.banmanager.common.ipaddr.IPAddress;
import me.confuser.banmanager.common.ormlite.dao.BaseDaoImpl;
import me.confuser.banmanager.common.ormlite.dao.CloseableIterator;
import me.confuser.banmanager.common.ormlite.stmt.QueryBuilder;
import me.confuser.banmanager.common.ormlite.stmt.SelectArg;
import me.confuser.banmanager.common.ormlite.stmt.Where;
import me.confuser.banmanager.common.ormlite.support.ConnectionSource;
import me.confuser.banmanager.common.ormlite.table.DatabaseTableConfig;
import me.confuser.banmanager.common.ormlite.table.TableUtils;
import me.confuser.banmanager.common.util.UUIDProfile;
import me.confuser.banmanager.common.util.UUIDUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DuplicatePlayerStorage extends BaseDaoImpl<PlayerData, byte[]> {

    @Getter
    private BanManagerPlugin plugin;

    public DuplicatePlayerStorage(BanManagerPlugin plugin) throws SQLException {
        super(plugin.getLocalConn(), (DatabaseTableConfig<PlayerData>) plugin.getConfig().getLocalDb()
                .getTable("duplicatePlayers"));

        this.plugin = plugin;

        if (!isTableExists()) {
            TableUtils.createTable(connectionSource, tableConfig);
        }
    }

    public DuplicatePlayerStorage(ConnectionSource connection, DatabaseTableConfig<?> table) throws SQLException {
        super(connection, (DatabaseTableConfig<PlayerData>) table);
    }

    public List<PlayerData> retrieve(String name) {
        try {
            return queryForEq("name", new SelectArg(name));
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public List<PlayerData> getDuplicatesInTime(IPAddress ip, long timeDiff) {
        ArrayList<PlayerData> players = new ArrayList<>();

        if (plugin.getConfig().getBypassPlayerIps().contains(ip.toString())) {
            return players;
        }

        QueryBuilder<PlayerData, byte[]> query = queryBuilder();
        try {
            query.leftJoin(plugin.getPlayerBanStorage().queryBuilder());

            Where<PlayerData, byte[]> where = query.where();

            where.eq("ip", ip);

            if (timeDiff != 0) {
                long currentTime = System.currentTimeMillis() / 1000L;

                where.and().ge("lastSeen", (currentTime - timeDiff));
            }

            query.setWhere(where);
        } catch (SQLException e) {
            e.printStackTrace();
            return players;
        }

        CloseableIterator<PlayerData> itr = null;
        try {
            itr = query.limit(300L).iterator();

            while (itr.hasNext()) {
                PlayerData player = itr.next();

                if (!plugin.getExemptionsConfig().isExempt(player, "alts")) {
                    players.add(player);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (itr != null) itr.closeQuietly();
        }

        return players;
    }
}
