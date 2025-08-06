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
package l2e.gameserver.handler.communityhandlers.impl;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import l2e.commons.dbutils.DbUtils;
import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.data.holder.CharNameHolder;
import l2e.gameserver.data.htm.HtmCache;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.handler.communityhandlers.ICommunityBoardHandler;
import l2e.gameserver.instancemanager.PunishmentManager;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.punishment.PunishmentAffect;
import l2e.gameserver.model.punishment.PunishmentSort;
import l2e.gameserver.model.punishment.PunishmentTemplate;
import l2e.gameserver.model.punishment.PunishmentType;

/**
 * Created by LordWinter
 */
public class CommunityPunishment extends AbstractCommunity implements ICommunityBoardHandler
{
	public CommunityPunishment()
	{
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
		        "_bbspunishment", "_bbspunishmentlist", "_bbsaddpunishment", "_bbsremovepunishment", "_bbspunishmentAnnounce"
		};
	}
	
	@Override
	public void onBypassCommand(String command, Player player)
	{
		final var gmAcsess = player.getAccessLevel().allowPunishment();
		if (!gmAcsess && !player.getAccessLevel().allowPunishmentChat())
		{
			return;
		}
		
		final var st = new StringTokenizer(command, " ");
		st.nextToken();
		if (command.equalsIgnoreCase("_bbspunishment"))
		{
			onBypassCommand("_bbspunishmentlist", player);
		}
		else if (command.equalsIgnoreCase("_bbspunishmentAnnounce"))
		{
			final var enable = player.getVarB("announce_ban", true);
			player.setVar("announce_ban", enable ? 0 : 1);
			onBypassCommand("_bbspunishmentlist", player);
		}
		else if (command.startsWith("_bbspunishmentlist"))
		{
			if (!gmAcsess)
			{
				return;
			}
			final var sort = st.hasMoreTokens() ? PunishmentSort.getByName(st.nextToken()) : PunishmentSort.ALL;
			final var type = st.hasMoreTokens() ? PunishmentType.getByName(st.nextToken()) : PunishmentType.ALL;
			final var affect = st.hasMoreTokens() ? PunishmentAffect.getByName(st.nextToken()) : PunishmentAffect.ALL;
			final var page = Integer.parseInt(st.hasMoreTokens() ? st.nextToken() : "1");
			final var search = st.hasMoreTokens() ? st.nextToken().trim() : "";
			sendPunishmentList(player, sort, type, affect, page, search);
		}
		else if (command.startsWith("_bbsaddpunishment"))
		{
			final var name = st.hasMoreTokens() ? st.nextToken() : null;
			final var srt = st.hasMoreTokens() ? st.nextToken() : null;
			final var af = st.hasMoreTokens() ? st.nextToken() : null;
			final var t = st.hasMoreTokens() ? st.nextToken() : null;
			final var exp = st.hasMoreTokens() ? st.nextToken() : null;
			var reason = st.hasMoreTokens() ? st.nextToken() : null;
			if (reason != null)
			{
				while (st.hasMoreTokens())
				{
					reason += " " + st.nextToken();
				}
				if (!reason.isEmpty())
				{
					reason = reason.replaceAll("\\$", "\\\\\\$");
					reason = reason.replaceAll("\r\n", "<br1>");
					reason = reason.replace("<", "&lt;");
					reason = reason.replace(">", "&gt;");
				}
			}
			else
			{
				reason = "";
			}
			
			if ((name == null) || (af == null) || (t == null) || (exp == null))
			{
				player.sendMessage("Please fill all the fields!");
				return;
			}
			if (!Util.isDigit(exp) && !exp.equals("-1"))
			{
				player.sendMessage("Incorrect value specified for expiration time!");
				return;
			}
			
			long expirationTime = Integer.parseInt(exp);
			if (expirationTime > 0)
			{
				expirationTime = System.currentTimeMillis() + (expirationTime * 60 * 1000);
			}
			
			final var sort = PunishmentSort.getByName(srt);
			final var affect = PunishmentAffect.getByName(af);
			final var type = PunishmentType.getByName(t);
			if ((sort == null) || (affect == null) || (type == null))
			{
				player.sendMessage("Incorrect value specified for sort/affect/punishment type!");
				return;
			}
			
			if (type != PunishmentType.CHAT_BAN && !player.getAccessLevel().allowPunishment())
			{
				return;
			}
			
			if (isBanPlayer(player, name, sort, affect, type, expirationTime, reason))
			{
				player.sendMessage("New ban records successfully created!");
			}
			
			if (gmAcsess)
			{
				sendPunishmentList(player, sort, type, affect, 1, "");
			}
		}
		else if (command.startsWith("_bbsremovepunishment"))
		{
			if (!gmAcsess)
			{
				return;
			}
			final var value = st.hasMoreTokens() ? st.nextToken() : null;
			final var srt = st.hasMoreTokens() ? st.nextToken() : null;
			final var af = st.hasMoreTokens() ? st.nextToken() : null;
			final var t = st.hasMoreTokens() ? st.nextToken() : null;
			
			var sort = PunishmentSort.getByName(srt);
			var affect = PunishmentAffect.getByName(af);
			var type = PunishmentType.getByName(t);
			if (sort == null)
			{
				sort = PunishmentSort.ALL;
			}
			if (affect == null)
			{
				affect = PunishmentAffect.ALL;
			}
			if (type == null)
			{
				type = PunishmentType.ALL;
			}
			
			if (value != null)
			{
				if (PunishmentManager.getInstance().removePunishment(Integer.parseInt(value)))
				{
					player.sendMessage("Ban records successfully removed!");
				}
			}
			sendPunishmentList(player, sort, type, affect, 1, "");
		}
	}
	
	private boolean isBanPlayer(Player activeChar, String name, PunishmentSort sort, PunishmentAffect affect, PunishmentType type, long expirationTime, String reason)
	{
		if (sort == PunishmentSort.CHARACTER)
		{
			if (affect == PunishmentAffect.ACCOUNT)
			{
				activeChar.sendMessage("Wrong params for ban records!");
				return false;
			}
			
			if (name.equals("ALL"))
			{
				final var enableTask = expirationTime > 0 && type != PunishmentType.BAN;
				String key = null;
				for (final var player : GameObjectsStorage.getPlayers())
				{
					if (player == null)
					{
						continue;
					}
					
					if (player.isInOfflineMode() || player.getClient() == null || !player.isOnline() || player.isGM())
					{
						continue;
					}
					
					switch (affect)
					{
						case CHARACTER :
							key = String.valueOf(player.getObjectId());
							break;
						case IP :
							key = player.getIPAddress();
							if (key != null && (key.equals("N/A") || key.equals("127.0.0.1") || key.isEmpty()))
							{
								key = null;
							}
							break;
						case HWID :
							key = player.getHWID();
							if (key != null && (key.equals("N/A") || key.isEmpty()))
							{
								key = null;
							}
							break;
					}
					
					if (key != null)
					{
						if (PunishmentManager.getInstance().hasPunishment(key, type, affect))
						{
							continue;
						}
						PunishmentManager.getInstance().addPunishment(player, activeChar, new PunishmentTemplate(key, player.getName(null), sort, affect, type, expirationTime, reason, activeChar.getName(null)), enableTask);
					}
				}
				return true;
			}
			else
			{
				final var player = GameObjectsStorage.getPlayer(name);
				if (player != null && player.isOnline() && player.getClient() != null)
				{
					final var enableTask = expirationTime > 0 && type != PunishmentType.BAN;
					String key = null;
					switch (affect)
					{
						case CHARACTER :
							key = String.valueOf(player.getObjectId());
							break;
						case IP :
							key = player.getIPAddress();
							if (key != null && (key.equals("N/A") || key.equals("127.0.0.1") || key.isEmpty()))
							{
								key = null;
							}
							break;
						case HWID :
							key = player.getHWID();
							if (key != null && (key.equals("N/A") || key.isEmpty()))
							{
								key = null;
							}
							break;
					}
					
					if (key != null)
					{
						if (PunishmentManager.getInstance().hasPunishment(key, type, affect))
						{
							activeChar.sendMessage("Character is already affected by that punishment.");
							return false;
						}
						return PunishmentManager.getInstance().addPunishment(player, activeChar, new PunishmentTemplate(key, player.getName(null), sort, affect, type, expirationTime, reason, activeChar.getName(null)), enableTask);
					}
				}
				else
				{
					final var charId = CharNameHolder.getInstance().getIdByName(name);
					if (charId > 0)
					{
						Connection con = null;
						PreparedStatement statement = null;
						ResultSet rset = null;
						try
						{
							con = DatabaseFactory.getInstance().getConnection();
							statement = con.prepareStatement("SELECT ip,hwid FROM characters WHERE charId=?");
							statement.setInt(1, charId);
							rset = statement.executeQuery();
							while (rset.next())
							{
								String key = null;
								switch (affect)
								{
									case CHARACTER :
										key = String.valueOf(charId);
										break;
									case IP :
										key = rset.getString("ip");
										if (key != null && (key.equals("N/A") || key.equals("127.0.0.1") || key.isEmpty()))
										{
											key = null;
										}
										break;
									case HWID :
										key = rset.getString("hwid");
										if (key != null && (key.equals("N/A") || key.isEmpty()))
										{
											key = null;
										}
										break;
								}
								
								if (key != null)
								{
									if (PunishmentManager.getInstance().hasPunishment(key, type, affect))
									{
										activeChar.sendMessage("Character is already affected by that punishment.");
										return false;
									}
									return PunishmentManager.getInstance().addPunishment(null, activeChar, new PunishmentTemplate(key, name, sort, affect, type, expirationTime, reason, activeChar.getName(null)), false);
								}
							}
						}
						catch (final Exception e)
						{
							_log.warn("Failed search character info.", e);
						}
						finally
						{
							DbUtils.closeQuietly(con, statement, rset);
						}
					}
				}
			}
		}
		else
		{
			if (affect == PunishmentAffect.CHARACTER)
			{
				activeChar.sendMessage("Wrong params for ban records!");
				return false;
			}
			
			Connection con = null;
			PreparedStatement statement = null;
			ResultSet rset = null;
			try
			{
				con = DatabaseFactory.getInstance().getConnection();
				statement = con.prepareStatement("SELECT ip,hwid FROM characters WHERE account_name=?");
				statement.setString(1, name);
				rset = statement.executeQuery();
				while (rset.next())
				{
					String key = null;
					switch (affect)
					{
						case ACCOUNT :
							key = name;
							break;
						case IP :
							key = rset.getString("ip");
							if (key != null && (key.equals("N/A") || key.equals("127.0.0.1") || key.isEmpty()))
							{
								key = null;
							}
							break;
						case HWID :
							key = rset.getString("hwid");
							if (key != null && (key.equals("N/A") || key.isEmpty()))
							{
								key = null;
							}
							break;
					}
					
					if (key != null)
					{
						if (PunishmentManager.getInstance().hasPunishment(key, type, affect))
						{
							activeChar.sendMessage("Account is already affected by that punishment.");
							return false;
						}
						PunishmentManager.getInstance().addAccountPunishment(name, new PunishmentTemplate(key, name, sort, affect, type, expirationTime, reason, activeChar.getName(null)), false);
						return true;
					}
				}
			}
			catch (final Exception e)
			{
				_log.warn("Failed search character account info.", e);
			}
			finally
			{
				DbUtils.closeQuietly(con, statement, rset);
			}
		}
		return false;
	}
	
	private void sendPunishmentList(Player player, PunishmentSort sort, PunishmentType type, PunishmentAffect affect, int page, String search)
	{
		var html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/admin/punishment.htm");
		final var template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/admin/punishment_template.htm");
		var block = "";
		var list = "";
		
		final List<PunishmentTemplate> totalList = new ArrayList<>();
		final Map<PunishmentType, List<PunishmentTemplate>> punishments = PunishmentManager.getInstance().getPunishmentList();
		for (final var tp : punishments.keySet())
		{
			if (tp != null && (tp == type || type == PunishmentType.ALL))
			{
				for (final var pList : punishments.get(tp))
				{
					if (pList != null && (pList.getAffect() == affect || affect == PunishmentAffect.ALL) && (sort == PunishmentSort.ALL || sort == pList.getSort()))
					{
						if ((search.isEmpty() || (!search.isEmpty() && pList.getKey().equalsIgnoreCase(search) || pList.getSortName().equalsIgnoreCase(search))))
						{
							if (pList.getExpirationTime() <= 0 || pList.getExpirationTime() > System.currentTimeMillis())
							{
								totalList.add(pList);
							}
						}
					}
				}
			}
		}
		
		final Comparator<PunishmentTemplate> statsComparator = new SortPunishmentInfo();
		Collections.sort(totalList, statsComparator);
		
		final var perpage = 12;
		var counter = 0;
		final var totalSize = totalList.size();
		final var isThereNextPage = totalSize > perpage;
		
		for (int i = (page - 1) * perpage; i < totalSize; i++)
		{
			final var data = totalList.get(i);
			if (data != null)
			{
				block = template;
				
				block = block.replace("%type%", data.getType().name());
				block = block.replace("%sort%", data.getSort().name());
				block = block.replace("%key%", data.getSortName());
				block = block.replace("%owner%", data.getPunishedBy());
				block = block.replace("%reason%", data.getReason());
				block = block.replace("%time%", data.getExpirationTime() <= 0 ? "Forever" : String.valueOf(new SimpleDateFormat("yyyy.MM.dd HH:mm").format(new Date(data.getExpirationTime()))));
				block = block.replace("%affect%", data.getAffect().name());
				block = block.replace("%punishBy%", data.getPunishedBy());
				block = block.replace("%action%", String.valueOf(data.getId()));
				list += block;
			}
			
			counter++;
			
			if (counter >= perpage)
			{
				break;
			}
		}
		
		final var pages = (double) totalSize / perpage;
		final var count = (int) Math.ceil(pages);
		html = html.replace("%type%", type.name());
		html = html.replace("%typeList%", getTypeList(type));
		html = html.replace("%sort%", sort.name());
		html = html.replace("%sortList%", getSortList(sort));
		html = html.replace("%aff%", affect.name());
		html = html.replace("%affList%", getAffectList(affect));
		html = html.replace("%list%", list);
		html = html.replace("%announce_img%", player.getVarB("announce_ban", true) ? "L2UI.CheckBox_checked" : "L2UI.CheckBox");
		html = html.replace("%page%", String.valueOf(page));
		html = html.replace("%navigation%", Util.getNavigationBlock(count, page, totalSize, perpage, isThereNextPage, "_bbspunishmentlist " + sort + " " + type + " " + affect + " %s " + search));
		separateAndSend(html, player);
		totalList.clear();
	}
	
	private static String getSortList(PunishmentSort sort)
	{
		var line = "";
		for (final var srt : PunishmentSort.values())
		{
			if (srt == sort)
			{
				continue;
			}
			line += "" + srt.name() + ";";
		}
		return line;
	}
	
	private static String getTypeList(PunishmentType type)
	{
		var line = "";
		for (final var tp : PunishmentType.values())
		{
			if (tp == type)
			{
				continue;
			}
			line += "" + tp.name() + ";";
		}
		return line;
	}
	
	private static String getAffectList(PunishmentAffect affect)
	{
		var line = "";
		for (final var aff : PunishmentAffect.values())
		{
			if (aff == affect)
			{
				continue;
			}
			line += "" + aff.name() + ";";
		}
		return line;
	}
	
	private static class SortPunishmentInfo implements Comparator<PunishmentTemplate>, Serializable
	{
		private static final long serialVersionUID = 7691414259610932752L;
		
		@Override
		public int compare(PunishmentTemplate o1, PunishmentTemplate o2)
		{
			return Integer.compare(o2.getId(), o1.getId());
		}
	}
	
	@Override
	public void onWriteCommand(String command, String ar1, String ar2, String ar3, String ar4, String ar5, Player activeChar)
	{
	}
	
	public static CommunityPunishment getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final CommunityPunishment _instance = new CommunityPunishment();
	}
}