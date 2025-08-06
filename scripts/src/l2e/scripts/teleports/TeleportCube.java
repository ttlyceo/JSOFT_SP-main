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
package l2e.scripts.teleports;

import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.service.BotFunctions;

/**
 * Based on L2J Eternity-World
 */
public class TeleportCube extends Quest
{
	public TeleportCube(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(32107);
		addTalkId(32107);
	}

	@Override
	public String onTalk(Npc npc, Player player)
	{
		final String htmltext = "";
		final QuestState st = player.getQuestState(getName());
		final int npcId = npc.getId();

		if (npcId == 32107)
		{
			if (BotFunctions.getInstance().isAutoTpByIdEnable(player))
			{
				BotFunctions.getInstance().getAutoTeleportById(player, player.getLocation(), new Location(10468, -24569, -3650), 1000);
				return null;
			}
			player.teleToLocation(10468, -24569, -3650, true, player.getReflection());
			return null;
		}
		st.exitQuest(true);
		return htmltext;
	}

	public static void main(String[] args)
	{
		new TeleportCube(-1, TeleportCube.class.getSimpleName(), "teleports");
	}
}