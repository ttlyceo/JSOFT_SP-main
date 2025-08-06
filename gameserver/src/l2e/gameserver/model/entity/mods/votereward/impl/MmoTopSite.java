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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.napile.primitive.pair.IntIntPair;
import org.napile.primitive.pair.impl.IntIntPairImpl;

import l2e.commons.apache.StringUtils;
import l2e.commons.collections.MultiValueSet;
import l2e.gameserver.model.actor.Player;

public class MmoTopSite extends AbstractAutoRewardSite
{
	private static final DateFormat MMOTOP_SERVER_DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
	static
	{
		MMOTOP_SERVER_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("Europe/Moscow"));
	}
	
	private final Pattern VOTES_PATTERN = Pattern.compile("(\\d+)\t+(\\d{2}\\.\\d{2}\\.\\d{4} +\\d{2}:\\d{2}:\\d{2})\t+(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})\t+(\\S+)\t+(\\d+)\\s+", Pattern.DOTALL | Pattern.MULTILINE | Pattern.UNICODE_CASE);
	
	private final String serverVotesLink;
	private final String identifierType;
	
	public MmoTopSite(MultiValueSet<String> parameters)
	{
		super(parameters);
		serverVotesLink = parameters.getString("votes_link");
		identifierType = parameters.getString("identifier_type");
	}
	
	@Override
	public boolean isEnabled()
	{
		if (StringUtils.isEmpty(serverVotesLink))
		{
			return false;
		}
		return super.isEnabled();
	}
	
	@Override
	protected String getIdentifier(Player player)
	{
		if (identifierType.equalsIgnoreCase("ip"))
		{
			return player.getIPAddress();
		}
		return player.getName(null);
	}
	
	@Override
	protected void parseVotes(Map<String, List<IntIntPair>> votesCache)
	{
		final String serverResponse = getApiResponse(String.format("https://mmotop.ru/votes/%s", serverVotesLink), true);
		if (serverResponse != null)
		{
			final Matcher m = VOTES_PATTERN.matcher(serverResponse);
			while (m.find())
			{
				Date voteDate;
				try
				{
					voteDate = MMOTOP_SERVER_DATE_FORMAT.parse(m.group(2));
				}
				catch (final Exception e)
				{
					error(String.format("Cannot parse voting date: %s", m.group(2)), e);
					continue;
				}
				
				String identifier;
				if (identifierType.equalsIgnoreCase("ip"))
				{
					identifier = m.group(3);
				}
				else
				{
					identifier = m.group(4);
				}
				
				var count = Integer.parseInt(m.group(5));
				switch (count)
				{
					case 2 :
						count = 10;
						break;
					case 3 :
						count = 50;
						break;
					case 4 :
						count = 100;
						break;
					case 5 :
						count = 500;
						break;
				}
				final List<IntIntPair> votes = votesCache.computeIfAbsent(identifier.toLowerCase(), list -> new ArrayList<>());
				votes.add(new IntIntPairImpl((int) (voteDate.getTime() / 1000), count));
			}
		}
	}
}