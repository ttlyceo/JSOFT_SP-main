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

import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.instancemanager.games.krateiscube.KrateisCubeManager;
import l2e.gameserver.instancemanager.games.krateiscube.model.Arena;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.Summon;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.entity.events.cleft.AerialCleftEvent;
import l2e.gameserver.model.olympiad.OlympiadManager;
import l2e.gameserver.network.SystemMessageId;

/**
 * Rework by LordWinter
 */
public class KrateisCubeManagerInstance extends NpcInstance
{
	public KrateisCubeManagerInstance(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public void onBypassFeedback(Player player, String command)
	{
		if (command.startsWith("Register"))
		{
			if ((player.getInventoryLimit() * 0.8) <= player.getInventory().getSize())
			{
				player.sendPacket(SystemMessageId.INVENTORY_LESS_THAN_80_PERCENT);
				showChatWindow(player, "data/html/krateisCube/32503-08.htm");
				return;
			}
			
			if (((AerialCleftEvent.getInstance().isStarted() || AerialCleftEvent.getInstance().isRewarding()) && AerialCleftEvent.getInstance().isPlayerParticipant(player.getObjectId())) || OlympiadManager.getInstance().isRegistered(player) || player.isInOlympiadMode() || player.isInFightEvent() || player.isRegisteredInFightEvent() || player.getTeam() != 0)
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

			final int id = Integer.parseInt(command.substring(9, 10).trim());
			final Arena arena = KrateisCubeManager.getInstance().getArenaId(id);
			if (arena != null)
			{
				if (player.getLevel() < arena.getMinLevel() || player.getLevel() > arena.getMaxLevel())
				{
					showChatWindow(player, "data/html/krateisCube/32503-06.htm");
					return;
				}
			}
			else
			{
				showChatWindow(player, "data/html/krateisCube/32503-09.htm");
				return;
			}

			if (KrateisCubeManager.getInstance().isRegisterTime())
			{
				if (arena.addRegisterPlayer(player))
				{
					showChatWindow(player, "data/html/krateisCube/32503-03.htm");
				}
				else
				{
					showChatWindow(player, "data/html/krateisCube/32503-04.htm");
				}
			}
			else
			{
				showChatWindow(player, "data/html/krateisCube/32503-07.htm");
			}
		}
		else if (command.startsWith("SeeList"))
		{
			if (player.getLevel() < 70)
			{
				showChatWindow(player, "data/html/krateisCube/32503-09.htm");
			}
			else
			{
				showChatWindow(player, "data/html/krateisCube/32503-02.htm");
			}
		}
		else if (command.startsWith("Cancel"))
		{
			for (final Arena arena : KrateisCubeManager.getInstance().getArenas().values())
			{
				if (arena != null && arena.removePlayer(player))
				{
					showChatWindow(player, "data/html/krateisCube/32503-05.htm");
					break;
				}
			}
		}
		else if (command.startsWith("TeleportToFI"))
		{
			player.teleToLocation(-59193, -56893, -2034, true, ReflectionManager.DEFAULT);
			final Summon pet = player.getSummon();
			if (pet != null)
			{
				pet.teleToLocation(-59193, -56893, -2034, true, ReflectionManager.DEFAULT);
			}
			return;
		}
		else
		{
			super.onBypassFeedback(player, command);
		}
	}

	@Override
	public String getHtmlPath(int npcId, int val)
	{
		String pom = "";

		if (val == 0)
		{
			pom = "" + npcId;
		}
		else
		{
			pom = npcId + "-0" + val;
		}

		return "data/html/krateisCube/" + pom + ".htm";
	}
}