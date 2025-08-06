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
package l2e.scripts.ai.hellbound;

import l2e.gameserver.ai.model.CtrlEvent;
import l2e.gameserver.ai.npc.Fighter;
import l2e.gameserver.data.parser.DoorParser;
import l2e.gameserver.instancemanager.HellboundManager;
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.instance.DoorInstance;

/**
 * Created by LordWinter 19.09.2018
 */
public class OutpostCaptain extends Fighter
{
	public OutpostCaptain(Attackable actor)
	{
		super(actor);
	}

	@Override
	protected void onEvtAttacked(Creature attacker, int damage)
	{
		final Attackable actor = getActiveChar();

		if ((attacker == null) || (attacker.getActingPlayer() == null))
		{
			return;
		}

		for (final Npc minion : World.getInstance().getAroundNpc(actor, 3000, 200))
		{
			if ((minion.getId() == 22358) || (minion.getId() == 22357))
			{
				minion.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, attacker, 5000);
			}
		}
	}

	@Override
	protected void onEvtDead(Creature killer)
	{
		if (HellboundManager.getInstance().getLevel() == 8)
		{
			HellboundManager.getInstance().setLevel(9);
		}
		super.onEvtDead(killer);
	}

	@Override
	protected void onEvtSpawn()
	{
		final Attackable actor = getActiveChar();

		actor.setIsNoRndWalk(true);
		final DoorInstance door = DoorParser.getInstance().getDoor(20250001);
		if (door != null)
		{
			door.closeMe();
		}
		super.onEvtSpawn();
	}
}
