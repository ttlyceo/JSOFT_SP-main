package l2e.gameserver.model.entity.events.model.object;

import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.items.itemcontainer.Inventory;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.InventoryUpdate;
import l2e.gameserver.network.serverpackets.SystemMessage;

/**
 * Created by LordWinter
 */
public class CTFCombatFlagObject
{
	private ItemInstance _item;
	private Player _player = null;

	public void spawnObject(Player player)
	{
		if (_item != null)
		{
			return;
		}
		_player = player;
		var wpn = _player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
		if (wpn == null)
		{
			wpn = _player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
		}
		
		if (wpn != null)
		{
			final var unequipped = _player.getInventory().unEquipItemInBodySlotAndRecord(wpn.getItem().getBodyPart());
			final var iu = new InventoryUpdate();
			for (final var unequippedItem : unequipped)
			{
				iu.addModifiedItem(unequippedItem);
			}
			_player.sendInventoryUpdate(iu);
		}
		
		_item = _player.getInventory().addItem("CTFFlag", 9819, 1, _player, null);
		if (_item == null)
		{
			return;
		}
		
		_player.getInventory().equipItem(_item);
		
		final var u = new InventoryUpdate();
		u.addItem(_item);
		player.sendInventoryUpdate(u);
		
		final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_EQUIPPED);
		sm.addItemName(_item);
		_player.sendPacket(sm);
		
		_player.broadcastUserInfo(true);
		_player.setCombatFlagEquipped(true);
	}

	public void despawnObject()
	{
		if (_item == null || _player == null)
		{
			return;
		}

		_player.setCombatFlagEquipped(false);
		final var unequipped = _player.getInventory().unEquipItemInBodySlotAndRecord(_item.getItem().getBodyPart());
		final var iu = new InventoryUpdate();
		for (final var unequippedItem : unequipped)
		{
			iu.addModifiedItem(unequippedItem);
		}
		_player.sendInventoryUpdate(iu);
		
		final var destroyItem = _player.getInventory().destroyItem("CTFFlag", _item, _player, null);
		if (destroyItem != null)
		{
			final var u = new InventoryUpdate();
			u.addRemovedItem(destroyItem);
			_player.sendInventoryUpdate(u);
		}
		
		checkFlag(_player);
		_player.broadcastUserInfo(true);
		
		_item = null;
		_player = null;
	}

	private void checkFlag(Player player)
	{
		final var flag = player.getInventory().getItemByItemId(9819);
		if (flag != null)
		{
			final var unequipped = player.getInventory().unEquipItemInBodySlotAndRecord(flag.getItem().getBodyPart());
			final var iu = new InventoryUpdate();
			for (final var unequippedItem : unequipped)
			{
				iu.addModifiedItem(unequippedItem);
			}
			player.sendInventoryUpdate(iu);
			
			final var destroyItem = player.getInventory().destroyItem("CTFFlag", flag, player, null);
			if (destroyItem != null)
			{
				final var u = new InventoryUpdate();
				u.addRemovedItem(destroyItem);
				player.sendInventoryUpdate(u);
			}
		}
	}
}