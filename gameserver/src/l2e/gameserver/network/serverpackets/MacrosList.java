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

import l2e.gameserver.model.Macro;
import l2e.gameserver.model.actor.templates.MacroTemplate;

public class MacrosList extends GameServerPacket
{
	private final int _rev;
	private final int _count;
	private final Macro _macro;
	
	public MacrosList(int rev, int count, Macro macro)
	{
		_rev = rev;
		_count = count;
		_macro = macro;
	}
	
	@Override
	protected final void writeImpl()
	{
		writeD(_rev);
		writeC(0x00);
		writeC(_count);
		writeC(_macro != null ? 0x01 : 0x00);
		if (_macro != null)
		{
			writeD(_macro.getId());
			writeS(_macro.getName());
			writeS(_macro.getDescr());
			writeS(_macro.getAcronym());
			writeC(_macro.getIcon());
			writeC(_macro.getCommands().size());
			int i = 1;
			for (final MacroTemplate cmd : _macro.getCommands())
			{
				writeC(i++);
				writeC(cmd.getType().ordinal());
				writeD(cmd.getD1());
				writeC(cmd.getD2());
				writeS(cmd.getCmd());
			}
		}
	}
}