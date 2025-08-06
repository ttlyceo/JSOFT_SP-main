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
package l2e.gameserver.model.entity.events.custom.achievements;

import java.io.File;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.parsers.DocumentBuilderFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import l2e.gameserver.Config;
import l2e.gameserver.data.htm.HtmCache;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.player.AchiveTemplate;
import l2e.gameserver.model.stats.StatsSet;
import l2e.gameserver.model.strings.server.ServerStorage;
import l2e.gameserver.network.serverpackets.TutorialCloseHtml;
import l2e.gameserver.network.serverpackets.TutorialShowHtml;

public class AchievementManager
{
	private static final Logger _log = LoggerFactory.getLogger(AchievementManager.class);
	
	private final Map<Integer, Integer> _achievementMaxLevels = new ConcurrentHashMap<>();
	private final List<AchievementCategory> _achievementCategories = new LinkedList<>();
	private final Map<Integer, AchiveTemplate> _achKillById = new ConcurrentHashMap<>();
	private final Map<Integer, AchiveTemplate> _achRefById = new ConcurrentHashMap<>();
	private final Map<Integer, AchiveTemplate> _achQuestById = new ConcurrentHashMap<>();
	private final Map<Integer, AchiveTemplate> _achEnchantWeaponByLvl = new ConcurrentHashMap<>();
	private final Map<Integer, AchiveTemplate> _achEnchantArmorByLvl = new ConcurrentHashMap<>();
	private final Map<Integer, AchiveTemplate> _achEnchantJewerlyByLvl = new ConcurrentHashMap<>();
	
	private boolean _isActive;
	
	public AchievementManager()
	{
		load();
	}
	
	public void onBypass(Player player, String bypass, String[] cm)
	{
		if (bypass.startsWith("_bbs_achievements_cat"))
		{
			generatePage(player, Integer.parseInt(cm[1]), Integer.parseInt(cm[2]));
		}
		else if (bypass.equals("_bbs_achievements_close"))
		{
			player.sendPacket(TutorialCloseHtml.STATIC_PACKET);
		}
		else if (bypass.startsWith("_bbs_achievements"))
		{
			checkAchievementRewards(player);
			int page = 1;
			if (cm != null && cm.length > 0)
			{
				page = Integer.parseInt(cm[1]);
			}
			generatePage(player, page);
		}
	}
	
	public void generatePage(Player player, int page)
	{
		if (player == null)
		{
			return;
		}
		
		int all = 0;
		int visual = 0;
		final boolean pagereached = false;
		final int totalpages = (int) Math.ceil((double) _achievementCategories.size() / 4);
		
		String achievements = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/achievements/achievements.htm");
		
		achievements = achievements.replaceAll("%back%", page == 1 ? "&nbsp;" : "<button value=\"\" action=\"bypass -h _bbs_achievements " + (page - 1) + "\" width=40 height=20 back=\"L2UI_CT1.Inventory_DF_Btn_RotateRight\" fore=\"L2UI_CT1.Inventory_DF_Btn_RotateRight\">");
		achievements = achievements.replaceAll("%more%", totalpages <= page ? "&nbsp;" : "<button value=\"\" action=\"bypass -h _bbs_achievements " + (page + 1) + "\" width=40 height=20 back=\"L2UI_CT1.Inventory_DF_Btn_RotateLeft\" fore=\"L2UI_CT1.Inventory_DF_Btn_RotateLeft\">");
		
		String ac = "";
		for (final AchievementCategory cat : _achievementCategories)
		{
			all++;
			if (page == 1 && visual > 4)
			{
				continue;
			}
			if (!pagereached && all > page * 4)
			{
				continue;
			}
			if (!pagereached && all <= (page - 1) * 4)
			{
				continue;
			}
			
			visual++;
			
			ac += cat.getHtml(player);
		}
		achievements = achievements.replace("%categories%", ac);
		player.sendPacket(new TutorialShowHtml(achievements));
	}
	
	public void generatePage(Player player, int category, int page)
	{
		if (player == null)
		{
			return;
		}
		
		String FULL_PAGE = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/achievements/inAchievements.htm");
		
		boolean done;
		String achievementsNotDone = "";
		String achievementsDone = "";
		String html;
		
		long playerPoints = 0;
		int all = 0;
		int clansvisual = 0;
		final boolean pagereached = false;
		final int totalpages = (int) Math.ceil((double) (player.getAchievements(category).size()) / 3); // +1
		
		FULL_PAGE = FULL_PAGE.replaceAll("%back%", page == 1 ? "&nbsp;" : "<button value=\"\" action=\"bypass -h _bbs_achievements_cat " + category + " " + (page - 1) + "\" width=40 height=20 back=\"L2UI_CT1.Inventory_DF_Btn_RotateRight\" fore=\"L2UI_CT1.Inventory_DF_Btn_RotateRight\">");
		FULL_PAGE = FULL_PAGE.replaceAll("%more%", totalpages <= page ? "&nbsp;" : "<button value=\"\" action=\"bypass -h _bbs_achievements_cat " + category + " " + (page + 1) + "\" width=40 height=20 back=\"L2UI_CT1.Inventory_DF_Btn_RotateLeft\" fore=\"L2UI_CT1.Inventory_DF_Btn_RotateLeft\">");
		
		final AchievementCategory cat = _achievementCategories.stream().filter(ctg -> ctg.getCategoryId() == category).findAny().orElse(null);
		if (cat == null)
		{
			_log.warn("AchievementManager: getCatById - cat - is null, return. for " + player.getName(null));
			return;
		}
		
		for (final Entry<Integer, Integer> entry : player.getAchievements(category).entrySet())
		{
			final int aId = entry.getKey();
			final int nextLevel = (entry.getValue() + 1) >= getMaxLevel(aId) ? getMaxLevel(aId) : (entry.getValue() + 1);
			final AchiveTemplate a = getAchievement(aId, Math.max(1, nextLevel));
			
			if (a == null)
			{
				_log.warn("AchievementManager: GetAchievement - a - is null, return. for " + player.getName(null));
				return;
			}
			
			playerPoints = player.getCounters().getAchievementInfo(a.getId());
			
			all++;
			if (page == 1 && clansvisual > 3)
			{
				continue;
			}
			if (!pagereached && all > page * 3)
			{
				continue;
			}
			if (!pagereached && all <= (page - 1) * 3)
			{
				continue;
			}
			
			clansvisual++;
			
			if (!a.isDone(playerPoints))
			{
				done = false;
				
				String notDoneAchievement = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/achievements/oneAchievement.htm");

				final long needpoints = a.getPointsToComplete();
				final long diff = Math.max(0, needpoints - playerPoints);
				long greenbar = 24 * (playerPoints * 100 / needpoints) / 100;
				if (greenbar < 0)
				{
					greenbar = 0;
				}
				
				if (greenbar > 24)
				{
					greenbar = 24;
				}
				
				notDoneAchievement = notDoneAchievement.replaceFirst("%fame%", "" + a.getFame());
				notDoneAchievement = notDoneAchievement.replaceAll("%bar1%", "" + greenbar);
				notDoneAchievement = notDoneAchievement.replaceAll("%bar2%", "" + (24 - greenbar));

				notDoneAchievement = notDoneAchievement.replaceFirst("%cap1%", greenbar > 0 ? "Gauge_DF_Food_Left" : "Gauge_DF_Exp_bg_Left");
				notDoneAchievement = notDoneAchievement.replaceFirst("%cap2%", "Gauge_DF_Exp_bg_Right");

				notDoneAchievement = notDoneAchievement.replaceFirst("%desc%", a.getDescr(player.getLang()).replaceAll("%need%", "" + (diff > 0 ? diff : "...")));

				notDoneAchievement = notDoneAchievement.replaceFirst("%bg%", a.getId() % 2 == 0 ? "090908" : "0f100f");
				notDoneAchievement = notDoneAchievement.replaceFirst("%icon%", a.getIcon());
				notDoneAchievement = notDoneAchievement.replaceFirst("%name%", a.getName(player.getLang()) + (a.getLevel() > 1 ? (" " + ServerStorage.getInstance().getString(player.getLang(), "Achievement.LEVEL") + " " + a.getLevel()) : ""));
				
				html = notDoneAchievement;
			}
			else
			{
				done = true;
				
				String doneAchievement = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/achievements/oneAchievement.htm");

				doneAchievement = doneAchievement.replaceFirst("%fame%", "" + a.getFame());
				doneAchievement = doneAchievement.replaceAll("%bar1%", "24");
				doneAchievement = doneAchievement.replaceAll("%bar2%", "0");

				doneAchievement = doneAchievement.replaceFirst("%cap1%", "Gauge_DF_Food_Left");
				doneAchievement = doneAchievement.replaceFirst("%cap2%", "Gauge_DF_Food_Right");

				doneAchievement = doneAchievement.replaceFirst("%desc%", ServerStorage.getInstance().getString(player.getLang(), "Achievement.DONE"));

				doneAchievement = doneAchievement.replaceFirst("%bg%", a.getId() % 2 == 0 ? "090908" : "0f100f");
				doneAchievement = doneAchievement.replaceFirst("%icon%", a.getIcon());
				doneAchievement = doneAchievement.replaceFirst("%name%", a.getName(player.getLang()) + (a.getLevel() > 1 ? (" " + ServerStorage.getInstance().getString(player.getLang(), "Achievement.LEVEL") + " " + a.getLevel()) : ""));
				
				html = doneAchievement;
			}
			
			if (clansvisual < 3)
			{
				for (int d = clansvisual + 1; d != 3; d++)
				{
					html = html.replaceAll("%icon" + d + "%", "L2UI_CT1.Inventory_DF_CloakSlot_Disable");
					html = html.replaceAll("%bar1" + d + "%", "0");
					html = html.replaceAll("%bar2" + d + "%", "0");
					html = html.replaceAll("%cap1" + d + "%", "&nbsp;");
					html = html.replaceAll("%cap2" + d + "%", "&nbsp");
					html = html.replaceAll("%desc" + d + "%", "&nbsp");
					html = html.replaceAll("%bg" + d + "%", "0f100f");
					html = html.replaceAll("%name" + d + "%", "&nbsp");
				}
			}
			
			if (!done)
			{
				achievementsNotDone += html;
			}
			else
			{
				achievementsDone += html;
			}
		}
		
		int greenbar = 0;
		if (getAchievementLevelSum(player, category) > 0)
		{
			greenbar = 248 * (getAchievementLevelSum(player, category) * 100 / cat.getAchievements().size()) / 100;
			greenbar = Math.min(greenbar, 248);
		}
		String fp = FULL_PAGE;
		fp = fp.replaceAll("%bar1up%", "" + greenbar);
		fp = fp.replaceAll("%bar2up%", "" + (248 - greenbar));
		
		fp = fp.replaceFirst("%caps1%", greenbar > 0 ? "Gauge_DF_Large_Food_Left" : "Gauge_DF_Large_Exp_bg_Left");
		
		fp = fp.replaceFirst("%caps2%", greenbar >= 248 ? "Gauge_DF_Large_Food_Right" : "Gauge_DF_Large_Exp_bg_Right");
		
		fp = fp.replaceFirst("%achievementsNotDone%", achievementsNotDone);
		fp = fp.replaceFirst("%achievementsDone%", achievementsDone);
		fp = fp.replaceFirst("%catname%", cat.getName(player.getLang()));
		fp = fp.replaceFirst("%catDesc%", cat.getDescr(player.getLang()));
		fp = fp.replaceFirst("%catIcon%", cat.getIcon());
		
		player.sendPacket(new TutorialShowHtml(fp));
	}
	
	public void checkAchievementRewards(Player player)
	{
		synchronized (player.getAchievements())
		{
			for (final Entry<Integer, Integer> arco : player.getAchievements().entrySet())
			{
				final int achievementId = arco.getKey();
				int achievementLevel = arco.getValue();
				if (getMaxLevel(achievementId) <= achievementLevel)
				{
					continue;
				}
				
				AchiveTemplate nextLevelAchievement;
				do
				{
					achievementLevel++;
					nextLevelAchievement = getAchievement(achievementId, achievementLevel);
					if (nextLevelAchievement != null && nextLevelAchievement.isDone(player.getCounters().getAchievementInfo(nextLevelAchievement.getId())))
					{
						nextLevelAchievement.reward(player);
					}
				}
				while (nextLevelAchievement != null);
			}
		}
	}
	
	public int getPointsForThisLevel(int totalPoints, int achievementId, int achievementLevel)
	{
		if (totalPoints == 0)
		{
			return 0;
		}
		
		int result = 0;
		for (int i = achievementLevel; i > 0; i--)
		{
			final AchiveTemplate a = getAchievement(achievementId, i);
			if (a != null)
			{
				result += a.getPointsToComplete();
			}
		}
		return totalPoints - result;
	}
	
	public AchiveTemplate getAchievement(int achievementId, int achievementLevel)
	{
		for (final AchievementCategory cat : _achievementCategories)
		{
			for (final AchiveTemplate ach : cat.getAchievements())
			{
				if (ach.getId() == achievementId && ach.getLevel() == achievementLevel)
				{
					return ach;
				}
			}
		}
		return null;
	}
	
	public AchiveTemplate getAchievement(int achievementId)
	{
		for (final AchievementCategory cat : _achievementCategories)
		{
			for (final AchiveTemplate ach : cat.getAchievements())
			{
				if (ach.getId() == achievementId)
				{
					return ach;
				}
			}
		}
		return null;
	}
	
	public AchiveTemplate getAchievementType(String type)
	{
		for (final AchievementCategory cat : _achievementCategories)
		{
			for (final AchiveTemplate ach : cat.getAchievements())
			{
				if (ach.getType().equals(type))
				{
					return ach;
				}
			}
		}
		return null;
	}
	
	public AchiveTemplate getAchievementKillById(int npcId)
	{
		return _achKillById.get(npcId);
	}
	
	public AchiveTemplate getAchievementRefById(int id)
	{
		return _achRefById.get(id);
	}
	
	public AchiveTemplate getAchievementQuestById(int id)
	{
		return _achQuestById.get(id);
	}
	
	public AchiveTemplate getAchievementWeaponEnchantByLvl(int lvl)
	{
		return _achEnchantWeaponByLvl.get(lvl);
	}
	
	public AchiveTemplate getAchievementArmorEnchantByLvl(int lvl)
	{
		return _achEnchantArmorByLvl.get(lvl);
	}
	
	public AchiveTemplate getAchievementJewerlyEnchantByLvl(int lvl)
	{
		return _achEnchantJewerlyByLvl.get(lvl);
	}
	
	public Collection<Integer> getAchievementIds()
	{
		return _achievementMaxLevels.keySet();
	}
	
	public int getMaxLevel(int id)
	{
		return _achievementMaxLevels.getOrDefault(id, 0);
	}
	
	public static int getAchievementLevelSum(Player player, int categoryId)
	{
		return player.getAchievements(categoryId).values().stream().mapToInt(level -> level).sum();
	}
	
	public void load()
	{
		_achievementMaxLevels.clear();
		_achievementCategories.clear();
		_achKillById.clear();
		_achRefById.clear();
		_achQuestById.clear();
		_achEnchantWeaponByLvl.clear();
		_achEnchantArmorByLvl.clear();
		_achEnchantJewerlyByLvl.clear();
		
		try
		{
			final File file = new File("data/stats/services/achievements.xml");
			final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);
			final Document doc = factory.newDocumentBuilder().parse(file);
			
			for (Node g = doc.getFirstChild(); g != null; g = g.getNextSibling())
			{
				for (Node z = g.getFirstChild(); z != null; z = z.getNextSibling())
				{
					if (z.getNodeName().equals("categories"))
					{
						for (Node i = z.getFirstChild(); i != null; i = i.getNextSibling())
						{
							if ("cat".equalsIgnoreCase(i.getNodeName()))
							{
								final int categoryId = Integer.valueOf(i.getAttributes().getNamedItem("id").getNodeValue());
								final StatsSet params = new StatsSet();
								for (final String lang : Config.MULTILANG_ALLOWED)
								{
									if (lang != null)
									{
										final String name = "name" + lang.substring(0, 1).toUpperCase() + lang.substring(1);
										final String desc = "desc" + lang.substring(0, 1).toUpperCase() + lang.substring(1);
										params.set(name, i.getAttributes().getNamedItem(name) != null ? i.getAttributes().getNamedItem(name).getNodeValue() : i.getAttributes().getNamedItem("nameEn") != null ? i.getAttributes().getNamedItem("nameEn").getNodeValue() : "");
										params.set(desc, i.getAttributes().getNamedItem(desc) != null ? i.getAttributes().getNamedItem(desc).getNodeValue() : i.getAttributes().getNamedItem("descEn") != null ? i.getAttributes().getNamedItem("descEn").getNodeValue() : "");
									}
								}
								final String categoryIcon = String.valueOf(i.getAttributes().getNamedItem("icon").getNodeValue());
								_achievementCategories.add(new AchievementCategory(categoryId, params, categoryIcon));
							}
						}
					}
					else if (z.getNodeName().equals("achievement"))
					{
						final int achievementId = Integer.valueOf(z.getAttributes().getNamedItem("id").getNodeValue());
						final int achievementCategory = Integer.valueOf(z.getAttributes().getNamedItem("cat").getNodeValue());
						final StatsSet params = new StatsSet();
						for (final String lang : Config.MULTILANG_ALLOWED)
						{
							if (lang != null)
							{
								final String name = "desc" + lang.substring(0, 1).toUpperCase() + lang.substring(1);
								params.set(name, z.getAttributes().getNamedItem(name) != null ? z.getAttributes().getNamedItem(name).getNodeValue() : z.getAttributes().getNamedItem("descEn") != null ? z.getAttributes().getNamedItem("descEn").getNodeValue() : "");
							}
						}
						final String fieldType = String.valueOf(z.getAttributes().getNamedItem("type").getNodeValue());
						final int select = z.getAttributes().getNamedItem("select") != null ? Integer.valueOf(z.getAttributes().getNamedItem("select").getNodeValue()) : 0;
						int achievementMaxLevel = 0;
						for (Node i = z.getFirstChild(); i != null; i = i.getNextSibling())
						{
							if ("level".equalsIgnoreCase(i.getNodeName()))
							{
								final int level = Integer.valueOf(i.getAttributes().getNamedItem("id").getNodeValue());
								final long pointsToComplete = Long.parseLong(i.getAttributes().getNamedItem("need").getNodeValue());
								final int fame = Integer.valueOf(i.getAttributes().getNamedItem("fame").getNodeValue());
								for (final String lang : Config.MULTILANG_ALLOWED)
								{
									if (lang != null)
									{
										final String name = "name" + lang.substring(0, 1).toUpperCase() + lang.substring(1);
										params.set(name, i.getAttributes().getNamedItem(name) != null ? i.getAttributes().getNamedItem(name).getNodeValue() : i.getAttributes().getNamedItem("nameEn") != null ? i.getAttributes().getNamedItem("nameEn").getNodeValue() : "");
									}
								}
								final String icon = String.valueOf(i.getAttributes().getNamedItem("icon").getNodeValue());
								final AchiveTemplate achievement = new AchiveTemplate(achievementId, level, params, achievementCategory, icon, pointsToComplete, fieldType, fame, select);
								
								if (achievementMaxLevel < level)
								{
									achievementMaxLevel = level;
								}
								
								for (Node o = i.getFirstChild(); o != null; o = o.getNextSibling())
								{
									if ("reward".equalsIgnoreCase(o.getNodeName()))
									{
										final int Itemid = Integer.valueOf(o.getAttributes().getNamedItem("id").getNodeValue());
										final long Itemcount = Long.parseLong(o.getAttributes().getNamedItem("count").getNodeValue());
										achievement.addReward(Itemid, Itemcount);
									}
								}
								
								final AchievementCategory lastCategory = _achievementCategories.stream().filter(ctg -> ctg.getCategoryId() == achievementCategory).findAny().orElse(null);
								if (lastCategory != null)
								{
									lastCategory.getAchievements().add(achievement);
								}
								
								if (fieldType.equals("killbyId") && !_achKillById.containsKey(select))
								{
									_achKillById.put(select, achievement);
								}
								else if (fieldType.equals("reflectionById") && !_achRefById.containsKey(select))
								{
									_achRefById.put(select, achievement);
								}
								else if (fieldType.equals("questById") && !_achQuestById.containsKey(select))
								{
									_achQuestById.put(select, achievement);
								}
								else if (fieldType.equals("enchantWeaponByLvl") && !_achEnchantWeaponByLvl.containsKey(achievementId))
								{
									_achEnchantWeaponByLvl.put(select, achievement);
								}
								else if (fieldType.equals("enchantArmorByLvl") && !_achEnchantArmorByLvl.containsKey(achievementId))
								{
									_achEnchantArmorByLvl.put(select, achievement);
								}
								else if (fieldType.equals("enchantJewerlyByLvl") && !_achEnchantJewerlyByLvl.containsKey(achievementId))
								{
									_achEnchantJewerlyByLvl.put(select, achievement);
								}
							}
						}
						_achievementMaxLevels.put(achievementId, achievementMaxLevel);
					}
				}
			}
		}
		catch (final Exception e)
		{
		}
		_log.info("AchievementManager: Loaded " + _achievementCategories.size() + " achievement categories and " + _achievementMaxLevels.size() + " achievements.");
	}
	
	public boolean isActive()
	{
		return _isActive;
	}
	
	public void setIsActive(boolean isActive)
	{
		_isActive = isActive;
	}
	
	public static final AchievementManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final AchievementManager _instance = new AchievementManager();
	}
}