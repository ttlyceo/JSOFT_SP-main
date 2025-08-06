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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;

import l2e.commons.geometry.Point3D;
import l2e.commons.util.Rnd;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.skills.SkillType;
import l2e.gameserver.model.skills.effects.EffectType;
import l2e.gameserver.network.serverpackets.NpcInfo;
import l2e.gameserver.network.serverpackets.SocialAction;

public final class TamedBeastInstance extends FeedableBeastInstance
{
	private int _foodSkillId;
	private static final int MAX_DISTANCE_FROM_HOME = 30000;
	private static final int MAX_DISTANCE_FROM_OWNER = 2000;
	private static final int MAX_DURATION = 1200000;
	private static final int DURATION_CHECK_INTERVAL = 60000;
	private static final int DURATION_INCREASE_INTERVAL = 20000;
	private static final int BUFF_INTERVAL = 5000;
	private int _remainingTime = MAX_DURATION;
	private int _homeX, _homeY, _homeZ;
	protected Player _owner;
	private Future<?> _buffTask = null;
	private Future<?> _durationCheckTask = null;
	protected boolean _isFreyaBeast;
	private List<Skill> _beastSkills = null;
	
	public TamedBeastInstance(int objectId, NpcTemplate template)
	{
		super(objectId, template);
		setInstanceType(InstanceType.TamedBeastInstance);
		setHome(this);
	}

	public TamedBeastInstance(int objectId, NpcTemplate template, Player owner, int foodSkillId, int x, int y, int z)
	{
		super(objectId, template);
		_isFreyaBeast = false;
		setInstanceType(InstanceType.TamedBeastInstance);
		setCurrentHp(getMaxHp());
		setCurrentMp(getMaxMp());
		setOwner(owner);
		setFoodType(foodSkillId);
		setHome(x, y, z);
		this.spawnMe(x, y, z);
	}
	
	public TamedBeastInstance(int objectId, NpcTemplate template, Player owner, int food, int x, int y, int z, boolean isFreyaBeast)
	{
		super(objectId, template);
		_isFreyaBeast = isFreyaBeast;
		setInstanceType(InstanceType.TamedBeastInstance);
		setCurrentHp(getMaxHp());
		setCurrentMp(getMaxMp());
		setFoodType(food);
		setHome(x, y, z);
		spawnMe(x, y, z);
		setOwner(owner);
		if (isFreyaBeast)
		{
			getAI().setIntention(CtrlIntention.FOLLOW, _owner);
		}
		
	}
	
	public void onReceiveFood()
	{
		_remainingTime = _remainingTime + DURATION_INCREASE_INTERVAL;
		if (_remainingTime > MAX_DURATION)
		{
			_remainingTime = MAX_DURATION;
		}
	}
	
	public Point3D getHome()
	{
		return new Point3D(_homeX, _homeY, _homeZ);
	}
	
	public void setHome(int x, int y, int z)
	{
		_homeX = x;
		_homeY = y;
		_homeZ = z;
	}
	
	public void setHome(Creature c)
	{
		setHome(c.getX(), c.getY(), c.getZ());
	}
	
	public int getRemainingTime()
	{
		return _remainingTime;
	}
	
	public void setRemainingTime(int duration)
	{
		_remainingTime = duration;
	}
	
	public int getFoodType()
	{
		return _foodSkillId;
	}
	
	public void setFoodType(int foodItemId)
	{
		if (foodItemId > 0)
		{
			_foodSkillId = foodItemId;
			
			if (_durationCheckTask != null)
			{
				_durationCheckTask.cancel(true);
			}
			_durationCheckTask = ThreadPoolManager.getInstance().scheduleAtFixedRate(new CheckDuration(this), DURATION_CHECK_INTERVAL, DURATION_CHECK_INTERVAL);
		}
	}
	
	@Override
	protected void onDeath(Creature killer)
	{
		getAI().stopFollow();
		if (_buffTask != null)
		{
			_buffTask.cancel(true);
		}
		if (_durationCheckTask != null)
		{
			_durationCheckTask.cancel(true);
		}
		
		if ((_owner != null) && (_owner.getTrainedBeasts() != null))
		{
			_owner.getTrainedBeasts().remove(this);
		}
		_buffTask = null;
		_durationCheckTask = null;
		_owner = null;
		_foodSkillId = 0;
		_remainingTime = 0;
		super.onDeath(killer);
	}
	
	@Override
	public boolean isAutoAttackable(Creature attacker, boolean isPoleAttack)
	{
		return !_isFreyaBeast;
	}
	
	public boolean isFreyaBeast()
	{
		return _isFreyaBeast;
	}
	
	public void addBeastSkill(Skill skill)
	{
		if (_beastSkills == null)
		{
			_beastSkills = new CopyOnWriteArrayList<>();
		}
		_beastSkills.add(skill);
	}
	
	public void castBeastSkills()
	{
		if ((_owner == null) || (_beastSkills == null))
		{
			return;
		}
		int delay = 100;
		for (final Skill skill : _beastSkills)
		{
			ThreadPoolManager.getInstance().schedule(new buffCast(skill), delay);
			delay += (100 + skill.getHitTime());
		}
		ThreadPoolManager.getInstance().schedule(new buffCast(null), delay);
	}
	
	private class buffCast implements Runnable
	{
		private final Skill _skill;
		
		public buffCast(Skill skill)
		{
			_skill = skill;
		}
		
		@Override
		public void run()
		{
			if (_skill == null)
			{
				getAI().setIntention(CtrlIntention.FOLLOW, _owner);
			}
			else
			{
				sitCastAndFollow(_skill, _owner);
			}
		}
	}
	
	public Player getOwner()
	{
		return _owner;
	}
	
	public void setOwner(Player owner)
	{
		if (owner != null)
		{
			_owner = owner;
			setGlobalTitle(owner.getName(null));
			setShowSummonAnimation(true);
			broadcastPacket(new NpcInfo.Info(this, owner));
			
			owner.addTrainedBeast(this);
			
			getAI().startFollow(_owner, 100);
			
			if (!_isFreyaBeast)
			{
				int totalBuffsAvailable = 0;
				for (final Skill skill : getTemplate().getSkills().values())
				{
					if (skill.getSkillType() == SkillType.BUFF)
					{
						totalBuffsAvailable++;
					}
				}
				
				if (_buffTask != null)
				{
					_buffTask.cancel(true);
				}
				_buffTask = ThreadPoolManager.getInstance().scheduleAtFixedRate(new CheckOwnerBuffs(this, totalBuffsAvailable), BUFF_INTERVAL, BUFF_INTERVAL);
			}
		}
		else
		{
			deleteMe();
		}
	}
	
	public boolean isTooFarFromHome()
	{
		return !isInsideRadius(_homeX, _homeY, _homeZ, MAX_DISTANCE_FROM_HOME, true, true);
	}
	
	@Override
	protected void onDelete()
	{
		if (_buffTask != null)
		{
			_buffTask.cancel(true);
		}
		
		if (_durationCheckTask != null)
		{
			_durationCheckTask.cancel(true);
		}
		stopHpMpRegeneration();
		
		if ((_owner != null) && (_owner.getTrainedBeasts() != null))
		{
			_owner.getTrainedBeasts().remove(this);
		}
		setTarget(null);
		_buffTask = null;
		_durationCheckTask = null;
		_owner = null;
		_foodSkillId = 0;
		_remainingTime = 0;
		
		super.onDelete();
	}
	
	public void onOwnerGotAttacked(Creature attacker)
	{
		if ((_owner == null) || !_owner.isOnline())
		{
			deleteMe();
			return;
		}
		
		if (!_owner.isInsideRadius(this, MAX_DISTANCE_FROM_OWNER, true, true))
		{
			getAI().startFollow(_owner);
			return;
		}
		
		if (_owner.isDead() || _isFreyaBeast)
		{
			return;
		}
		
		if (isCastingNow())
		{
			return;
		}
		
		final double HPRatio = (_owner.getCurrentHp()) / _owner.getMaxHp();
		
		if (HPRatio >= 0.8)
		{
			for (final Skill skill : getTemplate().getSkills().values())
			{
				if ((skill.getSkillType() == SkillType.DEBUFF) && (Rnd.get(3) < 1) && ((attacker != null) && (attacker.getFirstEffect(skill) != null)))
				{
					sitCastAndFollow(skill, attacker);
				}
			}
		}
		else if (HPRatio < 0.5)
		{
			int chance = 1;
			if (HPRatio < 0.25)
			{
				chance = 2;
			}
			
			for (final Skill skill : getTemplate().getSkills().values())
			{
				if ((Rnd.get(5) < chance) && skill.hasEffectType(EffectType.CPHEAL, EffectType.HEAL, EffectType.HEAL_PERCENT, EffectType.MANAHEAL_BY_LEVEL, EffectType.MANAHEAL_PERCENT))
				{
					sitCastAndFollow(skill, _owner);
				}
			}
		}
	}
	
	protected void sitCastAndFollow(Skill skill, Creature target)
	{
		stopMove(null);
		getAI().setIntention(CtrlIntention.IDLE);
		
		setTarget(target);
		doCast(skill);
		getAI().setIntention(CtrlIntention.FOLLOW, _owner);
	}
	
	private static class CheckDuration implements Runnable
	{
		private final TamedBeastInstance _tamedBeast;
		
		CheckDuration(TamedBeastInstance tamedBeast)
		{
			_tamedBeast = tamedBeast;
		}
		
		@Override
		public void run()
		{
			final int foodTypeSkillId = _tamedBeast.getFoodType();
			final Player owner = _tamedBeast.getOwner();
			
			ItemInstance item = null;
			if (_tamedBeast._isFreyaBeast)
			{
				item = owner.getInventory().getItemByItemId(foodTypeSkillId);
				if ((item != null) && (item.getCount() >= 1))
				{
					owner.destroyItem("BeastMob", item, 1, _tamedBeast, true);
					_tamedBeast.broadcastPacket(new SocialAction(_tamedBeast.getObjectId(), 3));
				}
				else
				{
					_tamedBeast.deleteMe();
				}
			}
			else
			{
				_tamedBeast.setRemainingTime(_tamedBeast.getRemainingTime() - DURATION_CHECK_INTERVAL);
				if (foodTypeSkillId == 2188)
				{
					item = owner.getInventory().getItemByItemId(6643);
				}
				else if (foodTypeSkillId == 2189)
				{
					item = owner.getInventory().getItemByItemId(6644);
				}
				
				if ((item != null) && (item.getCount() >= 1))
				{
					final GameObject oldTarget = owner.getTarget();
					owner.setTarget(_tamedBeast);
					final GameObject[] targets =
					{
					        _tamedBeast
					};
					
					owner.callSkill(SkillsParser.getInstance().getInfo(foodTypeSkillId, 1), targets);
					owner.setTarget(oldTarget);
				}
				else
				{
					if (_tamedBeast.getRemainingTime() < (MAX_DURATION - 300000))
					{
						_tamedBeast.setRemainingTime(-1);
					}
				}
				
				if (_tamedBeast.getRemainingTime() <= 0)
				{
					_tamedBeast.deleteMe();
				}
			}
		}
	}
	
	private class CheckOwnerBuffs implements Runnable
	{
		private final TamedBeastInstance _tamedBeast;
		private final int _numBuffs;
		
		CheckOwnerBuffs(TamedBeastInstance tamedBeast, int numBuffs)
		{
			_tamedBeast = tamedBeast;
			_numBuffs = numBuffs;
		}
		
		@Override
		public void run()
		{
			final Player owner = _tamedBeast.getOwner();
			
			if ((owner == null) || !owner.isOnline())
			{
				deleteMe();
				return;
			}
			
			if (!isInsideRadius(owner, MAX_DISTANCE_FROM_OWNER, true, true))
			{
				getAI().startFollow(owner);
				return;
			}
			
			if (owner.isDead())
			{
				return;
			}
			
			if (isCastingNow())
			{
				return;
			}
			
			int totalBuffsOnOwner = 0;
			int i = 0;
			final int rand = Rnd.get(_numBuffs);
			Skill buffToGive = null;
			
			for (final Skill skill : _tamedBeast.getTemplate().getSkills().values())
			{
				if (skill.getSkillType() == SkillType.BUFF)
				{
					if (i++ == rand)
					{
						buffToGive = skill;
					}
					if (owner.getFirstEffect(skill) != null)
					{
						totalBuffsOnOwner++;
					}
				}
			}
			
			if (((_numBuffs * 2) / 3) > totalBuffsOnOwner)
			{
				_tamedBeast.sitCastAndFollow(buffToGive, owner);
			}
			getAI().setIntention(CtrlIntention.FOLLOW, _tamedBeast.getOwner());
		}
	}
	
	@Override
	public void onAction(Player player, boolean interact, boolean shift)
	{
		if ((player == null) || !canTarget(player))
		{
			return;
		}
		
		if (this != player.getTarget())
		{
			player.setTarget(this);
		}
		else if (interact)
		{
			if (isAutoAttackable(player, false) && (Math.abs(player.getZ() - getZ()) < 100))
			{
				player.getAI().setIntention(CtrlIntention.ATTACK, this);
			}
			else
			{
				player.sendActionFailed();
			}
		}
	}
}