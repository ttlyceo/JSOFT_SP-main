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

import java.util.Arrays;
import java.util.List;

import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.data.parser.RecipeParser;
import l2e.gameserver.model.RecipeList;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.ManufactureItemTemplate;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.RecipeShopMsg;
import l2e.gameserver.taskmanager.AttackStanceTaskManager;

public final class RequestRecipeShopListSet extends GameClientPacket
{
	private static final int BATCH_LENGTH = 12;

	private ManufactureItemTemplate[] _items = null;
	
	@Override
	protected void readImpl()
	{
		final int count = readD();
		if ((count <= 0) || (count > Config.MAX_ITEM_IN_PACKET) || ((count * BATCH_LENGTH) != _buf.remaining()))
		{
			return;
		}
		
		_items = new ManufactureItemTemplate[count];
		for (int i = 0; i < count; i++)
		{
			final int id = readD();
			final long cost = readQ();
			if (cost < 0)
			{
				_items = null;
				return;
			}
			_items[i] = new ManufactureItemTemplate(id, cost);
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
		
		if (player.isActionsDisabled() || player.isSitting())
		{
			player.sendActionFailed();
			player.setPrivateStoreType(Player.STORE_PRIVATE_NONE);
			return;
		}

		if (AttackStanceTaskManager.getInstance().hasAttackStanceTask(player) || player.isInDuel())
		{
			player.sendPacket(SystemMessageId.CANT_OPERATE_PRIVATE_STORE_DURING_COMBAT);
			player.sendActionFailed();
			return;
		}
		
		if (!player.canOpenPrivateStore(false, false))
		{
			return;
		}

		final List<RecipeList> dwarfRecipes = Arrays.asList(player.getDwarvenRecipeBook());
		final List<RecipeList> commonRecipes = Arrays.asList(player.getCommonRecipeBook());
		if (player.hasManufactureShop())
		{
			player.getManufactureItems().clear();
		}
		
		for (final ManufactureItemTemplate i : _items)
		{
			final RecipeList list = RecipeParser.getInstance().getRecipeList(i.getRecipeId());
			if (!dwarfRecipes.contains(list) && !commonRecipes.contains(list))
			{
				Util.handleIllegalPlayerAction(player, "" + player.getName(null) + " of account " + player.getAccountName() + " tried to set recipe which he dont have.");
				return;
			}

			if (i.getCost() > MAX_ADENA)
			{
				Util.handleIllegalPlayerAction(player, "" + player.getName(null) + " of account " + player.getAccountName() + " tried to set price more than " + MAX_ADENA + " adena in Private Manufacture.");
				return;
			}

			player.getManufactureItems().put(i.getRecipeId(), i);
		}

		player.setStoreName(!player.hasManufactureShop() ? "" : player.getStoreName());
		player.setPrivateStoreType(Player.STORE_PRIVATE_MANUFACTURE);
		player.sitDown();
		player.saveTradeList();
		player.setIsInStoreNow(true);
		player.broadcastCharInfo();
		player.broadcastPacket(new RecipeShopMsg(player));
	}
}