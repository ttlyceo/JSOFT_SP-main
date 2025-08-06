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
import l2e.gameserver.model.actor.templates.ManufactureItemTemplate;

public class RecipeShopSellList extends GameServerPacket
{
	private final Player _buyer, _manufacturer;
	
	public RecipeShopSellList(Player buyer, Player manufacturer)
	{
		_buyer = buyer;
		_manufacturer = manufacturer;
	}

	@Override
	protected final void writeImpl()
	{
		writeD(_manufacturer.getObjectId());
		writeD((int) _manufacturer.getCurrentMp());
		writeD((int) _manufacturer.getMaxMp());
		writeQ(_buyer.getAdena());
		if (!_manufacturer.hasManufactureShop())
		{
			writeD(0x00);
		}
		else
		{
			writeD(_manufacturer.getManufactureItems().size());
			for (final ManufactureItemTemplate temp : _manufacturer.getManufactureItems().values())
			{
				writeD(temp.getRecipeId());
				writeD(0x00);
				writeQ(temp.getCost());
			}
		}
	}
}