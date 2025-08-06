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
package l2e.gameserver.network.clientpackets;

import l2e.commons.util.GMAudit;
import l2e.gameserver.Config;
import l2e.gameserver.data.parser.AdminParser;
import l2e.gameserver.handler.admincommandhandlers.AdminCommandHandler;
import l2e.gameserver.handler.admincommandhandlers.IAdminCommandHandler;
import l2e.gameserver.handler.admincommandhandlers.impl.Teleports;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.holders.DoorRequestHolder;
import l2e.gameserver.model.holders.SummonRequestHolder;
import l2e.gameserver.network.SystemMessageId;

public final class ConfirmDlg extends GameClientPacket
{
	private int _messageId;
	private int _answer;
	private int _requesterId;

	@Override
	protected void readImpl()
	{
		_messageId = readD();
		_answer = readD();
		_requesterId = readD();
	}

	@Override
	public void runImpl()
	{
		final Player activeChar = getClient().getActiveChar();
		if (activeChar == null)
		{
			return;
		}
		
		if (_messageId == SystemMessageId.S1.getId())
		{
			final String _command = activeChar.getAdminConfirmCmd();
			if (_command == null)
			{
				activeChar.scriptAswer(_answer);
			}
			else
			{
				activeChar.setAdminConfirmCmd(null);
				if (_answer == 0)
				{
					return;
				}
				final String command = _command.split(" ")[0];
				final IAdminCommandHandler ach = AdminCommandHandler.getInstance().getHandler(command);
				if (AdminParser.getInstance().hasAccess(command, activeChar.getAccessLevel()))
				{
					if (Config.GMAUDIT)
					{
						GMAudit.auditGMAction(activeChar.getName(null) + " [" + activeChar.getObjectId() + "]", _command, (activeChar.getTarget() != null ? activeChar.getTarget().getName(null) : "no-target"));
					}
					ach.useAdminCommand(_command, activeChar);
				}
			}
		}
		else if ((_messageId == SystemMessageId.RESSURECTION_REQUEST_BY_C1_FOR_S2_XP.getId()) || (_messageId == SystemMessageId.RESURRECT_USING_CHARM_OF_COURAGE.getId()))
		{
			activeChar.reviveAnswer(_answer);
		}
		else if (_messageId == SystemMessageId.C1_WISHES_TO_SUMMON_YOU_FROM_S2_DO_YOU_ACCEPT.getId())
		{
			final SummonRequestHolder holder = activeChar.removeScript(SummonRequestHolder.class);
			if ((_answer == 1) && (holder != null) && (holder.getTarget().getObjectId() == _requesterId))
			{
				if (holder.isAdminRecall())
				{
					Teleports.teleportCharacter(activeChar, holder.getTarget().getX(), holder.getTarget().getY(), holder.getTarget().getZ(), holder.getTarget());
				}
				else
				{
					activeChar.teleToLocation(holder.getTarget().getX(), holder.getTarget().getY(), holder.getTarget().getZ(), true, activeChar.getReflection());
				}
			}
		}
		else if (_messageId == SystemMessageId.WOULD_YOU_LIKE_TO_OPEN_THE_GATE.getId())
		{
			final DoorRequestHolder holder = activeChar.removeScript(DoorRequestHolder.class);
			if ((holder != null) && (holder.getDoor() == activeChar.getTarget()) && (_answer == 1))
			{
				holder.getDoor().openMe();
			}
		}
		else if (_messageId == SystemMessageId.WOULD_YOU_LIKE_TO_CLOSE_THE_GATE.getId())
		{
			final DoorRequestHolder holder = activeChar.removeScript(DoorRequestHolder.class);
			if ((holder != null) && (holder.getDoor() == activeChar.getTarget()) && (_answer == 1))
			{
				holder.getDoor().closeMe();
			}
		}
	}
}