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
package l2e.gameserver.taskmanager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2e.commons.dbutils.DbUtils;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.CreatureSay;

public class AutoAnnounceTaskManager
{
	private static final Logger _log = LoggerFactory.getLogger(AutoAnnounceTaskManager.class);

	protected final List<AutoAnnouncement> _announces = new ArrayList<>();
	private int _nextId = 1;

	protected AutoAnnounceTaskManager()
	{
		restore();
	}

	public List<AutoAnnouncement> getAutoAnnouncements()
	{
		return _announces;
	}

	public void restore()
	{
		if (!_announces.isEmpty())
		{
			for (final AutoAnnouncement a : _announces)
			{
				a.stopAnnounce();
			}

			_announces.clear();
		}

		int count = 0;
		Connection con = null;
		Statement s = null;
		ResultSet data = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			s = con.createStatement();
			data = s.executeQuery("SELECT * FROM auto_announcements");
			while (data.next())
			{
				final int id = data.getInt("id");
				final long initial = data.getLong("initial");
				final long delay = data.getLong("delay");
				final int repeat = data.getInt("cycle");
				final String memo = data.getString("memo");
				final boolean isCritical = data.getBoolean("isCritical");
				final String[] text = memo.split("/n");
				ThreadPoolManager.getInstance().schedule(new AutoAnnouncement(id, delay, repeat, text, isCritical), initial);
				count++;
				if (_nextId <= id)
				{
					_nextId = id + 1;
				}
			}
		}
		catch (final Exception e)
		{
			_log.warn("AutoAnnoucements: Failed to load announcements data.", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, s, data);
		}
		_log.info("AutoAnnoucements: Loaded " + count + " Auto Annoucement Data.");
	}

	public void addAutoAnnounce(long initial, long delay, int repeat, String memo, boolean isCritical)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("INSERT INTO auto_announcements (id, initial, delay, cycle, memo, isCritical) VALUES (?,?,?,?,?,?)");
			statement.setInt(1, _nextId);
			statement.setLong(2, initial);
			statement.setLong(3, delay);
			statement.setInt(4, repeat);
			statement.setString(5, memo);
			statement.setInt(6, isCritical ? 1 : 0);
			statement.execute();

			final String[] text = memo.split("/n");
			ThreadPoolManager.getInstance().schedule(new AutoAnnouncement(_nextId++, delay, repeat, text, isCritical), initial);
		}
		catch (final Exception e)
		{
			_log.warn("AutoAnnoucements: Failed to add announcements data.", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	public void deleteAutoAnnounce(int index)
	{
		final AutoAnnouncement a = _announces.remove(index);
		a.stopAnnounce();
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("DELETE FROM auto_announcements WHERE id = ?");
			statement.setInt(1, a.getId());
			statement.execute();

		}
		catch (final Exception e)
		{
			_log.warn("AutoAnnoucements: Failed to delete announcements data.", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	public class AutoAnnouncement implements Runnable
	{
		private final int _id;
		private final long _delay;
		private int _repeat = -1;
		private final String[] _memo;
		private boolean _stopped = false;
		private final boolean _isCritical;

		public AutoAnnouncement(int id, long delay, int repeat, String[] memo, boolean isCritical)
		{
			_id = id;
			_delay = delay;
			_repeat = repeat;
			_memo = memo;
			_isCritical = isCritical;
			if (!_announces.contains(this))
			{
				_announces.add(this);
			}
		}

		public int getId()
		{
			return _id;
		}

		public String[] getMemo()
		{
			return _memo;
		}

		public void stopAnnounce()
		{
			_stopped = true;
		}

		public boolean isCritical()
		{
			return _isCritical;
		}

		@Override
		public void run()
		{
			if (!_stopped && (_repeat != 0))
			{
				for (final String text : _memo)
				{
					announce(text, _isCritical);
				}

				if (_repeat > 0)
				{
					_repeat--;
				}
				ThreadPoolManager.getInstance().schedule(this, _delay);
			}
			else
			{
				stopAnnounce();
			}
		}
	}

	public void announce(String text, boolean isCritical)
	{
		final var cs = new CreatureSay(0, isCritical ? Say2.CRITICAL_ANNOUNCE : Say2.ANNOUNCEMENT, "", text);
		GameObjectsStorage.getPlayers().stream().filter(p -> p != null && p.isOnline()).forEach(p -> p.sendPacket(cs));
	}

	public static AutoAnnounceTaskManager getInstance()
	{
		return SingletonHolder._instance;
	}

	private static class SingletonHolder
	{
		protected static final AutoAnnounceTaskManager _instance = new AutoAnnounceTaskManager();
	}
}