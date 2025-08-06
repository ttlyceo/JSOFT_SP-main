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

import java.util.ArrayList;
import java.util.List;

import l2e.commons.util.Rnd;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.ai.npc.Mystic;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Player;

/**
 * Created by LordWinter 10.12.2018
 */
public class WhiteAllosceFollower extends Mystic
{
	private long _skillTimer = 0L;
	private final static long _skillInterval = 15000L;
	
	public WhiteAllosceFollower(Attackable actor)
	{
		super(actor);

		actor.setIsInvul(true);
	}
	
	@Override
	protected boolean thinkActive()
	{
		final Attackable actor = getActiveChar();

		if ((_skillTimer + _skillInterval) < System.currentTimeMillis())
		{
			final List<Player> aggressionList = new ArrayList<>();
			for (final Player p : World.getInstance().getAroundPlayers(actor, 1000, 200))
			{
				if (!p.isDead() && !p.isInvisible())
				{
					actor.addDamageHate(p, 0, 10);
					aggressionList.add(p.getActingPlayer());
				}
			}
			
			if (!aggressionList.isEmpty())
			{
				final Player aggressionTarget = aggressionList.get(Rnd.get(aggressionList.size()));
				if (aggressionTarget != null)
				{
					actor.setTarget(aggressionTarget);
					actor.doCast(SkillsParser.getInstance().getInfo(5624, 1));
				}
			}
			setIntention(CtrlIntention.ACTIVE);
			moveTo(Location.findPointToStay(actor, 400, true));
			_skillTimer = System.currentTimeMillis() + Rnd.get(1L, 5000L);
		}
		return super.thinkActive();
	}
	
	@Override
	protected void thinkAttack()
	{
		setIntention(CtrlIntention.ACTIVE);
	}
}