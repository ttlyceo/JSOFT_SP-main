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

import l2e.gameserver.SevenSigns;
import l2e.gameserver.instancemanager.CastleManager;
import l2e.gameserver.instancemanager.FortManager;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.entity.Castle;
import l2e.gameserver.model.entity.Fort;
import l2e.gameserver.model.stats.Env;
import l2e.gameserver.network.SystemMessageId;

public class ConditionPlayerCanSummonSiegeGolem extends Condition
{
	private final boolean _val;
	
	public ConditionPlayerCanSummonSiegeGolem(boolean val)
	{
		_val = val;
	}
	
	@Override
	public boolean testImpl(Env env)
	{
		boolean canSummonSiegeGolem = true;
		if ((env.getPlayer() == null) || env.getPlayer().isAlikeDead() || env.getPlayer().isCursedWeaponEquipped() || (env.getPlayer().getClan() == null))
		{
			canSummonSiegeGolem = false;
		}
		
		final Castle castle = CastleManager.getInstance().getCastle(env.getPlayer());
		final Fort fort = FortManager.getInstance().getFort(env.getPlayer());
		if ((castle == null) && (fort == null))
		{
			canSummonSiegeGolem = false;
		}
		
		final Player player = env.getPlayer().getActingPlayer();
		if (((fort != null) && (fort.getId() == 0)) || ((castle != null) && (castle.getId() == 0)))
		{
			player.sendPacket(SystemMessageId.INCORRECT_TARGET);
			canSummonSiegeGolem = false;
		}
		else if (((castle != null) && !castle.getSiege().getIsInProgress()) || ((fort != null) && !fort.getSiege().getIsInProgress()))
		{
			player.sendPacket(SystemMessageId.INCORRECT_TARGET);
			canSummonSiegeGolem = false;
		}
		else if ((player.getClanId() != 0) && (((castle != null) && (castle.getSiege().getAttackerClan(player.getClanId()) == null)) || ((fort != null) && (fort.getSiege().getAttackerClan(player.getClanId()) == null))))
		{
			player.sendPacket(SystemMessageId.INCORRECT_TARGET);
			canSummonSiegeGolem = false;
		}
		else if ((SevenSigns.getInstance().checkSummonConditions(env.getPlayer())))
		{
			canSummonSiegeGolem = false;
		}
		return (_val == canSummonSiegeGolem);
	}
}