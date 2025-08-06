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
package l2e.gameserver.network.clientpackets;

import l2e.gameserver.data.parser.SkillTreesParser;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.model.SkillLearn;
import l2e.gameserver.model.SquadTrainer;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.base.AcquireSkillType;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.network.serverpackets.AcquireSkillInfo;

public final class RequestAcquireSkillInfo extends GameClientPacket
{
	private int _id;
	private int _level;
	private AcquireSkillType _skillType;

	@Override
	protected void readImpl()
	{
		_id = readD();
		_level = readD();
		_skillType = AcquireSkillType.getAcquireSkillType(readD());
	}

	@Override
	protected void runImpl()
	{
		if ((_id <= 0) || (_level <= 0))
		{
			_log.warn(RequestAcquireSkillInfo.class.getSimpleName() + ": Invalid Id: " + _id + " or level: " + _level + "!");
			return;
		}
		
		final Player activeChar = getClient().getActiveChar();
		if (activeChar == null)
		{
			return;
		}
		
		final Npc trainer = activeChar.getLastFolkNPC();
		if ((_skillType != AcquireSkillType.CUSTOM && (trainer == null || (trainer != null && !trainer.isNpc()) || (trainer != null && !trainer.canInteract(activeChar)))))
		{
			return;
		}
		
		final Skill skill = SkillsParser.getInstance().getInfo(_id, _level);
		if (skill == null)
		{
			_log.warn(RequestAcquireSkillInfo.class.getSimpleName() + ": Skill Id: " + _id + " level: " + _level + " is undefined. " + RequestAcquireSkillInfo.class.getName() + " failed.");
			return;
		}
		
		final int prevSkillLevel = activeChar.getSkillLevel(_id);
		if ((prevSkillLevel > 0) && !((_skillType == AcquireSkillType.TRANSFER) || (_skillType == AcquireSkillType.SUBPLEDGE)))
		{
			if (prevSkillLevel == _level)
			{
				_log.warn(RequestAcquireSkillInfo.class.getSimpleName() + ": Player " + activeChar.getName(null) + " is trequesting info for a skill that already knows, Id: " + _id + " level: " + _level + "!");
			}
			else if (prevSkillLevel != (_level - 1))
			{
				_log.warn(RequestAcquireSkillInfo.class.getSimpleName() + ": Player " + activeChar.getName(null) + " is requesting info for skill Id: " + _id + " level " + _level + " without knowing it's previous level!");
			}
		}
		
		final SkillLearn s = SkillTreesParser.getInstance().getSkillLearn(_skillType, _id, _level, activeChar);
		if (s == null)
		{
			return;
		}
		
		switch (_skillType)
		{
			case TRANSFORM :
			case FISHING :
			case SUBCLASS :
			case COLLECT :
			case TRANSFER :
			case CUSTOM :
			{
				sendPacket(new AcquireSkillInfo(_skillType, s));
				break;
			}
			case CLASS :
			{
				if (trainer.getTemplate().canTeach(activeChar.getLearningClass()))
				{
					final int customSp = s.getCalculatedLevelUpSp(activeChar.getClassId(), activeChar.getLearningClass());
					sendPacket(new AcquireSkillInfo(_skillType, s, customSp));
				}
				break;
			}
			case PLEDGE :
			{
				if (!activeChar.isClanLeader())
				{
					return;
				}
				sendPacket(new AcquireSkillInfo(_skillType, s));
				break;
			}
			case SUBPLEDGE :
			{
				if (!activeChar.isClanLeader() || !(trainer instanceof SquadTrainer))
				{
					return;
				}
				sendPacket(new AcquireSkillInfo(_skillType, s));
				break;
			}
		}
	}
}