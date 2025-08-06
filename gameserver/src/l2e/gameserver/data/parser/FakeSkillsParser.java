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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import l2e.fake.model.HealingSpell;
import l2e.fake.model.OffensiveSpell;
import l2e.fake.model.SpellUsageCondition;
import l2e.fake.model.SupportSpell;
import l2e.gameserver.data.DocumentParser;
import l2e.gameserver.model.base.ClassId;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.skills.targets.TargetType;

public final class FakeSkillsParser extends DocumentParser
{
	private final Map<ClassId, List<OffensiveSpell>> _offensiveSkills = new HashMap<>();
	private final Map<ClassId, List<HealingSpell>> _healSkills = new HashMap<>();
	private final Map<ClassId, List<SupportSpell>> _supportSkills = new HashMap<>();
	private final Map<ClassId, Integer> _skillsChance = new HashMap<>();
	
	protected FakeSkillsParser()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_skillsChance.clear();
		_offensiveSkills.clear();
		_healSkills.clear();
		_supportSkills.clear();
		parseDatapackFile("config/mods/fakes/skills.xml");
		info("Loaded " + (_offensiveSkills.size() + _healSkills.size() + _supportSkills.size()) + " skills for fake players.");
	}
	
	@Override
	protected void reloadDocument()
	{
	}
	
	@Override
	protected void parseDocument()
	{
		for (Node n = getCurrentDocument().getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("fake".equalsIgnoreCase(d.getNodeName()))
					{
						parseEquipment(d);
					}
				}
			}
		}
	}
	
	private void parseEquipment(Node d)
	{
		NamedNodeMap attrs = d.getAttributes();
		int skillChance, id, value, priority;
		String cond;
		final ClassId classId = ClassId.getClassId(Integer.parseInt(attrs.getNamedItem("classId").getNodeValue()));
		skillChance = Integer.parseInt(attrs.getNamedItem("skillsChance").getNodeValue());
		final List<OffensiveSpell> offensiveSkills = new ArrayList<>();
		final List<HealingSpell> healSkills = new ArrayList<>();
		final List<SupportSpell> supportSkills = new ArrayList<>();
		for (Node c = d.getFirstChild(); c != null; c = c.getNextSibling())
		{
			if ("offensiveSkill".equalsIgnoreCase(c.getNodeName()))
			{
				attrs = c.getAttributes();
				id = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
				priority = Integer.parseInt(attrs.getNamedItem("priority").getNodeValue());
				
				final Skill skill = SkillsParser.getInstance().getInfo(id, SkillsParser.getInstance().getMaxLevel(id));
				if (skill == null)
				{
					warn("Can't find fake offensive skill id: " + id + " skills.xml");
					continue;
				}
				offensiveSkills.add(new OffensiveSpell(id, priority));
			}
			else if ("healSkill".equalsIgnoreCase(c.getNodeName()))
			{
				attrs = c.getAttributes();
				id = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
				value = Integer.parseInt(attrs.getNamedItem("value").getNodeValue());
				priority = Integer.parseInt(attrs.getNamedItem("priority").getNodeValue());
				
				final Skill skill = SkillsParser.getInstance().getInfo(id, SkillsParser.getInstance().getMaxLevel(id));
				if (skill == null)
				{
					warn("Can't find fake heal skill id: " + id + " in skills.xml");
					continue;
				}
				healSkills.add(new HealingSpell(id, TargetType.ONE, value, priority));
			}
			else if ("supportSkill".equalsIgnoreCase(c.getNodeName()))
			{
				attrs = c.getAttributes();
				
				id = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
				cond = attrs.getNamedItem("cond").getNodeValue();
				value = Integer.parseInt(attrs.getNamedItem("value").getNodeValue());
				priority = Integer.parseInt(attrs.getNamedItem("priority").getNodeValue());
				
				SpellUsageCondition condition = null;
				switch (cond)
				{
					case "MOREHPPERCENT":
						condition = SpellUsageCondition.MOREHPPERCENT;
						break;
					case "LESSHPPERCENT":
							condition = SpellUsageCondition.LESSHPPERCENT;
							break;
					case "MISSINGCP":
						condition = SpellUsageCondition.MISSINGCP;
						break;
					default:
						condition = SpellUsageCondition.NONE;
						break;
				}
				
				final Skill skill = SkillsParser.getInstance().getInfo(id, SkillsParser.getInstance().getMaxLevel(id));
				if (skill == null)
				{
					warn("Can't find fake support skill id: " + id + " level: skills.xml");
					continue;
				}
				supportSkills.add(new SupportSpell(id, condition, value, priority));
			}
		}
		_skillsChance.put(classId, skillChance);
		_offensiveSkills.put(classId, offensiveSkills);
		_healSkills.put(classId, healSkills);
		_supportSkills.put(classId, supportSkills);
	}
	
	public int getSkillsChance(ClassId cId)
	{
		return _skillsChance.get(cId);
	}
	
	public List<OffensiveSpell> getOffensiveSkills(ClassId cId)
	{
		return _offensiveSkills.get(cId);
	}
	
	public List<HealingSpell> getHealSkills(ClassId cId)
	{
		return _healSkills.get(cId);
	}
	
	public List<SupportSpell> getSupportSkills(ClassId cId)
	{
		return _supportSkills.get(cId);
	}
	
	public static FakeSkillsParser getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final FakeSkillsParser _instance = new FakeSkillsParser();
	}
}