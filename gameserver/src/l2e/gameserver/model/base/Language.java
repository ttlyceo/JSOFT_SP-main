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
package l2e.gameserver.model.base;

public enum Language
{
	KOREA("ko"), ENGLISH("en"), JAPAN("ja"), TAIWAN("tw"), CHINA("zh"), THAILAND("th"), PHILIPPINE("tl"), INDONESIA("id"), RUSSIAN("ru");

    public static final Language[] VALUES = Language.values();
	private final int _id;
    private final String _shortName;

	Language(String shortName)
	{
		_id = ordinal();
        _shortName = shortName;
    }

	public String getName()
	{
        return _shortName;
	}
	
	public int getId()
	{
		return _id;
	}
	
	public static String getById(int id)
	{
		for (final var lang : Language.VALUES)
		{
			if (lang.getId() == id)
			{
				return lang.getName();
			}
		}
		return null;
	}
	
	public static String getByName(String name)
	{
		if ((name != null) && !name.isEmpty())
		{
			for (final var lang : Language.VALUES)
			{
				if (lang.getName().equalsIgnoreCase(name))
				{
					return lang.getName();
				}
			}
		}
		return null;
	}
}