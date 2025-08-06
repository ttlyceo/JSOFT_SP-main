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

import l2e.gameserver.data.parser.ProductItemParser;
import l2e.gameserver.model.actor.Player;

/**
 * Created by LordWinter 08.15.2013 Fixed by L2J Eternity-World
 */
public class RequestBrRecentProductList extends GameClientPacket
{
	@Override
	public void readImpl()
	{
	}
	
	@Override
	public void runImpl()
	{
		final Player player = getClient().getActiveChar();
		if (player == null)
		{
			return;
		}
		ProductItemParser.getInstance().recentProductList(player);
	}
}