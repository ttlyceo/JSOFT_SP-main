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
package l2e.gameserver.model.service.premium;

public enum PremiumRates
{
	EXP,
	SP,
	SIEGE,
	ADENA,
	DROP,
	ELEMENT_STONES,
	SPOIL,
	CRAFT,
	MASTERWORK_CRAFT,
	WEIGHT_LIMIT,
	QUEST_REWARD,
	QUEST_DROP, EVENT, FISHING, DROP_RAID, ENCHANT, FAME, REFLECTION_REDUCE, SEAL_STONES, DROP_EPIC, MODIFIER_SEAL_STONES, MODIFIER_LIFE_STONES, MODIFIER_ENCHANT_SCROLLS, MODIFIER_FORGOTTEN_SCROLLS, MODIFIER_MATERIALS, MODIFIER_RECIPES, MODIFIER_BELTS, MODIFIER_BRACELETS, MODIFIER_CLOAKS, MODIFIER_CODEX, MODIFIER_ATT_STONES, MODIFIER_ATT_CRYSTALS, MODIFIER_ATT_JEWELS, MODIFIER_ATT_ENERGY, MODIFIER_ARMORS, MODIFIER_WEAPONS, MODIFIER_ACCESSORYES, MAX_SPOIL_PER_ONE_GROUP, MAX_DROP_PER_ONE_GROUP, MAX_DROP_RAID_PER_ONE_GROUP, MODIFIER_NOBLE_STONES, GROUP_RATE;
	
	private static final PremiumRates[] VALUES = values();
	
	private PremiumRates()
	{
	}
	
	public static PremiumRates find(String name)
	{
		for (final PremiumRates key : VALUES)
		{
			if (key.name().equalsIgnoreCase(name))
			{
				return key;
			}
		}
		return null;
	}
}