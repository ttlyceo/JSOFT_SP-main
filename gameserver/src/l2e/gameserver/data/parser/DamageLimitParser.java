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
import l2e.gameserver.model.actor.templates.npc.DamageLimit;

public final class DamageLimitParser extends DocumentParser
{
	private final Map<Integer, DamageLimit> _templates = new HashMap<>();

	protected DamageLimitParser()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_templates.clear();
		parseDatapackFile("data/stats/npcs/damageLimit.xml");
		info("Loaded " + _templates.size() + " damage limit templates.");
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
					if ("npc".equals(d.getNodeName()))
					{
						attrs = d.getAttributes();
						final int id = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
						final int damage = attrs.getNamedItem("damage") != null ? Integer.parseInt(attrs.getNamedItem("damage").getNodeValue()) : -1;
						final int physicDamage = attrs.getNamedItem("physicDamage") != null ? Integer.parseInt(attrs.getNamedItem("physicDamage").getNodeValue()) : -1;
						final int magicDamage = attrs.getNamedItem("magicDamage") != null ? Integer.parseInt(attrs.getNamedItem("magicDamage").getNodeValue()) : -1;
						_templates.put(id, new DamageLimit(damage, physicDamage, magicDamage));
					}
				}
			}
		}
	}
	
	public DamageLimit getDamageLimit(int npcId)
	{
		if (_templates.containsKey(npcId))
		{
			return _templates.get(npcId);
		}
		return null;
	}
	
	public static DamageLimitParser getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final DamageLimitParser _instance = new DamageLimitParser();
	}
}