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

import org.w3c.dom.Node;

import l2e.gameserver.data.DocumentParser;
import l2e.gameserver.model.PcCondOverride;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.stats.Stats;

/**
 * Created by LordWinter 23.10.2022
 */
public class LimitStatParser extends DocumentParser
{
	private final Map<LimitType, Map<Integer, Map<Stats, Double>>> _statsList = new HashMap<>();
	private final Map<LimitType, Map<Integer, Map<Stats, Double>>> _olyStatsList = new HashMap<>();
	
	public enum LimitType
	{
		PLAYER, SUMMON
	}
	
	public LimitStatParser()
	{
		load();
	}
	
	@Override
	public synchronized void load()
	{
		_statsList.clear();
		_olyStatsList.clear();
		parseDatapackFile("data/stats/chars/statLimit.xml");
		
		info("Loaded: " + _statsList.size() + " general limit size.");
		info("Loaded: " + _olyStatsList.size() + " olympiad limit size.");
	}
	
	@Override
	protected void reloadDocument()
	{
	}

	@Override
	protected void parseDocument()
	{
		for (var n = getCurrentDocument().getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				for (var cat = n.getFirstChild(); cat != null; cat = cat.getNextSibling())
				{
					if ("olympiad".equalsIgnoreCase(cat.getNodeName()))
					{
						for (var type = cat.getFirstChild(); type != null; type = type.getNextSibling())
						{
							if ("player".equalsIgnoreCase(type.getNodeName()))
							{
								final Map<Integer, Map<Stats, Double>> classLimit = new HashMap<>();
								for (var cs = type.getFirstChild(); cs != null; cs = cs.getNextSibling())
								{
									if ("class".equalsIgnoreCase(cs.getNodeName()))
									{
										final int id = Integer.parseInt(cs.getAttributes().getNamedItem("id").getNodeValue());
										final Map<Stats, Double> statsList = new HashMap<>();
										for (Node st = cs.getFirstChild(); st != null; st = st.getNextSibling())
										{
											if ("set".equalsIgnoreCase(st.getNodeName()))
											{
												final Stats tp = Stats.valueOf(st.getAttributes().getNamedItem("name").getNodeValue());
												final double value = Double.parseDouble(st.getAttributes().getNamedItem("value").getNodeValue());
												statsList.put(tp, value);
											}
										}
										classLimit.put(id, statsList);
									}
								}
								_olyStatsList.put(LimitType.PLAYER, classLimit);
							}
							else if ("summon".equalsIgnoreCase(type.getNodeName()))
							{
								final Map<Integer, Map<Stats, Double>> classLimit = new HashMap<>();
								final Map<Stats, Double> statsList = new HashMap<>();
								for (Node st = type.getFirstChild(); st != null; st = st.getNextSibling())
								{
									if ("set".equalsIgnoreCase(st.getNodeName()))
									{
										final Stats tp = Stats.valueOf(st.getAttributes().getNamedItem("name").getNodeValue());
										final double value = Double.parseDouble(st.getAttributes().getNamedItem("value").getNodeValue());
										statsList.put(tp, value);
									}
								}
								classLimit.put(-1, statsList);
								_olyStatsList.put(LimitType.SUMMON, classLimit);
							}
						}
					}
					else if ("general".equalsIgnoreCase(cat.getNodeName()))
					{
						for (var type = cat.getFirstChild(); type != null; type = type.getNextSibling())
						{
							if ("player".equalsIgnoreCase(type.getNodeName()))
							{
								final Map<Integer, Map<Stats, Double>> classLimit = new HashMap<>();
								for (var cs = type.getFirstChild(); cs != null; cs = cs.getNextSibling())
								{
									if ("class".equalsIgnoreCase(cs.getNodeName()))
									{
										final int id = Integer.parseInt(cs.getAttributes().getNamedItem("id").getNodeValue());
										final Map<Stats, Double> statsList = new HashMap<>();
										for (Node st = cs.getFirstChild(); st != null; st = st.getNextSibling())
										{
											if ("set".equalsIgnoreCase(st.getNodeName()))
											{
												final Stats tp = Stats.valueOf(st.getAttributes().getNamedItem("name").getNodeValue());
												final double value = Double.parseDouble(st.getAttributes().getNamedItem("value").getNodeValue());
												statsList.put(tp, value);
											}
										}
										classLimit.put(id, statsList);
									}
								}
								_statsList.put(LimitType.PLAYER, classLimit);
							}
							else if ("summon".equalsIgnoreCase(type.getNodeName()))
							{
								final Map<Integer, Map<Stats, Double>> classLimit = new HashMap<>();
								final Map<Stats, Double> statsList = new HashMap<>();
								for (Node st = type.getFirstChild(); st != null; st = st.getNextSibling())
								{
									if ("set".equalsIgnoreCase(st.getNodeName()))
									{
										final Stats tp = Stats.valueOf(st.getAttributes().getNamedItem("name").getNodeValue());
										final double value = Double.parseDouble(st.getAttributes().getNamedItem("value").getNodeValue());
										statsList.put(tp, value);
									}
								}
								classLimit.put(-1, statsList);
								_statsList.put(LimitType.SUMMON, classLimit);
							}
						}
					}
				}
			}
		}
	}
	
	public double getStatLimit(Creature actor, Stats stat, double limit)
	{
		if (actor.isPlayer() || actor.isSummon())
		{
			final var player = actor.getActingPlayer();
			if (player != null && !player.canOverrideCond(PcCondOverride.MAX_STATS_VALUE))
			{
				final Map<Integer, Map<Stats, Double>> limits = player.isInOlympiadMode() ? actor.isSummon() ? _olyStatsList.get(LimitType.SUMMON) : _olyStatsList.get(LimitType.PLAYER) : actor.isSummon() ? _statsList.get(LimitType.SUMMON) : _statsList.get(LimitType.PLAYER);
				if (limits != null && !limits.isEmpty())
				{
					final var classId = actor.isSummon() ? -1 : actor.getActingPlayer().getBaseClass();
					Map<Stats, Double> stats = limits.get(classId);
					if (stats != null && stats.containsKey(stat))
					{
						final double value = stats.get(stat);
						if (limit > value)
						{
							return value;
						}
					}
					else
					{
						if (actor.isSummon())
						{
							return limit;
						}
						
						stats = limits.get(-1);
						if (stats != null && stats.containsKey(stat))
						{
							final double value = stats.get(stat);
							if (limit > value)
							{
								return value;
							}
						}
					}
				}
			}
		}
		return limit;
	}
	
	public static final LimitStatParser getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final LimitStatParser _instance = new LimitStatParser();
	}
}