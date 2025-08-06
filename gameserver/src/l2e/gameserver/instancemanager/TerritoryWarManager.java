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
import java.io.IOException;
import java.io.InputStream;
import java.sql.ResultSet;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;

import l2e.commons.log.LoggerObject;
import l2e.commons.time.cron.SchedulingPattern;
import l2e.commons.time.cron.SchedulingPattern.InvalidPatternException;
import l2e.commons.util.GameSettings;
import l2e.commons.util.Util;
import l2e.gameserver.Announcements;
import l2e.gameserver.Config;
import l2e.gameserver.SevenSigns;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.data.holder.ClanHolder;
import l2e.gameserver.data.parser.NpcsParser;
import l2e.gameserver.data.parser.SkillTreesParser;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.model.Clan;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.SiegeClan;
import l2e.gameserver.model.SkillLearn;
import l2e.gameserver.model.TerritoryWard;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.DoorInstance;
import l2e.gameserver.model.actor.instance.SiegeFlagInstance;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.entity.Castle;
import l2e.gameserver.model.entity.Fort;
import l2e.gameserver.model.entity.Siegable;
import l2e.gameserver.model.interfaces.IIdentifiable;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.spawn.Spawner;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.GameServerPacket;
import l2e.gameserver.network.serverpackets.SystemMessage;

public class TerritoryWarManager extends LoggerObject implements Siegable
{
	private static final String DELETE = "DELETE FROM territory_registrations WHERE castleId = ? and registeredId = ?";
	private static final String INSERT = "INSERT INTO territory_registrations (castleId, registeredId) values (?, ?)";
	
	public static final TerritoryWarManager getInstance()
	{
		return SingletonHolder._instance;
	}

	public static String qn = "TerritoryWarSuperClass";
	public static int DEFENDERMAXCLANS;
	public static int DEFENDERMAXPLAYERS;
	public static int CLANMINLEVEL;
	public static int PLAYERMINLEVEL;
	public static int MINTWBADGEFORNOBLESS;
	public static int MINTWBADGEFORSTRIDERS;
	public static int MINTWBADGEFORBIGSTRIDER;
	public static Long WARLENGTH;
	public static boolean PLAYER_WITH_WARD_CAN_BE_KILLED_IN_PEACEZONE;
	public static boolean SPAWN_WARDS_WHEN_TW_IS_NOT_IN_PROGRESS;
	public static boolean RETURN_WARDS_WHEN_TW_STARTS;
	public static int TERRITORY_WARDS_LIMIT;
	private static String _siegeDateTime;
	public static final Map<Integer, Integer> TERRITORY_ITEM_IDS = new HashMap<>();
	
	private final Map<Integer, List<Clan>> _registeredClans = new ConcurrentHashMap<>();
	private final Map<Integer, List<Integer>> _registeredMercenaries = new ConcurrentHashMap<>();
	private final Map<Integer, Territory> _territoryList = new ConcurrentHashMap<>();
	protected List<Integer> _disguisedPlayers = new CopyOnWriteArrayList<>();
	private final List<TerritoryWard> _territoryWards = new CopyOnWriteArrayList<>();
	private final Map<Integer, SiegeFlagInstance> _outposts = new ConcurrentHashMap<>();
	private final Map<Clan, SiegeFlagInstance> _clanFlags = new ConcurrentHashMap<>();
	private final Map<Integer, Integer[]> _participantPoints = new ConcurrentHashMap<>();
	protected Calendar _startTWDate = Calendar.getInstance();
	protected boolean _isRegistrationOver = true;
	protected boolean _isTWChannelOpen = false;
	private boolean _isTWInProgress = false;
	protected ScheduledFuture<?> _scheduledStartTWTask = null;
	protected ScheduledFuture<?> _scheduledEndTWTask = null;
	protected ScheduledFuture<?> _scheduledRewardOnlineTask = null;
	
	static
	{
		TERRITORY_ITEM_IDS.put(81, 13757);
		TERRITORY_ITEM_IDS.put(82, 13758);
		TERRITORY_ITEM_IDS.put(83, 13759);
		TERRITORY_ITEM_IDS.put(84, 13760);
		TERRITORY_ITEM_IDS.put(85, 13761);
		TERRITORY_ITEM_IDS.put(86, 13762);
		TERRITORY_ITEM_IDS.put(87, 13763);
		TERRITORY_ITEM_IDS.put(88, 13764);
		TERRITORY_ITEM_IDS.put(89, 13765);
	}
	
	protected TerritoryWarManager()
	{
		load();
		generateNextWarTime();
	}
	
	public int getRegisteredTerritoryId(Player player)
	{
		if ((player == null) || !_isTWChannelOpen || (player.getLevel() < PLAYERMINLEVEL))
		{
			return 0;
		}
		if (player.getClan() != null)
		{
			if (player.getClan().getCastleId() > 0)
			{
				return player.getClan().getCastleId() + 80;
			}
			for (final int cId : _registeredClans.keySet())
			{
				if (_registeredClans.get(cId).contains(player.getClan()))
				{
					return cId + 80;
				}
			}
		}
		for (final int cId : _registeredMercenaries.keySet())
		{
			if (_registeredMercenaries.get(cId).contains(player.getObjectId()))
			{
				return cId + 80;
			}
		}
		return 0;
	}
	
	public boolean isAllyField(Player player, int fieldId)
	{
		if ((player == null) || (player.getSiegeSide() == 0))
		{
			return false;
		}
		else if ((player.getSiegeSide() - 80) == fieldId)
		{
			return true;
		}
		else if ((fieldId > 100) && _territoryList.containsKey((player.getSiegeSide() - 80)) && (_territoryList.get((player.getSiegeSide() - 80)).getFortId() == fieldId))
		{
			return true;
		}
		return false;
	}
	
	public final boolean checkIsRegistered(int castleId, Clan clan)
	{
		if (clan == null)
		{
			return false;
		}
		
		if (clan.getCastleId() > 0)
		{
			return (castleId == -1 ? true : (clan.getCastleId() == castleId));
		}
		
		if (castleId == -1)
		{
			for (final int cId : _registeredClans.keySet())
			{
				if (_registeredClans.get(cId).contains(clan))
				{
					return true;
				}
			}
			return false;
		}
		return _registeredClans.get(castleId).contains(clan);
	}
	
	public boolean canStealWard(Player player)
	{
		if (player == null || player.getClan() == null)
		{
			return false;
		}
		
		int currectWards = 0;
		for (final Territory t : _territoryList.values())
		{
			if (t.getOwnerClan() != null && t.getOwnerClan() == player.getClan())
			{
				currectWards = t.getWardsAmount();
				break;
			}
		}
		return currectWards < TERRITORY_WARDS_LIMIT;
	}
	
	public final boolean checkIsRegistered(int castleId, int objId)
	{
		if (castleId == -1)
		{
			for (final int cId : _registeredMercenaries.keySet())
			{
				if (_registeredMercenaries.get(cId).contains(objId))
				{
					return true;
				}
			}
			return false;
		}
		return _registeredMercenaries.get(castleId).contains(objId);
	}
	
	public Territory getTerritory(int castleId)
	{
		return _territoryList.get(castleId);
	}
	
	public List<Territory> getAllTerritories()
	{
		final List<Territory> ret = new LinkedList<>();
		for (final Territory t : _territoryList.values())
		{
			if (t.getOwnerClan() != null)
			{
				ret.add(t);
			}
		}
		return ret;
	}
	
	public Collection<Clan> getRegisteredClans(int castleId)
	{
		return _registeredClans.get(castleId);
	}
	
	public void addDisguisedPlayer(int playerObjId)
	{
		_disguisedPlayers.add(playerObjId);
	}
	
	public boolean isDisguised(int playerObjId)
	{
		return _disguisedPlayers.contains(playerObjId);
	}
	
	public Collection<Integer> getRegisteredMercenaries(int castleId)
	{
		return _registeredMercenaries.get(castleId);
	}
	
	public long getTWStartTimeInMillis()
	{
		return _startTWDate.getTimeInMillis();
	}
	
	public Calendar getTWStart()
	{
		return _startTWDate;
	}
	
	public void setTWStartTimeInMillis(long time)
	{
		_startTWDate.setTimeInMillis(time);
		if (_isTWInProgress)
		{
			if (_scheduledEndTWTask != null)
			{
				_scheduledEndTWTask.cancel(false);
			}
			_scheduledEndTWTask = ThreadPoolManager.getInstance().schedule(new ScheduleEndTWTask(), 1000);
		}
		else
		{
			if (_scheduledStartTWTask != null)
			{
				_scheduledStartTWTask.cancel(false);
			}
			_scheduledStartTWTask = ThreadPoolManager.getInstance().schedule(new ScheduleStartTWTask(), 1000);
		}
	}
	
	public boolean isTWChannelOpen()
	{
		return _isTWChannelOpen;
	}
	
	public void registerClan(int castleId, Clan clan)
	{
		if ((clan == null) || ((_registeredClans.get(castleId) != null) && _registeredClans.get(castleId).contains(clan)))
		{
			return;
		}
		
		_registeredClans.putIfAbsent(castleId, new CopyOnWriteArrayList<>());
		_registeredClans.get(castleId).add(clan);
		changeRegistration(castleId, clan.getId(), false);
	}
	
	public void registerMerc(int castleId, Player player)
	{
		if ((player == null) || (player.getLevel() < PLAYERMINLEVEL) || ((_registeredMercenaries.get(castleId) != null) && _registeredMercenaries.get(castleId).contains(player.getObjectId())))
		{
			return;
		}
		else if (_registeredMercenaries.get(castleId) == null)
		{
			_registeredMercenaries.put(castleId, new CopyOnWriteArrayList<>());
		}
		
		_registeredMercenaries.get(castleId).add(player.getObjectId());
		changeRegistration(castleId, player.getObjectId(), false);
	}
	
	public void removeClan(int castleId, Clan clan)
	{
		if (clan == null)
		{
			return;
		}
		else if ((_registeredClans.get(castleId) != null) && _registeredClans.get(castleId).contains(clan))
		{
			_registeredClans.get(castleId).remove(clan);
			changeRegistration(castleId, clan.getId(), true);
		}
	}
	
	public void removeMerc(int castleId, Player player)
	{
		if (player == null)
		{
			return;
		}
		else if ((_registeredMercenaries.get(castleId) != null) && _registeredMercenaries.get(castleId).contains(player.getObjectId()))
		{
			_registeredMercenaries.get(castleId).remove(_registeredMercenaries.get(castleId).indexOf(player.getObjectId()));
			changeRegistration(castleId, player.getObjectId(), true);
		}
	}
	
	public boolean getIsRegistrationOver()
	{
		return _isRegistrationOver;
	}
	
	public boolean isTWInProgress()
	{
		return _isTWInProgress;
	}
	
	public void territoryCatapultDestroyed(int castleId)
	{
		if (_territoryList.get(castleId) != null)
		{
			_territoryList.get(castleId).changeNPCsSpawn(2, false);
		}
		for (final DoorInstance door : CastleManager.getInstance().getCastleById(castleId).getDoors())
		{
			door.openMe();
		}
	}
	
	public Npc addTerritoryWard(int territoryId, int newOwnerId, int oldOwnerId, boolean broadcastMessage)
	{
		Npc ret = null;
		if (_territoryList.get(newOwnerId) != null)
		{
			final Territory terNew = _territoryList.get(newOwnerId);
			final TerritoryNPCSpawn ward = terNew.getFreeWardSpawnPlace();
			if (ward != null)
			{
				ward._npcId = territoryId;
				ret = spawnNPC(36491 + territoryId, ward.getLocation());
				ward.setNPC(ret);
				if (!isTWInProgress() && !SPAWN_WARDS_WHEN_TW_IS_NOT_IN_PROGRESS)
				{
					ret.decayMe();
				}
				if ((terNew.getOwnerClan() != null) && terNew.getOwnedWardIds().contains(newOwnerId + 80))
				{
					for (final int wardId : terNew.getOwnedWardIds())
					{
						final List<SkillLearn> residentialSkills = SkillTreesParser.getInstance().getAvailableResidentialSkills(wardId);
						for (final SkillLearn s : residentialSkills)
						{
							final Skill sk = SkillsParser.getInstance().getInfo(s.getId(), s.getLvl());
							if (sk != null)
							{
								for (final Player member : terNew.getOwnerClan().getOnlineMembers(0))
								{
									if (!member.isInOlympiadMode())
									{
										member.addSkill(sk, false);
									}
								}
							}
						}
					}
				}
			}
			if (_territoryList.containsKey(oldOwnerId))
			{
				final Territory terOld = _territoryList.get(oldOwnerId);
				terOld.removeWard(territoryId);
				updateTerritoryData(terOld);
				updateTerritoryData(terNew);
				if (broadcastMessage)
				{
					final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.CLAN_S1_HAS_SUCCEDED_IN_CAPTURING_S2_TERRITORY_WARD);
					sm.addString(terNew.getOwnerClan().getName());
					sm.addCastleId(terNew.getTerritoryId());
					announceToParticipants(sm, 135000, 13500);
				}
				if (terOld.getOwnerClan() != null)
				{
					final List<SkillLearn> territorySkills = SkillTreesParser.getInstance().getAvailableResidentialSkills(territoryId);
					for (final SkillLearn s : territorySkills)
					{
						final Skill sk = SkillsParser.getInstance().getInfo(s.getId(), s.getLvl());
						if (sk != null)
						{
							for (final Player member : terOld.getOwnerClan().getOnlineMembers(0))
							{
								member.removeSkill(sk, false);
							}
						}
					}
					
					if (!terOld.getOwnedWardIds().isEmpty() && !terOld.getOwnedWardIds().contains(oldOwnerId + 80))
					{
						for (final int wardId : terOld.getOwnedWardIds())
						{
							final List<SkillLearn> wardSkills = SkillTreesParser.getInstance().getAvailableResidentialSkills(wardId);
							for (final SkillLearn s : wardSkills)
							{
								final Skill sk = SkillsParser.getInstance().getInfo(s.getId(), s.getLvl());
								if (sk != null)
								{
									for (final Player member : terOld.getOwnerClan().getOnlineMembers(0))
									{
										member.removeSkill(sk, false);
									}
								}
							}
						}
					}
				}
			}
		}
		else
		{
			warn("Missing territory for new Ward owner: " + newOwnerId + ";" + territoryId);
		}
		return ret;
	}
	
	public SiegeFlagInstance getHQForClan(Clan clan)
	{
		if (clan != null && clan.getCastleId() > 0)
		{
			return _outposts.get(clan.getCastleId());
		}
		return null;
	}
	
	public void setHQForClan(Clan clan, SiegeFlagInstance hq)
	{
		if (clan != null && clan.getCastleId() > 0)
		{
			_outposts.put(clan.getCastleId(), hq);
		}
	}
	
	public void removeHQForClan(Clan clan)
	{
		if (clan != null && clan.getCastleId() > 0)
		{
			final SiegeFlagInstance flag = _outposts.remove(clan.getCastleId());
			if (flag != null)
			{
				flag.deleteMe();
			}
		}
	}
	
	public void addClanFlag(Clan clan, SiegeFlagInstance flag)
	{
		_clanFlags.put(clan, flag);
	}
	
	public boolean isClanHasFlag(Clan clan)
	{
		return _clanFlags.containsKey(clan);
	}
	
	public SiegeFlagInstance getFlagForClan(Clan clan)
	{
		return _clanFlags.get(clan);
	}
	
	public void removeClanFlag(Clan clan)
	{
		_clanFlags.remove(clan);
	}
	
	public List<TerritoryWard> getAllTerritoryWards()
	{
		return _territoryWards;
	}
	
	public TerritoryWard getTerritoryWardForOwner(int castleId)
	{
		for (final TerritoryWard twWard : _territoryWards)
		{
			if (twWard.getTerritoryId() == castleId)
			{
				return twWard;
			}
		}
		return null;
	}
	
	public TerritoryWard getTerritoryWard(int territoryId)
	{
		for (final TerritoryWard twWard : _territoryWards)
		{
			if (twWard.getTerritoryId() == territoryId)
			{
				return twWard;
			}
		}
		return null;
	}
	
	public TerritoryWard getTerritoryWard(Player player)
	{
		for (final TerritoryWard twWard : _territoryWards)
		{
			if (twWard.playerId == player.getObjectId())
			{
				return twWard;
			}
		}
		return null;
	}
	
	public void dropCombatFlag(Player player, boolean isKilled, boolean isSpawnBack)
	{
		for (final TerritoryWard twWard : _territoryWards)
		{
			if (twWard.playerId == player.getObjectId())
			{
				twWard.dropIt();
				if (isTWInProgress())
				{
					if (isKilled)
					{
						twWard.spawnMe();
					}
					else if (isSpawnBack)
					{
						twWard.spawnBack();
					}
					else
					{
						for (final TerritoryNPCSpawn wardSpawn : _territoryList.get(twWard.getOwnerCastleId()).getOwnedWard())
						{
							if (wardSpawn.getId() == twWard.getTerritoryId())
							{
								wardSpawn.setNPC(wardSpawn.getNpc().getSpawn().doSpawn());
								twWard.unSpawnMe();
								twWard.setNpc(wardSpawn.getNpc());
							}
						}
					}
				}
				if (isKilled)
				{
					final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.THE_CHAR_THAT_ACQUIRED_S1_WARD_HAS_BEEN_KILLED);
					sm.addString(twWard.getNpc().getName("en").replaceAll(" Ward", ""));
					announceToParticipants(sm, 0, 0);
				}
			}
		}
	}
	
	public void giveTWQuestPoint(Player player)
	{
		if (!_participantPoints.containsKey(player.getObjectId()))
		{
			_participantPoints.put(player.getObjectId(), new Integer[]
			{
			        player.getSiegeSide(), 0, 0, 0, 0, 0, 0
			});
		}
		_participantPoints.get(player.getObjectId())[2]++;
	}
	
	public void giveTWPoint(Player killer, int victimSide, int type)
	{
		if (victimSide == 0)
		{
			return;
		}
		if ((killer.getParty() != null) && (type < 5))
		{
			for (final Player pl : killer.getParty().getMembers())
			{
				if ((pl.getSiegeSide() == victimSide) || (pl.getSiegeSide() == 0) || !Util.checkIfInRange(2000, killer, pl, false))
				{
					continue;
				}
				else if (!_participantPoints.containsKey(pl.getObjectId()))
				{
					_participantPoints.put(pl.getObjectId(), new Integer[]
					{
					        pl.getSiegeSide(), 0, 0, 0, 0, 0, 0
					});
				}
				_participantPoints.get(pl.getObjectId())[type]++;
			}
		}
		else
		{
			if ((killer.getSiegeSide() == victimSide) || (killer.getSiegeSide() == 0))
			{
				return;
			}
			else if (!_participantPoints.containsKey(killer.getObjectId()))
			{
				_participantPoints.put(killer.getObjectId(), new Integer[]
				{
				        killer.getSiegeSide(), 0, 0, 0, 0, 0, 0
				});
			}
			_participantPoints.get(killer.getObjectId())[type]++;
		}
	}
	
	public int[] calcReward(Player player)
	{
		if (_participantPoints.containsKey(player.getObjectId()))
		{
			final int[] reward = new int[2];
			final Integer[] temp = _participantPoints.get(player.getObjectId());
			reward[0] = temp[0];
			reward[1] = 0;
			
			if (temp[6] < 10)
			{
				return reward;
			}
			reward[1] += (temp[6] > 70 ? 7 : (int) (temp[6] * 0.1));
			reward[1] += temp[2] * 7;
			if (temp[1] < 50)
			{
				reward[1] += temp[1] * 0.1;
			}
			else if (temp[1] < 120)
			{
				reward[1] += (5 + ((temp[1] - 50) / 14));
			}
			else
			{
				reward[1] += 10;
			}
			reward[1] += temp[3];
			reward[1] += temp[4] * 2;
			reward[1] += (temp[5] > 0 ? 5 : 0);
			reward[1] += Math.min(_territoryList.get(temp[0] - 80).getQuestDone()[0], 10);
			reward[1] += _territoryList.get(temp[0] - 80).getQuestDone()[1];
			reward[1] += _territoryList.get(temp[0] - 80).getOwnedWardIds().size();
			return reward;
		}
		return new int[]
		{
		        0, 0
		};
	}
	
	public void debugReward(Player player)
	{
		player.sendMessage("Registred TerrId: " + player.getSiegeSide());
		if (_participantPoints.containsKey(player.getObjectId()))
		{
			final Integer[] temp = _participantPoints.get(player.getObjectId());
			player.sendMessage("TerrId: " + temp[0]);
			player.sendMessage("PcKill: " + temp[1]);
			player.sendMessage("PcQuests: " + temp[2]);
			player.sendMessage("npcKill: " + temp[3]);
			player.sendMessage("CatatKill: " + temp[4]);
			player.sendMessage("WardKill: " + temp[5]);
			player.sendMessage("onlineTime: " + temp[6]);
		}
		else
		{
			player.sendMessage("No points for you!");
		}
		if (_territoryList.containsKey(player.getSiegeSide() - 80))
		{
			player.sendMessage("Your Territory's jobs:");
			player.sendMessage("npcKill: " + _territoryList.get(player.getSiegeSide() - 80).getQuestDone()[0]);
			player.sendMessage("WardCaptured: " + _territoryList.get(player.getSiegeSide() - 80).getQuestDone()[1]);
		}
	}
	
	public void resetReward(Player player)
	{
		if (_participantPoints.containsKey(player.getObjectId()))
		{
			_participantPoints.get(player.getObjectId())[6] = 0;
		}
	}
	
	public Npc spawnNPC(int npcId, Location loc)
	{
		final NpcTemplate template = NpcsParser.getInstance().getTemplate(npcId);
		if (template != null)
		{
			Spawner spawnDat;
			try
			{
				spawnDat = new Spawner(template);
				spawnDat.setAmount(1);
				spawnDat.setX(loc.getX());
				spawnDat.setY(loc.getY());
				spawnDat.setZ(loc.getZ());
				spawnDat.setHeading(loc.getHeading());
				spawnDat.stopRespawn();
				return spawnDat.spawnOne(false);
			}
			catch (final Exception e)
			{
				warn("" + e.getMessage(), e);
			}
		}
		else
		{
			warn("Data missing in NPC table for ID: " + npcId + ".");
		}
		return null;
	}
	
	private void changeRegistration(int castleId, int objId, boolean delete)
	{
		final String query = delete ? DELETE : INSERT;
		try (
		    var con = DatabaseFactory.getInstance().getConnection(); var statement = con.prepareStatement(query))
		{
			statement.setInt(1, castleId);
			statement.setInt(2, objId);
			statement.execute();
		}
		catch (final Exception e)
		{
			warn("registration: " + e.getMessage(), e);
		}
	}
	
	private void updateTerritoryData(Territory ter)
	{
		try (
		    var con = DatabaseFactory.getInstance().getConnection())
		{
			final var statement = con.prepareStatement("UPDATE territories SET ownedWardIds=? WHERE territoryId=?");
			final StringBuilder wardList = new StringBuilder();
			for (final int i : ter.getOwnedWardIds())
			{
				wardList.append(i + ";");
			}
			statement.setString(1, wardList.toString());
			statement.setInt(2, ter.getTerritoryId());
			statement.execute();
			statement.close();
		}
		catch (final Exception e)
		{
			warn("Territory Data update: " + e.getMessage(), e);
		}
	}
	
	private void updateTerritoryLord(int territoryId, int lordObjId)
	{
		try (
		    var con = DatabaseFactory.getInstance().getConnection())
		{
			final var statement = con.prepareStatement("UPDATE territories SET lordObjectId=? WHERE territoryId=?");
			statement.setInt(1, lordObjId);
			statement.setInt(2, territoryId);
			statement.execute();
			statement.close();
		}
		catch (final Exception e)
		{
			warn("Territory Lord Object update: " + e.getMessage(), e);
		}
	}
	
	private final void load()
	{
		final GameSettings territoryWarSettings = new GameSettings();
		try (
		    InputStream is = new FileInputStream(new File(Config.TW_CONFIGURATION_FILE)))
		{
			territoryWarSettings.load(is);
		}
		catch (final IOException e)
		{
			warn("Error while loading Territory War Manager settings!", e);
		}
		
		DEFENDERMAXCLANS = Integer.decode(territoryWarSettings.getProperty("DefenderMaxClans", "500"));
		DEFENDERMAXPLAYERS = Integer.decode(territoryWarSettings.getProperty("DefenderMaxPlayers", "500"));
		CLANMINLEVEL = Integer.decode(territoryWarSettings.getProperty("ClanMinLevel", "0"));
		PLAYERMINLEVEL = Integer.decode(territoryWarSettings.getProperty("PlayerMinLevel", "40"));
		WARLENGTH = Long.decode(territoryWarSettings.getProperty("WarLength", "120")) * 60000;
		PLAYER_WITH_WARD_CAN_BE_KILLED_IN_PEACEZONE = Boolean.parseBoolean(territoryWarSettings.getProperty("PlayerWithWardCanBeKilledInPeaceZone", "False"));
		SPAWN_WARDS_WHEN_TW_IS_NOT_IN_PROGRESS = Boolean.parseBoolean(territoryWarSettings.getProperty("SpawnWardsWhenTWIsNotInProgress", "False"));
		RETURN_WARDS_WHEN_TW_STARTS = Boolean.parseBoolean(territoryWarSettings.getProperty("ReturnWardsWhenTWStarts", "False"));
		MINTWBADGEFORNOBLESS = Integer.decode(territoryWarSettings.getProperty("MinTerritoryBadgeForNobless", "100"));
		MINTWBADGEFORSTRIDERS = Integer.decode(territoryWarSettings.getProperty("MinTerritoryBadgeForStriders", "50"));
		MINTWBADGEFORBIGSTRIDER = Integer.decode(territoryWarSettings.getProperty("MinTerritoryBadgeForBigStrider", "80"));
		TERRITORY_WARDS_LIMIT = Integer.decode(territoryWarSettings.getProperty("TerritoryWardsLimit", "9"));
		_siegeDateTime = territoryWarSettings.getProperty("TerritorySiegePattern", "");
		try (
		    var con = DatabaseFactory.getInstance().getConnection())
		{
			final var statement = con.prepareStatement("SELECT * FROM territory_spawnlist");
			final ResultSet rs = statement.executeQuery();
			
			while (rs.next())
			{
				final int castleId = rs.getInt("castleId");
				final int npcId = rs.getInt("npcId");
				final Location loc = new Location(rs.getInt("x"), rs.getInt("y"), rs.getInt("z"), rs.getInt("heading"));
				final int spawnType = rs.getInt("spawnType");
				if (!_territoryList.containsKey(castleId))
				{
					_territoryList.put(castleId, new Territory(castleId));
				}
				switch (spawnType)
				{
					case 0 :
					case 1 :
					case 2 :
						_territoryList.get(castleId).getSpawnList().add(new TerritoryNPCSpawn(castleId, loc, npcId, spawnType, null));
						break;
					case 3 :
						_territoryList.get(castleId).addWardSpawnPlace(loc);
						break;
					default :
						warn("Unknown npc type for " + rs.getInt("id"));
				}
			}
			
			rs.close();
			statement.close();
		}
		catch (final Exception e)
		{
			warn("SpawnList error: " + e.getMessage(), e);
		}
		
		try (
		    var con = DatabaseFactory.getInstance().getConnection())
		{
			final var statement = con.prepareStatement("SELECT * FROM territories");
			final ResultSet rs = statement.executeQuery();
			
			while (rs.next())
			{
				final int castleId = rs.getInt("castleId");
				final int fortId = rs.getInt("fortId");
				final String ownedWardIds = rs.getString("OwnedWardIds");
				final int lordObjectId = rs.getInt("lordObjectId");
				
				final Territory t = _territoryList.get(castleId);
				if (t != null)
				{
					t._fortId = fortId;
					t._lordObjId = lordObjectId;
					if (CastleManager.getInstance().getCastleById(castleId).getOwnerId() > 0)
					{
						t.setOwnerClan(ClanHolder.getInstance().getClan(CastleManager.getInstance().getCastleById(castleId).getOwnerId()));
						t.changeNPCsSpawn(0, true);
					}
					
					if (!ownedWardIds.isEmpty())
					{
						for (final String wardId : ownedWardIds.split(";"))
						{
							if (Integer.parseInt(wardId) > 0)
							{
								addTerritoryWard(Integer.parseInt(wardId), castleId, 0, false);
							}
						}
					}
				}
			}
			rs.close();
			statement.close();
		}
		catch (final Exception e)
		{
			warn("territory list error(): " + e.getMessage(), e);
		}
		
		try (
		    var con = DatabaseFactory.getInstance().getConnection())
		{
			final var statement = con.prepareStatement("SELECT * FROM territory_registrations");
			final ResultSet rs = statement.executeQuery();

			while (rs.next())
			{
				final int castleId = rs.getInt("castleId");
				final int registeredId = rs.getInt("registeredId");
				if (ClanHolder.getInstance().getClan(registeredId) != null)
				{
					if (_registeredClans.get(castleId) == null)
					{
						_registeredClans.putIfAbsent(castleId, new CopyOnWriteArrayList<>());
					}
					_registeredClans.get(castleId).add(ClanHolder.getInstance().getClan(registeredId));
				}
				else
				{
					if (_registeredMercenaries.get(castleId) == null)
					{
						_registeredMercenaries.put(castleId, new CopyOnWriteArrayList<>());
					}
					_registeredMercenaries.get(castleId).add(registeredId);
				}
			}
			rs.close();
			statement.close();
		}
		catch (final Exception e)
		{
			warn("registration list error: " + e.getMessage(), e);
		}
	}
	
	private boolean haveActiveTerritoryList()
	{
		final List<Territory> activeTerritoryList = new LinkedList<>();
		for (final Territory t : _territoryList.values())
		{
			final Castle castle = CastleManager.getInstance().getCastleById(t.getCastleId());
			if (castle != null)
			{
				if (castle.getOwnerId() > 0)
				{
					activeTerritoryList.add(t);
				}
			}
		}
		
		if (activeTerritoryList.size() < 2)
		{
			return false;
		}
		return true;
	}
	
	protected void startTerritoryWar()
	{
		if (_territoryList == null)
		{
			warn("TerritoryList is NULL!");
			return;
		}
		final List<Territory> activeTerritoryList = new LinkedList<>();
		for (final Territory t : _territoryList.values())
		{
			final Castle castle = CastleManager.getInstance().getCastleById(t.getCastleId());
			if (castle != null)
			{
				if (castle.getOwnerId() > 0)
				{
					activeTerritoryList.add(t);
				}
			}
			else
			{
				warn("Castle missing! CastleId: " + t.getCastleId());
			}
		}
		
		if (activeTerritoryList.size() < 2)
		{
			generateNextWarTime();
			return;
		}
		
		_isTWInProgress = true;
		if (!updatePlayerTWStateFlags(false))
		{
			return;
		}
		
		for (final Territory t : activeTerritoryList)
		{
			final Castle castle = CastleManager.getInstance().getCastleById(t.getCastleId());
			final Fort fort = FortManager.getInstance().getFortById(t.getFortId());
			if (castle != null)
			{
				t.changeNPCsSpawn(2, true);
				castle.spawnDoor();
				castle.getZone().setSiegeInstance(this);
				castle.getZone().setIsActive(true);
				castle.getZone().updateZoneStatusForCharactersInside();
			}
			else
			{
				warn("Castle missing! CastleId: " + t.getCastleId());
			}
			if (fort != null)
			{
				t.changeNPCsSpawn(1, true);
				fort.resetDoors();
				fort.getZone().setSiegeInstance(this);
				fort.getZone().setIsActive(true);
				fort.getZone().updateZoneStatusForCharactersInside();
			}
			else
			{
				warn("Fort missing! FortId: " + t.getFortId());
			}
			for (final TerritoryNPCSpawn ward : t.getOwnedWard())
			{
				if ((ward.getNpc() != null) && (t.getOwnerClan() != null))
				{
					if (!ward.getNpc().isVisible())
					{
						ward.setNPC(ward.getNpc().getSpawn().doSpawn());
					}
					_territoryWards.add(new TerritoryWard(ward.getId(), ward.getLocation().getX(), ward.getLocation().getY(), ward.getLocation().getZ(), 0, ward.getId() + 13479, t.getCastleId(), ward.getNpc()));
				}
			}
			t.getQuestDone()[0] = 0;
			t.getQuestDone()[1] = 0;
		}
		_participantPoints.clear();
		
		if (RETURN_WARDS_WHEN_TW_STARTS)
		{
			for (final TerritoryWard ward : _territoryWards)
			{
				if (ward.getOwnerCastleId() != (ward.getTerritoryId() - 80))
				{
					ward.unSpawnMe();
					ward.setNpc(addTerritoryWard(ward.getTerritoryId(), ward.getTerritoryId() - 80, ward.getOwnerCastleId(), false));
					ward.setOwnerCastleId(ward.getTerritoryId() - 80);
				}
			}
		}
		
		final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.TERRITORY_WAR_HAS_BEGUN);
		Announcements.getInstance().announceToAll(sm);
	}
	
	protected void endTerritoryWar()
	{
		_isTWInProgress = false;
		if (_territoryList == null)
		{
			warn("TerritoryList is NULL!");
			return;
		}
		final List<Territory> activeTerritoryList = new LinkedList<>();
		for (final Territory t : _territoryList.values())
		{
			final Castle castle = CastleManager.getInstance().getCastleById(t.getCastleId());
			if (castle != null)
			{
				if (castle.getOwnerId() > 0)
				{
					activeTerritoryList.add(t);
				}
			}
			else
			{
				warn("Castle missing! CastleId: " + t.getCastleId());
			}
		}
		
		if (activeTerritoryList.size() < 2)
		{
			generateNextWarTime();
			return;
		}
		
		if (!updatePlayerTWStateFlags(true))
		{
			return;
		}
		
		if (_territoryWards != null)
		{
			for (final TerritoryWard twWard : _territoryWards)
			{
				twWard.unSpawnMe();
			}
			_territoryWards.clear();
		}
		
		for (final Territory t : activeTerritoryList)
		{
			final Castle castle = CastleManager.getInstance().getCastleById(t.getCastleId());
			final Fort fort = FortManager.getInstance().getFortById(t.getFortId());
			
			if (castle != null)
			{
				castle.spawnDoor();
				t.changeNPCsSpawn(2, false);
				castle.getZone().setIsActive(false);
				castle.getZone().updateZoneStatusForCharactersInside();
				castle.getZone().setSiegeInstance(null);
			}
			else
			{
				warn("Castle missing! CastleId: " + t.getCastleId());
			}
			
			if (fort != null)
			{
				t.changeNPCsSpawn(1, false);
				fort.getZone().setIsActive(false);
				fort.getZone().updateZoneStatusForCharactersInside();
				fort.getZone().setSiegeInstance(null);
			}
			else
			{
				warn("Fort missing! FortId: " + t.getFortId());
			}
			
			for (final TerritoryNPCSpawn ward : t.getOwnedWard())
			{
				if (ward.getNpc() != null)
				{
					if (!ward.getNpc().isVisible() && SPAWN_WARDS_WHEN_TW_IS_NOT_IN_PROGRESS)
					{
						ward.setNPC(ward.getNpc().getSpawn().doSpawn());
					}
					else if (ward.getNpc().isVisible() && !SPAWN_WARDS_WHEN_TW_IS_NOT_IN_PROGRESS)
					{
						ward.getNpc().decayMe();
					}
				}
			}
		}
		
		for (final SiegeFlagInstance flag : _outposts.values())
		{
			flag.deleteMe();
		}
		_outposts.clear();
		
		for (final SiegeFlagInstance flag : _clanFlags.values())
		{
			flag.deleteMe();
		}
		_clanFlags.clear();
		
		for (final Integer castleId : _registeredClans.keySet())
		{
			for (final Clan clan : _registeredClans.get(castleId))
			{
				changeRegistration(castleId, clan.getId(), true);
			}
		}
		for (final Integer castleId : _registeredMercenaries.keySet())
		{
			for (final Integer pl_objId : _registeredMercenaries.get(castleId))
			{
				changeRegistration(castleId, pl_objId, true);
			}
		}
		final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.TERRITORY_WAR_HAS_ENDED);
		Announcements.getInstance().announceToAll(sm);
		generateNextWarTime();
	}
	
	protected boolean updatePlayerTWStateFlags(boolean clear)
	{
		final Quest twQuest = QuestManager.getInstance().getQuest(qn);
		if (twQuest == null)
		{
			warn("missing main Quest!");
			return false;
		}
		for (final int castleId : _registeredClans.keySet())
		{
			for (final Clan clan : _registeredClans.get(castleId))
			{
				for (final Player player : clan.getOnlineMembers(0))
				{
					if (player == null)
					{
						continue;
					}
					if (clear)
					{
						player.setSiegeState((byte) 0);
						if (!_isTWChannelOpen)
						{
							player.setSiegeSide(0);
						}
					}
					else
					{
						if ((player.getLevel() < PLAYERMINLEVEL) || (player.getClassId().level() < 2))
						{
							continue;
						}
						if (_isTWInProgress)
						{
							player.setSiegeState((byte) 1);
						}
						player.setSiegeSide(80 + castleId);
					}
					player.broadcastUserInfo(true);
				}
			}
		}
		for (final int castleId : _registeredMercenaries.keySet())
		{
			for (final int objId : _registeredMercenaries.get(castleId))
			{
				final Player player = GameObjectsStorage.getPlayer(objId);
				if (player == null)
				{
					continue;
				}
				if (clear)
				{
					player.setSiegeState((byte) 0);
					if (!_isTWChannelOpen)
					{
						player.setSiegeSide(0);
					}
				}
				else
				{
					if (_isTWInProgress)
					{
						player.setSiegeState((byte) 1);
					}
					player.setSiegeSide(80 + castleId);
				}
				player.broadcastUserInfo(true);
			}
		}
		for (final Territory terr : _territoryList.values())
		{
			if (terr.getOwnerClan() != null)
			{
				for (final Player player : terr.getOwnerClan().getOnlineMembers(0))
				{
					if (player == null)
					{
						continue;
					}
					if (clear)
					{
						player.setSiegeState((byte) 0);
						if (!_isTWChannelOpen)
						{
							player.setSiegeSide(0);
						}
					}
					else
					{
						if ((player.getLevel() < PLAYERMINLEVEL) || (player.getClassId().level() < 2))
						{
							continue;
						}
						if (_isTWInProgress)
						{
							player.setSiegeState((byte) 1);
						}
						player.setSiegeSide(80 + terr.getCastleId());
					}
					player.broadcastUserInfo(true);
				}
			}
		}
		twQuest.setOnEnterWorld(_isTWInProgress);
		return true;
	}
	
	private class RewardOnlineParticipants implements Runnable
	{
		public RewardOnlineParticipants()
		{
		}
		
		@Override
		public void run()
		{
			if (isTWInProgress())
			{
				for (final Player player : GameObjectsStorage.getPlayers())
				{
					if ((player != null) && (player.getSiegeSide() > 0))
					{
						giveTWPoint(player, 1000, 6);
					}
				}
			}
			else
			{
				_scheduledRewardOnlineTask.cancel(false);
			}
		}
	}
	
	private class ScheduleStartTWTask implements Runnable
	{
		public ScheduleStartTWTask()
		{
		}
		
		@Override
		public void run()
		{
			_scheduledStartTWTask.cancel(false);
			try
			{
				final long timeRemaining = _startTWDate.getTimeInMillis() - Calendar.getInstance().getTimeInMillis();
				if (timeRemaining > 7200000)
				{
					_isRegistrationOver = false;
					_scheduledStartTWTask = ThreadPoolManager.getInstance().schedule(new ScheduleStartTWTask(), timeRemaining - 7200000);
				}
				else if ((timeRemaining <= 7200000) && (timeRemaining > 1200000))
				{
					final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.THE_TERRITORY_WAR_REGISTERING_PERIOD_ENDED);
					Announcements.getInstance().announceToAll(sm);
					_isRegistrationOver = true;
					_scheduledStartTWTask = ThreadPoolManager.getInstance().schedule(new ScheduleStartTWTask(), timeRemaining - 1200000);
				}
				else if ((timeRemaining <= 1200000) && (timeRemaining > 600000))
				{
					if (haveActiveTerritoryList())
					{
						Announcements.getInstance().announceToAll(SystemMessage.getSystemMessage(SystemMessageId.TERRITORY_WAR_BEGINS_IN_20_MINUTES));
					}
					_isTWChannelOpen = true;
					_isRegistrationOver = true;
					updatePlayerTWStateFlags(false);
					_scheduledStartTWTask = ThreadPoolManager.getInstance().schedule(new ScheduleStartTWTask(), timeRemaining - 600000);
				}
				else if ((timeRemaining <= 600000) && (timeRemaining > 300000))
				{
					if (haveActiveTerritoryList())
					{
						Announcements.getInstance().announceToAll(SystemMessage.getSystemMessage(SystemMessageId.TERRITORY_WAR_BEGINS_IN_10_MINUTES));
					}
					_isTWChannelOpen = true;
					_isRegistrationOver = true;
					updatePlayerTWStateFlags(false);
					_scheduledStartTWTask = ThreadPoolManager.getInstance().schedule(new ScheduleStartTWTask(), timeRemaining - 300000);
				}
				else if ((timeRemaining <= 300000) && (timeRemaining > 60000))
				{
					if (haveActiveTerritoryList())
					{
						Announcements.getInstance().announceToAll(SystemMessage.getSystemMessage(SystemMessageId.TERRITORY_WAR_BEGINS_IN_5_MINUTES));
					}
					_isTWChannelOpen = true;
					_isRegistrationOver = true;
					updatePlayerTWStateFlags(false);
					_scheduledStartTWTask = ThreadPoolManager.getInstance().schedule(new ScheduleStartTWTask(), timeRemaining - 60000);
				}
				else if ((timeRemaining <= 60000) && (timeRemaining > 0))
				{
					if (haveActiveTerritoryList())
					{
						Announcements.getInstance().announceToAll(SystemMessage.getSystemMessage(SystemMessageId.TERRITORY_WAR_BEGINS_IN_1_MINUTE));
					}
					_isTWChannelOpen = true;
					_isRegistrationOver = true;
					updatePlayerTWStateFlags(false);
					_scheduledStartTWTask = ThreadPoolManager.getInstance().schedule(new ScheduleStartTWTask(), timeRemaining);
				}
				else if ((timeRemaining + WARLENGTH) > 0)
				{
					_isTWChannelOpen = true;
					_isRegistrationOver = true;
					startTerritoryWar();
					_scheduledEndTWTask = ThreadPoolManager.getInstance().schedule(new ScheduleEndTWTask(), 1000);
					_scheduledRewardOnlineTask = ThreadPoolManager.getInstance().scheduleAtFixedRate(new RewardOnlineParticipants(), 60000, 60000);
				}
			}
			catch (final Exception e)
			{
				warn("", e);
			}
		}
	}
	
	private class ScheduleEndTWTask implements Runnable
	{
		public ScheduleEndTWTask()
		{
		}
		
		@Override
		public void run()
		{
			try
			{
				if (isTWInProgress())
				{
					_scheduledEndTWTask.cancel(false);
					final long timeRemaining = (_startTWDate.getTimeInMillis() + WARLENGTH) - Calendar.getInstance().getTimeInMillis();
					if (timeRemaining > 3600000)
					{
						final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.THE_TERRITORY_WAR_WILL_END_IN_S1_HOURS);
						sm.addNumber(2);
						announceToParticipants(sm, 0, 0);
						_scheduledEndTWTask = ThreadPoolManager.getInstance().schedule(new ScheduleEndTWTask(), timeRemaining - 3600000);
					}
					else if ((timeRemaining <= 3600000) && (timeRemaining > 600000))
					{
						final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.THE_TERRITORY_WAR_WILL_END_IN_S1_MINUTES);
						sm.addNumber((int) (timeRemaining / 60000));
						announceToParticipants(sm, 0, 0);
						_scheduledEndTWTask = ThreadPoolManager.getInstance().schedule(new ScheduleEndTWTask(), timeRemaining - 600000);
					}
					else if ((timeRemaining <= 600000) && (timeRemaining > 300000))
					{
						final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.THE_TERRITORY_WAR_WILL_END_IN_S1_MINUTES);
						sm.addNumber((int) (timeRemaining / 60000));
						announceToParticipants(sm, 0, 0);
						_scheduledEndTWTask = ThreadPoolManager.getInstance().schedule(new ScheduleEndTWTask(), timeRemaining - 300000);
					}
					else if ((timeRemaining <= 300000) && (timeRemaining > 10000))
					{
						final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.THE_TERRITORY_WAR_WILL_END_IN_S1_MINUTES);
						sm.addNumber((int) (timeRemaining / 60000));
						announceToParticipants(sm, 0, 0);
						_scheduledEndTWTask = ThreadPoolManager.getInstance().schedule(new ScheduleEndTWTask(), timeRemaining - 10000);
					}
					else if ((timeRemaining <= 10000) && (timeRemaining > 0))
					{
						final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_SECONDS_TO_THE_END_OF_TERRITORY_WAR);
						sm.addNumber((int) (timeRemaining / 1000));
						announceToParticipants(sm, 0, 0);
						_scheduledEndTWTask = ThreadPoolManager.getInstance().schedule(new ScheduleEndTWTask(), timeRemaining);
					}
					else
					{
						endTerritoryWar();
						ThreadPoolManager.getInstance().schedule(new closeTerritoryChannelTask(), 600000);
					}
				}
				else
				{
					_scheduledEndTWTask.cancel(false);
				}
			}
			catch (final Exception e)
			{
				warn("", e);
			}
		}
	}
	
	private class closeTerritoryChannelTask implements Runnable
	{
		public closeTerritoryChannelTask()
		{
		}
		
		@Override
		public void run()
		{
			_isTWChannelOpen = false;
			_disguisedPlayers.clear();
			updatePlayerTWStateFlags(true);
		}
	}
	
	public void announceToParticipants(GameServerPacket sm, int exp, int sp)
	{
		for (final Territory ter : _territoryList.values())
		{
			if (ter.getOwnerClan() != null)
			{
				for (final Player member : ter.getOwnerClan().getOnlineMembers(0))
				{
					member.sendPacket(sm);
					if ((exp > 0) || (sp > 0))
					{
						member.addExpAndSp(exp, sp);
					}
				}
			}
		}
		for (final List<Clan> list : _registeredClans.values())
		{
			for (final Clan c : list)
			{
				for (final Player member : c.getOnlineMembers(0))
				{
					member.sendPacket(sm);
					if ((exp > 0) || (sp > 0))
					{
						member.addExpAndSp(exp, sp);
					}
				}
			}
		}
		for (final List<Integer> list : _registeredMercenaries.values())
		{
			for (final int objId : list)
			{
				final Player player = GameObjectsStorage.getPlayer(objId);
				if ((player != null) && ((player.getClan() == null) || !checkIsRegistered(-1, player.getClan())))
				{
					player.sendPacket(sm);
					if ((exp > 0) || (sp > 0))
					{
						player.addExpAndSp(exp, sp);
					}
				}
			}
		}
	}
	
	public static class TerritoryNPCSpawn implements IIdentifiable
	{
		private final Location _location;
		protected int _npcId;
		private final int _castleId;
		private final int _type;
		private Npc _npc;
		
		public TerritoryNPCSpawn(int castle_id, Location loc, int npc_id, int type, Npc npc)
		{
			_castleId = castle_id;
			_location = loc;
			_npcId = npc_id;
			_type = type;
			_npc = npc;
		}
		
		public int getCastleId()
		{
			return _castleId;
		}
		
		@Override
		public int getId()
		{
			return _npcId;
		}
		
		public int getType()
		{
			return _type;
		}
		
		public void setNPC(Npc npc)
		{
			if (_npc != null)
			{
				_npc.deleteMe();
			}
			_npc = npc;
		}
		
		public Npc getNpc()
		{
			return _npc;
		}
		
		public Location getLocation()
		{
			return _location;
		}
	}
	
	public class Territory
	{
		private final int _territoryId;
		private final int _castleId;
		protected int _fortId;
		private Clan _ownerClan;
		private int _lordObjId;
		private final List<TerritoryNPCSpawn> _spawnList = new LinkedList<>();
		private final TerritoryNPCSpawn[] _territoryWardSpawnPlaces;
		private boolean _isInProgress = false;
		private final int[] _questDone;
		
		public Territory(int castleId)
		{
			_castleId = castleId;
			_territoryId = castleId + 80;
			_territoryWardSpawnPlaces = new TerritoryNPCSpawn[9];
			_questDone = new int[2];
		}
		
		protected void addWardSpawnPlace(Location loc)
		{
			for (int i = 0; i < _territoryWardSpawnPlaces.length; i++)
			{
				if (_territoryWardSpawnPlaces[i] == null)
				{
					_territoryWardSpawnPlaces[i] = new TerritoryNPCSpawn(_castleId, loc, 0, 4, null);
					return;
				}
			}
		}
		
		protected int getWardsAmount()
		{
			int amount = 0;
			for (final TerritoryNPCSpawn _territoryWardSpawnPlace : _territoryWardSpawnPlaces)
			{
				if ((_territoryWardSpawnPlace != null) && (_territoryWardSpawnPlace.getNpc() != null))
				{
					amount++;
				}
			}
			return amount;
		}
		
		protected TerritoryNPCSpawn getFreeWardSpawnPlace()
		{
			for (final TerritoryNPCSpawn _territoryWardSpawnPlace : _territoryWardSpawnPlaces)
			{
				if ((_territoryWardSpawnPlace != null) && (_territoryWardSpawnPlace.getNpc() == null))
				{
					return _territoryWardSpawnPlace;
				}
			}
			warn("no free Ward spawn found for territory: " + _territoryId);
			for (int i = 0; i < _territoryWardSpawnPlaces.length; i++)
			{
				if (_territoryWardSpawnPlaces[i] == null)
				{
					warn("territory ward spawn place " + i + " is null!");
				}
				else if (_territoryWardSpawnPlaces[i].getNpc() != null)
				{
					warn("territory ward spawn place " + i + " has npc name: " + _territoryWardSpawnPlaces[i].getNpc().getName(null));
				}
				else
				{
					warn("territory ward spawn place " + i + " is empty!");
				}
			}
			return null;
		}
		
		public void changeOwner(Clan clan)
		{
			int newLordObjectId;
			if (clan == null)
			{
				if (_lordObjId > 0)
				{
					newLordObjectId = 0;
				}
				else
				{
					return;
				}
			}
			else
			{
				newLordObjectId = clan.getLeaderId();
				
				final SystemMessage message = SystemMessage.getSystemMessage(SystemMessageId.CLAN_LORD_C2_WHO_LEADS_CLAN_S1_HAS_BEEN_DECLARED_THE_LORD_OF_THE_S3_TERRITORY);
				message.addPcName(clan.getLeader().getPlayerInstance());
				message.addString(clan.getName());
				message.addCastleId(getCastleId());
				for (final Player player : GameObjectsStorage.getPlayers())
				{
					player.sendPacket(message);
				}
			}
			
			_lordObjId = newLordObjectId;
			
			updateTerritoryLord(_territoryId, _lordObjId);
			
			for (final Npc npc : GameObjectsStorage.getNpcs())
			{
				if (npc != null)
				{
					if (npc.getTerritory() == this)
					{
						npc.broadcastInfo();
					}
				}
			}
		}
		
		public List<TerritoryNPCSpawn> getSpawnList()
		{
			return _spawnList;
		}
		
		protected void changeNPCsSpawn(int type, boolean isSpawn)
		{
			if ((type < 0) || (type > 3))
			{
				warn("wrong type(" + type + ") for NPCs spawn change!");
				return;
			}
			for (final TerritoryNPCSpawn twSpawn : _spawnList)
			{
				if (twSpawn.getType() != type)
				{
					continue;
				}
				if (isSpawn)
				{
					twSpawn.setNPC(spawnNPC(twSpawn.getId(), twSpawn.getLocation()));
				}
				else
				{
					final Npc npc = twSpawn.getNpc();
					if ((npc != null) && !npc.isDead())
					{
						npc.deleteMe();
					}
					twSpawn.setNPC(null);
				}
			}
		}
		
		protected void removeWard(int wardId)
		{
			for (final TerritoryNPCSpawn wardSpawn : _territoryWardSpawnPlaces)
			{
				if (wardSpawn.getId() == wardId)
				{
					wardSpawn.getNpc().deleteMe();
					wardSpawn.setNPC(null);
					wardSpawn._npcId = 0;
					return;
				}
			}
			warn("cant delete wardId: " + wardId + " for territory: " + _territoryId);
		}
		
		public int getTerritoryId()
		{
			return _territoryId;
		}
		
		public int getCastleId()
		{
			return _castleId;
		}
		
		public int getFortId()
		{
			return _fortId;
		}
		
		public Clan getOwnerClan()
		{
			return _ownerClan;
		}
		
		public int getLordObjectId()
		{
			return _lordObjId;
		}
		
		public void setOwnerClan(Clan newOwner)
		{
			_ownerClan = newOwner;
		}
		
		public TerritoryNPCSpawn[] getOwnedWard()
		{
			return _territoryWardSpawnPlaces;
		}
		
		public int[] getQuestDone()
		{
			return _questDone;
		}
		
		public List<Integer> getOwnedWardIds()
		{
			final List<Integer> ret = new LinkedList<>();
			for (final TerritoryNPCSpawn wardSpawn : _territoryWardSpawnPlaces)
			{
				if (wardSpawn.getId() > 0)
				{
					ret.add(wardSpawn.getId());
				}
			}
			return ret;
		}
		
		public boolean getIsInProgress()
		{
			return _isInProgress;
		}
		
		public void setIsInProgress(boolean val)
		{
			_isInProgress = val;
		}
	}
	
	private static class SingletonHolder
	{
		protected static final TerritoryWarManager _instance = new TerritoryWarManager();
	}
	
	@Override
	public void startSiege()
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void endSiege()
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public SiegeClan getAttackerClan(int clanId)
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public SiegeClan getAttackerClan(Clan clan)
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public List<SiegeClan> getAttackerClans()
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public List<Player> getAttackersInZone()
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public boolean checkIsAttacker(Clan clan)
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public SiegeClan getDefenderClan(int clanId)
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public SiegeClan getDefenderClan(Clan clan)
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public List<SiegeClan> getDefenderClans()
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public boolean checkIsDefender(Clan clan)
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public List<Npc> getFlag(Clan clan)
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public long getSiegeStartTime()
	{
		return 0L;
	}
	
	@Override
	public boolean giveFame()
	{
		return true;
	}
	
	@Override
	public int getFameFrequency()
	{
		return Config.CASTLE_ZONE_FAME_TASK_FREQUENCY;
	}
	
	@Override
	public int getFameAmount()
	{
		return Config.CASTLE_ZONE_FAME_AQUIRE_POINTS;
	}
	
	@Override
	public void updateSiege()
	{
	}
	
	public String getSiegeDateTime()
	{
		return _siegeDateTime;
	}
	
	public void generateNextWarTime()
	{
		final Calendar cal = Calendar.getInstance();
		final long warTime = ServerVariables.getLong("TerritoryWarDate", 0);
		if (warTime > System.currentTimeMillis())
		{
			cal.setTimeInMillis(warTime);
		}
		else
		{
			if (cal.before(Calendar.getInstance()))
			{
				cal.setTimeInMillis(System.currentTimeMillis());
			}
			
			final String siegeDate = getSiegeDateTime();
			if (siegeDate != null && !siegeDate.isEmpty())
			{
				SchedulingPattern cronTime;
				try
				{
					cronTime = new SchedulingPattern(siegeDate);
				}
				catch (final InvalidPatternException e)
				{
					return;
				}
				
				final long nextTime = cronTime.next(System.currentTimeMillis());
				cal.setTimeInMillis(nextTime);
			}
			else
			{
				cal.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
				cal.set(Calendar.HOUR_OF_DAY, 20);
				cal.set(Calendar.MINUTE, 0);
			}
			cal.set(Calendar.SECOND, 0);
			
			if (cal.before(Calendar.getInstance()))
			{
				cal.add(Calendar.DAY_OF_MONTH, 7);
			}
			
			if (Config.ALLOW_CHECK_SEVEN_SIGN_STATUS)
			{
				if (!SevenSigns.getInstance().isDateInSealValidPeriod(cal))
				{
					cal.add(Calendar.DAY_OF_MONTH, 7);
				}
			}
			ServerVariables.set("TerritoryWarDate", cal.getTimeInMillis());
		}
		setTWStartTimeInMillis(cal.getTimeInMillis());
		info("Next battle " + cal.getTime());
	}
}