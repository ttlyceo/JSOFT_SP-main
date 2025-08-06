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
package l2e.gameserver.handler.usercommandhandlers.impl;

import l2e.gameserver.handler.usercommandhandlers.IUserCommandHandler;
import l2e.gameserver.model.Party;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.SystemMessage;

public class PartyInfo implements IUserCommandHandler
{
	private static final int[] COMMAND_IDS =
	{
	        81
	};

	@Override
	public boolean useUserCommand(int id, Player activeChar)
	{
		if (id != COMMAND_IDS[0])
		{
			return false;
		}
		
		activeChar.sendPacket(SystemMessageId.PARTY_INFORMATION);
		if (activeChar.isInParty())
		{
			final Party party = activeChar.getParty();
			switch (party.getLootDistribution())
			{
				case Party.ITEM_LOOTER :
					activeChar.sendPacket(SystemMessageId.LOOTING_FINDERS_KEEPERS);
					break;
				case Party.ITEM_ORDER :
					activeChar.sendPacket(SystemMessageId.LOOTING_BY_TURN);
					break;
				case Party.ITEM_ORDER_SPOIL :
					activeChar.sendPacket(SystemMessageId.LOOTING_BY_TURN_INCLUDE_SPOIL);
					break;
				case Party.ITEM_RANDOM :
					activeChar.sendPacket(SystemMessageId.LOOTING_RANDOM);
					break;
				case Party.ITEM_RANDOM_SPOIL :
					activeChar.sendPacket(SystemMessageId.LOOTING_RANDOM_INCLUDE_SPOIL);
					break;
			}
			
			if (!party.isLeader(activeChar))
			{
				final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.PARTY_LEADER_C1);
				sm.addPcName(party.getLeader());
				activeChar.sendPacket(sm);
			}
			activeChar.sendMessage("Members: " + party.getMemberCount() + "/9");
		}
		activeChar.sendPacket(SystemMessageId.FRIEND_LIST_FOOTER);
		return true;
	}
	
	@Override
	public int[] getUserCommandList()
	{
		return COMMAND_IDS;
	}
}