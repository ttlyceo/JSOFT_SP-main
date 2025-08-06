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

import l2e.gameserver.Config;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.PcCondOverride;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.skills.effects.Effect;
import l2e.gameserver.model.skills.effects.EffectType;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.DeleteObject;

public final class Action extends GameClientPacket
{
	protected int _objectId;
	protected int _originX;
	protected int _originY;
	protected int _originZ;
	protected int _actionId;

	@Override
	protected void readImpl()
	{
		_objectId = readD();
		_originX = readD();
		_originY = readD();
		_originZ = readD();
		_actionId = readC();
	}
	
	@Override
	protected void runImpl()
	{
		final Player activeChar = getActiveChar();
		if (activeChar == null)
		{
			return;
		}
		
		if (activeChar.isOutOfControl())
		{
			activeChar.sendActionFailed();
			return;
		}
		
		if (activeChar.inObserverMode())
		{
			activeChar.sendPacket(SystemMessageId.OBSERVERS_CANNOT_PARTICIPATE);
			activeChar.sendActionFailed();
			return;
		}
		
		Effect ef = null;
		if (((ef = activeChar.getFirstEffect(EffectType.ACTION_BLOCK)) != null) && !ef.checkCondition(-4))
		{
			activeChar.sendPacket(SystemMessageId.YOU_HAVE_BEEN_REPORTED_SO_ACTIONS_NOT_ALLOWED);
			activeChar.sendActionFailed();
			return;
		}
		
		final GameObject obj;
		if (activeChar.isInAirShip() && (activeChar.getAirShip().getHelmObjectId() == _objectId))
		{
			obj = activeChar.getAirShip();
		}
		else
		{
			obj = activeChar.getVisibleObject(_objectId);
		}
		
		if (obj == null)
		{
			if (activeChar.getObjectId() != _objectId)
			{
				sendPacket(new DeleteObject(_objectId));
			}
			activeChar.sendActionFailed();
			return;
		}
		
		if (activeChar.isLockedTarget())
		{
			if (activeChar.getLockedTarget() != null && activeChar.getLockedTarget() != obj)
			{
				activeChar.sendActionFailed();
				return;
			}
		}
		
		if (!obj.isTargetable() && !activeChar.canOverrideCond(PcCondOverride.TARGET_ALL))
		{
			activeChar.sendActionFailed();
			return;
		}
		
		if ((obj.getReflectionId() != activeChar.getReflectionId()) && (activeChar.getReflectionId() != -1))
		{
			activeChar.sendActionFailed();
			return;
		}
		
		if (!obj.isVisibleFor(activeChar) && !activeChar.isGM())
		{
			activeChar.sendActionFailed();
			return;
		}
		
		if (activeChar.getActiveRequester() != null)
		{
			activeChar.sendActionFailed();
			return;
		}
		
		switch (_actionId)
		{
			case 0 :
				obj.onAction(activeChar, false);
				break;
			case 1 :
				if ((!activeChar.isGM() && !(Config.ALT_GAME_VIEWNPC) && !(Config.ALT_GAME_VIEWPLAYER)) || activeChar.getBlockShiftClick())
				{
					obj.onAction(activeChar, true, true);
				}
				else
				{
					obj.onActionShift(activeChar);
				}
				break;
			default :
				activeChar.sendActionFailed();
				break;
		}
	}
	
	@Override
	protected boolean triggersOnActionRequest()
	{
		return false;
	}
}