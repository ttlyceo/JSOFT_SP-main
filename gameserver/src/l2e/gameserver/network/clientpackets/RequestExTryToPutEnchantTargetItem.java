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

import l2e.gameserver.data.parser.EnchantItemParser;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.items.enchant.EnchantScroll;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.ExPutEnchantTargetItemResult;

public class RequestExTryToPutEnchantTargetItem extends GameClientPacket
{
	private int _objectId = 0;

	@Override
	protected void readImpl()
	{
		_objectId = readD();
	}
	
	@Override
	protected void runImpl()
	{
		final Player activeChar = getClient().getActiveChar();

		if ((_objectId == 0) || (activeChar == null))
		{
			return;
		}

		if (activeChar.isEnchanting())
		{
			return;
		}
		
		if (activeChar.isActionsDisabled() || activeChar.isInStoreMode() || activeChar.getActiveTradeList() != null)
		{
			activeChar.setActiveEnchantItemId(Player.ID_NONE);
			activeChar.sendPacket(new ExPutEnchantTargetItemResult(0));
			return;
		}

		final ItemInstance item = activeChar.getInventory().getItemByObjectId(_objectId);
		final ItemInstance scroll = activeChar.getInventory().getItemByObjectId(activeChar.getActiveEnchantItemId());

		if ((item == null) || (scroll == null))
		{
			return;
		}
		
		final EnchantScroll scrollTemplate = EnchantItemParser.getInstance().getEnchantScroll(scroll);
		if ((scrollTemplate == null) || !scrollTemplate.isValid(item) || scrollTemplate.getChance(activeChar, item) <= 0)
		{
			activeChar.sendPacket(SystemMessageId.DOES_NOT_FIT_SCROLL_CONDITIONS);
			activeChar.setActiveEnchantItemId(Player.ID_NONE);
			activeChar.sendPacket(new ExPutEnchantTargetItemResult(0));
			if (scrollTemplate == null)
			{
				_log.warn(getClass().getSimpleName() + ": Undefined scroll have been used id: " + scroll.getId());
			}
			return;
		}
		
		activeChar.setIsEnchanting(true);
		activeChar.sendPacket(new ExPutEnchantTargetItemResult(_objectId));
	}
}