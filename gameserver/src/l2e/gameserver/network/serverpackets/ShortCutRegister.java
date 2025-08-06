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

import l2e.gameserver.model.actor.templates.ShortCutTemplate;

public final class ShortCutRegister extends GameServerPacket
{
	private final ShortCutTemplate _shortcut;
	
	public ShortCutRegister(ShortCutTemplate shortcut)
	{
		_shortcut = shortcut;
	}
	
	@Override
	protected final void writeImpl()
	{
		writeD(_shortcut.getType().ordinal());
		writeD(_shortcut.getSlot() + _shortcut.getPage() * 12);
		switch (_shortcut.getType())
		{
			case ITEM :
				writeD(_shortcut.getId());
				writeD(_shortcut.getCharacterType());
				writeD(_shortcut.getSharedReuseGroup());
				writeD(_shortcut.getCurrenReuse());
				writeD(_shortcut.getReuse());
				writeD(_shortcut.getAugmentationId());
				break;
			case SKILL :
				writeD(_shortcut.getId());
				writeD(_shortcut.getLevel());
				writeC(0x00);
				writeD(_shortcut.getCharacterType());
				break;
			case ACTION :
			case MACRO :
			case RECIPE :
			case BOOKMARK :
				writeD(_shortcut.getId());
				writeD(_shortcut.getCharacterType());
		}
	}
}