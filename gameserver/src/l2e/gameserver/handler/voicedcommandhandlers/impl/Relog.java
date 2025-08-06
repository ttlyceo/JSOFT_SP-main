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
import l2e.gameserver.SevenSignsFestival;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.data.holder.CharNameHolder;
import l2e.gameserver.handler.voicedcommandhandlers.IVoicedCommandHandler;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.instancemanager.ZoneManager;
import l2e.gameserver.model.Party;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.zone.ZoneId;
import l2e.gameserver.model.zone.type.FunPvpZone;
import l2e.gameserver.network.GameClient;
import l2e.gameserver.network.GameClient.GameClientState;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.CharacterSelected;
import l2e.gameserver.network.serverpackets.CharacterSelectionInfo;
import l2e.gameserver.network.serverpackets.RestartResponse;
import l2e.gameserver.network.serverpackets.SSQInfo;
import l2e.gameserver.taskmanager.AttackStanceTaskManager;

public class Relog implements IVoicedCommandHandler
{
	private static String[] _voicedCommands =
	{
	        "relog", "restart"
	};
	
	@Override
	public boolean useVoicedCommand(String command, Player player, String target)
	{
		if ((command.equals("relog")) || (command.equals("restart")))
		{
			if (player == null || !Config.ALLOW_RELOG_COMMAND)
			{
				return false;
			}
			
			if (!player.checkFloodProtection("REPAIR", "repair_delay"))
			{
				return false;
			}
			
			if ((player.getActiveEnchantItemId() != Player.ID_NONE) || (player.getActiveEnchantAttrItemId() != Player.ID_NONE) || player.isLocked())
			{
				return false;
			}
			
			if (player.getPrivateStoreType() != Player.STORE_PRIVATE_NONE)
			{
				player.sendMessage("Cannot restart while trading");
				return false;
			}

			if (player.isInsideZone(ZoneId.FUN_PVP))
			{
				final FunPvpZone zone = ZoneManager.getInstance().getZone(player, FunPvpZone.class);
				if (zone != null && zone.isNoRestartZone())
				{
					player.sendMessage("You cannot restart while inside at this zone.");
					return false;
				}
			}
			
			if (AttackStanceTaskManager.getInstance().hasAttackStanceTask(player) && !(player.isGM() && player.getAccessLevel().allowRestartFighting()))
			{
				player.sendPacket(SystemMessageId.CANT_RESTART_WHILE_FIGHTING);
				return false;
			}
			
			if (player.isBlocked())
			{
				player.sendMessage("You are blocked!");
				return false;
			}
			
			if (player.isInFightEvent())
			{
				player.sendMessage("You need to leave Fight Event first!");
				return false;
			}

			if (player.checkInTournament())
			{
				player.sendMessage("You need to leave Tournament first!");
				return false;
			}
			
			if (player.isFestivalParticipant())
			{
				if (SevenSignsFestival.getInstance().isFestivalInitialized())
				{
					player.sendMessage("You cannot restart while you are a participant in a festival.");
					return false;
				}
				
				final Party playerParty = player.getParty();
				if (playerParty != null)
				{
					player.getParty().broadcastString(player.getName(null) + " has been removed from the upcoming festival.");
				}
			}
			
			player.removeFromBossZone();
	       
			final GameClient client = player.getClient();
			if (client != null && !client.isDetached())
			{
				player.setReflection(ReflectionManager.DEFAULT);
				final int objId = player.getObjectId();
				client.setState(GameClientState.AUTHED);
				player.restart();
				client.sendPacket(RestartResponse.valueOf(true));
				final CharacterSelectionInfo cl = new CharacterSelectionInfo(client.getLogin(), client.getSessionId().playOkID1);
				client.sendPacket(cl);
				client.setCharSelection(cl.getCharInfo());
				ThreadPoolManager.getInstance().schedule(new RelogTask(objId, client), 333);
				return true;
			}
		}
		return false;
	}
	
	private class RelogTask implements Runnable
	{
		private final int _objId;
		private final GameClient _client;
		
		public RelogTask(int objId, GameClient client)
		{
			_objId = objId;
			_client = client;
		}
		
		@Override
		public void run()
		{
			if ((_client == null) || (_client.isDetached()))
			{
				return;
			}
			
			if (Config.SECOND_AUTH_ENABLED && !_client.getSecondaryAuth().isAuthed())
			{
				return;
			}
			
			final int slotIdx = _client.getSlotForObjectId(_objId);
			if (slotIdx < 0)
			{
				return;
			}
			
			final Player activeChar = _client.loadCharFromDisk(slotIdx);
			if (activeChar == null)
			{
				return;
			}
			activeChar.setClient(_client);
			_client.setActiveChar(activeChar);
			
			activeChar.setCacheIp(_client.getRealIpAddress());
			activeChar.setCacheHwid(_client.getHWID());
			
			activeChar.setOnlineStatus(true, true);
			CharNameHolder.getInstance().addName(activeChar);
			activeChar.sendPacket(new SSQInfo());
			
			_client.setState(GameClientState.ENTERING);
			_client.sendPacket(new CharacterSelected(activeChar, _client.getSessionId().playOkID1));
		}
	}

	@Override
	public String[] getVoicedCommandList()
	{
		return _voicedCommands;
	}
}