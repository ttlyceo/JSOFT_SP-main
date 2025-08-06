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
package l2e.gameserver.model.actor.templates.player;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import l2e.gameserver.Config;
import l2e.gameserver.data.htm.HtmCache;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.entity.events.custom.achievements.AchievementManager;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.reward.RewardItemResult;
import l2e.gameserver.model.stats.StatsSet;
import l2e.gameserver.model.strings.server.ServerStorage;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.CreatureSay;
import l2e.gameserver.network.serverpackets.MagicSkillUse;

public class AchiveTemplate
{
	private final int _id;
	private final int _level;
	private final StatsSet _params;
	private final int _categoryId;
	private final String _icon;
	private final long _pointsToComplete;
	private final String _achievementType;
	private final int _fame;
	private final int _select;
	private final List<RewardItemResult> _rewards;
	
	public AchiveTemplate(int id, int level, StatsSet params, int categoryId, String icon, long pointsToComplete, String achievementType, int fame, int select)
	{
		_id = id;
		_level = level;
		_params = params;
		_categoryId = categoryId;
		_icon = icon;
		_pointsToComplete = pointsToComplete;
		_achievementType = achievementType;
		_fame = fame;
		_select = select;
		_rewards = new LinkedList<>();
	}

	public boolean isDone(long playerPoints)
	{
		return playerPoints >= _pointsToComplete;
	}

	public String getNotDoneHtml(Player pl, int playerPoints)
	{
		String oneAchievement = HtmCache.getInstance().getHtm(pl, pl.getLang(), "data/html/mods/achievements/oneAchievement.htm");

		int greenbar = (int) (24 * (playerPoints * 100 / _pointsToComplete) / 100);
		greenbar = Math.max(greenbar, 0);

		if (greenbar > 24)
		{
			return "";
		}

		oneAchievement = oneAchievement.replaceFirst("%fame%", "" + _fame);
		oneAchievement = oneAchievement.replaceAll("%bar1%", "" + greenbar);
		oneAchievement = oneAchievement.replaceAll("%bar2%", "" + (24 - greenbar));

		oneAchievement = oneAchievement.replaceFirst("%cap1%", greenbar > 0 ? "Gauge_DF_Food_Left" : "Gauge_DF_Exp_bg_Left");
		oneAchievement = oneAchievement.replaceFirst("%cap2%", "Gauge_DF_Exp_bg_Right");

		oneAchievement = oneAchievement.replaceFirst("%desc%", getDescr(pl.getLang()).replaceAll("%need%", "" + Math.max(0, _pointsToComplete - playerPoints)));

		oneAchievement = oneAchievement.replaceFirst("%bg%", _id % 2 == 0 ? "090908" : "0f100f");
		oneAchievement = oneAchievement.replaceFirst("%icon%", _icon);
		oneAchievement = oneAchievement.replaceFirst("%name%", getName(pl.getLang()) + (_level > 1 ? (" " + ServerStorage.getInstance().getString(pl.getLang(), "Achievement.LEVEL") + " " + _level) : ""));
		return oneAchievement;
	}
	
	public String getDoneHtml(Player player)
	{
		String oneAchievement = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/achievements/oneAchievement.htm");

		oneAchievement = oneAchievement.replaceFirst("%fame%", "" + _fame);
		oneAchievement = oneAchievement.replaceAll("%bar1%", "24");
		oneAchievement = oneAchievement.replaceAll("%bar2%", "0");

		oneAchievement = oneAchievement.replaceFirst("%cap1%", "Gauge_DF_Food_Left");
		oneAchievement = oneAchievement.replaceFirst("%cap2%", "Gauge_DF_Food_Right");

		oneAchievement = oneAchievement.replaceFirst("%desc%", ServerStorage.getInstance().getString(player.getLang(), "Achievement.DONE"));

		oneAchievement = oneAchievement.replaceFirst("%bg%", _id % 2 == 0 ? "090908" : "0f100f");
		oneAchievement = oneAchievement.replaceFirst("%icon%", _icon);
		oneAchievement = oneAchievement.replaceFirst("%name%", getName(player.getLang()) + (_level > 1 ? (" " + ServerStorage.getInstance().getString(player.getLang(), "Achievement.LEVEL") + " " + _level) : ""));
		return oneAchievement;
	}
	
	public void reward(Player player)
	{
		synchronized (player.getAchievements())
		{
			player.sendPacket(new CreatureSay(player.getObjectId(), Say2.BATTLEFIELD, getName(player.getLang()), ServerStorage.getInstance().getString(player.getLang(), "Achievement.COMPLETED")));
			player.getAchievements().put(getId(), getLevel());
			player.setFame(player.getFame() + getFame());
			if (AchievementManager.getInstance().getMaxLevel(getId()) > getLevel())
			{
				player.getCounters().refreshAchievementInfo(getId());
			}
			for (final ItemInstance item : getRewards().stream().map(r -> r.createItem()).collect(Collectors.toList()))
			{
				player.addItem("Achievement", item, player, true);
			}
			player.sendUserInfo();
			player.broadcastPacket(new MagicSkillUse(player, player, 2528, 1, 0, 500));
		}
	}

	public List<RewardItemResult> getRewards()
	{
		return _rewards;
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

	public int getId()
	{
		return _id;
	}

	public int getLevel()
	{
		return _level;
	}

	public void addReward(int itemId, long itemCount)
	{
		_rewards.add(new RewardItemResult(itemId, itemCount));
	}

	public String getType()
	{
		return _achievementType;
	}

	public long getPointsToComplete()
	{
		return _pointsToComplete;
	}

	public int getCategoryId()
	{
		return _categoryId;
	}

	public String getIcon()
	{
		return _icon;
	}

	public int getFame()
	{
		return _fame;
	}
	
	public boolean isSelected()
	{
		return _select > 0;
	}
	
	public int getSelect()
	{
		return _select;
	}
}