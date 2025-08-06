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

import l2e.gameserver.model.actor.Summon;

public class ExPartyPetWindowUpdate extends GameServerPacket
{
	private final Summon _summon;
	
	public ExPartyPetWindowUpdate(Summon summon)
	{
		_summon = summon;
	}
	
	@Override
	protected void writeImpl()
	{
		writeD(_summon.getObjectId());
		writeD(_summon.getTemplate().getIdTemplate() + 1000000);
		writeD(_summon.getSummonType());
		writeD(_summon.getOwner().getObjectId());
		writeS(_summon.getName(null));
		writeD((int) _summon.getCurrentHp());
		writeD((int) _summon.getMaxHp());
		writeD((int) _summon.getCurrentMp());
		writeD((int) _summon.getMaxMp());
		writeD(_summon.getLevel());
	}
}