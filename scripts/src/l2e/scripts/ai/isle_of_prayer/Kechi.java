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
package l2e.scripts.ai.isle_of_prayer;

import java.util.ArrayList;

import l2e.commons.util.Rnd;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.ai.npc.Fighter;
import l2e.gameserver.data.parser.NpcsParser;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.templates.npc.aggro.AggroInfo;
import l2e.gameserver.model.spawn.Spawner;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.NpcSay;

/**
 * Created by LordWinter 16.09.2018
 */
public class Kechi extends Fighter
{
	private static final int[][] guard_run = new int[][]
	{
	                {
	                                22309, 153384, 149528, -12136
			},
			{
			                22309, 153975, 149823, -12152
			},
			{
			                22309, 154364, 149665, -12151
			},
			{
			                22309, 153786, 149367, -12151
			},
			{
			                22310, 154188, 149825, -12152
			},
			{
			                22310, 153945, 149224, -12151
			},
			{
			                22417, 154374, 149399, -12152
			},
			{
			                22417, 153796, 149646, -12159
			}
	};

	private static NpcStringId[] CHAT =
	{
	                NpcStringId.HELP_ME, NpcStringId.PREPARE_TO_DIE
	};

	private int stage = 0;

	public Kechi(Attackable actor)
	{
		super(actor);
	}

	@Override
	protected void onEvtSpawn()
	{
		stage = 0;
		super.onEvtSpawn();
	}

	@Override
	protected void onEvtAttacked(Creature attacker, int damage)
	{
		final Attackable actor = getActiveChar();
		if (actor.isDead() || (attacker == null))
		{
			return;
		}

		final double actor_hp_precent = actor.getCurrentHpPercents();

		switch (stage)
		{
			case 0:
				if (actor_hp_precent < 80)
				{
					spawnMobs();
				}
				break;
			case 1:
				if (actor_hp_precent < 60)
				{
					spawnMobs();
				}
				break;
			case 2:
				if (actor_hp_precent < 40)
				{
					spawnMobs();
				}
				break;
			case 3:
				if (actor_hp_precent < 30)
				{
					spawnMobs();
				}
				break;
			case 4:
				if (actor_hp_precent < 20)
				{
					spawnMobs();
				}
				break;
			case 5:
				if (actor_hp_precent < 10)
				{
					spawnMobs();
				}
				break;
			case 6:
				if (actor_hp_precent < 5)
				{
					spawnMobs();
				}
				break;
		}
		super.onEvtAttacked(attacker, damage);
	}

	protected void spawnMobs()
	{
		stage++;
		final Attackable actor = getActiveChar();
		actor.broadcastPacketToOthers(2000, new NpcSay(actor.getObjectId(), Say2.NPC_ALL, actor.getId(), CHAT[Rnd.get(2)]));
		for (final int[] run : guard_run)
		{
			try
			{
				final Spawner sp = new Spawner(NpcsParser.getInstance().getTemplate(run[0]));
				sp.setLocation(new Location(153384, 149528, -12136));
				sp.setReflection(actor.getReflection());
				sp.stopRespawn();
				final Npc guard = sp.spawnOne(false);

				guard.setRunning();
				guard.getAI().setIntention(CtrlIntention.MOVING, new Location(run[1], run[2], run[3]), 0);

				final ArrayList<Creature> chars = new ArrayList<>();
				for (final AggroInfo info : actor.getAggroList().getCharMap().values())
				{
					if (info == null)
					{
						continue;
					}
					chars.add(info.getAttacker());
				}

				Creature hated;
				if (chars.isEmpty())
				{
					hated = null;
				}
				else
				{
					hated = chars.get(Rnd.get(chars.size()));
				}

				if (hated != null)
				{
					((Attackable) guard).addDamageHate(hated, 0, Rnd.get(1, 100));
					guard.setTarget(hated);
					guard.getAI().setIntention(CtrlIntention.ATTACK, hated, null);
				}
			}
			catch (final Exception e)
			{
				e.printStackTrace();
			}
		}
	}
}
