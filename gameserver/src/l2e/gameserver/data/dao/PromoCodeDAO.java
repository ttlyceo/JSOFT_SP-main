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

import l2e.commons.dbutils.DbUtils;
import l2e.commons.log.LoggerObject;
import l2e.gameserver.data.parser.PromoCodeParser;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.promocode.PromoCodeTemplate;

public class PromoCodeDAO extends LoggerObject
{
	private final String ADD_PROMOCODE = "REPLACE INTO promocodes (name,value) VALUES (?,?)";
	private final String ADD_CHARACTER = "INSERT INTO character_promocodes (charId,name) VALUES (?,?)";
	private final String ADD_CHARACTER_ACC = "INSERT INTO character_promocodes_account (account,name) VALUES (?,?)";
	private final String ADD_CHARACTER_HWID = "INSERT INTO character_promocodes_hwid (hwid,name) VALUES (?,?)";
	
	public void insert(Player player, PromoCodeTemplate tpl)
	{
		PromoCodeParser.getInstance().addToCharList(tpl.getName(), player.getObjectId());
		final Connection con = DatabaseFactory.getInstance().getConnection();
		PreparedStatement statement = null;
		try
		{
			statement = con.prepareStatement(ADD_CHARACTER);
			statement.setInt(1, player.getObjectId());
			statement.setString(2, tpl.getName());
			statement.executeUpdate();
			statement.close();
		}
		catch (final Exception e)
		{
			warn("Could not insert character_promocodes data: " + e);
		}
		
		if (tpl.getLimit() > 0)
		{
			try
			{
				statement = con.prepareStatement(ADD_PROMOCODE);
				statement.setString(1, tpl.getName());
				statement.setInt(2, tpl.getCurLimit());
				statement.executeUpdate();
				statement.close();
			}
			catch (final Exception e)
			{
				warn("Could not insert promocodes data: " + e);
			}
		}
		
		if (tpl.isLimitByAccount())
		{
			PromoCodeParser.getInstance().addToAccountList(tpl.getName(), player.getAccountName());
			try
			{
				statement = con.prepareStatement(ADD_CHARACTER_ACC);
				statement.setString(1, player.getAccountName());
				statement.setString(2, tpl.getName());
				statement.executeUpdate();
				statement.close();
			}
			catch (final Exception e)
			{
				warn("Could not insert character_promocodes_account data: " + e);
			}
		}
		
		if (tpl.isLimitHWID())
		{
			PromoCodeParser.getInstance().addToHwidList(tpl.getName(), player.getHWID());
			try
			{
				statement = con.prepareStatement(ADD_CHARACTER_HWID);
				statement.setString(1, player.getHWID());
				statement.setString(2, tpl.getName());
				statement.executeUpdate();
				statement.close();
			}
			catch (final Exception e)
			{
				warn("Could not insert character_promocodes_hwid data: " + e);
			}
		}
		DbUtils.closeQuietly(con, statement);
	}
	
	public static PromoCodeDAO getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final PromoCodeDAO _instance = new PromoCodeDAO();
	}
}