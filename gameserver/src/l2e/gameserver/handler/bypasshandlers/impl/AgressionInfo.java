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

import java.util.List;
import java.util.StringTokenizer;

import l2e.commons.util.Util;
import l2e.gameserver.data.htm.HtmCache;
import l2e.gameserver.handler.bypasshandlers.IBypassHandler;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.Summon;
import l2e.gameserver.model.actor.templates.npc.aggro.AggroInfo;
import l2e.gameserver.model.strings.server.ServerStorage;

public class AgressionInfo implements IBypassHandler
{
	private static final String[] COMMANDS =
	{
	        "aggro"
	};

	@Override
	public boolean useBypass(String command, Player activeChar, Creature target)
	{
		try
		{
			final StringTokenizer st = new StringTokenizer(command, " ");
			final String actualCommand = st.nextToken();
			switch (actualCommand.toLowerCase())
			{
				case "aggro" :
				{
					try
					{
						String pg = null;
						try
						{
							pg = st.nextToken();
						}
						catch (final Exception e)
						{}
						
						if (pg != null)
						{
							final int page = Integer.parseInt(pg);
							final GameObject targetmob = activeChar.getTarget();
							if (!(targetmob instanceof Attackable))
							{
								activeChar.sendMessage("You cant use this option with this target.");
								return false;
							}
							
							final Attackable npc = (Attackable) targetmob;
						
							String html = HtmCache.getInstance().getHtm(activeChar, activeChar.getLang(), "data/html/aggro_info.htm");
							final String template = HtmCache.getInstance().getHtm(activeChar, activeChar.getLang(), "data/html/aggro_template.htm");
							String block = "";
							String list = "";
							
							if (npc.getAggroList() == null || npc.getAggroList().isEmpty())
							{
								html = html.replace("{list}", "<tr><td align=center>" + ServerStorage.getInstance().getString(activeChar.getLang(), "Info.EMPTY_LIST") + "</td></tr>");
								html = html.replace("{navigation}", "<td>&nbsp;</td>");
								html = html.replace("{npc_name}", npc.getName(activeChar.getLang()));
								Util.setHtml(html, activeChar);
								return false;
							}
						
							final List<AggroInfo> aggroList = npc.getAggroList().getAggroInfo();
							final int perpage = 8;
							int counter = 0;
							final int totalSize = aggroList.size();
							final boolean isThereNextPage = totalSize > perpage;
						
							for (int i = (page - 1) * perpage; i < totalSize; i++)
							{
								final AggroInfo data = aggroList.get(i);
								if (data != null)
								{
									block = template;
									
									block = block.replace("{name}", (data.getAttacker().isSummon() || data.getAttacker().isPet()) ? ((Summon) data.getAttacker()).getSummonName(activeChar.getLang()) : data.getAttacker().getName(activeChar.getLang()));
									block = block.replace("{damage}", String.valueOf(data.getDamage()));
									block = block.replace("{hate}", String.valueOf(data.getHate()));
									list += block;
								}
							
								counter++;
							
								if (counter >= perpage)
								{
									break;
								}
							}
						
							final double pages = (double) totalSize / perpage;
							final int count = (int) Math.ceil(pages);
							html = html.replace("{list}", list);
							html = html.replace("{navigation}", Util.getNavigationBlock(count, page, totalSize, perpage, isThereNextPage, "aggro %s"));
							html = html.replace("{npc_name}", npc.getName(activeChar.getLang()));
							Util.setHtml(html, activeChar);
						}
					}
					catch (final Exception e)
					{
						activeChar.sendMessage("Something went wrong with the aggro preview.");
					}
					break;
				}
			}
		}
		catch (final Exception e)
		{
			activeChar.sendMessage("You cant use this option with this target.");
		}
		return false;
	}

	@Override
	public String[] getBypassList()
	{
		return COMMANDS;
	}
}