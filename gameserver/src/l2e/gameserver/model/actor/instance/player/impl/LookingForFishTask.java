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

import l2e.commons.util.Rnd;
import l2e.gameserver.model.actor.Player;

public class LookingForFishTask extends AbstractPlayerTask
{
	private int _status;
	private final long _delay;
	private final boolean _isNoob, _isUpperGrade;
	private final int _fishGroup;
	private final double _fishGutsCheck;
	private final long _endTaskTime;
	private boolean _isSingleUse = false;

	public LookingForFishTask(long delay, int startCombatTime, double fishGutsCheck, int fishGroup, boolean isNoob, boolean isUpperGrade)
	{
		_status = 0;
		_delay = delay;
		_fishGutsCheck = fishGutsCheck;
		_endTaskTime = System.currentTimeMillis() + (startCombatTime * 1000) + 10000;
		_fishGroup = fishGroup;
		_isNoob = isNoob;
		_isUpperGrade = isUpperGrade;
	}
	
	@Override
	public boolean getTask(Player player)
	{
		_status++;
		if (player != null)
		{
			if (System.currentTimeMillis() >= _endTaskTime)
			{
				player.endFishing(false);
				return false;
			}
			if (_fishGroup == -1)
			{
				return false;
			}
			final int check = Rnd.get(100);
			if (_fishGutsCheck > check)
			{
				_isSingleUse = true;
				player.startFishCombat(_isNoob, _isUpperGrade);
			}
		}
		return true;
	}
	
	@Override
	public int getId()
	{
		return 30;
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
		return System.currentTimeMillis() + (_status > 0 ? _delay : 10000L);
	}
}