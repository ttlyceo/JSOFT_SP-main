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
package l2e.gameserver.model.entity.clanhall;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Calendar;

import l2e.commons.dbutils.DbUtils;
import l2e.commons.util.Util;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.model.Clan;
import l2e.gameserver.model.SiegeClan;
import l2e.gameserver.model.SiegeClan.SiegeClanType;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.DoorInstance;
import l2e.gameserver.model.entity.ClanHall;
import l2e.gameserver.model.stats.StatsSet;
import l2e.gameserver.model.zone.type.SiegableHallZone;
import l2e.gameserver.model.zone.type.SiegeZone;
import l2e.gameserver.network.serverpackets.CastleSiegeInfo;

public final class SiegableHall extends ClanHall
{
	private static final String SQL_SAVE = "UPDATE siegable_clanhall SET ownerId=?, nextSiege=? WHERE id=?";

	private final int _grade;
	private Calendar _nextSiege;
	private final long _siegeLength;
	private final int[] _scheduleConfig =
	{
	        7, 0, 0, 12, 0
	};
	
	private SiegeStatus _status = SiegeStatus.REGISTERING;
	private SiegeZone _siegeZone;
	private ClanHallSiegeEngine _siege;

	public SiegableHall(StatsSet set)
	{
		super(set);

		_grade = set.getInteger("grade");
		_siegeLength = set.getLong("siegeLenght");

		if (getOwnerId() != 0)
		{
			loadFunctions();
		}

		final String[] rawSchConfig = set.getString("scheduleConfig").split(";");
		if (rawSchConfig.length == 5)
		{
			for (int i = 0; i < 5; i++)
			{
				try
				{
					_scheduleConfig[i] = Integer.parseInt(rawSchConfig[i]);
				}
				catch (final Exception e)
				{
					_log.warn("SiegableHall - " + Util.clanHallName(null, getId()) + ": Wrong schedule_config parameters!");
				}
			}
		}
		else
		{
			_log.warn(Util.clanHallName(null, getId()) + ": Wrong schedule_config value in siegable_halls table, using default (7 days)");
		}
		_nextSiege = Calendar.getInstance();
		final long nextSiege = set.getLong("nextSiege");
		if ((nextSiege - System.currentTimeMillis()) < 0)
		{
			updateNextSiege();
		}
		else
		{
			_nextSiege.setTimeInMillis(nextSiege);
		}
	}

	public void spawnDoor()
	{
		spawnDoor(false);
	}

	public void spawnDoor(boolean isDoorWeak)
	{
		for (final DoorInstance door : getDoors())
		{
			if (door.isDead())
			{
				door.doRevive();
				if (isDoorWeak)
				{
					door.setCurrentHp(door.getMaxHp() / 2);
				}
				else
				{
					door.setCurrentHp(door.getMaxHp());
				}
			}

			if (door.getOpen())
			{
				door.closeMe();
			}
		}
	}

	@Override
	public final void updateDb()
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(SQL_SAVE);
			statement.setInt(1, getOwnerId());
			statement.setLong(2, getNextSiegeTime());
			statement.setInt(3, getId());
			statement.execute();
		}
		catch (final Exception e)
		{
			_log.warn("Exception: SiegableHall.updateDb(): " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	public final void setSiege(final ClanHallSiegeEngine siegable)
	{
		_siege = siegable;
		_siegeZone.setSiegeInstance(siegable);
	}

	public final ClanHallSiegeEngine getSiege()
	{
		return _siege;
	}

	public final Calendar getSiegeDate()
	{
		return _nextSiege;
	}

	public final long getNextSiegeTime()
	{
		return _nextSiege.getTimeInMillis();
	}

	public long getSiegeLenght()
	{
		return _siegeLength;
	}

	public final void setNextSiegeDate(long date)
	{
		_nextSiege.setTimeInMillis(date);
	}

	public final void setNextSiegeDate(final Calendar c)
	{
		_nextSiege = c;
	}

	public final void updateNextSiege()
	{
		final Calendar c = Calendar.getInstance();
		c.add(Calendar.DAY_OF_YEAR, _scheduleConfig[0]);
		c.add(Calendar.MONTH, _scheduleConfig[1]);
		c.add(Calendar.YEAR, _scheduleConfig[2]);
		c.set(Calendar.HOUR_OF_DAY, _scheduleConfig[3]);
		c.set(Calendar.MINUTE, _scheduleConfig[4]);
		c.set(Calendar.SECOND, 0);
		setNextSiegeDate(c);
		updateDb();
	}

	public final void addAttacker(final Clan clan)
	{
		if (getSiege() != null)
		{
			getSiege().getAttackers().put(clan.getId(), new SiegeClan(clan.getId(), SiegeClanType.ATTACKER));
		}
	}

	public final void removeAttacker(final Clan clan)
	{
		if (getSiege() != null)
		{
			getSiege().getAttackers().remove(clan.getId());
		}
	}

	public final boolean isRegistered(Clan clan)
	{
		if (getSiege() == null)
		{
			return false;
		}

		return getSiege().checkIsAttacker(clan);
	}

	public SiegeStatus getSiegeStatus()
	{
		return _status;
	}

	public final boolean isRegistering()
	{
		return _status == SiegeStatus.REGISTERING;
	}

	public final boolean isInSiege()
	{
		return _status == SiegeStatus.RUNNING;
	}

	public final boolean isWaitingBattle()
	{
		return _status == SiegeStatus.WAITING_BATTLE;
	}

	public final void updateSiegeStatus(SiegeStatus status)
	{
		_status = status;
	}

	public final SiegeZone getSiegeZone()
	{
		return _siegeZone;
	}

	public final void setSiegeZone(SiegeZone zone)
	{
		_siegeZone = zone;
	}

	public final void updateSiegeZone(boolean active)
	{
		_siegeZone.setIsActive(active);
	}

	public final void showSiegeInfo(Player player)
	{
		player.sendPacket(new CastleSiegeInfo(this));
	}

	@Override
	public final int getGrade()
	{
		return _grade;
	}

	@Override
	public final boolean isSiegableHall()
	{
		return true;
	}

	@Override
	public SiegableHallZone getZone()
	{
		return (SiegableHallZone) super.getZone();
	}
}