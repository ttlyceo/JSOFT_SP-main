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
package l2e.gameserver.instancemanager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

import l2e.commons.dbutils.DbUtils;
import l2e.commons.log.LoggerObject;
import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.model.Clan;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.entity.clanhall.ClanHallSiegeEngine;
import l2e.gameserver.model.entity.clanhall.SiegableHall;
import l2e.gameserver.model.stats.StatsSet;
import l2e.gameserver.model.zone.type.ClanHallZone;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.SystemMessage;

public final class CHSiegeManager extends LoggerObject
{
	private static final String SQL_LOAD_HALLS = "SELECT * FROM siegable_clanhall";
	
	private final Map<Integer, SiegableHall> _siegableHalls = new HashMap<>();

	protected CHSiegeManager()
	{
		loadClanHalls();
	}
	
	private final void loadClanHalls()
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(SQL_LOAD_HALLS);
			rs = statement.executeQuery();

			_siegableHalls.clear();

			while (rs.next())
			{
				final int id = rs.getInt("id");

				final StatsSet set = new StatsSet();

				set.set("id", id);
				set.set("ownerId", rs.getInt("ownerId"));
				set.set("grade", rs.getInt("Grade"));
				set.set("nextSiege", rs.getLong("nextSiege"));
				set.set("siegeLenght", rs.getLong("siegeLenght"));
				set.set("scheduleConfig", rs.getString("schedule_config"));
				final SiegableHall hall = new SiegableHall(set);
				_siegableHalls.put(id, hall);
				ClanHallManager.getInstance().addClanHall(hall);
			}
			info("Loaded " + _siegableHalls.size() + " siege clan halls.");
		}
		catch (final Exception e)
		{
			warn("Could not load siegable clan halls!:");
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rs);
		}
	}

	public Map<Integer, SiegableHall> getConquerableHalls()
	{
		return _siegableHalls;
	}
	
	public SiegableHall getSiegableHall(int clanHall)
	{
		return getConquerableHalls().get(clanHall);
	}

	public final SiegableHall getNearbyClanHall(Creature activeChar)
	{
		return getNearbyClanHall(activeChar.getX(), activeChar.getY(), 10000);
	}

	public final SiegableHall getNearbyClanHall(int x, int y, int maxDist)
	{
		ClanHallZone zone = null;

		for (final Map.Entry<Integer, SiegableHall> ch : _siegableHalls.entrySet())
		{
			zone = ch.getValue().getZone();
			if ((zone != null) && (zone.getDistanceToZone(x, y) < maxDist))
			{
				return ch.getValue();
			}
		}
		return null;
	}

	public final ClanHallSiegeEngine getSiege(Creature character)
	{
		final SiegableHall hall = getNearbyClanHall(character);
		if (hall == null)
		{
			return null;
		}
		return hall.getSiege();
	}

	public final void registerClan(Clan clan, SiegableHall hall, Player player)
	{
		if (clan.getLevel() < Config.CHS_CLAN_MINLEVEL)
		{
			player.sendMessage("Only clans of level " + Config.CHS_CLAN_MINLEVEL + " or higher may register for a castle siege");
		}
		else if (hall.isWaitingBattle())
		{
			final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.DEADLINE_FOR_SIEGE_S1_PASSED);
			sm.addString(Util.clanHallName(player, hall.getId()));
			player.sendPacket(sm);
		}
		else if (hall.isInSiege())
		{
			player.sendPacket(SystemMessageId.NOT_SIEGE_REGISTRATION_TIME2);
		}
		else if (hall.getOwnerId() == clan.getId())
		{
			player.sendPacket(SystemMessageId.CLAN_THAT_OWNS_CASTLE_IS_AUTOMATICALLY_REGISTERED_DEFENDING);
		}
		else if (clan.getHideoutId() != 0)
		{
			player.sendPacket(SystemMessageId.CLAN_THAT_OWNS_CASTLE_CANNOT_PARTICIPATE_OTHER_SIEGE);
		}
		else if (hall.getSiege().checkIsAttacker(clan))
		{
			player.sendPacket(SystemMessageId.ALREADY_REQUESTED_SIEGE_BATTLE);
		}
		else if (isClanParticipating(clan))
		{
			player.sendPacket(SystemMessageId.APPLICATION_DENIED_BECAUSE_ALREADY_SUBMITTED_A_REQUEST_FOR_ANOTHER_SIEGE_BATTLE);
		}
		else if (hall.getSiege().getAttackers().size() >= Config.CHS_MAX_ATTACKERS)
		{
			player.sendPacket(SystemMessageId.ATTACKER_SIDE_FULL);
		}
		else
		{
			hall.addAttacker(clan);
		}
	}

	public final void unRegisterClan(Clan clan, SiegableHall hall)
	{
		if (!hall.isRegistering())
		{
			return;
		}
		hall.removeAttacker(clan);
	}

	public final boolean isClanParticipating(Clan clan)
	{
		for (final SiegableHall hall : getConquerableHalls().values())
		{
			if ((hall.getSiege() != null) && hall.getSiege().checkIsAttacker(clan))
			{
				return true;
			}
		}
		return false;
	}

	public final void onServerShutDown()
	{
		for (final SiegableHall hall : getConquerableHalls().values())
		{
			if (hall.getSiege() == null)
			{
				continue;
			}
			hall.getSiege().saveAttackers();
		}
	}

	public static CHSiegeManager getInstance()
	{
		return SingletonHolder._instance;
	}

	private static class SingletonHolder
	{
		protected static final CHSiegeManager _instance = new CHSiegeManager();
	}
}