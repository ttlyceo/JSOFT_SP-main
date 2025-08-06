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
package l2e.gameserver.model.items.multisell;


import l2e.gameserver.model.Augmentation;
import l2e.gameserver.model.Elementals;
import l2e.gameserver.model.items.instance.ItemInstance;

public class ItemInfo
{
	private final int _enchantLevel, _timeLimit;
	private final Augmentation _augmentation;
	private final byte _elementId;
	private final int _elementPower;
	private final int[] _elementals = new int[6];

	public ItemInfo(ItemInstance item)
	{
		_enchantLevel = item.getEnchantLevel();
		_timeLimit = (int) item.getTime();
		_augmentation = item.getAugmentation();
		_elementId = item.getAttackElementType();
		_elementPower = item.getAttackElementPower();
		_elementals[0] = item.getElementDefAttr(Elementals.FIRE);
		_elementals[1] = item.getElementDefAttr(Elementals.WATER);
		_elementals[2] = item.getElementDefAttr(Elementals.WIND);
		_elementals[3] = item.getElementDefAttr(Elementals.EARTH);
		_elementals[4] = item.getElementDefAttr(Elementals.HOLY);
		_elementals[5] = item.getElementDefAttr(Elementals.DARK);
	}

	public ItemInfo(int enchantLevel)
	{
		_enchantLevel = enchantLevel;
		_timeLimit = -1;
		_augmentation = null;
		_elementId = Elementals.NONE;
		_elementPower = 0;
		_elementals[0] = 0;
		_elementals[1] = 0;
		_elementals[2] = 0;
		_elementals[3] = 0;
		_elementals[4] = 0;
		_elementals[5] = 0;
	}

	public final int getEnchantLevel()
	{
		return _enchantLevel;
	}

	public final int getTimeLimit()
	{
		return _timeLimit;
	}
	
	public final int getTime()
	{
		return (int) (_timeLimit > 0 ? (System.currentTimeMillis() + (_timeLimit * 60 * 1000L)) : -1);
	}
	
	public final byte getElementId()
	{
		return _elementId;
	}

	public final int getElementPower()
	{
		return _elementPower;
	}

	public final int[] getElementals()
	{
		return _elementals;
	}
	
	public Augmentation getAugmentation()
	{
		return _augmentation;
	}
}