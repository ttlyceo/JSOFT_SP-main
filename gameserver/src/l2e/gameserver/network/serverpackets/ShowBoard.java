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

import java.util.List;

import l2e.commons.util.StringUtil;
import l2e.gameserver.model.actor.Player;

public class ShowBoard extends GameServerPacket
{
	public static final ShowBoard STATIC = new ShowBoard();
	private final StringBuilder _htmlCode;
	private final boolean _isHide;
	
	public ShowBoard()
	{
		_htmlCode = null;
		_isHide = true;
	}
	
	public ShowBoard(String htmlCode, String id, Player player)
	{
		_htmlCode = StringUtil.startAppend(500, id, "\u0008", htmlCode);
		_isHide = false;
	}

	public ShowBoard(List<String> arg)
	{
		_htmlCode = StringUtil.startAppend(500, "1002\u0008");
		for (final String str : arg)
		{
			StringUtil.append(_htmlCode, str, " \u0008");
		}
		_isHide = false;
	}

	@Override
	protected final void writeImpl()
	{
		if (_isHide)
		{
			writeC(0x00);
		}
		else
		{
			writeC(0x01);
			writeS("bypass _bbshome");
			writeS("bypass _bbsgetfav");
			writeS("bypass _bbsloc");
			writeS("bypass _bbsclan");
			writeS("bypass _bbsmemo");
			writeS("bypass _maillist_0_1_0_");
			writeS("bypass _bbsfriends");
			writeS("bypass _bbsaddfav");
			writeS(_htmlCode.toString());
		}
	}
}