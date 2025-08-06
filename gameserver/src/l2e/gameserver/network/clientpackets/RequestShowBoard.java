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

import l2e.gameserver.Config;
import l2e.gameserver.handler.communityhandlers.CommunityBoardHandler;
import l2e.gameserver.handler.communityhandlers.ICommunityBoardHandler;
import l2e.gameserver.instancemanager.ZoneManager;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.entity.events.cleft.AerialCleftEvent;
import l2e.gameserver.model.strings.server.ServerMessage;
import l2e.gameserver.model.zone.ZoneId;
import l2e.gameserver.model.zone.type.FunPvpZone;
import l2e.gameserver.network.SystemMessageId;

public final class RequestShowBoard extends GameClientPacket
{
	protected int _unknown;
	
	@Override
	protected final void readImpl()
	{
		_unknown = readD();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getClient().getActiveChar();
		if (player == null)
		{
			return;
		}
		
		if (player.isBlocked() || player.isCursedWeaponEquipped() || player.isInDuel() || player.isFlying() || player.isJailed() || player.isInOlympiadMode() || player.inObserverMode() || player.isAlikeDead() || player.isInSiege() || player.isDead())
		{
			player.sendMessage((new ServerMessage("Community.ALL_DISABLE", player.getLang())).toString());
			return;
		}
		
		if (((AerialCleftEvent.getInstance().isStarted() || AerialCleftEvent.getInstance().isRewarding()) && AerialCleftEvent.getInstance().isPlayerParticipant(player.getObjectId())))
		{
			player.sendMessage((new ServerMessage("Community.ALL_DISABLE", player.getLang())).toString());
			return;
		}
		if (player.isInsideZone(ZoneId.PVP) && !player.isInFightEvent())
		{
			if (player.isInsideZone(ZoneId.FUN_PVP))
			{
				final FunPvpZone zone = ZoneManager.getInstance().getZone(player, FunPvpZone.class);
				if (zone != null)
				{
					if (!zone.canUseCbBuffs() && !zone.canUseCbTeleports())
					{
						player.sendMessage((new ServerMessage("Community.ALL_DISABLE", player.getLang())).toString());
						return;
					}
				}
			}
			else
			{
				player.sendMessage((new ServerMessage("Community.ALL_DISABLE", player.getLang())).toString());
				return;
			}
		}
		if (player.isInCombat() && !player.isInFightEvent() && !player.isInsideZone(ZoneId.FUN_PVP))
		{
			player.sendMessage((new ServerMessage("Community.ALL_DISABLE", player.getLang())).toString());
			return;
		}
		
		if (player.isCastingNow() && !player.isInFightEvent() && !player.isInsideZone(ZoneId.FUN_PVP))
		{
			player.sendMessage((new ServerMessage("Community.ALL_DISABLE", player.getLang())).toString());
			return;
		}
		if (player.isAttackingNow() && !player.isInFightEvent() && !player.isInsideZone(ZoneId.FUN_PVP))
		{
			player.sendMessage((new ServerMessage("Community.ALL_DISABLE", player.getLang())).toString());
			return;
		}
		
		if (Config.ALLOW_COMMUNITY_PEACE_ZONE)
		{
			if (!player.isInsideZone(ZoneId.PEACE) && !player.isInFightEvent())
			{
				player.sendMessage((new ServerMessage("Community.ALL_DISABLE", player.getLang())).toString());
				return;
			}
		}
		
		player.isntAfk();

		if (Config.ALLOW_COMMUNITY)
		{
			final ICommunityBoardHandler handler = CommunityBoardHandler.getInstance().getHandler(Config.BBS_HOME_PAGE);
			if (handler != null)
			{
				handler.onBypassCommand(Config.BBS_HOME_PAGE, player);
			}
		}
		else
		{
			player.sendPacket(SystemMessageId.CB_OFFLINE);
		}
	}
	
	@Override
	protected boolean triggersOnActionRequest()
	{
		return false;
	}
}