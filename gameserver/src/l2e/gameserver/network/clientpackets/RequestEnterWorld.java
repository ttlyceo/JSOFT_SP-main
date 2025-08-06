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
package l2e.gameserver.network.clientpackets;

import java.text.SimpleDateFormat;
import java.util.Date;

import l2e.commons.util.TimeUtils;
import l2e.commons.util.TransferSkillUtils;
import l2e.gameserver.Announcements;
import l2e.gameserver.AutoRestart;
import l2e.gameserver.Config;
import l2e.gameserver.SevenSigns;
import l2e.gameserver.data.dao.CharacterPremiumDAO;
import l2e.gameserver.data.dao.DailyTasksDAO;
import l2e.gameserver.data.htm.HtmCache;
import l2e.gameserver.data.parser.AdminParser;
import l2e.gameserver.data.parser.PremiumAccountsParser;
import l2e.gameserver.data.parser.SkillTreesParser;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.handler.voicedcommandhandlers.VoicedCommandHandler;
import l2e.gameserver.instancemanager.CHSiegeManager;
import l2e.gameserver.instancemanager.CastleManager;
import l2e.gameserver.instancemanager.ClanHallManager;
import l2e.gameserver.instancemanager.CoupleManager;
import l2e.gameserver.instancemanager.CursedWeaponsManager;
import l2e.gameserver.instancemanager.FortManager;
import l2e.gameserver.instancemanager.FortSiegeManager;
import l2e.gameserver.instancemanager.MailManager;
import l2e.gameserver.instancemanager.OnlineRewardManager;
import l2e.gameserver.instancemanager.PetitionManager;
import l2e.gameserver.instancemanager.QuestManager;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.instancemanager.ServerVariables;
import l2e.gameserver.instancemanager.SiegeManager;
import l2e.gameserver.instancemanager.TerritoryWarManager;
import l2e.gameserver.model.Clan;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.PcCondOverride;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.ClassMasterInstance;
import l2e.gameserver.model.actor.instance.player.impl.TeleportTask;
import l2e.gameserver.model.entity.Couple;
import l2e.gameserver.model.entity.Fort;
import l2e.gameserver.model.entity.FortSiege;
import l2e.gameserver.model.entity.Siege;
import l2e.gameserver.model.entity.clanhall.AuctionableHall;
import l2e.gameserver.model.entity.clanhall.SiegableHall;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.olympiad.Olympiad;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.service.premium.PremiumTemplate;
import l2e.gameserver.model.strings.server.ServerMessage;
import l2e.gameserver.model.strings.server.ServerStorage;
import l2e.gameserver.model.zone.ZoneId;
import l2e.gameserver.network.GameClient.GameClientState;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.*;
import l2e.gameserver.network.serverpackets.ChangeWaitType;
import l2e.gameserver.network.serverpackets.Interface.ExDynamicPacket;
import l2e.gameserver.network.serverpackets.pledge.PledgeShowMemberListAll;
import l2e.gameserver.network.serverpackets.pledge.PledgeShowMemberListUpdate;
import l2e.gameserver.network.serverpackets.pledge.PledgeSkillList;
import l2e.gameserver.network.serverpackets.pledge.PledgeStatusChanged;
import top.jsoft.phaseevent.PhaseManager;

public class RequestEnterWorld extends GameClientPacket
{
	@Override
	protected void readImpl()
	{
	}
	
	@Override
	protected void runImpl()
	{
		final var activeChar = getClient().getActiveChar();
		if (activeChar == null)
		{
			getClient().closeNow(false);
			return;
		}
		onEnterWorld(activeChar);
	}
		
	public static void onEnterWorld(Player activeChar)
	{
		final boolean first = activeChar._entering;
		
		activeChar.getClient().setState(GameClientState.IN_GAME);
		if (first && activeChar.isGM())
		{
			if (Config.ENABLE_SAFE_ADMIN_PROTECTION)
			{
				if (Config.SAFE_ADMIN_NAMES.contains(activeChar.getName(null)))
				{
					activeChar.getAdminProtection().setIsSafeAdmin(true);
					if (Config.SAFE_ADMIN_SHOW_ADMIN_ENTER)
					{
						_log.info("Safe Admin: " + activeChar.getName(null) + "(" + activeChar.getObjectId() + ") has been logged in.");
					}
				}
				else
				{
					activeChar.getAdminProtection().punishUnSafeAdmin();
					_log.warn("WARNING: Unsafe Admin: " + activeChar.getName(null) + "(" + activeChar.getObjectId() + ") as been logged in.");
					_log.warn("If you have enabled some punishment, He will be punished.");
				}
			}

			if (activeChar.getAccessLevel().allowStartupInvulnerable() && AdminParser.getInstance().hasAccess("admin_invul", activeChar.getAccessLevel()))
			{
				activeChar.setIsInvul(true);
			}

			if (activeChar.getAccessLevel().allowStartupInvisible() && AdminParser.getInstance().hasAccess("admin_invisible", activeChar.getAccessLevel()))
			{
				activeChar.setInvisible(true);
			}

			if (activeChar.getAccessLevel().allowStartupSilence() && AdminParser.getInstance().hasAccess("admin_silence", activeChar.getAccessLevel()))
			{
				activeChar.setSilenceMode(true);
			}

			if (activeChar.getAccessLevel().allowStartupDietMode() && AdminParser.getInstance().hasAccess("admin_diet", activeChar.getAccessLevel()))
			{
				activeChar.setDietMode(true);
				activeChar.refreshOverloaded();
			}

			if (activeChar.getAccessLevel().allowStartupAutoList() && AdminParser.getInstance().hasAccess("admin_gmliston", activeChar.getAccessLevel()))
			{
				AdminParser.getInstance().addGm(activeChar, false);
			}
			else
			{
				AdminParser.getInstance().addGm(activeChar, true);
			}
			
			if (activeChar.getAccessLevel().allowGiveSpecialSkills())
			{
				SkillTreesParser.getInstance().addSkills(activeChar, false);
			}
			
			if (activeChar.getAccessLevel().allowGiveSpecialAuraSkills())
			{
				SkillTreesParser.getInstance().addSkills(activeChar, true);
			}
			
			if (activeChar.getAccessLevel().isGmSpeedAccess())
			{
				activeChar.doSimultaneousCast(SkillsParser.getInstance().getInfo(7029, 2));
			}
			
			if (activeChar.getAccessLevel().allowEnterAnnounce())
			{
				final var msg = new ServerMessage("GM.ANNOUNCE", true);
				msg.add(activeChar.getAccessLevel().getName());
				msg.add(activeChar.getName(null));
				Announcements.getInstance().announceToAll(msg);
			}
		}

		if (activeChar.getCurrentHp() < 0.5)
		{
			activeChar.setIsDead(true);
		}

		boolean showClanNotice = false;
		
		final Clan clan = activeChar.getClan();
		if (first && clan != null)
		{
			activeChar.sendPacket(new PledgeSkillList(clan));
			notifyClanMembers(activeChar);
			notifySponsorOrApprentice(activeChar);

			final AuctionableHall clanHall = ClanHallManager.getInstance().getClanHallByOwner(clan);
			if (clanHall != null && !clanHall.getPaid() && (clan.getWarehouse().getAdena() < clanHall.getLease()))
			{
				if (!clanHall.getPaid())
				{
					activeChar.sendPacket(SystemMessageId.PAYMENT_FOR_YOUR_CLAN_HALL_HAS_NOT_BEEN_MADE_PLEASE_MAKE_PAYMENT_TO_YOUR_CLAN_WAREHOUSE_BY_S1_TOMORROW);
				}
			}

			for (final Siege siege : SiegeManager.getInstance().getSieges())
			{
				if (!siege.getIsInProgress())
				{
					continue;
				}

				if (siege.checkIsAttacker(clan))
				{
					activeChar.setSiegeState((byte) 1);
					activeChar.setSiegeSide(siege.getCastle().getId());
				}

				else if (siege.checkIsDefender(clan))
				{
					activeChar.setSiegeState((byte) 2);
					activeChar.setSiegeSide(siege.getCastle().getId());
				}
			}

			for (final FortSiege siege : FortSiegeManager.getInstance().getSieges())
			{
				if (!siege.getIsInProgress())
				{
					continue;
				}

				if (siege.checkIsAttacker(clan))
				{
					activeChar.setSiegeState((byte) 1);
					activeChar.setSiegeSide(siege.getFort().getId());
				}

				else if (siege.checkIsDefender(clan))
				{
					activeChar.setSiegeState((byte) 2);
					activeChar.setSiegeSide(siege.getFort().getId());
				}
			}

			for (final SiegableHall hall : CHSiegeManager.getInstance().getConquerableHalls().values())
			{
				if (!hall.isInSiege())
				{
					continue;
				}

				if (hall.isRegistered(clan))
				{
					activeChar.setSiegeState((byte) 1);
					activeChar.setSiegeSide(hall.getId());
					activeChar.setIsInHideoutSiege(true);
				}
			}

			activeChar.sendPacket(new PledgeShowMemberListAll(clan, activeChar));
			activeChar.sendPacket(new PledgeStatusChanged(clan));

			if (clan.getCastleId() > 0)
			{
				CastleManager.getInstance().getCastleByOwner(clan).giveResidentialSkills(activeChar);
			}

			if (clan.getFortId() > 0)
			{
				FortManager.getInstance().getFortByOwner(clan).giveResidentialSkills(activeChar);
			}
			
			if (clan.getHideoutId() > 0)
			{
				final var hall = ClanHallManager.getInstance().getAbstractHallByOwner(clan);
				if (hall != null)
				{
					hall.giveResidentialSkills(activeChar);
				}
			}
			showClanNotice = clan.isNoticeEnabled();

			if (TerritoryWarManager.getInstance().getRegisteredTerritoryId(activeChar) > 0)
			{
				if (TerritoryWarManager.getInstance().isTWInProgress())
				{
					activeChar.setSiegeState((byte) 1);
				}
				activeChar.setSiegeSide(TerritoryWarManager.getInstance().getRegisteredTerritoryId(activeChar));
			}
		}

		if (SevenSigns.getInstance().isSealValidationPeriod() && (SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE) != SevenSigns.CABAL_NULL))
		{
			final int cabal = SevenSigns.getInstance().getPlayerCabal(activeChar.getObjectId());
			if (cabal != SevenSigns.CABAL_NULL)
			{
				if (cabal == SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE))
				{
					activeChar.addSkill(SkillsParser.FrequentSkill.THE_VICTOR_OF_WAR.getSkill());
				}
				else
				{
					activeChar.addSkill(SkillsParser.FrequentSkill.THE_VANQUISHED_OF_WAR.getSkill());
				}
			}
		}
		else
		{
			activeChar.removeSkill(SkillsParser.FrequentSkill.THE_VICTOR_OF_WAR.getSkill());
			activeChar.removeSkill(SkillsParser.FrequentSkill.THE_VANQUISHED_OF_WAR.getSkill());
		}

		if (Config.ENABLE_VITALITY && Config.RECOVER_VITALITY_ON_RECONNECT)
		{
			final float points = (Config.RATE_RECOVERY_ON_RECONNECT * (System.currentTimeMillis() - activeChar.getLastAccess())) / 60000;
			if (points > 0)
			{
				activeChar.updateVitalityPoints(points, false, true);
			}
		}

		activeChar._entering = false;
		activeChar.broadcastUserInfo(true);
		activeChar.getMacros().sendUpdate();
		activeChar.sendItemList(false);
		activeChar.sendPacket(new ExBrPremiumState(activeChar.getObjectId(), activeChar.hasPremiumBonus() ? 1 : 0));
		activeChar.checkPlayer();
		activeChar.queryGameGuard();
		activeChar.sendPacket(new ExGetBookMarkInfo(activeChar));
		activeChar.sendPacket(new ShortCutInit(activeChar));
		activeChar.sendPacket(ExBasicActionList.STATIC_PACKET);
		activeChar.sendPacket(new HennaInfo(activeChar));

		Quest.playerEnter(activeChar);

		if (!Config.DISABLE_TUTORIAL)
		{
			loadTutorial(activeChar);
		}

		if (first)
		{
			for (final Quest quest : QuestManager.getInstance().getQuests())
			{
				if ((quest != null) && quest.getOnEnterWorld())
				{
					quest.notifyEnterWorld(activeChar);
				}
			}
			TransferSkillUtils.checkTransferItems(activeChar);
			
			if (Config.PLAYER_SPAWN_PROTECTION > 0)
			{
				activeChar.setProtection(true);
			}
			activeChar.spawnMe(activeChar.getX(), activeChar.getY(), activeChar.getZ());
		}
		activeChar.sendPacket(new QuestList(activeChar));

		if (first && Config.ALLOW_WEDDING)
		{
			engage(activeChar);
			notifyPartner(activeChar, activeChar.getPartnerId());
		}

		if (first)
		{
			activeChar.getInventory().applyItemSkills();
			if (activeChar.isCursedWeaponEquipped())
			{
				CursedWeaponsManager.getInstance().getCursedWeapon(activeChar.getCursedWeaponEquippedId()).cursedOnLogin();
			}
		}

		activeChar.updateEffectIcons();

		if (Config.PC_BANG_ENABLED)
		{
			final boolean blockPacket = Config.PC_BANG_ONLY_FOR_PREMIUM && !activeChar.hasPremiumBonus();
			if (!blockPacket)
			{
				activeChar.sendPacket(activeChar.getPcBangPoints() > 0 ? new ExPCCafePointInfo(activeChar.getPcBangPoints(), 0, false, false, 1) : new ExPCCafePointInfo());
			}
		}

		activeChar.sendPacket(new EtcStatusUpdate(activeChar));
		activeChar.sendPacket(new ExStorageMaxCount(activeChar));

		if (first)
		{
			activeChar.startTimers();
			activeChar.sendPacket(new L2FriendList(activeChar));
			
			final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.FRIEND_S1_HAS_LOGGED_IN);
			sm.addString(activeChar.getName(null));
			for (final int id : activeChar.getFriendList())
			{
				final var obj = GameObjectsStorage.getPlayer(id);
				if (obj != null)
				{
					obj.sendPacket(sm);
				}
			}
			
			if (Config.NEW_CHAR_IS_HERO)
			{
				activeChar.setHero(true, true);
			}
		}

		activeChar.sendPacket(SystemMessageId.WELCOME_TO_LINEAGE);
		SevenSigns.getInstance().sendCurrentPeriodMsg(activeChar);
		Announcements.getInstance().showAnnouncements(activeChar);

		if (AutoRestart.getInstance().getRestartNextTime() > 0)
		{
			final ServerMessage msg = new ServerMessage("AutoRestart.NEXT_TIME", activeChar.getLang());
			msg.add(TimeUtils.formatTime(activeChar, (int) AutoRestart.getInstance().getRestartNextTime(), false));
			final CreatureSay msg3 = new CreatureSay(0, Say2.BATTLEFIELD, ServerStorage.getInstance().getString(activeChar.getLang(), "AutoRestart.TITLE"), msg.toString());
			activeChar.sendPacket(msg3);
		}
		
		if (Config.ALLOW_PRINT_OLY_INFO)
		{
			final boolean isOlyActive = !Olympiad.getInstance().isOlympiadEnd();
			final long olyTime = isOlyActive ? Olympiad.getInstance().getOlympiadEndDate() : Olympiad.getInstance().getValidationEndDate();
			final ServerMessage msg = new ServerMessage(isOlyActive ? "Olympiad.END_DATE" : "Olympiad.NEXT_DATE", activeChar.getLang());
			msg.add(new SimpleDateFormat("yyyy.MM.dd HH:mm:ss").format(new Date(olyTime)));
			final CreatureSay msg3 = new CreatureSay(0, Say2.BATTLEFIELD, ServerStorage.getInstance().getString(activeChar.getLang(), "Olympiad.OLIMPIAD"), msg.toString());
			activeChar.sendPacket(msg3);
		}

		if (showClanNotice)
		{
			final NpcHtmlMessage notice = new NpcHtmlMessage(1);
			notice.setFile(activeChar, activeChar.getLang(), "data/html/clanNotice.htm");
			notice.replace("%clan_name%", activeChar.getClan().getName());
			notice.replace("%notice_text%", activeChar.getClan().getNotice());
			activeChar.sendPacket(notice);
		}
		else if (Config.SERVER_NEWS)
		{
			final String serverNews = HtmCache.getInstance().getHtm(activeChar, activeChar.getLang(), "data/html/servnews.htm");
			if (serverNews != null)
			{
				activeChar.sendPacket(new NpcHtmlMessage(activeChar, 1, serverNews));
			}
		}

		if (Config.PETITIONING_ALLOWED)
		{
			PetitionManager.getInstance().checkPetitionMessages(activeChar);
		}

		if (Config.ONLINE_PLAYERS_AT_STARTUP)
		{
			activeChar.sendMessage("" + ServerStorage.getInstance().getString(activeChar.getLang(), "OnlinePlayers.ONLINE") + " " + GameObjectsStorage.getAllPlayersCount());
		}

		if (activeChar.isAlikeDead())
		{
			activeChar.sendPacket(new Die(activeChar));
		}

		if (activeChar.isSitting())
		{
			activeChar.sendPacket(new ChangeWaitType(activeChar, ChangeWaitType.WT_SITTING));
		}
		
		activeChar.sendSkillList(true);
		
		if (first)
		{
			if (activeChar.getPrivateStoreType() != Player.STORE_PRIVATE_NONE)
			{
				if (activeChar.getPrivateStoreType() == Player.STORE_PRIVATE_BUY)
				{
					activeChar.sendPacket(new PrivateStoreBuyMsg(activeChar));
				}
				else if (activeChar.getPrivateStoreType() == Player.STORE_PRIVATE_SELL)
				{
					activeChar.sendPacket(new PrivateStoreSellMsg(activeChar));
				}
				else if (activeChar.getPrivateStoreType() == Player.STORE_PRIVATE_PACKAGE_SELL)
				{
					activeChar.sendPacket(new ExPrivateStorePackageMsg(activeChar));
				}
				else if (activeChar.getPrivateStoreType() == Player.STORE_PRIVATE_MANUFACTURE)
				{
					activeChar.sendPacket(new RecipeShopMsg(activeChar));
				}
				
				if (activeChar.getVar("offlineBuff") != null)
				{
					final var cmd = VoicedCommandHandler.getInstance().getHandler("sellbuff");
					if (cmd != null)
					{
						activeChar.setIsSellingBuffs(true);
						activeChar.unsetVar("offlineBuff");
						cmd.useVoicedCommand("sellbuff", activeChar, null);
					}
				}
			}
			activeChar.unsetVar("offline");
			activeChar.sendPacket(new ExReceiveShowPostFriend(activeChar));
			activeChar.getNevitSystem().onEnterWorld();
			
			activeChar.onPlayerEnter();

			for (final ItemInstance i : activeChar.getInventory().getItems())
			{
				if (i.isTimeLimitedItem())
				{
					i.scheduleLifeTimeTask();
				}
				if (i.isShadowItem() && i.isEquipped())
				{
					i.decreaseMana(false);
				}
				if (i.isEnergyItem() && i.isEquipped())
				{
					i.decreaseEnergy(false);
				}
			}
			
			for (final ItemInstance i : activeChar.getWarehouse().getItems())
			{
				if (i.isTimeLimitedItem())
				{
					i.scheduleLifeTimeTask();
				}
			}
			
			if (activeChar.getClanJoinExpiryTime() > System.currentTimeMillis())
			{
				activeChar.sendPacket(SystemMessageId.CLAN_MEMBERSHIP_TERMINATED);
			}

			if (activeChar.getInventory().getItemByItemId(9819) != null)
			{
				final Fort fort = FortManager.getInstance().getFort(activeChar);

				if (fort != null)
				{
					FortSiegeManager.getInstance().dropCombatFlag(activeChar, fort.getId());
				}
				else
				{
					final int slot = activeChar.getInventory().getSlotFromItem(activeChar.getInventory().getItemByItemId(9819));
					activeChar.getInventory().unEquipItemInBodySlot(slot);
					activeChar.destroyItem("CombatFlag", activeChar.getInventory().getItemByItemId(9819), null, true);
				}
			}
			
			if (!activeChar.canOverrideCond(PcCondOverride.ZONE_CONDITIONS) && activeChar.isInsideZone(ZoneId.SIEGE) && (!activeChar.isInSiege() || (activeChar.getSiegeState() < 2)))
			{
				activeChar.getPersonalTasks().addTask(new TeleportTask(2000, null));
			}
			
			if (Config.ALLOW_MAIL)
			{
				if (MailManager.getInstance().hasUnreadPost(activeChar))
				{
					activeChar.sendPacket(ExNoticePostArrived.valueOf(false));
				}
			}
		}

		if (Config.WELCOME_MESSAGE_ENABLED)
		{
			activeChar.sendPacket(new ExShowScreenMessage(Config.WELCOME_MESSAGE_TEXT, Config.WELCOME_MESSAGE_TIME));
		}

		if (first)
		{
			ClassMasterInstance.showQuestionMark(activeChar);
			activeChar.checkBirthDay();
		}

		if (!first)
		{
			if (activeChar.inObserverMode())
			{
				activeChar.leaveObserverMode();
			}
			else if (activeChar.isVisible())
			{
				activeChar.refreshInfos();
			}
			
			if (activeChar.isTeleporting())
			{
				activeChar.onTeleported();
			}
			
			final var summon = activeChar.getSummon();
			if (summon != null)
			{
				activeChar.sendPacket(new PetInfo(summon, 0));
				if (summon.isPet())
				{
					activeChar.sendPacket(new PetItemList(summon.getInventory().getItems()));
				}
			}
			
			if (activeChar.isInParty())
			{
				final var party = activeChar.getParty();
				final var leader = party.getLeader();
				if (leader != null)
				{
					activeChar.sendPacket(new PartySmallWindowAll(activeChar, party));
					activeChar.broadcastRelationChanged();
					if (party.isInCommandChannel())
					{
						activeChar.sendPacket(ExOpenMPCC.STATIC);
					}
				}
			}
			
			activeChar.getAppearance().setVisibleTitle(activeChar.getTitle(null));
			if (activeChar.getVar("titlecolor") != null)
			{
				activeChar.getAppearance().setTitleColor(Integer.parseInt(activeChar.getVar("titlecolor")));
			}
			else
			{
				activeChar.getAppearance().setTitleColor(0xFFFF77);
			}
			
			activeChar.sendActiveAutoShots();
			activeChar.broadcastCharInfo();
		}
		
		if (!activeChar.getPremiumItemList().isEmpty())
		{
			activeChar.sendPacket(ExNotifyPremiumItem.STATIC_PACKET);
		}

		if (first)
		{
			if (Config.AUTO_GIVE_PREMIUM && Config.USE_PREMIUMSERVICE)
			{
				if (!activeChar.hasPremiumBonus())
				{
					final String var = ServerVariables.getString("Premium-" + activeChar.getAccountName(), null);
					if (var == null)
					{
						final PremiumTemplate tpl = PremiumAccountsParser.getInstance().getPremiumTemplate(Config.GIVE_PREMIUM_ID);
						if (tpl != null)
						{
							final long time = !tpl.isOnlineType() ? (System.currentTimeMillis() + (tpl.getTime() * 1000)) : 0;
							if (tpl.isPersonal())
							{
								CharacterPremiumDAO.getInstance().updatePersonal(activeChar, tpl.getId(), time, tpl.isOnlineType());
							}
							else
							{
								CharacterPremiumDAO.getInstance().update(activeChar, tpl.getId(), time, tpl.isOnlineType());
							}
							ServerVariables.set("Premium-" + activeChar.getAccountName(), time);
						}
					}
				}
			}
			
			if (activeChar.getUseAutoHpPotions())
			{
				activeChar.startHpPotionTask();
			}
			
			if (activeChar.getUseAutoMpPotions())
			{
				activeChar.startMpPotionTask();
			}
			
			if (activeChar.getUseAutoCpPotions())
			{
				activeChar.startCpPotionTask();
			}
			
			if (activeChar.getUseAutoSoulPotions())
			{
				activeChar.startSoulPotionTask();
			}
			
			if (Config.ALLOW_DAILY_REWARD)
			{
				activeChar.restoreDailyRewards();
			}
			
			if (Config.ALLOW_DAILY_TASKS)
			{
				DailyTasksDAO.getInstance().restoreDailyTasks(activeChar);
			}
			
			if (Config.ALLOW_REVENGE_SYSTEM)
			{
				activeChar.getRevengeMark();
			}
		}
		
		activeChar.sendVoteSystemInfo();
		activeChar.updateAndBroadcastStatus(1);
		activeChar.sendActionFailed();

		if (activeChar.getPrivateStoreType() != 0 && !activeChar.canOpenPrivateStore(true, activeChar.isSellingBuffs()))
		{
			activeChar.setPrivateStoreType(0);
			activeChar.standUp();
		}
		
		if (first)
		{
			if (Config.ALLOW_CUSTOM_INTERFACE)
			{
				activeChar.sendPacket(new ExDynamicPacket(0, 0));
			}
			activeChar.getFarmSystem().restoreVariables();
			ReflectionManager.getInstance().restoreReflectionTimes(activeChar);
			OnlineRewardManager.getInstance().checkOnlineReward(activeChar);
		}

		if(Config.PHASEEVENT_ENABLE && PhaseManager.getInstance().getCurrentPhase().equalsIgnoreCase(PhaseManager.PHASE_REDMOON))
			activeChar.sendPacket(new ExRedSky(7200));
	}

	private static void engage(Player cha)
	{
		final int chaId = cha.getObjectId();

		for (final Couple cl : CoupleManager.getInstance().getCouples())
		{
			if ((cl.getPlayer1Id() == chaId) || (cl.getPlayer2Id() == chaId))
			{
				if (cl.getMaried())
				{
					cha.setMarried(true);
				}

				cha.setCoupleId(cl.getId());

				if (cl.getPlayer1Id() == chaId)
				{
					cha.setPartnerId(cl.getPlayer2Id());
				}
				else
				{
					cha.setPartnerId(cl.getPlayer1Id());
				}
			}
		}
	}

	private static void notifyPartner(Player cha, int partnerId)
	{
		final int objId = cha.getPartnerId();
		if (objId != 0)
		{
			final Player partner = GameObjectsStorage.getPlayer(objId);
			if (partner != null)
			{
				partner.sendMessage("Your Partner has logged in.");
			}
		}
	}

	private static void notifyClanMembers(Player activeChar)
	{
		final Clan clan = activeChar.getClan();
		if ((clan != null) && (clan.getClanMember(activeChar.getObjectId()) != null))
		{
			clan.getClanMember(activeChar.getObjectId()).setPlayerInstance(activeChar);

			final SystemMessage msg = SystemMessage.getSystemMessage(SystemMessageId.CLAN_MEMBER_S1_LOGGED_IN);
			msg.addString(activeChar.getName(null));
			clan.broadcastToOtherOnlineMembers(msg, activeChar);
			clan.broadcastToOtherOnlineMembers(new PledgeShowMemberListUpdate(activeChar), activeChar);
		}
	}

	private static void notifySponsorOrApprentice(Player activeChar)
	{
		if (activeChar.getSponsor() != 0)
		{
			final Player sponsor = GameObjectsStorage.getPlayer(activeChar.getSponsor());
			if (sponsor != null)
			{
				final SystemMessage msg = SystemMessage.getSystemMessage(SystemMessageId.YOUR_APPRENTICE_S1_HAS_LOGGED_IN);
				msg.addString(activeChar.getName(null));
				sponsor.sendPacket(msg);
			}
		}
		else if (activeChar.getApprentice() != 0)
		{
			final Player apprentice = GameObjectsStorage.getPlayer(activeChar.getApprentice());
			if (apprentice != null)
			{
				final SystemMessage msg = SystemMessage.getSystemMessage(SystemMessageId.YOUR_SPONSOR_C1_HAS_LOGGED_IN);
				msg.addString(activeChar.getName(null));
				apprentice.sendPacket(msg);
			}
		}
	}

	private static void loadTutorial(Player player)
	{
		final QuestState qs = player.getQuestState("_255_Tutorial");
		if (qs != null)
		{
			qs.getQuest().notifyEvent("UC", null, player);
		}
	}

	@Override
	protected boolean triggersOnActionRequest()
	{
		return false;
	}
}