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
package l2e.gameserver.model.actor.templates;

import l2e.gameserver.network.NpcStringId;

public class NpcWalkerTemplate
{
	private final int _routeId;
	private final String _chatString;
	private final NpcStringId _npcString;
	private final int _moveX;
	private final int _moveY;
	private final int _moveZ;
	private final int _delay;
	private final boolean _running;

	public NpcWalkerTemplate(int routeId, NpcStringId npcString, String chatText, int moveX, int moveY, int moveZ, int delay, boolean running)
	{
		super();
		_routeId = routeId;
		_chatString = chatText;
		_npcString = npcString;
		_moveX = moveX;
		_moveY = moveY;
		_moveZ = moveZ;
		_delay = delay;
		_running = running;
	}

	public int getRouteId()
	{
		return _routeId;
	}

	public String getChatText()
	{
		if (_npcString != null)
		{
			throw new IllegalStateException("npcString is defined for walker route!");
		}
		return _chatString;
	}

	public int getMoveX()
	{
		return _moveX;
	}

	public int getMoveY()
	{
		return _moveY;
	}

	public int getMoveZ()
	{
		return _moveZ;
	}

	public int getDelay()
	{
		return _delay;
	}

	public boolean getRunning()
	{
		return _running;
	}
	
	public NpcStringId getNpcString()
	{
		return _npcString;
	}
}