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
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.skills.SkillType;
import l2e.gameserver.model.skills.effects.Effect;
import l2e.gameserver.model.skills.effects.EffectType;

public class Detection implements ISkillHandler
{
	private static final SkillType[] SKILL_IDS =
	{
	        SkillType.DETECTION
	};

	@Override
	public void useSkill(Creature activeChar, Skill skill, GameObject[] targets)
	{
		final boolean hasParty;
		final boolean hasClan;
		final boolean hasAlly;
		final Player player = activeChar.getActingPlayer();
		if (player != null)
		{
			hasParty = player.isInParty();
			hasClan = player.getClanId() > 0;
			hasAlly = player.getAllyId() > 0;
			
			for (final Player target : World.getInstance().getAroundPlayers(activeChar, skill.getAffectRange(), 200))
			{
				if ((target != null) && target.isInvisible())
				{
					if (hasParty && (target.getParty() != null) && (player.getParty().getLeaderObjectId() == target.getParty().getLeaderObjectId()))
					{
						continue;
					}
					if (hasClan && (player.getClanId() == target.getClanId()))
					{
						continue;
					}
					if (hasAlly && (player.getAllyId() == target.getAllyId()))
					{
						continue;
					}
					
					final Effect eHide = target.getFirstEffect(EffectType.HIDE);
					if (eHide != null)
					{
						eHide.exit();
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