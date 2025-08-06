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
package l2e.gameserver.handler.skillhandlers.impl;

import l2e.gameserver.Config;
import l2e.gameserver.handler.skillhandlers.ISkillHandler;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.items.itemcontainer.Inventory;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.skills.SkillType;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.InventoryUpdate;
import l2e.gameserver.network.serverpackets.SystemMessage;

public class EnergyReplenish implements ISkillHandler
{
	private static final SkillType[] SKILL_IDS =
	{
	        SkillType.ENERGY_REPLENISH
	};
	
	@Override
	public void useSkill(Creature activeChar, Skill skill, GameObject[] targets)
	{
		final int energy = skill.getEnergyConsume();
		boolean emptyEnergy = false;
		
		final ItemInstance item = activeChar.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LBRACELET);
		if (item != null)
		{
			if (item.getAgathionEnergy() == 0)
			{
				emptyEnergy = true;
			}
			
			if (energy > 0)
			{
				item.setAgathionEnergy(item.getAgathionEnergy() + energy);
				activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.ENERGY_S1_REPLENISHED).addNumber(energy));
				if (!Config.FORCE_INVENTORY_UPDATE)
				{
					final InventoryUpdate playerIU = new InventoryUpdate();
					playerIU.addModifiedItem(item);
					activeChar.getActingPlayer().sendInventoryUpdate(playerIU);
				}
				else
				{
					activeChar.getActingPlayer().sendItemList(false);
				}
				
				if (emptyEnergy)
				{
					item.decreaseEnergy(false);
				}
			}
			else
			{
				activeChar.sendPacket(SystemMessageId.NOTHING_HAPPENED);
			}
		}
	}
	
	@Override
	public SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}
