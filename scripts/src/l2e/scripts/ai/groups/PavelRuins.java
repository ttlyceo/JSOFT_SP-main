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
package l2e.scripts.ai.groups;

import l2e.commons.util.PositionUtils;
import l2e.commons.util.Rnd;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.ai.model.CtrlEvent;
import l2e.gameserver.ai.npc.Fighter;
import l2e.gameserver.data.parser.NpcsParser;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.spawn.Spawner;

/**
 * Created by LordWinter 21.09.2018
 */
public class PavelRuins extends Fighter
{
	private static final int PAVEL_SAFETY_DEVICE = 18917;
	private static final int CRUEL_PINCER_GOLEM_1 = 22801;
	private static final int CRUEL_PINCER_GOLEM_2 = 22802;
	private static final int CRUEL_PINCER_GOLEM_3 = 22803;
	private static final int DRILL_GOLEM_OF_TERROR_1 = 22804;
	private static final int DRILL_GOLEM_OF_TERROR_2 = 22805;
	private static final int DRILL_GOLEM_OF_TERROR_3 = 22806;

	public PavelRuins(Attackable actor)
	{
		super(actor);
	}

	@Override
	protected void onEvtDead(Creature killer)
	{
		final Attackable actor = getActiveChar();
		super.onEvtDead(killer);
		ThreadPoolManager.getInstance().schedule(new SpawnNext(actor, killer), 5000);
	}

	private static class SpawnNext implements Runnable
	{
		private final Attackable _actor;
		private final Creature _killer;

		public SpawnNext(Attackable actor, Creature killer)
		{
			_actor = actor;
			_killer = killer;
		}

		@Override
		public void run()
		{
			if (Rnd.chance(70))
			{
				Location loc = _actor.getLocation();
				switch (_actor.getId())
				{
					case PAVEL_SAFETY_DEVICE :
						loc = new Location(loc.getX() + 30, loc.getY() + -30, loc.getZ());
						spawnNextMob(CRUEL_PINCER_GOLEM_3, _killer, loc);
						loc = new Location(loc.getX() + -30, loc.getY() + 30, loc.getZ());
						spawnNextMob(DRILL_GOLEM_OF_TERROR_3, _killer, loc);
						break;
					case CRUEL_PINCER_GOLEM_1 :
						spawnNextMob(CRUEL_PINCER_GOLEM_2, _killer, loc);
						break;
					case CRUEL_PINCER_GOLEM_3 :
						spawnNextMob(CRUEL_PINCER_GOLEM_1, _killer, loc);
						break;
					case DRILL_GOLEM_OF_TERROR_1 :
						spawnNextMob(DRILL_GOLEM_OF_TERROR_2, _killer, loc);
						break;
					case DRILL_GOLEM_OF_TERROR_3 :
						spawnNextMob(DRILL_GOLEM_OF_TERROR_1, _killer, loc);
						break;
				}
			}
		}
	}

	private static void spawnNextMob(int npcId, Creature killer, Location loc)
	{
		try
		{
			final Spawner sp = new Spawner(NpcsParser.getInstance().getTemplate(npcId));
			sp.setX(loc.getX());
			sp.setY(loc.getY());
			sp.setZ(loc.getZ());
			final Npc npc = sp.doSpawn(true, 0);
			npc.setHeading(PositionUtils.calculateHeadingFrom(npc, killer));
			npc.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, killer, 1000);
		}
		catch (final Exception e)
		{
			e.printStackTrace();
		}
	}
}
