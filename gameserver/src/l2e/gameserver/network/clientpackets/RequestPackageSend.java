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

import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.holders.ItemHolder;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.items.itemcontainer.ItemContainer;
import l2e.gameserver.model.items.itemcontainer.PcFreight;
import l2e.gameserver.model.items.itemcontainer.PcInventory;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.InventoryUpdate;
import l2e.gameserver.network.serverpackets.StatusUpdate;

public class RequestPackageSend extends GameClientPacket
{
	private static final int BATCH_LENGTH = 12;
	
	private ItemHolder _items[] = null;
	private int _objectId;
	
	@Override
	protected void readImpl()
	{
		_objectId = readD();
		
		final int count = readD();
		if ((count <= 0) || (count > Config.MAX_ITEM_IN_PACKET) || ((count * BATCH_LENGTH) != _buf.remaining()))
		{
			return;
		}
		
		_items = new ItemHolder[count];
		for (int i = 0; i < count; i++)
		{
			final int objId = readD();
			final long cnt = readQ();
			if ((objId < 1) || (cnt < 0))
			{
				_items = null;
				return;
			}
			
			_items[i] = new ItemHolder(objId, cnt);
		}
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getActiveChar();
		if ((_items == null) || (player == null) || !player.getAccountChars().containsKey(_objectId))
		{
			return;
		}
		
		if (player.isActionsDisabled())
		{
			player.sendActionFailed();
			return;
		}
		
		final Npc manager = player.getLastFolkNPC();
		if (((manager == null) || !player.isInsideRadius(manager, Npc.INTERACTION_DISTANCE, false, false)))
		{
			return;
		}
		
		if (player.getActiveEnchantItemId() != Player.ID_NONE)
		{
			Util.handleIllegalPlayerAction(player, "" + player.getName(null) + " tried to use enchant Exploit!");
			return;
		}
		
		if (player.getActiveTradeList() != null)
		{
			return;
		}
		
		if (!Config.ALT_GAME_KARMA_PLAYER_CAN_USE_WAREHOUSE && (player.getKarma() > 0))
		{
			return;
		}
		
		final int fee = _items.length * Config.ALT_FREIGHT_PRICE;
		long currentAdena = player.getAdena();
		int slots = 0;
		
		final ItemContainer warehouse = new PcFreight(_objectId);
		for (final ItemHolder i : _items)
		{
			final ItemInstance item = player.checkItemManipulation(i.getId(), i.getCount(), "freight");
			if (item == null)
			{
				Util.handleIllegalPlayerAction(player, "Error depositing a warehouse object for char " + player.getName(null) + " (validity check)!");
				if (Config.DEBUG)
				{
					_log.warn("Error depositing a warehouse object for char " + player.getName(null) + " (validity check)");
				}
				warehouse.deleteMe();
				return;
			}
			else if (!item.isFreightable())
			{
				warehouse.deleteMe();
				return;
			}
			
			if (item.getId() == PcInventory.ADENA_ID)
			{
				currentAdena -= i.getCount();
			}
			else if (!item.isStackable())
			{
				slots += i.getCount();
			}
			else if (warehouse.getItemByItemId(item.getId()) == null)
			{
				slots++;
			}
		}
		
		if (!warehouse.validateCapacity(slots))
		{
			player.sendPacket(SystemMessageId.YOU_HAVE_EXCEEDED_QUANTITY_THAT_CAN_BE_INPUTTED);
			warehouse.deleteMe();
			return;
		}
		
		if ((currentAdena < fee) || !player.reduceAdena(warehouse.getName(), fee, manager, false))
		{
			player.sendPacket(SystemMessageId.YOU_NOT_ENOUGH_ADENA);
			warehouse.deleteMe();
			return;
		}
		
		final InventoryUpdate playerIU = Config.FORCE_INVENTORY_UPDATE ? null : new InventoryUpdate();
		for (final ItemHolder i : _items)
		{
			final ItemInstance oldItem = player.checkItemManipulation(i.getId(), i.getCount(), "deposit");
			if (oldItem == null)
			{
				_log.warn("Error depositing a warehouse object for char " + player.getName(null) + " (olditem == null)");
				warehouse.deleteMe();
				return;
			}
			
			final ItemInstance newItem = player.getInventory().transferItem("Trade", i.getId(), i.getCount(), warehouse, player, null);
			if (newItem == null)
			{
				_log.warn("Error depositing a warehouse object for char " + player.getName(null) + " (newitem == null)");
				continue;
			}
			
			if (playerIU != null)
			{
				if ((oldItem.getCount() > 0) && (oldItem != newItem))
				{
					playerIU.addModifiedItem(oldItem);
				}
				else
				{
					playerIU.addRemovedItem(oldItem);
				}
			}
		}
		
		warehouse.deleteMe();
		
		if (playerIU != null)
		{
			sendPacket(playerIU);
		}
		else
		{
			player.sendItemList(false);
		}
		player.sendStatusUpdate(false, false, StatusUpdate.CUR_LOAD);
	}
}