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
package l2e.gameserver.model.zone.type;


import l2e.gameserver.GameServer;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.player.impl.TeleportTask;
import l2e.gameserver.model.zone.ZoneId;
import l2e.gameserver.model.zone.ZoneType;

public class NoRestartZone extends ZoneType
{
	private int _restartAllowedTime = 0;
	private int _restartTime = 0;
	
	public NoRestartZone(int id)
	{
		super(id);
		addZoneId(ZoneId.NO_RESTART);
	}

	@Override
	public void setParameter(String name, String value)
	{
		if (name.equalsIgnoreCase("restartAllowedTime"))
		{
			_restartAllowedTime = Integer.parseInt(value) * 1000;
		}
		else if (name.equalsIgnoreCase("restartTime"))
		{
			_restartTime = Integer.parseInt(value) * 1000;
		}
		else if (name.equalsIgnoreCase("instanceId"))
		{}
		else
		{
			super.setParameter(name, value);
		}
	}

	@Override
	protected void onEnter(Creature character)
	{
	}
	
	@Override
	protected void onExit(Creature character)
	{
	}
	
	@Override
	public void onPlayerLoginInside(Player player)
	{
		if (!isEnabled() || player.isInFightEvent() || player.checkInTournament())
		{
			return;
		}

		if (((System.currentTimeMillis() - player.getLastAccess()) > getRestartTime()) && ((System.currentTimeMillis() - GameServer.dateTimeServerStarted.getTimeInMillis()) > getRestartAllowedTime()))
		{
			player.getPersonalTasks().addTask(new TeleportTask(2000, null));
		}
	}

	public int getRestartAllowedTime()
	{
		return _restartAllowedTime;
	}

	public void setRestartAllowedTime(int time)
	{
		_restartAllowedTime = time;
	}

	public int getRestartTime()
	{
		return _restartTime;
	}

	public void setRestartTime(int time)
	{
		_restartTime = time;
	}
}