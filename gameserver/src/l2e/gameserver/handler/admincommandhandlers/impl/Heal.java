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

import l2e.gameserver.Config;
import l2e.gameserver.handler.admincommandhandlers.IAdminCommandHandler;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.network.SystemMessageId;

public class Heal implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
	        "admin_heal"
	};

	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		
		if (command.equals("admin_heal"))
		{
			handleHeal(activeChar);
		}
		else if (command.startsWith("admin_heal"))
		{
			try
			{
				final String healTarget = command.substring(11);
				handleHeal(activeChar, healTarget);
			}
			catch (final StringIndexOutOfBoundsException e)
			{
				if (Config.DEVELOPER)
				{
					_log.warn("Heal error: " + e);
				}
				activeChar.sendMessage("Incorrect target/radius specified.");
			}
		}
		return true;
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
	
	private void handleHeal(Player activeChar)
	{
		handleHeal(activeChar, null);
	}
	
	private void handleHeal(Player activeChar, String player)
	{
		
		GameObject obj = activeChar.getTarget();
		if (player != null)
		{
			final Player plyr = GameObjectsStorage.getPlayer(player);
			
			if (plyr != null)
			{
				obj = plyr;
			}
			else
			{
				try
				{
					final int radius = Integer.parseInt(player);
					for (final Creature character : World.getInstance().getAroundCharacters(activeChar))
					{
						character.setCurrentHpMp(character.getMaxHp(), character.getMaxMp());
						if (character.isPlayer())
						{
							character.setCurrentCp(character.getMaxCp());
						}
					}

					activeChar.sendMessage("Healed within " + radius + " unit radius.");
					return;
				}
				catch (final NumberFormatException nbe)
				{}
			}
		}
		if (obj == null)
		{
			obj = activeChar;
		}
		if (obj instanceof Creature)
		{
			final Creature target = (Creature) obj;
			target.setCurrentHpMp(target.getMaxHp(), target.getMaxMp());
			if (target instanceof Player)
			{
				target.setCurrentCp(target.getMaxCp());
			}
			if (Config.DEBUG)
			{
				_log.info("GM: " + activeChar.getName(null) + "(" + activeChar.getObjectId() + ") healed character " + target.getName(activeChar.getLang()));
			}
		}
		else
		{
			activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
		}
	}
}