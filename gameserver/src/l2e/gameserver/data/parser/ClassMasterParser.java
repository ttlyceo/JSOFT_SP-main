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

import org.w3c.dom.Node;

import l2e.gameserver.data.DocumentParser;
import l2e.gameserver.model.actor.templates.ClassMasterTemplate;
import l2e.gameserver.model.holders.ItemHolder;

public final class ClassMasterParser extends DocumentParser
{
	private final Map<Integer, ClassMasterTemplate> _templates = new HashMap<>();
	private final Map<Integer, List<ItemHolder>> _classСhangeRewards = new HashMap<>();

	private boolean _allowClassMaster;
	private boolean _allowCommunityClassMaster;
	
	protected ClassMasterParser()
	{
		load();
	}
	
	public void reload()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_templates.clear();
		_classСhangeRewards.clear();
		parseDatapackFile("data/stats/services/classMaster.xml");
		info("Loaded " + _templates.size() + " class master templates.");
		info("Loaded " + _classСhangeRewards.size() + " grand master rewards.");
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
				_allowClassMaster = Boolean.parseBoolean(n.getAttributes().getNamedItem("allowClassMaster").getNodeValue());
				_allowCommunityClassMaster = Boolean.parseBoolean(n.getAttributes().getNamedItem("allowCommunityClassMaster").getNodeValue());
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("class".equals(d.getNodeName()))
					{
						final int level = Integer.parseInt(d.getAttributes().getNamedItem("level").getNodeValue());
						final boolean allowedChange = d.getAttributes().getNamedItem("allowedChange") != null ? Boolean.parseBoolean(d.getAttributes().getNamedItem("allowedChange").getNodeValue()) : false;
						final List<ItemHolder> requestItems = new ArrayList<>();
						final Map<Integer, List<ItemHolder>> rewardItems = new HashMap<>();
						for (Node c = d.getFirstChild(); c != null; c = c.getNextSibling())
						{
							if ("requestItems".equals(c.getNodeName()))
							{
								for (Node i = c.getFirstChild(); i != null; i = i.getNextSibling())
								{
									if ("item".equals(i.getNodeName()))
									{
										final int id = Integer.parseInt(i.getAttributes().getNamedItem("id").getNodeValue());
										final long count = Long.parseLong(i.getAttributes().getNamedItem("count").getNodeValue());
										requestItems.add(new ItemHolder(id, count));
									}
								}
							}
							else if ("rewardItems".equals(c.getNodeName()))
							{
								final int classId = c.getAttributes().getNamedItem("classId") != null ? Integer.parseInt(c.getAttributes().getNamedItem("classId").getNodeValue()) : -1;
								if (!rewardItems.containsKey(classId))
								{
									rewardItems.put(classId, new ArrayList<>());
								}
								
								for (Node i = c.getFirstChild(); i != null; i = i.getNextSibling())
								{
									if ("item".equals(i.getNodeName()))
									{
										final int id = Integer.parseInt(i.getAttributes().getNamedItem("id").getNodeValue());
										final long count = Long.parseLong(i.getAttributes().getNamedItem("count").getNodeValue());
										final double chance = i.getAttributes().getNamedItem("chance") != null ? Double.parseDouble(i.getAttributes().getNamedItem("chance").getNodeValue()) : 100;
										rewardItems.get(classId).add(new ItemHolder(id, count, chance));
									}
								}
							}
						}
						_templates.put(level, new ClassMasterTemplate(requestItems, rewardItems, allowedChange));
					}
					else if ("grandMaster".equals(d.getNodeName()))
					{
						final int level = Integer.parseInt(d.getAttributes().getNamedItem("level").getNodeValue());
						final List<ItemHolder> items = new ArrayList<>();
						for (Node c = d.getFirstChild(); c != null; c = c.getNextSibling())
						{
							if ("item".equals(c.getNodeName()))
							{
								final int id = Integer.parseInt(c.getAttributes().getNamedItem("id").getNodeValue());
								final long count = Integer.parseInt(c.getAttributes().getNamedItem("count").getNodeValue());
								items.add(new ItemHolder(id, count));
							}
						}
						_classСhangeRewards.put(level, items);
					}
				}
			}
		}
	}
	
	public ClassMasterTemplate getClassTemplate(int level)
	{
		if (_templates.containsKey(level))
		{
			return _templates.get(level);
		}
		return null;
	}
	
	public boolean isAllowedClassChange(int level)
	{
		if (_templates.containsKey(level))
		{
			final ClassMasterTemplate template = _templates.get(level);
			return template != null && template.isAllowedChangeClass();
		}
		return false;
	}
	
	public boolean isAllowClassMaster()
	{
		return _allowClassMaster;
	}
	
	public boolean isAllowCommunityClassMaster()
	{
		return _allowCommunityClassMaster;
	}
	
	public List<ItemHolder> getGrandMasterRewards(int level)
	{
		return _classСhangeRewards.get(level);
	}
	
	public static ClassMasterParser getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final ClassMasterParser _instance = new ClassMasterParser();
	}
}