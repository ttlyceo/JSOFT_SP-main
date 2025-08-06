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

import java.util.Arrays;
import java.util.StringTokenizer;

import l2e.commons.util.StringUtil;
import l2e.gameserver.Config;
import l2e.gameserver.data.parser.FightEventParser;
import l2e.gameserver.handler.admincommandhandlers.IAdminCommandHandler;
import l2e.gameserver.instancemanager.QuestManager;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.entity.events.AbstractFightEvent;
import l2e.gameserver.model.entity.events.AbstractWorldEvent;
import l2e.gameserver.model.entity.events.custom.Leprechaun;
import l2e.gameserver.model.entity.events.model.FightEventManager;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.strings.server.ServerStorage;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;

public class Events implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
	        "admin_fight_events", "admin_fight_event_register", "admin_fight_event_start", "admin_fight_event_stop", "admin_leprechaun_start", "admin_leprechaun_abort", "admin_event_menu", "admin_event_start", "admin_event_stop", "admin_event_start_menu", "admin_event_stop_menu"
	};
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}

	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		if (activeChar == null)
		{
			return false;
		}
		
		String _event_name = "";
		final StringTokenizer st = new StringTokenizer(command, " ");
		st.nextToken();
		if (st.hasMoreTokens())
		{
			_event_name = st.nextToken();
		}

		if (command.contains("_menu"))
		{
			showWorldEventsMenu(activeChar);
		}

		if (command.startsWith("admin_fight_events"))
		{
			showFightEventsMenu(activeChar);
		}
		else if (command.startsWith("admin_fight_event_register"))
		{
			int id;
			try
			{
				id = Integer.parseInt(_event_name);
			}
			catch (final Exception e)
			{
				activeChar.sendMessage("Use it like that: //fight_event_register id(Id can be found in dir: data/stats/events)");
				return false;
			}
			
			final AbstractFightEvent event = FightEventParser.getInstance().getEvent(id);
			
			if (Arrays.binarySearch(Config.DISALLOW_FIGHT_EVENTS, event.getId()) >= 0)
			{
				activeChar.sendMessage(event.getName(activeChar.getLang()) + " forbidden to enabled!");
				return false;
			}
			
			if (!FightEventManager.getInstance().getActiveEventTask(event.getId()))
			{
				FightEventManager.getInstance().startEventCountdown(event);
				activeChar.sendMessage("Open register for event " + event.getName(activeChar.getLang()) + "!");
			}
			else
			{
				activeChar.sendMessage("Impossible to open register for event " + event.getName(activeChar.getLang()) + "!");
			}
			showFightEventsMenu(activeChar);
		}
		else if (command.startsWith("admin_fight_event_start"))
		{
			int id;
			try
			{
				id = Integer.parseInt(_event_name);
			}
			catch (final Exception e)
			{
				activeChar.sendMessage("Use it like that: //fight_event_start id(Id can be found in dir: data/stats/events)");
				return false;
			}
			
			final AbstractFightEvent event = FightEventParser.getInstance().getEvent(id);
			if (FightEventParser.getInstance().getDisabledEvents().contains(event.getId()))
			{
				activeChar.sendMessage(event.getName(activeChar.getLang()) + " forbidden to enabled!");
				return false;
			}
			
			if (FightEventManager.getInstance().getActiveEventTask(event.getId()))
			{
				FightEventManager.getInstance().prepareStartEventId(id);
			}
			else
			{
				activeChar.sendMessage("Impossible to start " + event.getName(activeChar.getLang()) + "!");
			}
			showFightEventsMenu(activeChar);
		}
		else if (command.startsWith("admin_fight_event_stop"))
		{
			int id;
			try
			{
				id = Integer.parseInt(_event_name);
			}
			catch (final Exception e)
			{
				activeChar.sendMessage("Use it like that: //fight_event_stop id(Id can be found in dir: data/stats/events)");
				return false;
			}
			
			final AbstractFightEvent event = FightEventManager.getInstance().getEventById(id);
			if (event != null)
			{
				if (FightEventParser.getInstance().getDisabledEvents().contains(event.getId()))
				{
					activeChar.sendMessage(event.getName(activeChar.getLang()) + " forbidden to disable!");
					return false;
				}

				if (event.getState() == AbstractFightEvent.EVENT_STATE.STARTED)
				{
					event.stopEvent();
					activeChar.sendMessage(event.getName(activeChar.getLang()) + " stopped!");
				}
				else if (event.getState() == AbstractFightEvent.EVENT_STATE.NOT_ACTIVE)
				{
					activeChar.sendMessage("Wait! Event in process of shutting down!");
				}
				else
				{
					activeChar.sendMessage("Wait! Event in process of loading...");
				}
			}
			else
			{
				activeChar.sendMessage("Impossible to stop this event!");
			}
			showFightEventsMenu(activeChar);
		}
		else if (command.startsWith("admin_leprechaun_start"))
		{
			if (Config.ENABLED_LEPRECHAUN)
			{
				if (Leprechaun.getInstance().isActive())
				{
					activeChar.sendMessage("There is problem with starting Leprechaun Event.");
				}
				else
				{
					Leprechaun.getInstance().startEvent();
				}
			}
			else
			{
				activeChar.sendMessage("Leprechaun Event is disabled.");
			}
			showWorldEventsMenu(activeChar);
		}
		else if (command.startsWith("admin_leprechaun_abort"))
		{
			if (Config.ENABLED_LEPRECHAUN)
			{
				if (Leprechaun.getInstance().isActive())
				{
					Leprechaun.getInstance().endEvent();
				}
				else
				{
					activeChar.sendMessage("There is problem with stoping Leprechaun Event.");
				}
			}
			else
			{
				activeChar.sendMessage("Leprechaun Event is disabled.");
			}
			showWorldEventsMenu(activeChar);
		}
		else if (command.startsWith("admin_event_start"))
		{
			try
			{
				if (_event_name != null)
				{
					final AbstractWorldEvent event = (AbstractWorldEvent) QuestManager.getInstance().getQuest(_event_name);
					if (event != null)
					{
						if (event.eventStart(-1, true))
						{
							activeChar.sendMessage("Event '" + _event_name + "' started.");
							showWorldEventsMenu(activeChar);
							return true;
						}
						activeChar.sendMessage("There is problem with starting '" + _event_name + "' event.");
						showWorldEventsMenu(activeChar);
						return true;
					}
				}
			}
			catch (final Exception e)
			{
				activeChar.sendMessage("Usage: //event_start <eventname>");
				e.printStackTrace();
				return false;
			}
		}
		else if (command.startsWith("admin_event_stop"))
		{
			try
			{
				if (_event_name != null)
				{
					final AbstractWorldEvent _event = (AbstractWorldEvent) QuestManager.getInstance().getQuest(_event_name);
					if (_event != null)
					{
						if (_event.eventStop())
						{
							activeChar.sendMessage("Event '" + _event_name + "' stopped.");
							showWorldEventsMenu(activeChar);
							return true;
						}
						activeChar.sendMessage("There is problem with stoping '" + _event_name + "' event.");
						showWorldEventsMenu(activeChar);
						return true;
					}
				}
			}
			catch (final Exception e)
			{
				activeChar.sendMessage("Usage: //event_start <eventname>");
				e.printStackTrace();
				return false;
			}
		}
		return false;
	}
	
	protected void showFightEventsMenu(Player activeChar)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile(activeChar, activeChar.getLang(), "data/html/admin/gm_fight-events.htm");
		final StringBuilder cList = new StringBuilder(500);
		StringUtil.append(cList, "<table width=280><tr>", "<td width=180><font color=\"c1b33a\">" + ServerStorage.getInstance().getString(activeChar.getLang(), "FightEvents.FIGHT_EVENT") + "</font></td><td width=100 align=center>" + ServerStorage.getInstance().getString(activeChar.getLang(), "FightEvents.ACTION") + "</td>", "</tr></table><img src=\"L2UI.squaregray\" width=\"280\" height=\"1\">");
		for (final AbstractFightEvent event : FightEventParser.getInstance().getEvents().valueCollection())
		{
			if (event != null)
			{
				String action = "";
				if (FightEventParser.getInstance().getDisabledEvents().contains(event.getId()))
				{
					action = "<font color=\"FF0000\">" + ServerStorage.getInstance().getString(activeChar.getLang(), "FightEvents.EVENT_DISABLE") + "</font>";
				}
				else if (!FightEventManager.getInstance().getActiveEventTask(event.getId()) && FightEventManager.getInstance().getEventById(event.getId()) == null)
				{
					action = "<button value=\"" + ServerStorage.getInstance().getString(activeChar.getLang(), "FightEvents.OPEN_REGISTER") + "\" action=\"bypass -h admin_fight_event_register " + event.getId() + "\" width=100 height=24 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">";
				}
				else if (FightEventManager.getInstance().getActiveEventTask(event.getId()) && FightEventManager.getInstance().getEventById(event.getId()) != null)
				{
					action = "<button value=\"" + ServerStorage.getInstance().getString(activeChar.getLang(), "FightEvents.START_EVENT") + "\" action=\"bypass -h admin_fight_event_start " + event.getId() + "\" width=100 height=24 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">";
				}
				else
				{
					action = "<button value=\"" + ServerStorage.getInstance().getString(activeChar.getLang(), "FightEvents.STOP_EVENT") + "\" action=\"bypass -h admin_fight_event_stop " + event.getId() + "\" width=100 height=24 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">";
				}
				StringUtil.append(cList, "<table width=280><tr><td width=180>" + activeChar.getEventName(event.getId()) + "</td>", "<td width=100 height=24 align=center>" + action + "</td>", "</tr></table><img src=\"L2UI.squaregray\" width=\"280\" height=\"1\">");
			}
		}
		html.replace("%LIST%", cList.toString());
		activeChar.sendPacket(html);
	}

	protected void showWorldEventsMenu(Player activeChar)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile(activeChar, activeChar.getLang(), "data/html/admin/gm_world-events.htm");
		final StringBuilder cList = new StringBuilder(500);
		StringUtil.append(cList, "<table width=280><tr>", "<td width=180><font color=\"c1b33a\">" + ServerStorage.getInstance().getString(activeChar.getLang(), "FightEvents.FIGHT_EVENT") + "</font></td><td width=100 align=center>" + ServerStorage.getInstance().getString(activeChar.getLang(), "FightEvents.ACTION") + "</td>", "</tr></table><img src=\"L2UI.squaregray\" width=\"280\" height=\"1\">");

		String leprechaun = "";
		if (!Leprechaun.getInstance().isActive())
		{
			leprechaun = "<button value=\"" + ServerStorage.getInstance().getString(activeChar.getLang(), "FightEvents.START_EVENT") + "\" action=\"bypass -h admin_leprechaun_start\" width=100 height=24 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">";
		}
		else
		{
			leprechaun = "<button value=\"" + ServerStorage.getInstance().getString(activeChar.getLang(), "FightEvents.STOP_EVENT") + "\" action=\"bypass -h admin_leprechaun_abort\" width=100 height=24 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">";
		}
		StringUtil.append(cList, "<table width=280><tr><td width=180>Leprechaun</td>", "<td width=100 align=center>" + leprechaun + "</td>", "</tr></table><img src=\"L2UI.squaregray\" width=\"280\" height=\"1\">");

		for (final Quest event : QuestManager.getInstance().getQuests())
		{
			if (event instanceof AbstractWorldEvent)
			{
				final AbstractWorldEvent _event = (AbstractWorldEvent) QuestManager.getInstance().getQuest(event.getName());
				if (_event != null && _event.getEventTemplate() != null)
				{
					String action = "";
					if (!_event.isEventActive())
					{
						action = "<button value=\"" + ServerStorage.getInstance().getString(activeChar.getLang(), "FightEvents.START_EVENT") + "\" action=\"bypass -h admin_event_start_menu " + event.getName() + "\" width=100 height=24 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">";
					}
					else
					{
						action = "<button value=\"" + ServerStorage.getInstance().getString(activeChar.getLang(), "FightEvents.STOP_EVENT") + "\" action=\"bypass -h admin_event_stop_menu " + event.getName() + "\" width=100 height=24 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">";
					}
					StringUtil.append(cList, "<table width=280><tr><td width=180>" + _event.getEventTemplate().getName(activeChar.getLang()) + "</td>", "<td width=100 align=center>" + action + "</td>", "</tr></table><img src=\"L2UI.squaregray\" width=\"280\" height=\"1\">");
				}
			}
		}
		html.replace("%LIST%", cList.toString());
		activeChar.sendPacket(html);
	}
}