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

import l2e.gameserver.handler.itemhandlers.IItemHandler;
import l2e.gameserver.model.ClanMember;
import l2e.gameserver.model.actor.Playable;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.olympiad.Olympiad;
import l2e.gameserver.network.SystemMessageId;

public class Nobless implements IItemHandler
{
	@Override
	public boolean useItem(Playable playable, ItemInstance item, boolean forceUse)
	{
		if (!playable.isPlayer())
		{
			playable.sendPacket(SystemMessageId.ITEM_NOT_FOR_PETS);
			return false;
		}
		
		final var player = playable.getActingPlayer();
		if (player.isNoble())
		{
			player.sendPacket(SystemMessageId.NOTHING_INSIDE_THAT);
			return false;
		}
		
		if (!player.destroyItem("Nobless", item.getObjectId(), 1, player, true))
		{
			return false;
		}
		
		Olympiad.addNoble(player);
		player.setNoble(true);
		if (player.getClan() != null)
		{
			player.setPledgeClass(ClanMember.calculatePledgeClass(player));
		}
		else
		{
			player.setPledgeClass(5);
		}
		player.sendUserInfo();
		return true;
	}
}