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
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.handler.skillhandlers.ISkillHandler;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.Seed;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.instance.MonsterInstance;
import l2e.gameserver.model.quest.Quest.QuestSound;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.skills.SkillType;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.SystemMessage;

public class Sow implements ISkillHandler
{
	private static final SkillType[] SKILL_IDS =
	{
	        SkillType.SOW
	};

	@Override
	public void useSkill(Creature activeChar, Skill skill, GameObject[] targets)
	{
		if (!activeChar.isPlayer())
		{
			return;
		}
		
		final GameObject[] targetList = skill.getTargetList(activeChar);
		if ((targetList == null) || (targetList.length == 0))
		{
			return;
		}
		
		MonsterInstance target;
		for (final GameObject tgt : targetList)
		{
			if (!tgt.isMonster())
			{
				continue;
			}
			
			target = (MonsterInstance) tgt;
			if (target.isDead() || target.isSeeded() || (target.getSeederId() != activeChar.getObjectId()))
			{
				activeChar.sendActionFailed();
				continue;
			}
			
			final Seed seed = target.getSeed();
			if (seed == null)
			{
				activeChar.sendActionFailed();
				continue;
			}
			
			if (!activeChar.destroyItemByItemId("Consume", seed.getSeedId(), 1, target, false))
			{
				activeChar.sendActionFailed();
				return;
			}
			
			SystemMessage sm;
			if (calcSuccess(activeChar, target, seed))
			{
				activeChar.sendPacket(QuestSound.ITEMSOUND_QUEST_ITEMGET.getPacket());
				target.setSeeded(activeChar.getActingPlayer());
				sm = SystemMessage.getSystemMessage(SystemMessageId.THE_SEED_WAS_SUCCESSFULLY_SOWN);
			}
			else
			{
				sm = SystemMessage.getSystemMessage(SystemMessageId.THE_SEED_WAS_NOT_SOWN);
			}
			
			if (activeChar.getParty() == null)
			{
				activeChar.sendPacket(sm);
			}
			else
			{
				activeChar.getParty().broadCast(sm);
			}
			
			target.getAI().setIntention(CtrlIntention.IDLE);
		}
	}
	
	private boolean calcSuccess(Creature activeChar, Creature target, Seed seed)
	{
		final int minlevelSeed = seed.getLevel() - 5;
		final int maxlevelSeed = seed.getLevel() + 5;
		final int levelPlayer = activeChar.getLevel();
		final int levelTarget = target.getLevel();
		int basicSuccess = seed.isAlternative() ? 20 : 90;
		
		if (levelTarget < minlevelSeed)
		{
			basicSuccess -= 5 * (minlevelSeed - levelTarget);
		}
		if (levelTarget > maxlevelSeed)
		{
			basicSuccess -= 5 * (levelTarget - maxlevelSeed);
		}
		
		int diff = (levelPlayer - levelTarget);
		if (diff < 0)
		{
			diff = -diff;
		}
		if (diff > 5)
		{
			basicSuccess -= 5 * (diff - 5);
		}
		Math.max(basicSuccess, 1);
		return Rnd.nextInt(99) < basicSuccess;
	}
	
	@Override
	public SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}