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
package l2e.scripts.vehicles;

import java.util.concurrent.Future;

import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.instancemanager.AirShipManager;
import l2e.gameserver.instancemanager.ZoneManager;
import l2e.gameserver.model.Clan;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.AirShipInstance;
import l2e.gameserver.model.actor.instance.ControllableAirShipInstance;
import l2e.gameserver.model.actor.templates.VehicleTemplate;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.zone.ZoneType;
import l2e.gameserver.model.zone.type.ScriptZone;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.NpcSay;
import l2e.gameserver.network.serverpackets.SystemMessage;

public abstract class AirShipController extends Quest
{
	protected int _dockZone = 0;
	
	protected int _shipSpawnX = 0;
	protected int _shipSpawnY = 0;
	protected int _shipSpawnZ = 0;
	protected int _shipHeading = 0;
	
	protected Location _oustLoc = null;
	
	protected int _locationId = 0;
	protected VehicleTemplate[] _arrivalPath = null;
	protected VehicleTemplate[] _departPath = null;
	
	protected VehicleTemplate[][] _teleportsTable = null;
	protected int[] _fuelTable = null;
	
	protected int _movieId = 0;
	
	protected boolean _isBusy = false;
	
	protected ControllableAirShipInstance _dockedShip = null;
	
	private final Runnable _decayTask = new DecayTask();
	private final Runnable _departTask = new DepartTask();
	private Future<?> _departSchedule = null;
	
	private NpcSay _arrivalMessage = null;
	
	private static final int DEPART_INTERVAL = 300000;
	
	private static final int LICENSE = 13559;
	private static final int STARSTONE = 13277;
	private static final int SUMMON_COST = 5;
	
	private static final SystemMessage SM_ALREADY_EXISTS = SystemMessage.getSystemMessage(SystemMessageId.THE_AIRSHIP_IS_ALREADY_EXISTS);
	private static final SystemMessage SM_ALREADY_SUMMONED = SystemMessage.getSystemMessage(SystemMessageId.ANOTHER_AIRSHIP_ALREADY_SUMMONED);
	private static final SystemMessage SM_NEED_LICENSE = SystemMessage.getSystemMessage(SystemMessageId.THE_AIRSHIP_NEED_LICENSE_TO_SUMMON);
	private static final SystemMessage SM_NEED_CLANLVL5 = SystemMessage.getSystemMessage(SystemMessageId.THE_AIRSHIP_NEED_CLANLVL_5_TO_SUMMON);
	private static final SystemMessage SM_NO_PRIVS = SystemMessage.getSystemMessage(SystemMessageId.THE_AIRSHIP_NO_PRIVILEGES);
	private static final SystemMessage SM_ALREADY_USED = SystemMessage.getSystemMessage(SystemMessageId.THE_AIRSHIP_ALREADY_USED);
	private static final SystemMessage SM_LICENSE_ALREADY_ACQUIRED = SystemMessage.getSystemMessage(SystemMessageId.THE_AIRSHIP_SUMMON_LICENSE_ALREADY_ACQUIRED);
	private static final SystemMessage SM_LICENSE_ENTERED = SystemMessage.getSystemMessage(SystemMessageId.THE_AIRSHIP_SUMMON_LICENSE_ENTERED);
	private static final SystemMessage SM_NEED_MORE = SystemMessage.getSystemMessage(SystemMessageId.THE_AIRSHIP_NEED_MORE_S1);
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		if ("summon".equalsIgnoreCase(event))
		{
			if (_dockedShip != null)
			{
				if (_dockedShip.isOwner(player))
				{
					player.sendPacket(SM_ALREADY_EXISTS);
				}
				return null;
			}
			if (_isBusy)
			{
				player.sendPacket(SM_ALREADY_SUMMONED);
				return null;
			}
			if ((player.getClanPrivileges() & Clan.CP_CL_SUMMON_AIRSHIP) != Clan.CP_CL_SUMMON_AIRSHIP)
			{
				player.sendPacket(SM_NO_PRIVS);
				return null;
			}
			final int ownerId = player.getClanId();
			if (!AirShipManager.getInstance().hasAirShipLicense(ownerId))
			{
				player.sendPacket(SM_NEED_LICENSE);
				return null;
			}
			if (AirShipManager.getInstance().hasAirShip(ownerId))
			{
				player.sendPacket(SM_ALREADY_USED);
				return null;
			}
			if (!player.destroyItemByItemId("AirShipSummon", STARSTONE, SUMMON_COST, npc, true))
			{
				SM_NEED_MORE.addItemName(STARSTONE);
				player.sendPacket(SM_NEED_MORE);
				return null;
			}
			
			_isBusy = true;
			final AirShipInstance ship = AirShipManager.getInstance().getNewAirShip(_shipSpawnX, _shipSpawnY, _shipSpawnZ, _shipHeading, ownerId);
			if (ship != null)
			{
				if (_arrivalPath != null)
				{
					ship.executePath(_arrivalPath);
				}
				
				if (_arrivalMessage == null)
				{
					_arrivalMessage = new NpcSay(npc.getObjectId(), Say2.NPC_SHOUT, npc.getId(), NpcStringId.THE_AIRSHIP_HAS_BEEN_SUMMONED_IT_WILL_AUTOMATICALLY_DEPART_IN_5_MINUTES);
				}
				
				npc.broadcastPacketToOthers(2000, _arrivalMessage);
			}
			else
			{
				_isBusy = false;
			}
			return null;
		}
		else if ("board".equalsIgnoreCase(event))
		{
			if (player.isTransformed())
			{
				player.sendPacket(SystemMessageId.YOU_CANNOT_BOARD_AN_AIRSHIP_WHILE_TRANSFORMED);
				return null;
			}
			else if (player.isParalyzed())
			{
				player.sendPacket(SystemMessageId.YOU_CANNOT_BOARD_AN_AIRSHIP_WHILE_PETRIFIED);
				return null;
			}
			else if (player.isDead() || player.isFakeDeath())
			{
				player.sendPacket(SystemMessageId.YOU_CANNOT_BOARD_AN_AIRSHIP_WHILE_DEAD);
				return null;
			}
			else if (player.isFishing())
			{
				player.sendPacket(SystemMessageId.YOU_CANNOT_BOARD_AN_AIRSHIP_WHILE_FISHING);
				return null;
			}
			else if (player.isInCombat())
			{
				player.sendPacket(SystemMessageId.YOU_CANNOT_BOARD_AN_AIRSHIP_WHILE_IN_BATTLE);
				return null;
			}
			else if (player.isInDuel())
			{
				player.sendPacket(SystemMessageId.YOU_CANNOT_BOARD_AN_AIRSHIP_WHILE_IN_A_DUEL);
				return null;
			}
			else if (player.isSitting())
			{
				player.sendPacket(SystemMessageId.YOU_CANNOT_BOARD_AN_AIRSHIP_WHILE_SITTING);
				return null;
			}
			else if (player.isCastingNow())
			{
				player.sendPacket(SystemMessageId.YOU_CANNOT_BOARD_AN_AIRSHIP_WHILE_CASTING);
				return null;
			}
			else if (player.isCursedWeaponEquipped())
			{
				player.sendPacket(SystemMessageId.YOU_CANNOT_BOARD_AN_AIRSHIP_WHILE_A_CURSED_WEAPON_IS_EQUIPPED);
				return null;
			}
			else if (player.isCombatFlagEquipped())
			{
				player.sendPacket(SystemMessageId.YOU_CANNOT_BOARD_AN_AIRSHIP_WHILE_HOLDING_A_FLAG);
				return null;
			}
			else if (player.hasSummon() || player.isMounted())
			{
				player.sendPacket(SystemMessageId.YOU_CANNOT_BOARD_AN_AIRSHIP_WHILE_A_PET_OR_A_SERVITOR_IS_SUMMONED);
				return null;
			}
			else if (player.isFlyingMounted())
			{
				player.sendPacket(SystemMessageId.YOU_CANNOT_BOARD_NOT_MEET_REQUEIREMENTS);
				return null;
			}
			
			if (_dockedShip != null)
			{
				_dockedShip.addPassenger(player);
			}
			
			return null;
		}
		else if ("register".equalsIgnoreCase(event))
		{
			if ((player.getClan() == null) || (player.getClan().getLevel() < 5))
			{
				player.sendPacket(SM_NEED_CLANLVL5);
				return null;
			}
			if (!player.isClanLeader())
			{
				player.sendPacket(SM_NO_PRIVS);
				return null;
			}
			final int ownerId = player.getClanId();
			if (AirShipManager.getInstance().hasAirShipLicense(ownerId))
			{
				player.sendPacket(SM_LICENSE_ALREADY_ACQUIRED);
				return null;
			}
			if (!player.destroyItemByItemId("AirShipLicense", LICENSE, 1, npc, true))
			{
				SM_NEED_MORE.addItemName(STARSTONE);
				player.sendPacket(SM_NEED_MORE);
				return null;
			}
			
			AirShipManager.getInstance().registerLicense(ownerId);
			player.sendPacket(SM_LICENSE_ENTERED);
			return null;
		}
		else
		{
			return event;
		}
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return npc.getId() + ".htm";
	}
	
	@Override
	public String onEnterZone(Creature character, ZoneType zone)
	{
		if (character instanceof ControllableAirShipInstance)
		{
			if (_dockedShip == null)
			{
				_dockedShip = (ControllableAirShipInstance) character;
				_dockedShip.setInDock(_dockZone);
				_dockedShip.setOustLoc(_oustLoc);
				
				if (!_dockedShip.isEmpty())
				{
					if (_movieId != 0)
					{
						for (final Player passenger : _dockedShip.getPassengers())
						{
							if (passenger != null)
							{
								passenger.showQuestMovie(_movieId);
							}
						}
					}
					ThreadPoolManager.getInstance().schedule(_decayTask, 5000);
				}
				else
				{
					_departSchedule = ThreadPoolManager.getInstance().schedule(_departTask, DEPART_INTERVAL);
				}
			}
		}
		return null;
	}
	
	@Override
	public String onExitZone(Creature character, ZoneType zone)
	{
		if (character instanceof ControllableAirShipInstance)
		{
			if (character.equals(_dockedShip))
			{
				if (_departSchedule != null)
				{
					_departSchedule.cancel(false);
					_departSchedule = null;
				}
				_dockedShip.setInDock(0);
				_dockedShip = null;
				_isBusy = false;
			}
		}
		return null;
	}
	
	protected void validityCheck()
	{
		final ScriptZone zone = ZoneManager.getInstance().getZoneById(_dockZone, ScriptZone.class);
		if (zone == null)
		{
			_log.warn(getName() + ": Invalid zone " + _dockZone + ", controller disabled");
			_isBusy = true;
			return;
		}
		
		VehicleTemplate p;
		if (_arrivalPath != null)
		{
			if (_arrivalPath.length == 0)
			{
				_log.warn(getName() + ": Zero arrival path length.");
				_arrivalPath = null;
			}
			else
			{
				p = _arrivalPath[_arrivalPath.length - 1];
				if (!zone.isInsideZone(p.getLocation()))
				{
					_log.warn(getName() + ": Arrival path finish point (" + p.getX() + "," + p.getY() + "," + p.getZ() + ") not in zone " + _dockZone);
					_arrivalPath = null;
				}
			}
		}
		if (_arrivalPath == null)
		{
			if (!ZoneManager.getInstance().getZoneById(_dockZone, ScriptZone.class).isInsideZone(_shipSpawnX, _shipSpawnY, _shipSpawnZ))
			{
				_log.warn(getName() + ": Arrival path is null and spawn point not in zone " + _dockZone + ", controller disabled");
				_isBusy = true;
				return;
			}
		}
		
		if (_departPath != null)
		{
			if (_departPath.length == 0)
			{
				_log.warn(getName() + ": Zero depart path length.");
				_departPath = null;
			}
			else
			{
				p = _departPath[_departPath.length - 1];
				if (zone.isInsideZone(p.getLocation()))
				{
					_log.warn(getName() + ": Departure path finish point (" + p.getX() + "," + p.getY() + "," + p.getZ() + ") in zone " + _dockZone);
					_departPath = null;
				}
			}
		}
		
		if (_teleportsTable != null)
		{
			if (_fuelTable == null)
			{
				_log.warn(getName() + ": Fuel consumption not defined.");
			}
			else
			{
				if (_teleportsTable.length != _fuelTable.length)
				{
					_log.warn(getName() + ": Fuel consumption not match teleport list.");
				}
				else
				{
					AirShipManager.getInstance().registerAirShipTeleportList(_dockZone, _locationId, _teleportsTable, _fuelTable);
				}
			}
		}
	}
	
	protected final class DecayTask implements Runnable
	{
		@Override
		public void run()
		{
			if (_dockedShip != null)
			{
				_dockedShip.oustPlayers();
				_dockedShip.deleteMe();
				_dockedShip.setInDock(0);
			}
			
			if (_departSchedule != null)
			{
				_departSchedule.cancel(false);
				_departSchedule = null;
			}
			_dockedShip = null;
			_isBusy = false;
		}
	}
	
	protected final class DepartTask implements Runnable
	{
		@Override
		public void run()
		{
			if ((_dockedShip != null) && _dockedShip.isInDock() && !_dockedShip.isMoving())
			{
				if (_departPath != null)
				{
					_dockedShip.executePath(_departPath);
				}
				else
				{
					_dockedShip.deleteMe();
				}
			}
		}
	}
	
	public AirShipController(int questId, String name, String descr)
	{
		super(questId, name, descr);
	}
	
	public static void main(String[] args)
	{
	}
}
