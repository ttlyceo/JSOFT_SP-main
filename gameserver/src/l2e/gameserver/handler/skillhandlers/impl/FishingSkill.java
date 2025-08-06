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

import l2e.gameserver.handler.skillhandlers.ISkillHandler;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.ShotType;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.items.Weapon;
import l2e.gameserver.model.fishing.Fishing;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.skills.SkillType;
import l2e.gameserver.network.SystemMessageId;

public class FishingSkill implements ISkillHandler
{
	private static final SkillType[] SKILL_IDS =
	{
	        SkillType.PUMPING, SkillType.REELING
	};
	
	@Override
	public void useSkill(Creature activeChar, Skill skill, GameObject[] targets)
	{
		if (!activeChar.isPlayer())
		{
			return;
		}

		final Player player = activeChar.getActingPlayer();

		final Fishing fish = player.getFishCombat();
		if (fish == null)
		{
			if (skill.getSkillType() == SkillType.PUMPING)
			{
				player.sendPacket(SystemMessageId.CAN_USE_PUMPING_ONLY_WHILE_FISHING);
			}
			else if (skill.getSkillType() == SkillType.REELING)
			{
				player.sendPacket(SystemMessageId.CAN_USE_REELING_ONLY_WHILE_FISHING);
			}
			player.sendActionFailed();
			return;
		}
		final Weapon weaponItem = player.getActiveWeaponItem();
		final ItemInstance weaponInst = activeChar.getActiveWeaponInstance();
		if ((weaponInst == null) || (weaponItem == null))
		{
			return;
		}
		int SS = 1;
		int pen = 0;
		if (activeChar.isChargedShot(ShotType.FISH_SOULSHOTS))
		{
			SS = 2;
		}
		final double gradebonus = 1 + (weaponItem.getCrystalType() * 0.1);
		int dmg = (int) (skill.getPower() * gradebonus * SS);
		if (player.getSkillLevel(1315) <= (skill.getLevel() - 2))
		{
			player.sendPacket(SystemMessageId.REELING_PUMPING_3_LEVELS_HIGHER_THAN_FISHING_PENALTY);
			pen = 50;
			final int penatlydmg = dmg - pen;
			if (player.isGM())
			{
				player.sendMessage("Dmg w/o penalty = " + dmg);
			}
			dmg = penatlydmg;
		}
		if (SS > 1)
		{
			weaponInst.setChargedShot(ShotType.FISH_SOULSHOTS, false);
		}
		if (skill.getSkillType() == SkillType.REELING)
		{
			fish.useReeling(dmg, pen);
		}
		else
		{
			fish.usePumping(dmg, pen);
		}
	}

	@Override
	public SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}