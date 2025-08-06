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

import java.util.Arrays;

import l2e.gameserver.data.parser.AugmentationParser;
import l2e.gameserver.data.parser.ItemsParser;
import l2e.gameserver.model.Augmentation;
import l2e.gameserver.model.Elementals;
import l2e.gameserver.model.actor.templates.items.Armor;
import l2e.gameserver.model.actor.templates.items.Item;
import l2e.gameserver.model.actor.templates.items.Weapon;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.stats.StatsSet;

public class Ingredient
{
	private int _itemId;
	private long _itemCount;
	private final int _enchantmentLevel;
	private final int _timeLimit;
	private Augmentation _augmentation = null;
	private Elementals[] _elementals = null;
	private boolean _isTaxIngredient;
	private boolean _maintainIngredient;
	private final boolean _isCheckParams;
	private Item _template = null;
	private ItemInfo _itemInfo = null;
	private final int _minEnchant;
	private final int _maxEnchant;

	public Ingredient(StatsSet set)
	{
		this(set.getInteger("id"), set.getLong("count"), set.getInteger("enchantmentLevel", 0), set.getInteger("timeLimit", -1), generateAugmentation(set.getString("augmentation", null), set.getInteger("id")), generateElementals(set.getString("elementals", null), set.getInteger("id")), set.getBool("isTaxIngredient", false), set.getBool("maintainIngredient", false), set.getBool("validParameters", true), set.getInteger("minEnchant", 0), set.getInteger("maxEnchant", 0));
	}
	
	public Ingredient(int itemId, long itemCount, int enchantmentLevel, int timeLimit, Augmentation augmentation, Elementals[] elementals, boolean isTaxIngredient, boolean maintainIngredient, boolean checkParams, int minEnchant, int maxEnchant)
	{
		_itemId = itemId;
		_itemCount = itemCount;
		_enchantmentLevel = enchantmentLevel;
		_timeLimit = timeLimit;
		_isTaxIngredient = isTaxIngredient;
		_maintainIngredient = maintainIngredient;
		_isCheckParams = checkParams;
		if (_itemId > 0)
		{
			_template = ItemsParser.getInstance().getTemplate(_itemId);
		}
		
		_augmentation = augmentation;
		_elementals = elementals;
		_minEnchant = minEnchant;
		_maxEnchant = maxEnchant;
	}
	
	private static Augmentation generateAugmentation(String augmentation, int itemId)
	{
		if (augmentation != null)
		{
			final String[] aug = augmentation.split(":");
			
			final Item template = ItemsParser.getInstance().getTemplate(itemId);
			
			if (template.isHeroItem() || (Arrays.binarySearch(AugmentationParser.getInstance().getForbiddenList(), template.getId()) >= 0) || template.isCommonItem() || (template.isPvpItem() && !AugmentationParser.getInstance().getParams().getBool("allowAugmentationPvpItems")) || (template.getCrystalType() < Item.CRYSTAL_C && !AugmentationParser.getInstance().getParams().getBool("allowAugmentationAllItemsGrade")) || template.getId() == 13752 || template.getId() == 13753 || template.getId() == 13754 || template.getId() == 13755)
			{
				return null;
			}
			
			if (template instanceof Weapon)
			{
				switch (((Weapon) template).getItemType())
				{
					case NONE :
					case FISHINGROD :
						return null;
				}
			}
			else if (template instanceof Armor)
			{
				switch (template.getBodyPart())
				{
					case Item.SLOT_LR_FINGER :
					case Item.SLOT_LR_EAR :
					case Item.SLOT_NECK :
						break;
					default :
						if (template.isAugmentable() && AugmentationParser.getInstance().isAllowArmorAugmentation())
						{
							break;
						}
						return null;
				}
			}
			return new Augmentation(((Integer.parseInt(aug[0]) << 16) + Integer.parseInt(aug[1])));
		}
		return null;
	}
	
	private static Elementals[] generateElementals(String elementals, int itemId)
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

	public Ingredient getCopy()
	{
		return new Ingredient(_itemId, _itemCount, _enchantmentLevel, _timeLimit, _augmentation, _elementals, _isTaxIngredient, _maintainIngredient, _isCheckParams, _minEnchant, _maxEnchant);
	}

	public final Item getTemplate()
	{
		return _template;
	}

	public final void setItemInfo(ItemInstance item)
	{
		_itemInfo = new ItemInfo(item);
	}

	public final void setItemInfo(ItemInfo info)
	{
		_itemInfo = info;
	}

	public final ItemInfo getItemInfo()
	{
		return _itemInfo;
	}

	public final int getEnchantLevel()
	{
		return _itemInfo == null ? _enchantmentLevel : _itemInfo.getEnchantLevel();
	}
	
	public final int getTimeLimit()
	{
		return _itemInfo == null ? _timeLimit : _itemInfo.getTimeLimit();
	}
	
	public final int getTime()
	{
		return (int) (_timeLimit > 0 ? (System.currentTimeMillis() + (_timeLimit * 60 * 1000L)) : -1);
	}
	
	public final int getAugmentationId()
	{
		return _itemInfo == null ? _augmentation != null ? _augmentation.getAugmentationId() : 0 : _itemInfo.getAugmentation() != null ? _itemInfo.getAugmentation().getAugmentationId() : 0;
	}

	public final Augmentation getAugmentation()
	{
		return _itemInfo == null ? _augmentation : _itemInfo.getAugmentation();
	}
	
	public final void setItemId(int itemId)
	{
		_itemId = itemId;
	}

	public final int getId()
	{
		return _itemId;
	}

	public final void setCount(long itemCount)
	{
		_itemCount = itemCount;
	}

	public final long getCount()
	{
		return _itemCount;
	}

	public final void setIsTaxIngredient(boolean isTaxIngredient)
	{
		_isTaxIngredient = isTaxIngredient;
	}

	public final boolean isTaxIngredient()
	{
		return _isTaxIngredient;
	}

	public final void setMaintainIngredient(boolean maintainIngredient)
	{
		_maintainIngredient = maintainIngredient;
	}

	public final boolean getMaintainIngredient()
	{
		return _maintainIngredient;
	}

	public final boolean isStackable()
	{
		return _template == null ? true : _template.isStackable();
	}

	public final boolean isArmorOrWeapon()
	{
		return _template == null ? false : (_template instanceof Armor) || (_template instanceof Weapon);
	}

	public final int getWeight()
	{
		return _template == null ? 0 : _template.getWeight();
	}
	
	public Elementals[] getElementals()
	{
		return _elementals;
	}
	
	public byte getAttackElementType()
	{
		if (_itemInfo == null)
		{
			if (_template != null)
			{
				if (!_template.isWeapon())
				{
					return 0;
				}
				else if (_template.getElementals() != null)
				{
					return _template.getElementals()[0].getElement();
				}
				else if (_elementals != null)
				{
					return _elementals[0].getElement();
				}
			}
			return 0;
		}
		else
		{
			_itemInfo.getElementId();
		}
		return 0;
	}
	
	public int getAttackElementPower()
	{
		if (_itemInfo == null)
		{
			if (_template != null)
			{
				if (!_template.isWeapon())
				{
					return 0;
				}
				else if (_template.getElementals() != null)
				{
					return _template.getElementals()[0].getValue();
				}
				else if (_elementals != null)
				{
					return _elementals[0].getValue();
				}
			}
			return 0;
		}
		return _itemInfo.getElementPower();
	}
	
	public int getElementDefAttr(byte element)
	{
		if (_itemInfo == null)
		{
			if (_template != null)
			{
				if (!_template.isArmor())
				{
					return 0;
				}
				else if (_template.getElementals() != null)
				{
					final Elementals elm = _template.getElemental(element);
					if (elm != null)
					{
						return elm.getValue();
					}
				}
				else if (_elementals != null)
				{
					final Elementals elm = getElemental(element);
					if (elm != null)
					{
						return elm.getValue();
					}
				}
			}
			return 0;
		}
		return _itemInfo.getElementals()[element];
	}
	
	public Elementals getElemental(byte attribute)
	{
		if (_elementals == null)
		{
			return null;
		}
		for (final Elementals elm : _elementals)
		{
			if (elm.getElement() == attribute)
			{
				return elm;
			}
		}
		return null;
	}
	
	public boolean isCheckParams()
	{
		return _isCheckParams;
	}
	
	public final int getMinEnchant()
	{
		return _minEnchant;
	}
	
	public final int getMaxEnchant()
	{
		return _maxEnchant;
	}
}