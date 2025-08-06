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
package l2e.gameserver.model.actor.tasks.character;

import l2e.gameserver.geodata.GeoEngine;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.network.SystemMessageId;

public final class MagicGeoCheckTask implements Runnable
{
	private final Creature _character;
	
	public MagicGeoCheckTask(Creature character)
	{
		_character = character;
	}
	
	@Override
	public void run()
	{
		if (_character == null)
		{
			return;
		}
		
		final Creature castingTarget = _character.getCastingTarget();
		if (castingTarget == null || !_character.isCastingNow())
		{
			return;
		}
		
		if (!GeoEngine.getInstance().canSeeTarget(_character, castingTarget))
		{
			if (_character.isPlayer())
			{
				_character.sendPacket(SystemMessageId.CANT_SEE_TARGET);
			}
			_character.abortCast();
			return;
		}
		_character._skillGeoCheckTask = null;
	}
}