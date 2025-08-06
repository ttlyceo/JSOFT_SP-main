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

import l2e.commons.util.Util;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.entity.clanhall.SiegableHall;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;

public class SiegeNpcInstance extends NpcInstance
{
	public SiegeNpcInstance(int objectID, NpcTemplate template)
	{
		super(objectID, template);
		setInstanceType(InstanceType.SiegeNpcInstance);
	}
	
	@Override
	public void showChatWindow(Player player)
	{
		showSiegeInfoWindow(player);
	}

	public void showSiegeInfoWindow(Player player)
	{
		if (validateCondition(player))
		{
			final SiegableHall hall = getConquerableHall();
			if (hall != null)
			{
				hall.showSiegeInfo(player);
			}
			else
			{
				getCastle().getSiege().listRegisterClan(player);
			}
		}
		else
		{
			final var castle = getCastle();
			if (castle != null && castle.getTemplate() != null)
			{
				final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile(player, player.getLang(), "data/html/siege/" + getId() + "-busy.htm");
				html.replace("%castlename%", getConquerableHall() != null ? Util.clanHallName(player, getConquerableHall().getId()) : castle.getName(player.getLang()));
				html.replace("%objectId%", String.valueOf(getObjectId()));
				player.sendPacket(html);
			}
			player.sendActionFailed();
		}
	}

	private boolean validateCondition(Player player)
	{
		if ((getConquerableHall() != null) && getConquerableHall().isInSiege())
		{
			return false;
		}
		if (getCastle().getSiege().getIsInProgress())
		{
			return false;
		}

		return true;
	}
}