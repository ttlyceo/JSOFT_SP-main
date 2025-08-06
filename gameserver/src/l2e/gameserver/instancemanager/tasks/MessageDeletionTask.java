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
package l2e.gameserver.instancemanager.tasks;

import l2e.commons.log.LoggerObject;
import l2e.gameserver.Config;
import l2e.gameserver.data.parser.ItemsParser;
import l2e.gameserver.instancemanager.MailManager;
import l2e.gameserver.model.Elementals;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.entity.Message;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.items.instance.ItemInstance.ItemLocation;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.InventoryUpdate;
import l2e.gameserver.network.serverpackets.SystemMessage;

public final class MessageDeletionTask extends LoggerObject implements Runnable
{
	final int _msgId;

	public MessageDeletionTask(int msgId)
	{
		_msgId = msgId;
	}
	
	@Override
	public void run()
	{
		final Message msg = MailManager.getInstance().getMessage(_msgId);
		if (msg == null)
		{
			return;
		}

		if (msg.hasAttachments())
		{
			try
			{
				final Player sender = GameObjectsStorage.getPlayer(msg.getSenderId());
				if (sender != null)
				{
					sender.sendPacket(SystemMessageId.MAIL_RETURNED);
					final InventoryUpdate playerIU = Config.FORCE_INVENTORY_UPDATE ? null : new InventoryUpdate();
					for (final ItemInstance item : msg.getAttachments().getItems())
					{
						if (item == null)
						{
							continue;
						}
						
						final long count = item.getCount();
						final ItemInstance newItem = msg.getAttachments().transferItem(msg.getAttachments().getName(), item.getObjectId(), item.getCount(), sender.getInventory(), sender, null);
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
						sender.sendPacket(sm);
					}
					
					if (playerIU != null)
					{
						sender.sendPacket(playerIU);
					}
					else
					{
						sender.sendItemList(false);
					}
				}
				else
				{
					for (final ItemInstance item : msg.getAttachments().getItems())
					{
						if (item == null)
						{
							continue;
						}
						final ItemInstance returnItem = ItemsParser.getInstance().createItem("PayMail", item.getId(), item.getCount(), null, null);
						if (returnItem.isWeapon() || returnItem.isArmor())
						{
							returnItem.setEnchantLevel(item.getEnchantLevel());
							returnItem.setAugmentation(item.getAugmentation());
							if (item.getElementals() != null)
							{
								for (final Elementals elm : item.getElementals())
								{
									if (elm.getElement() != -1 && elm.getValue() != -1)
									{
										returnItem.setElementAttr(elm.getElement(), elm.getValue());
									}
								}
							}
						}
						returnItem.setOwnerId(msg.getSenderId());
						returnItem.setItemLocation(ItemLocation.INVENTORY);
						returnItem.updateDatabase(true);
						GameObjectsStorage.removeItem(returnItem);
					}
				}

				msg.getAttachments().deleteMe();
				msg.removeAttachments();

				final Player receiver = GameObjectsStorage.getPlayer(msg.getReceiverId());
				if (receiver != null)
				{
					final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.MAIL_RETURNED);
					receiver.sendPacket(sm);
				}
			}
			catch (final Exception e)
			{
				warn("Error returning items:" + e.getMessage(), e);
			}
		}
		MailManager.getInstance().deleteMessageInDb(msg.getId());
	}
}