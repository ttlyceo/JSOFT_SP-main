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
import l2e.gameserver.instancemanager.CastleManorManager;
import l2e.gameserver.model.Clan;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.CastleChamberlainInstance;
import l2e.gameserver.model.actor.instance.ManorManagerInstance;
import l2e.gameserver.model.actor.instance.MerchantInstance;
import l2e.gameserver.model.entity.Castle;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.BuyListSeed;
import l2e.gameserver.network.serverpackets.ExShowCropInfo;
import l2e.gameserver.network.serverpackets.ExShowCropSetting;
import l2e.gameserver.network.serverpackets.ExShowManorDefaultInfo;
import l2e.gameserver.network.serverpackets.ExShowProcureCropDetail;
import l2e.gameserver.network.serverpackets.ExShowSeedInfo;
import l2e.gameserver.network.serverpackets.ExShowSeedSetting;
import l2e.gameserver.network.serverpackets.ExShowSellCropList;
import l2e.gameserver.network.serverpackets.SystemMessage;

public class ManorManager implements IBypassHandler
{
	private static final String[] COMMANDS =
	{
	        "manor_menu_select"
	};
	
	@Override
	public boolean useBypass(String command, Player activeChar, Creature target)
	{
		final Npc manager = activeChar.getLastFolkNPC();
		final boolean isCastle = manager instanceof CastleChamberlainInstance;
		if (!((manager instanceof ManorManagerInstance) || isCastle))
		{
			return false;
		}

		if (!activeChar.isInsideRadius(manager, Npc.INTERACTION_DISTANCE, true, false))
		{
			return false;
		}

		try
		{
			final Castle castle = manager.getCastle();
			if (isCastle)
			{
				if ((activeChar.getClan() == null) || (castle.getOwnerId() != activeChar.getClanId()) || ((activeChar.getClanPrivileges() & Clan.CP_CS_MANOR_ADMIN) != Clan.CP_CS_MANOR_ADMIN))
				{
					manager.showChatWindow(activeChar, "data/html/chamberlain/chamberlain-noprivs.htm");
					return false;
				}
				if (castle.getSiege().getIsInProgress())
				{
					manager.showChatWindow(activeChar, "data/html/chamberlain/chamberlain-busy.htm");
					return false;
				}
			}

			if (CastleManorManager.getInstance().isUnderMaintenance())
			{
				activeChar.sendActionFailed();
				activeChar.sendPacket(SystemMessageId.THE_MANOR_SYSTEM_IS_CURRENTLY_UNDER_MAINTENANCE);
				return true;
			}

			final StringTokenizer st = new StringTokenizer(command, "&");
			final int ask = Integer.parseInt(st.nextToken().split("=")[1]);
			final int state = Integer.parseInt(st.nextToken().split("=")[1]);
			final boolean isNextPeriod = st.nextToken().split("=")[1].equals("1");

			final int castleId;
			if (state < 0)
			{
				castleId = castle.getId();
			}
			else
			{
				castleId = state;
			}

			switch (ask)
			{
				case 1 :
					if (isCastle)
					{
						break;
					}
					if (castleId != castle.getId())
					{
						final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.HERE_YOU_CAN_BUY_ONLY_SEEDS_OF_S1_MANOR);
						sm.addString(manager.getCastle().getName(activeChar.getLang()));
						activeChar.sendPacket(sm);
					}
					else
					{
						activeChar.sendPacket(new BuyListSeed(activeChar.getAdena(), castleId));
					}
					break;
				case 2 :
					if (isCastle)
					{
						break;
					}
					activeChar.sendPacket(new ExShowSellCropList(activeChar, castleId));
					break;
				case 3 :
					activeChar.sendPacket(new ExShowSeedInfo(castleId, isNextPeriod, false));
					break;
				case 4 :
					activeChar.sendPacket(new ExShowCropInfo(castleId, isNextPeriod, false));
					break;
				case 5 :
					activeChar.sendPacket(new ExShowManorDefaultInfo(false));
					break;
				case 6 :
					if (isCastle)
					{
						break;
					}
					((MerchantInstance) manager).showBuyWindow(activeChar, 300000 + manager.getId());
					break;
				case 7 :
					if (!isCastle)
					{
						break;
					}
					if (CastleManorManager.getInstance().isManorApproved())
					{
						activeChar.sendPacket(SystemMessageId.A_MANOR_CANNOT_BE_SET_UP_BETWEEN_6_AM_AND_8_PM);
					}
					else
					{
						activeChar.sendPacket(new ExShowSeedSetting(castle.getId()));
					}
					break;
				case 8 :
					if (!isCastle)
					{
						break;
					}
					if (CastleManorManager.getInstance().isManorApproved())
					{
						activeChar.sendPacket(SystemMessageId.A_MANOR_CANNOT_BE_SET_UP_BETWEEN_6_AM_AND_8_PM);
					}
					else
					{
						activeChar.sendPacket(new ExShowCropSetting(castle.getId()));
					}
					break;
				case 9 :
					if (isCastle)
					{
						break;
					}
					activeChar.sendPacket(new ExShowProcureCropDetail(state));
					break;
				default :
					return false;
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
	public String[] getBypassList()
	{
		return COMMANDS;
	}
}