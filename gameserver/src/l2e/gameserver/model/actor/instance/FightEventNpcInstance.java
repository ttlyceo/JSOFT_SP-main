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

import java.util.StringTokenizer;

import l2e.gameserver.Config;
import l2e.gameserver.data.htm.HtmCache;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.entity.events.AbstractFightEvent;
import l2e.gameserver.model.entity.events.model.FightEventManager;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;

public class FightEventNpcInstance extends NpcInstance
{
	public FightEventNpcInstance(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public void showChatWindow(Player player, int val)
	{
		showMainPage(player);
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
			showMainPage(player);
		}
		else if (currentcommand.startsWith("unregPlayer"))
		{
			final int eventId = Integer.parseInt(st.nextToken());
			FightEventManager.getInstance().unsignFromEvent(player, eventId);
			showMainPage(player);
		}
		else
		{
			super.onBypassFeedback(player, command);
		}
	}

	private void showMainPage(Player player)
	{
		final AbstractFightEvent event = FightEventManager.getInstance().getNextEvent();
		if (event != null)
		{
			final String html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/events/index.htm");
			if (html != null)
			{
				final NpcHtmlMessage npcHtml = new NpcHtmlMessage(getObjectId());
				npcHtml.setHtml(player, html);
				npcHtml.replace("%eventIcon%", event.getIcon());
				npcHtml.replace("%eventName%", player.getEventName(event.getId()));
				npcHtml.replace("%eventDesc%", player.getEventDescr(event.getId()));
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
						if (player.getFightEventGameRoom() != null)
						{
							register = "<font color=\"FF0000\">You are already registered for other event!</font>";
						}
						else
						{
							register = "<button value=\"Register to Event\" action=\"bypass -h npc_%objectId%_regPlayer " + event.getId() + "\" back=\"L2UI_CT1.OlympiadWnd_DF_Fight3None_Down\" width=200 height=30 fore=\"L2UI_CT1.OlympiadWnd_DF_Fight3None\">";
						}
					}
				}
				npcHtml.replace("%eventRegister%", register);
				npcHtml.replace("%objectId%", String.valueOf(getObjectId()));
				player.sendPacket(npcHtml);
			}
		}
	}
}