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
package l2e.gameserver;

import org.nio.impl.SelectorThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2e.gameserver.data.holder.ClanHolder;
import l2e.gameserver.data.parser.BotReportParser;
import l2e.gameserver.database.DatabaseBackupFactory;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.instancemanager.BloodAltarManager;
import l2e.gameserver.instancemanager.CHSiegeManager;
import l2e.gameserver.instancemanager.CastleManorManager;
import l2e.gameserver.instancemanager.CursedWeaponsManager;
import l2e.gameserver.instancemanager.EpicBossManager;
import l2e.gameserver.instancemanager.HellboundManager;
import l2e.gameserver.instancemanager.ItemAuctionManager;
import l2e.gameserver.instancemanager.LakfiManager;
import l2e.gameserver.instancemanager.QuestManager;
import l2e.gameserver.instancemanager.RaidBossSpawnManager;
import l2e.gameserver.instancemanager.games.FishingChampionship;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.entity.Hero;
import l2e.gameserver.model.olympiad.Olympiad;
import l2e.gameserver.network.GameClient;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.communication.AuthServerCommunication;
import l2e.gameserver.network.serverpackets.SystemMessage;

public class Shutdown extends Thread
{
	private static final Logger _log = LoggerFactory.getLogger(Shutdown.class);
	private static Shutdown _instance = null;
	
	private int _secondsShut;
	private int _shutdownMode;
	public static final int SIGTERM = 0;
	public static final int GM_SHUTDOWN = 1;
	public static final int GM_RESTART = 2;
	public static final int ABORT = 3;
	private static final String[] MODE_TEXT =
	{
	        "SIGTERM", "shutting down", "restarting", "aborting"
	};

	private void SendServerQuit(int seconds)
	{
		final var sysm = SystemMessage.getSystemMessage(SystemMessageId.THE_SERVER_WILL_BE_COMING_DOWN_IN_S1_SECONDS);
		sysm.addNumber(seconds);
		GameObjectsStorage.getPlayers().stream().filter(p -> p != null && p.isOnline()).forEach(p -> p.sendPacket(sysm));
	}
	
	private void SendServerQuitAnnounce(int seconds)
	{
		Announcements.getInstance().announceToAll("The server will be coming down in " + (seconds / 60) + " minutes!");
	}

	public void autoRestart(int time)
	{
		_secondsShut = time;
		countdown();
		_shutdownMode = GM_RESTART;
		System.exit(2);
	}

	protected Shutdown()
	{
		_shutdownMode = SIGTERM;
	}

	public Shutdown(int seconds, boolean restart)
	{
		if (seconds < 0)
		{
			seconds = 0;
		}
		_secondsShut = seconds;
		if (restart)
		{
			_shutdownMode = GM_RESTART;
		}
		else
		{
			_shutdownMode = GM_SHUTDOWN;
		}
	}

	@Override
	public void run()
	{
		if (_secondsShut <= 0)
		{
			switch (_shutdownMode)
			{
				case SIGTERM :
					_log.info("Shutting down NOW!");
					break;
				case GM_SHUTDOWN :
					_log.info("Shutting down NOW!");
					break;
				case GM_RESTART :
					_log.info("Restarting NOW!");
					break;
			}
			
			final var tc = new TimeCounter();
			final var tc1 = new TimeCounter();

			if (!SevenSigns.getInstance().isSealValidationPeriod())
			{
				SevenSignsFestival.getInstance().saveFestivalData(false);
				_log.info("SevenSignsFestival: Festival data saved(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
			}
			GameServer.getInstance().getListeners().onShutdown();
			SevenSigns.getInstance().saveSevenSignsData();
			_log.info("SevenSigns: Seven Signs data saved(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
			SevenSigns.getInstance().saveSevenSignsStatus();
			_log.info("SevenSigns: Seven Signs status saved(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
			RaidBossSpawnManager.getInstance().cleanUp();
			_log.info("RaidBossSpawnManager: All raidboss info saved(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
			EpicBossManager.getInstance().cleanUp();
			_log.info("EpicBossManager: All Epic Boss info saved(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
			HellboundManager.getInstance().cleanUp();
			_log.info("Hellbound Manager: Data saved(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
			Olympiad.getInstance().saveOlympiadStatus();
			_log.info("Olympiad System: Data saved(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
			Hero.getInstance().shutdown();
			_log.info("Hero System: Data saved(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
			ClanHolder.getInstance().storeClanScore();
			_log.info("Clan System: Data saved(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
			if (!Config.ALT_MANOR_SAVE_ALL_ACTIONS)
			{
				CastleManorManager.getInstance().storeMe();
				_log.info("Castle Manor Manager: Data saved(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
			}
			CHSiegeManager.getInstance().onServerShutDown();
			_log.info("CHSiegeManager: Siegable hall attacker lists saved!");
			QuestManager.getInstance().save();
			_log.info("Quest Manager: Data saved(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
			AuthServerCommunication.getInstance().shutdown();
			_log.info("Login Server Communication: has been shutdown(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
			try
			{
				disconnectAllPlayers();
				_log.info("All players disconnected and saved(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
			}
			catch (final Throwable t)
			{}
			BloodAltarManager.getInstance().saveDb();
			_log.info("BloodAltarManager: All destruction bosses info saved(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
			LakfiManager.getInstance().stopTimer();
			_log.info("LakfiManager: Stop task timer.");
			ItemAuctionManager.getInstance().shutdown();
			_log.info("Item Auction Manager: All tasks stopped(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
			CursedWeaponsManager.getInstance().saveData();
			_log.info("Cursed Weapons Manager: Data saved(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
			FishingChampionship.getInstance().shutdown();
			_log.info("Fishing Champion Ship: Data saved(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");

			if (Config.BOTREPORT_ENABLE)
			{
				BotReportParser.getInstance().saveReportedCharData();
				_log.info("Bot Report Data: Sucessfully saved reports to database!");
			}
			
			try
			{
				GameTimeController.getInstance().stopTimer();
				_log.info("Game Time Controller: Timer stopped(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
			}
			catch (final Throwable t)
			{}

			try
			{
				ThreadPoolManager.getInstance().shutdown();
				_log.info("Thread Pool Manager: Manager has been shut down(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
			}
			catch (final Throwable t)
			{}

			try
			{
				if (GameServer.getInstance() != null)
				{
					for(final SelectorThread<GameClient> st : GameServer.getInstance().getSelectorThreads())
					{
						try
						{
							st.shutdown();
						}
						catch(final Exception e)
						{
							e.printStackTrace();
						}
					}
				}
				_log.info("Game Server: Selector thread has been shut down(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
			}
			catch (final Throwable t)
			{}
			
			GameObjectsStorage.clear();
			
			if (Config.ALLOW_BACKUP_DATABASE)
			{
				_log.info("DatabaseBackupFactory: Start Backup Database...");
				DatabaseBackupFactory.getInstance().doBackup();
			}
			
			try
			{
				DatabaseFactory.getInstance().close();
				_log.info("Database Factory: Database connection has been shut down(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
			}
			catch (final Throwable t)
			{}

			if (getInstance()._shutdownMode == GM_RESTART)
			{
				Runtime.getRuntime().halt(2);
			}
			else
			{
				Runtime.getRuntime().halt(0);
			}
			_log.info("The server has been successfully shut down in " + (tc1.getEstimatedTime() / 1000) + "seconds.");
		}
		else
		{
			countdown();
			_log.warn("GM shutdown countdown is over. " + MODE_TEXT[_shutdownMode] + " NOW!");
			switch (_shutdownMode)
			{
				case GM_SHUTDOWN :
					getInstance().setMode(GM_SHUTDOWN);
					System.exit(0);
					break;
				case GM_RESTART :
					getInstance().setMode(GM_RESTART);
					System.exit(2);
					break;
			}
		}
	}

	public void startShutdown(Player activeChar, int seconds, boolean restart)
	{
		if (restart)
		{
			_shutdownMode = GM_RESTART;
		}
		else
		{
			_shutdownMode = GM_SHUTDOWN;
		}

		_log.warn("GM: " + activeChar.getName(null) + "(" + activeChar.getObjectId() + ") issued shutdown command. " + MODE_TEXT[_shutdownMode] + " in " + seconds + " seconds!");

		if (_shutdownMode > 0)
		{
			switch (seconds)
			{
				case 540 :
				case 480 :
				case 420 :
				case 360 :
				case 300 :
				case 240 :
				case 180 :
				case 120 :
				case 60 :
				case 30 :
				case 10 :
				case 5 :
				case 4 :
				case 3 :
				case 2 :
				case 1 :
					break;
				default :
					SendServerQuit(seconds);
			}
		}

		if (_instance != null)
		{
			_instance._abort();
		}
		_instance = new Shutdown(seconds, restart);
		_instance.start();
	}

	public void abort(Player activeChar)
	{
		_log.warn("GM: " + activeChar.getName(null) + "(" + activeChar.getObjectId() + ") issued shutdown ABORT. " + MODE_TEXT[_shutdownMode] + " has been stopped!");
		if (_instance != null)
		{
			_instance._abort();
			final Announcements _an = Announcements.getInstance();
			_an.announceToAll("Server aborts " + MODE_TEXT[_shutdownMode] + " and continues normal operation!");
		}
	}

	private void setMode(int mode)
	{
		_shutdownMode = mode;
	}

	private void _abort()
	{
		_shutdownMode = ABORT;
	}

	private void countdown()
	{
		try
		{
			while (_secondsShut > 0)
			{
				
				switch (_secondsShut)
				{
					case 7200 :
						SendServerQuitAnnounce(7200);
						break;
					case 5400 :
						SendServerQuitAnnounce(5400);
						break;
					case 3600 :
						SendServerQuitAnnounce(3600);
						break;
					case 1800 :
						SendServerQuitAnnounce(1800);
						break;
					case 600 :
						SendServerQuit(600);
						break;
					case 540 :
						SendServerQuit(540);
						break;
					case 480 :
						SendServerQuit(480);
						break;
					case 420 :
						SendServerQuit(420);
						break;
					case 360 :
						SendServerQuit(360);
						break;
					case 300 :
						SendServerQuit(300);
						break;
					case 240 :
						SendServerQuit(240);
						break;
					case 180 :
						SendServerQuit(180);
						break;
					case 120 :
						SendServerQuit(120);
						break;
					case 60 :
						SendServerQuit(60);
						break;
					case 30 :
						SendServerQuit(30);
						break;
					case 10 :
						SendServerQuit(10);
						break;
					case 5 :
						SendServerQuit(5);
						break;
					case 4 :
						SendServerQuit(4);
						break;
					case 3 :
						SendServerQuit(3);
						break;
					case 2 :
						SendServerQuit(2);
						break;
					case 1 :
						SendServerQuit(1);
						break;
				}

				_secondsShut--;

				Thread.sleep(1000);

				if (_shutdownMode == ABORT)
				{
					break;
				}
			}
		}
		catch (final InterruptedException e)
		{}
	}

	private void disconnectAllPlayers()
	{
		for (final var player : GameObjectsStorage.getPlayers())
		{
			if (player != null)
			{
				player.logout();
			}
		}
	}

	private static final class TimeCounter
	{
		private long _startTime;

		protected TimeCounter()
		{
			restartCounter();
		}

		protected void restartCounter()
		{
			_startTime = System.currentTimeMillis();
		}

		protected long getEstimatedTimeAndRestartCounter()
		{
			final var toReturn = System.currentTimeMillis() - _startTime;
			restartCounter();
			return toReturn;
		}

		protected long getEstimatedTime()
		{
			return System.currentTimeMillis() - _startTime;
		}
	}
	
	public int getMode()
	{
		return _shutdownMode;
	}
	
	public int getSeconds()
	{
		return _secondsShut;
	}
	
	public static final Shutdown getInstance()
	{
		if (_instance == null)
		{
			_instance = new Shutdown();
		}
		return _instance;
	}
}