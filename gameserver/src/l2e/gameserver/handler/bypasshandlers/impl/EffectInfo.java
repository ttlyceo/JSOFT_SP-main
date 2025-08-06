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

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import l2e.commons.util.Util;
import l2e.gameserver.data.htm.HtmCache;
import l2e.gameserver.handler.bypasshandlers.IBypassHandler;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.skills.effects.Effect;
import l2e.gameserver.model.strings.server.ServerStorage;

public class EffectInfo implements IBypassHandler
{
	private static final String[] COMMANDS =
	{
	        "effects"
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
				case "effects" :
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
						
							String html = HtmCache.getInstance().getHtm(activeChar, activeChar.getLang(), "data/html/effects_info.htm");
							final String template = HtmCache.getInstance().getHtm(activeChar, activeChar.getLang(), "data/html/effects_template.htm");
							String block = "";
							String list = "";
							
							final List<Effect> effList = new ArrayList<>();
							for (final Effect ef : npc.getAllEffects())
							{
								if (ef != null && ef.isIconDisplay())
								{
									effList.add(ef);
								}
							}
							
							if (effList.isEmpty() || effList.size() == 0)
							{
								html = html.replace("{list}", "<tr><td align=center>" + ServerStorage.getInstance().getString(activeChar.getLang(), "Info.EMPTY_LIST") + "</td></tr>");
								html = html.replace("{navigation}", "<td>&nbsp;</td>");
								html = html.replace("{npc_name}", npc.getName(activeChar.getLang()));
								Util.setHtml(html, activeChar);
								return false;
							}
						
							final int perpage = 6;
							int counter = 0;
							final int totalSize = effList.size();
							final boolean isThereNextPage = totalSize > perpage;
						
							for (int i = (page - 1) * perpage; i < totalSize; i++)
							{
								final Effect data = effList.get(i);
								if (data != null)
								{
									block = template;
									
									block = block.replace("{name}", data.getSkill().getName(activeChar.getLang()));
									block = block.replace("{icon}", data.getSkill().getIcon());
									block = block.replace("{time}", getTimeLeft(data.getTimeLeft()));
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
							html = html.replace("{navigation}", Util.getNavigationBlock(count, page, totalSize, perpage, isThereNextPage, "effects %s"));
							html = html.replace("{npc_name}", npc.getName(activeChar.getLang()));
							Util.setHtml(html, activeChar);
						}
					}
					catch (final Exception e)
					{
						activeChar.sendMessage("Something went wrong with the effects preview.");
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
	
	private static String getTimeLeft(long time)
	{
		final int hours = (int) (time / 60 / 60);
		final int mins = (int) ((time - (hours * 60 * 60)) / 60);
		final int secs = (int) ((time - ((hours * 60 * 60) + (mins * 60))));
		
		final String Strhours = hours < 10 ? "0" + hours : "" + hours;
		final String Strmins = mins < 10 ? "0" + mins : "" + mins;
		final String Strsecs = secs < 10 ? "0" + secs : "" + secs;
		if (hours > 0)
		{
			return "<font color=\"b02e31\">" + Strhours + ":" + Strmins + ":" + Strsecs + "</font>";
		}
		else if (hours <= 0 && mins > 0)
		{
			return "<font color=\"b02e31\">" + Strmins + ":" + Strsecs + "</font>";
		}
		return "<font color=\"b02e31\">" + Strsecs + "</font>";
	}
	
	@Override
	public String[] getBypassList()
	{
		return COMMANDS;
	}
}