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

public final class ObservationInstance extends NpcInstance
{
	public ObservationInstance(int objectId, NpcTemplate template)
	{
		super(objectId, template);
		setInstanceType(InstanceType.ObservationInstance);
	}

	@Override
	public void showChatWindow(Player player, int val)
	{
		String filename = null;
		
		if (isInsideRadius(-79884, 86529, 50, true) || isInsideRadius(-78858, 111358, 50, true) || isInsideRadius(-76973, 87136, 50, true) || isInsideRadius(-75850, 111968, 50, true))
		{
			if (val == 0)
			{
				filename = "data/html/observation/" + getId() + "-Oracle.htm";
			}
			else
			{
				filename = "data/html/observation/" + getId() + "-Oracle-" + val + ".htm";
			}
		}
		else
		{
			if (val == 0)
			{
				filename = "data/html/observation/" + getId() + ".htm";
			}
			else
			{
				filename = "data/html/observation/" + getId() + "-" + val + ".htm";
			}
		}
		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(player, player.getLang(), filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		player.sendPacket(html);
	}
}