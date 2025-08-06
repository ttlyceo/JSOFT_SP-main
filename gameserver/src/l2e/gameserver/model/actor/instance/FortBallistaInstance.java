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
package l2e.gameserver.model.actor.instance;

import l2e.gameserver.Config;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.network.SystemMessageId;

public class FortBallistaInstance extends Npc
{
	public FortBallistaInstance(int objectId, NpcTemplate template)
	{
		super(objectId, template);
		setInstanceType(InstanceType.FortBallistaInstance);
	}
	
	@Override
	public boolean isAutoAttackable(Creature attacker, boolean isPoleAttack)
	{
		return true;
	}
	
	@Override
	public boolean canBeAttacked()
	{
		return false;
	}
	
	@Override
	protected void onDeath(Creature killer)
	{
		super.onDeath(killer);
		if (getFort().getSiege().getIsInProgress())
		{
			if (killer != null && killer.isPlayer())
			{
				final Player player = ((Player) killer);
				if (player.getClan() != null && player.getClan().getLevel() >= 5)
				{
					player.getClan().addReputationScore(Config.BALLISTA_POINTS, true);
					player.sendPacket(SystemMessageId.BALLISTA_DESTROYED_CLAN_REPU_INCREASED);
				}
			}
		}
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
			if (isAutoAttackable(player, false) && !isAlikeDead())
			{
				if (Math.abs(player.getZ() - getZ()) < 600)
				{
					player.getAI().setIntention(CtrlIntention.ATTACK, this);
				}
			}

			if (!canInteract(player))
			{
				player.getAI().setIntention(CtrlIntention.INTERACT, this);
			}
		}
		player.sendActionFailed();
	}
	
	@Override
	public boolean hasRandomAnimation()
	{
		return false;
	}
}