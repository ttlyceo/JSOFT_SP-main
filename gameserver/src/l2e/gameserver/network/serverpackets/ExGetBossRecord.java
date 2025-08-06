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

import java.util.List;

public class ExGetBossRecord extends GameServerPacket
{
	private final List<BossRecordInfo> _bossRecordInfo;
	private final int _ranking;
	private final int _totalPoints;
	
	public ExGetBossRecord(int ranking, int totalScore, List<BossRecordInfo> bossRecordInfo)
	{
		_ranking = ranking;
		_totalPoints = totalScore;
		_bossRecordInfo = bossRecordInfo;
	}
	
	public static class BossRecordInfo
	{
		public int _bossId;
		public int _points;
		public int _unk1;
		
		public BossRecordInfo(int bossId, int points, int unk1)
		{
			_bossId = bossId;
			_points = points;
			_unk1 = unk1;
		}
	}
	
	@Override
	protected void writeImpl()
	{
		writeD(_ranking);
		writeD(_totalPoints);
		writeD(_bossRecordInfo.size());
		for (final BossRecordInfo w : _bossRecordInfo)
		{
			writeD(w._bossId);
			writeD(w._points);
			writeD(w._unk1);
		}
	}
}