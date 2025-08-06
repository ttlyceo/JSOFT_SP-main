/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 *
 */
package l2e.gameserver.data.holder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;

import l2e.commons.dao.JdbcEntityState;
import l2e.commons.dbutils.DbUtils;
import l2e.commons.log.LoggerObject;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.model.entity.mods.votereward.VoteRewardRecord;

public class VoteRewardHolder extends LoggerObject
{
	private static final String SELECT_SQL_QUERY = "SELECT * FROM votereward_records WHERE site=?";
	private static final String INSERT_SQL_QUERY = "INSERT INTO votereward_records (site, identifier, votes, lastvotedate) VALUES (?,?,?,?)";
	private static final String UPDATE_SQL_QUERY = "UPDATE votereward_records SET votes=?, lastvotedate=? WHERE site=? AND identifier=?";

	public void restore(Map<String, VoteRewardRecord> records, String site)
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(SELECT_SQL_QUERY);
			statement.setString(1, site);
			rset = statement.executeQuery();
			while (rset.next())
			{
				final String identifier = rset.getString("identifier");
				final int votes = rset.getInt("votes");
				final int lastvotedate = rset.getInt("lastvotedate");
				records.put(identifier, new VoteRewardRecord(site, identifier, votes, lastvotedate));
			}
		}
		catch (final Exception e)
		{
			warn("select(String):" + e, e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
	}

	public void save(VoteRewardRecord voteRewardRecord)
	{
		if (!voteRewardRecord.getJdbcState().isSavable())
		{
			return;
		}

		voteRewardRecord.setJdbcState(JdbcEntityState.STORED);
		save0(voteRewardRecord);
	}

	private void save0(VoteRewardRecord voteRewardRecord)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(INSERT_SQL_QUERY);
			statement.setString(1, voteRewardRecord.getSite());
			statement.setString(2, voteRewardRecord.getIdentifier());
			statement.setInt(3, voteRewardRecord.getVotes());
			statement.setInt(4, voteRewardRecord.getLastVoteTime());
			statement.executeUpdate();
		}
		catch (final Exception e)
		{
			warn("save0(VoteRewardRecord):" + e, e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	public void update(VoteRewardRecord voteRewardRecord)
	{
		if (!voteRewardRecord.getJdbcState().isUpdatable())
		{
			return;
		}
		voteRewardRecord.setJdbcState(JdbcEntityState.STORED);
		update0(voteRewardRecord);
	}

	private void update0(VoteRewardRecord voteRewardRecord)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(UPDATE_SQL_QUERY);
			statement.setInt(1, voteRewardRecord.getVotes());
			statement.setInt(2, voteRewardRecord.getLastVoteTime());
			statement.setString(3, voteRewardRecord.getSite());
			statement.setString(4, voteRewardRecord.getIdentifier());

			statement.execute();
		}
		catch (final Exception e)
		{
			warn("update0(VoteRewardRecord):" + e, e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
	
	public static VoteRewardHolder getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final VoteRewardHolder _instance = new VoteRewardHolder();
	}
}