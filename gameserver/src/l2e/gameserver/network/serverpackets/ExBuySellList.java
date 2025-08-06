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
package l2e.gameserver.network.serverpackets;

import l2e.gameserver.Config;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.items.instance.ItemInstance;

public class ExBuySellList extends GameServerPacket
{
	private ItemInstance[] _sellList = null;
	private ItemInstance[] _refundList = null;
	private final boolean _done;
	
	public ExBuySellList(Player player, boolean done)
	{
		_sellList = player.getInventory().getAvailableItems(false, false, false);
		if (player.hasRefund())
		{
			_refundList = player.getRefund().getItems();
		}
		_done = done;
	}

	@Override
	protected final void writeImpl()
	{
		writeD(0x01);
		if ((_sellList != null) && (_sellList.length > 0))
		{
			writeH(_sellList.length);
			for (final ItemInstance item : _sellList)
			{
				final long sellPrice = (long) ((item.getItem().getReferencePrice() / 2) * Config.SELL_PRICE_MODIFIER);
				writeD(item.getObjectId());
				writeD(item.getDisplayId());
				writeD(item.getLocationSlot());
				writeQ(item.getCount());
				writeH(item.getItem().getType2());
				writeH(item.getCustomType1());
				writeH(0x00);
				writeD(item.getItem().getBodyPart());
				writeH(item.getEnchantLevel());
				writeH(item.getCustomType2());
				if (item.isAugmented())
				{
					writeD(item.getAugmentation().getAugmentationId());
				}
				else
				{
					writeD(0x00);
				}
				writeD(item.getMana());
				writeD(item.isTimeLimitedItem() ? (int) (item.getRemainingTime() / 1000) : -9999);
				writeH(item.getAttackElementType());
				writeH(item.getAttackElementPower());
				for (byte i = 0; i < 6; i++)
				{
					writeH(item.getElementDefAttr(i));
				}
				for (final int op : item.getEnchantOptions())
				{
					writeH(op);
				}
				writeQ(sellPrice);
			}
		}
		else
		{
			writeH(0x00);
		}
		
		if ((_refundList != null) && (_refundList.length > 0))
		{
			writeH(_refundList.length);
			int idx = 0;
			for (final ItemInstance item : _refundList)
			{
				final long sellPrice = (long) (((item.getItem().getReferencePrice() / 2) * item.getCount()) * Config.SELL_PRICE_MODIFIER);
				
				writeD(item.getObjectId());
				writeD(item.getDisplayId());
				writeD(0x00);
				writeQ(item.getCount());
				writeH(item.getItem().getType2());
				writeH(item.getCustomType1());
				writeH(0x00);
				writeD(item.getItem().getBodyPart());
				writeH(item.getEnchantLevel());
				writeH(item.getCustomType2());
				if (item.isAugmented())
				{
					writeD(item.getAugmentation().getAugmentationId());
				}
				else
				{
					writeD(0x00);
				}
				writeD(item.getMana());
				writeD(item.isTimeLimitedItem() ? (int) (item.getRemainingTime() / 1000) : -9999);
				writeH(item.getAttackElementType());
				writeH(item.getAttackElementPower());
				for (byte i = 0; i < 6; i++)
				{
					writeH(item.getElementDefAttr(i));
				}
				for (final int op : item.getEnchantOptions())
				{
					writeH(op);
				}
				writeD(idx++);
				writeQ(sellPrice);
			}
		}
		else
		{
			writeH(0x00);
		}
		
		writeC(_done ? 0x01 : 0x00);
	}
}