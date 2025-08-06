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

import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.actor.Creature;

public class MagicSkillLaunched extends GameServerPacket
{
	private final int _charObjId;
	private final int _skillId;
	private final int _skillLevel;
	private int _numberOfTargets;
	private GameObject[] _targets;
	private final int _singleTargetId;
	
	public MagicSkillLaunched(Creature cha, int skillId, int skillLevel, GameObject[] targets)
	{
		_charObjId = cha.getObjectId();
		_skillId = skillId;
		_skillLevel = skillLevel;
		
		if (targets != null)
		{
			_numberOfTargets = targets.length;
			_targets = targets;
		}
		else
		{
			_numberOfTargets = 1;
			GameObject[] objs =
			{
				cha
			};
			_targets = objs;
		}
		_singleTargetId = 0;
	}
	
	public MagicSkillLaunched(Creature cha, int skillId, int skillLevel)
	{
		_charObjId = cha.getObjectId();
		_skillId = skillId;
		_skillLevel = skillLevel;
		_numberOfTargets = 1;
		_singleTargetId = cha.getTargetId();
	}
	
	@Override
	protected final void writeImpl()
	{
		writeD(_charObjId);
		writeD(_skillId);
		writeD(_skillLevel);
		writeD(_numberOfTargets);
		if (_singleTargetId != 0 || _numberOfTargets == 0)
		{
			writeD(_singleTargetId);
		}
		else
			for (GameObject target : _targets)
			{
				writeD(target.getObjectId());
			}
	}
}