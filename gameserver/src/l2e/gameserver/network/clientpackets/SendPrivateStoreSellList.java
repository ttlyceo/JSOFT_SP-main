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

import static l2e.gameserver.model.actor.Npc.INTERACTION_DISTANCE;

import l2e.gameserver.Config;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.TradeList;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.items.ItemRequest;

public final class SendPrivateStoreSellList extends GameClientPacket
{
	private static final int BATCH_LENGTH = 28;

	private int _storePlayerId;
	private ItemRequest[] _items = null;
	
	@Override
	protected void readImpl()
	{
		_storePlayerId = readD();
		final int count = readD();
		if ((count <= 0) || (count > Config.MAX_ITEM_IN_PACKET) || ((count * BATCH_LENGTH) != _buf.remaining()))
		{
			return;
		}
		_items = new ItemRequest[count];

		for (int i = 0; i < count; i++)
		{
			final int objectId = readD();
			final int itemId = readD();
			readH();
			readH();
			final long cnt = readQ();
			final long price = readQ();

			if ((objectId < 1) || (itemId < 1) || (cnt < 1) || (price < 0))
			{
				_items = null;
				return;
			}
			_items[i] = new ItemRequest(objectId, itemId, cnt, price);
		}
	}

	@Override
	protected void runImpl()
	{
		final Player player = getClient().getActiveChar();
		if (player == null)
		{
			return;
		}

		if (_items == null)
		{
			sendActionFailed();
			return;
		}

		final Player object = GameObjectsStorage.getPlayer(_storePlayerId);
		if (object == null)
		{
			return;
		}

		final Player storePlayer = object;
		if (!player.isInsideRadius(storePlayer, INTERACTION_DISTANCE, true, false))
		{
			return;
		}

		if ((player.getReflectionId() != storePlayer.getReflectionId()) && (player.getReflectionId() != -1))
		{
			return;
		}

		if (storePlayer.getPrivateStoreType() != Player.STORE_PRIVATE_BUY)
		{
			return;
		}

		if (player.isCursedWeaponEquipped())
		{
			return;
		}

		final TradeList storeList = storePlayer.getBuyList();
		if (storeList == null)
		{
			return;
		}

		if (!player.getAccessLevel().allowTransaction())
		{
			player.sendMessage("Transactions are disabled for your Access Level.");
			sendActionFailed();
			return;
		}

		if (!storeList.privateStoreSell(player, _items))
		{
			sendActionFailed();
			_log.warn("PrivateStore sell has failed due to invalid list or request. Player: " + player.getName(null) + ", Private store of: " + storePlayer.getName(null));
			return;
		}

		if (storeList.getItemCount() == 0)
		{
			storePlayer.setPrivateStoreType(Player.STORE_PRIVATE_NONE);
			storePlayer.broadcastCharInfo();
		}
	}

	@Override
	protected boolean triggersOnActionRequest()
	{
		return false;
	}
}