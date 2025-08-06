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
package l2e.gameserver.model.skills;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2e.commons.util.Rnd;
import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.data.parser.SkillBalanceParser;
import l2e.gameserver.data.parser.SkillTreesParser;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.geodata.GeoEngine;
import l2e.gameserver.handler.targethandlers.ITargetTypeHandler;
import l2e.gameserver.handler.targethandlers.TargetHandler;
import l2e.gameserver.instancemanager.DuelManager;
import l2e.gameserver.model.ChanceCondition;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.PcCondOverride;
import l2e.gameserver.model.ShotType;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Playable;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.Summon;
import l2e.gameserver.model.actor.instance.BaseToCaptureInstance;
import l2e.gameserver.model.actor.instance.CubicInstance;
import l2e.gameserver.model.actor.instance.DoorInstance;
import l2e.gameserver.model.actor.instance.GuardInstance;
import l2e.gameserver.model.actor.templates.ExtractableProductItemTemplate;
import l2e.gameserver.model.actor.templates.ExtractableSkillTemplate;
import l2e.gameserver.model.base.SkillChangeType;
import l2e.gameserver.model.entity.Duel;
import l2e.gameserver.model.holders.ItemHolder;
import l2e.gameserver.model.interfaces.IChanceSkillTrigger;
import l2e.gameserver.model.interfaces.IIdentifiable;
import l2e.gameserver.model.skills.conditions.Condition;
import l2e.gameserver.model.skills.effects.Effect;
import l2e.gameserver.model.skills.effects.EffectTemplate;
import l2e.gameserver.model.skills.effects.EffectType;
import l2e.gameserver.model.skills.funcs.Func;
import l2e.gameserver.model.skills.funcs.FuncTemplate;
import l2e.gameserver.model.skills.targets.TargetType;
import l2e.gameserver.model.stats.BaseStats;
import l2e.gameserver.model.stats.Env;
import l2e.gameserver.model.stats.Formulas;
import l2e.gameserver.model.stats.StatsSet;
import l2e.gameserver.model.zone.ZoneId;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.FlyToLocation.FlyType;
import l2e.gameserver.network.serverpackets.SystemMessage;

public abstract class Skill implements IChanceSkillTrigger, IIdentifiable
{
	protected static final Logger _log = LoggerFactory.getLogger(Skill.class.getName());
	
	private static final GameObject[] EMPTY_TARGET_LIST = new GameObject[0];
	private static final Effect[] _emptyEffectSet = new Effect[0];
	private static final Func[] _emptyFunctionSet = new Func[0];
	
	public static final int SKILL_CREATE_DWARVEN = 172;
	public static final int SKILL_EXPERTISE = 239;
	public static final int SKILL_CRYSTALLIZE = 248;
	public static final int SKILL_CLAN_LUCK = 390;
	public static final int SKILL_ONYX_BEAST_TRANSFORMATION = 617;
	public static final int SKILL_CREATE_COMMON = 1320;
	public static final int SKILL_DIVINE_INSPIRATION = 1405;
	public static final int SKILL_NPC_RACE = 4416;
	public static final int SKILL_FISHING_MASTERY = 1315;
	
	public static final int COND_BEHIND = 0x0008;
	public static final int COND_CRIT = 0x0010;
	
	private final int _id;
	private final int _level;
	
	private final int _displayId;
	private final int _displayLevel;
	
	private final StatsSet _params;
	private final SkillOpType _operateType;
	private final int _magic;
	private final TraitType _traitType;
	private final boolean _staticReuse;
	private final boolean _staticDamage;
	private final boolean _ignoreCritDamage;
	private final int _mpConsume;
	private final int _mpInitialConsume;
	private final int _hpConsume;
	private final int _skillInterruptTime;
	private final int _activateRate;
	private final int _levelModifier;
	
	private final int _itemConsumeCount;
	private final int _itemConsumeId;
	
	private final int _castRange;
	private final int _effectRange;
	
	private final int _abnormalLvl;
	private final boolean _isAbnormalInstant;
	private final int _abnormalTime;
	private final boolean _stayAfterDeath;
	private final boolean _stayOnSubclassChange;
	private final int[] _negateCasterId;
	private final int _resurrectTime;
	
	private final int _refId;
	private final int _hitTime;
	private final int[] _hitTimings;
	private final int _coolTime;
	private final int _reuseHashCode;
	private final int _reuseDelay;
	
	private final TargetType _targetType;
	private final int _feed;
	private final double _power;
	private final double _pvpPower;
	private final double _pvePower;
	private final int _magicLevel;
	private final int _lvlBonusRate;
	private final int _minChance;
	private final int _maxChance;
	private final int _blowChance;
	
	private final boolean _isNeutral;
	private final int _affectRange;
	private final int[] _affectLimit = new int[2];
	
	private final SkillType _skillType;
	private final int _effectId;
	private final int _effectLvl;
	
	private final boolean _nextActionIsAttack;
	
	private final boolean _removedOnAnyActionExceptMove;
	private final boolean _removedOnDamage;
	
	private final byte _element;
	private final int _elementPower;
	
	private final BaseStats _saveVs;
	
	private final int _condition;
	private final int _conditionValue;
	private final boolean _overhit;
	
	private final int _minPledgeClass;
	private final boolean _isOffensive;
	private final boolean _isPVP;
	private final int _chargeConsume;
	private final int _triggeredId;
	private final int _triggeredLevel;
	private final String _chanceType;
	private final int _soulMaxConsume;
	private final boolean _dependOnTargetBuff;
	
	private final int _afterEffectId;
	private final int _afterEffectLvl;
	private final boolean _isHeroSkill;
	private final boolean _isGMSkill;
	private final boolean _isSevenSigns;
	
	private final int _baseCritRate;
	private final int _halfKillRate;
	private final int _lethalStrikeRate;
	private final boolean _isTriggeredSkill;
	private final int _aggroPoints;
	
	private List<Condition> _preCondition;
	private List<Condition> _itemPreCondition;
	private FuncTemplate[] _funcTemplates;
	public EffectTemplate[] _effectTemplates;
	private EffectTemplate[] _effectTemplatesSelf;
	private EffectTemplate[] _effectTemplatesPassive;
	
	protected ChanceCondition _chanceCondition = null;
	
	protected FlyType _flyType;
	private final int _flyRadius;
	private final float _flyCourse;
	protected boolean _flyToBack;
	
	private final boolean _isDebuff;
	
	private final String _attribute;
	
	private final boolean _ignoreShield;
	
	private final boolean _isSuicideAttack;
	private final boolean _canBeReflected;
	private final boolean _canBeDispeled;
	
	private final boolean _isClanSkill;
	private final boolean _excludedFromCheck;
	private final boolean _blockActorMove;
	private final boolean _isCustom;
	private final boolean _disableGeoCheck;
	private final boolean _ignoreInvincible;
	private final boolean _blockRemove;
	private final boolean _isBehind;
	private final boolean _isReplaceLimit;
	private boolean _isItemSkill = false;
	private final boolean _isPartyBuff;
	private final boolean _isReflectionBuff;
	private final boolean _isForClanLeader;
	private final boolean _isCantSteal;
	private final boolean _isBlockResetReuse;
	
	private ExtractableSkillTemplate _extractableItems = null;
	
	private int _npcId = 0;
	private final String _icon;
	private byte[] _effectTypes;
	private final int _energyConsume;
	private final boolean _blockSkillMastery;
	private Map<String, Byte> _negateAbnormalType;
	private final int _negateRate;
	private final boolean _petMajorHeal;
	private final boolean _isIgnoreCalcChance;
	private final boolean _farmAttackType;
	private final boolean _farmChanceType;
	private final boolean _farmSelfType;
	private final boolean _farmHealType;
	
	protected Skill(StatsSet set)
	{
		_params = new StatsSet();
		_isAbnormalInstant = set.getBool("abnormalInstant", false);
		_id = set.getInteger("skill_id");
		_level = set.getInteger("level");
		_refId = set.getInteger("referenceId", 0);
		_displayId = set.getInteger("displayId", _id);
		_displayLevel = set.getInteger("displayLevel", _level);
		for (final String lang : Config.MULTILANG_ALLOWED)
		{
			if (lang != null)
			{
				final String name = "name" + lang.substring(0, 1).toUpperCase() + lang.substring(1);
				final String descr = "descr" + lang.substring(0, 1).toUpperCase() + lang.substring(1) + _level;
				_params.set(name, set.getString(name, set.getString("nameEn", "")));
				_params.set(descr, set.getString(descr, set.getString("descrEn" + _level, "")));
			}
		}
		_operateType = set.getEnum("operateType", SkillOpType.class);
		_magic = set.getInteger("isMagic", 0);
		_traitType = set.getEnum("trait", TraitType.class, TraitType.NONE);
		_staticReuse = set.getBool("staticReuse", false);
		_staticDamage = set.getBool("staticDamage", false);
		_ignoreCritDamage = set.getBool("ignoreCritDamage", false);
		_mpConsume = set.getInteger("mpConsume", 0);
		_energyConsume = set.getInteger("energyConsume", 0);
		_mpInitialConsume = set.getInteger("mpInitialConsume", 0);
		_hpConsume = set.getInteger("hpConsume", 0);
		_itemConsumeCount = set.getInteger("itemConsumeCount", 0);
		_itemConsumeId = set.getInteger("itemConsumeId", 0);
		_afterEffectId = set.getInteger("afterEffectId", 0);
		_afterEffectLvl = set.getInteger("afterEffectLvl", 1);
		
		_castRange = set.getInteger("castRange", -1);
		_effectRange = set.getInteger("effectRange", -1);
		
		_abnormalLvl = set.getInteger("abnormalLvl", -1);
		final String negateCasterId = set.getString("negateCasterId", null);
		if (negateCasterId != null)
		{
			final String[] valuesSplit = negateCasterId.split(",");
			_negateCasterId = new int[valuesSplit.length];
			for (int i = 0; i < valuesSplit.length; i++)
			{
				_negateCasterId[i] = Integer.parseInt(valuesSplit[i]);
			}
		}
		else
		{
			_negateCasterId = new int[0];
		}
		
		_abnormalTime = set.getInteger("abnormalTime", 0);
		_attribute = set.getString("attribute", "");
		_resurrectTime = set.getInteger("resurrectTime", 0);
		_stayAfterDeath = set.getBool("stayAfterDeath", false);
		_stayOnSubclassChange = set.getBool("stayOnSubclassChange", true);
		
		_isNeutral = set.getBool("neutral", false);
		_hitTime = set.getInteger("hitTime", 0);
		final String hitTimings = set.getString("hitTimings", null);
		if (hitTimings != null)
		{
			try
			{
				final String[] valuesSplit = hitTimings.split(",");
				_hitTimings = new int[valuesSplit.length];
				for (int i = 0; i < valuesSplit.length; i++)
				{
					_hitTimings[i] = Integer.parseInt(valuesSplit[i]);
				}
			}
			catch (final Exception e)
			{
				throw new IllegalArgumentException("SkillId: " + _id + " invalid hitTimings value: " + hitTimings + ", \"percent,percent,...percent\" required");
			}
		}
		else
		{
			_hitTimings = new int[0];
		}
		
		_coolTime = set.getInteger("coolTime", 0);
		_skillInterruptTime = set.getInteger("hitCancelTime", 500);
		_isDebuff = set.getBool("isDebuff", false);
		_feed = set.getInteger("feed", 0);
		_reuseHashCode = SkillsParser.getSkillHashCode(_id, _level);
		_isReplaceLimit = set.getBool("isReplaceLimit", true);
		if (Config.ENABLE_MODIFY_SKILL_REUSE && Config.SKILL_REUSE_LIST.containsKey(_id))
		{
			if (Config.DEBUG)
			{
				_log.info("*** Skill " + getName(null) + " (" + _level + ") changed reuse from " + set.getInteger("reuseDelay", 0) + " to " + Config.SKILL_REUSE_LIST.get(_id) + " seconds.");
			}
			_reuseDelay = Config.SKILL_REUSE_LIST.get(_id);
		}
		else
		{
			_reuseDelay = set.getInteger("reuseDelay", 0);
		}
		
		_affectRange = set.getInteger("affectRange", 0);
		
		final String affectLimit = set.getString("affectLimit", null);
		if (affectLimit != null)
		{
			try
			{
				final String[] valuesSplit = affectLimit.split("-");
				_affectLimit[0] = Integer.parseInt(valuesSplit[0]);
				_affectLimit[1] = Integer.parseInt(valuesSplit[1]);
			}
			catch (final Exception e)
			{
				throw new IllegalArgumentException("SkillId: " + _id + " invalid affectLimit value: " + affectLimit + ", \"percent-percent\" required");
			}
		}
		
		_targetType = set.getEnum("targetType", TargetType.class);
		_power = set.getFloat("power", 0.f);
		_pvpPower = set.getFloat("pvpPower", (float) getPower());
		_pvePower = set.getFloat("pvePower", (float) getPower());
		_magicLevel = set.getInteger("magicLvl", 0);
		_lvlBonusRate = set.getInteger("lvlBonusRate", 0);
		_minChance = set.getInteger("minChance", Config.MIN_ABNORMAL_STATE_SUCCESS_RATE);
		_maxChance = set.getInteger("maxChance", Config.MAX_ABNORMAL_STATE_SUCCESS_RATE);
		_ignoreShield = set.getBool("ignoreShld", false);
		_skillType = set.getEnum("skillType", SkillType.class, SkillType.DUMMY);
		_effectId = set.getInteger("effectId", 0);
		_effectLvl = set.getInteger("effectLevel", 0);
		
		_nextActionIsAttack = set.getBool("nextActionAttack", false);
		
		_removedOnAnyActionExceptMove = set.getBool("removedOnAnyActionExceptMove", false);
		_removedOnDamage = set.getBool("removedOnDamage", false);
		
		_element = set.getByte("element", (byte) -1);
		_elementPower = set.getInteger("elementPower", 0);
		
		_activateRate = set.getInteger("activateRate", -1);
		_levelModifier = set.getInteger("levelModifier", 1);
		
		_saveVs = set.getEnum("saveVs", BaseStats.class, BaseStats.NULL);
		
		_condition = set.getInteger("condition", 0);
		_conditionValue = set.getInteger("conditionValue", 0);
		_overhit = set.getBool("overHit", false);
		_isSuicideAttack = set.getBool("isSuicideAttack", false);
		
		_minPledgeClass = set.getInteger("minPledgeClass", 0);
		_isOffensive = set.getBool("offensive", false);
		_isPVP = set.getBool("pvp", false);
		_chargeConsume = set.getInteger("chargeConsume", 0);
		_triggeredId = set.getInteger("triggeredId", 0);
		_triggeredLevel = set.getInteger("triggeredLevel", 1);
		_chanceType = set.getString("chanceType", "");
		if (!_chanceType.isEmpty())
		{
			_chanceCondition = ChanceCondition.parse(set);
		}
		
		_soulMaxConsume = set.getInteger("soulMaxConsumeCount", 0);
		_blowChance = set.getInteger("blowChance", 0);
		
		_isHeroSkill = SkillTreesParser.getInstance().isHeroSkill(_id, _level);
		_isGMSkill = SkillTreesParser.getInstance().isGMSkill(_id, _level);
		_isSevenSigns = (_id > 4360) && (_id < 4367);
		_isClanSkill = SkillTreesParser.getInstance().isClanSkill(_id, _level);
		
		_baseCritRate = set.getInteger("baseCritRate", 0);
		_halfKillRate = set.getInteger("halfKillRate", 0);
		_lethalStrikeRate = set.getInteger("lethalStrikeRate", 0);
		
		_isTriggeredSkill = set.getBool("isTriggeredSkill", false);
		_aggroPoints = set.getInteger("aggroPoints", 0);
		
		_flyType = FlyType.valueOf(set.getString("flyType", "NONE").toUpperCase());
		_flyToBack = set.getBool("flyToBack", false);
		_flyRadius = set.getInteger("flyRadius", 0);
		_flyCourse = set.getFloat("flyCourse", 0);
		_canBeReflected = set.getBool("canBeReflected", true);
		_canBeDispeled = set.getBool("canBeDispeled", true);
		
		_excludedFromCheck = set.getBool("excludedFromCheck", false);
		_dependOnTargetBuff = set.getBool("dependOnTargetBuff", false);
		_blockActorMove = set.getBool("blockActorMove", true);
		_isCustom = set.getBool("isCustom", false);
		_disableGeoCheck = set.getBool("disableGeoCheck", false);
		_ignoreInvincible = set.getBool("ignoreInvincible", false);
		final String capsuled_items = set.getString("capsuled_items_skill", null);
		if (capsuled_items != null)
		{
			if (capsuled_items.isEmpty())
			{
				_log.warn("Empty Extractable Item Skill data in Skill Id: " + _id);
			}
			
			_extractableItems = parseExtractableSkill(_id, _level, capsuled_items);
		}
		_npcId = set.getInteger("npcId", 0);
		_icon = set.getString("icon", "icon.skill0000");
		_blockSkillMastery = set.getBool("blockSkillMastery", false);
		
		final String[] negateStackTypeString = set.getString("negateAbnormalType", "").split(";");
		if (negateStackTypeString.length > 0)
		{
			_negateAbnormalType = new ConcurrentHashMap<>();
			for (int i = 0; i < negateStackTypeString.length; i++)
			{
				if (!negateStackTypeString[i].isEmpty())
				{
					final String[] entry = negateStackTypeString[i].split(",");
					_negateAbnormalType.put(entry[0], entry.length > 1 ? Byte.parseByte(entry[1]) : Byte.MAX_VALUE);
				}
			}
		}
		_negateRate = set.getInteger("negateRate", 0);
		_petMajorHeal = set.getBool("petMajorHeal", false);
		_isIgnoreCalcChance = set.getBool("ignoreCalcChance", false);
		_farmAttackType = set.getBool("farmAttackType", false);
		_farmChanceType = set.getBool("farmChanceType", false);
		_farmSelfType = set.getBool("farmSelfType", false);
		_farmHealType = set.getBool("farmHealType", false);
		_blockRemove = set.getBool("isBlockRemove", false);
		_isBehind = set.getBool("isBehind", false);
		_isPartyBuff = set.getBool("isPartyBuff", false);
		_isReflectionBuff = set.getBool("isReflectionBuff", false);
		_isForClanLeader = set.getBool("isForClanLeader", false);
		_isCantSteal = set.getBool("isCantSteal", false);
		_isBlockResetReuse = set.getBool("isBlockResetReuse", false);
	}
	
	public abstract void useSkill(Creature caster, GameObject[] targets);
	
	public final int getConditionValue()
	{
		return _conditionValue;
	}
	
	public final int getCondition()
	{
		return _condition;
	}
	
	public final SkillType getSkillType()
	{
		return _skillType;
	}
	
	public final TraitType getTraitType()
	{
		return _traitType;
	}
	
	public final byte getElement()
	{
		return _element;
	}
	
	public final int getElementPower()
	{
		return _elementPower;
	}
	
	public final TargetType getTargetType()
	{
		return _targetType;
	}
	
	public final boolean isOverhit()
	{
		return _overhit;
	}
	
	public final boolean isSuicideAttack()
	{
		return _isSuicideAttack;
	}
	
	public final boolean allowOnTransform()
	{
		return isPassive();
	}
	
	public final double getPower(Creature activeChar, Creature target, boolean isPvP, boolean isPvE)
	{
		final double power = getPower(isPvP, isPvE);
		if (activeChar == null)
		{
			return power;
		}
		final int targetClassId = activeChar.getTarget() instanceof Player ? activeChar.getTarget().getActingPlayer().getClassId().getId() : -1;
		return power * SkillBalanceParser.getInstance().getSkillValue(getId() + ";" + targetClassId, SkillChangeType.Power, activeChar.getTarget() instanceof Player ? activeChar.getTarget().getActingPlayer() : null);
	}
	
	public final double getPower()
	{
		return _power;
	}
	
	public final double getPvpPower()
	{
		return _pvpPower;
	}
	
	public final int getAbnormalLvl()
	{
		return _abnormalLvl;
	}
	
	public final double getPower(boolean isPvP, boolean isPvE)
	{
		return isPvE ? _pvePower : isPvP ? _pvpPower : _power;
	}
	
	public final boolean isAbnormalInstant()
	{
		return _isAbnormalInstant;
	}
	
	public final int getAbnormalTime()
	{
		return _abnormalTime;
	}
	
	public int getResurrectTime()
	{
		return _resurrectTime;
	}
	
	public final int[] getNegateCasterId()
	{
		return _negateCasterId;
	}
	
	public final int getMagicLevel()
	{
		return _magicLevel;
	}
	
	public final int getLvlBonusRate()
	{
		return _lvlBonusRate;
	}
	
	public final int getMinChance()
	{
		return _minChance;
	}
	
	public final int getMaxChance()
	{
		return _maxChance;
	}
	
	public final boolean isRemovedOnAnyActionExceptMove()
	{
		return _removedOnAnyActionExceptMove;
	}
	
	public final boolean isRemovedOnDamage()
	{
		return _removedOnDamage;
	}
	
	public final int getEffectId()
	{
		return _effectId;
	}
	
	public final int getEffectLvl()
	{
		return _effectLvl;
	}
	
	public final boolean nextActionIsAttack()
	{
		return _nextActionIsAttack;
	}
	
	public final int getCastRange()
	{
		return _castRange;
	}
	
	public final int getEffectRange()
	{
		return _effectRange;
	}
	
	public final int getHpConsume()
	{
		return _hpConsume;
	}
	
	@Override
	public final int getId()
	{
		return _id;
	}
	
	public final boolean isDebuff()
	{
		return _isDebuff;
	}
	
	public final boolean hasDebuffEffects()
	{
		return _isDebuff || hasEffectType(EffectType.STUN) || hasEffectType(EffectType.DEBUFF) || isOffensive() || getSkillType() == SkillType.DEBUFF;
	}
	
	public final boolean hasUnAggroEffects()
	{
		return hasEffectType(EffectType.UNAGGRO);
	}
	
	public int getDisplayId()
	{
		return _displayId;
	}
	
	public int getDisplayLevel()
	{
		return _displayLevel;
	}
	
	public int getTriggeredId()
	{
		return _triggeredId;
	}
	
	public int getTriggeredLevel()
	{
		return _triggeredLevel;
	}
	
	public boolean triggerAnotherSkill()
	{
		return _triggeredId > 1;
	}
	
	public final BaseStats getSaveVs()
	{
		return _saveVs;
	}
	
	public final int getItemConsume()
	{
		return _itemConsumeCount;
	}
	
	public final int getItemConsumeId()
	{
		return _itemConsumeId;
	}
	
	public final int getLevel()
	{
		return _level;
	}
	
	public final boolean isPhysical()
	{
		return _magic == 0;
	}
	
	public final boolean isMagic()
	{
		return _magic == 1;
	}
	
	public final boolean isDance()
	{
		return _magic == 3;
	}
	
	public final boolean isStatic()
	{
		return _magic == 2;
	}
	
	public final boolean isStaticReuse()
	{
		return _staticReuse;
	}
	
	public final boolean isStaticDamage()
	{
		return _staticDamage;
	}
	
	public final boolean isIgnoreCritDamage()
	{
		return _ignoreCritDamage;
	}
	
	public final int getMpConsume()
	{
		return _mpConsume;
	}
	
	public final int getMpInitialConsume()
	{
		return _mpInitialConsume;
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
	
	public String getDescr(String lang)
	{
		try
		{
			return _params.getString(lang != null ? "descr" + lang.substring(0, 1).toUpperCase() + lang.substring(1) + "" + getLevel() + "" : "descr" + Config.MULTILANG_DEFAULT.substring(0, 1).toUpperCase() + Config.MULTILANG_DEFAULT.substring(1) + "" + getLevel() + "");
		}
		catch (final IllegalArgumentException e)
		{
			return "";
		}
	}
	
	public final int getReuseDelay()
	{
		return (int) (_reuseDelay * SkillBalanceParser.getInstance().getSkillValue(getId() + ";-2", SkillChangeType.Reuse, null));
	}
	
	public final int getReuseHashCode()
	{
		return _reuseHashCode;
	}
	
	public final int getHitTime()
	{
		return (int) (_hitTime * SkillBalanceParser.getInstance().getSkillValue(getId() + ";-2", SkillChangeType.CastTime, null));
	}
	
	public final int getHitCounts()
	{
		return _hitTimings.length;
	}
	
	public final int[] getHitTimings()
	{
		return _hitTimings;
	}
	
	public final int getCoolTime()
	{
		return _coolTime;
	}
	
	public final int getAffectRange()
	{
		return _affectRange;
	}
	
	public final int getAffectLimit()
	{
		return (_affectLimit[0] + Rnd.get(_affectLimit[1]));
	}
	
	public final boolean isActive()
	{
		return (_operateType != null) && _operateType.isActive();
	}
	
	public final boolean isPassive()
	{
		return (_operateType != null) && _operateType.isPassive();
	}
	
	public final boolean isToggle()
	{
		return (_operateType != null) && _operateType.isToggle();
	}
	
	public boolean isContinuous()
	{
		return ((_operateType != null) && _operateType.isContinuous()) || isSelfContinuous();
	}

	public boolean isSelfContinuous()
	{
		return (_operateType != null) && _operateType.isSelfContinuous();
	}
	
	public final boolean isChance()
	{
		return (_chanceCondition != null) && isPassive();
	}
	
	public final boolean isTriggeredSkill()
	{
		return _isTriggeredSkill;
	}
	
	public final int getAggroPoints()
	{
		return _aggroPoints;
	}
	
	public final boolean useSoulShot()
	{
		switch (getSkillType())
		{
			case PDAM :
			case FATAL :
			case CHARGEDAM :
			case BLOW :
				return true;
			default :
				return false;
		}
	}
	
	public final boolean useSpiritShot()
	{
		return _magic == 1 || ((getSkillType() == SkillType.PUMPING) || (getSkillType() == SkillType.REELING));
	}
	
	public int getMinPledgeClass()
	{
		return _minPledgeClass;
	}
	
	public final boolean isOffensive()
	{
		return _isOffensive || isPVP();
	}
	
	public final boolean isNeutral()
	{
		return _isNeutral;
	}
	
	public final boolean isPVP()
	{
		return _isPVP;
	}
	
	public final boolean isHeroSkill()
	{
		return _isHeroSkill;
	}
	
	public final boolean isGMSkill()
	{
		return _isGMSkill;
	}
	
	public final boolean is7Signs()
	{
		return _isSevenSigns;
	}
	
	public final int getChargeConsume()
	{
		return _chargeConsume;
	}
	
	public final boolean isChargeBoost()
	{
		return _chargeConsume > 0;
	}
	
	public final int getMaxSoulConsumeCount()
	{
		return _soulMaxConsume;
	}
	
	public final int getBaseCritRate()
	{
		return _baseCritRate;
	}
	
	public final int getHalfKillRate()
	{
		return _halfKillRate;
	}
	
	public final int getLethalStrikeRate()
	{
		return _lethalStrikeRate;
	}
	
	public FlyType getFlyType()
	{
		return _flyType;
	}
	
	public final int getFlyRadius()
	{
		return _flyRadius;
	}

	public boolean isFlyToBack()
	{
		return _flyToBack;
	}
	
	public final float getFlyCourse()
	{
		return _flyCourse;
	}
	
	public final boolean isEffectTypeBattle()
	{
		switch (getSkillType())
		{
			case MDAM :
			case PDAM :
			case CHARGEDAM :
			case BLOW :
			case DEATHLINK :
				return true;
			default :
				return false;
		}
	}
	
	public final boolean isStayAfterDeath()
	{
		return _stayAfterDeath;
	}
	
	public final boolean isStayOnSubclassChange()
	{
		return _stayOnSubclassChange;
	}
	
	public boolean checkCondition(Creature activeChar, GameObject object, boolean itemOrWeapon, boolean printMsg)
	{
		if (activeChar.canOverrideCond(PcCondOverride.SKILL_CONDITIONS) && !activeChar.getAccessLevel().allowSkillRestriction())
		{
			return true;
		}
		
		final List<Condition> preCondition = itemOrWeapon ? _itemPreCondition : _preCondition;
		if ((preCondition == null) || preCondition.isEmpty())
		{
			return true;
		}
		
		final Creature target = (object instanceof Creature) ? (Creature) object : null;
		for (final Condition cond : preCondition)
		{
			final Env env = new Env();
			env.setCharacter(activeChar);
			env.setTarget(target);
			env.setSkill(this);
			
			if (cond != null && !cond.test(env))
			{
				if (printMsg)
				{
					final String msg = cond.getMessage();
					final int msgId = cond.getMessageId();
					if (msgId != 0)
					{
						final SystemMessage sm = SystemMessage.getSystemMessage(msgId);
						if (cond.isAddName())
						{
							sm.addSkillName(_id);
						}
						activeChar.sendPacket(sm);
					}
					else if (msg != null)
					{
						activeChar.sendMessage(msg);
					}
				}
				return false;
			}
		}
		return true;
	}
	
	public int[] checkPercentCondition()
	{
		final var preCondition = _preCondition;
		if ((preCondition == null) || preCondition.isEmpty())
		{
			return null;
		}
		
		for (final var cond : preCondition)
		{
			if (cond.getHpPercent() > 0)
			{
				return new int[]
				{
				        0, cond.getHpPercent()
				};
			}
			else if (cond.getMpPercent() > 0)
			{
				return new int[]
				{
				        1, cond.getMpPercent()
				};
			}
		}
		return null;
	}
	
	public final GameObject[] getTargetList(Creature activeChar, boolean onlyFirst)
	{
		final Creature target = activeChar.getTarget() != null ? (Creature) activeChar.getTarget() : activeChar.isPlayable() ? activeChar.getAI().getCastTarget() : null;
		return getTargetList(activeChar, onlyFirst, target);
	}
	
	public final GameObject[] getTargetList(Creature activeChar, boolean onlyFirst, Creature target)
	{
		final ITargetTypeHandler handler = TargetHandler.getInstance().getHandler(getTargetType());
		if (handler != null)
		{
			try
			{
				return handler.getTargetList(this, activeChar, onlyFirst, target);
			}
			catch (final Exception e)
			{
				_log.warn("Exception in Skill.getTargetList(): " + e.getMessage(), e);
			}
		}
		activeChar.sendMessage("Target type of skill is not currently handled.");
		return EMPTY_TARGET_LIST;
	}
	
	public final GameObject[] getTargetList(Creature activeChar)
	{
		return getTargetList(activeChar, false);
	}
	
	public final GameObject getFirstOfTargetList(Creature activeChar)
	{
		final GameObject[] targets = getTargetList(activeChar, true);
		if (targets == null || targets.length == 0)
		{
			return null;
		}
		return targets[0];
	}
	
	public static final boolean checkForAreaOffensiveSkills(Creature caster, Creature target, Skill skill, boolean sourceInArena, Creature geoTarget)
	{
		if ((target == null) || target.isDead() || (target == caster))
		{
			return false;
		}
		
		final var player = caster.getActingPlayer();
		final var targetPlayer = target.getActingPlayer();
		if (player != null)
		{
			if (targetPlayer != null)
			{
				if ((targetPlayer == caster) || (targetPlayer == player))
				{
					return false;
				}
				
				if (!targetPlayer.isVisibleFor(player) && targetPlayer.isGM())
				{
					return false;
				}
				
				if (targetPlayer.inObserverMode())
				{
					return false;
				}
				
				if (skill.isOffensive() && (player.getSiegeState() > 0) && player.isInsideZone(ZoneId.SIEGE) && (player.getSiegeState() == targetPlayer.getSiegeState()) && (player.getSiegeSide() == targetPlayer.getSiegeSide() && player.isFriend(targetPlayer)))
				{
					return false;
				}
				
				if (skill.isOffensive() && target.isInsideZone(ZoneId.PEACE))
				{
					return false;
				}
				
				if (player.getDuelState() == Duel.DUELSTATE_DUELLING && player.getDuelId() == targetPlayer.getDuelId())
				{
					final var duel = DuelManager.getInstance().getDuel(player.getDuelId());
					if (duel.isPartyDuel())
					{
						final var partyA = player.getParty();
						final var partyB = targetPlayer.getParty();
						
						if (partyA != null)
						{
							if (partyA.getMembers().contains(targetPlayer))
							{
								return false;
							}
						}
							
						if (partyB != null)
						{
							if (partyB.getMembers().contains(player))
							{
								return false;
							}
						}
						return true;
					}
					return true;
				}
				
				if (player.isInSameParty(targetPlayer) || player.isInSameChannel(targetPlayer))
				{
					return false;
				}
				
				for (final var e : player.getFightEvents())
				{
					if (e != null && !e.canUseMagic(targetPlayer, player, skill))
					{
						return false;
					}
				}

				var e = player.getPartyTournament();
				if (e != null && !e.canUseMagic(targetPlayer, player, skill))
				{
					return false;
				}
				
				if (!player.checkPvpSkill(targetPlayer, skill, (caster instanceof Summon)))
				{
					return false;
				}
			}
			else
			{
				if (target instanceof GuardInstance)
				{
					final boolean isCtrlPressed = player.getCurrentSkill() != null && player.getCurrentSkill().isCtrlPressed();
					if (!isCtrlPressed && !sourceInArena)
					{
						return false;
					}
				}
				
				for (final var e : player.getFightEvents())
				{
					if (e != null && !e.canUseMagic(target, player, skill))
					{
						return false;
					}
				}

				var e = player.getPartyTournament();
				if (e != null && !e.canUseMagic(target, player, skill))
				{
					return false;
				}
			}
		}
		else
		{
			if (targetPlayer == null && target.isAttackable() && caster.isAttackable())
			{
				return false;
			}
		}
		
		if (!GeoEngine.getInstance().canSeeTarget(geoTarget, target))
		{
			return false;
		}
		return true;
	}
	
	public static final boolean addSummon(Creature caster, Player owner, int radius, boolean isDead, boolean ownerIsDead)
	{
		if (!owner.hasSummon() || (owner.isDead() && ownerIsDead))
		{
			return false;
		}
		return addCharacter(caster, owner.getSummon(), radius, isDead);
	}
	
	public static final boolean addCharacter(Creature caster, Creature target, int radius, boolean isDead)
	{
		if (isDead != target.isDead())
		{
			return false;
		}
		
		if (caster.isPlayer() && target.isPlayer())
		{
			if (caster.getActingPlayer().isInFightEvent() && target.getActingPlayer().isInFightEvent() && caster.getActingPlayer().getFightEvent().getFightEventPlayer(caster).getTeam() != target.getActingPlayer().getFightEvent().getFightEventPlayer(target).getTeam())
			{
				return false;
			}
		}
		
		if (target.isSummon())
		{
			final var owner = target.getActingPlayer();
			if (owner != null && owner.isDead())
			{
				return false;
			}
		}
		
		if ((radius > 0) && !Util.checkIfInRange(radius, caster, target, true))
		{
			return false;
		}
		
		return true;
		
	}
	
	public final Func[] getStatFuncs(Effect effect, Creature player)
	{
		if (_funcTemplates == null)
		{
			return _emptyFunctionSet;
		}
		
		if (!(player instanceof Playable) && !(player instanceof Attackable))
		{
			return _emptyFunctionSet;
		}
		
		final List<Func> funcs = new ArrayList<>(_funcTemplates.length);
		
		final Env env = new Env();
		env.setCharacter(player);
		env.setSkill(this);
		
		Func f;
		
		for (final FuncTemplate t : _funcTemplates)
		{
			
			f = t.getFunc(env, this);
			if (f != null)
			{
				funcs.add(f);
			}
		}
		if (funcs.isEmpty())
		{
			return _emptyFunctionSet;
		}
		
		return funcs.toArray(new Func[funcs.size()]);
	}
	
	public boolean hasEffects()
	{
		return ((_effectTemplates != null) && (_effectTemplates.length > 0));
	}
	
	public EffectTemplate[] getEffectTemplates()
	{
		return _effectTemplates;
	}
	
	public EffectTemplate[] getEffectTemplatesPassive()
	{
		return _effectTemplatesPassive;
	}
	
	public boolean hasSelfEffects()
	{
		return ((_effectTemplatesSelf != null) && (_effectTemplatesSelf.length > 0));
	}
	
	public boolean hasPassiveEffects()
	{
		return ((_effectTemplatesPassive != null) && (_effectTemplatesPassive.length > 0));
	}
	
	public final Effect[] getEffects(Creature effector, Creature effected, Env env, boolean allowSkillMastery)
	{
		if (!hasEffects() || isPassive())
		{
			return _emptyEffectSet;
		}
		
		if ((effected instanceof DoorInstance) || (effected instanceof BaseToCaptureInstance))
		{
			return _emptyEffectSet;
		}
		
		if (effector != effected)
		{
			if (isOffensive() || isDebuff())
			{
				if (effected.isInvul() && !IsIgnoreInvincible())
				{
					return _emptyEffectSet;
				}
				
				if ((effector instanceof Player) && ((Player) effector).isGM())
				{
					if (!((Player) effector).getAccessLevel().canGiveDamage())
					{
						return _emptyEffectSet;
					}
				}
			}
		}
		
		if (effected.isInvulAgainst(getId(), getLevel()))
		{
			return _emptyEffectSet;
		}
		
		final List<Effect> effects = new ArrayList<>(_effectTemplates.length);
		
		if (env == null)
		{
			env = new Env();
		}
		
		env.setSkillMastery(allowSkillMastery ? Formulas.calcSkillMastery(effector, this) : false);
		env.setCharacter(effector);
		env.setTarget(effected);
		env.setSkill(this);
		
		final var allowReoverlay = (Config.DEBUFF_REOVERLAY && !Config.DEBUFF_REOVERLAY_ONLY_PVE) || (Config.DEBUFF_REOVERLAY && Config.DEBUFF_REOVERLAY_ONLY_PVE && effector.isPlayer() && !effected.isPlayable());
		
		for (final EffectTemplate et : _effectTemplates)
		{
			if (Formulas.calcEffectSuccess(effector, effected, et, this, env.getShield(), env.isSoulShot(), env.isSpiritShot(), env.isBlessedSpiritShot()))
			{
				final Effect e = et.getEffect(env);
				if (e != null)
				{
					if (!e.isReflectable() && (effected == effector) && !effector.isTrap())
					{
						continue;
					}
					
					if (allowReoverlay && !e.getSkill().hasEffectType(EffectType.CANCEL) && e.getAbnormalType() != null && !e.getAbnormalType().equalsIgnoreCase("none"))
					{
						if (e.getSkill().getSkillType() != SkillType.BUFF && !e.getClass().getSimpleName().equalsIgnoreCase("buff"))
						{
							effected.getEffectList().stopSkillEffect(e);
						}
					}
					e.scheduleEffect(true);
					effects.add(e);
				}
			}
			else if (effector.isPlayer())
			{
				final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_RESISTED_YOUR_S2);
				sm.addCharName(effected);
				sm.addSkillName(this);
				((Player) effector).sendPacket(sm);
			}
		}
		
		if (effects.isEmpty())
		{
			return _emptyEffectSet;
		}
		
		return effects.toArray(new Effect[effects.size()]);
	}
	
	public final Effect[] getEffects(Creature effector, Creature effected, boolean allowSkillMastery)
	{
		return getEffects(effector, effected, null, allowSkillMastery);
	}
	
	public final Effect[] getEffects(CubicInstance effector, Creature effected, Env env)
	{
		if (!hasEffects() || isPassive())
		{
			return _emptyEffectSet;
		}
		
		if (effector.getOwner() != effected)
		{
			if (isDebuff() || isOffensive())
			{
				if (effected.isInvul() && !IsIgnoreInvincible())
				{
					return _emptyEffectSet;
				}
				
				if (effector.getOwner().isGM() && !effector.getOwner().getAccessLevel().canGiveDamage())
				{
					return _emptyEffectSet;
				}
			}
		}
		
		final List<Effect> effects = new ArrayList<>(_effectTemplates.length);
		
		if (env == null)
		{
			env = new Env();
		}
		
		env.setCharacter(effector.getOwner());
		env.setCubic(effector);
		env.setTarget(effected);
		env.setSkill(this);
		
		for (final EffectTemplate et : _effectTemplates)
		{
			if (Formulas.calcEffectSuccess(effector.getOwner(), effected, et, this, env.getShield(), env.isSoulShot(), env.isSpiritShot(), env.isBlessedSpiritShot()))
			{
				final Effect e = et.getEffect(env);
				if (e != null)
				{
					e.scheduleEffect(true);
					effects.add(e);
				}
			}
		}
		
		if (effects.isEmpty())
		{
			return _emptyEffectSet;
		}
		
		return effects.toArray(new Effect[effects.size()]);
	}
	
	public final Effect[] getEffectsSelf(Creature effector)
	{
		if (!hasSelfEffects() || isPassive())
		{
			return _emptyEffectSet;
		}
		
		final List<Effect> effects = new ArrayList<>(_effectTemplatesSelf.length);
		
		for (final EffectTemplate et : _effectTemplatesSelf)
		{
			final Env env = new Env();
			env.setCharacter(effector);
			env.setTarget(effector);
			env.setSkill(this);
			final Effect e = et.getEffect(env);
			if (e != null)
			{
				e.setSelfEffect();
				e.scheduleEffect(true);
				effects.add(e);
			}
		}
		if (effects.isEmpty())
		{
			return _emptyEffectSet;
		}
		
		return effects.toArray(new Effect[effects.size()]);
	}
	
	public final Effect[] getEffectsPassive(Creature effector)
	{
		if (!hasPassiveEffects())
		{
			return _emptyEffectSet;
		}
		
		final List<Effect> effects = new ArrayList<>(_effectTemplatesPassive.length);
		
		for (final EffectTemplate et : _effectTemplatesPassive)
		{
			final Env env = new Env();
			env.setCharacter(effector);
			env.setTarget(effector);
			env.setSkill(this);
			final Effect e = et.getEffect(env);
			if (e != null)
			{
				e.setPassiveEffect();
				e.scheduleEffect(true);
				effects.add(e);
			}
		}
		if (effects.isEmpty())
		{
			return _emptyEffectSet;
		}
		
		return effects.toArray(new Effect[effects.size()]);
	}
	
	public final void attach(FuncTemplate f)
	{
		if (_funcTemplates == null)
		{
			_funcTemplates = new FuncTemplate[]
			{
			        f
			};
		}
		else
		{
			final int len = _funcTemplates.length;
			final FuncTemplate[] tmp = new FuncTemplate[len + 1];
			System.arraycopy(_funcTemplates, 0, tmp, 0, len);
			tmp[len] = f;
			_funcTemplates = tmp;
		}
	}
	
	public final void attach(EffectTemplate effect)
	{
		if (_effectTemplates == null)
		{
			_effectTemplates = new EffectTemplate[]
			{
			        effect
			};
		}
		else
		{
			final int len = _effectTemplates.length;
			final EffectTemplate[] tmp = new EffectTemplate[len + 1];
			System.arraycopy(_effectTemplates, 0, tmp, 0, len);
			tmp[len] = effect;
			_effectTemplates = tmp;
		}
		
	}
	
	public final void attachSelf(EffectTemplate effect)
	{
		if (_effectTemplatesSelf == null)
		{
			_effectTemplatesSelf = new EffectTemplate[]
			{
			        effect
			};
		}
		else
		{
			final int len = _effectTemplatesSelf.length;
			final EffectTemplate[] tmp = new EffectTemplate[len + 1];
			System.arraycopy(_effectTemplatesSelf, 0, tmp, 0, len);
			tmp[len] = effect;
			_effectTemplatesSelf = tmp;
		}
	}
	
	public final void attachPassive(EffectTemplate effect)
	{
		if (_effectTemplatesPassive == null)
		{
			_effectTemplatesPassive = new EffectTemplate[]
			{
			        effect
			};
		}
		else
		{
			final int len = _effectTemplatesPassive.length;
			final EffectTemplate[] tmp = new EffectTemplate[len + 1];
			System.arraycopy(_effectTemplatesPassive, 0, tmp, 0, len);
			tmp[len] = effect;
			_effectTemplatesPassive = tmp;
		}
	}
	
	public final void attach(Condition c, boolean itemOrWeapon)
	{
		if (itemOrWeapon)
		{
			if (_itemPreCondition == null)
			{
				_itemPreCondition = new ArrayList<>();
			}
			_itemPreCondition.add(c);
		}
		else
		{
			if (_preCondition == null)
			{
				_preCondition = new ArrayList<>();
			}
			_preCondition.add(c);
		}
	}
	
	@Override
	public String toString()
	{
		return "Skill " + getName(null) + "(" + _id + "," + _level + ")";
	}
	
	public int getFeed()
	{
		return _feed;
	}
	
	public int getReferenceItemId()
	{
		return _refId;
	}
	
	public int getAfterEffectId()
	{
		return _afterEffectId;
	}
	
	public int getAfterEffectLvl()
	{
		return _afterEffectLvl;
	}
	
	@Override
	public boolean triggersChanceSkill()
	{
		return (_triggeredId > 0) && isChance();
	}
	
	@Override
	public int getTriggeredChanceId()
	{
		return _triggeredId;
	}
	
	@Override
	public int getTriggeredChanceLevel()
	{
		return _triggeredLevel;
	}
	
	@Override
	public ChanceCondition getTriggeredChanceCondition()
	{
		return _chanceCondition;
	}
	
	public String getAttributeName()
	{
		return _attribute;
	}
	
	public int getBlowChance()
	{
		return _blowChance;
	}
	
	public boolean ignoreShield()
	{
		return _ignoreShield;
	}
	
	public boolean canBeReflected()
	{
		return _canBeReflected;
	}
	
	public boolean canBeDispeled()
	{
		return _canBeDispeled;
	}
	
	public boolean isClanSkill()
	{
		return _isClanSkill;
	}
	
	public boolean isExcludedFromCheck()
	{
		return _excludedFromCheck;
	}
	
	public boolean getDependOnTargetBuff()
	{
		return _dependOnTargetBuff;
	}
	
	public boolean isBlockActorMove()
	{
		return _blockActorMove;
	}
	
	public boolean isCustom()
	{
		return _isCustom;
	}
	
	public boolean isDisableGeoCheck()
	{
		return _disableGeoCheck;
	}
	
	private ExtractableSkillTemplate parseExtractableSkill(int skillId, int skillLvl, String values)
	{
		final String[] prodLists = values.split(";");
		final List<ExtractableProductItemTemplate> products = new ArrayList<>();
		String[] prodData;
		String[] enchantData;
		for (final String prodList : prodLists)
		{
			prodData = prodList.split(",");
			enchantData = prodList.split("/");
			if (prodData.length < 3)
			{
				_log.warn("Extractable skills data: Error in Skill Id: " + skillId + " Level: " + skillLvl + " -> wrong seperator!");
			}
			List<ItemHolder> items = null;
			double chance = 0;
			int prodId = 0;
			int enchantLevel = 0;
			long min, max = 0;
			final int lenght = prodData.length - 1;
			try
			{
				items = new ArrayList<>(lenght / 2);
				for (int j = 0; j < lenght; j++)
				{
					prodId = Integer.parseInt(prodData[j]);
					final String amount = prodData[j += 1];
					final String[] count = amount.split("-");
					if (count.length == 2)
					{
						min = Long.parseLong(count[0]);
						max = Long.parseLong(count[1]);
					}
					else
					{
						min = Long.parseLong(count[0]);
						max = min;
					}
					
					if (min <= 0 && max <= 0)
					{
						_log.warn("Extractable skills data: Error in Skill Id: " + skillId + " Level: " + skillLvl + " wrong production Id: " + prodId + " or wrond quantity: " + min + "!");
					}
					items.add(new ItemHolder(prodId, min, max));
				}
				chance = Double.parseDouble(prodData[lenght]);
				enchantLevel = enchantData != null && enchantData.length == 2 ? Integer.parseInt(enchantData[1]) : 0;
				if (enchantLevel > 0 && !items.isEmpty())
				{
					for (final var item : items)
					{
						if (item != null)
						{
							item.setEnchatLevel(enchantLevel);
						}
					}
				}
			}
			catch (final Exception e)
			{
				_log.warn("Extractable skills data: Error in Skill Id: " + skillId + " Level: " + skillLvl + " -> incomplete/invalid production data or wrong seperator!");
			}
			products.add(new ExtractableProductItemTemplate(items, chance, enchantLevel));
		}
		
		if (products.isEmpty())
		{
			_log.warn("Extractable skills data: Error in Skill Id: " + skillId + " Level: " + skillLvl + " -> There are no production items!");
		}
		return new ExtractableSkillTemplate(SkillsParser.getSkillHashCode(skillId, skillLvl), products);
	}
	
	public ExtractableSkillTemplate getExtractableSkill()
	{
		return _extractableItems;
	}
	
	public final int getActivateRate()
	{
		return _activateRate;
	}
	
	public final int getLevelModifier()
	{
		return _levelModifier;
	}
	
	public int getNpcId()
	{
		return _npcId;
	}
	
	public boolean hasEffectType(EffectType... types)
	{
		if (hasEffects() && (types != null) && (types.length > 0))
		{
			if (_effectTypes == null)
			{
				_effectTypes = new byte[_effectTemplates.length];
				
				final Env env = new Env();
				env.setSkill(this);
				
				int i = 0;
				for (final EffectTemplate et : _effectTemplates)
				{
					final Effect e = et.getEffect(env, true);
					if (e == null)
					{
						continue;
					}
					_effectTypes[i++] = (byte) e.getEffectType().ordinal();
				}
				Arrays.sort(_effectTypes);
			}
			
			for (final EffectType type : types)
			{
				if (Arrays.binarySearch(_effectTypes, (byte) type.ordinal()) >= 0)
				{
					return true;
				}
			}
		}
		return false;
	}
	
	public String getIcon()
	{
		return _icon;
	}
	
	public int getEnergyConsume()
	{
		return _energyConsume;
	}

	public static boolean getBlockBuffConditions(Creature activeChar, Creature aimingTarget)
	{
		if (activeChar.isPlayable() && activeChar.getActingPlayer() != null)
		{
			if (aimingTarget.isMonster())
			{
				return false;
			}
			
			final Player player = activeChar.getActingPlayer();
			if (aimingTarget.isPlayable() && aimingTarget.getActingPlayer() != null)
			{
				final Player target = aimingTarget.getActingPlayer();

				if (player.getParty() != null && target.getParty() != null && player.getParty() == target.getParty())
				{
					return true;
				}
				if (player.getClan() != null && target.getClan() != null)
				{
					if (player.getClan().getId() == target.getClan().getId() && !player.isInOlympiadMode())
					{
						return true;
					}
					if (player.getAllyId() > 0 && target.getAllyId() > 0 && player.getAllyId() == target.getAllyId() && !player.isInOlympiadMode())
					{
						return true;
					}
				}
				if (player.isInOlympiadMode() && player.getOlympiadSide() == target.getOlympiadSide())
				{
					return true;
				}
				if (player.getTeam() != 0 && target.getTeam() != 0 && player.getTeam() == target.getTeam())
				{
					return true;
				}
				if (player.isInSiege())
				{
					return false;
				}
				if (player.isInsideZone(ZoneId.PVP))
				{
					return false;
				}
				if (player.isInZonePeace())
				{
					return false;
				}
				if (target.getKarma() > 0)
				{
					return false;
				}
			}
		}
		return false;
	}
	
	public boolean isNotTargetAoE()
	{
		switch (_targetType)
		{
			case AURA :
			case FRONT_AURA :
			case BEHIND_AURA :
			case PARTY_CLAN :
			case CLAN :
			case PARTY_MEMBER :
			case PARTY :
				return true;
			default :
				return false;
		}
	}
	
	public boolean oneTarget()
	{
		switch (_targetType)
		{
			case CORPSE :
			case CORPSE_PLAYER :
			case HOLY :
			case FLAGPOLE :
			case NONE :
			case ONE :
			case PARTY_MEMBER :
			case SERVITOR :
			case SUMMON :
			case PET :
			case OWNER_PET :
			case ENEMY_SUMMON :
			case SELF :
			case UNLOCKABLE :
				return true;
			default :
				return false;
		}
	}
	
	public boolean isAura()
	{
		switch (_targetType)
		{
			case AURA :
			case FRONT_AURA :
			case BEHIND_AURA :
			case AURA_CORPSE_MOB :
			case AURA_FRIENDLY :
			case AURA_DOOR :
			case AURA_FRIENDLY_SUMMON :
			case AURA_UNDEAD_ENEMY :
			case AURA_MOB :
			case AURA_DEAD_MOB :
				return true;
			default :
				return false;
		}
	}
	
	public boolean isArea()
	{
		switch (_targetType)
		{
			case AREA :
			case AREA_FRIENDLY :
			case AREA_CORPSE_MOB :
			case AREA_SUMMON :
			case AREA_UNDEAD :
			case BEHIND_AREA :
			case FRONT_AREA :
				return true;
			default :
				return false;
		}
	}
	
	public boolean isForDead()
	{
		switch (_targetType)
		{
			case CORPSE :
			case CORPSE_PLAYER :
			case AURA_CORPSE_MOB :
			case AREA_CORPSE_MOB :
			case AURA_DEAD_MOB :
				return true;
			default :
				return false;
		}
	}
	
	public double getSimpleDamage(Creature attacker, Creature target)
	{
		if (isMagic())
		{
			final double mAtk = attacker.getMAtk(target, this);
			final double mdef = target.getMDef(null, this);
			final double power = getPower();
			final int sps = (attacker.isChargedShot(ShotType.SPIRITSHOTS) || attacker.isChargedShot(ShotType.BLESSED_SPIRITSHOTS)) && useSpiritShot() ? 2 : 1;
			return 91 * power * Math.sqrt(sps * mAtk) / mdef;
		}
		final double pAtk = attacker.getPAtk(target);
		final double pdef = target.getPDef(attacker);
		final double power = getPower();
		final int ss = attacker.isChargedShot(ShotType.SOULSHOTS) && useSpiritShot() ? 2 : 1;
		return ss * (pAtk + power) * 70. / pdef;
	}
	
	public final int getAOECastRange()
	{
		return Math.max(_castRange, _effectRange);
	}
	
	public boolean isHealingPotionSkill()
	{
		switch (getId())
		{
			case 2031 :
			case 2032 :
			case 2037 :
			case 26025 :
			case 26026 :
				return true;
		}
		return false;
	}
	
	public boolean isPotion()
	{
		switch (getId())
		{
			case 2031 :
			case 2032 :
			case 2037 :
			case 2335 :
			case 2336 :
			case 2337 :
			case 2338 :
			case 2339 :
			case 2340 :
			case 26025 :
			case 26026 :
				return true;
		}
		return false;
	}
	
	public boolean isDeathlink()
	{
		return _skillType == SkillType.DEATHLINK || _id == 314;
	}
	
	public final int getSkillInterruptTime()
	{
		return _skillInterruptTime;
	}
	
	public boolean isBlockSkillMastery()
	{
		return _blockSkillMastery;
	}
	
	public boolean IsIgnoreInvincible()
	{
		return _ignoreInvincible;
	}
	
	public boolean isSpoilSkill()
	{
		switch (getId())
		{
			case 254 :
			case 302 :
			case 348 :
			case 537 :
			case 947 :
				return true;
		}
		return false;
	}
	
	public boolean isSweepSkill()
	{
		switch (getId())
		{
			case 42 :
			case 444 :
				return true;
		}
		return false;
	}
	
	public boolean isHandler()
	{
		return ((_id >= 2000 && _id <= 9100) && _itemConsumeCount > 0);
	}
	
	public Map<String, Byte> getNegateAbnormalTypes()
	{
		return _negateAbnormalType;
	}
	
	public int getNegateRate()
	{
		return _negateRate;
	}
	
	public boolean isItemSkill()
	{
		return _isItemSkill;
	}
	
	public void setItemSkill(boolean value)
	{
		_isItemSkill = value;
	}
	
	public boolean isPetMajorHeal()
	{
		return _petMajorHeal;
	}
	
	public boolean isIgnoreCalcChance()
	{
		return _isIgnoreCalcChance;
	}
	
	public boolean isAttackSkill()
	{
		if (isFarmAttackType())
		{
			return true;
		}
		
		switch (getId())
		{
			case 484 :
			case 994 :
			case 998 :
				return true;
		}
		
		switch (getSkillType())
		{
			case AGGDAMAGE :
			case BLOW :
			case PDAM :
			case MANADAM :
			case MDAM :
			case DRAIN :
			case CHARGEDAM :
			case FATAL :
			case DEATHLINK :
			case CPDAMPERCENT :
			case STUN :
				return true;
		}
		
		if (hasEffectType(EffectType.STUN))
		{
			return true;
		}
		return false;
	}
	
	public boolean isChanceSkill()
	{
		if (isFarmChanceType())
		{
			return true;
		}
		
		switch (getId())
		{
			case 42 :
			case 444 :
			case 254 :
			case 302 :
			case 348 :
			case 537 :
			case 947 :
			case 1263 :
				return true;
		}
		
		switch (getSkillType())
		{
			case DOT :
			case MDOT :
			case POISON :
			case BLEED :
			case DEBUFF :
			case SLEEP :
			case ROOT :
			case PARALYZE :
			case MUTE :
				return true;
		}
		return false;
	}
	
	public boolean isNotSelfSkill()
	{
		if (isFarmSelfType())
		{
			return false;
		}
		return (!isToggle() && !isDance() && !hasEffectType(EffectType.BUFF) && getSkillType() != SkillType.BUFF && !hasEffectType(EffectType.SUMMON_CUBIC));
	}
	
	public boolean isNotNotHealSkill()
	{
		if (isFarmHealType())
		{
			return false;
		}
		return (getSkillType() != SkillType.DRAIN && !hasEffectType(EffectType.HEAL) && !hasEffectType(EffectType.HEAL_OVER_TIME) && !hasEffectType(EffectType.HEAL_PERCENT) && !hasEffectType(EffectType.MANAHEAL) && !hasEffectType(EffectType.MANA_HEAL_OVER_TIME) && !hasEffectType(EffectType.MANAHEAL_BY_LEVEL) && !hasEffectType(EffectType.MANAHEAL_PERCENT));
	}
	
	public boolean isFarmAttackType()
	{
		return _farmAttackType;
	}
	
	public boolean isFarmChanceType()
	{
		return _farmChanceType;
	}
	
	public boolean isFarmSelfType()
	{
		return _farmSelfType;
	}
	
	public boolean isFarmHealType()
	{
		return _farmHealType;
	}
	
	public boolean isBlockRemove()
	{
		return _blockRemove;
	}
	
	public boolean isBehind()
	{
		return _isBehind;
	}
	
	public boolean isPartyBuff()
	{
		return _isPartyBuff;
	}
	
	public boolean isReflectionBuff()
	{
		return _isReflectionBuff;
	}
	
	public final boolean isReplaceLimit()
	{
		return _isReplaceLimit;
	}
	
	public FuncTemplate[] getFuncTemplates()
	{
		return _funcTemplates;
	}
	
	public EffectTemplate[] getEffectFuncTemplates()
	{
		return _effectTemplates;
	}
	
	public EffectTemplate[] getSelfEffectFuncTemplates()
	{
		return _effectTemplatesSelf;
	}
	
	public EffectTemplate[] getPassiveEffectFuncTemplates()
	{
		return _effectTemplatesPassive;
	}
	
	public boolean calcCriticalBlow(Creature caster, Creature target)
	{
		return false;
	}
	
	public boolean isForClanLeader()
	{
		return _isForClanLeader;
	}
	
	public boolean isCantSteal()
	{
		return _isCantSteal;
	}
	
	public boolean isBlockResetReuse()
	{
		return _isBlockResetReuse;
	}
}