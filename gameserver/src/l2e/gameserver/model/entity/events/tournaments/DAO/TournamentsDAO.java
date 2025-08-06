package l2e.gameserver.model.entity.events.tournaments.DAO;

import l2e.commons.dbutils.DbUtils;
import l2e.commons.log.LoggerObject;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.entity.events.tournaments.TournamentData;
import l2e.gameserver.model.entity.events.tournaments.model.TournamentStats;
import l2e.gameserver.model.entity.events.tournaments.enums.TournamentStatsType;
import l2e.gameserver.model.entity.events.tournaments.util.TournamentUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

/**
 * @author psygrammator
 */
public class TournamentsDAO extends LoggerObject
{
    private static final String RESTORE_DATA = "SELECT * FROM tournament_player_data WHERE obj_id=?";
    private static final String CHECK_RESTORE_DATA = "SELECT * FROM tournament_player_data WHERE obj_id=? AND fight_type=?";
    private static final String UPDATE_BY_TYPE = "UPDATE tournament_player_data SET fights_done=?, victories=?, defeats=?, ties=?, kills=?, damage=?, wdt=?, dpf=? WHERE obj_id=? AND fight_type=?";
    private static final String INSERT_BY_TYPE = "INSERT INTO tournament_player_data (obj_id, fight_type, fights_done, victories, defeats, ties, kills, damage, wdt, dpf) VALUES (?,?,?,?,?,?,?,?,?,?)";

    public void loadTournamentData(Player player)
    {
        Map<Integer, Map<TournamentStatsType, Integer>> tournamentStats = new HashMap<>();
        TournamentUtil.TOURNAMENT_TYPES.forEach(i -> tournamentStats.put(i, new HashMap<>(Map.of(TournamentStatsType.VICTORIES, 0, TournamentStatsType.DEFEATS, 0, TournamentStatsType.TIES, 0, TournamentStatsType.KILLS, 0, TournamentStatsType.DAMAGE, 0))));
        Connection con = null;
        PreparedStatement preparedStatement = null;
        try
        {
            con = DatabaseFactory.getInstance().getConnection();
            preparedStatement = con.prepareStatement(RESTORE_DATA);
            preparedStatement.setInt(1, player.getObjectId());
            ResultSet rs = preparedStatement.executeQuery();
            while (rs.next())
            {
                tournamentStats.put(TournamentUtil.getIntNameType(rs.getString("fight_type")), new HashMap<>(Map.of(TournamentStatsType.VICTORIES, rs.getInt("victories"), TournamentStatsType.DEFEATS, rs.getInt("defeats"), TournamentStatsType.TIES, rs.getInt("ties"), TournamentStatsType.KILLS, rs.getInt("kills"), TournamentStatsType.DAMAGE, rs.getInt("damage"))));
            }
            player.setTournamentStats(new TournamentStats(tournamentStats));
        }
        catch (Exception e)
        {
            error("", e);
        }
        finally
        {
            DbUtils.closeQuietly(con, preparedStatement);
        }
    }

    public TournamentStats loadTournamentStatsFromTarget(int charId)
    {
        Player player = GameObjectsStorage.getPlayer(charId);
        if(player != null)
        {
            return player.getTournamentStats();
        }
        else
        {
            Connection con = null;
            PreparedStatement preparedStatement = null;
            try
            {
                con = DatabaseFactory.getInstance().getConnection();
                preparedStatement = con.prepareStatement(RESTORE_DATA);
                preparedStatement.setInt(1, charId);
                ResultSet rs = preparedStatement.executeQuery();
                Map<Integer, Map<TournamentStatsType, Integer>> tournamentStats = new HashMap<>();
                while (rs.next())
                {
                    tournamentStats.put(TournamentUtil.getIntNameType(rs.getString("fight_type")), new HashMap<>(Map.of(TournamentStatsType.VICTORIES, rs.getInt("victories"), TournamentStatsType.DEFEATS, rs.getInt("defeats"), TournamentStatsType.TIES, rs.getInt("ties"), TournamentStatsType.KILLS, rs.getInt("kills"), TournamentStatsType.DAMAGE, rs.getInt("damage"))));
                }

                return new TournamentStats(tournamentStats);
            }
            catch (Exception e)
            {
                error("", e);
            }
            finally
            {
                DbUtils.closeQuietly(con, preparedStatement);
            }
        }
        return null;
    }

    public void storeData(Player player, int type)
    {
        TournamentStats tournamentStats = player.getTournamentStats();
        if(tournamentStats == null)
        {
            error("Tournament Status is null: player " + player.getName(null));
            return;
        }

        Connection con = null;
        PreparedStatement preparedStatement = null;
        ResultSet rs = null;
        try
        {
            con = DatabaseFactory.getInstance().getConnection();
            preparedStatement = con.prepareStatement(CHECK_RESTORE_DATA);
            preparedStatement.setInt(1, player.getObjectId());
            preparedStatement.setString(2, TournamentUtil.getStringNameType(type));
            rs = preparedStatement.executeQuery();
            boolean hasResult = rs.next();
            if (hasResult)
            {
                preparedStatement = con.prepareStatement(UPDATE_BY_TYPE);
                preparedStatement.setInt(1, tournamentStats.getTotalTournamentFightsDone());
                preparedStatement.setInt(2, tournamentStats.getTournamentStats(type, TournamentStatsType.VICTORIES));
                preparedStatement.setInt(3, tournamentStats.getTournamentStats(type, TournamentStatsType.DEFEATS));
                preparedStatement.setInt(4, tournamentStats.getTournamentStats(type, TournamentStatsType.TIES));
                preparedStatement.setInt(5, tournamentStats.getTournamentStats(type, TournamentStatsType.KILLS));
                preparedStatement.setInt(6, tournamentStats.getTournamentStats(type, TournamentStatsType.DAMAGE));
                preparedStatement.setString(7, TournamentData.getInstance().getWinDefeatTie(player, type));
                preparedStatement.setString(8, TournamentData.getInstance().getDamagePerFight(tournamentStats, type));
                preparedStatement.setInt(9, player.getObjectId());
                preparedStatement.setString(10, TournamentUtil.getStringNameType(type));
                preparedStatement.execute();
            }
            else
            {
                preparedStatement = con.prepareStatement(INSERT_BY_TYPE);
                preparedStatement.setInt(1, player.getObjectId());
                preparedStatement.setString(2, TournamentUtil.getStringNameType(type));
                preparedStatement.setInt(3, tournamentStats.getTotalTournamentFightsDone());
                preparedStatement.setInt(4, tournamentStats.getTournamentStats(type, TournamentStatsType.VICTORIES));
                preparedStatement.setInt(5, tournamentStats.getTournamentStats(type, TournamentStatsType.DEFEATS));
                preparedStatement.setInt(6, tournamentStats.getTournamentStats(type, TournamentStatsType.TIES));
                preparedStatement.setInt(7, tournamentStats.getTournamentStats(type, TournamentStatsType.KILLS));
                preparedStatement.setInt(8, tournamentStats.getTournamentStats(type, TournamentStatsType.DAMAGE));
                preparedStatement.setString(9, TournamentData.getInstance().getWinDefeatTie(player, type));
                preparedStatement.setString(10, TournamentData.getInstance().getDamagePerFight(tournamentStats, type));
                preparedStatement.execute();
            }
        }
        catch (Exception e)
        {
            error("", e);
        }
        finally {
            DbUtils.closeQuietly(con, preparedStatement, rs);
        }
    }

    public static TournamentsDAO getInstance()
    {
        return SingletonHolder._instance;
    }

    private static class SingletonHolder
    {
        protected static final TournamentsDAO _instance = new TournamentsDAO();
    }
}
