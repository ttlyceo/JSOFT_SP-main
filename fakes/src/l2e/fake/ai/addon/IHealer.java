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
package l2e.fake.ai.addon;

import java.util.List;
import java.util.stream.Collectors;

import l2e.fake.FakePlayer;
import l2e.fake.ai.CombatAI;
import l2e.fake.model.HealingSpell;
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.Creature;

public interface IHealer
{
	default void tryTargetingLowestHpTargetInRadius(FakePlayer player, int radius)
	{
		if (player.getTarget() == null)
		{
			final List<Creature> targets = World.getInstance().getAroundPlayers(player, radius, 200).stream().filter(x -> x.isFakePlayer() && !x.isDead()).collect(Collectors.toList());

			if (!player.isDead())
			{
				targets.add(player);
			}
			
			final List<Creature> sortedTargets = targets.stream().sorted((x1, x2) -> Double.compare(x1.getCurrentHp(), x2.getCurrentHp())).collect(Collectors.toList());
			if (!sortedTargets.isEmpty())
			{
				final Creature target = sortedTargets.get(0);
				player.setTarget(target);
			}
		}
		else
		{
			if (((Creature) player.getTarget()).isDead())
			{
				player.setTarget(null);
			}
		}
	}

	default void tryHealingTarget(FakePlayer player)
	{
		if (player.getTarget() != null && player.getTarget() instanceof Creature)
		{
			final Creature target = (Creature) player.getTarget();
			if (player.getFakeAi() instanceof CombatAI)
			{
				final HealingSpell skill = ((CombatAI) player.getFakeAi()).getRandomAvaiableHealingSpellForTarget();
				if (skill != null)
				{
					switch (skill.getCondition())
					{
						case LESSHPPERCENT :
							final double currentHpPercentage = Math.round(100.0 / target.getMaxHp() * target.getCurrentHp());
							if (currentHpPercentage <= skill.getConditionValue())
							{
								player.getFakeAi().castSpell(player.getKnownSkill(skill.getSkillId()), !player.getKnownSkill(skill.getSkillId()).isOffensive());
							}
							break;
						default :
							break;
					}
				}
			}
		}
	}
}
