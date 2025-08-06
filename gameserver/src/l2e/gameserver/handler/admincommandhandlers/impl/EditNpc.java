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

import java.io.Serializable;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

import l2e.commons.apache.StringUtils;
import l2e.commons.util.Util;
import l2e.gameserver.data.htm.HtmCache;
import l2e.gameserver.data.parser.ExperienceParser;
import l2e.gameserver.data.parser.NpcsParser;
import l2e.gameserver.handler.admincommandhandlers.IAdminCommandHandler;
import l2e.gameserver.instancemanager.WalkingManager;
import l2e.gameserver.model.Elementals;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.actor.templates.npc.champion.ChampionTemplate;
import l2e.gameserver.model.reward.CalculateRewardChances;
import l2e.gameserver.model.reward.CalculateRewardChances.DropInfoTemplate;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.spawn.SpawnTerritory;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;

public class EditNpc implements IAdminCommandHandler
{
	private static final NumberFormat pf = NumberFormat.getPercentInstance(Locale.ENGLISH);
	static
	{
		pf.setMaximumFractionDigits(4);
	}
	
	private static final String[] ADMIN_COMMANDS =
	{
	        "admin_show_skill_list_npc", "admin_show_drop_list", "admin_show_spoil_list", "admin_log_npc_spawn", "admin_show_npc_info", "admin_show_npc_stats", "admin_visuality_npczone"
	};

	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		final StringTokenizer st = new StringTokenizer(command, " ");
		final String actualCommand = st.nextToken();
		switch (actualCommand.toLowerCase())
		{
			case "admin_show_skill_list_npc" :
			{
				if (st.countTokens() < 1)
				{
					activeChar.sendMessage("Usage: //show_skill_list_npc <npc_id> <page>");
					return false;
				}

				try
				{
					final int npcId = Integer.parseInt(st.nextToken());
					final int page = st.hasMoreTokens() ? Integer.parseInt(st.nextToken()) : 0;
					final NpcTemplate npc = NpcsParser.getInstance().getTemplate(npcId);
					if (npc != null)
					{
						showNpcSkillList(activeChar, npc, page);
					}
					else
					{
						activeChar.sendMessage("NPC does not exist or not loaded. npc_id:" + npcId);
					}
				}
				catch (final NumberFormatException e)
				{
					activeChar.sendMessage("npc_id must be a number.");
				}
				break;
			}
			case "admin_show_npc_info" :
			{
				if (activeChar.getTarget() == null)
				{
					activeChar.sendMessage("Usage: //show_npc_info <npc_id> and target!");
					return false;
				}
				
				try
				{
					final GameObject target = activeChar.getTarget();
					if (target instanceof Npc)
					{
						showNpcInfoList(activeChar, (Npc) target);
					}
					else
					{
						activeChar.sendMessage("NPC does not exist or not loaded.");
					}
				}
				catch (final NumberFormatException e)
				{
					activeChar.sendMessage("npc_id must be a number.");
				}
				break;
			}
			case "admin_show_npc_stats" :
			{
				if (activeChar.getTarget() == null)
				{
					activeChar.sendMessage("Usage: //show_npc_stats <npc_id> and target!");
					return false;
				}
				
				try
				{
					final GameObject target = activeChar.getTarget();
					if (target instanceof Npc)
					{
						showNpcStatList(activeChar, ((Npc) target));
					}
					else
					{
						activeChar.sendMessage("NPC does not exist or not loaded.");
					}
				}
				catch (final NumberFormatException e)
				{
					activeChar.sendMessage("npc_id must be a number.");
				}
				break;
			}
			case "admin_show_drop_list" :
			{
				if (st.countTokens() < 1)
				{
					activeChar.sendMessage("Usage: //show_drop_list <npc_id>!");
					return false;
				}

				try
				{
					NpcTemplate tpl = null;
					String npcId = null;
					String page = null;
					ChampionTemplate championTemplate = null;
					final GameObject target = activeChar.getTarget();
					Npc npc = null;
					try
					{
						npcId = st.nextToken();
					}
					catch (final Exception e)
					{}
					
					try
					{
						page = st.nextToken();
					}
					catch (final Exception e)
					{}
					
					if (npcId != null)
					{
						tpl = NpcsParser.getInstance().getTemplate(Integer.parseInt(npcId));
					}
					
					if (tpl != null)
					{
						if (target != null)
						{
							if (!target.isPlayable() && !target.isPlayer())
							{
								npc = (Npc) target;
								championTemplate = npc.getChampionTemplate();
							}
						}
						showNpcDropList(activeChar, npc, tpl, page != null ? Integer.parseInt(page) : 1, championTemplate);
					}
					else
					{
						activeChar.sendMessage("NPC does not exist or not loaded. npc_id:" + npcId);
					}
				}
				catch (final NumberFormatException e)
				{
					activeChar.sendMessage("npc_id must be a number.");
				}
				break;
			}
			case "admin_show_spoil_list" :
			{
				if (st.countTokens() < 1)
				{
					activeChar.sendMessage("Usage: //show_drop_list <npc_id>!");
					return false;
				}

				try
				{
					NpcTemplate tpl = null;
					String npcId = null;
					String page = null;
					ChampionTemplate championTemplate = null;
					final GameObject target = activeChar.getTarget();
					Npc npc = null;
					try
					{
						npcId = st.nextToken();
					}
					catch (final Exception e)
					{}
					
					try
					{
						page = st.nextToken();
					}
					catch (final Exception e)
					{}
					
					if (npcId != null)
					{
						tpl = NpcsParser.getInstance().getTemplate(Integer.parseInt(npcId));
					}
					
					if (tpl != null)
					{
						if (target != null)
						{
							if (!target.isPlayable() && !target.isPlayer())
							{
								npc = (Npc) target;
								championTemplate = npc.getChampionTemplate();
							}
						}
						showNpcSpoilList(activeChar, npc, tpl, page != null ? Integer.parseInt(page) : 1, championTemplate);
					}
					else
					{
						activeChar.sendMessage("NPC does not exist or not loaded. npc_id:" + npcId);
					}
				}
				catch (final NumberFormatException e)
				{
					activeChar.sendMessage("npc_id must be a number.");
				}
				break;
			}
			case "admin_log_npc_spawn" :
			{
				final GameObject target = activeChar.getTarget();
				if (target instanceof Npc)
				{
					final Npc npc = (Npc) target;
					_log.info("('', 1, " + npc.getId() + ", " + npc.getX() + ", " + npc.getY() + ", " + npc.getZ() + ", 0, 0, " + npc.getHeading() + ", 60, 0, 0),");
				}
				break;
			}
			case "admin_visuality_npczone" :
			{
				final var target = activeChar.getTarget();
				if (target.isNpc() && target.isAttackable())
				{
					final Npc npc = (Npc) target;
					final var spawnRange = npc.getSpawn().calcSpawnRange();
					if (spawnRange == null || !(spawnRange instanceof SpawnTerritory))
					{
						activeChar.sendMessage("Selected NPC dont have spawn territory.");
						return false;
					}
					
					((SpawnTerritory) spawnRange).setVisuality(activeChar);
					activeChar.sendMessage("NPC ID[" + target.getId() + "] success visuality spawn zone!");
				}
			}
		}
		return true;
	}

	private void showNpcSkillList(Player activeChar, NpcTemplate npc, int page)
	{
		final int PAGE_SIZE = 20;

		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile(activeChar, activeChar.getLang(), "data/html/admin/editnpc-skills.htm");

		html.replace("%npcId%", String.valueOf(npc.getId()));
		html.replace("%title_npc_name%", String.valueOf(npc.getName(activeChar.getLang())));
		html.replace("%page%", String.valueOf(page + 1));

		final Map<Integer, Skill> skills = npc.getSkills();

		int pages = skills.size() / PAGE_SIZE;
		if ((PAGE_SIZE * pages) < skills.size())
		{
			pages++;
		}

		if (pages > 1)
		{
			final StringBuilder sb = new StringBuilder();
			sb.append("<table width=280 cellspacing=0><tr>");
			for (int i = 0; i < pages; i++)
			{
				sb.append("<td align=center><button action=\"bypass -h admin_show_skill_list_npc " + npc.getId() + " " + i + "\" value=\"" + (i + 1) + "\" width=30 height=22 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
			}
			sb.append("</tr></table>");
			html.replace("%pages%", sb.toString());
		}
		else
		{
			html.replace("%pages%", "");
		}

		if (page >= pages)
		{
			page = pages - 1;
		}

		int start = 0;
		if (page > 0)
		{
			start = PAGE_SIZE * page;
		}

		int i = 0;
		final StringBuilder sb = new StringBuilder(Math.min(PAGE_SIZE, skills.size()) * 550);
		for (final Skill skill : skills.values())
		{
			if (i < start)
			{
				i++;
				continue;
			}

			sb.append("<table width=280 height=32 cellspacing=1 background=\"L2UI_CT1.Windows.Windows_DF_TooltipBG\">");
			sb.append("<tr><td fixwidth=32 background=\"" + skill.getIcon() + "\"></td>");
			sb.append("<td fixwidth=140>");
			sb.append(skill.getName(activeChar.getLang()));
			sb.append("</td>");
			sb.append("<td fixwidth=45 align=center>");
			sb.append(skill.getId());
			sb.append("</td>");
			sb.append("<td fixwidth=35 align=center>");
			sb.append(skill.getLevel());
			sb.append("</td></tr></table>");

			i++;
			if (i >= (PAGE_SIZE + start))
			{
				break;
			}
		}
		html.replace("%skills%", sb.toString());
		activeChar.sendPacket(html);
	}

	protected void showNpcDropList(Player player, Npc npc, NpcTemplate tpl, int page, ChampionTemplate championTemplate)
	{
		String html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/admin/editnpc-rewardlist_info.htm");
		final String template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/admin/editnpc-rewardlist_template.htm");
		String block = "";
		String list = "";
		
		final double penaltyMod = npc != null ? ExperienceParser.getInstance().penaltyModifier(npc.calculateLevelDiffForDrop(player.getLevel()), 9) : 1;
		final List<DropInfoTemplate> allItems = CalculateRewardChances.getAmountAndChance(player, tpl, penaltyMod, true, championTemplate);
		if (allItems.isEmpty())
		{
			final NpcHtmlMessage htm = new NpcHtmlMessage(0);
			htm.setFile(player, player.getLang(), "data/html/admin/editnpc-rewardlist_empty.htm");
			htm.replace("%npc_name%", tpl.getName(player.getLang()));
			player.sendPacket(htm);
			return;
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
				String name = data._item.getItem().getName(player.getLang());
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
		html = html.replace("{navigation}", Util.getNavigationBlock(count, page, totalSize, perpage, isThereNextPage, "admin_show_drop_list " + tpl.getId() + " %s"));
		html = html.replace("%npc_name%", tpl.getName(player.getLang()));
		allItems.clear();
		Util.setHtml(html, player);
	}
	
	protected void showNpcSpoilList(Player player, Npc npc, NpcTemplate tpl, int page, ChampionTemplate championTemplate)
	{
		final double penaltyMod = npc != null ? ExperienceParser.getInstance().penaltyModifier(npc.calculateLevelDiffForDrop(player.getLevel()), 9) : 1;
		final List<DropInfoTemplate> allItems = CalculateRewardChances.getAmountAndChance(player, tpl, penaltyMod, false, championTemplate);
		if (allItems.isEmpty())
		{
			final NpcHtmlMessage html = new NpcHtmlMessage(0);
			html.setFile(player, player.getLang(), "data/html/admin/editnpc-spoillist_empty.htm");
			html.replace("%npc_name%", tpl.getName(player.getLang()));
			player.sendPacket(html);
			return;
		}
		
		final Comparator<DropInfoTemplate> statsComparator = new SortDropInfo();
		Collections.sort(allItems, statsComparator);
		
		String html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/admin/editnpc-rewardlist_info.htm");
		final String template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/admin/editnpc-rewardlist_template.htm");
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
				String name = data._item.getItem().getName(player.getLang());
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
		html = html.replace("{navigation}", Util.getNavigationBlock(count, page, totalSize, perpage, isThereNextPage, "admin_show_spoil_list " + tpl.getId() + " %s"));
		html = html.replace("%npc_name%", tpl.getName(player.getLang()));
		allItems.clear();
		Util.setHtml(html, player);
	}

	protected void showNpcInfoList(Player player, Npc npc)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile(player, player.getLang(), "data/html/admin/npcinfo.htm");
		
		html.replace("%class%", npc.getClass().getSimpleName());
		html.replace("%id%", String.valueOf(npc.getTemplate().getId()));
		html.replace("%lvl%", String.valueOf(npc.getTemplate().getLevel()));
		html.replace("%name%", String.valueOf(npc.getTemplate().getName(player.getLang())));
		html.replace("%tmplid%", String.valueOf(npc.getTemplate().getId()));
		html.replace("%aggro%", String.valueOf((npc instanceof Attackable) ? ((Attackable) npc).getAggroRange() : 0));
		html.replace("%hp%", String.valueOf((int) npc.getCurrentHp()));
		html.replace("%hpmax%", String.valueOf((int) npc.getMaxHp()));
		html.replace("%mp%", String.valueOf((int) npc.getCurrentMp()));
		html.replace("%mpmax%", String.valueOf((int) npc.getMaxMp()));
		html.replace("%refId%", String.valueOf(npc.getReflectionId()));
		html.replace("%loc%", String.valueOf(npc.getX() + " " + npc.getY() + " " + npc.getZ()));
		html.replace("%heading%", String.valueOf(npc.getHeading()));
		html.replace("%collision_radius%", String.valueOf(npc.getTemplate().getfCollisionRadius()));
		html.replace("%collision_height%", String.valueOf(npc.getTemplate().getfCollisionHeight()));
		html.replace("%dist%", String.valueOf((int) Math.sqrt(player.getDistanceSq(npc))));
		html.replace("%region%", npc.getWorldRegion() != null ? npc.getWorldRegion().getName() : "<font color=FF0000>null</font>");
		if (npc.getSpawnedLoc() != null)
		{
			html.replace("%spawn%", npc.getSpawnedLoc().getX() + " " + npc.getSpawnedLoc().getY() + " " + npc.getSpawnedLoc().getZ());
			html.replace("%loc2d%", String.valueOf((int) Math.sqrt(npc.getPlanDistanceSq(npc.getSpawnedLoc().getX(), npc.getSpawnedLoc().getY()))));
			html.replace("%loc3d%", String.valueOf((int) Math.sqrt(npc.getDistanceSq(npc.getSpawnedLoc().getX(), npc.getSpawnedLoc().getY(), npc.getSpawnedLoc().getZ()))));
		}
		else
		{
			html.replace("%spawn%", "<font color=FF0000>null</font>");
			html.replace("%loc2d%", "<font color=FF0000>--</font>");
			html.replace("%loc3d%", "<font color=FF0000>--</font>");
		}
		if (npc.getSpawn() != null)
		{
			if (npc.getSpawn().getRespawnMinDelay() == 0)
			{
				html.replace("%resp%", "None");
			}
			else if (npc.getSpawn().hasRespawnRandom())
			{
				html.replace("%resp%", String.valueOf(npc.getSpawn().getRespawnMinDelay() / 1000) + "-" + String.valueOf(npc.getSpawn().getRespawnMaxDelay() / 1000) + " sec");
			}
			else
			{
				html.replace("%resp%", String.valueOf(npc.getSpawn().getRespawnMinDelay() / 1000) + " sec");
			}
			html.replace("%territory%", (npc.getSpawn().getTerritoryName() != null && !npc.getSpawn().getTerritoryName().isEmpty()) ? npc.getSpawn().getTerritoryName() : "<font color=FF0000>--</font>");
		}
		else
		{
			html.replace("%resp%", "<font color=FF0000>--</font>");
			html.replace("%territory%", "<font color=FF0000>--</font>");
		}
		
		if (npc.hasAI())
		{
			html.replace("%ai_intention%", "<tr><td><table width=270 border=0 bgcolor=131210><tr><td width=100><font color=FFAA00>Intention:</font></td><td align=right width=170>" + String.valueOf(npc.getAI().getIntention().name()) + "</td></tr></table></td></tr>");
			html.replace("%ai_type%", "<tr><td><table width=270 border=0><tr><td width=100><font color=FFAA00>AIType</font></td><td align=right width=170>" + String.valueOf(npc.getAiType()) + "</td></tr></table></td></tr>");
			html.replace("%ai_clan%", "<tr><td><table width=270 border=0 bgcolor=131210><tr><td width=100><font color=FFAA00>Clan & Range:</font></td><td align=right width=170>" + String.valueOf(npc.getFaction().getName()) + " " + String.valueOf(npc.getFaction().getRange()) + "</td></tr></table></td></tr>");
		}
		else
		{
			html.replace("%ai_intention%", "");
			html.replace("%ai_type%", "");
			html.replace("%ai_clan%", "");
		}
		
		final String routeName = WalkingManager.getInstance().getRouteName(npc);
		if (!routeName.isEmpty())
		{
			html.replace("%route%", "<tr><td><table width=270 border=0><tr><td width=100><font color=LEVEL>Route:</font></td><td align=right width=170>" + routeName + "</td></tr></table></td></tr>");
		}
		else
		{
			html.replace("%route%", "");
		}
		player.sendPacket(html);
	}
	
	protected void showNpcStatList(Player player, Npc npc)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile(player, player.getLang(), "data/html/admin/npcstats.htm");
		
		html.replace("%id%", String.valueOf(npc.getTemplate().getId()));
		html.replace("%lvl%", String.valueOf(npc.getTemplate().getLevel()));
		html.replace("%name%", String.valueOf(npc.getTemplate().getName(player.getLang())));
		html.replace("%tmplid%", String.valueOf(npc.getTemplate().getId()));
		html.replace("%patk%", String.valueOf((int) npc.getPAtk(null)));
		html.replace("%matk%", String.valueOf((int) npc.getMAtk(null, null)));
		html.replace("%pdef%", String.valueOf((int) npc.getPDef(null)));
		html.replace("%mdef%", String.valueOf((int) npc.getMDef(null, null)));
		html.replace("%accu%", String.valueOf(npc.getAccuracy()));
		html.replace("%evas%", String.valueOf(npc.getEvasionRate(null)));
		html.replace("%crit%", String.valueOf((int) npc.getCriticalHit(null, null)));
		html.replace("%rspd%", String.valueOf((int) npc.getRunSpeed()));
		html.replace("%aspd%", String.valueOf((int) npc.getPAtkSpd()));
		html.replace("%cspd%", String.valueOf((int) npc.getMAtkSpd()));
		html.replace("%atkType%", String.valueOf(npc.getTemplate().getBaseAttackType()));
		html.replace("%atkRng%", String.valueOf(npc.getTemplate().getBaseAttackRange()));
		html.replace("%str%", String.valueOf(npc.getSTR()));
		html.replace("%dex%", String.valueOf(npc.getDEX()));
		html.replace("%con%", String.valueOf(npc.getCON()));
		html.replace("%int%", String.valueOf(npc.getINT()));
		html.replace("%wit%", String.valueOf(npc.getWIT()));
		html.replace("%men%", String.valueOf(npc.getMEN()));
		
		final byte attackAttribute = npc.getAttackElement();
		html.replace("%ele_atk%", Elementals.getElementName(attackAttribute));
		html.replace("%ele_atk_value%", String.valueOf(npc.getAttackElementValue(attackAttribute)));
		html.replace("%ele_dfire%", String.valueOf(npc.getDefenseElementValue(Elementals.FIRE)));
		html.replace("%ele_dwater%", String.valueOf(npc.getDefenseElementValue(Elementals.WATER)));
		html.replace("%ele_dwind%", String.valueOf(npc.getDefenseElementValue(Elementals.WIND)));
		html.replace("%ele_dearth%", String.valueOf(npc.getDefenseElementValue(Elementals.EARTH)));
		html.replace("%ele_dholy%", String.valueOf(npc.getDefenseElementValue(Elementals.HOLY)));
		html.replace("%ele_ddark%", String.valueOf(npc.getDefenseElementValue(Elementals.DARK)));
		player.sendPacket(html);
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
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}