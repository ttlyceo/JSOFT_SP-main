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
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.SystemMessage;

public final class RequestStartPledgeWar extends GameClientPacket
{
	private String _pledgeName;
	private Clan _clan;
	protected Player player;
	
	@Override
	protected void readImpl()
	{
		_pledgeName = readS();
	}
	
	@Override
	protected void runImpl()
	{
		player = getClient().getActiveChar();
		if (player == null)
		{
			return;
		}
		
		_clan = getClient().getActiveChar().getClan();
		if (_clan == null)
		{
			return;
		}
		
		if ((_clan.getLevel() < 3) || (_clan.getMembersCount() < Config.ALT_CLAN_MEMBERS_FOR_WAR))
		{
			SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.CLAN_WAR_DECLARED_IF_CLAN_LVL3_OR_15_MEMBER);
			player.sendPacket(sm);
			player.sendActionFailed();
			sm = null;
			return;
		}
		else if ((player.getClanPrivileges() & Clan.CP_CL_PLEDGE_WAR) != Clan.CP_CL_PLEDGE_WAR)
		{
			player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
			player.sendActionFailed();
			return;
		}
		
		final Clan clan = ClanHolder.getInstance().getClanByName(_pledgeName);
		if (clan == null)
		{
			final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.CLAN_WAR_CANNOT_DECLARED_CLAN_NOT_EXIST);
			player.sendPacket(sm);
			player.sendActionFailed();
			return;
		}
		else if ((_clan.getAllyId() == clan.getAllyId()) && (_clan.getAllyId() != 0))
		{
			SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.CLAN_WAR_AGAINST_A_ALLIED_CLAN_NOT_WORK);
			player.sendPacket(sm);
			player.sendActionFailed();
			sm = null;
			return;
		}
		else if ((clan.getLevel() < 3) || (clan.getMembersCount() < Config.ALT_CLAN_MEMBERS_FOR_WAR))
		{
			SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.CLAN_WAR_DECLARED_IF_CLAN_LVL3_OR_15_MEMBER);
			player.sendPacket(sm);
			player.sendActionFailed();
			sm = null;
			return;
		}
		else if (_clan.isAtWarWith(clan.getId()))
		{
			SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.ALREADY_AT_WAR_WITH_S1_WAIT_5_DAYS); // msg
			                                                                                                       // id
			                                                                                                       // 628
			sm.addString(clan.getName());
			player.sendPacket(sm);
			player.sendActionFailed();
			sm = null;
			return;
		}
		ClanHolder.getInstance().storeclanswars(player.getClanId(), clan.getId());
		_clan.getOnlineMembers(0).forEach(member -> member.broadcastCharInfo());
		clan.getOnlineMembers(0).forEach(member -> member.broadcastCharInfo());
	}
}