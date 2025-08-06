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
package l2e.scripts.ai.dragonvalley;

import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.MonsterInstance;
import l2e.scripts.ai.AbstractNpcAI;

/**
 * Created by LordWinter 10.05.2012 Based on L2J Eternity-World
 */
public class DragonGuards extends AbstractNpcAI
{
	private static final int DRAGON_GUARD = 22852;
	private static final int DRAGON_MAGE = 22853;

	private static final int[] WALL_MONSTERS =
	{
	                DRAGON_GUARD, DRAGON_MAGE
	};

	public DragonGuards(String name, String descr)
	{
		super(name, descr);

		for (final int mobId : WALL_MONSTERS)
		{
			addAggroRangeEnterId(mobId);
			addAttackId(mobId);
		}
	}

	@Override
	public String onAggroRangeEnter(Npc npc, Player player, boolean isSummon)
	{
		if ((!npc.isCastingNow()) && (!npc.isAttackingNow()) && (!npc.isInCombat()) && (!player.isDead()))
		{
			npc.setIsImmobilized(false);
			npc.setRunning();
			((Attackable) npc).addDamageHate(player, 0, 999);
			((Attackable) npc).getAI().setIntention(CtrlIntention.ATTACK, player);
		}
		return super.onAggroRangeEnter(npc, player, isSummon);
	}

	@Override
	public String onAttack(Npc npc, Player player, int damage, boolean isSummon)
	{
		if (npc instanceof MonsterInstance)
		{
			for (final int mobId : WALL_MONSTERS)
			{
				if (mobId == npc.getId())
				{
					final MonsterInstance monster = (MonsterInstance) npc;
					monster.setIsImmobilized(false);
					monster.setRunning();
					if (!monster.getFaction().isNone())
					{
						final int factionRange = (int) (monster.getFaction().getRange() + monster.getColRadius());
						for (final Npc obj : World.getInstance().getAroundNpc(monster))
						{
							if (obj != null)
							{
								if (obj instanceof Attackable)
								{
									final Attackable called = (Attackable) obj;
									if (!called.getFaction().isNone() && !npc.isInFaction(called))
									{
										continue;
									}

									if (monster.isInsideRadius(called, factionRange, true, false) && called.hasAI())
									{
										called.setIsImmobilized(false);
										called.addDamageHate(player, 0, 999);
									}
								}
							}
						}
					}
					break;
				}
			}
		}
		return super.onAttack(npc, player, damage, isSummon);
	}

	public static void main(String[] args)
	{
		new DragonGuards(DragonGuards.class.getSimpleName(), "ai");
	}
}
