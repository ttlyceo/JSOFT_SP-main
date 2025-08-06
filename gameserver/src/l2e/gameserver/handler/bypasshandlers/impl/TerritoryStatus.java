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
package l2e.gameserver.handler.bypasshandlers.impl;

import l2e.gameserver.data.holder.ClanHolder;
import l2e.gameserver.handler.bypasshandlers.IBypassHandler;
import l2e.gameserver.model.Clan;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;

public class TerritoryStatus implements IBypassHandler
{
	private static final String[] COMMANDS =
	{
	        "TerritoryStatus"
	};
	
	@Override
	public boolean useBypass(String command, Player activeChar, Creature target)
	{
		if (!target.isNpc())
		{
			return false;
		}
		
		final Npc npc = (Npc) target;
		final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		{
			if (npc.getCastle().getOwnerId() > 0)
			{
				html.setFile(activeChar, activeChar.getLang(), "data/html/territorystatus.htm");
				final Clan clan = ClanHolder.getInstance().getClan(npc.getCastle().getOwnerId());
				html.replace("%clanname%", clan.getName());
				html.replace("%clanleadername%", clan.getLeaderName());
			}
			else
			{
				html.setFile(activeChar, activeChar.getLang(), "data/html/territorynoclan.htm");
			}
		}
		html.replace("%castlename%", npc.getCastle().getName(activeChar.getLang()));
		html.replace("%taxpercent%", "" + npc.getCastle().getTaxPercent());
		html.replace("%objectId%", String.valueOf(npc.getObjectId()));
		{
			if (npc.getCastle().getId() > 6)
			{
				html.replace("%territory%", "The Kingdom of Elmore");
			}
			else
			{
				html.replace("%territory%", "The Kingdom of Aden");
			}
		}
		activeChar.sendPacket(html);
		return true;
	}
	
	@Override
	public String[] getBypassList()
	{
		return COMMANDS;
	}
}