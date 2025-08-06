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
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.reflection.ReflectionTemplate;
import l2e.gameserver.model.actor.templates.reflection.ReflectionWorld;
import l2e.gameserver.model.quest.QuestState;

/**
 * Rework by LordWinter 02.10.2020
 */
public final class JiniaGuildHideout1 extends AbstractReflection
{
	private class JGH1World extends ReflectionWorld
	{
		public JGH1World()
		{
		}
	}

	private JiniaGuildHideout1()
	{
		super(JiniaGuildHideout1.class.getSimpleName(), "instances");

		addStartNpc(32020);
		addTalkId(32020);
	}
	
	private final synchronized void enterInstance(Player player, Npc npc)
	{
		enterInstance(player, npc, new JGH1World(), 140);
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
	public String onTalk(Npc npc, Player talker)
	{
		final int cond;
		final QuestState qs = talker.getQuestState("_10284_AcquisitionOfDivineSword");
		if ((qs != null) && (((cond = qs.getCond()) == 1) || (cond == 2)))
		{
			enterInstance(talker, npc);
			if (qs.getCond() < 2)
			{
				qs.setCond(2, true);
			}
		}
		return super.onTalk(npc, talker);
	}


	public static void main(String[] args)
	{
		new JiniaGuildHideout1();
	}
}
