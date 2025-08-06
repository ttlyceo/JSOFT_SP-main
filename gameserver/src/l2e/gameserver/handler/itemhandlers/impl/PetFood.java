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
package l2e.gameserver.handler.itemhandlers.impl;

import java.util.List;

import l2e.gameserver.Config;
import l2e.gameserver.data.parser.PetsParser;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.handler.itemhandlers.IItemHandler;
import l2e.gameserver.model.actor.Playable;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.PetInstance;
import l2e.gameserver.model.holders.SkillHolder;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.MagicSkillUse;
import l2e.gameserver.network.serverpackets.SystemMessage;

public class PetFood implements IItemHandler
{
	@Override
	public boolean useItem(Playable playable, ItemInstance item, boolean forceUse)
	{
		if (playable.isPet() && !((PetInstance) playable).canEatFoodId(item.getId()))
		{
			playable.sendPacket(SystemMessageId.PET_CANNOT_USE_ITEM);
			return false;
		}

		final SkillHolder[] skills = item.getItem().getSkills();
		if (skills != null)
		{
			for (final SkillHolder sk : skills)
			{
				useFood(playable, sk.getId(), sk.getLvl(), item);
			}
		}
		return true;
	}
	
	public boolean useFood(Playable activeChar, int skillId, int skillLevel, ItemInstance item)
	{
		final Skill skill = SkillsParser.getInstance().getInfo(skillId, skillLevel);
		if (skill != null)
		{
			if (activeChar.isPet())
			{
				final PetInstance pet = (PetInstance) activeChar;
				if (pet.destroyItem("Consume", item.getObjectId(), 1, null, false))
				{
					pet.broadcastPacket(new MagicSkillUse(pet, pet, skillId, skillLevel, 0, 0));
					pet.setCurrentFed(pet.getCurrentFed() + (skill.getFeed() * Config.PET_FOOD_RATE));
					pet.broadcastStatusUpdate();
					if (pet.getCurrentFed() < ((pet.getPetData().getHungryLimit() / 100f) * pet.getPetLevelData().getPetMaxFeed()))
					{
						pet.sendPacket(SystemMessageId.YOUR_PET_ATE_A_LITTLE_BUT_IS_STILL_HUNGRY);
					}
					return true;
				}
			}
			else if (activeChar.isPlayer())
			{
				final Player player = activeChar.getActingPlayer();
				if (player.isMounted())
				{
					final List<Integer> foodIds = PetsParser.getInstance().getPetData(player.getMountNpcId()).getFood();
					if (foodIds.contains(Integer.valueOf(item.getId())))
					{
						if (player.destroyItem("Consume", item.getObjectId(), 1, null, false))
						{
							player.broadcastPacket(new MagicSkillUse(player, player, skillId, skillLevel, 0, 0));
							player.setCurrentFeed(player.getCurrentFeed() + skill.getFeed());
							return true;
						}
					}
				}
				final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
				sm.addItemName(item);
				player.sendPacket(sm);
			}
		}
		return false;
	}
}