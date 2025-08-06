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

import l2e.commons.apache.ArrayUtils;
import l2e.commons.util.TimeUtils;
import l2e.gameserver.Announcements;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.data.parser.WorldEventParser;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.entity.events.AbstractWorldEvent;
import l2e.gameserver.model.entity.events.EventsDropManager;
import l2e.gameserver.model.entity.events.model.template.WorldEventReward;
import l2e.gameserver.model.entity.events.model.template.WorldEventSpawn;
import l2e.gameserver.model.entity.events.model.template.WorldEventTemplate;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.quest.State;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.strings.server.ServerMessage;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.CreatureSay;
import l2e.gameserver.network.serverpackets.SystemMessage;

/**
 * Updated by LordWinter 13.07.2020
 */
public class FreyaCelebration extends AbstractWorldEvent
{
	private boolean _isActive = false;
	private WorldEventTemplate _template = null;
	private ScheduledFuture<?> _eventTask = null;
	
	private final List<Npc> _npcList = new ArrayList<>();

	private static final int[] _skills =
	{
	        9150, 9151, 9152, 9153, 9154, 9155, 9156
	};

	private static final NpcStringId[] FREYA_TEXT =
	{
	        NpcStringId.EVEN_THOUGH_YOU_BRING_SOMETHING_CALLED_A_GIFT_AMONG_YOUR_HUMANS_IT_WOULD_JUST_BE_PROBLEMATIC_FOR_ME, NpcStringId.I_JUST_DONT_KNOW_WHAT_EXPRESSION_I_SHOULD_HAVE_IT_APPEARED_ON_ME_ARE_HUMANS_EMOTIONS_LIKE_THIS_FEELING, NpcStringId.THE_FEELING_OF_THANKS_IS_JUST_TOO_MUCH_DISTANT_MEMORY_FOR_ME, NpcStringId.BUT_I_KIND_OF_MISS_IT_LIKE_I_HAD_FELT_THIS_FEELING_BEFORE, NpcStringId.I_AM_ICE_QUEEN_FREYA_THIS_FEELING_AND_EMOTION_ARE_NOTHING_BUT_A_PART_OF_MELISSAA_MEMORIES
	};

	public FreyaCelebration(String name, String descr)
	{
		super(name, descr);

		addStartNpc(13296);
		addFirstTalkId(13296);
		addTalkId(13296);
		addSkillSeeId(13296);

		_template = WorldEventParser.getInstance().getEvent(6);
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

		final ServerMessage msg = new ServerMessage("EventFreyaCelebration.START", true);
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

		final ServerMessage msg = new ServerMessage("EventFreyaCelebration.STOP", true);
		Announcements.getInstance().announceToAll(msg);

		checkTimerTask(calcEventStartTime(_template, false));
		
		return true;
	}

	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		final QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			return null;
		}

		if (event.equalsIgnoreCase("give_potion"))
		{
			final long _curr_time = System.currentTimeMillis();
			final String value = loadGlobalQuestVar(player.getAccountName());
			final long _reuse_time = value == null || value.isEmpty() ? 0 : Long.parseLong(value);
			
			if (_curr_time > _reuse_time)
			{
				if (isTakeRequestItems(player, _template, 1))
				{
					st.setState(State.STARTED);
					calcReward(player, _template, 1);
					saveGlobalQuestVar(player.getAccountName(), Long.toString(System.currentTimeMillis() + (20 * 3600000)));
				}
				else
				{
					player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_REQUIRED_ITEMS));
				}
			}
			else
			{
				final List<WorldEventReward> rewards = _template.getVariantRewards().get(1);
				if (rewards != null && !rewards.isEmpty())
				{
					for (final WorldEventReward reward : rewards)
					{
						if (reward != null)
						{
							final long remainingTime = (_reuse_time - System.currentTimeMillis()) / 1000;
							final int hours = (int) (remainingTime / 3600);
							final int minutes = (int) ((remainingTime % 3600) / 60);
							final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.AVAILABLE_AFTER_S1_S2_HOURS_S3_MINUTES);
							sm.addItemName(reward.getId());
							sm.addNumber(hours);
							sm.addNumber(minutes);
							player.sendPacket(sm);
						}
					}
				}
			}
		}
		return null;
	}

	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			st = newQuestState(player);
		}
		return "13296.htm";
	}

	@Override
	public String onSkillSee(Npc npc, Player caster, Skill skill, GameObject[] targets, boolean isSummon)
	{
		if ((caster == null) || (npc == null))
		{
			return null;
		}

		if ((npc.getId() == 13296) && ArrayUtils.contains(targets, npc) && ArrayUtils.contains(_skills, skill.getId()))
		{
			if (getRandom(100) < 5)
			{
				final CreatureSay cs = new CreatureSay(npc.getObjectId(), Say2.NPC_ALL, npc.getName(null), NpcStringId.DEAR_S1_THINK_OF_THIS_AS_MY_APPRECIATION_FOR_THE_GIFT_TAKE_THIS_WITH_YOU_THERES_NOTHING_STRANGE_ABOUT_IT_ITS_JUST_A_BIT_OF_MY_CAPRICIOUSNESS);
				cs.addStringParameter(caster.getName(null));
				npc.broadcastPacketToOthers(2000, cs);
				calcReward(caster, _template, 2);
			}
			else
			{
				if (getRandom(10) < 2)
				{
					npc.broadcastPacketToOthers(2000, new CreatureSay(npc.getObjectId(), Say2.NPC_ALL, npc.getName(null), FREYA_TEXT[getRandom(FREYA_TEXT.length - 1)]));
				}
			}
		}
		return super.onSkillSee(npc, caster, skill, targets, isSummon);
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
		
		_template = WorldEventParser.getInstance().getEvent(6);
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
		new FreyaCelebration(FreyaCelebration.class.getSimpleName(), "events");
	}
}