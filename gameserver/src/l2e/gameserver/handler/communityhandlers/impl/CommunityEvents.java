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
package l2e.gameserver.handler.communityhandlers.impl;

import java.util.List;
import java.util.StringTokenizer;

import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.data.htm.HtmCache;
import l2e.gameserver.data.parser.FightEventParser;
import l2e.gameserver.handler.communityhandlers.ICommunityBoardHandler;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.entity.events.AbstractFightEvent;
import l2e.gameserver.model.entity.events.model.FightEventManager;
import l2e.gameserver.model.entity.events.model.FightLastStatsManager;
import l2e.gameserver.model.entity.events.model.template.FightEventLastPlayerStats;
import l2e.gameserver.model.strings.server.ServerMessage;
import l2e.gameserver.model.strings.server.ServerStorage;

/**
 * Created by LordWinter 11.08.2019
 */
public class CommunityEvents extends AbstractCommunity implements ICommunityBoardHandler
{
	public CommunityEvents()
	{
		if (Config.DEBUG)
		{
			_log.info(getClass().getSimpleName() + ": Loading all functions.");
		}
	}

	@Override
	public String[] getBypassCommands()
	{
		return new String[]
		{
		        "_bbsgetfav", "_bbsaddfav", "_bbsdelfav", "_bbsevent", "_bbseventUnregister", "_bbseventRegister", "_bbsfightList"
		};
	}
	
	@Override
	public void onBypassCommand(String command, Player player)
	{
		if (!Config.ALLOW_FIGHT_EVENTS)
		{
			player.sendMessage((new ServerMessage("Community.ALL_DISABLE", player.getLang())).toString());
			return;
		}
		
		final StringTokenizer st = new StringTokenizer(command, "_");
		final String cmd = st.nextToken();
		
		if (!checkCondition(player, cmd, false, false))
		{
			return;
		}
		
		if ("bbsevent".equals(cmd) || "bbsgetfav".equals(cmd))
		{
			final AbstractFightEvent event = FightEventManager.getInstance().getNextEvent();
			if (event != null)
			{
				onBypassCommand("_bbsfightList_" + getAcviteEvent(event.getId()), player);
			}
			else
			{
				onBypassCommand("_bbsfightList_1", player);
			}
			return;
		}
		else if ("bbsfightList".equals(cmd))
		{
			final int page = Integer.parseInt(st.nextToken());
			String html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/events.htm");
			final String template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/event-template.htm");

			String block = "";
			String list = "";

			final int perpage = 1;
			int counter = 0;
			
			final int totalSize = FightEventParser.getInstance().getAcviteEvents().size();
			final boolean isThereNextPage = totalSize > perpage;

			for (int i = (page - 1) * perpage; i < totalSize; i++)
			{
				final AbstractFightEvent event = FightEventParser.getInstance().getEvent(FightEventParser.getInstance().getAcviteEvents().get(i));
				if (event != null)
				{
					block = template;

					block = block.replace("%eventIcon%", event.getIcon());
					block = block.replace("%eventName%", player.getEventName(event.getId()));
					block = block.replace("%eventDesc%", player.getEventDescr(event.getId()));
					String register;
					
					if (!FightEventManager.getInstance().isRegistrationOpened(event))
					{
						register = ServerStorage.getInstance().getString(player.getLang(), "FightEvents.REG_CLOSE");
					}
					else
					{
						if (FightEventManager.getInstance().isPlayerRegistered(player, event.getId()))
						{
							final var msg = new ServerMessage("FightEvents.UNREG_COMPLETE", player.getLang());
							msg.add(event.getId());
							register = msg.toString();
						}
						else
						{
							if (player.getFightEventGameRoom() != null)
							{
								register = ServerStorage.getInstance().getString(player.getLang(), "FightEvents.AREADY_REG");
							}
							else
							{
								final var msg = new ServerMessage("FightEvents.REG_COMPLETE", player.getLang());
								msg.add(event.getId());
								register = msg.toString();
							}
						}
					}
					block = block.replace("%eventRegister%", register);
					block = getEventStats(event.getId(), block, player);

					list += block;
				}
				counter++;

				if (counter >= perpage)
				{
					break;
				}
			}
			final int count = (int) Math.ceil((double) totalSize / perpage);
			html = html.replace("{list}", list);
			html = html.replace("{navigation}", Util.getNavigationBlock(count, page, totalSize, perpage, isThereNextPage, "_bbsfightList_%s"));
			separateAndSend(html, player);
		}
		else if ("bbseventUnregister".equals(cmd))
		{
			final int eventId = Integer.parseInt(st.nextToken());
			FightEventManager.getInstance().unsignFromEvent(player, eventId);
			onBypassCommand("_bbsfightList_" + eventId, player);
		}
		else if ("bbseventRegister".equals(cmd))
		{
			final int eventId = Integer.parseInt(st.nextToken());
			final AbstractFightEvent event = FightEventManager.getInstance().getEventById(eventId);
			if (event != null)
			{
				FightEventManager.getInstance().trySignForEvent(player, event, true);
			}
			onBypassCommand("_bbsfightList_" + eventId, player);
		}
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
	
	private static String getEventStats(int eventId, String html, Player player)
	{
		final List<FightEventLastPlayerStats> stats = FightLastStatsManager.getInstance().getStats(eventId, true);

		for (int i = 0; i < 10; i++)
		{
			if ((i + 1) <= stats.size())
			{
				final FightEventLastPlayerStats stat = stats.get(i);
				if (stat.isMyStat(player))
				{
					html = html.replace("<?name_" + i + "?>", "<fonr color=\"CC3333\">" + stat.getPlayerName() + "</font>");
					html = html.replace("<?count_" + i + "?>", "<fonr color=\"CC3333\">" + Util.formatAdena(stat.getScore()) + "</font>");
					html = html.replace("<?class_" + i + "?>", "<fonr color=\"CC3333\">" + Util.className(player, stat.getClassId()) + "</font>");
					html = html.replace("<?clan_" + i + "?>", "<fonr color=\"CC3333\">" + stat.getClanName() + "</font>");
					html = html.replace("<?ally_" + i + "?>", "<fonr color=\"CC3333\">" + stat.getAllyName() + "</font>");
				}
				else
				{
					html = html.replace("<?name_" + i + "?>", stat.getPlayerName());
					html = html.replace("<?count_" + i + "?>", Util.formatAdena(stat.getScore()));
					html = html.replace("<?class_" + i + "?>", Util.className(player, stat.getClassId()));
					html = html.replace("<?clan_" + i + "?>", stat.getClanName());
					html = html.replace("<?ally_" + i + "?>", stat.getAllyName());
				}
			}
			else
			{
				html = html.replace("<?name_" + i + "?>", "...");
				html = html.replace("<?count_" + i + "?>", "...");
				html = html.replace("<?class_" + i + "?>", "...");
				html = html.replace("<?clan_" + i + "?>", "...");
				html = html.replace("<?ally_" + i + "?>", "...");
			}
		}

		final FightEventLastPlayerStats my = FightLastStatsManager.getInstance().getMyStat(eventId, player);
		if (my != null)
		{
			html = html.replace("<?name_me?>", "<fonr color=\"CC3333\">" + my.getPlayerName() + "</font>");
			html = html.replace("<?count_me?>", "<fonr color=\"CC3333\">" + Util.formatAdena(my.getScore()) + "</font>");
			html = html.replace("<?class_me?>", "<fonr color=\"CC3333\">" + Util.className(player, my.getClassId()) + "</font>");
			html = html.replace("<?clan_me?>", "<fonr color=\"CC3333\">" + my.getClanName() + "</font>");
			html = html.replace("<?ally_me?>", "<fonr color=\"CC3333\">" + my.getAllyName() + "</font>");
		}
		else
		{
			html = html.replace("<?name_me?>", "...");
			html = html.replace("<?count_me?>", "...");
			html = html.replace("<?class_me?>", "...");
			html = html.replace("<?clan_me?>", "...");
			html = html.replace("<?ally_me?>", "...");
		}

		return html;
	}

	@Override
	public void onWriteCommand(String command, String ar1, String ar2, String ar3, String ar4, String ar5, Player activeChar)
	{
	}

	public static CommunityEvents getInstance()
	{
		return SingletonHolder._instance;
	}

	private static class SingletonHolder
	{
		protected static final CommunityEvents _instance = new CommunityEvents();
	}
}