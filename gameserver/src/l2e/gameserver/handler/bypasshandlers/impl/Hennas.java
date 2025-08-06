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

import l2e.gameserver.handler.bypasshandlers.IBypassHandler;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.SymbolMakerInstance;
import l2e.gameserver.model.actor.templates.items.Henna;
import l2e.gameserver.network.serverpackets.HennaEquipList;
import l2e.gameserver.network.serverpackets.HennaUnequipList;

public class Hennas implements IBypassHandler
{
	private static final String[] COMMANDS =
	{
	        "Draw", "RemoveList"
	};

	@Override
	public boolean useBypass(String command, Player activeChar, Creature target)
	{
		if (!(target instanceof SymbolMakerInstance))
		{
			return false;
		}
		
		if (command.equals("Draw"))
		{
			activeChar.sendPacket(new HennaEquipList(activeChar));
		}
		else if (command.equals("RemoveList"))
		{
			for (final Henna henna : activeChar.getHennaList())
			{
				if (henna != null)
				{
					activeChar.sendPacket(new HennaUnequipList(activeChar));
					break;
				}
			}
		}
		return true;
	}
	
	@Override
	public String[] getBypassList()
	{
		return COMMANDS;
	}
}