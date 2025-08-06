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
package l2e.scripts.ai.isle_of_prayer;

import l2e.commons.util.Rnd;
import l2e.gameserver.ai.model.CtrlEvent;
import l2e.gameserver.ai.npc.Fighter;
import l2e.gameserver.data.parser.NpcsParser;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.spawn.Spawner;

/**
 * Created by LordWinter 21.09.2018
 */
public class DarkWaterDragon extends Fighter
{
	private static final int SHADE1 = 22268;
	private static final int SHADE2 = 22269;
	private static final int MOBS[] =
	{
	        SHADE1, SHADE2
	};
	
	public DarkWaterDragon(Attackable actor)
	{
		super(actor);
	}
	
	@Override
	protected void onEvtAttacked(Creature attacker, int damage)
	{
		final Attackable actor = getActiveChar();
		if (attacker != null && !actor.isDead())
		{
			switch (actor.getScriptValue())
			{
				case 0 :
					actor.setScriptValue(1);
					spawnShades(attacker);
					break;
				case 1 :
					if (actor.getCurrentHp() < (actor.getMaxHp() / 2))
					{
						actor.setScriptValue(2);
						spawnShades(attacker);
					}
					break;
			}
		}
		
		super.onEvtAttacked(attacker, damage);
	}
	
	private void spawnShades(Creature attacker)
	{
		final Attackable actor = getActiveChar();
		for (int i = 0; i < 5; i++)
		{
			try
			{
				final Spawner sp = new Spawner(NpcsParser.getInstance().getTemplate(MOBS[Rnd.get(MOBS.length)]));
				sp.setLocation(Location.findPointToStay(actor, 100, 120, true));
				sp.stopRespawn();
				final Npc npc = sp.doSpawn(true, 0);
				npc.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, attacker, Rnd.get(1, 100));
			}
			catch (final Exception e)
			{
				e.printStackTrace();
			}
		}
	}
	
	@Override
	protected void onEvtDead(Creature killer)
	{
		final Attackable actor = getActiveChar();
		try
		{
			final Spawner sp = new Spawner(NpcsParser.getInstance().getTemplate(18482));
			sp.setLocation(Location.findPointToStay(actor, 100, 120, true));
			sp.stopRespawn();
			sp.doSpawn(true, 0);
		}
		catch (final Exception e)
		{
			e.printStackTrace();
		}
		if (killer != null)
		{
			final Player player = killer.getActingPlayer();
			if (player != null)
			{
				if (Rnd.chance(77))
				{
					actor.dropSingleItem(player, 9596, 1);
				}
			}
		}
		super.onEvtDead(killer);
	}
}
