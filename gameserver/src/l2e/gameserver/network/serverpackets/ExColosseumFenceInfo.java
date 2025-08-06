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

import l2e.gameserver.model.actor.ColosseumFence;

public class ExColosseumFenceInfo extends GameServerPacket
{
	private final ColosseumFence _fence;
	
	public ExColosseumFenceInfo(ColosseumFence fence)
	{
		_fence = fence;
	}

	@Override
	protected void writeImpl()
	{
		writeD(_fence.getObjectId());
		writeD(_fence.getFenceState().ordinal());
		writeD(_fence.getX());
		writeD(_fence.getY());
		writeD(_fence.getZ());
		writeD(_fence.getFenceWidth());
		writeD(_fence.getFenceHeight());
	}
}