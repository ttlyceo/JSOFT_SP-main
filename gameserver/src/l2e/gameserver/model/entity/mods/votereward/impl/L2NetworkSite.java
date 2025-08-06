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
import l2e.gameserver.model.entity.mods.votereward.VoteRewardRecord;
import l2e.gameserver.model.entity.mods.votereward.VoteRewardSite;

public class L2NetworkSite extends VoteRewardSite
{
	private final String _serverName;
	private final long _voteDelay;
	
	public L2NetworkSite(MultiValueSet<String> parameters)
	{
		super(parameters);
		
		_serverName = parameters.getString("server_name");
		_voteDelay = TimeUnit.HOURS.toMillis(parameters.getInteger("vote_delay", 12));
	}
	
	@Override
	public boolean isEnabled()
	{
		if (StringUtils.isEmpty(_serverName))
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
			final VoteRewardRecord record = getRecord(player);
			
			final long lastVoteTime = (record.getLastVoteTime() * 1000L);
			
			final long nextVoteTime = lastVoteTime + _voteDelay;
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
			final URL obj = new URL(String.format("https://l2network.eu/index.php?a=in&u=%s&ipc=%s", _serverName, player.getIPAddress()));
			final var con = (HttpURLConnection) obj.openConnection();
			con.setRequestMethod("POST");
			con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			con.setRequestProperty("charset", "utf-8");
			con.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/56.0.2924.87 Safari/537.36");
			con.setDoOutput(true);
			
			final int responseCode = con.getResponseCode();
			if (responseCode == 200)
			{
				final StringBuilder sb = new StringBuilder();
				try (
				    BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream())))
				{
					String inputLine;
					while ((inputLine = in.readLine()) != null)
					{
						sb.append(inputLine);
					}
				}
				return sb.toString().equals("1");
			}
		}
		catch (final Exception e)
		{
			System.out.println("Error getApiResponse L2Network API: " + e);
		}
		return false;
	}
	
	@Override
	public long getVoteDelay()
	{
		return _voteDelay;
	}
}