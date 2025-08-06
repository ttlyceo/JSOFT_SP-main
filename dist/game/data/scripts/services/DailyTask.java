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
 * this program. If not, see <>.
 */
package services;

import l2e.commons.util.HtmlUtil;
import l2e.commons.util.Rnd;
import l2e.commons.util.Util;
import l2e.gameserver.data.htm.HtmCache;
import l2e.gameserver.data.parser.ItemsParser;
import l2e.gameserver.handler.communityhandlers.impl.AbstractCommunity;
import l2e.gameserver.handler.voicedcommandhandlers.IVoicedCommandHandler;
import l2e.gameserver.handler.voicedcommandhandlers.VoicedCommandHandler;
import l2e.gameserver.instancemanager.DailyTaskManager;
import l2e.gameserver.instancemanager.QuestManager;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.daily.DailyTaskTemplate;
import l2e.gameserver.model.actor.templates.player.PlayerTaskTemplate;
import l2e.gameserver.model.olympiad.Olympiad;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.CreatureSay;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by LordWinter
 */
public class DailyTask implements IVoicedCommandHandler
{
	private final String[] _commandList = new String[]
	{
	        "missions", "dailyTask", "setDailyTask", "setWeeklyTask", "setMonthTask", "rewardTask", "cancelTask"
	};

	@Override
	public boolean useVoicedCommand(final String command, final Player player, final String args)
	{
		String[] param = null;
		if (args != null)
		{
			param = args.split(" ");
		}
		
		if (command.startsWith("missions") || command.startsWith("dailyTask"))
		{
			String page = "1";
			if (param != null && param.length > 0)
			{
				page = param[0];
			}
			showInfo(player, Integer.parseInt(page));
			return true;
		}
		else if (command.startsWith("setDailyTask"))
		{
			if (player.getActiveTasks("daily") < DailyTaskManager.getInstance().getTaskPerDay() && player.getLastDailyTasks() > 0)
			{
				String page = "1";
				if (param != null && param.length > 0)
				{
					page = param[0];
				}
				if (DailyTaskManager.getInstance().getTaskPrice()[0] > 0)
				{
					if ((player.getInventory().getItemByItemId(DailyTaskManager.getInstance().getTaskPrice()[0]) == null) || (player.getInventory().getItemByItemId(DailyTaskManager.getInstance().getTaskPrice()[0]).getCount() < DailyTaskManager.getInstance().getTaskPrice()[1]))
					{
						sendErrorMessageToPlayer(player, "You didn't have enough items.");
						return false;
					}
					player.destroyItemByItemId("Daily Price", DailyTaskManager.getInstance().getTaskPrice()[0], DailyTaskManager.getInstance().getTaskPrice()[1], player, true);
				}
				
				final DailyTaskTemplate rndTask = rndTaskSelect(player, "daily");
				if (rndTask != null)
				{
					final PlayerTaskTemplate playerTask = new PlayerTaskTemplate(rndTask.getId(), rndTask.getType(), rndTask.getSort());
					player.addDailyTask(playerTask);
				}
				showInfo(player, Integer.parseInt(page));
			}
			return true;
		}
		else if (command.startsWith("setWeeklyTask"))
		{
			if (player.getActiveTasks("weekly") < DailyTaskManager.getInstance().getTaskPerWeek() && player.getLastWeeklyTasks() > 0)
			{
				String page = "1";
				if (param != null && param.length > 0)
				{
					page = param[0];
				}
				if (DailyTaskManager.getInstance().getTaskPrice()[0] > 0)
				{
					if ((player.getInventory().getItemByItemId(DailyTaskManager.getInstance().getTaskPrice()[0]) == null) || (player.getInventory().getItemByItemId(DailyTaskManager.getInstance().getTaskPrice()[0]).getCount() < DailyTaskManager.getInstance().getTaskPrice()[1]))
					{
						sendErrorMessageToPlayer(player, "You didn't have enough items.");
						return false;
					}
					player.destroyItemByItemId("Daily Price", DailyTaskManager.getInstance().getTaskPrice()[0], DailyTaskManager.getInstance().getTaskPrice()[1], player, true);
				}
				
				final DailyTaskTemplate rndTask = rndTaskSelect(player, "weekly");
				if (rndTask != null)
				{
					final PlayerTaskTemplate playerTask = new PlayerTaskTemplate(rndTask.getId(), rndTask.getType(), rndTask.getSort());
					player.addDailyTask(playerTask);
				}
				showInfo(player, Integer.parseInt(page));
			}
			return true;
		}
		else if (command.startsWith("setMonthTask"))
		{
			if (player.getActiveTasks("month") < DailyTaskManager.getInstance().getTaskPerMonth() && player.getLastMonthTasks() > 0)
			{
				String page = "1";
				if (param != null && param.length > 0)
				{
					page = param[0];
				}
				if (DailyTaskManager.getInstance().getTaskPrice()[0] > 0)
				{
					if ((player.getInventory().getItemByItemId(DailyTaskManager.getInstance().getTaskPrice()[0]) == null) || (player.getInventory().getItemByItemId(DailyTaskManager.getInstance().getTaskPrice()[0]).getCount() < DailyTaskManager.getInstance().getTaskPrice()[1]))
					{
						sendErrorMessageToPlayer(player, "You didn't have enough items.");
						return false;
					}
					player.destroyItemByItemId("Daily Price", DailyTaskManager.getInstance().getTaskPrice()[0], DailyTaskManager.getInstance().getTaskPrice()[1], player, true);
				}

				final DailyTaskTemplate rndTask = rndTaskSelect(player, "month");
				if (rndTask != null)
				{
					final PlayerTaskTemplate playerTask = new PlayerTaskTemplate(rndTask.getId(), rndTask.getType(), rndTask.getSort());
					player.addDailyTask(playerTask);
				}
				showInfo(player, Integer.parseInt(page));
			}
			return true;
		}
		else if (command.startsWith("rewardTask"))
		{
			String id = null;
			if (param != null && param.length > 0)
			{
				id = param[0];
			}
			
			String page = "1";
			if (param != null && param.length > 1)
			{
				page = param[1];
			}
			
			final PlayerTaskTemplate playerTask = player.getDailyTaskTemplate(Integer.parseInt(id));
			if ((playerTask != null) && playerTask.isComplete() && !playerTask.isRewarded())
			{
				final DailyTaskTemplate task = DailyTaskManager.getInstance().getDailyTask(Integer.parseInt(id));
				if (task.getRewards() != null && !task.getRewards().isEmpty())
				{
					for (final int i : task.getRewards().keySet())
					{
						player.addItem("Task Reward", i, task.getRewards().get(i), null, true);
					}
				}
				playerTask.setIsRewarded(true);
				player.updateDailyRewardStatus(playerTask);
			}
			showInfo(player, Integer.parseInt(page));
			return true;
		}
		else if (command.startsWith("cancelTask"))
		{
			String id = null;
			if (param != null && param.length > 0)
			{
				id = param[0];
			}
			
			String page = "1";
			if (param != null && param.length > 1)
			{
				page = param[1];
			}
			
			final PlayerTaskTemplate task = player.getDailyTaskTemplate(Integer.parseInt(id));
			if (task != null)
			{
				player.removeDailyTask(task.getId());
			}
			showInfo(player, Integer.parseInt(page));
			return true;
		}
		return false;
	}

	public static void showInfo(final Player player, int page)
	{
		final int perpage = 5;
		int counter = 0;
		
		final List<PlayerTaskTemplate> activeList = new ArrayList<>();
		for (final PlayerTaskTemplate playerTask : player.getActiveDailyTasks())
		{
			if (playerTask != null)
			{
				activeList.add(playerTask);
			}
		}
		final int totalSize = activeList.size();
		final boolean isThereNextPage = totalSize > perpage;
		
		if (totalSize < perpage && page > 1)
		{
			page = 1;
		}
		
		String html = null;
		
		if (player.getActiveDailyTasks() != null)
		{
			html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/dailyTasks/activeTask.htm");

			String list = "";
			String block = "";
			
			for (int i = (page - 1) * perpage; i < totalSize; i++)
			{
				final PlayerTaskTemplate playerTask = activeList.get(i);
				if (playerTask == null)
				{
					continue;
				}
				
				if (playerTask.getSort().equalsIgnoreCase("daily"))
				{
					final DailyTaskTemplate task = DailyTaskManager.getInstance().getDailyTask(playerTask.getId());
					
					String template = null;
					switch (task.getType())
					{
						case "Farm" :
							template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/dailyTasks/activeTask-farm.htm");
							block = template;
							block = block.replace("%count%", String.valueOf(task.getNpcCount()));
							block = block.replace("%curCount%", String.valueOf(playerTask.getCurrentNpcCount()));
							block = block.replace("%progress%", "" + HtmlUtil.getEternityGauge(380, playerTask.getCurrentNpcCount(), task.getNpcCount(), true) + "");
							break;
						case "Quest" :
							template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/dailyTasks/activeTask-quest.htm");
							block = template;
							
							if (playerTask.isComplete())
							{
								block = block.replace("%quest%", "Completed");
							}
							else
							{
								block = block.replace("%quest%", "In progress");
							}
							block = block.replace("%progress%", "" + HtmlUtil.getEternityGauge(380, playerTask.isComplete() ? 1 : 0, 1, true) + "");
							break;
						case "Reflection" :
							template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/dailyTasks/activeTask-reflection.htm");
							block = template;
							if (playerTask.isComplete())
							{
								block = block.replace("%reflection%", "Completed");
							}
							else
							{
								block = block.replace("%reflection%", "In progress");
							}
							block = block.replace("%progress%", "" + HtmlUtil.getEternityGauge(380, playerTask.isComplete() ? 1 : 0, 1, true) + "");
							break;
						case "Pvp" :
							template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/dailyTasks/activeTask-pvp.htm");
							block = template;
							block = block.replace("%pvpCount%", String.valueOf(task.getPvpCount()));
							block = block.replace("%curPvpCount%", String.valueOf(playerTask.getCurrentPvpCount()));
							block = block.replace("%progress%", "" + HtmlUtil.getEternityGauge(380, playerTask.getCurrentPvpCount(), task.getPvpCount(), true) + "");
							break;
						case "Pk" :
							template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/dailyTasks/activeTask-pk.htm");
							block = template;
							block = block.replace("%pkCount%", String.valueOf(task.getPkCount()));
							block = block.replace("%curPkCount%", String.valueOf(playerTask.getCurrentPkCount()));
							block = block.replace("%progress%", "" + HtmlUtil.getEternityGauge(380, playerTask.getCurrentPkCount(), task.getPkCount(), true) + "");
							break;
						case "Olympiad" :
							template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/dailyTasks/activeTask-olympiad.htm");
							block = template;
							block = block.replace("%olyCount%", String.valueOf(task.getOlyMatchCount()));
							block = block.replace("%curOlyCount%", String.valueOf(playerTask.getCurrentOlyMatchCount()));
							block = block.replace("%progress%", "" + HtmlUtil.getEternityGauge(380, playerTask.getCurrentOlyMatchCount(), task.getOlyMatchCount(), true) + "");
							break;
						case "Event" :
							template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/dailyTasks/activeTask-event.htm");
							block = template;
							block = block.replace("%eventsCount%", String.valueOf(task.getEventsCount()));
							block = block.replace("%curEventsCount%", String.valueOf(playerTask.getCurrentEventsCount()));
							block = block.replace("%progress%", "" + HtmlUtil.getEternityGauge(380, playerTask.getCurrentEventsCount(), task.getEventsCount(), true) + "");
							break;
						case "Siege" :
							template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/dailyTasks/activeTask-siege.htm");
							block = template;
							if (playerTask.isComplete())
							{
								block = block.replace("%siege%", "Completed");
							}
							else
							{
								block = block.replace("%siege%", "In progress");
							}
							block = block.replace("%progress%", "" + HtmlUtil.getEternityGauge(380, playerTask.isComplete() ? 1 : 0, 1, true) + "");
							break;
					}
					block = block.replace("%name%", String.valueOf(task.getName()));
					block = block.replace("%image%", String.valueOf(task.getImage() != "" ? getTaskIcon(task.getImage()) : ""));
					block = block.replace("%type%", String.valueOf(task.getSort()));
					block = block.replace("%taskDescr%", String.valueOf(task.getDescr()));
					
					if (!playerTask.isComplete())
					{
						if (!task.getRewards().isEmpty())
						{
							for (final int itemId : task.getRewards().keySet())
							{
								block = block.replace("%rewardIcon%", "<img src=\"" + ItemsParser.getInstance().getTemplate(itemId).getIcon() + "\" width=32 height=32 />");
								break;
							}
						}
						else
						{
							block = block.replace("%rewardIcon%", "");
						}
					}
					else if (playerTask.isComplete() && !playerTask.isRewarded())
					{
						block = block.replace("%rewardIcon%", "<button action=\"bypass -h .rewardTask " + task.getId() + " " + page + "\" value=\"Get Reward\" width=80 height=26 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
					}
					else
					{
						block = block.replace("%rewardIcon%", "Complete");
					}
					list += block;
				}
				else if (playerTask != null && playerTask.getSort().equalsIgnoreCase("weekly"))
				{
					final DailyTaskTemplate task = DailyTaskManager.getInstance().getDailyTask(playerTask.getId());
					
					String template = null;
					switch (task.getType())
					{
						case "Farm" :
							template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/dailyTasks/activeTask-farm.htm");
							block = template;
							block = block.replace("%count%", String.valueOf(task.getNpcCount()));
							block = block.replace("%curCount%", String.valueOf(playerTask.getCurrentNpcCount()));
							block = block.replace("%progress%", "" + HtmlUtil.getEternityGauge(380, playerTask.getCurrentNpcCount(), task.getNpcCount(), true) + "");
							break;
						case "Quest" :
							template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/dailyTasks/activeTask-quest.htm");
							block = template;
							
							if (playerTask.isComplete())
							{
								block = block.replace("%quest%", "Completed");
							}
							else
							{
								block = block.replace("%quest%", "In progress");
							}
							block = block.replace("%progress%", "" + HtmlUtil.getEternityGauge(380, playerTask.isComplete() ? 1 : 0, 1, true) + "");
							break;
						case "Reflection" :
							template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/dailyTasks/activeTask-reflection.htm");
							block = template;
							if (playerTask.isComplete())
							{
								block = block.replace("%reflection%", "Completed");
							}
							else
							{
								block = block.replace("%reflection%", "In progress");
							}
							block = block.replace("%progress%", "" + HtmlUtil.getEternityGauge(380, playerTask.isComplete() ? 1 : 0, 1, true) + "");
							break;
						case "Pvp" :
							template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/dailyTasks/activeTask-pvp.htm");
							block = template;
							block = block.replace("%pvpCount%", String.valueOf(task.getPvpCount()));
							block = block.replace("%curPvpCount%", String.valueOf(playerTask.getCurrentPvpCount()));
							block = block.replace("%progress%", "" + HtmlUtil.getEternityGauge(380, playerTask.getCurrentPvpCount(), task.getPvpCount(), true) + "");
							break;
						case "Pk" :
							template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/dailyTasks/activeTask-pk.htm");
							block = template;
							block = block.replace("%pkCount%", String.valueOf(task.getPkCount()));
							block = block.replace("%curPkCount%", String.valueOf(playerTask.getCurrentPkCount()));
							block = block.replace("%progress%", "" + HtmlUtil.getEternityGauge(380, playerTask.getCurrentPkCount(), task.getPkCount(), true) + "");
							break;
						case "Olympiad" :
							template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/dailyTasks/activeTask-olympiad.htm");
							block = template;
							block = block.replace("%olyCount%", String.valueOf(task.getOlyMatchCount()));
							block = block.replace("%curOlyCount%", String.valueOf(playerTask.getCurrentOlyMatchCount()));
							block = block.replace("%progress%", "" + HtmlUtil.getEternityGauge(380, playerTask.getCurrentOlyMatchCount(), task.getOlyMatchCount(), true) + "");
							break;
						case "Event" :
							template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/dailyTasks/activeTask-event.htm");
							block = template;
							block = block.replace("%eventsCount%", String.valueOf(task.getEventsCount()));
							block = block.replace("%curEventsCount%", String.valueOf(playerTask.getCurrentEventsCount()));
							block = block.replace("%progress%", "" + HtmlUtil.getEternityGauge(380, playerTask.getCurrentEventsCount(), task.getEventsCount(), true) + "");
							break;
						case "Siege" :
							template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/dailyTasks/activeTask-siege.htm");
							block = template;
							if (playerTask.isComplete())
							{
								block = block.replace("%siege%", "Completed");
							}
							else
							{
								block = block.replace("%siege%", "In progress");
							}
							block = block.replace("%progress%", "" + HtmlUtil.getEternityGauge(380, playerTask.isComplete() ? 1 : 0, 1, true) + "");
							break;
					}
					
					block = block.replace("%name%", String.valueOf(task.getName()));
					block = block.replace("%image%", String.valueOf(task.getImage() != "" ? getTaskIcon(task.getImage()) : ""));
					block = block.replace("%type%", String.valueOf(task.getSort()));
					block = block.replace("%taskDescr%", String.valueOf(task.getDescr()));
					
					if (!playerTask.isComplete())
					{
						if (!task.getRewards().isEmpty())
						{
							for (final int itemId : task.getRewards().keySet())
							{
								block = block.replace("%rewardIcon%", "<img src=\"" + ItemsParser.getInstance().getTemplate(itemId).getIcon() + "\" width=32 height=32 />");
								break;
							}
						}
						else
						{
							block = block.replace("%rewardIcon%", "");
						}
					}
					else if (playerTask.isComplete() && !playerTask.isRewarded())
					{
						block = block.replace("%rewardIcon%", "<button action=\"bypass -h .rewardTask " + task.getId() + " " + page + "\" value=\"Get Reward\" width=80 height=26 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
					}
					else
					{
						block = block.replace("%rewardIcon%", "Complete");
					}
					list += block;
				}
				else if (playerTask != null && playerTask.getSort().equalsIgnoreCase("month"))
				{
					final DailyTaskTemplate task = DailyTaskManager.getInstance().getDailyTask(playerTask.getId());

					String template = null;
					switch (task.getType())
					{
						case "Farm":
							template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/dailyTasks/activeTask-farm.htm");
							block = template;
							block = block.replace("%count%", String.valueOf(task.getNpcCount()));
							block = block.replace("%curCount%", String.valueOf(playerTask.getCurrentNpcCount()));
							block = block.replace("%progress%", "" + HtmlUtil.getEternityGauge(380, playerTask.getCurrentNpcCount(), task.getNpcCount(), true) + "");
							break;
						case "Quest":
							template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/dailyTasks/activeTask-quest.htm");
							block = template;

							if (playerTask.isComplete())
							{
								block = block.replace("%quest%", "Completed");
							}
							else
							{
								block = block.replace("%quest%", "In progress");
							}
							block = block.replace("%progress%", "" + HtmlUtil.getEternityGauge(380, playerTask.isComplete() ? 1 : 0, 1, true) + "");
							break;
						case "Reflection":
							template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/dailyTasks/activeTask-reflection.htm");
							block = template;
							if (playerTask.isComplete())
							{
								block = block.replace("%reflection%", "Completed");
							}
							else
							{
								block = block.replace("%reflection%", "In progress");
							}
							block = block.replace("%progress%", "" + HtmlUtil.getEternityGauge(380, playerTask.isComplete() ? 1 : 0, 1, true) + "");
							break;
						case "Pvp":
							template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/dailyTasks/activeTask-pvp.htm");
							block = template;
							block = block.replace("%pvpCount%", String.valueOf(task.getPvpCount()));
							block = block.replace("%curPvpCount%", String.valueOf(playerTask.getCurrentPvpCount()));
							block = block.replace("%progress%", "" + HtmlUtil.getEternityGauge(380, playerTask.getCurrentPvpCount(), task.getPvpCount(), true) + "");
							break;
						case "Pk":
							template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/dailyTasks/activeTask-pk.htm");
							block = template;
							block = block.replace("%pkCount%", String.valueOf(task.getPkCount()));
							block = block.replace("%curPkCount%", String.valueOf(playerTask.getCurrentPkCount()));
							block = block.replace("%progress%", "" + HtmlUtil.getEternityGauge(380, playerTask.getCurrentPkCount(), task.getPkCount(), true) + "");
							break;
						case "Olympiad":
							template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/dailyTasks/activeTask-olympiad.htm");
							block = template;
							block = block.replace("%olyCount%", String.valueOf(task.getOlyMatchCount()));
							block = block.replace("%curOlyCount%", String.valueOf(playerTask.getCurrentOlyMatchCount()));
							block = block.replace("%progress%", "" + HtmlUtil.getEternityGauge(380, playerTask.getCurrentOlyMatchCount(), task.getOlyMatchCount(), true) + "");
							break;
						case "Event":
							template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/dailyTasks/activeTask-event.htm");
							block = template;
							block = block.replace("%eventsCount%", String.valueOf(task.getEventsCount()));
							block = block.replace("%curEventsCount%", String.valueOf(playerTask.getCurrentEventsCount()));
							block = block.replace("%progress%", "" + HtmlUtil.getEternityGauge(380, playerTask.getCurrentEventsCount(), task.getEventsCount(), true) + "");
							break;
						case "Siege":
							template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/dailyTasks/activeTask-siege.htm");
							block = template;
							if (playerTask.isComplete())
							{
								block = block.replace("%siege%", "Completed");
							}
							else
							{
								block = block.replace("%siege%", "In progress");
							}
							block = block.replace("%progress%", "" + HtmlUtil.getEternityGauge(380, playerTask.isComplete() ? 1 : 0, 1, true) + "");
							break;
					}

					block = block.replace("%name%", String.valueOf(task.getName()));
					block = block.replace("%image%", String.valueOf(task.getImage() != "" ? getTaskIcon(task.getImage()) : ""));
					block = block.replace("%type%", String.valueOf(task.getSort()));
					block = block.replace("%taskDescr%", String.valueOf(task.getDescr()));
					
					if (!playerTask.isComplete())
					{
						if (!task.getRewards().isEmpty())
						{
							for (final int itemId : task.getRewards().keySet())
							{
								block = block.replace("%rewardIcon%", "<img src=\"" + ItemsParser.getInstance().getTemplate(itemId).getIcon() + "\" width=32 height=32 />");
								break;
							}
						}
						else
						{
							block = block.replace("%rewardIcon%", "");
						}
					}
					else if (playerTask.isComplete() && !playerTask.isRewarded())
					{
						block = block.replace("%rewardIcon%", "<button action=\"bypass -h .rewardTask " + task.getId() + " " + page + "\" value=\"Get Reward\" width=80 height=26 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
					}
					else
					{
						block = block.replace("%rewardIcon%", "Complete");
					}
					list += block;
				}
				
				counter++;
				if (counter >= perpage)
				{
					break;
				}
			}
			
			html = html.replace("{dailyAmount}", String.valueOf(player.getLastDailyTasks()));
			html = html.replace("{weeklyAmount}", String.valueOf(player.getLastWeeklyTasks()));
			html = html.replace("{monthAmount}", String.valueOf(player.getLastMonthTasks()));

			if (player.getActiveTasks("daily") < DailyTaskManager.getInstance().getTaskPerDay() && player.getLastDailyTasks() > 0)
			{
				html = html.replace("{addDailybutton}", "<button action=\"bypass -h .setDailyTask " + page + "\" value=\"Take Daily Task\" width=120 height=30 back=\"L2UI_CT1.ListCTRL_DF_Title_Down\" fore=\"L2UI_CT1.ListCTRL_DF_Title\">");
			}
			else
			{
				html = html.replace("{addDailybutton}", "No available tasks...");
			}
			
			if (player.getActiveTasks("weekly") < DailyTaskManager.getInstance().getTaskPerWeek() && player.getLastWeeklyTasks() > 0)
			{
				html = html.replace("{addWeeklybutton}", "<button action=\"bypass -h .setWeeklyTask " + page + "\" value=\"Take Weekly Task\" width=120 height=30 back=\"L2UI_CT1.ListCTRL_DF_Title_Down\" fore=\"L2UI_CT1.ListCTRL_DF_Title\">");
			}
			else
			{
				html = html.replace("{addWeeklybutton}", "No available tasks...");
			}
			
			if (player.getActiveTasks("month") < DailyTaskManager.getInstance().getTaskPerMonth() && player.getLastMonthTasks() > 0)
			{
				html = html.replace("{addMonthbutton}", "<button action=\"bypass -h .setMonthTask " + page + "\" value=\"Take Month Task\" width=120 height=30 back=\"L2UI_CT1.ListCTRL_DF_Title_Down\" fore=\"L2UI_CT1.ListCTRL_DF_Title\">");
			}
			else
			{
				html = html.replace("{addMonthbutton}", "No available tasks...");
			}

			if (list.isEmpty())
			{
				list = "No Active tasks...";
			}
			final int count = (int) Math.ceil((double) totalSize / perpage);
			html = html.replace("{navigation}", Util.getNavigationBlock(count, page, totalSize, perpage, isThereNextPage, ".missions %s"));
			html = html.replace("{list}", list);
		}
		AbstractCommunity.separateAndSend(html, player);
	}

	private DailyTaskTemplate rndTaskSelect(final Player player, String sort)
	{
		boolean cantAdd = false;
		final DailyTaskTemplate rndTask = DailyTaskManager.getInstance().getDailyTasks().get(Rnd.get(DailyTaskManager.getInstance().size()));
		
		for (final PlayerTaskTemplate task : player.getActiveDailyTasks())
		{
			if (task.getId() == rndTask.getId())
			{
				cantAdd = true;
			}
		}
		
		if (!rndTask.getSort().equalsIgnoreCase(sort))
		{
			cantAdd = true;
		}

		if (rndTask.getType().equalsIgnoreCase("Olympiad"))
		{
			if (!player.isNoble() || !Olympiad.getInstance().inCompPeriod())
			{
				cantAdd = true;
			}
		}

		if (rndTask.getType().equalsIgnoreCase("Quest"))
		{
			final Quest quest = QuestManager.getInstance().getQuest(rndTask.getQuestId());
			if (quest != null)
			{
				final QuestState qs = player.getQuestState(quest.getName());
				if ((qs != null) && qs.isCompleted())
				{
					cantAdd = true;
				}
			}
		}

		if (rndTask.getType().equalsIgnoreCase("Siege"))
		{
			if (player.getClan() == null)
			{
				cantAdd = true;
			}
			else if (player.getClan() != null)
			{
				if ((rndTask.getSiegeCastle() && (player.getClan().getCastleId() > 0)) || (rndTask.getSiegeFort() && (player.getClan().getFortId() > 0)))
				{
					cantAdd = true;
				}
			}
		}

		if (cantAdd)
		{
			return rndTaskSelect(player, sort);
		}
		return rndTask;
	}

	private static CharSequence getTaskIcon(final String image)
	{
		return "<img src=\"" + image + "\" width=32 height=32>";
	}

	private static void sendErrorMessageToPlayer(final Player player, final String msg)
	{
		player.sendPacket(new CreatureSay(player.getObjectId(), Say2.CRITICAL_ANNOUNCE, "Error", msg));
	}

	@Override
	public String[] getVoicedCommandList()
	{
		return _commandList;
	}

	public static void main(String[] args)
	{
		if(VoicedCommandHandler.getInstance().getHandler("missions") == null)
			VoicedCommandHandler.getInstance().registerHandler(new DailyTask());
	}
}