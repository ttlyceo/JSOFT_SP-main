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
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.PcCondOverride;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.skills.effects.Effect;
import l2e.gameserver.model.skills.effects.EffectType;
import l2e.gameserver.network.SystemMessageId;

public final class RequestAttack extends GameClientPacket
{
	private int _objectId;
	protected int _originX;
	protected int _originY;
	protected int _originZ;
	protected int _attackId;
	
	@Override
	protected void readImpl()
	{
		_objectId = readD();
		_originX = readD();
		_originY = readD();
		_originZ = readD();
		_attackId = readC();
	}

	@Override
	protected void runImpl()
	{
		final Player activeChar = getActiveChar();
		if (activeChar == null)
		{
			return;
		}
		
		if ((System.currentTimeMillis() - activeChar.getLastAttackPacket()) < Config.ATTACK_PACKET_DELAY)
		{
			activeChar.sendActionFailed();
			return;
		}
		
		activeChar.setLastAttackPacket();
		
		if (activeChar.isOutOfControl())
		{
			activeChar.sendActionFailed();
			return;
		}

		if (activeChar.isPlayable() && activeChar.isInBoat())
		{
			activeChar.sendPacket(SystemMessageId.NOT_ALLOWED_ON_BOAT);
			activeChar.sendActionFailed();
			return;
		}

		Effect ef = null;
		if (((ef = activeChar.getFirstEffect(EffectType.ACTION_BLOCK)) != null) && !ef.checkCondition(-1))
		{
			activeChar.sendPacket(SystemMessageId.YOU_HAVE_BEEN_REPORTED_SO_ACTIONS_NOT_ALLOWED);
			activeChar.sendActionFailed();
			return;
		}

		final GameObject target;
		if (activeChar.getTargetId() == _objectId)
		{
			target = activeChar.getTarget();
		}
		else
		{
			target = GameObjectsStorage.findObject(_objectId);
		}

		if (target == null)
		{
			return;
		}
		
		if (activeChar.isLockedTarget())
		{
			if ((activeChar.getLockedTarget() != null) && (activeChar.getLockedTarget() != target) && !activeChar.getLockedTarget().isDead())
			{
				activeChar.sendActionFailed();
				return;
			}
		}

		if (!target.isTargetable() && !activeChar.canOverrideCond(PcCondOverride.TARGET_ALL))
		{
			activeChar.sendActionFailed();
			return;
		}
		else if ((target.getReflectionId() != activeChar.getReflectionId()) && (activeChar.getReflectionId() != -1))
		{
			activeChar.sendActionFailed();
			return;
		}
		else if (!target.isVisibleFor(activeChar))
		{
			activeChar.sendActionFailed();
			return;
		}

		if (activeChar.getTarget() != target)
		{
			target.onAction(activeChar, _attackId == 1);
		}
		else
		{
			if ((target.getObjectId() != activeChar.getObjectId()) && (activeChar.getPrivateStoreType() == Player.STORE_PRIVATE_NONE) && (activeChar.getActiveRequester() == null))
			{
				target.onForcedAttack(activeChar, _attackId == 1);
			}
		}
	}
}