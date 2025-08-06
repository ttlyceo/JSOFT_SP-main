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

import l2e.commons.util.Util;
import l2e.gameserver.data.parser.TransformParser;
import l2e.gameserver.handler.admincommandhandlers.IAdminCommandHandler;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.MagicSkillUse;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;
import l2e.gameserver.network.serverpackets.SetupGauge;

public class Polymorph implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
	        "admin_polymorph", "admin_unpolymorph", "admin_polymorph_menu", "admin_unpolymorph_menu", "admin_transform", "admin_untransform", "admin_transform_menu", "admin_untransform_menu",
	};
	
	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		if (command.startsWith("admin_untransform"))
		{
			final GameObject obj = activeChar.getTarget();
			if (obj instanceof Creature)
			{
				((Creature) obj).stopTransformation(true);
			}
			else
			{
				activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
			}
		}
		else if (command.startsWith("admin_transform"))
		{
			final GameObject obj = activeChar.getTarget();
			if ((obj != null) && obj.isPlayer())
			{
				final Player cha = obj.getActingPlayer();

				if (activeChar.isSitting())
				{
					activeChar.sendPacket(SystemMessageId.CANNOT_TRANSFORM_WHILE_SITTING);
					return false;
				}
				else if (cha.isTransformed() || cha.isInStance())
				{
					activeChar.sendPacket(SystemMessageId.YOU_ALREADY_POLYMORPHED_AND_CANNOT_POLYMORPH_AGAIN);
					return false;
				}
				else if (cha.isInWater())
				{
					activeChar.sendPacket(SystemMessageId.YOU_CANNOT_POLYMORPH_INTO_THE_DESIRED_FORM_IN_WATER);
					return false;
				}
				else if (cha.isFlyingMounted() || cha.isMounted())
				{
					activeChar.sendPacket(SystemMessageId.YOU_CANNOT_POLYMORPH_WHILE_RIDING_A_PET);
					return false;
				}

				final String[] parts = command.split(" ");
				if (parts.length > 1)
				{
					if (Util.isDigit(parts[1]))
					{
						final int id = Integer.parseInt(parts[1]);
						if (!TransformParser.getInstance().transformPlayer(id, cha))
						{
							cha.sendMessage("Unknown transformation Id: " + id);
						}
					}
					else
					{
						activeChar.sendMessage("Usage: //transform <id>");
					}
				}
				else if (parts.length == 1)
				{
					cha.untransform();
				}
				else
				{
					activeChar.sendMessage("Usage: //transform <id>");
				}
			}
			else
			{
				activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
			}
		}
		if (command.startsWith("admin_polymorph"))
		{
			final StringTokenizer st = new StringTokenizer(command);
			final GameObject target = activeChar.getTarget();
			try
			{
				st.nextToken();
				final String p1 = st.nextToken();
				if (st.hasMoreTokens())
				{
					final String p2 = st.nextToken();
					doPolymorph(activeChar, target, p2, p1);
				}
				else
				{
					doPolymorph(activeChar, target, p1, "npc");
				}
			}
			catch (final Exception e)
			{
				activeChar.sendMessage("Usage: //polymorph [type] <id>");
			}
		}
		else if (command.equals("admin_unpolymorph"))
		{
			doUnpoly(activeChar, activeChar.getTarget());
		}
		if (command.contains("_menu"))
		{
			showMainPage(activeChar, command);
		}
		return true;
	}

	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}

	private void doPolymorph(Player activeChar, GameObject obj, String id, String type)
	{
		if (obj != null && obj instanceof Player)
		{
			final Player Char = (Player) obj;
			Char.setPolyId(Integer.parseInt(id));
			final MagicSkillUse msk = new MagicSkillUse(Char, 1008, 1, 4000, 0);
			Char.broadcastPacket(msk);
			final SetupGauge sg = new SetupGauge(Char, 0, 4000);
			Char.sendPacket(sg);
			Char.decayMe();
			Char.spawnMe(obj.getX(), obj.getY(), obj.getZ());
			activeChar.sendMessage("Polymorph succeed");
		}
		else
		{
			activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
		}
	}

	private void doUnpoly(Player activeChar, GameObject target)
	{
		if (target != null && target instanceof Player)
		{
			final Player Char = (Player) target;
			Char.setPolyId(0);
			Char.decayMe();
			Char.spawnMe(target.getX(), target.getY(), target.getZ());
			activeChar.sendMessage("Unpolymorph succeed");
		}
		else
		{
			activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
		}
	}

	private void showMainPage(Player activeChar, String command)
	{
		final NpcHtmlMessage adminhtm = new NpcHtmlMessage(5);

		if (command.contains("transform"))
		{
			adminhtm.setFile(activeChar, activeChar.getLang(), "data/html/admin/transform.htm");
			activeChar.sendPacket(adminhtm);
		}
		else if (command.contains("abnormal"))
		{
			adminhtm.setFile(activeChar, activeChar.getLang(), "data/html/admin/abnormal.htm");
			activeChar.sendPacket(adminhtm);
		}
		else
		{
			adminhtm.setFile(activeChar, activeChar.getLang(), "data/html/admin/effects_menu.htm");
			activeChar.sendPacket(adminhtm);
		}
	}
}