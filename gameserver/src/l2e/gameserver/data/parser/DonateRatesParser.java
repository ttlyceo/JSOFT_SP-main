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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.w3c.dom.Node;

import l2e.commons.util.TimeUtils;
import l2e.gameserver.Config;
import l2e.gameserver.data.DocumentParser;
import l2e.gameserver.model.actor.templates.player.DonateRateTempate;
import l2e.gameserver.model.base.BonusType;
import l2e.gameserver.model.stats.StatsSet;

/**
 * Created by LordWinter
 */
public final class DonateRatesParser extends DocumentParser
{
	private final Map<Integer, DonateRateTempate> _templates = new ConcurrentHashMap<>();

	private DonateRatesParser()
	{
		_templates.clear();
		load();
	}
	
	@Override
	public synchronized void load()
	{
		parseDatapackFile("data/stats/services/donateRates.xml");
		info("Loaded " + _templates.size() + " donate rate templates.");
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
				for (var d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("template".equalsIgnoreCase(d.getNodeName()))
					{
						final StatsSet params = new StatsSet();
						final Map<BonusType, Double> bonusList = new ConcurrentHashMap<>();
						final Map<BonusType, String> bonusIcon = new ConcurrentHashMap<>();
						final Map<Integer, Long> price = new HashMap<>();
						final int id = Integer.parseInt(d.getAttributes().getNamedItem("id").getNodeValue());
						for (final String lang : Config.MULTILANG_ALLOWED)
						{
							if (lang != null)
							{
								final String name = "name" + lang.substring(0, 1).toUpperCase() + lang.substring(1);
								params.set(name, d.getAttributes().getNamedItem(name) != null ? d.getAttributes().getNamedItem(name).getNodeValue() : d.getAttributes().getNamedItem("nameEn") != null ? d.getAttributes().getNamedItem("nameEn").getNodeValue() : "");
							}
						}
						params.set("icon", d.getAttributes().getNamedItem("icon") != null ? d.getAttributes().getNamedItem("icon").getNodeValue() : "");
						final var enable = Boolean.parseBoolean(d.getAttributes().getNamedItem("enable").getNodeValue());
						params.set("enable", enable);
						for (var cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling())
						{
							if ("rate".equalsIgnoreCase(cd.getNodeName()))
							{
								final var type = BonusType.valueOf(cd.getAttributes().getNamedItem("type").getNodeValue());
								final var value = cd.getAttributes().getNamedItem("value").getNodeValue();
								final var icon = cd.getAttributes().getNamedItem("icon") != null ? cd.getAttributes().getNamedItem("icon").getNodeValue() : "";
								if (type != null)
								{
									addBonusType(bonusList, bonusIcon, type, Double.parseDouble(value), icon);
								}
							}
							else if ("time".equalsIgnoreCase(cd.getNodeName()))
							{
								final int days = Integer.parseInt(cd.getAttributes().getNamedItem("days").getNodeValue());
								final int hours = Integer.parseInt(cd.getAttributes().getNamedItem("hours").getNodeValue());
								final int minutes = Integer.parseInt(cd.getAttributes().getNamedItem("minutes").getNodeValue());
								
								final int total = (int) ((TimeUtils.addDay(days) + TimeUtils.addHours(hours) + TimeUtils.addMinutes(minutes)) / 1000L);
								params.set("time", total);
							}
							else if ("price".equalsIgnoreCase(cd.getNodeName()))
							{
								for (Node gt = cd.getFirstChild(); gt != null; gt = gt.getNextSibling())
								{
									if ("item".equalsIgnoreCase(gt.getNodeName()))
									{
										final int itemId = Integer.parseInt(gt.getAttributes().getNamedItem("id").getNodeValue());
										final long count = Long.parseLong(gt.getAttributes().getNamedItem("count").getNodeValue());
										price.put(itemId, count);
									}
								}
							}
						}
						
						if (enable)
						{
							_templates.put(id, new DonateRateTempate(id, params, price, bonusList, bonusIcon));
						}
					}
				}
			}
		}
	}

	private void addBonusType(Map<BonusType, Double> bonusList, Map<BonusType, String> iconList, BonusType type, double value, String icon)
	{
		if (bonusList.containsKey(type))
		{
			final var val = bonusList.get(type);
			switch (type)
			{
				case CRAFT_CHANCE :
				case MASTER_WORK_CHANCE :
				case ENCHANT_CHANCE :
				case MAX_DROP_PER_ONE_GROUP :
				case MAX_SPOIL_PER_ONE_GROUP :
				case MAX_RAID_DROP_PER_ONE_GROUP :
					bonusList.put(type, (val + value));
					break;
				default :
					bonusList.put(type, (val + (value - 1)));
					break;
			}
			return;
		}
		bonusList.put(type, value);
		iconList.put(type, icon);
	}
	
	public DonateRateTempate getTemplate(int id)
	{
		return _templates.get(id);
	}
	
	public Collection<DonateRateTempate> getTemplates()
	{
		return _templates.values();
	}
	
	public static DonateRatesParser getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final DonateRatesParser _instance = new DonateRatesParser();
	}
}