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

import java.util.Collection;

import l2e.gameserver.Config;
import l2e.gameserver.model.items.buylist.Product;
import l2e.gameserver.model.items.buylist.ProductList;
import l2e.gameserver.network.ServerPacketOpcodes;

public final class BuyList extends GameServerPacket
{
	@Override
	protected ServerPacketOpcodes getOpcodes()
	{
		return ServerPacketOpcodes.ExBuySellList;
	}
	
	private final int _listId;
	private final Collection<Product> _list;
	private final long _money;
	private double _taxRate = 0;

	public BuyList(ProductList list, long currentMoney, double taxRate)
	{
		_listId = list.getListId();
		_list = list.getProducts();
		_money = currentMoney;
		_taxRate = taxRate;
	}

	@Override
	protected final void writeImpl()
	{
		writeD(0x00);
		writeQ(_money);
		writeD(_listId);

		writeH(_list.size());

		for (final Product product : _list)
		{
			if ((product.getCount() > 0) || !product.hasLimitedStock())
			{
				writeD(product.getId());
				writeD(product.getId());
				writeD(0x00);
				writeQ(product.getCount() < 0 ? 0 : product.getCount());
				writeH(product.getItem().getType2());
				writeH(product.getItem().getType1());
				writeH(0x00);
				writeD(product.getItem().getBodyPart());
				writeH(0x00);
				writeH(0x00);
				writeD(0x00);
				writeD(-1);
				writeD(-9999);
				writeH(0x00);
				writeH(0x00);
				for (byte i = 0; i < 6; i++)
				{
					writeH(0x00);
				}
				writeH(0x00);
				writeH(0x00);
				writeH(0x00);

				if ((product.getId() >= 3960) && (product.getId() <= 4026))
				{
					writeQ((long) (product.getPrice() * Config.RATE_SIEGE_GUARDS_PRICE * (1 + _taxRate)));
				}
				else
				{
					writeQ((long) (product.getPrice() * (1 + _taxRate)));
				}
			}
		}
	}
}