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
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.service.BotFunctions;

/**
 * Create by LordWinter 13.06.2019
 */
public class TemporaryTeleporter extends Quest
{
	public TemporaryTeleporter(int id, String name, String desc)
	{
		super(id, name, desc);

		addStartNpc(32602);
		addFirstTalkId(32602);
	}

	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		if (npc.getId() == 32602)
		{
			final var template = TeleLocationParser.getInstance().getTemplate(300007);
			if (template != null)
			{
				if (BotFunctions.getInstance().isAutoTpByIdEnable(player))
				{
					BotFunctions.getInstance().getAutoTeleportById(player, player.getLocation(), template.getLocation(), 1000);
					return null;
				}
				player.teleToLocation(template.getLocation(), true, player.getReflection());
			}
		}
		player.sendActionFailed();
		return null;
	}

	public static void main(String[] args)
	{
		new TemporaryTeleporter(-1, TemporaryTeleporter.class.getSimpleName(), "teleports");
	}
}