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

import l2e.gameserver.instancemanager.PunishmentManager;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.punishment.PunishmentTemplate;

public class PunishmentTask extends AbstractPlayerTask
{
	private final PunishmentTemplate _template;
	private final long _delay;

	public PunishmentTask(long delay, PunishmentTemplate template)
	{
		_delay = delay;
		_template = template;
	}
	
	@Override
	public boolean getTask(Player player)
	{
		if (player != null)
		{
			PunishmentManager.getInstance().stopPunishment(player.getClient(), _template);
		}
		return true;
	}
	
	@Override
	public int getId()
	{
		return 10;
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
	
	public PunishmentTemplate getTemplate()
	{
		return _template;
	}
	
	@Override
	public long getInterval()
	{
		return System.currentTimeMillis() + _delay;
	}
}