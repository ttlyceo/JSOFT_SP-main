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

import java.util.StringTokenizer;

import l2e.commons.util.Util;
import l2e.gameserver.handler.admincommandhandlers.IAdminCommandHandler;
import l2e.gameserver.model.PcCondOverride;
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.strings.server.ServerMessage;
import l2e.gameserver.model.strings.server.ServerStorage;
import l2e.gameserver.network.serverpackets.DeleteObject;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;

public class PcCondOverrides implements IAdminCommandHandler
{
	private static final String[] COMMANDS =
	{
	        "admin_exceptions", "admin_set_exception",
	};
	
	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		final StringTokenizer st = new StringTokenizer(command);
		if (st.hasMoreTokens())
		{
			switch (st.nextToken())
			{
				case "admin_exceptions" :
				{
					final NpcHtmlMessage msg = new NpcHtmlMessage(5, 1);
					msg.setFile(activeChar, activeChar.getLang(), "data/html/admin/cond_override.htm", false);
					final StringBuilder sb = new StringBuilder();
					for (final PcCondOverride ex : PcCondOverride.values())
					{
						sb.append("<tr><td fixwidth=\"200\">" + ServerStorage.getInstance().getString(activeChar.getLang(), ex.getDescription()) + ":</td><td><a action=\"bypass -h admin_set_exception " + ex.ordinal() + "\">" + (activeChar.canOverrideCond(ex) ? ServerStorage.getInstance().getString(activeChar.getLang(), "PcCondOverride.DISABLE") : ServerStorage.getInstance().getString(activeChar.getLang(), "PcCondOverride.ENABLE")) + "</a></td></tr>");
					}
					msg.replace("%cond_table%", sb.toString());
					activeChar.sendPacket(msg);
					break;
				}
				case "admin_set_exception" :
				{
					if (st.hasMoreTokens())
					{
						final String token = st.nextToken();
						if (Util.isDigit(token))
						{
							final var ex = PcCondOverride.getCondOverride(Integer.valueOf(token));
							if (ex != null)
							{
								if (activeChar.canOverrideCond(ex))
								{
									activeChar.removeOverridedCond(ex);
									final var msg = new ServerMessage("PcCondOverride.DISABLE_MSG", activeChar.getLang());
									msg.add(ServerStorage.getInstance().getString(activeChar.getLang(), ex.getDescription()));
									activeChar.sendMessage(msg.toString());
									if (ex == PcCondOverride.SEE_ALL_PLAYERS)
									{
										refreshHidePlayers(activeChar, false);
									}
								}
								else
								{
									activeChar.addOverrideCond(ex);
									final var msg = new ServerMessage("PcCondOverride.ENABLE_MSG", activeChar.getLang());
									msg.add(ServerStorage.getInstance().getString(activeChar.getLang(), ex.getDescription()));
									activeChar.sendMessage(msg.toString());
									if (ex == PcCondOverride.SEE_ALL_PLAYERS)
									{
										refreshHidePlayers(activeChar, true);
									}
								}
							}
						}
						else
						{
							switch (token)
							{
								case "enable_all" :
								{
									for (final var ex : PcCondOverride.values())
									{
										if (!activeChar.canOverrideCond(ex))
										{
											activeChar.addOverrideCond(ex);
											if (ex == PcCondOverride.SEE_ALL_PLAYERS)
											{
												refreshHidePlayers(activeChar, true);
											}
										}
									}
									activeChar.sendMessage(new ServerMessage("PcCondOverride.ENABLE_ALL", activeChar.getLang()).toString());
									break;
								}
								case "disable_all" :
								{
									for (final var ex : PcCondOverride.values())
									{
										if (activeChar.canOverrideCond(ex))
										{
											activeChar.removeOverridedCond(ex);
											if (ex == PcCondOverride.SEE_ALL_PLAYERS)
											{
												refreshHidePlayers(activeChar, false);
											}
										}
									}
									activeChar.sendMessage(new ServerMessage("PcCondOverride.DISABLE_ALL", activeChar.getLang()).toString());
									break;
								}
							}
						}
						useAdminCommand(COMMANDS[0], activeChar);
					}
					break;
				}
			}
		}
		return true;
	}
	
	private void refreshHidePlayers(Player player, boolean isEnable)
	{
		if (isEnable)
		{
			World.getInstance().getAroundPlayers(player).stream().filter(p -> p != null && p.isInvisible()).forEach(p -> p.broadcastInfo());
		}
		else
		{
			World.getInstance().getAroundPlayers(player).stream().filter(p -> p != null && p.isInvisible()).forEach(p -> player.sendPacket(new DeleteObject(p)));
		}
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return COMMANDS;
	}
}