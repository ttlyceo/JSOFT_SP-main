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
import l2e.gameserver.data.holder.ClanHolder;
import l2e.gameserver.handler.admincommandhandlers.IAdminCommandHandler;
import l2e.gameserver.instancemanager.CastleManager;
import l2e.gameserver.instancemanager.ClanHallManager;
import l2e.gameserver.instancemanager.FortManager;
import l2e.gameserver.model.Clan;
import l2e.gameserver.model.ClanMember;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;

public class Clans implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
	        "admin_clan_info", "admin_clan_changeleader", "admin_clan_show_pending", "admin_clan_force_pending"
	};

	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		final StringTokenizer st = new StringTokenizer(command);
		final String cmd = st.nextToken();
		switch (cmd)
		{
			case "admin_clan_info" :
			{
				final Player player = getPlayer(activeChar, st);
				if (player == null)
				{
					break;
				}

				final Clan clan = player.getClan();
				if (clan == null)
				{
					activeChar.sendPacket(SystemMessageId.TARGET_MUST_BE_IN_CLAN);
					return false;
				}

				final NpcHtmlMessage html = new NpcHtmlMessage(0, 5);
				html.setFile(activeChar, activeChar.getLang(), "data/html/admin/claninfo.htm");
				html.replace("%clan_name%", clan.getName());
				html.replace("%clan_leader%", clan.getLeaderName());
				html.replace("%clan_level%", String.valueOf(clan.getLevel()));
				html.replace("%clan_has_castle%", clan.getCastleId() > 0 ? CastleManager.getInstance().getCastleById(clan.getCastleId()).getName(player.getLang()) : "No");
				html.replace("%clan_has_clanhall%", clan.getHideoutId() > 0 ? Util.clanHallName(activeChar, ClanHallManager.getInstance().getClanHallById(clan.getHideoutId()).getId()) : "No");
				html.replace("%clan_has_fortress%", clan.getFortId() > 0 ? FortManager.getInstance().getFortById(clan.getFortId()).getName() : "No");
				html.replace("%clan_points%", String.valueOf(clan.getReputationScore()));
				html.replace("%clan_players_count%", String.valueOf(clan.getMembersCount()));
				html.replace("%clan_ally%", clan.getAllyId() > 0 ? clan.getAllyName() : "Not in ally");
				html.replace("%current_player_objectId%", String.valueOf(player.getObjectId()));
				html.replace("%current_player_name%", player.getName(null));
				activeChar.sendPacket(html);
				break;
			}
			case "admin_clan_changeleader" :
			{
				final Player player = getPlayer(activeChar, st);
				if (player == null)
				{
					break;
				}

				final Clan clan = player.getClan();
				if (clan == null)
				{
					activeChar.sendPacket(SystemMessageId.TARGET_MUST_BE_IN_CLAN);
					return false;
				}

				final ClanMember member = clan.getClanMember(player.getObjectId());
				if (member != null)
				{
					if (player.isAcademyMember())
					{
						player.sendPacket(SystemMessageId.RIGHT_CANT_TRANSFERRED_TO_ACADEMY_MEMBER);
					}
					else
					{
						clan.setNewLeader(member);
					}
				}
				break;
			}
			case "admin_clan_show_pending" :
			{
				final NpcHtmlMessage html = new NpcHtmlMessage(0, 5);
				html.setFile(activeChar, activeChar.getLang(), "data/html/admin/clanchanges.htm");
				final StringBuilder sb = new StringBuilder();
				for (final Clan clan : ClanHolder.getInstance().getClans())
				{
					if (clan.getNewLeaderId() != 0)
					{
						sb.append("<tr>");
						sb.append("<td>" + clan.getName() + "</td>");
						sb.append("<td>" + clan.getNewLeaderName() + "</td>");
						sb.append("<td><a action=\"bypass -h admin_clan_force_pending " + clan.getId() + "\">Force</a></td>");
						sb.append("</tr>");
					}
				}
				html.replace("%data%", sb.toString());
				activeChar.sendPacket(html);
				break;
			}
			case "admin_clan_force_pending" :
			{
				if (st.hasMoreElements())
				{
					final String token = st.nextToken();
					if (!Util.isDigit(token))
					{
						break;
					}
					final int clanId = Integer.parseInt(token);

					final Clan clan = ClanHolder.getInstance().getClan(clanId);
					if (clan == null)
					{
						break;
					}

					final ClanMember member = clan.getClanMember(clan.getNewLeaderId());
					if (member == null)
					{
						break;
					}
					clan.setNewLeader(member);
					activeChar.sendMessage("Task have been forcely executed.");
					break;
				}
			}
		}
		return true;
	}

	private Player getPlayer(Player activeChar, StringTokenizer st)
	{
		String val;
		Player player = null;
		if (st.hasMoreTokens())
		{
			val = st.nextToken();
			if (Util.isDigit(val))
			{
				player = GameObjectsStorage.getPlayer(Integer.parseInt(val));
				if (player == null)
				{
					activeChar.sendPacket(SystemMessageId.TARGET_IS_NOT_FOUND_IN_THE_GAME);
					return null;
				}
			}
			else
			{
				player = GameObjectsStorage.getPlayer(val);
				if (player == null)
				{
					activeChar.sendPacket(SystemMessageId.INCORRECT_NAME_TRY_AGAIN);
					return null;
				}
			}
		}
		else
		{
			final GameObject targetObj = activeChar.getTarget();
			if (targetObj instanceof Player)
			{
				player = targetObj.getActingPlayer();
			}
			else
			{
				activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
				return null;
			}
		}
		return player;
	}

	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}