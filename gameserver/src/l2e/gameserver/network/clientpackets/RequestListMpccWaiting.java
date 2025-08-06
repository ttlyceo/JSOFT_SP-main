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
package l2e.gameserver.network.clientpackets;

import l2e.gameserver.model.actor.Player;
import l2e.gameserver.network.serverpackets.ExListMpccWaiting;

public class RequestListMpccWaiting extends GameClientPacket
{
	private int _listId;
	private int _locationId;
	private boolean _allLevels;
	
	@Override
	protected void readImpl()
	{
		_listId = readD();
		_locationId = readD();
		_allLevels = readD() == 1;
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getClient().getActiveChar();
		if (player == null)
		{
			return;
		}
		
		player.sendPacket(new ExListMpccWaiting(player, _listId, _locationId, _allLevels));
	}
}