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
package l2e.gameserver.network.serverpackets;

import java.util.ArrayList;
import java.util.List;

import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.Location;

public final class ExShowTrace extends GameServerPacket
{
	private final List<Trace> _traces = new ArrayList<>();

	static final class Trace
	{
		public final int _x;
		public final int _y;
		public final int _z;
		public final int _time;

		public Trace(int x, int y, int z, int time)
		{
			_x = x;
			_y = y;
			_z = z;
			_time = time;
		}
	}

	public void addTrace(int x, int y, int z, int time)
	{
		_traces.add(new Trace(x, y, z, time));
	}
	
	public void addLine(Location from, Location to, int step, int time)
	{
		addLine(from.getX(), from.getY(), from.getZ(), to.getX(), to.getY(), to.getZ(), step, time);
	}
	
	public void addLine(int from_x, int from_y, int from_z, int to_x, int to_y, int to_z, int step, int time)
	{
		final int x_diff = to_x - from_x;
		final int y_diff = to_y - from_y;
		final int z_diff = to_z - from_z;
		final double xy_dist = Math.sqrt(x_diff * x_diff + y_diff * y_diff);
		final double full_dist = Math.sqrt(xy_dist * xy_dist + z_diff * z_diff);
		final int steps = (int) (full_dist / step);
		
		addTrace(from_x, from_y, from_z, time);
		if (steps > 1)
		{
			final int step_x = x_diff / steps;
			final int step_y = y_diff / steps;
			final int step_z = z_diff / steps;
			
			for (int i = 1; i < steps; i++)
			{
				addTrace(from_x + step_x * i, from_y + step_y * i, from_z + step_z * i, time);
			}
		}
		addTrace(to_x, to_y, to_z, time);
	}
	
	public void addTrace(GameObject obj, int time)
	{
		this.addTrace(obj.getX(), obj.getY(), obj.getZ(), time);
	}
	
	@Override
	protected void writeImpl()
	{
		writeH(_traces.size());
		for (final Trace t : _traces)
		{
			writeD(t._x);
			writeD(t._y);
			writeD(t._z);
			writeH(t._time);
		}
	}
}