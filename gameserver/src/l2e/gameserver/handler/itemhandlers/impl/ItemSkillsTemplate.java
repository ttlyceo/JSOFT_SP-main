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

import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.handler.itemhandlers.IItemHandler;
import l2e.gameserver.model.actor.Playable;
import l2e.gameserver.model.entity.events.cleft.AerialCleftEvent;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.items.type.EtcItemType;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.SystemMessage;

public class ItemSkillsTemplate implements IItemHandler
{
	@Override
	public boolean useItem(Playable playable, ItemInstance item, boolean forceUse)
	{
		if (!playable.isPlayer() && (!playable.isSummon()))
		{
			return false;
		}
		
		if (item.getItemType() == EtcItemType.SCROLL)
		{
			for (final var e : playable.getFightEvents())
			{
				if (e != null && !e.canUseScroll(playable))
				{
					playable.sendActionFailed();
					return false;
				}
			}

			var e = playable.getPartyTournament();
			if (e != null && !e.canUseScroll(playable))
			{
				playable.sendActionFailed();
				return false;
			}

			if (!AerialCleftEvent.getInstance().onScrollUse(playable.getObjectId()))
			{
				playable.sendActionFailed();
				return false;
			}
		}

		if (playable.isPet() && !item.isTradeable())
		{
			playable.sendPacket(SystemMessageId.ITEM_NOT_FOR_PETS);
			return false;
		}

		if (!checkReuse(playable, null, item))
		{
			return false;
		}
		
		final var template = item.getItem();
		final var requestItems = template.getRequestItems();
		if (requestItems != null && !requestItems.isEmpty())
		{
			for (final var it : requestItems)
			{
				if (it != null)
				{
					if (!playable.getInventory().haveItemsCountNotEquip(it.getId(), it.getCountMax()))
					{
						playable.sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
						return false;
					}
				}
			}
		}

		final var skills = item.getEtcItem().getSkills();
		if (skills == null)
		{
			_log.info("Item " + item + " does not have registered any skill for handler.");
			return false;
		}
		
		if (template.getItemConsume() > 0 && playable.getInventory().getItemByItemId(item.getId()).getCount() < template.getItemConsume())
		{
			playable.sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
			return false;
		}

		for (final var skillInfo : skills)
		{
			if (skillInfo == null)
			{
				continue;
			}

			final var itemSkill = skillInfo.getSkill();
			if (itemSkill != null)
			{
				if (!itemSkill.checkCondition(playable, playable.getTarget(), false, true))
				{
					return false;
				}

				if (playable.isSkillDisabled(itemSkill))
				{
					return false;
				}
				
				if (!checkReuse(playable, itemSkill, item))
				{
					return false;
				}

				if (!item.isPotion() && !item.isElixir() && playable.isCastingNow() && !template.isHerb())
				{
					return false;
				}

				if ((itemSkill.getItemConsumeId() == 0) && (itemSkill.getItemConsume() > 0) && (item.isPotion() || item.isElixir() || template.isCapsule()))
				{
					if (!playable.destroyItem("Consume", item.getObjectId(), itemSkill.getItemConsume(), playable, false))
					{
						playable.sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
						return false;
					}
				}

				if (playable.isSummon())
				{
					final var sm = SystemMessage.getSystemMessage(SystemMessageId.PET_USES_S1);
					sm.addSkillName(itemSkill);
					playable.sendPacket(sm);
				}

				if (item.isPotion() || template.isCapsule() || item.isElixir() || (template.isHerb()))
				{
					playable.doSimultaneousCast(itemSkill);
				}
				else
				{
					playable.getAI().setIntention(CtrlIntention.IDLE);

					if (!playable.useMagic(itemSkill, forceUse, false, true))
					{
						return false;
					}

					if ((itemSkill.getItemConsumeId() == 0) && (itemSkill.getItemConsume() > 0))
					{
						if (!playable.destroyItem("Consume", item.getObjectId(), itemSkill.getItemConsume(), null, false))
						{
							playable.sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
							return false;
						}
					}
				}

				if (itemSkill.getReuseDelay() > 0)
				{
					playable.addTimeStamp(itemSkill, itemSkill.getReuseDelay());
				}
			}
		}
		
		if (requestItems != null && !requestItems.isEmpty())
		{
			requestItems.stream().filter(i -> i != null).forEach(p -> playable.destroyItemByItemId("Consume", p.getId(), p.getCountMax(), null, false));
		}
		
		if (template.getItemConsume() > 0)
		{
			playable.destroyItemByItemId("Consume", item.getId(), template.getItemConsume(), null, false);
		}
		return true;
	}

	private boolean checkReuse(Playable playable, Skill skill, ItemInstance item)
	{
		if (skill != null)
		{
			switch (skill.getSkillType())
			{
				case UNLOCK :
				case UNLOCK_SPECIAL :
				case DELUXE_KEY_UNLOCK :
				{
					if (playable.isMuted())
					{
						return false;
					}
					break;
				}
			}
		}
		
		final long remainingTime = (skill != null) ? playable.getSkillRemainingReuseTime(skill.getReuseHashCode()) : playable.getItemRemainingReuseTime(item.getObjectId());
		final boolean isAvailable = remainingTime <= 0;
		if (playable.isPlayer())
		{
			if (!isAvailable)
			{
				final int hours = (int) (remainingTime / 3600000L);
				final int minutes = (int) (remainingTime % 3600000L) / 60000;
				final int seconds = (int) ((remainingTime / 1000) % 60);
				SystemMessage sm = null;
				if (hours > 0)
				{
					sm = SystemMessage.getSystemMessage(SystemMessageId.S2_HOURS_S3_MINUTES_S4_SECONDS_REMAINING_FOR_REUSE_S1);
					if ((skill == null) || skill.isStatic())
					{
						sm.addItemName(item);
					}
					else
					{
						sm.addSkillName(skill);
					}
					sm.addNumber(hours);
					sm.addNumber(minutes);
				}
				else if (minutes > 0)
				{
					sm = SystemMessage.getSystemMessage(SystemMessageId.S2_MINUTES_S3_SECONDS_REMAINING_FOR_REUSE_S1);
					if ((skill == null) || skill.isStatic())
					{
						sm.addItemName(item);
					}
					else
					{
						sm.addSkillName(skill);
					}
					sm.addNumber(minutes);
				}
				else
				{
					sm = SystemMessage.getSystemMessage(SystemMessageId.S2_SECONDS_REMAINING_FOR_REUSE_S1);
					if ((skill == null) || skill.isStatic())
					{
						sm.addItemName(item);
					}
					else
					{
						sm.addSkillName(skill);
					}
				}
				sm.addNumber(seconds);
				playable.sendPacket(sm);
			}
		}
		return isAvailable;
	}
}