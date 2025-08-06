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

import java.util.HashSet;
import java.util.Set;

import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.TradeList;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.items.ItemRequest;

public final class SendPrivateStoreBuyList extends GameClientPacket
{
	private static final int BATCH_LENGTH = 20;
	
	private int _storePlayerId;
	private Set<ItemRequest> _items = null;

	@Override
	protected void readImpl()
	{
		_storePlayerId = readD();
		final int count = readD();
		if ((count <= 0) || (count > Config.MAX_ITEM_IN_PACKET) || ((count * BATCH_LENGTH) != _buf.remaining()))
		{
			return;
		}
		_items = new HashSet<>();
		
		for (int i = 0; i < count; i++)
		{
			final int objectId = readD();
			final long cnt = readQ();
			final long price = readQ();
			
			if ((objectId < 1) || (cnt < 1) || (price < 0))
			{
				_items = null;
				return;
			}
			_items.add(new ItemRequest(objectId, cnt, price));
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
		
		final GameObject object = GameObjectsStorage.getPlayer(_storePlayerId);
		if (object == null)
		{
			return;
		}
		
		if (player.isCursedWeaponEquipped())
		{
			return;
		}
		
		final Player storePlayer = (Player) object;
		if (!player.isInsideRadius(storePlayer, INTERACTION_DISTANCE, true, false))
		{
			return;
		}
		
		if ((player.getReflectionId() != storePlayer.getReflectionId()) && (player.getReflectionId() != -1))
		{
			return;
		}
		
		if (!((storePlayer.getPrivateStoreType() == Player.STORE_PRIVATE_SELL) || (storePlayer.getPrivateStoreType() == Player.STORE_PRIVATE_PACKAGE_SELL)))
		{
			return;
		}
		
		final TradeList storeList = storePlayer.getSellList();
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
		
		if (storePlayer.getPrivateStoreType() == Player.STORE_PRIVATE_PACKAGE_SELL)
		{
			if (storeList.getItemCount() > _items.size())
			{
				final String msgErr = "[RequestPrivateStoreBuy] " + getClient().getActiveChar().getName(null) + " tried to buy less items than sold by package-sell, ban this player for bot usage!";
				Util.handleIllegalPlayerAction(getClient().getActiveChar(), msgErr);
				return;
			}
		}
		
		final int result = storeList.privateStoreBuy(player, _items);
		if (result > 0)
		{
			sendActionFailed();
			if (result > 1)
			{
				_log.warn("PrivateStore buy has failed due to invalid list or request. Player: " + player.getName(null) + ", Private store of: " + storePlayer.getName(null));
			}
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