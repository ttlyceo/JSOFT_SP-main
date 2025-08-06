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

import java.util.StringTokenizer;

import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.data.dao.CharacterCBTeleportDAO;
import l2e.gameserver.data.htm.HtmCache;
import l2e.gameserver.data.parser.CommunityTeleportsParser;
import l2e.gameserver.handler.communityhandlers.ICommunityBoardHandler;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.instancemanager.SiegeManager;
import l2e.gameserver.instancemanager.TerritoryWarManager;
import l2e.gameserver.instancemanager.ZoneManager;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.community.CBTeleportTemplate;
import l2e.gameserver.model.actor.templates.player.PcTeleportTemplate;
import l2e.gameserver.model.entity.Siege;
import l2e.gameserver.model.entity.events.cleft.AerialCleftEvent;
import l2e.gameserver.model.service.BotFunctions;
import l2e.gameserver.model.strings.server.ServerMessage;
import l2e.gameserver.model.zone.ZoneId;
import l2e.gameserver.model.zone.type.NoRestartZone;
import l2e.gameserver.model.zone.type.SiegeZone;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.ShowBoard;
import l2e.gameserver.network.serverpackets.SystemMessage;

/**
 * Created by LordWinter 25.02.2011
 */
public class CommunityTeleport extends AbstractCommunity implements ICommunityBoardHandler
{
	public CommunityTeleport()
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
		        "_bbsteleport;"
		};
	}
	
	@Override
	public void onBypassCommand(String command, Player player)
	{
		if (!checkCondition(player, new StringTokenizer(command, "_").nextToken(), false, true))
		{
			return;
		}
		
		if ((player.isInSiege() || player.getSiegeState() != 0) && !Config.ALLOW_COMMUNITY_TELEPORT_IN_SIEGE)
		{
			player.sendMessage((new ServerMessage("Community.ALL_DISABLE", player.getLang())).toString());
			return;
		}
		
		if (player.getReflectionId() > 0 || player.isInStoreMode())
		{
			player.sendMessage((new ServerMessage("CommunityTeleport.MSG_1", player.getLang())).toString());
			return;
		}
		
		if (command.equals("_bbsteleport;"))
		{
			showInfo(player, "index");
		}
		else if (command.startsWith("_bbsteleport;delete;"))
		{
			String id = null;
			final StringTokenizer st = new StringTokenizer(command, ";");
			st.nextToken();
			st.nextToken();
			try
			{
				id = st.nextToken();
			}
			catch (final Exception e)
			{}
			
			if (id != null)
			{
				final int tpId = Integer.parseInt(id);
				if (player.getCBTeleport(tpId) != null)
				{
					player.removeCBTeleport(tpId);
					CharacterCBTeleportDAO.getInstance().delete(player, tpId);
					showInfo(player, "index");
				}
			}
		}
		else if (command.startsWith("_bbsteleport;save;"))
		{
			String name = null;
			final StringTokenizer st = new StringTokenizer(command, ";");
			st.nextToken();
			st.nextToken();
			try
			{
				name = st.nextToken();
			}
			catch (final Exception e)
			{}
			
			if (name != null)
			{
				addNewPosition(player, name);
			}
			else
			{
				player.sendMessage((new ServerMessage("CommunityTeleport.MSG_5", player.getLang())).toString());
			}
			showInfo(player, "index");
		}
		else if (command.startsWith("_bbsteleport;tpl;"))
		{
			String id = null;
			String price = null;
			final StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken();
			try
			{
				id = st.nextToken();
			}
			catch (final Exception e)
			{}
			
			try
			{
				price = st.nextToken();
			}
			catch (final Exception e)
			{}
			
			if (Config.BLOCK_TP_AT_SIEGES_FOR_ALL && isSiegesIsAcvite())
			{
				player.sendMessage((new ServerMessage("Community.ALL_DISABLE", player.getLang())).toString());
				return;
			}
			
			if (id != null && price != null)
			{
				final PcTeleportTemplate tpl = player.getCBTeleport(Integer.parseInt(id));
				if (tpl != null)
				{
					doTeleport(player, tpl.getX(), tpl.getY(), tpl.getZ(), Integer.parseInt(price));
				}
			}
		}
		else if (command.startsWith("_bbsteleport;teleport;"))
		{
			String x = null;
			String y = null;
			String z = null;
			String price = null;
			final StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken();
			
			try
			{
				x = st.nextToken();
			}
			catch (final Exception e)
			{}
			
			try
			{
				y = st.nextToken();
			}
			catch (final Exception e)
			{}
			
			try
			{
				z = st.nextToken();
			}
			catch (final Exception e)
			{}
			
			try
			{
				price = st.nextToken();
			}
			catch (final Exception e)
			{}
			
			if (!Config.ALLOW_COMMUNITY_COORDS_TP)
			{
				player.sendMessage((new ServerMessage("Community.ALL_DISABLE", player.getLang())).toString());
				return;
			}
			
			if (x != null && y != null && z != null && price != null)
			{
				if (BotFunctions.getInstance().isAutoTpByCoordsEnable(player))
				{
					BotFunctions.getInstance().getAutoTeleportByCoords(player, player.getLocation(), new Location(Integer.parseInt(x), Integer.parseInt(y), Integer.parseInt(z)));
					return;
				}
				doTeleport(player, Integer.parseInt(x), Integer.parseInt(y), Integer.parseInt(z), Integer.parseInt(price));
			}
		}
		else if (command.startsWith("_bbsteleport;id;"))
		{
			final StringTokenizer st = new StringTokenizer(command, ";");
			st.nextToken();
			st.nextToken();
			
			String id = null;
			try
			{
				id = st.nextToken();
			}
			catch (final Exception e)
			{}
			
			if (id != null)
			{
				final CBTeleportTemplate template = CommunityTeleportsParser.getInstance().getTemplate(Integer.parseInt(id));
				if (template != null)
				{
					if (player.isCombatFlagEquipped() || player.isDead() || player.isAlikeDead() || player.isCastingNow() || player.isAttackingNow() || player.isInOlympiadMode() || player.isJailed() || player.isFlying() || (player.getKarma() > 0 && !template.canPk()) || player.isInDuel() || player.getLevel() < template.getMinLvl() || player.getLevel() > template.getMaxLvl())
					{
						player.sendMessage((new ServerMessage("CommunityTeleport.MSG_1", player.getLang())).toString());
						return;
					}
					
					if (template.isForPremium() && !player.hasPremiumBonus())
					{
						player.sendMessage((new ServerMessage("ServiceBBS.ONLY_FOR_PREMIUM", player.getLang())).toString());
						return;
					}
					
					if (player.isFestivalParticipant() || player.isInKrateisCube() || player.getUCState() > 0 || player.isInFightEvent() || ((AerialCleftEvent.getInstance().isStarted() || AerialCleftEvent.getInstance().isRewarding()) && AerialCleftEvent.getInstance().isPlayerParticipant(player.getObjectId())) || player.getReflectionId() != 0)
					{
						player.sendMessage((new ServerMessage("CommunityTeleport.MSG_1", player.getLang())).toString());
						return;
					}
					
					if (player.isInsideZone(ZoneId.NO_RESTART) && !Config.ALLOW_COMMUNITY_TP_NO_RESTART_ZONES)
					{
						player.sendMessage((new ServerMessage("CommunityTeleport.MSG_1", player.getLang())).toString());
						return;
					}
					
					if (player.isInsideZone(ZoneId.SIEGE) && !Config.ALLOW_COMMUNITY_TP_SIEGE_ZONES)
					{
						player.sendMessage((new ServerMessage("CommunityTeleport.MSG_1", player.getLang())).toString());
						return;
					}
					
					final NoRestartZone zone = ZoneManager.getInstance().getZone(template.getLocation().getX(), template.getLocation().getY(), template.getLocation().getZ(), NoRestartZone.class);
					if (zone != null)
					{
						if (!Config.ALLOW_COMMUNITY_TP_NO_RESTART_ZONES)
						{
							player.sendMessage((new ServerMessage("CommunityTeleport.MSG_1", player.getLang())).toString());
							return;
						}
					}
					
					final SiegeZone siegeZone = ZoneManager.getInstance().getZone(template.getLocation().getX(), template.getLocation().getY(), template.getLocation().getZ(), SiegeZone.class);
					if (siegeZone != null)
					{
						if (siegeZone.isActive() && !Config.ALLOW_COMMUNITY_TP_SIEGE_ZONES)
						{
							player.sendMessage((new ServerMessage("CommunityTeleport.MSG_1", player.getLang())).toString());
							return;
						}
					}
					
					if (player.getLevel() > template.getFreeLvl())
					{
						if (template.getReguestItem() != null)
						{
							if (player.getInventory().getItemByItemId(template.getReguestItem().getId()) == null)
							{
								player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
								return;
							}
							
							if (player.getInventory().getItemByItemId(template.getReguestItem().getId()).getCount() < template.getReguestItem().getCount())
							{
								player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
								return;
							}
						}
						
						if (template.getPrice() != null && (player.getLevel() > Config.COMMUNITY_FREE_TP_LVL))
						{
							if (player.getInventory().getItemByItemId(template.getPrice().getId()) == null)
							{
								player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
								return;
							}
							
							if (player.getInventory().getItemByItemId(template.getPrice().getId()).getCount() < template.getPrice().getCount())
							{
								player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
								return;
							}
							player.destroyItemByItemId("Teleport", template.getPrice().getId(), template.getPrice().getCount(), player, true);
						}
					}
					player.sendPacket(new ShowBoard());
					if (BotFunctions.getInstance().isAutoTpByIdEnable(player))
					{
						BotFunctions.getInstance().getAutoTeleportById(player, player.getLocation(), new Location(template.getLocation().getX(), template.getLocation().getY(), template.getLocation().getZ()), 0);
						return;
					}
					player.teleToLocation(template.getLocation().getX(), template.getLocation().getY(), template.getLocation().getZ(), true, ReflectionManager.DEFAULT);
				}
			}
		}
		else if (command.startsWith("_bbsteleport;page;"))
		{
			String page = null;
			final StringTokenizer st = new StringTokenizer(command, ";");
			st.nextToken();
			st.nextToken();
			
			try
			{
				page = st.nextToken();
			}
			catch (final Exception e)
			{}
			
			if (page != null)
			{
				showInfo(player, page);
			}
		}
	}
	
	private void doTeleport(Player player, int x, int y, int z, int price)
	{
		if (player.isFestivalParticipant() || player.isInKrateisCube() || player.getUCState() > 0 || player.isCombatFlagEquipped() || player.isDead() || player.isAlikeDead() || player.isCastingNow() || player.isAttackingNow() || player.isInOlympiadMode() || player.isJailed() || player.isFlying() || player.isInDuel())
		{
			player.sendMessage((new ServerMessage("CommunityTeleport.MSG_1", player.getLang())).toString());
			return;
		}
		
		if (player.checkInTournament() || player.isInFightEvent() || ((AerialCleftEvent.getInstance().isStarted() || AerialCleftEvent.getInstance().isRewarding()) && AerialCleftEvent.getInstance().isPlayerParticipant(player.getObjectId())) || player.getReflectionId() != 0)
		{
			player.sendMessage((new ServerMessage("CommunityTeleport.MSG_1", player.getLang())).toString());
			return;
		}

		if (player.isInsideZone(ZoneId.NO_RESTART) && !Config.ALLOW_COMMUNITY_TP_NO_RESTART_ZONES)
		{
			player.sendMessage((new ServerMessage("CommunityTeleport.MSG_1", player.getLang())).toString());
			return;
		}
		
		if (player.isInsideZone(ZoneId.SIEGE) && !Config.ALLOW_COMMUNITY_TP_SIEGE_ZONES)
		{
			player.sendMessage((new ServerMessage("CommunityTeleport.MSG_1", player.getLang())).toString());
			return;
		}
		
		final NoRestartZone zone = ZoneManager.getInstance().getZone(x, y, z, NoRestartZone.class);
		if (zone != null)
		{
			if (!Config.ALLOW_COMMUNITY_TP_NO_RESTART_ZONES)
			{
				player.sendMessage((new ServerMessage("CommunityTeleport.MSG_1", player.getLang())).toString());
				return;
			}
		}
		
		final SiegeZone siegeZone = ZoneManager.getInstance().getZone(x, y, z, SiegeZone.class);
		if (siegeZone != null)
		{
			if ((siegeZone.getFortId() > 0 || siegeZone.getCastleId() > 0) && siegeZone.isActive() && !Config.ALLOW_COMMUNITY_TP_SIEGE_ZONES)
			{
				player.sendMessage((new ServerMessage("CommunityTeleport.MSG_1", player.getLang())).toString());
				return;
			}
		}
		
		if (price > 0 && (player.getLevel() > Config.COMMUNITY_FREE_TP_LVL))
		{
			if (player.getAdena() < price)
			{
				player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_NOT_ENOUGH_ADENA));
				return;
			}
			player.reduceAdena("Teleport", price, player, true);
		}
		player.sendPacket(new ShowBoard());
		player.teleToLocation(x, y, z, true, ReflectionManager.DEFAULT);
	}
	
	private void showInfo(Player player, String htm)
	{
		final var button = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/teleports/button.htm");
		final var empty_button = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/teleports/empty_button.htm");
		final StringBuilder points = new StringBuilder();
		if (player.getCBTeleports() == null || player.getCBTeleports().size() == 0)
		{
			points.append(empty_button);
		}
		else
		{
			for (final PcTeleportTemplate tpl : player.getCBTeleports())
			{
				if (tpl != null)
				{
					var info = button;
					info = info.replace("%name%", tpl.getName());
					info = info.replace("%id%", String.valueOf(tpl.getId()));
					points.append(info);
				}
			}
		}
		
		String html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/teleports/" + htm + ".htm");
		html = html.replace("%tp%", points.toString());
		separateAndSend(html, player);
	}
	
	private void addNewPosition(Player player, String name)
	{
		if (player.isDead() || player.isAlikeDead() || player.isCastingNow() || player.isAttackingNow())
		{
			player.sendMessage((new ServerMessage("CommunityTeleport.MSG_2", player.getLang())).toString());
			return;
		}
		
		if (Config.BLOCK_TP_AT_SIEGES_FOR_ALL && isSiegesIsAcvite())
		{
			player.sendMessage((new ServerMessage("Community.ALL_DISABLE", player.getLang())).toString());
			return;
		}
		
		if (player.isInKrateisCube() || player.getUCState() > 0 || player.isInFightEvent() || ((AerialCleftEvent.getInstance().isStarted() || AerialCleftEvent.getInstance().isRewarding()) && AerialCleftEvent.getInstance().isPlayerParticipant(player.getObjectId())) || player.getReflectionId() > 0 || player.isInsideZone(ZoneId.NO_RESTART) || player.isInsideZone(ZoneId.SWAMP) || player.isInsideZone(ZoneId.LANDING) || player.isInsideZone(ZoneId.NO_RESTART) || player.isInsideZone(ZoneId.SIEGE) || player.isInsideZone(ZoneId.MONSTER_TRACK) || player.isInsideZone(ZoneId.CASTLE) || player.isInsideZone(ZoneId.MOTHER_TREE) || player.isInsideZone(ZoneId.SCRIPT) || player.isInsideZone(ZoneId.JAIL) || player.isFlying())
		{
			player.sendMessage((new ServerMessage("CommunityTeleport.MSG_4", player.getLang())).toString());
			return;
		}
		
		if (!Util.isMatchingRegexp(name, Config.CNAME_TEMPLATE))
		{
			player.sendMessage((new ServerMessage("CommunityTeleport.MSG_7", player.getLang())).toString());
			return;
		}
		
		final SiegeZone zone = ZoneManager.getInstance().getZone(player, SiegeZone.class);
		if (zone != null)
		{
			if (zone.getFortId() > 0 || zone.getCastleId() > 0)
			{
				player.sendMessage((new ServerMessage("CommunityTeleport.MSG_4", player.getLang())).toString());
				return;
			}
		}
		
		if (player.isInsideZone(ZoneId.NO_RESTART))
		{
			player.sendMessage((new ServerMessage("CommunityTeleport.MSG_4", player.getLang())).toString());
			return;
		}
		
		if (!CharacterCBTeleportDAO.getInstance().add(player, name))
		{
			final ServerMessage msg = new ServerMessage("CommunityTeleport.MSG_6", player.getLang());
			msg.add(Config.COMMUNITY_TELEPORT_TABS);
			player.sendMessage(msg.toString());
		}
	}
	
	private boolean isSiegesIsAcvite()
	{
		if (TerritoryWarManager.getInstance().isTWInProgress())
		{
			return true;
		}
		
		for (final Siege siege : SiegeManager.getInstance().getSieges())
		{
			if (siege.getIsInProgress())
			{
				return true;
			}
		}
		return false;
	}
	
	@Override
	public void onWriteCommand(String command, String ar1, String ar2, String ar3, String ar4, String ar5, Player activeChar)
	{
	}
	
	public static CommunityTeleport getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final CommunityTeleport _instance = new CommunityTeleport();
	}
}