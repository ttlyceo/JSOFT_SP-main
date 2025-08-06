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
package l2e.gameserver.handler.bypasshandlers.impl;

import java.util.StringTokenizer;

import l2e.gameserver.handler.bypasshandlers.IBypassHandler;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.MerchantInstance;

public class Buy implements IBypassHandler
{
	private static final String[] COMMANDS =
	{
	        "Buy"
	};
	
	@Override
	public boolean useBypass(String command, Player activeChar, Creature target)
	{
		if (!(target instanceof MerchantInstance))
		{
			return false;
		}

		try
		{
			final StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken();

			if (st.countTokens() < 1)
			{
				return false;
			}

			((MerchantInstance) target).showBuyWindow(activeChar, Integer.parseInt(st.nextToken()));
			return true;
		}
		catch (final Exception e)
		{
			_log.warn("Exception in " + getClass().getSimpleName(), e);
		}
		return false;
	}

	@Override
	public String[] getBypassList()
	{
		return COMMANDS;
	}
}