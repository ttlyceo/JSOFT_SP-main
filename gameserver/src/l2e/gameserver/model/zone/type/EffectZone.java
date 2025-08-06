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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import l2e.commons.util.Rnd;
import l2e.commons.util.StringUtil;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.instancemanager.ZoneManager;
import l2e.gameserver.model.GameObject.InstanceType;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.zone.TaskZoneSettings;
import l2e.gameserver.model.zone.ZoneId;
import l2e.gameserver.model.zone.ZoneType;
import l2e.gameserver.network.serverpackets.EtcStatusUpdate;
import l2e.gameserver.taskmanager.EffectTaskManager;

public class EffectZone extends ZoneType
{
	private int _chance;
	private int _initialDelay;
	private int _reuse;
	protected boolean _bypassConditions;
	private boolean _isShowDangerIcon;
	protected Map<Integer, Integer> _skills;
	protected boolean _isRemoveEffects;
	
	public EffectZone(int id)
	{
		super(id);
		_chance = 100;
		_initialDelay = 0;
		_reuse = 30000;
		setTargetType(InstanceType.Playable);
		_bypassConditions = false;
		_isShowDangerIcon = true;
		_isRemoveEffects = false;
		var settings = ZoneManager.getSettings(getName());
		if (settings == null)
		{
			settings = new TaskZoneSettings();
		}
		setSettings(settings);
		addZoneId(ZoneId.ALTERED);
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
		else if (name.equals("bypassSkillConditions"))
		{
			_bypassConditions = Boolean.parseBoolean(value);
		}
		else if (name.equals("maxDynamicSkillCount"))
		{
			_skills = new ConcurrentHashMap<>(Integer.parseInt(value));
		}
		else if (name.equals("removeEffectsFromExit"))
		{
			_isRemoveEffects = Boolean.parseBoolean(value);
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
		else if (name.equals("showDangerIcon"))
		{
			_isShowDangerIcon = Boolean.parseBoolean(value);
			if (_isShowDangerIcon)
			{
				addZoneId(ZoneId.DANGER_AREA);
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
		
		if (character.isPlayer())
		{
			if (_isShowDangerIcon)
			{
				character.sendPacket(new EtcStatusUpdate(character.getActingPlayer()));
			}
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
	protected void onExit(Creature character)
	{
		if (character.isPlayer())
		{
			if (_isShowDangerIcon)
			{
				if (!character.isInsideZone(ZoneId.DANGER_AREA, this))
				{
					character.sendPacket(new EtcStatusUpdate(character.getActingPlayer()));
				}
			}
		}
		
		if (_isRemoveEffects && character.isPlayable())
		{
			for (final int skillId : _skills.keySet())
			{
				if (character.getFirstEffect(skillId) != null)
				{
					character.stopSkillEffects(skillId);
				}
			}
		}
		
		if (getPlayersInside().isEmpty() && (getSettings().getTask() != null))
		{
			getSettings().clear();
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
			if (_skills != null)
			{
				for (final var temp : getCharactersInside())
				{
					if ((temp != null) && !temp.isDead())
					{
						if (Rnd.get(100) < getChance())
						{
							for (final var e : _skills.entrySet())
							{
								final var skill = getSkill(e.getKey(), e.getValue());
								if ((skill != null) && (_bypassConditions || skill.checkCondition(temp, temp, false, true)))
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
	
	@Override
	public void setIsEnabled(boolean val)
	{
		super.setIsEnabled(val);
		if (!val)
		{
			getSettings().clear();
		}
		else
		{
			if (!getPlayersInside().isEmpty())
			{
				enableEffect();
			}
		}
	}
}