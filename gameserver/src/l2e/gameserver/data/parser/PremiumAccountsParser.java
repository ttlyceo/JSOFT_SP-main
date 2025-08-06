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
import java.util.List;

import org.w3c.dom.Node;

import l2e.commons.util.TimeUtils;
import l2e.gameserver.Config;
import l2e.gameserver.data.DocumentParser;
import l2e.gameserver.model.service.premium.PremiumGift;
import l2e.gameserver.model.service.premium.PremiumPrice;
import l2e.gameserver.model.service.premium.PremiumRates;
import l2e.gameserver.model.service.premium.PremiumTemplate;
import l2e.gameserver.model.stats.StatsSet;

public final class PremiumAccountsParser extends DocumentParser
{
	private final List<PremiumTemplate> _templates = new ArrayList<>();

	private PremiumAccountsParser()
	{
		_templates.clear();
		load();
	}
	
	@Override
	public synchronized void load()
	{
		parseDirectory("data/stats/premiumAccounts", false);
		info("Loaded " + _templates.size() + " premium account templates.");
	}
	
	@Override
	protected void reloadDocument()
	{
	}
	
	@Override
	protected void parseDocument()
	{
		for (Node n = getCurrentDocument().getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("premium".equalsIgnoreCase(d.getNodeName()))
					{
						final int id = Integer.parseInt(d.getAttributes().getNamedItem("id").getNodeValue());
						final StatsSet params = new StatsSet();
						for (final String lang : Config.MULTILANG_ALLOWED)
						{
							if (lang != null)
							{
								final String name = "name" + lang.substring(0, 1).toUpperCase() + lang.substring(1);
								params.set(name, d.getAttributes().getNamedItem(name) != null ? d.getAttributes().getNamedItem(name).getNodeValue() : d.getAttributes().getNamedItem("nameEn") != null ? d.getAttributes().getNamedItem("nameEn").getNodeValue() : "");
							}
						}
						final String icon = d.getAttributes().getNamedItem("icon").getNodeValue();
						final boolean isOnlineType = Boolean.parseBoolean(d.getAttributes().getNamedItem("onlineType").getNodeValue());
						final boolean isPersonal = Boolean.parseBoolean(d.getAttributes().getNamedItem("isPersonal").getNodeValue());
						final int groupId = d.getAttributes().getNamedItem("groupId") != null ? Integer.parseInt(d.getAttributes().getNamedItem("groupId").getNodeValue()) : id;

						final PremiumTemplate premium = new PremiumTemplate(id, params, icon, isOnlineType, isPersonal, groupId);
						
						for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling())
						{
							if ("rate".equalsIgnoreCase(cd.getNodeName()))
							{
								final String type = cd.getAttributes().getNamedItem("type").getNodeValue();
								final PremiumRates key = PremiumRates.find(type);
								if (key != null)
								{
									premium.setRate(key, cd.getAttributes().getNamedItem("value").getNodeValue());
								}
							}
							else if ("gifts".equalsIgnoreCase(cd.getNodeName()))
							{
								for (Node gt = cd.getFirstChild(); gt != null; gt = gt.getNextSibling())
								{
									if ("gift".equalsIgnoreCase(gt.getNodeName()))
									{
										final int itemId = Integer.parseInt(gt.getAttributes().getNamedItem("id").getNodeValue());
										final long count = Long.parseLong(gt.getAttributes().getNamedItem("count").getNodeValue());
										final boolean removable = Boolean.parseBoolean(gt.getAttributes().getNamedItem("removable").getNodeValue());
										final PremiumGift gift = new PremiumGift(itemId, count, removable);
										premium.addGift(gift);
									}
								}
							}
							else if ("time".equalsIgnoreCase(cd.getNodeName()))
							{
								final int days = Integer.parseInt(cd.getAttributes().getNamedItem("days").getNodeValue());
								final int hours = Integer.parseInt(cd.getAttributes().getNamedItem("hours").getNodeValue());
								final int minutes = Integer.parseInt(cd.getAttributes().getNamedItem("minutes").getNodeValue());
								
								final int total = (int) ((TimeUtils.addDay(days) + TimeUtils.addHours(hours) + TimeUtils.addMinutes(minutes)) / 1000L);
								premium.setTime(total);
							}
							else if ("price".equalsIgnoreCase(cd.getNodeName()))
							{
								for (Node gt = cd.getFirstChild(); gt != null; gt = gt.getNextSibling())
								{
									if ("item".equalsIgnoreCase(gt.getNodeName()))
									{
										final int itemId = Integer.parseInt(gt.getAttributes().getNamedItem("id").getNodeValue());
										final long count = Long.parseLong(gt.getAttributes().getNamedItem("count").getNodeValue());
										final PremiumPrice price = new PremiumPrice(itemId, count);
										premium.addPrice(price);
									}
								}
							}
						}
						_templates.add(premium);
					}
				}
			}
		}
	}

	public List<PremiumTemplate> getTemplates()
	{
		return _templates;
	}
	
	public PremiumTemplate getPremiumTemplate(int id)
	{
		for (final PremiumTemplate template : _templates)
		{
			if (template.getId() == id)
			{
				return template;
			}
		}
		return null;
	}
	
	public static PremiumAccountsParser getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final PremiumAccountsParser _instance = new PremiumAccountsParser();
	}
}