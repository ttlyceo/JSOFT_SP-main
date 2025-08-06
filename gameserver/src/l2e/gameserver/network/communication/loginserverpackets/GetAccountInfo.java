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
package l2e.gameserver.network.communication.loginserverpackets;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import l2e.commons.dbutils.DbUtils;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.network.communication.AuthServerCommunication;
import l2e.gameserver.network.communication.ReceivablePacket;
import l2e.gameserver.network.communication.gameserverpackets.SetAccountInfo;

public class GetAccountInfo extends ReceivablePacket
{
	private String _account;
	
	@Override
	protected void readImpl()
	{
		_account = readS();
	}
	
	@Override
	protected void runImpl()
	{
		int playerSize = 0;
		final List<Long> deleteChars = new ArrayList<>();
		
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT deletetime FROM characters WHERE account_name=?");
			statement.setString(1, _account);
			rset = statement.executeQuery();
			while (rset.next())
			{
				playerSize++;
				final long d = rset.getLong("deletetime");
				if (d > 0)
				{
					deleteChars.add(d);
				}
			}
		}
		catch (final Exception e)
		{
			_log.warn("GetAccountInfo:runImpl():" + e, e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
		AuthServerCommunication.getInstance().sendPacket(new SetAccountInfo(_account, playerSize, deleteChars));
	}
}
