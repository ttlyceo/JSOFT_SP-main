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
import l2e.gameserver.ai.npc.Mystic;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Attackable;

/**
 * Created by LordWinter 10.12.2018
 */
public class WhiteAllosce extends Mystic
{
	private long _spawnTimer = 0L;
	private int _spawnCounter = 0;
	private final static long _spawnInterval = 60000L;
	private final static int _spawnLimit = 10;

	public WhiteAllosce(Attackable actor)
	{
		super(actor);
	}

	@Override
	protected void thinkAttack()
	{
		final Attackable actor = getActiveChar();
		
		if (_spawnTimer == 0)
		{
			_spawnTimer = System.currentTimeMillis();
		}
		if ((_spawnCounter < _spawnLimit) && ((_spawnTimer + _spawnInterval) < System.currentTimeMillis()))
		{
			NpcUtils.spawnSingle(18578, Location.findPointToStay(actor.getLocation(), 200, true), actor.getReflection(), 0);
			_spawnTimer = System.currentTimeMillis();
			_spawnCounter++;
		}
		super.thinkAttack();
	}
}