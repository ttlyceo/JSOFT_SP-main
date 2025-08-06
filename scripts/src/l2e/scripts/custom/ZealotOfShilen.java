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

import java.util.ArrayList;
import java.util.List;

import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.data.parser.SpawnParser;
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.spawn.Spawner;
import l2e.scripts.ai.AbstractNpcAI;

public class ZealotOfShilen extends AbstractNpcAI
{
	private static final int ZEALOT = 18782;
	private static final int GUARD1 = 32628;
	private static final int GUARD2 = 32629;

	private final List<Npc> _zealot = new ArrayList<>();
	private final List<Npc> _guard1 = new ArrayList<>();
	private final List<Npc> _guard2 = new ArrayList<>();

	private ZealotOfShilen(String name, String descr)
	{
		super(name, descr);

		addSpawnId(ZEALOT);

		addFirstTalkId(GUARD1, GUARD2);

		findNpcs();
	}

	private void findNpcs()
	{
		for (final Spawner spawn : SpawnParser.getInstance().getSpawnData())
		{
			if (spawn != null)
			{
				if (spawn.getId() == ZEALOT)
				{
					_zealot.add(spawn.getLastSpawn());
					for (final Npc zealot : _zealot)
					{
						zealot.setIsNoRndWalk(true);
					}
				}
				else if (spawn.getId() == GUARD1)
				{
					_guard1.add(spawn.getLastSpawn());
					for (final Npc guard : _guard1)
					{
						guard.setIsInvul(true);
						((Attackable) guard).setCanReturnToSpawnPoint(false);
						startQuestTimer("WATCHING", 10000, guard, null, true);
					}
				}
				else if (spawn.getId() == GUARD2)
				{
					_guard2.add(spawn.getLastSpawn());
					for (final Npc guards : _guard2)
					{
						guards.setIsInvul(true);
						((Attackable) guards).setCanReturnToSpawnPoint(false);
						startQuestTimer("WATCHING", 10000, guards, null, true);
					}
				}
			}
		}
	}

	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		if (event.equalsIgnoreCase("WATCHING") && !npc.isAttackingNow())
		{
			for (final Npc character : World.getInstance().getAroundNpc(npc))
			{
				if (character.isMonster() && !character.isDead())
				{
					npc.setRunning();
					((Attackable) npc).addDamageHate(character, 0, 999);
					npc.getAI().setIntention(CtrlIntention.ATTACK, character, null);
				}
			}
		}
		return null;
	}

	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return (npc.isAttackingNow()) ? "32628-01.htm" : npc.getId() + ".htm";
	}

	@Override
	public String onSpawn(Npc npc)
	{
		npc.setIsNoRndWalk(true);
		return super.onSpawn(npc);
	}

	public static void main(String[] args)
	{
		new ZealotOfShilen(ZealotOfShilen.class.getSimpleName(), "custom");
	}
}
