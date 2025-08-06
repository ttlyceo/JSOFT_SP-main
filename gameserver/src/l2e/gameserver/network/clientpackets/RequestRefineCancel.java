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
import l2e.gameserver.model.base.ShortcutType;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.ExVariationCancelResult;
import l2e.gameserver.network.serverpackets.InventoryUpdate;

public final class RequestRefineCancel extends GameClientPacket
{
	private int _targetItemObjId;
	
	@Override
	protected void readImpl()
	{
		_targetItemObjId = readD();
	}

	@Override
	protected void runImpl()
	{
		final Player activeChar = getClient().getActiveChar();
		if (activeChar == null)
		{
			return;
		}

		final ItemInstance targetItem = activeChar.getInventory().getItemByObjectId(_targetItemObjId);

		if (targetItem == null)
		{
			activeChar.sendPacket(new ExVariationCancelResult(0));
			return;
		}
		
		if (activeChar.isActionsDisabled())
		{
			activeChar.sendPacket(new ExVariationCancelResult(0));
			return;
		}

		if (targetItem.getOwnerId() != activeChar.getObjectId())
		{
			Util.handleIllegalPlayerAction(getClient().getActiveChar(), "" + getClient().getActiveChar().getName(null) + " of account " + getClient().getActiveChar().getAccountName() + " tryied to augment item that doesn't own.");
			return;
		}

		if (!targetItem.isAugmented())
		{
			activeChar.sendPacket(SystemMessageId.AUGMENTATION_REMOVAL_CAN_ONLY_BE_DONE_ON_AN_AUGMENTED_ITEM);
			activeChar.sendPacket(new ExVariationCancelResult(0));
			return;
		}

		int price = 0;
		switch (targetItem.getItem().getCrystalType())
		{
			case Item.CRYSTAL_C :
				if (targetItem.getCrystalCount() < 1720)
				{
					price = 95000;
				}
				else if (targetItem.getCrystalCount() < 2452)
				{
					price = 150000;
				}
				else
				{
					price = 210000;
				}
				break;
			case Item.CRYSTAL_B :
				if (targetItem.getCrystalCount() < 1746)
				{
					price = 240000;
				}
				else
				{
					price = 270000;
				}
				break;
			case Item.CRYSTAL_A :
				if (targetItem.getCrystalCount() < 2160)
				{
					price = 330000;
				}
				else if (targetItem.getCrystalCount() < 2824)
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
					activeChar.sendPacket(new ExVariationCancelResult(0));
					return;
				}
				price = 95000;
				break;
		}

		if (!activeChar.reduceAdena("RequestRefineCancel", price, null, true))
		{
			activeChar.sendPacket(new ExVariationCancelResult(0));
			activeChar.sendPacket(SystemMessageId.YOU_NOT_ENOUGH_ADENA);
			return;
		}
		
		final boolean equipped = targetItem.isEquipped();
		if (equipped)
		{
			activeChar.getInventory().unEquipItemInSlotAndRecord(targetItem.getLocationSlot());
		}

		targetItem.removeAugmentation();

		activeChar.sendPacket(new ExVariationCancelResult(1));

		if (equipped)
		{
			activeChar.getInventory().equipItem(targetItem);
		}
		
		final InventoryUpdate iu = new InventoryUpdate();
		iu.addModifiedItem(targetItem);
		activeChar.sendPacket(iu);
		
		activeChar.broadcastUserInfo(true);
		activeChar.updateShortCuts(targetItem.getObjectId(), ShortcutType.ITEM);
	}
}