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

import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.entity.events.tournaments.util.TournamentUtil;
import l2e.gameserver.network.serverpackets.ExReceiveOlympiadForTournamentList;
import l2e.gameserver.network.serverpackets.ExReceiveOlympiadList;

import java.util.Objects;

public class RequestOlympiadMatchList extends GameClientPacket
{
	@Override
	protected void readImpl()
	{
	}
	
	@Override
	protected void runImpl()
	{
		final Player activeChar = getClient().getActiveChar();
		if (activeChar == null)
		{
			return;
		}

		if(TournamentUtil.TOURNAMENT_MAIN.isEnable() && Objects.nonNull(activeChar.getPartyTournament()))
		{
			activeChar.sendPacket(new ExReceiveOlympiadForTournamentList.OlympiadList());
			return;
		}

		activeChar.sendPacket(new ExReceiveOlympiadList.OlympiadList());
	}
}