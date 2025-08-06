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

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2e.commons.dbutils.DbUtils;
import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.data.dao.ClanDAO;
import l2e.gameserver.data.dao.ClanVariablesDAO;
import l2e.gameserver.data.holder.CharNameHolder;
import l2e.gameserver.data.holder.ClanHolder;
import l2e.gameserver.data.holder.CrestHolder;
import l2e.gameserver.data.parser.ClanParser;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.instancemanager.CHSiegeManager;
import l2e.gameserver.instancemanager.CastleManager;
import l2e.gameserver.instancemanager.ClanHallManager;
import l2e.gameserver.instancemanager.FortManager;
import l2e.gameserver.instancemanager.FortSiegeManager;
import l2e.gameserver.instancemanager.SiegeManager;
import l2e.gameserver.instancemanager.TerritoryWarManager.Territory;
import l2e.gameserver.instancemanager.ZoneManager;
import l2e.gameserver.instancemanager.mods.TimeSkillsTaskManager;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.player.CharacterVariable;
import l2e.gameserver.model.actor.templates.AbstractTemplate;
import l2e.gameserver.model.base.BonusType;
import l2e.gameserver.model.entity.Castle;
import l2e.gameserver.model.interfaces.IIdentifiable;
import l2e.gameserver.model.items.itemcontainer.ClanWarehouse;
import l2e.gameserver.model.items.itemcontainer.ItemContainer;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.zone.ZoneId;
import l2e.gameserver.model.zone.type.SiegeZone;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.CreatureSay;
import l2e.gameserver.network.serverpackets.ExSubPledgeSkillAdd;
import l2e.gameserver.network.serverpackets.GameServerPacket;
import l2e.gameserver.network.serverpackets.SystemMessage;
import l2e.gameserver.network.serverpackets.pledge.PledgeReceiveSubPledgeCreated;
import l2e.gameserver.network.serverpackets.pledge.PledgeShowInfoUpdate;
import l2e.gameserver.network.serverpackets.pledge.PledgeShowMemberListAll;
import l2e.gameserver.network.serverpackets.pledge.PledgeShowMemberListDeleteAll;
import l2e.gameserver.network.serverpackets.pledge.PledgeShowMemberListUpdate;
import l2e.gameserver.network.serverpackets.pledge.PledgeSkillList;
import l2e.gameserver.network.serverpackets.pledge.PledgeSkillList.SubPledgeSkill;
import l2e.gameserver.network.serverpackets.pledge.PledgeSkillListAdd;

public class Clan implements IIdentifiable
{
	private static final Logger _log = LoggerFactory.getLogger(Clan.class);
	
	private static final String INSERT_CLAN_DATA = "INSERT INTO clan_data (clan_id,clan_name,clan_level,hasCastle,blood_alliance_count,blood_oath_count,ally_id,ally_name,leader_id,crest_id,crest_large_id,ally_crest_id,new_leader_id) values (?,?,?,?,?,?,?,?,?,?,?,?,?)";
	private static final String SELECT_CLAN_DATA = "SELECT * FROM clan_data where clan_id=?";

	public static final int PENALTY_TYPE_CLAN_LEAVED = 1;
	public static final int PENALTY_TYPE_CLAN_DISMISSED = 2;
	public static final int PENALTY_TYPE_DISMISS_CLAN = 3;
	public static final int PENALTY_TYPE_DISSOLVE_ALLY = 4;
	
	public static final int CP_NOTHING = 0;
	public static final int CP_CL_JOIN_CLAN = 2;
	public static final int CP_CL_GIVE_TITLE = 4;
	public static final int CP_CL_VIEW_WAREHOUSE = 8;
	public static final int CP_CL_MANAGE_RANKS = 16;
	public static final int CP_CL_PLEDGE_WAR = 32;
	public static final int CP_CL_DISMISS = 64;
	public static final int CP_CL_REGISTER_CREST = 128;
	public static final int CP_CL_APPRENTICE = 256;
	public static final int CP_CL_TROOPS_FAME = 512;
	public static final int CP_CL_SUMMON_AIRSHIP = 1024;
	public static final int CP_CH_OPEN_DOOR = 2048;
	public static final int CP_CH_OTHER_RIGHTS = 4096;
	public static final int CP_CH_AUCTION = 8192;
	public static final int CP_CH_DISMISS = 16384;
	public static final int CP_CH_SET_FUNCTIONS = 32768;
	public static final int CP_CS_OPEN_DOOR = 65536;
	public static final int CP_CS_MANOR_ADMIN = 131072;
	public static final int CP_CS_MANAGE_SIEGE = 262144;
	public static final int CP_CS_USE_FUNCTIONS = 524288;
	public static final int CP_CS_DISMISS = 1048576;
	public static final int CP_CS_TAXES = 2097152;
	public static final int CP_CS_MERCENARIES = 4194304;
	public static final int CP_CS_SET_FUNCTIONS = 8388608;
	public static final int CP_ALL = 16777214;
	
	public static final int SUBUNIT_ACADEMY = -1;
	public static final int SUBUNIT_ROYAL1 = 100;
	public static final int SUBUNIT_ROYAL2 = 200;
	public static final int SUBUNIT_KNIGHT1 = 1001;
	public static final int SUBUNIT_KNIGHT2 = 1002;
	public static final int SUBUNIT_KNIGHT3 = 2001;
	public static final int SUBUNIT_KNIGHT4 = 2002;
	
	private String _name;
	private int _clanId;
	private ClanMember _leader;
	private final Map<Integer, ClanMember> _members = new ConcurrentHashMap<>();
	
	private String _allyName;
	private int _allyId;
	private int _level;
	private int _castleId;
	private int _fortId;
	private int _hideoutId;
	private int _hiredGuards;
	private int _crestId;
	private int _crestLargeId;
	private int _allyCrestId;
	private int _auctionBiddedAt = 0;
	private long _allyPenaltyExpiryTime;
	private int _allyPenaltyType;
	private long _charPenaltyExpiryTime;
	private long _dissolvingExpiryTime;
	private int _bloodAllianceCount;
	private int _bloodOathCount;
	
	private final ItemContainer _warehouse = new ClanWarehouse(this);
	private final Set<Integer> _atWarWith = ConcurrentHashMap.newKeySet();
	private final Set<Integer> _atWarAttackers = ConcurrentHashMap.newKeySet();
	
	private final Map<Integer, Skill> _skills = new ConcurrentHashMap<>();
	private final Map<Integer, RankPrivs> _privs = new ConcurrentHashMap<>();
	private final Map<Integer, SubPledge> _subPledges = new ConcurrentHashMap<>();
	private final Map<Integer, Skill> _subPledgeSkills = new ConcurrentHashMap<>();
	
	private int _reputationScore = 0;
	
	private String _notice;
	private boolean _noticeEnabled = false;
	private static final int MAX_NOTICE_LENGTH = 8192;
	private int _newLeaderId;
	
	private AtomicInteger _siegeKills;
	private AtomicInteger _siegeDeaths;

	private boolean _recruting = false;
	private final ArrayList<Integer> _classesNeeded = new ArrayList<>();
	private String[] _questions = new String[8];
	private final ArrayList<SinglePetition> _petitions = new ArrayList<>();
	
	private final static ClanReputationComparator REPUTATION_COMPARATOR = new ClanReputationComparator();
	private final static int REPUTATION_PLACES = 100;
	
	private final Map<Integer, List<AbstractTemplate>> _templates = new ConcurrentHashMap<>();
	private final Map<String, CharacterVariable> _variables = new ConcurrentHashMap<>();
	private final Map<BonusType, Double> _bonusList = new ConcurrentHashMap<>();

	public Clan(int clanId)
	{
		_clanId = clanId;
		initializePrivs();
		restore();
		getWarehouse().restore();
	}
	
	public Clan(int clanId, String clanName)
	{
		_clanId = clanId;
		_name = clanName;
		initializePrivs();
	}
	
	@Override
	public int getId()
	{
		return _clanId;
	}
	
	public void setClanId(int clanId)
	{
		_clanId = clanId;
	}
	
	public int getLeaderId()
	{
		return (_leader != null ? _leader.getObjectId() : 0);
	}
	
	public ClanMember getLeader()
	{
		return _leader;
	}
	
	public void setLeader(ClanMember leader)
	{
		_leader = leader;
		_members.put(leader.getObjectId(), leader);
	}
	
	public void setNewLeader(ClanMember member)
	{
		final Player newLeader = member.getPlayerInstance();
		final ClanMember exMember = getLeader();
		final Player exLeader = exMember.getPlayerInstance();
		
		if (exLeader != null)
		{
			if (exLeader.isFlying())
			{
				exLeader.dismount();
			}
			
			if (getLevel() >= 5)
			{
				SiegeManager.getInstance().removeSiegeSkills(exLeader);
			}
			exLeader.setClanPrivileges(Clan.CP_NOTHING);
			exLeader.broadcastCharInfo();
			
		}
		else
		{
			Connection con = null;
			PreparedStatement statement = null;
			try
			{
				con = DatabaseFactory.getInstance().getConnection();
				statement = con.prepareStatement("UPDATE characters SET clan_privs = ? WHERE charId = ?");
				statement.setInt(1, Clan.CP_NOTHING);
				statement.setInt(2, getLeaderId());
				statement.execute();
			}
			catch (final Exception e)
			{
				_log.warn("Couldn't update clan privs for old clan leader", e);
			}
			finally
			{
				DbUtils.closeQuietly(con, statement);
			}
		}
		
		setLeader(member);
		if (getNewLeaderId() != 0)
		{
			setNewLeaderId(0, true);
		}
		updateClanInDB();
		
		if (exLeader != null)
		{
			exLeader.setPledgeClass(ClanMember.calculatePledgeClass(exLeader));
			exLeader.broadcastCharInfo();
			exLeader.checkItemRestriction();
		}
		
		if (newLeader != null)
		{
			newLeader.setPledgeClass(ClanMember.calculatePledgeClass(newLeader));
			newLeader.setClanPrivileges(Clan.CP_ALL);
			
			if (getLevel() >= 5)
			{
				SiegeManager.getInstance().addSiegeSkills(newLeader);
			}
			newLeader.broadcastCharInfo();
		}
		else
		{
			Connection con = null;
			PreparedStatement statement = null;
			try
			{
				con = DatabaseFactory.getInstance().getConnection();
				statement = con.prepareStatement("UPDATE characters SET clan_privs = ? WHERE charId = ?");
				statement.setInt(1, Clan.CP_ALL);
				statement.setInt(2, getLeaderId());
				statement.execute();
			}
			catch (final Exception e)
			{
				_log.warn("Couldn't update clan privs for new clan leader", e);
			}
			finally
			{
				DbUtils.closeQuietly(con, statement);
			}
		}
		
		broadcastClanStatus();
		broadcastToOnlineMembers(SystemMessage.getSystemMessage(SystemMessageId.CLAN_LEADER_PRIVILEGES_HAVE_BEEN_TRANSFERRED_TO_C1).addString(member.getName()));
		_log.info("Leader of Clan: " + getName() + " changed to: " + member.getName() + " ex leader: " + exMember.getName());
	}
	
	public String getLeaderName()
	{
		if (_leader == null)
		{
			_log.warn(Clan.class.getName() + ": Clan " + getName() + " without clan leader!");
			return "";
		}
		return _leader.getName();
	}
	
	public String getName()
	{
		return _name;
	}
	
	public void setName(String name)
	{
		_name = name;
	}
	
	public void addClanMember(ClanMember member)
	{
		_members.put(member.getObjectId(), member);
	}
	
	public void addClanMember(Player player)
	{
		final ClanMember member = new ClanMember(this, player);
		addClanMember(member);
		member.setPlayerInstance(player);
		player.setClan(this);
		player.setPledgeClass(ClanMember.calculatePledgeClass(player));
		player.sendPacket(new PledgeShowMemberListUpdate(player));
		player.sendPacket(new PledgeSkillList(this));
		addSkillEffects(player);
		
		var isInSiege = false;
		for (final var siege : SiegeManager.getInstance().getSieges())
		{
			if (!siege.getIsInProgress())
			{
				continue;
			}
			
			if (siege.checkIsAttacker(this))
			{
				isInSiege = true;
				player.setSiegeState((byte) 1);
				player.setSiegeSide(siege.getCastle().getId());
			}
			
			else if (siege.checkIsDefender(this))
			{
				isInSiege = true;
				player.setSiegeState((byte) 2);
				player.setSiegeSide(siege.getCastle().getId());
			}
		}
		
		for (final var siege : FortSiegeManager.getInstance().getSieges())
		{
			if (!siege.getIsInProgress())
			{
				continue;
			}
			
			if (siege.checkIsAttacker(this))
			{
				isInSiege = true;
				player.setSiegeState((byte) 1);
				player.setSiegeSide(siege.getFort().getId());
			}
			
			else if (siege.checkIsDefender(this))
			{
				isInSiege = true;
				player.setSiegeState((byte) 2);
				player.setSiegeSide(siege.getFort().getId());
			}
		}
		
		for (final var hall : CHSiegeManager.getInstance().getConquerableHalls().values())
		{
			if (!hall.isInSiege())
			{
				continue;
			}
			
			if (hall.isRegistered(this))
			{
				isInSiege = true;
				player.setSiegeState((byte) 1);
				player.setSiegeSide(hall.getId());
				player.setIsInHideoutSiege(true);
			}
		}
		
		if (isInSiege)
		{
			final var siegeZone = ZoneManager.getInstance().getZone(player, SiegeZone.class);
			if (siegeZone != null && player.isRegisteredOnThisSiegeField(siegeZone.getSiegeObjectId()))
			{
				player.setIsInSiege(true);
				final var settings = siegeZone.getSettings();
				if (settings == null)
				{
					return;
				}
				if (settings.getSiege().giveFame() && (settings.getSiege().getFameFrequency() > 0))
				{
					player.startFameTask(settings.getSiege().getFameFrequency() * 1000, settings.getSiege().getFameAmount());
				}
			}
		}
	}
	
	public void updateClanMember(Player player)
	{
		final ClanMember member = new ClanMember(player.getClan(), player);
		if (player.isClanLeader())
		{
			setLeader(member);
		}
		addClanMember(member);
	}
	
	public ClanMember getClanMember(String name)
	{
		for (final ClanMember temp : _members.values())
		{
			if (temp.getName().equals(name))
			{
				return temp;
			}
		}
		return null;
	}
	
	public ClanMember getClanMember(int objectId)
	{
		return _members.get(objectId);
	}
	
	public void removeClanMember(int objectId, long clanJoinExpiryTime)
	{
		final ClanMember exMember = _members.remove(objectId);
		if (exMember == null)
		{
			_log.warn("Member Object ID: " + objectId + " not found in clan while trying to remove");
			return;
		}
		final int leadssubpledge = getLeaderSubPledge(objectId);
		if (leadssubpledge != 0)
		{
			getSubPledge(leadssubpledge).setLeaderId(0);
			updateSubPledgeInDB(leadssubpledge);
		}
		
		if (exMember.getApprentice() != 0)
		{
			final ClanMember apprentice = getClanMember(exMember.getApprentice());
			if (apprentice != null)
			{
				if (apprentice.getPlayerInstance() != null)
				{
					apprentice.getPlayerInstance().setSponsor(0);
				}
				else
				{
					apprentice.setApprenticeAndSponsor(0, 0);
				}
				
				apprentice.saveApprenticeAndSponsor(0, 0);
			}
		}
		
		if (exMember.getSponsor() != 0)
		{
			final ClanMember sponsor = getClanMember(exMember.getSponsor());
			if (sponsor != null)
			{
				if (sponsor.getPlayerInstance() != null)
				{
					sponsor.getPlayerInstance().setApprentice(0);
				}
				else
				{
					sponsor.setApprenticeAndSponsor(0, 0);
				}
				
				sponsor.saveApprenticeAndSponsor(0, 0);
			}
		}
		exMember.saveApprenticeAndSponsor(0, 0);
		if (Config.REMOVE_CASTLE_CIRCLETS)
		{
			CastleManager.getInstance().removeCirclet(exMember, getCastleId());
		}
		
		if (exMember.isOnline())
		{
			final Player player = exMember.getPlayerInstance();
			if (!player.isNoble())
			{
				player.setGlobalTitle("");
			}
			player.setApprentice(0);
			player.setSponsor(0);
			
			if (player.isClanLeader())
			{
				SiegeManager.getInstance().removeSiegeSkills(player);
				player.setClanCreateExpiryTime(System.currentTimeMillis() + (Config.ALT_CLAN_CREATE_DAYS * 3600000L));
			}
			removeSkillEffects(player);
			
			if (player.getClan().getCastleId() > 0)
			{
				final Castle castle = CastleManager.getInstance().getCastleById(player.getClan().getCastleId());
				if (castle != null)
				{
					final Territory territory = castle.getTerritory();
					if (territory != null)
					{
						if (territory.getLordObjectId() == player.getObjectId())
						{
							castle.getTerritory().changeOwner(null);
						}
					}
				}
				CastleManager.getInstance().getCastleByOwner(player.getClan()).removeResidentialSkills(player);
			}
			if (player.getClan().getFortId() > 0)
			{
				FortManager.getInstance().getFortByOwner(player.getClan()).removeResidentialSkills(player);
			}
			if (player.getClan().getHideoutId() > 0)
			{
				final var hall = ClanHallManager.getInstance().getAbstractHallByOwner(player.getClan());
				if (hall != null)
				{
					hall.removeResidentialSkills(player);
				}
			}
			player.sendSkillList(false);
			player.setClan(null);
			
			if (exMember.getPledgeType() != SUBUNIT_ACADEMY)
			{
				player.setClanJoinExpiryTime(clanJoinExpiryTime);
			}
			
			player.setPledgeClass(ClanMember.calculatePledgeClass(player));
			player.broadcastUserInfo(true);
			player.sendPacket(PledgeShowMemberListDeleteAll.STATIC_PACKET);
		}
		else
		{
			removeMemberInDatabase(exMember, clanJoinExpiryTime, getLeaderId() == objectId ? System.currentTimeMillis() + (Config.ALT_CLAN_CREATE_DAYS * 3600000L) : 0);
		}
	}
	
	public ClanMember[] getMembers()
	{
		return _members.values().toArray(new ClanMember[_members.size()]);
	}

	public List<ClanMember> getAllMembers()
	{
		final List<ClanMember> members = new ArrayList<>(_members.size());
		for (final ClanMember unit : getMembers())
		{
			members.add(unit);
		}
		return members;
	}
	
	public int getAverageLevel()
	{
		int size = 0;
		int level = 0;
		
		for (final ClanMember member : getMembers())
		{
			size++;
			level += member.getLevel();
		}
		return level / size;
	}
	
	public int getMembersCount()
	{
		return _members.size();
	}
	
	public int getSubPledgeMembersCount(int subpl)
	{
		int result = 0;
		for (final ClanMember temp : _members.values())
		{
			if (temp.getPledgeType() == subpl)
			{
				result++;
			}
		}
		return result;
	}
	
	public int getMaxNrOfMembers(int pledgeType)
	{
		final var tpl = ClanParser.getInstance().getClanTemplate(getLevel());
		if (tpl != null)
		{
			switch (pledgeType)
			{
				case 0 :
					return tpl.getMembersLimit();
				case SUBUNIT_ACADEMY :
					return tpl.getAcademyLimit();
				case SUBUNIT_ROYAL1 :
				case SUBUNIT_ROYAL2 :
					return tpl.getRoyalsLimit();
				case SUBUNIT_KNIGHT1 :
				case SUBUNIT_KNIGHT2 :
				case SUBUNIT_KNIGHT3 :
				case SUBUNIT_KNIGHT4 :
					return tpl.getKnightsLimit();
			}
		}
		return 0;
	}
	
	public List<Player> getOnlineMembers(int exclude)
	{
		final List<Player> onlineMembers = new ArrayList<>();
		for (final ClanMember temp : _members.values())
		{
			if ((temp != null) && temp.isOnline() && (temp.getObjectId() != exclude))
			{
				onlineMembers.add(temp.getPlayerInstance());
			}
		}
		return onlineMembers;
	}

	public Player[] getOnlineMembers()
	{
		final List<Player> list = new ArrayList<>();
		for (final ClanMember temp : _members.values())
		{
			if (temp != null && temp.isOnline())
			{
				list.add(temp.getPlayerInstance());
			}
		}
		return list.toArray(new Player[list.size()]);
	}
	
	public int getOnlineMembersCount()
	{
		int count = 0;
		for (final ClanMember temp : _members.values())
		{
			if ((temp == null) || !temp.isOnline())
			{
				continue;
			}
			count++;
		}
		return count;
	}
	
	public int getAllyId()
	{
		return _allyId;
	}
	
	public String getAllyName()
	{
		return _allyName;
	}
	
	public void setAllyCrestId(int allyCrestId)
	{
		_allyCrestId = allyCrestId;
	}
	
	public int getAllyCrestId()
	{
		return _allyCrestId;
	}
	
	public int getLevel()
	{
		return _level;
	}
	
	public void setLevel(int level)
	{
		_level = level;
	}
	
	public int getCastleId()
	{
		return _castleId;
	}
	
	public int getFortId()
	{
		return _fortId;
	}
	
	public int getHideoutId()
	{
		return _hideoutId;
	}
	
	public void setCrestId(int crestId)
	{
		_crestId = crestId;
	}
	
	public int getCrestId()
	{
		return _crestId;
	}
	
	public void setCrestLargeId(int crestLargeId)
	{
		_crestLargeId = crestLargeId;
	}
	
	public int getCrestLargeId()
	{
		return _crestLargeId;
	}
	
	public void setAllyId(int allyId)
	{
		_allyId = allyId;
	}
	
	public void setAllyName(String allyName)
	{
		_allyName = allyName;
	}
	
	public void setCastleId(int castleId)
	{
		_castleId = castleId;
	}
	
	public void setFortId(int fortId)
	{
		_fortId = fortId;
	}
	
	public void setHideoutId(int hideoutId)
	{
		_hideoutId = hideoutId;
	}
	
	public boolean isMember(int id)
	{
		return (id == 0 ? false : _members.containsKey(id));
	}
	
	public int getBloodAllianceCount()
	{
		return _bloodAllianceCount;
	}
	
	public void increaseBloodAllianceCount(int count)
	{
		_bloodAllianceCount += count;
		updateBloodAllianceCountInDB();
	}
	
	public void resetBloodAllianceCount()
	{
		_bloodAllianceCount = 0;
		updateBloodAllianceCountInDB();
	}
	
	public void updateBloodAllianceCountInDB()
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE clan_data SET blood_alliance_count=? WHERE clan_id=?");
			statement.setInt(1, getBloodAllianceCount());
			statement.setInt(2, getId());
			statement.execute();
		}
		catch (final Exception e)
		{
			_log.warn("Exception on updateBloodAllianceCountInDB(): " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
	
	public int getBloodOathCount()
	{
		return _bloodOathCount;
	}
	
	public void increaseBloodOathCount()
	{
		_bloodOathCount += Config.FS_BLOOD_OATH_COUNT;
		updateBloodOathCountInDB();
	}
	
	public void resetBloodOathCount()
	{
		_bloodOathCount = 0;
		updateBloodOathCountInDB();
	}
	
	public void updateBloodOathCountInDB()
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE clan_data SET blood_oath_count=? WHERE clan_id=?");
			statement.setInt(1, getBloodOathCount());
			statement.setInt(2, getId());
			statement.execute();
		}
		catch (final Exception e)
		{
			_log.warn("Exception on updateBloodAllianceCountInDB(): " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
	
	public void updateClanScoreInDB()
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE clan_data SET reputation_score=? WHERE clan_id=?");
			statement.setInt(1, getReputationScore());
			statement.setInt(2, getId());
			statement.execute();
		}
		catch (final Exception e)
		{
			_log.warn("Exception on updateClanScoreInDb(): " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
	
	public void updateClanNameInDB()
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE clan_data SET clan_name=? WHERE clan_id=?");
			statement.setString(1, getName());
			statement.setInt(2, getId());
			statement.execute();
		}
		catch (final Exception e)
		{
			_log.warn("Error saving clan name: " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
	
	public void updateClanInDB()
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE clan_data SET leader_id=?,ally_id=?,ally_name=?,reputation_score=?,ally_penalty_expiry_time=?,ally_penalty_type=?,char_penalty_expiry_time=?,dissolving_expiry_time=?,new_leader_id=? WHERE clan_id=?");
			statement.setInt(1, getLeaderId());
			statement.setInt(2, getAllyId());
			statement.setString(3, getAllyName());
			statement.setInt(4, getReputationScore());
			statement.setLong(5, getAllyPenaltyExpiryTime());
			statement.setInt(6, getAllyPenaltyType());
			statement.setLong(7, getCharPenaltyExpiryTime());
			statement.setLong(8, getDissolvingExpiryTime());
			statement.setInt(9, getNewLeaderId());
			statement.setInt(10, getId());
			statement.execute();
			statement.close();
			if (Config.DEBUG)
			{
				_log.info("New clan leader saved in db: " + getId());
			}
		}
		catch (final Exception e)
		{
			_log.warn("Error saving clan: " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
	
	public void store()
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(INSERT_CLAN_DATA);
			statement.setInt(1, getId());
			statement.setString(2, getName());
			statement.setInt(3, getLevel());
			statement.setInt(4, getCastleId());
			statement.setInt(5, getBloodAllianceCount());
			statement.setInt(6, getBloodOathCount());
			statement.setInt(7, getAllyId());
			statement.setString(8, getAllyName());
			statement.setInt(9, getLeaderId());
			statement.setInt(10, getCrestId());
			statement.setInt(11, getCrestLargeId());
			statement.setInt(12, getAllyCrestId());
			statement.setInt(13, getNewLeaderId());
			statement.execute();
			if (Config.DEBUG)
			{
				_log.info("New clan saved in db: " + getId());
			}
		}
		catch (final Exception e)
		{
			_log.warn("Error saving new clan: " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
	
	private void removeMemberInDatabase(ClanMember member, long clanJoinExpiryTime, long clanCreateExpiryTime)
	{
		if (member.getClan().getCastleId() > 0)
		{
			final Castle castle = CastleManager.getInstance().getCastleById(member.getClan().getCastleId());
			if (castle != null)
			{
				final Territory territory = castle.getTerritory();
				if (territory != null)
				{
					if (territory.getLordObjectId() == member.getObjectId())
					{
						castle.getTerritory().changeOwner(null);
					}
				}
			}
		}
		
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE characters SET clanid=0, title=?, clan_join_expiry_time=?, clan_create_expiry_time=?, clan_privs=0, wantspeace=0, subpledge=0, lvl_joined_academy=0, apprentice=0, sponsor=0 WHERE charId=?");
			statement.setString(1, "");
			statement.setLong(2, clanJoinExpiryTime);
			statement.setLong(3, clanCreateExpiryTime);
			statement.setInt(4, member.getObjectId());
			statement.execute();
			statement.close();
			if (Config.DEBUG)
			{
				_log.info("clan member removed in db: " + getId());
			}
			
			statement = con.prepareStatement("UPDATE characters SET apprentice=0 WHERE apprentice=?");
			statement.setInt(1, member.getObjectId());
			statement.execute();
			statement.close();
			
			statement = con.prepareStatement("UPDATE characters SET sponsor=0 WHERE sponsor=?");
			statement.setInt(1, member.getObjectId());
			statement.execute();
		}
		catch (final Exception e)
		{
			_log.warn("Error removing clan member: " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
	
	private void restore()
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet clanData = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(SELECT_CLAN_DATA);
			statement.setInt(1, getId());
			clanData = statement.executeQuery();
			if (clanData.next())
			{
				setName(clanData.getString("clan_name"));
				setLevel(clanData.getInt("clan_level"));
				setCastleId(clanData.getInt("hasCastle"));
				_bloodAllianceCount = clanData.getInt("blood_alliance_count");
				_bloodOathCount = clanData.getInt("blood_oath_count");
				setAllyId(clanData.getInt("ally_id"));
				setAllyName(clanData.getString("ally_name"));
				setAllyPenaltyExpiryTime(clanData.getLong("ally_penalty_expiry_time"), clanData.getInt("ally_penalty_type"));
				if (getAllyPenaltyExpiryTime() < System.currentTimeMillis())
				{
					setAllyPenaltyExpiryTime(0, 0);
				}
				setCharPenaltyExpiryTime(clanData.getLong("char_penalty_expiry_time"));
				if ((getCharPenaltyExpiryTime() + (Config.ALT_CLAN_JOIN_DAYS * 3600000L)) < System.currentTimeMillis())
				{
					setCharPenaltyExpiryTime(0);
				}
				setDissolvingExpiryTime(clanData.getLong("dissolving_expiry_time"));
				
				setCrestId(clanData.getInt("crest_id"));
				setCrestLargeId(clanData.getInt("crest_large_id"));
				setAllyCrestId(clanData.getInt("ally_crest_id"));
				
				setReputationScore(clanData.getInt("reputation_score"), false);
				setAuctionBiddedAt(clanData.getInt("auction_bid_at"), false);
				setNewLeaderId(clanData.getInt("new_leader_id"), false);
				
				final int leaderId = (clanData.getInt("leader_id"));
				
				statement.clearParameters();
				statement.close();
				
				statement = con.prepareStatement("SELECT char_name,level,pvpkills,classid,charId,title,power_grade,subpledge,apprentice,sponsor,sex,race FROM characters WHERE clanid=?");
				statement.setInt(1, getId());
				try (ResultSet clanMember = statement.executeQuery())
				{
					ClanMember member = null;
					while (clanMember.next())
					{
						member = new ClanMember(this, clanMember);
						if (member.getObjectId() == leaderId)
						{
							setLeader(member);
						}
						else
						{
							addClanMember(member);
						}
					}
					clanMember.close();
				}
			}
			if (Config.DEBUG && (getName() != null))
			{
				_log.info("Restored clan data for \"" + getName() + "\" from database.");
			}
			
			restoreSubPledges();
			restoreRankPrivs();
			restoreSkills();
			restoreClanRecruitment();
			restoreNotice();
		}
		catch (final Exception e)
		{
			_log.warn("Error restoring clan data: " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, clanData);
		}
	}
	
	private void restoreNotice()
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT enabled,notice FROM clan_notices WHERE clan_id=?");
			statement.setInt(1, getId());
			rset = statement.executeQuery();
			while (rset.next())
			{
				_noticeEnabled = rset.getBoolean("enabled");
				_notice = rset.getString("notice");
			}
		}
		catch (final Exception e)
		{
			_log.warn("Error restoring clan notice: " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
	}
	
	private void storeNotice(String notice, boolean enabled)
	{
		if (notice == null)
		{
			notice = "";
		}
		
		if (notice.length() > MAX_NOTICE_LENGTH)
		{
			notice = notice.substring(0, MAX_NOTICE_LENGTH - 1);
		}
		
		notice = notice.replace("<a", "");
		notice = notice.replace("</a>", "");
		notice = notice.replace("bypass", ".1.");
		
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("INSERT INTO clan_notices (clan_id,notice,enabled) values (?,?,?) ON DUPLICATE KEY UPDATE notice=?,enabled=?");
			statement.setInt(1, getId());
			statement.setString(2, notice);
			statement.setInt(3, enabled ? 1 : 0);
			statement.setString(4, notice);
			statement.setInt(5, enabled ? 1 : 0);
			statement.execute();
		}
		catch (final Exception e)
		{
			_log.warn("Error could not store clan notice: " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
		
		_notice = notice;
		_noticeEnabled = enabled;
	}
	
	public void setNoticeEnabled(boolean enabled)
	{
		storeNotice(_notice, enabled);
	}
	
	public void setNotice(String notice)
	{
		storeNotice(notice, _noticeEnabled);
	}
	
	public boolean isNoticeEnabled()
	{
		return _noticeEnabled;
	}
	
	public String getNotice()
	{
		if (_notice == null)
		{
			return "";
		}
		return _notice;
	}
	
	private void restoreSkills()
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT skill_id,skill_level,sub_pledge_id FROM clan_skills WHERE clan_id=?");
			statement.setInt(1, getId());
			rset = statement.executeQuery();
			while (rset.next())
			{
				final int id = rset.getInt("skill_id");
				final int level = rset.getInt("skill_level");
				final Skill skill = SkillsParser.getInstance().getInfo(id, level);
				final int subType = rset.getInt("sub_pledge_id");
				
				if (subType == -2)
				{
					_skills.put(skill.getId(), skill);
				}
				else if (subType == 0)
				{
					_subPledgeSkills.put(skill.getId(), skill);
				}
				else
				{
					final SubPledge subunit = _subPledges.get(subType);
					if (subunit != null)
					{
						subunit.addNewSkill(skill);
					}
					else
					{
						_log.info("Missing subpledge " + subType + " for clan " + this + ", skill skipped.");
					}
				}
			}
		}
		catch (final Exception e)
		{
			_log.warn("Error restoring clan skills: " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
	}
	
	public final Skill[] getAllSkills()
	{
		if (_skills == null)
		{
			return new Skill[0];
		}
		return _skills.values().toArray(new Skill[_skills.values().size()]);
	}
	
	public final Skill getKnownSkill(int skillId)
	{
		return _skills.get(skillId);
	}
	
	public Map<Integer, Skill> getSkills()
	{
		return _skills;
	}
	
	public Skill addSkill(Skill newSkill)
	{
		Skill oldSkill = null;
		
		if (newSkill != null)
		{
			oldSkill = _skills.put(newSkill.getId(), newSkill);
		}
		return oldSkill;
	}
	
	public Skill addNewSkill(Skill newSkill)
	{
		return addNewSkill(newSkill, -2);
	}
	
	public Skill addNewSkill(Skill newSkill, int subType)
	{
		Skill oldSkill = null;
		if (newSkill != null)
		{
			if (subType == -2)
			{
				oldSkill = _skills.put(newSkill.getId(), newSkill);
			}
			else if (subType == 0)
			{
				oldSkill = _subPledgeSkills.put(newSkill.getId(), newSkill);
			}
			else
			{
				final SubPledge subunit = getSubPledge(subType);
				if (subunit != null)
				{
					oldSkill = subunit.addNewSkill(newSkill);
				}
				else
				{
					_log.warn("Subpledge " + subType + " does not exist for clan " + this);
					return oldSkill;
				}
			}
			
			Connection con = null;
			PreparedStatement statement = null;
			try
			{
				con = DatabaseFactory.getInstance().getConnection();
				if (oldSkill != null)
				{
					statement = con.prepareStatement("UPDATE clan_skills SET skill_level=? WHERE skill_id=? AND clan_id=?");
					statement.setInt(1, newSkill.getLevel());
					statement.setInt(2, oldSkill.getId());
					statement.setInt(3, getId());
					statement.execute();
				}
				else
				{
					statement = con.prepareStatement("INSERT INTO clan_skills (clan_id,skill_id,skill_level,skill_name,sub_pledge_id) VALUES (?,?,?,?,?)");
					statement.setInt(1, getId());
					statement.setInt(2, newSkill.getId());
					statement.setInt(3, newSkill.getLevel());
					statement.setString(4, newSkill.getName(null));
					statement.setInt(5, subType);
					statement.execute();
				}
			}
			catch (final Exception e)
			{
				_log.warn("Error could not store clan skills: " + e.getMessage(), e);
			}
			finally
			{
				DbUtils.closeQuietly(con, statement);
			}
			
			final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.CLAN_SKILL_S1_ADDED);
			sm.addSkillName(newSkill.getId());
			
			for (final ClanMember temp : _members.values())
			{
				if ((temp != null) && (temp.getPlayerInstance() != null) && temp.isOnline())
				{
					if (subType == -2)
					{
						if (newSkill.getMinPledgeClass() <= temp.getPlayerInstance().getPledgeClass())
						{
							temp.getPlayerInstance().addSkill(newSkill, false);
							temp.getPlayerInstance().sendPacket(new PledgeSkillListAdd(newSkill.getId(), newSkill.getLevel()));
							temp.getPlayerInstance().sendPacket(sm);
							temp.getPlayerInstance().sendSkillList(false);
						}
					}
					else
					{
						if (temp.getPledgeType() == subType)
						{
							temp.getPlayerInstance().addSkill(newSkill, false);
							temp.getPlayerInstance().sendPacket(new ExSubPledgeSkillAdd(subType, newSkill.getId(), newSkill.getLevel()));
							temp.getPlayerInstance().sendPacket(sm);
							temp.getPlayerInstance().sendSkillList(false);
						}
					}
				}
			}
		}
		return oldSkill;
	}
	
	public void addSkillEffects()
	{
		for (final Skill skill : _skills.values())
		{
			for (final var temp : _members.values())
			{
				if ((temp != null) && temp.isOnline())
				{
					if (skill == null || (skill.isForClanLeader() && temp.getObjectId() != getLeader().getObjectId()))
					{
						continue;
					}
					
					if (skill.getMinPledgeClass() <= temp.getPlayerInstance().getPledgeClass())
					{
						temp.getPlayerInstance().addSkill(skill, false);
					}
				}
			}
		}
	}
	
	public void addSkillEffects(Player player)
	{
		if (player == null)
		{
			return;
		}
		
		for (final Skill skill : _skills.values())
		{
			if (skill == null || (skill.isForClanLeader() && player.getObjectId() != getLeader().getObjectId()))
			{
				continue;
			}
			
			if (skill.getMinPledgeClass() <= player.getPledgeClass())
			{
				player.addSkill(skill, false);
			}
		}
		
		if (player.getPledgeType() == 0)
		{
			for (final Skill skill : _subPledgeSkills.values())
			{
				if (skill == null || (skill.isForClanLeader() && player.getObjectId() != getLeader().getObjectId()))
				{
					continue;
				}
				player.addSkill(skill, false);
			}
		}
		else
		{
			final SubPledge subunit = getSubPledge(player.getPledgeType());
			if (subunit == null)
			{
				return;
			}
			for (final Skill skill : subunit.getSkills())
			{
				if (skill == null || (skill.isForClanLeader() && player.getObjectId() != getLeader().getObjectId()))
				{
					continue;
				}
				player.addSkill(skill, false);
			}
		}
		
		if (_reputationScore < 0)
		{
			skillsStatus(player, true);
		}
		TimeSkillsTaskManager.getInstance().checkPlayerSkills(player, false, true);
	}
	
	public void removeSkillEffects(Player player)
	{
		if (player == null)
		{
			return;
		}
		
		for (final Skill skill : _skills.values())
		{
			player.removeSkill(skill, false);
		}
		
		if (player.getPledgeType() == 0)
		{
			for (final Skill skill : _subPledgeSkills.values())
			{
				player.removeSkill(skill, false);
			}
		}
		else
		{
			final SubPledge subunit = getSubPledge(player.getPledgeType());
			if (subunit == null)
			{
				return;
			}
			for (final Skill skill : subunit.getSkills())
			{
				player.removeSkill(skill, false);
			}
		}
	}
	
	public void skillsStatus(Player player, boolean disable)
	{
		if (player == null)
		{
			return;
		}
		
		for (final Skill skill : _skills.values())
		{
			if (disable)
			{
				player.disableSkill(skill, -1);
			}
			else
			{
				player.enableSkill(skill);
			}
		}
		
		if (player.getPledgeType() == 0)
		{
			for (final Skill skill : _subPledgeSkills.values())
			{
				if (disable)
				{
					player.disableSkill(skill, -1);
				}
				else
				{
					player.enableSkill(skill);
				}
			}
		}
		else
		{
			final SubPledge subunit = getSubPledge(player.getPledgeType());
			if (subunit != null)
			{
				for (final Skill skill : subunit.getSkills())
				{
					if (disable)
					{
						player.disableSkill(skill, -1);
					}
					else
					{
						player.enableSkill(skill);
					}
				}
			}
		}
	}
	
	public void broadcastToOnlineAllyMembers(GameServerPacket packet)
	{
		for (final Clan clan : ClanHolder.getInstance().getClanAllies(getAllyId()))
		{
			clan.broadcastToOnlineMembers(packet);
		}
	}
	
	public void broadcastToOnlineMembers(GameServerPacket packet)
	{
		for (final ClanMember member : _members.values())
		{
			if ((member != null) && member.isOnline())
			{
				member.getPlayerInstance().sendPacket(packet);
			}
		}
	}
	
	public void broadcastCSToOnlineMembers(CreatureSay packet, Player broadcaster)
	{
		for (final ClanMember member : _members.values())
		{
			if ((member != null) && member.isOnline() && !member.getPlayerInstance().getBlockList().isBlocked(broadcaster))
			{
				member.getPlayerInstance().sendPacket(packet);
			}
		}
	}
	
	public void broadcastToOtherOnlineMembers(GameServerPacket packet, Player player)
	{
		for (final ClanMember member : _members.values())
		{
			if ((member != null) && member.isOnline() && (member.getPlayerInstance() != player))
			{
				member.getPlayerInstance().sendPacket(packet);
			}
		}
	}
	
	@Override
	public String toString()
	{
		return getName() + "[" + getId() + "]";
	}
	
	public ItemContainer getWarehouse()
	{
		return _warehouse;
	}
	
	public boolean isAtWarWith(Integer id)
	{
		return _atWarWith.contains(id);
	}
	
	public boolean isAtWarWith(Clan clan)
	{
		if (clan == null)
		{
			return false;
		}
		return _atWarWith.contains(clan.getId());
	}
	
	public boolean isAtWarAttacker(Integer id)
	{
		if ((_atWarAttackers != null) && !_atWarAttackers.isEmpty())
		{
			return _atWarAttackers.contains(id);
		}
		return false;
	}
	
	public void setEnemyClan(Clan clan)
	{
		_atWarWith.add(clan.getId());
	}
	
	public List<Clan> getEnemyClans()
	{
		final List<Clan> _clanList = new ArrayList<>();
		for (final Integer i : _atWarWith)
		{
			_clanList.add(ClanHolder.getInstance().getClan(i));
		}
		return _clanList;
	}
	
	public void setEnemyClan(Integer clan)
	{
		_atWarWith.add(clan);
	}
	
	public void setAttackerClan(Clan clan)
	{
		_atWarAttackers.add(clan.getId());
	}
	
	public void setAttackerClan(Integer clan)
	{
		_atWarAttackers.add(clan);
	}
	
	public void deleteEnemyClan(Clan clan)
	{
		final Integer id = clan.getId();
		_atWarWith.remove(id);
	}
	
	public void deleteAttackerClan(Clan clan)
	{
		final Integer id = clan.getId();
		_atWarAttackers.remove(id);
	}
	
	public int getHiredGuards()
	{
		return _hiredGuards;
	}
	
	public void incrementHiredGuards()
	{
		_hiredGuards++;
	}
	
	public boolean isAtWar()
	{
		return (_atWarWith != null) && !_atWarWith.isEmpty();
	}
	
	public Set<Integer> getWarList()
	{
		return _atWarWith;
	}
	
	public Set<Integer> getAttackerList()
	{
		return _atWarAttackers;
	}
	
	public void broadcastClanStatus()
	{
		for (final Player member : getOnlineMembers(0))
		{
			member.sendPacket(PledgeShowMemberListDeleteAll.STATIC_PACKET);
			member.sendPacket(new PledgeShowMemberListAll(this, member));
		}
	}
	
	public static class SubPledge
	{
		public final int _id;
		public String _subPledgeName;
		private int _leaderId;
		public final Map<Integer, Skill> _subPledgeSkills = new HashMap<>();
		
		public SubPledge(int id, String name, int leaderId)
		{
			_id = id;
			_subPledgeName = name;
			_leaderId = leaderId;
		}
		
		public int getId()
		{
			return _id;
		}
		
		public String getName()
		{
			return _subPledgeName;
		}
		
		public void setName(String name)
		{
			_subPledgeName = name;
		}
		
		public int getLeaderId()
		{
			return _leaderId;
		}
		
		public void setLeaderId(int leaderId)
		{
			_leaderId = leaderId;
		}
		
		public Skill addNewSkill(Skill skill)
		{
			return _subPledgeSkills.put(skill.getId(), skill);
		}
		
		public Collection<Skill> getSkills()
		{
			return _subPledgeSkills.values();
		}
	}
	
	public static class RankPrivs
	{
		private final int _rankId;
		private int _party;
		private int _rankPrivs;
		
		public RankPrivs(int rank, int party, int privs)
		{
			_rankId = rank;
			_party = party;
			_rankPrivs = privs;
		}
		
		public int getRank()
		{
			return _rankId;
		}
		
		public int getParty()
		{
			return _party;
		}
		
		public int getPrivs()
		{
			return _rankPrivs;
		}
		
		public void setParty(int party)
		{
			_party = party;
		}
		
		public void setPrivs(int privs)
		{
			_rankPrivs = privs;
		}
	}
	
	private void restoreSubPledges()
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT sub_pledge_id,name,leader_id FROM clan_subpledges WHERE clan_id=?");
			statement.setInt(1, getId());
			rset = statement.executeQuery();
			while (rset.next())
			{
				final int id = rset.getInt("sub_pledge_id");
				final String name = rset.getString("name");
				final int leaderId = rset.getInt("leader_id");
				final SubPledge pledge = new SubPledge(id, name, leaderId);
				_subPledges.put(id, pledge);
			}
		}
		catch (final Exception e)
		{
			_log.warn("Could not restore clan sub-units: " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
	}
	
	public final SubPledge getSubPledge(int pledgeType)
	{
		if (_subPledges == null)
		{
			return null;
		}
		
		return _subPledges.get(pledgeType);
	}
	
	public final SubPledge getSubPledge(String pledgeName)
	{
		if (_subPledges == null)
		{
			return null;
		}
		
		for (final SubPledge sp : _subPledges.values())
		{
			if (sp.getName().equalsIgnoreCase(pledgeName))
			{
				return sp;
			}
		}
		return null;
	}
	
	public final SubPledge[] getAllSubPledges()
	{
		if (_subPledges == null)
		{
			return new SubPledge[0];
		}
		
		return _subPledges.values().toArray(new SubPledge[_subPledges.values().size()]);
	}
	
	public SubPledge createSubPledge(Player player, int pledgeType, int leaderId, String subPledgeName)
	{
		SubPledge subPledge = null;
		pledgeType = getAvailablePledgeTypes(pledgeType);
		if (pledgeType == 0)
		{
			if (pledgeType == Clan.SUBUNIT_ACADEMY)
			{
				player.sendPacket(SystemMessageId.CLAN_HAS_ALREADY_ESTABLISHED_A_CLAN_ACADEMY);
			}
			else
			{
				player.sendMessage("You can't create any more sub-units of this type");
			}
			return null;
		}
		if (_leader.getObjectId() == leaderId)
		{
			player.sendMessage("Leader is not correct");
			return null;
		}
		
		if ((pledgeType != -1) && (((getReputationScore() < Config.ROYAL_GUARD_COST) && (pledgeType < Clan.SUBUNIT_KNIGHT1)) || ((getReputationScore() < Config.KNIGHT_UNIT_COST) && (pledgeType > Clan.SUBUNIT_ROYAL2))))
		{
			player.sendPacket(SystemMessageId.THE_CLAN_REPUTATION_SCORE_IS_TOO_LOW);
			return null;
		}
		
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("INSERT INTO clan_subpledges (clan_id,sub_pledge_id,name,leader_id) values (?,?,?,?)");
			statement.setInt(1, getId());
			statement.setInt(2, pledgeType);
			statement.setString(3, subPledgeName);
			if (pledgeType != -1)
			{
				statement.setInt(4, leaderId);
			}
			else
			{
				statement.setInt(4, 0);
			}
			statement.execute();
			
			subPledge = new SubPledge(pledgeType, subPledgeName, leaderId);
			_subPledges.put(pledgeType, subPledge);
			
			if (pledgeType != -1)
			{
				if (pledgeType < Clan.SUBUNIT_KNIGHT1)
				{
					setReputationScore(getReputationScore() - Config.ROYAL_GUARD_COST, true);
				}
				else
				{
					setReputationScore(getReputationScore() - Config.KNIGHT_UNIT_COST, true);
				}
			}
			
			if (Config.DEBUG)
			{
				_log.info("New sub_clan saved in db: " + getId() + "; " + pledgeType);
			}
		}
		catch (final Exception e)
		{
			_log.warn("Error saving sub clan data: " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
		broadcastToOnlineMembers(new PledgeShowInfoUpdate(_leader.getClan()));
		broadcastToOnlineMembers(new PledgeReceiveSubPledgeCreated(subPledge, _leader.getClan()));
		return subPledge;
	}
	
	public int getAvailablePledgeTypes(int pledgeType)
	{
		if (_subPledges.get(pledgeType) != null)
		{
			switch (pledgeType)
			{
				case SUBUNIT_ACADEMY :
					return 0;
				case SUBUNIT_ROYAL1 :
					pledgeType = getAvailablePledgeTypes(SUBUNIT_ROYAL2);
					break;
				case SUBUNIT_ROYAL2 :
					return 0;
				case SUBUNIT_KNIGHT1 :
					pledgeType = getAvailablePledgeTypes(SUBUNIT_KNIGHT2);
					break;
				case SUBUNIT_KNIGHT2 :
					pledgeType = getAvailablePledgeTypes(SUBUNIT_KNIGHT3);
					break;
				case SUBUNIT_KNIGHT3 :
					pledgeType = getAvailablePledgeTypes(SUBUNIT_KNIGHT4);
					break;
				case SUBUNIT_KNIGHT4 :
					return 0;
			}
		}
		return pledgeType;
	}
	
	public void updateSubPledgeInDB(int pledgeType)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE clan_subpledges SET leader_id=?, name=? WHERE clan_id=? AND sub_pledge_id=?");
			statement.setInt(1, getSubPledge(pledgeType).getLeaderId());
			statement.setString(2, getSubPledge(pledgeType).getName());
			statement.setInt(3, getId());
			statement.setInt(4, pledgeType);
			statement.execute();
			if (Config.DEBUG)
			{
				_log.info("Subpledge updated in db: " + getId());
			}
		}
		catch (final Exception e)
		{
			_log.warn("Error updating subpledge: " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
	
	private void restoreRankPrivs()
	{
		ClanDAO.getInstance().getPrivileges(getId()).forEach((rank, privileges) -> _privs.get(rank).setPrivs(privileges));
	}
	
	public void initializePrivs()
	{
		RankPrivs privs;
		for (int i = 1; i < 10; i++)
		{
			privs = new RankPrivs(i, 0, CP_NOTHING);
			_privs.put(i, privs);
		}
	}
	
	public int getRankPrivs(int rank)
	{
		if (_privs.get(rank) != null)
		{
			return _privs.get(rank).getPrivs();
		}
		return CP_NOTHING;
	}
	
	public int countMembersByRank(int rank)
	{
		int ret = 0;
		for (final ClanMember m : getAllMembers())
		{
			if (m.getPowerGrade() == rank)
			{
				ret++;
			}
		}
		return ret;
	}
	
	public void setRankPrivs(int rank, int privs)
	{
		if (_privs.get(rank) != null)
		{
			_privs.get(rank).setPrivs(privs);
			ClanDAO.getInstance().storePrivileges(getId(), rank, privs);
			for (final ClanMember cm : getMembers())
			{
				if (cm.isOnline())
				{
					if (cm.getPowerGrade() == rank)
					{
						if (cm.getPlayerInstance() != null)
						{
							cm.getPlayerInstance().setClanPrivileges(privs);
							cm.getPlayerInstance().sendUserInfo();
						}
					}
				}
			}
			broadcastClanStatus();
		}
		else
		{
			_privs.put(rank, new RankPrivs(rank, countMembersByRank(rank), privs));
			ClanDAO.getInstance().storePrivileges(getId(), rank, privs);
		}
	}
	
	public final RankPrivs[] getAllRankPrivs()
	{
		if (_privs == null)
		{
			return new RankPrivs[0];
		}
		return _privs.values().toArray(new RankPrivs[_privs.values().size()]);
	}
	
	public int getLeaderSubPledge(int leaderId)
	{
		int id = 0;
		for (final SubPledge sp : _subPledges.values())
		{
			if (sp.getLeaderId() == 0)
			{
				continue;
			}
			if (sp.getLeaderId() == leaderId)
			{
				id = sp.getId();
			}
		}
		return id;
	}
	
	public synchronized void addReputationScore(int value, boolean save)
	{
		setReputationScore(getReputationScore() + value, save);
	}
	
	public synchronized void takeReputationScore(int value, boolean save)
	{
		setReputationScore(getReputationScore() - value, save);
	}
	
	private void setReputationScore(int value, boolean save)
	{
		if ((_reputationScore >= 0) && (value < 0))
		{
			broadcastToOnlineMembers(SystemMessage.getSystemMessage(SystemMessageId.REPUTATION_POINTS_0_OR_LOWER_CLAN_SKILLS_DEACTIVATED));
			for (final ClanMember member : _members.values())
			{
				if (member.isOnline() && (member.getPlayerInstance() != null))
				{
					skillsStatus(member.getPlayerInstance(), true);
				}
			}
		}
		else if ((_reputationScore < 0) && (value >= 0))
		{
			broadcastToOnlineMembers(SystemMessage.getSystemMessage(SystemMessageId.CLAN_SKILLS_WILL_BE_ACTIVATED_SINCE_REPUTATION_IS_0_OR_HIGHER));
			for (final ClanMember member : _members.values())
			{
				if (member.isOnline() && (member.getPlayerInstance() != null))
				{
					skillsStatus(member.getPlayerInstance(), false);
				}
			}
		}
		
		_reputationScore = value;
		if (_reputationScore > 100000000)
		{
			_reputationScore = 100000000;
		}
		if (_reputationScore < -100000000)
		{
			_reputationScore = -100000000;
		}
		broadcastToOnlineMembers(new PledgeShowInfoUpdate(this));
		if (save)
		{
			updateClanScoreInDB();
		}
	}
	
	public int getReputationScore()
	{
		return _reputationScore;
	}
	
	public int getRank()
	{
		final Clan[] clans = ClanHolder.getInstance().getClans();
		Arrays.sort(clans, REPUTATION_COMPARATOR);

		final int place = 1;
		for (int i = 0; i < clans.length; i++)
		{
			if (i == REPUTATION_PLACES)
			{
				return 0;
			}

			final Clan clan = clans[i];
			if (clan == this)
			{
				return place + i;
			}
		}
		return 0;
	}
	
	public int getAuctionBiddedAt()
	{
		return _auctionBiddedAt;
	}
	
	public void setAuctionBiddedAt(int id, boolean storeInDb)
	{
		_auctionBiddedAt = id;
		
		if (storeInDb)
		{
			Connection con = null;
			PreparedStatement statement = null;
			try
			{
				con = DatabaseFactory.getInstance().getConnection();
				statement = con.prepareStatement("UPDATE clan_data SET auction_bid_at=? WHERE clan_id=?");
				statement.setInt(1, id);
				statement.setInt(2, getId());
				statement.execute();
			}
			catch (final Exception e)
			{
				_log.warn("Could not store auction for clan: " + e.getMessage(), e);
			}
			finally
			{
				DbUtils.closeQuietly(con, statement);
			}
		}
	}
	
	public boolean checkClanJoinCondition(Player activeChar, Player target, int pledgeType)
	{
		if (activeChar == null)
		{
			return false;
		}
		if ((activeChar.getClanPrivileges() & Clan.CP_CL_JOIN_CLAN) != Clan.CP_CL_JOIN_CLAN)
		{
			activeChar.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
			return false;
		}
		if (target == null)
		{
			activeChar.sendPacket(SystemMessageId.YOU_HAVE_INVITED_THE_WRONG_TARGET);
			return false;
		}
		if (activeChar.getObjectId() == target.getObjectId())
		{
			activeChar.sendPacket(SystemMessageId.CANNOT_INVITE_YOURSELF);
			return false;
		}
		if (getCharPenaltyExpiryTime() > System.currentTimeMillis())
		{
			activeChar.sendPacket(SystemMessageId.YOU_MUST_WAIT_BEFORE_ACCEPTING_A_NEW_MEMBER);
			return false;
		}
		if (target.getClanId() != 0)
		{
			final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_WORKING_WITH_ANOTHER_CLAN);
			sm.addString(target.getName(null));
			activeChar.sendPacket(sm);
			return false;
		}
		if (target.getClanJoinExpiryTime() > System.currentTimeMillis())
		{
			final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_MUST_WAIT_BEFORE_JOINING_ANOTHER_CLAN);
			sm.addString(target.getName(null));
			activeChar.sendPacket(sm);
			return false;
		}
		if (((target.getLevel() > 40) || (target.getClassId().level() >= 2)) && (pledgeType == -1))
		{
			final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_DOESNOT_MEET_REQUIREMENTS_TO_JOIN_ACADEMY);
			sm.addString(target.getName(null));
			activeChar.sendPacket(sm);
			activeChar.sendPacket(SystemMessageId.ACADEMY_REQUIREMENTS);
			return false;
		}
		if (getSubPledgeMembersCount(pledgeType) >= getMaxNrOfMembers(pledgeType))
		{
			if (pledgeType == 0)
			{
				final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_CLAN_IS_FULL);
				sm.addString(getName());
				activeChar.sendPacket(sm);
			}
			else
			{
				activeChar.sendPacket(SystemMessageId.SUBCLAN_IS_FULL);
			}
			return false;
		}
		return true;
	}
	
	public boolean checkAllyJoinCondition(Player activeChar, Player target)
	{
		if (activeChar == null)
		{
			return false;
		}
		if ((activeChar.getAllyId() == 0) || !activeChar.isClanLeader() || (activeChar.getClanId() != activeChar.getAllyId()))
		{
			activeChar.sendPacket(SystemMessageId.FEATURE_ONLY_FOR_ALLIANCE_LEADER);
			return false;
		}
		final Clan leaderClan = activeChar.getClan();
		if (leaderClan.getAllyPenaltyExpiryTime() > System.currentTimeMillis())
		{
			if (leaderClan.getAllyPenaltyType() == PENALTY_TYPE_DISMISS_CLAN)
			{
				activeChar.sendPacket(SystemMessageId.CANT_INVITE_CLAN_WITHIN_1_DAY);
				return false;
			}
		}
		if (target == null)
		{
			activeChar.sendPacket(SystemMessageId.YOU_HAVE_INVITED_THE_WRONG_TARGET);
			return false;
		}
		if (activeChar.getObjectId() == target.getObjectId())
		{
			activeChar.sendPacket(SystemMessageId.CANNOT_INVITE_YOURSELF);
			return false;
		}
		if (target.getClan() == null)
		{
			activeChar.sendPacket(SystemMessageId.TARGET_MUST_BE_IN_CLAN);
			return false;
		}
		if (!target.isClanLeader())
		{
			SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_IS_NOT_A_CLAN_LEADER);
			sm.addString(target.getName(null));
			activeChar.sendPacket(sm);
			sm = null;
			return false;
		}
		final Clan targetClan = target.getClan();
		if (target.getAllyId() != 0)
		{
			SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_CLAN_ALREADY_MEMBER_OF_S2_ALLIANCE);
			sm.addString(targetClan.getName());
			sm.addString(targetClan.getAllyName());
			activeChar.sendPacket(sm);
			sm = null;
			return false;
		}
		if (targetClan.getAllyPenaltyExpiryTime() > System.currentTimeMillis())
		{
			if (targetClan.getAllyPenaltyType() == PENALTY_TYPE_CLAN_LEAVED)
			{
				SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_CANT_ENTER_ALLIANCE_WITHIN_1_DAY);
				sm.addString(target.getClan().getName());
				sm.addString(target.getClan().getAllyName());
				activeChar.sendPacket(sm);
				sm = null;
				return false;
			}
			if (targetClan.getAllyPenaltyType() == PENALTY_TYPE_CLAN_DISMISSED)
			{
				activeChar.sendPacket(SystemMessageId.CANT_ENTER_ALLIANCE_WITHIN_1_DAY);
				return false;
			}
		}
		if (activeChar.isInsideZone(ZoneId.SIEGE) && target.isInsideZone(ZoneId.SIEGE))
		{
			activeChar.sendPacket(SystemMessageId.OPPOSING_CLAN_IS_PARTICIPATING_IN_SIEGE);
			return false;
		}
		if (leaderClan.isAtWarWith(targetClan.getId()))
		{
			activeChar.sendPacket(SystemMessageId.MAY_NOT_ALLY_CLAN_BATTLE);
			return false;
		}
		
		if (ClanHolder.getInstance().getClanAllies(activeChar.getAllyId()).size() >= Config.ALT_MAX_NUM_OF_CLANS_IN_ALLY)
		{
			activeChar.sendPacket(SystemMessageId.YOU_HAVE_EXCEEDED_THE_LIMIT);
			return false;
		}
		
		return true;
	}
	
	public long getAllyPenaltyExpiryTime()
	{
		return _allyPenaltyExpiryTime;
	}
	
	public int getAllyPenaltyType()
	{
		return _allyPenaltyType;
	}
	
	public void setAllyPenaltyExpiryTime(long expiryTime, int penaltyType)
	{
		_allyPenaltyExpiryTime = expiryTime;
		_allyPenaltyType = penaltyType;
	}
	
	public long getCharPenaltyExpiryTime()
	{
		return _charPenaltyExpiryTime;
	}
	
	public void setCharPenaltyExpiryTime(long time)
	{
		_charPenaltyExpiryTime = time;
	}
	
	public long getDissolvingExpiryTime()
	{
		return _dissolvingExpiryTime;
	}
	
	public void setDissolvingExpiryTime(long time)
	{
		_dissolvingExpiryTime = time;
	}
	
	public void createAlly(Player player, String allyName)
	{
		if (null == player)
		{
			return;
		}
		
		if (Config.DEBUG)
		{
			_log.info(player.getObjectId() + "(" + player.getName(null) + ") requested ally creation from ");
		}
		
		if (!player.isClanLeader())
		{
			player.sendPacket(SystemMessageId.ONLY_CLAN_LEADER_CREATE_ALLIANCE);
			return;
		}
		if (getAllyId() != 0)
		{
			player.sendPacket(SystemMessageId.ALREADY_JOINED_ALLIANCE);
			return;
		}
		if (getLevel() < 5)
		{
			player.sendPacket(SystemMessageId.TO_CREATE_AN_ALLY_YOU_CLAN_MUST_BE_LEVEL_5_OR_HIGHER);
			return;
		}
		if (getAllyPenaltyExpiryTime() > System.currentTimeMillis())
		{
			if (getAllyPenaltyType() == Clan.PENALTY_TYPE_DISSOLVE_ALLY)
			{
				player.sendPacket(SystemMessageId.CANT_CREATE_ALLIANCE_10_DAYS_DISOLUTION);
				return;
			}
		}
		if (getDissolvingExpiryTime() > System.currentTimeMillis())
		{
			player.sendPacket(SystemMessageId.YOU_MAY_NOT_CREATE_ALLY_WHILE_DISSOLVING);
			return;
		}
		if (!Util.isAlphaNumeric(allyName))
		{
			player.sendPacket(SystemMessageId.INCORRECT_ALLIANCE_NAME);
			return;
		}
		if ((allyName.length() > 16) || (allyName.length() < 2))
		{
			player.sendPacket(SystemMessageId.INCORRECT_ALLIANCE_NAME_LENGTH);
			return;
		}
		if (ClanHolder.getInstance().isAllyExists(allyName))
		{
			player.sendPacket(SystemMessageId.ALLIANCE_ALREADY_EXISTS);
			return;
		}
		
		setAllyId(getId());
		setAllyName(allyName.trim());
		setAllyPenaltyExpiryTime(0, 0);
		updateClanInDB();
		
		player.sendUserInfo();
		player.sendMessage("Alliance " + allyName + " has been created.");
	}
	
	public void dissolveAlly(Player player)
	{
		if (getAllyId() == 0)
		{
			player.sendPacket(SystemMessageId.NO_CURRENT_ALLIANCES);
			return;
		}
		if (!player.isClanLeader() || (getId() != getAllyId()))
		{
			player.sendPacket(SystemMessageId.FEATURE_ONLY_FOR_ALLIANCE_LEADER);
			return;
		}
		if (player.isInsideZone(ZoneId.SIEGE))
		{
			player.sendPacket(SystemMessageId.CANNOT_DISSOLVE_ALLY_WHILE_IN_SIEGE);
			return;
		}
		
		broadcastToOnlineAllyMembers(SystemMessage.getSystemMessage(SystemMessageId.ALLIANCE_DISOLVED));
		
		final long currentTime = System.currentTimeMillis();
		for (final Clan clan : ClanHolder.getInstance().getClanAllies(getAllyId()))
		{
			if (clan.getId() != getId())
			{
				clan.setAllyId(0);
				clan.setAllyName(null);
				clan.setAllyPenaltyExpiryTime(0, 0);
				clan.updateClanInDB();
			}
		}
		
		setAllyId(0);
		setAllyName(null);
		changeAllyCrest(0, false);
		setAllyPenaltyExpiryTime(currentTime + (Config.ALT_CREATE_ALLY_DAYS_WHEN_DISSOLVED * 3600000L), Clan.PENALTY_TYPE_DISSOLVE_ALLY);
		updateClanInDB();
		
		player.deathPenalty(null, false, false, false);
	}
	
	public boolean levelUpClan(Player player)
	{
		final var nextLevel = getLevel() + 1;
		if (!ClanParser.getInstance().canIncreaseClanLevel(player, nextLevel))
		{
			player.sendPacket(SystemMessageId.FAILED_TO_INCREASE_CLAN_LEVEL);
			return false;
		}
		
		if (ClanParser.getInstance().isIncreaseClanLevel(player, nextLevel))
		{
			changeLevel(nextLevel, true);
			return true;
		}
		player.sendPacket(SystemMessageId.FAILED_TO_INCREASE_CLAN_LEVEL);
		return false;
	}
	
	public boolean changeLevel(int level, boolean sendMsg)
	{
		if (!ClanParser.getInstance().hasClanLevel(level))
		{
			return false;
		}
		
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE clan_data SET clan_level = ? WHERE clan_id = ?");
			statement.setInt(1, level);
			statement.setInt(2, getId());
			statement.execute();
		}
		catch (final Exception e)
		{
			_log.warn("could not increase clan level:" + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
		
		setLevel(level);
		
		if (getLeader().isOnline())
		{
			final Player leader = getLeader().getPlayerInstance();
			if (4 < level)
			{
				SiegeManager.getInstance().addSiegeSkills(leader);
				leader.sendPacket(SystemMessageId.CLAN_CAN_ACCUMULATE_CLAN_REPUTATION_POINTS);
			}
			else if (level < 5)
			{
				SiegeManager.getInstance().removeSiegeSkills(leader);
			}
		}
		
		if (level > 1)
		{
			for (final ClanMember member : getMembers())
			{
				if (member.isOnline())
				{
					if (member.getPlayerInstance() != null)
					{
						member.getPlayerInstance().setPledgeClass(ClanMember.calculatePledgeClass(member.getPlayerInstance()));
						member.getPlayerInstance().sendUserInfo();
					}
				}
			}
		}
		
		if (sendMsg)
		{
			broadcastToOnlineMembers(SystemMessage.getSystemMessage(SystemMessageId.CLAN_LEVEL_INCREASED));
		}
		broadcastToOnlineMembers(new PledgeShowInfoUpdate(this));
		return true;
	}
	
	public void changeClanCrest(int crestId)
	{
		if (getCrestId() != 0)
		{
			CrestHolder.getInstance().removeCrest(getCrestId());
		}
		
		setCrestId(crestId);
		
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE clan_data SET crest_id = ? WHERE clan_id = ?");
			statement.setInt(1, crestId);
			statement.setInt(2, getId());
			statement.executeUpdate();
		}
		catch (final SQLException e)
		{
			_log.warn("Could not update crest for clan " + getName() + " [" + getId() + "] : " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
		
		for (final Player member : getOnlineMembers(0))
		{
			member.broadcastUserInfo(true);
		}
	}
	
	public void changeAllyCrest(int crestId, boolean onlyThisClan)
	{
		String sqlStatement = "UPDATE clan_data SET ally_crest_id = ? WHERE clan_id = ?";
		int allyId = getId();
		if (!onlyThisClan)
		{
			if (getAllyCrestId() != 0)
			{
				CrestHolder.getInstance().removeCrest(getAllyCrestId());
			}
			sqlStatement = "UPDATE clan_data SET ally_crest_id = ? WHERE ally_id = ?";
			allyId = getAllyId();
		}
		
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(sqlStatement);
			statement.setInt(1, crestId);
			statement.setInt(2, allyId);
			statement.executeUpdate();
		}
		catch (final SQLException e)
		{
			_log.warn("Could not update ally crest for ally/clan id " + allyId + " : " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
		
		if (onlyThisClan)
		{
			setAllyCrestId(crestId);
			for (final Player member : getOnlineMembers(0))
			{
				member.broadcastUserInfo(true);
			}
		}
		else
		{
			for (final Clan clan : ClanHolder.getInstance().getClanAllies(getAllyId()))
			{
				clan.setAllyCrestId(crestId);
				for (final Player member : clan.getOnlineMembers(0))
				{
					member.broadcastUserInfo(true);
				}
			}
		}
	}
	
	public void changeLargeCrest(int crestId)
	{
		if (getCrestLargeId() != 0)
		{
			CrestHolder.getInstance().removeCrest(getCrestLargeId());
		}
		
		setCrestLargeId(crestId);
		
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE clan_data SET crest_large_id = ? WHERE clan_id = ?");
			statement.setInt(1, crestId);
			statement.setInt(2, getId());
			statement.executeUpdate();
		}
		catch (final SQLException e)
		{
			_log.warn("Could not update large crest for clan " + getName() + " [" + getId() + "] : " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
		
		for (final Player member : getOnlineMembers(0))
		{
			member.broadcastUserInfo(true);
		}
	}
	
	public boolean isLearnableSubSkill(int skillId, int skillLevel)
	{
		Skill current = _subPledgeSkills.get(skillId);
		
		if ((current != null) && ((current.getLevel() + 1) == skillLevel))
		{
			return true;
		}
		
		if ((current == null) && (skillLevel == 1))
		{
			return true;
		}
		
		for (final SubPledge subunit : _subPledges.values())
		{
			if (subunit._id == -1)
			{
				continue;
			}
			current = subunit._subPledgeSkills.get(skillId);
			
			if ((current != null) && ((current.getLevel() + 1) == skillLevel))
			{
				return true;
			}
			
			if ((current == null) && (skillLevel == 1))
			{
				return true;
			}
		}
		return false;
	}
	
	public boolean isLearnableSubPledgeSkill(Skill skill, int subType)
	{
		if (subType == -1)
		{
			return false;
		}
		
		final int id = skill.getId();
		Skill current;
		if (subType == 0)
		{
			current = _subPledgeSkills.get(id);
		}
		else
		{
			current = _subPledges.get(subType)._subPledgeSkills.get(id);
		}
		
		if ((current != null) && ((current.getLevel() + 1) == skill.getLevel()))
		{
			return true;
		}
		
		if ((current == null) && (skill.getLevel() == 1))
		{
			return true;
		}
		
		return false;
	}
	
	public SubPledgeSkill[] getAllSubSkills()
	{
		final List<SubPledgeSkill> list = new LinkedList<>();
		for (final Skill skill : _subPledgeSkills.values())
		{
			list.add(new SubPledgeSkill(0, skill.getId(), skill.getLevel()));
		}
		for (final SubPledge subunit : _subPledges.values())
		{
			for (final Skill skill : subunit.getSkills())
			{
				list.add(new SubPledgeSkill(subunit._id, skill.getId(), skill.getLevel()));
			}
		}
		final SubPledgeSkill[] result = list.toArray(new SubPledgeSkill[list.size()]);
		return result;
	}
	
	public void setNewLeaderId(int objectId, boolean storeInDb)
	{
		_newLeaderId = objectId;
		if (storeInDb)
		{
			updateClanInDB();
		}
	}
	
	public int getNewLeaderId()
	{
		return _newLeaderId;
	}
	
	public Player getNewLeader()
	{
		return GameObjectsStorage.getPlayer(_newLeaderId);
	}
	
	public String getNewLeaderName()
	{
		return CharNameHolder.getInstance().getNameById(_newLeaderId);
	}
	
	public int getSiegeKills()
	{
		return _siegeKills != null ? _siegeKills.get() : 0;
	}
	
	public int getSiegeDeaths()
	{
		return _siegeDeaths != null ? _siegeDeaths.get() : 0;
	}
	
	public int addSiegeKill()
	{
		if (_siegeKills == null)
		{
			synchronized (this)
			{
				if (_siegeKills == null)
				{
					_siegeKills = new AtomicInteger();
				}
			}
		}
		return _siegeKills.incrementAndGet();
	}
	
	public int addSiegeDeath()
	{
		if (_siegeDeaths == null)
		{
			synchronized (this)
			{
				if (_siegeDeaths == null)
				{
					_siegeDeaths = new AtomicInteger();
				}
			}
		}
		return _siegeDeaths.incrementAndGet();
	}
	
	public void clearSiegeKills()
	{
		if (_siegeKills != null)
		{
			_siegeKills.set(0);
		}
	}
	
	public void clearSiegeDeaths()
	{
		if (_siegeDeaths != null)
		{
			_siegeDeaths.set(0);
		}
	}

	private void restoreClanRecruitment()
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT * FROM clan_requiements where clan_id=" + getId());
			rset = statement.executeQuery();
			while (rset.next())
			{
				_recruting = (rset.getInt("recruting") == 1 ? true : false);
				for (final String clas : rset.getString("classes").split(","))
				{
					if (clas.length() > 0)
					{
						_classesNeeded.add(Integer.parseInt(clas));
					}
				}
				for (int i = 1; i <= 8; i++)
				{
					_questions[(i - 1)] = rset.getString("question" + i);
				}
			}
			statement.close();
			rset.close();
			
			statement = con.prepareStatement("SELECT * FROM clan_petitions where clan_id=" + getId());
			rset = statement.executeQuery();
			while (rset.next())
			{
				final String[] answers = new String[8];
				for (int i = 1; i <= 8; i++)
				{
					answers[(i - 1)] = rset.getString("answer" + i);
				}
				_petitions.add(new SinglePetition(rset.getInt("sender_id"), answers, rset.getString("comment")));
			}
		}
		catch (
		    NumberFormatException | SQLException e)
		{
			_log.warn("Error while restoring Clan Recruitment", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
	}

	public void updateRecrutationData()
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("INSERT INTO clan_requiements VALUES(" + getId() + ",0,'','','','','','','','','') ON DUPLICATE KEY UPDATE recruting=?,classes=?,question1=?,question2=?,question3=?,question4=?,question5=?,question6=?,question7=?,question8=?");
			statement.setInt(1, (_recruting == true ? 1 : 0));
			statement.setString(2, getClassesForData());
			for (int i = 0; i < 8; i++)
			{
				statement.setString(i + 3, _questions[i] == null ? "" : _questions[i]);
			}
			statement.execute();
			statement.close();
			
			statement = con.prepareStatement("DELETE FROM clan_petitions WHERE clan_id=" + getId());
			statement.execute();
			statement.close();


			for (final SinglePetition petition : getPetitions())
			{
				statement = con.prepareStatement("INSERT IGNORE INTO clan_petitions VALUES(?,?,?,?,?,?,?,?,?,?,?)");
				statement.setInt(1, petition.getSenderId());
				statement.setInt(2, getId());
				for (int i = 0; i < 8; i++)
				{
					statement.setString(i + 3, petition.getAnswers()[i] == null ? "" : petition.getAnswers()[i]);
				}
				statement.setString(11, petition.getComment());
				statement.execute();
				statement.close();
			}
		}
		catch (final SQLException e)
		{
			_log.warn("Error while updating clan recruitment system on clan id '" + _clanId + "' in db", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	public void setQuestions(String[] questions)
	{
		_questions = questions;
	}

	public class SinglePetition
	{
		int _sender;
		String[] _answers;
		String _comment;

		private SinglePetition(int sender, String[] answers, String comment)
		{
			_sender = sender;
			_answers = answers;
			_comment = comment;
		}

		public int getSenderId()
		{
			return _sender;
		}

		public String[] getAnswers()
		{
			return _answers;
		}

		public String getComment()
		{
			return _comment;
		}
	}

	public synchronized boolean addPetition(int senderId, String[] answers, String comment)
	{
		if (getPetition(senderId) != null)
		{
			return false;
		}

		_petitions.add(new SinglePetition(senderId, answers, comment));
		updateRecrutationData();

		final var leader = GameObjectsStorage.getPlayer(getLeaderId());
		if (leader != null)
		{
			leader.sendMessage("New Clan Petition has arrived!");
		}
		return true;
	}

	public SinglePetition getPetition(int senderId)
	{
		return _petitions.stream().filter(petition -> petition.getSenderId() == senderId).findAny().orElse(null);
	}

	public ArrayList<SinglePetition> getPetitions()
	{
		return _petitions;
	}

	public synchronized void deletePetition(int senderId)
	{
		final SinglePetition petition = _petitions.stream().filter(p -> p.getSenderId() == senderId).findAny().orElse(null);
		if (petition != null)
		{
			_petitions.remove(petition);
			updateRecrutationData();
		}
	}

	public void deletePetition(SinglePetition petition)
	{
		_petitions.remove(petition);
		updateRecrutationData();
	}

	public void setRecrutating(boolean b)
	{
		_recruting = b;
	}

	public void addClassNeeded(int clas)
	{
		_classesNeeded.add(clas);
	}

	public void deleteClassNeeded(int clas)
	{
		final int indexOfClass = _classesNeeded.indexOf(clas);
		if (indexOfClass != -1)
		{
			_classesNeeded.remove(indexOfClass);
		}
		else
		{
			_log.warn("Tried removing inexistent class: " + clas);
		}
	}

	public String getClassesForData()
	{
		String text = "";
		for (int i = 0; i < getClassesNeeded().size(); i++)
		{
			if (i != 0)
			{
				text += ",";
			}
			text += getClassesNeeded().get(i);
		}
		return text;
	}

	public ArrayList<Integer> getClassesNeeded()
	{
		return _classesNeeded;
	}

	public boolean isRecruting()
	{
		return _recruting;
	}

	public String[] getQuestions()
	{
		return _questions;
	}

	public boolean isFull()
	{
		for (final ClanMember unit : getMembers())
		{
			if (getSubPledgeMembersCount(unit.getPledgeType()) < getMaxNrOfMembers(unit.getPledgeType()))
			{
				return false;
			}
		}
		return true;
	}

	private static class ClanReputationComparator implements Comparator<Clan>, Serializable
	{
		private static final long serialVersionUID = -240267507300918307L;

		@Override
		public int compare(Clan o1, Clan o2)
		{
			if (o1 == null || o2 == null)
			{
				return 0;
			}
			return Integer.compare(o2.getReputationScore(), o1.getReputationScore());
		}
	}
	
	public List<AbstractTemplate> getClanTasks(int type)
	{
		if (!_templates.containsKey(type))
		{
			_templates.put(type, new ArrayList<>());
		}
		return _templates.get(type);
	}
	
	public void addTask(AbstractTemplate template, int type)
	{
		if (!_templates.containsKey(type))
		{
			_templates.put(type, new ArrayList<>());
		}
		_templates.get(type).add(template);
	}
	
	public void removeClanTask(int type, AbstractTemplate tpl)
	{
		if (_templates.containsKey(type))
		{
			_templates.get(type).remove(tpl);
		}
	}
	
	public void cleanClanTasks()
	{
		for (final var list : _templates.values())
		{
			if (list != null && !list.isEmpty())
			{
				list.clear();
			}
		}
	}
	
	public Collection<List<AbstractTemplate>> getClanTasks()
	{
		return _templates.values();
	}
	
	public void setVar(String name, Object value)
	{
		setVar(name, String.valueOf(value), -1);
	}
	
	public void setVar(String name, String value, long expirationTime)
	{
		final var var = new CharacterVariable(name, String.valueOf(value), expirationTime);
		if (ClanVariablesDAO.getInstance().insert(getId(), var))
		{
			_variables.put(name, var);
		}
	}
	
	public void restoreVar(CharacterVariable var)
	{
		_variables.put(var.getName(), var);
	}
	
	public void unsetVar(String name)
	{
		if (name == null || name.isEmpty())
		{
			return;
		}
		
		if (_variables.containsKey(name) && ClanVariablesDAO.getInstance().delete(getId(), name))
		{
			_variables.remove(name);
		}
	}
	
	public CharacterVariable getVariable(String name)
	{
		final var var = _variables.get(name);
		if (var != null)
		{
			return var;
		}
		return null;
	}
	
	public String getVar(String name)
	{
		return getVar(name, null);
	}
	
	public String getVar(String name, String defaultValue)
	{
		final var var = _variables.get(name);
		if (var != null && !var.isExpired())
		{
			return var.getValue();
		}
		return defaultValue;
	}
	
	public boolean getVarB(String name, boolean defaultVal)
	{
		final String var = getVar(name);
		if (var != null)
		{
			return !(var.equals("0") || var.equalsIgnoreCase("false"));
		}
		return defaultVal;
	}
	
	public byte[] getByteArray(String key, String splitOn)
	{
		final Object val = getVar(key);
		if (val == null)
		{
			throw new IllegalArgumentException("Byte value required, but not specified");
		}
		if (val instanceof Number)
		{
			return new byte[]
			{
			        ((Number) val).byteValue()
			};
		}
		int c = 0;
		final String[] vals = ((String) val).split(splitOn);
		final byte[] result = new byte[vals.length];
		for (final String v : vals)
		{
			try
			{
				result[c++] = Byte.parseByte(v);
			}
			catch (final Exception e)
			{
				throw new IllegalArgumentException("Byte value required, but found: " + val);
			}
		}
		return result;
	}
	
	public boolean getVarB(String name)
	{
		return getVarB(name, false);
	}
	
	public Collection<CharacterVariable> getVars()
	{
		return _variables.values();
	}
	
	public int getVarInt(final String name)
	{
		return getVarInt(name, 0);
	}
	
	public int getVarInt(final String name, final int defaultVal)
	{
		int result = defaultVal;
		final String var = getVar(name);
		if (var != null)
		{
			result = Integer.parseInt(var);
		}
		return result;
	}
	
	public long getVarLong(final String name, long defaultVal)
	{
		long result = defaultVal;
		final String var = getVar(name);
		if (var != null)
		{
			result = Long.parseLong(var);
		}
		return result;
	}
	
	public void addBonusType(BonusType type, double value)
	{
		if (_bonusList.containsKey(type))
		{
			final var val = _bonusList.get(type);
			switch (type)
			{
				case CRAFT_CHANCE :
				case MASTER_WORK_CHANCE :
				case ENCHANT_CHANCE :
				case MAX_DROP_PER_ONE_GROUP :
				case MAX_SPOIL_PER_ONE_GROUP :
				case MAX_RAID_DROP_PER_ONE_GROUP :
					_bonusList.put(type, (val + value));
					break;
				default :
					_bonusList.put(type, (val + (value - 1)));
					break;
			}
			return;
		}
		_bonusList.put(type, value);
	}
	
	public void removeBonusType(BonusType type, double value)
	{
		if (_bonusList.containsKey(type))
		{
			final var val = _bonusList.get(type);
			if (val != value)
			{
				switch (type)
				{
					case CRAFT_CHANCE :
					case MASTER_WORK_CHANCE :
					case ENCHANT_CHANCE :
					case MAX_DROP_PER_ONE_GROUP :
					case MAX_SPOIL_PER_ONE_GROUP :
					case MAX_RAID_DROP_PER_ONE_GROUP :
						_bonusList.put(type, (val - value));
						break;
					default :
						_bonusList.put(type, (val - (value - 1)));
						break;
				}
				return;
			}
			_bonusList.remove(type);
		}
	}
	
	public double getBonusType(BonusType type, double defaultValue)
	{
		if (!_bonusList.containsKey(type))
		{
			return defaultValue;
		}
		return Math.max(0, _bonusList.get(type));
	}
}