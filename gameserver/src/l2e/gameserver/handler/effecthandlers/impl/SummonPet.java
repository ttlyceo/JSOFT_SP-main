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
package l2e.gameserver.handler.effecthandlers.impl;

import l2e.gameserver.data.parser.NpcsParser;
import l2e.gameserver.data.parser.PetsParser;
import l2e.gameserver.model.PetData;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.PetInstance;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.holders.PetItemHolder;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.skills.effects.Effect;
import l2e.gameserver.model.skills.effects.EffectTemplate;
import l2e.gameserver.model.skills.effects.EffectType;
import l2e.gameserver.model.stats.Env;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.PetItemList;

public class SummonPet extends Effect
{
	public SummonPet(Env env, EffectTemplate template)
	{
		super(env, template);
	}
	
	@Override
	public EffectType getEffectType()
	{
		return EffectType.SUMMON_PET;
	}
	
	@Override
	public boolean onStart()
	{
		if ((getEffector() == null) || (getEffected() == null) || !getEffector().isPlayer() || !getEffected().isPlayer() || getEffected().isAlikeDead())
		{
			return false;
		}

		final Player player = getEffector().getActingPlayer();
		if (player.isInOlympiadMode())
		{
			player.sendPacket(SystemMessageId.THIS_SKILL_IS_NOT_AVAILABLE_FOR_THE_OLYMPIAD_EVENT);
			return false;
		}
		
		if (player.isInFightEvent())
		{
			player.sendMessage("You can not use that item in Fight Event!");
			return false;
		}

		if ((player.hasSummon() || player.isMounted()))
		{
			player.sendPacket(SystemMessageId.YOU_ALREADY_HAVE_A_PET);
			return false;
		}

		final PetItemHolder holder = player.removeScript(PetItemHolder.class);
		if (holder == null)
		{
			_log.warn("Summoning pet without attaching PetItemHandler!");
			return false;
		}

		final ItemInstance item = holder.getItem();
		if (player.getInventory().getItemByObjectId(item.getObjectId()) != item)
		{
			_log.warn("Player: " + player + " is trying to summon pet from item that he doesn't owns.");
			return false;
		}

		final PetData petData = PetsParser.getInstance().getPetDataByItemId(item.getId());
		if ((petData == null) || (petData.getNpcId() == -1))
		{
			return false;
		}

		final NpcTemplate npcTemplate = NpcsParser.getInstance().getTemplate(petData.getNpcId());
		final PetInstance pet = PetInstance.spawnPet(npcTemplate, player, item);

		pet.setShowSummonAnimation(true);
		if (!pet.isRespawned())
		{
			pet.setCurrentHp(pet.getMaxHp());
			pet.setCurrentMp(pet.getMaxMp());
			pet.getStat().setExp(pet.getExpForThisLevel());
			pet.setCurrentFed(pet.getMaxFed());
		}
		pet.setRunning();
		if (!pet.isRespawned())
		{
			pet.store();
		}
		player.setPet(pet);
		pet.spawnMe();
		pet.startFeed();
		item.setEnchantLevel(pet.getLevel());
		pet.startFeed();
		pet.setFollowStatus(true);
		pet.getOwner().sendPacket(new PetItemList(pet.getInventory().getItems()));
		pet.broadcastStatusUpdate();
		return true;
	}
}