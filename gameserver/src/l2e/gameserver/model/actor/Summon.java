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

import java.util.Set;

import l2e.commons.apache.ArrayUtils;
import l2e.gameserver.Config;
import l2e.gameserver.ai.character.CharacterAI;
import l2e.gameserver.ai.character.SummonAI;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.data.parser.CategoryParser;
import l2e.gameserver.data.parser.DamageLimitParser;
import l2e.gameserver.data.parser.ExperienceParser;
import l2e.gameserver.geodata.GeoEngine;
import l2e.gameserver.handler.itemhandlers.IItemHandler;
import l2e.gameserver.handler.itemhandlers.ItemHandler;
import l2e.gameserver.instancemanager.TerritoryWarManager;
import l2e.gameserver.instancemanager.ZoneManager;
import l2e.gameserver.model.CategoryType;
import l2e.gameserver.model.Clan;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.Party;
import l2e.gameserver.model.ShotType;
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.instance.ControlTowerInstance;
import l2e.gameserver.model.actor.instance.NpcInstance;
import l2e.gameserver.model.actor.instance.SiegeSummonInstance;
import l2e.gameserver.model.actor.instance.player.PremiumBonus;
import l2e.gameserver.model.actor.stat.SummonStat;
import l2e.gameserver.model.actor.status.SummonStatus;
import l2e.gameserver.model.actor.templates.items.Weapon;
import l2e.gameserver.model.actor.templates.npc.DamageLimit;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.actor.templates.npc.aggro.AggroInfo;
import l2e.gameserver.model.entity.events.AbstractFightEvent;
import l2e.gameserver.model.entity.events.tournaments.Tournament;
import l2e.gameserver.model.entity.events.tournaments.util.TournamentUtil;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.items.itemcontainer.PetInventory;
import l2e.gameserver.model.items.type.ActionType;
import l2e.gameserver.model.olympiad.OlympiadGameManager;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.zone.ZoneId;
import l2e.gameserver.model.zone.type.SiegeZone;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.ExPartyPetWindowAdd;
import l2e.gameserver.network.serverpackets.ExPartyPetWindowDelete;
import l2e.gameserver.network.serverpackets.ExPartyPetWindowUpdate;
import l2e.gameserver.network.serverpackets.GameServerPacket;
import l2e.gameserver.network.serverpackets.MoveToLocation;
import l2e.gameserver.network.serverpackets.NpcInfo.SummonInfo;
import l2e.gameserver.network.serverpackets.PetDelete;
import l2e.gameserver.network.serverpackets.PetInfo;
import l2e.gameserver.network.serverpackets.PetItemList;
import l2e.gameserver.network.serverpackets.PetStatusUpdate;
import l2e.gameserver.network.serverpackets.RelationChanged;
import l2e.gameserver.network.serverpackets.StatusUpdate;
import l2e.gameserver.network.serverpackets.SystemMessage;
import l2e.gameserver.network.serverpackets.TeleportToLocation;
import l2e.gameserver.taskmanager.DecayTaskManager;

public abstract class Summon extends Playable
{
	private Player _owner;
	private int _attackRange = 36;
	private boolean _follow = true;
	private boolean _previousFollowStatus = true;
	protected boolean _restoreSummon = true;
	
	private int _shotsMask = 0;
	private boolean _cancelAction = false;
	protected long _timeInterval = 0;
	private Skill _summonSkill = null;
	private boolean _isSiegeSummon = false;
	private double _physAttributteMod;
	private double _magicAttributteMod;
	
	private static final int[] PASSIVE_SUMMONS =
	{
	        12564, 12621, 14702, 14703, 14704, 14705, 14706, 14707, 14708, 14709, 14710, 14711, 14712, 14713, 14714, 14715, 14716, 14717, 14718, 14719, 14720, 14721, 14722, 14723, 14724, 14725, 14726, 14727, 14728, 14729, 14730, 14731, 14732, 14733, 14734, 14735, 14736
	};
	
	public Summon(int objectId, NpcTemplate template, Player owner)
	{
		super(objectId, template);
		setInstanceType(InstanceType.Summon);
		
		setReflection(owner.getReflection());
		
		_showSummonAnimation = true;
		_owner = owner;
		getAI();
		final var loc = Location.findPointToStayPet(_owner, 50, 70);
		setXYZInvisible(loc.getX(), loc.getY(), loc.getZ());
		_isSiegeSummon = (getId() == SiegeSummonInstance.SWOOP_CANNON_ID || getId() == SiegeSummonInstance.SIEGE_GOLEM_ID);
		GameObjectsStorage.put(this);
	}
	
	@Override
	public void onSpawn()
	{
		super.onSpawn();
		
		if (Config.SUMMON_STORE_SKILL_COOLTIME && !isTeleporting())
		{
			restoreEffects();
		}
			
		setFollowStatus(true);
		updateAndBroadcastStatus(0);
		for (final Player player : World.getInstance().getAroundPlayers(this))
		{
			player.sendPacket(RelationChanged.update(player, this, player));
		}
		final Party party = getOwner().getParty();
		if (party != null)
		{
			party.broadcastToPartyMembers(getOwner(), new ExPartyPetWindowAdd(this));
		}
		setShowSummonAnimation(false);
		_restoreSummon = false;
		getStatus().startHpMpRegeneration();
		_owner.getListeners().onSummonServitor(this);
	}
	
	@Override
	public void store()
	{
		storeEffect(true);
	}
	
	@Override
	public SummonStat getStat()
	{
		return (SummonStat) super.getStat();
	}
	
	@Override
	public void initCharStat()
	{
		setStat(new SummonStat(this));
	}
	
	@Override
	public SummonStatus getStatus()
	{
		return (SummonStatus) super.getStatus();
	}
	
	@Override
	public void initCharStatus()
	{
		setStatus(new SummonStatus(this));
	}
	
	@Override
	protected CharacterAI initAI()
	{
		return new SummonAI(this);
	}
	
	@Override
	public NpcTemplate getTemplate()
	{
		return (NpcTemplate) super.getTemplate();
	}
	
	public abstract int getSummonType();
	
	@Override
	public final void stopAllEffects()
	{
		super.stopAllEffects();
		updateAndBroadcastStatus(1);
	}
	
	@Override
	public final void stopAllEffectsExceptThoseThatLastThroughDeath()
	{
		super.stopAllEffectsExceptThoseThatLastThroughDeath();
		updateAndBroadcastStatus(1);
	}
	
	@Override
	public void updateAbnormalEffect()
	{
		for (final Player player : World.getInstance().getAroundPlayers(this))
		{
			player.sendPacket(new SummonInfo(this, player, 1));
		}
	}
	
	public boolean isMountable()
	{
		return false;
	}
	
	public long getExpForThisLevel()
	{
		if (getLevel() >= ExperienceParser.getInstance().getMaxPetLevel())
		{
			return 0;
		}
		return ExperienceParser.getInstance().getExpForLevel(getLevel());
	}
	
	public long getExpForNextLevel()
	{
		if (getLevel() >= (ExperienceParser.getInstance().getMaxPetLevel() - 1))
		{
			return 0;
		}
		return ExperienceParser.getInstance().getExpForLevel(getLevel() + 1);
	}
	
	@Override
	public final int getKarma()
	{
		return getOwner() != null ? getOwner().getKarma() : 0;
	}
	
	@Override
	public final byte getPvpFlag()
	{
		return getOwner() != null ? getOwner().getPvpFlag() : 0;
	}
	
	@Override
	public final int getTeam()
	{
		return getOwner() != null ? getOwner().getTeam() : 0;
	}
	
	public final Player getOwner()
	{
		return _owner;
	}
	
	@Override
	public final int getId()
	{
		return getTemplate().getId();
	}
	
	public int getSoulShotsPerHit()
	{
		return (getLevel() / 27) + 1;
	}
	
	public int getSpiritShotsPerHit()
	{
		return (getLevel() / 58) + 1;
	}
	
	public void followOwner()
	{
		setFollowStatus(true);
	}
	
	@Override
	protected void onDeath(Creature killer)
	{
		final Player owner = getOwner();
		if (owner != null)
		{
			for (final Npc npc : World.getInstance().getAroundNpc(this))
			{
				if (npc instanceof Attackable)
				{
					if (((Attackable) npc).isDead())
					{
						continue;
					}
					
					final AggroInfo info = ((Attackable) npc).getAggroList().get(this);
					if (info != null)
					{
						((Attackable) npc).addDamageHate(owner, info.getDamage(), info.getHate());
					}
				}
			}
		}

		boolean decayed = isServitor();
		if (decayed)
		{
			DecayTaskManager.getInstance().addDecayTask(this, (Config.NPC_DECAY_TIME * 1000L), true);
		}
		super.onDeath(killer);
		storeEffect(true);
	}
	
	public void doDie(Creature killer, boolean decayed)
	{
		if (!decayed)
		{
			DecayTaskManager.getInstance().addDecayTask(this, (Config.NPC_DECAY_TIME * 1000L), true);
		}
		super.onDeath(killer);
	}
	
	public void stopDecay()
	{
		DecayTaskManager.getInstance().cancelDecayTask(this);
	}
	
	@Override
	public void onDecay()
	{
		deleteMe(_owner);
	}
	
	@Override
	public void endDecayTask()
	{
		DecayTaskManager.getInstance().cancelDecayTask(this);
		onDecay();
	}
	
	@Override
	public void broadcastStatusUpdate()
	{
		super.broadcastStatusUpdate();
		updateAndBroadcastStatus(1);
	}
	
	@Override
	public StatusUpdate makeStatusUpdate(int... fields)
	{
		final var su = new StatusUpdate(this);
		for (final int field : fields)
		{
			switch (field)
			{
				case StatusUpdate.CUR_HP :
					su.addAttribute(field, (int) getCurrentHp());
					break;
				case StatusUpdate.MAX_HP :
					su.addAttribute(field, getMaxHp());
					break;
				case StatusUpdate.CUR_MP :
					su.addAttribute(field, (int) getCurrentMp());
					break;
				case StatusUpdate.MAX_MP :
					su.addAttribute(field, getMaxMp());
					break;
				case StatusUpdate.CUR_LOAD :
					su.addAttribute(field, getCurrentLoad());
					break;
				case StatusUpdate.MAX_LOAD :
					su.addAttribute(field, getMaxLoad());
					break;
				case StatusUpdate.LEVEL :
					su.addAttribute(field, getLevel());
					break;
				case StatusUpdate.PVP_FLAG :
					su.addAttribute(field, getPvpFlag());
					break;
				case StatusUpdate.KARMA :
					su.addAttribute(field, -getKarma());
					break;
			}
		}
		return su;
	}
	
	public void deleteMe(Player owner)
	{
		super.deleteMe();
		
		if (owner != null)
		{
			owner.sendPacket(new PetDelete(getSummonType(), getObjectId()));
			final Party party = owner.getParty();
			if (party != null)
			{
				party.broadcastToPartyMembers(owner, new ExPartyPetWindowDelete(this));
			}
		}
		
		if (getInventory() != null)
		{
			getInventory().destroyAllItems("pet deleted", getOwner(), this);
		}
		decayMe();
		owner.setPet(null);
		
		if (_summonSkill != null && isServitor() && getOwner() != null && getOwner().getFarmSystem().isAutofarming() && getOwner().getFarmSystem().getFarmType() == 4)
		{
			getOwner().useMagic(_summonSkill, false, false, false);
		}
	}
	
	public void setSummonSkill(Skill sk)
	{
		_summonSkill = sk;
	}
	
	public void unSummon(Player owner)
	{
		if (isVisible() && !isDead())
		{
			getAI().stopFollow();
			abortCast();
			store();
			
			if (hasAI())
			{
				getAI().stopAITask();
			}
			
			stopAllEffects();
			stopHpMpRegeneration();
			
			if (owner != null)
			{
				owner.sendPacket(new PetDelete(getSummonType(), getObjectId()));
				final Party party = owner.getParty();
				if (party != null)
				{
					party.broadcastToPartyMembers(owner, new ExPartyPetWindowDelete(this));
				}
				
				if ((getInventory() != null) && (getInventory().getSize() > 0))
				{
					owner.setPetInvItems(true);
					sendPacket(SystemMessageId.ITEMS_IN_PET_INVENTORY);
				}
				else
				{
					owner.setPetInvItems(false);
				}
				owner.setPet(null);
			}
			decayMe();
			setTarget(null);
			GameObjectsStorage.remove(this);
		}
	}
	
	public int getAttackRange()
	{
		return _attackRange;
	}
	
	public void setAttackRange(int range)
	{
		_attackRange = (range < 36) ? 36 : range;
	}
	
	public void setFollowStatus(boolean state)
	{
		_follow = state;
		if (_follow)
		{
			getAI().setIntention(CtrlIntention.FOLLOW, getOwner());
		}
		else
		{
			getAI().setIntention(CtrlIntention.IDLE, null);
		}
	}
	
	public boolean isInFollowStatus()
	{
		return _follow;
	}
	
	@Override
	public boolean isAutoAttackable(Creature attacker, boolean isPoleAttack)
	{
		return _owner.isAutoAttackable(attacker, isPoleAttack);
	}
	
	public int getControlObjectId()
	{
		return 0;
	}
	
	public Weapon getActiveWeapon()
	{
		return null;
	}
	
	@Override
	public PetInventory getInventory()
	{
		return null;
	}
	
	public void doPickupItem(GameObject object)
	{
	}
	
	public void setRestoreSummon(boolean val)
	{
	}
	
	@Override
	public ItemInstance getActiveWeaponInstance()
	{
		return null;
	}
	
	@Override
	public Weapon getActiveWeaponItem()
	{
		return null;
	}
	
	@Override
	public ItemInstance getSecondaryWeaponInstance()
	{
		return null;
	}
	
	@Override
	public Weapon getSecondaryWeaponItem()
	{
		return null;
	}
	
	@Override
	public boolean isInvul()
	{
		return super.isInvul() || getOwner().isSpawnProtected();
	}
	
	@Override
	public Party getParty()
	{
		if (_owner == null)
		{
			return null;
		}
		
		return _owner.getParty();
	}
	
	@Override
	public boolean isInParty()
	{
		return (_owner != null) && _owner.isInParty();
	}
	
	@Override
	public boolean useMagic(Skill skill, boolean forceUse, boolean dontMove, boolean msg)
	{
		final var owner = getOwner();
		if ((skill == null) || isDead() || (owner == null))
		{
			return false;
		}
		
		if (skill.isPassive())
		{
			return false;
		}
		
		if (isCastingNow())
		{
			return false;
		}
		owner.setCurrentPetSkill(skill, forceUse, dontMove);
		
		GameObject target = null;
		
		switch (skill.getTargetType())
		{
			case OWNER_PET :
				target = owner;
				break;
			case PARTY :
			case AURA :
			case FRONT_AURA :
			case BEHIND_AURA :
			case SELF :
			case AURA_CORPSE_MOB :
			case AURA_DOOR :
			case AURA_FRIENDLY :
			case AURA_FRIENDLY_SUMMON :
			case AURA_UNDEAD_ENEMY :
			case COMMAND_CHANNEL :
			case AURA_MOB :
			case AURA_DEAD_MOB :
				target = this;
				break;
			default :
				target = skill.getFirstOfTargetList(this);
				break;
		}
		
		if (target == null)
		{
			sendPacket(SystemMessageId.TARGET_CANT_FOUND);
			return false;
		}
		
		if (!GeoEngine.getInstance().canSeeTarget(this, target))
		{
			sendPacket(SystemMessageId.CANT_SEE_TARGET);
			sendActionFailed();
			return false;
		}
		
		if (isSkillDisabled(skill))
		{
			sendPacket(SystemMessageId.PET_SKILL_CANNOT_BE_USED_RECHARCHING);
			return false;
		}
		
		if (!skill.checkCondition(this, target, false, true))
		{
			sendActionFailed();
			return false;
		}
		
		final var mpConsume = getStat().getMpConsume(skill)[0];
		if (getCurrentMp() < (mpConsume + getStat().getMpInitialConsume(skill)))
		{
			sendPacket(SystemMessageId.NOT_ENOUGH_MP);
			return false;
		}
		
		if (getCurrentHp() <= skill.getHpConsume())
		{
			sendPacket(SystemMessageId.NOT_ENOUGH_HP);
			return false;
		}
		
		if (skill.isOffensive())
		{
			if (owner == target)
			{
				return false;
			}
			
			if (isInsidePeaceZone(this, target) && !owner.getAccessLevel().allowPeaceAttack())
			{
				sendPacket(SystemMessageId.TARGET_IN_PEACEZONE);
				return false;
			}
			
			if (owner.isInOlympiadMode() && !owner.isOlympiadStart())
			{
				sendActionFailed();
				return false;
			}
			
			if (!owner.isInFightEvent())
			{
				final var siegeZone = ZoneManager.getInstance().getZone(owner, SiegeZone.class);
				if ((target.getActingPlayer() != null) && (owner.getSiegeState() > 0) && siegeZone != null && (target.getActingPlayer().getSiegeState() == owner.getSiegeState()) && (target.getActingPlayer() != owner) && (target.getActingPlayer().getSiegeSide() == owner.getSiegeSide()))
				{
					if (siegeZone.isAttackSameSiegeSide())
					{
						final Clan clan1 = target.getActingPlayer().getClan();
						final Clan clan2 = owner.getClan();
						if (clan1 != null && clan2 != null)
						{
							if ((clan1.getAllyId() != 0 && clan2.getAllyId() != 0 && clan1.getAllyId() == clan2.getAllyId()) || clan1.getId() == clan2.getId())
							{
								if (TerritoryWarManager.getInstance().isTWInProgress())
								{
									sendPacket(SystemMessageId.YOU_CANNOT_ATTACK_A_MEMBER_OF_THE_SAME_TERRITORY);
								}
								else
								{
									sendPacket(SystemMessageId.FORCED_ATTACK_IS_IMPOSSIBLE_AGAINST_SIEGE_SIDE_TEMPORARY_ALLIED_MEMBERS);
								}
								sendActionFailed();
								return false;
							}
						}
					}
					else
					{
						if (TerritoryWarManager.getInstance().isTWInProgress())
						{
							sendPacket(SystemMessageId.YOU_CANNOT_ATTACK_A_MEMBER_OF_THE_SAME_TERRITORY);
						}
						else
						{
							sendPacket(SystemMessageId.FORCED_ATTACK_IS_IMPOSSIBLE_AGAINST_SIEGE_SIDE_TEMPORARY_ALLIED_MEMBERS);
						}
						sendActionFailed();
						return false;
					}
				}
			}
			
			if (target.isDoor() || target instanceof ControlTowerInstance)
			{
				if (!target.isAutoAttackable(owner, false))
				{
					return false;
				}
			}
			else
			{
				if (!target.canBeAttacked() && !owner.getAccessLevel().allowPeaceAttack())
				{
					return false;
				}
				
				if (!target.isAutoAttackable(this, false) && !forceUse && !target.isNpc())
				{
					switch (skill.getTargetType())
					{
						case AURA :
						case FRONT_AURA :
						case BEHIND_AURA :
						case AURA_CORPSE_MOB :
						case AURA_DOOR :
						case AURA_FRIENDLY :
						case CLAN :
						case PARTY :
						case SELF :
							break;
						default :
							sendActionFailed();
							return false;
					}
				}
			}
		}
		
		switch (skill.getTargetType())
		{
			case PARTY :
			case CLAN :
			case PARTY_CLAN :
			case PARTY_NOTME :
			case CORPSE_CLAN :
			case CORPSE_FRIENDLY :
			case AURA :
			case FRONT_AURA :
			case BEHIND_AURA :
			case AREA_SUMMON :
			case AURA_UNDEAD_ENEMY :
			case GROUND :
			case SELF :
				break;
			default :
				if (!owner.checkPvpSkill(target, skill, true))
				{
					sendActionFailed();
					return false;
				}
		}
		getAI().setIntention(CtrlIntention.CAST, skill, target);
		return true;
	}
	
	@Override
	public void setIsImmobilized(boolean value)
	{
		super.setIsImmobilized(value);
		
		_previousFollowStatus = isInFollowStatus();
		if (value)
		{
			if (_previousFollowStatus && !isInCombat())
			{
				setFollowStatus(false);
			}
		}
		else
		{
			if (!isInCombat() && !_previousFollowStatus)
			{
				setFollowStatus(true);
			}
		}
	}
	
	public void setOwner(Player newOwner)
	{
		_owner = newOwner;
	}
	
	@Override
	public void sendDamageMessage(Creature target, int damage, Skill skill, boolean mcrit, boolean pcrit, boolean miss)
	{
		if (miss || (getOwner() == null))
		{
			return;
		}
		
		if (Config.ALLOW_DAMAGE_LIMIT && target.isNpc())
		{
			final DamageLimit limit = DamageLimitParser.getInstance().getDamageLimit(target.getId());
			if (limit != null)
			{
				final int damageLimit = skill != null ? skill.isMagic() ? limit.getMagicDamage() : limit.getPhysicDamage() : limit.getDamage();
				if (damageLimit > 0 && damage > damageLimit)
				{
					damage = damageLimit;
				}
			}
		}
		
		if (target.getObjectId() != getOwner().getObjectId())
		{
			if (pcrit || mcrit)
			{
				if (isServitor())
				{
					sendPacket(SystemMessageId.CRITICAL_HIT_BY_SUMMONED_MOB);
				}
				else
				{
					sendPacket(SystemMessageId.CRITICAL_HIT_BY_PET);
				}
			}
			if (getOwner().isInOlympiadMode() && (target instanceof Player) && ((Player) target).isInOlympiadMode() && (((Player) target).getOlympiadGameId() == getOwner().getOlympiadGameId()))
			{
				OlympiadGameManager.getInstance().notifyCompetitorDamage(target.getActingPlayer(), damage);
			}
			
			final SystemMessage sm;
			
			if (target.isInvul() && !(target instanceof NpcInstance))
			{
				sm = SystemMessage.getSystemMessage(SystemMessageId.ATTACK_WAS_BLOCKED);
			}
			else
			{
				sm = SystemMessage.getSystemMessage(SystemMessageId.C1_DONE_S3_DAMAGE_TO_C2);
				sm.addNpcName(this);
				sm.addCharName(target);
				sm.addNumber(damage);
			}
			sendPacket(sm);
		}
	}
	
	@Override
	public void reduceCurrentHp(double damage, Creature attacker, Skill skill)
	{
		super.reduceCurrentHp(damage, attacker, skill);
		if ((getOwner() != null) && (attacker != null))
		{
			final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_RECEIVED_DAMAGE_OF_S3_FROM_C2);
			sm.addNpcName(this);
			sm.addCharName(attacker);
			sm.addNumber((int) damage);
			sendPacket(sm);
		}
	}
	
	@Override
	public void doCast(Skill skill)
	{
		final Player actingPlayer = getActingPlayer();
		
		if (!actingPlayer.checkPvpSkill(getTarget(), skill, true) && !actingPlayer.getAccessLevel().allowPeaceAttack())
		{
			actingPlayer.sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
			actingPlayer.sendActionFailed();
			return;
		}
		super.doCast(skill);
	}
	
	@Override
	public final void abortCast()
	{
		super.abortCast();
		final var target = getTarget();
		if (target != null && target == getOwner())
		{
			setFollowStatus(true);
		}
	}
	
	@Override
	public boolean isInCombat()
	{
		return (getOwner() != null) && getOwner().isInCombat();
	}
	
	@Override
	public Player getActingPlayer()
	{
		return getOwner();
	}
	
	public void updateAndBroadcastStatus(int val)
	{
		if (getOwner() == null)
		{
			return;
		}
		
		sendPacket(new PetInfo(this, val));
		sendPacket(new PetStatusUpdate(this));
		if (isVisible())
		{
			broadcastNpcInfo(val);
		}
		final Party party = getOwner().getParty();
		if (party != null)
		{
			party.broadcastToPartyMembers(getOwner(), new ExPartyPetWindowUpdate(this));
		}
		updateEffectIcons(true);
	}
	
	public void broadcastNpcInfo(int val)
	{
		for (final Player player : World.getInstance().getAroundPlayers(this))
		{
			if (player == getOwner())
			{
				continue;
			}
			player.sendPacket(new SummonInfo(this, player, val));
		}
	}
	
	public boolean isHungry()
	{
		return false;
	}
	
	public int getWeapon()
	{
		return 0;
	}
	
	public int getArmor()
	{
		return 0;
	}
	
	@Override
	public void sendInfo(Player activeChar)
	{
		if (activeChar == getOwner())
		{
			activeChar.sendPacket(new PetInfo(this, 0));
			updateEffectIcons(true);
			if (isPet())
			{
				activeChar.sendPacket(new PetItemList(getInventory().getItems()));
			}
		}
		else
		{
			activeChar.sendPacket(new SummonInfo(this, activeChar, 0));
		}
		
		if (isMoving())
		{
			activeChar.sendPacket(new MoveToLocation(this));
		}
	}
	
	@Override
	public void onTeleported()
	{
		super.onTeleported();
		sendPacket(new TeleportToLocation(this, getX(), getY(), getZ(), getHeading()));
	}
	
	@Override
	public String toString()
	{
		return super.toString() + "(" + getId() + ") Owner: " + getOwner();
	}
	
	@Override
	public boolean isUndead()
	{
		return getTemplate().isUndead();
	}
	
	public void switchMode()
	{
	}
	
	public void cancelAction()
	{
		if (!isMovementDisabled())
		{
			setCancelAction(true);
			getAI().setIntention(CtrlIntention.ACTIVE);
		}
	}
	
	public void doAttack()
	{
		if (getOwner() != null)
		{
			if (isCancelAction())
			{
				setCancelAction(false);
			}
			
			final GameObject target = getOwner().getTarget();
			if (target != null)
			{
				setTarget(target);
				getAI().setIntention(CtrlIntention.ATTACK, target);
			}
		}
	}
	
	public final boolean canAttack(GameObject target, boolean ctrlPressed)
	{
		if (getOwner() == null || (target == null) || (this == target))
		{
			return false;
		}
		
		if ((getOwner() == target) && !ctrlPressed)
		{
			getAI().setIntention(CtrlIntention.MOVING, getOwner().getLocation(), 0);
			followOwner();
			return false;
		}
		
		if ((getOwner() == target) && ctrlPressed && !Config.ALLOW_SUMMON_OWNER_ATTACK)
		{
			getAI().setIntention(CtrlIntention.MOVING, getOwner().getLocation(), 0);
			followOwner();
			return false;
		}
		
		final int npcId = getId();
		if (ArrayUtils.contains(PASSIVE_SUMMONS, npcId))
		{
			getOwner().sendActionFailed();
			return false;
		}
		
		if (isBetrayed())
		{
			sendPacket(SystemMessageId.PET_REFUSING_ORDER);
			sendActionFailed();
			return false;
		}
		
		if (isAttackingDisabled())
		{
			if (!isAttackingNow())
			{
				return false;
			}
			getAI().setIntention(CtrlIntention.ATTACK, target);
		}
		
		if (isPet() && ((getLevel() - getOwner().getLevel()) > 20))
		{
			sendPacket(SystemMessageId.PET_TOO_HIGH_TO_CONTROL);
			sendActionFailed();
			return false;
		}
		
		if (getOwner().isInOlympiadMode() && !getOwner().isOlympiadStart())
		{
			setFollowStatus(false);
			getAI().setIntention(CtrlIntention.FOLLOW, target);
			getOwner().sendActionFailed();
			return false;
		}
		
		if ((target.getActingPlayer() != null) && (getOwner().getSiegeState() > 0) && getOwner().isInsideZone(ZoneId.SIEGE) && (target.getActingPlayer().getSiegeSide() == getOwner().getSiegeSide()))
		{
			if (TerritoryWarManager.getInstance().isTWInProgress())
			{
				sendPacket(SystemMessageId.YOU_CANNOT_ATTACK_A_MEMBER_OF_THE_SAME_TERRITORY);
			}
			else
			{
				sendPacket(SystemMessageId.FORCED_ATTACK_IS_IMPOSSIBLE_AGAINST_SIEGE_SIDE_TEMPORARY_ALLIED_MEMBERS);
			}
			sendActionFailed();
			return false;
		}
		
		if (!getOwner().getAccessLevel().allowPeaceAttack() && getOwner().isInsidePeaceZone(this, target))
		{
			sendPacket(SystemMessageId.TARGET_IN_PEACEZONE);
			return false;
		}
		
		if (isLockedTarget())
		{
			sendPacket(SystemMessageId.FAILED_CHANGE_TARGET);
			return false;
		}
		
		if (!target.isAutoAttackable(getOwner(), false) && !ctrlPressed && !target.isNpc())
		{
			setFollowStatus(false);
			getAI().setIntention(CtrlIntention.FOLLOW, target);
			sendPacket(SystemMessageId.INCORRECT_TARGET);
			return false;
		}
		
		if (target.isDoor() || target instanceof ControlTowerInstance)
		{
			if (target.isAutoAttackable(getOwner(), false) || _isSiegeSummon)
			{
				return true;
			}
			else
			{
				return false;
			}
		}
		else
		{
			if (_isSiegeSummon)
			{
				return false;
			}
		}
		return true;
	}
	
	@Override
	public void sendPacket(GameServerPacket mov)
	{
		if (getOwner() != null)
		{
			getOwner().sendPacket(mov);
		}
	}
	
	@Override
	public void sendPacket(SystemMessageId id)
	{
		if (getOwner() != null)
		{
			getOwner().sendPacket(id);
		}
	}
	
	@Override
	public boolean isSummon()
	{
		return true;
	}
	
	@Override
	public Summon getSummon()
	{
		return this;
	}
	
	@Override
	public boolean isChargedShot(ShotType type)
	{
		return (_shotsMask & type.getMask()) == type.getMask();
	}
	
	@Override
	public void setChargedShot(ShotType type, boolean charged)
	{
		if (charged)
		{
			_shotsMask |= type.getMask();
		}
		else
		{
			_shotsMask &= ~type.getMask();
		}
		
		if (!charged)
		{
			rechargeShots(true, true);
		}
	}
	
	@Override
	public void rechargeShots(boolean physical, boolean magic)
	{
		ItemInstance item;
		IItemHandler handler;
		
		if ((getOwner().getAutoSoulShot() == null) || getOwner().getAutoSoulShot().isEmpty())
		{
			return;
		}
		
		for (final int itemId : getOwner().getAutoSoulShot())
		{
			item = getOwner().getInventory().getItemByItemId(itemId);
			
			if (item != null)
			{
				if (magic)
				{
					if (item.getItem().getDefaultAction() == ActionType.summon_spiritshot)
					{
						handler = ItemHandler.getInstance().getHandler(item.getEtcItem());
						if (handler != null)
						{
							handler.useItem(getOwner(), item, false);
						}
					}
				}
				
				if (physical)
				{
					if (item.getItem().getDefaultAction() == ActionType.summon_soulshot)
					{
						handler = ItemHandler.getInstance().getHandler(item.getEtcItem());
						if (handler != null)
						{
							handler.useItem(getOwner(), item, false);
						}
					}
				}
			}
			else
			{
				getOwner().removeAutoSoulShot(itemId);
			}
		}
	}
	
	@Override
	public int getClanId()
	{
		return (getOwner() != null) ? getOwner().getClanId() : 0;
	}
	
	@Override
	public int getAllyId()
	{
		return (getOwner() != null) ? getOwner().getAllyId() : 0;
	}
	
	@Override
	public boolean isInCategory(CategoryType type)
	{
		return CategoryParser.getInstance().isInCategory(type, getId());
	}
	
	@Override
	public <E extends AbstractFightEvent> E getEvent(Class<E> eventClass)
	{
		return getOwner() != null ? getOwner().getEvent(eventClass) : super.getEvent(eventClass);
	}

	@Override
	public Set<AbstractFightEvent> getFightEvents()
	{
		return getOwner() != null ? getOwner().getFightEvents() : super.getFightEvents();
	}

	@Override
	public Tournament getPartyTournament() {
		return getOwner() != null ? getOwner().getPartyTournament() : super.getPartyTournament();
	}

	@Override
	public boolean isCancelAction()
	{
		return _cancelAction;
	}
	
	public void setCancelAction(boolean val)
	{
		_cancelAction = val;
	}
	
	@Override
	public boolean hasPremiumBonus()
	{
		return _owner == null ? false : _owner.hasPremiumBonus();
	}
	
	@Override
	public PremiumBonus getPremiumBonus()
	{
		return _owner.getPremiumBonus();
	}
	
	public String getSummonName(String lang)
	{
		return (getName(lang) == null || getName(lang).isEmpty()) ? getTemplate().getName(lang) : getName(lang);
	}
	
	@Override
	public boolean isInFightEvent()
	{
		return _owner != null && _owner.isInFightEvent();
	}

	@Override
	public boolean isInPartyTournament() {
		return _owner != null && _owner.isInPartyTournament();
	}

	public void teleToLeader(boolean checFlood)
	{
		if (_owner == null || _timeInterval > System.currentTimeMillis())
		{
			return;
		}
		
		if (checFlood)
		{
			_timeInterval = System.currentTimeMillis() + 5000L;
		}
		
		final var loc = Location.findPointToStay(_owner, 60, 100, false);
		if (loc != null)
		{
			setFollowStatus(false);
			setShowSummonAnimation(true);
			teleToLocation(loc, false, _owner.getReflection());
			((SummonAI) getAI()).setStartFollowController(true);
			setFollowStatus(true);
			updateAndBroadcastStatus(0);
			setShowSummonAnimation(false);
		}
		else
		{
			_log.warn("Problem to found summon position for owner[" + _owner.getName(null) + "] Current loc: " + getX() + " " + getY() + " " + getZ());
		}
	}
	
	public void setPhysAttributteMod(double val)
	{
		_physAttributteMod = val;
	}
	
	@Override
	public double getPhysAttributteMod()
	{
		return _physAttributteMod;
	}
	
	public void setMagicAttributteMod(double val)
	{
		_magicAttributteMod = val;
	}
	
	@Override
	public double getMagicAttributteMod()
	{
		return _magicAttributteMod;
	}
	
	@Override
	public byte getAttackElement()
	{
		return getOwner() != null ? getOwner().getStat().getAttackElement() : -1;
	}
}