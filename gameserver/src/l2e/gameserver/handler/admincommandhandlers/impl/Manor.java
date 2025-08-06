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
package l2e.gameserver.handler.admincommandhandlers.impl;

import l2e.commons.util.StringUtil;
import l2e.commons.util.Util;
import l2e.gameserver.handler.admincommandhandlers.IAdminCommandHandler;
import l2e.gameserver.instancemanager.CastleManager;
import l2e.gameserver.instancemanager.CastleManorManager;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.entity.Castle;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;

public class Manor implements IAdminCommandHandler
{
	private static final String[] _adminCommands =
	{
	        "admin_manor"
	};

	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		final CastleManorManager manor = CastleManorManager.getInstance();
		final NpcHtmlMessage msg = new NpcHtmlMessage(5);
		msg.setFile(activeChar, activeChar.getLang(), "data/html/admin/manor.htm");
		msg.replace("%status%", manor.getCurrentModeName());
		msg.replace("%change%", manor.getNextModeChange());
		
		final StringBuilder sb = new StringBuilder(3400);
		for (final Castle c : CastleManager.getInstance().getCastles())
		{
			StringUtil.append(sb, "<tr><td>Name:</td><td><font color=008000>" + c.getName(activeChar.getLang()) + "</font></td></tr>");
			StringUtil.append(sb, "<tr><td>Current period cost:</td><td><font color=FF9900>", Util.formatAdena(manor.getManorCost(c.getId(), false)), " Adena</font></td></tr>");
			StringUtil.append(sb, "<tr><td>Next period cost:</td><td><font color=FF9900>", Util.formatAdena(manor.getManorCost(c.getId(), true)), " Adena</font></td></tr>");
			StringUtil.append(sb, "<tr><td><font color=808080>--------------------------</font></td><td><font color=808080>--------------------------</font></td></tr>");
		}
		msg.replace("%castleInfo%", sb.toString());
		activeChar.sendPacket(msg);
		
		sb.setLength(0);
		return true;
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return _adminCommands;
	}
}