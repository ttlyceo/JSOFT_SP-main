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
package l2e.gameserver.network.serverpackets.pledge;

import l2e.gameserver.model.actor.Player;
import l2e.gameserver.network.serverpackets.GameServerPacket;

public class ManagePledgePower extends GameServerPacket
{
	private final int _action, _clanId, _privs;
	
	public ManagePledgePower(Player player, int action, int rank)
	{
		_clanId = player.getClanId();
		_action = action;
		_privs = player.getClan().getRankPrivs(rank);
		player.sendPacket(new PledgeReceiveUpdatePower(_privs));
	}
	
	@Override
	protected final void writeImpl()
	{
		writeD(_clanId);
		writeD(_action);
		writeD(_privs);
	}
}