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

import java.util.ArrayList;
import java.util.List;

import l2e.gameserver.Config;
import l2e.gameserver.data.htm.HtmCache;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.player.AchiveTemplate;
import l2e.gameserver.model.stats.StatsSet;

public class AchievementCategory
{
	private static final int BAR_MAX = 24;
	private final List<AchiveTemplate> _achievements = new ArrayList<>();
	private final int _categoryId;
	private final StatsSet _params;
	private final String _icon;
	
	public AchievementCategory(int categoryId, StatsSet params, String categoryIcon)
	{
		_categoryId = categoryId;
		_params = params;
		_icon = categoryIcon;
	}
	
	public String getHtml(Player player)
	{
		return getHtml(player, AchievementManager.getAchievementLevelSum(player, getCategoryId()));
	}

	public String getHtml(Player player, int totalPlayerLevel)
	{
		int greenbar = 0;

		if (totalPlayerLevel > 0)
		{
			greenbar = BAR_MAX * (totalPlayerLevel * 100 / _achievements.size()) / 100;
			greenbar = Math.min(greenbar, BAR_MAX);
		}
		
		String temp = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/achievements/achievementsCat.htm");
		
		temp = temp.replaceFirst("%bg%", getCategoryId() % 2 == 0 ? "090908" : "0f100f");
		temp = temp.replaceFirst("%desc%", getDescr(player.getLang()));
		temp = temp.replaceFirst("%icon%", getIcon());
		temp = temp.replaceFirst("%name%", getName(player.getLang()));
		temp = temp.replaceFirst("%id%", "" + getCategoryId());

		temp = temp.replaceFirst("%caps1%", greenbar > 0 ? "Gauge_DF_Food_Left" : "Gauge_DF_Exp_bg_Left");
		temp = temp.replaceFirst("%caps2%", greenbar >= 24 ? "Gauge_DF_Food_Right" : "Gauge_DF_Exp_bg_Right");

		temp = temp.replaceAll("%bar1%", "" + greenbar);
		temp = temp.replaceAll("%bar2%", "" + (24 - greenbar));
		return temp;
	}

	public int getCategoryId()
	{
		return _categoryId;
	}

	public List<AchiveTemplate> getAchievements()
	{
		return _achievements;
	}

	public String getIcon()
	{
		return _icon;
	}

	public String getName(String lang)
	{
		try
		{
			return _params.getString(lang != null ? "name" + lang.substring(0, 1).toUpperCase() + lang.substring(1) : "name" + Config.MULTILANG_DEFAULT.substring(0, 1).toUpperCase() + Config.MULTILANG_DEFAULT.substring(1));
		}
		catch (final IllegalArgumentException e)
		{
			return "";
		}
	}
	
	public String getDescr(String lang)
	{
		try
		{
			return _params.getString(lang != null ? "desc" + lang.substring(0, 1).toUpperCase() + lang.substring(1) : "desc" + Config.MULTILANG_DEFAULT.substring(0, 1).toUpperCase() + Config.MULTILANG_DEFAULT.substring(1));
		}
		catch (final IllegalArgumentException e)
		{
			return "";
		}
	}
}