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
package l2e.gameserver.listener.player.impl;

import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.listener.player.OnAnswerListener;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.base.ShortcutType;
import l2e.gameserver.model.strings.server.ServerMessage;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.InventoryUpdate;
import l2e.gameserver.network.serverpackets.SystemMessage;

public class ExchangerAnswerListener implements OnAnswerListener
{
	private final Player _player;
		
	public ExchangerAnswerListener(Player player)
	{
		_player = player;
	}
		
	@Override
	public void sayYes()
	{
		if (_player != null && _player.isOnline())
		{
			final int selectObject = _player.getQuickVarI("selectObject", 0);
			final int transferObject = _player.getQuickVarI("transferObject", 0);
			final int transferOption = _player.getQuickVarI("transferOption", 0);
			
			final var select = _player.getInventory().getItemByObjectId(selectObject);
			final var transfer = _player.getInventory().getItemByObjectId(transferObject);
			final var isAugmentExchange = transferOption == 0;
			final int[] price = isAugmentExchange ? Config.SERVICE_EXCHANGE_AUGMENT : Config.SERVICE_EXCHANGE_ELEMENTS;
			if (select != null && transfer != null)
			{
				if ((isAugmentExchange && !select.isAugmented()) || (!isAugmentExchange && (select.getElementals() == null || transfer.getElementals() != null)))
				{
					return;
				}
				
				if (_player.getInventory().getItemByItemId(price[0]) == null)
				{
					_player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
					return;
				}
				if (_player.getInventory().getItemByItemId(price[0]).getCount() < price[1])
				{
					_player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
					return;
				}
				_player.destroyItemByItemId("Exchanger", price[0], price[1], _player, true);
				
				if (isAugmentExchange)
				{
					transfer.setAugmentation(select.getAugmentation());
					select.removeAugmentation();
					_player.updateShortCuts(transfer.getObjectId(), ShortcutType.ITEM);
					_player.updateShortCuts(select.getObjectId(), ShortcutType.ITEM);
				}
				else
				{
					for (final var elm : select.getElementals())
					{
						transfer.setElementAttr(elm.getElement(), elm.getValue());
					}
					transfer.updateDatabase();
					
					select.clearElementAttr((byte) -1);
					select.updateDatabase();
				}
				final InventoryUpdate iu = new InventoryUpdate();
				iu.addModifiedItem(transfer);
				iu.addModifiedItem(select);
				_player.sendPacket(iu);
				_player.broadcastCharInfo();
				_player.addQuickVar("selectObject", 0);
				_player.addQuickVar("transferObject", 0);
				_player.sendMessage((new ServerMessage(isAugmentExchange ? "Exchanger.EXCHANGE_AUGMENT_OK" : "Exchanger.EXCHANGE_ELEMENTALS_OK", _player.getLang())).toString());
				if (isAugmentExchange)
				{
					Util.addServiceLog(_player.getName(null) + " buy exchange augmentation service!");
				}
				else
				{
					Util.addServiceLog(_player.getName(null) + " buy exchange elementas service!");
				}
			}
		}
	}
	
	@Override
	public void sayNo()
	{
	}
}