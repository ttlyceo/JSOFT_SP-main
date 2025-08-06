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

import java.util.Collection;

import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.skills.Skill;

public class GMViewSkillInfo extends GameServerPacket
{
	private final Player _activeChar;
	private final Collection<Skill> _skills;

	public GMViewSkillInfo(Player cha)
	{
		_activeChar = cha;
		_skills = _activeChar.getAllSkills();
	}

	@Override
	protected final void writeImpl()
	{
		writeS(_activeChar.getName(null));
		writeD(_skills.size());
		final boolean isDisabled = (_activeChar.getClan() != null) ? (_activeChar.getClan().getReputationScore() < 0) : false;
		for (final Skill skill : _skills)
		{
			writeD(skill.isPassive() ? 1 : 0);
			writeD(skill.getDisplayLevel());
			writeD(skill.getDisplayId());
			writeC(isDisabled && skill.isClanSkill() ? 1 : 0);
			writeC(SkillsParser.getInstance().isEnchantable(skill.getDisplayId()) ? 1 : 0);
		}
	}
}