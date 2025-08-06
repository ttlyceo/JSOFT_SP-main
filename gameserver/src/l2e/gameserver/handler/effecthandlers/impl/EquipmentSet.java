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

import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.base.ClassId;
import l2e.gameserver.model.skills.effects.Effect;
import l2e.gameserver.model.skills.effects.EffectTemplate;
import l2e.gameserver.model.skills.effects.EffectType;
import l2e.gameserver.model.stats.Env;
import l2e.gameserver.network.SystemMessageId;

/**
 * Created by LordWinter
 */
public class EquipmentSet extends Effect
{
	private final int _level;

	private static final int[][] _classes =
	{
	        {
	                4, 19, 32
			}, // Knight
			{
			        11, 15, 26, 29, 39, 42
			}, // Magic
			{
			        1, 54, 56
			}, // Warrior
			{
			        7, 22, 35
			}, // Rogue
			{
			        125, 126
			}, // Kamael
			{
			        45, 47
			}, // Orc Fighter
			{
			        50
			}  // Orc Mage
	};
	
	private static final int[][] _equips =
	{
	        {
	                15194, 15201, 16968
			}, // Knight
			{
			        15195, 15202, 16969
			}, // Magic
			{
			        15196, 15203, 16970
			}, // Warrior
			{
			        15197, 15204, 16971
			}, // Rogue
			{
			        15198, 15205, 16972
			}, // Kamael
			{
			        15199, 15206, 16973
			}, // Orc Fighter
			{
			        15200, 15207, 16974
			}  // Orc Mage
	};
	
	public EquipmentSet(Env env, EffectTemplate template)
	{
		super(env, template);
		
		_level = template.getParameters().getInteger("level", 1);
	}

	@Override
	public EffectType getEffectType()
	{
		return EffectType.NONE;
	}

	@Override
	public boolean onStart()
	{
		if ((getEffected() == null) || !getEffected().isPlayer())
		{
			return false;
		}
		
		if (_level <= 0)
		{
			getEffected().sendPacket(SystemMessageId.NOTHING_INSIDE_THAT);
			return false;
		}

		final Player player = getEffected().getActingPlayer();
		if (player != null)
		{
			final ClassId plClass = ClassId.values()[player.isSubClassActive() ? player.getActiveClass() : player.getClassId().getId()];
			for (int i = 0; i < _classes.length; i++)
			{
				for (final int classId : _classes[i])
				{
					if (plClass.level() > 1)
					{
						final ClassId checkClass = ClassId.getClassId(classId);
						if (plClass.childOf(checkClass))
						{
							final int itemId = _equips[i][_level - 1];
							player.addItem("EquipmentSet", itemId, 1, null, true);
							return true;
						}
					}
					else
					{
						if (plClass.getId() == classId)
						{
							final int itemId = _equips[i][_level - 1];
							player.addItem("EquipmentSet", itemId, 1, null, true);
							return true;
						}
					}
				}
			}
		}
		return true;
	}
}