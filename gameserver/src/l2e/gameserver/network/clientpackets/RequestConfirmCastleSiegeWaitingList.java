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
import l2e.gameserver.instancemanager.CastleManager;
import l2e.gameserver.model.Clan;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.entity.Castle;
import l2e.gameserver.network.serverpackets.CastleSiegeDefenderList;

public final class RequestConfirmCastleSiegeWaitingList extends GameClientPacket
{
	private int _approved;
	private int _castleId;
	private int _clanId;

	@Override
	protected void readImpl()
	{
		_castleId = readD();
		_clanId = readD();
		_approved = readD();
	}

	@Override
	protected void runImpl()
	{
		final Player activeChar = getClient().getActiveChar();
		if (activeChar == null)
		{
			return;
		}
		
		if (activeChar.getClan() == null)
		{
			return;
		}
		
		final Castle castle = CastleManager.getInstance().getCastleById(_castleId);
		if (castle == null)
		{
			return;
		}
		
		if ((castle.getOwnerId() != activeChar.getClanId()) || (!activeChar.isClanLeader()))
		{
			return;
		}
		
		final Clan clan = ClanHolder.getInstance().getClan(_clanId);
		if (clan == null)
		{
			return;
		}
		
		if (!castle.getSiege().isRegistrationTimeOver())
		{
			if (_approved == 1)
			{
				if (castle.getSiege().checkIsDefenderWaiting(clan))
				{
					castle.getSiege().approveSiegeDefenderClan(_clanId);
				}
				else
				{
					return;
				}
			}
			else
			{
				if ((castle.getSiege().checkIsDefenderWaiting(clan)) || (castle.getSiege().checkIsDefender(clan)))
				{
					castle.getSiege().removeSiegeClan(_clanId);
				}
			}
		}
		activeChar.sendPacket(new CastleSiegeDefenderList(castle));
		
	}
}