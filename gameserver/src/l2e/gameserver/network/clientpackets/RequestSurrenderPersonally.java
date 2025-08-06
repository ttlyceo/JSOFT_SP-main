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

import l2e.gameserver.data.holder.ClanHolder;
import l2e.gameserver.model.Clan;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.SystemMessage;

public class RequestSurrenderPersonally extends GameClientPacket
{
	private String _pledgeName;
	
	@Override
	protected void readImpl()
	{
		_pledgeName = readS();
	}
	
	@Override
	protected void runImpl()
	{
		final Player activeChar = getClient().getActiveChar();
		if (activeChar == null)
		{
			return;
		}
		
		final Clan playerClan = activeChar.getClan();
		if (playerClan == null)
		{
			return;
		}
		
		final Clan clan = ClanHolder.getInstance().getClanByName(_pledgeName);
		if (clan == null)
		{
			return;
		}
		
		if (!playerClan.isAtWarWith(clan.getId()) || activeChar.getWantsPeace() == 1)
		{
			activeChar.sendPacket(SystemMessageId.FAILED_TO_PERSONALLY_SURRENDER);
			return;
		}
		
		activeChar.setWantsPeace(1);
		activeChar.deathPenalty(null, false, false, false);
		activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_HAVE_PERSONALLY_SURRENDERED_TO_THE_S1_CLAN).addString(_pledgeName));
		ClanHolder.getInstance().checkSurrender(playerClan, clan);
	}
}