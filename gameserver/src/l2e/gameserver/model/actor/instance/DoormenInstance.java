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

import l2e.gameserver.data.parser.DoorParser;
import l2e.gameserver.data.parser.TeleLocationParser;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.TeleportTemplate;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;

public class DoormenInstance extends NpcInstance
{
	public DoormenInstance(int objectID, NpcTemplate template)
	{
		super(objectID, template);
		setInstanceType(InstanceType.DoormenInstance);
	}
	
	@Override
	public void onBypassFeedback(Player player, String command)
	{
		if (command.startsWith("Chat"))
		{
			showChatWindow(player);
			return;
		}
		else if (command.startsWith("open_doors"))
		{
			if (isOwnerClan(player))
			{
				if (isUnderSiege())
				{
					cannotManageDoors(player);
				}
				else
				{
					openDoors(player, command);
				}
			}
			return;
		}
		else if (command.startsWith("close_doors"))
		{
			if (isOwnerClan(player))
			{
				if (isUnderSiege())
				{
					cannotManageDoors(player);
				}
				else
				{
					closeDoors(player, command);
				}
			}
			return;
		}
		else if (command.startsWith("tele"))
		{
			if (isOwnerClan(player))
			{
				doTeleport(player, command);
			}
			return;
		}
		super.onBypassFeedback(player, command);
	}

	@Override
	public void showChatWindow(Player player)
	{
		player.sendActionFailed();

		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());

		if (!isOwnerClan(player))
		{
			html.setFile(player, player.getLang(), "data/html/doormen/" + getTemplate().getId() + "-no.htm");
		}
		else
		{
			html.setFile(player, player.getLang(), "data/html/doormen/" + getTemplate().getId() + ".htm");
		}

		html.replace("%objectId%", String.valueOf(getObjectId()));
		player.sendPacket(html);
	}

	protected void openDoors(Player player, String command)
	{
		final StringTokenizer st = new StringTokenizer(command.substring(10), ", ");
		st.nextToken();

		while (st.hasMoreTokens())
		{
			DoorParser.getInstance().getDoor(Integer.parseInt(st.nextToken())).openMe();
		}
	}

	protected void closeDoors(Player player, String command)
	{
		final StringTokenizer st = new StringTokenizer(command.substring(11), ", ");
		st.nextToken();

		while (st.hasMoreTokens())
		{
			DoorParser.getInstance().getDoor(Integer.parseInt(st.nextToken())).closeMe();
		}
	}

	protected void cannotManageDoors(Player player)
	{
		player.sendActionFailed();

		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(player, player.getLang(), "data/html/doormen/" + getTemplate().getId() + "-busy.htm");
		player.sendPacket(html);
	}

	protected void doTeleport(Player player, String command)
	{
		final int whereTo = Integer.parseInt(command.substring(5).trim());
		final TeleportTemplate list = TeleLocationParser.getInstance().getTemplate(whereTo);
		if (list != null)
		{
			if (!player.isAlikeDead())
			{
				player.teleToLocation(list.getLocation(), false, ReflectionManager.DEFAULT);
			}
		}
		else
		{
			_log.warn("No teleport destination with id:" + whereTo);
		}

		player.sendActionFailed();
	}

	protected boolean isOwnerClan(Player player)
	{
		return true;
	}

	protected boolean isUnderSiege()
	{
		return false;
	}
}