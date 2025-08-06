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

import l2e.gameserver.Config;
import l2e.gameserver.data.parser.ItemsParser;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.InventoryUpdate;
import l2e.gameserver.network.serverpackets.SystemMessage;

public class CombatFlag
{
	private Player _player = null;
	private int _playerId = 0;
	private ItemInstance _item = null;
	private ItemInstance _itemInstance;
	private final Location _location;
	private final int _itemId;
	protected final int _fortId;

	public CombatFlag(int fort_id, int x, int y, int z, int heading, int item_id)
	{
		_fortId = fort_id;
		_location = new Location(x, y, z, heading);
		_itemId = item_id;
	}
	
	public synchronized void spawnMe()
	{
		_itemInstance = ItemsParser.getInstance().createItem("Combat", _itemId, 1, null, null);
		_itemInstance.dropMe(null, _location.getX(), _location.getY(), _location.getZ(), true);
	}
	
	public synchronized void unSpawnMe()
	{
		if (_player != null)
		{
			dropIt();
		}
		if (_itemInstance != null)
		{
			_itemInstance.decayMe();
		}
	}
	
	public boolean activate(Player player, ItemInstance item)
	{
		if (player.isMounted())
		{
			player.sendPacket(SystemMessageId.CANNOT_EQUIP_ITEM_DUE_TO_BAD_CONDITION);
			return false;
		}
		_player = player;
		_playerId = _player.getObjectId();
		_itemInstance = null;
		
		_item = item;
		_player.getInventory().equipItem(_item);
		final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_EQUIPPED);
		sm.addItemName(_item);
		_player.sendPacket(sm);
		
		if (!Config.FORCE_INVENTORY_UPDATE)
		{
			final InventoryUpdate iu = new InventoryUpdate();
			iu.addItem(_item);
			_player.sendPacket(iu);
		}
		else
		{
			_player.sendItemList(false);
		}
		_player.broadcastUserInfo(true);
		_player.setCombatFlagEquipped(true);
		return true;
	}
	
	public void dropIt()
	{
		_player.setCombatFlagEquipped(false);
		final int slot = _player.getInventory().getSlotFromItem(_item);
		_player.getInventory().unEquipItemInBodySlot(slot);
		_player.destroyItem("CombatFlag", _item, null, true);
		_item = null;
		_player.broadcastUserInfo(true);
		_player = null;
		_playerId = 0;
	}
	
	public int getPlayerObjectId()
	{
		return _playerId;
	}
	
	public ItemInstance getCombatFlagInstance()
	{
		return _itemInstance;
	}
}