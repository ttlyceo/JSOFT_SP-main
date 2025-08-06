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

import l2e.gameserver.network.GameClient;
import l2e.gameserver.network.serverpackets.VersionCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.jsoft.jguard.JGuard;
import top.jsoft.jguard.JGuardConfig;
import top.jsoft.jguard.manager.HWIDBanManager;

import java.nio.BufferUnderflowException;

public class ReplyGameGuardQuery extends GameClientPacket
{
	private static final Logger LOGGER = LoggerFactory.getLogger(ReplyGameGuardQuery.class);
	private static final int revisionNumber = JGuardConfig.JGUARD_REVISION_NUMBER;

	private int code;
	private int section;
	private int index;
	private int revision;
	private String procBuf;
	private int _dx;

	@Override
	protected void readImpl()
	{
		if(JGuard.isProtectEnabled()) {
			code = readD();
			section = readD();
			index = readD();
			revision = readD();
			if (code == 0x01) {
				try {
					procBuf = readS();
				} catch (BufferUnderflowException e) {

				}
			}
		}
		else
			_dx = readC();
	}

	@Override
	protected void runImpl()
	{
		final GameClient client = getClient();
		if (client == null) {
			return;
		}

		if(!JGuard.isProtectEnabled()) {
			if (_dx == 0)
			{
				client.setGameGuardOk(true);
			}
			else
			{
				client.setGameGuardOk(false);
			}
			return;
		}

		if(JGuardConfig.JGUARD_DEBUG)
			LOGGER.info(String.format("Account: %s, code(%d), section(%d), index(%d), revision(%d)", client.getLogin(), code, section, index, revision));

		if (revisionNumber != -1 && revision != revisionNumber) {
			LOGGER.warn("[JGuard] Client: " + client.getIPAddress() + " requested not correct revision from protect. Current revision: " + JGuardConfig.JGUARD_REVISION_NUMBER + " Client revision: " + revision);
			client.close(new VersionCheck(null));
			return;
		}

		final JGuard.GameGuardResponse gg = JGuard.GameGuardResponse.values()[code];

		switch (gg) {
			case NONE:
				client.setGameGuardOk(false);
				LOGGER.warn("Game Guard response NONE. Check this. Maybe hucked client(bot, antiGuard, etc)");
				break;
			case NORMAL_RESPONSE:
				client.setGameGuardOk(true);
				//client.sendPacket(new NetPing(client.getAccountId()));
				if (procBuf != null && !procBuf.isEmpty()) {
					JGuard.getInstance().storeProcBufInfo(client.getLogin(), procBuf);
				}
				//if(client.getActiveChar() != null)
				//	client.sendPacket(new DataPacket(client.getActiveChar().getName(null)));
				break;
			case KICK_RESPONSE:
				client.setGameGuardOk(false);
				HWIDBanManager.getInstance().systemBan(client, JGuard.GameGuardResponse.KICK_RESPONSE.toString(), HWIDBanManager.BanType.NONE);
				break;
			case USED_BOT_RESPONS:
				client.setGameGuardOk(false);
				//final BotResponse bot = BotResponse.values()[section];
				//Client senede info from bot detection. Value in BotResponse enume. Use bot enum, and ban/kick him :)
				HWIDBanManager.getInstance().systemBan(client, JGuard.GameGuardResponse.USED_BOT_RESPONS.toString(), HWIDBanManager.BanType.PLAYER_BAN);
				break;
			case GET_SN_IS_FALSE_RESPONSE:
				client.setGameGuardOk(false);
				HWIDBanManager.getInstance().systemBan(client, JGuard.GameGuardResponse.GET_SN_IS_FALSE_RESPONSE.toString(), HWIDBanManager.BanType.ACCOUNT_BAN);
				break;
			case SN_NULL_LENGHT_RESPONSE:
				client.setGameGuardOk(false);
				HWIDBanManager.getInstance().systemBan(client, JGuard.GameGuardResponse.SN_NULL_LENGHT_RESPONSE.toString(), HWIDBanManager.BanType.ACCOUNT_BAN);
				break;
			case SP_OBJECT_CHANGED_RESPONSE:
				client.setGameGuardOk(false);
				LOGGER.warn("Game Guard response SP_OBJECT_CHANGED_RESPONSE. Check this. Maybe hucked client(bot, antiGuard, etc)");
				HWIDBanManager.getInstance().systemBan(client, JGuard.GameGuardResponse.SP_OBJECT_CHANGED_RESPONSE.toString(), HWIDBanManager.BanType.ACCOUNT_BAN);
				break;
			case REQUEST_REVISION_VALIDATE:
				client.setGameGuardOk(false);
				break;
			case NOT_VALID_HOST_INFO:
				client.setGameGuardOk(false);
				LOGGER.warn("[JGuard] Client: " + client.getRealIpAddress() + " changed host info.");
				client.close(new VersionCheck(null));
				break;
		}
	}

	@Override
	protected boolean triggersOnActionRequest()
	{
		return false;
	}
}