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

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;


import l2e.commons.util.TimeUtils;
import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.data.htm.HtmCache;
import l2e.gameserver.data.parser.FightEventParser;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.entity.events.AbstractFightEvent;
import l2e.gameserver.model.entity.events.model.FightEventManager;

public class FightEventManagerInstance extends NpcInstance
{
	public FightEventManagerInstance(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public void showChatWindow(Player player, int val)
	{
		showMainPage(player, 1);
	}

	@Override
	public void onBypassFeedback(Player player, String command)
	{
		if (!Config.ALLOW_FIGHT_EVENTS)
		{
			player.sendMessage("Fight events disable!");
			return;
		}
		
		final StringTokenizer st = new StringTokenizer(command, " ");
		final String currentcommand = st.nextToken();

		if (currentcommand.startsWith("regPlayer"))
		{
			final int eventId = Integer.parseInt(st.nextToken());
			final AbstractFightEvent event = FightEventManager.getInstance().getEventById(eventId);
			if (event != null)
			{
				FightEventManager.getInstance().trySignForEvent(player, event, true);
			}
			
			final AbstractFightEvent event1 = FightEventManager.getInstance().getNextEvent();
			int page = 1;
			if (event1 != null)
			{
				page = getAcviteEvent(event1.getId());
			}
			showMainPage(player, page);
		}
		else if (currentcommand.startsWith("unregPlayer"))
		{
			final int eventId = Integer.parseInt(st.nextToken());
			FightEventManager.getInstance().unsignFromEvent(player, eventId);
			final AbstractFightEvent event1 = FightEventManager.getInstance().getNextEvent();
			int page = 1;
			if (event1 != null)
			{
				page = getAcviteEvent(event1.getId());
			}
			showMainPage(player, page);
		}
		else if (currentcommand.startsWith("nextPage"))
		{
			final int page = Integer.parseInt(st.nextToken());
			showMainPage(player, page);
		}
		else
		{
			super.onBypassFeedback(player, command);
		}
	}

	private void showMainPage(Player player, int page)
	{
		final int activeEvents = FightEventParser.getInstance().getAcviteEvents().size();
		if (activeEvents <= 0)
		{
			return;
		}
		
		final List<AbstractFightEvent> events = new ArrayList<>();
		for (int i = 0; i < activeEvents; i++)
		{
			final AbstractFightEvent event = FightEventParser.getInstance().getEvent(FightEventParser.getInstance().getAcviteEvents().get(i));
			if (event != null)
			{
				if (event.getAutoStartTimes() == null)
				{
					continue;
				}
				events.add(event);
			}
		}
		
		if (events.size() <= 0)
		{
			return;
		}
		
		String html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/events/all/index.htm");
		final String template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/events/all/template.htm");
		String block = "";
		String list = "";
		
		final int perpage = 1;
		int counter = 0;
		String bypass = "npc_%objectId%_nextPage";
		final boolean isThereNextPage = events.size() > perpage;
		for (int i = (page - 1) * perpage; i < events.size(); i++)
		{
			final AbstractFightEvent event = events.get(i);
			if (event != null)
			{
				if (event.getAutoStartTimes() == null)
				{
					continue;
				}
				block = template;
				block = block.replace("%eventIcon%", event.getIcon());
				block = block.replace("%eventName%", player.getEventName(event.getId()));
				block = block.replace("%eventDesc%", player.getEventDescr(event.getId()));
				block = block.replace("%eventDate%", TimeUtils.toSimpleFormat(event.getAutoStartTimes().next(System.currentTimeMillis())));
				String register;
				if (!FightEventManager.getInstance().isRegistrationOpened(event))
				{
					register = "<font color=\"FF0000\">Registration Closed</font>";
				}
				else
				{
					if (FightEventManager.getInstance().isPlayerRegistered(player, event.getId()))
					{
						register = "<button value=\"Unregister from Event\" action=\"bypass -h npc_%objectId%_unregPlayer " + event.getId() + "\" back=\"L2UI_CT1.OlympiadWnd_DF_Fight3None_Down\" width=200 height=30 fore=\"L2UI_CT1.OlympiadWnd_DF_Fight3None\">";
					}
					else
					{
						if (player.getFightEventGameRoom() != null || player.isInFightEvent())
						{
							register = "<font color=\"FF0000\">You are already registered for other event!</font>";
						}
						else
						{
							register = "<button value=\"Register to Event\" action=\"bypass -h npc_%objectId%_regPlayer " + event.getId() + "\" back=\"L2UI_CT1.OlympiadWnd_DF_Fight3None_Down\" width=200 height=30 fore=\"L2UI_CT1.OlympiadWnd_DF_Fight3None\">";
						}
					}
				}
				block = block.replace("%eventRegister%", register);
				block = block.replace("%objectId%", String.valueOf(getObjectId()));
				bypass = bypass.replace("%objectId%", String.valueOf(getObjectId()));
				list += block;
			}
			
			counter++;
			
			if (counter >= perpage)
			{
				break;
			}
		}
		
		final int count = (int) Math.ceil((double) events.size() / perpage);
		html = html.replace("%list%", list);
		html = html.replace("%navigation%", Util.getNavigationBlock(count, page, events.size(), perpage, isThereNextPage, "" + bypass + " %s"));
		Util.setHtml(html, player);
	}
	
	private int getAcviteEvent(int eventId)
	{
		int id = 0;
		for (int i = 0; i < FightEventParser.getInstance().getAcviteEvents().size(); i++)
		{
			final AbstractFightEvent event = FightEventParser.getInstance().getEvent(FightEventParser.getInstance().getAcviteEvents().get(i));
			if (event != null && event.getId() == eventId)
			{
				id = i;
			}
		}
		return id + 1;
	}
}