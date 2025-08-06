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
package l2e.gameserver.model.service.academy;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2e.commons.dbutils.DbUtils;
import l2e.commons.util.Functions;
import l2e.gameserver.Config;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.data.holder.CharNameHolder;
import l2e.gameserver.data.holder.ClanHolder;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.instancemanager.CastleManager;
import l2e.gameserver.instancemanager.ClanHallManager;
import l2e.gameserver.instancemanager.FortManager;
import l2e.gameserver.model.Clan;
import l2e.gameserver.model.ClanMember;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.Request;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.strings.server.ServerMessage;
import l2e.gameserver.model.strings.server.ServerStorage;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.CreatureSay;
import l2e.gameserver.network.serverpackets.JoinPledge;
import l2e.gameserver.network.serverpackets.SystemMessage;
import l2e.gameserver.network.serverpackets.pledge.PledgeShowInfoUpdate;
import l2e.gameserver.network.serverpackets.pledge.PledgeShowMemberListAdd;
import l2e.gameserver.network.serverpackets.pledge.PledgeShowMemberListAll;

public class AcademyList
{
	protected static final Logger _log = LoggerFactory.getLogger(AcademyList.class);
	
	private static List<Player> _academyList = new ArrayList<>();
	
	public static void addToAcademy(Player player)
	{
		_academyList.add(player);
	}
	
	public static void deleteFromAcdemyList(Player player)
	{
		if (_academyList.contains(player))
		{
			_academyList.remove(player);
		}
	}
	
	public static List<Player> getAcademyList()
	{
		return _academyList;
	}
	
	public static boolean isInAcademyList(Player player)
	{
		for (final Player plr : _academyList)
		{
			if (plr == null)
			{
				continue;
			}
			
			if (plr.getName(null).equalsIgnoreCase(player.getName(null)))
			{
				return true;
			}
		}
		return false;
	}
	
	public static void inviteInAcademy(Player activeChar, Player academyChar)
	{
		if (activeChar == null)
		{
			academyChar.sendPacket(SystemMessageId.TARGET_IS_NOT_FOUND_IN_THE_GAME);
			academyChar.sendActionFailed();
			return;
		}
		
		final Request request = activeChar.getRequest();
		if (request == null)
		{
			return;
		}
		
		if (!request.isProcessingRequest())
		{
			request.onRequestResponse();
			academyChar.sendActionFailed();
			return;
		}
		
		if(activeChar.isOutOfControl())
		{
			request.onRequestResponse();
			activeChar.sendActionFailed();
			return;
		}
		
		final Clan clan = activeChar.getClan();
		if (clan == null)
		{
			request.onRequestResponse();
			academyChar.sendActionFailed();
			return;
		}
		
		if (!clan.checkClanJoinCondition(activeChar, academyChar, -1))
		{
			return;
		}
		
		if (clan.getAvailablePledgeTypes(-1) != 0)
		{
			activeChar.sendPacket(new CreatureSay(activeChar.getObjectId(), Say2.BATTLEFIELD, ServerStorage.getInstance().getString(activeChar.getLang(), "CommunityAcademy.ACADEMY"), ServerStorage.getInstance().getString(activeChar.getLang(), "CommunityAcademy.MSG15")));
			academyChar.sendPacket(new CreatureSay(academyChar.getObjectId(), Say2.BATTLEFIELD, ServerStorage.getInstance().getString(academyChar.getLang(), "CommunityAcademy.ACADEMY"), ServerStorage.getInstance().getString(academyChar.getLang(), "CommunityAcademy.MSG16")));
			return;
		}
		
		try
		{
			if (academyChar.getPledgePrice() != 0)
			{
				final int itemId = academyChar.getPledgeItemId();
				final long price = academyChar.getPledgePrice();
				
				if (!activeChar.destroyItemByItemId("Academy Recruitment", itemId, price, activeChar, true))
				{
					final ServerMessage msg = new ServerMessage("CommunityAcademy.MSG17", activeChar.getLang());
					msg.add(academyChar.getName(null));
					
					final ServerMessage msg1 = new ServerMessage("CommunityAcademy.MSG18", academyChar.getLang());
					msg1.add(activeChar.getName(null));
					activeChar.sendPacket(new CreatureSay(activeChar.getObjectId(), Say2.BATTLEFIELD, ServerStorage.getInstance().getString(activeChar.getLang(), "CommunityAcademy.ACADEMY"), msg.toString()));
					academyChar.sendPacket(new CreatureSay(academyChar.getObjectId(), Say2.BATTLEFIELD, ServerStorage.getInstance().getString(academyChar.getLang(), "CommunityAcademy.ACADEMY"), msg1.toString()));
					return;
				}
				registerAcademy(activeChar.getClan(), academyChar, itemId, price);
				academyChar.sendPacket(new JoinPledge(activeChar.getClanId()));
				
				academyChar.setPledgeType(-1);
				academyChar.setPowerGrade(9);
				academyChar.setLvlJoinedAcademy(academyChar.getLevel());
				clan.addClanMember(academyChar);
				academyChar.setClanPrivileges(academyChar.getClan().getRankPrivs(academyChar.getPowerGrade()));
				academyChar.sendPacket(SystemMessageId.ENTERED_THE_CLAN);
				
				final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_HAS_JOINED_CLAN);
				sm.addString(academyChar.getName(null));
				clan.broadcastToOnlineMembers(sm);
				
				if (academyChar.getClan().getCastleId() > 0)
				{
					CastleManager.getInstance().getCastleByOwner(academyChar.getClan()).giveResidentialSkills(academyChar);
				}
				if (academyChar.getClan().getFortId() > 0)
				{
					FortManager.getInstance().getFortByOwner(academyChar.getClan()).giveResidentialSkills(academyChar);
				}
				if (academyChar.getClan().getHideoutId() > 0)
				{
					final var hall = ClanHallManager.getInstance().getAbstractHallByOwner(academyChar.getClan());
					if (hall != null)
					{
						hall.giveResidentialSkills(academyChar);
					}
				}
				academyChar.sendSkillList(false);
				
				clan.broadcastToOtherOnlineMembers(new PledgeShowMemberListAdd(academyChar), academyChar);
				clan.broadcastToOnlineMembers(new PledgeShowInfoUpdate(clan));
				
				academyChar.sendPacket(new PledgeShowMemberListAll(clan, academyChar));
				academyChar.setClanJoinExpiryTime(0);
				academyChar.broadcastCharInfo();
				deleteFromAcdemyList(academyChar);
			}

		}
		finally
		{
			request.onRequestResponse();
		}
	}
	
	private static void registerAcademy(Clan clan, Player player, int itemId, long price)
	{
		Connection connection = null;
		PreparedStatement statement = null;
		try
		{
			connection = DatabaseFactory.getInstance().getConnection();
			statement = connection.prepareStatement("INSERT INTO character_academy (clanId,charId,itemId,price,time) values(?,?,?,?,?)");
			statement.setInt(1, clan.getId());
			statement.setInt(2, player.getObjectId());
			statement.setInt(3, itemId);
			statement.setLong(4, price);
			statement.setLong(5, (System.currentTimeMillis() + (Config.MAX_TIME_IN_ACADEMY * 6000L)));
			statement.execute();
			deleteFromAcdemyList(player);
			ThreadPoolManager.getInstance().schedule(new CleanUpTask(clan, player.getObjectId()), (Config.MAX_TIME_IN_ACADEMY * 6000L));
		}
		catch (final Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(connection, statement);
		}
	}
	
	public static boolean isAcademyChar(int objId)
	{
		String result = "";
		
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT * FROM `character_academy` WHERE `charId` = ?");
			statement.setInt(1, objId);
			rset = statement.executeQuery();
			if(rset.next())
			{
				result = rset.getString("clanId");
			}
		}
		catch (final Exception e)
		{
			_log.warn("AcademyList: " + e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
		
		return !result.isEmpty();
	}
	
	public static void removeAcademyFromDB(Clan clan, int charId, boolean giveReward, boolean kick)
	{
		if (giveReward)
		{
			Connection con = null;
			PreparedStatement statement = null;
			ResultSet rset = null;
			try
			{
				con = DatabaseFactory.getInstance().getConnection();
				statement = con.prepareStatement("SELECT itemId, price FROM character_academy WHERE charId=?");
				statement.setInt(1, charId);
				rset = statement.executeQuery();
				while (rset.next())
				{
					final String charName = CharNameHolder.getInstance().getNameById(charId);
					final Map<Integer, Long> reward = new HashMap<>();
					reward.put(rset.getInt("itemId"), rset.getLong("price"));
					final Player player = GameObjectsStorage.getPlayer(charId);
					final String lang = player != null ? player.getLang() : Config.MULTILANG_DEFAULT;
					final ServerMessage msg = new ServerMessage("CommunityAcademy.MAIL_DESCR1", lang);
					msg.add(charName);
					Functions.sendSystemMail(charName, charId, ServerStorage.getInstance().getString(lang, "CommunityAcademy.MAIL_TITLE1"), msg.toString(), reward);
				}
			}
			catch (final Exception e)
			{
				_log.warn("AcademyList: Could not select char from character_academy", e);
			}
			finally
			{
				DbUtils.closeQuietly(con, statement, rset);
			}
		}
			
		if (kick)
		{
			Connection con = null;
			PreparedStatement statement = null;
			ResultSet rset = null;
			try
			{
				con = DatabaseFactory.getInstance().getConnection();
				statement = con.prepareStatement("SELECT itemId, price FROM character_academy WHERE clanId=? AND charId=?");
				statement.setInt(1, clan.getId());
				statement.setInt(2, charId);
				rset = statement.executeQuery();
				while (rset.next())
				{
					final String acadCharName = CharNameHolder.getInstance().getNameById(charId);
					final Map<Integer, Long> reward = new HashMap<>();
					reward.put(rset.getInt("itemId"), rset.getLong("price"));
					
					final Player player = GameObjectsStorage.getPlayer(clan.getLeaderId());
					final String lang = player != null ? player.getLang() : Config.MULTILANG_DEFAULT;
					final ServerMessage msg = new ServerMessage("CommunityAcademy.MAIL_DESCR2", lang);
					msg.add(clan.getLeaderName());
					msg.add(acadCharName);
					Functions.sendSystemMail(clan.getLeaderName(), clan.getLeaderId(), ServerStorage.getInstance().getString(lang, "CommunityAcademy.MAIL_TITLE2"), msg.toString(), reward);
					clan.removeClanMember(charId, System.currentTimeMillis() + (Config.ALT_CLAN_JOIN_DAYS * 3600000L));
				}
			}
			catch (final Exception e)
			{
				_log.warn("AcademyList: Could not select char from character_academy", e);
			}
			finally
			{
				DbUtils.closeQuietly(con, statement, rset);
			}
		}
			
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("DELETE FROM character_academy WHERE charId=?");
			statement.setInt(1, charId);
			statement.execute();
		}
		catch (final Exception e)
		{
			_log.warn("AcademyList: Could not delete char from character_academy: " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
	
	public static void restore()
	{
		Connection connection = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		try
		{
			connection = DatabaseFactory.getInstance().getConnection();
			statement = connection.prepareStatement("SELECT clanId,charId,time FROM character_academy");
			rs = statement.executeQuery();
			while (rs.next())
			{
				try
				{
					final int clanId = rs.getInt("clanId");
					final Clan clan = ClanHolder.getInstance().getClan(clanId);
					if (clan != null)
					{
						final int charId = rs.getInt("charId");
						final long date = rs.getLong("time");
						ThreadPoolManager.getInstance().schedule(new CleanUpTask(clan, charId), (date - System.currentTimeMillis()));
					}
				}
				catch (final Exception e)
				{
					e.printStackTrace();
				}
			}
		}
		catch (final Exception e)
		{
			_log.warn("AcademyList: Could not restore clan for academy " + e);
		}
		finally
		{
			DbUtils.closeQuietly(connection, statement, rs);
		}
	}
	
	private static class CleanUpTask implements Runnable
	{
		private final Clan _clan;
		private final int _charId;
		
		public CleanUpTask(Clan clan, int charId)
		{
			_clan = clan;
			_charId = charId;
		}
		
		@Override
		public void run()
		{
			if (_clan == null)
			{
				_log.warn("AcademyList: Clan was null for charID " + _charId);
				return;
			}
			
			final String charName = CharNameHolder.getInstance().getNameById(_charId);
			final ClanMember member = _clan.getClanMember(charName);
			if (member == null)
			{
				return;
			}
			
			if (member.getLevel() < 40 && member.getPledgeType() == -1)
			{
				removeAcademyFromDB(_clan, _charId, false, true);
				_clan.removeClanMember(member.getObjectId(), System.currentTimeMillis() + (Config.ALT_CLAN_JOIN_DAYS * 3600000L));
				final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.CLAN_MEMBER_S1_EXPELLED);
				sm.addString(charName);
				_clan.broadcastToOnlineMembers(sm);
			}
		}
	}
}