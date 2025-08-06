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
package l2e.gameserver.model.skills.conditions;

import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.stats.Env;

public class ConditionPlayerBaseStats extends Condition
{
	private final BaseStat _stat;
	private final int _value;
	
	public ConditionPlayerBaseStats(Creature player, BaseStat stat, int value)
	{
		super();
		_stat = stat;
		_value = value;
	}

	@Override
	public boolean testImpl(Env env)
	{
		if (env.getPlayer() == null)
		{
			return false;
		}
		final Player player = env.getPlayer();
		switch (_stat)
		{
			case Int :
				return player.getINT() >= _value;
			case Str :
				return player.getSTR() >= _value;
			case Con :
				return player.getCON() >= _value;
			case Dex :
				return player.getDEX() >= _value;
			case Men :
				return player.getMEN() >= _value;
			case Wit :
				return player.getWIT() >= _value;
		}
		return false;
	}
}

enum BaseStat
{
	Int, Str, Con, Dex, Men, Wit
}
