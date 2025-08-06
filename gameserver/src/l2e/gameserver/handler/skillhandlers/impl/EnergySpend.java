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
import l2e.gameserver.model.skills.effects.Effect;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.InventoryUpdate;

public class EnergySpend implements ISkillHandler
{
	private static final SkillType[] SKILL_IDS =
	{
	        SkillType.ENERGY_SPEND
	};
	
	@Override
	public void useSkill(Creature activeChar, Skill skill, GameObject[] targets)
	{
		final ItemInstance item = activeChar.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LBRACELET);
		if (item != null)
		{
			final int energy = skill.getEnergyConsume();
			if (energy > 0)
			{
				item.setAgathionEnergy(item.getAgathionEnergy() - energy);
				if (skill.hasEffects())
				{
					for (final Creature target : (Creature[]) targets)
					{
						skill.getEffects(activeChar, target, true);
					}
				}
				
				if (skill.hasSelfEffects())
				{
					final Effect effect = activeChar.getFirstEffect(skill.getId());
					if ((effect != null) && effect.isSelfEffect())
					{
						effect.exit();
					}
					skill.getEffectsSelf(activeChar);
				}
				
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
			}
		}
		else
		{
			activeChar.sendPacket(SystemMessageId.NOTHING_HAPPENED);
		}
	}
	
	@Override
	public SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}
