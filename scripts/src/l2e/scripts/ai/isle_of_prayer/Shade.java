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
package l2e.scripts.ai.isle_of_prayer;


import l2e.commons.util.Rnd;
import l2e.gameserver.ai.npc.Fighter;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;

/**
 * Created by LordWinter 21.09.2018
 */
public class Shade extends Fighter
{
	private long _wait_timeout = 0;
	private boolean _wait = false;
	private static final int DESPAWN_TIME = 5 * 60 * 1000;

	public Shade(Attackable actor)
	{
		super(actor);
	}

	@Override
	protected boolean thinkActive()
	{
		final Attackable actor = getActiveChar();
		if (actor.isDead())
		{
			return false;
		}

		if (!_wait)
		{
			_wait = true;
			_wait_timeout = System.currentTimeMillis() + DESPAWN_TIME;
		}

		if ((_wait_timeout != 0) && _wait && (_wait_timeout < System.currentTimeMillis()))
		{
			actor.deleteMe();
			return true;
		}
		return super.thinkActive();
	}

	@Override
	protected void onEvtDead(Creature killer)
	{
		if (killer != null)
		{
			final Player player = killer.getActingPlayer();
			if (player != null)
			{
				final Attackable actor = getActiveChar();
				if (Rnd.chance(10))
				{
					actor.dropSingleItem(player, 9595, 1);
				}
			}
		}
		super.onEvtDead(killer);
	}
}
