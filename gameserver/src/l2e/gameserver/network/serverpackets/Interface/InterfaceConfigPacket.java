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

import emudev.managers.InterfaceSettingManager;
import emudev.model.InterfaceSetting;
import l2e.gameserver.network.serverpackets.GameServerPacket;

public class InterfaceConfigPacket extends GameServerPacket
{
	private final List<InterfaceSetting> settings;
	
	public InterfaceConfigPacket()
	{
		settings = InterfaceSettingManager.getInstance().getSettings();
	}
	
	@Override
	protected final void writeImpl()
	{
		writeH(settings.size());
		for(final InterfaceSetting s : settings)
		{
			writeS(s.Name);
			writeC(s.Type.ordinal());
			switch(s.Type)
			{
				case TYPE_CHAR:
					writeC(s.CharValue);
					break;
				case TYPE_SHORT:
					writeH(s.ShortValue);
					break;
				case TYPE_INT:
					writeD(s.IntValue);
					break;
				case TYPE_LONG:
					writeQ(s.LongValue);
					break;
				case TYPE_DOUBLE:
					writeF(s.DoubleValue);
					break;
				case TYPE_TEXT:
					writeS(s.TextValue);
					break;
				case TYPE_ARR_CHAR:
					writeD(s.ArrSize);
					for(int i = 0; i < s.ArrSize; i++)
					{
						writeC(s.CharValueArr[i]);
					}
					break;
				case TYPE_ARR_SHORT:
					writeD(s.ArrSize);
					for(int i = 0; i < s.ArrSize; i++)
					{
						writeH(s.ShortValueArr[i]);
					}
					break;
				case TYPE_ARR_INT:
					writeD(s.ArrSize);
					for(int i = 0; i < s.ArrSize; i++)
					{
						writeD(s.IntValueArr[i]);
					}
					break;
				case TYPE_ARR_LONG:
					writeD(s.ArrSize);
					for(int i = 0; i < s.ArrSize; i++)
					{
						writeQ(s.LongValueArr[i]);
					}
					break;
				case TYPE_ARR_DOUBLE:
					writeD(s.ArrSize);
					for(int i = 0; i < s.ArrSize; i++)
					{
						writeF(s.DoubleValueArr[i]);
					}
					break;
				case TYPE_ARR_TEXT:
					writeD(s.ArrSize);
					for(int i = 0; i < s.ArrSize; i++)
					{
						writeS(s.TextValueArr[i]);
					}
					break;
			}
		}
    }
}
