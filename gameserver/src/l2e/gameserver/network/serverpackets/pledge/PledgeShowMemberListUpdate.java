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

import l2e.gameserver.model.Clan;
import l2e.gameserver.model.ClanMember;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.network.serverpackets.GameServerPacket;

public final class PledgeShowMemberListUpdate extends GameServerPacket
{
	private final int _pledgeType;
	private int _hasSponsor;
	private final String _name;
	private final int _level;
	private final int _classId;
	private final int _objectId;
	private final int _isOnline;
	private final int _sex;
	
	public PledgeShowMemberListUpdate(Player player)
	{
		_pledgeType = player.getPledgeType();
		if (_pledgeType == Clan.SUBUNIT_ACADEMY)
		{
			_hasSponsor = player.getSponsor() != 0 ? 1 : 0;
		}
		else
		{
			_hasSponsor = 0;
		}
		_name = player.getName(null);
		_level = player.getLevel();
		_classId = player.getClassId().getId();
		_sex = player.getAppearance().getSex() ? 1 : 0;
		_objectId = player.getObjectId();
		_isOnline = player.isOnline() ? 1 : 0;
	}

	public PledgeShowMemberListUpdate(ClanMember member)
	{
		_name = member.getName();
		_level = member.getLevel();
		_classId = member.getClassId();
		_objectId = member.getObjectId();
		_isOnline = member.isOnline() ? 1 : 0;
		_pledgeType = member.getPledgeType();
		_sex = member.getSex() ? 1 : 0;
		if (_pledgeType == Clan.SUBUNIT_ACADEMY)
		{
			_hasSponsor = member.getSponsor() != 0 ? 1 : 0;
		}
		else
		{
			_hasSponsor = 0;
		}
	}

	@Override
	protected final void writeImpl()
	{
		writeS(_name);
		writeD(_level);
		writeD(_classId);
		writeD(_sex);
		writeD(_objectId);
		writeD(_isOnline);
		writeD(_pledgeType);
		writeD(_hasSponsor);
	}
}