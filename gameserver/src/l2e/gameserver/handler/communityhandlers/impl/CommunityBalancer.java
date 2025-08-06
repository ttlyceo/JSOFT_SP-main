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
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import l2e.gameserver.Config;
import l2e.gameserver.data.htm.HtmCache;
import l2e.gameserver.data.parser.ClassBalanceParser;
import l2e.gameserver.handler.communityhandlers.ICommunityBoardHandler;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.base.AttackType;
import l2e.gameserver.model.base.ClassId;
import l2e.gameserver.model.holders.ClassBalanceHolder;

public class CommunityBalancer extends AbstractCommunity implements ICommunityBoardHandler
{
	public CommunityBalancer()
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
		        "classbalance", "_bbs_balancer", "_bbs_save_classbalance", "_bbs_remove_classbalance", "_bbs_modify_classbalance", "_bbs_add_menu_classbalance", "_bbs_add_classbalance", "_bbs_search_classbalance", "_bbs_search_nav_classbalance"
		};
	}
	
	private static void showMainWindow(Player activeChar)
	{
		final String html = HtmCache.getInstance().getHtm(activeChar, activeChar.getLang(), "data/html/mods/balancer/index.htm");
		separateAndSend(html, activeChar);
	}
	
	@Override
	public void onBypassCommand(String command, Player activeChar)
	{
		if (!Config.BALANCER_ALLOW || !activeChar.getAccessLevel().allowBalancer())
		{
			return;
		}
		
		if (command.equals("_bbs_balancer"))
		{
			showMainWindow(activeChar);
		}
		else if (command.startsWith("_bbs_classbalance"))
		{
			final StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken();
			
			final int pageId = st.countTokens() == 2 ? Integer.parseInt(st.nextToken()) : 1;
			final boolean isOly = Boolean.parseBoolean(st.nextToken());
			
			showMainHtml(activeChar, pageId, isOly);
		}
		else if (command.startsWith("_bbs_save_classbalance"))
		{
			final StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken();
			
			final int pageId = Integer.parseInt(st.nextToken());
			final boolean isOly = Boolean.parseBoolean(st.nextToken());
			
			ClassBalanceParser.getInstance().store(activeChar);
			showMainHtml(activeChar, pageId, isOly);
		}
		else if (command.startsWith("_bbs_remove_classbalance"))
		{
			final StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken();
			
			final String key = st.nextToken();
			final int pageId = Integer.parseInt(st.nextToken());
			final int type = Integer.valueOf(st.nextToken());
			final boolean isOly = Boolean.parseBoolean(st.nextToken());
			
			ClassBalanceParser.getInstance().removeClassBalance(key, AttackType.VALUES[type], isOly);
			showMainHtml(activeChar, pageId, isOly);
		}
		else if (command.startsWith("_bbs_modify_classbalance"))
		{
			final StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken();
			
			final  String[] array = st.nextToken().split(";");
			final int classId = Integer.valueOf(array[0]).intValue();
			final int targetClassId = Integer.valueOf(array[1]).intValue();
			final int attackType = Integer.valueOf(array[2]).intValue();
			final double value = Double.parseDouble(array[3]);
			final int pageId = Integer.parseInt(array[4]);
			final boolean isSearch = Boolean.parseBoolean(array[5]);
			final boolean isOly = Boolean.parseBoolean(array[6]);
			final String key = classId + ";" + targetClassId;
			final ClassBalanceHolder cbh = ClassBalanceParser.getInstance().getBalanceHolder(key);
			
			if (isOly)
			{
				cbh.addOlyBalance(AttackType.VALUES[attackType], value);
			}
			else
			{
				cbh.addNormalBalance(AttackType.VALUES[attackType], value);
			}
			
			ClassBalanceParser.getInstance().addClassBalance(key, cbh, true);
			
			if (isSearch)
			{
				showSearchHtml(activeChar, pageId, Integer.valueOf(classId).intValue(), isOly);
			}
			else
			{
				showMainHtml(activeChar, pageId, isOly);
			}
		}
		else if (command.startsWith("_bbs_add_menu_classbalance"))
		{
			final StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken();
			
			final int pageId = Integer.parseInt(st.nextToken());
			final int race = Integer.parseInt(st.nextToken());
			final int tRace = Integer.parseInt(st.nextToken());
			final boolean isOly = Boolean.parseBoolean(st.nextToken());
			
			showAddHtml(activeChar, pageId, race, tRace, isOly);
		}
		else if (command.startsWith("_bbs_add_classbalance"))
		{
			final StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken();
			
			final String className = st.nextToken().trim();
			final String attackTypeSt = st.nextToken();
			final String val = st.nextToken();
			final String targetClassName = st.nextToken().trim();
			final boolean isoly = Boolean.parseBoolean(st.nextToken());
			
			int classId = -1;
			if (!className.equals(""))
			{
				final ClassId[] values = ClassId.values();
				for (int key = 0; key < values.length; key++)
				{
					final ClassId cId = values[key];
					if (cId.name().equalsIgnoreCase(className))
					{
						classId = cId.ordinal();
					}
				}
			}
			
			int targetClassId = targetClassName.equals("All") ? -2 : -1;
			if (!targetClassName.equals(""))
			{
				final ClassId[] values = ClassId.values();
				for (int key = 0; key < values.length; key++)
				{
					final ClassId cId = values[key];
					if (cId.name().equalsIgnoreCase(targetClassName))
					{
						targetClassId = cId.ordinal();
					}
				}
			}
			
			targetClassId = AttackType.valueOf(attackTypeSt).isOnlyVsAll() ? -2 : targetClassId;
			
			final double value = Double.parseDouble(val);
			final String key = classId + ";" + targetClassId;
			final ClassBalanceHolder cbh = ClassBalanceParser.getInstance().getBalanceHolder(key) != null ? ClassBalanceParser.getInstance().getBalanceHolder(key) : new ClassBalanceHolder(classId, targetClassId);
			
			if (isoly)
			{
				cbh.addOlyBalance(AttackType.valueOf(attackTypeSt), value);
			}
			else
			{
				cbh.addNormalBalance(AttackType.valueOf(attackTypeSt), value);
			}
			
			ClassBalanceParser.getInstance().addClassBalance(key, cbh, false);
			showMainHtml(activeChar, 1, isoly);
		}
		else if (command.startsWith("_bbs_search_classbalance"))
		{
			final StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken();
			
			if (st.countTokens() == 2)
			{
				final int classId = Integer.valueOf(st.nextToken()).intValue();
				final boolean isOly = Boolean.parseBoolean(st.nextToken());
				
				showSearchHtml(activeChar, 1, classId, isOly);
			}
		}
		else if (command.startsWith("_bbs_search_nav_classbalance"))
		{
			final StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken();
			
			if (st.countTokens() == 3)
			{
				final int classId = Integer.valueOf(st.nextToken()).intValue();
				final int pageID = Integer.valueOf(st.nextToken()).intValue();
				final boolean isOly = Boolean.parseBoolean(st.nextToken());
				
				showSearchHtml(activeChar, pageID, classId, isOly);
			}
		}
	}
	
	public void showMainHtml(Player activeChar, int pageId, boolean isolyinfo)
	{
		String html = HtmCache.getInstance().getHtm(activeChar, activeChar.getLang(), "data/html/mods/balancer/classbalance/index.htm");
		final String info = getBalanceInfo(activeChar, ClassBalanceParser.getInstance().getAllBalances().values(), pageId, false, isolyinfo);
		
		final int count = ClassBalanceParser.getInstance().getSize(isolyinfo);
		final int limitInPage = 7;
		
		html = html.replace("<?title?>", isolyinfo ? "Olympiad" : "");
		html = html.replace("<?isoly?>", String.valueOf(isolyinfo));
		html = html.replace("%pageID%", String.valueOf(pageId));
		
		int totalpages = 1;
		int tmpcount = count;
		
		while (tmpcount - 7 > 0)
		{
			totalpages++;
			tmpcount -= 7;
		}
		
		html = html.replace("%totalPages%", String.valueOf(totalpages));
		html = html.replace("%info%", info);
		html = html.replace("%previousPage%", String.valueOf(pageId - 1 != 0 ? pageId - 1 : 1));
		html = html.replace("%nextPage%", String.valueOf(pageId * limitInPage >= count ? pageId : pageId + 1));
		
		separateAndSend(html, activeChar);
	}
	
	public void showAddHtml(Player activeChar, int pageId, int race, int tRace, boolean isOly)
	{
		String html = HtmCache.getInstance().getHtm(activeChar, activeChar.getLang(), "data/html/mods/balancer/classbalance/" + (isOly ? "olyadd.htm" : "add.htm"));
		
		String classes = "";
		if (race < 6)
		{
			final ClassId[] array = ClassId.values();
			for (int cId = 0; cId < array.length; cId++)
			{
				final ClassId classId = array[cId];
				if (classId.getRace() != null)
				{
					if (isOly)
					{
						if (classId.level() == 3 && classId.getRace().ordinal() == race)
						{
							classes = classes + classId.name() + ";";
						}
					}
					else if (classId.level() >= 2 && classId.getRace().ordinal() == race)
					{
						classes = classes + classId.name() + ";";
					}
				}
			}
		}
		else
		{
			classes = race == 6 ? "Monsters" : "All";
		}
		
		String tClasses = "";
		if (tRace < 6)
		{
			final ClassId[] array2 = ClassId.values();
			for (int cId = 0; cId < array2.length; cId++)
			{
				final ClassId classId = array2[cId];
				if (classId.getRace() != null)
				{
					if (isOly)
					{
						if (classId.level() == 3 && classId.getRace().ordinal() == tRace)
						{
							tClasses = tClasses + classId.name() + ";";
						}
					}
					else if (classId.level() >= 2 && classId.getRace().ordinal() == tRace)
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
		html = html.replace("<?tRace?>", String.valueOf(tRace));
		html = html.replace("<?race0Checked?>", race == 0 ? "_checked" : "");
		html = html.replace("<?race1Checked?>", race == 1 ? "_checked" : "");
		html = html.replace("<?race2Checked?>", race == 2 ? "_checked" : "");
		html = html.replace("<?race3Checked?>", race == 3 ? "_checked" : "");
		html = html.replace("<?race4Checked?>", race == 4 ? "_checked" : "");
		html = html.replace("<?race5Checked?>", race == 5 ? "_checked" : "");
		html = html.replace("<?race7Checked?>", race == 7 ? "_checked" : "");
		
		html = html.replace("<?classes?>", classes);
		html = html.replace("<?tClasses?>", tClasses);
		
		html = html.replace("<?race?>", String.valueOf(race));
		
		html = html.replace("<?trace0Checked?>", tRace == 0 ? "_checked" : "");
		html = html.replace("<?trace1Checked?>", tRace == 1 ? "_checked" : "");
		html = html.replace("<?trace2Checked?>", tRace == 2 ? "_checked" : "");
		html = html.replace("<?trace3Checked?>", tRace == 3 ? "_checked" : "");
		html = html.replace("<?trace4Checked?>", tRace == 4 ? "_checked" : "");
		html = html.replace("<?trace5Checked?>", tRace == 5 ? "_checked" : "");
		html = html.replace("<?trace6Checked?>", tRace == 6 ? "_checked" : "");
		html = html.replace("<?trace7Checked?>", tRace == 7 ? "_checked" : "");
		html = html.replace("<?isoly?>", String.valueOf(isOly));
		
		separateAndSend(html, activeChar);
	}
	
	public void showSearchHtml(Player activeChar, int pageId, int sclassId, boolean isolysearch)
	{
		String html = HtmCache.getInstance().getHtm(activeChar, activeChar.getLang(), "data/html/mods/balancer/classbalance/search.htm");
		final String info = getBalanceInfo(activeChar, ClassBalanceParser.getInstance().getClassBalances(sclassId), pageId, true, isolysearch);
		
		final int count = ClassBalanceParser.getInstance().getClassBalanceSize(sclassId, isolysearch);
		final int limitInPage = 7;
		
		html = html.replace("%pageID%", String.valueOf(pageId));
		
		int totalpages = 1;
		int tmpcount = count;
		
		while (tmpcount - 7 > 0)
		{
			totalpages++;
			tmpcount -= 7;
		}
		
		html = html.replace("<?title?>", isolysearch ? "Olympiad" : "");
		html = html.replace("<?isoly?>", String.valueOf(isolysearch));
		html = html.replace("%totalPages%", String.valueOf(totalpages));
		html = html.replace("%info%", info);
		html = html.replace("%classID%", String.valueOf(sclassId));
		html = html.replace("%previousPage%", String.valueOf(pageId - 1 != 0 ? pageId - 1 : 1));
		html = html.replace("%nextPage%", String.valueOf(pageId * limitInPage >= count ? pageId : pageId + 1));
		
		separateAndSend(html, activeChar);
	}
	
	private static String getBalanceInfo(Player activeChar, Collection<ClassBalanceHolder> collection, int pageId, boolean search, boolean isOly)
	{
		if (collection == null)
		{
			return "";
		}
		
		String info = "";
		int count = 1;
		final int limitInPage = 7;
		
		for (final ClassBalanceHolder balance : collection)
		{
			final int classId = balance.getActiveClass();
			final int targetClassId = balance.getTargetClass();
			final String id = classId + ";" + targetClassId;
			
			if ((!ClassId.getClassById(classId).name().equals("") && !ClassId.getClassById(targetClassId).name().equals("")) || !ClassId.getClassById(classId).name().equals("") || targetClassId == -1)
			{
				final Set<Map.Entry<AttackType, Double>> localCollection = isOly ? balance.getOlyBalance().entrySet() : balance.getNormalBalance().entrySet();
				for (final Map.Entry<AttackType, Double> dt : localCollection)
				{
					if ((count > limitInPage * (pageId - 1)) && (count <= limitInPage * pageId))
					{
						final double val = dt.getValue().doubleValue();
						final double percents = Math.round(val * 100.0D) - 100L;
						final double addedValue = Math.round((val + 0.1D) * 10.0D) / 10.0D;
						final double removedValue = Math.round((val - 0.1D) * 10.0D) / 10.0D;
						final String attackTypeSt = dt.getKey().name();
						
						String content = HtmCache.getInstance().getHtm(activeChar, activeChar.getLang(), "data/html/mods/balancer/classbalance/info-template.htm");
						content = content.replace("<?pos?>", String.valueOf(count));
						content = content.replace("<?classId?>", String.valueOf(classId));
						content = content.replace("<?className?>", classId <= -1 ? "(All)" : classId == -1 ? "Monster" : ClassId.getClassById(classId).name());
						content = content.replace("<?type?>", attackTypeSt);
						content = content.replace("<?key?>", id);
						content = content.replace("<?targetClassId?>", String.valueOf(targetClassId));
						content = content.replace("<?editedType?>", String.valueOf(dt.getKey().getId()));
						content = content.replace("<?removedValue?>", String.valueOf(removedValue));
						content = content.replace("<?search?>", String.valueOf(search));
						content = content.replace("<?isoly?>", String.valueOf(isOly));
						content = content.replace("<?addedValue?>", String.valueOf(addedValue));
						content = content.replace("<?pageId?>", String.valueOf(pageId));
						content = content.replace("<?targetClassName?>", targetClassId <= -1 ? "(All)" : targetClassId == -1 ? "Monster" : ClassId.getClassById(targetClassId).name());
						content = content.replace("<?value?>", String.valueOf(val));
						content = content.replace("<?percents?>", percents > 0.0D ? "+" : "");
						content = content.replace("<?percentValue?>", String.valueOf(percents).substring(0, String.valueOf(percents).indexOf(".")));
						
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
	
	public static CommunityBalancer getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final CommunityBalancer _instance = new CommunityBalancer();
	}
}