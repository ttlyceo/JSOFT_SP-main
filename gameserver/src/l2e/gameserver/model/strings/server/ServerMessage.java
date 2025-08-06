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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import l2e.commons.log.LoggerObject;
import l2e.gameserver.Config;

public class ServerMessage extends LoggerObject
{
	private int _index = 0;
	private String _message;
	private String _messageName;
	private boolean _isStoreType;
	private Map<String, ArrayList<String>> _args;
	
	public ServerMessage(String unicName, String lang)
	{
		_message = ServerStorage.getInstance().getString(lang, unicName);
		if (_message == null)
		{
			warn("message named \"" + unicName + "\" not found!");
			_message = "";
		}
	}
	
	public ServerMessage(String unicName, boolean isStoreType)
	{
		_messageName = unicName;
		_isStoreType = isStoreType;
	}
	
	public void add(Object l)
	{
		if (_isStoreType)
		{
			for (final String lang : Config.MULTILANG_ALLOWED)
			{
				if (lang != null)
				{
					getStoredArgs(lang).add(String.valueOf(l));
				}
			}
		}
		else
		{
			_message = _message.replace(String.format("{%d}", _index), String.valueOf(l));
			_index++;
		}
	}
	
	public void add(String lang, Object l)
	{
		if (_isStoreType)
		{
			getStoredArgs(lang).add(String.valueOf(l));
		}
		else
		{
			_message = _message.replace(String.format("{%d}", _index), String.valueOf(l));
			_index++;
		}
	}
	
	@Override
	public String toString()
	{
		return _isStoreType ? toString(Config.MULTILANG_DEFAULT) : _message;
	}
	
	public String toString(String lang)
	{
		if (!_isStoreType)
		{
			return "";
		}
		_message = ServerStorage.getInstance().getString(lang, _messageName);
		if (_message == null)
		{
			warn("message named \"" + _messageName + "\" not found!");
			return "";
		}
		for (final String arg : getStoredArgs(lang))
		{
			_message = _message.replace(String.format("{%d}", _index), arg);
			_index++;
		}
		_index = 0;
		return _message;
	}
	
	private ArrayList<String> getStoredArgs(String lang)
	{
		if (_args == null)
		{
			_args = new HashMap<>();
		}
		
		if (!_args.containsKey(lang))
		{
			_args.put(lang, new ArrayList<>());
		}
		return _args.get(lang);
	}
}