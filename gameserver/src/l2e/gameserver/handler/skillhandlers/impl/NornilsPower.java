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
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.instance.DoorInstance;
import l2e.gameserver.model.actor.templates.reflection.ReflectionWorld;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.skills.SkillType;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.SystemMessage;

public class NornilsPower implements ISkillHandler
{
	private static final SkillType[] SKILL_IDS =
	{
	        SkillType.NORNILS_POWER
	};

	@Override
	public void useSkill(Creature activeChar, Skill skill, GameObject[] targets)
	{
		if (!activeChar.isPlayer())
		{
			return;
		}
		
		ReflectionWorld world = null;

		final int instanceId = activeChar.getReflectionId();
		if (instanceId > 0)
		{
			world = ReflectionManager.getInstance().getPlayerWorld(activeChar.getActingPlayer());
		}

		if ((world != null) && (world.getReflectionId() == instanceId) && (world.getTemplateId() == 11))
		{
			if (activeChar.isInsideRadius(-107393, 83677, 100, true))
			{
				activeChar.destroyItemByItemId("NornilsPower", 9713, 1, activeChar, true);
				final DoorInstance door = world.getReflection().getDoor(16200010);
				if (door != null)
				{
					door.setMeshIndex(1);
					door.setTargetable(true);
					door.broadcastStatusUpdate();
				}
			}
			else
			{
				final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
				sm.addSkillName(skill);
				activeChar.sendPacket(sm);
			}
		}
		else
		{
			activeChar.sendPacket(SystemMessageId.NOTHING_HAPPENED);
		}
	}

	@Override
	public SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}