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
package l2e.gameserver.model.actor.templates.player;

import l2e.gameserver.instancemanager.DailyTaskManager;
import l2e.gameserver.model.actor.templates.daily.DailyTaskTemplate;

/**
 * Created by LordWinter
 */
public class PlayerTaskTemplate
{
	private final int _id;
	private final String _type;
	private final String _sort;
	private int _currentNpcCount = 0;
	private int _currentPvpCount = 0;
	private int _currentPkCount = 0;
	private int _currentOlyMatchCount = 0;
	private int _currentEventsCount = 0;
	private boolean _isComplete = false;
	private boolean _isRewarded = false;
	DailyTaskTemplate _task = null;
	
	public PlayerTaskTemplate(int id, String type, String sort)
	{
		_id = id;
		_type = type;
		_sort = sort;
		_task = DailyTaskManager.getInstance().getDailyTask(id);
	}

	public int getId()
	{
		return _id;
	}
	
	public String getType()
	{
		return _type;
	}
	
	public String getSort()
	{
		return _sort;
	}
	
	public boolean isComplete()
	{
		return _isComplete;
	}
	
	public void setIsComplete(boolean complete)
	{
		_isComplete = complete;
	}
	
	public boolean isRewarded()
	{
		return _isRewarded;
	}
	
	public void setIsRewarded(boolean rewarded)
	{
		_isRewarded = rewarded;
	}
	
	public void setCurrentNpcCount(int count)
	{
		_currentNpcCount = count;
		if (_currentNpcCount >= _task.getNpcCount())
		{
			_isComplete = true;
		}
	}

	public int getCurrentNpcCount()
	{
		return _currentNpcCount;
	}
	
	public void setCurrentPvpCount(int count)
	{
		_currentPvpCount = count;
		if (_currentPvpCount >= _task.getPvpCount())
		{
			_isComplete = true;
		}
	}

	public int getCurrentPvpCount()
	{
		return _currentPvpCount;
	}
	
	public void setCurrentPkCount(int count)
	{
		_currentPkCount = count;
		if (_currentPkCount >= _task.getPkCount())
		{
			_isComplete = true;
		}
	}

	public int getCurrentPkCount()
	{
		return _currentPkCount;
	}
	
	public void setCurrentOlyMatchCount(int count)
	{
		_currentOlyMatchCount = count;
		if (_currentOlyMatchCount >= _task.getOlyMatchCount())
		{
			_isComplete = true;
		}
	}

	public int getCurrentOlyMatchCount()
	{
		return _currentOlyMatchCount;
	}
	
	public void setCurrentEventsCount(int count)
	{
		_currentEventsCount = count;
		if (_currentEventsCount >= _task.getEventsCount())
		{
			_isComplete = true;
		}
	}

	public int getCurrentEventsCount()
	{
		return _currentEventsCount;
	}
}