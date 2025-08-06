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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.holders.SkillHolder;
import l2e.gameserver.model.skills.Skill;
import l2e.scripts.ai.AbstractNpcAI;

public class DemonPrince extends AbstractNpcAI
{
	private static final int DEMON_PRINCE = 25540;
	private static final int FIEND = 25541;

	private static final SkillHolder UD = new SkillHolder(5044, 2);
	private static final SkillHolder[] AOE =
	{
	        new SkillHolder(5376, 4), new SkillHolder(5376, 5), new SkillHolder(5376, 6)
	};
	
	private static final Map<Integer, Boolean> _attackState = new ConcurrentHashMap<>();

	private DemonPrince(String name, String descr)
	{
		super(name, descr);
		
		addAttackId(DEMON_PRINCE);
		addKillId(DEMON_PRINCE);
		addSpawnId(FIEND);
	}

	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		if (event.equalsIgnoreCase("cast") && (npc != null) && (npc.getId() == FIEND) && !npc.isDead())
		{
			npc.doCast(AOE[getRandom(AOE.length)].getSkill());
		}
		return null;
	}

	@Override
	public String onAttack(Npc npc, Player attacker, int damage, boolean isSummon, Skill skill)
	{
		if (!npc.isDead())
		{
			if (!_attackState.containsKey(npc.getObjectId()) && (npc.getCurrentHp() < (npc.getMaxHp() * 0.5)))
			{
				npc.doCast(UD.getSkill());
				spawnMinions(npc);
				_attackState.put(npc.getObjectId(), false);
			}
			else if ((npc.getCurrentHp() < (npc.getMaxHp() * 0.1)) && _attackState.containsKey(npc.getObjectId()) && (_attackState.get(npc.getObjectId()) == false))
			{
				npc.doCast(UD.getSkill());
				spawnMinions(npc);
				_attackState.put(npc.getObjectId(), true);
			}

			if (getRandom(1000) < 10)
			{
				spawnMinions(npc);
			}
		}
		return super.onAttack(npc, attacker, damage, isSummon, skill);
	}

	@Override
	public String onKill(Npc npc, Player killer, boolean isSummon)
	{
		_attackState.remove(npc.getObjectId());
		return super.onKill(npc, killer, isSummon);
	}

	@Override
	public final String onSpawn(Npc npc)
	{
		if (npc.getId() == FIEND)
		{
			startQuestTimer("cast", 15000, npc, null);
		}
		return super.onSpawn(npc);
	}

	private void spawnMinions(Npc master)
	{
		if ((master != null) && !master.isDead())
		{
			final var ref = master.getReflection();
			final int x = master.getX();
			final int y = master.getY();
			final int z = master.getZ();
			addSpawn(FIEND, x + 200, y, z, 0, false, 0, false, ref);
			addSpawn(FIEND, x - 200, y, z, 0, false, 0, false, ref);
			addSpawn(FIEND, x - 100, y - 140, z, 0, false, 0, false, ref);
			addSpawn(FIEND, x - 100, y + 140, z, 0, false, 0, false, ref);
			addSpawn(FIEND, x + 100, y - 140, z, 0, false, 0, false, ref);
			addSpawn(FIEND, x + 100, y + 140, z, 0, false, 0, false, ref);
		}
	}

	public static void main(String[] args)
	{
		new DemonPrince(DemonPrince.class.getSimpleName(), "ai");
	}
}