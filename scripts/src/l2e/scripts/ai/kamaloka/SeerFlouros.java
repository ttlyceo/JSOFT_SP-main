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
package l2e.scripts.ai.kamaloka;

import l2e.gameserver.ai.model.CtrlEvent;
import l2e.gameserver.ai.npc.Mystic;
import l2e.gameserver.data.parser.NpcsParser;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.spawn.Spawner;

/**
 * Created by LordWinter 16.11.2018
 */
public class SeerFlouros extends Mystic
{
	public SeerFlouros(Attackable actor)
	{
		super(actor);
	}
	
	@Override
	protected void onEvtAttacked(Creature attacker, int damage)
	{
		final Attackable actor = getActiveChar();
		if (attacker != null && !actor.isDead())
		{
			if (actor.getScriptValue() == 0 && actor.getCurrentHpPercents() <= 80)
			{
				actor.setScriptValue(1);
				spawnMobs(attacker);
			}
			else if (actor.getScriptValue() == 1 && actor.getCurrentHpPercents() <= 60)
			{
				actor.setScriptValue(2);
				spawnMobs(attacker);
			}
			else if (actor.getScriptValue() == 2 && actor.getCurrentHpPercents() <= 40)
			{
				actor.setScriptValue(3);
				spawnMobs(attacker);
			}
			else if (actor.getScriptValue() == 3 && actor.getCurrentHpPercents() <= 30)
			{
				actor.setScriptValue(4);
				spawnMobs(attacker);
			}
			else if (actor.getScriptValue() == 4 && actor.getCurrentHpPercents() <= 20)
			{
				actor.setScriptValue(5);
				spawnMobs(attacker);
			}
			else if (actor.getScriptValue() == 5 && actor.getCurrentHpPercents() <= 10)
			{
				actor.setScriptValue(6);
				spawnMobs(attacker);
			}
			else if (actor.getScriptValue() == 6 && actor.getCurrentHpPercents() <= 5)
			{
				actor.setScriptValue(7);
				spawnMobs(attacker);
			}
		}
		super.onEvtAttacked(attacker, damage);
	}
	
	private void spawnMobs(Creature attacker)
	{
		final Attackable actor = getActiveChar();
		for (int i = 0; i < 2; i++)
		{
			try
			{
				final Spawner sp = new Spawner(NpcsParser.getInstance().getTemplate(18560));
				sp.setLocation(Location.findPointToStay(actor, 100, 120, true));
				sp.setReflection(actor.getReflection());
				sp.stopRespawn();
				final Npc npc = sp.spawnOne(false);
				npc.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, attacker, 100);
			}
			catch (final Exception e)
			{
				e.printStackTrace();
			}
		}
	}
}
