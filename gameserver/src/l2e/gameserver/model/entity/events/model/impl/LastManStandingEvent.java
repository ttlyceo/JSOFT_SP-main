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

import java.util.HashMap;
import java.util.Map;

import l2e.commons.collections.MultiValueSet;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.entity.events.AbstractFightEvent;
import l2e.gameserver.model.entity.events.model.template.FightEventPlayer;
import l2e.gameserver.model.strings.server.ServerMessage;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.CreatureSay;

/**
 * Created by LordWinter
 */
public class LastManStandingEvent extends AbstractFightEvent
{
	private FightEventPlayer _winner;

	public LastManStandingEvent(MultiValueSet<String> set)
	{
		super(set);
	}

	@Override
	public void onKilled(Creature actor, Creature victim)
	{
		if (actor != null && actor.isPlayer())
		{
			final FightEventPlayer fActor = getFightEventPlayer(actor.getActingPlayer());
			if (fActor != null && victim.isPlayer())
			{
				increaseKills(fActor);
				final ServerMessage msg = new ServerMessage("FightEvents.YOU_HAVE_KILL", fActor.getPlayer().getLang());
				msg.add(victim.getName(null));
				sendMessageToPlayer(fActor.getPlayer(), MESSAGE_TYPES.GM, msg);
			}
		}

		if (victim.isPlayer())
		{
			final FightEventPlayer fVictim = getFightEventPlayer(victim);
			fVictim.increaseDeaths();
			if (actor != null)
			{
				final ServerMessage msg = new ServerMessage("FightEvents.YOU_KILLED", fVictim.getPlayer().getLang());
				msg.add(actor.getName(null));
				sendMessageToPlayer(fVictim.getPlayer(), MESSAGE_TYPES.GM, msg);
			}
			victim.getActingPlayer().broadcastCharInfo();
			leaveEvent(fVictim.getPlayer(), true, false, false);
			checkRoundOver();
		}

		super.onKilled(actor, victim);
	}
	
	@Override
	public void startEvent(boolean checkReflection)
	{
		super.startEvent(checkReflection);
		ThreadPoolManager.getInstance().schedule(new InactivityCheck(), 60000);
	}
	
	@Override
	public void startRound()
	{
		super.startRound();
		checkRoundOver();
	}
	
	@Override
	public boolean leaveEvent(Player player, boolean teleportTown, boolean isDestroy, boolean isLogOut)
	{
		final boolean result = super.leaveEvent(player, teleportTown, isDestroy, isLogOut);
		if (result)
		{
			checkRoundOver();
		}
		return result;
	}
	
	private boolean checkRoundOver()
	{
		if (getState() != EVENT_STATE.STARTED)
		{
			return true;
		}

		int alivePlayers = 0;
		FightEventPlayer aliveFPlayer = null;

		for (final FightEventPlayer iFPlayer : getPlayers(FIGHTING_PLAYERS))
		{
			if (isPlayerActive(iFPlayer.getPlayer()))
			{
				alivePlayers++;
				aliveFPlayer = iFPlayer;
			}
			if (aliveFPlayer == null)
			{
				if (!iFPlayer.getPlayer().isDead())
				{
					aliveFPlayer = iFPlayer;
				}
			}
		}

		if (alivePlayers <= 1)
		{
			_winner = aliveFPlayer;
			if (_winner != null)
			{
				_winner.increaseScore(1);
				announceWinnerPlayer(false, _winner);
			}
			updateScreenScores();
			setState(EVENT_STATE.OVER);
			
			ThreadPoolManager.getInstance().schedule(new Runnable()
			{
				@Override
				public void run()
				{
					endRound();
				}
			}, 5000L);
			if (_winner != null)
			{
				for (final Player player : GameObjectsStorage.getPlayers())
				{
					final ServerMessage msg = new ServerMessage("FightEvents.WON_LAST_HERO", player.getLang());
					msg.add(_winner.getPlayer().getName(null));
					msg.add(player.getEventName(getId()));
					player.sendPacket(new CreatureSay(0, Say2.CRITICAL_ANNOUNCE, player.getEventName(getId()), msg.toString()));
				}
			}
			return true;
		}
		return false;
	}

	@Override
	protected boolean inScreenShowBeScoreNotKills()
	{
		return false;
	}

	private class InactivityCheck implements Runnable
	{
		@Override
		public void run()
		{
			if (getState() == EVENT_STATE.NOT_ACTIVE)
			{
				return;
			}
			checkRoundOver();
			ThreadPoolManager.getInstance().schedule(this, 60000);
		}
	}
	
	@Override
	protected Map<Integer, Long> giveRewardForWinningTeam(FightEventPlayer fPlayer, Map<Integer, Long> rewards, boolean atLeast1Kill, boolean isTopKiller)
	{
		if (fPlayer == null)
		{
			return null;
		}
		
		if (rewards == null)
		{
			rewards = new HashMap<>();
		}
		
		if (fPlayer.equals(_winner))
		{
			for (final int[] item : _rewardByWinner)
			{
				if ((item == null) || (item.length != 2))
				{
					continue;
				}
				
				if (rewards.get(item[0]) != null)
				{
					final long amount = rewards.get(item[0]) + item[1];
					rewards.put(item[0], amount);
				}
				else
				{
					rewards.put(item[0], (long) item[1]);
				}
			}
			
			if (isGetHeroStatus())
			{
				fPlayer.getPlayer().setHero(true, isGetHeroSkills());
				if (getHeroTime() > 0)
				{
					final var endTime = System.currentTimeMillis() + (getHeroTime() * 60000L);
					fPlayer.getPlayer().setVar("tempHero", String.valueOf(endTime));
					if (isGetHeroSkills())
					{
						fPlayer.getPlayer().setVar("tempHeroSkills", "1");
					}
					fPlayer.getPlayer().startTempHeroTask(endTime, isGetHeroSkills() ? 1 : 0);
				}
			}
		}
		return rewards;
	}
	
	@Override
	public String getVisibleTitle(Player player, Player viewer, String currentTitle, boolean toMe)
	{
		final FightEventPlayer realPlayer = getFightEventPlayer(player);
		if (realPlayer == null)
		{
			return currentTitle;
		}
		final ServerMessage msg = new ServerMessage("FightEvents.TITLE_INFO2", viewer.getLang());
		msg.add(realPlayer.getKills());
		return msg.toString();
	}
}
