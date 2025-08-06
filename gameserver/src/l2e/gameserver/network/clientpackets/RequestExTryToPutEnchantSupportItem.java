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
import l2e.gameserver.model.items.enchant.EnchantItem;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.ExPutEnchantSupportItemResult;

public class RequestExTryToPutEnchantSupportItem extends GameClientPacket
{
	private int _supportObjectId;
	private int _enchantObjectId;

	@Override
	protected void readImpl()
	{
		_supportObjectId = readD();
		_enchantObjectId = readD();
	}

	@Override
	protected void runImpl()
	{
		final Player activeChar = getClient().getActiveChar();
		if (activeChar != null)
		{
			if (activeChar.isEnchanting())
			{
				final ItemInstance item = activeChar.getInventory().getItemByObjectId(_enchantObjectId);
				final ItemInstance support = activeChar.getInventory().getItemByObjectId(_supportObjectId);
				if ((item == null) || (support == null))
				{
					return;
				}

				final EnchantItem supportTemplate = EnchantItemParser.getInstance().getSupportItem(support);
				if ((supportTemplate == null) || !supportTemplate.isValid(item))
				{
					activeChar.sendPacket(SystemMessageId.INAPPROPRIATE_ENCHANT_CONDITION);
					activeChar.setActiveEnchantSupportItemId(Player.ID_NONE);
					activeChar.sendPacket(new ExPutEnchantSupportItemResult(0));
					return;
				}
				activeChar.setActiveEnchantSupportItemId(support.getObjectId());
				activeChar.sendPacket(new ExPutEnchantSupportItemResult(_supportObjectId));
			}
		}
	}
}