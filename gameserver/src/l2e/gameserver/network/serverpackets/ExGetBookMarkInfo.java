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
import l2e.gameserver.model.actor.templates.BookmarkTemplate;

public class ExGetBookMarkInfo extends GameServerPacket
{
	private final Player player;
	
	public ExGetBookMarkInfo(Player cha)
	{
		player = cha;
	}
	
	@Override
	protected final void writeImpl()
	{
		writeD(0x00);
		writeD(player.getBookmarkslot());
		writeD(player.getTeleportBookmarks().size());
		for (final BookmarkTemplate tpbm : player.getTeleportBookmarks())
		{
			writeD(tpbm.getId());
			writeD(tpbm.getX());
			writeD(tpbm.getY());
			writeD(tpbm.getZ());
			writeS(tpbm.getName());
			writeD(tpbm.getIcon());
			writeS(tpbm.getTag());
		}
	}
}