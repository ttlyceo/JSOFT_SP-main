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
package l2e.scripts.ai.selmahum;

import l2e.commons.apache.ArrayUtils;
import l2e.commons.util.Rnd;
import l2e.gameserver.ai.npc.Fighter;
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.network.serverpackets.SocialAction;

/**
 * Created by LordWinter 08.12.2018
 */
public class SelMahumTraining extends Fighter
{
	private static final int[] _recruits =
	{
	        22780, 22782, 22783, 22784, 22785
	};
	private long _waitTime = 0;

	public SelMahumTraining(Attackable actor)
	{
		super(actor);
	}

	@Override
	public boolean thinkActive()
	{
		final Attackable actor = getActiveChar();

		if (System.currentTimeMillis() > _waitTime)
		{
			_waitTime = System.currentTimeMillis() + (Rnd.get(10, 30) * 1000L);
			
			actor.broadcastPacketToOthers(2000, new SocialAction(actor.getObjectId(), 7));
			switch (Rnd.get(1, 3))
			{
				case 1 :
					for (final Npc npc : World.getInstance().getAroundNpc(actor, (int) (700 + actor.getColRadius()), 200))
					{
						if (npc.isMonster() && ArrayUtils.contains(_recruits, npc.getId()))
						{
							npc.broadcastPacketToOthers(2000, new SocialAction(npc.getObjectId(), 7));
						}
					}
					break;
				case 2 :
					for (final Npc npc : World.getInstance().getAroundNpc(actor, (int) (700 + actor.getColRadius()), 200))
					{
						if (npc.isMonster() && ArrayUtils.contains(_recruits, npc.getId()))
						{
							npc.broadcastPacketToOthers(2000, new SocialAction(npc.getObjectId(), 4));
						}
					}
					break;
				case 3 :
					for (final Npc npc : World.getInstance().getAroundNpc(actor, (int) (700 + actor.getColRadius()), 200))
					{
						if (npc.isMonster() && ArrayUtils.contains(_recruits, npc.getId()))
						{
							npc.broadcastPacketToOthers(2000, new SocialAction(npc.getObjectId(), 5));
						}
					}
					break;
			}
		}
		return false;
	}
}