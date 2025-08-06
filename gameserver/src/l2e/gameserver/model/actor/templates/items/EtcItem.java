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
package l2e.gameserver.model.actor.templates.items;

import l2e.commons.util.StringUtil;
import l2e.gameserver.model.holders.ItemHolder;
import l2e.gameserver.model.items.itemcontainer.PcInventory;
import l2e.gameserver.model.items.type.EtcItemType;
import l2e.gameserver.model.stats.StatsSet;

public final class EtcItem extends Item
{
	private String _handler;
	private EtcItemType _type;
	private final boolean _isBlessed;
	private boolean _checkSlots;
	private final boolean _isForceRandom, _isSingleRandom;
	private String _skinType;
	private int _skinId;

	public EtcItem(StatsSet set)
	{
		super(set);
		_type = EtcItemType.valueOf(set.getString("etcitem_type", "none").toUpperCase());

		switch (getDefaultAction())
		{
			case soulshot :
			case summon_soulshot :
			case summon_spiritshot :
			case spiritshot :
			{
				_type = EtcItemType.SHOT;
				break;
			}
		}

		if (is_ex_immediate_effect())
		{
			_type = EtcItemType.HERB;
		}

		_type1 = Item.TYPE1_ITEM_QUESTITEM_ADENA;
		_type2 = Item.TYPE2_OTHER;

		if (isQuestItem())
		{
			_type2 = Item.TYPE2_QUEST;
		}
		else if ((getId() == PcInventory.ADENA_ID) || (getId() == PcInventory.ANCIENT_ADENA_ID))
		{
			_type2 = Item.TYPE2_MONEY;
		}

		_handler = set.getString("handler", null);
		_isBlessed = set.getBool("blessed", false);
		_checkSlots = set.getBool("checkSlots", false);

		final String capsuled_items = set.getString("capsuled_items", null);
		if (capsuled_items != null)
		{
			final String[] split = capsuled_items.split(";");
			for (final String part : split)
			{
				if (part.trim().isEmpty())
				{
					continue;
				}
				final String[] data = part.split(",");
				if (data.length < 4)
				{
					_log.info(StringUtil.concat("> Couldnt parse ", part, " in capsuled_items! item ", toString()));
					continue;
				}
				final int itemId = Integer.parseInt(data[0]);
				final long min = Long.parseLong(data[1]);
				final long max = Long.parseLong(data[2]);
				final double chance = Double.parseDouble(data[3]);
				final int enchant = data.length > 4 ? Integer.parseInt(data[4]) : 0;
				if (max < min)
				{
					_log.info(StringUtil.concat("> Max amount < Min amount in ", part, ", item ", toString()));
					continue;
				}
				addRewardItem(new ItemHolder(itemId, min, max, chance, enchant));
			}
			
			if (_handler == null)
			{
				_log.warn("Item " + this + " define capsuled_items but missing handler.");
				_handler = "ExtractableItems";
			}
		}
		
		_isForceRandom = set.getBool("isForceRandom", false);
		_isSingleRandom = set.getBool("isSingleRandom", false);
		
		final String visual_skin = set.getString("visual_skin", null);
		if (visual_skin != null)
		{
			final String[] data = visual_skin.split(",");
			if (data.length != 2)
			{
				_log.info(StringUtil.concat("> Couldnt parse ", visual_skin, " in visual_skin! item ", toString()));
				return;
			}
			_skinType = data[0];
			_skinId = Integer.parseInt(data[1]);
		}
	}
	
	@Override
	public EtcItemType getItemType()
	{
		return _type;
	}
	
	@Override
	public final boolean isConsumable()
	{
		return ((getItemType() == EtcItemType.SHOT) || (getItemType() == EtcItemType.POTION));
	}
	
	@Override
	public int getItemMask()
	{
		return getItemType().mask();
	}
	
	public String getHandlerName()
	{
		return _handler;
	}
	
	public final boolean isBlessed()
	{
		return _isBlessed;
	}

	@Override
	public boolean isExtractableItem()
	{
		return getRewardItems() != null;
	}
	
	public String getSkinType()
	{
		return _skinType;
	}
	
	public int getSkinId()
	{
		return _skinId;
	}
	
	public boolean isForceRandom()
	{
		return _isForceRandom;
	}
	
	public boolean isSingleRandom()
	{
		return _isSingleRandom;
	}
	
	public boolean isCheckSlots()
	{
		return _checkSlots;
	}
}
