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
package l2e.gameserver.handler.itemhandlers.impl;

import l2e.commons.util.Rnd;
import l2e.gameserver.handler.itemhandlers.IItemHandler;
import l2e.gameserver.model.actor.Playable;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.zone.ZoneId;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.Dice;
import l2e.gameserver.network.serverpackets.SystemMessage;

public class RollingDice implements IItemHandler
{
	@Override
	public boolean useItem(Playable playable, ItemInstance item, boolean forceUse)
	{
		if (!playable.isPlayer())
		{
			playable.sendPacket(SystemMessageId.ITEM_NOT_FOR_PETS);
			return false;
		}

		final Player activeChar = playable.getActingPlayer();
		final int itemId = item.getId();
		
		if (activeChar.isInOlympiadMode())
		{
			activeChar.sendPacket(SystemMessageId.THIS_ITEM_IS_NOT_AVAILABLE_FOR_THE_OLYMPIAD_EVENT);
			return false;
		}
		
		final int number = rollDice(activeChar);
		if (number == 0)
		{
			activeChar.sendPacket(SystemMessageId.YOU_MAY_NOT_THROW_THE_DICE_AT_THIS_TIME_TRY_AGAIN_LATER);
			return false;
		}
		
		activeChar.broadcastPacket(2000, new Dice(activeChar.getObjectId(), itemId, number, activeChar.getX() - 30, activeChar.getY() - 30, activeChar.getZ()));
		
		final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_ROLLED_S2);
		sm.addString(activeChar.getName(null));
		sm.addNumber(number);
		activeChar.sendPacket(sm);
		
		if (activeChar.isInsideZone(ZoneId.PEACE))
		{
			activeChar.broadcastPacketToOthers(2000, sm);
			
		}
		else if (activeChar.isInParty())
		{
			activeChar.getParty().broadcastToPartyMembers(activeChar, sm);
		}
		return true;
		
	}
	
	private int rollDice(Player player)
	{
		if (!player.checkFloodProtection("ROLLDICE", "use_rollDice"))
		{
			return 0;
		}
		return Rnd.get(1, 6);
	}
}