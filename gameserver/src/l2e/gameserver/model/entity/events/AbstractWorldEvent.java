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
package l2e.gameserver.model.entity.events;

import java.util.ArrayList;
import java.util.List;

import l2e.commons.util.Rnd;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.MonsterInstance;
import l2e.gameserver.model.entity.events.model.template.WorldEventReward;
import l2e.gameserver.model.entity.events.model.template.WorldEventTemplate;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.taskmanager.ItemsAutoDestroy;

/**
 * Update by LordWinter 13.07.2020
 */
public abstract class AbstractWorldEvent extends Quest
{
	public AbstractWorldEvent(String name, String descr)
	{
		super(-1, name, descr);
	}
	
	public abstract boolean eventStart(long totalTime, boolean force);
	
	public abstract boolean eventStop();
	
	public abstract boolean isEventActive();

	public abstract WorldEventTemplate getEventTemplate();
	
	public abstract boolean isReloaded();
	
	public abstract void startTimerTask(long time);
	
	protected long calcEventStartTime(WorldEventTemplate template, boolean force)
	{
		if (template == null || (!template.isActivated() && !force) || template.getStartTimePattern() == null)
		{
			return 0L;
		}
		return template.getStartTimePattern().next(System.currentTimeMillis());
	}
	
	protected long calcEventStopTime(WorldEventTemplate template, boolean force)
	{
		if (template == null || (!template.isActivated() && !force) || template.getStopTimePattern() == null)
		{
			return 0L;
		}
		return template.getStopTimePattern().next(System.currentTimeMillis());
	}
	
	protected void checkTimerTask(long time)
	{
		if (time < System.currentTimeMillis())
		{
			return;
		}
		startTimerTask(time);
	}
	
	public static boolean isTakeRequestItems(Player player, WorldEventTemplate template, int variant)
	{
		if (template != null)
		{
			final List<WorldEventReward> requestItems = template.getVariantRequests().get(variant);
			if (requestItems != null && !requestItems.isEmpty())
			{
				for (final WorldEventReward request : requestItems)
				{
					if (player.getInventory().getItemByItemId(request.getId()) == null || player.getInventory().getItemByItemId(request.getId()).getCount() < (request.getMinCount()))
					{
						return false;
					}
				}
				
				for (final WorldEventReward request : requestItems)
				{
					player.destroyItemByItemId("takeItems", request.getId(), request.getMinCount(), player, true);
				}
				return true;
			}
		}
		return false;
	}
	
	public static void calcReward(Player player, WorldEventTemplate template, int variant)
	{
		if (template != null)
		{
			final List<WorldEventReward> rewards = template.getVariantRewards().get(variant);
			if (rewards != null && !rewards.isEmpty())
			{
				for (final WorldEventReward reward : rewards)
				{
					if (reward != null)
					{
						final long amount = reward.getMaxCount() != 0 ? Rnd.get(reward.getMinCount(), reward.getMaxCount()) : reward.getMinCount();
						player.addItem("Event-" + template.getId() + "", reward.getId(), amount, player, true);
					}
				}
			}
			
			final List<WorldEventReward> rndRewards = template.getVariantRandomRewards().get(variant);
			if (rndRewards != null && !rndRewards.isEmpty())
			{
				final WorldEventReward reward = rndRewards.get(Rnd.get(rndRewards.size()));
				if (reward != null)
				{
					final long amount = reward.getMaxCount() != 0 ? Rnd.get(reward.getMinCount(), reward.getMaxCount()) : reward.getMinCount();
					player.addItem("Event-" + template.getId() + "", reward.getId(), amount, player, true);
				}
			}
		}
	}
	
	public static void calcRandomReward(Player player, WorldEventTemplate template, int variant, double chance)
	{
		if (template != null)
		{
			final List<WorldEventReward> rewardList = new ArrayList<>();
			final List<WorldEventReward> rndRewards = template.getVariantRandomRewards().get(variant);
			if (rndRewards != null && !rndRewards.isEmpty())
			{
				for (final WorldEventReward reward : rndRewards)
				{
					if (reward != null && chance <= reward.getChance())
					{
						rewardList.add(reward);
					}
				}
			}
			
			if (rewardList != null && !rewardList.isEmpty())
			{
				final WorldEventReward reward = rewardList.get(Rnd.get(rewardList.size()));
				if (reward != null)
				{
					final long amount = reward.getMaxCount() != 0 ? Rnd.get(reward.getMinCount(), reward.getMaxCount()) : reward.getMinCount();
					player.addItem("Event-" + template.getId() + "", reward.getId(), amount, player, true);
				}
			}
		}
	}
	
	public static void calcRandomGroupReward(Npc npc, Player player, WorldEventTemplate template, int variant)
	{
		if (template != null)
		{
			final List<WorldEventReward> rndRewards = template.getVariantRewards().get(variant);
			if (rndRewards != null && !rndRewards.isEmpty())
			{
				double chance = 0;
				boolean isRecalc = false;
				for (final WorldEventReward reward : rndRewards)
				{
					if (reward != null && chance != reward.getChance())
					{
						if (Rnd.chance(reward.getChance()))
						{
							chance = reward.getChance();
							final long amount = reward.getMaxCount() != 0 ? Rnd.get(reward.getMinCount(), reward.getMaxCount()) : reward.getMinCount();
							((MonsterInstance) npc).dropItem(player, reward.getId(), (int) amount);
							isRecalc = true;
						}
					}
				}
				
				if (isRecalc)
				{
					ItemsAutoDestroy.getInstance().tryRecalcTime();
				}
			}
		}
	}
}