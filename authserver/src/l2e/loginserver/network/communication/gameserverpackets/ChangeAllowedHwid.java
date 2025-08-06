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
import java.sql.ResultSet;
import java.sql.SQLException;

import l2e.commons.dbutils.DbUtils;
import l2e.loginserver.database.DatabaseFactory;
import l2e.loginserver.network.communication.ReceivablePacket;

public class ChangeAllowedHwid extends ReceivablePacket
{
	private String _account;
	private String _hwid;
	
	@Override
	protected void readImpl()
	{
		_account = readS();
		_hwid = readS();
	}
	
	@Override
	protected void runImpl()
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT hwid FROM hwid_lock WHERE login=?");
			statement.setString(1, _account);
			rset = statement.executeQuery();
			final boolean is = rset.next();
			statement.close();
			statement = con.prepareStatement(!is ? "INSERT INTO hwid_lock (hwid, login) VALUES(?,?)" : "UPDATE hwid_lock SET hwid=? WHERE login=?");
			statement.setString(1, _hwid);
			statement.setString(2, _account);
			statement.execute();
		}
		catch (final SQLException e)
		{
			_log.warn("ChangeAllowedHwid: Could not write data. Reason: " + e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
	}
}