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
package l2e.scripts.quests;

import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.quest.QuestState;

/**
 * Rework by LordWinter 12.12.2021
 */
public class _073_SagaOfTheDuelist extends SagasSuperClass
{
	private final int TUNATUN = 31537;
	private final int TOPQUALITYMEAT = 7546;
	
	public _073_SagaOfTheDuelist(int id, String name, String descr)
	{
		super(id, name, descr);
		
		_npcs = new int[]
		{
		        30849, 31624, 31226, 31331, 31639, 31646, 31647, 31653, 31654, 31655, 31656, 31277
		};
		
		_items = new int[]
		{
		        7080, 7537, 7081, 7488, 7271, 7302, 7333, 7364, 7395, 7426, 7096, 7546
		};
		
		_mobs = new int[]
		{
		        27289, 27222, 27281
		};
		
		_classId = new int[]
		{
			88
		};
		
		_prevClass = new int[]
		{
			0x02
		};
		
		_npcSpawnLoc = new Location[]
		{
		        new Location(164650, -74121, -2871), new Location(47429, -56923, -2383), new Location(47391, -56929, -2370)
		};
		
		_msgs = new String[]
		{
			"PLAYERNAME! Pursued to here! However, I jumped out of the Banshouren boundaries! You look at the giant as the sign of power!",
			"... Oh ... good! So it was ... let's begin!",
			"I do not have the patience ..! I have been a giant force ...! Cough chatter ah ah ah!",
			"Paying homage to those who disrupt the orderly will be PLAYERNAME's death!",
			"Now, my soul freed from the shackles of the millennium, Halixia, to the back side I come ...",
			"Why do you interfere others' battles?",
			"This is a waste of time.. Say goodbye...!",
			"...That is the enemy",
			"...Goodness! PLAYERNAME you are still looking?",
			"PLAYERNAME ... Not just to whom the victory. Only personnel involved in the fighting are eligible to share in the victory.",
			"Your sword is not an ornament. Don't you think, PLAYERNAME?",
			"Goodness! I no longer sense a battle there now.",
			"let...",
			"Only engaged in the battle to bar their choice. Perhaps you should regret.",
			"The human nation was foolish to try and fight a giant's strength.",
			"Must...Retreat... Too...Strong.",
			"PLAYERNAME. Defeat...by...retaining...and...Mo...Hacker",
			"....! Fight...Defeat...It...Fight...Defeat...It..."
		};
		registerNPCs();
		
		addTalkId(TUNATUN);
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		if (npc.getId() == TUNATUN)
		{
			final String htmltext = getNoQuestMsg(player);
			final QuestState st = player.getQuestState(getName());
			if ((st != null) && st.isCond(3))
			{
				if (!st.hasQuestItems(TOPQUALITYMEAT))
				{
					st.giveItems(TOPQUALITYMEAT, 1);
					return "tunatun_01.htm";
				}
				return "tunatun_02.htm";
			}
			return htmltext;
		}
		return super.onTalk(npc, player);
	}
	
	public static void main(String[] args)
	{
		new _073_SagaOfTheDuelist(73, _073_SagaOfTheDuelist.class.getSimpleName(), "");
	}
}
