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
package l2e.gameserver.network.clientpackets;

import l2e.commons.log.Log;
import l2e.gameserver.Config;
import l2e.gameserver.SevenSignsFestival;
import l2e.gameserver.instancemanager.ZoneManager;
import l2e.gameserver.model.Party;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.entity.events.cleft.AerialCleftEvent;
import l2e.gameserver.model.entity.events.tournaments.TournamentData;
import l2e.gameserver.model.zone.ZoneId;
import l2e.gameserver.model.zone.type.FunPvpZone;
import l2e.gameserver.network.GameClient.GameClientState;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.CharacterSelectionInfo;
import l2e.gameserver.network.serverpackets.RestartResponse;
import l2e.gameserver.taskmanager.AttackStanceTaskManager;

public final class RequestRestart extends GameClientPacket
{
	@Override
	protected void readImpl()
	{
	}

	@Override
	protected void runImpl()
	{
		final Player player = getClient().getActiveChar();
		
		if (player == null)
		{
			return;
		}
		
		if (((AerialCleftEvent.getInstance().isStarted() || AerialCleftEvent.getInstance().isRewarding()) && AerialCleftEvent.getInstance().isPlayerParticipant(player.getObjectId())))
		{
			sendPacket(RestartResponse.valueOf(false));
			return;
		}

		if (player.checkInTournament())
		{
			player.sendMessage("You can not restart when you registering in Tournament Event.");
			return;
		}

		if ((player.getActiveEnchantItemId() != Player.ID_NONE) || (player.getActiveEnchantAttrItemId() != Player.ID_NONE))
		{
			sendPacket(RestartResponse.valueOf(false));
			return;
		}
		
		if (player.isLocked())
		{
			_log.warn("Player " + player.getName(null) + " tried to restart during class change.");
			sendPacket(RestartResponse.valueOf(false));
			return;
		}
		
		if (player.getPrivateStoreType() != Player.STORE_PRIVATE_NONE)
		{
			player.sendMessage("Cannot restart while trading");
			sendPacket(RestartResponse.valueOf(false));
			return;
		}

		if (player.isInsideZone(ZoneId.FUN_PVP))
		{
			final FunPvpZone zone = ZoneManager.getInstance().getZone(player, FunPvpZone.class);
			if (zone != null && zone.isNoRestartZone())
			{
				player.sendMessage("You cannot restart while inside at this zone.");
				sendPacket(RestartResponse.valueOf(false));
				return;
			}
		}
		
		if (AttackStanceTaskManager.getInstance().hasAttackStanceTask(player) && !(player.isGM() && player.getAccessLevel().allowRestartFighting()))
		{
			if (Config.DEBUG)
			{
				_log.info("Player " + player.getName(null) + " tried to logout while fighting.");
			}
			
			player.sendPacket(SystemMessageId.CANT_RESTART_WHILE_FIGHTING);
			sendPacket(RestartResponse.valueOf(false));
			return;
		}
		
		if (player.isBlocked())
		{
			player.sendMessage("You are blocked!");
			player.sendPacket(RestartResponse.valueOf(false));
			return;
		}
		
		if (player.isInFightEvent())
		{
			player.sendMessage("You need to leave Fight Event first!");
			sendPacket(RestartResponse.valueOf(false));
			return;
		}
		
		if (player.isFestivalParticipant())
		{
			if (SevenSignsFestival.getInstance().isFestivalInitialized())
			{
				player.sendMessage("You cannot restart while you are a participant in a festival.");
				sendPacket(RestartResponse.valueOf(false));
				return;
			}
			
			final Party playerParty = player.getParty();
			
			if (playerParty != null)
			{
				player.getParty().broadcastString(player.getName(null) + " has been removed from the upcoming festival.");
			}
		}
		
		if (player.isInOlympiadMode() && !Config.ALLOW_RESTART_AT_OLY)
		{
			player.sendMessage("You cannot restart while you are a participant in olympiad!");
			sendPacket(RestartResponse.valueOf(false));
			return;
		}
		
		player.removeFromBossZone();
		
		Log.addLogGame("RESTART:", "Logged out!", player.getName(null));
		
		if (getClient() != null)
		{
			getClient().setState(GameClientState.AUTHED);
		}
		
		player.restart();
		
		sendPacket(RestartResponse.valueOf(true));
		final CharacterSelectionInfo cl = new CharacterSelectionInfo(getClient().getLogin(), getClient().getSessionId().playOkID1);
		sendPacket(cl);
		getClient().setCharSelection(cl.getCharInfo());
	}
}