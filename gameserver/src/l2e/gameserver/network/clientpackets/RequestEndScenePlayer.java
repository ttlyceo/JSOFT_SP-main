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
package l2e.gameserver.network.clientpackets;

import l2e.gameserver.model.actor.Player;

public final class RequestEndScenePlayer extends GameClientPacket
{
	private int _movieId;
	
	@Override
	protected void readImpl()
	{
		_movieId = readD();
	}

	@Override
	protected void runImpl()
	{
		final Player activeChar = getClient().getActiveChar();
		if (activeChar == null)
		{
			return;
		}
		if (_movieId == 0)
		{
			return;
		}
		if (activeChar.getMovieId() != _movieId)
		{
			_log.warn("Player " + getClient() + " sent EndScenePlayer with wrong movie id: " + _movieId);
			return;
		}
		activeChar.setMovieId(0);
		activeChar.setIsTeleporting(true);
		activeChar.decayMe();
		activeChar.spawnMe(activeChar.getX(), activeChar.getY(), activeChar.getZ());
		activeChar.setIsTeleporting(false);
	}
}