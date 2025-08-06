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
import l2e.gameserver.listener.player.OnAnswerListener;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.Party;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.entity.events.tournaments.model.TournamentTeam;
import l2e.gameserver.model.entity.events.tournaments.util.TournamentUtil;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.ExShowScreenMessage;

public class AskToJoinTeamAnswerListner implements OnAnswerListener
{
	private final Player _player;

	public AskToJoinTeamAnswerListner(Player player)
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

		if (_player.isTournamentTeamBeingInvited())
		{
			Player leader = GameObjectsStorage.getPlayer(_player.getTournamentTeamRequesterId());
			if (leader != null)
			{
				if(JSOFT.checkCondition(_player) && JSOFT.checkCondition(leader))
				{
					TournamentTeam team = leader.getTournamentTeam();
					if (team == null)
					{
						team = new TournamentTeam(leader, _player);
						TournamentUtil.toPlayer(_player, new ExShowScreenMessage("Your Team have been created!", 3000));
					}
					else
					{
						team.addMember(_player);
						TournamentUtil.toPlayer(leader, new ExShowScreenMessage(_player.getName(null) + " entered your team.", 3000));
						TournamentUtil.toPlayer(_player, new ExShowScreenMessage("You entered " + team.getName() + ".", 3000));
					}
					Party party = leader.getParty();
					if (party != null)
					{
						party.addPartyMember(_player);
					}
					else
					{
						party = new Party(leader, Party.ITEM_LOOTER);
						team.setParty(party);
					}
				}
				else
				{
					TournamentUtil.toPlayer(_player, new ExShowScreenMessage("Request team denied.", 3000));
					TournamentUtil.toPlayer(_player, Say2.CLAN, "Request team denied.");
					_player.setTournamentTeamRequesterId(0);
					_player.setTournamentTeamBeingInvited(false);
				}
			}
			else
			{
				TournamentUtil.toPlayer(_player, new ExShowScreenMessage("Request team denied.", 3000));
				TournamentUtil.toPlayer(_player, Say2.CLAN, "Request team denied.");
				_player.setTournamentTeamRequesterId(0);
				_player.setTournamentTeamBeingInvited(false);
			}
		}
	}

	@Override
	public void sayNo()
	{
		if(_player == null)
			return;

		Player leader = GameObjectsStorage.getPlayer(_player.getTournamentTeamRequesterId());
		if (leader != null)
		{
			TournamentUtil.toPlayer(leader, Say2.CLAN, _player.getName(null) + " denied your Tournament Team request.");
		}

		_player.setTournamentTeamRequesterId(0);
		_player.setTournamentTeamBeingInvited(false);

	}
}
