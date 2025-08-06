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

import l2e.gameserver.model.Clan;
import l2e.gameserver.model.ClanMember;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.clientpackets.GameClientPacket;
import l2e.gameserver.network.serverpackets.SystemMessage;
import l2e.gameserver.network.serverpackets.pledge.PledgeReceiveMemberInfo;
import l2e.gameserver.network.serverpackets.pledge.PledgeShowMemberListUpdate;

public final class RequestPledgeSetAcademyMaster extends GameClientPacket
{
	private String _currPlayerName;
	private int _set;
	private String _targetPlayerName;
	
	@Override
	protected void readImpl()
	{
		_set = readD();
		_currPlayerName = readS();
		_targetPlayerName = readS();
	}
	
	@Override
	protected void runImpl()
	{
		final Player activeChar = getClient().getActiveChar();
		final Clan clan = activeChar.getClan();
		if (clan == null)
		{
			return;
		}

		if ((activeChar.getClanPrivileges() & Clan.CP_CL_APPRENTICE) != Clan.CP_CL_APPRENTICE)
		{
			activeChar.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_THE_RIGHT_TO_DISMISS_AN_APPRENTICE);
			return;
		}

		final ClanMember currentMember = clan.getClanMember(_currPlayerName);
		final ClanMember targetMember = clan.getClanMember(_targetPlayerName);
		if ((currentMember == null) || (targetMember == null))
		{
			return;
		}

		ClanMember apprenticeMember, sponsorMember;
		if (currentMember.getPledgeType() == Clan.SUBUNIT_ACADEMY)
		{
			apprenticeMember = currentMember;
			sponsorMember = targetMember;
		}
		else
		{
			apprenticeMember = targetMember;
			sponsorMember = currentMember;
		}

		final Player apprentice = apprenticeMember.getPlayerInstance();
		final Player sponsor = sponsorMember.getPlayerInstance();

		SystemMessage sm = null;
		if (_set == 0)
		{
			if (apprentice != null)
			{
				apprentice.setSponsor(0);
			}
			else
			{
				apprenticeMember.setApprenticeAndSponsor(0, 0);
			}

			if (sponsor != null)
			{
				sponsor.setApprentice(0);
			}
			else
			{
				sponsorMember.setApprenticeAndSponsor(0, 0);
			}

			apprenticeMember.saveApprenticeAndSponsor(0, 0);
			sponsorMember.saveApprenticeAndSponsor(0, 0);
			clan.broadcastToOnlineMembers(new PledgeShowMemberListUpdate(currentMember));
			sm = SystemMessage.getSystemMessage(SystemMessageId.S2_CLAN_MEMBER_C1_APPRENTICE_HAS_BEEN_REMOVED);
		}
		else
		{
			if ((apprenticeMember.getSponsor() != 0) || (sponsorMember.getApprentice() != 0) || (apprenticeMember.getApprentice() != 0) || (sponsorMember.getSponsor() != 0))
			{
				activeChar.sendMessage("Remove previous connections first.");
				return;
			}
			if (apprentice != null)
			{
				apprentice.setSponsor(sponsorMember.getObjectId());
			}
			else
			{
				apprenticeMember.setApprenticeAndSponsor(0, sponsorMember.getObjectId());
			}

			if (sponsor != null)
			{
				sponsor.setApprentice(apprenticeMember.getObjectId());
			}
			else
			{
				sponsorMember.setApprenticeAndSponsor(apprenticeMember.getObjectId(), 0);
			}

			apprenticeMember.saveApprenticeAndSponsor(0, sponsorMember.getObjectId());
			sponsorMember.saveApprenticeAndSponsor(apprenticeMember.getObjectId(), 0);
			clan.broadcastToOnlineMembers(new PledgeShowMemberListUpdate(currentMember));
			sm = SystemMessage.getSystemMessage(SystemMessageId.S2_HAS_BEEN_DESIGNATED_AS_APPRENTICE_OF_CLAN_MEMBER_S1);
		}
		sm.addString(sponsorMember.getName());
		sm.addString(apprenticeMember.getName());
		if ((sponsor != activeChar) && (sponsor != apprentice))
		{
			activeChar.sendPacket(sm);
		}
		if (sponsor != null)
		{
			sponsor.sendPacket(sm);
		}
		if (apprentice != null)
		{
			apprentice.sendPacket(sm);
			apprentice.broadcastUserInfo(false);
		}
		activeChar.sendPacket(new PledgeReceiveMemberInfo(sponsorMember));
	}
}