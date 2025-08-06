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
package l2e.gameserver.model.reward;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2e.commons.util.Rnd;
import l2e.gameserver.Config;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.npc.champion.ChampionTemplate;
import l2e.gameserver.model.stats.Stats;

public class RewardGroup implements Cloneable
{
	protected static final Logger _log = LoggerFactory.getLogger(RewardGroup.class);
	
	private double _chance;
	private final int _min;
	private final int _max;
	private boolean _isAdena = false;
	private boolean _notRate = false;
	private boolean _notUseMode = false;
	private final List<RewardData> _items = new ArrayList<>();
	
	public RewardGroup(double chance, int min, int max)
	{
		setChance(chance);
		_min = min;
		_max = max;
	}
	
	public boolean isNotUseMode()
	{
		return _notUseMode;
	}
	
	public int getMin()
	{
		return _min;
	}
	
	public int getMax()
	{
		return _max;
	}
	
	public void setIsNotUseMode(boolean notUseMode)
	{
		_notUseMode = notUseMode;
	}

	public boolean notRate()
	{
		return _notRate;
	}

	public void setNotRate(boolean notRate)
	{
		_notRate = notRate;
	}

	public double getChance()
	{
		return _chance;
	}

	public void setChance(double chance)
	{
		_chance = chance;
	}

	public boolean isAdena()
	{
		return _isAdena;
	}

	public void setIsAdena(boolean isAdena)
	{
		_isAdena = isAdena;
	}

	public void addData(RewardData item)
	{
		if (item.getItem().isAdena())
		{
			_isAdena = true;
		}
		
		if (item.getItem().getId() == 57 && Config.ADENA_FIXED_CHANCE > 0)
		{
			setChance(Config.ADENA_FIXED_CHANCE * 10000);
		}
		_items.add(item);
	}
	
	public List<RewardData> getItems()
	{
		return _items;
	}

	@Override
	public RewardGroup clone()
	{
		final RewardGroup ret = new RewardGroup(_chance, _min, _max);
		for (final RewardData i : _items)
		{
			ret.addData(i.clone());
		}
		return ret;
	}

	public List<RewardItem> roll(RewardType type, Player player, double penaltyMod, double rateMod, Attackable npc)
	{
		switch (type)
		{
			case NOT_RATED_GROUPED :
			case NOT_RATED_NOT_GROUPED :
			case NOT_RATED_GROUPED_EVENT :
				return rollItems(player, null, penaltyMod, penaltyMod, 1.0, 1, false, 1, false);
			case SWEEP :
				final double sweepRate = Config.RATE_DROP_SPOIL * rateMod;
				int spoilPerGroup = player.getPremiumBonus().getMaxSpoilItemsFromOneGroup();
				double premiumSweepRate = (player.isInParty() && Config.PREMIUM_PARTY_RATE ? player.getParty().getDropSpoil() : player.getPremiumBonus().getDropSpoil());
				if (npc != null && npc.getChampionTemplate() != null)
				{
					premiumSweepRate *= npc.getChampionTemplate().spoilDropMultiplier;
					spoilPerGroup = npc.getChampionTemplate().maxSpoilItemsFromOneGroup > spoilPerGroup ? npc.getChampionTemplate().maxSpoilItemsFromOneGroup : spoilPerGroup;
				}
				return rollItems(player, npc != null ? npc.getChampionTemplate() : null, penaltyMod, penaltyMod, player.calcStat(Stats.SPOIL_MULTIPLIER, sweepRate, player, null), premiumSweepRate, Config.ALLOW_MODIFIER_FOR_SPOIL, spoilPerGroup, false);
			case RATED_GROUPED :
				if (_isAdena)
				{
					double adenaRate = Config.RATE_DROP_ADENA * rateMod;
					final double premiumAdenaRate = (player.isInParty() && Config.PREMIUM_PARTY_RATE ? player.getParty().getDropAdena() : player.getPremiumBonus().getDropAdena());
					if (npc != null && npc.getChampionTemplate() != null)
					{
						adenaRate *= npc.getChampionTemplate().adenaMultiplier;
					}
					return rollAdena(player, npc != null ? npc.getChampionTemplate() : null, penaltyMod, player.calcStat(Stats.ADENA_MULTIPLIER, adenaRate, player, null), premiumAdenaRate);
				}

				if (npc != null && npc.isRaid())
				{
					final double dropRate = (npc.isEpicRaid() ? Config.RATE_DROP_EPICBOSS : Config.RATE_DROP_RAIDBOSS) * rateMod;
					int raidDropPerGroup = player.getPremiumBonus().getMaxRaidDropItemsFromOneGroup();
					double premiumRaidRate = npc.isEpicRaid() ? (player.isInParty() && Config.PREMIUM_PARTY_RATE ? player.getParty().getDropEpics() : player.getPremiumBonus().getDropEpics()) : (player.isInParty() && Config.PREMIUM_PARTY_RATE ? player.getParty().getDropRaids() : player.getPremiumBonus().getDropRaids());
					if (npc != null && npc.getChampionTemplate() != null)
					{
						premiumRaidRate *= npc.getChampionTemplate().itemDropMultiplier;
						raidDropPerGroup = npc.getChampionTemplate().maxRaidDropItemsFromOneGroup > raidDropPerGroup ? npc.getChampionTemplate().maxRaidDropItemsFromOneGroup : raidDropPerGroup;
					}
					return rollItems(player, npc != null ? npc.getChampionTemplate() : null, penaltyMod, penaltyMod, player.calcStat(Stats.RAID_REWARD_MULTIPLIER, dropRate, player, null), premiumRaidRate, Config.ALLOW_MODIFIER_FOR_RAIDS, player.getPremiumBonus().getMaxRaidDropItemsFromOneGroup(), true);
				}

				if (npc != null && npc.isSiegeGuard())
				{
					final double dropRate = Config.RATE_DROP_SIEGE_GUARD * rateMod;
					final double premiumGRate = (player.isInParty() && Config.PREMIUM_PARTY_RATE ? player.getParty().getDropSiege() : player.getPremiumBonus().getDropSiege());
					return rollItems(player, npc != null ? npc.getChampionTemplate() : null, penaltyMod, penaltyMod, player.calcStat(Stats.REWARD_MULTIPLIER, dropRate, player, null), premiumGRate, Config.ALLOW_MODIFIER_FOR_DROP, player.getPremiumBonus().getMaxDropItemsFromOneGroup(), false);
				}
				
				final double dropRate = Config.RATE_DROP_ITEMS * rateMod;
				int dropPerGroup = player.getPremiumBonus().getMaxDropItemsFromOneGroup();
				double premiumDRate = player.isInParty() && Config.PREMIUM_PARTY_RATE ? player.getParty().getDropItems() : player.getPremiumBonus().getDropItems();
				if (npc != null)
				{
					if (npc.getChampionTemplate() != null)
					{
						premiumDRate *= npc.getChampionTemplate().itemDropMultiplier;
						dropPerGroup = npc.getChampionTemplate().maxDropItemsFromOneGroup > dropPerGroup ? npc.getChampionTemplate().maxDropItemsFromOneGroup : dropPerGroup;
					}
				}
				return rollItems(player, npc != null ? npc.getChampionTemplate() : null, penaltyMod, penaltyMod, player.calcStat(Stats.REWARD_MULTIPLIER, dropRate, player, null), premiumDRate, Config.ALLOW_MODIFIER_FOR_DROP, dropPerGroup, false);
			default :
				return Collections.emptyList();
		}
	}
	
	private List<RewardItem> rollAdena(Player player, ChampionTemplate championTemplate, double mod, double rate, double premiumBonus)
	{
		if (notRate())
		{
			mod = Math.min(mod, 1.);
			rate = 1.;
		}
		
		final double adenaModifier = championTemplate != null ? championTemplate.adenaChanceModifier : 1.0;
		if (mod > 0 && rate > 0)
		{
			if ((getChance() * player.getPremiumBonus().getGroupRate() * adenaModifier) > Rnd.get(RewardList.MAX_CHANCE))
			{
				final List<RewardItem> rolledItems = new ArrayList<>();
				for (final RewardData data : getItems())
				{
					final RewardItem item = data.rollAdena(player, mod, rate, premiumBonus);
					if (item != null)
					{
						rolledItems.add(item);
					}
				}
				
				if (rolledItems.isEmpty())
				{
					return Collections.emptyList();
				}
				
				final List<RewardItem> result = new ArrayList<>();
				for (int i = 0; i < Config.MAX_DROP_ITEMS_FROM_ONE_GROUP; i++)
				{
					final RewardItem rolledItem = Rnd.get(rolledItems);
					if (rolledItems.remove(rolledItem))
					{
						result.add(rolledItem);
					}
					
					if (rolledItems.isEmpty())
					{
						break;
					}
				}
				return result;
			}
		}
		return Collections.emptyList();
	}
	
	private List<RewardItem> rollItems(Player player, ChampionTemplate championTemplate, double mod, double dpmod, double rate, double premiumBonus, boolean useModifier, int perGroup, boolean isRaid)
	{
		if (notRate())
		{
			mod = Math.min(mod, 1.);
			dpmod = Math.min(dpmod, 1.);
			rate = 1.;
		}

		final double groupModifier = championTemplate != null ? championTemplate.groudChanceModifier : isRaid ? Config.RAID_GROUP_CHANCE_MOD : 1.0;
		if (mod > 0 && rate > 0)
		{
			perGroup = isNotUseMode() ? 1 : getMax() > 1 ? Rnd.get(getMin(), getMax()) : perGroup;
			final double pRate = rate * premiumBonus;
			final double groupChanceModifier = pRate >= 2 ? ((Config.GROUP_CHANCE_MODIFIER * pRate) + 1) : pRate;
			final double dropChanceModifier = rate >= 2 ? ((Config.GROUP_CHANCE_MODIFIER * rate) + 1) : rate;
			double groupChance = getChance() * player.getPremiumBonus().getGroupRate() * mod * groupChanceModifier * groupModifier;
			if (groupChance > RewardList.MAX_CHANCE)
			{
				mod = (groupChance - RewardList.MAX_CHANCE) / getChance() + 1;
				groupChance = RewardList.MAX_CHANCE;
			}
			else
			{
				mod = 1.;
			}
			
			double dropChance = getChance() * dpmod * dropChanceModifier;
			if (dropChance > RewardList.MAX_CHANCE)
			{
				dpmod = (dropChance - RewardList.MAX_CHANCE) / getChance() + 1;
				dropChance = RewardList.MAX_CHANCE;
			}
			else
			{
				dpmod = 1.;
			}

			if (groupChance > 0)
			{
				boolean success = false;
				if (groupChance >= RewardList.MAX_CHANCE)
				{
					success = true;
				}
				else
				{
					if (groupChance > Rnd.get(RewardList.MAX_CHANCE))
					{
						success = true;
					}
				}

				if (success)
				{
					final List<RewardItem> rolledItems = new ArrayList<>();
					for (final RewardData data : getItems())
					{
						final RewardItem item = data.rollItem(player, championTemplate, mod, dpmod, rate, dropChance, premiumBonus, useModifier, isRaid);
						if (item != null)
						{
							rolledItems.add(item);
						}
					}
					
					if (rolledItems.isEmpty())
					{
						return Collections.emptyList();
					}
					
					final List<RewardItem> result = new ArrayList<>();
					for (int i = 0; i < perGroup; i++)
					{
						final RewardItem rolledItem = Rnd.get(rolledItems);
						if (rolledItems.remove(rolledItem))
						{
							result.add(rolledItem);
						}
						
						if (rolledItems.isEmpty())
						{
							break;
						}
					}
					return result;
				}
			}
		}
		return Collections.emptyList();
	}
}