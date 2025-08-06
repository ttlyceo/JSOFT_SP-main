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
package l2e.fake.ai;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.stream.Collectors;

import l2e.commons.util.Rnd;
import l2e.fake.FakePlayer;
import l2e.fake.FakePoolManager;
import l2e.fake.PassiveFakeTaskManager;
import l2e.fake.model.FakeSupport;
import l2e.fake.model.SupportSpell;
import l2e.gameserver.Config;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.handler.communityhandlers.impl.CommunityBuffer;
import l2e.gameserver.instancemanager.MapRegionManager;
import l2e.gameserver.instancemanager.TownManager;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.TeleportWhereType;
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.TreasureChestInstance;
import l2e.gameserver.model.actor.templates.player.FakeLocTemplate;
import l2e.gameserver.model.actor.templates.player.FakePassiveLocTemplate;
import l2e.gameserver.model.base.ClassId;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.skills.effects.Effect;
import l2e.gameserver.model.skills.effects.EffectTemplate;
import l2e.gameserver.model.skills.effects.EffectType;
import l2e.gameserver.model.stats.Env;
import l2e.gameserver.model.zone.type.TownZone;

public abstract class FakePlayerAI implements Runnable
{
	protected final FakePlayer _fakePlayer;
	protected boolean _isBusyThinking = false;
	protected int _iterationsOnDeath = 0;
	private final int _toVillageIterationsOnDeath = 10;
	private ScheduledFuture<?> _actionTask;
	private long _buffTime = 0L;
	protected long _idleTime = 0L;
	protected long _shotsTime = 0L;
	protected long _spiritOreTime = 0L;
	private long _arrowTime = 0L;
	protected long _sitTime = 0L;
	protected long _waitTime = 0L;
	private boolean _isPeaceLocation = false;
	private boolean _isWantToFarm = false;
	private boolean _isTargetLock = false;
	protected int _pathFindAmount = 0;
	private final Lock _lock = new ReentrantLock();

	public FakePlayerAI(FakePlayer character, boolean isPassive)
	{
		_fakePlayer = character;
		setup();
		
		if (!isPassive)
		{
			checkFakeLocation();
			applyDefaultBuffs();
			if (_fakePlayer.getClassId() == ClassId.shillienTemplar)
			{
				selfCubicBuffs();
			}
			startActionTask();
			_fakePlayer.setProtection(false);
			_fakePlayer.setTeleportProtection(false);
		}
		else
		{
			final FakePassiveLocTemplate loc = _fakePlayer.getFakeTerritory();
			if (loc != null)
			{
				PassiveFakeTaskManager.getInstance().addSpawnPlayer(_fakePlayer, (Rnd.get(loc.getMinDelay(), loc.getMaxDelay()) * 1000L));
			}
		}
	}
	
	private void checkFakeLocation()
	{
		final Location loc = _fakePlayer.getFakeLocation().getLocation();
		final TownZone zone = TownManager.getTown(loc.getX(), loc.getY(), loc.getZ());
		_isPeaceLocation = zone != null;
	}
	
	@Override
	public void run()
	{
		if (_fakePlayer == null || _fakePlayer.getFakeAi().isBusyThinking())
		{
			return;
		}
		_lock.lock();
		try
		{
			applyDefaultBuffs();
			thinkAndAct();
		}
		finally
		{
			_lock.unlock();
		}
	}
	
	public void setup()
	{
		_fakePlayer.setIsRunning(true);
	}
	
	protected void applyDefaultBuffs()
	{
		if (_buffTime <= System.currentTimeMillis())
		{
			if (_fakePlayer.getLevel() > 8)
			{
				for (final int[] buff : getBuffs())
				{
					final Map<Integer, Effect> activeEffects = Arrays.stream(_fakePlayer.getAllEffects()).filter(x -> x.getEffectType() == EffectType.BUFF).collect(Collectors.toMap(x -> x.getSkill().getId(), x -> x));
					final Skill skill = SkillsParser.getInstance().getInfo(buff[0], buff[1]);
					if (skill != null)
					{
						if (!activeEffects.containsKey(buff[0]))
						{
							final int buffTime = CommunityBuffer.getInstance().getBuffTime(_fakePlayer, skill.getId(), skill.getLevel());
							if (buffTime > 0 && skill.hasEffects())
							{
								final Env env = new Env();
								env.setCharacter(_fakePlayer);
								env.setTarget(_fakePlayer);
								env.setSkill(skill);
								
								Effect ef;
								for (final EffectTemplate et : skill.getEffectTemplates())
								{
									ef = et.getEffect(env);
									if (ef != null)
									{
										ef.setAbnormalTime(buffTime * 60);
										ef.scheduleEffect(true);
									}
								}
							}
							else
							{
								skill.getEffects(_fakePlayer, _fakePlayer, false);
							}
						}
						else
						{
							if ((activeEffects.get(buff[0]).getAbnormalTime() - activeEffects.get(buff[0]).getTime()) <= 20)
							{
								final int buffTime = CommunityBuffer.getInstance().getBuffTime(_fakePlayer, skill.getId(), skill.getLevel());
								if (buffTime > 0 && skill.hasEffects())
								{
									final Env env = new Env();
									env.setCharacter(_fakePlayer);
									env.setTarget(_fakePlayer);
									env.setSkill(skill);
									
									Effect ef;
									for (final EffectTemplate et : skill.getEffectTemplates())
									{
										ef = et.getEffect(env);
										if (ef != null)
										{
											ef.setAbnormalTime(buffTime * 60);
											ef.scheduleEffect(true);
										}
									}
								}
								else
								{
									skill.getEffects(_fakePlayer, _fakePlayer, false);
								}
							}
						}
					}
				}
			}
			_buffTime = System.currentTimeMillis() + (20 * 60000);
		}
	}

	protected void handleDeath()
	{
		if (_fakePlayer.isDead())
		{
			if (_iterationsOnDeath >= _toVillageIterationsOnDeath)
			{
				toVillageOnDeath();
			}
			_iterationsOnDeath++;
			return;
		}
		_iterationsOnDeath = 0;
	}
	
	public void setBusyThinking(boolean thinking)
	{
		_isBusyThinking = thinking;
	}
	
	public boolean isBusyThinking()
	{
		return _isBusyThinking;
	}
	
	protected void teleportToLocation(int x, int y, int z, int randomOffset)
	{
		_fakePlayer.teleToLocation(x, y, z, true, _fakePlayer.getReflection());
		_buffTime = 0L;
		applyDefaultBuffs();
		applyDefaultItems(_fakePlayer);
		_fakePlayer.heal();
		if (_fakePlayer.getClassId() == ClassId.shillienTemplar)
		{
			selfCubicBuffs();
		}
	}
	
	protected GameObject tryTargetRandomCreatureByTypeInRadius(int radius, Function<Creature, Boolean> condition)
	{
		GameObject target = _fakePlayer.getTarget();
		if (target != null && ((Creature) target).isDead())
		{
			_fakePlayer.setTarget(null);
			target = null;
		}
		
		if (target == null)
		{
			boolean havePlayerTarget = false;
			final List<Creature> result = new ArrayList<>();
			for (final Creature obj : World.getInstance().getAroundCharacters(_fakePlayer, radius, 300))
			{
				if (obj.isPlayer())
				{
					final Player pl = (Player) obj;
					final boolean canAttack = _fakePlayer.getLevel() >= pl.getLevel() || (pl.getLevel() - _fakePlayer.getLevel()) <= 5;
					if (pl.isVisible() && !pl.isDead() && pl.getPvpFlag() > 0 && _fakePlayer.getDistance(pl) < 1500 && canAttack && condition.apply(pl))
					{
						result.add(pl);
						havePlayerTarget = true;
					}
				}
				
				if (obj instanceof Attackable && !havePlayerTarget)
				{
					final Attackable npc = (Attackable) obj;
					if (!npc.isMonster() || npc.isDead() || !npc.isVisible() || (npc instanceof TreasureChestInstance) || npc.isRaid() || npc.isRaidMinion() || !condition.apply(npc))
					{
						continue;
					}
					
					if (npc.isInTargetList(_fakePlayer))
					{
						result.add(npc);
						continue;
					}
				}
			}
			
			if (!result.isEmpty())
			{
				final GameObject closestTarget = result.stream().min((o1, o2) -> Integer.compare((int) Math.sqrt(_fakePlayer.getDistanceSq(o1)), (int) Math.sqrt(_fakePlayer.getDistanceSq(o2)))).get();
				result.clear();
				return closestTarget;
			}
		}
		else
		{
			if (target.isPlayer())
			{
				if (target.getActingPlayer().isDead() || target.getActingPlayer().getPvpFlag() == 0)
				{
					_fakePlayer.setTarget(null);
					target = null;
				}
			}
		}
		return target;
	}
	
	public boolean castSpell(Skill skill, boolean isSelf)
	{
		if (isSelf)
		{
			final GameObject oldTarget = _fakePlayer.getTarget();
			_fakePlayer.setTarget(_fakePlayer);
			_fakePlayer.useMagic(skill, false, false, false);
			_fakePlayer.setTarget(oldTarget);
			return true;
		}
		_fakePlayer.useMagic(skill, true, false, false);
		return true;
	}
	
	protected void toVillageOnDeath()
	{
		final Location location = MapRegionManager.getInstance().getTeleToLocation(_fakePlayer, TeleportWhereType.TOWN);
		if (_fakePlayer.isDead())
		{
			_fakePlayer.doRevive();
		}
		
		final FakeLocTemplate template = _fakePlayer.getFakeLocation();
		if (_fakePlayer.getLevel() >= template.getMaxLvl())
		{
			FakeSupport.setLevel(_fakePlayer, template.getMinLvl());
		}
		_waitTime = System.currentTimeMillis() + (Config.FAKE_DELAY_TELEPORT_TO_FARM * 60000L);
		_fakePlayer.getFakeAi().teleportToLocation(location.getX(), location.getY(), location.getZ(), 20);
	}

	protected void applyDefaultItems(FakePlayer fakePlayer)
	{
		if (fakePlayer.getInventory().getItemByItemId(1539) != null)
		{
			if (fakePlayer.getInventory().getItemByItemId(1539).getCount() <= 10)
			{
				fakePlayer.getInventory().addItem("", 1539, 100, _fakePlayer, null);
			}
		}
		else
		{
			fakePlayer.getInventory().addItem("", 1539, 100, _fakePlayer, null);
		}
		
		if (fakePlayer.getInventory().getItemByItemId(728) != null)
		{
			if (fakePlayer.getInventory().getItemByItemId(1539).getCount() <= 5)
			{
				fakePlayer.getInventory().addItem("", 728, 50, _fakePlayer, null);
			}
		}
		else
		{
			fakePlayer.getInventory().addItem("", 728, 50, _fakePlayer, null);
		}
	}
	
	protected void selfCubicBuffs()
	{
		final List<SupportSpell> _cubics = new ArrayList<>();
		_cubics.add(new SupportSpell(33, 1));
		_cubics.add(new SupportSpell(22, 1));
		_cubics.add(new SupportSpell(278, 1));
		final SupportSpell rndSkill = _cubics.get(Rnd.get(_cubics.size()));
		
		final Skill skill = SkillsParser.getInstance().getInfo(rndSkill.getSkillId(), _fakePlayer.getSkillLevel(rndSkill.getSkillId()));
		castSpell(skill, true);
	}
	
	public abstract void thinkAndAct();
	protected abstract int[][] getBuffs();
	
	private synchronized void startActionTask()
	{
		if (_actionTask == null)
		{
			_actionTask = FakePoolManager.getInstance().scheduleAtFixedRate(this, 500L, 500L);
		}
	}
	
	protected synchronized void stopActionTask()
	{
		if (_actionTask != null)
		{
			_actionTask.cancel(true);
			_actionTask = null;
		}
	}
	
	public long getArrowTime()
	{
		return _arrowTime;
	}
	
	public void setArrowTime(long time)
	{
		_arrowTime = time;
	}
	
	public boolean isTownZone()
	{
		return _isPeaceLocation;
	}
	
	public boolean isWantToFarm()
	{
		return _isWantToFarm;
	}
	
	public void setWantToFarm(boolean isWant)
	{
		_isWantToFarm = isWant;
	}
	
	public boolean isTargetLock()
	{
		return _isTargetLock;
	}
	
	public void setTargetLock(boolean isWant)
	{
		_isTargetLock = isWant;
	}
}
