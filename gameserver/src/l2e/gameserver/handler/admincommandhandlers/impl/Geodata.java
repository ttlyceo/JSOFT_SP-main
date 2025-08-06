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

import java.util.StringTokenizer;

import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.geodata.GeoEngine;
import l2e.gameserver.geodata.util.GeoUtils;
import l2e.gameserver.handler.admincommandhandlers.IAdminCommandHandler;
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.network.SystemMessageId;

public class Geodata implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
	        "admin_geo_pos", "admin_geo_spawn_pos", "admin_geo_can_move", "admin_geo_can_see", "admin_geogrid", "admin_geomap"
	};

	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		if (!Config.GEODATA)
		{
			activeChar.sendMessage("Geo Engine is Turned Off!");
			return true;
		}

		final StringTokenizer st = new StringTokenizer(command, " ");
		final String actualCommand = st.nextToken();
		switch (actualCommand.toLowerCase())
		{
			case "admin_geo_pos" :
			{
				final int worldX = activeChar.getX();
				final int worldY = activeChar.getY();
				final int worldZ = activeChar.getZ();
				final int geoX = GeoEngine.getInstance().getGeoX(worldX);
				final int geoY = GeoEngine.getInstance().getGeoY(worldY);
				
				if (GeoEngine.getInstance().hasGeoPos(geoX, geoY))
				{
					activeChar.sendMessage("WorldX: " + worldX + ", WorldY: " + worldY + ", WorldZ: " + worldZ + ", GeoX: " + geoX + ", GeoY: " + geoY + ", GeoZ: " + GeoEngine.getInstance().getHeight(worldX, worldY, worldZ));
				}
				else
				{
					activeChar.sendMessage("There is no geodata at this position.");
				}
				break;
			}
			case "admin_geo_spawn_pos" :
			{
				final int worldX = activeChar.getX();
				final int worldY = activeChar.getY();
				final int worldZ = activeChar.getZ();
				final int geoX = GeoEngine.getInstance().getGeoX(worldX);
				final int geoY = GeoEngine.getInstance().getGeoY(worldY);
				
				if (GeoEngine.getInstance().hasGeoPos(geoX, geoY))
				{
					activeChar.sendMessage("WorldX: " + worldX + ", WorldY: " + worldY + ", WorldZ: " + worldZ + ", GeoX: " + geoX + ", GeoY: " + geoY + ", GeoZ: " + GeoEngine.getInstance().getHeight(worldX, worldY, worldZ));
				}
				else
				{
					activeChar.sendMessage("There is no geodata at this position.");
				}
				break;
			}
			case "admin_geo_can_move" :
			{
				final var target = activeChar.getTarget();
				if (target != null)
				{
					if (GeoEngine.getInstance().canSeeTarget(activeChar, target))
					{
						activeChar.sendMessage("Can move beeline.");
					}
					else
					{
						activeChar.sendMessage("Can not move beeline!");
					}
				}
				else
				{
					activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
				}
				break;
			}
			case "admin_geo_can_see" :
			{
				final var target = activeChar.getTarget();
				if (target != null)
				{
					if (GeoEngine.getInstance().canSeeTarget(activeChar, target))
					{
						activeChar.sendMessage("Can see target.");
					}
					else
					{
						activeChar.sendPacket(SystemMessageId.CANT_SEE_TARGET);
					}
				}
				else
				{
					activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
				}
				break;
			}
			case "admin_geogrid" :
			{
				showGeoGrid(activeChar, activeChar, st);
				break;
			}
			case "admin_geomap" :
			{
				final int x = ((activeChar.getX() - World.MAP_MIN_X) >> 15) + World.TILE_X_MIN;
				final int y = ((activeChar.getY() - World.MAP_MIN_Y) >> 15) + World.TILE_Y_MIN;
				activeChar.sendMessage("GeoMap: " + x + "_" + y + " (" + ((x - World.TILE_ZERO_COORD_X) * World.TILE_SIZE) + "," + ((y - World.TILE_ZERO_COORD_Y) * World.TILE_SIZE) + " to " + ((((x - World.TILE_ZERO_COORD_X) * World.TILE_SIZE) + World.TILE_SIZE) - 1) + "," + ((((y - World.TILE_ZERO_COORD_Y) * World.TILE_SIZE) + World.TILE_SIZE) - 1) + ")");
				break;
			}
		}
		return true;
	}
	
	private void showGeoGrid(Player caller, Player target, StringTokenizer st)
	{
		if (st.hasMoreTokens())
		{
			final String nextCommand = st.nextToken();
			if (Util.isDigit(nextCommand))
			{
				final int geoRadius = Integer.parseInt(nextCommand);
				
				if (geoRadius > 25)
				{
					caller.sendMessage("Wrong usage! radius can not be greater than 25!");
				}
				else
				{
					GeoUtils.debugGrid(target, geoRadius);
				}
			}
			else
			{
				caller.sendMessage("Wrong usage! //geogrid <0-25> / //showgeogrid <0-25>");
			}
		}
		else
		{
			caller.sendMessage("Wrong usage! //geogrid <0-25> / //showgeogrid <0-25>");
		}
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}