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
package l2e.scripts.hellbound;

import l2e.commons.util.Util;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.service.BotFunctions;

public class Deltuva extends Quest
{
	public Deltuva(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(32313);
		addTalkId(32313);
	}
	
	@Override
	public final String onAdvEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		if (event.equalsIgnoreCase("teleport"))
		{
			final QuestState hostQuest = player.getQuestState("_132_MatrasCuriosity");
			if ((hostQuest == null) || !hostQuest.isCompleted())
			{
				htmltext = "32313-02.htm";
			}
			else
			{
				if (BotFunctions.getInstance().isAllowLicence() && player.isInParty())
				{
					if (player.getParty().isLeader(player) && player.getVarB("autoTeleport@", false))
					{
						for (final Player member : player.getParty().getMembers())
						{
							if (member != null)
							{
								if (member.getObjectId() == player.getObjectId())
								{
									continue;
								}
								
								if (!Util.checkIfInRange(1000, player, member, true))
								{
									continue;
								}
								
								final QuestState qs = member.getQuestState("_132_MatrasCuriosity");
								if ((qs == null) || !qs.isCompleted())
								{
									continue;
								}
								
								if (!BotFunctions.checkCondition(member, false) || !member.getIPAddress().equalsIgnoreCase(player.getIPAddress()))
								{
									continue;
								}
								member.teleToLocation(17934, 283189, -9701, true, member.getReflection());
							}
						}
					}
				}
				player.teleToLocation(17934, 283189, -9701, true, player.getReflection());
			}
		}
		return htmltext;
	}
	
	public static void main(String[] args)
	{
		new Deltuva(-1, Deltuva.class.getSimpleName(), "hellbound");
	}
}