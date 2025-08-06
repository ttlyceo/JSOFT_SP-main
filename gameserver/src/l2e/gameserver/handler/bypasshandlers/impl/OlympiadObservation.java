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
import l2e.gameserver.model.entity.events.cleft.AerialCleftEvent;
import l2e.gameserver.model.entity.events.tournaments.TournamentData;
import l2e.gameserver.model.entity.events.tournaments.util.TournamentUtil;
import l2e.gameserver.model.olympiad.Olympiad;
import l2e.gameserver.model.olympiad.OlympiadGameManager;
import l2e.gameserver.model.olympiad.OlympiadManager;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.ExReceiveOlympiadForTournamentList;
import l2e.gameserver.network.serverpackets.ExReceiveOlympiadList;

public class OlympiadObservation implements IBypassHandler
{
	private static final String[] COMMANDS =
	{
	        "watchmatch", "arenachange"
	};
	
	@Override
	public final boolean useBypass(String command, Player activeChar, Creature target)
	{
		try
		{
			final var olymanager = activeChar.getLastFolkNPC();
			
			if (command.startsWith(COMMANDS[0]))
			{
				if(TournamentUtil.TOURNAMENT_MAIN.isEnable() && activeChar.getTournamentGameId() != -1 && activeChar.inObserverMode())
				{
					activeChar.sendPacket(new ExReceiveOlympiadForTournamentList.OlympiadList());
					return true;
				}

				if (!Olympiad.getInstance().inCompPeriod())
				{
					activeChar.sendPacket(SystemMessageId.THE_OLYMPIAD_GAME_IS_NOT_CURRENTLY_IN_PROGRESS);
					return false;
				}
				activeChar.sendPacket(new ExReceiveOlympiadList.OlympiadList());
			}
			else
			{
				if (!activeChar.inObserverMode() && (olymanager == null || !activeChar.isInsideRadius(olymanager, 300, false, false)))
				{
					return false;
				}
				
				if (OlympiadManager.getInstance().isRegisteredInComp(activeChar))
				{
					activeChar.sendPacket(SystemMessageId.WHILE_YOU_ARE_ON_THE_WAITING_LIST_YOU_ARE_NOT_ALLOWED_TO_WATCH_THE_GAME);
					return false;
				}

				if (activeChar.isInKrateisCube() || activeChar.getUCState() > 0 || activeChar.isInFightEvent() || AerialCleftEvent.getInstance().isPlayerParticipant(activeChar.getObjectId()))
				{
					activeChar.sendMessage("You can not observe games while registered for Event");
					return false;
				}

				if(TournamentUtil.TOURNAMENT_MAIN.isEnable() && activeChar.getTournamentGameId() != -1 && activeChar.inObserverMode())
				{
					final int arenaId = Integer.parseInt(command.substring(12).trim());
					final var tournament = TournamentData.getInstance().getTournament(arenaId);
					if(tournament != null)
					{
						if(activeChar.getTournamentGameId() != tournament.getId())
							activeChar.enterTournamentObserverMode(tournament);
						else
							activeChar.sendMessage("You can not observe same game.");
						return true;
					}
					return false;
				}

				if (!Olympiad.getInstance().inCompPeriod())
				{
					activeChar.sendPacket(SystemMessageId.THE_OLYMPIAD_GAME_IS_NOT_CURRENTLY_IN_PROGRESS);
					return false;
				}

				final int arenaId = Integer.parseInt(command.substring(12).trim());
				final var nextArena = OlympiadGameManager.getInstance().getOlympiadTask(arenaId);
				if (nextArena != null)
				{
					activeChar.enterOlympiadObserverMode(nextArena.getZone().getSpectatorSpawns().get(0), arenaId);
					activeChar.setReflection(OlympiadGameManager.getInstance().getOlympiadTask(arenaId).getZone().getReflection());
				}
			}
			return true;
			
		}
		catch (final Exception e)
		{
			_log.warn("Exception in " + getClass().getSimpleName(), e);
		}
		return false;
	}
	
	@Override
	public final String[] getBypassList()
	{
		return COMMANDS;
	}
}