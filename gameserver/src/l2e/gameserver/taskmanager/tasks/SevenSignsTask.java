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


import l2e.gameserver.SevenSigns;
import l2e.gameserver.SevenSignsFestival;

public class SevenSignsTask extends AutomaticTask
{
	@Override
	public void doTask() throws Exception
	{
		try
		{
			SevenSigns.getInstance().saveSevenSignsStatus();
			if (!SevenSigns.getInstance().isSealValidationPeriod())
			{
				SevenSignsFestival.getInstance().saveFestivalData(false);
			}
			_log.info("SevenSigns: Data updated successfully...");
		}
		catch (final Exception e)
		{
			_log.warn(getClass().getSimpleName() + ": SevenSigns: Failed to save Seven Signs configuration: " + e.getMessage());
		}
	}
	
	@Override
	public long reCalcTime(boolean start)
	{
		return System.currentTimeMillis() + 1800000L;
	}
}