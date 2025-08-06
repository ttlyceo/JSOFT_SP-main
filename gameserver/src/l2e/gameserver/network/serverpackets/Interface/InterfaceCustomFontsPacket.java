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
package l2e.gameserver.network.serverpackets.Interface;

import java.util.List;

import emudev.managers.CustomFontManager;
import emudev.model.fonts.CustomFont;
import l2e.gameserver.network.serverpackets.GameServerPacket;

public class InterfaceCustomFontsPacket extends GameServerPacket
{
	private List<CustomFont> fontInfos;
	
	public InterfaceCustomFontsPacket sendFontInfos()
	{
		fontInfos = CustomFontManager.getInstance().getInfos();
		return this;
	}
	
	@Override
	protected void writeImpl()
	{
		writeH(fontInfos.size());
		for(final CustomFont info : fontInfos)
		{
			writeS(info.font_name);
			writeS(info.font_file_name);
			writeC(info.loc.ordinal());
			writeC(info.size);
			writeC(info.index);
			writeC(info.index_on);
			writeC(info.use_shadow ? 1 : 0);
			writeC(info.shadow_x);
			writeC(info.shadow_y);
			writeC(info.stroke);
			writeC(info.stroke_large);
			writeH(info.line_gap);
			writeH(info.underline_offset);
		}
	}
}
