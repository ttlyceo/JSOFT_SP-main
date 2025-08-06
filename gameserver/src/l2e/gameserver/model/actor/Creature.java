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

import static l2e.gameserver.ai.model.CtrlIntention.ACTIVE;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.StampedLock;

import l2e.gameserver.model.entity.events.tournaments.*;
import l2e.gameserver.model.entity.events.tournaments.model.TournamentStats;
import l2e.gameserver.model.entity.events.tournaments.model.TournamentTeam;
import org.napile.primitive.maps.IntObjectMap;
import org.napile.primitive.maps.impl.CHashIntObjectMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2e.commons.apache.ArrayUtils;
import l2e.commons.listener.Listener;
import l2e.commons.util.PositionUtils;
import l2e.commons.util.PositionUtils.TargetDirection;
import l2e.commons.util.Rnd;
import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.GameTimeController;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.ai.DefaultAI;
import l2e.gameserver.ai.character.CharacterAI;
import l2e.gameserver.ai.model.CtrlEvent;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.data.parser.ItemsParser;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.geodata.GeoEngine;
import l2e.gameserver.geodata.pathfinding.AbstractNodeLoc;
import l2e.gameserver.geodata.pathfinding.PathFinding;
import l2e.gameserver.handler.skillhandlers.ISkillHandler;
import l2e.gameserver.handler.skillhandlers.SkillHandler;
import l2e.gameserver.instancemanager.MapRegionManager;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.instancemanager.TerritoryWarManager;
import l2e.gameserver.instancemanager.TownManager;
import l2e.gameserver.instancemanager.ZoneManager;
import l2e.gameserver.listener.actor.CharListenerList;
import l2e.gameserver.model.AccessLevel;
import l2e.gameserver.model.CategoryType;
import l2e.gameserver.model.ChanceSkillList;
import l2e.gameserver.model.CharEffectList;
import l2e.gameserver.model.Clan;
import l2e.gameserver.model.FusionSkill;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.Party;
import l2e.gameserver.model.PcCondOverride;
import l2e.gameserver.model.ShotType;
import l2e.gameserver.model.TeleportWhereType;
import l2e.gameserver.model.TimeStamp;
import l2e.gameserver.model.ToggleList;
import l2e.gameserver.model.World;
import l2e.gameserver.model.WorldRegion;
import l2e.gameserver.model.actor.instance.DoorInstance;
import l2e.gameserver.model.actor.instance.EventMapGuardInstance;
import l2e.gameserver.model.actor.instance.PetInstance;
import l2e.gameserver.model.actor.instance.player.AutoFarmOptions;
import l2e.gameserver.model.actor.instance.player.PremiumBonus;
import l2e.gameserver.model.actor.stat.CharStat;
import l2e.gameserver.model.actor.status.CharStatus;
import l2e.gameserver.model.actor.tasks.character.HitTask;
import l2e.gameserver.model.actor.tasks.character.MagicGeoCheckTask;
import l2e.gameserver.model.actor.tasks.character.MagicUseTask;
import l2e.gameserver.model.actor.tasks.character.NextActionTask;
import l2e.gameserver.model.actor.tasks.character.NotifyAITask;
import l2e.gameserver.model.actor.tasks.character.QueuedMagicUseTask;
import l2e.gameserver.model.actor.templates.character.CharTemplate;
import l2e.gameserver.model.actor.templates.items.Item;
import l2e.gameserver.model.actor.templates.items.Weapon;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.actor.templates.npc.champion.ChampionTemplate;
import l2e.gameserver.model.actor.transform.Transform;
import l2e.gameserver.model.actor.transform.TransformTemplate;
import l2e.gameserver.model.entity.Reflection;
import l2e.gameserver.model.entity.events.AbstractFightEvent;
import l2e.gameserver.model.holders.InvulSkillHolder;
import l2e.gameserver.model.holders.SkillHolder;
import l2e.gameserver.model.holders.SkillUseHolder;
import l2e.gameserver.model.interfaces.IChanceSkillTrigger;
import l2e.gameserver.model.interfaces.ILocational;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.items.itemcontainer.Inventory;
import l2e.gameserver.model.items.type.WeaponType;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestEventType;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.skills.SkillType;
import l2e.gameserver.model.skills.effects.AbnormalEffect;
import l2e.gameserver.model.skills.effects.Effect;
import l2e.gameserver.model.skills.effects.EffectFlag;
import l2e.gameserver.model.skills.effects.EffectType;
import l2e.gameserver.model.skills.funcs.Func;
import l2e.gameserver.model.skills.l2skills.SkillSummon;
import l2e.gameserver.model.skills.options.OptionsSkillHolder;
import l2e.gameserver.model.skills.options.OptionsSkillType;
import l2e.gameserver.model.skills.targets.TargetType;
import l2e.gameserver.model.stats.Calculator;
import l2e.gameserver.model.stats.Formulas;
import l2e.gameserver.model.stats.Stats;
import l2e.gameserver.model.zone.ZoneId;
import l2e.gameserver.model.zone.ZoneType;
import l2e.gameserver.model.zone.type.FunPvpZone;
import l2e.gameserver.model.zone.type.NoGeoZone;
import l2e.gameserver.model.zone.type.PeaceZone;
import l2e.gameserver.model.zone.type.SiegeZone;
import l2e.gameserver.model.zone.type.WaterZone;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.ActionFail;
import l2e.gameserver.network.serverpackets.Attack;
import l2e.gameserver.network.serverpackets.ChangeMoveType;
import l2e.gameserver.network.serverpackets.ChangeWaitType;
import l2e.gameserver.network.serverpackets.CreatureSay;
import l2e.gameserver.network.serverpackets.ExRotation;
import l2e.gameserver.network.serverpackets.FlyToLocation;
import l2e.gameserver.network.serverpackets.FlyToLocation.FlyType;
import l2e.gameserver.network.serverpackets.GameServerPacket;
import l2e.gameserver.network.serverpackets.MagicSkillCanceled;
import l2e.gameserver.network.serverpackets.MagicSkillLaunched;
import l2e.gameserver.network.serverpackets.MagicSkillUse;
import l2e.gameserver.network.serverpackets.MoveToLocation;
import l2e.gameserver.network.serverpackets.Revive;
import l2e.gameserver.network.serverpackets.SetupGauge;
import l2e.gameserver.network.serverpackets.SocialAction;
import l2e.gameserver.network.serverpackets.StatusUpdate;
import l2e.gameserver.network.serverpackets.StopMove;
import l2e.gameserver.network.serverpackets.SystemMessage;
import l2e.gameserver.network.serverpackets.TeleportToLocation;
import l2e.gameserver.network.serverpackets.ValidateLocation;
import l2e.gameserver.network.serverpackets.updatetype.UserInfoType;
import l2e.gameserver.taskmanager.AttackStanceTaskManager;

public abstract class Creature extends GameObject
{
	public static final Logger _log = LoggerFactory.getLogger(Creature.class);
	
	protected volatile CharListenerList _listeners;
	
	private volatile Set<Creature> _attackByList;
	private volatile boolean _isCastingNow = false;
	private volatile boolean _isCastingSimultaneouslyNow = false;
	
	private boolean _isDead = false;
	private boolean _isImmobilized = false;
	private boolean _isOverloaded = false;
	private boolean _isStunned = false;
	private boolean _isFakeDeath = false;
	private boolean _isParalyzed = false;
	private boolean _isPendingRevive = false;
	private boolean _isRunning = false;
	private boolean _isNoRndWalk = false;
	protected boolean _showSummonAnimation = false;
	private boolean _isTeleporting = false;
	private boolean _isInvul = false;
	private boolean _isUndying = false;
	private boolean _isDamageBlock = false;
	private boolean _isMortal = true;
	private boolean _isFlying = false;
	private boolean _isDanceStun = false;
	private boolean _isRooted = false;
	private boolean _isHealBlocked = false;
	private boolean _buffImmunity = false;
	private boolean _debuffImmunity = false;
	private ChampionTemplate _championTemplate = null;

	private Location _flyLoc;
	private boolean _isFlyingNow = false;
	private Location _flyDestLoc;
	private boolean _blocked;
	
	private CharStat _stat;
	private CharStatus _status;
	private CharTemplate _template;
	
	public static final double MAX_HP_BAR_PX = 352.0;
	
	private double _hpUpdateIncCheck = .0;
	private double _hpUpdateDecCheck = .0;
	private double _hpUpdateInterval = .0;
	
	private Calculator[] _calculators;
	protected final ReentrantLock _lock = new ReentrantLock();
	protected Collection<DoorInstance> _doors;
	
	private final Map<Integer, Skill> _skills = new ConcurrentHashMap<>();
	private final List<Skill> _blockSkills = new ArrayList<>();
	
	private volatile ChanceSkillList _chanceSkills;
	
	protected FusionSkill _fusionSkill;
	
	protected byte _zoneValidateCounter = 4;
	private List<ZoneType> _zoneList = null;
	private final ReentrantLock _zoneLock = new ReentrantLock();
	
	private Creature _debugger = null;
	
	private final ReentrantLock _teleportLock;
	private int _team;
	protected long _exceptions = 0L;
	
	private volatile Map<Integer, OptionsSkillHolder> _triggerSkills;
	private volatile Map<Integer, InvulSkillHolder> _invulAgainst;
	
	private final Set<AbnormalEffect> _abnormalEffects = new CopyOnWriteArraySet<>();
	private int _abnormalEffectsMask;
	private int _abnormalEffectsMask2;
	private int _abnormalEffectsMask3;
	
	protected IntObjectMap<TimeStamp> _skillReuses = new CHashIntObjectMap<>();
	private boolean _allSkillsDisabled;
	private final StampedLock _attackLock = new StampedLock();
	private GameObject _target;
	private Creature _castingTarget;
	
	private volatile long _attackEndTime;
	private long _disableBowAttackEndTime;
	
	private Skill _castingSkill;
	private boolean _isCriticalBlowCastingSkill;
	private TargetDirection _direction = TargetDirection.NONE;
	private long _castInterruptTime;
	private long _animationEndTime;
	
	private static final Calculator[] NPC_STD_CALCULATOR = Formulas.getStdNPCCalculators();
	
	protected volatile CharacterAI _ai = null;
	
	private Future<?> _skillCast;
	private Future<?> _skillCast2;
	public Future<?> _skillGeoCheckTask;
	private final ToggleList _toggleList = new ToggleList(this);
	
	protected MoveData _move;
	private boolean _keyboardMovement = false;
	private long _validationInterval = 0L;
	private boolean _isCorrectPos = false;
	
	public static class MoveData
	{
		public int _moveStartTime;
		public int _tick;
		public int _moveTimestamp;
		public int _xDestination;
		public int _yDestination;
		public int _zDestination;
		public double _xAccurate;
		public double _yAccurate;
		public double _zAccurate;
		public int _heading;
		public boolean _isIgnoreGeo;
		public int _pathFindIndex;
		public List<AbstractNodeLoc> _moveList;
		public int geoPathAccurateTx;
		public int geoPathAccurateTy;
		public int geoPathGtx;
		public int geoPathGty;
	}
	
	public boolean isDebug()
	{
		return _debugger != null;
	}
	
	public void setDebug(Creature d)
	{
		_debugger = d;
	}
	
	public void sendDebugPacket(GameServerPacket pkt)
	{
		if (_debugger != null)
		{
			_debugger.sendPacket(pkt);
		}
	}
	
	public void sendDebugMessage(String msg)
	{
		if (_debugger != null)
		{
			_debugger.sendMessage(msg);
		}
	}
	
	public Inventory getInventory()
	{
		return null;
	}
	
	public boolean destroyItemByItemId(String process, int itemId, long count, GameObject reference, boolean sendMessage)
	{
		return true;
	}
	
	public boolean destroyItem(String process, int objectId, long count, GameObject reference, boolean sendMessage)
	{
		return true;
	}
	
	@Override
	public boolean isInsideZone(ZoneId zone)
	{
		_zoneLock.lock();
		try
		{
			Reflection ref = null;
			if (getReflectionId() > 0)
			{
				ref = ReflectionManager.getInstance().getReflection(getReflectionId());
			}
			
			switch (zone)
			{
				case PVP :
					if ((ref != null) && ref.isPvPInstance())
					{
						return true;
					}
					break;
				case PEACE :
					if ((ref != null) && ref.isPvPInstance())
					{
						return false;
					}
			}
			
			if (_zoneList == null)
			{
				return false;
			}
			
			ZoneType zType;
			for (int i = 0; i < _zoneList.size(); i++)
			{
				zType = _zoneList.get(i);
				if (zType.isEnabled() && zType.getZoneId().contains(zone))
				{
					return true;
				}
			}
		}
		finally
		{
			_zoneLock.unlock();
		}
		return false;
	}
	
	public boolean isInsideZone(ZoneId zone, ZoneType type)
	{
		_zoneLock.lock();
		try
		{
			if (_zoneList == null)
			{
				return false;
			}
			
			ZoneType zType;
			for (int i = 0; i < _zoneList.size(); i++)
			{
				zType = _zoneList.get(i);
				if (zType == type)
				{
					continue;
				}
				
				if (zType.getZoneId().contains(zone))
				{
					return true;
				}
			}
		}
		finally
		{
			_zoneLock.unlock();
		}
		return false;
	}

	public boolean isInsideZoneExcluding(ZoneId zone, ZoneType excluded)
	{
		_zoneLock.lock();
		try
		{
			Reflection ref = null;
			if (getReflectionId() > 0)
			{
				ref = ReflectionManager.getInstance().getReflection(getReflectionId());
			}

			switch (zone)
			{
				case PVP :
					if ((ref != null) && ref.isPvPInstance())
					{
						return true;
					}
					break;
				case PEACE :
					if ((ref != null) && ref.isPvPInstance())
					{
						return false;
					}
			}

			if (_zoneList == null)
			{
				return false;
			}

			for (final ZoneType zType : _zoneList) {
				if (zType != excluded && zType.getZoneId().contains(zone)) {
					return true;
				}
			}
		}
		finally
		{
			_zoneLock.unlock();
		}
		return false;
	}
	
	public boolean isTransformed()
	{
		return false;
	}
	
	public Transform getTransformation()
	{
		return null;
	}
	
	public void untransform()
	{
	}
	
	public boolean isGM()
	{
		return false;
	}
	
	public AccessLevel getAccessLevel()
	{
		return null;
	}
	
	public Creature(int objectId, CharTemplate template)
	{
		super(objectId);
		
		if (template == null)
		{
			throw new NullPointerException("Template is null!");
		}
		setInstanceType(InstanceType.Creature);
		initCharStat();
		initCharStatus();
		
		_template = template;
		
		if (isDoor())
		{
			_calculators = Formulas.getStdDoorCalculators();
		}
		else if (isNpc())
		{
			_calculators = NPC_STD_CALCULATOR;
			
			if (template.getSkills() != null)
			{
				_skills.putAll(template.getSkills());
			}
			
			for (final Skill skill : _skills.values())
			{
				addStatFuncs(skill.getStatFuncs(null, this));
			}
		}
		else
		{
			_calculators = new Calculator[Stats.NUM_STATS];
			
			if (isSummon())
			{
				_skills.putAll(((NpcTemplate) template).getSkills());
				
				for (final Skill skill : _skills.values())
				{
					addStatFuncs(skill.getStatFuncs(null, this));
				}
			}
			Formulas.addFuncsToNewCharacter(this);
		}
		_teleportLock = new ReentrantLock();
		if (!isSummon())
		{
			GameObjectsStorage.put(this);
		}
	}
	
	protected void initCharStatusUpdateValues()
	{
		_hpUpdateIncCheck = getMaxHp();
		_hpUpdateInterval = _hpUpdateIncCheck / MAX_HP_BAR_PX;
		_hpUpdateDecCheck = _hpUpdateIncCheck - _hpUpdateInterval;
	}
	
	public void onDecay()
	{
		decayMe();
	}
	
	public void endDecayTask()
	{
	}
	
	@Override
	public void onSpawn()
	{
		super.onSpawn();
		revalidateZone(true);
	}
	
	@Override
	public final void setWorldRegion(WorldRegion value)
	{
		super.setWorldRegion(value);
		revalidateZone(true);
	}
	
	public void onTeleported()
	{
		if (!_teleportLock.tryLock())
		{
			return;
		}
		try
		{
			if (!isTeleporting())
			{
				return;
			}
			spawnMe(getX(), getY(), getZ());
			setIsTeleporting(false);
		}
		finally
		{
			_teleportLock.unlock();
		}
		
		if (_isPendingRevive)
		{
			doRevive();
		}
	}
	
	public void addAttackerToAttackByList(Creature player)
	{
	}
	
	public void broadcastPacket(GameServerPacket... packets)
	{
		sendPacket(packets);
		broadcastPacketToOthers(packets);
	}
	
	public void broadcastPacket(List<GameServerPacket> packets)
	{
		broadcastPacket(packets.toArray(new GameServerPacket[packets.size()]));
	}
	
	public void broadcastPacket(int range, GameServerPacket... packets)
	{
		sendPacket(packets);
		if (!isVisible() || packets.length == 0)
		{
			return;
		}
		World.getInstance().getAroundPlayers(this, range, 300).stream().filter(p -> p != null).forEach(p -> p.sendPacket(packets));
	}
	
	public void broadcastPacketToOthers(int range, GameServerPacket... packets)
	{
		if (!isVisible() || packets.length == 0)
		{
			return;
		}
		World.getInstance().getAroundPlayers(this, range, 300).stream().filter(p -> p != null).forEach(p -> p.sendPacket(packets));
	}
	
	public void broadcastPacketToOthers(GameServerPacket... packets)
	{
		if (!isVisible() || packets.length == 0)
		{
			return;
		}
		World.getInstance().getAroundPlayers(this).stream().filter(p -> p != null).forEach(p -> p.sendPacket(packets));
	}
	
	public void broadcastPacketToOthers(List<GameServerPacket> packets)
	{
		broadcastPacketToOthers(packets.toArray(new GameServerPacket[packets.size()]));
	}
	
	protected boolean needHpUpdate()
	{
		final double currentHp = getCurrentHp();
		final double maxHp = getMaxHp();
		
		if ((currentHp <= 1.0) || (maxHp < MAX_HP_BAR_PX))
		{
			return true;
		}
		
		if ((currentHp < _hpUpdateDecCheck) || (Math.abs(currentHp - _hpUpdateDecCheck) <= 1e-6) || (currentHp > _hpUpdateIncCheck) || (Math.abs(currentHp - _hpUpdateIncCheck) <= 1e-6))
		{
			if (Math.abs(currentHp - maxHp) <= 1e-6)
			{
				_hpUpdateIncCheck = currentHp + 1;
				_hpUpdateDecCheck = currentHp - _hpUpdateInterval;
			}
			else
			{
				final double doubleMulti = currentHp / _hpUpdateInterval;
				int intMulti = (int) doubleMulti;
				
				_hpUpdateDecCheck = _hpUpdateInterval * (doubleMulti < intMulti ? intMulti-- : intMulti);
				_hpUpdateIncCheck = _hpUpdateDecCheck + _hpUpdateInterval;
			}
			
			return true;
		}
		
		return false;
	}
	
	public void broadcastStatusUpdate()
	{
		if (getStatus().getStatusListener().isEmpty() || !needHpUpdate())
		{
			return;
		}
		final var su = makeStatusUpdate(StatusUpdate.MAX_HP, StatusUpdate.CUR_HP);
		getStatus().getStatusListener().stream().filter(c -> c != null).forEach(c -> c.sendPacket(su));
	}
	
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
			}
		}
		return su;
	}
	
	public void sendMessage(String text)
	{
	}
	
	public void teleToLocation(int x, int y, int z, int heading, int randomOffset, boolean revalidateZone, boolean sendTelePacket, Reflection r)
	{
		abortAttack();
		abortCast();
		stopMove(null);
		setIsTeleporting(true);
		setTarget(null);
		
		getAI().setIntention(CtrlIntention.ACTIVE);
		
		if (Config.OFFSET_ON_TELEPORT_ENABLED && (randomOffset > 0))
		{
			final Location loc = Location.findAroundPosition(x, y, z, (randomOffset / 2), randomOffset, r);
			if (loc != null)
			{
				x = loc.getX();
				y = loc.getY();
				z = loc.getZ();
			}
		}
		
		if (!(isPlayer() && (getActingPlayer().isInVehicle()) && !isFlying() && ZoneManager.getInstance().getZone(x, y, z, WaterZone.class) == null) && ZoneManager.getInstance().getZone(x, y, z, NoGeoZone.class) == null)
		{
			z = GeoEngine.getInstance().getHeight(x, y, z);
		}
		
		if (isPlayer())
		{
			getActingPlayer().setClientLoc(null);
			getActingPlayer().setFallingLoc(null);
		}
		
		decayMe();
		
		setXYZ(x, y, z);
		
		setReflection(r);
		
		if (sendTelePacket)
		{
			broadcastPacket(new TeleportToLocation(this, x, y, z, heading));
		}
		
		if (heading != 0)
		{
			setHeading(heading);
		}
		
		if (!isPlayer() || ((getActingPlayer().getClient() != null) && getActingPlayer().getClient().isDetached()))
		{
			onTeleported();
		}
		
		if (isPlayer())
		{
			getActingPlayer().getListeners().onTeleport(x, y, z);
		}
		
		if (revalidateZone)
		{
			revalidateZone(true);
		}
	}
	
	public void teleToLocation(int x, int y, int z, boolean revalidateZone, boolean sendTelePacket, Reflection r)
	{
		teleToLocation(x, y, z, getHeading(), 0, revalidateZone, sendTelePacket, r);
	}
	
	public void teleToLocation(int x, int y, int z, boolean revalidateZone, Reflection r)
	{
		teleToLocation(x, y, z, getHeading(), 0, revalidateZone, true, r);
	}
	
	public void teleToLocation(int x, int y, int z, int randomOffset, boolean revalidateZone, Reflection r)
	{
		teleToLocation(x, y, z, getHeading(), randomOffset, revalidateZone, true, r);
	}
	
	public void teleToLocation(ILocational loc, int randomOffset, boolean revalidateZone, Reflection r)
	{
		teleToLocation(loc.getX(), loc.getY(), loc.getZ(), getHeading(), randomOffset, revalidateZone, true, r);
	}
	
	public void teleToLocation(ILocational loc, boolean revalidateZone, Reflection r)
	{
		teleToLocation(new Location(loc.getX(), loc.getY(), loc.getZ(), loc.getHeading()), 0, revalidateZone, r);
	}
	
	public void teleToLocation(TeleportWhereType teleportWhere, boolean revalidateZone, Reflection r)
	{
		teleToLocation(MapRegionManager.getInstance().getTeleToLocation(this, teleportWhere), revalidateZone, r);
	}
	
	public void teleToLocation(ILocational loc, boolean allowRandomOffset, boolean revalidateZone, Reflection r)
	{
		teleToLocation(loc, (allowRandomOffset ? Config.MAX_OFFSET_ON_TELEPORT : 0), revalidateZone, r);
	}
	
	public void teleToLocation(int x, int y, int z, boolean allowRandomOffset, boolean revalidateZone, boolean sendTelePacket, Reflection r)
	{
		if (allowRandomOffset)
		{
			teleToLocation(x, y, z, Config.MAX_OFFSET_ON_TELEPORT, revalidateZone, sendTelePacket, r);
		}
		else
		{
			teleToLocation(x, y, z, 0, revalidateZone, sendTelePacket, r);
		}
	}
	
	public void teleToLocation(int x, int y, int z, int heading, boolean allowRandomOffset, boolean revalidateZone, Reflection r)
	{
		if (allowRandomOffset)
		{
			teleToLocation(x, y, z, heading, Config.MAX_OFFSET_ON_TELEPORT, revalidateZone, true, r);
		}
		else
		{
			teleToLocation(x, y, z, heading, 0, revalidateZone, true, r);
		}
	}
	
	private boolean canUseRangeWeapon()
	{
		if (isTransformed())
		{
			return true;
		}
		
		final var weaponItem = getActiveWeaponItem();
		if ((weaponItem == null) || !weaponItem.getItemType().isRanged())
		{
			return false;
		}
		
		if (isPlayer())
		{
			final var isBow = weaponItem.getItemType().isBow();
			if (isBow ? !checkAndEquipArrows() : !checkAndEquipBolts())
			{
				getAI().setIntention(CtrlIntention.IDLE);
				sendActionFailed();
				sendPacket(isBow ? SystemMessageId.NOT_ENOUGH_ARROWS : SystemMessageId.NOT_ENOUGH_BOLTS);
				return false;
			}
			
			final long timeToNextBowCrossBowAttack = _disableBowAttackEndTime - System.currentTimeMillis();
			if (timeToNextBowCrossBowAttack > 0)
			{
				ThreadPoolManager.getInstance().schedule(new NotifyAITask(this, CtrlEvent.EVT_READY_TO_ACT), timeToNextBowCrossBowAttack);
				sendActionFailed();
				return false;
			}
			
			int mpConsume = weaponItem.getMpConsume();
			if ((weaponItem.getReducedMpConsume() > 0) && Rnd.chance(weaponItem.getReducedMpConsumeChance()))
			{
				mpConsume = weaponItem.getReducedMpConsume();
			}
			mpConsume = (int) calcStat(Stats.BOW_MP_CONSUME_RATE, mpConsume, null, null);
			
			if (getCurrentMp() < mpConsume)
			{
				ThreadPoolManager.getInstance().schedule(new NotifyAITask(this, CtrlEvent.EVT_READY_TO_ACT), 100);
				sendPacket(SystemMessageId.NOT_ENOUGH_MP);
				sendActionFailed();
				return false;
			}
			
			if (mpConsume > 0)
			{
				getStatus().reduceMp(mpConsume);
			}
		}
		else if (isNpc())
		{
			if (_disableBowAttackEndTime > System.currentTimeMillis())
			{
				return false;
			}
		}
		return true;
	}
	
	public void doAttack(Creature target)
	{
		final long stamp = _attackLock.tryWriteLock();
		if (stamp == 0)
		{
			return;
		}
		try
		{
			if ((target == null) || isAttackingDisabled())
			{
				return;
			}
			
			if (isSummon() && isCancelAction())
			{
				((Summon) this).setCancelAction(false);
				getAI().setIntention(ACTIVE);
				return;
			}

			if (isInFightEvent())
			{
				if (target.isPlayable() && !target.isInFightEvent())
				{
					if (isPlayer())
					{
						sendActionFailed();
						getAI().setIntention(ACTIVE);
					}
					return;
				}
				else
				{
					for (final AbstractFightEvent e : getFightEvents())
					{
						if (e != null && !e.canAttack(target, this))
						{
							if (isPlayer())
							{
								sendActionFailed();
								getAI().setIntention(ACTIVE);
							}
							return;
						}
					}
				}
			}

			if (isInPartyTournament())
			{
				if (target.isPlayable() && !target.isInPartyTournament())
				{
					if (isPlayer())
					{
						sendActionFailed();
						getAI().setIntention(ACTIVE);
					}
					return;
				}
				else
				{
					var e = getPartyTournament();
					if (e != null && !e.canAttack(target, this))
					{
						if (isPlayer())
						{
							sendActionFailed();
							getAI().setIntention(ACTIVE);
						}
						return;
					}
				}
			}
			
			if (!isAlikeDead())
			{
				if ((isNpc() && target.isAlikeDead()) || !World.getInstance().getAroundCharacters(this).contains(target))
				{
					getAI().setIntention(CtrlIntention.ACTIVE);
					sendActionFailed();
					return;
				}
				else if (isPlayer())
				{
					if (target.isDead() || !target.isVisibleFor(getActingPlayer()))
					{
						getAI().setIntention(CtrlIntention.ACTIVE);
						sendActionFailed();
						return;
					}
					
					final Player actor = getActingPlayer();
					if (actor.isTransformed() && !actor.getTransformation().canAttack())
					{
						sendActionFailed();
						return;
					}
				}
				else if (isSummon() && target.isDead())
				{
					((Summon) this).setFollowStatus(true);
					getAI().setIntention(ACTIVE);
					return;
				}
			}
			
			if (getActiveWeaponItem() != null)
			{
				final Weapon wpn = getActiveWeaponItem();
				if ((wpn != null) && !wpn.isAttackWeapon() && !isGM())
				{
					if (wpn.getItemType() == WeaponType.FISHINGROD)
					{
						sendPacket(SystemMessageId.CANNOT_ATTACK_WITH_FISHING_POLE);
					}
					else
					{
						sendPacket(SystemMessageId.THAT_WEAPON_CANT_ATTACK);
					}
					sendActionFailed();
					return;
				}
			}
			
			if (getActingPlayer() != null && !isInFightEvent())
			{
				final var siegeZone = ZoneManager.getInstance().getZone(this, SiegeZone.class);
				if (getActingPlayer().inObserverMode())
				{
					sendPacket(SystemMessageId.OBSERVERS_CANNOT_PARTICIPATE);
					sendActionFailed();
					return;
				}
				else if ((target.getActingPlayer() != null) && (getActingPlayer().getSiegeState() > 0) && siegeZone != null && (target.getActingPlayer().getSiegeState() == getActingPlayer().getSiegeState()) && (target.getActingPlayer() != this) && (target.getActingPlayer().getSiegeSide() == getActingPlayer().getSiegeSide()))
				{
					if (siegeZone.isAttackSameSiegeSide())
					{
						final Clan clan1 = target.getActingPlayer().getClan();
						final Clan clan2 = getActingPlayer().getClan();
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
								return;
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
						return;
					}
				}
				else if (target.isInsidePeaceZone(getActingPlayer()))
				{
					getAI().setIntention(CtrlIntention.ACTIVE);
					sendActionFailed();
					return;
				}
			}
			else if (isInsidePeaceZone(this, target))
			{
				getAI().setIntention(CtrlIntention.ACTIVE);
				sendActionFailed();
				return;
			}
			
			stopEffectsOnAction();
			
			if (!GeoEngine.getInstance().canSeeTarget(this, target))
			{
				sendPacket(SystemMessageId.CANT_SEE_TARGET);
				getAI().setIntention(CtrlIntention.ACTIVE);
				sendActionFailed();
				return;
			}
			
			if (isAttackable())
			{
				stopMove(getLocation());
			}
			
			getListeners().onAttack(target);
			
			final Weapon weaponItem = getActiveWeaponItem();
			final int timeAtk = calculateTimeBetweenAttacks();
			final int timeToHit = timeAtk / 2;
			
			if (!isChargedShot(ShotType.SOULSHOTS))
			{
				rechargeShots(true, false);
			}
			
			final Attack attack = new Attack(this, target, isChargedShot(ShotType.SOULSHOTS), (weaponItem != null) ? weaponItem.getItemGradeSPlus() : 0);
			setHeading(Util.calculateHeadingFrom(this, target));
			final int reuse = calculateReuseTime(weaponItem);
			
			if (isNpc() || isSummon())
			{
				_attackEndTime = System.nanoTime() + TimeUnit.NANOSECONDS.convert(timeAtk, TimeUnit.MILLISECONDS);
			}
			
			final var isAroundAttack = (int) getStat().calcStat(Stats.ALLOW_AROUND_ATTACK, 0, null, null) > 0;
			
			boolean hitted = false;
			switch (getAttackType())
			{
				case BOW :
				{
					if (!canUseRangeWeapon())
					{
						return;
					}
					_attackEndTime = System.nanoTime() + TimeUnit.NANOSECONDS.convert(timeAtk, TimeUnit.MILLISECONDS);
					hitted = doAttackHitByBow(attack, target, timeAtk, reuse);
					if (isAroundAttack)
					{
						hitted |= doAttackHitByAround(attack, target, timeToHit);
					}
					break;
				}
				case CROSSBOW :
				{
					if (!canUseRangeWeapon())
					{
						return;
					}
					_attackEndTime = System.nanoTime() + TimeUnit.NANOSECONDS.convert(timeAtk, TimeUnit.MILLISECONDS);
					hitted = doAttackHitByCrossBow(attack, target, timeAtk, reuse);
					if (isAroundAttack)
					{
						hitted |= doAttackHitByAround(attack, target, timeToHit);
					}
					break;
				}
				case POLE :
				{
					_attackEndTime = System.nanoTime() + TimeUnit.NANOSECONDS.convert(timeAtk, TimeUnit.MILLISECONDS);
					hitted = doAttackHitByPole(attack, target, timeToHit);
					if (isAroundAttack)
					{
						hitted |= doAttackHitByAround(attack, target, timeToHit);
					}
					break;
				}
				case FIST :
				{
					if (!isPlayer())
					{
						_attackEndTime = System.nanoTime() + TimeUnit.NANOSECONDS.convert(timeAtk, TimeUnit.MILLISECONDS);
						hitted = doAttackHitSimple(attack, target, timeToHit, true);
						break;
					}
				}
				case DUAL :
				case DUALFIST :
				case DUALDAGGER :
				{
					_attackEndTime = System.nanoTime() + TimeUnit.NANOSECONDS.convert(timeAtk, TimeUnit.MILLISECONDS);
					hitted = doAttackHitByDual(attack, target, timeToHit);
					if (isAroundAttack)
					{
						hitted |= doAttackHitByAround(attack, target, timeToHit);
					}
					break;
				}
				default :
				{
					_attackEndTime = System.nanoTime() + TimeUnit.NANOSECONDS.convert(timeAtk, TimeUnit.MILLISECONDS);
					hitted = doAttackHitSimple(attack, target, timeToHit, true);
					if (isAroundAttack)
					{
						hitted |= doAttackHitByAround(attack, target, timeToHit);
					}
					break;
				}
			}
			
			final Player player = getActingPlayer();
			if (player != null)
			{
				AttackStanceTaskManager.getInstance().addAttackStanceTask(player);
				
				if (player.getSummon() != target)
				{
					player.updatePvPStatus(target);
				}
			}
			
			if (!hitted)
			{
				abortAttack();
			}
			else
			{
				setChargedShot(ShotType.SOULSHOTS, false);
				
				if (player != null)
				{
					if (player.isCursedWeaponEquipped())
					{
						if (!target.isInvul())
						{
							target.setCurrentCp(0);
						}
					}
					else if (player.isHero())
					{
						if (target.isPlayer() && target.getActingPlayer().isCursedWeaponEquipped())
						{
							target.setCurrentCp(0);
						}
					}
				}
			}
			
			if (attack.hasHits())
			{
				broadcastPacket(attack);
			}
			
			ThreadPoolManager.getInstance().schedule(new NotifyAITask(this, CtrlEvent.EVT_READY_TO_ACT), timeAtk);
		}
		finally
		{
			_attackLock.unlockWrite(stamp);
		}
	}
	
	private boolean doAttackHitByBow(Attack attack, Creature target, int sAtk, int reuse)
	{
		int damage1 = 0;
		byte shld1 = 0;
		boolean crit1 = false;
		
		final boolean miss1 = Formulas.calcHitMiss(this, target);
		
		if (!Config.INFINITE_ARROWS)
		{
			reduceArrowCount(false);
		}
		stopMove(false);
		
		if (!miss1)
		{
			shld1 = Formulas.calcShldUse(this, target);
			crit1 = Rnd.chance(Formulas.calcCrit(this, target, null, false));
			
			damage1 = (int) Formulas.calcPhysDam(this, target, null, shld1, crit1, attack.hasSoulshot());
			damage1 *= (Math.sqrt(getDistanceSq(target)) / 4000) + 0.8;
		}
		
		if (isPlayer())
		{
			sendPacket(new SetupGauge(this, SetupGauge.RED, sAtk + reuse));
		}
		ThreadPoolManager.getInstance().schedule(new HitTask(this, target, damage1, crit1, miss1, attack.hasSoulshot(), shld1), sAtk);
		
		_disableBowAttackEndTime = System.currentTimeMillis() + (sAtk + reuse);
		attack.addHit(target, damage1, miss1, crit1, shld1);
		
		return !miss1;
	}
	
	private boolean doAttackHitByCrossBow(Attack attack, Creature target, int sAtk, int reuse)
	{
		int damage1 = 0;
		byte shld1 = 0;
		boolean crit1 = false;
		
		final boolean miss1 = Formulas.calcHitMiss(this, target);
		
		if (!Config.INFINITE_ARROWS)
		{
			reduceArrowCount(true);
		}
		stopMove(false);
		
		if (!miss1)
		{
			shld1 = Formulas.calcShldUse(this, target);
			crit1 = Rnd.chance(Formulas.calcCrit(this, target, null, false));
			damage1 = (int) Formulas.calcPhysDam(this, target, null, shld1, crit1, attack.hasSoulshot());
		}
		
		if (isPlayer())
		{
			sendPacket(SystemMessageId.CROSSBOW_PREPARING_TO_FIRE);
			final SetupGauge sg = new SetupGauge(this, SetupGauge.RED, sAtk + reuse);
			sendPacket(sg);
		}
		
		ThreadPoolManager.getInstance().schedule(new HitTask(this, target, damage1, crit1, miss1, attack.hasSoulshot(), shld1), sAtk);
		_disableBowAttackEndTime = System.currentTimeMillis() + (sAtk + reuse);
		attack.addHit(target, damage1, miss1, crit1, shld1);
		
		return !miss1;
	}
	
	private boolean doAttackHitByDual(Attack attack, Creature target, int sAtk)
	{
		int damage1 = 0;
		int damage2 = 0;
		byte shld1 = 0;
		byte shld2 = 0;
		boolean crit1 = false;
		boolean crit2 = false;
		final boolean miss1 = Formulas.calcHitMiss(this, target);
		final boolean miss2 = Formulas.calcHitMiss(this, target);
		
		if (!miss1)
		{
			shld1 = Formulas.calcShldUse(this, target);
			crit1 = Rnd.chance(Formulas.calcCrit(this, target, null, false));
			damage1 = (int) Formulas.calcPhysDam(this, target, null, shld1, crit1, attack.hasSoulshot());
			damage1 /= 2;
		}
		
		if (!miss2)
		{
			shld2 = Formulas.calcShldUse(this, target);
			crit2 = Rnd.chance(Formulas.calcCrit(this, target, null, false));
			damage2 = (int) Formulas.calcPhysDam(this, target, null, shld2, crit2, attack.hasSoulshot());
			damage2 /= 2;
		}
		ThreadPoolManager.getInstance().schedule(new HitTask(this, target, damage1, crit1, miss1, attack.hasSoulshot(), shld1), sAtk / 2);
		ThreadPoolManager.getInstance().schedule(new HitTask(this, target, damage2, crit2, miss2, attack.hasSoulshot(), shld2), sAtk);
		
		attack.addHit(target, damage1, miss1, crit1, shld1);
		attack.addHit(target, damage2, miss2, crit2, shld2);
		
		return (!miss1 || !miss2);
	}
	
	private boolean doAttackHitByPole(Attack attack, Creature target, int sAtk)
	{
		boolean hitted = doAttackHitSimple(attack, target, 100, sAtk, true, true);
		if (isAffected(EffectFlag.SINGLE_TARGET))
		{
			return hitted;
		}
		
		final int attackRandomCountMax = (int) getStat().calcStat(Stats.ATTACK_COUNT_MAX, 1, null, null);
		int attackcount = 0;
		final Weapon weaponItem = getActiveWeaponItem();
		if (weaponItem == null)
		{
			return hitted;
		}
		
		final int[] _fanRange = weaponItem.getDamageRange();
		if (_fanRange != null)
		{
			final List<Creature> targets = new ArrayList<>();
			for (final Creature obj : World.getInstance().getAroundCharacters(this, (_fanRange[2] + (getPhysicalAttackRange() / 2)), 200))
			{
				if (attackcount >= attackRandomCountMax)
				{
					break;
				}
				
				if ((obj == null) || (obj == target))
				{
					continue;
				}
				
				if (obj.isPet() && isPlayer() && (((PetInstance) obj).getOwner() == getActingPlayer()))
				{
					continue;
				}
				
				if (Math.abs(obj.getZ() - getZ()) > 650)
				{
					continue;
				}
				
				if (!Util.isOnAngle(this, obj, _fanRange[1], _fanRange[3]))
				{
					continue;
				}
				
				if (isAttackable() && obj.isPlayer() && (getTarget() != null && getTarget().isAttackable()))
				{
					continue;
				}
				
				if (!obj.isAlikeDead())
				{
					if ((obj == getAI().getAttackTarget()) || obj.isAutoAttackable(this, !Config.ALLOW_POLE_FLAG_AROUND))
					{
						targets.add(obj);
						attackcount++;
					}
				}
			}
			
			if (!targets.isEmpty())
			{
				final double percent = polePencentDamage(attackcount + 1);
				for (final Creature obj : targets)
				{
					if (obj != null && !obj.isDead())
					{
						hitted |= doAttackHitSimple(attack, obj, percent, sAtk, true, true);
					}
				}
				targets.clear();
				return hitted;
			}
		}
		return hitted;
	}
	
	private boolean doAttackHitByAround(Attack attack, Creature target, int sAtk)
	{
		boolean hitted = false;
		final int attackCountMax = (int) getStat().calcStat(Stats.AROUND_ATTACK_COUNT_MAX, 1, null, null);
		final int attackRageMax = (int) getStat().calcStat(Stats.AROUND_ATTACK_RANGE, 1, null, null);
		int attackcount = 0;
		
		final List<Creature> targets = new ArrayList<>();
		for (final Creature obj : World.getInstance().getAroundCharacters(target, attackRageMax, 200))
		{
			if (attackcount >= attackCountMax)
			{
				break;
			}
			
			if ((obj == null) || (obj == target))
			{
				continue;
			}
			
			if (obj.isPet() && isPlayer() && (((PetInstance) obj).getOwner() == getActingPlayer()))
			{
				continue;
			}
			
			if (!GeoEngine.getInstance().canSeeTarget(this, obj))
			{
				continue;
			}
			
			if (isAttackable() && obj.isPlayer() && (getTarget() != null && getTarget().isAttackable()))
			{
				continue;
			}
			
			if (!obj.isAlikeDead())
			{
				if ((obj == getAI().getAttackTarget()) || obj.isAutoAttackable(this, !Config.ALLOW_POLE_FLAG_AROUND))
				{
					targets.add(obj);
					attackcount++;
				}
			}
		}
		
		if (!targets.isEmpty())
		{
			for (final Creature obj : targets)
			{
				if (obj != null && !obj.isDead())
				{
					hitted |= doAttackHitSimple(attack, obj, 100, sAtk, true, false);
				}
			}
			targets.clear();
			return hitted;
		}
		return hitted;
	}
	
	private double polePencentDamage(int target)
	{
		final String[] values = Config.POLE_ATTACK_MOD.split(";");
		if (values != null)
		{
			double pencent = 100;
			int curTarget = 1;
			for (final String targets : values)
			{
				final String[] info = targets.split(",");
				if (info != null && info.length == 2)
				{
					final var infoTarget = Integer.parseInt(info[0]);
					if (infoTarget == target)
					{
						return Double.parseDouble(info[1]);
					}
					
					if (infoTarget >= curTarget && target >= infoTarget)
					{
						pencent = Double.parseDouble(info[1]);
						curTarget = infoTarget;
					}
				}
			}
			return pencent;
		}
		return (100 / target);
	}
	
	private boolean doAttackHitSimple(Attack attack, Creature target, int sAtk, boolean addAnimation)
	{
		return doAttackHitSimple(attack, target, 100, sAtk, false, addAnimation);
	}
	
	private boolean doAttackHitSimple(Attack attack, Creature target, double attackpercent, int sAtk, boolean isPoleAtk, boolean addAnimation)
	{
		int damage1 = 0;
		byte shld1 = 0;
		boolean crit1 = false;
		
		final boolean miss1 = Formulas.calcHitMiss(this, target);
		
		if (!miss1)
		{
			shld1 = Formulas.calcShldUse(this, target);
			crit1 = Rnd.chance(Formulas.calcCrit(this, target, null, false));
			damage1 = (int) Formulas.calcPhysDam(this, target, null, shld1, crit1, attack.hasSoulshot());
			if (attackpercent != 100)
			{
				damage1 = (int) ((damage1 * attackpercent) / 100);
			}
			
			final Player player = getActingPlayer();
			if (player != null && isPoleAtk)
			{
				if (player.getTarget() != null && player.getTarget() != target)
				{
					player.updatePvPStatus(target);
				}
			}
		}
		ThreadPoolManager.getInstance().schedule(new HitTask(this, target, damage1, crit1, miss1, attack.hasSoulshot(), shld1), sAtk);
		if (addAnimation)
		{
			attack.addHit(target, damage1, miss1, crit1, shld1);
		}
		
		return !miss1;
	}
	
	public void doCast(Skill skill)
	{
		beginCast(skill, false);
	}
	
	public void doSimultaneousCast(Skill skill)
	{
		beginCast(skill, true);
	}
	
	public void doCast(Skill skill, Creature target, GameObject[] targets)
	{
		if (!checkDoCastConditions(skill, true))
		{
			setIsCastingNow(false);
			return;
		}
		stopEffectsOnAction();
		beginCast(skill, false, target, targets);
	}
	
	public void doSimultaneousCast(Skill skill, Creature target, GameObject[] targets)
	{
		if (!checkDoCastConditions(skill, true))
		{
			setIsCastingSimultaneouslyNow(false);
			return;
		}
		beginCast(skill, true, target, targets);
	}
	
	private void beginCast(Skill skill, boolean simultaneously)
	{
		boolean abort = false;
		
		Creature target = getTarget() != null ? (Creature) getTarget() : isPlayable() ? getAI().getCastTarget() : null;
		if (target != null && isInFightEvent())
		{
			if (target.isPlayable() && !target.isInFightEvent())
			{
				abort = true;
			}
			else
			{
				for (final AbstractFightEvent e : getFightEvents())
				{
					if (e != null && !e.canUseMagic(target, this, skill))
					{
						abort = true;
					}
				}
			}
		}

		if (target != null && isInPartyTournament())
		{
			if (target.isPlayable() && !target.isInPartyTournament())
			{
				abort = true;
			}
			else
			{
				var e = getPartyTournament();
				if (e != null && !e.canUseMagic(target, this, skill))
				{
					abort = true;
				}
			}
		}
		
		if (!checkDoCastConditions(skill, true) || abort)
		{
			if (simultaneously)
			{
				setIsCastingSimultaneouslyNow(false);
			}
			else
			{
				setIsCastingNow(false);
			}
			return;
		}
		
		if (!simultaneously)
		{
			stopEffectsOnAction();
		}
		
		final GameObject[] targets = skill.getTargetList(this);
		
		boolean doit = false;
		
		switch (skill.getTargetType())
		{
			case AREA_SUMMON :
				target = getSummon();
				break;
			case AURA :
			case AURA_CORPSE_MOB :
			case AURA_FRIENDLY :
			case AURA_DOOR :
			case AURA_FRIENDLY_SUMMON :
			case FRONT_AURA :
			case BEHIND_AURA :
			case GROUND :
				target = this;
				break;
			case AURA_MOB :
			case AURA_DEAD_MOB :
				if (targets.length == 0)
				{
					if (simultaneously)
					{
						setIsCastingSimultaneouslyNow(false);
					}
					else
					{
						setIsCastingNow(false);
					}
					
					if (isPlayer())
					{
						sendActionFailed();
						getAI().setIntention(ACTIVE);
					}
					else if (isSummon())
					{
						getAI().notifyEvent(CtrlEvent.EVT_FINISH_CASTING);
					}
					return;
				}
				target = this;
				break;
			case PET :
			case SELF :
			case SERVITOR :
			case SUMMON :
			case OWNER_PET :
			case PARTY :
			case PARTY_NOTME :
			case CLAN :
			case PARTY_CLAN :
			case CORPSE_CLAN :
			case CORPSE_FRIENDLY :
			case COMMAND_CHANNEL :
			case AURA_UNDEAD_ENEMY :
				doit = true;
			default :
				if (targets.length == 0)
				{
					if (simultaneously)
					{
						setIsCastingSimultaneouslyNow(false);
					}
					else
					{
						setIsCastingNow(false);
					}
					
					if (isPlayer())
					{
						sendActionFailed();
						getAI().setIntention(ACTIVE);
					}
					else if (isSummon())
					{
						getAI().notifyEvent(CtrlEvent.EVT_FINISH_CASTING);
					}
					return;
				}
				
				switch (skill.getSkillType())
				{
					case BUFF :
						doit = true;
						break;
					case DUMMY :
						if (skill.hasEffectType(EffectType.CPHEAL, EffectType.HEAL))
						{
							doit = true;
						}
						break;
				}
				target = (doit) ? (Creature) targets[0] : target;
		}
		beginCast(skill, simultaneously, target, targets);
	}
	
	private void beginCast(Skill skill, boolean simultaneously, Creature target, GameObject[] targets)
	{
		if (target == null)
		{
			if (simultaneously)
			{
				setIsCastingSimultaneouslyNow(false);
			}
			else
			{
				setIsCastingNow(false);
			}
			if (isPlayer())
			{
				sendActionFailed();
				getAI().setIntention(ACTIVE);
			}
			return;
		}
		
		if (isPlayable())
		{
			if ((skill.getReferenceItemId() > 0) && (ItemsParser.getInstance().getTemplate(skill.getReferenceItemId()).getBodyPart() == Item.SLOT_DECO))
			{
				for (final ItemInstance item : getInventory().getItemsByItemId(skill.getReferenceItemId()))
				{
					if (item.isEquipped())
					{
						if (item.getMana() < item.useSkillDisTime())
						{
							sendPacket(SystemMessageId.THERE_ARE_NOT_ENOUGH_NECESSARY_ITEMS_TO_USE_THE_SKILL);
							abortCast();
							return;
						}
						item.decreaseMana(false, item.useSkillDisTime());
						break;
					}
				}
			}
		}
		
		switch (skill.getFlyType())
		{
			case DUMMY :
			case CHARGE :
				if (!skill.isBlockActorMove() && isMoving())
				{
					_flyDestLoc = getDestination();
				}
				
				final Location flyLoc = getFlyLocation(target, skill);
				if (flyLoc != null)
				{
					_flyLoc = flyLoc;
					_isFlyingNow = true;
				}
				else
				{
					if (isPlayer())
					{
						if (!getActingPlayer().getFarmSystem().isLocked())
						{
							sendPacket(SystemMessageId.CANT_SEE_TARGET);
						}
					}
					setIsCastingNow(false);
					return;
				}
				break;
		}

		if (skill.getSkillType() == SkillType.ENERGY_SPEND)
		{
			final ItemInstance item = getInventory().getPaperdollItem(Inventory.PAPERDOLL_LBRACELET);
			if (item != null)
			{
				if (item.getAgathionEnergy() < skill.getEnergyConsume())
				{
					sendPacket(SystemMessageId.THE_SKILL_HAS_BEEN_CANCELED_BECAUSE_YOU_HAVE_INSUFFICIENT_ENERGY);
					setIsCastingNow(false);
					return;
				}
			}
		}

		if (skill.getSkillType() == SkillType.ENERGY_REPLENISH)
		{
			final ItemInstance item = getInventory().getPaperdollItem(Inventory.PAPERDOLL_LBRACELET);
			if (item == null)
			{
				sendPacket(SystemMessageId.YOUR_ENERGY_CANNOT_BE_REPLENISHED_BECAUSE_CONDITIONS_ARE_NOT_MET);
				setIsCastingNow(false);
				return;
			}
			else if ((item.getItem().getAgathionMaxEnergy() - item.getAgathionEnergy()) < skill.getEnergyConsume())
			{
				sendPacket(SystemMessageId.NOTHING_HAPPENED);
				setIsCastingNow(false);
				return;
			}
		}
		
		if (skill.getSkillType() == SkillType.RESURRECT)
		{
			if (isResurrectionBlocked() || target.isResurrectionBlocked())
			{
				sendPacket(SystemMessageId.REJECT_RESURRECTION);
				target.sendPacket(SystemMessageId.REJECT_RESURRECTION);
				
				if (simultaneously)
				{
					setIsCastingSimultaneouslyNow(false);
				}
				else
				{
					setIsCastingNow(false);
				}
				
				if (isPlayer())
				{
					sendActionFailed();
					getAI().setIntention(ACTIVE);
				}
				return;
			}
		}
		
		if (skill.getHitTime() > 100)
		{
			getAI().clientStopMoving(null);
		}
		
		final var isOldCast = Config.ENABLE_OLD_CAST;
		final boolean effectWhileCasting = (skill.getSkillType() == SkillType.FUSION) || (skill.getSkillType() == SkillType.SIGNET_CASTTIME);
		final int magicId = skill.getId();
		int hitTime = (effectWhileCasting || skill.isStatic()) ? skill.getHitTime() : Formulas.calcAtkSpd(this, skill, skill.getHitTime());
		int coolTime = effectWhileCasting || skill.getCoolTime() <= 0 ? skill.getCoolTime() : Formulas.calcAtkSpd(this, skill, skill.getCoolTime());
		int skillInterruptTime = skill.isMagic() || skill.isStatic() ? Formulas.calcAtkSpd(this, skill, skill.getSkillInterruptTime()) : 0;
		
		if (!isOldCast && !effectWhileCasting && !skill.isStatic())
		{
			if ((skill.getHitTime() >= Config.MIN_HIT_TIME) && (hitTime < Config.MIN_HIT_TIME))
			{
				hitTime = Config.MIN_HIT_TIME;
			}
		}
		
		if ((!skill.isStatic() && !effectWhileCasting && !skill.isHealingPotionSkill()) && (skill.getHitTime() > 0))
		{
			if (isOldCast)
			{
				final int minHitTime = Math.min(Config.MIN_HIT_TIME, skill.getHitTime());
				if (hitTime < minHitTime)
				{
					hitTime = minHitTime;
					skillInterruptTime = 0;
				}
			}
			else
			{
				if ((skill.getHitTime() < Config.MIN_HIT_TIME) && (skill.getHitTime() > 0) && (hitTime < Config.MIN_HIT_TIME))
				{
					hitTime = Config.MIN_HIT_TIME;
				}
			}
		}
		
		_animationEndTime = System.currentTimeMillis() + hitTime;
		
		if (skill.isMagic() && !effectWhileCasting)
		{
			if (isChargedShot(ShotType.SPIRITSHOTS) || isChargedShot(ShotType.BLESSED_SPIRITSHOTS))
			{
				hitTime = (int) (0.70 * hitTime);
				coolTime = (int) (0.70 * coolTime);
				skillInterruptTime = (int) (0.70 * skillInterruptTime);
			}
		}
		
		hitTime = (skill.getFlyType() != null) && (skill.getFlyType() == FlyType.DUMMY) ? skill.getHitTime() : hitTime;
		
		if (isCastingSimultaneouslyNow() && simultaneously)
		{
			ThreadPoolManager.getInstance().schedule(() -> beginCast(skill, simultaneously, target, targets), 100);
			return;
		}
		
		if (simultaneously)
		{
			setIsCastingSimultaneouslyNow(true);
		}
		else
		{
			setIsCastingNow(true);
		}
		
		int reuseDelay;
		if (skill.isStaticReuse() || skill.isStatic())
		{
			reuseDelay = skill.getReuseDelay();
		}
		else
		{
			if (isOldCast)
			{
				reuseDelay = skill.isMagic() ? (int) (calcStat(Stats.MAGIC_REUSE_RATE, skill.getReuseDelay(), null, skill) * 333 / (int) Math.max(getMAtkSpd(), 1)) : (int) (calcStat(Stats.P_REUSE, skill.getReuseDelay(), null, skill) * 330 / (int) Math.max(getMAtkSpd(), 1));
			}
			else
			{
				reuseDelay = skill.isMagic() ? (int) (skill.getReuseDelay() * calcStat(Stats.MAGIC_REUSE_RATE, 1, null, null)) : (int) (skill.getReuseDelay() * calcStat(Stats.P_REUSE, 1, null, null));
			}
		}

		_isCriticalBlowCastingSkill = skill.calcCriticalBlow(this, target);
		final boolean skillMastery = Formulas.calcSkillMastery(this, skill);
		
		if ((!skill.isHandler() || (reuseDelay > 1000)) && !skillMastery)
		{
			addTimeStamp(skill, reuseDelay);
		}
		
		final int initmpcons = getStat().getMpInitialConsume(skill);
		if (initmpcons > 0)
		{
			getStatus().reduceMp(initmpcons);
			sendPacket(makeStatusUpdate(StatusUpdate.CUR_MP));
		}
		
		if (skillMastery)
		{
			reuseDelay = 0;
			
			if (getActingPlayer() != null)
			{
				getActingPlayer().sendPacket(SystemMessageId.SKILL_READY_TO_USE_AGAIN);
			}
		}
		
		if (!skill.isHandler() || reuseDelay > 10)
		{
			disableSkill(skill, reuseDelay);
		}
		
		if (target != this)
		{
			setHeading(Util.calculateHeadingFrom(this, target));
			broadcastPacket(new ExRotation(getObjectId(), getHeading()));
		}
		
		if (isPlayer())
		{
			if (skill.getChargeConsume() > 0)
			{
				getActingPlayer().decreaseCharges(skill.getChargeConsume());
			}
		}
		
		if (effectWhileCasting)
		{
			if (skill.getItemConsumeId() > 0)
			{
				if (!destroyItemByItemId("Consume", skill.getItemConsumeId(), skill.getItemConsume(), null, true))
				{
					sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
					if (simultaneously)
					{
						setIsCastingSimultaneouslyNow(false);
					}
					else
					{
						setIsCastingNow(false);
					}
					
					if (isPlayer())
					{
						getAI().setIntention(ACTIVE);
					}
					return;
				}
			}
			
			if (skill.getMaxSoulConsumeCount() > 0)
			{
				if (isPlayer())
				{
					if (!getActingPlayer().decreaseSouls(skill.getMaxSoulConsumeCount(), skill))
					{
						if (simultaneously)
						{
							setIsCastingSimultaneouslyNow(false);
						}
						else
						{
							setIsCastingNow(false);
						}
						return;
					}
				}
			}
			
			switch (skill.getSkillType())
			{
				case FUSION :
					startFusionSkill(target, skill);
					break;
				default :
					callSkill(skill, targets);
					break;
			}
		}
		
		if (!skill.isToggle())
		{
			broadcastPacket(new MagicSkillUse(this, target, skill.getDisplayId(), skill.getDisplayLevel(), hitTime, reuseDelay));
			if (_flyLoc != null && _isFlyingNow)
			{
				_isFlyingNow = false;
				stopMove(false);
				broadcastPacket(new FlyToLocation(this, _flyLoc, skill.getFlyType()));
			}
		}
		
		if (isPlayer())
		{
			SystemMessage sm = null;
			switch (magicId)
			{
				case 1312 :
				{
					break;
				}
				case 2046 :
				{
					sm = SystemMessage.getSystemMessage(SystemMessageId.SUMMON_A_PET);
					break;
				}
				default :
				{
					sm = SystemMessage.getSystemMessage(SystemMessageId.USE_S1);
					sm.addSkillName(skill);
				}
			}
			sendPacket(sm);
		}
		
		if (isPlayable())
		{
			if (!effectWhileCasting && (skill.getItemConsumeId() > 0))
			{
				if (!destroyItemByItemId("Consume", skill.getItemConsumeId(), skill.getItemConsume(), null, true))
				{
					getActingPlayer().sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
					abortCast();
					return;
				}
			}
		}
		
		for (final int negateSkillId : skill.getNegateCasterId())
		{
			if (negateSkillId != 0)
			{
				stopSkillEffects(negateSkillId);
			}
		}
		
		_castingSkill = skill;
		_castInterruptTime = System.currentTimeMillis() + skillInterruptTime;
		setCastingTarget(target);
		
		final MagicUseTask mut = new MagicUseTask(this, targets, skill, hitTime, coolTime, simultaneously);
		
		if (isPlayer() && ((effectWhileCasting && (skill.getSkillType() == SkillType.SIGNET_CASTTIME)) || !effectWhileCasting) && !skill.isToggle() && hitTime >= 100)
		{
			sendPacket(new SetupGauge(this, SetupGauge.BLUE, hitTime));
		}
		
		if (!skill.isToggle() && (isOldCast ? hitTime > 0 : hitTime > 410))
		{
			int delay = 0;
			if (isOldCast)
			{
				delay = skillInterruptTime > 0 ? Math.max(0, hitTime - skillInterruptTime) : 0;
				if (skill.getHitCounts() > 0)
				{
					delay = (delay * skill.getHitTimings()[0]) / 100;
				}
				mut.setDelay(delay);
			}
			else
			{
				if (skill.getHitCounts() > 0)
				{
					hitTime = (hitTime * skill.getHitTimings()[0]) / 100;
				}
				delay = hitTime - 400;
			}
			
			if (effectWhileCasting)
			{
				mut.setPhase(3);
			}
			
			if (simultaneously)
			{
				final var future = _skillCast2;
				if (future != null)
				{
					future.cancel(true);
					_skillCast2 = null;
				}
				_skillCast2 = ThreadPoolManager.getInstance().schedule(mut, delay);
			}
			else
			{
				final var future = _skillCast;
				if (future != null)
				{
					future.cancel(true);
					_skillCast = null;
				}
				_skillCast = ThreadPoolManager.getInstance().schedule(mut, delay);
			}
			
			_skillGeoCheckTask = null;
			if (!skill.isDisableGeoCheck() && (skill.getCastRange() > 0 || skill.getEffectRange() > 0) && mut.getHitTime() > 550)
			{
				_skillGeoCheckTask = ThreadPoolManager.getInstance().schedule(new MagicGeoCheckTask(this), (long) (mut.getHitTime() * 0.5));
			}
		}
		else
		{
			mut.setHitTime(0);
			onMagicLaunchedTimer(mut);
		}
	}
	
	public boolean checkDoCastConditions(Skill skill, boolean msg)
	{
		if ((skill == null) || isSkillDisabled(skill) || isSkillBlocked(skill) || (((skill.getFlyRadius() > 0) || skill.getFlyType() != FlyType.NONE) && isMovementDisabled() && skill.getId() != 628 && skill.getId() != 821))
		{
			sendActionFailed();
			return false;
		}
		
		if (isPlayer() && isInsideZone(ZoneId.FUN_PVP))
		{
			final FunPvpZone zone = ZoneManager.getInstance().getZone(this, FunPvpZone.class);
			if (zone != null && !zone.checkSkill(skill))
			{
				if (msg)
				{
					sendMessage("You cannot use " + skill.getName(getActingPlayer().getLang()) + " inside this zone.");
				}
				return false;
			}
		}
		
		if ((getTarget() != null) && (skill.getFlyType() == FlyType.CHARGE) && (getDistanceSq(getTarget()) < 200))
		{
			if (msg)
			{
				sendPacket(SystemMessageId.NOT_ENOUGH_SPACE_FOR_SKILL);
			}
			return false;
		}
		
		final var mpConsume = getStat().getMpConsume(skill)[0];
		if (getCurrentMp() < (mpConsume + getStat().getMpInitialConsume(skill)))
		{
			if (msg)
			{
				sendPacket(SystemMessageId.NOT_ENOUGH_MP);
			}
			sendActionFailed();
			return false;
		}
		
		if (getCurrentHp() <= skill.getHpConsume())
		{
			if (msg)
			{
				sendPacket(SystemMessageId.NOT_ENOUGH_HP);
			}
			sendActionFailed();
			return false;
		}
		
		if (!skill.isStatic())
		{
			if (skill.isMagic())
			{
				if (isMuted())
				{
					sendActionFailed();
					return false;
				}
			}
			else
			{
				if (isPhysicalMuted())
				{
					sendActionFailed();
					return false;
				}
			}
		}
		
		switch (skill.getSkillType())
		{
			case SIGNET :
			case SIGNET_CASTTIME :
				boolean canCast = true;
				if ((skill.getTargetType() == TargetType.GROUND) && isPlayer())
				{
					final Location wp = getActingPlayer().getCurrentSkillWorldPosition();
					if (!checkEffectRangeInsidePeaceZone(skill, wp.getX(), wp.getY(), wp.getZ()))
					{
						canCast = false;
					}
				}
				else if (!checkEffectRangeInsidePeaceZone(skill, getX(), getY(), getZ()))
				{
					canCast = false;
				}
				if (!canCast && !isInFightEvent())
				{
					if (msg)
					{
						final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
						sm.addSkillName(skill);
						sendPacket(sm);
					}
					return false;
				}
				break;
		}
		
		if (getActiveWeaponItem() != null && !isGM())
		{
			final Weapon wep = getActiveWeaponItem();
			if (wep != null && wep.useWeaponSkillsOnly() && wep.hasSkills())
			{
				boolean found = false;
				for (final SkillHolder sh : wep.getSkills())
				{
					if (sh.getId() == skill.getId())
					{
						found = true;
					}
				}
				
				if (!found)
				{
					if (getActingPlayer() != null && !getActingPlayer().isCombatFlagEquipped())
					{
						if (msg)
						{
							sendPacket(SystemMessageId.WEAPON_CAN_USE_ONLY_WEAPON_SKILL);
						}
					}
					return false;
				}
			}
		}
		
		if ((skill.getItemConsumeId() > 0) && (getInventory() != null))
		{
			final ItemInstance requiredItems = getInventory().getItemByItemId(skill.getItemConsumeId());
			
			if ((requiredItems == null) || (requiredItems.getCount() < skill.getItemConsume()))
			{
				if (skill.getSkillType() == SkillType.SUMMON)
				{
					if (msg)
					{
						final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.SUMMONING_SERVITOR_COSTS_S2_S1);
						sm.addItemName(skill.getItemConsumeId());
						sm.addNumber(skill.getItemConsume());
						sendPacket(sm);
					}
				}
				else
				{
					if (msg)
					{
						sendPacket(SystemMessageId.THERE_ARE_NOT_ENOUGH_NECESSARY_ITEMS_TO_USE_THE_SKILL);
					}
				}
				return false;
			}
		}
		return true;
	}
	
	public void addTimeStampItem(ItemInstance item, long reuse, boolean byCron)
	{
	}
	
	public long getItemRemainingReuseTime(int itemObjId)
	{
		return -1;
	}
	
	public void addTimeStamp(Skill skill, long reuse)
	{
	}
	
	public void addTimeStamp(Skill skill, long reuse, long systime)
	{
	}
	
	public long getSkillRemainingReuseTime(int skillReuseHashId)
	{
		return -1;
	}
	
	public void startFusionSkill(Creature target, Skill skill)
	{
		if (skill.getSkillType() != SkillType.FUSION)
		{
			return;
		}
		
		if (_fusionSkill == null)
		{
			_fusionSkill = new FusionSkill(this, target, skill);
		}
	}
	
	@Override
	public void doDie(Creature killer)
	{
		synchronized (this)
		{
			if (_isDead)
			{
				return;
			}
			getStatus().stopHpMpRegeneration();
			setCurrentHp(0, true, true);
			setIsDead(true);
		}
		onDeath(killer);
	}
	
	protected void onDeath(Creature killer)
	{
		setTarget(null);
		stopMove(null);
		
		if (killer != null)
		{
			if (killer.isPlayer())
			{
				killer.getListeners().onKillIgnorePetOrSummon(this);
			}
			else
			{
				killer.getListeners().onKill(this);
			}
		}
		
		if (!isPlayable())
		{
			stopAllEffectsExceptThoseThatLastThroughDeath();
			calculateRewards(killer);
		}
		getListeners().onDeath(killer);
		broadcastStatusUpdate();
		onDeathInZones(this);
		
		if (!isPlayable())
		{
			if (getAI() != null)
			{
				getAI().stopAllTaskAndTimers();
			}
			ThreadPoolManager.getInstance().schedule(new NotifyAITask(this, CtrlEvent.EVT_DEAD, killer), 100);
		}
		
		getAttackByList().clear();
		
		try
		{
			if (_fusionSkill != null)
			{
				abortCast();
			}
			
			for (final Creature character : World.getInstance().getAroundCharacters(this))
			{
				if ((character.getFusionSkill() != null) && (character.getFusionSkill().getTarget() == this))
				{
					character.abortCast();
				}
			}
		}
		catch (final Exception e)
		{}
	}
	
	@Override
	protected void onDelete()
	{
		GameObjectsStorage.remove(this);
		setDebug(null);
		if (hasAI())
		{
			getAI().stopAllTaskAndTimers();
			getAI().stopAITask();
			detachAI();
		}
		super.onDelete();
	}

	public void detachAI()
	{
		if (isWalker())
		{
			return;
		}
		setAI(null);
	}
	
	protected void calculateRewards(Creature killer)
	{
	}
	
	public void doRevive()
	{
		if (!isDead())
		{
			return;
		}
		if (!isTeleporting())
		{
			setIsPendingRevive(false);
			setIsDead(false);
			boolean restorefull = false;
			
			if (isPlayable() && ((Playable) this).isPhoenixBlessed() || (isPlayer() && getActingPlayer().isInFightEvent()))
			{
				restorefull = true;
				stopEffects(EffectType.PHOENIX_BLESSING);
			}
			if (restorefull)
			{
				_status.setCurrentCp(getCurrentCp());
				_status.setCurrentHp(getMaxHp());
				_status.setCurrentMp(getMaxMp());
			}
			else
			{
				if ((Config.RESPAWN_RESTORE_CP > 0) && (getCurrentCp() < (getMaxCp() * Config.RESPAWN_RESTORE_CP)))
				{
					_status.setCurrentCp(getMaxCp() * Config.RESPAWN_RESTORE_CP);
				}
				if ((Config.RESPAWN_RESTORE_HP > 0) && (getCurrentHp() < (getMaxHp() * Config.RESPAWN_RESTORE_HP)))
				{
					_status.setCurrentHp(getMaxHp() * Config.RESPAWN_RESTORE_HP);
				}
				if ((Config.RESPAWN_RESTORE_MP > 0) && (getCurrentMp() < (getMaxMp() * Config.RESPAWN_RESTORE_MP)))
				{
					_status.setCurrentMp(getMaxMp() * Config.RESPAWN_RESTORE_MP);
				}
			}
			broadcastPacket(new Revive(this));
			onReviveInZones(this);
		}
		else
		{
			setIsPendingRevive(true);
		}
	}
	
	public void doRevive(double revivePower)
	{
		doRevive();
	}
	
	public CharacterAI getAI()
	{
		if (_ai == null)
		{
			synchronized (this)
			{
				if (_ai == null)
				{
					return _ai = initAI();
				}
			}
		}
		return _ai;
	}
	
	protected CharacterAI initAI()
	{
		return new CharacterAI(this);
	}
	
	public void setAI(CharacterAI newAI)
	{
		final CharacterAI oldAI = _ai;
		
		if ((oldAI != null) && (oldAI != newAI) && (oldAI instanceof DefaultAI))
		{
			oldAI.stopAITask();
		}
		_ai = newAI;
	}
	
	public boolean hasAI()
	{
		return _ai != null;
	}
	
	public boolean isRaid()
	{
		return false;
	}

	public boolean isEpicRaid()
	{
		return false;
	}
	
	public boolean isSiegeGuard()
	{
		return false;
	}
	
	public boolean isRaidMinion()
	{
		return false;
	}
	
	public final Set<Creature> getAttackByList()
	{
		if (_attackByList == null)
		{
			synchronized (this)
			{
				if (_attackByList == null)
				{
					_attackByList = ConcurrentHashMap.newKeySet();
				}
			}
		}
		return _attackByList;
	}
	
	public boolean isNoRndWalk()
	{
		return _isNoRndWalk;
	}
	
	public final void setIsNoRndWalk(boolean value)
	{
		_isNoRndWalk = value;
	}
	
	public final boolean isAfraid()
	{
		return isAffected(EffectFlag.FEAR);
	}
	
	public final boolean isAllSkillsDisabled()
	{
		return _allSkillsDisabled || isStunned() || isSleeping() || isParalyzed();
	}
	
	public boolean isAttackingDisabled()
	{
		return isStunned() || isSleeping() || isAttackingNow() || isAlikeDead() || isParalyzed() || isPhysicalAttackMuted() || isCoreAIDisabled();
	}
	
	public final Calculator[] getCalculators()
	{
		return _calculators;
	}
	
	public final boolean isConfused()
	{
		return isAffected(EffectFlag.CONFUSED);
	}
	
	public boolean isAlikeDead()
	{
		return _isDead;
	}
	
	public boolean isDead()
	{
		return _isDead;
	}
	
	public final void setIsDead(boolean value)
	{
		_isDead = value;
	}
	
	public boolean isImmobilized()
	{
		return _isImmobilized;
	}
	
	public void setIsImmobilized(boolean value)
	{
		_isImmobilized = value;
	}
	
	public final boolean isMuted()
	{
		return isAffected(EffectFlag.MUTED);
	}
	
	public final boolean isPhysicalMuted()
	{
		return isAffected(EffectFlag.PSYCHICAL_MUTED);
	}
	
	public final boolean isPhysicalAttackMuted()
	{
		return isAffected(EffectFlag.PSYCHICAL_ATTACK_MUTED);
	}
	
	public boolean isMovementDisabled()
	{
		return isBlocked() || isStunned() || isRooted() || isSleeping() || isOverloaded() || isParalyzed() || isImmobilized() || isAlikeDead() || isTeleporting();
	}
	
	public boolean isActionsDisabled()
	{
		if (Config.CHECK_ATTACK_STATUS_TO_MOVE)
		{
			if (isAttackingNow())
			{
				return true;
			}
		}
		return isBlocked() || isAlikeDead() || isStunned() || isSleeping() || isParalyzed() || isCastingNow();
	}
	
	public final boolean isOutOfControl()
	{
		return isBlocked() || isConfused() || isAfraid();
	}
	
	public final boolean isOverloaded()
	{
		return _isOverloaded;
	}
	
	public final void setIsOverloaded(boolean value)
	{
		_isOverloaded = value;
	}
	
	public final boolean isParalyzed()
	{
		return _isParalyzed || isAffected(EffectFlag.PARALYZED);
	}
	
	public final void setIsParalyzed(boolean value)
	{
		_isParalyzed = value;
	}
	
	public final boolean isPendingRevive()
	{
		return _isDead && _isPendingRevive;
	}
	
	public final void setIsPendingRevive(boolean value)
	{
		_isPendingRevive = value;
	}
	
	public final boolean isDisarmed()
	{
		return isAffected(EffectFlag.DISARMED);
	}
	
	public Summon getSummon()
	{
		return null;
	}
	
	public final boolean hasSummon()
	{
		return getSummon() != null;
	}
	
	public final boolean hasPet()
	{
		return hasSummon() && getSummon().isPet();
	}
	
	public final boolean hasServitor()
	{
		return hasSummon() && getSummon().isServitor();
	}
	
	public void startRooted(boolean rooted)
	{
		_isRooted = rooted;
		if (_isRooted)
		{
			stopMove(null);
			startAbnormalEffect(AbnormalEffect.ROOT);
		}
		else
		{
			stopAbnormalEffect(AbnormalEffect.ROOT);
		}
	}

	public final boolean isRooted()
	{
		return isAffected(EffectFlag.ROOTED) || _isRooted;
	}
	
	public boolean isRunning()
	{
		return _isRunning;
	}
	
	public final void setIsRunning(boolean value)
	{
		_isRunning = value;
		if (getRunSpeed() != 0)
		{
			broadcastPacket(new ChangeMoveType(this));
		}
		if (isPlayer())
		{
			getActingPlayer().broadcastUserInfo(true);
		}
		else if (isSummon())
		{
			broadcastStatusUpdate();
		}
		else if (isNpc())
		{
			broadcastInfo();
		}
	}
	
	public final void setRunning()
	{
		if (!isRunning())
		{
			setIsRunning(true);
		}
	}
	
	public final boolean isSleeping()
	{
		return isAffected(EffectFlag.SLEEP);
	}
	
	public final boolean isStunned()
	{
		return _isStunned;
	}
	
	public final boolean isBetrayed()
	{
		return isAffected(EffectFlag.BETRAYED);
	}
	
	public final boolean isFearing()
	{
		return isAffected(EffectFlag.FEAR);
	}
	
	public final boolean isTeleporting()
	{
		return _isTeleporting;
	}
	
	public void setIsTeleporting(boolean value)
	{
		_isTeleporting = value;
	}
	
	public void setIsInvul(boolean b)
	{
		_isInvul = b;
	}
	
	public boolean isInvul()
	{
		return _isInvul || isTeleporting() || isDamageBlock();
	}
	
	public boolean isOnlyInvul()
	{
		return _isInvul;
	}
	
	public void setUndying(boolean undying)
	{
		_isUndying = undying;
	}
	
	public boolean isUndying()
	{
		return _isUndying || isInvul();
	}
	
	public void setIsDamageBlock(boolean b)
	{
		_isDamageBlock = b;
	}
	
	public boolean isDamageBlock()
	{
		return _isDamageBlock;
	}
	
	public void setIsMortal(boolean b)
	{
		_isMortal = b;
	}
	
	public boolean isMortal()
	{
		return _isMortal;
	}
	
	public boolean isUndead()
	{
		return false;
	}
	
	public boolean isResurrectionBlocked()
	{
		return isAffected(EffectFlag.BLOCK_RESURRECTION);
	}
	
	@Override
	public boolean isInWater(GameObject object)
	{
		return object.isInsideZone(ZoneId.WATER) && !object.isFlying();
	}
	
	public boolean isInVehicle()
	{
		return isPlayer() && getActingPlayer().isInVehicle();
	}
	
	@Override
	public final boolean isFlying()
	{
		return _isFlying;
	}
	
	public final void setIsFlying(boolean mode)
	{
		_isFlying = mode;
	}
	
	public CharStat getStat()
	{
		return _stat;
	}
	
	public void initCharStat()
	{
		_stat = new CharStat(this);
	}
	
	public final void setStat(CharStat value)
	{
		_stat = value;
	}
	
	public CharStatus getStatus()
	{
		return _status;
	}
	
	public void initCharStatus()
	{
		_status = new CharStatus(this);
	}
	
	public final void setStatus(CharStatus value)
	{
		_status = value;
	}
	
	public CharTemplate getTemplate()
	{
		return _template;
	}
	
	protected final void setTemplate(CharTemplate template)
	{
		_template = template;
	}
	
	public final void setWalking()
	{
		if (isRunning())
		{
			setIsRunning(false);
		}
	}
	
	protected CharEffectList _effects = new CharEffectList(this);
	
	public final CharEffectList getEffectList()
	{
		return _effects;
	}
	
	public void addEffect(Effect newEffect)
	{
		_effects.queueEffect(newEffect, false, true);
	}
	
	public void removeEffect(Effect effect, boolean printMessage)
	{
		_effects.queueEffect(effect, true, printMessage);
	}
	
	public final void startAbnormalEffect(AbnormalEffect ae)
	{
		if (ae == AbnormalEffect.NONE)
		{
			return;
		}
		_abnormalEffects.add(ae);
		if (ae.isSpecial())
		{
			_abnormalEffectsMask2 |= ae.getMask();
		}
		else if (ae.isEvent())
		{
			_abnormalEffectsMask3 |= ae.getMask();
		}
		else
		{
			_abnormalEffectsMask |= ae.getMask();
		}
		updateAbnormalEffect();
	}
	
	protected void stoAllAbnormalEffects()
	{
		_abnormalEffects.clear();
		_abnormalEffectsMask = 0;
		_abnormalEffectsMask2 = 0;
		_abnormalEffectsMask3 = 0;
		updateAbnormalEffect();
	}
	
	public Set<AbnormalEffect> getAbnormalEffects()
	{
		return _abnormalEffects;
	}
	
	public AbnormalEffect[] getAbnormalEffectsArray()
	{
		return _abnormalEffects.toArray(new AbnormalEffect[_abnormalEffects.size()]);
	}
	
	public int getAbnormalEffectMask()
	{
		int ae = _abnormalEffectsMask;
		if (!isFlying() && isStunned())
		{
			if (isIsDanceStun())
			{
				ae |= AbnormalEffect.DANCE_STUNNED.getMask();
			}
			else
			{
				ae |= AbnormalEffect.STUN.getMask();
			}
		}
		if (!isFlying() && isRooted())
		{
			ae |= AbnormalEffect.ROOT.getMask();
		}
		if (isSleeping())
		{
			ae |= AbnormalEffect.SLEEP.getMask();
		}
		if (isConfused())
		{
			ae |= AbnormalEffect.FEAR.getMask();
		}
		if (isMuted())
		{
			ae |= AbnormalEffect.MUTED.getMask();
		}
		if (isPhysicalMuted())
		{
			ae |= AbnormalEffect.MUTED.getMask();
		}
		if (isAfraid())
		{
			ae |= AbnormalEffect.SKULL_FEAR.getMask();
		}
		return ae;
	}
	
	public int getAbnormalEffectMask2()
	{
		int se = _abnormalEffectsMask2;
		if (isFlying() && isStunned())
		{
			se |= AbnormalEffect.S_AIR_STUN.getMask();
		}
		if (isFlying() && isRooted())
		{
			se |= AbnormalEffect.S_AIR_ROOT.getMask();
		}
		return se;
	}
	
	public int getAbnormalEffectMask3()
	{
		return _abnormalEffectsMask3;
	}
	
	public void stopAbnormalEffect(AbnormalEffect ae)
	{
		_abnormalEffects.remove(ae);
		
		if (ae.isSpecial())
		{
			_abnormalEffectsMask2 &= ~ae.getMask();
		}
		if (ae.isEvent())
		{
			_abnormalEffectsMask3 &= ~ae.getMask();
		}
		else
		{
			_abnormalEffectsMask &= ~ae.getMask();
		}
		
		updateAbnormalEffect();
	}
	
	public final void startFakeDeath()
	{
		if (!isPlayer())
		{
			return;
		}
		
		abortAttack();
		abortCast();
		stopMove(null);
		getAI().notifyEvent(CtrlEvent.EVT_FAKE_DEATH);
		broadcastPacket(new ChangeWaitType(this, ChangeWaitType.WT_START_FAKEDEATH));
		_isFakeDeath = true;
	}
	
	public final void setIsStuned(boolean isStuned)
	{
		_isStunned = isStuned;
		if (!isStuned && !isPlayer())
		{
			getAI().notifyEvent(CtrlEvent.EVT_THINK);
		}
		updateAbnormalEffect();
	}

	public final void startConfused()
	{
		getAI().notifyEvent(CtrlEvent.EVT_CONFUSED);
		updateAbnormalEffect();
	}
	
	public final void stopConfused()
	{
		if (!isPlayer())
		{
			getAI().notifyEvent(CtrlEvent.EVT_THINK);
		}
		updateAbnormalEffect();
	}
	
	public final void startParalyze()
	{
		abortAttack();
		abortCast();
		stopMove(null);
		getAI().notifyEvent(CtrlEvent.EVT_PARALYZED);
	}
	
	public void stopAllEffects()
	{
		_effects.stopAllEffects();
	}
	
	public void stopAllEffects(boolean isAll)
	{
		_effects.stopAllEffects(isAll);
	}
	
	public void stopAllEffectsExceptThoseThatLastThroughDeath()
	{
		_effects.stopAllEffectsExceptThoseThatLastThroughDeath();
	}
	
	public void stopSkillEffects(int skillId)
	{
		_effects.stopSkillEffects(skillId);
	}
	
	public final void stopEffects(EffectType type)
	{
		_effects.stopEffects(type);
	}
	
	public final void stopEffectsOnAction()
	{
		_effects.stopEffectsOnAction();
	}
	
	public final void stopEffectsOnDamage(boolean awake)
	{
		_effects.stopEffectsOnDamage(awake);
	}
	
	public final void stopFakeDeath(boolean removeEffects)
	{
		if (removeEffects)
		{
			stopEffects(EffectType.FAKE_DEATH);
		}
		
		if (isPlayer())
		{
			getActingPlayer().setIsFakeDeath(false);
			getActingPlayer().setRecentFakeDeath(true);
		}
		broadcastPacket(new ChangeWaitType(this, ChangeWaitType.WT_STOP_FAKEDEATH));
		broadcastPacket(new Revive(this));
		_isFakeDeath = false;
	}
	
	public boolean isFakeDeathNow()
	{
		return _isFakeDeath;
	}
	
	public final void stopTransformation(boolean removeEffects)
	{
		if (removeEffects)
		{
			stopEffects(EffectType.TRANSFORMATION);
		}
		
		if (isPlayer())
		{
			if (getActingPlayer().getTransformation() != null)
			{
				getActingPlayer().untransform();
			}
		}
		
		if (!isPlayer())
		{
			getAI().notifyEvent(CtrlEvent.EVT_THINK);
		}
		updateAbnormalEffect();
	}
	
	public final void startFear()
	{
		abortCast();
		getAI().setIntention(CtrlIntention.ACTIVE);
		stopMove(null);
		updateAbnormalEffect();
	}
	
	public final void stopFear(boolean removeEffects)
	{
		if (removeEffects)
		{
			stopEffects(EffectType.FEAR);
		}
		updateAbnormalEffect();
	}
	
	public abstract void updateAbnormalEffect();
	
	public void updateEffectIcons()
	{
		updateEffectIcons(false);
	}
	
	public void updateEffectIcons(boolean partyOnly)
	{
	}
	
	public final Effect[] getAllEffects()
	{
		return _effects.getAllEffects();
	}

	public final Effect getFirstEffect(int skillId)
	{
		return _effects.getFirstEffect(skillId);
	}
	
	public final Effect getFirstEffect(Skill skill)
	{
		return _effects.getFirstEffect(skill);
	}
	
	public final Effect getFirstEffect(EffectType tp)
	{
		return _effects.getFirstEffect(tp);
	}
	
	public final Effect getFirstPassiveEffect(EffectType type)
	{
		return _effects.getFirstPassiveEffect(type);
	}
	
	public final Effect getFirstAbnormalType(Skill skill)
	{
		return _effects.getFirstAbnormalType(skill);
	}
	
	public final void addStatFunc(Func f)
	{
		if (f == null)
		{
			return;
		}
		
		synchronized (this)
		{
			if (_calculators == NPC_STD_CALCULATOR)
			{
				_calculators = new Calculator[Stats.NUM_STATS];
				
				for (int i = 0; i < Stats.NUM_STATS; i++)
				{
					if (NPC_STD_CALCULATOR[i] != null)
					{
						_calculators[i] = new Calculator(NPC_STD_CALCULATOR[i]);
					}
				}
			}
			final int stat = f.stat.ordinal();
			
			if (_calculators[stat] == null)
			{
				_calculators[stat] = new Calculator();
			}
			
			_calculators[stat].addFunc(f);
		}
	}
	
	public final void addStatFuncs(Func[] funcs)
	{
		if (!isPlayer() && World.getInstance().getAroundPlayers(this).isEmpty())
		{
			for (final Func f : funcs)
			{
				addStatFunc(f);
			}
		}
		else
		{
			final List<Stats> modifiedStats = new ArrayList<>();
			for (final Func f : funcs)
			{
				modifiedStats.add(f.stat);
				addStatFunc(f);
			}
			broadcastModifiedStats(modifiedStats);
		}
	}
	
	public final void removeStatFunc(Func f)
	{
		if (f == null)
		{
			return;
		}
		
		final int stat = f.stat.ordinal();
		
		synchronized (this)
		{
			if (_calculators[stat] == null)
			{
				return;
			}
			
			_calculators[stat].removeFunc(f);
			
			if (_calculators[stat].size() == 0)
			{
				_calculators[stat] = null;
			}
			
			if (isNpc())
			{
				int i = 0;
				for (; i < Stats.NUM_STATS; i++)
				{
					if (!Calculator.equalsCals(_calculators[i], NPC_STD_CALCULATOR[i]))
					{
						break;
					}
				}
				
				if (i >= Stats.NUM_STATS)
				{
					_calculators = NPC_STD_CALCULATOR;
				}
			}
		}
	}
	
	public final void removeStatFuncs(Func[] funcs)
	{
		if (!isPlayer() && World.getInstance().getAroundPlayers(this).isEmpty())
		{
			for (final Func f : funcs)
			{
				removeStatFunc(f);
			}
		}
		else
		{
			final List<Stats> modifiedStats = new ArrayList<>();
			for (final Func f : funcs)
			{
				modifiedStats.add(f.stat);
				removeStatFunc(f);
			}
			broadcastModifiedStats(modifiedStats);
		}
	}
	
	public final void removeStatsOwner(Object owner)
	{
		List<Stats> modifiedStats = null;
		
		int i = 0;
		synchronized (_calculators)
		{
			for (final Calculator calc : _calculators)
			{
				if (calc != null)
				{
					if (modifiedStats != null)
					{
						modifiedStats.addAll(calc.removeOwner(owner));
					}
					else
					{
						modifiedStats = calc.removeOwner(owner);
					}
					
					if (calc.size() == 0)
					{
						_calculators[i] = null;
					}
				}
				i++;
			}
			
			if (isNpc())
			{
				i = 0;
				for (; i < Stats.NUM_STATS; i++)
				{
					if (!Calculator.equalsCals(_calculators[i], NPC_STD_CALCULATOR[i]))
					{
						break;
					}
				}
				
				if (i >= Stats.NUM_STATS)
				{
					_calculators = NPC_STD_CALCULATOR;
				}
			}
			
			if (owner instanceof Effect)
			{
				if (!((Effect) owner)._preventExitUpdate)
				{
					broadcastModifiedStats(modifiedStats);
				}
			}
			else
			{
				broadcastModifiedStats(modifiedStats);
			}
		}
	}
	
	protected void broadcastModifiedStats(List<Stats> stats)
	{
		if ((stats == null) || stats.isEmpty())
		{
			return;
		}
		
		if (isSummon())
		{
			final Summon summon = (Summon) this;
			if (summon.getOwner() != null)
			{
				summon.updateAndBroadcastStatus(1);
			}
		}
		else
		{
			boolean broadcastFull = false;
			final StatusUpdate su = new StatusUpdate(this);
			
			for (final Stats stat : stats)
			{
				if (stat == Stats.POWER_ATTACK_SPEED)
				{
					su.addAttribute(StatusUpdate.ATK_SPD, (int) getPAtkSpd());
				}
				else if (stat == Stats.MAGIC_ATTACK_SPEED)
				{
					su.addAttribute(StatusUpdate.CAST_SPD, getMAtkSpd());
				}
				else if (stat == Stats.MOVE_SPEED)
				{
					broadcastFull = true;
				}
			}
			
			if (isPlayer())
			{
				if (broadcastFull)
				{
					getActingPlayer().updateAndBroadcastStatus(2);
				}
				else
				{
					getActingPlayer().updateAndBroadcastStatus(1);
					if (su.hasAttributes())
					{
						broadcastPacket(su);
					}
				}
				if ((getSummon() != null) && isAffected(EffectFlag.SERVITOR_SHARE))
				{
					getSummon().broadcastStatusUpdate();
				}
			}
			else if (isNpc())
			{
				if (broadcastFull)
				{
					broadcastInfo();
				}
				else if (su.hasAttributes())
				{
					broadcastPacket(su);
				}
			}
			else if (su.hasAttributes())
			{
				broadcastPacket(su);
			}
		}
	}
	
	public boolean isInCombat()
	{
		return hasAI() && ((getAI().getAttackTarget() != null) || getAI().isAutoAttacking());
	}
	
	public final boolean isCastingNow()
	{
		return _isCastingNow;
	}
	
	public void setIsCastingNow(boolean value)
	{
		_isCastingNow = value;
	}
	
	public final boolean isCastingSimultaneouslyNow()
	{
		return _isCastingSimultaneouslyNow;
	}
	
	public void setIsCastingSimultaneouslyNow(boolean value)
	{
		_isCastingSimultaneouslyNow = value;
	}
	
	public final boolean canAbortCast()
	{
		return _castInterruptTime > System.currentTimeMillis();
	}
	
	public boolean isAttackingNow()
	{
		return _attackEndTime > System.nanoTime();
	}
	
	public final void abortAttack()
	{
		if (isAttackingNow())
		{
			sendActionFailed();
		}
	}
	
	public void abortCast()
	{
		if (isCastingNow() || isCastingSimultaneouslyNow())
		{
			var future = _skillCast;
			if (future != null)
			{
				future.cancel(true);
				_skillCast = null;
			}
			
			future = _skillCast2;
			if (future != null)
			{
				future.cancel(true);
				_skillCast2 = null;
			}
			
			future = _skillGeoCheckTask;
			if (future != null)
			{
				future.cancel(false);
				_skillGeoCheckTask = null;
			}
			
			finishFly();
			
			if (getFusionSkill() != null)
			{
				getFusionSkill().onCastAbort();
			}
			
			final Effect mog = getFirstEffect(EffectType.SIGNET_GROUND);
			if (mog != null)
			{
				mog.exit();
			}
			
			if (_allSkillsDisabled)
			{
				enableAllSkills();
			}
			setIsCastingNow(false);
			setIsCastingSimultaneouslyNow(false);
			_animationEndTime = 0;
			_castInterruptTime = 0;
			_castingSkill = null;
			_isCriticalBlowCastingSkill = false;
			_direction = TargetDirection.NONE;
			if (isPlayer())
			{
				getAI().notifyEvent(CtrlEvent.EVT_FINISH_CASTING);
			}
			
			broadcastPacket(new MagicSkillCanceled(getObjectId()));
			sendActionFailed();
		}
	}
	
	public void revalidateZone(boolean force)
	{
		if (isTeleporting())
		{
			return;
		}
		
		if (force || _zoneValidateCounter > 4)
		{
			_zoneValidateCounter = 0;
			final List<ZoneType> currentZone = ZoneManager.getInstance().getZones(getX(), getY(), getZ());
			List<ZoneType> newZones = null;
			List<ZoneType> oldZones = null;
			
			_zoneLock.lock();
			try
			{
				if (_zoneList == null)
				{
					newZones = currentZone;
				}
				else
				{
					if (currentZone != null)
					{
						for (final ZoneType zone : currentZone)
						{
							if (!_zoneList.contains(zone))
							{
								if (newZones == null)
								{
									newZones = new ArrayList<>();
								}
								newZones.add(zone);
							}
						}
					}
					
					if (_zoneList.size() > 0)
					{
						for(final ZoneType zone : _zoneList)
						{
							if(currentZone == null || !currentZone.contains(zone))
							{
								if(oldZones == null)
								{
									oldZones = new ArrayList<>();
								}
								oldZones.add(zone);
							}
						}
					}
				}
				
				if (currentZone != null && currentZone.size() > 0)
				{
					_zoneList = currentZone;
				}
				else
				{
					_zoneList = null;
				}
			}
			finally
			{
				_zoneLock.unlock();
			}
			
			if (oldZones != null)
			{
				for (final ZoneType zone : oldZones)
				{
					if (zone != null)
					{
						zone.removeCharacter(this);
					}
				}
			}
			
			if (newZones != null)
			{
				for (final ZoneType zone : newZones)
				{
					if (zone != null)
					{
						zone.revalidateInZone(this, false);
					}
				}
			}
		}
		else
		{
			_zoneValidateCounter++;
		}
	}
	
	public void clearZones()
	{
		_zoneLock.lock();
		try
		{
			if (_zoneList != null)
			{
				for (final ZoneType zone : _zoneList)
				{
					if (zone != null)
					{
						zone.removeCharacter(this);
					}
				}
			}
			_zoneList = null;
		}
		finally
		{
			_zoneLock.unlock();
		}
	}

	public void setTarget(GameObject object)
	{
		if ((object != null) && !object.isVisible() && !isGM())
		{
			object = null;
		}
		_target = object;
	}
	
	public final int getTargetId()
	{
		final GameObject target = getTarget();
		return target == null ? -1 : target.getObjectId();
	}
	
	public final GameObject getTarget()
	{
		return _target;
	}
	
	@Override
	public double getDistance(int x, int y)
	{
		final double dx = x - getX();
		final double dy = y - getY();
		
		return Math.sqrt((dx * dx) + (dy * dy));
	}
	
	@Override
	public double getDistance(int x, int y, int z)
	{
		final double dx = x - getX();
		final double dy = y - getY();
		final double dz = z - getZ();
		
		return Math.sqrt((dx * dx) + (dy * dy) + (dz * dz));
	}
	
	public final double getDistanceSq(GameObject object)
	{
		return getDistanceSq(object.getX(), object.getY(), object.getZ());
	}
	
	public final double getDistanceSq(Location loc)
	{
		return getDistanceSq(loc.getX(), loc.getY(), loc.getZ());
	}
	
	public final double getDistanceSq(int x, int y, int z)
	{
		final double dx = x - getX();
		final double dy = y - getY();
		final double dz = z - getZ();
		
		return ((dx * dx) + (dy * dy) + (dz * dz));
	}
	
	public final double getPlanDistanceSq(GameObject object)
	{
		return getPlanDistanceSq(object.getX(), object.getY());
	}
	
	public final double getPlanDistanceSq(int x, int y)
	{
		final double dx = x - getX();
		final double dy = y - getY();
		
		return ((dx * dx) + (dy * dy));
	}
	
	public final boolean isInsideRadius(int x, int y, int radius, boolean strictCheck)
	{
		return isInsideRadius(x, y, 0, radius, false, strictCheck);
	}
	
	public final boolean isInsideRadius(GameObject obj, int radius, boolean checkZ, boolean strictCheck)
	{
		return isInsideRadius(obj.getX(), obj.getY(), obj.getZ(), radius, checkZ, strictCheck);
	}
	
	public final boolean isInsideRadius(int x, int y, int z, int radius, boolean checkZ, boolean strictCheck)
	{
		final double distance = calculateDistance(x, y, z, checkZ, true);
		return (strictCheck) ? (distance < (radius * radius)) : (distance <= (radius * radius));
	}
	
	protected boolean checkAndEquipArrows()
	{
		return true;
	}
	
	protected boolean checkAndEquipBolts()
	{
		return true;
	}
	
	public void addExpAndSp(long addToExp, int addToSp)
	{
	}
	
	public abstract ItemInstance getActiveWeaponInstance();
	
	public abstract Weapon getActiveWeaponItem();
	
	public abstract ItemInstance getSecondaryWeaponInstance();
	
	public abstract Item getSecondaryWeaponItem();
	
	public boolean canFinishAttack()
	{
		return !isStunned() && !isSleeping() && !isAlikeDead() && !isParalyzed() && !isPhysicalAttackMuted();
	}
	
	public void onHitTimer(Creature target, int damage, boolean crit, boolean miss, boolean soulshot, byte shld)
	{
		if ((isCastingNow() && getCastingSkill() != null) || !canFinishAttack())
		{
			return;
		}
		
		if ((target == null) || isAlikeDead() || (isNpc() && ((Npc) this).isEventMob()) || (target != null && isPlayer() && !target.isVisibleFor(getActingPlayer())))
		{
			getAI().notifyEvent(CtrlEvent.EVT_CANCEL);
			return;
		}
		
		if ((isNpc() && target.isAlikeDead()) || target.isDead() || (!World.getInstance().getAroundCharacters(this).contains(target) && !isDoor()))
		{
			rechargeShots(true, false);
			getAI().notifyEvent(CtrlEvent.EVT_CANCEL);
			sendActionFailed();
			return;
		}
		
		if (miss)
		{
			if (target.hasAI())
			{
				target.getAI().notifyEvent(CtrlEvent.EVT_EVADED, this);
			}
			
			if (target.getChanceSkills() != null)
			{
				target.getChanceSkills().onEvadedHit(this);
			}
		}
		
		if (!miss)
		{
			Formulas.calcStunBreak(target, crit);
		}
		
		if (target.isUndying())
		{
			damage = (int) (target.getCurrentHp() - damage < 1 ? target.getCurrentHp() - 1 : damage);
		}
		
		target.getListeners().onAttackHit(this);
		sendDamageMessage(target, damage, null, false, crit, miss);
		
		if (!target.isDead() && target.isRaid() && target.giveRaidCurse() && !Config.RAID_DISABLE_CURSE)
		{
			if (getLevel() > (target.getLevel() + 8) && target.getId() != 29054)
			{
				final Skill skill = SkillsParser.FrequentSkill.RAID_CURSE2.getSkill();
				if (skill != null)
				{
					abortAttack();
					abortCast();
					getAI().setIntention(CtrlIntention.IDLE);
					skill.getEffects(target, this, false);
				}
				damage = 0;
			}
		}
		
		if (target.isPlayer() && !miss)
		{
			if (!target.isInvul())
			{
				target.getActingPlayer().getAI().clientStartAutoAttack();
			}
		}
		
		if (!miss && (damage > 0))
		{
			final Weapon weapon = getActiveWeaponItem();
			final boolean isBow = ((weapon != null) && ((weapon.getItemType() == WeaponType.BOW) || (weapon.getItemType() == WeaponType.CROSSBOW)));
			double reflectedDamage = 0;
			
			if (!isBow && !target.isInvul())
			{
				if (!target.isRaid() || (getActingPlayer() == null) || (getActingPlayer().getLevel() <= (target.getLevel() + 8)))
				{
					final double reflectPercent = target.getStat().calcStat(Stats.REFLECT_DAMAGE_PERCENT, 0, null, null);
					
					if (reflectPercent > 0)
					{
						reflectedDamage = (int) ((reflectPercent / 100.) * damage);
						
						if (reflectedDamage > target.getMaxHp())
						{
							reflectedDamage = target.getMaxHp();
						}
					}
				}
			}
			
			if (target.hasAI())
			{
				target.getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, this, damage);
			}
			target.reduceCurrentHp(damage, this, null);
			
			if (reflectedDamage > 0 && !isInvul() && !target.isDead())
			{
				reduceCurrentHp(reflectedDamage, target, true, false, null);
			}
			
			if (!isBow && !target.isInvul() && isCanAbsorbDamage(target))
			{
				double absorbPercent = getStat().calcStat(Stats.ABSORB_DAMAGE_PERCENT, 0, null, null);
				if (absorbPercent > 0)
				{
					final int maxCanAbsorb = (int) (getMaxRecoverableHp() - getCurrentHp());
					int absorbDamage = (int) ((absorbPercent / 100.) * damage);
					
					if (absorbDamage > maxCanAbsorb)
					{
						absorbDamage = maxCanAbsorb;
					}
					
					if (absorbDamage > 0)
					{
						setCurrentHp(getCurrentHp() + absorbDamage);
					}
				}
				
				absorbPercent = getStat().calcStat(Stats.ABSORB_MANA_DAMAGE_PERCENT, 0, null, null);
				if (absorbPercent > 0)
				{
					final int maxCanAbsorb = (int) (getMaxRecoverableMp() - getCurrentMp());
					int absorbDamage = (int) ((absorbPercent / 100.) * damage);
					
					if (absorbDamage > maxCanAbsorb)
					{
						absorbDamage = maxCanAbsorb;
					}
					
					if (absorbDamage > 0)
					{
						setCurrentMp(getCurrentMp() + absorbDamage);
					}
				}
			}
			
			if (isBow && !target.isInvul() && isCanAbsorbDamage(target))
			{
				final double absorbPercent = getStat().calcStat(Stats.ABSORB_BOW_DAMAGE_PERCENT, 0, null, null);
				if (absorbPercent > 0)
				{
					final int maxCanAbsorb = (int) (getMaxRecoverableHp() - getCurrentHp());
					int absorbDamage = (int) ((absorbPercent / 100.) * damage);
					
					if (absorbDamage > maxCanAbsorb)
					{
						absorbDamage = maxCanAbsorb;
					}
					
					if (absorbDamage > 0)
					{
						setCurrentHp(getCurrentHp() + absorbDamage);
					}
				}
			}
			
			getAI().clientStartAutoAttack();
			if (isSummon())
			{
				final Player owner = ((Summon) this).getOwner();
				if (owner != null)
				{
					owner.getAI().clientStartAutoAttack();
				}
			}
			
			if (target.isPlayer() && target.getActingPlayer().isFakeDeathNow())
			{
				target.stopFakeDeath(true);
			}
			
			if (!target.isRaid() && Formulas.calcAtkBreak(target, crit))
			{
				target.breakAttack();
				target.breakCast();
			}
			
			if (_chanceSkills != null)
			{
				_chanceSkills.onHit(target, damage, false, crit);
				if (reflectedDamage > 0 && !isInvul())
				{
					_chanceSkills.onHit(target, (int) reflectedDamage, true, false);
				}
			}
			
			if (_triggerSkills != null)
			{
				for (final OptionsSkillHolder holder : _triggerSkills.values())
				{
					if ((!crit && (holder.getSkillType() == OptionsSkillType.ATTACK)) || ((holder.getSkillType() == OptionsSkillType.CRITICAL) && crit))
					{
						if (Rnd.chance(holder.getChance()))
						{
							makeTriggerCast(holder.getSkill(), target);
						}
					}
				}
			}
			
			if (target.getChanceSkills() != null)
			{
				target.getChanceSkills().onHit(this, damage, true, crit);
			}
		}
		
		final Weapon activeWeapon = getActiveWeaponItem();
		if (activeWeapon != null)
		{
			activeWeapon.getSkillEffects(this, target, crit);
		}
		
		if ((this instanceof EventMapGuardInstance) && (target instanceof Player))
		{
			target.doDie(this);
		}
		rechargeShots(true, false);
	}
	
	public void breakAttack()
	{
		if (isAttackingNow())
		{
			abortAttack();
			if (isPlayer())
			{
				sendPacket(SystemMessageId.ATTACK_FAILED);
			}
		}
	}
	
	public void breakCast()
	{
		final var sk = _castingSkill;
		if (isCastingNow() && canAbortCast() && (sk != null && (sk.isMagic() || sk.isStatic())))
		{
			abortCast();
			if (isPlayer())
			{
				sendPacket(SystemMessageId.CASTING_INTERRUPTED);
			}
		}
	}
	
	protected void reduceArrowCount(boolean bolts)
	{
	}
	
	@Override
	public void onForcedAttack(Player player, boolean shift)
	{
		for (final AbstractFightEvent e : player.getFightEvents())
		{
			if (e != null && !e.canAttack(this, player))
			{
				player.sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
				player.sendActionFailed();
				return;
			}
		}

		var e = getPartyTournament();
		if (e != null && !e.canAttack(this, player))
		{
			player.sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
			player.sendActionFailed();
			return;
		}

		if (isInsidePeaceZone(player) && !player.isInFightEvent())
		{
			player.sendPacket(SystemMessageId.TARGET_IN_PEACEZONE);
			player.sendActionFailed();
			return;
		}
		
		if (player.isInOlympiadMode() && player.getTarget() != null)
		{
			if (player.getTarget().isPlayable())
			{
				if (!Config.ALLOW_OLY_HIT_SUMMON || (Config.ALLOW_OLY_HIT_SUMMON && (!player.hasSummon() || (player.hasSummon() && (player.getTarget() != null) && player.getTarget() != player.getSummon()))))
				{
					Player target = null;
					final GameObject object = player.getTarget();
					if ((object != null) && object.isPlayable())
					{
						target = object.getActingPlayer();
					}
					
					if ((target == null) || (target.isInOlympiadMode() && (!player.isOlympiadStart() || (player.getOlympiadGameId() != target.getOlympiadGameId()))))
					{
						player.sendActionFailed();
						return;
					}
				}
			}
			else
			{
				player.sendActionFailed();
				return;
			}
		}
		
		if ((player.getTarget() != null) && !player.getTarget().canBeAttacked() && !player.getAccessLevel().allowPeaceAttack())
		{
			player.sendActionFailed();
			return;
		}
		
		if (player.isConfused())
		{
			player.sendActionFailed();
			return;
		}
		
		if (player.isBlocked())
		{
			player.sendActionFailed();
			return;
		}
		
		if (!GeoEngine.getInstance().canSeeTarget(player, this))
		{
			if (!player.getFarmSystem().isLocked())
			{
				player.sendPacket(SystemMessageId.CANT_SEE_TARGET);
			}
			player.sendActionFailed();
			return;
		}
		
		if (player.getBlockCheckerArena() != -1)
		{
			player.sendActionFailed();
			return;
		}
		player.getAI().setIntention(CtrlIntention.ATTACK, this, shift);
	}
	
	public boolean isInsidePeaceZone(Player attacker)
	{
		if (isInFightEvent() && attacker.isInFightEvent())
		{
			return false;
		}
		return isInsidePeaceZone(attacker, this);
	}

	public boolean isInsidePeaceZone(Player attacker, GameObject target)
	{
		if ((target.isPlayer() && target.getActingPlayer().isInFightEvent()) && attacker.isInFightEvent())
		{
			return false;
		}
		return (!attacker.getAccessLevel().allowPeaceAttack() && isInsidePeaceZone((GameObject) attacker, target));
	}
	
	public boolean isInsidePeaceZone(GameObject attacker, GameObject target)
	{
		if (target == null)
		{
			return false;
		}
		if (!(target.isPlayable() && attacker.isPlayable()))
		{
			return false;
		}

		if ((attacker.isPlayer() && attacker.getActingPlayer().isInFightEvent()) && (target.isPlayer() && target.getActingPlayer().isInFightEvent()))
		{
			return false;
		}

		if (!getReflection().isDefault() && getReflection().isPvPInstance())
		{
			return false;
		}
		
		if (TerritoryWarManager.PLAYER_WITH_WARD_CAN_BE_KILLED_IN_PEACEZONE && TerritoryWarManager.getInstance().isTWInProgress())
		{
			if (target.isPlayer() && target.getActingPlayer().isCombatFlagEquipped())
			{
				return false;
			}
		}
		
		if (Config.ALT_GAME_KARMA_PLAYER_CAN_BE_KILLED_IN_PEACEZONE)
		{
			if ((target.getActingPlayer() != null) && (target.getActingPlayer().getKarma() > 0))
			{
				return false;
			}
			
			if ((attacker.getActingPlayer() != null) && (attacker.getActingPlayer().getKarma() > 0) && (target.getActingPlayer() != null) && (target.getActingPlayer().getPvpFlag() > 0))
			{
				return false;
			}
			
			if ((attacker instanceof Creature) && (target instanceof Creature))
			{
				return (target.isInsideZone(ZoneId.PEACE) || attacker.isInsideZone(ZoneId.PEACE));
			}
			
			if (attacker instanceof Creature)
			{
				return ((TownManager.getTown(target.getX(), target.getY(), target.getZ()) != null) || attacker.isInsideZone(ZoneId.PEACE));
			}
		}
		
		if ((attacker instanceof Creature) && (target instanceof Creature))
		{
			return (target.isInsideZone(ZoneId.PEACE) || attacker.isInsideZone(ZoneId.PEACE));
		}
		
		if (attacker instanceof Creature)
		{
			return ((TownManager.getTown(target.getX(), target.getY(), target.getZ()) != null) || attacker.isInsideZone(ZoneId.PEACE));
		}
		return ((TownManager.getTown(target.getX(), target.getY(), target.getZ()) != null) || (TownManager.getTown(attacker.getX(), attacker.getY(), attacker.getZ()) != null));
	}
	
	public boolean isInActiveRegion()
	{
		final WorldRegion region = getWorldRegion();
		return ((region != null) && (region.isActive()));
	}
	
	public boolean isInParty()
	{
		return false;
	}
	
	public Party getParty()
	{
		return null;
	}
	
	public final WeaponType getAttackType()
	{
		if (isTransformed())
		{
			final TransformTemplate template = getTransformation().getTemplate(getActingPlayer());
			if (template != null)
			{
				return template.getBaseAttackType();
			}
		}
		final Weapon weapon = getActiveWeaponItem();
		if (weapon != null)
		{
			return weapon.getItemType();
		}
		
		if (isPlayer())
		{
			final var inv = getInventory();
			return inv != null && inv.getPaperdollItem(Inventory.PAPERDOLL_LHAND) != null ? WeaponType.SWORD : getTemplate().getBaseAttackType();
		}
		return getTemplate().getBaseAttackType();
	}
	
	public int calculateTimeBetweenAttacks()
	{
		return (int) (500000 / getPAtkSpd());
	}
	
	public int calculateReuseTime(final Weapon weapon)
	{
		if (isTransformed())
		{
			switch (getAttackType())
			{
				case BOW :
					return (int) ((1500 * 333 * getStat().getWeaponReuseModifier(null)) / getStat().getPAtkSpd());
				case CROSSBOW :
					return (int) ((1200 * 333 * getStat().getWeaponReuseModifier(null)) / getStat().getPAtkSpd());
			}
		}
		
		if ((weapon == null) || (weapon.getReuseDelay() == 0))
		{
			return 0;
		}
		return (int) ((weapon.getReuseDelay() * 333) / getPAtkSpd());
	}
	
	public boolean isUsingDualWeapon()
	{
		return false;
	}
	
	public Skill addSkill(Skill newSkill)
	{
		Skill oldSkill = null;
		
		if (newSkill != null)
		{
			final var isBlocked = isSkillBlocked(newSkill);
			oldSkill = _skills.put(newSkill.getId(), newSkill);
			
			if (oldSkill != null)
			{
				final TimeStamp sts = _skillReuses.get(oldSkill.hashCode());
				if ((sts != null) && sts.hasNotPassed())
				{
					_skillReuses.put(newSkill.hashCode(), sts);
					addTimeStamp(newSkill, sts.getReuse(), sts.getStamp());
				}
			}
			
			if (oldSkill != null)
			{
				if ((oldSkill.triggerAnotherSkill()))
				{
					removeSkill(oldSkill.getTriggeredId(), true);
				}
				removeStatsOwner(oldSkill);
			}
			
			if (!isBlocked)
			{
				addStatFuncs(newSkill.getStatFuncs(null, this));
			}
			
			if ((oldSkill != null) && (_chanceSkills != null))
			{
				removeChanceSkill(oldSkill.getId());
			}
			
			if (!isBlocked)
			{
				if (newSkill.isChance())
				{
					addChanceTrigger(newSkill);
				}
				newSkill.getEffectsPassive(this);
			}
		}
		return oldSkill;
	}
	
	public Skill removeSkill(Skill skill, boolean cancelEffect)
	{
		return (skill != null) ? removeSkill(skill.getId(), cancelEffect) : null;
	}
	
	public Skill removeSkill(int skillId)
	{
		return removeSkill(skillId, true);
	}
	
	public Skill removeSkill(int skillId, boolean cancelEffect)
	{
		final Skill oldSkill = _skills.remove(skillId);
		if (oldSkill != null)
		{
			if ((oldSkill.triggerAnotherSkill()) && (oldSkill.getTriggeredId() > 0))
			{
				removeSkill(oldSkill.getTriggeredId(), true);
			}
			
			if ((_castingSkill != null) && (isCastingNow() || isCastingSimultaneouslyNow()))
			{
				if (oldSkill.getId() == _castingSkill.getId())
				{
					abortCast();
				}
			}
			
			_effects.removePassiveEffects(skillId);
			
			if (cancelEffect || oldSkill.isToggle())
			{
				final Effect e = getFirstEffect(oldSkill);
				if ((e == null) || (e.getEffectType() != EffectType.TRANSFORMATION))
				{
					removeStatsOwner(oldSkill);
					stopSkillEffects(oldSkill.getId());
				}
			}
			
			if (isPlayer())
			{
				if ((oldSkill instanceof SkillSummon) && (oldSkill.getId() == 710) && hasSummon() && (getSummon().getId() == 14870))
				{
					getActingPlayer().getSummon().unSummon(getActingPlayer());
				}
			}
			
			if (oldSkill.isChance() && (_chanceSkills != null))
			{
				removeChanceSkill(oldSkill.getId());
			}
		}
		return oldSkill;
	}
	
	public void removeChanceSkill(int id)
	{
		if (_chanceSkills == null)
		{
			return;
		}
		synchronized (_chanceSkills)
		{
			for (final IChanceSkillTrigger trigger : _chanceSkills.keySet())
			{
				if (!(trigger instanceof Skill))
				{
					continue;
				}
				if (((Skill) trigger).getId() == id)
				{
					_chanceSkills.remove(trigger);
				}
			}
		}
	}
	
	public void addChanceTrigger(IChanceSkillTrigger trigger)
	{
		if (_chanceSkills == null)
		{
			synchronized (this)
			{
				if (_chanceSkills == null)
				{
					_chanceSkills = new ChanceSkillList(this);
				}
			}
		}
		_chanceSkills.put(trigger, trigger.getTriggeredChanceCondition());
	}
	
	public void removeChanceEffect(IChanceSkillTrigger effect)
	{
		if (_chanceSkills == null)
		{
			return;
		}
		_chanceSkills.remove(effect);
	}
	
	public void onStartChanceEffect(byte element)
	{
		if (_chanceSkills == null)
		{
			return;
		}
		
		_chanceSkills.onStart(element);
	}
	
	public void onActionTimeChanceEffect(byte element)
	{
		if (_chanceSkills == null)
		{
			return;
		}
		
		_chanceSkills.onActionTime(element);
	}
	
	public void onExitChanceEffect(byte element)
	{
		if (_chanceSkills == null)
		{
			return;
		}
		
		_chanceSkills.onExit(element);
	}
	
	public final Collection<Skill> getAllSkills()
	{
		return _skills.values();
	}
	
	public Map<Integer, Skill> getSkills()
	{
		return _skills;
	}
	
	public ChanceSkillList getChanceSkills()
	{
		return _chanceSkills;
	}
	
	public int getSkillLevel(int skillId)
	{
		final var skill = getKnownSkill(skillId);
		return (skill == null) ? -1 : skill.getLevel();
	}
	
	public final Skill getKnownSkill(int skillId)
	{
		return _skills.get(skillId);
	}
	
	public int getBuffCount()
	{
		return _effects.getBuffCount();
	}
	
	public int getDanceCount()
	{
		return _effects.getDanceCount();
	}
	
	public void onMagicLaunchedTimer(MagicUseTask mut)
	{
		final Skill skill = mut.getSkill();
		if ((skill == null) || (mut.getTargets() == null))
		{
			abortCast();
			return;
		}
		
		GameObject[] targets = isPlayer() ? skill.isAura() ? skill.getTargetList(this) : skill.isArea() ? skill.getTargetList(this, false, getAI().getCastTarget()) : mut.getTargets() : mut.getTargets();
		mut.setTargets(targets);
		
		if ((targets.length == 0) && !skill.isAura())
		{
			abortCast();
			return;
		}
		
		int escapeRange = 0;
		if (skill.getEffectRange() > escapeRange)
		{
			escapeRange = skill.getEffectRange();
		}
		else if ((skill.getCastRange() < 0) && (skill.getAffectRange() > 80))
		{
			escapeRange = skill.getAffectRange();
		}
		
		if ((targets.length > 0) && (escapeRange > 0))
		{
			int _skiprange = 0;
			int skipLOS = 0;
			int _skippeace = 0;
			final List<Creature> targetList = new ArrayList<>();
			for (final GameObject target : targets)
			{
				if (target instanceof Creature)
				{
					if (!isInsideRadius(target.getX(), target.getY(), target.getZ(), (int) (escapeRange + getColRadius()), true, false))
					{
						_skiprange++;
						continue;
					}
					
					if (!Config.ALLOW_SKILL_END_CAST && !skill.isDisableGeoCheck() && skill.getTargetType() != TargetType.PARTY && !skill.hasEffectType(EffectType.HEAL) && mut.getHitTime() > 550 && !GeoEngine.getInstance().canSeeTarget(this, target))
					{
						skipLOS++;
						continue;
					}
					
					if (skill.isOffensive() && !skill.isNeutral())
					{
						if (isPlayer())
						{
							if (((Creature) target).isInsidePeaceZone(getActingPlayer()))
							{
								_skippeace++;
								continue;
							}
						}
						else
						{
							if (((Creature) target).isInsidePeaceZone(this, target))
							{
								_skippeace++;
								continue;
							}
						}
					}
					targetList.add((Creature) target);
				}
			}
			if (targetList.isEmpty() && !skill.isAura())
			{
				if (isPlayer())
				{
					if (_skiprange > 0)
					{
						sendPacket(SystemMessageId.DIST_TOO_FAR_CASTING_STOPPED);
					}
					else if (skipLOS > 0)
					{
						sendPacket(SystemMessageId.CANT_SEE_TARGET);
					}
					else if (_skippeace > 0)
					{
						sendPacket(SystemMessageId.A_MALICIOUS_SKILL_CANNOT_BE_USED_IN_PEACE_ZONE);
					}
				}
				abortCast();
				return;
			}
			mut.setTargets(targetList.toArray(new Creature[targetList.size()]));
			targets = mut.getTargets();
		}
		
		if ((mut.isSimultaneous() && !isCastingSimultaneouslyNow()) || (!mut.isSimultaneous() && !isCastingNow()) || (isAlikeDead() && !skill.isStatic()))
		{
			getAI().notifyEvent(CtrlEvent.EVT_CANCEL);
			return;
		}
		
		if (!skill.isStatic())
		{
			broadcastPacket(new MagicSkillLaunched(this, skill.getDisplayId(), skill.getDisplayLevel(), targets));
		}
		
		getListeners().onMagicUse(skill, targets, true);
		
		mut.setPhase(2);
		if (mut.getHitTime() == 0)
		{
			onMagicHitTimer(mut);
		}
		else
		{
			int delay = 0;
			if (Config.ENABLE_OLD_CAST)
			{
				delay = skill.getHitCounts() > 0 ? ((mut.getHitTime() - mut.getDelay()) * skill.getHitTimings()[0]) / 100 : (mut.getHitTime() - mut.getDelay());
			}
			else
			{
				delay = 400;
			}
			_skillCast = ThreadPoolManager.getInstance().schedule(mut, delay);
		}
	}
	
	public void onMagicHitTimer(MagicUseTask mut)
	{
		try
		{
			final Skill skill = mut.getSkill();
			final GameObject[] targets = mut.getTargets();
			
			if ((skill == null) || (targets == null))
			{
				abortCast();
				return;
			}
			
			for (final GameObject tgt : targets)
			{
				if (tgt.isPlayable())
				{
					final Creature target = (Creature) tgt;
					
					if (skill.getSkillType() == SkillType.BUFF)
					{
						final SystemMessage smsg = SystemMessage.getSystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT);
						smsg.addSkillName(skill);
						target.sendPacket(smsg);
					}
					
					if (isPlayer() && target.isSummon())
					{
						((Summon) target).updateAndBroadcastStatus(1);
					}
				}
			}
			
			switch (skill.getFlyType())
			{
				case CHARGE :
					if (_flyLoc != null)
					{
						setLocation(_flyLoc);
					}
					break;
			}
			
			boolean isSendStatus = false;
			final var mpConsume = getStat().getMpConsume(skill);
			if (mpConsume[0] > 0)
			{
				if (mpConsume[0] > getCurrentMp())
				{
					sendPacket(SystemMessageId.NOT_ENOUGH_MP);
					abortCast();
					return;
				}
				getStatus().reduceMp(mpConsume[1]);
				isSendStatus = true;
			}
			
			if (skill.getHpConsume() > 0)
			{
				final double consumeHp = skill.getHpConsume();
				if (consumeHp >= getCurrentHp())
				{
					sendPacket(SystemMessageId.NOT_ENOUGH_HP);
					abortCast();
					return;
				}
				getStatus().reduceHp(consumeHp, this, true);
				isSendStatus = true;
			}
			
			if (isSendStatus)
			{
				sendPacket(makeStatusUpdate(StatusUpdate.CUR_HP, StatusUpdate.CUR_MP));
			}
			
			switch (skill.getFlyType())
			{
				case THROW_UP :
				case THROW_HORIZONTAL :
					Location flyLoc;
					for (final GameObject target : targets)
					{
						flyLoc = getFlyLocation(null, skill);
						broadcastPacket(new FlyToLocation((Creature) target, flyLoc, skill.getFlyType()));
						target.setLocation(flyLoc);
					}
					break;
			}
			

			callSkill(mut.getSkill(), mut.getTargets());
			
			if (mut.getHitTime() > 0)
			{
				mut.setCount(mut.getCount() + 1);
				if (mut.getCount() < skill.getHitCounts())
				{
					int skillTime = 0;
					if (Config.ENABLE_OLD_CAST)
					{
						skillTime = ((mut.getHitTime() - mut.getDelay()) * skill.getHitTimings()[mut.getCount()]) / 100;
					}
					else
					{
						skillTime = (mut.getHitTime() * skill.getHitTimings()[mut.getCount()]) / 100;
					}
					
					if (mut.isSimultaneous())
					{
						_skillCast2 = ThreadPoolManager.getInstance().schedule(mut, skillTime);
					}
					else
					{
						_skillCast = ThreadPoolManager.getInstance().schedule(mut, skillTime);
					}
					return;
				}
			}
			
			mut.setPhase(3);
			if ((mut.getHitTime() == 0) || (mut.getCoolTime() == 0))
			{
				onMagicFinalizer(mut);
			}
			else
			{
				if (mut.isSimultaneous())
				{
					_skillCast2 = ThreadPoolManager.getInstance().schedule(mut, mut.getCoolTime());
				}
				else
				{
					_skillCast = ThreadPoolManager.getInstance().schedule(mut, mut.getCoolTime());
				}
			}
		}
		catch (final Exception e)
		{
			if (Config.DEBUG)
			{
				_log.warn(getClass().getSimpleName() + ": onMagicHitTimer() failed.", e);
			}
		}
	}
	
	public void onMagicFinalizer(MagicUseTask mut)
	{
		if (mut.isSimultaneous())
		{
			_skillCast2 = null;
			_castInterruptTime = 0;
			_castingSkill = null;
			_isCriticalBlowCastingSkill = false;
			_direction = TargetDirection.NONE;
			setIsCastingSimultaneouslyNow(false);
			return;
		}
		finishFly();
		_animationEndTime = 0;
		_skillCast = null;
		_skillGeoCheckTask = null;
		_castInterruptTime = 0;
		_castingSkill = null;
		_isCriticalBlowCastingSkill = false;
		_direction = TargetDirection.NONE;
		setIsCastingNow(false);
		setIsCastingSimultaneouslyNow(false);
		
		final Skill skill = mut.getSkill();
		final GameObject target = mut.getTargets().length > 0 ? mut.getTargets()[0] : null;
		
		if (isPlayer() && (skill.getMaxSoulConsumeCount() > 0))
		{
			if (!getActingPlayer().decreaseSouls(skill.getMaxSoulConsumeCount(), skill))
			{
				abortCast();
				return;
			}
		}
		
		if (isPlayer() && skill.nextActionIsAttack())
		{
			if ((getAI().getNextIntention() == null) || (getAI().getNextIntention() != null && getAI().getNextIntention().getCtrlIntention() != CtrlIntention.MOVING))
			{
				final var player = getActingPlayer();
				final var isCtrlPressed = player.getCurrentSkill() != null && player.getCurrentSkill().isCtrlPressed();
				final var hasQueuedSkill = player.getQueuedSkill() != null;
				if (!isCtrlPressed && !hasQueuedSkill)
				{
					ThreadPoolManager.getInstance().schedule(new NextActionTask(player, target), 100);
				}
			}
		}
		
		if (skill.isOffensive() && !skill.isNeutral())
		{
			switch (skill.getSkillType())
			{
				case UNLOCK :
				case UNLOCK_SPECIAL :
				case DELUXE_KEY_UNLOCK :
				{
					break;
				}
				default :
				{
					getAI().clientStartAutoAttack();
					break;
				}
			}
		}
		
		getAI().notifyEvent(CtrlEvent.EVT_FINISH_CASTING);
		notifyQuestEventSkillFinished(skill, target);
		rechargeShots(mut.getSkill().useSoulShot(), mut.getSkill().useSpiritShot());
		
		if (isPlayer())
		{
			final Player currPlayer = getActingPlayer();
			final SkillUseHolder queuedSkill = currPlayer.getQueuedSkill();
			
			currPlayer.setCurrentSkill(null, false, false);
			
			if (queuedSkill != null)
			{
				currPlayer.setQueuedSkill(null, false, false);
				if (mut.getHitTime() == 0)
				{
					if (currPlayer != null)
					{
						currPlayer.useMagic(queuedSkill.getSkill(), queuedSkill.isCtrlPressed(), queuedSkill.isShiftPressed(), true);
					}
				}
				else
				{
					ThreadPoolManager.getInstance().execute(new QueuedMagicUseTask(currPlayer, queuedSkill.getSkill(), queuedSkill.isCtrlPressed(), queuedSkill.isShiftPressed()));
				}
			}
		}
	}
	
	protected void notifyQuestEventSkillFinished(Skill skill, GameObject target)
	{
	}
	
	public Collection<TimeStamp> getSkillReuses()
	{
		return _skillReuses.valueCollection();
	}
	
	public TimeStamp getSkillReuse(Skill skill)
	{
		return _skillReuses.get(skill.hashCode());
	}
	
	public void enableSkill(Skill skill)
	{
		if (skill == null)
		{
			return;
		}
		_skillReuses.remove(skill.hashCode());
	}
	
	public void disableSkill(Skill skill, long delay)
	{
		if (skill == null)
		{
			return;
		}
		_skillReuses.put(skill.hashCode(), new TimeStamp(skill, delay));
	}
	
	public final void resetDisabledSkills()
	{
		_skillReuses.clear();
	}

	public void addBlockSkill(Skill sk, boolean removeStats)
	{
		if (sk == null)
		{
			return;
		}
		
		if (removeStats)
		{
			removeStatsOwner(sk);
			_effects.removePassiveEffects(sk.getId());
			stopSkillEffects(sk.getId());
			if (sk.isChance() && (_chanceSkills != null))
			{
				removeChanceSkill(sk.getId());
			}
		}
		_blockSkills.add(sk);
	}

	public void removeBlockSkill(Skill sk, boolean restoreStats)
	{
		if (sk == null)
		{
			return;
		}

		if (isSkillBlocked(sk))
		{
			if (restoreStats)
			{
				addStatFuncs(sk.getStatFuncs(null, this));
				if (sk.isChance())
				{
					addChanceTrigger(sk);
				}
				sk.getEffectsPassive(this);
			}
			_blockSkills.remove(sk);
		}
	}
	
	public void cleanBlockSkills(boolean restoreStats)
	{
		if (_blockSkills.isEmpty())
		{
			return;
		}
		
		if (restoreStats)
		{
			for (final var sk : _blockSkills)
			{
				if (sk != null)
				{
					addStatFuncs(sk.getStatFuncs(null, this));
					if (sk.isChance())
					{
						addChanceTrigger(sk);
					}
					sk.getEffectsPassive(this);
				}
			}
		}
		_blockSkills.clear();
	}

	public List<Skill> getBlockSkills()
	{
		return _blockSkills;
	}

	public boolean isSkillBlocked(Skill sk)
	{
		if (sk == null)
		{
			return true;
		}
		return _blockSkills.contains(sk);
	}

	public boolean isSkillDisabled(Skill skill)
	{
		final TimeStamp sts = _skillReuses.get(skill.hashCode());
		if (sts == null)
		{
			return false;
		}
		if (sts.hasNotPassed())
		{
			return true;
		}
		_skillReuses.remove(skill.hashCode());
		return false;
	}
	
	public void disableAllSkills()
	{
		_allSkillsDisabled = true;
	}
	
	public void enableAllSkills()
	{
		_allSkillsDisabled = false;
	}
	
	public void callSkill(Skill skill, GameObject[] targets)
	{
		try
		{
			final Weapon activeWeapon = getActiveWeaponItem();
			
			if (skill.isToggle() && (getFirstEffect(skill.getId()) != null))
			{
				return;
			}
			
			for (final GameObject trg : targets)
			{
				if (trg instanceof Creature)
				{
					final Creature target = (Creature) trg;
					Creature targetsAttackTarget = null;
					Creature targetsCastTarget = null;
					if (target.hasAI())
					{
						targetsAttackTarget = target.getAI().getAttackTarget();
						targetsCastTarget = target.getAI().getCastTarget();
					}
					target.getListeners().onMagicHit(skill, this);
					if (!Config.RAID_DISABLE_CURSE && (!target.isDead() && ((target.isRaid() && target.giveRaidCurse() && (getLevel() > (target.getLevel() + 8))) || (!skill.isOffensive() && (targetsAttackTarget != null) && targetsAttackTarget.isRaid() && targetsAttackTarget.giveRaidCurse() && targetsAttackTarget.getAttackByList().contains(target) && (getLevel() > (targetsAttackTarget.getLevel() + 8))) || (!skill.isOffensive() && (targetsCastTarget != null) && targetsCastTarget.isRaid() && targetsCastTarget.giveRaidCurse() && targetsCastTarget.getAttackByList().contains(target) && (getLevel() > (targetsCastTarget.getLevel() + 8))))))
					{
						if (skill.isMagic())
						{
							final Skill tempSkill = SkillsParser.FrequentSkill.RAID_CURSE.getSkill();
							if (tempSkill != null)
							{
								abortAttack();
								abortCast();
								getAI().setIntention(CtrlIntention.IDLE);
								tempSkill.getEffects(target, this, false);
							}
						}
						else
						{
							if (target.getId() != 29054)
							{
								final Skill tempSkill = SkillsParser.FrequentSkill.RAID_CURSE2.getSkill();
								if (tempSkill != null)
								{
									abortAttack();
									abortCast();
									getAI().setIntention(CtrlIntention.IDLE);
									tempSkill.getEffects(target, this, false);
								}
							}
						}
						return;
					}
					
					if (skill.isOverhit())
					{
						if (target.isAttackable())
						{
							((Attackable) target).overhitEnabled(true);
						}
					}
				}
			}
			
			final ISkillHandler handler = SkillHandler.getInstance().getHandler(skill.getSkillType());
			if (handler != null)
			{
				handler.useSkill(this, skill, targets);
			}
			else
			{
				skill.useSkill(this, targets);
			}
			
			final Player player = getActingPlayer();
			for (final GameObject trg : targets)
			{
				if (trg instanceof Creature)
				{
					final Creature target = (Creature) trg;
					if (!skill.isStatic())
					{
						if ((activeWeapon != null) && !target.isDead())
						{
							if ((activeWeapon.getSkillEffects(this, target, skill).length > 0) && isPlayer())
							{
								final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_HAS_BEEN_ACTIVATED);
								sm.addSkillName(skill);
								sendPacket(sm);
							}
						}
						
						if (skill.isAttackSkill())
						{
							if (_chanceSkills != null)
							{
								_chanceSkills.onSkillHit(target, 0, skill, false);
							}
							
							if (target.getChanceSkills() != null)
							{
								target.getChanceSkills().onSkillHit(this, 0, skill, true);
							}
						}
						
						if (_triggerSkills != null)
						{
							for (final OptionsSkillHolder holder : _triggerSkills.values())
							{
								if ((skill.isMagic() && (holder.getSkillType() == OptionsSkillType.MAGIC)) || (skill.isPhysical() && (holder.getSkillType() == OptionsSkillType.ATTACK)))
								{
									if (Rnd.chance(holder.getChance()))
									{
										makeTriggerCast(holder.getSkill(), target);
									}
								}
							}
						}
					}
					
					if (player != null)
					{
						if (skill.isNeutral())
						{
						}
						else if (skill.isOffensive())
						{
							if (target.isPlayer() || target.isSummon() || target.isTrap())
							{
								if ((skill.getSkillType() != SkillType.SIGNET) && (skill.getSkillType() != SkillType.SIGNET_CASTTIME))
								{
									if (target.isPlayer())
									{
										if (!target.isInvul() || (target.isInvul() && !Config.ATTACK_STANCE_MAGIC))
										{
											target.getActingPlayer().getAI().clientStartAutoAttack();
										}
									}
									else if (target.isSummon() && target.hasAI())
									{
										final Player owner = ((Summon) target).getOwner();
										if (owner != null)
										{
											if (!owner.isInvul() || (owner.isInvul() && !Config.ATTACK_STANCE_MAGIC))
											{
												owner.getAI().clientStartAutoAttack();
											}
										}
									}
									
									if ((player.getSummon() != target) && !isTrap() && skill.getTargetType() != TargetType.CORPSE_MOB)
									{
										player.updatePvPStatus(target);
									}
								}
							}
							else if (target.isAttackable())
							{
								switch (skill.getId())
								{
									case 51 :
									case 511 :
										break;
									default :
										target.addAttackerToAttackByList(this);
								}
							}
							
							if (target.hasAI())
							{
								switch (skill.getSkillType())
								{
									case AGGREDUCE :
									case AGGREDUCE_CHAR :
									case AGGREMOVE :
										break;
									default :
										target.getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, this, 0);
								}
							}
						}
						else
						{
							if (target.isPlayer())
							{
								if (!(target.equals(this) || target.equals(player)) && ((target.getActingPlayer().getPvpFlag() > 0) || (target.getActingPlayer().getKarma() > 0)))
								{
									player.updatePvPStatus();
								}
							}
							else if (target.isAttackable())
							{
								switch (skill.getSkillType())
								{
									case SUMMON :
									case UNLOCK :
									case DELUXE_KEY_UNLOCK :
									case UNLOCK_SPECIAL :
										break;
									default :
										player.updatePvPStatus();
								}
							}
						}
					}
				}
			}
				
			if (player != null)
			{
				for (final Npc npcMob : World.getInstance().getAroundNpc(player, 1000, 200))
				{
					if (npcMob.hasAI())
					{
						npcMob.getAI().notifyEvent(CtrlEvent.EVT_SEE_SPELL, skill, this);
						if (npcMob.getTemplate().getEventQuests(QuestEventType.ON_SKILL_SEE) != null)
						{
							for (final Quest quest : npcMob.getTemplate().getEventQuests(QuestEventType.ON_SKILL_SEE))
							{
								quest.notifySkillSee(npcMob, player, skill, targets, isSummon());
							}
						}
						
						if (npcMob.isAttackable() && !skill.isEffectTypeBattle())
						{
							final Attackable attackable = (Attackable) npcMob;
							
							int skillEffectPoint = skill.getAggroPoints();
							
							if (player.hasSummon())
							{
								if ((targets.length == 1) && ArrayUtils.contains(targets, player.getSummon()))
								{
									skillEffectPoint = 0;
								}
							}
							
							if (skillEffectPoint > 0 && attackable.hasAI() && !attackable.isInvul())
							{
								if (attackable.getAI().getIntention() == CtrlIntention.ATTACK)
								{
									final GameObject npcTarget = attackable.getTarget();
									for (final GameObject skillTarget : targets)
									{
										if ((npcTarget == skillTarget) || (npcMob == skillTarget))
										{
											final Creature originalCaster = isSummon() ? player.getSummon() : player;
											attackable.addDamageHate(originalCaster, 0, (int) (skillEffectPoint * Config.MATK_HATE_MOD));
										}
									}
								}
							}
						}
					}
				}
			}
			
			if (skill.isOffensive())
			{
				switch (skill.getSkillType())
				{
					case AGGREDUCE :
					case AGGREDUCE_CHAR :
					case AGGREMOVE :
						break;
					default :
						for (final GameObject target : targets)
						{
							if ((target instanceof Creature) && ((Creature) target).hasAI())
							{
								((Creature) target).getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, this, 0);
							}
						}
						break;
				}
			}
		}
		catch (final Exception e)
		{
			_log.warn(getClass().getSimpleName() + ": callSkill() failed.", e);
		}
	}
	
	public boolean isBehindTarget()
	{
		return isBehind(getTarget());
	}
	
	public boolean isInFrontOf(Creature target)
	{
		double angleChar, angleTarget, angleDiff;
		final double maxAngleDiff = 60;
		if (target == null)
		{
			return false;
		}
		
		angleTarget = Util.calculateAngleFrom(target, this);
		angleChar = Util.convertHeadingToDegree(target.getHeading());
		angleDiff = angleChar - angleTarget;
		if (angleDiff <= (-360 + maxAngleDiff))
		{
			angleDiff += 360;
		}
		if (angleDiff >= (360 - maxAngleDiff))
		{
			angleDiff -= 360;
		}
		
		return Math.abs(angleDiff) <= maxAngleDiff;
	}
	
	public boolean isInFrontOfTarget(Creature target, int range)
	{
		double angleChar, angleTarget, angleDiff;
		final double maxAngleDiff = range;
		if (target == null)
		{
			return false;
		}
		
		angleTarget = Util.calculateAngleFrom(target, this);
		angleChar = Util.convertHeadingToDegree(target.getHeading());
		angleDiff = angleChar - angleTarget;
		if (angleDiff <= (-360 + maxAngleDiff))
		{
			angleDiff += 360;
		}
		if (angleDiff >= (360 - maxAngleDiff))
		{
			angleDiff -= 360;
		}
		
		return Math.abs(angleDiff) <= maxAngleDiff;
	}
	
	public boolean isFacing(GameObject target, int maxAngle)
	{
		double angleChar, angleTarget, angleDiff, maxAngleDiff;
		if (target == null)
		{
			return false;
		}
		maxAngleDiff = maxAngle / 2.;
		angleTarget = Util.calculateAngleFrom(this, target);
		angleChar = Util.convertHeadingToDegree(getHeading());
		angleDiff = angleChar - angleTarget;
		if (angleDiff <= (-360 + maxAngleDiff))
		{
			angleDiff += 360;
		}
		if (angleDiff >= (360 - maxAngleDiff))
		{
			angleDiff -= 360;
		}
		
		return Math.abs(angleDiff) <= maxAngleDiff;
	}
	
	public boolean isInFrontOfTarget()
	{
		final GameObject target = getTarget();
		if (target instanceof Creature)
		{
			return isInFrontOf((Creature) target);
		}
		return false;
	}
	
	public double getLevelMod()
	{
		return Config.ALLOW_NPC_LVL_MOD ? ((getLevel() + 89) / 100.0) : 1.0;
	}
	
	public final void setSkillCast(Future<?> newSkillCast)
	{
		_skillCast = newSkillCast;
	}
	
	public final void forceIsCasting(long interruptTime)
	{
		setIsCastingNow(true);
		_castInterruptTime = interruptTime;
	}
	
	private boolean _AIdisabled = false;
	
	public void updatePvPFlag(int value)
	{
	}
	
	public int getRandomDamage()
	{
		final Weapon weaponItem = getActiveWeaponItem();
		if (weaponItem == null)
		{
			return 5 + (int) Math.sqrt(getLevel());
		}
		return weaponItem.getRandomDamage();
	}
	
	public long getAttackEndTime()
	{
		return _attackEndTime;
	}
	
	public long getBowAttackEndTime()
	{
		return _disableBowAttackEndTime;
	}
	
	public abstract int getLevel();
	
	public final double calcStat(Stats stat, double init, Creature target, Skill skill)
	{
		return getStat().calcStat(stat, init, target, skill);
	}
	
	public int getAccuracy()
	{
		return getStat().getAccuracy();
	}
	
	public float getAttackSpeedMultiplier()
	{
		return getStat().getAttackSpeedMultiplier();
	}
	
	public int getCON()
	{
		return getStat().getCON();
	}
	
	public int getDEX()
	{
		return getStat().getDEX();
	}
	
	public final double getCriticalDmg(Creature target, double init, Skill skill)
	{
		return getStat().getCriticalDmg(target, init, skill);
	}
	
	public double getCriticalHit(Creature target, Skill skill)
	{
		return getStat().getCriticalHit(target, skill);
	}
	
	public int getEvasionRate(Creature target)
	{
		return getStat().getEvasionRate(target);
	}
	
	public int getINT()
	{
		return getStat().getINT();
	}
	
	public final int getMagicalAttackRange(Skill skill)
	{
		return getStat().getMagicalAttackRange(skill);
	}
	
	public double getMaxCp()
	{
		return getStat().getMaxCp();
	}
	
	public final int getMaxRecoverableCp()
	{
		return getStat().getMaxRecoverableCp();
	}
	
	public double getMAtk(Creature target, Skill skill)
	{
		return getStat().getMAtk(target, skill);
	}
	
	public double getMAtkSpd()
	{
		return getStat().getMAtkSpd();
	}
	
	public double getMaxMp()
	{
		return getStat().getMaxMp();
	}
	
	public int getMaxRecoverableMp()
	{
		return getStat().getMaxRecoverableMp();
	}
	
	public double getMaxHp()
	{
		return getStat().getMaxHp();
	}
	
	public int getMaxRecoverableHp()
	{
		return getStat().getMaxRecoverableHp();
	}
	
	public double getMCriticalHit(Creature target, Skill skill)
	{
		return getStat().getMCriticalHit(target, skill);
	}
	
	public double getMDef(Creature target, Skill skill)
	{
		return getStat().getMDef(target, skill);
	}
	
	public int getMEN()
	{
		return getStat().getMEN();
	}
	
	public double getMReuseRate(Skill skill)
	{
		return getStat().getMReuseRate(skill);
	}
	
	public double getMovementSpeedMultiplier()
	{
		return getStat().getMovementSpeedMultiplier();
	}
	
	public double getPAtk(Creature target)
	{
		return getStat().getPAtk(target);
	}
	
	public double getPAtkAnimals(Creature target)
	{
		return getStat().getPAtkAnimals(target);
	}
	
	public double getPAtkDragons(Creature target)
	{
		return getStat().getPAtkDragons(target);
	}
	
	public double getPAtkInsects(Creature target)
	{
		return getStat().getPAtkInsects(target);
	}
	
	public double getPAtkMonsters(Creature target)
	{
		return getStat().getPAtkMonsters(target);
	}
	
	public double getPAtkPlants(Creature target)
	{
		return getStat().getPAtkPlants(target);
	}
	
	public double getPAtkGiants(Creature target)
	{
		return getStat().getPAtkGiants(target);
	}
	
	public double getPAtkMagicCreatures(Creature target)
	{
		return getStat().getPAtkMagicCreatures(target);
	}
	
	public double getPDefAnimals(Creature target)
	{
		return getStat().getPDefAnimals(target);
	}
	
	public double getPDefDragons(Creature target)
	{
		return getStat().getPDefDragons(target);
	}
	
	public double getPDefInsects(Creature target)
	{
		return getStat().getPDefInsects(target);
	}
	
	public double getPDefMonsters(Creature target)
	{
		return getStat().getPDefMonsters(target);
	}
	
	public double getPDefPlants(Creature target)
	{
		return getStat().getPDefPlants(target);
	}
	
	public double getPDefGiants(Creature target)
	{
		return getStat().getPDefGiants(target);
	}
	
	public double getPDefMagicCreatures(Creature target)
	{
		return getStat().getPDefMagicCreatures(target);
	}
	
	public double getPAtkSpd()
	{
		return getStat().getPAtkSpd();
	}
	
	public double getPDef(Creature target)
	{
		return getStat().getPDef(target);
	}
	
	public final int getPhysicalAttackRange()
	{
		return getStat().getPhysicalAttackRange();
	}
	
	public double getRunSpeed()
	{
		return getStat().getRunSpeed();
	}
	
	public double getSwimRunSpeed()
	{
		return getStat().getSwimRunSpeed();
	}
	
	public final int getShldDef()
	{
		return getStat().getShldDef();
	}
	
	public int getSTR()
	{
		return getStat().getSTR();
	}
	
	public double getWalkSpeed()
	{
		return getStat().getWalkSpeed();
	}
	
	public final double getSwimWalkSpeed()
	{
		return getStat().getSwimWalkSpeed();
	}
	
	public double getMoveSpeed()
	{
		return getStat().getMoveSpeed();
	}
	
	public int getWIT()
	{
		return getStat().getWIT();
	}
	
	public double getRExp()
	{
		return getStat().getRExp();
	}
	
	public double getRSp()
	{
		return getStat().getRSp();
	}
	
	public double getPvpPhysSkillDmg()
	{
		return getStat().getPvpPhysSkillDmg();
	}
	
	public double getPvpPhysSkillDef()
	{
		return getStat().getPvpPhysSkillDef();
	}
	
	public double getPvpPhysDef()
	{
		return getStat().getPvpPhysDef();
	}
	
	public double getPvpPhysDmg()
	{
		return getStat().getPvpPhysDmg();
	}
	
	public double getPvpMagicDmg()
	{
		return getStat().getPvpMagicDmg();
	}
	
	public double getPvpMagicDef()
	{
		return getStat().getPvpMagicDef();
	}
	
	public void addStatusListener(Creature object)
	{
		getStatus().addStatusListener(object);
	}
	
	public void reduceCurrentHp(double i, Creature attacker, Skill skill)
	{
		if (skill != null && !skill.isStatic() && getChanceSkills() != null)
		{
			getChanceSkills().onSkillHit(attacker, i, skill, true);
		}
		reduceCurrentHp(i, attacker, true, false, skill);
	}
	
	public void reduceCurrentHpByDOT(double i, Creature attacker, Skill skill)
	{
		reduceCurrentHp(i, attacker, !skill.isToggle(), true, skill);
	}
	
	public void reduceCurrentHp(double i, Creature attacker, boolean awake, boolean isDOT, Skill skill)
	{
		getListeners().onCurrentHpDamage(i, attacker, skill);
		getStatus().reduceHp(i, attacker, awake, isDOT, false, true);
	}
	
	public void reduceCurrentMp(double i)
	{
		getStatus().reduceMp(i);
	}
	
	@Override
	public void removeStatusListener(Creature object)
	{
		getStatus().removeStatusListener(object);
	}
	
	public void stopHpMpRegeneration()
	{
		getStatus().stopHpMpRegeneration();
	}
	
	public final double getCurrentCp()
	{
		return getStatus().getCurrentCp();
	}
	
	public final void setCurrentCp(Double newCp)
	{
		setCurrentCp((double) newCp);
	}
	
	public final void setCurrentCp(double newCp)
	{
		getStatus().setCurrentCp(newCp);
	}
	
	public final double getCurrentHp()
	{
		return getStatus().getCurrentHp();
	}
	
	public final void setCurrentHp(double newHp)
	{
		getStatus().setCurrentHp(newHp);
	}
	
	public final void setCurrentHp(double newHp, boolean broadcast, boolean isDead)
	{
		getStatus().setCurrentHp(newHp, broadcast, isDead);
	}
	
	public final void setCurrentHpMp(double newHp, double newMp)
	{
		getStatus().setCurrentHpMp(newHp, newMp);
	}
	
	public final double getCurrentMp()
	{
		return getStatus().getCurrentMp();
	}
	
	public final void setCurrentMp(Double newMp)
	{
		setCurrentMp((double) newMp);
	}
	
	public final void setCurrentMp(double newMp)
	{
		getStatus().setCurrentMp(newMp);
	}
	
	public int getMaxLoad()
	{
		return 0;
	}
	
	public int getBonusWeightPenalty()
	{
		return 0;
	}
	
	public int getCurrentLoad()
	{
		return 0;
	}
	
	public int getMaxBuffCount()
	{
		final Effect effect = getFirstPassiveEffect(EffectType.ENLARGE_ABNORMAL_SLOT);
		return hasPremiumBonus() ? (Config.BUFFS_MAX_AMOUNT_PREMIUM + (effect == null ? 0 : (int) effect.calc())) : (Config.BUFFS_MAX_AMOUNT + (effect == null ? 0 : (int) effect.calc()));
	}
	
	public int getMaxDebuffCount()
	{
		return hasPremiumBonus() ? Config.DEBUFFS_MAX_AMOUNT_PREMIUM : Config.DEBUFFS_MAX_AMOUNT;
	}
	
	public void sendDamageMessage(Creature target, int damage, Skill skill, boolean mcrit, boolean pcrit, boolean miss)
	{
	}
	
	public FusionSkill getFusionSkill()
	{
		return _fusionSkill;
	}
	
	public void setFusionSkill(FusionSkill fb)
	{
		_fusionSkill = fb;
	}
	
	public byte getAttackElement()
	{
		return getStat().getAttackElement();
	}
	
	public int getAttackElementValue(byte attackAttribute)
	{
		return getStat().getAttackElementValue(attackAttribute);
	}
	
	public int getDefenseElementValue(byte defenseAttribute)
	{
		return getStat().getDefenseElementValue(defenseAttribute);
	}
	
	public final void startPhysicalAttackMuted()
	{
		abortAttack();
	}
	
	public void disableCoreAI(boolean val)
	{
		_AIdisabled = val;
	}
	
	public boolean isCoreAIDisabled()
	{
		return _AIdisabled;
	}
	
	public boolean giveRaidCurse()
	{
		return true;
	}
	
	public boolean isAffected(EffectFlag flag)
	{
		return _effects.isAffected(flag);
	}
	
	public void broadcastSocialAction(int id)
	{
		broadcastPacket(new SocialAction(getObjectId(), id));
	}
	
	public int getTeam()
	{
		return _team;
	}
	
	public void setTeam(int id)
	{
		if ((id >= 0) && (id <= 2))
		{
			_team = id;
		}
	}
	
	public void addOverrideCond(PcCondOverride... excs)
	{
		for (final PcCondOverride exc : excs)
		{
			_exceptions |= exc.getMask();
		}
	}
	
	public void removeOverridedCond(PcCondOverride... excs)
	{
		for (final PcCondOverride exc : excs)
		{
			_exceptions &= ~exc.getMask();
		}
	}
	
	public boolean canOverrideCond(PcCondOverride excs)
	{
		return (_exceptions & excs.getMask()) == excs.getMask();
	}
	
	public void setOverrideCond(long masks)
	{
		_exceptions = masks;
	}
	
	public Map<Integer, OptionsSkillHolder> getTriggerSkills()
	{
		if (_triggerSkills == null)
		{
			synchronized (this)
			{
				if (_triggerSkills == null)
				{
					_triggerSkills = new ConcurrentHashMap<>();
				}
			}
		}
		return _triggerSkills;
	}
	
	public void addTriggerSkill(OptionsSkillHolder holder)
	{
		getTriggerSkills().put(holder.getId(), holder);
	}
	
	public void removeTriggerSkill(OptionsSkillHolder holder)
	{
		getTriggerSkills().remove(holder.getId());
	}
	
	public void makeTriggerCast(Skill skill, Creature target)
	{
		try
		{
			if ((skill == null) || target.isDead())
			{
				return;
			}
			
			if (skill.checkCondition(this, target, false, true))
			{
				if (skill.triggersChanceSkill())
				{
					skill = SkillsParser.getInstance().getInfo(skill.getTriggeredChanceId(), skill.getTriggeredChanceLevel());
					if ((skill == null) || (skill.getSkillType() == SkillType.NOTDONE))
					{
						return;
					}
					
					if (!skill.checkCondition(this, target, false, true))
					{
						return;
					}
				}
				
				if (isSkillDisabled(skill) || isSkillBlocked(skill))
				{
					return;
				}
				
				if (!skill.isHandler() || skill.getReuseDelay() > 0)
				{
					disableSkill(skill, skill.getReuseDelay());
				}
				
				final GameObject[] targets = skill.getTargetList(this, false, target);
				
				if (targets.length == 0)
				{
					return;
				}
				
				final Creature firstTarget = (Creature) targets[0];
				
				if (Config.ALT_VALIDATE_TRIGGER_SKILLS && isPlayable() && (firstTarget != null) && firstTarget.isPlayable())
				{
					final Player player = getActingPlayer();
					if (!player.checkPvpSkill(firstTarget, skill, isSummon()))
					{
						return;
					}
				}
				
				broadcastPacket(new MagicSkillLaunched(this, skill.getDisplayId(), skill.getLevel(), targets));
				broadcastPacket(new MagicSkillUse(this, firstTarget, skill.getDisplayId(), skill.getLevel(), 0, 0));
				
				final ISkillHandler handler = SkillHandler.getInstance().getHandler(skill.getSkillType());
				if (handler != null)
				{
					handler.useSkill(this, skill, targets);
				}
				else
				{
					skill.useSkill(this, targets);
				}
			}
		}
		catch (final Exception e)
		{
			_log.warn("", e);
		}
	}
	
	public boolean canRevive()
	{
		if (isInsideZone(ZoneId.FUN_PVP))
		{
			final FunPvpZone zone = ZoneManager.getInstance().getZone(this, FunPvpZone.class);
			if (zone != null)
			{
				return !zone.canRevive();
			}
		}
		return true;
	}
	
	public void setCanRevive(boolean val)
	{
	}
	
	public boolean isSweepActive()
	{
		return false;
	}
	
	public boolean isPassiveSweepActive()
	{
		return false;
	}
	
	public int getClanId()
	{
		return 0;
	}
	
	public int getAllyId()
	{
		return 0;
	}
	
	public PremiumBonus getPremiumBonus()
	{
		return null;
	}
	
	public boolean hasPremiumBonus()
	{
		return false;
	}
	
	public void block()
	{
		_blocked = true;
	}
	
	public void unblock()
	{
		_blocked = false;
	}
	
	public boolean isBlocked()
	{
		return _blocked;
	}
	
	public boolean isInCategory(CategoryType type)
	{
		return false;
	}
	
	@Override
	public boolean isCreature()
	{
		return true;
	}
	
	public boolean isShowSummonAnimation()
	{
		return _showSummonAnimation;
	}
	
	public void setShowSummonAnimation(boolean showSummonAnimation)
	{
		_showSummonAnimation = showSummonAnimation;
	}
	
	public boolean isInZonePeace()
	{
		return isInsideZone(ZoneId.PEACE) || isInTownZone();
	}
	
	public boolean isInTownZone()
	{
		return isInsideZone(ZoneId.TOWN) && TownManager.getTownZone(getX(), getY(), getZ()) != null;
	}
	
	public void sayString(String text, int type)
	{
		broadcastPacket(new CreatureSay(getObjectId(), type, getName(null), text));
	}
	
	public void rndWalk()
	{
		int posX = getX();
		int posY = getY();
		final int posZ = getZ();
		switch (Rnd.get(1, 6))
		{
			case 1 :
				posX += 140;
				posY += 280;
				break;
			case 2 :
				posX += 250;
				posY += 150;
				break;
			case 3 :
				posX += 169;
				posY -= 200;
				break;
			case 4 :
				posX += 110;
				posY -= 200;
				break;
			case 5 :
				posX -= 250;
				posY -= 120;
				break;
			case 6 :
				posX -= 200;
				posY += 160;
		}
		
		if (GeoEngine.getInstance().canMoveToCoord(this, getX(), getY(), getZ(), posX, posY, posZ, getReflection(), false))
		{
			setRunning();
			getAI().setIntention(CtrlIntention.MOVING, new Location(posX, posY, posZ), 0);
		}
	}
	
	public int calcHeading(int x, int y)
	{
		return (int) (Math.atan2(getY() - y, getX() - x) * 10430.378350470453D) + 32768;
	}
	
	public void teleToClosestTown()
	{
	}

	public int getMinDistance(GameObject obj)
	{
		int distance = (int) getColRadius();
		if (obj != null)
		{
			distance += obj.getColRadius();
		}
		return distance;
	}

	public Location getFlyLocation(GameObject target, Skill skill)
	{
		if (target != null && target != this)
		{
			Location loc;
			
			final double radian = PositionUtils.convertHeadingToRadian(target.getHeading());
			if (skill.isFlyToBack())
			{
				loc = new Location(target.getX() + (int) (Math.sin(radian) * 40), target.getY() - (int) (Math.cos(radian) * 40), target.getZ());
			}
			else
			{
				loc = new Location(target.getX() - (int) (Math.sin(radian) * 40), target.getY() + (int) (Math.cos(radian) * 40), target.getZ());
			}
			
			if (isFlying())
			{
				if (isPlayer() && (isTransformed() && (loc.getZ() <= 0 || loc.getZ() >= 6000)))
				{
					return null;
				}
			}
			else
			{
				loc.correctGeoZ();
				if (!GeoEngine.getInstance().canMoveToCoord(this, getX(), getY(), getZ(), loc.getX(), loc.getY(), loc.getZ(), getReflection(), true))
				{
					loc = target.getLocation();
					if (!GeoEngine.getInstance().canMoveToCoord(this, getX(), getY(), getZ(), loc.getX(), loc.getY(), loc.getZ(), getReflection(), true))
					{
						return null;
					}
				}
			}
			return loc;
		}
		
		final double radian = PositionUtils.convertHeadingToRadian(getHeading());
		final int x1 = -(int) (Math.sin(radian) * skill.getFlyRadius());
		final int y1 = (int) (Math.cos(radian) * skill.getFlyRadius());
		
		if (isFlying())
		{
			return new Location(getX() + x1, getY() + y1, getZ());
		}
		return GeoEngine.getInstance().moveDoorCheck(this, getX() + x1, getY() + y1, getZ(), getReflection());
	}

	protected void finishFly()
	{
		final Location flyLoc = _flyLoc;
		_flyLoc = null;
		if (flyLoc != null)
		{
			setXYZ(flyLoc.getX(), flyLoc.getY(), flyLoc.getZ());
			validateLocation(2);
			revalidateZone(true);
			if (_flyDestLoc != null)
			{
				getAI().setIntention(CtrlIntention.MOVING, _flyDestLoc, 0);
				_flyDestLoc = null;
			}
		}
	}
	
	public void correctFallingPosition(Location loc)
	{
		if (!_isCorrectPos)
		{
			_isCorrectPos = true;
			final int z = isFloating() ? getZ() : GeoEngine.getInstance().getHeight(loc.getX(), loc.getY(), getZ());
			final var zDiff = Math.abs(z - loc.getZ());
			final int correctZ = zDiff > 400 ? getZ() : z;
			if (isMoving())
			{
				_move._xAccurate = loc.getX();
				_move._yAccurate = loc.getY();
				_move._zAccurate = correctZ;
			}
			super.setXYZ(loc.getX(), loc.getY(), correctZ);
			if (loc.getZ() <= -16000 || zDiff > 200)
			{
				validateLocation(0);
			}
			_isCorrectPos = false;
		}
	}
	
	public boolean isFloating()
	{
		return (isInWater() || isFlying() || isVehicle() || isInVehicle()) || isInsideZone(ZoneId.NO_GEO);
	}

	public final boolean isIsDanceStun()
	{
		return _isDanceStun;
	}
	
	public final void setIsDanceStun(boolean mode)
	{
		_isDanceStun = mode;
	}
	
	public void addInvulAgainst(int skillId, int skillLvl)
	{
		final InvulSkillHolder invulHolder = getInvulAgainstSkills().get(skillId);
		if (invulHolder != null)
		{
			invulHolder.increaseInstances();
			return;
		}
		getInvulAgainstSkills().put(skillId, new InvulSkillHolder(skillId, skillLvl));
	}
	
	private Map<Integer, InvulSkillHolder> getInvulAgainstSkills()
	{
		if (_invulAgainst == null)
		{
			synchronized (this)
			{
				if (_invulAgainst == null)
				{
					return _invulAgainst = new ConcurrentHashMap<>();
				}
			}
		}
		return _invulAgainst;
	}
	
	public void removeInvulAgainst(int skillId, int skillLvl)
	{
		final InvulSkillHolder invulHolder = getInvulAgainstSkills().get(skillId);
		if (invulHolder != null)
		{
			if (invulHolder.decreaseInstances() < 1)
			{
				_invulAgainst.remove(skillId);
			}
		}
	}
	
	public boolean isInvulAgainst(int skillId, int skillLvl)
	{
		final InvulSkillHolder invulHolder = getInvulAgainstSkills().get(skillId);
		if (invulHolder != null)
		{
			return invulHolder.getLvl() < 1 || invulHolder.getLvl() == skillLvl;
		}
		return false;
	}
	
	public final double getCurrentHpRatio()
	{
		return getCurrentHp() / getMaxHp();
	}

	public final double getCurrentHpPercents()
	{
		return getCurrentHpRatio() * 100.;
	}

	public final boolean isCurrentHpFull()
	{
		return (int) getCurrentHp() >= (int) getMaxHp();
	}

	public final double getCurrentMpRatio()
	{
		return getCurrentMp() / getMaxMp();
	}
	
	public final double getCurrentMpPercents()
	{
		return getCurrentMpRatio() * 100.;
	}

	public final double getCurrentCpRatio()
	{
		return getCurrentCp() / getMaxCp();
	}
	
	public final double getCurrentCpPercents()
	{
		return getCurrentCpRatio() * 100.;
	}

	public void setChampionTemplate(final ChampionTemplate championTemplate)
	{
		_championTemplate = championTemplate;
	}
	
	public ChampionTemplate getChampionTemplate()
	{
		return _championTemplate;
	}

	public AbstractFightEvent getFightEvent()
	{
		return getEvent(AbstractFightEvent.class);
	}
	
	public boolean isRegisteredInFightEvent()
	{
		return getEvent(AbstractFightEvent.class) != null;
	}
	
	public boolean isInFightEvent()
	{
		try
		{
			if (getEvent(AbstractFightEvent.class) == null)
			{
				return false;
			}

			return getEvent(AbstractFightEvent.class).getFightEventPlayer(this) != null;
		}
		catch (final NullPointerException e)
		{
			return false;
		}
	}

	/** Tournaments **/
	private Tournament partyTournament;
	private TournamentTeam tournamentTeam;
	private TournamentStats tournamentStats;
	private final Location lastLocation = new Location(0, 0, 0);

	public void setPartyTournament(final Tournament tournament)
	{
		partyTournament = tournament;
	}

	public Tournament getPartyTournament()
	{
		return partyTournament;
	}

	public boolean isInPartyTournament()
	{
		return Objects.nonNull(getPartyTournament());
	}

	public boolean isInPartyTournamentWith(final Creature target)
	{
		if (!(target instanceof Playable))
		{
			return false;
		}

		if (!isInPartyTournament() || !target.getActingPlayer().isInPartyTournament())
		{
			return false;
		}

		return getPartyTournament().getId() == target.getActingPlayer().getPartyTournament().getId();
	}

	public boolean isOnSameSidePartyTournamentWith(final Creature target)
	{
		if (!isInPartyTournamentWith(target))
		{
			return false;
		}

		return getTeam() == target.getActingPlayer().getTeam();
	}

	public Location getLastLocation()
	{
		return lastLocation;
	}

	public TournamentTeam getTournamentTeam()
	{
		return tournamentTeam;
	}

	public void setTournamentTeam(TournamentTeam tournamentTeam)
	{
		this.tournamentTeam = tournamentTeam;
	}

	public boolean isInTournamentTeam()
	{
		return tournamentTeam != null;
	}

	public TournamentStats getTournamentStats()
	{
		return tournamentStats;
	}

	public void setTournamentStats(TournamentStats tournamentStats)
	{
		this.tournamentStats = tournamentStats;
	}
	public boolean isRegisteredTournament()
	{
		return TournamentData.getInstance().isInLobbySolo(this) || (isInTournamentTeam() && TournamentData.getInstance().isInLobby(tournamentTeam));
	}
	public boolean checkInTournament()
	{
		return isRegisteredTournament() || isInPartyTournament();
	}

	public void startHealBlocked(boolean blocked)
	{
		_isHealBlocked = blocked;
	}
	
	public boolean isHealBlocked()
	{
		return isAlikeDead() || _isHealBlocked;
	}
	
	public boolean isBuffImmune()
	{
		return _buffImmunity || calcStat(Stats.BUFF_IMMUNITY, 0, null, null) > 0;
	}
	
	public boolean isDebuffImmune()
	{
		return _debuffImmunity;
	}
	
	public boolean startBuffImmunity(boolean value)
	{
		return _buffImmunity = value;
	}
	
	public boolean startDebuffImmunity(boolean value)
	{
		return _debuffImmunity = value;
	}
	
	public void sendActionFailed()
	{
		sendPacket(ActionFail.STATIC_PACKET);
	}
	
	public CharListenerList getListeners()
	{
		if (_listeners == null)
		{
			synchronized (this)
			{
				if (_listeners == null)
				{
					_listeners = new CharListenerList(this);
				}
			}
		}
		return _listeners;
	}
	
	public <T extends Listener<Creature>> boolean addListener(final T listener)
	{
		return getListeners().add(listener);
	}
	
	public <T extends Listener<Creature>> boolean removeListener(final T listener)
	{
		return getListeners().remove(listener);
	}
	
	public void onEvtTimer(int timerId, Object arg1)
	{
	}
	
	public boolean isCancelAction()
	{
		return false;
	}
	
	public void broadcastCharInfo(UserInfoType... types)
	{}
	
	public void broadcastCharInfoImpl()
	{}
	
	public Location getMinionPosition()
	{
		return null;
	}
	
	public boolean isLethalImmune()
	{
		return false;
	}
	
	public boolean isGlobalAI()
	{
		return false;
	}
	
	@Override
	public double getColRadius()
	{
		return getTemplate().getCollisionRadius();
	}
	
	@Override
	public double getColHeight()
	{
		return getTemplate().getCollisionHeight();
	}
	
	public Creature getCastingTarget()
	{
		return _castingTarget;
	}
	
	public void setCastingTarget(final Creature target)
	{
		_castingTarget = target;
	}
	
	public Skill getCastingSkill()
	{
		return _castingSkill;
	}
	
	public boolean isCriticalBlowCastingSkill()
	{
		return _isCriticalBlowCastingSkill;
	}
	
	public TargetDirection getDirection()
	{
		return _direction;
	}
	
	public void setDirection(TargetDirection val)
	{
		_direction = val;
	}
	
	public long getAnimationEndTime()
	{
		return _animationEndTime;
	}
	
	@Override
	public void removeInfoObject(GameObject object)
	{
		if (object == getTarget())
		{
			setTarget(null);
		}
	}
	
	public boolean isFakePlayer()
	{
		return false;
	}
	
	public AutoFarmOptions getFarmSystem()
	{
		return null;
	}
	
	public boolean checkEffectRangeInsidePeaceZone(Skill skill, final int x, final int y, final int z)
	{
		final List<ZoneType> zones = ZoneManager.getInstance().getZones(getX(), getY(), getZ());
		if (zones != null && !zones.isEmpty())
		{
			final int range = skill.getEffectRange();
			final int up = y + range;
			final int down = y - range;
			final int left = x + range;
			final int right = x - range;
			
			for (final ZoneType e : zones)
			{
				if (e instanceof PeaceZone)
				{
					if (e.isInsideZone(x, up, z))
					{
						return false;
					}
					
					if (e.isInsideZone(x, down, z))
					{
						return false;
					}
					
					if (e.isInsideZone(left, y, z))
					{
						return false;
					}
					
					if (e.isInsideZone(right, y, z))
					{
						return false;
					}
					
					if (e.isInsideZone(x, y, z))
					{
						return false;
					}
				}
			}
		}
		return true;
	}
	
	public void onDeathInZones(Creature character)
	{
		final List<ZoneType> zones = ZoneManager.getInstance().getZones(getX(), getY(), getZ());
		if (zones != null && !zones.isEmpty())
		{
			for (final ZoneType zone : zones)
			{
				if (zone != null)
				{
					zone.onDieInside(this);
				}
			}
		}
	}
	
	public void onReviveInZones(Creature character)
	{
		final List<ZoneType> zones = ZoneManager.getInstance().getZones(getX(), getY(), getZ());
		if (zones != null && !zones.isEmpty())
		{
			for (final ZoneType zone : zones)
			{
				if (zone != null)
				{
					zone.onReviveInside(this);
				}
			}
		}
	}
	
	public boolean containsZone(int zoneId)
	{
		final List<ZoneType> zones = ZoneManager.getInstance().getZones(getX(), getY(), getZ());
		if (zones != null && !zones.isEmpty())
		{
			for (final ZoneType zone : zones)
			{
				if (zone != null && zone.getId() == zoneId)
				{
					return true;
				}
			}
		}
		return false;
	}
	
	public boolean isFalling()
	{
		return false;
	}
	
	public boolean isHFClient()
	{
		return true;
	}
	
	public void validateLocation(int broadcast)
	{
		final GameServerPacket sp = new ValidateLocation(this);
		switch (broadcast)
		{
			case 0 :
				sendPacket(sp);
				break;
			case 1 :
				broadcastPacket(sp);
				break;
			default :
				broadcastPacketToOthers(sp);
				break;
		}
	}
	
	public ToggleList getToggleList()
	{
		return _toggleList;
	}
	
	public int[] getWaterZ()
	{
		final int[] waterZ = new int[]
		{
		        Integer.MIN_VALUE, Integer.MAX_VALUE
		};
		
		if (!isInWater())
		{
			return waterZ;
		}
		
		for (int i = 0; i < _zoneList.size(); i++)
		{
			final var zone = _zoneList.get(i);
			if (zone instanceof WaterZone)
			{
				final var water = (WaterZone) zone;
				if ((waterZ[0] == Integer.MIN_VALUE) || (waterZ[0] > water.getWaterMinZ()))
				{
					waterZ[0] = water.getWaterMinZ();
				}
				if ((waterZ[1] == Integer.MAX_VALUE) || (waterZ[1] < water.getWaterZ()))
				{
					waterZ[1] = water.getWaterZ();
				}
			}
		}
		return waterZ;
	}

	public final int getXdestination()
	{
		final MoveData m = _move;
		
		if (m != null)
		{
			return m._xDestination;
		}
		return getX();
	}
	
	public final int getYdestination()
	{
		final MoveData m = _move;
		
		if (m != null)
		{
			return m._yDestination;
		}
		return getY();
	}
	
	public final int getZdestination()
	{
		final MoveData m = _move;
		
		if (m != null)
		{
			return m._zDestination;
		}
		return getZ();
	}
	
	public Location getDestination()
	{
		final MoveData m = _move;
		if (m != null)
		{
			return new Location(m._xDestination, m._yDestination, m._zDestination);
		}
		return getLocation();
	}
	
	public final boolean isMoving()
	{
		return _move != null;
	}
	
	public final boolean isInPathFinding()
	{
		final MoveData m = _move;
		if (m == null)
		{
			return false;
		}
		if (m._pathFindIndex == -1)
		{
			return false;
		}
		if (m._pathFindIndex == (m._moveList.size() - 1))
		{
			return false;
		}
		return true;
	}
	
	public boolean updatePosition()
	{
		final MoveData m = _move;
		if (m == null || _isCorrectPos)
		{
			return true;
		}
		
		if (!isVisible())
		{
			stopMove(false);
			return true;
		}
		
		if (m._moveTimestamp == 0)
		{
			m._moveTimestamp = m._moveStartTime;
			m._xAccurate = getX();
			m._yAccurate = getY();
		}
		
		final int gameTicks = GameTimeController.getInstance().getGameTicks();
		if (m._moveTimestamp == gameTicks)
		{
			return false;
		}
		m._tick++;
		
		final int xPrev = getX();
		final int yPrev = getY();
		int zPrev = getZ();
		final var moveSpeed = getMoveSpeed();
		double dx, dy, dz;
		if (Config.COORD_SYNCHRONIZE)
		{
			dx = m._xDestination - xPrev;
			dy = m._yDestination - yPrev;
		}
		else
		{
			dx = m._xDestination - m._xAccurate;
			dy = m._yDestination - m._yAccurate;
		}
		dz = m._zDestination - zPrev;
		
		final WaterZone waterZone = ZoneManager.getInstance().getZone(this, WaterZone.class);
		final var isInWater = isInWater() && waterZone != null;
		final var isFloating = isFloating() || isInWater;
		Location clientLoc = null;
		final var isPlayer = getActingPlayer() != null;
		if (isPlayer && !isFakePlayer())
		{
			clientLoc = getActingPlayer().getClientLoc();
		}
		
		if ((!Config.COORD_SYNCHRONIZE) && !isFloating && !m._isIgnoreGeo && ((GameTimeController.getInstance().getGameTicks() % 10) == 0) && GeoEngine.getInstance().hasGeo(xPrev, yPrev))
		{
			final int geoHeight = GeoEngine.getInstance().getSpawnHeight(xPrev, yPrev, zPrev);
			dz = m._zDestination - geoHeight;
			if (clientLoc != null && (Math.abs(clientLoc.getZ() - geoHeight) > 200) && (Math.abs(clientLoc.getZ() - geoHeight) < 1500))
			{
				dz = m._zDestination - zPrev;
			}
			else if (isInCombat() && (Math.abs(dz) > 200) && (((dx * dx) + (dy * dy)) < 40000))
			{
				dz = m._zDestination - zPrev;
			}
			else
			{
				zPrev = geoHeight;
			}
		}
		else
		{
			dz = m._zDestination - zPrev;
		}
		
		if (isPlayer && !isFlying() && _keyboardMovement)
		{
			final double angle = Util.convertHeadingToDegree(getHeading());
			final double radian = Math.toRadians(angle);
			final double course = Math.toRadians(180);
			final double frontDistance = 10 * (moveSpeed / 100);
			final int x1 = (int) (Math.cos(Math.PI + radian + course) * frontDistance);
			final int y1 = (int) (Math.sin(Math.PI + radian + course) * frontDistance);
			final int x = xPrev + x1;
			final int y = yPrev + y1;
			if (!GeoEngine.getInstance().canMoveToCoord(this, xPrev, yPrev, zPrev, x, y, zPrev, getReflection(), true))
			{
				_move._pathFindIndex = -1;
				stopMove(null);
				return true;
			}
		}
		
		double delta = (dx * dx) + (dy * dy);
		if ((delta < 10000) && ((dz * dz) > 2500) && !isFloating)
		{
			delta = Math.sqrt(delta);
		}
		else
		{
			delta = Math.sqrt(delta + (dz * dz));
		}
		
		double distFraction = Double.MAX_VALUE;
		if (delta > 1)
		{
			final double distPassed = (getStat().getRealMoveSpeed(!isFlying() && moveSpeed > Config.GEO_MOVE_SPEED && m._tick <= Config.GEO_MOVE_TICK) * (gameTicks - m._moveTimestamp)) / GameTimeController.TICKS_PER_SECOND;
			distFraction = distPassed / delta;
		}
		
		if (distFraction > 1)
		{
			int z = m._zDestination;
			if (isInWater && z < waterZone.getWaterMinZ())
			{
				z = waterZone.getWaterMinZ();
			}
			else
			{
				z = getGeoZ(m._xDestination, m._yDestination, m._zDestination);
				final double zDiff = Math.abs(z - m._zDestination);
				if (zDiff > 100 || isFloating)
				{
					if (!isFloating && !GeoEngine.getInstance().canMoveToCoord(this, xPrev, yPrev, zPrev, m._xDestination, m._yDestination, m._zDestination, getReflection(), false))
					{
						stopMove(null);
						return true;
					}
					z = m._zDestination;
				}
			}
			super.setXYZ(m._xDestination, m._yDestination, z);
			revalidateZone(true);
		}
		else
		{
			final double x = dx * distFraction;
			final double y = dy * distFraction;
			m._xAccurate += x;
			m._yAccurate += y;
			m._zAccurate = (zPrev + (int) ((dz * distFraction) + 0.5));
			int z = (int) m._zAccurate;
			if (!isFloating)
			{
				z = getGeoZ((int) m._xAccurate, (int) m._yAccurate, z);
				double zDiff = Math.abs(z - zPrev);
				if (zDiff > 100 && !isFalling())
				{
					if (!GeoEngine.getInstance().canMoveToCoord(this, xPrev, yPrev, zPrev, (int) (m._xAccurate), (int) (m._yAccurate), z, getReflection(), false))
					{
						stopMove(null);
						return true;
					}
				}
				
				if (clientLoc != null)
				{
					zDiff = Math.abs(z - clientLoc.getZ());
					if (zDiff > 150 && _validationInterval < System.currentTimeMillis())
					{
						_validationInterval = System.currentTimeMillis() + 1000L;
						validateLocation(0);
					}
				}
			}
			
			if (isInWater && z < waterZone.getWaterMinZ())
			{
				z = waterZone.getWaterMinZ();
			}
			
			if (Config.ALLOW_DOOR_VALIDATE && isPlayable() && (m._tick % 2 != 0))
			{
				if (isInFrontDoor((int) (m._xAccurate + (x * 2)), (int) (m._yAccurate + (y * 2)), zPrev, getReflection()))
				{
					stopMove(null);
					return true;
				}
			}
			
			super.setXYZ((int) (m._xAccurate), (int) (m._yAccurate), z);
			revalidateZone(false);
		}
		
		m._moveTimestamp = gameTicks;
		
		if (distFraction > 1)
		{
			if (!isPlayer() && !isVehicle())
			{
				stopMove(true);
			}
			ThreadPoolManager.getInstance().execute(new NotifyAITask(this, CtrlEvent.EVT_ARRIVED));
			return true;
		}
		return false;
	}
	
	public void stopMove(boolean broadcast)
	{
			_move = null;
			_keyboardMovement = false;
			if (broadcast)
			{
				broadcastPacket(new StopMove(this));
			}
	}
	
	public void stopMove(Location loc)
	{
			_move = null;
			_keyboardMovement = false;
			if (loc != null)
			{
				setXYZ(loc.getX(), loc.getY(), loc.getZ());
				setHeading(loc.getHeading());
				revalidateZone(true);
			}
			broadcastPacket(new StopMove(this));
	}
	
	public void moveToLocation(int x, int y, int z, int offset)
	{
		if (Config.CHECK_ATTACK_STATUS_TO_MOVE)
		{
			if (isAttackingNow() || isCastingNow())
			{
				return;
			}
		}
		
		final double speed = getMoveSpeed();
		if ((speed <= 0) || isMovementDisabled())
		{
			return;
		}
		
		final int curX = super.getX();
		final int curY = super.getY();
		final int curZ = super.getZ();
		
		double dx = (x - curX);
		double dy = (y - curY);
		double dz = (z - curZ);
		double distance = Math.hypot(dx, dy);
		
		final boolean verticalMovementOnly = isFlying() && (distance == 0) && (dz != 0);
		if (verticalMovementOnly)
		{
			distance = Math.abs(dz);
		}
		
		double cos;
		double sin;
		
		if ((offset > 0) || (distance < 1))
		{
			offset -= Math.abs(dz);
			if (offset < 5)
			{
				offset = 5;
			}
			
			if ((distance < 1) || ((distance - offset) <= 0))
			{
				getAI().notifyEvent(CtrlEvent.EVT_ARRIVED);
				return;
			}
			sin = dy / distance;
			cos = dx / distance;
			
			distance -= (offset - 5);
			
			x = curX + (int) (distance * cos);
			y = curY + (int) (distance * sin);
		}
		else
		{
			sin = dy / distance;
			cos = dx / distance;
		}
		
		final MoveData m = new MoveData();
		m._pathFindIndex = -1;
		m._isIgnoreGeo = false;
		final var waterZone = ZoneManager.getInstance().getZone(this, WaterZone.class);
		final var isInWater = waterZone != null && isInWater();
		final var noGeoZone = ZoneManager.getInstance().getZone(this, NoGeoZone.class);
		final var isNoGeoZone = noGeoZone != null && !noGeoZone.isMoveCheck();
		if (!isNoGeoZone && !isFlying())
		{
			if (isInVehicle())
			{
				m._isIgnoreGeo = true;
			}
			
			final double originalDistance = distance;
			int originalX = x;
			int originalY = y;
			final int originalZ = z;
			final int gtx = (originalX - World.MAP_MIN_X) >> 4;
			final int gty = (originalY - World.MAP_MIN_Y) >> 4;
			
			if (Config.PATHFIND_BOOST)
			{
				if (isInPathFinding())
				{
					try
					{
						if ((gtx == _move.geoPathGtx) && (gty == _move.geoPathGty))
						{
							return;
						}
						_move._pathFindIndex = -1;
					}
					catch (final NullPointerException e)
					{
					}
				}
				
				if ((curX < World.MAP_MIN_X) || (curX > World.MAP_MAX_X) || (curY < World.MAP_MIN_Y) || (curY > World.MAP_MAX_Y))
				{
					if (Config.DEBUG)
					{
						_log.warn("Character " + getName(null) + " outside world area, in coordinates x:" + curX + " y:" + curY);
					}
					getAI().setIntention(CtrlIntention.IDLE);
					if (isPlayer())
					{
						getActingPlayer().logout();
					}
					else if (isSummon())
					{
						return;
					}
					else
					{
						onDecay();
					}
					return;
				}
				
				var destiny = isInWater ? GeoEngine.getInstance().moveWaterCheck(this, x, y, z, getReflection(), getWaterZ()) : GeoEngine.getInstance().moveCheck(this, curX, curY, curZ, x, y, z, getReflection(), Math.abs(curZ - z) < 100);
				if (isInWater)
				{
					final int diff = z - destiny.getZ();
					if (diff > 0 && diff < 128)
					{
						destiny = GeoEngine.getInstance().moveCheck(this, destiny.getX(), destiny.getY(), destiny.getZ(), x, y, z, getReflection());
					}
				}
				x = destiny.getX();
				y = destiny.getY();
				z = destiny.getZ();
				dx = x - curX;
				dy = y - curY;
				dz = z - curZ;
				distance = verticalMovementOnly ? Math.abs(dz * dz) : Math.sqrt((dx * dx) + (dy * dy));
			}
			
			if ((Config.PATHFIND_BOOST) && ((originalDistance - distance) > 30) && !isFloating() && !_keyboardMovement && !isFalling() && !isInWater(this))
			{
				m._moveList = PathFinding.getInstance().findPath(this, curX, curY, curZ, originalX, originalY, originalZ, getReflection(), isPlayable(), false);
				boolean found = (m._moveList != null) && (m._moveList.size() > 1);
				if (!found && isAttackable() && hasAI() && getAI().isWalkingToHome())
				{
					int xMin = Math.min(curX, originalX);
					int xMax = Math.max(curX, originalX);
					int yMin = Math.min(curY, originalY);
					int yMax = Math.max(curY, originalY);
					final double maxDiff = Math.min(Math.max(xMax - xMin, yMax - yMin), 500);
					xMin -= maxDiff;
					xMax += maxDiff;
					yMin -= maxDiff;
					yMax += maxDiff;
					int destinationX = 0;
					int destinationY = 0;
					double shortDistance = Double.MAX_VALUE;
					double tempDistance;
					List<AbstractNodeLoc> tempPath;
					for (int sX = xMin; sX < xMax; sX += 500)
					{
						for (int sY = yMin; sY < yMax; sY += 500)
						{
							tempDistance = Math.hypot(sX - originalX, sY - originalY);
							if (tempDistance < shortDistance)
							{
								tempPath = PathFinding.getInstance().findPath(this, curX, curY, curZ, sX, sY, originalZ, getReflection(), isPlayable(), false);
								found = (tempPath != null) && (tempPath.size() > 1);
								if (found)
								{
									shortDistance = tempDistance;
									m._moveList = tempPath;
									destinationX = sX;
									destinationY = sY;
									break;
								}
							}
						}
					}
					found = (m._moveList != null) && (m._moveList.size() > 1);
					if (found)
					{
						originalX = destinationX;
						originalY = destinationY;
					}
				}
				
				if (found)
				{
					m._pathFindIndex = 0;
					m.geoPathGtx = gtx;
					m.geoPathGty = gty;
					m.geoPathAccurateTx = originalX;
					m.geoPathAccurateTy = originalY;
					
					x = m._moveList.get(m._pathFindIndex).getX();
					y = m._moveList.get(m._pathFindIndex).getY();
					z = m._moveList.get(m._pathFindIndex).getZ();
					
					if (!Config.ALLOW_DOOR_VALIDATE || !isPlayable())
					{
						if (isInFrontDoor(x, y, z, getReflection()))
						{
							m._moveList = null;
							getAI().notifyEvent(CtrlEvent.EVT_ARRIVED);
							return;
						}
						
						for (int i = 0; i < (m._moveList.size() - 1); i++)
						{
							if (isInFrontDoor(m._moveList.get(i).getLocation(), m._moveList.get(i + 1).getLocation(), getReflection()))
							{
								m._moveList = null;
								getAI().notifyEvent(CtrlEvent.EVT_ARRIVED);
								return;
							}
						}
					}
					dx = x - curX;
					dy = y - curY;
					dz = z - curZ;
					distance = verticalMovementOnly ? Math.abs(dz * dz) : Math.sqrt((dx * dx) + (dy * dy));
					sin = dy / distance;
					cos = dx / distance;
				}
				else
				{
					if ((isPlayer() || isNpc() || isSummon()) && distance < 30)
					{
						if (isMonster() || isRaid() && (Math.abs(z - curZ) > 200))
						{
							((Attackable) this).clearAggroList(true);
							stopMove(null);
						}
						getAI().notifyEvent(CtrlEvent.EVT_ARRIVED);
						return;
					}
					m._isIgnoreGeo = true;
				}
			}
			
			if ((distance < 1 && Config.PATHFIND_BOOST) || isAfraid())
			{
				getAI().notifyEvent(CtrlEvent.EVT_ARRIVED);
				return;
			}
		}
		
		if ((isFlying() || isInWater(this)) && !verticalMovementOnly)
		{
			distance = Math.hypot(distance, dz);
		}
		
		final int ticksToMove = 1 + (int) ((GameTimeController.TICKS_PER_SECOND * distance) / speed);
		m._xDestination = x;
		m._yDestination = y;
		m._zDestination = z;
		m._heading = 0;
		if (distance != 0)
		{
			setHeading(PositionUtils.calculateHeadingFrom(curX, curY, x, y));
		}
		
		m._moveStartTime = GameTimeController.getInstance().getGameTicks();
		_move = m;
		
		GameTimeController.getInstance().registerMovingObject(this);
		
		if ((ticksToMove * GameTimeController.MILLIS_IN_TICK) > 3000)
		{
			ThreadPoolManager.getInstance().schedule(new NotifyAITask(this, CtrlEvent.EVT_ARRIVED_TARGET), 2000);
		}
	}
	
	public boolean moveToNextRoutePoint()
	{
		if (!isInPathFinding())
		{
			stopMove(false);
			return false;
		}
		final double speed = getMoveSpeed();
		if ((speed <= 0) || isMovementDisabled())
		{
			stopMove(false);
			return false;
		}
		
		final MoveData md = _move;
		if ((md == null) || (md._moveList == null))
		{
			return false;
		}
		
		if (isPlayer() && getAI().getNextAction() != null && getAI().getNextAction().isAttackAction())
		{
			if (getAI().getNextAction().isCanAtivate())
			{
				stopMove(null);
				getAI().notifyEvent(CtrlEvent.EVT_ARRIVED_ATTACK);
				return false;
			}
		}
		
		try
		{
			final MoveData m = new MoveData();
			m._pathFindIndex = md._pathFindIndex + 1;
			m._moveList = md._moveList;
			m.geoPathGtx = md.geoPathGtx;
			m.geoPathGty = md.geoPathGty;
			m.geoPathAccurateTx = md.geoPathAccurateTx;
			m.geoPathAccurateTy = md.geoPathAccurateTy;
			
			if (md._pathFindIndex == (md._moveList.size() - 2))
			{
				m._xDestination = md.geoPathAccurateTx;
				m._yDestination = md.geoPathAccurateTy;
				m._zDestination = md._moveList.get(m._pathFindIndex).getZ();
			}
			else
			{
				m._xDestination = md._moveList.get(m._pathFindIndex).getX();
				m._yDestination = md._moveList.get(m._pathFindIndex).getY();
				m._zDestination = md._moveList.get(m._pathFindIndex).getZ();
			}
			
			final double dx = (m._xDestination - super.getX());
			final double dy = (m._yDestination - super.getY());
			final double distance = Math.sqrt((dx * dx) + (dy * dy));
			
			if (distance != 0)
			{
				setHeading(Util.calculateHeadingFrom(getX(), getY(), m._xDestination, m._yDestination));
			}
			
			final int ticksToMove = 1 + (int) ((10 * distance) / speed);
			
			m._heading = 0;
			m._moveStartTime = GameTimeController.getInstance().getGameTicks();
			
			_move = m;
			
			GameTimeController.getInstance().registerMovingObject(this);
			
			if ((ticksToMove * GameTimeController.MILLIS_IN_TICK) > 3000)
			{
				ThreadPoolManager.getInstance().schedule(new NotifyAITask(this, CtrlEvent.EVT_ARRIVED_TARGET), 2000);
			}
			broadcastPacket(new MoveToLocation(this));
			if (isNpc() || (Config.ALLOW_GEOMOVE_VALIDATE && speed > Config.GEO_MOVE_SPEED))
			{
				validateLocation(1);
			}
		}
		catch (final Exception e)
		{
			if (Config.DEBUG)
			{
				_log.warn(getClass().getSimpleName() + ": moveToNextRoutePoint() failed.", e);
			}
		}
		return true;
	}
	
	public boolean validateMovementHeading(int heading)
	{
		final MoveData m = _move;
		if (m == null)
		{
			return true;
		}
		
		boolean result = true;
		if (m._heading != heading)
		{
			result = (m._heading == 0);
			m._heading = heading;
		}
		return result;
	}
	
	public void setKeyboardMovement(boolean value)
	{
		_keyboardMovement = value;
	}
	
	@Override
	public int getGeoZ(int x, int y, int z)
	{
		if (isFloating() || isDoor())
		{
			return z;
		}
		return super.getGeoZ(x, y, z);
	}
	
	public boolean isCanAbsorbDamage(Creature target)
	{
		return true;
	}
	
	public double getPhysAttributteMod()
	{
		return 0.;
	}
	
	public double getMagicAttributteMod()
	{
		return 0.;
	}
}