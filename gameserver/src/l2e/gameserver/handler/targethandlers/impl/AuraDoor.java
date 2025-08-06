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
package l2e.gameserver.handler.targethandlers.impl;

import java.util.ArrayList;
import java.util.List;

import l2e.commons.apache.ArrayUtils;
import l2e.gameserver.handler.targethandlers.ITargetTypeHandler;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.instance.DoorInstance;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.skills.targets.TargetType;

public class AuraDoor implements ITargetTypeHandler
{
	private final int[] _doors = new int[]
	{
	        19210001, 19210002, 19210003, 19210004, 20160001, 20160002, 20160007, 20160008, 20160009, 20220001, 20220002, 20220003, 20220004, 22130001, 22130002, 22130004, 22130005, 22190001, 22190002, 22190003, 22190004, 23220001, 23220002, 23220003, 23220004, 23250001, 23250002, 23250003, 23250004, 24160009, 24160010, 24160021, 24160022, 24180001, 24180002, 24180006, 24180011
	};
	
	@Override
	public GameObject[] getTargetList(Skill skill, Creature activeChar, boolean onlyFirst, Creature target)
	{
		final List<Creature> targetList = new ArrayList<>();
		final int maxTargets = skill.getAffectLimit();
		for (final var obj : World.getInstance().getAroundDoors(activeChar, skill.getAffectRange()))
		{
			if (obj == null || !checkTarget(obj))
			{
				continue;
			}
			
			if ((maxTargets > 0) && (targetList.size() >= maxTargets))
			{
				break;
			}
			targetList.add(obj);
		}
		
		if (targetList.isEmpty())
		{
			return EMPTY_TARGET_LIST;
		}
		return targetList.toArray(new Creature[targetList.size()]);
	}
	
	private boolean checkTarget(DoorInstance door)
	{
		if (door.isDead() || door.isOpen())
		{
			return false;
		}
		
		if (ArrayUtils.contains(_doors, door.getId()))
		{
			return true;
		}
		return false;
	}
	
	@Override
	public Enum<TargetType> getTargetType()
	{
		return TargetType.AURA_DOOR;
	}
}