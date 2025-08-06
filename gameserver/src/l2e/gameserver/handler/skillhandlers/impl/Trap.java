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

import l2e.gameserver.handler.skillhandlers.ISkillHandler;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.instance.TrapInstance;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.Quest.TrapAction;
import l2e.gameserver.model.quest.QuestEventType;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.skills.SkillType;
import l2e.gameserver.network.SystemMessageId;

public class Trap implements ISkillHandler
{
	private static final SkillType[] SKILL_IDS =
	{
	        SkillType.DETECT_TRAP, SkillType.REMOVE_TRAP
	};
	
	@Override
	public void useSkill(Creature activeChar, Skill skill, GameObject[] targets)
	{
		if ((activeChar == null) || (skill == null))
		{
			return;
		}
		
		switch (skill.getSkillType())
		{
			case DETECT_TRAP :
			{
				for (final Creature target : World.getInstance().getAroundCharacters(activeChar, skill.getAffectRange(), 200))
				{
					if (!target.isTrap())
					{
						continue;
					}
					
					if (target.isAlikeDead())
					{
						continue;
					}
					
					final TrapInstance trap = (TrapInstance) target;
					if (trap.getLevel() <= skill.getPower())
					{
						trap.setDetected(activeChar);
					}
				}
				break;
			}
			case REMOVE_TRAP :
			{
				for (final Creature target : (Creature[]) targets)
				{
					if (!target.isTrap())
					{
						continue;
					}
					
					if (target.isAlikeDead())
					{
						continue;
					}
					
					final TrapInstance trap = (TrapInstance) target;
					if (!trap.canBeSeen(activeChar))
					{
						if (activeChar.isPlayer())
						{
							activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
						}
						continue;
					}
					
					if (trap.getLevel() > skill.getPower())
					{
						continue;
					}
					
					if (trap.getTemplate().getEventQuests(QuestEventType.ON_TRAP_ACTION) != null)
					{
						for (final Quest quest : trap.getTemplate().getEventQuests(QuestEventType.ON_TRAP_ACTION))
						{
							quest.notifyTrapAction(trap, activeChar, TrapAction.TRAP_DISARMED);
						}
					}
					
					trap.unSummon();
					if (activeChar.isPlayer())
					{
						activeChar.sendPacket(SystemMessageId.A_TRAP_DEVICE_HAS_BEEN_STOPPED);
					}
				}
			}
		}
	}
	
	@Override
	public SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}