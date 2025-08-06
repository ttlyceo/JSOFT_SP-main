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

import l2e.gameserver.Config;
import l2e.gameserver.data.holder.ClanHolder;
import l2e.gameserver.model.Clan;
import l2e.gameserver.network.SystemMessageId;

public final class RequestStopPledgeWar extends GameClientPacket
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
		final var player = getClient().getActiveChar();
		if (player == null)
		{
			return;
		}
		final var playerClan = player.getClan();
		if (playerClan == null)
		{
			return;
		}
		
		final var clan = ClanHolder.getInstance().getClanByName(_pledgeName);
		if (clan == null)
		{
			player.sendMessage("No such clan.");
			player.sendActionFailed();
			return;
		}
		
		if (!playerClan.isAtWarWith(clan.getId()))
		{
			player.sendMessage("You aren't at war with this clan.");
			player.sendActionFailed();
			return;
		}
		
		if ((player.getClanPrivileges() & Clan.CP_CL_PLEDGE_WAR) != Clan.CP_CL_PLEDGE_WAR)
		{
			player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
			return;
		}
		
		for (final var member : playerClan.getMembers())
		{
			if ((member == null) || (member.getPlayerInstance() == null))
			{
				continue;
			}
			
			final var pl = member.getPlayerInstance();
			if (pl != null && (Config.STOP_WAR_PVP ? pl.getPvpFlag() > 0 : pl.isInCombat()))
			{
				player.sendPacket(SystemMessageId.CANT_STOP_CLAN_WAR_WHILE_IN_COMBAT);
				return;
			}
		}
		
		ClanHolder.getInstance().deleteclanswars(playerClan.getId(), clan.getId());
		playerClan.getOnlineMembers(0).forEach(member -> member.broadcastCharInfo());
		clan.getOnlineMembers(0).forEach(member -> member.broadcastCharInfo());
	}
}