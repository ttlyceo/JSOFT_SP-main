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
import l2e.gameserver.model.actor.templates.SoulCrystalTemplate;

public final class SoulCrystalParser extends DocumentParser
{
	private final Map<Integer, SoulCrystalTemplate> _crystals = new HashMap<>();
	
	private SoulCrystalParser()
	{
		load();
	}
	
	@Override
	public final void load()
	{
		_crystals.clear();
		parseDatapackFile("data/stats/items/soul_crystals.xml");
		info("Loaded " + _crystals.size() + " soul crystal templates.");
	}
	
	@Override
	protected void reloadDocument()
	{
	}
	
	@Override
	protected void parseDocument()
	{
		for (Node c = getCurrentDocument().getFirstChild(); c != null; c = c.getNextSibling())
		{
			if ("list".equalsIgnoreCase(c.getNodeName()))
			{
				for (Node d = c.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("crystal".equalsIgnoreCase(d.getNodeName()))
					{
						final int itemId = Integer.parseInt(d.getAttributes().getNamedItem("itemId").getNodeValue());
						final int level = Integer.parseInt(d.getAttributes().getNamedItem("level").getNodeValue());
						final int nextItemId = Integer.parseInt(d.getAttributes().getNamedItem("next_itemId").getNodeValue());
						final int cursedNextItemId = d.getAttributes().getNamedItem("cursed_next_itemId") == null ? 0 : Integer.parseInt(d.getAttributes().getNamedItem("cursed_next_itemId").getNodeValue());

						addCrystal(new SoulCrystalTemplate(itemId, level, nextItemId, cursedNextItemId));
					}
				}
			}
		}
	}

	public void addCrystal(SoulCrystalTemplate crystal)
	{
		_crystals.put(crystal.getId(), crystal);
	}

	public SoulCrystalTemplate getCrystal(int item)
	{
		return _crystals.get(item);
	}
	
	public SoulCrystalTemplate[] getCrystals()
	{
		return _crystals.values().toArray(new SoulCrystalTemplate[_crystals.size()]);
	}
	
	public static SoulCrystalParser getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final SoulCrystalParser _instance = new SoulCrystalParser();
	}
}