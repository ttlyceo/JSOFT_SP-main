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
import l2e.gameserver.data.parser.TeleLocationParser;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.service.BotFunctions;

/**
 * Based on L2J Eternity-World
 */
public class GraciaHeart extends Quest
{
	public GraciaHeart(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(36570);
		addTalkId(36570);
	}

	@Override
	public String onTalk(Npc npc, Player player)
	{
		String htmltext = "";
		final var st = player.getQuestState(getName());

		final int npcId = npc.getId();

		if (npcId == 36570)
		{
			final var template = TeleLocationParser.getInstance().getTemplate(300006);
			if (template != null)
			{
				if (player.getLevel() >= template.getMinLevel())
				{
					if (BotFunctions.getInstance().isAllowLicence() && player.isInParty())
					{
						if (player.getParty().isLeader(player) && player.getVarB("autoTeleport@", false))
						{
							for (final var member : player.getParty().getMembers())
							{
								if (member != null)
								{
									if (member.getObjectId() == player.getObjectId())
									{
										continue;
									}
									
									if (!Util.checkIfInRange(1000, player, member, true) || member.getLevel() < 75)
									{
										continue;
									}
									
									if (!BotFunctions.checkCondition(member, false) || !member.getIPAddress().equalsIgnoreCase(player.getIPAddress()))
									{
										continue;
									}
									member.teleToLocation(template.getLocation(), true, member.getReflection());
								}
							}
						}
					}
					player.teleToLocation(template.getLocation(), true, player.getReflection());
				}
				else
				{
					htmltext = "36570-00.htm";
				}
			}
		}
		st.exitQuest(true);
		return htmltext;
	}

	public static void main(String[] args)
	{
		new GraciaHeart(-1, GraciaHeart.class.getSimpleName(), "teleports");
	}
}