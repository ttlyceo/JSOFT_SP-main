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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import l2e.gameserver.data.DocumentParser;
import l2e.gameserver.model.fishing.Fish;
import l2e.gameserver.model.stats.StatsSet;

public final class FishParser extends DocumentParser
{
	private static final Map<Integer, Fish> _fishNormal = new HashMap<>();
	private static final Map<Integer, Fish> _fishEasy = new HashMap<>();
	private static final Map<Integer, Fish> _fishHard = new HashMap<>();

	protected FishParser()
	{
		load();
	}

	@Override
	public void load()
	{
		_fishEasy.clear();
		_fishNormal.clear();
		_fishHard.clear();
		parseDatapackFile("data/stats/items/fishing/fishes.xml");
		info("Loaded " + (_fishEasy.size() + _fishNormal.size() + _fishHard.size()) + " Fishes.");
	}
	
	@Override
	protected void reloadDocument()
	{
	}
	
	@Override
	protected void parseDocument()
	{
		NamedNodeMap attrs;
		Node att;
		Fish fish;
		StatsSet set;
		for (Node n = getCurrentDocument().getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("fish".equalsIgnoreCase(d.getNodeName()))
					{
						attrs = d.getAttributes();

						set = new StatsSet();
						for (int i = 0; i < attrs.getLength(); i++)
						{
							att = attrs.item(i);
							set.set(att.getNodeName(), att.getNodeValue());
						}
						fish = new Fish(set);
						switch (fish.getFishGrade())
						{
							case 0 :
							{
								_fishEasy.put(fish.getFishId(), fish);
								break;
							}
							case 1 :
							{
								_fishNormal.put(fish.getFishId(), fish);
								break;
							}
							case 2 :
							{
								_fishHard.put(fish.getFishId(), fish);
								break;
							}
						}
					}
				}
			}
		}
	}

	public List<Fish> getFish(int level, int group, int grade)
	{
		final ArrayList<Fish> result = new ArrayList<>();
		Map<Integer, Fish> fish = null;
		switch (grade)
		{
			case 0 :
			{
				fish = _fishEasy;
				break;
			}
			case 1 :
			{
				fish = _fishNormal;
				break;
			}
			case 2 :
			{
				fish = _fishHard;
				break;
			}
			default :
			{
				warn("Unmanaged fish grade!");
				return result;
			}
		}

		for (final Fish f : fish.values())
		{
			if ((f.getFishLevel() != level) || (f.getFishGroup() != group))
			{
				continue;
			}
			result.add(f);
		}

		if (result.isEmpty())
		{
			warn("Cannot find any fish for level: " + level + " group: " + group + " and grade: " + grade + "!");
		}
		return result;
	}

	public static FishParser getInstance()
	{
		return SingletonHolder._instance;
	}

	private static class SingletonHolder
	{
		protected static final FishParser _instance = new FishParser();
	}
}