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
package l2e.gameserver.model.actor.instance.player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;

import l2e.commons.annotations.NotNull;
import l2e.commons.log.Log;
import l2e.commons.log.LoggerObject;
import l2e.commons.util.Rnd;
import l2e.gameserver.Config;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.data.parser.PetsParser;
import l2e.gameserver.geodata.GeoEngine;
import l2e.gameserver.geodata.pathfinding.AbstractNodeLoc;
import l2e.gameserver.geodata.pathfinding.PathFinding;
import l2e.gameserver.handler.itemhandlers.ItemHandler;
import l2e.gameserver.handler.voicedcommandhandlers.VoicedCommandHandler;
import l2e.gameserver.instancemanager.AutoFarmManager;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.Party;
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.Summon;
import l2e.gameserver.model.actor.instance.player.impl.AutoFarmTask;
import l2e.gameserver.model.actor.tasks.player.AutoArcherFarmTask;
import l2e.gameserver.model.actor.tasks.player.AutoHealFarmTask;
import l2e.gameserver.model.actor.tasks.player.AutoMagicFarmTask;
import l2e.gameserver.model.actor.tasks.player.AutoPhysicalFarmTask;
import l2e.gameserver.model.actor.tasks.player.AutoSummonFarmTask;
import l2e.gameserver.model.actor.templates.ShortCutTemplate;
import l2e.gameserver.model.base.ShortcutType;
import l2e.gameserver.model.service.autofarm.FarmSettings;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.skills.SkillType;
import l2e.gameserver.model.skills.effects.EffectType;
import l2e.gameserver.model.skills.targets.TargetType;
import l2e.gameserver.model.stats.StatsSet;

/**
 * Created by LordWinter
 */
public class AutoFarmOptions extends LoggerObject
{
	final StatsSet _set;
	private boolean _keepLocationMowing = false;
	private boolean _searchPathFind = false;
	private long _autoFarmEnd;
	private long _farmOnlineTime = 0;
	private long _farmLastOnlineTime = 0;
	private boolean _activeFarmOnlineTime = false;
	private boolean _hasResSkills = false;
	private Player _resTarget;
	private final List<Player> _members = new ArrayList<>();

	private final List<Integer> _attackSlots = Arrays.asList(0, 1, 2, 3);
	private final List<Integer> _chanceSlots = Arrays.asList(4, 5);
	private final List<Integer> _selfSlots = Arrays.asList(6, 7, 8, 9);
	private final List<Integer> _lowLifeSlots = Arrays.asList(10, 11);
	
	private final List<Integer> _attackSkills = new ArrayList<>();
	private final List<Integer> _chanceSkills = new ArrayList<>();
	private final List<Integer> _selfSkills = new ArrayList<>();
	private final List<Integer> _lowLifeSkills = new ArrayList<>();
	
	private final List<Integer> _summonAttackSkills = new ArrayList<>();
	private final List<Integer> _summonSelfSkills = new ArrayList<>();
	private final List<Integer> _summonHealSkills = new ArrayList<>();
	private Location _keepLocation = null;
	private long _waitTime = 0;
	private long _emptyTime = 0;
	private int _taskInterval = FarmSettings.FARM_INTERVAL_TASK;
	private final List<Integer> _notSeeList = new ArrayList<>();
	private boolean _abortTarget = false;
	
	public enum SpellType
	{
		ATTACK, CHANCE, SELF, LOWLIFE
	}
	
	private final Player _player;
	public Future<?> _farmTask;
	
	public AutoFarmOptions(Player player)
	{
		_player = player;
		_set = new StatsSet();
	}
	
	public void setFarmTypeValue(int value)
	{
		_set.set("farmType", (value < 0 ? 0 : value > 4 ? 4 : value));
		if (isAutofarming())
		{
			stopFarmTask(true);
		}
	}
	
	public void setSummonFarmTypeValue(int value)
	{
		_set.set("summonFarmType", (value < 0 ? 0 : value > 1 ? 1 : value));
	}
	
	public int getSummonFarmType()
	{
		return _set.getInteger("summonFarmType", FarmSettings.FARM_TYPE);
	}
	
	public int getFarmType()
	{
		return _set.getInteger("farmType", FarmSettings.FARM_TYPE);
	}
	
	public void setRadiusValue(int value)
	{
		_set.set("radius", value);
	}
	
	public void setShortcutPageValue(int value)
	{
		_set.set("shortcutsIndex", ((value < 1 ? 1 : value > 10 ? 10 : value) - 1));
	}
	
	public int getAttackPercent()
	{
		return _set.getInteger("attackSkillPercent", FarmSettings.ATTACK_SKILL_PERCENT);
	}
	
	public int getAttackChance()
	{
		return _set.getInteger("attackSkillChance", FarmSettings.ATTACK_SKILL_CHANCE);
	}
	
	public int getChancePercent()
	{
		return _set.getInteger("chanceSkillPercent", FarmSettings.CHANCE_SKILL_PERCENT);
	}
	
	public int getChanceChance()
	{
		return _set.getInteger("chanceSkillChance", FarmSettings.CHANCE_SKILL_CHANCE);
	}
	
	public int getSelfPercent()
	{
		return _set.getInteger("selfSkillPercent", FarmSettings.SELF_SKILL_PERCENT);
	}
	
	public int getSelfChance()
	{
		return _set.getInteger("selfSkillChance", FarmSettings.SELF_SKILL_CHANCE);
	}
	
	public int getLifePercent()
	{
		return _set.getInteger("lifeSkillPercent", FarmSettings.HEAL_SKILL_PERCENT);
	}
	
	public int getLifeChance()
	{
		return _set.getInteger("lifeSkillChance", FarmSettings.HEAL_SKILL_CHANCE);
	}
	
	public void setAttackSkillValue(boolean isPercent, int value)
	{
		_set.set(isPercent ? "attackSkillPercent" : "attackSkillChance", value);
	}
	
	public void setChanceSkillValue(boolean isPercent, int value)
	{
		_set.set(isPercent ? "chanceSkillPercent" : "chanceSkillChance", value);
	}
	
	public void setSelfSkillValue(boolean isPercent, int value)
	{
		_set.set(isPercent ? "selfSkillPercent" : "selfSkillChance", value);
	}
	
	public void setLifeSkillValue(boolean isPercent, int value)
	{
		_set.set(isPercent ? "lifeSkillPercent" : "lifeSkillChance", value);
	}
	
	public void restoreVariables()
	{
		setAttackSkillValue(false, _player.getVarInt("attackChanceSkills", FarmSettings.ATTACK_SKILL_CHANCE));
		setAttackSkillValue(true, _player.getVarInt("attackSkillsPercent", FarmSettings.ATTACK_SKILL_PERCENT));
		setChanceSkillValue(false, _player.getVarInt("chanceChanceSkills", FarmSettings.CHANCE_SKILL_CHANCE));
		setChanceSkillValue(true, _player.getVarInt("chanceSkillsPercent", FarmSettings.CHANCE_SKILL_PERCENT));
		setSelfSkillValue(false, _player.getVarInt("selfChanceSkills", FarmSettings.SELF_SKILL_CHANCE));
		setSelfSkillValue(true, _player.getVarInt("selfSkillsPercent", FarmSettings.SELF_SKILL_PERCENT));
		setLifeSkillValue(false, _player.getVarInt("healChanceSkills", FarmSettings.HEAL_SKILL_CHANCE));
		setLifeSkillValue(true, _player.getVarInt("healSkillsPercent", FarmSettings.HEAL_SKILL_PERCENT));
		setSummonAttackSkillValue(false, _player.getVarInt("attackSummonChanceSkills", FarmSettings.SUMMON_ATTACK_SKILL_CHANCE));
		setSummonAttackSkillValue(true, _player.getVarInt("attackSummonSkillsPercent", FarmSettings.SUMMON_ATTACK_SKILL_PERCENT));
		setSummonSelfSkillValue(false, _player.getVarInt("selfSummonChanceSkills", FarmSettings.SUMMON_SELF_SKILL_CHANCE));
		setSummonSelfSkillValue(true, _player.getVarInt("selfSummonSkillsPercent", FarmSettings.SUMMON_SELF_SKILL_PERCENT));
		setSummonLifeSkillValue(false, _player.getVarInt("healSummonChanceSkills", FarmSettings.SUMMON_HEAL_SKILL_CHANCE));
		setSummonLifeSkillValue(true, _player.getVarInt("healSummonSkillsPercent", FarmSettings.SUMMON_HEAL_SKILL_PERCENT));
		setShortcutPageValue(_player.getVarInt("shortcutPage", FarmSettings.SHORTCUT_PAGE));
		setRadiusValue(_player.getVarInt("farmDistance", FarmSettings.SEARCH_DISTANCE));
		setFarmTypeValue(_player.getVarInt("farmType", FarmSettings.FARM_TYPE));
		setSummonFarmTypeValue(_player.getVarInt("summonFarmType", FarmSettings.FARM_TYPE));
		setRndAttackSkills(_player.getVarB("farmRndAttackSkills", false), true);
		setRndChanceSkills(_player.getVarB("farmRndChanceSkills", false), true);
		setRndSelfSkills(_player.getVarB("farmRndSelfSkills", false), true);
		setRndLifeSkills(_player.getVarB("farmRndLifeSkills", false), true);
		setRndSummonAttackSkills(_player.getVarB("farmRndSummonAttackSkills", false), true);
		setRndSummonSelfSkills(_player.getVarB("farmRndSummonSelfSkills", false), true);
		setRndSummonLifeSkills(_player.getVarB("farmRndSummonLifeSkills", false), true);
		setLeaderAssist(_player.getVarB("farmLeaderAssist", false), true);
		setPartySupport(_player.getVarB("farmPartySupport", false), true);
		setKeepLocation(_player.getVarB("farmKeepLocation", false), true);
		setExDelaySkill(_player.getVarB("farmExDelaySkill", false), true);
		setExSummonDelaySkill(_player.getVarB("farmExSummonDelaySkill", false), true);
		setSummonPhysAttack(_player.getVarB("farmAllowSummonPhysAttack", false), true);
		setRunTargetCloseUp(_player.getVarB("farmRunTargetCloseUp", false), true);
		setUseSummonSkills(_player.getVarB("farmUseSummonSkills", false), true);
		setAssistMonsterAttack(_player.getVarB("farmAssistMonsterAttack", false), true);
		setTargetRestoreMp(_player.getVarB("farmTargetRestoreMp", false), true);
		setAttackRaid(_player.getVarB("farmAttackRaid", false), true);
		setAttackChampion(_player.getVarB("farmAttackChampion", false), true);
		restoreSkills();
		checkSkillsAtTransformation();
	}
	
	private void restoreSkills()
	{
		final var attackSkills = _player.getVar("farmAttackSkills", null);
		if (attackSkills != null && !attackSkills.isEmpty())
		{
			getAttackSpells().clear();
			final var skills = attackSkills.split(";");
			for (final var sk : skills)
			{
				if (sk != null)
				{
					final var skill = _player.getKnownSkill(Integer.parseInt(sk));
					if (skill != null)
					{
						getAttackSpells().add(skill.getId());
					}
				}
			}
		}
		
		final var chanceSkills = _player.getVar("farmChanceSkills", null);
		if (chanceSkills != null && !chanceSkills.isEmpty())
		{
			getChanceSpells().clear();
			final var skills = chanceSkills.split(";");
			for (final var sk : skills)
			{
				if (sk != null)
				{
					final var skill = _player.getKnownSkill(Integer.parseInt(sk));
					if (skill != null)
					{
						getChanceSpells().add(skill.getId());
					}
				}
			}
		}
		
		final var selfSkills = _player.getVar("farmSelfSkills", null);
		if (selfSkills != null && !selfSkills.isEmpty())
		{
			getSelfSpells().clear();
			final var skills = selfSkills.split(";");
			for (final var sk : skills)
			{
				if (sk != null)
				{
					final var skill = _player.getKnownSkill(Integer.parseInt(sk));
					if (skill != null)
					{
						getSelfSpells().add(skill.getId());
					}
				}
			}
		}
		
		final var healSkills = _player.getVar("farmHealSkills", null);
		if (healSkills != null && !healSkills.isEmpty())
		{
			getLowLifeSpells().clear();
			final var skills = healSkills.split(";");
			for (final var sk : skills)
			{
				if (sk != null)
				{
					final var skill = _player.getKnownSkill(Integer.parseInt(sk));
					if (skill != null)
					{
						getLowLifeSpells().add(skill.getId());
					}
				}
			}
		}
		
		final var attackSummonSkills = _player.getVar("farmAttackSummonSkills", null);
		if (attackSummonSkills != null && !attackSummonSkills.isEmpty())
		{
			getSummonAttackSpells().clear();
			final var skills = attackSummonSkills.split(";");
			for (final var sk : skills)
			{
				if (sk != null)
				{
					getSummonAttackSpells().add(Integer.parseInt(sk));
				}
			}
		}
		
		final var selfSummonSkills = _player.getVar("farmSelfSummonSkills", null);
		if (selfSummonSkills != null && !selfSummonSkills.isEmpty())
		{
			getSummonSelfSpells().clear();
			final var skills = selfSummonSkills.split(";");
			for (final var sk : skills)
			{
				if (sk != null)
				{
					getSummonSelfSpells().add(Integer.parseInt(sk));
				}
			}
		}
		
		final var healSummonSkills = _player.getVar("farmHealSummonSkills", null);
		if (healSummonSkills != null && !healSummonSkills.isEmpty())
		{
			getSummonHealSpells().clear();
			final var skills = healSummonSkills.split(";");
			for (final var sk : skills)
			{
				if (sk != null)
				{
					getSummonHealSpells().add(Integer.parseInt(sk));
				}
			}
		}
	}
	
	public void saveSkills(String type)
	{
		List<Integer> skillList = null;
		switch (type)
		{
			case "farmAttackSkills" :
				skillList = getAttackSpells();
				break;
			case "farmChanceSkills" :
				skillList = getChanceSpells();
				break;
			case "farmSelfSkills" :
				skillList = getSelfSpells();
				break;
			case "farmHealSkills" :
				skillList = getLowLifeSpells();
				break;
			case "farmAttackSummonSkills" :
				skillList = getSummonAttackSpells();
				break;
			case "farmSelfSummonSkills" :
				skillList = getSummonSelfSpells();
				break;
			case "farmHealSummonSkills" :
				skillList = getSummonHealSpells();
				break;
		}
		
		if (skillList != null && !skillList.isEmpty())
		{
			var line = "";
			for (final int id : skillList)
			{
				line += id + ";";
			}
			_player.setVar(type, line);
		}
		else
		{
			_player.unsetVar(type);
		}
	}
	
	public int getShortcutsIndex()
	{
		return _set.getInteger("shortcutsIndex", FarmSettings.SHORTCUT_PAGE);
	}
	
	public int getFarmRadius()
	{
		return _set.getInteger("radius", FarmSettings.SEARCH_DISTANCE);
	}
	
	@NotNull
	private List<Integer> getSpellsInSlots(List<Integer> slots)
	{
		return Arrays.stream(_player.getAllShortCuts()).filter(shortcut -> shortcut.getPage() == getShortcutsIndex() && shortcut.getType() == ShortcutType.SKILL && slots.contains(shortcut.getSlot())).map(ShortCutTemplate::getId).collect(Collectors.toList());
	}
	
	public void refreshChanceSkills()
	{
		_chanceSkills.clear();
		final var newSkills = getSpellsInSlots(_chanceSlots);
		if (!newSkills.isEmpty())
		{
			for (final var skillId : newSkills)
			{
				final var skill = _player.getKnownSkill(skillId);
				if (skill != null)
				{
					if (skill.isChanceSkill())
					{
						_chanceSkills.add(skillId);
					}
				}
			}
			saveSkills("farmChanceSkills");
			newSkills.clear();
		}
		else
		{
			_player.unsetVar("farmChanceSkills");
		}
	}
	
	public List<Integer> getChanceSpells()
	{
		return _chanceSkills;
	}
	
	public void checkSkillsAtRemoveTransformation(Set<Integer> skills)
	{
		if (skills != null)
		{
			final List<Integer> attackToRemove = new ArrayList<>();
			final List<Integer> chanceToRemove = new ArrayList<>();
			final List<Integer> selfToRemove = new ArrayList<>();
			final List<Integer> healToRemove = new ArrayList<>();
			
			final var attackSkills = _attackSkills;
			if (!attackSkills.isEmpty())
			{
				for (final var skillId : attackSkills)
				{
					if (skills.contains(skillId))
					{
						attackToRemove.add(skillId);
					}
				}
				
				if (!attackToRemove.isEmpty())
				{
					for (final var skillId : attackToRemove)
					{
						_attackSkills.remove(Integer.valueOf(skillId));
					}
					attackToRemove.clear();
				}
			}
			
			final var chanceSkills = _chanceSkills;
			if (!chanceSkills.isEmpty())
			{
				for (final var skillId : chanceSkills)
				{
					if (skills.contains(skillId))
					{
						chanceToRemove.add(skillId);
					}
				}
				
				if (!chanceToRemove.isEmpty())
				{
					for (final var skillId : chanceToRemove)
					{
						_chanceSkills.remove(Integer.valueOf(skillId));
					}
					chanceToRemove.clear();
				}
			}
			
			final var selfSkills = _selfSkills;
			if (!selfSkills.isEmpty())
			{
				for (final var skillId : selfSkills)
				{
					if (skills.contains(skillId))
					{
						selfToRemove.add(skillId);
					}
				}
				
				if (!selfToRemove.isEmpty())
				{
					for (final var skillId : selfToRemove)
					{
						_selfSkills.remove(Integer.valueOf(skillId));
					}
					selfToRemove.clear();
				}
			}
			
			final var lowLifeSkills = _lowLifeSkills;
			if (!lowLifeSkills.isEmpty())
			{
				for (final var skillId : lowLifeSkills)
				{
					if (skills.contains(skillId))
					{
						healToRemove.add(skillId);
					}
				}
				
				if (!healToRemove.isEmpty())
				{
					for (final var skillId : healToRemove)
					{
						_lowLifeSkills.remove(Integer.valueOf(skillId));
					}
					healToRemove.clear();
				}
			}
		}
	}
	
	public void checkSkillsAtTransformation()
	{
		if (_player.getTransformation() != null)
		{
			final List<Integer> skillsToRemove = new ArrayList<>();
			final var attackSkills = _attackSkills;
			if (!attackSkills.isEmpty())
			{
				for (final var skillId : attackSkills)
				{
					final var sk = _player.getKnownSkill(skillId);
					if (sk == null)
					{
						skillsToRemove.add(skillId);
					}
					
					if (sk != null && !_player.hasTransformSkill(sk.getId()) && !sk.allowOnTransform())
					{
						skillsToRemove.add(skillId);
					}
				}
				
				if (!skillsToRemove.isEmpty())
				{
					for (final var skillId : skillsToRemove)
					{
						_attackSkills.remove(Integer.valueOf(skillId));
					}
					skillsToRemove.clear();
				}
			}
			
			final var chanceSkills = _chanceSkills;
			if (!chanceSkills.isEmpty())
			{
				for (final var skillId : chanceSkills)
				{
					final var sk = _player.getKnownSkill(skillId);
					if (sk == null)
					{
						skillsToRemove.add(skillId);
					}
					
					if (sk != null && !_player.hasTransformSkill(sk.getId()) && !sk.allowOnTransform())
					{
						skillsToRemove.add(skillId);
					}
				}
				
				if (!skillsToRemove.isEmpty())
				{
					for (final var skillId : skillsToRemove)
					{
						_chanceSkills.remove(Integer.valueOf(skillId));
					}
					skillsToRemove.clear();
				}
			}
			
			final var selfSkills = _selfSkills;
			if (!selfSkills.isEmpty())
			{
				for (final var skillId : selfSkills)
				{
					final var sk = _player.getKnownSkill(skillId);
					if (sk == null)
					{
						skillsToRemove.add(skillId);
					}
					
					if (sk != null && !_player.hasTransformSkill(sk.getId()) && !sk.allowOnTransform())
					{
						skillsToRemove.add(skillId);
					}
				}
				
				if (!skillsToRemove.isEmpty())
				{
					for (final var skillId : skillsToRemove)
					{
						_selfSkills.remove(Integer.valueOf(skillId));
					}
					skillsToRemove.clear();
				}
			}
			
			final var lowLifeSkills = _lowLifeSkills;
			if (!lowLifeSkills.isEmpty())
			{
				for (final var skillId : lowLifeSkills)
				{
					final var sk = _player.getKnownSkill(skillId);
					if (sk == null)
					{
						skillsToRemove.add(skillId);
					}
					
					if (sk != null && !_player.hasTransformSkill(sk.getId()) && !sk.allowOnTransform())
					{
						skillsToRemove.add(skillId);
					}
				}
				
				if (!skillsToRemove.isEmpty())
				{
					for (final var skillId : skillsToRemove)
					{
						_lowLifeSkills.remove(Integer.valueOf(skillId));
					}
					skillsToRemove.clear();
				}
			}
		}
	}
	
	public void refreshAttackSkills()
	{
		_attackSkills.clear();
		final var newSkills = getSpellsInSlots(_attackSlots);
		if (!newSkills.isEmpty())
		{
			for (final var skillId : newSkills)
			{
				final var skill = _player.getKnownSkill(skillId);
				if (skill != null)
				{
					if (!skill.isSpoilSkill() && !skill.isSweepSkill() && skill.getId() != 1263 && skill.isAttackSkill())
					{
						_attackSkills.add(skillId);
					}
				}
			}
			saveSkills("farmAttackSkills");
			newSkills.clear();
		}
		else
		{
			_player.unsetVar("farmAttackSkills");
		}
	}
	
	public List<Integer> getAttackSpells()
	{
		return _attackSkills;
	}
	
	public void refreshSelfSkills()
	{
		_selfSkills.clear();
		final var newSkills = getSpellsInSlots(_selfSlots);
		if (!newSkills.isEmpty())
		{
			for (final var skillId : newSkills)
			{
				final var skill = _player.getKnownSkill(skillId);
				if (skill != null)
				{
					if (skill.isNotSelfSkill())
					{
						continue;
					}
					_selfSkills.add(skillId);
				}
			}
			saveSkills("farmSelfSkills");
			newSkills.clear();
		}
		else
		{
			_player.unsetVar("farmSelfSkills");
		}
	}
	
	public List<Integer> getSelfSpells()
	{
		return _selfSkills;
	}
	
	public void refreshLowLifeSkills()
	{
		_lowLifeSkills.clear();
		final var newSkills = getSpellsInSlots(_lowLifeSlots);
		if (!newSkills.isEmpty())
		{
			for (final var skillId : newSkills)
			{
				final var skill = _player.getKnownSkill(skillId);
				if (skill != null)
				{
					if (skill.isNotNotHealSkill())
					{
						continue;
					}
					_lowLifeSkills.add(skillId);
				}
			}
			saveSkills("farmHealSkills");
			newSkills.clear();
		}
		else
		{
			_player.unsetVar("farmHealSkills");
		}
	}
	
	public List<Integer> getLowLifeSpells()
	{
		return _lowLifeSkills;
	}
	
	public void checkAllSlots()
	{
		refreshChanceSkills();
		refreshAttackSkills();
		refreshSelfSkills();
		refreshLowLifeSkills();
	}
	
	public void startFarmTask()
	{
		if (isAutofarming() || !FarmSettings.ALLOW_AUTO_FARM || (FarmSettings.AUTO_FARM_FOR_PREMIUM && !_player.hasPremiumBonus()))
		{
			return;
		}
		
		final var lastHwids = AutoFarmManager.getInstance().getActiveFarms(_player);
		if (lastHwids <= 0 && !AutoFarmManager.getInstance().isNonCheckPlayer(_player.getObjectId()))
		{
			_player.sendMessage("Exceeded limit on use of service!");
			return;
		}
		
		try
		{
			if (_farmTask != null)
			{
				_farmTask.cancel(false);
				_farmTask = null;
			}
		}
		catch (final Exception e)
		{}
		
		AutoFarmManager.getInstance().addActiveFarm(_player);
		
		if (isKeepLocation())
		{
			setKeepLocation(_player.getLocation());
		}
		
		for (final var sk : _player.getSkills().values())
		{
			if (sk != null && sk.getSkillType() == SkillType.RESURRECT && sk.getTargetType() == TargetType.CORPSE_PLAYER)
			{
				_hasResSkills = true;
			}
		}
		
		switch (getFarmType())
		{
			case 0 :
				if (_taskInterval <= 0)
				{
					_taskInterval = _player.getPAtkSpd() > 1000 ? Config.MIN_HIT_TIME : 1000;
				}
				_farmTask = ThreadPoolManager.getInstance().schedule(new AutoPhysicalFarmTask(_player), 1000);
				break;
			case 1 :
				if (_taskInterval <= 0)
				{
					_taskInterval = _player.getPAtkSpd() > 1000 ? Config.MIN_HIT_TIME : 1000;
				}
				_farmTask = ThreadPoolManager.getInstance().schedule(new AutoArcherFarmTask(_player), 1000);
				break;
			case 2 :
				if (_taskInterval <= 0)
				{
					_taskInterval = _player.getMAtkSpd() > 1000 ? Config.MIN_HIT_TIME : 1000;
				}
				_farmTask = ThreadPoolManager.getInstance().schedule(new AutoMagicFarmTask(_player), 1000);
				break;
			case 3 :
				if (_taskInterval <= 0)
				{
					if (_player.getPAtkSpd() > _player.getMAtkSpd())
					{
						_taskInterval = _player.getPAtkSpd() > 1000 ? Config.MIN_HIT_TIME : 1000;
					}
					else
					{
						_taskInterval = _player.getMAtkSpd() > 1000 ? Config.MIN_HIT_TIME : 1000;
					}
				}
				_farmTask = ThreadPoolManager.getInstance().schedule(new AutoHealFarmTask(_player), 1000);
				break;
			case 4 :
				if (!_player.hasSummon())
				{
					_player.sendMessage("You have no summon! Autofarming deactivate!");
					AutoFarmManager.getInstance().removeActiveFarm(_player);
					return;
				}
				
				if (_taskInterval <= 0)
				{
					if (_player.getPAtkSpd() > _player.getMAtkSpd())
					{
						_taskInterval = _player.getPAtkSpd() > 1000 ? Config.MIN_HIT_TIME : 1000;
					}
					else
					{
						_taskInterval = _player.getMAtkSpd() > 1000 ? Config.MIN_HIT_TIME : 1000;
					}
				}
				_farmTask = ThreadPoolManager.getInstance().schedule(new AutoSummonFarmTask(_player), 1000);
				break;
		}
		
		final var notCheckTime = (FarmSettings.PREMIUM_FARM_FREE && _player.hasPremiumBonus()) || FarmSettings.AUTO_FARM_FREE;
		if (FarmSettings.FARM_ONLINE_TYPE && !notCheckTime)
		{
			final var taskTime = _player.getVarLong("activeFarmOnlineTask", 0) - getLastFarmOnlineTime();
			_player.getPersonalTasks().removeTask(37, false);
			_player.getPersonalTasks().addTask(new AutoFarmTask(taskTime));
			setFarmOnlineTime();
		}
		_player.sendMessage("Autofarming activated");
		Log.addLogFarm("AUTOFARM:", "Activated by", _player);
	}
	
	public void stopFarmTask(boolean isSwitch)
	{
		if (!isAutofarming() || !FarmSettings.ALLOW_AUTO_FARM)
		{
			return;
		}
		
		try
		{
			if (_farmTask != null)
			{
				_farmTask.cancel(false);
				_farmTask = null;
			}
		}
		catch (final Exception e)
		{}
		
		final var notCheckTime = (FarmSettings.PREMIUM_FARM_FREE && _player.hasPremiumBonus()) || FarmSettings.AUTO_FARM_FREE;
		if (FarmSettings.FARM_ONLINE_TYPE && !notCheckTime)
		{
			_player.getPersonalTasks().removeTask(37, true);
			final var time = (getLastFarmOnlineTime() + (System.currentTimeMillis() - getFarmOnlineTime()));
			_player.setVar("activeFarmOnlineTime", time);
			_farmLastOnlineTime = time;
			_farmOnlineTime = 0;
		}
		_hasResSkills = false;
		AutoFarmManager.getInstance().removeActiveFarm(_player);
		_player.sendMessage("Autofarming deactivated");
		
		if (isSwitch)
		{
			startFarmTask();
		}
		else
		{
			Log.addLogFarm("AUTOFARM:", "Deactivated by", _player);
		}
	}
	
	public boolean isAutofarming()
	{
		return _farmTask != null;
	}
	
	public void checkFarmTask()
	{
		if (FarmSettings.FARM_ONLINE_TYPE)
		{
			final var timeEnd = _player.getVarLong("activeFarmOnlineTask", 0);
			if ((_player.getVarLong("activeFarmOnlineTime", 0) < timeEnd) && timeEnd != 0)
			{
				_activeFarmOnlineTime = true;
				_farmLastOnlineTime = _player.getVarLong("activeFarmOnlineTime", 0);
			}
			else
			{
				_activeFarmOnlineTime = false;
			}
		}
		else
		{
			final var timeEnd = _player.getVarLong("activeFarmTask", 0);
			if (timeEnd > System.currentTimeMillis())
			{
				_player.getPersonalTasks().addTask(new AutoFarmTask((timeEnd - System.currentTimeMillis())));
				_autoFarmEnd = timeEnd;
			}
			else
			{
				_autoFarmEnd = 0;
			}
		}
	}
	
	public void setAutoFarmEndTask(long value)
	{
		if (value == 0)
		{
			_player.getPersonalTasks().removeTask(37, true);
		}
		_autoFarmEnd = value;
	}
	
	public long getAutoFarmEnd()
	{
		return _autoFarmEnd;
	}
	
	public boolean isActiveAutofarm()
	{
		return isActiveFarmTask() || FarmSettings.AUTO_FARM_FREE || (FarmSettings.PREMIUM_FARM_FREE && _player.hasPremiumBonus()) || (FarmSettings.FARM_ONLINE_TYPE && isActiveFarmOnlineTime());
	}
	
	public boolean isActiveFarmTask()
	{
		return _player.getPersonalTasks().isActiveTask(37);
	}
	
	public boolean isRndAttackSkills()
	{
		return _set.getBool("rndAttackSkills", false);
	}
	
	public boolean isRndChanceSkills()
	{
		return _set.getBool("rndChanceSkills", false);
	}
	
	public boolean isRndSelfSkills()
	{
		return _set.getBool("rndSelfSkills", false);
	}
	
	public boolean isRndLifeSkills()
	{
		return _set.getBool("rndLifeSkills", false);
	}
	
	public void setRndAttackSkills(boolean rnd, boolean store)
	{
		_set.set("rndAttackSkills", rnd);
		if (!store)
		{
			_player.setVar("farmRndAttackSkills", rnd ? 1 : 0);
		}
	}
	
	public void setRndChanceSkills(boolean rnd, boolean store)
	{
		_set.set("rndChanceSkills", rnd);
		if (!store)
		{
			_player.setVar("farmRndChanceSkills", rnd ? 1 : 0);
		}
	}
	
	public void setRndSelfSkills(boolean rnd, boolean store)
	{
		_set.set("rndSelfSkills", rnd);
		if (!store)
		{
			_player.setVar("farmRndSelfSkills", rnd ? 1 : 0);
		}
	}
	
	public void setRndLifeSkills(boolean rnd, boolean store)
	{
		_set.set("rndLifeSkills", rnd);
		if (!store)
		{
			_player.setVar("farmRndLifeSkills", rnd ? 1 : 0);
		}
	}
	
	public boolean isLeaderAssist()
	{
		return _set.getBool("leaderAssist", false);
	}
	
	public boolean isPartySupport()
	{
		return _set.getBool("partySupport", false);
	}
	
	public boolean isKeepLocation()
	{
		return _set.getBool("keepLocation", false);
	}
	
	public boolean isExtraDelaySkill()
	{
		return _set.getBool("exDelaySkill", false);
	}
	
	public boolean isExtraSummonDelaySkill()
	{
		return _set.getBool("exSummonDelaySkill", false);
	}
	
	public boolean isAllowSummonPhysAttack()
	{
		return _set.getBool("allowSummonPhysAttack", false);
	}
	
	public boolean isRunTargetCloseUp()
	{
		return _set.getBool("runTargetCloseUp", false);
	}
	
	public boolean isUseSummonSkills()
	{
		return _set.getBool("useSummonSkills", false);
	}
	
	public boolean isAttackRaid()
	{
		return _set.getBool("isAttackRaid", false);
	}
	
	public boolean isAttackChampion()
	{
		return _set.getBool("isAttackChampion", false);
	}
	
	public void setLeaderAssist(boolean rnd, boolean store)
	{
		_set.set("leaderAssist", _player.getParty() != null && _player.getParty().getLeader() == _player ? false : rnd);
		if (!store)
		{
			_player.setVar("farmLeaderAssist", isLeaderAssist() ? 1 : 0);
		}
	}
	
	public void setPartySupport(boolean rnd, boolean store)
	{
		_set.set("partySupport", _player.getParty() != null && _player.getParty().getLeader() == _player ? false : rnd);
		if (!store)
		{
			_player.setVar("farmPartySupport", isLeaderAssist() ? 1 : 0);
		}
	}
	
	public void setKeepLocation(boolean rnd, boolean store)
	{
		_set.set("keepLocation", rnd);
		if (!store)
		{
			_player.setVar("farmKeepLocation", rnd ? 1 : 0);
			setKeepLocation(rnd ? _player.getLocation() : null);
		}
	}
	
	public void setExDelaySkill(boolean rnd, boolean store)
	{
		_set.set("exDelaySkill", rnd);
		if (!store)
		{
			_player.setVar("farmExDelaySkill", rnd ? 1 : 0);
		}
	}
	
	public void setExSummonDelaySkill(boolean rnd, boolean store)
	{
		_set.set("exSummonDelaySkill", rnd);
		if (!store)
		{
			_player.setVar("farmExSummonDelaySkill", rnd ? 1 : 0);
		}
	}
	
	public void setSummonPhysAttack(boolean rnd, boolean store)
	{
		_set.set("allowSummonPhysAttack", rnd);
		if (!store)
		{
			_player.setVar("farmAllowSummonPhysAttack", rnd ? 1 : 0);
		}
	}
	
	public void setRunTargetCloseUp(boolean rnd, boolean store)
	{
		_set.set("runTargetCloseUp", rnd);
		if (!store)
		{
			_player.setVar("farmRunTargetCloseUp", rnd ? 1 : 0);
		}
	}
	
	public void setUseSummonSkills(boolean rnd, boolean store)
	{
		_set.set("useSummonSkills", rnd);
		if (!store)
		{
			_player.setVar("farmUseSummonSkills", rnd ? 1 : 0);
		}
	}
	
	public boolean isAssistMonsterAttack()
	{
		return _set.getBool("isAssistMonsterAttack", false);
	}
	
	public boolean isTargetRestoreMp()
	{
		return _set.getBool("isTargetRestoreMp", false);
	}
	
	public void setAssistMonsterAttack(boolean rnd, boolean store)
	{
		_set.set("isAssistMonsterAttack", rnd);
		if (!store)
		{
			_player.setVar("farmAssistMonsterAttack", rnd ? 1 : 0);
		}
	}
	
	public void setTargetRestoreMp(boolean rnd, boolean store)
	{
		_set.set("isTargetRestoreMp", rnd);
		if (!store)
		{
			_player.setVar("farmTargetRestoreMp", rnd ? 1 : 0);
		}
	}
	
	public void setAttackRaid(boolean rnd, boolean store)
	{
		_set.set("isAttackRaid", rnd);
		if (!store)
		{
			_player.setVar("farmAttackRaid", rnd ? 1 : 0);
		}
	}
	
	public void setAttackChampion(boolean rnd, boolean store)
	{
		_set.set("isAttackChampion", rnd);
		if (!store)
		{
			_player.setVar("farmAttackChampion", rnd ? 1 : 0);
		}
	}
	
	public List<Integer> getSummonAttackSpells()
	{
		return _summonAttackSkills;
	}
	
	public List<Integer> getSummonSelfSpells()
	{
		return _summonSelfSkills;
	}
	
	public List<Integer> getSummonHealSpells()
	{
		return _summonHealSkills;
	}
	
	public int getSummonAttackPercent()
	{
		return _set.getInteger("attackSummonSkillPercent", FarmSettings.SUMMON_ATTACK_SKILL_PERCENT);
	}
	
	public int getSummonAttackChance()
	{
		return _set.getInteger("attackSummonSkillChance", FarmSettings.SUMMON_ATTACK_SKILL_CHANCE);
	}
	
	public int getSummonSelfPercent()
	{
		return _set.getInteger("selfSummonSkillPercent", FarmSettings.SUMMON_SELF_SKILL_PERCENT);
	}
	
	public int getSummonSelfChance()
	{
		return _set.getInteger("selfSummonSkillChance", FarmSettings.SUMMON_SELF_SKILL_CHANCE);
	}
	
	public int getSummonLifePercent()
	{
		return _set.getInteger("lifeSummonSkillPercent", FarmSettings.SUMMON_HEAL_SKILL_PERCENT);
	}
	
	public int getSummonLifeChance()
	{
		return _set.getInteger("lifeSummonSkillChance", FarmSettings.SUMMON_HEAL_SKILL_CHANCE);
	}
	
	public void setSummonAttackSkillValue(boolean isPercent, int value)
	{
		_set.set(isPercent ? "attackSummonSkillPercent" : "attackSummonSkillChance", value);
	}
	
	public void setSummonSelfSkillValue(boolean isPercent, int value)
	{
		_set.set(isPercent ? "selfSummonSkillPercent" : "selfSummonSkillChance", value);
	}
	
	public void setSummonLifeSkillValue(boolean isPercent, int value)
	{
		_set.set(isPercent ? "lifeSummonSkillPercent" : "lifeSummonSkillChance", value);
	}
	
	public boolean isRndSummonAttackSkills()
	{
		return _set.getBool("rndSummonAttackSkills", false);
	}
	
	public boolean isRndSummonSelfSkills()
	{
		return _set.getBool("rndSummonSelfSkills", false);
	}
	
	public boolean isRndSummonLifeSkills()
	{
		return _set.getBool("rndSummonLifeSkills", false);
	}
	
	public void setRndSummonAttackSkills(boolean rnd, boolean store)
	{
		_set.set("rndSummonAttackSkills", rnd);
		if (!store)
		{
			_player.setVar("farmRndSummonAttackSkills", rnd ? 1 : 0);
		}
	}
	
	public void setRndSummonSelfSkills(boolean rnd, boolean store)
	{
		_set.set("rndSummonSelfSkills", rnd);
		if (!store)
		{
			_player.setVar("farmRndSummonSelfSkills", rnd ? 1 : 0);
		}
	}
	
	public void setRndSummonLifeSkills(boolean rnd, boolean store)
	{
		_set.set("rndSummonLifeSkills", rnd);
		if (!store)
		{
			_player.setVar("farmRndSummonLifeSkills", rnd ? 1 : 0);
		}
	}
	
	public final Attackable getAroundNpc(Function<Npc, Boolean> condition)
	{
		final List<Attackable> notSeeList = new ArrayList<>();
		int count = 0;
		for (final var npc : World.getInstance().getAroundFarmNpc(_player, getFarmRadius(), getHeightRadius()))
		{
			final var canSee = condition.apply(npc);
			if (canSee)
			{
				if (isKeepLocation())
				{
					if (getKeepLocation() != null && (Math.sqrt(npc.getDistanceSq(getKeepLocation())) > getFarmRadius()) && npc.getDistance(_player) > 100)
					{
						continue;
					}
				}
				
				if (npc.isInTargetList(_player))
				{
					_notSeeList.clear();
					return npc;
				}
			}
			
			if (!canSee && count < 3 && !_keepLocationMowing && !_searchPathFind && !_notSeeList.contains(npc.getObjectId()))
			{
				count++;
				notSeeList.add(npc);
			}
		}
		
		if (!notSeeList.isEmpty())
		{
			for (final var npc : notSeeList)
			{
				if (npc != null && !npc.isDead())
				{
					if (isKeepLocation())
					{
						if (getKeepLocation() != null && (Math.sqrt(npc.getDistanceSq(getKeepLocation())) > getFarmRadius()) && npc.getDistance(_player) > 100)
						{
							continue;
						}
					}
					
					if (!npc.isInTargetList(_player))
					{
						continue;
					}
					
					final List<AbstractNodeLoc> path = PathFinding.getInstance().findPath(_player, _player.getX(), _player.getY(), _player.getZ(), npc.getX(), npc.getY(), npc.getZ(), _player.getReflection(), true, false);
					if (path == null || path.size() < 2)
					{
						_notSeeList.add(npc.getObjectId());
						addWaitTime(System.currentTimeMillis() + 500L);
						continue;
					}
					_player.getAI().setIntention(CtrlIntention.MOVING, npc.getLocation(), 0);
					_searchPathFind = true;
					break;
				}
			}
		}
		
		if (isKeepLocation() && getKeepLocation() != null && !_player.isMoving())
		{
			_keepLocationMowing = true;
			if (Math.abs(_player.getDistanceSq(getKeepLocation())) > 200)
			{
				_player.getAI().setIntention(CtrlIntention.MOVING, getKeepLocation(), 0);
				addWaitTime(System.currentTimeMillis() + 500L);
			}
		}
		return null;
	}
	
	public Skill nextAttackSkill(Attackable target, long extraDelay)
	{
		if (getAttackSpells().isEmpty() || !Rnd.chance(getAttackChance()))
		{
			return null;
		}
		
		if (isExtraDelaySkill() && extraDelay > System.currentTimeMillis())
		{
			return null;
		}
		
		final var mpPercent = _player.getCurrentMpPercents();
		if (mpPercent < getAttackPercent())
		{
			return null;
		}
		
		if (isRndAttackSkills())
		{
			return nextRndAttackSkill(target);
		}
		
		final var distance = target != null ? _player.getDistance(target) : 0;
		for (final var skillId : getAttackSpells())
		{
			final var skill = _player.getKnownSkill(skillId);
			if (skill == null)
			{
				continue;
			}
			
			if (distance > 0 && (skill.isAura() && (distance > skill.getAffectRange())))
			{
				continue;
			}
			
			if (!_player.checkDoCastConditions(skill, false))
			{
				continue;
			}
			
			if (skill.isOffensive() && skill.getTargetType() == TargetType.ONE && (target == null || target.isDead()))
			{
				continue;
			}
			_player.setTarget(target);
			return skill;
		}
		return null;
	}
	
	private Skill nextRndAttackSkill(Attackable target)
	{
		final List<Skill> skillList = new ArrayList<>();
		Skill rndSkill = null;
		final var distance = target != null ? _player.getDistance(target) : 0;
		for (final var skillId : getAttackSpells())
		{
			final var skill = _player.getKnownSkill(skillId);
			if (skill == null)
			{
				continue;
			}
			
			if (distance > 0 && (skill.isAura() && (distance > skill.getAffectRange())))
			{
				continue;
			}
			
			if (!_player.checkDoCastConditions(skill, false))
			{
				continue;
			}
			
			if (skill.isOffensive() && skill.getTargetType() == TargetType.ONE && (target == null || target.isDead()))
			{
				continue;
			}
			skillList.add(skill);
		}
		
		if (!skillList.isEmpty())
		{
			rndSkill = skillList.get(Rnd.get(skillList.size()));
			_player.setTarget(target);
		}
		skillList.clear();
		return rndSkill;
	}
	
	public Skill nextChanceSkill(Attackable target, long extraDelay)
	{
		if (getChanceSpells().isEmpty() || !Rnd.chance(getChanceChance()))
		{
			return null;
		}
		
		if (isExtraDelaySkill() && extraDelay > System.currentTimeMillis())
		{
			return null;
		}
		
		final var mpPercent = _player.getCurrentMpPercents();
		if (target == null || mpPercent < getChancePercent())
		{
			return null;
		}
		
		if (isRndChanceSkills())
		{
			return nextRndChanceSkill(target);
		}
		final var distance = target != null ? _player.getDistance(target) : 0;
		for (final var skillId : getChanceSpells())
		{
			final var skill = _player.getKnownSkill(skillId);
			if (skill == null)
			{
				continue;
			}
			
			if (distance > 0 && (skill.isAura() && (distance > skill.getAffectRange())))
			{
				continue;
			}
			
			if (!_player.checkDoCastConditions(skill, false))
			{
				continue;
			}
			
			if ((skill.isSpoilSkill() && target.isSpoil()) || (skill.isSweepSkill() && !target.isDead()))
			{
				continue;
			}
			
			if (target.getFirstEffect(skillId) != null)
			{
				continue;
			}
			return skill;
		}
		return null;
	}
	
	private Skill nextRndChanceSkill(Attackable target)
	{
		final List<Skill> skillList = new ArrayList<>();
		Skill rndSkill = null;
		final var distance = target != null ? _player.getDistance(target) : 0;
		for (final var skillId : getChanceSpells())
		{
			final var skill = _player.getKnownSkill(skillId);
			if (skill == null)
			{
				continue;
			}
			
			if (distance > 0 && (skill.isAura() && (distance > skill.getAffectRange())))
			{
				continue;
			}
			
			if (!_player.checkDoCastConditions(skill, false))
			{
				continue;
			}
			
			if ((skill.isSpoilSkill() && target.isSpoil()) || (skill.isSweepSkill() && !target.isDead()))
			{
				continue;
			}
			
			if (target.getFirstEffect(skillId) != null)
			{
				continue;
			}
			skillList.add(skill);
		}
		
		if (!skillList.isEmpty())
		{
			rndSkill = skillList.get(Rnd.get(skillList.size()));
		}
		skillList.clear();
		return rndSkill;
	}
	
	public Skill nextSelfSkill(Creature ownerTarget)
	{
		if (getSelfSpells().isEmpty() || !Rnd.chance(getSelfChance()))
		{
			return null;
		}
		
		final var mpPercent = _player.getCurrentMpPercents();
		final var hpPercent = _player.getCurrentHpPercents();
		if (mpPercent < getSelfPercent())
		{
			return null;
		}
		
		if (isRndSelfSkills())
		{
			return nextRndSelfSkill(ownerTarget);
		}
		
		for (final var skillId : getSelfSpells())
		{
			final var skill = _player.getKnownSkill(skillId);
			if (skill == null)
			{
				continue;
			}
			
			final var conds = skill.checkPercentCondition();
			if (conds != null)
			{
				if ((conds[0] < 1 && hpPercent > conds[1]) || (conds[0] > 0 && mpPercent > conds[1]))
				{
					continue;
				}
			}
			
			if (!_player.checkDoCastConditions(skill, false))
			{
				continue;
			}
			
			if (skill.isToggle() && _player.getFirstEffect(skillId) == null)
			{
				return skill;
			}
			
			if (ownerTarget != null && ownerTarget.getFirstEffect(skillId) == null && skill.getTargetType() != TargetType.SELF)
			{
				_player.setTarget(ownerTarget);
				return skill;
			}
			
			if (skill.hasEffectType(EffectType.SUMMON_CUBIC))
			{
				final var cubicId = skill.getEffectTemplates()[0].getCubicId();
				final var cubicMastery = _player.getFirstPassiveEffect(EffectType.CUBIC_MASTERY);
				final var cubicCount = (int) (cubicMastery != null ? (cubicMastery.calc() - 1) : 0);
				if (_player.isCubicLimit(cubicId, cubicCount))
				{
					continue;
				}
				return skill;
			}
			
			if (_player.getFirstEffect(skillId) == null && skill.getTargetType() != TargetType.SERVITOR)
			{
				if (_player.getFirstAbnormalType(skill) == null)
				{
					_player.setTarget(_player);
					return skill;
				}
			}
		}
		return null;
	}
	
	private Skill nextRndSelfSkill(Creature ownerTarget)
	{
		final List<Skill> skillList = new ArrayList<>();
		final List<Skill> skillOwnerList = new ArrayList<>();
		Skill rndSkill = null;
		final var mpPercent = _player.getCurrentMpPercents();
		final var hpPercent = _player.getCurrentHpPercents();
		for (final var skillId : getSelfSpells())
		{
			final var skill = _player.getKnownSkill(skillId);
			if (skill == null)
			{
				continue;
			}
			
			final var conds = skill.checkPercentCondition();
			if (conds != null)
			{
				if ((conds[0] < 1 && hpPercent > conds[1]) || (conds[0] > 0 && mpPercent > conds[1]))
				{
					continue;
				}
			}
			
			if (!_player.checkDoCastConditions(skill, false))
			{
				continue;
			}
			
			if (skill.isToggle() && _player.getFirstEffect(skillId) == null)
			{
				skillList.add(skill);
				continue;
			}
			
			if (skill.hasEffectType(EffectType.SUMMON_CUBIC))
			{
				final var cubicId = skill.getEffectTemplates()[0].getCubicId();
				final var cubicMastery = _player.getFirstPassiveEffect(EffectType.CUBIC_MASTERY);
				final var cubicCount = (int) (cubicMastery != null ? (cubicMastery.calc() - 1) : 0);
				if (_player.isCubicLimit(cubicId, cubicCount))
				{
					continue;
				}
				return skill;
			}
			
			if (_player.getFirstEffect(skillId) == null && skill.getTargetType() != TargetType.SERVITOR)
			{
				if (_player.getFirstAbnormalType(skill) == null)
				{
					skillList.add(skill);
				}
			}
			
			if (ownerTarget != null && ownerTarget.getFirstEffect(skillId) == null && skill.getTargetType() != TargetType.SELF)
			{
				skillOwnerList.add(skill);
			}
		}
		
		var isForSelf = true;
		if (!skillOwnerList.isEmpty())
		{
			rndSkill = skillOwnerList.get(Rnd.get(skillOwnerList.size()));
			isForSelf = false;
		}
		else
		{
			if (!skillList.isEmpty())
			{
				rndSkill = skillList.get(Rnd.get(skillList.size()));
			}
		}
		skillList.clear();
		skillOwnerList.clear();
		
		if (rndSkill == null)
		{
			return null;
		}
		
		if (ownerTarget != null && !isForSelf)
		{
			_player.setTarget(ownerTarget);
		}
		else
		{
			_player.setTarget(_player);
		}
		return rndSkill;
	}
	
	public Skill nextSelfPartySkill(Player owner, Party party)
	{
		if (getSelfSpells().isEmpty() || !Rnd.chance(getSelfChance()) || !isPartySupport())
		{
			return null;
		}
		
		final var mpPercent = _player.getCurrentMpPercents();
		if (mpPercent < getSelfPercent())
		{
			return null;
		}
		
		for (final var skillId : getSelfSpells())
		{
			final var skill = _player.getKnownSkill(skillId);
			if (skill == null)
			{
				continue;
			}
			
			if (!_player.checkDoCastConditions(skill, false))
			{
				continue;
			}
			
			if (skill.isToggle() && _player.getFirstEffect(skillId) == null)
			{
				return skill;
			}
			
			if (skill.hasEffectType(EffectType.SUMMON_CUBIC))
			{
				final var cubicId = skill.getEffectTemplates()[0].getCubicId();
				final var cubicMastery = _player.getFirstPassiveEffect(EffectType.CUBIC_MASTERY);
				final var cubicCount = (int) (cubicMastery != null ? (cubicMastery.calc() - 1) : 0);
				if (_player.isCubicLimit(cubicId, cubicCount))
				{
					continue;
				}
				return skill;
			}
			
			if (_player.getFirstEffect(skillId) == null && skill.getTargetType() != TargetType.SERVITOR)
			{
				if (_player.getFirstAbnormalType(skill) == null)
				{
					_player.setTarget(_player);
					return skill;
				}
			}
		}
		
		if (party == null)
		{
			return null;
		}
		
		_members.clear();
		_members.addAll(party.getMembers());
		Collections.sort(_members, new DistanceComparator(_player));
		for (final var pl : _members)
		{
			if (pl != null && _player.isInRange(pl, 2000))
			{
				for (final var skillId : getSelfSpells())
				{
					final var skill = _player.getKnownSkill(skillId);
					if (skill == null)
					{
						continue;
					}
					
					if (!_player.checkDoCastConditions(skill, false))
					{
						continue;
					}
					
					if (skill.isToggle() || skill.getTargetType() == TargetType.SERVITOR || skill.getTargetType() == TargetType.SELF)
					{
						continue;
					}
					
					if (pl.getFirstEffect(skillId) == null && pl.getFirstAbnormalType(skill) == null)
					{
						_player.setTarget(pl);
						owner = pl;
						return skill;
					}
				}
			}
		}
		owner = party.getLeader();
		return null;
	}
	
	public Skill nextHealSkill(Attackable target, Creature ownerTarget)
	{
		if (getLowLifeSpells().isEmpty() || !Rnd.chance(getLifeChance()))
		{
			return null;
		}
		
		final var hpPercent = _player.getCurrentHpPercents();
		final var mpPercent = _player.getCurrentMpPercents();
		final var ownerHpPercent = ownerTarget != null ? ownerTarget.getCurrentHpPercents() : 100;
		final var ownerMpPercent = ownerTarget != null ? ownerTarget.getCurrentMpPercents() : 100;
		
		final var ownerHeal = ownerHpPercent < getLifePercent();
		final var selfHeal = hpPercent < getLifePercent();
		final var ownerMp = ownerMpPercent < getLifePercent();
		final var selfMp = mpPercent < getLifePercent();
		
		if (!ownerHeal && !selfHeal && !ownerMp && !selfMp)
		{
			return null;
		}
		
		if (isRndLifeSkills())
		{
			return nextRndHealSkill(target, ownerTarget);
		}
		
		for (final var skillId : getLowLifeSpells())
		{
			final var skill = _player.getKnownSkill(skillId);
			if (skill == null)
			{
				continue;
			}
			
			if (!_player.checkDoCastConditions(skill, false))
			{
				continue;
			}
			
			if (skill.isOffensive() && (target == null || !selfHeal))
			{
				continue;
			}
			
			if (isHeal(skill))
			{
				if (!ownerHeal && !selfHeal)
				{
					continue;
				}
				
				if (ownerHeal && ownerTarget != null && !ownerTarget.isDead() && skill.getTargetType() != TargetType.SELF)
				{
					if (skill.getTargetType() == TargetType.SERVITOR && !ownerTarget.isSummon())
					{
						continue;
					}
					_player.setTarget(ownerTarget);
					return skill;
				}
				else if (selfHeal)
				{
					if (skill.getTargetType() == TargetType.SERVITOR)
					{
						continue;
					}
					_player.setTarget(_player);
					return skill;
				}
				return null;
			}
			else if (isManaHeal(skill))
			{
				if (!ownerMp && !selfMp)
				{
					continue;
				}
				
				if (isTargetRestoreMp() && ownerTarget != null && ownerMp && !ownerTarget.isDead() && skill.getTargetType() != TargetType.SELF)
				{
					_player.setTarget(ownerTarget);
					return skill;
				}
				else if (skill.getTargetType() == TargetType.SELF && selfMp)
				{
					return skill;
				}
				return null;
			}
			return skill;
		}
		return null;
	}
	
	private Skill nextRndHealSkill(Attackable target, Creature ownerTarget)
	{
		final List<Skill> skillList = new ArrayList<>();
		Skill rndSkill = null;
		
		final var hpPercent = _player.getCurrentHpPercents();
		final var mpPercent = _player.getCurrentMpPercents();
		final var ownerHpPercent = ownerTarget != null ? ownerTarget.getCurrentHpPercents() : 100;
		final var ownerMpPercent = ownerTarget != null ? ownerTarget.getCurrentMpPercents() : 100;
		
		final var ownerHeal = ownerHpPercent < getLifePercent();
		final var selfHeal = hpPercent < getLifePercent();
		final var ownerMp = ownerMpPercent < getLifePercent();
		final var selfMp = mpPercent < getLifePercent();
		
		if (!ownerHeal && !selfHeal && !ownerMp && !selfMp)
		{
			return null;
		}
		
		for (final var skillId : getLowLifeSpells())
		{
			final var skill = _player.getKnownSkill(skillId);
			if (skill == null)
			{
				continue;
			}
			
			if (!_player.checkDoCastConditions(skill, false))
			{
				continue;
			}
			
			if (skill.isOffensive() && (target == null || !selfHeal))
			{
				continue;
			}
			
			if (ownerHeal || selfHeal)
			{
				if (isHeal(skill))
				{
					if (ownerHeal)
					{
						if (ownerTarget != null && !ownerTarget.isDead() && skill.getTargetType() != TargetType.SELF)
						{
							if (skill.getTargetType() == TargetType.SERVITOR && !ownerTarget.isSummon())
							{
								continue;
							}
							skillList.add(skill);
						}
					}
					else if (selfHeal)
					{
						if (skill.getTargetType() == TargetType.SERVITOR)
						{
							continue;
						}
						skillList.add(skill);
					}
				}
			}
			else if (ownerMp || selfMp)
			{
				if (isManaHeal(skill))
				{
					if (isTargetRestoreMp() && ownerTarget != null && !ownerTarget.isDead() && skill.getTargetType() != TargetType.SELF)
					{
						skillList.add(skill);
					}
					else if (skill.getTargetType() == TargetType.SELF && selfMp)
					{
						skillList.add(skill);
					}
				}
			}
		}
		
		if (!skillList.isEmpty())
		{
			rndSkill = skillList.get(Rnd.get(skillList.size()));
		}
		skillList.clear();
		
		if (rndSkill == null)
		{
			return null;
		}
		
		if (ownerHeal || ownerMp)
		{
			_player.setTarget(ownerTarget);
		}
		else
		{
			_player.setTarget(_player);
		}
		return rndSkill;
	}
	
	public void tryUseMagic(Skill skill, boolean forceOnSelf)
	{
		if (forceOnSelf)
		{
			final var oldTarget = _player.getTarget();
			_player.setTarget(_player);
			_player.useMagic(skill, false, false, false);
			_player.setTarget(oldTarget);
			return;
		}
		_player.useMagic(skill, false, false, false);
	}
	
	public void setKeepLocation(Location loc)
	{
		_keepLocation = loc;
		if (loc == null)
		{
			_keepLocationMowing = false;
		}
	}
	
	public Location getKeepLocation()
	{
		return _keepLocation;
	}
	
	public boolean isNeedToReturn()
	{
		if (!isKeepLocation() || getKeepLocation() == null || isInWaitStatus(null))
		{
			return false;
		}
		
		if (_keepLocationMowing && getKeepLocation() != null && !_player.isMoving())
		{
			if (Math.abs(_player.getDistanceSq(getKeepLocation())) <= 200)
			{
				_searchPathFind = false;
				_keepLocationMowing = false;
			}
			else
			{
				final List<AbstractNodeLoc> path = PathFinding.getInstance().findPath(_player, _player.getX(), _player.getY(), _player.getZ(), getKeepLocation().getX(), getKeepLocation().getY(), getKeepLocation().getZ(), _player.getReflection(), true, false);
				if (path != null && path.size() > 2)
				{
					_keepLocationMowing = true;
					_player.getAI().setIntention(CtrlIntention.MOVING, getKeepLocation(), 0);
				}
				else
				{
					_searchPathFind = false;
					_keepLocationMowing = false;
				}
			}
		}
		return false;
	}
	
	private static boolean isManaHeal(Skill skill)
	{
		return ((skill.hasEffectType(EffectType.MANAHEAL) || skill.hasEffectType(EffectType.MANA_HEAL_OVER_TIME) || skill.hasEffectType(EffectType.MANAHEAL_BY_LEVEL) || skill.hasEffectType(EffectType.MANAHEAL_PERCENT)));
	}
	
	private static boolean isHeal(Skill skill)
	{
		return ((skill.hasEffectType(EffectType.HEAL) || skill.hasEffectType(EffectType.HEAL_OVER_TIME) || skill.hasEffectType(EffectType.HEAL_PERCENT)));
	}
	
	public Skill nextSummonAttackSkill(Attackable target, Summon summon, long extraDelay)
	{
		if (getSummonAttackSpells().isEmpty() || !Rnd.chance(getSummonAttackChance()))
		{
			return null;
		}
		
		if (isExtraSummonDelaySkill() && extraDelay > System.currentTimeMillis())
		{
			return null;
		}
		
		final var mpPercent = summon.getCurrentMpPercents();
		if (mpPercent < getSummonAttackPercent())
		{
			return null;
		}
		
		if (isRndSummonAttackSkills())
		{
			return nextSummonRndAttackSkill(target, summon);
		}
		
		Skill skill = null;
		for (final var skillId : getSummonAttackSpells())
		{
			if (summon.isPet())
			{
				skill = PetsParser.getInstance().getPetData(summon.getId()).getAvailableSkill(skillId, summon.getLevel());
			}
			else
			{
				skill = summon.getTemplate().getSkill(skillId);
			}
			
			if (skill != null)
			{
				if (!summon.checkDoCastConditions(skill, false))
				{
					continue;
				}
				
				if (skill.isOffensive() && !skill.isNotTargetAoE() && target == null)
				{
					continue;
				}
				
				if (skill.isForDead() && (target == null || !target.isDead()))
				{
					continue;
				}
				
				if (target != null && summon.getTarget() != target)
				{
					summon.setTarget(target);
				}
				return skill;
			}
		}
		return null;
	}
	
	private Skill nextSummonRndAttackSkill(Attackable target, Summon summon)
	{
		final List<Skill> skillList = new ArrayList<>();
		Skill rndSkill = null;
		Skill skill = null;
		for (final var skillId : getSummonAttackSpells())
		{
			if (summon.isPet())
			{
				skill = PetsParser.getInstance().getPetData(summon.getId()).getAvailableSkill(skillId, summon.getLevel());
			}
			else
			{
				skill = summon.getTemplate().getSkill(skillId);
			}
			
			if (skill != null)
			{
				if (!summon.checkDoCastConditions(skill, false))
				{
					continue;
				}
				
				if (skill.isOffensive() && !skill.isNotTargetAoE() && target == null)
				{
					continue;
				}
				
				if (skill.isForDead() && (target == null || !target.isDead()))
				{
					continue;
				}
				skillList.add(skill);
			}
		}
		
		if (!skillList.isEmpty())
		{
			rndSkill = skillList.get(Rnd.get(skillList.size()));
		}
		skillList.clear();
		if (target != null && summon.getTarget() != target)
		{
			summon.setTarget(target);
		}
		return rndSkill;
	}
	
	public Skill nextSummonSelfSkill(Summon summon, Creature ownerTarget)
	{
		if (getSummonSelfSpells().isEmpty() || !Rnd.chance(getSummonSelfChance()))
		{
			return null;
		}
		
		final var mpPercent = summon.getCurrentMpPercents();
		if (mpPercent < getSummonSelfPercent())
		{
			return null;
		}
		
		if (isRndSummonSelfSkills())
		{
			return nextSummonRndSelfSkill(summon, ownerTarget);
		}
		
		Skill skill = null;
		for (final var skillId : getSummonSelfSpells())
		{
			if (summon.isPet())
			{
				skill = PetsParser.getInstance().getPetData(summon.getId()).getAvailableSkill(skillId, summon.getLevel());
			}
			else
			{
				skill = summon.getTemplate().getSkill(skillId);
			}
			
			if (skill != null)
			{
				if (!summon.checkDoCastConditions(skill, false))
				{
					continue;
				}
				
				if (skill.isToggle() && summon.getFirstEffect(skillId) == null)
				{
					return skill;
				}
				
				if (ownerTarget != null && ownerTarget.getFirstEffect(skillId) == null && skill.getTargetType() != TargetType.SELF && skill.getTargetType() != TargetType.SERVITOR)
				{
					summon.setTarget(ownerTarget);
					return skill;
				}
				
				if (summon.getFirstEffect(skillId) == null)
				{
					return skill;
				}
			}
		}
		return null;
	}
	
	private Skill nextSummonRndSelfSkill(Summon summon, Creature ownerTarget)
	{
		final List<Skill> skillList = new ArrayList<>();
		final List<Skill> skillOwnerList = new ArrayList<>();
		Skill rndSkill = null;
		Skill skill = null;
		for (final var skillId : getSelfSpells())
		{
			if (summon.isPet())
			{
				skill = PetsParser.getInstance().getPetData(summon.getId()).getAvailableSkill(skillId, summon.getLevel());
			}
			else
			{
				skill = summon.getTemplate().getSkill(skillId);
			}
			
			if (skill != null)
			{
				if (!summon.checkDoCastConditions(skill, false))
				{
					continue;
				}
				
				if (skill.isToggle() && summon.getFirstEffect(skillId) == null)
				{
					skillList.add(skill);
					continue;
				}
				
				if (ownerTarget != null && ownerTarget.getFirstEffect(skillId) == null && skill.getTargetType() != TargetType.SELF && skill.getTargetType() != TargetType.SERVITOR)
				{
					skillOwnerList.add(skill);
				}
				
				if (summon.getFirstEffect(skillId) == null)
				{
					skillList.add(skill);
				}
			}
		}
		
		boolean isForSelf = true;
		if (!skillOwnerList.isEmpty())
		{
			rndSkill = skillOwnerList.get(Rnd.get(skillOwnerList.size()));
			isForSelf = false;
		}
		else
		{
			if (!skillList.isEmpty())
			{
				rndSkill = skillList.get(Rnd.get(skillList.size()));
			}
		}
		skillList.clear();
		skillOwnerList.clear();
		
		if (rndSkill == null)
		{
			return null;
		}
		
		if (ownerTarget != null && !isForSelf)
		{
			summon.setTarget(ownerTarget);
		}
		else
		{
			summon.setTarget(summon);
		}
		return rndSkill;
	}
	
	public Skill nextSummonHealSkill(Attackable target, Summon summon, Creature ownerTarget)
	{
		if (getSummonHealSpells().isEmpty() || !Rnd.chance(getSummonLifeChance()))
		{
			return null;
		}
		
		final var hpPercent = summon.getCurrentHpPercents();
		final var ownerHpPercent = ownerTarget != null ? ownerTarget.getCurrentHpPercents() : 100;
		final var ownerMpPercent = ownerTarget != null ? ownerTarget.getCurrentMpPercents() : 100;
		
		final var ownerHeal = ownerHpPercent < getSummonLifePercent();
		final var selfHeal = hpPercent < getSummonLifePercent();
		final var ownerMp = ownerMpPercent < getSummonLifePercent();
		
		if (!ownerHeal && !selfHeal && !ownerMp)
		{
			return null;
		}
		
		if (isRndLifeSkills())
		{
			return nextSummonRndHealSkill(target, summon, ownerTarget);
		}
		
		Skill skill = null;
		for (final var skillId : getSummonHealSpells())
		{
			if (summon.isPet())
			{
				skill = PetsParser.getInstance().getPetData(summon.getId()).getAvailableSkill(skillId, summon.getLevel());
			}
			else
			{
				skill = summon.getTemplate().getSkill(skillId);
			}
			
			if (skill != null)
			{
				if (!summon.checkDoCastConditions(skill, false))
				{
					continue;
				}
				
				if (skill.isOffensive() && target == null)
				{
					continue;
				}
				
				if (isHeal(skill))
				{
					if (!ownerHeal && !selfHeal)
					{
						continue;
					}
					
					if (ownerHeal && ownerTarget != null && !ownerTarget.isDead() && skill.getTargetType() != TargetType.SELF)
					{
						if (skill.getTargetType() == TargetType.SERVITOR && !ownerTarget.isSummon())
						{
							continue;
						}
						summon.setTarget(ownerTarget);
						return skill;
					}
					else if (selfHeal)
					{
						summon.setTarget(summon);
						return skill;
					}
					return null;
				}
				else if (isManaHeal(skill) && ownerTarget != null && ownerMp && !ownerTarget.isDead() && skill.getTargetType() != TargetType.SELF)
				{
					summon.setTarget(ownerTarget);
					return skill;
				}
				return skill;
			}
		}
		return null;
	}
	
	private Skill nextSummonRndHealSkill(Attackable target, Summon summon, Creature ownerTarget)
	{
		final List<Skill> skillList = new ArrayList<>();
		Skill rndSkill = null;
		
		final var hpPercent = summon.getCurrentHpPercents();
		final var ownerHpPercent = ownerTarget != null ? ownerTarget.getCurrentHpPercents() : 100;
		final var ownerMpPercent = ownerTarget != null ? ownerTarget.getCurrentMpPercents() : 100;
		
		final var ownerHeal = ownerHpPercent < getSummonLifePercent();
		final var selfHeal = hpPercent < getSummonLifePercent();
		final var ownerMp = ownerMpPercent < getSummonLifePercent();
		
		if (!ownerHeal && !selfHeal && !ownerMp)
		{
			return null;
		}
		
		Skill skill = null;
		for (final var skillId : getSummonHealSpells())
		{
			if (summon.isPet())
			{
				skill = PetsParser.getInstance().getPetData(summon.getId()).getAvailableSkill(skillId, summon.getLevel());
			}
			else
			{
				skill = summon.getTemplate().getSkill(skillId);
			}
			
			if (skill != null)
			{
				if (!summon.checkDoCastConditions(skill, false))
				{
					continue;
				}
				
				if (skill.isOffensive() && target == null)
				{
					continue;
				}
				
				if (ownerHeal || selfHeal)
				{
					if (isHeal(skill))
					{
						if (ownerHeal)
						{
							if (ownerTarget != null && !ownerTarget.isDead() && skill.getTargetType() != TargetType.SELF)
							{
								if (skill.getTargetType() == TargetType.SERVITOR && !ownerTarget.isSummon())
								{
									continue;
								}
								skillList.add(skill);
							}
						}
						else if (selfHeal)
						{
							skillList.add(skill);
						}
					}
				}
				else if (ownerMp)
				{
					if (isManaHeal(skill) && ownerTarget != null && !ownerTarget.isDead() && skill.getTargetType() != TargetType.SELF)
					{
						skillList.add(skill);
					}
				}
			}
		}
		
		if (!skillList.isEmpty())
		{
			rndSkill = skillList.get(Rnd.get(skillList.size()));
		}
		
		skillList.clear();
		
		if (rndSkill == null)
		{
			return null;
		}
		
		if (ownerHeal || ownerMp)
		{
			summon.setTarget(ownerTarget);
		}
		else
		{
			summon.setTarget(summon);
		}
		return rndSkill;
	}
	
	public Attackable getLeaderTarget(Player leader)
	{
		final var target = leader.getTarget();
		if (target != null && target != leader && target instanceof Attackable)
		{
			if (((Attackable) target).isInTargetList(leader))
			{
				return (Attackable) target;
			}
		}
		return null;
	}
	
	public long getLastFarmOnlineTime()
	{
		return _farmLastOnlineTime;
	}
	
	public boolean isActiveFarmOnlineTime()
	{
		return _activeFarmOnlineTime;
	}
	
	public void setFarmOnlineTime()
	{
		_farmOnlineTime = System.currentTimeMillis();
	}
	
	public void refreshFarmOnlineTime()
	{
		_farmOnlineTime = 0L;
	}
	
	public long getFarmOnlineTime()
	{
		return _farmOnlineTime;
	}
	
	public void checkTargetForRessurect()
	{
		if (!FarmSettings.ALLOW_RESURRECTION)
		{
			stopFarmTask(false);
			return;
		}
		final var isActiveFarm = isAutofarming();
		if (_player.isInParty())
		{
			var foundMember = false;
			for (final var pl : _player.getParty().getMembers())
			{
				if (pl != null && pl.getFarmSystem().isAutofarming() && pl != _player)
				{
					if (pl.getFarmSystem().canRessurect())
					{
						pl.getFarmSystem().setResTarget(_player);
						addWaitTime(System.currentTimeMillis() + 5000L);
						foundMember = true;
						break;
					}
				}
			}
			
			if (!foundMember && isActiveFarm)
			{
				stopFarmTask(false);
				final var cmd = VoicedCommandHandler.getInstance().getHandler("autofarm");
				if (cmd != null)
				{
					cmd.useVoicedCommand("autofarm", _player, "");
					return;
				}
			}
		}
		else
		{
			stopFarmTask(false);
			if (isActiveFarm)
			{
				final var cmd = VoicedCommandHandler.getInstance().getHandler("autofarm");
				if (cmd != null)
				{
					cmd.useVoicedCommand("autofarm", _player, "");
					return;
				}
			}
		}
	}
	
	public boolean canRessurect()
	{
		if (hasResSkills())
		{
			return true;
		}
		
		for (final var item : FarmSettings.RESURRECTION_ITEMS)
		{
			if (_player.getInventory().getItemByItemId(item) != null)
			{
				return true;
			}
		}
		return false;
	}
	
	public void addWaitTime(long time)
	{
		_waitTime = time;
	}
	
	public long getWaitTime()
	{
		return _waitTime;
	}
	
	public boolean isInWaitStatus(Attackable target)
	{
		if (target != null)
		{
			return false;
		}
		return _waitTime > System.currentTimeMillis();
	}
	
	public boolean hasResSkills()
	{
		return _hasResSkills;
	}
	
	public void setResTarget(Player player)
	{
		_resTarget = player;
	}
	
	public Player getResTarget()
	{
		return _resTarget;
	}
	
	public boolean tryResTarget()
	{
		if (_resTarget == null || !_resTarget.isDead())
		{
			_resTarget = null;
			return false;
		}
		
		if (hasResSkills())
		{
			for (final var sk : _player.getSkills().values())
			{
				if (sk != null && sk.getSkillType() == SkillType.RESURRECT && sk.getTargetType() == TargetType.CORPSE_PLAYER && _player.checkDoCastConditions(sk, false))
				{
					_player.setTarget(getResTarget());
					tryUseMagic(sk, false);
					addWaitTime(System.currentTimeMillis() + 3000L);
					return true;
				}
			}
		}
		
		for (final var item : FarmSettings.RESURRECTION_ITEMS)
		{
			final var it = _player.getInventory().getItemByItemId(item);
			if (it != null)
			{
				final var handler = ItemHandler.getInstance().getHandler(it.getEtcItem());
				if (handler != null)
				{
					_player.setTarget(getResTarget());
					handler.useItem(_player, it, false);
					addWaitTime(System.currentTimeMillis() + 3000L);
					return true;
				}
			}
		}
		return false;
	}
	
	public void checkEmptyTime()
	{
		if (_emptyTime == 0)
		{
			_emptyTime = (System.currentTimeMillis() + (FarmSettings.WAIT_TIME * 1000L));
		}
		
		if (System.currentTimeMillis() < _emptyTime)
		{
			return;
		}

		if (_player.isDead())
		{
			stopFarmTask(false);
			_emptyTime = 0L;
			final var cmd = VoicedCommandHandler.getInstance().getHandler("autofarm");
			if (cmd != null)
			{
				cmd.useVoicedCommand("autofarm", _player, "");
				return;
			}
		}
		_emptyTime = 0L;
	}
	
	public void clearEmptyTime()
	{
		_emptyTime = 0L;
		_searchPathFind = false;
	}
	
	private int getHeightRadius()
	{
		final int geoX = GeoEngine.getInstance().getMapX(_player.getX());
		final int geoY = GeoEngine.getInstance().getMapY(_player.getY());
		final String reg = "" + geoX + "_" + geoY + "";
		
		if (FarmSettings.REGIONS_SEARCH.containsKey(reg))
		{
			return FarmSettings.REGIONS_SEARCH.get(reg);
		}
		return 400;
	}
	
	public int getTaskInterval()
	{
		return _taskInterval;
	}
	
	public Player getSelectPartyHealPlayer(Party party)
	{
		if (party == null || getLowLifeSpells().isEmpty())
		{
			return party != null ? party.getLeader() : null;
		}
		
		if (isPartySupport())
		{
			_members.clear();
			_members.addAll(party.getMembers());
			if (!_members.isEmpty())
			{
				Collections.sort(_members, new HealComparator());
				for (final var player : _members)
				{
					if (player != null && player != _player && _player.isInRange(player, 2000) && player.getCurrentHpPercents() < getLifePercent())
					{
						return player;
					}
				}
			}
		}
		return party.getLeader();
	}
	
	private static class HealComparator implements Comparator<Player>
	{
		@Override
		public int compare(Player o1, Player o2)
		{
			if (o1 == null || o2 == null)
			{
				return 0;
			}
			return Double.compare(o1.getCurrentHpPercents(), o2.getCurrentHpPercents());
		}
	}
	
	private static class DistanceComparator implements Comparator<Player>
	{
		private final Player _player;
		
		DistanceComparator(Player player)
		{
			_player = player;
		}
		
		@Override
		public int compare(Player o1, Player o2)
		{
			if (o1 == null || o2 == null)
			{
				return 0;
			}
			return Double.compare(Math.sqrt(_player.getDistanceSq(o1)), Math.sqrt(_player.getDistanceSq(o2)));
		}
	}
	
	public boolean isLocked()
	{
		if (_farmTask != null)
		{
			_abortTarget = true;
			return true;
		}
		return false;
	}
	
	public boolean isAbortTarget()
	{
		return _abortTarget;
	}
	
	public void setAbortTarget(boolean val)
	{
		_abortTarget = val;
	}
}