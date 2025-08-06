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
package l2e.gameserver.handler.effecthandlers.impl;

import java.util.List;

import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.reward.RewardItem;
import l2e.gameserver.model.service.BotFunctions;
import l2e.gameserver.model.skills.effects.Effect;
import l2e.gameserver.model.skills.effects.EffectTemplate;
import l2e.gameserver.model.skills.effects.EffectType;
import l2e.gameserver.model.stats.Env;

public class Sweeper extends Effect
{
	public Sweeper(Env env, EffectTemplate template)
	{
		super(env, template);
	}
	
	@Override
	public boolean onStart()
	{
		if ((getEffector() == null) || (getEffected() == null) || !getEffector().isPlayer() || !getEffected().isAttackable())
		{
			return false;
		}

		final Player player = getEffector().getActingPlayer();
		final Attackable monster = (Attackable) getEffected();
		if (!monster.checkSpoilOwner(player, false))
		{
			return false;
		}

		final List<RewardItem> items = monster.takeSweep();
		if (items == null)
		{
			return false;
		}
		
		if (!player.getInventory().checkInventorySlotsAndWeight(monster.getSpoilLootItems(), false, false))
		{
			return false;
		}

		for (final RewardItem item : items)
		{
			if (player.isInParty() && (player.getParty().getLootDistribution() == 2 || BotFunctions.getInstance().isAutoSpoilEnable(player)))
			{
				player.getParty().distributeItem(player, item._itemId, item._count, true, monster);
				continue;
			}
			player.addItem("Sweeper", item._itemId, item._count, getEffected(), true);
		}
		return true;
	}

	@Override
	public EffectType getEffectType()
	{
		return EffectType.NONE;
	}
}