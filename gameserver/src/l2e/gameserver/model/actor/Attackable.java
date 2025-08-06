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
package l2e.gameserver.model.actor;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import l2e.commons.util.Rnd;
import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.SevenSigns;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.ai.DefaultAI;
import l2e.gameserver.ai.character.CharacterAI;
import l2e.gameserver.ai.model.CtrlEvent;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.data.parser.DamageLimitParser;
import l2e.gameserver.data.parser.ExperienceParser;
import l2e.gameserver.data.parser.ItemsParser;
import l2e.gameserver.handler.voicedcommandhandlers.VoicedCommandHandler;
import l2e.gameserver.instancemanager.CursedWeaponsManager;
import l2e.gameserver.instancemanager.DailyTaskManager;
import l2e.gameserver.instancemanager.VipManager;
import l2e.gameserver.instancemanager.WalkingManager;
import l2e.gameserver.model.AggroList;
import l2e.gameserver.model.CommandChannel;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.Party;
import l2e.gameserver.model.Seed;
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.instance.GrandBossInstance;
import l2e.gameserver.model.actor.instance.ServitorInstance;
import l2e.gameserver.model.actor.status.AttackableStatus;
import l2e.gameserver.model.actor.tasks.character.NotifyAITask;
import l2e.gameserver.model.actor.tasks.npc.OnKillByMobNotifyTask;
import l2e.gameserver.model.actor.tasks.npc.OnKillNotifyTask;
import l2e.gameserver.model.actor.templates.items.Item;
import l2e.gameserver.model.actor.templates.npc.DamageLimit;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.actor.templates.npc.aggro.AggroInfo;
import l2e.gameserver.model.actor.templates.npc.aggro.GroupInfo;
import l2e.gameserver.model.actor.templates.npc.aggro.RewardInfo;
import l2e.gameserver.model.actor.templates.npc.champion.ChampionRewardItem;
import l2e.gameserver.model.actor.templates.player.vip.VipNpcTemplate;
import l2e.gameserver.model.entity.events.EventsDropManager;
import l2e.gameserver.model.holders.ItemHolder;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestEventType;
import l2e.gameserver.model.reward.RewardItem;
import l2e.gameserver.model.reward.RewardList;
import l2e.gameserver.model.reward.RewardType;
import l2e.gameserver.model.service.BotFunctions;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.skills.effects.AbnormalEffect;
import l2e.gameserver.model.stats.Stats;
import l2e.gameserver.model.zone.ZoneId;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.SystemMessage;
import l2e.gameserver.taskmanager.DecayTaskManager;
import l2e.gameserver.taskmanager.ItemsAutoDestroy;

public class Attackable extends Npc
{
	private Class<DefaultAI> _classAI = DefaultAI.class;
	@SuppressWarnings("unchecked")
	private Constructor<DefaultAI> _constructorAI = (Constructor<DefaultAI>) DefaultAI.class.getConstructors()[0];
	
	private boolean _isRaid = false;
	private boolean _isEpicRaid = false;
	private boolean _isSiegeGuard = false;
	private boolean _isRaidMinion = false;
	private boolean _isLethalImmune = false;
	private boolean _isGlobalAI = false;
	private boolean _isPassiveSweepActive = false;
	
	private boolean _isReturningToSpawnPoint = false;
	private boolean _canReturnToSpawnPoint = true;
	private boolean _seeThroughSilentMove = false;
	private List<RewardItem> _sweepItems;
	private final AggroList _aggroList;
	private final Lock _sweepLock = new ReentrantLock();
	private ItemHolder[] _harvestItems;
	private boolean _seeded;
	private Seed _seed = null;
	private int _seederObjId = 0;

	private boolean _overhit;
	private double _overhitDamage;
	private Creature _overhitAttacker;

	private boolean _isSpoil = false;
	private int _isSpoiledBy = 0;

	private Set<Integer> _absorbersIds;
	private Set<Integer> _blockList;

	protected int _onKillDelay = 5000;
	protected long _findTargetDelay = 0;
	private final Set<Integer> _targetList = ConcurrentHashMap.newKeySet();

	public Attackable(int objectId, NpcTemplate template)
	{
		super(objectId, template);
		setInstanceType(InstanceType.Attackable);
		setIsInvul(false);
		if (template.getCanSeeInSilentMove())
		{
			setSeeThroughSilentMove(true);
		}
		
		if (template.isLethalImmune())
		{
			setIsLethalImmune(true);
		}
		_aggroList = new AggroList(this);
		_canReturnToSpawnPoint = getTemplate().getParameter("canReturnToSpawnPoint", true);
	}

	@Override
	public AttackableStatus getStatus()
	{
		return (AttackableStatus) super.getStatus();
	}

	@Override
	public void initCharStatus()
	{
		setStatus(new AttackableStatus(this));
	}

	@Override
	@SuppressWarnings("unchecked")
	protected CharacterAI initAI()
	{
		Class<DefaultAI> classAI = null;
		DefaultAI constructorAI = null;

		try
		{
			classAI = (Class<DefaultAI>) Class.forName("l2e.gameserver.ai.npc." + getAiType());
		}
		catch (final ClassNotFoundException e)
		{
			try
			{
				classAI = (Class<DefaultAI>) Class.forName("l2e.scripts.ai." + getAiType());
			}
			catch (final ClassNotFoundException e1)
			{
				e1.printStackTrace();
			}
		}
		
		if (classAI == null)
		{
			_log.warn("Not found type class for type: " + getAiType() + ". NpcId: " + getId());
		}
		else
		{
			_classAI = classAI;
			_constructorAI = (Constructor<DefaultAI>) _classAI.getConstructors()[0];
		}
		
		try
		{
			constructorAI = _constructorAI.newInstance(this);
		}
		catch (
		    IllegalAccessException | InvocationTargetException | InstantiationException | IllegalArgumentException e)
		{
			_log.warn("Unable to create ai of NpcId: " + getId());
		}
		return constructorAI;
	}

	public final boolean isReturningToSpawnPoint()
	{
		return _isReturningToSpawnPoint;
	}

	public final void setisReturningToSpawnPoint(boolean value)
	{
		_isReturningToSpawnPoint = value;
	}

	public final boolean canReturnToSpawnPoint()
	{
		return _canReturnToSpawnPoint;
	}

	public final void setCanReturnToSpawnPoint(boolean value)
	{
		_canReturnToSpawnPoint = value;
	}

	public boolean canSeeThroughSilentMove()
	{
		return _seeThroughSilentMove;
	}

	public void setSeeThroughSilentMove(boolean val)
	{
		_seeThroughSilentMove = val;
	}

	public void useMagic(Skill skill)
	{
		if ((skill == null) || isAlikeDead() || skill.isPassive() || isCastingNow() || isSkillDisabled(skill) || isSkillBlocked(skill))
		{
			return;
		}

		final var mpConsume = getStat().getMpConsume(skill)[0];
		if ((getCurrentMp() < (mpConsume + getStat().getMpInitialConsume(skill))) || (getCurrentHp() <= skill.getHpConsume()))
		{
			return;
		}

		if (!skill.isStatic())
		{
			if (skill.isMagic())
			{
				if (isMuted())
				{
					return;
				}
			}
			else
			{
				if (isPhysicalMuted())
				{
					return;
				}
			}
		}

		final GameObject target = skill.getFirstOfTargetList(this);
		if (target != null)
		{
			getAI().setIntention(CtrlIntention.CAST, skill, target);
		}
	}

	@Override
	public void reduceCurrentHp(double damage, Creature attacker, Skill skill)
	{
		if (Config.ALLOW_DAMAGE_LIMIT)
		{
			final DamageLimit limit = DamageLimitParser.getInstance().getDamageLimit(getId());
			if (limit != null)
			{
				final int damageLimit = skill != null ? skill.isMagic() ? limit.getMagicDamage() : limit.getPhysicDamage() : limit.getDamage();
				if (damageLimit > 0 && damage > damageLimit)
				{
					damage = damageLimit;
				}
			}
		}
		reduceCurrentHp(damage, attacker, true, false, skill);
	}

	@Override
	public void reduceCurrentHp(double damage, Creature attacker, boolean awake, boolean isDOT, Skill skill)
	{
		if (isEventMob())
		{
			return;
		}

		if (attacker != null && skill != null && !isInvul())
		{
			addDamage(attacker, (int) damage, skill);
			if (!Config.ALLOW_UNLIM_ENTER_CATACOMBS && isSevenSignsMonster())
			{
				final Player player = attacker.getActingPlayer();
				if (player != null)
				{
					if ((SevenSigns.getInstance().isSealValidationPeriod()) || (SevenSigns.getInstance().isCompResultsPeriod()))
					{
						final int pcabal = SevenSigns.getInstance().getPlayerCabal(player.getObjectId());
						final int wcabal = SevenSigns.getInstance().getCabalHighestScore();
						if ((pcabal != wcabal) && (wcabal != SevenSigns.CABAL_NULL))
						{
							player.sendMessage("You have been teleported to the nearest town because you not signed for winning cabal.");
							player.teleToClosestTown();
							return;
						}
					}
				}
			}
			
			if (attacker.isPlayer() && !_targetList.contains(attacker))
			{
				addToTargetList(attacker.getActingPlayer());
			}
		}
		super.reduceCurrentHp(damage, attacker, awake, isDOT, skill);
	}

	@Override
	protected void onDeath(Creature killer)
	{
		super.onDeath(killer);
		
		_targetList.clear();
		clearBlockList();
		
		if (killer != null && killer.isPlayable())
		{
			if (getTemplate().getEventQuests(QuestEventType.ON_KILL) != null)
			{
				ThreadPoolManager.getInstance().schedule(new OnKillNotifyTask(this, getTemplate().getEventQuests(QuestEventType.ON_KILL), killer.getActingPlayer(), (killer != null && killer.isSummon())), Config.NPC_DEAD_TIME_TASK * 1000L);
			}
				
			if (Config.ALLOW_DAILY_TASKS)
			{
				final var pl = killer.getActingPlayer();
				if (pl != null && (isMonster() || isRaid()))
				{
					var isForAll = false;
					for (final var taskTemplate : pl.getActiveDailyTasks())
					{
						if (taskTemplate.getType().equalsIgnoreCase("Farm") && !taskTemplate.isComplete())
						{
							final var task = DailyTaskManager.getInstance().getDailyTask(taskTemplate.getId());
							if ((task.getNpcId() == getId()) && (taskTemplate.getCurrentNpcCount() < task.getNpcCount()))
							{
								taskTemplate.setCurrentNpcCount((taskTemplate.getCurrentNpcCount() + 1));
							}
							
							isForAll = task.isForAll();
								
							if (taskTemplate.isComplete())
							{
								final var vch = VoicedCommandHandler.getInstance().getHandler("missions");
								if (vch != null)
								{
									pl.updateDailyStatus(taskTemplate);
									vch.useVoicedCommand("missions", pl, null);
								}
							}
						}
					}
					
					if (isForAll && pl.isInParty())
					{
						for (final var member : pl.getParty().getMembers())
						{
							if (member != null && !member.isDead() && member != pl && member.isInRange(getLocation(), Config.ALT_PARTY_RANGE2))
							{
								for (final var taskTemplate : member.getActiveDailyTasks())
								{
									if (taskTemplate.getType().equalsIgnoreCase("Farm") && !taskTemplate.isComplete())
									{
										final var task = DailyTaskManager.getInstance().getDailyTask(taskTemplate.getId());
										if ((task.getNpcId() == getId()) && (taskTemplate.getCurrentNpcCount() < task.getNpcCount()))
										{
											taskTemplate.setCurrentNpcCount((taskTemplate.getCurrentNpcCount() + 1));
										}
										
										isForAll = task.isForAll();
										
										if (taskTemplate.isComplete())
										{
											final var vch = VoicedCommandHandler.getInstance().getHandler("missions");
											if (vch != null)
											{
												member.updateDailyStatus(taskTemplate);
												vch.useVoicedCommand("missions", member, null);
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
		else if (killer != null && killer.isNpc())
		{
			if (getTemplate().getEventQuests(QuestEventType.ON_KILL) != null)
			{
				ThreadPoolManager.getInstance().schedule(new OnKillByMobNotifyTask(this, getTemplate().getEventQuests(QuestEventType.ON_KILL), (Npc) killer), Config.NPC_DEAD_TIME_TASK * 1000L);
			}
		}
	}
	
	@Override
	protected void onDespawn()
	{
		clearAggroList(false);
		super.onDespawn();
	}
	
	@Override
	protected void calculateRewards(Creature lastAttacker)
	{
		final Creature topDamager = getAggroList().getTopDamager(lastAttacker);
		final Map<Player, RewardInfo> rewards = new ConcurrentHashMap<>();
		long totalDamage = 0;
		
		for (final AggroInfo info : getAggroList().getCharMap().values())
		{
			if (info == null)
			{
				continue;
			}

			final Player attacker = info.getAttacker().getActingPlayer();
			if (attacker != null)
			{
				final int damage = info.getDamage();
				if (damage > 1)
				{
					totalDamage += damage;
					final RewardInfo ai = rewards.computeIfAbsent(attacker, RewardInfo::new);
					if (ai != null)
					{
						ai.addDamage(damage);
					}
				}
			}
		}

		doItemDrop(lastAttacker, topDamager != null ? topDamager : lastAttacker);

		if (!rewards.isEmpty())
		{
			double[] expSp;
			for (final RewardInfo reward : rewards.values())
			{
				if (reward == null)
				{
					continue;
				}

				final Player attacker = reward.getAttacker();
				final int damage = reward.getDamage();
				final Party attackerParty = attacker.getParty();
				final float penalty = attacker.hasServitor() ? ((ServitorInstance) attacker.getSummon()).getExpPenalty() : 0;
				
				if (!Util.checkIfInRange(Config.ALT_PARTY_RANGE, this, attacker, true))
				{
					continue;
				}
				
				if (attackerParty == null || attackerParty.getMemberCount() < 2)
				{
					if (World.getInstance().getAroundCharacters(attacker).contains(this))
					{
						final int levelDiff = attacker.getLevel() - getLevel();
						expSp = calculateExpAndSp(levelDiff, damage, totalDamage, attacker);
						double exp = expSp[0];
						double sp = expSp[1];
						
						if (attacker.isPlayer() && attacker.getPremiumBonus().isPersonal())
						{
							exp *= attacker.getPremiumBonus().getRateXp();
							sp *= attacker.getPremiumBonus().getRateSp();
						}

						if (getChampionTemplate() != null)
						{
							exp *= getChampionTemplate().expMultiplier;
							sp *= getChampionTemplate().spMultiplier;
						}

						exp *= 1 - penalty;

						final Creature overhitAttacker = getOverhitAttacker();
						if (isOverhit() && (overhitAttacker != null) && (overhitAttacker.getActingPlayer() != null) && (attacker == overhitAttacker.getActingPlayer()))
						{
							attacker.sendPacket(SystemMessageId.OVER_HIT);
							exp += calculateOverhitExp((long) exp);
						}

						if (!attacker.isDead())
						{
							exp *= attacker.getRExp();
							sp *= attacker.getRSp();
							final long addexp = Math.round(attacker.calcStat(Stats.EXPSP_RATE, exp, null, null));
							final int addsp = (int) attacker.calcStat(Stats.EXPSP_RATE, sp, null, null);

							attacker.addExpAndSp(addexp, addsp, useVitalityRate());
							if (addexp > 0)
							{
								if (!attacker.getNevitSystem().isActive() && attacker.getNevitSystem().getTime() > 0 && !attacker.isInsideZone(ZoneId.PEACE))
								{
									if ((attacker.getLevel() - getLevel()) <= 9)
									{
										final int nevitPoints = Math.round(((addexp / (getLevel() * getLevel())) * 100) / 20);
										attacker.getNevitSystem().addPoints(nevitPoints);
									}
								}
								attacker.updateVitalityPoints(getVitalityPoints(damage), true, false);
							}
						}
					}
				}
				else
				{
					int partyDmg = 0;
					float partyMul = 1;
					int partyLvl = 0;
					
					final List<Player> rewardedMembers = new ArrayList<>();
					final List<Player> groupMembers = attackerParty.getMembers();
					for (final Player partyPlayer : groupMembers)
					{
						if ((partyPlayer == null) || partyPlayer.isDead())
						{
							continue;
						}

						final RewardInfo reward2 = rewards.get(partyPlayer);
						if (reward2 != null)
						{
							if (Util.checkIfInRange(Config.ALT_PARTY_RANGE, this, partyPlayer, true))
							{
								partyDmg += reward2.getDamage();
								rewardedMembers.add(partyPlayer);
								if (partyPlayer.getLevel() > partyLvl)
								{
									partyLvl = partyPlayer.getLevel();
								}
							}
							rewards.remove(partyPlayer);
						}
						else
						{
							if (Util.checkIfInRange(Config.ALT_PARTY_RANGE, this, partyPlayer, true))
							{
								rewardedMembers.add(partyPlayer);
								if (partyPlayer.getLevel() > partyLvl)
								{
									partyLvl = partyPlayer.getLevel();
								}
							}
						}
					}

					if (partyDmg < totalDamage)
					{
						partyMul = ((float) partyDmg / totalDamage);
					}
					final int levelDiff = partyLvl - getLevel();
					expSp = calculateExpAndSp(levelDiff, partyDmg, totalDamage, attacker);
					double exp_premium = expSp[0];
					double sp_premium = expSp[1];

					expSp = calculateExpAndSp(levelDiff, partyDmg, totalDamage, attacker);
					double exp = expSp[0];
					double sp = expSp[1];

					if (getChampionTemplate() != null)
					{
						exp *= getChampionTemplate().expMultiplier;
						sp *= getChampionTemplate().spMultiplier;
						exp_premium *= getChampionTemplate().expMultiplier;
						sp_premium *= getChampionTemplate().spMultiplier;
					}

					exp *= partyMul;
					sp *= partyMul;
					exp_premium *= partyMul;
					sp_premium *= partyMul;

					final Creature overhitAttacker = getOverhitAttacker();
					if (isOverhit() && (overhitAttacker != null) && (overhitAttacker.getActingPlayer() != null) && (attacker == overhitAttacker.getActingPlayer()))
					{
						attacker.sendPacket(SystemMessageId.OVER_HIT);
						exp += calculateOverhitExp((long) exp);
						exp_premium += calculateOverhitExp((long) exp_premium);
					}

					if (partyDmg > 0)
					{
						attackerParty.distributeXpAndSp((long) exp_premium, (int) sp_premium, (long) exp, (int) sp, rewardedMembers, partyLvl, partyDmg, this);
					}
				}
			}
		}
	}

	@Override
	public void addAttackerToAttackByList(Creature player)
	{
		if ((player == null) || (player == this) || getAttackByList().contains(player))
		{
			return;
		}
		getAttackByList().add(player);
	}

	public void addDamage(Creature attacker, int damage, Skill skill)
	{
		if (attacker == null)
		{
			return;
		}

		if (!isDead())
		{
			try
			{
				if (isWalker() && !isCoreAIDisabled() && WalkingManager.getInstance().isOnWalk(this))
				{
					WalkingManager.getInstance().stopMoving(this, false, true);
				}
				
				getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, attacker, damage);

				if (attacker.isPlayable())
				{
					final Player player = attacker.getActingPlayer();
					if (player != null)
					{
						if (getTemplate().getEventQuests(QuestEventType.ON_ATTACK) != null)
						{
							for (final Quest quest : getTemplate().getEventQuests(QuestEventType.ON_ATTACK))
							{
								quest.notifyAttack(this, player, damage, attacker.isSummon(), skill);
							}
						}
					}
				}
			}
			catch (final Exception e)
			{
				_log.warn("", e);
			}
		}
	}
	
	public void addDamageHate(Creature attacker, int damage, int aggro)
	{
		getAggroList().addAggro(attacker, damage, aggro);
	}
	
	private void calcVipPointsReward(long totalPoints)
	{
		final Map<Object, GroupInfo> groupsInfo = new HashMap<>();
		final double totalHp = getMaxHp();

		for (final AggroInfo ai : getAggroList().getCharMap().values())
		{
			final Player player = ai.getAttacker().getActingPlayer();
			if (player != null)
			{
				final Object key = player.getParty() != null ? player.getParty().getCommandChannel() != null ? player.getParty().getCommandChannel() : player.getParty() : player.getActingPlayer();
				GroupInfo info = groupsInfo.get(key);
				if (info == null)
				{
					info = new GroupInfo();
					groupsInfo.put(key, info);
				}
				
				if (key instanceof CommandChannel)
				{
					for (final Player p : ((CommandChannel) key))
					{
						if (p.isInRangeZ(this, Config.ALT_PARTY_RANGE2))
						{
							info.getPlayer().add(p);
						}
					}
				}
				else if (key instanceof Party)
				{
					for (final Player p : ((Party) key).getMembers())
					{
						if (p.isInRangeZ(this, Config.ALT_PARTY_RANGE2))
						{
							info.getPlayer().add(p);
						}
					}
				}
				else
				{
					info.getPlayer().add(player);
				}
				info.addReward(ai.getDamage());
			}
		}
		
		for (final GroupInfo groupInfo : groupsInfo.values())
		{
			final HashSet<Player> players = groupInfo.getPlayer();
			final int perPlayer = (int) Math.round(totalPoints * groupInfo.getReward() / (totalHp * players.size()));
			for (final Player player : players)
			{
				if (player != null)
				{
					int playerReward = perPlayer;
					playerReward = (int) Math.round(playerReward * ExperienceParser.getInstance().penaltyModifier(calculateLevelDiffForDrop(player.getLevel()), 9));
					if (playerReward == 0)
					{
						continue;
					}
					player.setVipPoints(player.getVipPoints() + playerReward);
				}
			}
		}
	}
	
	public void doItemDrop(Creature lastAttacker, Creature mainDamageDealer)
	{
		doItemDrop(getTemplate(), lastAttacker, mainDamageDealer);
	}

	public void doItemDrop(NpcTemplate npcTemplate, Creature lastAttacker, Creature mainDamageDealer)
	{
		if (mainDamageDealer == null)
		{
			return;
		}

		final Player player = mainDamageDealer.getActingPlayer();
		if (player == null || mainDamageDealer.isFakePlayer())
		{
			return;
		}
		
		if (Config.ALLOW_VIP_SYSTEM)
		{
			final VipNpcTemplate vipNpc = VipManager.getInstance().getNpcTemplate(getId());
			if (vipNpc != null)
			{
				calcVipPointsReward(vipNpc.getPoints());
			}
			else
			{
				player.setVipPoints(player.getVipPoints() + 1);
			}
		}

		if (isMonster() && getReflectionId() == 0)
		{
			CursedWeaponsManager.getInstance().checkDrop(this, player);
		}
		
		if (isSiegeGuard() && Config.EPAULETTE_ONLY_FOR_REG)
		{
			return;
		}
		
		player.getCounters().addAchivementInfo("killbyId", getId(), -1, false, true, false);
		
		if (isMonster() && !isRaid() && !isMinion())
		{
			player.getCounters().addAchivementInfo("monsterKiller", getId(), -1, false, true, false);
		}
		
		for (final Map.Entry<RewardType, RewardList> entry : npcTemplate.getRewards().entrySet())
		{
			rollRewards(entry, lastAttacker, mainDamageDealer);
		}
		
		if (!isSpoil() && player.isPassiveSpoil())
		{
			final List<RewardItem> items = takeSweep();
			if (items != null && items.size() > 0)
			{
				_isPassiveSweepActive = true;
				final var overweight = !player.isInventoryUnderRepcent(false, 100.);
				for (final var item : items)
				{
					if (player.isInParty() && (player.getParty().getLootDistribution() == 2 || BotFunctions.getInstance().isAutoSpoilEnable(player)))
					{
						player.getParty().distributeItem(player, item._itemId, item._count, true, this);
						continue;
					}
					
					final var template = item.getTemplate();
					if (template == null)
					{
						continue;
					}
					
					if (overweight && (!template.isStackable() || (template.isStackable() && player.getInventory().getItemByItemId(item._itemId) == null)))
					{
						continue;
					}
					player.addItem("Sweeper", item._itemId, item._count, this, true);
				}
				DecayTaskManager.getInstance().addDecayTask(this, 2000, false);
			}
		}
		
		if (getChampionTemplate() != null)
		{
			player.getCounters().addAchivementInfo("championKiller", getId(), -1, false, true, false);
			final double mod = 1.0 * ExperienceParser.getInstance().penaltyModifier(calculateLevelDiffForDrop(player.getLevel()), 9);
			if (mod > 0)
			{
				for (final ChampionRewardItem ri : getChampionTemplate().rewards)
				{
					if (ri != null)
					{
						if (Rnd.get(100) < (ri.getDropChance() * mod))
						{
							final long count = Rnd.get(ri.getMinCount(), ri.getMaxCount());
							final Item itemTemplate = ItemsParser.getInstance().getTemplate(ri.getItemId());
							if (itemTemplate != null)
							{
								final ItemHolder item = new ItemHolder(ri.getItemId(), count);
								if ((((player.getUseAutoLoot() || Config.AUTO_LOOT || player.getFarmSystem().isAutofarming()) && !itemTemplate.isHerb()) || isFlying()) || ((player.getUseAutoLootHerbs() || Config.AUTO_LOOT_HERBS) && itemTemplate.isHerb()))
								{
									player.doAutoLoot(this, itemTemplate.getId(), count);
								}
								else if (Config.AUTO_LOOT_BY_ID_SYSTEM)
								{
									if (Arrays.binarySearch(Config.AUTO_LOOT_BY_ID, item.getId()) >= 0)
									{
										player.doAutoLoot(this, item);
									}
									else
									{
										dropItem(player, item);
									}
								}
								else
								{
									dropItem(player, item);
								}
							}
						}
					}
				}
			}
		}

		if (!EventsDropManager.getInstance().getEventRules().isEmpty())
		{
			final int rewardItem[] = EventsDropManager.getInstance().calculateRewardItem(npcTemplate, mainDamageDealer);
			if ((rewardItem[0] > 0) && (rewardItem[1] > 0))
			{
				final ItemHolder item = new ItemHolder(rewardItem[0], rewardItem[1]);
				if (((player.getUseAutoLoot() || Config.AUTO_LOOT || player.getFarmSystem().isAutofarming()) || isFlying()))
				{
					player.doAutoLoot(this, item.getId(), item.getCount());
				}
				else if (Config.AUTO_LOOT_BY_ID_SYSTEM)
				{
					if (Arrays.binarySearch(Config.AUTO_LOOT_BY_ID, item.getId()) >= 0)
					{
						player.doAutoLoot(this, item);
					}
					else
					{
						dropItem(player, item);
					}
				}
				else
				{
					dropItem(player, item);
				}
			}
		}
		ItemsAutoDestroy.getInstance().tryRecalcTime();
	}
	
	public void rollRewards(Map.Entry<RewardType, RewardList> entry, final Creature lastAttacker, Creature topDamager)
	{
		final RewardType type = entry.getKey();
		final RewardList list = entry.getValue();

		final Creature activeChar = (type == RewardType.SWEEP ? lastAttacker : topDamager);
		if (activeChar == null)
		{
			return;
		}
		
		final Player activePlayer = activeChar.getActingPlayer();
		if (activePlayer == null)
		{
			return;
		}
		
		if (type == RewardType.SWEEP && !isSpoil() && !activePlayer.isPassiveSpoil())
		{
			return;
		}

		final boolean isSiegeGuard = isSiegeGuard();
		final int diff = calculateLevelDiffForDrop(topDamager.getLevel());
		final double mod = 1.0;
		final double penaltyMod = isSiegeGuard && Config.EPAULETTE_WITHOUT_PENALTY ? 1 : ExperienceParser.getInstance().penaltyModifier(diff, 9);
		
		final List<RewardItem> rewardItems = list.roll(activePlayer, penaltyMod, mod, this);
		switch (type)
		{
			case SWEEP :
				_sweepItems = rewardItems;
				break;
			default :
				for (final RewardItem drop : rewardItems)
				{
					if (isSeeded() && getSeed() != null && !drop.isAdena())
					{
						continue;
					}

					if (isFlying() || (!drop.isHerb() && (((activePlayer.getUseAutoLoot() || activePlayer.getFarmSystem().isAutofarming()) && !isRaid()) || (!isRaid() && Config.AUTO_LOOT) || (isRaid() && Config.AUTO_LOOT_RAIDS))) || ((activePlayer.getUseAutoLootHerbs() || Config.AUTO_LOOT_HERBS) && drop.isHerb()))
					{
						activePlayer.doAutoLoot(this, drop._itemId, (int) drop._count);
					}
					else if (Config.AUTO_LOOT_BY_ID_SYSTEM)
					{
						if (Arrays.binarySearch(Config.AUTO_LOOT_BY_ID, drop._itemId) >= 0)
						{
							activePlayer.doAutoLoot(this, new ItemHolder(drop._itemId, (int) drop._count));
						}
						else
						{
							dropItem(activePlayer, drop._itemId, (int) drop._count);
						}
					}
					else
					{
						dropItem(activePlayer, drop._itemId, (int) drop._count);
					}

					if (isRaid() && !isRaidMinion())
					{
						final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_DIED_DROPPED_S3_S2);
						sm.addCharName(this);
						sm.addItemName(drop._itemId);
						sm.addItemNumber((int) drop._count);
						broadcastPacket(sm);
					}
				}
				break;
		}
	}

	public ItemInstance dropItem(Player mainDamageDealer, ItemHolder item)
	{
		if (Config.DISABLE_ITEM_DROP_LIST.contains(item.getId()))
		{
			return null;
		}
		
		final var template = ItemsParser.getInstance();
		ItemInstance ditem = null;
		for (int i = 0; i < item.getCount(); i++)
		{
			final var pos = Location.findAroundPosition(this, 100);
			if (template.getTemplate(item.getId()) != null)
			{
				ditem = template.createItem("Loot", item.getId(), item.getCount(), mainDamageDealer, this);
				ditem.getDropProtection().protect(mainDamageDealer, isRaid());
				ditem.dropMe(this, pos, false);

				if (!Config.LIST_PROTECTED_ITEMS.contains(item.getId()))
				{
					if (((Config.AUTODESTROY_ITEM_AFTER > 0) && (!ditem.getItem().isHerb())) || ((Config.HERB_AUTO_DESTROY_TIME > 0) && (ditem.getItem().isHerb())))
					{
						ItemsAutoDestroy.getInstance().addItem(ditem, 0);
					}
				}
				ditem.setProtected(false);

				if (ditem.isStackable() || !Config.MULTIPLE_ITEM_DROP)
				{
					break;
				}
			}
			else
			{
				_log.warn("Item doesn't exist so cannot be dropped. Item ID: " + item.getId());
			}
		}
		return ditem;
	}
	
	public ItemInstance dropSingleItem(Player mainDamageDealer, ItemHolder item)
	{
		if (Config.DISABLE_ITEM_DROP_LIST.contains(item.getId()))
		{
			return null;
		}
		
		final var manager = ItemsAutoDestroy.getInstance();
		final var template = ItemsParser.getInstance();
		ItemInstance ditem = null;
		for (int i = 0; i < item.getCount(); i++)
		{
			final var pos = Location.findAroundPosition(this, 100);
			if (template.getTemplate(item.getId()) != null)
			{
				ditem = template.createItem("Loot", item.getId(), item.getCount(), mainDamageDealer, this);
				ditem.getDropProtection().protect(mainDamageDealer, isRaid());
				ditem.dropMe(this, pos, false);
				
				if (!Config.LIST_PROTECTED_ITEMS.contains(item.getId()))
				{
					if (((Config.AUTODESTROY_ITEM_AFTER > 0) && (!ditem.getItem().isHerb())) || ((Config.HERB_AUTO_DESTROY_TIME > 0) && (ditem.getItem().isHerb())))
					{
						if (manager.addItem(ditem, 0))
						{
							manager.tryRecalcTime();
						}
					}
				}
				ditem.setProtected(false);
				
				if (ditem.isStackable() || !Config.MULTIPLE_ITEM_DROP)
				{
					break;
				}
			}
			else
			{
				_log.warn("Item doesn't exist so cannot be dropped. Item ID: " + item.getId());
			}
		}
		return ditem;
	}
	
	public ItemInstance dropSingleItem(Player lastAttacker, int itemId, long itemCount)
	{
		return dropSingleItem(lastAttacker, new ItemHolder(itemId, itemCount));
	}

	public ItemInstance dropItem(Player lastAttacker, int itemId, long itemCount)
	{
		return dropItem(lastAttacker, new ItemHolder(itemId, itemCount));
	}

	public ItemInstance getActiveWeapon()
	{
		return null;
	}

	public boolean noTarget()
	{
		return getAggroList().isEmpty();
	}

	public boolean containsTarget(Creature player)
	{
		return getAggroList().getCharMap().containsKey(player);
	}

	public void clearAggroList(boolean onlyHate)
	{
		getAggroList().clear(onlyHate);
		final var ai = getAI();
		if (ai != null)
		{
			getAI().stopAutoAttack();
		}
		_targetList.clear();
		_overhit = false;
		_overhitDamage = 0;
		_overhitAttacker = null;
	}

	@Override
	public boolean isSweepActive()
	{
		_sweepLock.lock();
		try
		{
			return _sweepItems != null && _sweepItems.size() > 0;
		}
		finally
		{
			_sweepLock.unlock();
		}
	}

	public List<Item> getSpoilLootItems()
	{
		final List<Item> lootItems = new ArrayList<>();
		if (_sweepItems != null)
		{
			for (final RewardItem item : _sweepItems)
			{
				lootItems.add(ItemsParser.getInstance().createDummyItem(item._itemId).getItem());
			}
		}
		return lootItems;
	}

	public List<RewardItem> takeSweep()
	{
		_sweepLock.lock();
		try
		{
			final List<RewardItem> sweep = _sweepItems;
			clearSweep();
			return sweep;
		}
		finally
		{
			_sweepLock.unlock();
		}
	}
	
	public void clearSweep()
	{
		_sweepLock.lock();
		try
		{
			_isSpoil = false;
			_isSpoiledBy = 0;
			_sweepItems = null;
		}
		finally
		{
			_sweepLock.unlock();
		}
	}

	public synchronized ItemHolder[] takeHarvest()
	{
		final ItemHolder[] harvest = _harvestItems;
		_harvestItems = null;
		return harvest;
	}

	public boolean isOldCorpse(Player attacker, int remainingTime, boolean sendMessage)
	{
		if (isDead() && (System.currentTimeMillis() - getDeathTime()) > remainingTime)
		{
			if (sendMessage && (attacker != null))
			{
				attacker.sendPacket(SystemMessageId.CORPSE_TOO_OLD_SKILL_NOT_USED);
			}
			return true;
		}
		return false;
	}

	public boolean checkSpoilOwner(Player sweeper, boolean sendMessage)
	{
		if ((sweeper.getObjectId() != getIsSpoiledBy()) && !sweeper.isInLooterParty(getIsSpoiledBy()))
		{
			if (sendMessage)
			{
				sweeper.sendPacket(SystemMessageId.SWEEP_NOT_ALLOWED);
			}
			return false;
		}
		return true;
	}

	public void overhitEnabled(boolean status)
	{
		_overhit = status;
	}

	public void setOverhitValues(Creature attacker, double damage)
	{
		final double overhitDmg = -(getCurrentHp() - damage);
		if (overhitDmg < 0)
		{
			overhitEnabled(false);
			_overhitDamage = 0;
			_overhitAttacker = null;
			return;
		}
		overhitEnabled(true);
		_overhitDamage = overhitDmg;
		_overhitAttacker = attacker;
	}

	public Creature getOverhitAttacker()
	{
		return _overhitAttacker;
	}

	public double getOverhitDamage()
	{
		return _overhitDamage;
	}

	public boolean isOverhit()
	{
		return _overhit;
	}

	private double[] calculateExpAndSp(int diff, int damage, long totalDamage, Creature attacker)
	{
		double xp;
		double sp;
		
		xp = ((double) getExpReward(attacker) * damage) / totalDamage;
		if ((Config.ALT_GAME_EXPONENT_XP != 0) && (Math.abs(diff) >= Config.ALT_GAME_EXPONENT_XP))
		{
			xp = 0;
		}
		
		sp = ((double) getSpReward(attacker) * damage) / totalDamage;
		if ((Config.ALT_GAME_EXPONENT_SP != 0) && (Math.abs(diff) >= Config.ALT_GAME_EXPONENT_XP))
		{
			sp = 0;
		}
		
		if ((Config.ALT_GAME_EXPONENT_XP == 0) && (Config.ALT_GAME_EXPONENT_SP == 0))
		{
			if (diff < -5)
			{
				diff = -5;
			}
			
			if (diff > 5)
			{
				final double pow = Math.pow((double) 5 / 6, diff - 5);
				xp = xp * pow;
				sp = sp * pow;
			}
		}
		
		xp = Math.max(0., xp);
		sp = Math.max(0., sp);
		
		return new double[]
		{
		        xp, sp
		};
	}

	public long calculateOverhitExp(long normalExp)
	{
		double overhitPercentage = ((getOverhitDamage() * 100) / getMaxHp());

		if (overhitPercentage > 25)
		{
			overhitPercentage = 25;
		}

		final double overhitExp = ((overhitPercentage / 100) * normalExp);

		final long bonusOverhit = Math.round(overhitExp);
		return bonusOverhit;
	}

	@Override
	public boolean isAttackable()
	{
		return true;
	}
	
	@Override
	public void onSpawn()
	{
		super.onSpawn();
		ThreadPoolManager.getInstance().schedule(new NotifyAITask(this, CtrlEvent.EVT_SPAWN), 100);
		setSpoil(false);
		clearAbsorbers();
		_harvestItems = null;
		_seeded = false;
		_seed = null;
		_seederObjId = 0;
		overhitEnabled(false);
		_isPassiveSweepActive = false;
		_sweepItems = null;

		setWalking();
		
		if (getChampionTemplate() != null)
		{
			for (final AbnormalEffect effect : getChampionTemplate().abnormalEffect)
			{
				if (effect != null)
				{
					startAbnormalEffect(effect);
				}
			}
		}

		if (!isInActiveRegion())
		{
			if (hasAI())
			{
				if (!isGlobalAI())
				{
					getAI().stopAITask();
				}
			}
		}
	}

	public boolean isSpoil()
	{
		return _isSpoil;
	}

	public void setSpoil(boolean isSpoil)
	{
		_isSpoil = isSpoil;
	}

	public final int getIsSpoiledBy()
	{
		return _isSpoiledBy;
	}

	public final void setIsSpoiledBy(int value)
	{
		_isSpoiledBy = value;
	}

	public void setSeeded(Player seeder)
	{
		if ((_seed != null) && (_seederObjId == seeder.getObjectId()))
		{
			_seeded = true;
			
			int count = 1;
			for (final int skillId : getTemplate().getSkills().keySet())
			{
				switch (skillId)
				{
					case 4303 :
						count *= 2;
						break;
					case 4304 :
						count *= 3;
						break;
					case 4305 :
						count *= 4;
						break;
					case 4306 :
						count *= 5;
						break;
					case 4307 :
						count *= 6;
						break;
					case 4308 :
						count *= 7;
						break;
					case 4309 :
						count *= 8;
						break;
					case 4310 :
						count *= 9;
						break;
				}
			}
			
			final int diff = getLevel() - _seed.getLevel() - 5;
			if (diff > 0)
			{
				count += diff;
			}
			
			_harvestItems = new ItemHolder[]
			{
			        new ItemHolder(_seed.getCropId(), (long) (count * Config.RATE_DROP_MANOR))
			};
		}
	}
	
	public final void setSeeded(Seed seed, Player seeder)
	{
		if (!_seeded)
		{
			_seed = seed;
			_seederObjId = seeder.getObjectId();
		}
	}

	public int getSeederId()
	{
		return _seederObjId;
	}

	public final Seed getSeed()
	{
		return _seed;
	}

	public boolean isSeeded()
	{
		return _seeded;
	}

	public final void setOnKillDelay(int delay)
	{
		_onKillDelay = delay;
	}

	public final int getOnKillDelay()
	{
		return _onKillDelay;
	}
	
	public final void setFindTargetDelay(int delay)
	{
		_findTargetDelay = (System.currentTimeMillis() + delay);
	}
	
	public final long getFindTargetDelay()
	{
		return _findTargetDelay;
	}

	@Override
	public boolean hasRandomAnimation()
	{
		return ((Config.MAX_MONSTER_ANIMATION > 0) && isRandomAnimationEnabled() && !(this instanceof GrandBossInstance));
	}

	@Override
	public boolean isMob()
	{
		return true;
	}

	public void returnHome()
	{
		clearAggroList(true);
		
		if (hasAI() && (getSpawn() != null))
		{
			getAI().setIntention(CtrlIntention.MOVING, getSpawn().getLocation(), 0);
		}
	}

	public double getVitalityPoints(int damage)
	{
		if (damage <= 0)
		{
			return 0;
		}

		final double divider = getTemplate().getBaseVitalityDivider();
		if (divider == 0)
		{
			return 0;
		}

		return -Math.min(damage, getMaxHp()) / divider;
	}

	public boolean useVitalityRate()
	{
		if (getChampionTemplate() != null && !getChampionTemplate().useVitalityRate)
		{
			return false;
		}
		return true;
	}

	@Override
	public boolean isRaid()
	{
		return _isRaid;
	}
	
	@Override
	public boolean isEpicRaid()
	{
		return _isEpicRaid;
	}
	
	public void setIsEpicRaid(boolean isEpicRaid)
	{
		_isEpicRaid = isEpicRaid;
	}

	public void setIsRaid(boolean isRaid)
	{
		_isRaid = isRaid;
	}
	
	@Override
	public boolean isSiegeGuard()
	{
		return _isSiegeGuard;
	}
	
	public void setIsSiegeGuard(boolean isSiegeGuard)
	{
		_isSiegeGuard = isSiegeGuard;
	}
	
	@Override
	public void setIsRaidMinion(boolean val)
	{
		_isRaid = val;
		_isRaidMinion = val;
	}
	
	@Override
	public boolean isRaidMinion()
	{
		return _isRaidMinion;
	}
	
	@Override
	public boolean isLethalImmune()
	{
		return _isLethalImmune;
	}
	
	public void setIsLethalImmune(boolean isLethalImmune)
	{
		_isLethalImmune = isLethalImmune;
	}
	
	@Override
	public boolean isGlobalAI()
	{
		return _isGlobalAI;
	}
	
	public void setIsGlobalAI(boolean isGlobalAI)
	{
		_isGlobalAI = isGlobalAI;
	}

	public boolean canShowLevelInTitle()
	{
		return !(getName("en").equals("Chest"));
	}

	public void addAbsorber(final Player attacker)
	{
		if (attacker == null)
		{
			return;
		}
		
		if (getCurrentHpPercents() > 50)
		{
			return;
		}
		
		if (_absorbersIds == null)
		{
			_absorbersIds = new HashSet<>();
		}
		_absorbersIds.add(attacker.getObjectId());
	}
	
	public boolean isAbsorbed(Player player)
	{
		if (_absorbersIds == null)
		{
			return false;
		}
		if (!_absorbersIds.contains(player.getObjectId()))
		{
			return false;
		}
		return true;
	}
	
	public void clearAbsorbers()
	{
		if (_absorbersIds != null)
		{
			_absorbersIds.clear();
		}
	}
	
	@Override
	public boolean canBeAttacked()
	{
		return true;
	}
	
	@Override
	public Location getMinionPosition()
	{
		return Location.findPointToStay(this, 100, 150, false);
	}
	
	@Override
	public void addInfoObject(GameObject object)
	{
		if (object.isPlayer() && getAI().getIntention() == CtrlIntention.IDLE)
		{
			getAI().setIntention(CtrlIntention.ACTIVE, null);
		}
	}
	
	@Override
	public void removeInfoObject(GameObject object)
	{
		super.removeInfoObject(object);
		
		if (object.isAttackable())
		{
			getAggroList().remove((Creature) object);
		}
		
		if (hasAI())
		{
			if (isInBlockList(object))
			{
				_blockList.remove(Integer.valueOf(object.getObjectId()));
			}
			
			if (getAggroList().isEmpty())
			{
				final var ai = getAI();
				if (ai != null)
				{
					ai.stopAutoAttack();
				}
				_targetList.clear();
			}
			getAI().notifyEvent(CtrlEvent.EVT_FORGET_OBJECT, object);
		}
	}
	
	public AggroList getAggroList()
	{
		return _aggroList;
	}
	
	@Override
	public boolean isPassiveSweepActive()
	{
		return _isPassiveSweepActive;
	}
	
	public void addToBlockList(final GameObject attacker)
	{
		if (attacker == null)
		{
			return;
		}
		
		if (_blockList == null)
		{
			_blockList = new HashSet<>();
		}
		_blockList.add(attacker.getObjectId());
	}
	
	public boolean isInBlockList(GameObject attacker)
	{
		if (_blockList == null)
		{
			return false;
		}
		if (!_blockList.contains(attacker.getObjectId()))
		{
			return false;
		}
		return true;
	}
	
	public void clearBlockList()
	{
		if (_blockList != null)
		{
			_blockList.clear();
		}
	}
	
	public void addToTargetList(Player player)
	{
		if (!_targetList.contains(player.getObjectId()))
		{
			final var party = player.getParty();
			if (party != null)
			{
				party.getMembers().stream().filter(p -> p != null).forEach(m -> _targetList.add(m.getObjectId()));
			}
			else
			{
				_targetList.add(player.getObjectId());
			}
		}
	}
	
	public boolean isInTargetList(Player player)
	{
		return getCurrentHpPercents() > 98 ? true : _targetList.isEmpty() || _targetList.contains(player.getObjectId());
	}
}