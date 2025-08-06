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
package l2e.gameserver.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2e.commons.dbutils.DbUtils;

public class DatabaseUtils
{
	public static final Logger _log = LoggerFactory.getLogger(DatabaseUtils.class);
	
	public static boolean setEx(DatabaseFactory db, String query, Object... vars)
	{
		Connection con = null;
		Statement statement = null;
		PreparedStatement pstatement = null;
		try
		{
			if (db == null)
			{
				db = DatabaseFactory.getInstance();
			}
			con = db.getConnection();
			if (vars.length == 0)
			{
				statement = con.createStatement();
				statement.executeUpdate(query);
			}
			else
			{
				pstatement = con.prepareStatement(query);
				setVars(pstatement, vars);
				pstatement.executeUpdate();
			}
		}
		catch (final Exception e)
		{
			_log.warn("Could not execute update '" + query + "': " + e);
			e.printStackTrace();
			return false;
		}
		finally
		{
			DbUtils.closeQuietly(con, vars.length == 0 ? statement : pstatement);
		}
		return true;
	}
	
	public static void setVars(PreparedStatement statement, Object... vars) throws SQLException
	{
		Number n;
		long long_val;
		double double_val;
		for (int i = 0; i < vars.length; i++)
		{
			if (vars[i] instanceof Number)
			{
				n = (Number) vars[i];
				long_val = n.longValue();
				double_val = n.doubleValue();
				if (long_val == double_val)
				{
					statement.setLong(i + 1, long_val);
				}
				else
				{
					statement.setDouble(i + 1, double_val);
				}
			}
			else if (vars[i] instanceof String)
			{
				statement.setString(i + 1, (String) vars[i]);
			}
		}
	}
	
	public static boolean set(String query, Object... vars)
	{
		return setEx(null, query, vars);
	}
	
	public static boolean set(String query)
	{
		return setEx(null, query);
	}
	
	public static Object get(String query)
	{
		Object ret = null;
		Connection con = null;
		Statement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.createStatement();
			rset = statement.executeQuery(query + " LIMIT 1");
			final ResultSetMetaData md = rset.getMetaData();
			
			if (rset.next())
			{
				if (md.getColumnCount() > 1)
				{
					final Map<String, Object> tmp = new HashMap<>();
					for (int i = md.getColumnCount(); i > 0; i--)
					{
						tmp.put(md.getColumnName(i), rset.getObject(i));
					}
					ret = tmp;
				}
				else
				{
					ret = rset.getObject(1);
				}
			}
			
		}
		catch (final Exception e)
		{
			_log.warn("Could not execute query '" + query + "': " + e);
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
		return ret;
	}
	
	public static List<Map<String, Object>> getAll(String query)
	{
		final List<Map<String, Object>> ret = new ArrayList<>();
		Connection con = null;
		Statement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.createStatement();
			rset = statement.executeQuery(query);
			final ResultSetMetaData md = rset.getMetaData();
			
			while (rset.next())
			{
				final Map<String, Object> tmp = new HashMap<>();
				for (int i = md.getColumnCount(); i > 0; i--)
				{
					tmp.put(md.getColumnName(i), rset.getObject(i));
				}
				ret.add(tmp);
			}
		}
		catch (final Exception e)
		{
			_log.warn("Could not execute query '" + query + "': " + e);
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
		return ret;
	}
	
	public static List<Object> get_array(DatabaseFactory db, String query)
	{
		final List<Object> ret = new ArrayList<>();
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			if (db == null)
			{
				db = DatabaseFactory.getInstance();
			}
			con = db.getConnection();
			statement = con.prepareStatement(query);
			rset = statement.executeQuery();
			final ResultSetMetaData md = rset.getMetaData();
			
			while (rset.next())
			{
				if (md.getColumnCount() > 1)
				{
					final Map<String, Object> tmp = new HashMap<>();
					for (int i = 0; i < md.getColumnCount(); i++)
					{
						tmp.put(md.getColumnName(i + 1), rset.getObject(i + 1));
					}
					ret.add(tmp);
				}
				else
				{
					ret.add(rset.getObject(1));
				}
			}
		}
		catch (final Exception e)
		{
			_log.warn("Could not execute query '" + query + "': " + e);
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
		return ret;
	}
	
	public static List<Object> get_array(String query)
	{
		return get_array(null, query);
	}
	
	public static int simple_get_int(String ret_field, String table, String where)
	{
		final String query = "SELECT " + ret_field + " FROM `" + table + "` WHERE " + where + " LIMIT 1;";
		
		int res = 0;
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(query);
			rset = statement.executeQuery();
			
			if (rset.next())
			{
				res = rset.getInt(1);
			}
		}
		catch (final Exception e)
		{
			_log.warn("mSGI: Error in query '" + query + "':" + e);
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
		
		return res;
	}
	
	public static Integer[][] simple_get_int_array(DatabaseFactory db, String[] ret_fields, String table, String where)
	{
		String fields = null;
		for (final String field : ret_fields)
		{
			if (fields != null)
			{
				fields += ",";
				fields += "`" + field + "`";
			}
			else
			{
				fields = "`" + field + "`";
			}
		}
		
		final String query = "SELECT " + fields + " FROM `" + table + "` WHERE " + where;
		
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		
		Integer res[][] = null;
		
		try
		{
			if (db == null)
			{
				db = DatabaseFactory.getInstance();
			}
			con = db.getConnection();
			statement = con.prepareStatement(query);
			rset = statement.executeQuery();
			
			final List<Integer[]> al = new ArrayList<>();
			int row = 0;
			while (rset.next())
			{
				final Integer[] tmp = new Integer[ret_fields.length];
				for (int i = 0; i < ret_fields.length; i++)
				{
					tmp[i] = rset.getInt(i + 1);
				}
				al.add(row, tmp);
				row++;
			}
			
			res = al.toArray(new Integer[row][ret_fields.length]);
		}
		catch (final Exception e)
		{
			_log.warn("mSGIA: Error in query '" + query + "':" + e);
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
		
		return res;
	}
	
	public static Integer[][] simple_get_int_array(String[] ret_fields, String table, String where)
	{
		return simple_get_int_array(null, ret_fields, table, where);
	}
	
}