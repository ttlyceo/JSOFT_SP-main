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
package l2e.gameserver.taskmanager.tasks;


import l2e.commons.time.cron.SchedulingPattern;
import l2e.gameserver.instancemanager.SoIManager;

public class SoIStageTask extends AutomaticTask
{
	private static final SchedulingPattern PATTERN = new SchedulingPattern("0 12 * * 1");
	
	@Override
	public void doTask() throws Exception
	{
		SoIManager.getInstance().setCurrentStage(1);
		_log.info("Seed of Infinity: Seed updated successfuly...");
	}

	@Override
	public long reCalcTime(boolean start)
	{
		return PATTERN.next(System.currentTimeMillis());
	}
}