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
package l2e.gameserver.model.actor.protection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2e.gameserver.Config;
import l2e.gameserver.model.actor.Player;

public class ExtensionProtection
{
	protected static final Logger _log = LoggerFactory.getLogger(ExtensionProtection.class);

	private Player _activeChar = null;
	
	public ExtensionProtection(Player activeChar)
	{
		if (activeChar == null)
		{
			_log.warn("[ExtensionProtection] _activeChar: There can be a null value!");
			return;
		}
		_activeChar = activeChar;
		if (Config.DEBUG)
		{
			_log.info("[ExtensionProtection] _activeChar: " + _activeChar.getObjectId() + " - " + _activeChar.getName(null) + ".");
		}
	}

	public Player getPlayer()
	{
		return _activeChar;
	}
}