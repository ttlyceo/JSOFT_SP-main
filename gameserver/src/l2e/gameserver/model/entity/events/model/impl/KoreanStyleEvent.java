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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import l2e.commons.collections.MultiValueSet;
import l2e.commons.util.Rnd;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Playable;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.PetInstance;
import l2e.gameserver.model.entity.events.AbstractFightEvent;
import l2e.gameserver.model.entity.events.model.template.FightEventPlayer;
import l2e.gameserver.model.entity.events.model.template.FightEventTeam;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.skills.effects.AbnormalEffect;
import l2e.gameserver.model.skills.effects.Effect;
import l2e.gameserver.model.skills.effects.EffectType;
import l2e.gameserver.model.strings.server.ServerMessage;

/**
 * Created by LordWinter
 */
public class KoreanStyleEvent extends AbstractFightEvent
{
	private final long MAX_FIGHT_TIME = 90000L;
	private final FightEventPlayer[] _fightingPlayers;
	private final int[] _lastTeamChosenSpawn;
	private long _lastKill;
	private final boolean _winnerByDamage;
	private ScheduledFuture<?> _fightTask;

	public KoreanStyleEvent(MultiValueSet<String> set)
	{
		super(set);

		_winnerByDamage = set.getBool("winnerByDamage", true);
		_lastKill = 0L;
		_fightingPlayers = new FightEventPlayer[2];
		_lastTeamChosenSpawn = new int[]
		{
		        0, 0
		};
	}

	@Override
	public void onKilled(Creature actor, Creature victim)
	{
		if (actor != null && actor.isPlayer())
		{
			final FightEventPlayer realActor = getFightEventPlayer(actor.getActingPlayer());
			if (victim.isPlayer() && realActor != null)
			{
				increaseKills(realActor);
				updateScreenScores();
				final ServerMessage msg = new ServerMessage("FightEvents.YOU_HAVE_KILL", realActor.getPlayer().getLang());
				msg.add(victim.getName(null));
				sendMessageToPlayer(realActor.getPlayer(), MESSAGE_TYPES.GM, msg);
			}
		}

		if (victim.isPlayer())
		{
			if ((victim.getSummon() != null) && (!victim.getSummon().isDead()))
			{
				victim.getSummon().doDie(actor);
			}

			final FightEventPlayer realVictim = getFightEventPlayer(victim);
			realVictim.increaseDeaths();
			if (actor != null)
			{
				final ServerMessage msg = new ServerMessage("FightEvents.YOU_KILLED", realVictim.getPlayer().getLang());
				msg.add(actor.getName(null));
				sendMessageToPlayer(realVictim.getPlayer(), MESSAGE_TYPES.GM, msg);
			}
			victim.getActingPlayer().broadcastCharInfo();

			_lastKill = System.currentTimeMillis();
		}
		checkFightingPlayers();
		super.onKilled(actor, victim);
	}

	@Override
	public void onDamage(Creature actor, Creature victim, double damage)
	{
		if (actor != null && actor.isPlayable())
		{
			final FightEventPlayer realActor = getFightEventPlayer(actor.getActingPlayer());
			if (victim.isPlayer() && realActor != null)
			{
				realActor.increaseDamage(damage);
			}
		}
		super.onDamage(actor, victim, damage);
	}

	@Override
	public void loggedOut(Player player)
	{
		super.loggedOut(player);
		for (final FightEventPlayer fPlayer : _fightingPlayers)
		{
			if (fPlayer != null && fPlayer.getPlayer() != null && fPlayer.getPlayer().equals(player))
			{
				checkFightingPlayers();
			}
		}
	}

	@Override
	public boolean leaveEvent(Player player, boolean teleportTown, boolean isDestroy, boolean isLogOut)
	{
		super.leaveEvent(player, teleportTown, isDestroy, isLogOut);
		
		final Effect eInvis = player.getFirstEffect(EffectType.INVINCIBLE);
		if (eInvis != null)
		{
			eInvis.exit();
		}
		player.startHealBlocked(false);
		player.setIsInvul(false);
		if (player.isRooted())
		{
			player.startRooted(false);
		}
		
		if (player.getSummon() != null)
		{
			if (player.getSummon().isRooted())
			{
				player.getSummon().startRooted(false);
			}
		}
		
		if (getState() != EVENT_STATE.STARTED)
		{
			return true;
		}
		for (final FightEventPlayer fPlayer : _fightingPlayers)
		{
			if (fPlayer != null && fPlayer.getPlayer() != null && fPlayer.getPlayer().equals(player))
			{
				checkFightingPlayers();
			}
		}
		return true;
	}

	@Override
	public void startEvent(boolean checkReflection)
	{
		super.startEvent(checkReflection);
		for (final FightEventPlayer fPlayer : getPlayers(FIGHTING_PLAYERS, REGISTERED_PLAYERS))
		{
			final Player player = fPlayer.getPlayer();
			if (player.isDead())
			{
				player.doRevive();
			}
			if (player.isFakeDeathNow())
			{
				player.stopFakeDeath(true);
			}
			player.sitDownNow();
			player.resetReuse();
			player.sendSkillList(true);
			if (player.getSummon() != null)
			{
				player.getSummon().startAbnormalEffect(AbnormalEffect.ROOT);
			}
		}
	}

	@Override
	public void startRound()
	{
		super.startRound();
		checkFightingPlayers();
		_lastKill = System.currentTimeMillis();
		
		if (_fightTask != null)
		{
			_fightTask.cancel(false);
			_fightTask = null;
		}
		_fightTask = ThreadPoolManager.getInstance().scheduleAtFixedRate(new CheckFightersInactive(), 1000, 5000L);
	}

	@Override
	public void endRound()
	{
		super.endRound();
		super.unrootPlayers();
	}
	
	@Override
	public void stopEvent()
	{
		super.stopEvent();
		if (_fightTask != null)
		{
			_fightTask.cancel(false);
			_fightTask = null;
		}
	}

	private void checkFightingPlayers()
	{
		if (getState() == EVENT_STATE.OVER || getState() == EVENT_STATE.NOT_ACTIVE)
		{
			return;
		}
		boolean changed = false;
		for (int i = 0; i < _fightingPlayers.length; i++)
		{
			final FightEventPlayer oldPlayer = _fightingPlayers[i];
			if (oldPlayer == null || !isPlayerActive(oldPlayer.getPlayer()) || getFightEventPlayer(oldPlayer.getPlayer()) == null)
			{
				if (oldPlayer != null && !oldPlayer.getPlayer().isDead())
				{
					oldPlayer.getPlayer().doDie(null);
					oldPlayer.setDamage(0);
					return;
				}
				final FightEventPlayer newPlayer = chooseNewPlayer(i + 1);
				if (newPlayer == null)
				{
					for (final FightEventTeam team : getTeams())
					{
						if (team.getIndex() != (i + 1))
						{
							team.incScore(1);
						}
					}
					endRound();
					return;
				}
				newPlayer.getPlayer().isntAfk();
				_fightingPlayers[i] = newPlayer;
				changed = true;
			}
		}

		if (changed)
		{
			for (final FightEventPlayer iFPlayer : getPlayers(FIGHTING_PLAYERS))
			{
				if (iFPlayer != null)
				{
					final ServerMessage message = new ServerMessage("FightEvents.PL_VS_PL", iFPlayer.getPlayer().getLang());
					message.add(_fightingPlayers[0].getPlayer().getName(null));
					message.add(_fightingPlayers[1].getPlayer().getName(null));
					sendMessageToPlayer(iFPlayer.getPlayer(), MESSAGE_TYPES.SCREEN_BIG, message);
				}
			}
			preparePlayers();
		}
	}

	private FightEventPlayer chooseNewPlayer(int teamIndex)
	{
		final List<FightEventPlayer> alivePlayersFromTeam = new ArrayList<>();
		for (final FightEventPlayer fPlayer : getPlayers(FIGHTING_PLAYERS))
		{
			if (fPlayer.getPlayer().isSitting() && fPlayer.getTeam().getIndex() == teamIndex)
			{
				alivePlayersFromTeam.add(fPlayer);
			}
		}

		if (alivePlayersFromTeam.isEmpty())
		{
			return null;
		}
		if (alivePlayersFromTeam.size() == 1)
		{
			return alivePlayersFromTeam.get(0);
		}
		return Rnd.get(alivePlayersFromTeam);
	}

	private void preparePlayers()
	{
		for (int i = 0; i < _fightingPlayers.length; i++)
		{
			final FightEventPlayer fPlayer = _fightingPlayers[i];
			final Player player = fPlayer.getPlayer();
			
			if (player.isBlocked())
			{
				player.unblock();
			}
			player.standUp();
			player.isntAfk();
			player.resetDisabledSkills();
			player.resetReuse();
			player.sendSkillList(true);
			healFull(player);
			if (player.getSummon() instanceof PetInstance)
			{
				player.getSummon().unSummon(player);
			}
			if (player.getSummon() != null && !player.getSummon().isDead())
			{
				healFull(player.getSummon());
			}
			
			final Effect eInvis = player.getFirstEffect(EffectType.INVINCIBLE);
			if (eInvis != null)
			{
				eInvis.exit();
			}
			player.startHealBlocked(false);
			player.setIsInvul(false);

			fPlayer.setLastDamageTime();

			final Location loc = getMap().getKeyLocations()[i];
			player.teleToLocation(loc.getX(), loc.getY(), loc.getZ(), true, getReflection());
			rootPlayer(player);
			player.broadcastUserInfo(true);
			player.sendMessage(new ServerMessage("FightEvents.YOU_HAVE_10SEC", player.getLang()).toString());
		}

		ThreadPoolManager.getInstance().schedule(new Runnable()
		{
			@Override
			public void run()
			{
				for (int i = 0; i < _fightingPlayers.length; i++)
				{
					final FightEventPlayer fPlayer = _fightingPlayers[i];
					final Player player = fPlayer.getPlayer();

					if (player.isRooted())
					{
						player.startRooted(false);
					}

					healFull(player);
					if (player.getSummon() instanceof PetInstance)
					{
						player.getSummon().unSummon(player);
					}

					if (player.getSummon() != null && !player.getSummon().isDead())
					{
						healFull(player.getSummon());
						if (player.getSummon().isRooted())
						{
							player.getSummon().startRooted(false);
						}
					}
				}
			}
		}, 10000);
	}

	private static void healFull(Playable playable)
	{
		cleanse(playable);
		playable.setCurrentHp(playable.getMaxHp());
		playable.setCurrentMp(playable.getMaxMp());
		playable.setCurrentCp(playable.getMaxCp());
	}

	private static void cleanse(Playable playable)
	{
		try
		{
			for (final Effect e : playable.getAllEffects())
			{
				if (e.getSkill().isOffensive() && e.getSkill().canBeDispeled())
				{
					e.exit();
				}
			}
		}
		catch (final IllegalStateException e)
		{}
	}

	@Override
	public boolean canAttack(Creature target, Creature attacker)
	{
		if (getState() != EVENT_STATE.STARTED)
		{
			return false;
		}
		
		if (target == null || !target.isPlayable() || attacker == null || !attacker.isPlayable())
		{
			return false;
		}
		
		if (isFighting(target) && isFighting(attacker))
		{
			return true;
		}
		return false;
	}

	@Override
	public boolean canUseMagic(Creature target, Creature attacker, Skill skill)
	{
		if (getState() != EVENT_STATE.STARTED)
		{
			return false;
		}
		
		if (target == null || !target.isPlayable() || attacker == null || !attacker.isPlayable())
		{
			return false;
		}
		
		if (attacker != null && target != null)
		{
			if (!canUseSkill(attacker, target, skill))
			{
				return false;
			}
		}
		
		if (isFighting(target) && isFighting(attacker))
		{
			return true;
		}
		return false;
	}

	private boolean isFighting(Creature actor)
	{
		for (final FightEventPlayer fPlayer : _fightingPlayers)
		{
			if (fPlayer != null && fPlayer.getPlayer() != null && fPlayer.getPlayer().equals(actor.getActingPlayer()))
			{
				return true;
			}
		}
		return false;
	}

	private class CheckFightersInactive implements Runnable
	{
		@Override
		public void run()
		{
			if (getState() != EVENT_STATE.STARTED)
			{
				return;
			}

			final long currentTime = System.currentTimeMillis();

			for (final FightEventPlayer fPlayer : _fightingPlayers)
			{
				if (fPlayer != null && fPlayer.getPlayer() != null)
				{
					if (fPlayer.getLastDamageTime() < currentTime - 120000)
					{
						fPlayer.getPlayer().doDie(null);
					}
				}
			}

			if (_lastKill + MAX_FIGHT_TIME < currentTime)
			{
				Player playerToKill = null;
				if (_winnerByDamage)
				{
					playerToKill = calcWinnerByDamage(currentTime);
				}
				else
				{
					playerToKill = calcWinnerByKills(currentTime);
					if (playerToKill == null)
					{
						playerToKill = calcWinnerByDamage(currentTime);
					}
				}

				if (playerToKill != null)
				{
					playerToKill.doDie(null);
				}
			}
		}
	}
	
	private Player calcWinnerByKills(long currentTime)
	{
		Player playerToKill = null;
		for (final FightEventPlayer fPlayer : _fightingPlayers)
		{
			if (fPlayer != null && fPlayer.getPlayer() != null)
			{
				if (fPlayer.getPlayer().getClient().isDetached())
				{
					playerToKill = fPlayer.getPlayer();
					break;
				}
			}
		}
		
		if (playerToKill == null)
		{
			if (_fightingPlayers[0] != null && _fightingPlayers[1] != null)
			{
				if (_fightingPlayers[0].getKills() == 0 && _fightingPlayers[1].getKills() == 0)
				{
					return null;
				}
				
				if (_fightingPlayers[0].getKills() > _fightingPlayers[1].getKills())
				{
					playerToKill = _fightingPlayers[1].getPlayer();
				}
				else if (_fightingPlayers[0].getKills() < _fightingPlayers[1].getKills())
				{
					playerToKill = _fightingPlayers[0].getPlayer();
				}
			}
		}
		return playerToKill;
	}
	
	private Player calcWinnerByDamage(long currentTime)
	{
		double playerMinDamage = Double.MAX_VALUE;
		Player playerToKill = null;
		for (final FightEventPlayer fPlayer : _fightingPlayers)
		{
			if (fPlayer != null && fPlayer.getPlayer() != null)
			{
				if (fPlayer.getPlayer().getClient().isDetached())
				{
					playerToKill = fPlayer.getPlayer();
					playerMinDamage = -100.0;
				}
				else if (currentTime - fPlayer.getPlayer().getLastNotAfkTime() > 8000L)
				{
					playerToKill = fPlayer.getPlayer();
					playerMinDamage = -1.0;
				}
				else if (fPlayer.getDamage() < playerMinDamage)
				{
					playerToKill = fPlayer.getPlayer();
					playerMinDamage = fPlayer.getDamage();
				}
			}
		}
		return playerToKill;
	}

	@Override
	protected Location getSinglePlayerSpawnLocation(FightEventPlayer fPlayer)
	{
		final Location[] spawnLocations = getMap().getTeamSpawns().get(fPlayer.getTeam().getIndex());
		final int ordinalTeamIndex = fPlayer.getTeam().getIndex() - 1;
		int lastSpawnIndex = _lastTeamChosenSpawn[ordinalTeamIndex];
		lastSpawnIndex++;
		if (lastSpawnIndex >= spawnLocations.length)
		{
			lastSpawnIndex = 0;
		}
		_lastTeamChosenSpawn[ordinalTeamIndex] = lastSpawnIndex;
		return spawnLocations[lastSpawnIndex];
	}

	@Override
	protected Map<Integer, Long> giveRewardForWinningTeam(FightEventPlayer fPlayer, Map<Integer, Long> rewards, boolean atLeast1Kill, boolean isTopKiller)
	{
		return super.giveRewardForWinningTeam(fPlayer, rewards, false, isTopKiller);
	}

	@Override
	protected void handleAfk(FightEventPlayer fPlayer, boolean setAsAfk)
	{
	}

	@Override
	protected void unrootPlayers()
	{
	}

	@Override
	protected boolean inScreenShowBeScoreNotKills()
	{
		return false;
	}

	@Override
	protected boolean inScreenShowBeTeamNotInvidual()
	{
		return false;
	}

	@Override
	protected boolean isAfkTimerStopped(Player player)
	{
		return player.isSitting() || super.isAfkTimerStopped(player);
	}

	@Override
	public boolean canStandUp(Player player)
	{
		for (final FightEventPlayer fPlayer : _fightingPlayers)
		{
			if (fPlayer != null && fPlayer.getPlayer().equals(player))
			{
				return true;
			}
		}
		return false;
	}

	@Override
	protected List<List<Player>> spreadTeamInPartys(FightEventTeam team)
	{
		return Collections.emptyList();
	}

	@Override
	protected void createParty(List<Player> listOfPlayers)
	{
	}
}
