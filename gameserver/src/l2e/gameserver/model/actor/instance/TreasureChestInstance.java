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
package l2e.gameserver.model.actor.instance;

import l2e.commons.util.Rnd;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.skills.Skill;

public class TreasureChestInstance extends ChestInstance
{
	private static final int TREASURE_BOMB_ID = 4143;

	public TreasureChestInstance(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public void tryOpen(Player opener, Skill skill)
	{
		final double chance = calcChance(opener, skill);
		if (chance > 0 && Rnd.chance(chance))
		{
			addDamageHate(opener, 10000, 0);
			doDie(opener);
			opener.getCounters().addAchivementInfo("treasureBoxesOpened", 0, -1, false, false, false);
		}
		else
		{
			fakeOpen(opener);
		}
	}

	public double calcChance(Player opener, Skill skill)
	{
		double chance = skill.getActivateRate();
		final int npcLvl = getLevel();
		if (!getTemplate().isCommonChest())
		{
			final double levelmod = (double) skill.getMagicLevel() - npcLvl;
			chance += levelmod * skill.getLevelModifier();
			if (npcLvl - opener.getLevel() >= 5)
			{
				chance += (opener.getLevel() - npcLvl) * 10;
			}
		}
		else
		{
			final int npcLvlDiff = npcLvl - opener.getLevel();
			final int baseDiff = opener.getLevel() <= 77 ? 6 : 5;
			if (npcLvlDiff >= baseDiff)
			{
				chance = 0;
			}
		}
		if (chance < 0)
		{
			chance = 1;
		}
		return chance;
	}

	private void fakeOpen(Creature opener)
	{
		final Skill bomb = SkillsParser.getInstance().getInfo(TREASURE_BOMB_ID, getBombLvl());
		if (bomb != null)
		{
			setTarget(opener);
			doCast(bomb);
		}
		clearAggroList(false);
		onDecay();
	}

	private int getBombLvl()
	{
		final int npcLvl = getLevel();
		int lvl = 1;
		if (npcLvl >= 78)
		{
			lvl = 10;
		}
		else if (npcLvl >= 72)
		{
			lvl = 9;
		}
		else if (npcLvl >= 66)
		{
			lvl = 8;
		}
		else if (npcLvl >= 60)
		{
			lvl = 7;
		}
		else if (npcLvl >= 54)
		{
			lvl = 6;
		}
		else if (npcLvl >= 48)
		{
			lvl = 5;
		}
		else if (npcLvl >= 42)
		{
			lvl = 4;
		}
		else if (npcLvl >= 36)
		{
			lvl = 3;
		}
		else if (npcLvl >= 30)
		{
			lvl = 2;
		}
		return lvl;
	}

	@Override
	public void reduceCurrentHp(double damage, Creature attacker, boolean awake, boolean isDOT, Skill skill)
	{
		if (!getTemplate().isCommonChest())
		{
			fakeOpen(attacker);
		}
	}
}