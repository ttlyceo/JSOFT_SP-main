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
import l2e.gameserver.data.holder.CharNameHolder;
import l2e.gameserver.instancemanager.PunishmentManager;
import l2e.gameserver.model.CharSelectInfoPackage;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.punishment.PunishmentAffect;
import l2e.gameserver.model.punishment.PunishmentType;
import l2e.gameserver.network.GameClient;
import l2e.gameserver.network.GameClient.GameClientState;
import l2e.gameserver.network.serverpackets.CharacterSelected;
import l2e.gameserver.network.serverpackets.SSQInfo;
import l2e.gameserver.network.serverpackets.ServerClose;

public class RequestGameStart extends GameClientPacket
{
	private int _charSlot;
	protected int _unk1;
	protected int _unk2;
	protected int _unk3;
	protected int _unk4;
	
	@Override
	protected void readImpl()
	{
		_charSlot = readD();
		_unk1 = readH();
		_unk2 = readD();
		_unk3 = readD();
		_unk4 = readD();
	}

	@Override
	protected void runImpl()
	{
		final GameClient client = getClient();
		if (Config.SECOND_AUTH_ENABLED && !client.getSecondaryAuth().isAuthed())
		{
			client.getSecondaryAuth().openDialog();
			return;
		}

		if (client.getActiveCharLock().tryLock())
		{
			try
			{
				if (client.getActiveChar() == null)
				{
					final CharSelectInfoPackage info = client.getCharSelection(_charSlot);
					if (info == null)
					{
						return;
					}

					if (info.getAccessLevel() < 0)
					{
						client.close(ServerClose.STATIC_PACKET);
						return;
					}

					if (PunishmentManager.getInstance().checkPunishment(client, PunishmentType.BAN) || !checkSecurity(client))
					{
						client.close(ServerClose.STATIC_PACKET);
						return;
					}

					if (Config.DEBUG)
					{
						_log.info("selected slot:" + _charSlot);
					}

					final Player cha = client.loadCharFromDisk(_charSlot);
					if (cha == null)
					{
						return;
					}
					
					cha.setClient(client);
					client.setActiveChar(cha);
					
					cha.setCacheIp(client.getRealIpAddress());
					cha.setCacheHwid(client.getHWID());
					
					cha.setOnlineStatus(true, true);
					CharNameHolder.getInstance().addName(cha);
					sendPacket(new SSQInfo());
					
					if (PunishmentManager.getInstance().checkPunishment(client, PunishmentType.BAN, PunishmentAffect.CHARACTER) || PunishmentManager.getInstance().checkPunishment(client, PunishmentType.BAN, PunishmentAffect.ACCOUNT) || PunishmentManager.getInstance().checkPunishment(client, PunishmentType.BAN, PunishmentAffect.IP) || PunishmentManager.getInstance().checkPunishment(client, PunishmentType.BAN, PunishmentAffect.HWID))
					{
						client.close(ServerClose.STATIC_PACKET);
						return;
					}
					
					client.setState(GameClientState.ENTERING);
					final CharacterSelected cs = new CharacterSelected(cha, client.getSessionId().playOkID1);
					sendPacket(cs);
				}
			}
			finally
			{
				client.getActiveCharLock().unlock();
			}
			Log.addLogGame("LOG IN:", "logged into the game!", client.getActiveChar().getName(null));
		}
	}
	
	private boolean checkSecurity(GameClient client)
	{
		if (Config.ALLOW_PRE_START_SYSTEM && Config.PRE_START_PATTERN > 0)
		{
			if (Config.PRE_START_PATTERN > System.currentTimeMillis())
			{
				return false;
			}
		}
		
		if (Config.ALLOW_HWID_LOCK && !client.getLockedHwid().isEmpty() && !Config.PROTECTION.equalsIgnoreCase("NONE"))
		{
			if (!client.getLockedHwid().equalsIgnoreCase(client.getHWID()))
			{
				return false;
			}
		}
		return true;
	}
}