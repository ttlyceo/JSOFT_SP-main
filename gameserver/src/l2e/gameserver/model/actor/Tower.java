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
package l2e.gameserver.model.actor;

import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.geodata.GeoEngine;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;

public abstract class Tower extends Npc
{
	public Tower(int objectId, NpcTemplate template)
	{
		super(objectId, template);
		setIsInvul(false);
	}
	
	@Override
	public boolean canBeAttacked()
	{
		return ((getCastle() != null) && (getCastle().getId() > 0) && getCastle().getSiege().getIsInProgress());
	}
	
	@Override
	public boolean isAutoAttackable(Creature attacker, boolean isPoleAttack)
	{
		return ((attacker != null) && attacker.isPlayer() && (getCastle() != null) && (getCastle().getId() > 0) && getCastle().getSiege().getIsInProgress() && getCastle().getSiege().checkIsAttacker(((Player) attacker).getClan()));
	}
	
	@Override
	public void onAction(Player player, boolean interact, boolean shift)
	{
		if (!canTarget(player))
		{
			return;
		}
		
		if (this != player.getTarget())
		{
			player.setTarget(this);
		}
		else if (interact)
		{
			if (isAutoAttackable(player, false) && (Math.abs(player.getZ() - getZ()) < 100) && GeoEngine.getInstance().canSeeTarget(player, this))
			{
				player.getAI().setIntention(CtrlIntention.ATTACK, this);
			}
		}
		player.sendActionFailed();
	}
	
	@Override
	public void onForcedAttack(Player player, boolean shift)
	{
		onAction(player, false, shift);
	}
}