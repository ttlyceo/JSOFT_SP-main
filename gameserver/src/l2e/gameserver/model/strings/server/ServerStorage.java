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
package l2e.gameserver.model.strings.server;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2e.commons.util.Files;
import l2e.gameserver.Config;

public class ServerStorage
{
	private static final Logger _log = LoggerFactory.getLogger(ServerStorage.class);

	private static final Pattern LINE_PATTERN = Pattern.compile("^(((?!=).)+)=(.*?)$");
	
	private final HashMap<String, HashMap<String, String>> _sysmessages = new HashMap<>();
	
	protected ServerStorage()
	{
		_sysmessages.clear();
		reload();
	}

	public String getString(String lang, String name)
	{
		if (lang == null)
		{
			lang = "en";
		}
		if (_sysmessages.get(lang) == null)
		{
			return "";
		}
		if (_sysmessages.get(lang).get(name) == null)
		{
			return "";
		}
		return _sysmessages.get(lang).get(name);
	}

	private void reload()
	{
		final File dir = new File(Config.DATAPACK_ROOT, "data/localization/");
		for (final File file : dir.listFiles())
		{
			if (file.isDirectory() && !file.isHidden())
			{
				final String lang = file.getName();
				final HashMap<String, String> map = new HashMap<>();
				readFromDisk(map, lang, false);
				readFromDisk(map, lang, true);
				_sysmessages.put(lang, map);
				_log.info("ServerStorage: Loading " + map.size() + " server messages for [" + lang + "] lang.");
			}
		}
	}

	private void readFromDisk(HashMap<String, String> map, String lang, boolean isCustom)
	{
		Scanner scanner = null;
		try
		{
			final File file = new File(Config.DATAPACK_ROOT, "data/localization/" + lang + "/messages" + (isCustom ? "_custom" : "") + ".txt");
			if (!file.exists())
			{
				return;
			}
			final String content = Files.readFile(file);
			scanner = new Scanner(content);
			int i = 0;
			String line;
			while (scanner.hasNextLine())
			{
				i++;
				line = scanner.nextLine();
				if (line.startsWith("#"))
				{
					continue;
				}
				
				if (line.startsWith("\uFEFF"))
				{
					line = line.substring(1);
				}
				
				final Matcher m = LINE_PATTERN.matcher(line);
				if (m.find())
				{
					final String name = m.group(1);
					String value = m.group(3);
					if (m.groupCount() > 3)
					{
						for (int g = 4; g <= m.groupCount(); g++)
						{
							value += m.group(g);
						}
					}
					
					map.put(name, value);
				}
				else
				{
					_log.warn("Error on line #: " + i + "; file: " + file.getName());
				}
			}
		}
		catch (final IOException e1)
		{
			_log.warn("Error loading \"" + lang + "\" language pack: ", e1);
		}
		finally
		{
			try
			{
				scanner.close();
			}
			catch (final Exception e)
			{}
		}
	}

	public static ServerStorage getInstance()
	{
		return SingletonHolder._instance;
	}

	private static class SingletonHolder
	{
		protected static final ServerStorage _instance = new ServerStorage();
	}
}