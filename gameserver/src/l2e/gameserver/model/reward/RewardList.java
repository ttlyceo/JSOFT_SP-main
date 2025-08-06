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
import java.util.List;

import org.slf4j.Logger;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import l2e.gameserver.Config;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Player;

public class RewardList extends ArrayList<RewardGroup>
{
	private static final long serialVersionUID = 1L;
	
	public static final int MAX_CHANCE = 1000000;
	private final RewardType _type;
	private final boolean _autoLoot;
	
	public RewardList(RewardType rewardType, boolean a)
	{
		super(5);
		_type = rewardType;
		_autoLoot = a;
	}

	public List<RewardItem> roll(Player player)
	{
		return roll(player, 1.0, 1.0, null);
	}

	public List<RewardItem> roll(Player player, double penaltyMod, double rateMod)
	{
		return roll(player, penaltyMod, rateMod, null);
	}
	
	public List<RewardItem> roll(Player player, double penaltyMod, double rateMod, Attackable npc)
	{
		final List<RewardItem> temp = new ArrayList<>();
		for (final RewardGroup g : this)
		{
			temp.addAll(g.roll(_type, player, penaltyMod, rateMod, npc));
		}
		return temp;
	}
	
	public RewardType getType()
	{
		return _type;
	}
	
	public boolean isAutoLoot()
	{
		return _autoLoot;
	}
	
	public static RewardList parseRewardList(Logger _log, Node cat, NamedNodeMap rewardElement, RewardType type, boolean isRaid, String debug)
	{
		final boolean autoLoot = rewardElement.getNamedItem("auto_loot") != null && Boolean.parseBoolean(rewardElement.getNamedItem("auto_loot").getNodeValue());
		final RewardList list = new RewardList(type, autoLoot);
		for (Node reward = cat.getFirstChild(); reward != null; reward = reward.getNextSibling())
		{
			final boolean notGroupType = type == RewardType.SWEEP || type == RewardType.NOT_RATED_NOT_GROUPED;
			if ("group".equalsIgnoreCase(reward.getNodeName()))
			{
				final NamedNodeMap attrs = reward.getAttributes();
				
				final double enterChance = attrs.getNamedItem("chance") == null ? RewardList.MAX_CHANCE : Double.parseDouble(attrs.getNamedItem("chance").getNodeValue()) * 10000;
				final int min = attrs.getNamedItem("min") == null ? 1 : Integer.parseInt(attrs.getNamedItem("min").getNodeValue());
				final int max = attrs.getNamedItem("max") == null ? 1 : Integer.parseInt(attrs.getNamedItem("max").getNodeValue());
				final RewardGroup group = notGroupType ? null : new RewardGroup(enterChance * Config.RATE_CHANCE_GROUP_DROP_ITEMS, min, max);
				for (Node drop = reward.getFirstChild(); drop != null; drop = drop.getNextSibling())
				{
					if ("reward".equalsIgnoreCase(drop.getNodeName()))
					{
						final RewardData data = RewardData.parseReward(drop.getAttributes(), type);
						if (notGroupType)
						{
							_log.warn("Can't load rewardlist from group: " + debug + "; type: " + type);
						}
						else
						{
							if (data != null)
							{
								group.addData(data);
								if (data.notRate() && Config.NO_RATE_GROUPS)
								{
									group.setNotRate(true);
								}
								
								if (data.getItem().isEquipment() && !group.isNotUseMode() && !isRaid)
								{
									group.setIsNotUseMode(true);
								}
							}
						}
					}
				}
				list.add(group);
			}
			else if ("reward".equalsIgnoreCase(reward.getNodeName()))
			{
				if (!notGroupType)
				{
					_log.warn("Reward can't be without group(and not grouped): " + debug + "; type: " + type);
					continue;
				}
				
				final RewardData data = RewardData.parseReward(reward.getAttributes(), type);
				if (data != null)
				{
					final RewardGroup g = new RewardGroup(RewardList.MAX_CHANCE, 1, 1);
					g.addData(data);
					list.add(g);
				}
			}
		}
		return list;
	}
}