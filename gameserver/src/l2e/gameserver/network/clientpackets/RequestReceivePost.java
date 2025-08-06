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

import static l2e.gameserver.model.items.itemcontainer.PcInventory.ADENA_ID;

import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.data.parser.ItemsParser;
import l2e.gameserver.instancemanager.MailManager;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.entity.Message;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.items.instance.ItemInstance.ItemLocation;
import l2e.gameserver.model.items.itemcontainer.ItemContainer;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.ExChangePostState;
import l2e.gameserver.network.serverpackets.InventoryUpdate;
import l2e.gameserver.network.serverpackets.StatusUpdate;
import l2e.gameserver.network.serverpackets.SystemMessage;

public final class RequestReceivePost extends GameClientPacket
{
	private int _msgId;
	
	@Override
	protected void readImpl()
	{
		_msgId = readD();
	}
	
	@Override
	public void runImpl()
	{
		if (!Config.ALLOW_MAIL || !Config.ALLOW_ATTACHMENTS)
		{
			return;
		}
		
		final Player activeChar = getClient().getActiveChar();
		if (activeChar == null)
		{
			return;
		}
		
		if (!activeChar.getAccessLevel().allowTransaction())
		{
			activeChar.sendMessage("Transactions are disabled for your Access Level");
			return;
		}
		
		if (!activeChar.isInZonePeace())
		{
			activeChar.sendPacket(SystemMessageId.CANT_RECEIVE_NOT_IN_PEACE_ZONE);
			return;
		}
		
		if (activeChar.getActiveTradeList() != null)
		{
			activeChar.sendPacket(SystemMessageId.CANT_RECEIVE_DURING_EXCHANGE);
			return;
		}
		
		if (activeChar.isEnchanting())
		{
			activeChar.sendPacket(SystemMessageId.CANT_RECEIVE_DURING_ENCHANT);
			return;
		}
		
		if (activeChar.getPrivateStoreType() > Player.STORE_PRIVATE_NONE)
		{
			activeChar.sendPacket(SystemMessageId.CANT_RECEIVE_PRIVATE_STORE);
			return;
		}
		
		final Message msg = MailManager.getInstance().getMessage(_msgId);
		if (msg == null)
		{
			return;
		}
		
		if (msg.getReceiverId() != activeChar.getObjectId())
		{
			Util.handleIllegalPlayerAction(activeChar, "" + activeChar.getName(null) + " tried to get not own attachment!");
			return;
		}
		
		if (!msg.hasAttachments())
		{
			return;
		}
		
		final ItemContainer attachments = msg.getAttachments();
		if (attachments == null)
		{
			return;
		}
		
		int weight = 0;
		int slots = 0;
		
		for (final ItemInstance item : attachments.getItems())
		{
			if (item == null)
			{
				continue;
			}
			
			if (item.getOwnerId() != msg.getSenderId())
			{
				Util.handleIllegalPlayerAction(activeChar, "" + activeChar.getName(null) + " tried to get wrong item (ownerId != senderId) from attachment!");
				return;
			}
			
			if (item.getItemLocation() != ItemLocation.MAIL)
			{
				Util.handleIllegalPlayerAction(activeChar, "" + activeChar.getName(null) + " tried to get wrong item (Location != MAIL) from attachment!");
				return;
			}
			
			if (item.getLocationSlot() != msg.getId())
			{
				Util.handleIllegalPlayerAction(activeChar, "" + activeChar.getName(null) + " tried to get items from different attachment!");
				return;
			}
			
			weight += item.getCount() * item.getItem().getWeight();
			if (!item.isStackable())
			{
				slots += item.getCount();
			}
			else if (activeChar.getInventory().getItemByItemId(item.getId()) == null)
			{
				slots++;
			}
		}
		
		if (!activeChar.getInventory().validateCapacity(slots))
		{
			activeChar.sendPacket(SystemMessageId.CANT_RECEIVE_INVENTORY_FULL);
			return;
		}
		
		if (!activeChar.getInventory().validateWeight(weight))
		{
			activeChar.sendPacket(SystemMessageId.CANT_RECEIVE_INVENTORY_FULL);
			return;
		}
		
		final long adena = msg.getReqAdena();
		if ((adena > 0) && !activeChar.reduceAdena("PayMail", adena, null, true))
		{
			activeChar.sendPacket(SystemMessageId.CANT_RECEIVE_NO_ADENA);
			return;
		}
		
		final InventoryUpdate playerIU = Config.FORCE_INVENTORY_UPDATE ? null : new InventoryUpdate();
		for (final ItemInstance item : attachments.getItems())
		{
			if (item == null)
			{
				continue;
			}
			
			if (item.getOwnerId() != msg.getSenderId())
			{
				Util.handleIllegalPlayerAction(activeChar, "" + activeChar.getName(null) + " tried to get items with owner != sender !");
				return;
			}
			
			final long count = item.getCount();
			final ItemInstance newItem = attachments.transferItem(attachments.getName(), item.getObjectId(), item.getCount(), activeChar.getInventory(), activeChar, null);
			if (newItem == null)
			{
				return;
			}
			
			if (playerIU != null)
			{
				if (newItem.getCount() > count)
				{
					playerIU.addModifiedItem(newItem);
				}
				else
				{
					playerIU.addNewItem(newItem);
				}
			}
			final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.YOU_ACQUIRED_S2_S1);
			sm.addItemName(item.getId());
			sm.addItemNumber(count);
			activeChar.sendPacket(sm);
		}
		
		if (playerIU != null)
		{
			activeChar.sendPacket(playerIU);
		}
		else
		{
			activeChar.sendItemList(false);
		}
		
		msg.removeAttachments();
		activeChar.sendStatusUpdate(false, false, StatusUpdate.CUR_LOAD);
		
		SystemMessage sm;
		final Player sender = GameObjectsStorage.getPlayer(msg.getSenderId());
		if (adena > 0)
		{
			if (sender != null)
			{
				sender.addAdena("PayMail", adena, activeChar, false);
				sm = SystemMessage.getSystemMessage(SystemMessageId.PAYMENT_OF_S1_ADENA_COMPLETED_BY_S2);
				sm.addItemNumber(adena);
				sm.addCharName(activeChar);
				sender.sendPacket(sm);
			}
			else
			{
				final ItemInstance paidAdena = ItemsParser.getInstance().createItem("PayMail", ADENA_ID, adena, activeChar, null);
				paidAdena.setOwnerId(msg.getSenderId());
				paidAdena.setItemLocation(ItemLocation.INVENTORY);
				paidAdena.updateDatabase(true);
				GameObjectsStorage.removeItem(paidAdena);
			}
		}
		else if (sender != null)
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.S1_ACQUIRED_ATTACHED_ITEM);
			sm.addCharName(activeChar);
			sender.sendPacket(sm);
		}
		
		activeChar.sendPacket(new ExChangePostState(true, _msgId, Message.READED));
		activeChar.sendPacket(SystemMessageId.MAIL_SUCCESSFULLY_RECEIVED);
	}
	
	@Override
	protected boolean triggersOnActionRequest()
	{
		return false;
	}
}