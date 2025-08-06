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
package l2e.gameserver.model;

import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2e.commons.util.Util;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.geodata.GeoEngine;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.skills.effects.Effect;

public final class FusionSkill
{
	protected static final Logger _log = LoggerFactory.getLogger(FusionSkill.class);
	
	protected int _skillCastRange;
	protected int _fusionId;
	protected int _fusionLevel;
	protected Creature _caster;
	protected Creature _target;
	protected Future<?> _geoCheckTask;

	public Creature getCaster()
	{
		return _caster;
	}
	
	public Creature getTarget()
	{
		return _target;
	}

	public FusionSkill(Creature caster, Creature target, Skill skill)
	{
		_skillCastRange = skill.getCastRange();
		_caster = caster;
		_target = target;
		_fusionId = skill.getTriggeredId();
		_fusionLevel = skill.getTriggeredLevel();
		
		final Effect effect = _target.getFirstEffect(_fusionId);
		if (effect != null)
		{
			effect.increaseEffect();
		}
		else
		{
			final Skill force = SkillsParser.getInstance().getInfo(_fusionId, _fusionLevel);
			if (force != null)
			{
				force.getEffects(_caster, _target, null, true);
			}
			else
			{
				_log.warn("Triggered skill [" + _fusionId + ";" + _fusionLevel + "] not found!");
			}
		}
		_geoCheckTask = ThreadPoolManager.getInstance().scheduleAtFixedRate(new GeoCheckTask(), 1000, 1000);
	}
	
	public void onCastAbort()
	{
		_caster.setFusionSkill(null);
		final Effect effect = _target.getFirstEffect(_fusionId);
		if (effect != null)
		{
			effect.decreaseForce();
		}
		
		_geoCheckTask.cancel(true);
	}
	
	public class GeoCheckTask implements Runnable
	{
		@Override
		public void run()
		{
			try
			{
				if (!Util.checkIfInRange(_skillCastRange, _caster, _target, true))
				{
					_caster.abortCast();
				}
				
				if (!GeoEngine.getInstance().canSeeTarget(_caster, _target))
				{
					_caster.abortCast();
				}
			}
			catch (final Exception e)
			{}
		}
	}
}