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

import java.util.List;

import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.skills.effects.Effect;

public class BuffsBackTask extends AbstractPlayerTask
{
	private final long _delay;
	private final List<Effect> _effects;

	public BuffsBackTask(long delay, List<Effect> effects)
	{
		_delay = delay;
		_effects = effects;
	}
	
	@Override
	public boolean getTask(Player player)
	{
		if (player != null && !player.isInOlympiadMode())
		{
			if (_effects != null && !_effects.isEmpty())
			{
				_effects.stream().filter(i -> i != null).forEach(e -> e.scheduleEffect(true));
			}
		}
		return true;
	}
	
	@Override
	public int getId()
	{
		return 16;
	}
	
	@Override
	public boolean isOneUse()
	{
		return false;
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