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
package l2e.loginserver.database;

import java.sql.Connection;
import java.sql.SQLException;

import org.mariadb.jdbc.MariaDbPoolDataSource;

import l2e.commons.log.LoggerObject;
import l2e.loginserver.Config;

public class DatabaseFactory extends LoggerObject
{
	private MariaDbPoolDataSource _source;
	
	public DatabaseFactory()
	{
		try
		{
			_source = new MariaDbPoolDataSource(Config.DATABASE_URL + "&user=" + Config.DATABASE_LOGIN + "&password=" + Config.DATABASE_PASSWORD + "&maxPoolSize=" + Config.DATABASE_MAX_CONNECTIONS);
		}
		catch (final SQLException e)
		{
			error("Problem with database connector initialize...");
		}
	}
	
	public Connection getConnection()
	{
		Connection con = null;
		while (con == null)
		{
			try
			{
				con = _source.getConnection();
			}
			catch (final Exception e)
			{
				error("Cound not get a connection. " + e);
			}
		}
		return con;
	}
	
	public void close()
	{
		try
		{
			_source.close();
		}
		catch (final Exception e)
		{
			error("There was a problem closing the data source. " + e);
		}
	}
	
	public static DatabaseFactory getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final DatabaseFactory INSTANCE = new DatabaseFactory();
	}
}