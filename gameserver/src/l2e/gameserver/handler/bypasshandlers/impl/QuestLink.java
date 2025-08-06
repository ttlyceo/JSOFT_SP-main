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
package l2e.gameserver.handler.bypasshandlers.impl;

import java.util.ArrayList;
import java.util.List;

import l2e.commons.util.StringUtil;
import l2e.gameserver.data.htm.HtmCache;
import l2e.gameserver.handler.bypasshandlers.IBypassHandler;
import l2e.gameserver.instancemanager.QuestManager;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestEventType;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.quest.State;
import l2e.gameserver.model.strings.server.ServerMessage;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;

public class QuestLink implements IBypassHandler
{
	private static final String[] COMMANDS =
	{
	        "Quest"
	};
	
	@Override
	public boolean useBypass(String command, Player activeChar, Creature target)
	{
		if (target == null || !target.isNpc())
		{
			return false;
		}

		String quest = "";
		try
		{
			quest = command.substring(5).trim();
		}
		catch (final IndexOutOfBoundsException ioobe)
		{}
		if (quest.length() == 0)
		{
			showQuestWindow(activeChar, (Npc) target);
		}
		else
		{
			showQuestWindow(activeChar, (Npc) target, quest);
		}
		
		return true;
	}
	
	public static void showQuestChooseWindow(Player player, Npc npc, Quest[] quests)
	{
		final StringBuilder sb = StringUtil.startAppend(150, "<html><body>");
		for (final Quest q : quests)
		{
			StringUtil.append(sb, "<a action=\"bypass -h npc_", String.valueOf(npc.getObjectId()), "_Quest ", q.getName(), "\">[", q.getDescr(player));
			
			final QuestState qs = player.getQuestState(q.getScriptName());
			if (qs != null)
			{
				if ((qs.getState() == State.STARTED) && (qs.getInt("cond") > 0))
				{
					sb.append((new ServerMessage("quest.progress", player.getLang())).toString());
				}
				else if (qs.getState() == State.COMPLETED)
				{
					if (!qs.isNowAvailable())
					{
						sb.append((new ServerMessage("quest.done", player.getLang())).toString());
					}
				}
			}
			sb.append("]</a><br>");
		}
		
		sb.append("</body></html>");
		
		npc.insertObjectIdAndShowChatWindow(player, sb.toString());
	}
	
	public static void showQuestWindow(Player player, Npc npc, String questId)
	{
		String content = null;
		
		final Quest q = QuestManager.getInstance().getQuest(questId);
		
		QuestState qs = player.getQuestState(questId);
		
		if (q != null)
		{
			if (((q.getId() >= 1) && (q.getId() < 20000)) && ((player.getWeightPenalty() >= 3) || !player.isInventoryUnder90(true)))
			{
				player.sendPacket(SystemMessageId.INVENTORY_LESS_THAN_80_PERCENT);
				return;
			}
			
			if (qs == null)
			{
				if ((q.getId() >= 1) && (q.getId() < 20000))
				{
					if (player.getAllActiveQuests().length > 40)
					{
						final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
						html.setFile(player, player.getLang(), "data/html/fullquest.htm");
						player.sendPacket(html);
						return;
					}
				}
				final List<Quest> qlst = npc.getTemplate().getEventQuests(QuestEventType.QUEST_START);
				
				if ((qlst != null) && !qlst.isEmpty())
				{
					for (final Quest temp : qlst)
					{
						if (temp == q)
						{
							qs = q.newQuestState(player);
							break;
						}
					}
				}
			}
		}
		else
		{
			content = Quest.getNoQuestMsg(player);
		}
		
		if (qs != null)
		{
			if (!qs.getQuest().notifyTalk(npc, qs))
			{
				return;
			}
			
			questId = qs.getQuest().getName();
			final String stateId = State.getStateName(qs.getState());
			final String path = "data/html/scripts/quests/" + questId + "/" + stateId + ".htm";
			content = HtmCache.getInstance().getHtm(player, player.getLang(), path);
		}
		
		if (content != null)
		{
			npc.insertObjectIdAndShowChatWindow(player, content);
		}
		player.sendActionFailed();
	}
	
	public static void showQuestWindow(Player player, Npc npc)
	{
		final List<Quest> options = new ArrayList<>();
		final QuestState[] awaits = player.getQuestsForTalk(npc.getTemplate().getId());
		final List<Quest> starts = npc.getTemplate().getEventQuests(QuestEventType.QUEST_START);
		
		Quest quest998 = null;
		boolean blocked = false;
		if (awaits != null)
		{
			for (final QuestState x : awaits)
			{
				if (!options.contains(x.getQuest()))
				{
					if (x.getQuest().getId() == 998)
					{
						quest998 = x.getQuest();
						continue;
					}
					
					if (x.getQuest().getId() == 142 || x.getQuest().getId() == 143)
					{
						blocked = true;
					}
					
					if ((x.getQuest().getId() > 0) && (x.getQuest().getId() < 20000))
					{
						options.add(x.getQuest());
					}
				}
			}
		}
		
		if (starts != null)
		{
			for (final Quest x : starts)
			{
				if (!options.contains(x))
				{
					if (x.getId() == 998)
					{
						quest998 = x;
						continue;
					}
					
					if (x.getId() == 142 || x.getId() == 143)
					{
						blocked = true;
					}
					
					if ((x.getId() > 0) && (x.getId() < 20000))
					{
						options.add(x);
					}
				}
			}
		}
		
		if (quest998 != null && !blocked)
		{
			options.add(quest998);
		}
		
		if (options.size() > 1)
		{
			showQuestChooseWindow(player, npc, options.toArray(new Quest[options.size()]));
		}
		else if (options.size() == 1)
		{
			showQuestWindow(player, npc, options.get(0).getName());
		}
		else
		{
			showQuestWindow(player, npc, "");
		}
	}
	
	@Override
	public String[] getBypassList()
	{
		return COMMANDS;
	}
}