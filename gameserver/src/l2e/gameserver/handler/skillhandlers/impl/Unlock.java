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
package l2e.gameserver.handler.skillhandlers.impl;

import l2e.commons.util.Rnd;
import l2e.gameserver.handler.skillhandlers.ISkillHandler;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.ChestInstance;
import l2e.gameserver.model.actor.instance.DoorInstance;
import l2e.gameserver.model.entity.Reflection;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.skills.SkillType;
import l2e.gameserver.network.SystemMessageId;

public class Unlock implements ISkillHandler
{
	private static final SkillType[] SKILL_IDS =
	{
	        SkillType.UNLOCK, SkillType.UNLOCK_SPECIAL
	};
	
	@Override
	public void useSkill(Creature activeChar, Skill skill, GameObject[] targets)
	{
		for (final GameObject target : targets)
		{
			if (target.isDoor())
			{
				DoorInstance door = (DoorInstance) target;
				
				if (activeChar.getReflectionId() != door.getReflectionId())
				{
					final Reflection inst = ReflectionManager.getInstance().getReflection(activeChar.getReflectionId());
					if (inst == null)
					{
						activeChar.sendActionFailed();
						return;
					}
					final DoorInstance instanceDoor = inst.getDoor(door.getDoorId());
					if (instanceDoor != null)
					{
						door = instanceDoor;
					}
					if (activeChar.getReflectionId() != door.getReflectionId())
					{
						activeChar.sendActionFailed();
						return;
					}
				}
				
				if ((!door.isOpenableBySkill() && (skill.getSkillType() != SkillType.UNLOCK_SPECIAL)) || (door.getFort() != null))
				{
					activeChar.sendPacket(SystemMessageId.UNABLE_TO_UNLOCK_DOOR);
					activeChar.sendActionFailed();
					return;
				}
				
				if (doorUnlock(skill) && !door.getOpen())
				{
					door.openMe();
				}
				else
				{
					activeChar.sendPacket(SystemMessageId.FAILED_TO_UNLOCK_DOOR);
				}
			}
			else if (target instanceof ChestInstance)
			{
				final ChestInstance chest = (ChestInstance) target;
				if ((chest.getCurrentHp() <= 0) || (activeChar.getReflectionId() != chest.getReflectionId()))
				{
					activeChar.sendActionFailed();
					return;
				}

				final ChestInstance targ = (ChestInstance) target;
				if (!targ.isDead())
				{
					targ.tryOpen((Player) activeChar, skill);
				}
			}
		}
	}
	
	private static final boolean doorUnlock(Skill skill)
	{
		if (skill.getSkillType() == SkillType.UNLOCK_SPECIAL)
		{
			return Rnd.get(100) < skill.getPower();
		}
		
		switch (skill.getLevel())
		{
			case 0 :
				return false;
			case 1 :
				return Rnd.get(120) < 30;
			case 2 :
				return Rnd.get(120) < 50;
			case 3 :
				return Rnd.get(120) < 75;
			default :
				return Rnd.get(120) < 100;
		}
	}
	
	@Override
	public SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}