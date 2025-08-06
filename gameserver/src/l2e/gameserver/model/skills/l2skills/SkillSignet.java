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
package l2e.gameserver.model.skills.l2skills;

import l2e.gameserver.data.parser.NpcsParser;
import l2e.gameserver.geodata.GeoEngine;
import l2e.gameserver.idfactory.IdFactory;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.instance.EffectPointInstance;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.skills.targets.TargetType;
import l2e.gameserver.model.stats.StatsSet;

public final class SkillSignet extends Skill
{
	public SkillSignet(StatsSet set)
	{
		super(set);
	}

	@Override
	public void useSkill(Creature caster, GameObject[] targets)
	{
		if (caster.isAlikeDead())
		{
			return;
		}

		final NpcTemplate template = NpcsParser.getInstance().getTemplate(getNpcId());
		final EffectPointInstance effectPoint = new EffectPointInstance(IdFactory.getInstance().getNextId(), template, caster, this);
		effectPoint.setCurrentHp(effectPoint.getMaxHp());
		effectPoint.setCurrentMp(effectPoint.getMaxMp());

		int x = caster.getX();
		int y = caster.getY();
		int z = caster.getZ();

		if (caster.isPlayer() && (getTargetType() == TargetType.GROUND))
		{
			final Location wordPosition = caster.getActingPlayer().getCurrentSkillWorldPosition();

			if (wordPosition != null)
			{
				x = wordPosition.getX();
				y = wordPosition.getY();
				z = wordPosition.getZ();
			}
		}
		z = GeoEngine.getInstance().getSpawnHeight(x, y, z);
		getEffects(caster, effectPoint, true);

		effectPoint.setIsInvul(true);
		effectPoint.spawnMe(x, y, z);
		if (effectPoint.getAI() != null)
		{
			effectPoint.getAI().enableAI();
		}
	}
}