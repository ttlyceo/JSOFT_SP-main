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
package l2e.gameserver.model.actor.stat;

import l2e.gameserver.Config;
import l2e.gameserver.data.parser.LimitStatParser;
import l2e.gameserver.instancemanager.NpcStatManager.CustomStatType;
import l2e.gameserver.instancemanager.ZoneManager;
import l2e.gameserver.model.Elementals;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.Summon;
import l2e.gameserver.model.actor.instance.PetInstance;
import l2e.gameserver.model.actor.templates.items.Weapon;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.stats.Calculator;
import l2e.gameserver.model.stats.Env;
import l2e.gameserver.model.stats.MoveType;
import l2e.gameserver.model.stats.Stats;
import l2e.gameserver.model.zone.type.WaterZone;

public class CharStat
{
	private final Creature _activeChar;
	private long _exp = 0;
	private int _sp = 0;
	private int _level = 1;
	private double _oldMaxHp;
	private double _oldMaxMp;

	public CharStat(Creature activeChar)
	{
		_activeChar = activeChar;
	}
	
	public final double calcStat(Stats stat, double init)
	{
		return calcStat(stat, init, null, null);
	}

	public final double calcStat(Stats stat, double init, Creature target, Skill skill)
	{
		double value = init;
		if (stat == null)
		{
			return value;
		}

		final int id = stat.ordinal();

		final Calculator c = _activeChar.getCalculators()[id];

		if ((c == null) || (c.size() == 0))
		{
			return value;
		}
		
		if (getActiveChar().isPlayer() && getActiveChar().isTransformed())
		{
			final double val = getActiveChar().getTransformation().getStat(getActiveChar().getActingPlayer(), stat);
			if (val > 0)
			{
				value = val;
			}
		}

		final Env env = new Env();
		env.setCharacter(_activeChar);
		env.setTarget(target);
		env.setSkill(skill);
		env.setValue(value);

		c.calc(env);
		if (env.getValue() <= 0)
		{
			switch (stat)
			{
				case MAX_HP :
				case MAX_MP :
				case MAX_CP :
				case MAGIC_DEFENCE :
				case POWER_DEFENCE :
				case POWER_ATTACK :
				case MAGIC_ATTACK :
				case POWER_ATTACK_SPEED :
				case MAGIC_ATTACK_SPEED :
				case SHIELD_DEFENCE :
				case STAT_CON :
				case STAT_DEX :
				case STAT_INT :
				case STAT_MEN :
				case STAT_STR :
				case STAT_WIT :
					env.setValue(1);
			}
		}
		return LimitStatParser.getInstance().getStatLimit(getActiveChar(), stat, env.getValue());
	}

	public int getAccuracy()
	{
		return (int) Math.round(calcStat(Stats.ACCURACY_COMBAT, 0, null, null));
	}

	public Creature getActiveChar()
	{
		return _activeChar;
	}

	public float getAttackSpeedMultiplier()
	{
		return (float) ((1.1) * getPAtkSpd() / _activeChar.getTemplate().getBasePAtkSpd());
	}

	public final int getCON()
	{
		return (int) calcStat(Stats.STAT_CON, _activeChar.getTemplate().getBaseCON());
	}

	public double getCriticalDmg(Creature target, double init, Skill skill)
	{
		return calcStat(Stats.CRITICAL_DAMAGE, init, target, skill);
	}

	public double getCriticalHit(Creature target, Skill skill)
	{
		return calcStat(Stats.CRITICAL_RATE, _activeChar.getTemplate().getBaseCritRate(), target, skill);
	}

	public final int getDEX()
	{
		return (int) calcStat(Stats.STAT_DEX, _activeChar.getTemplate().getBaseDEX());
	}

	public int getEvasionRate(Creature target)
	{
		return (int) Math.round(calcStat(Stats.EVASION_RATE, 0, target, null));
	}

	public long getExp()
	{
		return _exp;
	}

	public void setExp(long value)
	{
		_exp = value;
	}

	public int getINT()
	{
		return (int) calcStat(Stats.STAT_INT, _activeChar.getTemplate().getBaseINT());
	}

	public int getLevel()
	{
		return _level;
	}

	public void setLevel(int value)
	{
		_level = value;
	}

	public final int getMagicalAttackRange(Skill skill)
	{
		if (skill != null)
		{
			return (int) calcStat(Stats.MAGIC_ATTACK_RANGE, skill.getCastRange(), null, skill);
		}
		return _activeChar.getTemplate().getBaseAttackRange();
	}

	public double getMaxCp()
	{
		return calcStat(Stats.MAX_CP, _activeChar.getTemplate().getBaseCpMax());
	}

	public int getMaxRecoverableCp()
	{
		return (int) calcStat(Stats.MAX_RECOVERABLE_CP, getMaxCp());
	}

	public double getMaxHp()
	{
		double val = 1;
		if (_activeChar.isSummon())
		{
			final Player owner = ((Summon) _activeChar).getOwner();
			if (_activeChar.isServitor())
			{
				final double addHp = owner != null ? (owner.getMaxHp() * (owner.getServitorShareBonus(Stats.MAX_HP) - 1.0)) : 0;
				val = calcStat(Stats.MAX_HP, (_activeChar.getTemplate().getBaseHpMax() + addHp));
			}
			else
			{
				final double addHp = owner != null ? (owner.getMaxHp() * (owner.getPetShareBonus(Stats.MAX_HP) - 1.0)) : 0;
				val = calcStat(Stats.MAX_HP, (_activeChar.getTemplate().getBaseHpMax() + addHp));
			}
		}
		else
		{
			double bonus = 1;
			if (_activeChar.isNpc())
			{
				bonus *= ((Npc) _activeChar).getTemplate().getStatValue(CustomStatType.MAXHP);
			}
			
			if (_activeChar.getChampionTemplate() != null)
			{
				bonus *= _activeChar.getChampionTemplate().hpMultiplier;
			}
			val = calcStat(Stats.MAX_HP, _activeChar.getTemplate().getBaseHpMax() * bonus);
		}
		
		if (val != _oldMaxHp && _activeChar.isSummon())
		{
			_oldMaxHp = val;
			
			if (_activeChar.getStatus().getCurrentHp() != val)
			{
				_activeChar.getStatus().setCurrentHp(_activeChar.getStatus().getCurrentHp());
			}
		}
		return val;
	}

	public int getMaxRecoverableHp()
	{
		return (int) calcStat(Stats.MAX_RECOVERABLE_HP, getMaxHp());
	}

	public double getMaxMp()
	{
		double val = 1;
		if (_activeChar.isSummon())
		{
			final Player owner = ((Summon) _activeChar).getOwner();
			if (_activeChar.isServitor())
			{
				final double addMp = owner != null ? (owner.getMaxMp() * (owner.getServitorShareBonus(Stats.MAX_MP) - 1.0)) : 0;
				val = calcStat(Stats.MAX_MP, (_activeChar.getTemplate().getBaseMpMax() + addMp));
			}
			else
			{
				final double addMp = owner != null ? (owner.getMaxMp() * (owner.getPetShareBonus(Stats.MAX_MP) - 1.0)) : 0;
				val = calcStat(Stats.MAX_MP, (_activeChar.getTemplate().getBaseMpMax() + addMp));
			}
		}
		else
		{
			double bonus = 1;
			if (_activeChar.isNpc())
			{
				bonus *= ((Npc) _activeChar).getTemplate().getStatValue(CustomStatType.MAXMP);
			}
			val = calcStat(Stats.MAX_MP, _activeChar.getTemplate().getBaseMpMax() * bonus);
		}
		
		if (val != _oldMaxMp && _activeChar.isSummon())
		{
			_oldMaxMp = val;
			
			if (_activeChar.getStatus().getCurrentMp() != val)
			{
				_activeChar.getStatus().setCurrentMp(_activeChar.getStatus().getCurrentMp());
			}
		}
		return val;
	}

	public int getMaxRecoverableMp()
	{
		return (int) calcStat(Stats.MAX_RECOVERABLE_MP, getMaxMp());
	}

	public double getMAtk(Creature target, Skill skill)
	{
		float bonus = 1;
		if (_activeChar.getChampionTemplate() != null)
		{
			bonus = _activeChar.getChampionTemplate().matkMultiplier;
		}
		
		if (_activeChar.isNpc())
		{
			bonus *= ((Npc) _activeChar).getTemplate().getStatValue(CustomStatType.MATK);
		}
		return calcStat(Stats.MAGIC_ATTACK, _activeChar.getTemplate().getBaseMAtk() * bonus, target, skill);
	}

	public double getMAtkSpd()
	{
		double bonus = 1;
		if (_activeChar.getChampionTemplate() != null)
		{
			bonus = _activeChar.getChampionTemplate().matkSpdMultiplier;
		}
		
		if (_activeChar.isNpc())
		{
			bonus *= ((Npc) _activeChar).getTemplate().getStatValue(CustomStatType.MSPEED);
		}

		return calcStat(Stats.MAGIC_ATTACK_SPEED, _activeChar.getTemplate().getBaseMAtkSpd() * bonus) * Config.MATK_SPEED_MULTI;
	}

	public double getMCriticalHit(Creature target, Skill skill)
	{
		return calcStat(Stats.MCRITICAL_RATE, 1, target, skill) * 10;
	}

	public double getMDef(Creature target, Skill skill)
	{
		double bonus = 1;
		if (_activeChar.getChampionTemplate() != null)
		{
			bonus *= _activeChar.getChampionTemplate().mdefMultiplier;
		}

		if (_activeChar.isNpc())
		{
			bonus *= ((Npc) _activeChar).getTemplate().getStatValue(CustomStatType.MDEF);
		}
		return calcStat(Stats.MAGIC_DEFENCE, _activeChar.getTemplate().getBaseMDef() * bonus, target, skill);
	}

	public final int getMEN()
	{
		return (int) calcStat(Stats.STAT_MEN, _activeChar.getTemplate().getBaseMEN());
	}

	public double getMovementSpeedMultiplier()
	{
		double baseSpeed;
		if (_activeChar.isInWater())
		{
			final WaterZone waterZone = ZoneManager.getInstance().getZone(_activeChar, WaterZone.class);
			if (waterZone != null && waterZone.canUseWaterTask())
			{
				baseSpeed = getBaseMoveSpeed(_activeChar.isRunning() ? MoveType.FAST_SWIM : MoveType.SLOW_SWIM);
			}
			else
			{
				baseSpeed = getBaseMoveSpeed(_activeChar.isRunning() ? MoveType.RUN : MoveType.WALK);
			}
		}
		else
		{
			baseSpeed = getBaseMoveSpeed(_activeChar.isRunning() ? MoveType.RUN : MoveType.WALK);
		}
		return getMoveSpeed() * (1. / baseSpeed);
	}
	
	public double getBaseMoveSpeed(MoveType type)
	{
		return _activeChar.getTemplate().getBaseMoveSpeed(type);
	}

	public double getMoveSpeed()
	{
		if (_activeChar.isInWater())
		{
			return _activeChar.isRunning() ? getSwimRunSpeed() : getSwimWalkSpeed();
		}
		return _activeChar.isRunning() ? getRunSpeed() : getWalkSpeed();
	}
	
	public double getRealMoveSpeed(boolean isStillWalking)
	{
		return getMoveSpeed();
	}
	
	public final double getMReuseRate(Skill skill)
	{
		return calcStat(Stats.MAGIC_REUSE_RATE, _activeChar.getTemplate().getBaseMReuseRate(), null, skill);
	}

	public double getPAtk(Creature target)
	{
		double bonus = 1;
		if (_activeChar.getChampionTemplate() != null)
		{
			bonus = _activeChar.getChampionTemplate().patkMultiplier;
		}
		
		if (_activeChar.isNpc())
		{
			bonus *= ((Npc) _activeChar).getTemplate().getStatValue(CustomStatType.PATK);
		}
		return calcStat(Stats.POWER_ATTACK, _activeChar.getTemplate().getBasePAtk() * bonus, target, null);
	}

	public final double getPAtkAnimals(Creature target)
	{
		return calcStat(Stats.PATK_ANIMALS, 1, target, null);
	}

	public final double getPAtkDragons(Creature target)
	{
		return calcStat(Stats.PATK_DRAGONS, 1, target, null);
	}

	public final double getPAtkInsects(Creature target)
	{
		return calcStat(Stats.PATK_INSECTS, 1, target, null);
	}

	public final double getPAtkMonsters(Creature target)
	{
		return calcStat(Stats.PATK_MONSTERS, 1, target, null);
	}

	public final double getPAtkPlants(Creature target)
	{
		return calcStat(Stats.PATK_PLANTS, 1, target, null);
	}

	public final double getPAtkGiants(Creature target)
	{
		return calcStat(Stats.PATK_GIANTS, 1, target, null);
	}

	public final double getPAtkMagicCreatures(Creature target)
	{
		return calcStat(Stats.PATK_MCREATURES, 1, target, null);
	}
	
	public double getPAtkSpd()
	{
		double bonus = 1;
		if (_activeChar.getChampionTemplate() != null)
		{
			bonus *= _activeChar.getChampionTemplate().atkSpdMultiplier;
		}
		
		if (_activeChar.isNpc())
		{
			bonus *= ((Npc) _activeChar).getTemplate().getStatValue(CustomStatType.PSPEED);
		}
		return Math.round(calcStat(Stats.POWER_ATTACK_SPEED, _activeChar.getTemplate().getBasePAtkSpd() * bonus, null, null) * Config.PATK_SPEED_MULTI);
	}

	public final double getPDefAnimals(Creature target)
	{
		return calcStat(Stats.PDEF_ANIMALS, 1, target, null);
	}

	public final double getPDefDragons(Creature target)
	{
		return calcStat(Stats.PDEF_DRAGONS, 1, target, null);
	}

	public final double getPDefInsects(Creature target)
	{
		return calcStat(Stats.PDEF_INSECTS, 1, target, null);
	}

	public final double getPDefMonsters(Creature target)
	{
		return calcStat(Stats.PDEF_MONSTERS, 1, target, null);
	}

	public final double getPDefPlants(Creature target)
	{
		return calcStat(Stats.PDEF_PLANTS, 1, target, null);
	}

	public final double getPDefGiants(Creature target)
	{
		return calcStat(Stats.PDEF_GIANTS, 1, target, null);
	}
	
	public final double getPDefMagicCreatures(Creature target)
	{
		return calcStat(Stats.PDEF_MCREATURES, 1, target, null);
	}
	
	public double getPDef(Creature target)
	{
		double bonus = 1;
		if (_activeChar.getChampionTemplate() != null)
		{
			bonus *= _activeChar.getChampionTemplate().pdefMultiplier;
		}
		
		if (_activeChar.isNpc())
		{
			bonus *= ((Npc) _activeChar).getTemplate().getStatValue(CustomStatType.PDEF);
		}
		return calcStat(Stats.POWER_DEFENCE, _activeChar.getTemplate().getBasePDef() * bonus, target, null);
	}

	public final int getPhysicalAttackRange()
	{
		final Weapon weapon = _activeChar.getActiveWeaponItem();
		int baseAttackRange;
		if (_activeChar.isTransformed() && _activeChar.isPlayer())
		{
			return baseAttackRange = _activeChar.getTransformation().getBaseAttackRange(_activeChar.getActingPlayer());
		}
		else if (weapon != null)
		{
			baseAttackRange = weapon.getBaseAttackRange();
		}
		else
		{
			baseAttackRange = _activeChar.getTemplate().getBaseAttackRange();
		}
		return (int) calcStat(Stats.POWER_ATTACK_RANGE, baseAttackRange, null, null);
	}

	public final double getWeaponReuseModifier(Creature target)
	{
		return calcStat(Stats.ATK_REUSE, 1, target, null);
	}

	public double getRunSpeed()
	{
		final double baseRunSpd = _activeChar.isInWater() ? getSwimRunSpeed() : getBaseMoveSpeed(MoveType.RUN);
		if (baseRunSpd <= 0)
		{
			return 0;
		}
		double bonus = 1;
		if (_activeChar.isPet())
		{
			if (((PetInstance) _activeChar).isUncontrollable())
			{
				bonus = 0.5;
			}
			else if (((PetInstance) _activeChar).isHungry())
			{
				bonus = 0.7;
			}
		}
		return calcStat(Stats.MOVE_SPEED, baseRunSpd * bonus, null, null);
	}
	
	public double getWalkSpeed()
	{
		final double baseWalkSpd = _activeChar.isInWater() ? getSwimWalkSpeed() : getBaseMoveSpeed(MoveType.WALK);
		if (baseWalkSpd <= 0)
		{
			return 0;
		}
		
		double bonus = 1;
		if (_activeChar.isPet())
		{
			if (((PetInstance) _activeChar).isUncontrollable())
			{
				bonus = 0.5;
			}
			else if (((PetInstance) _activeChar).isHungry())
			{
				bonus = 0.7;
			}
		}
		return calcStat(Stats.MOVE_SPEED, baseWalkSpd * bonus);
	}

	public double getSwimRunSpeed()
	{
		final double baseRunSpd = getBaseMoveSpeed(MoveType.FAST_SWIM);
		if (baseRunSpd <= 0)
		{
			return 0;
		}
		return calcStat(Stats.MOVE_SPEED, baseRunSpd, null, null);
	}
	
	public double getSwimWalkSpeed()
	{
		final double baseWalkSpd = getBaseMoveSpeed(MoveType.SLOW_SWIM);
		if (baseWalkSpd <= 0)
		{
			return 0;
		}
		return calcStat(Stats.MOVE_SPEED, baseWalkSpd);
	}

	public final int getShldDef()
	{
		return (int) calcStat(Stats.SHIELD_DEFENCE, 0);
	}

	public int getSp()
	{
		return _sp;
	}

	public void setSp(int value)
	{
		_sp = value;
	}

	public final int getSTR()
	{
		return (int) calcStat(Stats.STAT_STR, _activeChar.getTemplate().getBaseSTR());
	}

	public final int getWIT()
	{
		return (int) calcStat(Stats.STAT_WIT, _activeChar.getTemplate().getBaseWIT());
	}

	public final int[] getMpConsume(Skill skill)
	{
		if (skill == null)
		{
			return new int[]
			{
			        1, 1
			};
		}
		final double defaultConsume = skill.getMpConsume();
		double mpConsume = defaultConsume;
		final double nextDanceMpCost = Math.ceil(skill.getMpConsume() / 2.);
		if (skill.isDance())
		{
			if (Config.DANCE_CONSUME_ADDITIONAL_MP && _activeChar != null && _activeChar.getDanceCount() > 0)
			{
				mpConsume += _activeChar.getDanceCount() * nextDanceMpCost;
			}
		}

		mpConsume = calcStat(Stats.MP_CONSUME, mpConsume, null, skill);

		if (skill.isDance())
		{
			return new int[]
			{
			        (int) defaultConsume, (int) calcStat(Stats.DANCE_MP_CONSUME_RATE, mpConsume)
			};
		}
		else if (skill.isMagic())
		{
			return new int[]
			{
			        (int) defaultConsume, (int) calcStat(Stats.MAGICAL_MP_CONSUME_RATE, mpConsume)
			};
		}
		else
		{
			return new int[]
			{
			        (int) defaultConsume, (int) calcStat(Stats.PHYSICAL_MP_CONSUME_RATE, mpConsume)
			};
		}
	}
	
	public final int getMpInitialConsume(Skill skill)
	{
		if (skill == null)
		{
			return 1;
		}

		final double mpConsume = calcStat(Stats.MP_CONSUME, skill.getMpInitialConsume(), null, skill);

		if (skill.isDance())
		{
			return (int) calcStat(Stats.DANCE_MP_CONSUME_RATE, mpConsume);
		}
		else if (skill.isMagic())
		{
			return (int) calcStat(Stats.MAGICAL_MP_CONSUME_RATE, mpConsume);
		}
		else
		{
			return (int) calcStat(Stats.PHYSICAL_MP_CONSUME_RATE, mpConsume);
		}
	}

	public byte getAttackElement()
	{
		final ItemInstance weaponInstance = _activeChar.getActiveWeaponInstance();
		
		if (weaponInstance != null && weaponInstance.getAttackElementType() >= 0)
		{
			return weaponInstance.getAttackElementType();
		}

		int tempVal = 0;
		final int stats[] =
		{
		        0, 0, 0, 0, 0, 0
		};

		byte returnVal = -2;
		stats[0] = (int) calcStat(Stats.FIRE_POWER, _activeChar.getTemplate().getBaseFire());
		stats[1] = (int) calcStat(Stats.WATER_POWER, _activeChar.getTemplate().getBaseWater());
		stats[2] = (int) calcStat(Stats.WIND_POWER, _activeChar.getTemplate().getBaseWind());
		stats[3] = (int) calcStat(Stats.EARTH_POWER, _activeChar.getTemplate().getBaseEarth());
		stats[4] = (int) calcStat(Stats.HOLY_POWER, _activeChar.getTemplate().getBaseHoly());
		stats[5] = (int) calcStat(Stats.DARK_POWER, _activeChar.getTemplate().getBaseDark());

		for (byte x = 0; x < 6; x++)
		{
			if (stats[x] > tempVal)
			{
				returnVal = x;
				tempVal = stats[x];
			}
		}

		return returnVal;
	}

	public int getAttackElementValue(byte attackAttribute)
	{
		switch (attackAttribute)
		{
			case Elementals.FIRE :
				return (int) calcStat(Stats.FIRE_POWER, _activeChar.getTemplate().getBaseFire());
			case Elementals.WATER :
				return (int) calcStat(Stats.WATER_POWER, _activeChar.getTemplate().getBaseWater());
			case Elementals.WIND :
				return (int) calcStat(Stats.WIND_POWER, _activeChar.getTemplate().getBaseWind());
			case Elementals.EARTH :
				return (int) calcStat(Stats.EARTH_POWER, _activeChar.getTemplate().getBaseEarth());
			case Elementals.HOLY :
				return (int) calcStat(Stats.HOLY_POWER, _activeChar.getTemplate().getBaseHoly());
			case Elementals.DARK :
				return (int) calcStat(Stats.DARK_POWER, _activeChar.getTemplate().getBaseDark());
			default :
				return 0;
		}
	}

	public int getDefenseElementValue(byte defenseAttribute)
	{
		switch (defenseAttribute)
		{
			case Elementals.FIRE :
				return (int) calcStat(Stats.FIRE_RES, _activeChar.getTemplate().getBaseFireRes());
			case Elementals.WATER :
				return (int) calcStat(Stats.WATER_RES, _activeChar.getTemplate().getBaseWaterRes());
			case Elementals.WIND :
				return (int) calcStat(Stats.WIND_RES, _activeChar.getTemplate().getBaseWindRes());
			case Elementals.EARTH :
				return (int) calcStat(Stats.EARTH_RES, _activeChar.getTemplate().getBaseEarthRes());
			case Elementals.HOLY :
				return (int) calcStat(Stats.HOLY_RES, _activeChar.getTemplate().getBaseHolyRes());
			case Elementals.DARK :
				return (int) calcStat(Stats.DARK_RES, _activeChar.getTemplate().getBaseDarkRes());
			default :
				return 0;
		}
	}
	
	public final double getRExp()
	{
		return calcStat(Stats.RUNE_OF_EXP, 1);
	}
	
	public final double getRSp()
	{
		return calcStat(Stats.RUNE_OF_SP, 1);
	}
	
	public double getPvpPhysSkillDmg()
	{
		return calcStat(Stats.PVP_PHYS_SKILL_DMG, 1, null, null);
	}
	
	public double getPvpPhysSkillDef()
	{
		return calcStat(Stats.PVP_PHYS_SKILL_DEF, 1, null, null);
	}
	
	public double getPvpPhysDef()
	{
		return calcStat(Stats.PVP_PHYSICAL_DEF, 1, null, null);
	}
	
	public double getPvpPhysDmg()
	{
		return calcStat(Stats.PVP_PHYSICAL_DMG, 1, null, null);
	}
	
	public double getPvpMagicDmg()
	{
		return calcStat(Stats.PVP_MAGICAL_DMG, 1, null, null);
	}
	
	public double getPvpMagicDef()
	{
		return calcStat(Stats.PVP_MAGICAL_DEF, 1, null, null);
	}
	
	public double getPvePhysDamage()
	{
		return 1.0;
	}
	
	public double getPvePhysDefence()
	{
		return 1.0;
	}
	
	public double getPveMagicDamage()
	{
		return 1.0;
	}
	
	public double getPveMagicDefence()
	{
		return 1.0;
	}
	
	public double getPvpPhysDamage()
	{
		return 1.0;
	}
	
	public double getPvpPhysDefence()
	{
		return 1.0;
	}
	
	public double getPvpMagicDamage()
	{
		return 1.0;
	}
	
	public double getPvpMagicDefence()
	{
		return 1.0;
	}
}