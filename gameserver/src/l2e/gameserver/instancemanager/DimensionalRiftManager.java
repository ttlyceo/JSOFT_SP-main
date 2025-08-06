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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.NamedNodeMap;

import l2e.commons.log.LoggerObject;
import l2e.commons.util.Rnd;
import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.data.parser.NpcsParser;
import l2e.gameserver.data.parser.SpawnParser;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.DimensionalRiftRoomTemplate;
import l2e.gameserver.model.actor.templates.DimensionalRiftTemplate;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.entity.DimensionalRift;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.spawn.Spawner;
import l2e.gameserver.model.stats.StatsSet;
import l2e.gameserver.model.strings.server.ServerMessage;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;

public final class DimensionalRiftManager extends LoggerObject
{
	private final Map<Byte, List<DimensionalRiftRoomTemplate>> _rooms = new HashMap<>(7);
	private final Map<Byte, DimensionalRiftTemplate> _templates = new HashMap<>();
	private final List<DimensionalRift> _rifts = new ArrayList<>();

	public DimensionalRiftManager()
	{
		loadSpawns();
	}

	public DimensionalRiftRoomTemplate getRoom(byte type, byte room)
	{
		return _rooms.get(type) == null ? null : _rooms.get(type).get(room);
	}
	
	public void loadSpawns()
	{
		_templates.clear();
		int countGood = 0, countBad = 0;
		try
		{
			final var factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);
			
			final File file = new File(Config.DATAPACK_ROOT, "data/stats/npcs/spawnZones/dimensionalRift.xml");
			if (!file.exists())
			{
				warn("Couldn't find data/" + file.getName());
				return;
			}
			
			final var doc = factory.newDocumentBuilder().parse(file);
			NamedNodeMap attrs;
			byte type, roomId;
			int mobId, x, y, z, delay, count;
			Spawner spawnDat;
			NpcTemplate template;
			
			for (var rift = doc.getFirstChild(); rift != null; rift = rift.getNextSibling())
			{
				if ("rift".equalsIgnoreCase(rift.getNodeName()))
				{
					for (var area = rift.getFirstChild(); area != null; area = area.getNextSibling())
					{
						if ("area".equalsIgnoreCase(area.getNodeName()))
						{
							attrs = area.getAttributes();
							type = Byte.parseByte(attrs.getNamedItem("type").getNodeValue());
							final int minPlayers = attrs.getNamedItem("minPlayers") != null ? Integer.parseInt(attrs.getNamedItem("minPlayers").getNodeValue()) : 9;
							final int hwidsLimit = attrs.getNamedItem("hwidsLimit") != null ? Integer.parseInt(attrs.getNamedItem("hwidsLimit").getNodeValue()) : 0;
							final int ipsLimit = attrs.getNamedItem("ipsLimit") != null ? Integer.parseInt(attrs.getNamedItem("minPlayers").getNodeValue()) : 0;
							final long fragmentCount = attrs.getNamedItem("fragmentCount") != null ? Long.parseLong(attrs.getNamedItem("fragmentCount").getNodeValue()) : 0;
							final double bossRoomChance = attrs.getNamedItem("bossRoomChance") != null ? Double.parseDouble(attrs.getNamedItem("bossRoomChance").getNodeValue()) : 0;
							final int maxJumps = attrs.getNamedItem("maxJumps") != null ? Integer.parseInt(attrs.getNamedItem("maxJumps").getNodeValue()) : 0;
							final boolean customTeleFunction = attrs.getNamedItem("customTeleFunction") != null ? Boolean.parseBoolean(attrs.getNamedItem("customTeleFunction").getNodeValue()) : false;
							final StatsSet params = new StatsSet();
							for (var room = area.getFirstChild(); room != null; room = room.getNextSibling())
							{
								if ("add_parameters".equalsIgnoreCase(room.getNodeName()))
								{
									for (var sp = room.getFirstChild(); sp != null; sp = sp.getNextSibling())
									{
										if ("set".equalsIgnoreCase(sp.getNodeName()))
										{
											params.set(sp.getAttributes().getNamedItem("name").getNodeValue(), sp.getAttributes().getNamedItem("value").getNodeValue());
										}
									}
								}
								if ("room".equalsIgnoreCase(room.getNodeName()))
								{
									attrs = room.getAttributes();
									roomId = Byte.parseByte(attrs.getNamedItem("id").getNodeValue());
									final boolean isBossRoom = Boolean.parseBoolean(attrs.getNamedItem("isBossRoom").getNodeValue());
									DimensionalRiftRoomTemplate dRoom = null;
									Location teleportLoc = null;
									for (var spawn = room.getFirstChild(); spawn != null; spawn = spawn.getNextSibling())
									{
										if ("teleport".equalsIgnoreCase(spawn.getNodeName()))
										{
											attrs = spawn.getAttributes();
											final int x1 = Integer.parseInt(attrs.getNamedItem("x").getNodeValue());
											final int y1 = Integer.parseInt(attrs.getNamedItem("y").getNodeValue());
											final int z1 = Integer.parseInt(attrs.getNamedItem("z").getNodeValue());
											teleportLoc = new Location(x1, y1, z1);
										}
										else if ("polygon".equalsIgnoreCase(spawn.getNodeName()))
										{
											attrs = spawn.getAttributes();
											final int minX = Integer.parseInt(attrs.getNamedItem("minX").getNodeValue());
											final int maxX = Integer.parseInt(attrs.getNamedItem("maxX").getNodeValue());
											final int minY = Integer.parseInt(attrs.getNamedItem("minY").getNodeValue());
											final int maxY = Integer.parseInt(attrs.getNamedItem("maxY").getNodeValue());
											final int minZ = Integer.parseInt(attrs.getNamedItem("minZ").getNodeValue());
											final int maxZ = Integer.parseInt(attrs.getNamedItem("maxZ").getNodeValue());
											
											if (teleportLoc == null)
											{
												warn("teleportLoc for room " + roomId + " not found!");
											}
											dRoom = new DimensionalRiftRoomTemplate(minX, maxX, minY, maxY, minZ, maxZ, roomId, type, teleportLoc, isBossRoom);
											if (!_rooms.containsKey(type))
											{
												_rooms.put(type, new LinkedList<>());
											}
											_rooms.get(type).add(dRoom);
										}
										else if ("spawn".equalsIgnoreCase(spawn.getNodeName()))
										{
											attrs = spawn.getAttributes();
											mobId = Integer.parseInt(attrs.getNamedItem("mobId").getNodeValue());
											delay = Integer.parseInt(attrs.getNamedItem("delay").getNodeValue());
											count = Integer.parseInt(attrs.getNamedItem("count").getNodeValue());
											
											template = NpcsParser.getInstance().getTemplate(mobId);
											if (template == null)
											{
												warn("Template " + mobId + " not found!");
											}
											if (!_rooms.containsKey(type))
											{
												warn("Type " + type + " not found!");
											}
											
											for (int i = 0; i < count; i++)
											{
												x = dRoom.getRandomX();
												y = dRoom.getRandomY();
												z = dRoom.getTeleportLocation().getZ();
												if (template != null)
												{
													spawnDat = new Spawner(template);
													spawnDat.setAmount(1);
													spawnDat.setX(x);
													spawnDat.setY(y);
													spawnDat.setZ(z);
													spawnDat.setHeading(-1);
													spawnDat.setRespawnDelay(delay);
													SpawnParser.getInstance().addNewSpawn(spawnDat);
													dRoom.getSpawns().add(spawnDat);
													countGood++;
												}
												else
												{
													countBad++;
												}
											}
										}
									}
								}
							}
							_templates.put(type, new DimensionalRiftTemplate(type, minPlayers, fragmentCount, bossRoomChance, maxJumps, params, customTeleFunction, hwidsLimit, ipsLimit));
						}
					}
				}
			}
		}
		catch (final Exception e)
		{
			warn("Error on loading dimensional rift spawns: " + e.getMessage(), e);
		}
		if (Config.DEBUG)
		{
			info("Loaded " + countGood + " dimensional rift spawns, " + countBad + " errors.");
		}
	}
	
	public void reload()
	{
		_rifts.stream().filter(r -> (r != null)).forEach(r -> r.manualExit());
		_rifts.clear();
		
		for (final byte b : _rooms.keySet())
		{
			_rooms.get(b).stream().filter(r -> (r != null)).forEach(r -> r.stop());
		}
		_rooms.clear();
		loadSpawns();
	}
	
	public boolean checkIfInPeaceZone(int x, int y, int z)
	{
		return _rooms.get((byte) 0).get((byte) 0).isInside(x, y, z);
	}
	
	public void teleportToWaitingRoom(Player player)
	{
		final Location loc = getRoom((byte) 0, (byte) 0).getTeleportLocation();
		if (loc != null)
		{
			player.teleToLocation(loc, true, player.getReflection());
		}
	}
	
	public synchronized void start(Player player, byte type, Npc npc)
	{
		final var template = _templates.get(type);
		if (template == null)
		{
			return;
		}
		
		boolean canPass = true;
		final var party = player.getParty();
		if (!player.isInParty() || party == null)
		{
			showHtmlFile(player, "data/html/seven_signs/rift/NoParty.htm", npc);
			return;
		}
		
		if (party.getLeaderObjectId() != player.getObjectId())
		{
			showHtmlFile(player, "data/html/seven_signs/rift/NotPartyLeader.htm", npc);
			return;
		}
		
		if (party.isInDimensionalRift())
		{
			handleCheat(player, npc);
			return;
		}
		
		if (party.getMemberCount() < template.getMinPlayers())
		{
			final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
			html.setFile(player, player.getLang(), "data/html/seven_signs/rift/SmallParty.htm");
			html.replace("%npc_name%", npc.getName(player.getLang()));
			html.replace("%count%", Integer.toString(template.getMinPlayers()));
			player.sendPacket(html);
			return;
		}
		
		if (getFreeRooms(type, false, false).size() < 3)
		{
			player.sendMessage("Rift is full. Try later.");
			return;
		}
		
		final var members = party.getMembers();
		for (final var p : members)
		{
			if (!checkIfInPeaceZone(p.getX(), p.getY(), p.getZ()))
			{
				canPass = false;
				break;
			}
		}
		
		if (template.getHwidsLimit() > 0)
		{
			if (!Util.isAvalibleWindows(members, template.getHwidsLimit(), true))
			{
				final var msg = new ServerMessage("Reflection.HWIDS_LIMIT", true);
				msg.add(template.getHwidsLimit());
				party.broadCastMessage(msg);
				return;
			}
		}
		
		if (template.getIpsLimit() > 0)
		{
			if (!Util.isAvalibleWindows(members, template.getIpsLimit(), false))
			{
				final var msg = new ServerMessage("Reflection.IPS_LIMIT", true);
				msg.add(template.getIpsLimit());
				party.broadCastMessage(msg);
				return;
			}
		}
		
		if (!canPass)
		{
			showHtmlFile(player, "data/html/seven_signs/rift/NotInWaitingRoom.htm", npc);
			return;
		}
		
		ItemInstance i;
		final long count = getNeededItems(type);
		for (final var p : members)
		{
			i = p.getInventory().getItemByItemId(7079);
			if (i == null)
			{
				canPass = false;
				break;
			}
			
			if (i.getCount() > 0)
			{
				if (i.getCount() < getNeededItems(type))
				{
					canPass = false;
					break;
				}
			}
			else
			{
				canPass = false;
				break;
			}
		}
		
		if (!canPass)
		{
			final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
			html.setFile(player, player.getLang(), "data/html/seven_signs/rift/NoFragments.htm");
			html.replace("%npc_name%", npc.getName(player.getLang()));
			html.replace("%count%", String.valueOf(count));
			player.sendPacket(html);
			return;
		}
		
		for (final var p : members)
		{
			i = p.getInventory().getItemByItemId(7079);
			if (!p.destroyItem("RiftEntrance", i, count, null, false))
			{
				final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
				html.setFile(player, player.getLang(), "data/html/seven_signs/rift/NoFragments.htm");
				html.replace("%npc_name%", npc.getName(player.getLang()));
				html.replace("%count%", String.valueOf(count));
				player.sendPacket(html);
				return;
			}
		}
		
		final var isBossRoom = template.isCustomTeleFunction() && Rnd.chance(template.getBossRoomChance());
		final var rooms = getFreeRooms(type, false, isBossRoom);
		if ((isBossRoom && rooms.size() > 0) || (!isBossRoom && rooms.size() > 2))
		{
			_rifts.add(new DimensionalRift(player.getParty(), rooms.get(Rnd.get(rooms.size())), template));
		}
	}
	
	public void removeRift(DimensionalRift rift)
	{
		if (_rifts.contains(rift))
		{
			_rifts.remove(rift);
		}
	}
	
	private long getNeededItems(byte type)
	{
		final var template = _templates.get(type);
		return template != null ? template.getFragmentCount() : 0;
	}
	
	public void showHtmlFile(Player player, String file, Npc npc)
	{
		final var html = new NpcHtmlMessage(npc.getObjectId());
		html.setFile(player, player.getLang(), file);
		html.replace("%npc_name%", npc.getName(player.getLang()));
		player.sendPacket(html);
	}
	
	public void handleCheat(Player player, Npc npc)
	{
		showHtmlFile(player, "data/html/seven_signs/rift/Cheater.htm", npc);
		if (!player.isGM())
		{
			Util.handleIllegalPlayerAction(player, "" + player.getName(null) + " tried to cheat in dimensional rift.");
		}
	}
	
	public synchronized List<DimensionalRiftRoomTemplate> getFreeRooms(byte type, boolean allowBoss, boolean isBossRoom)
	{
		final List<DimensionalRiftRoomTemplate> ret = new LinkedList<>();
		final var list = _rooms.get(type);
		if (isBossRoom)
		{
			list.stream().filter(r -> (r != null && !r.isBusy() && r.isBossRoom())).forEach(r -> ret.add(r));
			if (ret.size() > 0)
			{
				return ret;
			}
		}
		list.stream().filter(r -> (r != null && !r.isBusy() && !(r.isBossRoom() && !allowBoss))).forEach(r -> ret.add(r));
		return ret;
	}
	
	public static DimensionalRiftManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final DimensionalRiftManager _instance = new DimensionalRiftManager();
	}
}