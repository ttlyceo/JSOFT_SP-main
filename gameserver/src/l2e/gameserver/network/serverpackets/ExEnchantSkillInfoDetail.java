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

import l2e.gameserver.Config;
import l2e.gameserver.data.parser.EnchantSkillGroupsParser;
import l2e.gameserver.instancemanager.mods.EnchantSkillManager;
import l2e.gameserver.model.EnchantSkillGroup.EnchantSkillsHolder;
import l2e.gameserver.model.EnchantSkillLearn;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.items.itemcontainer.PcInventory;

public class ExEnchantSkillInfoDetail extends GameServerPacket
{
	private static final int TYPE_SAFE_ENCHANT = 1;
	private static final int TYPE_UNTRAIN_ENCHANT = 2;
	private int multi = 1;
	private final int _type;
	private final int _skillid;
	private final int _skilllvl;
	private final int _chance;
	private int _sp;
	private final int _adenacount;
	private final int[] requestItems;
	
	public ExEnchantSkillInfoDetail(int type, int skillid, int skilllvl, Player ply)
	{
		
		final EnchantSkillLearn enchantLearn = EnchantSkillGroupsParser.getInstance().getSkillEnchantmentBySkillId(skillid);
		EnchantSkillsHolder esd = null;

		if (enchantLearn != null)
		{
			if (skilllvl > 100)
			{
				esd = enchantLearn.getEnchantSkillsHolder(skilllvl);
			}
			else
			{
				esd = enchantLearn.getFirstRouteGroup().getEnchantGroupDetails().get(0);
			}
		}

		if (esd == null)
		{
			throw new IllegalArgumentException("Skill " + skillid + " dont have enchant data for level " + skilllvl);
		}

		if (type == 0)
		{
			multi = EnchantSkillGroupsParser.NORMAL_ENCHANT_COST_MULTIPLIER;
		}
		else if (type == 1)
		{
			multi = EnchantSkillGroupsParser.SAFE_ENCHANT_COST_MULTIPLIER;
		}
		_chance = esd.getRate(ply);
		_sp = esd.getSpCost();
		if (type == TYPE_UNTRAIN_ENCHANT)
		{
			_sp = (int) (0.8 * _sp);
		}
		_adenacount = esd.getAdenaCost() * multi;
		_type = type;
		_skillid = skillid;
		_skilllvl = skilllvl;

		requestItems = EnchantSkillManager.getInstance().getEnchantItems(_skillid, _type, _skilllvl);
		if ((type != TYPE_SAFE_ENCHANT) && !Config.ES_SP_BOOK_NEEDED)
		{
			requestItems[1] = 0;
		}
	}

	@Override
	protected void writeImpl()
	{
		writeD(_type);
		writeD(_skillid);
		writeD(_skilllvl);
		writeD(_sp * multi);
		writeD(_chance);
		writeD(0x02);
		writeD(PcInventory.ADENA_ID);
		writeD(_adenacount);
		writeD(requestItems[0]);
		writeD(requestItems[1]);
	}
}