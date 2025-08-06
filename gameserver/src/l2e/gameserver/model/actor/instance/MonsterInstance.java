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
package l2e.gameserver.model.actor.instance;

import java.util.concurrent.Future;

import l2e.gameserver.Config;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.templates.npc.MinionData;
import l2e.gameserver.model.actor.templates.npc.MinionTemplate;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.network.serverpackets.SocialAction;

public class MonsterInstance extends Attackable
{
	protected boolean _enableMinions = true;
	private Future<?> _minionSpawner;
	private boolean _canAgroWhileMoving = false;
	private boolean _isAutoAttackable = true;
	private boolean _isPassive = false;
	protected int _aggroRangeOverride = 0;
	
	public MonsterInstance(int objectId, NpcTemplate template)
	{
		super(objectId, template);
		setInstanceType(InstanceType.MonsterInstance);
	}
	
	public int getKilledInterval(MonsterInstance minion)
	{
		final int respawnTime = Config.MINIONS_RESPAWN_TIME.containsKey(minion.getId()) ? Config.MINIONS_RESPAWN_TIME.get(minion.getId()) * 1000 : -1;
		return respawnTime < 0 ? 0 : respawnTime;
	}
	
	public int getMinionUnspawnInterval()
	{
		return 5000;
	}
	
	@Override
	public boolean isAutoAttackable(Creature attacker, boolean isPoleAttack)
	{
		return _isAutoAttackable && !isEventMob() && (attacker != null && !attacker.isMonster());
	}
	
	@Override
	public boolean isAggressive()
	{
		if (_isPassive)
		{
			return false;
		}
		return (getAggroRange() > 0) && !isEventMob();
	}
	
	@Override
	public void onSpawn()
	{
		if (!isTeleporting())
		{
			if (getSpawn() != null && getSpawn().getMinionList() != null && !getMinionList().hasMinions() && isCanSupportMinion())
			{
				final int[] minionList = getSpawn().getMinionList();
				if (minionList != null)
				{
					getMinionList().addMinion(new MinionData(new MinionTemplate(minionList[0], minionList[1])), false);
				}
			}
			startMinionMaintainTask();
		}
		super.onSpawn();
	}
	
	public void enableMinions(boolean b)
	{
		_enableMinions = b;
	}
	
	protected int getMaintenanceInterval()
	{
		return 1000;
	}
	
	@Override
	protected void onDeath(Creature killer)
	{
		super.onDeath(killer);
		if (_minionSpawner != null)
		{
			_minionSpawner.cancel(false);
			_minionSpawner = null;
		}
		
		if (getLeader() != null)
		{
			if (killer != null && killer.isPlayer())
			{
				killer.getActingPlayer().getCounters().addAchivementInfo("minionKiller", getId(), -1, false, true, false);
			}
			getLeader().notifyMinionDied(this);
		}
	}
	
	@Override
	protected void onDelete()
	{
		if (_minionSpawner != null)
		{
			_minionSpawner.cancel(false);
			_minionSpawner = null;
		}
		super.onDelete();
	}
	
	public class MinionMaintainTask implements Runnable
	{
		private final boolean _isRandom;
		
		public MinionMaintainTask(boolean isRandom)
		{
			_isRandom = isRandom;
		}
		
		@Override
		public void run()
		{
			if (isDead() || !_enableMinions)
			{
				return;
			}
			
			if (_isRandom)
			{
				getMinionList().spawnRndMinions();
			}
			else
			{
				getMinionList().spawnMinions();
			}
		}
	}
	
	@Override
	public boolean isMonster()
	{
		return true;
	}
	
	@Override
	public Npc getActingNpc()
	{
		return this;
	}
	
	public final boolean canAgroWhileMoving()
	{
		return _canAgroWhileMoving;
	}
	
	public final void setCanAgroWhileMoving()
	{
		_canAgroWhileMoving = true;
	}
	
	public void setClanOverride(String newClan)
	{
	}
	
	public void setIsAggresiveOverride(int aggroR)
	{
		_aggroRangeOverride = aggroR;
	}
	
	@Override
	public void addDamageHate(Creature attacker, int damage, int aggro)
	{
		if (!_isPassive)
		{
			super.addDamageHate(attacker, damage, aggro);
		}
	}
	
	public void setPassive(boolean state)
	{
		_isPassive = state;
	}
	
	public boolean isPassive()
	{
		return _isPassive;
	}
	
	@Override
	public void setAutoAttackable(boolean state)
	{
		_isAutoAttackable = state;
	}
	
	@Override
	public boolean isWalker()
	{
		return ((getLeader() == null) ? super.isWalker() : getLeader().isWalker());
	}
	
	@Override
	public boolean isRunner()
	{
		return ((getLeader() == null) ? super.isRunner() : getLeader().isRunner());
	}
	
	@Override
	public boolean isEkimusFood()
	{
		return ((getLeader() == null) ? super.isEkimusFood() : getLeader().isEkimusFood());
	}

	@Override
	public boolean isSpecialCamera()
	{
		return ((getLeader() == null) ? super.isSpecialCamera() : getLeader().isSpecialCamera());
	}
	
	@Override
	public boolean giveRaidCurse()
	{
		return (isRaidMinion() && (getLeader() != null)) ? getLeader().giveRaidCurse() : super.giveRaidCurse();
	}
	
	public void startMinionMaintainTask()
	{
		if (getMinionList().hasMinions())
		{
			if (getMinionList().isRandomMinons())
			{
				getMinionList().onMasterDelete();
			}
			
			if (_minionSpawner != null)
			{
				_minionSpawner.cancel(false);
				_minionSpawner = null;
			}
			_minionSpawner = ThreadPoolManager.getInstance().schedule(new MinionMaintainTask(getMinionList().isRandomMinons()), 1000L);
		}
	}
	
	@Override
	public void onRandomAnimation()
	{
		if (System.currentTimeMillis() - _lastSocialBroadcast > 10000L)
		{
			broadcastPacket(new SocialAction(getObjectId(), 1));
			_lastSocialBroadcast = System.currentTimeMillis();
		}
	}
	
	@Override
	public Location getSpawnedLoc()
	{
		return getLeader() != null ? getLeader().getSpawnedLoc() : getSpawn() != null ? getSpawn().getLocation() : getLocation();
	}
	
	@Override
	public boolean isLethalImmune()
	{
		return getId() == 22215 || getId() == 22216 || getId() == 22217 || super.isLethalImmune();
	}
}