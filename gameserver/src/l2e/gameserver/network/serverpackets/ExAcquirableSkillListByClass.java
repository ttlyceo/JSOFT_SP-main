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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import l2e.gameserver.model.base.AcquireSkillType;
import l2e.gameserver.network.ServerPacketOpcodes;

public final class ExAcquirableSkillListByClass extends GameServerPacket
{
	private final List<Skill> _skills;
	private final AcquireSkillType _skillType;

	private static class Skill
	{
		public int id;
		public int getLvl;
		public int nextLevel;
		public int maxLevel;
		public int spCost;
		public int requirements;

		public Skill(int pId, int gtLvl, int pNextLevel, int pMaxLevel, int pSpCost, int pRequirements)
		{
			id = pId;
			getLvl = gtLvl;
			nextLevel = pNextLevel;
			maxLevel = pMaxLevel;
			spCost = pSpCost;
			requirements = pRequirements;
		}
	}

	public ExAcquirableSkillListByClass(AcquireSkillType type)
	{
		_skillType = type;
		_skills = new ArrayList<>();
	}

	public void addSkill(int id, int gtLvl, int nextLevel, int maxLevel, int spCost, int requirements)
	{
		_skills.add(new Skill(id, gtLvl, nextLevel, maxLevel, spCost, requirements));
	}
	
	private static class SortSkillInfo implements Comparator<Skill>
	{
		@Override
		public int compare(Skill o1, Skill o2)
		{
			return o1.getLvl - o2.getLvl;
		}
	}

	@Override
	protected void writeImpl()
	{
		if (_skills.isEmpty())
		{
			return;
		}
		Collections.sort(_skills, new SortSkillInfo());
		
		writeD(_skillType.ordinal());
		writeD(_skills.size());
		for (final Skill temp : _skills)
		{
			writeD(temp.id);
			writeD(temp.nextLevel);
			writeD(temp.maxLevel);
			writeD(temp.spCost);
			writeD(temp.requirements);
			if (_skillType == AcquireSkillType.SUBPLEDGE)
			{
				writeD(2002);
			}
		}
	}
	
	@Override
	protected ServerPacketOpcodes getOpcodes()
	{
		return ServerPacketOpcodes.AcquireSkillList;
	}
}