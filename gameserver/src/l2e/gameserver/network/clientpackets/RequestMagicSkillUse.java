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


import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.skills.SkillType;
import l2e.gameserver.model.skills.effects.EffectType;
import l2e.gameserver.model.skills.targets.TargetType;
import l2e.gameserver.network.SystemMessageId;

public final class RequestMagicSkillUse extends GameClientPacket
{
	private int _magicId;
	private boolean _ctrlPressed;
	private boolean _shiftPressed;

	@Override
	protected void readImpl()
	{
		_magicId = readD();
		_ctrlPressed = readD() != 0;
		_shiftPressed = readC() != 0;
	}

	@Override
	protected void runImpl()
	{
		final Player activeChar = getActiveChar();
		if (activeChar == null)
		{
			return;
		}
		
		if ((System.currentTimeMillis() - activeChar.getLastRequestMagicPacket()) < Config.REQUEST_MAGIC_PACKET_DELAY)
		{
			return;
		}
		
		activeChar.setLastRequestMagicPacket();
		
		if (activeChar.isOutOfControl())
		{
			activeChar.sendActionFailed();
			return;
		}
		
		Skill skill = activeChar.getKnownSkill(_magicId);
		if (skill == null)
		{
			skill = activeChar.getCustomSkill(_magicId);
			if (skill == null)
			{
				activeChar.sendActionFailed();
				Util.handleIllegalPlayerAction(activeChar, "SkillId " + _magicId + " not found in player: " + activeChar.getName(null) + "!");
				return;
			}
		}
		
		if (activeChar.isPlayable() && activeChar.isInAirShip())
		{
			activeChar.sendPacket(SystemMessageId.ACTION_PROHIBITED_WHILE_MOUNTED_OR_ON_AN_AIRSHIP);
			activeChar.sendActionFailed();
			return;
		}
		
		if ((activeChar.isTransformed() || activeChar.isInStance()) && !activeChar.hasTransformSkill(skill.getId()))
		{
			activeChar.sendActionFailed();
			return;
		}
		
		if (!Config.ALT_GAME_KARMA_PLAYER_CAN_TELEPORT && (activeChar.getKarma() > 0) && skill.hasEffectType(EffectType.TELEPORT))
		{
			return;
		}
		
		if (skill != null && skill.isToggle() && activeChar.isMounted())
		{
			return;
		}
		
		if (skill != null && ((skill.getSkillType() == SkillType.BUFF) && (skill.getTargetType() == TargetType.SELF)) && (!activeChar.isInAirShip() || !activeChar.isInBoat()))
		{
			activeChar.getAI().setIntention(CtrlIntention.MOVING, activeChar.getLocation(), 0);
		}
		
		if (skill != null)
		{
			activeChar.useMagic(skill, _ctrlPressed, _shiftPressed, true);
		}
	}
}