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
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.data.parser.WorldEventParser;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.EventChestInstance;
import l2e.gameserver.model.actor.instance.EventMonsterInstance;
import l2e.gameserver.model.actor.instance.MonsterInstance;
import l2e.gameserver.model.entity.events.AbstractWorldEvent;
import l2e.gameserver.model.entity.events.model.template.WorldEventDrop;
import l2e.gameserver.model.entity.events.model.template.WorldEventTemplate;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.spawn.SpawnTerritory;
import l2e.gameserver.model.strings.server.ServerMessage;
import l2e.gameserver.taskmanager.ItemsAutoDestroy;

/**
 * Updated by LordWinter 13.07.2020
 */
public class Rabbits extends AbstractWorldEvent
{
	private boolean _isActive = false;
	private WorldEventTemplate _template = null;
	private ScheduledFuture<?> _eventTask = null;
	
	private final List<Npc> _npcList = new ArrayList<>();
	
	private static int _chest_count = 0;
	private static int _option_howmuch;
	private static boolean _canUseMagic;

	private static final int _npc_snow = 900101;
	private static final int _npc_chest = 900102;
	private static final int _skill_tornado = 630;
	private static final int _skill_magic_eye = 629;

	public Rabbits(String name, String descr)
	{
		super(name, descr);

		addStartNpc(_npc_snow);
		addFirstTalkId(_npc_snow);
		addTalkId(_npc_snow);

		addFirstTalkId(_npc_chest);
		addSkillSeeId(_npc_chest);
		addSpawnId(_npc_chest);
		addAttackId(_npc_chest);
		
		_template = WorldEventParser.getInstance().getEvent(14);
		if (_template != null)
		{
			_option_howmuch = _template.getParams().getInteger("totalAmount", 100);
			_canUseMagic = _template.getParams().getBool("canUseMagic", false);
			if (!_isActive)
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
	public String onSpawn(Npc npc)
	{
		((EventMonsterInstance) npc).eventSetDropOnGround(true);
		if (!_canUseMagic)
		{
			((EventMonsterInstance) npc).eventSetBlockOffensiveSkills(true);
		}

		npc.setIsImmobilized(true);
		npc.disableCoreAI(true);

		return super.onSpawn(npc);
	}

	@Override
	public boolean eventStart(long totalTime, boolean force)
	{
		if (_isActive || totalTime == 0)
		{
			return false;
		}
		
		if (totalTime == -1)
		{
			final long startTime = calcEventStartTime(_template, force);
			final long expireTime = calcEventStopTime(_template, force);
			totalTime = expireTime - startTime;
			if (totalTime <= 0)
			{
				return false;
			}
		}
		
		if (_eventTask != null)
		{
			_eventTask.cancel(false);
			_eventTask = null;
		}
		
		if (_template.getTerritories() == null)
		{
			_log.info(_template.getName(null) + ": Event can't be started, because territoty cant selected!");
		}
		
		final int rndSelect = Rnd.get(_template.getTerritories().size());
		
		_npcList.clear();
		_isActive = true;

		final Location npcLoc = _template.getLocations().get(rndSelect).getLocation();
		final SpawnTerritory territory = _template.getTerritories().get(rndSelect).getTerritory();
		
		_npcList.add(addSpawn(_npc_snow, npcLoc.getX(), npcLoc.getY(), npcLoc.getZ(), npcLoc.getHeading(), false, 0));
		for (int i = 0; i < _option_howmuch; i++)
		{
			final Location loc = territory.getRandomLoc(false);
			_npcList.add(addSpawn(_npc_chest, loc.getX(), loc.getY(), loc.getZ(), 0, true, 0));
			_chest_count++;
		}

		final ServerMessage msg1 = new ServerMessage("EventRabbits.START_MSG_1", true);
		Announcements.getInstance().announceToAll(msg1);

		final ServerMessage msg2 = new ServerMessage("EventRabbits.START_MSG_2", true);
		Announcements.getInstance().announceToAll(msg2);

		final ServerMessage msg3 = new ServerMessage("EventRabbits.START_MSG_3", true);
		msg3.add((totalTime / 60000));
		Announcements.getInstance().announceToAll(msg3);
		
		if (totalTime > 0)
		{
			_eventTask = ThreadPoolManager.getInstance().schedule(new Runnable()
			{
				@Override
				public void run()
				{
					eventStop();
				}
			}, totalTime);
			_log.info("Event " + _template.getName(null) + " will end in: " + TimeUtils.toSimpleFormat(System.currentTimeMillis() + totalTime));
		}
		return true;
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
		_chest_count = 0;
		_npcList.clear();

		final ServerMessage msg = new ServerMessage("EventRabbits.STOP", true);
		Announcements.getInstance().announceToAll(msg);

		checkTimerTask(calcEventStartTime(_template, false));
		
		return true;
	}

	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		final String htmltext = event;

		if (event.equalsIgnoreCase("transform"))
		{
			if (player.isTransformed() || player.isInStance())
			{
				player.untransform();
			}

			SkillsParser.getInstance().getInfo(2428, 1).getEffects(npc, player, false);

			return null;
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
		return npc.getId() + ".htm";
	}

	@Override
	public String onSkillSee(Npc npc, Player caster, Skill skill, GameObject[] targets, boolean isSummon)
	{
		if (skill.getId() == _skill_tornado && npc.isVisible() && caster.getTarget() == npc)
		{
			if (_template.getDropList() != null && !_template.getDropList().isEmpty())
			{
				boolean isRecalc = false;
				for (final WorldEventDrop drop : _template.getDropList())
				{
					if (Rnd.chance(drop.getChance()))
					{
						final long amount = drop.getMinCount() != drop.getMaxCount() ? Rnd.get(drop.getMinCount(), drop.getMaxCount()) : drop.getMinCount();
						((MonsterInstance) npc).dropItem(caster, drop.getId(), (int) amount);
						isRecalc = true;
					}
				}
				
				if (isRecalc)
				{
					ItemsAutoDestroy.getInstance().tryRecalcTime();
				}
			}
			npc.deleteMe();
			_chest_count--;
			
			if (_chest_count <= 0)
			{
				final ServerMessage msg = new ServerMessage("EventRabbits.NO_MORE", true);
				Announcements.getInstance().announceToAll(msg);
				eventStop();
			}
		}
		else if (skill.getId() == _skill_magic_eye && npc.isInvisible() && npc.isInRange(caster, skill.getAffectRange()))
		{
			if (npc instanceof EventChestInstance)
			{
				((EventChestInstance) npc).trigger(caster);
			}
		}
		return super.onSkillSee(npc, caster, skill, targets, isSummon);
	}

	@Override
	public String onAttack(Npc npc, Player attacker, int damage, boolean isSummon, Skill skill)
	{
		if (_isActive && (npc.getId() == _npc_chest))
		{
			SkillsParser.getInstance().getInfo(4515, 1).getEffects(npc, attacker, false);
		}
		return super.onAttack(npc, attacker, damage, isSummon);
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
		
		_template = WorldEventParser.getInstance().getEvent(14);
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
		new Rabbits(Rabbits.class.getSimpleName(), "events");
	}
}