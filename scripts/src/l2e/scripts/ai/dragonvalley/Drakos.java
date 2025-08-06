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
package l2e.scripts.ai.dragonvalley;

import l2e.commons.util.Rnd;
import l2e.gameserver.ai.model.CtrlEvent;
import l2e.gameserver.data.parser.NpcsParser;
import l2e.gameserver.idfactory.IdFactory;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.instance.MonsterInstance;

/**
 * Created by LordWinter 22.11.2018
 */
public class Drakos extends HerbCollectorFighter
{
	public Drakos(Attackable actor)
	{
		super(actor);
	}

	@Override
	protected void onEvtAttacked(Creature attacker, int damage)
	{
		final Attackable actor = getActiveChar();
		if (actor == null || actor.isDead())
		{
			return;
		}
		
		final int chance = actor.getTemplate().getParameter("helpersSpawnChance", 0);
		if (attacker != null && (actor.getCurrentHp() < (actor.getMaxHp() / 2)) && (Rnd.chance(chance)) && actor.isScriptValue(0))
		{
			actor.setScriptValue(1);
			final String[] amount = actor.getTemplate().getParameter("helpersRndAmount", "1;2").split(";");
			final int rnd = Rnd.get(Integer.parseInt(amount[0]), Integer.parseInt(amount[1]));
			for (int i = 0; i < rnd; i++)
			{
				try
				{
					final MonsterInstance npc = new MonsterInstance(IdFactory.getInstance().getNextId(), NpcsParser.getInstance().getTemplate(22823));
					final Location loc = ((MonsterInstance) actor).getMinionPosition();
					npc.setReflection(actor.getReflection());
					npc.setHeading(actor.getHeading());
					npc.setCurrentHpMp(npc.getMaxHp(), npc.getMaxMp());
					npc.spawnMe(loc.getX(), loc.getY(), loc.getZ());
					npc.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, attacker, Rnd.get(1, 100));
				}
				catch (final Exception e)
				{
					e.printStackTrace();
				}
			}
		}
		super.onEvtAttacked(attacker, damage);
	}
}