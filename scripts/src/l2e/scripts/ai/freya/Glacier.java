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
package l2e.scripts.ai.freya;

import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.ai.npc.Fighter;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;

public class Glacier extends Fighter
{
	public Glacier(Attackable actor)
	{
		super(actor);
		actor.block();
	}

	@Override
	protected void onEvtSpawn()
	{
		super.onEvtSpawn();
		getActiveChar().setDisplayEffect(1);
		ThreadPoolManager.getInstance().schedule(new Freeze(), 800);
		ThreadPoolManager.getInstance().schedule(new Despawn(), 30000L);
	}

	@Override
	protected void onEvtDead(Creature killer)
	{
		for (final Player cha : World.getInstance().getAroundPlayers(getActiveChar(), 350, 200))
		{
			cha.makeTriggerCast(SkillsParser.getInstance().getInfo(6301, 1), cha);
		}
		super.onEvtDead(killer);
	}

	private class Freeze implements Runnable
	{
		@Override
		public void run()
		{
			getActiveChar().setDisplayEffect(2);
		}
	}

	private class Despawn implements Runnable
	{
		@Override
		public void run()
		{
			getActor().deleteMe();
		}
	}
}
