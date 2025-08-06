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
package l2e.gameserver.instancemanager;

import java.io.File;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import l2e.commons.log.LoggerObject;
import l2e.gameserver.Config;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.model.items.itemauction.ItemAuctionInstance;

public final class ItemAuctionManager extends LoggerObject
{
	public static final ItemAuctionManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private final Map<Integer, ItemAuctionInstance> _managerInstances = new HashMap<>();
	private final AtomicInteger _auctionIds;

	protected ItemAuctionManager()
	{
		_auctionIds = new AtomicInteger(1);

		if (!Config.ALT_ITEM_AUCTION_ENABLED)
		{
			info("Disabled by config.");
			return;
		}

		try (
		    var con = DatabaseFactory.getInstance().getConnection(); Statement statement = con.createStatement(); var rset = statement.executeQuery("SELECT auctionId FROM item_auction ORDER BY auctionId DESC LIMIT 0, 1"))
		{
			if (rset.next())
			{
				_auctionIds.set(rset.getInt(1) + 1);
			}
		}
		catch (final SQLException e)
		{
			warn("Failed loading auctions.", e);
		}

		final File file = new File(Config.DATAPACK_ROOT + "/data/stats/items/itemAuctions.xml");
		if (!file.exists())
		{
			warn("Missing itemAuctions.xml!");
			return;
		}

		final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(false);
		factory.setIgnoringComments(true);

		try
		{
			final Document doc = factory.newDocumentBuilder().parse(file);
			for (Node na = doc.getFirstChild(); na != null; na = na.getNextSibling())
			{
				if ("list".equalsIgnoreCase(na.getNodeName()))
				{
					for (Node nb = na.getFirstChild(); nb != null; nb = nb.getNextSibling())
					{
						if ("instance".equalsIgnoreCase(nb.getNodeName()))
						{
							final NamedNodeMap nab = nb.getAttributes();
							final int instanceId = Integer.parseInt(nab.getNamedItem("id").getNodeValue());

							if (_managerInstances.containsKey(instanceId))
							{
								throw new Exception("Dublicated instanceId " + instanceId);
							}

							final ItemAuctionInstance instance = new ItemAuctionInstance(instanceId, _auctionIds, nb);
							_managerInstances.put(instanceId, instance);
						}
					}
				}
			}
			info("Loaded " + _managerInstances.size() + " instance(s).");
		}
		catch (final Exception e)
		{
			warn("Failed loading auctions from xml.", e);
		}
	}

	public final void shutdown()
	{
		for (final ItemAuctionInstance instance : _managerInstances.values())
		{
			instance.shutdown();
		}
	}

	public final ItemAuctionInstance getManagerInstance(final int instanceId)
	{
		return _managerInstances.get(instanceId);
	}

	public final int getNextAuctionId()
	{
		return _auctionIds.getAndIncrement();
	}

	public static final void deleteAuction(final int auctionId) throws Exception
	{
		try (
		    var con = DatabaseFactory.getInstance().getConnection())
		{
			try (
			    var statement = con.prepareStatement("DELETE FROM item_auction WHERE auctionId=?"))
			{
				statement.setInt(1, auctionId);
				statement.execute();
			}

			try (
			    var statement = con.prepareStatement("DELETE FROM item_auction_bid WHERE auctionId=?"))
			{
				statement.setInt(1, auctionId);
				statement.execute();
			}
		}
		catch (final Exception e)
		{
		}
	}

	private static class SingletonHolder
	{
		protected static final ItemAuctionManager _instance = new ItemAuctionManager();
	}
}