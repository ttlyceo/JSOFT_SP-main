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
import java.sql.SQLException;

import l2e.commons.dbutils.DbUtils;
import l2e.commons.log.LoggerObject;
import l2e.gameserver.Config;
import l2e.gameserver.data.parser.PremiumAccountsParser;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.player.impl.PcPointsTask;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.ExBrPremiumState;
import l2e.gameserver.network.serverpackets.MagicSkillUse;

public class CharacterPremiumDAO extends LoggerObject
{
	private static final String SELECT_SQL_QUERY = "SELECT id,status,expireTime FROM character_premium WHERE account=?";
	private static final String INSERT_SQL_QUERY = "INSERT INTO character_premium (account,id,status,expireTime) values(?,?,?,?)";
	private static final String UPDATE_SQL_QUERY = "UPDATE character_premium SET id=?,status=?,expireTime=? WHERE account=?";
	private static final String UPDATE_TIME_SQL_QUERY = "UPDATE character_premium SET expireTime=? WHERE account=?";
	private static final String REPLACE_SQL_QUERY = "UPDATE character_premium SET id=?,status=?,expireTime=? WHERE account=?";
	
	private static final String SELECT_SQL_QUERY_PERSONAL = "SELECT id,status,expireTime FROM character_premium_personal WHERE charId=?";
	private static final String INSERT_SQL_QUERY_PERSONAL = "INSERT INTO character_premium_personal (charId,id,status,expireTime) values(?,?,?,?)";
	private static final String UPDATE_SQL_QUERY_PERSONAL = "UPDATE character_premium_personal SET id=?,status=?,expireTime=? WHERE charId=?";
	private static final String UPDATE_TIME_SQL_QUERY_PERSONAL = "UPDATE character_premium_personal SET expireTime=? WHERE charId=?";
	private static final String REPLACE_SQL_QUERY_PERSONAL = "UPDATE character_premium_personal SET id=?,status=?,expireTime=? WHERE charId=?";
	
	private void insert(Player player)
	{
		if (!Config.USE_PREMIUMSERVICE)
		{
			return;
		}
		
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(INSERT_SQL_QUERY);
			statement.setString(1, player.getAccountName());
			statement.setInt(2, 0);
			statement.setInt(3, 0);
			statement.setLong(4, 0);
			statement.executeUpdate();
		}
		catch (final Exception e)
		{
			warn("Could not insert char data: " + e);
			return;
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
	
	private void insertPersonal(Player player)
	{
		if (!Config.USE_PREMIUMSERVICE)
		{
			return;
		}
		
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(INSERT_SQL_QUERY_PERSONAL);
			statement.setInt(1, player.getObjectId());
			statement.setInt(2, 0);
			statement.setInt(3, 0);
			statement.setLong(4, 0);
			statement.executeUpdate();
		}
		catch (final Exception e)
		{
			warn("Could not insert char data: " + e);
			return;
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
	
	public void updateOnlineTime(Player player)
	{
		if (!Config.USE_PREMIUMSERVICE)
		{
			return;
		}
		
		final long time = (player.getPremiumBonus().getOnlineTime() + (System.currentTimeMillis() - player.getPremiumOnlineTime()));
		
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(UPDATE_TIME_SQL_QUERY);
			statement.setLong(1, time);
			statement.setString(2, player.getAccountName());
			statement.execute();
		}
		catch (final SQLException e)
		{
			warn("Could not update data: " + e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
	
	public void updateOnlineTimePersonal(Player player)
	{
		if (!Config.USE_PREMIUMSERVICE)
		{
			return;
		}
		final long time = (player.getPremiumBonus().getOnlineTime() + (System.currentTimeMillis() - player.getPremiumOnlineTime()));
		
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(UPDATE_TIME_SQL_QUERY_PERSONAL);
			statement.setLong(1, time);
			statement.setInt(2, player.getObjectId());
			statement.execute();
		}
		catch (final SQLException e)
		{
			warn("Could not update data: " + e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
	
	public void update(Player player, int id, long time, boolean isOnlineType)
	{
		if (!Config.USE_PREMIUMSERVICE)
		{
			return;
		}
		
		var isDouble = false;
		if (Config.PREMIUMSERVICE_DOUBLE && !isOnlineType)
		{
			final var bonus = player.getPremiumBonus();
			final int premId = bonus.getPremiumId();
			if (premId > 0)
			{
				final var template = PremiumAccountsParser.getInstance().getPremiumTemplate(id);
				if (premId == id || template.getGroupId() == bonus.getGroupId())
				{
					final long exitTime = bonus.getOnlineTime();
					final long newTime = time - System.currentTimeMillis();
					time = exitTime + newTime;
					bonus.setOnlineTime(time);
					isDouble = true;
				}
				else
				{
					if (template != null)
					{
						if (!template.getBonusList().isEmpty())
						{
							for (final var type : template.getBonusList().keySet())
							{
								if (type != null)
								{
									final double value = template.getBonusList().get(type);
									if (value > 0)
									{
										bonus.removeBonusType(type, value);
									}
								}
							}
						}
					}
				}
				
				if (template != null)
				{
					for (final var gift : template.getGifts())
					{
						if (gift != null && gift.isRemovable() && !isDouble)
						{
							if (player.getInventory().getItemByItemId(gift.getId()) != null)
							{
								player.destroyItemByItemId("Remove Premium", gift.getId(), gift.getCount(), player, false);
							}
							else if (player.getWarehouse().getItemByItemId(gift.getId()) != null)
							{
								player.getWarehouse().destroyItemByItemId(gift.getId(), gift.getCount(), "Remove Premium");
							}
						}
					}
				}
				player.getPersonalTasks().removeTask(2, false);
			}
		}
		
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(REPLACE_SQL_QUERY);
			statement.setInt(1, id);
			statement.setInt(2, 1);
			statement.setLong(3, time);
			statement.setString(4, player.getAccountName());
			statement.execute();
		}
		catch (final SQLException e)
		{
			warn("Could not update data: " + e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
		
		final var template = PremiumAccountsParser.getInstance().getPremiumTemplate(id);
		if (template != null && !template.getBonusList().isEmpty())
		{
			if (!isDouble)
			{
				final var bonus = player.getPremiumBonus();
				bonus.setPremiumId(template.getId());
				bonus.setGroupId(template.getGroupId());
				bonus.setIsPersonal(template.isPersonal());
				bonus.setOnlineType(template.isOnlineType());
				bonus.setOnlineTime(time);
				bonus.setActivate(true);
				for (final var type : template.getBonusList().keySet())
				{
					if (type != null)
					{
						final double value = template.getBonusList().get(type);
						if (value > 0)
						{
							bonus.addBonusType(type, value);
						}
					}
				}
			}
			player.startPremiumTask(time);
			
			player.sendPacket(new ExBrPremiumState(player.getObjectId(), 1));
			player.broadcastPacket(new MagicSkillUse(player, player, 6463, 1, 0, 0));
			player.sendPacket(SystemMessageId.THE_PREMIUM_ITEM_FOR_THIS_ACCOUNT_WAS_PROVIDED);
			if (!isDouble)
			{
				if (Config.PC_BANG_ENABLED && Config.PC_BANG_ONLY_FOR_PREMIUM)
				{
					player.getPersonalTasks().addTask(new PcPointsTask(Config.PC_BANG_INTERVAL * 1000L));
				}
			}
			
			for (final var gift : template.getGifts())
			{
				if (gift != null)
				{
					if (gift.isRemovable() && isDouble)
					{
						continue;
					}
					player.addItem("PremiumGift", gift.getId(), gift.getCount(), player, true);
				}
			}
		}
	}
	
	public void updatePersonal(Player player, int id, long time, boolean isOnlineType)
	{
		if (!Config.USE_PREMIUMSERVICE)
		{
			return;
		}
		
		var isDouble = false;
		if (Config.PREMIUMSERVICE_DOUBLE && !isOnlineType)
		{
			final var bonus = player.getPremiumBonus();
			final int premId = bonus.getPremiumId();
			if (premId > 0)
			{
				final var template = PremiumAccountsParser.getInstance().getPremiumTemplate(id);
				if (premId == id || template.getGroupId() == bonus.getGroupId())
				{
					final long exitTime = bonus.getOnlineTime();
					final long newTime = time - System.currentTimeMillis();
					time = exitTime + newTime;
					bonus.setOnlineTime(time);
					isDouble = true;
					
				}
				else
				{
					if (template != null)
					{
						if (!template.getBonusList().isEmpty())
						{
							for (final var type : template.getBonusList().keySet())
							{
								if (type != null)
								{
									final double value = template.getBonusList().get(type);
									if (value > 0)
									{
										bonus.removeBonusType(type, value);
									}
								}
							}
						}
					}
				}
				
				if (template != null)
				{
					for (final var gift : template.getGifts())
					{
						if (gift != null && gift.isRemovable())
						{
							if (player.getInventory().getItemByItemId(gift.getId()) != null)
							{
								player.destroyItemByItemId("Remove Premium", gift.getId(), gift.getCount(), player, false);
							}
							else if (player.getWarehouse().getItemByItemId(gift.getId()) != null)
							{
								player.getWarehouse().destroyItemByItemId(gift.getId(), gift.getCount(), "Remove Premium");
							}
						}
					}
				}
				player.getPersonalTasks().removeTask(2, false);
			}
		}
		
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(REPLACE_SQL_QUERY_PERSONAL);
			statement.setInt(1, id);
			statement.setInt(2, 1);
			statement.setLong(3, time);
			statement.setInt(4, player.getObjectId());
			statement.execute();
		}
		catch (final SQLException e)
		{
			warn("Could not update data: " + e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
		
		final var template = PremiumAccountsParser.getInstance().getPremiumTemplate(id);
		if (template != null && !template.getBonusList().isEmpty())
		{
			if (!isDouble)
			{
				final var bonus = player.getPremiumBonus();
				bonus.setPremiumId(template.getId());
				bonus.setGroupId(template.getGroupId());
				bonus.setIsPersonal(template.isPersonal());
				bonus.setOnlineType(template.isOnlineType());
				bonus.setOnlineTime(time);
				bonus.setActivate(true);
				for (final var type : template.getBonusList().keySet())
				{
					if (type != null)
					{
						final double value = template.getBonusList().get(type);
						if (value > 0)
						{
							bonus.addBonusType(type, value);
						}
					}
				}
			}
			player.startPremiumTask(time);
			
			player.sendPacket(new ExBrPremiumState(player.getObjectId(), 1));
			player.broadcastPacket(new MagicSkillUse(player, player, 6463, 1, 0, 0));
			player.sendPacket(SystemMessageId.THE_PREMIUM_ITEM_FOR_THIS_ACCOUNT_WAS_PROVIDED);
			if (!isDouble)
			{
				if (Config.PC_BANG_ENABLED && Config.PC_BANG_ONLY_FOR_PREMIUM)
				{
					player.getPersonalTasks().addTask(new PcPointsTask(Config.PC_BANG_INTERVAL * 1000L));
				}
			}
			for (final var gift : template.getGifts())
			{
				if (gift != null)
				{
					player.addItem("PremiumGift", gift.getId(), gift.getCount(), player, true);
				}
			}
		}
	}
	
	public void disable(Player player)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(UPDATE_SQL_QUERY);
			statement.setInt(1, 0);
			statement.setLong(2, 0);
			statement.setLong(3, 0);
			statement.setString(4, player.getAccountName());
			statement.execute();
		}
		catch (final SQLException e)
		{
			warn("Could not disable data: " + e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
	
	public void disablePersonal(Player player)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(UPDATE_SQL_QUERY_PERSONAL);
			statement.setInt(1, 0);
			statement.setLong(2, 0);
			statement.setLong(3, 0);
			statement.setInt(4, player.getObjectId());
			statement.execute();
		}
		catch (final SQLException e)
		{
			warn("Could not disable data: " + e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
	
	public void restore(Player player)
	{
		if (!Config.USE_PREMIUMSERVICE)
		{
			return;
		}
		
		boolean sucess = false;
		boolean active = false;
		
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(SELECT_SQL_QUERY_PERSONAL);
			statement.setInt(1, player.getObjectId());
			rset = statement.executeQuery();
			while (rset.next())
			{
				sucess = true;
				if (rset.getInt("status") == 1)
				{
					final var template = PremiumAccountsParser.getInstance().getPremiumTemplate(rset.getInt("id"));
					if (template != null)
					{
						boolean validTime;
						if (template.isOnlineType())
						{
							validTime = rset.getLong("expireTime") < (template.getTime() * 1000L);
						}
						else
						{
							validTime = rset.getLong("expireTime") > System.currentTimeMillis();
						}
						
						if (validTime)
						{
							active = true;
							final var bonus = player.getPremiumBonus();
							bonus.setPremiumId(template.getId());
							bonus.setGroupId(template.getGroupId());
							bonus.setIsPersonal(template.isPersonal());
							bonus.setOnlineType(template.isOnlineType());
							bonus.setOnlineTime(rset.getLong("expireTime"));
							bonus.setActivate(true);
							if (!template.getBonusList().isEmpty())
							{
								for (final var type : template.getBonusList().keySet())
								{
									if (type != null)
									{
										final double value = template.getBonusList().get(type);
										if (value > 0)
										{
											bonus.addBonusType(type, value);
										}
									}
								}
							}
							player.startPremiumTask(rset.getLong("expireTime"));
						}
						else
						{
							disablePersonal(player);
							boolean removed = false;
							for (final var gift : template.getGifts())
							{
								if (gift != null && gift.isRemovable())
								{
									if (player.getInventory().getItemByItemId(gift.getId()) != null)
									{
										if (player.destroyItemByItemId("Remove Premium", gift.getId(), gift.getCount(), player, false))
										{
											removed = true;
										}
									}
									else if (player.getWarehouse().getItemByItemId(gift.getId()) != null)
									{
										if (player.getWarehouse().destroyItemByItemId(gift.getId(), gift.getCount(), "Remove Premium"))
										{
											removed = true;
										}
									}
								}
							}
							
							if (removed)
							{
								player.sendPacket(SystemMessageId.THE_PREMIUM_ACCOUNT_HAS_BEEN_TERMINATED);
							}
						}
					}
				}
			}
		}
		catch (final Exception e)
		{
			warn("Could not restore premium data for:" + player.getAccountName() + " " + e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
		
		if (!sucess)
		{
			insertPersonal(player);
		}
		
		if (!active)
		{
			restoreForAcc(player);
		}
	}
	
	public void restoreForAcc(Player player)
	{
		if (!Config.USE_PREMIUMSERVICE)
		{
			return;
		}
		
		boolean sucess = false;
		
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(SELECT_SQL_QUERY);
			statement.setString(1, player.getAccountName());
			rset = statement.executeQuery();
			while (rset.next())
			{
				sucess = true;
				if (rset.getInt("status") == 1)
				{
					final var template = PremiumAccountsParser.getInstance().getPremiumTemplate(rset.getInt("id"));
					if (template != null)
					{
						boolean validTime;
						if (template.isOnlineType())
						{
							validTime = rset.getLong("expireTime") < (template.getTime() * 1000L);
						}
						else
						{
							validTime = rset.getLong("expireTime") > System.currentTimeMillis();
						}
						
						if (validTime)
						{
							final var bonus = player.getPremiumBonus();
							bonus.setPremiumId(template.getId());
							bonus.setGroupId(template.getGroupId());
							bonus.setIsPersonal(template.isPersonal());
							bonus.setOnlineType(template.isOnlineType());
							bonus.setOnlineTime(rset.getLong("expireTime"));
							bonus.setActivate(true);
							if (!template.getBonusList().isEmpty())
							{
								for (final var type : template.getBonusList().keySet())
								{
									if (type != null)
									{
										final double value = template.getBonusList().get(type);
										if (value > 0)
										{
											bonus.addBonusType(type, value);
										}
									}
								}
							}
							player.startPremiumTask(rset.getLong("expireTime"));
						}
						else
						{
							disable(player);
							boolean removed = false;
							for (final var gift : template.getGifts())
							{
								if (gift != null && gift.isRemovable())
								{
									if (player.getInventory().getItemByItemId(gift.getId()) != null)
									{
										if (player.destroyItemByItemId("Remove Premium", gift.getId(), gift.getCount(), player, false))
										{
											removed = true;
										}
									}
									else if (player.getWarehouse().getItemByItemId(gift.getId()) != null)
									{
										if (player.getWarehouse().destroyItemByItemId(gift.getId(), gift.getCount(), "Remove Premium"))
										{
											removed = true;
										}
									}
								}
							}
							
							if (removed)
							{
								player.sendPacket(SystemMessageId.THE_PREMIUM_ACCOUNT_HAS_BEEN_TERMINATED);
							}
						}
					}
				}
			}
		}
		catch (final Exception e)
		{
			warn("Could not restore premium data for:" + player.getAccountName() + " " + e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
		
		if (sucess == false)
		{
			insert(player);
		}
	}
	
	public static CharacterPremiumDAO getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final CharacterPremiumDAO _instance = new CharacterPremiumDAO();
	}
}