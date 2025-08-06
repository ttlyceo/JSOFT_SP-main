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

import java.util.HashMap;
import java.util.Map;

import l2e.commons.util.Rnd;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.instancemanager.ZoneManager;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.base.ClassId;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.zone.TaskZoneSettings;
import l2e.gameserver.model.zone.ZoneId;
import l2e.gameserver.model.zone.ZoneType;
import l2e.gameserver.taskmanager.EffectTaskManager;

/**
 * Updated by LordWinter 08.10.2020
 */
public class DravonValleyZone extends ZoneType
{
	private static final Map<ClassId, Double> weight = new HashMap<>();

	private int _chance;
	private int _initialDelay;
	private int _reuse;

	public DravonValleyZone(int id)
	{
		super(id);
		
		var settings = ZoneManager.getSettings(getName());
		if (settings == null)
		{
			settings = new TaskZoneSettings();
		}
		setSettings(settings);
		addZoneId(ZoneId.ALTERED);
	}
	
	static
	{
		weight.put(ClassId.duelist, 0.2);
		weight.put(ClassId.dreadnought, 0.7);
		weight.put(ClassId.phoenixKnight, 0.5);
		weight.put(ClassId.hellKnight, 0.5);
		weight.put(ClassId.sagittarius, 0.3);
		weight.put(ClassId.adventurer, 0.4);
		weight.put(ClassId.archmage, 0.3);
		weight.put(ClassId.soultaker, 0.3);
		weight.put(ClassId.arcanaLord, 1.);
		weight.put(ClassId.cardinal, -0.6);
		weight.put(ClassId.hierophant, 0.);
		weight.put(ClassId.evaTemplar, 0.8);
		weight.put(ClassId.swordMuse, 0.5);
		weight.put(ClassId.windRider, 0.4);
		weight.put(ClassId.moonlightSentinel, 0.3);
		weight.put(ClassId.mysticMuse, 0.3);
		weight.put(ClassId.elementalMaster, 1.);
		weight.put(ClassId.evaSaint, -0.6);
		weight.put(ClassId.shillienTemplar, 0.8);
		weight.put(ClassId.spectralDancer, 0.5);
		weight.put(ClassId.ghostHunter, 0.4);
		weight.put(ClassId.ghostSentinel, 0.3);
		weight.put(ClassId.stormScreamer, 0.3);
		weight.put(ClassId.spectralMaster, 1.);
		weight.put(ClassId.shillienSaint, -0.6);
		weight.put(ClassId.titan, 0.3);
		weight.put(ClassId.dominator, 0.1);
		weight.put(ClassId.grandKhavatari, 0.2);
		weight.put(ClassId.doomcryer, 0.1);
		weight.put(ClassId.fortuneSeeker, 0.9);
		weight.put(ClassId.maestro, 0.7);
		weight.put(ClassId.doombringer, 0.2);
		weight.put(ClassId.trickster, 0.5);
		weight.put(ClassId.judicator, 0.1);
		weight.put(ClassId.maleSoulhound, 0.3);
		weight.put(ClassId.femaleSoulhound, 0.3);
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
		else
		{
			super.setParameter(name, value);
		}
	}
	
	@Override
	protected void onEnter(Creature character)
	{
		if (isEnabled() && character.isPlayer())
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
					getSettings().setTask(EffectTaskManager.getInstance().scheduleAtFixedRate(new BuffTask(), _initialDelay, _reuse));
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
	
	protected int getBuffLevel(Creature character)
	{
		if (character == null || character.getParty() == null)
		{
			return 0;
		}
		
		final var party = character.getParty();
		if (party.getMemberCount() < 4)
		{
			return 0;
		}
		
		for (final var p : party.getMembers())
		{
			if ((p.getLevel() < 80) || (p.getClassId().level() != 3))
			{
				return 0;
			}
		}
		
		double points = 0;
		final int count = party.getMemberCount();
		for (final var p : party.getMembers())
		{
			points += weight.get(p.getClassId());
		}
		return (int) Math.max(0, Math.min(3, Math.round(points * getCoefficient(count))));
	}
	
	private double getCoefficient(int count)
	{
		double cf;
		switch (count)
		{
			case 1 :
				cf = 0.7;
				break;
			case 4 :
				cf = 0.7;
				break;
			case 5 :
				cf = 0.75;
				break;
			case 6 :
				cf = 0.8;
				break;
			case 7 :
				cf = 0.85;
				break;
			case 8 :
				cf = 0.9;
				break;
			case 9 :
				cf = 0.95;
				break;
			default :
				cf = 1;
		}
		return cf;
	}
	
	public int getChance()
	{
		return _chance;
	}
	
	protected final class BuffTask implements Runnable
	{
		@Override
		public void run()
		{
			for (final var player : getPlayersInside())
			{
				if ((player != null) && !player.isDead())
				{
					if (getBuffLevel(player) > 0)
					{
						if (Rnd.get(100) < getChance())
						{
							final var skill = getSkill(6885, getBuffLevel(player));
							if (skill != null)
							{
								if (player.getFirstEffect(6885) == null)
								{
									skill.getEffects(player, player, false);
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