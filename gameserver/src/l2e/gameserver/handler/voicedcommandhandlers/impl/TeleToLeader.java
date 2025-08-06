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
package l2e.gameserver.handler.voicedcommandhandlers.impl;

import l2e.gameserver.Config;
import l2e.gameserver.data.parser.ItemsParser;
import l2e.gameserver.handler.voicedcommandhandlers.IVoicedCommandHandler;
import l2e.gameserver.instancemanager.CastleManager;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.entity.events.cleft.AerialCleftEvent;
import l2e.gameserver.model.strings.server.ServerMessage;
import l2e.gameserver.model.zone.ZoneId;

public class TeleToLeader implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
	        "teletocl"
	};

	@Override
	public boolean useVoicedCommand(String command, Player activeChar, String target)
	{
		if (!Config.ALLOW_TELETO_LEADER)
		{
			return false;
		}
		
		if (command.equalsIgnoreCase("teletocl"))
		{
			
			if (activeChar.getClan() == null)
			{
				return false;
			}

			final Player leader = GameObjectsStorage.getPlayer(activeChar.getClan().getLeaderId());
			if (leader == null)
			{
				activeChar.sendMessage((new ServerMessage("TeleToLeader.LEADER_OFFLINE", activeChar.getLang())).toString());
				return false;
			}
			else if (leader.isJailed())
			{
				activeChar.sendMessage((new ServerMessage("TeleToLeader.LEADER_ISJAILED", activeChar.getLang())).toString());
				return false;
			}
			else if (leader.isInOlympiadMode())
			{
				activeChar.sendMessage((new ServerMessage("TeleToLeader.LEADER_INOLYPIAD", activeChar.getLang())).toString());
				return false;
			}
			else if (leader.isInDuel())
			{
				activeChar.sendMessage((new ServerMessage("TeleToLeader.LEADER_INDUEL", activeChar.getLang())).toString());
				return false;
			}
			else if (leader.isFestivalParticipant())
			{
				activeChar.sendMessage((new ServerMessage("TeleToLeader.LEADER_INFESTIVAL", activeChar.getLang())).toString());
				return false;
			}
			else if (leader.isInParty() && leader.getParty().isInDimensionalRift())
			{
				activeChar.sendMessage((new ServerMessage("TeleToLeader.LEADER_INRIFT", activeChar.getLang())).toString());
				return false;
			}
			else if (leader.inObserverMode())
			{
				activeChar.sendMessage((new ServerMessage("TeleToLeader.LEADER_REGTOOLY", activeChar.getLang())).toString());
			}
			else if ((leader.getClan() != null) && (CastleManager.getInstance().getCastleByOwner(leader.getClan()) != null) && CastleManager.getInstance().getCastleByOwner(leader.getClan()).getSiege().getIsInProgress())
			{
				activeChar.sendMessage((new ServerMessage("TeleToLeader.LEADER_INSIEGE", activeChar.getLang())).toString());
				return false;
			}
			else if (leader.checkInTournament() || leader.isInFightEvent() || ((AerialCleftEvent.getInstance().isStarted() || AerialCleftEvent.getInstance().isRewarding()) && AerialCleftEvent.getInstance().isPlayerParticipant(leader.getObjectId())))
			{
				activeChar.sendMessage((new ServerMessage("TeleToLeader.LEADER_INEVENT", activeChar.getLang())).toString());
				return false;
			}
			else if ((leader.getParty() != null && leader.getParty().getUCState() != null) || leader.getUCState() > 0)
			{
				activeChar.sendMessage((new ServerMessage("TeleToLeader.LEADER_INEVENT", activeChar.getLang())).toString());
				return false;
			}
			else if (leader.isInKrateisCube() || leader.getReflectionId() != 0)
			{
				activeChar.sendMessage((new ServerMessage("Community.ALL_DISABLE", activeChar.getLang())).toString());
				return false;
			}
			else if (leader.isInsideZone(ZoneId.NO_RESTART) || leader.isInsideZone(ZoneId.SIEGE))
			{
				activeChar.sendMessage((new ServerMessage("Community.ALL_DISABLE", activeChar.getLang())).toString());
				return false;
			}
			else if (activeChar.isJailed())
			{
				activeChar.sendMessage((new ServerMessage("TeleToLeader.YOU_INJAILED", activeChar.getLang())).toString());
				return false;
			}
			else if (activeChar.isInOlympiadMode())
			{
				activeChar.sendMessage((new ServerMessage("TeleToLeader.YOU_INOLIMPIAD", activeChar.getLang())).toString());
				return false;
			}
			else if (activeChar.isInDuel())
			{
				activeChar.sendMessage((new ServerMessage("TeleToLeader.YOU_INDUEL", activeChar.getLang())).toString());
				return false;
			}
			else if (activeChar.inObserverMode())
			{
				activeChar.sendMessage((new ServerMessage("TeleToLeader.YOU_REGTOOLY", activeChar.getLang())).toString());
			}
			else if ((activeChar.getClan() != null) && (CastleManager.getInstance().getCastleByOwner(activeChar.getClan()) != null) && CastleManager.getInstance().getCastleByOwner(activeChar.getClan()).getSiege().getIsInProgress())
			{
				activeChar.sendMessage((new ServerMessage("TeleToLeader.YOU_INSIEGE", activeChar.getLang())).toString());
				return false;
			}
			else if (activeChar.isFestivalParticipant())
			{
				activeChar.sendMessage((new ServerMessage("TeleToLeader.YOU_INFESTIVAL", activeChar.getLang())).toString());
				return false;
			}
			else if (activeChar.isInParty() && activeChar.getParty().isInDimensionalRift())
			{
				activeChar.sendMessage((new ServerMessage("TeleToLeader.YOU_INRIGT", activeChar.getLang())).toString());
				return false;
			}
			else if ((activeChar.getParty() != null && activeChar.getParty().getUCState() != null) || activeChar.getUCState() > 0)
			{
				activeChar.sendMessage((new ServerMessage("TeleToLeader.YOU_INRIGT", activeChar.getLang())).toString());
				return false;
			}
			else if (activeChar.isInKrateisCube() || activeChar.isInFightEvent() || ((AerialCleftEvent.getInstance().isStarted() || AerialCleftEvent.getInstance().isRewarding()) && AerialCleftEvent.getInstance().isPlayerParticipant(activeChar.getObjectId())))
			{
				activeChar.sendMessage((new ServerMessage("TeleToLeader.YOU_INEVENT", activeChar.getLang())).toString());
				return false;
			}
			else if (activeChar.isInCombat() || activeChar.isInStoreMode() || activeChar.isSellingBuffs())
			{
				activeChar.sendMessage((new ServerMessage("Community.ALL_DISABLE", activeChar.getLang())).toString());
				return false;
			}
			else if (activeChar.getReflectionId() != 0)
			{
				activeChar.sendMessage((new ServerMessage("Community.ALL_DISABLE", activeChar.getLang())).toString());
				return false;
			}
			else if (activeChar.isInsideZone(ZoneId.NO_RESTART) || activeChar.isInsideZone(ZoneId.SIEGE))
			{
				activeChar.sendMessage((new ServerMessage("Community.ALL_DISABLE", activeChar.getLang())).toString());
				return false;
			}
			else if (activeChar == leader)
			{
				activeChar.sendMessage((new ServerMessage("TeleToLeader.YOU_LEADER", activeChar.getLang())).toString());
				return false;
			}
			
			if (activeChar.getInventory().getItemByItemId(Config.TELETO_LEADER_ID) == null)
			{
				final ServerMessage msg = new ServerMessage("TeleToLeader.NOT_ITEMS", activeChar.getLang());
				msg.add(ItemsParser.getInstance().getTemplate(Config.TELETO_LEADER_ID).getName(activeChar.getLang()));
				msg.add(Config.TELETO_LEADER_COUNT);
				activeChar.sendMessage(msg.toString());
				return false;
			}
			
			activeChar.teleToLocation(leader.getX(), leader.getY(), leader.getZ(), true, leader.getReflection());
			activeChar.sendMessage((new ServerMessage("TeleToLeader.YOU_TELETOCL", activeChar.getLang())).toString());
			activeChar.getInventory().destroyItemByItemId("RessSystem", Config.TELETO_LEADER_ID, Config.TELETO_LEADER_COUNT, activeChar, activeChar.getTarget());
			final ServerMessage msg = new ServerMessage("TeleToLeader.TAKE_ITEMS", activeChar.getLang());
			msg.add(ItemsParser.getInstance().getTemplate(Config.TELETO_LEADER_ID).getName(activeChar.getLang()));
			msg.add(Config.TELETO_LEADER_COUNT);
			activeChar.sendMessage(msg.toString());
		}
		return true;
	}

	@Override
	public String[] getVoicedCommandList()
	{
		return VOICED_COMMANDS;
	}
}