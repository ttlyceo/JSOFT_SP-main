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

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import l2e.gameserver.data.DocumentParser;
import l2e.gameserver.model.items.enchant.EnchantItem;
import l2e.gameserver.model.items.enchant.EnchantScroll;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.stats.StatsSet;

public class EnchantItemParser extends DocumentParser
{
	public static final Map<Integer, EnchantScroll> _scrolls = new HashMap<>();
	public static final Map<Integer, EnchantItem> _supports = new HashMap<>();
	
	public EnchantItemParser()
	{
		load();
	}
	
	@Override
	public synchronized void load()
	{
		_scrolls.clear();
		_supports.clear();
		parseDatapackFile("data/stats/enchanting/enchantItemData.xml");
		info("Loaded " + _scrolls.size() + " enchant scrolls and " + _supports.size() + " support items.");
	}
	
	@Override
	protected void reloadDocument()
	{
	}
	
	@Override
	protected void parseDocument()
	{
		StatsSet set;
		Node att;
		for (Node n = getCurrentDocument().getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("enchant".equalsIgnoreCase(d.getNodeName()))
					{
						final NamedNodeMap attrs = d.getAttributes();
						set = new StatsSet();
						for (int i = 0; i < attrs.getLength(); i++)
						{
							att = attrs.item(i);
							set.set(att.getNodeName(), att.getNodeValue());
						}
						
						final EnchantScroll item = new EnchantScroll(set);
						for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling())
						{
							if ("item".equalsIgnoreCase(cd.getNodeName()))
							{
								item.addItem(parseInteger(cd.getAttributes(), "id"));
							}
						}
						_scrolls.put(item.getId(), item);
					}
					else if ("support".equalsIgnoreCase(d.getNodeName()))
					{
						final NamedNodeMap attrs = d.getAttributes();
						
						set = new StatsSet();
						for (int i = 0; i < attrs.getLength(); i++)
						{
							att = attrs.item(i);
							set.set(att.getNodeName(), att.getNodeValue());
						}
						
						final EnchantItem item = new EnchantItem(set);
						for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling())
						{
							if ("item".equalsIgnoreCase(cd.getNodeName()))
							{
								item.addItem(parseInteger(cd.getAttributes(), "id"));
							}
						}
						_supports.put(item.getId(), item);
					}
				}
			}
		}
	}
	
	public final EnchantScroll getEnchantScroll(ItemInstance scroll)
	{
		return _scrolls.get(scroll.getId());
	}
	
	public final EnchantItem getSupportItem(ItemInstance item)
	{
		return _supports.get(item.getId());
	}
	
	public static final EnchantItemParser getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final EnchantItemParser _instance = new EnchantItemParser();
	}
}