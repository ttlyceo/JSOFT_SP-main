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
package l2e.scripts.instances;

import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.reflection.ReflectionTemplate;
import l2e.gameserver.model.actor.templates.reflection.ReflectionWorld;
import l2e.gameserver.model.quest.QuestState;

/**
 * Rework by LordWinter 02.10.2020
 */
public class HideoutoftheDawn extends AbstractReflection
{
	private class HoDWorld extends ReflectionWorld
	{
		public HoDWorld()
		{
		}
	}

	public HideoutoftheDawn(String name, String descr)
	{
		super(name, descr);

		addStartNpc(32593, 32617);
		addTalkId(32593, 32617);
	}
	
	private final synchronized void enterInstance(Player player, Npc npc)
	{
		enterInstance(player, npc, new HoDWorld(), 113);
	}
	
	@Override
	protected void onTeleportEnter(Player player, ReflectionTemplate template, ReflectionWorld world, boolean firstEntrance)
	{
		if (firstEntrance)
		{
			world.addAllowed(player.getObjectId());
			player.getAI().setIntention(CtrlIntention.IDLE);
			final Location teleLoc = template.getTeleportCoord();
			player.teleToLocation(teleLoc, true, world.getReflection());
			if (player.hasSummon())
			{
				player.getSummon().getAI().setIntention(CtrlIntention.IDLE);
				player.getSummon().teleToLocation(teleLoc, true, world.getReflection());
			}
		}
		else
		{
			player.getAI().setIntention(CtrlIntention.IDLE);
			final Location teleLoc = template.getTeleportCoord();
			player.teleToLocation(teleLoc, true, world.getReflection());
			if (player.hasSummon())
			{
				player.getSummon().getAI().setIntention(CtrlIntention.IDLE);
				player.getSummon().teleToLocation(teleLoc, true, world.getReflection());
			}
		}
	}

	@Override
	public String onTalk(Npc npc, Player player)
	{
		final String htmltext = getNoQuestMsg(player);
		QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			st = newQuestState(player);
		}

		if (npc.getId() == 32593)
		{
			enterInstance(player, npc);
			return null;
		}
		else if (npc.getId() == 32617)
		{
			final ReflectionWorld world = ReflectionManager.getInstance().getPlayerWorld(player);
			if (world != null)
			{
				world.removeAllowed(player.getObjectId());
			}
			player.teleToLocation(new Location(147072, 23743, -1984, 0), true, ReflectionManager.DEFAULT);
			return null;
		}
		return htmltext;
	}

	public static void main(String[] args)
	{
		new HideoutoftheDawn(HideoutoftheDawn.class.getSimpleName(), "instances");
	}
}