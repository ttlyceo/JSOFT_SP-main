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


import l2e.commons.util.Rnd;
import l2e.gameserver.ai.npc.Fighter;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.serverpackets.NpcSay;

/**
 * Created by LordWinter 10.12.2018
 */
public class BiletteFollower extends Fighter
{
	private long _skillTimer = 0L;
	private final static long _skillInterval = 20000L;

	public BiletteFollower(Attackable actor)
	{
		super(actor);
	}

	@Override
	protected void thinkAttack()
	{
		final Attackable actor = getActiveChar();

		if (_skillTimer == 0)
		{
			_skillTimer = System.currentTimeMillis();
		}
		if (((_skillTimer + _skillInterval) < System.currentTimeMillis()))
		{
			Npc boss = null;
			for (final Npc npc : World.getInstance().getAroundNpc(actor))
			{
				if (npc.getId() == 18573)
				{
					boss = npc;
				}
			}

			if (boss != null)
			{
				actor.setTarget(boss);
				actor.doCast(SkillsParser.getInstance().getInfo(4065, 6));
			}
			_skillTimer = System.currentTimeMillis();
		}
		super.thinkAttack();
	}

	@Override
	protected void onEvtAttacked(Creature attacker, int damage)
	{
		if (attacker != null)
		{
			if (Rnd.chance(10))
			{
				getActiveChar().broadcastPacketToOthers(2000, new NpcSay(getActiveChar().getObjectId(), 0, getActiveChar().getId(), NpcStringId.ARG_THE_PAIN_IS_MORE_THAN_I_CAN_STAND));
			}
			else if (Rnd.chance(3))
			{
				getActiveChar().broadcastPacketToOthers(2000, new NpcSay(getActiveChar().getObjectId(), 0, getActiveChar().getId(), NpcStringId.AHH_HOW_DID_HE_FIND_MY_WEAKNESS));
			}
		}
	}
}