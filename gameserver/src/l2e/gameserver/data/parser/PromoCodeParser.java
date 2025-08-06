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
package l2e.gameserver.data.parser;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import l2e.commons.apache.StringUtils;
import l2e.commons.dbutils.DbUtils;
import l2e.commons.util.TimeUtils;
import l2e.gameserver.Config;
import l2e.gameserver.data.DocumentParser;
import l2e.gameserver.data.dao.PromoCodeDAO;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.promocode.PromoCodeTemplate;
import l2e.gameserver.model.actor.templates.promocode.impl.AbstractCodeReward;
import l2e.gameserver.model.strings.server.ServerMessage;

public final class PromoCodeParser extends DocumentParser
{
	private final Map<String, PromoCodeTemplate> _promoCodes = new HashMap<>();
	
	private final Map<String, List<String>> _codeHwidList = new ConcurrentHashMap<>();
	private final Map<String, List<String>> _codeAccountList = new ConcurrentHashMap<>();
	private final Map<String, List<Integer>> _codeCharList = new ConcurrentHashMap<>();
	
	private final Map<String, Long> _delayList = new ConcurrentHashMap<>();
	private final Map<String, Long> _delayRewardList = new ConcurrentHashMap<>();
	
	private boolean canUse = false;
	
	private PromoCodeParser()
	{
		_promoCodes.clear();
		_codeHwidList.clear();
		_codeAccountList.clear();
		_codeCharList.clear();
		load();
		restore();
		canUse = true;
	}
	
	public void reload()
	{
		canUse = false;
		_promoCodes.clear();
		_codeHwidList.clear();
		_codeAccountList.clear();
		_codeCharList.clear();
		load();
		restore();
		canUse = true;
	}
	
	public void restore()
	{
		restoreCodes();
		restoreCharacterCodes();
		restoreAccountCodes();
		restoreHwidCodes();
	}
	
	@Override
	public synchronized void load()
	{
		parseDatapackFile("data/stats/services/promoCodes.xml");
		info("Loaded " + _promoCodes.size() + " promocode templates.");
	}
	
	@Override
	protected void reloadDocument()
	{
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected void parseDocument()
	{
		for (var n = getCurrentDocument().getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("code".equalsIgnoreCase(d.getNodeName()))
					{
						var code = d.getAttributes();
						
						final var name = code.getNamedItem("name").getNodeValue();
						final var limit = Integer.parseInt(code.getNamedItem("limit").getNodeValue());
						final var limitByAccount = code.getNamedItem("limitByAccount") != null ? Boolean.parseBoolean(code.getNamedItem("limitByAccount").getNodeValue()) : false;
						final var limitHWID = code.getNamedItem("limitByHWID") != null ? Boolean.parseBoolean(code.getNamedItem("limitByHWID").getNodeValue()) : false;
						
						final List<AbstractCodeReward> rewards = new ArrayList<>();
						var fromValue = -1L;
						var toValue = -1L;
						int minLvl = 1, maxLvl = 85;
						var canUseSubClass = false;
						
						for (var cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling())
						{
							code = cd.getAttributes();
							if ("date".equalsIgnoreCase(cd.getNodeName()))
							{
								try
								{
									fromValue = code.getNamedItem("from") != null ? TimeUtils.parse(code.getNamedItem("from").getNodeValue()) : -1;
								}
								catch (final Exception e)
								{}
								
								try
								{
									toValue = code.getNamedItem("to") != null ? TimeUtils.parse(code.getNamedItem("to").getNodeValue()) : -1;
								}
								catch (final Exception e)
								{}
							}
							else if ("limit".equalsIgnoreCase(cd.getNodeName()))
							{
								minLvl = code.getNamedItem("minLvl") != null ? Integer.parseInt(code.getNamedItem("minLvl").getNodeValue()) : 1;
								maxLvl = code.getNamedItem("maxLvl") != null ? Integer.parseInt(code.getNamedItem("maxLvl").getNodeValue()) : 85;
								canUseSubClass = code.getNamedItem("canUseSubClass") != null ? Boolean.parseBoolean(code.getNamedItem("canUseSubClass").getNodeValue()) : false;
							}
							else if ("item".equalsIgnoreCase(cd.getNodeName()) || "exp".equalsIgnoreCase(cd.getNodeName()) || "sp".equalsIgnoreCase(cd.getNodeName()) || "setLevel".equalsIgnoreCase(cd.getNodeName()) || "addLevel".equalsIgnoreCase(cd.getNodeName()) || "premium".equalsIgnoreCase(cd.getNodeName()) || "pcPoint".equalsIgnoreCase(cd.getNodeName()) || "reputation".equalsIgnoreCase(cd.getNodeName()) || "fame".equalsIgnoreCase(cd.getNodeName()))
							{
								Class<AbstractCodeReward> aClass = null;
								try
								{
									aClass = (Class<AbstractCodeReward>) Class.forName("l2e.gameserver.model.actor.templates.promocode.impl." + StringUtils.capitalize(cd.getNodeName()) + "CodeReward");
								}
								catch (final Exception e)
								{
									warn("Not found class " + cd.getNodeName() + "CodeReward.java!");
								}
								
								Constructor<AbstractCodeReward> constructor = null;
								try
								{
									constructor = aClass.getConstructor(new Class[] { NamedNodeMap.class });
								}
								catch ( IllegalArgumentException | NoSuchMethodException | SecurityException e)
								{
									warn("Unable to create code reward class " + aClass.getSimpleName() + "!");
								}
								
								AbstractCodeReward reward = null;
								try
								{
									reward = constructor.newInstance(code);
								}
								catch (
								    IllegalAccessException | InvocationTargetException | InstantiationException | IllegalArgumentException e)
								{
									warn("Unable to create reward!");
								}
								rewards.add(reward);
							}
						}
						_promoCodes.put(name, new PromoCodeTemplate(name, minLvl, maxLvl, canUseSubClass, fromValue, toValue, limit, rewards, limitByAccount, limitHWID));
					}
				}
			}
		}
	}
	
	public PromoCodeTemplate getPromoCode(String name)
	{
		return _promoCodes.get(name);
	}
	
	public boolean isValidCheckTime(Player player, boolean isGetReward)
	{
		final var l = System.currentTimeMillis();
		
		if (isGetReward)
		{
			if (_delayRewardList.containsKey(player.getIPAddress()))
			{
				if (l < _delayRewardList.get(player.getIPAddress()))
				{
					player.sendMessage((new ServerMessage("PromoCode.DELAY_MSG", player.getLang())).toString());
					return false;
				}
			}
			_delayRewardList.put(player.getIPAddress(), (System.currentTimeMillis() + (Config.PROMOCODES_USE_DELAY * 1000)));
		}
		else
		{
			if (_delayList.containsKey(player.getIPAddress()))
			{
				if (l < _delayList.get(player.getIPAddress()))
				{
					player.sendMessage((new ServerMessage("PromoCode.DELAY_MSG", player.getLang())).toString());
					return false;
				}
			}
			_delayList.put(player.getIPAddress(), (System.currentTimeMillis() + (Config.PROMOCODES_USE_DELAY * 1000)));
		}
		return true;
	}
	
	public boolean isActivePromoCode(PromoCodeTemplate tpl, Player player, boolean saveInfo)
	{
		if (!canUse)
		{
			return false;
		}
		
		final long l = System.currentTimeMillis();
		
		if (!tpl.canUseSubClass() && player.isSubClassActive())
		{
			player.sendMessage((new ServerMessage("PromoCode.CANT_USE_SUB", player.getLang())).toString());
			return false;
		}
		
		if (player.getLevel() < tpl.getMinLvl() || player.getLevel() > tpl.getMaxLvl())
		{
			player.sendMessage((new ServerMessage("PromoCode.WRONG_LEVEL", player.getLang())).toString());
			return false;
		}
		
		if (_codeCharList.containsKey(tpl.getName()))
		{
			if (_codeCharList.get(tpl.getName()).contains(player.getObjectId()))
			{
				player.sendMessage((new ServerMessage("PromoCode.ALREADY_USED", player.getLang())).toString());
				return false;
			}
		}
		
		if ((tpl.getStartDate() > 0) && (l < tpl.getStartDate()))
		{
			player.sendMessage((new ServerMessage("PromoCode.NOT_START", player.getLang())).toString());
			return false;
		}
		
		if ((tpl.getEndDate() > 0) && (l > tpl.getEndDate()))
		{
			player.sendMessage((new ServerMessage("PromoCode.ALREADY_END", player.getLang())).toString());
			return false;
		}
		
		if (tpl.isLimitByAccount() && _codeAccountList.containsKey(tpl.getName()))
		{
			if (_codeAccountList.get(tpl.getName()).contains(player.getHWID()))
			{
				player.sendMessage((new ServerMessage("PromoCode.ALREADY_USED", player.getLang())).toString());
				return false;
			}
		}
		
		if (tpl.isLimitHWID() && _codeHwidList.containsKey(tpl.getName()))
		{
			if (_codeHwidList.get(tpl.getName()).contains(player.getHWID()))
			{
				player.sendMessage((new ServerMessage("PromoCode.ALREADY_USED", player.getLang())).toString());
				return false;
			}
		}
		
		if (tpl.getLimit() > 0)
		{
			final var i = tpl.getCurLimit();
			if (i >= tpl.getLimit())
			{
				player.sendMessage((new ServerMessage("PromoCode.LIMIT_EXCEEDED", player.getLang())).toString());
				return false;
			}
			
			if (saveInfo)
			{
				tpl.setCurLimit(i + 1);
			}
		}
		
		if (saveInfo)
		{
			PromoCodeDAO.getInstance().insert(player, tpl);
		}
		return true;
	}
	
	public void addToHwidList(String name, String hwid)
	{
		if (!_codeHwidList.containsKey(name))
		{
			_codeHwidList.put(name, new ArrayList<>());
		}
		_codeHwidList.get(name).add(hwid);
	}
	
	public void addToAccountList(String name, String account)
	{
		if (!_codeAccountList.containsKey(name))
		{
			_codeAccountList.put(name, new ArrayList<>());
		}
		_codeAccountList.get(name).add(account);
	}
	
	public void addToCharList(String name, int objId)
	{
		if (!_codeCharList.containsKey(name))
		{
			_codeCharList.put(name, new ArrayList<>());
		}
		_codeCharList.get(name).add(objId);
	}
	
	private void restoreCodes()
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT * FROM promocodes");
			rset = statement.executeQuery();
			PromoCodeTemplate tpl;
			while (rset.next())
			{
				tpl = getPromoCode(rset.getString("name"));
				if (tpl != null && tpl.getLimit() > 0)
				{
					tpl.setCurLimit(rset.getInt("value"));
				}
			}
			rset.close();
			statement.close();
		}
		catch (final Exception e)
		{
			warn("Could not restore promocodes " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
	}
	
	private void restoreCharacterCodes()
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT * FROM character_promocodes");
			rset = statement.executeQuery();
			while (rset.next())
			{
				addToCharList(rset.getString("name"), rset.getInt("charId"));
			}
			rset.close();
			statement.close();
		}
		catch (final Exception e)
		{
			warn("Could not restore character_promocodes " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
	}
	
	private void restoreAccountCodes()
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT * FROM character_promocodes_account");
			rset = statement.executeQuery();
			while (rset.next())
			{
				addToAccountList(rset.getString("name"), rset.getString("account"));
			}
			rset.close();
			statement.close();
		}
		catch (final Exception e)
		{
			warn("Could not restore character_promocodes_account " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
	}
	
	private void restoreHwidCodes()
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT * FROM character_promocodes_hwid");
			rset = statement.executeQuery();
			while (rset.next())
			{
				addToHwidList(rset.getString("name"), rset.getString("hwid"));
			}
			rset.close();
			statement.close();
		}
		catch (final Exception e)
		{
			warn("Could not restore character_promocodes_hwid " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
	}

	public static PromoCodeParser getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final PromoCodeParser _instance = new PromoCodeParser();
	}
}