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
package l2e.gameserver.network.clientpackets.pledge;

import l2e.gameserver.instancemanager.CastleManager;
import l2e.gameserver.instancemanager.ClanHallManager;
import l2e.gameserver.instancemanager.FortManager;
import l2e.gameserver.model.Clan;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.clientpackets.GameClientPacket;
import l2e.gameserver.network.serverpackets.JoinPledge;
import l2e.gameserver.network.serverpackets.SystemMessage;
import l2e.gameserver.network.serverpackets.pledge.PledgeShowInfoUpdate;
import l2e.gameserver.network.serverpackets.pledge.PledgeShowMemberListAdd;
import l2e.gameserver.network.serverpackets.pledge.PledgeShowMemberListAll;

public final class RequestAnswerJoinPledge extends GameClientPacket
{
	private int _answer;
	
	@Override
	protected void readImpl()
	{
		_answer = readD();
	}

	@Override
	protected void runImpl()
	{
		final Player activeChar = getClient().getActiveChar();
		if (activeChar == null)
		{
			return;
		}
		
		final Player requestor = activeChar.getRequest().getPartner();
		if (requestor == null)
		{
			return;
		}

		if (_answer == 0)
		{
			SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.YOU_DID_NOT_RESPOND_TO_S1_CLAN_INVITATION);
			sm.addString(requestor.getName(null));
			activeChar.sendPacket(sm);
			sm = null;
			sm = SystemMessage.getSystemMessage(SystemMessageId.S1_DID_NOT_RESPOND_TO_CLAN_INVITATION);
			sm.addString(activeChar.getName(null));
			requestor.sendPacket(sm);
			sm = null;
		}
		else
		{
			if (!(requestor.getRequest().getRequestPacket() instanceof RequestJoinPledge))
			{
				return;
			}

			final RequestJoinPledge requestPacket = (RequestJoinPledge) requestor.getRequest().getRequestPacket();
			final Clan clan = requestor.getClan();

			if (clan.checkClanJoinCondition(requestor, activeChar, requestPacket.getPledgeType()))
			{
				activeChar.sendPacket(new JoinPledge(requestor.getClanId()));

				activeChar.setPledgeType(requestPacket.getPledgeType());
				if (requestPacket.getPledgeType() == Clan.SUBUNIT_ACADEMY)
				{
					activeChar.setPowerGrade(9);
					activeChar.setLvlJoinedAcademy(activeChar.getLevel());
				}
				else
				{
					activeChar.setPowerGrade(6);
				}

				clan.addClanMember(activeChar);
				activeChar.setClanPrivileges(activeChar.getClan().getRankPrivs(activeChar.getPowerGrade()));

				activeChar.sendPacket(SystemMessageId.ENTERED_THE_CLAN);

				SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_HAS_JOINED_CLAN);
				sm.addString(activeChar.getName(null));
				clan.broadcastToOnlineMembers(sm);
				sm = null;

				if (activeChar.getClan().getCastleId() > 0)
				{
					CastleManager.getInstance().getCastleByOwner(activeChar.getClan()).giveResidentialSkills(activeChar);
				}
				if (activeChar.getClan().getFortId() > 0)
				{
					FortManager.getInstance().getFortByOwner(activeChar.getClan()).giveResidentialSkills(activeChar);
				}
				if (activeChar.getClan().getHideoutId() > 0)
				{
					final var hall = ClanHallManager.getInstance().getAbstractHallByOwner(activeChar.getClan());
					if (hall != null)
					{
						hall.giveResidentialSkills(activeChar);
					}
				}
				activeChar.sendSkillList(false);

				clan.broadcastToOtherOnlineMembers(new PledgeShowMemberListAdd(activeChar), activeChar);
				clan.broadcastToOnlineMembers(new PledgeShowInfoUpdate(clan));

				activeChar.sendPacket(new PledgeShowMemberListAll(clan, activeChar));
				activeChar.setClanJoinExpiryTime(0);
				activeChar.broadcastCharInfo();
			}
		}

		activeChar.getRequest().onRequestResponse();
	}
}