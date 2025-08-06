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

import l2e.gameserver.geodata.GeoEngine;
import l2e.gameserver.handler.targethandlers.ITargetTypeHandler;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.instance.SiegeFlagInstance;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.skills.targets.TargetType;

public class AuraFriendly implements ITargetTypeHandler
{
	@Override
	public GameObject[] getTargetList(Skill skill, Creature activeChar, boolean onlyFirst, Creature target)
	{
		final List<Creature> targetList = new ArrayList<>();
		final int maxTargets = skill.getAffectLimit();
		for (final var obj : World.getInstance().getAroundCharacters(activeChar, skill.getAffectRange(), 200))
		{
			if ((obj == activeChar) || !checkTarget(activeChar, obj))
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
	
	private boolean checkTarget(Creature activeChar, Creature target)
	{
		if ((target == null) || (activeChar == null) || !GeoEngine.getInstance().canSeeTarget(activeChar, target))
		{
			return false;
		}
		
		if (!activeChar.isPlayer() || target.isAlikeDead() || target.isDoor() || (target instanceof SiegeFlagInstance) || target.isMonster())
		{
			return false;
		}
		
		if (target.isPlayable())
		{
			final var targetPlayer = target.getActingPlayer();
			if (!activeChar.getActingPlayer().isFriend(targetPlayer))
			{
				return false;
			}
		}
		return true;
	}
	
	@Override
	public Enum<TargetType> getTargetType()
	{
		return TargetType.AURA_FRIENDLY;
	}
}