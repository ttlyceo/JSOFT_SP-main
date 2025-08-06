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
package l2e.gameserver.handler.admincommandhandlers.impl;

import java.util.Calendar;
import java.util.Date;
import java.util.StringTokenizer;

import l2e.commons.util.StringUtil;
import l2e.commons.util.Util;
import l2e.gameserver.SevenSigns;
import l2e.gameserver.data.holder.ClanHolder;
import l2e.gameserver.handler.admincommandhandlers.IAdminCommandHandler;
import l2e.gameserver.instancemanager.AuctionManager;
import l2e.gameserver.instancemanager.CHSiegeManager;
import l2e.gameserver.instancemanager.CastleManager;
import l2e.gameserver.instancemanager.ClanHallManager;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.model.Clan;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.entity.Castle;
import l2e.gameserver.model.entity.ClanHall;
import l2e.gameserver.model.entity.clanhall.SiegableHall;
import l2e.gameserver.model.zone.type.ClanHallZone;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;

public class Siege implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
	        "admin_siege", "admin_castle", "admin_siegeclanhall", "admin_clanhall", "admin_add_attacker", "admin_add_defender", "admin_add_guard", "admin_list_siege_clans", "admin_clear_siege_list", "admin_move_defenders", "admin_spawn_doors", "admin_endsiege", "admin_startsiege", "admin_setsiegetime", "admin_setcastle", "admin_removecastle", "admin_clanhall", "admin_clanhallset", "admin_clanhalldel", "admin_clanhallopendoors", "admin_clanhallclosedoors", "admin_clanhallteleportself"
	};

	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		final StringTokenizer st = new StringTokenizer(command, " ");
		command = st.nextToken();
		
		Castle castle = null;
		ClanHall clanhall = null;
		
		if (st.hasMoreTokens())
		{
			Player player = null;
			if ((activeChar.getTarget() != null) && activeChar.getTarget().isPlayer())
			{
				player = activeChar.getTarget().getActingPlayer();
			}
			
			String val = st.nextToken();
			if (command.startsWith("admin_clanhall"))
			{
				if (Util.isDigit(val))
				{
					clanhall = ClanHallManager.getInstance().getClanHallById(Integer.parseInt(val));
					Clan clan = null;
					switch (command)
					{
						case "admin_clanhallset" :
							if ((player == null) || (player.getClan() == null))
							{
								activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
								return false;
							}
							
							if (clanhall.getOwnerId() > 0)
							{
								activeChar.sendMessage("This Clan Hall is not free!");
								return false;
							}
							
							clan = player.getClan();
							if (clan.getHideoutId() > 0)
							{
								activeChar.sendMessage("You have already a Clan Hall!");
								return false;
							}
							
							if (!clanhall.isSiegableHall())
							{
								ClanHallManager.getInstance().setOwner(clanhall.getId(), clan);
								if (AuctionManager.getInstance().getAuction(clanhall.getId()) != null)
								{
									AuctionManager.getInstance().getAuction(clanhall.getId()).deleteAuctionFromDB();
								}
							}
							else
							{
								clanhall.setOwner(clan);
								clan.setHideoutId(clanhall.getId());
							}
							break;
						case "admin_clanhalldel" :
							
							if (!clanhall.isSiegableHall())
							{
								if (!ClanHallManager.getInstance().isFree(clanhall.getId()))
								{
									ClanHallManager.getInstance().setFree(clanhall.getId());
									AuctionManager.getInstance().initNPC(clanhall.getId());
								}
								else
								{
									activeChar.sendMessage("This Clan Hall is already free!");
								}
							}
							else
							{
								final int oldOwner = clanhall.getOwnerId();
								if (oldOwner > 0)
								{
									clanhall.free();
									clan = ClanHolder.getInstance().getClan(oldOwner);
									if (clan != null)
									{
										clan.setHideoutId(0);
										clan.broadcastClanStatus();
									}
								}
							}
							break;
						case "admin_clanhallopendoors" :
							clanhall.openCloseDoors(true);
							break;
						case "admin_clanhallclosedoors" :
							clanhall.openCloseDoors(false);
							break;
						case "admin_clanhallteleportself" :
							final ClanHallZone zone = clanhall.getZone();
							if (zone != null)
							{
								activeChar.teleToLocation(zone.getSpawnLoc(), true, ReflectionManager.DEFAULT);
							}
							break;
						default :
							if (!clanhall.isSiegableHall())
							{
								showClanHallPage(activeChar, clanhall);
							}
							else
							{
								showSiegableHallPage(activeChar, (SiegableHall) clanhall);
							}
							break;
					}
				}
			}
			else
			{
				castle = CastleManager.getInstance().getCastleById(Integer.parseInt(val));
				if (castle == null || castle.getTemplate() == null || castle.getSiege() == null)
				{
					return false;
				}
				switch (command)
				{
					case "admin_add_attacker" :
						if (player == null)
						{
							activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
						}
						else
						{
							castle.getSiege().registerAttacker(player, true);
							showSiegePage(activeChar, castle);
						}
						break;
					case "admin_add_defender" :
						if (player == null)
						{
							activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
						}
						else
						{
							castle.getSiege().registerDefender(player, true);
							showSiegePage(activeChar, castle);
						}
						break;
					case "admin_add_guard" :
						if (st.hasMoreTokens())
						{
							val = st.nextToken();
							if (Util.isDigit(val))
							{
								castle.getSiege().getSiegeGuardManager().addSiegeGuard(activeChar, Integer.parseInt(val));
								showSiegePage(activeChar, castle);
								break;
							}
						}
						activeChar.sendMessage("Usage: //add_guard castle npcId");
						break;
					case "admin_clear_siege_list" :
						castle.getSiege().clearSiegeClan();
						showSiegePage(activeChar, castle);
						break;
					case "admin_endsiege" :
						castle.getSiege().endSiege();
						showSiegePage(activeChar, castle);
						break;
					case "admin_list_siege_clans" :
						castle.getSiege().listRegisterClan(activeChar);
						break;
					case "admin_move_defenders" :
						activeChar.sendMessage("Not implemented yet.");
						break;
					case "admin_setcastle" :
						if ((player == null) || (player.getClan() == null))
						{
							activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
						}
						else
						{
							castle.setOwner(player.getClan());
							showSiegePage(activeChar, castle);
						}
						break;
					case "admin_removecastle" :
						final Clan clan = ClanHolder.getInstance().getClan(castle.getOwnerId());
						if (clan != null)
						{
							castle.removeOwner(clan);
							showSiegePage(activeChar, castle);
						}
						else
						{
							activeChar.sendMessage("Unable to remove castle.");
						}
						break;
					case "admin_setsiegetime" :
						if (st.hasMoreTokens())
						{
							val = st.nextToken();
							final Calendar newAdminSiegeDate = Calendar.getInstance();
							newAdminSiegeDate.setTimeInMillis(castle.getSiege().getSiegeStartTime());
							if (val.equalsIgnoreCase("day"))
							{
								newAdminSiegeDate.set(Calendar.DAY_OF_YEAR, Integer.parseInt(st.nextToken()));
							}
							else if (val.equalsIgnoreCase("hour"))
							{
								newAdminSiegeDate.set(Calendar.HOUR_OF_DAY, Integer.parseInt(st.nextToken()));
							}
							else if (val.equalsIgnoreCase("min"))
							{
								newAdminSiegeDate.set(Calendar.MINUTE, Integer.parseInt(st.nextToken()));
							}
							
							if (newAdminSiegeDate.getTimeInMillis() < Calendar.getInstance().getTimeInMillis())
							{
								activeChar.sendMessage("Unable to change siege date.");
							}
							else if (newAdminSiegeDate.getTimeInMillis() != castle.getSiege().getSiegeStartTime())
							{
								castle.getSiege().setSiegeStartTime(newAdminSiegeDate.getTimeInMillis());
								castle.getSiege().saveSiegeDate();
								activeChar.sendMessage("Castle siege time for castle " + castle.getName(activeChar.getLang()) + " has been changed.");
							}
						}
						showSiegeTimePage(activeChar, castle);
						break;
					case "admin_spawn_doors" :
						castle.spawnDoor();
						showSiegePage(activeChar, castle);
						break;
					case "admin_startsiege" :
						castle.getSiege().setSiegeStartTime(System.currentTimeMillis());
						castle.getSiege().startSiege();
						showSiegePage(activeChar, castle);
						break;
					default :
						showSiegePage(activeChar, castle);
						break;
				}
			}
		}
		else
		{
			if (command.equals("admin_castle"))
			{
				showCastleSelectPage(activeChar);
				return true;
			}
			else if (command.equals("admin_siegeclanhall"))
			{
				showSiegeClanHallSelectPage(activeChar);
				return true;
			}
			else if (command.equals("admin_clanhall"))
			{
				showClanHallSelectPage(activeChar);
				return true;
			}
		}
		return true;
	}
	
	private void showCastleSelectPage(Player activeChar)
	{
		int i = 0;
		final NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		adminReply.setFile(activeChar, activeChar.getLang(), "data/html/admin/castles.htm");
		final StringBuilder cList = new StringBuilder(500);
		for (final Castle castle : CastleManager.getInstance().getCastles())
		{
			if (castle != null)
			{
				final String name = castle.getName(activeChar.getLang());
				StringUtil.append(cList, "<td fixwidth=100 align=center><button value=\"" + name + "\" action=\"bypass -h admin_siege ", castle.getId(), "\" width=80 height=21 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
				i++;
			}
			if (i > 2)
			{
				cList.append("</tr><tr>");
				i = 0;
			}
		}
		adminReply.replace("%castles%", cList.toString());
		activeChar.sendPacket(adminReply);
	}
	
	private void showSiegeClanHallSelectPage(Player activeChar)
	{
		int i = 0;
		final NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		adminReply.setFile(activeChar, activeChar.getLang(), "data/html/admin/siegehallinfo.htm");
		final StringBuilder cList = new StringBuilder(500);
		for (final SiegableHall hall : CHSiegeManager.getInstance().getConquerableHalls().values())
		{
			if (hall != null)
			{
				StringUtil.append(cList, "<td fixwidth=150 align=center><button value=\"" + Util.clanHallName(activeChar, hall.getId()) + "\" action=\"bypass -h admin_chsiege_siegablehall ", String.valueOf(hall.getId()), "\" width=140 height=21 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
				i++;
			}
			if (i > 1)
			{
				cList.append("</tr><tr>");
				i = 0;
			}
		}
		adminReply.replace("%siegableHalls%", cList.toString());
		activeChar.sendPacket(adminReply);
	}
	
	private void showClanHallSelectPage(Player activeChar)
	{
		int i = 0;
		final NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		adminReply.setFile(activeChar, activeChar.getLang(), "data/html/admin/hallinfo.htm");
		final StringBuilder cList = new StringBuilder(500);
		for (final ClanHall clanhall : ClanHallManager.getInstance().getClanHalls().values())
		{
			if (clanhall != null)
			{
				StringUtil.append(cList, "<td fixwidth=150 align=center><a action=\"bypass -h admin_clanhall ", String.valueOf(clanhall.getId()), "\">", Util.clanHallName(activeChar, clanhall.getId()), "</a></td>");
				i++;
			}
			if (i > 1)
			{
				cList.append("</tr><tr>");
				i = 0;
			}
		}
		adminReply.replace("%clanhalls%", cList.toString());
		cList.setLength(0);
		i = 0;
		for (final ClanHall clanhall : ClanHallManager.getInstance().getFreeClanHalls().values())
		{
			if (clanhall != null)
			{
				StringUtil.append(cList, "<td fixwidth=150 align=center><a action=\"bypass -h admin_clanhall ", String.valueOf(clanhall.getId()), "\">", Util.clanHallName(activeChar, clanhall.getId()), "</a></td>");
				i++;
			}
			if (i > 1)
			{
				cList.append("</tr><tr>");
				i = 0;
			}
		}
		adminReply.replace("%freeclanhalls%", cList.toString());
		activeChar.sendPacket(adminReply);
	}
	
	private void showSiegePage(Player activeChar, Castle castle)
	{
		final NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		adminReply.setFile(activeChar, activeChar.getLang(), "data/html/admin/castle.htm");
		adminReply.replace("%castleName%", castle.getName(activeChar.getLang()));
		adminReply.replace("%castleId%", String.valueOf(castle.getId()));
		activeChar.sendPacket(adminReply);
	}
	
	private void showSiegeTimePage(Player activeChar, Castle castle)
	{
		final NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		adminReply.setFile(activeChar, activeChar.getLang(), "data/html/admin/castlesiegetime.htm");
		adminReply.replace("%castleName%", castle.getName(activeChar.getLang()));
		adminReply.replace("%castleId%", String.valueOf(castle.getId()));
		adminReply.replace("%time%", new Date(castle.getSiege().getSiegeStartTime()).toString());
		final Calendar newDay = Calendar.getInstance();
		boolean isSunday = false;
		if (newDay.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)
		{
			isSunday = true;
		}
		else
		{
			newDay.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
		}
		
		if (!SevenSigns.getInstance().isDateInSealValidPeriod(newDay))
		{
			newDay.add(Calendar.DAY_OF_MONTH, 7);
		}
		
		if (isSunday)
		{
			adminReply.replace("%sundaylink%", String.valueOf(newDay.get(Calendar.DAY_OF_YEAR)));
			adminReply.replace("%sunday%", String.valueOf(newDay.get(Calendar.MONTH) + "/" + String.valueOf(newDay.get(Calendar.DAY_OF_MONTH))));
			newDay.add(Calendar.DAY_OF_MONTH, 13);
			adminReply.replace("%saturdaylink%", String.valueOf(newDay.get(Calendar.DAY_OF_YEAR)));
			adminReply.replace("%saturday%", String.valueOf(newDay.get(Calendar.MONTH) + "/" + String.valueOf(newDay.get(Calendar.DAY_OF_MONTH))));
		}
		else
		{
			adminReply.replace("%saturdaylink%", String.valueOf(newDay.get(Calendar.DAY_OF_YEAR)));
			adminReply.replace("%saturday%", String.valueOf(newDay.get(Calendar.MONTH) + "/" + String.valueOf(newDay.get(Calendar.DAY_OF_MONTH))));
			newDay.add(Calendar.DAY_OF_MONTH, 1);
			adminReply.replace("%sundaylink%", String.valueOf(newDay.get(Calendar.DAY_OF_YEAR)));
			adminReply.replace("%sunday%", String.valueOf(newDay.get(Calendar.MONTH) + "/" + String.valueOf(newDay.get(Calendar.DAY_OF_MONTH))));
		}
		activeChar.sendPacket(adminReply);
	}
	
	private void showClanHallPage(Player activeChar, ClanHall clanhall)
	{
		final NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		adminReply.setFile(activeChar, activeChar.getLang(), "data/html/admin/clanhall.htm");
		adminReply.replace("%clanhallName%", Util.clanHallName(activeChar, clanhall.getId()));
		adminReply.replace("%clanhallId%", String.valueOf(clanhall.getId()));
		final Clan owner = ClanHolder.getInstance().getClan(clanhall.getOwnerId());
		adminReply.replace("%clanhallOwner%", (owner == null) ? "None" : owner.getName());
		activeChar.sendPacket(adminReply);
	}
	
	private void showSiegableHallPage(Player activeChar, SiegableHall hall)
	{
		final NpcHtmlMessage msg = new NpcHtmlMessage(5);
		msg.setFile(activeChar, activeChar.getLang(), "data/html/admin/siegablehall.htm");
		msg.replace("%clanhallId%", String.valueOf(hall.getId()));
		msg.replace("%clanhallName%", Util.clanHallName(activeChar, hall.getId()));
		if (hall.getOwnerId() > 0)
		{
			final Clan owner = ClanHolder.getInstance().getClan(hall.getOwnerId());
			msg.replace("%clanhallOwner%", (owner != null) ? owner.getName() : "No Owner");
		}
		else
		{
			msg.replace("%clanhallOwner%", "No Owner");
		}
		activeChar.sendPacket(msg);
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}