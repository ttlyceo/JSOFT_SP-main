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

import l2e.gameserver.model.actor.Player;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;

public final class RequestLinkHtml extends GameClientPacket
{
	private String _link;
	
	@Override
	protected void readImpl()
	{
		_link = readS();
	}
	
	@Override
	public void runImpl()
	{
		final Player actor = getClient().getActiveChar();
		if (actor == null)
		{
			return;
		}
		
		if (_link.contains("..") || !_link.contains(".htm"))
		{
			_log.warn("[RequestLinkHtml] hack? link contains prohibited characters: '" + _link + "', skipped");
			return;
		}
		try
		{
			final String filename = "data/html/" + _link;
			final NpcHtmlMessage msg = new NpcHtmlMessage(0);
			msg.setFile(actor, actor.getLang(), filename);
			sendPacket(msg);
		}
		catch (final Exception e)
		{
			_log.warn("Bad RequestLinkHtml: ", e);
		}
	}
}