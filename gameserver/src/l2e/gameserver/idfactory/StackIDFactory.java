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
package l2e.gameserver.idfactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Stack;

import l2e.commons.dbutils.DbUtils;
import l2e.gameserver.Config;
import l2e.gameserver.database.DatabaseFactory;

public class StackIDFactory extends IdFactory
{
	private int _curOID;
	private int _tempOID;
	
	private final Stack<Integer> _freeOIDStack = new Stack<>();
	
	protected StackIDFactory()
	{
		super();
		_curOID = FIRST_OID;
		_tempOID = FIRST_OID;
		
		Connection con = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			
			final Integer[] tmp_obj_ids = extractUsedObjectIDTable();
			if (tmp_obj_ids.length > 0)
			{
				_curOID = tmp_obj_ids[tmp_obj_ids.length - 1];
			}
			_log.info("Max Id = " + _curOID);
			
			int N = tmp_obj_ids.length;
			for (int idx = 0; idx < N; idx++)
			{
				N = insertUntil(tmp_obj_ids, idx, N, con);
			}
			
			_curOID++;
			_log.info("IdFactory: Next usable Object ID is: " + _curOID);
			_initialized = true;
		}
		catch (final Exception e)
		{
			_log.error(getClass().getSimpleName() + ": Could not be initialized properly:" + e.getMessage());
		}
		finally
		{
			DbUtils.closeQuietly(con);
		}
	}
	
	private int insertUntil(Integer[] tmp_obj_ids, int idx, int N, Connection con) throws SQLException
	{
		final int id = tmp_obj_ids[idx];

		if (id == _tempOID)
		{
			_tempOID++;
			return N;
		}

		if (Config.BAD_ID_CHECKING)
		{
			for (final String check : ID_CHECKS)
			{
				final var ps = con.prepareStatement(check);
				ps.setInt(1, _tempOID);
				ps.setInt(2, id);
				final var rs = ps.executeQuery();
				while (rs.next())
				{
					final int badId = rs.getInt(1);
					_log.error("Bad ID " + badId + " in DB found by: " + check);
					throw new RuntimeException();
				}
				rs.close();
				ps.close();
			}
		}

		int hole = id - _tempOID;
		if (hole > N - idx)
		{
			hole = N - idx;
		}
		for (int i = 1; i <= hole; i++)
		{
			_freeOIDStack.push(_tempOID);
			_tempOID++;
		}
		if (hole < N - idx)
		{
			_tempOID++;
		}
		return N - hole;
	}
	
	public static IdFactory getInstance()
	{
		return _instance;
	}
	
	@Override
	public synchronized int getNextId()
	{
		int id;
		if (!_freeOIDStack.empty())
		{
			id = _freeOIDStack.pop();
		}
		else
		{
			id = _curOID;
			_curOID = _curOID + 1;
		}
		return id;
	}
	
	@Override
	public synchronized void releaseId(int id)
	{
		_freeOIDStack.push(id);
	}
	
	@Override
	public int size()
	{
		return (FREE_OBJECT_ID_SIZE - _curOID) + FIRST_OID + _freeOIDStack.size();
	}
}