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

import l2e.gameserver.data.parser.SpawnParser;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.holders.SkillHolder;
import l2e.gameserver.model.spawn.Spawner;
import l2e.scripts.ai.AbstractNpcAI;

public class Typhoon extends AbstractNpcAI
{
	private static final int TYPHOON = 25539;
	private Npc _typhoon;
	
	private static SkillHolder STORM = new SkillHolder(5434, 1);
	
	private Typhoon(String name, String descr)
	{
		super(name, descr);
		
		addAggroRangeEnterId(TYPHOON);
		addSpawnId(TYPHOON);

		for (final Spawner spawn : SpawnParser.getInstance().getSpawnData())
		{
			if (spawn != null)
			{
				if (spawn.getId() == TYPHOON)
				{
					_typhoon = spawn.getLastSpawn();
					if (_typhoon != null)
					{
						onSpawn(_typhoon);
					}
				}
			}
		}
	}
	
	@Override
	public String onAggroRangeEnter(Npc npc, Player player, boolean isSummon)
	{
		if (npc.getId() == TYPHOON)
		{
			if ((!npc.isCastingNow()) && (!npc.isAttackingNow()) && (!player.isDead()))
			{
				npc.doSimultaneousCast(STORM.getSkill());
			}
		}
		return null;
	}
	
	@Override
	public final String onSpawn(Npc npc)
	{
		((Attackable) npc).setCanReturnToSpawnPoint(false);
		npc.setIsRunner(true);
		return super.onSpawn(npc);
	}
	
	public static void main(String[] args)
	{
		new Typhoon(Typhoon.class.getSimpleName(), "ai");
	}
}
