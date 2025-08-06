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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import l2e.gameserver.Config;
import l2e.gameserver.data.DocumentParser;
import l2e.gameserver.model.ProductItem;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.ProductItemTemplate;
import l2e.gameserver.model.actor.templates.items.Item;
import l2e.gameserver.network.serverpackets.ExBrBuyProduct;
import l2e.gameserver.network.serverpackets.ExBrGamePoint;
import l2e.gameserver.network.serverpackets.ExBrRecentProductList;
import l2e.gameserver.network.serverpackets.StatusUpdate;

public class ProductItemParser extends DocumentParser
{
	private final Map<Integer, ProductItem> _itemsList = new TreeMap<>();
	private final ConcurrentHashMap<Integer, List<ProductItem>> _recentList = new ConcurrentHashMap<>();
	
	protected ProductItemParser()
	{
		load();
	}
	
	@Override
	public final void load()
	{
		_itemsList.clear();
		_recentList.clear();
		parseDatapackFile("data/stats/services/item-mall.xml");
		info("Loaded " + _itemsList.size() + " items for item mall.");
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
				for (Node n = c.getFirstChild(); n != null; n = n.getNextSibling())
				{
					if ("product".equalsIgnoreCase(n.getNodeName()))
					{
						final NamedNodeMap list = n.getAttributes();

						final int productId = Integer.parseInt(list.getNamedItem("id").getNodeValue());
						final int category = list.getNamedItem("category") != null ? Integer.parseInt(list.getNamedItem("category").getNodeValue()) : 5;
						final int price = list.getNamedItem("price") != null ? Integer.parseInt(list.getNamedItem("price").getNodeValue()) : 0;

						final Boolean isEvent = (list.getNamedItem("isEvent") != null) && Boolean.parseBoolean(list.getNamedItem("isEvent").getNodeValue());
						final Boolean isBest = (list.getNamedItem("isBest") != null) && Boolean.parseBoolean(list.getNamedItem("isBest").getNodeValue());
						final Boolean isNew = (list.getNamedItem("isNew") != null) && Boolean.parseBoolean(list.getNamedItem("isNew").getNodeValue());
						final int tabId = getProductTabId(isEvent, isBest, isNew);
						
						final long startTimeSale = list.getNamedItem("sale_start_date") != null ? getMillisecondsFromString(list.getNamedItem("sale_start_date").getNodeValue()) : 0;
						final long endTimeSale = list.getNamedItem("sale_end_date") != null ? getMillisecondsFromString(list.getNamedItem("sale_end_date").getNodeValue()) : 0;
						final int daysOfWeek = list.getNamedItem("daysOfWeek") != null ? Integer.parseInt(list.getNamedItem("daysOfWeek").getNodeValue()) : 127;
						final int stock = list.getNamedItem("stock") != null ? Integer.parseInt(list.getNamedItem("stock").getNodeValue()) : 0;
						final int maxStock = list.getNamedItem("maxStock") != null ? Integer.parseInt(list.getNamedItem("maxStock").getNodeValue()) : -1;
						
						final ArrayList<ProductItemTemplate> components = new ArrayList<>();
						final ProductItem pr = new ProductItem(productId, category, price, tabId, startTimeSale, endTimeSale, daysOfWeek, stock, maxStock);
						for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
						{
							if ("component".equalsIgnoreCase(d.getNodeName()))
							{
								final NamedNodeMap component = d.getAttributes();

								final int itemId = Integer.parseInt(component.getNamedItem("itemId").getNodeValue());
								final int count = Integer.parseInt(component.getNamedItem("count").getNodeValue());
								final ProductItemTemplate product = new ProductItemTemplate(itemId, count);
								components.add(product);
							}
						}
						pr.setComponents(components);
						_itemsList.put(productId, pr);
					}
				}
			}
		}
	}
	
	public void requestBuyItem(Player player, int productId, int count)
	{
		if ((count > 99) || (count <= 0))
		{
			return;
		}
		
		final ProductItem product = ProductItemParser.getInstance().getProduct(productId);
		if (product == null)
		{
			player.sendPacket(new ExBrBuyProduct(ExBrBuyProduct.RESULT_WRONG_PRODUCT));
			return;
		}
		
		if ((System.currentTimeMillis() < product.getStartTimeSale()) || (System.currentTimeMillis() > product.getEndTimeSale()))
		{
			player.sendPacket(new ExBrBuyProduct(ExBrBuyProduct.RESULT_SALE_PERIOD_ENDED));
			return;
		}
		
		final long totalPoints = product.getPoints() * count;
		
		if (totalPoints <= 0)
		{
			player.sendPacket(new ExBrBuyProduct(ExBrBuyProduct.RESULT_WRONG_PRODUCT));
			return;
		}
		
		final long gamePointSize = Config.GAME_POINT_ITEM_ID == -1 ? player.getGamePoints() : player.getInventory().getInventoryItemCount(Config.GAME_POINT_ITEM_ID, -1);
		
		if (totalPoints > gamePointSize)
		{
			player.sendPacket(new ExBrBuyProduct(ExBrBuyProduct.RESULT_NOT_ENOUGH_POINTS));
			return;
		}
		
		int totalWeight = 0;
		for (final ProductItemTemplate com : product.getComponents())
		{
			totalWeight += com.getWeight();
		}
		totalWeight *= count;
		
		int totalCount = 0;
		
		for (final ProductItemTemplate com : product.getComponents())
		{
			final Item item = ItemsParser.getInstance().getTemplate(com.getId());
			if (item == null)
			{
				player.sendPacket(new ExBrBuyProduct(ExBrBuyProduct.RESULT_WRONG_PRODUCT));
				return;
			}
			totalCount += item.isStackable() ? 1 : com.getCount() * count;
		}
		
		if (!player.getInventory().validateCapacity(totalCount) || !player.getInventory().validateWeight(totalWeight))
		{
			player.sendPacket(new ExBrBuyProduct(ExBrBuyProduct.RESULT_INVENTORY_FULL));
			return;
		}
		
		if (Config.GAME_POINT_ITEM_ID == -1)
		{
			player.setGamePoints(player.getGamePoints() - totalPoints);
		}
		else
		{
			player.getInventory().destroyItemByItemId("Buy Product" + productId, Config.GAME_POINT_ITEM_ID, totalPoints, player, null);
		}
		
		for (final ProductItemTemplate comp : product.getComponents())
		{
			player.getInventory().addItem("Buy Product" + productId, comp.getId(), comp.getCount() * count, player, null);
		}
		
		if (_recentList.get(player.getObjectId()) == null)
		{
			final List<ProductItem> charList = new ArrayList<>();
			charList.add(product);
			_recentList.put(player.getObjectId(), charList);
		}
		else
		{
			_recentList.get(player.getObjectId()).add(product);
		}
		player.sendStatusUpdate(false, false, StatusUpdate.CUR_LOAD);
		player.sendPacket(new ExBrGamePoint(player));
		player.sendPacket(new ExBrBuyProduct(ExBrBuyProduct.RESULT_OK));
	}
	
	private static int getProductTabId(boolean isEvent, boolean isBest, boolean isNew)
	{
		if (isEvent && isBest)
		{
			return 3;
		}
		
		if (isEvent)
		{
			return 1;
		}
		
		if (isBest)
		{
			return 2;
		}
		return 4;
	}
	
	private static long getMillisecondsFromString(String datetime)
	{
		final DateFormat df = new SimpleDateFormat("dd.MM.yyyy HH:mm");
		try
		{
			final Date time = df.parse(datetime);
			final Calendar calendar = Calendar.getInstance();
			calendar.setTime(time);
			
			return calendar.getTimeInMillis();
		}
		catch (final Exception e)
		{
			e.printStackTrace();
		}
		return 0;
	}
	
	public Collection<ProductItem> getAllItems()
	{
		return _itemsList.values();
	}
	
	public ProductItem getProduct(int id)
	{
		return _itemsList.get(id);
	}
	
	public void recentProductList(Player player)
	{
		player.sendPacket(new ExBrRecentProductList(player.getObjectId()));
	}
	
	public List<ProductItem> getRecentListByOID(int objId)
	{
		return _recentList.get(objId) == null ? new ArrayList<>() : _recentList.get(objId);
	}
	
	public static ProductItemParser getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final ProductItemParser _instance = new ProductItemParser();
	}
}