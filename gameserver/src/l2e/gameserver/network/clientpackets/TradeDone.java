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

import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.TradeList;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.network.SystemMessageId;

public final class TradeDone extends GameClientPacket
{
	private int _response;
	
	@Override
	protected void readImpl()
	{
		_response = readD();
	}

	@Override
	protected void runImpl()
	{
		final Player player = getActiveChar();
		if (player == null)
		{
			return;
		}
		
		final TradeList trade = player.getActiveTradeList();
		if (trade == null)
		{
			if (Config.DEBUG)
			{
				_log.warn("player.getTradeList == null in " + getType() + " for player " + player.getName(null));
			}
			return;
		}
		
		if (trade.isLocked())
		{
			return;
		}
		
		if (_response == 1)
		{
			if ((trade.getPartner() == null) || (GameObjectsStorage.getPlayer(trade.getPartner().getObjectId()) == null))
			{
				player.cancelActiveTrade();
				player.sendPacket(SystemMessageId.TARGET_IS_NOT_FOUND_IN_THE_GAME);
				return;
			}
			
			if ((trade.getOwner().getActiveEnchantItemId() != Player.ID_NONE) || (trade.getPartner().getActiveEnchantItemId() != Player.ID_NONE))
			{
				return;
			}
			
			if (!player.getAccessLevel().allowTransaction())
			{
				player.cancelActiveTrade();
				player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
				return;
			}
			
			if ((player.getReflectionId() != trade.getPartner().getReflectionId()) && (player.getReflectionId() != -1))
			{
				player.cancelActiveTrade();
				return;
			}
			
			if (Util.calculateDistance(player, trade.getPartner(), true) > 150)
			{
				player.cancelActiveTrade();
				return;
			}
			
			if (player.isSameAddress(trade.getPartner()) && (player.isUseAutoTrade() && trade.getPartner().isUseAutoTrade()))
			{
				trade.confirm();
				trade.getPartner().getActiveTradeList().confirm();
			}
			else
			{
				trade.confirm();
			}
		}
		else
		{
			player.cancelActiveTrade();
		}
	}
}