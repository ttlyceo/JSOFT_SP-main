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
import l2e.gameserver.model.Party;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;

public class StakatoNest extends Quest
{
	private final static Location[] _locations =
	{
		new Location(80456, -52322, -5640),
		new Location(88718, -46214, -4640),
		new Location(87464, -54221, -5120),
		new Location(80848, -49426, -5128),
		new Location(87682, -43291, -4128)
	};
	
	public StakatoNest(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(32640);
		addTalkId(32640);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			st = newQuestState(player);
		}
		
		final int index = Integer.parseInt(event) - 1;
		
		if (_locations.length > index)
		{
			final Location loc = _locations[index];
			
			final Party party = player.getParty();
			if (party != null)
			{
				final Location playerLoc = player.getLocation();
				for (final Player member : party.getMembers())
				{
					if (member != null)
					{
						if (member.isInsideRadius(playerLoc.getX(), playerLoc.getY(), playerLoc.getZ(), 1000, true, true))
						{
							member.teleToLocation(loc, true, member.getReflection());
						}
					}
				}
			}
			else
			{
				player.teleToLocation(loc, true, player.getReflection());
			}
			st.exitQuest(true);
		}
		return "";
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		String htmltext = "";
		final QuestState accessQuest = player.getQuestState("_240_ImTheOnlyOneYouCanTrust");
		if ((accessQuest != null) && accessQuest.isCompleted())
		{
			htmltext = "32640.htm";
		}
		else
		{
			htmltext = "32640-no.htm";
		}
		return htmltext;
	}
	
	public static void main(String[] args)
	{
		new StakatoNest(-1, StakatoNest.class.getSimpleName(), "teleports");
	}
}