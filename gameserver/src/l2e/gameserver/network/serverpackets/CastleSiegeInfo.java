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

import java.util.Calendar;

import l2e.commons.util.Util;
import l2e.gameserver.data.holder.ClanHolder;
import l2e.gameserver.instancemanager.CHSiegeManager;
import l2e.gameserver.model.entity.Castle;
import l2e.gameserver.model.entity.ClanHall;

public class CastleSiegeInfo extends GameServerPacket
{
	private Castle _castle;
	private ClanHall _hall;
	
	public CastleSiegeInfo(Castle castle)
	{
		_castle = castle;
	}
	
	public CastleSiegeInfo(ClanHall hall)
	{
		_hall = hall;
	}
	
	@Override
	protected final void writeImpl()
	{
		final var activeChar = getClient().getActiveChar();
		if (activeChar == null)
		{
			return;
		}
		
		if (_castle != null)
		{
			final var line = _castle.getTemplate() != null && _castle.getTemplate().getCorrectSiegeTime() != null && !_castle.getTemplate().getCorrectSiegeTime().isEmpty() ? _castle.getTemplate().getCorrectSiegeTime() : null;
			final var canChangeSiege = (!_castle.getSiege().isTimeChangeDataOver() && activeChar.isClanLeader() && (activeChar.getClanId() == _castle.getOwnerId())) && line != null;
			writeD(_castle.getId());
			final int ownerId = _castle.getOwnerId();
			writeD(((ownerId == activeChar.getClanId()) && (activeChar.isClanLeader())) ? 0x01 : 0x00);
			writeD(ownerId);
			if (ownerId > 0)
			{
				final var owner = ClanHolder.getInstance().getClan(ownerId);
				if (owner != null)
				{
					writeS(owner.getName());
					writeS(owner.getLeaderName());
					writeD(owner.getAllyId());
					writeS(owner.getAllyName());
				}
				else
				{
					_log.warn("Null owner for castle: " + _castle.getName(null));
				}
			}
			else
			{
				writeS("");
				writeS("");
				writeD(0x00);
				writeS("");
			}
			writeD((int) (System.currentTimeMillis() / 1000));
			if (canChangeSiege)
			{
				final var cal = Calendar.getInstance();
				cal.setTimeInMillis(_castle.getSiege().getSiegeStartTime());
				cal.set(Calendar.MINUTE, 0);
				cal.set(Calendar.SECOND, 0);
				writeD(0x00);
				writeD(line.split(",").length);
				for (final String hour : line.split(","))
				{
					cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(hour));
					writeD((int) (cal.getTimeInMillis() / 1000));
				}
			}
			else
			{
				writeD((int) (_castle.getSiege().getSiegeStartTime() / 1000));
				writeD(0x00);
			}
		}
		else
		{
			writeD(_hall.getId());
			
			final int ownerId = _hall.getOwnerId();
			
			writeD(((ownerId == activeChar.getClanId()) && (activeChar.isClanLeader())) ? 0x01 : 0x00);
			writeD(ownerId);
			if (ownerId > 0)
			{
				final var owner = ClanHolder.getInstance().getClan(ownerId);
				if (owner != null)
				{
					writeS(owner.getName());
					writeS(owner.getLeaderName());
					writeD(owner.getAllyId());
					writeS(owner.getAllyName());
				}
				else
				{
					_log.warn("Null owner for siegable hall: " + Util.clanHallName(null, _hall.getId()));
				}
			}
			else
			{
				writeS("");
				writeS("");
				writeD(0x00);
				writeS("");
			}
			writeD((int) (Calendar.getInstance().getTimeInMillis() / 1000));
			writeD((int) ((CHSiegeManager.getInstance().getSiegableHall(_hall.getId()).getNextSiegeTime()) / 1000));
			writeD(0x00);
		}
	}
}