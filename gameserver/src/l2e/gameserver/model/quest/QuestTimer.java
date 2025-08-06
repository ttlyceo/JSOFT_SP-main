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
package l2e.gameserver.model.quest;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;

public class QuestTimer
{
	protected static final Logger _log = LoggerFactory.getLogger(QuestTimer.class);
	
	public class ScheduleTimerTask implements Runnable
	{
		@Override
		public void run()
		{
			if (!getIsActive())
			{
				return;
			}

			try
			{
				if (!getIsRepeating())
				{
					cancelAndRemove();
				}
				getQuest().notifyEvent(getName(), getNpc(), getPlayer());
			}
			catch (final Exception e)
			{
				_log.warn("", e);
			}
		}
	}

	private boolean _isActive = true;
	private final String _name;
	private final Quest _quest;
	private final Npc _npc;
	private final Player _player;
	private final boolean _isRepeating;
	private ScheduledFuture<?> _schedular;
	private int _instanceId;

	public QuestTimer(Quest quest, String name, long time, Npc npc, Player player, boolean repeating)
	{
		_name = name;
		_quest = quest;
		_player = player;
		_npc = npc;
		_isRepeating = repeating;
		if (npc != null)
		{
			_instanceId = npc.getReflectionId();
		}
		else if (player != null)
		{
			_instanceId = player.getReflectionId();
		}
		if (repeating)
		{
			_schedular = ThreadPoolManager.getInstance().scheduleAtFixedRate(new ScheduleTimerTask(), time, time);
		}
		else
		{
			_schedular = ThreadPoolManager.getInstance().schedule(new ScheduleTimerTask(), time);
		}
	}

	public QuestTimer(Quest quest, String name, long time, Npc npc, Player player)
	{
		this(quest, name, time, npc, player, false);
	}

	public QuestTimer(QuestState qs, String name, long time)
	{
		this(qs.getQuest(), name, time, null, qs.getPlayer(), false);
	}
	
	public void cancel()
	{
		_isActive = false;
		if (_schedular != null)
		{
			_schedular.cancel(false);
		}
	}
	
	public void cancelAndRemove()
	{
		cancel();
		_quest.removeQuestTimer(this);
	}
	
	public boolean isMatch(Quest quest, String name, Npc npc, Player player)
	{
		if ((quest == null) || (name == null))
		{
			return false;
		}
		if ((quest != _quest) || !name.equalsIgnoreCase(getName()))
		{
			return false;
		}
		return ((npc == _npc) && (player == _player));
	}

	public final boolean getIsActive()
	{
		return _isActive;
	}

	public final boolean getIsRepeating()
	{
		return _isRepeating;
	}

	public final Quest getQuest()
	{
		return _quest;
	}

	public final String getName()
	{
		return _name;
	}

	public final Npc getNpc()
	{
		return _npc;
	}

	public final Player getPlayer()
	{
		return _player;
	}

	public final int getReflectionId()
	{
		return _instanceId;
	}

	public final long getRemainDelay()
	{
		return _schedular == null ? 0 : _schedular.getDelay(TimeUnit.MILLISECONDS);
	}

	@Override
	public final String toString()
	{
		return _name;
	}
}