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

import java.util.concurrent.ScheduledFuture;

import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.ai.npc.Fighter;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;

/**
 * Created by LordWinter 15.11.2018
 */
public class BodyDestroyer extends Fighter
{
	private ScheduledFuture<?> _destroyTask;
	
	public BodyDestroyer(Attackable actor)
	{
		super(actor);
	}
	
	@Override
	protected void onEvtAttacked(Creature attacker, int damage)
	{
		final Attackable actor = getActiveChar();
		if (attacker != null && actor.isScriptValue(0))
		{
			actor.setScriptValue(1);
			actor.addDamageHate(attacker, 0, 9999);
			actor.setTarget(attacker);
			actor.doCast(SkillsParser.getInstance().getInfo(5256, 1));
			attacker.setCurrentHp(1);
			_destroyTask = ThreadPoolManager.getInstance().schedule(new Destroy(attacker), 30000L);
		}
		super.onEvtAttacked(attacker, damage);
	}
	
	@Override
	protected void onEvtDead(Creature killer)
	{
		if (_destroyTask != null)
		{
			_destroyTask.cancel(false);
		}
		killer.stopSkillEffects(5256);
		super.onEvtDead(killer);
	}
	
	private class Destroy implements Runnable
	{
		Creature _attacker;
		
		public Destroy(Creature attacker)
		{
			_attacker = attacker;
		}
		
		@Override
		public void run()
		{
			_attacker.setCurrentHp(1);
		}
	}
}
