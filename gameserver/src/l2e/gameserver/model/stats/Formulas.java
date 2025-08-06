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
package l2e.gameserver.model.stats;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2e.commons.util.PositionUtils;
import l2e.commons.util.PositionUtils.TargetDirection;
import l2e.commons.util.Rnd;
import l2e.commons.util.StringUtil;
import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.SevenSigns;
import l2e.gameserver.SevenSignsFestival;
import l2e.gameserver.data.parser.ClassBalanceParser;
import l2e.gameserver.data.parser.HitConditionBonusParser;
import l2e.gameserver.data.parser.SkillBalanceParser;
import l2e.gameserver.instancemanager.CastleManager;
import l2e.gameserver.instancemanager.ClanHallManager;
import l2e.gameserver.instancemanager.FortManager;
import l2e.gameserver.instancemanager.SiegeManager;
import l2e.gameserver.instancemanager.ZoneManager;
import l2e.gameserver.model.Elementals;
import l2e.gameserver.model.ShotType;
import l2e.gameserver.model.SiegeClan;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.CubicInstance;
import l2e.gameserver.model.actor.instance.EffectPointInstance;
import l2e.gameserver.model.actor.instance.FortCommanderInstance;
import l2e.gameserver.model.actor.instance.PetInstance;
import l2e.gameserver.model.actor.templates.items.Armor;
import l2e.gameserver.model.actor.templates.items.Item;
import l2e.gameserver.model.actor.templates.items.Weapon;
import l2e.gameserver.model.actor.templates.npc.champion.ChampionTemplate;
import l2e.gameserver.model.base.AttackType;
import l2e.gameserver.model.base.SkillChangeType;
import l2e.gameserver.model.entity.Castle;
import l2e.gameserver.model.entity.ClanHall;
import l2e.gameserver.model.entity.Fort;
import l2e.gameserver.model.entity.Siege;
import l2e.gameserver.model.items.type.ArmorType;
import l2e.gameserver.model.items.type.WeaponType;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.skills.SkillType;
import l2e.gameserver.model.skills.TraitType;
import l2e.gameserver.model.skills.effects.Effect;
import l2e.gameserver.model.skills.effects.EffectTemplate;
import l2e.gameserver.model.skills.effects.EffectType;
import l2e.gameserver.model.skills.funcs.formulas.FuncArmorSet;
import l2e.gameserver.model.skills.funcs.formulas.FuncAtkAccuracy;
import l2e.gameserver.model.skills.funcs.formulas.FuncAtkCritical;
import l2e.gameserver.model.skills.funcs.formulas.FuncAtkEvasion;
import l2e.gameserver.model.skills.funcs.formulas.FuncGatesMDefMod;
import l2e.gameserver.model.skills.funcs.formulas.FuncGatesPDefMod;
import l2e.gameserver.model.skills.funcs.formulas.FuncHenna;
import l2e.gameserver.model.skills.funcs.formulas.FuncMAtkCritical;
import l2e.gameserver.model.skills.funcs.formulas.FuncMAtkMod;
import l2e.gameserver.model.skills.funcs.formulas.FuncMAtkSpeed;
import l2e.gameserver.model.skills.funcs.formulas.FuncMDefMod;
import l2e.gameserver.model.skills.funcs.formulas.FuncMaxCpMul;
import l2e.gameserver.model.skills.funcs.formulas.FuncMaxHpMul;
import l2e.gameserver.model.skills.funcs.formulas.FuncMaxMpMul;
import l2e.gameserver.model.skills.funcs.formulas.FuncMoveSpeed;
import l2e.gameserver.model.skills.funcs.formulas.FuncPAtkMod;
import l2e.gameserver.model.skills.funcs.formulas.FuncPAtkSpeed;
import l2e.gameserver.model.skills.funcs.formulas.FuncPDefMod;
import l2e.gameserver.model.strings.server.ServerMessage;
import l2e.gameserver.model.zone.ZoneId;
import l2e.gameserver.model.zone.type.CastleZone;
import l2e.gameserver.model.zone.type.ClanHallZone;
import l2e.gameserver.model.zone.type.FortZone;
import l2e.gameserver.model.zone.type.MotherTreeZone;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.StatusUpdate;
import l2e.gameserver.network.serverpackets.SystemMessage;

public final class Formulas
{
	private static final Logger _log = LoggerFactory.getLogger(Formulas.class);

	public static final byte SHIELD_DEFENSE_FAILED = 0;
	public static final byte SHIELD_DEFENSE_SUCCEED = 1;
	public static final byte SHIELD_DEFENSE_PERFECT_BLOCK = 2;
	
	public static final byte SKILL_REFLECT_FAILED = 0;
	public static final byte SKILL_REFLECT_SUCCEED = 1;
	public static final byte SKILL_REFLECT_VENGEANCE = 2;

	private static final byte MELEE_ATTACK_RANGE = 40;
	
	public static long getRegeneratePeriod(Creature cha)
	{
		return cha.isDoor() ? 300000 : (Config.REGEN_MAIN_INTERVAL + Rnd.get(Config.REGEN_MIN_RND, Config.REGEN_MAX_RND));
	}

	public static Calculator[] getStdNPCCalculators()
	{
		final Calculator[] std = new Calculator[Stats.NUM_STATS];

		std[Stats.MAX_HP.ordinal()] = new Calculator();
		std[Stats.MAX_HP.ordinal()].addFunc(FuncMaxHpMul.getInstance());

		std[Stats.MAX_MP.ordinal()] = new Calculator();
		std[Stats.MAX_MP.ordinal()].addFunc(FuncMaxMpMul.getInstance());

		std[Stats.POWER_ATTACK.ordinal()] = new Calculator();
		std[Stats.POWER_ATTACK.ordinal()].addFunc(FuncPAtkMod.getInstance());

		std[Stats.MAGIC_ATTACK.ordinal()] = new Calculator();
		std[Stats.MAGIC_ATTACK.ordinal()].addFunc(FuncMAtkMod.getInstance());

		std[Stats.POWER_DEFENCE.ordinal()] = new Calculator();
		std[Stats.POWER_DEFENCE.ordinal()].addFunc(FuncPDefMod.getInstance());

		std[Stats.MAGIC_DEFENCE.ordinal()] = new Calculator();
		std[Stats.MAGIC_DEFENCE.ordinal()].addFunc(FuncMDefMod.getInstance());

		std[Stats.CRITICAL_RATE.ordinal()] = new Calculator();
		std[Stats.CRITICAL_RATE.ordinal()].addFunc(FuncAtkCritical.getInstance());

		std[Stats.MCRITICAL_RATE.ordinal()] = new Calculator();
		std[Stats.MCRITICAL_RATE.ordinal()].addFunc(FuncMAtkCritical.getInstance());

		std[Stats.ACCURACY_COMBAT.ordinal()] = new Calculator();
		std[Stats.ACCURACY_COMBAT.ordinal()].addFunc(FuncAtkAccuracy.getInstance());

		std[Stats.EVASION_RATE.ordinal()] = new Calculator();
		std[Stats.EVASION_RATE.ordinal()].addFunc(FuncAtkEvasion.getInstance());

		std[Stats.POWER_ATTACK_SPEED.ordinal()] = new Calculator();
		std[Stats.POWER_ATTACK_SPEED.ordinal()].addFunc(FuncPAtkSpeed.getInstance());

		std[Stats.MAGIC_ATTACK_SPEED.ordinal()] = new Calculator();
		std[Stats.MAGIC_ATTACK_SPEED.ordinal()].addFunc(FuncMAtkSpeed.getInstance());

		std[Stats.MOVE_SPEED.ordinal()] = new Calculator();
		std[Stats.MOVE_SPEED.ordinal()].addFunc(FuncMoveSpeed.getInstance());

		return std;
	}

	public static Calculator[] getStdDoorCalculators()
	{
		final Calculator[] std = new Calculator[Stats.NUM_STATS];

		std[Stats.ACCURACY_COMBAT.ordinal()] = new Calculator();
		std[Stats.ACCURACY_COMBAT.ordinal()].addFunc(FuncAtkAccuracy.getInstance());

		std[Stats.EVASION_RATE.ordinal()] = new Calculator();
		std[Stats.EVASION_RATE.ordinal()].addFunc(FuncAtkEvasion.getInstance());

		std[Stats.POWER_DEFENCE.ordinal()] = new Calculator();
		std[Stats.POWER_DEFENCE.ordinal()].addFunc(FuncGatesPDefMod.getInstance());

		std[Stats.MAGIC_DEFENCE.ordinal()] = new Calculator();
		std[Stats.MAGIC_DEFENCE.ordinal()].addFunc(FuncGatesMDefMod.getInstance());

		return std;
	}

	public static void addFuncsToNewCharacter(Creature cha)
	{
		if (cha.isPlayer())
		{
			cha.addStatFunc(FuncMaxHpMul.getInstance());
			cha.addStatFunc(FuncMaxCpMul.getInstance());
			cha.addStatFunc(FuncMaxMpMul.getInstance());
			cha.addStatFunc(FuncPAtkMod.getInstance());
			cha.addStatFunc(FuncMAtkMod.getInstance());
			cha.addStatFunc(FuncPDefMod.getInstance());
			cha.addStatFunc(FuncMDefMod.getInstance());
			cha.addStatFunc(FuncAtkCritical.getInstance());
			cha.addStatFunc(FuncMAtkCritical.getInstance());
			cha.addStatFunc(FuncAtkAccuracy.getInstance());
			cha.addStatFunc(FuncAtkEvasion.getInstance());
			cha.addStatFunc(FuncPAtkSpeed.getInstance());
			cha.addStatFunc(FuncMAtkSpeed.getInstance());
			cha.addStatFunc(FuncMoveSpeed.getInstance());

			cha.addStatFunc(FuncHenna.getInstance(Stats.STAT_STR));
			cha.addStatFunc(FuncHenna.getInstance(Stats.STAT_DEX));
			cha.addStatFunc(FuncHenna.getInstance(Stats.STAT_INT));
			cha.addStatFunc(FuncHenna.getInstance(Stats.STAT_MEN));
			cha.addStatFunc(FuncHenna.getInstance(Stats.STAT_CON));
			cha.addStatFunc(FuncHenna.getInstance(Stats.STAT_WIT));

			cha.addStatFunc(FuncArmorSet.getInstance(Stats.STAT_STR));
			cha.addStatFunc(FuncArmorSet.getInstance(Stats.STAT_DEX));
			cha.addStatFunc(FuncArmorSet.getInstance(Stats.STAT_INT));
			cha.addStatFunc(FuncArmorSet.getInstance(Stats.STAT_MEN));
			cha.addStatFunc(FuncArmorSet.getInstance(Stats.STAT_CON));
			cha.addStatFunc(FuncArmorSet.getInstance(Stats.STAT_WIT));
		}
		else if (cha.isSummon())
		{
			cha.addStatFunc(FuncMaxHpMul.getInstance());
			cha.addStatFunc(FuncMaxMpMul.getInstance());
			cha.addStatFunc(FuncPAtkMod.getInstance());
			cha.addStatFunc(FuncMAtkMod.getInstance());
			cha.addStatFunc(FuncPDefMod.getInstance());
			cha.addStatFunc(FuncMDefMod.getInstance());
			cha.addStatFunc(FuncAtkCritical.getInstance());
			cha.addStatFunc(FuncMAtkCritical.getInstance());
			cha.addStatFunc(FuncAtkAccuracy.getInstance());
			cha.addStatFunc(FuncAtkEvasion.getInstance());
			cha.addStatFunc(FuncMoveSpeed.getInstance());
			cha.addStatFunc(FuncPAtkSpeed.getInstance());
			cha.addStatFunc(FuncMAtkSpeed.getInstance());
		}
	}

	public static final double calcHpRegen(Creature cha)
	{
		double init = cha.isPlayer() ? cha.getActingPlayer().getTemplate().getBaseHpRegen(cha.getLevel()) * cha.getLevelMod() * 1.06 : cha.getTemplate().getBaseHpReg();
		double hpRegenMultiplier = cha.isRaid() ? Config.RAID_HP_REGEN_MULTIPLIER : Config.HP_REGEN_MULTIPLIER;
		double hpRegenBonus = 0;

		if (cha.getChampionTemplate() != null)
		{
			hpRegenMultiplier *= cha.getChampionTemplate().hpRegenMultiplier;
		}

		if (cha.isPlayer())
		{
			final Player player = cha.getActingPlayer();

			if (SevenSignsFestival.getInstance().isFestivalInProgress() && player.isFestivalParticipant())
			{
				hpRegenMultiplier *= calcFestivalRegenModifier(player);
			}
			else
			{
				final double siegeModifier = calcSiegeRegenModifier(player);
				if (siegeModifier > 0)
				{
					hpRegenMultiplier *= siegeModifier;
				}
			}

			if (player.isInsideZone(ZoneId.CLAN_HALL) && (player.getClan() != null) && (player.getClan().getHideoutId() > 0))
			{
				final ClanHallZone zone = ZoneManager.getInstance().getZone(player, ClanHallZone.class);
				final int posChIndex = zone == null ? -1 : zone.getClanHallId();
				final int clanHallIndex = player.getClan().getHideoutId();
				if ((clanHallIndex > 0) && (clanHallIndex == posChIndex))
				{
					final ClanHall clansHall = ClanHallManager.getInstance().getClanHallById(clanHallIndex);
					if (clansHall != null)
					{
						if (clansHall.getFunction(ClanHall.FUNC_RESTORE_HP) != null)
						{
							hpRegenMultiplier *= 1 + ((double) clansHall.getFunction(ClanHall.FUNC_RESTORE_HP).getLvl() / 100);
						}
					}
				}
			}

			if (player.isInsideZone(ZoneId.CASTLE) && (player.getClan() != null) && (player.getClan().getCastleId() > 0))
			{
				final CastleZone zone = ZoneManager.getInstance().getZone(player, CastleZone.class);
				final int posCastleIndex = zone == null ? -1 : zone.getCastleId();
				final int castleIndex = player.getClan().getCastleId();
				if ((castleIndex > 0) && (castleIndex == posCastleIndex))
				{
					final Castle castle = CastleManager.getInstance().getCastleById(castleIndex);
					if (castle != null)
					{
						if (castle.getFunction(Castle.FUNC_RESTORE_HP) != null)
						{
							hpRegenMultiplier *= 1 + ((double) castle.getFunction(Castle.FUNC_RESTORE_HP).getLvl() / 100);
						}
					}
				}
			}

			if (player.isInsideZone(ZoneId.FORT) && (player.getClan() != null) && (player.getClan().getFortId() > 0))
			{
				final FortZone zone = ZoneManager.getInstance().getZone(player, FortZone.class);
				final int posFortIndex = zone == null ? -1 : zone.getFortId();
				final int fortIndex = player.getClan().getFortId();
				if ((fortIndex > 0) && (fortIndex == posFortIndex))
				{
					final Fort fort = FortManager.getInstance().getFortById(fortIndex);
					if (fort != null)
					{
						if (fort.getFunction(Fort.FUNC_RESTORE_HP) != null)
						{
							hpRegenMultiplier *= 1 + ((double) fort.getFunction(Fort.FUNC_RESTORE_HP).getLvl() / 100);
						}
					}
				}
			}

			if (player.isInsideZone(ZoneId.MOTHER_TREE))
			{
				final MotherTreeZone zone = ZoneManager.getInstance().getZone(player, MotherTreeZone.class);
				final int hpBonus = zone == null ? 0 : zone.getHpRegenBonus();
				hpRegenBonus += hpBonus;
			}
			init *= BaseStats.CON.calcBonus(cha);
		}
		else if (cha.isServitor())
		{
			init *= 2;
		}
		else if (cha.isPet())
		{
			init = ((PetInstance) cha).getPetLevelData().getPetRegenHP() * Config.PET_HP_REGEN_MULTIPLIER;
		}
		
		double regen = cha.calcStat(Stats.REGENERATE_HP_RATE, Math.max(1, init), null, null) + hpRegenBonus;
		regen *= hpRegenMultiplier;
		
		if (cha.isPlayer() && cha.getActingPlayer().isSitting())
		{
			regen *= 1.5;
		}
		else if (!cha.isMoving())
		{
			regen *= 1.1;
			regen = Rnd.chance(1) ? Math.ceil(regen) : Math.floor(regen);
		}
		else if (cha.isMoving() && cha.isRunning())
		{
			regen *= 0.7;
			regen = Math.ceil(regen);
		}
		return regen;
	}

	public static final double calcMpRegen(Creature cha)
	{
		double init = cha.isPlayer() ? cha.getActingPlayer().getTemplate().getBaseMpRegen(cha.getLevel()) * cha.getLevelMod() * 1.06 : cha.getTemplate().getBaseMpReg();
		double mpRegenMultiplier = cha.isRaid() ? Config.RAID_MP_REGEN_MULTIPLIER : Config.MP_REGEN_MULTIPLIER;
		double mpRegenBonus = 0;

		if (cha.isPlayer())
		{
			final Player player = cha.getActingPlayer();

			if (SevenSignsFestival.getInstance().isFestivalInProgress() && player.isFestivalParticipant())
			{
				mpRegenMultiplier *= calcFestivalRegenModifier(player);
			}

			if (player.isInsideZone(ZoneId.MOTHER_TREE))
			{
				final MotherTreeZone zone = ZoneManager.getInstance().getZone(player, MotherTreeZone.class);
				final int mpBonus = zone == null ? 0 : zone.getMpRegenBonus();
				mpRegenBonus += mpBonus;
			}

			if (player.isInsideZone(ZoneId.CLAN_HALL) && (player.getClan() != null) && (player.getClan().getHideoutId() > 0))
			{
				final ClanHallZone zone = ZoneManager.getInstance().getZone(player, ClanHallZone.class);
				final int posChIndex = zone == null ? -1 : zone.getClanHallId();
				final int clanHallIndex = player.getClan().getHideoutId();
				if ((clanHallIndex > 0) && (clanHallIndex == posChIndex))
				{
					final ClanHall clansHall = ClanHallManager.getInstance().getClanHallById(clanHallIndex);
					if (clansHall != null)
					{
						if (clansHall.getFunction(ClanHall.FUNC_RESTORE_MP) != null)
						{
							mpRegenMultiplier *= 1 + ((double) clansHall.getFunction(ClanHall.FUNC_RESTORE_MP).getLvl() / 100);
						}
					}
				}
			}

			if (player.isInsideZone(ZoneId.CASTLE) && (player.getClan() != null) && (player.getClan().getCastleId() > 0))
			{
				final CastleZone zone = ZoneManager.getInstance().getZone(player, CastleZone.class);
				final int posCastleIndex = zone == null ? -1 : zone.getCastleId();
				final int castleIndex = player.getClan().getCastleId();
				if ((castleIndex > 0) && (castleIndex == posCastleIndex))
				{
					final Castle castle = CastleManager.getInstance().getCastleById(castleIndex);
					if (castle != null)
					{
						if (castle.getFunction(Castle.FUNC_RESTORE_MP) != null)
						{
							mpRegenMultiplier *= 1 + ((double) castle.getFunction(Castle.FUNC_RESTORE_MP).getLvl() / 100);
						}
					}
				}
			}

			if (player.isInsideZone(ZoneId.FORT) && (player.getClan() != null) && (player.getClan().getFortId() > 0))
			{
				final FortZone zone = ZoneManager.getInstance().getZone(player, FortZone.class);
				final int posFortIndex = zone == null ? -1 : zone.getFortId();
				final int fortIndex = player.getClan().getFortId();
				if ((fortIndex > 0) && (fortIndex == posFortIndex))
				{
					final Fort fort = FortManager.getInstance().getFortById(fortIndex);
					if (fort != null)
					{
						if (fort.getFunction(Fort.FUNC_RESTORE_MP) != null)
						{
							mpRegenMultiplier *= 1 + ((double) fort.getFunction(Fort.FUNC_RESTORE_MP).getLvl() / 100);
						}
					}
				}
			}
			init *= BaseStats.MEN.calcBonus(cha);
		}
		else if (cha.isServitor())
		{
			init *= 2;
		}
		else if (cha.isPet())
		{
			init = ((PetInstance) cha).getPetLevelData().getPetRegenMP() * Config.PET_MP_REGEN_MULTIPLIER;
		}
		
		double regen = cha.calcStat(Stats.REGENERATE_MP_RATE, Math.max(1, init), null, null) + mpRegenBonus;
		regen *= mpRegenMultiplier;
		if (cha.isPlayer() && cha.getActingPlayer().isSitting())
		{
			regen *= 1.5;
		}
		else if (!cha.isMoving())
		{
			regen *= 1.1;
			regen = Rnd.chance(1) ? Math.ceil(regen) : Math.floor(regen);
		}
		else if (cha.isMoving() && cha.isRunning())
		{
			regen *= 0.7;
			regen = Math.ceil(regen);
		}
		return regen;
	}

	public static final double calcCpRegen(Creature cha)
	{
		double init = cha.isPlayer() ? cha.getActingPlayer().getTemplate().getBaseCpRegen(cha.getLevel()) * cha.getLevelMod() * 1.06 : cha.getTemplate().getBaseHpReg();
		final double cpRegenMultiplier = Config.CP_REGEN_MULTIPLIER;
		final double cpRegenBonus = 0;

		if (cha.isPlayer())
		{
			init *= BaseStats.CON.calcBonus(cha);
		}
		
		double regen = cha.calcStat(Stats.REGENERATE_CP_RATE, Math.max(1, init), null, null) + cpRegenBonus;
		regen *= cpRegenMultiplier;
		if (cha.isPlayer() && cha.getActingPlayer().isSitting())
		{
			regen *= 1.5;
		}
		else if (!cha.isMoving())
		{
			regen *= 1.1;
			regen = Rnd.chance(1) ? Math.ceil(regen) : Math.floor(regen);
		}
		else if (cha.isMoving() && cha.isRunning())
		{
			regen *= 0.7;
			regen = Math.ceil(regen);
		}
		return regen;
	}

	public static final double calcFestivalRegenModifier(Player activeChar)
	{
		final int[] festivalInfo = SevenSignsFestival.getInstance().getFestivalForPlayer(activeChar);
		final int oracle = festivalInfo[0];
		final int festivalId = festivalInfo[1];
		int[] festivalCenter;

		if (festivalId < 0)
		{
			return 0;
		}

		if (oracle == SevenSigns.CABAL_DAWN)
		{
			festivalCenter = SevenSignsFestival.FESTIVAL_DAWN_PLAYER_SPAWNS[festivalId];
		}
		else
		{
			festivalCenter = SevenSignsFestival.FESTIVAL_DUSK_PLAYER_SPAWNS[festivalId];
		}

		final double distToCenter = activeChar.getDistance(festivalCenter[0], festivalCenter[1]);

		if (Config.DEBUG)
		{
			_log.info("Distance: " + distToCenter + ", RegenMulti: " + ((distToCenter * 2.5) / 50));
		}
		return 1.0 - (distToCenter * 0.0005);
	}

	public static final double calcSiegeRegenModifier(Player activeChar)
	{
		if ((activeChar == null) || (activeChar.getClan() == null))
		{
			return 0;
		}

		final Siege siege = SiegeManager.getInstance().getSiege(activeChar.getX(), activeChar.getY(), activeChar.getZ());
		if ((siege == null) || !siege.getIsInProgress())
		{
			return 0;
		}

		final SiegeClan siegeClan = siege.getAttackerClan(activeChar.getClan().getId());
		if ((siegeClan == null) || siegeClan.getFlag().isEmpty() || !Util.checkIfInRange(200, activeChar, siegeClan.getFlag().get(0), true))
		{
			return 0;
		}
		return 1.5;
	}

	public static double calcBlowDamage(Creature attacker, Creature target, Skill skill, byte shld, boolean ss)
	{
		double defence = target.getPDef(attacker);

		switch (shld)
		{
			case Formulas.SHIELD_DEFENSE_SUCCEED :
				defence += target.getShldDef();
				break;
			case Formulas.SHIELD_DEFENSE_PERFECT_BLOCK :
				return 1;
		}

		final boolean isPvP = attacker.isPlayable() && target.isPlayer();
		final boolean isPvE = attacker.isPlayable() && target.isAttackable();
		final double power = skill.getPower(isPvP, isPvE);
		double damage = 0;
		double proximityBonus = 1;
		final double graciaPhysSkillBonus = skill.isMagic() ? 1 : 1.10113;
		final double ssboost = ss ? skill.isBehind() ? 1.5 : 2.04 : 1;
		double pvpBonus = 1;

		if (attacker.isPlayable() && target.isPlayable())
		{
			pvpBonus *= attacker.getPvpPhysSkillDmg();
			defence *= target.getPvpPhysSkillDef();
		}

		if (skill.isBehind())
		{
			proximityBonus = attacker.getDirection() == TargetDirection.BEHIND ? 1.5 : 1;
		}
		else
		{
			proximityBonus = attacker.getDirection() == TargetDirection.BEHIND ? 1.2 : attacker.getDirection() == TargetDirection.FRONT ? 1.1 : 1;
		}

		damage *= calcValakasTrait(attacker, target, skill);

		final double element = calcElemental(attacker, target, skill);
		final double critDamage = attacker.getCriticalDmg(target, 1, skill);
		if (skill.isBehind())
		{
			damage += (((70. * graciaPhysSkillBonus * (attacker.getPAtk(target) + power)) / defence) * critDamage * (target.calcStat(Stats.CRIT_VULN, 1, target, skill)) * ssboost * proximityBonus * element * pvpBonus) + ((((attacker.calcStat(Stats.CRITICAL_DAMAGE_ADD, 0, target, skill) - target.calcStat(Stats.CRIT_ADD_VULN, 0, target, skill)) * 6.1 * 70) / defence) * graciaPhysSkillBonus);
		}
		else
		{
			damage += (((70. * graciaPhysSkillBonus * (power + (attacker.getPAtk(target) * ssboost))) / defence) * critDamage * (target.calcStat(Stats.CRIT_VULN, 1, target, skill)) * proximityBonus * element * pvpBonus) + ((((attacker.calcStat(Stats.CRITICAL_DAMAGE_ADD, 0, target, skill) - target.calcStat(Stats.CRIT_ADD_VULN, 0, target, skill)) * 6.1 * 70) / defence) * graciaPhysSkillBonus);
		}
		damage = target.calcStat(Stats.DAGGER_WPN_VULN, damage, target, null);
		damage *= Config.ALLOW_RND_DAMAGE_BY_SKILLS ? (1 + (Rnd.get() * attacker.getRandomDamage() * 2 - attacker.getRandomDamage()) / 100) : 1.;
		if (target.isRaid())
		{
			damage *= attacker.getStat().calcStat(Stats.RAID_DAMAGE, 1.);
		}
		damage *= ClassBalanceParser.getInstance().getBalancedClass(AttackType.Blow, attacker, target);
		final int keyId = (target.isPlayer()) ? target.getActingPlayer().getClassId().getId() : (target.isPlayer()) ? -1 : -2;
		damage *= SkillBalanceParser.getInstance().getSkillValue(skill.getId() + ";" + keyId, SkillChangeType.SkillBlow, target);
		
		return damage < 1 ? 1. : damage;
	}
	
	public static void calcStunBreak(Creature target, boolean crit)
	{
		if (target != null && target.isStunned() && Rnd.chance(crit ? Config.STUN_CHANCE_CRIT_MOD : Config.STUN_CHANCE_MOD))
		{
			target.stopEffects(EffectType.STUN);
			target.setIsStuned(false);
		}
	}

	public static final double calcPhysDam(Creature attacker, Creature target, Skill skill, byte shld, boolean crit, boolean ss)
	{
		final boolean isPvP = attacker.isPlayable() && target.isPlayable();
		final boolean isPvE = attacker.isPlayable() && target.isAttackable();
		final boolean isPvEAtt = attacker.isAttackable() && target.isPlayable();
		final double proximityBonus = skill == null ? attacker.isBehindTarget() ? 1.2 : attacker.isInFrontOfTarget() ? 1 : 1.1 : 1;
		double damage = attacker.getPAtk(target);
		double defence = target.getPDef(attacker);
		damage *= calcValakasTrait(attacker, target, skill);
		final int ssBoost = ss ? 2 : 1;
		
		if (isPvP)
		{
			defence *= skill != null ? target.getPvpPhysSkillDef() : target.getPvpPhysDef();
		}

		switch (shld)
		{
			case SHIELD_DEFENSE_SUCCEED :
				if (!Config.ALT_GAME_SHIELD_BLOCKS)
				{
					defence += target.getShldDef();
				}
				break;
			case SHIELD_DEFENSE_PERFECT_BLOCK :
				return 1.;
		}
		
		damage = (skill != null) ? (damage + skill.getPower(attacker, target, isPvP, isPvE)) * ssBoost : damage * ssBoost;
		
		final Weapon weapon = attacker.getActiveWeaponItem();
		Stats stat = null;
		boolean isBow = false;
		if ((weapon != null) && !attacker.isTransformed())
		{
			switch (weapon.getItemType())
			{
				case BOW :
					isBow = true;
					stat = Stats.BOW_WPN_VULN;
					break;
				case CROSSBOW :
					isBow = true;
					stat = Stats.CROSSBOW_WPN_VULN;
					break;
				case BLUNT :
					stat = Stats.BLUNT_WPN_VULN;
					break;
				case DAGGER :
					stat = Stats.DAGGER_WPN_VULN;
					break;
				case DUAL :
					stat = Stats.DUAL_WPN_VULN;
					break;
				case DUALFIST :
					stat = Stats.DUALFIST_WPN_VULN;
					break;
				case ETC :
					stat = Stats.ETC_WPN_VULN;
					break;
				case FIST :
					stat = Stats.FIST_WPN_VULN;
					break;
				case POLE :
					stat = Stats.POLE_WPN_VULN;
					break;
				case SWORD :
					stat = Stats.SWORD_WPN_VULN;
					break;
				case BIGSWORD :
					stat = Stats.BIGSWORD_WPN_VULN;
					break;
				case BIGBLUNT :
					stat = Stats.BIGBLUNT_WPN_VULN;
					break;
				case DUALDAGGER :
					stat = Stats.DUALDAGGER_WPN_VULN;
					break;
				case RAPIER :
					stat = Stats.RAPIER_WPN_VULN;
					break;
				case ANCIENTSWORD :
					stat = Stats.ANCIENT_WPN_VULN;
					break;
			}
		}

		if (attacker.isServitor())
		{
			stat = Stats.PET_WPN_VULN;
		}

		if (crit)
		{
			damage = 2 * attacker.getCriticalDmg(target, 1, skill) * target.calcStat(Stats.CRIT_VULN, target.getTemplate().getBaseCritVuln(), target, null) * ((76 * damage * proximityBonus) / defence);
		}
		else
		{
			damage = (76 * damage * proximityBonus) / defence;
			damage *= skill != null ? ClassBalanceParser.getInstance().getBalancedClass(AttackType.PSkillDamage, attacker, target) : ClassBalanceParser.getInstance().getBalancedClass(AttackType.Normal, attacker, target);
		}
		
		if (skill != null && damage > 1 && skill.isDeathlink())
		{
			damage *= 1.8 * (1.0 - attacker.getCurrentHpRatio());
		}

		if (stat != null)
		{
			damage = target.calcStat(stat, damage, target, null);
		}

		damage *= (skill == null || (skill != null && skill.getSkillType() != SkillType.CHARGEDAM && Config.ALLOW_RND_DAMAGE_BY_SKILLS)) ? (1 + (Rnd.get() * attacker.getRandomDamage() * 2 - attacker.getRandomDamage()) / 100) : 1.;
		if ((shld > 0) && Config.ALT_GAME_SHIELD_BLOCKS)
		{
			damage -= target.getShldDef();
			if (damage < 0)
			{
				damage = 0;
			}
		}
		
		if (crit)
		{
			damage += (((attacker.calcStat(Stats.CRITICAL_DAMAGE_ADD, 0, target, skill) - target.calcStat(Stats.CRIT_ADD_VULN, 0, target, skill)) * 100) / defence) * 2;
			if (skill != null)
			{
				damage *= ClassBalanceParser.getInstance().getBalancedClass(AttackType.PSkillCritical, attacker, target);
				final int keyId = (target.isPlayer()) ? target.getActingPlayer().getClassId().getId() : (target.isMonster()) ? -1 : -2;
				damage *= SkillBalanceParser.getInstance().getSkillValue(skill.getId() + ";" + keyId, SkillChangeType.PCrit, target);
			}
			else
			{
				damage *= ClassBalanceParser.getInstance().getBalancedClass(AttackType.Crit, attacker, target);
			}
		}

		if (target.isNpc())
		{
			switch (((Npc) target).getTemplate().getRace())
			{
				case BEAST :
					damage *= attacker.getPAtkMonsters(target);
					break;
				case ANIMAL :
					damage *= attacker.getPAtkAnimals(target);
					break;
				case PLANT :
					damage *= attacker.getPAtkPlants(target);
					break;
				case DRAGON :
					damage *= attacker.getPAtkDragons(target);
					break;
				case BUG :
					damage *= attacker.getPAtkInsects(target);
					break;
				case GIANT :
					damage *= attacker.getPAtkGiants(target);
					break;
				case MAGICCREATURE :
					damage *= attacker.getPAtkMagicCreatures(target);
					break;
				default :
					break;
			}
		}

		if ((damage > 0) && (damage < 1))
		{
			damage = 1;
		}
		else if (damage < 0)
		{
			damage = 0;
		}

		if (isPvP)
		{
			damage *= skill != null ? attacker.getPvpPhysSkillDmg() : attacker.getPvpPhysDmg();
			if (attacker.isPet())
			{
				damage *= attacker.getStat().getPvpPhysDamage();
			}
			
			if (target.isPet())
			{
				damage /= target.getStat().getPvpPhysDefence();
			}
		}

		if (skill != null)
		{
			damage = attacker.calcStat(Stats.PHYSICAL_SKILL_POWER, damage, null, null);
		}

		damage *= calcElemental(attacker, target, skill);
		if (isPvE)
		{
			damage *= isBow ? skill != null ? attacker.calcStat(Stats.PVE_BOW_SKILL_DMG, 1, null, null) : attacker.calcStat(Stats.PVE_BOW_DMG, 1, null, null) : skill != null ? attacker.calcStat(Stats.PVE_PHYS_SKILL_DMG, 1, null, null) : attacker.calcStat(Stats.PVE_PHYSICAL_DMG, 1, null, null);
			if (attacker.isPet())
			{
				damage *= attacker.getStat().getPvePhysDamage();
			}
		}
		
		if (isPvEAtt)
		{
			damage /= isBow ? skill != null ? target.calcStat(Stats.PVE_BOW_SKILL_DEF, 1, null, null) : target.calcStat(Stats.PVE_BOW_DEF, 1, null, null) : skill != null ? target.calcStat(Stats.PVE_PHYS_SKILL_DEF, 1, null, null) : target.calcStat(Stats.PVE_PHYSICAL_DEF, 1, null, null);
			if (target.isPet())
			{
				damage /= target.getStat().getPvePhysDefence();
			}
		}

		if (target.isPlayer() && (weapon != null) && (weapon.getItemType() == WeaponType.DAGGER) && (skill != null))
		{
			final Armor armor = ((Player) target).getActiveChestArmorItem();
			if (armor != null)
			{
				if (((Player) target).isWearingHeavyArmor())
				{
					damage /= Config.ALT_DAGGER_DMG_VS_HEAVY;
				}

				if (((Player) target).isWearingLightArmor())
				{
					damage /= Config.ALT_DAGGER_DMG_VS_LIGHT;
				}

				if (((Player) target).isWearingMagicArmor())
				{
					damage /= Config.ALT_DAGGER_DMG_VS_ROBE;
				}
			}
		}

		if (target.isPlayer() && (weapon != null) && (weapon.getItemType() == WeaponType.BOW) && (skill != null))
		{
			final Armor armor = ((Player) target).getActiveChestArmorItem();
			if (armor != null)
			{
				if (((Player) target).isWearingHeavyArmor())
				{
					damage /= Config.ALT_BOW_DMG_VS_HEAVY;
				}

				if (((Player) target).isWearingLightArmor())
				{
					damage /= Config.ALT_BOW_DMG_VS_LIGHT;
				}

				if (((Player) target).isWearingMagicArmor())
				{
					damage /= Config.ALT_BOW_DMG_VS_ROBE;
				}
			}
		}

		if (attacker.isPlayer())
		{
			damage *= attacker.getActingPlayer().getClassId().isMage() ? Config.ALT_MAGES_PHYSICAL_DAMAGE_MULTI : Config.ALT_FIGHTERS_PHYSICAL_DAMAGE_MULTI;
		}
		else if (attacker.isSummon())
		{
			damage *= Config.ALT_PETS_PHYSICAL_DAMAGE_MULTI;
		}
		else if (attacker.isNpc())
		{
			damage *= Config.ALT_NPC_PHYSICAL_DAMAGE_MULTI;
		}
		
		if (target.isRaid())
		{
			damage *= attacker.getStat().calcStat(Stats.RAID_DAMAGE, 1.);
		}
		return damage;
	}
	
	public static double calcWeaponTraitBonus(Creature attacker, Creature target)
	{
		final var weapon = attacker.getActiveWeaponItem();
		Stats stat = null;
		if ((weapon != null) && !attacker.isTransformed())
		{
			switch (weapon.getItemType())
			{
				case BOW :
					stat = Stats.BOW_WPN_VULN;
					break;
				case CROSSBOW :
					stat = Stats.CROSSBOW_WPN_VULN;
					break;
				case BLUNT :
					stat = Stats.BLUNT_WPN_VULN;
					break;
				case DAGGER :
					stat = Stats.DAGGER_WPN_VULN;
					break;
				case DUAL :
					stat = Stats.DUAL_WPN_VULN;
					break;
				case DUALFIST :
					stat = Stats.DUALFIST_WPN_VULN;
					break;
				case ETC :
					stat = Stats.ETC_WPN_VULN;
					break;
				case FIST :
					stat = Stats.FIST_WPN_VULN;
					break;
				case POLE :
					stat = Stats.POLE_WPN_VULN;
					break;
				case SWORD :
					stat = Stats.SWORD_WPN_VULN;
					break;
				case DUALDAGGER :
					stat = Stats.DUALDAGGER_WPN_VULN;
					break;
				case RAPIER :
					stat = Stats.RAPIER_WPN_VULN;
					break;
				case ANCIENTSWORD :
					stat = Stats.ANCIENT_WPN_VULN;
					break;
			}
		}
		
		if (attacker.isServitor())
		{
			stat = Stats.PET_WPN_VULN;
		}
		
		if (stat != null)
		{
			return target.calcStat(stat, 1, target, null);
		}
		return 1.0;
	}

	public static final double calcMagicDam(Creature attacker, Creature target, Skill skill, byte shld, boolean sps, boolean bss, boolean mcrit)
	{
		double mAtk = attacker.getMAtk(target, skill);
		double mDef = target.getMDef(attacker, skill);
		final boolean isPvP = attacker.isPlayable() && target.isPlayable();
		final boolean isPvE = attacker.isPlayable() && target.isAttackable();
		final boolean isPvEAtt = attacker.isAttackable() && target.isPlayable();
		if (skill.isIgnoreCritDamage())
		{
			mcrit = false;
		}

		if (isPvP)
		{
			mDef *= skill.isMagic() ? target.getPvpMagicDef() : target.getPvpMagicDef();
		}

		switch (shld)
		{
			case SHIELD_DEFENSE_SUCCEED :
				mDef += target.getShldDef();
				break;
			case SHIELD_DEFENSE_PERFECT_BLOCK :
				return 1;
		}

		mAtk *= bss ? 4 : sps ? 2 : 1;

		double damage = ((91 * Math.sqrt(mAtk)) / mDef) * skill.getPower(attacker, target, isPvP, isPvE);

		if (Config.ALT_GAME_MAGICFAILURES && !calcMagicSuccess(attacker, target, skill, true))
		{
			if (attacker.isPlayer())
			{
				if ((target.getLevel() - attacker.getLevel()) <= 9)
				{
					if (skill.getSkillType() == SkillType.DRAIN)
					{
						attacker.sendPacket(SystemMessageId.DRAIN_HALF_SUCCESFUL);
					}
					else
					{
						attacker.sendPacket(SystemMessageId.ATTACK_FAILED);
					}
					damage /= 2;
				}
				else
				{
					final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_RESISTED_YOUR_S2);
					sm.addCharName(target);
					sm.addSkillName(skill);
					attacker.sendPacket(sm);
					damage = 1;
				}
			}

			if (target.isPlayer())
			{
				final SystemMessage sm = (skill.getSkillType() == SkillType.DRAIN) ? SystemMessage.getSystemMessage(SystemMessageId.RESISTED_C1_DRAIN) : SystemMessage.getSystemMessage(SystemMessageId.RESISTED_C1_MAGIC);
				sm.addCharName(attacker);
				target.sendPacket(sm);
			}
		}
		else if (mcrit)
		{
			damage *= attacker.isPlayer() && target.isPlayer() ? 2.5 : 3;
			damage *= attacker.calcStat(Stats.MAGIC_CRIT_DMG, 1, null, null);
		}
		
		if (mcrit)
		{
			damage *= ClassBalanceParser.getInstance().getBalancedClass(AttackType.MCrit, attacker, target);
			final int keyId = (target.isPlayer()) ? target.getActingPlayer().getClassId().getId() : (target.isMonster()) ? -1 : -2;
			damage *= SkillBalanceParser.getInstance().getSkillValue(skill.getId() + ";" + keyId, SkillChangeType.MCrit, target);
		}
		else
		{
			damage *= ClassBalanceParser.getInstance().getBalancedClass(AttackType.Magic, attacker, target);
		}

		if (damage > 1 && skill.isDeathlink())
		{
			damage *= 1.8 * (1.0 - attacker.getCurrentHpRatio());
		}
		
		damage *= Config.ALLOW_RND_DAMAGE_BY_SKILLS && skill.getSkillType() != SkillType.CHARGEDAM ? (1 + (Rnd.get() * attacker.getRandomDamage() * 2 - attacker.getRandomDamage()) / 100) : 1.;
		
		if (isPvP)
		{
			damage *= skill.isMagic() ? attacker.getPvpMagicDmg() : attacker.getPvpPhysSkillDmg();
			if (attacker.isPet())
			{
				damage *= attacker.getStat().getPvpMagicDamage();
			}
			
			if (target.isPet())
			{
				damage /= target.getStat().getPvpMagicDefence();
			}
		}
		damage *= target.calcStat(Stats.MAGIC_DAMAGE_VULN, 1, null, null);
		damage = attacker.calcStat(Stats.MAGIC_SKILL_POWER, damage, null, null);
		damage *= calcElemental(attacker, target, skill);

		if (isPvE)
		{
			damage *= attacker.calcStat(Stats.PVE_MAGICAL_DMG, 1, null, null);
			if (attacker.isPet())
			{
				damage *= attacker.getStat().getPveMagicDamage();
			}
		}
		
		if (isPvEAtt)
		{
			damage /= target.calcStat(Stats.PVE_MAGICAL_DEF, 1, null, null);
			if (target.isPet())
			{
				damage /= target.getStat().getPveMagicDefence();
			}
		}

		if (attacker.isPlayer())
		{
			damage *= attacker.getActingPlayer().getClassId().isMage() ? Config.ALT_MAGES_MAGICAL_DAMAGE_MULTI : Config.ALT_FIGHTERS_MAGICAL_DAMAGE_MULTI;
		}
		else if (attacker.isSummon())
		{
			damage *= Config.ALT_PETS_MAGICAL_DAMAGE_MULTI;
		}
		else if (attacker.isNpc())
		{
			damage *= Config.ALT_NPC_MAGICAL_DAMAGE_MULTI;
		}
		
		if (target.isRaid())
		{
			damage *= attacker.getStat().calcStat(Stats.RAID_DAMAGE, 1.);
		}
		return damage;
	}

	public static final double calcMagicDam(CubicInstance attacker, Creature target, Skill skill, boolean mcrit, byte shld)
	{
		final int mAtk = attacker.getCubicPower();
		double mDef = target.getMDef(attacker.getOwner(), skill);
		final boolean isPvP = target.isPlayable();
		final boolean isPvE = target.isAttackable();

		switch (shld)
		{
			case SHIELD_DEFENSE_SUCCEED :
				mDef += target.getShldDef();
				break;
			case SHIELD_DEFENSE_PERFECT_BLOCK :
				return 1;
		}

		double damage = 91 * ((mAtk + skill.getPower(isPvP, isPvE)) / mDef);
		final Player owner = attacker.getOwner();

		if (Config.ALT_GAME_MAGICFAILURES && !calcMagicSuccess(owner, target, skill, true))
		{
			if ((target.getLevel() - skill.getMagicLevel()) <= 9)
			{
				if (skill.getSkillType() == SkillType.DRAIN)
				{
					owner.sendPacket(SystemMessageId.DRAIN_HALF_SUCCESFUL);
				}
				else
				{
					owner.sendPacket(SystemMessageId.ATTACK_FAILED);
				}
				damage /= 2;
			}
			else
			{
				final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_RESISTED_YOUR_S2);
				sm.addCharName(target);
				sm.addSkillName(skill);
				owner.sendPacket(sm);
				damage = 1;
			}

			if (target.isPlayer())
			{
				if (skill.getSkillType() == SkillType.DRAIN)
				{
					final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.RESISTED_C1_DRAIN);
					sm.addCharName(owner);
					target.sendPacket(sm);
				}
				else
				{
					final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.RESISTED_C1_MAGIC);
					sm.addCharName(owner);
					target.sendPacket(sm);
				}
			}
		}
		else if (mcrit)
		{
			damage *= 3;
		}

		damage *= target.calcStat(Stats.MAGIC_DAMAGE_VULN, 1, null, null);
		damage *= calcElemental(owner, target, skill);

		if (isPvE)
		{
			damage *= attacker.getOwner().calcStat(Stats.PVE_MAGICAL_DMG, 1, null, null);
		}
		return damage;
	}
	
	public static double calcCrit(Creature attacker, Creature target, Skill skill, boolean blow)
	{
		if (attacker.isPlayer() && (attacker.getActiveWeaponItem() == null))
		{
			return 0;
		}
		if (skill != null)
		{
			return skill.getBaseCritRate() * (blow ? BaseStats.DEX.calcBonus(attacker) : BaseStats.STR.calcBonus(attacker)) * target.calcStat(Stats.CRIT_VULN, 1, attacker, skill);
		}
		
		double rate = attacker.getCriticalHit(target, null) * 0.01 * target.calcStat(Stats.CRIT_DAMAGE_EVASION, 100, attacker, skill);
		
		final var direct = attacker.getDirection() != TargetDirection.NONE ? attacker.getDirection() : PositionUtils.getDirectionTo(target, attacker);
		switch (direct)
		{
			case BEHIND :
				rate *= 1.4;
				break;
			case SIDE :
				rate *= 1.2;
				break;
		}
		return rate / 10;
	}

	public static final boolean calcLethalHit(Creature activeChar, Creature target, Skill skill)
	{
		double lethal2chance = 0;
		double lethal1chance = 0;

		if (activeChar.isPlayer() && !activeChar.getAccessLevel().canGiveDamage())
		{
			return false;
		}
		
		if (skill.isBehind() && activeChar.getDirection() != TargetDirection.BEHIND)
		{
			return false;
		}

		if (target.isRaid() || target.isRaidMinion() || target.isLethalImmune() || target.isDoor())
		{
			return false;
		}
		
		if (target.getChampionTemplate() != null && target.getChampionTemplate().lethalImmune)
		{
			return false;
		}
		
		if (target.getChampionTemplate() != null && target.getChampionTemplate().lethalImmune)
		{
			return false;
		}
		
		if (!target.isInvul() && target.getLevel() - activeChar.getLevel() <= 5)
		{
			final double lethalStrikeRate = skill.getLethalStrikeRate() * calcLvlBonusMod(activeChar, target, skill);
			final double halfKillRate = skill.getHalfKillRate() * calcLvlBonusMod(activeChar, target, skill);

			if (Rnd.get(100) < activeChar.calcStat(Stats.LETHAL_RATE, lethalStrikeRate, target, null))
			{
				lethal2chance = activeChar.calcStat(Stats.LETHAL_RATE, lethalStrikeRate, target, null);
				if ((activeChar.isPlayer()) && Config.SKILL_CHANCE_SHOW && activeChar.getActingPlayer().isSkillChanceShow())
				{
					final Player attacker = activeChar.getActingPlayer();
					attacker.sendMessage((new ServerMessage("Formulas.Lethal_Shot", attacker.getLang())).toString() + ": " + String.format("%1.2f", (lethal2chance / 10)) + "%");
				}

				if (target.isPlayer())
				{
					target.setCurrentCp(1);
					target.setCurrentHp(1);
					target.sendPacket(SystemMessageId.LETHAL_STRIKE);
				}
				else if (target.isMonster())
				{
					final double damage = skill.getId() == 1400 && (skill.getLevel() > 100 && skill.getLevel() <= 130) ? target.getCurrentHp() : target.getCurrentHp() - 1.0;
					target.reduceCurrentHp(damage, activeChar, skill);
				}
				else if (target.isSummon())
				{
					target.setCurrentHp(1);
				}
				activeChar.sendPacket(SystemMessageId.LETHAL_STRIKE_SUCCESSFUL);
			}
			else if (Rnd.get(100) < activeChar.calcStat(Stats.LETHAL_RATE, halfKillRate, target, null))
			{
				lethal1chance = activeChar.calcStat(Stats.LETHAL_RATE, halfKillRate, target, null);
				if ((activeChar.isPlayer()) && Config.SKILL_CHANCE_SHOW && activeChar.getActingPlayer().isSkillChanceShow())
				{
					final Player attacker = activeChar.getActingPlayer();
					attacker.sendMessage((new ServerMessage("Formulas.Lethal_Shot", attacker.getLang())).toString() + ": " + String.format("%1.2f", (lethal1chance / 10)) + "%");
				}

				if (target.isPlayer())
				{
					target.setCurrentCp(1);
					target.sendPacket(SystemMessageId.HALF_KILL);
					target.sendPacket(SystemMessageId.CP_DISAPPEARS_WHEN_HIT_WITH_A_HALF_KILL_SKILL);
				}
				else if (target.isMonster())
				{
					final double damage = target.getCurrentHp() * 0.5;
					target.reduceCurrentHp(damage, activeChar, skill);
				}
				else if (target.isSummon())
				{
					target.setCurrentHp(target.getCurrentHp() * 0.5);
				}
				activeChar.sendPacket(SystemMessageId.HALF_KILL);
			}
		}
		else
		{
			return false;
		}
		return true;
	}

	public static final boolean calcMCrit(double mRate)
	{
		return mRate > Rnd.get(1000);
	}

	public static final boolean calcAtkBreak(Creature target, boolean crit)
	{
		if (target == null || target.isInvul() || target.isRaid() || !target.isCastingNow() || !Config.ALT_GAME_CANCEL_CAST)
		{
			return false;
		}

		final Skill skill = target.getCastingSkill();
		if (skill == null)
		{
			return false;
		}
		
		if (skill.isPhysical() || skill.isDance())
		{
			return false;
		}
		return Rnd.chance(target.calcStat(Stats.ATTACK_CANCEL, crit ? Config.SKILL_BREAK_CRIT_MOD : Config.SKILL_BREAK_MOD, null, skill));
	}

	public static final int calcAtkSpd(Creature attacker, Skill skill, double skillTime)
	{
		return skill.isMagic() ? (int) (skillTime * 333.0D / Math.max(attacker.getMAtkSpd(), 1)) : (int) (skillTime * 300.0D / Math.max(attacker.getPAtkSpd(), 1));
	}
	
	public static boolean calcHitMiss(Creature attacker, Creature target)
	{
		int chance = (80 + (2 * (attacker.getAccuracy() - target.getEvasionRate(attacker)))) * 10;

		chance *= HitConditionBonusParser.getInstance().getConditionBonus(attacker, target);

		chance = Math.max(chance, 200);
		chance = Math.min(chance, 980);

		return chance < Rnd.get(1000);
	}

	public static byte calcShldUse(Creature attacker, Creature target, Skill skill, boolean sendSysMsg)
	{
		if ((skill != null) && skill.ignoreShield())
		{
			return 0;
		}

		final Item item = target.getSecondaryWeaponItem();
		if ((item == null) || !(item instanceof Armor) || (((Armor) item).getItemType() == ArmorType.SIGIL))
		{
			return 0;
		}

		double shldRate = target.calcStat(Stats.SHIELD_RATE, 0, attacker, null) * BaseStats.DEX.calcBonus(target);
		if (shldRate <= 1e-6)
		{
			return 0;
		}

		final int degreeside = (int) target.calcStat(Stats.SHIELD_DEFENCE_ANGLE, 0, null, null) + 120;
		if ((degreeside < 360) && (!target.isFacing(attacker, degreeside)))
		{
			return 0;
		}

		byte shldSuccess = SHIELD_DEFENSE_FAILED;

		final Weapon at_weapon = attacker.getActiveWeaponItem();
		if ((at_weapon != null) && (at_weapon.getItemType() == WeaponType.BOW))
		{
			shldRate *= 1.3;
		}
		
		shldRate *= Config.ALT_SHLD_BLOCK_MODIFIER;

		if ((shldRate > 0) && Rnd.chance(Config.ALT_PERFECT_SHLD_BLOCK))
		{
			shldSuccess = SHIELD_DEFENSE_PERFECT_BLOCK;
		}
		else if (Rnd.chance(shldRate))
		{
			shldSuccess = SHIELD_DEFENSE_SUCCEED;
		}

		if (sendSysMsg && target.isPlayer())
		{
			final Player enemy = target.getActingPlayer();

			switch (shldSuccess)
			{
				case SHIELD_DEFENSE_SUCCEED :
					enemy.sendPacket(SystemMessageId.SHIELD_DEFENCE_SUCCESSFULL);
					break;
				case SHIELD_DEFENSE_PERFECT_BLOCK :
					enemy.sendPacket(SystemMessageId.YOUR_EXCELLENT_SHIELD_DEFENSE_WAS_A_SUCCESS);
					break;
			}
		}
		return shldSuccess;
	}

	public static byte calcShldUse(Creature attacker, Creature target, Skill skill)
	{
		return calcShldUse(attacker, target, skill, true);
	}

	public static byte calcShldUse(Creature attacker, Creature target)
	{
		return calcShldUse(attacker, target, null, true);
	}

	public static double calcSkillVulnerability(Creature attacker, Creature target, Skill skill)
	{
		double multiplier = 0;
		if (skill != null)
		{
			multiplier = calcSkillTraitVulnerability(multiplier, target, skill, target.getChampionTemplate());
		}
		return multiplier;
	}

	public static double calcSkillTraitVulnerability(double multiplier, Creature target, Skill skill, ChampionTemplate champion)
	{
		if (skill == null)
		{
			return multiplier;
		}

		final TraitType trait = skill.getTraitType();

		if ((trait != null) && (trait != TraitType.NONE))
		{
			switch (trait)
			{
				case BLEED :
					if (champion != null)
					{
						multiplier = champion.getResistValue(Stats.BLEED_VULN);
					}
					multiplier = calcMin(target.calcStat(Stats.BLEED_VULN, multiplier, target, null), Config.BLEED_VULN);
					break;
				case BOSS :
					if (champion != null)
					{
						multiplier = champion.getResistValue(Stats.BOSS_VULN);
					}
					multiplier = calcMin(target.calcStat(Stats.BOSS_VULN, multiplier, target, null), Config.BOSS_VULN);
					break;
				case DEATH :
				case DERANGEMENT :
					if (champion != null)
					{
						multiplier = champion.getResistValue(Stats.DERANGEMENT_VULN);
					}
					multiplier = calcMin(target.calcStat(Stats.DERANGEMENT_VULN, multiplier, target, null), Config.MENTAL_VULN);
					break;
				case GUST :
					if (champion != null)
					{
						multiplier = champion.getResistValue(Stats.GUST_VULN);
					}
					multiplier = calcMin(target.calcStat(Stats.GUST_VULN, multiplier, target, null), Config.GUST_VULN);
					break;
				case HOLD :
					if (champion != null)
					{
						multiplier = champion.getResistValue(Stats.ROOT_VULN);
					}
					multiplier = calcMin(target.calcStat(Stats.ROOT_VULN, multiplier, target, null), Config.HOLD_VULN);
					break;
				case PARALYZE :
					if (champion != null)
					{
						multiplier = champion.getResistValue(Stats.PARALYZE_VULN);
					}
					multiplier = calcMin(target.calcStat(Stats.PARALYZE_VULN, multiplier, target, null), Config.PARALYZE_VULN);
					break;
				case PHYSICAL_BLOCKADE :
					if (champion != null)
					{
						multiplier = champion.getResistValue(Stats.PHYSICALBLOCKADE_VULN);
					}
					multiplier = calcMin(target.calcStat(Stats.PHYSICALBLOCKADE_VULN, multiplier, target, null), Config.PHYSICAL_BLOCKADE_VULN);
					break;
				case POISON :
					if (champion != null)
					{
						multiplier = champion.getResistValue(Stats.POISON_VULN);
					}
					multiplier = calcMin(target.calcStat(Stats.POISON_VULN, multiplier, target, null), Config.POISON_VULN);
					break;
				case SHOCK :
					if (champion != null)
					{
						multiplier = champion.getResistValue(Stats.STUN_VULN);
					}
					multiplier = calcMin(target.calcStat(Stats.STUN_VULN, multiplier, target, null), Config.SHOCK_VULN);
					break;
				case SLEEP :
					if (champion != null)
					{
						multiplier = champion.getResistValue(Stats.SLEEP_VULN);
					}
					multiplier = calcMin(target.calcStat(Stats.SLEEP_VULN, multiplier, target, null), Config.SLEEP_VULN);
					break;
				case VALAKAS :
					if (champion != null)
					{
						multiplier = champion.getResistValue(Stats.VALAKAS_VULN);
					}
					multiplier = calcMin(target.calcStat(Stats.VALAKAS_VULN, multiplier, target, null), Config.VALAKAS_VULN);
					break;
			}
		}
		else
		{
			final SkillType type = skill.getSkillType();
			if (type == SkillType.BUFF)
			{
				if (champion != null)
				{
					multiplier = champion.getResistValue(Stats.BUFF_VULN);
				}
				multiplier = calcMin(target.calcStat(Stats.BUFF_VULN, multiplier, target, null), Config.BUFF_VULN);
			}
			else if ((type == SkillType.DEBUFF) || (skill.isDebuff()))
			{
				if (champion != null)
				{
					multiplier = champion.getResistValue(Stats.DEBUFF_VULN);
				}
				multiplier = calcMin(target.calcStat(Stats.DEBUFF_VULN, multiplier, target, null), Config.DEBUFF_VULN);
			}
		}
		return multiplier;
	}
	
	private static double calcMin(double a, double b)
	{
		if (a < b)
		{
			return b;
		}
		return a;
	}

	public static double calcSkillProficiency(Skill skill, Creature attacker, Creature target)
	{
		double multiplier = 0;

		if (skill != null)
		{
			multiplier = calcSkillTraitProficiency(multiplier, attacker, target, skill);
		}

		return multiplier;
	}

	public static double calcSkillTraitProficiency(double multiplier, Creature attacker, Creature target, Skill skill)
	{
		if (skill == null)
		{
			return multiplier;
		}

		final TraitType trait = skill.getTraitType();

		if ((trait != null) && (trait != TraitType.NONE))
		{
			switch (trait)
			{
				case BLEED :
					multiplier = Math.min(attacker.calcStat(Stats.BLEED_PROF, multiplier, target, null), Config.BLEED_PROF);
					break;
				case DEATH :
				case DERANGEMENT :
					multiplier = Math.min(attacker.calcStat(Stats.DERANGEMENT_PROF, multiplier, target, null), Config.MENTAL_PROF);
					break;
				case HOLD :
					multiplier = Math.min(attacker.calcStat(Stats.ROOT_PROF, multiplier, target, null), Config.HOLD_PROF);
					break;
				case PARALYZE :
					multiplier = Math.min(attacker.calcStat(Stats.PARALYZE_PROF, multiplier, target, null), Config.PARALYZE_PROF);
					break;
				case POISON :
					multiplier = Math.min(attacker.calcStat(Stats.POISON_PROF, multiplier, target, null), Config.POISON_PROF);
					break;
				case SHOCK :
					multiplier = Math.min(attacker.calcStat(Stats.STUN_PROF, multiplier, target, null), Config.SHOCK_PROF);
					break;
				case SLEEP :
					multiplier = Math.min(attacker.calcStat(Stats.SLEEP_PROF, multiplier, target, null), Config.SLEEP_PROF);
					break;
				case VALAKAS :
					multiplier = Math.min(attacker.calcStat(Stats.VALAKAS_PROF, multiplier, target, null), Config.VALAKAS_PROF);
					break;
			}
		}
		else
		{
			final SkillType type = skill.getSkillType();
			if ((type == SkillType.DEBUFF) || (skill.isDebuff()))
			{
				multiplier = Math.min(target.calcStat(Stats.DEBUFF_PROF, multiplier, target, null), Config.DEBUFF_PROF);
			}
		}
		return multiplier;
	}

	public static double calcEffectStatMod(Skill skill, Creature target)
	{
		if (target.isNpc())
		{
			if (((!target.isRaid() && !target.isRaidMinion() && !Config.CALC_NPC_DEBUFFS_BY_STATS) || (target.isRaid() && !target.isRaidMinion() && !Config.CALC_RAID_DEBUFFS_BY_STATS)))
			{
				return 1;
			}
		}
		return skill.getSaveVs() != null ? Math.min(Math.max((2 - skill.getSaveVs().calcChanceMod(target)), 0.1), 1.) : 1;
	}

	public static double calcResMod(Creature attacker, Creature target, Skill skill)
	{
		final double vuln = calcSkillVulnerability(attacker, target, skill);
		final double checkVuln = Math.abs(vuln);
		if (checkVuln >= 100 && target.isNpc())
		{
			return 0;
		}
		final double prof = calcSkillProficiency(skill, attacker, target);
		final double resMod = 1 + ((vuln + prof) / 100);
		return Math.min(Math.max(resMod, 0.1), 1.9);
	}

	public static double calcLvlBonusMod(Creature attacker, Creature target, Skill skill)
	{
		final int attackerLvl = skill.getMagicLevel() > 0 ? skill.getMagicLevel() : attacker.getLevel();
		final double skillLvlBonusRateMod = 1 + (skill.getLvlBonusRate() / 100.);
		final double lvlMod = 1 + ((attackerLvl - target.getLevel()) / 100.);
		return skillLvlBonusRateMod * lvlMod;
	}

	public static double calcElementMod(Creature attacker, Creature target, Skill skill)
	{
		final byte skillElement = skill.getElement();
		if (skillElement == Elementals.NONE)
		{
			return 1;
		}

		final int attackerElement = attacker.getAttackElement() == skillElement ? attacker.getAttackElementValue(skillElement) + skill.getElementPower() : attacker.getAttackElementValue(skillElement);
		final int targetElement = target.getDefenseElementValue(skillElement);
		final double elementMod = 1 + ((attackerElement - targetElement) / 1000.);
		return elementMod;
	}
	
	public static double calcMAtkMod(Creature attacker, Creature target, Skill skill)
	{
		final double mdef = Math.max(1, target.getMDef(target, skill));
		double matk = attacker.getMAtk(target, skill);
		double val = 0;
		if (attacker.isChargedShot(ShotType.BLESSED_SPIRITSHOTS))
		{
			val = 2;
			matk *= val;
		}
		else if (attacker.isChargedShot(ShotType.SPIRITSHOTS))
		{
			val = 1.5;
			matk *= val;
		}
		return (Config.SKILLS_CHANCE_MOD * Math.pow(matk, Config.SKILLS_CHANCE_POW) / mdef);
	}

	public static boolean calcEffectSuccess(Creature attacker, Creature target, EffectTemplate effect, Skill skill, byte shld, boolean ss, boolean sps, boolean bss)
	{
		final double baseRate = effect.getEffectPower();
		if ((baseRate < 0) || skill.hasEffectType(EffectType.CANCEL_DEBUFF, EffectType.CANCEL))
		{
			return true;
		}

		if (skill.hasDebuffEffects() || skill.isDebuff() || skill.hasEffectType(EffectType.STUN))
		{
			if ((target.isNpc() && !(target instanceof Attackable) && !(target instanceof EffectPointInstance)))
			{
				attacker.sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
				return false;
			}

			if (target.isEkimusFood() || ((target.isRaid() || (target instanceof FortCommanderInstance)) && !Config.ALLOW_RAIDBOSS_CHANCE_DEBUFF) || ((target.isEpicRaid()) && !Config.ALLOW_GRANDBOSS_CHANCE_DEBUFF) || (target.isDoor()))
			{
				attacker.sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
				return false;
			}

			if (skill.getPower() == -1)
			{
				if (attacker.isDebug())
				{
					attacker.sendDebugMessage(skill.getName(attacker.getActingPlayer().getLang()) + " effect ignoring resists");
				}
				return true;
			}
			else if ((target.calcStat(Stats.DEBUFF_IMMUNITY, 0, null, skill) > 0 || target.isDebuffImmune()) && skill.canBeReflected())
			{
				return false;
			}
		}

		if (shld == SHIELD_DEFENSE_PERFECT_BLOCK)
		{
			if (attacker.isDebug())
			{
				attacker.sendDebugMessage(skill.getName(attacker.getActingPlayer().getLang()) + " effect blocked by shield");
			}

			return false;
		}

		final double statMod = calcEffectStatMod(skill, target);
		double rate = (baseRate * statMod);
		
		double mAtkMod = 1.0;
		if (skill.isMagic())
		{
			mAtkMod = calcMAtkMod(attacker, target, skill);
		}
		
		if (attacker.isNpc())
		{
			rate *= Config.SKILLS_MOB_CHANCE;
		}
		else
		{
			rate *= mAtkMod;
		}
		
		final double lvlBonusMod = calcLvlBonusMod(attacker, target, skill);
		rate *= lvlBonusMod;
		
		final double resMod = calcResMod(attacker, target, skill);
		if (resMod <= 0)
		{
			return false;
		}
		rate *= resMod;

		final double elementMod = calcElementMod(attacker, target, skill);
		rate *= elementMod;
		
		final int keyId = (target.isPlayer()) ? target.getActingPlayer().getClassId().getId() : (target.isMonster()) ? -1 : -2;
		final double multiplier = SkillBalanceParser.getInstance().getSkillValue(skill.getId() + ";" + keyId, SkillChangeType.Chance, target);

		if ((attacker.isDebug() || Config.DEVELOPER) && attacker.isPlayer())
		{
			final StringBuilder stat = new StringBuilder(100);
			StringUtil.append(stat, skill.getName(attacker.getActingPlayer().getLang()), " power:", String.valueOf(baseRate), " stat:", String.format("%1.2f", statMod), " res:", String.format("%1.2f", resMod), " elem:", String.format("%1.2f", elementMod), " lvl:", String.format("%1.2f", lvlBonusMod), " mAtkMod:", String.format("%1.2f", mAtkMod), " total:", String.valueOf(rate));
			final String result = stat.toString();
			if (attacker.isDebug())
			{
				attacker.sendDebugMessage(result);
			}
			if (Config.DEVELOPER)
			{
				_log.info(result);
			}
		}

		if (target.isRaid())
		{
			if (target.isEpicRaid())
			{
				if (Arrays.binarySearch(Config.GRANDBOSS_DEBUFF_SPECIAL, ((Npc) target).getId()) > 0)
				{
					rate *= Config.GRANDBOSS_CHANCE_DEBUFF_SPECIAL;
				}
				else
				{
					rate *= Config.GRANDBOSS_CHANCE_DEBUFF;
				}
			}
			else
			{
				if (Arrays.binarySearch(Config.RAIDBOSS_DEBUFF_SPECIAL, ((Npc) target).getId()) > 0)
				{
					rate *= Config.RAIDBOSS_CHANCE_DEBUFF_SPECIAL;
				}
				else
				{
					rate *= Config.RAIDBOSS_CHANCE_DEBUFF;
				}
			}
		}
		
		double finalRate = Math.max(rate, skill.getMinChance());
		finalRate *= multiplier;
		finalRate = Math.min(finalRate, skill.getMaxChance());
		
		if (Config.SKILL_CHANCE_SHOW)
		{
			if (attacker.isPlayer() && attacker.getActingPlayer().isSkillChanceShow())
			{
				attacker.sendMessage(skill.getName(attacker.getActingPlayer().getLang()) + ": " + String.format("%1.2f", finalRate) + "%");
			}
			if (target.isPlayer() && target.getActingPlayer().isSkillChanceShow())
			{
				target.sendMessage(attacker.getName(null) + " - " + skill.getName(target.getActingPlayer().getLang()) + ": " + String.format("%1.2f", finalRate) + "%");
			}
		}
		return Rnd.chance(finalRate);
	}

	public static boolean calcEffectSuccess(Env env)
	{
		final double baseRate = env.getEffect().getEffectTemplate().getEffectPower();
		if (baseRate < 0)
		{
			return true;
		}

		if (env.getSkill().isDebuff() || env.getSkill().hasEffectType(EffectType.STUN))
		{
			if ((env.getTarget().isNpc()) && !(env.getTarget() instanceof Attackable))
			{
				env.getCharacter().sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
				return false;
			}

			if (env.getTarget().isEkimusFood() || ((env.getTarget().isRaid() || (env.getTarget() instanceof FortCommanderInstance)) && !Config.ALLOW_RAIDBOSS_CHANCE_DEBUFF) || ((env.getTarget().isEpicRaid()) && !Config.ALLOW_GRANDBOSS_CHANCE_DEBUFF) || (env.getTarget().isDoor()))
			{
				env.getCharacter().sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
				return false;
			}

			if (env.getSkill().getPower() == -1)
			{
				if (env.getCharacter().isDebug())
				{
					env.getCharacter().sendDebugMessage(env.getSkill().getName(env.getPlayer().getActingPlayer().getLang()) + " effect ignoring resists");
				}
				return true;
			}
			else if (env.getTarget().calcStat(Stats.DEBUFF_IMMUNITY, 0, null, env.getSkill()) > 0)
			{
				return false;
			}
		}

		if (env.getShield() == SHIELD_DEFENSE_PERFECT_BLOCK)
		{
			if (env.getCharacter().isDebug())
			{
				env.getCharacter().sendDebugMessage(env.getSkill().getName(env.getPlayer().getActingPlayer().getLang()) + " effect blocked by shield");
			}
			return false;
		}

		final double statMod = calcEffectStatMod(env.getSkill(), env.getTarget());
		double rate = (baseRate * statMod);
		
		double mAtkMod = 1.0;
		if (env.getSkill().isMagic())
		{
			mAtkMod = calcMAtkMod(env.getCharacter(), env.getTarget(), env.getSkill());
		}
		
		rate *= env.getCharacter().isNpc() ? Config.SKILLS_MOB_CHANCE : mAtkMod;
		
		final double lvlBonusMod = calcLvlBonusMod(env.getCharacter(), env.getTarget(), env.getSkill());
		rate *= lvlBonusMod;
		
		final double resMod = calcResMod(env.getCharacter(), env.getTarget(), env.getSkill());
		if (resMod <= 0)
		{
			return false;
		}
		rate *= resMod;

		final double elementMod = calcElementMod(env.getCharacter(), env.getTarget(), env.getSkill());
		rate *= elementMod;
		
		final int keyId = (env.getTarget().isPlayer()) ? env.getTarget().getActingPlayer().getClassId().getId() : (env.getTarget().isMonster()) ? -1 : -2;
		final double multiplier = SkillBalanceParser.getInstance().getSkillValue(env.getSkill().getId() + ";" + keyId, SkillChangeType.Chance, env.getTarget());

		if ((env.getCharacter().isDebug() || Config.DEVELOPER) && env.getCharacter().isPlayer())
		{
			final StringBuilder stat = new StringBuilder(100);
			StringUtil.append(stat, "Effect Name: ", String.valueOf(env.getEffect().getEffectTemplate().getName()), " Base Rate: ", String.valueOf(baseRate), " Stat Type: ", String.valueOf(env.getSkill().getSaveVs()), " Stat Mod: ", String.format("%1.2f", statMod), " Res Mod: ", String.format("%1.2f", resMod), " Elem Mod: ", String.format("%1.2f", elementMod), " Lvl Mod: ", String.format("%1.2f", lvlBonusMod), " Final Rate: ", String.valueOf(rate));
			final String result = stat.toString();
			if (env.getCharacter().isDebug())
			{
				env.getCharacter().sendDebugMessage(result);
			}
			if (Config.DEVELOPER)
			{
				_log.info(result);
			}
		}

		if (env.getTarget().isRaid())
		{
			if (env.getTarget().isEpicRaid())
			{
				if (Arrays.binarySearch(Config.GRANDBOSS_DEBUFF_SPECIAL, ((Npc) env.getTarget()).getId()) > 0)
				{
					rate *= Config.GRANDBOSS_CHANCE_DEBUFF_SPECIAL;
				}
				else
				{
					rate *= Config.GRANDBOSS_CHANCE_DEBUFF;
				}
			}
			else
			{
				if (Arrays.binarySearch(Config.RAIDBOSS_DEBUFF_SPECIAL, ((Npc) env.getTarget()).getId()) > 0)
				{
					rate *= Config.RAIDBOSS_CHANCE_DEBUFF_SPECIAL;
				}
				else
				{
					rate *= Config.RAIDBOSS_CHANCE_DEBUFF;
				}
			}
		}
		
		double finalRate = Math.max(rate, env.getSkill().getMinChance());
		finalRate *= multiplier;
		finalRate = Math.min(finalRate, env.getSkill().getMaxChance());
		
		if (Config.SKILL_CHANCE_SHOW)
		{
			if (env.getCharacter().isPlayer() && env.getCharacter().getActingPlayer().isSkillChanceShow())
			{
				env.getCharacter().sendMessage(env.getSkill().getName(env.getPlayer().getActingPlayer().getLang()) + ": " + String.format("%1.2f", finalRate) + "%");
			}
			if (env.getTarget().isPlayer() && env.getTarget().getActingPlayer().isSkillChanceShow())
			{
				env.getTarget().sendMessage(env.getCharacter().getName(null) + " - " + env.getSkill().getName(env.getTarget().getActingPlayer().getLang()) + ": " + String.format("%1.2f", finalRate) + "%");
			}
		}
		
		if (!Rnd.chance(finalRate))
		{
			final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_RESISTED_YOUR_S2);
			sm.addCharName(env.getTarget());
			sm.addSkillName(env.getSkill());
			env.getCharacter().sendPacket(sm);
			return false;
		}
		return true;
	}

	public static boolean calcSkillSuccess(Creature attacker, Creature target, Skill skill, byte shld, boolean ss, boolean sps, boolean bss)
	{
		if (skill.hasDebuffEffects() || skill.isDebuff() || skill.hasEffectType(EffectType.STUN))
		{
			if ((target.isNpc() && !(target instanceof Attackable) && !(target instanceof EffectPointInstance)))
			{
				attacker.sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
				return false;
			}

			if (target.isEkimusFood() || ((target.isRaid() || (target instanceof FortCommanderInstance)) && !Config.ALLOW_RAIDBOSS_CHANCE_DEBUFF) || ((target.isEpicRaid()) && !Config.ALLOW_GRANDBOSS_CHANCE_DEBUFF) || (target.isDoor()))
			{
				attacker.sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
				return false;
			}

			if (skill.getPower() == -1)
			{
				if (attacker.isDebug())
				{
					attacker.sendDebugMessage(skill.getName(attacker.getActingPlayer().getLang()) + " ignoring resists");
				}
				return true;
			}
			else if ((target.calcStat(Stats.DEBUFF_IMMUNITY, 0, null, skill) > 0 || target.isDebuffImmune()) && skill.canBeReflected())
			{
				return false;
			}
		}

		if (shld == SHIELD_DEFENSE_PERFECT_BLOCK)
		{
			if (attacker.isDebug())
			{
				attacker.sendDebugMessage(skill.getName(attacker.getActingPlayer().getLang()) + " blocked by shield");
			}

			return false;
		}

		final double baseRate = skill.getPower();
		final double statMod = calcEffectStatMod(skill, target);
		double rate = (baseRate * statMod);
		
		double mAtkMod = 1.0;
		if (skill.isMagic())
		{
			mAtkMod = calcMAtkMod(attacker, target, skill);
		}
		
		rate *= attacker.isNpc() ? Config.SKILLS_MOB_CHANCE : mAtkMod;
		
		final double lvlBonusMod = calcLvlBonusMod(attacker, target, skill);
		rate *= lvlBonusMod;
		
		final double resMod = calcResMod(attacker, target, skill);
		if (resMod <= 0)
		{
			return false;
		}
		rate *= resMod;
		
		final double elementMod = calcElementMod(attacker, target, skill);
		rate *= elementMod;
		
		final int keyId = (target.isPlayer()) ? target.getActingPlayer().getClassId().getId() : (target.isMonster()) ? -1 : -2;
		final double multiplier = SkillBalanceParser.getInstance().getSkillValue(skill.getId() + ";" + keyId, SkillChangeType.Chance, target);

		if (target.isRaid())
		{
			if (target.isEpicRaid())
			{
				if (Arrays.binarySearch(Config.GRANDBOSS_DEBUFF_SPECIAL, ((Npc) target).getId()) > 0)
				{
					rate *= Config.GRANDBOSS_CHANCE_DEBUFF_SPECIAL;
				}
				else
				{
					rate *= Config.GRANDBOSS_CHANCE_DEBUFF;
				}
			}
			else
			{
				if (Arrays.binarySearch(Config.RAIDBOSS_DEBUFF_SPECIAL, ((Npc) target).getId()) > 0)
				{
					rate *= Config.RAIDBOSS_CHANCE_DEBUFF_SPECIAL;
				}
				else
				{
					rate *= Config.RAIDBOSS_CHANCE_DEBUFF;
				}
			}
		}
		
		double finalRate = Math.max(rate, skill.getMinChance());
		finalRate *= multiplier;
		finalRate = Math.min(finalRate, skill.getMaxChance());
		
		if ((attacker.isDebug() || Config.DEVELOPER) && attacker.isPlayer())
		{
			final StringBuilder stat = new StringBuilder(100);
			StringUtil.append(stat, skill.getName(attacker.getActingPlayer().getLang()), " type:", skill.getSkillType().toString(), " power:", String.valueOf(baseRate), " stat:", String.format("%1.2f", statMod), " res:", String.format("%1.2f", resMod), " elem:", String.format("%1.2f", elementMod), " lvl:", String.format("%1.2f", lvlBonusMod), " mAtkMod:", String.format("%1.2f", mAtkMod), " total:", String.valueOf(finalRate));
			final String result = stat.toString();
			if (attacker.isDebug())
			{
				attacker.sendDebugMessage(result);
			}
			if (Config.DEVELOPER)
			{
				_log.info(result);
			}
		}
		
		if (Config.SKILL_CHANCE_SHOW)
		{
			if (attacker.isPlayer() && attacker.getActingPlayer().isSkillChanceShow())
			{
				attacker.sendMessage(skill.getName(attacker.getActingPlayer().getLang()) + ": " + String.format("%1.2f", finalRate) + "%");
			}
			if (target.isPlayer() && target.getActingPlayer().isSkillChanceShow())
			{
				target.sendMessage(attacker.getName(null) + " - " + skill.getName(target.getActingPlayer().getLang()) + ": " + String.format("%1.2f", finalRate) + "%");
			}
		}
		return Rnd.chance(finalRate);
	}

	public static boolean calcSkillSuccess(Creature attacker, Creature target, Skill skill, byte shld, boolean ss, boolean sps, boolean bss, int activateRate)
	{
		final Env env = new Env();
		env._character = attacker;
		env._target = target;
		env._skill = skill;
		env._shield = shld;
		env._soulShot = ss;
		env._spiritShot = sps;
		env._blessedSpiritShot = bss;
		env._value = activateRate;
		return calcSkillSuccess(attacker, target, skill, shld, ss, sps, bss);
	}

	public static boolean calcCubicSkillSuccess(CubicInstance attacker, Creature target, Skill skill, byte shld)
	{
		if (skill.hasDebuffEffects() || skill.isDebuff() || skill.hasEffectType(EffectType.STUN))
		{
			if ((target.isNpc() && !(target instanceof Attackable) && !(target instanceof EffectPointInstance)))
			{
				attacker.getOwner().sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
				return false;
			}

			if (target.isEkimusFood() || ((target.isRaid() || (target instanceof FortCommanderInstance)) && !Config.ALLOW_RAIDBOSS_CHANCE_DEBUFF) || ((target.isEpicRaid()) && !Config.ALLOW_GRANDBOSS_CHANCE_DEBUFF) || (target.isDoor()))
			{
				attacker.getOwner().sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
				return false;
			}

			if (skill.getPower() == -1)
			{
				return true;
			}
			else if ((target.calcStat(Stats.DEBUFF_IMMUNITY, 0, null, skill) > 0 || target.isDebuffImmune()) && skill.canBeReflected())
			{
				return false;
			}
		}

		if (shld == SHIELD_DEFENSE_PERFECT_BLOCK)
		{
			return false;
		}

		if (calcSkillReflect(target, skill) != SKILL_REFLECT_FAILED)
		{
			return false;
		}

		final double baseRate = skill.getPower();
		final double statMod = calcEffectStatMod(skill, target);
		double rate = (baseRate * statMod);
		
		double mAtkMod = 1.0;
		if (skill.isMagic())
		{
			mAtkMod = calcMAtkMod(attacker.getOwner(), target, skill);
		}
		rate *= mAtkMod;
		
		final double lvlBonusMod = calcLvlBonusMod(attacker.getOwner(), target, skill);
		rate *= lvlBonusMod;
		
		final double resMod = calcResMod(attacker.getOwner(), target, skill);
		if (resMod <= 0)
		{
			return false;
		}
		rate *= resMod;
		
		final double elementMod = calcElementMod(attacker.getOwner(), target, skill);
		rate *= elementMod;
		
		final int keyId = (target.isPlayer()) ? target.getActingPlayer().getClassId().getId() : (target.isMonster()) ? -1 : -2;
		final double multiplier = SkillBalanceParser.getInstance().getSkillValue(skill.getId() + ";" + keyId, SkillChangeType.Chance, target);

		if (target.isRaid())
		{
			if (target.isEpicRaid())
			{
				if (Arrays.binarySearch(Config.GRANDBOSS_DEBUFF_SPECIAL, ((Npc) target).getId()) > 0)
				{
					rate *= Config.GRANDBOSS_CHANCE_DEBUFF_SPECIAL;
				}
				else
				{
					rate *= Config.GRANDBOSS_CHANCE_DEBUFF;
				}
			}
			else
			{
				if (Arrays.binarySearch(Config.RAIDBOSS_DEBUFF_SPECIAL, ((Npc) target).getId()) > 0)
				{
					rate *= Config.RAIDBOSS_CHANCE_DEBUFF_SPECIAL;
				}
				else
				{
					rate *= Config.RAIDBOSS_CHANCE_DEBUFF;
				}
			}
		}
		
		double finalRate = Math.max(rate, skill.getMinChance());
		finalRate *= multiplier;
		finalRate = Math.min(finalRate, skill.getMaxChance());
		
		if ((attacker.getOwner().isDebug() || Config.DEVELOPER) && attacker.getOwner().isPlayer())
		{
			final StringBuilder stat = new StringBuilder(100);
			StringUtil.append(stat, skill.getName(attacker.getOwner().getActingPlayer().getLang()), " type:", skill.getSkillType().toString(), " power:", String.valueOf(baseRate), " stat:", String.format("%1.2f", statMod), " res:", String.format("%1.2f", resMod), " elem:", String.format("%1.2f", elementMod), " lvl:", String.format("%1.2f", lvlBonusMod), " mAtkMod:", String.format("%1.2f", mAtkMod), " total:", String.valueOf(finalRate));
			final String result = stat.toString();
			if (attacker.getOwner().isDebug())
			{
				attacker.getOwner().sendDebugMessage(result);
			}
			if (Config.DEVELOPER)
			{
				_log.info(result);
			}
		}
		return Rnd.chance(finalRate);
	}

	public static boolean calcMagicSuccess(Creature attacker, Creature target, Skill skill, boolean correctChance)
	{
		if (skill.getPower() == -1)
		{
			return true;
		}

		final int lvlDifference = (target.getLevel() - (skill.getMagicLevel() > 0 ? skill.getMagicLevel() : attacker.getLevel()));
		final double lvlModifier = Math.pow(1.3, lvlDifference);
		final int levelMod = target.getLevel() - attacker.getLevel();
		final float targetModifier = 1;
		final double resModifier = target.calcStat(Stats.MAGIC_SUCCESS_RES, 1, null, skill);
		double rate = 100 - Math.round((float) ((lvlModifier + targetModifier) * resModifier));

		if (attacker.isPlayer())
		{
			if (correctChance)
			{
				if (levelMod <= 5)
				{
					if (rate > skill.getMaxChance() || rate < skill.getMaxChance())
					{
						rate = skill.getMaxChance();
					}
				}
				else if (levelMod > 5 && levelMod <= 9)
				{
					rate -= (levelMod * 1.5);
					if (rate < skill.getMinChance())
					{
						rate = skill.getMinChance();
					}
				}
				else
				{
					rate -= (levelMod * 3);
					rate /= 2;
					if (rate < skill.getMinChance())
					{
						rate = skill.getMinChance();
					}
				}
			}
			else
			{
				if (rate > skill.getMaxChance())
				{
					rate = skill.getMaxChance();
				}
				else if (rate < skill.getMinChance())
				{
					rate = skill.getMinChance();
				}
			}
		}

		if ((attacker.isDebug() || Config.DEVELOPER) && attacker.isPlayer())
		{
			final StringBuilder stat = new StringBuilder(100);
			StringUtil.append(stat, skill.getName(attacker.getActingPlayer().getLang()), " lvlDiff:", String.valueOf(lvlDifference), " lvlMod:", String.format("%1.2f", lvlModifier), " res:", String.format("%1.2f", resModifier), " fail:", " tgt:", String.valueOf(targetModifier), " total:", String.valueOf(rate));
			final String result = stat.toString();
			if (attacker.isDebug())
			{
				attacker.sendDebugMessage(result);
			}
			if (Config.DEVELOPER)
			{
				_log.info(result);
			}
		}
		return (Rnd.get(100) < rate);
	}

	public static double calcManaDam(Creature attacker, Creature target, Skill skill, boolean ss, boolean bss)
	{
		double mAtk = attacker.getMAtk(target, skill);
		final double mDef = target.getMDef(attacker, skill);
		final boolean isPvP = attacker.isPlayable() && target.isPlayable();
		final boolean isPvE = attacker.isPlayable() && target.isAttackable();
		final boolean isPvEAtt = attacker.isAttackable() && target.isPlayable();
		final double mp = target.getMaxMp();
		if (bss)
		{
			mAtk *= 4;
		}
		else if (ss)
		{
			mAtk *= 2;
		}

		double damage = (Math.sqrt(mAtk) * skill.getPower(attacker, target, isPvP, isPvE) * (mp / 97)) / mDef;
		damage *= (1 + (calcSkillVulnerability(attacker, target, skill) / 100));
		if (isPvE)
		{
			damage *= attacker.calcStat(Stats.PVE_MAGICAL_DMG, 1, null, null);
		}
		
		if (isPvEAtt)
		{
			damage /= target.calcStat(Stats.PVE_MAGICAL_DEF, 1, null, null);
		}
		return damage;
	}

	public static double calculateSkillResurrectRestorePercent(double baseRestorePercent, Creature caster)
	{
		if ((baseRestorePercent == 0) || (baseRestorePercent == 100))
		{
			return baseRestorePercent;
		}

		double restorePercent = baseRestorePercent * BaseStats.WIT.calcBonus(caster);
		if ((restorePercent - baseRestorePercent) > 20.0)
		{
			restorePercent += 20.0;
		}

		restorePercent = Math.max(restorePercent, baseRestorePercent);
		restorePercent = Math.min(restorePercent, 90.0);

		return restorePercent;
	}

	public static boolean calcPhysicalSkillEvasion(Creature activeChar, Creature target, Skill skill)
	{
		if ((skill.isMagic() && (skill.getSkillType() != SkillType.BLOW && skill.getSkillType() != SkillType.PDAM && skill.getSkillType() != SkillType.FATAL && skill.getSkillType() != SkillType.CHARGEDAM)) || skill.isDebuff())
		{
			return false;
		}
		
		if (Rnd.get(100) < target.calcStat(Stats.P_SKILL_EVASION, 0, null, skill))
		{
			if (activeChar.isPlayer())
			{
				final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_DODGES_ATTACK);
				sm.addString(target.getName(null));
				activeChar.getActingPlayer().sendPacket(sm);
			}
			if (target.isPlayer())
			{
				final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.AVOIDED_C1_ATTACK2);
				sm.addString(activeChar.getName(null));
				target.getActingPlayer().sendPacket(sm);
			}
			return true;
		}
		return false;
	}

	public static boolean calcSkillMastery(Creature actor, Skill sk)
	{
		if (sk.isStatic() || sk.isBlockSkillMastery())
		{
			return false;
		}
		return ((actor.getSkillLevel(331) > 0 && actor.calcStat(Stats.SKILL_MASTERY, actor.getINT(), null, sk) >= Rnd.get(3000)) || (actor.getSkillLevel(330) > 0 && actor.calcStat(Stats.SKILL_MASTERY, actor.getSTR(), null, sk) >= Rnd.get(3000)));
	}

	public static double calcValakasTrait(Creature attacker, Creature target, Skill skill)
	{
		double calcPower = 0;
		double calcDefen = 0;

		if ((skill != null) && (skill.getTraitType() == TraitType.VALAKAS))
		{
			calcPower = attacker.calcStat(Stats.VALAKAS_PROF, calcPower, target, skill);
			calcDefen = target.calcStat(Stats.VALAKAS_VULN, calcDefen, target, skill);
		}
		else
		{
			calcPower = attacker.calcStat(Stats.VALAKAS_PROF, calcPower, target, skill);
			if (calcPower > 0)
			{
				calcPower = attacker.calcStat(Stats.VALAKAS_PROF, calcPower, target, skill);
				calcDefen = target.calcStat(Stats.VALAKAS_VULN, calcDefen, target, skill);
			}
		}
		return 1 + ((calcDefen + calcPower) / 100);
	}

	public static double calcElemental(Creature attacker, Creature target, Skill skill)
	{
		int calcPower = 0;
		int calcDefen = 0;
		int calcTotal = 0;
		double result = 1.0;
		byte element;

		if (skill != null)
		{
			element = skill.getElement();
			if (element >= 0)
			{
				calcPower = skill.getElementPower();
				calcDefen = target.getDefenseElementValue(element);
				
				final var summon = attacker.getSummon();
				if (summon != null)
				{
					final var owner = attacker.getActingPlayer();
					if (owner != null && summon.isServitor())
					{
						if (owner.getAttackElement() == element)
						{
							calcPower += Math.max(owner.getAttackElementValue(element) * summon.getMagicAttributteMod(), 0);
						}
					}
				}
				else
				{
					if (attacker.getAttackElement() == element)
					{
						calcPower += attacker.getAttackElementValue(element);
					}
				}

				calcTotal = calcPower - calcDefen;
				calcTotal = Math.max(calcTotal, 0);
				if (calcTotal <= 0)
				{
					result = 1.0;
				}
				else if (calcTotal > 0 && calcTotal < 50)
				{
					result = 1.0 + (calcTotal * 0.003948);
				}
				else if (calcTotal >= 50 && calcTotal < 150)
				{
					result = 1.2;
				}
				else if (calcTotal >= 150 && calcTotal < 300)
				{
					result = 1.4;
				}
				else
				{
					result = 1.7;
				}
				
				if (Config.DEVELOPER)
				{
					_log.info(skill.getName(null) + ": " + calcPower + ", " + calcDefen + ", " + result);
				}
			}
		}
		else
		{
			element = attacker.getAttackElement();
			if (element >= 0)
			{
				final var summon = attacker.getSummon();
				if (summon != null)
				{
					final var owner = attacker.getActingPlayer();
					if (owner != null && summon.isServitor())
					{
						calcTotal = (int) Math.max((owner.getAttackElementValue(element) * summon.getPhysAttributteMod()) - target.getDefenseElementValue(element), 0);
					}
				}
				else
				{
					calcTotal = Math.max(attacker.getAttackElementValue(element) - target.getDefenseElementValue(element), 0);
				}
				
				if (calcTotal <= 0)
				{
					result = 1.0;
				}
				else if (calcTotal > 0 && calcTotal < 50)
				{
					result = 1.0 + (calcTotal * 0.003948);
				}
				else if (calcTotal >= 50 && calcTotal < 150)
				{
					result = 1.2;
				}
				else if (calcTotal >= 150 && calcTotal < 300)
				{
					result = 1.4;
				}
				else
				{
					result = 1.7;
				}

				if (Config.DEVELOPER)
				{
					_log.info("Hit: " + calcPower + ", " + calcDefen + ", " + result);
				}
			}
		}
		return result;
	}

	public static void calcDamageReflected(Creature activeChar, Creature target, Skill skill, double damage, boolean crit)
	{
		boolean reflect = true;
		if ((skill.getCastRange() == -1) || (skill.getCastRange() > MELEE_ATTACK_RANGE))
		{
			reflect = false;
		}

		if (reflect)
		{
			final double vengeanceChance = target.getStat().calcStat(Stats.VENGEANCE_SKILL_PHYSICAL_DAMAGE, 0, target, skill);
			if (vengeanceChance > Rnd.get(100))
			{
				calcReflectDamage(activeChar, target, skill, damage, crit);
			}
		}
	}
	
	public static void calcReflectDamage(Creature activeChar, Creature target, Skill skill, double damage, boolean crit)
	{
		final int amount = Config.ALLOW_REFLECT_DAMAGE ? skill.getElementPower() > 0 ? 2 : skill.getPvpPower() != skill.getPower() ? 0 : 1 : 1;
		if (amount == 0)
		{
			return;
		}
		if (target.isPlayer())
		{
			final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.COUNTERED_C1_ATTACK);
			sm.addCharName(activeChar);
			target.sendPacket(sm);
		}
		if (activeChar.isPlayer())
		{
			final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_PERFORMING_COUNTERATTACK);
			sm.addCharName(target);
			activeChar.sendPacket(sm);
		}
		
		final Stats stat = skill.isMagic() ? Stats.REFLECT_MAGIC_SKILLS_PERCENT : Stats.REFLECT_PHYS_SKILLS_PERCENT;
		final double reflectPercent = target.getStat().calcStat(stat, 0, target, skill);
		
		for (int i = 0; i < amount; i++)
		{
			if (reflectPercent > 0)
			{
				double reflectedDamage = (int) ((reflectPercent / 100.) * damage);
				if (crit)
				{
					reflectedDamage *= 2;
				}
				
				if (reflectedDamage > target.getMaxHp())
				{
					reflectedDamage = target.getMaxHp();
				}
				
				if (reflectedDamage > 0 && !activeChar.isInvul() && !activeChar.isDead())
				{
					activeChar.reduceCurrentHp(reflectedDamage, target, skill);
				}
			}
			else
			{
				double vegdamage = ((1189 * target.getPAtk(activeChar)) / activeChar.getPDef(target));
				if (crit)
				{
					vegdamage *= 2;
				}
				activeChar.reduceCurrentHp(vegdamage, target, skill);
			}
		}
	}

	public static byte calcSkillReflect(Creature target, Skill skill)
	{
		if (!skill.canBeReflected() || (skill.getPower() == -1))
		{
			return SKILL_REFLECT_FAILED;
		}

		if (!skill.isMagic() && ((skill.getCastRange() == -1) || (skill.getCastRange() > MELEE_ATTACK_RANGE)))
		{
			return SKILL_REFLECT_FAILED;
		}

		byte reflect = SKILL_REFLECT_FAILED;
		switch (skill.getSkillType())
		{
			case FEAR :
			case ROOT :
			case STUN :
			case MUTE :
			case BLEED :
			case PARALYZE :
			case SLEEP :
			case DEBUFF :
			case PDAM :
			case MDAM :
			case BLOW :
			case DRAIN :
			case CHARGEDAM :
			case FATAL :
			case DEATHLINK :
			case MANADAM :
			case CPDAMPERCENT :
				final Stats stat = skill.isMagic() ? Stats.VENGEANCE_SKILL_MAGIC_DAMAGE : Stats.VENGEANCE_SKILL_PHYSICAL_DAMAGE;
				final double venganceChance = target.getStat().calcStat(stat, 0, target, skill);
				if (venganceChance > Rnd.get(100))
				{
					reflect |= SKILL_REFLECT_VENGEANCE;
				}
				break;
		}

		final double reflectChance = target.calcStat(skill.isMagic() ? Stats.REFLECT_SKILL_MAGIC : Stats.REFLECT_SKILL_PHYSIC, 0, null, skill);
		if (Rnd.get(100) < reflectChance)
		{
			reflect |= SKILL_REFLECT_SUCCEED;
		}
		return reflect;
	}

	public static boolean calcBlowSuccess(Creature activeChar, Creature target, Skill skill)
	{
		final double baseWeaponCrit = activeChar.getActiveWeaponItem() == null ? 1.2 : 1.6;
		final double dexMod = BaseStats.DEX.calcBonus(activeChar);
		final double critHeightBonus = 0.008 * Math.min(25, Math.max(-25, target.getZ() - activeChar.getZ())) + 1.1;
		final double blowChance = skill.getBlowChance();
		final double baseRate = activeChar.calcStat(Stats.BLOW_RATE, 1, target, skill);
		double rate = blowChance * dexMod * critHeightBonus * baseRate * baseWeaponCrit;
		
		if (!target.isInCombat())
		{
			rate *= 1.1;
		}
		
		var isBackstab = false;
		final var direction = PositionUtils.getDirectionTo(target, activeChar);
		switch (direction)
		{
			case BEHIND :
				if (skill.isBehind())
				{
					rate = 100;
					isBackstab = true;
				}
				else
				{
					rate *= 1.3;
				}
				break;
			case SIDE :
				rate *= skill.isBehind() ? 0.5 : 1.1;
				break;
		}
		
		double finalRate = direction == TargetDirection.FRONT && skill.isBehind() ? 3.0 : isBackstab ? rate : skill.isBehind() ? Math.min(rate, skill.getBlowChance()) : Math.max(rate, skill.getBlowChance());
		finalRate = Math.min(finalRate, skill.isBehind() ? 100 : 80);
		
		final int keyId = (target.isPlayer()) ? target.getActingPlayer().getClassId().getId() : (target.isMonster()) ? -1 : -2;
		final double multiplier = SkillBalanceParser.getInstance().getSkillValue(skill.getId() + ";" + keyId, SkillChangeType.Chance, target);
		finalRate *= multiplier;
		final boolean result = Rnd.chance(finalRate);
		if (result)
		{
			activeChar.setDirection(direction);
		}
		
		if (Config.SKILL_CHANCE_SHOW && activeChar.isPlayer() && activeChar.getActingPlayer().isSkillChanceShow())
		{
			activeChar.sendMessage(skill.getName(activeChar.getActingPlayer().getLang()) + ": " + String.format("%1.2f", finalRate) + "%");
		}
		return result;
	}

	public static List<Effect> calcCancelStealEffects(Creature activeChar, Creature target, Skill skill, String slot, int rate, int min, int max, boolean randomEffects, boolean checkResistAmount, boolean isStealBuff)
	{
		int total = 0;
		if (min > 0 && min != max)
		{
			total = Rnd.get(min, max);
		}
		else
		{
			total = max;
		}
		
		if (total <= 0)
		{
			return Collections.emptyList();
		}
		
		final List<Effect> canceled = new ArrayList<>(total);
		switch (slot)
		{
			case "buff" :
			{
				final int cancelMagicLvl = skill.getMagicLevel();
				final double vuln = target.calcStat(Stats.CANCEL_VULN, 0, target, null);
				final double prof = activeChar.calcStat(Stats.CANCEL_PROF, 0, target, null);
				final double resMod = 1 + (((vuln + prof) * -1) / 100);
				final double finalRate = rate / resMod;
				
				if (Config.SKILL_CHANCE_SHOW)
				{
					if (activeChar.isPlayer() && activeChar.getActingPlayer().isSkillChanceShow())
					{
						activeChar.sendMessage(skill.getName(activeChar.getActingPlayer().getLang()) + ": " + String.format("%1.2f", finalRate) + "%");
					}
				}

				if ((activeChar.isDebug() || Config.DEVELOPER) && activeChar.isPlayer())
				{
					final StringBuilder stat = new StringBuilder(100);
					StringUtil.append(stat, skill.getName(activeChar.getActingPlayer().getLang()), " Base Rate:", String.valueOf(rate), " Magiclvl:", String.valueOf(cancelMagicLvl), " resMod:", String.format("%1.2f", resMod), " Rate:", String.format("%1.2f", finalRate));
					final String result = stat.toString();
					if (activeChar.isDebug())
					{
						activeChar.sendDebugMessage(result);
					}
					if (Config.DEVELOPER)
					{
						_log.info(result);
					}
				}
				
				final Effect[] effects = target.getAllEffects();
				final List<Effect> musicList = new LinkedList<>();
				final List<Effect> buffList = new LinkedList<>();

				for (final Effect eff : effects)
				{
					if ((eff == null) || !eff.isIconDisplay() || eff.getSkill().isOffensive() || !eff.canBeStolen() || eff.getSkill().isTriggeredSkill() || eff.getSkill().isToggle() || (isStealBuff && eff.getSkill().isCantSteal()))
					{
						continue;
					}
					
					if (eff.getSkill().isDance())
					{
						musicList.add(eff);
					}
					else
					{
						buffList.add(eff);
					}
				}
				
				final List<Effect> effectList = new LinkedList<>();
				Collections.reverse(musicList);
				Collections.reverse(buffList);
				effectList.addAll(musicList);
				effectList.addAll(buffList);
				if (randomEffects)
				{
					Collections.shuffle(effectList);
				}
				
				if (!effectList.isEmpty())
				{
					int i = 0;
					for (final Effect e : effectList)
					{
						if (!e.canBeStolen())
						{
							continue;
						}
						
						if (!calcStealSuccess(activeChar, target, skill, rate))
						{
							if (checkResistAmount)
							{
								i++;
								if (i >= total)
								{
									break;
								}
							}
							continue;
						}
						
						i++;
						canceled.add(e);
						if (i >= total)
						{
							break;
						}
					}
				}
				break;
			}
			case "debuff" :
			{
				for (final Effect info : target.getAllEffects())
				{
					if ((info == null) || !info.isIconDisplay() || !info.getSkill().isOffensive() || !info.getSkill().canBeDispeled() || (isStealBuff && info.getSkill().isCantSteal()))
					{
						continue;
					}
					
					if (Rnd.get(100) <= rate)
					{
						canceled.add(info);
						if (canceled.size() >= total)
						{
							break;
						}
					}
				}
				break;
			}
		}
		return canceled;
	}
	
	public static boolean calcStealSuccess(Creature activeChar, Creature target, Skill skill, double power)
	{
		final double vuln = target.calcStat(Stats.CANCEL_VULN, 0, target, null);
		final double prof = activeChar.calcStat(Stats.CANCEL_PROF, 0, target, null);
		final double resMod = 1 + (((vuln + prof) * -1) / 100);
		final double rate = power / resMod;
		final int keyId = (target.isPlayer()) ? target.getActingPlayer().getClassId().getId() : (target.isMonster()) ? -1 : -2;
		final double multiplier = SkillBalanceParser.getInstance().getSkillValue(skill.getId() + ";" + keyId, SkillChangeType.Chance, target);
		double finalRate = Math.max(rate, skill.getMinChance());
		finalRate *= multiplier;
		finalRate = Math.min(finalRate, skill.getMaxChance());
		return Rnd.chance(finalRate);
	}
	
	private static int calcSimpleTime(Creature target, Skill skill, EffectTemplate template)
	{
		if (target != null && skill != null && Config.SKILL_DURATION_LIST_SIMPLE.containsKey(skill.getId()))
		{
			return Config.SKILL_DURATION_LIST_SIMPLE.get(skill.getId());
		}
		return (template.getAbnormalTime() != 0) || (skill == null) ? template.getAbnormalTime() : skill.isPassive() || skill.isToggle() ? -1 : skill.getAbnormalTime();
	}
	
	private static int calcPremiumTime(Creature target, Skill skill, EffectTemplate template)
	{
		if (Config.SKILL_DURATION_LIST_PREMIUM.containsKey(skill.getId()))
		{
			return Config.SKILL_DURATION_LIST_PREMIUM.get(skill.getId());
		}
		return (template.getAbnormalTime() != 0) || (skill == null) ? template.getAbnormalTime() : skill.isPassive() || skill.isToggle() ? -1 : skill.getAbnormalTime();
	}

	public static int calcEffectAbnormalTime(Env env, EffectTemplate template)
	{
		final Creature caster = env.getCharacter();
		final Creature target = env.getTarget();
		final Skill skill = env.getSkill();
		if (skill != null && !skill.isToggle() && template.getTotalTickCount() > 0)
		{
			return 1;
		}
		
		int time;
		
		if (Config.ENABLE_MODIFY_SKILL_DURATION && target != null && skill != null)
		{
			time = target.hasPremiumBonus() ? calcPremiumTime(target, skill, template) : calcSimpleTime(target, skill, template);
		}
		else
		{
			time = (template.getAbnormalTime() != 0) || (skill == null) ? template.getAbnormalTime() : skill.isPassive() || skill.isToggle() ? -1 : skill.getAbnormalTime();
		}
		
		if ((target != null) && target.isServitor() && (skill != null) && skill.isAbnormalInstant())
		{
			time /= 2;
		}

		if (env.isSkillMastery())
		{
			time *= 2;
		}

		if ((caster != null) && (target != null) && (skill != null) && skill.hasDebuffEffects() && !target.isRaid() && Config.ALLOW_DEBUFF_RES_TIME)
		{
			double res = 0;
			res += calcSkillTraitVulnerability(0, target, skill, target.getChampionTemplate());
			res += calcSkillTraitProficiency(0, caster, target, skill);
			res -= calcElementMod(caster, target, skill);
			if (res != 0)
			{
				double mod = 1 + Math.abs(0.01 * res);
				if (res > 0)
				{
					mod = 1. / mod;
				}
				time = (int) Math.floor(Math.max(time / mod, 1));
				time *= caster.getStat().calcStat(Stats.DEBUFF_RATE, 1, null, null);
			}
		}
		
		if (caster != null && skill != null && !skill.isDebuff() && time > 1)
		{
			time *= caster.getStat().calcStat(Stats.BUFF_RATE, 1, null, null);
			if (target != null && ((caster == target) || (caster.isPlayer() && caster.getActingPlayer().isSellingBuffs())))
			{
				time *= caster.getStat().calcStat(Stats.SELF_BUFF_RATE, 1, null, null);
			}
		}
		
		if (caster != null && (caster.isDebug() || Config.DEVELOPER) && caster.isPlayer() && time > 1)
		{
			final StringBuilder stat = new StringBuilder(100);
			StringUtil.append(stat, "Effect Name: ", String.valueOf(skill.getName(caster.getActingPlayer().getLang())), " Time: ", String.valueOf(time));
			final String result = stat.toString();
			if (env.getCharacter().isDebug())
			{
				env.getCharacter().sendDebugMessage(result);
			}
			if (Config.DEVELOPER)
			{
				_log.info(result);
			}
		}
		return time;
	}
	
	public static int calcEffectTickCount(Env env, EffectTemplate template)
	{
		final Creature caster = env.getCharacter();
		final Creature target = env.getTarget();
		final Skill skill = env.getSkill();
		
		int tickCount = template.getTotalTickCount();
		if (tickCount <= 0)
		{
			return 0;
		}
		
		if ((caster != null) && (target != null) && (skill != null) && skill.hasDebuffEffects() && !target.isRaid() && Config.ALLOW_DEBUFF_RES_TIME)
		{
			double res = 0;
			res += calcSkillTraitVulnerability(0, target, skill, target.getChampionTemplate());
			res += calcSkillTraitProficiency(0, caster, target, skill);
			res -= calcElementMod(caster, target, skill);
			if (res != 0)
			{
				double mod = 1 + Math.abs(0.01 * res);
				if (res > 0)
				{
					mod = 1. / mod;
				}
				tickCount = (int) Math.floor(Math.max(tickCount / mod, 1));
				tickCount *= caster.getStat().calcStat(Stats.DEBUFF_RATE, 1, null, null);
			}
		}
		
		if (caster != null && skill != null && !skill.isDebuff() && tickCount > 1)
		{
			tickCount *= caster.getStat().calcStat(Stats.BUFF_RATE, 1, null, null);
			if (target != null && ((caster == target) || (caster.isPlayer() && caster.getActingPlayer().isSellingBuffs())))
			{
				tickCount *= caster.getStat().calcStat(Stats.SELF_BUFF_RATE, 1, null, null);
			}
		}
		
		if (caster != null && (caster.isDebug() || Config.DEVELOPER) && caster.isPlayer() && tickCount > 1)
		{
			final StringBuilder stat = new StringBuilder(100);
			StringUtil.append(stat, "Effect Name: ", String.valueOf(skill.getName(caster.getActingPlayer().getLang())), " Ticks: ", String.valueOf(tickCount));
			final String result = stat.toString();
			if (env.getCharacter().isDebug())
			{
				env.getCharacter().sendDebugMessage(result);
			}
			if (Config.DEVELOPER)
			{
				_log.info(result);
			}
		}
		return tickCount;
	}

	public static boolean calcProbability(double baseChance, Creature attacker, Creature target, Skill skill, boolean printChance)
	{
		if (baseChance == -1)
		{
			return true;
		}
		
		final double chance = ((((((skill.getMagicLevel() + baseChance) - target.getLevel()) + 30) - target.getINT()) * calcElemental(attacker, target, skill)) * calcValakasTrait(attacker, target, skill));
		if ((attacker.isPlayer()) && Config.SKILL_CHANCE_SHOW && printChance && attacker.getActingPlayer().isSkillChanceShow())
		{
			attacker.sendMessage(skill.getName(attacker.getActingPlayer().getLang()) + ": " + String.format("%1.2f", (chance)) + "%");
		}
		
		if ((attacker.isDebug() || Config.DEVELOPER) && attacker.isPlayer())
		{
			if (attacker.isDebug())
			{
				attacker.sendMessage(skill.getName(attacker.getActingPlayer().getLang()) + ": " + String.format("%1.2f", (chance)) + "%");
			}
		}
		return Rnd.chance(chance);
	}
	
	public static final void calcDrainDamage(Creature activeChar, Creature target, int damage, double absorbAbs, double percent)
	{
		if (!activeChar.isCanAbsorbDamage(target))
		{
			return;
		}
		int drain = 0;
		final int cp = (int) target.getCurrentCp();
		final int hp = (int) target.getCurrentHp();
		
		if (cp > 0)
		{
			if (damage < cp)
			{
				drain = 0;
			}
			else
			{
				drain = damage - cp;
			}
		}
		else if (damage > hp)
		{
			drain = hp;
		}
		else
		{
			drain = damage;
		}
		
		double hpAdd = absorbAbs + (percent * drain);
		if (hpAdd > hp)
		{
			hpAdd = hp;
		}
		
		final double finalHp = ((activeChar.getCurrentHp() + hpAdd) > activeChar.getMaxHp() ? activeChar.getMaxHp() : (activeChar.getCurrentHp() + hpAdd));
		if (!activeChar.isHealBlocked() && !target.isInvul())
		{
			activeChar.setCurrentHp(finalHp);
			final var suhp = activeChar.makeStatusUpdate(StatusUpdate.CUR_HP);
			activeChar.sendPacket(suhp);
		}
	}
}