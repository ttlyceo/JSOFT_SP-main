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
package l2e.fake;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2e.commons.util.Rnd;
import l2e.gameserver.data.holder.CharNameHolder;

public class FakePlayerNameManager
{
	public static final Logger _log = LoggerFactory.getLogger(FakePlayerNameManager.class);
	
	private List<String> _fakePlayerNames;
	
	public FakePlayerNameManager()
	{
		loadWordlist();
	}
	
	public String getRandomAvailableName()
	{
		String name = getRandomNameFromWordlist();
		
		while (nameAlreadyExists(name))
		{
			name = getRandomNameFromWordlist();
		}
		
		return name;
	}
	
	public boolean doesCharNameExist(String name)
	{
		for (final String fake : _fakePlayerNames)
		{
			if (fake.toLowerCase().equals(name.toLowerCase()))
			{
				return true;
			}
		}
		return false;
	}
	
	private String getRandomNameFromWordlist()
	{
		return _fakePlayerNames.get(Rnd.get(0, _fakePlayerNames.size() - 1));
	}
	
	public List<String> getFakePlayerNames()
	{
		return _fakePlayerNames;
	}
	
	private void loadWordlist()
	{
		try (
		    LineNumberReader lnr = new LineNumberReader(new BufferedReader(new FileReader(new File("./config/mods/fakes/fakenamewordlist.txt"))));)
		{
			String line;
			final ArrayList<String> playersList = new ArrayList<>();
			while ((line = lnr.readLine()) != null)
			{
				if (line.trim().length() == 0 || line.startsWith("#"))
				{
					continue;
				}
				playersList.add(line);
			}
			_fakePlayerNames = playersList;
			_log.info(String.format("Loaded %s fake player names.", _fakePlayerNames.size()));
		}
		catch (final Exception e)
		{
			e.printStackTrace();
		}
	}
	
	private boolean nameAlreadyExists(String name)
	{
		return CharNameHolder.getInstance().getIdByName(name) > 0;
	}
	
	public static FakePlayerNameManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final FakePlayerNameManager _instance = new FakePlayerNameManager();
	}
}
