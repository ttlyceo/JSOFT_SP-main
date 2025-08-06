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
package l2e.gameserver.handler.itemhandlers.impl;


import l2e.gameserver.Config;
import l2e.gameserver.data.dao.CharacterPremiumDAO;
import l2e.gameserver.data.parser.PremiumAccountsParser;
import l2e.gameserver.handler.itemhandlers.IItemHandler;
import l2e.gameserver.model.actor.Playable;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.service.premium.PremiumTemplate;
import l2e.gameserver.model.strings.server.ServerMessage;
import l2e.gameserver.network.SystemMessageId;

public class Premium implements IItemHandler
{
	@Override
	public boolean useItem(Playable playable, ItemInstance item, boolean forceUse)
	{
		if (!playable.isPlayer())
		{
			playable.sendPacket(SystemMessageId.ITEM_NOT_FOR_PETS);
			return false;
		}
		
		if (!Config.USE_PREMIUMSERVICE)
		{
			return false;
		}
		
		final int id = item.getItem().getPremiumId();
		final Player player = playable.getActingPlayer();
		if (player != null && id >= 0)
		{
			if (player.hasPremiumBonus() && !Config.PREMIUMSERVICE_DOUBLE)
			{
				player.sendMessage((new ServerMessage("ServiceBBS.PREMIUM_MSG", player.getLang())).toString());
				return false;
			}
			
			final PremiumTemplate template = PremiumAccountsParser.getInstance().getPremiumTemplate(id);
			if (template != null)
			{
				if (!player.destroyItem("Premium", item.getObjectId(), 1, player, true))
				{
					return false;
				}
				
				final long time = !template.isOnlineType() ? (System.currentTimeMillis() + (template.getTime() * 1000)) : 0;
				if (template.isPersonal())
				{
					CharacterPremiumDAO.getInstance().updatePersonal(player, id, time, template.isOnlineType());
				}
				else
				{
					CharacterPremiumDAO.getInstance().update(player, id, time, template.isOnlineType());
				}
				
				if (player.isInParty())
				{
					player.getParty().recalculatePartyData();
				}
			}
		}
		player.sendActionFailed();
		return true;
	}
}