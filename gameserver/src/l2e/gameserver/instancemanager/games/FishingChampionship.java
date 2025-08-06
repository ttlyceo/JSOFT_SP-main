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
package l2e.gameserver.instancemanager.games;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import l2e.commons.log.LoggerObject;
import l2e.commons.util.Rnd;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.instancemanager.ServerVariables;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.strings.server.ServerMessage;
import l2e.gameserver.model.strings.server.ServerStorage;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;
import l2e.gameserver.network.serverpackets.SystemMessage;

/**
 * Updated by LordWinter
 */
public class FishingChampionship extends LoggerObject
{
	private long _enddate = 0;
	
	private final List<String> _playersName = new ArrayList<>();
	private final List<String> _fishLength = new ArrayList<>();
	private final List<String> _winPlayersName = new ArrayList<>();
	private final List<String> _winFishLength = new ArrayList<>();
	private final List<Fisher> _tmpPlayer = new ArrayList<>();
	private final List<Fisher> _winPlayer = new ArrayList<>();
	private double _minFishLength = 0;

	public FishingChampionship()
	{
		restoreData();
		refreshWinResult();
		setNewMin();
		if (_enddate <= System.currentTimeMillis())
		{
			_enddate = System.currentTimeMillis();
			new FinishTask().run();
		}
		else
		{
			ThreadPoolManager.getInstance().schedule(new FinishTask(), _enddate - System.currentTimeMillis());
		}
	}

	private class Fisher
	{
		public double _length = 0;
		public String _name;
		public int _rewarded = 0;
	}

	private class FinishTask implements Runnable
	{
		@Override
		public void run()
		{
			_winPlayer.clear();
			for (final Fisher fisher : _tmpPlayer)
			{
				fisher._rewarded = 1;
				_winPlayer.add(fisher);
			}
			_tmpPlayer.clear();
			refreshWinResult();
			setEndOfChamp();
			shutdown();
		}
	}

	private void setEndOfChamp()
	{
		final Calendar finishtime = Calendar.getInstance();
		finishtime.setTimeInMillis(_enddate);
		finishtime.set(Calendar.MINUTE, 0);
		finishtime.set(Calendar.SECOND, 0);
		finishtime.add(Calendar.DAY_OF_MONTH, 6);
		finishtime.set(Calendar.DAY_OF_WEEK, 3);
		finishtime.set(Calendar.HOUR_OF_DAY, 19);
		_enddate = finishtime.getTimeInMillis();
		ServerVariables.set("fish_champion", _enddate);
		info("Period ends at " + new Date(_enddate));
	}

	private void restoreData()
	{
		_enddate = ServerVariables.getLong("fish_champion", 0);
		try (
		    var con = DatabaseFactory.getInstance().getConnection())
		{
			final var statement = con.prepareStatement("SELECT PlayerName,fishLength,rewarded FROM fishing_championship");
			final ResultSet rs = statement.executeQuery();
			while (rs.next())
			{
				final int rewarded = rs.getInt("rewarded");
				Fisher fisher;
				if (rewarded == 0)
				{
					fisher = new Fisher();
					fisher._name = rs.getString("PlayerName");
					fisher._length = rs.getFloat("fishLength");
					_tmpPlayer.add(fisher);
				}
				if (rewarded > 0)
				{
					fisher = new Fisher();
					fisher._name = rs.getString("PlayerName");
					fisher._length = rs.getFloat("fishLength");
					fisher._rewarded = rewarded;
					_winPlayer.add(fisher);
				}
			}
			rs.close();
			statement.close();
		}
		catch (final Exception e)
		{
			warn("Can't get fishing championship info: " + e.getMessage(), e);
		}
	}

	public synchronized void newFish(Player player, int lureId)
	{
		double len = Rnd.get(60, 89) + (Rnd.get(0, 1000) / 1000.);
		if ((lureId >= 8484) && (lureId <= 8486))
		{
			len += Rnd.get(0, 3000) / 1000.;
		}
		
		if (_tmpPlayer.size() < 5)
		{
			for (int i = 0; i < _tmpPlayer.size(); i++)
			{
				if (_tmpPlayer.get(i)._name.equalsIgnoreCase(player.getName(null)))
				{
					if (_tmpPlayer.get(i)._length < len)
					{
						_tmpPlayer.get(i)._length = len;
						player.sendMessage((new ServerMessage("FishingChampionship.IMPROVED_RESULT", player.getLang())).toString());
						setNewMin();
					}
					return;
				}
			}
			final Fisher newFisher = new Fisher();
			newFisher._name = player.getName(null);
			newFisher._length = len;
			_tmpPlayer.add(newFisher);
			player.sendMessage((new ServerMessage("FishingChampionship.GOT_TO_LIST", player.getLang())).toString());
			setNewMin();
		}
		else
		{
			if (_minFishLength >= len)
			{
				return;
			}
			for (int i = 0; i < _tmpPlayer.size(); i++)
			{
				if (_tmpPlayer.get(i)._name.equalsIgnoreCase(player.getName(null)))
				{
					if (_tmpPlayer.get(i)._length < len)
					{
						_tmpPlayer.get(i)._length = len;
						player.sendMessage((new ServerMessage("FishingChampionship.IMPROVED_RESULT", player.getLang())).toString());
						setNewMin();
					}
					return;
				}
			}
			Fisher minFisher = null;
			double minLen = 99999;
			for (final Fisher a_tmpPlayer : _tmpPlayer)
			{
				if (a_tmpPlayer._length < minLen)
				{
					minFisher = a_tmpPlayer;
					minLen = minFisher._length;
				}
			}
			_tmpPlayer.remove(minFisher);
			final Fisher newFisher = new Fisher();
			newFisher._name = player.getName(null);
			newFisher._length = len;
			_tmpPlayer.add(newFisher);
			player.sendMessage((new ServerMessage("FishingChampionship.GOT_TO_LIST", player.getLang())).toString());
			setNewMin();
		}
	}

	private void setNewMin()
	{
		double minLen = 99999;
		for (final Fisher a_tmpPlayer : _tmpPlayer)
		{
			if (a_tmpPlayer._length < minLen)
			{
				minLen = a_tmpPlayer._length;
			}
		}
		_minFishLength = minLen;
	}

	public long getTimeRemaining()
	{
		return (_enddate - System.currentTimeMillis()) / 60000;
	}

	public String getWinnerName(Player player, int par)
	{
		if (_winPlayersName.size() >= par)
		{
			return _winPlayersName.get(par - 1);
		}
		return "" + ServerStorage.getInstance().getString(player.getLang(), "FishingChampionship.NO") + "";
	}

	public String getCurrentName(Player player, int par)
	{
		if (_playersName.size() >= par)
		{
			return _playersName.get(par - 1);
		}
		return "" + ServerStorage.getInstance().getString(player.getLang(), "FishingChampionship.NO") + "";
	}

	public String getFishLength(int par)
	{
		if (_winFishLength.size() >= par)
		{
			return _winFishLength.get(par - 1);
		}
		return "0";
	}

	public String getCurrentFishLength(int par)
	{
		if (_fishLength.size() >= par)
		{
			return _fishLength.get(par - 1);
		}
		return "0";
	}

	public void getReward(Player player)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(player.getObjectId());
		
		String str = "<html><head><title>" + ServerStorage.getInstance().getString(player.getLang(), "FishingChampionship.ROYAL_TOURNAMENT") + "</title></head>";
		str = str + "" + ServerStorage.getInstance().getString(player.getLang(), "FishingChampionship.ACCEPT_CONGRATULATIONS") + "<br>";
		str = str + "" + ServerStorage.getInstance().getString(player.getLang(), "FishingChampionship.HERE_YOUR_PRIZE") + "<br>";
		str = str + "" + ServerStorage.getInstance().getString(player.getLang(), "FishingChampionship.GOOD_LUCK") + "";
		str = str + "</body></html>";
		html.setHtml(player, str);
		player.sendPacket(html);
		for (final Fisher fisher : _winPlayer)
		{
			if (fisher._name.equalsIgnoreCase(player.getName(null)) && (fisher._rewarded != 2))
			{
				int rewardCnt = 0;
				for (int x = 0; x < _winPlayersName.size(); x++)
				{
					if (_winPlayersName.get(x).equalsIgnoreCase(player.getName(null)))
					{
						switch (x)
						{
							case 0 :
								rewardCnt = 800000;
								break;
							case 1 :
								rewardCnt = 500000;
								break;
							case 2 :
								rewardCnt = 300000;
								break;
							case 3 :
								rewardCnt = 200000;
								break;
							case 4 :
								rewardCnt = 100000;
								break;
						}
					}
				}
				fisher._rewarded = 2;
				if (rewardCnt > 0)
				{
					final ItemInstance item = player.getInventory().addItem("reward", 57, rewardCnt, player, player);
					player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.EARNED_S2_S1_S).addItemName(item).addNumber(rewardCnt));
					player.sendItemList(false);
				}
			}
		}
	}

	public void showMidResult(Player player)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(player.getObjectId());
		
		String str = "<html><head><title>" + ServerStorage.getInstance().getString(player.getLang(), "FishingChampionship.ROYAL_TOURNAMENT") + "</title></head>";
		str = str + "" + ServerStorage.getInstance().getString(player.getLang(), "FishingChampionship.NOW_PASS_COMPETITIONS") + "<br><br>";
		str = str + "" + ServerStorage.getInstance().getString(player.getLang(), "FishingChampionship.UPON_CPMPETITIONS") + "<br>";
		str = str + "<table width=280 border=0 bgcolor=\"000000\"><tr><td width=70 align=center>" + ServerStorage.getInstance().getString(player.getLang(), "FishingChampionship.PLACES") + "</td><td width=110 align=center>" + ServerStorage.getInstance().getString(player.getLang(), "FishingChampionship.FISHERMAN") + "</td><td width=80 align=center>" + ServerStorage.getInstance().getString(player.getLang(), "FishingChampionship.LENGTH") + "</td></tr></table><table width=280>";
		for (int x = 1; x <= 5; x++)
		{
			str = str + "<tr><td width=70 align=center>" + x + " " + ServerStorage.getInstance().getString(player.getLang(), "FishingChampionship.PLACES") + ":</td>";
			str = str + "<td width=110 align=center>" + getCurrentName(player, x) + "</td>";
			str = str + "<td width=80 align=center>" + getCurrentFishLength(x) + "</td></tr>";
		}
		str = str + "<td width=80 align=center>0</td></tr></table><br>";
		str = str + "" + ServerStorage.getInstance().getString(player.getLang(), "FishingChampionship.PRIZES_LIST") + "<br><table width=280 border=0 bgcolor=\"000000\"><tr><td width=70 align=center>" + ServerStorage.getInstance().getString(player.getLang(), "FishingChampionship.PLACES") + "</td><td width=110 align=center>" + ServerStorage.getInstance().getString(player.getLang(), "FishingChampionship.PRIZE") + "</td><td width=80 align=center>" + ServerStorage.getInstance().getString(player.getLang(), "FishingChampionship.AMOUNT") + "</td></tr></table><table width=280>";
		str = str + "<tr><td width=70 align=center>1 " + ServerStorage.getInstance().getString(player.getLang(), "FishingChampionship.PLACES") + ":</td><td width=110 align=center>" + ServerStorage.getInstance().getString(player.getLang(), "FishingChampionship.ADENA") + "</td><td width=80 align=center>800000</td></tr><tr><td width=70 align=center>2 " + ServerStorage.getInstance().getString(player.getLang(), "FishingChampionship.PLACES") + ":</td><td width=110 align=center>" + ServerStorage.getInstance().getString(player.getLang(), "FishingChampionship.ADENA") + "</td><td width=80 align=center>500000</td></tr><tr><td width=70 align=center>3 " + ServerStorage.getInstance().getString(player.getLang(), "FishingChampionship.PLACES") + ":</td><td width=110 align=center>" + ServerStorage.getInstance().getString(player.getLang(), "FishingChampionship.ADENA") + "</td><td width=80 align=center>300000</td></tr>";
		str = str + "<tr><td width=70 align=center>4 " + ServerStorage.getInstance().getString(player.getLang(), "FishingChampionship.PLACES") + ":</td><td width=110 align=center>" + ServerStorage.getInstance().getString(player.getLang(), "FishingChampionship.ADENA") + "</td><td width=80 align=center>200000</td></tr><tr><td width=70 align=center>5 " + ServerStorage.getInstance().getString(player.getLang(), "FishingChampionship.PLACES") + ":</td><td width=110 align=center>" + ServerStorage.getInstance().getString(player.getLang(), "FishingChampionship.ADENA") + "</td><td width=80 align=center>100000</td></tr></table></body></html>";
		html.setHtml(player, str);
		player.sendPacket(html);
	}

	public void shutdown()
	{
		PreparedStatement statement;
		try (
		    var con = DatabaseFactory.getInstance().getConnection())
		{
			statement = con.prepareStatement("DELETE FROM fishing_championship");
			statement.execute();
			statement.close();

			for (final Fisher fisher : _winPlayer)
			{
				statement = con.prepareStatement("INSERT INTO fishing_championship(PlayerName,fishLength,rewarded) VALUES (?,?,?)");
				statement.setString(1, fisher._name);
				statement.setDouble(2, fisher._length);
				statement.setInt(3, fisher._rewarded);
				statement.execute();
				statement.close();
			}
			for (final Fisher fisher : _tmpPlayer)
			{
				statement = con.prepareStatement("INSERT INTO fishing_championship(PlayerName,fishLength,rewarded) VALUES (?,?,?)");
				statement.setString(1, fisher._name);
				statement.setDouble(2, fisher._length);
				statement.setInt(3, 0);
				statement.execute();
				statement.close();
			}
		}
		catch (final Exception e)
		{
			warn("Can't update player vitality: " + e.getMessage(), e);
		}
	}

	private void refreshWinResult()
	{
		_winPlayersName.clear();
		_winFishLength.clear();
		Fisher fisher1, fisher2;
		for (int x = 0; x <= (_winPlayer.size() - 1); x++)
		{
			for (int y = 0; y <= (_winPlayer.size() - 2); y++)
			{
				fisher1 = _winPlayer.get(y);
				fisher2 = _winPlayer.get(y + 1);
				if (fisher1._length < fisher2._length)
				{
					_winPlayer.set(y, fisher2);
					_winPlayer.set(y + 1, fisher1);
				}
			}
		}

		for (int i = 0; i <= (_winPlayer.size() - 1); i++)
		{
			_winPlayersName.add(_winPlayer.get(i)._name);
			_winFishLength.add("" + _winPlayer.get(i)._length);
		}
	}
	
	public static final FishingChampionship getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final FishingChampionship _instance = new FishingChampionship();
	}
}