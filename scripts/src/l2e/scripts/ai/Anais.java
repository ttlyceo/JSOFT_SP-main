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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.geodata.GeoEngine;
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.network.serverpackets.NpcInfo;

/**
 * Created by LordWinter 21.08.2012 Based on L2J Eternity-World
 */
public class Anais extends AbstractNpcAI
{
	private static final int ANAIS = 25701;
	private static final int GUARD = 25702;
	
	private static boolean FIGHTHING = false;
	private final List<Npc> burners = new ArrayList<>();
	private final List<Npc> guards = new ArrayList<>();
	private final Map<Npc, Player> targets = new HashMap<>();
	
	private static int BURNERS_ENABLED = 0;
	
	private static final int[][] BURNERS =
	{
	        {
	                113632, -75616, 50
			},
			{
			        111904, -75616, 58
			},
			{
			        111904, -77424, 51
			},
			{
			        113696, -77393, 48
			}
	};
	
	Skill guard_skill = SkillsParser.getInstance().getInfo(6326, 1);
	
	public Anais(String name, String descr)
	{
		super(name, descr);
		
		registerMobs(new int[]
		{
		        ANAIS, GUARD
		});
		
		spawnBurners();
	}
	
	private void spawnBurners()
	{
		for (final int[] SPAWN : BURNERS)
		{
			final Npc npc = addSpawn(18915, SPAWN[0], SPAWN[1], SPAWN[2], 0, false, 0L);
			if (npc == null)
			{
				continue;
			}
			burners.add(npc);
		}
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		if (event.equalsIgnoreCase("check_status"))
		{
			if (FIGHTHING)
			{
				if ((npc.getAI().getIntention() == CtrlIntention.ACTIVE) || (npc.getAI().getIntention() == CtrlIntention.IDLE))
				{
					stopFight();
				}
				else
				{
					startQuestTimer("check_status", 50000L, npc, null);
				}
			}
		}
		else if (event.equalsIgnoreCase("burner_action"))
		{
			if ((FIGHTHING) && (npc != null))
			{
				final Npc guard = addSpawn(GUARD, npc);
				if (guard != null)
				{
					guards.add(guard);
					startQuestTimer("guard_action", 500L, guard, null);
				}
				startQuestTimer("burner_action", 20000L, npc, null);
			}
		}
		else if (event.equalsIgnoreCase("guard_action"))
		{
			if ((FIGHTHING) && (npc != null) && (!npc.isDead()))
			{
				if (targets.containsKey(npc))
				{
					final Player target = targets.get(npc);
					if ((target != null) && (target.isOnline()) && (target.isInsideRadius(npc, 5000, false, false)))
					{
						npc.setIsRunning(true);
						npc.setTarget(target);
						
						if (target.isInsideRadius(npc, 200, false, false))
						{
							npc.doCast(guard_skill);
						}
						else
						{
							npc.getAI().setIntention(CtrlIntention.FOLLOW, target);
						}
					}
					else
					{
						npc.deleteMe();
						if (targets.containsKey(npc))
						{
							targets.remove(npc);
						}
					}
				}
				else
				{
					final List<Player> result = new ArrayList<>();
					Player target = null;
					for (final Player pl : World.getInstance().getAroundPlayers(npc, 3000, 200))
					{
						if ((pl == null) || (pl.isAlikeDead()))
						{
							continue;
						}
						
						if ((pl.isInsideRadius(npc, 3000, true, false)) && (GeoEngine.getInstance().canSeeTarget(npc, pl)))
						{
							result.add(pl);
						}
					}
					if (!result.isEmpty())
					{
						target = result.get(getRandom(result.size() - 1));
					}
					
					if (target != null)
					{
						npc.setGlobalTitle(target.getName(null));
						npc.broadcastPacketToOthers(new NpcInfo.Info(npc, target));
						npc.setIsRunning(true);
						targets.put(npc, target);
					}
				}
				startQuestTimer("guard_action", 1000L, npc, null);
			}
		}
		return super.onAdvEvent(event, npc, player);
	}
	
	@Override
	public String onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		if (npc.getId() == ANAIS)
		{
			if (!FIGHTHING)
			{
				FIGHTHING = true;
				startQuestTimer("check_status", 50000L, npc, null);
			}
			else if ((getRandom(10) == 0) && (BURNERS_ENABLED < 4))
			{
				checkBurnerStatus(npc);
			}
		}
		return super.onAttack(npc, attacker, damage, isSummon);
	}
	
	@Override
	public String onSpellFinished(Npc npc, Player player, Skill skill)
	{
		if (npc.getId() == GUARD)
		{
			if (guards.contains(npc))
			{
				guards.remove(npc);
			}
			npc.doDie(npc);
			npc.deleteMe();
		}
		return super.onSpellFinished(npc, player, skill);
	}
	
	@Override
	public String onKill(Npc npc, Player killer, boolean isSummon)
	{
		if (npc.getId() == ANAIS)
		{
			stopFight();
		}
		
		return super.onKill(npc, killer, isSummon);
	}
	
	private synchronized void checkBurnerStatus(Npc anais)
	{
		switch (BURNERS_ENABLED)
		{
			case 0 :
				enableBurner(1);
				BURNERS_ENABLED = 1;
				break;
			case 1 :
				if (anais.getCurrentHp() > (anais.getMaxHp() * 0.75D))
				{
					break;
				}
				enableBurner(2);
				BURNERS_ENABLED = 2;
				break;
			case 2 :
				if (anais.getCurrentHp() > (anais.getMaxHp() * 0.5D))
				{
					break;
				}
				enableBurner(3);
				BURNERS_ENABLED = 3;
				break;
			case 3 :
				if (anais.getCurrentHp() > (anais.getMaxHp() * 0.25D))
				{
					break;
				}
				enableBurner(4);
				BURNERS_ENABLED = 4;
				break;
		}
	}
	
	private void enableBurner(int index)
	{
		if (!burners.isEmpty())
		{
			final Npc burner = burners.get(index - 1);
			if (burner != null)
			{
				burner.setDisplayEffect(1);
				startQuestTimer("burner_action", 1000L, burner, null);
			}
		}
	}
	
	private void stopFight()
	{
		if (!targets.isEmpty())
		{
			targets.clear();
		}
		
		if (!burners.isEmpty())
		{
			for (final Npc burner : burners)
			{
				if (burner != null)
				{
					burner.setDisplayEffect(2);
				}
			}
		}
		
		if (!guards.isEmpty())
		{
			for (final Npc guard : guards)
			{
				if (guard != null)
				{
					guard.deleteMe();
				}
			}
		}
		
		cancelQuestTimers("guard_action");
		cancelQuestTimers("burner_action");
		
		BURNERS_ENABLED = 0;
		FIGHTHING = false;
	}
	
	public static void main(String[] args)
	{
		new Anais(Anais.class.getSimpleName(), "ai");
	}
}
