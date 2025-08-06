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
import l2e.gameserver.Config;
import l2e.gameserver.data.parser.DressArmorParser;
import l2e.gameserver.data.parser.DressCloakParser;
import l2e.gameserver.data.parser.DressHatParser;
import l2e.gameserver.data.parser.DressShieldParser;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.model.actor.Player;

public class CharacterVisualDAO extends LoggerObject
{
	private static final String INSERT_VISUAL = "INSERT INTO characters_visual (charId, skinType, skinId, active) VALUES (?,?,?,?)";
	private static final String UPDATE_VISUAL = "UPDATE characters_visual SET active=? WHERE charId=? and skinType=? and skinId=?";
	private static final String REMOVE_VISUAL = "DELETE FROM characters_visual WHERE charId=? and skinType=? and skinId=?";
	private static final String RESTORE_VISUAL = "SELECT * FROM characters_visual WHERE charId=?";
	
	public void add(Player player, String type, int skinId)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(INSERT_VISUAL);
			statement.setInt(1, player.getObjectId());
			statement.setString(2, type);
			statement.setInt(3, skinId);
			statement.setInt(4, 0);
			statement.executeUpdate();
		}
		catch (final Exception e)
		{
			warn("Could not insert char visual skin: " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
	
	public void update(Player player, final String type, final int active, final int skinId)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(UPDATE_VISUAL);
			statement.setInt(1, active);
			statement.setInt(2, player.getObjectId());
			statement.setString(3, type);
			statement.setInt(4, skinId);
			statement.execute();
		}
		catch (final Exception e)
		{
			warn("Failed update character visual skins.", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
	
	public void restore(final Player player)
	{
		if (!Config.ALLOW_VISUAL_SYSTEM)
		{
			return;
		}
		
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(RESTORE_VISUAL);
			statement.setInt(1, player.getObjectId());
			rset = statement.executeQuery();
			while (rset.next())
			{
				final var type = rset.getString("skinType");
				final var active = rset.getInt("active");
				
				switch (type)
				{
					case "Weapon" :
						if (active == 1)
						{
							player.setActiveWeaponSkin(rset.getInt("skinId"), true);
						}
						player.addWeaponSkin(rset.getInt("skinId"));
						break;
					case "Armor" :
						if (active == 1)
						{
							final var set = DressArmorParser.getInstance().getArmor(rset.getInt("skinId"));
							if (set != null)
							{
								player.setActiveArmorSkin(rset.getInt("skinId"), true);
								if (set.getShieldId() > 0 && DressShieldParser.getInstance().getShieldId(set.getShieldId()) != -1)
								{
									player.setActiveShieldSkin(DressShieldParser.getInstance().getShieldId(set.getShieldId()), false);
								}
								
								if (set.getCloakId() > 0 && DressCloakParser.getInstance().getCloakId(set.getCloakId()) != -1)
								{
									player.setActiveCloakSkin(DressCloakParser.getInstance().getCloakId(set.getCloakId()), false);
								}
								
								if (set.getHatId() > 0 && DressHatParser.getInstance().getHatId(set.getHatId()) != -1)
								{
									if (set.getSlot() == 3)
									{
										player.setActiveMaskSkin(DressHatParser.getInstance().getHatId(set.getHatId()), false);
									}
									else
									{
										player.setActiveHairSkin(DressHatParser.getInstance().getHatId(set.getHatId()), false);
									}
								}
							}
						}
						player.addArmorSkin(rset.getInt("skinId"));
						break;
					case "Shield" :
						if (active == 1)
						{
							player.setActiveShieldSkin(rset.getInt("skinId"), true);
						}
						player.addShieldSkin(rset.getInt("skinId"));
						break;
					case "Cloak" :
						if (active == 1)
						{
							player.setActiveCloakSkin(rset.getInt("skinId"), true);
						}
						player.addCloakSkin(rset.getInt("skinId"));
						break;
					case "Hair" :
						if (active == 1)
						{
							final var visual = DressHatParser.getInstance().getHat(rset.getInt("skinId"));
							if (visual.getSlot() == 2)
							{
								player.setActiveHairSkin(rset.getInt("skinId"), true);
							}
							else
							{
								player.setActiveMaskSkin(rset.getInt("skinId"), true);
							}
						}
						player.addHairSkin(rset.getInt("skinId"));
						break;
				}
			}
		}
		catch (final Exception e)
		{
			warn("Failed restore character visual skins.", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
	}
	
	public void remove(Player player, String type, int skinId)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(REMOVE_VISUAL);
			statement.setInt(1, player.getObjectId());
			statement.setString(2, type);
			statement.setInt(3, skinId);
			statement.execute();
		}
		catch (final Exception e)
		{
			warn("Failed update character visual skins.", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
	
	public static CharacterVisualDAO getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final CharacterVisualDAO _instance = new CharacterVisualDAO();
	}
}