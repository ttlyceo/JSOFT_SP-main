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
package l2e.scripts.ai;

import l2e.commons.util.Rnd;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.ai.npc.Fighter;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.quest.Quest;

public class AncientFighter extends Fighter
{
	public AncientFighter(Attackable actor)
	{
		super(actor);
	}

	@Override
	protected void onEvtDead(Creature killer)
	{
		Attackable npc = getActiveChar();

		if (Rnd.get(1000) < 2)
		{
			final Attackable box = (Attackable) Quest.addSpawn(18693, npc.getX(), npc.getY(), npc.getZ(), 0, false, 300000L);
			final int x = box.getX();
			final int y = box.getY();

			final Attackable guard1 = (Attackable) Quest.addSpawn(18694, x + 50, y + 50, npc.getZ(), 0, false, 300000L);
			guard1.addDamageHate(killer, 0, 999);
			guard1.getAI().setIntention(CtrlIntention.ATTACK, killer);

			final Attackable guard2 = (Attackable) Quest.addSpawn(18695, x + 50, y - 50, npc.getZ(), 0, false, 300000L);
			guard2.addDamageHate(killer, 0, 999);
			guard2.getAI().setIntention(CtrlIntention.ATTACK, killer);

			final Attackable guard3 = (Attackable) Quest.addSpawn(18695, x - 50, y + 50, npc.getZ(), 0, false, 300000L);
			guard3.addDamageHate(killer, 0, 999);
			guard3.getAI().setIntention(CtrlIntention.ATTACK, killer);

			final Attackable guard4 = (Attackable) Quest.addSpawn(18694, x - 50, y - 50, npc.getZ(), 0, false, 300000L);
			guard4.addDamageHate(killer, 0, 999);
			guard4.getAI().setIntention(CtrlIntention.ATTACK, killer);
		}
		super.onEvtDead(killer);
	}
}
