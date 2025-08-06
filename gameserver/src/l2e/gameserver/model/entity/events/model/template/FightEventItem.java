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
package l2e.gameserver.model.entity.events.model.template;

import l2e.gameserver.data.parser.ItemsParser;
import l2e.gameserver.model.Augmentation;
import l2e.gameserver.model.Elementals;
import l2e.gameserver.model.actor.templates.items.Item;

public class FightEventItem
{
	private final int _itemId;
	private final long _itemCount;
	private final int _itemEnchant;
	private Augmentation _augmentation = null;
	private Elementals[] _elementals = null;
	
	public FightEventItem(final int itemId, final long itemCount, final int itemEnchant, String augmentation, String elementals)
    {
    	_itemId = itemId;
    	_itemCount = itemCount;
    	_itemEnchant = itemEnchant;
		_augmentation = generateAugmentation(augmentation, _itemId);
		_elementals = generateElementals(elementals, _itemId);
    }

    public int getId()
    {
        return _itemId;
    }

    public long getAmount()
    {
        return _itemCount;
    }

    public int getEnchantLvl()
    {
        return _itemEnchant;
    }
	
	public Augmentation getAugmentation()
	{
		return _augmentation;
	}
	
	public Elementals[] getElementals()
	{
		return _elementals;
	}
	
	private Augmentation generateAugmentation(String augmentation, int itemId)
	{
		if (augmentation != null)
		{
			final String[] aug = augmentation.split(":");
			
			final Item template = ItemsParser.getInstance().getTemplate(itemId);
			if (!template.isWeapon() || template.getCrystalType() == Item.CRYSTAL_NONE || template.getCrystalType() == Item.CRYSTAL_D || template.isHeroItem() || template.getId() == 13752 || template.getId() == 13753 || template.getId() == 13754 || template.getId() == 13755)
			{
				return null;
			}
			else
			{
				return new Augmentation(((Integer.parseInt(aug[0]) << 16) + Integer.parseInt(aug[1])));
			}
		}
		return null;
	}
	
	private Elementals[] generateElementals(String elementals, int itemId)
	{
		Elementals[] elementalss = null;
		if (elementals != null)
		{
			final Item template = ItemsParser.getInstance().getTemplate(itemId);
			final String[] elements = elementals.split(";");
			if (template.isWeapon())
			{
				if (template.isCommonItem() || !template.isElementable() || template.getCrystalType() < Item.CRYSTAL_S)
				{
					return null;
				}
				
				final String[] element = elements[0].split(":");
				if (element != null)
				{
					int value = Integer.parseInt(element[1]);
					if (value > 300)
					{
						value = 300;
					}
					
					if (elementalss == null)
					{
						elementalss = new Elementals[1];
						elementalss[0] = new Elementals(Byte.parseByte(element[0]), value);
					}
				}
			}
			else if (template.isArmor())
			{
				if (elements.length > 3)
				{
					return null;
				}
				
				if (template.isCommonItem() || !template.isElementable() || template.getCrystalType() < Item.CRYSTAL_S || template.getBodyPart() == Item.SLOT_L_HAND)
				{
					return null;
				}
				
				for (final String el : elements)
				{
					final String[] element = el.split(":");
					if (element != null)
					{
						final byte et = Elementals.getReverseElement(Byte.parseByte(element[0]));
						int value = Integer.parseInt(element[1]);
						if (value > 120)
						{
							value = 120;
						}
						
						if (elementalss == null)
						{
							elementalss = new Elementals[1];
							elementalss[0] = new Elementals(et, value);
						}
						else
						{
							for (final Elementals elm : elementalss)
							{
								if (elm.getElement() == Elementals.getReverseElement(et))
								{
									continue;
								}
							}
							
							final Elementals elm = new Elementals(et, value);
							final Elementals[] array = new Elementals[elementalss.length + 1];
							System.arraycopy(elementalss, 0, array, 0, elementalss.length);
							array[elementalss.length] = elm;
							elementalss = array;
						}
					}
				}
			}
		}
		return elementalss;
	}
}