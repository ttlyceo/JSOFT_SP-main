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
import l2e.gameserver.data.parser.DressArmorParser;
import l2e.gameserver.data.parser.DressCloakParser;
import l2e.gameserver.data.parser.DressHatParser;
import l2e.gameserver.data.parser.DressShieldParser;
import l2e.gameserver.data.parser.DressWeaponParser;
import l2e.gameserver.handler.itemhandlers.IItemHandler;
import l2e.gameserver.model.actor.Playable;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.DressArmorTemplate;
import l2e.gameserver.model.actor.templates.DressCloakTemplate;
import l2e.gameserver.model.actor.templates.DressHatTemplate;
import l2e.gameserver.model.actor.templates.DressShieldTemplate;
import l2e.gameserver.model.actor.templates.DressWeaponTemplate;
import l2e.gameserver.model.actor.templates.items.EtcItem;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.MagicSkillUse;

public class VisualItems implements IItemHandler
{
	@Override
	public boolean useItem(Playable playable, ItemInstance item, boolean forceUse)
	{
		if (!Config.ALLOW_VISUAL_SYSTEM)
		{
			return false;
		}
		
		if (!playable.isPlayer())
		{
			playable.sendPacket(SystemMessageId.ITEM_NOT_FOR_PETS);
			return false;
		}
		
		final Player player = playable.getActingPlayer();
		final EtcItem etcitem = (EtcItem) item.getItem();
		if (etcitem.getSkinType() == null || etcitem.getSkinType().isEmpty() || etcitem.getSkinId() == 0)
		{
			_log.info("Not correct visual params for " + etcitem);
			return false;
		}
		
		switch (etcitem.getSkinType())
		{
			case "Weapon" :
				final DressWeaponTemplate weapon = DressWeaponParser.getInstance().getWeapon(etcitem.getSkinId());
				if (weapon != null)
				{
					if (player.getWeaponSkins().contains(weapon.getId()))
					{
						player.sendMessage("You already have this skin!");
						return false;
					}
				}
				break;
			case "Armor" :
				final DressArmorTemplate armor = DressArmorParser.getInstance().getArmor(etcitem.getSkinId());
				if (armor != null)
				{
					if (player.getArmorSkins().contains(armor.getId()))
					{
						player.sendMessage("You already have this skin!");
						return false;
					}
				}
				break;
			case "Shield" :
				final DressShieldTemplate shield = DressShieldParser.getInstance().getShield(etcitem.getSkinId());
				if (shield != null)
				{
					if (player.getShieldSkins().contains(shield.getId()))
					{
						player.sendMessage("You already have this skin!");
						return false;
					}
				}
				break;
			case "Cloak" :
				final DressCloakTemplate cloak = DressCloakParser.getInstance().getCloak(etcitem.getSkinId());
				if (cloak != null)
				{
					if (player.getCloakSkins().contains(cloak.getId()))
					{
						player.sendMessage("You already have this skin!");
						return false;
					}
				}
				break;
			case "Hair" :
				final DressHatTemplate hat = DressHatParser.getInstance().getHat(etcitem.getSkinId());
				if (hat != null)
				{
					if (player.getHairSkins().contains(hat.getId()))
					{
						player.sendMessage("You already have this skin!");
						return false;
					}
				}
				break;
		}
		
		if (!player.destroyItem("Visual", item.getObjectId(), 1, player, true))
		{
			return false;
		}
		
		boolean activated = false;
		
		if (etcitem.getSkinId() > 0)
		{
			if (etcitem.getSkinId() > 0)
			{
				player.addVisual(etcitem.getSkinType(), etcitem.getSkinId());
				activated = true;
			}
			player.broadcastPacket(new MagicSkillUse(player, player, 22217, 1, 0, 0));
		}
		player.broadcastUserInfo(true);
		
		if (!activated)
		{
			player.sendPacket(SystemMessageId.NOTHING_INSIDE_THAT);
		}
		return true;
	}
}