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
package l2e.gameserver.model.zone.type;

import java.util.ArrayList;
import java.util.List;

import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.instancemanager.ZoneManager;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.DoorInstance;
import l2e.gameserver.model.actor.instance.OlympiadManagerInstance;
import l2e.gameserver.model.olympiad.OlympiadGameTask;
import l2e.gameserver.model.zone.AbstractZoneSettings;
import l2e.gameserver.model.zone.ZoneId;
import l2e.gameserver.model.zone.ZoneRespawn;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.ExOlympiadMatchEnd;
import l2e.gameserver.network.serverpackets.ExOlympiadUserInfo;
import l2e.gameserver.network.serverpackets.GameServerPacket;

public class OlympiadStadiumZone extends ZoneRespawn
{
	private List<Location> _spectatorLocations;
	
	public OlympiadStadiumZone(int id)
	{
		super(id);
		AbstractZoneSettings settings = ZoneManager.getSettings(getName());
		if (settings == null)
		{
			settings = new Settings();
		}
		setSettings(settings);
	}
	
	public final class Settings extends AbstractZoneSettings
	{
		private OlympiadGameTask _task = null;
		
		public Settings()
		{
		}
		
		public OlympiadGameTask getOlympiadTask()
		{
			return _task;
		}
		
		protected void setTask(OlympiadGameTask task)
		{
			_task = task;
		}
		
		@Override
		public void clear()
		{
			_task = null;
		}
	}
	
	@Override
	public Settings getSettings()
	{
		return (Settings) super.getSettings();
	}
	
	public final void registerTask(OlympiadGameTask task)
	{
		getSettings().setTask(task);
	}
	
	public final void openDoors()
	{
		for (final DoorInstance door : ReflectionManager.getInstance().getReflection(getReflectionId()).getDoors())
		{
			if ((door != null) && door.isClosed())
			{
				door.openMe();
			}
		}
	}
	
	public final void closeDoors()
	{
		for (final DoorInstance door : ReflectionManager.getInstance().getReflection(getReflectionId()).getDoors())
		{
			if ((door != null) && door.isOpened())
			{
				door.closeMe();
			}
		}
	}
	
	public final void spawnBuffers()
	{
		for (final Npc buffer : ReflectionManager.getInstance().getReflection(getReflectionId()).getNpcs())
		{
			if ((buffer instanceof OlympiadManagerInstance) && !buffer.isVisible())
			{
				buffer.spawnMe();
			}
		}
	}
	
	public final void deleteBuffers()
	{
		for (final Npc buffer : ReflectionManager.getInstance().getReflection(getReflectionId()).getNpcs())
		{
			if ((buffer instanceof OlympiadManagerInstance) && buffer.isVisible())
			{
				buffer.decayMe();
			}
		}
	}
	
	public final void broadcastStatusUpdate(Player player)
	{
		final ExOlympiadUserInfo packet = new ExOlympiadUserInfo(player);
		for (final Player target : getPlayersInside())
		{
			if ((target != null) && (target.inObserverMode() || (target.getOlympiadSide() != player.getOlympiadSide())))
			{
				target.sendPacket(packet);
			}
		}
	}
	
	public final void broadcastPacketToObservers(GameServerPacket packet)
	{
		for (final Creature character : getCharactersInside())
		{
			if ((character != null) && character.isPlayer() && character.getActingPlayer().inObserverMode())
			{
				character.sendPacket(packet);
			}
		}
	}
	
	@Override
	protected final void onEnter(Creature character)
	{
		if (getSettings().getOlympiadTask() != null)
		{
			if (getSettings().getOlympiadTask().isBattleStarted())
			{
				if (character.isPlayer())
				{
					character.sendPacket(SystemMessageId.ENTERED_COMBAT_ZONE);
					getSettings().getOlympiadTask().getGame().sendOlympiadInfo(character);
				}
			}
		}
		
		if (character.isPlayable())
		{
			final Player player = character.getActingPlayer();
			if (player != null && player.hasPet())
			{
				player.getSummon().unSummon(player);
			}
		}
	}
	
	@Override
	protected final void onExit(Creature character)
	{
		character.abortAttack();
		character.abortCast();
		
		if (character.isPlayer())
		{
			character.sendPacket(SystemMessageId.LEFT_COMBAT_ZONE);
			character.sendPacket(ExOlympiadMatchEnd.STATIC_PACKET);
		}
	}
	
	public final void updateZoneStatusForCharactersInside(boolean forceStoped)
	{
		if (getSettings().getOlympiadTask() == null)
		{
			return;
		}
		
		final boolean battleStarted = getSettings().getOlympiadTask().isBattleStarted() && !forceStoped;
		if (battleStarted)
		{
			addZoneId(ZoneId.PVP);
		}
		else
		{
			getZoneId().clear();
		}
		
		for (final Creature character : getCharactersInside())
		{
			if (character == null)
			{
				continue;
			}
			
			if (battleStarted)
			{
				if (character.isPlayer())
				{
					character.sendPacket(SystemMessageId.ENTERED_COMBAT_ZONE);
					character.getActingPlayer().broadcastRelationChanged();
				}
			}
			else
			{
				character.abortAttack();
				character.abortCast();
				
				if (character.isPlayer())
				{
					if (character.hasSummon())
					{
						character.getSummon().cancelAction();
					}
					character.sendPacket(ExOlympiadMatchEnd.STATIC_PACKET);
				}
			}
		}
		
		if (!battleStarted)
		{
			getZoneId().clear();
		}
	}
	
	@Override
	public void onDieInside(Creature character)
	{
	}
	
	@Override
	public void onReviveInside(Creature character)
	{
	}
	
	@Override
	public void parseLoc(int x, int y, int z, String type)
	{
		if ((type != null) && type.equals("spectatorSpawn"))
		{
			if (_spectatorLocations == null)
			{
				_spectatorLocations = new ArrayList<>();
			}
			_spectatorLocations.add(new Location(x, y, z));
		}
		else
		{
			super.parseLoc(x, y, z, type);
		}
	}
	
	public List<Location> getSpectatorSpawns()
	{
		return _spectatorLocations;
	}
}