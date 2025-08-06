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
package l2e.gameserver.model.entity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2e.commons.dbutils.DbUtils;
import l2e.gameserver.Config;
import l2e.gameserver.SevenSigns;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.data.holder.ClanHolder;
import l2e.gameserver.data.parser.DoorParser;
import l2e.gameserver.data.parser.SkillTreesParser;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.handler.voicedcommandhandlers.IVoicedCommandHandler;
import l2e.gameserver.handler.voicedcommandhandlers.VoicedCommandHandler;
import l2e.gameserver.instancemanager.CastleManager;
import l2e.gameserver.instancemanager.CastleManorManager;
import l2e.gameserver.instancemanager.DailyTaskManager;
import l2e.gameserver.instancemanager.FortManager;
import l2e.gameserver.instancemanager.TerritoryWarManager;
import l2e.gameserver.instancemanager.TerritoryWarManager.Territory;
import l2e.gameserver.instancemanager.ZoneManager;
import l2e.gameserver.model.Clan;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.MountType;
import l2e.gameserver.model.SkillLearn;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.ArtefactInstance;
import l2e.gameserver.model.actor.instance.DoorInstance;
import l2e.gameserver.model.actor.templates.daily.DailyTaskTemplate;
import l2e.gameserver.model.actor.templates.player.PlayerTaskTemplate;
import l2e.gameserver.model.actor.templates.sieges.CastleTemplate;
import l2e.gameserver.model.interfaces.IIdentifiable;
import l2e.gameserver.model.items.itemcontainer.PcInventory;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.zone.type.CastleZone;
import l2e.gameserver.model.zone.type.ResidenceTeleportZone;
import l2e.gameserver.model.zone.type.SiegeZone;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.PlaySound;
import l2e.gameserver.network.serverpackets.SystemMessage;
import l2e.gameserver.network.serverpackets.pledge.PledgeShowInfoUpdate;

public class Castle implements IIdentifiable
{
	protected static final Logger _log = LoggerFactory.getLogger(Castle.class);

	private int _castleId = 0;
	private final List<DoorInstance> _doors = new ArrayList<>();
	private String _name = "";
	private int _ownerId = 0;
	private Siege _siege = null;
	private long _siegeStartTime;
	private int _taxPercent = 0;
	private double _taxRate = 0;
	private long _treasury = 0;
	private SiegeZone _zone = null;
	private CastleZone _castleZone = null;
	private ResidenceTeleportZone _teleZone;
	private Clan _formerOwner = null;
	private final List<ArtefactInstance> _artefacts = new ArrayList<>(1);
	private final Map<Integer, CastleFunction> _function;
	private final List<Skill> _residentialSkills = new ArrayList<>();
	private int _ticketBuyCount = 0;
	
	public static final int FUNC_TELEPORT = 1;
	public static final int FUNC_RESTORE_HP = 2;
	public static final int FUNC_RESTORE_MP = 3;
	public static final int FUNC_RESTORE_EXP = 4;
	public static final int FUNC_SUPPORT = 5;
	private CastleTemplate _template = null;

	public class CastleFunction
	{
		private final int _type;
		private int _lvl;
		protected int _fee;
		protected int _tempFee;
		private final long _rate;
		private long _endDate;
		protected boolean _inDebt;
		public boolean _cwh;

		public CastleFunction(int type, int lvl, int lease, int tempLease, long rate, long time, boolean cwh)
		{
			_type = type;
			_lvl = lvl;
			_fee = lease;
			_tempFee = tempLease;
			_rate = rate;
			_endDate = time;
			initializeTask(cwh);
		}

		public int getType()
		{
			return _type;
		}

		public int getLvl()
		{
			return _lvl;
		}

		public int getLease()
		{
			return _fee;
		}

		public long getRate()
		{
			return _rate;
		}

		public long getEndTime()
		{
			return _endDate;
		}

		public void setLvl(int lvl)
		{
			_lvl = lvl;
		}

		public void setLease(int lease)
		{
			_fee = lease;
		}

		public void setEndTime(long time)
		{
			_endDate = time;
		}

		private void initializeTask(boolean cwh)
		{
			if (getOwnerId() <= 0)
			{
				return;
			}
			final long currentTime = System.currentTimeMillis();
			if (_endDate > currentTime)
			{
				ThreadPoolManager.getInstance().schedule(new FunctionTask(cwh), _endDate - currentTime);
			}
			else
			{
				ThreadPoolManager.getInstance().schedule(new FunctionTask(cwh), 0);
			}
		}

		private class FunctionTask implements Runnable
		{
			public FunctionTask(boolean cwh)
			{
				_cwh = cwh;
			}

			@Override
			public void run()
			{
				try
				{
					if (getOwnerId() <= 0)
					{
						return;
					}
					if ((ClanHolder.getInstance().getClan(getOwnerId()).getWarehouse().getAdena() >= _fee) || !_cwh)
					{
						int fee = _fee;
						if (getEndTime() == -1)
						{
							fee = _tempFee;
						}

						setEndTime(System.currentTimeMillis() + getRate());
						dbSave();
						if (_cwh)
						{
							ClanHolder.getInstance().getClan(getOwnerId()).getWarehouse().destroyItemByItemId("CS_function_fee", PcInventory.ADENA_ID, fee, null, null);
						}
						ThreadPoolManager.getInstance().schedule(new FunctionTask(true), getRate());
					}
					else
					{
						removeFunction(getType());
					}
				}
				catch (final Exception e)
				{
					_log.warn("", e);
				}
			}
		}

		public void dbSave()
		{
			Connection con = null;
			PreparedStatement statement = null;
			try
			{
				con = DatabaseFactory.getInstance().getConnection();
				statement = con.prepareStatement("REPLACE INTO castle_functions (castle_id, type, lvl, lease, rate, endTime) VALUES (?,?,?,?,?,?)");
				statement.setInt(1, getId());
				statement.setInt(2, getType());
				statement.setInt(3, getLvl());
				statement.setInt(4, getLease());
				statement.setLong(5, getRate());
				statement.setLong(6, getEndTime());
				statement.execute();
			}
			catch (final Exception e)
			{
				_log.warn("Exception: Castle.updateFunctions(int type, int lvl, int lease, long rate, long time, boolean addNew): " + e.getMessage(), e);
			}
			finally
			{
				DbUtils.closeQuietly(con, statement);
			}
		}
	}

	public Castle(int castleId)
	{
		_castleId = castleId;
		load();
		_function = new ConcurrentHashMap<>();
		final List<SkillLearn> residentialSkills = SkillTreesParser.getInstance().getAvailableResidentialSkills(castleId);
		for (final SkillLearn s : residentialSkills)
		{
			final Skill sk = SkillsParser.getInstance().getInfo(s.getId(), s.getLvl());
			if (sk != null)
			{
				_residentialSkills.add(sk);
			}
			else
			{
				_log.warn("Castle Id: " + castleId + " has a null residential skill Id: " + s.getId() + " level: " + s.getLvl() + "!");
			}
		}
		if (getOwnerId() != 0)
		{
			loadFunctions();
		}
	}

	public CastleFunction getFunction(int type)
	{
		if (_function.containsKey(type))
		{
			return _function.get(type);
		}
		return null;
	}

	public synchronized void engrave(Clan clan, GameObject target)
	{
		if (!_artefacts.contains(target))
		{
			return;
		}
		setOwner(clan);
		final SystemMessage msg = SystemMessage.getSystemMessage(SystemMessageId.CLAN_S1_ENGRAVED_RULER);
		msg.addString(clan.getName());
		getSiege().announceToPlayer(msg, true);
	}

	public void addToTreasury(long amount)
	{
		if (getOwnerId() <= 0)
		{
			return;
		}

		if (_name.equalsIgnoreCase("Schuttgart") || _name.equalsIgnoreCase("Goddard"))
		{
			final Castle rune = CastleManager.getInstance().getCastleById(8);
			if (rune != null)
			{
				final long runeTax = (long) (amount * rune.getTaxRate());
				if (rune.getOwnerId() > 0)
				{
					rune.addToTreasury(runeTax);
				}
				amount -= runeTax;
			}
		}
		if (!_name.equalsIgnoreCase("aden") && !_name.equalsIgnoreCase("Rune") && !_name.equalsIgnoreCase("Schuttgart") && !_name.equalsIgnoreCase("Goddard"))
		{
			final Castle aden = CastleManager.getInstance().getCastleById(5);
			if (aden != null)
			{
				final long adenTax = (long) (amount * aden.getTaxRate());
				if (aden.getOwnerId() > 0)
				{
					aden.addToTreasury(adenTax);
				}

				amount -= adenTax;
			}
		}

		addToTreasuryNoTax(amount);
	}

	public boolean addToTreasuryNoTax(long amount)
	{
		if (getOwnerId() <= 0)
		{
			return false;
		}

		if (amount < 0)
		{
			amount *= -1;
			if (_treasury < amount)
			{
				return false;
			}
			_treasury -= amount;
		}
		else
		{
			if ((_treasury + amount) > PcInventory.MAX_ADENA)
			{
				_treasury = PcInventory.MAX_ADENA;
			}
			else
			{
				_treasury += amount;
			}
		}

		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE castle SET treasury = ? WHERE id = ?");
			statement.setLong(1, getTreasury());
			statement.setInt(2, getId());
			statement.execute();
		}
		catch (final Exception e)
		{
			_log.warn(e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
		return true;
	}

	public void banishForeigners()
	{
		getCastleZone().banishForeigners(getOwnerId());
	}

	public boolean checkIfInZone(int x, int y, int z)
	{
		return getZone().isInsideZone(x, y, z);
	}

	public SiegeZone getZone()
	{
		if (_zone == null)
		{
			for (final SiegeZone zone : ZoneManager.getInstance().getAllZones(SiegeZone.class))
			{
				if (zone.getSiegeObjectId() == getId())
				{
					_zone = zone;
					break;
				}
			}
		}
		return _zone;
	}

	public CastleZone getCastleZone()
	{
		if (_castleZone == null)
		{
			for (final CastleZone zone : ZoneManager.getInstance().getAllZones(CastleZone.class))
			{
				if (zone.getCastleId() == getId())
				{
					_castleZone = zone;
					break;
				}
			}
		}
		return _castleZone;
	}

	public ResidenceTeleportZone getTeleZone()
	{
		if (_teleZone == null)
		{
			for (final ResidenceTeleportZone zone : ZoneManager.getInstance().getAllZones(ResidenceTeleportZone.class))
			{
				if (zone.getResidenceId() == getId())
				{
					_teleZone = zone;
					break;
				}
			}
		}
		return _teleZone;
	}

	public void oustAllPlayers()
	{
		getTeleZone().oustAllPlayers();
	}

	public double getDistance(GameObject obj)
	{
		return getZone().getDistanceToZone(obj);
	}

	public void closeDoor(Player activeChar, int doorId)
	{
		openCloseDoor(activeChar, doorId, false);
	}

	public void openDoor(Player activeChar, int doorId)
	{
		openCloseDoor(activeChar, doorId, true);
	}

	public void openCloseDoor(Player activeChar, int doorId, boolean open)
	{
		if (activeChar.getClanId() != getOwnerId())
		{
			return;
		}

		final DoorInstance door = getDoor(doorId);
		if (door != null)
		{
			if (open)
			{
				door.openMe();
			}
			else
			{
				door.closeMe();
			}
		}
	}

	public void removeUpgrade()
	{
		removeDoorUpgrade();
		for (final Integer fc : _function.keySet())
		{
			removeFunction(fc);
		}
		_function.clear();
	}

	public void setOwner(Clan clan)
	{
		Clan oldOwner = null;
		if ((getOwnerId() > 0) && ((clan == null) || (clan.getId() != getOwnerId())))
		{
			oldOwner = ClanHolder.getInstance().getClan(getOwnerId());
			if (oldOwner != null)
			{
				if (_formerOwner == null)
				{
					_formerOwner = oldOwner;
					if (Config.REMOVE_CASTLE_CIRCLETS)
					{
						CastleManager.getInstance().removeCirclet(_formerOwner, getId());
					}
				}
				try
				{
					final Player oldleader = oldOwner.getLeader().getPlayerInstance();
					if (oldleader != null)
					{
						if (oldleader.getMountType() == MountType.WYVERN)
						{
							oldleader.dismount();
						}
					}
				}
				catch (final Exception e)
				{
					_log.warn("Exception in setOwner: " + e.getMessage(), e);
				}
				oldOwner.setCastleId(0);
				getTerritory().changeOwner(null);
				for (final Player member : oldOwner.getOnlineMembers(0))
				{
					removeResidentialSkills(member);
					member.sendSkillList(false);
					if (Config.ALLOW_DAILY_TASKS)
					{
						if (member.getActiveDailyTasks() != null)
						{
							for (final PlayerTaskTemplate taskTemplate : member.getActiveDailyTasks())
							{
								if (taskTemplate.getType().equalsIgnoreCase("Siege") && !taskTemplate.isComplete())
								{
									final DailyTaskTemplate task = DailyTaskManager.getInstance().getDailyTask(taskTemplate.getId());
									if (task.getSiegeCastle() && task.isAttackSiege())
									{
										taskTemplate.setIsComplete(true);
										member.updateDailyStatus(taskTemplate);
										final IVoicedCommandHandler vch = VoicedCommandHandler.getInstance().getHandler("missions");
										if (vch != null)
										{
											vch.useVoicedCommand("missions", member, null);
										}
									}
								}
							}
						}
					}
				}
			}
		}
		updateOwnerInDB(clan);

		if ((clan != null) && (clan.getFortId() > 0))
		{
			FortManager.getInstance().getFortByOwner(clan).removeOwner(true);
		}

		getTerritory().setOwnerClan(clan);

		if (clan != null)
		{
			if (Config.SHOW_CREST_WITHOUT_QUEST)
			{
				for (final Npc npc : GameObjectsStorage.getNpcs())
				{
					if (npc != null)
					{
						if (npc.getTerritory() == getTerritory())
						{
							npc.broadcastInfo();
						}
					}
				}
			}
			
			for (final Player member : clan.getOnlineMembers(0))
			{
				giveResidentialSkills(member);
				member.sendSkillList(false);
			}
		}

		updateClansReputation();

		for (final Player member : clan.getOnlineMembers(0))
		{
			giveResidentialSkills(member);
			member.sendSkillList(false);
		}

		if (getSiege().getIsInProgress())
		{
			getSiege().midVictory();
		}
	}

	public void removeOwner(Clan clan)
	{
		if (clan != null)
		{
			_formerOwner = clan;
			if (Config.REMOVE_CASTLE_CIRCLETS)
			{
				CastleManager.getInstance().removeCirclet(_formerOwner, getId());
			}
			for (final Player member : clan.getOnlineMembers(0))
			{
				removeResidentialSkills(member);
				member.sendSkillList(false);
			}
			clan.setCastleId(0);
			clan.broadcastToOnlineMembers(new PledgeShowInfoUpdate(clan));
		}

		updateOwnerInDB(null);
		if (getSiege().getIsInProgress())
		{
			getSiege().midVictory();
		}

		for (final Integer fc : _function.keySet())
		{
			removeFunction(fc);
		}
		_function.clear();
	}

	public void setTaxPercent(Player activeChar, int taxPercent)
	{
		int maxTax;
		switch (SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE))
		{
			case SevenSigns.CABAL_DAWN :
				maxTax = 25;
				break;
			case SevenSigns.CABAL_DUSK :
				maxTax = 5;
				break;
			default :
				maxTax = 15;
		}

		if ((taxPercent < 0) || (taxPercent > maxTax))
		{
			activeChar.sendMessage("Tax value must be between 0 and " + maxTax + ".");
			return;
		}

		setTaxPercent(taxPercent);
		activeChar.sendMessage(getName(activeChar.getLang()) + " castle tax changed to " + taxPercent + "%.");
	}

	public void setTaxPercent(int taxPercent)
	{
		_taxPercent = taxPercent;
		_taxRate = _taxPercent / 100.0;

		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE castle SET taxPercent = ? WHERE id = ?");
			statement.setInt(1, taxPercent);
			statement.setInt(2, getId());
			statement.execute();
		}
		catch (final Exception e)
		{
			_log.warn(e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	public void spawnDoor()
	{
		spawnDoor(false);
	}

	public void spawnDoor(boolean isDoorWeak)
	{
		for (final DoorInstance door : _doors)
		{
			if (door.isDead())
			{
				door.doRevive();
				if (isDoorWeak)
				{
					door.setCurrentHp(door.getMaxHp() / 2);
				}
				else
				{
					door.setCurrentHp(door.getMaxHp());
				}
			}

			if (door.getOpen())
			{
				door.closeMe();
			}
		}
		loadDoorUpgrade();
	}

	public void upgradeDoor(int doorId, int hp, int pDef, int mDef)
	{
		final DoorInstance door = getDoor(doorId);
		if (door == null)
		{
			return;
		}

		door.setCurrentHp(door.getMaxHp() + hp);

		saveDoorUpgrade(doorId, hp, pDef, mDef);
	}

	private void load()
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT * FROM castle WHERE id = ?");
			statement.setInt(1, getId());
			rs = statement.executeQuery();
			while (rs.next())
			{
				_name = rs.getString("name");
				_siegeStartTime = rs.getLong("siegeDate");
				_taxPercent = rs.getInt("taxPercent");
				_treasury = rs.getLong("treasury");
				
				_ticketBuyCount = rs.getInt("ticketBuyCount");
			}
			_taxRate = _taxPercent / 100.0;
			rs.close();
			statement.close();
			
			statement = con.prepareStatement("SELECT clan_id FROM clan_data WHERE hasCastle = ?");
			statement.setInt(1, getId());
			rs = statement.executeQuery();
			while (rs.next())
			{
				_ownerId = rs.getInt("clan_id");
			}
		}
		catch (final Exception e)
		{
			_log.warn("Exception: loadCastleData(): " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rs);
		}
	}

	private void loadFunctions()
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT * FROM castle_functions WHERE castle_id = ?");
			statement.setInt(1, getId());
			rs = statement.executeQuery();
			while (rs.next())
			{
				_function.put(rs.getInt("type"), new CastleFunction(rs.getInt("type"), rs.getInt("lvl"), rs.getInt("lease"), 0, rs.getLong("rate"), rs.getLong("endTime"), true));
			}
		}
		catch (final Exception e)
		{
			_log.warn("Exception: Castle.loadFunctions(): " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rs);
		}
	}

	public void removeFunction(int functionType)
	{
		_function.remove(functionType);
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("DELETE FROM castle_functions WHERE castle_id=? AND type=?");
			statement.setInt(1, getId());
			statement.setInt(2, functionType);
			statement.execute();
		}
		catch (final Exception e)
		{
			_log.warn("Exception: Castle.removeFunctions(int functionType): " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	public boolean updateFunctions(Player player, int type, int lvl, int lease, long rate, boolean addNew)
	{
		if (player == null)
		{
			return false;
		}
		if (lease > 0)
		{
			if (!player.destroyItemByItemId("Consume", PcInventory.ADENA_ID, lease, null, true))
			{
				return false;
			}
		}
		if (addNew)
		{
			_function.put(type, new CastleFunction(type, lvl, lease, 0, rate, 0, false));
		}
		else
		{
			if ((lvl == 0) && (lease == 0))
			{
				removeFunction(type);
			}
			else
			{
				final int diffLease = lease - _function.get(type).getLease();
				if (diffLease > 0)
				{
					_function.remove(type);
					_function.put(type, new CastleFunction(type, lvl, lease, 0, rate, -1, false));
				}
				else
				{
					_function.get(type).setLease(lease);
					_function.get(type).setLvl(lvl);
					_function.get(type).dbSave();
				}
			}
		}
		return true;
	}

	public void activateInstance()
	{
		loadDoor();
	}

	private void loadDoor()
	{
		for (final DoorInstance door : DoorParser.getInstance().getDoors())
		{
			if ((door.getCastle() != null) && (door.getCastle().getId() == getId()))
			{
				_doors.add(door);
			}
		}
	}

	private void loadDoorUpgrade()
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			final StringBuilder doorIds = new StringBuilder(100);
			for (final DoorInstance door : getDoors())
			{
				doorIds.append(door.getDoorId()).append(',');
			}
			doorIds.deleteCharAt(doorIds.length() - 1);
			statement = con.prepareStatement("Select * from castle_doorupgrade where doorId in (" + doorIds.toString() + ")");
			rs = statement.executeQuery();
			while (rs.next())
			{
				upgradeDoor(rs.getInt("id"), rs.getInt("hp"), rs.getInt("pDef"), rs.getInt("mDef"));
			}
		}
		catch (final Exception e)
		{
			_log.warn("Exception: loadCastleDoorUpgrade(): " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rs);
		}
	}

	private void removeDoorUpgrade()
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			
			final StringBuilder doorIds = new StringBuilder(100);
			for (final DoorInstance door : getDoors())
			{
				doorIds.append(door.getDoorId()).append(',');
			}
			doorIds.deleteCharAt(doorIds.length() - 1);
			statement = con.prepareStatement("delete from castle_doorupgrade where doorId in (" + doorIds.toString() + ")");
			statement.execute();
		}
		catch (final Exception e)
		{
			_log.warn("Exception: removeDoorUpgrade(): " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	private void saveDoorUpgrade(int doorId, int hp, int pDef, int mDef)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("INSERT INTO castle_doorupgrade (doorId, hp, pDef, mDef) values (?,?,?,?)");
			statement.setInt(1, doorId);
			statement.setInt(2, hp);
			statement.setInt(3, pDef);
			statement.setInt(4, mDef);
			statement.execute();
		}
		catch (final Exception e)
		{
			_log.warn("Exception: saveDoorUpgrade(int doorId, int hp, int pDef, int mDef): " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	private void updateOwnerInDB(Clan clan)
	{
		if (clan != null)
		{
			_ownerId = clan.getId();
		}
		else
		{
			_ownerId = 0;
			CastleManorManager.getInstance().resetManorData(getId());
		}

		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE clan_data SET hasCastle=0 WHERE hasCastle=?");
			statement.setInt(1, getId());
			statement.execute();
			statement.close();

			statement = con.prepareStatement("UPDATE clan_data SET hasCastle=? WHERE clan_id=?");
			statement.setInt(1, getId());
			statement.setInt(2, getOwnerId());
			statement.execute();
			if (clan != null)
			{
				clan.setCastleId(getId());
				clan.broadcastToOnlineMembers(new PledgeShowInfoUpdate(clan));
				clan.broadcastToOnlineMembers(new PlaySound(1, "Siege_Victory", 0, 0, 0, 0, 0));
			}
		}
		catch (final Exception e)
		{
			_log.warn("Exception: updateOwnerInDB(Clan clan): " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	@Override
	public final int getId()
	{
		return _castleId;
	}

	public final DoorInstance getDoor(int doorId)
	{
		if (doorId <= 0)
		{
			return null;
		}

		for (final DoorInstance door : getDoors())
		{
			if (door.getDoorId() == doorId)
			{
				return door;
			}
		}
		return null;
	}

	public final List<DoorInstance> getDoors()
	{
		return _doors;
	}

	public final String getName1()
	{
		return _name;
	}
	
	public final String getName(String lang)
	{
		return getTemplate() != null ? getTemplate().getName(lang) : "";
	}

	public final int getOwnerId()
	{
		return _ownerId;
	}
	
	public final Clan getOwner()
	{
		return (_ownerId != 0) ? ClanHolder.getInstance().getClan(_ownerId) : null;
	}

	public final Siege getSiege()
	{
		if (_siege == null)
		{
			_siege = new Siege(this, _siegeStartTime);
		}
		return _siege;
	}

	public final int getTaxPercent()
	{
		return _taxPercent;
	}

	public final double getTaxRate()
	{
		return _taxRate;
	}

	public final long getTreasury()
	{
		return _treasury;
	}

	public void updateClansReputation()
	{
		if (_formerOwner != null)
		{
			if (_formerOwner != ClanHolder.getInstance().getClan(getOwnerId()))
			{
				final int maxreward = Math.max(0, _formerOwner.getReputationScore());
				_formerOwner.takeReputationScore(Config.LOOSE_CASTLE_POINTS, true);
				final Clan owner = ClanHolder.getInstance().getClan(getOwnerId());
				if (owner != null)
				{
					owner.addReputationScore(Math.min(Config.TAKE_CASTLE_POINTS, maxreward), true);
				}
			}
			else
			{
				_formerOwner.addReputationScore(Config.CASTLE_DEFENDED_POINTS, true);
			}
		}
		else
		{
			final Clan owner = ClanHolder.getInstance().getClan(getOwnerId());
			if (owner != null)
			{
				owner.addReputationScore(Config.TAKE_CASTLE_POINTS, true);
			}
		}
	}

	public List<Skill> getResidentialSkills()
	{
		return _residentialSkills;
	}

	public void giveResidentialSkills(Player player)
	{
		for (final Skill sk : _residentialSkills)
		{
			player.addSkill(sk, false);
		}
		
		if ((getTerritory() != null) && getTerritory().getOwnedWardIds().contains(getId() + 80))
		{
			for (final int wardId : getTerritory().getOwnedWardIds())
			{
				final List<SkillLearn> territorySkills = SkillTreesParser.getInstance().getAvailableResidentialSkills(wardId);
				for (final SkillLearn s : territorySkills)
				{
					final Skill sk = SkillsParser.getInstance().getInfo(s.getId(), s.getLvl());
					if (sk != null)
					{
						player.addSkill(sk, false);
					}
					else
					{
						_log.warn("Trying to add a null skill for Territory Ward Id: " + wardId + ", skill Id: " + s.getId() + " level: " + s.getLvl() + "!");
					}
				}
			}
		}
	}

	public void removeResidentialSkills(Player player)
	{
		for (final Skill sk : _residentialSkills)
		{
			player.removeSkill(sk, false, true);
		}
		if (getTerritory() != null)
		{
			for (final int wardId : getTerritory().getOwnedWardIds())
			{
				final List<SkillLearn> territorySkills = SkillTreesParser.getInstance().getAvailableResidentialSkills(wardId);
				for (final SkillLearn s : territorySkills)
				{
					final Skill sk = SkillsParser.getInstance().getInfo(s.getId(), s.getLvl());
					if (sk != null)
					{
						player.removeSkill(sk, false, true);
					}
					else
					{
						_log.warn("Trying to remove a null skill for Territory Ward Id: " + wardId + ", skill Id: " + s.getId() + " level: " + s.getLvl() + "!");
					}
				}
			}
		}
	}

	public void registerArtefact(ArtefactInstance artefact)
	{
		_artefacts.add(artefact);
	}

	public List<ArtefactInstance> getArtefacts()
	{
		return _artefacts;
	}

	public int getTicketBuyCount()
	{
		return _ticketBuyCount;
	}

	public void setTicketBuyCount(int count)
	{
		_ticketBuyCount = count;

		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE castle SET ticketBuyCount = ? WHERE id = ?");
			statement.setInt(1, _ticketBuyCount);
			statement.setInt(2, _castleId);
			statement.execute();
		}
		catch (final Exception e)
		{
			_log.warn(e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
	
	public Territory getTerritory()
	{
		return TerritoryWarManager.getInstance().getTerritory(getId());
	}
	
	public void setTemplate(CastleTemplate val)
	{
		_template = val;
	}
	
	public CastleTemplate getTemplate()
	{
		return _template;
	}

	@Override
	public String toString()
	{
		return _name + "(" + _castleId + ")";
	}
}