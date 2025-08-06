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

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2e.gameserver.data.parser.ItemsParser;
import l2e.gameserver.model.actor.templates.items.Item;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.stats.StatsSet;

public class EnchantItem
{
	protected static final Logger _log = LoggerFactory.getLogger(EnchantItem.class);

	private final int _id;
	private final boolean _isWeapon;
	private final int _grade;
	private final int _minEnchantLevel;
	private final int _maxEnchantLevel;
	private final double _bonusRate;
	private List<Integer> _itemIds;
	
	public EnchantItem(StatsSet set)
	{
		_id = set.getInteger("id");
		_isWeapon = set.getBool("isWeapon", true);
		_grade = ItemsParser._crystalTypes.get(set.getString("targetGrade", "none"));
		_minEnchantLevel = set.getInteger("minEnchant", 0);
		_maxEnchantLevel = set.getInteger("maxEnchant", 65535);
		_bonusRate = set.getDouble("bonusRate", 0);
	}

	public final int getId()
	{
		return _id;
	}

	public final double getBonusRate()
	{
		return _bonusRate;
	}

	public void addItem(int id)
	{
		if (_itemIds == null)
		{
			_itemIds = new ArrayList<>();
		}
		_itemIds.add(id);
	}

	public final boolean isValid(ItemInstance enchantItem)
	{
		if (enchantItem == null)
		{
			return false;
		}
		else if (enchantItem.isEnchantable() == 0)
		{
			return false;
		}
		else if (!isValidItemType(enchantItem.getItem().getType2()))
		{
			return false;
		}
		else if (((_maxEnchantLevel != 0) && (enchantItem.getEnchantLevel() >= _maxEnchantLevel)) || ((_minEnchantLevel > 0) && (enchantItem.getEnchantLevel() < _minEnchantLevel)))
		{
			return false;
		}
		else if (_grade != enchantItem.getItem().getItemGradeSPlus())
		{
			return false;
		}
		else if ((_itemIds != null) && !_itemIds.contains(enchantItem.getId()))
		{
			return false;
		}
		return true;
	}

	private boolean isValidItemType(int type2)
	{
		if (type2 == Item.TYPE2_WEAPON)
		{
			return _isWeapon;
		}
		else if ((type2 == Item.TYPE2_SHIELD_ARMOR) || (type2 == Item.TYPE2_ACCESSORY))
		{
			return !_isWeapon;
		}
		return false;
	}
	
	public int getMaxEnchant()
	{
		return _maxEnchantLevel;
	}
}