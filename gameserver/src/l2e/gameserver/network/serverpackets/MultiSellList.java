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

import static l2e.gameserver.data.parser.MultiSellParser.PAGE_SIZE;

import l2e.gameserver.model.items.multisell.Entry;
import l2e.gameserver.model.items.multisell.Ingredient;
import l2e.gameserver.model.items.multisell.ListContainer;

public final class MultiSellList extends GameServerPacket
{
	private int _size, _index;
	private final ListContainer _list;
	private final boolean _finished;

	public MultiSellList(ListContainer list, int index)
	{
		_list = list;
		_index = index;
		_size = list.getEntries().size() - index;
		if (_size > PAGE_SIZE)
		{
			_finished = false;
			_size = PAGE_SIZE;
		}
		else
		{
			_finished = true;
		}
	}
	
	@Override
	protected void writeImpl()
	{
		writeD(_list.getListId());
		writeD(1 + (_index / PAGE_SIZE));
		writeD(_finished ? 1 : 0);
		writeD(PAGE_SIZE);
		writeD(_size);
		Entry ent;
		while (_size-- > 0)
		{
			ent = _list.getEntries().get(_index++);
			writeD(ent.getEntryId());
			writeC(ent.isStackable() ? 0x01 : 0x00);
			writeH(0x00);
			writeD(0x00);
			writeD(0x00);
			writeH(-1);
			writeH(0x00);
			writeH(0x00);
			writeH(0x00);
			writeH(0x00);
			writeH(0x00);
			writeH(0x00);
			writeH(0x00);
			writeH(ent.getProducts().size());
			writeH(ent.getIngredients().size());
			for (final Ingredient ing : ent.getProducts())
			{
				writeD(ing.getId());
				writeD(ing.getTemplate() != null ? ing.getTemplate().getBodyPart() : 0);
				writeH(ing.getTemplate() != null ? ing.getTemplate().getType2() : 65535);
				writeQ(ing.getCount());
				writeH(ing.getItemInfo() != null ? ing.getItemInfo().getEnchantLevel() : ing.getEnchantLevel());
				writeD(ing.getItemInfo() != null ? ing.getItemInfo().getAugmentation() != null ? ing.getItemInfo().getAugmentation().getAugmentationId() : 0x00 : ing.getAugmentation() != null ? ing.getAugmentation().getAugmentationId() : 0x00);
				writeD(ing.getItemInfo() != null ? (int) (ing.getItemInfo().getTime() / 1000) : (int) (ing.getTime() / 1000));
				writeH(ing.getItemInfo() != null ? ing.getItemInfo().getElementId() : ing.getAttackElementType());
				writeH(ing.getItemInfo() != null ? ing.getItemInfo().getElementPower() : ing.getAttackElementPower());
				for (byte i = 0; i < 6; i++)
				{
					writeH(ing.getItemInfo() != null ? ing.getItemInfo().getElementals()[i] : ing.getElementDefAttr(i));
				}
			}

			for (final Ingredient ing : ent.getIngredients())
			{
				writeD(ing.getId());
				writeH(ing.getTemplate() != null ? ing.getTemplate().getType2() : 65535);
				writeQ(ing.getCount());
				writeH(ing.getItemInfo() != null ? ing.getItemInfo().getEnchantLevel() : ing.getEnchantLevel());
				writeD(ing.getItemInfo() != null ? ing.getItemInfo().getAugmentation() != null ? ing.getItemInfo().getAugmentation().getAugmentationId() : 0x00 : ing.getAugmentation() != null ? ing.getAugmentation().getAugmentationId() : 0x00);
				writeD(ing.getItemInfo() != null ? (int) (ing.getItemInfo().getTime() / 1000) : (int) (ing.getTime() / 1000));
				writeH(ing.getItemInfo() != null ? ing.getItemInfo().getElementId() : ing.getAttackElementType());
				writeH(ing.getItemInfo() != null ? ing.getItemInfo().getElementPower() : ing.getAttackElementPower());
				for (byte i = 0; i < 6; i++)
				{
					writeH(ing.getItemInfo() != null ? ing.getItemInfo().getElementals()[i] : ing.getElementDefAttr(i));
				}
			}
		}
	}
}