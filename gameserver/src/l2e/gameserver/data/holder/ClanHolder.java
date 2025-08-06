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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import l2e.commons.dbutils.DbUtils;
import l2e.commons.log.LoggerObject;
import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.idfactory.IdFactory;
import l2e.gameserver.instancemanager.AuctionManager;
import l2e.gameserver.instancemanager.CHSiegeManager;
import l2e.gameserver.instancemanager.CastleManager;
import l2e.gameserver.instancemanager.FortManager;
import l2e.gameserver.instancemanager.FortSiegeManager;
import l2e.gameserver.instancemanager.SiegeManager;
import l2e.gameserver.model.Clan;
import l2e.gameserver.model.ClanMember;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.CreatePledge;
import l2e.gameserver.network.serverpackets.SystemMessage;
import l2e.gameserver.network.serverpackets.pledge.PledgeShowInfoUpdate;
import l2e.gameserver.network.serverpackets.pledge.PledgeShowMemberListAll;
import l2e.gameserver.network.serverpackets.pledge.PledgeShowMemberListUpdate;

public class ClanHolder extends LoggerObject
{
	private final Map<Integer, Clan> _clans = new HashMap<>();
	
	public Clan[] getClans()
	{
		return _clans.values().toArray(new Clan[_clans.size()]);
	}
	
	protected ClanHolder()
	{
		Clan clan;
		var clanCount = 0;
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT clan_id FROM clan_data");
			rset = statement.executeQuery();
			while (rset.next())
			{
				final var clanId = rset.getInt("clan_id");
				_clans.put(clanId, new Clan(clanId));
				clan = getClan(clanId);
				if (clan.getDissolvingExpiryTime() != 0)
				{
					scheduleRemoveClan(clan.getId());
				}
				clanCount++;
			}
		}
		catch (final Exception e)
		{
			warn("Error restoring ClanHolder.", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
		info("Restored " + clanCount + " clans from the database.");
		allianceCheck();
		restorewars();
	}
	
	public Clan getClan(int clanId)
	{
		return _clans.get(clanId);
	}
	
	public int getClanId(int playerId)
	{
		var res = 0;
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT clanid FROM `characters` WHERE charId=" + playerId + " LIMIT 1;");
			rset = statement.executeQuery();
			if (rset.next())
			{
				res = rset.getInt(1);
			}
		}
		catch (final SQLException er)
		{
			warn("Error check player clanId", er);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
		return res;
	}
	
	public Clan getClanByName(String clanName)
	{
		for (final var clan : getClans())
		{
			if (clan.getName().equalsIgnoreCase(clanName))
			{
				return clan;
			}
			
		}
		return null;
	}
	
	public Clan createClan(Player player, String clanName)
	{
		if (null == player)
		{
			return null;
		}
		
		if (Config.DEBUG)
		{
			info("" + player.getObjectId() + "(" + player.getName(null) + ") requested a clan creation.");
		}
		
		if (10 > player.getLevel())
		{
			player.sendPacket(SystemMessageId.YOU_DO_NOT_MEET_CRITERIA_IN_ORDER_TO_CREATE_A_CLAN);
			return null;
		}
		if (0 != player.getClanId())
		{
			player.broadcastPacket(CreatePledge.STATIC);
			return null;
		}
		if (System.currentTimeMillis() < player.getClanCreateExpiryTime())
		{
			player.sendPacket(SystemMessageId.YOU_MUST_WAIT_XX_DAYS_BEFORE_CREATING_A_NEW_CLAN);
			return null;
		}
		if (!Util.isAlphaNumeric(clanName) || (2 > clanName.length()))
		{
			player.sendPacket(SystemMessageId.CLAN_NAME_INCORRECT);
			return null;
		}
		if (16 < clanName.length())
		{
			player.sendPacket(SystemMessageId.CLAN_NAME_TOO_LONG);
			return null;
		}
		
		if (null != getClanByName(clanName))
		{
			SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_ALREADY_EXISTS);
			sm.addString(clanName);
			player.sendPacket(sm);
			sm = null;
			return null;
		}
		
		final var clan = new Clan(IdFactory.getInstance().getNextId(), clanName);
		final var leader = new ClanMember(clan, player);
		clan.setLeader(leader);
		leader.setPlayerInstance(player);
		clan.store();
		player.setClan(clan);
		player.setPledgeClass(ClanMember.calculatePledgeClass(player));
		player.setClanPrivileges(Clan.CP_ALL);
		if (Config.ALT_CLAN_DEFAULT_LEVEL > 1)
		{
			clan.changeLevel(Config.ALT_CLAN_DEFAULT_LEVEL, false);
		}
		_clans.put(Integer.valueOf(clan.getId()), clan);
		
		player.sendPacket(new PledgeShowInfoUpdate(clan));
		player.sendPacket(new PledgeShowMemberListAll(clan, player));
		player.sendUserInfo();
		player.sendPacket(new PledgeShowMemberListUpdate(player));
		player.sendPacket(SystemMessageId.CLAN_CREATED);
		
		return clan;
	}
	
	public synchronized void destroyClan(int clanId)
	{
		final var clan = getClan(clanId);
		if (clan == null)
		{
			return;
		}
		
		clan.broadcastToOnlineMembers(SystemMessage.getSystemMessage(SystemMessageId.CLAN_HAS_DISPERSED));
		final var castleId = clan.getCastleId();
		if (castleId == 0)
		{
			for (final var siege : SiegeManager.getInstance().getSieges())
			{
				siege.removeSiegeClan(clan);
			}
		}
		final var fortId = clan.getFortId();
		if (fortId == 0)
		{
			for (final var siege : FortSiegeManager.getInstance().getSieges())
			{
				siege.removeSiegeClan(clan);
			}
		}
		final int hallId = clan.getHideoutId();
		if (hallId == 0)
		{
			for (final var hall : CHSiegeManager.getInstance().getConquerableHalls().values())
			{
				hall.removeAttacker(clan);
			}
		}
		
		final var auction = AuctionManager.getInstance().getAuction(clan.getAuctionBiddedAt());
		if (auction != null)
		{
			auction.cancelBid(clan.getId());
		}
		
		final var leaderMember = clan.getLeader();
		if (leaderMember == null)
		{
			clan.getWarehouse().destroyAllItems("ClanRemove", null, null);
		}
		else
		{
			clan.getWarehouse().destroyAllItems("ClanRemove", clan.getLeader().getPlayerInstance(), null);
		}
		
		for (final var member : clan.getMembers())
		{
			clan.removeClanMember(member.getObjectId(), 0);
		}
		
		_clans.remove(clanId);
		IdFactory.getInstance().releaseId(clanId);
		
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("DELETE FROM clan_data WHERE clan_id=?");
			statement.setInt(1, clanId);
			statement.execute();
			statement.close();
			
			statement = con.prepareStatement("DELETE FROM clan_privs WHERE clan_id=?");
			statement.setInt(1, clanId);
			statement.execute();
			statement.close();
			
			statement = con.prepareStatement("DELETE FROM clan_skills WHERE clan_id=?");
			statement.setInt(1, clanId);
			statement.execute();
			statement.close();
			
			statement = con.prepareStatement("DELETE FROM clan_subpledges WHERE clan_id=?");
			statement.setInt(1, clanId);
			statement.execute();
			statement.close();
			
			statement = con.prepareStatement("DELETE FROM clan_wars WHERE clan1=? OR clan2=?");
			statement.setInt(1, clanId);
			statement.setInt(2, clanId);
			statement.execute();
			statement.close();
			
			statement = con.prepareStatement("DELETE FROM clan_notices WHERE clan_id=?");
			statement.setInt(1, clanId);
			statement.execute();
			statement.close();
			
			if (castleId != 0)
			{
				final var castle = CastleManager.getInstance().getCastleById(castleId);
				if (castle != null)
				{
					castle.getTerritory().changeOwner(null);
				}
				statement = con.prepareStatement("UPDATE castle SET taxPercent = 0 WHERE id = ?");
				statement.setInt(1, castleId);
				statement.execute();
				statement.close();
			}
			if (fortId != 0)
			{
				final var fort = FortManager.getInstance().getFortById(fortId);
				if (fort != null)
				{
					final var owner = fort.getOwnerClan();
					if (clan == owner)
					{
						fort.removeOwner(true);
					}
				}
			}
			if (hallId != 0)
			{
				final var hall = CHSiegeManager.getInstance().getSiegableHall(hallId);
				if ((hall != null) && (hall.getOwnerId() == clanId))
				{
					hall.free();
				}
			}
		}
		catch (final Exception e)
		{
			warn("Error removing clan from DB.", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
	
	public void scheduleRemoveClan(final int clanId)
	{
		ThreadPoolManager.getInstance().schedule(new Runnable()
		{
			@Override
			public void run()
			{
				if (getClan(clanId) == null)
				{
					return;
				}
				if (getClan(clanId).getDissolvingExpiryTime() != 0)
				{
					destroyClan(clanId);
				}
			}
		}, Math.max(getClan(clanId).getDissolvingExpiryTime() - System.currentTimeMillis(), 300000));
	}
	
	public boolean isAllyExists(String allyName)
	{
		for (final var clan : getClans())
		{
			if ((clan.getAllyName() != null) && clan.getAllyName().equalsIgnoreCase(allyName))
			{
				return true;
			}
		}
		return false;
	}
	
	public void storeclanswars(int clanId1, int clanId2)
	{
		final var clan1 = ClanHolder.getInstance().getClan(clanId1);
		final var clan2 = ClanHolder.getInstance().getClan(clanId2);
		
		clan1.setEnemyClan(clan2);
		clan2.setAttackerClan(clan1);
		clan1.broadcastClanStatus();
		clan2.broadcastClanStatus();
		
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("REPLACE INTO clan_wars (clan1, clan2, wantspeace1, wantspeace2) VALUES(?,?,?,?)");
			statement.setInt(1, clanId1);
			statement.setInt(2, clanId2);
			statement.setInt(3, 0);
			statement.setInt(4, 0);
			statement.execute();
		}
		catch (final Exception e)
		{
			warn("Error storing clan wars data.", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
		
		SystemMessage msg = SystemMessage.getSystemMessage(SystemMessageId.CLAN_WAR_DECLARED_AGAINST_S1_IF_KILLED_LOSE_LOW_EXP);
		msg.addString(clan2.getName());
		clan1.broadcastToOnlineMembers(msg);
		msg = SystemMessage.getSystemMessage(SystemMessageId.CLAN_S1_DECLARED_WAR);
		msg.addString(clan1.getName());
		clan2.broadcastToOnlineMembers(msg);
	}
	
	public void deleteclanswars(int clanId1, int clanId2)
	{
		final var clan1 = ClanHolder.getInstance().getClan(clanId1);
		final var clan2 = ClanHolder.getInstance().getClan(clanId2);
		
		clan1.deleteEnemyClan(clan2);
		clan2.deleteAttackerClan(clan1);
		clan1.broadcastClanStatus();
		clan2.broadcastClanStatus();
		
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("DELETE FROM clan_wars WHERE clan1=? AND clan2=?");
			statement.setInt(1, clanId1);
			statement.setInt(2, clanId2);
			statement.execute();
		}
		catch (final Exception e)
		{
			warn("Error removing clan wars data.", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
		
		SystemMessage msg = SystemMessage.getSystemMessage(SystemMessageId.WAR_AGAINST_S1_HAS_STOPPED);
		msg.addString(clan2.getName());
		clan1.broadcastToOnlineMembers(msg);
		msg = SystemMessage.getSystemMessage(SystemMessageId.CLAN_S1_HAS_DECIDED_TO_STOP);
		msg.addString(clan1.getName());
		clan2.broadcastToOnlineMembers(msg);
	}
	
	public void checkSurrender(Clan clan1, Clan clan2)
	{
		var count = 0;
		for (final var player : clan1.getMembers())
		{
			if ((player != null) && (player.getPlayerInstance().getWantsPeace() == 1))
			{
				count++;
			}
		}
		if (count == (clan1.getMembers().length - 1))
		{
			clan1.deleteEnemyClan(clan2);
			clan2.deleteEnemyClan(clan1);
			deleteclanswars(clan1.getId(), clan2.getId());
		}
	}
	
	private void restorewars()
	{
		Clan clan1, clan2;
		
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT clan1, clan2 FROM clan_wars");
			rset = statement.executeQuery();
			while (rset.next())
			{
				clan1 = getClan(rset.getInt("clan1"));
				clan2 = getClan(rset.getInt("clan2"));
				if ((clan1 != null) && (clan2 != null))
				{
					clan1.setEnemyClan(rset.getInt("clan2"));
					clan2.setAttackerClan(rset.getInt("clan1"));
				}
				else
				{
					warn("restorewars one of clans is null clan1:" + clan1 + " clan2:" + clan2);
				}
			}
		}
		catch (final Exception e)
		{
			warn("Error restoring clan wars data.", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
	}
	
	private void allianceCheck()
	{
		for (final var clan : _clans.values())
		{
			final var allyId = clan.getAllyId();
			if ((allyId != 0) && (clan.getId() != allyId))
			{
				if (!_clans.containsKey(allyId))
				{
					clan.setAllyId(0);
					clan.setAllyName(null);
					clan.changeAllyCrest(0, true);
					clan.updateClanInDB();
					info("Removed alliance from clan: " + clan);
				}
			}
		}
	}
	
	public List<Clan> getClanAllies(int allianceId)
	{
		final List<Clan> clanAllies = new ArrayList<>();
		if (allianceId != 0)
		{
			for (final var clan : _clans.values())
			{
				if ((clan != null) && (clan.getAllyId() == allianceId))
				{
					clanAllies.add(clan);
				}
			}
		}
		return clanAllies;
	}
	
	public void storeClanScore()
	{
		for (final var clan : _clans.values())
		{
			clan.updateClanScoreInDB();
		}
	}
	
	public static ClanHolder getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final ClanHolder _instance = new ClanHolder();
	}
}