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
package l2e.gameserver.listener;

import java.nio.file.Path;

public abstract class ManagedLoader
{
	private final Path _scriptFile;
	private long _lastLoadTime;
	private boolean _isActive;
	
	public ManagedLoader()
	{
		_scriptFile = ScriptListenerLoader.getFileForClass(getClass());
		setLastLoadTime(System.currentTimeMillis());
	}
	
	public boolean reload()
	{
		try
		{
			ScriptListenerLoader.getInstance().executeScript(getScriptFile());
			return true;
		}
		catch (final Exception e)
		{
			return false;
		}
	}
	
	public abstract boolean unload();
	public abstract String getScriptName();
	
	public void setActive(boolean status)
	{
		_isActive = status;
	}
	
	public boolean isActive()
	{
		return _isActive;
	}
	
	public Path getScriptFile()
	{
		return _scriptFile;
	}
	
	protected void setLastLoadTime(long lastLoadTime)
	{
		_lastLoadTime = lastLoadTime;
	}
	
	protected long getLastLoadTime()
	{
		return _lastLoadTime;
	}
}