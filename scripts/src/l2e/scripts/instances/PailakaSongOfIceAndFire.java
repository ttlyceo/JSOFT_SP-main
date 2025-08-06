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

import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.reflection.ReflectionTemplate;
import l2e.gameserver.model.actor.templates.reflection.ReflectionWorld;
import l2e.gameserver.model.entity.Reflection;
import l2e.gameserver.model.zone.ZoneType;

/**
 * Rework by LordWinter 12.12.2020
 */
public class PailakaSongOfIceAndFire extends AbstractReflection
{
	private class PSFWorld extends ReflectionWorld
	{
		public PSFWorld()
		{
		}
	}
	
	public PailakaSongOfIceAndFire(String name, String descr)
	{
		super(name, descr);
		
		addStartNpc(32497);
		addTalkId(32497);
		addExitZoneId(20108);
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
		enterInstance(player, npc, new PSFWorld(), 43);
	}
	
	@Override
	public final String onAdvEvent(String event, Npc npc, Player player)
	{
		if (event.equalsIgnoreCase("enter"))
		{
			enterInstance(player, npc);
			return null;
		}
		return super.onAdvEvent(event, npc, player);
	}
	
	@Override
	public String onExitZone(Creature character, ZoneType zone)
	{
		if ((character.isPlayer()) && !character.isDead() && !character.isTeleporting() && ((Player) character).isOnline())
		{
			final ReflectionWorld world = ReflectionManager.getInstance().getWorld(character.getReflectionId());
			if ((world != null) && (world.getTemplateId() == 43))
			{
				ThreadPoolManager.getInstance().schedule(new Teleport(character.getActingPlayer(), world.getReflection()), 1000);
			}
		}
		return super.onExitZone(character, zone);
	}
	
	private static final class Teleport implements Runnable
	{
		private final Player _player;
		private final Reflection _r;
		
		public Teleport(Player c, Reflection r)
		{
			_player = c;
			_r = r;
		}
		
		@Override
		public void run()
		{
			if (_player != null && _r != null)
			{
				_player.getAI().setIntention(CtrlIntention.IDLE);
				_player.teleToLocation(-52875, 188232, -4696, true, _r);
				
			}
		}
	}
	
	public static void main(String[] args)
	{
		new PailakaSongOfIceAndFire(PailakaSongOfIceAndFire.class.getSimpleName(), "instances");
	}
}
