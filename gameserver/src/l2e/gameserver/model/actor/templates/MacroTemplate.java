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
package l2e.gameserver.model.actor.templates;

import l2e.gameserver.model.base.MacroType;

public class MacroTemplate
{
	private final int _entry;
	private final MacroType _type;
	private final int _d1;
	private final int _d2;
	private final String _cmd;
	
	public MacroTemplate(int entry, MacroType type, int d1, int d2, String cmd)
	{
		_entry = entry;
		_type = type;
		_d1 = d1;
		_d2 = d2;
		_cmd = cmd;
	}
	
	public int getEntry()
	{
		return _entry;
	}
	
	public MacroType getType()
	{
		return _type;
	}
	
	public int getD1()
	{
		return _d1;
	}
	
	public int getD2()
	{
		return _d2;
	}
	
	public String getCmd()
	{
		return _cmd;
	}
}