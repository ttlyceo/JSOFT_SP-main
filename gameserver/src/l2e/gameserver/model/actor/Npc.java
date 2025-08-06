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

import java.util.List;
import java.util.concurrent.Future;

import l2e.commons.util.Rnd;
import l2e.gameserver.Config;
import l2e.gameserver.SevenSigns;
import l2e.gameserver.SevenSignsFestival;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.ai.npc.Corpse;
import l2e.gameserver.data.htm.HtmCache;
import l2e.gameserver.data.parser.CategoryParser;
import l2e.gameserver.data.parser.ItemsParser;
import l2e.gameserver.handler.bypasshandlers.BypassHandler;
import l2e.gameserver.handler.bypasshandlers.IBypassHandler;
import l2e.gameserver.handler.communityhandlers.CommunityBoardHandler;
import l2e.gameserver.handler.communityhandlers.ICommunityBoardHandler;
import l2e.gameserver.instancemanager.CHSiegeManager;
import l2e.gameserver.instancemanager.CastleManager;
import l2e.gameserver.instancemanager.FortManager;
import l2e.gameserver.instancemanager.TerritoryWarManager;
import l2e.gameserver.instancemanager.TerritoryWarManager.Territory;
import l2e.gameserver.instancemanager.TownManager;
import l2e.gameserver.instancemanager.WalkingManager;
import l2e.gameserver.instancemanager.games.krateiscube.model.Arena;
import l2e.gameserver.listener.npc.AbstractNpcListener;
import l2e.gameserver.listener.npc.NpcListenerList;
import l2e.gameserver.model.CategoryType;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.MinionList;
import l2e.gameserver.model.ShotType;
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.instance.ClanHallManagerInstance;
import l2e.gameserver.model.actor.instance.DoormenInstance;
import l2e.gameserver.model.actor.instance.FishermanInstance;
import l2e.gameserver.model.actor.instance.GrandBossInstance;
import l2e.gameserver.model.actor.instance.MerchantInstance;
import l2e.gameserver.model.actor.instance.MonsterInstance;
import l2e.gameserver.model.actor.instance.RaidBossInstance;
import l2e.gameserver.model.actor.instance.TeleporterInstance;
import l2e.gameserver.model.actor.instance.TrainerInstance;
import l2e.gameserver.model.actor.instance.WarehouseInstance;
import l2e.gameserver.model.actor.stat.NpcStat;
import l2e.gameserver.model.actor.status.NpcStatus;
import l2e.gameserver.model.actor.tasks.npc.OnAnimationTask;
import l2e.gameserver.model.actor.templates.items.Item;
import l2e.gameserver.model.actor.templates.items.Weapon;
import l2e.gameserver.model.actor.templates.npc.Faction;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate.ShotsType;
import l2e.gameserver.model.entity.Castle;
import l2e.gameserver.model.entity.Fort;
import l2e.gameserver.model.entity.clanhall.SiegableHall;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.olympiad.Olympiad;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestEventType;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.spawn.Spawner;
import l2e.gameserver.model.stats.NpcStats;
import l2e.gameserver.model.zone.type.TownZone;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.EventTrigger;
import l2e.gameserver.network.serverpackets.ExChangeNpcState;
import l2e.gameserver.network.serverpackets.MagicSkillUse;
import l2e.gameserver.network.serverpackets.MoveToLocation;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;
import l2e.gameserver.network.serverpackets.NpcInfo;
import l2e.gameserver.network.serverpackets.NpcSay;
import l2e.gameserver.network.serverpackets.ServerObjectInfo;
import l2e.gameserver.network.serverpackets.SocialAction;
import l2e.gameserver.taskmanager.DecayTaskManager;

public class Npc extends Creature
{
	public static final int INTERACTION_DISTANCE = 150;
	private Spawner _spawn;
	private boolean _isBusy = false;
	private String _busyMessage = "";
	private int _castleIndex = -2;
	private int _fortIndex = -2;
	
	private boolean _isRandomAnimationEnabled = true;
	private boolean _isHasNoChatWindow = false;
	private boolean _eventMob = false;
	private boolean _isInTown = false;
	private boolean _isAutoAttackable = false;
	private boolean _isRunner = false;
	private boolean _isSpecialCamera = false;
	private boolean _isEkimusFood = false;
	private boolean _isTargetable = true;
	private String _showBoard = "";
	protected long _lastSocialBroadcast;
	private Arena _arena = null;
	private Future<?> _despawnTask;
	private Future<?> _animationTask;
	private long _deathTime = 0L;
	private boolean _isDecayed = false;
	private boolean _isUnAggred = false;
	
	private int _currentLHandId;
	private int _currentRHandId;
	private int _currentEnchant;
	private double _currentCollisionHeight;
	private double _currentCollisionRadius;

	private int _displayEffect = 0;

	private Creature _summoner = null;

	private int _shotsMask = 0;
	private Territory _nearestTerritory;
	
	private Npc _master = null;
	private MinionList _minionList = null;
	private boolean _isCanSupportMinion = true;
	private int _triggerId = 0;
	
	public Faction getFaction()
	{
		return getTemplate().getFaction();
	}
	
	public boolean isInFaction(Npc npc)
	{
		return getFaction().equals(npc.getFaction()) && !getFaction().isIgnoreNpcId(npc.getId());
	}

	public void onRandomAnimation()
	{
		if (System.currentTimeMillis() - _lastSocialBroadcast > 10000L)
		{
			broadcastPacketToOthers(2000, new SocialAction(getObjectId(), 2));
			_lastSocialBroadcast = System.currentTimeMillis();
		}
	}

	public void startRandomAnimation()
	{
		if (!hasRandomAnimation() || getId() == 32705 || !isRandomAnimationEnabled() || (getAI() instanceof Corpse) || this instanceof Attackable)
		{
			return;
		}
		
		if (_animationTask != null)
		{
			_animationTask.cancel(false);
			_animationTask = null;
		}
		final int minWait = isMob() ? Config.MIN_MONSTER_ANIMATION : Config.MIN_NPC_ANIMATION;
		final int maxWait = isMob() ? Config.MAX_MONSTER_ANIMATION : Config.MAX_NPC_ANIMATION;
		_animationTask = ThreadPoolManager.getInstance().scheduleAtFixedRate(new OnAnimationTask(this), 1000L, Rnd.get(minWait, maxWait) * 1000L);
	}
	
	public void stopRandomAnimation()
	{
		if (_animationTask != null)
		{
			_animationTask.cancel(false);
			_animationTask = null;
		}
	}

	public Npc(int objectId, NpcTemplate template)
	{
		super(objectId, template);
		
		if (template == null)
		{
			_log.error("No template for Npc. Please check your datapack is setup correctly.");
			return;
		}
		
		setInstanceType(InstanceType.Npc);
		initCharStatusUpdateValues();

		_currentLHandId = getTemplate().getLeftHand();
		_currentRHandId = getTemplate().getRightHand();
		_currentEnchant = Config.ENABLE_RANDOM_ENCHANT_EFFECT ? Rnd.get(4, 21) : getTemplate().getEnchantEffect();
		_currentCollisionHeight = getTemplate().getfCollisionHeight();
		_currentCollisionRadius = getTemplate().getfCollisionRadius();
		for (final String lang : Config.MULTILANG_ALLOWED)
		{
			if (lang != null)
			{
				setName(lang, template.getName(lang) != null ? template.getName(lang) : template.getName(null));
				setTitle(lang, template.getTitle(lang) != null ? template.getTitle(lang) : template.getTitle(null));
			}
		}
		
		if (template.isImmobilized())
		{
			setIsImmobilized(true);
		}
		
		if (template.getRandomWalk())
		{
			setIsNoRndWalk(true);
		}
		
		if (template.getRandomAnimation())
		{
			setRandomAnimationEnabled(false);
		}
		
		if (template.isHasNoChatWindow())
		{
			_isHasNoChatWindow = true;
		}
		_showBoard = template.getParameter("showBoard", "");
	}

	@Override
	public NpcStat getStat()
	{
		return (NpcStat) super.getStat();
	}

	@Override
	public void initCharStat()
	{
		setStat(new NpcStat(this));
	}

	@Override
	public NpcStatus getStatus()
	{
		return (NpcStatus) super.getStatus();
	}

	@Override
	public void initCharStatus()
	{
		setStatus(new NpcStatus(this));
	}

	@Override
	public final NpcTemplate getTemplate()
	{
		return (NpcTemplate) super.getTemplate();
	}

	@Override
	public int getId()
	{
		return getTemplate().getId();
	}

	@Override
	public final int getLevel()
	{
		return getTemplate().getLevel();
	}

	public boolean isAggressive()
	{
		return false;
	}
	
	public final long getDeathTime()
	{
		return _deathTime;
	}

	public int getAggroRange()
	{
		if (_isUnAggred)
		{
			return 0;
		}
		return getTemplate().getAggroRange();
	}
	
	public int getHideAggroRange()
	{
		return getTemplate().getHideAggroRange();
	}

	@Override
	public boolean isUndead()
	{
		return getTemplate().isUndead();
	}

	@Override
	public void updateAbnormalEffect()
	{
		broadcastInfo();
	}

	public boolean isEventMob()
	{
		return _eventMob;
	}

	public void setEventMob(boolean val)
	{
		_eventMob = val;
	}

	@Override
	public boolean isAutoAttackable(Creature attacker, boolean isPoleAttack)
	{
		return _isAutoAttackable;
	}

	public void setAutoAttackable(boolean flag)
	{
		_isAutoAttackable = flag;
	}

	public int getLeftHandItem()
	{
		return _currentLHandId;
	}

	public int getRightHandItem()
	{
		return _currentRHandId;
	}

	public int getEnchantEffect()
	{
		return _currentEnchant;
	}

	public final boolean isBusy()
	{
		return _isBusy;
	}

	public void setBusy(boolean isBusy)
	{
		_isBusy = isBusy;
	}

	public final String getBusyMessage()
	{
		return _busyMessage;
	}

	public void setBusyMessage(String message)
	{
		_busyMessage = message;
	}

	public boolean isWarehouse()
	{
		return false;
	}

	public void setIsHasNoChatWindow(boolean isHasNoChatWindow)
	{
		_isHasNoChatWindow = isHasNoChatWindow;
	}
	
	public boolean isHasNoChatWindow()
	{
		return _isHasNoChatWindow;
	}
	
	public boolean canTarget(Player player)
	{
		if (!isTargetable())
		{
			player.sendActionFailed();
			return false;
		}
		if (player.isOutOfControl())
		{
			player.sendActionFailed();
			return false;
		}
		if (player.isLockedTarget() && (player.getLockedTarget() != this))
		{
			player.sendPacket(SystemMessageId.FAILED_CHANGE_TARGET);
			player.sendActionFailed();
			return false;
		}
		return true;
	}

	public boolean canInteract(Player player)
	{
		if (player.isCastingNow() || player.isCastingSimultaneouslyNow())
		{
			return false;
		}
		if (player.isDead() || player.isFakeDeathNow())
		{
			return false;
		}
		if (player.isSitting())
		{
			return false;
		}
		if (player.getPrivateStoreType() != Player.STORE_PRIVATE_NONE)
		{
			return false;
		}
		if (!isInsideRadius(player, (int) (INTERACTION_DISTANCE + player.getColRadius()), true, false))
		{
			return false;
		}
		if (player.getReflectionId() != getReflectionId())
		{
			return false;
		}
		if (isBusy())
		{
			return false;
		}
		return true;
	}

	public final Castle getCastle()
	{
		if (_castleIndex < 0)
		{
			final TownZone town = TownManager.getTown(getX(), getY(), getZ());

			if (town != null)
			{
				_castleIndex = CastleManager.getInstance().getCastleIndex(town.getTaxById());
			}

			if (_castleIndex < 0)
			{
				_castleIndex = CastleManager.getInstance().findNearestCastleIndex(this);
			}
			else
			{
				_isInTown = true;
			}
		}

		if (_castleIndex < 0)
		{
			return null;
		}

		return CastleManager.getInstance().getCastles().get(_castleIndex);
	}

	public boolean isMyLord(Player player)
	{
		if (player.isClanLeader())
		{
			final int castleId = getCastle() != null ? getCastle().getId() : -1;
			final int fortId = getFort() != null ? getFort().getId() : -1;
			return (player.getClan().getCastleId() == castleId) || (player.getClan().getFortId() == fortId);
		}
		return false;
	}

	public final SiegableHall getConquerableHall()
	{
		return CHSiegeManager.getInstance().getNearbyClanHall(getX(), getY(), 10000);
	}

	public final Castle getCastle(long maxDistance)
	{
		final int index = CastleManager.getInstance().findNearestCastleIndex(this, maxDistance);

		if (index < 0)
		{
			return null;
		}

		return CastleManager.getInstance().getCastles().get(index);
	}

	public final Fort getFort()
	{
		if (_fortIndex < 0)
		{
			final Fort fort = FortManager.getInstance().getFort(getX(), getY(), getZ());
			if (fort != null)
			{
				_fortIndex = FortManager.getInstance().getFortIndex(fort.getId());
			}

			if (_fortIndex < 0)
			{
				_fortIndex = FortManager.getInstance().findNearestFortIndex(this);
			}
		}

		if (_fortIndex < 0)
		{
			return null;
		}

		return FortManager.getInstance().getForts().get(_fortIndex);
	}

	public final Fort getFort(long maxDistance)
	{
		final int index = FortManager.getInstance().findNearestFortIndex(this, maxDistance);

		if (index < 0)
		{
			return null;
		}

		return FortManager.getInstance().getForts().get(index);
	}

	public final boolean getIsInTown()
	{
		if (_castleIndex < 0)
		{
			getCastle();
		}

		return _isInTown;
	}

	public void onBypassFeedback(Player player, String command)
	{
		if (player.isActionsDisabled())
		{
			player.sendActionFailed();
			return;
		}
		
		if (isBusy() && (getBusyMessage().length() > 0))
		{
			player.sendActionFailed();
			final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			html.setFile(player, player.getLang(), "data/html/npcbusy.htm");
			html.replace("%busymessage%", getBusyMessage());
			html.replace("%npcname%", getName(player.getLang()));
			html.replace("%playername%", player.getName(null));
			player.sendPacket(html);
		}
		else
		{
			final IBypassHandler handler = BypassHandler.getInstance().getHandler(command);
			if (handler != null)
			{
				handler.useBypass(command, player, this);
			}
			else
			{
				_log.info(getClass().getSimpleName() + ": Unknown NPC bypass: \"" + command + "\" NpcId: " + getId());
			}
		}
	}

	@Override
	public ItemInstance getActiveWeaponInstance()
	{
		return null;
	}

	@Override
	public Weapon getActiveWeaponItem()
	{
		final int weaponId = getTemplate().getRightHand();

		if (weaponId < 1)
		{
			return null;
		}

		final Item item = ItemsParser.getInstance().getTemplate(getTemplate().getRightHand());

		if (!(item instanceof Weapon))
		{
			return null;
		}

		return (Weapon) item;
	}

	@Override
	public ItemInstance getSecondaryWeaponInstance()
	{
		return null;
	}

	@Override
	public Weapon getSecondaryWeaponItem()
	{
		final int weaponId = getTemplate().getLeftHand();

		if (weaponId < 1)
		{
			return null;
		}

		final Item item = ItemsParser.getInstance().getTemplate(getTemplate().getLeftHand());

		if (!(item instanceof Weapon))
		{
			return null;
		}

		return (Weapon) item;
	}

	public void insertObjectIdAndShowChatWindow(Player player, String content)
	{
		content = content.replaceAll("%objectId%", String.valueOf(getObjectId()));
		final NpcHtmlMessage npcReply = new NpcHtmlMessage(getObjectId());
		npcReply.setHtml(player, content);
		player.sendPacket(npcReply);
	}

	public String getHtmlPath(int npcId, int val)
	{
		String pom = "";

		if (val == 0)
		{
			pom = "" + npcId;
		}
		else
		{
			pom = npcId + "-" + val;
		}

		final String temp = "data/html/default/" + pom + ".htm";

		if (!Config.ALLOW_CACHE)
		{
			return temp;
		}
		else
		{
			if (HtmCache.getInstance().contains(temp) || HtmCache.getInstance().isLoadable(temp))
			{
				return temp;
			}
		}
		return "data/html/npcdefault.htm";
	}

	public void showChatWindow(Player player)
	{
		showChatWindow(player, 0);
	}

	private boolean showPkDenyChatWindow(Player player, String type)
	{
		final String html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/" + type + "/" + getId() + "-pk.htm");
		if (html != null)
		{
			final NpcHtmlMessage pkDenyMsg = new NpcHtmlMessage(getObjectId());
			pkDenyMsg.setHtml(player, html);
			player.sendPacket(pkDenyMsg);
			player.sendActionFailed();
			return true;
		}
		return false;
	}

	public void showChatWindow(Player player, int val)
	{
		if (isHasNoChatWindow())
		{
			player.sendActionFailed();
			return;
		}
		
		if (!_showBoard.isEmpty())
		{
			final ICommunityBoardHandler handler = CommunityBoardHandler.getInstance().getHandler(_showBoard);
			if (handler != null)
			{
				handler.onBypassCommand(_showBoard, player);
			}
			return;
		}
		
		if (player.isCursedWeaponEquipped() && (!(player.getTarget() instanceof ClanHallManagerInstance) || !(player.getTarget() instanceof DoormenInstance)))
		{
			player.setTarget(player);
			return;
		}
		if (player.getKarma() > 0)
		{
			if (!Config.ALT_GAME_KARMA_PLAYER_CAN_SHOP && (this instanceof MerchantInstance))
			{
				if (showPkDenyChatWindow(player, "merchant"))
				{
					return;
				}
			}
			else if (!Config.ALT_GAME_KARMA_PLAYER_CAN_USE_GK && (this instanceof TeleporterInstance))
			{
				if (showPkDenyChatWindow(player, "teleporter"))
				{
					return;
				}
			}
			else if (!Config.ALT_GAME_KARMA_PLAYER_CAN_USE_WAREHOUSE && (this instanceof WarehouseInstance))
			{
				if (showPkDenyChatWindow(player, "warehouse"))
				{
					return;
				}
			}
			else if (!Config.ALT_GAME_KARMA_PLAYER_CAN_SHOP && (this instanceof FishermanInstance))
			{
				if (showPkDenyChatWindow(player, "fisherman"))
				{
					return;
				}
			}
		}

		if (getTemplate().isType("Auctioneer") && (val == 0))
		{
			return;
		}

		final int npcId = getTemplate().getId();

		String filename = SevenSigns.SEVEN_SIGNS_HTML_PATH;
		final int sealAvariceOwner = SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_AVARICE);
		final int sealGnosisOwner = SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_GNOSIS);
		final int playerCabal = SevenSigns.getInstance().getPlayerCabal(player.getObjectId());
		final int compWinner = SevenSigns.getInstance().getCabalHighestScore();

		switch (npcId)
		{
			case 31127 :
			case 31128 :
			case 31129 :
			case 31130 :
			case 31131 :
				filename += "festival/dawn_guide.htm";
				break;
			case 31137 :
			case 31138 :
			case 31139 :
			case 31140 :
			case 31141 :
				filename += "festival/dusk_guide.htm";
				break;
			case 31092 :
				filename += "blkmrkt_1.htm";
				break;
			case 31113 :
				if (Config.ALT_STRICT_SEVENSIGNS)
				{
					switch (compWinner)
					{
						case SevenSigns.CABAL_DAWN :
							if ((playerCabal != compWinner) || (playerCabal != sealAvariceOwner))
							{
								player.sendPacket(SystemMessageId.CAN_BE_USED_BY_DAWN);
								player.sendActionFailed();
								return;
							}
							break;
						case SevenSigns.CABAL_DUSK :
							if ((playerCabal != compWinner) || (playerCabal != sealAvariceOwner))
							{
								player.sendPacket(SystemMessageId.CAN_BE_USED_BY_DUSK);
								player.sendActionFailed();
								return;
							}
							break;
						default :
							player.sendPacket(SystemMessageId.SSQ_COMPETITION_UNDERWAY);
							return;
					}
				}
				filename += "mammmerch_1.htm";
				break;
			case 31126 :
				if (Config.ALT_STRICT_SEVENSIGNS)
				{
					switch (compWinner)
					{
						case SevenSigns.CABAL_DAWN :
							if ((playerCabal != compWinner) || (playerCabal != sealGnosisOwner))
							{
								player.sendPacket(SystemMessageId.CAN_BE_USED_BY_DAWN);
								player.sendActionFailed();
								return;
							}
							break;
						case SevenSigns.CABAL_DUSK :
							if ((playerCabal != compWinner) || (playerCabal != sealGnosisOwner))
							{
								player.sendPacket(SystemMessageId.CAN_BE_USED_BY_DUSK);
								player.sendActionFailed();
								return;
							}
							break;
						default :
							player.sendPacket(SystemMessageId.SSQ_COMPETITION_UNDERWAY);
							return;
					}
				}
				filename += "mammblack_1.htm";
				break;
			case 31132 :
			case 31133 :
			case 31134 :
			case 31135 :
			case 31136 :
			case 31142 :
			case 31143 :
			case 31144 :
			case 31145 :
			case 31146 :
				filename += "festival/festival_witch.htm";
				break;
			case 31688 :
				if (player.isNoble())
				{
					filename = Olympiad.OLYMPIAD_HTML_PATH + "noble_main.htm";
				}
				else
				{
					filename = (getHtmlPath(npcId, val));
				}
				break;
			case 31690 :
			case 31769 :
			case 31770 :
			case 31771 :
			case 31772 :
				if (player.isHero() || player.isNoble())
				{
					filename = Olympiad.OLYMPIAD_HTML_PATH + "hero_main.htm";
				}
				else
				{
					filename = (getHtmlPath(npcId, val));
				}
				break;
			case 36402 :
				if (player.olyBuff > 0)
				{
					filename = (player.olyBuff == 5 ? Olympiad.OLYMPIAD_HTML_PATH + "olympiad_buffs.htm" : Olympiad.OLYMPIAD_HTML_PATH + "olympiad_5buffs.htm");
				}
				else
				{
					filename = Olympiad.OLYMPIAD_HTML_PATH + "olympiad_nobuffs.htm";
				}
				break;
			case 30298 :
				if (player.isAcademyMember())
				{
					filename = (getHtmlPath(npcId, 1));
				}
				else
				{
					filename = (getHtmlPath(npcId, val));
				}
				break;
			default :
				if ((npcId >= 31865) && (npcId <= 31918))
				{
					if (val == 0)
					{
						filename += "rift/GuardianOfBorder.htm";
					}
					else
					{
						filename += "rift/GuardianOfBorder-" + val + ".htm";
					}
					break;
				}
				if (((npcId >= 31093) && (npcId <= 31094)) || ((npcId >= 31172) && (npcId <= 31201)) || ((npcId >= 31239) && (npcId <= 31254)))
				{
					return;
				}

				filename = (getHtmlPath(npcId, val));
				break;
		}

		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(player, player.getLang(), filename);

		if (this instanceof MerchantInstance)
		{
			if (Config.LIST_PET_RENT_NPC.contains(npcId))
			{
				html.replace("_Quest", "_RentPet\">Rent Pet</a><br><a action=\"bypass -h npc_%objectId%_Quest");
			}
		}

		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%festivalMins%", SevenSignsFestival.getInstance().getTimeToNextFestivalStr());
		player.sendPacket(html);
		player.sendActionFailed();
	}

	public void showChatWindow(Player player, String filename)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(player, player.getLang(), filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		player.sendPacket(html);
		player.sendActionFailed();
	}

	public int getExpReward(Creature attacker)
	{
		return getTemplate().getExpReward(attacker);
	}

	public int getSpReward(Creature attacker)
	{
		return getTemplate().getSpReward(attacker);
	}

	@Override
	protected void onDeath(Creature killer)
	{
		_deathTime = System.currentTimeMillis();
		setDecayed(false);
		long delay;
		if (isRaid() && !isRaidMinion())
		{
			delay = (_deathTime + (Config.RAID_BOSS_DECAY_TIME * 1000L));
		}
		else
		{
			if ((this instanceof Attackable) && (((Attackable) this).isSpoil() || ((Attackable) this).isSeeded()))
			{
				delay = (_deathTime + (Config.SPOILED_DECAY_TIME * 1000L));
			}
			else
			{
				delay = (_deathTime + (Config.NPC_DECAY_TIME * 1000L));
			}
		}
		
		DecayTaskManager.getInstance().addDecayTask(this, delay, false);
		
		if (killer != null && killer.isPlayer() && killer.isInFightEvent())
		{
			killer.getFightEvent().onKilled(killer, this);
		}

		_currentLHandId = getTemplate().getLeftHand();
		_currentRHandId = getTemplate().getRightHand();
		_currentCollisionHeight = getTemplate().getfCollisionHeight();
		_currentCollisionRadius = getTemplate().getfCollisionRadius();
		stopRandomAnimation();
		if (hasMinions())
		{
			getMinionList().onMasterDeath();
		}
		stoAllAbnormalEffects();
		super.onDeath(killer);
	}

	public void setSpawn(Spawner spawn)
	{
		_spawn = spawn;
	}

	@Override
	public void onSpawn()
	{
		super.onSpawn();
		_deathTime = 0L;
		setDecayed(false);
		if (getTemplate().getEventQuests(QuestEventType.ON_SPAWN) != null)
		{
			for (final Quest quest : getTemplate().getEventQuests(QuestEventType.ON_SPAWN))
			{
				quest.notifySpawn(this);
			}
		}

		if (!isTeleporting())
		{
			WalkingManager.getInstance().onSpawn(this);
		}
		getListeners().onSpawn();
		
		if (isGlobalAI() || isInActiveRegion())
		{
			startRandomAnimation();
		}
	}

	@Override
	public void onDecay()
	{
		if (isDecayed())
		{
			return;
		}
		setDecayed(true);
		
		super.onDecay();
		getListeners().onDecay();
		WalkingManager.getInstance().onDeath(this);
		if (!hasMinions() || !getMinionList().hasAliveMinions())
		{
			if (_spawn != null)
			{
				_spawn.decreaseCount(this);
			}
			else
			{
				deleteMe();
			}
		}
	}
	
	public final void setDecayed(boolean mode)
	{
		_isDecayed = mode;
	}
	
	public final boolean isDecayed()
	{
		return _isDecayed;
	}
	
	@Override
	protected void onDelete()
	{
		try
		{
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
		}
		catch (final Exception e)
		{
			_log.warn("deleteMe()", e);
		}
		
		if (!hasMinions() || !getMinionList().hasAliveMinions())
		{
			if (_spawn != null)
			{
				_spawn.decreaseCount(this);
			}
		}
		
		if (hasMinions())
		{
			getMinionList().onMasterDelete();
		}
		
		final Npc leader = getLeader();
		if (leader != null && leader.hasMinions())
		{
			leader.getMinionList().onMinionDelete();
		}
		super.onDelete();
	}
	
	@Override
	protected void onDespawn()
	{
		stopRandomAnimation();
		super.onDespawn();
	}

	public Spawner getSpawn()
	{
		return _spawn;
	}
	
	@Override
	public Location getSpawnedLoc()
	{
		return _spawn != null ? _spawn.getLocation() : null;
	}

	@Override
	public String toString()
	{
		return getClass().getSimpleName() + ":" + getName(null) + "(" + getId() + ")" + "[" + getObjectId() + "]";
	}
	
	@Override
	public void endDecayTask()
	{
		DecayTaskManager.getInstance().cancelDecayTask(this);
		onDecay();
	}

	public boolean isMob()
	{
		return false;
	}

	public void setLHandId(int newWeaponId)
	{
		_currentLHandId = newWeaponId;
		updateAbnormalEffect();
	}

	public void setRHandId(int newWeaponId)
	{
		_currentRHandId = newWeaponId;
		updateAbnormalEffect();
	}

	public void setLRHandId(int newLWeaponId, int newRWeaponId)
	{
		_currentRHandId = newRWeaponId;
		_currentLHandId = newLWeaponId;
		updateAbnormalEffect();
	}

	public void setEnchant(int newEnchantValue)
	{
		_currentEnchant = newEnchantValue;
		updateAbnormalEffect();
	}

	public boolean isShowName()
	{
		return !getTemplate().isShowName();
	}
	
	@Override
	public boolean isTargetable()
	{
		if (!_isTargetable)
		{
			return false;
		}
		return !getTemplate().isTargetable();
	}
	
	public void setIsTargetable(boolean val)
	{
		_isTargetable = val;
	}

	public void setCollisionHeight(double height)
	{
		_currentCollisionHeight = height;
	}

	public void setCollisionRadius(double radius)
	{
		_currentCollisionRadius = radius;
	}

	public double getCollisionHeight()
	{
		return _currentCollisionHeight;
	}

	public double getCollisionRadius()
	{
		return _currentCollisionRadius;
	}

	@Override
	public void sendInfo(Player activeChar)
	{
		if (isVisibleFor(activeChar))
		{
			if (Config.CHECK_KNOWN && activeChar.isGM())
			{
				activeChar.sendMessage("Added NPC: " + getName(activeChar.getLang()));
			}

			if (getRunSpeed() == 0)
			{
				activeChar.sendPacket(new ServerObjectInfo(this, activeChar));
			}
			else
			{
				activeChar.sendPacket(new NpcInfo.Info(this, activeChar));
			}
			if (getTriggerId() != 0)
			{
				activeChar.sendPacket(new EventTrigger(getTriggerId(), false));
			}
			
			if (isMoving())
			{
				activeChar.sendPacket(new MoveToLocation(this));
			}
		}
	}

	public void showNoTeachHtml(Player player)
	{
		final int npcId = getId();
		String html = "";

		if (this instanceof WarehouseInstance)
		{
			html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/warehouse/" + npcId + "-noteach.htm");
		}
		else if (this instanceof TrainerInstance)
		{
			html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/trainer/" + npcId + "-noteach.htm");
			if (html == null)
			{
				html = HtmCache.getInstance().getHtm(player, player.getLang(),  "data/html/scripts/custom/HealerTrainer/" + npcId + "-noteach.htm");
			}
		}

		final NpcHtmlMessage noTeachMsg = new NpcHtmlMessage(getObjectId());
		if (html == null)
		{
			_log.warn("Npc " + npcId + " missing noTeach html!");
			noTeachMsg.setHtml(player, "<html><body>I cannot teach you any skills.<br>You must find your current class teachers.</body></html>");
		}
		else
		{
			noTeachMsg.setHtml(player, html);
			noTeachMsg.replace("%objectId%", String.valueOf(getObjectId()));
		}
		player.sendPacket(noTeachMsg);
	}

	public Npc scheduleDespawn(long delay)
	{
		stopDespawn();
		_despawnTask = ThreadPoolManager.getInstance().schedule(new DespawnTask(), delay);
		return this;
	}
	
	public class DespawnTask implements Runnable
	{
		@Override
		public void run()
		{
			if (!isDecayed())
			{
				deleteMe();
			}
		}
	}
	
	public void stopDespawn()
	{
		if (_despawnTask != null)
		{
			_despawnTask.cancel(false);
			_despawnTask = null;
		}
	}

	@Override
	protected final void notifyQuestEventSkillFinished(Skill skill, GameObject target)
	{
		try
		{
			if (getTemplate().getEventQuests(QuestEventType.ON_SPELL_FINISHED) != null)
			{
				Player player = null;
				if (target != null)
				{
					player = target.getActingPlayer();
				}
				for (final Quest quest : getTemplate().getEventQuests(QuestEventType.ON_SPELL_FINISHED))
				{
					quest.notifySpellFinished(this, player, skill);
				}
			}
		}
		catch (final Exception e)
		{
			_log.warn("", e);
		}
	}

	@Override
	public boolean isMovementDisabled()
	{
		return super.isMovementDisabled() || (getTemplate().isMovementDisabled()) || (getAI() instanceof Corpse);
	}

	public String getAiType()
	{
		return getTemplate().getAI();
	}

	public void setDisplayEffect(int val)
	{
		if (val != _displayEffect)
		{
			_displayEffect = val;
			broadcastPacketToOthers(2000, new ExChangeNpcState(getObjectId(), val));
		}
	}

	public int getDisplayEffect()
	{
		return _displayEffect;
	}

	public int getColorEffect()
	{
		return 0;
	}

	public void broadcastNpcSay(String text)
	{
		broadcastNpcSay(0, text);
	}

	public void broadcastNpcSay(int messageType, String text)
	{
		broadcastPacketToOthers(2000, new NpcSay(getObjectId(), messageType, getId(), text));
	}

	public Creature getSummoner()
	{
		return _summoner;
	}

	public void setSummoner(Creature summoner)
	{
		_summoner = summoner;
	}

	@Override
	public boolean isNpc()
	{
		return true;
	}

	@Override
	public void setTeam(int id)
	{
		super.setTeam(id);
		broadcastInfo();
	}

	public boolean hasRandomAnimation()
	{
		return Config.MAX_NPC_ANIMATION > 0;
	}

	@Override
	public boolean isWalker()
	{
		return WalkingManager.getInstance().isRegistered(this);
	}

	@Override
	public boolean isRunner()
	{
		return _isRunner;
	}

	public void setIsRunner(boolean status)
	{
		_isRunner = status;
	}

	@Override
	public boolean isSpecialCamera()
	{
		return _isSpecialCamera;
	}

	public void setIsSpecialCamera(boolean status)
	{
		_isSpecialCamera = status;
	}

	@Override
	public boolean isEkimusFood()
	{
		return _isEkimusFood;
	}

	public void setIsEkimusFood(boolean status)
	{
		_isEkimusFood = status;
	}

	@Override
	public boolean isChargedShot(ShotType type)
	{
		return (_shotsMask & type.getMask()) == type.getMask();
	}

	@Override
	public void setChargedShot(ShotType type, boolean charged)
	{
		if (charged)
		{
			_shotsMask |= type.getMask();
		}
		else
		{
			_shotsMask &= ~type.getMask();
		}
	}

	@Override
	public void rechargeShots(boolean physical, boolean magic)
	{
		if (getTemplate().getShots() != ShotsType.NONE)
		{
			if (physical)
			{
				if (getTemplate().getShots() == ShotsType.SOUL || getTemplate().getShots() == ShotsType.SOUL_SPIRIT || getTemplate().getShots() == ShotsType.SOUL_BSPIRIT)
				{
					if (Rnd.get(100) > Config.SOULSHOT_CHANCE)
					{
						return;
					}
					broadcastPacketToOthers(600, new MagicSkillUse(this, this, 2154, 1, 0, 0));
					setChargedShot(ShotType.SOULSHOTS, true);
				}
			}
			if (magic)
			{
				if (getTemplate().getShots() == ShotsType.SPIRIT || getTemplate().getShots() == ShotsType.SOUL_SPIRIT || getTemplate().getShots() == ShotsType.SOUL_BSPIRIT)
				{
					if (Rnd.get(100) > Config.SPIRITSHOT_CHANCE)
					{
						return;
					}
					broadcastPacketToOthers(600, new MagicSkillUse(this, this, 2061, 1, 0, 0));
					setChargedShot(ShotType.SPIRITSHOTS, true);
				}
			}
		}
	}

	public int getScriptValue()
	{
		return getVariables().getInteger("script_val");
	}
	
	public void setScriptValue(int val)
	{
		getVariables().set("script_val", val);
	}
	
	public boolean isScriptValue(int val)
	{
		return getVariables().getInteger("script_val") == val;
	}
	
	public boolean hasVariables()
	{
		return getScript(NpcStats.class) != null;
	}
	
	public NpcStats getVariables()
	{
		final NpcStats vars = getScript(NpcStats.class);
		return vars != null ? vars : addScript(new NpcStats());
	}
	
	public void broadcastEvent(String eventName, int radius, GameObject reference)
	{
		for (final Npc obj : World.getInstance().getAroundNpc(this, radius, 200))
		{
			if (obj.getTemplate().getEventQuests(QuestEventType.ON_EVENT_RECEIVED) != null)
			{
				for (final Quest quest : obj.getTemplate().getEventQuests(QuestEventType.ON_EVENT_RECEIVED))
				{
					quest.notifyEventReceived(eventName, this, obj, reference);
				}
			}
		}
	}
	
	public void sendScriptEvent(Quest quest, String eventName, GameObject receiver, GameObject reference)
	{
		if (getTemplate().getEventQuests(QuestEventType.ON_EVENT_RECEIVED) != null)
		{
			getTemplate().getEventQuests(QuestEventType.ON_EVENT_RECEIVED).stream().filter(q -> q != null && quest.getId() == q.getId()).forEach(q -> q.notifyEventReceived(eventName, this, this, reference));
		}
	}

	@Override
	public boolean isInCategory(CategoryType type)
	{
		return CategoryParser.getInstance().isInCategory(type, getId());
	}

	@Override
	public Npc getActingNpc()
	{
		return this;
	}

	public final boolean isSevenSignsMonster()
	{
		return getFaction().getName().equalsIgnoreCase("c_dungeon_clan");
	}
	
	public boolean staysInSpawnLoc()
	{
		return ((getSpawn() != null) && (getSpawn().getX(this) == getX()) && (getSpawn().getY(this) == getY()));
	}

	@Override
	public boolean isVisibleFor(Creature creature)
	{
		if (creature.isPlayer())
		{
			if (getTemplate().getEventQuests(QuestEventType.ON_CAN_SEE_ME) != null)
			{
				for (final Quest quest : getTemplate().getEventQuests(QuestEventType.ON_CAN_SEE_ME))
				{
					return quest.notifyOnCanSeeMe(this, creature.getActingPlayer());
				}
			}
		}
		return super.isVisibleFor(creature);
	}

	public void broadcastSay(int chatType, NpcStringId npcStringId, String... parameters)
	{
		final NpcSay npcSay = new NpcSay(this, chatType, npcStringId);
		if (parameters != null)
		{
			for (final String parameter : parameters)
			{
				if (parameter != null)
				{
					npcSay.addStringParameter(parameter);
				}
			}
		}

		switch (chatType)
		{
			case Say2.NPC_ALL :
			{
				broadcastPacketToOthers(1250, npcSay);
				break;
			}
			default :
			{
				broadcastPacketToOthers(npcSay);
				break;
			}
		}
	}

	public int calculateLevelDiffForDrop(int charLevel)
	{
		return calculateLevelDiffForDrop(getLevel(), charLevel, (this instanceof RaidBossInstance || this instanceof GrandBossInstance));
	}
	
	public static int calculateLevelDiffForDrop(int mobLevel, int charLevel, boolean boss)
	{
		if (!Config.DEEPBLUE_DROP_RULES)
		{
			return 0;
		}
		final int deepblue_maxdiff = boss ? Config.DEEPBLUE_DROP_RAID_MAXDIFF : Config.DEEPBLUE_DROP_MAXDIFF;
		
		return Math.max(charLevel - mobLevel - deepblue_maxdiff, 0);
	}
	
	public void setRandomAnimationEnabled(boolean val)
	{
		_isRandomAnimationEnabled = val;
	}
	
	public boolean isRandomAnimationEnabled()
	{
		return _isRandomAnimationEnabled;
	}
	
	public Territory getTerritory()
	{
		if (getReflectionId() != 0)
		{
			return null;
		}
		
		if (_nearestTerritory == null)
		{
			if (getTemplate().getCastleId() == 0)
			{
				return null;
			}
			final int castleId = getId() == 30990 ? getCastle().getId() : getTemplate().getCastleId();
			_nearestTerritory = TerritoryWarManager.getInstance().getTerritory(castleId);
		}
		return _nearestTerritory;
	}
	
	@Override
	public boolean canBeAttacked()
	{
		return Config.ALT_ATTACKABLE_NPCS;
	}
	
	@Override
	public double getColRadius()
	{
		return getCollisionRadius();
	}
	
	@Override
	public double getColHeight()
	{
		return getCollisionHeight();
	}
	
	@Override
	public void addInfoObject(GameObject object)
	{
		if (object.isCreature())
		{
			final List<Quest> quests = getTemplate().getEventQuests(QuestEventType.ON_SEE_CREATURE);
			if (quests != null)
			{
				for (final Quest quest : quests)
				{
					quest.notifySeeCreature(this, (Creature) object, object.isSummon());
				}
			}
		}
	}
	
	public void setArena(Arena arena)
	{
		_arena = arena;
	}
	
	public Arena getArena()
	{
		return _arena;
	}
	
	public void setLeader(Npc leader)
	{
		_master = leader != null ? leader : null;
	}
	
	public Npc getLeader()
	{
		return _master != null ? _master : null;
	}
	
	@Override
	public boolean isMinion()
	{
		return _master != null;
	}
	
	public MinionList getMinionList()
	{
		if (_minionList == null)
		{
			_minionList = new MinionList(this);
		}
		return _minionList;
	}
	
	public boolean hasMinions()
	{
		return _minionList != null && _minionList.hasMinions();
	}
	
	public void notifyMinionDied(MonsterInstance minion)
	{
	}
	
	public void setIsRaidMinion(boolean val)
	{
	}
	
	public void spawnMinion(MonsterInstance minion)
	{
		minion.stopAllEffects();
		minion.setIsDead(false);
		minion.setCurrentHpMp(minion.getMaxHp(), minion.getMaxMp());
		minion.setScriptValue(0);
		minion.setHeading(getHeading());
		minion.setReflection(getReflection());
		if (!minion.isCanSupportMinion() && minion.getMinionList() != null)
		{
			minion.getMinionList().clearMinions();
		}
		final Location pos = getMinionPosition();
		if (pos != null)
		{
			minion.spawnMe(pos.getX(), pos.getY(), pos.getZ());
		}
	}
	
	@Override
	public Location getMinionPosition()
	{
		return Location.findPointToStay(this, 100, 150, false);
	}
	
	public boolean isCanSupportMinion()
	{
		return _isCanSupportMinion;
	}
	
	public void isCanSupportMinion(boolean canSupportMinion)
	{
		_isCanSupportMinion = canSupportMinion;
	}
	
	@Override
	public NpcListenerList getListeners()
	{
		if (_listeners == null)
		{
			synchronized (this)
			{
				if (_listeners == null)
				{
					_listeners = new NpcListenerList(this);
				}
			}
		}
		return (NpcListenerList) _listeners;
	}
	
	public <T extends AbstractNpcListener> boolean addListener(T listener)
	{
		return getListeners().add(listener);
	}
	
	public <T extends AbstractNpcListener> boolean removeListener(T listener)
	{
		return getListeners().remove(listener);
	}
	
	@Override
	public int getGeoZ(int x, int y, int z)
	{
		final int geoZ = super.getGeoZ(x, y, z);
		final var spawnedLoc = getSpawnedLoc();
		if (spawnedLoc != null && spawnedLoc.equals(x, y, z))
		{
			if (Math.abs(geoZ - z) > 200)
			{
				return z;
			}
		}
		return geoZ;
	}
	
	public void setUnAggred(boolean state)
	{
		_isUnAggred = state;
	}
	
	public void setTriggerId(int triggerId)
	{
		_triggerId = triggerId;
	}
	
	public int getTriggerId()
	{
		return _triggerId;
	}
}