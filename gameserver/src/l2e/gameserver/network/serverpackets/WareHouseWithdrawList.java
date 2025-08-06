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

import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.items.instance.ItemInstance;

public final class WareHouseWithdrawList extends GameServerPacket
{
	public static final int PRIVATE = 1;
	public static final int CLAN = 4;
	public static final int CASTLE = 3;
	public static final int FREIGHT = 1;
	public static final int PRIVATE_INVENTORY = 5;
	private Player _activeChar;
	private long _playerAdena;
	private ItemInstance[] _items;
	private int _whType;
	private int agathionItems = 0;

	public WareHouseWithdrawList(int type, Player player, int whType)
	{
		_activeChar = player;
		_whType = whType;
		_playerAdena = _activeChar.getAdena();
		if (_activeChar.getActiveWarehouse() == null)
		{
			_log.warn("error while sending withdraw request to: " + _activeChar.getName(null));
			return;
		}
		_items = _activeChar.getActiveWarehouse().getItems();
		for (final ItemInstance item : _items)
		{
			if (item.isEnergyItem())
			{
				agathionItems++;
			}
		}
	}
	
	@Override
	protected final void writeImpl()
	{
		writeH(_whType);
		writeQ(_playerAdena);
		writeH(_items.length);
		for (final ItemInstance item : _items)
		{
			writeD(item.getObjectId());
			writeD(item.getDisplayId());
			writeD(item.getLocationSlot());
			writeQ(item.getCount());
			writeH(item.getItem().getType2());
			writeH(item.getCustomType1());
			writeH(item.isEquipped() ? 0x01 : 0x00);
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
			writeD(item.getObjectId());
		}
		
		if (_activeChar != null && agathionItems > 0)
		{
			_activeChar.sendPacket(new ExBrAgathionEnergyInfo(agathionItems, _items));
		}
	}
}