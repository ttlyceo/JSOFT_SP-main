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
package l2e.scripts.ai.freya;


import l2e.gameserver.ai.model.CtrlEvent;
import l2e.gameserver.ai.npc.Fighter;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.entity.Reflection;

public class JiniaGuild extends Fighter
{
	private long _buffTimer = 0;
	
	public JiniaGuild(Attackable actor)
	{
		super(actor);
	}
	
	@Override
	protected boolean thinkActive()
	{
		final Npc npc = getSelectTarget();
		if (npc != null && !npc.isDead())
		{
			getActiveChar().getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, npc, 3000);
			getActiveChar().getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, npc, 300);
		}
		return true;
	}
	
	@Override
	protected void onEvtAttacked(Creature attacker, int damage)
	{
		if ((attacker == null) || attacker.isPlayable())
		{
			return;
		}

		if (!getActiveChar().isCastingNow() && (_buffTimer < System.currentTimeMillis()))
		{
			if (defaultThinkBuff(100))
			{
				_buffTimer = System.currentTimeMillis() + (30 * 1000L);
				return;
			}
		}
		super.onEvtAttacked(attacker, damage);
	}
	
	@Override
	protected boolean checkAggression(Creature target)
	{
		if (target.isPlayable())
		{
			return false;
		}
		
		final Npc npc = getSelectTarget();
		if (npc != null && !npc.isDead())
		{
			getActiveChar().getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, npc, 3000);
		}
		return super.checkAggression(target);
	}
	
	private Npc getSelectTarget()
	{
		final Reflection inst = ReflectionManager.getInstance().getReflection(getActiveChar().getReflectionId());
		if (inst != null)
		{
			for (final Npc n : inst.getNpcs())
			{
				if (n != null && (n.getId() == 29179 || n.getId() == 29180) && n.getReflectionId() == getActiveChar().getReflectionId())
				{
					return n;
				}
			}
		}
		else
		{
			if (getActiveChar().getReflectionId() == 0)
			{
				for (final Npc n : World.getInstance().getAroundNpc(getActiveChar(), 3000, 300))
				{
					if (n != null && (n.getId() == 29179 || n.getId() == 29180))
					{
						return n;
					}
				}
			}
		}
		return null;
	}
}
