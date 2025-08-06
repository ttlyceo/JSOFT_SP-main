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


import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.ai.npc.Mystic;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.serverpackets.NpcSay;

/**
 * Created by LordWinter 10.12.2018
 */
public class BladeOtisFollower extends Mystic
{
	private long _skillTimer = 0L;
	private final static long _skillInterval = 20000L;

	public BladeOtisFollower(Attackable actor)
	{
		super(actor);
	}
	
	@Override
	protected boolean thinkActive()
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
				if (npc.getId() == 18562)
				{
					boss = npc;
				}
			}
			
			if (boss != null)
			{
				actor.setTarget(boss);
				actor.doCast(SkillsParser.getInstance().getInfo(4209, 6));
				_skillTimer = System.currentTimeMillis();
				setIntention(CtrlIntention.ACTIVE);
				moveTo(Location.findPointToStay(actor, 300, true));
				actor.broadcastPacketToOthers(2000, new NpcSay(actor.getObjectId(), 0, actor.getId(), NpcStringId.THERES_NOT_MUCH_I_CAN_DO_BUT_I_WANT_TO_HELP_YOU));
			}
		}
		return super.thinkActive();
	}
	
	@Override
	protected void thinkAttack()
	{
		setIntention(CtrlIntention.ACTIVE);
	}
}