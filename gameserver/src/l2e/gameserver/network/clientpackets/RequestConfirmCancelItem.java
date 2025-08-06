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

import l2e.commons.util.Util;
import l2e.gameserver.data.parser.AugmentationParser;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.items.Item;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.ExPutItemResultForVariationCancel;

public final class RequestConfirmCancelItem extends GameClientPacket
{
	private int _objectId;

	@Override
	protected void readImpl()
	{
		_objectId = readD();
	}
	
	@Override
	protected void runImpl()
	{
		final Player activeChar = getClient().getActiveChar();
		if (activeChar == null)
		{
			return;
		}
		final ItemInstance item = activeChar.getInventory().getItemByObjectId(_objectId);
		if (item == null)
		{
			return;
		}

		if (item.getOwnerId() != activeChar.getObjectId())
		{
			Util.handleIllegalPlayerAction(activeChar, "" + activeChar.getName(null) + " of account " + activeChar.getAccountName() + " tryied to destroy augment on item that doesn't own.");
			return;
		}

		if (!item.isAugmented())
		{
			activeChar.sendPacket(SystemMessageId.AUGMENTATION_REMOVAL_CAN_ONLY_BE_DONE_ON_AN_AUGMENTED_ITEM);
			return;
		}

		if (item.isPvp() && !AugmentationParser.getInstance().getParams().getBool("allowAugmentationPvpItems"))
		{
			activeChar.sendPacket(SystemMessageId.THIS_IS_NOT_A_SUITABLE_ITEM);
			return;
		}

		int price = 0;
		switch (item.getItem().getCrystalType())
		{
			case Item.CRYSTAL_C :
				if (item.getCrystalCount() < 1720)
				{
					price = 95000;
				}
				else if (item.getCrystalCount() < 2452)
				{
					price = 150000;
				}
				else
				{
					price = 210000;
				}
				break;
			case Item.CRYSTAL_B :
				if (item.getCrystalCount() < 1746)
				{
					price = 240000;
				}
				else
				{
					price = 270000;
				}
				break;
			case Item.CRYSTAL_A :
				if (item.getCrystalCount() < 2160)
				{
					price = 330000;
				}
				else if (item.getCrystalCount() < 2824)
				{
					price = 390000;
				}
				else
				{
					price = 420000;
				}
				break;
			case Item.CRYSTAL_S :
				price = 480000;
				break;
			case Item.CRYSTAL_S80 :
			case Item.CRYSTAL_S84 :
				price = 920000;
				break;
			default :
				if (!AugmentationParser.getInstance().getParams().getBool("allowAugmentationAllItemsGrade"))
				{
					return;
				}
				price = 95000;
				break;
		}
		activeChar.sendPacket(new ExPutItemResultForVariationCancel(item, price));
	}
}