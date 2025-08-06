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
package l2e.fake.ai.addon;


import l2e.fake.FakePlayer;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.items.itemcontainer.Inventory;

public interface IConsumableSpender
{
	default void handleConsumable(FakePlayer fakePlayer, int consumableId)
	{
		if (fakePlayer.getFakeAi().getArrowTime() <= System.currentTimeMillis() && consumableId != 0)
		{
			if (fakePlayer.getInventory().getItemByItemId(consumableId) != null)
			{
				if (fakePlayer.getInventory().getItemByItemId(consumableId).getCount() <= 20)
				{
					fakePlayer.getInventory().addItem("", consumableId, 500, fakePlayer, null);
				}
			}
			else
			{
				fakePlayer.getInventory().addItem("", consumableId, 500, fakePlayer, null);
				final ItemInstance consumable = fakePlayer.getInventory().getItemByItemId(consumableId);
				if (consumable.isEquipable())
				{
					fakePlayer.getInventory().equipItem(consumable);
				}
			
				final ItemInstance weapon = fakePlayer.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
				if (weapon != null)
				{
					fakePlayer.getInventory().unEquipItem(weapon);
					fakePlayer.getInventory().equipItem(weapon);
				}
			}
			fakePlayer.getFakeAi().setArrowTime((System.currentTimeMillis() + (5 * 60000)));
		}
	}
}