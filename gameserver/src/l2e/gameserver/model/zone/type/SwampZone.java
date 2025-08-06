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
package l2e.gameserver.model.zone.type;

import l2e.gameserver.instancemanager.CastleManager;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.entity.Castle;
import l2e.gameserver.model.zone.ZoneId;
import l2e.gameserver.model.zone.ZoneType;

public class SwampZone extends ZoneType
{
	private double _moveBonus;
	private int _castleId;
	private Castle _castle;

	public SwampZone(int id)
	{
		super(id);
		addZoneId(ZoneId.SWAMP);
		_moveBonus = 0.5;
		_castleId = 0;
		_castle = null;
	}

	@Override
	public void setParameter(String name, String value)
	{
		if (name.equals("move_bonus"))
		{
			_moveBonus = Double.parseDouble(value);
		}
		else if (name.equals("castleId"))
		{
			_castleId = Integer.parseInt(value);
		}
		else
		{
			super.setParameter(name, value);
		}
	}

	private Castle getCastle()
	{
		if ((_castleId > 0) && (_castle == null))
		{
			_castle = CastleManager.getInstance().getCastleById(_castleId);
		}
		return _castle;
	}

	@Override
	protected void onEnter(Creature character)
	{
		if (getCastle() != null)
		{
			if (!getCastle().getSiege().getIsInProgress() || !getCastle().getSiege().isTrapsActive())
			{
				return;
			}

			final Player player = character.getActingPlayer();
			if ((player != null) && player.isInSiege() && (player.getSiegeState() == 2))
			{
				return;
			}
		}

		if (character.isPlayer())
		{
			character.getActingPlayer().broadcastUserInfo(true);
		}
	}

	@Override
	protected void onExit(Creature character)
	{
		if (!character.isInsideZone(ZoneId.SWAMP, this) && character.isPlayer())
		{
			character.getActingPlayer().broadcastUserInfo(true);
		}
	}

	public double getMoveBonus(Creature character)
	{
		if (getCastle() != null)
		{
			if (!getCastle().getSiege().getIsInProgress() || !getCastle().getSiege().isTrapsActive())
			{
				return 1.0;
			}
			
			final Player player = character.getActingPlayer();
			if ((player != null) && player.isInSiege() && (player.getSiegeState() == 2))
			{
				return 1.0;
			}
		}
		return _moveBonus;
	}
}