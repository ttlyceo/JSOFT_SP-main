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

import l2e.commons.util.Rnd;
import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.geodata.GeoEngine;
import l2e.gameserver.handler.skillhandlers.ISkillHandler;
import l2e.gameserver.instancemanager.ZoneManager;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.PcCondOverride;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.items.Weapon;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.items.itemcontainer.Inventory;
import l2e.gameserver.model.items.type.WeaponType;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.skills.SkillType;
import l2e.gameserver.model.zone.ZoneId;
import l2e.gameserver.model.zone.ZoneType;
import l2e.gameserver.model.zone.type.FishingZone;
import l2e.gameserver.model.zone.type.HotSpringZone;
import l2e.gameserver.model.zone.type.WaterZone;
import l2e.gameserver.network.SystemMessageId;

public class Fishing implements ISkillHandler
{
	private static final SkillType[] SKILL_IDS =
	{
	        SkillType.FISHING
	};

	@Override
	public void useSkill(Creature activeChar, Skill skill, GameObject[] targets)
	{
		if (!activeChar.isPlayer() || activeChar.getSkillLevel(Skill.SKILL_FISHING_MASTERY) == -1)
		{
			return;
		}

		final Player player = activeChar.getActingPlayer();
		
		if (!Config.ALLOWFISHING && !player.canOverrideCond(PcCondOverride.SKILL_CONDITIONS))
		{
			player.sendMessage("Fishing server is currently offline");
			return;
		}
		if (player.isFishing())
		{
			if (player.getFishCombat() != null)
			{
				player.getFishCombat().doDie(false);
			}
			else
			{
				player.endFishing(false);
			}
			
			player.sendPacket(SystemMessageId.FISHING_ATTEMPT_CANCELLED);
			return;
		}
		final Weapon weaponItem = player.getActiveWeaponItem();
		if (((weaponItem == null) || (weaponItem.getItemType() != WeaponType.FISHINGROD)))
		{
			player.sendPacket(SystemMessageId.FISHING_POLE_NOT_EQUIPPED);
			return;
		}
		final ItemInstance lure = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
		if (lure == null)
		{
			player.sendPacket(SystemMessageId.BAIT_ON_HOOK_BEFORE_FISHING);
			return;
		}
		player.setLure(lure);
		final ItemInstance lure2 = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
		
		if ((lure2 == null) || (lure2.getCount() < 1))
		{
			player.sendPacket(SystemMessageId.NOT_ENOUGH_BAIT);
			return;
		}
		
		if (!player.isGM())
		{
			if (player.isInBoat())
			{
				player.sendPacket(SystemMessageId.CANNOT_FISH_ON_BOAT);
				return;
			}
			
			if (player.isInCraftMode() || player.isInStoreMode())
			{
				player.sendPacket(SystemMessageId.CANNOT_FISH_WHILE_USING_RECIPE_BOOK);
				return;
			}
			
			if (player.isInWater())
			{
				player.sendPacket(SystemMessageId.CANNOT_FISH_UNDER_WATER);
				return;
			}
			
			if (player.isInsideZone(ZoneId.PEACE))
			{
				player.sendPacket(SystemMessageId.CANNOT_FISH_HERE);
				return;
			}
		}
		boolean isHotSpringZone = false;
		int distance = Rnd.get(90, 250);
		final double angle = Util.convertHeadingToDegree(player.getHeading());
		final double radian = Math.toRadians(angle);
		final double sin = Math.sin(radian);
		final double cos = Math.cos(radian);
		int baitX = player.getX() + (int) (cos * distance);
		int baitY = player.getY() + (int) (sin * distance);
		
		FishingZone fishZone = null;
		WaterZone water = null;
		HotSpringZone hszone = null;
		for (final ZoneType zone : ZoneManager.getInstance().isInsideZone(baitX, baitY))
		{
			if (zone instanceof FishingZone)
			{
				fishZone = (FishingZone) zone;
			}
			else if (zone instanceof WaterZone)
			{
				water = (WaterZone) zone;
			}
			else if (zone instanceof HotSpringZone)
			{
				hszone = (HotSpringZone) zone;
				isHotSpringZone = true;
			}
			
			if ((fishZone != null) && (water != null) && (hszone != null))
			{
				break;
			}
		}
		
		int baitZ = computeBaitZ(player, baitX, baitY, fishZone, water, hszone);
		if (baitZ == Integer.MIN_VALUE)
		{
			isHotSpringZone = false;
			for (distance = 250; distance >= 90; --distance)
			{
				baitX = (int) (player.getX() + (cos * distance));
				baitY = (int) (player.getY() + (sin * distance));
				
				fishZone = null;
				water = null;
				hszone = null;
				for (final ZoneType zone : ZoneManager.getInstance().isInsideZone(baitX, baitY))
				{
					if (zone instanceof FishingZone)
					{
						fishZone = (FishingZone) zone;
					}
					else if (zone instanceof WaterZone)
					{
						water = (WaterZone) zone;
					}
					else if (zone instanceof HotSpringZone)
					{
						hszone = (HotSpringZone) zone;
						isHotSpringZone = true;
					}
					
					if ((fishZone != null) && (water != null) && (hszone != null))
					{
						break;
					}
				}
				
				baitZ = computeBaitZ(player, baitX, baitY, fishZone, water, hszone);
				if (baitZ != Integer.MIN_VALUE)
				{
					break;
				}
			}
			
			if (baitZ == Integer.MIN_VALUE)
			{
				player.sendPacket(SystemMessageId.CANNOT_FISH_HERE);
				return;
			}
		}
		
		if (!player.destroyItem("Fishing", player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_LHAND), 1, null, false))
		{
			player.sendPacket(SystemMessageId.NOT_ENOUGH_BAIT);
			return;
		}
		player.startFishing(baitX, baitY, baitZ, isHotSpringZone);
	}
	
	private static int computeBaitZ(final Player player, final int baitX, final int baitY, final FishingZone fishingZone, final WaterZone waterZone, HotSpringZone hszone)
	{
		if (fishingZone == null && waterZone == null)
		{
			return Integer.MIN_VALUE;
		}
		
		if (fishingZone == null && hszone == null)
		{
			return Integer.MIN_VALUE;
		}
		
		int baitZ = 0;
		if (waterZone != null)
		{
			baitZ = waterZone.getWaterZ();
		}
		else if (fishingZone != null)
		{
			baitZ = fishingZone.getWaterZ();
		}
		else if (hszone != null)
		{
			baitZ = hszone.getWaterZ();
		}
		
		if (baitZ == 0)
		{
			return Integer.MIN_VALUE;
		}
		
		if (!GeoEngine.getInstance().canSeeTarget(player.getX(), player.getY(), player.getZ(), baitX, baitY, baitZ))
		{
			return Integer.MIN_VALUE;
		}
		
		if (GeoEngine.getInstance().hasGeo(baitX, baitY))
		{
			if (GeoEngine.getInstance().getHeight(baitX, baitY, baitZ) > baitZ)
			{
				return Integer.MIN_VALUE;
			}
			
			if (GeoEngine.getInstance().getHeight(baitX, baitY, player.getZ()) > baitZ)
			{
				return Integer.MIN_VALUE;
			}
		}
		return baitZ;
	}
	
	@Override
	public SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}