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

import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.reflection.ReflectionWorld;
import l2e.scripts.ai.AbstractNpcAI;

public final class Sirra extends AbstractNpcAI
{
	private Sirra()
	{
		super(Sirra.class.getSimpleName(), "custom");
		addFirstTalkId(32762);
	}

	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		final ReflectionWorld world = ReflectionManager.getInstance().getWorld(npc.getReflectionId());
		if (world != null)
		{
			if (world.getTemplateId() == 139)
			{
				return world.isStatus(0) ? "32762-easy.htm" : "32762-easyfight.htm";
			}
			else if (world.getTemplateId() == 144)
			{
				return world.isStatus(0) ? "32762-hard.htm" : "32762-hardfight.htm";
			}
		}
		else
		{
			if (npc.getReflectionId() == 0)
			{
				return "32762-world.htm";
			}
		}
		return "32762.htm";
	}

	public static void main(String[] args)
	{
		new Sirra();
	}
}