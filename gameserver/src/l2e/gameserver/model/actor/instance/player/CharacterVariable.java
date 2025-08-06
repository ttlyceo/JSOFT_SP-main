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
package l2e.gameserver.model.actor.instance.player;



public class CharacterVariable
{
	private final String _name;
	private final String _value;
	private final long _expireTime;

	public CharacterVariable(String name, String value, long expireTime)
	{
		_name = name;
		_value = value;
		_expireTime = expireTime;
	}

	public String getName()
	{
		return _name;
	}

	public String getValue()
	{
		return _value;
	}

	public long getExpireTime()
	{
		return _expireTime;
	}

	public boolean isExpired()
	{
		return _expireTime > 0 && _expireTime < System.currentTimeMillis();
	}
}