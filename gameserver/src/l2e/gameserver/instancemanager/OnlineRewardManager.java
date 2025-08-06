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
package l2e.gameserver.instancemanager;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import l2e.commons.log.LoggerObject;
import l2e.commons.time.cron.SchedulingPattern;
import l2e.commons.util.Rnd;
import l2e.commons.util.TimeUtils;
import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.player.online.OnlinePlayerTemplate;
import l2e.gameserver.model.actor.templates.player.online.OnlineRewardTemplate;
import l2e.gameserver.model.holders.ItemHolder;
import l2e.gameserver.model.strings.server.ServerMessage;
import l2e.gameserver.model.strings.server.ServerStorage;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.CreatureSay;
import l2e.gameserver.network.serverpackets.ExPCCafePointInfo;
import l2e.gameserver.network.serverpackets.ExSendUIEvent;
import l2e.gameserver.network.serverpackets.SystemMessage;

/**
 * Created by LordWinter
 */
public class OnlineRewardManager extends LoggerObject
{
	private boolean _hwidCheck;
	private boolean _isShowTimer;
	private int _playersLimit;
	private boolean _isActive = false;
	private final Map<String, OnlinePlayerTemplate> _activePlayers = new HashMap<>();
	private final Map<Integer, OnlineRewardTemplate> _rewardList = new HashMap<>();
	private ScheduledFuture<?> _onlineTask = null;
	
	public OnlineRewardManager()
	{
		if (isAllowXmlFile() && !_isActive)
		{
			_activePlayers.clear();
			_rewardList.clear();
			loadRewards();
			loadDataInfo();
			checkTimeTask();
			_isActive = true;
		}
	}
	
	public boolean reloadRewards()
	{
		if (isAllowXmlFile() && _isActive)
		{
			_isActive = false;
			_rewardList.clear();
			loadRewards();
			_isActive = true;
			return true;
		}
		return false;
	}
	
	private void loadRewards()
	{
		try
		{
			final File file = new File(Config.DATAPACK_ROOT + "/data/stats/services/onlineRewards.xml");
			final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);
			final Document doc1 = factory.newDocumentBuilder().parse(file);

			int counter = 0;
			for (Node n1 = doc1.getFirstChild(); n1 != null; n1 = n1.getNextSibling())
			{
				if ("list".equalsIgnoreCase(n1.getNodeName()))
				{
					_isShowTimer = n1.getAttributes().getNamedItem("showTimer") != null ? Boolean.parseBoolean(n1.getAttributes().getNamedItem("showTimer").getNodeValue()) : true;
					_hwidCheck = Boolean.parseBoolean(n1.getAttributes().getNamedItem("hwidCheck").getNodeValue());
					_playersLimit = Integer.parseInt(n1.getAttributes().getNamedItem("playersLimit").getNodeValue());
					for (Node d1 = n1.getFirstChild(); d1 != null; d1 = d1.getNextSibling())
					{
						if ("reward".equalsIgnoreCase(d1.getNodeName()))
						{
							counter++;
							OnlineRewardTemplate template = null;
							final List<ItemHolder> rewards = new ArrayList<>();
							final int id = Integer.parseInt(d1.getAttributes().getNamedItem("id").getNodeValue());
							final int time = Integer.parseInt(d1.getAttributes().getNamedItem("time").getNodeValue());
							final boolean printItem = d1.getAttributes().getNamedItem("printItem") != null ? Boolean.parseBoolean(d1.getAttributes().getNamedItem("printItem").getNodeValue()) : false;
							for (Node s1 = d1.getFirstChild(); s1 != null; s1 = s1.getNextSibling())
							{
								if ("item".equalsIgnoreCase(s1.getNodeName()))
								{
									final int itemId = Integer.parseInt(s1.getAttributes().getNamedItem("id").getNodeValue());
									final long count = Long.parseLong(s1.getAttributes().getNamedItem("count").getNodeValue());
									final double chance = Double.parseDouble(s1.getAttributes().getNamedItem("chance").getNodeValue());
									rewards.add(new ItemHolder(itemId, count, chance));
								}
							}
							template = new OnlineRewardTemplate(id, time, rewards, printItem);
							
							_rewardList.put(id, template);
						}
					}
				}
			}
			info("Loaded " + counter + " online reward templates.");
		}
		catch (NumberFormatException | DOMException | ParserConfigurationException | SAXException e)
		{
			warn("onlineRewards.xml could not be initialized.", e);
		}
		catch (IOException | IllegalArgumentException e)
		{
			warn("IOException or IllegalArgumentException.", e);
		}
	}
	
	private void loadDataInfo()
	{
		try (
		    var con = DatabaseFactory.getInstance().getConnection(); var statement = con.prepareStatement("SELECT * FROM online_rewards"); var rset = statement.executeQuery())
		{
			while (rset.next())
			{
				final String charInfo = rset.getString("charInfo");
				final String[] list = rset.getString("list").split(";");
				if (list != null)
				{
					final OnlinePlayerTemplate tpl = new OnlinePlayerTemplate(_hwidCheck ? charInfo : "N/A", _hwidCheck ? "N/A" : charInfo);
					for (final String info : list)
					{
						final String[] datas = info.split(",");
						final int playerId = Integer.parseInt(datas[0]);
						final int rewardId = Integer.parseInt(datas[1]);
						final long time = Long.parseLong(datas[2]);
						
						tpl.updatePlayerRewardId(playerId, rewardId);
						tpl.updatePlayerTimer(playerId, time);
					}
					_activePlayers.put(charInfo, tpl);
				}
			}
		}
		catch (final SQLException e)
		{
			warn("Couldnt load online_rewards table");
		}
		catch (final Exception e)
		{
			error("Error while initializing OnlineRewardManager: " + e.getMessage(), e);
		}
	}
	
	private void updateDbInfo(OnlinePlayerTemplate tpl)
	{
		if (tpl == null)
		{
			return;
		}
		
		String data = "";
		for (final int playerId : tpl.getPlayerTimers().keySet())
		{
			data += "" + playerId + "," + tpl.getPlayerRewardId(playerId) + "," + tpl.getPlayerTimer(playerId) + ";";
		}
		
		try (
		    var con = DatabaseFactory.getInstance().getConnection())
		{
			final var statement = con.prepareStatement("REPLACE INTO online_rewards (charInfo,list) VALUES (?,?)");
			statement.setString(1, _hwidCheck ? tpl.getHWID() : tpl.getIP());
			statement.setString(2, data);
			statement.executeUpdate();
			statement.close();
		}
		catch (final Exception e)
		{
			warn("Could not insert online_rewards data: " + e);
		}
	}
	
	private void checkTimeTask()
	{
		final long lastUpdate = ServerVariables.getLong("Online_Rewards", 0);
		if (System.currentTimeMillis() > lastUpdate)
		{
			cleanOnlineRewards();
		}
		else
		{
			_onlineTask = ThreadPoolManager.getInstance().schedule(() -> cleanOnlineRewards(), (lastUpdate - System.currentTimeMillis()));
			info("Next tasks refresh at: " + new Date(lastUpdate));
		}
	}
	
	public void cleanOnlineRewards()
	{
		_activePlayers.clear();
		
		try (
		    var con = DatabaseFactory.getInstance().getConnection(); var statement = con.prepareStatement("DELETE FROM online_rewards;"))
		{
			statement.execute();
		}
		catch (final Exception e)
		{
			warn("Failed clear online_rewards.", e);
		}
		
		final long newTime = new SchedulingPattern("30 6 * * *").next(System.currentTimeMillis());
		ServerVariables.set("Online_Rewards", newTime);
		if (_onlineTask != null)
		{
			_onlineTask.cancel(false);
			_onlineTask = null;
		}
		_onlineTask = ThreadPoolManager.getInstance().schedule(() -> cleanOnlineRewards(), (newTime - System.currentTimeMillis()));
		
		info("Refresh rewards completed!");
		info("Next tasks refresh at: " + new Date(newTime));
		for (final Player player : GameObjectsStorage.getPlayers())
		{
			if (player != null && player.isOnline() && !player.isInOfflineMode())
			{
				player.getPersonalTasks().removeTask(9, false);
				checkOnlineReward(player);
			}
		}
	}
	
	public void checkOnlineReward(Player player)
	{
		if (player == null || !_isActive)
		{
			return;
		}
		
		if (player.isInOfflineMode())
		{
			return;
		}
		
		OnlinePlayerTemplate tpl = _activePlayers.get(_hwidCheck ? player.getHWID() : player.getIPAddress());
		if (tpl != null)
		{
			if (!tpl.getPlayer(player) && tpl.getPlayers().size() < _playersLimit)
			{
				tpl.addPlayer(player);
				final OnlineRewardTemplate reward = getOnlineReward((tpl.getPlayerRewardId(player) + 1));
				if (reward != null)
				{
					setTimeLeft(player, tpl, reward);
				}
			}
		}
		else
		{
			tpl = new OnlinePlayerTemplate(player.getHWID(), player.getIPAddress());
			tpl.addPlayer(player);
			_activePlayers.put(_hwidCheck ? player.getHWID() : player.getIPAddress(), tpl);
			final OnlineRewardTemplate reward = getOnlineReward((tpl.getPlayerRewardId(player) + 1));
			if (reward != null)
			{
				setTimeLeft(player, tpl, reward);
			}
		}
	}
	
	private void setTimeLeft(Player player, OnlinePlayerTemplate template, OnlineRewardTemplate tpl)
	{
		if (player == null || !_isActive)
		{
			return;
		}
		
		if (player.isInOfflineMode())
		{
			return;
		}
		
		final long templateTimer = template.getPlayerTimer(player);
		
		final boolean isNewCalc = templateTimer <= 0;
		final long startTime = System.currentTimeMillis();
		long endTime = 0L;
		if (isNewCalc)
		{
			endTime = startTime + (tpl.getMinutes() * 60000);
		}
		else
		{
			endTime = startTime + templateTimer;
		}
		
		ServerMessage msg = null;
		if (tpl != null && tpl.isPrintItem() && tpl.haveRewards())
		{
			msg = new ServerMessage("OnlineRewards.ITEM_INFO", player.getLang());
			msg.add(Util.getItemName(player, tpl.getRewards().get(0).getId()));
		}
		else
		{
			msg = new ServerMessage("OnlineRewards.LAST_TIME", player.getLang());
		}
		
		if (_isShowTimer)
		{
			if(Config.ENABLE_SOMIK_INTERFACE)
			{
				if(tpl != null)
				{
					final int rewardId = tpl.getRewards().getFirst().getId();
					final long delay = endTime - startTime;
					final String info = String.format("DailyRewardClassId=%d TimeRemaining=%d ", rewardId, delay);
					if(rewardId > 0)
						player.sendPacket(new CreatureSay(0, 13, player.getName(null), info + " "));
				}
			}
			else
				player.sendPacket(new ExSendUIEvent(player, false, false, (int) ((endTime / 1000) - (startTime / 1000)), 0, msg.toString()));
		}
		else
		{
			final ServerMessage message = new ServerMessage("OnlineRewards.LAST_TIME_MSG", player.getLang());
			message.add(TimeUtils.formatTime(player, (int) ((endTime - System.currentTimeMillis()) / 1000), false));
			final CreatureSay pm = new CreatureSay(0, Say2.TELL, ServerStorage.getInstance().getString(player.getLang(), "OnlineRewards.TITLE"), message.toString());
			player.sendPacket(pm);
		}
		template.updatePlayerTimer(player, endTime);
		player.startOnlineRewardTask((endTime - System.currentTimeMillis()));
		updateDbInfo(template);
	}
	
	public void activePlayerDisconnect(Player player)
	{
		if (player == null || !_isActive)
		{
			return;
		}
		
		OnlinePlayerTemplate tpl = _activePlayers.get(_hwidCheck ? player.getHWID() : player.getIPAddress());
		if (tpl == null)
		{
			for (final OnlinePlayerTemplate template : _activePlayers.values())
			{
				if (template != null && template.getPlayer(player))
				{
					tpl = template;
					break;
				}
			}
		}
		
		if (tpl != null)
		{
			if (tpl.getPlayer(player))
			{
				tpl.updatePlayerTimer(player, (tpl.getPlayerTimer(player) - System.currentTimeMillis()));
				tpl.removePlayer(player);
				updateDbInfo(tpl);
			}
		}
	}
	
	public void getOnlineReward(Player player)
	{
		if (player == null || !_isActive)
		{
			return;
		}
		
		final OnlinePlayerTemplate tpl = _activePlayers.get(_hwidCheck ? player.getHWID() : player.getIPAddress());
		if (tpl != null)
		{
			tpl.updatePlayerRewardId(player, (tpl.getPlayerRewardId(player) + 1));
			tpl.updatePlayerTimer(player, 0);
			OnlineRewardTemplate reward = getOnlineReward(tpl.getPlayerRewardId(player));
			if (reward == null)
			{
				return;
			}
			
			if (reward.haveRewards())
			{
				for (final ItemHolder holder : reward.getRewards())
				{
					if (holder != null)
					{
						if (holder.getChance() < 100)
						{
							if (Rnd.chance(holder.getChance()))
							{
								rewardItems(holder, player);
							}
						}
						else
						{
							rewardItems(holder, player);
						}
					}
				}
			}
			
			reward = getOnlineReward((tpl.getPlayerRewardId(player) + 1));
			if (reward != null)
			{
				setTimeLeft(player, tpl, reward);
			}
			else
			{
				updateDbInfo(tpl);
			}
		}
	}
	
	private void rewardItems(ItemHolder holder, Player player)
	{
		if (holder.getId() == -300)
		{
			player.setFame((int) (player.getFame() + holder.getCount()));
			final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.ACQUIRED_S1_REPUTATION_SCORE);
			sm.addNumber((int) holder.getCount());
			player.sendPacket(sm);
			player.sendUserInfo();
		}
		else if (holder.getId() == -200)
		{
			player.getClan().addReputationScore((int) holder.getCount(), true);
			final ServerMessage msg = new ServerMessage("ServiceBBS.ADD_REP", player.getLang());
			msg.add(String.valueOf((int) holder.getCount()));
			player.sendMessage(msg.toString());
		}
		else if (holder.getId() == -100)
		{
			player.setPcBangPoints((int) (player.getPcBangPoints() + holder.getCount()));
			final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.YOU_HAVE_ACQUIRED_S1_PC_CAFE_POINTS);
			sm.addNumber((int) holder.getCount());
			player.sendPacket(sm);
			player.sendPacket(new ExPCCafePointInfo(player.getPcBangPoints(), (int) holder.getCount(), false, false, 1));
		}
		else if (holder.getId() == -1)
		{
			player.setGamePoints(player.getGamePoints() + holder.getCount());
			final ServerMessage msg = new ServerMessage("ServiceBBS.ADD_GAME_POINTS", player.getLang());
			msg.add(String.valueOf((int) holder.getCount()));
			player.sendMessage(msg.toString());
		}
		else
		{
			player.addItem("Online", holder.getId(), holder.getCount(), player, true);
		}
	}
	
	public OnlineRewardTemplate getOnlineReward(int id)
	{
		return _rewardList.get(id);
	}
	
	public Map<Integer, OnlineRewardTemplate> getOnlineRewards()
	{
		return _rewardList;
	}
		
	public boolean isHwidCheck()
	{
		return _hwidCheck;
	}
	
	private boolean isAllowXmlFile()
	{
		return new File(Config.DATAPACK_ROOT + "/data/stats/services/onlineRewards.xml").exists();
	}
	
	public static final OnlineRewardManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final OnlineRewardManager _instance = new OnlineRewardManager();
	}
}