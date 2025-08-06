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
import l2e.gameserver.instancemanager.SiegeManager;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.entity.events.cleft.AerialCleftEvent;
import l2e.gameserver.network.SystemMessageId;

public class Observation implements IBypassHandler
{
	private static final String[] COMMANDS =
	{
	        "observesiege", "observeoracle", "observe"
	};
	
	@Override
	public boolean useBypass(String command, Player activeChar, Creature target)
	{
		if (!target.isNpc())
		{
			return false;
		}
		
		try
		{
			if (command.toLowerCase().startsWith(COMMANDS[0])) // siege
			{
				final String val = command.substring(13);
				final StringTokenizer st = new StringTokenizer(val);
				st.nextToken(); // Bypass cost
				
				if (SiegeManager.getInstance().getSiege(Integer.parseInt(st.nextToken()), Integer.parseInt(st.nextToken()), Integer.parseInt(st.nextToken())) != null)
				{
					doObserve(activeChar, (Npc) target, val);
				}
				else
				{
					activeChar.sendPacket(SystemMessageId.ONLY_VIEW_SIEGE);
				}
				return true;
			}
			else if (command.toLowerCase().startsWith(COMMANDS[1])) // oracle
			{
				final String val = command.substring(13);
				final StringTokenizer st = new StringTokenizer(val);
				st.nextToken(); // Bypass cost
				doObserve(activeChar, (Npc) target, val);
				return true;
			}
			else if (command.toLowerCase().startsWith(COMMANDS[2])) // observe
			{
				doObserve(activeChar, (Npc) target, command.substring(8));
				return true;
			}
			
			return false;
		}
		catch (final Exception e)
		{
			_log.warn("Exception in " + getClass().getSimpleName(), e);
		}
		return false;
	}
	
	private static final void doObserve(Player player, Npc npc, String val)
	{
		if (player.isInParty() || player.isPartyBanned() || player.isCursedWeaponEquipped() || player.isCursedWeaponEquipped() || player.isInFightEvent() || player.checkInTournament())
		{
			player.sendMessage("You can not observe games while registered for event.");
			return;
		}
		
		if ((AerialCleftEvent.getInstance().isStarted() || AerialCleftEvent.getInstance().isRewarding()) && AerialCleftEvent.getInstance().isPlayerParticipant(player.getObjectId()))
		{
			player.sendMessage("You can not observe games while registered for event.");
			return;
		}
		
		final StringTokenizer st = new StringTokenizer(val);
		final long cost = Long.parseLong(st.nextToken());
		final int x = Integer.parseInt(st.nextToken());
		final int y = Integer.parseInt(st.nextToken());
		final int z = Integer.parseInt(st.nextToken());
		
		if (player.reduceAdena("Broadcast", cost, npc, true))
		{
			player.enterObserverMode(x, y, z);
		}
		player.sendActionFailed();
	}
	
	@Override
	public String[] getBypassList()
	{
		return COMMANDS;
	}
}