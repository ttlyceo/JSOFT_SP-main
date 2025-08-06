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
package l2e.scripts.ai;

import l2e.commons.apache.ArrayUtils;
import l2e.commons.util.Rnd;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.instancemanager.ZoneManager;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.zone.ZoneType;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.serverpackets.ExShowScreenMessage;

/**
 * Created by LordWinter 20.06.2012 Based on L2J Eternity-World
 */
public class Maguen extends AbstractNpcAI
{
	private static final int MAGUEN = 18839;

	private static final int[] MOBS =
	{
	                22746, 22747, 22748, 22749, 22754, 22755, 22756, 22760, 22761, 22762
	};

	private static final int[] maguenStatsSkills =
	{
	                6343, 6365, 6366
	};

	private static final int[] maguenRaceSkills =
	{
	                6367, 6368, 6369
	};

	public Maguen(String name, String descr)
	{
		super(name, descr);

		addSpawnId(MAGUEN);
		addSkillSeeId(MAGUEN);

		for (final int i : MOBS)
		{
			addKillId(i);
		}
	}

	@Override
	public String onSpawn(Npc npc)
	{
		ThreadPoolManager.getInstance().schedule(new Plasma(npc), 2000L);
		return super.onSpawn(npc);
	}

	@Override
	public String onSkillSee(Npc npc, Player caster, Skill skill, GameObject[] targets, boolean isSummon)
	{
		if (npc.getId() == MAGUEN)
		{
			if (skill.getId() != 9060)
			{
				return null;
			}

			if (Rnd.chance(4))
			{
				caster.addItem("Maguen", 15490, 1, null, true);
			}
			if (Rnd.chance(2))
			{
				caster.addItem("Maguen", 15491, 1, null, true);
			}

			final ZoneType zone = getZone(npc, "Seed of Annihilation", true);

			if (zone != null)
			{
				for (final Creature ch : zone.getCharactersInside())
				{
					if ((ch != null) && !ch.isDead())
					{
						npc.setTarget(caster);

						switch (npc.getDisplayEffect())
						{
							case 1:
								if (Rnd.chance(80))
								{
									npc.doCast(SkillsParser.getInstance().getInfo(maguenRaceSkills[0], getRandom(2, 3)));
								}
								else
								{
									npc.doCast(SkillsParser.getInstance().getInfo(maguenStatsSkills[0], getRandom(1, 2)));
								}
								break;
							case 2:
								if (Rnd.chance(80))
								{
									npc.doCast(SkillsParser.getInstance().getInfo(maguenRaceSkills[1], getRandom(2, 3)));
								}
								else
								{
									npc.doCast(SkillsParser.getInstance().getInfo(maguenStatsSkills[1], getRandom(1, 2)));
								}
								break;
							case 3:
								if (Rnd.chance(80))
								{
									npc.doCast(SkillsParser.getInstance().getInfo(maguenRaceSkills[2], getRandom(2, 3)));
								}
								else
								{
									npc.doCast(SkillsParser.getInstance().getInfo(maguenStatsSkills[2], getRandom(1, 2)));
								}
								break;
							default:
								break;
						}
					}
					else
					{
						switch (npc.getDisplayEffect())
						{
							case 1:
								npc.doCast(SkillsParser.getInstance().getInfo(maguenRaceSkills[0], 1));
								break;
							case 2:
								npc.doCast(SkillsParser.getInstance().getInfo(maguenRaceSkills[1], 1));
								break;
							case 3:
								npc.doCast(SkillsParser.getInstance().getInfo(maguenRaceSkills[2], 1));
								break;
							default:
								break;
						}
					}
				}
			}
		}
		return super.onSkillSee(npc, caster, skill, targets, isSummon);
	}

	@Override
	public String onKill(Npc npc, Player killer, boolean isSummon)
	{
		if (ArrayUtils.contains(MOBS, npc.getId()))
		{
			if (Rnd.chance(5))
			{
				final Npc maguen = addSpawn(MAGUEN, npc.getX() + getRandom(10, 50), npc.getY() + getRandom(10, 50), npc.getZ(), 0, false, 10000, true);
				maguen.setRunning();
				((Attackable) maguen).addDamageHate(killer, 1, 99999);
				maguen.getAI().setIntention(CtrlIntention.ATTACK, killer);

				killer.sendPacket(new ExShowScreenMessage(NpcStringId.MAGUEN_APPEARANCE, 2, 5000));
			}
		}
		return super.onKill(npc, killer, isSummon);
	}

	private class Plasma implements Runnable
	{
		private final Npc _npc;

		public Plasma(Npc npc)
		{
			_npc = npc;
		}

		@Override
		public void run()
		{
			_npc.setDisplayEffect(getRandom(1, 3));
		}
	}

	private ZoneType getZone(Npc npc, String nameTemplate, boolean currentLoc)
	{
		try
		{
			int x;
			int y;
			int z;

			if (currentLoc)
			{
				x = npc.getX();
				y = npc.getY();
				z = npc.getZ();
			}
			else
			{
				x = npc.getSpawn().getX();
				y = npc.getSpawn().getY();
				z = npc.getSpawn().getZ();
			}

			for (final ZoneType zone : ZoneManager.getInstance().getZones(x, y, z))
			{
				if (zone.getName().startsWith(nameTemplate))
				{
					return zone;
				}
			}
		}

		catch (final NullPointerException e)
		{
		}
		catch (final IndexOutOfBoundsException e)
		{
		}
		return null;
	}

	public static void main(String[] args)
	{
		new Maguen(Maguen.class.getSimpleName(), "ai");
	}
}
