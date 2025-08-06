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

import java.util.List;

import l2e.gameserver.data.parser.ProductItemParser;
import l2e.gameserver.model.ProductItem;

/**
 * Created by LordWinter 08.15.2013 Fixed by L2J Eternity-World
 */
public class ExBrRecentProductList extends GameServerPacket
{
	List<ProductItem> list;

	public ExBrRecentProductList(int objId)
	{
		list = ProductItemParser.getInstance().getRecentListByOID(objId);
	}

	@Override
	protected void writeImpl()
	{
		writeD(list.size());
		for (final ProductItem template : list)
		{
			writeD(template.getProductId());
			writeH(template.getCategory());
			writeD(template.getPoints());
			writeD(template.getTabId());
			writeD((int) (template.getStartTimeSale() / 1000));
			writeD((int) (template.getEndTimeSale() / 1000));
			writeC(template.getDaysOfWeek());
			writeC(template.getStartHour());
			writeC(template.getStartMin());
			writeC(template.getEndHour());
			writeC(template.getEndMin());
			writeD(template.getStock());
			writeD(template.getTotal());
		}
	}
}