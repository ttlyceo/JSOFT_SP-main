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

import l2e.gameserver.ai.model.CtrlEvent;
import l2e.gameserver.ai.npc.Fighter;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.quest.Quest;

/**
 * Created by LordWinter 15.11.2018
 */
public class EtisEtina extends Fighter
{
	private Npc summon1;
	private Npc summon2;

	public EtisEtina(Attackable actor)
	{
		super(actor);
	}

	@Override
	protected void onEvtAttacked(Creature attacker, int damage)
	{
		final Attackable actor = getActiveChar();
		if (attacker != null && (actor.getCurrentHpPercents() < 70) && actor.isScriptValue(0))
		{
			actor.setScriptValue(1);
			summon1 = Quest.addSpawn(18950, actor.getLocation(), actor.getReflection(), 150);
			summon1.setRunning();
			summon1.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, attacker, 100);

			summon2 = Quest.addSpawn(18951, actor.getLocation(), actor.getReflection(), 150);
			summon2.setRunning();
			summon2.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, attacker, 100);
		}
		super.onEvtAttacked(attacker, damage);
	}

	@Override
	protected void onEvtDead(Creature killer)
	{
		if ((summon1 != null) && !summon1.isDead())
		{
			summon1.decayMe();
		}
		if ((summon2 != null) && !summon2.isDead())
		{
			summon2.decayMe();
		}
		super.onEvtDead(killer);
	}
}
