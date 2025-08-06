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
package l2e.loginserver.network.communication.gameserverpackets;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import l2e.commons.dbutils.DbUtils;
import l2e.loginserver.database.DatabaseFactory;
import l2e.loginserver.network.communication.ReceivablePacket;

public class ChangeAllowedIp extends ReceivablePacket
{
	private String _account;
	private String _ip;
	
	@Override
	protected void readImpl()
	{
		_account = readS();
		_ip = readS();
	}
	
	@Override
	protected void runImpl()
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE accounts SET allowIp=? WHERE login=?");
			statement.setString(1, _ip);
			statement.setString(2, _account);
			statement.execute();
			statement.close();
		}
		catch (final SQLException e)
		{
			_log.warn("ChangeAllowedIP: Could not write data. Reason: " + e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
}