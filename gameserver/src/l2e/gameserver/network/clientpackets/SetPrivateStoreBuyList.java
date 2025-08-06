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
import l2e.gameserver.model.TradeList;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.PrivateStoreBuyManageList;
import l2e.gameserver.network.serverpackets.PrivateStoreBuyMsg;
import l2e.gameserver.taskmanager.AttackStanceTaskManager;

public final class SetPrivateStoreBuyList extends GameClientPacket
{
	private static final int BATCH_LENGTH = 40;
	
	private Item[] _items = null;

	@Override
	protected void readImpl()
	{
		final int count = readD();
		if ((count < 1) || (count > Config.MAX_ITEM_IN_PACKET) || ((count * BATCH_LENGTH) != _buf.remaining()))
		{
			return;
		}
		
		_items = new Item[count];
		for (int i = 0; i < count; i++)
		{
			final int itemId = readD();
			final int enchant = readD();
			final long cnt = readQ();
			final long price = readQ();
			
			if ((itemId < 1) || (cnt < 1) || (price < 0))
			{
				_items = null;
				return;
			}
			final int elemAtkType = readH();
			final int elemAtkPower = readH();
			final int[] elemDefAttr =
			{
			        0, 0, 0, 0, 0, 0
			};
			
			for (byte e = 0; e < 6; e++)
			{
				elemDefAttr[e] = readH();
			}
			_items[i] = new Item(itemId, enchant, cnt, price, elemAtkType, elemAtkPower, elemDefAttr);
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
			player.setPrivateStoreType(Player.STORE_PRIVATE_NONE);
			player.broadcastCharInfo();
			return;
		}
		
		if (!player.getAccessLevel().allowTransaction())
		{
			player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
			return;
		}
		
		if (player.isActionsDisabled() || player.isSitting())
		{
			player.sendActionFailed();
			player.setPrivateStoreType(Player.STORE_PRIVATE_NONE);
			return;
		}
		
		if (AttackStanceTaskManager.getInstance().hasAttackStanceTask(player) || player.isInDuel())
		{
			player.sendPacket(SystemMessageId.CANT_OPERATE_PRIVATE_STORE_DURING_COMBAT);
			player.sendPacket(new PrivateStoreBuyManageList(player));
			player.sendActionFailed();
			return;
		}
		
		if (!player.canOpenPrivateStore(true, false))
		{
			player.sendPacket(new PrivateStoreBuyManageList(player));
			return;
		}
		
		final TradeList tradeList = player.getBuyList();
		tradeList.clear();
		
		if (_items.length > player.getPrivateBuyStoreLimit())
		{
			player.sendPacket(new PrivateStoreBuyManageList(player));
			player.sendPacket(SystemMessageId.YOU_HAVE_EXCEEDED_QUANTITY_THAT_CAN_BE_INPUTTED);
			return;
		}
		
		long totalCost = 0;
		for (final Item i : _items)
		{
			if (!i.addToTradeList(tradeList))
			{
				Util.handleIllegalPlayerAction(player, "" + player.getName(null) + " of account " + player.getAccountName() + " tried to set price more than " + MAX_ADENA + " adena in Private Store - Buy.");
				return;
			}
			
			totalCost += i.getCost();
			if (totalCost > MAX_ADENA)
			{
				Util.handleIllegalPlayerAction(player, "" + player.getName(null) + " of account " + player.getAccountName() + " tried to set total price more than " + MAX_ADENA + " adena in Private Store - Buy.");
				return;
			}
		}
		
		if (totalCost > player.getAdena())
		{
			player.sendPacket(new PrivateStoreBuyManageList(player));
			player.sendPacket(SystemMessageId.THE_PURCHASE_PRICE_IS_HIGHER_THAN_MONEY);
			return;
		}
		player.sitDown();
		player.setPrivateStoreType(Player.STORE_PRIVATE_BUY);
		player.saveTradeList();
		player.setIsInStoreNow(true);
		player.broadcastCharInfo();
		player.broadcastPacket(new PrivateStoreBuyMsg(player));
	}
	
	private static class Item
	{
		private final int _itemId;
		private final int _enchant;
		private final long _count;
		private final long _price;
		private final int _elemAtkType;
		private final int _elemAtkPower;
		private final int[] _elemDefAttr;
		
		public Item(int id, int enchant, long num, long pri, int elemAtkType, int elemAtkPower, int[] elemDefAttr)
		{
			_itemId = id;
			_enchant = enchant;
			_count = num;
			_price = pri;
			_elemAtkType = elemAtkType;
			_elemAtkPower = elemAtkPower;
			_elemDefAttr = elemDefAttr;
		}
		
		public boolean addToTradeList(TradeList list)
		{
			if ((MAX_ADENA / _count) < _price)
			{
				return false;
			}
			
			list.addItemByItemId(_itemId, _enchant, _count, _price, null, -1, -9999, _elemAtkType, _elemAtkPower, _elemDefAttr);
			return true;
		}
		
		public long getCost()
		{
			return _count * _price;
		}
	}
}