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

import l2e.gameserver.model.MountType;
import l2e.gameserver.model.TeleportWhereType;
import l2e.gameserver.model.actor.Player;

public class RentPetTask extends AbstractPlayerTask
{
	private final long _delay;
	private boolean _isSingleUse = false;

	public RentPetTask(long delay)
	{
		_delay = delay;
	}
	
	@Override
	public boolean getTask(Player player)
	{
		if (player != null)
		{
			if (player.checkLandingState() && (player.getMountType() == MountType.WYVERN))
			{
				player.teleToLocation(TeleportWhereType.TOWN, true, player.getReflection());
			}
			
			if (player.dismount())
			{
				_isSingleUse = true;
			}
		}
		return true;
	}
	
	@Override
	public int getId()
	{
		return 12;
	}
	
	@Override
	public boolean isOneUse()
	{
		return true;
	}
	
	@Override
	public boolean isSingleUse()
	{
		return _isSingleUse;
	}
	
	@Override
	public long getInterval()
	{
		return System.currentTimeMillis() + _delay;
	}
}