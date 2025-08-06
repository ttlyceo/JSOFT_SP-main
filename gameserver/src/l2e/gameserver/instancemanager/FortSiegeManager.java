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
package l2e.gameserver.instancemanager;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;

import l2e.commons.log.LoggerObject;
import l2e.gameserver.Config;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.model.Clan;
import l2e.gameserver.model.CombatFlag;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.entity.Fort;
import l2e.gameserver.model.entity.FortSiege;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.spawn.SpawnFortSiege;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.SystemMessage;

public final class FortSiegeManager extends LoggerObject
{
	private int _attackerMaxClans = 500;
	
	private Map<Integer, List<SpawnFortSiege>> _commanderSpawnList;
	private Map<Integer, List<CombatFlag>> _flagList;
	private Map<Integer, List<SpawnFortSiege>> _powerUnitList;
	private Map<Integer, List<SpawnFortSiege>> _controlUnitList;
	private Map<Integer, List<SpawnFortSiege>> _mainMachineList;
	
	private int _flagMaxCount = 1;
	private int _siegeClanMinLevel = 4;
	private int _siegeLength = 60;
	private int _countDownLength = 10;
	private int _suspiciousMerchantRespawnDelay = 180;
	private int _fortHwidLimit = 0;
	private final List<FortSiege> _sieges = new ArrayList<>();
	
	protected FortSiegeManager()
	{
		load();
	}

	public final void addSiegeSkills(Player character)
	{
		character.addSkill(SkillsParser.FrequentSkill.SEAL_OF_RULER.getSkill(), false);
		character.addSkill(SkillsParser.FrequentSkill.BUILD_HEADQUARTERS.getSkill(), false);
	}

	public final boolean checkIsRegistered(Clan clan, int fortid)
	{
		if (clan == null)
		{
			return false;
		}

		boolean register = false;
		try (
		    var con = DatabaseFactory.getInstance().getConnection())
		{
			final var statement = con.prepareStatement("SELECT clan_id FROM fortsiege_clans where clan_id=? and fort_id=?");
			statement.setInt(1, clan.getId());
			statement.setInt(2, fortid);
			final ResultSet rs = statement.executeQuery();

			while (rs.next())
			{
				register = true;
				break;
			}

			rs.close();
			statement.close();
		}
		catch (final Exception e)
		{
			warn("checkIsRegistered(): " + e.getMessage(), e);
		}
		return register;
	}

	public final void removeSiegeSkills(Player character)
	{
		character.removeSkill(SkillsParser.FrequentSkill.SEAL_OF_RULER.getSkill());
		character.removeSkill(SkillsParser.FrequentSkill.BUILD_HEADQUARTERS.getSkill());
	}
	
	private final void load()
	{
		final Properties siegeSettings = new Properties();
		final File file = new File(Config.FORTSIEGE_CONFIGURATION_FILE);
		try (
		    InputStream is = new FileInputStream(file))
		{
			siegeSettings.load(is);
		}
		catch (final Exception e)
		{
			warn("Error while loading Fort Siege Manager settings!", e);
		}

		_attackerMaxClans = Integer.decode(siegeSettings.getProperty("AttackerMaxClans", "500"));
		_flagMaxCount = Integer.decode(siegeSettings.getProperty("MaxFlags", "1"));
		_siegeClanMinLevel = Integer.decode(siegeSettings.getProperty("SiegeClanMinLevel", "4"));
		_siegeLength = Integer.decode(siegeSettings.getProperty("SiegeLength", "60"));
		_countDownLength = Integer.decode(siegeSettings.getProperty("CountDownLength", "10"));
		_suspiciousMerchantRespawnDelay = Integer.decode(siegeSettings.getProperty("SuspiciousMerchantRespawnDelay", "180"));
		_fortHwidLimit = Integer.decode(siegeSettings.getProperty("FortSiegeLimitPlayers", "0"));
		
		_commanderSpawnList = new ConcurrentHashMap<>();
		_flagList = new ConcurrentHashMap<>();
		_powerUnitList = new ConcurrentHashMap<>();
		_controlUnitList = new ConcurrentHashMap<>();
		_mainMachineList = new ConcurrentHashMap<>();

		for (final Fort fort : FortManager.getInstance().getForts())
		{
			final List<SpawnFortSiege> _commanderSpawns = new ArrayList<>();
			final List<CombatFlag> _flagSpawns = new ArrayList<>();
			final List<SpawnFortSiege> _powerUnitSpawns = new ArrayList<>();
			final List<SpawnFortSiege> _controlUnitSpawns = new ArrayList<>();
			final List<SpawnFortSiege> _mainMachineSpawns = new ArrayList<>();
			for (int i = 1; i < 5; i++)
			{
				final String _spawnParams = siegeSettings.getProperty(fort.getName().replace(" ", "") + "Commander" + i, "");
				if (_spawnParams.isEmpty())
				{
					break;
				}
				final StringTokenizer st = new StringTokenizer(_spawnParams.trim(), ",");

				try
				{
					final int x = Integer.parseInt(st.nextToken());
					final int y = Integer.parseInt(st.nextToken());
					final int z = Integer.parseInt(st.nextToken());
					final int heading = Integer.parseInt(st.nextToken());
					final int npc_id = Integer.parseInt(st.nextToken());

					_commanderSpawns.add(new SpawnFortSiege(fort.getId(), x, y, z, heading, npc_id, i));
				}
				catch (final Exception e)
				{
					warn("Error while loading commander(s) for " + fort.getName() + " fort.");
				}
			}

			_commanderSpawnList.put(fort.getId(), _commanderSpawns);

			for (int i = 1; i < 4; i++)
			{
				final String _spawnParams = siegeSettings.getProperty(fort.getName().replace(" ", "") + "Flag" + i, "");
				if (_spawnParams.isEmpty())
				{
					break;
				}
				final StringTokenizer st = new StringTokenizer(_spawnParams.trim(), ",");

				try
				{
					final int x = Integer.parseInt(st.nextToken());
					final int y = Integer.parseInt(st.nextToken());
					final int z = Integer.parseInt(st.nextToken());
					final int flag_id = Integer.parseInt(st.nextToken());

					_flagSpawns.add(new CombatFlag(fort.getId(), x, y, z, 0, flag_id));
				}
				catch (final Exception e)
				{
					warn("Error while loading flag(s) for " + fort.getName() + " fort.");
				}
			}
			_flagList.put(fort.getId(), _flagSpawns);
			
			for (int i = 1; i < 5; i++)
			{
				final String _spawnParams = siegeSettings.getProperty(fort.getName().replace(" ", "") + "PowerUnit" + i, "");
				if (_spawnParams.isEmpty())
				{
					break;
				}
				final StringTokenizer st = new StringTokenizer(_spawnParams.trim(), ",");
				
				try
				{
					final int x = Integer.parseInt(st.nextToken());
					final int y = Integer.parseInt(st.nextToken());
					final int z = Integer.parseInt(st.nextToken());
					final int heading = Integer.parseInt(st.nextToken());
					final int powerUnitId = Integer.parseInt(st.nextToken());
					_powerUnitSpawns.add(new SpawnFortSiege(fort.getId(), x, y, z, heading, powerUnitId, i));
				}
				catch (final Exception e)
				{
					warn("Error while loading power unit(s) for " + fort.getName() + " fort.");
				}
			}
			_powerUnitList.put(fort.getId(), _powerUnitSpawns);
			
			for (int i = 1; i < 5; i++)
			{
				final String _spawnParams = siegeSettings.getProperty(fort.getName().replace(" ", "") + "ControlUnit" + i, "");
				if (_spawnParams.isEmpty())
				{
					break;
				}
				final StringTokenizer st = new StringTokenizer(_spawnParams.trim(), ",");
				
				try
				{
					final int x = Integer.parseInt(st.nextToken());
					final int y = Integer.parseInt(st.nextToken());
					final int z = Integer.parseInt(st.nextToken());
					final int heading = Integer.parseInt(st.nextToken());
					final int controlUnitId = Integer.parseInt(st.nextToken());
					_controlUnitSpawns.add(new SpawnFortSiege(fort.getId(), x, y, z, heading, controlUnitId, i));
				}
				catch (final Exception e)
				{
					warn("Error while loading control unit(s) for " + fort.getName() + " fort.");
				}
			}
			_controlUnitList.put(fort.getId(), _controlUnitSpawns);
			
			for (int i = 1; i < 2; i++)
			{
				final String _spawnParams = siegeSettings.getProperty(fort.getName().replace(" ", "") + "MainMachine" + i, "");
				if (_spawnParams.isEmpty())
				{
					break;
				}
				final StringTokenizer st = new StringTokenizer(_spawnParams.trim(), ",");
				
				try
				{
					final int x = Integer.parseInt(st.nextToken());
					final int y = Integer.parseInt(st.nextToken());
					final int z = Integer.parseInt(st.nextToken());
					final int heading = Integer.parseInt(st.nextToken());
					final int mainMachineId = Integer.parseInt(st.nextToken());
					_mainMachineSpawns.add(new SpawnFortSiege(fort.getId(), x, y, z, heading, mainMachineId, i));
				}
				catch (final Exception e)
				{
					warn("Error while loading main machine for " + fort.getName() + " fort.");
				}
			}
			_mainMachineList.put(fort.getId(), _mainMachineSpawns);
		}
	}

	public final List<SpawnFortSiege> getCommanderSpawnList(int fortId)
	{
		if (_commanderSpawnList.containsKey(fortId))
		{
			return _commanderSpawnList.get(fortId);
		}
		return null;
	}
	
	public final List<SpawnFortSiege> getPowerUnitSpawnList(int fortId)
	{
		if (_powerUnitList.containsKey(fortId))
		{
			return _powerUnitList.get(fortId);
		}
		return null;
	}
	
	public final List<SpawnFortSiege> getControlUnitSpawnList(int fortId)
	{
		if (_controlUnitList.containsKey(fortId))
		{
			return _controlUnitList.get(fortId);
		}
		return null;
	}
	
	public final List<SpawnFortSiege> getMainMachineSpawnList(int fortId)
	{
		if (_mainMachineList.containsKey(fortId))
		{
			return _mainMachineList.get(fortId);
		}
		return null;
	}

	public final List<CombatFlag> getFlagList(int fortId)
	{
		if (_flagList.containsKey(fortId))
		{
			return _flagList.get(fortId);
		}
		return null;
	}

	public final int getAttackerMaxClans()
	{
		return _attackerMaxClans;
	}

	public final int getFlagMaxCount()
	{
		return _flagMaxCount;
	}

	public final int getSuspiciousMerchantRespawnDelay()
	{
		return _suspiciousMerchantRespawnDelay;
	}

	public final FortSiege getSiege(GameObject activeObject)
	{
		return getSiege(activeObject.getX(), activeObject.getY(), activeObject.getZ());
	}

	public final FortSiege getSiege(int x, int y, int z)
	{
		for (final Fort fort : FortManager.getInstance().getForts())
		{
			if (fort.getSiege().checkIfInZone(x, y, z))
			{
				return fort.getSiege();
			}
		}
		return null;
	}

	public final int getSiegeClanMinLevel()
	{
		return _siegeClanMinLevel;
	}

	public final int getSiegeLength()
	{
		return _siegeLength;
	}

	public final int getCountDownLength()
	{
		return _countDownLength;
	}

	public final List<FortSiege> getSieges()
	{
		return _sieges;
	}

	public final void addSiege(FortSiege fortSiege)
	{
		_sieges.add(fortSiege);
	}

	public boolean isCombat(int itemId)
	{
		return (itemId == 9819);
	}

	public boolean activateCombatFlag(Player player, ItemInstance item)
	{
		if (!checkIfCanPickup(player))
		{
			return false;
		}

		final Fort fort = FortManager.getInstance().getFort(player);

		final List<CombatFlag> fcf = _flagList.get(fort.getId());
		for (final CombatFlag cf : fcf)
		{
			if (cf.getCombatFlagInstance() == item)
			{
				cf.activate(player, item);
			}
		}
		return true;
	}

	public boolean checkIfCanPickup(Player player)
	{
		final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.THE_FORTRESS_BATTLE_OF_S1_HAS_FINISHED);
		sm.addItemName(9819);

		if (player.isCombatFlagEquipped())
		{
			player.sendPacket(sm);
			return false;
		}

		final Fort fort = FortManager.getInstance().getFort(player);

		if ((fort == null) || (fort.getId() <= 0))
		{
			player.sendPacket(sm);
			return false;
		}
		else if (!fort.getSiege().getIsInProgress())
		{
			player.sendPacket(sm);
			return false;
		}
		else if (fort.getSiege().getAttackerClan(player.getClan()) == null)
		{
			player.sendPacket(sm);
			return false;
		}
		return true;
	}

	public void dropCombatFlag(Player player, int fortId)
	{
		final Fort fort = FortManager.getInstance().getFortById(fortId);

		final List<CombatFlag> fcf = _flagList.get(fort.getId());

		for (final CombatFlag cf : fcf)
		{
			if (cf.getPlayerObjectId() == player.getObjectId())
			{
				cf.dropIt();
				if (fort.getSiege().getIsInProgress())
				{
					cf.spawnMe();
				}
			}
		}
	}
	
	public int getFortHwidLimit()
	{
		return _fortHwidLimit;
	}

	public static final FortSiegeManager getInstance()
	{
		return SingletonHolder._instance;
	}

	private static class SingletonHolder
	{
		protected static final FortSiegeManager _instance = new FortSiegeManager();
	}
}