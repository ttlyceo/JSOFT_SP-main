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

public class ExVoteSystemInfo extends GameServerPacket
{
	private final int _recHave;
	private final int _recLeft;
	private final int _time;
	private final int _bonus;
	private final int _paused;

	public ExVoteSystemInfo(Player player)
	{
		_recHave = player.getRecommendation().getRecomHave();
		_recLeft = player.getRecommendation().getRecomLeft();
		_time = player.getRecommendation().isHourglassBonusActive() > 0 ? (int) player.getRecommendation().isHourglassBonusActive() : player.getRecommendation().getRecomTimeLeft();
		_bonus = player.getRecommendation().getRecomExpBonus();
		_paused = (_time == 0 || player.getRecommendation().isRecBonusActive() || player.getRecommendation().isHourglassBonusActive() > 0 ? 0 : 1);
	}
	
	@Override
	protected void writeImpl()
	{
		writeD(_recLeft);
		writeD(_recHave);
		writeD(_time);
		writeD(_bonus);
		writeD(_paused);
	}
}