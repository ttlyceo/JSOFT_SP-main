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
package l2e.scripts.custom;


import l2e.commons.util.Util;
import l2e.gameserver.instancemanager.QuestManager;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.quest.State;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;
import l2e.gameserver.network.serverpackets.SystemMessage;

/**
 * Based on L2J Eternity-World
 */
public class Blessing extends Quest
{
	private static final int _priest = 32783;
	private static final int _price_voice = 100000;
	private static final int _nevit_voice = 17094;
	
	private static final int[] _prices_hourglass =
	{
	        4000, 30000, 110000, 310000, 970000, 2160000, 5000000
	};
	
	private static final int[][] _hourglasses =
	{
	        {
	                17095, 17096, 17097, 17098, 17099
			},
			{
			        17100, 17101, 17102, 17103, 17104
			},
			{
			        17105, 17106, 17107, 17108, 17109
			},
			{
			        17110, 17111, 17112, 17113, 17114
			},
			{
			        17115, 17116, 17117, 17118, 17119
			},
			{
			        17120, 17121, 17122, 17123, 17124
			},
			{
			        17125, 17126, 17127, 17128, 17129
			}
	};
	
	public Blessing(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(_priest);
		addFirstTalkId(_priest);
		addTalkId(_priest);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		
		final QuestState st = player.getQuestState(getName());
		final Quest q = QuestManager.getInstance().getQuest(getName());
		if ((st == null) || (q == null))
		{
			return null;
		}
		
		if (event.equalsIgnoreCase("buy_voice"))
		{
			if (st.getQuestItemsCount(57) >= _price_voice)
			{
				final String value = q.loadGlobalQuestVar(player.getAccountName() + "_voice");
				final long _reuse_time = value == null || value.isEmpty() ? 0 : Long.parseLong(value);
				
				if (System.currentTimeMillis() > _reuse_time)
				{
					st.setState(State.STARTED);
					st.takeItems(57, _price_voice);
					st.giveItems(_nevit_voice, 1);
					q.saveGlobalQuestVar(player.getAccountName() + "_voice", Long.toString(System.currentTimeMillis() + (20 * 3600000)));
				}
				else
				{
					final long remainingTime = (_reuse_time - System.currentTimeMillis()) / 1000;
					final int hours = (int) (remainingTime / 3600);
					final int minutes = (int) ((remainingTime % 3600) / 60);
					final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.AVAILABLE_AFTER_S1_S2_HOURS_S3_MINUTES);
					sm.addItemName(_nevit_voice);
					sm.addNumber(hours);
					sm.addNumber(minutes);
					player.sendPacket(sm);
				}
				return null;
			}
			htmltext = "32783-adena.htm";
		}
		else if (event.equalsIgnoreCase("buy_hourglass"))
		{
			final int _index = getHGIndex(player.getLevel());
			final int _price_hourglass = _prices_hourglass[_index];
			
			if (st.getQuestItemsCount(57) >= _price_hourglass)
			{
				final String value = q.loadGlobalQuestVar(player.getAccountName() + "_hg_" + _index);
				final long _reuse_time = value == null || value.isEmpty() ? 0 : Long.parseLong(value);
				
				if (System.currentTimeMillis() > _reuse_time)
				{
					final int[] _hg = _hourglasses[_index];
					final int _nevit_hourglass = _hg[getRandom(0, _hg.length - 1)];
					
					st.setState(State.STARTED);
					st.takeItems(57, _price_hourglass);
					st.giveItems(_nevit_hourglass, 1);
					q.saveGlobalQuestVar(player.getAccountName() + "_hg_" + _index, Long.toString(System.currentTimeMillis() + (20 * 3600000)));
				}
				else
				{
					final long remainingTime = (_reuse_time - System.currentTimeMillis()) / 1000;
					final int hours = (int) (remainingTime / 3600);
					final int minutes = (int) ((remainingTime % 3600) / 60);
					final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.AVAILABLE_AFTER_S1_S2_HOURS_S3_MINUTES);
					sm.addString("Nevit's Hourglass");
					sm.addNumber(hours);
					sm.addNumber(minutes);
					player.sendPacket(sm);
				}
				return null;
			}
			htmltext = "32783-adena.htm";
		}
		return htmltext;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			final Quest q = QuestManager.getInstance().getQuest(getName());
			st = q.newQuestState(player);
		}
		
		if (npc.getId() == _priest)
		{
			final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
			html.setFile(player, player.getLang(), "data/html/default/32783.htm");
			html.replace("%donate%", Util.formatAdena(_prices_hourglass[getHGIndex(player.getLevel())]));
			player.sendPacket(html);
			return null;
		}
		return "";
	}
	
	private int getHGIndex(int lvl)
	{
		int index = 0;
		if (lvl < 20)
		{
			index = 0;
		}
		else if (lvl < 40)
		{
			index = 1;
		}
		else if (lvl < 52)
		{
			index = 2;
		}
		else if (lvl < 61)
		{
			index = 3;
		}
		else if (lvl < 76)
		{
			index = 4;
		}
		else if (lvl < 80)
		{
			index = 5;
		}
		else if (lvl < 86)
		{
			index = 6;
		}
		return index;
	}
	
	public static void main(String[] args)
	{
		new Blessing(-1, "Blessing", "custom");
	}
}
