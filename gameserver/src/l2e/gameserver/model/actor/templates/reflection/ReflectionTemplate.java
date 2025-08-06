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
package l2e.gameserver.model.actor.templates.reflection;

import java.util.List;
import java.util.Map;

import l2e.commons.util.Rnd;
import l2e.gameserver.Config;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.holders.ReflectionReenterTimeHolder;
import l2e.gameserver.model.spawn.SpawnTerritory;
import l2e.gameserver.model.spawn.Spawner;
import l2e.gameserver.model.stats.StatsSet;

public class ReflectionTemplate
{
	private final int _id;
	private final String _name;
	private final int _timelimit;
	private final boolean _dispelBuffs;
	private final int _respawnTime;
	private final int _minLevel;
	private final int _maxLevel;
	private final int _minParty;
	private final int _maxParty;
	private final int _minRebirth;
	private final int _hwidsLimit;
	private final int _ipsLimit;
	private final List<Location> _teleportCoords;
	private final Location _returnCoords;
	private final int _collapseIfEmpty;
	private final int _maxChannels;
	final List<ReflectionItemTemplate> _requestItems;
	final List<ReflectionItemTemplate> _rewardItems;
	
	private final boolean _allowSummon;
	private final boolean _isPvPInstance;
	private final boolean _showTimer;
	private final boolean _isTimerIncrease;
	private final boolean _isForPremium;
	private final boolean _isHwidCheck;
	private final String _timerText;

	private ReflectionEntryType _entryType = null;
	
	private final Map<Integer, StatsSet> _doors;
	private final Map<String, SpawnInfo2> _spawns;
	private final List<SpawnInfo> _spawnsInfo;
	
	private final boolean _reuseUponEntry;
	private final int _sharedReuseGroup;
	final List<ReflectionReenterTimeHolder> _resetData;
	
	private final String _requiredQuest;
	private final ReflectionQuestType _questType;
	
	private final StatsSet _params;
	
	public enum ReflectionQuestType
	{
		STARTED, COMPLETED;
	}
	
	public enum ReflectionRemoveType
	{
		NONE, LEADER, ALL;
	}
	
	public enum ReflectionEntryType
	{
		SOLO, SOLO_PARTY, PARTY, EVENT, PARTY_COMMAND_CHANNEL, COMMAND_CHANNEL;
	}
	
	public static class SpawnInfo2
	{
		private final List<Spawner> _template;
		private final boolean _spawned;
		
		public SpawnInfo2(List<Spawner> template, boolean spawned)
		{
			_template = template;
			_spawned = spawned;
		}
		
		public List<Spawner> getTemplates()
		{
			return _template;
		}
		
		public boolean isSpawned()
		{
			return _spawned;
		}
	}
	
	public static class SpawnInfo
	{
		private final int _spawnType;
		private final int _npcId;
		private final int _count;
		private final int _respawn;
		private final int _respawnRnd;
		private final List<Location> _coords;
		private final SpawnTerritory _territory;
		
		public SpawnInfo(int spawnType, int npcId, int count, int respawn, int respawnRnd, SpawnTerritory territory)
		{
			this(spawnType, npcId, count, respawn, respawnRnd, null, territory);
		}
		
		public SpawnInfo(int spawnType, int npcId, int count, int respawn, int respawnRnd, List<Location> coords)
		{
			this(spawnType, npcId, count, respawn, respawnRnd, coords, null);
		}
		
		public SpawnInfo(int spawnType, int npcId, int count, int respawn, int respawnRnd, List<Location> coords, SpawnTerritory territory)
		{
			_spawnType = spawnType;
			_npcId = npcId;
			_count = count;
			_respawn = respawn;
			_respawnRnd = respawnRnd;
			_coords = coords;
			_territory = territory;
		}
		
		public int getSpawnType()
		{
			return _spawnType;
		}
		
		public int getId()
		{
			return _npcId;
		}
		
		public int getCount()
		{
			return _count;
		}
		
		public int getRespawnDelay()
		{
			return _respawn;
		}
		
		public int getRespawnRnd()
		{
			return _respawnRnd;
		}
		
		public List<Location> getCoords()
		{
			return _coords;
		}
		
		public SpawnTerritory getLoc()
		{
			return _territory;
		}
	}

	public ReflectionTemplate(int id, String name, int timelimit, boolean dispelBuffs, int respawnTime, int minLevel, int maxLevel, int minParty, int maxParty, int minRebirth, int hwidsLimit, int ipsLimit, List<Location> tele, Location ret, int collapseIfEmpty, int maxChannels, List<ReflectionItemTemplate> requestItems, List<ReflectionItemTemplate> rewardItems, boolean allowSummon, boolean isPvPInstance, boolean showTimer, boolean isTimerIncrease, String timerText, Map<Integer, StatsSet> doors, Map<String, SpawnInfo2> spawns, List<SpawnInfo> spawnsInfo, boolean reuseUponEntry, int sharedReuseGroup, List<ReflectionReenterTimeHolder> resetData, String requiredQuest, ReflectionQuestType questType, boolean isForPremium, boolean isHwidCheck, StatsSet params)
	{
		_id = id;
		_name = name;
		_timelimit = timelimit;
		_dispelBuffs = dispelBuffs;
		_respawnTime = respawnTime;
		_minLevel = minLevel;
		_maxLevel = maxLevel;
		_minRebirth = minRebirth;
		_hwidsLimit = hwidsLimit;
		_ipsLimit = ipsLimit;
		_teleportCoords = tele;
		_returnCoords = ret;
		_minParty = minParty;
		_maxParty = maxParty;
		_collapseIfEmpty = collapseIfEmpty;
		_maxChannels = maxChannels;
		_requestItems = requestItems;
		_rewardItems = rewardItems;
		
		_allowSummon = allowSummon;
		_isPvPInstance = isPvPInstance;
		_showTimer = showTimer;
		_isTimerIncrease = isTimerIncrease;
		_timerText = timerText;
		
		_doors = doors;
		_spawnsInfo = spawnsInfo;
		_spawns = spawns;
		
		_reuseUponEntry = reuseUponEntry;
		_sharedReuseGroup = sharedReuseGroup;
		_resetData = resetData;
		
		_requiredQuest = requiredQuest;
		_questType = questType;
		_isForPremium = isForPremium;
		_isHwidCheck = isHwidCheck;
		
		_params = params;
		
		if (getMinParty() == 1 && getMaxParty() == 1)
		{
			_entryType = ReflectionEntryType.SOLO;
		}
		else if (getMinParty() == 1 && getMaxParty() <= Config.PARTY_LIMIT)
		{
			_entryType = ReflectionEntryType.SOLO_PARTY;
		}
		else if (getMinParty() > 1 && getMaxParty() <= Config.PARTY_LIMIT)
		{
			_entryType = ReflectionEntryType.PARTY;
		}
		else if (getMinParty() <= Config.PARTY_LIMIT && getMaxParty() > Config.PARTY_LIMIT)
		{
			_entryType = ReflectionEntryType.PARTY_COMMAND_CHANNEL;
		}
		else if (getMinParty() >= Config.PARTY_LIMIT && getMaxParty() > Config.PARTY_LIMIT)
		{
			_entryType = ReflectionEntryType.COMMAND_CHANNEL;
		}
		else if(getMaxParty() == 0)
		{
			_entryType = ReflectionEntryType.EVENT;
		}
		
		if (_entryType == null)
		{
			throw new IllegalArgumentException("Invalid type for reflection: " + _name);
		}
	}
	
	public int getId()
	{
		return _id;
	}

	public String getName()
	{
		return _name;
	}

	public boolean isDispelBuffs()
	{
		return _dispelBuffs;
	}

	public int getTimelimit()
	{
		return _timelimit;
	}
	
	public int getRespawnTime()
	{
		return _respawnTime;
	}

	public int getMinLevel()
	{
		return _minLevel;
	}

	public int getMaxLevel()
	{
		return _maxLevel;
	}

	public int getMinParty()
	{
		return _minParty;
	}

	public int getMaxParty()
	{
		return _maxParty;
	}

	public Location getTeleportCoord()
	{
		if(_teleportCoords == null || _teleportCoords.isEmpty())
		{
			return null;
		}
		if(_teleportCoords.size() == 1)
		{
			return _teleportCoords.get(0);
		}
		return _teleportCoords.get(Rnd.get(_teleportCoords.size()));
	}
	
	public void setNewTeleportCoords(Location loc)
	{
		_teleportCoords.clear();
		_teleportCoords.add(loc);
	}

	public Location getReturnCoords()
	{
		return _returnCoords;
	}

	public int getCollapseIfEmpty()
	{
		return _collapseIfEmpty;
	}

	public List<ReflectionItemTemplate> getRequestItems()
	{
		return _requestItems;
	}
	
	public List<ReflectionItemTemplate> getRewardItems()
	{
		return _rewardItems;
	}

	public int getMaxChannels()
	{
		return _maxChannels;
	}

	public ReflectionEntryType getEntryType()
	{
		return _entryType;
	}

	public List<Location> getTeleportCoords()
	{
		return _teleportCoords;
	}
	
	public boolean isSummonAllowed()
	{
		return _allowSummon;
	}
	
	public boolean isPvPInstance()
	{
		return _isPvPInstance;
	}
	
	public boolean isForPremium()
	{
		return _isForPremium;
	}
	
	public boolean isShowTimer()
	{
		return _showTimer;
	}
	
	public boolean isTimerIncrease()
	{
		return _isTimerIncrease;
	}
	
	public String getTimerText()
	{
		return _timerText;
	}
	
	public Map<Integer, StatsSet> getDoorList()
	{
		return _doors;
	}
	
	public List<SpawnInfo> getSpawnsInfo()
	{
		return _spawnsInfo;
	}
	
	public Map<String, SpawnInfo2> getSpawns()
	{
		return _spawns;
	}
	
	public boolean getReuseUponEntry()
	{
		return _reuseUponEntry;
	}
	
	public int getSharedReuseGroup()
	{
		return _sharedReuseGroup;
	}
	
	public List<ReflectionReenterTimeHolder> getReenterData()
	{
		return _resetData;
	}
	
	public String getRequiredQuest()
	{
		return _requiredQuest;
	}
	
	public ReflectionQuestType getQuestType()
	{
		return _questType;
	}
	
	public StatsSet getParams()
	{
		return _params;
	}
	
	public int getRebirth()
	{
		return _minRebirth;
	}
	
	public int getHwidsLimit()
	{
		return _hwidsLimit;
	}
	
	public int getIpsLimit()
	{
		return _ipsLimit;
	}
	
	public boolean isHwidCheck()
	{
		return _isHwidCheck;
	}
}