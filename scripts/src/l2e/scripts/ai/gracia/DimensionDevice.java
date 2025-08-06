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
package l2e.scripts.ai.gracia;

import java.util.concurrent.ScheduledFuture;

import l2e.commons.util.Rnd;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.ai.npc.Fighter;
import l2e.gameserver.data.parser.NpcsParser;
import l2e.gameserver.idfactory.IdFactory;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.instance.MonsterInstance;

/**
 * Created by LordWinter 27.12.2020
 */
public class DimensionDevice extends Fighter
{
	private static final int[] _mobs =
	{
	        22536, 22537, 22538, 22539, 22540, 22541, 22542, 22543, 22544, 22547, 22550, 22551, 22552, 22596
	};
	
	private ScheduledFuture<?> _spawnTask = null;
	
	public DimensionDevice(Attackable actor)
	{
		super(actor);
		
		actor.setIsGlobalAI(true);
	}
	
	@Override
	protected void onEvtSpawn()
	{
		if (getActiveChar().getReflectionId() == 0)
		{
			final long time = calcRespawnTime();
			switch (SoDDefenceStage.getDefenceStage())
			{
				case 0 :
					if (_spawnTask != null)
					{
						_spawnTask.cancel(false);
						_spawnTask = null;
					}
					getActiveChar().deleteMe();
					break;
				case 1 :
				case 2 :
				case 3 :
				case 4 :
					if (time > 0)
					{
						if (_spawnTask != null)
						{
							_spawnTask.cancel(false);
							_spawnTask = null;
						}
						_spawnTask = ThreadPoolManager.getInstance().scheduleAtFixedRate(new SpawnGuards(), Rnd.get(1000, 5000), time);
					}
					break;
			}
		}
		getActiveChar().getAI().enableAI();
		super.onEvtSpawn();
	}
	
	@Override
	protected boolean thinkActive()
	{
		final Attackable actor = getActiveChar();
		if (actor.isDead())
		{
			return true;
		}
		
		final int stage = SoDDefenceStage.getDefenceStage();
		if (stage == 0)
		{
			if (_spawnTask != null)
			{
				_spawnTask.cancel(false);
				_spawnTask = null;
			}
			getActiveChar().deleteMe();
			return true;
		}
		return true;
	}
	
	@Override
	protected void onEvtDead(Creature killer)
	{
		if (_spawnTask != null)
		{
			_spawnTask.cancel(false);
			_spawnTask = null;
		}
		super.onEvtDead(killer);
	}

	@Override
	protected void onEvtAttacked(Creature attacker, int damage)
	{
	}

	@Override
	protected void onEvtAggression(Creature target, int aggro)
	{
	}
	
	private class SpawnGuards implements Runnable
	{
		@Override
		public void run()
		{
			final MonsterInstance npc = new MonsterInstance(IdFactory.getInstance().getNextId(), NpcsParser.getInstance().getTemplate(_mobs[Rnd.get(_mobs.length)]));
			if (npc != null)
			{
				final Location loc = ((MonsterInstance) getActiveChar()).getMinionPosition();
				npc.setReflection(getActiveChar().getReflection());
				npc.setHeading(getActiveChar().getHeading());
				npc.setCurrentHpMp(npc.getMaxHp(), npc.getMaxMp());
				npc.spawnMe(loc.getX(), loc.getY(), loc.getZ());
			}
		}
	}
	
	private long calcRespawnTime()
	{
		switch (SoDDefenceStage.getDefenceStage())
		{
			case 1 :
				return Rnd.get(30, 60) * 1000;
			case 2 :
				return Rnd.get(20, 30) * 1000;
			case 3 :
				return Rnd.get(10, 20) * 1000;
			case 4 :
				return Rnd.get(2, 5) * 1000;
		}
		return -1;
	}
}