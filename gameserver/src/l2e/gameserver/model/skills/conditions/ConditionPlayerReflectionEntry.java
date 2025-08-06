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


import l2e.gameserver.data.parser.ReflectionParser;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.player.CharacterVariable;
import l2e.gameserver.model.stats.Env;

public class ConditionPlayerReflectionEntry extends Condition
{
	private final int _type;
	private final int _attempts;
	
	public ConditionPlayerReflectionEntry(int type, int attempts)
	{
		_type = type;
		_attempts = attempts;
	}
	
	@Override
	public boolean testImpl(Env env)
	{
		final Player player = env.getPlayer();
		if (player != null)
		{
			final CharacterVariable var = player.getVariable("reflectionEntry_" + _type + "");
			if (var != null)
			{
				if ((Integer.parseInt(var.getValue()) < _attempts) && !var.isExpired())
				{
					return player.getReflectionId() == 0 && canResetReflection(player);
				}
				return var.isExpired() && player.getReflectionId() == 0 && canResetReflection(player);
			}
			else
			{
				return player.getReflectionId() == 0 && canResetReflection(player);
			}
		}
		return false;
	}
	
	private boolean canResetReflection(Player player)
	{
		for (final int i : ReflectionParser.getInstance().getSharedReuseInstanceIdsByGroup(_type))
		{
			if (System.currentTimeMillis() < ReflectionManager.getInstance().getReflectionTime(player, i))
			{
				return true;
			}
		}
		return false;
	}
}