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
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.templates.community.CBTeleportTemplate;
import l2e.gameserver.model.holders.ItemHolder;

public class CommunityTeleportsParser extends DocumentParser
{
	private final Map<Integer, CBTeleportTemplate> _templates = new HashMap<>();
	
	protected CommunityTeleportsParser()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_templates.clear();
		parseDatapackFile("data/stats/services/communityTeleports.xml");
		info("Loaded " + _templates.size() + " community teleport templates.");
	}
	
	@Override
	protected void reloadDocument()
	{
	}
	
	@Override
	protected void parseDocument()
	{
		for (Node list = getCurrentDocument().getFirstChild().getFirstChild(); list != null; list = list.getNextSibling())
		{
			if (list.getNodeName().equalsIgnoreCase("point"))
			{
				final int id = Integer.parseInt(list.getAttributes().getNamedItem("id").getNodeValue());
				final String name = list.getAttributes().getNamedItem("name").getNodeValue();
				final int minLvl = Integer.parseInt(list.getAttributes().getNamedItem("minLevel").getNodeValue());
				final int maxLvl = Integer.parseInt(list.getAttributes().getNamedItem("maxLevel").getNodeValue());
				final int freeLevel = list.getAttributes().getNamedItem("freeLevel") != null ? Integer.parseInt(list.getAttributes().getNamedItem("freeLevel").getNodeValue()) : 85;
				final boolean canPk = Boolean.parseBoolean(list.getAttributes().getNamedItem("pk").getNodeValue());
				final boolean forPremium = Boolean.parseBoolean(list.getAttributes().getNamedItem("forPremium").getNodeValue());
				
				ItemHolder price = null;
				ItemHolder request = null;
				Location loc = null;
				
				for (Node cd = list.getFirstChild(); cd != null; cd = cd.getNextSibling())
				{
					if ("request".equalsIgnoreCase(cd.getNodeName()))
					{
						final int itemId = Integer.parseInt(cd.getAttributes().getNamedItem("itemId").getNodeValue());
						final long count = Long.parseLong(cd.getAttributes().getNamedItem("count").getNodeValue());
						request = new ItemHolder(itemId, count);
					}
					else if ("cost".equalsIgnoreCase(cd.getNodeName()))
					{
						final int itemId = Integer.parseInt(cd.getAttributes().getNamedItem("itemId").getNodeValue());
						final long count = Long.parseLong(cd.getAttributes().getNamedItem("count").getNodeValue());
						price = new ItemHolder(itemId, count);
					}
					else if ("coordinates".equalsIgnoreCase(cd.getNodeName()))
					{
						final int x = Integer.parseInt(cd.getAttributes().getNamedItem("x").getNodeValue());
						final int y = Integer.parseInt(cd.getAttributes().getNamedItem("y").getNodeValue());
						final int z = Integer.parseInt(cd.getAttributes().getNamedItem("z").getNodeValue());
						loc = new Location(x, y, z);
					}
				}
				_templates.put(id, new CBTeleportTemplate(id, name, minLvl, maxLvl, freeLevel, canPk, forPremium, loc, price, request));
			}
		}
	}
	
	public CBTeleportTemplate getTemplate(int id)
	{
		return _templates.get(id);
	}
	
	public static CommunityTeleportsParser getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final CommunityTeleportsParser _instance = new CommunityTeleportsParser();
	}
}