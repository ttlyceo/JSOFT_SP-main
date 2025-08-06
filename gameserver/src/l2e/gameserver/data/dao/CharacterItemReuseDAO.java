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
package l2e.gameserver.data.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import l2e.commons.dbutils.DbUtils;
import l2e.commons.log.LoggerObject;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.network.serverpackets.ExUseSharedGroupItem;

public class CharacterItemReuseDAO extends LoggerObject
{
	private static final String ADD_ITEM_REUSE_SAVE = "INSERT INTO character_item_reuse_save (charId,itemId,itemObjId,reuseDelay,systime) VALUES (?,?,?,?,?)";
	private static final String RESTORE_ITEM_REUSE_SAVE = "SELECT charId,itemId,itemObjId,reuseDelay,systime FROM character_item_reuse_save WHERE charId=?";
	private static final String DELETE_ITEM_REUSE_SAVE = "DELETE FROM character_item_reuse_save WHERE charId=?";
	
	public void restore(Player player)
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(RESTORE_ITEM_REUSE_SAVE);
			statement.setInt(1, player.getObjectId());
			rset = statement.executeQuery();
			int itemId;
			long reuseDelay;
			long systime;
			boolean isInInventory;
			long remainingTime;
			while (rset.next())
			{
				itemId = rset.getInt("itemId");
				rset.getInt("itemObjId");
				reuseDelay = rset.getLong("reuseDelay");
				systime = rset.getLong("systime");
				isInInventory = true;
				
				var item = player.getInventory().getItemByItemId(itemId);
				if (item == null)
				{
					item = player.getWarehouse().getItemByItemId(itemId);
					isInInventory = false;
				}
				
				if ((item != null) && (item.getId() == itemId) && (item.getReuseDelay() > 0))
				{
					remainingTime = systime - System.currentTimeMillis();
					
					if (remainingTime > 10)
					{
						player.addTimeStampItem(item, reuseDelay, systime);
						
						if (isInInventory && item.isEtcItem())
						{
							final var group = item.getSharedReuseGroup();
							if (group > 0)
							{
								player.sendPacket(new ExUseSharedGroupItem(itemId, group, (int) remainingTime, (int) reuseDelay));
							}
						}
					}
				}
			}
			statement.close();
			
			statement = con.prepareStatement(DELETE_ITEM_REUSE_SAVE);
			statement.setInt(1, player.getObjectId());
			statement.executeUpdate();
			statement.close();
		}
		catch (final Exception e)
		{
			warn("Could not restore " + this + " Item Reuse data: " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
	}
	
	public void store(Player player)
	{
		Connection con = null;
		PreparedStatement statement = null;
		PreparedStatement statement2 = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(DELETE_ITEM_REUSE_SAVE);
			statement2 = con.prepareStatement(ADD_ITEM_REUSE_SAVE);
			
			statement.setInt(1, player.getObjectId());
			statement.execute();
			
			for (final var ts : player.getItemRemainingReuseTime().values())
			{
				if ((ts != null) && ts.hasNotPassed())
				{
					statement2.setInt(1, player.getObjectId());
					statement2.setInt(2, ts.getItemId());
					statement2.setInt(3, ts.getItemObjectId());
					statement2.setLong(4, ts.getReuse());
					statement2.setDouble(5, ts.getStamp());
					statement2.execute();
				}
			}
			statement2.close();
		}
		catch (final Exception e)
		{
			warn("Could not store char item reuse data: ", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
	
	public static CharacterItemReuseDAO getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final CharacterItemReuseDAO _instance = new CharacterItemReuseDAO();
	}
}