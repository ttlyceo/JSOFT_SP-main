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
package l2e.gameserver.model.items.enchant;

import l2e.commons.util.Rnd;
import l2e.gameserver.Config;
import l2e.gameserver.data.parser.EnchantItemGroupsParser;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.stats.Stats;
import l2e.gameserver.model.stats.StatsSet;

public final class EnchantScroll extends EnchantItem
{
	private final boolean _isBlessed;
	private final boolean _isSafe;
	private final int _scrollGroupId;
	private final int _increaseEnchantBy;
	
	public EnchantScroll(StatsSet set)
	{
		super(set);

		_isBlessed = set.getBool("isBlessed", false);
		_isSafe = set.getBool("isSafe", false);
		_scrollGroupId = set.getInteger("scrollGroupId", 0);
		_increaseEnchantBy = set.getInteger("increaseEnchantBy", 1);
	}
	
	public boolean isBlessed()
	{
		return _isBlessed;
	}

	public boolean isSafe()
	{
		return _isSafe;
	}

	public int getScrollGroupId()
	{
		return _scrollGroupId;
	}

	public boolean isValid(ItemInstance enchantItem, EnchantItem supportItem)
	{
		if ((supportItem != null) && (!supportItem.isValid(enchantItem) || isBlessed()))
		{
			return false;
		}

		return super.isValid(enchantItem);
	}
	
	public double getChance(Player player, ItemInstance enchantItem)
	{
		if (EnchantItemGroupsParser.getInstance().getScrollGroup(_scrollGroupId) == null)
		{
			_log.warn(getClass().getSimpleName() + ": Unexistent enchant scroll group specified for enchant scroll: " + getId());
			return -1.0;
		}
		final EnchantItemGroup group = EnchantItemGroupsParser.getInstance().getItemGroup(enchantItem.getItem(), _scrollGroupId);
		if (group == null)
		{
			_log.warn(getClass().getSimpleName() + ": Couldn't find enchant item group for scroll: " + getId() + " requested by: " + player);
			return -1.0;
		}
		
		double finalChance;
		final double chance = group.getChance(enchantItem.getEnchantLevel());
		if (chance <= 0.0)
		{
			return -1.0;
		}
		
		final double bonusRate = getBonusRate() + player.getStat().calcStat(Stats.ENCHANT_BONUS, 0, null, null);
		if (Config.CUSTOM_ENCHANT_ITEMS_ENABLED)
		{
			if (Config.ENCHANT_ITEMS_ID.containsKey(enchantItem.getItem().getId()))
			{
				if (enchantItem.getEnchantLevel() < 3)
				{
					finalChance = 100 + bonusRate;
				}
				else
				{
					finalChance = Config.ENCHANT_ITEMS_ID.get(enchantItem.getItem().getId()) + bonusRate;
				}
			}
			else
			{
				finalChance = chance + bonusRate;
			}
		}
		else
		{
			finalChance = chance + bonusRate;
		}
		
		if (finalChance <= 0)
		{
			return -1.0;
		}
		return finalChance;
	}
	
	public boolean isEnchantAnnounce(Player player, ItemInstance enchantItem, EnchantItem supportItem)
	{
		final EnchantItemGroup group = EnchantItemGroupsParser.getInstance().getItemGroup(enchantItem.getItem(), _scrollGroupId);
		if (group != null)
		{
			return group.isEnchantAnnounce(enchantItem.getEnchantLevel());
		}
		return false;
	}

	public EnchantResultType calculateSuccess(Player player, ItemInstance enchantItem, EnchantItem supportItem)
	{
		if (!isValid(enchantItem, supportItem))
		{
			return EnchantResultType.ERROR;
		}

		if (EnchantItemGroupsParser.getInstance().getScrollGroup(_scrollGroupId) == null)
		{
			_log.warn(getClass().getSimpleName() + ": Unexistent enchant scroll group specified for enchant scroll: " + getId());
			return EnchantResultType.ERROR;
		}

		final EnchantItemGroup group = EnchantItemGroupsParser.getInstance().getItemGroup(enchantItem.getItem(), _scrollGroupId);
		if (group == null)
		{
			_log.warn(getClass().getSimpleName() + ": Couldn't find enchant item group for scroll: " + getId() + " requested by: " + player);
			return EnchantResultType.ERROR;
		}

		double finalChance;

		final double chance = group.getChance(enchantItem.getEnchantLevel());
		
		if (chance <= 0)
		{
			return EnchantResultType.ERROR;
		}
		
		final double bonusRate = getBonusRate() + player.getStat().calcStat(Stats.ENCHANT_BONUS, 0, null, null);;
		final double supportBonusRate = ((supportItem != null) && !_isBlessed) ? supportItem.getBonusRate() : 0;

		if (Config.CUSTOM_ENCHANT_ITEMS_ENABLED)
		{
			if (Config.ENCHANT_ITEMS_ID.containsKey(enchantItem.getItem().getId()))
			{
				if (enchantItem.getEnchantLevel() < 3)
				{
					finalChance = 100 + bonusRate + supportBonusRate;
				}
				else
				{
					finalChance = Config.ENCHANT_ITEMS_ID.get(enchantItem.getItem().getId()) + bonusRate + supportBonusRate;
				}
			}
			else
			{
				finalChance = chance + bonusRate + supportBonusRate;
			}
		}
		else
		{
			finalChance = chance + bonusRate + supportBonusRate;
		}
		
		if (finalChance <= 0)
		{
			return EnchantResultType.ERROR;
		}

		final double random = 100 * Rnd.nextDouble();
		final boolean success = (random < (finalChance + player.getPremiumBonus().getEnchantChance()));

		return success ? EnchantResultType.SUCCESS : EnchantResultType.FAILURE;
	}
	
	public final int getIncreaseEnchantValue()
	{
		return _increaseEnchantBy;
	}
}