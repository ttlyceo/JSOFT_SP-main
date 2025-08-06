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

import java.util.List;

import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.base.PlayerState;
import l2e.gameserver.model.stats.Env;

public class ConditionPlayerState extends Condition
{
	private final PlayerState _check;
	private final boolean _required;
	private final List<Integer> _classIds;
	
	public ConditionPlayerState(PlayerState check, boolean required, List<Integer> classId)
	{
		_check = check;
		_required = required;
		_classIds = classId;
	}

	@Override
	public boolean testImpl(Env env)
	{
		final Creature character = env.getCharacter();
		final Player player = env.getPlayer();
		switch (_check)
		{
			case RESTING :
				if (player != null)
				{
					return (player.isSitting() == _required);
				}
				return !_required;
			case MOVING :
				return character.isMoving() && !character.isRunning() == _required;
			case RUNNING :
				return (character.isMoving() && character.isRunning()) == _required;
			case STANDING :
				if (player != null)
				{
					return (_required != (player.isSitting() || player.isMoving()));
				}
				return (_required != character.isMoving());
			case FLYING :
				return (character.isFlying() == _required);
			case BEHIND :
				return (character.isBehindTarget() == _required);
			case FRONT :
				return (character.isInFrontOfTarget() == _required);
			case CHAOTIC :
				if (player != null)
				{
					return ((player.getKarma() > 0) == _required);
				}
				return !_required;
			case OLYMPIAD :
				if (player != null)
				{
					var found = false;
					if (!_classIds.isEmpty())
					{
						found = env.getPlayer().isSubClassActive() ? _classIds.contains(env.getPlayer().getActiveClass()) : _classIds.contains(env.getPlayer().getClassId().getId());
						return found ? (player.isInOlympiadMode() == _required) : !_required;
					}
					return player.isInOlympiadMode() == _required;
				}
				return !_required;
		}
		return !_required;
	}
}