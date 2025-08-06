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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import l2e.commons.log.LoggerObject;
import l2e.commons.util.Rnd;
import l2e.gameserver.Config;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.data.parser.MultiSellParser;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.items.multisell.Entry;
import l2e.gameserver.model.items.multisell.Ingredient;
import l2e.gameserver.model.items.multisell.ListContainer;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.strings.server.ServerStorage;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.CreatureSay;

public class WeeklyTraderManager extends LoggerObject
{
	private final List<Entry> _selectedEntries = new ArrayList<>();
	private final List<WeeklyTraderSpawn> _spawnList = new ArrayList<>();
	private final List<Npc> _npcList = new ArrayList<>();
	
	private static final int _activeTime = 604800000;
	private boolean _isTradingPeriod;
	private boolean _isAlwaysSpawn;
	private long _endingTime = 0;
	private long _startTime = 0;
	private int _validNpcId;
	
	public WeeklyTraderManager()
	{
		if (Config.WEEKLY_TRADER_ENABLE)
		{
			load(false);
			validatePeriod();
		}
	}
	
	public void validatePeriod()
	{
		_endingTime = 0;
		final int period = getWeekOfTradingPeriod();
		if (period == 0)
		{
			_isTradingPeriod = false;
			cleanEntries();
			generateNewEntries();
			saveEntries();
			createMultisell();
			scheduleStartPeriod();
		}
		else
		{
			_isTradingPeriod = true;
			info("Currently in Trading Period.");
			loadEntries();
			createMultisell();
			spawnNPC();
			scheduleEndPeriod(period == 1);
		}
	}
	
	private void startPeriod()
	{
		info("New trading period has been started!");
		for (final Player player : GameObjectsStorage.getPlayers())
		{
			if (player != null && player.isOnline())
			{
				player.sendPacket(new CreatureSay(0, Say2.ANNOUNCEMENT, ServerStorage.getInstance().getString(player.getLang(), "WeeklyTrader.TRADER"), ServerStorage.getInstance().getString(player.getLang(), "WeeklyTrader.PERIOD_START")));
			}
		}
		spawnNPC();
		_isTradingPeriod = true;
		final long duration = Config.WEEKLY_TRADER_DURATION * 60 * 1000;
		_endingTime = System.currentTimeMillis() + duration;
		info("Period will end in " + duration + " min(s).");
		ThreadPoolManager.getInstance().schedule(() ->
		{
			endPeriod();
		}, duration);
	}
	
	private void scheduleStartPeriod()
	{
		final Calendar thisWeek = getConfiguredCalendar();
		long nextStart;
		if (thisWeek.getTimeInMillis() > System.currentTimeMillis())
		{
			nextStart = thisWeek.getTimeInMillis();
		}
		else
		{
			nextStart = thisWeek.getTimeInMillis() + _activeTime;
		}
		_startTime = nextStart;
		info("Next Trading Period: " + new Date(nextStart));
		final long delay = nextStart - System.currentTimeMillis();
		ThreadPoolManager.getInstance().schedule(() ->
		{
			startPeriod();
		}, delay);
	}
	
	private void scheduleEndPeriod(boolean startedThisWeek)
	{
		final long duration = Config.WEEKLY_TRADER_DURATION * 60 * 1000;
		final Calendar thisWeek = getConfiguredCalendar();
		long endTime = thisWeek.getTimeInMillis() + duration;
		if (!startedThisWeek)
		{
			endTime -= _activeTime;
		}
		info("Period ends " + new Date(endTime));
		final long delay = endTime - System.currentTimeMillis();
		_endingTime = endTime;
		ThreadPoolManager.getInstance().schedule(() ->
		{
			endPeriod();
		}, delay);
	}
	
	private void endPeriod()
	{
		info("Period ended!");
		for (final Player player : GameObjectsStorage.getPlayers())
		{
			if (player != null && player.isOnline())
			{
				player.sendPacket(new CreatureSay(0, Say2.ANNOUNCEMENT, ServerStorage.getInstance().getString(player.getLang(), "WeeklyTrader.TRADER"), ServerStorage.getInstance().getString(player.getLang(), "WeeklyTrader.PERIOD_END")));
			}
		}
		_isTradingPeriod = false;
		if (!_npcList.isEmpty() && !_isAlwaysSpawn)
		{
			for (final Npc npc : _npcList)
			{
				if (npc != null)
				{
					npc.deleteMe();
				}
			}
			_npcList.clear();
		}
		validatePeriod();
	}

	private int getWeekOfTradingPeriod()
	{
		final long currentTime = System.currentTimeMillis();
		final long duration = Config.WEEKLY_TRADER_DURATION * 60 * 1000;
		final Calendar thisWeek = getConfiguredCalendar();
		boolean after = currentTime > thisWeek.getTimeInMillis();
		boolean beforeEnd = currentTime < (thisWeek.getTimeInMillis() + duration);
		if (after && beforeEnd)
		{
			return 1;
		}
		final long previousWeek = thisWeek.getTimeInMillis() - _activeTime;
		after = currentTime > previousWeek;
		beforeEnd = currentTime < (previousWeek + duration);
		if (after && beforeEnd)
		{
			return 2;
		}
		return 0;
	}
	
	private void spawnNPC()
	{
		if (_npcList.size() > 0)
		{
			return;
		}
		
		for (final WeeklyTraderSpawn spawn : _spawnList)
		{
			_validNpcId = spawn.getNpcId();
			_npcList.add(Quest.addSpawn(spawn.getNpcId(), spawn.getLocation().getX(), spawn.getLocation().getY(), spawn.getLocation().getZ(), spawn.getLocation().getHeading(), false, 0));
		}
	}
	
	private void generateNewEntries()
	{
		load(true);
	}
	
	public void load(boolean isParseItems)
	{
		Document doc = null;
		final File file = new File(Config.DATAPACK_ROOT, "data/stats/services/weeklyTrader.xml");
		if (!file.exists())
		{
			warn("weeklyTrader.xml file is missing.");
			return;
		}
		
		try
		{
			final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);
			doc = factory.newDocumentBuilder().parse(file);
		}
		catch (final Exception e)
		{
			e.printStackTrace();
		}
		
		try
		{
			if (isParseItems)
			{
				parseItemsInfo(doc);
			}
			else
			{
				parseNpcsInfo(doc);
			}
		}
		catch (final Exception e)
		{
			e.printStackTrace();
		}
		
	}
	
	public void parseNpcsInfo(Document doc)
	{
		_npcList.clear();
		for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node c = n.getFirstChild(); c != null; c = c.getNextSibling())
				{
					if ("spawnlist".equalsIgnoreCase(c.getNodeName()))
					{
						_isAlwaysSpawn = Boolean.parseBoolean(c.getAttributes().getNamedItem("isAlwaysSpawn").getNodeValue());
						for (Node d = c.getFirstChild(); d != null; d = d.getNextSibling())
						{
							if ("npc".equalsIgnoreCase(d.getNodeName()))
							{
								final int id = Integer.parseInt(d.getAttributes().getNamedItem("id").getNodeValue());
								final int x = Integer.parseInt(d.getAttributes().getNamedItem("x").getNodeValue());
								final int y = Integer.parseInt(d.getAttributes().getNamedItem("y").getNodeValue());
								final int z = Integer.parseInt(d.getAttributes().getNamedItem("z").getNodeValue());
								final int heading = Integer.parseInt(d.getAttributes().getNamedItem("heading").getNodeValue());
								_spawnList.add(new WeeklyTraderSpawn(id, new Location(x, y, z, heading)));
							}
						}
					}
				}
			}
		}
		info("loaded " + _spawnList.size() + " npc locations...");
		if (_isAlwaysSpawn)
		{
			spawnNPC();
		}
	}
	
	public void parseItemsInfo(Document doc)
	{
		info("selecting random entries...");
		_selectedEntries.clear();
		for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				NamedNodeMap attrs;
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("category".equalsIgnoreCase(d.getNodeName()))
					{
						attrs = d.getAttributes();
						final double chance = Integer.parseInt(attrs.getNamedItem("chance").getNodeValue());
						if (!Rnd.chance(chance))
						{
							continue;
						}
						
						final List<Item> entries = new ArrayList<>();
						for (Node item = d.getFirstChild(); item != null; item = item.getNextSibling())
						{
							if ("item".equalsIgnoreCase(item.getNodeName()))
							{
								final Item entry = new Item();
								for (Node ing = item.getFirstChild(); ing != null; ing = ing.getNextSibling())
								{
									if ("production".equalsIgnoreCase(ing.getNodeName()))
									{
										attrs = ing.getAttributes();
										final int id = Integer.parseInt(attrs.getNamedItem("itemId").getNodeValue());
										final long count = Long.parseLong(attrs.getNamedItem("count").getNodeValue());
										final Ingredient i = new Ingredient(id, count, 0, -1, null, null, false, false, false, 0, 0);
										entry.products.add(i);
									}
									else if ("ingredient".equalsIgnoreCase(ing.getNodeName()))
									{
										attrs = ing.getAttributes();
										final int id = Integer.parseInt(attrs.getNamedItem("itemId").getNodeValue());
										final long min = Long.parseLong(attrs.getNamedItem("minCount").getNodeValue());
										final long max = Long.parseLong(attrs.getNamedItem("maxCount").getNodeValue());
										final Ingredient i = new Ingredient(id, Rnd.get(min, max), 0, -1, null, null, false, false, false, 0, 0);
										entry.ingredients.add(i);
									}
								}
								entries.add(entry);
							}
						}
						final Item randomEntryOfThisCategory = entries.get(Rnd.get(entries.size()));
						final Entry e = new Entry(_selectedEntries.size() + 1);
						for (final Ingredient i : randomEntryOfThisCategory.ingredients)
						{
							e.addIngredient(i);
						}
						for (final Ingredient i : randomEntryOfThisCategory.products)
						{
							e.addProduct(i);
						}
						_selectedEntries.add(e);
					}
				}
			}
		}
	}
	
	private Calendar getConfiguredCalendar()
	{
		final Calendar c = Calendar.getInstance();
		c.set(Calendar.DAY_OF_WEEK, Config.WEEKLY_TRADER_DAY_OF_WEEK);
		c.set(Calendar.HOUR_OF_DAY, Config.WEEKLY_TRADER_HOUR_OF_DAY);
		c.set(Calendar.MINUTE, Config.WEEKLY_TRADER_MINUTE_OF_DAY);
		c.set(Calendar.SECOND, 0);
		return c;
	}
	
	private void createMultisell()
	{
		final ListContainer listContainer = new ListContainer(Config.WEEKLY_TRADER_MULTISELL_ID);
		for (final Entry e : _selectedEntries)
		{
			listContainer.getEntries().add(e);
		}
		int npcId = 0;
		if (!_spawnList.isEmpty())
		{
			for (final WeeklyTraderSpawn spawn : _spawnList)
			{
				if (spawn != null)
				{
					npcId = spawn.getNpcId();
					break;
				}
			}
		}
		listContainer.allowNpc(npcId);
		MultiSellParser.getInstance().getEntries().put(Config.WEEKLY_TRADER_MULTISELL_ID, listContainer);
	}
	
	private class Item
	{
		private final List<Ingredient> products = new ArrayList<>();
		private final List<Ingredient> ingredients = new ArrayList<>();
	}
	
	private class WeeklyTraderSpawn
	{
		protected final int _npcId;
		protected final Location _loc;
		
		public WeeklyTraderSpawn(int npcId, Location loc)
		{
			_npcId = npcId;
			_loc = loc;
		}
		
		public int getNpcId()
		{
			return _npcId;
		}
		
		public Location getLocation()
		{
			return _loc;
		}
	}
	
	public boolean isTradingPeriod()
	{
		return _isTradingPeriod;
	}
	
	public long getEndingTime()
	{
		return _endingTime;
	}
	
	public long getStartTime()
	{
		return _startTime;
	}
	
	private void cleanEntries()
	{
		try (
		    var con = DatabaseFactory.getInstance().getConnection();
		    PreparedStatement ps = con.prepareStatement("DELETE FROM weekly_trader_entries"))
		{
			final int k = ps.executeUpdate();
			info("Cleaned " + k + " entries from database.");
		}
		catch (final Exception e)
		{
			warn("Error cleaning entries from database.", e);
		}
	}
	
	private void loadEntries()
	{
		try (
		    var con = DatabaseFactory.getInstance().getConnection();
		    PreparedStatement ps = con.prepareStatement("SELECT * FROM weekly_trader_entries"))
		{
			try (ResultSet rset = ps.executeQuery())
			{
				int entryId = 0;
				while (rset.next())
				{
					final Entry e = new Entry(++entryId);
					final String productSplit[] = rset.getString("products").split(";");
					for (final String product : productSplit)
					{
						final int id = Integer.parseInt(product.split(",")[0]);
						final long count = Long.parseLong(product.split(",")[1]);
						e.addProduct(new Ingredient(id, count, 0, -1, null, null, false, false, false, 0, 0));
					}
					final String ingredintSplit[] = rset.getString("ingredients").split(";");
					for (final String ingredient : ingredintSplit)
					{
						final int id = Integer.parseInt(ingredient.split(",")[0]);
						final long count = Long.parseLong(ingredient.split(",")[1]);
						e.addIngredient(new Ingredient(id, count, 0, -1, null, null, false, false, false, 0, 0));
					}
					_selectedEntries.add(e);
				}
			}
		}
		catch (final Exception e)
		{
			warn("Error loading entries from database.", e);
		}
	}
	
	private void saveEntries()
	{
		for (final Entry e : _selectedEntries)
		{
			try (
			    var con = DatabaseFactory.getInstance().getConnection();
			    PreparedStatement ps = con.prepareStatement("INSERT INTO weekly_trader_entries (products,ingredients) values(?,?)"))
			{
				String products = "";
				String ingredients = "";
				for (final Ingredient product : e.getProducts())
				{
					products += product.getId() + ",";
					products += product.getCount() + ";";
				}
				for (final Ingredient ingredient : e.getIngredients())
				{
					ingredients += ingredient.getId() + ",";
					ingredients += ingredient.getCount() + ";";
				}
				ps.setString(1, products);
				ps.setString(2, ingredients);
				ps.execute();
				con.close();
				ps.close();
			}
			catch (final Exception e1)
			{
				warn("Error saving entries into database.", e1);
			}
		}
	}
	
	public int getValidNpcId()
	{
		return _validNpcId;
	}
	
	public static WeeklyTraderManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final WeeklyTraderManager _instance = new WeeklyTraderManager();
	}
}