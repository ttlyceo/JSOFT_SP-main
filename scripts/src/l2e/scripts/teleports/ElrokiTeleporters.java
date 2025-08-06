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

import l2e.gameserver.data.parser.TeleLocationParser;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.service.BotFunctions;

public class ElrokiTeleporters extends Quest
{
	public ElrokiTeleporters(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(32111);
		addTalkId(32111);
		addStartNpc(32112);
		addTalkId(32112);
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		final String htmltext = "";
		final QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			return null;
		}
		
		switch (npc.getId())
		{
			case 32111:
				if (player.isInCombat())
				{
					return "32111-no.htm";
				}
				final var template = TeleLocationParser.getInstance().getTemplate(300003);
				if (template != null)
				{
					if (BotFunctions.getInstance().isAutoTpByIdEnable(player))
					{
						BotFunctions.getInstance().getAutoTeleportById(player, player.getLocation(), template.getLocation(), 1000);
						return null;
					}
					player.teleToLocation(template.getLocation(), true, player.getReflection());
				}
				break;
			case 32112:
				final var template2 = TeleLocationParser.getInstance().getTemplate(300004);
				if (template2 != null)
				{
					if (BotFunctions.getInstance().isAutoTpByIdEnable(player))
					{
						BotFunctions.getInstance().getAutoTeleportById(player, player.getLocation(), template2.getLocation(), 1000);
						return null;
					}
					player.teleToLocation(template2.getLocation(), true, player.getReflection());
				}
				break;
		}
		return htmltext;
	}
	
	public static void main(String[] args)
	{
		new ElrokiTeleporters(-1, ElrokiTeleporters.class.getSimpleName(), "teleports");
	}
}