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
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.templates.TeleportTemplate;

public class TeleLocationParser extends DocumentParser
{
	private final Map<Integer, TeleportTemplate> _teleports = new HashMap<>();
	
	protected TeleLocationParser()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_teleports.clear();
		parseDatapackFile("data/stats/npcs/teleports.xml");
		info("Loaded " + _teleports.size() + " teleport templates.");
	}
	
	@Override
	protected void reloadDocument()
	{
	}
	
	@Override
	protected void parseDocument()
	{
		for (var list = getCurrentDocument().getFirstChild().getFirstChild(); list != null; list = list.getNextSibling())
		{
			if (list.getNodeName().equalsIgnoreCase("teleport"))
			{
				final var node = list.getAttributes();
				
				final int id = Integer.valueOf(node.getNamedItem("id").getNodeValue());
				final Location loc = Location.parseLoc(node.getNamedItem("loc").getNodeValue());
				final long price = node.getNamedItem("price") != null ? Long.valueOf(node.getNamedItem("price").getNodeValue()) : 0;
				final boolean isForNoble = node.getNamedItem("noobless") != null ? Boolean.parseBoolean(node.getNamedItem("noobless").getNodeValue()) : false;
				final int itemId = node.getNamedItem("itemId") != null ? Integer.valueOf(node.getNamedItem("itemId").getNodeValue()) : 0;
				final int minLevel = node.getNamedItem("minLevel") != null ? Integer.valueOf(node.getNamedItem("minLevel").getNodeValue()) : 1;
				final int maxLevel = node.getNamedItem("maxLevel") != null ? Integer.valueOf(node.getNamedItem("maxLevel").getNodeValue()) : Config.PLAYER_MAXIMUM_LEVEL;
				if (loc != null)
				{
					_teleports.put(id, new TeleportTemplate(id, loc, isForNoble, itemId, price, minLevel, maxLevel));
				}
			}
		}
	}
	
	public TeleportTemplate getTemplate(int id)
	{
		return _teleports.get(id);
	}
	
	public static TeleLocationParser getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final TeleLocationParser _instance = new TeleLocationParser();
	}
}