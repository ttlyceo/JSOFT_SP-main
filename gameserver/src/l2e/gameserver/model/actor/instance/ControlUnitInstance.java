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
package l2e.gameserver.model.actor.instance;

import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.items.instance.ItemInstance;

/**
 * Created by LordWinter
 */
public class ControlUnitInstance extends NpcInstance
{
	private static final int COND_CAN_OPEN = 0;
	private static final int COND_NO_ITEM = 1;
	private static final int COND_POWER = 2;

	public ControlUnitInstance(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public void onBypassFeedback(Player player, String command)
	{
		final int cond = getCond(player);
		if(cond == COND_CAN_OPEN)
		{
			if (getFort().getSiege().isControlDoorsOpen())
			{
				return;
			}
			
			final ItemInstance item = player.getInventory().getItemByItemId(10014);
			if (item == null)
			{
				showChatWindow(player, "data/html/fortress/fortress_controller002.htm");
				return;
			}
			
			if (player.getInventory().destroyItemByObjectId(item.getObjectId(), 1, player, true) != null)
			{
				getFort().getSiege().spawnPowerUnits();
				getFort().getSiege().spawnMainMachine();
				getFort().getSiege().openControlDoors(getFort().getId());
			}
			else
			{
				showChatWindow(player, "data/html/fortress/fortress_controller002.htm");
			}
		}
	}

	@Override
	public void showChatWindow(Player player, int val)
	{
		final int cond = getCond(player);
		switch(cond)
		{
			case COND_CAN_OPEN:
				showChatWindow(player, "data/html/fortress/fortress_controller001.htm");
				break;
			case COND_NO_ITEM:
				showChatWindow(player, "data/html/fortress/fortress_controller002.htm");
				break;
			case COND_POWER:
				showChatWindow(player, "data/html/fortress/fortress_controller003.htm");
				break;
		}
	}

	private int getCond(Player player)
	{
		if (!getFort().getSiege().getIsInProgress())
		{
			return COND_POWER;
		}
		
		boolean allPowerDisabled = false;

		if (getFort().getSiege().getControlUnits().isEmpty())
		{
			allPowerDisabled = true;
		}

		if(allPowerDisabled)
		{
			if (player.getInventory().getItemByItemId(10014) != null && player.getInventory().getItemByItemId(10014).getCount() > 0)
			{
				return COND_CAN_OPEN;
			}
			else
			{
				return COND_NO_ITEM;
			}
		}
		return COND_POWER;
	}
}
