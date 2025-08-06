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

public final class FoundationParser extends DocumentParser
{
	private final Map<Integer, Integer> _foundation = new HashMap<>();

	protected FoundationParser()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_foundation.clear();
		parseDatapackFile("data/stats/services/foundation.xml");
		info("Loaded " + _foundation.size() + " foundation templates.");
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
			if ("list".equals(n.getNodeName()))
			{
				NamedNodeMap attrs;
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("foundation".equals(d.getNodeName()))
					{
						attrs = d.getAttributes();
						final int simple = Integer.parseInt(attrs.getNamedItem("simple").getNodeValue());
						final int found = Integer.parseInt(attrs.getNamedItem("found").getNodeValue());
						addFoundation(simple, found);
					}
				}
			}
		}
	}
	
	public void addFoundation(int simple, int found)
	{
		_foundation.put(Integer.valueOf(simple), Integer.valueOf(found));
	}

	public int getFoundation(int id)
	{
		if (_foundation.containsKey(Integer.valueOf(id)))
		{
			return _foundation.get(Integer.valueOf(id));
		}
		return -1;
	}
	
	public static FoundationParser getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final FoundationParser _instance = new FoundationParser();
	}
}