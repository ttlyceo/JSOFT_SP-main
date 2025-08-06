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

import l2e.gameserver.Config;
import l2e.gameserver.model.SkillLearn;
import l2e.gameserver.model.base.AcquireSkillType;
import l2e.gameserver.model.holders.ItemHolder;
import l2e.gameserver.model.skills.Skill;

public class AcquireSkillInfo extends GameServerPacket
{
	private final AcquireSkillType _type;
	private final int _id;
	private final int _level;
	private final int _spCost;
	private final List<Req> _reqs;
	
	private static class Req
	{
		public int itemId;
		public long count;
		public int type;
		public int unk;
		
		public Req(int pType, int pItemId, long itemCount, int pUnk)
		{
			itemId = pItemId;
			type = pType;
			count = itemCount;
			unk = pUnk;
		}
	}
	
	public AcquireSkillInfo(AcquireSkillType skillType, SkillLearn skillLearn)
	{
		_id = skillLearn.getId();
		_level = skillLearn.getLvl();
		_spCost = skillLearn.getLevelUpSp();
		_type = skillType;
		_reqs = new ArrayList<>();
		if ((skillType != AcquireSkillType.PLEDGE) || Config.LIFE_CRYSTAL_NEEDED)
		{
			for (final ItemHolder item : skillLearn.getRequiredItems(_type))
			{
				if (!Config.DIVINE_SP_BOOK_NEEDED && (_id == Skill.SKILL_DIVINE_INSPIRATION))
				{
					continue;
				}
				_reqs.add(new Req(99, item.getId(), item.getCount(), 50));
			}
		}
	}
	
	public AcquireSkillInfo(AcquireSkillType skillType, SkillLearn skillLearn, int sp)
	{
		_id = skillLearn.getId();
		_level = skillLearn.getLvl();
		_spCost = sp;
		_type = skillType;
		_reqs = new ArrayList<>();
		for (final ItemHolder item : skillLearn.getRequiredItems(_type))
		{
			_reqs.add(new Req(99, item.getId(), item.getCount(), 50));
		}
	}
	
	@Override
	protected final void writeImpl()
	{
		writeD(_id);
		writeD(_level);
		writeD(_spCost);
		writeD(_type.ordinal());
		writeD(_reqs.size());
		for (final Req temp : _reqs)
		{
			writeD(temp.type);
			writeD(temp.itemId);
			writeQ(temp.count);
			writeD(temp.unk);
		}
	}
}