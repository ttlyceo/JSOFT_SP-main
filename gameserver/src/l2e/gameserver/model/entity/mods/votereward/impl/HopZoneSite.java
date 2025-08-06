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
package l2e.gameserver.model.entity.mods.votereward.impl;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import l2e.commons.apache.StringUtils;
import l2e.commons.collections.MultiValueSet;
import l2e.commons.gson.JsonElement;
import l2e.commons.gson.JsonObject;
import l2e.commons.gson.JsonParser;
import l2e.commons.gson.JsonPrimitive;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.entity.mods.votereward.VoteRewardRecord;
import l2e.gameserver.model.entity.mods.votereward.VoteRewardSite;

public class HopZoneSite extends VoteRewardSite
{
	private static final DateFormat HOP_ZONE_SERVER_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	static
	{
		HOP_ZONE_SERVER_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("Europe/Bucharest"));
	}

	private final String _apiKey;
	private final long _voteDelay;

	public HopZoneSite(MultiValueSet<String> parameters)
	{
		super(parameters);
		_apiKey = parameters.getString("api_key");
		_voteDelay = TimeUnit.HOURS.toMillis(parameters.getInteger("vote_delay", 12));
	}

	@Override
	public boolean isEnabled()
	{
		if (StringUtils.isEmpty(_apiKey))
		{
			return false;
		}
		return super.isEnabled();
	}

	@Override
	public boolean tryGiveRewards(Player player)
	{
		final String serverResponse = getApiResponse(String.format("https://api.hopzone.net/lineage2/vote?token=%s&ip_address=%s", _apiKey, player.getIPAddress()), false);
		if (serverResponse == null)
		{
			return false;
		}

		final JsonElement jelement = new JsonParser().parse(serverResponse);
		final JsonObject topObject = jelement.getAsJsonObject();

		final JsonPrimitive statusCodePrimitive = topObject.getAsJsonPrimitive("status_code");
		if (statusCodePrimitive.getAsInt() != 200)
		{
			return false;
		}

		final JsonPrimitive votedPrimitive = topObject.getAsJsonPrimitive("voted");
		if (!votedPrimitive.getAsBoolean())
		{
			return false;
		}

		final JsonPrimitive voteTimePrimitive = topObject.getAsJsonPrimitive("voteTime");
		long voteTime;
		try
		{
			final Date voteDate = HOP_ZONE_SERVER_DATE_FORMAT.parse(voteTimePrimitive.getAsString());
			voteTime = voteDate.getTime();
		}
		catch (final ParseException e)
		{
			error("Cannot parse voteDate from HopZone.net!", e);
			return false;
		}

		getLock().lock();
		try
		{
			final VoteRewardRecord record = getRecord(player);

			final long lastVoteTime = (record.getLastVoteTime() * 1000L);
			if (lastVoteTime >= voteTime)
			{
				return false;
			}

			final long nextVoteTime = lastVoteTime + _voteDelay;
			if (System.currentTimeMillis() < nextVoteTime)
			{
				return false;
			}

			record.onReceiveReward(1, voteTime);
			giveRewards(player, 1);
			return true;
		}
		finally
		{
			getLock().unlock();
		}
	}
	
	@Override
	public long getVoteDelay()
	{
		return _voteDelay;
	}
}