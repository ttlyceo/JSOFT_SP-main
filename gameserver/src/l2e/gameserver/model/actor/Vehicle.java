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

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;

import l2e.commons.util.Util;
import l2e.gameserver.GameTimeController;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.instancemanager.MapRegionManager;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.TeleportWhereType;
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.stat.VehicleStat;
import l2e.gameserver.model.actor.templates.VehicleTemplate;
import l2e.gameserver.model.actor.templates.character.CharTemplate;
import l2e.gameserver.model.actor.templates.items.Weapon;
import l2e.gameserver.model.entity.Reflection;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.GameServerPacket;
import l2e.gameserver.network.serverpackets.InventoryUpdate;

public abstract class Vehicle extends Creature
{
	protected int _dockId = 0;
	protected final List<Player> _passengers = new CopyOnWriteArrayList<>();
	protected Location _oustLoc = null;
	private Runnable _engine = null;
	
	protected VehicleTemplate[] _currentPath = null;
	protected int _runState = 0;
	private Future<?> _checkTask = null;
	private final Location _checkLoc = new Location(this);

	public Vehicle(int objectId, CharTemplate template)
	{
		super(objectId, template);
		setInstanceType(InstanceType.Vehicle);
		setIsFlying(true);
	}
	
	public abstract GameServerPacket infoPacket();
	
	public boolean isBoat()
	{
		return false;
	}
	
	public boolean isAirShip()
	{
		return false;
	}
	
	public boolean canBeControlled()
	{
		return _engine == null;
	}
	
	public void registerEngine(Runnable r)
	{
		_engine = r;
	}
	
	public void runEngine(int delay)
	{
		if (_engine != null)
		{
			ThreadPoolManager.getInstance().schedule(_engine, delay);
		}
	}
	
	public void executePath(VehicleTemplate[] path)
	{
		_runState = 0;
		_currentPath = path;
		
		if ((_currentPath != null) && (_currentPath.length > 0))
		{
			final VehicleTemplate point = _currentPath[0];
			if (point.getMoveSpeed() > 0)
			{
				getStat().setMoveSpeed(point.getMoveSpeed());
			}
			if (point.getRotationSpeed() > 0)
			{
				getStat().setRotationSpeed(point.getRotationSpeed());
			}
			getAI().setIntention(CtrlIntention.MOVING, new Location(point.getX(), point.getY(), point.getZ(), 0), 0);
			return;
		}
		getAI().setIntention(CtrlIntention.ACTIVE);
	}
	
	@Override
	public boolean moveToNextRoutePoint()
	{
		_move = null;
		
		if (_currentPath != null)
		{
			_runState++;
			if (_runState < _currentPath.length)
			{
				final VehicleTemplate point = _currentPath[_runState];
				if (!isMovementDisabled())
				{
					if (point.getMoveSpeed() == 0)
					{
						teleToLocation(point.getX(), point.getY(), point.getZ(), point.getRotationSpeed(), false, ReflectionManager.DEFAULT);
						if (_checkTask != null)
						{
							_checkTask.cancel(true);
							_checkTask = null;
						}
						_currentPath = null;
					}
					else
					{
						if (point.getMoveSpeed() > 0)
						{
							getStat().setMoveSpeed(point.getMoveSpeed());
						}
						if (point.getRotationSpeed() > 0)
						{
							getStat().setRotationSpeed(point.getRotationSpeed());
						}
						
						final MoveData m = new MoveData();
						m._isIgnoreGeo = false;
						m._pathFindIndex = -1;
						m._xDestination = point.getX();
						m._yDestination = point.getY();
						m._zDestination = point.getZ();
						m._heading = 0;
						
						final double dx = point.getX() - getX();
						final double dy = point.getY() - getY();
						final double distance = Math.sqrt((dx * dx) + (dy * dy));
						if (distance > 1)
						{
							setHeading(Util.calculateHeadingFrom(getX(), getY(), point.getX(), point.getY()));
						}
						
						m._moveStartTime = GameTimeController.getInstance().getGameTicks();
						_move = m;
						GameTimeController.getInstance().registerMovingObject(this);
						if (_checkTask == null)
						{
							_checkTask = ThreadPoolManager.getInstance().scheduleAtFixedRate(() ->
							{
								if (!isInDock() && (calculateDistance3D(_checkLoc) == 0))
								{
									if (_currentPath != null)
									{
										if (_runState < _currentPath.length)
										{
											_runState = Math.max(0, _runState - 1);
											moveToNextRoutePoint();
										}
										else
										{
											broadcastInfo();
										}
									}
								}
								else
								{
									_checkLoc.setXYZ(this);
								}
							}, 1000, 1000);
						}
						return true;
					}
				}
			}
			else
			{
				if (_checkTask != null)
				{
					_checkTask.cancel(true);
					_checkTask = null;
				}
				_currentPath = null;
			}
		}
		runEngine(10);
		return false;
	}
	
	@Override
	public VehicleStat getStat()
	{
		return (VehicleStat) super.getStat();
	}
	
	@Override
	public void initCharStat()
	{
		setStat(new VehicleStat(this));
	}
	
	public boolean isInDock()
	{
		return _dockId > 0;
	}
	
	public int getDockId()
	{
		return _dockId;
	}
	
	public void setInDock(int d)
	{
		_dockId = d;
	}
	
	public void setOustLoc(Location loc)
	{
		_oustLoc = loc;
	}
	
	public Location getOustLoc()
	{
		return _oustLoc != null ? _oustLoc : MapRegionManager.getInstance().getTeleToLocation(this, TeleportWhereType.TOWN);
	}
	
	public void oustPlayers()
	{
		if (!_passengers.isEmpty())
		{
			_passengers.forEach(p -> oustPlayer(p));
		}
		_passengers.clear();
	}
	
	public void oustPlayer(Player player)
	{
		player.setVehicle(null);
		player.setInVehiclePosition(null);
		removePassenger(player);
	}
	
	public boolean addPassenger(Player player)
	{
		if ((player == null) || _passengers.contains(player))
		{
			return false;
		}
		
		if ((player.getVehicle() != null) && (player.getVehicle() != this))
		{
			return false;
		}
		
		_passengers.add(player);
		return true;
	}
	
	public void removePassenger(Player player)
	{
		try
		{
			_passengers.remove(player);
		}
		catch (final Exception e)
		{}
	}
	
	public boolean isEmpty()
	{
		return _passengers.isEmpty();
	}
	
	public List<Player> getPassengers()
	{
		return _passengers;
	}
	
	public void broadcastToPassengers(GameServerPacket sm)
	{
		for (final Player player : _passengers)
		{
			if (player != null)
			{
				player.sendPacket(sm);
			}
		}
	}
	
	public void payForRide(int itemId, int count, int oustX, int oustY, int oustZ)
	{
		final Collection<Player> passengers = World.getInstance().getAroundPlayers(this, 1000, 200);
		if ((passengers != null) && !passengers.isEmpty())
		{
			ItemInstance ticket;
			InventoryUpdate iu;
			for (final Player player : passengers)
			{
				if (player == null)
				{
					continue;
				}
				if (player.isInBoat() && (player.getBoat() == this))
				{
					if (itemId > 0)
					{
						ticket = player.getInventory().getItemByItemId(itemId);
						if ((ticket == null) || (player.getInventory().destroyItem("Boat", ticket, count, player, this) == null))
						{
							player.sendPacket(SystemMessageId.NOT_CORRECT_BOAT_TICKET);
							player.teleToLocation(oustX, oustY, oustZ, true, ReflectionManager.DEFAULT);
							continue;
						}
						iu = new InventoryUpdate();
						iu.addModifiedItem(ticket);
						player.sendPacket(iu);
					}
					addPassenger(player);
				}
			}
		}
	}
	
	@Override
	public boolean updatePosition()
	{
		final boolean result = super.updatePosition();
		
		for (final Player player : _passengers)
		{
			if ((player != null) && (player.getVehicle() == this))
			{
				player.setXYZ(getX(), getY(), getZ());
				player.revalidateZone(false);
			}
		}
		
		return result;
	}
	
	@Override
	public void teleToLocation(int x, int y, int z, int heading, boolean allowRandomOffset, Reflection r)
	{
		if (isMoving())
		{
			stopMove(null);
		}
		
		setIsTeleporting(true);
		
		getAI().setIntention(CtrlIntention.ACTIVE);
		
		for (final Player player : _passengers)
		{
			if (player != null)
			{
				player.teleToLocation(x, y, z, true, r);
			}
		}
		
		decayMe();
		setXYZ(x, y, z);
		
		if (heading != 0)
		{
			setHeading(heading);
		}
		
		onTeleported();
		revalidateZone(true);
	}
	
	@Override
	public void stopMove(Location loc)
	{
		_move = null;
		if (loc != null)
		{
			setXYZ(loc.getX(), loc.getY(), loc.getZ());
			setHeading(loc.getHeading());
			revalidateZone(true);
		}
	}
	
	@Override
	public void deleteMe()
	{
		_engine = null;
		try
		{
			if (isMoving())
			{
				stopMove(null);
			}
		}
		catch (final Exception e)
		{
			_log.warn("Failed stopMove().", e);
		}
		
		try
		{
			oustPlayers();
		}
		catch (final Exception e)
		{
			_log.warn("Failed oustPlayers().", e);
		}
		super.deleteMe();
	}
	
	@Override
	public void updateAbnormalEffect()
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
	public int getLevel()
	{
		return 0;
	}
	
	@Override
	public boolean isAutoAttackable(Creature attacker, boolean isPoleAttack)
	{
		return false;
	}
	
	@Override
	public void detachAI()
	{
	}
	
	@Override
	public boolean isWalker()
	{
		return true;
	}
	
	@Override
	public boolean isVehicle()
	{
		return true;
	}
}