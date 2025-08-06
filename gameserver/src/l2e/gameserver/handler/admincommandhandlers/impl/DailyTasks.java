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
package l2e.gameserver.handler.admincommandhandlers.impl;

import l2e.gameserver.handler.admincommandhandlers.IAdminCommandHandler;
import l2e.gameserver.instancemanager.DailyTaskManager;
import l2e.gameserver.instancemanager.ServerVariables;
import l2e.gameserver.model.actor.Player;

public class DailyTasks implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
	        "admin_daily", "admin_weekly", "admin_month"
	};

	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		if (command.startsWith("admin_daily"))
		{
			DailyTaskManager.getInstance().checkDailyTimeTask(true);
			activeChar.sendMessage("Daily Tasks Clean Up!");
		}
		else if (command.startsWith("admin_weekly"))
		{
			ServerVariables.set("Weekly_Tasks", 0);
			DailyTaskManager.getInstance().checkWeeklyTimeTask();
			activeChar.sendMessage("Weekly Tasks Clean Up!");
		}
		else if (command.startsWith("admin_month"))
		{
			ServerVariables.set("Month_Tasks", 0);
			DailyTaskManager.getInstance().checkMonthTimeTask();
			activeChar.sendMessage("Month Tasks Clean Up!");
		}
		return true;
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}