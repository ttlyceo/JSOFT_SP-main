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
package l2e.gameserver.model.items.multisell;

import java.util.ArrayList;
import java.util.LinkedList;

import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.items.Armor;
import l2e.gameserver.model.actor.templates.items.Weapon;
import l2e.gameserver.model.items.instance.ItemInstance;

public class PreparedListContainer extends ListContainer
{
	private int _npcObjectId = 0;
	
	public PreparedListContainer(ListContainer template, boolean inventoryOnly, Player player, Npc npc)
	{
		super(template.getListId());
		
		_maintainEnchantment = template.getMaintainEnchantment();
		_allowCheckEquipItems = template.isAllowCheckEquipItems();
		_applyTaxes = false;
		double taxRate = 0;
		if (npc != null)
		{
			_npcObjectId = npc.getObjectId();
			if (template.getApplyTaxes() && npc.getIsInTown() && (npc.getCastle().getOwnerId() > 0))
			{
				_applyTaxes = true;
				taxRate = npc.getCastle().getTaxRate();
			}
		}
		
		if (inventoryOnly)
		{
			if (player == null)
			{
				return;
			}
			
			final ItemInstance[] items;
			if (_maintainEnchantment)
			{
				items = player.getInventory().getUniqueItemsByEnchantLevel(false, false, false);
			}
			else
			{
				items = player.getInventory().getUniqueItems(false, false, false);
			}
			
			_entries = new LinkedList<>();
			for (final var item : items)
			{
				if ((!item.isEquipped() && !_allowCheckEquipItems || item.isEquipped() && _allowCheckEquipItems) && ((item.getItem() instanceof Armor) || (item.getItem() instanceof Weapon)))
				{
					for (final var ent : template.getEntries())
					{
						for (final var ing : ent.getIngredients())
						{
							if (item.getId() == ing.getId())
							{
								_entries.add(new PreparedEntry(ent, item, _applyTaxes, _maintainEnchantment, taxRate));
								break;
							}
						}
					}
				}
			}
		}
		else
		{
			_entries = new ArrayList<>(template.getEntries().size());
			if (_maintainEnchantment)
			{
				final var items = player.getInventory().getUniqueItemsByEnchantLevel(false, false, false);
				if (items != null && items.length > 0)
				{
					for (final var ent : template.getEntries())
					{
						var found = false;
						for (final var ing : ent.getIngredients())
						{
							for (final var item : items)
							{
								if ((!item.isEquipped() && !_allowCheckEquipItems || item.isEquipped() && _allowCheckEquipItems) && ((item.getItem() instanceof Armor) || (item.getItem() instanceof Weapon)) && item.getId() == ing.getId())
								{
									_entries.add(new PreparedEntry(ent, item, _applyTaxes, _maintainEnchantment, taxRate));
									found = true;
									break;
								}
							}
						}
						
						if (!found)
						{
							_entries.add(new PreparedEntry(ent, null, _applyTaxes, false, taxRate));
						}
					}
					
				}
				else
				{
					for (final var ent : template.getEntries())
					{
						_entries.add(new PreparedEntry(ent, null, _applyTaxes, false, taxRate));
					}
				}
			}
			else
			{
				for (final var ent : template.getEntries())
				{
					_entries.add(new PreparedEntry(ent, null, _applyTaxes, false, taxRate));
				}
			}
		}
	}
	
	public final boolean checkNpcObjectId(int npcObjectId)
	{
		return _npcObjectId != 0 ? _npcObjectId == npcObjectId : true;
	}
}