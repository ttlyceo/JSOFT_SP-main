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
package l2e.gameserver.database;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import l2e.commons.log.LoggerObject;
import l2e.gameserver.Config;

public final class DatabaseBackupFactory extends LoggerObject
{
	private static final Pattern DATABASE_NAME_PATTERN = Pattern.compile("jdbc:mariadb://localhost/(.+?)\\?(.+?)");

	private final String FOLDER_PATH;

	public DatabaseBackupFactory()
	{
		FOLDER_PATH = new File("").getAbsolutePath() + "\\log\\backups";
		new File(FOLDER_PATH).mkdir();
	}

	public void doBackup()
	{
		final var databaseName = getDatabaseName();
		if (databaseName == null)
		{
			warn("Error while getting Database Name!");
			return;
		}

		final var path = getPath(databaseName);
		if (path == null)
		{
			warn("Error while creating Backup File!");
			return;
		}
		execute(databaseName, path);
	}

	private void execute(String database, String path)
	{
		final var filePath = new File(path);
		
		PrintWriter printWriter = null;
		BufferedReader bufferedReader = null;
		try
		{
			printWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(filePath), "utf8"));
			final var run = Runtime.getRuntime().exec("mysqldump --user=" + Config.DATABASE_LOGIN + " --password=" + Config.DATABASE_PASSWORD + " --default-character-set=binary -c " + database + "");
			final var inputStreamReader = new InputStreamReader(run.getInputStream(), "utf8");
			bufferedReader = new BufferedReader(inputStreamReader);
			String line;
			while ((line = bufferedReader.readLine()) != null)
			{
				printWriter.println(line);
			}
			printWriter.flush();
			
			if (run.waitFor() == 0)
			{
				info("Complete reading info...");
			}
		}
		catch (final Exception t)
		{
			warn("Error while creating backup execute! " + t);
		}
		finally
		{
			try
			{
				if (bufferedReader != null)
				{
					bufferedReader.close();
				}
				if (printWriter != null)
				{
					printWriter.close();
				}
			}
			catch (final IOException e)
			{
				e.printStackTrace();
			}
		}
		
		ZipOutputStream zip = null;
		try
		{
			zip = new ZipOutputStream(new FileOutputStream(getZipPath(database)));
		}
		catch (final FileNotFoundException e)
		{
			e.printStackTrace();
		}
		
		final var file = new File(path);
		try
		{
			if (!file.exists())
			{
				return;
			}
			final var bis = new BufferedInputStream(new FileInputStream(file));
			try
			{
				zip.setMethod(8);
				zip.setLevel(9);
				zip.putNextEntry(new ZipEntry("" + database + ".sql"));
				
				int count;
				final byte data[] = new byte[10485760];
				while ((count = bis.read(data, 0, 10485760)) != -1)
				{
					zip.write(data, 0, count);
				}
				
			}
			catch (final Exception e)
			{
				throw new RuntimeException(e);
			}
			finally
			{
				zip.closeEntry();
				bis.close();
			}
			
		}
		catch (final Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				zip.close();
				file.delete();
			}
			catch (final IOException e)
			{
				e.printStackTrace();
			}
		}
		info("Finish Backup Database...");
	}

	private String getDatabaseName()
	{
		final Matcher m = DATABASE_NAME_PATTERN.matcher(Config.DATABASE_URL);
		if (m.find())
		{
			return m.group(1);
		}
		return null;
	}

	private String getPath(String databaseName)
	{
		return FOLDER_PATH + "\\" + databaseName + ".sql";
	}
	
	private String getZipPath(String databaseName)
	{
		final var c = Calendar.getInstance();
		final var format1 = new SimpleDateFormat("yyyy-MM-dd_HH-mm");
		final var formatted = format1.format(c.getTime());
		return FOLDER_PATH + "\\" + "[" + databaseName + "]_" + formatted + ".zip";
	}

	public static DatabaseBackupFactory getInstance()
	{
		return SingletonHolder._instance;
	}

	private static class SingletonHolder
	{
		protected static final DatabaseBackupFactory _instance = new DatabaseBackupFactory();
	}
}