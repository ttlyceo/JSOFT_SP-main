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
package l2e.scripts.ai.primeval_isle;

import l2e.gameserver.ai.npc.Fighter;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.network.serverpackets.SocialAction;

public class SprigantStun extends Fighter
{
	private long _waitTime;
	private static final int TICK_IN_MILISECONDS = 15000;

	public SprigantStun(Attackable actor)
	{
		super(actor);
	}

	@Override
	protected boolean thinkActive()
	{
		final Attackable actor = getActiveChar();
		if (System.currentTimeMillis() > _waitTime)
		{
			actor.setTarget(actor);
			actor.doCast(SkillsParser.getInstance().getInfo(5085, 1));
			_waitTime = System.currentTimeMillis() + TICK_IN_MILISECONDS;
			actor.broadcastPacketToOthers(2000, new SocialAction(actor.getObjectId(), 1));
		}
		return super.thinkActive();
	}
}
