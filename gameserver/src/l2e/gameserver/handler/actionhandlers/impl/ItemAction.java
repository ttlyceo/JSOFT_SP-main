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
import l2e.gameserver.handler.actionhandlers.IActionHandler;
import l2e.gameserver.instancemanager.MercTicketManager;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.GameObject.InstanceType;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.items.instance.ItemInstance;

public class ItemAction implements IActionHandler
{
	@Override
	public boolean action(Player activeChar, GameObject target, boolean interact, boolean shift)
	{
		final int castleId = MercTicketManager.getInstance().getTicketCastleId(((ItemInstance) target).getId());
		if (castleId > 0 && (!activeChar.isCastleLord(castleId) || activeChar.isInParty()))
		{
			if (activeChar.isInParty())
			{
				activeChar.sendMessage("You cannot pickup mercenaries while in a party.");
			}
			else
			{
				activeChar.sendMessage("Only the castle lord can pickup mercenaries.");
			}
			activeChar.setTarget(target);
			activeChar.getAI().setIntention(CtrlIntention.IDLE);
		}
		else if (!activeChar.isFlying())
		{
			activeChar.getAI().setIntention(CtrlIntention.PICK_UP, target);
		}

		return true;
	}

	@Override
	public InstanceType getInstanceType()
	{
		return InstanceType.ItemInstance;
	}
}