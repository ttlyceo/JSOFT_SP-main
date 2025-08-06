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
package l2e.gameserver.data.parser;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import l2e.commons.log.LoggerObject;
import l2e.gameserver.model.holders.SkillHolder;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.skills.engines.DocumentEngine;

public class SkillsParser extends LoggerObject
{
	private final Map<Integer, Skill> _skills = new HashMap<>();
	private final Map<Integer, Integer> _skillMaxLevel = new HashMap<>();
	private final Set<Integer> _enchantable = new HashSet<>();

	protected SkillsParser()
	{
		load();
	}

	public void reload()
	{
		load();
		SkillTreesParser.getInstance().load();
	}

	private void load()
	{
		final Map<Integer, Skill> temp = new HashMap<>();
		DocumentEngine.getInstance().loadAllSkills(temp);
		
		_skills.clear();
		_skills.putAll(temp);
		
		_skillMaxLevel.clear();
		_enchantable.clear();
		
		for (final Skill skill : _skills.values())
		{
			final int skillId = skill.getId();
			final int skillLvl = skill.getLevel();
			if (skillLvl > 99 && !skill.isCustom())
			{
				if (!_enchantable.contains(skillId))
				{
					_enchantable.add(skillId);
				}
				continue;
			}
			final int maxLvl = getMaxLevel(skillId);
			if (skillLvl > maxLvl)
			{
				_skillMaxLevel.put(skillId, skillLvl);
			}
		}
	}

	public static int getSkillHashCode(Skill skill)
	{
		return getSkillHashCode(skill.getId(), skill.getLevel());
	}

	public static int getSkillHashCode(int skillId, int skillLevel)
	{
		return (skillId * 1021) + skillLevel;
	}
	
	public static int getId(int skillHashCode)
	{
		return skillHashCode / 1021;
	}
	
	public static int getLvl(int skillHashCode)
	{
		return skillHashCode % 1021;
	}
	
	public Skill getSkill(int skillHashCode)
	{
		return getInfo(getId(skillHashCode), 1);
	}

	public final Skill getInfo(final int skillId, final int level)
	{
		final Skill result = _skills.get(getSkillHashCode(skillId, level));
		if (result != null)
		{
			return result;
		}
		
		if (!_skillMaxLevel.containsKey(skillId))
		{
			warn("No skill info found for skill id " + skillId + " and skill level " + level + ".");
			return null;
		}

		final int maxLvl = _skillMaxLevel.get(skillId);
		if ((maxLvl > 0) && (level > maxLvl))
		{
			return _skills.get(getSkillHashCode(skillId, maxLvl));
		}
		return null;
	}

	public final int getMaxLevel(final int skillId)
	{
		final Integer maxLevel = _skillMaxLevel.get(skillId);
		return maxLevel != null ? maxLevel : 0;
	}

	public final boolean isEnchantable(final int skillId)
	{
		return _enchantable.contains(skillId);
	}

	public Skill[] getSiegeSkills(boolean addNoble, boolean hasCastle)
	{
		final Skill[] temp = new Skill[2 + (addNoble ? 1 : 0) + (hasCastle ? 2 : 0)];
		int i = 0;
		temp[i++] = _skills.get(SkillsParser.getSkillHashCode(246, 1));
		temp[i++] = _skills.get(SkillsParser.getSkillHashCode(247, 1));

		if (addNoble)
		{
			temp[i++] = _skills.get(SkillsParser.getSkillHashCode(326, 1));
		}
		if (hasCastle)
		{
			temp[i++] = _skills.get(SkillsParser.getSkillHashCode(844, 1));
			temp[i++] = _skills.get(SkillsParser.getSkillHashCode(845, 1));
		}
		return temp;
	}

	public static enum FrequentSkill
	{
		RAID_CURSE(4215, 1), RAID_CURSE2(4515, 1), SEAL_OF_RULER(246, 1), BUILD_HEADQUARTERS(247, 1), WYVERN_BREATH(4289, 1), STRIDER_SIEGE_ASSAULT(325, 1), FAKE_PETRIFICATION(4616, 1), FIREWORK(5965, 1), LARGE_FIREWORK(2025, 1), BLESSING_OF_PROTECTION(5182, 1), VOID_BURST(3630, 1), VOID_FLOW(3631, 1), THE_VICTOR_OF_WAR(5074, 1), THE_VANQUISHED_OF_WAR(5075, 1), SPECIAL_TREE_RECOVERY_BONUS(2139, 1), WEAPON_GRADE_PENALTY(6209, 1), ARMOR_GRADE_PENALTY(6213, 1);

		private final SkillHolder _holder;

		private FrequentSkill(int id, int level)
		{
			_holder = new SkillHolder(id, level);
		}

		public int getId()
		{
			return _holder.getId();
		}

		public int getLevel()
		{
			return _holder.getLvl();
		}

		public Skill getSkill()
		{
			return _holder.getSkill();
		}
	}
	
	public static SkillsParser getInstance()
	{
		return SingletonHolder._instance;
	}

	private static class SingletonHolder
	{
		protected static final SkillsParser _instance = new SkillsParser();
	}
}