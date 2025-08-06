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
package l2e.gameserver.network.serverpackets;

import java.util.ArrayList;
import java.util.List;

import l2e.gameserver.data.parser.EnchantSkillGroupsParser;
import l2e.gameserver.model.EnchantSkillGroup.EnchantSkillsHolder;
import l2e.gameserver.model.EnchantSkillLearn;

public final class ExEnchantSkillInfo extends GameServerPacket
{
	private final List<Integer> _routes = new ArrayList<>();
	
	private final int _id;
	private final int _lvl;
	private boolean _maxEnchanted = false;

	public ExEnchantSkillInfo(int id, int lvl)
	{
		_id = id;
		_lvl = lvl;
		
		final EnchantSkillLearn enchantLearn = EnchantSkillGroupsParser.getInstance().getSkillEnchantmentBySkillId(_id);
		if (enchantLearn != null)
		{
			if (_lvl > 100)
			{
				_maxEnchanted = enchantLearn.isMaxEnchant(_lvl);
				
				final EnchantSkillsHolder esd = enchantLearn.getEnchantSkillsHolder(_lvl);
				
				if (esd != null)
				{
					_routes.add(_lvl);
				}
				
				final int skillLvL = (_lvl % 100);
				
				for (final int route : enchantLearn.getAllRoutes())
				{
					if (((route * 100) + skillLvL) == _lvl)
					{
						continue;
					}
					
					_routes.add((route * 100) + skillLvL);
				}
				
			}
			else
			{
				for (final int route : enchantLearn.getAllRoutes())
				{
					_routes.add((route * 100) + 1);
				}
			}
		}
	}
	
	@Override
	protected void writeImpl()
	{
		writeD(_id);
		writeD(_lvl);
		writeD(_maxEnchanted ? 0x00 : 0x01);
		writeD(_lvl > 100 ? 0x01 : 0x00);
		writeD(_routes.size());
		for (final int level : _routes)
		{
			writeD(level);
		}
	}
}