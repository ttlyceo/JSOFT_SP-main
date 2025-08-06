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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.geodata.GeoEngine;
import l2e.gameserver.handler.actionhandlers.ActionHandler;
import l2e.gameserver.handler.actionshifthandlers.ActionShiftHandler;
import l2e.gameserver.idfactory.IdFactory;
import l2e.gameserver.instancemanager.MapRegionManager;
import l2e.gameserver.instancemanager.PvpColorManager;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.DoorInstance;
import l2e.gameserver.model.entity.Reflection;
import l2e.gameserver.model.entity.events.AbstractFightEvent;
import l2e.gameserver.model.entity.events.model.FightEventOwner;
import l2e.gameserver.model.interfaces.IIdentifiable;
import l2e.gameserver.model.interfaces.ILocational;
import l2e.gameserver.model.interfaces.IPositionable;
import l2e.gameserver.model.stats.StatsSet;
import l2e.gameserver.model.zone.ZoneId;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.DeleteObject;
import l2e.gameserver.network.serverpackets.ExSendUIEvent;
import l2e.gameserver.network.serverpackets.GameServerPacket;

public abstract class GameObject extends FightEventOwner implements IIdentifiable, IPositionable
{
	public static final Logger _log = LoggerFactory.getLogger(GameObject.class);
	
	private boolean _isInvisible;
	private boolean _isIgnoreSearch = false;
	private int _objectId;
	private WorldRegion _worldRegion;
	private int _x;
	private int _y;
	private int _z;
	private int _heading;
	private Reflection _reflection = ReflectionManager.DEFAULT;
	private final StatsSet _params;
	private InstanceType _instanceType = null;
	private volatile Map<String, Object> _scripts;

	private int _state = CREATED;
	protected final static int CREATED = 0;
	protected final static int VISIBLE = 1;
	protected final static int DELETED = -1;
	
	public GameObject(int objectId)
	{
		setInstanceType(InstanceType.GameObject);
		_objectId = objectId;
		_params = new StatsSet();
	}
	
	public static enum InstanceType
	{
		GameObject(null), ItemInstance(GameObject), Creature(GameObject), Npc(Creature), Playable(Creature), Summon(Playable), Decoy(Creature), Player(Playable), NpcInstance(Npc), MerchantInstance(NpcInstance), WarehouseInstance(NpcInstance), StaticObjectInstance(Creature), DoorInstance(Creature), TerrainObjectInstance(Npc), EffectPointInstance(Npc), ServitorInstance(Summon), SiegeSummonInstance(ServitorInstance), MerchantSummonInstance(ServitorInstance), PetInstance(Summon), BabyPetInstance(PetInstance), DecoyInstance(Decoy), TrapInstance(Npc), Attackable(Npc), GuardInstance(Attackable), QuestGuardInstance(GuardInstance), MonsterInstance(Attackable), ChestInstance(MonsterInstance), ControllableMobInstance(MonsterInstance), FeedableBeastInstance(MonsterInstance), TamedBeastInstance(FeedableBeastInstance), FriendlyMobInstance(Attackable), RiftInvaderInstance(MonsterInstance), RaidBossInstance(MonsterInstance), GrandBossInstance(RaidBossInstance), FlyNpcInstance(NpcInstance), FlyMonsterInstance(MonsterInstance), FlyRaidBossInstance(RaidBossInstance), FlyTerrainObjectInstance(MonsterInstance), SepulcherNpcInstance(NpcInstance), SepulcherMonsterInstance(MonsterInstance), FestivalGiudeInstance(Npc), FestivalMonsterInstance(MonsterInstance), Vehicle(Creature), BoatInstance(Vehicle), AirShipInstance(Vehicle), ControllableAirShipInstance(AirShipInstance), DefenderInstance(Attackable), ArtefactInstance(NpcInstance), ControlTowerInstance(Npc), FlameTowerInstance(Npc), SiegeFlagInstance(Npc), SiegeNpcInstance(Npc), FortBallistaInstance(Npc), FortCommanderInstance(DefenderInstance), CastleChamberlainInstance(MerchantInstance), CastleMagicianInstance(NpcInstance), FortEnvoyInstance(Npc), FortLogisticsInstance(MerchantInstance), FortManagerInstance(MerchantInstance), FortSiegeNpcInstance(Npc), FortSupportCaptainInstance(MerchantInstance), SignsPriestInstance(Npc), DawnPriestInstance(SignsPriestInstance), DuskPriestInstance(SignsPriestInstance), DungeonGatekeeperInstance(Npc), AdventurerInstance(NpcInstance), AuctioneerInstance(Npc), ClanHallManagerInstance(MerchantInstance), FishermanInstance(MerchantInstance), ManorManagerInstance(MerchantInstance), ObservationInstance(Npc), OlympiadManagerInstance(Npc), PetManagerInstance(MerchantInstance), RaceManagerInstance(Npc), SymbolMakerInstance(Npc), TeleporterInstance(Npc), TrainerInstance(NpcInstance), VillageMasterInstance(NpcInstance), DoormenInstance(NpcInstance), CastleDoormenInstance(DoormenInstance), FortDoormenInstance(DoormenInstance), ClanHallDoormenInstance(DoormenInstance), ClassMasterInstance(NpcInstance), NpcBufferInstance(Npc), TvTEventNpcInstance(Npc), TvTRoundEventNpcInstance(Npc), EventMobInstance(Npc), UCManagerInstance(Npc), ChronoMonsterInstance(MonsterInstance), HotSpringSquashInstance(MonsterInstance), CommunityBufferInstance(Npc), CommunityBankInstance(Npc), CommunityTeleporterInstance(Npc), CommunityDonationInstance(Npc), CommunityShopInstance(Npc), CommunityDyesInstance(Npc), CommunitySpecialInstance(Npc), CommunitySubInstance(Npc), CommunityCraftInstance(Npc), DominationInstance(Npc);
		
		private final InstanceType _parent;
		private final long _typeL;
		private final long _typeH;
		private final long _maskL;
		private final long _maskH;
		
		private InstanceType(InstanceType parent)
		{
			_parent = parent;
			
			final int high = ordinal() - (Long.SIZE - 1);
			if (high < 0)
			{
				_typeL = 1L << ordinal();
				_typeH = 0;
			}
			else
			{
				_typeL = 0;
				_typeH = 1L << high;
			}
			
			if ((_typeL < 0) || (_typeH < 0))
			{
				throw new Error("Too many instance types, failed to load " + name());
			}
			
			if (parent != null)
			{
				_maskL = _typeL | parent._maskL;
				_maskH = _typeH | parent._maskH;
			}
			else
			{
				_maskL = _typeL;
				_maskH = _typeH;
			}
		}
		
		public final InstanceType getParent()
		{
			return _parent;
		}
		
		public final boolean isType(InstanceType it)
		{
			return ((_maskL & it._typeL) > 0) || ((_maskH & it._typeH) > 0);
		}
		
		public final boolean isTypes(InstanceType... it)
		{
			for (final InstanceType i : it)
			{
				if (isType(i))
				{
					return true;
				}
			}
			return false;
		}
	}
	
	protected final void setInstanceType(InstanceType instanceType)
	{
		_instanceType = instanceType;
	}
	
	public final InstanceType getInstanceType()
	{
		return _instanceType;
	}
	
	public final boolean isInstanceType(InstanceType instanceType)
	{
		return _instanceType.isType(instanceType);
	}
	
	public final boolean isInstanceTypes(InstanceType... instanceType)
	{
		return _instanceType.isTypes(instanceType);
	}
	
	public void onAction(Player player, boolean shift)
	{
		onAction(player, true, shift);
	}
	
	public void onAction(Player player, boolean interact, boolean shift)
	{
		final var handler = ActionHandler.getInstance().getHandler(getInstanceType());
		if (handler != null)
		{
			handler.action(player, this, interact, shift);
		}
		player.sendActionFailed();
	}
	
	public void onActionShift(Player player)
	{
		final var handler = ActionShiftHandler.getInstance().getHandler(getInstanceType());
		if (handler != null)
		{
			handler.action(player, this, true, true);
		}
		player.sendActionFailed();
	}
	
	public void onForcedAttack(Player player, boolean shift)
	{
		player.sendActionFailed();
	}
	
	public void onSpawn()
	{
	}
	
	@Override
	public boolean setXYZ(int x, int y, int z)
	{
		if (_x == x && _y == y && _z == z)
		{
			return false;
		}
		
		setX(x);
		setY(y);
		setZ(z);
		
		if (!isVisible())
		{
			return false;
		}
		
		try
		{
			final var region = World.getInstance().getRegion(getLocation());
			if (region != _worldRegion)
			{
				setWorldRegion(region);
			}
		}
		catch (final Exception e)
		{
			if (isCreature())
			{
				decayMe();
			}
			else if (isPlayer())
			{
				_log.warn("Error with " + getName(null) + " coords: " + getX() + " " + getY() + " " + getZ());
				final var loc = MapRegionManager.getInstance().getTeleToLocation((Creature) this, TeleportWhereType.TOWN);
				if (loc != null)
				{
					((Creature) this).teleToLocation(loc, true, getReflection());
				}
				else
				{
					((Creature) this).teleToLocation(new Location(-83646, 243397, -3700), false, getReflection());
				}
			}
		}
		return true;
	}
	
	public void setXYZInvisible(int x, int y, int z)
	{
		_x = Util.limit(x, World.MAP_MIN_X, World.MAP_MAX_X);
		_y = Util.limit(y, World.MAP_MIN_Y, World.MAP_MAX_Y);
		_z = z;
		setIsVisible(false);
	}
	
	@Override
	public final int getX()
	{
		return _x;
	}
	
	@Override
	public final int getY()
	{
		return _y;
	}
	
	@Override
	public final int getZ()
	{
		return _z;
	}
	
	@Override
	public boolean setXYZ(ILocational loc)
	{
		return setXYZ(loc.getX(), loc.getY(), loc.getZ());
	}
	
	@Override
	public Location getLocation()
	{
		return new Location(getX(), getY(), getZ(), getHeading());
	}

	@Override
	public boolean setLocation(Location loc)
	{
		return setXYZ(loc);
	}
	
	@Override
	public void setHeading(int newHeading)
	{
		_heading = newHeading;
	}
	
	@Override
	public int getHeading()
	{
		return _heading;
	}
	
	public int getReflectionId()
	{
		return _reflection.getId();
	}
	
	@Override
	public void setX(int newX)
	{
		_x = newX;
	}

	@Override
	public void setY(int newY)
	{
		_y = newY;
	}
	
	@Override
	public void setZ(int newZ)
	{
		_z = newZ;
	}

	public synchronized void setReflection(Reflection ref)
	{
		if (_reflection == ref || ref == null)
		{
			return;
		}

		if (isPlayer())
		{
			final var player = getActingPlayer();
			if (_reflection != null && !_reflection.isDefault())
			{
				_reflection.removePlayer(getObjectId());
				if (_reflection.isShowTimer())
				{
					final int startTime = (int) ((System.currentTimeMillis() - _reflection.getInstanceStartTime()) / 1000);
					final int endTime = (int) ((_reflection.getInstanceEndTime() - _reflection.getInstanceStartTime()) / 1000);
					if (_reflection.isTimerIncrease())
					{
						sendPacket(new ExSendUIEvent(getActingPlayer(), true, true, startTime, endTime, _reflection.getTimerText()));
					}
					else
					{
						sendPacket(new ExSendUIEvent(getActingPlayer(), true, false, endTime - startTime, 0, _reflection.getTimerText()));
					}
				}
			}
			
			if (!ref.isDefault())
			{
				ref.addPlayer(getObjectId());
				if (ref.isShowTimer())
				{
					final int startTime = (int) ((System.currentTimeMillis() - ref.getInstanceStartTime()) / 1000);
					final int endTime = (int) ((ref.getInstanceEndTime() - ref.getInstanceStartTime()) / 1000);
					if (ref.isTimerIncrease())
					{
						sendPacket(new ExSendUIEvent(getActingPlayer(), false, true, startTime, endTime, ref.getTimerText()));
					}
					else
					{
						sendPacket(new ExSendUIEvent(getActingPlayer(), false, false, endTime - startTime, 0, ref.getTimerText()));
					}
				}
			}
			else
			{
				player.getEffectList().stopAllReflectionBuffs();
			}
			
			final var summon = player.getSummon();
			if (summon != null)
			{
				summon.setReflection(ref);
				if (ref.isDefault())
				{
					summon.getEffectList().stopAllReflectionBuffs();
				}
			}
		}
		else if (isNpc())
		{
			final var npc = (Npc) this;
			if (_reflection != null && !_reflection.isDefault())
			{
				_reflection.removeNpc(npc);
			}
			
			if (!ref.isDefault())
			{
				ref.addNpc(npc);
			}
		}
		
		_reflection = ref;
		
		if (isVisible())
		{
			if (!isPlayer())
			{
				decayMe();
				spawnMe();
			}
		}
	}
	
	public void decayMe()
	{
		if (_state == VISIBLE)
		{
			_state = CREATED;
			setWorldRegion(null);
			if (isCreature())
			{
				((Creature) this).clearZones();
			}
			onDespawn();
		}
	}
	
	public void decayItem()
	{
		_state = CREATED;
		setWorldRegion(null);
		onDespawn();
	}
	
	protected void onDespawn()
	{
	}
	
	public void deleteMe()
	{
		decayMe();
		
		if (_state == CREATED)
		{
			_state = DELETED;
			onDelete();
		}
	}
	
	protected void onDelete()
	{
	}
	
	public void refreshID()
	{
		GameObjectsStorage.remove(this);
		IdFactory.getInstance().releaseId(getObjectId());
		_objectId = IdFactory.getInstance().getNextId();
		refreshState();
		GameObjectsStorage.put(this);
	}
	
	public void refreshState()
	{
		if (_state == DELETED)
		{
			_state = CREATED;
		}
	}
	
	public final void spawnMe()
	{
		if (_state == CREATED)
		{
			_state = VISIBLE;
			setWorldRegion(World.getInstance().getRegion(getLocation()));
			onSpawn();
		}
	}
	
	public final void spawnMe(int x, int y, int z)
	{
		setXYZ(Util.limit(x, World.MAP_MIN_X, World.MAP_MAX_X), Util.limit(y, World.MAP_MIN_Y, World.MAP_MAX_Y), z);
		spawnMe();
	}
	
	public final void spawnMe(Location loc)
	{
		spawnMe(loc.getX(), loc.getY(), loc.getZ());
	}
	
	public boolean isAttackable()
	{
		return false;
	}
	
	public abstract boolean isAutoAttackable(Creature attacker, boolean isPoleAttack);
	
	public final boolean isVisible()
	{
		return _worldRegion != null && _state == VISIBLE;
	}
	
	public final void setIsVisible(boolean value)
	{
		if (!value)
		{
			setWorldRegion(null);
		}
	}
	
	public final String getName(String lang)
	{
		try
		{
			return _params.getString(lang != null ? "name" + lang.substring(0, 1).toUpperCase() + lang.substring(1) : "name" + Config.MULTILANG_DEFAULT.substring(0, 1).toUpperCase() + Config.MULTILANG_DEFAULT.substring(1));
		}
		catch (final IllegalArgumentException e)
		{
			return "";
		}
	}

	public void setName(String lang, String value)
	{
		_params.set("name" + lang.substring(0, 1).toUpperCase() + lang.substring(1), value);
	}
	
	public String getTitle(String lang)
	{
		try
		{
			final String title = _params.getString(lang != null ? "title" + lang.substring(0, 1).toUpperCase() + lang.substring(1) : "title" + Config.MULTILANG_DEFAULT.substring(0, 1).toUpperCase() + Config.MULTILANG_DEFAULT.substring(1));
			if (PvpColorManager.getInstance().isActive() && (title == null || title.isEmpty()) && isPlayer())
			{
				return PvpColorManager.getInstance().getPlayerTitle(getActingPlayer());
			}
			return title;
		}
		catch (final IllegalArgumentException e)
		{
			return "";
		}
	}
	
	public void setTitle(String lang, String title)
	{
		_params.set("title" + lang.substring(0, 1).toUpperCase() + lang.substring(1), title);
	}
	
	public void setGlobalName(String value)
	{
		for (final String lang : Config.MULTILANG_ALLOWED)
		{
			_params.set("name" + lang.substring(0, 1).toUpperCase() + lang.substring(1), value);
		}
	}
	
	public void setGlobalTitle(String value)
	{
		if (value != null)
		{
			value = value.length() > 21 ? value.substring(0, 20) : value;
		}
		
		for (final String lang : Config.MULTILANG_ALLOWED)
		{
			_params.set("title" + lang.substring(0, 1).toUpperCase() + lang.substring(1), value == null ? "" : value);
		}
	}

	public final int getObjectId()
	{
		return _objectId;
	}
	
	public WorldRegion getWorldRegion()
	{
		return _worldRegion;
	}
	
	public void setWorldRegion(WorldRegion newRegion)
	{
		try
		{
			List<WorldRegion> oldAreas = Collections.emptyList();
			
			if (_worldRegion != null)
			{
				_worldRegion.removeVisibleObject(this);
				oldAreas = _worldRegion.getNeighbors();
			}
			
			List<WorldRegion> newAreas = Collections.emptyList();
			
			if (newRegion != null)
			{
				newRegion.addVisibleObject(this);
				newAreas = newRegion.getNeighbors();
			}
			
			final int oid = getObjectId();
			final int rid = getReflectionId();
			
			for (final var region : oldAreas)
			{
				if (!newAreas.contains(region))
				{
					for (final var obj : region.getVisibleObjects())
					{
						if (obj != null && obj.getObjectId() == oid)
						{
							continue;
						}
						obj.removeInfoObject(this);
						removeInfoObject(obj);
					}
					
					if (isPlayer() && region.isEmptyNeighborhood())
					{
						region.switchActive(false);
					}
				}
			}
			
			for (final var region : newAreas)
			{
				if (!oldAreas.contains(region))
				{
					for (final var obj : region.getVisibleObjects())
					{
						if ((obj == null) || obj.getObjectId() == oid || (obj.getReflectionId() != rid))
						{
							continue;
						}
						
						if (obj.isPlayer() && isPlayer())
						{
							if (obj.getActingPlayer().getAppearance().isGhost() || (obj.getActingPlayer().isInStoreNow() && getActingPlayer().getNotShowTraders()))
							{
								obj.addInfoObject(this);
								continue;
							}
						}
						
						obj.addInfoObject(this);
						addInfoObject(obj);
					}
					
					if (isPlayer())
					{
						region.switchActive(true);
					}
				}
			}
			_worldRegion = newRegion;
		}
		catch (final Exception e)
		{}
	}
	
	public Player getActingPlayer()
	{
		return null;
	}
	
	public Npc getActingNpc()
	{
		return null;
	}
	
	public final static Player getActingPlayer(GameObject obj)
	{
		return (obj == null ? null : obj.getActingPlayer());
	}
	
	public abstract void sendInfo(Player activeChar);
	
	@Override
	public String toString()
	{
		return (getClass().getSimpleName() + ":" + getName(null) + "[" + getObjectId() + "]");
	}
	
	public void doDie(Creature killer)
	{
	}
	
	public void sendPacket(GameServerPacket mov)
	{
	}
	
	public void sendPacket(GameServerPacket... mov)
	{
	}
	
	public void sendPacket(List<? extends GameServerPacket> mov)
	{
	}

	public void sendPacket(SystemMessageId id)
	{
	}

	public void sendPacket(GameServerPacket packet, SystemMessageId id)
	{
	}
	
	public Creature getActingCharacter()
	{
		return null;
	}
	
	public boolean isPlayer()
	{
		return false;
	}
	
	public boolean isPlayable()
	{
		return false;
	}
	
	public boolean isSummon()
	{
		return false;
	}
	
	public boolean isPet()
	{
		return false;
	}
	
	public boolean isServitor()
	{
		return false;
	}
	
	public boolean isCreature()
	{
		return false;
	}
	
	public boolean isDoor()
	{
		return false;
	}
	
	public boolean isNpc()
	{
		return false;
	}
	
	public boolean isMonster()
	{
		return false;
	}
	
	public boolean isMinion()
	{
		return false;
	}
	
	public boolean isTrap()
	{
		return false;
	}
	
	public boolean isItem()
	{
		return false;
	}
	
	public boolean isVehicle()
	{
		return false;
	}
	
	public boolean isWalker()
	{
		return false;
	}
	
	public boolean isRunner()
	{
		return false;
	}
	
	public boolean isSpecialCamera()
	{
		return false;
	}
	
	public boolean isEkimusFood()
	{
		return false;
	}
	
	public boolean isTargetable()
	{
		return true;
	}
	
	public boolean isInsideZone(ZoneId zone)
	{
		return false;
	}
	
	public boolean isChargedShot(ShotType type)
	{
		return false;
	}
	
	public void setChargedShot(ShotType type, boolean charged)
	{
	}
	
	public void rechargeShots(boolean physical, boolean magical)
	{
	}
	
	public final <T> T addScript(T script)
	{
		if (_scripts == null)
		{
			synchronized (this)
			{
				if (_scripts == null)
				{
					_scripts = new ConcurrentHashMap<>();
				}
			}
		}
		_scripts.put(script.getClass().getName(), script);
		return script;
	}
	
	@SuppressWarnings("unchecked")
	public final <T> T removeScript(Class<T> script)
	{
		if (_scripts == null)
		{
			return null;
		}
		return (T) _scripts.remove(script.getName());
	}
	
	@SuppressWarnings("unchecked")
	public final <T> T getScript(Class<T> script)
	{
		if (_scripts == null)
		{
			return null;
		}
		return (T) _scripts.get(script.getName());
	}
	
	public void removeStatusListener(Creature object)
	{
	}
	
	public boolean isInvisible()
	{
		return _isInvisible;
	}
	
	public void setInvisible(boolean invis)
	{
		_isInvisible = invis;
		if (invis)
		{
			final var deletePacket = new DeleteObject(this);
			for (final Player player : World.getInstance().getAroundPlayers(this))
			{
				if (!isVisibleFor(player))
				{
					if (player.getTarget() == this)
					{
						player.setTarget(null);
						player.abortAttack();
						player.abortCast();
						player.getAI().setIntention(CtrlIntention.IDLE);
					}
					
					if (player.hasSummon())
					{
						player.getSummon().setTarget(null);
						player.getSummon().abortAttack();
						player.getSummon().abortCast();
					}
					player.sendPacket(deletePacket);
				}
			}
			
			for (final var npc : World.getInstance().getAroundNpc(this))
			{
				if (npc != null && npc instanceof Attackable && !npc.isDead())
				{
					npc.removeInfoObject(this);
				}
			}
		}
		broadcastInfo();
	}
	
	public boolean isVisibleFor(Creature creature)
	{
		return !isInvisible() || creature.canOverrideCond(PcCondOverride.SEE_ALL_PLAYERS);
	}
	
	public void broadcastInfo()
	{
		for (final var player : World.getInstance().getAroundPlayers(this))
		{
			if (isVisibleFor(player))
			{
				sendInfo(player);
			}
		}
	}
	
	public final long getXYDeltaSq(int x, int y)
	{
		final long dx = x - getX();
		final long dy = y - getY();
		return (dx * dx) + (dy * dy);
	}
	
	public final long getXYZDeltaSq(int x, int y, int z)
	{
		return getXYDeltaSq(x, y) + getZDeltaSq(z);
	}
	
	public final long getZDeltaSq(int z)
	{
		final long dz = z - getZ();
		return dz * dz;
	}
	
	public final double getDistance(GameObject obj)
	{
		if (obj == null)
		{
			return 0;
		}
		return Math.sqrt(getXYDeltaSq(obj.getX(), obj.getY()));
	}
	
	public final double getDistance3D(GameObject obj)
	{
		if (obj == null)
		{
			return 0;
		}
		return Math.sqrt(getXYZDeltaSq(obj.getX(), obj.getY(), obj.getZ()));
	}
	
	public final double getRealDistance3D(GameObject obj)
	{
		return getRealDistance3D(obj, false);
	}
	
	public final double getRealDistance3D(GameObject obj, boolean ignoreZ)
	{
		double distance = ignoreZ ? getDistance(obj) : getDistance3D(obj);
		distance -= getColRadius();
		return distance > 0 ? distance : 0;
	}
	
	public final boolean isInRange(Location loc, long range)
	{
		return isInRangeSq(loc, range * range);
	}

	public final boolean isInRangeSq(Location loc, long range)
	{
		return getXYDeltaSq(loc) <= range;
	}
	
	public final long getXYDeltaSq(Location loc)
	{
		return getXYDeltaSq(loc.getX(), loc.getY());
	}

	public final boolean isInRangeZ(GameObject obj, long range)
	{
		if (obj == null)
		{
			return false;
		}
		if (obj.getReflectionId() != getReflectionId())
		{
			return false;
		}
		final long dx = Math.abs(obj.getX() - getX());
		if (dx > range)
		{
			return false;
		}
		final long dy = Math.abs(obj.getY() - getY());
		if (dy > range)
		{
			return false;
		}
		final long dz = Math.abs(obj.getZ() - getZ());
		return dz <= range && dx * dx + dy * dy + dz * dz <= range * range;
	}
	
	public final boolean isInRangeZ(Creature actor, GameObject obj, long range)
	{
		if (obj == null)
		{
			return false;
		}

		final long dx = Math.abs(obj.getX() - actor.getX());
		if (dx > range)
		{
			return false;
		}
		final long dy = Math.abs(obj.getY() - actor.getY());
		if (dy > range)
		{
			return false;
		}
		final long dz = Math.abs(obj.getZ() - actor.getZ());
		return dz <= range && dx * dx + dy * dy + dz * dz <= range * range;
	}
	
	public Reflection getReflection()
	{
		return _reflection;
	}

	public final double calculateDistance(ILocational loc, boolean includeZAxis, boolean squared)
	{
		return calculateDistance(loc.getX(), loc.getY(), loc.getZ(), includeZAxis, squared);
	}
	
	public double calculateDistance3D(int x, int y, int z)
	{
		return Math.sqrt(Math.pow(x - _x, 2) + Math.pow(y - _y, 2) + Math.pow(z - _z, 2));
	}
	
	public double calculateDistance3D(ILocational loc)
	{
		return calculateDistance3D(loc.getX(), loc.getY(), loc.getZ());
	}
	
	public final double calculateDistance(int x, int y, int z, boolean includeZAxis, boolean squared)
	{
		final double distance = Math.pow(x - getX(), 2) + Math.pow(y - getY(), 2) + (includeZAxis ? Math.pow(z - getZ(), 2) : 0);
		return (squared) ? distance : Math.sqrt(distance);
	}

	public final double calculateDirectionTo(GameObject target)
	{
		final int heading = (Util.calculateHeadingFrom(this, target) - getHeading()) & 0x0000FFFF;
		return Util.convertHeadingToDegree(heading);
	}
	
	public final double calculateDistance(GameObject obj, boolean includeZAxis, boolean squared)
	{
		return calculateDistance(obj.getX(), obj.getY(), obj.getZ(), includeZAxis, squared);
	}
	
	public final boolean isInRange(GameObject obj, long range)
	{
		if (obj == null)
		{
			return false;
		}
		if (obj.getReflectionId() != getReflectionId())
		{
			return false;
		}
		final long dx = Math.abs(obj.getX() - getX());
		if (dx > range)
		{
			return false;
		}
		final long dy = Math.abs(obj.getY() - getY());
		if (dy > range)
		{
			return false;
		}
		final long dz = Math.abs(obj.getZ() - getZ());
		return dz <= 1500 && dx * dx + dy * dy <= range * range;
	}

	public final boolean isInRangeZ(Location loc, long range)
	{
		return isInRangeZSq(loc, range * range);
	}
	
	public final boolean isInRangeZSq(Location loc, long range)
	{
		return getXYZDeltaSq(loc) <= range;
	}

	public final long getXYZDeltaSq(Location loc)
	{
		return getXYDeltaSq(loc.getX(), loc.getY()) + getZDeltaSq(loc.getZ());
	}
	
	public final double getDistance(Location loc)
	{
		return getDistance(loc.getX(), loc.getY(), loc.getZ());
	}
	
	public double getDistance(int x, int y, int z)
	{
		return Math.sqrt(getXYZDeltaSq(x, y, z));
	}
	
	public double getDistance(int x, int y)
	{
		return Math.sqrt(getXYDeltaSq(x, y));
	}
	
	public void addInfoObject(GameObject object)
	{
	}
	
	public void removeInfoObject(GameObject object)
	{
	}
	
	@Override
	public void addEvent(AbstractFightEvent event)
	{
		event.onAddEvent(this);
		super.addEvent(event);
	}
	
	@Override
	public void removeEvent(AbstractFightEvent event)
	{
		event.onRemoveEvent(this);
		super.removeEvent(event);
	}
	
	public boolean canBeAttacked()
	{
		return false;
	}
	
	public boolean isFlying()
	{
		return false;
	}
	
	public boolean isInWater(GameObject object)
	{
		return false;
	}

	public boolean isInWater()
	{
		return false;
	}
	
	public double getColRadius()
	{
		return 0;
	}
	
	public double getColHeight()
	{
		return 0;
	}
	
	public Location getSpawnedLoc()
	{
		return null;
	}
	
	public boolean isBehind(GameObject target)
	{
		double angleChar, angleTarget, angleDiff;
		final double maxAngleDiff = 60;
		
		if (target == null)
		{
			return false;
		}
		
		if (target instanceof Creature)
		{
			final var target1 = (Creature) target;
			angleChar = Util.calculateAngleFrom(this, target1);
			angleTarget = Util.convertHeadingToDegree(target1.getHeading());
			angleDiff = angleChar - angleTarget;
			if (angleDiff <= (-360 + maxAngleDiff))
			{
				angleDiff += 360;
			}
			if (angleDiff >= (360 - maxAngleDiff))
			{
				angleDiff -= 360;
			}
			if (Math.abs(angleDiff) <= maxAngleDiff)
			{
				return true;
			}
		}
		return false;
	}
	
	public int getGeoZ(int x, int y, int z)
	{
		return GeoEngine.getInstance().getHeight(x, y, z);
	}
	
	public final int getGeoZ(ILocational loc)
	{
		return getGeoZ(loc.getX(), loc.getY(), loc.getZ());
	}
	
	public Collection<DoorInstance> getDoorsAround(Reflection r, double distance)
	{
		if (distance <= 0)
		{
			return Collections.emptyList();
		}
		return !r.isDefault() ? r.getDoors() : World.getInstance().getAroundDoors(this, (int) distance);
	}
	
	public boolean isInFrontDoor(Collection<DoorInstance> allDoors, Location loc, Location end, Reflection r)
	{
		return isInFrontDoor(allDoors, loc.getX(), loc.getY(), loc.getZ(), end.getX(), end.getY(), end.getZ(), r);
	}
	
	public boolean isInFrontDoor(int tx, int ty, int tz, Reflection r)
	{
		final var doors = getDoorsAround(r, Math.max(Math.hypot(tx - getX(), ty - getY()), 200));
		return isInFrontDoor(doors, getX(), getY(), getZ(), tx, ty, tz, r);
	}
	
	public boolean isInFrontDoor(Location loc, Location end, Reflection r)
	{
		final var doors = getDoorsAround(r, Math.max(Math.hypot(end.getX() - loc.getX(), end.getY() - loc.getY()), 0));
		return isInFrontDoor(doors, loc.getX(), loc.getY(), loc.getZ(), end.getX(), end.getY(), end.getZ(), r);
	}
	
	public boolean isInFrontDoor(Collection<DoorInstance> allDoors, int x, int y, int z, int tx, int ty, int tz, Reflection r)
	{
		if (allDoors == null || allDoors.isEmpty())
		{
			return false;
		}
		
		for (final var doorInst : allDoors)
		{
			if (doorInst.isDead() || (doorInst.isOpen() && doorInst.getId() != 20250777 && doorInst.getId() != 20250778) || (doorInst.isClosed() && (doorInst.getId() == 20250777 || doorInst.getId() == 20250778)) || (doorInst.getX(0) == 0))
			{
				continue;
			}
			
			for (int i = 0; i < 4; i++)
			{
				final int j = (i + 1) < 4 ? i + 1 : 0;
				final int denominator = ((ty - y) * (doorInst.getX(i) - doorInst.getX(j))) - ((tx - x) * (doorInst.getY(i) - doorInst.getY(j)));
				if (denominator == 0)
				{
					continue;
				}
				
				final float multiplier1 = (float) (((doorInst.getX(j) - doorInst.getX(i)) * (y - doorInst.getY(i))) - ((doorInst.getY(j) - doorInst.getY(i)) * (x - doorInst.getX(i)))) / denominator;
				final float multiplier2 = (float) (((tx - x) * (y - doorInst.getY(i))) - ((ty - y) * (x - doorInst.getX(i)))) / denominator;
				if ((multiplier1 >= 0) && (multiplier1 <= 1) && (multiplier2 >= 0) && (multiplier2 <= 1))
				{
					final int intersectZ = Math.round(z + (multiplier1 * (tz - z)));
					if ((intersectZ > doorInst.getZMin()) && (intersectZ < doorInst.getZMax()))
					{
						return true;
					}
				}
			}
		}
		return false;
	}
	
	public boolean isIgnoreSearch()
	{
		return _isIgnoreSearch;
	}
	
	public void setIsIgnoreSearch(boolean val)
	{
		_isIgnoreSearch = val;
	}
}