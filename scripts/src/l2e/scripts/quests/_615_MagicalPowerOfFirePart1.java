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
public class _615_MagicalPowerOfFirePart1 extends Quest
{
	public _615_MagicalPowerOfFirePart1(int questId, String name, String descr)
  	{
		super(questId, name, descr);

		addStartNpc(31378);
		addTalkId(31379, 31378, 31559);
		
		addAttackId(21324, 21325, 21327, 21328, 21329, 21331, 21332, 21334, 21335, 21336, 21338, 21339, 21340, 21342, 21343, 21344, 21345, 21346, 21347, 21348, 21349);
		
		questItemIds = new int[]
		{
		        7242
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
			case "31378-02.htm" :
				if (st.isCreated())
				{
					st.startQuest();
				}
				htmltext = event;
				break;
			case "open_box" :
				if (!st.hasQuestItems(1661))
				{
					htmltext = "31559-02.htm";
				}
				else if (st.isCond(2))
				{
					if (st.isSet("spawned"))
					{
						st.takeItems(1661, 1);
						htmltext = "31559-04.htm";
					}
					else
					{
						st.giveItems(7242, 1);
						st.takeItems(1661, 1);
						st.setCond(3, true);
						htmltext = "31559-03.htm";
					}
				}
				break;
			case "eye_despawn" :
				npc.broadcastPacketToOthers(2000, new NpcSay(npc, Say2.NPC_ALL, NpcStringId.ASEFA_HAS_ALREADY_SEEN_YOUR_FACE));
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
				eye.broadcastPacketToOthers(2000, new NpcSay(eye, Say2.NPC_ALL, NpcStringId.YOU_CANT_AVOID_THE_EYES_OF_ASEFA));
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
			case 31378 :
				switch (st.getState())
				{
					case State.CREATED :
						htmltext = (player.getLevel() >= getMinLvl(getId())) ? (hasAtLeastOneQuestItem(player, new int[]
						{
						        7221, 7222, 7223, 7224, 7225
						})) ? "31378-01.htm" : "31378-00.htm" : "31378-00a.htm";
						break;
					case State.STARTED :
						if (st.isCond(1))
						{
							htmltext = "31378-03.htm";
						}
						break;
				}
				break;
			case 31379 :
				if (st.isStarted())
				{
					switch (st.getCond())
					{
						case 1 :
							htmltext = "31379-01.htm";
							st.setCond(2, true);
							break;
						case 2 :
							if (st.isSet("spawned"))
							{
								st.unset("spawned");
								npc.setTarget(player);
								npc.doCast(new SkillHolder(4548, 1).getSkill());
								htmltext = "31379-03.htm";
							}
							else
							{
								htmltext = "31379-02.htm";
							}
							break;
						case 3 :
							st.calcReward(getId());
							st.exitQuest(true, true);
							htmltext = "31379-04.htm";
							break;
					}
				}
				break;
			case 31559 :
				if (st.isCond(2))
				{
					htmltext = "31559-01.htm";
				}
				break;
		}
		return htmltext;
	}

  	public static void main(String[] args)
  	{
		new _615_MagicalPowerOfFirePart1(615, _615_MagicalPowerOfFirePart1.class.getSimpleName(), "");
  	}
}