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
package l2e.gameserver.model;

import l2e.gameserver.model.actor.templates.items.Item;
import l2e.gameserver.model.items.instance.ItemInstance;

public class TradeItem
{
	private int _objectId;
	private final Item _item;
	private final int _location;
	private int _enchant;
	private final Augmentation _augmentation;
	private final int _type1;
	private final int _type2;
	private long _count;
	private long _storeCount;
	private long _price;
	private byte _elemAtkType;
	private int _elemAtkPower;
	private int[] _elemDefAttr =
	{
	        0, 0, 0, 0, 0, 0
	};

	private final int[] _enchantOptions;
	private int _auctionId;
	private int _mana;
	private int _time;
	
	public TradeItem(ItemInstance item, long count, long price)
	{
		_objectId = item.getObjectId();
		_item = item.getItem();
		_location = item.getLocationSlot();
		_enchant = item.getEnchantLevel();
		_type1 = item.getCustomType1();
		_type2 = item.getCustomType2();
		_count = count;
		_price = price;
		_augmentation = item.getAugmentation();
		_mana = item.getMana();
		_time = item.isTimeLimitedItem() ? (int) (item.getRemainingTime() / 1000) : -9999;
		_elemAtkType = item.getAttackElementType();
		_elemAtkPower = item.getAttackElementPower();
		for (byte i = 0; i < 6; i++)
		{
			_elemDefAttr[i] = item.getElementDefAttr(i);
		}
		_enchantOptions = item.getEnchantOptions();
	}
	
	public TradeItem(Item item, int enchant, long count, long price, Augmentation augmentation, int mana, int time, int elemAtkType, int elemAtkPower, int[] elemDefAttr)
	{
		_objectId = 0;
		_item = item;
		_location = 0;
		_enchant = enchant;
		_type1 = 0;
		_type2 = 0;
		_count = count;
		_storeCount = count;
		_price = price;
		_augmentation = augmentation;
		_mana = mana;
		_time = time;
		_elemAtkType = (byte) elemAtkType;
		_elemAtkPower = elemAtkPower;
		_elemDefAttr = elemDefAttr;
		_enchantOptions = ItemInstance.DEFAULT_ENCHANT_OPTIONS;
	}
	
	public TradeItem(TradeItem item, long count, long price)
	{
		_objectId = item.getObjectId();
		_item = item.getItem();
		_location = item.getLocationSlot();
		_enchant = item.getEnchant();
		_type1 = item.getCustomType1();
		_type2 = item.getCustomType2();
		_count = count;
		_storeCount = count;
		_price = price;
		_augmentation = item.getAugmentation();
		_elemAtkType = item.getAttackElementType();
		_elemAtkPower = item.getAttackElementPower();
		for (byte i = 0; i < 6; i++)
		{
			_elemDefAttr[i] = item.getElementDefAttr(i);
		}
		_enchantOptions = item.getEnchantOptions();
	}
	
	public void setObjectId(int objectId)
	{
		_objectId = objectId;
	}
	
	public int getObjectId()
	{
		return _objectId;
	}
	
	public Item getItem()
	{
		return _item;
	}
	
	public int getLocationSlot()
	{
		return _location;
	}
	
	public void setEnchant(int enchant)
	{
		_enchant = enchant;
	}
	
	public int getEnchant()
	{
		return _enchant;
	}
	
	public int getCustomType1()
	{
		return _type1;
	}
	
	public int getCustomType2()
	{
		return _type2;
	}
	
	public void setCount(long count)
	{
		_count = count;
	}
	
	public long getCount()
	{
		return _count;
	}
	
	public long getStoreCount()
	{
		return _storeCount;
	}
	
	public void setPrice(long price)
	{
		_price = price;
	}
	
	public long getPrice()
	{
		return _price;
	}
	
	public void setAttackElementType(byte elemAtkType)
	{
		_elemAtkType = elemAtkType;
	}
	
	public byte getAttackElementType()
	{
		return _elemAtkType;
	}
	
	public void setAttackElementPower(int elemPower)
	{
		_elemAtkPower = (byte) elemPower;
	}
	
	public int getAttackElementPower()
	{
		return _elemAtkPower;
	}
	
	public int getElementDefAttr(byte i)
	{
		return _elemDefAttr[i];
	}
	
	public void setElemDefAttr(int i, int elemDefAttr)
	{
		_elemDefAttr[i] = elemDefAttr;
	}

	public int[] getEnchantOptions()
	{
		return _enchantOptions;
	}
	
	public void setAuctionId(int id)
	{
		_auctionId = id;
	}
	
	public int getAuctionId()
	{
		return _auctionId;
	}
	
	public Augmentation getAugmentation()
	{
		return _augmentation;
	}
	
	public int getMana()
	{
		return _mana;
	}
	
	public int getTime()
	{
		return _time;
	}
}