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
package l2e.gameserver.model.actor.instance;

import l2e.gameserver.Config;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.entity.events.cleft.AerialCleftEvent;
import l2e.gameserver.model.olympiad.OlympiadManager;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.network.SystemMessageId;

/**
 * Created by LordWinter 03.12.2018
 */
public final class CleftGatesInstance extends NpcInstance
{
	public CleftGatesInstance(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public void showChatWindow(Player player)
	{
		boolean checkReg = false;
		for (final Skill s : player.getAllSkills())
		{
			if (s == null)
			{
				continue;
			}

			if (s.getId() == 840 || s.getId() == 841 || s.getId() == 842)
			{
				checkReg = true;
			}
		}
		
		if (player.getLevel() < 75 || !checkReg)
		{
			showChatWindow(player, "data/html/aerialCleft/32518-03.htm");
			return;
		}
		switch (getId())
		{
			case 32518 :
			case 32519 :
				if (AerialCleftEvent.getInstance().isPlayerParticipant(player.getObjectId()))
				{
					showChatWindow(player, "data/html/aerialCleft/32518-02.htm");
				}
				else
				{
					showChatWindow(player, "data/html/aerialCleft/32518-00.htm");
				}
				break;
		}
	}

	@Override
	public void onBypassFeedback(Player player, String command)
	{
		if ((player == null) || (player.getLastFolkNPC() == null) || (player.getLastFolkNPC().getObjectId() != getObjectId()))
		{
			return;
		}

		if (command.equalsIgnoreCase("Info"))
		{
			showChatWindow(player, "data/html/aerialCleft/32518-01.htm");
		}
		else if (command.startsWith("Register"))
		{
			if (player.isInKrateisCube() || OlympiadManager.getInstance().isRegistered(player) || player.isInOlympiadMode() || player.isInFightEvent() || player.isRegisteredInFightEvent() || player.getTeam() != 0)
			{
				player.sendPacket(SystemMessageId.YOU_CANNOT_BE_SIMULTANEOUSLY_REGISTERED_FOR_PVP_MATCHES_SUCH_AS_THE_OLYMPIAD_UNDERGROUND_COLISEUM_AERIAL_CLEFT_KRATEIS_CUBE_AND_HANDYS_BLOCK_CHECKERS);
				return;
			}
			
			if ((player.getParty() != null && player.getParty().getUCState() != null) || player.getUCState() > 0)
			{
				player.sendPacket(SystemMessageId.YOU_CANNOT_BE_SIMULTANEOUSLY_REGISTERED_FOR_PVP_MATCHES_SUCH_AS_THE_OLYMPIAD_UNDERGROUND_COLISEUM_AERIAL_CLEFT_KRATEIS_CUBE_AND_HANDYS_BLOCK_CHECKERS);
				return;
			}
			
			if (player.isCursedWeaponEquipped())
			{
				player.sendPacket(SystemMessageId.CANNOT_REGISTER_PROCESSING_CURSED_WEAPON);
				return;
			}

			if (AerialCleftEvent.getInstance().checkRegistration())
			{
				if (AerialCleftEvent.getInstance().isPlayerParticipant(player.getObjectId()))
				{
					showChatWindow(player, "data/html/aerialCleft/32518-02.htm");
					return;
				}
				
				if (AerialCleftEvent.getInstance().isValidRegistration())
				{
					if (AerialCleftEvent.getInstance().getTotalEventPlayers() < Config.CLEFT_MAX_PLAYERS)
					{
						AerialCleftEvent.getInstance().registerPlayer(player);
					}
					else
					{
						showChatWindow(player, "data/html/aerialCleft/32518-02.htm");
					}
				}
				else
				{
					showChatWindow(player, "data/html/aerialCleft/32518-05.htm");
				}
			}
			else
			{
				showChatWindow(player, "data/html/aerialCleft/32518-04.htm");
			}
		}
		else
		{
			super.onBypassFeedback(player, command);
		}
	}
}