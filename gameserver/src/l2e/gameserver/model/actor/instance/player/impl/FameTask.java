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

import l2e.gameserver.Config;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.SystemMessage;

public class FameTask extends AbstractPlayerTask
{
	private final long _delay;
	private final int _value;

	public FameTask(long delay, int value)
	{
		_delay = delay;
		_value = value;
	}
	
	@Override
	public boolean getTask(Player player)
	{
		if ((player == null) || (player.isDead() && !Config.FAME_FOR_DEAD_PLAYERS) || player.inObserverMode())
		{
			return false;
		}
		if (((player.getClient() == null) || player.getClient().isDetached()) && !Config.OFFLINE_FAME)
		{
			return false;
		}
		
		final int value = (int) (_value * player.getPremiumBonus().getFameBonus());
		player.setFame(player.getFame() + value);
		final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.ACQUIRED_S1_REPUTATION_SCORE);
		sm.addNumber(value);
		player.sendPacket(sm);
		player.sendUserInfo();
		return true;
	}
	
	@Override
	public int getId()
	{
		return 1;
	}
	
	@Override
	public boolean isOneUse()
	{
		return true;
	}
	
	@Override
	public boolean isSingleUse()
	{
		return false;
	}
	
	@Override
	public long getInterval()
	{
		return System.currentTimeMillis() + _delay;
	}
}