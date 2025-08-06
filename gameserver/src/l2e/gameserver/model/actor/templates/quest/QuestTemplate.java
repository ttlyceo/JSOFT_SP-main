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
package l2e.gameserver.model.actor.templates.quest;

import java.util.List;
import java.util.Map;

import l2e.gameserver.Config;
import l2e.gameserver.model.stats.StatsSet;

public class QuestTemplate
{
	private final int _id;
	private final int _minLvl;
	private final int _maxLvl;
	private final QuestExperience _experience;
	private final List<QuestRewardItem> _rewards;
	private final Map<Integer, List<QuestRewardItem>> _variantRewards;
	private final Map<Integer, List<QuestDropItem>> _dropList;
	private final boolean _rateable;
	private final StatsSet _params;
	
	public QuestTemplate(int id, int minLvl, int maxLvl, Map<Integer, List<QuestDropItem>> dropList, QuestExperience experience, List<QuestRewardItem> rewards, Map<Integer, List<QuestRewardItem>> variantRewards, boolean rateable, StatsSet params)
	{
		_id = id;
		_minLvl = minLvl;
		_maxLvl = maxLvl;
		_dropList = dropList;
		_experience = experience;
		_rewards = rewards;
		_variantRewards = variantRewards;
		_rateable = rateable;
		_params = params;
	}
	
	public int getId()
	{
		return _id;
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
	
	public int getMinLvl()
	{
		return _minLvl;
	}
	
	public int getMaxLvl()
	{
		return _maxLvl;
	}
	
	public Map<Integer, List<QuestDropItem>> getDropList()
	{
		return _dropList;
	}
	
	public QuestExperience getExperienceRewards()
	{
		return _experience;
	}
	
	public List<QuestRewardItem> getRewards()
	{
		return _rewards;
	}
	
	public Map<Integer, List<QuestRewardItem>> getVariantRewards()
	{
		return _variantRewards;
	}
	
	public boolean isRateable()
	{
		return _rateable;
	}
	
	public StatsSet getParams()
	{
		return _params;
	}
}