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

import l2e.gameserver.model.Clan;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.AskJoinAlliance;
import l2e.gameserver.network.serverpackets.SystemMessage;

public final class RequestJoinAlly extends GameClientPacket
{
	private int _id;

	@Override
	protected void readImpl()
	{
		_id = readD();
	}
	
	@Override
	protected void runImpl()
	{
		final Player activeChar = getClient().getActiveChar();
		if (activeChar == null)
		{
			return;
		}

		final Player ob = GameObjectsStorage.getPlayer(_id);

		if (ob == null)
		{
			activeChar.sendPacket(SystemMessageId.YOU_HAVE_INVITED_THE_WRONG_TARGET);
			return;
		}

		if (activeChar.getClan() == null)
		{
			activeChar.sendPacket(SystemMessageId.YOU_ARE_NOT_A_CLAN_MEMBER);
			return;
		}
		
		if (activeChar.isInFightEvent() && !activeChar.getFightEvent().canReceiveInvitations(activeChar, ob))
		{
			activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.C1_IS_BUSY_TRY_LATER).addString(ob.getName(null)));
			return;
		}

		final Player target = ob;
		final Clan clan = activeChar.getClan();
		if (!clan.checkAllyJoinCondition(activeChar, target))
		{
			return;
		}
		if (!activeChar.getRequest().setRequest(target, this))
		{
			return;
		}

		SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S2_ALLIANCE_LEADER_OF_S1_REQUESTED_ALLIANCE);
		sm.addString(activeChar.getClan().getAllyName());
		sm.addString(activeChar.getName(null));
		target.sendPacket(sm);
		sm = null;
		final AskJoinAlliance aja = new AskJoinAlliance(activeChar.getObjectId(), activeChar.getName(null), activeChar.getClan().getAllyName());
		target.sendPacket(aja);
	}
}