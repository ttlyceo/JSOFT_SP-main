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

import l2e.commons.util.TimeUtils;
import l2e.gameserver.Announcements;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.data.parser.WorldEventParser;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.entity.events.AbstractWorldEvent;
import l2e.gameserver.model.entity.events.EventsDropManager;
import l2e.gameserver.model.entity.events.model.template.WorldEventReward;
import l2e.gameserver.model.entity.events.model.template.WorldEventSpawn;
import l2e.gameserver.model.entity.events.model.template.WorldEventTemplate;
import l2e.gameserver.model.items.itemcontainer.PcInventory;
import l2e.gameserver.model.strings.server.ServerMessage;
import l2e.gameserver.network.SystemMessageId;

/**
 * Updated by Projack 24.07.2022
 */
public class LoversJubilee extends AbstractWorldEvent
{
    private boolean _isActive = false;
    private WorldEventTemplate _template = null;
	private ScheduledFuture<?> _eventTask = null;

    private final List<Npc> _npcList = new ArrayList<>();

	public LoversJubilee(String name, String descr)
	{
        super(name, descr);

        addStartNpc(4305);
        addFirstTalkId(4305);
        addTalkId(4305);

		_template = WorldEventParser.getInstance().getEvent(18);
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

        Announcements.getInstance().announceToAll(new ServerMessage("EventLoversJubilee.START", true));

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

        Announcements.getInstance().announceToAll(new ServerMessage("EventLoversJubilee.STOP", true));

		checkTimerTask(calcEventStartTime(_template, false));

        return true;
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

		_template = WorldEventParser.getInstance().getEvent(18);
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

    @Override
    public String onFirstTalk(Npc npc, Player player)
    {
        if (player.getQuestState(getName()) == null)
        {
            newQuestState(player);
        }
        return npc.getId() + ".htm";
    }

    @Override
    public String onAdvEvent(String event, Npc npc, Player player)
    {
        String htmltext = event;

		if (event.startsWith("menu_select"))
		{
            final String[] args = event.split(" ");
            final int reply = Integer.parseInt(args[1]);

            final PcInventory inv = player.getInventory();
			if (reply == 1)
			{
                if (inv.getInventoryItemCount(20921, 0) >= 1)
				{
					htmltext = "4305-010.htm";
				}
				else
				{
					htmltext = "4305-002.htm";
				}
			}
			else if (reply > 1 && reply < 8)
			{
				if (isTakeRequestItems(player, _template, reply))
				{
                    calcReward(player, _template, reply);
                    htmltext = "4305-023.htm";
				}
				else
				{
                    htmltext = "4305-024.htm";
                }
			}
			else if (reply == 8)
			{
                if(inv.getInventoryItemCount(20914, 0) >= 1)
				{
					htmltext = "4305-007.htm";
				}
				else
				{
					htmltext = "4305-008.htm";
				}
			}
			else
			{
                final List<WorldEventReward> rewards = _template.getVariantRewards().get(reply);
				if (rewards != null && player.getInventoryLimit() - player.getInventory().getSize() >= rewards.size())
				{
                    calcReward(player, _template, reply);
                    if (reply < 15)
					{
						htmltext = "4305-025.htm";
					}
					else
					{
						htmltext = "4305-026.htm";
					}
				}
				else
				{
                    player.sendPacket(SystemMessageId.CANT_RECEIVE_INVENTORY_FULL);
                }
            }
        }
        return htmltext;
    }

    public static void main(String[] args)
    {
        new LoversJubilee(LoversJubilee.class.getSimpleName(), "events");
    }
}