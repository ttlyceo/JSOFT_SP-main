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
package l2e.gameserver.handler.admincommandhandlers.impl.model;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2e.gameserver.Config;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.data.htm.HtmCache;
import l2e.gameserver.handler.communityhandlers.impl.AbstractCommunity;
import l2e.gameserver.model.actor.Player;

public class ViewerUtils
{
	private static final Logger _log = LoggerFactory.getLogger(ViewerUtils.class);
	
	private static int readLinesAmount(String path)
	{
		final File file = new File(Config.DATAPACK_ROOT, path);
		if (file.exists())
		{
			int i = 0;
			try (
			    LineNumberReader lnr = new LineNumberReader(new FileReader(file)))
			{
				String line = null;
				while ((line = lnr.readLine()) != null)
				{
					final StringTokenizer st = new StringTokenizer(line, Config.EOL);
					if (st.hasMoreTokens())
					{
						i++;
					}
				}
				return i;
			}
			catch (final IOException e1)
			{
				_log.warn("Error reading announcements: ", e1);
			}
		}
		else
		{
			_log.warn(file.getAbsolutePath() + " doesn't exist");
		}
		return 0;
	}
	
	private static String readFromDisk(String path)
	{
		final int totalAmount = readLinesAmount(path) - 26;
		final File file = new File(Config.DATAPACK_ROOT, path);
		if (file.exists())
		{
			String lines = "";
			int i = 0;
			try (
			    LineNumberReader lnr = new LineNumberReader(new FileReader(file)))
			{
				String line = null;
				while ((line = lnr.readLine()) != null)
				{
					final StringTokenizer st = new StringTokenizer(line, Config.EOL);
					if (st.hasMoreTokens())
					{
						i++;
						if (i <= totalAmount)
						{
							continue;
						}
						lines += "<tr><td align=left valign=top><font color=66FFFF>" + st.nextToken() + "</font></td></tr>";
					}
				}
				return lines;
			}
			catch (final IOException e1)
			{
				_log.warn("Error reading announcements: ", e1);
			}
		}
		else
		{
			_log.warn(file.getAbsolutePath() + " doesn't exist");
		}
		return null;
	}
  
	public static void startLogViewer(Player player, String pach)
	{
		sendCbWindow(player, pach);
		player._captureTask = ThreadPoolManager.getInstance().schedule(new Capture(player, pach), 1000);
	}
  
	public static void stopLogViewer(Player player, String pach)
	{
		if (player._captureTask != null)
		{
			player._captureTask.cancel(true);
		}
		sendCbWindow(player, pach);
	}
  
	public static void sendCbWindow(Player player, String file)
	{
		String html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/admin/logViewer-template.htm");
		html = html.replace("%file%", file);
		
		if ((player._captureTask != null) && (!player._captureTask.isDone()))
		{
			html = html.replace("%stop_button_fore%", "L2UI_CT1.Button_DF.Gauge_DF_Attribute_Fire").replace("%stop_button_back%", "L2UI_CT1.Button_DF.Gauge_DF_Attribute_Fire_bg");
		}
		else
		{
			html = html.replace("%stop_button_fore%", "L2UI_CT1.Button_DF.Button_DF").replace("%stop_button_back%", "L2UI_CT1.Button_DF.Button_DF_Down");
		}
		
		final String logs = readFromDisk("./log/" + file);
		if (logs != null)
		{
			html = html.replace("%logs%", readFromDisk("./log/" + file));
		}
		else
		{
			html = html.replace("%logs%", "");
			player.sendMessage("Error! File " + file + " not found!");
			if (player._captureTask != null)
			{
				player._captureTask.cancel(true);
			}
		}
		AbstractCommunity.separateAndSend(html, player);
	}
}