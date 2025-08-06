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
import l2e.gameserver.instancemanager.FortManager;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.entity.Fort;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.skills.SkillType;

public class TakeFort implements ISkillHandler
{
	private static final SkillType[] SKILL_IDS =
	{
	        SkillType.TAKEFORT
	};

	@Override
	public void useSkill(Creature activeChar, Skill skill, GameObject[] targets)
	{
		if (!activeChar.isPlayer() || targets.length == 0)
		{
			return;
		}
		
		final Player player = activeChar.getActingPlayer();
		if (player.getClan() == null)
		{
			return;
		}
		
		final Fort fort = FortManager.getInstance().getFort(player);
		if (fort == null || !player.checkIfOkToCastFlagDisplay(fort, true, skill, targets[0]))
		{
			return;
		}
		
		try
		{
			fort.endOfSiege(player.getClan());
			player.getCounters().addAchivementInfo("fortSiegesWon", 0, -1, false, false, true);
		}
		catch (final Exception e)
		{
			e.printStackTrace();
		}
	}
	
	@Override
	public SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}