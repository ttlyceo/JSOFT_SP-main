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
package l2e.gameserver.data.htm;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import l2e.commons.log.LoggerObject;
import l2e.commons.util.Util;
import l2e.commons.util.file.filter.HTMLFilter;
import l2e.gameserver.Config;
import l2e.gameserver.model.actor.Player;

public class HtmCache extends LoggerObject
{
	private static final HTMLFilter htmlFilter = new HTMLFilter();

	private static final Map<String, String> _cache = Config.ALLOW_CACHE ? new ConcurrentHashMap<>() : new HashMap<>();

	private int _loadedFiles;
	private long _bytesBuffLen;

	protected HtmCache()
	{
		reload();
	}

	public void reload()
	{
		reload(Config.DATAPACK_ROOT);
	}

	public void reload(File f)
	{
		_cache.clear();
		if (!Config.ALLOW_CACHE)
		{
			_loadedFiles = 0;
			_bytesBuffLen = 0;
			info("Running lazy cache...");
		}
		else
		{
			info("start...");
			parseDir(f);
			info("" + String.format("%.3f", getMemoryUsage()) + " megabytes on " + getLoadedFiles() + " files loaded...");
		}
	}

	public void reloadPath(File f)
	{
		parseDir(f);
		info("Reloaded specified path.");
	}

	public double getMemoryUsage()
	{
		return ((float) _bytesBuffLen / 1048576);
	}

	public int getLoadedFiles()
	{
		return _loadedFiles;
	}

	private void parseDir(File dir)
	{
		final File[] files = dir.listFiles();

		for (final var file : files)
		{
			if (!file.isDirectory())
			{
				loadFile(file);
			}
			else
			{
				parseDir(file);
			}
		}
	}

	public String loadFile(File file)
	{
		if (!htmlFilter.accept(file))
		{
			return null;
		}

		final var relpath = Util.getRelativePath(Config.DATAPACK_ROOT, file);
		String content = null;
		try (
		    var fis = new FileInputStream(file); BufferedInputStream bis = new BufferedInputStream(fis))
		{
			final int bytes = bis.available();
			final byte[] raw = new byte[bytes];

			bis.read(raw);
			content = new String(raw, "UTF-8");

			final var oldContent = _cache.get(relpath);
			if (oldContent == null)
			{
				_bytesBuffLen += bytes;
				_loadedFiles++;
			}
			else
			{
				_bytesBuffLen = (_bytesBuffLen - oldContent.length()) + bytes;
			}
			
			if (Config.ALLOW_CACHE)
			{
				_cache.put(relpath, content);
			}
		}
		catch (final Exception e)
		{
			warn("Problem with htm file " + e.getMessage(), e);
		}
		return content;
	}

	public String getHtmForce(Player player, String prefix, String path)
	{
		var content = getHtm(player, prefix, path);

		if (content == null)
		{
			content = "<html><body>My text is missing:<br>" + path + "</body></html>";
			warn("Missing HTML page: " + path);
		}
		return content;
	}

	public String getHtm(Player player, String prefix, String path)
	{
		if (prefix == null)
		{
			prefix = "en";
		}
		
		var oriPath = path;
		if (prefix != null)
		{
			if (path.contains("html/"))
			{
				path = path.replace("html/", "html/" + prefix + "/");
				oriPath = oriPath.replace("html/", "html/en/");
			}
		}
		
		var content = HtmCache.getInstance().getHtm(player, path);
		if (content == null && !oriPath.equals(path))
		{
			content = HtmCache.getInstance().getHtm(player, oriPath);
		}
		return content;
	}

	public String getHtm(Player player, String path)
	{
		if ((path == null) || path.isEmpty())
		{
			return "";
		}

		var content = _cache.get(path);
		if (content == null)
		{
			content = loadFile(new File(Config.DATAPACK_ROOT, path));
		}

		if (player != null && player.isGM() && content != null)
		{
			final var link = path.replace("data/html/", "");
			final String[] output = link.split("/");
			player.sendMessage(output[0].toUpperCase() + ":" + link.replace(output[0] + "/", ""));
		}
		return content;
	}

	public boolean contains(String path)
	{
		var oriPath = path;
		if (oriPath.contains("html/"))
		{
			oriPath = oriPath.replace("html/", "html/en/");
		}
		return _cache.containsKey(oriPath);
	}

	public boolean isLoadable(String path)
	{
		var oriPath = path;
		if (oriPath.contains("html/"))
		{
			oriPath = oriPath.replace("html/", "html/en/");
		}
		return htmlFilter.accept(new File(oriPath));
	}

	public static HtmCache getInstance()
	{
		return SingletonHolder._instance;
	}

	private static class SingletonHolder
	{
		protected static final HtmCache _instance = new HtmCache();
	}
}