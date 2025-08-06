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

import l2e.gameserver.instancemanager.CHSiegeManager;
import l2e.gameserver.instancemanager.CastleManager;
import l2e.gameserver.instancemanager.FortManager;
import l2e.gameserver.instancemanager.TerritoryWarManager;
import l2e.gameserver.model.AccessLevel;
import l2e.gameserver.model.Clan;
import l2e.gameserver.model.SiegeClan;
import l2e.gameserver.model.actor.Creature;

public class Die extends GameServerPacket
{
	private final int _charObjId;
	private final boolean _canTeleport;
	private final boolean _sweepable;
	private AccessLevel _access = null;
	private boolean _haveItem = false;
	private Clan _clan;
	private final Creature _activeChar;
	private boolean _isJailed;

	public Die(Creature cha)
	{
		_charObjId = cha.getObjectId();
		_activeChar = cha;
		_canTeleport = cha.canRevive();
		_sweepable = cha.isAttackable() && (cha.isSweepActive() || cha.isPassiveSweepActive());
		if (cha.isPlayer())
		{
			final var player = cha.getActingPlayer();
			_access = player.getAccessLevel();
			_clan = player.getClan();
			_haveItem = player.getInventory().hasSelfResurrection() && !player.isInFightEvent();;
			_isJailed = player.isJailed();
		}
	}
	
	@Override
	protected final void writeImpl()
	{
		writeD(_charObjId);
		writeD(_canTeleport ? 0x01 : 0x00);
		if (_canTeleport && (_clan != null) && !_isJailed)
		{
			boolean isInCastleDefense = false;
			boolean isInFortDefense = false;
			
			SiegeClan siegeClan = null;
			final var castle = CastleManager.getInstance().getCastle(_activeChar);
			final var fort = FortManager.getInstance().getFort(_activeChar);
			final var hall = CHSiegeManager.getInstance().getNearbyClanHall(_activeChar);
			if ((castle != null) && castle.getSiege().getIsInProgress())
			{
				siegeClan = castle.getSiege().getAttackerClan(_clan);
				if ((siegeClan == null) && castle.getSiege().checkIsDefender(_clan))
				{
					isInCastleDefense = true;
				}
			}
			else if ((fort != null) && fort.getSiege().getIsInProgress())
			{
				siegeClan = fort.getSiege().getAttackerClan(_clan);
				if ((siegeClan == null) && fort.getSiege().checkIsDefender(_clan))
				{
					isInFortDefense = true;
				}
			}
			
			writeD(_clan.getHideoutId() > 0 ? 0x01 : 0x00);
			writeD((_clan.getCastleId() > 0) || isInCastleDefense ? 0x01 : 0x00);
			final var tw = TerritoryWarManager.getInstance();
			writeD((tw.getHQForClan(_clan) != null) || (tw.getFlagForClan(_clan) != null) || ((siegeClan != null) && !isInCastleDefense && !isInFortDefense && !siegeClan.getFlag().isEmpty()) || ((hall != null) && hall.getSiege().checkIsAttacker(_clan)) ? 0x01 : 0x00);
			writeD(_sweepable ? 0x01 : 0x00);
			writeD(_access != null && _access.allowFixedRes() || _haveItem ? 0x01 : 0x00);
			writeD((_clan.getFortId() > 0) || isInFortDefense ? 0x01 : 0x00);
		}
		else
		{
			writeD(0x00);
			writeD(0x00);
			writeD(0x00);
			writeD(_sweepable ? 0x01 : 0x00);
			writeD(_access != null && _access.allowFixedRes() || _haveItem ? 0x01 : 0x00);
			writeD(0x00);
		}
	}
}