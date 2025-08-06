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
package l2e.gameserver.model.actor.templates.npc.champion;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import l2e.gameserver.Config;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.skills.effects.AbnormalEffect;
import l2e.gameserver.model.stats.Stats;

public class ChampionTemplate
{
	public boolean lethalImmune;
	public int minChance = -1;
	public int maxChance = -1;
	public int minLevel = -1;
	public int maxLevel = -1;
	public boolean isPassive = false;
	public boolean isSocialAggro = false;
	public boolean useVitalityRate = false;
	public boolean spawnsInInstances = false;
	public String title = null;
	public float patkMultiplier = 1;
	public float matkMultiplier = 1;
	public float pdefMultiplier = 1;
	public float mdefMultiplier = 1;
	public float atkSpdMultiplier = 1;
	public float matkSpdMultiplier = 1;
	public double hpMultiplier = 1;
	public double hpRegenMultiplier = 1;
	public double expMultiplier = 1;
	public double spMultiplier = 1;
	public double itemDropMultiplier = 1;
	public double spoilDropMultiplier = 1;
	public double adenaMultiplier = 1;
	public int weaponEnchant = 0;
	public int lifeTime = 0;
	public boolean redCircle = false;
	public boolean blueCircle = false;
	public boolean switchIdList = false;
	public List<Integer> npcIdList = new ArrayList<>();
	public List<ChampionRewardItem> rewards = new ArrayList<>();
	public List<AbnormalEffect> abnormalEffect = new ArrayList<>();
	public List<Skill> skills = new ArrayList<>();
	public int maxSpoilItemsFromOneGroup = Config.MAX_SPOIL_ITEMS_FROM_ONE_GROUP;
	public int maxDropItemsFromOneGroup = Config.MAX_DROP_ITEMS_FROM_ONE_GROUP;
	public int maxRaidDropItemsFromOneGroup = Config.MAX_DROP_ITEMS_FROM_ONE_GROUP_RAIDS;
	public double adenaChanceModifier = 1;
	public double groudChanceModifier = 1;
	public double itemChanceModifier = 1;
	public Map<Stats, Double> _resists = null;
	public double rateNobleStonesMin = Config.RATE_NOBLE_STONES_COUNT_MIN;
	public double rateLifeStonesMin = Config.RATE_LIFE_STONES_COUNT_MIN;
	public double rateEnchantScrollsMin = Config.RATE_ENCHANT_SCROLLS_COUNT_MIN;
	public double rateForgottenScrollsMin = Config.RATE_FORGOTTEN_SCROLLS_COUNT_MIN;
	public double rateKeyMaterialMin = Config.RATE_KEY_MATHETIRALS_COUNT_MIN;
	public double rateRecipesMin = Config.RATE_RECEPIES_COUNT_MIN;
	public double rateBeltsMin = Config.RATE_BELTS_COUNT_MIN;
	public double rateBraceletsMin = Config.RATE_BRACELETS_COUNT_MIN;
	public double rateCloaksMin = Config.RATE_CLOAKS_COUNT_MIN;
	public double rateCodexBooksMin = Config.RATE_CODEX_BOOKS_COUNT_MIN;
	public double rateAttStonesMin = Config.RATE_ATTRIBUTE_STONES_COUNT_MIN;
	public double rateAttCrystalsMin = Config.RATE_ATTRIBUTE_CRYSTALS_COUNT_MIN;
	public double rateAttJewelsMin = Config.RATE_ATTRIBUTE_JEWELS_COUNT_MIN;
	public double rateAttEnergyMin = Config.RATE_ATTRIBUTE_ENERGY_COUNT_MIN;
	public double rateWeaponsMin = Config.RATE_WEAPONS_COUNT_MIN;
	public double rateArmorsMin = Config.RATE_ARMOR_COUNT_MIN;
	public double rateAccessoryMin = Config.RATE_ACCESSORY_COUNT_MIN;
	public double rateSealStonesMin = Config.RATE_SEAL_STONES_COUNT_MIN;
	public double rateNobleStonesMax = Config.RATE_NOBLE_STONES_COUNT_MAX;
	public double rateLifeStonesMax = Config.RATE_LIFE_STONES_COUNT_MAX;
	public double rateEnchantScrollsMax = Config.RATE_ENCHANT_SCROLLS_COUNT_MAX;
	public double rateForgottenScrollsMax = Config.RATE_FORGOTTEN_SCROLLS_COUNT_MAX;
	public double rateKeyMaterialMax = Config.RATE_KEY_MATHETIRALS_COUNT_MAX;
	public double rateRecipesMax = Config.RATE_RECEPIES_COUNT_MAX;
	public double rateBeltsMax = Config.RATE_BELTS_COUNT_MAX;
	public double rateBraceletsMax = Config.RATE_BRACELETS_COUNT_MAX;
	public double rateCloaksMax = Config.RATE_CLOAKS_COUNT_MAX;
	public double rateCodexBooksMax = Config.RATE_CODEX_BOOKS_COUNT_MAX;
	public double rateAttStonesMax = Config.RATE_ATTRIBUTE_STONES_COUNT_MAX;
	public double rateAttCrystalsMax = Config.RATE_ATTRIBUTE_CRYSTALS_COUNT_MAX;
	public double rateAttJewelsMax = Config.RATE_ATTRIBUTE_JEWELS_COUNT_MAX;
	public double rateAttEnergyMax = Config.RATE_ATTRIBUTE_ENERGY_COUNT_MAX;
	public double rateWeaponsMax = Config.RATE_WEAPONS_COUNT_MAX;
	public double rateArmorsMax = Config.RATE_ARMOR_COUNT_MAX;
	public double rateAccessoryMax = Config.RATE_ACCESSORY_COUNT_MAX;
	public double rateSealStonesMax = Config.RATE_SEAL_STONES_COUNT_MAX;
	
	public double getResistValue(Stats type)
	{
		if (_resists == null || _resists.isEmpty())
		{
			return 0;
		}
		return _resists.get(type) != null ? _resists.get(type) : 0;
	}
}
