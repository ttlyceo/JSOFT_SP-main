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

import l2e.commons.util.Util;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.service.BotFunctions;

public class TeleportWithCharm extends Quest
{
	public TeleportWithCharm(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(30540, 30576);
		addTalkId(30540, 30576);
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		String htmltext = "";
		final QuestState st = player.getQuestState(getName());
		
		if (st == null)
		{
			return null;
		}
		
		switch (npc.getId())
		{
			case 30540 :
			{
				if (st.hasQuestItems(1659))
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
									
									if (!Util.checkIfInRange(1000, player, member, true) || member.getInventory().getItemByItemId(1659) == null)
									{
										continue;
									}
									
									if (!BotFunctions.checkCondition(member, false) || !member.getIPAddress().equalsIgnoreCase(player.getIPAddress()))
									{
										continue;
									}
									takeItems(member, 1659, 1);
									member.teleToLocation(-80826, 149775, -3043, true, member.getReflection());
								}
							}
						}
					}
					st.takeItems(1659, 1);
					player.teleToLocation(-80826, 149775, -3043, true, player.getReflection());
					st.exitQuest(true);
				}
				else
				{
					st.exitQuest(true);
					htmltext = "30540-01.htm";
				}
				break;
			}
			case 30576 :
			{
				if (st.hasQuestItems(1658))
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
									
									if (!Util.checkIfInRange(1000, player, member, true) || member.getInventory().getItemByItemId(1658) == null)
									{
										continue;
									}
									
									if (!BotFunctions.checkCondition(member, false) || !member.getIPAddress().equalsIgnoreCase(player.getIPAddress()))
									{
										continue;
									}
									takeItems(member, 1658, 1);
									member.teleToLocation(-80826, 149775, -3043, true, member.getReflection());
								}
							}
						}
					}
					st.takeItems(1658, 1);
					player.teleToLocation(-80826, 149775, -3043, true, player.getReflection());
					st.exitQuest(true);
				}
				else
				{
					st.exitQuest(true);
					htmltext = "30576-01.htm";
				}
				break;
			}
		}
		return htmltext;
	}
	
	public static void main(String[] args)
	{
		new TeleportWithCharm(-1, TeleportWithCharm.class.getSimpleName(), "teleports");
	}
}