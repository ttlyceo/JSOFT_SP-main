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
package l2e.gameserver.network.serverpackets;

import java.util.ArrayList;
import java.util.List;

import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.skills.effects.Effect;

public class PartySpelled extends GameServerPacket
{
	private final List<Effect> _effects = new ArrayList<>();
	private final Creature _activeChar;
	
	public PartySpelled(Creature cha)
	{
		_activeChar = cha;
	}
	
	public void addSkill(Effect info)
	{
		_effects.add(info);
	}
	
	@Override
	protected final void writeImpl()
	{
		writeD(_activeChar.isServitor() ? 0x02 : _activeChar.isPet() ? 0x01 : 0x00);
		writeD(_activeChar.getObjectId());
		writeD(_effects.size());
		for (final Effect info : _effects)
		{
			if ((info != null) && info.isInUse())
			{
				writeD(info.getSkill().getDisplayId());
				writeH(info.getSkill().getDisplayLevel());
				writeD(info.getTimeLeft());
			}
		}
	}
}