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

import java.io.File;
import java.io.FileWriter;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import l2e.commons.util.StringUtil;
import l2e.gameserver.Config;
import l2e.gameserver.SevenSigns;
import l2e.gameserver.data.holder.SpawnHolder;
import l2e.gameserver.data.parser.AdminParser;
import l2e.gameserver.data.parser.ColosseumFenceParser;
import l2e.gameserver.data.parser.NpcsParser;
import l2e.gameserver.data.parser.SpawnParser;
import l2e.gameserver.handler.admincommandhandlers.IAdminCommandHandler;
import l2e.gameserver.instancemanager.DayNightSpawnManager;
import l2e.gameserver.instancemanager.MapRegionManager;
import l2e.gameserver.instancemanager.QuestManager;
import l2e.gameserver.instancemanager.RaidBossSpawnManager;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.model.AutoSpawnHandler;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.ColosseumFence;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.entity.Reflection;
import l2e.gameserver.model.spawn.Spawner;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;
import l2e.gameserver.network.serverpackets.SystemMessage;

public class Spawn implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
	        "admin_show_spawns", "admin_spawn", "admin_spawn_fence", "admin_delete_fence", "admin_list_fence", "admin_spawn_monster", "admin_spawn_index", "admin_unspawnall", "admin_respawnall", "admin_spawn_reload", "admin_npc_index", "admin_spawn_once", "admin_show_npcs", "admin_spawnnight", "admin_spawnday", "admin_instance_spawns", "admin_list_spawns", "admin_list_positions", "admin_spawn_debug_menu", "admin_spawn_debug_print", "admin_spawn_debug_print_menu", "admin_dumpspawn", "admin_locspawn", "admin_draw_zone", "admin_draw_banned_zone"
	};
	
	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		final NpcHtmlMessage adminhtm = new NpcHtmlMessage(5);

		if (command.equals("admin_show_spawns"))
		{
			adminhtm.setFile(activeChar, activeChar.getLang(), "data/html/admin/spawns.htm");
			activeChar.sendPacket(adminhtm);
		}
		else if (command.equalsIgnoreCase("admin_spawn_debug_menu"))
		{
			adminhtm.setFile(activeChar, activeChar.getLang(), "data/html/admin/spawns_debug.htm");
			activeChar.sendPacket(adminhtm);
		}
		else if (command.startsWith("admin_spawn_debug_print"))
		{
			final StringTokenizer st = new StringTokenizer(command, " ");
			final GameObject target = activeChar.getTarget();
			if (target instanceof Npc)
			{
				try
				{
					st.nextToken();
					final int type = Integer.parseInt(st.nextToken());
					printSpawn((Npc) target, type);
					if (command.contains("_menu"))
					{
						adminhtm.setFile(activeChar, activeChar.getLang(), "data/html/admin/spawns_debug.htm");
						activeChar.sendPacket(adminhtm);
					}
				}
				catch (final Exception e)
				{}
			}
			else
			{
				activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
			}
		}
		else if (command.startsWith("admin_spawn_index"))
		{
			final StringTokenizer st = new StringTokenizer(command, " ");
			try
			{
				st.nextToken();
				final int level = Integer.parseInt(st.nextToken());
				int from = 0;
				try
				{
					from = Integer.parseInt(st.nextToken());
				}
				catch (final NoSuchElementException nsee)
				{}
				showMonsters(activeChar, level, from);
			}
			catch (final Exception e)
			{
				adminhtm.setFile(activeChar, activeChar.getLang(), "data/html/admin/spawns.htm");
				activeChar.sendPacket(adminhtm);
			}
		}
		else if (command.equals("admin_show_npcs"))
		{
			adminhtm.setFile(activeChar, activeChar.getLang(), "data/html/admin/npcs.htm");
			activeChar.sendPacket(adminhtm);
		}
		else if (command.startsWith("admin_npc_index"))
		{
			final StringTokenizer st = new StringTokenizer(command, " ");
			try
			{
				st.nextToken();
				final String letter = st.nextToken();
				int from = 0;
				try
				{
					from = Integer.parseInt(st.nextToken());
				}
				catch (final NoSuchElementException nsee)
				{}
				showNpcs(activeChar, letter, from);
			}
			catch (final Exception e)
			{
				adminhtm.setFile(activeChar, activeChar.getLang(), "data/html/admin/npcs.htm");
				activeChar.sendPacket(adminhtm);
			}
		}
		else if (command.startsWith("admin_instance_spawns"))
		{
			final StringTokenizer st = new StringTokenizer(command, " ");
			try
			{
				st.nextToken();
				final int instance = Integer.parseInt(st.nextToken());
				if (instance >= 300000)
				{
					final StringBuilder html = StringUtil.startAppend(500 + 1000, "<html><table width=\"100%\"><tr><td width=45><button value=\"Main\" action=\"bypass -h admin_admin\" width=45 height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td><td width=180><center>", "<font color=\"LEVEL\">Spawns for " + String.valueOf(instance) + "</font>", "</td><td width=45><button value=\"Back\" action=\"bypass -h admin_current_player\" width=45 height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr></table><br>", "<table width=\"100%\"><tr><td width=200>NpcName</td><td width=70>Action</td></tr>");
					int counter = 0;
					int skiped = 0;
					final Reflection inst = ReflectionManager.getInstance().getReflection(instance);
					if (inst != null)
					{
						for (final Npc npc : inst.getNpcs())
						{
							if (!npc.isDead())
							{
								if (counter < 50)
								{
									StringUtil.append(html, "<tr><td>" + npc.getName(activeChar.getLang()) + "</td><td>", "<a action=\"bypass -h admin_move_to " + npc.getX() + " " + npc.getY() + " " + npc.getZ() + "\">Go</a>", "</td></tr>");
									counter++;
								}
								else
								{
									skiped++;
								}
							}
						}
						StringUtil.append(html, "<tr><td>Skipped:</td><td>" + String.valueOf(skiped) + "</td></tr></table></body></html>");
						final NpcHtmlMessage ms = new NpcHtmlMessage(1);
						ms.setHtml(activeChar, html.toString());
						activeChar.sendPacket(ms);
					}
					else
					{
						activeChar.sendMessage("Cannot find instance " + instance);
					}
				}
				else
				{
					activeChar.sendMessage("Invalid instance number.");
				}
			}
			catch (final Exception e)
			{
				activeChar.sendMessage("Usage //instance_spawns <instance_number>");
			}
		}
		else if (command.startsWith("admin_unspawnall"))
		{
			final var sm = SystemMessage.getSystemMessage(SystemMessageId.NPC_SERVER_NOT_OPERATING);
			GameObjectsStorage.getPlayers().stream().filter(p -> p != null && p.isOnline()).forEach(p -> p.sendPacket(sm));
			RaidBossSpawnManager.getInstance().cleanUp();
			DayNightSpawnManager.getInstance().cleanUp();
			World.getInstance().deleteVisibleNpcSpawns();
			AdminParser.getInstance().broadcastMessageToGMs("NPC Unspawn completed!");
		}
		else if (command.startsWith("admin_spawnday"))
		{
			DayNightSpawnManager.getInstance().changeMode(0);
		}
		else if (command.startsWith("admin_spawnnight"))
		{
			DayNightSpawnManager.getInstance().changeMode(1);
		}
		else if (command.startsWith("admin_respawnall") || command.startsWith("admin_spawn_reload"))
		{
			RaidBossSpawnManager.getInstance().cleanUp();
			DayNightSpawnManager.getInstance().cleanUp();
			World.getInstance().deleteVisibleNpcSpawns();
			NpcsParser.getInstance();
			RaidBossSpawnManager.getInstance();
			SpawnParser.getInstance().reloadAll();
			SpawnHolder.getInstance().reloadAll();
			AutoSpawnHandler.getInstance().reload();
			SevenSigns.getInstance().spawnSevenSignsNPC();
			QuestManager.getInstance().reloadAllQuests();
			AdminParser.getInstance().broadcastMessageToGMs("NPC Respawn completed!");
		}
		else if (command.startsWith("admin_spawn_fence"))
		{
			final StringTokenizer st = new StringTokenizer(command, " ");
			try
			{
				st.nextToken();
				final int type = Integer.parseInt(st.nextToken());
				final int width = Integer.parseInt(st.nextToken());
				final int height = Integer.parseInt(st.nextToken());
				
				ColosseumFenceParser.getInstance().addDynamic(activeChar, activeChar.getX(), activeChar.getY(), activeChar.getZ(), activeChar.getZ() + 50, activeChar.getZ() - 50, width, height, type);
				listFences(activeChar);
			}
			catch (final Exception e)
			{
				activeChar.sendMessage("Usage: //spawn_fence <type> <width> <height>");
			}
		}
		else if (command.startsWith("admin_delete_fence"))
		{
			final StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken();
			try
			{
				final var object = GameObjectsStorage.findObject(Integer.parseInt(st.nextToken()));
				if (object != null && object instanceof ColosseumFence)
				{
					ColosseumFenceParser.getInstance().removeFence((ColosseumFence) object, Integer.parseInt(st.nextToken()));
					if (st.hasMoreTokens())
					{
						listFences(activeChar);
					}
				}
				else
				{
					activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
				}
			}
			catch (final Exception e)
			{
				activeChar.sendMessage("Usage: //deletefence <objectId> <regionId>");
			}
		}
		else if (command.startsWith("admin_list_fence"))
		{
			listFences(activeChar);
		}
		else if (command.startsWith("admin_spawn_monster") || command.startsWith("admin_spawn"))
		{
			final StringTokenizer st = new StringTokenizer(command, " ");
			try
			{
				final String cmd = st.nextToken();
				final String id = st.nextToken();
				int respawnTime = 60;
				int mobCount = 1;
				if (st.hasMoreTokens())
				{
					mobCount = Integer.parseInt(st.nextToken());
				}
				if (st.hasMoreTokens())
				{
					respawnTime = Integer.parseInt(st.nextToken());
				}
				if (cmd.equalsIgnoreCase("admin_spawn_once"))
				{
					spawnMonster(activeChar, id, respawnTime, mobCount, false, Config.SAVE_GMSPAWN_ON_CUSTOM);
				}
				else
				{
					spawnMonster(activeChar, id, respawnTime, mobCount, true, Config.SAVE_GMSPAWN_ON_CUSTOM);
				}
			}
			catch (final Exception e)
			{
				adminhtm.setFile(activeChar, activeChar.getLang(), "data/html/admin/spawns.htm");
				activeChar.sendPacket(adminhtm);
			}
		}
		else if (command.startsWith("admin_list_spawns") || command.startsWith("admin_list_positions"))
		{
			int npcId = 0;
			int teleportIndex = -1;
			try
			{
				final String[] params = command.split(" ");
				final Pattern pattern = Pattern.compile("[0-9]*");
				final Matcher regexp = pattern.matcher(params[1]);
				if (regexp.matches())
				{
					npcId = Integer.parseInt(params[1]);
				}
				else
				{
					params[1] = params[1].replace('_', ' ');
					npcId = NpcsParser.getInstance().getTemplateByName(params[1]).getId();
				}
				if (params.length > 2)
				{
					teleportIndex = Integer.parseInt(params[2]);
				}
			}
			catch (final Exception e)
			{
				activeChar.sendMessage("Command format is //list_spawns <npcId|npc_name> [tele_index]");
			}
			if (command.startsWith("admin_list_positions"))
			{
				SpawnParser.getInstance().findNPCInstances(activeChar, npcId, teleportIndex, true);
			}
			else
			{
				SpawnParser.getInstance().findNPCInstances(activeChar, npcId, teleportIndex, false);
			}
		}
		else if (command.startsWith("admin_dumpspawn"))
		{
			final StringTokenizer st = new StringTokenizer(command, " ");
			try
			{
				st.nextToken();
				final String id = st.nextToken();
				int respawnTime = 60;
				int mobCount = 1;
				if (st.hasMoreTokens())
				{
					mobCount = Integer.parseInt(st.nextToken());
				}
				if (st.hasMoreTokens())
				{
					respawnTime = Integer.parseInt(st.nextToken());
				}
				
				spawnMonster(activeChar, id, respawnTime, mobCount, true, false);
				try
				{
					new File("./data/stats/npcs/spawns/dumps").mkdir();
					final File f = new File("./data/stats/npcs/spawns/dumps/spawndump.txt");
					if (!f.exists())
					{
						f.createNewFile();
					}
					final FileWriter writer = new FileWriter(f, true);
					writer.write("<spawn count=\"" + mobCount + "\" respawn=\"" + respawnTime + "\" respawn_random=\"0\" period_of_day=\"none\">\n\t" + "<point x=\"" + activeChar.getX() + "\" y=\"" + activeChar.getY() + "\" z=\"" + activeChar.getZ() + "\" h=\"" + activeChar.getHeading() + "\" />\n\t" + "<npc id=\"" + Integer.parseInt(id) + "\" /><!--" + NpcsParser.getInstance().getTemplate(Integer.parseInt(id)).getName(activeChar.getLang()) + "-->\n" + "</spawn>\n");
					writer.close();
				}
				catch (final Exception e)
				{}
			}
			catch (final Exception e)
			{
				adminhtm.setFile(activeChar, activeChar.getLang(), "data/html/admin/spawns.htm");
				activeChar.sendPacket(adminhtm);
			}
		}
		else if (command.equals("admin_locspawn"))
		{
			try
			{
				try
				{
					final File f = new File("./data/spawnlocs.txt");
					if (!f.exists())
					{
						f.createNewFile();
					}
					final FileWriter writer = new FileWriter(f, true);
					writer.write("<point x=\"" + activeChar.getX() + "\" y=\"" + activeChar.getY() + "\" z=\"" + activeChar.getZ() + "\" h=\"" + activeChar.getHeading() + "\" />\n");
					writer.close();
				}
				catch (final Exception e)
				{}
			}
			catch (final Exception e)
			{
				adminhtm.setFile(activeChar, activeChar.getLang(), "data/html/admin/spawns.htm");
				activeChar.sendPacket(adminhtm);
			}
		}
		else if (command.equals("admin_draw_zone"))
		{
			if (activeChar.isInDrawZone())
			{
				final var list = activeChar.getDrawCoods();
				if (activeChar.getDrawCoods().size() > 2)
				{
					try
					{
						final File f = new File("./data/spawnZones.txt");
						if (!f.exists())
						{
							f.createNewFile();
						}
						
						final FileWriter writer = new FileWriter(f, true);
						writer.write("\t\t<territory>\n\t");
						final var zMin = getMinZ(list);
						final var zMax = getMaxZ(list);
						for (final var loc : list)
						{
							writer.write("\t\t<add x=\"" + loc.getX() + "\" y=\"" + loc.getY() + "\" zmin=\"" + (zMin - 50) + "\" zmax=\"" + (zMax + 50) + "\" />\n\t");
						}
						writer.write("\t</territory>\n");
						writer.close();
						activeChar.sendMessage("Add new spawn zone to ./data/spawnZones.txt");
					}
					catch (final Exception e)
					{
					}
				}
				activeChar.setIsDrawZone(false);
				activeChar.getDrawCoods().clear();
			}
			else
			{
				activeChar.getDrawCoods().clear();
				activeChar.setIsDrawZone(true);
				activeChar.addDrawCoords(activeChar.getLocation());
			}
		}
		else if (command.equals("admin_draw_banned_zone"))
		{
			if (activeChar.isInDrawZone())
			{
				final var list = activeChar.getDrawCoods();
				if (activeChar.getDrawCoods().size() > 2)
				{
					try
					{
						final File f = new File("./data/spawnZones.txt");
						if (!f.exists())
						{
							f.createNewFile();
						}
						
						final FileWriter writer = new FileWriter(f, true);
						writer.write("\t\t<territory>\n\t");
						final var zMin = getMinZ(list);
						final var zMax = getMaxZ(list);
						writer.write("\t\t<banned_territory>\n\t");
						for (final var loc : list)
						{
							writer.write("\t\t\t<add x=\"" + loc.getX() + "\" y=\"" + loc.getY() + "\" zmin=\"" + (zMin - 50) + "\" zmax=\"" + (zMax + 50) + "\" />\n\t");
						}
						writer.write("\t\t</banned_territory>\n\t");
						writer.write("\t</territory>\n");
						writer.close();
						activeChar.sendMessage("Add new banned zone to ./data/spawnZones.txt");
					}
					catch (final Exception e)
					{
					}
				}
				activeChar.setIsDrawZone(false);
				activeChar.getDrawCoods().clear();
			}
			else
			{
				activeChar.getDrawCoods().clear();
				activeChar.setIsDrawZone(true);
				activeChar.addDrawCoords(activeChar.getLocation());
			}
		}
		return true;
	}
	
	private int getMaxZ(List<Location> list)
	{
		int maxValue = list.get(0).getZ();
		for (int i = 1; i < (list.size() - 1); i++)
		{
			if (list.get(i).getZ() > maxValue)
			{
				maxValue = list.get(i).getZ();
			}
		}
		return maxValue;
	}
	
	private int getMinZ(List<Location> list)
	{
		int minValue = list.get(0).getZ();
		for (int i = 1; i < (list.size() - 1); i++)
		{
			if (list.get(i).getZ() < minValue)
			{
				minValue = list.get(i).getZ();
			}
		}
		return minValue;
	}

	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}

	private void printSpawn(Npc target, int type)
	{
		final int i = target.getId();
		final int x = target.getSpawn().getX();
		final int y = target.getSpawn().getY();
		final int z = target.getSpawn().getZ();
		final int h = target.getSpawn().getHeading();
		switch (type)
		{
			default :
			case 0 :
				_log.info("('',1," + i + "," + x + "," + y + "," + z + ",0,0," + h + ",60,0,0),");
				break;
			case 1 :
				_log.info("<spawn npcId=\"" + i + "\" x=\"" + x + "\" y=\"" + y + "\" z=\"" + z + "\" heading=\"" + h + "\" respawn=\"0\" />");
				break;
			case 2 :
				_log.info("{ " + i + ", " + x + ", " + y + ", " + z + ", " + h + " },");
				break;
		}
	}

	private void spawnMonster(Player activeChar, String monsterId, int respawnTime, int mobCount, boolean permanent, boolean saveToDb)
	{
		GameObject target = activeChar.getTarget();
		if (target == null)
		{
			target = activeChar;
		}

		NpcTemplate template1;
		if (monsterId.matches("[0-9]*"))
		{
			final int monsterTemplate = Integer.parseInt(monsterId);
			template1 = NpcsParser.getInstance().getTemplate(monsterTemplate);
		}
		else
		{
			monsterId = monsterId.replace('_', ' ');
			template1 = NpcsParser.getInstance().getTemplateByName(monsterId);
		}

		try
		{
			final Spawner spawn = new Spawner(template1);
			spawn.setX(target.getX());
			spawn.setY(target.getY());
			spawn.setZ(target.getZ());
			spawn.setAmount(mobCount);
			spawn.setHeading(activeChar.getHeading());
			spawn.setRespawnDelay(respawnTime);
			if (!activeChar.getReflection().isDefault())
			{
				permanent = false;
			}
			spawn.setReflection(activeChar.getReflection());

			if (RaidBossSpawnManager.getInstance().isDefined(spawn.getId()))
			{
				activeChar.sendMessage("You cannot spawn another instance of " + template1.getName(activeChar.getLang()) + ".");
			}
			else
			{
				if (saveToDb)
				{
					SpawnHolder.getInstance().addNewSpawn(spawn, permanent);
				}
				spawn.init();
				
				if (!permanent)
				{
					spawn.stopRespawn();
				}
				activeChar.sendMessage("Created " + template1.getName(activeChar.getLang()) + " on " + target.getObjectId());
			}
		}
		catch (final Exception e)
		{
			activeChar.sendPacket(SystemMessageId.TARGET_CANT_FOUND);
		}
	}

	private void showMonsters(Player activeChar, int level, int from)
	{
		final List<NpcTemplate> mobs = NpcsParser.getInstance().getAllMonstersOfLevel(level);
		final int mobsCount = mobs.size();
		final StringBuilder tb = StringUtil.startAppend(500 + (mobsCount * 80), "<html><title>Spawn Monster:</title><body><p> Level : ", Integer.toString(level), "<br>Total Npc's : ", Integer.toString(mobsCount), "<br>");

		int i = from;
		for (int j = 0; (i < mobsCount) && (j < 50); i++, j++)
		{
			StringUtil.append(tb, "<a action=\"bypass -h admin_spawn_monster ", Integer.toString(mobs.get(i).getId()), "\">", mobs.get(i).getName(activeChar.getLang()), "</a><br1>");
		}

		if (i == mobsCount)
		{
			tb.append("<br><center><button value=\"Back\" action=\"bypass -h admin_show_spawns\" width=40 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></center></body></html>");
		}
		else
		{
			StringUtil.append(tb, "<br><center><button value=\"Next\" action=\"bypass -h admin_spawn_index ", Integer.toString(level), " ", Integer.toString(i), "\" width=40 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"><button value=\"Back\" action=\"bypass -h admin_show_spawns\" width=40 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></center></body></html>");
		}

		activeChar.sendPacket(new NpcHtmlMessage(activeChar, 5, tb.toString()));
	}

	private void showNpcs(Player activeChar, String starting, int from)
	{
		final List<NpcTemplate> mobs = NpcsParser.getInstance().getAllNpcStartingWith(starting);
		final int mobsCount = mobs.size();
		final StringBuilder tb = StringUtil.startAppend(500 + (mobsCount * 80), "<html><title>Spawn Monster:</title><body><p> There are ", Integer.toString(mobsCount), " Npcs whose name starts with ", starting, ":<br>");

		int i = from;
		for (int j = 0; (i < mobsCount) && (j < 50); i++, j++)
		{
			StringUtil.append(tb, "<a action=\"bypass -h admin_spawn_monster ", Integer.toString(mobs.get(i).getId()), "\">", mobs.get(i).getName(activeChar.getLang()), "</a><br1>");
		}

		if (i == mobsCount)
		{
			tb.append("<br><center><button value=\"Back\" action=\"bypass -h admin_show_npcs\" width=40 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></center></body></html>");
		}
		else
		{
			StringUtil.append(tb, "<br><center><button value=\"Next\" action=\"bypass -h admin_npc_index ", starting, " ", Integer.toString(i), "\" width=40 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"><button value=\"Back\" action=\"bypass -h admin_show_npcs\" width=40 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></center></body></html>");
		}
		activeChar.sendPacket(new NpcHtmlMessage(activeChar, 5, tb.toString()));
	}

	private static void listFences(Player activeChar)
	{
		final int region = MapRegionManager.getInstance().getMapRegionLocId(activeChar.getX(), activeChar.getY());
		final List<ColosseumFence> fences = ColosseumFenceParser.getInstance().getFences().get(region);
		final StringBuilder sb = new StringBuilder();

		sb.append("<html><body>Total Fences: " + fences.size() + "<br><br>");
		for (final ColosseumFence fence : fences)
		{
			sb.append("<a action=\"bypass -h admin_delete_fence " + fence.getObjectId() + " " + region + " 1\">Fence: " + fence.getObjectId() + " [" + fence.getX() + " " + fence.getY() + " " + fence.getZ() + "]</a><br>");
		}
		sb.append("</body></html>");

		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setHtml(activeChar, sb.toString());
		activeChar.sendPacket(html);
	}
}