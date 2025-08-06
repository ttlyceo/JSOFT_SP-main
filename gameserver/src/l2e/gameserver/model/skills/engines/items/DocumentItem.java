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
package l2e.gameserver.model.skills.engines.items;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import l2e.gameserver.Config;
import l2e.gameserver.model.actor.templates.items.Item;
import l2e.gameserver.model.base.ItemType;
import l2e.gameserver.model.holders.ItemHolder;
import l2e.gameserver.model.skills.DocumentBase;
import l2e.gameserver.model.skills.conditions.Condition;
import l2e.gameserver.model.stats.StatsSet;

public final class DocumentItem extends DocumentBase
{
	private ItemTemplate _currentItem = null;
	private final List<Item> _itemsInFile = new ArrayList<>();
	
	public DocumentItem(File file)
	{
		super(file);
	}
	
	@Override
	protected StatsSet getStatsSet()
	{
		return _currentItem.set;
	}
	
	@Override
	protected String getTableValue(String name)
	{
		return _tables.get(name)[_currentItem.currentLevel];
	}
	
	@Override
	protected String getTableValue(String name, int idx)
	{
		return _tables.get(name)[idx - 1];
	}
	
	@Override
	protected void parseDocument(Document doc)
	{
		for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("item".equalsIgnoreCase(d.getNodeName()))
					{
						try
						{
							_currentItem = new ItemTemplate();
							parseItem(d);
							_itemsInFile.add(_currentItem.item);
							resetTable();
						}
						catch (final Exception e)
						{
							_log.warn("Cannot create item " + _currentItem.id, e);
						}
					}
				}
			}
		}
	}
	
	protected void parseItem(Node n) throws InvocationTargetException
	{
		final int itemId = Integer.parseInt(n.getAttributes().getNamedItem("id").getNodeValue());
		final String className = n.getAttributes().getNamedItem("type").getNodeValue();
		_currentItem.id = itemId;
		_currentItem.type = className;
		_currentItem.set = new StatsSet();
		_currentItem.set.set("item_id", itemId);
		for (final String lang : Config.MULTILANG_ALLOWED)
		{
			if (lang != null)
			{
				final String name = "name" + lang.substring(0, 1).toUpperCase() + lang.substring(1);
				_currentItem.set.set(name, n.getAttributes().getNamedItem(name) != null ? n.getAttributes().getNamedItem(name).getNodeValue() : n.getAttributes().getNamedItem("nameEn") != null ? n.getAttributes().getNamedItem("nameEn").getNodeValue() : "");
			}
		}
		
		final Node first = n.getFirstChild();
		for (n = first; n != null; n = n.getNextSibling())
		{
			if ("table".equalsIgnoreCase(n.getNodeName()))
			{
				if (_currentItem.item != null)
				{
					throw new IllegalStateException("Item created but table node found! Item " + itemId);
				}
				parseTable(n);
			}
			else if ("set".equalsIgnoreCase(n.getNodeName()))
			{
				if (_currentItem.item != null)
				{
					throw new IllegalStateException("Item created but set node found! Item " + itemId);
				}
				parseBeanSet(n, _currentItem.set, 1);
			}
			else if ("for".equalsIgnoreCase(n.getNodeName()))
			{
				makeItem();
				parseTemplate(n, _currentItem.item);
			}
			else if ("cond".equalsIgnoreCase(n.getNodeName()))
			{
				makeItem();
				final Condition condition = parseCondition(n.getFirstChild(), _currentItem.item);
				final Node msg = n.getAttributes().getNamedItem("msg");
				final Node msgId = n.getAttributes().getNamedItem("msgId");
				if (condition != null && msg != null)
				{
					condition.setMessage(msg.getNodeValue());
				}
				else if (condition != null && msgId != null)
				{
					condition.setMessageId(Integer.decode(getValue(msgId.getNodeValue(), null)));
					final Node addName = n.getAttributes().getNamedItem("addName");
					if (addName != null && Integer.decode(getValue(msgId.getNodeValue(), null)) > 0)
					{
						condition.addName();
					}
				}
				_currentItem.item.attach(condition);
			}
			else if ("items".equalsIgnoreCase(n.getNodeName()))
			{
				makeItem();
				for (Node b = n.getFirstChild(); b != null; b = b.getNextSibling())
				{
					if ("item".equalsIgnoreCase(b.getNodeName()))
					{
						final int id = Integer.parseInt(b.getAttributes().getNamedItem("id").getNodeValue());
						final long min = b.getAttributes().getNamedItem("min") != null ? Long.parseLong(b.getAttributes().getNamedItem("min").getNodeValue()) : 1;
						final long max = b.getAttributes().getNamedItem("max") != null ? Long.parseLong(b.getAttributes().getNamedItem("max").getNodeValue()) : min;
						final int enchant = b.getAttributes().getNamedItem("enchant") != null ? Integer.parseInt(b.getAttributes().getNamedItem("enchant").getNodeValue()) : 0;
						final var type = b.getAttributes().getNamedItem("type") != null ? ItemType.valueOf(b.getAttributes().getNamedItem("type").getNodeValue()) : ItemType.REWARD;
						final int chance = b.getAttributes().getNamedItem("chance") != null ? Integer.parseInt(b.getAttributes().getNamedItem("chance").getNodeValue()) : 100;
						switch (type)
						{
							case REQUEST :
								_currentItem.item.addRequestItem(new ItemHolder(id, min, max, chance, enchant));
								break;
							case REWARD :
								_currentItem.item.addRewardItem(new ItemHolder(id, min, max, chance, enchant));
								break;
						}
					}
				}
			}
		}
		makeItem();
	}
	
	private void makeItem() throws InvocationTargetException
	{
		if (_currentItem.item != null)
		{
			return;
		}
		try
		{
			final Constructor<?> c = Class.forName("l2e.gameserver.model.actor.templates.items." + _currentItem.type).getConstructor(StatsSet.class);
			_currentItem.item = (Item) c.newInstance(_currentItem.set);
		}
		catch (final Exception e)
		{
			throw new InvocationTargetException(e);
		}
	}
	
	public List<Item> getItemList()
	{
		return _itemsInFile;
	}
}