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

import l2e.gameserver.model.actor.Player;
import l2e.gameserver.network.serverpackets.ExNevitAdventTimeChange;

public class AdventTask extends AbstractPlayerTask
{
	private final long _delay;
	private boolean _isSingleUse = false;
	
	public AdventTask(long delay)
	{
		_delay = delay;
	}
	
	@Override
	public boolean getTask(Player player)
	{
		if (player != null)
		{
			player.getNevitSystem().changeTime(30);
			final var time = player.getNevitSystem().getTime();
			if (time <= 0)
			{
				_isSingleUse = true;
				player.getNevitSystem().setTime(0);
				player.getNevitSystem().stopAdventTask(false);
			}
			else
			{
				player.getNevitSystem().addPoints(72);
				if ((time % 60) == 0)
				{
					player.sendPacket(new ExNevitAdventTimeChange(true, time));
				}
			}
		}
		return true;
	}
	
	@Override
	public int getId()
	{
		return 28;
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