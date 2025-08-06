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

import l2e.gameserver.data.parser.ProductItemParser;
import l2e.gameserver.model.ProductItem;
import l2e.gameserver.model.actor.templates.ProductItemTemplate;

/**
 * Created by LordWinter 06.10.2011 Fixed by L2J Eternity-World
 */
public class ExBrProductInfo extends GameServerPacket
{
	private final ProductItem _productId;

	public ExBrProductInfo(int id)
	{
		_productId = ProductItemParser.getInstance().getProduct(id);
	}
	
	@Override
	protected void writeImpl()
	{
		if (_productId == null)
		{
			return;
		}
		writeD(_productId.getProductId());
		writeD(_productId.getPoints());
		writeD(_productId.getComponents().size());
		for (final ProductItemTemplate com : _productId.getComponents())
		{
			writeD(com.getId());
			writeD(com.getCount());
			writeD(com.getWeight());
			writeD(com.isDropable() ? 0x01 : 0x00);
		}
	}
}