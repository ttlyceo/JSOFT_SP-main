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
package l2e.gameserver.handler.bypasshandlers.impl;

import java.util.StringTokenizer;

import l2e.gameserver.Config;
import l2e.gameserver.data.parser.BuyListParser;
import l2e.gameserver.handler.bypasshandlers.IBypassHandler;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.items.buylist.ProductList;
import l2e.gameserver.network.serverpackets.ShopPreviewList;

public class Wear implements IBypassHandler
{
	private static final String[] COMMANDS =
	{
	        "Wear"
	};
	
	@Override
	public boolean useBypass(String command, Player activeChar, Creature target)
	{
		if (!target.isNpc())
		{
			return false;
		}

		if (!Config.ALLOW_WEAR)
		{
			return false;
		}

		try
		{
			final StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken();

			if (st.countTokens() < 1)
			{
				return false;
			}

			showWearWindow(activeChar, Integer.parseInt(st.nextToken()));
			return true;
		}
		catch (final Exception e)
		{
			_log.warn("Exception in " + getClass().getSimpleName(), e);
		}
		return false;
	}

	private static final void showWearWindow(Player player, int val)
	{
		final ProductList buyList = BuyListParser.getInstance().getBuyList(val);
		if (buyList == null)
		{
			_log.warn("BuyList not found! BuyListId:" + val);
			player.sendActionFailed();
			return;
		}
		player.setInventoryBlockingStatus(true);
		player.sendPacket(new ShopPreviewList(buyList, player.getAdena(), player.getExpertiseLevel()));
		if (player.isGM())
		{
			player.sendMessage("BuyList: " + val + ".xml ");
		}
	}

	@Override
	public String[] getBypassList()
	{
		return COMMANDS;
	}
}