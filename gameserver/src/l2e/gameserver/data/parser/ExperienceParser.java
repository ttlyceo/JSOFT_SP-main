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
package l2e.gameserver.data.parser;

import java.util.HashMap;
import java.util.Map;

import l2e.gameserver.Config;
import l2e.gameserver.data.DocumentParser;

public final class ExperienceParser extends DocumentParser
{
	private final Map<Integer, Long> _expTable = new HashMap<>();
	
	private int MAX_LEVEL;
	private int MAX_PET_LEVEL;
	
	protected ExperienceParser()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_expTable.clear();
		parseDatapackFile("data/stats/chars/experience.xml");
		info("Loaded " + _expTable.size() + " levels.");
		if (Config.DEBUG)
		{
			info("Max Player Level is: " + (MAX_LEVEL - 1));
			info("Max Pet Level is: " + (MAX_PET_LEVEL - 1));
		}
	}
	
	@Override
	protected void reloadDocument()
	{
	}
	
	@Override
	protected void parseDocument()
	{
		final var table = getCurrentDocument().getFirstChild();
		final var tableAttr = table.getAttributes();
		
		MAX_LEVEL = Integer.parseInt(tableAttr.getNamedItem("maxLevel").getNodeValue()) + 1;
		MAX_PET_LEVEL = Integer.parseInt(tableAttr.getNamedItem("maxPetLevel").getNodeValue()) + 1;
		if (MAX_LEVEL > Config.PLAYER_MAXIMUM_LEVEL)
		{
			MAX_LEVEL = Config.PLAYER_MAXIMUM_LEVEL;
		}
		
		if (MAX_PET_LEVEL > MAX_LEVEL)
		{
			MAX_PET_LEVEL = MAX_LEVEL;
		}
		
		int level = 0;
		for (var n = table.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("experience".equals(n.getNodeName()))
			{
				final var attrs = n.getAttributes();
				level = parseInteger(attrs, "level");
				if (level > Config.PLAYER_MAXIMUM_LEVEL)
				{
					break;
				}
				_expTable.put(level, parseLong(attrs, "tolevel"));
			}
		}
	}
	
	public long getExpForLevel(int level)
	{
		if (level > Config.PLAYER_MAXIMUM_LEVEL)
		{
			return _expTable.get(Config.PLAYER_MAXIMUM_LEVEL);
		}
		return _expTable.get(level);
	}
	
	public int getMaxLevel()
	{
		return MAX_LEVEL;
	}
	
	public int getMaxPetLevel()
	{
		return MAX_PET_LEVEL;
	}

	public double penaltyModifier(long count, double percents)
	{
		return Math.max(1. - count * percents / 100, 0);
	}

	public static ExperienceParser getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final ExperienceParser _instance = new ExperienceParser();
	}
}