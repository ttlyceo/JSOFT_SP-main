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

import java.awt.Toolkit;
import java.io.File;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import l2e.gameserver.data.parser.*;
import l2e.gameserver.instancemanager.*;
import l2e.gameserver.model.entity.events.tournaments.TournamentData;
import l2e.gameserver.model.entity.events.tournaments.data.parser.TournamentsParser;
import org.HostInfo;
import org.nio.impl.HaProxySelectorThread;
import org.nio.impl.SelectorThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.strixplatform.StrixPlatform;

import l2e.commons.apache.StatsUtils;
import l2e.commons.listener.Listener;
import l2e.commons.listener.ListenerList;
import l2e.commons.net.IPSettings;
import l2e.commons.util.Functions;
import l2e.fake.FakePlayerManager;
import l2e.gameserver.data.dao.ClanVariablesDAO;
import l2e.gameserver.data.holder.CharMiniGameHolder;
import l2e.gameserver.data.holder.CharNameHolder;
import l2e.gameserver.data.holder.CharSummonHolder;
import l2e.gameserver.data.holder.ClanHolder;
import l2e.gameserver.data.holder.CrestHolder;
import l2e.gameserver.data.holder.NpcBufferHolder;
import l2e.gameserver.data.holder.SpawnHolder;
import l2e.gameserver.data.htm.HtmCache;
import l2e.gameserver.data.htm.ImagesCache;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.geodata.GeoEngine;
import l2e.gameserver.handler.actionhandlers.ActionHandler;
import l2e.gameserver.handler.actionshifthandlers.ActionShiftHandler;
import l2e.gameserver.handler.admincommandhandlers.AdminCommandHandler;
import l2e.gameserver.handler.bypasshandlers.BypassHandler;
import l2e.gameserver.handler.chathandlers.ChatHandler;
import l2e.gameserver.handler.communityhandlers.CommunityBoardHandler;
import l2e.gameserver.handler.effecthandlers.EffectHandler;
import l2e.gameserver.handler.itemhandlers.ItemHandler;
import l2e.gameserver.handler.skillhandlers.SkillHandler;
import l2e.gameserver.handler.targethandlers.TargetHandler;
import l2e.gameserver.handler.usercommandhandlers.UserCommandHandler;
import l2e.gameserver.handler.voicedcommandhandlers.VoicedCommandHandler;
import l2e.gameserver.idfactory.IdFactory;
import l2e.gameserver.instancemanager.games.FishingChampionship;
import l2e.gameserver.instancemanager.games.MonsterRaceManager;
import l2e.gameserver.instancemanager.games.krateiscube.KrateisCubeManager;
import l2e.gameserver.instancemanager.mods.EnchantSkillManager;
import l2e.gameserver.instancemanager.mods.TimeSkillsTaskManager;
import l2e.gameserver.listener.ScriptListenerLoader;
import l2e.gameserver.listener.game.OnShutdownListener;
import l2e.gameserver.listener.game.OnStartListener;
import l2e.gameserver.model.AutoSpawnHandler;
import l2e.gameserver.model.World;
import l2e.gameserver.model.entity.Hero;
import l2e.gameserver.model.entity.events.EventsDropManager;
import l2e.gameserver.model.entity.events.cleft.AerialCleftEvent;
import l2e.gameserver.model.entity.events.custom.Leprechaun;
import l2e.gameserver.model.entity.events.custom.achievements.AchievementManager;
import l2e.gameserver.model.entity.events.model.FightEventManager;
import l2e.gameserver.model.entity.events.model.FightEventNpcManager;
import l2e.gameserver.model.entity.events.model.FightLastStatsManager;
import l2e.gameserver.model.olympiad.Olympiad;
import l2e.gameserver.model.service.autofarm.FarmSettings;
import l2e.gameserver.model.strings.server.ServerStorage;
import l2e.gameserver.network.GameClient;
import l2e.gameserver.network.GamePacketHandler;
import l2e.gameserver.network.communication.AuthServerCommunication;
import l2e.gameserver.taskmanager.AutoAnnounceTaskManager;
import l2e.gameserver.taskmanager.AutoTaskManager;
import l2e.gameserver.taskmanager.ItemsAutoDestroy;
import l2e.gameserver.taskmanager.RestoreOfflineTraders;
import top.jsoft.jguard.JGuard;
import top.jsoft.phaseevent.PhaseManager;

public class GameServer
{
	private static final Logger _log = LoggerFactory.getLogger(GameServer.class);

	public class GameServerListenerList extends ListenerList<GameServer>
	{
		public void onStart()
		{
			for (final Listener<GameServer> listener : getListeners())
			{
				if (OnStartListener.class.isInstance(listener))
				{
					((OnStartListener) listener).onStart();
				}
			}
		}
		
		public void onShutdown()
		{
			for (final Listener<GameServer> listener : getListeners())
			{
				if (OnShutdownListener.class.isInstance(listener))
				{
					((OnShutdownListener) listener).onShutdown();
				}
			}
		}
	}
	
	private final List<SelectorThread<GameClient>> _selectorThreads = new ArrayList<>();
	
	private final GameServerListenerList _listeners;
	private final IdFactory _idFactory;
	public static GameServer _instance;
	public static final Calendar dateTimeServerStarted = Calendar.getInstance();
	public static Date server_started;
	
	public List<SelectorThread<GameClient>> getSelectorThreads()
	{
		return _selectorThreads;
	}
	
	public GameServer() throws Exception
	{
		_instance = this;
		_listeners = new GameServerListenerList();
		_idFactory = IdFactory.getInstance();

		if (!_idFactory.isInitialized())
		{
			_log.warn("Could not read object IDs from DB. Please Check Your Data.");
			throw new Exception("Could not initialize the ID factory");
		}

		new File(Config.DATAPACK_ROOT, "data/crests").mkdirs();
		new File("log/game").mkdirs();
		new File("log/game/chat").mkdirs();
		
		final HostInfo[] hosts = IPSettings.getInstance().getGameServerHosts();
		if (hosts.length == 0)
		{
			throw new Exception("Server hosts list is empty!");
		}
		final List<HostInfo> freeHosts = new ArrayList<>();
		for (final var host : hosts)
		{
			if (host.getAddress() != null)
			{
				while (!checkFreePort(host.getAddress(), host.getPort()))
				{
					_log.warn("Port '" + host.getPort() + "' on host '" + host.getAddress() + "' is allready binded. Please free it and restart server.");
					try
					{
						Thread.sleep(1000L);
					}
					catch (final InterruptedException e2)
					{}
				}
				freeHosts.add(host);
			}
		}
		
		if (freeHosts.size() == 0)
		{
			throw new Exception("Server host list is empty!");
		}
		
		if (Config.ALLOW_CUSTOM_INTERFACE)
		{
			emudev.KeyChecker.getInstance();
		}
		StrixPlatform.getInstance();
		
		printSection("Engines");
		ScriptListenerLoader.getInstance();
		ServerStorage.getInstance();
		
		printSection("World");
		GameTimeController.getInstance();
		ReflectionManager.getInstance();
		World.getInstance();
		MapRegionManager.getInstance();
		Announcements.getInstance();
		EventsDropManager.getInstance();
		CategoryParser.getInstance();
		ServerVariables.getVars();

		printSection("Skills");
		EffectHandler.getInstance().executeScript();
		EnchantSkillGroupsParser.getInstance();
		SkillTreesParser.getInstance();
		SkillsParser.getInstance();
		SchemesParser.getInstance();
		if (Config.ALLOW_SELLBUFFS_COMMAND)
		{
			SellBuffsParser.getInstance();
		}

		printSection("Items");
		ItemsParser.getInstance();
		ProductItemParser.getInstance();
		DonationParser.getInstance();
		ExchangeItemParser.getInstance();
		FoundationParser.getInstance();
		if (Config.ALLOW_VISUAL_ARMOR_COMMAND)
		{
			DressArmorParser.getInstance();
			DressCloakParser.getInstance();
			DressShieldParser.getInstance();
			DressHatParser.getInstance();
			DressWeaponParser.getInstance();
		}
		SoulCrystalParser.getInstance();
		EnchantItemGroupsParser.getInstance();
		EnchantItemParser.getInstance();
		EnchantItemOptionsParser.getInstance();
		OptionsParser.getInstance();
		EnchantItemHPBonusParser.getInstance();
		MerchantPriceParser.getInstance().loadInstances();
		BuyListParser.getInstance();
		MultiSellParser.getInstance();
		RecipeParser.getInstance();
		ArmorSetsParser.getInstance();
		FishMonstersParser.getInstance();
		FishParser.getInstance();
		FishingChampionship.getInstance();
		HennaParser.getInstance();
		CursedWeaponsManager.getInstance();
		HerbsDropParser.getInstance();

		printSection("Characters");
		ClassListParser.getInstance();
		ClassMasterParser.getInstance();
		InitialBuffParser.getInstance();
		InitialEquipmentParser.getInstance();
		InitialShortcutParser.getInstance();
		ExperienceParser.getInstance();
		ExpPercentLostParser.getInstance();
		HitConditionBonusParser.getInstance();
		CharTemplateParser.getInstance();
		CharNameHolder.getInstance();
		PremiumAccountsParser.getInstance();
		DailyTaskManager.getInstance();
		AdminParser.getInstance();
		PetsParser.getInstance();
		CharSummonHolder.getInstance().init();
		if (Config.NEW_PETITIONING_SYSTEM)
		{
			PetitionGroupParser.getInstance();
		}
		PetitionManager.getInstance();
		if (Config.ENABLE_ANTI_BOT_SYSTEM)
		{
			BotCheckManager.getInstance();
		}
		PromoCodeParser.getInstance();
		PvpColorManager.getInstance();
		LimitStatParser.getInstance();
		
		printSection("Clans");
		ClanParser.getInstance();
		ClanHolder.getInstance();
		ClanVariablesDAO.getInstance().restore();
		CHSiegeManager.getInstance();
		ClanHallManager.getInstance();
		AuctionManager.getInstance();

		printSection("Geodata");
		GeoEngine.getInstance();

		printSection("NPCs");
		NpcsParser.getInstance();
		DropManager.getInstance();
		NpcStatManager.getInstance();
		WalkingManager.getInstance();
		StaticObjectsParser.getInstance();
		ZoneManager.getInstance();
		DoorParser.getInstance();
		ColosseumFenceParser.getInstance();
		ItemAuctionManager.getInstance();
		CastleManager.getInstance().loadInstances();
		FortManager.getInstance().loadInstances();
		NpcBufferHolder.getInstance();
		ChampionManager.getInstance();
		BloodAltarManager.getInstance();
		RaidBossSpawnManager.getInstance();
		SpawnParser.getInstance();
		SpawnHolder.getInstance();
		DamageLimitParser.getInstance();
		HellboundManager.getInstance();
		ReflectionParser.getInstance();
		ZoneManager.getInstance().createZoneReflections();
		DayNightSpawnManager.getInstance().trim().notifyChangeMode();
		EpicBossManager.getInstance().initZones();
		FourSepulchersManager.getInstance().init();
		DimensionalRiftManager.getInstance();
		BotReportParser.getInstance();
		TeleLocationParser.getInstance();
		CommunityTeleportsParser.getInstance();
		AugmentationParser.getInstance();
		TransformParser.getInstance();
		
		printSection("Seven Signs");
		SevenSigns.getInstance();
		SevenSigns.getInstance().spawnSevenSignsNPC();
		SevenSignsFestival.getInstance();

		printSection("Siege");
		SiegeManager.getInstance().getSieges();
		FortSiegeManager.getInstance();
		TerritoryWarManager.getInstance();
		CastleManorManager.getInstance();
		MercTicketManager.getInstance();

		printSection("Olympiad");
		Olympiad.getInstance();
		Hero.getInstance();

		printSection("Cache");
		HtmCache.getInstance();
		CrestHolder.getInstance();
		ImagesCache.getInstance();

		printSection("Handlers");
		if (!Config.ALT_DEV_NO_HANDLERS)
		{
			AutoSpawnHandler.getInstance();
			ActionHandler.getInstance();
			ActionShiftHandler.getInstance();
			AdminCommandHandler.getInstance();
			BypassHandler.getInstance();
			ChatHandler.getInstance();
			CommunityBoardHandler.getInstance();
			ItemHandler.getInstance();
			SkillHandler.getInstance();
			TargetHandler.getInstance();
			UserCommandHandler.getInstance();
			VoicedCommandHandler.getInstance();
		}
		
		if (Config.BALANCER_ALLOW)
		{
			printSection("Balancer");
			ClassBalanceParser.getInstance();
			SkillBalanceParser.getInstance();
		}
		
		printSection("Gracia");
		SoDManager.getInstance();
		SoIManager.getInstance();
		AerialCleftEvent.getInstance();

		printSection("Vehicles");
		BoatManager.getInstance();
		AirShipManager.getInstance();

		printSection("Game Processes");
		KrateisCubeManager.getInstance();
		UndergroundColiseumManager.getInstance();
		MonsterRaceManager.getInstance();
		CharMiniGameHolder.getInstance().select();
		
		printSection("Events");
		TournamentsParser.getInstance();
		TournamentData.getInstance();
		WorldEventParser.getInstance();
		if (Config.ALLOW_FIGHT_EVENTS)
		{
			FightEventMapParser.getInstance();
			FightEventParser.getInstance();
			FightLastStatsManager.getInstance().restoreStats();
			FightEventManager.getInstance();
			FightEventNpcManager.getInstance();
		}
		else
		{
			_log.info("FightEventManager: All fight events disabled.");
		}
		
		if (Config.ENABLED_LEPRECHAUN)
		{
			Leprechaun.getInstance();
		}

		printSection("Scripts");
		QuestManager.getInstance();
		QuestsParser.getInstance();
		CastleManager.getInstance().activateInstances();
		FortManager.getInstance().activateInstances();
		MerchantPriceParser.getInstance().updateReferences();
		ScriptListenerLoader.getInstance().executeScriptList();
		DragonValleyManager.getInstance();
		
		if (Config.LAKFI_ENABLED)
		{
			LakfiManager.getInstance();
		}
		
		DoubleSessionManager.getInstance().registerEvent(DoubleSessionManager.GAME_ID);
		Runtime.getRuntime().addShutdownHook(Shutdown.getInstance());

		printSection("Protection System");
		PunishmentManager.getInstance();
		JGuard.load();

		printSection("Mods");
		if (Config.ALLOW_VIP_SYSTEM)
		{
			VipManager.getInstance();
		}
		TimeSkillsTaskManager.getInstance();
		RewardManager.getInstance();
		AchievementManager.getInstance();
		DailyRewardManager.getInstance();
		OnlineRewardManager.getInstance();
		SpecialRatesParser.getInstance();
		DonateRatesParser.getInstance();

		if (Config.ALLOW_WEDDING)
		{
			CoupleManager.getInstance();
		}
		WeeklyTraderManager.getInstance();
		EnchantSkillManager.getInstance();
		printSection("Other");
		SpecialBypassManager.getInstance();
		VoteRewardParser.getInstance();
		if (Config.ALLOW_FAKE_PLAYERS)
		{
			_log.info("FakePlayerManager: Loading fake players system...");
			FakePlayerManager.getInstance();
		}
		AutoTaskManager.init();
		ItemsAutoDestroy.getInstance();
		if (Config.ALLOW_MAIL)
		{
			MailManager.getInstance();
		}
		AutoRestart.getInstance();
		if (Config.ALLOW_RECOVERY_ITEMS)
		{
			ItemRecoveryManager.getInstance();
		}
		if (Config.ONLINE_PLAYERS_ANNOUNCE_INTERVAL > 0)
		{
			OnlinePlayers.getInstance();
		}
		
		if (FarmSettings.ALLOW_AUTO_FARM)
		{
			AutoFarmManager.getInstance();
		}
		if ((Config.OFFLINE_TRADE_ENABLE || Config.OFFLINE_CRAFT_ENABLE) && Config.RESTORE_OFFLINERS)
		{
			ThreadPoolManager.getInstance().schedule(new RestoreOfflineTraders(), 10000L);
		}
		// JSOFT
		if(Config.PHASEEVENT_ENABLE)
			PhaseManager.getInstance();

		getListeners().onStart();
		Toolkit.getDefaultToolkit().beep();

		AutoAnnounceTaskManager.getInstance();
		DelayedItemsManager.getInstance();

		_log.info("-------------------------------------------------------------------------------");
		StatsUtils.getMemUsage(_log);
		_log.info("-------------------------------------------------------------------------------");
		registerSelectorThreads(freeHosts);
		AuthServerCommunication.getInstance().start();
		server_started = new Date();
	}

	public static void main(String[] args) throws Exception
	{
		final File logFolder = new File(Config.DATAPACK_ROOT, "log");
		logFolder.mkdir();

		Config.load();
		printSection("Database");
		Class.forName(Config.DATABASE_DRIVER).getDeclaredConstructor().newInstance();
		DatabaseFactory.getInstance().getConnection().close();
		ThreadPoolManager.getInstance();
		new GameServer();
	}
	
	private static boolean checkFreePort(String hostname, int port)
	{
		ServerSocket ss = null;
		try
		{
			if (hostname.equalsIgnoreCase("*"))
			{
				ss = new ServerSocket(port);
			}
			else
			{
				ss = new ServerSocket(port, 50, InetAddress.getByName(hostname));
			}
		}
		catch (final Exception e)
		{
			return false;
		}
		finally
		{
			try
			{
				ss.close();
			}
			catch (final Exception e)
			{}
		}
		return true;
	}
	
	private void registerSelectorThreads(List<HostInfo> hosts)
	{
		final GamePacketHandler gph = new GamePacketHandler();
		
		for (final var host : hosts)
		{
			registerSelectorThread(gph, null, host);
		}
	}

	private void registerSelectorThread(GamePacketHandler gph, String ip, HostInfo host)
	{
		try
		{
			if (host.isAllowHaProxy())
			{
				final var selectorThread = new HaProxySelectorThread<>(Config.SELECTOR_CONFIG, gph, gph, gph, null);
				selectorThread.openServerSocket(ip == null ? null : InetAddress.getByName(ip), host.getPort());
				selectorThread.start();
				_selectorThreads.add(selectorThread);
			}
			else
			{
				final var selectorThread = new SelectorThread<>(Config.SELECTOR_CONFIG, gph, gph, gph, null);
				selectorThread.openServerSocket(ip == null ? null : InetAddress.getByName(ip), host.getPort());
				selectorThread.start();
				_selectorThreads.add(selectorThread);
			}
		}
		catch (final Exception e)
		{}
	}
	
	public static void printSection(String s)
	{
		s = "=[ " + s + " ]";
		while (s.length() < 78)
		{
			s = "-" + s;
		}
		_log.info(s);
	}

	public GameServerListenerList getListeners()
	{
		return _listeners;
	}
	
	public <T extends Listener<GameServer>> boolean addListener(T listener)
	{
		return _listeners.add(listener);
	}
	
	public <T extends Listener<GameServer>> boolean removeListener(T listener)
	{
		return _listeners.remove(listener);
	}
	
	public static GameServer getInstance()
	{
		return _instance;
	}
	
	public int getOnlineLimit()
	{
		return Functions.isValidKey(Config.USER_KEY) ? Config.MAXIMUM_ONLINE_USERS : 10;
	}
}