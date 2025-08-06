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
package l2e.gameserver.model.reward;

import l2e.gameserver.Config;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.items.Item;
import l2e.gameserver.model.actor.templates.npc.champion.ChampionTemplate;

public class RewardItemRates
{
	public static double getMinCountModifier(Player player, ChampionTemplate championTemplate, Item item, boolean allowModifier)
	{
		double modifier = 1.;
		if (!allowModifier)
		{
			return modifier;
		}
		
		if (item.isNobleStone())
		{
			modifier *= player.hasPremiumBonus() ? player.getPremiumBonus().getMinNobleStonesCount() : Config.RATE_NOBLE_STONES_COUNT_MIN;
			if (championTemplate != null)
			{
				modifier *= championTemplate.rateNobleStonesMin;
			}
		}
		
		if(item.isLifeStone())
		{
			modifier *= player.hasPremiumBonus() ? player.getPremiumBonus().getMinLifeStonesCount() : Config.RATE_LIFE_STONES_COUNT_MIN;
			if (championTemplate != null)
			{
				modifier *= championTemplate.rateLifeStonesMin;
			}
		}

		if(item.isEnchantScroll())
		{
			modifier *= player.hasPremiumBonus() ? player.getPremiumBonus().getMinEnchantScrollsCount() : Config.RATE_ENCHANT_SCROLLS_COUNT_MIN;
			if (championTemplate != null)
			{
				modifier *= championTemplate.rateEnchantScrollsMin;
			}
		}

		if(item.isForgottenScroll())
		{
			modifier *= player.hasPremiumBonus() ? player.getPremiumBonus().getMinForgottenScrollsCount() : Config.RATE_FORGOTTEN_SCROLLS_COUNT_MIN;
			if (championTemplate != null)
			{
				modifier *= championTemplate.rateForgottenScrollsMin;
			}
		}

		if(item.isKeyMatherial())
		{
			modifier *= player.hasPremiumBonus() ? player.getPremiumBonus().getMinMaterialsCount() : Config.RATE_KEY_MATHETIRALS_COUNT_MIN;
			if (championTemplate != null)
			{
				modifier *= championTemplate.rateKeyMaterialMin;
			}
		}

		if(item.isRecipe())
		{
			modifier *= player.hasPremiumBonus() ? player.getPremiumBonus().getMinRepicesCount() : Config.RATE_RECEPIES_COUNT_MIN;
			if (championTemplate != null)
			{
				modifier *= championTemplate.rateRecipesMin;
			}
		}

		if(item.isBelt())
		{
			modifier *= player.hasPremiumBonus() ? player.getPremiumBonus().getMinBeltsCount() : Config.RATE_BELTS_COUNT_MIN;
			if (championTemplate != null)
			{
				modifier *= championTemplate.rateBeltsMin;
			}
		}

		if(item.isBracelet())
		{
			modifier *= player.hasPremiumBonus() ? player.getPremiumBonus().getMinBraceletsCount() : Config.RATE_BRACELETS_COUNT_MIN;
			if (championTemplate != null)
			{
				modifier *= championTemplate.rateBraceletsMin;
			}
		}

		if(item.isCloak())
		{
			modifier *= player.hasPremiumBonus() ? player.getPremiumBonus().getMinCloaksCount() : Config.RATE_CLOAKS_COUNT_MIN;
			if (championTemplate != null)
			{
				modifier *= championTemplate.rateCloaksMin;
			}
		}
	
		if(item.isCodexBook())
		{
			modifier *= player.hasPremiumBonus() ? player.getPremiumBonus().getMinCodexCount() : Config.RATE_CODEX_BOOKS_COUNT_MIN;
			if (championTemplate != null)
			{
				modifier *= championTemplate.rateCodexBooksMin;
			}
		}
	
		if(item.isAttributeStone())
		{
			modifier *= player.hasPremiumBonus() ? player.getPremiumBonus().getMinAttStonesCount() : Config.RATE_ATTRIBUTE_STONES_COUNT_MIN;
			if (championTemplate != null)
			{
				modifier *= championTemplate.rateAttStonesMin;
			}
		}
			
		if(item.isAttributeCrystal())
		{
			modifier *= player.hasPremiumBonus() ? player.getPremiumBonus().getMinAttCrystalsCount() : Config.RATE_ATTRIBUTE_CRYSTALS_COUNT_MIN;
			if (championTemplate != null)
			{
				modifier *= championTemplate.rateAttCrystalsMin;
			}
		}
	
		if(item.isAttributeJewel())
		{
			modifier *= player.hasPremiumBonus() ? player.getPremiumBonus().getMinAttJewelsCount() : Config.RATE_ATTRIBUTE_JEWELS_COUNT_MIN;
			if (championTemplate != null)
			{
				modifier *= championTemplate.rateAttJewelsMin;
			}
		}
	
		if(item.isAttributeEnergy())
		{
			modifier *= player.hasPremiumBonus() ? player.getPremiumBonus().getMinAttEnergyCount() : Config.RATE_ATTRIBUTE_ENERGY_COUNT_MIN;
			if (championTemplate != null)
			{
				modifier *= championTemplate.rateAttEnergyMin;
			}
		}
	
		if(item.isWeapon())
		{
			modifier *= player.hasPremiumBonus() ? player.getPremiumBonus().getMinWeaponsCount() : Config.RATE_WEAPONS_COUNT_MIN;
			if (championTemplate != null)
			{
				modifier *= championTemplate.rateWeaponsMin;
			}
		}
	
		if(item.isArmor())
		{
			modifier *= player.hasPremiumBonus() ? player.getPremiumBonus().getMinArmorsCount() : Config.RATE_ARMOR_COUNT_MIN;
			if (championTemplate != null)
			{
				modifier *= championTemplate.rateArmorsMin;
			}
		}
	
		if(item.isAccessory())
		{
			modifier *= player.hasPremiumBonus() ? player.getPremiumBonus().getMinAccessoryesCount() : Config.RATE_ACCESSORY_COUNT_MIN;
			if (championTemplate != null)
			{
				modifier *= championTemplate.rateAccessoryMin;
			}
		}
		
		if (item.isSealStone())
		{
			modifier *= player.hasPremiumBonus() ? player.getPremiumBonus().getMinSealStonesCount() : Config.RATE_SEAL_STONES_COUNT_MIN;
			if (championTemplate != null)
			{
				modifier *= championTemplate.rateSealStonesMin;
			}
			if (player != null)
			{
				modifier *= player.isInParty() && Config.PREMIUM_PARTY_RATE ? player.getParty().getDropSealStones() : player.getPremiumBonus().getDropSealStones();
			}
		}
		return modifier;
	}

	public static double getMaxCountModifier(Player player, ChampionTemplate championTemplate, Item item, boolean allowModifier)
	{
		double modifier = 1.;
		if (!allowModifier)
		{
			return modifier;
		}
		
		if (item.isNobleStone())
		{
			modifier *= player.hasPremiumBonus() ? player.getPremiumBonus().getMaxNobleStonesCount() : Config.RATE_NOBLE_STONES_COUNT_MAX;
			if (championTemplate != null)
			{
				modifier *= championTemplate.rateNobleStonesMax;
			}
		}
		
		if(item.isLifeStone())
		{
			modifier *= player.hasPremiumBonus() ? player.getPremiumBonus().getMaxLifeStonesCount() : Config.RATE_LIFE_STONES_COUNT_MAX;
			if (championTemplate != null)
			{
				modifier *= championTemplate.rateLifeStonesMax;
			}
		}

		if(item.isEnchantScroll())
		{
			modifier *= player.hasPremiumBonus() ? player.getPremiumBonus().getMaxEnchantScrollsCount() : Config.RATE_ENCHANT_SCROLLS_COUNT_MAX;
			if (championTemplate != null)
			{
				modifier *= championTemplate.rateEnchantScrollsMax;
			}
		}
		
		if (item.isForgottenScroll())
		{
			modifier *= player.hasPremiumBonus() ? player.getPremiumBonus().getMaxForgottenScrollsCount() : Config.RATE_FORGOTTEN_SCROLLS_COUNT_MAX;
			if (championTemplate != null)
			{
				modifier *= championTemplate.rateForgottenScrollsMax;
			}
		}

		if(item.isKeyMatherial())
		{
			modifier *= player.hasPremiumBonus() ? player.getPremiumBonus().getMaxMaterialsCount() : Config.RATE_KEY_MATHETIRALS_COUNT_MAX;
			if (championTemplate != null)
			{
				modifier *= championTemplate.rateKeyMaterialMax;
			}
		}

		if(item.isRecipe())
		{
			modifier *= player.hasPremiumBonus() ? player.getPremiumBonus().getMaxRepicesCount() : Config.RATE_RECEPIES_COUNT_MAX;
			if (championTemplate != null)
			{
				modifier *= championTemplate.rateRecipesMax;
			}
		}
		
		if (item.isBelt())
		{
			modifier *= player.hasPremiumBonus() ? player.getPremiumBonus().getMaxBeltsCount() : Config.RATE_BELTS_COUNT_MAX;
			if (championTemplate != null)
			{
				modifier *= championTemplate.rateBeltsMax;
			}
		}
		
		if (item.isBracelet())
		{
			modifier *= player.hasPremiumBonus() ? player.getPremiumBonus().getMaxBraceletsCount() : Config.RATE_BRACELETS_COUNT_MAX;
			if (championTemplate != null)
			{
				modifier *= championTemplate.rateBraceletsMax;
			}
		}

		if (item.isCloak())
		{
			modifier *= player.hasPremiumBonus() ? player.getPremiumBonus().getMaxCloaksCount() : Config.RATE_CLOAKS_COUNT_MAX;
			if (championTemplate != null)
			{
				modifier *= championTemplate.rateCloaksMax;
			}
		}
		
		if(item.isCodexBook())
		{
			modifier *= player.hasPremiumBonus() ? player.getPremiumBonus().getMaxCodexCount() : Config.RATE_CODEX_BOOKS_COUNT_MAX;
			if (championTemplate != null)
			{
				modifier *= championTemplate.rateCodexBooksMax;
			}
		}
	
		if(item.isAttributeStone())
		{
			modifier *= player.hasPremiumBonus() ? player.getPremiumBonus().getMaxAttStonesCount() : Config.RATE_ATTRIBUTE_STONES_COUNT_MAX;
			if (championTemplate != null)
			{
				modifier *= championTemplate.rateAttStonesMax;
			}
			if (player != null)
			{
				modifier *= player.isInParty() && Config.PREMIUM_PARTY_RATE ? player.getParty().getDropElementStones() : player.getPremiumBonus().getDropElementStones();
			}
		}
		
		if (item.isAttributeCrystal())
		{
			modifier *= player.hasPremiumBonus() ? player.getPremiumBonus().getMaxAttCrystalsCount() : Config.RATE_ATTRIBUTE_CRYSTALS_COUNT_MAX;
			if (championTemplate != null)
			{
				modifier *= championTemplate.rateAttCrystalsMax;
			}
			if (player != null)
			{
				modifier *= player.isInParty() && Config.PREMIUM_PARTY_RATE ? player.getParty().getDropElementStones() : player.getPremiumBonus().getDropElementStones();
			}
		}
		
		if (item.isAttributeJewel())
		{
			modifier *= player.hasPremiumBonus() ? player.getPremiumBonus().getMaxAttJewelsCount() : Config.RATE_ATTRIBUTE_JEWELS_COUNT_MAX;
			if (championTemplate != null)
			{
				modifier *= championTemplate.rateAttJewelsMax;
			}
		}
		
		if (item.isAttributeEnergy())
		{
			modifier *= player.hasPremiumBonus() ? player.getPremiumBonus().getMaxAttEnergyCount() : Config.RATE_ATTRIBUTE_ENERGY_COUNT_MAX;
			if (championTemplate != null)
			{
				modifier *= championTemplate.rateAttEnergyMax;
			}
		}
		
		if (item.isWeapon())
		{
			modifier *= player.hasPremiumBonus() ? player.getPremiumBonus().getMaxWeaponsCount() : Config.RATE_WEAPONS_COUNT_MAX;
			if (championTemplate != null)
			{
				modifier *= championTemplate.rateWeaponsMax;
			}
		}
		
		if (item.isArmor())
		{
			modifier *= player.hasPremiumBonus() ? player.getPremiumBonus().getMaxArmorsCount() : Config.RATE_ARMOR_COUNT_MAX;
			if (championTemplate != null)
			{
				modifier *= championTemplate.rateArmorsMax;
			}
		}
	
		if (item.isAccessory())
		{
			modifier *= player.hasPremiumBonus() ? player.getPremiumBonus().getMaxAccessoryesCount() : Config.RATE_ACCESSORY_COUNT_MAX;
			if (championTemplate != null)
			{
				modifier *= championTemplate.rateAccessoryMax;
			}
		}
		
		if (item.isSealStone())
		{
			modifier *= player.hasPremiumBonus() ? player.getPremiumBonus().getMaxSealStonesCount() : Config.RATE_SEAL_STONES_COUNT_MAX;
			if (championTemplate != null)
			{
				modifier *= championTemplate.rateSealStonesMax;
			}
			if (player != null)
			{
				modifier *= player.isInParty() && Config.PREMIUM_PARTY_RATE ? player.getParty().getDropSealStones() : player.getPremiumBonus().getDropSealStones();
			}
		}
		return modifier;
	}
}
