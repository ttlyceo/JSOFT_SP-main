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


import l2e.gameserver.ai.npc.Mystic;
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.network.serverpackets.MagicSkillUse;

/**
 * Created by LordWinter 10.12.2018
 */
public class Anakim extends Mystic
{
	private long _lastSkillTime = 0;

	public Anakim(Attackable actor)
	{
		super(actor);
	}

	@Override
	protected void onEvtSpawn()
	{
		getActiveChar().setIsNoRndWalk(true);
		getActiveChar().setIsInvul(true);
		super.onEvtSpawn();
	}

	@Override
	protected boolean thinkActive()
	{
		if (_lastSkillTime < System.currentTimeMillis())
		{
			if (getLilith() != null)
			{
				getActiveChar().broadcastPacketToOthers(2000, new MagicSkillUse(getActiveChar(), getLilith(), 6191, 1, 5000, 10));
			}
			_lastSkillTime = System.currentTimeMillis() + 6500;
		}
		return true;
	}

	private Npc getLilith()
	{
		Npc lilith = null;
		for (final Npc npc : World.getInstance().getAroundNpc(getActiveChar()))
		{
			if (npc.getId() == 32715)
			{
				lilith = npc;
			}
		}
		return lilith;
	}

	@Override
	protected void onEvtAttacked(Creature attacker, int damage)
	{
	}

	@Override
	protected void onEvtAggression(Creature attacker, int aggro)
	{
	}
}