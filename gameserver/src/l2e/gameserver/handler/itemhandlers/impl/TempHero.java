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
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.network.SystemMessageId;

public class TempHero implements IItemHandler
{
	@Override
	public boolean useItem(Playable playable, ItemInstance item, boolean forceUse)
	{
		if (!playable.isPlayer())
		{
			playable.sendPacket(SystemMessageId.ITEM_NOT_FOR_PETS);
			return false;
		}
		
		final Player player = playable.getActingPlayer();
		if (player.isHero())
		{
			player.sendPacket(SystemMessageId.NOTHING_INSIDE_THAT);
			return false;
		}
		
		final int timeLimit = item.getItem().getTimeLimit();
		if (timeLimit == 0)
		{
			_log.info("Not correct timeLimit for item: " + item.getId());
			return false;
		}
		
		if (!player.destroyItem("tempHero", item.getObjectId(), 1, player, true))
		{
			return false;
		}
		
		final boolean isWithSkills = item.getItem().isWithHeroSkills();
		final long endTime = System.currentTimeMillis() + (timeLimit * 60000L);
		player.setVar("tempHero", String.valueOf(endTime));
		if (isWithSkills)
		{
			player.setVar("tempHeroSkills", "1");
		}
		player.setHero(true, isWithSkills);
		player.startTempHeroTask(endTime, isWithSkills ? 1 : 0);
		if (player.getClan() != null)
		{
			player.setPledgeClass(ClanMember.calculatePledgeClass(player));
		}
		else
		{
			player.setPledgeClass(8);
		}
		player.broadcastUserInfo(true);
		return true;
	}
}