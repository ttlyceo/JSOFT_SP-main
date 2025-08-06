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
package l2e.gameserver.model.entity.events.model.template;

import java.util.List;
import java.util.Map;

import l2e.commons.time.cron.SchedulingPattern;
import l2e.gameserver.Config;
import l2e.gameserver.model.stats.StatsSet;

/**
 * Created by LordWinter 13.07.2020
 */
public class WorldEventTemplate
{
	private final int _id;
	private final boolean _activate;
	private final SchedulingPattern _startTimePattern;
	private final SchedulingPattern _stopTimePattern;
	private final boolean _isNonStop;
	private final Map<Integer, List<WorldEventReward>> _variantRequests;
	private final Map<Integer, List<WorldEventReward>> _variantRewards;
	private final Map<Integer, List<WorldEventReward>> _variantRndRewards;
	private final List<WorldEventDrop> _dropList;
	private final List<WorldEventSpawn> _spawnList;
	private final List<WorldEventLocation> _locations;
	private final List<WorldEventTerritory> _territories;
	private final StatsSet _params;
	
	public WorldEventTemplate(int id, boolean activate, SchedulingPattern startTimePattern, SchedulingPattern stopTimePattern, boolean isNonStop, List<WorldEventDrop> dropList, Map<Integer, List<WorldEventReward>> variantRequests, Map<Integer, List<WorldEventReward>> variantRewards, Map<Integer, List<WorldEventReward>> variantRndRewards, List<WorldEventSpawn> spawnList, List<WorldEventLocation> locations, List<WorldEventTerritory> territories, StatsSet params)
	{
		_id = id;
		_activate = activate;
		_startTimePattern = startTimePattern;
		_stopTimePattern = stopTimePattern;
		_isNonStop = isNonStop;
		_dropList = dropList;
		_variantRequests = variantRequests;
		_variantRewards = variantRewards;
		_variantRndRewards = variantRndRewards;
		_spawnList = spawnList;
		_locations = locations;
		_territories = territories;
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
	
	public SchedulingPattern getStartTimePattern()
	{
		return _startTimePattern;
	}
	
	public SchedulingPattern getStopTimePattern()
	{
		return _stopTimePattern;
	}
	
	public boolean isNonStop()
	{
		return _isNonStop;
	}
	
	public List<WorldEventDrop> getDropList()
	{
		return _dropList;
	}
	
	public Map<Integer, List<WorldEventReward>> getVariantRequests()
	{
		return _variantRequests;
	}
	
	public Map<Integer, List<WorldEventReward>> getVariantRewards()
	{
		return _variantRewards;
	}
	
	public Map<Integer, List<WorldEventReward>> getVariantRandomRewards()
	{
		return _variantRndRewards;
	}
	
	public List<WorldEventSpawn> getSpawnList()
	{
		return _spawnList;
	}
	
	public List<WorldEventLocation> getLocations()
	{
		return _locations;
	}
	
	public List<WorldEventTerritory> getTerritories()
	{
		return _territories;
	}
	
	public boolean isActivated()
	{
		return _activate;
	}
	
	public StatsSet getParams()
	{
		return _params;
	}
}