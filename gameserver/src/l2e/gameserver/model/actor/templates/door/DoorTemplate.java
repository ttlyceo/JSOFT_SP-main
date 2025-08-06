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
package l2e.gameserver.model.actor.templates.door;

import l2e.commons.geometry.Polygon;
import l2e.gameserver.model.actor.templates.character.CharTemplate;
import l2e.gameserver.model.interfaces.IIdentifiable;
import l2e.gameserver.model.stats.StatsSet;

public class DoorTemplate extends CharTemplate implements IIdentifiable
{
	public final int doorId;
	public final int nodeX[];
	public final int nodeY[];
	public final int nodeZ;
	public final int height;
	public final int posX;
	public final int posY;
	public final int posZ;
	public final int emmiter;
	public final int childDoorId;
	public final String name;
	public final String groupName;
	public final boolean showHp;
	public final boolean isWall;
	public final byte masterDoorClose;
	public final byte masterDoorOpen;
	private final Polygon _polygon;
	
	public DoorTemplate(StatsSet set)
	{
		super(set);
		
		doorId = set.getInteger("id");
		name = set.getString("name");
		
		final String[] pos = set.getString("pos").split(";");
		posX = Integer.parseInt(pos[0]);
		posY = Integer.parseInt(pos[1]);
		posZ = Integer.parseInt(pos[2]);
		height = set.getInteger("height");
		
		nodeZ = set.getInteger("nodeZ");
		nodeX = new int[4];
		nodeY = new int[4];
		for (int i = 0; i < 4; i++)
		{
			final String split[] = set.getString("node" + (i + 1)).split(",");
			nodeX[i] = Integer.parseInt(split[0]);
			nodeY[i] = Integer.parseInt(split[1]);
		}
		
		emmiter = set.getInteger("emitter_id", 0);
		showHp = set.getBool("hp_showable", true);
		isWall = set.getBool("is_wall", false);
		groupName = set.getString("group", null);
		
		childDoorId = set.getInteger("child_id_event", -1);
		String masterevent = set.getString("master_close_event", "act_nothing");
		if (masterevent.equals("act_open"))
		{
			masterDoorClose = 1;
		}
		else if (masterevent.equals("act_close"))
		{
			masterDoorClose = -1;
		}
		else
		{
			masterDoorClose = 0;
		}
		
		masterevent = set.getString("master_open_event", "act_nothing");
		if (masterevent.equals("act_open"))
		{
			masterDoorOpen = 1;
		}
		else if (masterevent.equals("act_close"))
		{
			masterDoorOpen = -1;
		}
		else
		{
			masterDoorOpen = 0;
		}
		
		final String[] pos1 = set.getString("node1").split(",");
		final String[] pos2 = set.getString("node2").split(",");
		final String[] pos3 = set.getString("node3").split(",");
		final String[] pos4 = set.getString("node4").split(",");
		
		final Polygon shape = new Polygon();
		if (pos1 != null)
		{
			shape.add(Integer.parseInt(pos1[0]), Integer.parseInt(pos1[1]));
			shape.add(Integer.parseInt(pos2[0]), Integer.parseInt(pos2[1]));
			shape.add(Integer.parseInt(pos3[0]), Integer.parseInt(pos3[1]));
			shape.add(Integer.parseInt(pos4[0]), Integer.parseInt(pos4[1]));
		}
		shape.setZmin(set.getInteger("nodeZ"));
		shape.setZmax(set.getInteger("nodeZ") + height);
		_polygon = shape;
	}
	
	@Override
	public int getId()
	{
		return doorId;
	}
	
	public Polygon getPolygon()
	{
		return _polygon;
	}
}