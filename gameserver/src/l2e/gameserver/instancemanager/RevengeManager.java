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
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Calendar;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;

import l2e.commons.log.LoggerObject;
import l2e.commons.util.GameSettings;
import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.data.holder.CharNameHolder;
import l2e.gameserver.data.htm.HtmCache;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.entity.events.cleft.AerialCleftEvent;
import l2e.gameserver.model.holders.SkillHolder;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.zone.ZoneId;
import l2e.gameserver.network.serverpackets.RadarControl;
import l2e.gameserver.network.serverpackets.ShowMiniMap;
import l2e.gameserver.network.serverpackets.TutorialCloseHtml;
import l2e.gameserver.network.serverpackets.TutorialShowHtml;

/**
 * Created by LordWinter
 */
public class RevengeManager extends LoggerObject
{
	public static int[] TELEPORT_PRICE = new int[2];
	
	public RevengeManager()
	{
		if (Config.ALLOW_REVENGE_SYSTEM)
		{
			load();
			cleanUpDatas(false);
		}
	}
	
	private final void load()
	{
		final GameSettings revSettings = new GameSettings();
		final File file = new File(Config.REVENGE_FILE);
		try (
		    InputStream is = new FileInputStream(file))
		{
			revSettings.load(is);
		}
		catch (final Exception e)
		{}
		
		final String[] price = revSettings.getProperty("TeleportPrice", "4037,10").split(",");
		try
		{
			TELEPORT_PRICE[0] = Integer.parseInt(price[0]);
			TELEPORT_PRICE[1] = Integer.parseInt(price[1]);
		}
		catch (final NumberFormatException nfe)
		{}
	}
	
	public void checkKiller(Player player, Player killer)
	{
		if (killer == null || !Config.ALLOW_REVENGE_SYSTEM)
		{
			return;
		}
		
		if (((AerialCleftEvent.getInstance().isStarted() || AerialCleftEvent.getInstance().isRewarding()) && AerialCleftEvent.getInstance().isPlayerParticipant(killer.getObjectId())))
		{
			return;
		}
		
		if ((killer.getParty() != null && killer.getParty().getUCState() != null) || killer.getUCState() > 0)
		{
			return;
		}
		
		if (killer.isInKrateisCube() || killer.isJailed() || killer.isFlying() || killer.isInOlympiadMode() || killer.inObserverMode() || killer.isInDuel() || killer.isInsideZone(ZoneId.NO_RESTART) || killer.isInsideZone(ZoneId.PEACE) || killer.isInsideZone(ZoneId.PVP) || killer.isInSiege() || killer.getReflectionId() > 0 || killer.isInFightEvent() && killer.isInsideZone(ZoneId.FUN_PVP))
		{
			return;
		}
		player.addRevengeId(killer.getObjectId());
		killer.removeRevengeId(player.getObjectId());
	}
	
	public void getRevengeList(Player player)
	{
		if (!Config.ALLOW_REVENGE_SYSTEM)
		{
			return;
		}
		
		if (player.getRevengeList() == null || player.getRevengeList().isEmpty())
		{
			player.sendMessage("Your list is empty...");
			return;
		}
		
		player.setRevengeActive(true);
		
		String html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/revenge/revengeList.htm");
		final String template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/revenge/template.htm");
		
		String block = "";
		String list = "";
		
		for (final int charId : player.getRevengeList())
		{
			block = template;
			String charName = null;
			String className = null;
			boolean isOnline = false;
			final Player target = GameObjectsStorage.getPlayer(charId);
			if (target != null)
			{
				charName = target.getName(null);
				className = Util.className(player, target.getClassId().getId());
				isOnline = true;
			}
			else
			{
				charName = CharNameHolder.getInstance().getNameById(charId);
				className = getClassName(player, charId);
				isOnline = false;
			}
			block = block.replace("%nick%", charName != null ? charName : "");
			block = block.replace("%class%", className != null ? className : "");
			if (isOnline)
			{
				block = block.replace("%button%", "<table><tr><td><button action=\"bypass -h _revenge search " + charId + "\" width=16 height=16 back=\"L2UI_CH3.ShortCut_TooltipMax_Down\" fore=\"L2UI_CH3.ShortCut_TooltipMax\"></td><td><button action=\"bypass -h _revenge kill " + charId + "\" width=16 height=16 back=\"L2UI_CH3.shortcut_next_down\" fore=\"L2UI_CH3.shortcut_next\"></td></tr></table>");
			}
			else
			{
				block = block.replace("%button%", "<font color=\"FF0000\">OFFLINE</font>");
			}
			list += block;
		}
		html = html.replace("%list%", list);
		player.sendPacket(new TutorialShowHtml(html));
	}
	
	public void requestPlayerMenuBypass(Player player, String bypass)
	{
		if (!bypass.startsWith("_revenge"))
		{
			return;
		}
		
		final StringTokenizer st = new StringTokenizer(bypass, " ");
		st.nextToken();
		
		final String action = st.nextToken();
		String charId = null;
		try
		{
			charId = st.nextToken();
		}
		catch (final Exception e)
		{}
		
		switch (action)
		{
			case "search" :
				if (charId != null)
				{
					final Player target = GameObjectsStorage.getPlayer(Integer.parseInt(charId));
					if (target != null)
					{
						new Timer().schedule(new TimerTask()
						{
							@Override
							public void run()
							{
								player.sendPacket(new RadarControl(2, 2, target.getX(), target.getY(), target.getZ()));
								player.sendPacket(new RadarControl(0, 1, target.getX(), target.getY(), target.getZ()));
							}
						}, 500);
						player.sendPacket(new ShowMiniMap(0));
					}
					else
					{
						player.sendMessage("Your target offline...");
					}
				}
				break;
			case "kill" :
				if (charId != null)
				{
					final Player target = GameObjectsStorage.getPlayer(Integer.parseInt(charId));
					if (target != null)
					{
						if (((AerialCleftEvent.getInstance().isStarted() || AerialCleftEvent.getInstance().isRewarding()) && AerialCleftEvent.getInstance().isPlayerParticipant(target.getObjectId())))
						{
							player.sendMessage("Not suitable conditions!");
							return;
						}
						
						if (((AerialCleftEvent.getInstance().isStarted() || AerialCleftEvent.getInstance().isRewarding()) && AerialCleftEvent.getInstance().isPlayerParticipant(player.getObjectId())))
						{
							player.sendMessage("Not suitable conditions!");
							return;
						}
						
						if (player.isDead() || player.isJailed() || player.isFlying() || player.isInOlympiadMode() || player.inObserverMode() || player.isInDuel() || player.isInsideZone(ZoneId.NO_RESTART) || player.getReflectionId() > 0 || player.isInFightEvent())
						{
							player.sendMessage("Not suitable conditions!");
							return;
						}
						
						if (target.isDead() || target.isJailed() || target.isFlying() || target.isInOlympiadMode() || target.inObserverMode() || target.isInDuel() || target.isInsideZone(ZoneId.NO_RESTART) || target.isInsideZone(ZoneId.PEACE) || target.isInsideZone(ZoneId.PVP) || target.isInSiege() || target.getReflectionId() > 0 || target.isInFightEvent() || target.isInsideZone(ZoneId.FUN_PVP))
						{
							player.sendMessage("Not suitable conditions!");
							return;
						}
						
						final Location loc = Location.findPointToStay(target.getLocation(), 80, 120, false);
						if (loc != null)
						{
							if (TELEPORT_PRICE[0] > 0)
							{
								if (player.getInventory().getItemByItemId(TELEPORT_PRICE[0]) == null || player.getInventory().getItemByItemId(TELEPORT_PRICE[0]).getCount() < TELEPORT_PRICE[1])
								{
									player.sendMessage("You need " + TELEPORT_PRICE[1] + " " + Util.getItemName(player, TELEPORT_PRICE[0]) + " to use function!");
									return;
								}
								player.destroyItemByItemId("Rebirth", TELEPORT_PRICE[0], TELEPORT_PRICE[1], player, true);
							}
							final Skill hide = new SkillHolder(922, 1).getSkill();
							if (hide != null)
							{
								hide.getEffects(player, player, false);
							}
							player.teleToLocation(loc.getX(), loc.getY(), loc.getZ(), true, ReflectionManager.DEFAULT);
							player.setRevengeActive(false);
							player.sendPacket(TutorialCloseHtml.STATIC_PACKET);
						}
					}
					else
					{
						player.sendMessage("Your target offline...");
					}
				}
				break;
			case "close" :
				player.setRevengeActive(false);
				player.sendPacket(TutorialCloseHtml.STATIC_PACKET);
				player.getRevengeMark();
				break;
		}
	}
	
	private String getClassName(Player player, int charId)
	{
		int classId = -1;
		try (
		    var con = DatabaseFactory.getInstance().getConnection(); var statement = con.prepareStatement("SELECT classid FROM characters WHERE charId=?"))
		{
			statement.setInt(1, charId);
			
			try (
			    var rset = statement.executeQuery())
			{
				if (rset.next())
				{
					classId = rset.getInt(1);
				}
			}
		}
		catch (final Exception e)
		{
			warn("Error select classId:", e);
		}
		
		if (classId >= 0)
		{
			return Util.className(player, classId);
		}
		return null;
	}
	
	public void cleanUpDatas(boolean isClear)
	{
		if (!Config.ALLOW_REVENGE_SYSTEM)
		{
			return;
		}
		
		if (isClear)
		{
			ServerVariables.set("Revenge_Task", 0);
		}
		
		final Calendar currentTime = Calendar.getInstance();
		
		final long lastUpdate = ServerVariables.getLong("Revenge_Task", 0);
		if (currentTime.getTimeInMillis() > lastUpdate)
		{
			final Calendar newTime = Calendar.getInstance();
			newTime.setLenient(true);
			newTime.set(Calendar.HOUR_OF_DAY, 6);
			newTime.set(Calendar.MINUTE, 30);
			if (newTime.getTimeInMillis() < currentTime.getTimeInMillis())
			{
				newTime.add(Calendar.DAY_OF_MONTH, 1);
			}
			ServerVariables.set("Revenge_Task", newTime.getTimeInMillis());
			
			try (
			    var con = DatabaseFactory.getInstance().getConnection(); var statement = con.prepareStatement("DELETE FROM character_variables WHERE name = ?"))
			{
				statement.setString(1, "revengeList");
				statement.execute();
			}
			catch (final Exception e)
			{
				warn("Failed to clean up revenge datas.", e);
			}
			
			for (final Player player : GameObjectsStorage.getPlayers())
			{
				if (player != null)
				{
					player.clenUpRevengeList();
				}
			}
			
			info("Info reshresh completed.");
			info("Next refresh throught: " + Util.formatTime((int) (newTime.getTimeInMillis() - System.currentTimeMillis()) / 1000));
		}
	}
	
	public static final RevengeManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final RevengeManager _instance = new RevengeManager();
	}
}