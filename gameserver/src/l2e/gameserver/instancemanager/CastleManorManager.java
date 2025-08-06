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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import l2e.commons.dbutils.DbUtils;
import l2e.commons.util.Rnd;
import l2e.gameserver.Config;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.data.DocumentParser;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.model.Clan;
import l2e.gameserver.model.ClanMember;
import l2e.gameserver.model.Seed;
import l2e.gameserver.model.actor.templates.CropProcureTemplate;
import l2e.gameserver.model.actor.templates.SeedTemplate;
import l2e.gameserver.model.entity.Castle;
import l2e.gameserver.model.items.itemcontainer.ItemContainer;
import l2e.gameserver.model.stats.StatsSet;
import l2e.gameserver.network.SystemMessageId;

public final class CastleManorManager extends DocumentParser
{
	private static final String INSERT_PRODUCT = "INSERT INTO castle_manor_production VALUES (?, ?, ?, ?, ?, ?)";
	private static final String INSERT_CROP = "INSERT INTO castle_manor_procure VALUES (?, ?, ?, ?, ?, ?, ?)";
	
	private ManorMode _mode = ManorMode.APPROVED;
	private Calendar _nextModeChange = null;
	
	private static final Map<Integer, Seed> _seeds = new HashMap<>();
	private final Map<Integer, List<CropProcureTemplate>> _procure = new HashMap<>();
	private final Map<Integer, List<CropProcureTemplate>> _procureNext = new HashMap<>();
	private final Map<Integer, List<SeedTemplate>> _production = new HashMap<>();
	private final Map<Integer, List<SeedTemplate>> _productionNext = new HashMap<>();
	
	private enum ManorMode
	{
		DISABLED, MODIFIABLE, MAINTENANCE, APPROVED
	}
	
	public CastleManorManager()
	{
		if (Config.ALLOW_MANOR)
		{
			load();
			loadDb();
			
			final Calendar currentTime = Calendar.getInstance();
			final int hour = currentTime.get(Calendar.HOUR_OF_DAY);
			final int min = currentTime.get(Calendar.MINUTE);
			final int maintenanceMin = Config.ALT_MANOR_REFRESH_MIN + Config.ALT_MANOR_MAINTENANCE_MIN;
			
			if (((hour >= Config.ALT_MANOR_REFRESH_TIME) && (min >= maintenanceMin)) || (hour < Config.ALT_MANOR_APPROVE_TIME) || ((hour == Config.ALT_MANOR_APPROVE_TIME) && (min <= Config.ALT_MANOR_APPROVE_MIN)))
			{
				_mode = ManorMode.MODIFIABLE;
			}
			else if ((hour == Config.ALT_MANOR_REFRESH_TIME) && ((min >= Config.ALT_MANOR_REFRESH_MIN) && (min < maintenanceMin)))
			{
				_mode = ManorMode.MAINTENANCE;
			}
			
			scheduleModeChange();
			
			if (!Config.ALT_MANOR_SAVE_ALL_ACTIONS)
			{
				ThreadPoolManager.getInstance().scheduleAtFixedRate(this::storeMe, Config.ALT_MANOR_SAVE_PERIOD_RATE, Config.ALT_MANOR_SAVE_PERIOD_RATE);
			}
			info("Current mode: " + _mode.toString());
		}
		else
		{
			_mode = ManorMode.DISABLED;
			info("Manor system is deactivated.");
		}
	}
	
	@Override
	public final void load()
	{
		parseDatapackFile("data/stats/items/seeds.xml");
		info("Loaded " + _seeds.size() + " seeds.");
	}
	
	@Override
	protected void reloadDocument()
	{
	}
	
	@Override
	protected void parseDocument()
	{
		StatsSet set;
		NamedNodeMap attrs;
		Node att;
		for (Node n = getCurrentDocument().getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("castle".equalsIgnoreCase(d.getNodeName()))
					{
						final int castleId = parseInteger(d.getAttributes(), "id");
						for (Node c = d.getFirstChild(); c != null; c = c.getNextSibling())
						{
							if ("crop".equalsIgnoreCase(c.getNodeName()))
							{
								set = new StatsSet();
								set.set("castleId", castleId);
								
								attrs = c.getAttributes();
								for (int i = 0; i < attrs.getLength(); i++)
								{
									att = attrs.item(i);
									set.set(att.getNodeName(), att.getNodeValue());
								}
								_seeds.put(set.getInteger("seedId"), new Seed(set));
							}
						}
					}
				}
			}
		}
	}
	
	private final void loadDb()
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT * FROM castle_manor_production WHERE castle_id=?");
			final var stProcure = con.prepareStatement("SELECT * FROM castle_manor_procure WHERE castle_id=?");
			for (final Castle castle : CastleManager.getInstance().getCastles())
			{
				final int castleId = castle.getId();
				
				statement.clearParameters();
				stProcure.clearParameters();
				
				final List<SeedTemplate> pCurrent = new ArrayList<>();
				final List<SeedTemplate> pNext = new ArrayList<>();
				statement.setInt(1, castleId);
				final ResultSet rs = statement.executeQuery();
				while (rs.next())
				{
					final int seedId = rs.getInt("seed_id");
					if (_seeds.containsKey(seedId))
					{
						final SeedTemplate sp = new SeedTemplate(seedId, rs.getLong("can_produce"), rs.getLong("seed_price"), rs.getInt("start_produce"));
						if (rs.getBoolean("period"))
						{
							pNext.add(sp);
						}
						else
						{
							pCurrent.add(sp);
						}
					}
					else
					{
						warn("Unknown seed id: " + seedId + "!");
					}
				}
				_production.put(castleId, pCurrent);
				_productionNext.put(castleId, pNext);
				rs.close();
				
				final List<CropProcureTemplate> current = new ArrayList<>();
				final List<CropProcureTemplate> next = new ArrayList<>();
				stProcure.setInt(1, castleId);
				final var rs1 = stProcure.executeQuery();
				final Set<Integer> cropIds = getCropIds();
				while (rs1.next())
				{
					final int cropId = rs1.getInt("crop_id");
					if (cropIds.contains(cropId))
					{
						final CropProcureTemplate cp = new CropProcureTemplate(cropId, rs1.getLong("can_buy"), rs1.getInt("reward_type"), rs1.getLong("start_buy"), rs1.getLong("price"));
						if (rs1.getBoolean("period"))
						{
							next.add(cp);
						}
						else
						{
							current.add(cp);
						}
					}
					else
					{
						warn("Unknown crop id: " + cropId + "!");
					}
				}
				rs1.close();
				_procure.put(castleId, current);
				_procureNext.put(castleId, next);
			}
			stProcure.close();
		}
		catch (final Exception e)
		{
			warn("Unable to load manor data! " + e.getMessage());
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
	
	private final void scheduleModeChange()
	{
		_nextModeChange = Calendar.getInstance();
		_nextModeChange.set(Calendar.SECOND, 0);
		switch (_mode)
		{
			case MODIFIABLE :
				_nextModeChange.set(Calendar.HOUR_OF_DAY, Config.ALT_MANOR_APPROVE_TIME);
				_nextModeChange.set(Calendar.MINUTE, Config.ALT_MANOR_APPROVE_MIN);
				if (_nextModeChange.before(Calendar.getInstance()))
				{
					_nextModeChange.add(Calendar.DATE, 1);
				}
				break;
			case MAINTENANCE :
				_nextModeChange.set(Calendar.HOUR_OF_DAY, Config.ALT_MANOR_REFRESH_TIME);
				_nextModeChange.set(Calendar.MINUTE, Config.ALT_MANOR_REFRESH_MIN + Config.ALT_MANOR_MAINTENANCE_MIN);
				break;
			case APPROVED :
				_nextModeChange.set(Calendar.HOUR_OF_DAY, Config.ALT_MANOR_REFRESH_TIME);
				_nextModeChange.set(Calendar.MINUTE, Config.ALT_MANOR_REFRESH_MIN);
				break;
		}
		
		final long milliToEnd = getMillisToPeriodEnd();
		info("Period ends at " + new Date(milliToEnd + System.currentTimeMillis()));
		
		ThreadPoolManager.getInstance().schedule(this::changeMode, getMillisToPeriodEnd());
		info("Next mode change: " + new Date(_nextModeChange.getTimeInMillis()));
	}
	
	public long getMillisToPeriodEnd()
	{
		return _nextModeChange.getTimeInMillis() - Calendar.getInstance().getTimeInMillis();
	}
	
	public final void changeMode()
	{
		switch (_mode)
		{
			case APPROVED :
			{
				_mode = ManorMode.MAINTENANCE;
				for (final Castle castle : CastleManager.getInstance().getCastles())
				{
					final Clan owner = castle.getOwner();
					if (owner == null)
					{
						continue;
					}
					
					final int castleId = castle.getId();
					final ItemContainer cwh = owner.getWarehouse();
					for (final CropProcureTemplate crop : _procure.get(castleId))
					{
						if (crop.getStartAmount() > 0)
						{
							if (crop.getStartAmount() != crop.getAmount())
							{
								long count = (long) ((crop.getStartAmount() - crop.getAmount()) * 0.9);
								if ((count < 1) && (Rnd.nextInt(99) < 90))
								{
									count = 1;
								}
								
								if (count > 0)
								{
									cwh.addItem("Manor", getSeedByCrop(crop.getId()).getMatureId(), count, null, null);
								}
							}
							
							if (crop.getAmount() > 0)
							{
								castle.addToTreasuryNoTax(crop.getAmount() * crop.getPrice());
							}
						}
					}
					
					final List<SeedTemplate> _nextProduction = _productionNext.get(castleId);
					final List<CropProcureTemplate> _nextProcure = _procureNext.get(castleId);
					
					_production.put(castleId, _nextProduction);
					_procure.put(castleId, _nextProcure);
					
					if (castle.getTreasury() < getManorCost(castleId, false))
					{
						_productionNext.put(castleId, Collections.emptyList());
						_procureNext.put(castleId, Collections.emptyList());
					}
					else
					{
						final List<SeedTemplate> production = new ArrayList<>(_nextProduction);
						for (final SeedTemplate s : production)
						{
							s.setAmount(s.getStartAmount());
						}
						_productionNext.put(castleId, production);
						
						final List<CropProcureTemplate> procure = new ArrayList<>(_nextProcure);
						for (final CropProcureTemplate cr : procure)
						{
							cr.setAmount(cr.getStartAmount());
						}
						_procureNext.put(castleId, procure);
					}
				}
				storeMe();
				break;
			}
			case MAINTENANCE :
			{
				for (final Castle castle : CastleManager.getInstance().getCastles())
				{
					final Clan owner = castle.getOwner();
					if (owner != null)
					{
						final ClanMember clanLeader = owner.getLeader();
						if ((clanLeader != null) && clanLeader.isOnline())
						{
							clanLeader.getPlayerInstance().sendPacket(SystemMessageId.THE_MANOR_INFORMATION_HAS_BEEN_UPDATED);
						}
					}
				}
				_mode = ManorMode.MODIFIABLE;
				break;
			}
			case MODIFIABLE :
			{
				_mode = ManorMode.APPROVED;
				
				for (final Castle castle : CastleManager.getInstance().getCastles())
				{
					final Clan owner = castle.getOwner();
					if (owner == null)
					{
						continue;
					}
					
					int slots = 0;
					final int castleId = castle.getId();
					final ItemContainer cwh = owner.getWarehouse();
					for (final CropProcureTemplate crop : _procureNext.get(castleId))
					{
						if ((crop.getStartAmount() > 0) && (cwh.getItemsByItemId(getSeedByCrop(crop.getId()).getMatureId()) == null))
						{
							slots++;
						}
					}
					
					final long manorCost = getManorCost(castleId, true);
					if (!cwh.validateCapacity(slots) && (castle.getTreasury() < manorCost))
					{
						_productionNext.get(castleId).clear();
						_procureNext.get(castleId).clear();
						
						final ClanMember clanLeader = owner.getLeader();
						if ((clanLeader != null) && clanLeader.isOnline())
						{
							clanLeader.getPlayerInstance().sendPacket(SystemMessageId.THE_AMOUNT_IS_NOT_SUFFICIENT_AND_SO_THE_MANOR_IS_NOT_IN_OPERATION);
						}
					}
					else
					{
						castle.addToTreasuryNoTax(-manorCost);
					}
				}
				
				if (Config.ALT_MANOR_SAVE_ALL_ACTIONS)
				{
					storeMe();
				}
				break;
			}
		}
		
		info("Manor mode changed to " + _mode.toString() + "!");
		scheduleModeChange();
	}
	
	public final void setNextSeedProduction(List<SeedTemplate> list, int castleId)
	{
		_productionNext.put(castleId, list);
		if (Config.ALT_MANOR_SAVE_ALL_ACTIONS)
		{
			Connection con = null;
			PreparedStatement statement = null;
			try
			{
				con = DatabaseFactory.getInstance().getConnection();
				statement = con.prepareStatement("DELETE FROM castle_manor_production WHERE castle_id = ? AND period = 1");
				statement.setInt(1, castleId);
				statement.executeUpdate();
				statement.close();
				
				statement = con.prepareStatement(INSERT_PRODUCT);
				if (!list.isEmpty())
				{
					for (final SeedTemplate sp : list)
					{
						statement.setInt(1, castleId);
						statement.setInt(2, sp.getId());
						statement.setLong(3, sp.getAmount());
						statement.setLong(4, sp.getStartAmount());
						statement.setLong(5, sp.getPrice());
						statement.setBoolean(6, true);
						statement.addBatch();
					}
					statement.executeBatch();
				}
			}
			catch (final Exception e)
			{
				error("Unable to store manor data! " + e.getMessage());
			}
			finally
			{
				DbUtils.closeQuietly(con, statement);
			}
		}
	}
	
	public final void setNextCropProcure(List<CropProcureTemplate> list, int castleId)
	{
		_procureNext.put(castleId, list);
		if (Config.ALT_MANOR_SAVE_ALL_ACTIONS)
		{
			Connection con = null;
			PreparedStatement statement = null;
			try
			{
				con = DatabaseFactory.getInstance().getConnection();
				statement = con.prepareStatement("DELETE FROM castle_manor_procure WHERE castle_id = ? AND period = 1");
				statement.setInt(1, castleId);
				statement.executeUpdate();
				statement.close();
				
				statement = con.prepareStatement(INSERT_CROP);
				if (!list.isEmpty())
				{
					for (final CropProcureTemplate cp : list)
					{
						statement.setInt(1, castleId);
						statement.setInt(2, cp.getId());
						statement.setLong(3, cp.getAmount());
						statement.setLong(4, cp.getStartAmount());
						statement.setLong(5, cp.getPrice());
						statement.setInt(6, cp.getReward());
						statement.setBoolean(7, true);
						statement.addBatch();
					}
					statement.executeBatch();
				}
			}
			catch (final Exception e)
			{
				error("Unable to store manor data! " + e.getMessage());
			}
			finally
			{
				DbUtils.closeQuietly(con, statement);
			}
		}
	}
	
	public final void updateCurrentProduction(int castleId, Collection<SeedTemplate> items)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE castle_manor_production SET can_produce = ? WHERE castle_id = ? AND seed_id = ? AND period = 0");
			for (final SeedTemplate sp : items)
			{
				statement.setLong(1, sp.getAmount());
				statement.setInt(2, castleId);
				statement.setInt(3, sp.getId());
				statement.addBatch();
			}
			statement.executeBatch();
		}
		catch (final Exception e)
		{
			info("Unable to store manor data! " + e.getMessage());
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
	
	public final void updateCurrentProcure(int castleId, Collection<CropProcureTemplate> items)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE castle_manor_procure SET can_buy = ? WHERE castle_id = ? AND crop_id = ? AND period = 0");
			for (final CropProcureTemplate sp : items)
			{
				statement.setLong(1, sp.getAmount());
				statement.setInt(2, castleId);
				statement.setInt(3, sp.getId());
				statement.addBatch();
			}
			statement.executeBatch();
		}
		catch (final Exception e)
		{
			info("Unable to store manor data! " + e.getMessage());
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
	
	public final List<SeedTemplate> getSeedProduction(int castleId, boolean nextPeriod)
	{
		return (nextPeriod) ? _productionNext.get(castleId) : _production.get(castleId);
	}
	
	public final SeedTemplate getSeedProduct(int castleId, int seedId, boolean nextPeriod)
	{
		for (final SeedTemplate sp : getSeedProduction(castleId, nextPeriod))
		{
			if (sp.getId() == seedId)
			{
				return sp;
			}
		}
		return null;
	}
	
	public final List<CropProcureTemplate> getCropProcure(int castleId, boolean nextPeriod)
	{
		return (nextPeriod) ? _procureNext.get(castleId) : _procure.get(castleId);
	}
	
	public final CropProcureTemplate getCropProcure(int castleId, int cropId, boolean nextPeriod)
	{
		for (final CropProcureTemplate cp : getCropProcure(castleId, nextPeriod))
		{
			if (cp.getId() == cropId)
			{
				return cp;
			}
		}
		return null;
	}
	
	public final long getManorCost(int castleId, boolean nextPeriod)
	{
		final List<CropProcureTemplate> procure = getCropProcure(castleId, nextPeriod);
		final List<SeedTemplate> production = getSeedProduction(castleId, nextPeriod);
		
		long total = 0;
		for (final SeedTemplate seed : production)
		{
			final Seed s = getSeed(seed.getId());
			total += (s == null) ? 1 : (s.getSeedReferencePrice() * seed.getStartAmount());
		}
		for (final CropProcureTemplate crop : procure)
		{
			total += (crop.getPrice() * crop.getStartAmount());
		}
		return total;
	}
	
	public final boolean storeMe()
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("DELETE FROM castle_manor_production");
			statement.executeUpdate();
			statement.close();
			
			statement = con.prepareStatement(INSERT_PRODUCT);
			for (final Map.Entry<Integer, List<SeedTemplate>> entry : _production.entrySet())
			{
				for (final SeedTemplate sp : entry.getValue())
				{
					statement.setInt(1, entry.getKey());
					statement.setInt(2, sp.getId());
					statement.setLong(3, sp.getAmount());
					statement.setLong(4, sp.getStartAmount());
					statement.setLong(5, sp.getPrice());
					statement.setBoolean(6, false);
					statement.addBatch();
				}
			}
			
			for (final Map.Entry<Integer, List<SeedTemplate>> entry : _productionNext.entrySet())
			{
				for (final SeedTemplate sp : entry.getValue())
				{
					statement.setInt(1, entry.getKey());
					statement.setInt(2, sp.getId());
					statement.setLong(3, sp.getAmount());
					statement.setLong(4, sp.getStartAmount());
					statement.setLong(5, sp.getPrice());
					statement.setBoolean(6, true);
					statement.addBatch();
				}
			}
			
			statement.executeBatch();
			statement.close();
			
			statement = con.prepareStatement("DELETE FROM castle_manor_procure");
			statement.executeUpdate();
			statement.close();
			
			statement = con.prepareStatement(INSERT_CROP);
			for (final Map.Entry<Integer, List<CropProcureTemplate>> entry : _procure.entrySet())
			{
				for (final CropProcureTemplate cp : entry.getValue())
				{
					statement.setInt(1, entry.getKey());
					statement.setInt(2, cp.getId());
					statement.setLong(3, cp.getAmount());
					statement.setLong(4, cp.getStartAmount());
					statement.setLong(5, cp.getPrice());
					statement.setInt(6, cp.getReward());
					statement.setBoolean(7, false);
					statement.addBatch();
				}
			}
			
			for (final Map.Entry<Integer, List<CropProcureTemplate>> entry : _procureNext.entrySet())
			{
				for (final CropProcureTemplate cp : entry.getValue())
				{
					statement.setInt(1, entry.getKey());
					statement.setInt(2, cp.getId());
					statement.setLong(3, cp.getAmount());
					statement.setLong(4, cp.getStartAmount());
					statement.setLong(5, cp.getPrice());
					statement.setInt(6, cp.getReward());
					statement.setBoolean(7, true);
					statement.addBatch();
				}
			}
			statement.executeBatch();
			return true;
		}
		catch (final Exception e)
		{
			error("Unable to store manor data! " + e.getMessage());
			return false;
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
	
	public final void resetManorData(int castleId)
	{
		if (!Config.ALLOW_MANOR)
		{
			return;
		}
		
		_procure.get(castleId).clear();
		_procureNext.get(castleId).clear();
		_production.get(castleId).clear();
		_productionNext.get(castleId).clear();
		
		if (Config.ALT_MANOR_SAVE_ALL_ACTIONS)
		{
			Connection con = null;
			PreparedStatement statement = null;
			try
			{
				con = DatabaseFactory.getInstance().getConnection();
				statement = con.prepareStatement("DELETE FROM castle_manor_production WHERE castle_id = ?");
				statement.setInt(1, castleId);
				statement.executeUpdate();
				statement.close();
				
				statement = con.prepareStatement("DELETE FROM castle_manor_procure WHERE castle_id = ?");
				statement.setInt(1, castleId);
				statement.executeUpdate();
			}
			catch (final Exception e)
			{
				error("Unable to store manor data! " + e.getMessage());
			}
			finally
			{
				DbUtils.closeQuietly(con, statement);
			}
		}
	}
	
	public final boolean isUnderMaintenance()
	{
		return _mode.equals(ManorMode.MAINTENANCE);
	}
	
	public final boolean isManorApproved()
	{
		return _mode.equals(ManorMode.APPROVED);
	}
	
	public final boolean isModifiablePeriod()
	{
		return _mode.equals(ManorMode.MODIFIABLE);
	}
	
	public final String getCurrentModeName()
	{
		return _mode.toString();
	}
	
	public final String getNextModeChange()
	{
		return new SimpleDateFormat("dd/MM HH:mm:ss").format(_nextModeChange.getTime());
	}
	
	public final List<Seed> getCrops()
	{
		final List<Seed> seeds = new ArrayList<>();
		final List<Integer> cropIds = new ArrayList<>();
		for (final Seed seed : _seeds.values())
		{
			if (!cropIds.contains(seed.getCropId()))
			{
				seeds.add(seed);
				cropIds.add(seed.getCropId());
			}
		}
		cropIds.clear();
		return seeds;
	}
	
	public final Set<Seed> getSeedsForCastle(int castleId)
	{
		return _seeds.values().stream().filter(s -> s.getCastleId() == castleId).collect(Collectors.toSet());
	}
	
	public final Set<Integer> getSeedIds()
	{
		return _seeds.keySet();
	}
	
	public final Set<Integer> getCropIds()
	{
		return _seeds.values().stream().map(Seed::getCropId).collect(Collectors.toSet());
	}
	
	public final Seed getSeed(int seedId)
	{
		return _seeds.get(seedId);
	}
	
	public final Seed getSeedByCrop(int cropId, int castleId)
	{
		for (final Seed s : getSeedsForCastle(castleId))
		{
			if (s.getCropId() == cropId)
			{
				return s;
			}
		}
		return null;
	}
	
	public final Seed getSeedByCrop(int cropId)
	{
		for (final Seed s : _seeds.values())
		{
			if (s.getCropId() == cropId)
			{
				return s;
			}
		}
		return null;
	}
	
	public static final CastleManorManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final CastleManorManager _instance = new CastleManorManager();
	}
}