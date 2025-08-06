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
import l2e.gameserver.model.skills.effects.AbnormalEffect;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.ExNavitAdventPointInfo;
import l2e.gameserver.network.serverpackets.ExNevitAdventEffect;

public class NevitEffectTask extends AbstractPlayerTask
{
	private final long _delay;
	
	public NevitEffectTask(long delay)
	{
		_delay = delay;
	}
	
	@Override
	public boolean getTask(Player player)
	{
		if (player != null)
		{
			player.sendPacket(new ExNevitAdventEffect(0));
			player.sendPacket(new ExNavitAdventPointInfo(player.getNevitSystem().getPoints()));
			player.sendPacket(SystemMessageId.NEVITS_ADVENT_BLESSING_HAS_ENDED);
			player.stopAbnormalEffect(AbnormalEffect.NAVIT_ADVENT);
			player.unsetVar("nevit");
		}
		return true;
	}
	
	@Override
	public int getId()
	{
		return 29;
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