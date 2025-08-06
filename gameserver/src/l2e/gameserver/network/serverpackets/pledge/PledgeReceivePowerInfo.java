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

public class PledgeReceivePowerInfo extends GameServerPacket
{
	private final ClanMember _member;

	public PledgeReceivePowerInfo(ClanMember member)
	{
		_member = member;
	}
	
	@Override
	protected void writeImpl()
	{
		writeD(_member.getPowerGrade());
		writeS(_member.getName());
		writeD(_member.getClan().getRankPrivs(_member.getPowerGrade()));
	}
}