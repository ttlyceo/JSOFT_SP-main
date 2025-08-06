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

import emudev.managers.ScreenTextInfoManager;
import emudev.model.ScreenTextInfo;
import l2e.gameserver.network.serverpackets.GameServerPacket;

public class InterfaceScreenTextInfoPacket extends GameServerPacket
{
	private List<ScreenTextInfo> textInfos;
	private ScreenTextInfo textInfo;
	private int type;
	private int _index;
	
	public InterfaceScreenTextInfoPacket sendTextInfos()
	{
		textInfos = ScreenTextInfoManager.getInstance().getInfos();
		type = textInfos.size();
		return this;
	}
	
	public InterfaceScreenTextInfoPacket updateTextInfo(int index, ScreenTextInfo info)
	{
		type = 9999;
		_index = index;
		textInfo = info;
		return this;
	}
	
	@Override
	protected void writeImpl()
	{
		writeH(type);
		if(type < 9999)
		{
			for(final ScreenTextInfo info : textInfos)
			{
				writeH(info.id);
				writeC(info.enabled == true ? 1 : 0);
				writeC(info.type.ordinal());
				writeS(info.text_en);
				writeS(info.text_ru);
				writeS(info.font_name);
				writeD(info.font_color);
				writeC(info.anchor_point.ordinal());
				writeC(info.relative_point.ordinal());
				writeH(info.offset_x);
				writeH(info.offset_y);
				writeC(info.alpha);
			}
		}
		else if(type == 9999)
		{
			writeH(_index);
			writeC(textInfo.enabled == true ? 1 : 0);
			writeC(textInfo.type.ordinal());
			writeS(textInfo.text_en);
			writeS(textInfo.text_ru);
			writeS(textInfo.font_name);
			writeD(textInfo.font_color);
			writeC(textInfo.anchor_point.ordinal());
			writeC(textInfo.relative_point.ordinal());
			writeH(textInfo.offset_x);
			writeH(textInfo.offset_y);
			writeC(textInfo.alpha);
		}
	}
}
