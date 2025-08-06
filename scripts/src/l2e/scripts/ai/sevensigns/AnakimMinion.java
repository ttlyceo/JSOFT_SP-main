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
package l2e.scripts.ai.sevensigns;

import l2e.commons.apache.ArrayUtils;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.ai.model.CtrlEvent;
import l2e.gameserver.ai.npc.Fighter;
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Npc;

/**
 * Created by LordWinter 10.12.2018
 */
public class AnakimMinion extends Fighter
{
	private final int[] _enemies =
	{
	        32717, 32716
	};

	public AnakimMinion(Attackable actor)
	{
		super(actor);
	}

	@Override
	protected void onEvtSpawn()
	{
		getActiveChar().setIsNoRndWalk(true);
		getActiveChar().setIsImmobilized(true);
		super.onEvtSpawn();
		ThreadPoolManager.getInstance().schedule(new Attack(), 3000);
	}
	
	@Override
	protected boolean thinkActive()
	{
		for (final Npc npc : World.getInstance().getAroundNpc(getActiveChar()))
		{
			if (ArrayUtils.contains(_enemies, npc.getId()))
			{
				getActiveChar().getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, npc, 100000);
				getActiveChar().getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, npc, 100000);
			}
		}
		return true;
	}

	public class Attack implements Runnable
	{
		@Override
		public void run()
		{
			getActiveChar().setIsImmobilized(false);
		}
	}
}