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

import l2e.gameserver.Config;
import l2e.gameserver.data.holder.CharSummonHolder;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.stats.Env;
import l2e.gameserver.network.SystemMessageId;

public class ConditionPlayerCanSummon extends Condition
{
	private final boolean _val;
	
	public ConditionPlayerCanSummon(boolean val)
	{
		_val = val;
	}
	
	@Override
	public boolean testImpl(Env env)
	{
		final Player player = env.getPlayer();
		if (player == null)
		{
			return false;
		}
		
		boolean canSummon = true;
		if (Config.RESTORE_SERVITOR_ON_RECONNECT && CharSummonHolder.getInstance().getServitors().containsKey(player.getObjectId()))
		{
			player.sendPacket(SystemMessageId.SUMMON_ONLY_ONE);
			canSummon = false;
		}
		else if (Config.RESTORE_PET_ON_RECONNECT && CharSummonHolder.getInstance().getPets().containsKey(player.getObjectId()))
		{
			player.sendPacket(SystemMessageId.SUMMON_ONLY_ONE);
			canSummon = false;
		}
		else if (player.hasSummon())
		{
			player.sendPacket(SystemMessageId.SUMMON_ONLY_ONE);
			canSummon = false;
		}
		else if (player.isFlyingMounted() || player.isMounted())
		{
			canSummon = false;
		}
		return (_val == canSummon);
	}
}