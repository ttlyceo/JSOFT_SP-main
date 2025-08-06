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
package l2e.gameserver.listener.player.impl;

import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.data.parser.AugmentationParser;
import l2e.gameserver.handler.communityhandlers.CommunityBoardHandler;
import l2e.gameserver.handler.communityhandlers.ICommunityBoardHandler;
import l2e.gameserver.listener.player.OnAnswerListener;
import l2e.gameserver.model.Augmentation;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.items.Item;
import l2e.gameserver.model.actor.templates.items.Weapon;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.items.itemcontainer.Inventory;
import l2e.gameserver.model.strings.server.ServerMessage;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.ExVariationCancelResult;
import l2e.gameserver.network.serverpackets.InventoryUpdate;
import l2e.gameserver.network.serverpackets.SystemMessage;

public class AugmentationAnswerListener implements OnAnswerListener
{
	private final Player _player;
	private final int _augId;
	private final int _selectPage;
	private final int _page;
		
	public AugmentationAnswerListener(Player player, int augId, int selectPage, int page)
	{
		_player = player;
		_augId = augId;
		_selectPage = selectPage;
		_page = page;
	}
		
	@Override
	public void sayYes()
	{
		if (_player != null && _player.isOnline())
		{
			if ((_player.isInStoreMode()) || (_player.isProcessingRequest()) || (_player.getActiveRequester() != null))
			{
				_player.sendMessage((new ServerMessage("ServiceBBS.AUGMENT_STOREMOD", _player.getLang())).toString());
				return;
			}
			
			final ItemInstance targetItem = _player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
			if (targetItem == null)
			{
				_player.sendMessage((new ServerMessage("ServiceBBS.AUGMENT_NOWEAPON", _player.getLang())).toString());
				return;
			}
			if (!checkItemType(targetItem))
			{
				return;
			}
			if (_player.getInventory().getItemByItemId(Config.SERVICES_AUGMENTATION_ITEM[0]) == null)
			{
				_player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
				return;
			}
			if (_player.getInventory().getItemByItemId(Config.SERVICES_AUGMENTATION_ITEM[0]).getCount() < Config.SERVICES_AUGMENTATION_ITEM[1])
			{
				_player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
				return;
			}
			_player.destroyItemByItemId("AugmentBBS", Config.SERVICES_AUGMENTATION_ITEM[0], Config.SERVICES_AUGMENTATION_ITEM[1], _player, true);
			Util.addServiceLog(_player.getName(null) + " buy augmentation for item service!");
			unAugment(_player, targetItem);
			_player.getInventory().unEquipItemInSlot(Inventory.PAPERDOLL_RHAND);
			final int augId = _augId;
			final int secAugId = AugmentationParser.getInstance().generateRandomSecondaryAugmentation();
			targetItem.setAugmentation(new Augmentation(((augId << 16) + secAugId)));
			_player.getInventory().equipItem(targetItem);
			final InventoryUpdate iu = new InventoryUpdate();
			iu.addModifiedItem(targetItem);
			_player.sendPacket(iu);
			_player.broadcastCharInfo();
			_player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.THE_ITEM_WAS_SUCCESSFULLY_AUGMENTED));
			final ICommunityBoardHandler cmd = CommunityBoardHandler.getInstance().getHandler("_bbsservice");
			if (cmd != null)
			{
				cmd.onBypassCommand("_bbsservice:augmentationPage " + _page + " " + _selectPage, _player);
			}
		}
	}
	
	@Override
	public void sayNo()
	{
		//
	}
	
	private boolean checkItemType(ItemInstance item)
	{
		if (item.isHeroItem() || item.isShadowItem() || item.isCommonItem())
		{
			return false;
		}
		
		switch (item.getId())
		{
			case 13752 :
			case 13753 :
			case 13754 :
			case 13755 :
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
		
		switch (((Weapon) item.getItem()).getItemType())
		{
			case NONE :
			case FISHINGROD :
				return false;
			default :
				break;
		}
		return true;
	}
	
	private void unAugment(Player player, ItemInstance item)
	{
		if (!item.isAugmented())
		{
			return;
		}
		
		final boolean equipped = item.isEquipped();
		if (equipped)
		{
			player.getInventory().unEquipItemInSlot(Inventory.PAPERDOLL_RHAND);
		}
		item.removeAugmentation();
		
		if (equipped)
		{
			player.getInventory().equipItem(item);
		}
		
		final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.AUGMENTATION_HAS_BEEN_SUCCESSFULLY_REMOVED_FROM_YOUR_S1);
		sm.addItemName(item.getId());
		player.sendPacket(sm);
		player.sendPacket(new ExVariationCancelResult(1));
		
		final InventoryUpdate iu = new InventoryUpdate();
		iu.addModifiedItem(item);
		player.sendPacket(iu);
	}
}