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
package l2e.gameserver.network.clientpackets;

import java.util.Arrays;

import l2e.gameserver.data.parser.AugmentationParser;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.items.Armor;
import l2e.gameserver.model.actor.templates.items.Item;
import l2e.gameserver.model.actor.templates.items.LifeStone;
import l2e.gameserver.model.actor.templates.items.Weapon;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.network.SystemMessageId;

public abstract class AbstractRefinePacket extends GameClientPacket
{
	protected static final boolean isValid(Player player, ItemInstance item, ItemInstance refinerItem, ItemInstance gemStones)
	{
		if (!isValid(player, item, refinerItem))
		{
			return false;
		}
		
		if (gemStones.getOwnerId() != player.getObjectId())
		{
			return false;
		}
		
		if (gemStones.getItemLocation() != ItemInstance.ItemLocation.INVENTORY)
		{
			return false;
		}
		
		final int grade = item.getItem().getItemGrade();
		final LifeStone ls = AugmentationParser.getInstance().getLifeStone(refinerItem.getId());
		
		if (getGemStoneId(grade) != gemStones.getId())
		{
			return false;
		}
		
		if (getGemStoneCount(grade, ls.getGrade()) > gemStones.getCount())
		{
			return false;
		}
		return true;
	}
	
	protected static final boolean isValid(Player player, ItemInstance item, ItemInstance refinerItem)
	{
		if (!isValid(player, item))
		{
			return false;
		}
		
		if (refinerItem.getOwnerId() != player.getObjectId())
		{
			return false;
		}
		
		if (refinerItem.getItemLocation() != ItemInstance.ItemLocation.INVENTORY)
		{
			return false;
		}
		
		final LifeStone ls = AugmentationParser.getInstance().getLifeStone(refinerItem.getId());
		if (ls == null)
		{
			return false;
		}
		
		if ((item.getItem() instanceof Weapon) && !ls.isWeaponAugment())
		{
			return false;
		}
		
		if ((item.getItem() instanceof Armor) && !ls.isArmorAugment())
		{
			return false;
		}
		
		if (player.getLevel() < ls.getPlayerLevel())
		{
			return false;
		}
		return true;
	}
	
	protected static final boolean isValid(Player player, ItemInstance item)
	{
		if (!isValid(player))
		{
			return false;
		}
		
		if (item.getOwnerId() != player.getObjectId())
		{
			return false;
		}
		if (item.isAugmented())
		{
			return false;
		}
		if (item.isHeroItem())
		{
			return false;
		}
		if (item.isShadowItem())
		{
			return false;
		}
		if (item.isCommonItem())
		{
			return false;
		}
		if (item.isEtcItem())
		{
			return false;
		}
		if (item.isTimeLimitedItem())
		{
			return false;
		}
		if (item.isPvp() && !AugmentationParser.getInstance().getParams().getBool("allowAugmentationPvpItems"))
		{
			return false;
		}
		if (item.getItem().getCrystalType() < Item.CRYSTAL_C && !AugmentationParser.getInstance().getParams().getBool("allowAugmentationAllItemsGrade"))
		{
			return false;
		}
		
		switch (item.getItemLocation())
		{
			case INVENTORY :
			case PAPERDOLL :
				break;
			default :
				return false;
		}
		
		if (item.getItem() instanceof Weapon)
		{
			switch (((Weapon) item.getItem()).getItemType())
			{
				case NONE :
				case FISHINGROD :
					return false;
				default :
					break;
			}
		}
		else if (item.getItem() instanceof Armor)
		{
			switch (item.getItem().getBodyPart())
			{
				case Item.SLOT_LR_FINGER :
				case Item.SLOT_LR_EAR :
				case Item.SLOT_NECK :
					break;
				default :
					if (item.getItem().isAugmentable() && AugmentationParser.getInstance().isAllowArmorAugmentation())
					{
						break;
					}
					return false;
			}
		}
		else
		{
			return false;
		}
		
		if (Arrays.binarySearch(AugmentationParser.getInstance().getForbiddenList(), item.getId()) >= 0)
		{
			return false;
		}
		return true;
	}
	
	protected static final boolean isValid(Player player)
	{
		if (player.getPrivateStoreType() != Player.STORE_PRIVATE_NONE)
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_AUGMENT_ITEMS_WHILE_A_PRIVATE_STORE_OR_PRIVATE_WORKSHOP_IS_IN_OPERATION);
			return false;
		}
		if (player.getActiveTradeList() != null)
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_AUGMENT_ITEMS_WHILE_TRADING);
			return false;
		}
		if (player.isDead())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_AUGMENT_ITEMS_WHILE_DEAD);
			return false;
		}
		if (player.isParalyzed())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_AUGMENT_ITEMS_WHILE_PARALYZED);
			return false;
		}
		if (player.isFishing())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_AUGMENT_ITEMS_WHILE_FISHING);
			return false;
		}
		if (player.isSitting())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_AUGMENT_ITEMS_WHILE_SITTING_DOWN);
			return false;
		}
		if (player.isCursedWeaponEquipped() || player.isActionsDisabled())
		{
			return false;
		}
		if (player.isEnchanting() || player.isProcessingTransaction())
		{
			return false;
		}
		return true;
	}
	
	protected static final int getGemStoneId(int itemGrade)
	{
		return AugmentationParser.getInstance().getGemStoneId(itemGrade);
	}
	
	protected static final int getGemStoneCount(int itemGrade, int lifeStoneGrade)
	{
		return AugmentationParser.getInstance().getGemStoneCount(itemGrade, lifeStoneGrade);
	}
}