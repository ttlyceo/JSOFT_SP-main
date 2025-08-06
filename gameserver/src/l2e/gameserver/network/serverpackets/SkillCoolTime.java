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


import l2e.gameserver.model.TimeStamp;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.skills.Skill;

public class SkillCoolTime extends GameServerPacket
{
	private final List<TimeStamp> _skillReuseTimeStamps = new ArrayList<>();

	public SkillCoolTime(Player cha)
	{
		for (final TimeStamp ts : cha.getSkillReuseTimeStamps().values())
		{
			if (ts.hasNotPassed() && (ts.getRemaining() / 1000 > 0))
			{
				_skillReuseTimeStamps.add(ts);
			}
		}
		
		for (final Skill sk : cha.getBlockSkills())
		{
			if (sk != null)
			{
				final long reuse = 172800000L;
				_skillReuseTimeStamps.add(new TimeStamp(sk, reuse, (System.currentTimeMillis() + reuse)));
			}
		}
	}
	
	@Override
	protected void writeImpl()
	{
		writeD(_skillReuseTimeStamps.size());
		for (final TimeStamp ts : _skillReuseTimeStamps)
		{
			writeD(ts.getSkillId());
			writeD(ts.getSkillLvl());
			writeD((int) ts.getReuse() / 1000);
			writeD((int) ts.getRemaining() / 1000);
		}
	}
}