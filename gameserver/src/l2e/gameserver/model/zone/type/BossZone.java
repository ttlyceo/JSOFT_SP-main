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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import l2e.gameserver.GameServer;
import l2e.gameserver.instancemanager.EpicBossManager;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.instancemanager.ZoneManager;
import l2e.gameserver.model.PcCondOverride;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.Summon;
import l2e.gameserver.model.actor.instance.player.impl.TeleportTask;
import l2e.gameserver.model.zone.AbstractZoneSettings;
import l2e.gameserver.model.zone.ZoneType;

public class BossZone extends ZoneType
{
	private int _timeInvade;
	private boolean _canTeleport = false;
	private int[] _oustLoc =
	{
	        0, 0, 0
	};

	private final class Settings extends AbstractZoneSettings
	{
		private final Map<Integer, Long> _playerAllowedReEntryTimes = new ConcurrentHashMap<>();
		private final List<Integer> _playersAllowed = new CopyOnWriteArrayList<>();
		private final List<Creature> _raidList = new CopyOnWriteArrayList<>();
		
		public Settings()
		{
		}
		
		public Map<Integer, Long> getPlayerAllowedReEntryTimes()
		{
			return _playerAllowedReEntryTimes;
		}
		
		public List<Integer> getPlayersAllowed()
		{
			return _playersAllowed;
		}
		
		public List<Creature> getRaidList()
		{
			return _raidList;
		}
		
		@Override
		public void clear()
		{
			_playerAllowedReEntryTimes.clear();
			_playersAllowed.clear();
			_raidList.clear();
		}
	}
	
	public BossZone(int id)
	{
		super(id);
		_oustLoc = new int[3];
		AbstractZoneSettings settings = ZoneManager.getSettings(getName());
		if (settings == null)
		{
			settings = new Settings();
		}
		setSettings(settings);
		EpicBossManager.getInstance().addZone(this);
	}
	
	@Override
	public Settings getSettings()
	{
		return (Settings) super.getSettings();
	}
	
	@Override
	public void setParameter(String name, String value)
	{
		if (name.equals("InvadeTime"))
		{
			_timeInvade = Integer.parseInt(value);
		}
		else if (name.equals("canTeleport"))
		{
			_canTeleport = Boolean.parseBoolean(value);
		}
		else if (name.equals("oustX"))
		{
			_oustLoc[0] = Integer.parseInt(value);
		}
		else if (name.equals("oustY"))
		{
			_oustLoc[1] = Integer.parseInt(value);
		}
		else if (name.equals("oustZ"))
		{
			_oustLoc[2] = Integer.parseInt(value);
		}
		else
		{
			super.setParameter(name, value);
		}
	}
	
	@Override
	protected void onEnter(Creature character)
	{
		if (character.isPlayer())
		{
			final Player player = character.getActingPlayer();
			if (player.canOverrideCond(PcCondOverride.ZONE_CONDITIONS) || player.isInFightEvent() || player.checkInTournament())
			{
				return;
			}
			
			if (getSettings().getPlayersAllowed().contains(player.getObjectId()))
			{
				final Long expirationTime = getSettings().getPlayerAllowedReEntryTimes().get(player.getObjectId());
				
				if (expirationTime == null)
				{
					final long serverStartTime = GameServer.dateTimeServerStarted.getTimeInMillis();
					if ((serverStartTime > (System.currentTimeMillis() - _timeInvade)))
					{
						return;
					}
				}
				else
				{
					getSettings().getPlayerAllowedReEntryTimes().remove(player.getObjectId());
					if (expirationTime.longValue() > System.currentTimeMillis())
					{
						return;
					}
				}
				getSettings().getPlayersAllowed().remove(getSettings().getPlayersAllowed().indexOf(player.getObjectId()));
			}
			
			if ((_oustLoc[0] != 0) && (_oustLoc[1] != 0) && (_oustLoc[2] != 0))
			{
				player.teleToLocation(_oustLoc[0], _oustLoc[1], _oustLoc[2], true, ReflectionManager.DEFAULT);
			}
			else
			{
				player.getPersonalTasks().addTask(new TeleportTask(2000, null));
			}
		}
		else if (character.isSummon())
		{
			final Player player = character.getActingPlayer();
			if (player != null)
			{
				if (getSettings().getPlayersAllowed().contains(player.getObjectId()) || player.canOverrideCond(PcCondOverride.ZONE_CONDITIONS) || player.isInFightEvent() || player.checkInTournament())
				{
					return;
				}
				
				if ((_oustLoc[0] != 0) && (_oustLoc[1] != 0) && (_oustLoc[2] != 0))
				{
					player.teleToLocation(_oustLoc[0], _oustLoc[1], _oustLoc[2], true, ReflectionManager.DEFAULT);
				}
				else
				{
					player.getPersonalTasks().addTask(new TeleportTask(2000, null));
				}
			}
			((Summon) character).unSummon(player);
		}
	}
	
	@Override
	protected void onExit(Creature character)
	{
		if (character.isPlayer())
		{
			final Player player = character.getActingPlayer();
			if (player.canOverrideCond(PcCondOverride.ZONE_CONDITIONS) || player.isInFightEvent() || player.checkInTournament())
			{
				return;
			}
			
			if (!player.isOnline() && getSettings().getPlayersAllowed().contains(player.getObjectId()))
			{
				getSettings().getPlayerAllowedReEntryTimes().put(player.getObjectId(), System.currentTimeMillis() + _timeInvade);
			}
			else
			{
				if (getSettings().getPlayersAllowed().contains(player.getObjectId()))
				{
					getSettings().getPlayersAllowed().remove(getSettings().getPlayersAllowed().indexOf(player.getObjectId()));
				}
				getSettings().getPlayerAllowedReEntryTimes().remove(player.getObjectId());
			}
		}
		
		if (character.isPlayable())
		{
			if ((getCharactersInside() != null) && !getCharactersInside().isEmpty())
			{
				getSettings().getRaidList().clear();
				int count = 0;
				for (final Creature obj : getCharactersInside())
				{
					if (obj == null)
					{
						continue;
					}
					if (obj.isPlayable())
					{
						count++;
					}
					else if (obj.isAttackable() && obj.isRaid())
					{
						getSettings().getRaidList().add(obj);
					}
				}
				
				if ((count == 0) && !getSettings().getRaidList().isEmpty())
				{
					for (int i = 0; i < getSettings().getRaidList().size(); i++)
					{
						final Attackable raid = (Attackable) getSettings().getRaidList().get(i);
						if ((raid == null) || (raid.getSpawn() == null) || raid.isDead())
						{
							continue;
						}
						if (!raid.isInsideRadius(raid.getSpawn().getX(), raid.getSpawn().getY(), 150, false))
						{
							raid.returnHome();
						}
					}
				}
			}
		}
		
		if (character.isAttackable() && character.isRaid() && !character.isDead())
		{
			((Attackable) character).returnHome();
		}
	}
	
	public int getTimeInvade()
	{
		return _timeInvade;
	}
	
	public void setAllowedPlayers(List<Integer> players)
	{
		if (players != null)
		{
			getSettings().getPlayersAllowed().clear();
			getSettings().getPlayersAllowed().addAll(players);
		}
	}
	
	public List<Integer> getAllowedPlayers()
	{
		return getSettings().getPlayersAllowed();
	}
	
	public boolean isPlayerAllowed(Player player)
	{
		if (player.canOverrideCond(PcCondOverride.ZONE_CONDITIONS) || player.isInFightEvent() || player.checkInTournament())
		{
			return true;
		}
		else if (getSettings().getPlayersAllowed().contains(player.getObjectId()))
		{
			return true;
		}
		else
		{
			if ((_oustLoc[0] != 0) && (_oustLoc[1] != 0) && (_oustLoc[2] != 0))
			{
				player.teleToLocation(_oustLoc[0], _oustLoc[1], _oustLoc[2], true, ReflectionManager.DEFAULT);
			}
			else
			{
				player.getPersonalTasks().addTask(new TeleportTask(2000, null));
			}
			return false;
		}
	}
	
	public void movePlayersTo(int x, int y, int z)
	{
		if (_characterList.isEmpty())
		{
			return;
		}
		
		for (final Creature character : getCharactersInside())
		{
			if ((character != null) && character.isPlayer())
			{
				final Player player = character.getActingPlayer();
				if (player.isOnline() && !player.isInFightEvent() && !player.checkInTournament())
				{
					player.teleToLocation(x, y, z, true, ReflectionManager.DEFAULT);
				}
			}
		}
	}
	
	public void oustAllPlayers()
	{
		if (_characterList.isEmpty())
		{
			return;
		}
		
		for (final Creature character : getCharactersInside())
		{
			if ((character != null) && character.isPlayer())
			{
				final Player player = character.getActingPlayer();
				if (player.isOnline() && !player.isInFightEvent() && !player.checkInTournament())
				{
					if ((_oustLoc[0] != 0) && (_oustLoc[1] != 0) && (_oustLoc[2] != 0))
					{
						player.teleToLocation(_oustLoc[0], _oustLoc[1], _oustLoc[2], true, ReflectionManager.DEFAULT);
					}
					else
					{
						player.getPersonalTasks().addTask(new TeleportTask(2000, null));
					}
				}
			}
		}
		getSettings().getPlayerAllowedReEntryTimes().clear();
		getSettings().getPlayersAllowed().clear();
	}
	
	public void allowPlayerEntry(Player player, int durationInSec)
	{
		if (!player.canOverrideCond(PcCondOverride.ZONE_CONDITIONS))
		{
			if (!getSettings().getPlayersAllowed().contains(player.getObjectId()))
			{
				getSettings().getPlayersAllowed().add(player.getObjectId());
			}
			getSettings().getPlayerAllowedReEntryTimes().put(player.getObjectId(), System.currentTimeMillis() + (durationInSec * 1000));
		}
	}
	
	public void removePlayer(Player player)
	{
		if (!player.canOverrideCond(PcCondOverride.ZONE_CONDITIONS))
		{
			getSettings().getPlayersAllowed().remove(Integer.valueOf(player.getObjectId()));
			getSettings().getPlayerAllowedReEntryTimes().remove(player.getObjectId());
		}
	}
	
	public void setCanTeleport(boolean canTele)
	{
		_canTeleport = canTele;
	}
	
	public boolean isCanTeleport()
	{
		return _canTeleport;
	}
}