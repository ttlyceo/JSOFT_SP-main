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
package l2e.scripts.custom;

import l2e.commons.util.Util;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.service.BotFunctions;
import l2e.scripts.ai.AbstractNpcAI;

public final class FreyasSteward extends AbstractNpcAI
{
	private FreyasSteward()
	{
		super(FreyasSteward.class.getSimpleName(), "custom");

		addStartNpc(32029);
		addFirstTalkId(32029);
		addTalkId(32029);
	}

	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return "32029.htm";
	}

	@Override
	public String onTalk(Npc npc, Player player)
	{
		if (player.getLevel() >= 82)
		{
			if (BotFunctions.getInstance().isAllowLicence() && player.isInParty())
			{
				if (player.getParty().isLeader(player) && player.getVarB("autoTeleport@", false))
				{
					for (final Player member : player.getParty().getMembers())
					{
						if (member != null)
						{
							if (member.getObjectId() == player.getObjectId())
							{
								continue;
							}
							
							if (!Util.checkIfInRange(1000, player, member, true) || member.getLevel() < 82)
							{
								continue;
							}
							
							if (!BotFunctions.checkCondition(member, false) || !member.getIPAddress().equalsIgnoreCase(player.getIPAddress()))
							{
								continue;
							}
							member.teleToLocation(103045, -124361, -2768, true, member.getReflection());
						}
					}
				}
			}
			player.teleToLocation(103045, -124361, -2768, true, player.getReflection());
			return null;
		}
		return "32029-1.htm";
	}

	public static void main(String[] args)
	{
		new FreyasSteward();
	}
}
