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

import l2e.gameserver.data.htm.HtmCache;
import l2e.gameserver.handler.itemhandlers.IItemHandler;
import l2e.gameserver.model.actor.Playable;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;

public class Bypass implements IItemHandler
{
	@Override
	public boolean useItem(Playable playable, ItemInstance item, boolean forceUse)
	{
		if (!(playable.isPlayer()))
		{
			return false;
		}
		final Player activeChar = (Player) playable;
		final int itemId = item.getId();

		final String filename = "data/html/item/" + itemId + ".htm";
		final String content = HtmCache.getInstance().getHtm(activeChar, activeChar.getLang(), filename);
		final NpcHtmlMessage html = new NpcHtmlMessage(0, itemId);
		if (content == null)
		{
			html.setHtml(activeChar, "<html><body>My Text is missing:<br>" + filename + "</body></html>");
			activeChar.sendPacket(html);
		}
		else
		{
			html.setHtml(activeChar, content);
			html.replace("%itemId%", String.valueOf(item.getObjectId()));
			activeChar.sendPacket(html);
		}
		return true;
	}
}