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

import java.util.Arrays;

import l2e.gameserver.Config;
import l2e.gameserver.data.DocumentParser;

public final class ExpPercentLostParser extends DocumentParser
{
	private final int _maxlevel = ExperienceParser.getInstance().getMaxLevel();
	private final double[] _expPercentLost = new double[_maxlevel + 1];
	
	protected ExpPercentLostParser()
	{
		load();
	}
	
	@Override
	public void load()
	{
		Arrays.fill(_expPercentLost, 1.);
		parseDatapackFile("data/stats/chars/expPercentLost.xml");
		info("Loaded " + _expPercentLost.length + " levels.");
	}
	
	@Override
	protected void reloadDocument()
	{
	}
	
	@Override
	public void parseDocument()
	{
		int level = 0;
		for (var n = getCurrentDocument().getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				for (var d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("set".equalsIgnoreCase(d.getNodeName()))
					{
						final var attrs = d.getAttributes();
						level = parseInteger(attrs, "level");
						if (level > Config.PLAYER_MAXIMUM_LEVEL)
						{
							break;
						}
						_expPercentLost[level] = parseDouble(attrs, "val");
					}
				}
			}
		}
	}
	
	public double getExpPercent(final int level)
	{
		return level > _maxlevel ? _expPercentLost[_maxlevel] : _expPercentLost[level];
	}
	
	public static ExpPercentLostParser getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final ExpPercentLostParser _instance = new ExpPercentLostParser();
	}
}
