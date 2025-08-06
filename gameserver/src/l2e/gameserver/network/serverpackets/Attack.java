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

import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.actor.Creature;

public class Attack extends GameServerPacket
{
	public final int _attackerId;
	public final boolean _soulshot;
	private final int _grade;
	private final int _x, _y, _z, _tx, _ty, _tz;
	private Hit[] _hits;
	
	private class Hit
	{
		int _targetId, _damage, _flags;
		
		Hit(GameObject target, int damage, boolean miss, boolean crit, byte shld)
		{
			_targetId = target.getObjectId();
			_damage = damage;
			if (_soulshot)
			{
				_flags |= 0x10 | _grade;
			}
			if (crit)
			{
				_flags |= 0x20;
			}
			if (shld > 0)
			{
				_flags |= 0x40;
			}
			if (miss)
			{
				_flags |= 0x80;
			}
		}
	}

	public Attack(Creature attacker, Creature target, boolean useShots, int ssGrade)
	{
		_attackerId = attacker.getObjectId();
		_soulshot = useShots;
		_grade = ssGrade;
		_x = attacker.getX();
		_y = attacker.getY();
		_z = attacker.getZ();
		_tx = target.getX();
		_ty = target.getY();
		_tz = target.getZ();
		_hits = new Hit[0];
	}

	public void addHit(Creature target, int damage, boolean miss, boolean crit, byte shld)
	{
		final int pos = _hits.length;
		final Hit[] tmp = new Hit[pos + 1];
		
		System.arraycopy(_hits, 0, tmp, 0, _hits.length);
		tmp[pos] = new Hit(target, damage, miss, crit, shld);
		_hits = tmp;
	}

	public boolean hasHits()
	{
		return _hits.length > 0;
	}

	public boolean hasSoulshot()
	{
		return _soulshot;
	}

	@Override
	protected final void writeImpl()
	{
		final boolean shouldSeeShots = !(getClient().getActiveChar() != null && getClient().getActiveChar().getNotShowBuffsAnimation());
		
		writeD(_attackerId);
		writeD(_hits[0]._targetId);
		writeD(_hits[0]._damage);
		writeC(shouldSeeShots ? _hits[0]._flags : 0);
		writeD(_x);
		writeD(_y);
		writeD(_z);
		writeH(_hits.length - 1);
		for (int i = 1; i < _hits.length; i++)
		{
			writeD(_hits[i]._targetId);
			writeD(_hits[i]._damage);
			writeC(shouldSeeShots ? _hits[i]._flags : 0);
		}
		writeD(_tx);
		writeD(_ty);
		writeD(_tz);
	}
}