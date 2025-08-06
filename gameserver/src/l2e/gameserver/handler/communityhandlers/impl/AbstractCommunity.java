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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2e.gameserver.Config;
import l2e.gameserver.data.htm.HtmCache;
import l2e.gameserver.instancemanager.ZoneManager;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.entity.events.AbstractFightEvent;
import l2e.gameserver.model.entity.events.cleft.AerialCleftEvent;
import l2e.gameserver.model.strings.server.ServerMessage;
import l2e.gameserver.model.strings.server.ServerStorage;
import l2e.gameserver.model.zone.ZoneId;
import l2e.gameserver.model.zone.type.FunPvpZone;
import l2e.gameserver.network.serverpackets.ShowBoard;
import l2e.gameserver.taskmanager.AttackStanceTaskManager;

public abstract class AbstractCommunity
{
	protected static final Logger _log = LoggerFactory.getLogger(AbstractCommunity.class);
	
	public static void separateAndSend(String html, Player player)
	{
		if (player.isInsideZone(ZoneId.PVP) && Config.BLOCK_COMMUNITY_IN_PVP_ZONE)
		{
			return;
		}
		
		for (final AbstractFightEvent e : player.getFightEvents())
		{
			if (e != null && !e.canUseCommunity())
			{
				return;
			}
		}
		
		if (html == null)
		{
			return;
		}
		html = html.replaceAll("\t", "");
		final Pattern p = Pattern.compile("%include\\(([^)]+)\\)%");
		final Matcher m = p.matcher(html);
		while (m.find())
		{
			html = html.replace(m.group(0), HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/" + m.group(1)));
		}
		
		final Pattern ps = Pattern.compile("%msg\\(([^)]+)\\)%");
		final Matcher ms = ps.matcher(html);
		while (ms.find())
		{
			html = html.replace(ms.group(0), ServerStorage.getInstance().getString(player.getLang(), ms.group(1)));
		}
		html = html.replaceAll("\n", "");
		
		if (html.contains("<!-- PLAYER -->"))
		{
			html = CommunityGeneral.getInstance().getPlayerInfo(html, player);
		}
		
		if (html.contains("<!-- SERVER -->"))
		{
			html = CommunityGeneral.getInstance().getServerInfo(html, player);
		}
		
		html = player.getBypassStorage().encodeBypasses(html, true);
		
		if (html.length() < 8180)
		{
			player.sendPacket(new ShowBoard(html, "101", player));
			player.sendPacket(new ShowBoard(null, "102", player));
			player.sendPacket(new ShowBoard(null, "103", player));

		}
		else if (html.length() < 8180 * 2)
		{
			player.sendPacket(new ShowBoard(html.substring(0, 8180), "101", player));
			player.sendPacket(new ShowBoard(html.substring(8180, html.length()), "102", player));
			player.sendPacket(new ShowBoard(null, "103", player));

		}
		else if (html.length() < 8180 * 3)
		{
			player.sendPacket(new ShowBoard(html.substring(0, 8180), "101", player));
			player.sendPacket(new ShowBoard(html.substring(8180, 8180 * 2), "102", player));
			player.sendPacket(new ShowBoard(html.substring(8180 * 2, html.length()), "103", player));
		}
        else if (html.length() < 8180 * 4)
        {
            player.sendPacket(new ShowBoard(html.substring(0, 8180), "101", player));
            player.sendPacket(new ShowBoard(html.substring(8180, 8180 * 2), "102", player));
            player.sendPacket(new ShowBoard(html.substring(8180 * 2, 8180 * 3), "103", player));
            player.sendPacket(new ShowBoard(html.substring(8180 * 3, html.length()), "104", player));
        }
	}

	protected void send1001(String html, Player acha)
	{
		if (html.length() < 8192)
		{
			acha.sendPacket(new ShowBoard(html, "1001", acha));
		}
	}

	protected void send1002(Player acha)
	{
		send1002(acha, " ", " ", "0");
	}

	protected void send1002(Player activeChar, String string, String string2, String string3)
	{
		final List<String> _arg = new ArrayList<>();
		_arg.add("0");
		_arg.add("0");
		_arg.add("0");
		_arg.add("0");
		_arg.add("0");
		_arg.add("0");
		_arg.add(activeChar.getName(null));
		_arg.add(Integer.toString(activeChar.getObjectId()));
		_arg.add(activeChar.getAccountName());
		_arg.add("9");
		_arg.add(string2);
		_arg.add(string2);
		_arg.add(string);
		_arg.add(string3);
		_arg.add(string3);
		_arg.add("0");
		_arg.add("0");
		activeChar.sendPacket(new ShowBoard(_arg));
	}

	public boolean sendHtm(Player player, String path)
	{
		String prefix = player.getLang();
		if (prefix == null)
		{
			prefix = "en";
		}
		
		String oriPath = path;
		if (prefix != null)
		{
			if (path.contains("html/"))
			{
				path = path.replace("html/", "html/" + prefix + "/");
				oriPath = oriPath.replace("html/", "html/en/");
			}
		}
		
		String content = HtmCache.getInstance().getHtm(player, path);
		if ((content == null) && !oriPath.equals(path))
		{
			content = HtmCache.getInstance().getHtm(player, oriPath);
		}
		if (content == null)
		{
			return false;
		}
		separateAndSend(content, player);
		return true;
	}
	
	private boolean isFoundBypass(List<String> list, String name)
	{
		for (final String n : list)
		{
			if (n.equalsIgnoreCase(name) || n.contains(name))
			{
				return true;
			}
		}
		return false;
	}
	
	private boolean isFoundCorrectBypass(List<String> list, String name)
	{
		final String[] lenght = name.split(";");
		final var isStartsWith = lenght != null && lenght.length > 1;
		for (final String n : list)
		{
			if ((!isStartsWith && n.equals(name)) || (isStartsWith && name.startsWith(n)))
			{
				return true;
			}
		}
		return false;
	}
	
	protected boolean checkCondition(Player player, String command, boolean isBuff, boolean isTeleport)
	{
		if (player == null)
		{
			return false;
		}
		
		for (final AbstractFightEvent e : player.getFightEvents())
		{
			if (e != null && !e.canUseCommunity())
			{
				return false;
			}
		}

		var e = player.getPartyTournament();
		if (e != null && !e.canUseCommunity())
		{
			return false;
		}
		
		if (isBlockedBypass(command) && (AttackStanceTaskManager.getInstance().hasAttackStanceTask(player) || player.isCastingNow() || player.isAttackingNow()))
		{
			player.sendMessage((new ServerMessage("Community.ALL_DISABLE", player.getLang())).toString());
			return false;
		}
		
		if (Config.DISABLE_COMMUNITY_BYPASSES_FLAG.size() > 0 && (isTeleport ? isFoundCorrectBypass(Config.DISABLE_COMMUNITY_BYPASSES_FLAG, command) : isFoundBypass(Config.DISABLE_COMMUNITY_BYPASSES_FLAG, command)) && player.getPvpFlag() > 0)
		{
			player.sendMessage((new ServerMessage("Community.ALL_DISABLE", player.getLang())).toString());
			return false;
		}
		
		if (player.isInKrateisCube() || player.getUCState() > 0 || player.isDamageBlock() || player.isBlocked() || player.isCursedWeaponEquipped() || player.isInDuel() || player.isFlying() || player.isJailed() || player.isInOlympiadMode() || player.inObserverMode() || player.isAlikeDead() || player.isDead())
		{
			player.sendMessage((new ServerMessage("Community.ALL_DISABLE", player.getLang())).toString());
			return false;
		}
		
		if (((AerialCleftEvent.getInstance().isStarted() || AerialCleftEvent.getInstance().isRewarding()) && AerialCleftEvent.getInstance().isPlayerParticipant(player.getObjectId())))
		{
			player.sendMessage((new ServerMessage("Community.ALL_DISABLE", player.getLang())).toString());
			return false;
		}
		if (player.isInsideZone(ZoneId.PVP) && !player.isInFightEvent() && !player.isInPartyTournament())
		{
			if (player.isInsideZone(ZoneId.FUN_PVP))
			{
				final FunPvpZone zone = ZoneManager.getInstance().getZone(player, FunPvpZone.class);
				if (zone != null)
				{
					if ((isBuff && !zone.canUseCbBuffs()) || (isTeleport && !zone.canUseCbTeleports()))
					{
						player.sendMessage((new ServerMessage("Community.ALL_DISABLE", player.getLang())).toString());
						return false;
					}
				}
			}
			else
			{
				if ((isBuff && player.isInsideZone(ZoneId.SIEGE) && !Config.ALLOW_COMMUNITY_BUFF_IN_SIEGE) || (isTeleport && player.isInsideZone(ZoneId.SIEGE) && !Config.ALLOW_COMMUNITY_TELEPORT_IN_SIEGE))
				{
					player.sendMessage((new ServerMessage("Community.ALL_DISABLE", player.getLang())).toString());
					return false;
				}
			}
		}
		
		if (Config.ALLOW_COMMUNITY_PEACE_ZONE)
		{
			if (!player.isInsideZone(ZoneId.PEACE) && !player.isInFightEvent() && !player.isInPartyTournament())
			{
				player.sendMessage((new ServerMessage("Community.ALL_DISABLE", player.getLang())).toString());
				return false;
			}
		}
		return true;
	}
	
	private static boolean isBlockedBypass(String bypass)
	{
		final var list = Config.DISABLE_COMMUNITY_BYPASSES_COMBAT;
		if (list == null || list.isEmpty())
		{
			return false;
		}
		for (final var cmd : list)
		{
			if (bypass.startsWith(cmd))
			{
				return true;
			}
		}
		return false;
	}
}