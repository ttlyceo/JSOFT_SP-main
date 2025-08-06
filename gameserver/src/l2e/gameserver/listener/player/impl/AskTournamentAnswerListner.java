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
package l2e.gameserver.listener.player.impl;

import l2e.gameserver.JSOFT;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.listener.player.OnAnswerListener;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.entity.events.tournaments.TournamentData;
import l2e.gameserver.model.entity.events.tournaments.util.TournamentUtil;
import top.jsoft.jguard.utils.Rnd;

public class AskTournamentAnswerListner implements OnAnswerListener
{
	private final Player _player;

	public AskTournamentAnswerListner(Player player)
	{
		_player = player;
	}

	@Override
	public void sayYes()
	{
		if (_player == null)
		{
			return;
		}

		if (TournamentData.getInstance().isTournamentTeleporting())
		{
			if (JSOFT.checkCondition(_player))
			{
				Location location = TournamentUtil.TOURNAMENT_MAIN.getNpcSpawnLocs().get(Rnd.get(TournamentUtil.TOURNAMENT_MAIN.getNpcSpawnLocs().size()));
				if(location != null)
				{
					_player.teleToLocation(location.getX() + Rnd.get(-50, 50), location.getY() + Rnd.get(-50, 50), location.getZ(), true, ReflectionManager.DEFAULT);
				}
			}
			else
				_player.sendMessage("You do not meet the conditions to be teleported..");
		}
	}

	@Override
	public void sayNo()
	{
	}
}
