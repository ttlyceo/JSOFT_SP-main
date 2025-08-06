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

import l2e.gameserver.ai.model.CtrlEvent;
import l2e.gameserver.ai.npc.Fighter;
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Npc;

/**
 * Created by LordWinter 22.11.2018
 */
public class SolinaKnight extends Fighter
{
	private Npc scarecrow = null;

	public SolinaKnight(Attackable actor)
	{
		super(actor);
		
		actor.setIsGlobalAI(true);
	}

	@Override
	protected void onEvtSpawn()
	{
		getActiveChar().getAI().enableAI();
		super.onEvtSpawn();
	}

	@Override
	protected boolean thinkActive()
	{
		if (scarecrow == null)
		{
			for (final Npc npc : World.getInstance().getAroundNpc(getActiveChar(), 400, 200))
			{
				if (npc.getId() == 18912)
				{
					if ((scarecrow == null) || (getActiveChar().getDistance3D(npc) < getActiveChar().getDistance3D(scarecrow)))
					{
						scarecrow = npc;
					}
				}
			}
		}
		else
		{
			getActiveChar().getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, scarecrow, 1);
			return true;
		}
		return super.thinkActive();
	}
}