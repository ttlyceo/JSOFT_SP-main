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

import java.util.Iterator;

import l2e.gameserver.model.RecipeList;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.ManufactureItemTemplate;

public class RecipeShopManageList extends GameServerPacket
{
	private final Player _seller;
	private final boolean _isDwarven;
	private RecipeList[] _recipes;

	public RecipeShopManageList(Player seller, boolean isDwarven)
	{
		_seller = seller;
		_isDwarven = isDwarven;
		
		if (_isDwarven && _seller.hasDwarvenCraft())
		{
			_recipes = _seller.getDwarvenRecipeBook();
		}
		else
		{
			_recipes = _seller.getCommonRecipeBook();
		}
		
		if (_seller.hasManufactureShop())
		{
			final Iterator<ManufactureItemTemplate> it = _seller.getManufactureItems().values().iterator();
			ManufactureItemTemplate item;
			while (it.hasNext())
			{
				item = it.next();
				if ((item.isDwarven() != _isDwarven) || !seller.hasRecipeList(item.getRecipeId()))
				{
					it.remove();
				}
			}
		}
	}
	
	@Override
	protected final void writeImpl()
	{
		writeD(_isDwarven ? 0x00 : 0x01);
		writeD((int) _seller.getAdena());
		writeD(_seller.getObjectId());
		if (_recipes == null)
		{
			writeD(0x00);
		}
		else
		{
			writeD(_recipes.length);
			
			for (int i = 0; i < _recipes.length; i++)
			{
				final RecipeList temp = _recipes[i];
				writeD(temp.getId());
				writeD(i + 1);
			}
		}
		
		if (!_seller.hasManufactureShop())
		{
			writeD(0x00);
		}
		else
		{
			writeD(_seller.getManufactureItems().size());
			for (final ManufactureItemTemplate item : _seller.getManufactureItems().values())
			{
				writeD(item.getRecipeId());
				writeD(0x00);
				writeQ(item.getCost());
			}
		}
	}
}