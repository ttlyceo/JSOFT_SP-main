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

import l2e.commons.util.Util;
import l2e.gameserver.handler.targethandlers.ITargetTypeHandler;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.skills.targets.TargetType;

public class ClanMember implements ITargetTypeHandler
{
	@Override
	public GameObject[] getTargetList(Skill skill, Creature activeChar, boolean onlyFirst, Creature target)
	{
		final List<Creature> targetList = new ArrayList<>();
		if (activeChar.isNpc())
		{
			final Npc npc = (Npc) activeChar;
			if (npc.getFaction().isNone())
			{
				return new Creature[]
				{
				        activeChar
				};
			}
			
			for (final Npc newTarget : World.getInstance().getAroundNpc(activeChar))
			{
				if (newTarget.isAttackable() && npc.isInFaction((newTarget)))
				{
					if (!Util.checkIfInRange(skill.getCastRange(), activeChar, newTarget, true))
					{
						continue;
					}
					if (newTarget.getFirstEffect(skill) != null)
					{
						continue;
					}
					targetList.add(newTarget);
					break;
				}
			}
			if (targetList.isEmpty())
			{
				targetList.add(npc);
			}
		}
		else
		{
			return EMPTY_TARGET_LIST;
		}
		return targetList.toArray(new Creature[targetList.size()]);
	}
	
	@Override
	public Enum<TargetType> getTargetType()
	{
		return TargetType.CLAN_MEMBER;
	}
}