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

import l2e.gameserver.network.serverpackets.ExBrLoadEventTopRankers;

public class RequestExBrEventRankerList extends GameClientPacket
{
	private int _eventId;
	private int _day;
	protected int _ranking;

	@Override
	protected void readImpl()
	{
		_eventId = readD();
		_day = readD();
		_ranking = readD();
	}

	@Override
	protected void runImpl()
	{
		final int count = 0;
		final int bestScore = 0;
		final int myScore = 0;
		getClient().sendPacket(new ExBrLoadEventTopRankers(_eventId, _day, count, bestScore, myScore));
	}
}