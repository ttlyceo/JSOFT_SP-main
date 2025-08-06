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
package l2e.gameserver.model.actor.instance;

import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.entity.events.cleft.AerialCleftEvent;

/**
 * Created by LordWinter 03.12.2018
 */
public final class CleftConverterInstance extends NpcInstance
{
	public CleftConverterInstance(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public void showChatWindow(Player player)
	{
		showChatWindow(player, "data/html/aerialCleft/32520-00.htm");
	}
	
	@Override
	public void onBypassFeedback(Player player, String command)
	{
		if ((player == null) || (player.getLastFolkNPC() == null) || (player.getLastFolkNPC().getObjectId() != getObjectId()))
		{
			return;
		}
		
		if (command.equalsIgnoreCase("Teleport"))
		{
			if (AerialCleftEvent.getInstance().isStarted() || AerialCleftEvent.getInstance().isRewarding())
			{
				AerialCleftEvent.getInstance().removePlayer(player.getObjectId(), true);
			}
			else
			{
				player.teleToClosestTown();
			}
		}
		else
		{
			super.onBypassFeedback(player, command);
		}
	}
}