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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import l2e.commons.dbutils.DbUtils;
import l2e.commons.log.LoggerObject;
import l2e.gameserver.Announcements;
import l2e.gameserver.data.htm.HtmCache;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.punishment.PunishmentAffect;
import l2e.gameserver.model.punishment.PunishmentSort;
import l2e.gameserver.model.punishment.PunishmentTemplate;
import l2e.gameserver.model.punishment.PunishmentType;
import l2e.gameserver.model.strings.server.ServerMessage;
import l2e.gameserver.network.GameClient;
import l2e.gameserver.network.serverpackets.EtcStatusUpdate;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;

public class PunishmentManager extends LoggerObject
{
	private final Map<PunishmentType, List<PunishmentTemplate>> _tasks = new ConcurrentHashMap<>();
	
	protected PunishmentManager()
	{
		load();
	}
	
	private void load()
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			final long expireTimeSecs = System.currentTimeMillis();
			statement = con.prepareStatement("DELETE FROM punishments WHERE expiration > 0 AND expiration < ?");
			statement.setLong(1, expireTimeSecs);
			statement.executeUpdate();
		}
		catch (final SQLException e)
		{
			warn("Error while clean up punishments!", e);
		}
		finally
		{
			DbUtils.closeQuietly(statement);
		}
		
		for (final PunishmentType type : PunishmentType.values())
		{
			_tasks.put(type, new ArrayList<>());
		}
		
		int initiated = 0;
		int expired = 0;
		
		Statement st = null;
		try
		{
			st = con.createStatement();
			rset = st.executeQuery("SELECT * FROM punishments");
			while (rset.next())
			{
				final int id = rset.getInt("id");
				final String key = rset.getString("key");
				final String sortName = rset.getString("sortName");
				final PunishmentSort sort = PunishmentSort.getByName(rset.getString("sort"));
				final PunishmentAffect affect = PunishmentAffect.getByName(rset.getString("affect"));
				final PunishmentType type = PunishmentType.getByName(rset.getString("type"));
				final long expirationTime = rset.getLong("expiration");
				final String reason = rset.getString("reason");
				final String punishedBy = rset.getString("punishedBy");
				if ((type != null) && (affect != null))
				{
					if ((expirationTime > 0) && (System.currentTimeMillis() > expirationTime))
					{
						expired++;
					}
					else
					{
						initiated++;
						_tasks.get(type).add(new PunishmentTemplate(id, key, sortName, sort, affect, type, expirationTime, reason, punishedBy, true));
					}
				}
			}
		}
		catch (final Exception e)
		{
			warn("Error while loading punishments: ", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, st, rset);
		}
		info("Loaded " + initiated + " active and " + expired + " expired punishments.");
	}
	
	public boolean addPunishment(Player player, Player gm, PunishmentTemplate task, boolean enableTask)
	{
		if (player != null && task != null)
		{
			ServerMessage msg = null;
			final long delayTime = ((task.getExpirationTime() - System.currentTimeMillis()) / 1000);
			switch (task.getType())
			{
				case BAN :
					if (player.isSellingBuffs())
					{
						player.unsetVar("offlineBuff");
					}
					
					if (player.isInOfflineMode())
					{
						player.unsetVar("offline");
						player.unsetVar("offlineTime");
						player.unsetVar("storemode");
					}
					player.logout();
					switch (task.getAffect())
					{
						case IP :
							for (final Player pl : GameObjectsStorage.getPlayers())
							{
								if (pl != null && pl.isOnline() && pl.getIPAddress().equals(task.getKey()))
								{
									if (pl.isSellingBuffs())
									{
										pl.unsetVar("offlineBuff");
									}
									
									if (pl.isInOfflineMode())
									{
										pl.unsetVar("offline");
										pl.unsetVar("offlineTime");
										pl.unsetVar("storemode");
									}
									pl.logout();
								}
							}
							break;
						case HWID :
							for (final Player pl : GameObjectsStorage.getPlayers())
							{
								if (pl != null && pl.isOnline() && pl.getHWID().equals(task.getKey()))
								{
									if (pl.isSellingBuffs())
									{
										pl.unsetVar("offlineBuff");
									}
									
									if (pl.isInOfflineMode())
									{
										pl.unsetVar("offline");
										pl.unsetVar("offlineTime");
										pl.unsetVar("storemode");
									}
									pl.logout();
								}
							}
							break;
					}
					msg = delayTime >= 60 ? new ServerMessage("PUNISHMENT.BAN_TIME", true) : new ServerMessage("PUNISHMENT.BAN", true);
					break;
				case CHAT_BAN :
					if (delayTime > 0)
					{
						player.sendMessage("You've been chat banned for " + (delayTime > 60 ? ((delayTime / 60) + " minutes.") : delayTime + " seconds."));
					}
					else
					{
						player.sendMessage("You've been chat banned forever.");
					}
					if (enableTask)
					{
						if (!player.startPunishmentTask(task))
						{
							return false;
						}
					}
					player.sendPacket(new EtcStatusUpdate(player));
					msg = delayTime >= 60 ? new ServerMessage("PUNISHMENT.CHAT_BAN_TIME", true) : new ServerMessage("PUNISHMENT.CHAT_BAN", true);
					break;
				case PARTY_BAN :
					if (enableTask)
					{
						if (!player.startPunishmentTask(task))
						{
							return false;
						}
					}
					msg = delayTime >= 60 ? new ServerMessage("PUNISHMENT.PARTY_BAN_TIME", true) : new ServerMessage("PUNISHMENT.PARTY_BAN", true);
					break;
				case JAIL :
					player.startJail();
					final NpcHtmlMessage htm = new NpcHtmlMessage(0);
					String content = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/jail_in.htm");
					if (content != null)
					{
						content = content.replaceAll("%reason%", task != null ? task.getReason() : "");
						content = content.replaceAll("%punishedBy%", task != null ? task.getPunishedBy() : "");
						htm.setHtml(player, content);
					}
					else
					{
						htm.setHtml(player, "<html><body>You have been put in jail by an admin.</body></html>");
					}
					player.sendPacket(htm);
					
					if (delayTime >= 0)
					{
						player.sendMessage("You've been jailed for " + (delayTime >= 60 ? (((delayTime / 60) + 1) + " minutes.") : delayTime + " seconds."));
					}
					else
					{
						player.sendMessage("You've been jailed forever.");
					}
					
					if (enableTask)
					{
						if (!player.startPunishmentTask(task))
						{
							return false;
						}
					}
					msg = delayTime >= 60 ? new ServerMessage("PUNISHMENT.JAIL_TIME", true) : new ServerMessage("PUNISHMENT.JAIL", true);
					break;
			}
			_tasks.get(task.getType()).add(task);
			
			if (gm != null && gm.getVarB("announce_ban", true) && msg != null)
			{
				msg.add(player.getName(null));
				if (delayTime >= 60)
				{
					msg.add((delayTime / 60) + 1);
				}
				msg.add(task.getReason());
				Announcements.getInstance().announceToAll(msg);
			}
		}
		return true;
	}
	
	public void addAccountPunishment(String account, PunishmentTemplate task, boolean enableTask)
	{
		_tasks.get(task.getType()).add(task);
		if (account != null && task != null)
		{
			String affectInfo = null;
			for (final Player pl : GameObjectsStorage.getPlayers())
			{
				if (pl != null && pl.isOnline() && pl.getAccountName().equals(account))
				{
					if (pl.isSellingBuffs())
					{
						pl.unsetVar("offlineBuff");
					}
					
					if (pl.isInOfflineMode())
					{
						pl.unsetVar("offline");
						pl.unsetVar("offlineTime");
						pl.unsetVar("storemode");
					}
					
					if (affectInfo == null)
					{
						switch (task.getAffect())
						{
							case IP :
								if (pl.getIPAddress() != null)
								{
									affectInfo = pl.getIPAddress();
								}
								break;
							case HWID :
								if (pl.getHWID() != null)
								{
									affectInfo = pl.getHWID();
								}
								break;
						}
					}
					pl.logout();
				}
			}
			
			if (affectInfo != null)
			{
				for (final Player pl : GameObjectsStorage.getPlayers())
				{
					if (pl != null && pl.isOnline())
					{
						switch (task.getAffect())
						{
							case IP :
								if (pl.getIPAddress() != null && pl.getIPAddress().equals(affectInfo))
								{
									if (pl.isSellingBuffs())
									{
										pl.unsetVar("offlineBuff");
									}
									
									if (pl.isInOfflineMode())
									{
										pl.unsetVar("offline");
										pl.unsetVar("offlineTime");
										pl.unsetVar("storemode");
									}
									pl.logout();
								}
								break;
							case HWID :
								if (pl.getHWID() != null && pl.getHWID().equals(affectInfo))
								{
									if (pl.isSellingBuffs())
									{
										pl.unsetVar("offlineBuff");
									}
									
									if (pl.isInOfflineMode())
									{
										pl.unsetVar("offline");
										pl.unsetVar("offlineTime");
										pl.unsetVar("storemode");
									}
									pl.logout();
								}
								break;
						}
					}
				}
			}
		}
	}
	
	public void stopPunishment(GameClient client, PunishmentTemplate template)
	{
		final List<PunishmentTemplate> list = _tasks.get(template.getType());
		if (list == null || list.isEmpty() || client == null)
		{
			return;
		}
		
		PunishmentTemplate task = null;
		for (final PunishmentTemplate tpl : list)
		{
			if (tpl != null && tpl.getId() == template.getId())
			{
				task = tpl;
				break;
			}
		}
		
		if (task != null)
		{
			final Player player = client.getActiveChar();
			if (player != null)
			{
				switch (task.getType())
				{
					case BAN :
					case PARTY_BAN :
						break;
					case CHAT_BAN :
						player.sendMessage("Your Chat ban has been lifted");
						player.sendPacket(new EtcStatusUpdate(player));
						player.getPersonalTasks().removePunishment(task);
						break;
					case JAIL :
						player.stopJail();
						final NpcHtmlMessage msg = new NpcHtmlMessage(0);
						final String content = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/jail_out.htm");
						if (content != null)
						{
							msg.setHtml(player, content);
						}
						else
						{
							msg.setHtml(player, "<html><body>You are free for now, respect server rules!</body></html>");
						}
						player.sendPacket(msg);
						player.getPersonalTasks().removePunishment(task);
						break;
				}
			}
			removeDbInfo(task.getId());
			_tasks.get(task.getType()).remove(task);
		}
	}
	
	public boolean clearPunishment(String key, PunishmentType type, PunishmentAffect aff)
	{
		final List<PunishmentTemplate> list = _tasks.get(type);
		if (list == null || list.isEmpty())
		{
			return false;
		}
		
		PunishmentTemplate template = null;
		for (final PunishmentTemplate tpl : list)
		{
			if (tpl != null && tpl.getAffect() == aff)
			{
				if (tpl.getKey().equals(key))
				{
					template = tpl;
				}
			}
		}
		
		if (template != null)
		{
			removeDbInfo(template.getId());
			_tasks.get(type).remove(template);
			return true;
		}
		return false;
	}
	
	public boolean removePunishment(int id)
	{
		PunishmentTemplate template = null;
		PunishmentType type = null;
		for (final PunishmentType tp : getPunishmentList().keySet())
		{
			if (tp != null)
			{
				for (final PunishmentTemplate pList : getPunishmentList().get(tp))
				{
					if (pList != null)
					{
						if (pList.getId() == id)
						{
							template = pList;
							type = tp;
							break;
						}
					}
				}
			}
		}
		
		if (template != null && type != null)
		{
			if (template.getSort() == PunishmentSort.CHARACTER && !template.getSortName().isEmpty())
			{
				final Player player = GameObjectsStorage.getPlayer(template.getSortName());
				if (player != null)
				{
					switch (template.getType())
					{
						case BAN :
						case PARTY_BAN :
							break;
						case CHAT_BAN :
							player.sendMessage("Your Chat ban has been lifted");
							player.sendPacket(new EtcStatusUpdate(player));
							player.getPersonalTasks().removePunishment(template);
							break;
						case JAIL :
							player.stopJail();
							final NpcHtmlMessage msg = new NpcHtmlMessage(0);
							final String content = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/jail_out.htm");
							if (content != null)
							{
								msg.setHtml(player, content);
							}
							else
							{
								msg.setHtml(player, "<html><body>You are free for now, respect server rules!</body></html>");
							}
							player.sendPacket(msg);
							player.getPersonalTasks().removePunishment(template);
							break;
					}
				}
			}
			removeDbInfo(template.getId());
			_tasks.get(type).remove(template);
			return true;
		}
		return false;
	}
	
	private void removeDbInfo(int id)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("DELETE FROM punishments WHERE id=?");
			statement.setInt(1, id);
			statement.execute();
		}
		catch (final Exception e)
		{
			warn("Could not delete punishment data: " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
	
	public boolean hasPunishment(String key, PunishmentType type, PunishmentAffect aff)
	{
		final List<PunishmentTemplate> list = _tasks.get(type);
		if (list == null || list.isEmpty())
		{
			return false;
		}
		
		for (final PunishmentTemplate tpl : list)
		{
			if (tpl != null && tpl.getAffect() == aff)
			{
				if (tpl.getKey().equals(key))
				{
					if (tpl.getExpirationTime() <= 0 || tpl.getExpirationTime() > System.currentTimeMillis())
					{
						return true;
					}
				}
			}
		}
		return false;
	}
	
	public PunishmentTemplate getPunishmentTemplate(String key, PunishmentType type, PunishmentAffect aff)
	{
		final List<PunishmentTemplate> list = _tasks.get(type);
		if (list == null || list.isEmpty())
		{
			return null;
		}
		
		for (final PunishmentTemplate tpl : list)
		{
			if (tpl != null && tpl.getAffect() == aff)
			{
				if (tpl.getSortName().equals(key))
				{
					if (tpl.getExpirationTime() <= 0 || tpl.getExpirationTime() > System.currentTimeMillis())
					{
						return tpl;
					}
				}
			}
		}
		return null;
	}
	
	public boolean checkPunishment(GameClient client, PunishmentType type)
	{
		final List<PunishmentTemplate> list = _tasks.get(type);
		if (list == null || list.isEmpty() || client == null)
		{
			return false;
		}
		
		boolean found = false;
		for (final PunishmentTemplate tpl : list)
		{
			if (tpl != null)
			{
				switch (tpl.getAffect())
				{
					case ACCOUNT :
						if (client.getLogin().equals(tpl.getKey()))
						{
							found = true;
						}
						break;
					case CHARACTER :
						if (client.getActiveChar() != null && client.getActiveChar().getObjectId() == Integer.parseInt(tpl.getKey()))
						{
							found = true;
						}
						break;
					case IP :
						if (client.getIPAddress() != null && client.getIPAddress().equals(tpl.getKey()))
						{
							found = true;
						}
						break;
					case HWID :
						if (client.getHWID() != null && client.getHWID().equals(tpl.getKey()))
						{
							found = true;
						}
						break;
				}
				
				if (found && (tpl.getExpirationTime() <= 0 || tpl.getExpirationTime() > System.currentTimeMillis()))
				{
					return true;
				}
			}
		}
		return false;
	}
	
	public boolean checkPunishment(GameClient client, PunishmentType type, PunishmentAffect aff)
	{
		final List<PunishmentTemplate> list = _tasks.get(type);
		if (list == null || list.isEmpty() || client == null)
		{
			return false;
		}
		
		boolean found = false;
		for (final PunishmentTemplate tpl : list)
		{
			if (tpl != null && tpl.getAffect() == aff)
			{
				switch (tpl.getAffect())
				{
					case ACCOUNT :
						if (client.getLogin().equals(tpl.getKey()))
						{
							found = true;
						}
						break;
					case CHARACTER :
						if (client.getActiveChar() != null && client.getActiveChar().getObjectId() == Integer.parseInt(tpl.getKey()))
						{
							found = true;
						}
						break;
					case IP :
						if (client.getIPAddress() != null && client.getIPAddress().equals(tpl.getKey()))
						{
							found = true;
						}
						break;
					case HWID :
						if (client.getHWID() != null && client.getHWID().equals(tpl.getKey()))
						{
							found = true;
						}
						break;
				}
				
				if (found && (tpl.getExpirationTime() <= 0 || tpl.getExpirationTime() > System.currentTimeMillis()))
				{
					return true;
				}
			}
		}
		return false;
	}
	
	public Map<PunishmentType, List<PunishmentTemplate>> getPunishmentList()
	{
		return _tasks;
	}
	
	public static final PunishmentManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final PunishmentManager _instance = new PunishmentManager();
	}
}