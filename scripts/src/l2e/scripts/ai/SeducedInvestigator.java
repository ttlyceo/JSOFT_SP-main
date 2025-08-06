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
import java.util.List;

import l2e.commons.apache.ArrayUtils;
import l2e.commons.util.Rnd;
import l2e.gameserver.ai.model.CtrlEvent;
import l2e.gameserver.ai.npc.Fighter;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.skills.Skill;

/**
 * Created by LordWinter 11.11.2019
 */
public class SeducedInvestigator extends Fighter
{
	private final int[] _allowedTargets =
	{
	        25653, 25654, 25655, 25656, 25657, 25658, 25659, 25660, 25661, 25662, 25663, 25664
	};
	
	private long _reuse = 0;

	public SeducedInvestigator(Attackable actor)
	{
		super(actor);
		
		actor.setIsImmobilized(true);
		actor.startHealBlocked(true);
	}

	@Override
	protected boolean thinkActive()
	{
		final Attackable actor = getActiveChar();
		if(actor.isDead())
		{
			return false;
		}

		for (final Npc around : World.getInstance().getAroundNpc(actor, 1000, 200))
		{
			if (around != null)
			{
				if (ArrayUtils.contains(_allowedTargets, around.getId()))
				{
					if (!around.isDead())
					{
						actor.getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, around, 300);
					}
				}
			}
		}

		if (Rnd.chance(50) && _reuse + 30000 < System.currentTimeMillis())
		{
			final List<Player> players = new ArrayList<>();
			for (final Player pl : World.getInstance().getAroundPlayers(actor, 500, 200))
			{
				players.add(pl);
			}
			
			if(players == null || players.size() < 1)
			{
				return false;
			}
			
			final Player player = players.get(Rnd.get(players.size()));
			if (player.getReflectionId() == actor.getReflectionId())
			{
				_reuse = System.currentTimeMillis();
				final int[] buffs = { 5970, 5971, 5972, 5973 };
				Skill skill = null;
				if (actor.getId() == 36562)
				{
					skill = SkillsParser.getInstance().getInfo(buffs[0], 1);
					if (player.getFirstEffect(skill) == null)
					{
						actor.setTarget(player);
						actor.doCast(skill);
					}
				}
				else if (actor.getId() == 36563)
				{
					skill = SkillsParser.getInstance().getInfo(buffs[1], 1);
					if (player.getFirstEffect(skill) == null)
					{
						actor.setTarget(player);
						actor.doCast(skill);
					}
				}
				else if (actor.getId() == 36564)
				{
					skill = SkillsParser.getInstance().getInfo(buffs[2], 1);
					if (player.getFirstEffect(skill) == null)
					{
						actor.setTarget(player);
						actor.doCast(skill);
					}
				}
				else
				{
					skill = SkillsParser.getInstance().getInfo(buffs[3], 1);
					if (player.getFirstEffect(skill) == null)
					{
						actor.setTarget(player);
						actor.doCast(skill);
					}
				}
			}
		}
		return true;
	}

	@Override
	protected void onEvtAttacked(Creature attacker, int damage)
	{
		final Attackable actor = getActiveChar();
		if(attacker == null)
		{
			return;
		}

		if(attacker.isPlayable())
		{
			return;
		}

		if (attacker.getId() == 25659 || attacker.getId() == 25660 || attacker.getId() == 25661)
		{
			actor.addDamageHate(attacker, 0, 20);
		}
		super.onEvtAttacked(attacker, damage);
	}

	@Override
	protected void onEvtAggression(Creature target, int aggro)
	{
		if(target.isPlayer() || target.isPet() || target.isSummon())
		{
			return;
		}
		super.onEvtAggression(target, aggro);
	}

	@Override
	public boolean checkAggression(Creature target)
	{
		if(target.isPlayable())
		{
			return false;
		}
		return super.checkAggression(target);
	}
}