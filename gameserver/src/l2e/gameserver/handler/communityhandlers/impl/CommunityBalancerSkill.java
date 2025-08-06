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
package l2e.gameserver.handler.communityhandlers.impl;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import l2e.gameserver.Config;
import l2e.gameserver.data.htm.HtmCache;
import l2e.gameserver.data.parser.SkillBalanceParser;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.handler.communityhandlers.ICommunityBoardHandler;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.base.ClassId;
import l2e.gameserver.model.base.SkillChangeType;
import l2e.gameserver.model.holders.SkillBalanceHolder;
import l2e.gameserver.model.skills.Skill;

public class CommunityBalancerSkill extends AbstractCommunity implements ICommunityBoardHandler
{
	public CommunityBalancerSkill()
	{
		if (Config.DEBUG)
		{
			_log.info(getClass().getSimpleName() + ": Loading all functions.");
		}
	}
	
	@Override
	public String[] getBypassCommands()
	{
		return new String[]
		{
		        "skillbalance", "_bbs_skillbalance", "_bbs_save_skillbalance", "_bbs_remove_skillbalance", "_bbs_modify_skillbalance", "_bbs_add_menu_skillbalance", "_bbs_add_skillbalance", "_bbs_search_skillbalance", "_bbs_search_nav_skillbalance", "_bbs_get_skillbalance"
		};
	}
	
	@Override
	public void onBypassCommand(String command, Player activeChar)
	{
		if (!Config.BALANCER_ALLOW || !activeChar.getAccessLevel().allowBalancer())
		{
			return;
		}
		
		if (command.startsWith("_bbs_skillbalance"))
		{
			final StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken();
			
			final int pageId = st.countTokens() == 2 ? Integer.parseInt(st.nextToken()) : 1;
			final boolean isOly = Boolean.parseBoolean(st.nextToken());
			
			showMainHtml(activeChar, pageId, isOly);
		}
		else if (command.startsWith("_bbs_save_skillbalance"))
		{
			final StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken();
			
			final int pageId = Integer.parseInt(st.nextToken());
			final boolean isOly = Boolean.parseBoolean(st.nextToken());
			
			SkillBalanceParser.getInstance().store(activeChar);
			showMainHtml(activeChar, pageId, isOly);
		}
		else if (command.startsWith("_bbs_remove_skillbalance"))
		{
			final String[] info = command.substring(25).split(" ");
			final String key = info[0];
			final int pageId = info.length > 1 ? Integer.parseInt(info[1]) : 1;
			final int type = Integer.valueOf(info[2]).intValue();
			final boolean isOly = Boolean.parseBoolean(info[3]);
			
			SkillBalanceParser.getInstance().removeSkillBalance(key, SkillChangeType.VALUES[type], isOly);
			showMainHtml(activeChar, pageId, isOly);
		}
		else if (command.startsWith("_bbs_modify_skillbalance"))
		{
			final String[] st = command.split(";");
			final int skillId = Integer.valueOf(st[0].substring(25)).intValue();
			final int target = Integer.valueOf(st[1]).intValue();
			final int changeType = Integer.valueOf(st[2]).intValue();
			final double value = Double.parseDouble(st[3]);
			final int pageId = Integer.parseInt(st[4]);
			final boolean isSearch = Boolean.parseBoolean(st[5]);
			final boolean isOly = Boolean.parseBoolean(st[6]);
			final String key = skillId + ";" + target;
			final SkillBalanceHolder cbh = SkillBalanceParser.getInstance().getSkillHolder(key);
			
			if (isOly)
			{
				cbh.addOlySkillBalance(SkillChangeType.VALUES[changeType], value);
			}
			else
			{
				cbh.addSkillBalance(SkillChangeType.VALUES[changeType], value);
			}
			
			SkillBalanceParser.getInstance().addSkillBalance(key, cbh, true);
			
			if (isSearch)
			{
				showSearchHtml(activeChar, pageId, skillId, isOly);
			}
			else
			{
				showMainHtml(activeChar, pageId, isOly);
			}
		}
		else if (command.startsWith("_bbs_add_menu_skillbalance"))
		{
			final StringTokenizer st = new StringTokenizer(command.substring(27), " ");
			final int pageId = Integer.parseInt(st.nextToken());
			final int tRace = Integer.parseInt(st.nextToken());
			final boolean isOly = Boolean.parseBoolean(st.nextToken());
			
			showAddHtml(activeChar, pageId, tRace, isOly);
		}
		else if (command.startsWith("_bbs_add_skillbalance"))
		{
			final String[] st = command.substring(22).split(";");
			final StringTokenizer st2 = new StringTokenizer(command.substring(22), ";");
			
			if (st2.countTokens() != 5 || st[0].isEmpty() || st[1].isEmpty() || st[2].isEmpty() || st[3].isEmpty() || st[4].isEmpty())
			{
				activeChar.sendMessage("Incorrect input count.");
				return;
			}
			
			final int skillId = Integer.valueOf(st[0].trim()).intValue();
			final String attackTypeSt = st[1].trim();
			final String val = st[2].trim();
			final String targetClassName = st[3].trim();
			final boolean isoly = Boolean.parseBoolean(st[4].trim());
			final double value = Double.parseDouble(val);
			
			if (SkillsParser.getInstance().getInfo(skillId, SkillsParser.getInstance().getMaxLevel(skillId)) == null)
			{
				activeChar.sendMessage("Skill with id: " + skillId + " not found!");
				return;
			}
			
			int targetClassId = targetClassName.equals("All") ? -2 : -1;
			if (!targetClassName.equals(""))
			{
				for (final ClassId cId : ClassId.values())
				{
					if (cId.name().equalsIgnoreCase(targetClassName))
					{
						targetClassId = cId.ordinal();
					}
				}
			}
			
			targetClassId = SkillChangeType.valueOf(attackTypeSt).isOnlyVsAll() ? -2 : targetClassId;
			
			final String key = skillId + ";" + targetClassId;
			final SkillBalanceHolder cbh = SkillBalanceParser.getInstance().getSkillHolder(key) != null ? SkillBalanceParser.getInstance().getSkillHolder(key) : new SkillBalanceHolder(skillId, targetClassId);
			
			if (isoly)
			{
				cbh.addOlySkillBalance(SkillChangeType.valueOf(attackTypeSt), value);
			}
			else
			{
				cbh.addSkillBalance(SkillChangeType.valueOf(attackTypeSt), value);
			}
			
			SkillBalanceParser.getInstance().addSkillBalance(key, cbh, false);
			
			showMainHtml(activeChar, 1, isoly);
		}
		else if (command.startsWith("_bbs_search_skillbalance"))
		{
			final StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken();
			
			if (st.countTokens() == 2)
			{
				final int skillId = Integer.valueOf(st.nextToken()).intValue();
				final boolean isOly = Boolean.parseBoolean(st.nextToken());
				
				showSearchHtml(activeChar, 1, skillId, isOly);
			}
		}
		else if (command.startsWith("_bbs_search_nav_skillbalance"))
		{
			final StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken();
			
			if (st.countTokens() == 3)
			{
				final int skillId = Integer.valueOf(st.nextToken()).intValue();
				final int pageID = Integer.valueOf(st.nextToken()).intValue();
				final boolean isOly = Boolean.parseBoolean(st.nextToken());
				
				showSearchHtml(activeChar, pageID, skillId, isOly);
			}
		}
		else if (command.startsWith("_bbs_get_skillbalance"))
		{
			final StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken();
			
			if (st.hasMoreTokens())
			{
				final int skillId = Integer.valueOf(st.nextToken()).intValue();
				final Skill skill = SkillsParser.getInstance().getInfo(skillId, SkillsParser.getInstance().getMaxLevel(skillId));
				if (skill != null)
				{
					activeChar.addSkill(skill);
					activeChar.sendMessage("You have learned: " + skill.getName(activeChar.getLang()));
				}
			}
		}
	}
	
	public void showMainHtml(Player activeChar, int pageId, boolean isolyinfo)
	{
		String html = HtmCache.getInstance().getHtm(activeChar, activeChar.getLang(), "data/html/mods/balancer/skillbalance/index.htm");
		final String info = getSkillBalanceInfo(activeChar, SkillBalanceParser.getInstance().getAllBalances().values(), pageId, false, isolyinfo);
		
		final int count = SkillBalanceParser.getInstance().getSize(isolyinfo);
		final int limitInPage = 6;
		
		html = html.replace("<?title?>", isolyinfo ? "Olympiad" : "");
		html = html.replace("<?isoly?>", String.valueOf(isolyinfo));
		html = html.replace("%pageID%", String.valueOf(pageId));
		
		int totalpages = 1;
		int tmpcount = count;
		
		while (tmpcount - 6 > 0)
		{
			totalpages++;
			tmpcount -= 6;
		}
		
		html = html.replace("%totalPages%", String.valueOf(totalpages));
		html = html.replace("%info%", info);
		html = html.replace("%previousPage%", String.valueOf(pageId - 1 != 0 ? pageId - 1 : 1));
		html = html.replace("%nextPage%", String.valueOf(pageId * limitInPage >= count ? pageId : pageId + 1));
		
		separateAndSend(html, activeChar);
	}
	
	public void showSearchHtml(Player activeChar, int pageId, int skillId, boolean isolysearch)
	{
		String html = HtmCache.getInstance().getHtm(activeChar, activeChar.getLang(), "data/html/mods/balancer/skillbalance/search.htm");
		final String info = getSkillBalanceInfo(activeChar, SkillBalanceParser.getInstance().getSkillBalances(skillId), pageId, true, isolysearch);
		
		final int count = SkillBalanceParser.getInstance().getSkillBalanceSize(skillId, isolysearch);
		final int limitInPage = 6;
		
		html = html.replace("%pageID%", String.valueOf(pageId));
		
		int totalpages = 1;
		int tmpcount = count;
		
		while (tmpcount - 6 > 0)
		{
			totalpages++;
			tmpcount -= 6;
		}
		
		html = html.replace("<?title?>", isolysearch ? "Olympiad" : "");
		html = html.replace("<?isoly?>", String.valueOf(isolysearch));
		html = html.replace("%totalPages%", String.valueOf(totalpages));
		html = html.replace("%info%", info);
		html = html.replace("%skillId%", String.valueOf(skillId));
		html = html.replace("%previousPage%", String.valueOf(pageId - 1 != 0 ? pageId - 1 : 1));
		html = html.replace("%nextPage%", String.valueOf(pageId * limitInPage >= count ? pageId : pageId + 1));
		
		separateAndSend(html, activeChar);
	}
	
	public void showAddHtml(Player activeChar, int pageId, int tRace, boolean isoly)
	{
		String html = HtmCache.getInstance().getHtm(activeChar, activeChar.getLang(), "data/html/mods/balancer/skillbalance/" + (isoly ? "olyadd.htm" : "add.htm"));
		String tClasses = "";
		
		if (tRace < 6)
		{
			for (final ClassId classId : ClassId.values())
			{
				if (classId.getRace() != null)
				{
					if (classId.level() == 3 && classId.getRace().ordinal() == tRace)
					{
						tClasses = tClasses + classId.name() + ";";
					}
				}
			}
		}
		else
		{
			tClasses = tRace == 6 ? "Monsters" : "All";
		}
		
		html = html.replace("<?pageId?>", String.valueOf(pageId));
		html = html.replace("<?isoly?>", String.valueOf(isoly));
		html = html.replace("<?tClasses?>", tClasses);
		
		html = html.replace("<?trace0Checked?>", tRace == 0 ? "_checked" : "");
		html = html.replace("<?trace1Checked?>", tRace == 1 ? "_checked" : "");
		html = html.replace("<?trace2Checked?>", tRace == 2 ? "_checked" : "");
		html = html.replace("<?trace3Checked?>", tRace == 3 ? "_checked" : "");
		html = html.replace("<?trace4Checked?>", tRace == 4 ? "_checked" : "");
		html = html.replace("<?trace5Checked?>", tRace == 5 ? "_checked" : "");
		html = html.replace("<?trace6Checked?>", tRace == 6 ? "_checked" : "");
		html = html.replace("<?trace7Checked?>", tRace == 7 ? "_checked" : "");
		
		separateAndSend(html, activeChar);
	}
	
	private static String getSkillBalanceInfo(Player activeChar, Collection<SkillBalanceHolder> collection, int pageId, boolean search, boolean isOly)
	{
		if (collection == null)
		{
			return "";
		}
		
		String info = "";
		int count = 1;
		final int limitInPage = 6;
		
		for (final Iterator<SkillBalanceHolder> localIterator1 = collection.iterator(); localIterator1.hasNext();)
		{
			final SkillBalanceHolder balance = localIterator1.next();
			final int targetClassId = balance.getTarget();
			if (!ClassId.getClassById(targetClassId).name().equals("") || targetClassId <= -1)
			{
				final Set<Map.Entry<SkillChangeType, Double>> localCollection = isOly ? balance.getOlyBalance().entrySet() : balance.getNormalBalance().entrySet();
				for (final Map.Entry<SkillChangeType, Double> dt : localCollection)
				{
					if ((count > limitInPage * (pageId - 1)) && (count <= limitInPage * pageId))
					{
						final double val = dt.getValue().doubleValue();
						final double percents = Math.round(val * 100.0D) - 100L;
						final double addedValue = Math.round((val + 0.1D) * 10.0D) / 10.0D;
						final double removedValue = Math.round((val - 0.1D) * 10.0D) / 10.0D;
						
						String content = HtmCache.getInstance().getHtm(activeChar, activeChar.getLang(), "data/html/mods/balancer/skillbalance/info-template.htm");
						content = content.replace("<?pos?>", String.valueOf(count));
						content = content.replace("<?key?>", balance.getSkillId() + ";" + balance.getTarget());
						content = content.replace("<?skillId?>", String.valueOf(balance.getSkillId()));
						content = content.replace("<?skillName?>", SkillsParser.getInstance().getInfo(balance.getSkillId(), SkillsParser.getInstance().getMaxLevel(balance.getSkillId())).getName(activeChar.getLang()));
						content = content.replace("<?type?>", dt.getKey().name());
						content = content.replace("<?editedType?>", String.valueOf(dt.getKey().getId()));
						content = content.replace("<?removedValue?>", String.valueOf(removedValue));
						content = content.replace("<?search?>", String.valueOf(search));
						content = content.replace("<?isoly?>", String.valueOf(isOly));
						content = content.replace("<?addedValue?>", String.valueOf(addedValue));
						content = content.replace("<?pageId?>", String.valueOf(pageId));
						content = content.replace("<?value?>", String.valueOf(val));
						content = content.replace("<?targetClassName?>", targetClassId <= -1 ? "All" : targetClassId == -1 ? "Monster" : ClassId.getClassById(targetClassId).name());
						content = content.replace("<?percents?>", percents > 0.0D ? "+" : "");
						content = content.replace("<?percentValue?>", String.valueOf(percents).substring(0, String.valueOf(percents).indexOf(".")));
						content = content.replace("<?targetId?>", String.valueOf(targetClassId));
						content = content.replace("<?skillIcon?>", balance.getSkillIcon());
						
						info = info + content;
					}
					
					count++;
				}
			}
		}
		return info;
	}
	
	@Override
	public void onWriteCommand(String command, String s, String s1, String s2, String s3, String s4, Player Player)
	{
	}
	
	public static CommunityBalancerSkill getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final CommunityBalancerSkill _instance = new CommunityBalancerSkill();
	}
}