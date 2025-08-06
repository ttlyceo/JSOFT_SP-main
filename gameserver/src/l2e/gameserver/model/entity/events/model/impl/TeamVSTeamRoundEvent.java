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
package l2e.gameserver.model.entity.events.model.impl;

import l2e.commons.collections.MultiValueSet;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.entity.events.AbstractFightEvent;
import l2e.gameserver.model.entity.events.model.template.FightEventPlayer;
import l2e.gameserver.model.entity.events.model.template.FightEventTeam;
import l2e.gameserver.model.strings.server.ServerMessage;

/**
 * Created by LordWinter
 */
public class TeamVSTeamRoundEvent extends AbstractFightEvent
{
	private static final long MAX_FIGHT_TIME = 300000L;
	protected long _lastKill;

	public TeamVSTeamRoundEvent(MultiValueSet<String> set)
	{
		super(set);

		_lastKill = 0L;
	}
	
	@Override
	public void onKilled(Creature actor, Creature victim)
	{
		if (actor != null && actor.isPlayer())
		{
			final FightEventPlayer realActor = getFightEventPlayer(actor.getActingPlayer());
			if (victim.isPlayer() && realActor != null)
			{
				realActor.getTeam().incScore(1);
				increaseKills(realActor);
				updateScreenScores();
				final ServerMessage msg = new ServerMessage("FightEvents.YOU_HAVE_KILL", realActor.getPlayer().getLang());
				msg.add(victim.getName(null));
				sendMessageToPlayer(realActor.getPlayer(), MESSAGE_TYPES.GM, msg);
			}
		}
		
		if (victim.isPlayer())
		{
			final FightEventPlayer realVictim = getFightEventPlayer(victim);
			if (realVictim != null)
			{
				realVictim.increaseDeaths();
				if (actor != null)
				{
					final ServerMessage msg = new ServerMessage("FightEvents.YOU_KILLED", realVictim.getPlayer().getLang());
					msg.add(actor.getName(null));
					sendMessageToPlayer(realVictim.getPlayer(), MESSAGE_TYPES.GM, msg);
				}
				checkTeamPlayers(realVictim);
				victim.getActingPlayer().broadcastCharInfo();
			}
			_lastKill = System.currentTimeMillis();
		}
		super.onKilled(actor, victim);
	}

	@Override
	public void startRound()
	{
		super.startRound();
		_lastKill = System.currentTimeMillis();
		ThreadPoolManager.getInstance().schedule(new CheckFightersInactive(this), 5000L);
	}

	@Override
	public void endRound()
	{
		_state = EVENT_STATE.OVER;

		if (!isLastRound())
		{
			sendMessageToFighting(MESSAGE_TYPES.SCREEN_BIG, "FightEvents.ROUND_IS_OVER", false, String.valueOf(getCurrentRound()));
		}
		else
		{
			sendMessageToFighting(MESSAGE_TYPES.SCREEN_BIG, "FightEvents.EVENT_IS_OVER", false);
		}

		if (!isLastRound())
		{
			for (final FightEventTeam team : getTeams())
			{
				team.setSpawnLoc(null);
			}

			ThreadPoolManager.getInstance().schedule(() ->
			{
				for (final FightEventPlayer iFPlayer : getPlayers(FIGHTING_PLAYERS))
				{
					teleportSinglePlayer(iFPlayer, false, true, false);
				}
				startNewTimer(true, 0, "startRoundTimer", TIME_PREPARATION_BETWEEN_NEXT_ROUNDS);

			}, TIME_AFTER_ROUND_END_TO_RETURN_SPAWN * 1000);
		}
		else
		{
			ressAndHealPlayers();
			ThreadPoolManager.getInstance().schedule(() -> stopEvent(), 10 * 1000);

			if (isTeamed())
			{
				announceWinnerTeam(true, null);
			}
			else
			{
				announceWinnerPlayer(true, null);
			}
		}

		for (final FightEventPlayer iFPlayer : getPlayers(FIGHTING_PLAYERS))
		{
			iFPlayer.getPlayer().broadcastUserInfo(true);
		}
	}

	private void checkTeamPlayers(FightEventPlayer player)
	{
		boolean allDeath = true;
		for (final FightEventPlayer fPlayer : player.getTeam().getPlayers())
		{
			if (fPlayer != null && !fPlayer.getPlayer().isDead())
			{
				allDeath = false;
				break;
			}
		}

		if (allDeath)
		{
			endRound();
		}
	}

	protected static class CheckFightersInactive implements Runnable
	{
		private final TeamVSTeamRoundEvent _activeEvent;
		
		public CheckFightersInactive(TeamVSTeamRoundEvent event)
		{
			_activeEvent = event;
		}
		
		@Override
		public void run()
		{
			if (_activeEvent.getState() != EVENT_STATE.STARTED)
			{
				return;
			}
			
			final long currentTime = System.currentTimeMillis();
			
			if (_activeEvent._lastKill + MAX_FIGHT_TIME < currentTime)
			{
				for (final FightEventPlayer fPlayer : _activeEvent.getPlayers(FIGHTING_PLAYERS))
				{
					if (fPlayer != null && fPlayer.getPlayer() != null && !fPlayer.getPlayer().isDead())
					{
						fPlayer.getPlayer().doDie(null);
					}
				}
				_activeEvent.endRound();
			}
			ThreadPoolManager.getInstance().schedule(this, 5000L);
		}
	}
	
	@Override
	public String getVisibleTitle(Player player, Player viewer, String currentTitle, boolean toMe)
	{
		final FightEventPlayer fPlayer = getFightEventPlayer(player);
		if (fPlayer == null)
		{
			return currentTitle;
		}
		final ServerMessage msg = new ServerMessage("FightEvents.TITLE_INFO", viewer.getLang());
		msg.add(fPlayer.getKills());
		msg.add(fPlayer.getDeaths());
		return msg.toString();
	}
}
