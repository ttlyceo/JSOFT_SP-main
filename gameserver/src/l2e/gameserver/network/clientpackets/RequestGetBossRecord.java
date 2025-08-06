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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import l2e.gameserver.instancemanager.RaidBossSpawnManager;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.network.serverpackets.ExGetBossRecord;
import l2e.gameserver.network.serverpackets.ExGetBossRecord.BossRecordInfo;

public class RequestGetBossRecord extends GameClientPacket
{
	protected int _bossId;
	
	@Override
	protected void readImpl()
	{
		_bossId = readD();
	}

	@Override
	protected void runImpl()
	{
		final Player activeChar = getClient().getActiveChar();
		if (activeChar == null)
		{
			return;
		}
		
		int totalPoints = 0;
		int ranking = 0;
		
		final List<BossRecordInfo> list = new ArrayList<>();
		final Map<Integer, Integer> points = RaidBossSpawnManager.getInstance().getPointsForOwnerId(activeChar.getObjectId());
		if (points != null && !points.isEmpty())
		{
			for (final Map.Entry<Integer, Integer> e : points.entrySet())
			{
				switch (e.getKey())
				{
					case -1:
						ranking = e.getValue();
						break;
					case 0:
						totalPoints = e.getValue();
						break;
					default:
						list.add(new BossRecordInfo(e.getKey(), e.getValue(), 0));
				}
			}
		}
		
		activeChar.sendPacket(new ExGetBossRecord(ranking, totalPoints, list));
	}
	
	@Override
	protected boolean triggersOnActionRequest()
	{
		return false;
	}
}