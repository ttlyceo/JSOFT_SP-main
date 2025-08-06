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
package l2e.gameserver.geodata.loader;

public interface IBlock
{
	public static final int TYPE_FLAT = 0;
	public static final int TYPE_COMPLEX = 1;
	public static final int TYPE_COMPLEX_L2OFF = 0x40;
	public static final int TYPE_MULTILAYER = 2;
	
	public static final int BLOCK_CELLS_X = 8;
	public static final int BLOCK_CELLS_Y = 8;
	public static final int BLOCK_CELLS = BLOCK_CELLS_X * BLOCK_CELLS_Y;
	
	boolean checkNearestNswe(int geoX, int geoY, int worldZ, int nswe);
	
	int getNearestNswe(int geoX, int geoY, int worldZ);
	
	int getNearestZ(int geoX, int geoY, int worldZ);
	
	int getNextLowerZ(int geoX, int geoY, int worldZ);
	
	int getNextHigherZ(int geoX, int geoY, int worldZ);
}
