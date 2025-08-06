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

import l2e.gameserver.model.actor.Summon;
import l2e.gameserver.model.actor.instance.PetInstance;
import l2e.gameserver.model.actor.instance.ServitorInstance;

public class PetInfo extends GameServerPacket
{
	private final Summon _summon;
	private final int _x, _y, _z, _heading;
	private final boolean _isSummoned;
	private final int _val;
	private final double _mAtkSpd, _pAtkSpd;
	private final int _runSpd, _walkSpd;
	private final int _swimRunSpd, _swimWalkSpd;
	private final int _flyRunSpd, _flyWalkSpd;
	private final double _moveMultiplier;
	private final double _maxHp, _maxMp;
	private int _maxFed, _curFed;
	
	public PetInfo(Summon summon, int val)
	{
		_summon = summon;
		_isSummoned = _summon.isShowSummonAnimation();
		_x = _summon.getX();
		_y = _summon.getY();
		_z = _summon.getZ();
		_heading = _summon.getHeading();
		_mAtkSpd = _summon.getMAtkSpd();
		_pAtkSpd = (int) _summon.getPAtkSpd();
		_moveMultiplier = summon.getMovementSpeedMultiplier();
		_runSpd = (int) Math.round(summon.getRunSpeed() / _moveMultiplier);
		_walkSpd = (int) Math.round(summon.getWalkSpeed() / _moveMultiplier);
		_swimRunSpd = (int) Math.round(summon.getSwimRunSpeed() / _moveMultiplier);
		_swimWalkSpd = (int) Math.round(summon.getSwimWalkSpeed() / _moveMultiplier);
		_flyRunSpd = summon.isFlying() ? _runSpd : 0;
		_flyWalkSpd = summon.isFlying() ? _walkSpd : 0;
		_maxHp = _summon.getMaxHp();
		_maxMp = _summon.getMaxMp();
		_val = val;
		if (_summon instanceof PetInstance)
		{
			final PetInstance pet = (PetInstance) _summon;
			_curFed = pet.getCurrentFed();
			_maxFed = pet.getMaxFed();
		}
		else if (_summon instanceof ServitorInstance)
		{
			final ServitorInstance sum = (ServitorInstance) _summon;
			_curFed = sum.getTimeRemaining();
			_maxFed = sum.getTotalLifeTime();
		}
	}
	
	@Override
	protected final void writeImpl()
	{
		writeD(_summon.getSummonType());
		writeD(_summon.getObjectId());
		writeD(_summon.getTemplate().getIdTemplate() + 1000000);
		writeD(0x00);
		writeD(_x);
		writeD(_y);
		writeD(_z);
		writeD(_heading);
		writeD(0x00);
		writeD((int) _mAtkSpd);
		writeD((int) _pAtkSpd);
		writeD(_runSpd);
		writeD(_walkSpd);
		writeD(_swimRunSpd);
		writeD(_swimWalkSpd);
		writeD(_flyRunSpd);
		writeD(_flyWalkSpd);
		writeD(_flyRunSpd);
		writeD(_flyWalkSpd);
		writeF(_moveMultiplier);
		writeF(_summon.getAttackSpeedMultiplier());
		writeF(_summon.getTemplate().getfCollisionRadius());
		writeF(_summon.getTemplate().getfCollisionHeight());
		writeD(_summon.getWeapon());
		writeD(_summon.getArmor());
		writeD(0x00);
		writeC(_summon.getOwner() != null ? 1 : 0);
		writeC(_summon.isRunning() ? 1 : 0);
		writeC(_summon.isInCombat() ? 1 : 0);
		writeC(_summon.isAlikeDead() ? 1 : 0);
		writeC(_isSummoned ? 2 : _val);
		writeD(-1);
		writeS(_summon.getName(null));
		writeD(-1);
		writeS(_summon.getOwner().isInFightEvent() ? _summon.getOwner().getFightEvent().getVisibleTitle(_summon.getOwner(), _summon.getOwner(), _summon.getTitle(null), false) : _summon.getTitle(null));
		writeD(1);
		writeD(_summon.getPvpFlag());
		writeD(_summon.getKarma());
		writeD(_curFed);
		writeD(_maxFed);
		writeD((int) _summon.getCurrentHp());
		writeD((int) _maxHp);
		writeD((int) _summon.getCurrentMp());
		writeD((int) _maxMp);
		writeD(_summon.getStat().getSp());
		writeD(_summon.getLevel());
		writeQ(_summon.getStat().getExp());
		if (_summon.getExpForThisLevel() > _summon.getStat().getExp())
		{
			writeQ(_summon.getStat().getExp());
		}
		else
		{
			writeQ(_summon.getExpForThisLevel());
		}
		writeQ(_summon.getExpForNextLevel());
		writeD(_summon instanceof PetInstance ? _summon.getInventory().getTotalWeight() : 0x00);
		writeD(_summon.getMaxLoad());
		writeD((int) _summon.getPAtk(null));
		writeD((int) _summon.getPDef(null));
		writeD((int) _summon.getMAtk(null, null));
		writeD((int) _summon.getMDef(null, null));
		writeD(_summon.getAccuracy());
		writeD(_summon.getEvasionRate(null));
		writeD((int) _summon.getCriticalHit(null, null));
		writeD((int) _summon.getMoveSpeed());
		writeD((int) _summon.getPAtkSpd());
		writeD((int) _summon.getMAtkSpd());
		writeD(_summon.getAbnormalEffectMask());
		writeH(_summon.isMountable() ? 0x01 : 0x00);
		writeC(_summon.isInWater(_summon) ? 0x01 : _summon.isFlying() ? 0x02 : 0x00);
		writeH(0x00);
		writeC(_summon.getTeam());
		writeD(_summon.getSoulShotsPerHit());
		writeD(_summon.getSpiritShotsPerHit());
		int form = 0;
		final int npcId = _summon.getId();
		if ((npcId == 16041) || (npcId == 16042))
		{
			if (_summon.getLevel() > 69)
			{
				form = 3;
			}
			else if (_summon.getLevel() > 64)
			{
				form = 2;
			}
			else if (_summon.getLevel() > 59)
			{
				form = 1;
			}
		}
		else if ((npcId == 16025) || (npcId == 16037))
		{
			if (_summon.getLevel() > 69)
			{
				form = 3;
			}
			else if (_summon.getLevel() > 64)
			{
				form = 2;
			}
			else if (_summon.getLevel() > 59)
			{
				form = 1;
			}
		}
		writeD(form);
		writeD(_summon.getAbnormalEffectMask2());
	}
}