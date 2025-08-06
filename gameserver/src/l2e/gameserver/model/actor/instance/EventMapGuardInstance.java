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
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;

public final class EventMapGuardInstance extends GuardInstance
{
	public EventMapGuardInstance(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public boolean isAutoAttackable(Creature attacker, boolean isPoleAttack)
	{
		return false;
	}

	@Override
	public void onAction(Player player, boolean shift)
	{
		if (!canTarget(player))
		{
			return;
		}

		if (getObjectId() != player.getTargetId())
		{
			if (Config.DEBUG)
			{
				_log.info(player.getObjectId() + ": Targetted guard " + getObjectId());
			}

			player.setTarget(this);
		}
		else
		{
			if (containsTarget(player))
			{
				if (Config.DEBUG)
				{
					_log.info(player.getObjectId() + ": Attacked guard " + getObjectId());
				}

				player.getAI().setIntention(CtrlIntention.ATTACK, this);
			}
			else
			{
				if (!canInteract(player))
				{
					player.getAI().setIntention(CtrlIntention.INTERACT, this);
				}
				else
				{
					player.sendMessage("Did you know that you are on the event right now?");
					player.sendActionFailed();
				}
			}
		}
		player.sendActionFailed();
	}
}