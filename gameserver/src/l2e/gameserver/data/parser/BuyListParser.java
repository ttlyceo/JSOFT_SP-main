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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import l2e.commons.dbutils.DbUtils;
import l2e.commons.util.file.filter.NumericNameFilter;
import l2e.gameserver.Config;
import l2e.gameserver.data.DocumentParser;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.model.items.buylist.Product;
import l2e.gameserver.model.items.buylist.ProductList;

public final class BuyListParser extends DocumentParser
{
	private final Map<Integer, ProductList> _buyLists = new HashMap<>();

	protected BuyListParser()
	{
		setCurrentFileFilter(new NumericNameFilter());
		load();
	}

	@Override
	public synchronized void load()
	{
		_buyLists.clear();
		parseDirectory("data/stats/npcs/buylists", false);
		if (Config.CUSTOM_BUYLIST)
		{
			parseDirectory("data/stats/npcs/buylists/custom", false);
		}

		info("Loaded " + _buyLists.size() + " buyLists.");

		Connection con = null;
		Statement statement = null;
		ResultSet rs = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.createStatement();
			rs = statement.executeQuery("SELECT * FROM `buylists`");
			while (rs.next())
			{
				final var buyListId = rs.getInt("buylist_id");
				final var itemId = rs.getInt("item_id");
				final var count = rs.getLong("count");
				final var nextRestockTime = rs.getLong("next_restock_time");
				final var buyList = getBuyList(buyListId);
				if (buyList == null)
				{
					warn("BuyList found in database but not loaded from xml! BuyListId: " + buyListId);
					continue;
				}
				final var product = buyList.getProductByItemId(itemId);
				if (product == null)
				{
					warn("ItemId found in database but not loaded from xml! BuyListId: " + buyListId + " ItemId: " + itemId);
					continue;
				}
				if (count < product.getMaxCount())
				{
					product.setCount(count);
					product.restartRestockTask(nextRestockTime);
				}
			}
		}
		catch (final Exception e)
		{
			warn("Failed to load buyList data from database.", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rs);
		}
	}
	
	@Override
	protected void reloadDocument()
	{
	}
	
	@Override
	protected void parseDocument()
	{
		try
		{
			final var buyListId = Integer.parseInt(getCurrentFile().getName().replaceAll(".xml", ""));

			for (var node = getCurrentDocument().getFirstChild(); node != null; node = node.getNextSibling())
			{
				if ("list".equalsIgnoreCase(node.getNodeName()))
				{
					final var buyList = new ProductList(buyListId);
					for (var list_node = node.getFirstChild(); list_node != null; list_node = list_node.getNextSibling())
					{
						if ("item".equalsIgnoreCase(list_node.getNodeName()))
						{
							var itemId = -1;
							var price = -1L;
							var restockDelay = -1L;
							var count = -1L;
							final var attrs = list_node.getAttributes();
							var attr = attrs.getNamedItem("id");
							itemId = Integer.parseInt(attr.getNodeValue());
							attr = attrs.getNamedItem("price");
							if (attr != null)
							{
								price = Long.parseLong(attr.getNodeValue());
							}
							attr = attrs.getNamedItem("restock_delay");
							if (attr != null)
							{
								restockDelay = Long.parseLong(attr.getNodeValue());
							}
							attr = attrs.getNamedItem("count");
							if (attr != null)
							{
								count = Long.parseLong(attr.getNodeValue());
							}
							final var item = ItemsParser.getInstance().getTemplate(itemId);
							if (item != null)
							{
								buyList.addProduct(new Product(buyList.getListId(), item, price, restockDelay, count));
							}
							else
							{
								warn("Item not found. BuyList:" + buyList.getListId() + " ItemID:" + itemId + " File:" + getCurrentFile().getName());
							}
						}
						else if ("npcs".equalsIgnoreCase(list_node.getNodeName()))
						{
							for (var npcs_node = list_node.getFirstChild(); npcs_node != null; npcs_node = npcs_node.getNextSibling())
							{
								if ("npc".equalsIgnoreCase(npcs_node.getNodeName()))
								{
									final var npcId = Integer.parseInt(npcs_node.getTextContent());
									buyList.addAllowedNpc(npcId);
								}
							}
						}
					}
					_buyLists.put(buyList.getListId(), buyList);
				}
			}
		}
		catch (final Exception e)
		{
			warn("Failed to load buyList data from xml File:" + getCurrentFile().getName(), e);
		}
	}

	public ProductList getBuyList(int listId)
	{
		return _buyLists.get(listId);
	}

	public static BuyListParser getInstance()
	{
		return SingletonHolder._instance;
	}

	private static class SingletonHolder
	{
		protected final static BuyListParser _instance = new BuyListParser();
	}
}