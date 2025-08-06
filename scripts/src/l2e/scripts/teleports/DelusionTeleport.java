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
package l2e.scripts.teleports;

import l2e.gameserver.data.parser.TeleLocationParser;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.service.BotFunctions;
import l2e.scripts.ai.AbstractNpcAI;

/**
 * Based on L2J Eternity-World
 */
public class DelusionTeleport extends AbstractNpcAI
{
	public DelusionTeleport(String name, String descr)
	{
		super(name, descr);
		
		addStartNpc(32484, 32658, 32659, 32660, 32661, 32662, 32663);
		addTalkId(32484, 32658, 32659, 32660, 32661, 32662, 32663);
	}

	@Override
	public String onTalk(Npc npc, Player player)
	{
		final var st = player.getQuestState(getName());
		final int npcId = npc.getId();

		if (npcId == 32484)
		{
			final var template = TeleLocationParser.getInstance().getTemplate(300002);
			if (template != null)
			{
				if (BotFunctions.getInstance().isAutoTpByIdEnable(player))
				{
					player.setVar("delusion_coords", "" + player.getX() + " " + player.getY() + " " + player.getZ() + "");
					BotFunctions.getInstance().getAutoTeleportById(player, player.getLocation(), template.getLocation(), 1000);
					return null;
				}
				player.setVar("delusion_coords", "" + player.getX() + " " + player.getY() + " " + player.getZ() + "");
				player.teleToLocation(template.getLocation(), true, player.getReflection());
				if (player.hasSummon())
				{
					player.getSummon().teleToLocation(template.getLocation(), true, player.getReflection());
				}
			}
		}
		else if (npcId == 32658 || npcId == 32659 || npcId == 32660 || npcId == 32661 || npcId == 32663 || npcId == 32662)
		{
			final String locInfo = player.getVar("delusion_coords");
			if (locInfo != null)
			{
				final Location loc = Location.parseLoc(locInfo);
				if (loc != null)
				{
					if (BotFunctions.getInstance().isAutoTpByIdEnable(player))
					{
						BotFunctions.getInstance().getAutoTeleportById(player, player.getLocation(), new Location(loc.getX(), loc.getY(), loc.getZ()), 1000);
						return null;
					}
					player.teleToLocation(loc.getX(), loc.getY(), loc.getZ(), true, player.getReflection());
					if (player.hasSummon())
					{
						player.getSummon().teleToLocation(loc.getX(), loc.getY(), loc.getZ(), true, player.getReflection());
					}
				}
				player.unsetVar("delusion_coords");
			}
			st.exitQuest(true);
		}
		return "";
	}
	public static void main(String[] args)
	{
		new DelusionTeleport(DelusionTeleport.class.getSimpleName(), "teleports");
	}
}