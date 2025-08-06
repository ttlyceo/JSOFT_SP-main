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
package l2e.gameserver.model.service.academy;

import java.util.ArrayList;
import java.util.List;

import l2e.gameserver.Config;
import l2e.gameserver.data.parser.ItemsParser;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.items.Item;

public class AcademyRewards
{
	private static List<AcademyReward> _academyRewards = new ArrayList<>();
	
	public void load()
	{
		_academyRewards.clear();
		final String[] rewardList = Config.SERVICES_ACADEMY_REWARD.split(";");
		for (final String reward : rewardList)
		{
			final Item tmp = ItemsParser.getInstance().getTemplate(Integer.parseInt(reward));
			if (tmp != null)
			{
				_academyRewards.add(new AcademyReward(tmp));
			}
		}
	}
	
	public int getItemId(String itemName, Player player)
	{
		for (final AcademyReward item : _academyRewards)
		{
			for (final String lang : Config.MULTILANG_ALLOWED)
			{
				if (lang != null)
				{
					if (item.getItem().getName(lang).equalsIgnoreCase(itemName))
					{
						return item.getItem().getId();
					}
				}
			}
		}
		return -1;
	}
	
	public String toList(Player player)
	{
		String list = "";
		for (final AcademyReward a : _academyRewards)
		{
			list += a.getItem().getName(player.getLang()) + ";";
		}
		return list;
	}
	
	public class AcademyReward
	{
		private final Item _item;
		
		public AcademyReward(Item item)
		{
			_item = item;
		}
		
		public Item getItem()
		{
			return _item;
		}
	}

	public static AcademyRewards getInstance()
	{
		return SingletonHolder._instance;
	}

	private static class SingletonHolder
	{
		protected static final AcademyRewards _instance = new AcademyRewards();
	}
}