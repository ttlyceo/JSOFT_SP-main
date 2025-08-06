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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import l2e.gameserver.Config;
import l2e.gameserver.data.DocumentParser;
import l2e.gameserver.model.service.donate.Attribution;
import l2e.gameserver.model.service.donate.DonateItem;
import l2e.gameserver.model.service.donate.Donation;
import l2e.gameserver.model.service.donate.Enchant;
import l2e.gameserver.model.service.donate.FoundList;
import l2e.gameserver.model.service.donate.SimpleList;
import l2e.gameserver.model.stats.StatsSet;

public final class DonationParser extends DocumentParser
{
	private final Map<Integer, Donation> _donate = new HashMap<>();

	protected DonationParser()
	{
		_donate.clear();
		load();
	}
	
	@Override
	public synchronized void load()
	{
		parseDatapackFile("data/stats/services/donation.xml");
		info("Loaded " + _donate.size() + " donation templates.");
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
					if ("donation".equalsIgnoreCase(d.getNodeName()))
					{
						var donation = d.getAttributes();

						final int id = Integer.parseInt(donation.getNamedItem("id").getNodeValue());
						final var params = new StatsSet();
						for (final String lang : Config.MULTILANG_ALLOWED)
						{
							if (lang != null)
							{
								final String name = "name" + lang.substring(0, 1).toUpperCase() + lang.substring(1);
								params.set(name, donation.getNamedItem(name) != null ? donation.getNamedItem(name).getNodeValue() : donation.getNamedItem("nameEn") != null ? donation.getNamedItem("nameEn").getNodeValue() : "");
							}
						}
						final String icon = donation.getNamedItem("icon").getNodeValue();
						final int group = Integer.parseInt(donation.getNamedItem("group").getNodeValue());
						final boolean found = Boolean.parseBoolean(donation.getNamedItem("found").getNodeValue());
						final var donate = new Donation(id, params, icon, group, found);
						
						for (var cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling())
						{
							donation = cd.getAttributes();

							if ("simples".equalsIgnoreCase(cd.getNodeName()))
							{
								final int s_id = Integer.parseInt(donation.getNamedItem("costId").getNodeValue());
								final long s_count = Long.parseLong(donation.getNamedItem("count").getNodeValue());
								final SimpleList s_list = new SimpleList(s_id, s_count, simple_parse(cd, donation));
								donate.addSimple(s_list);
							}

							if ("foundations".equalsIgnoreCase(cd.getNodeName()))
							{
								if (found)
								{
									final int enchant = Integer.parseInt(donation.getNamedItem("costId").getNodeValue());
									final long attribution = Long.parseLong(donation.getNamedItem("count").getNodeValue());
									final FoundList a_count = new FoundList(enchant, attribution, donate_parse(cd, donation));
									donate.addFound(a_count);
								}
							}

							if ("enchant".equalsIgnoreCase(cd.getNodeName()))
							{
								final int attribution1 = Integer.parseInt(donation.getNamedItem("costId").getNodeValue());
								final long a_id = Long.parseLong(donation.getNamedItem("count").getNodeValue());
								final int e_value = Integer.parseInt(donation.getNamedItem("value").getNodeValue());
								final Enchant a_value = new Enchant(attribution1, a_id, e_value);
								donate.setEnchant(a_value);
							}

							if ("attribution".equalsIgnoreCase(cd.getNodeName()))
							{
								final int a_id1 = Integer.parseInt(donation.getNamedItem("costId").getNodeValue());
								final long a_count1 = Long.parseLong(donation.getNamedItem("count").getNodeValue());
								final int a_value1 = Integer.parseInt(donation.getNamedItem("value").getNodeValue());
								final int size = Integer.parseInt(donation.getNamedItem("size").getNodeValue());
								final var atr = new Attribution(a_id1, a_count1, a_value1, size);
								donate.setAttribution(atr);
							}
						}
						_donate.put(id, donate);
					}
				}
			}
		}
	}
	
	private List<DonateItem> simple_parse(Node d, NamedNodeMap attrs)
	{
		final ArrayList<DonateItem> list = new ArrayList<>();
		for (var cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling())
		{
			if ("simple".equalsIgnoreCase(cd.getNodeName()))
			{
				attrs = cd.getAttributes();

				final int id = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
				final long count = Long.parseLong(attrs.getNamedItem("count").getNodeValue());
				final int enchant = Integer.parseInt(attrs.getNamedItem("enchant").getNodeValue());
				final var donate = new DonateItem(id, count, enchant);
				list.add(donate);
			}
		}
		return list;
	}

	private List<DonateItem> donate_parse(Node d, NamedNodeMap attrs)
	{
		final ArrayList<DonateItem> list = new ArrayList<>();
		for (var cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling())
		{
			if ("foundation".equalsIgnoreCase(cd.getNodeName()))
			{
				attrs = cd.getAttributes();

				final int id = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
				final long count = Long.parseLong(attrs.getNamedItem("count").getNodeValue());
				final int enchant = Integer.parseInt(attrs.getNamedItem("enchant").getNodeValue());
				final DonateItem donate = new DonateItem(id, count, enchant);
				list.add(donate);
			}
		}
		return list;
	}

	public Collection<Donation> getAllDonates()
	{
		return _donate.values();
	}
	
	public Donation getDonate(int id)
	{
		return _donate.get(id);
	}
	
	public List<Donation> getGroup(int id)
	{
		final ArrayList<Donation> group = new ArrayList<>();
		for (final var donate : _donate.values())
		{
			if (donate.getGroup() == id)
			{
				group.add(donate);
			}
		}
		return group;
	}
	
	public static DonationParser getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final DonationParser _instance = new DonationParser();
	}
}
