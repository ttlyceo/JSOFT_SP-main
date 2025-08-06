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
package l2e.gameserver.network.clientpackets.pledge;

import l2e.gameserver.model.Clan;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.network.clientpackets.GameClientPacket;
import l2e.gameserver.network.serverpackets.pledge.ManagePledgePower;

public final class RequestPledgePower extends GameClientPacket
{
	private int _rank;
	private int _action;
	private int _privs;
	
	@Override
	protected void readImpl()
	{
		_rank = readD();
		_action = readD();

		if (_action == 2)
		{
			_privs = readD();
		}
		else
		{
			_privs = 0;
		}
	}

	@Override
	protected void runImpl()
	{
		final Player player = getClient().getActiveChar();
		if (player == null)
		{
			return;
		}
		
		if (_action == 2)
		{
			if (player.isClanLeader())
			{
				if (_rank == 9)
				{
					_privs = (_privs & Clan.CP_CL_VIEW_WAREHOUSE) + (_privs & Clan.CP_CH_OPEN_DOOR) + (_privs & Clan.CP_CS_OPEN_DOOR);
				}
				player.getClan().setRankPrivs(_rank, _privs);
			}
		}
		else if (player.getClan() != null)
		{
			player.sendPacket(new ManagePledgePower(player, _action, _rank));
		}
		else
		{
			player.sendActionFailed();
		}
	}
}