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
package l2e.gameserver.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.model.actor.templates.PetLevelTemplate;
import l2e.gameserver.model.holders.PetSkillLearnHolder;
import l2e.gameserver.model.skills.Skill;

public class PetData
{
	private final Map<Integer, PetLevelTemplate> _levelStats = new HashMap<>();
	private final List<PetSkillLearnHolder> _skills = new ArrayList<>();

	private final int _npcId;
	private final int _itemId;
	private int _load = 20000;
	private int _hungryLimit = 1;
	private int _minlvl = Byte.MAX_VALUE;
	private boolean _syncLevel = false;
	private final List<Integer> _food = new ArrayList<>();

	public PetData(int npcId, int itemId)
	{
		_npcId = npcId;
		_itemId = itemId;
	}
	
	public int getNpcId()
	{
		return _npcId;
	}

	public int getItemId()
	{
		return _itemId;
	}

	public void addNewStat(int level, PetLevelTemplate data)
	{
		if (_minlvl > level)
		{
			_minlvl = level;
		}
		_levelStats.put(level, data);
	}

	public PetLevelTemplate getPetLevelData(int petLevel)
	{
		return _levelStats.get(petLevel);
	}

	public int getLoad()
	{
		return _load;
	}

	public int getHungryLimit()
	{
		return _hungryLimit;
	}

	public boolean isSynchLevel()
	{
		return _syncLevel;
	}

	public int getMinLevel()
	{
		return _minlvl;
	}

	public List<Integer> getFood()
	{
		return _food;
	}

	public void addFood(Integer foodId)
	{
		_food.add(foodId);
	}

	public void setLoad(int load)
	{
		_load = load;
	}

	public void setHungryLimit(int limit)
	{
		_hungryLimit = limit;
	}

	public void setSyncLevel(boolean val)
	{
		_syncLevel = val;
	}

	public void addNewSkill(int skillId, int skillLvl, int petLvl, double hpPercent)
	{
		_skills.add(new PetSkillLearnHolder(skillId, skillLvl, petLvl, hpPercent));
	}
	
	public double getHpPercent(int id, int lvl)
	{
		for (final PetSkillLearnHolder temp : _skills)
		{
			if (temp.getId() == id && (temp.getLvl() == lvl || temp.getLvl() == 0))
			{
				return temp.getHpPercent();
			}
		}
		return 0;
	}

	public Skill getAvailableSkill(int skillId, int petLvl)
	{
		int level = 0;
		Skill skill = null;
		for (final PetSkillLearnHolder temp : _skills)
		{
			if (temp != null && temp.getId() == skillId)
			{
				if (temp.getLvl() == 0)
				{
					if (petLvl < 70)
					{
						level = (petLvl / 10);
						if (level <= 0)
						{
							level = 1;
						}
					}
					else
					{
						level = (7 + ((petLvl - 70) / 5));
					}
					
					final int maxLvl = SkillsParser.getInstance().getMaxLevel(temp.getId());
					if (level > maxLvl)
					{
						level = maxLvl;
					}
					skill = SkillsParser.getInstance().getInfo(temp.getId(), level);
					break;
				}
				else
				{
					if (temp.getSkill() != null && petLvl >= temp.getMinLevel())
    				{
						if (temp.getSkill().getLevel() > level)
						{
							skill = temp.getSkill();
						}
    				}
				}
			}
		}
		return skill;
	}
	
	public List<Integer> getAllSkills()
	{
		final List<Integer> skills = new ArrayList<>();
		for (final PetSkillLearnHolder temp : _skills)
		{
			if (temp != null && temp.getSkill() != null && !skills.contains(temp.getId()))
			{
				skills.add(temp.getId());
			}
		}
		return skills;
	}

	public List<PetSkillLearnHolder> getAvailableSkills()
	{
		return _skills;
	}
}