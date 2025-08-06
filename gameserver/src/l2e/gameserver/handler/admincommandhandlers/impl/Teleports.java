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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import l2e.commons.dbutils.DbUtils;
import l2e.commons.util.StringUtil;
import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.data.holder.SpawnHolder;
import l2e.gameserver.data.parser.NpcsParser;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.handler.admincommandhandlers.IAdminCommandHandler;
import l2e.gameserver.instancemanager.MapRegionManager;
import l2e.gameserver.instancemanager.RaidBossSpawnManager;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.GrandBossInstance;
import l2e.gameserver.model.actor.instance.RaidBossInstance;
import l2e.gameserver.model.entity.events.cleft.AerialCleftEvent;
import l2e.gameserver.model.holders.SummonRequestHolder;
import l2e.gameserver.model.spawn.Spawner;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.ConfirmDlg;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;

public class Teleports implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
	        "admin_teleport", "admin_show_moves", "admin_show_moves_other", "admin_show_teleport", "admin_teleport_to_character", "admin_teleportto", "admin_move_to", "admin_teleport_character", "admin_recall", "admin_recall_players", "admin_walk", "teleportto", "recall", "admin_recall_npc", "admin_gonorth", "admin_gosouth", "admin_goeast", "admin_gowest", "admin_goup", "admin_godown", "admin_tele", "admin_teleto", "admin_instant_move", "admin_sendhome", "admin_tonpc", "admin_teleto_raid"
	};

	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		final var adminhtm = new NpcHtmlMessage(5);

		if (command.equals("admin_teleto"))
		{
			activeChar.setTeleMode(1);
		}
		if (command.equals("admin_instant_move"))
		{
			activeChar.sendMessage("Instant move ready. Click where you want to go.");
			activeChar.setTeleMode(activeChar.getTeleMode() == 2 ? 0 : 2);
		}
		if (command.equals("admin_teleto r"))
		{
			activeChar.setTeleMode(2);
		}
		if (command.equals("admin_teleto end"))
		{
			activeChar.setTeleMode(0);
		}
		if (command.equals("admin_show_moves"))
		{
			adminhtm.setFile(activeChar, activeChar.getLang(), "data/html/admin/teleports.htm");
			activeChar.sendPacket(adminhtm);
		}
		if (command.equals("admin_show_moves_other"))
		{
			adminhtm.setFile(activeChar, activeChar.getLang(), "data/html/admin/telepots/other.htm");
			activeChar.sendPacket(adminhtm);
		}
		else if (command.equals("admin_show_teleport"))
		{
			showTeleportCharWindow(activeChar);
		}
		else if (command.equals("admin_recall_npc"))
		{
			recallNPC(activeChar);
		}
		else if (command.equals("admin_teleport_to_character"))
		{
			teleportToCharacter(activeChar, activeChar.getTarget());
		}
		else if (command.startsWith("admin_tonpc"))
		{
			final var val = command.substring(12);
			final var st = new StringTokenizer(val);
			var npcId = 0;
			try
			{
				npcId = Integer.parseInt(st.nextToken());
			}
			catch (final Exception e)
			{}
			
			if (npcId != 0)
			{
				Npc npc = null;
				try
				{
					if ((npc = GameObjectsStorage.getByNpcId(npcId)) != null)
					{
						teleportToNpc(activeChar, npc);
						return true;
					}
				}
				catch (final Exception e)
				{}
				activeChar.sendMessage("Npc with id: " + npcId + " not found!");
			}
			else
			{
				activeChar.sendMessage("USAGE: //tonpc npcId");
				return false;
			}
		}
		else if (command.startsWith("admin_teleto_raid"))
		{
			final var val = command.substring(18);
			final var st = new StringTokenizer(val);
			int bossId = 0;
			try
			{
				bossId = Integer.parseInt(st.nextToken());
			}
			catch (final Exception e)
			{}
			
			if (bossId != 0)
			{
				final var spawn = RaidBossSpawnManager.getInstance().getSpawns().get(bossId);
				if (spawn != null)
				{
					final var loc = Location.findPointToStay(spawn.calcSpawnRangeLoc(spawn.getTemplate()), 100, 150, false);
					if (loc != null)
					{
						activeChar.teleToLocation(loc, true, ReflectionManager.DEFAULT);
					}
				}
				else
				{
					activeChar.sendMessage("Raidboss not found!");
					return false;
				}
			}
			else
			{
				activeChar.sendMessage("USAGE: //teleto_raid <id>");
				return false;
			}
		}
		else if (command.startsWith("admin_walk"))
		{
			try
			{
				final var val = command.substring(11);
				final var st = new StringTokenizer(val);
				final var x = Integer.parseInt(st.nextToken());
				final var y = Integer.parseInt(st.nextToken());
				final var z = Integer.parseInt(st.nextToken());
				activeChar.getAI().setIntention(CtrlIntention.MOVING, new Location(x, y, z, 0), 0);
			}
			catch (final Exception e)
			{
				if (Config.DEBUG)
				{
					_log.info("admin_walk: " + e);
				}
			}
		}
		else if (command.startsWith("admin_move_to"))
		{
			try
			{
				final var val = command.substring(14);
				teleportTo(activeChar, val);
			}
			catch (final StringIndexOutOfBoundsException e)
			{
				adminhtm.setFile(activeChar, activeChar.getLang(), "data/html/admin/teleports.htm");
				activeChar.sendPacket(adminhtm);
			}
			catch (final NumberFormatException nfe)
			{
				activeChar.sendMessage("Usage: //move_to <x> <y> <z>");
				adminhtm.setFile(activeChar, activeChar.getLang(), "data/html/admin/teleports.htm");
				activeChar.sendPacket(adminhtm);
			}
		}
		else if (command.startsWith("admin_teleport_character"))
		{
			try
			{
				final var val = command.substring(25);

				teleportCharacter(activeChar, val);
			}
			catch (final StringIndexOutOfBoundsException e)
			{
				activeChar.sendMessage("Wrong or no Coordinates given.");
				showTeleportCharWindow(activeChar);
			}
		}
		else if (command.startsWith("admin_teleportto "))
		{
			try
			{
				final var targetName = command.substring(17);
				final var player = GameObjectsStorage.getPlayer(targetName);
				teleportToCharacter(activeChar, player);
			}
			catch (final StringIndexOutOfBoundsException e)
			{}
		}
		else if (command.startsWith("admin_recall "))
		{
			try
			{
				final String[] param = command.split(" ");
				if (param.length != 2)
				{
					activeChar.sendMessage("Usage: //recall <playername>");
					return false;
				}
				final var targetName = param[1];
				final var player = GameObjectsStorage.getPlayer(targetName);
				if (player != null)
				{
					teleportCharacter(player, activeChar.getX(), activeChar.getY(), activeChar.getZ(), activeChar);
				}
				else
				{
					changeCharacterPosition(activeChar, targetName);
				}
			}
			catch (final StringIndexOutOfBoundsException e)
			{}
		}
		else if (command.startsWith("admin_recall_players"))
		{
			for (final var player : GameObjectsStorage.getPlayers())
			{
				if (player == null)
				{
					continue;
				}
				
				if (player.isInKrateisCube() || player.getUCState() > 0 || player.isRegisteredInFightEvent() || player.isInFightEvent() || player.isBlocked() || player.isCursedWeaponEquipped() || player.isInDuel() || player.isFlying() || player.isJailed() || player.isInOlympiadMode() || player.inObserverMode() || player.isAlikeDead() || player.isInSiege() || player.isFestivalParticipant())
				{
					continue;
				}
				
				if (((AerialCleftEvent.getInstance().isStarted() || AerialCleftEvent.getInstance().isRewarding()) && AerialCleftEvent.getInstance().isPlayerParticipant(player.getObjectId())))
				{
					continue;
				}
								
				if (!Util.checkIfInRange(1000, activeChar, player, false))
				{
					player.addScript(new SummonRequestHolder(activeChar, null, true));
					final var confirm = new ConfirmDlg(SystemMessageId.C1_WISHES_TO_SUMMON_YOU_FROM_S2_DO_YOU_ACCEPT.getId());
					confirm.addCharName(activeChar);
					confirm.addZoneName(activeChar.getX(), activeChar.getY(), activeChar.getZ());
					confirm.addTime(15000);
					confirm.addRequesterId(activeChar.getObjectId());
					player.sendPacket(confirm);
				}
			}
		}
		else if (command.equals("admin_tele"))
		{
			showTeleportWindow(activeChar);
		}
		else if (command.startsWith("admin_teleport"))
		{
			String x = null;
			String y = null;
			String z = null;
			final var st = new StringTokenizer(command, " ");
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
			
			if (x != null && y != null)
			{
				final var z_loc = z != null ? Integer.parseInt(z) : 32767;
				final var loc = Location.parseLoc(x + " " + y + " " + z_loc);
				if (loc != null)
				{
					loc.correctZ();
					activeChar.getAI().setIntention(CtrlIntention.IDLE);
					activeChar.teleToLocation(loc, false, ReflectionManager.DEFAULT);
					activeChar.sendMessage("You have been teleported to " + loc.getX() + " " + loc.getY() + " " + loc.getZ());
				}
			}
		}
		else if (command.startsWith("admin_go"))
		{
			var intVal = 150;
			int x = activeChar.getX(), y = activeChar.getY(),
			        z = activeChar.getZ();
			try
			{
				final var val = command.substring(8);
				final var st = new StringTokenizer(val);
				final var dir = st.nextToken();
				if (st.hasMoreTokens())
				{
					intVal = Integer.parseInt(st.nextToken());
				}
				if (dir.equals("east"))
				{
					x += intVal;
				}
				else if (dir.equals("west"))
				{
					x -= intVal;
				}
				else if (dir.equals("north"))
				{
					y -= intVal;
				}
				else if (dir.equals("south"))
				{
					y += intVal;
				}
				else if (dir.equals("up"))
				{
					z += intVal;
				}
				else if (dir.equals("down"))
				{
					z -= intVal;
				}
				activeChar.teleToLocation(x, y, z, false, ReflectionManager.DEFAULT);
				showTeleportWindow(activeChar);
			}
			catch (final Exception e)
			{
				activeChar.sendMessage("Usage: //go<north|south|east|west|up|down> [offset] (default 150)");
			}
		}
		else if (command.startsWith("admin_sendhome"))
		{
			final var st = new StringTokenizer(command, " ");
			st.nextToken();
			if (st.countTokens() > 1)
			{
				activeChar.sendMessage("Usage: //sendhome <playername>");
			}
			else if (st.countTokens() == 1)
			{
				final var name = st.nextToken();
				final var player = GameObjectsStorage.getPlayer(name);
				if (player == null)
				{
					activeChar.sendPacket(SystemMessageId.TARGET_IS_NOT_FOUND_IN_THE_GAME);
					return false;
				}
				teleportHome(player);
			}
			else
			{
				final var target = activeChar.getTarget();
				if (target instanceof Player)
				{
					teleportHome(target.getActingPlayer());
				}
				else
				{
					activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
				}
			}
		}
		return true;
	}

	private void teleportHome(Player player)
	{
		String regionName;
		switch (player.getRace())
		{
			case Elf :
				regionName = "elf_town";
				break;
			case DarkElf :
				regionName = "darkelf_town";
				break;
			case Orc :
				regionName = "orc_town";
				break;
			case Dwarf :
				regionName = "dwarf_town";
				break;
			case Kamael :
				regionName = "kamael_town";
				break;
			case Human :
			default :
				regionName = "talking_island_town";
		}

		player.teleToLocation(MapRegionManager.getInstance().getMapRegionByName(regionName).getSpawnLoc(), true, ReflectionManager.DEFAULT);
		player.setIsIn7sDungeon(false);
	}

	private void teleportTo(Player activeChar, String Coords)
	{
		try
		{
			final var st = new StringTokenizer(Coords);
			final var x1 = st.nextToken();
			final var x = Integer.parseInt(x1);
			final var y1 = st.nextToken();
			final var y = Integer.parseInt(y1);
			final var z1 = st.nextToken();
			final var z = Integer.parseInt(z1);

			activeChar.getAI().setIntention(CtrlIntention.IDLE);
			activeChar.teleToLocation(x, y, z, false, ReflectionManager.DEFAULT);

			activeChar.sendMessage("You have been teleported to " + Coords);
		}
		catch (final NoSuchElementException nsee)
		{
			activeChar.sendMessage("Wrong or no Coordinates given.");
		}
	}

	private void showTeleportWindow(Player activeChar)
	{
		final var adminhtm = new NpcHtmlMessage(5);
		adminhtm.setFile(activeChar, activeChar.getLang(), "data/html/admin/move.htm");
		activeChar.sendPacket(adminhtm);
	}

	private void showTeleportCharWindow(Player activeChar)
	{
		final var target = activeChar.getTarget();
		Player player = null;
		if (target instanceof Player)
		{
			player = (Player) target;
		}
		else
		{
			activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
			return;
		}
		final var adminReply = new NpcHtmlMessage(5);

		final var replyMSG = StringUtil.concat("<html><title>Teleport Character</title>" + "<body>" + "The character you will teleport is ", player.getName(null), "." + "<br>" + "Co-ordinate x" + "<edit var=\"char_cord_x\" width=110>" + "Co-ordinate y" + "<edit var=\"char_cord_y\" width=110>" + "Co-ordinate z" + "<edit var=\"char_cord_z\" width=110>" + "<button value=\"Teleport\" action=\"bypass -h admin_teleport_character $char_cord_x $char_cord_y $char_cord_z\" width=60 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">" + "<button value=\"Teleport near you\" action=\"bypass -h admin_teleport_character ", String.valueOf(activeChar.getX()), " ", String.valueOf(activeChar.getY()), " ", String.valueOf(activeChar.getZ()), "\" width=115 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">" + "<center><button value=\"Back\" action=\"bypass -h admin_current_player\" width=40 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></center>" + "</body></html>");
		adminReply.setHtml(activeChar, replyMSG);
		activeChar.sendPacket(adminReply);
	}

	private void teleportCharacter(Player activeChar, String Cords)
	{
		final var target = activeChar.getTarget();
		Player player = null;
		if (target instanceof Player)
		{
			player = (Player) target;
		}
		else
		{
			activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
			return;
		}

		if (player.getObjectId() == activeChar.getObjectId())
		{
			player.sendPacket(SystemMessageId.CANNOT_USE_ON_YOURSELF);
		}
		else
		{
			try
			{
				final var st = new StringTokenizer(Cords);
				final var x1 = st.nextToken();
				final var x = Integer.parseInt(x1);
				final var y1 = st.nextToken();
				final var y = Integer.parseInt(y1);
				final var z1 = st.nextToken();
				final var z = Integer.parseInt(z1);
				teleportCharacter(player, x, y, z, null);
			}
			catch (final NoSuchElementException nsee)
			{}
		}
	}

	public static void teleportCharacter(Player player, int x, int y, int z, Player activeChar)
	{
		if (player != null)
		{
			if (player.isJailed())
			{
				activeChar.sendMessage("Sorry, player " + player.getName(null) + " is in Jail.");
			}
			else
			{
				if (activeChar != null && !activeChar.getReflection().isDefault())
				{
					activeChar.sendMessage("You have recalled " + player.getName(null));
				}
				player.sendMessage("Admin is teleporting you.");
				player.getAI().setIntention(CtrlIntention.IDLE);
				player.teleToLocation(x, y, z, true, activeChar != null ? activeChar.getReflection() : ReflectionManager.DEFAULT);
			}
		}
	}

	private void teleportToCharacter(Player activeChar, GameObject target)
	{
		if (target == null)
		{
			activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
			return;
		}

		Player player = null;
		if (target instanceof Player)
		{
			player = (Player) target;
		}
		else
		{
			activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
			return;
		}

		if (player.getObjectId() == activeChar.getObjectId())
		{
			player.sendPacket(SystemMessageId.CANNOT_USE_ON_YOURSELF);
		}
		else
		{
			final var x = player.getX();
			final var y = player.getY();
			final var z = player.getZ();

			activeChar.getAI().setIntention(CtrlIntention.IDLE);
			activeChar.teleToLocation(x, y, z, true, target.getReflection());

			activeChar.sendMessage("You have teleported to " + player.getName(null) + ".");
		}
	}
	
	private void teleportToNpc(Player activeChar, Npc npc)
	{
		if (npc == null)
		{
			activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
			return;
		}
		
		final var x = npc.getX();
		final var y = npc.getY();
		final var z = npc.getZ();
		
		activeChar.getAI().setIntention(CtrlIntention.IDLE);
		activeChar.teleToLocation(x, y, z, true, npc.getReflection());
		
		activeChar.sendMessage("You have teleported to " + npc.getName(activeChar.getLang()) + ".");
	}

	private void changeCharacterPosition(Player activeChar, String name)
	{
		final var x = activeChar.getX();
		final var y = activeChar.getY();
		final var z = activeChar.getZ();
		
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE characters SET x=?, y=?, z=? WHERE char_name=?");
			statement.setInt(1, x);
			statement.setInt(2, y);
			statement.setInt(3, z);
			statement.setString(4, name);
			statement.execute();
			final int count = statement.getUpdateCount();
			if (count == 0)
			{
				activeChar.sendMessage("Character not found or position unaltered.");
			}
			else
			{
				activeChar.sendMessage("Player's [" + name + "] position is now set to (" + x + "," + y + "," + z + ").");
			}
		}
		catch (final SQLException se)
		{
			activeChar.sendMessage("SQLException while changing offline character's position");
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	private void recallNPC(Player activeChar)
	{
		final var obj = activeChar.getTarget();
		if ((obj instanceof Npc) && !((Npc) obj).isMinion() && !(obj instanceof RaidBossInstance) && !(obj instanceof GrandBossInstance))
		{
			final var target = (Npc) obj;

			final var monsterTemplate = target.getTemplate().getId();
			final var template1 = NpcsParser.getInstance().getTemplate(monsterTemplate);
			if (template1 == null)
			{
				activeChar.sendMessage("Incorrect monster template.");
				_log.warn("ERROR: NPC " + target.getObjectId() + " has a 'null' template.");
				return;
			}

			var spawn = target.getSpawn();
			if (spawn == null)
			{
				activeChar.sendMessage("Incorrect monster spawn.");
				_log.warn("ERROR: NPC " + target.getObjectId() + " has a 'null' spawn.");
				return;
			}
			final var respawnTime = spawn.getRespawnDelay() / 1000;

			target.deleteMe();
			spawn.stopRespawn();
			SpawnHolder.getInstance().deleteSpawn(spawn, true);

			try
			{
				spawn = new Spawner(template1);
				if (Config.SAVE_GMSPAWN_ON_CUSTOM)
				{
					spawn.setCustom(true);
				}
				spawn.setX(activeChar.getX());
				spawn.setY(activeChar.getY());
				spawn.setZ(activeChar.getZ());
				spawn.setAmount(1);
				spawn.setHeading(activeChar.getHeading());
				spawn.setRespawnDelay(respawnTime);
				spawn.setReflection(activeChar.getReflection());
				SpawnHolder.getInstance().addNewSpawn(spawn, true);
				spawn.init();

				activeChar.sendMessage("Created " + template1.getName(activeChar.getLang()) + " on " + target.getObjectId() + ".");

				if (Config.DEBUG)
				{
					_log.info("Spawn at X=" + spawn.getX() + " Y=" + spawn.getY() + " Z=" + spawn.getZ());
					_log.warn("GM: " + activeChar.getName(null) + "(" + activeChar.getObjectId() + ") moved NPC " + target.getObjectId());
				}
			}
			catch (final Exception e)
			{
				activeChar.sendMessage("Target is not in game.");
			}

		}
		else if (obj instanceof RaidBossInstance)
		{
			final var target = (RaidBossInstance) obj;
			final var spawn = target.getSpawn();
			if (spawn == null)
			{
				activeChar.sendMessage("Incorrect raid spawn.");
				_log.warn("ERROR: NPC Id" + target.getId() + " has a 'null' spawn.");
				return;
			}
			target.deleteMe();
			RaidBossSpawnManager.getInstance().deleteSpawn(spawn, true);
			try
			{
				final var template = NpcsParser.getInstance().getTemplate(target.getId());
				final var spawnDat = new Spawner(template);
				if (Config.SAVE_GMSPAWN_ON_CUSTOM)
				{
					spawn.setCustom(true);
				}
				spawnDat.setX(activeChar.getX());
				spawnDat.setY(activeChar.getY());
				spawnDat.setZ(activeChar.getZ());
				spawnDat.setAmount(1);
				spawnDat.setHeading(activeChar.getHeading());
				spawnDat.setRespawnMinDelay(43200);
				spawnDat.setRespawnMaxDelay(129600);

				RaidBossSpawnManager.getInstance().addNewSpawn(spawnDat, true);
			}
			catch (final Exception e)
			{
				activeChar.sendPacket(SystemMessageId.TARGET_CANT_FOUND);
			}
		}
		else
		{
			activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
		}
	}

	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}