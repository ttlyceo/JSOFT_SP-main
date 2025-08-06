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
package l2e.scripts.ai.kamaloka;


import l2e.commons.util.NpcUtils;
import l2e.gameserver.ai.model.CtrlEvent;
import l2e.gameserver.ai.npc.Fighter;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.instance.MonsterInstance;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.serverpackets.NpcSay;

/**
 * Created by LordWinter 10.12.2018
 */
public class OlAriosh extends Fighter
{
	private long _spawnTimer = 0L;
	private final static long _spawnInterval = 60000L;
	MonsterInstance follower = null;

	public OlAriosh(Attackable actor)
	{
		super(actor);
	}
	
	@Override
	protected void thinkAttack()
	{
		final Attackable actor = getActiveChar();

		if (((follower == null) || follower.isDead()) && ((_spawnTimer + _spawnInterval) < System.currentTimeMillis()))
		{
			follower = NpcUtils.spawnSingle(18556, Location.findPointToStay(actor.getLocation(), 200, true), actor.getReflection(), 0);
			follower.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, getAttackTarget(), 1000);
			_spawnTimer = System.currentTimeMillis();
			actor.broadcastPacketToOthers(2000, new NpcSay(actor.getObjectId(), 0, actor.getId(), NpcStringId.WHAT_ARE_YOU_DOING_HURRY_UP_AND_HELP_ME));
		}
		super.thinkAttack();
	}
}