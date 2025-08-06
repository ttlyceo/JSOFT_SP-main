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
package l2e.gameserver.model.skills.conditions;

import l2e.gameserver.Config;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.stats.Env;
import l2e.gameserver.network.SystemMessageId;

public class ConditionPlayerCanSweep extends Condition
{
	private final boolean _val;

	public ConditionPlayerCanSweep(boolean val)
	{
		_val = val;
	}
	
	@Override
	public boolean testImpl(Env env)
	{
		boolean canSweep = false;
		if (env.getPlayer() != null)
		{
			final Player sweeper = env.getPlayer();
			final Skill sweep = env.getSkill();
			if (sweep != null)
			{
				final GameObject[] targets = sweep.getTargetList(sweeper);
				if (targets != null)
				{
					if (sweep.getId() == 444)
					{
						Attackable target;
						for (final GameObject objTarget : targets)
						{
							if (objTarget instanceof Attackable)
							{
								target = (Attackable) objTarget;
								if (target.isDead() && target.isSpoil())
								{
									if (!target.checkSpoilOwner(sweeper, false) || target.isOldCorpse(sweeper, (Config.MAX_SWEEPER_TIME * 1000), false))
									{
										continue;
									}
									canSweep = sweeper.getInventory().checkInventorySlotsAndWeight(target.getSpoilLootItems(), true, true);
								}
							}
						}
					}
					else
					{
						Attackable target;
						for (final GameObject objTarget : targets)
						{
							if (objTarget instanceof Attackable)
							{
								target = (Attackable) objTarget;
								if (target.isDead())
								{
									if (target.isSpoil())
									{
										canSweep = target.checkSpoilOwner(sweeper, true);
										canSweep &= !target.isOldCorpse(sweeper, (Config.MAX_SWEEPER_TIME * 1000), true);
										canSweep &= sweeper.getInventory().checkInventorySlotsAndWeight(target.getSpoilLootItems(), true, true);
									}
									else
									{
										sweeper.sendPacket(SystemMessageId.SWEEPER_FAILED_TARGET_NOT_SPOILED);
									}
								}
							}
						}
					}
				}
			}
		}
		return (_val == canSweep);
	}
}