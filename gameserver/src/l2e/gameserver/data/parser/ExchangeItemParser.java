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
import l2e.gameserver.model.service.exchange.Change;
import l2e.gameserver.model.service.exchange.Variant;

public class ExchangeItemParser extends DocumentParser
{
	private final Map<Integer, Change> _changes = new HashMap<>();
	private final Map<Integer, Change> _upgrades = new HashMap<>();
	
	public static ExchangeItemParser getInstance()
	{
		return SingletonHolder._instance;
	}
	
	protected ExchangeItemParser()
	{
		_changes.clear();
		_upgrades.clear();
		load();
	}
	
	@Override
	public synchronized void load()
	{
		parseDatapackFile("data/stats/services/exchange.xml");
		info("Loaded " + _changes.size() + " changes groups and " + _upgrades.size() + " update changes groups.");
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
					if ("change".equalsIgnoreCase(d.getNodeName()))
					{
						final NamedNodeMap attrs = d.getAttributes();
						final int changeId = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
						final String changeName = attrs.getNamedItem("name").getNodeValue();
						final String changeIcon = attrs.getNamedItem("icon").getNodeValue();
						final int cost_id = Integer.parseInt(attrs.getNamedItem("cost_id").getNodeValue());
						final long cost_count = Long.parseLong(attrs.getNamedItem("cost_count").getNodeValue());
						final boolean attribute_change = Boolean.parseBoolean(attrs.getNamedItem("attribute_change").getNodeValue());
						final boolean is_upgrade = Boolean.parseBoolean(attrs.getNamedItem("is_upgrade").getNodeValue());

						addChanges(new Change(changeId, changeName, changeIcon, cost_id, cost_count, attribute_change, is_upgrade, parseVariants(d, attrs)));
					}
				}
			}
		}
	}
	
	private List<Variant> parseVariants(Node d, NamedNodeMap attrs)
	{
		final List<Variant> list = new ArrayList<>();
		for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling())
		{
			if ("variant".equalsIgnoreCase(cd.getNodeName()))
			{
				attrs = cd.getAttributes();
				
				final int number = Integer.parseInt(attrs.getNamedItem("number").getNodeValue());
				final int id = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
				final String name = attrs.getNamedItem("name").getNodeValue();
				final String icon = attrs.getNamedItem("icon").getNodeValue();
				
				list.add(new Variant(number, id, name, icon));
			}
		}
		return list;
	}

	public void addChanges(Change armorset)
	{
		if (armorset.isUpgrade())
		{
			_upgrades.put(armorset.getId(), armorset);
		}
		else
		{
			_changes.put(armorset.getId(), armorset);
		}
	}
	
	public Change getChanges(int id, boolean isUpgrade)
	{
		if (isUpgrade)
		{
			return _upgrades.get(id);
		}
		return _changes.get(id);
	}
	
	public int size()
	{
		return _changes.size() + _upgrades.size();
	}
	
	private static class SingletonHolder
	{
		protected static final ExchangeItemParser _instance = new ExchangeItemParser();
	}
}