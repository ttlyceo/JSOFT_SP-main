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
package l2e.scripts.clanhallsiege;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import l2e.commons.util.Util;
import l2e.gameserver.Announcements;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.ai.guard.SpecialGuardAI;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.data.holder.ClanHolder;
import l2e.gameserver.data.htm.HtmCache;
import l2e.gameserver.data.parser.NpcsParser;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.instancemanager.ZoneManager;
import l2e.gameserver.model.Clan;
import l2e.gameserver.model.ClanMember;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.SiegeClan;
import l2e.gameserver.model.SiegeClan.SiegeClanType;
import l2e.gameserver.model.TeleportWhereType;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.entity.clanhall.ClanHallSiegeEngine;
import l2e.gameserver.model.entity.clanhall.SiegeStatus;
import l2e.gameserver.model.spawn.Spawner;
import l2e.gameserver.model.zone.type.ResidenceHallTeleportZone;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;
import l2e.gameserver.network.serverpackets.SystemMessage;

/**
 * Rework by LordWinter 31.05.2013 Based on L2J Eternity-World
 */
public final class BanditStrongHoldSiege extends ClanHallSiegeEngine
{
	private static String qn = "BanditStrongHoldSiege";

	private static final String SQL_LOAD_ATTACKERS = "SELECT * FROM siegable_hall_flagwar_attackers WHERE hall_id = ?";
	private static final String SQL_SAVE_ATTACKER = "INSERT INTO siegable_hall_flagwar_attackers_members VALUES (?,?,?)";
	private static final String SQL_LOAD_MEMEBERS = "SELECT object_id FROM siegable_hall_flagwar_attackers_members WHERE clan_id = ?";
	private static final String SQL_SAVE_CLAN = "INSERT INTO siegable_hall_flagwar_attackers VALUES(?,?,?,?)";
	private static final String SQL_SAVE_NPC = "UPDATE siegable_hall_flagwar_attackers SET npc = ? WHERE clan_id = ?";
	private static final String SQL_CLEAR_CLAN = "DELETE FROM siegable_hall_flagwar_attackers WHERE hall_id = ?";
	private static final String SQL_CLEAR_CLAN_ATTACKERS = "DELETE FROM siegable_hall_flagwar_attackers_members WHERE hall_id = ?";

	private static int ROYAL_FLAG = 35422;
	private static int FLAG_RED = 35423;

	private static int ALLY_1 = 35428;
	private static int ALLY_2 = 35429;
	private static int ALLY_3 = 35430;
	private static int ALLY_4 = 35431;
	private static int ALLY_5 = 35432;

	private static int TELEPORT_1 = 35560;

	private static int MESSENGER = 35437;

	protected static int[] OUTTER_DOORS_TO_OPEN = new int[2];
	protected static int[] INNER_DOORS_TO_OPEN = new int[2];
	private static Location[] FLAG_COORDS = new Location[7];

	private static ResidenceHallTeleportZone[] TELE_ZONES = new ResidenceHallTeleportZone[6];

	private static int QUEST_REWARD = 5009;
	private static int TARLK_AMULET = 4332;

	private static Location CENTER = new Location(82882, -16280, -1894, 0);

	protected static Map<Integer, ClanData> _data = new HashMap<>();
	private Clan _winner;
	private boolean _firstPhase;

	static
	{
		FLAG_COORDS[0] = new Location(83699, -17468, -1774, 19048);
		FLAG_COORDS[1] = new Location(82053, -17060, -1784, 5432);
		FLAG_COORDS[2] = new Location(82142, -15528, -1799, 58792);
		FLAG_COORDS[3] = new Location(83544, -15266, -1770, 44976);
		FLAG_COORDS[4] = new Location(84609, -16041, -1769, 35816);
		FLAG_COORDS[5] = new Location(81981, -15708, -1858, 60392);
		FLAG_COORDS[6] = new Location(84375, -17060, -1860, 27712);

		OUTTER_DOORS_TO_OPEN[0] = 22170001;
		OUTTER_DOORS_TO_OPEN[1] = 22170002;

		INNER_DOORS_TO_OPEN[0] = 22170003;
		INNER_DOORS_TO_OPEN[1] = 22170004;

		final Collection<ResidenceHallTeleportZone> zoneList = ZoneManager.getInstance().getAllZones(ResidenceHallTeleportZone.class);

		for (final ResidenceHallTeleportZone teleZone : zoneList)
		{
			if (teleZone.getResidenceId() != BANDIT_STRONGHOLD)
			{
				continue;
			}

			final int id = teleZone.getResidenceZoneId();

			if ((id < 0) || (id >= 6))
			{
				continue;
			}

			TELE_ZONES[id] = teleZone;
		}
	}

	public BanditStrongHoldSiege(int questId, String name, String descr, int hallId)
	{
		super(questId, name, descr, hallId);

		addStartNpc(MESSENGER);
		addFirstTalkId(MESSENGER);
		addTalkId(MESSENGER);

		for (int i = 0; i < 6; i++)
		{
			addFirstTalkId(TELEPORT_1 + i);
		}

		addKillId(ALLY_1);
		addKillId(ALLY_2);
		addKillId(ALLY_3);
		addKillId(ALLY_4);
		addKillId(ALLY_5);

		addSpawnId(ALLY_1);
		addSpawnId(ALLY_2);
		addSpawnId(ALLY_3);
		addSpawnId(ALLY_4);
		addSpawnId(ALLY_5);

		_winner = ClanHolder.getInstance().getClan(_hall.getOwnerId());
	}

	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		if (player.getQuestState(qn) == null)
		{
			newQuestState(player);
		}

		if (npc.getId() == MESSENGER)
		{
			if (!checkIsAttacker(player.getClan()))
			{
				final Clan clan = ClanHolder.getInstance().getClan(_hall.getOwnerId());
				final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
				html.setFile(player, player.getLang(), "data/html/scripts/clanhallsiege/" + getName() + "/35437-00.htm");
				html.replace("%clanName%", clan == null ? "no owner" : clan.getName());
				player.sendPacket(html);
			}
			else
			{
				return "35437-01.htm";
			}
		}
		else
		{
			final int index = npc.getId() - TELEPORT_1;
			if ((index == 0) && _firstPhase)
			{
				return "35560-00.htm";
			}
			TELE_ZONES[index].checkTeleporTask();
			return "35560-01.htm";
		}
		return "";
	}

	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		final Clan clan = player.getClan();

		if (event.startsWith("Register"))
		{
			if (!_hall.isRegistering())
			{
				if (_hall.isInSiege())
				{
					htmltext = "35437-02.htm";
				}
				else
				{
					sendRegistrationPageDate(player);
					return null;
				}
			}
			else if ((clan == null) || !player.isClanLeader())
			{
				htmltext = "35437-03.htm";
			}
			else if (getAttackers().size() >= 5)
			{
				htmltext = "35437-04.htm";
			}
			else if (checkIsAttacker(clan))
			{
				htmltext = "35437-05.htm";
			}
			else if (_hall.getOwnerId() == clan.getId())
			{
				htmltext = "35437-06.htm";
			}
			else
			{
				final String[] arg = event.split(" ");
				if (arg.length >= 2)
				{
					if (arg[1].equals("wQuest"))
					{
						if (player.destroyItemByItemId("Bandit Strong Hall Siege", QUEST_REWARD, 1, player, true))
						{
							registerClan(clan);
							htmltext = getFlagHtml(_data.get(clan.getId()).flag);
						}
						else
						{
							htmltext = "35437-07.htm";
						}
					}
					else if (arg[1].equals("wFee") && canPayRegistration())
					{
						if (player.reduceAdena(qn + " Siege", 200000, player, true))
						{
							registerClan(clan);
							htmltext = getFlagHtml(_data.get(clan.getId()).flag);
						}
						else
						{
							htmltext = "35437-08.htm";
						}
					}
				}
			}
		}
		else if (event.startsWith("Select_NPC"))
		{
			if (!player.isClanLeader())
			{
				htmltext = "35437-09.htm";
			}
			else if (!_data.containsKey(clan.getId()))
			{
				htmltext = "35437-10.htm";
			}
			else
			{
				final String[] var = event.split(" ");
				if (var.length >= 2)
				{
					int id = 0;
					try
					{
						id = Integer.parseInt(var[1]);
					}
					catch (final Exception e)
					{
						_log.warn(qn + "->select_clan_npc->Wrong mahum warrior id: " + var[1]);
					}
					if ((id > 0) && ((htmltext = getAllyHtml(id)) != null))
					{
						_data.get(clan.getId()).npc = id;
						saveNpc(id, clan.getId());
					}
				}
				else
				{
					_log.warn(qn + " Siege: Not enough parameters to save clan npc for clan: " + clan.getName());
				}
			}
		}
		else if (event.startsWith("View"))
		{
			ClanData cd = null;
			if (clan == null)
			{
				htmltext = "35437-10.htm";
			}
			else if ((cd = _data.get(clan.getId())) == null)
			{
				htmltext = "35437-03.htm";
			}
			else if (cd.npc == 0)
			{
				htmltext = "35437-11.htm";
			}
			else
			{
				htmltext = getAllyHtml(cd.npc);
			}
		}
		else if (event.startsWith("RegisterMember"))
		{
			if (clan == null)
			{
				htmltext = "35437-10.htm";
			}
			else if (!_hall.isRegistering())
			{
				htmltext = "35437-02.htm";
			}
			else if (!_data.containsKey(clan.getId()))
			{
				htmltext = "35437-03.htm";
			}
			else if (_data.get(clan.getId()).players.size() >= 18)
			{
				htmltext = "35437-12.htm";
			}
			else
			{
				final ClanData data = _data.get(clan.getId());
				data.players.add(player.getObjectId());
				saveMember(clan.getId(), player.getObjectId());
				if (data.npc == 0)
				{
					htmltext = "35437-11.htm";
				}
				else
				{
					htmltext = "35437-05.htm";
				}
			}
		}
		else if (event.startsWith("Attackers"))
		{
			if (_hall.isRegistering())
			{
				sendRegistrationPageDate(player);
				return null;
			}

			htmltext = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/scripts/clanhallsiege/" + getName() + "/35437-13.htm");
			int i = 1;
			for (final Entry<Integer, ClanData> clanData : _data.entrySet())
			{
				final Clan attacker = ClanHolder.getInstance().getClan(clanData.getKey());
				if (attacker == null)
				{
					continue;
				}
				htmltext = htmltext.replaceAll("%clan" + i + "%", clan.getName());
				htmltext = htmltext.replaceAll("%clanMem" + i + "%", String.valueOf(clanData.getValue().players.size()));
				i++;
			}
			if (_data.size() < 5)
			{
				for (int c = i; c < 5; c++)
				{
					htmltext = htmltext.replaceAll("%clan" + c + "%", "Empty pos.");
					htmltext = htmltext.replaceAll("%clanMem" + c + "%", String.valueOf(0));
				}
			}
		}
		else if (event.startsWith("CheckQuest"))
		{
			if ((clan == null) || (clan.getLevel() < 4))
			{
				htmltext = "35437-22.htm";
			}
			else if (!player.isClanLeader())
			{
				htmltext = "35437-23.htm";
			}
			else if ((clan.getHideoutId() > 0) || (clan.getFortId() > 0) || (clan.getCastleId() > 0))
			{
				htmltext = "35437-24.htm";
			}
			else if (!_hall.isWaitingBattle())
			{
				sendRegistrationPageDate(player);
				return null;
			}
			else if (player.getInventory().getItemByItemId(QUEST_REWARD) != null)
			{
				htmltext = "35437-25.htm";
			}
			else
			{
				if (player.getInventory().getInventoryItemCount(TARLK_AMULET, -1) >= 30)
				{
					htmltext = "35437-21a.htm";
				}
				else
				{
					htmltext = "35437-21.htm";
				}
			}
		}
		return htmltext;
	}

	@Override
	public String onKill(Npc npc, Player killer, boolean isSummon)
	{
		if (_hall.isInSiege())
		{
			final int npcId = npc.getId();
			for (final int keys : _data.keySet())
			{
				if (_data.get(keys).npc == npcId)
				{
					removeParticipant(keys, true);
				}
			}

			synchronized (this)
			{
				final List<Integer> clanIds = new ArrayList<>(_data.keySet());
				if (_firstPhase)
				{
					if (((clanIds.size() == 1) && (_hall.getOwnerId() <= 0)) || (_data.get(clanIds.get(0)).npc == 0))
					{
						_missionAccomplished = true;
						cancelSiegeTask();
						endSiege();
					}
					else if ((_data.size() == 2) && (_hall.getOwnerId() > 0))
					{
						cancelSiegeTask();
						_firstPhase = false;
						_hall.getSiegeZone().setIsActive(false);
						for (final int doorId : INNER_DOORS_TO_OPEN)
						{
							_hall.openCloseDoor(doorId, true);
						}

						for (final ClanData data : _data.values())
						{
							doUnSpawns(data);
						}

						ThreadPoolManager.getInstance().schedule(new Runnable()
						{
							@Override
							public void run()
							{
								for (final int doorId : INNER_DOORS_TO_OPEN)
								{
									_hall.openCloseDoor(doorId, false);
								}

								for (final Entry<Integer, ClanData> e : _data.entrySet())
								{
									doSpawns(e.getKey(), e.getValue());
								}
								_hall.getSiegeZone().setIsActive(true);
							}
						}, 300000);
					}
				}
				else
				{
					_missionAccomplished = true;
					_winner = ClanHolder.getInstance().getClan(clanIds.get(0));
					removeParticipant(clanIds.get(0), false);
					endSiege();
				}
			}
		}
		return null;
	}

	@Override
	public String onSpawn(Npc npc)
	{
		npc.getAI().setIntention(CtrlIntention.MOVING, CENTER, 0);
		return null;
	}

	@Override
	public Clan getWinner()
	{
		return _winner;
	}

	@Override
	public void prepareOwner()
	{
		if (_hall.getOwnerId() > 0)
		{
			registerClan(ClanHolder.getInstance().getClan(_hall.getOwnerId()));
		}

		_hall.banishForeigners();
		final SystemMessage msg = SystemMessage.getSystemMessage(SystemMessageId.REGISTRATION_TERM_FOR_S1_ENDED);
		msg.addString(Util.clanHallName(null, _hall.getId()));
		Announcements.getInstance().announceToAll(msg);
		_hall.updateSiegeStatus(SiegeStatus.WAITING_BATTLE);

		_siegeTask = ThreadPoolManager.getInstance().schedule(new SiegeStarts(), 3600000);
	}

	@Override
	public void startSiege()
	{
		if (getAttackers().size() < 2)
		{
			onSiegeEnds();
			getAttackers().clear();
			_hall.updateNextSiege();
			final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.SIEGE_OF_S1_HAS_BEEN_CANCELED_DUE_TO_LACK_OF_INTEREST);
			sm.addString(Util.clanHallName(null, _hall.getId()));
			Announcements.getInstance().announceToAll(sm);
			return;
		}

		for (final int door : OUTTER_DOORS_TO_OPEN)
		{
			_hall.openCloseDoor(door, true);
		}

		if (_hall.getOwnerId() > 0)
		{
			final Clan owner = ClanHolder.getInstance().getClan(_hall.getOwnerId());
			final Location loc = _hall.getZone().getSpawns().get(0);
			for (final ClanMember pc : owner.getMembers())
			{
				if (pc != null)
				{
					final Player player = pc.getPlayerInstance();
					if ((player != null) && player.isOnline())
					{
						player.teleToLocation(loc, false, player.getReflection());
					}
				}
			}
		}

		ThreadPoolManager.getInstance().schedule(new Runnable()
		{
			@SuppressWarnings("synthetic-access")
			@Override
			public void run()
			{
				for (final int door : OUTTER_DOORS_TO_OPEN)
				{
					_hall.openCloseDoor(door, false);
				}

				_hall.getZone().banishNonSiegeParticipants();

				BanditStrongHoldSiege.super.startSiege();
			}
		}, 300000);
	}

	@Override
	public void onSiegeStarts()
	{
		for (final Entry<Integer, ClanData> clan : _data.entrySet())
		{
			try
			{
				final ClanData data = clan.getValue();
				doSpawns(clan.getKey(), data);
				fillPlayerList(data);
			}
			catch (final Exception e)
			{
				endSiege();
				_log.warn(qn + ": Problems in siege initialization!");
				e.printStackTrace();
			}
		}
	}

	@Override
	public void endSiege()
	{
		if (_hall.getOwnerId() > 0)
		{
			final Clan clan = ClanHolder.getInstance().getClan(_hall.getOwnerId());
			clan.setHideoutId(0);
			_hall.free();
		}
		super.endSiege();
	}

	@Override
	public void onSiegeEnds()
	{
		if (_data.size() > 0)
		{
			for (final int clanId : _data.keySet())
			{
				if (_hall.getOwnerId() == clanId)
				{
					removeParticipant(clanId, false);
				}
				else
				{
					removeParticipant(clanId, true);
				}
			}
		}
		clearTables();
	}

	@Override
	public final Location getInnerSpawnLoc(final Player player)
	{
		Location loc = null;
		if (player.getClanId() == _hall.getOwnerId())
		{
			loc = _hall.getZone().getSpawns().get(0);
		}
		else
		{
			final ClanData cd = _data.get(player.getClanId());
			if (cd != null)
			{
				final int index = cd.flag - FLAG_RED;
				if ((index >= 0) && (index <= 4))
				{
					loc = _hall.getZone().getChallengerSpawns().get(index);
				}
				else
				{
					throw new ArrayIndexOutOfBoundsException();
				}
			}
		}
		return loc;
	}

	@Override
	public final boolean canPlantFlag()
	{
		return false;
	}

	@Override
	public final boolean doorIsAutoAttackable()
	{
		return false;
	}

	void doSpawns(int clanId, ClanData data)
	{
		try
		{
			final NpcTemplate mahumTemplate = NpcsParser.getInstance().getTemplate(data.npc);
			final NpcTemplate flagTemplate = NpcsParser.getInstance().getTemplate(data.flag);

			if (flagTemplate == null)
			{
				_log.warn(qn + ": Flag L2NpcTemplate[" + data.flag + "] does not exist!");
				throw new NullPointerException();
			}
			else if (mahumTemplate == null)
			{
				_log.warn(qn + ": Ally L2NpcTemplate[" + data.npc + "] does not exist!");
				throw new NullPointerException();
			}

			int index = 0;
			if (_firstPhase)
			{
				index = data.flag - FLAG_RED;
			}
			else
			{
				index = clanId == _hall.getOwnerId() ? 5 : 6;
			}
			final Location loc = FLAG_COORDS[index];

			data.flagInstance = new Spawner(flagTemplate);
			data.flagInstance.setLocation(loc);
			data.flagInstance.setRespawnDelay(10000);
			data.flagInstance.setAmount(1);
			data.flagInstance.init();

			data.warrior = new Spawner(mahumTemplate);
			data.warrior.setLocation(loc);
			data.warrior.setRespawnDelay(10000);
			data.warrior.setAmount(1);
			data.warrior.init();
			((SpecialGuardAI) data.warrior.getLastSpawn().getAI()).getAlly().addAll(data.players);
		}
		catch (final Exception e)
		{
			_log.warn(qn + ": Couldnt make clan spawns: " + e.getMessage());
			e.printStackTrace();
		}
	}

	private void fillPlayerList(ClanData data)
	{
		for (final int objId : data.players)
		{
			final Player plr = GameObjectsStorage.getPlayer(objId);
			if (plr != null)
			{
				data.playersInstance.add(plr);
			}
		}
	}

	private void registerClan(Clan clan)
	{
		final int clanId = clan.getId();

		final SiegeClan sc = new SiegeClan(clanId, SiegeClanType.ATTACKER);
		getAttackers().put(clanId, sc);

		final ClanData data = new ClanData();
		data.flag = ROYAL_FLAG + _data.size();
		data.players.add(clan.getLeaderId());
		_data.put(clanId, data);

		saveClan(clanId, data.flag);
		saveMember(clanId, clan.getLeaderId());
	}

	private final void doUnSpawns(ClanData data)
	{
		if (data.flagInstance != null)
		{
			data.flagInstance.stopRespawn();
			data.flagInstance.getLastSpawn().deleteMe();
		}
		if (data.warrior != null)
		{
			data.warrior.stopRespawn();
			data.warrior.getLastSpawn().deleteMe();
		}
	}

	private final void removeParticipant(int clanId, boolean teleport)
	{
		final ClanData dat = _data.remove(clanId);

		if (dat != null)
		{
			if (dat.flagInstance != null)
			{
				dat.flagInstance.stopRespawn();
				if (dat.flagInstance.getLastSpawn() != null)
				{
					dat.flagInstance.getLastSpawn().deleteMe();
				}
			}

			if (dat.warrior != null)
			{
				dat.warrior.stopRespawn();
				if (dat.warrior.getLastSpawn() != null)
				{
					dat.warrior.getLastSpawn().deleteMe();
				}
			}

			dat.players.clear();

			if (teleport)
			{
				for (final Player pc : dat.playersInstance)
				{
					if (pc != null)
					{
						pc.teleToLocation(TeleportWhereType.TOWN, true, pc.getReflection());
					}
				}
			}
			dat.playersInstance.clear();
		}
	}

	public boolean canPayRegistration()
	{
		return true;
	}

	private void sendRegistrationPageDate(Player player)
	{
		final NpcHtmlMessage msg = new NpcHtmlMessage(5);
		msg.setFile(player, player.getLang(), "data/html/scripts/clanhallsiege/" + qn + "/35437-14.htm");
		msg.replace("%nextSiege%", _hall.getSiegeDate().getTime().toString());
		player.sendPacket(msg);
	}

	private String getFlagHtml(int flag)
	{
		String result = "35437-14a.htm";

		switch (flag)
		{
			case 35423 :
				result = "35437-15.htm";
				break;
			case 35424 :
				result = "35437-16.htm";
				break;
			case 35425 :
				result = "35437-17.htm";
				break;
			case 35426 :
				result = "35437-18.htm";
				break;
			case 35427 :
				result = "35437-19.htm";
				break;
		}
		return result;
	}

	private String getAllyHtml(int ally)
	{
		String result = null;

		switch (ally)
		{
			case 35428 :
				result = "35437-15a.htm";
				break;
			case 35429 :
				result = "35437-16a.htm";
				break;
			case 35430 :
				result = "35437-17a.htm";
				break;
			case 35431 :
				result = "35437-18a.htm";
				break;
			case 35432 :
				result = "35437-19a.htm";
				break;
		}
		return result;
	}

	@Override
	public final void loadAttackers()
	{
		try (
		    Connection con = DatabaseFactory.getInstance().getConnection())
		{
			final PreparedStatement statement = con.prepareStatement(SQL_LOAD_ATTACKERS);
			statement.setInt(1, _hall.getId());
			final ResultSet rset = statement.executeQuery();
			while (rset.next())
			{
				final int clanId = rset.getInt("clan_id");

				if (ClanHolder.getInstance().getClan(clanId) == null)
				{
					_log.warn(qn + ": Loaded an unexistent clan as attacker! Clan Id: " + clanId);
					continue;
				}

				final int flag = rset.getInt("flag");
				final int npc = rset.getInt("npc");

				final SiegeClan sc = new SiegeClan(clanId, SiegeClanType.ATTACKER);
				getAttackers().put(clanId, sc);

				final ClanData data = new ClanData();
				data.flag = flag;
				data.npc = npc;
				_data.put(clanId, data);

				loadAttackerMembers(clanId);
			}
			rset.close();
			statement.close();
		}
		catch (final Exception e)
		{
			_log.warn(qn + ".loadAttackers()->" + e.getMessage());
			e.printStackTrace();
		}
	}

	private final void loadAttackerMembers(int clanId)
	{
		try (
		    Connection con = DatabaseFactory.getInstance().getConnection())
		{
			final ArrayList<Integer> listInstance = _data.get(clanId).players;

			if (listInstance == null)
			{
				_log.warn(qn + ": Tried to load unregistered clan: " + clanId + "[clan Id]");
				return;
			}

			final PreparedStatement statement = con.prepareStatement(SQL_LOAD_MEMEBERS);
			statement.setInt(1, clanId);
			final ResultSet rset = statement.executeQuery();
			while (rset.next())
			{
				listInstance.add(rset.getInt("object_id"));

			}
			rset.close();
			statement.close();
		}
		catch (final Exception e)
		{
			_log.warn(qn + ".loadAttackerMembers()->" + e.getMessage());
			e.printStackTrace();
		}
	}

	private final void saveClan(int clanId, int flag)
	{
		try (
		    Connection con = DatabaseFactory.getInstance().getConnection())
		{
			final PreparedStatement statement = con.prepareStatement(SQL_SAVE_CLAN);
			statement.setInt(1, _hall.getId());
			statement.setInt(2, flag);
			statement.setInt(3, 0);
			statement.setInt(4, clanId);
			statement.execute();
			statement.close();
		}
		catch (final Exception e)
		{
			_log.warn(qn + ".saveClan()->" + e.getMessage());
			e.printStackTrace();
		}
	}

	private final void saveNpc(int npc, int clanId)
	{
		try (
		    Connection con = DatabaseFactory.getInstance().getConnection())
		{
			final PreparedStatement statement = con.prepareStatement(SQL_SAVE_NPC);
			statement.setInt(1, npc);
			statement.setInt(2, clanId);
			statement.execute();
			statement.close();
		}
		catch (final Exception e)
		{
			_log.warn(qn + ".saveNpc()->" + e.getMessage());
			e.printStackTrace();
		}
	}

	private final void saveMember(int clanId, int objectId)
	{
		try (
		    Connection con = DatabaseFactory.getInstance().getConnection())
		{
			final PreparedStatement statement = con.prepareStatement(SQL_SAVE_ATTACKER);
			statement.setInt(1, _hall.getId());
			statement.setInt(2, clanId);
			statement.setInt(3, objectId);
			statement.execute();
			statement.close();
		}
		catch (final Exception e)
		{
			_log.warn(qn + ".saveMember()->" + e.getMessage());
			e.printStackTrace();
		}
	}

	private void clearTables()
	{
		try (
		    Connection con = DatabaseFactory.getInstance().getConnection())
		{
			final PreparedStatement stat1 = con.prepareStatement(SQL_CLEAR_CLAN);
			stat1.setInt(1, _hall.getId());
			stat1.execute();
			stat1.close();

			final PreparedStatement stat2 = con.prepareStatement(SQL_CLEAR_CLAN_ATTACKERS);
			stat2.setInt(1, _hall.getId());
			stat2.execute();
			stat2.close();
		}
		catch (final Exception e)
		{
			_log.warn(qn + ".clearTables()->" + e.getMessage());
		}
	}

	protected final class ClanData
	{
		int flag = 0;
		int npc = 0;
		ArrayList<Integer> players = new ArrayList<>(18);
		ArrayList<Player> playersInstance = new ArrayList<>(18);
		Spawner warrior = null;
		Spawner flagInstance = null;
	}

	public static void main(String[] args)
	{
		new BanditStrongHoldSiege(-1, qn, "clanhallsiege", BANDIT_STRONGHOLD);
	}
}
