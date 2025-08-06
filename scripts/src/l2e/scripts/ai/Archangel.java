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

import l2e.commons.util.Rnd;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.ai.npc.Fighter;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.network.serverpackets.MagicSkillUse;

/**
 * Created by LordWinter 16.10.2019
 */
public class Archangel extends Fighter
{
	private long _targetTask = 0L;

	public Archangel(Attackable actor)
	{
		super(actor);
		
		MAX_PURSUE_RANGE = 4000;
		actor.setIsGlobalAI(true);
	}

	@Override
	protected void onEvtSpawn()
	{
		final Attackable npc = getActiveChar();
		if (npc == null)
		{
			return;
		}
		
		_targetTask = System.currentTimeMillis();
		super.onEvtSpawn();
		ThreadPoolManager.getInstance().schedule(new Runnable()
		{
			@Override
			public void run()
			{
				for (final Creature target : World.getInstance().getAroundCharacters(npc, 3000, 200))
				{
					if (target != null)
					{
						if (target.getId() == 29020 && !target.isDead())
						{
							npc.addDamageHate(target, 0, 100);
						}
					}
				}
			}
		}, 2000);
	}

	@Override
	protected void thinkAttack()
	{
		final Attackable npc = getActiveChar();
		if (npc == null)
		{
			return;
		}

		if ((_targetTask + 20000L) < System.currentTimeMillis())
		{
			final List<Creature> alive = new ArrayList<>();
			for (final Creature target : World.getInstance().getAroundCharacters(npc, 2000, 200))
			{
				if (target != null)
				{
					if (!target.isDead() && !target.isInvisible())
					{
						if (target.getId() == 29021)
						{
							continue;
						}
					
						if (target.getId() == 29020 && Rnd.get(100) <= 50)
						{
							continue;
						}
						alive.add(target);
					}
				}
			}
			
			if (alive != null && !alive.isEmpty())
			{
				final Creature rndTarget = alive.get(Rnd.get(alive.size()));
				if (rndTarget != null && (rndTarget.getId() == 29020 || rndTarget.isPlayer()))
				{
					final Creature mostHate = npc.getAggroList().getMostHated();
					if (mostHate != null)
					{
						npc.addDamageHate(rndTarget, 0, (npc.getAggroList().getHating(mostHate) + 500));
					}
					else
					{
						npc.addDamageHate(rndTarget, 0, 2000);
					}
					npc.setTarget(rndTarget);
					setAttackTarget(rndTarget);
				}
			}
			_targetTask = System.currentTimeMillis();
		}
		super.thinkAttack();
	}

	@Override
	protected void onEvtAttacked(Creature attacker, int damage)
	{
		final Attackable npc = getActiveChar();
		if (npc != null)
		{
			if (!npc.isDead() && attacker != null)
			{
				if (attacker.getId() == 29020)
				{
					npc.addDamageHate(attacker, 10, 1000);
					setIntention(CtrlIntention.ATTACK, attacker);
				}
			}
		}
		super.onEvtAttacked(attacker, damage);
	}

	@Override
	public void returnHome()
	{
		final Attackable actor = getActiveChar();
		final Location sloc = actor.getSpawn().getLocation();

		actor.stopMove(null);
		actor.clearAggroList(true);

		_attackTimeout = Long.MAX_VALUE;
		setAttackTarget(null);

		changeIntention(CtrlIntention.ACTIVE, null, null);

		actor.broadcastPacketToOthers(new MagicSkillUse(actor, actor, 2036, 1, 500, 0));
		actor.teleToLocation(sloc.getX(), sloc.getY(), sloc.getZ(), true, actor.getReflection());
	}
}