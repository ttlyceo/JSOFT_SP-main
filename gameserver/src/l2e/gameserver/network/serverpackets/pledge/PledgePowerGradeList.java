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

import java.util.ArrayList;
import java.util.List;

import l2e.gameserver.model.Clan;
import l2e.gameserver.model.Clan.RankPrivs;
import l2e.gameserver.network.serverpackets.GameServerPacket;

public class PledgePowerGradeList extends GameServerPacket
{
	private final List<RankPrivs> _privs = new ArrayList<>();

	public PledgePowerGradeList(Clan clan)
	{
		for (final RankPrivs priv : clan.getAllRankPrivs())
		{
			priv.setParty(clan.countMembersByRank(priv.getRank()));
			_privs.add(priv);
		}
	}
	
	@Override
	protected final void writeImpl()
	{
		writeD(_privs.size());
		for (final RankPrivs temp : _privs)
		{
			writeD(temp.getRank());
			writeD(temp.getParty());
		}
	}
}