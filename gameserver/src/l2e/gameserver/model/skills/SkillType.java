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

import java.lang.reflect.Constructor;

import l2e.gameserver.model.skills.l2skills.SkillBlow;
import l2e.gameserver.model.skills.l2skills.SkillChargeDmg;
import l2e.gameserver.model.skills.l2skills.SkillDefault;
import l2e.gameserver.model.skills.l2skills.SkillDrain;
import l2e.gameserver.model.skills.l2skills.SkillSiegeFlag;
import l2e.gameserver.model.skills.l2skills.SkillSignet;
import l2e.gameserver.model.skills.l2skills.SkillSignetCasttime;
import l2e.gameserver.model.skills.l2skills.SkillSummon;
import l2e.gameserver.model.stats.StatsSet;

public enum SkillType
{
	// Damage
	PDAM, MDAM, MANADAM, CPDAMPERCENT, DOT, MDOT, DRAIN(SkillDrain.class), DEATHLINK, FATAL, BLOW(SkillBlow.class), SIGNET(SkillSignet.class), SIGNET_CASTTIME(SkillSignetCasttime.class),

	// Disablers
	BLEED, POISON, STUN, ROOT, CONFUSION, FEAR, SLEEP, CONFUSE_MOB_ONLY, MUTE, PARALYZE, DISARM,

	// Aggro
	AGGDAMAGE, AGGREDUCE, AGGREMOVE, AGGREDUCE_CHAR, AGGDEBUFF,

	// Fishing
	FISHING, PUMPING, REELING,

	// MISC
	UNLOCK, UNLOCK_SPECIAL, ENCHANT_ARMOR, ENCHANT_WEAPON, ENCHANT_ATTRIBUTE, SOULSHOT, SPIRITSHOT, SIEGEFLAG(SkillSiegeFlag.class), TAKEFORT, DELUXE_KEY_UNLOCK, SOW, GET_PLAYER, DETECTION, DUMMY, INSTANT_JUMP,

	// Summons
	SUMMON(SkillSummon.class), FEED_PET, ERASE, BETRAY,

	BUFF, DEBUFF, CONT, FUSION,

	RESURRECT, CHARGEDAM(SkillChargeDmg.class), DETECT_TRAP, REMOVE_TRAP,

	// Skill is done within the core.
	COREDONE,

	// Nornil's Power (Nornil's Garden instance)
	NORNILS_POWER,

	// unimplemented
	NOTDONE, BALLISTA, BOMB, CAPTURE,
	
	// Agathions type
	ENERGY_REPLENISH, ENERGY_SPEND, EXTRACT_STONE, CONVERT_ITEM,
	
	NEGATE_EFFECTS;

	private final Class<? extends Skill> _class;

	public Skill makeSkill(StatsSet set)
	{
		try
		{
			final Constructor<? extends Skill> c = _class.getConstructor(StatsSet.class);

			return c.newInstance(set);
		}
		catch (final Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	private SkillType()
	{
		_class = SkillDefault.class;
	}

	private SkillType(Class<? extends Skill> classType)
	{
		_class = classType;
	}
}