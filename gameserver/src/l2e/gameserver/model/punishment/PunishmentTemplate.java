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
package l2e.gameserver.model.punishment;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2e.commons.dbutils.DbUtils;
import l2e.gameserver.database.DatabaseFactory;

public class PunishmentTemplate
{
	protected static final Logger _log = LoggerFactory.getLogger(PunishmentTemplate.class);

	private static final String INSERT_QUERY = "INSERT INTO punishments (`key`, `sortName`, `sort`, `affect`, `type`, `expiration`, `reason`, `punishedBy`) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
	private int _id;
	private final String _key;
	private final PunishmentSort _sort;
	private final String _sortName;
	private final PunishmentAffect _affect;
	private final PunishmentType _type;
	private final long _expirationTime;
	private final String _reason;
	private final String _punishedBy;
	private boolean _isStored;
	
	public PunishmentTemplate(String key, String sortName, PunishmentSort sort, PunishmentAffect affect, PunishmentType type, long expirationTime, String reason, String punishedBy)
	{
		this(0, key, sortName, sort, affect, type, expirationTime, reason, punishedBy, false);
	}

	public PunishmentTemplate(int id, String key, String sortName, PunishmentSort sort, PunishmentAffect affect, PunishmentType type, long expirationTime, String reason, String punishedBy, boolean isStored)
	{
		_id = id;
		_key = String.valueOf(key);
		_sortName = sortName;
		_sort = sort;
		_affect = affect;
		_type = type;
		_expirationTime = expirationTime;
		_reason = reason;
		_punishedBy = punishedBy;
		_isStored = isStored;

		if (!isStored)
		{
			startPunishment();
		}
	}
	
	public int getId()
	{
		return _id;
	}
	
	public String getKey()
	{
		return _key;
	}
	
	public String getSortName()
	{
		return _sortName;
	}
	
	public PunishmentSort getSort()
	{
		return _sort;
	}
	
	public PunishmentAffect getAffect()
	{
		return _affect;
	}

	public PunishmentType getType()
	{
		return _type;
	}

	public final long getExpirationTime()
	{
		return _expirationTime;
	}

	public String getReason()
	{
		return _reason;
	}

	public String getPunishedBy()
	{
		return _punishedBy;
	}

	public boolean isStored()
	{
		return _isStored;
	}

	private void startPunishment()
	{
		if (!_isStored)
		{
			Connection con = null;
			PreparedStatement st = null;
			ResultSet rset = null;
			try
			{
				con = DatabaseFactory.getInstance().getConnection();
				st = con.prepareStatement(INSERT_QUERY, Statement.RETURN_GENERATED_KEYS);
				st.setString(1, _key);
				st.setString(2, _sortName);
				st.setString(3, _sort.name());
				st.setString(4, _affect.name());
				st.setString(5, _type.name());
				st.setLong(6, _expirationTime);
				st.setString(7, _reason);
				st.setString(8, _punishedBy);
				st.execute();
				rset = st.getGeneratedKeys();
				if (rset.next())
				{
					_id = rset.getInt(1);
				}
				_isStored = true;
			}
			catch (final SQLException e)
			{
				_log.warn(getClass().getSimpleName() + ": Couldn't store punishment task for: " + _affect + " " + _key, e);
			}
			finally
			{
				DbUtils.closeQuietly(con, st, rset);
			}
		}
	}
}