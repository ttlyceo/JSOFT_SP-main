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
package l2e.gameserver.model.actor.templates.items;

import l2e.commons.util.StringUtil;
import l2e.gameserver.model.holders.SkillHolder;
import l2e.gameserver.model.items.type.ArmorType;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.stats.StatsSet;

public final class Armor extends Item
{
	private SkillHolder _enchant4Skill = null;
	private ArmorType _type;

	public Armor(StatsSet set)
	{
		super(set);
		_type = ArmorType.valueOf(set.getString("armor_type", "none").toUpperCase());

		final int _bodyPart = getBodyPart();
		if ((_bodyPart == Item.SLOT_NECK) || ((_bodyPart & Item.SLOT_L_EAR) != 0) || ((_bodyPart & Item.SLOT_L_FINGER) != 0) || ((_bodyPart & Item.SLOT_R_BRACELET) != 0) || ((_bodyPart & Item.SLOT_L_BRACELET) != 0))
		{
			_type1 = Item.TYPE1_WEAPON_RING_EARRING_NECKLACE;
			_type2 = Item.TYPE2_ACCESSORY;
		}
		else
		{
			if ((_type == ArmorType.NONE) && (getBodyPart() == Item.SLOT_L_HAND))
			{
				_type = ArmorType.SHIELD;
			}
			_type1 = Item.TYPE1_SHIELD_ARMOR;
			_type2 = Item.TYPE2_SHIELD_ARMOR;
		}

		final String skill = set.getString("enchant4_skill", null);
		if (skill != null)
		{
			final String[] info = skill.split("-");

			if ((info != null) && (info.length == 2))
			{
				int id = 0;
				int level = 0;
				try
				{
					id = Integer.parseInt(info[0]);
					level = Integer.parseInt(info[1]);
				}
				catch (final Exception nfe)
				{
					_log.info(StringUtil.concat("> Couldnt parse ", skill, " in armor enchant skills! item ", toString()));
				}
				if ((id > 0) && (level > 0))
				{
					_enchant4Skill = new SkillHolder(id, level);
				}
			}
		}
	}

	@Override
	public ArmorType getItemType()
	{
		return _type;
	}

	@Override
	public final int getItemMask()
	{
		return getItemType().mask();
	}

	@Override
	public Skill getEnchant4Skill()
	{
		if (_enchant4Skill == null)
		{
			return null;
		}
		return _enchant4Skill.getSkill();
	}
}