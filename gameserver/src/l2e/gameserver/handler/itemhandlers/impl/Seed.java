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

import l2e.gameserver.Config;
import l2e.gameserver.handler.itemhandlers.IItemHandler;
import l2e.gameserver.instancemanager.CastleManorManager;
import l2e.gameserver.instancemanager.MapRegionManager;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Playable;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.ChestInstance;
import l2e.gameserver.model.actor.instance.MonsterInstance;
import l2e.gameserver.model.holders.SkillHolder;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.network.SystemMessageId;

public class Seed implements IItemHandler
{
	@Override
	public boolean useItem(Playable playable, ItemInstance item, boolean forceUse)
	{
		if (!playable.isPlayer())
		{
			playable.sendPacket(SystemMessageId.ITEM_NOT_FOR_PETS);
			return false;
		}
		
		if (!Config.ALLOW_MANOR)
		{
			return false;
		}

		final GameObject tgt = playable.getTarget();
		if (!(tgt instanceof Npc))
		{
			playable.sendPacket(SystemMessageId.INCORRECT_TARGET);
			playable.sendActionFailed();
			return false;
		}
		if (!(tgt instanceof MonsterInstance) || (tgt instanceof ChestInstance) || ((Creature) tgt).isRaid())
		{
			playable.sendPacket(SystemMessageId.THE_TARGET_IS_UNAVAILABLE_FOR_SEEDING);
			playable.sendActionFailed();
			return false;
		}

		final MonsterInstance target = (MonsterInstance) tgt;
		if (target.isDead())
		{
			playable.sendPacket(SystemMessageId.INCORRECT_TARGET);
			playable.sendActionFailed();
			return false;
		}

		if (target.isSeeded())
		{
			playable.sendActionFailed();
			return false;
		}
		
		final l2e.gameserver.model.Seed seed = CastleManorManager.getInstance().getSeed(item.getId());
		if (seed == null)
		{
			return false;
		}
		
		if (seed.getCastleId() != MapRegionManager.getInstance().getAreaCastle(playable))
		{
			playable.sendPacket(SystemMessageId.THIS_SEED_MAY_NOT_BE_SOWN_HERE);
			return false;
		}
		
		final Player activeChar = playable.getActingPlayer();
		if (activeChar != null)
		{
			target.setSeeded(seed, activeChar);
			
			final SkillHolder[] skills = item.getItem().getSkills();
			if (skills != null)
			{
				for (final SkillHolder sk : skills)
				{
					activeChar.useMagic(sk.getSkill(), false, false, true);
				}
			}
		}
		return true;
	}
}