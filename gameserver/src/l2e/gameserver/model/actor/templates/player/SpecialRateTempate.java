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

import java.util.Map;

import l2e.commons.time.cron.SchedulingPattern;
import l2e.commons.time.cron.SchedulingPattern.InvalidPatternException;
import l2e.gameserver.model.base.BonusType;
import l2e.gameserver.model.stats.StatsSet;
import l2e.gameserver.model.strings.server.ServerMessage;

/**
 * Created by LordWinter
 */
public class SpecialRateTempate
{
	private final int _id;
	private final StatsSet _params;
	private final Map<BonusType, Double> _bonusList;
	private final ServerMessage _startAnnounce;
	private final ServerMessage _stopAnnounce;
	
	public SpecialRateTempate(int id, StatsSet params, Map<BonusType, Double> bonusList, ServerMessage startAnnounce, ServerMessage stopAnnounce)
	{
		_id = id;
		_params = params;
		_bonusList = bonusList;
		_startAnnounce = startAnnounce;
		_stopAnnounce = stopAnnounce;
	}
	
	public int getId()
	{
		return _id;
	}
	
	public Map<BonusType, Double> getBonusList()
	{
		return _bonusList;
	}
	
	public long getStartTime()
	{
		final var startTime = 0L;
		SchedulingPattern cronTime;
		try
		{
			cronTime = new SchedulingPattern(_params.getString("startPattern"));
		}
		catch (final InvalidPatternException e)
		{
			return startTime;
		}
		return cronTime.next(System.currentTimeMillis());
	}
	
	public long getDisableTime()
	{
		final var stopTime = 0L;
		SchedulingPattern cronTime;
		try
		{
			cronTime = new SchedulingPattern(_params.getString("stopPattern"));
		}
		catch (final InvalidPatternException e)
		{
			return stopTime;
		}
		return cronTime.next(System.currentTimeMillis());
	}
	
	public boolean isAnnounceEnable()
	{
		return _params.getBool("allowAnnounce");
	}
	
	public ServerMessage getStartAnnounce()
	{
		return _startAnnounce;
	}
	
	public ServerMessage getStopAnnounce()
	{
		return _stopAnnounce;
	}
}