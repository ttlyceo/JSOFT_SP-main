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
import static l2e.gameserver.model.items.itemcontainer.PcInventory.MAX_ADENA;

import java.util.ArrayList;
import java.util.List;

import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.data.parser.BuyListParser;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.MerchantInstance;
import l2e.gameserver.model.holders.ItemHolder;
import l2e.gameserver.model.items.buylist.ProductList;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.network.serverpackets.ExBuySellList;
import l2e.gameserver.network.serverpackets.StatusUpdate;

public final class RequestSellItem extends GameClientPacket
{
	private static final int BATCH_LENGTH = 16;

	private int _listId;
	private List<ItemHolder> _items = null;

	@Override
	protected void readImpl()
	{
		_listId = readD();
		final int size = readD();
		if ((size <= 0) || (size > Config.MAX_ITEM_IN_PACKET) || ((size * BATCH_LENGTH) != _buf.remaining()))
		{
			return;
		}

		_items = new ArrayList<>(size);
		for (int i = 0; i < size; i++)
		{
			final int objectId = readD();
			final int itemId = readD();
			final long count = readQ();
			if ((objectId < 1) || (itemId < 1) || (count < 1))
			{
				_items = null;
				return;
			}
			_items.add(new ItemHolder(itemId, objectId, count));
		}
	}

	@Override
	protected void runImpl()
	{
		processSell();
	}

	protected void processSell()
	{
		final Player player = getClient().getActiveChar();

		if (player == null)
		{
			return;
		}

		if (player.isActionsDisabled() || player.getActiveTradeList() != null)
		{
			player.sendActionFailed();
			return;
		}

		if (_items == null)
		{
			sendActionFailed();
			return;
		}

		if (!Config.ALT_GAME_KARMA_PLAYER_CAN_SHOP && (player.getKarma() > 0))
		{
			sendActionFailed();
			return;
		}

		final GameObject target = player.getTarget();
		Creature merchant = null;
		if (!player.isGM())
		{
			if ((target != null) && (target instanceof MerchantInstance))
			{
				merchant = (Creature) target;
			}
		}

		if (merchant != null)
		{
			if ((!player.isInsideRadius(target, INTERACTION_DISTANCE, true, false)) || (player.getReflectionId() != target.getReflectionId()))
			{
				sendActionFailed();
				return;
			}
		}

		final ProductList buyList = BuyListParser.getInstance().getBuyList(_listId);
		if (buyList == null)
		{
			Util.handleIllegalPlayerAction(player, "" + player.getName(null) + " of account " + player.getAccountName() + " sent a false BuyList list_id " + _listId);
			return;
		}

		if (merchant != null)
		{
			if (merchant instanceof MerchantInstance)
			{
				if (!buyList.isNpcAllowed(((MerchantInstance) merchant).getId()))
				{
					sendActionFailed();
					return;
				}
			}
		}

		long totalPrice = 0;

		for (final ItemHolder i : _items)
		{
			ItemInstance item = player.checkItemManipulation(i.getObjectId(), i.getCount(), "sell");
			if ((item == null) || (!item.isSellable()))
			{
				continue;
			}

			final long price = (long) ((item.getReferencePrice() / 2) * Config.SELL_PRICE_MODIFIER);
			totalPrice += price * i.getCount();
			if (((MAX_ADENA / i.getCount()) < price) || (totalPrice > MAX_ADENA))
			{
				Util.handleIllegalPlayerAction(player, "" + player.getName(null) + " of account " + player.getAccountName() + " tried to purchase over " + MAX_ADENA + " adena worth of goods.");
				return;
			}

			if (Config.ALLOW_REFUND)
			{
				item = player.getInventory().transferItem("Sell", i.getObjectId(), i.getCount(), player.getRefund(), player, merchant);
			}
			else
			{
				item = player.getInventory().destroyItem("Sell", i.getObjectId(), i.getCount(), player, merchant);
			}
		}
		player.addAdena("Sell", totalPrice, merchant, false);

		player.sendStatusUpdate(false, false, StatusUpdate.CUR_LOAD);
		player.sendPacket(new ExBuySellList(player, true));
	}
}