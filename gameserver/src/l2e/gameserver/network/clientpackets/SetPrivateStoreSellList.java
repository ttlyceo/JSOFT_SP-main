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

import static l2e.gameserver.model.items.itemcontainer.PcInventory.MAX_ADENA;

import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.model.TradeItem;
import l2e.gameserver.model.TradeList;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.entity.auction.Auction;
import l2e.gameserver.model.entity.auction.AuctionsManager;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.ExPrivateStorePackageMsg;
import l2e.gameserver.network.serverpackets.PrivateStoreSellManageList;
import l2e.gameserver.network.serverpackets.PrivateStoreSellMsg;
import l2e.gameserver.taskmanager.AttackStanceTaskManager;

public class SetPrivateStoreSellList extends GameClientPacket
{
	private static final int BATCH_LENGTH = 20;

	private boolean _packageSale;
	private Item[] _items = null;
	
	@Override
	protected void readImpl()
	{
		_packageSale = (readD() == 1);
		final int count = readD();
		if ((count < 1) || (count > Config.MAX_ITEM_IN_PACKET) || ((count * BATCH_LENGTH) != _buf.remaining()))
		{
			return;
		}

		_items = new Item[count];
		for (int i = 0; i < count; i++)
		{
			final int itemId = readD();
			final long cnt = readQ();
			final long price = readQ();

			if ((itemId < 1) || (cnt < 1) || (price < 0))
			{
				_items = null;
				return;
			}
			_items[i] = new Item(itemId, cnt, price);
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
			player.sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT);
			player.setPrivateStoreType(Player.STORE_PRIVATE_NONE);
			player.broadcastCharInfo();
			return;
		}
		
		if (player.isActionsDisabled() || player.isSitting())
		{
			player.sendActionFailed();
			player.setPrivateStoreType(Player.STORE_PRIVATE_NONE);
			return;
		}

		if (!player.getAccessLevel().allowTransaction())
		{
			player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
			return;
		}

		if (AttackStanceTaskManager.getInstance().hasAttackStanceTask(player) || player.isInDuel())
		{
			player.sendPacket(SystemMessageId.CANT_OPERATE_PRIVATE_STORE_DURING_COMBAT);
			player.sendPacket(new PrivateStoreSellManageList(player, _packageSale));
			player.sendActionFailed();
			return;
		}
		
		if (!player.canOpenPrivateStore(true, false))
		{
			player.sendPacket(new PrivateStoreSellManageList(player, _packageSale));
			return;
		}
		
		if (_items.length > player.getPrivateSellStoreLimit())
		{
			player.sendPacket(new PrivateStoreSellManageList(player, _packageSale));
			player.sendPacket(SystemMessageId.YOU_HAVE_EXCEEDED_QUANTITY_THAT_CAN_BE_INPUTTED);
			return;
		}

		final TradeList tradeList = player.getSellList();
		tradeList.clear();
		tradeList.setPackaged(_packageSale);

		long totalCost = player.getAdena();
		for (final Item i : _items)
		{
			if (!i.addToTradeList(tradeList))
			{
				Util.handleIllegalPlayerAction(player, "" + player.getName(null) + " of account " + player.getAccountName() + " tried to set price more than " + MAX_ADENA + " adena in Private Store - Sell.");
				return;
			}

			totalCost += i.getPrice();
			if (totalCost > MAX_ADENA)
			{
				Util.handleIllegalPlayerAction(player, "" + player.getName(null) + " of account " + player.getAccountName() + " tried to set total price more than " + MAX_ADENA + " adena in Private Store - Sell.");
				return;
			}
		}

		player.sitDown();
		if (_packageSale)
		{
			player.setPrivateStoreType(Player.STORE_PRIVATE_PACKAGE_SELL);
		}
		else
		{
			player.setPrivateStoreType(Player.STORE_PRIVATE_SELL);
		}
		player.saveTradeList();
		player.setIsInStoreNow(true);
		player.broadcastCharInfo();
		
		if (Config.AUCTION_PRIVATE_STORE_AUTO_ADDED && !_packageSale)
		{
			for (final TradeItem ti : player.getSellList().getItems())
			{
				final ItemInstance item = player.getInventory().getItemByObjectId(ti.getObjectId());
				if (item == null)
				{
					continue;
				}
				final Auction auc = AuctionsManager.getInstance().addNewStore(player, item, 57, ti.getPrice(), ti.getCount());
				ti.setAuctionId(auc.getAuctionId());
			}
		}

		if (_packageSale)
		{
			player.broadcastPacket(new ExPrivateStorePackageMsg(player));
		}
		else
		{
			player.broadcastPacket(new PrivateStoreSellMsg(player));
		}
	}

	private static class Item
	{
		private final int _itemId;
		private final long _count;
		private final long _price;

		public Item(int id, long num, long pri)
		{
			_itemId = id;
			_count = num;
			_price = pri;
		}

		public boolean addToTradeList(TradeList list)
		{
			if ((MAX_ADENA / _count) < _price)
			{
				return false;
			}

			list.addItem(_itemId, _count, _price);
			return true;
		}

		public long getPrice()
		{
			return _count * _price;
		}
	}
}