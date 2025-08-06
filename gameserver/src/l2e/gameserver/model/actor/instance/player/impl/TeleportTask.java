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
package l2e.gameserver.model.actor.instance.player.impl;

import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.TeleportWhereType;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.entity.Reflection;

public class TeleportTask extends AbstractPlayerTask
{
	private final long _delay;
	private final Location _loc;
	private final Reflection _ref;

	public TeleportTask(long delay, Location loc)
	{
		_delay = delay;
		_loc = loc;
		_ref = null;
	}
	
	public TeleportTask(long delay, Location loc, Reflection ref)
	{
		_delay = delay;
		_loc = loc;
		_ref = ref;
	}
	
	@Override
	public boolean getTask(Player player)
	{
		if ((player != null) && player.isOnline())
		{
			if (_loc != null)
			{
				player.teleToLocation(_loc, true, _ref != null ? _ref : player.getReflection());
			}
			else
			{
				player.teleToLocation(TeleportWhereType.TOWN, true, ReflectionManager.DEFAULT);
			}
		}
		return true;
	}
	
	@Override
	public int getId()
	{
		return 21;
	}
	
	@Override
	public boolean isOneUse()
	{
		return true;
	}
	
	@Override
	public boolean isSingleUse()
	{
		return true;
	}
	
	@Override
	public long getInterval()
	{
		return System.currentTimeMillis() + _delay;
	}
}