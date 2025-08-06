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
package l2e.gameserver.network.clientpackets;

import java.util.Calendar;

import l2e.gameserver.instancemanager.CastleManager;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.entity.Castle;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.CastleSiegeInfo;
import l2e.gameserver.network.serverpackets.SystemMessage;

public class RequestSetCastleSiegeTime extends GameClientPacket
{
	private int _castleId;
	private long _time;
	
	@Override
	protected void readImpl()
	{
		_castleId = readD();
		_time = readD();
		_time *= 1000;
	}
	
	@Override
	protected void runImpl()
	{
		final var activeChar = getClient().getActiveChar();
		final var castle = CastleManager.getInstance().getCastleById(_castleId);
		if ((activeChar == null) || (castle == null) || castle.getTemplate() == null || castle.getSiege() == null || !castle.getTemplate().isEnableSiege())
		{
			return;
		}
		if ((castle.getOwnerId() > 0) && (castle.getOwnerId() != activeChar.getClanId()))
		{
			return;
		}
		else if (!activeChar.isClanLeader())
		{
			return;
		}
		else if (!castle.getSiege().isTimeChangeDataOver())
		{
			if (isSiegeTimeValid(castle, _time))
			{
				castle.getSiege().setSiegeStartTime(_time);
				castle.getSiege().setIsTimeChangeDataOver(0L);
				castle.getSiege().saveSiegeDate();
				final var msg = SystemMessage.getSystemMessage(SystemMessageId.S1_ANNOUNCED_SIEGE_TIME);
				msg.addCastleId(_castleId);
				GameObjectsStorage.getPlayers().stream().filter(p -> p != null && p.isOnline()).forEach(p -> p.sendPacket(msg));
				activeChar.sendPacket(new CastleSiegeInfo(castle));
			}
		}
	}

	private static boolean isSiegeTimeValid(Castle castle, long choosenDate)
	{
		final String line = castle.getTemplate().getCorrectSiegeTime();
		if (line == null || line.isEmpty())
		{
			return false;
		}
		
		final var cal1 = Calendar.getInstance();
		cal1.setTimeInMillis(castle.getSiege().getSiegeStartTime());
		cal1.set(Calendar.MINUTE, 0);
		cal1.set(Calendar.SECOND, 0);

		final var cal2 = Calendar.getInstance();
		cal2.setTimeInMillis(choosenDate);

		for (final String hour : line.split(","))
		{
			cal1.set(Calendar.HOUR_OF_DAY, Integer.parseInt(hour));
			if (isEqual(cal1, cal2, Calendar.YEAR, Calendar.MONTH, Calendar.DAY_OF_MONTH, Calendar.HOUR, Calendar.MINUTE, Calendar.SECOND))
			{
				return true;
			}
		}
		return false;
	}

	private static boolean isEqual(Calendar cal1, Calendar cal2, int... fields)
	{
		for (final int field : fields)
		{
			if (cal1.get(field) != cal2.get(field))
			{
				return false;
			}
		}
		return true;
	}

	@Override
	public String getType()
	{
		return getClass().getSimpleName();
	}
}