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


import l2e.gameserver.ai.npc.Fighter;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.serverpackets.NpcSay;

/**
 * Created by LordWinter 15.09.2018
 */
public class Gargos extends Fighter
{
	private long _lastFire;

	public Gargos(Attackable actor)
	{
		super(actor);
	}

	@Override
	protected boolean thinkActive()
	{
		return super.thinkActive() || thinkFire();
	}

	protected boolean thinkFire()
	{
		if ((System.currentTimeMillis() - _lastFire) > 60000L)
		{
			final Attackable actor = getActiveChar();
			actor.broadcastPacketToOthers(1000, new NpcSay(actor.getObjectId(), 0, actor.getId(), NpcStringId.STEP_FORWARD_YOU_WORTHLESS_CREATURES_WHO_CHALLENGE_MY_AUTHORITY));
			actor.setTarget(actor);
			actor.doCast(SkillsParser.getInstance().getInfo(5705, 1));
			_lastFire = System.currentTimeMillis();
			return true;
		}
		return false;
	}
}
