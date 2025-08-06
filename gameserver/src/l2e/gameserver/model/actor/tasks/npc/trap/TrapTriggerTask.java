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
package l2e.gameserver.model.actor.tasks.npc.trap;

import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.model.actor.instance.TrapInstance;

public class TrapTriggerTask implements Runnable
{
	private final TrapInstance _trap;

	public TrapTriggerTask(TrapInstance trap)
	{
		_trap = trap;
	}

	@Override
	public void run()
	{
		try
		{
			_trap.doCast(_trap.getSkill());
			ThreadPoolManager.getInstance().schedule(new TrapUnsummonTask(_trap), _trap.getSkill().getHitTime() + 300);
		}
		catch (final Exception e)
		{
			_trap.unSummon();
		}
	}
}