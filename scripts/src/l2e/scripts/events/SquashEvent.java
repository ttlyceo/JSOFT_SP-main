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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import l2e.commons.util.TimeUtils;
import l2e.gameserver.Announcements;
import l2e.gameserver.Config;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.data.parser.WorldEventParser;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.ChronoMonsterInstance;
import l2e.gameserver.model.actor.templates.items.Weapon;
import l2e.gameserver.model.entity.events.AbstractWorldEvent;
import l2e.gameserver.model.entity.events.EventsDropManager;
import l2e.gameserver.model.entity.events.model.template.WorldEventSpawn;
import l2e.gameserver.model.entity.events.model.template.WorldEventTemplate;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.strings.server.ServerMessage;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.CreatureSay;
import l2e.gameserver.network.serverpackets.PlaySound;

/**
 * Updated by LordWinter 13.07.2020
 */
public class SquashEvent extends AbstractWorldEvent
{
	private boolean _isActive = false;
	private static WorldEventTemplate _template = null;
	private ScheduledFuture<?> _eventTask = null;
	
	private final List<Npc> _npcList = new ArrayList<>();

	private static final int[] CHRONO_LIST =
	{
	        4202, 5133, 5817, 7058, 8350
	};

	private static final String[] SPAWN_TEXT =
	{
	        "SquashEvent.SPAWN_TEXT1", "SquashEvent.SPAWN_TEXT2", "SquashEvent.SPAWN_TEXT3", "SquashEvent.SPAWN_TEXT4", "SquashEvent.SPAWN_TEXT5"
	};

	private static final String[] GROWUP_TEXT =
	{
	        "SquashEvent.GROWUP_TEXT1", "SquashEvent.GROWUP_TEXT2", "SquashEvent.GROWUP_TEXT3", "SquashEvent.GROWUP_TEXT4", "SquashEvent.GROWUP_TEXT5"
	};

	private static final String[] KILL_TEXT =
	{
	        "SquashEvent.KILL_TEXT1", "SquashEvent.KILL_TEXT2", "SquashEvent.KILL_TEXT3", "SquashEvent.KILL_TEXT4", "SquashEvent.KILL_TEXT5"
	};

	private static final String[] NOCHRONO_TEXT =
	{
	        "SquashEvent.NOCHRONO_TEXT1", "SquashEvent.NOCHRONO_TEXT2", "SquashEvent.NOCHRONO_TEXT3", "SquashEvent.NOCHRONO_TEXT4", "SquashEvent.NOCHRONO_TEXT5"
	};

	private static final String[] CHRONO_TEXT =
	{
	        "SquashEvent.CHRONO_TEXT1", "SquashEvent.CHRONO_TEXT2", "SquashEvent.CHRONO_TEXT3", "SquashEvent.CHRONO_TEXT4", "SquashEvent.CHRONO_TEXT5"
	};

	private static final String[] NECTAR_TEXT =
	{
	        "SquashEvent.NECTAR_TEXT1", "SquashEvent.NECTAR_TEXT2", "SquashEvent.NECTAR_TEXT3", "SquashEvent.NECTAR_TEXT4", "SquashEvent.NECTAR_TEXT5", "SquashEvent.NECTAR_TEXT6"
	};

	class TheInstance
	{
		long despawnTime;
	}

	Map<ChronoMonsterInstance, TheInstance> _monsterInstances = new ConcurrentHashMap<>();

	private TheInstance create(ChronoMonsterInstance mob)
	{
		final TheInstance mons = new TheInstance();
		_monsterInstances.put(mob, mons);
		return mons;
	}

	private TheInstance get(ChronoMonsterInstance mob)
	{
		return _monsterInstances.get(mob);
	}

	private void remove(ChronoMonsterInstance mob)
	{
		cancelQuestTimer("countdown", mob, null);
		cancelQuestTimer("despawn", mob, null);
		_monsterInstances.remove(mob);
	}

	public SquashEvent(String name, String descr)
	{
		super(name, descr);

		addAttackId(12774, 12775, 12776, 12777, 12778, 12779, 13016, 13017);
		addKillId(12774, 12775, 12776, 12777, 12778, 12779, 13016, 13017);
		addSpawnId(12774, 12775, 12776, 12777, 12778, 12779, 13016, 13017);
		addSkillSeeId(12774, 12775, 12776, 12777, 12778, 12779, 13016, 13017);

		addStartNpc(31255);
		addFirstTalkId(31255);
		addTalkId(31255);

		_template = WorldEventParser.getInstance().getEvent(16);
		if (_template != null && !_isActive)
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
		
		if (_eventTask != null)
		{
			_eventTask.cancel(false);
			_eventTask = null;
		}
		
		_isActive = true;
		
		final List<WorldEventSpawn> spawnList = _template.getSpawnList();
		if (spawnList != null && !spawnList.isEmpty())
		{
			for (final WorldEventSpawn spawn : spawnList)
			{
				_npcList.add(addSpawn(spawn.getNpcId(), spawn.getLocation().getX(), spawn.getLocation().getY(), spawn.getLocation().getZ(), spawn.getLocation().getHeading(), false, 0));
			}
		}
		
		if (_template.getDropList() != null && !_template.getDropList().isEmpty())
		{
			EventsDropManager.getInstance().addRule(_template.getId(), _template.getDropList(), true);
		}

		final ServerMessage msg = new ServerMessage("EventSquashes.START", true);
		Announcements.getInstance().announceToAll(msg);

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
		_npcList.clear();

		EventsDropManager.getInstance().removeRule(_template.getId());
		
		final ServerMessage msg = new ServerMessage("EventSquashes.STOP", true);
		Announcements.getInstance().announceToAll(msg);

		checkTimerTask(calcEventStartTime(_template, false));
		
		return true;
	}

	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		if (event.equalsIgnoreCase("countdown"))
		{
			final ChronoMonsterInstance mob = (ChronoMonsterInstance) npc;
			final TheInstance self = get(mob);
			final int timeLeft = (int) ((self.despawnTime - System.currentTimeMillis()) / 1000);
			if (timeLeft == 30)
			{
				autoChat(player, mob, "OTHER_TEXT1");
			}
			else if (timeLeft == 20)
			{
				autoChat(player, mob, "OTHER_TEXT2");
			}
			else if (timeLeft == 10)
			{
				autoChat(player, mob, "OTHER_TEXT3");
			}
			else if (timeLeft == 0)
			{
				if (mob.getLevelUp() == 0)
				{
					autoChat(player, mob, "OTHER_TEXT4");
				}
				else
				{
					autoChat(player, mob, "OTHER_TEXT5");
				}
			}
			else if ((timeLeft % 60) == 0)
			{
				if (mob.getLevelUp() == 0)
				{
					autoChat(player, mob, "OTHER_TEXT6" + (timeLeft / 60) + "OTHER_TEXT7");
				}
			}
		}
		else if (event.equalsIgnoreCase("despawn"))
		{
			remove((ChronoMonsterInstance) npc);
			npc.deleteMe();
		}
		else if (event.equalsIgnoreCase("sound"))
		{
			final ChronoMonsterInstance mob = (ChronoMonsterInstance) npc;
			mob.broadcastPacketToOthers(2000, new PlaySound(0, "ItemSound3.sys_sow_success", 0, 0, 0, 0, 0));
		}
		else
		{
			return super.onAdvEvent(event, npc, player);
		}
		return null;
	}

	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		if (player.getQuestState(getName()) == null)
		{
			newQuestState(player);
		}
		switch (npc.getId())
		{
			case 31255 :
				return "31255.htm";
		}
		throw new RuntimeException();
	}

	@Override
	public String onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		final ChronoMonsterInstance mob = (ChronoMonsterInstance) npc;
		Weapon weapon;
		final boolean isChronoAttack = !isSummon && ((weapon = attacker.getActiveWeaponItem()) != null) && contains(CHRONO_LIST, weapon.getId());
		switch (mob.getId())
		{
			case 12774 :
			case 12775 :
			case 12776 :
			case 13016 :
				if (isChronoAttack)
				{
					chronoText(attacker, mob);
				}
				else
				{
					noChronoText(attacker, mob);
				}
				break;
			case 12777 :
			case 12778 :
			case 12779 :
			case 13017 :
				if (isChronoAttack)
				{
					mob.setIsInvul(false);
					if (damage == 0)
					{
						mob.getStatus().reduceHp(5, attacker);
					}
					else if (damage > 12)
					{
						mob.getStatus().setCurrentHp((mob.getStatus().getCurrentHp() + damage) - 12);
					}
					chronoText(attacker, mob);
				}
				else
				{
					mob.setIsInvul(true);
					mob.setCurrentHp(mob.getMaxHp());
					noChronoText(attacker, mob);
				}
				break;
			default :
				throw new RuntimeException();
		}
		mob.getStatus().stopHpMpRegeneration();
		return super.onAttack(npc, attacker, damage, isSummon);
	}

	@Override
	public String onSkillSee(Npc npc, Player caster, Skill skill, GameObject[] targets, boolean isSummon)
	{
		if ((skill.getId() == 2005) && (targets[0] == npc))
		{
			final ChronoMonsterInstance mob = (ChronoMonsterInstance) npc;
			switch (mob.getId())
			{
				case 12774 :
				case 12777 :
					if (mob.getScriptValue() < 5)
					{
						mob.setScriptValue(mob.getScriptValue() + 1);
						nectarText(caster, mob);
						if (getRandom(100) < 50)
						{
							npc.doCast(SkillsParser.getInstance().getInfo(4514, 1));
						}
						else
						{
							npc.doCast(SkillsParser.getInstance().getInfo(4513, 1));
							mob.setLevelUp(mob.getLevelUp() + 1);
						}
						
						if (mob.getScriptValue() >= 5)
						{
							randomSpawn(mob);
						}
					}
					break;
			}
		}
		return null;
	}

	@Override
	public String onKill(Npc npc, Player killer, boolean isSummon)
	{
		final ChronoMonsterInstance mob = (ChronoMonsterInstance) npc;
		remove(mob);
		autoChat(killer, mob, KILL_TEXT[getRandom(KILL_TEXT.length)]);
		dropItem(npc, killer);
		return super.onKill(npc, killer, isSummon);
	}

	@Override
	public String onSpawn(Npc npc)
	{
		assert npc instanceof ChronoMonsterInstance;

		final ChronoMonsterInstance mob = (ChronoMonsterInstance) npc;
		mob.setOnKillDelay(1500);
		final TheInstance self = create(mob);
		
		Player player = null;
		for (final Player target : World.getInstance().getAroundPlayers(npc, 100, 200))
		{
			if (player == null)
			{
				player = target;
			}
		}
		switch (mob.getId())
		{
			case 12774 :
			case 12777 :
				startQuestTimer("countdown", 10000, mob, null, true);
				startQuestTimer("despawn", 180000, mob, null);
				self.despawnTime = System.currentTimeMillis() + 180000;
				autoChat(player, mob, SPAWN_TEXT[getRandom(SPAWN_TEXT.length)]);
				break;
			case 12775 :
			case 12776 :
			case 12778 :
			case 12779 :
			case 13016 :
			case 13017 :
				startQuestTimer("countdown", 10000, mob, null, true);
				startQuestTimer("despawn", 90000, mob, null);
				startQuestTimer("sound", 100, mob, null);
				self.despawnTime = System.currentTimeMillis() + 90000;
				autoChat(player, mob, GROWUP_TEXT[getRandom(GROWUP_TEXT.length)]);
				break;
			default :
				throw new RuntimeException();
		}
		return null;
	}

	private static final void dropItem(Npc mob, Player player)
	{
		switch (mob.getId())
		{
			case 12775 :
				calcRandomGroupReward(mob, player, _template, 1);
				break;
			case 12776 :
				calcRandomGroupReward(mob, player, _template, 2);
				break;
			case 13016 :
				calcRandomGroupReward(mob, player, _template, 3);
				break;
			case 12778 :
				calcRandomGroupReward(mob, player, _template, 4);
				break;
			case 12779 :
				calcRandomGroupReward(mob, player, _template, 5);
				break;
			case 13017 :
				calcRandomGroupReward(mob, player, _template, 6);
				break;
		}
	}

	private void randomSpawn(ChronoMonsterInstance mob)
	{
		int npcId = 0;
		switch (mob.getLevelUp())
		{
			case 5 :
				npcId = mob.getId() == 12774 ? 13016 : mob.getId() == 12777 ? 13017 : 0;
				break;
			case 4 :
				npcId = mob.getId() == 12774 ? 12775 : mob.getId() == 12777 ? 12778 : 0;
				break;
			case 3 :
			case 2 :
			case 1 :
			case 0 :
				npcId = mob.getId() == 12774 ? 12776 : mob.getId() == 12777 ? 12779 : 0;
				break;
		}
		
		if (npcId > 0)
		{
			spawnNext(npcId, mob);
		}
	}

	private void autoChat(Player player, ChronoMonsterInstance mob, String text)
	{
		ServerMessage msg = null;
		if (player != null)
		{
			msg = new ServerMessage(text, player.getLang());
		}
		else
		{
			msg = new ServerMessage(text, true);
		}
		mob.broadcastPacketToOthers(2000, new CreatureSay(mob.getObjectId(), Say2.ALL, mob.getName(null), msg.toString()));
	}

	private void chronoText(Player player, ChronoMonsterInstance mob)
	{
		if (getRandom(100) < 20)
		{
			autoChat(player, mob, CHRONO_TEXT[getRandom(CHRONO_TEXT.length)]);
		}
	}

	private void noChronoText(Player player, ChronoMonsterInstance mob)
	{
		if (getRandom(100) < 20)
		{
			autoChat(player, mob, NOCHRONO_TEXT[getRandom(NOCHRONO_TEXT.length)]);
		}
	}

	private void nectarText(Player player, ChronoMonsterInstance mob)
	{
		autoChat(player, mob, NECTAR_TEXT[getRandom(NECTAR_TEXT.length)]);
	}

	private void spawnNext(int npcId, ChronoMonsterInstance oldMob)
	{
		remove(oldMob);
		final ChronoMonsterInstance newMob = (ChronoMonsterInstance) addSpawn(npcId, oldMob.getX(), oldMob.getY(), oldMob.getZ(), oldMob.getHeading(), false, 0);
		newMob.setOwner(oldMob.getOwner());
		for (final String lang : Config.MULTILANG_ALLOWED)
		{
			if (lang != null)
			{
				newMob.setTitle(lang, oldMob.getTitle(lang));
			}
		}
		oldMob.deleteMe();
	}

	public static <T> boolean contains(T[] array, T obj)
	{
		for (final T element : array)
		{
			if (element == obj)
			{
				return true;
			}
		}
		return false;
	}

	public static boolean contains(int[] array, int obj)
	{
		for (final int element : array)
		{
			if (element == obj)
			{
				return true;
			}
		}
		return false;
	}

	@Override
	public String onEvent(String event, QuestState qs)
	{
		return event;
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
		
		_template = WorldEventParser.getInstance().getEvent(16);
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
		new SquashEvent(SquashEvent.class.getSimpleName(), "events");
	}
}
