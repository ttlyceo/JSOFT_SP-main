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

import l2e.commons.collections.MultiValueSet;
import l2e.commons.util.Rnd;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.data.parser.NpcsParser;
import l2e.gameserver.data.parser.SpawnParser;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.entity.events.AbstractFightEvent;
import l2e.gameserver.model.entity.events.model.template.FightEventPlayer;
import l2e.gameserver.model.spawn.Spawner;
import l2e.gameserver.model.strings.server.ServerMessage;
import l2e.gameserver.model.zone.ZoneType;

/**
 * Created by LordWinter
 */
public class LuckyCreaturesEvent extends AbstractFightEvent
{
	private final int _monstersCount;
	private final int[] _monsterTemplates;
	private final int _respawnSeconds;

	private final List<Npc> _monsters = new CopyOnWriteArrayList<>();
	private final List<Long> _deathTimes = new CopyOnWriteArrayList<>();

	public LuckyCreaturesEvent(MultiValueSet<String> set)
	{
		super(set);

		_monstersCount = set.getInteger("monstersCount", 1);
		_respawnSeconds = set.getInteger("monstersRespawn", 60);
		_monsterTemplates = parseExcludedSkills(set.getString("monsterTemplates", ""));
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
			_deathTimes.add(System.currentTimeMillis() + _respawnSeconds * 1000);
			_monsters.remove(victim);
		}
		super.onKilled(actor, victim);
	}
	
	@Override
	public void startEvent(boolean checkReflection)
	{
		super.startEvent(checkReflection);

		ThreadPoolManager.getInstance().schedule(new RespawnThread(), 30000);
	}
	
	@Override
	public void startRound()
	{
		super.startRound();

		for (int i = 0; i < _monstersCount; i++)
		{
			spawnMonster();
		}
		info("Spawning " + _monstersCount + " monsters.");
	}
	
	@Override
	public void stopEvent()
	{
		super.stopEvent();

		for (final Npc npc : _monsters)
		{
			if (npc != null)
			{
				npc.onDecay();
			}
		}
		_monsters.clear();
	}
	
	private void spawnMonster()
	{
		if (getState() == EVENT_STATE.OVER || getState() == EVENT_STATE.NOT_ACTIVE)
		{
			return;
		}
		
		final ZoneType zone = getActiveZones().values().iterator().next();
		final int[] coords = zone.getZone().getRandomPoint();
		
		try
		{
			final NpcTemplate template = NpcsParser.getInstance().getTemplate(Rnd.get(_monsterTemplates));
			if (template != null)
			{
				final Spawner spawn = new Spawner(template);
				spawn.setX(coords[0]);
				spawn.setY(coords[1]);
				spawn.setZ(coords[2]);
				spawn.setAmount(1);
				spawn.setRespawnDelay(0);
				spawn.setReflection(getReflection());
				spawn.stopRespawn();
				SpawnParser.getInstance().addNewSpawn(spawn);
				spawn.init();
				final Npc monster = spawn.getLastSpawn();
				_monsters.add(monster);
			}
		}
		catch (final Exception e1)
		{}
	}

	private class RespawnThread implements Runnable
	{
		@Override
		public void run()
		{
			if (getState() == EVENT_STATE.OVER || getState() == EVENT_STATE.NOT_ACTIVE)
			{
				return;
			}

			final long current = System.currentTimeMillis();
			final List<Long> toRemove = new ArrayList<>();
			for (final Long deathTime : _deathTimes)
			{
				if (deathTime < current)
				{
					spawnMonster();
					toRemove.add(deathTime);
				}
			}

			for (final Long l : toRemove)
			{
				_deathTimes.remove(l);
			}
			ThreadPoolManager.getInstance().schedule(this, 10000L);
		}
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
}