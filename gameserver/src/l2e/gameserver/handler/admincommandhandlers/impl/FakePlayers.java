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
package l2e.gameserver.handler.admincommandhandlers.impl;

import l2e.fake.FakePlayer;
import l2e.fake.FakePlayerManager;
import l2e.fake.ai.EnchanterAI;
import l2e.gameserver.Config;
import l2e.gameserver.data.parser.FakeLocationParser;
import l2e.gameserver.handler.admincommandhandlers.IAdminCommandHandler;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;

public class FakePlayers implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
	        "admin_takecontrol", "admin_releasecontrol", "admin_fakes", "admin_spawnrandom", "admin_deletefake", "admin_spawnenchanter", "admin_autofakes"
	};

	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}

	private void fakeMenu(Player activeChar)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile(activeChar, activeChar.getLang(), "data/html/admin/fakeplayers/index.htm");
		html.replace("%fakecount%", FakePlayerManager.getInstance().getFakePlayers().size());
		activeChar.sendPacket(html);
	}

	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		if (command.startsWith("admin_fakes"))
		{
			fakeMenu(activeChar);
		}
		else if (command.startsWith("admin_deletefake"))
		{
			if (activeChar.getTarget() != null && activeChar.getTarget() instanceof FakePlayer)
			{
				final FakePlayer fakePlayer = (FakePlayer) activeChar.getTarget();
				if (FakePlayerManager.getInstance().removeFakePlayers(fakePlayer))
				{
					fakePlayer.kick();
				}
			}
			return true;
		}
		else if (command.startsWith("admin_autofakes"))
		{
			FakePlayerManager.getInstance().spawnPlayer();
			if (command.contains(" "))
			{
				final String arg = command.split(" ")[1];
				if (arg.equalsIgnoreCase("htm"))
				{
					fakeMenu(activeChar);
				}
			}
		}
		else if (command.startsWith("admin_spawnenchanter"))
		{
			if (FakePlayerManager.getInstance().getFakePlayers().size() >= Config.FAKE_PLAYERS_AMOUNT)
			{
				activeChar.sendMessage("You reach limit fake players!");
				fakeMenu(activeChar);
				return false;
			}
			
			final FakePlayer fakePlayer = FakePlayerManager.getInstance().spawnRndPlayer(FakeLocationParser.getInstance().createRndLoc(Location.findPointToStay(activeChar, activeChar.getLocation(), 0, 60, true)));
			if (fakePlayer != null)
			{
				fakePlayer.setFakeAi(new EnchanterAI(fakePlayer));
				FakePlayerManager.getInstance().addFakePlayers(fakePlayer);
			}
			fakeMenu(activeChar);
		}
		else if (command.startsWith("admin_spawnrandom"))
		{
			if (FakePlayerManager.getInstance().getFakePlayers().size() >= Config.FAKE_PLAYERS_AMOUNT)
			{
				activeChar.sendMessage("You reach limit fake players!");
				fakeMenu(activeChar);
				return false;
			}
			
			final FakePlayer fakePlayer = FakePlayerManager.getInstance().spawnRndPlayer(FakeLocationParser.getInstance().createRndLoc(Location.findPointToStay(activeChar, activeChar.getLocation(), 0, 60, true)));
			if (fakePlayer != null)
			{
				fakePlayer.assignDefaultAI(false);
				FakePlayerManager.getInstance().addFakePlayers(fakePlayer);
				if (command.contains(" "))
				{
					final String arg = command.split(" ")[1];
					if (arg.equalsIgnoreCase("htm"))
					{
						fakeMenu(activeChar);
					}
				}
			}
			return true;
		}
		else if (command.startsWith("admin_takecontrol"))
		{
			if (activeChar.getTarget() != null && activeChar.getTarget() instanceof FakePlayer)
			{
				final FakePlayer fakePlayer = (FakePlayer) activeChar.getTarget();
				fakePlayer.setUnderControl(true);
				activeChar.setPlayerUnderControl(fakePlayer);
				activeChar.sendMessage("You are now controlling: " + fakePlayer.getName(null));
				return true;
			}
			activeChar.sendMessage("You can only take control of a Fake Player");
		}
		else if (command.startsWith("admin_releasecontrol"))
		{
			if (activeChar.isControllingFakePlayer())
			{
				final FakePlayer fakePlayer = activeChar.getPlayerUnderControl();
				activeChar.sendMessage("You are no longer controlling: " + fakePlayer.getName(null));
				fakePlayer.setUnderControl(false);
				activeChar.setPlayerUnderControl(null);
				return true;
			}
			activeChar.sendMessage("You are not controlling a Fake Player");
		}
		return true;
	}
}