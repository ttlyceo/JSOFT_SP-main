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

import l2e.gameserver.handler.targethandlers.ITargetTypeHandler;
import l2e.gameserver.instancemanager.FortManager;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.entity.Fort;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.skills.targets.TargetType;

public class FlagPole implements ITargetTypeHandler
{
	@Override
	public GameObject[] getTargetList(Skill skill, Creature activeChar, boolean onlyFirst, Creature target)
	{
		if (!activeChar.isPlayer())
		{
			return EMPTY_TARGET_LIST;
		}

		final Player player = activeChar.getActingPlayer();
		final Fort fort = FortManager.getInstance().getFort(player);
		if ((player.getClan() == null) || (fort == null) || !player.checkIfOkToCastFlagDisplay(fort, true, skill, activeChar.getTarget()))
		{
			return EMPTY_TARGET_LIST;
		}
		
		return new GameObject[]
		{
		        activeChar.getTarget()
		};
	}
	
	@Override
	public Enum<TargetType> getTargetType()
	{
		return TargetType.FLAGPOLE;
	}
}
