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
import java.util.stream.Collectors;

import l2e.commons.util.Rnd;
import l2e.fake.FakePlayer;
import l2e.fake.model.BotSkill;
import l2e.fake.model.HealingSpell;
import l2e.fake.model.OffensiveSpell;
import l2e.fake.model.SupportSpell;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.geodata.GeoEngine;
import l2e.gameserver.geodata.pathfinding.AbstractNodeLoc;
import l2e.gameserver.geodata.pathfinding.PathFinding;
import l2e.gameserver.handler.itemhandlers.IItemHandler;
import l2e.gameserver.handler.itemhandlers.ItemHandler;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.instance.ClanHallDoormenInstance;
import l2e.gameserver.model.actor.instance.ClanHallManagerInstance;
import l2e.gameserver.model.actor.instance.TeleporterInstance;
import l2e.gameserver.model.actor.templates.items.Item;
import l2e.gameserver.model.actor.templates.player.FakeLocTemplate;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.items.itemcontainer.Inventory;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.zone.ZoneId;

public abstract class CombatAI extends FakePlayerAI
{
	public CombatAI(FakePlayer character)
	{
		super(character, false);
	}
	
	protected void tryAction(boolean isMage)
	{
		if (_fakePlayer.isInsideZone(ZoneId.PEACE))
		{
			townBehavior(isMage);
		}
		else
		{
			areaBehavior(isMage);
		}
	}
	
	private void townBehavior(boolean isMage)
	{
		if (_fakePlayer.getFakeAi().isTownZone())
		{
			final GameObject target = _fakePlayer.getTarget();
			if (target != null && _fakePlayer.getFakeAi().isTargetLock())
			{
				final double distance = _fakePlayer.getDistance(target);
				if (distance <= 150)
				{
					sleep(Rnd.get(4, 6));
					_fakePlayer.getFakeAi().setTargetLock(false);
					_fakePlayer.setTarget(null);
				}
				else
				{
					if (_fakePlayer.isInPathFinding())
					{
						return;
					}
					
					if (_idleTime > System.currentTimeMillis())
					{
						if (!GeoEngine.getInstance().canSeeTarget(_fakePlayer, target))
						{
							final List<AbstractNodeLoc> path = PathFinding.getInstance().findPath(_fakePlayer, _fakePlayer.getX(), _fakePlayer.getY(), _fakePlayer.getZ(), target.getX(), target.getY(), target.getZ(), _fakePlayer.getReflection(), true, false);
							if (path == null || path.size() < 2)
							{
								_fakePlayer.getAI().setIntention(CtrlIntention.MOVING, target.getLocation(), 40);
								return;
							}
						}
						else
						{
							_fakePlayer.getAI().setIntention(CtrlIntention.MOVING, target.getLocation(), 40);
							return;
						}
					}
					else
					{
						_idleTime = 0;
						_fakePlayer.getFakeAi().setTargetLock(false);
						_fakePlayer.setTarget(null);
					}
				}
			}
			else
			{
				if (Rnd.get(1000) <= 50)
				{
					sitDown(_fakePlayer);
					return;
				}
				
				if (_sitTime <= System.currentTimeMillis())
				{
					if (_fakePlayer.isSitting())
					{
						_fakePlayer.standUp();
						return;
					}
					
					final GameObject newTarget = searchTownNpc(2000);
					if (newTarget != null)
					{
						_fakePlayer.setTarget(newTarget);
						if (!GeoEngine.getInstance().canSeeTarget(_fakePlayer, target))
						{
							final List<AbstractNodeLoc> path = PathFinding.getInstance().findPath(_fakePlayer, _fakePlayer.getX(), _fakePlayer.getY(), _fakePlayer.getZ(), newTarget.getX(), newTarget.getY(), newTarget.getZ(), _fakePlayer.getReflection(), true, false);
							if (path == null || path.size() < 2)
							{
								_fakePlayer.getAI().setIntention(CtrlIntention.MOVING, newTarget.getLocation(), 40);
								_fakePlayer.getFakeAi().setTargetLock(true);
							}
						}
						else
						{
							_fakePlayer.getAI().setIntention(CtrlIntention.MOVING, newTarget.getLocation(), 40);
							_fakePlayer.getFakeAi().setTargetLock(true);
						}
						_idleTime = System.currentTimeMillis() + (60 * 1000);
					}
				}
			}
		}
		else
		{
			if (_fakePlayer.getFakeAi().isWantToFarm() && _waitTime < System.currentTimeMillis())
			{
				final GameObject target = _fakePlayer.getTarget();
				if (target != null && target instanceof TeleporterInstance)
				{
					final double distance = _fakePlayer.getDistance(target);
					if (distance <= 150)
					{
						sleep(Rnd.get(2, 3));
						_fakePlayer.getFakeAi().setWantToFarm(false);
						final Location loc = _fakePlayer.getFakeLocation().getLocation();
						_fakePlayer.getFakeAi().teleportToLocation(loc.getX(), loc.getY(), loc.getZ(), 0);
					}
					else
					{
						if (_fakePlayer.isInPathFinding())
						{
							return;
						}
						
						if (!GeoEngine.getInstance().canSeeTarget(_fakePlayer, target))
						{
							final List<AbstractNodeLoc> path = PathFinding.getInstance().findPath(_fakePlayer, _fakePlayer.getX(), _fakePlayer.getY(), _fakePlayer.getZ(), target.getX(), target.getY(), target.getZ(), _fakePlayer.getReflection(), true, false);
							if (path == null || path.size() < 2)
							{
								_fakePlayer.getAI().setIntention(CtrlIntention.MOVING, target.getLocation(), 40);
								return;
							}
						}
						else
						{
							_fakePlayer.getAI().setIntention(CtrlIntention.MOVING, target.getLocation(), 40);
							return;
						}
						_fakePlayer.getAI().clientStopMoving(null);
						_fakePlayer.getFakeAi().setWantToFarm(false);
						final Location loc = _fakePlayer.getFakeLocation().getLocation();
						_fakePlayer.getFakeAi().teleportToLocation(loc.getX(), loc.getY(), loc.getZ(), 0);
					}
				}
				else
				{
					if (_fakePlayer.isInPathFinding() || _fakePlayer.isMoving())
					{
						return;
					}
					final Creature telepoter = searchTeleporter(5000);
					if (telepoter != null)
					{
						_fakePlayer.setTarget(telepoter);
						if (!GeoEngine.getInstance().canSeeTarget(_fakePlayer, target))
						{
							
							final List<AbstractNodeLoc> path = PathFinding.getInstance().findPath(_fakePlayer, _fakePlayer.getX(), _fakePlayer.getY(), _fakePlayer.getZ(), target.getX(), target.getY(), target.getZ(), _fakePlayer.getReflection(), true, false);
							if (path == null || path.size() < 2)
							{
								_fakePlayer.getAI().setIntention(CtrlIntention.MOVING, target.getLocation(), 40);
								return;
							}
						}
						else
						{
							_fakePlayer.getAI().setIntention(CtrlIntention.MOVING, target.getLocation(), 40);
							return;
						}
						_fakePlayer.getFakeAi().setWantToFarm(false);
						final Location loc = _fakePlayer.getFakeLocation().getLocation();
						_fakePlayer.getFakeAi().teleportToLocation(loc.getX(), loc.getY(), loc.getZ(), 0);
					}
					else
					{
						_fakePlayer.getFakeAi().setWantToFarm(false);
						final Location loc = _fakePlayer.getFakeLocation().getLocation();
						_fakePlayer.getFakeAi().teleportToLocation(loc.getX(), loc.getY(), loc.getZ(), 0);
					}
				}
			}
			else
			{
				townActions();
			}
		}
	}
	
	private void townActions()
	{
		_fakePlayer.getFakeAi().setWantToFarm(true);
	}
	
	private void sitDown(FakePlayer fakePlayer)
	{
		if (!fakePlayer.isSitting())
		{
			fakePlayer.sitDown();
			_sitTime = System.currentTimeMillis() + (Rnd.get(60, 120) * 1000);
		}
	}
	
	private Creature searchTownNpc(int range)
	{
		final List<Npc> targets = new ArrayList<>();
		for (final Npc target : World.getInstance().getAroundNpc(_fakePlayer, range, 400))
		{
			if (target.isWalker() || target instanceof ClanHallDoormenInstance || target instanceof ClanHallManagerInstance)
			{
				continue;
			}
			targets.add(target);
		}
		
		if (targets != null && !targets.isEmpty())
		{
			return targets.get(Rnd.get(targets.size()));
		}
		return null;
	}
	
	private Creature searchTeleporter(int range)
	{
		for (final Npc target : World.getInstance().getAroundNpc(_fakePlayer, range, 400))
		{
			if (target instanceof TeleporterInstance)
			{
				return target;
			}
		}
		return null;
	}
	
	private void areaBehavior(boolean isMage)
	{
		if (Rnd.chance(50))
		{
			checkMp(_fakePlayer);
		}
		
		if (Rnd.chance(70))
		{
			checkHp(_fakePlayer);
		}
		
		if (_fakePlayer.isInWater())
		{
			_fakePlayer.teleToLocation(_fakePlayer.getFakeLocation().getLocation(), true, _fakePlayer.getReflection());
			_pathFindAmount = 0;
			return;
		}
		
		final GameObject target = tryTargetRandomCreatureByTypeInRadius(2000, creature -> GeoEngine.getInstance().canSeeTarget(_fakePlayer, creature) && !creature.isDead());
		if (target != null)
		{
			_fakePlayer.setTarget(target);
			if (isMage)
			{
				tryAttackingUsingMageOffensiveSkill();
			}
			else
			{
				tryAttackingUsingFighterOffensiveSkill();
			}
		}
		else
		{
			final FakeLocTemplate tmpl = _fakePlayer.getFakeLocation();
			if (!_fakePlayer.isInsideRadius(tmpl.getLocation().getX(), tmpl.getLocation().getY(), tmpl.getLocation().getZ(), tmpl.getDistance(), true, true))
			{
				_fakePlayer.setRunning();
				if (!GeoEngine.getInstance().canSeeTarget(_fakePlayer, tmpl.getLocation()))
				{
					final List<AbstractNodeLoc> path = PathFinding.getInstance().findPath(_fakePlayer, _fakePlayer.getX(), _fakePlayer.getY(), _fakePlayer.getZ(), tmpl.getLocation().getX(), tmpl.getLocation().getY(), tmpl.getLocation().getZ(), _fakePlayer.getReflection(), true, false);
					if (path == null || path.size() < 2)
					{
						_fakePlayer.getAI().setIntention(CtrlIntention.MOVING, new Location(tmpl.getLocation().getX(), tmpl.getLocation().getY(), tmpl.getLocation().getZ()), 0);
						sleep(Rnd.get(3, 5));
					}
					else
					{
						if (_pathFindAmount > 5)
						{
							_fakePlayer.teleToLocation(tmpl.getLocation(), true, _fakePlayer.getReflection());
							_pathFindAmount = 0;
							return;
						}
						_pathFindAmount++;
						sleep(Rnd.get(3, 5));
					}
				}
				else
				{
					_fakePlayer.getAI().setIntention(CtrlIntention.MOVING, new Location(tmpl.getLocation().getX(), tmpl.getLocation().getY(), tmpl.getLocation().getZ()), 0);
					sleep(Rnd.get(3, 5));
				}
			}
			else
			{
				sleep(Rnd.get(1, 2));
			}
		}
	}
	
	protected void tryAttackingUsingMageOffensiveSkill()
	{
		if (_fakePlayer.getTarget() != null)
		{
			final BotSkill botSkill = getRandomAvaiableMageSpellForTarget();
			if (botSkill == null)
			{
				return;
			}
			
			final Skill skill = _fakePlayer.getKnownSkill(botSkill.getSkillId());
			if (skill != null)
			{
				if (!castSpell(skill, !skill.isOffensive()) && !_fakePlayer.checkDoCastConditions(skill, false))
				{
					if (Rnd.chance(30))
					{
						_fakePlayer.setCurrentMp(_fakePlayer.getMaxMp());
					}
				}
			}
		}
	}
	
	protected void tryAttackingUsingFighterOffensiveSkill()
	{
		final GameObject target = _fakePlayer.getTarget();
		if (target != null)
		{
			if (getOffensiveSpells() != null && !getOffensiveSpells().isEmpty())
			{
				final Skill skill = getRandomAvaiableFighterSpellForTarget();
				if (skill != null && Rnd.chance(changeOfUsingSkill()))
				{
					_fakePlayer.getAI().setIntention(CtrlIntention.CAST, skill, target);
				}
			}
			
			if (GeoEngine.getInstance().canSeeTarget(_fakePlayer, target))
			{
				_fakePlayer.getAI().setIntention(CtrlIntention.ATTACK, target);
				return;
			}
			_fakePlayer.getAI().setIntention(CtrlIntention.MOVING, target.getLocation(), 0);
		}
	}
	
	@Override
	public void thinkAndAct()
	{
		handleDeath();
	}
	
	protected int getArrowId()
	{
		final ItemInstance weapon = _fakePlayer.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
		if (weapon != null)
		{
			switch (weapon.getItem().getItemGrade())
			{
				case Item.CRYSTAL_S84 :
				case Item.CRYSTAL_S80 :
				case Item.CRYSTAL_S :
					return 1345;
				case Item.CRYSTAL_A :
					return 1344;
				case Item.CRYSTAL_B :
					return 1343;
				case Item.CRYSTAL_C :
					return 1342;
				case Item.CRYSTAL_D :
					return 1341;
				case Item.CRYSTAL_NONE :
					return 17;
			}
		}
		return 0;
	}
	
	protected int getBoltId()
	{
		final ItemInstance weapon = _fakePlayer.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
		if (weapon != null)
		{
			switch (weapon.getItem().getItemGrade())
			{
				case Item.CRYSTAL_S84 :
				case Item.CRYSTAL_S80 :
				case Item.CRYSTAL_S :
					return 9637;
				case Item.CRYSTAL_A :
					return 9636;
				case Item.CRYSTAL_B :
					return 9635;
				case Item.CRYSTAL_C :
					return 9634;
				case Item.CRYSTAL_D :
					return 9633;
				case Item.CRYSTAL_NONE :
					return 9632;
			}
		}
		return 0;
	}
	
	protected void handleShots()
	{
		if (_shotsTime <= System.currentTimeMillis())
		{
			int shotId = 0;
			final ItemInstance weapon = _fakePlayer.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
			if (weapon != null)
			{
				switch (weapon.getItem().getItemGrade())
				{
					case Item.CRYSTAL_S84 :
					case Item.CRYSTAL_S80 :
					case Item.CRYSTAL_S :
						shotId = weapon.getItem().isMagicWeapon() ? 3952 : 1467;
						break;
					case Item.CRYSTAL_A :
						shotId = weapon.getItem().isMagicWeapon() ? 3951 : 1466;
						break;
					case Item.CRYSTAL_B :
						shotId = weapon.getItem().isMagicWeapon() ? 3950 : 1465;
						break;
					case Item.CRYSTAL_C :
						shotId = weapon.getItem().isMagicWeapon() ? 3949 : 1464;
						break;
					case Item.CRYSTAL_D :
						shotId = weapon.getItem().isMagicWeapon() ? 3948 : 1463;
						break;
					case Item.CRYSTAL_NONE :
						shotId = weapon.getItem().isMagicWeapon() ? 3947 : 1835;
						break;
				}
		
				if (_fakePlayer.getInventory().getItemByItemId(shotId) != null)
				{
					if (_fakePlayer.getInventory().getItemByItemId(shotId).getCount() <= 20)
					{
						_fakePlayer.getInventory().addItem("", shotId, 500, _fakePlayer, null);
					}
				}
				else
				{
					_fakePlayer.getInventory().addItem("", shotId, 500, _fakePlayer, null);
				}
		
				if (_fakePlayer.getAutoSoulShot().isEmpty())
				{
					_fakePlayer.addAutoSoulShot(shotId);
					_fakePlayer.rechargeShots(true, true);
				}
			}
			_shotsTime = System.currentTimeMillis() + (3 * 60000);
		}
	}
	
	protected void handleSpiritOre()
	{
		if (_spiritOreTime <= System.currentTimeMillis())
		{
			if (_fakePlayer.getInventory().getItemByItemId(3031) != null)
			{
				if (_fakePlayer.getInventory().getItemByItemId(3031).getCount() <= 20)
				{
					_fakePlayer.getInventory().addItem("", 3031, 500, _fakePlayer, null);
				}
			}
			else
			{
				_fakePlayer.getInventory().addItem("", 3031, 500, _fakePlayer, null);
			}
			_spiritOreTime = System.currentTimeMillis() + (20 * 60000);
		}
	}
	
	public HealingSpell getRandomAvaiableHealingSpellForTarget()
	{
		if (getHealingSpells().isEmpty())
		{
			return null;
		}
		
		final List<HealingSpell> spellsOrdered = getHealingSpells().stream().sorted((o1, o2) -> Integer.compare(o1.getPriority(), o2.getPriority())).collect(Collectors.toList());
		int skillIndex = 0;
		HealingSpell sk = null;
		Skill skill = _fakePlayer.getKnownSkill(spellsOrdered.get(skillIndex).getSkillId());
		if (skill != null)
		{
			sk = spellsOrdered.get(skillIndex);
			while (!_fakePlayer.checkDoCastConditions(skill, false))
			{
				if ((skillIndex < 0) || (skillIndex >= spellsOrdered.size()))
				{
					return null;
				}
				skill = _fakePlayer.getKnownSkill(spellsOrdered.get(skillIndex).getSkillId());
				sk = spellsOrdered.get(skillIndex);
				skillIndex++;
			}
			return sk;
		}
		return null;
	}

	protected BotSkill getRandomAvaiableMageSpellForTarget()
	{
		final List<OffensiveSpell> spellsOrdered = getOffensiveSpells().stream().sorted((o1, o2) -> Integer.compare(o1.getPriority(), o2.getPriority())).collect(Collectors.toList());
		final BotSkill skill = waitAndPickAvailablePrioritisedSpell(spellsOrdered, spellsOrdered.size());
		if (skill != null)
		{
			return skill;
		}
		return null;
	}
	
	private BotSkill waitAndPickAvailablePrioritisedSpell(List<? extends BotSkill> spellsOrdered, int skillListSize)
	{
		int skillIndex = 0;
		BotSkill botSkill = spellsOrdered.get(skillIndex);
		Skill skill = _fakePlayer.getKnownSkill(botSkill.getSkillId());
		if (skill != null)
		{
			if (skill.getCastRange() > 0)
			{
				if (!GeoEngine.getInstance().canSeeTarget(_fakePlayer, _fakePlayer.getTarget()))
				{
					_fakePlayer.getAI().setIntention(CtrlIntention.MOVING, _fakePlayer.getTarget().getLocation(), 0);
					return null;
				}
			}
			
			while (!_fakePlayer.checkDoCastConditions(skill, false))
			{
				_isBusyThinking = true;
				if (_fakePlayer.isDead() || _fakePlayer.isOutOfControl())
				{
					return null;
				}
				if ((skillIndex < 0) || (skillIndex >= skillListSize))
				{
					return null;
				}
				skill = _fakePlayer.getKnownSkill(spellsOrdered.get(skillIndex).getSkillId());
				botSkill = spellsOrdered.get(skillIndex);
				skillIndex++;
			}
			return botSkill;
		}
		return null;
	}
	
	protected Skill getRandomAvaiableFighterSpellForTarget()
	{
		final List<OffensiveSpell> spellsOrdered = getOffensiveSpells().stream().sorted((o1, o2) -> Integer.compare(o1.getPriority(), o2.getPriority())).collect(Collectors.toList());
		int skillIndex = 0;
		Skill skill = _fakePlayer.getKnownSkill(spellsOrdered.get(skillIndex).getSkillId());
		if (skill != null)
		{
			while (!_fakePlayer.checkDoCastConditions(skill, false))
			{
				if ((skillIndex < 0) || (skillIndex >= spellsOrdered.size()))
				{
					return null;
				}
				skill = _fakePlayer.getKnownSkill(spellsOrdered.get(skillIndex).getSkillId());
				skillIndex++;
			}
		
			if (!_fakePlayer.checkDoCastConditions(skill, false))
			{
				if (GeoEngine.getInstance().canSeeTarget(_fakePlayer, _fakePlayer.getTarget()))
				{
					_fakePlayer.getAI().setIntention(CtrlIntention.ATTACK, _fakePlayer.getTarget());
					return null;
				}
				_fakePlayer.getAI().setIntention(CtrlIntention.MOVING, _fakePlayer.getTarget(), 0);
				return null;
			}
			return skill;
		}
		return null;
	}
	
	protected void selfSupportBuffs()
	{
		if (getSelfSupportSpells() == null || getSelfSupportSpells().isEmpty() || _fakePlayer.getFakeAi().isTownZone())
		{
			return;
		}
		
		final List<Integer> activeEffects = Arrays.stream(_fakePlayer.getAllEffects()).map(x -> x.getSkill().getId()).collect(Collectors.toList());
		for (final SupportSpell selfBuff : getSelfSupportSpells())
		{
			if (activeEffects.contains(selfBuff.getSkillId()))
			{
				continue;
			}
			
			Skill skill = _fakePlayer.getKnownSkill(selfBuff.getSkillId());
			if (skill != null)
			{
				skill = SkillsParser.getInstance().getInfo(selfBuff.getSkillId(), _fakePlayer.getSkillLevel(selfBuff.getSkillId()));

				if (!_fakePlayer.checkDoCastConditions(skill, false))
				{
					continue;
				}
			
				switch (selfBuff.getCondition())
				{
					case LESSHPPERCENT :
						if (Math.round(100.0 / _fakePlayer.getMaxHp() * _fakePlayer.getCurrentHp()) <= selfBuff.getConditionValue())
						{
							castSpell(skill, !skill.isOffensive());
						}
						break;
					case MISSINGCP :
						if (getMissingHealth() >= selfBuff.getConditionValue())
						{
							castSpell(skill, !skill.isOffensive());
						}
						break;
					case NONE :
						castSpell(skill, !skill.isOffensive());
					default :
						break;
				}
			}
		}
	}

	private double getMissingHealth()
	{
		return _fakePlayer.getMaxCp() - _fakePlayer.getCurrentCp();
	}

	protected int changeOfUsingSkill()
	{
		return 10;
	}
	
	protected void checkMp(FakePlayer fakePlayer)
	{
		final ItemInstance mpPoint = fakePlayer.getInventory().getItemByItemId(728);
		if (mpPoint != null)
		{
			if (fakePlayer.getCurrentMp() < 0.70 * fakePlayer.getMaxMp())
			{
				final IItemHandler handler = ItemHandler.getInstance().getHandler(mpPoint.getEtcItem());
				if (handler != null)
				{
					handler.useItem(fakePlayer, mpPoint, false);
				}
			}
		}
	}
	
	protected void checkHp(FakePlayer fakePlayer)
	{
		final ItemInstance hpPoint = fakePlayer.getInventory().getItemByItemId(1539);
		if (hpPoint != null)
		{
			if (fakePlayer.getCurrentHp() < 0.95 * fakePlayer.getMaxHp())
			{
				final IItemHandler handler = ItemHandler.getInstance().getHandler(hpPoint.getEtcItem());
				if (handler != null)
				{
					handler.useItem(fakePlayer, hpPoint, false);
				}
			}
		}
	}
	
	protected void sleep(long time)
	{
		try
		{
			Thread.sleep((time * 1000));
		}
		catch (final InterruptedException e)
		{}
	}
	
	protected abstract List<OffensiveSpell> getOffensiveSpells();
	protected abstract List<HealingSpell> getHealingSpells();
	protected abstract List<SupportSpell> getSelfSupportSpells();
}
