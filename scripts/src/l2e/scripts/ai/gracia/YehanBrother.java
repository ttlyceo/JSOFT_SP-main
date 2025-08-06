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
package l2e.scripts.ai.gracia;

import l2e.commons.apache.util.ArrayUtils;
import l2e.commons.util.NpcUtils;
import l2e.commons.util.Rnd;
import l2e.gameserver.ai.model.CtrlEvent;
import l2e.gameserver.ai.npc.Fighter;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.entity.Reflection;
import l2e.gameserver.model.skills.effects.Effect;

public class YehanBrother extends Fighter
{
	private long _spawnTimer = 0;
	private static final int[] _minions = ArrayUtils.createAscendingArray(22509, 22512);

	public YehanBrother(Attackable actor)
	{
		super(actor);
	}

	@Override
	protected void onEvtSpawn()
	{
		super.onEvtSpawn();
		_spawnTimer = System.currentTimeMillis();
	}

	private Npc getBrother()
	{
		final Attackable actor = getActiveChar();
		if (actor == null)
		{
			return null;
		}
		
		int brotherId = 0;
		if (actor.getId() == 25665)
		{
			brotherId = 25666;
		}
		else if (actor.getId() == 25666)
		{
			brotherId = 25665;
		}
		
		final Reflection inst = ReflectionManager.getInstance().getReflection(actor.getReflectionId());
		if (inst != null)
		{
			for (final Npc npc : inst.getNpcs())
			{
				if (npc.getId() == brotherId)
				{
					return npc;
				}
			}
		}
		return null;
	}

	@Override
	protected void thinkAttack()
	{
		final Attackable actor = getActiveChar();
		if (actor == null)
		{
			return;
		}
		
		final Npc brother = getBrother();
		if (brother != null && !brother.isDead() && !actor.isInRange(brother, 300))
		{
			actor.makeTriggerCast(SkillsParser.getInstance().getInfo(6371, 1), actor);
		}
		else
		{
			removeInvul(actor);
		}
		if(_spawnTimer + 40000 < System.currentTimeMillis())
		{
			_spawnTimer = System.currentTimeMillis();
			final Npc mob = NpcUtils.spawnSingle(_minions[Rnd.get(_minions.length)], Location.findAroundPosition(actor, 100, 300), actor.getReflection(), 0);
			if (actor.getAI().getAttackTarget() != null)
			{
				mob.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, actor.getAI().getAttackTarget(), 1000);
			}
		}
		super.thinkAttack();
	}

	private void removeInvul(Npc npc)
	{
		for(final Effect e : npc.getEffectList().getAllEffects())
		{
			if(e.getSkill().getId() == 6371)
			{
				e.exit();
			}
		}
	}
}