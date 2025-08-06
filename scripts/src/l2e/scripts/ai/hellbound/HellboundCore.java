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

import l2e.gameserver.instancemanager.HellboundManager;
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.holders.SkillHolder;
import l2e.scripts.ai.AbstractNpcAI;

public class HellboundCore extends AbstractNpcAI
{
	private static final int NAIA = 18484;
	private static final int HELLBOUND_CORE = 32331;
	
	private static SkillHolder BEAM = new SkillHolder(5493, 1);
	
	private HellboundCore(String name, String descr)
	{
		super(name, descr);
		addSpawnId(HELLBOUND_CORE);
	}

	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		if (event.equalsIgnoreCase("cast") && (HellboundManager.getInstance().getLevel() <= 6))
		{
			for (final Npc naia : World.getInstance().getAroundNpc(npc, 900, 200))
			{
				if (naia.isMonster() && naia.getId() == NAIA && !naia.isDead())
				{
					naia.setTarget(npc);
					naia.doSimultaneousCast(BEAM.getSkill());
				}
			}
			startQuestTimer("cast", 10000, npc, null);
		}
		return null;
	}
	
	@Override
	public final String onSpawn(Npc npc)
	{
		startQuestTimer("cast", 10000, npc, null);
		return super.onSpawn(npc);
	}
	
	public static void main(String[] args)
	{
		new HellboundCore(HellboundCore.class.getSimpleName(), "ai");
	}
}
