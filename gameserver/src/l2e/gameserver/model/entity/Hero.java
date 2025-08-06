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
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2e.commons.dbutils.DbUtils;
import l2e.commons.util.StringUtil;
import l2e.gameserver.Config;
import l2e.gameserver.data.holder.CharNameHolder;
import l2e.gameserver.data.holder.ClanHolder;
import l2e.gameserver.data.htm.HtmCache;
import l2e.gameserver.data.parser.ClassListParser;
import l2e.gameserver.data.parser.NpcsParser;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.instancemanager.CastleManager;
import l2e.gameserver.model.Clan;
import l2e.gameserver.model.ClanMember;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.items.itemcontainer.Inventory;
import l2e.gameserver.model.olympiad.Olympiad;
import l2e.gameserver.model.stats.StatsSet;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.InventoryUpdate;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;
import l2e.gameserver.network.serverpackets.SocialAction;
import l2e.gameserver.network.serverpackets.SystemMessage;

public class Hero
{
	private static final Logger _log = LoggerFactory.getLogger(Hero.class);
	
	private static final String GET_HEROES = "SELECT heroes.charId, characters.char_name, heroes.class_id, heroes.count, heroes.played, heroes.active FROM heroes, characters WHERE characters.charId = heroes.charId AND heroes.played = 1";
	private static final String GET_ALL_HEROES = "SELECT heroes.charId, characters.char_name, heroes.class_id, heroes.count, heroes.played, heroes.active FROM heroes, characters WHERE characters.charId = heroes.charId";
	private static final String UPDATE_ALL = "UPDATE heroes SET played = 0, active = 0";
	private static final String INSERT_HERO = "INSERT INTO heroes (charId, class_id, count, played, active) VALUES (?,?,?,?,?)";
	private static final String UPDATE_HERO = "UPDATE heroes SET count = ?, played = ?, active = ? WHERE charId = ?";
	private static final String GET_CLAN_ALLY = "SELECT characters.clanid AS clanid, coalesce(clan_data.ally_Id, 0) AS allyId FROM characters LEFT JOIN clan_data ON clan_data.clan_id = characters.clanid WHERE characters.charId = ?";
	private static final String GET_CLAN_NAME = "SELECT clan_name FROM clan_data WHERE clan_id = (SELECT clanid FROM characters WHERE charId = ?)";
	
	private static final String DELETE_ITEMS = "DELETE FROM items WHERE item_id IN (6842, 6611, 6612, 6613, 6614, 6615, 6616, 6617, 6618, 6619, 6620, 6621, 9388, 9389, 9390) AND owner_id NOT IN (SELECT charId FROM characters WHERE accesslevel > 0)";

	private static final Map<Integer, StatsSet> _heroes = new ConcurrentHashMap<>();
	private static final Map<Integer, StatsSet> _completeHeroes = new ConcurrentHashMap<>();

	private static final Map<Integer, StatsSet> _herocounts = new ConcurrentHashMap<>();
	private static final Map<Integer, List<StatsSet>> _herofights = new ConcurrentHashMap<>();

	private static final Map<Integer, List<StatsSet>> _herodiary = new ConcurrentHashMap<>();
	private static final Map<Integer, String> _heroMessage = new ConcurrentHashMap<>();

	public static final String COUNT = "count";
	public static final String PLAYED = "played";
	public static final String ACTIVE = "active";
	public static final String CLAN_NAME = "clan_name";
	public static final String CLAN_CREST = "clan_crest";
	public static final String ALLY_NAME = "ally_name";
	public static final String ALLY_CREST = "ally_crest";

	public static final int ACTION_RAID_KILLED = 1;
	public static final int ACTION_HERO_GAINED = 2;
	public static final int ACTION_CASTLE_TAKEN = 3;

	public static Hero getInstance()
	{
		return SingletonHolder._instance;
	}

	protected Hero()
	{
		init();
	}

	private void init()
	{
		_heroes.clear();
		_completeHeroes.clear();
		_herocounts.clear();
		_herofights.clear();
		_herodiary.clear();
		_heroMessage.clear();

		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(GET_HEROES);
			rset = statement.executeQuery();
			final var statement2 = con.prepareStatement(GET_CLAN_ALLY);
			while (rset.next())
			{
				final StatsSet hero = new StatsSet();
				final int charId = rset.getInt(Olympiad.CHAR_ID);
				hero.set(Olympiad.CHAR_NAME, rset.getString(Olympiad.CHAR_NAME));
				hero.set(Olympiad.CLASS_ID, rset.getInt(Olympiad.CLASS_ID));
				hero.set(COUNT, rset.getInt(COUNT));
				hero.set(PLAYED, rset.getInt(PLAYED));
				hero.set(ACTIVE, rset.getInt(ACTIVE));
				loadFights(charId);
				loadDiary(charId);
				loadMessage(charId);

				processHeros(statement2, charId, hero);

				_heroes.put(charId, hero);
			}

			rset.close();
			statement.close();

			statement = con.prepareStatement(GET_ALL_HEROES);
			rset = statement.executeQuery();

			while (rset.next())
			{
				final StatsSet hero = new StatsSet();
				final int charId = rset.getInt(Olympiad.CHAR_ID);
				hero.set(Olympiad.CHAR_NAME, rset.getString(Olympiad.CHAR_NAME));
				hero.set(Olympiad.CLASS_ID, rset.getInt(Olympiad.CLASS_ID));
				hero.set(COUNT, rset.getInt(COUNT));
				hero.set(PLAYED, rset.getInt(PLAYED));
				hero.set(ACTIVE, rset.getInt(ACTIVE));
				processHeros(statement2, charId, hero);

				_completeHeroes.put(charId, hero);
			}
			statement2.close();
		}
		catch (final SQLException e)
		{
			_log.warn("Hero System: Couldnt load Heroes", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
		_log.info(getClass().getSimpleName() + ": Loaded " + _heroes.size() + " heroes.");
		_log.info(getClass().getSimpleName() + ": Loaded " + _completeHeroes.size() + " all time heroes.");
	}

	private void processHeros(PreparedStatement ps, int charId, StatsSet hero) throws SQLException
	{
		ps.setInt(1, charId);
		final ResultSet rs = ps.executeQuery();
		if (rs.next())
		{
			final int clanId = rs.getInt("clanid");
			final int allyId = rs.getInt("allyId");
			String clanName = "";
			String allyName = "";
			int clanCrest = 0;
			int allyCrest = 0;
			if (clanId > 0)
			{
				clanName = ClanHolder.getInstance().getClan(clanId).getName();
				clanCrest = ClanHolder.getInstance().getClan(clanId).getCrestId();
				if (allyId > 0)
				{
					allyName = ClanHolder.getInstance().getClan(clanId).getAllyName();
					allyCrest = ClanHolder.getInstance().getClan(clanId).getAllyCrestId();
				}
			}
			hero.set(CLAN_CREST, clanCrest);
			hero.set(CLAN_NAME, clanName);
			hero.set(ALLY_CREST, allyCrest);
			hero.set(ALLY_NAME, allyName);
		}
		rs.close();
		ps.clearParameters();
	}

	private String calcFightTime(long FightTime)
	{
		final String format = String.format("%%0%dd", 2);
		FightTime = FightTime / 1000;
		final String seconds = String.format(format, FightTime % 60);
		final String minutes = String.format(format, (FightTime % 3600) / 60);
		final String time = minutes + ":" + seconds;
		return time;
	}

	public void loadMessage(int charId)
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			
			String message = null;
			statement = con.prepareStatement("SELECT message FROM heroes WHERE charId=?");
			statement.setInt(1, charId);
			rset = statement.executeQuery();
			rset.next();
			message = rset.getString("message");
			_heroMessage.put(charId, message);
		}
		catch (final SQLException e)
		{
			_log.warn("Hero System: Couldnt load Hero Message for CharId: " + charId, e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
	}

	public void loadDiary(int charId)
	{
		final List<StatsSet> _diary = new ArrayList<>();
		int diaryentries = 0;
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT * FROM  heroes_diary WHERE charId=? ORDER BY time ASC");
			statement.setInt(1, charId);
			rset = statement.executeQuery();
			while (rset.next())
			{
				final StatsSet _diaryentry = new StatsSet();

				final long time = rset.getLong("time");
				final int action = rset.getInt("action");
				final int param = rset.getInt("param");

				final String date = (new SimpleDateFormat("yyyy-MM-dd HH")).format(new Date(time));
				_diaryentry.set("date", date);

				if (action == ACTION_RAID_KILLED)
				{
					final NpcTemplate template = NpcsParser.getInstance().getTemplate(param);
					if (template != null)
					{
						_diaryentry.set("action", template.getName(null) + " was defeated");
					}
				}
				else if (action == ACTION_HERO_GAINED)
				{
					_diaryentry.set("action", "Gained Hero status");
				}
				else if (action == ACTION_CASTLE_TAKEN)
				{
					final Castle castle = CastleManager.getInstance().getCastleById(param);
					if (castle != null)
					{
						_diaryentry.set("action", castle.getName(null) + " Castle was successfuly taken");
					}
				}
				_diary.add(_diaryentry);
				diaryentries++;
			}

			_herodiary.put(charId, _diary);

			_log.info(getClass().getSimpleName() + ": Loaded " + diaryentries + " diary entries for hero: " + CharNameHolder.getInstance().getNameById(charId));
		}
		catch (final SQLException e)
		{
			_log.warn("Hero System: Couldnt load Hero Diary for CharId: " + charId, e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
	}

	public void loadFights(int charId)
	{
		final List<StatsSet> _fights = new ArrayList<>();
		final StatsSet _herocountdata = new StatsSet();
		final Calendar _data = Calendar.getInstance();
		_data.set(Calendar.DAY_OF_MONTH, 1);
		_data.set(Calendar.HOUR_OF_DAY, 0);
		_data.set(Calendar.MINUTE, 0);
		_data.set(Calendar.MILLISECOND, 0);

		final long from = _data.getTimeInMillis();
		int numberoffights = 0;
		int _victorys = 0;
		int _losses = 0;
		int _draws = 0;

		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT * FROM olympiad_fights WHERE (charOneId=? OR charTwoId=?) AND start<? ORDER BY start ASC");
			statement.setInt(1, charId);
			statement.setInt(2, charId);
			statement.setLong(3, from);
			rset = statement.executeQuery();

			int charOneId;
			int charOneClass;
			int charTwoId;
			int charTwoClass;
			int winner;
			long start;
			int time;
			int classed;
			while (rset.next())
			{
				charOneId = rset.getInt("charOneId");
				charOneClass = rset.getInt("charOneClass");
				charTwoId = rset.getInt("charTwoId");
				charTwoClass = rset.getInt("charTwoClass");
				winner = rset.getInt("winner");
				start = rset.getLong("start");
				time = rset.getInt("time");
				classed = rset.getInt("classed");

				if (charId == charOneId)
				{
					final String name = CharNameHolder.getInstance().getNameById(charTwoId);
					final String cls = ClassListParser.getInstance().getClass(charTwoClass).getClientCode();
					if ((name != null) && (cls != null))
					{
						final StatsSet fight = new StatsSet();
						fight.set("oponent", name);
						fight.set("oponentclass", cls);

						fight.set("time", calcFightTime(time));
						final String date = (new SimpleDateFormat("yyyy-MM-dd HH:mm")).format(new Date(start));
						fight.set("start", date);

						fight.set("classed", classed);
						if (winner == 1)
						{
							fight.set("result", "<font color=\"00ff00\">victory</font>");
							_victorys++;
						}
						else if (winner == 2)
						{
							fight.set("result", "<font color=\"ff0000\">loss</font>");
							_losses++;
						}
						else if (winner == 0)
						{
							fight.set("result", "<font color=\"ffff00\">draw</font>");
							_draws++;
						}

						_fights.add(fight);

						numberoffights++;
					}
				}
				else if (charId == charTwoId)
				{
					final String name = CharNameHolder.getInstance().getNameById(charOneId);
					final String cls = ClassListParser.getInstance().getClass(charOneClass).getClientCode();
					if ((name != null) && (cls != null))
					{
						final StatsSet fight = new StatsSet();
						fight.set("oponent", name);
						fight.set("oponentclass", cls);

						fight.set("time", calcFightTime(time));
						final String date = (new SimpleDateFormat("yyyy-MM-dd HH:mm")).format(new Date(start));
						fight.set("start", date);

						fight.set("classed", classed);
						if (winner == 1)
						{
							fight.set("result", "<font color=\"ff0000\">loss</font>");
							_losses++;
						}
						else if (winner == 2)
						{
							fight.set("result", "<font color=\"00ff00\">victory</font>");
							_victorys++;
						}
						else if (winner == 0)
						{
							fight.set("result", "<font color=\"ffff00\">draw</font>");
							_draws++;
						}

						_fights.add(fight);

						numberoffights++;
					}
				}
			}

			_herocountdata.set("victory", _victorys);
			_herocountdata.set("draw", _draws);
			_herocountdata.set("loss", _losses);

			_herocounts.put(charId, _herocountdata);
			_herofights.put(charId, _fights);

			_log.info(getClass().getSimpleName() + ": Loaded " + numberoffights + " fights for hero: " + CharNameHolder.getInstance().getNameById(charId));
		}
		catch (final SQLException e)
		{
			_log.warn("Hero System: Couldnt load Hero fights history for CharId: " + charId, e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
	}

	public Map<Integer, StatsSet> getHeroes()
	{
		return _heroes;
	}

	public int getHeroByClass(int classid)
	{
		for (final Entry<Integer, StatsSet> e : _heroes.entrySet())
		{
			if (e.getValue().getInteger(Olympiad.CLASS_ID) == classid)
			{
				return e.getKey();
			}
		}
		return 0;
	}

	public void resetData()
	{
		_herodiary.clear();
		_herofights.clear();
		_herocounts.clear();
		_heroMessage.clear();
	}

	public void showHeroDiary(Player activeChar, int heroclass, int charid, int page)
	{
		final int perpage = 10;

		if (_herodiary.containsKey(charid))
		{
			final List<StatsSet> _mainlist = _herodiary.get(charid);
			final NpcHtmlMessage DiaryReply = new NpcHtmlMessage(5);
			final String htmContent = HtmCache.getInstance().getHtm(activeChar, activeChar.getLang(), "data/html/olympiad/herodiary.htm");
			if ((htmContent != null) && _heroMessage.containsKey(charid))
			{
				DiaryReply.setHtml(activeChar, htmContent);
				DiaryReply.replace("%heroname%", CharNameHolder.getInstance().getNameById(charid));
				DiaryReply.replace("%message%", _heroMessage.get(charid));

				if (!_mainlist.isEmpty())
				{
					final List<StatsSet> _list = new ArrayList<>(_mainlist);
					Collections.reverse(_list);

					boolean color = true;
					final StringBuilder fList = new StringBuilder(500);
					int counter = 0;
					int breakat = 0;
					for (int i = ((page - 1) * perpage); i < _list.size(); i++)
					{
						breakat = i;
						final StatsSet _diaryentry = _list.get(i);
						StringUtil.append(fList, "<tr><td>");
						if (color)
						{
							StringUtil.append(fList, "<table width=270 bgcolor=\"131210\">");
						}
						else
						{
							StringUtil.append(fList, "<table width=270>");
						}
						StringUtil.append(fList, "<tr><td width=270><font color=\"LEVEL\">" + _diaryentry.getString("date") + ":xx</font></td></tr>");
						StringUtil.append(fList, "<tr><td width=270>" + _diaryentry.getString("action") + "</td></tr>");
						StringUtil.append(fList, "<tr><td>&nbsp;</td></tr></table>");
						StringUtil.append(fList, "</td></tr>");
						color = !color;
						counter++;
						if (counter >= perpage)
						{
							break;
						}
					}

					if (breakat < (_list.size() - 1))
					{
						DiaryReply.replace("%buttprev%", "<button value=\"Prev\" action=\"bypass -h _diary?class=" + heroclass + "&page=" + (page + 1) + "\" width=60 height=25 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
					}
					else
					{
						DiaryReply.replace("%buttprev%", "");
					}

					if (page > 1)
					{
						DiaryReply.replace("%buttnext%", "<button value=\"Next\" action=\"bypass -h _diary?class=" + heroclass + "&page=" + (page - 1) + "\" width=60 height=25 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
					}
					else
					{
						DiaryReply.replace("%buttnext%", "");
					}
					DiaryReply.replace("%list%", fList.toString());
				}
				else
				{
					DiaryReply.replace("%list%", "");
					DiaryReply.replace("%buttprev%", "");
					DiaryReply.replace("%buttnext%", "");
				}

				activeChar.sendPacket(DiaryReply);
			}
		}
	}

	public void showHeroFights(Player activeChar, int heroclass, int charid, int page)
	{
		final int perpage = 20;
		int _win = 0;
		int _loss = 0;
		int _draw = 0;

		if (_herofights.containsKey(charid))
		{
			final List<StatsSet> _list = _herofights.get(charid);

			final NpcHtmlMessage FightReply = new NpcHtmlMessage(5);
			final String htmContent = HtmCache.getInstance().getHtm(activeChar, activeChar.getLang(), "data/html/olympiad/herohistory.htm");
			if (htmContent != null)
			{
				FightReply.setHtml(activeChar, htmContent);
				FightReply.replace("%heroname%", CharNameHolder.getInstance().getNameById(charid));

				if (!_list.isEmpty())
				{
					if (_herocounts.containsKey(charid))
					{
						final StatsSet _herocount = _herocounts.get(charid);
						_win = _herocount.getInteger("victory");
						_loss = _herocount.getInteger("loss");
						_draw = _herocount.getInteger("draw");
					}

					boolean color = true;
					final StringBuilder fList = new StringBuilder(500);
					int counter = 0;
					int breakat = 0;
					for (int i = ((page - 1) * perpage); i < _list.size(); i++)
					{
						breakat = i;
						final StatsSet fight = _list.get(i);
						StringUtil.append(fList, "<tr><td>");
						if (color)
						{
							StringUtil.append(fList, "<table width=270 bgcolor=\"131210\">");
						}
						else
						{
							StringUtil.append(fList, "<table width=270>");
						}
						StringUtil.append(fList, "<tr><td width=220><font color=\"LEVEL\">" + fight.getString("start") + "</font>&nbsp;&nbsp;" + fight.getString("result") + "</td><td width=50 align=right>" + (fight.getInteger("classed") > 0 ? "<font color=\"FFFF99\">cls</font>" : "<font color=\"999999\">non-cls<font>") + "</td></tr>");
						StringUtil.append(fList, "<tr><td width=220>vs " + fight.getString("oponent") + " (" + fight.getString("oponentclass") + ")</td><td width=50 align=right>(" + fight.getString("time") + ")</td></tr>");
						StringUtil.append(fList, "<tr><td colspan=2>&nbsp;</td></tr></table>");
						StringUtil.append(fList, "</td></tr>");
						color = !color;
						counter++;
						if (counter >= perpage)
						{
							break;
						}
					}

					if (breakat < (_list.size() - 1))
					{
						FightReply.replace("%buttprev%", "<button value=\"Prev\" action=\"bypass -h _match?class=" + heroclass + "&page=" + (page + 1) + "\" width=60 height=25 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
					}
					else
					{
						FightReply.replace("%buttprev%", "");
					}

					if (page > 1)
					{
						FightReply.replace("%buttnext%", "<button value=\"Next\" action=\"bypass -h _match?class=" + heroclass + "&page=" + (page - 1) + "\" width=60 height=25 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
					}
					else
					{
						FightReply.replace("%buttnext%", "");
					}

					FightReply.replace("%list%", fList.toString());
				}
				else
				{
					FightReply.replace("%list%", "");
					FightReply.replace("%buttprev%", "");
					FightReply.replace("%buttnext%", "");
				}

				FightReply.replace("%win%", String.valueOf(_win));
				FightReply.replace("%draw%", String.valueOf(_draw));
				FightReply.replace("%loos%", String.valueOf(_loss));

				activeChar.sendPacket(FightReply);
			}
		}
	}

	public synchronized void clearHeroes()
	{
		updateHeroes(true);

		if (!_heroes.isEmpty())
		{
			for (final StatsSet hero : _heroes.values())
			{
				final String name = hero.getString(Olympiad.CHAR_NAME);

				final Player player = GameObjectsStorage.getPlayer(name);

				if (player == null)
				{
					continue;
				}
				try
				{
					player.setHero(false, true);
					if (player.getClan() != null)
					{
						player.setPledgeClass(ClanMember.calculatePledgeClass(player));
					}
					else
					{
						player.setPledgeClass(player.isNoble() ? 5 : 1);
					}
					
					for (int i = 0; i < Inventory.PAPERDOLL_TOTALSLOTS; i++)
					{
						final ItemInstance equippedItem = player.getInventory().getPaperdollItem(i);
						if ((equippedItem != null) && equippedItem.isHeroItem())
						{
							player.getInventory().unEquipItemInSlot(i);
						}
					}

					for (final ItemInstance item : player.getInventory().getAvailableItems(false, false, false))
					{
						if ((item != null) && item.isHeroItem())
						{
							player.destroyItem("Hero", item, null, true);
							final InventoryUpdate iu = new InventoryUpdate();
							iu.addRemovedItem(item);
							player.sendPacket(iu);
						}
					}

					player.broadcastUserInfo(true);
				}
				catch (final NullPointerException e)
				{}
			}
		}
		_heroes.clear();
	}
	
	public synchronized void computeNewHeroes(List<StatsSet> newHeroes)
	{
		if (newHeroes.isEmpty())
		{
			_heroes.clear();
			return;
		}

		final Map<Integer, StatsSet> heroes = new HashMap<>();
		for (final StatsSet hero : newHeroes)
		{
			final int charId = hero.getInteger(Olympiad.CHAR_ID);

			if ((_completeHeroes != null) && _completeHeroes.containsKey(charId))
			{
				final StatsSet oldHero = _completeHeroes.get(charId);
				final int count = oldHero.getInteger(COUNT);
				oldHero.set(COUNT, count + 1);
				oldHero.set(PLAYED, 1);
				if (Config.AUTO_GET_HERO)
				{
					oldHero.set(ACTIVE, 1);
				}
				else
				{
					oldHero.set(ACTIVE, 0);
				}
				heroes.put(charId, oldHero);
			}
			else
			{
				final StatsSet newHero = new StatsSet();
				newHero.set(Olympiad.CHAR_NAME, hero.getString(Olympiad.CHAR_NAME));
				newHero.set(Olympiad.CLASS_ID, hero.getInteger(Olympiad.CLASS_ID));
				newHero.set(COUNT, 1);
				newHero.set(PLAYED, 1);
				if (Config.AUTO_GET_HERO)
				{
					newHero.set(ACTIVE, 1);
				}
				else
				{
					newHero.set(ACTIVE, 0);
				}
				heroes.put(charId, newHero);
			}
		}

		deleteItemsInDb();

		_heroes.clear();
		_heroes.putAll(heroes);

		heroes.clear();

		updateHeroes(false);
		
		Player player;
		for (final Integer charId : _heroes.keySet())
		{
			player = GameObjectsStorage.getPlayer(charId);
			if (player != null)
			{
				if (Config.AUTO_GET_HERO)
				{
					player.setHero(true, true);
					player.broadcastPacket(new SocialAction(player.getObjectId(), 20016));
					player.getCounters().addAchivementInfo("setHero", 0, -1, false, false, false);
					final Clan clan = player.getClan();
					if (clan != null)
					{
						player.setPledgeClass(ClanMember.calculatePledgeClass(player));
						clan.addReputationScore(Config.HERO_POINTS, true);
						final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.CLAN_MEMBER_C1_BECAME_HERO_AND_GAINED_S2_REPUTATION_POINTS);
						sm.addString(CharNameHolder.getInstance().getNameById(charId));
						sm.addNumber(Config.HERO_POINTS);
						clan.broadcastToOnlineMembers(sm);
					}
					else
					{
						player.setPledgeClass(8);
					}
					player.broadcastUserInfo(true);
					
					setHeroGained(player.getObjectId());
					loadDiary(player.getObjectId());
				}
				loadFights(player.getObjectId());
				_heroMessage.put(player.getObjectId(), "");
			}
			else
			{
				if (Config.AUTO_GET_HERO)
				{
					setHeroGained(charId);
					loadDiary(charId);

					Connection con = null;
					PreparedStatement statement = null;
					ResultSet rset = null;
					try
					{
						con = DatabaseFactory.getInstance().getConnection();
						statement = con.prepareStatement(GET_CLAN_NAME);
						statement.setInt(1, charId);
						rset = statement.executeQuery();
						if (rset.next())
						{
							final String clanName = rset.getString("clan_name");
							if (clanName != null)
							{
								final Clan clan = ClanHolder.getInstance().getClanByName(clanName);
								if (clan != null)
								{
									clan.addReputationScore(Config.HERO_POINTS, true);
									final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.CLAN_MEMBER_C1_BECAME_HERO_AND_GAINED_S2_REPUTATION_POINTS);
									sm.addString(CharNameHolder.getInstance().getNameById(charId));
									sm.addNumber(Config.HERO_POINTS);
									clan.broadcastToOnlineMembers(sm);
								}
							}
						}
					}
					catch (final Exception e)
					{
						_log.warn("could not get clan name of player with objectId:" + charId + ": " + e);
					}
					finally
					{
						DbUtils.closeQuietly(con, statement, rset);
					}
				}
				loadFights(charId);
				_heroMessage.put(charId, "");
			}
		}
	}

	public void updateHeroes(boolean setDefault)
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			if (setDefault)
			{
				statement = con.prepareStatement(UPDATE_ALL);
				statement.execute();
				statement.close();
			}
			else
			{
				StatsSet hero;
				int heroId;
				for (final Entry<Integer, StatsSet> entry : _heroes.entrySet())
				{
					hero = entry.getValue();
					heroId = entry.getKey();
					if (_completeHeroes.isEmpty() || !_completeHeroes.containsKey(heroId))
					{
						statement = con.prepareStatement(INSERT_HERO);
						statement.setInt(1, heroId);
						statement.setInt(2, hero.getInteger(Olympiad.CLASS_ID));
						statement.setInt(3, hero.getInteger(COUNT));
						statement.setInt(4, hero.getInteger(PLAYED));
						statement.setInt(5, hero.getInteger(ACTIVE));
						statement.execute();
						statement.close();

						statement = con.prepareStatement(GET_CLAN_ALLY);
						statement.setInt(1, heroId);
						rset = statement.executeQuery();
						if (rset.next())
						{
							final int clanId = rset.getInt("clanid");
							final int allyId = rset.getInt("allyId");

							String clanName = "";
							String allyName = "";
							int clanCrest = 0;
							int allyCrest = 0;

							if (clanId > 0)
							{
								clanName = ClanHolder.getInstance().getClan(clanId).getName();
								clanCrest = ClanHolder.getInstance().getClan(clanId).getCrestId();

								if (allyId > 0)
								{
									allyName = ClanHolder.getInstance().getClan(clanId).getAllyName();
									allyCrest = ClanHolder.getInstance().getClan(clanId).getAllyCrestId();
								}
							}

							hero.set(CLAN_CREST, clanCrest);
							hero.set(CLAN_NAME, clanName);
							hero.set(ALLY_CREST, allyCrest);
							hero.set(ALLY_NAME, allyName);
						}

						rset.close();
						statement.close();

						_heroes.remove(heroId);
						_heroes.put(heroId, hero);

						_completeHeroes.put(heroId, hero);
					}
					else
					{
						statement = con.prepareStatement(UPDATE_HERO);
						statement.setInt(1, hero.getInteger(COUNT));
						statement.setInt(2, hero.getInteger(PLAYED));
						statement.setInt(3, hero.getInteger(ACTIVE));
						statement.setInt(4, heroId);
						statement.execute();
						statement.close();
					}
				}
			}
		}
		catch (final SQLException e)
		{
			_log.warn("Hero System: Couldnt update Heroes", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
	}

	public void setHeroGained(int charId)
	{
		setDiaryData(charId, ACTION_HERO_GAINED, 0);
	}

	public void setRBkilled(int charId, int npcId)
	{
		setDiaryData(charId, ACTION_RAID_KILLED, npcId);

		final NpcTemplate template = NpcsParser.getInstance().getTemplate(npcId);

		if (_herodiary.containsKey(charId) && (template != null))
		{
			final List<StatsSet> _list = _herodiary.get(charId);
			_herodiary.remove(charId);
			final StatsSet _diaryentry = new StatsSet();
			final String date = (new SimpleDateFormat("yyyy-MM-dd HH")).format(new Date(System.currentTimeMillis()));
			_diaryentry.set("date", date);
			_diaryentry.set("action", template.getName(null) + " was defeated");
			_list.add(_diaryentry);
			_herodiary.put(charId, _list);
		}
	}

	public void setCastleTaken(int charId, int castleId)
	{
		setDiaryData(charId, ACTION_CASTLE_TAKEN, castleId);

		final Castle castle = CastleManager.getInstance().getCastleById(castleId);

		if ((castle != null) && _herodiary.containsKey(charId))
		{
			final List<StatsSet> _list = _herodiary.get(charId);
			_herodiary.remove(charId);
			final StatsSet _diaryentry = new StatsSet();
			final String date = (new SimpleDateFormat("yyyy-MM-dd HH")).format(new Date(System.currentTimeMillis()));
			_diaryentry.set("date", date);
			_diaryentry.set("action", castle.getName(null) + " Castle was successfuly taken");
			_list.add(_diaryentry);
			_herodiary.put(charId, _list);
		}
	}

	public void setDiaryData(int charId, int action, int param)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("INSERT INTO heroes_diary (charId, time, action, param) values(?,?,?,?)");
			statement.setInt(1, charId);
			statement.setLong(2, System.currentTimeMillis());
			statement.setInt(3, action);
			statement.setInt(4, param);
			statement.execute();
		}
		catch (final SQLException e)
		{
			_log.warn("SQL exception while saving DiaryData.", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	public void setHeroMessage(Player player, String message)
	{
		_heroMessage.put(player.getObjectId(), message);
	}

	public void saveHeroMessage(int charId)
	{
		if (_heroMessage.get(charId) == null)
		{
			return;
		}

		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE heroes SET message=? WHERE charId=?;");
			statement.setString(1, _heroMessage.get(charId));
			statement.setInt(2, charId);
			statement.execute();
		}
		catch (final SQLException e)
		{
			_log.warn("SQL exception while saving HeroMessage.", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	private void deleteItemsInDb()
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(DELETE_ITEMS);
			statement.execute();
		}
		catch (final SQLException e)
		{
			_log.warn("", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	public void shutdown()
	{
		for (final int charId : _heroMessage.keySet())
		{
			saveHeroMessage(charId);
		}
	}

	public boolean isHero(int objectId)
	{
		if (_heroes == null || _heroes.isEmpty())
		{
			return false;
		}
		if (_heroes.containsKey(objectId) && _heroes.get(objectId).getInteger(ACTIVE) == 1)
		{
			return true;
		}
		return false;
	}
	
	public boolean isInactiveHero(int id)
	{
		if (_heroes == null || _heroes.isEmpty())
		{
			return false;
		}
		if (_heroes.containsKey(id) && _heroes.get(id).getInteger(PLAYED) == 1 && _heroes.get(id).getInteger(ACTIVE) == 0)
		{
			return true;
		}
		return false;
	}
	
	public void activateHero(Player player)
	{
		final StatsSet hero = _heroes.get(player.getObjectId());
		hero.set(ACTIVE, 1);
		_heroes.remove(player.getObjectId());
		_heroes.put(player.getObjectId(), hero);
		
		player.setHero(true, true);
		player.broadcastPacket(new SocialAction(player.getObjectId(), 20016));
		player.getCounters().addAchivementInfo("setHero", 0, -1, false, false, false);
		final Clan clan = player.getClan();
		if (clan != null)
		{
			clan.addReputationScore(Config.HERO_POINTS, true);
			final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.CLAN_MEMBER_C1_BECAME_HERO_AND_GAINED_S2_REPUTATION_POINTS);
			sm.addString(CharNameHolder.getInstance().getNameById(player.getObjectId()));
			sm.addNumber(Config.HERO_POINTS);
			clan.broadcastToOnlineMembers(sm);
		}
		player.broadcastUserInfo(true);
		
		setHeroGained(player.getObjectId());
		loadDiary(player.getObjectId());
		updateHero(player.getObjectId());
	}
	
	public void updateHero(int id)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(UPDATE_HERO);
			for (final Integer heroId : _heroes.keySet())
			{
				if (id > 0 && heroId != id)
				{
					continue;
				}
				
				final StatsSet hero = _heroes.get(heroId);
				statement.setInt(1, hero.getInteger(COUNT));
				statement.setInt(2, hero.getInteger(PLAYED));
				statement.setInt(3, hero.getInteger(ACTIVE));
				statement.setInt(4, heroId);
				statement.execute();
				statement.clearParameters();
			}
		}
		catch (final SQLException e)
		{
			_log.warn("Hero System: Couldnt update Hero", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	private static class SingletonHolder
	{
		protected static final Hero _instance = new Hero();
	}
}