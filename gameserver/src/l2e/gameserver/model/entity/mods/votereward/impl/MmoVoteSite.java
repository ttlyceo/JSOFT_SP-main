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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.napile.primitive.pair.IntIntPair;
import org.napile.primitive.pair.impl.IntIntPairImpl;

import l2e.commons.apache.StringUtils;
import l2e.commons.collections.MultiValueSet;
import l2e.gameserver.model.actor.Player;

public class MmoVoteSite extends AbstractAutoRewardSite
{
	private static final DateFormat MMOVOTE_SERVER_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss zzz");
	
	private final Pattern VOTES_PATTERN = Pattern.compile("(\\d+)\t+(\\d{4}-\\d{2}-\\d{2} +\\d{2}:\\d{2}:\\d{2} +([a-zA-Z]{3}(-\\s{1,2})?))\t+(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})\t+(\\S+)\t+(\\d+)\\s+", Pattern.DOTALL | Pattern.MULTILINE | Pattern.UNICODE_CASE);
	
	private final String serverVotesLink;
	private final String identifierType;
	
	public MmoVoteSite(MultiValueSet<String> parameters)
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
		final String serverResponse = getApiResponse(String.format("http://stat.mmovote.ru/ru/stat/%s", serverVotesLink), false);
		if (serverResponse != null)
		{
			final Matcher m = VOTES_PATTERN.matcher(serverResponse);
			while (m.find())
			{
				Date voteDate;
				try
				{
					voteDate = MMOVOTE_SERVER_DATE_FORMAT.parse(m.group(2));
				}
				catch (final Exception e)
				{
					error(String.format("Cannot parse voting date: %s", m.group(2)), e);
					continue;
				}
				
				String identifier;
				if (identifierType.equalsIgnoreCase("ip"))
				{
					identifier = m.group(5);
				}
				else
				{
					identifier = m.group(6);
				}
				
				final List<IntIntPair> votes = votesCache.computeIfAbsent(identifier.toLowerCase(), list -> new ArrayList<>());
				votes.add(new IntIntPairImpl((int) (voteDate.getTime() / 1000), Integer.parseInt(m.group(7))));
			}
		}
	}
}