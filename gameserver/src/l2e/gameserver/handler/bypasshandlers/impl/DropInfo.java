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

import java.io.Serializable;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

import l2e.commons.apache.StringUtils;
import l2e.commons.util.Util;
import l2e.gameserver.data.htm.HtmCache;
import l2e.gameserver.data.parser.ExperienceParser;
import l2e.gameserver.data.parser.NpcsParser;
import l2e.gameserver.data.parser.SpawnParser;
import l2e.gameserver.handler.bypasshandlers.IBypassHandler;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.actor.templates.npc.champion.ChampionTemplate;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestEventType;
import l2e.gameserver.model.reward.CalculateRewardChances;
import l2e.gameserver.model.reward.CalculateRewardChances.DropInfoTemplate;
import l2e.gameserver.model.reward.CalculateRewardChances.GroupInfoTemplate;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.strings.server.ServerStorage;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.CreatureSay;
import l2e.gameserver.network.serverpackets.RadarControl;

public class DropInfo implements IBypassHandler
{
	private static final NumberFormat pf = NumberFormat.getPercentInstance(Locale.ENGLISH);
	static
	{
		pf.setMaximumFractionDigits(4);
	}
	
	private static final String[] COMMANDS =
	{
	        "drop", "group", "spoil", "info", "list", "quests", "location", "skills"
	};

	@Override
	public boolean useBypass(String command, Player activeChar, Creature target)
	{
		try
		{
			final StringTokenizer st = new StringTokenizer(command, " ");
			final String actualCommand = st.nextToken();
			switch (actualCommand.toLowerCase())
			{
				case "info" :
				{
					try
					{
						final GameObject targetmob = activeChar.getTarget();
						final Npc npc = (Npc) targetmob;
						if (npc != null)
						{
							npc.onActionShift(activeChar);
						}
					}
					catch (final Exception e)
					{
						activeChar.sendMessage("Something went wrong with the info preview.");
					}
					break;
				}
				case "location" :
				{
					try
					{
						String npcId = null;
						try
						{
							npcId = st.nextToken();
						}
						catch (final Exception e)
						{
						}
						
						if (npcId == null || Integer.parseInt(npcId) <= 0)
						{
							activeChar.getRadar().removeAllMarkers();
							return false;
						}
						
						final Npc npc = GameObjectsStorage.getByNpcId(Integer.parseInt(npcId));
						if (npc != null)
						{
							final List<Location> locs = SpawnParser.getInstance().getRandomSpawnsByNpc(Integer.parseInt(npcId));
							if (locs == null || locs.isEmpty())
							{
								return false;
							}
							
							activeChar.sendPacket(new RadarControl(2, 2, 0, 0, 0));
							activeChar.sendPacket(new CreatureSay(activeChar.getObjectId(), Say2.PARTYROOM_COMMANDER, "", "" + ServerStorage.getInstance().getString(activeChar.getLang(), "CommunityNpcCalc.LOCATION") + ""));
							
							for (final Location loc : locs)
							{
								activeChar.sendPacket(new RadarControl(0, 1, loc.getX(), loc.getY(), loc.getZ()));
							}
							activeChar.getRadar().addMarker(locs.get(0).getX(), locs.get(0).getY(), locs.get(0).getZ());
						}
					}
					catch (final Exception e)
					{
					}
					break;
				}
				case "skills" :
				{
					try
					{
						NpcTemplate tpl = null;
						String npcId = null;
						Npc npc = null;
						final int page = Integer.parseInt(st.nextToken());
						try
						{
							npcId = st.nextToken();
						}
						catch (final Exception e)
						{
						}
						
						if (npcId != null)
						{
							tpl = NpcsParser.getInstance().getTemplate(Integer.parseInt(npcId));
						}
						else
						{
							final GameObject targetmob = activeChar.getTarget();
							npc = (Npc) targetmob;
							tpl = npc.getTemplate();
						}
						
						String html = HtmCache.getInstance().getHtm(activeChar, activeChar.getLang(), npcId != null ? "data/html/skilllist_info_id.htm" : "data/html/skilllist_info.htm");
						
						final var skillList = tpl.getSkills().values().stream().toList();
						if (skillList.isEmpty())
						{
							html = html.replace("%npc_name%", tpl.getName(activeChar.getLang()));
							html = html.replace("{list}", "<tr><td align=center>" + ServerStorage.getInstance().getString(activeChar.getLang(), "Info.EMPTY_LIST") + "</td></tr>");
							html = html.replace("{navigation}", "<td>&nbsp;</td>");
							Util.setHtml(html, activeChar);
							return false;
						}
						
						final String template = HtmCache.getInstance().getHtm(activeChar, activeChar.getLang(), "data/html/skilllist_info_template.htm");
						String block = "";
						String list = "";
						
						final Comparator<Skill> statsComparator = new SortSkillInfo();
						Collections.sort(skillList, statsComparator);
						
						final int perpage = 6;
						int counter = 0;
						final int totalSize = skillList.size();
						final boolean isThereNextPage = totalSize > perpage;
						
						for (int i = (page - 1) * perpage; i < totalSize; i++)
						{
							final Skill data = skillList.get(i);
							if (data != null)
							{
								block = template;
								
								block = block.replace("{name}", data.getName(activeChar.getLang()));
								block = block.replace("{icon}", data.getIcon());
								block = block.replace("{level}", String.valueOf(data.getLevel()));
								block = block.replace("{id}", String.valueOf(data.getId()));
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
						if (npcId != null)
						{
							html = html.replace("{navigation}", Util.getNavigationBlock(count, page, totalSize, perpage, isThereNextPage, "skills %s " + Integer.parseInt(npcId) + ""));
						}
						else
						{
							html = html.replace("{navigation}", Util.getNavigationBlock(count, page, totalSize, perpage, isThereNextPage, "skills %s"));
						}
						html = html.replace("%npc_name%", tpl.getName(activeChar.getLang()));
						Util.setHtml(html, activeChar);
					}
					catch (final Exception e)
					{
						activeChar.sendMessage("Something went wrong with the skills preview.");
					}
					break;
				}
				case "quests" :
				{
					try
					{
						String npcId = null;
						Npc npc = null;
						final int page = Integer.parseInt(st.nextToken());
						try
						{
							npcId = st.nextToken();
						}
						catch (final Exception e)
						{
						}
						
						final GameObject targetmob = activeChar.getTarget();
						npc = (Npc) targetmob;
						if (npc == null && npcId != null)
						{
							npc = GameObjectsStorage.getByNpcId(Integer.parseInt(npcId));
						}
						
						if (npc != null)
						{
							String html = HtmCache.getInstance().getHtm(activeChar, activeChar.getLang(), "data/html/quest_info.htm");
							final var list = npc.getTemplate().getEventQuests().entrySet();
							if (list == null || list.isEmpty())
							{
								html = html.replace("%npc_name%", npc.getName(activeChar.getLang()));
								html = html.replace("{navigation}", "<td>&nbsp;</td>");
								html = html.replace("{list}", "<tr><td align=center>" + ServerStorage.getInstance().getString(activeChar.getLang(), "Info.EMPTY_LIST") + "</td></tr>");
								Util.setHtml(html, activeChar);
								return false;
							}
							
							final String template = HtmCache.getInstance().getHtm(activeChar, activeChar.getLang(), "data/html/quest_info_template.htm");
							
							String block = "";
							String info = "";
							
							final List<Quest> questList = new ArrayList<>();
							for (final var quests : list)
							{
								for (final var quest : quests.getValue())
								{
									if (quest != null && quest.getId() > 0 && !questList.contains(quest))
									{
										questList.add(quest);
									}
								}
							}
							
							final Comparator<Quest> statsComparator = new SortQuestInfo();
							Collections.sort(questList, statsComparator);
							
							if (questList.isEmpty())
							{
								html = html.replace("%npc_name%", npc.getName(activeChar.getLang()));
								html = html.replace("{navigation}", "<td>&nbsp;</td>");
								html = html.replace("{list}", "<tr><td align=center>" + ServerStorage.getInstance().getString(activeChar.getLang(), "Info.EMPTY_LIST") + "</td></tr>");
								Util.setHtml(html, activeChar);
								return false;
							}
							
							final int perpage = 8;
							int counter = 0;
							final int totalSize = questList.size();
							final boolean isThereNextPage = totalSize > perpage;
							
							for (int i = (page - 1) * perpage; i < totalSize; i++)
							{
								final var quest = questList.get(i);
								if (quest != null)
								{
									block = template;
									block = block.replace("{number}", String.valueOf(i + 1));
									block = block.replace("{id}", String.valueOf(quest.getId()));
									block = block.replace("{name}", quest.getDescr(activeChar));
									final var startNpc = quest.getNpcsType(QuestEventType.QUEST_START);
									if (startNpc != null)
									{
										final int start = startNpc[0];
										block = block.replace("{start}", String.valueOf(start));
									}
									else
									{
										block = block.replace("{start}", String.valueOf(0));
									}
									info += block;
									
									counter++;
									if (counter >= perpage)
									{
										break;
									}
								}
							}
							final double pages = (double) totalSize / perpage;
							final int count = (int) Math.ceil(pages);
							html = html.replace("{list}", info);
							if (npcId != null)
							{
								html = html.replace("{navigation}", Util.getNavigationBlock(count, page, totalSize, perpage, isThereNextPage, "quests %s " + Integer.parseInt(npcId) + ""));
							}
							else
							{
								html = html.replace("{navigation}", Util.getNavigationBlock(count, page, totalSize, perpage, isThereNextPage, "quests %s"));
							}
							html = html.replace("%npc_name%", npc.getName(activeChar.getLang()));
							Util.setHtml(html, activeChar);
							questList.clear();
						}
					}
					catch (final Exception e)
					{
						activeChar.sendMessage("Something went wrong with the quest preview.");
					}
					break;
				}
				case "drop" :
				{
					try
					{
						NpcTemplate tpl = null;
						String npcId = null;
						ChampionTemplate championTemplate = null;
						Npc npc = null;
						final int page = Integer.parseInt(st.nextToken());
						try
						{
							npcId = st.nextToken();
						}
						catch (final Exception e)
						{}
						
						if (npcId != null)
						{
							tpl = NpcsParser.getInstance().getTemplate(Integer.parseInt(npcId));
						}
						else
						{
							final GameObject targetmob = activeChar.getTarget();
							npc = (Npc) targetmob;
							tpl = npc.getTemplate();
							championTemplate = npc.getChampionTemplate();
						}
						
						String html = HtmCache.getInstance().getHtm(activeChar, activeChar.getLang(), npcId != null ? "data/html/rewardlist_info_id.htm" : "data/html/rewardlist_info.htm");
						if (tpl.getRewards().isEmpty())
						{
							html = html.replace("%npc_name%", tpl.getName(activeChar.getLang()));
							html = html.replace("{list}", "<tr><td align=center>" + ServerStorage.getInstance().getString(activeChar.getLang(), "Info.EMPTY_LIST") + "</td></tr>");
							html = html.replace("{navigation}", "<td>&nbsp;</td>");
							Util.setHtml(html, activeChar);
							return false;
						}
						
						final String template = HtmCache.getInstance().getHtm(activeChar, activeChar.getLang(), "data/html/rewardlist_info_template.htm");
						String block = "";
						String list = "";
						
						final double penaltyMod = npc != null ? ExperienceParser.getInstance().penaltyModifier(npc.calculateLevelDiffForDrop(activeChar.getLevel()), 9) : 1;
						final List<DropInfoTemplate> allItems = CalculateRewardChances.getAmountAndChance(activeChar, tpl, penaltyMod, true, championTemplate);
						if (allItems.isEmpty())
						{
							html = html.replace("%npc_name%", tpl.getName(activeChar.getLang()));
							html = html.replace("{list}", "<tr><td align=center>" + ServerStorage.getInstance().getString(activeChar.getLang(), "Info.EMPTY_LIST") + "</td></tr>");
							html = html.replace("{navigation}", "<td>&nbsp;</td>");
							Util.setHtml(html, activeChar);
							return false;
						}
						
						final Comparator<DropInfoTemplate> statsComparator = new SortDropInfo();
						Collections.sort(allItems, statsComparator);
						
						final int perpage = 6;
						int counter = 0;
						final int totalSize = allItems.size();
						final boolean isThereNextPage = totalSize > perpage;
						
						for (int i = (page - 1) * perpage; i < totalSize; i++)
						{
							final DropInfoTemplate data = allItems.get(i);
							if (data != null)
							{
								block = template;
								
								String icon = data._item.getItem().getIcon();
								if (icon == null || icon.equals(StringUtils.EMPTY))
								{
									icon = "icon.etc_question_mark_i00";
								}
								String name = data._item.getItem().getName(activeChar.getLang());
								if (name.length() > 32)
								{
									name = name.substring(0, 32) + ".";
								}
								block = block.replace("{name}", name);
								block = block.replace("{icon}", "<table border=0 cellspacing=0 cellpadding=0 width=32 height=32 background=\"" + icon + "\"><tr><td width=32 align=center valign=top><img src=\"L2UI.PETITEM_CLICK\" width=32 height=32></td></tr></table>");
								block = block.replace("{count}", data._maxCount > data._minCount ? "" + data._minCount + " - " + data._maxCount + "" : String.valueOf(data._minCount));
								block = block.replace("{chance}", pf.format(data._chance));
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
						if (npcId != null)
						{
							html = html.replace("{navigation}", Util.getNavigationBlock(count, page, totalSize, perpage, isThereNextPage, "drop %s " + Integer.parseInt(npcId) + ""));
						}
						else
						{
							html = html.replace("{navigation}", Util.getNavigationBlock(count, page, totalSize, perpage, isThereNextPage, "drop %s"));
						}
						html = html.replace("%npc_name%", tpl.getName(activeChar.getLang()));
						allItems.clear();
						Util.setHtml(html, activeChar);
					}
					catch (final Exception e)
					{
						activeChar.sendMessage("Something went wrong with the drop preview.");
					}
					break;
				}
				case "spoil" :
				{
					try
					{
						NpcTemplate tpl = null;
						String npcId = null;
						Npc npc = null;
						ChampionTemplate championTemplate = null;
						final int page = Integer.parseInt(st.nextToken());
						try
						{
							npcId = st.nextToken();
						}
						catch (final Exception e)
						{}
						
						if (npcId != null)
						{
							tpl = NpcsParser.getInstance().getTemplate(Integer.parseInt(npcId));
						}
						else
						{
							final GameObject targetmob = activeChar.getTarget();
							npc = (Npc) targetmob;
							tpl = npc.getTemplate();
							championTemplate = npc.getChampionTemplate();
						}
						
						final double penaltyMod = npc != null ? ExperienceParser.getInstance().penaltyModifier(npc.calculateLevelDiffForDrop(activeChar.getLevel()), 9) : 1;
						
						String html = HtmCache.getInstance().getHtm(activeChar, activeChar.getLang(), npcId != null ? "data/html/rewardlist_info_id.htm" : "data/html/rewardlist_spoil_info.htm");
						final List<DropInfoTemplate> allItems = CalculateRewardChances.getAmountAndChance(activeChar, tpl, penaltyMod, false, championTemplate);
						if (allItems.isEmpty())
						{
							html = html.replace("%npc_name%", tpl.getName(activeChar.getLang()));
							html = html.replace("{list}", "<tr><td align=center>" + ServerStorage.getInstance().getString(activeChar.getLang(), "Info.EMPTY_LIST") + "</td></tr>");
							html = html.replace("{navigation}", "<td>&nbsp;</td>");
							Util.setHtml(html, activeChar);
							return false;
						}
						final Comparator<DropInfoTemplate> statsComparator = new SortDropInfo();
						Collections.sort(allItems, statsComparator);
						
						final String template = HtmCache.getInstance().getHtm(activeChar, activeChar.getLang(), "data/html/rewardlist_info_template.htm");
						String block = "";
						String list = "";
						
						final int perpage = 6;
						int counter = 0;
						final int totalSize = allItems.size();
						final boolean isThereNextPage = totalSize > perpage;
						
						for (int i = (page - 1) * perpage; i < totalSize; i++)
						{
							final DropInfoTemplate data = allItems.get(i);
							if (data != null)
							{
								block = template;
								
								String icon = data._item.getItem().getIcon();
								if (icon == null || icon.equals(StringUtils.EMPTY))
								{
									icon = "icon.etc_question_mark_i00";
								}
								String name = data._item.getItem().getName(activeChar.getLang());
								if (name.length() > 32)
								{
									name = name.substring(0, 32) + ".";
								}
								block = block.replace("{name}", name);
								block = block.replace("{icon}", "<table border=0 cellspacing=0 cellpadding=0 width=32 height=32 background=\"" + icon + "\"><tr><td width=32 align=center valign=top><img src=\"L2UI.PETITEM_CLICK\" width=32 height=32></td></tr></table>");
								block = block.replace("{count}", data._maxCount > data._minCount ? "" + data._minCount + " - " + data._maxCount + "" : String.valueOf(data._minCount));
								block = block.replace("{chance}", pf.format(data._chance));
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
						if (npcId != null)
						{
							html = html.replace("{navigation}", Util.getNavigationBlock(count, page, totalSize, perpage, isThereNextPage, "spoil %s " + Integer.parseInt(npcId) + ""));
						}
						else
						{
							html = html.replace("{navigation}", Util.getNavigationBlock(count, page, totalSize, perpage, isThereNextPage, "spoil %s"));
						}
						html = html.replace("%npc_name%", tpl.getName(activeChar.getLang()));
						allItems.clear();
						Util.setHtml(html, activeChar);
					}
					catch (final Exception e)
					{
						activeChar.sendMessage("Something went wrong with the spoil preview.");
					}
					break;
				}
				case "group" :
				{
					try
					{
						NpcTemplate tpl = null;
						String npcId = null;
						Npc npc = null;
						ChampionTemplate championTemplate = null;
						final int page = Integer.parseInt(st.nextToken());
						try
						{
							npcId = st.nextToken();
						}
						catch (final Exception e)
						{}
						
						if (npcId != null)
						{
							tpl = NpcsParser.getInstance().getTemplate(Integer.parseInt(npcId));
						}
						else
						{
							final GameObject targetmob = activeChar.getTarget();
							npc = (Npc) targetmob;
							tpl = npc.getTemplate();
							championTemplate = npc.getChampionTemplate();
						}
						
						String html = HtmCache.getInstance().getHtm(activeChar, activeChar.getLang(), npcId != null ? "data/html/group_info_id.htm" : "data/html/group_info.htm");
						if (tpl.getRewards().isEmpty())
						{
							html = html.replace("%npc_name%", tpl.getName(activeChar.getLang()));
							html = html.replace("{list}", "<tr><td align=center>" + ServerStorage.getInstance().getString(activeChar.getLang(), "Info.EMPTY_LIST") + "</td></tr>");
							html = html.replace("{navigation}", "<td>&nbsp;</td>");
							Util.setHtml(html, activeChar);
							return false;
						}
						
						final double penaltyMod = npc != null ? ExperienceParser.getInstance().penaltyModifier(npc.calculateLevelDiffForDrop(activeChar.getLevel()), 9) : 1;
						
						final List<GroupInfoTemplate> allItems = CalculateRewardChances.getGroupAmountAndChance(activeChar, tpl, penaltyMod, championTemplate);
						if (allItems.isEmpty())
						{
							html = html.replace("%npc_name%", tpl.getName(activeChar.getLang()));
							html = html.replace("{list}", "<tr><td align=center>" + ServerStorage.getInstance().getString(activeChar.getLang(), "Info.EMPTY_LIST") + "</td></tr>");
							html = html.replace("{navigation}", "<td>&nbsp;</td>");
							Util.setHtml(html, activeChar);
							return false;
						}
						
						final Comparator<GroupInfoTemplate> statsComparator = new SortGroupInfo();
						Collections.sort(allItems, statsComparator);
						
						final String groupHtm = HtmCache.getInstance().getHtm(activeChar, activeChar.getLang(), "data/html/group_template.htm");
						final String groupTableHtm = HtmCache.getInstance().getHtm(activeChar, activeChar.getLang(), "data/html/group_table_template.htm");
						final String template = HtmCache.getInstance().getHtm(activeChar, activeChar.getLang(), "data/html/group_info_template.htm");
						
						final Map<Integer, String> htmlInfo = new HashMap<>();
						final int perpage = 6;
						var htmPage = 1;
						var i = 0;
						var openTable = false;
						var nextGroup = false;
						var totalSize = allItems.size();
						
						String groupList = "";
						String block = "";
						String groupBlock = "";
						String groupBlock1 = null;
						var isNextBlock = false;
						
						for (final GroupInfoTemplate group : allItems)
						{
							if (group != null)
							{
								i++;
								
								if (i == perpage)
								{
									groupList += "</table>";
									if (isNextBlock)
									{
										groupBlock1 = groupBlock1.replace("{list}", groupList);
										groupBlock += groupBlock1;
										isNextBlock = false;
									}
									else
									{
										groupBlock = groupBlock.replace("{list}", groupList);
									}
									openTable = false;
									htmlInfo.put(htmPage, groupBlock);
									htmPage++;
									i = 1;
								}
								
								if (nextGroup)
								{
									if (openTable)
									{
										groupList += "</table>";
										if (isNextBlock)
										{
											groupBlock1 = groupBlock1.replace("{list}", groupList);
											groupBlock += groupBlock1;
										}
										else
										{
											groupBlock = groupBlock.replace("{list}", groupList);
										}
										openTable = true;
										groupList = "";
										block = "";
										groupBlock1 = groupHtm;
										groupBlock1 = groupBlock1.replace("{name}", group._type);
										groupBlock1 = groupBlock1.replace("{chance}", group._type.equalsIgnoreCase("CHAMPION_GROUP") ? "" + (int) (group._groupChance / 10) + "%" : pf.format(group._groupChance));
										groupBlock1 = groupBlock1.replace("{amount}", String.valueOf(group._amount));
										isNextBlock = true;
									}
								}
								
								if (!openTable)
								{
									openTable = true;
									nextGroup = true;
									groupList = "";
									block = "";
									groupBlock = groupHtm;
									groupBlock = groupBlock.replace("{name}", group._type);
									groupBlock = groupBlock.replace("{chance}", group._type.equalsIgnoreCase("CHAMPION_GROUP") ? "" + (int) (group._groupChance / 10) + "%" : pf.format(group._groupChance));
									groupBlock = groupBlock.replace("{amount}", String.valueOf(group._amount));
								}
								
								for (final DropInfoTemplate data : group._list)
								{
									if (data != null)
									{
										totalSize++;
										if (!openTable)
										{
											openTable = true;
											groupList = "";
											block = "";
											groupBlock = groupTableHtm;
										}
										
										block = template;
										
										String icon = data._item.getItem().getIcon();
										if (icon == null || icon.equals(StringUtils.EMPTY))
										{
											icon = "icon.etc_question_mark_i00";
										}
										String name = data._item.getItem().getName(activeChar.getLang());
										if (name.length() > 32)
										{
											name = name.substring(0, 32) + ".";
										}
										block = block.replace("{name}", name);
										block = block.replace("{icon}", "<table border=0 cellspacing=0 cellpadding=0 width=32 height=32 background=\"" + icon + "\"><tr><td width=32 align=center valign=top><img src=\"L2UI.PETITEM_CLICK\" width=32 height=32></td></tr></table>");
										block = block.replace("{count}", data._maxCount > data._minCount ? "" + data._minCount + " - " + data._maxCount + "" : String.valueOf(data._minCount));
										block = block.replace("{chance}", pf.format(data._chance));
										groupList += block;
										
										i++;
										if (i >= perpage)
										{
											groupList += "</table>";
											if (isNextBlock)
											{
												groupBlock1 = groupBlock1.replace("{list}", groupList);
												groupBlock += groupBlock1;
												isNextBlock = false;
											}
											else
											{
												groupBlock = groupBlock.replace("{list}", groupList);
											}
											openTable = false;
											htmlInfo.put(htmPage, groupBlock);
											htmPage++;
											i = 0;
										}
									}
								}
							}
						}
						
						if (openTable)
						{
							groupList += "</table>";
							if (isNextBlock)
							{
								groupBlock1 = groupBlock1.replace("{list}", groupList);
								groupBlock += groupBlock1;
								isNextBlock = false;
							}
							else
							{
								groupBlock = groupBlock.replace("{list}", groupList);
							}
							openTable = false;
							htmlInfo.put(htmPage, groupBlock);
							htmPage++;
							i = 0;
						}
						
						final String info = htmlInfo.get(page);
						if (info != null)
						{
							final var isThereNextPage = page < htmlInfo.size();
							final double pages = (double) totalSize / perpage;
							final int count = (int) Math.ceil(pages);
							html = html.replace("{list}", info);
							if (npcId != null)
							{
								html = html.replace("{navigation}", Util.getNavigationBlock(count, page, totalSize, perpage, isThereNextPage, "group %s " + Integer.parseInt(npcId) + ""));
							}
							else
							{
								html = html.replace("{navigation}", Util.getNavigationBlock(count, page, totalSize, perpage, isThereNextPage, "group %s"));
							}
							html = html.replace("%npc_name%", tpl.getName(activeChar.getLang()));
							allItems.clear();
							htmlInfo.clear();
							Util.setHtml(html, activeChar);
						}
					}
					catch (final Exception e)
					{
						activeChar.sendMessage("Something went wrong with the group preview.");
					}
					break;
				}
				case "list" :
				{
					try
					{
						NpcTemplate tpl = null;
						String npcId = null;
						Npc npc = null;
						ChampionTemplate championTemplate = null;
						try
						{
							npcId = st.nextToken();
						}
						catch (final Exception e)
						{
						}
						
						if (npcId != null)
						{
							tpl = NpcsParser.getInstance().getTemplate(Integer.parseInt(npcId));
						}
						else
						{
							final GameObject targetmob = activeChar.getTarget();
							npc = (Npc) targetmob;
							tpl = npc.getTemplate();
							championTemplate = npc.getChampionTemplate();
						}
						
						String html = HtmCache.getInstance().getHtm(activeChar, activeChar.getLang(), npcId != null ? "data/html/group_info.htm_id" : "data/html/group_info.htm");
						if (tpl.getRewards().isEmpty())
						{
							html = html.replace("%npc_name%", tpl.getName(activeChar.getLang()));
							html = html.replace("{list}", "<tr><td align=center>" + ServerStorage.getInstance().getString(activeChar.getLang(), "Info.EMPTY_LIST") + "</td></tr>");
							html = html.replace("{navigation}", "<td>&nbsp;</td>");
							Util.setHtml(html, activeChar);
							return false;
						}
						
						final double penaltyMod = npc != null ? ExperienceParser.getInstance().penaltyModifier(npc.calculateLevelDiffForDrop(activeChar.getLevel()), 9) : 1;
						
						final List<GroupInfoTemplate> allItems = CalculateRewardChances.getGroupAmountAndChance(activeChar, tpl, penaltyMod, championTemplate);
						if (allItems.isEmpty())
						{
							html = html.replace("%npc_name%", tpl.getName(activeChar.getLang()));
							html = html.replace("{list}", "<tr><td align=center>" + ServerStorage.getInstance().getString(activeChar.getLang(), "Info.EMPTY_LIST") + "</td></tr>");
							html = html.replace("{navigation}", "<td>&nbsp;</td>");
							Util.setHtml(html, activeChar);
							return false;
						}
						
						final Comparator<GroupInfoTemplate> statsComparator = new SortGroupInfo();
						Collections.sort(allItems, statsComparator);
						
						final String groupHtm = HtmCache.getInstance().getHtm(activeChar, activeChar.getLang(), "data/html/group_template.htm");
						final String template = HtmCache.getInstance().getHtm(activeChar, activeChar.getLang(), "data/html/group_info_template.htm");
						
						String htmlInfo = "";
						String groupList = "";
						String block = "";
						String groupBlock = "";
						
						for (final GroupInfoTemplate group : allItems)
						{
							if (group != null)
							{
								groupBlock = groupHtm;
								groupBlock = groupBlock.replace("{name}", group._type);
								groupBlock = groupBlock.replace("{chance}", pf.format(group._groupChance));
								groupBlock = groupBlock.replace("{amount}", String.valueOf(group._amount));
								
								groupList = "";
								for (final DropInfoTemplate data : group._list)
								{
									if (data != null)
									{
										block = template;
										
										String icon = data._item.getItem().getIcon();
										if (icon == null || icon.equals(StringUtils.EMPTY))
										{
											icon = "icon.etc_question_mark_i00";
										}
										String name = data._item.getItem().getName(activeChar.getLang());
										if (name.length() > 32)
										{
											name = name.substring(0, 32) + ".";
										}
										block = block.replace("{name}", name);
										block = block.replace("{icon}", "<table border=0 cellspacing=0 cellpadding=0 width=32 height=32 background=\"" + icon + "\"><tr><td width=32 align=center valign=top><img src=\"L2UI.PETITEM_CLICK\" width=32 height=32></td></tr></table>");
										block = block.replace("{count}", data._maxCount > data._minCount ? "" + data._minCount + " - " + data._maxCount + "" : String.valueOf(data._minCount));
										block = block.replace("{chance}", pf.format(data._chance));
										groupList += block;
										
									}
								}
								groupList += "</table>";
								groupBlock = groupBlock.replace("{list}", groupList);
								htmlInfo += groupBlock;
							}
						}
						
						if (!htmlInfo.isEmpty())
						{
							html = html.replace("{list}", htmlInfo);
							html = html.replace("{navigation}", "<td>&nbsp;</td>");
							html = html.replace("%npc_name%", tpl.getName(activeChar.getLang()));
							allItems.clear();
							Util.setHtml(html, activeChar);
						}
					}
					catch (final Exception e)
					{
						activeChar.sendMessage("Something went wrong with the group preview.");
					}
					break;
				}
			}
		}
		catch (final Exception e)
		{
			activeChar.sendMessage("You cant use this option with this target.");
		}
		return false;
	}
	
	private static class SortSkillInfo implements Comparator<Skill>, Serializable
	{
		private static final long serialVersionUID = 7691414259610932752L;
		
		@Override
		public int compare(Skill o1, Skill o2)
		{
			return Integer.compare(o1.getId(), o2.getId());
		}
	}
	
	private static class SortDropInfo implements Comparator<DropInfoTemplate>, Serializable
	{
		private static final long serialVersionUID = 7691414259610932752L;
		
		@Override
		public int compare(DropInfoTemplate o1, DropInfoTemplate o2)
		{
			return Double.compare(o2._chance, o1._chance);
		}
	}
	
	private static class SortGroupInfo implements Comparator<GroupInfoTemplate>, Serializable
	{
		private static final long serialVersionUID = 7691414259610932752L;
		
		@Override
		public int compare(GroupInfoTemplate o1, GroupInfoTemplate o2)
		{
			return Double.compare(o2._groupChance, o1._groupChance);
		}
	}
	
	private static class SortQuestInfo implements Comparator<Quest>, Serializable
	{
		private static final long serialVersionUID = 7691414259610932752L;
		
		@Override
		public int compare(Quest o1, Quest o2)
		{
			return Integer.compare(o1.getId(), o2.getId());
		}
	}

	@Override
	public String[] getBypassList()
	{
		return COMMANDS;
	}
}