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

import java.util.StringTokenizer;

import l2e.commons.util.Util;
import l2e.gameserver.handler.admincommandhandlers.IAdminCommandHandler;
import l2e.gameserver.instancemanager.QuestManager;
import l2e.gameserver.instancemanager.SoDManager;
import l2e.gameserver.instancemanager.SoIManager;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;

public class GraciaSeeds implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
	        "admin_gracia_seeds", "admin_opensod", "admin_defencesod", "admin_closesod", "admin_set_soistage"
	};
	
	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		final StringTokenizer st = new StringTokenizer(command, " ");
		final String cmd = st.nextToken();

		String val = "";
		if (st.countTokens() >= 1)
		{
			val = st.nextToken();
		}

		if (cmd.equalsIgnoreCase("admin_defencesod"))
		{
			SoDManager.getInstance().startDefenceStage(true, true);
		}
		else if (cmd.equalsIgnoreCase("admin_opensod"))
		{
			SoDManager.getInstance().openSeed(Integer.parseInt(val) * 60 * 1000L, true);
		}
		else if (cmd.equalsIgnoreCase("admin_closesod"))
		{
			final Quest qs = QuestManager.getInstance().getQuest("SoDDefenceStage");
			if (qs != null)
			{
				qs.notifyEvent("EndDefence", null, null);
			}
			else
			{
				SoDManager.getInstance().closeSeed();
			}
		}
		else if (cmd.equalsIgnoreCase("admin_set_soistage"))
		{
			SoIManager.getInstance().setCurrentStage(Integer.parseInt(val));
		}
		showMenu(activeChar);
		return true;
	}

	private void showMenu(Player activeChar)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile(activeChar, activeChar.getLang(), "data/html/admin/gracia.htm");
		html.replace("%sodstate%", String.valueOf(SoDManager.getInstance().isAttackStage() ? "1 (Attack)" : SoDManager.getInstance().isDefenceStage() ? "3 (Defence)" : "2 (Open)"));
		html.replace("%sodtiatkill%", String.valueOf(SoDManager.getInstance().getTiatKills()));
		if (!SoDManager.getInstance().isAttackStage())
		{
			if (SoDManager.getInstance().isDefenceStage())
			{
				html.replace("%sodtime%", Util.formatTime((int) SoDManager.getInstance().getDefenceStageTimeLimit() / 1000));
			}
			else
			{
				html.replace("%sodtime%", Util.formatTime((int) SoDManager.getInstance().getOpenedTimeLimit() / 1000));
			}
		}
		else
		{
			html.replace("%sodtime%", "-1");
		}
		html.replace("%soistage%", SoIManager.getInstance().getCurrentStage());
		activeChar.sendPacket(html);
	}

	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}