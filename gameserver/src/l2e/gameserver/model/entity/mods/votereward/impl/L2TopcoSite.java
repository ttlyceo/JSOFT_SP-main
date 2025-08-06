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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import l2e.commons.apache.StringUtils;
import l2e.commons.collections.MultiValueSet;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.entity.mods.votereward.VoteRewardSite;

public class L2TopcoSite extends VoteRewardSite
{
	private final String _serverId;
	private final long _voteDelay;
	
	public L2TopcoSite(MultiValueSet<String> parameters)
	{
		super(parameters);
		
		_serverId = parameters.getString("server_id");
		_voteDelay = TimeUnit.HOURS.toMillis(parameters.getInteger("vote_delay", 12));
	}
	
	@Override
	public boolean isEnabled()
	{
		if (StringUtils.isEmpty(_serverId))
		{
			return false;
		}
		return super.isEnabled();
	}
	
	@Override
	public boolean tryGiveRewards(Player player)
	{
		if (!getApiResponse(player))
		{
			return false;
		}
		
		getLock().lock();
		try
		{
			final var record = getRecord(player);
			
			final var lastVoteTime = (record.getLastVoteTime() * 1000L);
			
			final var nextVoteTime = lastVoteTime + _voteDelay;
			if (System.currentTimeMillis() < nextVoteTime)
			{
				return false;
			}
			
			record.onReceiveReward(1, System.currentTimeMillis());
			giveRewards(player, 1);
			return true;
		}
		finally
		{
			getLock().unlock();
		}
	}
	
	private boolean getApiResponse(Player player)
	{
		try
		{
			final var url = new URL(String.format("https://l2top.co/reward/VoteCheck.php?id=%s&ip=%s", _serverId, player.getIPAddress()));
			final var con = (HttpURLConnection) url.openConnection();
			con.addRequestProperty("Host", url.getHost());
			con.addRequestProperty("Accept", "*/*");
			con.addRequestProperty("Connection", "close");
			con.addRequestProperty("User-Agent", "Eternity");
			con.setConnectTimeout(5000);
			
			final var sb = new StringBuilder();
			try (var in = new BufferedReader(new InputStreamReader(con.getInputStream())))
			{
				String inputLine;
				while ((inputLine = in.readLine()) != null)
				{
					sb.append(inputLine);
				}
			}
			return !sb.toString().equals("FALSE");
		}
		catch (final Exception e)
		{
			warn("Check request failed", e);
		}
		return false;
	}
	
	@Override
	public long getVoteDelay()
	{
		return _voteDelay;
	}
}