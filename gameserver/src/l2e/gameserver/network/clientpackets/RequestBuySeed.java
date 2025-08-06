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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.data.parser.ItemsParser;
import l2e.gameserver.instancemanager.CastleManager;
import l2e.gameserver.instancemanager.CastleManorManager;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.ManorManagerInstance;
import l2e.gameserver.model.actor.templates.SeedTemplate;
import l2e.gameserver.model.actor.templates.items.Item;
import l2e.gameserver.model.entity.Castle;
import l2e.gameserver.model.holders.ItemHolder;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.SystemMessage;

public class RequestBuySeed extends GameClientPacket
{
	private static final int BATCH_LENGTH = 12;

	private int _manorId;
	private List<ItemHolder> _items = null;

	@Override
	protected void readImpl()
	{
		_manorId = readD();
		
		final int count = readD();
		if ((count <= 0) || (count > Config.MAX_ITEM_IN_PACKET) || ((count * BATCH_LENGTH) != _buf.remaining()))
		{
			return;
		}
		
		_items = new ArrayList<>(count);
		for (int i = 0; i < count; i++)
		{
			final int itemId = readD();
			final long cnt = readQ();
			if ((cnt < 1) || (itemId < 1))
			{
				_items = null;
				return;
			}
			_items.add(new ItemHolder(itemId, cnt));
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
			sendActionFailed();
			return;
		}
		
		if (_items == null)
		{
			sendActionFailed();
			return;
		}
		
		GameObject manager = player.getTarget();
		
		if (!(manager instanceof ManorManagerInstance))
		{
			manager = player.getLastFolkNPC();
		}
		
		if (!(manager instanceof ManorManagerInstance))
		{
			return;
		}
		
		if (!player.isInsideRadius(manager, INTERACTION_DISTANCE, true, false))
		{
			return;
		}
		
		final Castle castle = CastleManager.getInstance().getCastleById(_manorId);
		
		long totalPrice = 0;
		int slots = 0;
		int totalWeight = 0;
		
		final Map<Integer, SeedTemplate> _productInfo = new HashMap<>();
		for (final ItemHolder ih : _items)
		{
			final SeedTemplate sp = CastleManorManager.getInstance().getSeedProduct(_manorId, ih.getId(), false);
			if ((sp == null) || (sp.getPrice() <= 0) || (sp.getAmount() < ih.getCount()) || ((MAX_ADENA / ih.getCount()) < sp.getPrice()))
			{
				sendActionFailed();
				return;
			}
			
			totalPrice += (sp.getPrice() * ih.getCount());
			
			if (totalPrice > MAX_ADENA)
			{
				Util.handleIllegalPlayerAction(player, "" + player.getName(null) + " of account " + player.getAccountName() + " tried to purchase over " + MAX_ADENA + " adena worth of goods.");
				return;
			}
			
			final Item template = ItemsParser.getInstance().getTemplate(ih.getId());
			totalWeight += ih.getCount() * template.getWeight();
			if (!template.isStackable())
			{
				slots += ih.getCount();
			}
			else if (player.getInventory().getItemByItemId(ih.getId()) == null)
			{
				slots++;
			}
			_productInfo.put(ih.getId(), sp);
		}
		
		if (!player.getInventory().validateWeight(totalWeight))
		{
			player.sendPacket(SystemMessageId.WEIGHT_LIMIT_EXCEEDED);
			return;
		}
		
		if (!player.getInventory().validateCapacity(slots))
		{
			player.sendPacket(SystemMessageId.SLOTS_FULL);
			return;
		}
		
		if ((totalPrice < 0) || (player.getAdena() < totalPrice))
		{
			player.sendPacket(SystemMessageId.YOU_NOT_ENOUGH_ADENA);
			return;
		}
		
		for (final ItemHolder i : _items)
		{
			final SeedTemplate sp = _productInfo.get(i.getId());
			if (sp == null)
			{
				continue;
			}
			
			final long price = sp.getPrice() * i.getCount();
			if (!sp.decreaseAmount(i.getCount()) || !player.reduceAdena("Buy", price, player, false))
			{
				totalPrice -= price;
				continue;
			}
			player.addItem("Buy", i.getId(), i.getCount(), manager, true);
		}
		
		if (totalPrice > 0)
		{
			castle.addToTreasuryNoTax(totalPrice);
			final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_DISAPPEARED_ADENA);
			sm.addItemNumber(totalPrice);
			player.sendPacket(sm);
			
			if (Config.ALT_MANOR_SAVE_ALL_ACTIONS)
			{
				CastleManorManager.getInstance().updateCurrentProduction(_manorId, _productInfo.values());
			}
		}
	}
}