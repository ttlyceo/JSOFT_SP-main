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
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.NpcSay;

/**
 * Rework by LordWinter 02.10.2020
 */
public class LibraryOfSages extends AbstractReflection
{
	private static final NpcStringId[] spam =
	{
	        NpcStringId.I_MUST_ASK_LIBRARIAN_SOPHIA_ABOUT_THE_BOOK, NpcStringId.THIS_LIBRARY_ITS_HUGE_BUT_THERE_ARENT_MANY_USEFUL_BOOKS_RIGHT, NpcStringId.AN_UNDERGROUND_LIBRARY_I_HATE_DAMP_AND_SMELLY_PLACES, NpcStringId.THE_BOOK_THAT_WE_SEEK_IS_CERTAINLY_HERE_SEARCH_INCH_BY_INCH
	};

	private class LibraryOfSagesWorld extends ReflectionWorld
	{
		Npc support = null;
	}

	public LibraryOfSages(String name, String descr)
	{
		super(name, descr);

		addStartNpc(32861, 32596);
		addTalkId(32861, 32863, 32596, 32785);
	}
	
	private final synchronized void enterInstance(Player player, Npc npc)
	{
		if (enterInstance(player, npc, new LibraryOfSagesWorld(), 156))
		{
			final ReflectionWorld world = ReflectionManager.getInstance().getPlayerWorld(player);
			((LibraryOfSagesWorld) world).support = addSpawn(32785, player.getX(), player.getY(), player.getZ(), 0, false, 0, false, player.getReflection());
			startQuestTimer("check_follow", 3000, ((LibraryOfSagesWorld) world).support, player);
		}
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

	private void teleportPlayer(Npc npc, Player player, Location loc, Reflection r)
	{
		player.stopAllEffectsExceptThoseThatLastThroughDeath();
		player.getAI().setIntention(CtrlIntention.IDLE);
		player.teleToLocation(loc.getX(), loc.getY(), loc.getZ(), false, r);
		if (!r.isDefault())
		{
			final ReflectionWorld tmpworld = ReflectionManager.getInstance().getWorld(npc.getReflectionId());
			if (tmpworld instanceof LibraryOfSagesWorld)
			{
				final LibraryOfSagesWorld world = (LibraryOfSagesWorld) tmpworld;
				cancelQuestTimer("check_follow", world.support, player);
				world.support.deleteMe();
				world.support = addSpawn(32785, player.getX(), player.getY(), player.getZ(), 0, false, 0, false, r);
				startQuestTimer("check_follow", 3000, world.support, player);
			}
		}
	}

	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		final String htmltext = getNoQuestMsg(player);
		QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			st = newQuestState(player);
		}

		if (event.equalsIgnoreCase("check_follow"))
		{
			cancelQuestTimer("check_follow", npc, player);
			npc.getAI().stopFollow();
			npc.setIsRunning(true);
			npc.getAI().startFollow(player);
			npc.broadcastPacketToOthers(2000, new NpcSay(npc.getObjectId(), Say2.ALL, npc.getId(), spam[getRandom(0, spam.length - 1)]));
			startQuestTimer("check_follow", 20000, npc, player);
			return "";
		}
		else if (npc.getId() == 32596)
		{
			if (event.equalsIgnoreCase("tele1"))
			{
				enterInstance(player, npc);
				return null;
			}
		}
		else if (npc.getId() == 32861)
		{
			if (event.equalsIgnoreCase("tele2"))
			{
				teleportPlayer(player, new Location(37355, -50065, -1127), player.getReflection());
				return null;
			}
			else if (event.equalsIgnoreCase("tele3"))
			{
				final ReflectionWorld tmpworld = ReflectionManager.getInstance().getWorld(npc.getReflectionId());
				if (tmpworld instanceof LibraryOfSagesWorld)
				{
					final LibraryOfSagesWorld world = (LibraryOfSagesWorld) tmpworld;
					cancelQuestTimer("check_follow", world.support, player);
					world.support.deleteMe();
					teleportPlayer(npc, player, new Location(37063, -49813, -1128), ReflectionManager.DEFAULT);
				}
				return null;
			}
		}
		else if (npc.getId() == 32863)
		{
			if (event.equalsIgnoreCase("tele4"))
			{
				teleportPlayer(npc, player, new Location(37063, -49813, -1128), player.getReflection());
				return null;
			}
		}
		return htmltext;
	}

	public static void main(String[] args)
	{
		new LibraryOfSages(LibraryOfSages.class.getSimpleName(), "instances");
	}
}