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
import l2e.gameserver.handler.voicedcommandhandlers.IVoicedCommandHandler;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.items.instance.ItemInstance;

public class AncientAdenaExchanger implements IVoicedCommandHandler
{
	private static final String[] commands =
	{
	        "aa", "makeaa"
	};
	
	private static final int ANCIENT_ADENA = 5575;
	private static final int BLUE_SEAL_STONE = 6360;
	private static final int GREEN_SEAL_STONE = 6361;
	private static final int RED_SEAL_STONE = 6362;

	@Override
	public boolean useVoicedCommand(String command, Player activeChar, String params)
	{
		if (!Config.ALLOW_ANCIENT_EXCHANGER_COMMAND)
		{
			return false;
		}
		
		if (command.equalsIgnoreCase("aa") || command.equalsIgnoreCase("makeaa"))
		{
			final ItemInstance redStones = activeChar.getInventory().getItemByItemId(RED_SEAL_STONE);
			final ItemInstance greenStones = activeChar.getInventory().getItemByItemId(GREEN_SEAL_STONE);
			final ItemInstance blueStones = activeChar.getInventory().getItemByItemId(BLUE_SEAL_STONE);
			int count = 0;
			int aa = 0;

			if (redStones == null && greenStones == null && blueStones == null)
			{
				activeChar.sendMessage("You do not have any seal stones to exchange.");
				return false;
			}
			
			if (redStones != null)
			{
				count += redStones.getCount();
				aa += redStones.getCount() * 10;
				activeChar.destroyItem("AncientAdenaExchanger", redStones, null, true);
			}
			if (greenStones != null)
			{
				count += greenStones.getCount();
				aa += greenStones.getCount() * 5;
				activeChar.destroyItem("AncientAdenaExchanger", greenStones, null, true);
			}
			if (blueStones != null)
			{
				count += blueStones.getCount();
				aa += blueStones.getCount() * 3;
				activeChar.destroyItem("AncientAdenaExchanger", blueStones, null, true);
			}
			activeChar.addItem("AncientAdenaExchanger", ANCIENT_ADENA, aa, activeChar, true);
			activeChar.sendMessage("You have successfully exchanged " + count + " seal stones!");
		}
		return true;
	}

	@Override
	public String[] getVoicedCommandList()
	{
		return commands;
	}
}