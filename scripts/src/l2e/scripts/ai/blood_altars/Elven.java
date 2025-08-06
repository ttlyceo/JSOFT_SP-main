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
package l2e.scripts.ai.blood_altars;

import java.util.concurrent.ScheduledFuture;

import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.model.entity.BloodAltarsEngine;

/**
 * Created by LordWinter 15.02.2019
 */
public class Elven extends BloodAltarsEngine
{
	private static ScheduledFuture<?> _changeStatusTask = null;
	
	private int _status = 0;
	private int _progress = 0;
	
	public Elven(String name, String descr)
	{
		super(name, descr);
		
		restoreStatus(getName());
	}
	
	@Override
	public boolean changeSpawnInterval(long time, int status, int progress)
	{
		if (_changeStatusTask != null)
		{
			_changeStatusTask.cancel(false);
			_changeStatusTask = null;
		}
		_status = status;
		_progress = progress;
		_changeStatusTask = ThreadPoolManager.getInstance().schedule(new Runnable()
		{
			@Override
			public void run()
			{
				changeStatus(getName(), getChangeTime(), getStatus());
			}
		}, time);
		
		return true;
	}

	@Override
	public int getStatus()
	{
		return _status;
	}
	
	@Override
	public int getProgress()
	{
		return _progress;
	}
	
	public static void main(String[] args)
	{
		new Elven(Elven.class.getSimpleName(), "ai");
	}
}
