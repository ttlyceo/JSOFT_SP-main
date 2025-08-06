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
import l2e.gameserver.model.entity.Castle;
import l2e.gameserver.model.entity.Fort;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;

public class FortEnvoyInstance extends NpcInstance
{
	public FortEnvoyInstance(int objectID, NpcTemplate template)
	{
		super(objectID, template);
		setInstanceType(InstanceType.FortEnvoyInstance);
	}
	
	@Override
	public void showChatWindow(Player player)
	{
		String filePath;
		final Fort fortress = getFort();
		if (!player.isClanLeader() || (fortress.getId() != player.getClan().getFortId()))
		{
			filePath = "data/html/fortress/ambassador-not-leader.htm";
		}
		else if (fortress.getFortState() == 1)
		{
			filePath = "data/html/fortress/ambassador-rejected.htm";
		}
		else if (fortress.getFortState() == 2)
		{
			filePath = "data/html/fortress/ambassador-signed.htm";
		}
		else if (fortress.isBorderFortress())
		{
			filePath = "data/html/fortress/ambassador-border.htm";
		}
		else
		{
			filePath = "data/html/fortress/ambassador.htm";
		}
		player.sendActionFailed();
		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(player, player.getLang(), filePath);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%castleName%", String.valueOf(fortress.getCastleByAmbassador(getId()).getName(player.getLang())));
		player.sendPacket(html);
	}

	@Override
	public void onBypassFeedback(Player player, String command)
	{
		if (command.startsWith("select "))
		{
			final String param = command.substring(7);
			final Fort fortress = getFort();
			final Castle castle = fortress.getCastleByAmbassador(getId());
			String filePath;
			
			if (castle != null && castle.getTemplate() != null)
			{
				if (castle.getOwnerId() == 0)
				{
					filePath = "data/html/fortress/ambassador-not-owned.htm";
				}
				else
				{
					final int choice = Util.isDigit(param) ? Integer.parseInt(param) : 0;
					fortress.setFortState(choice, castle.getId());
					filePath = (choice == 1) ? "data/html/fortress/ambassador-independent.htm" : "data/html/fortress/ambassador-signed.htm";
				}
				
				final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile(player, player.getLang(), filePath);
				html.replace("%castleName%", castle.getName(player.getLang()));
				player.sendPacket(html);
			}
		}
		else
		{
			super.onBypassFeedback(player, command);
		}
	}
}