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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import l2e.commons.apache.StringUtils;
import l2e.gameserver.Config;
import l2e.gameserver.data.parser.NpcsParser;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.items.Item;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.actor.templates.npc.champion.ChampionTemplate;
import l2e.gameserver.model.stats.Stats;

public class CalculateRewardChances
{
	private static final Map<Integer, Integer[]> droplistsCountCache = new HashMap<>();

	public static List<NpcTemplate> getNpcsContainingString(CharSequence name)
	{
		final List<NpcTemplate> templates = new ArrayList<>();
		
		for (final NpcTemplate template : NpcsParser.getInstance().getAllNpcs())
		{
			if (templateExists(template))
			{
				for (final String lang : Config.MULTILANG_ALLOWED)
				{
					if (lang != null)
					{
						if (StringUtils.containsIgnoreCase(template.getName(lang), name) && isDroppingAnything(template))
						{
							templates.add(template);
						}
					}
				}
			}
		}
		return templates;
	}

	public static int getDroplistsCountByItemId(int itemId, boolean drop)
	{
		if (droplistsCountCache.containsKey(itemId))
		{
			if (drop)
			{
				return droplistsCountCache.get(itemId)[0].intValue();
			}
			else
			{
				return droplistsCountCache.get(itemId)[1].intValue();
			}
		}
		
		int dropCount = 0;
		int spoilCount = 0;
		for (final NpcTemplate template : NpcsParser.getInstance().getAllNpcs())
		{
			if (templateExists(template))
			{
				for (final Map.Entry<RewardType, RewardList> rewardEntry : template.getRewards().entrySet())
				{
					for (final RewardGroup group : rewardEntry.getValue())
					{
						for (final RewardData data : group.getItems())
						{
							if (data.getItem().getId() == itemId)
							{
								if (rewardEntry.getKey() == RewardType.SWEEP)
								{
									spoilCount++;
								}
								else
								{
									dropCount++;
								}
							}
						}
					}
				}
			}
		}
		
		droplistsCountCache.put(itemId, new Integer[]
		{
		        dropCount, spoilCount
		});
		
		if (drop)
		{
			return dropCount;
		}
		else
		{
			return spoilCount;
		}
	}

	private static boolean templateExists(NpcTemplate template)
	{
		return template != null;
	}
	
	public static List<Item> getDroppableItems()
	{
		final List<Item> items = new ArrayList<>();
		for (final NpcTemplate template : NpcsParser.getInstance().getAllNpcs())
		{
			if (templateExists(template))
			{
				for (final Map.Entry<RewardType, RewardList> rewardEntry : template.getRewards().entrySet())
				{
					for (final RewardGroup group : rewardEntry.getValue())
					{
						for (final RewardData data : group.getItems())
						{
							if (!items.contains(data.getItem()))
							{
								items.add(data.getItem());
							}
						}
					}
				}
			}
		}
		return items;
	}
	
	public static List<NpcTemplateDrops> getNpcsByDropOrSpoil(int itemId)
	{
		final List<NpcTemplateDrops> templates = new ArrayList<>();
		for (final NpcTemplate template : NpcsParser.getInstance().getAllNpcs())
		{
			if (template == null)
			{
				continue;
			}
			
			final boolean[] dropSpoil = templateContainsItemId(template, itemId);
			
			if (dropSpoil[0])
			{
				templates.add(new NpcTemplateDrops(template, true));
			}
			if (dropSpoil[1])
			{
				templates.add(new NpcTemplateDrops(template, false));
			}
		}
		return templates;
	}
	
	public static class NpcTemplateDrops
	{
		public NpcTemplate _template;
		public boolean _dropNoSpoil;
		private NpcTemplateDrops(NpcTemplate template, boolean dropNoSpoil)
		{
			_template = template;
			_dropNoSpoil = dropNoSpoil;
		}
	}
	
	private static boolean[] templateContainsItemId(NpcTemplate template, int itemId)
	{
		final boolean[] dropSpoil =
		{
		        false, false
		};
		
		for (final Map.Entry<RewardType, RewardList> rewardEntry : template.getRewards().entrySet())
		{
			if (rewardListContainsItemId(rewardEntry.getValue(), itemId))
			{
				if (rewardEntry.getKey() == RewardType.SWEEP)
				{
					dropSpoil[1] = true;
				}
				else
				{
					dropSpoil[0] = true;
				}
			}
		}
		return dropSpoil;
	}

	private static boolean rewardListContainsItemId(RewardList list, int itemId)
	{
		for (final RewardGroup group : list)
		{
			for (final RewardData reward : group.getItems())
			{
				if (reward.getId() == itemId)
				{
					return true;
				}
			}
		}
		return false;
	}

	private static boolean isDroppingAnything(NpcTemplate template)
	{
		for (final Map.Entry<RewardType, RewardList> rewardEntry : template.getRewards().entrySet())
		{
			for (final RewardGroup group : rewardEntry.getValue())
			{
				if (!group.getItems().isEmpty())
				{
					return true;
				}
			}
		}
		return false;
	}

	public static List<RewardData> getDrops(NpcTemplate template, boolean drop, boolean spoil)
	{
		final List<RewardData> allRewards = new ArrayList<>();
		if (template == null)
		{
			return allRewards;
		}
		
		for (final Map.Entry<RewardType, RewardList> rewardEntry : template.getRewards().entrySet())
		{
			if (rewardEntry.getKey() == RewardType.SWEEP && !spoil)
			{
				continue;
			}
			if (rewardEntry.getKey() != RewardType.SWEEP && !drop)
			{
				continue;
			}
			for (final RewardGroup group : rewardEntry.getValue())
			{
				for (final RewardData reward : group.getItems())
				{
					allRewards.add(reward);
				}
			}
		}
		return allRewards;
	}
	
	public static double[] getAmountAndChanceById(Player player, NpcTemplate template, double penaltyMod, boolean dropNoSpoil, int itemId, ChampionTemplate championTemplate)
	{
		double[] shortInfo = new double[]
		{
		        0, 0, 0
		};
		
		final List<DropInfoTemplate> infoAndData = getAmountAndChance(player, template, penaltyMod, dropNoSpoil, championTemplate);
		if (infoAndData == null)
		{
			return shortInfo;
		}
		
		for (final DropInfoTemplate dp : infoAndData)
		{
			if (dp._item.getId() == itemId)
			{
				shortInfo = new double[]
				{
				        dp._minCount, dp._maxCount, dp._chance
				};
			}
		}
		infoAndData.clear();
		return shortInfo;
	}
	
	public static List<DropInfoTemplate> getAmountAndChance(Player player, NpcTemplate template, double penaltyMod, boolean dropNoSpoil, ChampionTemplate championTemplate)
	{
		final List<DropInfoTemplate> info = new ArrayList<>();
		for (final Map.Entry<RewardType, RewardList> rewardEntry : template.getRewards().entrySet())
		{
			if (rewardEntry.getKey() == RewardType.SWEEP && dropNoSpoil)
			{
				continue;
			}
			if (rewardEntry.getKey() != RewardType.SWEEP && !dropNoSpoil)
			{
				continue;
			}
			
			for (final RewardGroup group : rewardEntry.getValue())
			{
				final List<RewardData> items = new ArrayList<>();
				for (final RewardData d : group.getItems())
				{
					if (!d.getItem().isHerb())
					{
						items.add(d);
					}
				}
				
				if (items.isEmpty())
				{
					continue;
				}
				
				double grate = 1.0;
				double gpmod = penaltyMod;
				double dpmod = penaltyMod;
				double premiumBonus = 1;
				var isNoRate = false;
				final RewardType type = rewardEntry.getKey();
				if (type == RewardType.RATED_GROUPED)
				{
					if (group.isAdena())
					{
						double adenaRate = Config.RATE_DROP_ADENA;
						if (championTemplate != null)
						{
							adenaRate *= championTemplate.adenaMultiplier;
						}
						premiumBonus = player.isInParty() && Config.PREMIUM_PARTY_RATE ? player.getParty().getDropAdena() : player.getPremiumBonus().getDropAdena();
						grate = player.calcStat(Stats.ADENA_MULTIPLIER, adenaRate, player, null);
					}
					else
					{
						if (template.isEpicRaid())
						{
							final double dropRate = Config.RATE_DROP_EPICBOSS;
							premiumBonus = player.isInParty() && Config.PREMIUM_PARTY_RATE ? player.getParty().getDropEpics() : player.getPremiumBonus().getDropEpics();
							if (championTemplate != null)
							{
								premiumBonus *= championTemplate.itemDropMultiplier;
							}
							grate = player.calcStat(Stats.RAID_REWARD_MULTIPLIER, dropRate, player, null);
						}
						else if (template.isRaid())
						{
							final double dropRate = Config.RATE_DROP_RAIDBOSS;
							premiumBonus = player.isInParty() && Config.PREMIUM_PARTY_RATE ? player.getParty().getDropRaids() : player.getPremiumBonus().getDropRaids();
							if (championTemplate != null)
							{
								premiumBonus *= championTemplate.itemDropMultiplier;
							}
							grate = player.calcStat(Stats.RAID_REWARD_MULTIPLIER, dropRate, player, null);
						}
						else if (template.isSiegeGuard())
						{
							final double dropRate = Config.RATE_DROP_SIEGE_GUARD;
							premiumBonus = player.isInParty() && Config.PREMIUM_PARTY_RATE ? player.getParty().getDropSiege() : player.getPremiumBonus().getDropSiege();
							if (championTemplate != null)
							{
								premiumBonus *= championTemplate.itemDropMultiplier;
							}
							grate = player.calcStat(Stats.REWARD_MULTIPLIER, dropRate, player, null);
						}
						else
						{
							final double dropRate = Config.RATE_DROP_ITEMS;
							premiumBonus = player.isInParty() && Config.PREMIUM_PARTY_RATE ? player.getParty().getDropItems() : player.getPremiumBonus().getDropItems();
							if (championTemplate != null)
							{
								premiumBonus *= championTemplate.itemDropMultiplier;
							}
							grate = player.calcStat(Stats.REWARD_MULTIPLIER, dropRate, player, null);
						}
					}
				}
				else if (type == RewardType.SWEEP)
				{
					double sweepRate = Config.RATE_DROP_SPOIL;
					if (championTemplate != null)
					{
						sweepRate *= championTemplate.spoilDropMultiplier;
					}
					premiumBonus = player.isInParty() && Config.PREMIUM_PARTY_RATE ? player.getParty().getDropSpoil() : player.getPremiumBonus().getDropSpoil();
					grate = player.calcStat(Stats.SPOIL_MULTIPLIER, sweepRate, player, null);
				}
				else if (type == RewardType.NOT_RATED_GROUPED || type == RewardType.NOT_RATED_NOT_GROUPED || type == RewardType.NOT_RATED_GROUPED_EVENT)
				{
					gpmod = Math.min(gpmod, 1.);
					dpmod = Math.min(dpmod, 1.);
					grate = 1.;
					isNoRate = true;
				}
				
				if (group.notRate())
				{
					gpmod = Math.min(gpmod, 1.);
					dpmod = Math.min(dpmod, 1.);
					grate = 1.;
					isNoRate = true;
				}
				
				if (!player.isGM() && (gpmod == 0 || grate == 0))
				{
					continue;
				}
				
				final double groupModifier = isNoRate ? 1.0 : championTemplate != null ? championTemplate.groudChanceModifier : template.isRaid() ? Config.RAID_GROUP_CHANCE_MOD : 1.0;
				final double adenaModifier = isNoRate ? 1.0 : championTemplate != null ? championTemplate.adenaChanceModifier : 1.0;
				final double pRate = grate * premiumBonus;
				final double groupChanceModifier = pRate >= 2 ? ((Config.GROUP_CHANCE_MODIFIER * pRate) + 1) : pRate;
				final double dropChanceModifier = grate >= 2 ? ((Config.GROUP_CHANCE_MODIFIER * grate) + 1) : grate;
				double groupChance = group.isAdena() ? (group.getChance() * player.getPremiumBonus().getGroupRate() * gpmod * adenaModifier) : (group.getChance() * player.getPremiumBonus().getGroupRate() * gpmod * groupChanceModifier * groupModifier);
				if (groupChance > RewardList.MAX_CHANCE)
				{
					gpmod = (groupChance - RewardList.MAX_CHANCE) / group.getChance() + 1;
					groupChance = RewardList.MAX_CHANCE;
				}
				else
				{
					gpmod = 1.;
				}
				
				double dropChance = group.isAdena() ? (group.getChance() * dpmod) : (group.getChance() * dpmod * dropChanceModifier);
				if (dropChance > RewardList.MAX_CHANCE)
				{
					dpmod = (dropChance - RewardList.MAX_CHANCE) / group.getChance() + 1;
					dropChance = RewardList.MAX_CHANCE;
				}
				else
				{
					dpmod = 1.;
				}
				
				final double itemChanceModifier = isNoRate ? 1.0 : championTemplate != null ? championTemplate.itemChanceModifier : template.isRaid() ? Config.RAID_ITEM_CHANCE_MOD : 1.0;
				
				for (final RewardData d : items)
				{
					boolean allowModifier = true;
					if (type == RewardType.SWEEP)
					{
						allowModifier = Config.ALLOW_MODIFIER_FOR_SPOIL;
					}
					else
					{
						allowModifier = template.isEpicRaid() || template.isRaid() ? Config.ALLOW_MODIFIER_FOR_RAIDS : Config.ALLOW_MODIFIER_FOR_DROP;
					}
					
					if (isNoRate)
					{
						allowModifier = false;
					}
					
					final double ipmod = d.notRate() ? Math.min(gpmod, 1.) : gpmod;
					final double idmod = d.notRate() ? Math.min(dpmod, 1.) : dpmod;
					final double irate = d.notRate() ? 1.0 : grate;
					long minCount = (long) Math.max(1, d.getMinDrop() * (group.isAdena() ? irate * premiumBonus : 1));
					double maxCount = Math.max(1, (d.getMaxDrop() * (group.isAdena() ? (irate * premiumBonus) : irate)));
					final double chanceGeneral = (group.isAdena() ? Math.min(RewardList.MAX_CHANCE, (RewardList.MAX_CHANCE * groupChance / d.getChance()) * ipmod) : Math.min(RewardList.MAX_CHANCE, (((d.getChance() * groupChance * itemChanceModifier) / RewardList.MAX_CHANCE) * ipmod))) / RewardList.MAX_CHANCE;
					final double dropGeneral = (group.isAdena() ? Math.min(RewardList.MAX_CHANCE, (RewardList.MAX_CHANCE * dropChance / d.getChance()) * idmod) : Math.min(RewardList.MAX_CHANCE, (((d.getChance() * dropChance) / RewardList.MAX_CHANCE) * idmod))) / RewardList.MAX_CHANCE;
					if (!group.isAdena())
					{
						if (irate * premiumBonus > 1)
						{
							maxCount = getCorrectMaxAmount(d.notRate() ? 1 : d.getMaxDrop() > 1 ? (((maxCount * dropGeneral) * premiumBonus) / (irate * premiumBonus >= 2 ? 2 : 1)) : ((maxCount * dropGeneral) * premiumBonus));
						}
						minCount = Math.max(1, (long) (d.notRate() ? d.getMinDrop() : RewardItemRates.getMinCountModifier(player, championTemplate, d.getItem(), allowModifier) * minCount));
						maxCount = Math.max(1, (long) (d.notRate() ? d.getMaxDrop() : RewardItemRates.getMaxCountModifier(player, championTemplate, d.getItem(), allowModifier) * maxCount));
					}
					info.add(new DropInfoTemplate(d, minCount, (long) maxCount, chanceGeneral));
				}
			}
		}
		
		if (championTemplate != null && !championTemplate.rewards.isEmpty() && dropNoSpoil)
		{
			for (final var reward : championTemplate.rewards)
			{
				if (reward != null)
				{
					final var data = new RewardData(reward.getItemId(), reward.getMinCount(), reward.getMaxCount(), reward.getDropChance() * 10000);
					final double chanceGeneral = Math.min(RewardList.MAX_CHANCE, (((data.getChance() * (reward.getDropChance() * 10000) * 1.0) / RewardList.MAX_CHANCE) * penaltyMod)) / RewardList.MAX_CHANCE;
					info.add(new DropInfoTemplate(data, data.getMinDrop(), data.getMaxDrop(), chanceGeneral));
				}
			}
		}
		return info;
	}
	
	public static List<GroupInfoTemplate> getGroupAmountAndChance(Player player, NpcTemplate template, double penaltyMod, ChampionTemplate championTemplate)
	{
		final List<GroupInfoTemplate> groups = new ArrayList<>();
		for (final Map.Entry<RewardType, RewardList> rewardEntry : template.getRewards().entrySet())
		{
			if (rewardEntry.getKey() == RewardType.SWEEP)
			{
				continue;
			}
			
			for (final RewardGroup group : rewardEntry.getValue())
			{
				final List<DropInfoTemplate> info = new ArrayList<>();
				final List<RewardData> items = new ArrayList<>();
				for (final RewardData d : group.getItems())
				{
					if (!d.getItem().isHerb())
					{
						items.add(d);
					}
				}
				
				if (items.isEmpty())
				{
					continue;
				}
				
				double grate = 1.0;
				double gpmod = penaltyMod;
				double dpmod = penaltyMod;
				double premiumBonus = 1;
				var isNoRate = false;
				final RewardType type = rewardEntry.getKey();
				int groupAmount = player.getPremiumBonus().getMaxDropItemsFromOneGroup();
				if (type == RewardType.RATED_GROUPED)
				{
					if (group.isAdena())
					{
						double adenaRate = Config.RATE_DROP_ADENA;
						if (championTemplate != null)
						{
							adenaRate *= championTemplate.adenaMultiplier;
						}
						premiumBonus = player.isInParty() && Config.PREMIUM_PARTY_RATE ? player.getParty().getDropAdena() : player.getPremiumBonus().getDropAdena();
						grate = player.calcStat(Stats.ADENA_MULTIPLIER, adenaRate, player, null);
					}
					else
					{
						if (template.isEpicRaid())
						{
							final double dropRate = Config.RATE_DROP_EPICBOSS;
							premiumBonus = player.isInParty() && Config.PREMIUM_PARTY_RATE ? player.getParty().getDropEpics() : player.getPremiumBonus().getDropEpics();
							grate = player.calcStat(Stats.REWARD_MULTIPLIER, dropRate, player, null);
							groupAmount = player.getPremiumBonus().getMaxRaidDropItemsFromOneGroup();
							if (championTemplate != null)
							{
								groupAmount = championTemplate.maxRaidDropItemsFromOneGroup > groupAmount ? championTemplate.maxRaidDropItemsFromOneGroup : groupAmount;
							}
						}
						else if (template.isRaid())
						{
							final double dropRate = Config.RATE_DROP_RAIDBOSS;
							premiumBonus = player.isInParty() && Config.PREMIUM_PARTY_RATE ? player.getParty().getDropRaids() : player.getPremiumBonus().getDropRaids();
							grate = player.calcStat(Stats.REWARD_MULTIPLIER, dropRate, player, null);
							groupAmount = player.getPremiumBonus().getMaxRaidDropItemsFromOneGroup();
							if (championTemplate != null)
							{
								groupAmount = championTemplate.maxRaidDropItemsFromOneGroup > groupAmount ? championTemplate.maxRaidDropItemsFromOneGroup : groupAmount;
							}
						}
						else if (template.isSiegeGuard())
						{
							final double dropRate = Config.RATE_DROP_SIEGE_GUARD;
							premiumBonus = player.isInParty() && Config.PREMIUM_PARTY_RATE ? player.getParty().getDropSiege() : player.getPremiumBonus().getDropSiege();
							grate = player.calcStat(Stats.REWARD_MULTIPLIER, dropRate, player, null);
						}
						else
						{
							final double dropRate = Config.RATE_DROP_ITEMS;
							premiumBonus = player.isInParty() && Config.PREMIUM_PARTY_RATE ? player.getParty().getDropItems() : player.getPremiumBonus().getDropItems();
							if (championTemplate != null)
							{
								premiumBonus *= championTemplate.itemDropMultiplier;
							}
							grate = player.calcStat(Stats.REWARD_MULTIPLIER, dropRate, player, null);
							if (championTemplate != null)
							{
								groupAmount = championTemplate.maxDropItemsFromOneGroup > groupAmount ? championTemplate.maxDropItemsFromOneGroup : groupAmount;
							}
						}
					}
				}
				else if (type == RewardType.NOT_RATED_GROUPED || type == RewardType.NOT_RATED_NOT_GROUPED || type == RewardType.NOT_RATED_GROUPED_EVENT)
				{
					gpmod = Math.min(gpmod, 1.);
					dpmod = Math.min(dpmod, 1.);
					grate = 1.;
					groupAmount = 1;
					isNoRate = true;
				}
				
				if (group.notRate())
				{
					gpmod = Math.min(gpmod, 1.);
					dpmod = Math.min(dpmod, 1.);
					grate = 1.;
					groupAmount = 1;
					isNoRate = true;
				}
				
				if (group.getMax() > 1)
				{
					groupAmount = group.getMax();
				}
				
				if (!player.isGM() && (gpmod == 0 || grate == 0))
				{
					continue;
				}
				
				final double groupModifier = isNoRate ? 1.0 : championTemplate != null ? championTemplate.groudChanceModifier : template.isRaid() ? Config.RAID_GROUP_CHANCE_MOD : 1.0;
				final double adenaModifier = isNoRate ? 1.0 : championTemplate != null ? championTemplate.adenaChanceModifier : 1.0;
				final double pRate = grate * premiumBonus;
				final double groupChanceModifier = pRate >= 2 ? ((Config.GROUP_CHANCE_MODIFIER * pRate) + 1) : pRate;
				final double dropChanceModifier = grate >= 2 ? ((Config.GROUP_CHANCE_MODIFIER * grate) + 1) : grate;
				double groupChance = group.isAdena() ? (group.getChance() * player.getPremiumBonus().getGroupRate() * gpmod * adenaModifier) : (group.getChance() * player.getPremiumBonus().getGroupRate() * gpmod * groupChanceModifier * groupModifier);
				if (groupChance > RewardList.MAX_CHANCE)
				{
					gpmod = (groupChance - RewardList.MAX_CHANCE) / group.getChance() + 1;
					groupChance = RewardList.MAX_CHANCE;
				}
				else
				{
					gpmod = 1.;
				}
				
				double dropChance = group.isAdena() ? (group.getChance() * dpmod) : (group.getChance() * dpmod * dropChanceModifier);
				if (dropChance > RewardList.MAX_CHANCE)
				{
					dpmod = (dropChance - RewardList.MAX_CHANCE) / group.getChance() + 1;
					dropChance = RewardList.MAX_CHANCE;
				}
				else
				{
					dpmod = 1.;
				}
				
				final double itemChanceModifier = isNoRate ? 1.0 : championTemplate != null ? championTemplate.itemChanceModifier : template.isRaid() ? Config.RAID_ITEM_CHANCE_MOD : 1.0;
				
				for (final RewardData d : items)
				{
					boolean allowModifier = true;
					allowModifier = isNoRate ? false : template.isEpicRaid() || template.isRaid() ? Config.ALLOW_MODIFIER_FOR_RAIDS : Config.ALLOW_MODIFIER_FOR_DROP;
					
					final double ipmod = d.notRate() ? Math.min(gpmod, 1.) : gpmod;
					final double idmod = d.notRate() ? Math.min(dpmod, 1.) : dpmod;
					final double irate = d.notRate() ? 1.0 : grate;
					long minCount = (long) Math.max(1, d.getMinDrop() * (group.isAdena() ? irate * premiumBonus : 1));
					double maxCount = Math.max(1, (d.getMaxDrop() * (group.isAdena() ? (irate * premiumBonus) : irate)));
					final double chanceGeneral = (group.isAdena() ? Math.min(RewardList.MAX_CHANCE, d.getChance() * ipmod) : Math.min(RewardList.MAX_CHANCE, ((d.getChance() * itemChanceModifier) * ipmod))) / RewardList.MAX_CHANCE;
					final double dropGeneral = (group.isAdena() ? Math.min(RewardList.MAX_CHANCE, (RewardList.MAX_CHANCE * dropChance / d.getChance()) * idmod) : Math.min(RewardList.MAX_CHANCE, (((d.getChance() * dropChance) / RewardList.MAX_CHANCE) * idmod))) / RewardList.MAX_CHANCE;
					if (!group.isAdena())
					{
						if (irate * premiumBonus > 1)
						{
							maxCount = getCorrectMaxAmount(d.notRate() ? 1 : d.getMaxDrop() > 1 ? (((maxCount * dropGeneral) * premiumBonus) / (irate * premiumBonus >= 2 ? 2 : 1)) : ((maxCount * dropGeneral) * premiumBonus));
						}
						minCount = Math.max(1, (long) (d.notRate() ? d.getMinDrop() : RewardItemRates.getMinCountModifier(player, championTemplate, d.getItem(), allowModifier) * minCount));
						maxCount = Math.max(1, (long) (d.notRate() ? d.getMaxDrop() : RewardItemRates.getMaxCountModifier(player, championTemplate, d.getItem(), allowModifier) * maxCount));
					}
					info.add(new DropInfoTemplate(d, minCount, (long) maxCount, chanceGeneral));
				}
				groups.add(new GroupInfoTemplate(group, rewardEntry.getKey().name(), groupChance / RewardList.MAX_CHANCE, groupAmount, info));
			}
		}
		
		if (championTemplate != null)
		{
			final List<DropInfoTemplate> info = new ArrayList<>();
			for (final var reward : championTemplate.rewards)
			{
				if (reward != null)
				{
					final var data = new RewardData(reward.getItemId(), reward.getMinCount(), reward.getMaxCount(), (reward.getDropChance() * 100));
					final double chanceGeneral = Math.min(RewardList.MAX_CHANCE, ((data.getChance() * 100) * penaltyMod)) / RewardList.MAX_CHANCE;
					info.add(new DropInfoTemplate(data, data.getMinDrop(), data.getMaxDrop(), chanceGeneral));
				}
			}
			groups.add(new GroupInfoTemplate(new RewardGroup(1000, 1, 1), "CHAMPION_GROUP", 1000, info.size(), info));
		}
		return groups;
	}
	
	private static double getCorrectMaxAmount(double modifier)
	{
		double finalMod = 1;
		for (final int amounts : Config.MAX_AMOUNT_CORRECTOR.keySet())
		{
			if (modifier >= amounts && finalMod <= amounts)
			{
				finalMod = Config.MAX_AMOUNT_CORRECTOR.get(amounts);
			}
		}
		finalMod = finalMod * modifier;
		if (finalMod < 1)
		{
			finalMod = 1;
		}
		return finalMod;
	}
	
	public static class GroupInfoTemplate
	{
		public final RewardGroup _group;
		public String _type;
		public double _groupChance;
		public int _amount;
		public final List<DropInfoTemplate> _list;
		
		public GroupInfoTemplate(RewardGroup group, String type, double groupChance, int amount, List<DropInfoTemplate> list)
		{
			_group = group;
			_type = type;
			_groupChance = groupChance;
			_amount = amount;
			_list = list;
		}
	}
	
	public static class DropInfoTemplate
	{
		public final RewardData _item;
		public final long _minCount;
		public final long _maxCount;
		public final double _chance;
		
		public DropInfoTemplate(RewardData item, long minCount, long maxCount, double chance)
		{
			_item = item;
			_minCount = minCount;
			_maxCount = maxCount;
			_chance = chance;
		}
	}
}
