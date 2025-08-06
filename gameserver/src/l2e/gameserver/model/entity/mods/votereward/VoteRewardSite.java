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
package l2e.gameserver.model.entity.mods.votereward;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.napile.primitive.maps.impl.HashIntLongMap;

import l2e.commons.collections.MultiValueSet;
import l2e.commons.log.LoggerObject;
import l2e.gameserver.Config;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.data.holder.VoteRewardHolder;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.reward.RewardItem;
import l2e.gameserver.model.reward.RewardList;
import l2e.gameserver.model.strings.server.ServerMessage;

public abstract class VoteRewardSite extends LoggerObject implements Runnable
{
	private final MultiValueSet<String> _params;
	private final String _name;
	private final boolean _enabled;
	private final int _runDelay;
	private final List<RewardList> _rewardLists = new ArrayList<>();
	private final Map<String, VoteRewardRecord> _records = new ConcurrentHashMap<>();
	private final Map<Integer, Long> _attempts = new ConcurrentHashMap<>();
	private final Lock _lock = new ReentrantLock();
	private boolean _isHwidCheck = false;
	protected long _attemptDelay;
	private final String _voteLink;
	
	public VoteRewardSite(MultiValueSet<String> parameters)
	{
		_name = parameters.getString("name");
		_enabled = parameters.getBool("enabled", false);
		_runDelay = parameters.getInteger("run_delay", 0);
		_isHwidCheck = parameters.getBool("isHwidCheck", false);
		_attemptDelay = parameters.getLong("attemptDelay", 0L);
		_voteLink = parameters.getString("voteLink", null);
		_params = parameters;
	}
	
	@Override
	public void run()
	{
		throw new UnsupportedOperationException(getClass().getName() + " not implemented run");
	}
	
	public final String getName()
	{
		return _name;
	}
	
	public boolean isEnabled()
	{
		return _enabled;
	}
	
	public final void addRewardList(RewardList rewardList)
	{
		_rewardLists.add(rewardList);
	}
	
	public final VoteRewardRecord getRecord(Player player)
	{
		final var identifier = _isHwidCheck ? player.getHWID() : player.getIPAddress();
		var record = _records.get(identifier);
		if (record == null)
		{
			record = new VoteRewardRecord(getName(), identifier, 0, -1);
			record.save();
			_records.put(record.getIdentifier(), record);
		}
		return record;
	}
	
	public final boolean isValidAttempt(Player player)
	{
		final var attempt = _attempts.containsKey(player.getObjectId());
		if (!attempt || _attempts.get(player.getObjectId()) < System.currentTimeMillis())
		{
			_attempts.put(player.getObjectId(), (System.currentTimeMillis() + _attemptDelay));
			return true;
		}
		return false;
	}
	
	public final Lock getLock()
	{
		return _lock;
	}
	
	public void init()
	{
		VoteRewardHolder.getInstance().restore(_records, getName());
		if (_runDelay > 0 && isEnabled())
		{
			ThreadPoolManager.getInstance().scheduleAtFixedRate(this, _runDelay, _runDelay * 1000L);
		}
	}
	
	public boolean tryGiveRewards(Player player)
	{
		return false;
	}
	
	protected void giveRewards(Player player, int count)
	{
		final List<RewardItem> rolledItems = new ArrayList<>();
		for (final var rewardList : _rewardLists)
		{
			for (int i = 0; i < count; i++)
			{
				rolledItems.addAll(rewardList.roll(player));
			}
		}
		
		if (rolledItems.isEmpty())
		{
			player.sendMessage(new ServerMessage("VoteReward.REWARD_NOT_RECEIVED." + getName(), player.getLang()).toString());
			return;
		}
		
		player.sendMessage(new ServerMessage("VoteReward.REWARD_RECEIVED." + getName(), player.getLang()).toString());
		
		final var rewards = new HashIntLongMap();
		for (final var rewardItem : rolledItems)
		{
			rewards.put(rewardItem._itemId, rewards.get(rewardItem._itemId) + rewardItem._count);
		}
		
		for (final var pair : rewards.entrySet())
		{
			player.addItem("VoteReward", pair.getKey(), pair.getValue(), player, true);
		}
	}
	
	public long getVoteDelay()
	{
		return 0;
	}
	
	protected String getApiResponse(String endpoint, boolean isUTF8)
	{
		HttpURLConnection connection = null;
		try
		{
			final var stringBuilder = new StringBuilder();
			final var url = new URL(endpoint);
			connection = (HttpURLConnection) url.openConnection();
			connection.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.106 Safari/537.36");
			connection.setRequestMethod("GET");
			connection.setReadTimeout(5000);
			connection.connect();
			
			final int responseCode = connection.getResponseCode();
			if (responseCode != 200)
			{
				warn("Problem in VoteApiService: getApiResponse LINK[" + endpoint + "]");
				if (Config.DEBUG)
				{
					warn("VoteApiService::getApiResponse returned error CODE[" + responseCode + "] LINK[" + endpoint + "]");
				}
				return null;
			}
			
			final var encoding = isUTF8 ? new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8) : new InputStreamReader(connection.getInputStream(), "cp1251");
			try (var reader = new BufferedReader(encoding))
			{
				String line;
				while ((line = reader.readLine()) != null)
				{
					stringBuilder.append(line).append("\n");
				}
			}
			return stringBuilder.toString();
		}
		catch (final Exception e)
		{
			warn("Problem in VoteApiService: getApiResponse LINK[" + endpoint + "]");
			if (Config.DEBUG)
			{
				warn("Something went wrong in VoteApiService::getApiResponse LINK[" + endpoint + "]", e);
			}
		}
		finally
		{
			if (connection != null)
			{
				connection.disconnect();
			}
		}
		return null;
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
	
	public String getVoteLink()
	{
		return _voteLink;
	}
	
	public long getDelay()
	{
		return _attemptDelay;
	}
}
