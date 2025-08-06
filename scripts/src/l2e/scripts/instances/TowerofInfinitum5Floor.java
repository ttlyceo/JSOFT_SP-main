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
import l2e.gameserver.model.entity.Reflection;

/**
 * Rework by LordWinter 27.09.2020
 */
public class TowerofInfinitum5Floor extends AbstractReflection
{
	protected class TOI5World extends ReflectionWorld
	{
		public TOI5World()
		{
		}
	}
	
	public TowerofInfinitum5Floor(String name, String descr)
	{
		super(name, descr);
		
		addStartNpc(32375, 32748);
		addTalkId(32375, 32748);

		addKillId(25540);
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
	
	private final synchronized void enterInstance(Player player, Npc npc)
	{
		enterInstance(player, npc, new TOI5World(), 142);
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		switch (npc.getId())
		{
			case 32748 :
				enterInstance(player, npc);
				break;
			case 32375 :
				final ReflectionWorld world = ReflectionManager.getInstance().getWorld(npc.getReflectionId());
				if ((world != null) && (world instanceof TOI5World))
				{
					world.removeAllowed(player.getObjectId());
					teleportPlayer(player, new Location(-19024, 277122, -8256), ReflectionManager.DEFAULT);
				}
				break;
		}
		return null;
	}
	
	@Override
	public String onKill(Npc npc, Player killer, boolean isSummon)
	{
		final ReflectionWorld world = ReflectionManager.getInstance().getWorld(npc.getReflectionId());
		if ((world != null) && (world instanceof TOI5World))
		{
			final Reflection inst = ReflectionManager.getInstance().getReflection(npc.getReflectionId());
			if (inst != null)
			{
				inst.setReturnLoc(new Location(-19024, 277122, -8256));
			}
			finishInstance(world, true);
			addSpawn(32375, -22144, 278744, -8256, 0, false, 0, false, npc.getReflection());
		}
		return super.onKill(npc, killer, isSummon);
	}
	
	public static void main(String[] args)
	{
		new TowerofInfinitum5Floor(TowerofInfinitum5Floor.class.getSimpleName(), "instances");
	}
}