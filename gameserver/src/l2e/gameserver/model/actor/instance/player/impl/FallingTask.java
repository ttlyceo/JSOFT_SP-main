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
package l2e.gameserver.model.actor.instance.player.impl;

import l2e.gameserver.geodata.GeoEngine;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.zone.ZoneId;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.SystemMessage;

public class FallingTask extends AbstractPlayerTask
{
	private final long _delay;
	private final int _damage;
	
	public FallingTask(long delay, int damage)
	{
		_delay = delay;
		_damage = damage;
	}
	
	@Override
	public boolean getTask(Player player)
	{
		if (player == null || player.isDead())
		{
			return false;
		}
		
		if (GeoEngine.getInstance().hasGeo(player.getX(), player.getY()) && !player.isInsideZone(ZoneId.NO_GEO))
		{
			final int z = (int) GeoEngine.getInstance().getSpawnHeight(player.getLocation());
			player.setZ(z);
		}
		player.reduceCurrentHp(Math.min(_damage, player.getCurrentHp() - 1), null, false, true, null);
		final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.FALL_DAMAGE_S1);
		sm.addNumber(_damage);
		player.sendPacket(sm);
		return true;
	}
	
	@Override
	public int getId()
	{
		return 14;
	}
	
	@Override
	public boolean isOneUse()
	{
		return false;
	}
	
	@Override
	public boolean isSingleUse()
	{
		return true;
	}
	
	@Override
	public long getInterval()
	{
		return System.currentTimeMillis() + _delay;
	}
}