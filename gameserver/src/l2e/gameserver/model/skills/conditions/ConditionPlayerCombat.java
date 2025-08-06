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

import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.stats.Env;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.taskmanager.AttackStanceTaskManager;

public class ConditionPlayerCombat extends Condition
{
	private final boolean _val;

	public ConditionPlayerCombat(boolean val)
	{
		_val = val;
	}

	@Override
	public boolean testImpl(Env env)
	{
		boolean canCallPlayer = true;
		final Player player = env.getPlayer();
		if (player == null)
		{
			canCallPlayer = false;
		}
		if (AttackStanceTaskManager.getInstance().hasAttackStanceTask(player))
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_SUMMON_IN_COMBAT);
			canCallPlayer = false;
		}
		return (_val == canCallPlayer);
	}
}