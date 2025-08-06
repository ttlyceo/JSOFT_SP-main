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
package l2e.gameserver.model.actor.instance;

import java.util.StringTokenizer;

import l2e.commons.util.Rnd;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;

public class UCHelperInstance extends NpcInstance
{
	private final Location[][] _locs = new Location[][]
	{
	        {
	                new Location(-84451, -45452, -10728), new Location(-84580, -45587, -10728)
			},
			{
			        new Location(-86154, -50429, -10728), new Location(-86118, -50624, -10728)
			},
			{
			        new Location(-82009, -53652, -10728), new Location(-81802, -53665, -10728)
			},
			{
			        new Location(-77603, -50673, -10728), new Location(-77586, -50503, -10728)
			},
			{
			        new Location(-79186, -45644, -10728), new Location(-79309, -45561, -10728)
			}
	};
	
	public static final Location[] _points =
	{
	        new Location(-60695, -56896, -2032), new Location(-59716, -55920, -2032), new Location(-58752, -56896, -2032), new Location(-59716, -57864, -2032)
	};
	
	public UCHelperInstance(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public String getHtmlPath(int npcId, int val)
	{
		String pom = "";
		
		if (val == 0)
		{
			pom = "" + npcId;
		}
		else
		{
			pom = npcId + "-" + val;
		}
		
		return "data/html/underground_coliseum/" + pom + ".htm";
	}
	
	@Override
	public void onBypassFeedback(Player player, String command)
	{
		final StringTokenizer token = new StringTokenizer(command, " ");
		final String actualCommand = token.nextToken();
		
		if (actualCommand.equals("teleOut"))
		{
			player.teleToLocation(_points[Rnd.get(_points.length)], true, ReflectionManager.DEFAULT);
		}
		else if (actualCommand.startsWith("coliseum"))
		{
			final int a = Integer.parseInt(token.nextToken());
			final Location[] locs = _locs[a];
			
			player.teleToLocation(locs[Rnd.get(locs.length)], true, ReflectionManager.DEFAULT);
		}
		else
		{
			super.onBypassFeedback(player, command);
		}
	}
}