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

import java.util.ArrayList;
import java.util.List;

import l2e.commons.util.Rnd;
import l2e.gameserver.ai.npc.Mystic;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.templates.npc.MinionData;
import l2e.gameserver.model.actor.templates.npc.MinionTemplate;
import l2e.gameserver.model.skills.funcs.Func;
import l2e.gameserver.model.stats.Env;
import l2e.gameserver.model.stats.Stats;

/**
 * Created by LordWinter 22.11.2018
 */
public class GraveRobberSummoner extends Mystic
{
	private static final int[] Servitors =
	{
	        22683, 22684, 22685, 22686
	};

	private int _lastMinionCount = 1;
	private final List<Func> _stats = new ArrayList<>();

	private class FuncMulMinionCount extends Func
	{
		public FuncMulMinionCount(Stats stat, int order, Object owner)
		{
			super(stat, order, owner);
		}

		@Override
		public void calc(Env env)
		{
			env._value *= _lastMinionCount;
		}
	}

	public GraveRobberSummoner(Attackable actor)
	{
		super(actor);
	}

	@Override
	protected void onEvtSpawn()
	{
		super.onEvtSpawn();

		final var actor = getActiveChar();
		actor.getMinionList().addMinion(new MinionData(new MinionTemplate(Servitors[Rnd.get(Servitors.length)], Rnd.get(0, 2))), true);
		_lastMinionCount = Math.max(actor.getMinionList().getAliveMinions().size(), 1);
		if (_lastMinionCount > 1)
		{
			final var mDef = new FuncMulMinionCount(Stats.MAGIC_DEFENCE, 0x30, actor);
			actor.addStatFunc(mDef);
			_stats.add(mDef);
			final var pDef = new FuncMulMinionCount(Stats.POWER_DEFENCE, 0x30, actor);
			actor.addStatFunc(pDef);
			_stats.add(pDef);
		}
	}

	@Override
	protected void onEvtDead(Creature killer)
	{
		final var actor = getActiveChar();
		final var minionList = actor.getMinionList();
		if (minionList != null)
		{
			minionList.onMasterDelete();
			minionList.clearMinions();
		}
		
		_lastMinionCount = 1;
		if (!_stats.isEmpty())
		{
			for (final var funk : _stats)
			{
				if (funk != null)
				{
					actor.removeStatFunc(funk);
				}
			}
			_stats.clear();
		}
		super.onEvtDead(killer);
	}
}