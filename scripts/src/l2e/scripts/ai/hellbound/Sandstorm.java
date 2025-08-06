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
package l2e.scripts.ai.hellbound;

import l2e.commons.util.Rnd;
import l2e.gameserver.ai.DefaultAI;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.geodata.GeoEngine;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Playable;
import l2e.gameserver.model.skills.Skill;

/**
 * Created by LordWinter 15.11.2022
 */
public class Sandstorm extends DefaultAI
{
	private long _lastThrow = 0;
	private static final Skill _skill1 = SkillsParser.getInstance().getInfo(5435, 1);
	private static final Skill _skill2 = SkillsParser.getInstance().getInfo(5494, 1);

	public Sandstorm(Attackable actor)
	{
		super(actor);
		
		actor.setUndying(true);
		actor.setIsIgnoreSearch(true);
	}

	@Override
	protected boolean thinkActive()
	{
		final Attackable actor = getActiveChar();
		if (actor == null)
		{
			return false;
		}
		
		if(_lastThrow + 5000 < System.currentTimeMillis())
		{
			for(final Playable target : World.getInstance().getAroundPlayables(actor, 200, 200))
			{
				if (target != null && !target.isAlikeDead() && !target.isInvul() && (target.isVisible() || (!target.isVisible() && target.isSilentMoving())) && GeoEngine.getInstance().canSeeTarget(actor, target))
				{
					actor.setTarget(target);
					actor.doCast(_skill1);
					if (Rnd.chance(80))
					{
						target.doCast(_skill2);
					}
					_lastThrow = System.currentTimeMillis();
					break;
				}
			}
		}
		actor.clearAggroList(false);
		return super.thinkActive();
	}

	@Override
	protected void thinkAttack()
	{
	}
	
	@Override
	protected void onIntentionAttack(Creature target, boolean shift)
	{
	}
	
	@Override
	protected void onEvtAttacked(Creature attacker, int damage)
	{
	}
	
	@Override
	protected void onEvtAggression(Creature attacker, int aggro)
	{
	}

	@Override
	protected void onEvtClanAttacked(Creature attacker, int damage)
	{
	}
	
	@Override
	protected boolean maybeMoveToHome()
	{
		final var actor = getActiveChar();
		if (actor.isDead())
		{
			return false;
		}
		
		final var sloc = actor.getSpawnedLoc();
		if (sloc == null || Rnd.chance(30))
		{
			return false;
		}
		
		final var pos = Location.findPointToStay(actor, sloc, 150, 300, true);
		if (pos != null)
		{
			actor.setRunning();
			moveTo(Location.findPointToStay(pos, 40, true));
		}
		return true;
	}
}