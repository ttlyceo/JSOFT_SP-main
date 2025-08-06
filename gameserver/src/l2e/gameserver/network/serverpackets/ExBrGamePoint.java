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

import l2e.gameserver.Config;
import l2e.gameserver.model.actor.Player;

/**
 * Created by LordWinter 06.10.2011
 * Fixed by L2J Eternity-World
 */
public class ExBrGamePoint extends GameServerPacket
{
	private final int _objId;
	private long _points;

	public ExBrGamePoint(Player player)
	{
		_objId = player.getObjectId();
		
		if (Config.GAME_POINT_ITEM_ID == -1)
		{
			_points = player.getGamePoints();
		}
		else
		{
			_points = player.getInventory().getInventoryItemCount(Config.GAME_POINT_ITEM_ID, -100);
		}
	}
	
	@Override
	public void writeImpl()
	{
		writeD(_objId);
		writeQ(_points);
		writeD(0x00);
	}
}