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
import l2e.gameserver.handler.communityhandlers.CommunityBoardHandler;
import l2e.gameserver.instancemanager.PunishmentManager;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.punishment.PunishmentAffect;
import l2e.gameserver.model.punishment.PunishmentSort;
import l2e.gameserver.model.punishment.PunishmentType;

public class Punishment implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
	        "admin_punishment", "admin_punishment_add", "admin_bch", "admin_punishment_target_add", "admin_punishment_target_remove"
	};

	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		final StringTokenizer st = new StringTokenizer(command, " ");
		if (!st.hasMoreTokens())
		{
			return false;
		}
		final String cmd = st.nextToken();
		switch (cmd)
		{
			case "admin_punishment" :
			{
				final var handler = CommunityBoardHandler.getInstance().getHandler("_bbspunishment");
				if (handler != null)
				{
					handler.onBypassCommand("_bbspunishment", activeChar);
				}
				break;
			}
			case "admin_bch" :
			{
				final String name = st.hasMoreTokens() ? st.nextToken() : null;
				final String exp = st.hasMoreTokens() ? st.nextToken() : null;
				String reason = st.hasMoreTokens() ? st.nextToken() : null;
				if (reason != null)
				{
					while (st.hasMoreTokens())
					{
						reason += " " + st.nextToken();
					}
					if (!reason.isEmpty())
					{
						reason = reason.replaceAll("\\$", "\\\\\\$");
						reason = reason.replaceAll("\r\n", "<br1>");
						reason = reason.replace("<", "&lt;");
						reason = reason.replace(">", "&gt;");
					}
				}
				else
				{
					reason = "";
				}
				
				if (name == null || exp == null)
				{
					activeChar.sendMessage("Please fill all the fields!");
					break;
				}
				if (!Util.isDigit(exp) && !exp.equals("-1"))
				{
					activeChar.sendMessage("Incorrect value specified for expiration time!");
					break;
				}
				
				long expirationTime = Integer.parseInt(exp);
				if (expirationTime > 0)
				{
					expirationTime = System.currentTimeMillis() + (expirationTime * 60 * 1000);
				}
				
				final var handler = CommunityBoardHandler.getInstance().getHandler("_bbsaddpunishment");
				if (handler != null)
				{
					handler.onBypassCommand("_bbsaddpunishment " + name + " " + PunishmentSort.CHARACTER + " " + PunishmentAffect.CHARACTER + " " + PunishmentType.CHAT_BAN + " " + exp + " " + reason + "", activeChar);
				}
				break;
			}
			case "admin_punishment_add" :
			{
				final String name = st.hasMoreTokens() ? st.nextToken() : null;
				final String srt = st.hasMoreTokens() ? st.nextToken() : null;
				final String af = st.hasMoreTokens() ? st.nextToken() : null;
				final String t = st.hasMoreTokens() ? st.nextToken() : null;
				final String exp = st.hasMoreTokens() ? st.nextToken() : null;
				String reason = st.hasMoreTokens() ? st.nextToken() : null;
				if (reason != null)
				{
					while (st.hasMoreTokens())
					{
						reason += " " + st.nextToken();
					}
					if (!reason.isEmpty())
					{
						reason = reason.replaceAll("\\$", "\\\\\\$");
						reason = reason.replaceAll("\r\n", "<br1>");
						reason = reason.replace("<", "&lt;");
						reason = reason.replace(">", "&gt;");
					}
				}
				else
				{
					reason = "";
				}
				
				if ((name == null) || (af == null) || (t == null) || (exp == null))
				{
					activeChar.sendMessage("Please fill all the fields!");
					break;
				}
				if (!Util.isDigit(exp) && !exp.equals("-1"))
				{
					activeChar.sendMessage("Incorrect value specified for expiration time!");
					break;
				}
				
				long expirationTime = Integer.parseInt(exp);
				if (expirationTime > 0)
				{
					expirationTime = System.currentTimeMillis() + (expirationTime * 60 * 1000);
				}
				
				final var sort = PunishmentSort.getByName(srt);
				final var affect = PunishmentAffect.getByName(af);
				final var type = PunishmentType.getByName(t);
				if ((sort == null) || (affect == null) || (type == null))
				{
					activeChar.sendMessage("Incorrect value specified for sort/affect/punishment type!");
					break;
				}
				
				final var handler = CommunityBoardHandler.getInstance().getHandler("_bbsaddpunishment");
				if (handler != null)
				{
					handler.onBypassCommand("_bbsaddpunishment " + name + " " + srt + " " + af + " " + t + " " + exp + " " + reason + "", activeChar);
				}
			}
			case "admin_punishment_target_add" :
			{
				if ((activeChar.getTarget() == null) || !activeChar.getTarget().isPlayer())
				{
					activeChar.sendMessage("You must target player!");
					break;
				}
				final Player target = activeChar.getTarget().getActingPlayer();
				
				final String srt = st.hasMoreTokens() ? st.nextToken() : null;
				final String af = st.hasMoreTokens() ? st.nextToken() : null;
				final String t = st.hasMoreTokens() ? st.nextToken() : null;
				final String exp = st.hasMoreTokens() ? st.nextToken() : null;
				String reason = st.hasMoreTokens() ? st.nextToken() : null;
				if (reason != null)
				{
					while (st.hasMoreTokens())
					{
						reason += " " + st.nextToken();
					}
					if (!reason.isEmpty())
					{
						reason = reason.replaceAll("\\$", "\\\\\\$");
						reason = reason.replaceAll("\r\n", "<br1>");
						reason = reason.replace("<", "&lt;");
						reason = reason.replace(">", "&gt;");
					}
				}
				else
				{
					reason = "";
				}
				
				if ((target == null) || (af == null) || (t == null) || (exp == null))
				{
					activeChar.sendMessage("Please fill all the fields!");
					break;
				}
				if (!Util.isDigit(exp) && !exp.equals("-1"))
				{
					activeChar.sendMessage("Incorrect value specified for expiration time!");
					break;
				}
				
				long expirationTime = Integer.parseInt(exp);
				if (expirationTime > 0)
				{
					expirationTime = System.currentTimeMillis() + (expirationTime * 60 * 1000);
				}
				
				final var sort = PunishmentSort.getByName(srt);
				final var affect = PunishmentAffect.getByName(af);
				final var type = PunishmentType.getByName(t);
				if ((sort == null) || (affect == null) || (type == null))
				{
					activeChar.sendMessage("Incorrect value specified for sort/affect/punishment type!");
					break;
				}
				
				final var handler = CommunityBoardHandler.getInstance().getHandler("_bbsaddpunishment");
				if (handler != null)
				{
					handler.onBypassCommand("_bbsaddpunishment " + target.getName(activeChar.getLang()) + " " + srt + " " + af + " " + t + " " + exp + " " + reason + "", activeChar);
				}
			}
			case "admin_punishment_target_remove" :
			{
				final String af = st.hasMoreTokens() ? st.nextToken() : null;
				final String t = st.hasMoreTokens() ? st.nextToken() : null;
				String name = st.hasMoreTokens() ? st.nextToken() : null;
				
				if (name == null && (activeChar.getTarget() != null && activeChar.getTarget().isPlayer()))
				{
					name = activeChar.getTarget().getActingPlayer().getName(activeChar.getLang());
				}
				
				if ((name == null) || (af == null) || (t == null))
				{
					activeChar.sendMessage("Not enough data specified!");
					break;
				}
				
				final var affect = PunishmentAffect.getByName(af);
				final var type = PunishmentType.getByName(t);
				if ((affect == null) || (type == null))
				{
					activeChar.sendMessage("Incorrect value specified for affect/punishment type!");
					break;
				}
				
				final var tpl = PunishmentManager.getInstance().getPunishmentTemplate(name, type, affect);
				if (tpl != null)
				{
					final var handler = CommunityBoardHandler.getInstance().getHandler("_bbsremovepunishment");
					if (handler != null)
					{
						handler.onBypassCommand("_bbsremovepunishment " + tpl.getId() + " " + tpl.getSort().name() + " " + tpl.getAffect().name() + " " + tpl.getType().name(), activeChar);
					}
				}
				else
				{
					activeChar.sendMessage("Target is not affected by that punishment!");
				}
			}
		}
		return true;
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}