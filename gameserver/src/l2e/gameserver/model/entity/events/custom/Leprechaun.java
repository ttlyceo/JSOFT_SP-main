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
package l2e.gameserver.model.entity.events.custom;

import java.util.concurrent.ScheduledFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2e.commons.util.Rnd;
import l2e.gameserver.Announcements;
import l2e.gameserver.Config;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.data.parser.NpcsParser;
import l2e.gameserver.data.parser.SpawnParser;
import l2e.gameserver.instancemanager.MapRegionManager;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.spawn.Spawner;
import l2e.gameserver.model.strings.server.ServerMessage;
import l2e.gameserver.network.serverpackets.EarthQuake;
import l2e.gameserver.network.serverpackets.ExRedSky;
import l2e.gameserver.network.serverpackets.MagicSkillUse;

/**
 * Updated by LordWinter 27.11.2018
 */
public class Leprechaun
{
	protected static final Logger _log = LoggerFactory.getLogger(Leprechaun.class);

	private final Object _lock = new Object();
	private int _x, _y, _z, _timer = 0, _timerAnnounce = 0;
	private Npc _eventNpc = null;
	private String nearestTown = "", mobName = "";
	private ScheduledFuture<?> _eventTask;

	public Leprechaun()
	{
		new LeprechaunQuest(-1, "Leprechaun", "events");
		if (Config.ENABLED_LEPRECHAUN && _eventTask == null)
		{
			_eventTask = ThreadPoolManager.getInstance().scheduleAtFixedRate(new LeprechaunSpawn(), Config.LEPRECHAUN_FIRST_SPAWN_DELAY * 60000, 60000);
		}
	}

	public class LeprechaunQuest extends Quest
	{
		public LeprechaunQuest(int id, String name, String descr)
		{
			super(id, name, descr);

			addStartNpc(Config.LEPRECHAUN_ID);
			addFirstTalkId(Config.LEPRECHAUN_ID);
			addKillId(Config.LEPRECHAUN_ID);
		}

		@Override
		public String onFirstTalk(Npc npc, Player player)
		{
			QuestState qst = player.getQuestState(getName());
			if (qst == null)
			{
				qst = newQuestState(player);
			}
			
			if (npc.getId() == _eventNpc.getId())
			{
				player.broadcastPacket(new EarthQuake(player.getX(), player.getY(), player.getZ(), 30, 3));
				player.broadcastPacket(new ExRedSky(5));
				player.broadcastPacket(new MagicSkillUse(_eventNpc, _eventNpc, 1469, 1, 0x300, 0));
				endEvent();
				final int[] chance = Config.LEPRECHAUN_REWARD_CHANCE;
				for (int i = 0; i < chance.length; i++)
				{
					if (Rnd.chance(Config.LEPRECHAUN_REWARD_CHANCE[i]))
					{
						qst.giveItems(Config.LEPRECHAUN_REWARD_ID[i], Config.LEPRECHAUN_REWARD_COUNT[i]);
					}
				}

				if (Config.SHOW_NICK)
				{
					final ServerMessage msg = new ServerMessage("Leprechaun.WAS_FOUND", true);
					msg.add(player.getName(null));
					Announcements.getInstance().announceToAll(msg);
				}
				else
				{
					final ServerMessage msg = new ServerMessage("Leprechaun.NOT_FOUND", true);
					Announcements.getInstance().announceToAll(msg);
				}
			}
			return null;
		}
		
		@Override
		public String onKill(Npc npc, Player player, boolean isSummon)
		{
			QuestState qst = player.getQuestState(getName());
			if (qst == null)
			{
				qst = newQuestState(player);
			}
			
			if (_eventNpc != null && npc.getId() == _eventNpc.getId())
			{
				player.broadcastPacket(new EarthQuake(player.getX(), player.getY(), player.getZ(), 30, 3));
				player.broadcastPacket(new ExRedSky(5));
				endEvent();
				final int[] chance = Config.LEPRECHAUN_REWARD_CHANCE;
				for (int i = 0; i < chance.length; i++)
				{
					if (Rnd.chance(Config.LEPRECHAUN_REWARD_CHANCE[i]))
					{
						qst.giveItems(Config.LEPRECHAUN_REWARD_ID[i], Config.LEPRECHAUN_REWARD_COUNT[i]);
					}
				}
				
				if (Config.SHOW_NICK)
				{
					final ServerMessage msg = new ServerMessage("Leprechaun.WAS_FOUND", true);
					msg.add(player.getName(null));
					Announcements.getInstance().announceToAll(msg);
				}
				else
				{
					final ServerMessage msg = new ServerMessage("Leprechaun.NOT_FOUND", true);
					Announcements.getInstance().announceToAll(msg);
				}
			}
			return super.onKill(npc, player, isSummon);
		}
	}
	
	public void startEvent()
	{
		if (_eventTask != null)
		{
			_eventTask.cancel(false);
			_eventTask = null;
		}
		_eventTask = ThreadPoolManager.getInstance().scheduleAtFixedRate(new LeprechaunSpawn(), 100, 60000);
	}

	public void endEvent()
	{
		if (_eventTask != null)
		{
			_eventTask.cancel(false);
			_eventTask = null;
		}
		
		if (_eventNpc != null)
		{
			_eventNpc.deleteMe();
			_eventNpc = null;
		}
		_timer = 0;
		_timerAnnounce = 0;

		final ServerMessage msg = new ServerMessage("Leprechaun.DISAPPEARED", true);
		Announcements.getInstance().announceToAll(msg);

		_log.info("Leprechaun: Event Leprechaun Ended!");
		_eventTask = ThreadPoolManager.getInstance().scheduleAtFixedRate(new LeprechaunSpawn(), Config.LEPRECHAUN_RESPAWN_INTERVAL * 60000, 60000);
	}

	private class LeprechaunSpawn implements Runnable
	{
		@Override
		public void run()
		{
			if (_eventNpc != null && (_timer == Config.LEPRECHAUN_SPAWN_TIME))
			{
				endEvent();
				return;
			}

			if (_timerAnnounce == Config.LEPRECHAUN_ANNOUNCE_INTERVAL)
			{
				if (_eventNpc != null)
				{
					final ServerMessage msg = new ServerMessage("Leprechaun.NEAR", true);
					msg.add(mobName);
					msg.add(nearestTown);
					msg.add(Config.LEPRECHAUN_SPAWN_TIME - _timer);
					Announcements.getInstance().announceToAll(msg);
				}
				_timerAnnounce = 0;
			}

			if (_timer == 0)
			{
				boolean repeat = true;
				while (repeat)
				{
					selectRandomNpc();
					if (!mobName.isEmpty() && !mobName.equals("Treasure Chest"))
					{
						repeat = false;
					}
				}
				spawnLep();
			}
			_timer++;
			_timerAnnounce++;
		}

		private void selectRandomNpc()
		{
			synchronized (_lock)
			{
				final int number = Rnd.get(SpawnParser.getInstance().getSpawnData().size());
				int count = 0;
				for (final Spawner spawn : SpawnParser.getInstance().getSpawnData())
				{
					if (spawn != null)
					{
						count++;
						if (count == number)
						{
							_x = spawn.getX() + 80;
							_y = spawn.getY() + 10;
							_z = spawn.getZ();
							mobName = spawn.getTemplate().getName(null);
							break;
						}
					}
				}
			}
		}

		private void spawnLep()
		{
			if (_eventNpc != null)
			{
				_eventNpc.deleteMe();
				_eventNpc = null;
			}

			final NpcTemplate template = NpcsParser.getInstance().getTemplate(Config.LEPRECHAUN_ID);
			if (template != null)
			{
				Spawner spawnDat;
				try
				{
					spawnDat = new Spawner(template);
					spawnDat.setAmount(1);
					spawnDat.setX(_x);
					spawnDat.setY(_y);
					spawnDat.setZ(_z);
					spawnDat.setHeading(0);
					spawnDat.stopRespawn();
					_eventNpc = spawnDat.spawnOne(false);
					if (Config.SHOW_REGION)
					{
						nearestTown = " (" + MapRegionManager.getInstance().getClosestTownName(_eventNpc) + ")";
					}
					final ServerMessage msg = new ServerMessage("Leprechaun.NEAR", true);
					msg.add(mobName);
					msg.add(nearestTown);
					msg.add(Config.LEPRECHAUN_SPAWN_TIME - _timer);
					Announcements.getInstance().announceToAll(msg);
					System.out.println("Leprechaun spawned in " + mobName + ": " + _x + "," + _y + "," + _z);
					_log.info("Leprechaun: Event Leprechaun is Starting!");
				}
				catch (final Exception e)
				{
				}
			}
			else
			{
				_log.warn("Leprechaun: Data missing in NPC table for ID: " + Config.LEPRECHAUN_ID + ".");
			}
		}
	}
	
	public boolean isActive()
	{
		return _eventNpc != null;
	}
	
	public static Leprechaun getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final Leprechaun _instance = new Leprechaun();
	}
}