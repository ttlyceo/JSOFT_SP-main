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

import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.entity.events.model.impl.FFATreasureHuntEvent;
import l2e.gameserver.model.skills.Skill;

public class EventTreasureChestInstance extends Npc
{
	public EventTreasureChestInstance(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public void onAction(Player player, boolean interact, boolean shift)
	{
		if (!canTarget(player))
		{
			return;
		}

		if (!isInRange(player, INTERACTION_DISTANCE))
		{
			if (player.getAI().getIntention() != CtrlIntention.INTERACT)
			{
				player.getAI().setIntention(CtrlIntention.INTERACT, this, null);
			}
			return;
		}
		
		if (this != player.getTarget())
		{
			player.setTarget(this);
		}
		else if (interact)
		{
			if (!canInteract(player))
			{
				player.getAI().setIntention(CtrlIntention.INTERACT, this);
			}
			else
			{
				if (player.isInFightEvent())
				{
					boolean shouldDisappear = false;
					if (player.getFightEvent() instanceof FFATreasureHuntEvent)
					{
						shouldDisappear = ((FFATreasureHuntEvent) player.getFightEvent()).openTreasure(player, this);
					}
					
					if (shouldDisappear)
					{
						deleteMe();
					}
				}
			}
		}
	}

	@Override
	public void onForcedAttack(Player player, boolean shift)
	{
		onAction(player, false, shift);
	}
	
	@Override
	public void reduceCurrentHp(double damage, Creature attacker, boolean awake, boolean isDOT, Skill skill)
	{
		return;
	}
}
