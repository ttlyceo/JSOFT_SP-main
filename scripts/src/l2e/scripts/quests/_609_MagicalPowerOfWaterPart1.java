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

import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.holders.SkillHolder;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.quest.State;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.NpcSay;

/**
 * Rework by LordWinter 13.06.2020
 */
public class _609_MagicalPowerOfWaterPart1 extends Quest
{
  	public _609_MagicalPowerOfWaterPart1(int questId, String name, String descr)
  	{
		super(questId, name, descr);

		addStartNpc(31371);
		addTalkId(31372, 31371, 31561);
		
		addAttackId(21350, 21351, 21353, 21354, 21355, 21357, 21358, 21360, 21361, 21362, 21364, 21365, 21366, 21368, 21369, 21370, 21371, 21372, 21373, 21374, 21375);
		
		questItemIds = new int[]
		{
		        7237
		};
  	}

	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		final QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			return null;
		}
		String htmltext = null;
		switch (event)
		{
			case "31371-02.htm" :
				st.startQuest();
				htmltext = event;
				break;
			case "open_box" :
				if (!st.hasQuestItems(1661))
				{
					htmltext = "31561-02.htm";
				}
				else if (st.isCond(2))
				{
					if (st.isSet("spawned"))
					{
						st.takeItems(1661, 1);
						htmltext = "31561-04.htm";
					}
					else
					{
						st.giveItems(7237, 1);
						st.takeItems(1661, 1);
						st.setCond(3, true);
						htmltext = "31561-03.htm";
					}
				}
				break;
			case "eye_despawn" :
				npc.broadcastPacketToOthers(2000, new NpcSay(npc, Say2.NPC_ALL, NpcStringId.UDAN_HAS_ALREADY_SEEN_YOUR_FACE));
				npc.deleteMe();
				break;
		}
		return htmltext;
  	}
	
	@Override
	public String onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		final QuestState st = attacker.getQuestState(getName());
		if ((st != null) && st.isCond(2) && !st.isSet("spawned"))
		{
			st.set("spawned", "1");
			npc.setTarget(attacker);
			npc.doCast(new SkillHolder(4547, 1).getSkill());
			final Npc eye = addSpawn(31684, npc);
			if (eye != null)
			{
				eye.broadcastPacketToOthers(2000, new NpcSay(eye, Say2.NPC_ALL, NpcStringId.YOU_CANT_AVOID_THE_EYES_OF_UDAN));
				startQuestTimer("eye_despawn", 10000, eye, attacker);
			}
		}
		return super.onAttack(npc, attacker, damage, isSummon);
	}

	@Override
	public String onTalk(Npc npc, Player player)
	{
		String htmltext = getNoQuestMsg(player);
		final QuestState st = getQuestState(player, true);
		if (st == null)
		{
			return htmltext;
		}
		
		switch (npc.getId())
		{
			case 31371 :
				switch (st.getState())
				{
					case State.CREATED :
						htmltext = (player.getLevel() >= getMinLvl(getId())) ? (hasAtLeastOneQuestItem(player, new int[]
						{
						        7211, 7212, 7213, 7214, 7215
						})) ? "31371-01.htm" : "31371-00.htm" : "31371-00a.htm";
						break;
					case State.STARTED :
						if (st.isCond(1))
						{
							htmltext = "31371-03.htm";
						}
						break;
				}
				break;
			case 31372 :
				if (st.isStarted())
				{
					switch (st.getCond())
					{
						case 1 :
							htmltext = "31372-01.htm";
							st.setCond(2, true);
							break;
						case 2 :
							if (st.isSet("spawned"))
							{
								st.unset("spawned");
								npc.setTarget(player);
								npc.doCast(new SkillHolder(4548, 1).getSkill());
								htmltext = "31372-03.htm";
							}
							else
							{
								htmltext = "31372-02.htm";
							}
							break;
						case 3 :
							st.calcReward(getId());
							st.exitQuest(true, true);
							htmltext = "31372-04.htm";
							break;
					}
				}
				break;
			case 31561 :
				if (st.isCond(2))
				{
					htmltext = "31561-01.htm";
				}
				break;
		}
		return htmltext;
  	}

  	public static void main(String[] args)
  	{
		new _609_MagicalPowerOfWaterPart1(609, _609_MagicalPowerOfWaterPart1.class.getSimpleName(), "");
  	}
}