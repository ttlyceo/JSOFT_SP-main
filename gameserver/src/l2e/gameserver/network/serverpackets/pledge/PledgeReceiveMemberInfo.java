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
package l2e.gameserver.network.serverpackets.pledge;

import l2e.gameserver.model.ClanMember;
import l2e.gameserver.network.serverpackets.GameServerPacket;

public class PledgeReceiveMemberInfo extends GameServerPacket
{
	private final ClanMember _member;

	public PledgeReceiveMemberInfo(ClanMember member)
	{
		_member = member;
	}

	@Override
	protected void writeImpl()
	{
		writeD(_member.getPledgeType());
		writeS(_member.getName());
		writeS(_member.getTitle());
		writeD(_member.getPowerGrade());
		if (_member.getPledgeType() != 0)
		{
			writeS((_member.getClan().getSubPledge(_member.getPledgeType())).getName());
		}
		else
		{
			writeS(_member.getClan().getName());
		}
		writeS(_member.getApprenticeOrSponsorName());
	}
}