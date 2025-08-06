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
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;

import l2e.commons.collections.MultiValueSet;
import l2e.commons.util.Rnd;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.data.parser.NpcsParser;
import l2e.gameserver.data.parser.SpawnParser;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.entity.events.AbstractFightEvent;
import l2e.gameserver.model.entity.events.model.template.FightEventPlayer;
import l2e.gameserver.model.spawn.Spawner;
import l2e.gameserver.model.strings.server.ServerMessage;
import l2e.gameserver.network.serverpackets.ExShowScreenMessage;

/**
 * Created by LordWinter
 */
public class MonsterAttackEvent extends AbstractFightEvent
{
	private final int[] _monstersCount;
	private final int[] _wavesInterval;
	private final int[] _mobsId;

	private final List<Npc> _monsters = new CopyOnWriteArrayList<>();

	private int _waveCount = 0;
	private Npc _defender = null;
	private ScheduledFuture<?> _deathTask;
	private ScheduledFuture<?> _mobsTask;
	private final List<ScheduledFuture<?>> _activeTask = new ArrayList<>();
	
	public MonsterAttackEvent(MultiValueSet<String> set)
	{
		super(set);

		_monstersCount = parseExcludedSkills(set.getString("mobsInWaveCount", ""));
		_wavesInterval = parseExcludedSkills(set.getString("wavesInterval", ""));
		_mobsId = parseExcludedSkills(set.getString("monstersId", ""));
	}

	@Override
	public void onKilled(Creature actor, Creature victim)
	{
		if (victim.isMonster() && actor != null && actor.isPlayer())
		{
			final FightEventPlayer fActor = getFightEventPlayer(actor.getActingPlayer());
			if (fActor != null)
			{
				increaseKills(fActor);
			}
			actor.getActingPlayer().broadcastCharInfo();
		}
		super.onKilled(actor, victim);
	}
	
	@Override
	public void startRound()
	{
		super.startRound();
		
		spawnCommander();

		for (int i = 0; i < _wavesInterval.length; i++)
		{
			final ScheduledFuture<?> task = ThreadPoolManager.getInstance().schedule(new WaveTask(_monstersCount[i]), _wavesInterval[i] * 1000);
			_activeTask.add(task);
		}
		info("Loaded " + _wavesInterval.length + " waves.");
	}
	
	@Override
	public void stopEvent()
	{
		for (ScheduledFuture<?> task : _activeTask)
		{
			if (task != null)
			{
				task.cancel(false);
				task = null;
			}
		}
		
		if (_defender != null)
		{
			super.stopEvent();
			sendMessageToFighting(MESSAGE_TYPES.SCREEN_BIG, "FightEvents.MATTACK_WIN", true);
		}
		else
		{
			ThreadPoolManager.getInstance().schedule(() ->
			{
				for (final Player player : getAllFightingPlayers())
				{
					leaveEvent(player, true, false, false);
					player.sendPacket(new ExShowScreenMessage("", 10, ExShowScreenMessage.TOP_LEFT, false));
				}
			}, 10 * 1000);
			
			ThreadPoolManager.getInstance().schedule(() ->
			{
				destroyMe();
			}, (15 + TIME_TELEPORT_BACK_TOWN) * 1000);
		}
		cleanUp();
	}
	
	private void cleanUp()
	{
		for (final Npc npc : _monsters)
		{
			if (npc != null)
			{
				npc.deleteMe();
			}
		}
		_monsters.clear();

		if (_deathTask != null)
		{
			_deathTask.cancel(false);
			_deathTask = null;
		}
		
		if (_mobsTask != null)
		{
			_mobsTask.cancel(false);
			_mobsTask = null;
		}
		
		if (_defender != null)
		{
			_defender.deleteMe();
			_defender = null;
		}
	}

	private void spawnCommander()
	{
		if (getState() == EVENT_STATE.OVER || getState() == EVENT_STATE.NOT_ACTIVE)
		{
			return;
		}

		final Location loc = Rnd.get(getMap().getDefLocations());
		try
		{
			final NpcTemplate template = NpcsParser.getInstance().getTemplate(53006);
			if (template != null)
			{
				final Spawner spawn = new Spawner(template);
				spawn.setX(loc.getX());
				spawn.setY(loc.getY());
				spawn.setZ(loc.getZ());
				spawn.setAmount(1);
				spawn.setRespawnDelay(0);
				spawn.setReflection(getReflection());
				spawn.stopRespawn();
				SpawnParser.getInstance().addNewSpawn(spawn);
				spawn.init();
				_defender = spawn.getLastSpawn();
				_defender.setIsImmobilized(true);
				if (_deathTask == null)
				{
					_deathTask = ThreadPoolManager.getInstance().scheduleAtFixedRate(new DeathTask(), 2000, 2000);
				}
			}
		}
		catch (final Exception e1)
		{}
	}
	
	@Override
	protected boolean inScreenShowBeScoreNotKills()
	{
		return false;
	}
	
	@Override
	public boolean isFriend(Creature c1, Creature c2)
	{
		return !(c1.isMonster() || c2.isMonster());
	}

	@Override
	public String getVisibleTitle(Player player, Player viewer, String currentTitle, boolean toMe)
	{
		final FightEventPlayer fPlayer = getFightEventPlayer(player);
		if (fPlayer == null)
		{
			return currentTitle;
		}
		final ServerMessage msg = new ServerMessage("FightEvents.TITLE_INFO2", viewer.getLang());
		msg.add(fPlayer.getKills());
		return msg.toString();
	}
	
	private class WaveTask implements Runnable
	{
		private final int _mobAmount;
		
		private WaveTask(int mobAmount)
		{
			_mobAmount = mobAmount;
		}
		
		@Override
		public void run()
		{
			if (getState() == EVENT_STATE.OVER || getState() == EVENT_STATE.NOT_ACTIVE)
			{
				return;
			}
			_waveCount++;

			sendMessageToFighting(MESSAGE_TYPES.SCREEN_BIG, "FightEvents.WAVE_COME", true, String.valueOf(_waveCount));
			try
			{
				Thread.sleep(5000L);
			}
			catch (final Exception e)
			{}
			
			for (int i = 0; i < _mobAmount; i++)
			{
				_monsters.add(chooseLocAndSpawnNpc(Rnd.get(_mobsId), getMap().getKeyLocations(), 0, true));
				try
				{
					Thread.sleep(500L);
				}
				catch (final Exception e)
				{}
			}
			
			if (_waveCount == _wavesInterval.length)
			{
				if (_mobsTask == null)
				{
					_mobsTask = ThreadPoolManager.getInstance().scheduleAtFixedRate(new MonsterTask(), 2000, 2000);
				}
			}
		}
	}

	private class DeathTask implements Runnable
	{
		@Override
		public void run()
		{
			if (getState() == EVENT_STATE.OVER || getState() == EVENT_STATE.NOT_ACTIVE)
			{
				return;
			}
			
			if (_defender == null || _defender.isDead())
			{
				sendMessageToFighting(MESSAGE_TYPES.SCREEN_BIG, "FightEvents.FAIL_BACK", true);
				_defender = null;
				if (_deathTask != null)
				{
					_deathTask.cancel(false);
					_deathTask = null;
				}
				
				if (_mobsTask != null)
				{
					_mobsTask.cancel(false);
					_mobsTask = null;
				}
				stopEvent();
			}
		}
	}
	
	private class MonsterTask implements Runnable
	{
		@Override
		public void run()
		{
			if (getState() == EVENT_STATE.OVER || getState() == EVENT_STATE.NOT_ACTIVE)
			{
				return;
			}
			
			boolean found = false;
			for (final Npc npc : _monsters)
			{
				if (npc != null && !npc.isDead())
				{
					found = true;
				}
			}
			
			if (!found)
			{
				stopEvent();
			}
		}
	}
	
	public void checkAlivePlayer()
	{
		boolean found = false;
		for (final Player player : getAllFightingPlayers())
		{
			if (player != null)
			{
				if (!player.isDead())
				{
					found = true;
					break;
				}
			}
		}
		
		if (!found)
		{
			if (_deathTask != null)
			{
				_deathTask.cancel(false);
				_deathTask = null;
			}
			
			if (_mobsTask != null)
			{
				_mobsTask.cancel(false);
				_mobsTask = null;
			}
			_defender.deleteMe();
			_defender = null;
			stopEvent();
		}
	}
}