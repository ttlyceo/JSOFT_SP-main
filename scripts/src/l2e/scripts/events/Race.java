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
package l2e.scripts.events;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

import l2e.commons.util.Rnd;
import l2e.commons.util.TimeUtils;
import l2e.gameserver.Announcements;
import l2e.gameserver.Config;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.data.parser.WorldEventParser;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.entity.events.AbstractWorldEvent;
import l2e.gameserver.model.entity.events.model.template.WorldEventLocation;
import l2e.gameserver.model.entity.events.model.template.WorldEventSpawn;
import l2e.gameserver.model.entity.events.model.template.WorldEventTemplate;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.skills.effects.Effect;
import l2e.gameserver.model.strings.server.ServerMessage;
import l2e.gameserver.network.serverpackets.CreatureSay;

/**
 * Updated by LordWinter 13.07.2020
 */
public class Race extends AbstractWorldEvent
{
	private boolean _isActive = false;
	private WorldEventTemplate _template = null;
	private ScheduledFuture<?> _eventTask = null;
	
	private final List<Npc> _npcList = new ArrayList<>();
	
	private Npc _npc;
	private final List<Player> _players = new ArrayList<>();
	

	private static boolean _isRaceStarted = false;
	private static int _timeRegister;
	private static int _skill;
	private static final int _start_npc = 900103;
	private static final int _stop_npc = 900104;
	private static int[] _randspawn = null;
	
	public Race(String name, String descr)
	{
		super(name, descr);
		
		addStartNpc(_start_npc);
		addFirstTalkId(_start_npc);
		addTalkId(_start_npc);
		
		addStartNpc(_stop_npc);
		addFirstTalkId(_stop_npc);
		addTalkId(_stop_npc);
		
		_template = WorldEventParser.getInstance().getEvent(15);
		if (_template != null && !_isActive)
		{
			_timeRegister = _template.getParams().getInteger("regTime", 5);
			_skill = _template.getParams().getInteger("transformSkill", 6201);
			if (_template.isNonStop())
			{
				eventStart(-1, false);
			}
			else
			{
				final long startTime = calcEventStartTime(_template, false);
				final long expireTime = calcEventStopTime(_template, false);
				if (startTime <= System.currentTimeMillis() && expireTime > System.currentTimeMillis() || (expireTime < startTime && expireTime > System.currentTimeMillis()))
				{
					eventStart(expireTime - System.currentTimeMillis(), false);
				}
				else
				{
					checkTimerTask(startTime);
				}
			}
		}
	}

	@Override
	public boolean isEventActive()
	{
		return _isActive;
	}
	
	@Override
	public WorldEventTemplate getEventTemplate()
	{
		return _template;
	}

	@Override
	public boolean eventStart(long totalTime, boolean force)
	{
		if (_isActive || totalTime == 0)
		{
			return false;
		}
		
		long caltime;
		if (totalTime == -1)
		{
			final long startTime = calcEventStartTime(_template, force);
			final long expireTime = calcEventStopTime(_template, force);
			caltime = expireTime - startTime;
			if (caltime <= 0)
			{
				return false;
			}
		}
		else
		{
			caltime = totalTime;
		}
		
		if (!Config.CUSTOM_NPC)
		{
			_log.info(_template.getName(null) + ": Event can't be started, because custom npc table is disabled!");
			return false;
		}
		
		if (_eventTask != null)
		{
			_eventTask.cancel(false);
			_eventTask = null;
		}
		
		_npcList.clear();
		_players.clear();
		_isActive = true;
		
		final List<WorldEventSpawn> spawnList = _template.getSpawnList();
		if (spawnList != null && !spawnList.isEmpty())
		{
			for (final WorldEventSpawn spawn : spawnList)
			{
				_npcList.add(_npc = addSpawn(spawn.getNpcId(), spawn.getLocation().getX(), spawn.getLocation().getY(), spawn.getLocation().getZ(), spawn.getLocation().getHeading(), false, 0));
			}
		}
		
		final ServerMessage msg1 = new ServerMessage("EventRace.START_MSG_1", true);
		Announcements.getInstance().announceToAll(msg1);
		
		final ServerMessage msg2 = new ServerMessage("EventRace.START_MSG_2", true);
		msg2.add(_timeRegister);
		Announcements.getInstance().announceToAll(msg2);
		
		_eventTask = ThreadPoolManager.getInstance().schedule(new Runnable()
		{
			@Override
			public void run()
			{
				startRace(caltime);
			}
		}, _timeRegister * 60 * 1000);
		return true;
		
	}
	
	protected void startRace(long totalTime)
	{
		if (_eventTask != null)
		{
			_eventTask.cancel(false);
			_eventTask = null;
		}
		
		if (_players.isEmpty())
		{
			final ServerMessage msg = new ServerMessage("EventRace.ABORTED", true);
			Announcements.getInstance().announceToAll(msg);
			eventStop();
			return;
		}
		_isRaceStarted = true;
		
		final ServerMessage msg = new ServerMessage("EventRace.RACE_START", true);
		Announcements.getInstance().announceToAll(msg);
		
		final WorldEventLocation loc = _template.getLocations().get(Rnd.get(_template.getLocations().size()));
		
		_npcList.add(addSpawn(_stop_npc, loc.getLocation().getX(), loc.getLocation().getY(), loc.getLocation().getZ(), loc.getLocation().getHeading(), false, 0));
		_randspawn = new int[]
		{
		        loc.getLocation().getX(), loc.getLocation().getY(), loc.getLocation().getZ()
		};
		for (final Player player : _players)
		{
			if ((player != null) && player.isOnline())
			{
				if (player.isInsideRadius(_npc, 500, false, false))
				{
					sendMessage(player, "Race started! Go find Finish NPC as fast as you can... He is located near " + loc.getName());
					transformPlayer(player);
					player.getRadar().addMarker(_randspawn[0], _randspawn[1], _randspawn[2]);
				}
				else
				{
					sendMessage(player, "I told you stay near me right? Distance was too high, you are excluded from race");
					_players.remove(player);
				}
			}
		}
		_eventTask = ThreadPoolManager.getInstance().schedule(new Runnable()
		{
			@Override
			public void run()
			{
				timeUp();
			}
		}, totalTime);
		_log.info("Event " + _template.getName(null) + " will end in: " + TimeUtils.toSimpleFormat(System.currentTimeMillis() + totalTime));
	}
	
	@Override
	public boolean eventStop()
	{
		if (!_isActive)
		{
			return false;
		}
		
		if (_eventTask != null)
		{
			_eventTask.cancel(false);
			_eventTask = null;
		}
		
		_isActive = false;
		_isRaceStarted = false;
		
		if (!_players.isEmpty())
		{
			for (final Player player : _players)
			{
				if ((player != null) && player.isOnline())
				{
					player.untransform();
					player.teleToLocation(_npc.getX(), _npc.getY(), _npc.getZ(), true, player.getReflection());
				}
			}
		}
		
		if (!_npcList.isEmpty())
		{
			for (final Npc _npc : _npcList)
			{
				if (_npc != null)
				{
					_npc.deleteMe();
				}
			}
		}
		_npcList.clear();
		_players.clear();
		_npc = null;
		
		final ServerMessage msg = new ServerMessage("EventRace.STOP", true);
		Announcements.getInstance().announceToAll(msg);
		
		return true;
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		final String htmltext = event;
		final QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			return null;
		}
		
		if (event.equalsIgnoreCase("transform"))
		{
			transformPlayer(player);
			return null;
		}
		else if (event.equalsIgnoreCase("untransform"))
		{
			player.untransform();
			return null;
		}
		else if (event.equalsIgnoreCase("showfinish"))
		{
			player.getRadar().addMarker(_randspawn[0], _randspawn[1], _randspawn[2]);
			return null;
		}
		else if (event.equalsIgnoreCase("signup"))
		{
			if (_players.contains(player))
			{
				return "900103-onlist.htm";
			}
			_players.add(player);
			return "900103-signup.htm";
		}
		else if (event.equalsIgnoreCase("quit"))
		{
			player.untransform();
			if (_players.contains(player))
			{
				_players.remove(player);
			}
			return "900103-quit.htm";
		}
		else if (event.equalsIgnoreCase("finish"))
		{
			if (player.getFirstEffect(_skill) != null)
			{
				winRace(player);
				return "900104-winner.htm";
			}
			return "900104-notrans.htm";
		}
		return htmltext;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			st = newQuestState(player);
		}
		if (npc.getId() == _start_npc)
		{
			if (_isRaceStarted)
			{
				return _start_npc + "-started-" + isRacing(player) + ".htm";
			}
			return _start_npc + "-" + isRacing(player) + ".htm";
		}
		else if ((npc.getId() == _stop_npc) && _isRaceStarted)
		{
			return _stop_npc + "-" + isRacing(player) + ".htm";
		}
		return npc.getId() + ".htm";
	}
	
	private int isRacing(Player player)
	{
		if (_players.isEmpty())
		{
			return 0;
		}
		if (_players.contains(player))
		{
			return 1;
		}
		return 0;
	}
	
	private void transformPlayer(Player player)
	{
		if (player.isTransformed() || player.isInStance())
		{
			player.untransform();
		}
		if (player.isSitting())
		{
			player.standUp();
		}
		
		for (final Effect e : player.getAllEffects())
		{
			if (e.getAbnormalType().equalsIgnoreCase("SPEED_UP"))
			{
				e.exit();
			}
			if ((e.getSkill() != null) && ((e.getSkill().getId() == 268) || (e.getSkill().getId() == 298)))
			{
				e.exit();
			}
		}
		SkillsParser.getInstance().getInfo(_skill, 1).getEffects(player, player, false);
	}
	
	private void sendMessage(Player player, String text)
	{
		player.sendPacket(new CreatureSay(_npc.getObjectId(), 20, _npc.getName(null), text));
	}
	
	protected void timeUp()
	{
		final ServerMessage msg = new ServerMessage("EventRace.TIME_UP", true);
		Announcements.getInstance().announceToAll(msg);
		eventStop();
	}
	
	private void winRace(Player player)
	{
		if (_isActive)
		{
			calcReward(player, _template, 1);
			final ServerMessage msg = new ServerMessage("EventRace.WINNER", true);
			msg.add(player.getName(null));
			Announcements.getInstance().announceToAll(msg);
			eventStop();
		}
	}
	
	@Override
	public void startTimerTask(long time)
	{
		if (_eventTask != null)
		{
			_eventTask.cancel(false);
			_eventTask = null;
		}
		
		_eventTask = ThreadPoolManager.getInstance().schedule(new Runnable()
		{
			@Override
			public void run()
			{
				final long expireTime = calcEventStopTime(_template, false);
				if (expireTime > System.currentTimeMillis())
				{
					eventStart(expireTime - System.currentTimeMillis(), false);
				}
			}
		}, (time - System.currentTimeMillis()));
		_log.info("Event " + _template.getName(null) + " will start in: " + TimeUtils.toSimpleFormat(time));
	}
	
	@Override
	public boolean isReloaded()
	{
		if (isEventActive())
		{
			return false;
		}
		
		_template = WorldEventParser.getInstance().getEvent(15);
		if (_template != null)
		{
			if (_template.isNonStop())
			{
				eventStart(-1, false);
			}
			else
			{
				final long startTime = calcEventStartTime(_template, false);
				final long expireTime = calcEventStopTime(_template, false);
				if (startTime <= System.currentTimeMillis() && expireTime > System.currentTimeMillis() || (expireTime < startTime && expireTime > System.currentTimeMillis()))
				{
					eventStart(expireTime - System.currentTimeMillis(), false);
				}
				else
				{
					checkTimerTask(startTime);
				}
			}
			return true;
		}
		return false;
	}
	
	public static void main(String[] args)
	{
		new Race(Race.class.getSimpleName(), "events");
	}
}
