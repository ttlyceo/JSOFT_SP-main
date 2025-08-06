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
package l2e.gameserver.handler.admincommandhandlers.impl;

import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import l2e.commons.util.Util;
import l2e.gameserver.data.htm.HtmCache;
import l2e.gameserver.handler.admincommandhandlers.IAdminCommandHandler;
import l2e.gameserver.instancemanager.QuestManager;
import l2e.gameserver.listener.ScriptListenerLoader;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestEventType;
import l2e.gameserver.model.quest.QuestTimer;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;

public class Quests implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
	        "admin_quest_reload", "admin_script_load", "admin_script_unload", "admin_show_quests", "admin_quest_info", "admin_scripts", "admin_script_reload"
	};

	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		if (activeChar == null)
		{
			return false;
		}
		
		if (command.startsWith("admin_scripts"))
		{
			final var scripts = QuestManager.getInstance().getAllScripts();
			if (scripts.isEmpty())
			{
				return false;
			}
			
			final String[] parts = command.split(" ");
			final int page = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
			
			String html = HtmCache.getInstance().getHtm(activeChar, activeChar.getLang(), "data/html/admin/scripts.htm");
			final String template = HtmCache.getInstance().getHtm(activeChar, activeChar.getLang(), "data/html/admin/script-template.htm");
			String block = "";
			String list = "";
			
			final int perpage = 8;
			int counter = 0;
			final int totalSize = scripts.size();
			final boolean isThereNextPage = totalSize > perpage;
			
			for (int i = (page - 1) * perpage; i < totalSize; i++)
			{
				final var data = scripts.get(i);
				if (data != null)
				{
					block = template;
					
					final var name = data.toString().substring(0, data.toString().lastIndexOf('@'));
					block = block.replace("{name}", name);
					list += block;
				}
				
				counter++;
				
				if (counter >= perpage)
				{
					break;
				}
			}
			
			final double pages = (double) totalSize / perpage;
			final int count = (int) Math.ceil(pages);
			html = html.replace("{list}", list);
			html = html.replace("{page}", String.valueOf(page));
			html = html.replace("{navigation}", Util.getNavigationBlock(count, page, totalSize, perpage, isThereNextPage, "admin_scripts %s"));
			Util.setHtml(html, activeChar);
		}
		else if (command.startsWith("admin_script_reload"))
		{
			final String[] parts = command.split(" ");
			if (parts.length < 2)
			{
				activeChar.sendMessage("Usage: //admin_script_reload <name>");
			}
			final int page = parts.length > 2 ? Integer.parseInt(parts[2]) : 1;
			QuestManager.getInstance().reloadScript(parts[1]);
			useAdminCommand("admin_scripts " + page, activeChar);
		}
		else if (command.startsWith("admin_quest_reload"))
		{
			final String[] parts = command.split(" ");
			if (parts.length < 2)
			{
				activeChar.sendMessage("Usage: //quest_reload <questFolder>.<questSubFolders...>.questName> or //quest_reload <id>");
			}
			else
			{
				try
				{
					final int questId = Integer.parseInt(parts[1]);
					if (QuestManager.getInstance().reload(questId))
					{
						activeChar.sendMessage("Quest Reloaded Successfully.");
					}
					else
					{
						activeChar.sendMessage("Quest Reloaded Failed");
					}
				}
				catch (final NumberFormatException e)
				{
					if (QuestManager.getInstance().reload(parts[1]))
					{
						activeChar.sendMessage("Quest Reloaded Successfully.");
					}
					else
					{
						activeChar.sendMessage("Quest Reloaded Failed");
					}
				}
			}
		}
		else if (command.startsWith("admin_script_load"))
		{
			final String[] parts = command.split(" ");
			if (parts.length < 2)
			{
				activeChar.sendMessage("Example: //script_load path/to/script.java");
			}
			else
			{
				final String script = parts[1];
				try
				{
					ScriptListenerLoader.getInstance().executeScript(Paths.get(script));
					activeChar.sendMessage("Script loaded seccessful!");
				}
				catch (final Exception e)
				{
					activeChar.sendMessage("Failed to load script " + script + "!");
				}
			}
			
		}
		else if (command.startsWith("admin_script_unload"))
		{
			final String[] parts = command.split(" ");
			if (parts.length < 2)
			{
				activeChar.sendMessage("Example: //script_unload questName/questId");
			}
			else
			{
				final Quest q = Util.isDigit(parts[1]) ? QuestManager.getInstance().getQuest(Integer.parseInt(parts[1])) : QuestManager.getInstance().getQuest(parts[1]);
				
				if (q != null)
				{
					if (q.unload())
					{
						activeChar.sendMessage("Script Successfully Unloaded [" + q.getName() + "/" + q.getId() + "]");
					}
					else
					{
						activeChar.sendMessage("Failed unloading [" + q.getName() + "/" + q.getId() + "].");
					}
				}
				else
				{
					activeChar.sendMessage("The quest [" + parts[1] + "] was not found!.");
				}
			}
		}
		else if (command.startsWith("admin_show_quests"))
		{
			if (activeChar.getTarget() == null)
			{
				activeChar.sendMessage("Get a target first.");
			}
			else if (!activeChar.getTarget().isNpc())
			{
				activeChar.sendMessage("Invalid Target.");
			}
			else
			{
				final Npc npc = Npc.class.cast(activeChar.getTarget());
				final NpcHtmlMessage msg = new NpcHtmlMessage(npc.getObjectId(), 1);
				msg.setFile(activeChar, activeChar.getLang(), "data/html/admin/npc-quests.htm");
				final StringBuilder sb = new StringBuilder();
				final Set<String> questset = new HashSet<>();
				for (final Entry<QuestEventType, List<Quest>> quests : npc.getTemplate().getEventQuests().entrySet())
				{
					for (final Quest quest : quests.getValue())
					{
						if (questset.contains(quest.getName()))
						{
							continue;
						}
						questset.add(quest.getName());
						sb.append("<tr><td colspan=\"4\"><font color=\"LEVEL\"><a action=\"bypass -h admin_quest_info " + quest.getName() + "\">" + quest.getName() + "</a></font></td></tr>");
					}
				}
				msg.replace("%quests%", sb.toString());
				msg.replace("%tmplid%", Integer.toString(npc.getTemplate().getId()));
				msg.replace("%questName%", "");
				activeChar.sendPacket(msg);
				questset.clear();
			}
		}
		else if (command.startsWith("admin_quest_info "))
		{
			if (activeChar.getTarget() == null)
			{
				activeChar.sendMessage("Get a target first.");
			}
			else if (!activeChar.getTarget().isNpc())
			{
				activeChar.sendMessage("Invalid Target.");
			}
			else
			{
				final String questName = command.substring("admin_quest_info ".length());
				final Quest quest = QuestManager.getInstance().getQuest(questName);
				if (quest == null)
				{
					return false;
				}
				final Npc npc = Npc.class.cast(activeChar.getTarget());
				final StringBuilder sb = new StringBuilder();
				final NpcHtmlMessage msg = new NpcHtmlMessage(npc.getObjectId(), 1);
				msg.setFile(activeChar, activeChar.getLang(), "data/html/admin/npc-quests.htm");
				String events = "", npcs = "", items = "", timers = "";
				
				for (final QuestEventType type : npc.getTemplate().getEventQuests().keySet())
				{
					events += ", " + type.toString();
				}
				events = events.substring(2);
				
				if (quest.getQuestInvolvedNpcs().size() < 100)
				{
					for (final int npcId : quest.getQuestInvolvedNpcs())
					{
						npcs += ", " + npcId;
					}
					npcs = npcs.substring(2);
				}
				
				if (quest.getRegisteredItemIds() != null)
				{
					for (final int itemId : quest.getRegisteredItemIds())
					{
						items += ", " + itemId;
					}
					items = items.substring(2);
				}
				
				for (final List<QuestTimer> list : quest.getQuestTimers().values())
				{
					for (final QuestTimer timer : list)
					{
						timers += "<tr><td colspan=\"4\"><table width=270 border=0 bgcolor=131210><tr><td width=270><font color=\"LEVEL\">" + timer.getName() + ":</font> <font color=00FF00>Active: " + timer.getIsActive() + " Repeatable: " + timer.getIsRepeating() + " Player: " + timer.getPlayer() + " Npc: " + timer.getNpc() + "</font></td></tr></table></td></tr>";
					}
				}
				
				sb.append("<tr><td colspan=\"4\"><table width=270 border=0 bgcolor=131210><tr><td width=270><font color=\"LEVEL\">ID:</font> <font color=00FF00>" + quest.getId() + "</font></td></tr></table></td></tr>");
				sb.append("<tr><td colspan=\"4\"><table width=270 border=0 bgcolor=131210><tr><td width=270><font color=\"LEVEL\">Name:</font> <font color=00FF00>" + quest.getName() + "</font></td></tr></table></td></tr>");
				sb.append("<tr><td colspan=\"4\"><table width=270 border=0 bgcolor=131210><tr><td width=270><font color=\"LEVEL\">Descr:</font> <font color=00FF00>" + quest.getDescr(activeChar) + "</font></td></tr></table></td></tr>");
				sb.append("<tr><td colspan=\"4\"><table width=270 border=0 bgcolor=131210><tr><td width=270><font color=\"LEVEL\">Path:</font> <font color=00FF00>" + quest.getClass().getName().substring(0, quest.getClass().getName().lastIndexOf('.')).replaceAll("\\.", "/") + "</font></td></tr></table></td></tr>");
				sb.append("<tr><td colspan=\"4\"><table width=270 border=0 bgcolor=131210><tr><td width=270><font color=\"LEVEL\">Events:</font> <font color=00FF00>" + events + "</font></td></tr></table></td></tr>");
				if (!npcs.isEmpty())
				{
					sb.append("<tr><td colspan=\"4\"><table width=270 border=0 bgcolor=131210><tr><td width=270><font color=\"LEVEL\">NPCs:</font> <font color=00FF00>" + npcs + "</font></td></tr></table></td></tr>");
				}
				if (!items.isEmpty())
				{
					sb.append("<tr><td colspan=\"4\"><table width=270 border=0 bgcolor=131210><tr><td width=270><font color=\"LEVEL\">Items:</font> <font color=00FF00>" + items + "</font></td></tr></table></td></tr>");
				}
				if (!timers.isEmpty())
				{
					sb.append("<tr><td colspan=\"4\"><table width=270 border=0 bgcolor=131210><tr><td width=270><font color=\"LEVEL\">Timers:</font> <font color=00FF00></font></td></tr></table></td></tr>");
					sb.append(timers);
				}
				msg.replace("%quests%", sb.toString());
				msg.replace("%tmplid%", Integer.toString(npc.getId()));
				msg.replace("%questName%", "<table><tr><td width=\"50\" align=\"left\"><a action=\"bypass -h admin_script_load " + quest.getName() + "\">Reload</a></td> <td width=\"150\"  align=\"center\"><a action=\"bypass -h admin_quest_info " + quest.getName() + "\">" + quest.getName() + "</a></td> <td width=\"50\" align=\"right\"><a action=\"bypass -h admin_script_unload " + quest.getName() + "\">Unload</a></tr></td></table>");
				activeChar.sendPacket(msg);
			}
		}
		return true;
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}