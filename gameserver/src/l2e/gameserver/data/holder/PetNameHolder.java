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
package l2e.gameserver.data.holder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import l2e.commons.dbutils.DbUtils;
import l2e.commons.log.LoggerObject;
import l2e.gameserver.Config;
import l2e.gameserver.data.parser.PetsParser;
import l2e.gameserver.database.DatabaseFactory;

public class PetNameHolder extends LoggerObject
{
	protected PetNameHolder()
	{
	}

	public static PetNameHolder getInstance()
	{
		return SingletonHolder._instance;
	}

	public boolean doesPetNameExist(String name, int petNpcId)
	{
		var result = true;
		
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT name FROM pets p, items i WHERE p.item_obj_id = i.object_id AND name=? AND i.item_id IN (?)");
			statement.setString(1, name);
			final var cond = new StringBuilder();
			for (final var it : PetsParser.getPetItemsByNpc(petNpcId))
			{
				if (!cond.toString().isEmpty())
				{
					cond.append(", ");
				}
				cond.append(it);
			}
			statement.setString(2, cond.toString());
			rset = statement.executeQuery();
			result = rset.next();
		}
		catch (final SQLException e)
		{
			warn("Could not check existing petname:" + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
		return result;
	}

	public boolean isValidPetName(String name)
	{
		boolean result = true;

		if (!isAlphaNumeric(name))
		{
			return result;
		}

		Pattern pattern;
		try
		{
			pattern = Pattern.compile(Config.PET_NAME_TEMPLATE);
		}
		catch (final PatternSyntaxException e)
		{
			warn("Pet name pattern of config is wrong!");
			pattern = Pattern.compile(".*");
		}
		final var regexp = pattern.matcher(name);
		if (!regexp.matches())
		{
			result = false;
		}
		return result;
	}

	private boolean isAlphaNumeric(String text)
	{
		var result = true;
		final char[] chars = text.toCharArray();
		for (int i = 0; i < chars.length; i++)
		{
			if (!Character.isLetterOrDigit(chars[i]))
			{
				result = false;
				break;
			}
		}
		return result;
	}

	private static class SingletonHolder
	{
		protected static final PetNameHolder _instance = new PetNameHolder();
	}
}