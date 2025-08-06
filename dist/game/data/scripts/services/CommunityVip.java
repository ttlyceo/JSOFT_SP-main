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
 * this program. If not, see <>.
 */
package services;

import l2e.commons.time.cron.SchedulingPattern;
import l2e.commons.util.HtmlUtil;
import l2e.commons.util.TimeUtils;
import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.data.htm.HtmCache;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.handler.communityhandlers.CommunityBoardHandler;
import l2e.gameserver.handler.communityhandlers.ICommunityBoardHandler;
import l2e.gameserver.handler.communityhandlers.impl.AbstractCommunity;
import l2e.gameserver.instancemanager.ServerVariables;
import l2e.gameserver.instancemanager.VipManager;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.player.vip.VipTemplate;
import l2e.gameserver.model.holders.ItemHolder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Created by LordWinter 02.07.2020
 */
public class CommunityVip extends AbstractCommunity implements ICommunityBoardHandler
{
	private final Map<String, Long> _rewardTime = new HashMap<>();
	
	public CommunityVip()
	{
		_rewardTime.clear();
		load();
		if (Config.DEBUG)
		{
			_log.info(getClass().getSimpleName() + ": Loading all functions.");
		}
	}
	
	@Override
	public String[] getBypassCommands()
	{
		return new String[]
		{
		        "_bbsvip", "_bbsvipList", "_bbsvipReward", "_bbsvipBuy"
		};
	}
	
	@Override
	public void onBypassCommand(String command, Player player)
	{
		final StringTokenizer st = new StringTokenizer(command, "_");
		final String cmd = st.nextToken();
		
		if ("bbsvip".equals(cmd))
		{
			onBypassCommand("_bbsvipList_" + (player.getVipLevel() + 1), player);
			return;
		}
		else if ("bbsvipList".equals(cmd))
		{
			int page = Integer.parseInt(st.nextToken());
			if (page > VipManager.getInstance().getMaxLevel())
			{
				page = VipManager.getInstance().getMaxLevel();
			}
			
			String html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/vip/index.htm");
			final String template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/vip/template.htm");
			
			String block = "";
			String list = "";
			
			final int perpage = 1;
			int counter = 0;
			
			final int totalSize = VipManager.getInstance().getVipTemplates().size();
			final boolean isThereNextPage = totalSize > perpage;
			
			for (int i = (page - 1) * perpage; i < totalSize; i++)
			{
				final VipTemplate vip = VipManager.getInstance().getVipLevel(i + 1);
				if (vip != null)
				{
					block = template;
					
					block = block.replace("%vipLevel%", String.valueOf(vip.getId()));
					block = block.replace("%vipPrev%", String.valueOf(vip.getId() - 1));
					block = block.replace("%vipPoints%", String.valueOf(vip.getPoints()));
					block = block.replace("%expRate%", String.valueOf(vip.getExpRate()));
					block = block.replace("%spRate%", String.valueOf(vip.getSpRate()));
					block = block.replace("%adenaRate%", String.valueOf(vip.getAdenaRate()));
					block = block.replace("%dropRate%", String.valueOf(vip.getDropRate()));
					block = block.replace("%dropRaidRate%", String.valueOf(vip.getDropRaidRate()));
					block = block.replace("%spoilRate%", String.valueOf(vip.getSpoilRate()));
					block = block.replace("%epRate%", String.valueOf(vip.getEpRate()));
					block = block.replace("%enchantRate%", String.valueOf(vip.getEnchantChance()));
					
					list += block;
				}
				counter++;
				
				if (counter >= perpage)
				{
					break;
				}
			}
			final int count = (int) Math.ceil((double) totalSize / perpage);
			
			long totalAmount = 0L;
			VipTemplate vip = VipManager.getInstance().getVipLevel(player.getVipLevel() + 1);
			if (vip == null)
			{
				vip = VipManager.getInstance().getVipLevel(VipManager.getInstance().getMaxLevel());
			}
			
			totalAmount = vip.getPoints();
			
			final long timeRemain = (ServerVariables.getLong("RefreshVipTime") - System.currentTimeMillis()) / 1000;
			
			String msg = player.getLang().equalsIgnoreCase("ru") ? "Нет доступных наград!" : "No Daily Reward!";
			final VipTemplate myVip = VipManager.getInstance().getVipLevel(player.getVipLevel());
			if (myVip.haveRewards())
			{
				if (!canPlayerReward(player))
				{
					final long time = getPlayerTime(player);
					msg = TimeUtils.formatTime(player, (int) ((time - System.currentTimeMillis()) / 1000), false);
				}
				else
				{
					if (player.getLang().equalsIgnoreCase("ru"))
					{
						msg = "<button value=\"Получить Награду\" action=\"bypass _bbsvipReward\" back=\"L2UI_CT1.OlympiadWnd_DF_Fight3None_Down\" width=200 height=30 fore=\"L2UI_CT1.OlympiadWnd_DF_Fight3None\">";
					}
					else
					{
						msg = "<button value=\"Get Reward\" action=\"bypass _bbsvipReward\" back=\"L2UI_CT1.OlympiadWnd_DF_Fight3None_Down\" width=200 height=30 fore=\"L2UI_CT1.OlympiadWnd_DF_Fight3None\">";
					}
				}
			}
			html = html.replace("%reward%", msg);
			html = html.replace("%myVip%", String.valueOf(player.getVipLevel()));
			html = html.replace("%nextVip%", String.valueOf(vip.getId()));
			html = html.replace("%percent%", HtmlUtil.getEternityGauge(400, player.getVipPoints(), totalAmount, false));
			html = html.replace("%myPoints%", String.valueOf(player.getVipPoints()));
			html = html.replace("%totalPoints%", String.valueOf(totalAmount));
			html = html.replace("%refresh_time%", TimeUtils.formatTime(player, (int) timeRemain, false));
			html = html.replace("{list}", list);
			html = html.replace("{navigation}", Util.getNavigationBlock(count, page, totalSize, perpage, isThereNextPage, "_bbsvipList_%s"));
			
			separateAndSend(html, player);
		}
		else if ("bbsvipReward".equals(cmd))
		{
			final VipTemplate myVip = VipManager.getInstance().getVipLevel(player.getVipLevel());
			if (myVip.haveRewards())
			{
				final long time = getPlayerTime(player);
				if (time < System.currentTimeMillis())
				{
					for (final ItemHolder holder : myVip.getDailyItems())
					{
						player.addItem("VIPReward", holder.getId(), holder.getCount(), player, true);
					}
					player.sendMessage(player.getLang().equalsIgnoreCase("ru") ? "Вы успешно получили награду!" : "You have successfully received reward!");
					
					long nextTime = 0;
					final SchedulingPattern pattern = new SchedulingPattern("30 6 * * *");
					if (pattern != null)
					{
						nextTime = pattern.next(System.currentTimeMillis());
					}
					_rewardTime.put(player.getHWID(), nextTime);
					try (
					    Connection con = DatabaseFactory.getInstance().getConnection())
					{
						final PreparedStatement statement = con.prepareStatement("REPLACE INTO vip_rewards (value,time) VALUES (?,?)");
						statement.setString(1, player.getHWID());
						statement.setLong(2, nextTime);
						statement.executeUpdate();
						statement.close();
					}
					catch (final Exception e)
					{
						_log.warn("Could not insert value data: " + e);
					}
				}
			}
			onBypassCommand("_bbsvip", player);
		}
		else if ("bbsvipBuy".equals(cmd))
		{
			if (player.getPremiumBonus().getVipTemplate().getId() < VipManager.getInstance().getMaxLevel())
			{
				final VipTemplate tmp = VipManager.getInstance().getVipLevel(player.getVipLevel() + 1);
				if (tmp != null)
				{
					if (tmp.getRequestItems() != null && !tmp.getRequestItems().isEmpty())
					{
						boolean canBuy = true;
						for (final ItemHolder holder : tmp.getRequestItems())
						{
							if (player.getInventory().getItemByItemId(holder.getId()) == null || player.getInventory().getItemByItemId(holder.getId()).getCount() < holder.getCount())
							{
								player.sendMessage(player.getLang().equalsIgnoreCase("ru") ? "Вам нехватает " + holder.getCount() + " " + Util.getItemName(player, holder.getId()) + " для повышения уровня випа!" : "You need " + holder.getCount() + " " + Util.getItemName(player, holder.getId()) + " to increase vip level!");
								canBuy = false;
							}
						}
						
						if (canBuy)
						{
							for (final ItemHolder holder : tmp.getRequestItems())
							{
								player.destroyItemByItemId("Rebirth", holder.getId(), holder.getCount(), player, true);
							}
							player.setVipLevel(tmp.getId());
							player.refreshVipPoints();
							player.sendMessage(player.getLang().equalsIgnoreCase("ru") ? "Ваш уровень випа увеличен до " + tmp.getId() + " уровня!" : "Your vip level has been increased to " + tmp.getId() + " level!");
						}
						onBypassCommand("_bbsvip", player);
					}
				}
			}
			else
			{
				player.sendMessage(player.getLang().equalsIgnoreCase("ru") ? "Вы достигли максимального уровня випа!" : "You have reached the maximum vip level!");
				onBypassCommand("_bbsvip", player);
			}
		}
	}
	
	private void load()
	{
		try (
		    Connection con = DatabaseFactory.getInstance().getConnection(); PreparedStatement statement = con.prepareStatement("SELECT * FROM vip_rewards ORDER BY value"); ResultSet rset = statement.executeQuery())
		{
			while (rset.next())
			{
				final String value = rset.getString("value");
				final long time = rset.getLong("time");
				_rewardTime.put(value, time);
			}
		}
		catch (final SQLException e)
		{
			_log.warn(getClass().getSimpleName() + ": Couldnt load vip_rewards table");
		}
		catch (final Exception e)
		{
			_log.warn(getClass().getSimpleName() + ": Error while initializing vip rewards: " + e.getMessage(), e);
		}
	}
	
	private boolean canPlayerReward(Player player)
	{
		if (_rewardTime.containsKey(player.getHWID()))
		{
			return System.currentTimeMillis() > _rewardTime.get(player.getHWID());
		}
		return true;
	}
	
	private long getPlayerTime(Player player)
	{
		return _rewardTime.containsKey(player.getHWID()) ? _rewardTime.get(player.getHWID()) : 0;
	}
	
	@Override
	public void onWriteCommand(String command, String s, String s1, String s2, String s3, String s4, Player Player)
	{
	}
	
	public static CommunityVip getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final CommunityVip _instance = new CommunityVip();
	}
	
	public static void main(String[] args)
	{
		if(CommunityBoardHandler.getInstance().getHandler("_bbsvip") == null)
			CommunityBoardHandler.getInstance().registerHandler(new CommunityVip());
	}
}