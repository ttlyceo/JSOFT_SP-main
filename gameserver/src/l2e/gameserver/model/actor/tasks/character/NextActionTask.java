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
package l2e.gameserver.model.actor.tasks.character;

import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.actor.Player;

public final class NextActionTask implements Runnable
{
	private final Player _player;
	private final GameObject _target;
	
	public NextActionTask(Player player, GameObject target)
	{
		_player = player;
		_target = target;
	}

	@Override
	public void run()
	{
		if (_player != null)
		{
			final var target = _player.getTarget();
			if (_target == null || target == null || _player.isCastingNow())
			{
				return;
			}
			
			if ((target.isCreature()) && (target != _player) && (target == _target) && _target.isAutoAttackable(_player, false))
			{
				_player.getAI().setIntention(CtrlIntention.ATTACK, _target);
			}
		}
	}
}