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
import l2e.gameserver.model.actor.templates.ShortCutTemplate;

public final class ShortCutInit extends GameServerPacket
{
	private ShortCutTemplate[] _shortCuts;

	public ShortCutInit(Player activeChar)
	{
		if (activeChar == null)
		{
			return;
		}
		_shortCuts = activeChar.getAllShortCuts();
	}
	
	@Override
	protected final void writeImpl()
	{
		writeD(_shortCuts.length);
		for (final ShortCutTemplate sc : _shortCuts)
		{
			writeD(sc.getType().ordinal());
			writeD(sc.getSlot() + sc.getPage() * 12);
			switch (sc.getType())
			{
				case ITEM :
					writeD(sc.getId());
					writeD(0x01);
					writeD(sc.getSharedReuseGroup());
					writeD(sc.getCurrenReuse());
					writeD(sc.getReuse());
					writeH(sc.getAugmentationId());
					writeH(0x00);
					break;
				case SKILL :
					writeD(sc.getId());
					writeD(sc.getLevel());
					writeC(0x00);
					writeD(0x01);
					break;
				case ACTION :
				case MACRO :
				case RECIPE :
				case BOOKMARK :
					writeD(sc.getId());
					writeD(0x01);
					break;
			}
		}
	}
}