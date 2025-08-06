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
package l2e.gameserver.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import l2e.commons.collections.LazyArrayList;
import l2e.gameserver.Config;
import l2e.gameserver.ai.DefaultAI;
import l2e.gameserver.ai.guard.FortGuardAI;
import l2e.gameserver.ai.guard.GuardAI;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.data.parser.DamageLimitParser;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.TrapInstance;
import l2e.gameserver.model.actor.templates.npc.DamageLimit;
import l2e.gameserver.model.actor.templates.npc.aggro.AggroInfo;
import l2e.gameserver.model.actor.templates.npc.aggro.DamageInfo;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestEventType;

/**
 * Created by LordWinter
 */
public class AggroList
{
	private final Attackable _npc;
	private final Map<Creature, AggroInfo> _aggroList = new ConcurrentHashMap<>();
	private final ReadWriteLock _lock = new ReentrantReadWriteLock();
	private final Lock _readLock = _lock.readLock();
	private final Lock _writeLock = _lock.writeLock();
	
	public AggroList(Attackable npc)
	{
		_npc = npc;
	}

	public AggroInfo get(Creature attacker)
	{
		_readLock.lock();
		try
		{
			return _aggroList.get(attacker);
		}
		finally
		{
			_readLock.unlock();
		}
	}
	
	public void clear()
	{
		clear(false);
	}
	
	public void clear(boolean onlyHate)
	{
		_writeLock.lock();
		try
		{
			if (_aggroList.isEmpty())
			{
				return;
			}
			
			if (!onlyHate)
			{
				_aggroList.clear();
				return;
			}
			
			AggroInfo ai;
			for (final Iterator<AggroInfo> itr = _aggroList.values().iterator(); itr.hasNext();)
			{
				ai = itr.next();
				ai.stopHate();
				if (ai.getDamage() == 0)
				{
					itr.remove();
				}
			}
		}
		finally
		{
			_writeLock.unlock();
		}
	}
	
	public boolean isEmpty()
	{
		_readLock.lock();
		try
		{
			return _aggroList.isEmpty();
		}
		finally
		{
			_readLock.unlock();
		}
	}
	
	public List<Creature> getHateList()
	{
		AggroInfo[] hated;
		
		_readLock.lock();
		try
		{
			if (_aggroList.isEmpty())
			{
				return Collections.emptyList();
			}
			hated = _aggroList.values().toArray(new AggroInfo[_aggroList.size()]);
		}
		finally
		{
			_readLock.unlock();
		}

		try
		{
			Arrays.sort(hated, HateComparator.getInstance());
		}
		catch (final Exception e)
		{}

		if (hated[0].getHate() == 0 && hated[0].getDamage() == 0)
		{
			return Collections.emptyList();
		}

		final List<Creature> hateList = new LazyArrayList<>();
		AggroInfo ai;
		for (int i = 0; i < hated.length; i++)
		{
			ai = hated[i];
			if (ai.getHate() == 0 && ai.getDamage() == 0)
			{
				continue;
			}

			final Creature cha = getOrRemoveHated(ai.getAttacker(), true);
			if (cha == null)
			{
				continue;
			}
			hateList.add(cha);
		}
		return hateList;
	}
	
	public List<AggroInfo> getAggroInfo()
	{
		AggroInfo[] hated;
		
		_readLock.lock();
		try
		{
			if (_aggroList.isEmpty())
			{
				return Collections.emptyList();
			}
			hated = _aggroList.values().toArray(new AggroInfo[_aggroList.size()]);
		}
		finally
		{
			_readLock.unlock();
		}
		
		try
		{
			Arrays.sort(hated, HateComparator.getInstance());
		}
		catch (final Exception e)
		{}
		
		if (hated[0].getHate() == 0 && hated[0].getDamage() == 0)
		{
			return Collections.emptyList();
		}
		
		final List<AggroInfo> hateList = new LazyArrayList<>();
		AggroInfo ai;
		for (int i = 0; i < hated.length; i++)
		{
			ai = hated[i];
			if (ai.getHate() == 0 && ai.getDamage() == 0)
			{
				continue;
			}
			
			final Creature cha = getOrRemoveHated(ai.getAttacker(), true);
			if (cha == null)
			{
				continue;
			}
			hateList.add(ai);
		}
		return hateList;
	}
	
	private Creature getOrRemoveHated(Creature creature, boolean allowAll)
	{
		final GameObject object = GameObjectsStorage.findObject(creature.getObjectId());
		if (object == null)
		{
			remove(creature);
			return null;
		}

		final Creature character = (Creature) object;
		if (character.isPlayer())
		{
			if (character.isDead() && !allowAll)
			{
				stopHating(character);
				return null;
			}
			
			if (!((Player) character).isOnline())
			{
				remove(character);
				return null;
			}
		}
		return character;
	}
	
	public void remove(Creature creature)
	{
		_writeLock.lock();
		try
		{
			final AggroInfo ai = _aggroList.get(creature);
			if (ai != null)
			{
				if (ai.getDamage() == 0)
				{
					_aggroList.remove(creature);
				}
				else
				{
					ai.stopHate();
				}
			}
		}
		finally
		{
			_writeLock.unlock();
		}
	}
	
	public void addAggro(Creature attacker, int damage, int aggro)
	{
		if (attacker == null)
		{
			return;
		}
		
		final Player targetPlayer = attacker.isTrap() ? ((TrapInstance) attacker).getOwner() : attacker.getActingPlayer();
		final AggroInfo ai = attacker.isTrap() ? _aggroList.computeIfAbsent(targetPlayer != null ? targetPlayer : attacker, AggroInfo::new) : _aggroList.computeIfAbsent(attacker, AggroInfo::new);
		
		if (Config.ALLOW_DAMAGE_LIMIT)
		{
			final DamageLimit limit = DamageLimitParser.getInstance().getDamageLimit(_npc.getId());
			if (limit != null)
			{
				final int damageLimit = limit.getDamage();
				if (damageLimit > 0 && damage > damageLimit)
				{
					damage = damageLimit;
				}
			}
		}
		
		if (damage > _npc.getCurrentHp())
		{
			damage = (int) _npc.getCurrentHp();
		}
		
		ai.addDamage(damage);
		ai.addHate(aggro);
		
		if (damage > 1)
		{
			if (attacker.isPlayable())
			{
				final Player player = attacker.getActingPlayer();
				if (player != null)
				{
					if (_npc.getTemplate().getEventQuests(QuestEventType.ON_ATTACK) != null)
					{
						for (final Quest quest : _npc.getTemplate().getEventQuests(QuestEventType.ON_ATTACK))
						{
							quest.notifyAttack(_npc, player, damage, attacker.isSummon(), null);
						}
					}
				}
			}
		}
		if ((targetPlayer != null) && (aggro == 0))
		{
			addAggro(attacker.isTrap() ? targetPlayer : attacker, 0, damage > 2 ? (int) (damage * Config.PATK_HATE_MOD) : 1);
			if (_npc.getAI().getIntention() == CtrlIntention.IDLE)
			{
				_npc.getAI().setIntention(CtrlIntention.ACTIVE);
			}
			
			if (_npc.getTemplate().getEventQuests(QuestEventType.ON_AGGRO_RANGE_ENTER) != null)
			{
				_npc.getTemplate().getEventQuests(QuestEventType.ON_AGGRO_RANGE_ENTER).stream().filter(q -> q != null).forEach(q -> q.notifyAggroRangeEnter(_npc, targetPlayer, attacker.isSummon()));
			}
		}
		else if ((targetPlayer == null) && (aggro == 0))
		{
			aggro = 1;
			ai.addHate(1);
		}
		
		if ((aggro > 0) && (_npc.getAI().getIntention() == CtrlIntention.IDLE))
		{
			_npc.getAI().setIntention(CtrlIntention.ACTIVE);
		}
	}
	
	public void reduceHate(Creature target, int amount, boolean isAddToBlockList)
	{
		if ((_npc.getAI() instanceof GuardAI) || (_npc.getAI() instanceof FortGuardAI))
		{
			stopHating(target);
			_npc.setTarget(null);
			_npc.getAI().setIntention(CtrlIntention.IDLE);
			return;
		}
		
		if (target == null)
		{
			final Creature mostHated = getMostHated();
			if (mostHated == null)
			{
				((DefaultAI) _npc.getAI()).setGlobalAggro(-25);
				return;
			}
			
			for (final AggroInfo ai : _aggroList.values())
			{
				if (ai == null)
				{
					return;
				}
				ai.addHate(-amount);
				if (ai.getHate() <= 0 && isAddToBlockList)
				{
					final var attacker = ai.getAttacker();
					if (attacker != null)
					{
						if (_npc.getDistance(attacker) <= Config.ALT_PARTY_RANGE)
						{
							_npc.addToBlockList(attacker);
						}
					}
				}
			}
			
			amount = getHating(mostHated);
			
			if (amount <= 0)
			{
				((DefaultAI) _npc.getAI()).setGlobalAggro(-25);
				_npc.clearAggroList(false);
				_npc.getAI().setIntention(CtrlIntention.ACTIVE);
				_npc.setWalking();
			}
			return;
		}
		final AggroInfo ai = get(target);
		
		if (ai == null)
		{
			return;
		}
		ai.addHate(-amount);
		
		if (ai.getHate() <= 0)
		{
			final var attacker = ai.getAttacker();
			if (attacker != null && isAddToBlockList)
			{
				_npc.addToBlockList(attacker);
			}
			
			if (getMostHated() == null)
			{
				((DefaultAI) _npc.getAI()).setGlobalAggro(-25);
				_npc.clearAggroList(false);
				_npc.getAI().setIntention(CtrlIntention.ACTIVE);
				_npc.setWalking();
			}
		}
	}
	
	public int getHating(final Creature target)
	{
		if (_aggroList.isEmpty() || (target == null))
		{
			return 0;
		}
		
		final AggroInfo ai = get(target);
		if (ai == null)
		{
			return 0;
		}
		
		if (ai.getAttacker() instanceof Player)
		{
			final Player act = (Player) ai.getAttacker();
			if (act.isInvisible() || act.isSpawnProtected())
			{
				stopHating(target);
				return 0;
			}
		}
		
		if (!ai.getAttacker().isVisible() || ai.getAttacker().isInvisible())
		{
			stopHating(target);
			return 0;
		}
		
		if (ai.getAttacker().isAlikeDead())
		{
			ai.stopHate();
			return 0;
		}
		return ai.getHate();
	}
	
	public void stopHating(Creature target)
	{
		if (target == null)
		{
			return;
		}
		final AggroInfo ai = get(target);
		if (ai != null)
		{
			ai.stopHate();
		}
	}
	
	public Creature getMostHated()
	{
		if (_npc.isAlikeDead())
		{
			return null;
		}
		
		AggroInfo[] hated;
		_readLock.lock();
		try
		{
			if (_aggroList.isEmpty())
			{
				return null;
			}
			hated = _aggroList.values().toArray(new AggroInfo[_aggroList.size()]);
		}
		finally
		{
			_readLock.unlock();
		}
		
		try
		{
			Arrays.sort(hated, HateComparator.getInstance());
		}
		catch (final Exception e)
		{}
		
		if (hated[0].getHate() == 0)
		{
			return null;
		}
		
		AggroInfo ai;
		for (int i = 0; i < hated.length; i++)
		{
			ai = hated[i];
			if (ai.getHate() == 0)
			{
				continue;
			}
			
			final Creature cha = getOrRemoveHated(ai.getAttacker(), false);
			if (cha == null)
			{
				continue;
			}
			return cha;
		}
		return null;
	}
	
	public List<Creature> get2MostHated()
	{
		if (_aggroList.isEmpty() || _npc.isAlikeDead())
		{
			return null;
		}
		
		Creature mostHated = null;
		Creature secondMostHated = null;
		int maxHate = 0;
		final List<Creature> result = new ArrayList<>();
		
		for (final AggroInfo ai : _aggroList.values())
		{
			if (ai == null)
			{
				continue;
			}
			
			if (ai.checkHate() > maxHate)
			{
				secondMostHated = mostHated;
				mostHated = ai.getAttacker();
				maxHate = ai.getHate();
			}
		}
		
		result.add(mostHated);
		
		if (_npc.getAttackByList().contains(secondMostHated))
		{
			result.add(secondMostHated);
		}
		else
		{
			result.add(null);
		}
		return result;
	}
	
	public Creature getTopDamager(Creature lastAttacker)
	{
		if (_aggroList.isEmpty())
		{
			return lastAttacker;
		}
		
		DamageInfo[] hated = null;
		final Map<PlayerGroup, DamageInfo> aggroList = new ConcurrentHashMap<>();
		
		_readLock.lock();
		try
		{
			for (final AggroInfo info : _aggroList.values())
			{
				if (info != null)
				{
					final Player player = info.getAttacker().getActingPlayer();
					if (player != null && info.getDamage() > 0)
					{
						final DamageInfo ai = aggroList.computeIfAbsent(player.getPlayerGroup(), DamageInfo::new);
						if (ai != null)
						{
							ai.addDamage(info.getDamage());
						}
					}
				}
			}
			
			if (!aggroList.isEmpty())
			{
				hated = aggroList.values().toArray(new DamageInfo[aggroList.size()]);
			}
		}
		finally
		{
			_readLock.unlock();
		}
		
		if (hated == null)
		{
			return lastAttacker;
		}
		
		try
		{
			Arrays.sort(hated, DamageComparator.getInstance());
		}
		catch (final Exception e)
		{}
		
		if (hated[0].getDamage() == 0)
		{
			return lastAttacker;
		}
		
		DamageInfo ai;
		for (int i = 0; i < hated.length; i++)
		{
			ai = hated[i];
			if (ai.getDamage() == 0)
			{
				continue;
			}
			
			if (ai.getGroup() != null && ai.getGroup().getGroupLeader() != null)
			{
				if (lastAttacker != null)
				{
					final Player player = lastAttacker.getActingPlayer();
					if (player != null && player.getPlayerGroup() != null && player.getPlayerGroup().getGroupLeader() == ai.getGroup().getGroupLeader())
					{
						return lastAttacker;
					}
					else
					{
						return ai.getGroup().getGroupLeader();
					}
				}
				else
				{
					return ai.getGroup().getGroupLeader();
				}
			}
		}
		return lastAttacker;
	}
	
	private static class DamageComparator implements Comparator<DamageInfo>
	{
		private static Comparator<DamageInfo> instance = new DamageComparator();
		
		public static Comparator<DamageInfo> getInstance()
		{
			return instance;
		}
		
		DamageComparator()
		{
		}
		
		@Override
		public int compare(DamageInfo o1, DamageInfo o2)
		{
			if (o1 == null || o2 == null)
			{
				return 0;
			}
			if (o1 == o2)
			{
				return 0;
			}
			return Integer.compare(o2.getDamage(), o1.getDamage());
		}
	}
	
	private static class HateComparator implements Comparator<AggroInfo>
	{
		private static Comparator<AggroInfo> instance = new HateComparator();
		
		public static Comparator<AggroInfo> getInstance()
		{
			return instance;
		}
		
		HateComparator()
		{
		}
		
		@Override
		public int compare(AggroInfo o1, AggroInfo o2)
		{
			if (o1 == null || o2 == null)
			{
				return 0;
			}
			if (o1 == o2)
			{
				return 0;
			}
			if (o1.getHate() == o2.getHate() && o1.getHate() > 0)
			{
				return Integer.compare(o2.getDamage(), o1.getDamage());
			}
			return Integer.compare(o2.getHate(), o1.getHate());
		}
	}
	
	public Map<Creature, AggroInfo> getCharMap()
	{
		return _aggroList;
	}
}