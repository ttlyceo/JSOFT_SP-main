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

import java.util.HashMap;
import java.util.Map;

import l2e.commons.util.Util;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.holders.SkillHolder;
import l2e.gameserver.model.skills.Skill;

/**
 * Based on L2J Eternity-World
 */
public class SelfExplosiveKamikaze extends AbstractNpcAI
{
	private static final Map<Integer, SkillHolder> MONSTERS = new HashMap<>();
	
	static
	{
		MONSTERS.put(18817, new SkillHolder(5376, 4));
		MONSTERS.put(18818, new SkillHolder(5376, 4));
		MONSTERS.put(18821, new SkillHolder(5376, 5));
		MONSTERS.put(21666, new SkillHolder(4614, 3));
		MONSTERS.put(21689, new SkillHolder(4614, 4));
		MONSTERS.put(21712, new SkillHolder(4614, 5));
		MONSTERS.put(21735, new SkillHolder(4614, 6));
		MONSTERS.put(21758, new SkillHolder(4614, 7));
		MONSTERS.put(21781, new SkillHolder(4614, 9));
	}
	
	public SelfExplosiveKamikaze(String name, String descr)
	{
		super(name, descr);
		
		for (final int npcId : MONSTERS.keySet())
		{
			addAttackId(npcId);
			addSpellFinishedId(npcId);
		}
	}
	
	@Override
	public String onAttack(Npc npc, Player player, int damage, boolean isSummon, Skill skil)
	{
		if (player != null)
		{
			if (MONSTERS.containsKey(npc.getId()) && !npc.isDead() && Util.checkIfInRange(MONSTERS.get(npc.getId()).getSkill().getAffectRange(), player, npc, true))
			{
				npc.doCast(MONSTERS.get(npc.getId()).getSkill());
			}
		}
		return super.onAttack(npc, player, damage, isSummon, skil);
	}
	
	@Override
	public String onSpellFinished(Npc npc, Player player, Skill skill)
	{
		if (MONSTERS.containsKey(npc.getId()) && !npc.isDead() && ((skill.getId() == 4614) || (skill.getId() == 5376)))
		{
			npc.doDie(null);
		}
		
		return super.onSpellFinished(npc, player, skill);
	}
	
	public static void main(String[] args)
	{
		new SelfExplosiveKamikaze(SelfExplosiveKamikaze.class.getSimpleName(), "ai");
	}
}
