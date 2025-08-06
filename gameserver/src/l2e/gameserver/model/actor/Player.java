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
package l2e.gameserver.model.actor;

import static l2e.gameserver.network.serverpackets.ExSetCompassZoneCode.ZONE_ALTERED_FLAG;
import static l2e.gameserver.network.serverpackets.ExSetCompassZoneCode.ZONE_PEACE_FLAG;
import static l2e.gameserver.network.serverpackets.ExSetCompassZoneCode.ZONE_PVP_FLAG;
import static l2e.gameserver.network.serverpackets.ExSetCompassZoneCode.ZONE_SIEGE_FLAG;
import static l2e.gameserver.network.serverpackets.ExSetCompassZoneCode.ZONE_SSQ_FLAG;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import l2e.commons.apache.math.NumberUtils;
import l2e.commons.apache.tuple.ImmutablePair;
import l2e.commons.apache.tuple.Pair;
import l2e.commons.dbutils.DbUtils;
import l2e.commons.util.Rnd;
import l2e.commons.util.TransferSkillUtils;
import l2e.commons.util.Util;
import l2e.fake.FakePlayer;
import l2e.gameserver.Announcements;
import l2e.gameserver.Config;
import l2e.gameserver.GameTimeController;
import l2e.gameserver.RecipeController;
import l2e.gameserver.SevenSigns;
import l2e.gameserver.SevenSignsFestival;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.ai.character.CharacterAI;
import l2e.gameserver.ai.character.PlayerAI;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.data.dao.AchievementsDAO;
import l2e.gameserver.data.dao.CharacterBookMarkDAO;
import l2e.gameserver.data.dao.CharacterDAO;
import l2e.gameserver.data.dao.CharacterHennaDAO;
import l2e.gameserver.data.dao.CharacterItemReuseDAO;
import l2e.gameserver.data.dao.CharacterPremiumDAO;
import l2e.gameserver.data.dao.CharacterSkillSaveDAO;
import l2e.gameserver.data.dao.CharacterSkillsDAO;
import l2e.gameserver.data.dao.CharacterVariablesDAO;
import l2e.gameserver.data.dao.CharacterVisualDAO;
import l2e.gameserver.data.dao.DailyTasksDAO;
import l2e.gameserver.data.holder.CharNameHolder;
import l2e.gameserver.data.htm.HtmCache;
import l2e.gameserver.data.htm.WarehouseCache;
import l2e.gameserver.data.parser.AdminParser;
import l2e.gameserver.data.parser.CategoryParser;
import l2e.gameserver.data.parser.CharTemplateParser;
import l2e.gameserver.data.parser.DamageLimitParser;
import l2e.gameserver.data.parser.DressArmorParser;
import l2e.gameserver.data.parser.DressCloakParser;
import l2e.gameserver.data.parser.DressHatParser;
import l2e.gameserver.data.parser.DressShieldParser;
import l2e.gameserver.data.parser.DressWeaponParser;
import l2e.gameserver.data.parser.EnchantSkillGroupsParser;
import l2e.gameserver.data.parser.ExpPercentLostParser;
import l2e.gameserver.data.parser.ExperienceParser;
import l2e.gameserver.data.parser.FightEventParser;
import l2e.gameserver.data.parser.FishParser;
import l2e.gameserver.data.parser.ItemsParser;
import l2e.gameserver.data.parser.NpcsParser;
import l2e.gameserver.data.parser.PetsParser;
import l2e.gameserver.data.parser.PremiumAccountsParser;
import l2e.gameserver.data.parser.RecipeParser;
import l2e.gameserver.data.parser.SkillTreesParser;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.data.parser.SkillsParser.FrequentSkill;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.geodata.GeoEngine;
import l2e.gameserver.handler.itemhandlers.IItemHandler;
import l2e.gameserver.handler.itemhandlers.ItemHandler;
import l2e.gameserver.handler.voicedcommandhandlers.IVoicedCommandHandler;
import l2e.gameserver.handler.voicedcommandhandlers.VoicedCommandHandler;
import l2e.gameserver.idfactory.IdFactory;
import l2e.gameserver.instancemanager.BotCheckManager;
import l2e.gameserver.instancemanager.BotCheckManager.BotCheckQuestion;
import l2e.gameserver.instancemanager.CastleManager;
import l2e.gameserver.instancemanager.ClanHallManager;
import l2e.gameserver.instancemanager.CursedWeaponsManager;
import l2e.gameserver.instancemanager.DailyRewardManager;
import l2e.gameserver.instancemanager.DailyTaskManager;
import l2e.gameserver.instancemanager.DoubleSessionManager;
import l2e.gameserver.instancemanager.DuelManager;
import l2e.gameserver.instancemanager.EpicBossManager;
import l2e.gameserver.instancemanager.FortManager;
import l2e.gameserver.instancemanager.FortSiegeManager;
import l2e.gameserver.instancemanager.HandysBlockCheckerManager;
import l2e.gameserver.instancemanager.MailManager;
import l2e.gameserver.instancemanager.MatchingRoomManager;
import l2e.gameserver.instancemanager.OnlineRewardManager;
import l2e.gameserver.instancemanager.PunishmentManager;
import l2e.gameserver.instancemanager.QuestManager;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.instancemanager.RevengeManager;
import l2e.gameserver.instancemanager.RewardManager;
import l2e.gameserver.instancemanager.SiegeManager;
import l2e.gameserver.instancemanager.TerritoryWarManager;
import l2e.gameserver.instancemanager.VipManager;
import l2e.gameserver.instancemanager.ZoneManager;
import l2e.gameserver.instancemanager.games.krateiscube.model.Arena;
import l2e.gameserver.instancemanager.mods.OfflineTaskManager;
import l2e.gameserver.instancemanager.mods.TimeSkillsTaskManager;
import l2e.gameserver.listener.player.OnAnswerListener;
import l2e.gameserver.listener.player.PlayerListenerList;
import l2e.gameserver.listener.player.impl.BotCheckAnswerListner;
import l2e.gameserver.model.AccessLevel;
import l2e.gameserver.model.ArenaParticipantsHolder;
import l2e.gameserver.model.BlockedList;
import l2e.gameserver.model.CategoryType;
import l2e.gameserver.model.Clan;
import l2e.gameserver.model.ClanMember;
import l2e.gameserver.model.ContactList;
import l2e.gameserver.model.EnchantSkillLearn;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.Macro;
import l2e.gameserver.model.MacroList;
import l2e.gameserver.model.MountType;
import l2e.gameserver.model.Party;
import l2e.gameserver.model.Party.messageType;
import l2e.gameserver.model.PcCondOverride;
import l2e.gameserver.model.PetData;
import l2e.gameserver.model.PlayerGroup;
import l2e.gameserver.model.Radar;
import l2e.gameserver.model.RecipeList;
import l2e.gameserver.model.Request;
import l2e.gameserver.model.ShortCuts;
import l2e.gameserver.model.ShotType;
import l2e.gameserver.model.SkillLearn;
import l2e.gameserver.model.TeleportWhereType;
import l2e.gameserver.model.TerritoryWard;
import l2e.gameserver.model.TimeStamp;
import l2e.gameserver.model.TradeItem;
import l2e.gameserver.model.TradeList;
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.appearance.PcAppearance;
import l2e.gameserver.model.actor.instance.AirShipInstance;
import l2e.gameserver.model.actor.instance.BoatInstance;
import l2e.gameserver.model.actor.instance.ControlTowerInstance;
import l2e.gameserver.model.actor.instance.CubicInstance;
import l2e.gameserver.model.actor.instance.DefenderInstance;
import l2e.gameserver.model.actor.instance.DoorInstance;
import l2e.gameserver.model.actor.instance.EventChestInstance;
import l2e.gameserver.model.actor.instance.EventMonsterInstance;
import l2e.gameserver.model.actor.instance.FortBallistaInstance;
import l2e.gameserver.model.actor.instance.FortCommanderInstance;
import l2e.gameserver.model.actor.instance.FriendlyMobInstance;
import l2e.gameserver.model.actor.instance.GuardInstance;
import l2e.gameserver.model.actor.instance.PetInstance;
import l2e.gameserver.model.actor.instance.StaticObjectInstance;
import l2e.gameserver.model.actor.instance.TamedBeastInstance;
import l2e.gameserver.model.actor.instance.TrapInstance;
import l2e.gameserver.model.actor.instance.player.AchievementCounters;
import l2e.gameserver.model.actor.instance.player.AutoFarmOptions;
import l2e.gameserver.model.actor.instance.player.BypassStorage;
import l2e.gameserver.model.actor.instance.player.CharacterVariable;
import l2e.gameserver.model.actor.instance.player.DonateRates;
import l2e.gameserver.model.actor.instance.player.NevitSystem;
import l2e.gameserver.model.actor.instance.player.PersonalTasks;
import l2e.gameserver.model.actor.instance.player.PremiumBonus;
import l2e.gameserver.model.actor.instance.player.Recommendation;
import l2e.gameserver.model.actor.instance.player.impl.AutoSaveTask;
import l2e.gameserver.model.actor.instance.player.impl.CheckBotTask;
import l2e.gameserver.model.actor.instance.player.impl.CleanUpTask;
import l2e.gameserver.model.actor.instance.player.impl.DismountTask;
import l2e.gameserver.model.actor.instance.player.impl.FallingTask;
import l2e.gameserver.model.actor.instance.player.impl.FameTask;
import l2e.gameserver.model.actor.instance.player.impl.HardWareTask;
import l2e.gameserver.model.actor.instance.player.impl.InventoryEnableTask;
import l2e.gameserver.model.actor.instance.player.impl.LookingForFishTask;
import l2e.gameserver.model.actor.instance.player.impl.OnlineRewardTask;
import l2e.gameserver.model.actor.instance.player.impl.PcPointsTask;
import l2e.gameserver.model.actor.instance.player.impl.PetFeedTask;
import l2e.gameserver.model.actor.instance.player.impl.PremiumItemTask;
import l2e.gameserver.model.actor.instance.player.impl.PremiumTask;
import l2e.gameserver.model.actor.instance.player.impl.PunishmentTask;
import l2e.gameserver.model.actor.instance.player.impl.PvPFlagTask;
import l2e.gameserver.model.actor.instance.player.impl.RemoveWearItemsTask;
import l2e.gameserver.model.actor.instance.player.impl.RentPetTask;
import l2e.gameserver.model.actor.instance.player.impl.ResetChargesTask;
import l2e.gameserver.model.actor.instance.player.impl.ResetSoulsTask;
import l2e.gameserver.model.actor.instance.player.impl.ReviveTask;
import l2e.gameserver.model.actor.instance.player.impl.SitDownTask;
import l2e.gameserver.model.actor.instance.player.impl.StandUpTask;
import l2e.gameserver.model.actor.instance.player.impl.TeleportTask;
import l2e.gameserver.model.actor.instance.player.impl.TempHeroTask;
import l2e.gameserver.model.actor.instance.player.impl.VitalityTask;
import l2e.gameserver.model.actor.instance.player.impl.WarnUserTakeBreakTask;
import l2e.gameserver.model.actor.instance.player.impl.WaterTask;
import l2e.gameserver.model.actor.protection.AdminProtection;
import l2e.gameserver.model.actor.stat.PcStat;
import l2e.gameserver.model.actor.status.PcStatus;
import l2e.gameserver.model.actor.tasks.player.AcpTask;
import l2e.gameserver.model.actor.templates.BookmarkTemplate;
import l2e.gameserver.model.actor.templates.DressArmorTemplate;
import l2e.gameserver.model.actor.templates.DressCloakTemplate;
import l2e.gameserver.model.actor.templates.DressHatTemplate;
import l2e.gameserver.model.actor.templates.DressShieldTemplate;
import l2e.gameserver.model.actor.templates.DressWeaponTemplate;
import l2e.gameserver.model.actor.templates.ManufactureItemTemplate;
import l2e.gameserver.model.actor.templates.PetLevelTemplate;
import l2e.gameserver.model.actor.templates.PremiumItemTemplate;
import l2e.gameserver.model.actor.templates.ShortCutTemplate;
import l2e.gameserver.model.actor.templates.daily.DailyTaskTemplate;
import l2e.gameserver.model.actor.templates.items.Armor;
import l2e.gameserver.model.actor.templates.items.EtcItem;
import l2e.gameserver.model.actor.templates.items.Henna;
import l2e.gameserver.model.actor.templates.items.Item;
import l2e.gameserver.model.actor.templates.items.Weapon;
import l2e.gameserver.model.actor.templates.npc.DamageLimit;
import l2e.gameserver.model.actor.templates.player.AchiveTemplate;
import l2e.gameserver.model.actor.templates.player.FakeLocTemplate;
import l2e.gameserver.model.actor.templates.player.FakePassiveLocTemplate;
import l2e.gameserver.model.actor.templates.player.PcTeleportTemplate;
import l2e.gameserver.model.actor.templates.player.PcTemplate;
import l2e.gameserver.model.actor.templates.player.PlayerTaskTemplate;
import l2e.gameserver.model.actor.templates.player.ranking.PartyTemplate;
import l2e.gameserver.model.actor.templates.player.vip.VipTemplate;
import l2e.gameserver.model.actor.transform.Transform;
import l2e.gameserver.model.actor.transform.TransformTemplate;
import l2e.gameserver.model.base.BonusType;
import l2e.gameserver.model.base.ClassId;
import l2e.gameserver.model.base.ClassLevel;
import l2e.gameserver.model.base.Language;
import l2e.gameserver.model.base.PlayerClass;
import l2e.gameserver.model.base.Race;
import l2e.gameserver.model.base.ShortcutType;
import l2e.gameserver.model.base.SubClass;
import l2e.gameserver.model.entity.Castle;
import l2e.gameserver.model.entity.Duel;
import l2e.gameserver.model.entity.Fort;
import l2e.gameserver.model.entity.Message;
import l2e.gameserver.model.entity.Reflection;
import l2e.gameserver.model.entity.Siege;
import l2e.gameserver.model.entity.auction.AuctionsManager;
import l2e.gameserver.model.entity.events.AbstractFightEvent;
import l2e.gameserver.model.entity.events.cleft.AerialCleftEvent;
import l2e.gameserver.model.entity.events.custom.achievements.AchievementManager;
import l2e.gameserver.model.entity.events.model.FightEventManager;
import l2e.gameserver.model.entity.events.model.impl.MonsterAttackEvent;
import l2e.gameserver.model.entity.events.model.template.FightEventGameRoom;
import l2e.gameserver.model.entity.events.tournaments.Tournament;
import l2e.gameserver.model.entity.events.tournaments.TournamentData;
import l2e.gameserver.model.entity.events.tournaments.util.TournamentUtil;
import l2e.gameserver.model.entity.mods.SellBuffsManager;
import l2e.gameserver.model.entity.underground_coliseum.UCTeam;
import l2e.gameserver.model.fishing.Fish;
import l2e.gameserver.model.fishing.Fishing;
import l2e.gameserver.model.holders.ItemHolder;
import l2e.gameserver.model.holders.SellBuffHolder;
import l2e.gameserver.model.holders.SkillHolder;
import l2e.gameserver.model.holders.SkillUseHolder;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.items.itemcontainer.Inventory;
import l2e.gameserver.model.items.itemcontainer.ItemContainer;
import l2e.gameserver.model.items.itemcontainer.PcFreight;
import l2e.gameserver.model.items.itemcontainer.PcInventory;
import l2e.gameserver.model.items.itemcontainer.PcPrivateInventory;
import l2e.gameserver.model.items.itemcontainer.PcRefund;
import l2e.gameserver.model.items.itemcontainer.PcWarehouse;
import l2e.gameserver.model.items.itemcontainer.PetInventory;
import l2e.gameserver.model.items.multisell.PreparedListContainer;
import l2e.gameserver.model.items.type.ActionType;
import l2e.gameserver.model.items.type.ArmorType;
import l2e.gameserver.model.items.type.EtcItemType;
import l2e.gameserver.model.items.type.WeaponType;
import l2e.gameserver.model.matching.MatchingRoom;
import l2e.gameserver.model.olympiad.AbstractOlympiadGame;
import l2e.gameserver.model.olympiad.OlympiadGameManager;
import l2e.gameserver.model.olympiad.OlympiadManager;
import l2e.gameserver.model.petition.PetitionMainGroup;
import l2e.gameserver.model.punishment.PunishmentTemplate;
import l2e.gameserver.model.punishment.PunishmentType;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestEventType;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.quest.State;
import l2e.gameserver.model.service.academy.AcademyList;
import l2e.gameserver.model.service.autoenchant.EnchantParams;
import l2e.gameserver.model.service.autofarm.FarmSettings;
import l2e.gameserver.model.service.buffer.PlayerScheme;
import l2e.gameserver.model.service.premium.PremiumGift;
import l2e.gameserver.model.service.premium.PremiumTemplate;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.skills.SkillType;
import l2e.gameserver.model.skills.effects.AbnormalEffect;
import l2e.gameserver.model.skills.effects.Effect;
import l2e.gameserver.model.skills.effects.EffectFlag;
import l2e.gameserver.model.skills.effects.EffectType;
import l2e.gameserver.model.skills.l2skills.SkillSiegeFlag;
import l2e.gameserver.model.skills.targets.TargetType;
import l2e.gameserver.model.stats.BaseStats;
import l2e.gameserver.model.stats.Formulas;
import l2e.gameserver.model.stats.Stats;
import l2e.gameserver.model.strings.server.ServerMessage;
import l2e.gameserver.model.strings.server.ServerStorage;
import l2e.gameserver.model.zone.ZoneId;
import l2e.gameserver.model.zone.ZoneType;
import l2e.gameserver.model.zone.type.BossZone;
import l2e.gameserver.model.zone.type.FunPvpZone;
import l2e.gameserver.model.zone.type.JailZone;
import l2e.gameserver.model.zone.type.SiegeZone;
import l2e.gameserver.model.zone.type.WaterZone;
import l2e.gameserver.network.GameClient;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.communication.AuthServerCommunication;
import l2e.gameserver.network.serverpackets.*;
import l2e.gameserver.network.serverpackets.FlyToLocation.FlyType;
import l2e.gameserver.network.serverpackets.Interface.ExAbnormalStatusUpdateFromTarget;
import l2e.gameserver.network.serverpackets.pledge.PledgeShowMemberListDelete;
import l2e.gameserver.network.serverpackets.pledge.PledgeShowMemberListDeleteAll;
import l2e.gameserver.network.serverpackets.pledge.PledgeShowMemberListUpdate;
import l2e.gameserver.network.serverpackets.updatetype.UserInfoType;
import l2e.gameserver.taskmanager.AttackStanceTaskManager;
import l2e.gameserver.taskmanager.ItemsAutoDestroy;
import top.jsoft.jguard.JGuard;

public class Player extends Playable implements PlayerGroup
{
	public static final int ID_NONE = -1;
	public static final int REQUEST_TIMEOUT = 15;
	public static final int STORE_PRIVATE_NONE = 0;
	public static final int STORE_PRIVATE_SELL = 1;
	public static final int STORE_PRIVATE_BUY = 3;
	public static final int STORE_PRIVATE_MANUFACTURE = 5;
	public static final int STORE_PRIVATE_PACKAGE_SELL = 8;
	
	private int _pledgeItemId = 0;
	private long _pledgePrice = 0;
	private boolean _isInAcademyList = false;
	private int _botRating;
	private GameClient _client;
	private final String _accountName;
	private String _lang;
	private long _deleteTimer;
	private volatile boolean _isOnline = false;
	private boolean _isLogout = false;
	private long _onlineTime;
	private long _onlineBeginTime;
	private long _lastAccess;
	private long _uptime;
	private int _ping = -1;
	private int _baseClass;
	private int _activeClass;
	private int _classIndex = 0;
	private int _controlItemId;
	private PetData _data;
	private PetLevelTemplate _leveldata;
	private int _curFeed;
	private boolean _petItems = false;
	private final PcAppearance _appearance;
	private long _expBeforeDeath;
	private int _karma;
	private int _pvpKills;
	private int _pkKills;
	private byte _pvpFlag;
	private int _fame;
	private int _pcBangPoints = 0;
	private long _gamePoints;
	private byte _siegeState = 0;
	private int _siegeSide = 0;
	private int _curWeightPenalty = 0;
	private int _zoneMask;
	private boolean _isIn7sDungeon = false;
	private boolean _canFeed;
	private boolean _isInSiege;
	private boolean _isInHideoutSiege = false;
	private boolean _isInPvpFunZone = false;
	private PcTemplate _antifeedTemplate = null;
	private boolean _antifeedSex;
	private int _bookmarkslot = 0;
	private Vehicle _vehicle = null;
	private Location _inVehiclePosition;
	private MountType _mountType = MountType.NONE;
	private int _mountNpcId;
	private int _mountLevel;
	private int _mountObjectID = 0;
	private int _telemode = 0;
	private boolean _inCrystallize;
	private boolean _inCraftMode;
	private boolean _inInStoreNow = false;
	private boolean _offline = false;
	private Transform _transformation;
	private boolean _waitTypeSitting;
	private StaticObjectInstance _sittingObject;
	private int _lastX;
	private int _lastY;
	private int _lastZ;
	private boolean _observerMode = false;
	private final Recommendation _recommendation = new Recommendation(this);
	private final NevitSystem _nevitSystem = new NevitSystem(this);
	private final AutoFarmOptions _autoFarmSystem = new AutoFarmOptions(this);
	private final BypassStorage _bypassStorage = new BypassStorage();
	private final PcInventory _inventory = new PcInventory(this);
	private final PcFreight _freight = new PcFreight(this);
	private PcWarehouse _warehouse;
	private PcPrivateInventory _privateInventory;
	private PcRefund _refund;
	private int _privatestore;
	private TradeList _activeTradeList;
	private ItemContainer _activeWarehouse;
	private String _storeName = "";
	private TradeList _sellList;
	private TradeList _buyList;
	private PreparedListContainer _currentMultiSell = null;
	private int _newbie;
	private boolean _noble = false;
	private boolean _hero = false;
	private Npc _lastFolkNpc = null;
	private int _questNpcObject = 0;
	private int _hennaSTR;
	private int _hennaINT;
	private int _hennaDEX;
	private int _hennaMEN;
	private int _hennaWIT;
	private int _hennaCON;
	private Summon _summon = null;
	private Decoy _decoy = null;
	private TrapInstance _trap = null;
	private int _agathionId = 0;
	private boolean _minimapAllowed = false;
	private final Radar _radar;
	private int _clanId;
	private Clan _clan;
	private int _apprentice = 0;
	private int _sponsor = 0;
	private long _clanJoinExpiryTime;
	private long _clanCreateExpiryTime;
	private int _powerGrade = 0;
	private int _clanPrivileges = 0;
	private int _pledgeClass = 0;
	private int _pledgeType = 0;
	private int _lvlJoinedAcademy = 0;
	private int _wantsPeace = 0;
	private long _lastMovePacket = 0;
	private long _lastAttackPacket = 0;
	private long _lastRequestMagicPacket = 0;
	private int _deathPenaltyBuffLevel = 0;
	private final AtomicInteger _charges = new AtomicInteger();
	private boolean _inOlympiadMode = false;
	private boolean _OlympiadStart = false;
	public List<Skill> _olyRestoreSkills = new ArrayList<>();
	public List<Skill> _olyDeleteSkills = new ArrayList<>();
	private AbstractOlympiadGame _olympiadGame;
	private int _olympiadGameId = -1;
	private int _olympiadSide = -1;
	public int olyBuff = 0;
	private boolean _isInDuel = false;
	private int _duelState = Duel.DUELSTATE_NODUEL;
	private int _duelId = 0;
	private int _souls = 0;
	private long _premiumOnlineTime = 0;
	private int _polyId;
	private Map<Stats, Double> _servitorShare;
	private Map<Stats, Double> _petShare;
	private Location _fallingLoc = null;
	private Location _saveLoc = null;
	private Location _bookmarkLocation = null;
	
	private final Map<Integer, PlayerTaskTemplate> _activeTasks = new ConcurrentHashMap<>();
	
	private Future<?> _broadcastCharInfoTask;
	private Future<?> _broadcastStatusUpdateTask;
	private Future<?> _effectsUpdateTask;
	private Future<?> _updateAndBroadcastStatusTask;
	private Future<?> _acpTask;
	private Future<?> _userInfoTask;
	public Future<?> _captureTask;
	
	private Pair<Integer, OnAnswerListener> _askDialog = null;
	private Map<Integer, SubClass> _subClasses;
	private List<TamedBeastInstance> _tamedBeast = null;
	private volatile Map<Integer, ManufactureItemTemplate> _manufactureItems;
	private final Map<Integer, BookmarkTemplate> _tpbookmarks = new ConcurrentHashMap<>();
	private final Map<Integer, RecipeList> _dwarvenRecipeBook = new ConcurrentHashMap<>();
	private final Map<Integer, RecipeList> _commonRecipeBook = new ConcurrentHashMap<>();
	private final Map<Integer, PremiumItemTemplate> _premiumItems = new ConcurrentHashMap<>();
	private final Set<Player> _snoopListener = ConcurrentHashMap.newKeySet(1);
	private final Set<Player> _snoopedPlayer = ConcurrentHashMap.newKeySet(1);
	private final Map<String, QuestState> _quests = new ConcurrentHashMap<>();
	private final Map<String, CharacterVariable> _variables = new ConcurrentHashMap<>();
	private final Map<Integer, TimeStamp> _reuseTimeStampsItems = new ConcurrentHashMap<>();
	private final Map<Integer, TimeStamp> _reuseTimeStampsSkills = new ConcurrentHashMap<>();
	private final List<String> _bannedActions = new ArrayList<>();
	private final ShortCuts _shortCuts = new ShortCuts(this);
	private final MacroList _macros = new MacroList(this);
	private Henna[] _henna = new Henna[3];
	private long _createDate;
	private FakePlayer _fakePlayerUnderControl = null;
	private boolean _fakePlayer = false;
	private FakeLocTemplate _fakeLocation = null;
	private FakePassiveLocTemplate _fakePassiveLocation = null;
	private ContactList _contactList = null;
	private final Map<Integer, PcTeleportTemplate> _communtyTeleports = new ConcurrentHashMap<>();
	private final Map<Integer, Integer> _achievementLevels = new ConcurrentHashMap<>();
	private final AchievementCounters _achievementCounters = new AchievementCounters(this);
	private SystemMessageId _noDuelReason = SystemMessageId.THERE_IS_NO_OPPONENT_TO_RECEIVE_YOUR_CHALLENGE_FOR_A_DUEL;
	private final int[] _charInfoSlots = new int[11];
	private final int[] _userInfoSlots = new int[11];
	private final int[] _previewSlots = new int[11];
	
	private Location _currentSkillWorldPosition;
	private AccessLevel _accessLevel;

	private boolean _messageRefusal = false;
	private boolean _silenceMode = false;
	private List<Integer> _silenceModeExcluded;
	private boolean _dietMode = false;
	private boolean _exchangeRefusal = false;

	private Party _party;
	private Player _activeRequester;
	private long _requestExpireTime = 0;
	private final Request _request = new Request(this);
	private ItemInstance _arrowItem;
	private ItemInstance _boltItem;

	private long _protectEndTime = 0;
	private long _respawnProtectTime = 0;
	private int _lectureMark;

	public int expertiseIndex = 0;

	public static final int[] EXPERTISE_LEVELS =
	{
	        0, 20, 40, 52, 61, 76, 80, 84, Integer.MAX_VALUE
	};

	private boolean _matchingRoomWindowOpened = false;
	private MatchingRoom _matchingRoom;
	private PetitionMainGroup _petitionGroup;

	private final List<Integer> _loadedImages = new ArrayList<>();
	private final List<ItemInstance> _eventItems = new ArrayList<>();
	
	public boolean isSpawnProtected()
	{
		return _protectEndTime > GameTimeController.getInstance().getGameTicks();
	}
	
	public boolean isRespawnProtected()
	{
		return _respawnProtectTime > System.currentTimeMillis();
	}
	
	public void setRespawnProtect()
	{
		_respawnProtectTime = System.currentTimeMillis() + 5000L;
	}

	private long _teleportProtectEndTime = 0;

	public boolean isTeleportProtected()
	{
		return _teleportProtectEndTime > GameTimeController.getInstance().getGameTicks();
	}

	private long _recentFakeDeathEndTime = 0;
	private boolean _isFakeDeath;

	private Weapon _fistsWeaponItem;

	private final Map<Integer, String> _chars = new LinkedHashMap<>();

	private int _expertiseArmorPenalty = 0;
	private int _expertiseWeaponPenalty = 0;

	private boolean _isEnchanting = false;
	private int _activeEnchantItemId = ID_NONE;
	private int _activeEnchantSupportItemId = ID_NONE;
	private int _activeEnchantAttrItemId = ID_NONE;

	protected boolean _inventoryDisable = false;

	private final Map<Integer, CubicInstance> _cubics = new ConcurrentSkipListMap<>();

	protected Set<Integer> _activeSoulShots = ConcurrentHashMap.newKeySet(1);

	private final ReentrantLock _subclassLock = new ReentrantLock();
	public final ReentrantLock soulShotLock = new ReentrantLock();
	public final ReentrantReadWriteLock _useItemLock = new ReentrantReadWriteLock();

	private int UCKills = 0;
	private int UCDeaths = 0;
	public static final int UC_STATE_NONE = 0;
	public static final int UC_STATE_POINT = 1;
	public static final int UC_STATE_ARENA = 2;
	private int UCState = 0;
	private Arena _arena = null;
	private byte _handysBlockCheckerEventArena = -1;

	private final int _loto[] = new int[5];
	private final int _race[] = new int[2];

	private final BlockedList _blockList = new BlockedList(this);

	private Fishing _fishCombat;
	private boolean _fishing = false;
	private int _fishx = 0;
	private int _fishy = 0;
	private int _fishz = 0;

	private Set<Integer> _transformAllowedSkills;
	private final Set<Integer> _transformNotCheckSkills = new HashSet<>();

	private SkillUseHolder _currentSkill;
	private SkillUseHolder _currentPetSkill;
	private SkillUseHolder _queuedSkill;

	private int _cursedWeaponEquippedId = 0;
	private boolean _isWithoutCursed = false;
	private boolean _combatFlagEquippedId = false;

	private int _reviveRequested = 0;
	private double _revivePower = 0;
	private boolean _revivePet = false;

	private double _cpUpdateIncCheck = .0;
	private double _cpUpdateDecCheck = .0;
	private double _cpUpdateInterval = .0;
	private double _mpUpdateIncCheck = .0;
	private double _mpUpdateDecCheck = .0;
	private double _mpUpdateInterval = .0;
	private Location _clientLocation;

	private volatile long _fallingTimestamp = 0;

	private int _multiSocialTarget = 0;
	private int _multiSociaAction = 0;

	private int _movieId = 0;

	private String _adminConfirmCmd = null;

	private volatile long _lastItemAuctionInfoRequest = 0;

	private long _pvpFlagLasts;

	private long _notMoveUntil = 0;

	private Map<Integer, Skill> _customSkills = null;

	private int _kamaID = 0;

	private AdminProtection _AdminProtection = null;

	private boolean _canRevive = true;

	private final Map<String, Object> quickVars = new ConcurrentHashMap<>();
	
	private boolean _isSellingBuffs = false;
	private List<SellBuffHolder> _sellingBuffs = null;
	
	protected int _cleftKills = 0;
	protected int _cleftDeaths = 0;
	protected int _cleftKillTowers = 0;
	protected boolean _cleftCat = false;
	
	private long _lastNotAfkTime = 0;
	private FightEventGameRoom _fightEventGameRoom = null;
	
	private long _hpReuseDelay;
	private long _hpPotionDelay;
	private long _mpReuseDelay;
	private long _mpPotionDelay;
	private long _cpReuseDelay;
	private long _cpPotionDelay;
	private long _soulReuseDelay;
	private long _soulPotionDelay;
	private long _hardWareDelay;
	
	private int _chatMsg = 0;
	
	private final List<PlayerScheme> _buffSchemes = new CopyOnWriteArrayList<>();
	
	private final PremiumBonus _bonus = new PremiumBonus(this);
	private final PersonalTasks _tasks = new PersonalTasks(this);
	private final DonateRates _donateRates = new DonateRates(this);
	private final EnchantParams _enchantParams = new EnchantParams();
	
	private final List<Integer> _weaponSkins = new ArrayList<>();
	private final List<Integer> _armorSkins = new ArrayList<>();
	private final List<Integer> _shieldSkins = new ArrayList<>();
	private final List<Integer> _cloakSkins = new ArrayList<>();
	private final List<Integer> _hairSkins = new ArrayList<>();
	private DressWeaponTemplate _activeWeaponSkin;
	private DressArmorTemplate _activeArmorSkin;
	private DressShieldTemplate _activeShieldSkin;
	private DressCloakTemplate _activeCloakSkin;
	private DressHatTemplate _activeHairSkin;
	private DressHatTemplate _activeMaskSkin;
	private boolean _activeWeapon;
	private boolean _activeArmor;
	private boolean _activeShield;
	private boolean _activeCloak;
	private boolean _activeHair;
	private boolean _activeMask;
	private int _vipLevel = 0;
	private long _vipPoints = 0L;
	public boolean _entering = true;
	private boolean _IsInDrawZone = false;
	private final List<Location> _drawCoords = new ArrayList<>();
	
	private final List<Integer> _revengeList = new ArrayList<>();
	private boolean _isRevengeActive = false;
	
	private String _cacheIp = "";
	private String _cacheHwid = "";
	
	@Override
	public void doAttack(Creature target)
	{
		if (target != null && target.isPlayer() && isPKProtected(target.getActingPlayer()))
		{
			sendMessage("You can't attack this player!");
			sendActionFailed();
			return;
		}
		super.doAttack(target);
		setRecentFakeDeath(false);
	}
	
	@Override
	public void doCast(Skill skill)
	{
		super.doCast(skill);
		setRecentFakeDeath(false);
	}
	
	public void setPvpFlagLasts(long time)
	{
		_pvpFlagLasts = time;
	}

	public long getPvpFlagLasts()
	{
		return _pvpFlagLasts;
	}

	public void startPvPFlag()
	{
		if (ZoneManager.getInstance().getOlympiadStadium(this) != null)
		{
			return;
		}
		updatePvPFlag(1);
		
		if (getFarmSystem().isAutofarming())
		{
			getFarmSystem().stopFarmTask(false);
		}
		getPersonalTasks().addTask(new PvPFlagTask(1000L));
	}

	public void stopPvpRegTask()
	{
		getPersonalTasks().removeTask(33, true);
	}

	public void stopPvPFlag()
	{
		stopPvpRegTask();
		updatePvPFlag(0);
	}

	private boolean _married = false;
	private int _partnerId = 0;
	private int _coupleId = 0;
	private boolean _engagerequest = false;
	private int _engageid = 0;
	private boolean _marryrequest = false;
	private boolean _marryaccepted = false;
	private String _lastPetitionGmName = null;

	protected static class SummonRequest
	{
		private Player _target = null;
		private Skill _skill = null;

		public void setTarget(Player destination, Skill skill)
		{
			_target = destination;
			_skill = skill;
		}

		public Player getTarget()
		{
			return _target;
		}

		public Skill getSkill()
		{
			return _skill;
		}
	}

	public static Player create(PcTemplate template, String accountName, String name, PcAppearance app)
	{
		final Player player = new Player(IdFactory.getInstance().getNextId(), template, accountName, app);

		player.setGlobalName(name);
		player.setGlobalTitle("");
		player.setCreateDate(System.currentTimeMillis());
		player.setBaseClass(player.getClassId());
		player.setNewbie(1);
		player.getRecommendation().setRecomTimeLeft(3600);
		player.getRecommendation().setRecomLeft(20);
		player.getNevitSystem().restartSystem();
		player.setChatMsg(Config.CHAT_MSG_SIMPLE);
		
		return CharacterDAO.getInstance().isPlayerCreated(player) ? player : null;
	}

	public String getAccountName()
	{
		if (getClient() == null)
		{
			return getAccountNamePlayer();
		}
		return getClient().getLogin();
	}

	public String getAccountNamePlayer()
	{
		return _accountName;
	}

	public Map<Integer, String> getAccountChars()
	{
		return _chars;
	}

	public int getRelation(Player target)
	{
		int result = 0;

		if (getClan() != null)
		{
			result |= RelationChanged.RELATION_CLAN_MEMBER;
			if (getClan() == target.getClan())
			{
				result |= RelationChanged.RELATION_CLAN_MATE;
			}
			if (getAllyId() != 0)
			{
				result |= RelationChanged.RELATION_ALLY_MEMBER;
			}
		}
		if (isClanLeader())
		{
			result |= RelationChanged.RELATION_LEADER;
		}
		if ((getParty() != null) && (getParty() == target.getParty()))
		{
			result |= RelationChanged.RELATION_HAS_PARTY;
			for (int i = 0; i < getParty().getMembers().size(); i++)
			{
				if (getParty().getMembers().get(i) != this)
				{
					continue;
				}
				switch (i)
				{
					case 0 :
						result |= RelationChanged.RELATION_PARTYLEADER;
						break;
					case 1 :
						result |= RelationChanged.RELATION_PARTY4;
						break;
					case 2 :
						result |= RelationChanged.RELATION_PARTY3 + RelationChanged.RELATION_PARTY2 + RelationChanged.RELATION_PARTY1;
						break;
					case 3 :
						result |= RelationChanged.RELATION_PARTY3 + RelationChanged.RELATION_PARTY2;
						break;
					case 4 :
						result |= RelationChanged.RELATION_PARTY3 + RelationChanged.RELATION_PARTY1;
						break;
					case 5 :
						result |= RelationChanged.RELATION_PARTY3;
						break;
					case 6 :
						result |= RelationChanged.RELATION_PARTY2 + RelationChanged.RELATION_PARTY1;
						break;
					case 7 :
						result |= RelationChanged.RELATION_PARTY2;
						break;
					case 8 :
						result |= RelationChanged.RELATION_PARTY1;
						break;
				}
			}
		}
		if (getSiegeState() != 0)
		{
			if (TerritoryWarManager.getInstance().getRegisteredTerritoryId(this) != 0)
			{
				result |= RelationChanged.RELATION_TERRITORY_WAR;
			}
			else
			{
				result |= RelationChanged.RELATION_INSIEGE;
				if (getSiegeState() != target.getSiegeState())
				{
					result |= RelationChanged.RELATION_ENEMY;
				}
				else
				{
					result |= RelationChanged.RELATION_ALLY;
				}
				if (getSiegeState() == 1)
				{
					result |= RelationChanged.RELATION_ATTACKER;
				}
			}
		}
		if ((getClan() != null) && (target.getClan() != null))
		{
			if ((target.getPledgeType() != Clan.SUBUNIT_ACADEMY) && (getPledgeType() != Clan.SUBUNIT_ACADEMY) && target.getClan().isAtWarWith(getClan().getId()))
			{
				result |= RelationChanged.RELATION_1SIDED_WAR;
				if (getClan().isAtWarWith(target.getClan().getId()))
				{
					result |= RelationChanged.RELATION_MUTUAL_WAR;
				}
			}
		}
		if (getBlockCheckerArena() != -1)
		{
			result |= RelationChanged.RELATION_INSIEGE;
			final ArenaParticipantsHolder holder = HandysBlockCheckerManager.getInstance().getHolder(getBlockCheckerArena());
			if (holder.getPlayerTeam(this) == 0)
			{
				result |= RelationChanged.RELATION_ENEMY;
			}
			else
			{
				result |= RelationChanged.RELATION_ALLY;
			}
			result |= RelationChanged.RELATION_ATTACKER;
		}
		
		if (target.isInsideZone(ZoneId.FUN_PVP) && !isFriend(target))
		{
			final FunPvpZone zone = ZoneManager.getInstance().getZone(target, FunPvpZone.class);
			if (zone != null && zone.isEnableRelation())
			{
				result |= RelationChanged.RELATION_1SIDED_WAR;
				result |= RelationChanged.RELATION_MUTUAL_WAR;
			}
		}

		for (final AbstractFightEvent e : getFightEvents())
		{
			result = e.getRelation(this, target, result);
		}
		return result;
	}

	public static Player load(int objectId)
	{
		return CharacterDAO.getInstance().restore(objectId);
	}

	private void initPcStatusUpdateValues()
	{
		_cpUpdateInterval = getMaxCp() / 352.0;
		_cpUpdateIncCheck = getMaxCp();
		_cpUpdateDecCheck = getMaxCp() - _cpUpdateInterval;
		_mpUpdateInterval = getMaxMp() / 352.0;
		_mpUpdateIncCheck = getMaxMp();
		_mpUpdateDecCheck = getMaxMp() - _mpUpdateInterval;
	}

	public Player(int objectId, PcTemplate template, String accountName, PcAppearance app)
	{
		super(objectId, template);
		setInstanceType(InstanceType.Player);
		super.initCharStatusUpdateValues();
		initPcStatusUpdateValues();

		_accountName = accountName;
		app.setOwner(this);
		_appearance = app;

		getAI();

		_radar = new Radar(this);
	}

	@Override
	public final PcStat getStat()
	{
		return (PcStat) super.getStat();
	}

	@Override
	public void initCharStat()
	{
		setStat(new PcStat(this));
	}

	@Override
	public final PcStatus getStatus()
	{
		return (PcStatus) super.getStatus();
	}

	@Override
	public void initCharStatus()
	{
		setStatus(new PcStatus(this));
	}

	public final PcAppearance getAppearance()
	{
		return _appearance;
	}

	public final PcTemplate getBaseTemplate()
	{
		return CharTemplateParser.getInstance().getTemplate(_baseClass);
	}

	@Override
	public final PcTemplate getTemplate()
	{
		return (PcTemplate) super.getTemplate();
	}

	public void setTemplate(ClassId newclass)
	{
		super.setTemplate(CharTemplateParser.getInstance().getTemplate(newclass));
	}

	@Override
	public CharacterAI initAI()
	{
		return new PlayerAI(this);
	}

	@Override
	public final int getLevel()
	{
		return getStat().getLevel();
	}

	@Override
	public double getLevelMod()
	{
		if (isTransformed())
		{
			final double levelMod = getTransformation().getLevelMod(this);
			if (levelMod > -1)
			{
				return levelMod;
			}
		}
		return (89. + getLevel()) / 100.0;
	}

	public int getNewbie()
	{
		return _newbie;
	}

	public void setNewbie(int newbieRewards)
	{
		_newbie = newbieRewards;
	}

	public void setBaseClass(int baseClass)
	{
		_baseClass = baseClass;
	}

	public void setBaseClass(ClassId classId)
	{
		_baseClass = classId.ordinal();
	}

	public boolean isInStoreMode()
	{
		return (getPrivateStoreType() > Player.STORE_PRIVATE_NONE);
	}
	
	public void setIsInStoreNow(boolean b)
	{
		_inInStoreNow = b;
	}
	
	public boolean isInStoreNow()
	{
		return _inInStoreNow;
	}

	public boolean isInCraftMode()
	{
		return _inCraftMode;
	}

	public void isInCraftMode(boolean b)
	{
		_inCraftMode = b;
	}

	public RecipeList[] getCommonRecipeBook()
	{
		return _commonRecipeBook.values().toArray(new RecipeList[_commonRecipeBook.values().size()]);
	}

	public RecipeList[] getDwarvenRecipeBook()
	{
		return _dwarvenRecipeBook.values().toArray(new RecipeList[_dwarvenRecipeBook.values().size()]);
	}

	public void registerCommonRecipeList(RecipeList recipe, boolean saveToDb)
	{
		_commonRecipeBook.put(recipe.getId(), recipe);

		if (saveToDb)
		{
			insertNewRecipeParser(recipe.getId(), false);
		}
	}

	public void registerDwarvenRecipeList(RecipeList recipe, boolean saveToDb)
	{
		_dwarvenRecipeBook.put(recipe.getId(), recipe);

		if (saveToDb)
		{
			insertNewRecipeParser(recipe.getId(), true);
		}
	}

	public boolean hasRecipeList(int recipeId)
	{
		return _dwarvenRecipeBook.containsKey(recipeId) || _commonRecipeBook.containsKey(recipeId);
	}

	public void unregisterRecipeList(int recipeId)
	{
		if (_dwarvenRecipeBook.remove(recipeId) != null)
		{
			deleteRecipeParser(recipeId, true);
		}
		else if (_commonRecipeBook.remove(recipeId) != null)
		{
			deleteRecipeParser(recipeId, false);
		}
		else
		{
			_log.warn("Attempted to remove unknown RecipeList: " + recipeId);
		}

		for (final ShortCutTemplate sc : getAllShortCuts())
		{
			if ((sc != null) && (sc.getId() == recipeId) && (sc.getType() == ShortcutType.RECIPE))
			{
				deleteShortCut(sc.getSlot(), sc.getPage());
			}
		}
	}

	private void insertNewRecipeParser(int recipeId, boolean isDwarf)
	{
		Connection con = null;
		PreparedStatement statement = null;
 		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("INSERT INTO character_recipebook (charId, id, classIndex, type) values(?,?,?,?)");
			statement.setInt(1, getObjectId());
			statement.setInt(2, recipeId);
			statement.setInt(3, isDwarf ? _classIndex : 0);
			statement.setInt(4, isDwarf ? 1 : 0);
			statement.execute();
		}
		catch (final SQLException e)
		{
			_log.error("SQL exception while inserting recipe: " + recipeId + " from character " + getObjectId(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	private void deleteRecipeParser(int recipeId, boolean isDwarf)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("DELETE FROM character_recipebook WHERE charId=? AND id=? AND classIndex=?");
			statement.setInt(1, getObjectId());
			statement.setInt(2, recipeId);
			statement.setInt(3, isDwarf ? _classIndex : 0);
			statement.execute();
		}
		catch (final SQLException e)
		{
			_log.error("SQL exception while deleting recipe: " + recipeId + " from character " + getObjectId(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	public int getLastQuestNpcObject()
	{
		return _questNpcObject;
	}

	public void setLastQuestNpcObject(int npcId)
	{
		_questNpcObject = npcId;
	}

	public QuestState getQuestState(String quest)
	{
		return _quests.get(quest);
	}

	public void setQuestState(QuestState qs)
	{
		_quests.put(qs.getQuestName(), qs);
	}

	public boolean hasQuestState(String quest)
	{
		return _quests.containsKey(quest);
	}

	public void delQuestState(String quest)
	{
		_quests.remove(quest);
	}

	private QuestState[] addToQuestStateArray(QuestState[] questStateArray, QuestState state)
	{
		final int len = questStateArray.length;
		final QuestState[] tmp = new QuestState[len + 1];
		System.arraycopy(questStateArray, 0, tmp, 0, len);
		tmp[len] = state;
		return tmp;
	}

	public Quest[] getAllActiveQuests()
	{
		final List<Quest> quests = new ArrayList<>();
		for (final QuestState qs : _quests.values())
		{
			if ((qs == null) || (qs.getQuest() == null) || (!qs.isStarted() && !Config.DEVELOPER))
			{
				continue;
			}
			final int questId = qs.getQuest().getId();
			if ((questId > 19999) || (questId < 1))
			{
				continue;
			}
			quests.add(qs.getQuest());
		}
		return quests.toArray(new Quest[quests.size()]);
	}

	public QuestState[] getQuestsForAttacks(Npc npc)
	{
		QuestState[] states = null;

		for (final Quest quest : npc.getTemplate().getEventQuests(QuestEventType.ON_ATTACK))
		{
			if (getQuestState(quest.getName()) != null)
			{
				if (states == null)
				{
					states = new QuestState[]
					{
					        getQuestState(quest.getName())
					};
				}
				else
				{
					states = addToQuestStateArray(states, getQuestState(quest.getName()));
				}
			}
		}
		return states;
	}

	public QuestState[] getQuestsForKills(Npc npc)
	{
		QuestState[] states = null;

		for (final Quest quest : npc.getTemplate().getEventQuests(QuestEventType.ON_KILL))
		{
			if (getQuestState(quest.getName()) != null)
			{
				if (states == null)
				{
					states = new QuestState[]
					{
					        getQuestState(quest.getName())
					};
				}
				else
				{
					states = addToQuestStateArray(states, getQuestState(quest.getName()));
				}
			}
		}
		return states;
	}

	public QuestState[] getQuestsForTalk(int npcId)
	{
		QuestState[] states = null;

		final List<Quest> quests = NpcsParser.getInstance().getTemplate(npcId).getEventQuests(QuestEventType.ON_TALK);
		if (quests != null)
		{
			for (final Quest quest : quests)
			{
				if (quest != null)
				{
					if (getQuestState(quest.getName()) != null)
					{
						if (states == null)
						{
							states = new QuestState[]
							{
							        getQuestState(quest.getName())
							};
						}
						else
						{
							states = addToQuestStateArray(states, getQuestState(quest.getName()));
						}
					}
				}
			}
		}
		return states;
	}

	public QuestState processQuestEvent(String quest, String event)
	{
		QuestState retval = null;
		if (event == null)
		{
			event = "";
		}
		QuestState qs = getQuestState(quest);
		if ((qs == null) && event.isEmpty())
		{
			return retval;
		}
		if (qs == null)
		{
			final Quest q = QuestManager.getInstance().getQuest(quest);
			if (q == null)
			{
				return retval;
			}
			qs = q.newQuestState(this);
		}
		
		if (qs != null)
		{
			if (getLastQuestNpcObject() > 0)
			{
				final Npc npc = GameObjectsStorage.getNpc(getLastQuestNpcObject());
				if (npc != null && isInsideRadius(npc, Npc.INTERACTION_DISTANCE, false, false))
				{
					final QuestState[] states = getQuestsForTalk(npc.getId());
					if (states != null)
					{
						for (final QuestState state : states)
						{
							if (state.getQuest().getName().equals(qs.getQuest().getName()))
							{
								if (qs.getQuest().notifyEvent(event, npc, this))
								{
									showQuestWindow(quest, State.getStateName(qs.getState()));
								}
								retval = qs;
								break;
							}
						}
					}
				}
			}
		}
		return retval;
	}

	private void showQuestWindow(String questId, String stateId)
	{
		final String path = "data/html/scripts/quests/" + questId + "/" + stateId + ".htm";
		final String content = HtmCache.getInstance().getHtm(this, getLang(), path);

		if (content != null)
		{
			final NpcHtmlMessage npcReply = new NpcHtmlMessage(5);
			npcReply.setHtml(this, content);
			sendPacket(npcReply);
		}

		sendActionFailed();
	}

	private volatile List<QuestState> _notifyQuestOfDeathList;

	public void addNotifyQuestOfDeath(QuestState qs)
	{
		if (qs == null)
		{
			return;
		}

		if (!getNotifyQuestOfDeath().contains(qs))
		{
			getNotifyQuestOfDeath().add(qs);
		}
	}

	public void removeNotifyQuestOfDeath(QuestState qs)
	{
		if ((qs == null) || (_notifyQuestOfDeathList == null))
		{
			return;
		}

		_notifyQuestOfDeathList.remove(qs);
	}

	public final List<QuestState> getNotifyQuestOfDeath()
	{
		if (_notifyQuestOfDeathList == null)
		{
			synchronized (this)
			{
				if (_notifyQuestOfDeathList == null)
				{
					_notifyQuestOfDeathList = new CopyOnWriteArrayList<>();
				}
			}
		}
		return _notifyQuestOfDeathList;
	}

	public final boolean isNotifyQuestOfDeathEmpty()
	{
		return (_notifyQuestOfDeathList == null) || _notifyQuestOfDeathList.isEmpty();
	}

	public ShortCutTemplate[] getAllShortCuts()
	{
		return _shortCuts.getAllShortCuts();
	}

	public ShortCutTemplate getShortCut(int slot, int page)
	{
		return _shortCuts.getShortCut(slot, page);
	}

	private PcTemplate createRandomAntifeedTemplate()
	{
		Race race = null;

		while (race == null)
		{
			race = Race.values()[Rnd.get(Race.values().length)];
			if ((race == getRace()) || (race == Race.Kamael))
			{
				race = null;
			}
		}

		PlayerClass p;
		for (final ClassId c : ClassId.values())
		{
			p = PlayerClass.values()[c.getId()];
			if (p.isOfRace(race) && p.isOfLevel(ClassLevel.Fourth))
			{
				_antifeedTemplate = CharTemplateParser.getInstance().getTemplate(c);
				break;
			}
		}

		if (getRace() == Race.Kamael)
		{
			_antifeedSex = getAppearance().getSex();
		}

		_antifeedSex = Rnd.get(2) == 0 ? true : false;

		return _antifeedTemplate;
	}

	public void startAntifeedProtection(boolean start, boolean broadcast)
	{
		if (!start)
		{
			getAppearance().setVisibleName(getName(null));
			_antifeedTemplate = null;
		}
		else
		{
			getAppearance().setVisibleName("Unknown");
			createRandomAntifeedTemplate();
		}
	}

	public PcTemplate getAntifeedTemplate()
	{
		return _antifeedTemplate;
	}

	public boolean getAntifeedSex()
	{
		return _antifeedSex;
	}

	public void registerShortCut(ShortCutTemplate shortcut)
	{
		_shortCuts.registerShortCut(shortcut);
	}

	public void updateShortCuts(int skillId, int skillLevel)
	{
		_shortCuts.updateShortCuts(skillId, skillLevel);
	}
	
	public void updateShortCuts(int objId, ShortcutType type)
	{
		_shortCuts.updateShortCuts(objId, type);
	}

	public void registerShortCut(ShortCutTemplate shortcut, boolean storeToDb)
	{
		_shortCuts.registerShortCut(shortcut, storeToDb);
	}

	public void deleteShortCut(int slot, int page)
	{
		_shortCuts.deleteShortCut(slot, page);
	}

	public void deleteShortCut(int slot, int page, boolean fromDb)
	{
		_shortCuts.deleteShortCut(slot, page, fromDb);
	}

	public void removeAllShortcuts()
	{
		_shortCuts.tempRemoveAll();
	}

	public void registerMacro(Macro macro)
	{
		_macros.registerMacro(macro);
	}

	public void deleteMacro(int id)
	{
		_macros.deleteMacro(id);
	}

	public MacroList getMacros()
	{
		return _macros;
	}

	public void setSiegeState(byte siegeState)
	{
		_siegeState = siegeState;
	}

	public byte getSiegeState()
	{
		return _siegeState;
	}

	public void setSiegeSide(int val)
	{
		_siegeSide = val;
	}

	public boolean isRegisteredOnThisSiegeField(int val)
	{
		if ((_siegeSide != val) && ((_siegeSide < 81) || (_siegeSide > 89)))
		{
			return false;
		}
		return true;
	}

	public int getSiegeSide()
	{
		return _siegeSide;
	}

	public void setPvpFlag(int pvpFlag)
	{
		_pvpFlag = (byte) pvpFlag;
	}

	@Override
	public byte getPvpFlag()
	{
		return _pvpFlag;
	}

	@Override
	public void updatePvPFlag(int value)
	{
		if (getPvpFlag() == value)
		{
			return;
		}
		setPvpFlag(value);
		sendStatusUpdate(true, true, StatusUpdate.PVP_FLAG);
		broadcastRelationChanged();
	}
	
	@Override
	public int getRandomDamage()
	{
		final Weapon weaponItem = getActiveWeaponItem();
		if (weaponItem == null)
		{
			return getTemplate().getBaseRndDamage();
		}
		return weaponItem.getRandomDamage();
	}
	
	public int getZoneMask()
	{
		return _zoneMask;
	}

	@Override
	public void revalidateZone(boolean force)
	{
		super.revalidateZone(force);
		
		final boolean lastInCombatZone = (_zoneMask & ZONE_PVP_FLAG) == ZONE_PVP_FLAG;
		final boolean lastOnSiegeField = (_zoneMask & ZONE_SIEGE_FLAG) == ZONE_SIEGE_FLAG;
		
		final boolean isInCombatZone = isInsideZone(ZoneId.PVP);
		final boolean isInDangerArea = isInsideZone(ZoneId.ALTERED);
		final boolean isOnSiegeField = isInsideZone(ZoneId.SIEGE);
		final boolean isInPeaceZone = isInZonePeace();
		final boolean isInSSQZone = isIn7sDungeon();
		
		final int lastZoneMask = _zoneMask;
		_zoneMask = 0;
		
		if (isInCombatZone)
		{
			_zoneMask |= ZONE_PVP_FLAG;
		}
		if (isInDangerArea)
		{
			_zoneMask |= ZONE_ALTERED_FLAG;
		}
		if (isOnSiegeField)
		{
			_zoneMask |= ZONE_SIEGE_FLAG;
		}
		if (isInPeaceZone)
		{
			_zoneMask |= ZONE_PEACE_FLAG;
		}
		
		if (isInSSQZone)
		{
			_zoneMask |= ZONE_SSQ_FLAG;
		}
		
		if (lastZoneMask != _zoneMask)
		{
			sendPacket(new ExSetCompassZoneCode(this));
		}
		
		if (lastInCombatZone != isInCombatZone || lastOnSiegeField != isOnSiegeField)
		{
			broadcastRelationChanged();
			if (lastOnSiegeField != isOnSiegeField)
			{
				updatePvPStatus();
			}
		}
		
		if (Config.ALLOW_WATER)
		{
			checkWaterState();
		}
	}

	public boolean hasDwarvenCraft()
	{
		return getSkillLevel(Skill.SKILL_CREATE_DWARVEN) >= 1;
	}

	public int getDwarvenCraft()
	{
		return getSkillLevel(Skill.SKILL_CREATE_DWARVEN);
	}

	public boolean hasCommonCraft()
	{
		return getSkillLevel(Skill.SKILL_CREATE_COMMON) >= 1;
	}

	public int getCommonCraft()
	{
		return getSkillLevel(Skill.SKILL_CREATE_COMMON);
	}

	public int getPkKills()
	{
		return _pkKills;
	}

	public void setPkKills(int pkKills)
	{
		_pkKills = pkKills;
	}

	public long getDeleteTimer()
	{
		return _deleteTimer;
	}

	public void setDeleteTimer(long deleteTimer)
	{
		_deleteTimer = deleteTimer;
	}

	public void setExpBeforeDeath(long exp)
	{
		_expBeforeDeath = exp;
	}

	public long getExpBeforeDeath()
	{
		return _expBeforeDeath;
	}

	@Override
	public int getKarma()
	{
		return _karma;
	}

	public void setKarma(int karma)
	{
		if (karma < 0)
		{
			karma = 0;
		}
		boolean flagChanged = false;
		if ((_karma == 0) && (karma > 0))
		{
			for (final GameObject object : World.getInstance().getAroundNpc(this))
			{
				if (!(object instanceof GuardInstance))
				{
					continue;
				}

				if (((GuardInstance) object).getAI().getIntention() == CtrlIntention.IDLE)
				{
					((GuardInstance) object).getAI().setIntention(CtrlIntention.ACTIVE, null);
				}
			}
			flagChanged = true;
		}
		else if ((_karma > 0) && (karma == 0))
		{
			setKarmaFlag(0);
			flagChanged = true;
		}

		_karma = karma;
		broadcastKarma(flagChanged);
	}

	public int getExpertiseArmorPenalty()
	{
		return _expertiseArmorPenalty;
	}

	public int getExpertiseWeaponPenalty()
	{
		return _expertiseWeaponPenalty;
	}

	public int getWeightPenalty()
	{
		if (_dietMode)
		{
			return 0;
		}
		return _curWeightPenalty;
	}

	public void refreshOverloaded()
	{
		if (isLogoutStarted())
		{
			return;
		}
		
		final int maxLoad = getMaxLoad();
		if (maxLoad > 0)
		{
			final long weightproc = (((getCurrentLoad() - getBonusWeightPenalty()) * 1000) / getMaxLoad());
			int newWeightPenalty;
			if ((weightproc < 500) || _dietMode)
			{
				newWeightPenalty = 0;
			}
			else if (weightproc < 666)
			{
				newWeightPenalty = 1;
			}
			else if (weightproc < 800)
			{
				newWeightPenalty = 2;
			}
			else if (weightproc < 1000)
			{
				newWeightPenalty = 3;
			}
			else
			{
				newWeightPenalty = 4;
			}

			if (_curWeightPenalty != newWeightPenalty)
			{
				_curWeightPenalty = newWeightPenalty;
				if ((newWeightPenalty > 0) && !_dietMode)
				{
					addSkill(SkillsParser.getInstance().getInfo(4270, newWeightPenalty));
					setIsOverloaded(getCurrentLoad() > maxLoad);
				}
				else
				{
					removeSkill(getKnownSkill(4270), false, true);
					setIsOverloaded(false);
				}
				sendPacket(new EtcStatusUpdate(this));
				broadcastUserInfo(true);
			}
		}
	}

	public void refreshExpertisePenalty()
	{
		if (!Config.EXPERTISE_PENALTY || isLogoutStarted())
		{
			return;
		}

		final int level = (int) calcStat(Stats.GRADE_EXPERTISE_LEVEL, getLevel(), null, null);
		int i = 0;
		for (i = 0; i < EXPERTISE_LEVELS.length; i++)
		{
			if (level < EXPERTISE_LEVELS[i + 1])
			{
				break;
			}
		}

		if (expertiseIndex != i)
		{
			expertiseIndex = i;
			if (expertiseIndex > 0)
			{
				addSkill(SkillsParser.getInstance().getInfo(239, expertiseIndex), false);
			}
		}

		int armorPenalty = 0;
		int weaponPenalty = 0;

		for (final ItemInstance item : getInventory().getItems())
		{
			if ((item != null) && item.isEquipped() && ((item.getItemType() != EtcItemType.ARROW) && (item.getItemType() != EtcItemType.BOLT)))
			{
				final int crystaltype = item.getItem().getCrystalType();

				if (item.getItem().getType2() == Item.TYPE2_WEAPON)
				{
					if (crystaltype > weaponPenalty)
					{
						weaponPenalty = crystaltype;
					}
				}
				else if ((item.getItem().getType2() == Item.TYPE2_SHIELD_ARMOR) || (item.getItem().getType2() == Item.TYPE2_ACCESSORY))
				{
					if (crystaltype > armorPenalty)
					{
						armorPenalty = crystaltype;
					}
				}
			}
		}

		boolean changed = false;

		armorPenalty = armorPenalty - expertiseIndex;
		armorPenalty = Math.min(Math.max(armorPenalty, 0), 4);

		if ((getExpertiseArmorPenalty() != armorPenalty) || (getSkillLevel(6213) != armorPenalty))
		{
			_expertiseArmorPenalty = armorPenalty;

			if (_expertiseArmorPenalty > 0)
			{
				addSkill(SkillsParser.getInstance().getInfo(6213, _expertiseArmorPenalty));
			}
			else
			{
				removeSkill(getKnownSkill(6213), false, true);
			}
			changed = true;
		}

		weaponPenalty = weaponPenalty - expertiseIndex;
		weaponPenalty = Math.min(Math.max(weaponPenalty, 0), 4);

		if ((getExpertiseWeaponPenalty() != weaponPenalty) || (getSkillLevel(6209) != weaponPenalty))
		{
			_expertiseWeaponPenalty = weaponPenalty;

			if (_expertiseWeaponPenalty > 0)
			{
				addSkill(SkillsParser.getInstance().getInfo(6209, _expertiseWeaponPenalty));
			}
			else
			{
				removeSkill(getKnownSkill(6209), false, true);
			}

			changed = true;
		}

		if (changed)
		{
			sendPacket(new EtcStatusUpdate(this));
		}
	}

	public void useEquippableItem(int objectId, boolean abortAttack)
	{
		final ItemInstance item = getInventory().getItemByObjectId(objectId);
		if (item == null)
		{
			return;
		}
		
		ItemInstance[] items = null;
		final boolean isEquiped = item.isEquipped();
		final int oldInvLimit = getInventoryLimit();
		SystemMessage sm = null;

		if (isEquiped)
		{
			if (item.isEventItem() && isInFightEvent())
			{
				return;
			}
			
			if (item.getEnchantLevel() > 0)
			{
				sm = SystemMessage.getSystemMessage(SystemMessageId.EQUIPMENT_S1_S2_REMOVED);
				sm.addNumber(item.getEnchantLevel());
				sm.addItemName(item);
			}
			else
			{
				sm = SystemMessage.getSystemMessage(SystemMessageId.S1_DISARMED);
				sm.addItemName(item);
			}
			sendPacket(sm);

			final int slot = getInventory().getSlotFromItem(item);

			if (slot == Item.SLOT_DECO)
			{
				items = getInventory().unEquipItemInSlotAndRecord(item.getLocationSlot());
			}
			else
			{
				items = getInventory().unEquipItemInBodySlotAndRecord(slot);
			}
		}
		else
		{
			if (item.isEventItem() && !isInFightEvent())
			{
				return;
			}
			
			items = getInventory().equipItemAndRecord(item);

			if (item.isEquipped())
			{
				if (item.getEnchantLevel() > 0)
				{
					sm = SystemMessage.getSystemMessage(SystemMessageId.S1_S2_EQUIPPED);
					sm.addNumber(item.getEnchantLevel());
					sm.addItemName(item);
				}
				else
				{
					sm = SystemMessage.getSystemMessage(SystemMessageId.S1_EQUIPPED);
					sm.addItemName(item);
				}
				sendPacket(sm);

				item.decreaseMana(false);
				item.decreaseEnergy(false);

				if ((item.getItem().getBodyPart() & Item.SLOT_MULTI_ALLWEAPON) != 0)
				{
					rechargeShots(true, true);
				}
			}
			else
			{
				sendPacket(SystemMessageId.CANNOT_EQUIP_ITEM_DUE_TO_BAD_CONDITION);
			}
		}
		refreshExpertisePenalty();

		final InventoryUpdate iu = new InventoryUpdate();
		iu.addItems(Arrays.asList(items));
		sendPacket(iu);
		sendItemList(false);
		broadcastUserInfo(true);
		
		if (abortAttack)
		{
			abortAttack();
		}

		if (getInventoryLimit() != oldInvLimit)
		{
			sendPacket(new ExStorageMaxCount(this));
		}
	}

	public int getPvpKills()
	{
		return _pvpKills;
	}

	public void setPvpKills(int pvpKills)
	{
		_pvpKills = pvpKills;
	}

	public int getFame()
	{
		return _fame;
	}

	public void setFame(int fame)
	{
		final int nextFame = (fame > Config.MAX_PERSONAL_FAME_POINTS) ? Config.MAX_PERSONAL_FAME_POINTS : fame;
		final boolean addAch = nextFame > _fame;
		_fame = nextFame;
		if (addAch)
		{
			getCounters().addAchivementInfo("fameAcquired", 0, _fame, true, false, false);
		}
	}

	public ClassId getClassId()
	{
		return getTemplate().getClassId();
	}

	public void setClassId(int Id)
	{
		if (!_subclassLock.tryLock())
		{
			return;
		}

		try
		{
			if ((getLvlJoinedAcademy() != 0) && (_clan != null) && (PlayerClass.values()[Id].getLevel() == ClassLevel.Third))
			{
				if (getLvlJoinedAcademy() <= 16)
				{
					_clan.addReputationScore(Config.JOIN_ACADEMY_MAX_REP_SCORE, true);
				}
				else if (getLvlJoinedAcademy() >= 39)
				{
					_clan.addReputationScore(Config.JOIN_ACADEMY_MIN_REP_SCORE, true);
				}
				else
				{
					_clan.addReputationScore((Config.JOIN_ACADEMY_MAX_REP_SCORE - ((getLvlJoinedAcademy() - 16) * 20)), true);
				}
				setLvlJoinedAcademy(0);
				final SystemMessage msg = SystemMessage.getSystemMessage(SystemMessageId.CLAN_MEMBER_S1_EXPELLED);
				msg.addPcName(this);
				_clan.broadcastToOnlineMembers(msg);
				_clan.broadcastToOnlineMembers(new PledgeShowMemberListDelete(getName(null)));
				_clan.removeClanMember(getObjectId(), 0);
				sendPacket(PledgeShowMemberListDeleteAll.STATIC_PACKET);
				sendPacket(SystemMessageId.ACADEMY_MEMBERSHIP_TERMINATED);
				getInventory().addItem("Gift", 8181, 1, this, null);
				AcademyList.removeAcademyFromDB(_clan, getObjectId(), true, false);
			}
			if (isSubClassActive())
			{
				getSubClasses().get(_classIndex).setClassId(Id);
			}
			setTarget(this);
			broadcastPacket(new MagicSkillUse(this, 5103, 1, 1000, 0));
			setClassTemplate(Id);
			if (getClassId().level() == 3)
			{
				sendPacket(SystemMessageId.THIRD_CLASS_TRANSFER);
			}
			else
			{
				sendPacket(SystemMessageId.CLASS_TRANSFER);
			}

			if (isInParty())
			{
				getParty().broadCast(new PartySmallWindowUpdate(this));
			}

			if (getClan() != null)
			{
				getClan().broadcastToOnlineMembers(new PledgeShowMemberListUpdate(this));
			}

			if (_matchingRoom != null)
			{
				_matchingRoom.broadcastPlayerUpdate(this);
			}

			rewardSkills();

			if (!canOverrideCond(PcCondOverride.SKILL_CONDITIONS) && Config.DECREASE_SKILL_LEVEL)
			{
				checkPlayerSkills();
			}
		}
		finally
		{
			_subclassLock.unlock();
		}
	}

	private ClassId _learningClass = getClassId();
	private int _learningGroupId;

	public ClassId getLearningClass()
	{
		return _learningClass;
	}

	public void setLearningClass(ClassId learningClass)
	{
		_learningClass = learningClass;
	}
	
	public int getLearningGroupId()
	{
		return _learningGroupId;
	}
	
	public void setLearningGroupId(int value)
	{
		_learningGroupId = value;
	}

	public long getExp()
	{
		return getStat().getExp();
	}

	public void setActiveEnchantAttrItemId(int objectId)
	{
		_activeEnchantAttrItemId = objectId;
	}

	public int getActiveEnchantAttrItemId()
	{
		return _activeEnchantAttrItemId;
	}

	public void setActiveEnchantItemId(int objectId)
	{
		if (objectId == ID_NONE)
		{
			setActiveEnchantSupportItemId(ID_NONE);
			setIsEnchanting(false);
		}
		_activeEnchantItemId = objectId;
	}

	public int getActiveEnchantItemId()
	{
		return _activeEnchantItemId;
	}

	public void setActiveEnchantSupportItemId(int objectId)
	{
		_activeEnchantSupportItemId = objectId;
	}

	public int getActiveEnchantSupportItemId()
	{
		return _activeEnchantSupportItemId;
	}

	public void setIsEnchanting(boolean val)
	{
		_isEnchanting = val;
	}

	public boolean isEnchanting()
	{
		return _isEnchanting;
	}

	public void setFistsWeaponItem(Weapon weaponItem)
	{
		_fistsWeaponItem = weaponItem;
	}

	public Weapon getFistsWeaponItem()
	{
		return _fistsWeaponItem;
	}

	public Weapon findFistsWeaponItem(int classId)
	{
		Weapon weaponItem = null;
		if ((classId >= 0x00) && (classId <= 0x09))
		{
			final Item temp = ItemsParser.getInstance().getTemplate(246);
			weaponItem = (Weapon) temp;
		}
		else if ((classId >= 0x0a) && (classId <= 0x11))
		{
			final Item temp = ItemsParser.getInstance().getTemplate(251);
			weaponItem = (Weapon) temp;
		}
		else if ((classId >= 0x12) && (classId <= 0x18))
		{
			final Item temp = ItemsParser.getInstance().getTemplate(244);
			weaponItem = (Weapon) temp;
		}
		else if ((classId >= 0x19) && (classId <= 0x1e))
		{
			final Item temp = ItemsParser.getInstance().getTemplate(249);
			weaponItem = (Weapon) temp;
		}
		else if ((classId >= 0x1f) && (classId <= 0x25))
		{
			final Item temp = ItemsParser.getInstance().getTemplate(245);
			weaponItem = (Weapon) temp;
		}
		else if ((classId >= 0x26) && (classId <= 0x2b))
		{
			final Item temp = ItemsParser.getInstance().getTemplate(250);
			weaponItem = (Weapon) temp;
		}
		else if ((classId >= 0x2c) && (classId <= 0x30))
		{
			final Item temp = ItemsParser.getInstance().getTemplate(248);
			weaponItem = (Weapon) temp;
		}
		else if ((classId >= 0x31) && (classId <= 0x34))
		{
			final Item temp = ItemsParser.getInstance().getTemplate(252);
			weaponItem = (Weapon) temp;
		}
		else if ((classId >= 0x35) && (classId <= 0x39))
		{
			final Item temp = ItemsParser.getInstance().getTemplate(247);
			weaponItem = (Weapon) temp;
		}
		return weaponItem;
	}

	public void rewardSkills()
	{
		if (Config.AUTO_LEARN_SKILLS && (getLevel() <= Config.AUTO_LEARN_SKILLS_MAX_LEVEL) || isFakePlayer())
		{
			giveAvailableSkills(Config.AUTO_LEARN_FS_SKILLS, true);
		}
		else
		{
			giveAvailableAutoGetSkills();
		}

		if (Config.UNSTUCK_SKILL && (getSkillLevel((short) 1050) < 0))
		{
			addSkill(SkillsParser.getInstance().getInfo(2099, 1), false);
		}

		if (!canOverrideCond(PcCondOverride.SKILL_CONDITIONS) && Config.DECREASE_SKILL_LEVEL)
		{
			checkPlayerSkills();
		}
		checkItemRestriction();
		sendSkillList(false);
	}

	public void regiveTemporarySkills()
	{
		if (isNoble())
		{
			setNoble(true);
		}

		if (isHero())
		{
			if (isTimeHero())
			{
				setHero(true, getVarInt("tempHeroSkills", 0) == 1);
			}
			else
			{
				setHero(true, true);
			}
		}

		final Clan clan = getClan();
		if (clan != null)
		{
			clan.addSkillEffects(this);
			if ((clan.getLevel() >= 5) && isClanLeader())
			{
				SiegeManager.getInstance().addSiegeSkills(this);
			}
			if (clan.getCastleId() > 0)
			{
				CastleManager.getInstance().getCastleByOwner(clan).giveResidentialSkills(this);
			}
			if (clan.getFortId() > 0)
			{
				FortManager.getInstance().getFortByOwner(clan).giveResidentialSkills(this);
			}
			if (clan.getHideoutId() > 0)
			{
				final var hall = ClanHallManager.getInstance().getAbstractHallByOwner(clan);
				if (hall != null)
				{
					hall.giveResidentialSkills(this);
				}
			}
		}
		getInventory().reloadEquippedItems();

		restoreDeathPenaltyBuffLevel();
	}

	public int giveAvailableSkills(boolean includedByFs, boolean includeAutoGet)
	{
		int skillCounter = 0;

		final Collection<Skill> skills = SkillTreesParser.getInstance().getAllAvailableSkills(this, getClassId(), includedByFs, includeAutoGet);
		final List<Skill> skillsForStore = new ArrayList<>();
		for (final var sk : skills)
		{
			if (Config.DECREASE_SKILL_LEVEL && getKnownSkill(sk.getId()) == sk)
			{
				continue;
			}
			else
			{
				final var skill = getKnownSkill(sk.getId());
				if (skill != null && skill.getLevel() > sk.getLevel())
				{
					continue;
				}
			}

			if (getSkillLevel(sk.getId()) == -1)
			{
				skillCounter++;
			}

			if (sk.isToggle())
			{
				final var toggleEffect = getFirstEffect(sk.getId());
				if (toggleEffect != null)
				{
					toggleEffect.exit();
					sk.getEffects(this, this, true);
				}
			}
			addSkill(sk, false);
			skillsForStore.add(sk);
		}
		CharacterSkillsDAO.getInstance().storeSkills(this, skillsForStore, -1);
		if (Config.AUTO_LEARN_SKILLS && (skillCounter > 0))
		{
			sendMessage("You have learned " + skillCounter + " new skills");
		}
		return skillCounter;
	}

	public void giveAvailableAutoGetSkills()
	{
		final List<SkillLearn> autoGetSkills = SkillTreesParser.getInstance().getAvailableAutoGetSkills(this);
		final SkillsParser st = SkillsParser.getInstance();
		Skill skill;
		for (final SkillLearn s : autoGetSkills)
		{
			skill = st.getInfo(s.getId(), s.getLvl());
			if (skill != null)
			{
				addSkill(skill, true);
			}
			else
			{
				_log.warn("Skipping null auto-get skill for player: " + toString());
			}
		}
	}

	public void setExp(long exp)
	{
		if (exp < 0)
		{
			exp = 0;
		}

		getStat().setExp(exp);
	}

	public Race getRace()
	{
		if (!isSubClassActive())
		{
			return getTemplate().getRace();
		}
		return CharTemplateParser.getInstance().getTemplate(_baseClass).getRace();
	}

	public Radar getRadar()
	{
		return _radar;
	}

	public boolean isMinimapAllowed()
	{
		return _minimapAllowed;
	}

	public void setMinimapAllowed(boolean b)
	{
		_minimapAllowed = b;
	}

	public int getSp()
	{
		return getStat().getSp();
	}

	public void setSp(int sp)
	{
		if (sp < 0)
		{
			sp = 0;
		}

		super.getStat().setSp(sp);
	}

	public boolean isCastleLord(int castleId)
	{
		final Clan clan = getClan();

		if ((clan != null) && (clan.getLeader().getPlayerInstance() == this))
		{
			final Castle castle = CastleManager.getInstance().getCastleByOwner(clan);
			if ((castle != null) && (castle == CastleManager.getInstance().getCastleById(castleId)))
			{
				return true;
			}
		}

		return false;
	}

	@Override
	public int getClanId()
	{
		return _clanId;
	}

	public int getClanCrestId()
	{
		if (_clan != null)
		{
			return _clan.getCrestId();
		}

		return 0;
	}

	public int getClanCrestLargeId()
	{
		if (_clan != null)
		{
			return _clan.getCrestLargeId();
		}

		return 0;
	}

	public long getClanJoinExpiryTime()
	{
		return _clanJoinExpiryTime;
	}

	public void setClanJoinExpiryTime(long time)
	{
		_clanJoinExpiryTime = time;
	}

	public long getClanCreateExpiryTime()
	{
		return _clanCreateExpiryTime;
	}

	public void setClanCreateExpiryTime(long time)
	{
		_clanCreateExpiryTime = time;
	}

	public void setOnlineTime(long time)
	{
		_onlineTime = time;
		_onlineBeginTime = System.currentTimeMillis();
	}

	@Override
	public PcInventory getInventory()
	{
		return _inventory;
	}

	public void removeItemFromShortCut(int objectId)
	{
		_shortCuts.deleteShortCutByObjectId(objectId);
	}

	public boolean isSitting()
	{
		return _waitTypeSitting;
	}

	public void setIsSitting(boolean state)
	{
		_waitTypeSitting = state;
	}

	public void sitDown()
	{
		sitDown(true);
	}
	
	public void sitDownNow()
	{
		breakAttack();
		setIsSitting(true);
		getAI().setIntention(CtrlIntention.REST);
		broadcastPacket(new ChangeWaitType(this, ChangeWaitType.WT_SITTING));
		getPersonalTasks().addTask(new SitDownTask(2500L));
		setIsParalyzed(true);
		setIsInvul(true);
	}

	public void sitDown(boolean checkCast)
	{
		if (checkCast && isCastingNow())
		{
			sendMessage("Cannot sit while casting");
			return;
		}

		if (!_waitTypeSitting && !isAttackingDisabled() && !isOutOfControl() && !isImmobilized())
		{
			breakAttack();
			setIsSitting(true);
			getAI().setIntention(CtrlIntention.REST);
			broadcastPacket(new ChangeWaitType(this, ChangeWaitType.WT_SITTING));
			getPersonalTasks().addTask(new SitDownTask(2500L));
			setIsParalyzed(true);
		}
	}

	public void standUp()
	{
		if (isInFightEvent() && !getFightEvent().canStandUp(this))
		{
			return;
		}

		_sittingObject = null;
		
		if (_waitTypeSitting && !isInStoreMode() && !isAlikeDead())
		{
			if (_effects.isAffected(EffectFlag.RELAXING))
			{
				stopEffects(EffectType.RELAXING);
			}
			broadcastPacket(new ChangeWaitType(this, ChangeWaitType.WT_STANDING));
			getPersonalTasks().addTask(new StandUpTask(2500L));
		}
	}

	public PcWarehouse getWarehouse()
	{
		if (_warehouse == null)
		{
			_warehouse = new PcWarehouse(this);
			_warehouse.restore();
		}
		if (Config.WAREHOUSE_CACHE)
		{
			WarehouseCache.getInstance().addCacheTask(this);
		}
		return _warehouse;
	}

	public void clearWarehouse()
	{
		if (_warehouse != null)
		{
			_warehouse.deleteMe();
		}
		_warehouse = null;
	}
	
	public PcPrivateInventory getPrivateInventory()
	{
		if (_privateInventory == null)
		{
			_privateInventory = new PcPrivateInventory(this);
			_privateInventory.restore();
		}
		return _privateInventory;
	}
	
	public void clearPrivateInventory()
	{
		if (_privateInventory != null)
		{
			_privateInventory.deleteMe();
		}
		_privateInventory = null;
	}

	public PcFreight getFreight()
	{
		return _freight;
	}

	public boolean hasRefund()
	{
		return (_refund != null) && (_refund.getSize() > 0) && Config.ALLOW_REFUND;
	}

	public PcRefund getRefund()
	{
		if (_refund == null)
		{
			_refund = new PcRefund(this);
		}
		return _refund;
	}

	public void clearRefund()
	{
		if (_refund != null)
		{
			_refund.deleteMe();
		}
		_refund = null;
	}

	public long getAdena()
	{
		return _inventory.getAdena();
	}

	public long getAncientAdena()
	{
		return _inventory.getAncientAdena();
	}

	public void addAdena(String process, long count, GameObject reference, boolean sendMessage)
	{
		if (sendMessage)
		{
			final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.EARNED_S1_ADENA);
			sm.addItemNumber(count);
			sendPacket(sm);
		}

		if (count > 0)
		{
			_inventory.addAdena(process, count, this, reference);
			getCounters().addAchivementInfo("adenaAcquired", 0, -1, false, false, false);
		}
	}

	public boolean reduceAdena(String process, long count, GameObject reference, boolean sendMessage)
	{
		if (count > getAdena())
		{
			if (sendMessage)
			{
				sendPacket(SystemMessageId.YOU_NOT_ENOUGH_ADENA);
			}
			return false;
		}

		if (count > 0)
		{
			final ItemInstance adenaItem = _inventory.getAdenaInstance();
			if (!_inventory.reduceAdena(process, count, this, reference))
			{
				return false;
			}
			getCounters().addAchivementInfo("adenaReduced", 0, -1, false, false, false);
			if (!Config.FORCE_INVENTORY_UPDATE)
			{
				final InventoryUpdate iu = new InventoryUpdate();
				iu.addItem(adenaItem);
				sendPacket(iu);
			}
			else
			{
				sendItemList(false);
			}

			if (sendMessage)
			{
				final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_DISAPPEARED_ADENA);
				sm.addItemNumber(count);
				sendPacket(sm);
			}
		}
		return true;
	}

	public void addAncientAdena(String process, long count, GameObject reference, boolean sendMessage)
	{
		if (sendMessage)
		{
			final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.EARNED_S2_S1_S);
			sm.addItemName(PcInventory.ANCIENT_ADENA_ID);
			sm.addItemNumber(count);
			sendPacket(sm);
		}

		if (count > 0)
		{
			if (isAllowPrivateInventory() && isPrivateInventoryUnder90())
			{
				getPrivateInventory().addItem(process, 5575, count, this, reference);
			}
			else
			{
				_inventory.addAncientAdena(process, count, this, reference);
			}
		}
	}

	public boolean reduceAncientAdena(String process, long count, GameObject reference, boolean sendMessage)
	{
		if (count > getAncientAdena())
		{
			if (sendMessage)
			{
				sendPacket(SystemMessageId.YOU_NOT_ENOUGH_ADENA);
			}

			return false;
		}

		if (count > 0)
		{
			final ItemInstance ancientAdenaItem = _inventory.getAncientAdenaInstance();
			if (!_inventory.reduceAncientAdena(process, count, this, reference))
			{
				return false;
			}

			if (!Config.FORCE_INVENTORY_UPDATE)
			{
				final InventoryUpdate iu = new InventoryUpdate();
				iu.addItem(ancientAdenaItem);
				sendPacket(iu);
			}
			else
			{
				sendItemList(false);
			}

			if (sendMessage)
			{
				if (count > 1)
				{
					final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S2_S1_DISAPPEARED);
					sm.addItemName(PcInventory.ANCIENT_ADENA_ID);
					sm.addItemNumber(count);
					sendPacket(sm);
				}
				else
				{
					final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_DISAPPEARED);
					sm.addItemName(PcInventory.ANCIENT_ADENA_ID);
					sendPacket(sm);
				}
			}
		}
		return true;
	}

	public void addItem(String process, ItemInstance item, GameObject reference, boolean sendMessage)
	{
		if (item.getCount() > 0)
		{
			if (sendMessage)
			{
				if (item.getCount() > 1)
				{
					final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.YOU_PICKED_UP_S1_S2);
					sm.addItemName(item);
					sm.addItemNumber(item.getCount());
					sendPacket(sm);
				}
				else if (item.getEnchantLevel() > 0)
				{
					final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.YOU_PICKED_UP_A_S1_S2);
					sm.addNumber(item.getEnchantLevel());
					sm.addItemName(item);
					sendPacket(sm);
				}
				else
				{
					final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.YOU_PICKED_UP_S1);
					sm.addItemName(item);
					sendPacket(sm);
				}
			}
			
			if (!noValidItems(item) && isAllowPrivateInventory() && isPrivateInventoryUnder90())
			{
				getPrivateInventory().addItem(process, item, this, reference);
			}
			else
			{
				final ItemInstance newitem = _inventory.addItem(process, item, this, reference);
				sendPacket(makeStatusUpdate(StatusUpdate.CUR_LOAD));

				if (!canOverrideCond(PcCondOverride.ITEM_CONDITIONS) && !_inventory.validateCapacity(item))
				{
					dropItem("InvDrop", newitem, null, true, true);
				}
				else if (CursedWeaponsManager.getInstance().isCursed(newitem.getId()))
				{
					CursedWeaponsManager.getInstance().activate(this, newitem);
				}
				else if (FortSiegeManager.getInstance().isCombat(item.getId()) && !isInFightEvent())
				{
					if (FortSiegeManager.getInstance().activateCombatFlag(this, item))
					{
						final Fort fort = FortManager.getInstance().getFort(this);
						fort.getSiege().announceToPlayer(SystemMessage.getSystemMessage(SystemMessageId.C1_ACQUIRED_THE_FLAG), getName(null));
					}
				}
				else if ((item.getId() >= 13560) && (item.getId() <= 13568))
				{
					final TerritoryWard ward = TerritoryWarManager.getInstance().getTerritoryWard(item.getId() - 13479);
					if (ward != null)
					{
						ward.activate(this, item);
					}
				}
				
				if (newitem.isEtcItem())
				{
					checkToEquipArrows(newitem);
				}
				else if (newitem.isTalisman())
				{
					checkCombineTalisman(newitem, false);
				}
			}
		}
	}
	
	private boolean noValidItems(ItemInstance item)
	{
		return item.isQuestItem() || item.getItem().isHerb() || CursedWeaponsManager.getInstance().isCursed(item.getId()) || (FortSiegeManager.getInstance().isCombat(item.getId()) && !isInFightEvent() || ((item.getId() >= 13560) && (item.getId() <= 13568)) || (item.getId() == 57));
	}

	public ItemInstance addItem(String process, int itemId, long count, GameObject reference, boolean sendMessage)
	{
		if (count > 0)
		{
			ItemInstance item = null;
			if (ItemsParser.getInstance().getTemplate(itemId) != null)
			{
				item = ItemsParser.getInstance().createDummyItem(itemId);
			}
			else
			{
				_log.warn("Item doesn't exist so cannot be added. Item ID: " + itemId);
				return null;
			}
			
			if (sendMessage)
			{
				if (count > 1)
				{
					if (process.equalsIgnoreCase("Sweeper") || process.equalsIgnoreCase("Quest"))
					{
						final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.EARNED_S2_S1_S);
						sm.addItemName(itemId);
						sm.addItemNumber(count);
						sendPacket(sm);
					}
					else
					{
						final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.YOU_PICKED_UP_S1_S2);
						sm.addItemName(itemId);
						sm.addItemNumber(count);
						sendPacket(sm);
					}
				}
				else
				{
					if (process.equalsIgnoreCase("Sweeper") || process.equalsIgnoreCase("Quest"))
					{
						final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.EARNED_ITEM_S1);
						sm.addItemName(itemId);
						sendPacket(sm);
					}
					else
					{
						final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.YOU_PICKED_UP_S1);
						sm.addItemName(itemId);
						sendPacket(sm);
					}
				}
			}
			
			if (!noValidItems(item) && isAllowPrivateInventory() && isPrivateInventoryUnder90())
			{
				return getPrivateInventory().addItem(process, itemId, count, this, reference);
			}
			else
			{
				if (item.getItem().isHerb())
				{
					final IItemHandler handler = ItemHandler.getInstance().getHandler(item.getEtcItem());
					if (handler == null)
					{
						_log.warn("No item handler registered for Herb ID " + item.getId() + "!");
					}
					else
					{
						handler.useItem(this, new ItemInstance(itemId), false);
						if ((getSummon() != null) && getSummon().isServitor() && !getSummon().isDead())
						{
							handler.useItem(getSummon(), new ItemInstance(itemId), false);
						}
					}
				}
				else
				{
					final ItemInstance createdItem = _inventory.addItem(process, itemId, count, this, reference);
					
					if (!canOverrideCond(PcCondOverride.ITEM_CONDITIONS) && !_inventory.validateCapacity(item))
					{
						dropItem("InvDrop", createdItem, null, true);
					}
					else if (CursedWeaponsManager.getInstance().isCursed(createdItem.getId()))
					{
						CursedWeaponsManager.getInstance().activate(this, createdItem);
					}
					else if (FortSiegeManager.getInstance().isCombat(createdItem.getId()) && !isInFightEvent())
					{
						if (FortSiegeManager.getInstance().activateCombatFlag(this, item))
						{
							final Fort fort = FortManager.getInstance().getFort(this);
							fort.getSiege().announceToPlayer(SystemMessage.getSystemMessage(SystemMessageId.C1_ACQUIRED_THE_FLAG), getName(null));
						}
					}
					else if ((createdItem.getId() >= 13560) && (createdItem.getId() <= 13568))
					{
						final TerritoryWard ward = TerritoryWarManager.getInstance().getTerritoryWard(createdItem.getId() - 13479);
						if (ward != null)
						{
							ward.activate(this, createdItem);
						}
					}
					
					if (createdItem.isEtcItem())
					{
						checkToEquipArrows(createdItem);
					}
					else if (createdItem.isTalisman())
					{
						checkCombineTalisman(createdItem, false);
					}
					return createdItem;
				}
			}
		}
		return null;
	}

	public void addItem(String process, ItemHolder itemHolder, GameObject reference, boolean sendMessage)
	{
		int itemId = itemHolder.getId();
		long count = itemHolder.getCount() != itemHolder.getCountMax() ? Rnd.get(itemHolder.getCount(), itemHolder.getCountMax()) : itemHolder.getCount();
		double chance = itemHolder.getChance();
		int enchantLevel = itemHolder.getEnchantLevel();
		if(!Rnd.chance(chance))
			return;

		ItemInstance item = addItem(process, itemId, count, reference, sendMessage);
		if(item != null)
		{
			item.setEnchantLevel(enchantLevel);
		}
	}

	public boolean destroyItem(String process, ItemInstance item, GameObject reference, boolean sendMessage)
	{
		return destroyItem(process, item, item.getCount(), reference, sendMessage);
	}

	public boolean destroyItem(String process, ItemInstance item, long count, GameObject reference, boolean sendMessage)
	{
		item = _inventory.destroyItem(process, item, count, this, reference);

		if (item == null)
		{
			if (sendMessage)
			{
				sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
			}
			return false;
		}

		if (!Config.FORCE_INVENTORY_UPDATE)
		{
			final InventoryUpdate playerIU = new InventoryUpdate();
			playerIU.addItem(item);
			sendPacket(playerIU);
		}
		else
		{
			sendItemList(false);
		}

		if (sendMessage)
		{
			if (count > 1)
			{
				final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S2_S1_DISAPPEARED);
				sm.addItemName(item);
				sm.addItemNumber(count);
				sendPacket(sm);
			}
			else
			{
				final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_DISAPPEARED);
				sm.addItemName(item);
				sendPacket(sm);
			}
		}
		return true;
	}

	@Override
	public boolean destroyItem(String process, int objectId, long count, GameObject reference, boolean sendMessage)
	{
		final ItemInstance item = _inventory.getItemByObjectId(objectId);

		if (item == null)
		{
			if (sendMessage)
			{
				sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
			}

			return false;
		}
		return destroyItem(process, item, count, reference, sendMessage);
	}

	public boolean destroyItemWithoutTrace(String process, int objectId, long count, GameObject reference, boolean sendMessage)
	{
		final ItemInstance item = _inventory.getItemByObjectId(objectId);

		if ((item == null) || (item.getCount() < count))
		{
			if (sendMessage)
			{
				sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
			}

			return false;
		}
		return destroyItem(null, item, count, reference, sendMessage);
	}
	
	public boolean destroyItemWithoutEquip(String process, int itemId, long count, GameObject reference, boolean sendMessage)
	{
		final ItemInstance item = _inventory.getItemByItemId(itemId);
		if (item == null)
		{
			sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
			return false;
		}
		
		if (item.isStackable())
		{
			if (!destroyItemByItemId("Craft", itemId, count, reference, sendMessage))
			{
				sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
				return false;
			}
		}
		else
		{
			final ItemInstance[] inventoryContents = _inventory.getAllItemsByItemId(itemId, false);
			for (int i = 0; i < count; i++)
			{
				if (!destroyItem(process, inventoryContents[i].getObjectId(), 1, reference, sendMessage))
				{
					sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
					return false;
				}
			}
		}
		return true;
	}

	@Override
	public boolean destroyItemByItemId(String process, int itemId, long count, GameObject reference, boolean sendMessage)
	{
		if (itemId == PcInventory.ADENA_ID)
		{
			return reduceAdena(process, count, reference, sendMessage);
		}

		final ItemInstance item = _inventory.getItemByItemId(itemId);

		if ((item == null) || (item.getCount() < count) || (_inventory.destroyItemByItemId(process, itemId, count, this, reference) == null))
		{
			if (sendMessage)
			{
				sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
			}

			return false;
		}

		if (!Config.FORCE_INVENTORY_UPDATE)
		{
			final InventoryUpdate playerIU = new InventoryUpdate();
			playerIU.addItem(item);
			sendPacket(playerIU);
		}
		else
		{
			sendItemList(false);
		}

		if (item.getId() != PcInventory.ADENA_ID)
		{
			sendPacket(makeStatusUpdate(StatusUpdate.CUR_LOAD));
		}

		if (sendMessage)
		{
			if (count > 1)
			{
				final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S2_S1_DISAPPEARED);
				sm.addItemName(itemId);
				sm.addItemNumber(count);
				sendPacket(sm);
			}
			else
			{
				final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_DISAPPEARED);
				sm.addItemName(itemId);
				sendPacket(sm);
			}
		}
		return true;
	}

	public ItemInstance transferItem(String process, int objectId, long count, Inventory target, GameObject reference)
	{
		final ItemInstance oldItem = checkItemManipulation(objectId, count, "transfer");
		if (oldItem == null)
		{
			return null;
		}
		final ItemInstance newItem = getInventory().transferItem(process, objectId, count, target, this, reference);
		if (newItem == null)
		{
			return null;
		}

		if (!Config.FORCE_INVENTORY_UPDATE)
		{
			final InventoryUpdate playerIU = new InventoryUpdate();

			if ((oldItem.getCount() > 0) && (oldItem != newItem))
			{
				playerIU.addModifiedItem(oldItem);
			}
			else
			{
				playerIU.addRemovedItem(oldItem);
			}

			sendPacket(playerIU);
		}
		else
		{
			sendItemList(false);
		}
		
		if (newItem.getId() != PcInventory.ADENA_ID)
		{
			sendPacket(makeStatusUpdate(StatusUpdate.CUR_LOAD));
		}

		if (target instanceof PcInventory)
		{
			final Player targetPlayer = ((PcInventory) target).getOwner();

			if (!Config.FORCE_INVENTORY_UPDATE)
			{
				final InventoryUpdate playerIU = new InventoryUpdate();

				if (newItem.getCount() > count)
				{
					playerIU.addModifiedItem(newItem);
				}
				else
				{
					playerIU.addNewItem(newItem);
				}

				targetPlayer.sendPacket(playerIU);
			}
			else
			{
				targetPlayer.sendItemList(false);
			}

			if (newItem.getId() != PcInventory.ADENA_ID)
			{
				targetPlayer.sendPacket(targetPlayer.makeStatusUpdate(StatusUpdate.CUR_LOAD));
			}
		}
		else if (target instanceof PetInventory)
		{
			final PetInventoryUpdate petIU = new PetInventoryUpdate();

			if (newItem.getCount() > count)
			{
				petIU.addModifiedItem(newItem);
			}
			else
			{
				petIU.addNewItem(newItem);
			}

			((PetInventory) target).getOwner().sendPacket(petIU);
		}
		return newItem;
	}

	public boolean exchangeItemsById(String process, GameObject reference, int coinId, long cost, int rewardId, long count, boolean sendMessage)
	{
		final PcInventory inv = getInventory();
		if (!inv.validateCapacityByItemId(rewardId, count))
		{
			if (sendMessage)
			{
				sendPacket(SystemMessageId.SLOTS_FULL);
			}
			return false;
		}

		if (!inv.validateWeightByItemId(rewardId, count))
		{
			if (sendMessage)
			{
				sendPacket(SystemMessageId.WEIGHT_LIMIT_EXCEEDED);
			}
			return false;
		}

		if (destroyItemByItemId(process, coinId, cost, reference, sendMessage))
		{
			addItem(process, rewardId, count, reference, sendMessage);
			return true;
		}
		return false;
	}

	public boolean dropItem(String process, ItemInstance item, GameObject reference, boolean sendMessage, boolean protectItem)
	{
		item = _inventory.dropItem(process, item, this, reference);

		if (item == null)
		{
			if (sendMessage)
			{
				sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
			}

			return false;
		}

		item.dropMe(this, (getX() + Rnd.get(50)) - 25, (getY() + Rnd.get(50)) - 25, getZ(), true);

		if ((Config.AUTODESTROY_ITEM_AFTER > 0) && Config.DESTROY_DROPPED_PLAYER_ITEM && !Config.LIST_PROTECTED_ITEMS.contains(item.getId()))
		{
			if ((item.isEquipable() && Config.DESTROY_EQUIPABLE_PLAYER_ITEM) || !item.isEquipable())
			{
				final var manager = ItemsAutoDestroy.getInstance();
				if (manager.addItem(item, 0))
				{
					manager.tryRecalcTime();
				}
			}
		}

		if (Config.DESTROY_DROPPED_PLAYER_ITEM)
		{
			if (!item.isEquipable() || (item.isEquipable() && Config.DESTROY_EQUIPABLE_PLAYER_ITEM))
			{
				item.setProtected(false);
			}
			else
			{
				item.setProtected(true);
			}
		}
		else
		{
			item.setProtected(true);
		}

		if (protectItem)
		{
			item.getDropProtection().protect(this, false);
		}

		if (!Config.FORCE_INVENTORY_UPDATE)
		{
			final InventoryUpdate playerIU = new InventoryUpdate();
			playerIU.addItem(item);
			sendPacket(playerIU);
		}
		else
		{
			sendItemList(false);
		}
		
		if (item.getId() != PcInventory.ADENA_ID)
		{
			sendPacket(makeStatusUpdate(StatusUpdate.CUR_LOAD));
		}

		if (sendMessage)
		{
			final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.YOU_DROPPED_S1);
			sm.addItemName(item);
			sendPacket(sm);
		}
		return true;
	}

	public boolean dropItem(String process, ItemInstance item, GameObject reference, boolean sendMessage)
	{
		return dropItem(process, item, reference, sendMessage, false);
	}

	public ItemInstance dropItem(String process, int objectId, long count, int x, int y, int z, GameObject reference, boolean sendMessage, boolean protectItem)
	{
		final ItemInstance invitem = _inventory.getItemByObjectId(objectId);
		final ItemInstance item = _inventory.dropItem(process, objectId, count, this, reference);

		if (item == null)
		{
			if (sendMessage)
			{
				sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
			}

			return null;
		}

		item.dropMe(this, x, y, z, true);

		if ((Config.AUTODESTROY_ITEM_AFTER > 0) && Config.DESTROY_DROPPED_PLAYER_ITEM && !Config.LIST_PROTECTED_ITEMS.contains(item.getId()))
		{
			if ((item.isEquipable() && Config.DESTROY_EQUIPABLE_PLAYER_ITEM) || !item.isEquipable())
			{
				final var manager = ItemsAutoDestroy.getInstance();
				if (manager.addItem(item, 0))
				{
					manager.tryRecalcTime();
				}
			}
		}
		if (Config.DESTROY_DROPPED_PLAYER_ITEM)
		{
			if (!item.isEquipable() || (item.isEquipable() && Config.DESTROY_EQUIPABLE_PLAYER_ITEM))
			{
				item.setProtected(false);
			}
			else
			{
				item.setProtected(true);
			}
		}
		else
		{
			item.setProtected(true);
		}

		if (protectItem)
		{
			item.getDropProtection().protect(this, false);
		}

		if (!Config.FORCE_INVENTORY_UPDATE)
		{
			final InventoryUpdate playerIU = new InventoryUpdate();
			playerIU.addItem(invitem);
			sendPacket(playerIU);
		}
		else
		{
			sendItemList(false);
		}
		
		if (item.getId() != PcInventory.ADENA_ID)
		{
			sendPacket(makeStatusUpdate(StatusUpdate.CUR_LOAD));
		}

		if (sendMessage)
		{
			final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.YOU_DROPPED_S1);
			sm.addItemName(item);
			sendPacket(sm);
		}
		return item;
	}

	public ItemInstance checkItemManipulation(int objectId, long count, String action)
	{
		if (GameObjectsStorage.getItem(objectId) == null)
		{
			if (Config.DEBUG)
			{
				_log.warn(getObjectId() + ": player tried to " + action + " item not available in World");
			}
			return null;
		}
		
		final ItemInstance item = getInventory().getItemByObjectId(objectId);
		if ((item == null) || (item.getOwnerId() != getObjectId()))
		{
			if (Config.DEBUG)
			{
				_log.warn(getObjectId() + ": player tried to " + action + " item he is not owner of");
			}
			return null;
		}

		if ((count < 0) || ((count > 1) && !item.isStackable()))
		{
			if (Config.DEBUG)
			{
				_log.warn(getObjectId() + ": player tried to " + action + " item with invalid count: " + count);
			}
			return null;
		}

		if (count > item.getCount())
		{
			if (Config.DEBUG)
			{
				_log.warn(getObjectId() + ": player tried to " + action + " more items than he owns");
			}
			return null;
		}

		if ((hasSummon() && (getSummon().getControlObjectId() == objectId)) || (getMountObjectID() == objectId))
		{
			if (Config.DEBUG)
			{
				_log.warn(getObjectId() + ": player tried to " + action + " item controling pet");
			}

			return null;
		}

		if (getActiveEnchantItemId() == objectId)
		{
			if (Config.DEBUG)
			{
				_log.warn(getObjectId() + ":player tried to " + action + " an enchant scroll he was using");
			}
			return null;
		}

		if (item.isAugmented() && (isCastingNow() || isCastingSimultaneouslyNow()))
		{
			return null;
		}

		return item;
	}

	public void setProtection(boolean protect)
	{
		if (Config.DEVELOPER && (protect || (_protectEndTime > 0)))
		{
			_log.warn(getName(null) + ": Protection " + (protect ? "ON " + (GameTimeController.getInstance().getGameTicks() + (Config.PLAYER_SPAWN_PROTECTION * GameTimeController.TICKS_PER_SECOND)) : "OFF") + " (currently " + GameTimeController.getInstance().getGameTicks() + ")");
		}
		_protectEndTime = protect ? GameTimeController.getInstance().getGameTicks() + (Config.PLAYER_SPAWN_PROTECTION * GameTimeController.TICKS_PER_SECOND) : 0;
	}

	public void setTeleportProtection(boolean protect)
	{
		if (Config.DEVELOPER && (protect || (_teleportProtectEndTime > 0)))
		{
			_log.warn(getName(null) + ": Tele Protection " + (protect ? "ON " + (GameTimeController.getInstance().getGameTicks() + (Config.PLAYER_TELEPORT_PROTECTION * GameTimeController.TICKS_PER_SECOND)) : "OFF") + " (currently " + GameTimeController.getInstance().getGameTicks() + ")");
		}
		_teleportProtectEndTime = protect ? GameTimeController.getInstance().getGameTicks() + (Config.PLAYER_TELEPORT_PROTECTION * GameTimeController.TICKS_PER_SECOND) : 0;
	}

	public void setRecentFakeDeath(boolean protect)
	{
		_recentFakeDeathEndTime = protect ? GameTimeController.getInstance().getGameTicks() + (Config.PLAYER_FAKEDEATH_UP_PROTECTION * GameTimeController.TICKS_PER_SECOND) : 0;
	}

	public boolean isRecentFakeDeath()
	{
		return _recentFakeDeathEndTime > GameTimeController.getInstance().getGameTicks();
	}

	public final boolean isFakeDeath()
	{
		return _isFakeDeath;
	}

	public final void setIsFakeDeath(boolean value)
	{
		_isFakeDeath = value;
	}

	@Override
	public final boolean isAlikeDead()
	{
		return super.isAlikeDead() || isFakeDeath();
	}

	public GameClient getClient()
	{
		return _client;
	}

	public void setClient(GameClient client)
	{
		_client = client;
	}

	public String getIPAddress()
	{
		return _client != null ? _client.isDetached() ? "Disconnected" : _client.getIPAddress() : "N/A";
	}
	
	public String getDefaultAddress()
	{
		return _client != null ? _client.isDetached() ? "" : _client.getDefaultAddress() : "";
	}

	public String getHWID()
	{
		return _client != null ? _client.getHWID() : "N/A";
	}
	
	public int getRequestId()
	{
		return _client != null ? _client.getRequestId() : Config.REQUEST_ID;
	}
	
	public boolean checkFloodProtection(String type, String command)
	{
		return _client == null ? false : _client.checkFloodProtection(type, command);
	}
	
	public boolean isConnected()
	{
		return _client != null && _client.isConnected();
	}

	public boolean canOfflineMode(Player player, boolean sendMsg)
	{
		boolean canSetShop = false;
		if (player.getTransformationId() > 0 || player.isInFightEvent() || player.isInOlympiadMode() || player.isFestivalParticipant() || player.isJailed() || (player.getVehicle() != null))
		{
			return false;
		}
		
		if (player.getLevel() < Config.OFFLINE_TRADE_MIN_LVL)
		{
			if (sendMsg)
			{
				player.sendMessage((new ServerMessage("CommunityGeneral.YOU_LEVEL_LOW", player.getLang())).toString());
			}
			return false;
		}
		
		if (Config.OFFLINE_MODE_PRICE[0] > 0 && !OfflineTaskManager.getInstance().isActivePlayer(this))
		{
			if (player.getInventory().getItemByItemId(Config.OFFLINE_MODE_PRICE[0]) == null || player.getInventory().getItemByItemId(Config.OFFLINE_MODE_PRICE[0]).getCount() < Config.OFFLINE_MODE_PRICE[1])
			{
				if (sendMsg)
				{
					player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
				}
				return false;
			}
		}
		
		if (Config.OFFLINE_TRADE_ENABLE && ((player.isSellingBuffs() && player.getPrivateStoreType() == Player.STORE_PRIVATE_PACKAGE_SELL) || ((player.getPrivateStoreType() == Player.STORE_PRIVATE_PACKAGE_SELL) || (player.getPrivateStoreType() == Player.STORE_PRIVATE_SELL) || (player.getPrivateStoreType() == Player.STORE_PRIVATE_BUY))))
		{
			canSetShop = true;
		}
		else if (Config.OFFLINE_CRAFT_ENABLE && (player.isInCraftMode() || (player.getPrivateStoreType() == Player.STORE_PRIVATE_MANUFACTURE)))
		{
			canSetShop = true;
		}
		
		if (Config.OFFLINE_MODE_IN_PEACE_ZONE && !player.isInsideZone(ZoneId.PEACE))
		{
			canSetShop = false;
		}
		return canSetShop;
	}
	
	public Location getCurrentSkillWorldPosition()
	{
		return _currentSkillWorldPosition;
	}

	public void setCurrentSkillWorldPosition(Location worldPosition)
	{
		_currentSkillWorldPosition = worldPosition;
	}

	@Override
	public void enableSkill(Skill skill)
	{
		super.enableSkill(skill);
		_reuseTimeStampsSkills.remove(skill.getReuseHashCode());
	}

	@Override
	public boolean checkDoCastConditions(Skill skill, boolean msg)
	{
		if (!super.checkDoCastConditions(skill, msg))
		{
			return false;
		}

		if ((skill.getSkillType() == SkillType.SUMMON) && (hasSummon() || isMounted() || inObserverMode()))
		{
			if (msg)
			{
				sendPacket(SystemMessageId.SUMMON_ONLY_ONE);
			}
			return false;
		}

		if (isInOlympiadMode() && (skill.isHeroSkill() || (skill.getSkillType() == SkillType.RESURRECT)))
		{
			if (msg)
			{
				sendPacket(SystemMessageId.THIS_SKILL_IS_NOT_AVAILABLE_FOR_THE_OLYMPIAD_EVENT);
			}
			return false;
		}

		if (((getCharges() < skill.getChargeConsume())) || (isInAirShip() && !skill.hasEffectType(EffectType.REFUEL_AIRSHIP)))
		{
			if (msg)
			{
				final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
				sm.addSkillName(skill);
				sendPacket(sm);
			}
			return false;
		}
		
		if (getActiveTradeList() != null)
		{
			cancelActiveTrade();
		}
		return true;
	}

	private boolean needCpUpdate()
	{
		final double currentCp = getCurrentCp();

		if ((currentCp <= 1.0) || (getMaxCp() < MAX_HP_BAR_PX))
		{
			return true;
		}

		if ((currentCp <= _cpUpdateDecCheck) || (currentCp >= _cpUpdateIncCheck))
		{
			if (currentCp == getMaxCp())
			{
				_cpUpdateIncCheck = currentCp + 1;
				_cpUpdateDecCheck = currentCp - _cpUpdateInterval;
			}
			else
			{
				final double doubleMulti = currentCp / _cpUpdateInterval;
				int intMulti = (int) doubleMulti;

				_cpUpdateDecCheck = _cpUpdateInterval * (doubleMulti < intMulti ? intMulti-- : intMulti);
				_cpUpdateIncCheck = _cpUpdateDecCheck + _cpUpdateInterval;
			}

			return true;
		}

		return false;
	}

	private boolean needMpUpdate()
	{
		final double currentMp = getCurrentMp();

		if ((currentMp <= 1.0) || (getMaxMp() < MAX_HP_BAR_PX))
		{
			return true;
		}

		if ((currentMp <= _mpUpdateDecCheck) || (currentMp >= _mpUpdateIncCheck))
		{
			if (currentMp == getMaxMp())
			{
				_mpUpdateIncCheck = currentMp + 1;
				_mpUpdateDecCheck = currentMp - _mpUpdateInterval;
			}
			else
			{
				final double doubleMulti = currentMp / _mpUpdateInterval;
				int intMulti = (int) doubleMulti;

				_mpUpdateDecCheck = _mpUpdateInterval * (doubleMulti < intMulti ? intMulti-- : intMulti);
				_mpUpdateIncCheck = _mpUpdateDecCheck + _mpUpdateInterval;
			}

			return true;
		}

		return false;
	}
	
	private class BroadcastStatusUpdateTask implements Runnable
	{
		@Override
		public void run()
		{
			broadcastStatusUpdateImpl();
			_broadcastStatusUpdateTask = null;
		}
	}

	@Override
	public void broadcastStatusUpdate()
	{
		try
		{
			if (_broadcastStatusUpdateTask != null)
			{
				return;
			}
			_broadcastStatusUpdateTask = ThreadPoolManager.getInstance().schedule(new BroadcastStatusUpdateTask(), Config.BROADCAST_STATUS_UPDATE_INTERVAL);
		}
		catch (final Exception e)
		{}
	}
	
	public void broadcastStatusUpdateImpl()
	{
		final boolean needCpUpdate = needCpUpdate();
		final boolean needHpUpdate = needHpUpdate();
		final boolean needMpUpdate = needMpUpdate();
		
		if (!needCpUpdate && !needHpUpdate && !needMpUpdate)
		{
			return;
		}
		
		final var su = makeStatusUpdate(StatusUpdate.MAX_HP, StatusUpdate.CUR_HP, StatusUpdate.MAX_MP, StatusUpdate.CUR_MP, StatusUpdate.MAX_CP, StatusUpdate.CUR_CP);
		sendPacket(su);

		if (isInParty() && (needCpUpdate || needHpUpdate || needMpUpdate))
		{
			getParty().broadcastToPartyMembers(this, new PartySmallWindowUpdate(this));
		}

		if (isInOlympiadMode() && isOlympiadStart() && (needCpUpdate || needHpUpdate))
		{
			final var game = OlympiadGameManager.getInstance().getOlympiadTask(getOlympiadGameId());
			if ((game != null) && game.isBattleStarted())
			{
				game.getZone().broadcastStatusUpdate(this);
			}
		}

		final Tournament tournament = TournamentData.getInstance().getTournament(getTournamentGameId());
		if ((tournament != null) && tournament.isRunning())
		{
			tournament.broadcastStatusUpdate(this);
		}

		if (isInDuel() && (needCpUpdate || needHpUpdate))
		{
			DuelManager.getInstance().broadcastToOppositTeam(this, new ExDuelUpdateUserInfo(this));
		}
	}
	
	@Override
	public StatusUpdate makeStatusUpdate(int... fields)
	{
		final var su = new StatusUpdate(this);
		for (final int field : fields)
		{
			switch (field)
			{
				case StatusUpdate.CUR_HP :
					su.addAttribute(field, (int) getCurrentHp());
					break;
				case StatusUpdate.MAX_HP :
					su.addAttribute(field, getMaxHp());
					break;
				case StatusUpdate.CUR_MP :
					su.addAttribute(field, (int) getCurrentMp());
					break;
				case StatusUpdate.MAX_MP :
					su.addAttribute(field, getMaxMp());
					break;
				case StatusUpdate.CUR_LOAD :
					su.addAttribute(field, getCurrentLoad());
					break;
				case StatusUpdate.MAX_LOAD :
					su.addAttribute(field, getMaxLoad());
					break;
				case StatusUpdate.LEVEL :
					su.addAttribute(field, getLevel());
					break;
				case StatusUpdate.PVP_FLAG :
					su.addAttribute(field, getPvpFlag());
					break;
				case StatusUpdate.KARMA :
					su.addAttribute(field, getKarma());
					break;
				case StatusUpdate.CUR_CP :
					su.addAttribute(field, (int) getCurrentCp());
					break;
				case StatusUpdate.MAX_CP :
					su.addAttribute(field, getMaxCp());
					break;
			}
		}
		return su;
	}
	
	public void sendStatusUpdate(boolean broadCast, boolean withPet, int... fields)
	{
		if (fields.length == 0 || _entering && !broadCast)
		{
			return;
		}
		
		final var su = makeStatusUpdate(fields);
		if (!su.hasAttributes())
		{
			return;
		}
		
		final List<GameServerPacket> packets = new ArrayList<>(withPet ? 2 : 1);
		if (withPet)
		{
			final var summon = getSummon();
			if (summon != null)
			{
				packets.add(summon.makeStatusUpdate(fields));
			}
		}
		packets.add(su);
		
		if (!broadCast)
		{
			sendPacket(packets);
		}
		else if (_entering)
		{
			broadcastPacketToOthers(packets);
		}
		else
		{
			broadcastPacket(packets);
		}
	}

	public final void broadcastTitleInfo()
	{
		sendUserInfo(true);
		broadcastPacket(new NickNameChanged(this));
	}

	@Override
	public int getAllyId()
	{
		if (_clan == null)
		{
			return 0;
		}
		return _clan.getAllyId();
	}

	public int getAllyCrestId()
	{
		if (getClanId() == 0)
		{
			return 0;
		}
		if (getClan().getAllyId() == 0)
		{
			return 0;
		}
		return getClan().getAllyCrestId();
	}

	public void queryGameGuard()
	{
		if (getClient() != null)
		{
			getClient().setGameGuardOk(false);
			if(!JGuard.isProtectEnabled()) {
				sendPacket(new GameGuardQuery());
			}
		}
	}

	@Override
	public void sendPacket(GameServerPacket packet)
	{
		if (_client != null)
		{
			_client.sendPacket(packet);
		}
	}

	@Override
	public void sendPacket(GameServerPacket... packets)
	{
		if (_client != null)
		{
			for (final GameServerPacket p : packets)
			{
				_client.sendPacket(p);
			}
		}
	}
	
	@Override
	public void sendPacket(List<? extends GameServerPacket> packets)
	{
		if (_client != null)
		{
			for (final GameServerPacket p : packets)
			{
				_client.sendPacket(p);
			}
		}
	}

	@Override
	public void sendPacket(GameServerPacket packet, SystemMessageId id)
	{
		if (_client != null)
		{
			_client.sendPacket(packet);
			if (id != null)
			{
				sendPacket(SystemMessage.getSystemMessage(id));
			}
		}
	}

	@Override
	public void sendPacket(SystemMessageId id)
	{
		sendPacket(SystemMessage.getSystemMessage(id));
	}

	public void doInteract(Creature target)
	{
		if ((target == null) || isActionsDisabled())
		{
			sendActionFailed();
			return;
		}
		
		if (target instanceof Player)
		{
			final Player temp = (Player) target;
			sendActionFailed();

			if ((temp.getPrivateStoreType() == STORE_PRIVATE_SELL) || (temp.getPrivateStoreType() == STORE_PRIVATE_PACKAGE_SELL))
			{
				if (temp.isSellingBuffs())
				{
					SellBuffsManager.sendBuffMenu(this, temp, 1);
				}
				else
				{
					sendPacket(new PrivateStoreSellList(this, temp));
				}
			}
			else if (temp.getPrivateStoreType() == STORE_PRIVATE_BUY)
			{
				sendPacket(new PrivateStoreBuyList(this, temp));
			}
			else if (temp.getPrivateStoreType() == STORE_PRIVATE_MANUFACTURE)
			{
				sendPacket(new RecipeShopSellList(this, temp));
			}

		}
		else
		{
			target.onAction(this, false);
		}
	}
	
	public void doAutoLoot(Attackable target, int itemId, long itemCount)
	{
		if (Config.DISABLE_ITEM_DROP_LIST.contains(itemId))
		{
			return;
		}
		
		if (isInParty() && !ItemsParser.getInstance().getTemplate(itemId).isHerb())
		{
			getParty().distributeItem(this, itemId, itemCount, false, target);
			return;
		}
		
		if (itemId == PcInventory.ADENA_ID)
		{
			addAdena("Loot", itemCount, target, true);
		}
		else
		{
			addItem("Loot", itemId, itemCount, target, true);
		}
	}

	public void doAutoLoot(Attackable target, ItemHolder item)
	{
		doAutoLoot(target, item.getId(), item.getCount());
	}
	
	public void doPickupItem(GameObject object)
	{
		if (isAlikeDead() || isFakeDeathNow())
		{
			return;
		}

		getAI().setIntention(CtrlIntention.IDLE);

		if (!(object instanceof ItemInstance))
		{
			_log.warn(this + " trying to pickup wrong target." + getTarget());
			return;
		}

		final ItemInstance target = (ItemInstance) object;

		sendActionFailed();

		final StopMove sm = new StopMove(this);
		sendPacket(sm);

		SystemMessage smsg = null;
		synchronized (target)
		{
			if (!target.isVisible())
			{
				sendActionFailed();
				return;
			}

			if (!target.getDropProtection().tryPickUp(this))
			{
				sendActionFailed();
				smsg = SystemMessage.getSystemMessage(SystemMessageId.FAILED_TO_PICKUP_S1);
				smsg.addItemName(target);
				sendPacket(smsg);
				return;
			}

			if (((isInParty() && (getParty().getLootDistribution() == Party.ITEM_LOOTER)) || !isInParty()) && !_inventory.validateCapacity(target))
			{
				sendActionFailed();
				sendPacket(SystemMessageId.SLOTS_FULL);
				return;
			}

			if (isInvul() && !canOverrideCond(PcCondOverride.ITEM_CONDITIONS))
			{
				sendActionFailed();
				smsg = SystemMessage.getSystemMessage(SystemMessageId.FAILED_TO_PICKUP_S1);
				smsg.addItemName(target);
				sendPacket(smsg);
				return;
			}

			if ((target.getOwnerId() != 0) && (target.getOwnerId() != getObjectId()) && !isInLooterParty(target.getOwnerId()))
			{
				if (target.getId() == PcInventory.ADENA_ID)
				{
					smsg = SystemMessage.getSystemMessage(SystemMessageId.FAILED_TO_PICKUP_S1_ADENA);
					smsg.addItemNumber(target.getCount());
				}
				else if (target.getCount() > 1)
				{
					smsg = SystemMessage.getSystemMessage(SystemMessageId.FAILED_TO_PICKUP_S2_S1_S);
					smsg.addItemName(target);
					smsg.addItemNumber(target.getCount());
				}
				else
				{
					smsg = SystemMessage.getSystemMessage(SystemMessageId.FAILED_TO_PICKUP_S1);
					smsg.addItemName(target);
				}
				sendActionFailed();
				sendPacket(smsg);
				return;
			}

			if (FortSiegeManager.getInstance().isCombat(target.getId()) && !isInFightEvent())
			{
				if (!FortSiegeManager.getInstance().checkIfCanPickup(this))
				{
					return;
				}
			}
			target.pickupMe(this);
		}

		if (target.getItem().isHerb())
		{
			final IItemHandler handler = ItemHandler.getInstance().getHandler(target.getEtcItem());
			if (handler == null)
			{
				_log.warn("No item handler registered for item ID: " + target.getId() + ".");
			}
			else
			{
				handler.useItem(this, target, false);
				if ((getSummon() != null) && getSummon().isServitor() && !getSummon().isDead())
				{
					handler.useItem(getSummon(), target, false);
				}
			}
			ItemsParser.getInstance().destroyItem("Consume", target, this, null);
		}
		else if (CursedWeaponsManager.getInstance().isCursed(target.getId()))
		{
			addItem("Pickup", target, null, true);
		}
		else if (FortSiegeManager.getInstance().isCombat(target.getId()) && !isInFightEvent())
		{
			addItem("Pickup", target, null, true);
		}
		else
		{
			if ((target.getItemType() instanceof ArmorType) || (target.getItemType() instanceof WeaponType))
			{
				if (target.getEnchantLevel() > 0)
				{
					smsg = SystemMessage.getSystemMessage(SystemMessageId.ANNOUNCEMENT_C1_PICKED_UP_S2_S3);
					smsg.addPcName(this);
					smsg.addNumber(target.getEnchantLevel());
					smsg.addItemName(target.getId());
					broadcastPacket(1400, smsg);
				}
				else
				{
					smsg = SystemMessage.getSystemMessage(SystemMessageId.ANNOUNCEMENT_C1_PICKED_UP_S2);
					smsg.addPcName(this);
					smsg.addItemName(target.getId());
					broadcastPacket(1400, smsg);
				}
			}

			if (isInParty())
			{
				getParty().distributeItem(this, target);
			}
			else if ((target.getId() == PcInventory.ADENA_ID) && (getInventory().getAdenaInstance() != null))
			{
				addAdena("Pickup", target.getCount(), null, true);
				ItemsParser.getInstance().destroyItem("Pickup", target, this, null);
			}
			else
			{
				addItem("Pickup", target, null, true);
				if (target.isEtcItem())
				{
					checkToEquipArrows(target);
				}
				else if (target.isTalisman())
				{
					checkCombineTalisman(target, false);
				}
			}
		}
	}
	
	public void checkToEquipArrows(ItemInstance newItem)
	{
		if (newItem != null)
		{
			final ItemInstance weapon = getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
			if (weapon != null)
			{
				final EtcItem etcItem = newItem.getEtcItem();
				if (etcItem != null)
				{
					final EtcItemType itemType = etcItem.getItemType();
					if ((weapon.getItemType() == WeaponType.BOW) && (itemType == EtcItemType.ARROW))
					{
						checkAndEquipArrows();
					}
					else if ((weapon.getItemType() == WeaponType.CROSSBOW) && (itemType == EtcItemType.BOLT))
					{
						checkAndEquipBolts();
					}
				}
			}
		}
	}
	
	public void checkCombineTalisman(ItemInstance newItem, boolean sendMessage)
	{
		if (!Config.AUTO_COMBINE_TALISMANS && !sendMessage)
		{
			return;
		}
		
		if (newItem != null || sendMessage)
		{
			final List<int[]> _sameIds = new ArrayList<>();
			
			for (final var item : getInventory().getItems())
			{
				if ((item.getMana() > 0) && item.getItem().isTalisman())
				{
					for (var i = 0; i < _sameIds.size(); i++)
					{
						if (_sameIds.get(i)[0] == item.getId())
						{
							_sameIds.get(i)[1] = _sameIds.get(i)[1] + 1;
							break;
						}
					}
					_sameIds.add(new int[]
					{
					        item.getId(), 1
					});
				}
			}
			
			var newCount = 0;
			for (final var idCount : _sameIds)
			{
				if (idCount[1] > 1)
				{
					var lifeTime = 0;
					final var existingTalismans = getInventory().getItemsByItemId(idCount[0]);
					for (final var existingTalisman : existingTalismans)
					{
						lifeTime += existingTalisman.getMana();
						getInventory().destroyItem("Combine Talismans", existingTalisman, this, null);
					}
					
					final var newTalisman = addItem("Combine talismans", idCount[0], 1, null, false);
					newTalisman.setMana(lifeTime);
					newTalisman.updateDatabase();
					newCount++;
				}
			}
			
			if (newCount > 0)
			{
				sendItemList(false);
			}
			
			if (sendMessage)
			{
				if (newCount > 0)
				{
					sendMessage("You have combined " + newCount + " talismans.");
				}
				else
				{
					sendMessage("You don't have Talismans to combine!");
				}
			}
		}
	}
	
	private boolean isValidPrivateStoreZone(boolean isTrade, boolean isSellBuff)
	{
		final var inNone = getPrivateStoreType() == Player.STORE_PRIVATE_NONE || getPrivateStoreType() == (Player.STORE_PRIVATE_BUY + 1) || getPrivateStoreType() == (Player.STORE_PRIVATE_SELL + 1);
		if (Config.TRADE_ONLY_IN_PEACE_ZONE && !isInZonePeace() && !isSellBuff && inNone)
		{
			sendPacket(SystemMessageId.NO_PRIVATE_STORE_HERE);
			return false;
		}
		
		if (isInsideZone(ZoneId.NO_TRADE) && !isSellBuff && inNone)
		{
			sendPacket(isTrade ? SystemMessageId.NO_PRIVATE_STORE_HERE : SystemMessageId.NO_PRIVATE_WORKSHOP_HERE);
			return false;
		}
		
		if (World.getInstance().getAroundTraders(this, 80, 200) > 0 && inNone)
		{
			sendMessage((new ServerMessage("TRADE.OTHER_TRADERS", getLang())).toString());
			return false;
		}
		
		if (!Config.ALLOW_TRADE_IN_ZONE && !isSellBuff)
		{
			return true;
		}
		
		if (!Config.ALLOW_SELLBUFFS_ZONE && isSellBuff)
		{
			return true;
		}
		
		final List<ZoneType> zones = ZoneManager.getInstance().getZones(this);
		if (zones != null && !zones.isEmpty())
		{
			for (final ZoneType zone : zones)
			{
				if (zone != null)
				{
					if (Config.ALLOW_TRADE_IN_ZONE && zone.isAllowStore() && !isSellBuff)
					{
						return true;
					}
					else if (Config.ALLOW_SELLBUFFS_ZONE && zone.isAllowSellBuff() && isSellBuff)
					{
						return true;
					}
				}
			}
		}
		
		if (!inNone)
		{
			return true;
		}
		
		if (isSellBuff)
		{
			sendMessage((new ServerMessage("SellBuff.WRONG_ZONE", getLang())).toString());
		}
		else if (isTrade)
		{
			sendPacket(SystemMessageId.NO_PRIVATE_STORE_HERE);
		}
		else
		{
			sendPacket(SystemMessageId.NO_PRIVATE_WORKSHOP_HERE);
		}
		sendActionFailed();
		return false;
	}

	public boolean canOpenPrivateStore(boolean isTrade, boolean isSellBuff)
	{
		return !isSellingBuffs() && !isAlikeDead() && !isInOlympiadMode() && !isMounted() && isValidPrivateStoreZone(isTrade, isSellBuff) && !isActionsDisabled() && !isInCombat() && !isInDuel() && !isProcessingRequest() && !isProcessingTransaction() && !isInFightEvent();
	}

	public void tryOpenPrivateBuyStore()
	{
		if (canOpenPrivateStore(true, false))
		{
			if ((getPrivateStoreType() == Player.STORE_PRIVATE_BUY) || (getPrivateStoreType() == (Player.STORE_PRIVATE_BUY + 1)))
			{
				setPrivateStoreType(Player.STORE_PRIVATE_NONE);
			}
			if (getPrivateStoreType() == Player.STORE_PRIVATE_NONE)
			{
				if (isSitting())
				{
					standUp();
				}
				setPrivateStoreType(Player.STORE_PRIVATE_BUY + 1);
				sendPacket(new PrivateStoreBuyManageList(this));
			}
		}
	}

	public void tryOpenPrivateSellStore(boolean isPackageSale)
	{
		if (canOpenPrivateStore(true, false))
		{
			if ((getPrivateStoreType() == Player.STORE_PRIVATE_SELL) || (getPrivateStoreType() == (Player.STORE_PRIVATE_SELL + 1)) || (getPrivateStoreType() == Player.STORE_PRIVATE_PACKAGE_SELL))
			{
				setPrivateStoreType(Player.STORE_PRIVATE_NONE);
			}

			if (getPrivateStoreType() == Player.STORE_PRIVATE_NONE)
			{
				if (isSitting())
				{
					standUp();
				}
				setPrivateStoreType(Player.STORE_PRIVATE_SELL + 1);
				sendPacket(new PrivateStoreSellManageList(this, isPackageSale));
			}
		}
	}

	public final PreparedListContainer getMultiSell()
	{
		return _currentMultiSell;
	}

	public final void setMultiSell(PreparedListContainer list)
	{
		_currentMultiSell = list;
	}

	@Override
	public boolean isTransformed()
	{
		return (_transformation != null) && !_transformation.isStance();
	}

	public boolean isInStance()
	{
		return (_transformation != null) && _transformation.isStance();
	}

	public void transform(Transform transformation)
	{
		if (_transformation != null)
		{
			final SystemMessage msg = SystemMessage.getSystemMessage(SystemMessageId.YOU_ALREADY_POLYMORPHED_AND_CANNOT_POLYMORPH_AGAIN);
			sendPacket(msg);
			return;
		}
		
		final FunPvpZone zone = ZoneManager.getInstance().getZone(this, FunPvpZone.class);
		if (zone != null)
		{
			if (zone.isBlockTransfroms() && zone.isBlockedTransfrom(transformation.getId()))
			{
				sendPacket(SystemMessageId.NOTHING_HAPPENED);
				return;
			}
		}
		
		setQueuedSkill(null, false, false);
		if (isMounted())
		{
			dismount();
		}
		_transformation = transformation;
		final TransformTemplate template = transformation.getTemplate(this);
		if (template != null)
		{
			if (hasSummon())
			{
				final Summon summon = getSummon();
				if (summon != null && transformation.isFlying())
				{
					summon.unSummon(this);
				}
			}
			
			for (final Effect effect : getAllEffects())
			{
				if (effect != null)
				{
					if (effect.getSkill().isToggle() || effect.getSkill().getId() == 1557)
					{
						boolean found = false;
						
						if (effect.getSkill().getId() != 1557)
						{
							for (final SkillHolder holder : template.getSkills())
							{
								if (holder.getId() == effect.getSkill().getId())
								{
									found = true;
								}
							}
						}
						
						if (!found)
						{
							effect.exit();
						}
					}
				}
			}
		}
		transformation.onTransform(this);
		
		sendSkillList(true);
		broadcastUserInfo(true);
		broadcastTargetUpdate();
	}

	@Override
	public void untransform()
	{
		if (_transformation != null)
		{
			setQueuedSkill(null, false, false);
			_transformation.onUntransform(this);
			_transformation = null;
			
			Skill skill = null;
			for (final Effect effect : getAllEffects())
			{
				if (effect != null)
				{
					if (effect.getSkill().hasEffectType(EffectType.TRANSFORMATION))
					{
						skill = effect.getSkill();
					}
				}
			}
			
			if (skill != null)
			{
				stopSkillEffects(skill.getId());
			}
			sendSkillList(true);
			broadcastUserInfo(true);
		}
	}

	@Override
	public Transform getTransformation()
	{
		return _transformation;
	}

	public int getTransformationId()
	{
		return (isTransformed() ? getTransformation().getId() : 0);
	}
	
	public void setFastTarget(GameObject newTarget)
	{
		super.setTarget(newTarget);
	}

	@Override
	public void setTarget(GameObject newTarget)
	{
		if (newTarget != null)
		{
			final boolean isInParty = (newTarget.isPlayer() && isInParty() && getParty().containsPlayer(newTarget.getActingPlayer()));

			if (!isInParty && (Math.abs(newTarget.getZ() - getZ()) > 1000))
			{
				newTarget = null;
			}

			if ((newTarget != null) && !isInParty && !newTarget.isVisible())
			{
				newTarget = null;
			}

			if (!isGM() && (newTarget instanceof Vehicle))
			{
				newTarget = null;
			}
		}

		final GameObject oldTarget = getTarget();

		if (oldTarget != null)
		{
			if (oldTarget.equals(newTarget))
			{
				if ((newTarget != null) && (newTarget.getObjectId() != getObjectId()))
				{
					sendPacket(new ValidateLocation(newTarget));
				}
				return;
			}
			oldTarget.removeStatusListener(this);
		}

		if (newTarget instanceof Creature)
		{
			final Creature target = (Creature) newTarget;

			if (newTarget.getObjectId() != getObjectId())
			{
				sendPacket(new ValidateLocation(target));
			}

			sendPacket(new MyTargetSelected(this, target));
			target.addStatusListener(this);

			final var su = target.makeStatusUpdate(StatusUpdate.MAX_HP, StatusUpdate.CUR_HP);
			sendPacket(su);

			if(Config.ENABLE_SOMIK_INTERFACE)
			{
				final var debuffs = ((Creature) newTarget).getEffectList().getDebuffs();
				final var debuffMessage = new StringBuilder("[TargetDebuffList]");
				if ((debuffs != null) && !debuffs.isEmpty())
				{
					debuffs.stream()
							.filter(e -> (e != null) && (e.getSkill() != null))
							.forEach(e -> debuffMessage.append(e.getSkill().getIcon()).append(";"));
				}
				sendPacket(new CreatureSay(0, Say2.MSNCHAT, getName(null), debuffMessage.toString()));
			}

			broadcastPacket(new TargetSelected(getObjectId(), newTarget.getObjectId(), getLocation()));
			if (Config.ALLOW_CUSTOM_INTERFACE)
			{
				sendPacket(new ExAbnormalStatusUpdateFromTarget(target));
			}
		}

		if ((newTarget == null) && (getTarget() != null))
		{
			broadcastPacket(new TargetUnselected(this));
		}
		super.setTarget(newTarget);
	}

	@Override
	public ItemInstance getActiveWeaponInstance()
	{
		return getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
	}

	@Override
	public Weapon getActiveWeaponItem()
	{
		final ItemInstance weapon = getActiveWeaponInstance();
		if (weapon == null)
		{
			return getFistsWeaponItem();
		}

		return (Weapon) weapon.getItem();
	}

	public ItemInstance getChestArmorInstance()
	{
		return getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST);
	}

	public ItemInstance getLegsArmorInstance()
	{
		return getInventory().getPaperdollItem(Inventory.PAPERDOLL_LEGS);
	}

	public Armor getActiveChestArmorItem()
	{
		final ItemInstance armor = getChestArmorInstance();

		if (armor == null)
		{
			return null;
		}

		return (Armor) armor.getItem();
	}

	public Armor getActiveLegsArmorItem()
	{
		final ItemInstance legs = getLegsArmorInstance();

		if (legs == null)
		{
			return null;
		}

		return (Armor) legs.getItem();
	}

	public boolean isWearingHeavyArmor()
	{
		final ItemInstance legs = getLegsArmorInstance();
		final ItemInstance armor = getChestArmorInstance();

		if ((armor != null) && (legs != null))
		{
			if (((ArmorType) legs.getItemType() == ArmorType.HEAVY) && ((ArmorType) armor.getItemType() == ArmorType.HEAVY))
			{
				return true;
			}
		}
		if (armor != null)
		{
			if (((getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST).getItem().getBodyPart() == Item.SLOT_FULL_ARMOR) && ((ArmorType) armor.getItemType() == ArmorType.HEAVY)))
			{
				return true;
			}
		}
		return false;
	}

	public boolean isWearingLightArmor()
	{
		final ItemInstance legs = getLegsArmorInstance();
		final ItemInstance armor = getChestArmorInstance();

		if ((armor != null) && (legs != null))
		{
			if (((ArmorType) legs.getItemType() == ArmorType.LIGHT) && ((ArmorType) armor.getItemType() == ArmorType.LIGHT))
			{
				return true;
			}
		}
		if (armor != null)
		{
			if (((getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST).getItem().getBodyPart() == Item.SLOT_FULL_ARMOR) && ((ArmorType) armor.getItemType() == ArmorType.LIGHT)))
			{
				return true;
			}
		}
		return false;
	}

	public boolean isWearingMagicArmor()
	{
		final ItemInstance legs = getLegsArmorInstance();
		final ItemInstance armor = getChestArmorInstance();

		if ((armor != null) && (legs != null))
		{
			if (((ArmorType) legs.getItemType() == ArmorType.MAGIC) && ((ArmorType) armor.getItemType() == ArmorType.MAGIC))
			{
				return true;
			}
		}
		if (armor != null)
		{
			if (((getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST).getItem().getBodyPart() == Item.SLOT_FULL_ARMOR) && ((ArmorType) armor.getItemType() == ArmorType.MAGIC)))
			{
				return true;
			}
		}
		return false;
	}

	public boolean isMarried()
	{
		return _married;
	}

	public void setMarried(boolean state)
	{
		_married = state;
	}

	public boolean isEngageRequest()
	{
		return _engagerequest;
	}

	public void setEngageRequest(boolean state, int playerid)
	{
		_engagerequest = state;
		_engageid = playerid;
	}

	public void setMarryRequest(boolean state)
	{
		_marryrequest = state;
	}

	public boolean isMarryRequest()
	{
		return _marryrequest;
	}

	public void setMarryAccepted(boolean state)
	{
		_marryaccepted = state;
	}

	public boolean isMarryAccepted()
	{
		return _marryaccepted;
	}

	public int getEngageId()
	{
		return _engageid;
	}

	public int getPartnerId()
	{
		return _partnerId;
	}

	public void setPartnerId(int partnerid)
	{
		_partnerId = partnerid;
	}

	public int getCoupleId()
	{
		return _coupleId;
	}

	public void setCoupleId(int coupleId)
	{
		_coupleId = coupleId;
	}

	public void scriptAswer(int answer)
	{
		final Pair<Integer, OnAnswerListener> entry = getAskListener(true);
		if (entry == null)
		{
			return;
		}
				
		final OnAnswerListener listener = entry.getValue();
		if (answer == 1)
		{
			listener.sayYes();
		}
		else
		{
			listener.sayNo();
		}
	}

	@Override
	public ItemInstance getSecondaryWeaponInstance()
	{
		return getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
	}

	@Override
	public Item getSecondaryWeaponItem()
	{
		final ItemInstance item = getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
		if (item != null)
		{
			return item.getItem();
		}
		return null;
	}

	@Override
	protected void onDeath(Creature killer)
	{
		if (isMounted())
		{
			getPersonalTasks().removeTask(8, true);
		}

		synchronized (this)
		{
			if (isFakeDeathNow())
			{
				stopFakeDeath(true);
			}
		}
		
		if (isInFightEvent())
		{
			if (killer != null && killer.isPlayable() && killer.isInFightEvent())
			{
				final Player player = killer.isSummon() ? killer.getSummon().getOwner() : killer.getActingPlayer();
				if (player != null)
				{
					player.getFightEvent().onKilled(player, this);
				}
			}
			else if (isPlayer())
			{
				getFightEvent().onKilled(killer, this);
			}
			
			if (getFightEvent() instanceof MonsterAttackEvent)
			{
				((MonsterAttackEvent) getFightEvent()).checkAlivePlayer();
			}
		}

		if (killer != null && isInPartyTournament())
		{
			getPartyTournament().onKill(killer, this);
		}

		AerialCleftEvent.getInstance().onKill(killer, this);

		if (isInKrateisCube())
		{
			getArena().setRespawnTask(this);
			if (killer != null && killer.isPlayable())
			{
				final Player player = killer.isSummon() ? killer.getSummon().getOwner() : killer.getActingPlayer();
				if (player != null && player.isInKrateisCube())
				{
					player.getArena().addPoints(player, true);
				}
			}
		}
		
		if (killer != null)
		{
			final Player pk = killer.getActingPlayer();
			if (pk != null)
			{
				RevengeManager.getInstance().checkKiller(this, pk);
				final FunPvpZone zone = ZoneManager.getInstance().getZone(pk, FunPvpZone.class);
				if (zone != null && !isInSameParty(pk))
				{
					if (DoubleSessionManager.getInstance().check(pk, this))
					{
						zone.givereward(pk, this);
					}
				}
				
				if (getParty() != null && (getParty().getUCState() instanceof UCTeam))
				{
					((UCTeam) getParty().getUCState()).onKill(this, pk);
				}
				
				if (Config.ANNOUNCE_PK_PVP && !pk.isGM())
				{
					String msg = "";
					if (getPvpFlag() == 0)
					{
						msg = Config.ANNOUNCE_PK_MSG.replace("$killer", pk.getName(null)).replace("$target", getName(null));
						if (Config.ANNOUNCE_PK_PVP_NORMAL_MESSAGE)
						{
							final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1);
							sm.addString(msg);
							Announcements.getInstance().announceToAll(sm);
						}
						else
						{
							Announcements.getInstance().announceToAll(msg);
						}
					}
					else if (getPvpFlag() != 0)
					{
						msg = Config.ANNOUNCE_PVP_MSG.replace("$killer", pk.getName(null)).replace("$target", getName(null));
						if (Config.ANNOUNCE_PK_PVP_NORMAL_MESSAGE)
						{
							final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1);
							sm.addString(msg);
							Announcements.getInstance().announceToAll(sm);
						}
						else
						{
							Announcements.getInstance().announceToAll(msg);
						}
					}
				}

				if (isInParty() && Config.ALLOW_PARTY_RANK_COMMAND)
				{
					final PartyTemplate tpl = getParty().getMemberRank(this);
					if (tpl != null)
					{
						tpl.addDeaths();
					}
				}
			}

			broadcastStatusUpdate();
			setExpBeforeDeath(0);

			if (isCursedWeaponEquipped())
			{
				CursedWeaponsManager.getInstance().drop(_cursedWeaponEquippedId, killer);
				if (pk != null)
				{
					setIsWithoutCursed(true);
				}
			}
			else if (isCombatFlagEquipped() && !isInFightEvent())
			{
				if (TerritoryWarManager.getInstance().isTWInProgress())
				{
					TerritoryWarManager.getInstance().dropCombatFlag(this, true, false);
				}
				else
				{
					final Fort fort = FortManager.getInstance().getFort(this);
					if (fort != null)
					{
						FortSiegeManager.getInstance().dropCombatFlag(this, fort.getId());
					}
					else
					{
						final int slot = getInventory().getSlotFromItem(getInventory().getItemByItemId(9819));
						getInventory().unEquipItemInBodySlot(slot);
						destroyItem("CombatFlag", getInventory().getItemByItemId(9819), null, true);
					}
				}
			}
			else
			{
				if ((pk == null) || !pk.isCursedWeaponEquipped())
				{
					onDieDropItem(killer);

					if (!(isInsideZone(ZoneId.PVP) && !isInsideZone(ZoneId.SIEGE)))
					{
						if ((pk != null) && (pk.getClan() != null) && (getClan() != null) && !isAcademyMember() && !(pk.isAcademyMember()))
						{
							if ((_clan.isAtWarWith(pk.getClanId()) && pk.getClan().isAtWarWith(_clan.getId())) || (isInSiege() && pk.isInSiege()))
							{
								if (DoubleSessionManager.getInstance().check(killer, this))
								{
									if (getClan().getReputationScore() > 0)
									{
										pk.getClan().addReputationScore(Config.REPUTATION_SCORE_PER_KILL, false);
									}

									if (pk.getClan().getReputationScore() > 0)
									{
										_clan.takeReputationScore(Config.REPUTATION_SCORE_PER_KILL, false);
									}
									RewardManager.getInstance().checkClanWarReward(pk, this);
									pk.getCounters().addAchivementInfo("clanWarKills", 0, -1, false, false, false);
								}
							}
						}
					}

					if (Config.ALT_GAME_DELEVEL && !isLucky())
					{
						final boolean siegeNpc = (killer instanceof DefenderInstance) || (killer instanceof FortCommanderInstance);
						final boolean atWar = (pk != null) && (getClan() != null) && (getClan().isAtWarWith(pk.getClanId()));
						deathPenalty(killer, atWar, (pk != null), siegeNpc);
					}
				}
			}
		}

		stopCubics(false);

		if (_fusionSkill != null)
		{
			abortCast();
		}

		for (final Creature character : World.getInstance().getAroundCharacters(this))
		{
			if ((character.getFusionSkill() != null) && (character.getFusionSkill().getTarget() == this))
			{
				character.abortCast();
			}
		}

		if (isInParty() && getParty().isInDimensionalRift())
		{
			getParty().getDimensionalRift().checkDeath();
		}

		if (getAgathionId() != 0)
		{
			setAgathionId(0);
		}

		calculateDeathPenaltyBuffLevel(killer);

		final var task = getPersonalTasks().getActiveTask(12);
		if (task != null)
		{
			task.getTask(this);
			getPersonalTasks().removeTask(12, true);
		}
		getRecommendation().stopRecBonus();
		final var isFarming = getFarmSystem().isAutofarming();
		var haveRes = false;
		if (!isInFightEvent() && (isPhoenixBlessed() || (isAffected(EffectFlag.CHARM_OF_COURAGE) && isInsideZone(ZoneId.SIEGE))))
		{
			reviveRequest(this, null, 0, false, isFarming);
			haveRes = true;
		}
		
		if (isFarming && !haveRes)
		{
			getFarmSystem().checkTargetForRessurect();
		}
		super.onDeath(killer);
	}

	private void onDieDropItem(Creature killer)
	{
		if (killer == null || isInPartyTournament())
		{
			return;
		}

		final Player pk = killer.getActingPlayer();
		if ((getKarma() <= 0) && (pk != null) && (pk.getClan() != null) && (getClan() != null) && (pk.getClan().isAtWarWith(getClanId())))
		{
			return;
		}

		if ((!isInsideZone(ZoneId.PVP) || (pk == null)) && (!isGM() || Config.KARMA_DROP_GM))
		{
			boolean isKarmaDrop = false;
			final boolean isKillerNpc = (killer instanceof Npc);
			final int pkLimit = Config.KARMA_PK_LIMIT;

			int dropEquip = 0;
			int dropEquipWeapon = 0;
			int dropItem = 0;
			int dropLimit = 0;
			int dropPercent = 0;

			if ((getKarma() > 0) && (getPkKills() >= pkLimit))
			{
				isKarmaDrop = true;
				dropPercent = Config.KARMA_RATE_DROP;
				dropEquip = Config.KARMA_RATE_DROP_EQUIP;
				dropEquipWeapon = Config.KARMA_RATE_DROP_EQUIP_WEAPON;
				dropItem = Config.KARMA_RATE_DROP_ITEM;
				dropLimit = Config.KARMA_DROP_LIMIT;
			}
			else if (isKillerNpc && (getLevel() > 4) && !isFestivalParticipant())
			{
				dropPercent = Config.PLAYER_RATE_DROP;
				dropEquip = Config.PLAYER_RATE_DROP_EQUIP;
				dropEquipWeapon = Config.PLAYER_RATE_DROP_EQUIP_WEAPON;
				dropItem = Config.PLAYER_RATE_DROP_ITEM;
				dropLimit = Config.PLAYER_DROP_LIMIT;
			}

			if ((dropPercent > 0) && (Rnd.get(100) < dropPercent))
			{
				int dropCount = 0;

				int itemDropPercent = 0;

				for (final ItemInstance itemDrop : getInventory().getItems())
				{
					if (itemDrop.isShadowItem() || itemDrop.isTimeLimitedItem() || !itemDrop.isDropable() || (itemDrop.getId() == PcInventory.ADENA_ID) || (itemDrop.getItem().getType2() == Item.TYPE2_QUEST) || (hasSummon() && (getSummon().getControlObjectId() == itemDrop.getObjectId())) || (Arrays.binarySearch(Config.KARMA_LIST_NONDROPPABLE_ITEMS, itemDrop.getId()) >= 0) || (Arrays.binarySearch(Config.KARMA_LIST_NONDROPPABLE_PET_ITEMS, itemDrop.getId()) >= 0))
					{
						continue;
					}

					if (itemDrop.isEquipped())
					{
						itemDropPercent = itemDrop.getItem().getType2() == Item.TYPE2_WEAPON ? dropEquipWeapon : dropEquip;
						getInventory().unEquipItemInSlot(itemDrop.getLocationSlot());
					}
					else
					{
						itemDropPercent = dropItem;
					}

					if (Rnd.get(100) < itemDropPercent)
					{
						dropItem("DieDrop", itemDrop, killer, true);

						if (isKarmaDrop)
						{
							_log.warn(getName(null) + " has karma and dropped id = " + itemDrop.getId() + ", count = " + itemDrop.getCount());
						}
						else
						{
							_log.warn(getName(null) + " dropped id = " + itemDrop.getId() + ", count = " + itemDrop.getCount());
						}

						if (++dropCount >= dropLimit)
						{
							break;
						}
					}
				}
			}
		}
	}

	protected void onDieUpdateKarma()
	{
		if (getKarma() > 0)
		{
			double karmaLost = Config.KARMA_LOST_BASE;
			karmaLost *= getLevel();
			karmaLost *= (getLevel() / 100.0);
			karmaLost = Math.round(karmaLost);
			if (karmaLost <= 0)
			{
				karmaLost = 1;
			}
			setKarma(getKarma() - (int) karmaLost);
		}
	}

	public void onKillUpdatePvPKarma(Creature target)
	{
		if (target == null)
		{
			return;
		}
		if (!(target instanceof Playable))
		{
			return;
		}

		final Player targetPlayer = target.getActingPlayer();
		if (targetPlayer == null)
		{
			return;
		}

		if (targetPlayer == this || isInOlympiadMode())
		{
			return;
		}

		if(isInPartyTournament() && targetPlayer.isInPartyTournament() && TournamentUtil.TOURNAMENT_MAIN.isGivePvp())
		{
			setPvpKills(getPvpKills() + 1);
			getListeners().onPvp(targetPlayer);
			return;
		}

		if (isInFightEvent() && targetPlayer.isInFightEvent())
		{
			if (getFightEvent() != null && getFightEvent().givePvpPoints())
			{
				increasePvpKills(target);
				return;
			}
		}
		
		if (isCursedWeaponEquipped() && target.isPlayer())
		{
			CursedWeaponsManager.getInstance().increaseKills(_cursedWeaponEquippedId);
			return;
		}

		if (isInDuel() && targetPlayer.isInDuel())
		{
			return;
		}
		
		if (isInsideZone(ZoneId.FUN_PVP) && targetPlayer.isInsideZone(ZoneId.FUN_PVP))
		{
			final FunPvpZone zone = ZoneManager.getInstance().getZone(this, FunPvpZone.class);
			if (zone != null)
			{
				if (zone.allowPvpKills())
				{
					increasePvpKills(target);
				}
				
				if (!zone.isPvpZone() && targetPlayer.getPvpFlag() == 0)
				{
					increasePkKillsAndKarma(target);
				}
				return;
			}
		}

		if (isInsideZone(ZoneId.PVP) || targetPlayer.isInsideZone(ZoneId.PVP))
		{
			if ((getSiegeState() > 0) && (targetPlayer.getSiegeState() > 0) && (getSiegeState() != targetPlayer.getSiegeState()))
			{
				final Clan killerClan = getClan();
				final Clan targetClan = targetPlayer.getClan();
				if ((killerClan != null) && (targetClan != null))
				{
					if (DoubleSessionManager.getInstance().check(this, targetPlayer))
					{
						final SiegeZone zone = ZoneManager.getInstance().getZone(this, SiegeZone.class);
						if (zone != null)
						{
							if (zone.getFortId() > 0)
							{
								RewardManager.getInstance().checkFortPvpReward(this, targetPlayer);
								getCounters().addAchivementInfo("fortSiegePvpKills", 0, -1, false, false, false);
							}
							else if (zone.getCastleId() > 0)
							{
								zone.getCastlePvpReward(this, targetPlayer);
								getCounters().addAchivementInfo("siegePvpKills", 0, -1, false, false, false);
							}
						}
					}
					killerClan.addSiegeKill();
					targetClan.addSiegeDeath();
				}
			}
			return;
		}

		if ((checkIfPvP(target) && (targetPlayer.getPvpFlag() != 0)) || (isInsideZone(ZoneId.PVP) && targetPlayer.isInsideZone(ZoneId.PVP)))
		{
			increasePvpKills(target);
		}
		else
		{
			if ((targetPlayer.getClan() != null) && (getClan() != null) && getClan().isAtWarWith(targetPlayer.getClanId()) && targetPlayer.getClan().isAtWarWith(getClanId()) && (targetPlayer.getPledgeType() != Clan.SUBUNIT_ACADEMY) && (getPledgeType() != Clan.SUBUNIT_ACADEMY))
			{
				increasePvpKills(target);
				return;
			}

			if (targetPlayer.getKarma() > 0)
			{
				if (Config.KARMA_AWARD_PK_KILL)
				{
					increasePvpKills(target);
				}
			}
			else if (targetPlayer.getPvpFlag() == 0)
			{
				increasePkKillsAndKarma(target);
				checkItemRestriction();
			}
		}
	}

	public void increasePvpKills(Creature target)
	{
		if (target instanceof Player && DoubleSessionManager.getInstance().check(this, target) || target.isFakePlayer())
		{
			if (isInFightEvent() && target.isInFightEvent())
			{
				if (getFightEvent() != null && getFightEvent().givePvpPoints())
				{
					setPvpKills(getPvpKills() + 1);
					getListeners().onPvp(target.getActingPlayer());
				}
			}
			else
			{
				setPvpKills(getPvpKills() + 1);
				getListeners().onPvp(target.getActingPlayer());
				getCounters().addAchivementInfo("pvpKills", 0, -1, false, false, false);
				if (isInParty() && Config.ALLOW_PARTY_RANK_COMMAND)
				{
					final PartyTemplate tpl = getParty().getMemberRank(this);
					if (tpl != null)
					{
						tpl.addKills();
					}
				}
					
				if (Config.ALLOW_DAILY_TASKS && (getActiveDailyTasks() != null))
				{
					for (final PlayerTaskTemplate taskTemplate : getActiveDailyTasks())
					{
						if (taskTemplate.getType().equalsIgnoreCase("Pvp") && !taskTemplate.isComplete())
						{
							final DailyTaskTemplate task = DailyTaskManager.getInstance().getDailyTask(taskTemplate.getId());
							if ((taskTemplate.getCurrentPvpCount() < task.getPvpCount()) && DailyTaskManager.getInstance().checkHWID(this, target.getActingPlayer()))
							{
								taskTemplate.setCurrentPvpCount((taskTemplate.getCurrentPvpCount() + 1));
								if (taskTemplate.isComplete())
								{
									final IVoicedCommandHandler vch = VoicedCommandHandler.getInstance().getHandler("missions");
									if (vch != null)
									{
										updateDailyStatus(taskTemplate);
										vch.useVoicedCommand("missions", this, null);
									}
								}
							}
						}
					}
				}
				RewardManager.getInstance().checkPvpReward(this, target.getActingPlayer());
			}
		}
		sendUserInfo(true);
	}

	public void increasePkKillsAndKarma(Creature target)
	{
		if (isInFightEvent() || isInOlympiadMode())
		{
			return;
		}
		
		if (target.isPlayer() && target.getActingPlayer().isWithoutCursed())
		{
			target.getActingPlayer().setIsWithoutCursed(false);
			return;
		}

		final int baseKarma = Config.KARMA_MIN_KARMA;
		int newKarma = baseKarma;
		final int karmaLimit = Config.KARMA_MAX_KARMA;

		final int pkLVL = getLevel();
		final int pkPKCount = getPkKills();

		final int targLVL = target.getLevel();

		int lvlDiffMulti = 0;
		int pkCountMulti = 0;

		if (pkPKCount > 0)
		{
			pkCountMulti = pkPKCount / 2;
		}
		else
		{
			pkCountMulti = 1;
		}

		if (pkCountMulti < 1)
		{
			pkCountMulti = 1;
		}

		if (pkLVL > targLVL)
		{
			lvlDiffMulti = pkLVL / targLVL;
		}
		else
		{
			lvlDiffMulti = 1;
		}

		if (lvlDiffMulti < 1)
		{
			lvlDiffMulti = 1;
		}

		newKarma *= pkCountMulti;
		newKarma *= lvlDiffMulti;

		if (newKarma < baseKarma)
		{
			newKarma = baseKarma;
		}
		if (newKarma > karmaLimit)
		{
			newKarma = karmaLimit;
		}

		if (getKarma() > (Integer.MAX_VALUE - newKarma))
		{
			newKarma = Integer.MAX_VALUE - getKarma();
		}

		if (!isInFightEvent())
		{
			setKarma(getKarma() + newKarma);
		}

		if ((target instanceof Player) && DoubleSessionManager.getInstance().check(this, target) || target.isFakePlayer())
		{
			int newPks = 0;
			newPks = getPkKills() + 1;
			setPkKills(newPks);
			getCounters().addAchivementInfo("pkKills", 0, -1, false, false, false);
			
			if (isInParty() && Config.ALLOW_PARTY_RANK_COMMAND)
			{
				final PartyTemplate tpl = getParty().getMemberRank(this);
				if (tpl != null)
				{
					tpl.addKills();
				}
			}
			
			if (Config.ALLOW_DAILY_TASKS && (getActiveDailyTasks() != null))
			{
				for (final PlayerTaskTemplate taskTemplate : getActiveDailyTasks())
				{
					if (taskTemplate.getType().equalsIgnoreCase("Pk") && !taskTemplate.isComplete())
					{
						final DailyTaskTemplate task = DailyTaskManager.getInstance().getDailyTask(taskTemplate.getId());
						if ((taskTemplate.getCurrentPkCount() < task.getPkCount()) && DailyTaskManager.getInstance().checkHWID(this, target.getActingPlayer()))
						{
							taskTemplate.setCurrentPkCount((taskTemplate.getCurrentPkCount() + 1));
							if (taskTemplate.isComplete())
							{
								final IVoicedCommandHandler vch = VoicedCommandHandler.getInstance().getHandler("missions");
								if (vch != null)
								{
									updateDailyStatus(taskTemplate);
									vch.useVoicedCommand("missions", this, null);
								}
							}
						}
					}
				}
			}
		}
		sendUserInfo(true);
	}

	public int calculateKarmaLost(long exp)
	{
		long expGained = Math.abs(exp);
		expGained /= Config.KARMA_XP_DIVIDER;

		int karmaLost = 0;
		if (expGained > Integer.MAX_VALUE)
		{
			karmaLost = Integer.MAX_VALUE;
		}
		else
		{
			karmaLost = (int) expGained;
		}

		if (karmaLost < Config.KARMA_LOST_BASE)
		{
			karmaLost = Config.KARMA_LOST_BASE;
		}
		if (karmaLost > getKarma())
		{
			karmaLost = getKarma();
		}

		return karmaLost;
	}

	public void updatePvPStatus()
	{
		if (isInFightEvent() || isInPartyTournament())
		{
			return;
		}
		if (isInsideZone(ZoneId.PVP) || ZoneManager.getInstance().getOlympiadStadium(this) != null)
		{
			return;
		}
		setPvpFlagLasts(System.currentTimeMillis() + Config.PVP_NORMAL_TIME);

		if (getPvpFlag() == 0)
		{
			startPvPFlag();
		}
	}

	public void updatePvPStatus(Creature target)
	{
		final Player player_target = target.getActingPlayer();

		if (isInFightEvent() || isInPartyTournament())
		{
			return;
		}

		if (player_target == null)
		{
			return;
		}

		if ((isInDuel() && (player_target.getDuelId() == getDuelId())))
		{
			return;
		}
		if ((!isInsideZone(ZoneId.PVP) || !player_target.isInsideZone(ZoneId.PVP)) && (player_target.getKarma() == 0))
		{
			if (checkIfPvP(player_target))
			{
				setPvpFlagLasts(System.currentTimeMillis() + Config.PVP_PVP_TIME);
			}
			else
			{
				setPvpFlagLasts(System.currentTimeMillis() + Config.PVP_NORMAL_TIME);
			}
			if (getPvpFlag() == 0)
			{
				startPvPFlag();
			}
		}
	}

	public boolean isLucky()
	{
		return ((getLevel() <= 9) && (getFirstPassiveEffect(EffectType.LUCKY) != null));
	}

	public void restoreExp(double restorePercent)
	{
		if (getExpBeforeDeath() > 0)
		{
			getStat().addExp(Math.round(((getExpBeforeDeath() - getExp()) * restorePercent) / 100));
			setExpBeforeDeath(0);
		}
	}

	public void deathPenalty(Creature killer, boolean atwar, boolean isPlayer, boolean isSiege)
	{
		if (getNevitSystem().isBlessingActive() || isInFightEvent())
		{
			return;
		}
		
		final int lvl = getLevel();
		double percentLost = ExpPercentLostParser.getInstance().getExpPercent(getLevel());
		
		if (killer != null)
		{
			if (killer.isRaid())
			{
				percentLost *= calcStat(Stats.REDUCE_EXP_LOST_BY_RAID, 1, null, null);
			}
			else if (killer.isMonster())
			{
				percentLost *= calcStat(Stats.REDUCE_EXP_LOST_BY_MOB, 1, null, null);
			}
			else if (killer.isPlayable())
			{
				percentLost *= calcStat(Stats.REDUCE_EXP_LOST_BY_PVP, 1, null, null);
			}
		}

		if (getKarma() > 0)
		{
			percentLost *= Config.RATE_KARMA_EXP_LOST;
		}

		if (isFestivalParticipant() || atwar)
		{
			percentLost /= 4.0;
		}

		long lostExp = 0;
		if (lvl < ExperienceParser.getInstance().getMaxLevel())
		{
			lostExp = Math.round(((getStat().getExpForLevel(lvl + 1) - getStat().getExpForLevel(lvl)) * percentLost) / 100);
		}
		else
		{
			lostExp = Math.round(((getStat().getExpForLevel(ExperienceParser.getInstance().getMaxLevel()) - getStat().getExpForLevel(ExperienceParser.getInstance().getMaxLevel() - 1)) * percentLost) / 100);
		}

		setExpBeforeDeath(getExp());

		if (isInsideZone(ZoneId.PVP) || percentLost <= 0)
		{
			if (isInsideZone(ZoneId.SIEGE))
			{
				if (isInSiege() && (isPlayer || isSiege))
				{
					lostExp = 0;
				}
			}
			else if (isPlayer)
			{
				lostExp = 0;
			}
			
			if (percentLost <= 0)
			{
				lostExp = 0;
			}
		}
		getStat().removeExp(lostExp);
	}

	public void startTimers()
	{
		if (isFakePlayer())
		{
			return;
		}
		
		if (Config.PC_BANG_ENABLED && (Config.PC_BANG_INTERVAL > 0))
		{
			getPersonalTasks().addTask(new PcPointsTask(Config.PC_BANG_INTERVAL * 1000L));
		}
		
		if (Config.ENABLE_VITALITY)
		{
			getPersonalTasks().addTask(new VitalityTask(60000L));
		}
		
		if (Config.CHAR_STORE_INTERVAL > 0)
		{
			getPersonalTasks().addTask(new AutoSaveTask(Config.CHAR_STORE_INTERVAL * 60000L));
		}
		
		if (Config.CHAR_PREMIUM_ITEM_INTERVAL > 0)
		{
			if (!_premiumItems.isEmpty())
			{
				sendPacket(ExNotifyPremiumItem.STATIC_PACKET);
			}
			getPersonalTasks().addTask(new PremiumItemTask(Config.CHAR_PREMIUM_ITEM_INTERVAL * 60000L));
		}
		
		if (Config.DOUBLE_SESSIONS_HWIDS)
		{
			final int limit = DoubleSessionManager.getInstance().getHardWareLimit(getHWID());
			if (limit > 0 && limit > Config.DOUBLE_SESSIONS_CHECK_MAX_PLAYERS)
			{
				final var clients = AuthServerCommunication.getInstance().getAuthedClientsByHWID(getHWID());
				if (clients.size() > Config.DOUBLE_SESSIONS_CHECK_MAX_PLAYERS)
				{
					boolean found = false;
					for (final var client : clients)
					{
						final var pl = client.getActiveChar();
						if (pl != null && pl.isOnline() && pl.getWareDelay() > 0)
						{
							if (limit - Config.DOUBLE_SESSIONS_CHECK_MAX_PLAYERS <= 1)
							{
								found = true;
								break;
							}
						}
					}
					
					if (!found)
					{
						final var info = DoubleSessionManager.getInstance().getHardWareInfo(getHWID());
						if (info != null)
						{
							setWareDelay(info[1]);
						}
					}
				}
			}
		}
	}

	public void stopAllTimers()
	{
		stopHpMpRegeneration();
		stopAcpTask();
		getNevitSystem().saveTime();
		getPersonalTasks().cleanUp();
		getDonateRates().cleanUp();
		clearPetData();
		storePetFood(_mountNpcId);
	}

	@Override
	public Summon getSummon()
	{
		return _summon;
	}

	public Decoy getDecoy()
	{
		return _decoy;
	}

	public TrapInstance getTrap()
	{
		return _trap;
	}

	public void setPet(Summon summon)
	{
		_summon = summon;
	}

	public void setDecoy(Decoy decoy)
	{
		_decoy = decoy;
	}

	public void setTrap(TrapInstance trap)
	{
		_trap = trap;
	}

	public List<TamedBeastInstance> getTrainedBeasts()
	{
		return _tamedBeast;
	}

	public void addTrainedBeast(TamedBeastInstance tamedBeast)
	{
		if (_tamedBeast == null)
		{
			_tamedBeast = new CopyOnWriteArrayList<>();
		}
		_tamedBeast.add(tamedBeast);
	}

	public Request getRequest()
	{
		return _request;
	}

	public void setActiveRequester(Player requester)
	{
		_activeRequester = requester;
	}

	public Player getActiveRequester()
	{
		final Player requester = _activeRequester;
		if (requester != null)
		{
			if (requester.isRequestExpired() && (_activeTradeList == null))
			{
				_activeRequester = null;
			}
		}
		return _activeRequester;
	}

	public boolean isProcessingRequest()
	{
		return (getActiveRequester() != null) || (_requestExpireTime > GameTimeController.getInstance().getGameTicks());
	}

	public boolean isProcessingTransaction()
	{
		return (getActiveRequester() != null) || (_activeTradeList != null) || (_requestExpireTime > GameTimeController.getInstance().getGameTicks());
	}

	public void onTransactionRequest(Player partner)
	{
		_requestExpireTime = GameTimeController.getInstance().getGameTicks() + (REQUEST_TIMEOUT * GameTimeController.TICKS_PER_SECOND);
		partner.setActiveRequester(this);
	}

	public boolean isRequestExpired()
	{
		return !(_requestExpireTime > GameTimeController.getInstance().getGameTicks());
	}

	public void onTransactionResponse()
	{
		_requestExpireTime = 0;
	}

	public void setActiveWarehouse(ItemContainer warehouse)
	{
		_activeWarehouse = warehouse;
	}

	public ItemContainer getActiveWarehouse()
	{
		return _activeWarehouse;
	}

	public void setActiveTradeList(TradeList tradeList)
	{
		_activeTradeList = tradeList;
	}

	public TradeList getActiveTradeList()
	{
		return _activeTradeList;
	}

	public void onTradeStart(Player partner)
	{
		_activeTradeList = new TradeList(this);
		_activeTradeList.setPartner(partner);

		final SystemMessage msg = SystemMessage.getSystemMessage(SystemMessageId.BEGIN_TRADE_WITH_C1);
		msg.addPcName(partner);
		sendPacket(msg);
		sendPacket(new TradeStart(this));
	}

	public void onTradeConfirm(Player partner)
	{
		final SystemMessage msg = SystemMessage.getSystemMessage(SystemMessageId.C1_CONFIRMED_TRADE);
		msg.addPcName(partner);
		sendPacket(msg);
		partner.sendPacket(TradePressOwnOk.STATIC_PACKET);
		sendPacket(TradePressOtherOk.STATIC_PACKET);
	}

	public void onTradeCancel(Player partner)
	{
		if (_activeTradeList == null)
		{
			return;
		}

		_activeTradeList.lock();
		_activeTradeList = null;

		sendPacket(new TradeDone(0));
		final SystemMessage msg = SystemMessage.getSystemMessage(SystemMessageId.C1_CANCELED_TRADE);
		msg.addPcName(partner);
		sendPacket(msg);
	}

	public void onTradeFinish(boolean successfull)
	{
		_activeTradeList = null;
		sendPacket(new TradeDone(1));
		if (successfull)
		{
			sendPacket(SystemMessageId.TRADE_SUCCESSFUL);
		}
	}

	public void startTrade(Player partner)
	{
		onTradeStart(partner);
		partner.onTradeStart(this);
	}

	public void cancelActiveTrade()
	{
		if (_activeTradeList == null)
		{
			return;
		}

		final Player partner = _activeTradeList.getPartner();
		if (partner != null)
		{
			partner.onTradeCancel(this);
		}
		onTradeCancel(this);
	}

	public boolean hasManufactureShop()
	{
		return (_manufactureItems != null) && !_manufactureItems.isEmpty();
	}

	public Map<Integer, ManufactureItemTemplate> getManufactureItems()
	{
		if (_manufactureItems == null)
		{
			synchronized (this)
			{
				if (_manufactureItems == null)
				{
					_manufactureItems = Collections.synchronizedMap(new LinkedHashMap<>());
				}
			}
		}
		return _manufactureItems;
	}

	public String getStoreName()
	{
		return _storeName;
	}

	public void setStoreName(String name)
	{
		_storeName = name == null ? "" : name;
	}

	public TradeList getSellList()
	{
		if (_sellList == null)
		{
			_sellList = new TradeList(this);
		}
		return _sellList;
	}

	public TradeList getBuyList()
	{
		if (_buyList == null)
		{
			_buyList = new TradeList(this);
		}
		return _buyList;
	}

	public void setPrivateStoreType(int type)
	{
		_privatestore = type;

		if (_privatestore == STORE_PRIVATE_NONE)
		{
			for (final TradeItem item : getSellList().getItems())
			{
				AuctionsManager.getInstance().removeStore(this, item.getAuctionId());
			}
			unsetVar("storemode");
			setIsInStoreNow(false);
			if (getClient() != null)
			{
				resetHidePlayer();
			}
			
			if (Config.OFFLINE_DISCONNECT_FINISHED && ((getClient() == null) || getClient().isDetached()))
			{
				setOfflineMode(false);
				OfflineTaskManager.getInstance().removeOfflinePlayer(this);
				deleteMe();
			}
		}
		else
		{
			setVar("storemode", String.valueOf(type), -1);
			if (type == STORE_PRIVATE_PACKAGE_SELL)
			{
				setVar("isPacketSell", String.valueOf(1), -1);
			}
		}
	}

	public int getPrivateStoreType()
	{
		return _privatestore;
	}

	public void setClan(Clan clan)
	{
		_clan = clan;
		if (clan == null)
		{
			_clanId = 0;
			_clanPrivileges = 0;
			_pledgeType = 0;
			_powerGrade = 0;
			_lvlJoinedAcademy = 0;
			_apprentice = 0;
			_sponsor = 0;
			_activeWarehouse = null;
			return;
		}

		if (!clan.isMember(getObjectId()))
		{
			setClan(null);
			return;
		}
		
		broadcastUserInfo(true);
		_clanId = clan.getId();
	}

	public Clan getClan()
	{
		return _clan;
	}
	
	public boolean hasClan()
	{
		return _clan != null;
	}

	public boolean isClanLeader()
	{
		if (getClan() == null)
		{
			return false;
		}
		return getObjectId() == getClan().getLeaderId();
	}

	@Override
	protected void reduceArrowCount(boolean bolts)
	{
		final var inventory = getInventory();
		if (inventory != null)
		{
			inventory.reduceAmmunitionCount(bolts ? EtcItemType.BOLT : EtcItemType.ARROW);
		}
	}

	@Override
	protected boolean checkAndEquipArrows()
	{
		final var inventory = getInventory();
		if (inventory != null)
		{
			if (inventory.getPaperdollItem(Inventory.PAPERDOLL_LHAND) == null)
			{
				_arrowItem = inventory.findArrowForBow(getActiveWeaponItem());
				if (_arrowItem != null)
				{
					inventory.setPaperdollItem(Inventory.PAPERDOLL_LHAND, _arrowItem);
					if (!Config.FORCE_INVENTORY_UPDATE)
					{
						final InventoryUpdate iu = new InventoryUpdate();
						iu.addModifiedItem(_arrowItem);
						sendPacket(iu);
					}
					else
					{
						sendItemList(false);
					}
				}
			}
			else
			{
				_arrowItem = inventory.getPaperdollItem(Inventory.PAPERDOLL_LHAND);
			}
		}
		return _arrowItem != null;
	}

	@Override
	protected boolean checkAndEquipBolts()
	{
		final var inventory = getInventory();
		if (inventory != null)
		{
			if (inventory.getPaperdollItem(Inventory.PAPERDOLL_LHAND) == null)
			{
				_boltItem = inventory.findBoltForCrossBow(getActiveWeaponItem());
				if (_boltItem != null)
				{
					inventory.setPaperdollItem(Inventory.PAPERDOLL_LHAND, _boltItem);
					if (!Config.FORCE_INVENTORY_UPDATE)
					{
						final InventoryUpdate iu = new InventoryUpdate();
						iu.addModifiedItem(_boltItem);
						sendPacket(iu);
					}
					else
					{
						sendItemList(false);
					}
				}
			}
			else
			{
				_boltItem = inventory.getPaperdollItem(Inventory.PAPERDOLL_LHAND);
			}
		}
		return _boltItem != null;
	}

	public boolean disarmWeapons()
	{
		final ItemInstance wpn = getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
		if (wpn == null)
		{
			return true;
		}

		if (isCursedWeaponEquipped())
		{
			return false;
		}

		if (isCombatFlagEquipped())
		{
			return false;
		}

		if (wpn.getWeaponItem().isForceEquip())
		{
			return false;
		}
		
		if (wpn.isEventItem() && isInFightEvent())
		{
			return false;
		}

		final ItemInstance[] unequiped = getInventory().unEquipItemInBodySlotAndRecord(wpn.getItem().getBodyPart());
		final InventoryUpdate iu = new InventoryUpdate();
		for (final ItemInstance itm : unequiped)
		{
			iu.addModifiedItem(itm);
		}

		sendPacket(iu);
		abortAttack();
		broadcastUserInfo(true);

		if (unequiped.length > 0)
		{
			final SystemMessage sm;
			if (unequiped[0].getEnchantLevel() > 0)
			{
				sm = SystemMessage.getSystemMessage(SystemMessageId.EQUIPMENT_S1_S2_REMOVED);
				sm.addNumber(unequiped[0].getEnchantLevel());
				sm.addItemName(unequiped[0]);
			}
			else
			{
				sm = SystemMessage.getSystemMessage(SystemMessageId.S1_DISARMED);
				sm.addItemName(unequiped[0]);
			}
			sendPacket(sm);
		}
		
		if (isFakePlayer())
		{
			ThreadPoolManager.getInstance().schedule(() -> getInventory().equipItem(wpn), 2000);
		}
		return true;
	}

	public boolean disarmShield()
	{
		final ItemInstance sld = getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
		if (sld != null)
		{
			if (sld.isEventItem() && isInFightEvent())
			{
				return false;
			}
			final ItemInstance[] unequiped = getInventory().unEquipItemInBodySlotAndRecord(sld.getItem().getBodyPart());
			final InventoryUpdate iu = new InventoryUpdate();
			for (final ItemInstance itm : unequiped)
			{
				iu.addModifiedItem(itm);
			}
			sendPacket(iu);

			abortAttack();
			broadcastUserInfo(true);

			if (unequiped.length > 0)
			{
				SystemMessage sm = null;
				if (unequiped[0].getEnchantLevel() > 0)
				{
					sm = SystemMessage.getSystemMessage(SystemMessageId.EQUIPMENT_S1_S2_REMOVED);
					sm.addNumber(unequiped[0].getEnchantLevel());
					sm.addItemName(unequiped[0]);
				}
				else
				{
					sm = SystemMessage.getSystemMessage(SystemMessageId.S1_DISARMED);
					sm.addItemName(unequiped[0]);
				}
				sendPacket(sm);
			}
		}
		return true;
	}

	public boolean mount(Summon pet)
	{
		if (!disarmWeapons() || !disarmShield() || isTransformed())
		{
			return false;
		}

		if (!GeoEngine.getInstance().canSeeTarget(this, pet))
		{
			sendPacket(SystemMessageId.CANT_SEE_TARGET);
			return false;
		}

		stopAllToggles();
		setMount(pet.getId(), pet.getLevel());
		setMountObjectID(pet.getControlObjectId());
		clearPetData();
		startFeed(pet.getId());
		broadcastPacket(new Ride(this));
		broadcastUserInfo(true);

		pet.unSummon(this);
		broadcastTargetUpdate();
		return true;
	}
	
	public void broadcastTargetUpdate()
	{
		if (getTarget() != null && getTarget() == this)
		{
			sendPacket(new MyTargetSelected(this, this));
		}
		
		for (final Creature temp : getStatus().getStatusListener())
		{
			if (temp != null && temp.isPlayer())
			{
				temp.sendPacket(new MyTargetSelected(temp.getActingPlayer(), this));
			}
		}
	}

	public boolean mount(int npcId, int controlItemObjId, boolean useFood)
	{
		if (!disarmWeapons() || !disarmShield() || isTransformed())
		{
			return false;
		}

		stopAllToggles();
		setMount(npcId, getLevel());
		clearPetData();
		setMountObjectID(controlItemObjId);
		broadcastPacket(new Ride(this));
		broadcastUserInfo(true);
		if (useFood)
		{
			startFeed(npcId);
		}
		broadcastTargetUpdate();
		return true;
	}

	public boolean mountPlayer(Summon pet)
	{
		if ((pet != null) && pet.isMountable() && !isMounted() && !isBetrayed())
		{
			if (isDead())
			{
				sendActionFailed();
				sendPacket(SystemMessageId.STRIDER_CANT_BE_RIDDEN_WHILE_DEAD);
				return false;
			}
			else if (pet.isDead())
			{
				sendActionFailed();
				sendPacket(SystemMessageId.DEAD_STRIDER_CANT_BE_RIDDEN);
				return false;
			}
			else if (pet.isInCombat() || pet.isRooted())
			{
				sendActionFailed();
				sendPacket(SystemMessageId.STRIDER_IN_BATLLE_CANT_BE_RIDDEN);
				return false;

			}
			else if (isInCombat() || isInPartyTournament())
			{
				sendActionFailed();
				sendPacket(SystemMessageId.STRIDER_CANT_BE_RIDDEN_WHILE_IN_BATTLE);
				return false;
			}
			else if (isSitting())
			{
				sendActionFailed();
				sendPacket(SystemMessageId.STRIDER_CAN_BE_RIDDEN_ONLY_WHILE_STANDING);
				return false;
			}
			else if (isFishing())
			{
				sendActionFailed();
				sendPacket(SystemMessageId.CANNOT_DO_WHILE_FISHING_2);
				return false;
			}
			else if (isTransformed() || isCursedWeaponEquipped())
			{
				sendActionFailed();
				return false;
			}
			else if (getInventory().getItemByItemId(9819) != null)
			{
				sendActionFailed();
				sendMessage("You cannot mount a steed while holding a flag.");
				return false;
			}
			else if (pet.isHungry())
			{
				sendActionFailed();
				sendPacket(SystemMessageId.HUNGRY_STRIDER_NOT_MOUNT);
				return false;
			}
			else if (!Util.checkIfInRange(200, this, pet, true))
			{
				sendActionFailed();
				sendPacket(SystemMessageId.TOO_FAR_AWAY_FROM_FENRIR_TO_MOUNT);
				return false;
			}
			else if (!pet.isDead() && !isMounted())
			{
				mount(pet);
			}
		}
		else if (isRentedPet())
		{
			final var task = getPersonalTasks().getActiveTask(12);
			if (task != null)
			{
				task.getTask(this);
				getPersonalTasks().removeTask(12, true);
			}
		}
		else if (isMounted())
		{
			if ((getMountType() == MountType.WYVERN) && isInsideZone(ZoneId.NO_LANDING))
			{
				sendActionFailed();
				sendPacket(SystemMessageId.NO_DISMOUNT_HERE);
				return false;
			}
			else if (isHungry())
			{
				sendActionFailed();
				sendPacket(SystemMessageId.HUNGRY_STRIDER_NOT_MOUNT);
				return false;
			}
			else
			{
				dismount();
			}
		}
		return true;
	}

	public boolean dismount()
	{
		final boolean wasFlying = isFlying();

		sendPacket(new SetupGauge(this, 3, 0, 0));
		final int petId = _mountNpcId;
		setMount(0, 0);
		getPersonalTasks().removeTask(8, true);
		clearPetData();
		if (wasFlying)
		{
			removeSkill(SkillsParser.FrequentSkill.WYVERN_BREATH.getSkill());
		}
		broadcastPacket(new Ride(this));
		setMountObjectID(0);
		storePetFood(petId);
		broadcastUserInfo(true);
		broadcastTargetUpdate();
		return true;
	}

	public void setUptime(long time)
	{
		_uptime = time;
	}

	public long getUptime()
	{
		return System.currentTimeMillis() - _uptime;
	}

	@Override
	public boolean isInvul()
	{
		return super.isInvul() || (_teleportProtectEndTime > GameTimeController.getInstance().getGameTicks());
	}

	@Override
	public boolean isBlocked()
	{
		return super.isBlocked() || inObserverMode() || isTeleporting();
	}

	@Override
	public boolean isInParty()
	{
		return _party != null;
	}

	public void setParty(Party party)
	{
		_party = party;
	}

	public void joinParty(Party party)
	{
		if (party != null)
		{
			_party = party;
			party.addPartyMember(this);
		}
	}

	public void leaveParty()
	{
		if (isInParty())
		{
			_party.removePartyMember(this, messageType.Disconnected);
			_party = null;
		}
	}

	@Override
	public Party getParty()
	{
		return _party;
	}

	@Override
	public boolean isGM()
	{
		return getAccessLevel().isGm();
	}
	
	public boolean isGameMaster()
	{
		return isGM() || isModerator();
	}
	
	public boolean isModerator()
	{
		return getAccessLevel().allowPunishment() || getAccessLevel().allowPunishmentChat();
	}

	public void setAccessLevel(int level)
	{
		_accessLevel = AdminParser.getInstance().getAccessLevel(level);

		getAppearance().setNameColor(_accessLevel.getNameColor());
		getAppearance().setTitleColor(_accessLevel.getTitleColor());
		broadcastUserInfo(true);

		CharNameHolder.getInstance().addName(this);

		if (!AdminParser.getInstance().hasAccessLevel(level))
		{
			_log.warn("Tryed to set unregistered access level " + level + " for " + toString() + ". Setting access level without privileges!");
		}
	}

	@Override
	public AccessLevel getAccessLevel()
	{
		if (Config.EVERYBODY_HAS_ADMIN_RIGHTS)
		{
			return AdminParser.getInstance().getMasterAccessLevel();
		}
		else if (_accessLevel == null)
		{
			setAccessLevel(Config.DEFAULT_ACCSESS_LEVEL);
		}
		return _accessLevel;
	}
	
	private class UpdateAndBroadcastStatusTask implements Runnable
	{
		private final int _broadcastType;
		
		public UpdateAndBroadcastStatusTask(int broadcastType)
		{
			_broadcastType = broadcastType;
		}
		
		@Override
		public void run()
		{
			broadcastStatusImpl(_broadcastType);
			_updateAndBroadcastStatusTask = null;
		}
	}

	public void updateAndBroadcastStatus(int broadcastType)
	{
		if (_entering)
		{
			return;
		}

		final var oldTask = _updateAndBroadcastStatusTask;
		if (oldTask != null)
		{
			_updateAndBroadcastStatusTask = null;
			oldTask.cancel(false);
		}
		_updateAndBroadcastStatusTask = ThreadPoolManager.getInstance().schedule(new UpdateAndBroadcastStatusTask(broadcastType), Config.USER_STATS_UPDATE_INTERVAL);
	}
	
	public void broadcastStatusImpl(int broadcastType)
	{
		refreshOverloaded();
		refreshExpertisePenalty();

		if (broadcastType == 1)
		{
			sendUserInfo(true);
		}
		
		if (broadcastType == 2)
		{
			broadcastUserInfo(true);
		}
	}
	
	public void setKarmaFlag(int flag)
	{
		sendUserInfo(true);
		broadcastRelationChanged();
	}
	
	public void broadcastKarma(boolean flagChanged)
	{
		sendStatusUpdate(true, true, StatusUpdate.KARMA);
		if (flagChanged)
		{
			broadcastRelationChanged();
		}
	}
	
	public void setOnlineStatus(boolean isOnline, boolean updateInDb)
	{
		if (_isOnline != isOnline)
		{
			_isOnline = isOnline;
		}
		
		if (updateInDb)
		{
			updateOnlineStatus();
		}
	}

	public void setIsIn7sDungeon(boolean isIn7sDungeon)
	{
		_isIn7sDungeon = isIn7sDungeon;
	}
	
	public void updateOnlineStatus()
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE characters SET online=?, lastAccess=? WHERE charId=?");
			statement.setInt(1, isOnlineInt());
			statement.setLong(2, System.currentTimeMillis());
			statement.setInt(3, getObjectId());
			statement.execute();
		}
		catch (final Exception e)
		{
			_log.warn("Failed updating character online status.", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
	
	public void updateAcessStatus(boolean isOnline)
	{
		if (_isOnline != isOnline)
		{
			_isOnline = isOnline;
		}
		
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE characters SET lastAccess=? WHERE charId=?");
			statement.setLong(1, System.currentTimeMillis());
			statement.setInt(2, getObjectId());
			statement.execute();
		}
		catch (final Exception e)
		{
			_log.warn("Failed updating character lastAccess status.", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	public void restoreCharData()
	{
		CharacterSkillsDAO.getInstance().restoreSkills(this);
		_macros.restoreMe();
		_shortCuts.restoreMe();
		restoreHenna();
		CharacterBookMarkDAO.getInstance().restore(this);
		restoreRecipeBook(true);

		if (Config.STORE_RECIPE_SHOPLIST)
		{
			restoreRecipeShopList();
		}

		loadPremiumItemList(false);
		restorePetInventoryItems();
	}

	private void restoreRecipeBook(boolean loadCommon)
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(loadCommon ? "SELECT id, type, classIndex FROM character_recipebook WHERE charId=?" : "SELECT id FROM character_recipebook WHERE charId=? AND classIndex=? AND type = 1");
			statement.setInt(1, getObjectId());
			if (!loadCommon)
			{
				statement.setInt(2, _classIndex);
			}

			_dwarvenRecipeBook.clear();
			
			RecipeList recipe;
			final RecipeParser rd = RecipeParser.getInstance();
			rset = statement.executeQuery();
			while (rset.next())
			{
				recipe = rd.getRecipeList(rset.getInt("id"));
				if (loadCommon)
				{
					if (rset.getInt(2) == 1)
					{
						if (rset.getInt(3) == _classIndex)
						{
							registerDwarvenRecipeList(recipe, false);
						}
					}
					else
					{
						registerCommonRecipeList(recipe, false);
					}
				}
				else
				{
					registerDwarvenRecipeList(recipe, false);
				}
			}
		}
		catch (final Exception e)
		{
			_log.warn("Could not restore recipe book data:" + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
	}

	public Map<Integer, PremiumItemTemplate> getPremiumItemList()
	{
		return _premiumItems;
	}

	public void loadPremiumItemList(boolean sendPacket)
	{
		boolean newItem = false;
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT itemNum, itemId, itemCount, itemSender FROM character_premium_items WHERE charId=? AND status=0");
			statement.setInt(1, getObjectId());
			rset = statement.executeQuery();
			while (rset.next())
			{
				final int itemNum = rset.getInt("itemNum");
				final int itemId = rset.getInt("itemId");
				final long itemCount = rset.getLong("itemCount");
				final String itemSender = rset.getString("itemSender");
				if (!_premiumItems.containsKey(itemNum))
				{
					_premiumItems.put(itemNum, new PremiumItemTemplate(itemId, itemCount, itemSender));
					newItem = true;
				}
			}
		}
		catch (final Exception e)
		{
			_log.warn("Could not restore premium items: " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
		
		if (!_premiumItems.isEmpty() && newItem && sendPacket)
		{
			sendPacket(ExNotifyPremiumItem.STATIC_PACKET);
		}
	}

	public void updatePremiumItem(int itemNum, long newcount)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE character_premium_items SET itemCount=?, status=?, time=?, recipient=? WHERE charId=? AND itemNum=?");
			statement.setLong(1, newcount);
			statement.setInt(2, newcount > 0 ? 0 : 1);
			statement.setString(3, String.valueOf(new SimpleDateFormat("dd-MMM-yyyy HH:mm").format(new Date(System.currentTimeMillis()))));
			statement.setString(4, Config.DOUBLE_SESSIONS_HWIDS ? getHWID() : getName(null));
			statement.setInt(5, getObjectId());
			statement.setInt(6, itemNum);
			statement.execute();
		}
		catch (final Exception e)
		{
			_log.warn("Could not update premium items: " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	public synchronized void store(boolean storeActiveEffects)
	{
		CharacterDAO.getInstance().storePlayer(this);
		CharacterDAO.getInstance().storeSubClasses(this);
		storeEffect(storeActiveEffects);
		CharacterItemReuseDAO.getInstance().store(this);
		
		if (Config.STORE_RECIPE_SHOPLIST)
		{
			storeRecipeShopList();
		}
		SevenSigns.getInstance().saveSevenSignsData(getObjectId());
	}

	@Override
	public void store()
	{
		store(true);
	}

	@Override
	public void storeEffect(boolean storeEffects)
	{
		if (!Config.STORE_SKILL_COOLTIME)
		{
			return;
		}
		CharacterSkillSaveDAO.getInstance().store(this, Config.SUBCLASS_STORE_SKILL, storeEffects);
	}

	public boolean isOnline()
	{
		return _isOnline;
	}

	public int isOnlineInt()
	{
		if (_isOnline && (getClient() != null))
		{
			return getClient().isDetached() ? 2 : 1;
		}
		return 0;
	}

	public boolean isIn7sDungeon()
	{
		return _isIn7sDungeon;
	}

	@Override
	public Skill addSkill(Skill newSkill)
	{
		addCustomSkill(newSkill);
		return super.addSkill(newSkill);
	}

	public Skill addSkill(Skill newSkill, boolean store)
	{
		final Skill oldSkill = addSkill(newSkill);
		if (store)
		{
			CharacterSkillsDAO.getInstance().store(this, newSkill, oldSkill, -1);
		}
		return oldSkill;
	}

	@Override
	public Skill removeSkill(Skill skill, boolean store)
	{
		removeCustomSkill(skill);
		return store ? removeSkill(skill) : super.removeSkill(skill, true);
	}

	public Skill removeSkill(Skill skill, boolean store, boolean cancelEffect)
	{
		removeCustomSkill(skill);
		return store ? removeSkill(skill) : super.removeSkill(skill, cancelEffect);
	}

	public Skill removeSkill(Skill skill)
	{
		removeCustomSkill(skill);
		final Skill oldSkill = super.removeSkill(skill, true);
		if (oldSkill != null)
		{
			CharacterSkillsDAO.getInstance().remove(this, oldSkill.getId());
		}

		if ((getTransformationId() > 0) || isCursedWeaponEquipped())
		{
			return oldSkill;
		}

		final ShortCutTemplate[] allShortCuts = getAllShortCuts();
		for (final ShortCutTemplate sc : allShortCuts)
		{
			if ((sc != null) && (skill != null) && (sc.getId() == skill.getId()) && (sc.getType() == ShortcutType.SKILL) && !((skill.getId() >= 3080) && (skill.getId() <= 3259)))
			{
				deleteShortCut(sc.getSlot(), sc.getPage());
			}
		}
		return oldSkill;
	}

	@Override
	public void restoreEffects()
	{
		CharacterSkillSaveDAO.getInstance().restore(this);
	}

	private void restoreHenna()
	{
		CharacterHennaDAO.getInstance().restore(this);
		recalcHennaStats();
	}

	public int getHennaEmptySlots()
	{
		int totalSlots = 0;
		if (getClassId().level() == 1)
		{
			totalSlots = 2;
		}
		else
		{
			totalSlots = 3;
		}

		for (int i = 0; i < 3; i++)
		{
			if (_henna[i] != null)
			{
				totalSlots--;
			}
		}

		if (totalSlots <= 0)
		{
			return 0;
		}

		return totalSlots;
	}

	public boolean removeHenna(int slot)
	{
		if ((slot < 1) || (slot > 3))
		{
			return false;
		}

		slot--;

		final Henna henna = _henna[slot];
		if (henna == null)
		{
			return false;
		}

		_henna[slot] = null;
		CharacterHennaDAO.getInstance().delete(this, slot + 1);
		if (!henna.getSkillList().isEmpty())
		{
			for (final Skill sk : henna.getSkillList())
			{
				if ((sk != null) && (getKnownSkill(sk.getId()) != null))
				{
					removeSkill(sk, false, true);
				}
			}
			sendSkillList(false);
		}
		recalcHennaStats();
		sendPacket(new HennaInfo(this));
		sendUserInfo(true);
		getInventory().addItem("Henna", henna.getDyeItemId(), henna.getCancelCount(), this, null);
		reduceAdena("Henna", henna.getCancelFee(), this, false);

		final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.EARNED_S2_S1_S);
		sm.addItemName(henna.getDyeItemId());
		sm.addItemNumber(henna.getCancelCount());
		sendPacket(sm);
		sendPacket(SystemMessageId.SYMBOL_DELETED);
		return true;
	}

	public boolean addHenna(Henna henna)
	{
		for (int i = 0; i < 3; i++)
		{
			if (_henna[i] == null)
			{
				_henna[i] = henna;
				recalcHennaStats();
				CharacterHennaDAO.getInstance().add(this, henna.getDyeId(), i + 1);
				sendPacket(new HennaInfo(this));
				sendUserInfo(true);
				return true;
			}
		}
		return false;
	}

	private void recalcHennaStats()
	{
		_hennaINT = 0;
		_hennaSTR = 0;
		_hennaCON = 0;
		_hennaMEN = 0;
		_hennaWIT = 0;
		_hennaDEX = 0;

		for (final Henna h : _henna)
		{
			if (h == null)
			{
				continue;
			}
			
			if (!h.getSkillList().isEmpty())
			{
				for (final Skill sk : h.getSkillList())
				{
					if ((sk != null) && (getKnownSkill(sk.getId()) == null))
					{
						addSkill(sk, false);
					}
				}
			}

			_hennaINT += ((_hennaINT + h.getStatINT()) > 5) ? 5 - _hennaINT : h.getStatINT();
			_hennaSTR += ((_hennaSTR + h.getStatSTR()) > 5) ? 5 - _hennaSTR : h.getStatSTR();
			_hennaMEN += ((_hennaMEN + h.getStatMEN()) > 5) ? 5 - _hennaMEN : h.getStatMEN();
			_hennaCON += ((_hennaCON + h.getStatCON()) > 5) ? 5 - _hennaCON : h.getStatCON();
			_hennaWIT += ((_hennaWIT + h.getStatWIT()) > 5) ? 5 - _hennaWIT : h.getStatWIT();
			_hennaDEX += ((_hennaDEX + h.getStatDEX()) > 5) ? 5 - _hennaDEX : h.getStatDEX();
		}
	}

	public Henna getHenna(int slot)
	{
		if ((slot < 1) || (slot > 3))
		{
			return null;
		}
		return _henna[slot - 1];
	}
	
	public void setHenna(Henna[] henna)
	{
		_henna = henna;
	}
	
	public boolean hasHennas()
	{
		for (final Henna henna : _henna)
		{
			if (henna != null)
			{
				return true;
			}
		}
		return false;
	}

	public Henna[] getHennaList()
	{
		return _henna;
	}

	public int getHennaStatINT()
	{
		return _hennaINT;
	}

	public int getHennaStatSTR()
	{
		return _hennaSTR;
	}

	public int getHennaStatCON()
	{
		return _hennaCON;
	}

	public int getHennaStatMEN()
	{
		return _hennaMEN;
	}

	public int getHennaStatWIT()
	{
		return _hennaWIT;
	}

	public int getHennaStatDEX()
	{
		return _hennaDEX;
	}

	@Override
	public boolean isAutoAttackable(Creature attacker, boolean isPoleAttack)
	{
		if (attacker == null)
		{
			return false;
		}

		if ((attacker == this) || (attacker == getSummon()))
		{
			return false;
		}

		if (attacker instanceof Player)
		{
			if (isCombatFlagEquipped() && (((Player) attacker).getSiegeSide() != 0) || attacker.getActingPlayer().isCursedWeaponEquipped())
			{
				return true;
			}
		}
		
		if (attacker instanceof FriendlyMobInstance)
		{
			return false;
		}

		if (attacker.isMonster())
		{
			return true;
		}

		if (attacker.isPlayer() && (getDuelState() == Duel.DUELSTATE_DUELLING) && (getDuelId() == ((Player) attacker).getDuelId()))
		{
			return true;
		}

		if (isInParty() && getParty().getMembers().contains(attacker))
		{
			return false;
		}

		if (attacker.isPlayer() && attacker.getActingPlayer().isInOlympiadMode())
		{
			if (isInOlympiadMode() && isOlympiadStart() && (((Player) attacker).getOlympiadGameId() == getOlympiadGameId()))
			{
				return true;
			}
			return false;
		}

		if (isInFightEvent())
		{
			if (attacker.isPlayable() && !attacker.isInFightEvent())
			{
				return false;
			}
			return true;
		}

		if (isInPartyTournament())
		{
			if (attacker.isPlayable() && !attacker.isInPartyTournament())
			{
				return false;
			}
			return true;
		}

		if (attacker.isPlayable())
		{
			if (isInsideZone(ZoneId.PEACE))
			{
				return false;
			}

			final Player attackerPlayer = attacker.getActingPlayer();
			if (attackerPlayer != null && !isInFightEvent())
			{
				if (getClan() != null)
				{
					final Siege siege = SiegeManager.getInstance().getSiege(getX(), getY(), getZ());
					if (siege != null)
					{
						final var siegeZone = ZoneManager.getInstance().getZone(this, SiegeZone.class);
						if (siegeZone != null && siege.checkIsDefender(attackerPlayer.getClan()) && siege.checkIsDefender(getClan()) && !siegeZone.isAttackSameSiegeSide())
						{
							return false;
						}
						
						if (siegeZone != null && siege.checkIsAttacker(attackerPlayer.getClan()) && siege.checkIsAttacker(getClan()) && !siegeZone.isAttackSameSiegeSide())
						{
							return false;
						}
					}
					
					if ((getClan() != null) && (attackerPlayer.getClan() != null) && getClan().isAtWarWith(attackerPlayer.getClanId()) && attackerPlayer.getClan().isAtWarWith(getClanId()) && (getWantsPeace() == 0) && (attackerPlayer.getWantsPeace() == 0) && !isAcademyMember())
					{
						return true;
					}
				}
				
				if ((isInsideZone(ZoneId.PVP) && attackerPlayer.isInsideZone(ZoneId.PVP)) && !(isInsideZone(ZoneId.SIEGE) && attackerPlayer.isInsideZone(ZoneId.SIEGE)))
				{
					if (isInsideZone(ZoneId.FUN_PVP))
					{
						final FunPvpZone zone = ZoneManager.getInstance().getZone(this, FunPvpZone.class);
						if (zone != null)
						{
							if (!zone.isPvpZone() || (zone.isPvpZone() && isFriend(attackerPlayer)))
							{
								return false;
							}
						}
					}
					return true;
				}
				
				if (isFriend(attackerPlayer) && attackerPlayer.isFriend(this))
				{
					return false;
				}
				
				if ((isInsideZone(ZoneId.PVP) && attackerPlayer.isInsideZone(ZoneId.PVP)) && (isInsideZone(ZoneId.SIEGE) && attackerPlayer.isInsideZone(ZoneId.SIEGE)))
				{
					return true;
				}
			}
		}
		else if (attacker instanceof DefenderInstance)
		{
			if (getClan() != null)
			{
				final Siege siege = SiegeManager.getInstance().getSiege(this);
				return ((siege != null) && siege.checkIsAttacker(getClan()));
			}
		}

		if ((getKarma() > 0) || (getPvpFlag() > 0 && !isPoleAttack) || (isPoleAttack && getPvpFlag() > 0 && attacker.isPlayer() && attacker.getActingPlayer().getPvpFlag() > 0))
		{
			return true;
		}
		return false;
	}

	@Override
	public boolean useMagic(Skill skill, boolean forceUse, boolean dontMove, boolean msg)
	{
		if (skill.isPassive())
		{
			sendActionFailed();
			return false;
		}

		if (skill.isToggle())
		{
			if (isSitting())
			{
				if (msg)
				{
					sendPacket(SystemMessageId.CANT_MOVE_SITTING);
				}
				sendActionFailed();
				return false;
			}
			
			if (isFakeDeathNow() && skill.getId() != 60)
			{
				stopFakeDeath(true);
			}
			
			if (getFirstEffect(skill.getId()) != null)
			{
				stopSkillEffects(skill.getId());
				return false;
			}
			
			if (!skill.checkCondition(this, this, false, msg) || isAllSkillsDisabled())
			{
				sendActionFailed();
				return false;
			}
			
			doSimultaneousCast(skill);
			return false;
		}
		
		GameObject target = null;
		switch (skill.getTargetType())
		{
			case AURA :
			case FRONT_AURA :
			case BEHIND_AURA :
			case GROUND :
			case SELF :
			case AURA_CORPSE_MOB :
			case AURA_FRIENDLY :
			case AURA_DOOR :
			case AURA_FRIENDLY_SUMMON :
			case AURA_UNDEAD_ENEMY :
			case COMMAND_CHANNEL :
			case AURA_MOB :
			case AURA_DEAD_MOB :
				target = this;
				break;
			default :
				target = skill.getFirstOfTargetList(this);
				break;
		}

		if (target != null && target != this && target.isPlayer() && skill.isOffensive() && isPKProtected(target.getActingPlayer()))
		{
			setIsCastingNow(false);
			sendMessage("You cannot attack this player!");
			sendActionFailed();
			return false;
		}
		
		if (isCastingNow())
		{
			final var currentSkill = getCurrentSkill();
			if ((currentSkill != null) && (skill.getId() == currentSkill.getId()))
			{
				sendActionFailed();
				return false;
			}
			else if (isSkillDisabled(skill) || isSkillBlocked(skill))
			{
				sendActionFailed();
				return false;
			}
			setQueuedSkill(skill, forceUse, dontMove);
			sendActionFailed();
			return false;
		}
		
		setIsCastingNow(true);
		setCurrentSkill(skill, forceUse, dontMove);
		
		if (getQueuedSkill() != null)
		{
			setQueuedSkill(null, false, false);
		}
		
		if (!checkUseMagicConditions(skill, forceUse, dontMove, msg))
		{
			setIsCastingNow(false);
			final GameObject attackTarget = getTarget();
			if (skill.getFlyRadius() != 0 && attackTarget != null && attackTarget != this && attackTarget.isAutoAttackable(this, false))
			{
				getAI().setIntention(CtrlIntention.ATTACK, attackTarget);
			}
			return false;
		}
		
		if (target == null)
		{
			setIsCastingNow(false);
			sendActionFailed();
			return false;
		}
		getAI().setIntention(CtrlIntention.CAST, skill, target);
		return true;
	}

	private boolean checkUseMagicConditions(Skill skill, boolean forceUse, boolean dontMove, boolean msg)
	{
		final SkillType sklType = skill.getSkillType();

		if (isOutOfControl() || isParalyzed() || isStunned() || isSleeping() || isSkillBlocked(skill))
		{
			sendActionFailed();
			return false;
		}

		if (isDead())
		{
			sendActionFailed();
			return false;
		}

		if (isFishing() && ((sklType != SkillType.PUMPING) && (sklType != SkillType.REELING) && (sklType != SkillType.FISHING)))
		{
			if (msg)
			{
				sendPacket(SystemMessageId.ONLY_FISHING_SKILLS_NOW);
			}
			return false;
		}

		if (inObserverMode())
		{
			if (msg)
			{
				sendPacket(SystemMessageId.OBSERVERS_CANNOT_PARTICIPATE);
			}
			abortCast();
			sendActionFailed();
			return false;
		}

		if (isSitting())
		{
			if (msg)
			{
				sendPacket(SystemMessageId.CANT_MOVE_SITTING);
			}
			sendActionFailed();
			return false;
		}

		if (skill.isToggle())
		{
			final Effect effect = getFirstEffect(skill.getId());
			if (effect != null)
			{
				effect.exit();
				sendActionFailed();
				return false;
			}
		}

		if (isFakeDeathNow())
		{
			sendActionFailed();
			return false;
		}
		GameObject target = null;
		final TargetType sklTargetType = skill.getTargetType();
		final Location worldPosition = getCurrentSkillWorldPosition();

		if ((sklTargetType == TargetType.GROUND) && (worldPosition == null))
		{
			Util.handleIllegalPlayerAction(this, "" + getName(null) + " try use skill: " + skill.getName(null) + " with null worldPosition!");
			sendActionFailed();
			return false;
		}

		switch (sklTargetType)
		{
			case AURA :
			case FRONT_AURA :
			case BEHIND_AURA :
			case PARTY :
			case CLAN :
			case PARTY_CLAN :
			case PARTY_NOTME :
			case CORPSE_CLAN :
			case CORPSE_FRIENDLY :
			case GROUND :
			case SELF :
			case AREA_SUMMON :
			case AURA_CORPSE_MOB :
			case AURA_DOOR :
			case AURA_FRIENDLY :
			case AURA_FRIENDLY_SUMMON :
			case AURA_UNDEAD_ENEMY :
			case AURA_MOB :
			case AURA_DEAD_MOB :
			case COMMAND_CHANNEL :
				target = this;
				break;
			case PET :
			case SERVITOR :
			case SUMMON :
				target = getSummon();
				break;
			default :
				target = getTarget();
				break;
		}

		if (target == null)
		{
			sendActionFailed();
			return false;
		}

		if (target instanceof FortBallistaInstance)
		{
			if (skill.getFlyType() != FlyType.NONE)
			{
				if (msg)
				{
					sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
				}
				return false;
			}
		}

		if (target.isDoor())
		{
			if (skill.getFlyType() != FlyType.NONE)
			{
				if (msg)
				{
					sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
				}
				return false;
			}
			
			final int activeSiegeId = (((DoorInstance) target).getFort() != null ? ((DoorInstance) target).getFort().getId() : (((DoorInstance) target).getCastle() != null ? ((DoorInstance) target).getCastle().getId() : 0));
			
			if (TerritoryWarManager.getInstance().isTWInProgress())
			{
				if (TerritoryWarManager.getInstance().isAllyField(this, activeSiegeId))
				{
					if (msg)
					{
						sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
					}
					return false;
				}
			}
			else if ((((DoorInstance) target).getCastle() != null) && (((DoorInstance) target).getCastle().getId() > 0))
			{
				if (!((DoorInstance) target).getCastle().getSiege().getIsInProgress())
				{
					if (msg)
					{
						sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
					}
					return false;
				}
			}
			else if ((((DoorInstance) target).getFort() != null) && (((DoorInstance) target).getFort().getId() > 0) && !((DoorInstance) target).getIsShowHp())
			{
				if (!((DoorInstance) target).getFort().getSiege().getIsInProgress())
				{
					if (msg)
					{
						sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
					}
					return false;
				}
			}
			
			if (!((DoorInstance) target).isAutoAttackable(this, false))
			{
				if (sklType != SkillType.UNLOCK && sklType != SkillType.UNLOCK_SPECIAL)
				{
					return false;
				}
			}
		}
		
		if (target instanceof ControlTowerInstance)
		{
			if (!target.isAutoAttackable(this, false))
			{
				return false;
			}
		}

		if (isInDuel())
		{
			if (target instanceof Playable)
			{
				final Player cha = target.getActingPlayer();
				if (cha.getDuelId() != getDuelId())
				{
					if (msg)
					{
						sendMessage("You cannot do this while duelling.");
					}
					sendActionFailed();
					return false;
				}
			}
		}
		
		if (isSkillBlocked(skill))
		{
			sendActionFailed();
			return false;
		}

		if (isSkillDisabled(skill))
		{
			SystemMessage sm = null;
			if (_reuseTimeStampsSkills.containsKey(skill.getReuseHashCode()))
			{
				final int remainingTime = (int) (_reuseTimeStampsSkills.get(skill.getReuseHashCode()).getRemaining() / 1000);
				final int hours = remainingTime / 3600;
				final int minutes = (remainingTime % 3600) / 60;
				final int seconds = (remainingTime % 60);
				if (hours > 0)
				{
					sm = SystemMessage.getSystemMessage(SystemMessageId.S2_HOURS_S3_MINUTES_S4_SECONDS_REMAINING_FOR_REUSE_S1);
					sm.addSkillName(skill);
					sm.addNumber(hours);
					sm.addNumber(minutes);
				}
				else if (minutes > 0)
				{
					sm = SystemMessage.getSystemMessage(SystemMessageId.S2_MINUTES_S3_SECONDS_REMAINING_FOR_REUSE_S1);
					sm.addSkillName(skill);
					sm.addNumber(minutes);
				}
				else
				{
					sm = SystemMessage.getSystemMessage(SystemMessageId.S2_SECONDS_REMAINING_FOR_REUSE_S1);
					sm.addSkillName(skill);
				}

				sm.addNumber(seconds);
			}
			else
			{
				sm = SystemMessage.getSystemMessage(SystemMessageId.S1_PREPARED_FOR_REUSE);
				sm.addSkillName(skill);
			}
			if (msg)
			{
				sendPacket(sm);
			}
			return false;
		}

		if (!skill.checkCondition(this, target, false, msg))
		{
			sendActionFailed();
			return false;
		}

		if (skill.isOffensive())
		{
			if ((isInsidePeaceZone(this, target)) && !getAccessLevel().allowPeaceAttack())
			{
				if (msg)
				{
					sendPacket(SystemMessageId.TARGET_IN_PEACEZONE);
				}
				sendActionFailed();
				return false;
			}

			if (isInOlympiadMode() && !isOlympiadStart())
			{
				sendActionFailed();
				return false;
			}

			final var siegeZone = ZoneManager.getInstance().getZone(this, SiegeZone.class);
			if (!isInFightEvent())
			{
				if ((target.getActingPlayer() != null) && (getSiegeState() > 0) && siegeZone != null && (target.getActingPlayer().getSiegeState() == getSiegeState()) && (target.getActingPlayer() != this) && (target.getActingPlayer().getSiegeSide() == getSiegeSide()))
				{
					if (siegeZone.isAttackSameSiegeSide())
					{
						final Clan clan1 = target.getActingPlayer().getClan();
						final Clan clan2 = getClan();
						if (clan1 != null && clan2 != null)
						{
							if ((clan1.getAllyId() != 0 && clan2.getAllyId() != 0 && clan1.getAllyId() == clan2.getAllyId()) || clan1.getId() == clan2.getId())
							{
								if (msg)
								{
									if (TerritoryWarManager.getInstance().isTWInProgress())
									{
										sendPacket(SystemMessageId.YOU_CANNOT_ATTACK_A_MEMBER_OF_THE_SAME_TERRITORY);
									}
									else
									{
										sendPacket(SystemMessageId.FORCED_ATTACK_IS_IMPOSSIBLE_AGAINST_SIEGE_SIDE_TEMPORARY_ALLIED_MEMBERS);
									}
								}
								sendActionFailed();
								return false;
							}
						}
					}
					else
					{
						if (msg)
						{
							if (TerritoryWarManager.getInstance().isTWInProgress())
							{
								sendPacket(SystemMessageId.YOU_CANNOT_ATTACK_A_MEMBER_OF_THE_SAME_TERRITORY);
							}
							else
							{
								sendPacket(SystemMessageId.FORCED_ATTACK_IS_IMPOSSIBLE_AGAINST_SIEGE_SIDE_TEMPORARY_ALLIED_MEMBERS);
							}
						}
						sendActionFailed();
						return false;
					}
				}
			}

			switch (skill.getSkillType())
			{
				case UNLOCK :
				case UNLOCK_SPECIAL :
				case DELUXE_KEY_UNLOCK :
				{
					if (isMuted())
					{
						sendActionFailed();
						return false;
					}
					break;
				}
				default :
				{
					if (!target.isDoor() && !target.canBeAttacked() && !getAccessLevel().allowPeaceAttack())
					{
						sendActionFailed();
						return false;
					}
				}
			}

			if ((target instanceof EventMonsterInstance) && ((EventMonsterInstance) target).eventSkillAttackBlocked())
			{
				sendActionFailed();
				return false;
			}

			if (!target.isAutoAttackable(this, false) && !forceUse)
			{
				switch (sklTargetType)
				{
					case AURA :
					case FRONT_AURA :
					case BEHIND_AURA :
					case AURA_CORPSE_MOB :
					case AURA_DOOR :
					case AURA_FRIENDLY :
					case AURA_FRIENDLY_SUMMON :
					case CLAN :
					case PARTY :
					case SELF :
					case GROUND :
					case AREA_SUMMON :
					case UNLOCKABLE :
					case AURA_UNDEAD_ENEMY :
					case AURA_MOB :
					case AURA_DEAD_MOB :
						break;
					case CORPSE_MOB :
						if (target.isServitor() && ((Summon) target).getSummon() == getSummon())
						{
							break;
						}
					default :
						sendActionFailed();
						return false;
				}
			}

			if (dontMove)
			{
				if (sklTargetType == TargetType.GROUND)
				{
					if (!isInsideRadius(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), (int) (skill.getCastRange() + getColRadius()), false, false))
					{
						if (msg)
						{
							sendPacket(SystemMessageId.TARGET_TOO_FAR);
						}
						sendActionFailed();
						return false;
					}
				}
				else if ((skill.getCastRange() > 0) && !isInsideRadius(target, (int) (skill.getCastRange() + getColRadius()), false, false))
				{
					if (msg)
					{
						sendPacket(SystemMessageId.TARGET_TOO_FAR);
					}
					sendActionFailed();
					return false;
				}
			}
		}

		if (skill.hasEffectType(EffectType.TELEPORT_TO_TARGET))
		{
			if (isMovementDisabled())
			{
				if (msg)
				{
					final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
					sm.addSkillName(skill.getId());
					sendPacket(sm);
				}
				sendActionFailed();
				return false;
			}

			if (isInsideZone(ZoneId.PEACE) && !isInFightEvent())
			{
				if (msg)
				{
					sendPacket(SystemMessageId.TARGET_IN_PEACEZONE);
				}
				sendActionFailed();
				return false;
			}

		}

		if (!skill.isOffensive() && target.isMonster() && !forceUse && !skill.isNeutral())
		{
			switch (sklTargetType)
			{
				case PET :
				case SERVITOR :
				case SUMMON :
				case AURA :
				case FRONT_AURA :
				case BEHIND_AURA :
				case AURA_CORPSE_MOB :
				case AURA_DOOR :
				case AURA_FRIENDLY :
				case AURA_FRIENDLY_SUMMON :
				case CLAN :
				case PARTY_CLAN :
				case PARTY_NOTME :
				case CORPSE_CLAN :
				case CORPSE_FRIENDLY :
				case SELF :
				case PARTY :
				case CORPSE_MOB :
				case AREA_CORPSE_MOB :
				case AURA_UNDEAD_ENEMY :
				case AURA_MOB :
				case AURA_DEAD_MOB :
				case GROUND :
					break;
				default :
				{
					switch (sklType)
					{
						case DELUXE_KEY_UNLOCK :
						case UNLOCK :
							break;
						default :
							sendActionFailed();
							return false;
					}
					break;
				}
			}
		}
		
		if (!skill.isOffensive() && !forceUse && (target.isPlayer() || target.isSummon()) && !isFriend(target.getActingPlayer()))
		{
			sendActionFailed();
			return false;
		}
		
		switch (sklTargetType)
		{
			case PARTY :
			case CLAN :
			case PARTY_CLAN :
			case PARTY_NOTME :
			case CORPSE_CLAN :
			case CORPSE_FRIENDLY :
			case AURA :
			case FRONT_AURA :
			case BEHIND_AURA :
			case AREA_SUMMON :
			case AURA_UNDEAD_ENEMY :
			case AURA_MOB :
			case AURA_DEAD_MOB :
			case GROUND :
			case SELF :
				break;
			case CORPSE_MOB :
				if (target.isServitor() && forceUse)
				{
					break;
				}
			default :
				if (!checkPvpSkill(target, skill) && !getAccessLevel().allowPeaceAttack())
				{
					if (msg)
					{
						sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
					}
					sendActionFailed();
					return false;
				}
		}

		if (((sklTargetType == TargetType.HOLY) && !checkIfOkToCastSealOfRule(CastleManager.getInstance().getCastle(this), false, skill, target)) || ((sklTargetType == TargetType.FLAGPOLE) && !checkIfOkToCastFlagDisplay(FortManager.getInstance().getFort(this), false, skill, target)) || ((sklType == SkillType.SIEGEFLAG) && !SkillSiegeFlag.checkIfOkToPlaceFlag(this, false, skill.getId() == 844)))
		{
			sendActionFailed();
			abortCast();
			return false;
		}

		if (skill.getCastRange() > 0 && !skill.isDisableGeoCheck())
		{
			if (sklTargetType == TargetType.GROUND)
			{
				if (!GeoEngine.getInstance().canSeeTarget(this, worldPosition))
				{
					if (msg)
					{
						sendPacket(SystemMessageId.CANT_SEE_TARGET);
					}
					sendActionFailed();
					return false;
				}
			}
			else if (!GeoEngine.getInstance().canSeeTarget(this, target))
			{
				if (msg)
				{
					sendPacket(SystemMessageId.CANT_SEE_TARGET);
				}
				getFarmSystem().isLocked();
				sendActionFailed();
				return false;
			}
		}
		return true;
	}

	public boolean checkIfOkToCastSealOfRule(Castle castle, boolean isCheckOnly, Skill skill, GameObject target)
	{
		SystemMessage sm;
		if ((castle == null) || (castle.getId() <= 0))
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
			sm.addSkillName(skill);
		}
		else if (!castle.getArtefacts().contains(target))
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.INCORRECT_TARGET);
		}
		else if (!castle.getSiege().getIsInProgress())
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
			sm.addSkillName(skill);
		}
		else if (!Util.checkIfInRange(200, this, target, true))
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.DIST_TOO_FAR_CASTING_STOPPED);
		}
		else if (castle.getSiege().getAttackerClan(getClan()) == null)
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
			sm.addSkillName(skill);
		}
		else
		{
			if (!isCheckOnly)
			{
				sm = SystemMessage.getSystemMessage(SystemMessageId.OPPONENT_STARTED_ENGRAVING);
				castle.getSiege().announceToPlayer(sm, false);
			}
			return true;
		}

		sendPacket(sm);
		return false;
	}

	public boolean checkIfOkToCastFlagDisplay(Fort fort, boolean isCheckOnly, Skill skill, GameObject target)
	{
		SystemMessage sm;

		if ((fort == null) || (fort.getId() <= 0))
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
			sm.addSkillName(skill);
		}
		else if (fort.getFlagPole() != target)
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.INCORRECT_TARGET);
		}
		else if (!fort.getSiege().getIsInProgress())
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
			sm.addSkillName(skill);
		}
		else if (!Util.checkIfInRange(200, this, target, true))
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.DIST_TOO_FAR_CASTING_STOPPED);
		}
		else if (fort.getSiege().getAttackerClan(getClan()) == null)
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
			sm.addSkillName(skill);
		}
		else
		{
			if (!isCheckOnly)
			{
				fort.getSiege().announceToPlayer(SystemMessage.getSystemMessage(SystemMessageId.S1_TRYING_RAISE_FLAG), getClan().getName());
			}
			return true;
		}

		sendPacket(sm);
		return false;
	}

	public boolean isInLooterParty(int LooterId)
	{
		final Player looter = GameObjectsStorage.getPlayer(LooterId);

		if (isInParty() && getParty().isInCommandChannel() && (looter != null))
		{
			return getParty().getCommandChannel().getMembers().contains(looter);
		}

		if (isInParty() && (looter != null))
		{
			return getParty().getMembers().contains(looter);
		}

		return false;
	}

	public boolean checkPvpSkill(GameObject target, Skill skill)
	{
		return checkPvpSkill(target, skill, false);
	}

	public boolean checkPvpSkill(GameObject target, Skill skill, boolean srcIsSummon)
	{
		final Player targetPlayer = target != null ? target.getActingPlayer() : null;
		
		final var isFriend = targetPlayer != null ? isFriend(targetPlayer) : false;
		if (skill.isDebuff())
		{
			if (this == targetPlayer)
			{
				return false;
			}
			
			if (targetPlayer != null)
			{
				if (targetPlayer.isInsideZone(ZoneId.PEACE) || isFriend)
				{
					return false;
				}
				
				if (isPKProtected(targetPlayer))
				{
					return false;
				}
			}
		}
		
		if (!(target instanceof EventChestInstance) && (targetPlayer != null) && (target != this) && !(isInDuel() && (targetPlayer.getDuelId() == getDuelId())) && !isInsideZone(ZoneId.PVP) && !targetPlayer.isInsideZone(ZoneId.PVP))
		{
			final var skilldat = getCurrentSkill();
			final var skilldatpet = getCurrentPetSkill();
			final var isInSiege = (targetPlayer.isInsideZone(ZoneId.SIEGE) && !isFriend);
			final var isPKProtected = isPKProtected(targetPlayer);
			final boolean isNotMyTarget = skill.getTargetType() == TargetType.AURA || (skill.getTargetType() != TargetType.ONE && getTarget() != targetPlayer);
			if (((skilldat != null) && !skilldat.isCtrlPressed() && skill.isOffensive() && !srcIsSummon) || ((skilldatpet != null) && !skilldatpet.isCtrlPressed() && skill.isOffensive() && srcIsSummon))
			{
				if ((getClan() != null) && (targetPlayer.getClan() != null))
				{
					if (getClan().isAtWarWith(targetPlayer.getClan().getId()) && targetPlayer.getClan().isAtWarWith(getClan().getId()))
					{
						return true;
					}
				}
				
				if (isCursedWeaponEquipped() || isInSiege)
				{
					return true;
				}
				
				if ((targetPlayer.getPvpFlag() == 0 && targetPlayer.getKarma() == 0) || isPKProtected || isFriend)
				{
					return false;
				}
				
				if (getPvpFlag() == 0 && skill.getTargetType() == TargetType.AURA)
				{
					return false;
				}
			}
			else if (((skilldat != null) && skilldat.isCtrlPressed() && skill.isOffensive() && !srcIsSummon && isNotMyTarget) || ((skilldatpet != null) && skilldatpet.isCtrlPressed() && skill.isOffensive() && srcIsSummon && isNotMyTarget))
			{
				if ((getClan() != null) && (targetPlayer.getClan() != null))
				{
					if (getClan().isAtWarWith(targetPlayer.getClan().getId()) && targetPlayer.getClan().isAtWarWith(getClan().getId()))
					{
						return true;
					}
				}
				
				if (isCursedWeaponEquipped() || isInSiege)
				{
					return true;
				}
				
				if ((targetPlayer.getPvpFlag() == 0 && targetPlayer.getKarma() == 0) || isPKProtected)
				{
					return false;
				}
				
				if (isFriend && skill.getTargetType() == TargetType.AURA)
				{
					return false;
				}
			}
			else if (((skilldat == null) && skill.isOffensive() && !srcIsSummon))
			{
				if ((getClan() != null) && (targetPlayer.getClan() != null))
				{
					if (getClan().isAtWarWith(targetPlayer.getClan().getId()) && targetPlayer.getClan().isAtWarWith(getClan().getId()))
					{
						return true;
					}
				}
				
				if (isCursedWeaponEquipped() || isInSiege)
				{
					return true;
				}
				
				if ((targetPlayer.getPvpFlag() == 0 && targetPlayer.getKarma() == 0) || isPKProtected)
				{
					return false;
				}
				
				if (isFriend && skill.getTargetType() == TargetType.AURA)
				{
					return false;
				}
			}
		}
		else if ((targetPlayer != null) && (target != this) && !(isInDuel() && (targetPlayer.getDuelId() == getDuelId())) && isInsideZone(ZoneId.FUN_PVP) && targetPlayer.isInsideZone(ZoneId.FUN_PVP))
		{
			final var skilldat = getCurrentSkill();
			final var skilldatpet = getCurrentPetSkill();
			final boolean isNotMyTarget = skill.getTargetType() == TargetType.AURA || (skill.getTargetType() != TargetType.ONE && getTarget() != targetPlayer);
			if (((skilldat != null) && !skilldat.isCtrlPressed() && skill.isOffensive() && !srcIsSummon) || ((skilldatpet != null) && !skilldatpet.isCtrlPressed() && skill.isOffensive() && srcIsSummon))
			{
				if (isFriend)
				{
					return false;
				}
			}
			else if (((skilldat != null) && skilldat.isCtrlPressed() && skill.isOffensive() && !srcIsSummon && isNotMyTarget) || ((skilldatpet != null) && skilldatpet.isCtrlPressed() && skill.isOffensive() && srcIsSummon && isNotMyTarget))
			{
				if (isFriend && skill.getTargetType() == TargetType.AURA)
				{
					return false;
				}
			}
		}
		return true;
	}

	public boolean isMageClass()
	{
		return getClassId().isMage();
	}

	public boolean isMounted()
	{
		return _mountType != MountType.NONE;
	}

	public boolean checkLandingState()
	{
		if (isInsideZone(ZoneId.NO_LANDING))
		{
			return true;
		}
		else if (isInsideZone(ZoneId.SIEGE) && !((getClan() != null) && (CastleManager.getInstance().getCastle(this) == CastleManager.getInstance().getCastleByOwner(getClan())) && (this == getClan().getLeader().getPlayerInstance())))
		{
			return true;
		}

		return false;
	}

	public void setMount(int npcId, int npcLevel)
	{
		final MountType type = MountType.findByNpcId(npcId);
		switch (type)
		{
			case NONE :
			{
				setIsFlying(false);
				break;
			}
			case STRIDER :
			{
				if (isNoble())
				{
					addSkill(FrequentSkill.STRIDER_SIEGE_ASSAULT.getSkill(), false);
				}
				break;
			}
			case WYVERN :
			{
				setIsFlying(true);
				break;
			}
		}

		_mountType = type;
		_mountNpcId = npcId;
		_mountLevel = npcLevel;
	}

	public MountType getMountType()
	{
		return _mountType;
	}

	@Override
	public final void stopAllEffects()
	{
		super.stopAllEffects();
		updateAndBroadcastStatus(2);
	}

	@Override
	public final void stopAllEffectsExceptThoseThatLastThroughDeath()
	{
		super.stopAllEffectsExceptThoseThatLastThroughDeath();
		updateAndBroadcastStatus(2);
	}

	public final void stopAllEffectsNotStayOnSubclassChange()
	{
		for (final Effect effect : _effects.getAllEffects())
		{
			if ((effect != null) && !effect.getSkill().isStayOnSubclassChange())
			{
				effect.exit(true, true);
			}
		}
		updateAndBroadcastStatus(2);
	}

	public final void stopAllToggles()
	{
		_effects.stopAllToggles();
	}

	public final void stopCubics(boolean sendPacket)
	{
		if (!_cubics.isEmpty())
		{
			for (final var cubic : _cubics.values())
			{
				cubic.stopAction();
				cubic.cancelDisappear();
			}
			_cubics.clear();
			if (sendPacket)
			{
				broadcastUserInfo(true);
			}
		}
	}

	public final void stopCubicsByOthers()
	{
		if (!_cubics.isEmpty())
		{
			boolean broadcast = false;
			for (final var cubic : _cubics.values())
			{
				final var valid = Config.ALLOW_STOP_ALL_CUBICS || (!Config.ALLOW_STOP_ALL_CUBICS && cubic.givenByOther());
				if (valid)
				{
					cubic.stopAction();
					cubic.cancelDisappear();
					_cubics.remove(cubic.getId());
					broadcast = true;
				}
			}
			if (broadcast)
			{
				broadcastUserInfo(true);
			}
		}
	}
	
	private class UpdateAbnormalEffectTask implements Runnable
	{
		@Override
		public void run()
		{
			broadcastUserInfo(false);
			_effectsUpdateTask = null;
		}
	}

	@Override
	public void updateAbnormalEffect()
	{
		try
		{
			if (_effectsUpdateTask != null)
			{
				return;
			}
			_effectsUpdateTask = ThreadPoolManager.getInstance().schedule(new UpdateAbnormalEffectTask(), Config.USER_ABNORMAL_EFFECTS_INTERVAL);
		}
		catch (final Exception e)
		{}
	}

	public void setInventoryBlockingStatus(boolean val)
	{
		_inventoryDisable = val;
		if (val)
		{
			getPersonalTasks().addTask(new InventoryEnableTask(1500L));
		}
	}

	public boolean isInventoryDisabled()
	{
		return _inventoryDisable;
	}

	public Map<Integer, CubicInstance> getCubics()
	{
		return _cubics;
	}

	public void addCubic(int id, int level, double cubicPower, int cubicDelay, int cubicSkillChance, int cubicMaxCount, int cubicDuration, boolean givenByOther)
	{
		_cubics.put(id, new CubicInstance(this, id, level, (int) cubicPower, cubicDelay, cubicSkillChance, cubicMaxCount, cubicDuration, givenByOther));
	}

	public CubicInstance getCubicById(int id)
	{
		if (_cubics.containsKey(id))
		{
			return _cubics.get(id);
		}
		return null;
	}
	
	public int getCubicsSize()
	{
		return _cubics.size();
	}
	
	public boolean removeCubicById(int id)
	{
		if (_cubics.containsKey(id))
		{
			final var cubic = _cubics.get(id);
			if (cubic != null)
			{
				cubic.stopAction();
				cubic.cancelDisappear();
				_cubics.remove(id);
				return true;
			}
		}
		return false;
	}
	
	public CubicInstance getCubicByPosition(int pos)
	{
		if (_cubics.isEmpty())
		{
			return null;
		}
		
		int i = 0;
		for (final var cubic : _cubics.values())
		{
			if (cubic != null)
			{
				if (i == pos)
				{
					return cubic;
				}
				i++;
			}
		}
		return null;
	}
	
	public boolean isCubicLimit(int id, int limit)
	{
		if (_cubics.containsKey(id) || _cubics.size() >= limit)
		{
			return true;
		}
		return false;
	}

	public int getEnchantEffect()
	{
		final ItemInstance wpn = getActiveWeaponInstance();
		if (wpn == null)
		{
			return 0;
		}
		return Math.min(127, wpn.getEnchantLevel());
	}

	public void setLastFolkNPC(Npc folkNpc)
	{
		_lastFolkNpc = folkNpc;
	}

	public Npc getLastFolkNPC()
	{
		return _lastFolkNpc != null && _lastFolkNpc.isVisible() ? _lastFolkNpc : null;
	}

	public boolean isFestivalParticipant()
	{
		return SevenSignsFestival.getInstance().isParticipant(this);
	}

	public void addAutoSoulShot(int itemId)
	{
		_activeSoulShots.add(itemId);
	}

	public boolean removeAutoSoulShot(int itemId)
	{
		return _activeSoulShots.remove(itemId);
	}

	public Set<Integer> getAutoSoulShot()
	{
		return _activeSoulShots;
	}
	
	public void sendActiveAutoShots()
	{
		for (final int id : _activeSoulShots)
		{
			sendPacket(new ExAutoSoulShot(id, 1));
		}
	}

	@Override
	public void rechargeShots(boolean physical, boolean magic)
	{
		ItemInstance item;
		IItemHandler handler;

		if ((_activeSoulShots == null) || _activeSoulShots.isEmpty())
		{
			return;
		}

		for (final int itemId : _activeSoulShots)
		{
			item = getInventory().getItemByItemId(itemId);
			
			if (item != null)
			{
				if (magic)
				{
					if (item.getItem().getDefaultAction() == ActionType.fishingshot)
					{
						handler = ItemHandler.getInstance().getHandler(item.getEtcItem());
						if (handler != null)
						{
							handler.useItem(this, item, false);
						}
					}
					else if (item.getItem().getDefaultAction() == ActionType.spiritshot)
					{
						handler = ItemHandler.getInstance().getHandler(item.getEtcItem());
						if (handler != null)
						{
							handler.useItem(this, item, false);
						}
					}
				}
				
				if (physical)
				{
					if (item.getItem().getDefaultAction() == ActionType.soulshot)
					{
						handler = ItemHandler.getInstance().getHandler(item.getEtcItem());
						if (handler != null)
						{
							handler.useItem(this, item, false);
						}
					}
				}
			}
			else
			{
				removeAutoSoulShot(itemId);
			}
		}
	}

	public boolean haveAutoShot(int itemId)
	{
		return _activeSoulShots.contains(itemId);
	}

	public void disableAutoShotsAll()
	{
		for (final int itemId : _activeSoulShots)
		{
			sendPacket(new ExAutoSoulShot(itemId, 0));
			final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.AUTO_USE_OF_S1_CANCELLED);
			sm.addItemName(itemId);
			sendPacket(sm);
		}
		_activeSoulShots.clear();
	}

	public int getClanPrivileges()
	{
		return _clanPrivileges;
	}

	public void setClanPrivileges(int n)
	{
		_clanPrivileges = n;
	}

	public void setPledgeClass(int classId)
	{
		_pledgeClass = classId;
		checkItemRestriction();
	}

	public int getPledgeClass()
	{
		return _pledgeClass;
	}

	public void setPledgeType(int typeId)
	{
		_pledgeType = typeId;
	}

	public int getPledgeType()
	{
		return _pledgeType;
	}

	public int getApprentice()
	{
		return _apprentice;
	}

	public void setApprentice(int apprentice_id)
	{
		_apprentice = apprentice_id;
	}

	public int getSponsor()
	{
		return _sponsor;
	}

	public void setSponsor(int sponsor_id)
	{
		_sponsor = sponsor_id;
	}

	public int getBookMarkSlot()
	{
		return _bookmarkslot;
	}

	public void setBookMarkSlot(int slot)
	{
		_bookmarkslot = slot;
		sendPacket(new ExGetBookMarkInfo(this));
	}

	@Override
	public void sendMessage(String message)
	{
		sendPacket(SystemMessage.sendString(message));
	}

	public void enterObserverMode(int x, int y, int z)
	{
		_lastX = getX();
		_lastY = getY();
		_lastZ = getZ();

		stopEffects(EffectType.HIDE);

		_observerMode = true;
		setTarget(null);
		setIsParalyzed(true);
		startParalyze();
		setIsInvul(true);
		setInvisible(true);
		sendPacket(new ObserverStart(x, y, z));
		teleToLocation(x, y, z, false, getReflection());
		broadcastCharInfo();
	}

	public void enterTournamentObserverMode(Tournament tournament)
	{
		Location loc = TournamentUtil.TOURNAMENT_EVENTS.get(tournament.getType()).getTeamObserverLoc().stream().findFirst().orElse(new Location());
		Reflection reflection = tournament.getInstanceId();

		if (hasSummon())
		{
			getSummon().unSummon(this);
		}

		stopEffects(EffectType.HIDE);

		stopCubics(false);

		if (getParty() != null)
		{
			getParty().removePartyMember(this, messageType.Expelled);
		}

		tournamentGameId = tournament.getId();
		if (isSitting())
		{
			standUp();
		}
		if (!_observerMode)
		{
			_lastX = getX();
			_lastY = getY();
			_lastZ = getZ();
		}

		setPartyTournament(tournament);
		_observerMode = true;
		setTarget(null);
		setIsInvul(true);
		setInvisible(true);
		teleToLocation(loc, false, reflection);
		sendPacket(new ExOlympiadMode(3));
		broadcastCharInfo();
		tournament.broadcastUserInfoToObserver(this);
	}

	public void leaveTournamentObserverMode()
	{
		if (tournamentGameId == -1)
		{
			return;
		}
		tournamentGameId = -1;
		_observerMode = false;
		TournamentData.getInstance().onPlayerObserverLeave(this);
		setTarget(null);
		setPartyTournament(null);
		sendPacket(new ExOlympiadMode(0));
		teleToLocation(_lastX, _lastY, _lastZ, true, ReflectionManager.DEFAULT);
		if (!isGM())
		{
			setInvisible(false);
			setIsInvul(false);
		}
		if (hasAI())
		{
			getAI().setIntention(CtrlIntention.IDLE);
		}
		setLastCords(0, 0, 0);
		broadcastUserInfo(true);
	}

	public void setLastCords(int x, int y, int z)
	{
		_lastX = getX();
		_lastY = getY();
		_lastZ = getZ();
	}

	public void enterOlympiadObserverMode(Location loc, int id)
	{
		if (hasSummon())
		{
			getSummon().unSummon(this);
		}

		stopEffects(EffectType.HIDE);

		stopCubics(false);

		if (getParty() != null)
		{
			getParty().removePartyMember(this, messageType.Expelled);
		}

		_olympiadGameId = id;
		if (isSitting())
		{
			standUp();
		}
		if (!_observerMode)
		{
			_lastX = getX();
			_lastY = getY();
			_lastZ = getZ();
		}

		_observerMode = true;
		setTarget(null);
		setIsInvul(true);
		setInvisible(true);
		teleToLocation(loc, false, getReflection());
		sendPacket(new ExOlympiadMode(3));
		broadcastCharInfo();
	}

	public void leaveObserverMode()
	{
		setTarget(null);
		setPartyTournament(null);
		teleToLocation(_lastX, _lastY, _lastZ, false, ReflectionManager.DEFAULT);
		setLastCords(0, 0, 0);
		sendPacket(new ObserverEnd(this));
		setIsParalyzed(false);
		if (!isGM())
		{
			setInvisible(false);
			setIsInvul(false);
		}
		if (hasAI())
		{
			getAI().setIntention(CtrlIntention.IDLE);
		}

		_observerMode = false;
		broadcastCharInfo();
		broadcastUserInfo(true);
	}

	public void leaveOlympiadObserverMode()
	{
		if (_olympiadGameId == -1)
		{
			return;
		}
		_olympiadGameId = -1;
		_observerMode = false;
		setTarget(null);
		sendPacket(new ExOlympiadMode(0));
		teleToLocation(_lastX, _lastY, _lastZ, true, ReflectionManager.DEFAULT);
		if (!isGM())
		{
			setInvisible(false);
			setIsInvul(false);
		}
		if (hasAI())
		{
			getAI().setIntention(CtrlIntention.IDLE);
		}
		setLastCords(0, 0, 0);
		broadcastUserInfo(true);
	}

	public void setOlympiadSide(int i)
	{
		_olympiadSide = i;
	}

	public int getOlympiadSide()
	{
		return _olympiadSide;
	}

	public void setOlympiadGameId(int id)
	{
		_olympiadGameId = id;
	}

	public int getOlympiadGameId()
	{
		return _olympiadGameId;
	}
	
	public void setOlympiadGame(AbstractOlympiadGame game)
	{
		_olympiadGame = game;
	}
	
	public AbstractOlympiadGame getOlympiadGame()
	{
		return _olympiadGame;
	}

	public int getLastX()
	{
		return _lastX;
	}

	public int getLastY()
	{
		return _lastY;
	}

	public int getLastZ()
	{
		return _lastZ;
	}

	public boolean inObserverMode()
	{
		return _observerMode;
	}

	public int getTeleMode()
	{
		return _telemode;
	}

	public void setTeleMode(int mode)
	{
		_telemode = mode;
	}

	public void setLoto(int i, int val)
	{
		_loto[i] = val;
	}

	public int getLoto(int i)
	{
		return _loto[i];
	}

	public void setRace(int i, int val)
	{
		_race[i] = val;
	}

	public int getRace(int i)
	{
		return _race[i];
	}

	public boolean getMessageRefusal()
	{
		return _messageRefusal;
	}

	public void setMessageRefusal(boolean mode)
	{
		_messageRefusal = mode;
		sendPacket(new EtcStatusUpdate(this));
	}

	public void setDietMode(boolean mode)
	{
		_dietMode = mode;
	}

	public boolean getDietMode()
	{
		return _dietMode;
	}

	public void setExchangeRefusal(boolean mode)
	{
		_exchangeRefusal = mode;
	}

	public boolean getExchangeRefusal()
	{
		return _exchangeRefusal;
	}

	public BlockedList getBlockList()
	{
		return _blockList;
	}

	public void setHero(boolean hero, boolean giveSkills)
	{
		if (hero && (_baseClass == _activeClass))
		{
			if (giveSkills)
			{
				for (final Skill skill : SkillTreesParser.getInstance().getHeroSkillTree().values())
				{
					addSkill(skill, false);
				}
			}
		}
		else
		{
			if (giveSkills)
			{
				for (final Skill skill : SkillTreesParser.getInstance().getHeroSkillTree().values())
				{
					removeSkill(skill, false, true);
				}
			}
		}
		_hero = hero;
		sendSkillList(false);
	}

	public void setIsInOlympiadMode(boolean b)
	{
		_inOlympiadMode = b;
	}

	public void setIsOlympiadStart(boolean b)
	{
		_OlympiadStart = b;
	}

	public boolean isOlympiadStart()
	{
		return _OlympiadStart;
	}

	public boolean isHero()
	{
		return _hero;
	}
	
	public boolean isTimeHero()
	{
		return getVar("tempHero") != null;
	}

	public boolean isInOlympiadMode()
	{
		return _inOlympiadMode;
	}

	public boolean isInDuel()
	{
		return _isInDuel;
	}

	public int getDuelId()
	{
		return _duelId;
	}

	public void setDuelState(int mode)
	{
		_duelState = mode;
	}

	public int getDuelState()
	{
		return _duelState;
	}

	public void setIsInDuel(int duelId)
	{
		if (duelId > 0)
		{
			_isInDuel = true;
			_duelState = Duel.DUELSTATE_DUELLING;
			_duelId = duelId;
		}
		else
		{
			if (_duelState == Duel.DUELSTATE_DEAD)
			{
				enableAllSkills();
				getStatus().startHpMpRegeneration();
			}
			_isInDuel = false;
			_duelState = Duel.DUELSTATE_NODUEL;
			_duelId = 0;
		}
	}

	public SystemMessage getNoDuelReason()
	{
		final SystemMessage sm = SystemMessage.getSystemMessage(_noDuelReason);
		sm.addPcName(this);
		_noDuelReason = SystemMessageId.THERE_IS_NO_OPPONENT_TO_RECEIVE_YOUR_CHALLENGE_FOR_A_DUEL;
		return sm;
	}

	public boolean canDuel()
	{
		if (isInCombat() || isJailed())
		{
			_noDuelReason = SystemMessageId.C1_CANNOT_DUEL_BECAUSE_C1_IS_CURRENTLY_ENGAGED_IN_BATTLE;
			return false;
		}
		if (isDead() || isAlikeDead() || ((getCurrentHp() < (getMaxHp() / 2)) || (getCurrentMp() < (getMaxMp() / 2))))
		{
			_noDuelReason = SystemMessageId.C1_CANNOT_DUEL_BECAUSE_C1_HP_OR_MP_IS_BELOW_50_PERCENT;
			return false;
		}
		if (isInDuel())
		{
			_noDuelReason = SystemMessageId.C1_CANNOT_DUEL_BECAUSE_C1_IS_ALREADY_ENGAGED_IN_A_DUEL;
			return false;
		}
		if (isInOlympiadMode() || OlympiadManager.getInstance().isRegistered(this))
		{
			_noDuelReason = SystemMessageId.C1_CANNOT_DUEL_BECAUSE_C1_IS_PARTICIPATING_IN_THE_OLYMPIAD;
			return false;
		}
		if (isCursedWeaponEquipped())
		{
			_noDuelReason = SystemMessageId.C1_CANNOT_DUEL_BECAUSE_C1_IS_IN_A_CHAOTIC_STATE;
			return false;
		}
		if (getPrivateStoreType() != STORE_PRIVATE_NONE)
		{
			_noDuelReason = SystemMessageId.C1_CANNOT_DUEL_BECAUSE_C1_IS_CURRENTLY_ENGAGED_IN_A_PRIVATE_STORE_OR_MANUFACTURE;
			return false;
		}
		if (isMounted() || isInBoat())
		{
			_noDuelReason = SystemMessageId.C1_CANNOT_DUEL_BECAUSE_C1_IS_CURRENTLY_RIDING_A_BOAT_STEED_OR_STRIDER;
			return false;
		}
		if (isFishing())
		{
			_noDuelReason = SystemMessageId.C1_CANNOT_DUEL_BECAUSE_C1_IS_CURRENTLY_FISHING;
			return false;
		}
		if (isInsideZone(ZoneId.PVP) || isInsideZone(ZoneId.PEACE) || isInsideZone(ZoneId.SIEGE))
		{
			_noDuelReason = SystemMessageId.C1_CANNOT_MAKE_A_CHALLANGE_TO_A_DUEL_BECAUSE_C1_IS_CURRENTLY_IN_A_DUEL_PROHIBITED_AREA;
			return false;
		}
		return true;
	}

	public boolean isNoble()
	{
		return _noble;
	}

	public void setNoble(boolean val)
	{
		if (Config.ENABLE_NOBLESS_COLOR)
		{
			getAppearance().setNameColor(Config.NOBLESS_COLOR_NAME);
		}
		if (Config.ENABLE_NOBLESS_TITLE_COLOR)
		{
			getAppearance().setTitleColor(Config.NOBLESS_COLOR_TITLE_NAME);
		}

		final Collection<Skill> nobleSkillTree = SkillTreesParser.getInstance().getNobleSkillTree().values();
		if (val)
		{
			broadcastPacket(new MagicSkillUse(this, this, 6673, 1, 1000, 0));
			for (final Skill skill : nobleSkillTree)
			{
				addSkill(skill, false);
			}
		}
		else
		{
			for (final Skill skill : nobleSkillTree)
			{
				removeSkill(skill, false, true);
			}
		}

		_noble = val;

		sendSkillList(false);
	}

	public void setLvlJoinedAcademy(int lvl)
	{
		_lvlJoinedAcademy = lvl;
	}

	public int getLvlJoinedAcademy()
	{
		return _lvlJoinedAcademy;
	}

	public boolean isAcademyMember()
	{
		return _lvlJoinedAcademy > 0;
	}

	@Override
	public void setTeam(int team)
	{
		super.setTeam(team);
		broadcastUserInfo(true);
		if (hasSummon())
		{
			getSummon().broadcastStatusUpdate();
		}
	}

	public void setWantsPeace(int wantsPeace)
	{
		_wantsPeace = wantsPeace;
	}

	public int getWantsPeace()
	{
		return _wantsPeace;
	}

	public boolean isFishing()
	{
		return _fishing;
	}

	public void setFishing(boolean fishing)
	{
		_fishing = fishing;
	}

	public void sendSkillList(boolean coolTime)
	{
		sendSkillList(this, coolTime);
	}

	public void sendSkillList(Player player, boolean coolTime)
	{
		boolean isDisabled = false;
		final SkillList sl = new SkillList();
		if (player != null)
		{
			for (final Skill s : player.getAllSkills())
			{
				if (s == null)
				{
					continue;
				}

				if ((_transformation != null) && (!hasTransformSkill(s.getId()) && !s.allowOnTransform()))
				{
					continue;
				}

				if (player.getClan() != null)
				{
					isDisabled = s.isClanSkill() && (player.getClan().getReputationScore() < 0);
				}
				
				isDisabled = !s.isClanSkill() && player.isSkillBlocked(s);

				boolean isEnchantable = SkillsParser.getInstance().isEnchantable(s.getId());
				if (isEnchantable)
				{
					final EnchantSkillLearn esl = EnchantSkillGroupsParser.getInstance().getSkillEnchantmentBySkillId(s.getId());
					if (esl != null)
					{
						if (s.getLevel() < esl.getBaseLevel())
						{
							isEnchantable = false;
						}
					}
					else
					{
						isEnchantable = false;
					}
				}
				sl.addSkill(s.getDisplayId(), s.getDisplayLevel(), s.isPassive(), isDisabled, isEnchantable);
			}
		}
		sendPacket(sl);
		if (coolTime)
		{
			sendPacket(new SkillCoolTime(this));
		}
	}

	public boolean addSubClass(int classId, int classIndex)
	{
		if (!_subclassLock.tryLock())
		{
			return false;
		}

		try
		{
			if ((getTotalSubClasses() == Config.MAX_SUBCLASS) || (classIndex == 0))
			{
				return false;
			}

			if (getSubClasses().containsKey(classIndex))
			{
				return false;
			}

			if (CharacterDAO.getInstance().addSubClass(this, classId, classIndex))
			{
				final ClassId subTemplate = ClassId.getClassId(classId);
				final Map<Integer, SkillLearn> skillTree = SkillTreesParser.getInstance().getCompleteClassSkillTree(subTemplate);
				final Map<Integer, Skill> prevSkillList = new HashMap<>();

				for (final SkillLearn skillInfo : skillTree.values())
				{
					if (skillInfo.getGetLevel() <= 40)
					{
						final Skill prevSkill = prevSkillList.get(skillInfo.getId());
						final Skill newSkill = SkillsParser.getInstance().getInfo(skillInfo.getId(), skillInfo.getLvl());

						if ((prevSkill != null) && (prevSkill.getLevel() > newSkill.getLevel()))
						{
							continue;
						}

						prevSkillList.put(newSkill.getId(), newSkill);
						CharacterSkillsDAO.getInstance().store(this, newSkill, prevSkill, classIndex);
					}
				}
			}
			return true;
		}
		finally
		{
			_subclassLock.unlock();
		}
	}

	public boolean modifySubClass(int classIndex, int newClassId)
	{
		if (!_subclassLock.tryLock())
		{
			return false;
		}

		try
		{
			if (CharacterDAO.getInstance().modifySubClass(this, classIndex, newClassId))
			{
				getSubClasses().remove(classIndex);
			}
		}
		finally
		{
			_subclassLock.unlock();
		}
		return addSubClass(newClassId, classIndex);
	}

	public boolean isSubClassActive()
	{
		return _classIndex > 0;
	}

	public Map<Integer, SubClass> getSubClasses()
	{
		if (_subClasses == null)
		{
			_subClasses = new ConcurrentSkipListMap<>();
		}
		return _subClasses;
	}

	public int getTotalSubClasses()
	{
		return getSubClasses().size();
	}

	public int getBaseClass()
	{
		return _baseClass;
	}

	public void setActiveClassId(int activeClass)
	{
		_activeClass = activeClass;
	}
	
	public int getActiveClass()
	{
		return _activeClass;
	}

	public void setClassIndex(int classIndex)
	{
		_classIndex = classIndex;
	}
	
	public int getClassIndex()
	{
		return _classIndex;
	}

	private void setClassTemplate(int classId)
	{
		_activeClass = classId;

		final PcTemplate pcTemplate = CharTemplateParser.getInstance().getTemplate(classId);
		if (pcTemplate == null)
		{
			_log.error("Missing template for classId: " + classId);
			throw new Error();
		}
		setTemplate(pcTemplate);
		
		TransferSkillUtils.checkTransferItems(this);
	}

	public boolean setActiveClass(int classIndex)
	{
		if (!_subclassLock.tryLock())
		{
			return false;
		}

		try
		{
			if (_transformation != null)
			{
				return false;
			}

			for (final ItemInstance item : getInventory().getAugmentedItems())
			{
				if ((item != null) && item.isEquipped())
				{
					item.getAugmentation().removeBonus(this);
				}
			}

			abortCast();
			getActingPlayer().setQueuedSkill(null, false, false);

			for (final Creature character : World.getInstance().getAroundCharacters(this))
			{
				if ((character.getFusionSkill() != null) && (character.getFusionSkill().getTarget() == this))
				{
					character.abortCast();
				}
			}

			if (_sellingBuffs != null)
			{
				_sellingBuffs.clear();
			}

			store(Config.SUBCLASS_STORE_SKILL_COOLTIME);
			clenaUpSkillReuseTimeStamps();
			_charges.set(0);
			getPersonalTasks().removeTask(5, true);

			if (hasServitor())
			{
				getSummon().unSummon(this);
			}

			if (classIndex == 0)
			{
				setClassTemplate(getBaseClass());
			}
			else
			{
				try
				{
					setClassTemplate(getSubClasses().get(classIndex).getClassId());
				}
				catch (final Exception e)
				{
					_log.warn("Could not switch " + getName(null) + "'s sub class to class index " + classIndex + ": " + e.getMessage(), e);
					return false;
				}
			}
			_classIndex = classIndex;

			setLearningClass(getClassId());

			if (isInParty())
			{
				getParty().recalculatePartyLevel();
			}

			for (final Skill oldSkill : getAllSkills())
			{
				if (oldSkill != null && oldSkill.isBlockRemove())
				{
					continue;
				}
				removeSkill(oldSkill, false, true);
			}

			stopAllEffectsExceptThoseThatLastThroughDeath();
			stopAllEffectsNotStayOnSubclassChange();
			stopCubics(false);

			restoreRecipeBook(false);
			restoreDeathPenaltyBuffLevel();

			CharacterSkillsDAO.getInstance().restoreSkills(this);
			rewardSkills();
			regiveTemporarySkills();

			resetDisabledSkills();
			restoreEffects();
			getInventory().checkRuneSkills();
			TimeSkillsTaskManager.getInstance().checkPlayerSkills(this, true, false);
			updateEffectIcons();
			sendPacket(new EtcStatusUpdate(this));

			final QuestState st = getQuestState("_422_RepentYourSins");
			if (st != null)
			{
				st.exitQuest(true);
			}

			for (int i = 0; i < 3; i++)
			{
				_henna[i] = null;
			}

			restoreHenna();
			sendPacket(new HennaInfo(this));

			if (getCurrentHp() > getMaxHp())
			{
				setCurrentHp(getMaxHp());
			}
			if (getCurrentMp() > getMaxMp())
			{
				setCurrentMp(getMaxMp());
			}
			if (getCurrentCp() > getMaxCp())
			{
				setCurrentCp(getMaxCp());
			}

			refreshOverloaded();
			refreshExpertisePenalty();
			setExpBeforeDeath(0);

			_shortCuts.restoreMe();
			sendPacket(new ShortCutInit(this));

			broadcastPacket(new SocialAction(getObjectId(), SocialAction.LEVEL_UP));
			sendPacket(new SkillCoolTime(this));
			sendPacket(new ExStorageMaxCount(this));

			broadcastUserInfo(true);
			
			if (hasPet())
			{
				final PetInstance pet = (PetInstance) getSummon();
				if (pet != null && pet.getPetData().isSynchLevel() && (pet.getLevel() != getLevel()))
				{
					pet.getStat().setLevel(getStat().getLevel());
					pet.getStat().getExpForLevel(getStat().getLevel());
					pet.setCurrentHp(pet.getMaxHp());
					pet.setCurrentMp(pet.getMaxMp());
					pet.broadcastPacket(new SocialAction(getObjectId(), SocialAction.LEVEL_UP));
					pet.updateAndBroadcastStatus(1);
				}
			}
			return true;
		}
		finally
		{
			_subclassLock.unlock();
		}
	}

	public boolean isLocked()
	{
		return _subclassLock.isLocked();
	}
	
	public void stopAcpTask()
	{
		final var task = _acpTask;
		if (task != null)
		{
			task.cancel(true);
			_acpTask = null;
		}
	}

	public void startWarnUserTakeBreak()
	{
		getPersonalTasks().addTask(new WarnUserTakeBreakTask(7200000L));
	}

	public void startRentPet(int seconds)
	{
		getPersonalTasks().addTask(new RentPetTask(seconds * 1000L));
	}

	public boolean isRentedPet()
	{
		return getPersonalTasks().isActiveTask(12);
	}

	public void stopWaterTask()
	{
		if (getPersonalTasks().removeTask(32, true))
		{
			sendPacket(new SetupGauge(this, 2, 0));
			broadcastUserInfo(true);
		}
	}

	public void startWaterTask()
	{
		if (!isDead())
		{
			final int timeinwater = (int) calcStat(Stats.BREATH, 60000, this, null);
			if (getPersonalTasks().addTask(new WaterTask(timeinwater)))
			{
				sendPacket(new SetupGauge(this, 2, timeinwater));
			}
		}
	}
	
	@Override
	public boolean isInWater()
	{
		return getPersonalTasks().isActiveTask(32);
	}

	public void checkWaterState()
	{
		if (isInWater(this))
		{
			final WaterZone waterZone = ZoneManager.getInstance().getZone(this, WaterZone.class);
			if (waterZone != null && waterZone.canUseWaterTask())
			{
				startWaterTask();
			}
		}
		else
		{
			stopWaterTask();
		}
	}

	public void onPlayerEnter()
	{
		startWarnUserTakeBreak();

		if (SevenSigns.getInstance().isSealValidationPeriod() || SevenSigns.getInstance().isCompResultsPeriod())
		{
			if (!isGM() && isIn7sDungeon() && (SevenSigns.getInstance().getPlayerCabal(getObjectId()) != SevenSigns.getInstance().getCabalHighestScore()))
			{
				teleToLocation(TeleportWhereType.TOWN, true, ReflectionManager.DEFAULT);
				setIsIn7sDungeon(false);
				sendMessage("You have been teleported to the nearest town due to the beginning of the Seal Validation period.");
			}
		}
		else
		{
			if (!isGM() && isIn7sDungeon() && (SevenSigns.getInstance().getPlayerCabal(getObjectId()) == SevenSigns.CABAL_NULL))
			{
				teleToLocation(TeleportWhereType.TOWN, true, ReflectionManager.DEFAULT);
				setIsIn7sDungeon(false);
				sendMessage("You have been teleported to the nearest town because you have not signed for any cabal.");
			}
		}

		if (isGM())
		{
			if (isInvul())
			{
				sendMessage("Entering world in Invulnerable mode.");
			}
			if (isInvisible())
			{
				sendMessage("Entering world in Invisible mode.");
			}
			if (isSilenceMode())
			{
				sendMessage("Entering world in Silence mode.");
			}
			
			if (!Config.ENABLE_SAFE_ADMIN_PROTECTION && (_accessLevel.getLevel() > 0))
			{
				_log.warn(_accessLevel.getName() + " access level set for character " + getName(null) + "! [" + getIPAddress() + "] " + getDefaultAddress());
			}
		}
		revalidateZone(true);

		notifyFriends();
		if (!canOverrideCond(PcCondOverride.SKILL_CONDITIONS) && Config.DECREASE_SKILL_LEVEL)
		{
			checkPlayerSkills();
		}
		getListeners().onEnter();
		
		if (isJailed() && !isInsideZone(ZoneId.JAIL))
		{
			startJail();
		}
		else if (!isJailed() && isInsideZone(ZoneId.JAIL) && !isGM())
		{
			stopJail();
		}

		try
		{
			final List<ZoneType> zones = ZoneManager.getInstance().getZones(this);
			if (zones != null && !zones.isEmpty())
			{
				for (final ZoneType zone : zones)
				{
					if (zone != null)
					{
						zone.onPlayerLoginInside(this);
					}
				}
			}
		}
		catch (final Exception e)
		{
			_log.warn("", e);
		}
	}

	public void setLastAccess(long access)
	{
		_lastAccess = access;
	}
	
	public long getLastAccess()
	{
		return _lastAccess;
	}

	@Override
	public void doRevive()
	{
		super.doRevive();
		stopEffects(EffectType.CHARMOFCOURAGE);
		updateEffectIcons();
		sendPacket(new EtcStatusUpdate(this));

		if (isMounted())
		{
			startFeed(_mountNpcId);
		}

		if (getReflectionId() > 0)
		{
			final Reflection instance = ReflectionManager.getInstance().getReflection(getReflectionId());
			if (instance != null)
			{
				instance.cancelEjectDeadPlayer(this);
			}
		}
	}

	@Override
	public void setName(String lang, String value)
	{
		super.setName(lang, value);
		if (Config.CACHE_CHAR_NAMES)
		{
			CharNameHolder.getInstance().addName(this);
		}
	}

	@Override
	public void doRevive(double revivePower)
	{
		restoreExp(revivePower);
		doRevive();
	}

	public void reviveRequest(Player reviver, Skill skill, int time, boolean isSummon, boolean resByAutoFarm)
	{
		if (isResurrectionBlocked())
		{
			return;
		}

		if (_reviveRequested == 1)
		{
			if (_revivePet == isSummon)
			{
				reviver.sendPacket(SystemMessageId.RES_HAS_ALREADY_BEEN_PROPOSED);
			}
			else
			{
				if (isSummon)
				{
					reviver.sendPacket(SystemMessageId.CANNOT_RES_PET2);
				}
				else
				{
					reviver.sendPacket(SystemMessageId.MASTER_CANNOT_RES);
				}
			}
			return;
		}
		if ((isSummon && hasSummon() && getSummon().isDead()) || (!isSummon && isDead()))
		{
			_reviveRequested = 1;
			int restoreExp = 0;
			if (isInFightEvent())
			{
				_revivePower = 100;
			}
			else if (isPhoenixBlessed())
			{
				_revivePower = 100;
			}
			else if (isAffected(EffectFlag.CHARM_OF_COURAGE))
			{
				_revivePower = 0;
			}
			else
			{
				_revivePower = Formulas.calculateSkillResurrectRestorePercent(skill != null ? skill.getPower() : 0, reviver);
			}

			restoreExp = (int) Math.round(((getExpBeforeDeath() - getExp()) * _revivePower) / 100);

			_revivePet = isSummon;

			if (resByAutoFarm || (getFarmSystem().isAutofarming() && reviver.isFriend(this, false)))
			{
				getPersonalTasks().addTask(new ReviveTask(2000));
				return;
			}
			
			if (isAffected(EffectFlag.CHARM_OF_COURAGE))
			{
				final ConfirmDlg dlg = new ConfirmDlg(SystemMessageId.RESURRECT_USING_CHARM_OF_COURAGE.getId());
				dlg.addTime(60000);
				sendPacket(dlg);
				return;
			}
			final ConfirmDlg dlg = new ConfirmDlg(SystemMessageId.RESSURECTION_REQUEST_BY_C1_FOR_S2_XP.getId());
			if (time > 0)
			{
				dlg.addTime(time * 1000);
			}
			dlg.addPcName(reviver);
			dlg.addString(String.valueOf(Math.abs(restoreExp)));
			sendPacket(dlg);
		}
	}

	public void reviveAnswer(int answer)
	{
		if ((_reviveRequested != 1) || (!isDead() && !_revivePet) || (_revivePet && hasSummon() && !getSummon().isDead()))
		{
			_reviveRequested = 0;
			return;
		}

		if ((answer == 0) && isPhoenixBlessed())
		{
			stopEffects(EffectType.PHOENIX_BLESSING);
			stopAllEffectsExceptThoseThatLastThroughDeath();
		}

		if (answer == 1)
		{
			if (!_revivePet)
			{
				if (_revivePower != 0)
				{
					doRevive(_revivePower);
				}
				else
				{
					doRevive();
				}
			}
			else if (hasSummon())
			{
				if (_revivePower != 0)
				{
					getSummon().doRevive(_revivePower);
				}
				else
				{
					getSummon().doRevive();
				}
			}
		}
		
		if (getFarmSystem().isAutofarming())
		{
			getFarmSystem().clearEmptyTime();
		}
		_reviveRequested = 0;
		_revivePower = 0;
	}

	public boolean isReviveRequested()
	{
		return (_reviveRequested == 1);
	}

	public boolean isRevivingPet()
	{
		return _revivePet;
	}

	public void removeReviving()
	{
		_reviveRequested = 0;
		_revivePower = 0;
	}

	public void onActionRequest()
	{
		if (isSpawnProtected())
		{
			if (!isRegisteredInFightEvent())
			{
				sendPacket(SystemMessageId.YOU_ARE_NO_LONGER_PROTECTED_FROM_AGGRESSIVE_MONSTERS);
			}
		}
		if (isTeleportProtected())
		{
			sendMessage("Teleport spawn protection ended.");
		}
		isntAfk();
		setProtection(false);
		setTeleportProtection(false);
	}

	public int getExpertiseLevel()
	{
		int level = getSkillLevel(239);
		if (level < 0)
		{
			level = 0;
		}
		return level;
	}

	@Override
	public void teleToLocation(int x, int y, int z, int heading, boolean allowRandomOffset, Reflection r)
	{
		if (isInVehicle() && !getVehicle().isTeleporting())
		{
			setVehicle(null);
		}

		if (isFlyingMounted() && (z < -1005))
		{
			z = -1005;
		}

		super.teleToLocation(x, y, z, heading, allowRandomOffset, r);
	}

	@Override
	public final void onTeleported()
	{
		super.onTeleported();

		if (isInAirShip())
		{
			getAirShip().sendInfo(this);
		}

		revalidateZone(true);
		checkItemRestriction();

		if ((Config.PLAYER_TELEPORT_PROTECTION > 0) && !isInOlympiadMode() && !isInPartyTournament())
		{
			setTeleportProtection(true);
		}

		if (getTrainedBeasts() != null)
		{
			for (final TamedBeastInstance tamedBeast : getTrainedBeasts())
			{
				tamedBeast.deleteMe();
			}
			getTrainedBeasts().clear();
		}

		if (hasSummon())
		{
			getSummon().teleToLeader(false);
		}
	}

	@Override
	public void addExpAndSp(long addToExp, int addToSp)
	{
		if (getExpOn())
		{
			if (addToExp > 0)
			{
				getCounters().addAchivementInfo("expAcquired", 0, addToExp, false, false, false);
			}
			
			if (addToSp > 0)
			{
				getCounters().addAchivementInfo("spAcquired", 0, addToSp, false, false, false);
			}
			getStat().addExpAndSp(addToExp, addToSp, false);
		}
		else
		{
			getStat().addExpAndSp(0, addToSp, false);
		}
	}

	public void addExpAndSp(long addToExp, int addToSp, boolean useVitality)
	{
		if (getExpOn())
		{
			if (addToExp > 0)
			{
				getCounters().addAchivementInfo("expAcquired", 0, addToExp, false, false, false);
			}
			
			if (addToSp > 0)
			{
				getCounters().addAchivementInfo("spAcquired", 0, addToSp, false, false, false);
			}
			getStat().addExpAndSp(addToExp, addToSp, useVitality);
		}
		else
		{
			getStat().addExpAndSp(0, addToSp, useVitality);
		}
	}

	public void removeExpAndSp(long removeExp, int removeSp)
	{
		getStat().removeExpAndSp(removeExp, removeSp, true);
	}

	public void removeExpAndSp(long removeExp, int removeSp, boolean sendMessage)
	{
		getStat().removeExpAndSp(removeExp, removeSp, sendMessage);
	}

	@Override
	public void reduceCurrentHp(double value, Creature attacker, boolean awake, boolean isDOT, Skill skill)
	{
		if ((attacker != null) && attacker.isPlayer() && attacker.getActingPlayer().isInFightEvent())
		{
			attacker.getActingPlayer().getFightEvent().onDamage(attacker, this, value);
		}

		if (skill != null)
		{
			getStatus().reduceHp(value, attacker, awake, isDOT, skill.isToggle(), skill.isBehind(), true);
		}
		else
		{
			getStatus().reduceHp(value, attacker, awake, isDOT, false, false, true);
		}

		if (getTrainedBeasts() != null)
		{
			for (final TamedBeastInstance tamedBeast : getTrainedBeasts())
			{
				tamedBeast.onOwnerGotAttacked(attacker);
			}
		}
	}

	public void broadcastSnoop(int type, String name, String _text)
	{
		if (!_snoopListener.isEmpty())
		{
			final Snoop sn = new Snoop(getObjectId(), getName(null), type, name, _text);

			for (final Player pci : _snoopListener)
			{
				if (pci != null)
				{
					pci.sendPacket(sn);
				}
			}
		}
	}

	public void addSnooper(Player pci)
	{
		if (!_snoopListener.contains(pci))
		{
			_snoopListener.add(pci);
		}
	}

	public void removeSnooper(Player pci)
	{
		_snoopListener.remove(pci);
	}

	public void addSnooped(Player pci)
	{
		if (!_snoopedPlayer.contains(pci))
		{
			_snoopedPlayer.add(pci);
		}
	}

	public void removeSnooped(Player pci)
	{
		_snoopedPlayer.remove(pci);
	}

	public boolean validateItemManipulation(int objectId, String action)
	{
		final ItemInstance item = getInventory().getItemByObjectId(objectId);

		if ((item == null) || (item.getOwnerId() != getObjectId()))
		{
			if (Config.DEBUG)
			{
				_log.warn(getObjectId() + ": player tried to " + action + " item he is not owner of");
			}
			return false;
		}

		if ((hasSummon() && (getSummon().getControlObjectId() == objectId)) || (getMountObjectID() == objectId))
		{
			if (Config.DEBUG)
			{
				_log.warn(getObjectId() + ": player tried to " + action + " item controling pet");
			}
			return false;
		}

		if (getActiveEnchantItemId() == objectId)
		{
			if (Config.DEBUG)
			{
				_log.warn(getObjectId() + ":player tried to " + action + " an enchant scroll he was using");
			}
			return false;
		}

		if (CursedWeaponsManager.getInstance().isCursed(item.getId()))
		{
			return false;
		}

		return true;
	}

	public void setInCrystallize(boolean inCrystallize)
	{
		_inCrystallize = inCrystallize;
	}

	public boolean isInCrystallize()
	{
		return _inCrystallize;
	}

	@Override
	protected void onDelete()
	{
		if (!isInOfflineMode() && getPrivateStoreType() == STORE_PRIVATE_SELL && isSitting())
		{
			for (final TradeItem item : getSellList().getItems())
			{
				AuctionsManager.getInstance().removeStore(this, item.getAuctionId());
			}
		}
		notifyFriends();
		getTeleportBookmarks().clear();
		super.onDelete();
	}

	private Fish _fish;

	public void startFishing(int _x, int _y, int _z, boolean isHotSpring)
	{
		if (_lure == null)
		{
			sendPacket(SystemMessageId.BAIT_ON_HOOK_BEFORE_FISHING);
			return;
		}
		
		stopMove(null);
		setIsImmobilized(true);
		_fishing = true;
		_fishx = _x;
		_fishy = _y;
		_fishz = _z;

		final int lvl = GetRandomFishLvl();
		final int group = GetRandomGroup();
		final int type = GetRandomFishType(group);
		List<Fish> fishs = FishParser.getInstance().getFish(lvl, type, group);

		if ((fishs == null) || fishs.isEmpty())
		{
			sendMessage("Error - Fishes are not definied");
			endFishing(false);
			return;
		}
		final int check = Rnd.get(fishs.size());

		_fish = fishs.get(check).clone();

		if (isHotSpring && (_lure.getId() == 8548) && (getSkillLevel(1315) > 19) && Rnd.nextBoolean())
		{
			_fish = new Fish(271, 8547, "Old Box", 10, 20, 100, 618, 1185, 0, 10, 40, 20, 30, 3, 618, 0, 1);
		}

		fishs.clear();
		fishs = null;
		sendPacket(SystemMessageId.CAST_LINE_AND_START_FISHING);
		if (!GameTimeController.getInstance().isNight() && _lure.isNightLure())
		{
			_fish.setFishGroup(-1);
		}

		broadcastPacket(new ExFishingStart(this, _fish.getFishGroup(), _x, _y, _z, _lure.isNightLure()));
		sendPacket(new PlaySound(1, "SF_P_01", 0, 0, 0, 0, 0));
		startLookingForFishTask();
	}

	public void startLookingForFishTask()
	{
		if (!isDead() && !getPersonalTasks().isActiveTask(30))
		{
			int checkDelay = 0;
			boolean isNoob = false;
			boolean isUpperGrade = false;

			if (_lure != null)
			{
				final int lureid = _lure.getId();
				isNoob = _fish.getFishGrade() == 0;
				isUpperGrade = _fish.getFishGrade() == 2;
				if ((lureid == 6519) || (lureid == 6522) || (lureid == 6525) || (lureid == 8505) || (lureid == 8508) || (lureid == 8511))
				{
					checkDelay = Math.round((float) (_fish.getGutsCheckTime() * (1.33)));
				}
				else if ((lureid == 6520) || (lureid == 6523) || (lureid == 6526) || ((lureid >= 8505) && (lureid <= 8513)) || ((lureid >= 7610) && (lureid <= 7613)) || ((lureid >= 7807) && (lureid <= 7809)) || ((lureid >= 8484) && (lureid <= 8486)))
				{
					checkDelay = Math.round((float) (_fish.getGutsCheckTime() * (1.00)));
				}
				else if ((lureid == 6521) || (lureid == 6524) || (lureid == 6527) || (lureid == 8507) || (lureid == 8510) || (lureid == 8513) || (lureid == 8548))
				{
					checkDelay = Math.round((float) (_fish.getGutsCheckTime() * (0.66)));
				}
			}
			getPersonalTasks().addTask(new LookingForFishTask(checkDelay, _fish.getStartCombatTime(), _fish.getFishGuts(), _fish.getFishGroup(), isNoob, isUpperGrade));
		}
	}

	private int GetRandomGroup()
	{
		switch (_lure.getId())
		{
			case 7807 :
			case 7808 :
			case 7809 :
			case 8486 :
				return 0;
			case 8485 :
			case 8506 :
			case 8509 :
			case 8512 :
				return 2;
			default :
				return 1;
		}
	}

	private int GetRandomFishType(int group)
	{
		final int check = Rnd.get(100);
		int type = 1;
		switch (group)
		{
			case 0 :
				switch (_lure.getId())
				{
					case 7807 :
						if (check <= 54)
						{
							type = 5;
						}
						else if (check <= 77)
						{
							type = 4;
						}
						else
						{
							type = 6;
						}
						break;
					case 7808 :
						if (check <= 54)
						{
							type = 4;
						}
						else if (check <= 77)
						{
							type = 6;
						}
						else
						{
							type = 5;
						}
						break;
					case 7809 :
						if (check <= 54)
						{
							type = 6;
						}
						else if (check <= 77)
						{
							type = 5;
						}
						else
						{
							type = 4;
						}
						break;
					case 8486 :
						if (check <= 33)
						{
							type = 4;
						}
						else if (check <= 66)
						{
							type = 5;
						}
						else
						{
							type = 6;
						}
						break;
				}
				break;
			case 1 :
				switch (_lure.getId())
				{
					case 7610 :
					case 7611 :
					case 7612 :
					case 7613 :
						type = 3;
						break;
					case 6519 :
					case 8505 :
					case 6520 :
					case 6521 :
					case 8507 :
						if (check <= 54)
						{
							type = 1;
						}
						else if (check <= 74)
						{
							type = 0;
						}
						else if (check <= 94)
						{
							type = 2;
						}
						else
						{
							type = 3;
						}
						break;
					case 6522 :
					case 8508 :
					case 6523 :
					case 6524 :
					case 8510 :
						if (check <= 54)
						{
							type = 0;
						}
						else if (check <= 74)
						{
							type = 1;
						}
						else if (check <= 94)
						{
							type = 2;
						}
						else
						{
							type = 3;
						}
						break;
					case 6525 :
					case 8511 :
					case 6526 :
					case 6527 :
					case 8513 :
						if (check <= 55)
						{
							type = 2;
						}
						else if (check <= 74)
						{
							type = 1;
						}
						else if (check <= 94)
						{
							type = 0;
						}
						else
						{
							type = 3;
						}
						break;
					case 8484 :
						if (check <= 33)
						{
							type = 0;
						}
						else if (check <= 66)
						{
							type = 1;
						}
						else
						{
							type = 2;
						}
						break;
				}
				break;
			case 2 :
				switch (_lure.getId())
				{
					case 8506 :
						if (check <= 54)
						{
							type = 8;
						}
						else if (check <= 77)
						{
							type = 7;
						}
						else
						{
							type = 9;
						}
						break;
					case 8509 :
						if (check <= 54)
						{
							type = 7;
						}
						else if (check <= 77)
						{
							type = 9;
						}
						else
						{
							type = 8;
						}
						break;
					case 8512 :
						if (check <= 54)
						{
							type = 9;
						}
						else if (check <= 77)
						{
							type = 8;
						}
						else
						{
							type = 7;
						}
						break;
					case 8485 :
						if (check <= 33)
						{
							type = 7;
						}
						else if (check <= 66)
						{
							type = 8;
						}
						else
						{
							type = 9;
						}
						break;
				}
		}
		return type;
	}

	private int GetRandomFishLvl()
	{
		int skilllvl = getSkillLevel(1315);
		final Effect e = getFirstEffect(2274);
		if (e != null)
		{
			skilllvl = (int) e.getSkill().getPower();
		}

		if (skilllvl <= 0)
		{
			return 1;
		}
		int randomlvl;
		final int check = Rnd.get(100);

		if (check <= 50)
		{
			randomlvl = skilllvl;
		}
		else if (check <= 85)
		{
			randomlvl = skilllvl - 1;
			if (randomlvl <= 0)
			{
				randomlvl = 1;
			}
		}
		else
		{
			randomlvl = skilllvl + 1;
			if (randomlvl > 27)
			{
				randomlvl = 27;
			}
		}
		return randomlvl;
	}

	public void startFishCombat(boolean isNoob, boolean isUpperGrade)
	{
		_fishCombat = new Fishing(this, _fish, isNoob, isUpperGrade, _lure.getId());
	}

	public void endFishing(boolean win)
	{
		_fishing = false;
		_fishx = 0;
		_fishy = 0;
		_fishz = 0;
		if (_fishCombat == null)
		{
			sendPacket(SystemMessageId.BAIT_LOST_FISH_GOT_AWAY);
		}
		_fishCombat = null;
		_lure = null;
		broadcastPacket(new ExFishingEnd(win, this));
		sendPacket(SystemMessageId.REEL_LINE_AND_STOP_FISHING);
		setIsImmobilized(false);
		getPersonalTasks().removeTask(30, true);
	}

	public Fishing getFishCombat()
	{
		return _fishCombat;
	}

	public int getFishx()
	{
		return _fishx;
	}

	public int getFishy()
	{
		return _fishy;
	}

	public int getFishz()
	{
		return _fishz;
	}

	public void setLure(ItemInstance lure)
	{
		_lure = lure;
	}

	public ItemInstance getLure()
	{
		return _lure;
	}

	public int getInventoryLimit()
	{
		int ivlim;
		if (isGM())
		{
			ivlim = Config.INVENTORY_MAXIMUM_GM;
		}
		else if (getRace() == Race.Dwarf)
		{
			ivlim = Config.INVENTORY_MAXIMUM_DWARF;
		}
		else
		{
			ivlim = Config.INVENTORY_MAXIMUM_NO_DWARF;
		}
		ivlim += (int) getStat().calcStat(Stats.INV_LIM, 0, null, null);
		ivlim += getVarInt("expandInventory", 0);
		ivlim = Math.min(ivlim, Config.EXPAND_INVENTORY_LIMIT);
		return ivlim;
	}
	
	public int getPrivateInventoryLimit()
	{
		return getInventoryLimit();
	}

	public int getWareHouseLimit()
	{
		int whlim;
		if (getRace() == Race.Dwarf)
		{
			whlim = Config.WAREHOUSE_SLOTS_DWARF;
		}
		else
		{
			whlim = Config.WAREHOUSE_SLOTS_NO_DWARF;
		}

		whlim += (int) getStat().calcStat(Stats.WH_LIM, 0, null, null);
		whlim += getVarInt("expandWareHouse", 0);
		whlim = Math.min(whlim, Config.EXPAND_WAREHOUSE_LIMIT);
		return whlim;
	}

	public int getPrivateSellStoreLimit()
	{
		int pslim;

		if (getRace() == Race.Dwarf)
		{
			pslim = Config.MAX_PVTSTORESELL_SLOTS_DWARF;
		}
		else
		{
			pslim = Config.MAX_PVTSTORESELL_SLOTS_OTHER;
		}

		pslim += (int) getStat().calcStat(Stats.P_SELL_LIM, 0, null, null);
		pslim += getVarInt("expandSellStore", 0);
		pslim = Math.min(pslim, Config.EXPAND_SELLSTORE_LIMIT);
		return pslim;
	}

	public int getPrivateBuyStoreLimit()
	{
		int pblim;

		if (getRace() == Race.Dwarf)
		{
			pblim = Config.MAX_PVTSTOREBUY_SLOTS_DWARF;
		}
		else
		{
			pblim = Config.MAX_PVTSTOREBUY_SLOTS_OTHER;
		}
		pblim += (int) getStat().calcStat(Stats.P_BUY_LIM, 0, null, null);
		pblim += getVarInt("expandBuyStore", 0);
		pblim = Math.min(pblim, Config.EXPAND_BUYSTORE_LIMIT);
		return pblim;
	}

	public int getDwarfRecipeLimit()
	{
		int recdlim = Config.DWARF_RECIPE_SLOTS;
		recdlim += (int) getStat().calcStat(Stats.REC_D_LIM, 0, null, null);
		recdlim += getVarInt("expandDwarfRecipe", 0);
		recdlim = Math.min(recdlim, Config.EXPAND_DWARFRECIPE_LIMIT);
		return recdlim;
	}

	public int getCommonRecipeLimit()
	{
		int recclim = Config.COMMON_RECIPE_SLOTS;
		recclim += (int) getStat().calcStat(Stats.REC_C_LIM, 0, null, null);
		recclim += getVarInt("expandCommonRecipe", 0);
		recclim = Math.min(recclim, Config.EXPAND_COMMONRECIPE_LIMIT);
		return recclim;
	}

	public int getMountNpcId()
	{
		return _mountNpcId;
	}

	public int getMountLevel()
	{
		return _mountLevel;
	}

	public void setMountObjectID(int newID)
	{
		_mountObjectID = newID;
	}

	public int getMountObjectID()
	{
		return _mountObjectID;
	}

	private ItemInstance _lure = null;

	public SkillUseHolder getCurrentSkill()
	{
		return _currentSkill;
	}

	public void setCurrentSkill(Skill currentSkill, boolean ctrlPressed, boolean shiftPressed)
	{
		if (currentSkill == null)
		{
			_currentSkill = null;
			return;
		}
		_currentSkill = new SkillUseHolder(currentSkill, ctrlPressed, shiftPressed);
	}

	public SkillUseHolder getCurrentPetSkill()
	{
		return _currentPetSkill;
	}

	public void setCurrentPetSkill(Skill currentSkill, boolean ctrlPressed, boolean shiftPressed)
	{
		if (currentSkill == null)
		{
			_currentPetSkill = null;
			return;
		}
		_currentPetSkill = new SkillUseHolder(currentSkill, ctrlPressed, shiftPressed);
	}

	public SkillUseHolder getQueuedSkill()
	{
		return _queuedSkill;
	}

	public void setQueuedSkill(Skill queuedSkill, boolean ctrlPressed, boolean shiftPressed)
	{
		if (queuedSkill == null)
		{
			_queuedSkill = null;
			return;
		}
		_queuedSkill = new SkillUseHolder(queuedSkill, ctrlPressed, shiftPressed);
	}

	public boolean isJailed()
	{
		return PunishmentManager.getInstance().checkPunishment(getClient(), PunishmentType.JAIL);
	}

	public boolean isChatBanned()
	{
		return PunishmentManager.getInstance().checkPunishment(getClient(), PunishmentType.CHAT_BAN);
	}

	public void startFameTask(long delay, int fameFixRate)
	{
		if ((getLevel() < 40) || (getClassId().level() < 2))
		{
			return;
		}
		getPersonalTasks().addTask(new FameTask(delay, fameFixRate));
	}

	public void stopFameTask()
	{
		getPersonalTasks().removeTask(1, true);
	}

	public int getPowerGrade()
	{
		return _powerGrade;
	}

	public void setPowerGrade(int power)
	{
		_powerGrade = power;
	}

	public boolean isCursedWeaponEquipped()
	{
		return _cursedWeaponEquippedId != 0;
	}

	public void setCursedWeaponEquippedId(int value)
	{
		_cursedWeaponEquippedId = value;
	}

	public int getCursedWeaponEquippedId()
	{
		return _cursedWeaponEquippedId;
	}

	public boolean isCombatFlagEquipped()
	{
		return _combatFlagEquippedId;
	}

	public void setCombatFlagEquipped(boolean value)
	{
		_combatFlagEquippedId = value;
	}
	
	public void setIsWithoutCursed(boolean val)
	{
		_isWithoutCursed = val;
	}
	
	public boolean isWithoutCursed()
	{
		return _isWithoutCursed;
	}

	public int getChargedSouls()
	{
		return _souls;
	}

	public void increaseSouls(int count)
	{
		_souls += count;
		final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.YOUR_SOUL_HAS_INCREASED_BY_S1_SO_IT_IS_NOW_AT_S2);
		sm.addNumber(count);
		sm.addNumber(_souls);
		sendPacket(sm);
		restartSoulTask();
		sendPacket(new EtcStatusUpdate(this));
	}

	public boolean decreaseSouls(int count, Skill skill)
	{
		_souls -= count;

		if (getChargedSouls() < 0)
		{
			_souls = 0;
		}

		if (getChargedSouls() == 0)
		{
			getPersonalTasks().removeTask(6, true);
		}
		else
		{
			restartSoulTask();
		}
		sendPacket(new EtcStatusUpdate(this));
		return true;
	}

	public void clearSouls()
	{
		_souls = 0;
		getPersonalTasks().removeTask(6, true);
		sendPacket(new EtcStatusUpdate(this));
	}

	private void restartSoulTask()
	{
		getPersonalTasks().removeTask(6, false);
		getPersonalTasks().addTask(new ResetSoulsTask(600000L));
	}

	public int getDeathPenaltyBuffLevel()
	{
		return _deathPenaltyBuffLevel;
	}

	public void setDeathPenaltyBuffLevel(int level)
	{
		_deathPenaltyBuffLevel = level;
	}

	public void calculateDeathPenaltyBuffLevel(Creature killer)
	{
		if (killer == null)
		{
			return;
		}
		
		if (isInFightEvent() || ((AerialCleftEvent.getInstance().isStarted() || AerialCleftEvent.getInstance().isRewarding()) && AerialCleftEvent.getInstance().isPlayerParticipant(getObjectId())))
		{
			return;
		}
		
		if ((isCharmOfLuckAffected() && killer.isRaid()) || getNevitSystem().isBlessingActive() || isPhoenixBlessed() || isLucky() || isInsideZone(ZoneId.PVP) || isInsideZone(ZoneId.SIEGE) || canOverrideCond(PcCondOverride.DEATH_PENALTY))
		{
			return;
		}
		double percent = 1.0;
		
		if (killer.isRaid())
		{
			percent *= calcStat(Stats.REDUCE_DEATH_PENALTY_BY_RAID, 1, null, null);
		}
		else if (killer.isMonster())
		{
			percent *= calcStat(Stats.REDUCE_DEATH_PENALTY_BY_MOB, 1, null, null);
		}
		else if (killer.isPlayable())
		{
			percent *= calcStat(Stats.REDUCE_DEATH_PENALTY_BY_PVP, 1, null, null);
		}
		
		if (Rnd.get(1, 100) <= ((Config.DEATH_PENALTY_CHANCE) * percent))
		{
			if (!killer.isPlayable() || (getKarma() > 0))
			{
				increaseDeathPenaltyBuffLevel();
			}
		}
	}

	public void increaseDeathPenaltyBuffLevel()
	{
		if (getDeathPenaltyBuffLevel() >= 15 || isInFightEvent())
		{
			return;
		}

		if (getDeathPenaltyBuffLevel() != 0)
		{
			final Skill skill = SkillsParser.getInstance().getInfo(5076, getDeathPenaltyBuffLevel());

			if (skill != null)
			{
				removeSkill(skill, true);
			}
		}

		_deathPenaltyBuffLevel++;

		addSkill(SkillsParser.getInstance().getInfo(5076, getDeathPenaltyBuffLevel()), false);
		sendPacket(new EtcStatusUpdate(this));
		final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.DEATH_PENALTY_LEVEL_S1_ADDED);
		sm.addNumber(getDeathPenaltyBuffLevel());
		sendPacket(sm);
	}

	public void reduceDeathPenaltyBuffLevel()
	{
		if (getDeathPenaltyBuffLevel() <= 0)
		{
			return;
		}

		final Skill skill = SkillsParser.getInstance().getInfo(5076, getDeathPenaltyBuffLevel());

		if (skill != null)
		{
			removeSkill(skill, true);
		}

		_deathPenaltyBuffLevel--;

		if (getDeathPenaltyBuffLevel() > 0)
		{
			addSkill(SkillsParser.getInstance().getInfo(5076, getDeathPenaltyBuffLevel()), false);
			sendPacket(new EtcStatusUpdate(this));
			final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.DEATH_PENALTY_LEVEL_S1_ADDED);
			sm.addNumber(getDeathPenaltyBuffLevel());
			sendPacket(sm);
		}
		else
		{
			sendPacket(new EtcStatusUpdate(this));
			sendPacket(SystemMessageId.DEATH_PENALTY_LIFTED);
		}
	}

	public void restoreDeathPenaltyBuffLevel()
	{
		if (getDeathPenaltyBuffLevel() > 0)
		{
			addSkill(SkillsParser.getInstance().getInfo(5076, getDeathPenaltyBuffLevel()), false);
		}
	}

	@Override
	public void addTimeStampItem(ItemInstance item, long reuse, boolean byCron)
	{
		_reuseTimeStampsItems.put(item.getObjectId(), new TimeStamp(item, reuse, byCron));
	}

	public void addTimeStampItem(ItemInstance item, long reuse, long systime)
	{
		_reuseTimeStampsItems.put(item.getObjectId(), new TimeStamp(item, reuse, systime));
	}
	
	public Map<Integer, TimeStamp> getItemRemainingReuseTime()
	{
		return _reuseTimeStampsItems;
	}

	@Override
	public long getItemRemainingReuseTime(int itemObjId)
	{
		if (!_reuseTimeStampsItems.containsKey(itemObjId))
		{
			return -1;
		}
		return _reuseTimeStampsItems.get(itemObjId).getRemaining();
	}

	public long getReuseDelayOnGroup(int group)
	{
		if (group > 0)
		{
			for (final TimeStamp ts : _reuseTimeStampsItems.values())
			{
				if ((ts.getSharedReuseGroup() == group) && ts.hasNotPassed())
				{
					return ts.getRemaining();
				}
			}
		}
		return 0;
	}

	public TimeStamp getSharedItemReuse(int itemObjId)
	{
		return _reuseTimeStampsItems.get(itemObjId);
	}

	public Map<Integer, TimeStamp> getSkillReuseTimeStamps()
	{
		return _reuseTimeStampsSkills;
	}
	
	public void clenaUpSkillReuseTimeStamps()
	{
		if (!_reuseTimeStampsSkills.isEmpty())
		{
			for (final var entry : _reuseTimeStampsSkills.entrySet())
			{
				final var skillId = entry.getKey();
				final var skill = SkillsParser.getInstance().getSkill(skillId);
				if (skill != null && !skill.isBlockResetReuse())
				{
					_reuseTimeStampsSkills.remove(skillId);
				}
			}
		}
	}
	
	public void clenaUpItemReuseTimeStamps()
	{
		if (!_reuseTimeStampsItems.isEmpty())
		{
			for (final var entry : _reuseTimeStampsItems.entrySet())
			{
				final var objectId = entry.getKey();
				final var item = getInventory().getItemByObjectId(objectId);
				if (item != null && !item.isBlockResetReuse())
				{
					_reuseTimeStampsSkills.remove(objectId);
				}
			}
		}
	}

	@Override
	public long getSkillRemainingReuseTime(int skillReuseHashId)
	{
		if (!_reuseTimeStampsSkills.containsKey(skillReuseHashId))
		{
			return -1;
		}
		return _reuseTimeStampsSkills.get(skillReuseHashId).getRemaining();
	}

	public boolean hasSkillReuse(int skillReuseHashId)
	{
		if (!_reuseTimeStampsSkills.containsKey(skillReuseHashId))
		{
			return false;
		}
		return _reuseTimeStampsSkills.get(skillReuseHashId).hasNotPassed();
	}

	public TimeStamp getSkillReuseTimeStamp(int skillReuseHashId)
	{
		return _reuseTimeStampsSkills.get(skillReuseHashId);
	}

	@Override
	public void addTimeStamp(Skill skill, long reuse)
	{
		_reuseTimeStampsSkills.put(skill.getReuseHashCode(), new TimeStamp(skill, reuse));
	}

	@Override
	public void addTimeStamp(Skill skill, long reuse, long systime)
	{
		_reuseTimeStampsSkills.put(skill.getReuseHashCode(), new TimeStamp(skill, reuse, systime));
	}

    @Override
    public boolean isRegisteredInFightEvent() {
        return super.isRegisteredInFightEvent() || FightEventManager.getInstance().isPlayerRegistered(this);
    }

	public void resetReuse()
	{
		resetDisabledSkills();
		clenaUpSkillReuseTimeStamps();
		clenaUpItemReuseTimeStamps();
	}

	@Override
	public Player getActingPlayer()
	{
		return this;
	}

	@Override
	public final void sendDamageMessage(Creature target, int damage, Skill skill, boolean mcrit, boolean pcrit, boolean miss)
	{
		if (miss)
		{
			if (target.isPlayer())
			{
				final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_EVADED_C2_ATTACK);
				sm.addPcName(target.getActingPlayer());
				sm.addCharName(this);
				target.sendPacket(sm);
			}
			final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_ATTACK_WENT_ASTRAY);
			sm.addPcName(this);
			sendPacket(sm);
			return;
		}

		if (pcrit)
		{
			final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_HAD_CRITICAL_HIT);
			sm.addPcName(this);
			sendPacket(sm);
		}
		if (mcrit)
		{
			sendPacket(SystemMessageId.CRITICAL_HIT_MAGIC);
		}

		if (isInOlympiadMode() && target.isPlayer() && !target.isInvul() && (target.getActingPlayer().isInOlympiadMode() && (target.getActingPlayer().getOlympiadGameId() == getOlympiadGameId())))
		{
			OlympiadGameManager.getInstance().notifyCompetitorDamage(target.getActingPlayer(), damage);
		}

		if(isInPartyTournament() && target.isInPartyTournament())
		{
			getTournamentStats().addTournamentMatchDamage(damage);
		}

		if (Config.ALLOW_DAMAGE_LIMIT && target.isNpc())
		{
			final DamageLimit limit = DamageLimitParser.getInstance().getDamageLimit(target.getId());
			if (limit != null)
			{
				final int damageLimit = skill != null ? skill.isMagic() ? limit.getMagicDamage() : limit.getPhysicDamage() : limit.getDamage();
				if (damageLimit > 0 && damage > damageLimit)
				{
					damage = damageLimit;
				}
			}
		}

		final SystemMessage sm;

		if (target.isInvul())
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.ATTACK_WAS_BLOCKED);
		}
		else if (target.isDoor() || (target instanceof ControlTowerInstance))
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.YOU_DID_S1_DMG);
			sm.addNumber(damage);
		}
		else
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.C1_DONE_S3_DAMAGE_TO_C2);
			sm.addPcName(this);
			sm.addCharName(target);
			sm.addNumber(damage);
		}
		sendPacket(sm);
	}

	public void setAgathionId(int npcId)
	{
		_agathionId = npcId;
	}

	public int getAgathionId()
	{
		return _agathionId;
	}

	public int getVitalityPoints()
	{
		return getStat().getVitalityPoints();
	}

	public int getVitalityLevel()
	{
		return getStat().getVitalityLevel();
	}

	public void setVitalityPoints(int points, boolean quiet)
	{
		getStat().setVitalityPoints(points, quiet);
	}

	public void updateVitalityPoints(double vitalityPoints, boolean useRates, boolean quiet)
	{
		getStat().updateVitalityPoints(vitalityPoints, useRates, quiet);
	}

	public void checkItemRestriction()
	{
		for (int i = 0; i < Inventory.PAPERDOLL_TOTALSLOTS; i++)
		{
			final ItemInstance equippedItem = getInventory().getPaperdollItem(i);
			if (equippedItem != null)
			{
				if (!equippedItem.getItem().checkCondition(this, this, false) || (isInOlympiadMode() && equippedItem.isOlyRestrictedItem()) || (isInFightEvent() && equippedItem.isEventRestrictedItem()))
				{
					getInventory().unEquipItemInSlot(i);

					final InventoryUpdate iu = new InventoryUpdate();
					iu.addModifiedItem(equippedItem);
					sendPacket(iu);

					SystemMessage sm = null;
					if (equippedItem.getItem().getBodyPart() == Item.SLOT_BACK)
					{
						sendPacket(SystemMessageId.CLOAK_REMOVED_BECAUSE_ARMOR_SET_REMOVED);
						return;
					}

					if (equippedItem.getEnchantLevel() > 0)
					{
						sm = SystemMessage.getSystemMessage(SystemMessageId.EQUIPMENT_S1_S2_REMOVED);
						sm.addNumber(equippedItem.getEnchantLevel());
						sm.addItemName(equippedItem);
					}
					else
					{
						sm = SystemMessage.getSystemMessage(SystemMessageId.S1_DISARMED);
						sm.addItemName(equippedItem);
					}
					sendPacket(sm);
				}
			}
		}
	}

	public void addTransformSkill(int id)
	{
		if (_transformAllowedSkills == null)
		{
			synchronized (this)
			{
				if (_transformAllowedSkills == null)
				{
					_transformAllowedSkills = new HashSet<>();
				}
			}
		}
		_transformAllowedSkills.add(id);
	}
	
	public void addTransformNotCheckSkill(int id)
	{
		_transformNotCheckSkills.add(id);
	}
	
	public Set<Integer> getTransformNotCheckSkills()
	{
		return _transformNotCheckSkills;
	}

	public boolean hasTransformSkill(int id)
	{
		return (_transformAllowedSkills != null) && _transformAllowedSkills.contains(id);
	}
	
	public Set<Integer> getTransformSkills()
	{
		return _transformAllowedSkills;
	}

	public synchronized void removeAllTransformSkills()
	{
		_transformAllowedSkills = null;
		_transformNotCheckSkills.clear();
	}

	protected void startFeed(int npcId)
	{
		_canFeed = npcId > 0;
		if (!isMounted())
		{
			return;
		}
		if (hasSummon())
		{
			setCurrentFeed(((PetInstance) getSummon()).getCurrentFed());
			_controlItemId = getSummon().getControlObjectId();
			sendPacket(new SetupGauge(this, 3, (getCurrentFeed() * 10000) / getFeedConsume(), (getMaxFeed() * 10000) / getFeedConsume()));
			if (!isDead())
			{
				getPersonalTasks().addTask(new PetFeedTask(10000L));
			}
		}
		else if (_canFeed)
		{
			setCurrentFeed(getMaxFeed());
			final var sg = new SetupGauge(this, 3, (getCurrentFeed() * 10000) / getFeedConsume(), (getMaxFeed() * 10000) / getFeedConsume());
			sendPacket(sg);
			if (!isDead())
			{
				getPersonalTasks().addTask(new PetFeedTask(10000L));
			}
		}
	}

	private final void clearPetData()
	{
		_data = null;
	}

	public final PetData getPetData(int npcId)
	{
		if (_data == null)
		{
			_data = PetsParser.getInstance().getPetData(npcId);
		}
		return _data;
	}

	private final PetLevelTemplate getPetLevelData(int npcId)
	{
		if (_leveldata == null)
		{
			_leveldata = PetsParser.getInstance().getPetData(npcId).getPetLevelData(getMountLevel());
		}
		return _leveldata;
	}

	public int getCurrentFeed()
	{
		return _curFeed;
	}

	public int getFeedConsume()
	{
		if (isAttackingNow())
		{
			return getPetLevelData(_mountNpcId).getPetFeedBattle();
		}
		return getPetLevelData(_mountNpcId).getPetFeedNormal();
	}

	public void setCurrentFeed(int num)
	{
		final boolean lastHungryState = isHungry();
		_curFeed = num > getMaxFeed() ? getMaxFeed() : num;
		final SetupGauge sg = new SetupGauge(this, 3, (getCurrentFeed() * 10000) / getFeedConsume(), (getMaxFeed() * 10000) / getFeedConsume());
		sendPacket(sg);
		if (lastHungryState != isHungry())
		{
			broadcastUserInfo(true);
		}
	}

	private int getMaxFeed()
	{
		return getPetLevelData(_mountNpcId).getPetMaxFeed();
	}

	public boolean isHungry()
	{
		return _canFeed ? (getCurrentFeed() < ((getPetData(getMountNpcId()).getHungryLimit() / 100f) * getPetLevelData(getMountNpcId()).getPetMaxFeed())) : false;
	}

	public void enteredNoLanding(int delay)
	{
		getPersonalTasks().addTask(new DismountTask(delay * 1000L));
	}

	public void exitedNoLanding()
	{
		getPersonalTasks().removeTask(7, true);
	}

	public void storePetFood(int petId)
	{
		if ((_controlItemId != 0) && (petId != 0))
		{
			Connection con = null;
			PreparedStatement statement = null;
			try
			{
				con = DatabaseFactory.getInstance().getConnection();
				statement = con.prepareStatement("UPDATE pets SET fed=? WHERE item_obj_id = ?");
				statement.setInt(1, getCurrentFeed());
				statement.setInt(2, _controlItemId);
				statement.executeUpdate();
				_controlItemId = 0;
			}
			catch (final Exception e)
			{
				_log.warn("Failed to store Pet [NpcId: " + petId + "] data", e);
			}
			finally
			{
				DbUtils.closeQuietly(con, statement);
			}
		}
	}

	public void setIsInSiege(boolean b)
	{
		_isInSiege = b;
	}

	public boolean isInSiege()
	{
		return _isInSiege;
	}

	public void setIsInHideoutSiege(boolean isInHideoutSiege)
	{
		_isInHideoutSiege = isInHideoutSiege;
	}

	public boolean isInHideoutSiege()
	{
		return _isInHideoutSiege;
	}

	public void setIsInPvpFunZone(boolean isInPvpFunZone)
	{
		_isInPvpFunZone = isInPvpFunZone;
	}

	public boolean isInPvpFunZone()
	{
		return _isInPvpFunZone;
	}

	public boolean isFlyingMounted()
	{
		return (isTransformed() && (getTransformation().isFlying()));
	}

	public int getCharges()
	{
		return _charges.get();
	}

	public void increaseCharges(int count, int max)
	{
		if (_charges.get() >= max)
		{
			sendPacket(SystemMessageId.FORCE_MAXLEVEL_REACHED);
			return;
		}
		restartChargeTask();

		if (_charges.addAndGet(count) >= max)
		{
			_charges.set(max);
			sendPacket(SystemMessageId.FORCE_MAXLEVEL_REACHED);
		}
		else
		{
			final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.FORCE_INCREASED_TO_S1);
			sm.addNumber(_charges.get());
			sendPacket(sm);
		}
		sendPacket(new EtcStatusUpdate(this));
	}

	public boolean decreaseCharges(int count)
	{
		if (_charges.get() < count)
		{
			return false;
		}

		if (_charges.addAndGet(-count) == 0)
		{
			getPersonalTasks().removeTask(5, true);
		}
		else
		{
			restartChargeTask();
		}

		sendPacket(new EtcStatusUpdate(this));
		return true;
	}

	public void clearCharges()
	{
		_charges.set(0);
		sendPacket(new EtcStatusUpdate(this));
	}

	private void restartChargeTask()
	{
		getPersonalTasks().removeTask(5, false);
		getPersonalTasks().addTask(new ResetChargesTask(600000L));
	}

	public void teleportBookmarkModify(int id, int icon, String tag, String name)
	{
		final BookmarkTemplate bookmark = _tpbookmarks.get(id);
		if (bookmark != null)
		{
			bookmark.setIcon(icon);
			bookmark.setTag(tag);
			bookmark.setName(name);
			CharacterBookMarkDAO.getInstance().update(this, id, icon, tag, name);
		}
		sendPacket(new ExGetBookMarkInfo(this));
	}

	public void teleportBookmarkDelete(int id)
	{
		if (_tpbookmarks.remove(id) != null)
		{
			CharacterBookMarkDAO.getInstance().delete(this, id);
			sendPacket(new ExGetBookMarkInfo(this));
		}
	}

	public void teleportBookmarkGo(int id)
	{
		if (!teleportBookmarkCondition(0))
		{
			return;
		}
		
		final Skill sk = SkillsParser.getInstance().getInfo(2588, 1);
		if (sk == null || !checkDoCastConditions(sk, false) || isCastingNow())
		{
			if (!isCastingNow())
			{
				_bookmarkLocation = null;
			}
			return;
		}
		
		int itemId = 0;
		if (getInventory().getInventoryItemCount(13016, 0) > 0)
		{
			itemId = 13016;
		}
		else if (getInventory().getInventoryItemCount(13302, 0) > 0)
		{
			itemId = 13302;
		}
		else if (getInventory().getInventoryItemCount(20025, 0) > 0)
		{
			itemId = 20025;
		}
		else
		{
			sendPacket(SystemMessageId.YOU_CANNOT_TELEPORT_BECAUSE_YOU_DO_NOT_HAVE_A_TELEPORT_ITEM);
			return;
		}
		
		final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_DISAPPEARED);
		sm.addItemName(itemId);
		sendPacket(sm);

		final BookmarkTemplate bookmark = _tpbookmarks.get(id);
		if (bookmark != null)
		{
			destroyItem("Consume", getInventory().getItemByItemId(itemId).getObjectId(), 1, null, false);
			_bookmarkLocation = bookmark;
			useMagic(sk, false, false, false);
		}
		sendPacket(new ExGetBookMarkInfo(this));
	}

	public boolean teleportBookmarkCondition(int type)
	{
		if (isInCombat())
		{
			sendPacket(SystemMessageId.YOU_CANNOT_USE_MY_TELEPORTS_DURING_A_BATTLE);
			return false;
		}
		else if (isInSiege() || (getSiegeState() != 0))
		{
			sendPacket(SystemMessageId.YOU_CANNOT_USE_MY_TELEPORTS_WHILE_PARTICIPATING);
			return false;
		}
		else if (isInDuel())
		{
			sendPacket(SystemMessageId.YOU_CANNOT_USE_MY_TELEPORTS_DURING_A_DUEL);
			return false;
		}
		else if (isFlying())
		{
			sendPacket(SystemMessageId.YOU_CANNOT_USE_MY_TELEPORTS_WHILE_FLYING);
			return false;
		}
		else if (isInOlympiadMode())
		{
			sendPacket(SystemMessageId.YOU_CANNOT_USE_MY_TELEPORTS_WHILE_PARTICIPATING_IN_AN_OLYMPIAD_MATCH);
			return false;
		}
		else if (isParalyzed())
		{
			sendPacket(SystemMessageId.YOU_CANNOT_USE_MY_TELEPORTS_WHILE_YOU_ARE_PARALYZED);
			return false;
		}
		else if (isDead())
		{
			sendPacket(SystemMessageId.YOU_CANNOT_USE_MY_TELEPORTS_WHILE_YOU_ARE_DEAD);
			return false;
		}
		else if ((type == 1) && (isIn7sDungeon() || (isInParty() && getParty().isInDimensionalRift())))
		{
			sendPacket(SystemMessageId.YOU_CANNOT_USE_MY_TELEPORTS_TO_REACH_THIS_AREA);
			return false;
		}
		else if (isInWater())
		{
			sendPacket(SystemMessageId.YOU_CANNOT_USE_MY_TELEPORTS_UNDERWATER);
			return false;
		}
		else if ((type == 1) && (isInsideZone(ZoneId.SIEGE) || isInsideZone(ZoneId.CLAN_HALL) || isInsideZone(ZoneId.JAIL) || isInsideZone(ZoneId.CASTLE) || isInsideZone(ZoneId.NO_SUMMON_FRIEND) || isInsideZone(ZoneId.FORT)))
		{
			sendPacket(SystemMessageId.YOU_CANNOT_USE_MY_TELEPORTS_TO_REACH_THIS_AREA);
			return false;
		}
		else if (isInsideZone(ZoneId.NO_BOOKMARK) || isInBoat() || isInAirShip())
		{
			if (type == 0)
			{
				sendPacket(SystemMessageId.YOU_CANNOT_USE_MY_TELEPORTS_IN_THIS_AREA);
			}
			else if (type == 1)
			{
				sendPacket(SystemMessageId.YOU_CANNOT_USE_MY_TELEPORTS_TO_REACH_THIS_AREA);
			}
			return false;
		}
		else
		{
			return true;
		}
	}

	public void teleportBookmarkAdd(int x, int y, int z, int icon, String tag, String name)
	{
		if (!teleportBookmarkCondition(1))
		{
			return;
		}

		if (_tpbookmarks.size() >= _bookmarkslot)
		{
			sendPacket(SystemMessageId.YOU_HAVE_NO_SPACE_TO_SAVE_THE_TELEPORT_LOCATION);
			return;
		}

		if (getInventory().getInventoryItemCount(20033, 0) == 0)
		{
			sendPacket(SystemMessageId.YOU_CANNOT_BOOKMARK_THIS_LOCATION_BECAUSE_YOU_DO_NOT_HAVE_A_MY_TELEPORT_FLAG);
			return;
		}

		int id;
		for (id = 1; id <= _bookmarkslot; ++id)
		{
			if (!_tpbookmarks.containsKey(id))
			{
				break;
			}
		}

		_tpbookmarks.put(id, new BookmarkTemplate(id, x, y, z, icon, tag, name));

		destroyItem("Consume", getInventory().getItemByItemId(20033).getObjectId(), 1, null, false);

		final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_DISAPPEARED);
		sm.addItemName(20033);
		sendPacket(sm);

		CharacterBookMarkDAO.getInstance().add(this, id, x, y, z, icon, tag, name);
		sendPacket(new ExGetBookMarkInfo(this));
	}

	@Override
	public void sendInfo(Player activeChar)
	{
		if (activeChar.getNotShowTraders() && isInStoreNow())
		{
			return;
		}
		
		activeChar.sendPacket(isPolymorphed() ? new NpcInfoPoly(this, activeChar) : new CharInfo(this, activeChar), new ExBrExtraUserInfo(this));
		if (isInBoat())
		{
			activeChar.sendPacket(new GetOnVehicle(getObjectId(), getBoat().getObjectId(), getInVehiclePosition()));
		}
		else if (isInAirShip())
		{
			activeChar.sendPacket(new ExGetOnAirShip(this, getAirShip()));
		}
		else
		{
			if (isMoving())
			{
				activeChar.sendPacket(new MoveToLocation(this));
			}
		}
		
		if (isSitting() && (getSittingObject() != null))
		{
			activeChar.sendPacket(new ChairSit(this, _sittingObject.getId()));
		}
		
		activeChar.sendPacket(RelationChanged.update(activeChar, this, activeChar));

		switch (getPrivateStoreType())
		{
			case Player.STORE_PRIVATE_SELL :
				activeChar.sendPacket(new PrivateStoreSellMsg(this));
				break;
			case Player.STORE_PRIVATE_PACKAGE_SELL :
				activeChar.sendPacket(new ExPrivateStorePackageMsg(this));
				break;
			case Player.STORE_PRIVATE_BUY :
				activeChar.sendPacket(new PrivateStoreBuyMsg(this));
				break;
			case Player.STORE_PRIVATE_MANUFACTURE :
				activeChar.sendPacket(new RecipeShopMsg(this));
				break;
		}
		
		if (activeChar.isInZonePeace())
		{
			return;
		}
		
		if (isCastingNow())
		{
			final Creature castingTarget = getCastingTarget();
			final var castingSkill = getCastingSkill();
			final long animationEndTime = getAnimationEndTime();
			if ((castingSkill != null) && (castingTarget != null) && (getAnimationEndTime() > 0))
			{
				activeChar.sendPacket(new MagicSkillUse(this, castingTarget, castingSkill.getId(), castingSkill.getLevel(), (int) (animationEndTime - System.currentTimeMillis()), 0));
			}
		}
		
		if (isInCombat())
		{
			activeChar.sendPacket(new AutoAttackStart(getObjectId()));
		}
		
		final GameServerPacket dominion = TerritoryWarManager.getInstance().isTWInProgress() && (TerritoryWarManager.getInstance().checkIsRegistered(-1, getObjectId()) || TerritoryWarManager.getInstance().checkIsRegistered(-1, getClan())) ? new ExDominionWarStart(this) : null;
		if (dominion != null)
		{
			activeChar.sendPacket(dominion);
		}
	}

	public void showQuestMovie(int id)
	{
		if (_movieId > 0)
		{
			return;
		}
		abortAttack();
		abortCast();
		stopMove(null);
		_movieId = id;
		sendPacket(new ExStartScenePlayer(id));
	}

	public boolean isAllowedToEnchantSkills()
	{
		if (isLocked())
		{
			return false;
		}
		if (isTransformed() || isInStance())
		{
			return false;
		}
		if (AttackStanceTaskManager.getInstance().hasAttackStanceTask(this))
		{
			return false;
		}
		if (isCastingNow() || isCastingSimultaneouslyNow())
		{
			return false;
		}
		if (isInBoat() || isInAirShip())
		{
			return false;
		}
		return true;
	}

	public void setCreateDate(long createDate)
	{
		_createDate = createDate;
	}

	public long getCreateDate()
	{
		return _createDate;
	}

	public void checkBirthDay()
	{
		final var create = Calendar.getInstance();
		create.setTimeInMillis(getCreateDate());
		final var now = Calendar.getInstance();
		int day = create.get(Calendar.DAY_OF_MONTH);
		if (create.get(Calendar.MONTH) == Calendar.FEBRUARY && day == 29)
		{
			day = 28;
		}
		
		final int myBirthdayReceiveYear = getVarInt("MyBirthdayReceiveYear", 0);
		if (create.get(Calendar.MONTH) == now.get(Calendar.MONTH) && create.get(Calendar.DAY_OF_MONTH) == day)
		{
			if ((myBirthdayReceiveYear == 0 && create.get(Calendar.YEAR) != now.get(Calendar.YEAR)) || myBirthdayReceiveYear > 0 && myBirthdayReceiveYear != now.get(Calendar.YEAR))
			{
				final int age = now.get(Calendar.YEAR) - create.get(Calendar.YEAR);
				if (age <= 0)
				{
					return;
				}
				
				String text = Config.ALT_BIRTHDAY_MAIL_TEXT;
				if (text.contains("$c1"))
				{
					text = text.replace("$c1", getName(null));
				}
				if (text.contains("$s1"))
				{
					text = text.replace("$s1", String.valueOf(age));
				}
				
				final var msg = new Message(getObjectId(), Config.ALT_BIRTHDAY_MAIL_SUBJECT, text, Message.SenderType.BIRTHDAY);
				final var attachments = msg.createAttachments();
				attachments.addItem("Birthday", Config.ALT_BIRTHDAY_GIFT, 1, null, null);
				MailManager.getInstance().sendMessage(msg);
				sendPacket(SystemMessageId.YOUR_BIRTHDAY_GIFT_HAS_ARRIVED);
				setVar("MyBirthdayReceiveYear", String.valueOf(now.get(Calendar.YEAR)), -1);
			}
		}
	}
	
	private final List<Integer> _friendList = new CopyOnWriteArrayList<>();

	public List<Integer> getFriendList()
	{
		return _friendList;
	}

	public void restoreFriendList()
	{
		_friendList.clear();

		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT friendId FROM character_friends WHERE charId=? AND relation=0");
			statement.setInt(1, getObjectId());
			int friendId;
			rset = statement.executeQuery();
			while (rset.next())
			{
				friendId = rset.getInt("friendId");
				if (friendId == getObjectId())
				{
					continue;
				}
				_friendList.add(friendId);
			}
		}
		catch (final Exception e)
		{
			_log.warn("Error found in " + getName(null) + "'s FriendList: " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
	}

	protected void notifyFriends()
	{
		final L2FriendStatus pkt = new L2FriendStatus(getObjectId());
		for (final int id : _friendList)
		{
			final Player friend = GameObjectsStorage.getPlayer(id);
			if (friend != null)
			{
				friend.sendPacket(pkt);
			}
		}
	}

	public boolean isSilenceMode()
	{
		return _silenceMode;
	}

	public boolean isSilenceMode(int playerObjId)
	{
		if (Config.SILENCE_MODE_EXCLUDE && _silenceMode && (_silenceModeExcluded != null))
		{
			return !_silenceModeExcluded.contains(playerObjId);
		}
		return _silenceMode;
	}

	public void setSilenceMode(boolean mode)
	{
		_silenceMode = mode;
		if (_silenceModeExcluded != null)
		{
			_silenceModeExcluded.clear();
		}
		sendPacket(new EtcStatusUpdate(this));
	}

	public void addSilenceModeExcluded(int playerObjId)
	{
		if (_silenceModeExcluded == null)
		{
			_silenceModeExcluded = new ArrayList<>(1);
		}
		_silenceModeExcluded.add(playerObjId);
	}

	private void storeRecipeShopList()
	{
		if (hasManufactureShop())
		{
			Connection con = null;
			PreparedStatement st = null;
			try
			{
				con = DatabaseFactory.getInstance().getConnection();
				st = con.prepareStatement("DELETE FROM character_recipeshoplist WHERE charId=?");
				st.setInt(1, getObjectId());
				st.execute();
				st.close();
				
				st = con.prepareStatement("INSERT INTO character_recipeshoplist (`charId`, `recipeId`, `price`, `index`) VALUES (?, ?, ?, ?)");
				int i = 1;
				for (final ManufactureItemTemplate item : _manufactureItems.values())
				{
					st.setInt(1, getObjectId());
					st.setInt(2, item.getRecipeId());
					st.setLong(3, item.getCost());
					st.setInt(4, i++);
					st.addBatch();
				}
				st.executeBatch();
			}
			catch (final Exception e)
			{
				_log.warn("Could not store recipe shop for playerId " + getObjectId() + ": ", e);
			}
			finally
			{
				DbUtils.closeQuietly(con, st);
			}
		}
	}

	private void restoreRecipeShopList()
	{
		if (_manufactureItems != null)
		{
			_manufactureItems.clear();
		}

		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT * FROM character_recipeshoplist WHERE charId=? ORDER BY `index`");
			statement.setInt(1, getObjectId());
			rset = statement.executeQuery();
			while (rset.next())
			{
				getManufactureItems().put(rset.getInt("recipeId"), new ManufactureItemTemplate(rset.getInt("recipeId"), rset.getLong("price")));
			}
		}
		catch (final Exception e)
		{
			_log.warn("Could not restore recipe shop list data for playerId: " + getObjectId(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
	}
	
	public final boolean isFalling(int z)
	{
		if (isDead() || isFlying() || isInWater(this) || isInVehicle())
		{
			return false;
		}
		
		if (isFalling())
		{
			return true;
		}
		
		final double deltaZ = Math.abs(getZ() - z);
		if (deltaZ <= getBaseTemplate().getSafeFallHeight())
		{
			return false;
		}
		
		if (!GeoEngine.getInstance().hasGeo(getX(), getY()))
		{
			return false;
		}
		
		if (Config.ENABLE_FALLING_DAMAGE)
		{
			final int damage = (int) calcStat(Stats.FALL, (deltaZ * getMaxHp()) / 2100., null, null);
			if (damage > 0)
			{
				getPersonalTasks().addTask(new FallingTask(5000L, damage));
			}
		}
		
		setFalling();
		
		return false;
	}
	
	@Override
	public boolean isFalling()
	{
		return System.currentTimeMillis() < _fallingTimestamp;
	}
	
	public final void setFalling()
	{
		_fallingTimestamp = System.currentTimeMillis() + 5000;
	}
	
	public int getMovieId()
	{
		return _movieId;
	}

	public void setMovieId(int id)
	{
		_movieId = id;
	}

	public void updateLastItemAuctionRequest()
	{
		_lastItemAuctionInfoRequest = System.currentTimeMillis();
	}

	public boolean isItemAuctionPolling()
	{
		return (System.currentTimeMillis() - _lastItemAuctionInfoRequest) < 2000;
	}

	@Override
	public boolean isMovementDisabled()
	{
		return super.isMovementDisabled() || (_movieId > 0);
	}

	public String getLang()
	{
		return _lang != null && !_lang.isEmpty() ? _lang : getDefaultLang();
	}
	
	private String getDefaultLang()
	{
		if (_client != null)
		{
			final var lang = Language.getById(_client.getLang());
			if (lang != null && Config.MULTILANG_ALLOWED.contains(lang))
			{
				setLang(lang);
				return lang;
			}
		}
		setLang(Config.MULTILANG_DEFAULT);
		return Config.MULTILANG_DEFAULT;
	}
	
	public String getLang(GameClient client)
	{
		if (client != null)
		{
			final var lang = Language.getById(client.getLang());
			if (lang != null && Config.MULTILANG_ALLOWED.contains(lang))
			{
				setLang(lang);
				return lang;
			}
		}
		setLang(Config.MULTILANG_DEFAULT);
		return Config.MULTILANG_DEFAULT;
	}

	public void setLang(String lang)
	{
		_lang = lang;
	}

	public boolean getUseAutoLoot()
	{
		return Config.AUTO_LOOT ? true : (getVarB("useAutoLoot@") || getStat().calcStat(Stats.AUTOLOOT, 0, null, null) > 0);
	}

	public boolean getUseAutoLootHerbs()
	{
		return Config.AUTO_LOOT_HERBS ? true : getVarB("useAutoLootHerbs@");
	}
	
	public boolean getBlockShiftClick()
	{
		return getVarB("shiftclick@");
	}
	
	public boolean isBusy()
	{
		return getVarB("busy@");
	}

	public boolean getExpOn()
	{
		return !getVarB("blockedEXP@");
	}

	public boolean getBlockBuffs()
	{
		return getVarB("useBlockBuffs@");
	}
	
	public boolean getNotShowTraders()
	{
		return getVarB("useHideTraders@");
	}
	
	public boolean getBlockPartyRecall()
	{
		return getVarB("useBlockPartyRecall@");
	}
	
	public boolean isAllowPrivateInventory()
	{
		return Config.ALLOW_PRIVATE_INVENTORY ? getVarB("privateInv@") : false;
	}
	
	public boolean getNotShowBuffsAnimation()
	{
		return getVarB("useHideBuffs@");
	}

	public boolean getTradeRefusal()
	{
		return getVarB("useBlockTrade@");
	}

	public boolean getPartyInviteRefusal()
	{
		return getVarB("useBlockParty@");
	}

	public boolean getFriendInviteRefusal()
	{
		return getVarB("useBlockFriend@");
	}
	
	public boolean isSkillChanceShow()
	{
		return Config.SKILL_CHANCE_SHOW ? getVarB("showSkillChance@") : false;
	}
	
	public boolean isUseAutoParty()
	{
		return getVarB("useAutoParty@");
	}
	public boolean isUseAutoTrade()
	{
		return getVarB("useAutoTrade@");
	}
	
	public boolean isHideTitles()
	{
		return getVarB("useHideTitle@");
	}

	public void setVar(String name, Object value)
	{
		setVar(name, String.valueOf(value), -1);
	}

	public void setVar(String name, Object value, long expirationTime)
	{
		setVar(name, String.valueOf(value), expirationTime);
	}

	public void setVar(String name, String value, long expirationTime)
	{
		final CharacterVariable var = new CharacterVariable(name, String.valueOf(value), expirationTime);
		if (CharacterVariablesDAO.getInstance().insert(getObjectId(), var))
		{
			_variables.put(name, var);
		}
	}
	
	public void unsetVar(String name)
	{
		if (name == null || name.isEmpty())
		{
			return;
		}
		
		if (_variables.containsKey(name) && CharacterVariablesDAO.getInstance().delete(getObjectId(), name))
		{
			_variables.remove(name);
		}
	}
	
	public CharacterVariable getVariable(String name)
	{
		final CharacterVariable var = _variables.get(name);
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
		final CharacterVariable var = _variables.get(name);
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

	public void restoreVariables()
	{
		final List<CharacterVariable> variables = CharacterVariablesDAO.getInstance().restore(getObjectId());
		for (final CharacterVariable var : variables)
		{
			_variables.put(var.getName(), var);
		}
		
		if (getVar("lang@") == null)
		{
			setVar("lang@", Config.MULTILANG_DEFAULT);
		}
		
		if (getVar("visualBlock") == null)
		{
			setVar("visualBlock", Config.ENABLE_VISUAL_BY_DEFAULT ? 0 : 1);
		}
	}

	public void removeFromBossZone()
	{
		try
		{
			for (final BossZone _zone : EpicBossManager.getInstance().getZones().values())
			{
				_zone.removePlayer(this);
			}
		}
		catch (final Exception e)
		{
			_log.warn("Exception on removeFromBossZone(): " + e.getMessage(), e);
		}
	}

	public void checkPlayerSkills()
	{
		SkillLearn learn;
		for (final Entry<Integer, Skill> e : getSkills().entrySet())
		{
			if (e.getValue().getLevel() > 99 && !Config.DECREASE_ENCHANT_SKILLS)
			{
				continue;
			}
			
			if (e.getValue().isBlockRemove())
			{
				continue;
			}
			
			final int checkLvl = e.getValue().getLevel() > 99 ? SkillsParser.getInstance().getMaxLevel(e.getKey()) : e.getValue().getLevel();
			learn = SkillTreesParser.getInstance().getClassSkill(e.getKey(), checkLvl, getClassId());
			if (learn != null)
			{
				final int lvlDiff = e.getKey() == Skill.SKILL_EXPERTISE ? 0 : 9;
				if (getLevel() < (e.getValue().getLevel() > 99 ? (e.getValue().getMagicLevel() - lvlDiff) : (learn.getGetLevel() - lvlDiff)))
				{
					deacreaseSkillLevel(e.getValue(), lvlDiff, e.getValue().getLevel() > 99);
				}
			}
		}
	}

	private void deacreaseSkillLevel(Skill skill, int lvlDiff, boolean isEnchant)
	{
		int nextLevel = -1;
		if (isEnchant)
		{
			for (int i = skill.getLevel(); i != 0; i--)
			{
				if (i < 100 || i > 130 && i < 200 || i > 230 && i < 300 || i > 330 && i < 400 || i > 430 && i < 500 || i > 530 && i < 600 || i > 630 && i < 700)
				{
					break;
				}
				
				final Skill newSkill = SkillsParser.getInstance().getInfo(skill.getId(), i);
				if (newSkill != null && newSkill.getMagicLevel() <= (skill.getMagicLevel() - lvlDiff))
				{
					nextLevel = newSkill.getLevel();
					break;
				}
			}
		}
		
		if (nextLevel == -1)
		{
			final Map<Integer, SkillLearn> skillTree = SkillTreesParser.getInstance().getCompleteClassSkillTree(getClassId());
			for (final SkillLearn sl : skillTree.values())
			{
				if ((sl.getId() == skill.getId()) && (nextLevel < sl.getLvl()) && (getLevel() >= (sl.getGetLevel() - lvlDiff)))
				{
					nextLevel = sl.getLvl();
				}
			}
		}

		if (nextLevel == -1)
		{
			if (Config.DEBUG)
			{
				_log.info("Removing skill " + skill + " from player " + toString());
			}
			removeSkill(skill, true);
		}
		else
		{
			if (Config.DEBUG)
			{
				_log.info("Decreasing skill " + skill + " to " + nextLevel + " for player " + toString());
			}
			addSkill(SkillsParser.getInstance().getInfo(skill.getId(), nextLevel), true);
		}
	}

	public boolean canMakeSocialAction()
	{
		return ((getPrivateStoreType() == Player.STORE_PRIVATE_NONE) && (getActiveRequester() == null) && !isAlikeDead() && !isAllSkillsDisabled() && !isInDuel() && !isCastingNow() && !isCastingSimultaneouslyNow() && (getAI().getIntention() == CtrlIntention.IDLE) && !AttackStanceTaskManager.getInstance().hasAttackStanceTask(this) && !isInOlympiadMode());
	}

	public void setMultiSocialAction(int id, int targetId)
	{
		_multiSociaAction = id;
		_multiSocialTarget = targetId;
	}

	public int getMultiSociaAction()
	{
		return _multiSociaAction;
	}

	public int getMultiSocialTarget()
	{
		return _multiSocialTarget;
	}

	public Collection<BookmarkTemplate> getTeleportBookmarks()
	{
		return _tpbookmarks.values();
	}
	
	public void addTeleportBookmarks(int id, BookmarkTemplate template)
	{
		_tpbookmarks.put(id, template);
	}

	public int getBookmarkslot()
	{
		return _bookmarkslot;
	}

	public int getQuestInventoryLimit()
	{
		return Config.INVENTORY_MAXIMUM_QUEST_ITEMS;
	}

	public boolean canAttackCharacter(Creature cha)
	{
		if (cha instanceof Attackable)
		{
			return true;
		}
		else if (cha instanceof Playable)
		{
			if (cha.isInsideZone(ZoneId.PVP) && !cha.isInsideZone(ZoneId.SIEGE))
			{
				if (cha.isInsideZone(ZoneId.FUN_PVP) && cha.isPlayer())
				{
					final FunPvpZone zone = ZoneManager.getInstance().getZone(cha, FunPvpZone.class);
					if (zone != null)
					{
						if (zone.isPvpZone() && isFriend(cha.getActingPlayer()))
						{
							return false;
						}
					}
				}
				return true;
			}

			Player target;
			if (cha instanceof Summon)
			{
				target = ((Summon) cha).getOwner();
			}
			else
			{
				target = (Player) cha;
			}

			if (isInDuel() && target.isInDuel() && (target.getDuelId() == getDuelId()))
			{
				return true;
			}
			else if (isInParty() && target.isInParty())
			{
				if (getParty() == target.getParty())
				{
					return false;
				}
				if (((getParty().getCommandChannel() != null) || (target.getParty().getCommandChannel() != null)) && (getParty().getCommandChannel() == target.getParty().getCommandChannel()))
				{
					return false;
				}
			}
			else if ((getClan() != null) && (target.getClan() != null))
			{
				if (getClanId() == target.getClanId())
				{
					return false;
				}
				if (((getAllyId() > 0) || (target.getAllyId() > 0)) && (getAllyId() == target.getAllyId()))
				{
					return false;
				}
				if (getClan().isAtWarWith(target.getClan().getId()) && target.getClan().isAtWarWith(getClan().getId()))
				{
					return true;
				}
			}
			else if ((getClan() == null) || (target.getClan() == null))
			{
				if ((target.getPvpFlag() == 0) && (target.getKarma() == 0))
				{
					return false;
				}
			}
		}
		return true;
	}

	public boolean isInventoryUnder90(boolean includeQuestInv)
	{
		return (getInventory().getSize(includeQuestInv) <= (getInventoryLimit() * 0.9));
	}
	
	public boolean isInventoryUnderRepcent(boolean includeQuestInv, double percent)
	{
		return (getInventory().getSize(includeQuestInv) < (getInventoryLimit() * (percent / 100.)));
	}
	
	public boolean isPrivateInventoryUnder90()
	{
		return (getPrivateInventory().getItemSize() <= (getPrivateInventoryLimit() * 0.9));
	}

	public boolean havePetInvItems()
	{
		return _petItems;
	}

	public void setPetInvItems(boolean haveit)
	{
		_petItems = haveit;
	}

	private void restorePetInventoryItems()
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT object_id FROM `items` WHERE `owner_id`=? AND (`loc`='PET' OR `loc`='PET_EQUIP') LIMIT 1;");
			statement.setInt(1, getObjectId());
			rset = statement.executeQuery();
			if (rset.next() && (rset.getInt("object_id") > 0))
			{
				setPetInvItems(true);
			}
			else
			{
				setPetInvItems(false);
			}
		}
		catch (final Exception e)
		{
			_log.warn("Could not check Items in Pet Inventory for playerId: " + getObjectId(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
	}

	public String getAdminConfirmCmd()
	{
		return _adminConfirmCmd;
	}

	public void setAdminConfirmCmd(String adminConfirmCmd)
	{
		_adminConfirmCmd = adminConfirmCmd;
	}

	public void setBlockCheckerArena(byte arena)
	{
		_handysBlockCheckerEventArena = arena;
	}

	public int getBlockCheckerArena()
	{
		return _handysBlockCheckerEventArena;
	}

	public void setLastPetitionGmName(String gmName)
	{
		_lastPetitionGmName = gmName;
	}

	public String getLastPetitionGmName()
	{
		return _lastPetitionGmName;
	}

	public ContactList getContactList()
	{
		return _contactList;
	}
	
	public void restoreContactList()
	{
		_contactList = new ContactList(this);
	}

	public long getNotMoveUntil()
	{
		return _notMoveUntil;
	}

	public void updateNotMoveUntil()
	{
		_notMoveUntil = System.currentTimeMillis() + Config.PLAYER_MOVEMENT_BLOCK_TIME;
	}

	@Override
	public boolean isPlayer()
	{
		return true;
	}

	@Override
	public boolean isChargedShot(ShotType type)
	{
		final ItemInstance weapon = getActiveWeaponInstance();
		return (weapon != null) && weapon.isChargedShot(type);
	}

	@Override
	public void setChargedShot(ShotType type, boolean charged)
	{
		final ItemInstance weapon = getActiveWeaponInstance();
		if (weapon != null)
		{
			weapon.setChargedShot(type, charged);
		}

		if (!charged)
		{
			rechargeShots(true, true);
		}
	}

	public final Skill getCustomSkill(int skillId)
	{
		return (_customSkills != null) ? _customSkills.get(skillId) : null;
	}

	private final void addCustomSkill(Skill skill)
	{
		if ((skill != null) && (skill.getDisplayId() != skill.getId()))
		{
			if (_customSkills == null)
			{
				_customSkills = new ConcurrentHashMap<>();
			}
			_customSkills.put(skill.getDisplayId(), skill);
		}
	}

	private final void removeCustomSkill(Skill skill)
	{
		if ((skill != null) && (_customSkills != null) && (skill.getDisplayId() != skill.getId()))
		{
			_customSkills.remove(skill.getDisplayId());
		}
	}

	@Override
	public boolean canRevive()
	{
		return _canRevive;
	}

	@Override
	public void setCanRevive(boolean val)
	{
		_canRevive = val;
	}

	@Override
	public void addOverrideCond(PcCondOverride... excs)
	{
		super.addOverrideCond(excs);
		setVar("cond_override", Long.toString(_exceptions));
	}

	@Override
	public void removeOverridedCond(PcCondOverride... excs)
	{
		super.removeOverridedCond(excs);
		setVar("cond_override", Long.toString(_exceptions));
	}

	public void enterMovieMode()
	{
		setTarget(null);
		stopMove(null);
		setIsInvul(true);
		setIsImmobilized(true);
		sendPacket(CameraMode.FIRST_PERSON);
	}

	public void leaveMovieMode()
	{
		if (!isGM())
		{
			setIsInvul(false);
		}
		setIsImmobilized(false);
		sendPacket(CameraMode.THIRD_PERSON);
		sendPacket(NormalCamera.STATIC_PACKET);
	}
	
	public void specialCamera(GameObject target, int dist, int yaw, int pitch, int time, int duration, int turn, int rise, int widescreen, int unk)
	{
		sendPacket(new SpecialCamera(target.getObjectId(), dist, yaw, pitch, time, duration, turn, rise, widescreen, unk));
	}

	public void specialCamera(GameObject target, int dist, int yaw, int pitch, int time, int duration)
	{
		sendPacket(new SpecialCamera((Creature) target, dist, yaw, pitch, time, duration));
	}

	public int getPcBangPoints()
	{
		return _pcBangPoints;
	}

	public void setPcBangPoints(final int i)
	{
		final int nextPoints = i < Config.MAX_PC_BANG_POINTS ? i : Config.MAX_PC_BANG_POINTS;
		final boolean canAch = nextPoints > _pcBangPoints;
		_pcBangPoints = nextPoints;
		if (canAch)
		{
			getCounters().addAchivementInfo("pcPointAcquired", 0, _pcBangPoints, true, false, false);
		}
	}

	public long getLastMovePacket()
	{
		return _lastMovePacket;
	}
	
	public void setLastMovePacket()
	{
		_lastMovePacket = System.currentTimeMillis();
	}

	public int getUCKills()
	{
		return UCKills;
	}

	public void addKillCountUC()
	{
		UCKills++;
	}

	public int getUCDeaths()
	{
		return UCDeaths;
	}

	public void addDeathCountUC()
	{
		UCDeaths++;
	}

	public void cleanUCStats()
	{
		UCDeaths = 0;
		UCKills = 0;
	}

	public void setUCState(int state)
	{
		UCState = state;
	}

	public int getUCState()
	{
		return UCState;
	}

	public long getGamePoints()
	{
		return _gamePoints;
	}

	public void setGamePoints(long gamePoints)
	{
		_gamePoints = gamePoints >= Long.MAX_VALUE ? Long.MAX_VALUE : gamePoints;
	}

	public boolean isPKProtected(final Player target)
	{
		if (Config.DISABLE_ATTACK_IF_LVL_DIFFERENCE_OVER != 0)
		{
			if (target.isInSameClan(this) || target.isInSameParty(this) || target.isInSameChannel(this) || target.isInOlympiadMode() || target.isInsideZone(ZoneId.SIEGE) || target.isInsideZone(ZoneId.PVP) || (getClan() != null && target.getClan() != null && target.getClan().isAtWarWith(getClanId()) && getClan().isAtWarWith(target.getClan().getId())))
			{
				return false;
			}

			if ((target.getPvpFlag() == 0 || target.getKarma() == 0) && ((target.getLevel() + Config.DISABLE_ATTACK_IF_LVL_DIFFERENCE_OVER) < getLevel()) || Config.DISABLE_ATTACK_IF_LVL_DIFFERENCE_OVER > 85)
			{
				return true;
			}
		}
		return false;
	}

	public int getRevision()
	{
		return _client == null ? 0 : _client.getRevision();
	}

	public int getKamalokaId()
	{
		return _kamaID;
	}

	public void setKamalokaId(int instanceId)
	{
		_kamaID = instanceId;
	}

	public void checkPlayer()
	{
		if (!Config.SECURITY_SKILL_CHECK || isTransformed() || isInStance())
		{
			return;
		}

		boolean haveWrongSkills = false;

		for (final Skill skill : getAllSkills())
		{
			boolean wrongSkill = false;
			if (SkillTreesParser.getInstance().isNotCheckSkill(this, skill.getId(), skill.getLevel()) || skill.isBlockRemove())
			{
				continue;
			}
			
			for (final int skillId : SkillTreesParser.getInstance().getRestrictedSkills(getClassId()))
			{
				if (skill.getId() == skillId)
				{
					wrongSkill = true;
					break;
				}
			}
			
			if (wrongSkill)
			{
				haveWrongSkills = true;
				if (Config.SECURITY_SKILL_CHECK_CLEAR)
				{
					removeSkill(skill);
				}
				else
				{
					break;
				}
			}
		}
		
		if (haveWrongSkills)
		{
			if (Config.SECURITY_SKILL_CHECK_CLEAR)
			{
				sendPacket(new SkillList());
			}
			Util.handleIllegalPlayerAction(this, "Possible cheater with wrong skills! Name: " + getName(null) + " (" + getObjectId() + ")" + ", account: " + getAccountName());
		}
	}

	public AdminProtection getAdminProtection()
	{
		if (_AdminProtection == null)
		{
			_AdminProtection = new AdminProtection(this);
		}
		return _AdminProtection;
	}

	public long getOnlineTime()
	{
		return _onlineTime;
	}
	
	public long getTotalOnlineTime()
	{
		return _onlineTime + ((System.currentTimeMillis() - getOnlineBeginTime()) / 1000);
	}

	public long getOnlineBeginTime()
	{
		return _onlineBeginTime;
	}

	@Override
	public boolean isInCategory(CategoryType type)
	{
		return CategoryParser.getInstance().isInCategory(type, getClassId().getId());
	}

	@Override
	public int getId()
	{
		return 0;
	}

	public boolean isPartyBanned()
	{
		return PunishmentManager.getInstance().checkPunishment(getClient(), PunishmentType.PARTY_BAN);
	}

	@Override
	public void teleToClosestTown()
	{
		teleToLocation(TeleportWhereType.TOWN, true, ReflectionManager.DEFAULT);
	}
	
	private int _hoursInGame = 0;
	
	public int getHoursInGame()
	{
		_hoursInGame++;
		return _hoursInGame;
	}
	
	public int getHoursInGames()
	{
		return _hoursInGame;
	}

	public void setOfflineMode(boolean val)
	{
		if (!val)
		{
			unsetVar("offline");
			unsetVar("offlineBuff");
		}
		_offline = val;
	}
	
	public boolean isInOfflineMode()
	{
		return _offline;
	}

	public void addQuickVar(String name, Object value)
	{
		if (quickVars.containsKey(name))
		{
			quickVars.remove(name);
		}
		quickVars.put(name, value);
	}
	
	public String getQuickVarS(String name, String... defaultValue)
	{
		if (!quickVars.containsKey(name))
		{
			if (defaultValue.length > 0)
			{
				return defaultValue[0];
			}
			return null;
		}
		return (String) quickVars.get(name);
	}
	
	public boolean getQuickVarB(String name, boolean... defaultValue)
	{
		if (!quickVars.containsKey(name))
		{
			if (defaultValue.length > 0)
			{
				return defaultValue[0];
			}
			return false;
		}
		return ((Boolean) quickVars.get(name)).booleanValue();
	}
	
	public int getQuickVarI(String name, int... defaultValue)
	{
		if (!quickVars.containsKey(name))
		{
			if (defaultValue.length > 0)
			{
				return defaultValue[0];
			}
			return -1;
		}
		return ((Integer) quickVars.get(name)).intValue();
	}
	
	public long getQuickVarL(String name, long... defaultValue)
	{
		if (!quickVars.containsKey(name))
		{
			if (defaultValue.length > 0)
			{
				return defaultValue[0];
			}
			return -1L;
		}
		return ((Long) quickVars.get(name)).longValue();
	}
	
	public Object getQuickVarO(String name, Object... defaultValue)
	{
		if (!quickVars.containsKey(name))
		{
			if (defaultValue.length > 0)
			{
				return defaultValue[0];
			}
			return null;
		}
		return quickVars.get(name);
	}
	
	public boolean containsQuickVar(String name)
	{
		return quickVars.containsKey(name);
	}
	
	public void deleteQuickVar(String name)
	{
		quickVars.remove(name);
	}
	
	public void sendConfirmDlg(OnAnswerListener listener, int time, String msg)
	{
		final ConfirmDlg packet = new ConfirmDlg(SystemMessageId.S1.getId());
		packet.addString(msg);
		packet.addTime(time);
		ask(packet, listener);
	}
	
	public void ask(ConfirmDlg dlg, OnAnswerListener listener)
	{
		if (_askDialog != null)
		{
			return;
		}

		final int rnd = Rnd.nextInt();
		_askDialog = new ImmutablePair<>(rnd, listener);
		dlg.addRequesterId(rnd);
		sendPacket(dlg);
	}

	public Pair<Integer, OnAnswerListener> getAskListener(boolean clear)
	{
		if (!clear)
		{
			return _askDialog;
		}
		else
		{
			final Pair<Integer, OnAnswerListener> ask = _askDialog;
			_askDialog = null;
			return ask;
		}
	}

	public boolean hasDialogAskActive()
	{
		return _askDialog != null;
	}
	
	public boolean isSellingBuffs()
	{
		return _isSellingBuffs;
	}
	
	public void setIsSellingBuffs(boolean val)
	{
		_isSellingBuffs = val;
	}
	
	public List<SellBuffHolder> getSellingBuffs()
	{
		if (_sellingBuffs == null)
		{
			_sellingBuffs = new ArrayList<>();
		}
		return _sellingBuffs;
	}
	
	public void setCleftKill(int killPoint)
	{
		_cleftKills += killPoint;
	}
	
	public int getCleftKills()
	{
		return _cleftKills;
	}
	
	public void setCleftDeath(int deathPoint)
	{
		_cleftDeaths += deathPoint;
	}
	
	public int getCleftDeaths()
	{
		return _cleftDeaths;
	}
	
	public void setCleftKillTower(int killPoint)
	{
		_cleftKillTowers += killPoint;
	}
	
	public int getCleftKillTowers()
	{
		return _cleftKillTowers;
	}
	
	public void setCleftCat(boolean cat)
	{
		_cleftCat = cat;
	}
	
	public boolean isCleftCat()
	{
		return _cleftCat;
	}

	public void cleanCleftStats()
	{
		_cleftKills = 0;
		_cleftDeaths = 0;
		_cleftKillTowers = 0;
		_cleftCat = false;
	}
	
	public MatchingRoom getMatchingRoom()
	{
		return _matchingRoom;
	}

	public void setMatchingRoom(MatchingRoom matchingRoom)
	{
		_matchingRoom = matchingRoom;
		if (matchingRoom == null)
		{
			_matchingRoomWindowOpened = false;
		}
	}

	public boolean isMatchingRoomWindowOpened()
	{
		return _matchingRoomWindowOpened;
	}

	public void setMatchingRoomWindowOpened(boolean b)
	{
		_matchingRoomWindowOpened = b;
	}
	
	@Override
	public Iterator<Player> iterator()
	{
		return Collections.singleton(this).iterator();
	}
	
	@Override
	public void broadCast(GameServerPacket... packet)
	{
		sendPacket(packet);
	}
	
	@Override
	public void broadCastMessage(ServerMessage msg)
	{
		sendMessage(msg.toString(getLang()));
	}
	
	@Override
	public int getMemberCount()
	{
		return 1;
	}
	
	@Override
	public Player getGroupLeader()
	{
		return this;
	}
	
	public int getPing()
	{
		return _ping;
	}
	
	public void setPing(int ping)
	{
		_ping = ping;
	}

	public int getLectureMark()
	{
		return _lectureMark;
	}
	
	public void setLectureMark(int lectureMark)
	{
		_lectureMark = lectureMark;
	}
	
	public PetitionMainGroup getPetitionGroup()
	{
		return _petitionGroup;
	}

	public void setPetitionGroup(PetitionMainGroup petitionGroup)
	{
		_petitionGroup = petitionGroup;
	}
	
	public void hidePrivateStores()
	{
		final ArrayList<GameServerPacket> pls = new ArrayList<>();
		for (final Player player : World.getInstance().getAroundPlayers(this))
		{
			if (player.isInStoreNow())
			{
				pls.add(new DeleteObject(player));
			}
		}
		sendPacket(pls);
		pls.clear();
	}

	public void restorePrivateStores()
	{
		for (final Player player : World.getInstance().getAroundPlayers(this))
		{
			if (player.isInStoreNow())
			{
				player.broadcastInfo();
			}
		}
	}
	
	public void resetHidePlayer()
	{
		for (final Player player : World.getInstance().getAroundPlayers(this))
		{
			if (player.getNotShowTraders())
			{
				sendInfo(player);
			}
		}
	}

	public void addLoadedImage(int id)
	{
		_loadedImages.add(id);
	}

	public boolean wasImageLoaded(int id)
	{
		return _loadedImages.contains(id);
	}

	public int getLoadedImagesSize()
	{
		return _loadedImages.size();
	}
	
	public String getEventName(int eventId)
	{
		final AbstractFightEvent event = FightEventParser.getInstance().getEvent(eventId);
		if (event != null)
		{
			return event.getName(getLang());
		}
		return "";
	}
	
	public String getEventDescr(int eventId)
	{
		final AbstractFightEvent event = FightEventParser.getInstance().getEvent(eventId);
		if (event != null)
		{
			return event.getDescription(getLang());
		}
		return "";
	}

	public void updateNpcNames()
	{
		for (final Npc npc : World.getInstance().getAroundNpc(this))
		{
			npc.broadcastPacket(new NpcInfo.Info(npc, this));
		}
	}

	public NevitSystem getNevitSystem()
	{
		return _nevitSystem;
	}

	public void sendVoteSystemInfo()
	{
		if (Config.ALLOW_RECO_BONUS_SYSTEM)
		{
			sendPacket(new ExVoteSystemInfo(this));
		}
	}
	
	public void isntAfk()
	{
		_lastNotAfkTime = System.currentTimeMillis();
	}
	
	public long getLastNotAfkTime()
	{
		return _lastNotAfkTime;
	}
	
	public FightEventGameRoom getFightEventGameRoom()
	{
		return _fightEventGameRoom;
	}
	
	public void setFightEventGameRoom(final FightEventGameRoom room)
	{
		_fightEventGameRoom = room;
	}
	
	public void requestCheckBot()
	{
		final BotCheckQuestion question = BotCheckManager.getInstance().generateRandomQuestion();
		final int qId = question.getId();
		final String qDescr = question.getDescr(getLang());
		
		sendConfirmDlg(new BotCheckAnswerListner(this, qId), Config.ASK_ANSWER_DELAY * 60000, qDescr);
		
		startAbnormalEffect(AbnormalEffect.HOLD_2);
		getAI().setIntention(CtrlIntention.IDLE, this);
		setIsParalyzed(true);
		getPersonalTasks().addTask(new CheckBotTask(Config.ASK_ANSWER_DELAY * 60000L));
	}
	
	public void increaseBotRating()
	{
		final int bot_points = getBotRating();
		if (bot_points + 1 >= Config.MAX_BOT_POINTS)
		{
			return;
		}
		setBotRating(bot_points + 1);
	}
	
	public void decreaseBotRating()
	{
		final int bot_points = getBotRating();
		if (bot_points - 1 <= Config.MINIMAL_BOT_RATING_TO_BAN)
		{
			Util.handleIllegalPlayerAction(this, "" + ServerStorage.getInstance().getString(getLang(), "BotCheck.PUNISH_MSG"));
			if (Config.ANNOUNCE_AUTO_BOT_BAN)
			{
				Announcements.getInstance().announceToAll("Player " + getName(null) + " jailed for botting!");
			}
		}
		else
		{
			setBotRating(bot_points - 1);
			if (Config.ON_WRONG_QUESTION_KICK)
			{
				logout();
			}
		}
	}
	
	public void setBotRating(int rating)
	{
		_botRating = rating;
	}
	
	public int getBotRating()
	{
		return _botRating;
	}
	
	public void startHpPotionTask()
	{
		if (!Config.AUTO_POINTS_SYSTEM)
		{
			return;
		}
		
		final int potionId = getVarInt("autoHpItemId", 0);
		final ItemInstance hpPotion = getInventory().getItemByItemId(potionId);
		if (hpPotion == null)
		{
			setVar("useAutoHpPotions@", 0);
			if (potionId > 0)
			{
				final ServerMessage msg = new ServerMessage("Menu.STRING_AUTO_POINTS_REQUIRED", getLang());
				msg.add(ItemsParser.getInstance().getTemplate(potionId).getName(getLang()));
				sendMessage(msg.toString());
			}
			return;
		}
		
		long reuseDelay = hpPotion.getReuseDelay();
		if (reuseDelay >= 10000L)
		{
			reuseDelay = reuseDelay + 3000L;
		}
		else if (reuseDelay < 1000L)
		{
			reuseDelay = 1000L;
		}
		_hpPotionDelay = reuseDelay;
		if (_acpTask == null)
		{
			_acpTask = ThreadPoolManager.getInstance().scheduleAtFixedDelay(new AcpTask(this), 500, 500);
		}
		sendMessage((new ServerMessage("Menu.STRING_AUTO_HP_START", getLang())).toString());
	}
	
	public void stopHpPotionTask()
	{
		setVar("useAutoHpPotions@", 0);
		sendMessage((new ServerMessage("Menu.STRING_AUTO_HP_STOP", getLang())).toString());
	}

	public boolean getUseAutoHpPotions()
	{
		return getVarB("useAutoHpPotions@");
	}
	
	public void startMpPotionTask()
	{
		if (!Config.AUTO_POINTS_SYSTEM)
		{
			return;
		}

		final int potionId = getVarInt("autoMpItemId", 0);
		final ItemInstance mpPotion = getInventory().getItemByItemId(potionId);
		if (mpPotion == null)
		{
			setVar("useAutoMpPotions@", 0);
			if (potionId > 0)
			{
				final ServerMessage msg = new ServerMessage("Menu.STRING_AUTO_POINTS_REQUIRED", getLang());
				msg.add(ItemsParser.getInstance().getTemplate(potionId).getName(getLang()));
				sendMessage(msg.toString());
			}
			return;
		}
		
		long reuseDelay = mpPotion.getReuseDelay();
		if (reuseDelay >= 10000L)
		{
			reuseDelay = reuseDelay + 3000L;
		}
		else if (reuseDelay < 1000L)
		{
			reuseDelay = 1000L;
		}
		_mpPotionDelay = reuseDelay;
		if (_acpTask == null)
		{
			_acpTask = ThreadPoolManager.getInstance().scheduleAtFixedDelay(new AcpTask(this), 500, 500);
		}
		sendMessage((new ServerMessage("Menu.STRING_AUTO_MP_START", getLang())).toString());
	}
	
	public void stopMpPotionTask()
	{
		setVar("useAutoMpPotions@", 0);
		sendMessage((new ServerMessage("Menu.STRING_AUTO_MP_STOP", getLang())).toString());
	}

	public boolean getUseAutoMpPotions()
	{
		return getVarB("useAutoMpPotions@");
	}
	
	public void startCpPotionTask()
	{
		if (!Config.AUTO_POINTS_SYSTEM)
		{
			return;
		}
		
		final int potionId = getVarInt("autoCpItemId", 0);
		final ItemInstance cpPotion = getInventory().getItemByItemId(potionId);
		if (cpPotion == null)
		{
			setVar("useAutoCpPotions@", 0);
			if (potionId > 0)
			{
				final ServerMessage msg = new ServerMessage("Menu.STRING_AUTO_POINTS_REQUIRED", getLang());
				msg.add(ItemsParser.getInstance().getTemplate(potionId).getName(getLang()));
				sendMessage(msg.toString());
			}
			return;
		}
		
		long reuseDelay = cpPotion.getReuseDelay();
		if (reuseDelay >= 10000L)
		{
			reuseDelay = reuseDelay + 3000L;
		}
		else if (reuseDelay < 1000L)
		{
			reuseDelay = 500L;
		}
		_cpPotionDelay = reuseDelay;
		if (_acpTask == null)
		{
			_acpTask = ThreadPoolManager.getInstance().scheduleAtFixedDelay(new AcpTask(this), 500, 500);
		}
		sendMessage((new ServerMessage("Menu.STRING_AUTO_CP_START", getLang())).toString());
	}
	
	public void stopCpPotionTask()
	{
		setVar("useAutoCpPotions@", 0);
		sendMessage((new ServerMessage("Menu.STRING_AUTO_CP_STOP", getLang())).toString());
	}

	public boolean getUseAutoCpPotions()
	{
		return getVarB("useAutoCpPotions@");
	}
	
	public void startSoulPotionTask()
	{
		if (!Config.AUTO_POINTS_SYSTEM)
		{
			return;
		}
		
		final int potionId = getVarInt("autoSoulItemId", 0);
		final ItemInstance soulPotion = getInventory().getItemByItemId(potionId);
		if (soulPotion == null)
		{
			setVar("useAutoSoulPotions@", 0);
			if (potionId > 0)
			{
				final ServerMessage msg = new ServerMessage("Menu.STRING_AUTO_POINTS_REQUIRED", getLang());
				msg.add(ItemsParser.getInstance().getTemplate(potionId).getName(getLang()));
				sendMessage(msg.toString());
			}
			return;
		}
		
		long reuseDelay = soulPotion.getReuseDelay();
		if (reuseDelay >= 10000L)
		{
			reuseDelay = reuseDelay + 3000L;
		}
		else if (reuseDelay < 1000L)
		{
			reuseDelay = 1000L;
		}
		
		_soulPotionDelay = reuseDelay;
		if (_acpTask == null)
		{
			_acpTask = ThreadPoolManager.getInstance().scheduleAtFixedDelay(new AcpTask(this), 500, 500);
		}
		sendMessage((new ServerMessage("Menu.STRING_AUTO_CP_START", getLang())).toString());
	}
	
	public void stopSoulPotionTask()
	{
		setVar("useAutoSoulPotions@", 0);
		sendMessage((new ServerMessage("Menu.STRING_AUTO_CP_STOP", getLang())).toString());
	}

	public boolean getUseAutoSoulPotions()
	{
		return getVarB("useAutoSoulPotions@");
	}
	
	public void onUseAcp()
	{
		if (!getUseAutoHpPotions() && !getUseAutoMpPotions() && !getUseAutoCpPotions() && !getUseAutoSoulPotions())
		{
			return;
		}
		
		if (isInvisible() || isInKrateisCube() || isDead() || isAllSkillsDisabled() || isHealBlocked() || isCombatFlagEquipped() || getUCState() > 0 || isCombatFlagEquipped() || isInFightEvent() || isInOlympiadMode() || ((AerialCleftEvent.getInstance().isStarted() || AerialCleftEvent.getInstance().isRewarding()) && AerialCleftEvent.getInstance().isPlayerParticipant(getObjectId())))
		{
			return;
		}
		
		final long timeNow = System.currentTimeMillis();
		if (getUseAutoHpPotions() && timeNow > _hpReuseDelay)
		{
			final ItemInstance hpPoint = getInventory().getItemByItemId(getVarInt("autoHpItemId", 0));
			if (hpPoint != null)
			{
				final int hpVar = getVarInt("hpPercent", Config.DEFAULT_HP_PERCENT);
				if (getCurrentHp() < ((getMaxHp() * hpVar) / 100))
				{
					final IItemHandler handler = ItemHandler.getInstance().getHandler(hpPoint.getEtcItem());
					if (handler != null)
					{
						if (handler.useItem(this, hpPoint, false))
						{
							_hpReuseDelay = timeNow + _hpPotionDelay;
							if (_hpPotionDelay > 0)
							{
								addTimeStampItem(hpPoint, hpPoint.getReuseDelay(), hpPoint.isReuseByCron());
								if (hpPoint.getSharedReuseGroup() > 0)
								{
									sendPacket(new ExUseSharedGroupItem(hpPoint.getId(), hpPoint.getSharedReuseGroup(), hpPoint.getReuseDelay(), hpPoint.getReuseDelay()));
								}
							}
						}
					}
				}
			}
			else
			{
				if (Config.DISABLE_WITHOUT_POTIONS)
				{
					stopHpPotionTask();
				}
			}
		}
		
		if (getUseAutoMpPotions() && timeNow > _mpReuseDelay)
		{
			final ItemInstance mpPoint = getInventory().getItemByItemId(getVarInt("autoMpItemId", 0));
			if (mpPoint != null)
			{
				final int mpVar = getVarInt("mpPercent", Config.DEFAULT_MP_PERCENT);
				if (getCurrentMp() < ((getMaxMp() * mpVar) / 100))
				{
					final IItemHandler handler = ItemHandler.getInstance().getHandler(mpPoint.getEtcItem());
					if (handler != null)
					{
						if (handler.useItem(this, mpPoint, false))
						{
							_mpReuseDelay = timeNow + _mpPotionDelay;
							if (_mpReuseDelay > 0)
							{
								addTimeStampItem(mpPoint, mpPoint.getReuseDelay(), mpPoint.isReuseByCron());
								if (mpPoint.getSharedReuseGroup() > 0)
								{
									sendPacket(new ExUseSharedGroupItem(mpPoint.getId(), mpPoint.getSharedReuseGroup(), mpPoint.getReuseDelay(), mpPoint.getReuseDelay()));
								}
							}
						}
					}
				}
			}
			else
			{
				if (Config.DISABLE_WITHOUT_POTIONS)
				{
					stopMpPotionTask();
				}
			}
		}
		
		if (getUseAutoCpPotions() && timeNow > _cpReuseDelay)
		{
			final ItemInstance cpPoint = getInventory().getItemByItemId(getVarInt("autoCpItemId", 0));
			if (cpPoint != null)
			{
				final int cpVar = getVarInt("cpPercent", Config.DEFAULT_CP_PERCENT);
				if (getCurrentCp() < ((getMaxCp() * cpVar) / 100))
				{
					final IItemHandler handler = ItemHandler.getInstance().getHandler(cpPoint.getEtcItem());
					if (handler != null)
					{
						if (handler.useItem(this, cpPoint, false))
						{
							_cpReuseDelay = timeNow + _cpPotionDelay;
							if (_cpReuseDelay > 0)
							{
								addTimeStampItem(cpPoint, cpPoint.getReuseDelay(), cpPoint.isReuseByCron());
								if (cpPoint.getSharedReuseGroup() > 0)
								{
									sendPacket(new ExUseSharedGroupItem(cpPoint.getId(), cpPoint.getSharedReuseGroup(), cpPoint.getReuseDelay(), cpPoint.getReuseDelay()));
								}
							}
						}
					}
				}
			}
			else
			{
				if (Config.DISABLE_WITHOUT_POTIONS)
				{
					stopCpPotionTask();
				}
			}
		}
		
		if (getUseAutoSoulPotions() && timeNow > _soulReuseDelay)
		{
			final ItemInstance soulPoint = getInventory().getItemByItemId(getVarInt("autoSoulItemId", 0));
			if (soulPoint != null)
			{
				final int soulVar = getVarInt("soulPercent", Config.DEFAULT_SOUL_AMOUNT);
				if (getChargedSouls() < soulVar)
				{
					_soulReuseDelay = timeNow + _soulPotionDelay;
					final IItemHandler handler = ItemHandler.getInstance().getHandler(soulPoint.getEtcItem());
					if (handler != null)
					{
						handler.useItem(this, soulPoint, false);
					}
				}
			}
			else
			{
				if (Config.DISABLE_WITHOUT_POTIONS)
				{
					stopSoulPotionTask();
				}
			}
		}
	}
	
	public int getChatMsg()
	{
		return _chatMsg;
	}
	
	public void setChatMsg(final int value)
	{
		_chatMsg = value;
		if (value > 0 && value <= Config.CHAT_MSG_ANNOUNCE)
		{
			final ServerMessage msg = new ServerMessage("CustomChat.LAST_MSG", getLang());
			msg.add(value);
			sendMessage(msg.toString());
		}
		else if (value == 0)
		{
			sendMessage((new ServerMessage("CustomChat.LIMIT", getLang())).toString());
		}
	}
	
	public int getCustomChatStatus()
	{
		return (int) getStat().calcStat(Stats.CHAT_STATUS, 0, null, null);
	}
	
	public void checkChatMessages()
	{
		if (!Config.ALLOW_CUSTOM_CHAT)
		{
			return;
		}
		
		final Calendar temp = Calendar.getInstance();
		temp.set(Calendar.HOUR_OF_DAY, 6);
		temp.set(Calendar.MINUTE, 30);
		temp.set(Calendar.SECOND, 0);
		temp.set(Calendar.MILLISECOND, 0);
		long count = Math.round(((System.currentTimeMillis() - _lastAccess) / 1000) / 86400);
		if ((count == 0) && (_lastAccess < temp.getTimeInMillis()) && (System.currentTimeMillis() > temp.getTimeInMillis()))
		{
			count++;
		}
		
		if (count > 0)
		{
			restartChatMessages();
		}
	}
	
	public void restartChatMessages()
	{
		setChatMsg(hasPremiumBonus() ? Config.CHAT_MSG_PREMIUM : Config.CHAT_MSG_SIMPLE);
	}
	
	@Override
	public int getMaxLoad()
	{
		return (int) calcStat(Stats.WEIGHT_LIMIT, Math.floor(BaseStats.CON.calcBonus(this) * 69000 * Config.ALT_WEIGHT_LIMIT * getPremiumBonus().getWeight()), this, null);
	}
	
	@Override
	public int getBonusWeightPenalty()
	{
		return (int) calcStat(Stats.WEIGHT_PENALTY, 1, this, null);
	}
	
	@Override
	public int getCurrentLoad()
	{
		return getInventory().getTotalWeight();
	}
	
	public boolean isControllingFakePlayer()
	{
		return _fakePlayerUnderControl != null;
	}
	
	public FakePlayer getPlayerUnderControl()
	{
		return _fakePlayerUnderControl;
	}
	
	public void setPlayerUnderControl(FakePlayer fakePlayer)
	{
		_fakePlayerUnderControl = fakePlayer;
	}
	
	public void setFakePlayer(boolean isFakePlayer)
	{
		_fakePlayer = isFakePlayer;
	}
	
	@Override
	public boolean isFakePlayer()
	{
		return _fakePlayer;
	}
	
	public void setFakeLocation(FakeLocTemplate loc)
	{
		_fakeLocation = loc;
	}
	
	public void setFakeTerritoryLocation(FakePassiveLocTemplate loc)
	{
		_fakePassiveLocation = loc;
	}
	
	public FakeLocTemplate getFakeLocation()
	{
		return _fakeLocation;
	}
	
	public FakePassiveLocTemplate getFakeTerritory()
	{
		return _fakePassiveLocation;
	}
	
	public PlayerGroup getPlayerGroup()
	{
		if (getParty() != null)
		{
			if (getParty().getCommandChannel() != null)
			{
				return getParty().getCommandChannel();
			}
			else
			{
				return getParty();
			}
		}
		else
		{
			return this;
		}
	}
	
	public void restoreDailyRewards()
	{
		final String charInfo = DailyRewardManager.getInstance().getCharInfo(this);
		if (charInfo == null)
		{
			return;
		}
		
		if (!DailyRewardManager.getInstance().isHaveCharInfo(charInfo))
		{
			DailyRewardManager.getInstance().addNewDailyPlayer(charInfo);
		}
	}
	
	public Collection<PlayerTaskTemplate> getActiveDailyTasks()
	{
		return _activeTasks.values();
	}
	
	public void addActiveDailyTasks(int id, PlayerTaskTemplate template)
	{
		_activeTasks.put(id, template);
	}
	
	public void removeActiveDailyTasks(int id)
	{
		_activeTasks.remove(id);
	}
	
	public int getActiveTasks(String type)
	{
		int amount = 0;
		for (final PlayerTaskTemplate template : _activeTasks.values())
		{
			if (template != null && template.getSort().equalsIgnoreCase(type))
			{
				amount++;
			}
		}
		return amount;
	}
	
	public PlayerTaskTemplate getDailyTaskTemplate(final int id)
	{
		return _activeTasks.get(id);
	}
	
	public int getLastDailyTasks()
	{
		return DailyTaskManager.getInstance().getTaskCount(this)[0];
	}
	
	public int getLastWeeklyTasks()
	{
		return DailyTaskManager.getInstance().getTaskCount(this)[1];
	}
	
	public int getLastMonthTasks()
	{
		return DailyTaskManager.getInstance().getTaskCount(this)[2];
	}
	
	public void addDailyTask(final PlayerTaskTemplate template)
	{
		DailyTasksDAO.getInstance().addNewDailyTask(this, template);
	}
	
	public void updateDailyStatus(final PlayerTaskTemplate template)
	{
		DailyTasksDAO.getInstance().updateTaskStatus(this, template);
	}
	
	public void updateDailyRewardStatus(final PlayerTaskTemplate template)
	{
		DailyTasksDAO.getInstance().updateTaskRewardStatus(this, template);
	}
	
	public void removeDailyTask(final int taskId)
	{
		DailyTasksDAO.getInstance().removeTask(this, taskId);
	}
	
	public void saveDailyTasks()
	{
		if (getActiveDailyTasks() != null)
		{
			for (final PlayerTaskTemplate taskTemplate : getActiveDailyTasks())
			{
				if (!taskTemplate.isComplete())
				{
					int params = 0;
					switch (taskTemplate.getType())
					{
						case "Farm" :
							params = taskTemplate.getCurrentNpcCount();
							break;
						case "Pvp" :
							params = taskTemplate.getCurrentPvpCount();
							break;
						case "Pk" :
							params = taskTemplate.getCurrentPkCount();
							break;
						case "Olympiad" :
							params = taskTemplate.getCurrentOlyMatchCount();
							break;
					}
					DailyTasksDAO.getInstance().updateTaskParams(this, taskTemplate.getId(), params);
				}
			}
		}
	}
	
	public void cleanDailyTasks()
	{
		for (final PlayerTaskTemplate template : _activeTasks.values())
		{
			if (template != null && template.getSort().equalsIgnoreCase("daily"))
			{
				_activeTasks.remove(template.getId());
			}
		}
	}
	
	public void cleanWeeklyTasks()
	{
		for (final PlayerTaskTemplate template : _activeTasks.values())
		{
			if (template != null && template.getSort().equalsIgnoreCase("weekly"))
			{
				_activeTasks.remove(template.getId());
			}
		}
	}
	
	public void cleanMonthTasks()
	{
		for (final PlayerTaskTemplate template : _activeTasks.values())
		{
			if (template != null && template.getSort().equalsIgnoreCase("month"))
			{
				_activeTasks.remove(template.getId());
			}
		}
	}
	
	public boolean canJoinParty(final Player inviter)
	{
		final Request request = getRequest();
		if ((request != null) && request.isProcessingRequest())
		{
			return false;
		}
		
		if (getBlockList().isBlocked(inviter) || getMessageRefusal())
		{
			return false;
		}
		
		if (isInParty() || isPartyBanned() || !isVisibleFor(inviter) || isCursedWeaponEquipped() || inviter.isCursedWeaponEquipped())
		{
			return false;
		}
		
		if ((AerialCleftEvent.getInstance().isStarted() || AerialCleftEvent.getInstance().isRewarding()) && AerialCleftEvent.getInstance().isPlayerParticipant(getObjectId()))
		{
			return false;
		}
		
		if (inviter.getReflectionId() != getReflectionId())
		{
			if ((inviter.getReflectionId() != 0) && (getReflectionId() != 0))
			{
				return false;
			}
		}
		
		if (inviter.isInOlympiadMode() || isInOlympiadMode())
		{
			return false;
		}
		
		if (getTeam() != 0)
		{
			return false;
		}
		if (isInFightEvent() && !getFightEvent().canJoinParty(inviter, this))
		{
			return false;
		}
		if (isInPvpFunZone())
		{
			final FunPvpZone zone = ZoneManager.getInstance().getZone(this, FunPvpZone.class);
			if (zone != null && !zone.canJoinParty(inviter, this))
			{
				return false;
			}
		}
		return true;
	}
	
	@Override
	public PlayerListenerList getListeners()
	{
		if (_listeners == null)
		{
			synchronized (this)
			{
				if (_listeners == null)
				{
					_listeners = new PlayerListenerList(this);
				}
			}
		}
		return (PlayerListenerList) _listeners;
	}
	
	public void restoreTradeList()
	{
		String var;
		var = getVar("selllist");
		if (var != null)
		{
			final String[] items = var.split(":");
			for (final String item : items)
			{
				if (item.equals(""))
				{
					continue;
				}
				final String[] values = item.split(";");
				if (values.length < 3)
				{
					continue;
				}
				
				final int oId = Integer.parseInt(values[0]);
				long count = Long.parseLong(values[1]);
				final long price = Long.parseLong(values[2]);
				
				final ItemInstance itemToSell = getInventory().getItemByObjectId(oId);
				
				if ((count < 1) || (itemToSell == null))
				{
					continue;
				}
				
				if (count > itemToSell.getCount())
				{
					count = itemToSell.getCount();
				}
				getSellList().addItem(itemToSell.getObjectId(), count, price);
			}
			var = getVar("sellstorename");
			if (var != null)
			{
				getSellList().setTitle(var);
			}
			
			if (getVar("storemode") != null)
			{
				getSellList().setPackaged(Integer.parseInt(getVar("storemode")) == Player.STORE_PRIVATE_PACKAGE_SELL);
			}
		}
		var = getVar("buylist");
		if (var != null)
		{
			final String[] items = var.split(":");
			for (final String item : items)
			{
				if (item.equals(""))
				{
					continue;
				}
				final String[] values = item.split(";");
				if (values.length < 3)
				{
					continue;
				}
				
				final ItemInstance itemToSell = getInventory().getItemByItemId(Integer.parseInt(values[0]));
				if (itemToSell == null)
				{
					continue;
				}
				
				final int[] elemDefAttr =
				{
				        0, 0, 0, 0, 0, 0
				};
				
				for (byte i = 0; i < 6; i++)
				{
					elemDefAttr[i] = itemToSell.getElementDefAttr(i);
				}
				
				getBuyList().addItemByItemId(Integer.parseInt(values[0]), itemToSell.getEnchantLevel(), Long.parseLong(values[1]), Long.parseLong(values[2]), null, -1, -9999, itemToSell.getAttackElementType(), itemToSell.getAttackElementPower(), elemDefAttr);
			}
			var = getVar("buystorename");
			if (var != null)
			{
				getBuyList().setTitle(var);
			}
		}
		var = getVar("createlist");
		if (var != null)
		{
			final String[] items = var.split(":");
			for (final String item : items)
			{
				if (item.equals(""))
				{
					continue;
				}
				final String[] values = item.split(";");
				if (values.length < 2)
				{
					continue;
				}
				final int recId = Integer.parseInt(values[0]);
				final long price = Long.parseLong(values[1]);
				getManufactureItems().put(recId, new ManufactureItemTemplate(recId, price));
			}
			var = getVar("manufacturename");
			if (var != null)
			{
				setStoreName(var);
			}
		}
	}
	
	public void saveTradeList()
	{
		final StringBuilder tradeListBuilder = new StringBuilder();
		
		if (_sellList == null)
		{
			unsetVar("selllist");
		}
		else
		{
			for (final TradeItem i : getSellList().getItems())
			{
				tradeListBuilder.append(i.getObjectId()).append(";").append(i.getCount()).append(";").append(i.getPrice()).append(":");
			}
			setVar("selllist", tradeListBuilder.toString(), -1);
			tradeListBuilder.delete(0, tradeListBuilder.length());
			if (getSellList().getTitle() != null)
			{
				setVar("sellstorename", getSellList().getTitle(), -1);
			}
		}
		
		if (_buyList == null)
		{
			unsetVar("buylist");
		}
		else
		{
			for (final TradeItem i : getBuyList().getItems())
			{
				tradeListBuilder.append(i.getItem().getId()).append(";").append(i.getCount()).append(";").append(i.getPrice()).append(":");
			}
			setVar("buylist", tradeListBuilder.toString(), -1);
			tradeListBuilder.delete(0, tradeListBuilder.length());
			if (getBuyList().getTitle() != null)
			{
				setVar("buystorename", getBuyList().getTitle(), -1);
			}
		}
		
		if ((_manufactureItems == null) || _manufactureItems.isEmpty())
		{
			unsetVar("createlist");
		}
		else
		{
			for (final ManufactureItemTemplate i : getManufactureItems().values())
			{
				tradeListBuilder.append(i.getRecipeId()).append(";").append(i.getCost()).append(":");
			}
			setVar("createlist", tradeListBuilder.toString(), -1);
			if (getStoreName() != null)
			{
				setVar("manufacturename", getStoreName(), -1);
			}
		}
	}
	
	@Override
	public PremiumBonus getPremiumBonus()
	{
		return _bonus;
	}
	
	public PersonalTasks getPersonalTasks()
	{
		return _tasks;
	}
	
	@Override
	public boolean hasPremiumBonus()
	{
		return _bonus.isActive();
	}
	
	public void startTempHeroTask(long expirTime, int status)
	{
		getPersonalTasks().addTask(new TempHeroTask(expirTime - System.currentTimeMillis(), status));
	}
	
	public boolean isTempHero()
	{
		return getPersonalTasks().isActiveTask(3);
	}
	
	public void startPremiumTask(long expirTime)
	{
		if (!Config.USE_PREMIUMSERVICE)
		{
			return;
		}
		
		final PremiumBonus bonus = getPremiumBonus();
		if (bonus.isActive())
		{
			final PremiumTemplate template = PremiumAccountsParser.getInstance().getPremiumTemplate(bonus.getPremiumId());
			if (template != null)
			{
				long taskTime = 0L;
				if (template.isOnlineType())
				{
					taskTime = (template.getTime() * 1000L) - expirTime;
					setPremiumOnlineTime();
				}
				else
				{
					taskTime = expirTime - System.currentTimeMillis();
				}
				getPersonalTasks().addTask(new PremiumTask(taskTime));
			}
		}
	}
	
	public void removePremiumAccount(boolean isSwitch)
	{
		if (isSwitch)
		{
			getPersonalTasks().removeTask(2, false);
		}
		
		final PremiumBonus bonus = getPremiumBonus();
		final int premiumId = bonus.getPremiumId();
		bonus.setPremiumId(0);
		bonus.setGroupId(0);
		bonus.setOnlineType(false);
		bonus.setActivate(false);
		if (bonus.isPersonal())
		{
			CharacterPremiumDAO.getInstance().disablePersonal(this);
			bonus.setIsPersonal(false);
		}
		else
		{
			CharacterPremiumDAO.getInstance().disable(this);
		}
		
		if (!isSwitch)
		{
			sendPacket(new ExBrPremiumState(getObjectId(), 0));
			sendPacket(SystemMessageId.THE_PREMIUM_ACCOUNT_HAS_BEEN_TERMINATED);
		}
		
		final PremiumTemplate template = PremiumAccountsParser.getInstance().getPremiumTemplate(premiumId);
		if (template != null)
		{
			if (!template.getBonusList().isEmpty())
			{
				for (final BonusType type : template.getBonusList().keySet())
				{
					if (type != null)
					{
						final double value = template.getBonusList().get(type);
						if (value > 0)
						{
							bonus.removeBonusType(type, value);
						}
					}
				}
			}
			
			for (final PremiumGift gift : template.getGifts())
			{
				if (gift != null && gift.isRemovable())
				{
					if (getInventory().getItemByItemId(gift.getId()) != null)
					{
						getInventory().destroyItemByItemId(gift.getId(), gift.getCount(), "Remove Premium");
						
					}
					else if (getWarehouse().getItemByItemId(gift.getId()) != null)
					{
						getWarehouse().destroyItemByItemId(gift.getId(), gift.getCount(), "Remove Premium");
					}
				}
			}
		}
		
		if (isInParty())
		{
			getParty().recalculatePartyData();
		}
		
		if (FarmSettings.ALLOW_AUTO_FARM && FarmSettings.PREMIUM_FARM_FREE && !isSwitch)
		{
			if (!getFarmSystem().isActiveFarmTask())
			{
				getFarmSystem().stopFarmTask(false);
			}
		}
	}
	
	private void setPremiumOnlineTime()
	{
		_premiumOnlineTime = System.currentTimeMillis();
	}
	
	public long getPremiumOnlineTime()
	{
		return _premiumOnlineTime;
	}
	
	public EnchantParams getEnchantParams()
	{
		return _enchantParams;
	}
	
	public void addVisual(final String type, final int skinId)
	{
		switch (type)
		{
			case "Weapon" :
				_weaponSkins.add(skinId);
				break;
			case "Armor" :
				_armorSkins.add(skinId);
				break;
			case "Shield" :
				_shieldSkins.add(skinId);
				break;
			case "Cloak" :
				_cloakSkins.add(skinId);
				break;
			case "Hair" :
				_hairSkins.add(skinId);
				break;
		}
		CharacterVisualDAO.getInstance().add(this, type, skinId);
	}
	
	public void removeVisual(final String type, final int skinId)
	{
		switch (type)
		{
			case "Weapon" :
				if (_activeWeaponSkin != null && _activeWeaponSkin.getId() == skinId)
				{
					broadcastPacket(new MagicSkillUse(this, this, 22217, 1, 0, 0));
					unVisualWeaponSkin(false);
					broadcastUserInfo(true);
				}
				_weaponSkins.remove(_weaponSkins.indexOf(Integer.valueOf(skinId)));
				break;
			case "Armor" :
				if (_activeArmorSkin != null && _activeArmorSkin.getId() == skinId)
				{
					broadcastPacket(new MagicSkillUse(this, this, 22217, 1, 0, 0));
					unVisualArmorSkin(false);
					broadcastUserInfo(true);
				}
				_armorSkins.remove(_armorSkins.indexOf(Integer.valueOf(skinId)));
				break;
			case "Shield" :
				if (_activeShieldSkin != null && _activeShieldSkin.getId() == skinId)
				{
					broadcastPacket(new MagicSkillUse(this, this, 22217, 1, 0, 0));
					unVisualShieldSkin(false);
					broadcastUserInfo(true);
				}
				_shieldSkins.remove(_shieldSkins.indexOf(Integer.valueOf(skinId)));
				break;
			case "Cloak" :
				if (_activeCloakSkin != null && _activeCloakSkin.getId() == skinId)
				{
					broadcastPacket(new MagicSkillUse(this, this, 22217, 1, 0, 0));
					unVisualCloakSkin(false);
					broadcastUserInfo(true);
				}
				_cloakSkins.remove(_cloakSkins.indexOf(Integer.valueOf(skinId)));
				break;
			case "Hair" :
				if (_activeHairSkin != null && _activeHairSkin.getId() == skinId)
				{
					broadcastPacket(new MagicSkillUse(this, this, 22217, 1, 0, 0));
					unVisualHairSkin(false);
					broadcastUserInfo(true);
				}
				_hairSkins.remove(_hairSkins.indexOf(Integer.valueOf(skinId)));
				break;
			case "Mask" :
				if (_activeMaskSkin != null && _activeMaskSkin.getId() == skinId)
				{
					broadcastPacket(new MagicSkillUse(this, this, 22217, 1, 0, 0));
					unVisualFaceSkin(false);
					broadcastUserInfo(true);
				}
				_hairSkins.remove(_hairSkins.indexOf(Integer.valueOf(skinId)));
				break;
		}
		CharacterVisualDAO.getInstance().remove(this, type.equals("Mask") ? "Hair" : type, skinId);
	}
	
	private void updateVisual(final String type, final int active, final int skinId)
	{
		CharacterVisualDAO.getInstance().update(this, type, active, skinId);
	}
	
	public List<Integer> getWeaponSkins()
	{
		return _weaponSkins;
	}
	
	public void addWeaponSkin(int id)
	{
		_weaponSkins.add(id);
	}
	
	public List<Integer> getArmorSkins()
	{
		return _armorSkins;
	}
	
	public void addArmorSkin(int id)
	{
		_armorSkins.add(id);
	}
	
	public List<Integer> getShieldSkins()
	{
		return _shieldSkins;
	}
	
	public void addShieldSkin(int id)
	{
		_shieldSkins.add(id);
	}
	
	public List<Integer> getCloakSkins()
	{
		return _cloakSkins;
	}
	
	public List<Integer> getHairSkins()
	{
		return _hairSkins;
	}
	
	public void addCloakSkin(int id)
	{
		_cloakSkins.add(id);
	}
	
	public void addHairSkin(int id)
	{
		_hairSkins.add(id);
	}
	
	public DressWeaponTemplate getActiveWeaponSkin()
	{
		return _activeWeaponSkin;
	}
	
	public DressArmorTemplate getActiveArmorSkin()
	{
		return _activeArmorSkin;
	}
	
	public DressShieldTemplate getActiveShieldSkin()
	{
		return _activeShieldSkin;
	}
	
	public DressCloakTemplate getActiveCloakSkin()
	{
		return _activeCloakSkin;
	}
	
	public DressHatTemplate getActiveHairSkin()
	{
		return _activeHairSkin;
	}
	
	public DressHatTemplate getActiveMaskSkin()
	{
		return _activeMaskSkin;
	}
	
	private boolean switchWeaponSkills(boolean isEnable, boolean sendSkillList)
	{
		boolean isFound = false;
		if (_activeWeaponSkin != null)
		{
			if ((isEnable && _activeWeapon) || (!isEnable && !_activeWeapon))
			{
				return isFound;
			}
			
			if (!_activeWeaponSkin.getSkills().isEmpty())
			{
				if (isEnable)
				{
					for (final Skill skill : _activeWeaponSkin.getSkills())
					{
						final Skill sk = getKnownSkill(skill.getId());
						if (sk != null && sk.getLevel() > skill.getLevel())
						{
							continue;
						}
						addSkill(skill, false);
						isFound = true;
					}
					_activeWeapon = true;
				}
				else
				{
					for (final Skill skill : _activeWeaponSkin.getSkills())
					{
						final Skill sk = getKnownSkill(skill.getId());
						if (sk != null && sk.getLevel() == skill.getLevel())
						{
							removeSkill(sk, false);
							isFound = true;
						}
					}
					_activeWeapon = false;
				}
			}
			
			if (isFound && sendSkillList)
			{
				sendSkillList(false);
			}
		}
		return isFound;
	}
	
	public void setActiveWeaponSkin(final int skinId, boolean separate)
	{
		boolean isFound = switchWeaponSkills(false, false);
		if (_activeWeaponSkin != null)
		{
			updateVisual("Weapon", 0, _activeWeaponSkin.getId());
		}
		_activeWeaponSkin = DressWeaponParser.getInstance().getWeapon(skinId);
		if (_activeWeaponSkin == null)
		{
			if (isFound)
			{
				sendSkillList(false);
			}
			return;
		}
		
		if (separate)
		{
			updateVisual("Weapon", 1, _activeWeaponSkin.getId());
		}
		
		if (hasWeaponForVisualEquipped(false) && !isFound)
		{
			isFound = true;
		}
		
		if (isFound)
		{
			sendSkillList(false);
		}
	}
	
	private boolean switchArmorSkills(boolean isEnable, boolean sendSkillList)
	{
		boolean isFound = false;
		if (_activeArmorSkin != null)
		{
			if ((isEnable && _activeArmor) || (!isEnable && !_activeArmor))
			{
				return isFound;
			}
			
			if (!_activeArmorSkin.getSkills().isEmpty())
			{
				if (isEnable)
				{
					for (final Skill skill : _activeArmorSkin.getSkills())
					{
						final Skill sk = getKnownSkill(skill.getId());
						if (sk != null && sk.getLevel() > skill.getLevel())
						{
							continue;
						}
						addSkill(skill, false);
						isFound = true;
					}
					_activeArmor = true;
				}
				else
				{
					for (final Skill skill : _activeArmorSkin.getSkills())
					{
						final Skill sk = getKnownSkill(skill.getId());
						if (sk != null && sk.getLevel() == skill.getLevel())
						{
							removeSkill(sk, false);
							isFound = true;
						}
					}
					_activeArmor = false;
				}
			}
			
			if (isFound && sendSkillList)
			{
				sendSkillList(false);
			}
		}
		return isFound;
	}
	
	public void setActiveArmorSkin(final int skinId, boolean separate)
	{
		boolean isFound = switchArmorSkills(false, false);
		if (_activeArmorSkin != null)
		{
			if (_activeArmorSkin.getShieldId() > 0)
			{
				unVisualShieldSkin(false);
			}
			
			if (_activeArmorSkin.getCloakId() > 0)
			{
				unVisualCloakSkin(false);
			}
			
			if (_activeArmorSkin.getHatId() > 0)
			{
				if (_activeArmorSkin.getSlot() == 3)
				{
					unVisualFaceSkin(false);
				}
				else
				{
					unVisualHairSkin(false);
				}
			}
			updateVisual("Armor", 0, _activeArmorSkin.getId());
		}
		_activeArmorSkin = DressArmorParser.getInstance().getArmor(skinId);
		if (_activeArmorSkin == null)
		{
			if (isFound)
			{
				sendSkillList(false);
			}
			return;
		}
		
		updateVisual("Armor", 1, _activeArmorSkin.getId());
		
		if (hasArmorForVisualEquipped(false) && !isFound)
		{
			isFound = true;
		}
		
		if (isFound)
		{
			sendSkillList(false);
		}
	}
	
	private boolean switchShieldSkills(boolean isEnable, boolean sendSkillList)
	{
		boolean isFound = false;
		if (_activeShieldSkin != null)
		{
			if ((isEnable && _activeShield) || (!isEnable && !_activeShield))
			{
				return isFound;
			}
			
			if (!_activeShieldSkin.getSkills().isEmpty())
			{
				if (isEnable)
				{
					for (final Skill skill : _activeShieldSkin.getSkills())
					{
						final Skill sk = getKnownSkill(skill.getId());
						if (sk != null && sk.getLevel() > skill.getLevel())
						{
							continue;
						}
						addSkill(skill, false);
						isFound = true;
					}
					_activeShield = true;
				}
				else
				{
					for (final Skill skill : _activeShieldSkin.getSkills())
					{
						final Skill sk = getKnownSkill(skill.getId());
						if (sk != null && sk.getLevel() == skill.getLevel())
						{
							removeSkill(sk, false);
							isFound = true;
						}
					}
					_activeShield = false;
				}
			}
			
			if (isFound && sendSkillList)
			{
				sendSkillList(false);
			}
		}
		return isFound;
	}
	
	public void setActiveShieldSkin(final int skinId, boolean separate)
	{
		boolean isFound = switchShieldSkills(false, false);
		if (_activeShieldSkin != null)
		{
			updateVisual("Shield", 0, _activeShieldSkin.getId());
		}
		
		_activeShieldSkin = DressShieldParser.getInstance().getShield(skinId);
		if (_activeShieldSkin == null)
		{
			if (isFound)
			{
				sendSkillList(false);
			}
			return;
		}
		
		if (separate)
		{
			updateVisual("Shield", 1, _activeShieldSkin.getId());
		}
		
		if (hasShieldForVisualEquipped(false) && !isFound)
		{
			isFound = true;
		}
		
		if (isFound)
		{
			sendSkillList(false);
		}
	}
	
	private boolean switchCloakSkills(boolean isEnable, boolean sendSkillList)
	{
		boolean isFound = false;
		if (_activeCloakSkin != null)
		{
			if ((isEnable && _activeCloak) || (!isEnable && !_activeCloak))
			{
				return isFound;
			}
			
			if (!_activeCloakSkin.getSkills().isEmpty())
			{
				if (isEnable)
				{
					for (final Skill skill : _activeCloakSkin.getSkills())
					{
						final Skill sk = getKnownSkill(skill.getId());
						if (sk != null && sk.getLevel() > skill.getLevel())
						{
							continue;
						}
						addSkill(skill, false);
						isFound = true;
					}
					_activeCloak = true;
				}
				else
				{
					for (final Skill skill : _activeCloakSkin.getSkills())
					{
						final Skill sk = getKnownSkill(skill.getId());
						if (sk != null && sk.getLevel() == skill.getLevel())
						{
							removeSkill(sk, false);
							isFound = true;
						}
					}
					_activeCloak = false;
				}
			}
			
			if (isFound && sendSkillList)
			{
				sendSkillList(false);
			}
		}
		return isFound;
	}
	
	public void setActiveCloakSkin(final int skinId, boolean separate)
	{
		boolean isFound = switchCloakSkills(false, false);
		if (_activeCloakSkin != null)
		{
			updateVisual("Cloak", 0, _activeCloakSkin.getId());
		}
		_activeCloakSkin = DressCloakParser.getInstance().getCloak(skinId);
		if (_activeCloakSkin == null)
		{
			if (isFound)
			{
				sendSkillList(false);
			}
			return;
		}
		
		if (separate)
		{
			updateVisual("Cloak", 1, _activeCloakSkin.getId());
		}
		
		if (hasCloakForVisualEquipped(false) && !isFound)
		{
			isFound = true;
		}
		
		if (isFound)
		{
			sendSkillList(false);
		}
	}
	
	private boolean switchHairSkills(boolean isEnable, boolean sendSkillList)
	{
		boolean isFound = false;
		if (_activeHairSkin != null)
		{
			if ((isEnable && _activeHair) || (!isEnable && !_activeHair))
			{
				return isFound;
			}
			
			if (!_activeHairSkin.getSkills().isEmpty())
			{
				if (isEnable)
				{
					for (final Skill skill : _activeHairSkin.getSkills())
					{
						final Skill sk = getKnownSkill(skill.getId());
						if (sk != null && sk.getLevel() > skill.getLevel())
						{
							continue;
						}
						addSkill(skill, false);
						isFound = true;
					}
					_activeHair = true;
				}
				else
				{
					for (final Skill skill : _activeHairSkin.getSkills())
					{
						final Skill sk = getKnownSkill(skill.getId());
						if (sk != null && sk.getLevel() == skill.getLevel())
						{
							removeSkill(sk, false);
							isFound = true;
						}
					}
					_activeHair = false;
				}
			}
			
			if (isFound && sendSkillList)
			{
				sendSkillList(false);
			}
		}
		return isFound;
	}
	
	public void setActiveHairSkin(final int skinId, boolean separate)
	{
		boolean isFound = switchHairSkills(false, false);
		if (_activeHairSkin != null)
		{
			updateVisual("Hair", 0, _activeHairSkin.getId());
		}
		_activeHairSkin = DressHatParser.getInstance().getHat(skinId);
		if (_activeHairSkin == null)
		{
			if (isFound)
			{
				sendSkillList(false);
			}
			return;
		}
		
		if (separate)
		{
			updateVisual("Hair", 1, _activeHairSkin.getId());
		}
		
		if (hasHairForVisualEquipped(false) && !isFound)
		{
			isFound = true;
		}
		
		if (isFound)
		{
			sendSkillList(false);
		}
	}
	
	private boolean switchMaskSkills(boolean isEnable, boolean sendSkillList)
	{
		boolean isFound = false;
		if (_activeMaskSkin != null)
		{
			if ((isEnable && _activeMask) || (!isEnable && !_activeMask))
			{
				return isFound;
			}
			
			if (!_activeMaskSkin.getSkills().isEmpty())
			{
				if (isEnable)
				{
					for (final Skill skill : _activeMaskSkin.getSkills())
					{
						final Skill sk = getKnownSkill(skill.getId());
						if (sk != null && sk.getLevel() > skill.getLevel())
						{
							continue;
						}
						addSkill(skill, false);
						isFound = true;
					}
					_activeMask = true;
				}
				else
				{
					for (final Skill skill : _activeMaskSkin.getSkills())
					{
						final Skill sk = getKnownSkill(skill.getId());
						if (sk != null && sk.getLevel() == skill.getLevel())
						{
							removeSkill(sk, false);
							isFound = true;
						}
					}
					_activeMask = false;
				}
			}
			
			if (isFound && sendSkillList)
			{
				sendSkillList(false);
			}
		}
		return isFound;
	}
	
	public void setActiveMaskSkin(final int skinId, boolean separate)
	{
		boolean isFound = switchMaskSkills(false, false);
		if (_activeMaskSkin != null)
		{
			updateVisual("Hair", 0, _activeMaskSkin.getId());
		}
		_activeMaskSkin = DressHatParser.getInstance().getHat(skinId);
		if (_activeMaskSkin == null)
		{
			if (isFound)
			{
				sendSkillList(false);
			}
			return;
		}
		
		if (separate)
		{
			updateVisual("Hair", 1, _activeMaskSkin.getId());
		}
		
		if (hasFaceForVisualEquipped(false) && !isFound)
		{
			isFound = true;
		}
		
		if (isFound)
		{
			sendSkillList(false);
		}
	}
	
	public boolean hasHairForVisualEquipped(boolean sendSkillList)
	{
		if (_activeHairSkin == null)
		{
			switchHairSkills(false, sendSkillList);
			return false;
		}
		
		final Item template = ItemsParser.getInstance().getTemplate(_activeHairSkin.getHatId());
		if (template == null)
		{
			switchHairSkills(false, sendSkillList);
			return false;
		}
		
		if (_activeArmorSkin != null && _activeArmorSkin.getHatId() > 0 && !_activeArmorSkin.isCheckEquip())
		{
			switchHairSkills(true, sendSkillList);
			return true;
		}
		
		final int paperdoll = Inventory.getPaperdollIndex(template.getBodyPart());
		if (paperdoll < 0)
		{
			switchHairSkills(false, sendSkillList);
			return false;
		}
		
		final ItemInstance item = getInventory().getPaperdollItem(Inventory.PAPERDOLL_HAIR2);
		if (item != null)
		{
			final Item itemTemp = ItemsParser.getInstance().getTemplate(item.getId());
			if (itemTemp.getBodyPart() != template.getBodyPart())
			{
				switchHairSkills(false, sendSkillList);
				return false;
			}
		}
		
		final ItemInstance slot = getInventory().getPaperdollItem(paperdoll);
		if (slot == null)
		{
			switchHairSkills(false, sendSkillList);
			return false;
		}
		
		if ((template.getBodyPart() == Item.SLOT_HAIRALL) && (_activeMaskSkin != null))
		{
			switchHairSkills(false, sendSkillList);
			return false;
		}
		switchHairSkills(true, sendSkillList);
		return true;
	}
	
	public boolean hasFaceForVisualEquipped(boolean sendSkillList)
	{
		if (_activeMaskSkin == null)
		{
			switchMaskSkills(false, sendSkillList);
			return false;
		}
		
		final Item template = ItemsParser.getInstance().getTemplate(_activeMaskSkin.getHatId());
		if (template == null)
		{
			switchMaskSkills(false, sendSkillList);
			return false;
		}
		
		if (_activeArmorSkin != null && _activeArmorSkin.getHatId() > 0 && !_activeArmorSkin.isCheckEquip())
		{
			switchMaskSkills(true, sendSkillList);
			return true;
		}
		
		final int paperdoll = Inventory.getPaperdollIndex(template.getBodyPart());
		if (paperdoll < 0)
		{
			switchMaskSkills(false, sendSkillList);
			return false;
		}
		
		final ItemInstance item = getInventory().getPaperdollItem(Inventory.PAPERDOLL_HAIR);
		if (item != null)
		{
			final Item itemTemp = ItemsParser.getInstance().getTemplate(item.getId());
			if (itemTemp.getBodyPart() != template.getBodyPart())
			{
				switchMaskSkills(false, sendSkillList);
				return false;
			}
		}
		
		final ItemInstance slot = getInventory().getPaperdollItem(paperdoll);
		if (slot == null)
		{
			switchMaskSkills(false, sendSkillList);
			return false;
		}
		switchMaskSkills(true, sendSkillList);
		return true;
	}
	
	public boolean hasArmorForVisualEquipped(boolean sendSkillList)
	{
		final ItemInstance chestItem = getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST);
		final ItemInstance legsItem = getInventory().getPaperdollItem(Inventory.PAPERDOLL_LEGS);
		final ItemInstance glovesItem = getInventory().getPaperdollItem(Inventory.PAPERDOLL_GLOVES);
		final ItemInstance feetItem = getInventory().getPaperdollItem(Inventory.PAPERDOLL_FEET);
		if (((chestItem == null) || (glovesItem == null) || (feetItem == null)) || (legsItem == null && chestItem.getItem().getBodyPart() != Item.SLOT_FULL_ARMOR))
		{
			switchArmorSkills(false, sendSkillList);
			return false;
		}
		else
		{
			switchArmorSkills(true, sendSkillList);
			return true;
		}
	}
	
	public boolean hasWeaponForVisualEquipped(boolean sendSkillList)
	{
		final ItemInstance weapon = getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
		if (weapon == null || _activeWeaponSkin == null || !weapon.getItemType().toString().equals(_activeWeaponSkin.getType()))
		{
			switchWeaponSkills(false, sendSkillList);
			return false;
		}
		else
		{
			switchWeaponSkills(true, sendSkillList);
			return true;
		}
	}
	
	public boolean hasShieldForVisualEquipped(boolean sendSkillList)
	{
		if (_activeArmorSkin != null && _activeArmorSkin.getShieldId() > 0 && !_activeArmorSkin.isCheckEquip())
		{
			switchShieldSkills(true, sendSkillList);
			return true;
		}
		
		final ItemInstance shield = getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
		if (shield == null || (shield != null && shield.getItem().isArrow()))
		{
			switchShieldSkills(false, sendSkillList);
			return false;
		}
		switchShieldSkills(true, sendSkillList);
		return true;
	}
	
	public boolean hasCloakForVisualEquipped(boolean sendSkillList)
	{
		if (_activeArmorSkin != null && _activeArmorSkin.getCloakId() > 0 && !_activeArmorSkin.isCheckEquip())
		{
			switchCloakSkills(true, sendSkillList);
			return true;
		}
		
		final ItemInstance cloak = getInventory().getPaperdollItem(Inventory.PAPERDOLL_CLOAK);
		if (cloak == null)
		{
			switchCloakSkills(false, sendSkillList);
			return false;
		}
		switchCloakSkills(true, sendSkillList);
		return true;
	}
	
	public void unVisualArmorSkin(boolean separate)
	{
		if (_activeArmorSkin != null)
		{
			switchArmorSkills(false, separate);
			if (separate)
			{
				updateVisual("Armor", 0, _activeArmorSkin.getId());
			}
			_activeArmorSkin = null;
		}
	}
	
	public void unVisualWeaponSkin(boolean separate)
	{
		if (_activeWeaponSkin != null)
		{
			switchWeaponSkills(false, separate);
			if (separate)
			{
				updateVisual("Weapon", 0, _activeWeaponSkin.getId());
			}
			_activeWeaponSkin = null;
		}
	}
	
	public void unVisualShieldSkin(boolean separate)
	{
		if (_activeShieldSkin != null)
		{
			switchShieldSkills(false, separate);
			if (separate)
			{
				updateVisual("Shield", 0, _activeShieldSkin.getId());
			}
			_activeShieldSkin = null;
		}
	}
	
	public void unVisualCloakSkin(boolean separate)
	{
		if (_activeCloakSkin != null)
		{
			switchCloakSkills(false, separate);
			if (separate)
			{
				updateVisual("Cloak", 0, _activeCloakSkin.getId());
			}
			_activeCloakSkin = null;
		}
	}
	
	public void unVisualHairSkin(boolean separate)
	{
		if (_activeHairSkin != null)
		{
			switchHairSkills(false, separate);
			if (separate)
			{
				updateVisual("Hair", 0, _activeHairSkin.getId());
			}
			_activeHairSkin = null;
		}
	}
	
	public void unVisualFaceSkin(boolean separate)
	{
		if (_activeMaskSkin != null)
		{
			switchMaskSkills(false, true);
			if (separate)
			{
				updateVisual("Hair", 0, _activeMaskSkin.getId());
			}
			_activeMaskSkin = null;
		}
	}
	
	public List<PlayerScheme> getBuffSchemes()
	{
		return _buffSchemes;
	}
	
	public PlayerScheme getBuffSchemeById(final int id)
	{
		for (final PlayerScheme scheme : _buffSchemes)
		{
			if (scheme.getSchemeId() == id)
			{
				return scheme;
			}
		}
		return null;
	}
	
	public PlayerScheme getBuffSchemeByName(final String name)
	{
		for (final PlayerScheme scheme : _buffSchemes)
		{
			if (scheme.getName().equals(name))
			{
				return scheme;
			}
		}
		return null;
	}
	
	public boolean isInSameParty(Player target)
	{
		return (((getParty() != null) && (target != null) && (target.getParty() != null)) && (getParty() == target.getParty()));
	}
	
	public boolean isInSameChannel(Player target)
	{
		return (((getParty() != null) && (target != null) && (target.getParty() != null)) && (getParty().getCommandChannel() != null) && (target.getParty().getCommandChannel() != null) && (getParty().getCommandChannel() == target.getParty().getCommandChannel()));
	}
	
	public boolean isInSameClan(Player target)
	{
		return (((getClan() != null) && (target != null) && (target.getClan() != null)) && (getClanId() == target.getClanId()));
	}
	
	public final boolean isInSameAlly(final Player target)
	{
		return (((getAllyId() != 0) && (target != null) && (target.getAllyId() != 0)) && (getAllyId() == target.getAllyId()));
	}
	
	public boolean isInTwoSidedWar(Player target)
	{
		final Clan aClan = getClan();
		final Clan tClan = target.getClan();
		if ((aClan != null) && (tClan != null))
		{
			if (aClan.isAtWarWith(tClan.getId()) && tClan.isAtWarWith(aClan.getId()))
			{
				return true;
			}
		}
		return false;
	}
	
	public class BroadcastCharInfoTask implements Runnable
	{
		@Override
		public void run()
		{
			broadcastCharInfoImpl();
			_broadcastCharInfoTask = null;
		}
	}
	
	@Override
	public void broadcastCharInfo(UserInfoType... types)
	{
		broadcastUserInfo(false);
	}
	
	public void broadcastUserInfo(boolean force)
	{
		sendUserInfo(force);
		if (Config.BROADCAST_CHAR_INFO_INTERVAL == 0)
		{
			force = true;
		}
		
		if (force)
		{
			final var oldTask = _broadcastCharInfoTask;
			if (oldTask != null)
			{
				_broadcastCharInfoTask = null;
				oldTask.cancel(false);
			}
			broadcastCharInfoImpl();
			return;
		}
		
		if (_broadcastCharInfoTask != null)
		{
			return;
		}
		_broadcastCharInfoTask = ThreadPoolManager.getInstance().schedule(new BroadcastCharInfoTask(), Config.BROADCAST_CHAR_INFO_INTERVAL);
	}
	
	@Override
	public void broadcastCharInfoImpl()
	{
		final GameServerPacket exCi = new ExBrExtraUserInfo(this);
		final GameServerPacket dominion = TerritoryWarManager.getInstance().isTWInProgress() && (TerritoryWarManager.getInstance().checkIsRegistered(-1, getObjectId()) || TerritoryWarManager.getInstance().checkIsRegistered(-1, getClan())) ? new ExDominionWarStart(this) : null;
		
		for (final Player player : World.getInstance().getAroundPlayers(this))
		{
			if (player.getNotShowTraders() && isInStoreNow())
			{
				player.sendPacket(new DeleteObject(this));
				continue;
			}
			
			player.sendPacket(isPolymorphed() ? new NpcInfoPoly(this, player) : new CharInfo(this, player), exCi);
			player.sendPacket(RelationChanged.update(player, this, player));
			if (dominion != null)
			{
				player.sendPacket(dominion);
			}
		}
	}
	
	public void broadcastCharInfoAround()
	{
		final var oldTask = _broadcastCharInfoTask;
		if (oldTask != null)
		{
			_broadcastCharInfoTask = null;
			oldTask.cancel(false);
		}
		_broadcastCharInfoTask = ThreadPoolManager.getInstance().schedule(new BroadcastCharInfoAround(), Config.BROADCAST_CHAR_INFO_INTERVAL);
	}
	
	public class BroadcastCharInfoAround implements Runnable
	{
		@Override
		public void run()
		{
			broadcastCharAroundImpl();
			_broadcastCharInfoTask = null;
		}
	}
	
	private void broadcastCharAroundImpl()
	{
		for (final Player player : World.getInstance().getAroundPlayers(this))
		{
			if (player != null && player.isOnline())
			{
				sendPacket(player.isPolymorphed() ? new NpcInfoPoly(player, this) : new CharInfo(player, this));
			}
		}
	}
	
	private class UserInfoTask implements Runnable
	{
		@Override
		public void run()
		{
			sendUserInfoImpl();
			_userInfoTask = null;
		}
	}
	
	private void sendUserInfoImpl()
	{
		sendPacket(new UserInfo(this, null), new ExBrExtraUserInfo(this));
		if (TerritoryWarManager.getInstance().isTWInProgress() && (TerritoryWarManager.getInstance().checkIsRegistered(-1, getObjectId()) || TerritoryWarManager.getInstance().checkIsRegistered(-1, getClan())))
		{
			sendPacket(new ExDominionWarStart(this));
		}
	}
	
	public void sendPreviewUserInfo(int weapon, int shield, int gloves, int chest, int legs, int feet, int cloak, int hair, int mask)
	{
		final int visualId = getVarInt("visualBuff", 0);
		final boolean isCostumeBuff = visualId > 0 && !isTransformed();
		if ((_activeWeaponSkin != null) && hasWeaponForVisualEquipped(true) && weapon == 0)
		{
			if (shield > 0)
			{
				_previewSlots[0] = 0;
				_previewSlots[9] = 0;
				_previewSlots[10] = 0;
			}
			else
			{
				_previewSlots[0] = _activeWeaponSkin.getId();
				_previewSlots[9] = _activeWeaponSkin.isAllowEnchant() ? getEnchantEffect() : 0;
				_previewSlots[10] = _activeWeaponSkin.isAllowAugment() ? getInventory().getPaperdollAugmentationId(Inventory.PAPERDOLL_RHAND) : 0;
			}
		}
		else
		{
			if (Config.ALLOW_VISUAL_ARMOR_COMMAND && weapon == 0)
			{
				if (shield > 0)
				{
					_previewSlots[0] = 0;
					_previewSlots[9] = 0;
					_previewSlots[10] = 0;
				}
				else
				{
					final ItemInstance item = getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
					if (item != null && item.getVisualItemId() > 0)
					{
						final DressWeaponTemplate wp = DressWeaponParser.getInstance().getWeapon(item.getVisualItemId());
						_previewSlots[0] = wp != null ? wp.getId() : 0;
						_previewSlots[9] = wp != null && wp.isAllowEnchant() ? getEnchantEffect() : 0;
						_previewSlots[10] = wp != null && wp.isAllowAugment() ? getInventory().getPaperdollAugmentationId(Inventory.PAPERDOLL_RHAND) : 0;
					}
					else
					{
						_previewSlots[0] = getInventory().getPaperdollItemDisplayId(Inventory.PAPERDOLL_RHAND);
						_previewSlots[9] = getEnchantEffect();
						_previewSlots[10] = getInventory().getPaperdollAugmentationId(Inventory.PAPERDOLL_RHAND);
					}
				}
			}
			else
			{
				if (weapon > 0)
				{
					if (shield > 0)
					{
						_previewSlots[0] = 0;
						_previewSlots[9] = 0;
						_previewSlots[10] = 0;
					}
					else
					{
						_previewSlots[0] = weapon;
						_previewSlots[9] = 0;
						_previewSlots[10] = 0;
					}
				}
				else
				{
					_previewSlots[0] = getInventory().getPaperdollItemDisplayId(Inventory.PAPERDOLL_RHAND);
					_previewSlots[9] = getEnchantEffect();
					_previewSlots[10] = getInventory().getPaperdollAugmentationId(Inventory.PAPERDOLL_RHAND);
				}
			}
		}
		
		if ((_activeShieldSkin != null) && hasShieldForVisualEquipped(true) && shield == 0)
		{
			_previewSlots[1] = _activeShieldSkin.getShieldId();
		}
		else
		{
			_previewSlots[1] = shield == -1 ? 0 : shield > 0 ? shield : Config.ALLOW_VISUAL_ARMOR_COMMAND ? getInventory().getPaperdollVisualItemId(Inventory.PAPERDOLL_LHAND) : getInventory().getPaperdollItemDisplayId(Inventory.PAPERDOLL_LHAND);
		}
		
		if (isCostumeBuff && chest == 0)
		{
			final DressArmorTemplate tpl = DressArmorParser.getInstance().getArmor(visualId);
			_previewSlots[2] = tpl != null ? tpl.getGloves() : getInventory().getPaperdollItemDisplayId(Inventory.PAPERDOLL_GLOVES);
			_previewSlots[3] = tpl != null ? tpl.getChest() : getInventory().getPaperdollItemDisplayId(Inventory.PAPERDOLL_CHEST);
			_previewSlots[4] = tpl != null ? tpl.getLegs() : getInventory().getPaperdollItemDisplayId(Inventory.PAPERDOLL_LEGS);
			_previewSlots[5] = tpl != null ? tpl.getFeet() : getInventory().getPaperdollItemDisplayId(Inventory.PAPERDOLL_FEET);
		}
		else if ((_activeArmorSkin != null) && hasArmorForVisualEquipped(true) && chest == 0)
		{
			_previewSlots[2] = _activeArmorSkin.getGloves();
			_previewSlots[3] = _activeArmorSkin.getChest();
			_previewSlots[4] = _activeArmorSkin.getLegs();
			_previewSlots[5] = _activeArmorSkin.getFeet();
		}
		else
		{
			_previewSlots[2] = gloves == -1 ? 0 : gloves > 0 ? gloves : Config.ALLOW_VISUAL_ARMOR_COMMAND ? getInventory().getPaperdollVisualItemId(Inventory.PAPERDOLL_GLOVES) : getInventory().getPaperdollItemDisplayId(Inventory.PAPERDOLL_GLOVES);
			_previewSlots[3] = chest > 0 ? chest : Config.ALLOW_VISUAL_ARMOR_COMMAND ? getInventory().getPaperdollVisualItemId(Inventory.PAPERDOLL_CHEST) : getInventory().getPaperdollItemDisplayId(Inventory.PAPERDOLL_CHEST);
			_previewSlots[4] = legs == -1 ? 0 : legs > 0 ? legs : Config.ALLOW_VISUAL_ARMOR_COMMAND ? getInventory().getPaperdollVisualItemId(Inventory.PAPERDOLL_LEGS) : getInventory().getPaperdollItemDisplayId(Inventory.PAPERDOLL_LEGS);
			_previewSlots[5] = feet == -1 ? 0 : feet > 0 ? feet : Config.ALLOW_VISUAL_ARMOR_COMMAND ? getInventory().getPaperdollVisualItemId(Inventory.PAPERDOLL_FEET) : getInventory().getPaperdollItemDisplayId(Inventory.PAPERDOLL_FEET);
		}
		
		if (isCostumeBuff && (cloak == 0 || cloak == -1))
		{
			final DressArmorTemplate tpl = DressArmorParser.getInstance().getArmor(visualId);
			_previewSlots[6] = tpl != null ? tpl.getCloakId() : getInventory().getPaperdollItemDisplayId(Inventory.PAPERDOLL_CLOAK);
		}
		if ((_activeCloakSkin != null) && hasCloakForVisualEquipped(true) && (cloak == 0 || cloak == -1))
		{
			_previewSlots[6] = _activeCloakSkin.getCloakId();
		}
		else
		{
			_previewSlots[6] = cloak > 0 ? cloak : Config.ALLOW_VISUAL_ARMOR_COMMAND ? getInventory().getPaperdollVisualItemId(Inventory.PAPERDOLL_CLOAK) : getInventory().getPaperdollItemDisplayId(Inventory.PAPERDOLL_CLOAK);
		}
		
		if ((_activeMaskSkin != null) && hasFaceForVisualEquipped(true) && mask == 0)
		{
			_previewSlots[7] = _activeMaskSkin.getSlot() == 3 ? _activeMaskSkin.getHatId() : 0;
		}
		else
		{
			_previewSlots[7] = mask == -1 ? 0 : mask > 0 ? mask : Config.ALLOW_VISUAL_ARMOR_COMMAND ? getInventory().getPaperdollVisualItemId(Inventory.PAPERDOLL_HAIR) : getInventory().getPaperdollItemDisplayId(Inventory.PAPERDOLL_HAIR);
		}
		
		if ((_activeHairSkin != null) && hasHairForVisualEquipped(true) && hair == 0)
		{
			_previewSlots[8] = _activeHairSkin.getSlot() == 2 ? _activeHairSkin.getHatId() : 0;
		}
		else
		{
			_previewSlots[8] = hair == -1 ? 0 : hair > 0 ? hair : Config.ALLOW_VISUAL_ARMOR_COMMAND ? getInventory().getPaperdollVisualItemId(Inventory.PAPERDOLL_HAIR2) : getInventory().getPaperdollItemDisplayId(Inventory.PAPERDOLL_HAIR2);
		}
		sendPacket(new UserInfo(this, _previewSlots));
	}
	
	public void sendUserInfo(UserInfoType... types)
	{
		sendUserInfo(false);
	}
	
	public void sendUserInfo(boolean force)
	{
		if (isLogoutStarted() || isFakePlayer() || _entering)
		{
			return;
		}
		
		if (Config.USER_INFO_INTERVAL == 0 || force)
		{
			final var oldTask = _userInfoTask;
			if (oldTask != null)
			{
				_userInfoTask = null;
				oldTask.cancel(false);
			}
			sendUserInfoImpl();
			return;
		}
		
		if (_userInfoTask != null)
		{
			return;
		}
		_userInfoTask = ThreadPoolManager.getInstance().schedule(new UserInfoTask(), Config.USER_INFO_INTERVAL);
	}
	
	@Override
	public void updateEffectIcons(boolean partyOnly)
	{
		if (_entering || isLogoutStarted())
		{
			return;
		}
		_effects.updateEffectIcons(partyOnly, true);
	}
	
	public void broadcastRelationChanged()
	{
		for (final Player player : World.getInstance().getAroundPlayers(this))
		{
			player.sendPacket(RelationChanged.update(player, this, player));
			if (hasSummon())
			{
				player.sendPacket(RelationChanged.update(player, getSummon(), player));
			}
		}
	}
	
	public boolean isFriend(Player target)
	{
		return isFriend(target, true);
	}
	
	public boolean isFriend(Player target, boolean isAll)
	{
		if (target == this)
		{
			return true;
		}
		
		final var isInSameParty = isInSameParty(target);
		final var isInSameChannel = isInSameChannel(target);
		final var isInSameClan = isInSameClan(target);
		final var isInSameAlly = isInSameAlly(target);
		final var isInsideZone = isInsideZone(ZoneId.SIEGE);
		final var targetInsideZone = target.isInsideZone(ZoneId.SIEGE);
		final var isInsidePvpZone = isInsideZone(ZoneId.PVP);
		final var targetInsidePvpZone = target.isInsideZone(ZoneId.PVP);
		
		if ((getDuelState() == Duel.DUELSTATE_DUELLING) && (getDuelId() == target.getActingPlayer().getDuelId()))
		{
			final var duel = DuelManager.getInstance().getDuel(getDuelId());
			if (duel.isPartyDuel())
			{
				final var partyA = getParty();
				final var partyB = target.getParty();
				if (partyA != null && partyA.getMembers().contains(target))
				{
					return true;
				}
				
				if (partyB != null && partyB.getMembers().contains(this))
				{
					return true;
				}
				return false;
			}
			return false;
		}
		
		if (isInSameParty || isInSameChannel)
		{
			return true;
		}
		
		for (final var e : getFightEvents())
		{
			if (e != null)
			{
				return e.canAttack(target, this) ? false : true;
			}
		}

		var e = getPartyTournament();
		if (e != null)
		{
			return e.canAttack(target, this) ? false : true;
		}

		if (!target.isInsideZone(ZoneId.FUN_PVP))
		{
			if (isInsidePvpZone && targetInsidePvpZone && !isInsideZone && !targetInsideZone)
			{
				return false;
			}
		}
		
		if (isInOlympiadMode() && target.isInOlympiadMode())
		{
			if (getOlympiadGameId() == target.getOlympiadGameId())
			{
				return false;
			}
		}
		
		if (isInTwoSidedWar(target))
		{
			return false;
		}
		
		if (target.isInPvpFunZone() && isInPvpFunZone())
		{
			final var zone = ZoneManager.getInstance().getZone(target, FunPvpZone.class);
			if (zone != null)
			{
				if (!isInSameParty && !isInSameChannel && (zone.canAttackAllies() || !isInSameClan && !isInSameAlly))
				{
					return false;
				}
			}
		}
		
		if (isInSameClan || isInSameAlly)
		{
			return true;
		}
		
		if (isInsideZone && isInSiege() && (getSiegeState() != 0) && (target.getSiegeState() != 0))
		{
			final var siege = SiegeManager.getInstance().getSiege(getX(), getY(), getZ());
			if (siege != null)
			{
				final var siegeZone = ZoneManager.getInstance().getZone(this, SiegeZone.class);
				if ((siege.checkIsDefender(getClan()) && siege.checkIsDefender(target.getClan())))
				{
					return true;
				}
				if (siegeZone != null && (siege.checkIsAttacker(getClan()) && siege.checkIsAttacker(target.getClan())) && !siegeZone.isAttackSameSiegeSide())
				{
					return true;
				}
				return false;
			}
		}
		
		if ((isInsidePvpZone && targetInsidePvpZone) && (isInsideZone && targetInsideZone))
		{
			return false;
		}
		
		if (target.getPvpFlag() > 0 || target.getKarma() > 0 || isCursedWeaponEquipped())
		{
			if (!isInSameParty && !isInSameChannel && !isInSameClan && !isInSameAlly)
			{
				return false;
			}
		}
		return isAll ? true : false;
	}
	
	public int getVipLevel()
	{
		return _vipLevel;
	}
	
	public void setVipLevel(final int level)
	{
		_vipLevel = level;
		getPremiumBonus().setVipTemplate(_vipLevel);
	}
	
	public long getVipPoints()
	{
		return _vipPoints;
	}
	
	public void refreshVipPoints()
	{
		_vipPoints = 0L;
	}
	
	public void setVipPoints(final long point)
	{
		if (point > VipManager.getInstance().getMaxPoints())
		{
			_vipPoints = VipManager.getInstance().getMaxPoints();
		}
		else
		{
			_vipPoints = point;
		}
		
		if (getPremiumBonus().getVipTemplate().getId() < VipManager.getInstance().getMaxLevel())
		{
			final VipTemplate tmp = VipManager.getInstance().getVipLevel(_vipLevel + 1);
			if (tmp != null)
			{
				if (_vipPoints >= tmp.getPoints())
				{
					setVipLevel(tmp.getId());
					if (!tmp.getRewardItems().isEmpty())
					{
						for (final ItemHolder holder : tmp.getRewardItems())
						{
							addItem("VIPReward", holder.getId(), holder.getCount(), this, true);
						}
					}
					_vipPoints = _vipPoints > tmp.getPoints() ? _vipPoints - tmp.getPoints() : 0;
				}
			}
		}
	}
	
	@Override
	public double getColRadius()
	{
		if (isMounted() && (getMountNpcId() > 0))
		{
			return NpcsParser.getInstance().getTemplate(getMountNpcId()).getfCollisionRadius();
		}
		else if (isTransformed())
		{
			return getTransformation().getCollisionRadius(this);
		}
		return getAppearance().getSex() ? getBaseTemplate().getFCollisionRadiusFemale() : getBaseTemplate().getfCollisionRadius();
	}
	
	@Override
	public double getColHeight()
	{
		if (isMounted() && (getMountNpcId() > 0))
		{
			return NpcsParser.getInstance().getTemplate(getMountNpcId()).getfCollisionHeight();
		}
		else if (isTransformed())
		{
			return getTransformation().getCollisionHeight(this);
		}
		return getAppearance().getSex() ? getBaseTemplate().getFCollisionHeightFemale() : getBaseTemplate().getfCollisionHeight();
	}
	
	public void setServitorShare(Map<Stats, Double> map)
	{
		_servitorShare = map;
	}
	
	public final double getServitorShareBonus(Stats stat)
	{
		if (_servitorShare != null && _servitorShare.containsKey(stat))
		{
			return _servitorShare.get(stat);
		}
		return 1.0D;
	}
	
	public void setPetShare(Map<Stats, Double> map)
	{
		_petShare = map;
	}
	
	public final double getPetShareBonus(Stats stat)
	{
		if (_petShare != null && _petShare.containsKey(stat))
		{
			return _petShare.get(stat);
		}
		return 1.0D;
	}
	
	public List<Integer> getRevengeList()
	{
		return _revengeList;
	}
	
	public void addRevengeId(int charId)
	{
		if (_revengeList.size() >= 10)
		{
			return;
		}
		
		if (!_revengeList.contains(charId))
		{
			_revengeList.add(charId);
		}
		getRevengeMark();
	}
	
	public void removeRevengeId(int objectId)
	{
		String line = "";
		int amount = 0;
		for (final int charId : _revengeList)
		{
			if (charId == objectId)
			{
				continue;
			}
			amount++;
			line += "" + charId + "";
			if (amount < (_revengeList.size() - 1))
			{
				line += ";";
			}
			
		}
		_revengeList.clear();
		
		if (!line.isEmpty())
		{
			final String[] targets = line.split(";");
			for (final String charId : targets)
			{
				_revengeList.add(Integer.parseInt(charId));
			}
		}
		getRevengeMark();
	}
	
	public void clenUpRevengeList()
	{
		_revengeList.clear();
	}
	
	public void getRevengeMark()
	{
		if (_revengeList != null && !_revengeList.isEmpty())
		{
			sendPacket(new ShowTutorialMark(1002, 0));
		}
	}
	
	public boolean isRevengeActive()
	{
		return _isRevengeActive;
	}
	
	public void setRevengeActive(boolean active)
	{
		_isRevengeActive = active;
	}
	
	public String saveRevergeList()
	{
		if (_revengeList != null && !_revengeList.isEmpty())
		{
			String line = "";
			int amount = 0;
			for (final int charId : _revengeList)
			{
				amount++;
				line += "" + charId + "";
				if (amount < _revengeList.size())
				{
					line += ";";
				}
			}
			return line;
		}
		return "";
	}
	
	public void loadRevergeList(String line)
	{
		if (line != null && !line.isEmpty())
		{
			final String[] targets = line.split(";");
			for (final String charId : targets)
			{
				_revengeList.add(Integer.parseInt(charId));
			}
		}
	}
	
	@Override
	public void addInfoObject(GameObject object)
	{
		if (isLogoutStarted())
		{
			return;
		}
		sendInfoFrom(object);
	}
	
	@Override
	public void removeInfoObject(GameObject object)
	{
		if (isLogoutStarted())
		{
			return;
		}
		super.removeInfoObject(object);
		if (object instanceof AirShipInstance)
		{
			if ((((AirShipInstance) object).getCaptainId() != 0) && (((AirShipInstance) object).getCaptainId() != getObjectId()))
			{
				sendPacket(new DeleteObject(((AirShipInstance) object).getCaptainId()));
			}
			if (((AirShipInstance) object).getHelmObjectId() != 0)
			{
				sendPacket(new DeleteObject(((AirShipInstance) object).getHelmObjectId()));
			}
		}
		final var npc = object.getActingNpc();
		if (npc != null && npc.getTriggerId() != 0)
		{
			sendPacket(new EventTrigger(npc.getTriggerId(), true));
		}
		sendPacket(new DeleteObject(object));
	}
	
	public final void refreshInfos()
	{
		for (final GameObject object : World.getInstance().getAroundObjects(this))
		{
			if (object.isPlayer() && object.getActingPlayer().inObserverMode())
			{
				continue;
			}
			sendInfoFrom(object);
		}
	}
	
	private final void sendInfoFrom(GameObject object)
	{
		object.sendInfo(this);
		if (object instanceof Creature)
		{
			final Creature obj = (Creature) object;
			if (obj.hasAI())
			{
				obj.getAI().describeStateToPlayer(this);
			}
		}
	}
	
	public void setSittingObject(StaticObjectInstance object)
	{
		_sittingObject = object;
	}
	
	public StaticObjectInstance getSittingObject()
	{
		return _sittingObject;
	}
	
	public Recommendation getRecommendation()
	{
		return _recommendation;
	}
	
	public void addBannedAction(String info)
	{
		_bannedActions.add(info);
	}
	
	public List<String> getBannedActions()
	{
		return _bannedActions;
	}
	
	public boolean canUsePreviewTask()
	{
		return !getPersonalTasks().isActiveTask(22);
	}
	
	public void setRemovePreviewTask()
	{
		getPersonalTasks().addTask(new RemoveWearItemsTask(3000L));
	}
	
	public Location getFallingLoc()
	{
		return _fallingLoc;
	}
	
	public void setFallingLoc(Location loc)
	{
		if (isFalling())
		{
			return;
		}
		_fallingLoc = loc;
	}
	
	public void addCBTeleport(int id, PcTeleportTemplate data)
	{
		_communtyTeleports.put(id, data);
	}
	
	public Collection<PcTeleportTemplate> getCBTeleports()
	{
		return _communtyTeleports.values();
	}
	
	public void removeCBTeleport(int id)
	{
		_communtyTeleports.remove(id);
	}
	
	public PcTeleportTemplate getCBTeleport(final int id)
	{
		return _communtyTeleports.get(id);
	}
	
	public Location getSaveLoc()
	{
		return _saveLoc;
	}
	
	public void setSaveLoc(Location loc)
	{
		_saveLoc = loc;
	}
	
	public Map<Integer, Integer> getAchievements(final int category)
	{
		final Map<Integer, Integer> result = new HashMap<>();
		for (final Entry<Integer, Integer> entry : _achievementLevels.entrySet())
		{
			final int achievementId = entry.getKey();
			final int achievementLevel = entry.getValue();
			final AchiveTemplate ach = AchievementManager.getInstance().getAchievement(achievementId, Math.max(1, achievementLevel));
			if ((ach != null) && (ach.getCategoryId() == category))
			{
				result.put(achievementId, achievementLevel);
			}
		}
		return result;
	}
	
	public Map<Integer, Integer> getAchievements()
	{
		return _achievementLevels;
	}
	
	public void loadAchivements()
	{
		final String achievements = getVar("achievements");
		if ((achievements != null) && !achievements.isEmpty())
		{
			final String[] levels = achievements.split(";");
			for (final String ach : levels)
			{
				final String[] lvl = ach.split(",");
				if (AchievementManager.getInstance().getMaxLevel(Integer.parseInt(lvl[0])) > 0)
				{
					_achievementLevels.put(Integer.parseInt(lvl[0]), Integer.parseInt(lvl[1]));
				}
			}
		}
		
		for (final int achievementId : AchievementManager.getInstance().getAchievementIds())
		{
			if (!_achievementLevels.containsKey(achievementId))
			{
				_achievementLevels.put(achievementId, 0);
			}
		}
		AchievementsDAO.getInstance().restoreAchievements(this);
	}
	
	public void saveAchivements()
	{
		String str = "";
		for (final Entry<Integer, Integer> a : _achievementLevels.entrySet())
		{
			str += a.getKey() + "," + a.getValue() + ";";
		}
		setVar("achievements", str);
		AchievementsDAO.getInstance().saveAchievements(this);
	}
	
	public AchievementCounters getCounters()
	{
		return _achievementCounters;
	}
	
	@Override
	public AutoFarmOptions getFarmSystem()
	{
		return _autoFarmSystem;
	}
	
	public Location getBookmarkLocation()
	{
		return _bookmarkLocation;
	}
	
	public void setBookmarkLocation(Location loc)
	{
		_bookmarkLocation = loc;
	}
	
	public int getPledgeItemId()
	{
		return _pledgeItemId;
	}
	
	public void setPledgeItemId(final int itemId)
	{
		_pledgeItemId = itemId;
	}
	
	public void setPledgePrice(final long price)
	{
		_pledgePrice = price;
	}
	
	public long getPledgePrice()
	{
		return _pledgePrice;
	}
	
	public void setSearchforAcademy(final boolean search)
	{
		_isInAcademyList = search;
	}
	
	public boolean isInSearchOfAcademy()
	{
		return _isInAcademyList;
	}
	
	public void sendInventoryUpdate(InventoryUpdate iu)
	{
		sendPacket(iu);
	}
	
	public void sendItemList(boolean show)
	{
		final ItemInstance[] items = getInventory().getItems();
		final int allSize = items.length;
		int questItemsSize = 0;
		int agathionItemsSize = 0;
		for (final ItemInstance item : items)
		{
			if (item == null)
			{
				continue;
			}
			
			if (item.isQuestItem())
			{
				questItemsSize++;
			}
			if (item.isEnergyItem())
			{
				agathionItemsSize++;
			}
		}
		
		sendPacket(new ItemList(getInventory(), allSize - questItemsSize, items, show));
		sendPacket(new ExQuestItemList(getInventory(), questItemsSize, items));
		if (agathionItemsSize > 0)
		{
			sendPacket(new ExBrAgathionEnergyInfo(agathionItemsSize, items));
		}
	}
	
	public int[] getUserVisualSlots()
	{
		final int visualId = getVarInt("visualBuff", 0);
		final boolean isCostumeBuff = visualId > 0 && !isTransformed();
		final boolean allowBlock = (isInOlympiadMode() && Config.BLOCK_VISUAL_OLY);
		
		if ((_activeWeaponSkin != null) && hasWeaponForVisualEquipped(true) && !allowBlock)
		{
			_userInfoSlots[0] = _activeWeaponSkin.getId();
			_userInfoSlots[9] = _activeWeaponSkin.isAllowEnchant() ? getEnchantEffect() : 0;
			_userInfoSlots[10] = _activeWeaponSkin.isAllowAugment() ? getInventory().getPaperdollAugmentationId(Inventory.PAPERDOLL_RHAND) : 0;
		}
		else
		{
			if (Config.ALLOW_VISUAL_ARMOR_COMMAND && !allowBlock)
			{
				final ItemInstance item = getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
				if (item != null && item.getVisualItemId() > 0)
				{
					final DressWeaponTemplate weapon = DressWeaponParser.getInstance().getWeapon(item.getVisualItemId());
					_userInfoSlots[0] = weapon != null ? weapon.getId() : 0;
					_userInfoSlots[9] = weapon != null && weapon.isAllowEnchant() ? getEnchantEffect() : 0;
					_userInfoSlots[10] = weapon != null && weapon.isAllowAugment() ? getInventory().getPaperdollAugmentationId(Inventory.PAPERDOLL_RHAND) : 0;
				}
				else
				{
					_userInfoSlots[0] = getInventory().getPaperdollItemDisplayId(Inventory.PAPERDOLL_RHAND);
					_userInfoSlots[9] = getEnchantEffect();
					_userInfoSlots[10] = getInventory().getPaperdollAugmentationId(Inventory.PAPERDOLL_RHAND);
				}
			}
			else
			{
				_userInfoSlots[0] = getInventory().getPaperdollItemDisplayId(Inventory.PAPERDOLL_RHAND);
				_userInfoSlots[9] = getEnchantEffect();
				_userInfoSlots[10] = getInventory().getPaperdollAugmentationId(Inventory.PAPERDOLL_RHAND);
			}
		}
		
		if ((_activeShieldSkin != null) && hasShieldForVisualEquipped(true) && !allowBlock)
		{
			_userInfoSlots[1] = _activeShieldSkin.getShieldId();
		}
		else
		{
			_userInfoSlots[1] = Config.ALLOW_VISUAL_ARMOR_COMMAND && !allowBlock ? getInventory().getPaperdollVisualItemId(Inventory.PAPERDOLL_LHAND) : getInventory().getPaperdollItemDisplayId(Inventory.PAPERDOLL_LHAND);
		}
		
		if (isCostumeBuff && !allowBlock)
		{
			final DressArmorTemplate tpl = DressArmorParser.getInstance().getArmor(visualId);
			_userInfoSlots[2] = tpl != null ? tpl.getGloves() : getInventory().getPaperdollItemDisplayId(Inventory.PAPERDOLL_GLOVES);
			_userInfoSlots[3] = tpl != null ? tpl.getChest() : getInventory().getPaperdollItemDisplayId(Inventory.PAPERDOLL_CHEST);
			_userInfoSlots[4] = tpl != null ? tpl.getLegs() : getInventory().getPaperdollItemDisplayId(Inventory.PAPERDOLL_LEGS);
			_userInfoSlots[5] = tpl != null ? tpl.getFeet() : getInventory().getPaperdollItemDisplayId(Inventory.PAPERDOLL_FEET);
		}
		else if ((_activeArmorSkin != null) && hasArmorForVisualEquipped(true) && !allowBlock)
		{
			_userInfoSlots[2] = _activeArmorSkin.getGloves();
			_userInfoSlots[3] = _activeArmorSkin.getChest();
			_userInfoSlots[4] = _activeArmorSkin.getLegs();
			_userInfoSlots[5] = _activeArmorSkin.getFeet();
		}
		else
		{
			_userInfoSlots[2] = Config.ALLOW_VISUAL_ARMOR_COMMAND && !allowBlock ? getInventory().getPaperdollVisualItemId(Inventory.PAPERDOLL_GLOVES) : getInventory().getPaperdollItemDisplayId(Inventory.PAPERDOLL_GLOVES);
			_userInfoSlots[3] = Config.ALLOW_VISUAL_ARMOR_COMMAND && !allowBlock ? getInventory().getPaperdollVisualItemId(Inventory.PAPERDOLL_CHEST) : getInventory().getPaperdollItemDisplayId(Inventory.PAPERDOLL_CHEST);
			_userInfoSlots[4] = Config.ALLOW_VISUAL_ARMOR_COMMAND && !allowBlock ? getInventory().getPaperdollVisualItemId(Inventory.PAPERDOLL_LEGS) : getInventory().getPaperdollItemDisplayId(Inventory.PAPERDOLL_LEGS);
			_userInfoSlots[5] = Config.ALLOW_VISUAL_ARMOR_COMMAND && !allowBlock ? getInventory().getPaperdollVisualItemId(Inventory.PAPERDOLL_FEET) : getInventory().getPaperdollItemDisplayId(Inventory.PAPERDOLL_FEET);
		}
		
		if (isCostumeBuff && !allowBlock)
		{
			final DressArmorTemplate tpl = DressArmorParser.getInstance().getArmor(visualId);
			_userInfoSlots[6] = tpl != null ? tpl.getCloakId() : getInventory().getPaperdollItemDisplayId(Inventory.PAPERDOLL_CLOAK);
		}
		if ((_activeCloakSkin != null) && hasCloakForVisualEquipped(true) && !allowBlock)
		{
			_userInfoSlots[6] = _activeCloakSkin.getCloakId();
		}
		else
		{
			_userInfoSlots[6] = Config.ALLOW_VISUAL_ARMOR_COMMAND && !allowBlock ? getInventory().getPaperdollVisualItemId(Inventory.PAPERDOLL_CLOAK) : getInventory().getPaperdollItemDisplayId(Inventory.PAPERDOLL_CLOAK);
		}
		
		if ((_activeMaskSkin != null) && hasFaceForVisualEquipped(true) && !allowBlock)
		{
			_userInfoSlots[7] = _activeMaskSkin.getSlot() == 3 ? _activeMaskSkin.getHatId() : 0;
		}
		else
		{
			_userInfoSlots[7] = Config.ALLOW_VISUAL_ARMOR_COMMAND && !allowBlock ? getInventory().getPaperdollVisualItemId(Inventory.PAPERDOLL_HAIR) : getInventory().getPaperdollItemDisplayId(Inventory.PAPERDOLL_HAIR);
		}
		
		if ((_activeHairSkin != null) && hasHairForVisualEquipped(true) && !allowBlock)
		{
			_userInfoSlots[8] = _activeHairSkin.getSlot() == 2 ? _activeHairSkin.getHatId() : 0;
		}
		else
		{
			_userInfoSlots[8] = Config.ALLOW_VISUAL_ARMOR_COMMAND && !allowBlock ? getInventory().getPaperdollVisualItemId(Inventory.PAPERDOLL_HAIR2) : getInventory().getPaperdollItemDisplayId(Inventory.PAPERDOLL_HAIR2);
		}
		return _userInfoSlots;
	}
	
	public int[] getCharVisualSlots(Player viewer)
	{
		final int visualId = getVarInt("visualBuff", 0);
		final boolean isCostumeBuff = visualId > 0 && !isTransformed();
		final boolean allowBlock = (isInOlympiadMode() && Config.BLOCK_VISUAL_OLY) || (viewer != null && viewer.getVarInt("visualBlock", 0) > 0);
		
		if ((_activeWeaponSkin != null) && hasWeaponForVisualEquipped(true) && !allowBlock)
		{
			_charInfoSlots[0] = _activeWeaponSkin.getId();
			_charInfoSlots[9] = _activeWeaponSkin.isAllowEnchant() ? getEnchantEffect() : 0;
			_charInfoSlots[10] = _activeWeaponSkin.isAllowAugment() ? getInventory().getPaperdollAugmentationId(Inventory.PAPERDOLL_RHAND) : 0;
		}
		else
		{
			if (Config.ALLOW_VISUAL_ARMOR_COMMAND && !allowBlock)
			{
				final ItemInstance item = getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
				if (item != null && item.getVisualItemId() > 0)
				{
					final DressWeaponTemplate weapon = DressWeaponParser.getInstance().getWeapon(item.getVisualItemId());
					_charInfoSlots[0] = weapon != null ? weapon.getId() : 0;
					_charInfoSlots[9] = weapon != null && weapon.isAllowEnchant() ? getEnchantEffect() : 0;
					_charInfoSlots[10] = weapon != null && weapon.isAllowAugment() ? getInventory().getPaperdollAugmentationId(Inventory.PAPERDOLL_RHAND) : 0;
				}
				else
				{
					_charInfoSlots[0] = getInventory().getPaperdollItemDisplayId(Inventory.PAPERDOLL_RHAND);
					_charInfoSlots[9] = getEnchantEffect();
					_charInfoSlots[10] = getInventory().getPaperdollAugmentationId(Inventory.PAPERDOLL_RHAND);
				}
			}
			else
			{
				_charInfoSlots[0] = getInventory().getPaperdollItemDisplayId(Inventory.PAPERDOLL_RHAND);
				_charInfoSlots[9] = getEnchantEffect();
				_charInfoSlots[10] = getInventory().getPaperdollAugmentationId(Inventory.PAPERDOLL_RHAND);
			}
		}
		
		if ((_activeShieldSkin != null) && hasShieldForVisualEquipped(true) && !allowBlock)
		{
			_charInfoSlots[1] = _activeShieldSkin.getShieldId();
		}
		else
		{
			_charInfoSlots[1] = Config.ALLOW_VISUAL_ARMOR_COMMAND && !allowBlock ? getInventory().getPaperdollVisualItemId(Inventory.PAPERDOLL_LHAND) : getInventory().getPaperdollItemDisplayId(Inventory.PAPERDOLL_LHAND);
		}
		
		if (isCostumeBuff && !allowBlock)
		{
			final DressArmorTemplate tpl = DressArmorParser.getInstance().getArmor(visualId);
			_charInfoSlots[2] = tpl != null ? tpl.getGloves() : getInventory().getPaperdollItemDisplayId(Inventory.PAPERDOLL_GLOVES);
			_charInfoSlots[3] = tpl != null ? tpl.getChest() : getInventory().getPaperdollItemDisplayId(Inventory.PAPERDOLL_CHEST);
			_charInfoSlots[4] = tpl != null ? tpl.getLegs() : getInventory().getPaperdollItemDisplayId(Inventory.PAPERDOLL_LEGS);
			_charInfoSlots[5] = tpl != null ? tpl.getFeet() : getInventory().getPaperdollItemDisplayId(Inventory.PAPERDOLL_FEET);
		}
		else if ((_activeArmorSkin != null) && hasArmorForVisualEquipped(true) && !allowBlock)
		{
			_charInfoSlots[2] = _activeArmorSkin.getGloves();
			_charInfoSlots[3] = _activeArmorSkin.getChest();
			_charInfoSlots[4] = _activeArmorSkin.getLegs();
			_charInfoSlots[5] = _activeArmorSkin.getFeet();
		}
		else
		{
			_charInfoSlots[2] = Config.ALLOW_VISUAL_ARMOR_COMMAND && !allowBlock ? getInventory().getPaperdollVisualItemId(Inventory.PAPERDOLL_GLOVES) : getInventory().getPaperdollItemDisplayId(Inventory.PAPERDOLL_GLOVES);
			_charInfoSlots[3] = Config.ALLOW_VISUAL_ARMOR_COMMAND && !allowBlock ? getInventory().getPaperdollVisualItemId(Inventory.PAPERDOLL_CHEST) : getInventory().getPaperdollItemDisplayId(Inventory.PAPERDOLL_CHEST);
			_charInfoSlots[4] = Config.ALLOW_VISUAL_ARMOR_COMMAND && !allowBlock ? getInventory().getPaperdollVisualItemId(Inventory.PAPERDOLL_LEGS) : getInventory().getPaperdollItemDisplayId(Inventory.PAPERDOLL_LEGS);
			_charInfoSlots[5] = Config.ALLOW_VISUAL_ARMOR_COMMAND && !allowBlock ? getInventory().getPaperdollVisualItemId(Inventory.PAPERDOLL_FEET) : getInventory().getPaperdollItemDisplayId(Inventory.PAPERDOLL_FEET);
		}
		
		if (isCostumeBuff && !allowBlock)
		{
			final DressArmorTemplate tpl = DressArmorParser.getInstance().getArmor(visualId);
			_charInfoSlots[6] = tpl != null ? tpl.getCloakId() : getInventory().getPaperdollItemDisplayId(Inventory.PAPERDOLL_CLOAK);
		}
		if ((_activeCloakSkin != null) && hasCloakForVisualEquipped(true) && !allowBlock)
		{
			_charInfoSlots[6] = _activeCloakSkin.getCloakId();
		}
		else
		{
			_charInfoSlots[6] = Config.ALLOW_VISUAL_ARMOR_COMMAND && !allowBlock ? getInventory().getPaperdollVisualItemId(Inventory.PAPERDOLL_CLOAK) : getInventory().getPaperdollItemDisplayId(Inventory.PAPERDOLL_CLOAK);
		}
		
		if ((_activeMaskSkin != null) && hasFaceForVisualEquipped(true) && !allowBlock)
		{
			_charInfoSlots[7] = _activeMaskSkin.getSlot() == 3 ? _activeMaskSkin.getHatId() : 0;
		}
		else
		{
			_charInfoSlots[7] = Config.ALLOW_VISUAL_ARMOR_COMMAND && !allowBlock ? getInventory().getPaperdollVisualItemId(Inventory.PAPERDOLL_HAIR) : getInventory().getPaperdollItemDisplayId(Inventory.PAPERDOLL_HAIR);
		}
		
		if ((_activeHairSkin != null) && hasHairForVisualEquipped(true) && !allowBlock)
		{
			_charInfoSlots[8] = _activeHairSkin.getSlot() == 2 ? _activeHairSkin.getHatId() : 0;
		}
		else
		{
			_charInfoSlots[8] = Config.ALLOW_VISUAL_ARMOR_COMMAND && !allowBlock ? getInventory().getPaperdollVisualItemId(Inventory.PAPERDOLL_HAIR2) : getInventory().getPaperdollItemDisplayId(Inventory.PAPERDOLL_HAIR2);
		}
		return _charInfoSlots;
	}
	
	public void startOnlineRewardTask(long time)
	{
		getPersonalTasks().removeTask(9, false);
		getPersonalTasks().addTask(new OnlineRewardTask(time));
	}
	
	public boolean startPunishmentTask(PunishmentTemplate template)
	{
		long lastTime = 0L;
		if (template.getExpirationTime() > 0)
		{
			lastTime = template.getExpirationTime() - System.currentTimeMillis();
		}
		getPersonalTasks().addTask(new PunishmentTask(lastTime, template));
		return true;
	}
	
	public void startJail()
	{
		setReflection(ReflectionManager.DEFAULT);
		setIsIn7sDungeon(false);
		
		if (getFightEventGameRoom() != null)
		{
			FightEventManager.getInstance().unsignFromAllEvents(this);
		}
		
		if (isInFightEvent())
		{
			if (getFightEvent().leaveEvent(this, false, true, true))
			{
				sendMessage("You have left the event!");
			}
		}
		
		if (OlympiadManager.getInstance().isRegisteredInComp(this))
		{
			OlympiadManager.getInstance().removeDisconnectedCompetitor(this);
		}
		
		if (isSellingBuffs())
		{
			unsetVar("offlineBuff");
		}
		
		if (isInOfflineMode())
		{
			unsetVar("offline");
			unsetVar("offlineTime");
			unsetVar("storemode");
		}
		getPersonalTasks().addTask(new TeleportTask(2000L, JailZone.getLocationIn()));
	}
	
	public void stopJail()
	{
		getPersonalTasks().addTask(new TeleportTask(2000L, JailZone.getLocationOut()));
	}
	
	public long getLastAttackPacket()
	{
		return _lastAttackPacket;
	}
	
	public void setLastAttackPacket()
	{
		_lastAttackPacket = System.currentTimeMillis();
	}
	
	public long getLastRequestMagicPacket()
	{
		return _lastRequestMagicPacket;
	}
	
	public void setLastRequestMagicPacket()
	{
		_lastRequestMagicPacket = System.currentTimeMillis();
	}
	
	public final int getPolyId()
	{
		return _polyId;
	}
	
	public final void setPolyId(int value)
	{
		_polyId = value;
		broadcastUserInfo(true);
	}
	
	public boolean isPolymorphed()
	{
		return _polyId != 0;
	}
	
	public void addEventItem(ItemInstance item)
	{
		_eventItems.add(item);
	}
	
	public List<ItemInstance> getEventItems()
	{
		return _eventItems;
	}
	
	public boolean isPassiveSpoil()
	{
		return getStat().calcStat(Stats.PASSIVE_SPOIL, 0, null, null) > 0;
	}
	
	public void setCacheIp(String ip)
	{
		_cacheIp = ip;
	}
	
	public void setCacheHwid(String hwid)
	{
		_cacheHwid = hwid;
	}
	
	public String getCacheIp()
	{
		return _cacheIp;
	}
	
	public String getCacheHwid()
	{
		return _cacheHwid;
	}
	
	public Arena getArena()
	{
		return _arena;
	}
	
	public void setArena(Arena arena)
	{
		_arena = arena;
	}
	
	public boolean isInKrateisCube()
	{
		return _arena != null;
	}
	
	public boolean isInBoat()
	{
		return (_vehicle != null) && _vehicle.isBoat();
	}
	
	public BoatInstance getBoat()
	{
		return (BoatInstance) _vehicle;
	}
	
	public boolean isInAirShip()
	{
		return (_vehicle != null) && _vehicle.isAirShip();
	}
	
	public AirShipInstance getAirShip()
	{
		return (AirShipInstance) _vehicle;
	}
	
	public Vehicle getVehicle()
	{
		return _vehicle;
	}
	
	public void setVehicle(Vehicle v)
	{
		if ((v == null) && (_vehicle != null))
		{
			_vehicle.removePassenger(this);
		}
		
		_vehicle = v;
	}
	
	@Override
	public boolean isInVehicle()
	{
		return _vehicle != null;
	}
	
	public Location getInVehiclePosition()
	{
		return _inVehiclePosition;
	}
	
	public void setInVehiclePosition(Location loc)
	{
		_inVehiclePosition = loc;
	}
	
	public void setClientLoc(Location loc)
	{
		_clientLocation = loc;
	}
	
	public Location getClientLoc()
	{
		return _clientLocation;
	}
	
	public boolean isLogoutStarted()
	{
		return _isLogout;
	}
	
	public void kick()
	{
		prepareToLogout();
		if (_client != null)
		{
			_client.close(LogOutOk.STATIC_PACKET);
			setClient(null);
		}
		finishToLogout();
		deleteMe();
	}
	
	public void restart()
	{
		prepareToLogout();
		if (_client != null)
		{
			_client.setActiveChar(null);
			setClient(null);
		}
		finishToLogout();
		deleteMe();
	}
	
	public void logout()
	{
		prepareToLogout();
		if (_client != null)
		{
			_client.close(ServerClose.STATIC_PACKET);
			setClient(null);
		}
		finishToLogout();
		deleteMe();
	}
	
	public void prepareToLogout()
	{
		final var summon = getSummon();
		if (summon != null)
		{
			summon.setRestoreSummon(true);
			summon.unSummon(this);
		}
		
		if (Config.ALLOW_VIP_SYSTEM)
		{
			setVar("vipLevel", String.valueOf(getVipLevel()));
			setVar("vipPoints", String.valueOf(getVipPoints()));
		}
		
		if (Config.ALLOW_REVENGE_SYSTEM)
		{
			setVar("revengeList", saveRevergeList());
		}
		
		if (getActiveRequester() != null)
		{
			setActiveRequester(null);
			cancelActiveTrade();
		}
	}
	
	public void finishToLogout()
	{
		if (isLogoutStarted())
		{
			return;
		}
		
		_isLogout = true;
		
		getFarmSystem().stopFarmTask(false);
		OnlineRewardManager.getInstance().activePlayerDisconnect(this);
		DoubleSessionManager.getInstance().onDisconnect(this);
		
		if (Config.ALLOW_DAILY_TASKS)
		{
			saveDailyTasks();
		}
		
		if (!getTransformNotCheckSkills().isEmpty())
		{
			untransform();
		}
		
		if (isInSearchOfAcademy())
		{
			setSearchforAcademy(false);
			AcademyList.deleteFromAcdemyList(this);
		}
		
		if (isInFightEvent())
		{
			getFightEvent().leaveEvent(this, true, true, true);
		}
		
		if (isInKrateisCube())
		{
			getArena().removePlayer(this);
			teleToLocation(-70381, -70937, -1428, 0, true, getReflection());
		}
		
		_bannedActions.clear();
		
		final var zones = ZoneManager.getInstance().getZones(this);
		if (zones != null && !zones.isEmpty())
		{
			for (final var zone : zones)
			{
				if (zone != null)
				{
					zone.onPlayerLogoutInside(this);
				}
			}
		}
		
		if (Config.ENABLE_BLOCK_CHECKER_EVENT && (getBlockCheckerArena() != -1))
		{
			HandysBlockCheckerManager.getInstance().onDisconnect(this);
		}
		
		if (getInventory().getItemByItemId(9819) != null && !isInFightEvent())
		{
			final var fort = FortManager.getInstance().getFort(this);
			if (fort != null)
			{
				FortSiegeManager.getInstance().dropCombatFlag(this, fort.getId());
			}
			else
			{
				final int slot = getInventory().getSlotFromItem(getInventory().getItemByItemId(9819));
				getInventory().unEquipItemInBodySlot(slot);
				destroyItem("CombatFlag", getInventory().getItemByItemId(9819), null, true);
			}
		}
		else if (isCombatFlagEquipped() && !isInFightEvent())
		{
			TerritoryWarManager.getInstance().dropCombatFlag(this, false, false);
		}
		
		setClient(null);
		setOnlineStatus(false, false);
		
		getListeners().onExit();
		
		abortAttack();
		abortCast();
		stopMove(null);
		setDebug(null);
		
		if (isFlying())
		{
			removeSkill(SkillsParser.getInstance().getInfo(4289, 1));
		}
		
		if (hasPremiumBonus() && getPremiumBonus().isOnlineType())
		{
			if (getPremiumBonus().isPersonal())
			{
				CharacterPremiumDAO.getInstance().updateOnlineTimePersonal(this);
			}
			else
			{
				CharacterPremiumDAO.getInstance().updateOnlineTime(this);
			}
		}
		
		final var party = getParty();
		if (party != null)
		{
			final var rift = party.getDimensionalRift();
			if (rift != null)
			{
				rift.oustMember(getName(null));
			}
			leaveParty();
		}
		
		if (inObserverMode())
		{
			setXYZInvisible(_lastX, _lastY, _lastZ);
		}
		
		if (OlympiadManager.getInstance().isRegistered(this) || (getOlympiadGameId() != -1))
		{
			OlympiadManager.getInstance().removeDisconnectedCompetitor(this);
		}
		
		if (isMounted())
		{
			dismount();
		}
		
		getFreight().deleteMe();
		getBlockList().playerLogout();
		
		if (isCursedWeaponEquipped())
		{
			CursedWeaponsManager.getInstance().getCursedWeapon(_cursedWeaponEquippedId).setPlayer(null);
		}
		
		stopAllTimers();
		setIsTeleporting(false);
		
		RecipeController.getInstance().requestMakeItemAbort(this);
		
		if (isInVehicle())
		{
			getVehicle().oustPlayer(this);
		}
		
		final var clan = getClan();
		if (clan != null)
		{
			final ClanMember clanMember = clan.getClanMember(getObjectId());
			if (clanMember != null)
			{
				clanMember.setPlayerInstance(null);
			}
			clan.broadcastToOtherOnlineMembers(new PledgeShowMemberListUpdate(this), this);
		}
		
		final MatchingRoom room = getMatchingRoom();
		if (room != null)
		{
			if (room.getLeader() == this)
			{
				room.disband();
			}
			else
			{
				room.removeMember(this, false);
			}
		}
		setMatchingRoom(null);
		
		MatchingRoomManager.getInstance().removeFromWaitingList(this);
		
		if (isGM())
		{
			AdminParser.getInstance().deleteGm(this);
		}
		
		final var ref = getReflection();
		if (!ref.isDefault())
		{
			ref.removePlayer(getObjectId());
			getEffectList().stopAllReflectionBuffs();
				
			final var loc = ref.getReturnLoc();
			if (loc != null)
			{
				final int x = loc.getX() + Rnd.get(-30, 30);
				final int y = loc.getY() + Rnd.get(-30, 30);
				setXYZInvisible(x, y, loc.getZ());
				final var summon = getSummon();
				if (summon != null)
				{
					summon.teleToLocation(loc, true, ReflectionManager.DEFAULT);
				}
			}
		}
		
		if (getFightEventGameRoom() != null)
		{
			FightEventManager.getInstance().unsignFromAllEvents(this);
		}
		
		for (final ItemInstance item : getInventory().getItems())
		{
			if (item != null && item.isEventItem())
			{
				getInventory().destroyItemByObjectId(item.getObjectId(), item.getCount(), this, null);
			}
		}
		
		AerialCleftEvent.getInstance().onLogout(this);
		
		try
		{
			getInventory().deleteMe();
			clearPrivateInventory();
			clearRefund();
			clearWarehouse();
			getListeners().clear();
		}
		catch (final Throwable t)
		{
			_log.error("", t);
		}
		
		if (Config.WAREHOUSE_CACHE)
		{
			WarehouseCache.getInstance().remCacheTask(this);
		}
		
		for (final Player player : _snoopedPlayer)
		{
			player.removeSnooper(this);
		}
		
		for (final Player player : _snoopListener)
		{
			player.removeSnooped(this);
		}
		
		try
		{
			store();
			getEffectList().stopAllEffects();
			getToggleList().clear();
		}
		catch (final Throwable t)
		{
			_log.error("", t);
		}
	}
	
	public void scheduleDelete()
	{
		final long time = NumberUtils.toInt(getVar("noCarrier"), Config.DISCONNECT_TIMEOUT);
		
		if (Config.DISCONNECT_SYSTEM_ENABLED && time > 0)
		{
			getFarmSystem().stopFarmTask(false);
			getAppearance().setVisibleTitle(Config.DISCONNECT_TITLE);
			getAppearance().setDisplayName(true);
			final String color = String.valueOf(Config.DISCONNECT_TITLECOLOR.charAt(4)) + String.valueOf(Config.DISCONNECT_TITLECOLOR.charAt(5)) + String.valueOf(Config.DISCONNECT_TITLECOLOR.charAt(2)) + String.valueOf(Config.DISCONNECT_TITLECOLOR.charAt(3)) + String.valueOf(Config.DISCONNECT_TITLECOLOR.charAt(0)) + String.valueOf(Config.DISCONNECT_TITLECOLOR.charAt(1));
			getAppearance().setTitleColor(Integer.decode("0x" + color));
			broadcastPacket(new CharInfo(this, null));
		}
		scheduleDelete(time * 1000L);
	}
	
	public void scheduleDelete(long time)
	{
		if (isLogoutStarted() || isInOfflineMode())
		{
			return;
		}
		getPersonalTasks().addTask(new CleanUpTask(time));
	}
	
	public void setInOfflineMode(boolean closeClient)
	{
		if (isFakePlayer())
		{
			return;
		}
		
		if (canOfflineMode(_client.getActiveChar(), false))
		{
			AuthServerCommunication.getInstance().removeAuthedClient(_client.getLogin());
			if (!Config.DOUBLE_SESSIONS_CONSIDER_OFFLINE_TRADERS)
			{
				DoubleSessionManager.getInstance().onDisconnect(this);
			}
			
			if (_client != null)
			{
				_client.setActiveChar(null);
				if (closeClient)
				{
					_client.close(LogOutOk.STATIC_PACKET);
				}
				else
				{
					_client.close(ServerClose.STATIC_PACKET);
				}
				setClient(null);
			}
			
			final var party = getParty();
			if (party != null)
			{
				leaveParty();
			}
			
			final var summon = getSummon();
			if (summon != null)
			{
				summon.setRestoreSummon(true);
				summon.unSummon(this);
			}
			
			if (Config.OFFLINE_SET_NAME_COLOR)
			{
				getAppearance().setNameColor(Config.OFFLINE_NAME_COLOR);
				broadcastCharInfo();
			}
			stopAcpTask();
			getPersonalTasks().cleanUp();
			getDonateRates().cleanUp();
			clearPetData();
			storePetFood(_mountNpcId);
			
			setOfflineMode(true);
			if (Config.OFFLINE_SET_VISUAL_EFFECT)
			{
				startAbnormalEffect(AbnormalEffect.SLEEP);
			}
			
			if (isSellingBuffs())
			{
				setVar("offlineBuff", String.valueOf(System.currentTimeMillis() / 1000L), -1);
			}
			else
			{
				setVar("offline", String.valueOf(System.currentTimeMillis() / 1000L), -1);
			}
			
			if (isInSearchOfAcademy())
			{
				setSearchforAcademy(false);
				AcademyList.deleteFromAcdemyList(this);
			}
		}
		else
		{
			scheduleDelete();
		}
	}
	
	public boolean isSameAddress(Player target)
	{
		if (Config.PROTECTION.equalsIgnoreCase("NONE"))
		{
			return getIPAddress().equals(target.getIPAddress());
		}
		else
		{
			return getHWID().equals(target.getHWID());
		}
	}
	
	public GameObject getVisibleObject(int id)
	{
		if (getObjectId() == id)
		{
			return this;
		}
		
		GameObject target = null;
		if (getTargetId() == id)
		{
			target = getTarget();
		}
		
		if (target == null && getParty() != null)
		{
			for (final Player p : getParty().getMembers())
			{
				if (p != null && p.getObjectId() == id)
				{
					target = p;
					break;
				}
			}
		}
		
		if (target == null)
		{
			target = World.getInstance().getAroundObjectById(this, id);
		}
		return target == null || !target.isVisibleFor(this) ? null : target;
	}
	
	@Override
	public boolean isCanAbsorbDamage(Creature target)
	{
		return !Config.PVP_ABSORB_DAMAGE || isInOlympiadMode() || (Config.PVP_ABSORB_DAMAGE && getPvpFlag() == 0 && !target.isPlayer());
	}
	
	public int getActiveAura()
	{
		return (int) getStat().calcStat(Stats.ACTIVE_AURA, 0);
	}
	
	public void setIsDrawZone(boolean val)
	{
		_IsInDrawZone = val;
	}
	
	public boolean isInDrawZone()
	{
		return _IsInDrawZone;
	}
	
	public void addDrawCoords(Location loc)
	{
		_drawCoords.add(loc);
		sendMessage("Added coords: x " + loc.getX() + " y " + loc.getZ() + " z " + loc.getZ());
	}
	
	public List<Location> getDrawCoods()
	{
		return _drawCoords;
	}
	
	public void setWareDelay(long val)
	{
		_hardWareDelay = val;
		if (_hardWareDelay > System.currentTimeMillis())
		{
			getPersonalTasks().addTask(new HardWareTask(_hardWareDelay - System.currentTimeMillis()));
		}
	}
	
	public long getWareDelay()
	{
		return _hardWareDelay;
	}
	
	public DonateRates getDonateRates()
	{
		return _donateRates;
	}
	
	public BypassStorage getBypassStorage()
	{
		return _bypassStorage;
	}
	
	public String isDecodedBypass(String bypass)
	{
		final var bypassType = getBypassStorage().getBypassType(bypass);
		try
		{
			switch (bypassType)
			{
				case SIMPLE_DIRECT :
					Util.handleIllegalPlayerAction(this, "Wrong action bypass: " + bypass);
					return null;
				case ENCODED :
					bypass = getBypassStorage().decodeBypass(bypass.substring(1), false);
					if (bypass == null)
					{
						return null;
					}
					break;
				case ENCODED_BBS :
					bypass = getBypassStorage().decodeBypass(bypass.substring(1), true);
					if (bypass == null)
					{
						sendPacket(new ShowBoard());
						return null;
					}
					break;
			}
		}
		catch (final Exception e)
		{
			return null;
		}
		return bypass;
	}

	private boolean tournamentTeamBeingInvited;
	private int tournamentTeamRequesterId;
	public int getTournamentTeamRequesterId()
	{
		return tournamentTeamRequesterId;
	}
	public void setTournamentTeamRequesterId(int tournamentTeamRequesterId)
	{
		this.tournamentTeamRequesterId = tournamentTeamRequesterId;
	}
	public boolean isTournamentTeamBeingInvited()
	{
		return tournamentTeamBeingInvited;
	}
	public void setTournamentTeamBeingInvited(boolean tournamentTeamBeingInvited)
	{
		this.tournamentTeamBeingInvited = tournamentTeamBeingInvited;
	}

	private int tournamentGameId;

	public int getTournamentGameId() {
		return tournamentGameId;
	}

	public void setTournamentGameId(int tournamentGameId) {
		this.tournamentGameId = tournamentGameId;
	}

	@Override
	public double getRunSpeed() {
		if (Config.STATS_UP_SYSTEM && getVar("runSpd") != null) {
			double pvAtk = (getVar("runSpd") != null) ? (100 + getVarInt("runSpd")) : 0.0D;
			pvAtk /= 100.0D;
			if (pvAtk != 0.0D) {
				return Math.max(1, super.getRunSpeed() * pvAtk);
			}
		}
		return super.getRunSpeed();
	}

	@Override
	public double getMaxCp() {
		if (Config.STATS_UP_SYSTEM && getVar("mCp") != null) {
			double pvAtk = (getVar("mCp") != null) ? (100 + getVarInt("mCp")) : 0.0D;
			pvAtk /= 100.0D;
			if (pvAtk != 0.0D)
				return Math.max(1, super.getMaxCp() * pvAtk);
		}
		return super.getMaxCp();
	}

	@Override
	public double getMaxHp() {
		if (Config.STATS_UP_SYSTEM && getVar("mHp") != null) {
			double pvAtk = (getVar("mHp") != null) ? (100 + getVarInt("mHp")) : 0.0D;
			pvAtk /= 100.0D;
			if (pvAtk != 0.0D) {
				return Math.max(1, super.getMaxHp() * pvAtk);
			}
		}
		return super.getMaxHp();
	}

	@Override
	public double getMaxMp() {
		if (Config.STATS_UP_SYSTEM && getVar("mMp") != null) {
			double pvAtk = (getVar("mMp") != null) ? (100 + getVarInt("mMp")) : 0.0D;
			pvAtk /= 100.0D;
			if (pvAtk != 0.0D)
				return Math.max(1, super.getMaxMp() * pvAtk);
		}
		return super.getMaxMp();
	}

	@Override
	public double getPDef(Creature target) {
		if (Config.STATS_UP_SYSTEM && getVar("pDef") != null) {
			double pvAtk = (getVar("pDef") != null) ? (100 + getVarInt("pDef")) : 0.0D;
			pvAtk /= 100.0D;
			if (pvAtk != 0.0D) {
				return Math.max(1, super.getPDef(target) * pvAtk);
			}
		}
		return super.getPDef(target);
	}

	@Override
	public double getMDef(Creature target, Skill skill) {
		if (Config.STATS_UP_SYSTEM && getVar("mDef") != null) {
			double pvAtk = (getVar("mDef") != null) ? (100 + getVarInt("mDef")) : 0.0D;
			pvAtk /= 100.0D;
			if (pvAtk != 0.0D) {
				return Math.max(1, super.getMDef(target, skill) * pvAtk);
			}
		}
		return super.getMDef(target, skill);
	}

	@Override
	public int getINT() {
		if (Config.STATS_UP_SYSTEM && getVar("INT") != null) {
			return Math.max(1, super.getINT() + getVarInt("INT"));
		}
		return super.getINT();
	}

	@Override
	public int getSTR() {
		if (Config.STATS_UP_SYSTEM && getVar("STR") != null) {
			return Math.max(1, super.getSTR() + getVarInt("STR"));
		}
		return super.getSTR();
	}

	@Override
	public int getCON() {
		if (Config.STATS_UP_SYSTEM && getVar("CON") != null) {
			return Math.max(1, super.getCON() + getVarInt("CON"));
		}
		return super.getCON();
	}

	@Override
	public int getMEN() {
		if (Config.STATS_UP_SYSTEM && getVar("MEN") != null) {
			return Math.max(1, super.getMEN() + getVarInt("MEN"));
		}
		return super.getMEN();
	}

	@Override
	public int getDEX() {
		if (Config.STATS_UP_SYSTEM && getVar("DEX") != null) {
			return Math.max(1, super.getDEX() + getVarInt("DEX"));
		}
		return super.getDEX();
	}

	@Override
	public int getWIT() {
		if (Config.STATS_UP_SYSTEM && getVar("WIT") != null) {
			return Math.max(1, super.getWIT() + getVarInt("WIT"));
		}
		return super.getWIT();
	}

	@Override
	public int getAccuracy() {
		if (Config.STATS_UP_SYSTEM && getVar("accuracy") != null) {
			double pvAtk = (getVar("accuracy") != null) ? (100 + getVarInt("accuracy")) : 0.0D;
			pvAtk /= 100.0D;
			if (pvAtk != 0.0D)
				return (int) Math.max(1, super.getAccuracy() * pvAtk);
		}
		return super.getAccuracy();
	}

	@Override
	public int getEvasionRate(Creature target) {
		if (Config.STATS_UP_SYSTEM && getVar("evasion") != null) {
			double pvAtk = (getVar("evasion") != null) ? (100 + getVarInt("evasion")) : 0.0D;
			pvAtk /= 100.0D;
			if (pvAtk != 0.0D)
				return (int) Math.max(1, super.getEvasionRate(target) * pvAtk);
		}
		return super.getEvasionRate(target);
	}

	@Override
	public double getCriticalHit(Creature target, Skill skill) {
		if (Config.STATS_UP_SYSTEM && getVar("pCritRate") != null) {
			double pvAtk = (getVar("pCritRate") != null) ? (100 + getVarInt("pCritRate")) : 0.0D;
			pvAtk /= 100.0D;
			if (pvAtk != 0.0D) {
				return (int) Math.max(1, super.getCriticalHit(target, skill) * pvAtk);
			}
		}
		return super.getCriticalHit(target, skill);
	}

	@Override
	public double getMCriticalHit(Creature target, Skill skill) {
		if (Config.STATS_UP_SYSTEM && getVar("mCritRate") != null) {
			double pvAtk = (getVar("mCritRate") != null) ? (100 + getVarInt("mCritRate")) : 0.0D;
			pvAtk /= 100.0D;
			if (pvAtk != 0.0D) {
				return (int) Math.max(1, super.getMCriticalHit(target, skill) * pvAtk);
			}
		}
		return super.getMCriticalHit(target, skill);
	}

	@Override
	public double getPAtk(Creature target) {
		if (Config.STATS_UP_SYSTEM && getVar("pAtk") != null) {
			double pvAtk = (getVar("pAtk") != null) ? (100 + getVarInt("pAtk")) : 0.0D;
			pvAtk /= 100.0D;
			if (pvAtk != 0.0D) {
				return (int) Math.max(1, super.getPAtk(target) * pvAtk);
			}
		}
		return super.getPAtk(target);
	}

	@Override
	public double getMAtk(Creature target, Skill skill) {
		if (Config.STATS_UP_SYSTEM && getVar("mAtk") != null) {
			double pvAtk = (getVar("mAtk") != null) ? (100 + getVarInt("mAtk")) : 0.0D;
			pvAtk /= 100.0D;
			if (pvAtk != 0.0D)
				return (int) Math.max(1, super.getMAtk(target, skill) * pvAtk);
		}
		return super.getMAtk(target, skill);
	}

	@Override
	public double getPAtkSpd() {
		if (Config.STATS_UP_SYSTEM && getVar("pAtkSpd") != null) {
			double pvAtk = (getVar("pAtkSpd") != null) ? (100 + getVarInt("pAtkSpd")) : 0.0D;
			pvAtk /= 100.0D;
			if (pvAtk != 0.0D) {
				return (int) Math.max(1, super.getPAtkSpd() * pvAtk);
			}
		}
		return super.getPAtkSpd();
	}

	@Override
	public double getMAtkSpd() {
		if (Config.STATS_UP_SYSTEM && getVar("mAtkSpd") != null) {
			double pvAtk = (getVar("mAtkSpd") != null) ? (100 + getVarInt("mAtkSpd")) : 0.0D;
			pvAtk /= 100.0D;
			if (pvAtk != 0.0D) {
				return (int) Math.max(1, super.getMAtkSpd() * pvAtk);
			}
		}
		return super.getMAtkSpd();
	}
}