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

import l2e.gameserver.instancemanager.EpicBossManager;
import l2e.gameserver.model.PcCondOverride;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.entity.events.cleft.AerialCleftEvent;
import l2e.gameserver.model.stats.Env;

public class ConditionPlayerCanEscape extends Condition
{
	private final boolean _val;
	
	public ConditionPlayerCanEscape(boolean val)
	{
		_val = val;
	}
	
	@Override
	public boolean testImpl(Env env)
	{
		boolean canTeleport = true;
		final Player player = env.getPlayer();
		if (player == null)
		{
			canTeleport = false;
		}
		else if (player.getFightEvent() != null && !player.getFightEvent().canUseEscape(player))
		{
			canTeleport = false;
		}
		else if (player.isInPartyTournament() && !player.getPartyTournament().canUseEscape(player))
		{
			canTeleport = false;
		}
		else if (!AerialCleftEvent.getInstance().onEscapeUse(player.getObjectId()))
		{
			canTeleport = false;
		}
		else if (player.isInDuel())
		{
			canTeleport = false;
		}
		else if (player.isAfraid())
		{
			canTeleport = false;
		}
		else if (player.isCombatFlagEquipped())
		{
			canTeleport = false;
		}
		else if (player.isFlying() || player.isFlyingMounted())
		{
			canTeleport = false;
		}
		else if (player.isInOlympiadMode())
		{
			canTeleport = false;
		}
		else if ((EpicBossManager.getInstance().getZone(player) != null) && !EpicBossManager.getInstance().getZone(player).isCanTeleport())
		{
			canTeleport = false;
		}
		else if ((EpicBossManager.getInstance().getZone(player) != null) && player.isGM() && player.canOverrideCond(PcCondOverride.SKILL_CONDITIONS))
		{
			canTeleport = true;
		}
		return (_val == canTeleport);
	}
}