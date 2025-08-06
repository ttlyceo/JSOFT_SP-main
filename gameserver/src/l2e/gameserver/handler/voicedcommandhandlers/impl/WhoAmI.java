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
package l2e.gameserver.handler.voicedcommandhandlers.impl;

import java.text.NumberFormat;
import java.util.Locale;

import l2e.commons.apache.text.StrBuilder;
import l2e.commons.util.Strings;
import l2e.gameserver.Config;
import l2e.gameserver.data.htm.HtmCache;
import l2e.gameserver.handler.voicedcommandhandlers.IVoicedCommandHandler;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.stats.Formulas;
import l2e.gameserver.model.stats.Stats;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;

public class WhoAmI implements IVoicedCommandHandler
{
	private final String[] _commandList =
	{
	        "stats"
	};
	
	@Override
	public boolean useVoicedCommand(String command, Player player, String targets)
	{
		if (!Config.ALLOW_STATS_COMMAND)
		{
			return false;
		}
		
		final Player playerToShow = player.isGM() && player.getTarget() != null && player.getTarget().isPlayer() ? player.getTarget().getActingPlayer() : player;

		final double hpRegen = Formulas.calcHpRegen(playerToShow);
		final double cpRegen = Formulas.calcCpRegen(playerToShow);
		final double mpRegen = Formulas.calcMpRegen(playerToShow);
		final double hpDrain = playerToShow.calcStat(Stats.ABSORB_DAMAGE_PERCENT, 0, null, null);
		final double hpGain = playerToShow.calcStat(Stats.HEAL_EFFECT, 100, null, null);
		final double mpGain = playerToShow.calcStat(Stats.MANA_CHARGE, 100, null, null);
		final double critPerc = playerToShow.getCriticalDmg(null, 1, null);
		final double critProc = 100 * critPerc;
		final int critStatic = (int) playerToShow.calcStat(Stats.CRITICAL_DAMAGE_ADD, 0, null, null);
		final double mCritRate = playerToShow.getMCriticalHit(null, null) * 0.1;
		final double blowRate = playerToShow.calcStat(Stats.BLOW_RATE, 0, null, null);

		final ItemInstance shld = playerToShow.getSecondaryWeaponInstance();
		final boolean shield = shld != null && shld.getItem().isShield();

		final double shieldDef = shield ? playerToShow.calcStat(Stats.SHIELD_DEFENCE, player.getTemplate().getBaseShldDef(), null, null) : 0;
		final double shieldRate = shield ? playerToShow.calcStat(Stats.SHIELD_RATE, 0, null, null) : 0;

		final double xpRate = Config.RATE_XP_BY_LVL[playerToShow.getLevel()] * playerToShow.getPremiumBonus().getRateXp();
		final double spRate = Config.RATE_SP_BY_LVL[playerToShow.getLevel()] * playerToShow.getPremiumBonus().getRateSp();
		final double dropRate = Config.RATE_DROP_ITEMS * playerToShow.getPremiumBonus().getDropItems();
		final double spoilRate = Config.RATE_DROP_SPOIL * playerToShow.getPremiumBonus().getDropSpoil();
		final double fireResist = playerToShow.calcStat(Stats.FIRE_RES, 0, null, null);
		final double windResist = playerToShow.calcStat(Stats.WIND_RES, 0, null, null);
		final double waterResist = playerToShow.calcStat(Stats.WATER_RES, 0, null, null);
		final double earthResist = playerToShow.calcStat(Stats.EARTH_RES, 0, null, null);
		final double holyResist = playerToShow.calcStat(Stats.HOLY_RES, 0, null, null);
		final double unholyResist = playerToShow.calcStat(Stats.DARK_RES, 0, null, null);

		final double bleedPower = Math.min(playerToShow.calcStat(Stats.BLEED_PROF, 0, null, null), Config.BLEED_PROF);
		final double bleedResist = calcMin(playerToShow.calcStat(Stats.BLEED_VULN, 0, null, null), Config.BLEED_VULN);
		final double poisonPower = Math.min(playerToShow.calcStat(Stats.POISON_PROF, 0, null, null), Config.POISON_PROF);
		final double poisonResist = calcMin(playerToShow.calcStat(Stats.POISON_VULN, 0, null, null), Config.POISON_VULN);
		final double stunPower = Math.min(playerToShow.calcStat(Stats.STUN_PROF, 0, null, null), Config.STUN_PROF);
		final double stunResist = calcMin(playerToShow.calcStat(Stats.STUN_VULN, 0, null, null), Config.STUN_VULN);
		final double rootPower = Math.min(playerToShow.calcStat(Stats.ROOT_PROF, 0, null, null), Config.ROOT_PROF);
		final double rootResist = calcMin(playerToShow.calcStat(Stats.ROOT_VULN, 0, null, null), Config.ROOT_VULN);
		final double sleepPower = Math.min(playerToShow.calcStat(Stats.SLEEP_PROF, 0, null, null), Config.SLEEP_PROF);
		final double sleepResist = calcMin(playerToShow.calcStat(Stats.SLEEP_VULN, 0, null, null), Config.SLEEP_VULN);
		final double paralyzePower = Math.min(playerToShow.calcStat(Stats.PARALYZE_PROF, 0, null, null), Config.PARALYZE_PROF);
		final double paralyzeResist = calcMin(playerToShow.calcStat(Stats.PARALYZE_VULN, 0, null, null), Config.PARALYZE_VULN);
		final double mentalPower = Math.min(playerToShow.calcStat(Stats.DERANGEMENT_PROF, 0, null, null), Config.MENTAL_PROF);
		final double mentalResist = calcMin(playerToShow.calcStat(Stats.DERANGEMENT_VULN, 0, null, null), Config.MENTAL_VULN);
		final double debuffPower = Math.min(playerToShow.calcStat(Stats.DEBUFF_PROF, 0, null, null), Config.DEBUFF_PROF);
		final double debuffResist = calcMin(playerToShow.calcStat(Stats.DEBUFF_VULN, 0, null, null), Config.DEBUFF_VULN);
		final double cancelPower = Math.min(playerToShow.calcStat(Stats.CANCEL_PROF, 0, null, null), Config.CANCEL_PROF);
		final double cancelResist = calcMin(playerToShow.calcStat(Stats.CANCEL_VULN, 0, null, null), Config.CANCEL_VULN);
		
		final double swordResist = 100. - playerToShow.calcStat(Stats.SWORD_WPN_VULN, 0, null, null);
		final double dualResist = 100. - playerToShow.calcStat(Stats.DUAL_WPN_VULN, 0, null, null);
		final double bluntResist = 100. - playerToShow.calcStat(Stats.BLUNT_WPN_VULN, 0, null, null);
		final double daggerResist = 100. - playerToShow.calcStat(Stats.DAGGER_WPN_VULN, 0, null, null);
		final double bowResist = 100. - playerToShow.calcStat(Stats.BOW_WPN_VULN, 0, null, null);
		final double crossbowResist = 100. - playerToShow.calcStat(Stats.CROSSBOW_WPN_VULN, 0, null, null);
		final double poleResist = 100. - playerToShow.calcStat(Stats.POLE_WPN_VULN, 0, null, null);
		final double fistResist = 100. - playerToShow.calcStat(Stats.FIST_WPN_VULN, 0, null, null);
		final double dualFistResist = 100. - playerToShow.calcStat(Stats.DUALFIST_WPN_VULN, 0, null, null);
		final double bigSwordResist = 100. - playerToShow.calcStat(Stats.BIGSWORD_WPN_VULN, 0, null, null);
		final double bigBluntResist = 100. - playerToShow.calcStat(Stats.BIGBLUNT_WPN_VULN, 0, null, null);
		final double dualDaggerResist = 100. - playerToShow.calcStat(Stats.DUALDAGGER_WPN_VULN, 0, null, null);
		final double rapierResist = 100. - playerToShow.calcStat(Stats.RAPIER_WPN_VULN, 0, null, null);
		final double ancientResist = 100. - playerToShow.calcStat(Stats.ANCIENT_WPN_VULN, 0, null, null);
		
		final double critChanceResist = playerToShow.calcStat(Stats.CRITICAL_RATE, playerToShow.getTemplate().getBaseCritRate(), null, null) * 0.01;
		final double critDamResist = 100 * playerToShow.calcStat(Stats.CRIT_VULN, 1, null, null) - 100;

		final double pReuse = 100 - 100 * (playerToShow.calcStat(Stats.P_REUSE, 1, null, null));
		final double mReuse = 100 - 100 * (playerToShow.calcStat(Stats.MAGIC_REUSE_RATE, 1, null, null));
		final double mCritPower = 100 * (playerToShow.calcStat(Stats.MAGIC_CRIT_DMG, 1, null, null)) - 100;
		final double pvpPhysDmg = 100 * (playerToShow.calcStat(Stats.PVP_PHYSICAL_DMG, 1, null, null)) - 100;
		final double pvpMagicalDmg = 100 * (playerToShow.calcStat(Stats.PVP_MAGICAL_DMG, 1, null, null)) - 100;
		final double pvpPhysDef = 100 * (playerToShow.calcStat(Stats.PVP_PHYSICAL_DEF, 1, null, null)) - 100;
		final double pvpMagicalDef = 100 * (playerToShow.calcStat(Stats.PVP_MAGICAL_DEF, 1, null, null)) - 100;
		final double pvePhysDmg = 100 * (playerToShow.calcStat(Stats.PVE_PHYSICAL_DMG, 1, null, null)) - 100;
		final double pvePhysSkillsDmg = 100 * (playerToShow.calcStat(Stats.PVE_PHYS_SKILL_DMG, 1, null, null)) - 100;
		final double pvePhysSkillsDef = 100 * (playerToShow.calcStat(Stats.PVE_PHYS_SKILL_DEF, 1, null, null)) - 100;
		final double pveMagicalDmg = 100 * (playerToShow.calcStat(Stats.PVE_MAGICAL_DMG, 1, null, null)) - 100;
		final double pvePhysDef = 100 * (playerToShow.calcStat(Stats.PVE_PHYSICAL_DEF, 1, null, null)) - 100;
		final double pveMagicalDef = 100 * (playerToShow.calcStat(Stats.PVE_MAGICAL_DEF, 1, null, null)) - 100;
		final double critDamEvas = 100 * (playerToShow.calcStat(Stats.CRIT_DAMAGE_EVASION, 1, null, null)) - 100;
		final int pAtkRange = playerToShow.getPhysicalAttackRange();
		final int atkCountMax = (int) playerToShow.calcStat(Stats.ATTACK_COUNT_MAX, 1, null, null);
		final double reflectMagicSkillDam = 100 * (playerToShow.calcStat(Stats.REFLECT_MAGIC_SKILLS_PERCENT, 1, null, null)) - 100;
		final double reflectPhysSkillDam = 100 * (playerToShow.calcStat(Stats.REFLECT_PHYS_SKILLS_PERCENT, 1, null, null)) - 100;
		final double reflectSkillMagic = 100 * (playerToShow.calcStat(Stats.REFLECT_SKILL_MAGIC, 1, null, null)) - 100;
		final double reflectSkillPhysic = 100 * (playerToShow.calcStat(Stats.REFLECT_SKILL_PHYSIC, 1, null, null)) - 100;
		final double reflectDam = playerToShow.calcStat(Stats.REFLECT_DAMAGE_PERCENT, 0, null, null);
		final double adenaMultiplier = 100 * (playerToShow.calcStat(Stats.ADENA_MULTIPLIER, 1, null, null) * playerToShow.getPremiumBonus().getDropAdena()) - 100;
		final double spoilMultiplier = 100 * (playerToShow.calcStat(Stats.SPOIL_MULTIPLIER, 1, null, null) * playerToShow.getPremiumBonus().getDropSpoil()) - 100;
		final double dropMultiplier = 100 * (playerToShow.calcStat(Stats.REWARD_MULTIPLIER, 1, null, null) * playerToShow.getPremiumBonus().getDropItems()) - 100;
		final double raidDropMultiplier = 100 * (playerToShow.calcStat(Stats.RAID_REWARD_MULTIPLIER, 1, null, null) * playerToShow.getPremiumBonus().getDropRaids()) - 100;
		final int enchantBonus = (int) playerToShow.calcStat(Stats.ENCHANT_BONUS, 0, null, null) + playerToShow.getPremiumBonus().getEnchantChance();
		final double selfBuffRate = 100 * (playerToShow.calcStat(Stats.SELF_BUFF_RATE, 1, null, null)) - 100;
		final double expBonus = (100 * playerToShow.calcStat(Stats.RUNE_OF_EXP, 1, null, null) * playerToShow.calcStat(Stats.BONUS_EXP, 1, null, null) * playerToShow.getPremiumBonus().getRateXp()) - 100;
		final double bonusSp = (100 * playerToShow.calcStat(Stats.RUNE_OF_SP, 1, null, null) * playerToShow.calcStat(Stats.BONUS_SP, 1, null, null) * playerToShow.getPremiumBonus().getRateSp()) - 100;
		
		final String dialog = HtmCache.getInstance().getHtm(player, player.getLang(), player.isGM() ? "data/html/mods/whoamiGM.htm" : "data/html/mods/whoami.htm");

		final NumberFormat df = NumberFormat.getInstance(Locale.ENGLISH);
		df.setMaximumFractionDigits(1);
		df.setMinimumFractionDigits(1);

		final StrBuilder sb = new StrBuilder(dialog);
		sb.replaceAll("%hpRegen%", df.format(hpRegen));
		sb.replaceAll("%cpRegen%", df.format(cpRegen));
		sb.replaceAll("%mpRegen%", df.format(mpRegen));
		sb.replaceAll("%hpDrain%", df.format(hpDrain));
		sb.replaceAll("%hpGain%", df.format(hpGain));
		sb.replaceAll("%mpGain%", df.format(mpGain));
		sb.replaceAll("%critPerc%", critPerc > 1 ? "" + df.format(critPerc) + "%" : "");
		sb.replaceAll("%critProc%", df.format(critProc));
		sb.replaceAll("%critStatic%", critStatic > 0 ? "+ " + critStatic + "" : critPerc < 1 ? "-" : "");
		sb.replaceAll("%mCritRate%", df.format(mCritRate));
		sb.replaceAll("%blowRate%", df.format(blowRate));
		sb.replaceAll("%shieldDef%", df.format(shieldDef));
		sb.replaceAll("%shieldRate%", df.format(shieldRate));
		sb.replaceAll("%xpRate%", df.format(xpRate));
		sb.replaceAll("%spRate%", df.format(spRate));
		sb.replaceAll("%dropRate%", df.format(dropRate));
		sb.replaceAll("%spoilRate%", df.format(spoilRate));
		sb.replaceAll("%fireResist%", df.format(fireResist));
		sb.replaceAll("%windResist%", df.format(windResist));
		sb.replaceAll("%waterResist%", df.format(waterResist));
		sb.replaceAll("%earthResist%", df.format(earthResist));
		sb.replaceAll("%holyResist%", df.format(holyResist));
		sb.replaceAll("%darkResist%", df.format(unholyResist));
		sb.replaceAll("%bleedPower%", df.format(bleedPower));
		sb.replaceAll("%bleedResist%", df.format(Math.abs(bleedResist)));
		sb.replaceAll("%poisonPower%", df.format(poisonPower));
		sb.replaceAll("%poisonResist%", df.format(Math.abs(poisonResist)));
		sb.replaceAll("%stunPower%", df.format(stunPower));
		sb.replaceAll("%stunResist%", df.format(Math.abs(stunResist)));
		sb.replaceAll("%rootPower%", df.format(rootPower));
		sb.replaceAll("%rootResist%", df.format(Math.abs(rootResist)));
		sb.replaceAll("%sleepPower%", df.format(sleepPower));
		sb.replaceAll("%sleepResist%", df.format(Math.abs(sleepResist)));
		sb.replaceAll("%paralyzePower%", df.format(paralyzePower));
		sb.replaceAll("%paralyzeResist%", df.format(Math.abs(paralyzeResist)));
		sb.replaceAll("%mentalPower%", df.format(mentalPower));
		sb.replaceAll("%mentalResist%", df.format(Math.abs(mentalResist)));
		sb.replaceAll("%debuffPower%", df.format(debuffPower));
		sb.replaceAll("%debuffResist%", df.format(Math.abs(debuffResist)));
		sb.replaceAll("%cancelPower%", df.format(cancelPower));
		sb.replaceAll("%cancelResist%", df.format(Math.abs(cancelResist)));
		sb.replaceAll("%swordResist%", df.format(swordResist));
		sb.replaceAll("%dualResist%", df.format(dualResist));
		sb.replaceAll("%bluntResist%", df.format(bluntResist));
		sb.replaceAll("%daggerResist%", df.format(daggerResist));
		sb.replaceAll("%bowResist%", df.format(bowResist));
		sb.replaceAll("%crossbowResist%", df.format(crossbowResist));
		sb.replaceAll("%fistResist%", df.format(fistResist));
		sb.replaceAll("%poleResist%", df.format(poleResist));
		sb.replaceAll("%dualFistResist%", df.format(dualFistResist));
		sb.replaceAll("%bigSwordResist%", df.format(bigSwordResist));
		sb.replaceAll("%bigBluntResist%", df.format(bigBluntResist));
		sb.replaceAll("%dualDaggerResist%", df.format(dualDaggerResist));
		sb.replaceAll("%rapierResist%", df.format(rapierResist));
		sb.replaceAll("%ancientResist%", df.format(ancientResist));
		sb.replaceAll("%critChanceResist%", df.format(Math.abs(critChanceResist)));
		sb.replaceAll("%critDamResist%", critDamResist < 1 ? "" + df.format(Math.abs(critDamResist)) + "" : "0");
		sb.replaceAll("%pReuse%", df.format(pReuse));
		sb.replaceAll("%mReuse%", df.format(mReuse));
		sb.replaceAll("%mCritPower%", df.format(mCritPower));
		sb.replaceAll("%pvpPhysDmg%", df.format(pvpPhysDmg));
		sb.replaceAll("%pvpMagicalDmg%", df.format(pvpMagicalDmg));
		sb.replaceAll("%pvpPhysDef%", df.format(pvpPhysDef));
		sb.replaceAll("%pvpMagicalDef%", df.format(pvpMagicalDef));
		sb.replaceAll("%pvePhysSkillsDmg%", df.format(pvePhysSkillsDmg));
		sb.replaceAll("%pvePhysSkillsDef%", df.format(pvePhysSkillsDef));
		sb.replaceAll("%pvePhysDmg%", df.format(pvePhysDmg));
		sb.replaceAll("%pveMagicalDmg%", df.format(pveMagicalDmg));
		sb.replaceAll("%pvePhysDef%", df.format(pvePhysDef));
		sb.replaceAll("%pveMagicalDef%", df.format(pveMagicalDef));
		sb.replaceAll("%critDamEvas%", df.format(critDamEvas));
		sb.replaceAll("%pAtkRange%", String.valueOf(pAtkRange));
		sb.replaceAll("%atkCountMax%", String.valueOf(atkCountMax));
		sb.replaceAll("%reflectMagicSkillDam%", df.format(reflectMagicSkillDam));
		sb.replaceAll("%reflectPhysSkillDam%", df.format(reflectPhysSkillDam));
		sb.replaceAll("%reflectSkillMagic%", df.format(reflectSkillMagic));
		sb.replaceAll("%reflectSkillPhysic%", df.format(reflectSkillPhysic));
		sb.replaceAll("%reflectDam%", df.format(reflectDam));
		sb.replaceAll("%adenaMultiplier%", df.format(adenaMultiplier));
		sb.replaceAll("%spoilMultiplier%", df.format(spoilMultiplier));
		sb.replaceAll("%dropMultiplier%", df.format(dropMultiplier));
		sb.replaceAll("%raidDropMultiplier%", df.format(raidDropMultiplier));
		sb.replaceAll("%enchantBonus%", String.valueOf(enchantBonus));
		sb.replaceAll("%selfBuffRate%", df.format(selfBuffRate));
		sb.replaceAll("%expBonus%", df.format(expBonus));
		sb.replaceAll("%bonusSp%", df.format(bonusSp));

		final NpcHtmlMessage msg = new NpcHtmlMessage(0);
		msg.setHtml(player, Strings.bbParse(sb.toString()));
		player.sendPacket(msg);

		return true;
	}
	
	private static double calcMin(double a, double b)
	{
		if (a < b)
		{
			return b;
		}
		return a;
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return _commandList;
	}
}
