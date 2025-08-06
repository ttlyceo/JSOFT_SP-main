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
package l2e.gameserver.model.actor.instance.player.impl;

import l2e.gameserver.handler.itemhandlers.ItemHandler;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.SystemMessage;

public class PetFeedTask extends AbstractPlayerTask
{
	private final long _delay;
	private boolean _isSingleUse = false;

	public PetFeedTask(long delay)
	{
		_delay = delay;
	}
	
	@Override
	public boolean getTask(Player player)
	{
		if (player != null)
		{
			try
			{
				if (!player.isMounted() || (player.getMountNpcId() == 0) || (player.getPetData(player.getMountNpcId()) == null))
				{
					_isSingleUse = true;
					return false;
				}

				if (player.getCurrentFeed() > player.getFeedConsume())
				{
					player.setCurrentFeed(player.getCurrentFeed() - player.getFeedConsume());
				}
				else
				{
					player.setCurrentFeed(0);
					_isSingleUse = true;
					player.dismount();
					player.sendPacket(SystemMessageId.OUT_OF_FEED_MOUNT_CANCELED);
					return false;
				}

				final var foodIds = player.getPetData(player.getMountNpcId()).getFood();
				if (foodIds == null || foodIds.isEmpty())
				{
					return false;
				}
				
				boolean summonHaveFood = false;
				ItemInstance food = null;
				if (player.getSummon() != null)
				{
					for (final int id : foodIds)
					{
						food = player.getSummon().getInventory().getItemByItemId(id);
						if (food != null)
						{
							summonHaveFood = true;
							break;
						}
					}
				}
				
				if (food == null)
				{
					for (final int id : foodIds)
					{
						food = player.getInventory().getItemByItemId(id);
						if (food != null)
						{
							break;
						}
					}
				}

				if ((food != null) && player.isHungry())
				{
					final var handler = ItemHandler.getInstance().getHandler(food.getEtcItem());
					if (handler != null)
					{
						handler.useItem(summonHaveFood ? player.getSummon() : player, food, false);
						final var sm = SystemMessage.getSystemMessage(SystemMessageId.PET_TOOK_S1_BECAUSE_HE_WAS_HUNGRY);
						sm.addItemName(food.getId());
						player.sendPacket(sm);
					}
				}
			}
			catch (final Exception e)
			{
				warn("Mounted Pet [NpcId: " + player.getMountNpcId() + "] a feed task error has occurred", e);
			}
		}
		return true;
	}
	
	@Override
	public int getId()
	{
		return 8;
	}
	
	@Override
	public boolean isOneUse()
	{
		return true;
	}
	
	@Override
	public boolean isSingleUse()
	{
		return _isSingleUse;
	}
	
	@Override
	public long getInterval()
	{
		return System.currentTimeMillis() + _delay;
	}
}