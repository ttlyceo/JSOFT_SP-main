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
import l2e.gameserver.model.items.buylist.Product;
import l2e.gameserver.model.items.buylist.ProductList;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.ExBuySellList;
import l2e.gameserver.network.serverpackets.StatusUpdate;
import l2e.gameserver.network.serverpackets.SystemMessage;

public final class RequestBuyItem extends GameClientPacket
{
	private static final int BATCH_LENGTH = 12;
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
			final int itemId = readD();
			final long count = readQ();
			if ((itemId < 1) || (count < 1))
			{
				_items = null;
				return;
			}
			_items.add(new ItemHolder(itemId, count));
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
		
		if (player.isActionsDisabled())
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
			if ((target == null) || (!player.isInsideRadius(target, INTERACTION_DISTANCE, true, false)) || (player.getReflectionId() != target.getReflectionId()))
			{
				sendActionFailed();
				return;
			}
			if (target instanceof MerchantInstance)
			{
				merchant = (Creature) target;
			}
			else
			{
				sendActionFailed();
				return;
			}
		}
		
		double castleTaxRate = 0;
		double baseTaxRate = 0;
		
		if ((merchant == null) && !player.isGM())
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
		
		if (merchant != null)
		{
			if (merchant instanceof MerchantInstance)
			{
				if (!buyList.isNpcAllowed(((MerchantInstance) merchant).getId()))
				{
					sendActionFailed();
					return;
				}
				castleTaxRate = ((MerchantInstance) merchant).getMpc().getCastleTaxRate();
				baseTaxRate = ((MerchantInstance) merchant).getMpc().getBaseTaxRate();
			}
			else
			{
				baseTaxRate = 50;
			}
		}
		
		long subTotal = 0;
		
		long slots = 0;
		long weight = 0;
		for (final ItemHolder i : _items)
		{
			long price = -1;
			
			final Product product = buyList.getProductByItemId(i.getId());
			if (product == null)
			{
				Util.handleIllegalPlayerAction(player, "" + player.getName(null) + " of account " + player.getAccountName() + " sent a false BuyList list_id " + _listId + " and item_id " + i.getId());
				return;
			}
			
			if (!product.getItem().isStackable() && (i.getCount() > 1))
			{
				Util.handleIllegalPlayerAction(player, "" + player.getName(null) + " of account " + player.getAccountName() + " tried to purchase invalid quantity of items at the same time.");
				SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.YOU_HAVE_EXCEEDED_QUANTITY_THAT_CAN_BE_INPUTTED);
				sendPacket(sm);
				sm = null;
				return;
			}
			
			price = product.getPrice();
			if ((product.getId() >= 3960) && (product.getId() <= 4026))
			{
				price *= Config.RATE_SIEGE_GUARDS_PRICE;
			}
			
			if (price < 0)
			{
				_log.warn("ERROR, no price found .. wrong buylist ??");
				sendActionFailed();
				return;
			}
			
			if ((price == 0) && !player.isGM() && Config.ONLY_GM_ITEMS_FREE)
			{
				player.sendMessage("Ohh Cheat dont work? You have a problem now!");
				Util.handleIllegalPlayerAction(player, "" + player.getName(null) + " of account " + player.getAccountName() + " tried buy item for 0 adena.");
				return;
			}
			
			if (product.hasLimitedStock())
			{
				if (i.getCount() > product.getCount())
				{
					sendActionFailed();
					return;
				}
			}
			
			if ((MAX_ADENA / i.getCount()) < price)
			{
				Util.handleIllegalPlayerAction(player, "" + player.getName(null) + " of account " + player.getAccountName() + " tried to purchase over " + MAX_ADENA + " adena worth of goods.");
				return;
			}
			price = (long) (price * (1 + castleTaxRate + baseTaxRate));
			subTotal += i.getCount() * price;
			if (subTotal > MAX_ADENA)
			{
				Util.handleIllegalPlayerAction(player, "" + player.getName(null) + " of account " + player.getAccountName() + " tried to purchase over " + MAX_ADENA + " adena worth of goods.");
				return;
			}
			
			weight += i.getCount() * product.getItem().getWeight();
			if (player.getInventory().getItemByItemId(product.getId()) == null)
			{
				slots++;
			}
		}
		
		if (!player.isGM() && ((weight > Integer.MAX_VALUE) || (weight < 0) || !player.getInventory().validateWeight((int) weight)))
		{
			player.sendPacket(SystemMessageId.WEIGHT_LIMIT_EXCEEDED);
			sendActionFailed();
			return;
		}
		
		if (!player.isGM() && ((slots > Integer.MAX_VALUE) || (slots < 0) || !player.getInventory().validateCapacity((int) slots)))
		{
			player.sendPacket(SystemMessageId.SLOTS_FULL);
			sendActionFailed();
			return;
		}
		
		if ((subTotal < 0) || !player.reduceAdena("Buy", subTotal, player.getLastFolkNPC(), false))
		{
			player.sendPacket(SystemMessageId.YOU_NOT_ENOUGH_ADENA);
			sendActionFailed();
			return;
		}
		
		for (final ItemHolder i : _items)
		{
			final Product product = buyList.getProductByItemId(i.getId());
			if (product == null)
			{
				Util.handleIllegalPlayerAction(player, "" + player.getName(null) + " of account " + player.getAccountName() + " sent a false BuyList list_id " + _listId + " and item_id " + i.getId());
				continue;
			}
			
			if (product.hasLimitedStock())
			{
				if (product.decreaseCount(i.getCount()))
				{
					player.getInventory().addItem("Buy", i.getId(), i.getCount(), player, merchant);
				}
			}
			else
			{
				player.getInventory().addItem("Buy", i.getId(), i.getCount(), player, merchant);
			}
		}
		
		if (merchant instanceof MerchantInstance)
		{
			((MerchantInstance) merchant).getCastle().addToTreasury((long) (subTotal * castleTaxRate));
		}
		
		player.sendStatusUpdate(false, false, StatusUpdate.CUR_LOAD);
		player.sendPacket(new ExBuySellList(player, true));
	}
}