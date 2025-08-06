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
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import l2e.commons.dbutils.DbUtils;
import l2e.commons.log.LoggerObject;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.listener.AbstractFunctions;
import l2e.gameserver.listener.ScriptListenerLoader;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.quest.Quest;

public class QuestManager extends LoggerObject
{
	private final Map<String, Quest> _quests = new ConcurrentHashMap<>();
	private final Map<String, AbstractFunctions> _scripts = new ConcurrentHashMap<>();
	private final Map<String, List<Integer>> _hwidList = new ConcurrentHashMap<>();
	
	protected QuestManager()
	{
		_hwidList.clear();
		loadHwidList();
		checkTimeTask();
	}
	
	public final boolean reload(String questFolder)
	{
		final Quest q = getQuest(questFolder);
		if (q == null)
		{
			return false;
		}
		return q.reload();
	}
	
	public final boolean reload(int questId)
	{
		final Quest q = getQuest(questId);
		if (q == null)
		{
			return false;
		}
		return q.reload();
	}
	
	public final void reloadAllQuests()
	{
		info("Reloading Server Scripts");
		
		for (final Quest quest : _quests.values())
		{
			if (quest != null)
			{
				quest.unload(false);
			}
		}
		_quests.clear();
		
		try
		{
			//TODO: Очищаем перед перезагрузкой.
			ScriptListenerLoader.getInstance().clear();
			ScriptListenerLoader.getInstance().executeScriptList();
		}
		catch (final Exception e)
		{
			warn("Failed executing script list!", e);
		}
	}
	
	public final void save()
	{
		for (final Quest q : _quests.values())
		{
			q.saveGlobalData();
		}
	}
	
	public final Quest getQuest(String name)
	{
		return _quests.get(name);
	}
	
	public final Quest getQuest(int questId)
	{
		for (final Quest q : _quests.values())
		{
			if (q.getId() == questId)
			{
				return q;
			}
		}
		return null;
	}
	
	public final void addQuest(Quest newQuest)
	{
		if (newQuest == null)
		{
			throw new IllegalArgumentException("Quest argument cannot be null");
		}
		final Quest old = _quests.get(newQuest.getName());
		
		if (old != null)
		{
			old.unload();
		}
		_quests.put(newQuest.getName(), newQuest);
	}
	
	public final boolean removeQuest(Quest q)
	{
		return _quests.remove(q.getName()) != null;
	}
	
	public Iterable<Quest> getQuests()
	{
		return _quests.values();
	}
	
	public boolean unload(Quest ms)
	{
		ms.saveGlobalData();
		return removeQuest(ms);
	}
	
	public final void addScript(AbstractFunctions script)
	{
		if (!_scripts.containsKey(script.toString()))
		{
			_scripts.put(script.toString(), script);
		}
	}
	
	public final void reloadScript(String name)
	{
		final var script = _scripts.get(name);
		if (script != null)
		{
			script.onReload();
		}
	}
	
	public List<AbstractFunctions> getAllScripts()
	{
		return _scripts.values().stream().toList();
	}

	private void checkTimeTask()
	{
		final Calendar currentTime = Calendar.getInstance();
		
		final long lastUpdate = ServerVariables.getLong("Quest_HwidMap", 0);
		if (currentTime.getTimeInMillis() > lastUpdate)
		{
			cleanHwidList();
		}
	}
	
	private void loadHwidList()
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT * FROM character_quests_hwid_data");
			rset = statement.executeQuery();
			while (rset.next())
			{
				List<Integer> quests;
				if ((quests = _hwidList.get(rset.getString("hwid"))) == null)
				{
					_hwidList.put(rset.getString("hwid"), quests = new ArrayList<>());
				}
				quests.add(rset.getInt("questId"));
				_hwidList.put(rset.getString("hwid"), quests);
			}
			info("Loaded " + _hwidList.size() + " players quest hwid map.");
		}
		catch (final Exception e)
		{
			warn("" + e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
	}
	
	public boolean isHwidAvailable(final Player player, final int questId)
	{
		final String hwid = player.getHWID();
		if (_hwidList.containsKey(hwid))
		{
			final List<Integer> quests = _hwidList.get(hwid);
			for (final int id : quests)
			{
				if (id == questId)
				{
					return false;
				}
			}
		}
		return true;
	}
	
	public void insert(final String hwid, final int questId)
	{
		List<Integer> quests;
		if ((quests = _hwidList.get(hwid)) == null)
		{
			_hwidList.put(hwid, quests = new ArrayList<>());
		}
		quests.add(questId);
		_hwidList.put(hwid, quests);
		
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("INSERT INTO character_quests_hwid_data VALUES (?,?)");
			statement.setString(1, hwid);
			statement.setInt(2, questId);
			statement.executeUpdate();
		}
		catch (final Exception e)
		{
			warn("" + e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
	
	public void cleanHwidList()
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("DELETE FROM character_quests_hwid_data");
			statement.execute();
		}
		catch (final Exception e)
		{
			warn("Failed to clean up quest hwid list.", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
		
		_hwidList.clear();
		
		final Calendar newTime = Calendar.getInstance();
		newTime.setLenient(true);
		newTime.set(Calendar.HOUR_OF_DAY, 6);
		newTime.set(Calendar.MINUTE, 30);
		newTime.add(Calendar.DAY_OF_MONTH, 1);
		ServerVariables.set("Quest_HwidMap", newTime.getTimeInMillis());
		
		info("Hwids map refresh completed.");
	}
	
	public static final QuestManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final QuestManager _instance = new QuestManager();
	}
}