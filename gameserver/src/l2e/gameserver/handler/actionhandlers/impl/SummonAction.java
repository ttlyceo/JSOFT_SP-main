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
package l2e.gameserver.handler.actionhandlers.impl;

import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.geodata.GeoEngine;
import l2e.gameserver.handler.actionhandlers.IActionHandler;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.GameObject.InstanceType;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.Summon;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.PetStatusShow;

public class SummonAction implements IActionHandler
{
	@Override
	public boolean action(Player activeChar, GameObject target, boolean interact, boolean shift)
	{
		if (activeChar.isLockedTarget() && (activeChar.getLockedTarget() != target))
		{
			activeChar.sendPacket(SystemMessageId.FAILED_CHANGE_TARGET);
			return false;
		}
		
		if ((activeChar == ((Summon) target).getOwner()) && (activeChar.getTarget() == target))
		{
			activeChar.sendPacket(new PetStatusShow((Summon) target));
			activeChar.updateNotMoveUntil();
			activeChar.sendActionFailed();
		}
		else if (activeChar.getTarget() != target)
		{
			activeChar.setTarget(target);
		}
		else if (interact)
		{
			if (target.isAutoAttackable(activeChar, false))
			{
				if (GeoEngine.getInstance().canSeeTarget(activeChar, target) || shift)
				{
					activeChar.getAI().setIntention(CtrlIntention.ATTACK, target, shift);
					activeChar.onActionRequest();
				}
			}
			else
			{
				activeChar.sendActionFailed();
				if (((Summon) target).isInsideRadius(activeChar, 150, false, false))
				{
					activeChar.updateNotMoveUntil();
				}
				else
				{
					if (GeoEngine.getInstance().canSeeTarget(activeChar, target))
					{
						activeChar.getAI().setIntention(CtrlIntention.FOLLOW, target);
					}
				}
			}
		}
		return true;
	}

	@Override
	public InstanceType getInstanceType()
	{
		return InstanceType.Summon;
	}
}