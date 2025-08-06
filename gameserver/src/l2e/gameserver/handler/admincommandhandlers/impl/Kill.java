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

import java.util.StringTokenizer;

import l2e.gameserver.Config;
import l2e.gameserver.ai.model.CtrlEvent;
import l2e.gameserver.handler.admincommandhandlers.IAdminCommandHandler;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.ControllableMobInstance;
import l2e.gameserver.network.SystemMessageId;

public class Kill implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
	        "admin_kill"
	};

	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		if (command.startsWith("admin_kill"))
		{
			final StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken();
			
			if (st.hasMoreTokens())
			{
				try
				{
					final int radius = Integer.parseInt(st.nextToken());
					for (final Creature knownChar : World.getInstance().getAroundCharacters(activeChar, radius, 1000))
					{
						if (knownChar instanceof ControllableMobInstance || knownChar == activeChar)
						{
							continue;
						}
						kill(activeChar, knownChar);
					}
					activeChar.sendMessage("Killed all characters within a " + radius + " unit radius.");
					return true;
				}
				catch (final NumberFormatException e)
				{
					activeChar.sendMessage("Usage: //kill <radius>");
					return false;
				}
			}
			else
			{
				final GameObject obj = activeChar.getTarget();
				if (obj == null || obj instanceof ControllableMobInstance || !obj.isCreature())
				{
					activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
				}
				else
				{
					kill(activeChar, (Creature) obj);
				}
			}
		}
		return true;
	}
	
	private void kill(Player activeChar, Creature target)
	{
		boolean targetIsInvul = false;
		if (target.isInvul())
		{
			targetIsInvul = true;
			target.setIsInvul(false);
		}
		
		if (target.isMonster() && target.hasAI())
		{
			target.getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, activeChar, target.getCurrentHp());
		}
		target.doDie(activeChar);
		
		if (targetIsInvul)
		{
			target.setIsInvul(true);
		}
		
		if (Config.DEBUG)
		{
			_log.info("GM: " + activeChar.getName(null) + "(" + activeChar.getObjectId() + ")" + " killed character " + target.getObjectId());
		}
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}