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

import org.w3c.dom.NamedNodeMap;

import l2e.commons.apache.ArrayUtils;
import l2e.commons.util.Rnd;
import l2e.gameserver.Config;
import l2e.gameserver.data.parser.ItemsParser;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.items.Item;
import l2e.gameserver.model.actor.templates.npc.champion.ChampionTemplate;

public class RewardData implements Cloneable
{
	private final Item _item;
	private boolean _notRate = false;

	private long _mindrop;
	private long _maxdrop;
	private double _chance;
	
	public RewardData(int itemId)
	{
		_item = ItemsParser.getInstance().getTemplate(itemId);
		if (_item != null && (_item.isArrow() || (Config.NO_RATE_EQUIPMENT && (_item.isEquipment() || _item.isCloak())) || (Config.NO_RATE_KEY_MATERIAL && _item.isKeyMatherial()) || (Config.NO_RATE_RECIPES && _item.isRecipe()) || ArrayUtils.contains(Config.NO_RATE_ITEMS, itemId)))
		{
			_notRate = true;
		}
	}
	
	public RewardData(int itemId, long min, long max, double chance)
	{
		this(itemId);
		_mindrop = min;
		_maxdrop = max;
		_chance = chance;
	}
	
	public boolean notRate()
	{
		return _notRate;
	}

	public void setNotRate(boolean notRate)
	{
		_notRate = notRate;
	}
	
	public int getId()
	{
		return _item.getId();
	}
	
	public Item getItem()
	{
		return _item;
	}
	
	public long getMinDrop()
	{
		return _mindrop;
	}
	
	public long getMaxDrop()
	{
		return _maxdrop;
	}
	
	public double getChance()
	{
		return _chance;
	}
	
	public void setMinDrop(long mindrop)
	{
		_mindrop = mindrop;
	}
	
	public void setMaxDrop(long maxdrop)
	{
		_maxdrop = maxdrop;
	}
	
	public void setChance(double chance)
	{
		_chance = chance;
	}
	
	@Override
	public String toString()
	{
		return "ItemID: " + getId() + " Min: " + getMinDrop() + " Max: " + getMaxDrop() + " Chance: " + getChance() / 10000.0 + "%";
	}
	
	@Override
	public RewardData clone()
	{
		return new RewardData(getId(), getMinDrop(), getMaxDrop(), getChance());
	}

	@Override
	public boolean equals(Object o)
	{
		if (o instanceof RewardData)
		{
			final RewardData drop = (RewardData) o;
			return drop.getId() == getId();
		}
		return false;
	}
	
	@Override
	public int hashCode()
	{
		return 18 * getId() + 184140;
	}
	
	protected RewardItem rollAdena(Player player, double mod, double rate, double premiumBonus)
	{
		if (notRate())
		{
			mod = Math.min(mod, 1.);
			rate = 1.;
		}
		
		if (mod > 0 && rate > 0)
		{
			final double chance = getChance() * mod;
			if (chance > Rnd.get(RewardList.MAX_CHANCE))
			{
				final RewardItem t = new RewardItem(_item.getId());
				if (getMinDrop() >= getMaxDrop())
				{
					t._count = (long) (rate * getMinDrop() * premiumBonus);
				}
				else
				{
					t._count = (long) (rate * Rnd.get((long) (getMinDrop() * premiumBonus), (long) (getMaxDrop() * premiumBonus)));
				}
				
				t._count = player.getListeners().onItemDropListener(t._itemId, t._count);
				if (t._count <= 0)
				{
					return null;
				}
				return t;
			}
		}
		return null;
	}
	
	protected RewardItem rollItem(Player player, ChampionTemplate championTemplate, double mod, double dpmod, double rate, double dropChance, double premiumBonus, boolean useModifier, boolean isRaid)
	{
		if (notRate())
		{
			mod = Math.min(mod, 1.);
			dpmod = Math.min(dpmod, 1.);
			rate = 1.;
		}
		
		final double itemChanceModifier = championTemplate != null ? championTemplate.itemChanceModifier : isRaid ? Config.RAID_ITEM_CHANCE_MOD : 1.0;
		if (mod > 0 && rate > 0)
		{
			final double chance = Math.min(RewardList.MAX_CHANCE, (getChance() * mod * itemChanceModifier));
			final double dropGeneral = Math.min(RewardList.MAX_CHANCE, (((getChance() * dropChance) / RewardList.MAX_CHANCE) * dpmod)) / RewardList.MAX_CHANCE;
			if (chance > 0)
			{
				boolean success = false;
				if (chance >= RewardList.MAX_CHANCE)
				{
					success = true;
				}
				else
				{
					if (chance > Rnd.get(RewardList.MAX_CHANCE))
					{
						success = true;
					}
				}
				
				if (success)
				{
					final RewardItem t = new RewardItem(_item.getId());
					
					double maxAmount = getMaxDrop() * rate;
					if ((rate * premiumBonus) > 1)
					{
						maxAmount = getCorrectMaxAmount(notRate() ? 1 : getMaxDrop() > 1 ? (((maxAmount * dropGeneral) * premiumBonus) / (rate * premiumBonus >= 2 ? 2 : 1)) : ((maxAmount * dropGeneral) * premiumBonus));
					}
					
					final long minCount = Math.max(1, (long) (notRate() ? getMinDrop() : RewardItemRates.getMinCountModifier(player, championTemplate, _item, useModifier) * getMinDrop()));
					final long maxCount = Math.max(1, (long) (notRate() ? getMaxDrop() : RewardItemRates.getMaxCountModifier(player, championTemplate, _item, useModifier) * maxAmount));
					if (minCount >= maxCount)
					{
						t._count = minCount;
					}
					else
					{
						t._count = Rnd.get(minCount, maxCount);
					}
					
					t._count = player.getListeners().onItemDropListener(t._itemId, t._count);
					if (t._count <= 0)
					{
						return null;
					}
					return t;
				}
			}
		}
		return null;
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
	
	public static RewardData parseReward(NamedNodeMap rewardElement, RewardType type)
	{
		final int itemId = Integer.parseInt(rewardElement.getNamedItem("itemId").getNodeValue());
		if (type == RewardType.SWEEP)
		{
			if (Config.NO_DROP_ITEMS_FOR_SWEEP.contains(itemId))
			{
				return null;
			}
		}
		else if (Config.DISABLE_ITEM_DROP_LIST.contains(itemId))
		{
			return null;
		}
		
		if ((Config.ALLOW_ONLY_THESE_DROP_ITEMS_ID.size() >= 1) && (Config.ALLOW_ONLY_THESE_DROP_ITEMS_ID.get(0) != 0) && (!Config.ALLOW_ONLY_THESE_DROP_ITEMS_ID.contains(itemId)))
		{
			return null;
		}
		
		final int min = Integer.parseInt(rewardElement.getNamedItem("min").getNodeValue());
		final int max = Integer.parseInt(rewardElement.getNamedItem("max").getNodeValue());
		
		final int chance = (int) (Double.parseDouble(rewardElement.getNamedItem("chance").getNodeValue()) * 10000);
		
		double chance_dop = chance * Config.RATE_CHANCE_DROP_ITEMS;
		double chance_h = chance * Config.RATE_CHANCE_DROP_HERBS;
		double chance_sp = chance * Config.RATE_CHANCE_SPOIL;
		double chance_weapon = chance * Config.RATE_CHANCE_DROP_WEAPON_ARMOR_ACCESSORY;
		double chance_weapon_sp = chance * Config.RATE_CHANCE_SPOIL_WEAPON_ARMOR_ACCESSORY;
		double chance_epolet = chance * Config.RATE_CHANCE_DROP_EPOLET;
		double chance_att = chance * Config.RATE_CHANCE_ATTRIBUTE;
		if (chance_dop > 1000000)
		{
			chance_dop = 1000000;
		}
		if (chance_h > 1000000)
		{
			chance_h = 1000000;
		}
		if (chance_sp > 1000000)
		{
			chance_sp = 1000000;
		}
		if (chance_weapon > 1000000)
		{
			chance_weapon = 1000000;
		}
		if (chance_weapon_sp > 1000000)
		{
			chance_weapon_sp = 1000000;
		}
		if (chance_epolet > 1000000)
		{
			chance_epolet = 1000000;
		}
		if (chance_att > 1000000)
		{
			chance_att = 1000000;
		}
		
		final RewardData data = new RewardData(itemId);
		if (data.getItem() == null)
		{
			return null;
		}
		if (type == RewardType.RATED_GROUPED)
		{
			if (data.getItem().isHerb())
			{
				data.setChance(chance_h);
			}
			else if (data.getItem().isWeapon() || data.getItem().isArmor() || data.getItem().isAccessory())
			{
				data.setChance(chance_weapon);
			}
			else if (data.getItem().isEpolets())
			{
				data.setChance(chance_epolet);
			}
			else if (data.getItem().isAttributeCrystal() || data.getItem().isAttributeStone())
			{
				data.setChance(chance_att);
			}
			else
			{
				data.setChance(chance_dop);
			}
		}
		else if (type == RewardType.SWEEP)
		{
			if (data.getItem().isWeapon() || data.getItem().isArmor() || data.getItem().isAccessory())
			{
				data.setChance(chance_weapon_sp);
			}
			else
			{
				data.setChance(chance_sp);
			}
		}
		else if (type == RewardType.NOT_RATED_GROUPED || type == RewardType.NOT_RATED_NOT_GROUPED || type == RewardType.NOT_RATED_GROUPED_EVENT)
		{
			if (data.getItem().isHerb())
			{
				data.setChance(chance_h);
			}
			else
			{
				data.setChance(chance);
			}
		}
		
		if (data.getChance() <= 0)
		{
			return null;
		}
		data.setMinDrop(min);
		data.setMaxDrop(max);
		return data;
	}
}