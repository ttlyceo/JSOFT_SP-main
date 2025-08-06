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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import l2e.gameserver.Config;
import l2e.gameserver.model.base.BonusType;
import l2e.gameserver.model.stats.StatsSet;

public class PremiumTemplate
{
	private final int _id;
	private long _time;
	private final StatsSet _params;
	private final String _icon;
	private final List<PremiumGift> _list = new ArrayList<>();
	private final List<PremiumPrice> _price = new ArrayList<>();
	private final Map<BonusType, Double> _bonusList = new ConcurrentHashMap<>();
	private final boolean _isOnlineType;
	private final boolean _isPersonal;
	private final int _groupId;
	
	public PremiumTemplate(int id, StatsSet params, String icon, boolean isOnlineType, boolean isPersonal, int groupId)
	{
		_id = id;
		_params = params;
		_icon = icon;
		_isOnlineType = isOnlineType;
		_isPersonal = isPersonal;
		_groupId = groupId;
	}
	
	public int getId()
	{
		return _id;
	}
	
	public long getTime()
	{
		return _time;
	}
	
	public void setTime(long time)
	{
		_time = time;
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
	
	public String getIcon()
	{
		return _icon;
	}
	
	public boolean isOnlineType()
	{
		return _isOnlineType;
	}
	
	public boolean isPersonal()
	{
		return _isPersonal;
	}
	
	public int getGroupId()
	{
		return _groupId;
	}
	
	public void setRate(PremiumRates key, String value)
	{
		final String[] param = value.split("-");
		switch (key)
		{
			case ADENA:
				addBonusType(BonusType.DROP_ADENA, Double.parseDouble(value));
				break;
			case DROP_RAID :
				addBonusType(BonusType.DROP_RAID, Double.parseDouble(value));
				break;
			case DROP_EPIC :
				addBonusType(BonusType.DROP_EPIC, Double.parseDouble(value));
				break;
			case FISHING :
				addBonusType(BonusType.FISHING, Double.parseDouble(value));
				break;
			case QUEST_REWARD :
				addBonusType(BonusType.QUEST_REWARD, Double.parseDouble(value));
				break;
			case QUEST_DROP :
				addBonusType(BonusType.QUEST_DROP, Double.parseDouble(value));
				break;
			case CRAFT:
				addBonusType(BonusType.CRAFT_CHANCE, Integer.parseInt(value));
				break;
			case DROP:
				addBonusType(BonusType.DROP_ITEMS, Double.parseDouble(value));
				break;
			case ELEMENT_STONES:
				addBonusType(BonusType.ELEMENT_STONE, Double.parseDouble(value));
				break;
			case SEAL_STONES :
				addBonusType(BonusType.SEAL_STONE, Double.parseDouble(value));
				break;
			case EXP:
				addBonusType(BonusType.EXP, Double.parseDouble(value));
				break;
			case MASTERWORK_CRAFT:
				addBonusType(BonusType.MASTER_WORK_CHANCE, Integer.parseInt(value));
				break;
			case ENCHANT :
				addBonusType(BonusType.ENCHANT_CHANCE, Integer.parseInt(value));
				break;
			case SIEGE:
				addBonusType(BonusType.SIEGE, Double.parseDouble(value));
				break;
			case SP:
				addBonusType(BonusType.SP, Double.parseDouble(value));
				break;
			case SPOIL:
				addBonusType(BonusType.SPOIL, Double.parseDouble(value));
				break;
			case WEIGHT_LIMIT:
				addBonusType(BonusType.WEIGHT, Double.parseDouble(value));
				break;
			case FAME :
				addBonusType(BonusType.FAME, Double.parseDouble(value));
				break;
			case REFLECTION_REDUCE :
				addBonusType(BonusType.REFLECTION_REDUCE, Double.parseDouble(value));
				break;
			case EVENT :
				addBonusType(BonusType.EVENTS, Double.parseDouble(value));
				break;
			case MODIFIER_NOBLE_STONES :
				if (param.length == 2)
				{
					addBonusType(BonusType.NOBLE_STONE_MIN_AMOUNT, Double.parseDouble(param[0]));
					addBonusType(BonusType.NOBLE_STONE_MAX_AMOUNT, Double.parseDouble(param[1]));
				}
				break;
			case MODIFIER_SEAL_STONES :
				if (param.length == 2)
				{
					addBonusType(BonusType.SEAL_STONE_MIN_AMOUNT, Double.parseDouble(param[0]));
					addBonusType(BonusType.SEAL_STONE_MAX_AMOUNT, Double.parseDouble(param[1]));
				}
				break;
			case MODIFIER_LIFE_STONES :
				if (param.length == 2)
				{
					addBonusType(BonusType.LIFE_STONE_MIN_AMOUNT, Double.parseDouble(param[0]));
					addBonusType(BonusType.LIFE_STONE_MAX_AMOUNT, Double.parseDouble(param[1]));
				}
				break;
			case MODIFIER_ENCHANT_SCROLLS :
				if (param.length == 2)
				{
					addBonusType(BonusType.ENCHANT_SCROLL_MIN_AMOUNT, Double.parseDouble(param[0]));
					addBonusType(BonusType.ENCHANT_SCROLL_MAX_AMOUNT, Double.parseDouble(param[1]));
				}
				break;
			case MODIFIER_FORGOTTEN_SCROLLS :
				if (param.length == 2)
				{
					addBonusType(BonusType.FORGOTTEN_SCROLL_MIN_AMOUNT, Double.parseDouble(param[0]));
					addBonusType(BonusType.FORGOTTEN_SCROLL_MAX_AMOUNT, Double.parseDouble(param[1]));
				}
				break;
			case MODIFIER_MATERIALS :
				if (param.length == 2)
				{
					addBonusType(BonusType.MATERIAL_MIN_AMOUNT, Double.parseDouble(param[0]));
					addBonusType(BonusType.MATERIAL_MAX_AMOUNT, Double.parseDouble(param[1]));
				}
				break;
			case MODIFIER_RECIPES :
				if (param.length == 2)
				{
					addBonusType(BonusType.RECIPE_MIN_AMOUNT, Double.parseDouble(param[0]));
					addBonusType(BonusType.RECIPE_MAX_AMOUNT, Double.parseDouble(param[1]));
				}
				break;
			case MODIFIER_BELTS :
				if (param.length == 2)
				{
					addBonusType(BonusType.BELT_MIN_AMOUNT, Double.parseDouble(param[0]));
					addBonusType(BonusType.BELT_MAX_AMOUNT, Double.parseDouble(param[1]));
				}
				break;
			case MODIFIER_BRACELETS :
				if (param.length == 2)
				{
					addBonusType(BonusType.BRACLET_MIN_AMOUNT, Double.parseDouble(param[0]));
					addBonusType(BonusType.BRACLET_MAX_AMOUNT, Double.parseDouble(param[1]));
				}
				break;
			case MODIFIER_CLOAKS :
				if (param.length == 2)
				{
					addBonusType(BonusType.CLOAK_MIN_AMOUNT, Double.parseDouble(param[0]));
					addBonusType(BonusType.CLOAK_MAX_AMOUNT, Double.parseDouble(param[1]));
				}
				break;
			case MODIFIER_CODEX :
				if (param.length == 2)
				{
					addBonusType(BonusType.CODEX_BOOK_MIN_AMOUNT, Double.parseDouble(param[0]));
					addBonusType(BonusType.CODEX_BOOK_MAX_AMOUNT, Double.parseDouble(param[1]));
				}
				break;
			case MODIFIER_ATT_STONES :
				if (param.length == 2)
				{
					addBonusType(BonusType.ATT_STONE_MIN_AMOUNT, Double.parseDouble(param[0]));
					addBonusType(BonusType.ATT_STONE_MAX_AMOUNT, Double.parseDouble(param[1]));
				}
				break;
			case MODIFIER_ATT_CRYSTALS :
				if (param.length == 2)
				{
					addBonusType(BonusType.ATT_CRYSTAL_MIN_AMOUNT, Double.parseDouble(param[0]));
					addBonusType(BonusType.ATT_CRYSTAL_MAX_AMOUNT, Double.parseDouble(param[1]));
				}
				break;
			case MODIFIER_ATT_JEWELS :
				if (param.length == 2)
				{
					addBonusType(BonusType.ATT_JEWEL_MIN_AMOUNT, Double.parseDouble(param[0]));
					addBonusType(BonusType.ATT_JEWEL_MAX_AMOUNT, Double.parseDouble(param[1]));
				}
				break;
			case MODIFIER_ATT_ENERGY :
				if (param.length == 2)
				{
					addBonusType(BonusType.ATT_ENERGY_MIN_AMOUNT, Double.parseDouble(param[0]));
					addBonusType(BonusType.ATT_ENERGY_MAX_AMOUNT, Double.parseDouble(param[1]));
				}
				break;
			case MODIFIER_WEAPONS :
				if (param.length == 2)
				{
					addBonusType(BonusType.WEAPON_MIN_AMOUNT, Double.parseDouble(param[0]));
					addBonusType(BonusType.WEAPON_MAX_AMOUNT, Double.parseDouble(param[1]));
				}
				break;
			case MODIFIER_ARMORS :
				if (param.length == 2)
				{
					addBonusType(BonusType.ARMOR_MIN_AMOUNT, Double.parseDouble(param[0]));
					addBonusType(BonusType.ARMOR_MAX_AMOUNT, Double.parseDouble(param[1]));
				}
				break;
			case MODIFIER_ACCESSORYES :
				if (param.length == 2)
				{
					addBonusType(BonusType.ACCESSORY_MIN_AMOUNT, Double.parseDouble(param[0]));
					addBonusType(BonusType.ACCESSORY_MAX_AMOUNT, Double.parseDouble(param[1]));
				}
				break;
			case MAX_SPOIL_PER_ONE_GROUP :
				addBonusType(BonusType.MAX_SPOIL_PER_ONE_GROUP, Integer.parseInt(value));
				break;
			case MAX_DROP_PER_ONE_GROUP :
				addBonusType(BonusType.MAX_DROP_PER_ONE_GROUP, Integer.parseInt(value));
				break;
			case MAX_DROP_RAID_PER_ONE_GROUP :
				addBonusType(BonusType.MAX_RAID_DROP_PER_ONE_GROUP, Integer.parseInt(value));
				break;
			case GROUP_RATE :
				addBonusType(BonusType.GROUP_RATE, Double.parseDouble(value));
				break;
		}
	}
	
	private void addBonusType(BonusType type, double value)
	{
		if (_bonusList.containsKey(type))
		{
			final var val = _bonusList.get(type);
			switch (type)
			{
				case CRAFT_CHANCE :
				case MASTER_WORK_CHANCE :
				case ENCHANT_CHANCE :
				case MAX_DROP_PER_ONE_GROUP :
				case MAX_SPOIL_PER_ONE_GROUP :
				case MAX_RAID_DROP_PER_ONE_GROUP :
					_bonusList.put(type, (val + value));
					break;
				default :
					_bonusList.put(type, (val * value));
					break;
			}
			return;
		}
		_bonusList.put(type, value);
	}
	
	public double getBonusType(BonusType type, double defaultValue)
	{
		if (!_bonusList.containsKey(type))
		{
			return defaultValue;
		}
		return _bonusList.get(type);
	}
	
	public Map<BonusType, Double> getBonusList()
	{
		return _bonusList;
	}
	
	public List<PremiumGift> getGifts()
	{
		return _list;
	}
	
	public void addGift(PremiumGift gift)
	{
		_list.add(gift);
	}
	
	public List<PremiumPrice> getPriceList()
	{
		return _price;
	}
	
	public void addPrice(PremiumPrice price)
	{
		_price.add(price);
	}
}