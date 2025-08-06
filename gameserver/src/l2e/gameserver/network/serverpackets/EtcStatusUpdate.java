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

import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.skills.effects.EffectFlag;
import l2e.gameserver.model.zone.ZoneId;

public class EtcStatusUpdate extends GameServerPacket
{
	private final Player _activeChar;

	public EtcStatusUpdate(Player activeChar)
	{
		_activeChar = activeChar;
	}
	
	@Override
	protected void writeImpl()
	{
		writeD(_activeChar.getCharges());
		writeD(_activeChar.getWeightPenalty());
		writeD((_activeChar.getMessageRefusal() || _activeChar.isChatBanned() || _activeChar.isSilenceMode()) ? 0x01 : 0x00);
		writeD(_activeChar.isInsideZone(ZoneId.DANGER_AREA) ? 0x01 : 0x00);
		writeD(_activeChar.getExpertiseWeaponPenalty());
		writeD(_activeChar.getExpertiseArmorPenalty());
		writeD(_activeChar.isAffected(EffectFlag.CHARM_OF_COURAGE) ? 0x01 : 0x00);
		writeD(_activeChar.getDeathPenaltyBuffLevel());
		writeD(_activeChar.getChargedSouls());
	}
}