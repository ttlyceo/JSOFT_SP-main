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
import l2e.gameserver.instancemanager.TerritoryWarManager;
import l2e.gameserver.model.Clan;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.FortSiegeNpcInstance;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.SystemMessage;

public class FortSiege implements IBypassHandler
{
	private static final String[] COMMANDS =
	{
	        "fort_register", "fort_unregister"
	};
	
	@Override
	public boolean useBypass(String command, Player activeChar, Creature target)
	{
		if (!(target instanceof FortSiegeNpcInstance))
		{
			return false;
		}

		if ((activeChar.getClanId() > 0) && ((activeChar.getClanPrivileges() & Clan.CP_CS_MANAGE_SIEGE) == Clan.CP_CS_MANAGE_SIEGE))
		{
			if (command.toLowerCase().startsWith(COMMANDS[0]))
			{
				if ((System.currentTimeMillis() < TerritoryWarManager.getInstance().getTWStartTimeInMillis()) && TerritoryWarManager.getInstance().getIsRegistrationOver())
				{
					activeChar.sendPacket(SystemMessageId.NOT_SIEGE_REGISTRATION_TIME2);
					return false;
				}
				else if ((System.currentTimeMillis() > TerritoryWarManager.getInstance().getTWStartTimeInMillis()) && TerritoryWarManager.getInstance().isTWChannelOpen())
				{
					activeChar.sendPacket(SystemMessageId.NOT_SIEGE_REGISTRATION_TIME2);
					return false;
				}
				else if (((Npc) target).getFort().getSiege().registerAttacker(activeChar, false))
				{
					final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.REGISTERED_TO_S1_FORTRESS_BATTLE);
					sm.addString(((Npc) target).getFort().getName());
					activeChar.sendPacket(sm);
					((Npc) target).showChatWindow(activeChar, 7);
					return true;
				}
			}
			else if (command.toLowerCase().startsWith(COMMANDS[1]))
			{
				((Npc) target).getFort().getSiege().removeSiegeClan(activeChar.getClan());
				((Npc) target).showChatWindow(activeChar, 8);
				return true;
			}
			return false;
		}

		((Npc) target).showChatWindow(activeChar, 10);

		return true;
	}

	@Override
	public String[] getBypassList()
	{
		return COMMANDS;
	}
}