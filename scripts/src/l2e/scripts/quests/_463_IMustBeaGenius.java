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

import l2e.commons.apache.ArrayUtils;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.quest.QuestState.QuestType;
import l2e.gameserver.model.quest.State;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.ExNpcQuestHtmlMessage;
import l2e.gameserver.network.serverpackets.NpcSay;

/**
 * Updated by LordWinter 03.10.2011 Based on L2J Eternity-World
 */
public class _463_IMustBeaGenius extends Quest
{
	private static final String qn = "_463_IMustBeaGenius";
	
	private static final int _gutenhagen = 32069;
	private static final int _corpse_log = 15510;
	private static final int _collection = 15511;
	private static final int[] _mobs =
	{
	        22801, 22802, 22804, 22805, 22807, 22808, 22809, 22810, 22811, 22812
	};
	
	public _463_IMustBeaGenius(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(_gutenhagen);
		addTalkId(_gutenhagen);
		for (final int _mob : _mobs)
		{
			addKillId(_mob);
		}
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		final String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null || (st.isCompleted() && !st.isNowAvailable()))
		{
			return htmltext;
		}
		
		if (npc.getId() == _gutenhagen)
		{
			if (event.equalsIgnoreCase("32069-03"))
			{
				st.playSound("ItemSound.quest_accept");
				st.setState(State.STARTED);
				st.set("cond", "1");
				
				final int _number = getRandom(500, 600);
				st.set("number", String.valueOf(_number));
				for (final int _mob : _mobs)
				{
					int _rand = getRandom(-2, 4);
					if (_rand == 0)
					{
						_rand = 5;
					}
					st.set(String.valueOf(_mob), String.valueOf(_rand));
				}
				st.set(String.valueOf(_mobs[getRandom(0, _mobs.length - 1)]), String.valueOf(getRandom(1, 100)));
				
				final ExNpcQuestHtmlMessage html = new ExNpcQuestHtmlMessage(npc.getObjectId(), getId());
				html.setFile(player, player.getLang(), "data/html/scripts/quests/" + getName() + "/32069-03.htm");
				htmltext.replace("%num%", String.valueOf(_number));
				player.sendPacket(html);
			}
			else if (event.equalsIgnoreCase("32069-05"))
			{
				final ExNpcQuestHtmlMessage html = new ExNpcQuestHtmlMessage(npc.getObjectId(), getId());
				
				html.setFile(player, player.getLang(), "data/html/scripts/quests/" + getName() + "/32069-05.htm");
				html.replace("%num%", String.valueOf(st.get("number")));
				player.sendPacket(html);
			}
			else if (event.equalsIgnoreCase("32069-07.htm"))
			{
				if (st.isCond(2))
				{
					st.addExpAndSp(317961, 25427);
					st.unset("number");
					for (final int _mob : _mobs)
					{
						st.unset(String.valueOf(_mob));
					}
					st.takeItems(_collection, -1);
					st.playSound("ItemSound.quest_finish");
					st.setState(State.COMPLETED);
					st.exitQuest(QuestType.DAILY);
				}
			}
		}
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		String htmltext = getNoQuestMsg(player);
		final QuestState st = player.getQuestState(qn);
		if (st == null)
		{
			return htmltext;
		}
		
		if (npc.getId() == _gutenhagen)
		{
			switch (st.getState())
			{
				case State.CREATED :
					if (player.getLevel() >= 70)
					{
						htmltext = "32069-01.htm";
					}
					else
					{
						htmltext = "32069-00.htm";
					}
					break;
				case State.STARTED :
					if (st.getInt("cond") == 1)
					{
						htmltext = "32069-04.htm";
					}
					else if (st.getInt("cond") == 2)
					{
						htmltext = "32069-06.htm";
					}
					break;
				case State.COMPLETED :
					if (st.isNowAvailable())
					{
						if (player.getLevel() >= 70)
						{
							htmltext = "32069-01.htm";
						}
						else
						{
							htmltext = "32069-00.htm";
						}
					}
					else
					{
						htmltext = "32069-08.htm";
					}
					break;
			}
		}
		return htmltext;
	}
	
	@Override
	public String onKill(Npc npc, Player player, boolean isSummon)
	{
		final QuestState st = player.getQuestState(qn);
		if (st == null)
		{
			return null;
		}
		
		if ((st.getState() == State.STARTED) && (st.getInt("cond") == 1) && ArrayUtils.contains(_mobs, npc.getId()))
		{
			final int _day_number = st.getInt("number");
			final int _number = st.getInt(String.valueOf(npc.getId()));
			
			if (_number > 0)
			{
				st.giveItems(_corpse_log, _number);
				st.playSound("ItemSound.quest_itemget");
				final NpcSay ns = new NpcSay(npc.getObjectId(), Say2.NPC_ALL, npc.getId(), NpcStringId.ATT_ATTACK_S1_RO_ROGUE_S2);
				ns.addStringParameter(player.getName(null));
				ns.addStringParameter(String.valueOf(_number));
				npc.broadcastPacketToOthers(2000, ns);
			}
			else if ((_number < 0) && ((st.getQuestItemsCount(_corpse_log) + _number) > 0))
			{
				st.takeItems(_corpse_log, Math.abs(_number));
				st.playSound("ItemSound.quest_itemget");
				final NpcSay ns = new NpcSay(npc.getObjectId(), Say2.NPC_ALL, npc.getId(), NpcStringId.ATT_ATTACK_S1_RO_ROGUE_S2);
				ns.addStringParameter(player.getName(null));
				ns.addStringParameter(String.valueOf(_number));
				npc.broadcastPacketToOthers(2000, ns);
			}
			
			if (st.getQuestItemsCount(_corpse_log) == _day_number)
			{
				st.takeItems(_corpse_log, -1);
				st.giveItems(_collection, 1);
				st.set("cond", "2");
			}
			
		}
		return null;
	}
	
	public static void main(String[] args)
	{
		new _463_IMustBeaGenius(463, qn, "");
	}
}