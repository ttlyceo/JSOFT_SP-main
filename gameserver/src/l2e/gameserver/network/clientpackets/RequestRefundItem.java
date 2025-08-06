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

import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.data.parser.BuyListParser;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.MerchantInstance;
import l2e.gameserver.model.actor.templates.items.Item;
import l2e.gameserver.model.items.buylist.ProductList;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.ExBuySellList;
import l2e.gameserver.network.serverpackets.StatusUpdate;

public final class RequestRefundItem extends GameClientPacket
{
	private static final int BATCH_LENGTH = 4;

	private int _listId;
	private int[] _items = null;

	@Override
	protected void readImpl()
	{
		_listId = readD();
		final int count = readD();
		if ((count <= 0) || (count > Config.MAX_ITEM_IN_PACKET) || ((count * BATCH_LENGTH) != _buf.remaining()))
		{
			return;
		}

		_items = new int[count];
		for (int i = 0; i < count; i++)
		{
			_items[i] = readD();
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

		if (player.isActionsDisabled())
		{
			player.sendActionFailed();
			return;
		}

		if (_items == null)
		{
			sendActionFailed();
			return;
		}

		if (!player.hasRefund())
		{
			sendActionFailed();
			return;
		}
		
		if (player.getActiveTradeList() != null)
		{
			player.cancelActiveTrade();
		}

		final GameObject target = player.getTarget();
		if (!player.isGM() && ((target == null) || !(target instanceof MerchantInstance) || (player.getReflectionId() != target.getReflectionId()) || !player.isInsideRadius(target, INTERACTION_DISTANCE, true, false)))
		{
			sendActionFailed();
			return;
		}

		Creature merchant = null;
		if (target instanceof MerchantInstance)
		{
			merchant = (Creature) target;
		}
		else if (!player.isGM())
		{
			sendActionFailed();
			return;
		}

		if (merchant == null)
		{
			sendActionFailed();
			return;
		}

		final ProductList buyList = BuyListParser.getInstance().getBuyList(_listId);
		if (buyList == null)
		{
			Util.handleIllegalPlayerAction(player, "" + player.getName(null) + " of account " + player.getAccountName() + " sent a false BuyList list_id " + _listId);
			return;
		}

		if (!buyList.isNpcAllowed(((MerchantInstance) merchant).getId()))
		{
			sendActionFailed();
			return;
		}

		long weight = 0;
		long adena = 0;
		long slots = 0;

		final ItemInstance[] refund = player.getRefund().getItems();
		final int[] objectIds = new int[_items.length];

		for (int i = 0; i < _items.length; i++)
		{
			final int idx = _items[i];
			if ((idx < 0) || (idx >= refund.length))
			{
				Util.handleIllegalPlayerAction(player, "" + player.getName(null) + " of account " + player.getAccountName() + " sent invalid refund index");
				return;
			}

			for (int j = i + 1; j < _items.length; j++)
			{
				if (idx == _items[j])
				{
					Util.handleIllegalPlayerAction(player, "" + player.getName(null) + " of account " + player.getAccountName() + " sent duplicate refund index");
					return;
				}
			}

			final ItemInstance item = refund[idx];
			final Item template = item.getItem();
			objectIds[i] = item.getObjectId();

			for (int j = 0; j < i; j++)
			{
				if (objectIds[i] == objectIds[j])
				{
					Util.handleIllegalPlayerAction(player, "" + player.getName(null) + " of account " + player.getAccountName() + " has duplicate items in refund list");
					return;
				}
			}

			final long count = item.getCount();
			weight += count * template.getWeight();
			adena += (count * template.getReferencePrice()) / 2;
			if (!template.isStackable())
			{
				slots += count;
			}
			else if (player.getInventory().getItemByItemId(template.getId()) == null)
			{
				slots++;
			}
		}

		if ((weight > Integer.MAX_VALUE) || (weight < 0) || !player.getInventory().validateWeight((int) weight))
		{
			player.sendPacket(SystemMessageId.WEIGHT_LIMIT_EXCEEDED);
			sendActionFailed();
			return;
		}

		if ((slots > Integer.MAX_VALUE) || (slots < 0) || !player.getInventory().validateCapacity((int) slots))
		{
			player.sendPacket(SystemMessageId.SLOTS_FULL);
			sendActionFailed();
			return;
		}

		if ((adena < 0) || !player.reduceAdena("Refund", adena, player.getLastFolkNPC(), false))
		{
			player.sendPacket(SystemMessageId.YOU_NOT_ENOUGH_ADENA);
			sendActionFailed();
			return;
		}

		for (int i = 0; i < _items.length; i++)
		{
			final ItemInstance item = player.getRefund().transferItem("Refund", objectIds[i], Long.MAX_VALUE, player.getInventory(), player, player.getLastFolkNPC());
			if (item == null)
			{
				_log.warn("Error refunding object for char " + player.getName(null) + " (newitem == null)");
				continue;
			}
		}
		player.sendStatusUpdate(false, false, StatusUpdate.CUR_LOAD);
		player.sendPacket(new ExBuySellList(player, true));
	}
}