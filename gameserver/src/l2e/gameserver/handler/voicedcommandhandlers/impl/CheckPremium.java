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

import java.util.ArrayList;
import java.util.List;

import l2e.gameserver.Config;
import l2e.gameserver.handler.voicedcommandhandlers.IVoicedCommandHandler;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.strings.server.ServerMessage;
import l2e.gameserver.model.strings.server.ServerStorage;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.CreatureSay;

public class CheckPremium implements IVoicedCommandHandler
{
	private static final String[] _voicedCommands =
	{
	        "premium", "premiumlist", "premiuminfo"
	};

	@Override
	public boolean useVoicedCommand(String command, Player activeChar, String target)
	{
		if (!Config.USE_PREMIUMSERVICE)
		{
			return false;
		}
		
		if (command.equals("premium") || command.equals("premiumlist"))
		{
			if (activeChar.isInParty())
			{
				final List<Player> premiums = new ArrayList<>();
				for (final Player player : activeChar.getParty().getMembers())
				{
					if (player != null && player.hasPremiumBonus())
					{
						premiums.add(player);
					}
				}
			
				if (!premiums.isEmpty())
				{
					int i = 0;
					final CreatureSay[] packets = new CreatureSay[premiums.size() + 2];
					packets[i++] = new CreatureSay(activeChar.getObjectId(), Say2.BATTLEFIELD, ServerStorage.getInstance().getString(activeChar.getLang(), "CheckPremium.PARTY"), ServerStorage.getInstance().getString(activeChar.getLang(), "CheckPremium.PREMIUM_LIST"));
					for (final Player player : premiums)
					{
						if (player != null)
						{
							final String rate = player.getPremiumBonus().getRateXp() > 1.0 ? "(+" + cutOff((player.getPremiumBonus().getRateXp() - 1) * 100, 0) + "%)" : "";
							final ServerMessage msg = player.getPremiumBonus().isPersonal() ? new ServerMessage("CheckPremium.PERSONAL", activeChar.getLang()) : new ServerMessage("CheckPremium.ACCOUNT", activeChar.getLang());
							msg.add(player.getName(null));
							packets[i++] = new CreatureSay(activeChar.getObjectId(), Say2.BATTLEFIELD, "" + (i - 1) + "", "" + msg.toString() + " " + rate);
						}
					}
					packets[i++] = new CreatureSay(activeChar.getObjectId(), Say2.BATTLEFIELD, ServerStorage.getInstance().getString(activeChar.getLang(), "CheckPremium.PARTY"), ServerStorage.getInstance().getString(activeChar.getLang(), "CheckPremium.PREMIUM_LIST"));
					activeChar.sendPacket(packets);
					return true;
				}
				else
				{
					activeChar.sendMessage((new ServerMessage("CheckPremium.EMPTY_LIST", activeChar.getLang())).toString());
					return true;
				}
			}
			else
			{
				activeChar.sendMessage((new ServerMessage("CheckPremium.NO_PARTY", activeChar.getLang())).toString());
				return true;
			}
		}
		else if (command.equals("premiuminfo"))
		{
			if (activeChar.isInParty())
			{
				int i = 0;
				final CreatureSay[] packets = new CreatureSay[9];
				packets[i++] = new CreatureSay(activeChar.getObjectId(), Say2.BATTLEFIELD, ServerStorage.getInstance().getString(activeChar.getLang(), "CheckPremium.PARTY"), ServerStorage.getInstance().getString(activeChar.getLang(), "CheckPremium.PREMIUM_BONUSES"));
				packets[i++] = new CreatureSay(activeChar.getObjectId(), Say2.BATTLEFIELD, "" + (i - 1) + "", "Rate Xp: +" + cutOff((activeChar.getParty().getRateXp() - 1) * 100, 0) + "%");
				packets[i++] = new CreatureSay(activeChar.getObjectId(), Say2.BATTLEFIELD, "" + (i - 1) + "", "Rate Sp: +" + cutOff((activeChar.getParty().getRateSp() - 1) * 100, 0) + "%");
				packets[i++] = new CreatureSay(activeChar.getObjectId(), Say2.BATTLEFIELD, "" + (i - 1) + "", "Rate Adena: +" + cutOff((activeChar.getParty().getDropAdena() - 1) * 100, 0) + "%");
				packets[i++] = new CreatureSay(activeChar.getObjectId(), Say2.BATTLEFIELD, "" + (i - 1) + "", "Rate Drop: +" + cutOff((activeChar.getParty().getDropItems() - 1) * 100, 0) + "%");
				packets[i++] = new CreatureSay(activeChar.getObjectId(), Say2.BATTLEFIELD, "" + (i - 1) + "", "Rate Spoil: +" + cutOff((activeChar.getParty().getDropSpoil() - 1) * 100, 0) + "%");
				packets[i++] = new CreatureSay(activeChar.getObjectId(), Say2.BATTLEFIELD, "" + (i - 1) + "", "Rate Raids: +" + cutOff((activeChar.getParty().getDropRaids() - 1) * 100, 0) + "%");
				packets[i++] = new CreatureSay(activeChar.getObjectId(), Say2.BATTLEFIELD, "" + (i - 1) + "", "Rate Epics: +" + cutOff((activeChar.getParty().getDropEpics() - 1) * 100, 0) + "%");
				packets[i++] = new CreatureSay(activeChar.getObjectId(), Say2.BATTLEFIELD, ServerStorage.getInstance().getString(activeChar.getLang(), "CheckPremium.PARTY"), ServerStorage.getInstance().getString(activeChar.getLang(), "CheckPremium.PREMIUM_BONUSES"));
				activeChar.sendPacket(packets);
				return true;
			}
			else
			{
				activeChar.sendMessage((new ServerMessage("CheckPremium.NO_PARTY", activeChar.getLang())).toString());
				return true;
			}
		}
		return true;
	}
	
	private static double cutOff(double num, int pow)
	{
		return (int) (num * Math.pow(10, pow)) / Math.pow(10, pow);
	}

	@Override
	public String[] getVoicedCommandList()
	{
		return _voicedCommands;
	}
}