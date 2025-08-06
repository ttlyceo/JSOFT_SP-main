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
import l2e.fake.FakePlayer;
import l2e.gameserver.Config;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.geodata.GeoEngine;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.StopMove;

public class MoveBackwardToLocation extends GameClientPacket
{
	private int _targetX;
	private int _targetY;
	private int _targetZ;
	private int _originX;
	private int _originY;
	private int _originZ;
	private int _moveMovement;

	@Override
	protected void readImpl()
	{
		_targetX = readD();
		_targetY = readD();
		_targetZ = readD();
		_originX = readD();
		_originY = readD();
		_originZ = readD();
		if (_buf.hasRemaining())
		{
			_moveMovement = readD();
		}
		else
		{
			final Player activeChar = getClient().getActiveChar();
			Util.handleIllegalPlayerAction(activeChar, "" + activeChar.getName(null) + " is trying to use L2Walker!");
		}
	}

	@Override
	protected void runImpl()
	{
		final Player activeChar = getClient().getActiveChar();
		if (activeChar == null)
		{
			return;
		}
		
		if (((System.currentTimeMillis() - activeChar.getLastMovePacket()) < Config.MOVE_PACKET_DELAY) && _moveMovement != 0)
		{
			activeChar.sendActionFailed();
			return;
		}
		
		activeChar.isntAfk();
		
		if (_moveMovement == 1 && activeChar.isFearing())
		{
			return;
		}
		
		if (activeChar.isAttackingNow())
		{
			activeChar.getAI().setIntention(CtrlIntention.IDLE);
		}

		if (_moveMovement != 0)
		{
			activeChar.setLastMovePacket();
		}
		
		if ((Config.PLAYER_MOVEMENT_BLOCK_TIME > 0) && !activeChar.isGM() && (activeChar.getNotMoveUntil() > System.currentTimeMillis()))
		{
			activeChar.sendPacket(SystemMessageId.CANNOT_MOVE_WHILE_SPEAKING_TO_AN_NPC);
			activeChar.sendActionFailed();
			return;
		}

		if ((_targetX == _originX) && (_targetY == _originY) && (_targetZ == _originZ))
		{
			if (_moveMovement == 0)
			{
				activeChar.stopMove(null);
			}
			else
			{
				activeChar.sendPacket(new StopMove(activeChar));
			}
			return;
		}
		_targetZ += activeChar.getColHeight();

		if (activeChar.getTeleMode() > 0)
		{
			Location loc = null;
			if (activeChar.getTeleMode() == 1)
			{
				loc = GeoEngine.getInstance().moveCheck(activeChar, activeChar.getX(), activeChar.getY(), activeChar.getZ(), _targetX, _targetY, activeChar.getZ(), activeChar.getReflection());
				activeChar.setTeleMode(0);
			}
			else if (activeChar.getTeleMode() == 2)
			{
				loc = new Location(_targetX, _targetY, _targetZ);
			}
			activeChar.sendActionFailed();
			activeChar.teleToLocation(loc.getX(), loc.getY(), loc.getZ(), false, activeChar.getReflection());
			return;
		}

		if (activeChar.isControllingFakePlayer())
		{
			final FakePlayer fakePlayer = activeChar.getPlayerUnderControl();
			activeChar.sendActionFailed();
			fakePlayer.getAI().setIntention(CtrlIntention.MOVING, new Location(_targetX, _targetY, _targetZ), 0);
			return;
		}
		
		final double dx = _targetX - activeChar.getX();
		final double dy = _targetY - activeChar.getY();
		
		if (activeChar.isOutOfControl() || (((dx * dx) + (dy * dy)) > 98010000))
		{
			activeChar.sendActionFailed();
			return;
		}
		activeChar.setKeyboardMovement(_moveMovement == 0);
		activeChar.setFallingLoc(new Location(_targetX, _targetY, _targetZ));
		activeChar.getAI().setIntention(CtrlIntention.MOVING, new Location(_targetX, _targetY, _targetZ), 0);
		if (activeChar.isInDrawZone())
		{
			activeChar.addDrawCoords(new Location(_targetX, _targetY, _targetZ));
		}
	}
}