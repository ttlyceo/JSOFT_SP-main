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
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.SystemMessage;

public class WaterTask extends AbstractPlayerTask
{
	private final long _delay;
	private int _status = 0;
	private boolean _isSingleUse = false;

	public WaterTask(long delay)
	{
		_delay = delay;
	}
	
	@Override
	public boolean getTask(Player player)
	{
		_status++;
		if (player != null)
		{
			if (player.isDead())
			{
				_isSingleUse = true;
			}
			else
			{
				double reduceHp = player.getMaxHp() / 100.0;
				if (reduceHp < 1)
				{
					reduceHp = 1;
				}
				player.reduceCurrentHp(reduceHp, player, false, false, null);
				final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.DROWN_DAMAGE_S1);
				sm.addNumber((int) reduceHp);
				player.sendPacket(sm);
			}
		}
		return true;
	}
	
	@Override
	public int getId()
	{
		return 32;
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
		return System.currentTimeMillis() + (_status > 0 ? 1000L : _delay);
	}
}