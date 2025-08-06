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
package l2e.gameserver.model.actor.templates.npc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2e.commons.apache.ArrayUtils;
import l2e.commons.collections.MultiValueSet;
import l2e.gameserver.Config;
import l2e.gameserver.instancemanager.NpcStatManager.CustomStatType;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.templates.character.CharTemplate;
import l2e.gameserver.model.base.ClassId;
import l2e.gameserver.model.interfaces.IIdentifiable;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestEventType;
import l2e.gameserver.model.reward.RewardList;
import l2e.gameserver.model.reward.RewardType;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.skills.SkillType;
import l2e.gameserver.model.skills.effects.EffectType;
import l2e.gameserver.model.skills.targets.TargetType;
import l2e.gameserver.model.stats.StatsSet;

public final class NpcTemplate extends CharTemplate implements IIdentifiable
{
	private static final Logger _log = LoggerFactory.getLogger(NpcTemplate.class);
	
	private final int _npcId;
	private final int _displayId;
	private final String _type;
	private final StatsSet _params;
	private int _level;
	private final int _rewardExp;
	private final int _rewardSp;
	private final int _rewardRp;
	private int _aggroRange;
	private int _hideAggroRange;
	private final int _rHand;
	private final int _lHand;
	private final int _enchantEffect;
	private final int _castleId;
	
	private Race _race;
	private final String _clientClass;
	private final boolean _isCustom;
	
	private final boolean _isQuestMonster;
	private final double _baseVitalityDivider;
	private boolean _isRandomMinons = false;
	
	private String _classAI = null;
	private final ShotsType _shots;
	
	public static enum ShotsType
	{
		NONE, SOUL, SPIRIT, BSPIRIT, SOUL_SPIRIT, SOUL_BSPIRIT
	}
	
	private Faction _faction = Faction.NONE;
	private boolean _isRaid = false;
	private boolean _isEpicRaid = false;
	private boolean _isSiegeGuard = false;
	private boolean _isFlying = false;
	private final boolean _isCommonChest;
	private final boolean _isLethalImmune;
	private Map<RewardType, RewardList> _rewards = Collections.emptyMap();
	
	private final List<MinionData> _minions = new ArrayList<>();
	private List<AbsorbInfo> _absorbInfo = Collections.emptyList();
	private final List<ClassId> _teachInfo = new ArrayList<>();
	private final Map<Integer, Skill> _skills = new HashMap<>();
	
	private Skill[] _damageSkills = new Skill[0];
	private Skill[] _dotSkills = new Skill[0];
	private Skill[] _debuffSkills = new Skill[0];
	private Skill[] _buffSkills = new Skill[0];
	private Skill[] _stunSkills = new Skill[0];
	private Skill[] _healSkills = new Skill[0];
	private Skill[] _suicideSkills = new Skill[0];
	private Skill[] _resSkills = new Skill[0];
	
	private final double _physAttributteMod;
	private final double _magicAttributteMod;
	
	private final Map<QuestEventType, List<Quest>> _questEvents = new ConcurrentHashMap<>();
	
	private MultiValueSet<String> _parameters = StatsSet.EMPTY;
	private Map<CustomStatType, Double> _statsList = null;
	
	public static enum Race
	{
		UNDEAD, MAGICCREATURE, BEAST, ANIMAL, PLANT, HUMANOID, SPIRIT, ANGEL, DEMON, DRAGON, GIANT, BUG, FAIRIE, HUMAN, ELVE, DARKELVE, ORC, DWARVE, OTHER, NONLIVING, SIEGEWEAPON, DEFENDINGARMY, MERCENARIE, UNKNOWN, KAMAEL, NONE
	}
	
	public static boolean isAssignableTo(Class<?> sub, Class<?> clazz)
	{
		if (clazz.isInterface())
		{
			final Class<?>[] interfaces = sub.getInterfaces();
			for (final Class<?> interface1 : interfaces)
			{
				if (clazz.getName().equals(interface1.getName()))
				{
					return true;
				}
			}
		}
		else
		{
			do
			{
				if (sub.getName().equals(clazz.getName()))
				{
					return true;
				}
				
				sub = sub.getSuperclass();
			}
			while (sub != null);
		}
		return false;
	}
	
	public static boolean isAssignableTo(Object obj, Class<?> clazz)
	{
		return NpcTemplate.isAssignableTo(obj.getClass(), clazz);
	}
	
	public NpcTemplate(StatsSet set)
	{
		super(set);
		_params = new StatsSet();
		_npcId = set.getInteger("npcId");
		_displayId = set.getInteger("displayId");
		_type = set.getString("type");
		for (final String lang : Config.MULTILANG_ALLOWED)
		{
			if (lang != null)
			{
				final String name = "name" + lang.substring(0, 1).toUpperCase() + lang.substring(1);
				final String title = "title" + lang.substring(0, 1).toUpperCase() + lang.substring(1);
				_params.set(name, set.getString(name, set.getString("nameEn", "")));
				_params.set(title, set.getString(title, set.getString("titleEn", "")));
			}
		}
		_isQuestMonster = getTitle("en").equalsIgnoreCase("Quest Monster");
		_level = set.getInteger("level");
		_rewardExp = set.getInteger("rewardExp");
		_rewardSp = set.getInteger("rewardSp");
		_rewardRp = set.getInteger("rewardRp");
		_aggroRange = set.getInteger("aggroRange");
		_hideAggroRange = set.getInteger("hideAggroRange", 0);
		_rHand = set.getInteger("rhand", 0);
		_lHand = set.getInteger("lhand", 0);
		_shots = set.getEnum("shots", ShotsType.class, ShotsType.NONE);
		_enchantEffect = set.getInteger("enchant", 0);
		_clientClass = set.getString("texture", "");
		_castleId = set.getInteger("castle_id", 0);
		_baseVitalityDivider = (getLevel() > 0) && (getRewardExp() > 0) ? (getBaseHpMax() * 9 * getLevel() * getLevel()) / (100 * getRewardExp()) : 0;
		_isCommonChest = set.getBool("isCommonChest", false);
		_isLethalImmune = set.getBool("isLethalImmune", false);
		_isCustom = _npcId != _displayId;
		_physAttributteMod = set.getDouble("physAttributteMod", 1.);
		_magicAttributteMod = set.getDouble("magicAttributteMod", 1.);
		
		setAI(set.getString("ai_type"));
	}
	
	public void setParameter(String str, Object val)
	{
		if (_parameters == StatsSet.EMPTY)
		{
			_parameters = new StatsSet();
		}
		_parameters.set(str, val);
	}
	
	public boolean hasParameters()
	{
		return (_parameters != null || _parameters != StatsSet.EMPTY);
	}

	public void setParameters(MultiValueSet<String> set)
	{
		if (set.isEmpty())
		{
			return;
		}

		if (_parameters == StatsSet.EMPTY)
		{
			_parameters = new MultiValueSet<>(set.size());
		}
		_parameters.putAll(set);
	}

	public int getParameter(String str, int val)
	{
		return _parameters.getInteger(str, val);
	}

	public long getParameter(String str, long val)
	{
		return _parameters.getLong(str, val);
	}

	public boolean getParameter(String str, boolean val)
	{
		return _parameters.getBool(str, val);
	}
	
	public boolean getParameter(String str)
	{
		return _parameters.getBool(str);
	}

	public String getParameter(String str, String val)
	{
		return _parameters.getString(str, val);
	}

	public MultiValueSet<String> getParameters()
	{
		return _parameters;
	}
	
	private void setAI(String ai)
	{
		_classAI = ai;
		_isEpicRaid = isType("GrandBoss");
		_isRaid = isType("RaidBoss") || isType("FlyRaidBoss") || isType("LostCaptain");
		_isSiegeGuard = isType("Defender") || isType("FortCommander");
		_isFlying = isType("FlyRaidBoss") || isType("FlyMonster") || isType("FlyNpc");
	}

	public String getAI()
	{
		return _classAI;
	}

	public boolean isRaid()
	{
		return _isRaid;
	}
	
	public boolean isEpicRaid()
	{
		return _isEpicRaid;
	}
	
	public boolean isSiegeGuard()
	{
		return _isSiegeGuard;
	}
	
	public boolean isFlying()
	{
		return _isFlying;
	}

	public void setFaction(Faction faction)
	{
		_faction = faction;
	}

	public Faction getFaction()
	{
		return _faction;
	}
	
	public ShotsType getShots()
	{
		return _shots;
	}
	
	public void setAggroRange(int val)
	{
		_aggroRange = val;
	}
	
	public int getAggroRange()
	{
		return _aggroRange;
	}
	
	public void setHideAggroRange(int val)
	{
		_hideAggroRange = val;
	}
	
	public int getHideAggroRange()
	{
		return _hideAggroRange;
	}
	
	public void addQuestEvent(QuestEventType EventType, Quest q)
	{
		if (!_questEvents.containsKey(EventType))
		{
			final List<Quest> quests = new ArrayList<>();
			quests.add(q);
			_questEvents.put(EventType, quests);
		}
		else
		{
			final List<Quest> quests = _questEvents.get(EventType);
			
			if (!EventType.isMultipleRegistrationAllowed() && !quests.isEmpty() && Config.DEBUG)
			{
				_log.info("Quest event not allowed in multiple quests.  Skipped addition of Event Type \"" + EventType + "\" for NPC \"" + getName(null) + "\" and quest \"" + q.getName() + "\".");
			}
			else
			{
				quests.add(q);
			}
		}
	}
	
	public void removeQuest(Quest q)
	{
		for (final Entry<QuestEventType, List<Quest>> entry : _questEvents.entrySet())
		{
			if (entry.getValue().contains(q))
			{
				final Iterator<Quest> it = entry.getValue().iterator();
				while (it.hasNext())
				{
					final Quest q1 = it.next();
					if (q1 == q)
					{
						it.remove();
					}
				}
				
				if (entry.getValue().isEmpty())
				{
					_questEvents.remove(entry.getKey());
				}
			}
		}
	}
	
	public void addRaidData(MinionData minion, boolean isRandomMinons)
	{
		_minions.add(minion);
		_isRandomMinons = isRandomMinons;
	}
	
	public boolean isRandomMinons()
	{
		return _isRandomMinons;
	}
	
	public void addTeachInfo(List<ClassId> teachInfo)
	{
		_teachInfo.addAll(teachInfo);
	}
	
	public boolean canTeach(ClassId classId)
	{
		return Config.ALT_GAME_SKILL_LEARN ? true : classId.level() == 3 ? _teachInfo.contains(classId.getParent()) : _teachInfo.contains(classId);
	}
	
	public double getBaseVitalityDivider()
	{
		return _baseVitalityDivider;
	}
	
	public String getClientClass()
	{
		return _clientClass;
	}
	
	public int getEnchantEffect()
	{
		return _enchantEffect;
	}
	
	public Map<QuestEventType, List<Quest>> getEventQuests()
	{
		return _questEvents;
	}
	
	public List<Quest> getEventQuests(QuestEventType EventType)
	{
		return _questEvents.get(EventType);
	}

	public int getIdTemplate()
	{
		return _displayId;
	}
	
	public int getLeftHand()
	{
		return _lHand;
	}
	
	public int getLevel()
	{
		return _level;
	}
	
	public void setLevel(int val)
	{
		_level = val;
	}
	
	public List<MinionData> getMinionData()
	{
		return _minions;
	}
	
	@Override
	public int getId()
	{
		return _npcId;
	}
	
	public Race getRace()
	{
		if (_race == null)
		{
			_race = Race.NONE;
		}
		return _race;
	}
	
	public int getRewardExp()
	{
		return _rewardExp;
	}
	
	public int getRewardSp()
	{
		return _rewardSp;
	}

	public int getRewardRp()
	{
		return _rewardRp;
	}
	
	public int getRightHand()
	{
		return _rHand;
	}
	
	public List<ClassId> getTeachInfo()
	{
		return _teachInfo;
	}
	
	public String getName(String lang)
	{
		try
		{
			return _params.getString(lang != null ? "name" + lang.substring(0, 1).toUpperCase() + lang.substring(1) : "name" + Config.MULTILANG_DEFAULT.substring(0, 1).toUpperCase() + Config.MULTILANG_DEFAULT.substring(1));
		}
		catch (final IllegalArgumentException e)
		{
			return "";
		}
	}
	
	public String getTitle(String lang)
	{
		try
		{
			return _params.getString(lang != null ? "title" + lang.substring(0, 1).toUpperCase() + lang.substring(1) : "name" + Config.MULTILANG_DEFAULT.substring(0, 1).toUpperCase() + Config.MULTILANG_DEFAULT.substring(1));
		}
		catch (final IllegalArgumentException e)
		{
			return "";
		}
	}
	
	public void setTitle(String lang, String title)
	{
		_params.set("title" + lang.substring(0, 1).toUpperCase() + lang.substring(1), title);
	}
	
	public StatsSet getParams()
	{
		return _params;
	}
	
	public String getType()
	{
		return _type;
	}
	
	public boolean isCustom()
	{
		return _isCustom;
	}
	
	public boolean isQuestMonster()
	{
		return _isQuestMonster;
	}
	
	public boolean isType(String t)
	{
		return _type.equalsIgnoreCase(t);
	}
	
	public boolean isUndead()
	{
		return _race == Race.UNDEAD;
	}
	
	public void setRace(int raceId)
	{
		switch (raceId)
		{
			case 1 :
				_race = Race.UNDEAD;
				break;
			case 2 :
				_race = Race.MAGICCREATURE;
				break;
			case 3 :
				_race = Race.BEAST;
				break;
			case 4 :
				_race = Race.ANIMAL;
				break;
			case 5 :
				_race = Race.PLANT;
				break;
			case 6 :
				_race = Race.HUMANOID;
				break;
			case 7 :
				_race = Race.SPIRIT;
				break;
			case 8 :
				_race = Race.ANGEL;
				break;
			case 9 :
				_race = Race.DEMON;
				break;
			case 10 :
				_race = Race.DRAGON;
				break;
			case 11 :
				_race = Race.GIANT;
				break;
			case 12 :
				_race = Race.BUG;
				break;
			case 13 :
				_race = Race.FAIRIE;
				break;
			case 14 :
				_race = Race.HUMAN;
				break;
			case 15 :
				_race = Race.ELVE;
				break;
			case 16 :
				_race = Race.DARKELVE;
				break;
			case 17 :
				_race = Race.ORC;
				break;
			case 18 :
				_race = Race.DWARVE;
				break;
			case 19 :
				_race = Race.OTHER;
				break;
			case 20 :
				_race = Race.NONLIVING;
				break;
			case 21 :
				_race = Race.SIEGEWEAPON;
				break;
			case 22 :
				_race = Race.DEFENDINGARMY;
				break;
			case 23 :
				_race = Race.MERCENARIE;
				break;
			case 24 :
				_race = Race.UNKNOWN;
				break;
			case 25 :
				_race = Race.KAMAEL;
				break;
			default :
				_race = Race.NONE;
				break;
		}
	}

	public void putRewardList(RewardType rewardType, RewardList list)
	{
		if (_rewards.isEmpty())
		{
			_rewards = new HashMap<>(RewardType.values().length);
		}
		_rewards.put(rewardType, list);
	}
	
	public RewardList getRewardList(RewardType t)
	{
		return _rewards.get(t);
	}
	
	public Map<RewardType, RewardList> getRewards()
	{
		return _rewards;
	}

	public void addAbsorbInfo(AbsorbInfo absorbInfo)
	{
		if (_absorbInfo.isEmpty())
		{
			_absorbInfo = new ArrayList<>(1);
		}
		_absorbInfo.add(absorbInfo);
	}
	
	public List<AbsorbInfo> getAbsorbInfo()
	{
		return _absorbInfo;
	}

	public boolean isTargetable()
	{
		if (getParameter("noTargetable", false))
		{
			return true;
		}
		return false;
	}

	public boolean isHasNoChatWindow()
	{
		if (getParameter("noChatWindow", false))
		{
			return true;
		}
		return false;
	}

	public boolean isShowName()
	{
		if (getParameter("noShowName", false))
		{
			return true;
		}
		return false;
	}

	public boolean getRandomAnimation()
	{
		if (getParameter("noRandomAnimation", false))
		{
			return true;
		}
		return false;
	}

	public boolean getRandomWalk()
	{
		if (getParameter("noRandomWalk", false))
		{
			return true;
		}
		return false;
	}

	public boolean isMovementDisabled()
	{
		if (getParameter("isMovementDisabled", false))
		{
			return true;
		}
		return false;
	}
	
	public boolean isImmobilized()
	{
		if (getParameter("isImmobilized", false))
		{
			return true;
		}
		return false;
	}
	
	public boolean getCanChampion()
	{
		if (getParameter("noChampion", false))
		{
			return true;
		}
		return false;
	}

	public boolean getCanSeeInSilentMove()
	{
		if (getParameter("canSeeInSilentMove", false))
		{
			return true;
		}
		return false;
	}
	
	@Override
	public Map<Integer, Skill> getSkills()
	{
		return _skills;
	}
	
	public Skill getSkill(int id)
	{
		return _skills.get(id);
	}
	
	public List<Integer> getAllSkills()
	{
		final List<Integer> skills = new ArrayList<>();
		for (final Skill temp : _skills.values())
		{
			if (temp != null && !skills.contains(temp.getId()))
			{
				skills.add(temp.getId());
			}
		}
		return skills;
	}

	public void addSkill(Skill skill)
	{
		if (!_skills.containsKey(skill.getId()))
		{
			_skills.put(skill.getId(), skill);
		}
		
		if (skill.getTargetType() == TargetType.NONE || skill.getSkillType() == SkillType.NOTDONE || !skill.isActive())
		{
			return;
		}
		
		if (skill.isSuicideAttack())
		{
			_suicideSkills = ArrayUtils.add(_suicideSkills, skill);
		}
		else
		{
			switch (skill.getSkillType())
			{
				case PDAM :
				case MANADAM :
				case MDAM :
				case DRAIN :
				case CHARGEDAM :
				case FATAL :
				case DEATHLINK :
				case CPDAMPERCENT :
					_damageSkills = ArrayUtils.add(_damageSkills, skill);
					break;
				case DOT :
				case MDOT :
				case POISON :
				case BLEED :
					_dotSkills = ArrayUtils.add(_dotSkills, skill);
					break;
				case DEBUFF :
				case SLEEP :
				case ROOT :
				case PARALYZE :
				case MUTE :
					_debuffSkills = ArrayUtils.add(_debuffSkills, skill);
					break;
				case BUFF :
					_buffSkills = ArrayUtils.add(_buffSkills, skill);
					break;
				case RESURRECT :
					_resSkills = ArrayUtils.add(_resSkills, skill);
					break;
				case STUN :
					_stunSkills = ArrayUtils.add(_stunSkills, skill);
					break;
				default :
					if (skill.hasEffectType(EffectType.CANCEL, EffectType.CANCEL_ALL, EffectType.CANCEL_BY_SLOT, EffectType.MUTE, EffectType.FEAR, EffectType.SLEEP, EffectType.ROOT, EffectType.PARALYZE, EffectType.NEGATE))
					{
						_debuffSkills = ArrayUtils.add(_debuffSkills, skill);
					}
					else if (skill.hasEffectType(EffectType.HEAL, EffectType.HEAL_OVER_TIME, EffectType.HEAL_PERCENT))
					{
						_healSkills = ArrayUtils.add(_healSkills, skill);
					}
					else if (skill.hasEffectType(EffectType.STUN))
					{
						_stunSkills = ArrayUtils.add(_stunSkills, skill);
					}
					else if (skill.hasEffectType(EffectType.DMG_OVER_TIME, EffectType.DMG_OVER_TIME_PERCENT))
					{
						_dotSkills = ArrayUtils.add(_dotSkills, skill);
					}
					break;
			}
		}
	}
	
	public Skill[] getDamageSkills()
	{
		return _damageSkills;
	}
	
	public Skill[] getDotSkills()
	{
		return _dotSkills;
	}
	
	public Skill[] getDebuffSkills()
	{
		return _debuffSkills;
	}
	
	public Skill[] getBuffSkills()
	{
		return _buffSkills;
	}
	
	public Skill[] getStunSkills()
	{
		return _stunSkills;
	}
	
	public Skill[] getSuicideSkills()
	{
		return _suicideSkills;
	}
	
	public Skill[] getResSkills()
	{
		return _resSkills;
	}
	
	public Skill[] getHealSkills()
	{
		return _healSkills;
	}
	
	public int getCastleId()
	{
		return _castleId;
	}
	
	public boolean isCommonChest()
	{
		return _isCommonChest;
	}
	
	public boolean isLethalImmune()
	{
		return _isLethalImmune;
	}
	
	public void setStatList(Map<CustomStatType, Double> statsList)
	{
		if (_statsList != null)
		{
			_statsList = null;
		}
		_statsList = statsList;
	}
	
	public double getStatValue(CustomStatType type)
	{
		if (_statsList == null || _statsList.isEmpty())
		{
			return 1;
		}
		return _statsList.get(type);
	}
	
	public int getExpReward(Creature attacker)
	{
		return (int) (getRewardExp() * Config.RATE_XP_BY_LVL[attacker.getLevel()] * (attacker.isPlayer() && !attacker.getPremiumBonus().isPersonal() ? (attacker.isInParty() && Config.PREMIUM_PARTY_RATE ? attacker.getParty().getRateXp() : attacker.getPremiumBonus().getRateXp()) : 1));
	}
	
	public int getSpReward(Creature attacker)
	{
		return (int) (getRewardSp() * Config.RATE_SP_BY_LVL[attacker.getLevel()] * (attacker.isPlayer() && !attacker.getPremiumBonus().isPersonal() ? (attacker.isInParty() && Config.PREMIUM_PARTY_RATE ? attacker.getParty().getRateSp() : attacker.getPremiumBonus().getRateSp()) : 1));
	}
	
	public double getPhysAttributteMod()
	{
		return _physAttributteMod;
	}
	
	public double getMagicAttributteMod()
	{
		return _magicAttributteMod;
	}
}