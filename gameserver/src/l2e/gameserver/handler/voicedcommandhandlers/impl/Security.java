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
package l2e.gameserver.handler.voicedcommandhandlers.impl;

import l2e.gameserver.Config;
import l2e.gameserver.handler.voicedcommandhandlers.IVoicedCommandHandler;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.strings.server.ServerMessage;
import l2e.gameserver.network.communication.AuthServerCommunication;
import l2e.gameserver.network.communication.gameserverpackets.ChangeAllowedHwid;
import l2e.gameserver.network.communication.gameserverpackets.ChangeAllowedIp;
import l2e.gameserver.network.serverpackets.MagicSkillUse;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;

public class Security implements IVoicedCommandHandler
{
	private final String[] _commandList =
	{
	        "lock", "lockIp", "lockHwid"
	};

	@Override
	public boolean useVoicedCommand(String command, Player activeChar, String target)
	{
		if (!Config.ALLOW_SECURITY_COMMAND)
		{
			return false;
		}
		
		final var lockIp = activeChar.getClient().getLockedIp();
		final var lockHwid = activeChar.getClient().getLockedHwid();
		if (command.equalsIgnoreCase("lock"))
		{
			final NpcHtmlMessage html = new NpcHtmlMessage(activeChar.getObjectId());
			html.setFile(activeChar, activeChar.getLang(), "data/html/mods/lock/lock.htm");
			html.replace("%ipButton%", lockIp.isEmpty() ? new ServerMessage("Security.LOCK_IP", activeChar.getLang()).toString() : new ServerMessage("Security.UNLOCK_IP", activeChar.getLang()).toString());
			html.replace("%hwidButton%", lockHwid.isEmpty() ? new ServerMessage("Security.LOCK_HWID", activeChar.getLang()).toString() : new ServerMessage("Security.UNLOCK_HWID", activeChar.getLang()).toString());
			activeChar.sendPacket(html);
			return true;
		}
		else if (command.equalsIgnoreCase("lockIp"))
		{
			if(!Config.ALLOW_IP_LOCK)
			{
				activeChar.sendMessage((new ServerMessage("Security.DISABLED", activeChar.getLang())).toString());
				useVoicedCommand("lock", activeChar, target);
				return true;
			}
			
			if (!activeChar.checkFloodProtection("SECURITY", "security_delay"))
			{
				return false;
			}
			
			if (lockIp.isEmpty())
			{
				AuthServerCommunication.getInstance().sendPacket(new ChangeAllowedIp(activeChar.getAccountName(), activeChar.getIPAddress()));
				activeChar.sendMessage((new ServerMessage("Security.LOCK_BY_IP", activeChar.getLang())).toString());
				activeChar.broadcastPacket(new MagicSkillUse(activeChar, activeChar, 5662, 1, 1000, 0));
				activeChar.getClient().setLockedIp(activeChar.getIPAddress());
			}
			else
			{
				AuthServerCommunication.getInstance().sendPacket(new ChangeAllowedIp(activeChar.getAccountName(), ""));
				activeChar.sendMessage((new ServerMessage("Security.UNLOCK_BY_IP", activeChar.getLang())).toString());
				activeChar.broadcastPacket(new MagicSkillUse(activeChar, activeChar, 6802, 1, 1000, 0));
				activeChar.getClient().setLockedIp("");
			}
			useVoicedCommand("lock", activeChar, target);
			return true;
		}
		else if (command.equalsIgnoreCase("lockHwid"))
		{
			if (!Config.ALLOW_HWID_LOCK)
			{
				activeChar.sendMessage((new ServerMessage("Security.DISABLED", activeChar.getLang())).toString());
				useVoicedCommand("lock", activeChar, target);
				return true;
			}
			
			if (!activeChar.checkFloodProtection("SECURITY", "security_delay"))
			{
				return false;
			}
			
			if (lockHwid.isEmpty())
			{
				AuthServerCommunication.getInstance().sendPacket(new ChangeAllowedHwid(activeChar.getAccountName(), activeChar.getHWID()));
				activeChar.sendMessage((new ServerMessage("Security.LOCK_BY_HWID", activeChar.getLang())).toString());
				activeChar.broadcastPacket(new MagicSkillUse(activeChar, activeChar, 5662, 1, 1000, 0));
				activeChar.getClient().setLockedHwid(activeChar.getHWID());
			}
			else
			{
				AuthServerCommunication.getInstance().sendPacket(new ChangeAllowedHwid(activeChar.getAccountName(), ""));
				activeChar.sendMessage((new ServerMessage("Security.UNLOCK_BY_HWID", activeChar.getLang())).toString());
				activeChar.broadcastPacket(new MagicSkillUse(activeChar, activeChar, 6802, 1, 1000, 0));
				activeChar.getClient().setLockedHwid("");
			}
			useVoicedCommand("lock", activeChar, target);
			return true;
		}
		return false;
	}

	@Override
	public String[] getVoicedCommandList()
	{
		return _commandList;
	}
}