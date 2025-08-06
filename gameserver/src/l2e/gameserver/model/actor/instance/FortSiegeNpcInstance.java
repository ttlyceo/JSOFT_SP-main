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
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;

public class FortSiegeNpcInstance extends NpcInstance
{
	public FortSiegeNpcInstance(int objectID, NpcTemplate template)
	{
		super(objectID, template);
		setInstanceType(InstanceType.FortSiegeNpcInstance);
	}

	@Override
	public void showChatWindow(Player player, int val)
	{
		player.sendActionFailed();
		
		String filename;
		
		if (val == 0)
		{
			filename = "data/html/fortress/merchant.htm";
		}
		else
		{
			filename = "data/html/fortress/merchant-" + val + ".htm";
		}
		
		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(player, player.getLang(), filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%npcId%", String.valueOf(getId()));
		if (getFort().getOwnerClan() != null)
		{
			html.replace("%clanname%", getFort().getOwnerClan().getName());
		}
		else
		{
			html.replace("%clanname%", "NPC");
		}
		player.sendPacket(html);
	}
	
	@Override
	public boolean hasRandomAnimation()
	{
		return false;
	}
}