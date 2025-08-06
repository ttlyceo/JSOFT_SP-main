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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import l2e.commons.dbutils.DbUtils;
import l2e.gameserver.Config;
import l2e.gameserver.data.DocumentParser;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.handler.voicedcommandhandlers.IVoicedCommandHandler;
import l2e.gameserver.handler.voicedcommandhandlers.VoicedCommandHandler;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.reflection.ReflectionNameTemplate;
import l2e.gameserver.model.actor.templates.reflection.ReflectionTemplate;
import l2e.gameserver.model.actor.templates.reflection.ReflectionWorld;
import l2e.gameserver.model.entity.Reflection;
import l2e.gameserver.model.stats.StatsSet;

public class ReflectionManager extends DocumentParser
{
	private final Map<Integer, Reflection> _reflectionList = new ConcurrentHashMap<>();
	private final Map<Integer, ReflectionWorld> _reflectionWorlds = new ConcurrentHashMap<>();
	private int _dynamic = 300000;
	
	private final Map<Integer, ReflectionNameTemplate> _reflectionNames = new HashMap<>();
	private final Map<Integer, Map<Integer, Long>> _playerRefTimes = new ConcurrentHashMap<>();
	private final Map<String, Map<Integer, Long>> _hwidRefTimes = new ConcurrentHashMap<>();
	
	private final ReadWriteLock lock = new ReentrantReadWriteLock();
	private final Lock readLock = lock.readLock();
	
	public static Reflection DEFAULT = null;
	
	protected ReflectionManager()
	{
		load();
		DEFAULT = new Reflection(0, "Default");
		
	}
	
	@Override
	public void load()
	{
		_reflectionNames.clear();
		parseDatapackFile("data/stats/reflectionNames.xml");
		loadData();
		_log.info(getClass().getSimpleName() + ": Loaded " + _reflectionNames.size() + " reflection names.");
	}
	
	@Override
	protected void reloadDocument()
	{
	}
	
	public void loadData()
	{
		final var now = System.currentTimeMillis();
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("DELETE FROM character_instance_time WHERE time > 0 AND time < ?");
			statement.setLong(1, now);
			statement.execute();
			statement.close();
			
			statement = con.prepareStatement("DELETE FROM character_hwid_instance_time WHERE time > 0 AND time < ?");
			statement.setLong(1, now);
			statement.execute();
			statement.close();
			
			statement = con.prepareStatement("SELECT * FROM character_instance_time");
			rset = statement.executeQuery();
			while (rset.next())
			{
				final var charId = rset.getInt("charId");
				if (!_playerRefTimes.containsKey(charId))
				{
					_playerRefTimes.put(charId, new ConcurrentHashMap<>());
				}
				_playerRefTimes.get(charId).put(rset.getInt("instanceId"), rset.getLong("time"));
			}
			rset.close();
			statement.close();
			
			statement = con.prepareStatement("SELECT * FROM character_hwid_instance_time");
			rset = statement.executeQuery();
			while (rset.next())
			{
				final var hwid = rset.getString("hwid");
				if (!_hwidRefTimes.containsKey(hwid))
				{
					_hwidRefTimes.put(hwid, new ConcurrentHashMap<>());
				}
				_hwidRefTimes.get(hwid).put(rset.getInt("instanceId"), rset.getLong("time"));
			}
			rset.close();
			statement.close();
		}
		catch (final Exception e)
		{
			warn("Could not restore instance time : " + e.getMessage());
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
	}
	
	public long getReflectionTime(Player player, int id)
	{
		final var single = _playerRefTimes.get(player.getObjectId()).containsKey(id) ? _playerRefTimes.get(player.getObjectId()).get(id) : -1;
		final var hwid = _hwidRefTimes.get(player.getHWID()).containsKey(id) ? _hwidRefTimes.get(player.getHWID()).get(id) : -1;
		return hwid > 0 ? hwid : single;
	}
	
	public Map<Integer, Long> getAllReflectionTimes(Player player)
	{
		final Map<Integer, Long> reflections = new HashMap<>();
		reflections.putAll(_playerRefTimes.get(player.getObjectId()));
		if (!_hwidRefTimes.isEmpty())
		{
			final var list = _hwidRefTimes.get(player.getHWID());
			for (final var id : list.keySet())
			{
				if (!reflections.containsKey(id))
				{
					reflections.put(id, list.get(id));
				}
			}
		}
		return reflections;
	}

	public List<Integer> getLockedReflectionList(Player player)
	{
		final List<Integer> result = new ArrayList<>();
		final var refTimes = getAllReflectionTimes(player);
		if (refTimes != null)
		{
			for (final int reflectionId : refTimes.keySet())
			{
				final long remainingTime = (refTimes.get(reflectionId) - System.currentTimeMillis()) / 1000;
				if (remainingTime > 60)
				{
					result.add(reflectionId);
				}
			}
		}
		return result;
	}
	
	public void setReflectionTime(Player player, int playerObjId, int id, long time, boolean isHwidCheck)
	{
		if (player != null)
		{
			player.getListeners().onReflectionFinish(id);
			player.getCounters().addAchivementInfo("reflectionById", id, -1, false, false, false);
			player.getCounters().addAchivementInfo("reflectionsFinish", 0, -1, false, false, false);
			if (Config.ALLOW_DAILY_TASKS)
			{
				if (player.getActiveDailyTasks() != null)
				{
					for (final var taskTemplate : player.getActiveDailyTasks())
					{
						if (taskTemplate.getType().equalsIgnoreCase("Reflection") && !taskTemplate.isComplete())
						{
							final var task = DailyTaskManager.getInstance().getDailyTask(taskTemplate.getId());
							if (task.getReflectionId() == id)
							{
								taskTemplate.setIsComplete(true);
								player.updateDailyStatus(taskTemplate);
								final IVoicedCommandHandler vch = VoicedCommandHandler.getInstance().getHandler("missions");
								if (vch != null)
								{
									player.updateDailyStatus(taskTemplate);
									vch.useVoicedCommand("missions", player, null);
								}
							}
						}
					}
				}
			}
		}
		
		if (player != null && player.hasPremiumBonus())
		{
			final long nextTimte = time - System.currentTimeMillis();
			time = (long) (System.currentTimeMillis() + (nextTimte * player.getPremiumBonus().getReflectionReduce()));
		}
		
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("INSERT INTO character_instance_time (charId,instanceId,time) values (?,?,?) ON DUPLICATE KEY UPDATE time=?");
			statement.setInt(1, playerObjId);
			statement.setInt(2, id);
			statement.setLong(3, time);
			statement.setLong(4, time);
			statement.execute();
			statement.close();
			_playerRefTimes.get(playerObjId).put(id, time);
			
			if (player != null && isHwidCheck)
			{
				statement = con.prepareStatement("INSERT INTO character_hwid_instance_time (hwid,instanceId,time) values (?,?,?) ON DUPLICATE KEY UPDATE time=?");
				statement.setString(1, player.getHWID());
				statement.setInt(2, id);
				statement.setLong(3, time);
				statement.setLong(4, time);
				statement.execute();
				_hwidRefTimes.get(player.getHWID()).put(id, time);
			}
		}
		catch (final Exception e)
		{
			_log.warn(getClass().getSimpleName() + ": Could not insert character reflection time data: " + e.getMessage());
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
	
	public void restoreReflectionTimes(Player player)
	{
		if (!_playerRefTimes.containsKey(player.getObjectId()))
		{
			_playerRefTimes.put(player.getObjectId(), new ConcurrentHashMap<>());
		}
		
		if (!_hwidRefTimes.containsKey(player.getHWID()))
		{
			_hwidRefTimes.put(player.getHWID(), new ConcurrentHashMap<>());
		}
	}
	
	public void deleteReflectionTime(Player player, int id)
	{
		Connection con = null;
		PreparedStatement statement = null;
		final ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("DELETE FROM character_instance_time WHERE charId=? AND instanceId=?");
			statement.setInt(1, player.getObjectId());
			statement.setInt(2, id);
			statement.execute();
			statement.close();
			_playerRefTimes.get(player.getObjectId()).remove(id);
			
			statement = con.prepareStatement("DELETE FROM character_hwid_instance_time WHERE hwid=? AND instanceId=?");
			statement.setString(1, player.getHWID());
			statement.setInt(2, id);
			statement.execute();
			_hwidRefTimes.get(player.getHWID()).remove(id);
		}
		catch (final Exception e)
		{
			warn("deleteReflectionTime()", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
	}
	
	public String getReflectionName(Player player, int id)
	{
		if (_reflectionNames.containsKey(id))
		{
			final var tpl = _reflectionNames.get(id);
			if (tpl != null)
			{
				return tpl.getName(player.getLang());
			}
		}
		return ("UnknownInstance");
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
					if ("instance".equals(d.getNodeName()))
					{
						attrs = d.getAttributes();
						final StatsSet params = new StatsSet();
						for (final String lang : Config.MULTILANG_ALLOWED)
						{
							if (lang != null)
							{
								final String name = "name" + lang.substring(0, 1).toUpperCase() + lang.substring(1);
								params.set(name, attrs.getNamedItem(name) != null ? attrs.getNamedItem(name).getNodeValue() : attrs.getNamedItem("nameEn") != null ? attrs.getNamedItem("nameEn").getNodeValue() : "");
							}
						}
						_reflectionNames.put(parseInteger(attrs, "id"), new ReflectionNameTemplate(params));
					}
				}
			}
		}
	}
	
	public void addWorld(ReflectionWorld world)
	{
		_reflectionWorlds.put(world.getReflectionId(), world);
	}
	
	public ReflectionWorld getWorld(int reflectionId)
	{
		return _reflectionWorlds.get(reflectionId);
	}
	
	public ReflectionWorld getPlayerWorld(Player player)
	{
		for (final var temp : _reflectionWorlds.values())
		{
			if ((temp != null) && (temp.isAllowed(player.getObjectId())))
			{
				return temp;
			}
		}
		return null;
	}
	
	public void destroyRef(int reflectionId)
	{
		if (reflectionId <= 0)
		{
			return;
		}
		
		final var temp = _reflectionList.get(reflectionId);
		if (temp != null)
		{
			_reflectionList.remove(reflectionId);
			if (_reflectionWorlds.containsKey(reflectionId))
			{
				_reflectionWorlds.remove(reflectionId);
			}
		}
	}
	
	public Reflection getReflection(int reflectionId)
	{
		return _reflectionList.get(reflectionId);
	}
	
	public Map<Integer, Reflection> getReflections()
	{
		return _reflectionList;
	}
	
	public Reflection addReflection(Reflection ref)
	{
		DEFAULT = ref;
		return _reflectionList.put(ref.getId(), ref);
	}
	
	public int getPlayerReflection(int objectId)
	{
		for (final var temp : _reflectionList.values())
		{
			if (temp != null && temp.containsPlayer(objectId))
			{
				return temp.getId();
			}
		}
		return 0;
	}
	
	public Reflection getPlayerRef(int objectId)
	{
		for (final var temp : _reflectionList.values())
		{
			if (temp != null && temp.containsPlayer(objectId))
			{
				return temp;
			}
		}
		return DEFAULT;
	}
	
	public int createReflection()
	{
		_dynamic = 1;
		while (getReflection(_dynamic) != null)
		{
			_dynamic++;
			if (_dynamic == Integer.MAX_VALUE)
			{
				_log.warn("ReflectionManager: More then " + (Integer.MAX_VALUE - 300000) + " reflections created");
				_dynamic = 300000;
			}
		}
		final var ref = new Reflection(_dynamic);
		_reflectionList.put(_dynamic, ref);
		return _dynamic;
	}
	
	public Reflection createRef()
	{
		_dynamic = 1;
		while (getReflection(_dynamic) != null)
		{
			_dynamic++;
			if (_dynamic == Integer.MAX_VALUE)
			{
				_log.warn("ReflectionManager: More then " + (Integer.MAX_VALUE - 300000) + " reflections created");
				_dynamic = 300000;
			}
		}
		final var ref = new Reflection(_dynamic);
		_reflectionList.put(_dynamic, ref);
		return ref;
	}
	
	public boolean createReflection(int id)
	{
		if (getReflection(id) != null)
		{
			return false;
		}
		
		if ((id <= 0) || (id >= 300000) || reflectionExist(id))
		{
			return false;
		}
		
		final var ref = new Reflection(id);
		_reflectionList.put(id, ref);
		return true;
	}
	
	public boolean createReflectionFromTemplate(int id, String template)
	{
		if (getReflection(id) != null)
		{
			return false;
		}
		
		if ((id <= 0) || (id >= 300000) || reflectionExist(id))
		{
			return false;
		}
		
		final var ref = new Reflection(id);
		_reflectionList.put(id, ref);
		ref.loadInstanceTemplate(template);
		return true;
	}
	
	public Reflection createDynamicReflection(String template)
	{
		while (getReflection(_dynamic) != null)
		{
			_dynamic++;
			if (_dynamic == Integer.MAX_VALUE)
			{
				_log.warn(getClass().getSimpleName() + ": More then " + (Integer.MAX_VALUE - 300000) + " reflections created");
				_dynamic = 300000;
			}
		}
		final var ref = new Reflection(_dynamic);
		_reflectionList.put(_dynamic, ref);
		if (template != null)
		{
			ref.loadInstanceTemplate(template);
		}
		return ref;
	}
	
	public Reflection createDynamicReflection(ReflectionTemplate template)
	{
		while (getReflection(_dynamic) != null)
		{
			_dynamic++;
			if (_dynamic == Integer.MAX_VALUE)
			{
				_log.warn(getClass().getSimpleName() + ": More then " + (Integer.MAX_VALUE - 300000) + " reflections created");
				_dynamic = 300000;
			}
		}
		final var ref = new Reflection(_dynamic);
		_reflectionList.put(_dynamic, ref);
		if (template != null)
		{
			ref.loadReflectionTemplate(template);
		}
		return ref;
	}
	
	public boolean reflectionExist(final int reflectionId)
	{
		return _reflectionList.get(reflectionId) != null;
	}
	
	public int getCountByIzId(int izId)
	{
		readLock.lock();
		try
		{
			int i = 0;
			for (final var r : _reflectionWorlds.values())
			{
				if (r != null && r.getTemplateId() == izId)
				{
					i++;
				}
			}
			return i;
		}
		finally
		{
			readLock.unlock();
		}
	}
	
	public static final ReflectionManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final ReflectionManager _instance = new ReflectionManager();
	}
}