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
package l2e.gameserver.network.clientpackets;

import l2e.gameserver.Config;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.ExAutoSoulShot;
import l2e.gameserver.network.serverpackets.SystemMessage;

public final class RequestAutoSoulShot extends GameClientPacket
{
	private int _itemId;
	private int _type;
	
	@Override
	protected void readImpl()
	{
		_itemId = readD();
		_type = readD();
	}

	@Override
	protected void runImpl()
	{
		final Player activeChar = getClient().getActiveChar();
		if (activeChar == null)
		{
			return;
		}
		
		if ((activeChar.getPrivateStoreType() == Player.STORE_PRIVATE_NONE) && (activeChar.getActiveRequester() == null) && !activeChar.isDead())
		{
			final ItemInstance item = activeChar.getInventory().getItemByItemId(_itemId);
			if (item == null)
			{
				return;
			}
			
			if (_type == 1)
			{
				if (!activeChar.getInventory().canManipulateWithItemId(item.getId()))
				{
					activeChar.sendMessage("Cannot use this item.");
					return;
				}
				
				if (((_itemId >= 6535) && (_itemId <= 6540)) && !Config.ALLOW_AUTO_FISH_SHOTS)
				{
					return;
				}
					
				activeChar.addAutoSoulShot(_itemId);
				activeChar.sendPacket(new ExAutoSoulShot(_itemId, _type));
						
				final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.USE_OF_S1_WILL_BE_AUTO);
				sm.addItemName(item);
				activeChar.sendPacket(sm);
				activeChar.rechargeShots(true, true);
				if (((_itemId == 6645) || (_itemId == 6646) || (_itemId == 6647) || (_itemId == 20332) || (_itemId == 20333) || (_itemId == 20334)) && activeChar.hasSummon())
				{
					activeChar.getSummon().rechargeShots(true, true);
				}
			}
			else if (_type == 0)
			{
				activeChar.removeAutoSoulShot(_itemId);
				activeChar.sendPacket(new ExAutoSoulShot(_itemId, _type));
				
				final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.AUTO_USE_OF_S1_CANCELLED);
				sm.addItemName(item);
				activeChar.sendPacket(sm);
			}
		}
	}
	
	@Override
	protected boolean triggersOnActionRequest()
	{
		return false;
	}
}