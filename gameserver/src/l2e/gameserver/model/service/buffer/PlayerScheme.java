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
package l2e.gameserver.model.service.buffer;

import java.util.ArrayList;
import java.util.List;

public class PlayerScheme
{
	private final int _schemeId;
	private String _schemeName;
	private int _iconId;
	private final List<SchemeBuff> _schemeBuffs;
	
	public PlayerScheme(int schemeId, String schemeName, int iconId)
	{
		_schemeId = schemeId;
		_schemeName = schemeName;
		_iconId = iconId;
		_schemeBuffs = new ArrayList<>();
	}
	
	public int getSchemeId()
	{
		return _schemeId;
	}
	
	public void setName(String name)
	{
		_schemeName = name;
	}
	
	public String getName()
	{
		return _schemeName;
	}
	
	public int getIconId()
	{
		return _iconId;
	}
	
	public void setIcon(int iconId)
	{
		_iconId = iconId;
	}
	
	public List<SchemeBuff> getBuffs()
	{
		return _schemeBuffs;
	}
	
	public void addBuff(SchemeBuff buff)
	{
		_schemeBuffs.add(buff);
	}
}