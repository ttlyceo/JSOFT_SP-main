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

public class ItemRecovery
{
	private int _charId;
	private int _itemId;
	private int _objectId;
	private long _count;
	private int _enchantLevel;
	private int _augmentationId;
	private String _elementals;
	private long _time;

    public ItemRecovery()
	{
    }

	public int getCharId()
	{
		return _charId;
    }

	public void setCharId(int charId)
	{
		_charId = charId;
    }

	public int getItemId()
	{
		return _itemId;
    }

	public void setItemId(int itemId)
	{
		_itemId = itemId;
    }

	public int getObjectId()
	{
		return _objectId;
    }

	public void setObjectId(int objectId)
	{
		_objectId = objectId;
    }

	public long getCount()
	{
		return _count;
    }

	public void setCount(long count)
	{
		_count = count;
    }

	public int getEnchantLevel()
	{
		return _enchantLevel;
    }

	public void setEnchantLevel(int enchantLevel)
	{
		_enchantLevel = enchantLevel;
    }

	public long getTime()
	{
		return _time;
    }

	public void setTime(long time)
	{
		_time = time;
    }
	
	public void setAugmentationId(int augmentationId)
	{
		_augmentationId = augmentationId;
	}
	
	public int getAugmentationId()
	{
		return _augmentationId;
	}
	
	public void setElementals(String elementals)
	{
		_elementals = elementals;
	}
	
	public String getElementals()
	{
		return _elementals;
	}
}