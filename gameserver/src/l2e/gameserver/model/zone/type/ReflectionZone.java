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
package l2e.gameserver.model.zone.type;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import l2e.commons.util.Rnd;
import l2e.commons.util.StringUtil;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.instancemanager.ZoneManager;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.zone.TaskZoneSettings;
import l2e.gameserver.model.zone.ZoneId;
import l2e.gameserver.model.zone.ZoneType;
import l2e.gameserver.taskmanager.EffectTaskManager;

public class ReflectionZone extends ZoneType
{
	private int _chance;
	private int _initialDelay;
	private int _reuse;
	private Map<Integer, Integer> _skills;
	private final List<Integer> _reflections = new ArrayList<>();
	
	public ReflectionZone(int id)
	{
		super(id);
		
		var settings = ZoneManager.getSettings(getName());
		if (settings == null)
		{
			settings = new TaskZoneSettings();
		}
		setSettings(settings);
		addZoneId(ZoneId.REFLECTION);
	}
	
	@Override
	public TaskZoneSettings getSettings()
	{
		return (TaskZoneSettings) super.getSettings();
	}
	
	@Override
	public void setParameter(String name, String value)
	{
		if (name.equals("chance"))
		{
			_chance = Integer.parseInt(value);
		}
		else if (name.equals("initialDelay"))
		{
			_initialDelay = Integer.parseInt(value);
		}
		else if (name.equals("reuse"))
		{
			_reuse = Integer.parseInt(value);
		}
		else if (name.equals("maxDynamicSkillCount"))
		{
			_skills = new ConcurrentHashMap<>(Integer.parseInt(value));
		}
		else if (name.equals("skillIdLvl"))
		{
			final String[] propertySplit = value.split(";");
			_skills = new ConcurrentHashMap<>(propertySplit.length);
			for (final String skill : propertySplit)
			{
				final String[] skillSplit = skill.split("-");
				if (skillSplit.length != 2)
				{
					_log.warn(StringUtil.concat(getClass().getSimpleName() + ": invalid config property -> skillsIdLvl \"", skill, "\""));
				}
				else
				{
					try
					{
						_skills.put(Integer.parseInt(skillSplit[0]), Integer.parseInt(skillSplit[1]));
					}
					catch (final NumberFormatException nfe)
					{
						if (!skill.isEmpty())
						{
							_log.warn(StringUtil.concat(getClass().getSimpleName() + ": invalid config property -> skillsIdLvl \"", skillSplit[0], "\"", skillSplit[1]));
						}
					}
				}
			}
		}
		else
		{
			super.setParameter(name, value);
		}
	}
	
	@Override
	protected void onEnter(Creature character)
	{
		if (isEnabled() && character.isPlayer() && _skills != null)
		{
			enableEffect();
		}
	}
	
	@Override
	protected void onExit(Creature character)
	{
		if (getPlayersInside().isEmpty() && (getSettings().getTask() != null))
		{
			getSettings().clear();
		}
	}
	
	private void enableEffect()
	{
		if (getSettings().getTask() == null)
		{
			synchronized (this)
			{
				if (getSettings().getTask() == null)
				{
					getSettings().setTask(EffectTaskManager.getInstance().scheduleAtFixedRate(new ApplySkill(), _initialDelay, _reuse));
				}
			}
		}
	}
	
	@Override
	public void clearTask()
	{
		if (getSettings().getTask() != null)
		{
			getSettings().getTask().cancel(false);
		}
		super.clearTask();
	}
	
	protected Skill getSkill(int skillId, int skillLvl)
	{
		return SkillsParser.getInstance().getInfo(skillId, skillLvl);
	}
	
	public int getChance()
	{
		return _chance;
	}
	
	public void addSkill(int skillId, int skillLvL)
	{
		if (skillLvL < 1)
		{
			removeSkill(skillId);
			return;
		}
		if (_skills == null)
		{
			synchronized (this)
			{
				if (_skills == null)
				{
					_skills = new ConcurrentHashMap<>(3);
				}
			}
		}
		_skills.put(skillId, skillLvL);
	}
	
	public void removeSkill(int skillId)
	{
		if (_skills != null)
		{
			_skills.remove(skillId);
		}
	}
	
	public void clearSkills()
	{
		if (_skills != null)
		{
			_skills.clear();
		}
	}
	
	public int getSkillLevel(int skillId)
	{
		if ((_skills == null) || !_skills.containsKey(skillId))
		{
			return 0;
		}
		return _skills.get(skillId);
	}
	
	private final class ApplySkill implements Runnable
	{
		@Override
		public void run()
		{
			if (!_reflections.isEmpty())
			{
				for (final var temp : getCharactersInside())
				{
					if ((temp != null) && !temp.isDead())
					{
						if (_reflections.contains(temp.getReflectionId()) && (Rnd.chance(getChance())))
						{
							for (final var e : _skills.entrySet())
							{
								final var skill = getSkill(e.getKey(), e.getValue());
								if (skill != null && skill.checkCondition(temp, temp, false, true))
								{
									if (temp.getFirstEffect(e.getKey()) == null)
									{
										skill.getEffects(temp, temp, false);
									}
								}
							}
						}
					}
				}
			}
		}
	}
	
	public void removeRef(int id)
	{
		if (_reflections.contains(id))
		{
			_reflections.remove(_reflections.indexOf(Integer.valueOf(id)));
		}
	}
	
	public void addRef(int id)
	{
		if (!_reflections.contains(id))
		{
			_reflections.add(id);
		}
	}
}