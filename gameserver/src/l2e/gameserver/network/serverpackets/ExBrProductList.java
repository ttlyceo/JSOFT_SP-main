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


import l2e.gameserver.data.parser.ProductItemParser;
import l2e.gameserver.model.ProductItem;

/**
 * Created by LordWinter 06.10.2011 Fixed by L2J Eternity-World
 */
public class ExBrProductList extends GameServerPacket
{
	@Override
	protected void writeImpl()
	{
		final Collection<ProductItem> items = ProductItemParser.getInstance().getAllItems();
		writeD(items.size());

		for (final ProductItem template : items)
		{
			if (System.currentTimeMillis() < template.getStartTimeSale())
			{
				continue;
			}

			if (System.currentTimeMillis() > template.getEndTimeSale())
			{
				continue;
			}

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