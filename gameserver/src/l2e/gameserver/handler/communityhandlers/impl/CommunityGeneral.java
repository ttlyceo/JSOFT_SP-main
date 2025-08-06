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
package l2e.gameserver.handler.communityhandlers.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.locks.ReentrantLock;

import l2e.commons.dbutils.DbUtils;
import l2e.commons.util.TimeUtils;
import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.GameServer;
import l2e.gameserver.GameTimeController;
import l2e.gameserver.data.dao.CharacterPremiumDAO;
import l2e.gameserver.data.holder.ClanHolder;
import l2e.gameserver.data.htm.HtmCache;
import l2e.gameserver.data.parser.AugmentationParser;
import l2e.gameserver.data.parser.BuyListParser;
import l2e.gameserver.data.parser.ClanParser;
import l2e.gameserver.data.parser.DonationParser;
import l2e.gameserver.data.parser.ExchangeItemParser;
import l2e.gameserver.data.parser.ExperienceParser;
import l2e.gameserver.data.parser.InitialShortcutParser;
import l2e.gameserver.data.parser.ItemsParser;
import l2e.gameserver.data.parser.MultiSellParser;
import l2e.gameserver.data.parser.OptionsParser;
import l2e.gameserver.data.parser.PremiumAccountsParser;
import l2e.gameserver.data.parser.SkillTreesParser;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.handler.communityhandlers.CommunityBoardHandler;
import l2e.gameserver.handler.communityhandlers.ICommunityBoardHandler;
import l2e.gameserver.handler.communityhandlers.impl.model.CertificationUtils;
import l2e.gameserver.handler.voicedcommandhandlers.VoicedCommandHandler;
import l2e.gameserver.instancemanager.PunishmentManager;
import l2e.gameserver.instancemanager.QuestManager;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.listener.player.impl.AugmentationAnswerListener;
import l2e.gameserver.listener.player.impl.ChangeMainClassListener;
import l2e.gameserver.listener.player.impl.ChangeSubClassListener;
import l2e.gameserver.listener.player.impl.ClanLevelAnswerListener;
import l2e.gameserver.listener.player.impl.EnchantOlfAnswerListner;
import l2e.gameserver.listener.player.impl.HeroAnswerListener;
import l2e.gameserver.listener.player.impl.LevelAnswerListener;
import l2e.gameserver.listener.player.impl.NooblesAnswerListener;
import l2e.gameserver.listener.player.impl.RecoveryKarmaAnswerListener;
import l2e.gameserver.listener.player.impl.RecoveryPkAnswerListener;
import l2e.gameserver.listener.player.impl.RecoveryVitalityAnswerListener;
import l2e.gameserver.listener.player.impl.ReputationAnswerListener;
import l2e.gameserver.model.Clan;
import l2e.gameserver.model.Elementals;
import l2e.gameserver.model.Elementals.Elemental;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.SkillLearn;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.items.Item;
import l2e.gameserver.model.actor.templates.items.Weapon;
import l2e.gameserver.model.base.AcquireSkillType;
import l2e.gameserver.model.base.BonusType;
import l2e.gameserver.model.base.ClassId;
import l2e.gameserver.model.base.PlayerClass;
import l2e.gameserver.model.base.Race;
import l2e.gameserver.model.base.SubClass;
import l2e.gameserver.model.entity.Duel;
import l2e.gameserver.model.entity.events.cleft.AerialCleftEvent;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.items.itemcontainer.Inventory;
import l2e.gameserver.model.olympiad.Olympiad;
import l2e.gameserver.model.punishment.PunishmentAffect;
import l2e.gameserver.model.punishment.PunishmentType;
import l2e.gameserver.model.service.donate.Donation;
import l2e.gameserver.model.service.exchange.Change;
import l2e.gameserver.model.service.exchange.Variant;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.skills.funcs.FuncTemplate;
import l2e.gameserver.model.skills.options.Options;
import l2e.gameserver.model.skills.options.Options.AugmentationFilter;
import l2e.gameserver.model.stats.Stats;
import l2e.gameserver.model.strings.server.ServerMessage;
import l2e.gameserver.model.strings.server.ServerStorage;
import l2e.gameserver.model.zone.ZoneId;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.AcquireSkillDone;
import l2e.gameserver.network.serverpackets.BuyList;
import l2e.gameserver.network.serverpackets.ExAcquirableSkillListByClass;
import l2e.gameserver.network.serverpackets.ExBuySellList;
import l2e.gameserver.network.serverpackets.ExShowVariationCancelWindow;
import l2e.gameserver.network.serverpackets.ExShowVariationMakeWindow;
import l2e.gameserver.network.serverpackets.ExStorageMaxCount;
import l2e.gameserver.network.serverpackets.HennaEquipList;
import l2e.gameserver.network.serverpackets.HennaUnequipList;
import l2e.gameserver.network.serverpackets.InventoryUpdate;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;
import l2e.gameserver.network.serverpackets.ShortCutInit;
import l2e.gameserver.network.serverpackets.ShowBoard;
import l2e.gameserver.network.serverpackets.SocialAction;
import l2e.gameserver.network.serverpackets.SystemMessage;
import l2e.gameserver.network.serverpackets.WareHouseDepositList;
import l2e.gameserver.network.serverpackets.WareHouseWithdrawList;

public class CommunityGeneral extends AbstractCommunity implements ICommunityBoardHandler
{
	private static final String[] _vars = new String[]
	{
	        "FOUNDATION", "ENCHANT", "ATTRIBUTION"
	};
	
	private static final String[] SUB_VAR_NAMES =
	{
	        "EmergentAbility65-", "EmergentAbility70-", "ClassAbility75-", "ClassAbility80-"
	};
	
	private static final int[] CLASSITEMS =
	{
	        10281, 10282, 10283, 10287, 10284, 10286, 10285
	};
	
	private static final int[] TRANSFORMITEMS =
	{
	        10289, 10288, 10290, 10293, 10292, 10294, 10291
	};
	
	private static final ReentrantLock _changeClassLock = new ReentrantLock();
	
	public CommunityGeneral()
	{
		if (Config.DEBUG)
		{
			_log.info(getClass().getSimpleName() + ": Loading all functions.");
		}
	}
	
	@Override
	public String[] getBypassCommands()
	{
		return new String[]
		{
		        "_bbshome", "_bbsCustomSkillGroup", "_bbslang", "_bbsCmdlang", "_bbsabout", "_bbspage", "_bbshtm", "_bbsmod", "_bbsvoice", "_bbsmultisell;", "_bbsmsell;", "_bbsExcMultisell;", "_bbsExcMsell;", "_bbslistclanskills", "_bbslearnclanskills", "_bbspremium", "_bbspremiumPage", "_bbspremiumOnlyPage", "_bbspremiumBuy", "_bbspremiumOnlyBuy", "_bbspremiumList", "_bbswarhouse", "_bbsAugment", "_bbsdraw", "_bbsservice", "_bbsunban", "_bbsSkillStatInfo"
		};
	}

	@Override
	public void onBypassCommand(String command, Player activeChar)
	{
		if (command.startsWith("_bbshtm"))
		{
			final var st = new StringTokenizer(command, ":");
			st.nextToken();
			final var page = st.nextToken();
			
			final var htm = new NpcHtmlMessage(5);
			htm.setFile(activeChar, activeChar.getLang(), "data/html/community/" + page + ".htm");
			activeChar.sendPacket(htm);
			return;
		}
		else if (command.startsWith("_bbslang"))
		{
			final var st = new StringTokenizer(command, " ");
			st.nextToken();
			
			String lang = null;
			String page = null;
			try
			{
				lang = st.nextToken();
			}
			catch (final Exception e)
			{
			}
			
			try
			{
				page = st.nextToken();
			}
			catch (final Exception e)
			{
			}
			
			if (lang != null && page != null)
			{
				if (!Config.MULTILANG_ALLOWED.contains(lang))
				{
					String answer = "" + ServerStorage.getInstance().getString(activeChar.getLang(), "Lang.WRONG_LANG") + "";
					for (final String lng : Config.MULTILANG_ALLOWED)
					{
						answer += " " + lng;
					}
					activeChar.sendMessage(answer);
				}
				else
				{
					activeChar.setLang(lang);
					if (lang.equalsIgnoreCase("en"))
					{
						activeChar.sendMessage("" + ServerStorage.getInstance().getString(activeChar.getLang(), "Lang.EN_LANG") + "");
					}
					else if (lang.equalsIgnoreCase("ru"))
					{
						activeChar.sendMessage("" + ServerStorage.getInstance().getString(activeChar.getLang(), "Lang.RU_LANG") + "");
					}
				}
				
				var html = HtmCache.getInstance().getHtm(activeChar, activeChar.getLang(), "data/html/community/" + page + ".htm");
				if (page.equalsIgnoreCase("index"))
				{
					html = getPlayerInfo(html, activeChar);
				}
				if (page.equalsIgnoreCase("about"))
				{
					html = getServerInfo(html, activeChar);
				}
				separateAndSend(html, activeChar);
				return;
			}
		}
		else if (command.startsWith("_bbsCmdlang"))
		{
			final var st = new StringTokenizer(command, " ");
			st.nextToken();
			
			String lang = null;
			String cmd = null;
			try
			{
				lang = st.nextToken();
			}
			catch (final Exception e)
			{
			}
			
			try
			{
				cmd = st.nextToken();
			}
			catch (final Exception e)
			{
			}
			
			if (lang != null && cmd != null)
			{
				if (!Config.MULTILANG_ALLOWED.contains(lang))
				{
					String answer = "" + ServerStorage.getInstance().getString(activeChar.getLang(), "Lang.WRONG_LANG") + "";
					for (final String lng : Config.MULTILANG_ALLOWED)
					{
						answer += " " + lng;
					}
					activeChar.sendMessage(answer);
				}
				else
				{
					activeChar.setLang(lang);
					if (lang.equalsIgnoreCase("en"))
					{
						activeChar.sendMessage("" + ServerStorage.getInstance().getString(activeChar.getLang(), "Lang.EN_LANG") + "");
					}
					else if (lang.equalsIgnoreCase("ru"))
					{
						activeChar.sendMessage("" + ServerStorage.getInstance().getString(activeChar.getLang(), "Lang.RU_LANG") + "");
					}
				}
				final var handler = CommunityBoardHandler.getInstance().getHandler(cmd);
				if (handler != null)
				{
					handler.onBypassCommand(cmd, activeChar);
				}
				return;
			}
		}
		else if (command.startsWith("_bbsmod"))
		{
			final var st = new StringTokenizer(command, ":");
			st.nextToken();
			final var page = st.nextToken();
			
			final var htm = new NpcHtmlMessage(5);
			htm.setFile(activeChar, activeChar.getLang(), "data/html/mods/" + page + ".htm");
			activeChar.sendPacket(htm);
			return;
		}
		else if (command.startsWith("_bbsvoice"))
		{
			final var st = new StringTokenizer(command, ":");
			st.nextToken();
			final var voice_command = st.nextToken();
			
			final var use_command = VoicedCommandHandler.getInstance().getHandler(voice_command);
			if (use_command != null)
			{
				use_command.useVoicedCommand(voice_command, activeChar, "");
				return;
			}
			return;
		}
		
		if (!checkCondition(activeChar, new StringTokenizer(command, "_").nextToken(), false, false))
		{
			return;
		}
		
		if (command.equals("_bbshome"))
		{
			var html = HtmCache.getInstance().getHtm(activeChar, activeChar.getLang(), "data/html/community/index.htm");
			html = getPlayerInfo(html, activeChar);
			separateAndSend(html, activeChar);
		}
		else if (command.equals("_bbsabout"))
		{
			var html = HtmCache.getInstance().getHtm(activeChar, activeChar.getLang(), "data/html/community/about.htm");
			html = getServerInfo(html, activeChar);
			separateAndSend(html, activeChar);
		}
		else if (command.startsWith("_bbspage"))
		{
			final var st = new StringTokenizer(command, ":");
			st.nextToken();
			final var page = st.nextToken();

			final var html = HtmCache.getInstance().getHtm(activeChar, activeChar.getLang(), "data/html/community/" + page + ".htm");
			separateAndSend(html, activeChar);
		}
		else if (command.startsWith("_bbsSkillStatInfo"))
		{
			final var st = new StringTokenizer(command, "_");
			st.nextToken();
			
			if (st.countTokens() < 4)
			{
				return;
			}
			
			final var skillId = Integer.parseInt(st.nextToken());
			final var skillLevel = Integer.parseInt(st.nextToken());
			final var type = Integer.parseInt(st.nextToken());
			final var maxLevel = Integer.parseInt(st.nextToken());
			final var sk = SkillsParser.getInstance().getInfo(skillId, maxLevel < 1 ? skillLevel : SkillsParser.getInstance().getMaxLevel(skillId));
			if (sk != null)
			{
				final var html = new NpcHtmlMessage(5);
				html.setFile(activeChar, activeChar.getLang(), "data/html/mods/stats.htm");
				html.replace("%skillIcon%", SkillsParser.getInstance().getInfo(skillId, SkillsParser.getInstance().getMaxLevel(skillId)).getIcon());
				html.replace("%skillName%", SkillsParser.getInstance().getInfo(skillId, SkillsParser.getInstance().getMaxLevel(skillId)).getName(activeChar.getLang()));
				html.replace("%skillId%", skillId);
				html.replace("%skillLevel%", skillLevel);
				html.replace("%infoLevel%", maxLevel < 1 ? skillLevel : SkillsParser.getInstance().getMaxLevel(skillId));
				if (type == 1)
				{
					html.replace("%title%", "Adding");
				}
				else if (type == 2)
				{
					html.replace("%title%", "Increase");
				}
				else
				{
					html.replace("%title%", "Percent");
				}
				html.replace("%list%", getFunc(activeChar, sk, type));
				html.replace("%type%", type == 1 ? String.valueOf(2) : type == 2 ? String.valueOf(3) : String.valueOf(1));
				html.replace("%maxLevel%", maxLevel);
				html.replace("%switch%", maxLevel < 1 ? 1 : 0);
				activeChar.sendPacket(html);
			}
		}
		else if (command.startsWith("_bbsCustomSkillGroup"))
		{
			final var st = new StringTokenizer(command, " ");
			st.nextToken();
			
			if (st.countTokens() < 1)
			{
				return;
			}
			
			final var groupId = Integer.parseInt(st.nextToken());
			final var skills = SkillTreesParser.getInstance().getAvailableCustomSkills(activeChar, groupId);
			final var asl = new ExAcquirableSkillListByClass(AcquireSkillType.CUSTOM);
			if (skills == null || skills.isEmpty())
			{
				return;
			}
			
			int count = 0;
			for (final var s : skills)
			{
				final var sk = SkillsParser.getInstance().getInfo(s.getId(), s.getLvl());
				if (sk == null)
				{
					continue;
				}
				count++;
				asl.addSkill(s.getId(), s.getGetLevel(), s.getLvl(), s.getLvl(), s.getLevelUpSp(), 1);
			}
			
			if (count == 0)
			{
				final var allMap = SkillTreesParser.getInstance().getCustomSkills(groupId);
				if (allMap != null)
				{
					final int minlLevel = SkillTreesParser.getInstance().getMinLevelForNewSkill(activeChar, allMap);
					if (minlLevel > 0)
					{
						final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.DO_NOT_HAVE_FURTHER_SKILLS_TO_LEARN_S1);
						sm.addNumber(minlLevel);
						activeChar.sendPacket(sm);
					}
					else
					{
						activeChar.sendPacket(SystemMessageId.NO_MORE_SKILLS_TO_LEARN);
					}
					activeChar.sendPacket(AcquireSkillDone.STATIC);
				}
			}
			else
			{
				activeChar.setLearningGroupId(groupId);
				activeChar.sendPacket(asl);
			}
		}
		else if (command.startsWith("_bbspremiumPage"))
		{
			final var st = new StringTokenizer(command, " ");
			st.nextToken();
			String page = null;
			try
			{
				page = st.nextToken();
			}
			catch (final Exception e)
			{}
			
			if (activeChar.hasPremiumBonus() || Config.SERVICES_WITHOUT_PREMIUM)
			{
				var html = HtmCache.getInstance().getHtm(activeChar, activeChar.getLang(), "data/html/community/donate/premium.htm");
				final var tpl = PremiumAccountsParser.getInstance().getPremiumTemplate(activeChar.getPremiumBonus().getPremiumId());
				if (tpl != null)
				{
					var line = "";
					if (tpl.isOnlineType())
					{
						html = html.replace("%onlineType%", ServerStorage.getInstance().getString(activeChar.getLang(), "CommunityGeneral.PREMIUM_ONLINE"));
						final var lastTime = ((tpl.getTime() * 1000L - (activeChar.getPremiumBonus().getOnlineTime() + (System.currentTimeMillis() - activeChar.getPremiumOnlineTime()))) / 1000L);
						line = "<font color=\"E6D0AE\">" + TimeUtils.formatTime(activeChar, (int) lastTime, false) + "</font>";
					}
					else
					{
						html = html.replace("%onlineType%", ServerStorage.getInstance().getString(activeChar.getLang(), "CommunityGeneral.PREMIUM_COMMON"));
						final var lastTime = (activeChar.getPremiumBonus().getOnlineTime() - System.currentTimeMillis()) / 1000L;
						line = "<font color=\"E6D0AE\">" + TimeUtils.formatTime(activeChar, (int) lastTime, false) + "</font>";
					}
					
					html = html.replace("%timeLeft%", line);
					
					if (tpl.isPersonal())
					{
						html = html.replace("%isPersonal%", ServerStorage.getInstance().getString(activeChar.getLang(), "CommunityGeneral.RERSONAL"));
					}
					else
					{
						html = html.replace("%isPersonal%", ServerStorage.getInstance().getString(activeChar.getLang(), "CommunityGeneral.ACCOUNT"));
					}
				}
				else
				{
					html = html.replace("%onlineType%", "");
					html = html.replace("%isPersonal%", ServerStorage.getInstance().getString(activeChar.getLang(), "CommunityGeneral.NO_PREMIUM"));
					html = html.replace("%timeLeft%", "<font color=cb2821>...</font>");
				}
				separateAndSend(html, activeChar);
				return;
			}
			else
			{
				checkFullPremiumList(activeChar, page != null ? Integer.parseInt(page) : 1);
			}
		}
		else if (command.startsWith("_bbspremiumOnlyPage"))
		{
			final var st = new StringTokenizer(command, " ");
			st.nextToken();
			String page = null;
			try
			{
				page = st.nextToken();
			}
			catch (final Exception e)
			{}
			checkFullPremiumListOnly(activeChar, page != null ? Integer.parseInt(page) : 1);
		}
		else if (command.startsWith("_bbspremiumList"))
		{
			final var st = new StringTokenizer(command, " ");
			st.nextToken();
			final var page = Integer.parseInt(st.nextToken());
			checkPremiumList(activeChar, page);
		}
		else if (command.startsWith("_bbspremiumBuy"))
		{
			final var st = new StringTokenizer(command, " ");
			st.nextToken();
			final var premiumId = Integer.parseInt(st.nextToken());
			final var page = Integer.parseInt(st.nextToken());
			String type = null;
			try
			{
				type = st.nextToken();
			}
			catch (final Exception e)
			{}
			
			final var typeInfo = type != null ? Integer.parseInt(type) : 0;
			
			if (activeChar.hasPremiumBonus() && !Config.PREMIUMSERVICE_DOUBLE)
			{
				activeChar.sendMessage((new ServerMessage("ServiceBBS.PREMIUM_MSG", activeChar.getLang())).toString());
				return;
			}
			checkPremium(activeChar, premiumId, page, typeInfo);
		}
		else if (command.startsWith("_bbspremiumOnlyBuy"))
		{
			final var st = new StringTokenizer(command, " ");
			st.nextToken();
			String premiumId = null;
			try
			{
				premiumId = st.nextToken();
			}
			catch (final Exception e)
			{
			}
			
			if (!Config.USE_PREMIUMSERVICE)
			{
				activeChar.sendMessage((new ServerMessage("Community.ALL_DISABLE", activeChar.getLang())).toString());
				return;
			}

			if (activeChar.hasPremiumBonus() && !Config.PREMIUMSERVICE_DOUBLE)
			{
				activeChar.sendMessage((new ServerMessage("ServiceBBS.PREMIUM_MSG", activeChar.getLang())).toString());
				return;
			}

			final var template = PremiumAccountsParser.getInstance().getPremiumTemplate(Integer.parseInt(premiumId));
			if (template != null)
			{
				for (final var price : template.getPriceList())
				{
					if (price != null)
					{
						if (activeChar.getInventory().getItemByItemId(price.getId()) == null)
						{
							activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
							return;
						}
						
						if (activeChar.getInventory().getItemByItemId(price.getId()).getCount() < (price.getCount()))
						{
							activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
							return;
						}
					}
				}
				
				for (final var price : template.getPriceList())
				{
					if (price != null)
					{
						activeChar.destroyItemByItemId("PremiumBBS", price.getId(), price.getCount(), activeChar, true);
					}
				}
				
				Util.addServiceLog(activeChar.getName(null) + " buy premium account template id: " + template.getId());
				
				final var time = !template.isOnlineType() ? (System.currentTimeMillis() + (template.getTime() * 1000)) : 0;
				if (template.isPersonal())
				{
					CharacterPremiumDAO.getInstance().updatePersonal(activeChar, Integer.parseInt(premiumId), time, template.isOnlineType());
				}
				else
				{
					CharacterPremiumDAO.getInstance().update(activeChar, Integer.parseInt(premiumId), time, template.isOnlineType());
				}
				
				if (activeChar.isInParty())
				{
					activeChar.getParty().recalculatePartyData();
				}
			}
		}
		else if (command.startsWith("_bbspremium"))
		{
			final var st = new StringTokenizer(command, " ");
			st.nextToken();
			final var premiumId = Integer.parseInt(st.nextToken());
			final var page = Integer.parseInt(st.nextToken());
			String type = null;
			try
			{
				type = st.nextToken();
			}
			catch (final Exception e)
			{}
			
			final var typeInfo = type != null ? Integer.parseInt(type) : 0;
			
			final var tpl = PremiumAccountsParser.getInstance().getPremiumTemplate(premiumId);
			if (tpl != null)
			{
				final var html = new NpcHtmlMessage(5);
				if (typeInfo == 0)
				{
					html.setFile(activeChar, activeChar.getLang(), "data/html/community/donate/premiumInfo.htm");
				}
				else
				{
					html.setFile(activeChar, activeChar.getLang(), "data/html/community/donate/buyPremiumInfo.htm");
				}
				
				html.replace("%name%", tpl.getName(activeChar.getLang()));
				html.replace("%icon%", tpl.getIcon());
				html.replace("%time%", TimeUtils.formatTime(activeChar, (int) tpl.getTime()));
				var priceLine = "<font color=99CC66>Cost:</font> ";
				for (final var price : tpl.getPriceList())
				{
					if (price != null)
					{
						priceLine += "" + Util.formatPay(activeChar, price.getCount(), price.getId()) + " ";
					}
				}
				html.replace("%price%", priceLine);
				html.replace("%link%", "bypass -h _bbspremiumBuy " + tpl.getId() + " " + page + " " + typeInfo);
				if (typeInfo == 0)
				{
					html.replace("%back%", "bypass -h _bbspremiumList " + page);
				}
				else if (typeInfo == 1)
				{
					html.replace("%back%", "bypass -h _bbspremiumPage " + page);
				}
				else
				{
					html.replace("%back%", "bypass -h _bbspremiumOnlyPage " + page);
				}
				html.replace("%xp%", "+" + Math.abs((int) (100 - (tpl.getBonusType(BonusType.EXP, 1.) * 100))) + "%");
				html.replace("%sp%", "+" + Math.abs((int) (100 - (tpl.getBonusType(BonusType.SP, 1.) * 100))) + "%");
				html.replace("%adena%", "+" + Math.abs((int) (100 - (tpl.getBonusType(BonusType.DROP_ADENA, 1.) * 100))) + "%");
				html.replace("%items%", "+" + Math.abs((int) (100 - (tpl.getBonusType(BonusType.DROP_ITEMS, 1.) * 100))) + "%");
				html.replace("%raids%", "+" + Math.abs((int) (100 - (tpl.getBonusType(BonusType.DROP_RAID, 1.) * 100))) + "%");
				html.replace("%epics%", "+" + Math.abs((int) (100 - (tpl.getBonusType(BonusType.DROP_EPIC, 1.) * 100))) + "%");
				html.replace("%elementStones%", "+" + Math.abs((int) (100 - (tpl.getBonusType(BonusType.ELEMENT_STONE, 1.) * 100))) + "%");
				html.replace("%spoil%", "+" + Math.abs((int) (100 - (tpl.getBonusType(BonusType.SPOIL, 1.) * 100))) + "%");
				html.replace("%questReward%", "+" + Math.abs((int) (100 - (tpl.getBonusType(BonusType.QUEST_REWARD, 1.) * 100))) + "%");
				html.replace("%questDrop%", "+" + Math.abs((int) (100 - (tpl.getBonusType(BonusType.QUEST_DROP, 1.) * 100))) + "%");
				html.replace("%fishing%", "+" + Math.abs((int) (100 - (tpl.getBonusType(BonusType.FISHING, 1.) * 100))) + "%");
				html.replace("%epaulette%", "+" + Math.abs((int) (100 - (tpl.getBonusType(BonusType.SIEGE, 1.) * 100))) + "%");
				html.replace("%weight%", "+" + Math.abs((int) (100 - (tpl.getBonusType(BonusType.WEIGHT, 1.) * 100))) + "%");
				html.replace("%masterwork%", "+" + tpl.getBonusType(BonusType.MASTER_WORK_CHANCE, 0.) + "%");
				html.replace("%craft%", "+" + tpl.getBonusType(BonusType.CRAFT_CHANCE, 0.) + "%");
				html.replace("%enchant%", "+" + tpl.getBonusType(BonusType.ENCHANT_CHANCE, 0.) + "%");
				html.replace("%fame%", "+" + Math.abs((int) (100 - (tpl.getBonusType(BonusType.FAME, 1.) * 100))) + "%");
				html.replace("%reflection%", "" + Math.abs((int) (100 - (tpl.getBonusType(BonusType.REFLECTION_REDUCE, 1.) * 100))) + "%");
				html.replace("%events%", "" + Math.abs((int) (100 - (tpl.getBonusType(BonusType.EVENTS, 1.) * 100))) + "%");
				if (tpl.isOnlineType())
				{
					html.replace("%onlineType%", "<font color=LEVEL>Premium is only spent when you are online!</font>");
				}
				else
				{
					html.replace("%onlineType%", "&nbsp;");
				}
				
				if (tpl.isPersonal())
				{
					html.replace("%isPersonal%", "(<font color=\"b02e31\">Personal Type</font>)");
				}
				else
				{
					html.replace("%isPersonal%", "&nbsp;");
				}
				
				html.replace("%xp_f%", String.valueOf(Config.RATE_XP_BY_LVL[activeChar.getLevel()] * tpl.getBonusType(BonusType.EXP, 1.)));
				html.replace("%sp_f%", String.valueOf(Config.RATE_SP_BY_LVL[activeChar.getLevel()] * tpl.getBonusType(BonusType.SP, 1.)));
				html.replace("%adena_f%", String.valueOf(Config.RATE_DROP_ADENA * tpl.getBonusType(BonusType.DROP_ADENA, 1.)));
				html.replace("%items_f%", String.valueOf(Config.RATE_DROP_ITEMS * tpl.getBonusType(BonusType.DROP_ITEMS, 1.)));
				html.replace("%raids_f%", String.valueOf(Config.RATE_DROP_RAIDBOSS * tpl.getBonusType(BonusType.DROP_RAID, 1.)));
				html.replace("%epics_f%", String.valueOf(Config.RATE_DROP_EPICBOSS * tpl.getBonusType(BonusType.DROP_EPIC, 1.)));
				html.replace("%elementStones_f%", String.valueOf(Config.RATE_DROP_ITEMS * tpl.getBonusType(BonusType.DROP_ITEMS, 1.) * tpl.getBonusType(BonusType.ELEMENT_STONE, 1.)));
				html.replace("%spoil_f%", String.valueOf(Config.RATE_DROP_SPOIL * tpl.getBonusType(BonusType.SPOIL, 1.)));
				html.replace("%questReward_f%", String.valueOf(Config.RATE_QUEST_REWARD * tpl.getBonusType(BonusType.QUEST_REWARD, 1.)));
				html.replace("%questDrop_f%", String.valueOf(Config.RATE_QUEST_DROP * tpl.getBonusType(BonusType.QUEST_DROP, 1.)));
				html.replace("%fishing_f%", String.valueOf(Config.RATE_DROP_FISHING * tpl.getBonusType(BonusType.FISHING, 1.)));
				html.replace("%epaulette_f%", String.valueOf(Config.RATE_DROP_SIEGE_GUARD * tpl.getBonusType(BonusType.SIEGE, 1.)));
				html.replace("%weight_f%", String.valueOf(activeChar.getMaxLoad() * tpl.getBonusType(BonusType.WEIGHT, 1.)));
				
				activeChar.sendPacket(html);
			}
		}
		else if (command.startsWith("_bbslistclanskills"))
		{
			if ((activeChar.getClanPrivileges() & Clan.CP_ALL) != Clan.CP_ALL)
			{
				activeChar.sendMessage((new ServerMessage("ServiceBBS.CLAN_LEADER", activeChar.getLang())).toString());
				return;
			}
			final var st = new StringTokenizer(command, " ");
			st.nextToken();
			final var page = Integer.parseInt(st.nextToken());
			checkClanSkills(activeChar, page);
		}
		else if (command.startsWith("_bbslearnclanskills"))
		{
			final var st = new StringTokenizer(command, " ");
			st.nextToken();
			final var skillId = Integer.parseInt(st.nextToken());
			final var skillLvl = Integer.parseInt(st.nextToken());
			final var page = Integer.parseInt(st.nextToken());
			
			if (activeChar.getInventory().getItemByItemId(Config.SERVICES_CLANSKILLS_ITEM[0]) == null)
			{
				activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
				return;
			}
			if (activeChar.getInventory().getItemByItemId(Config.SERVICES_CLANSKILLS_ITEM[0]).getCount() < Config.SERVICES_CLANSKILLS_ITEM[1])
			{
				activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
				return;
			}
			
			if ((activeChar.getClanPrivileges() & Clan.CP_ALL) != Clan.CP_ALL)
			{
				activeChar.sendMessage((new ServerMessage("ServiceBBS.CLAN_LEADER", activeChar.getLang())).toString());
				return;
			}
			
			if (!SkillTreesParser.getInstance().checkClanSkill(activeChar, skillId, skillLvl))
			{
				return;
			}
			
			activeChar.destroyItemByItemId("ClanSkillsBBS", Config.SERVICES_CLANSKILLS_ITEM[0], Config.SERVICES_CLANSKILLS_ITEM[1], activeChar, true);
			activeChar.getClan().addNewSkill(Config.LEARN_CLAN_SKILLS_MAX_LEVEL ? SkillsParser.getInstance().getInfo(skillId, SkillsParser.getInstance().getMaxLevel(skillId)) : SkillsParser.getInstance().getInfo(skillId, skillLvl));
			Util.addServiceLog(activeChar.getName(null) + " buy clan skill " + SkillsParser.getInstance().getInfo(skillId, skillLvl).getName(null) + " " + skillLvl + " level!");
			checkClanSkills(activeChar, page);
		}
		else if (command.startsWith("_bbswarhouse"))
		{
			if (command.equals("_bbswarhouse:chardeposit"))
			{
				activeChar.sendActionFailed();
				activeChar.setActiveWarehouse(activeChar.getWarehouse());
				if (activeChar.getWarehouse().getSize() == activeChar.getWareHouseLimit())
				{
					activeChar.sendPacket(SystemMessageId.WAREHOUSE_FULL);
					return;
				}
				activeChar.setInventoryBlockingStatus(true);
				activeChar.sendPacket(new WareHouseDepositList(activeChar, WareHouseDepositList.PRIVATE));
			}
			else if (command.equals("_bbswarhouse:clandeposit"))
			{
				if (activeChar.isEnchanting())
				{
					return;
				}
				if (activeChar.getClan() == null)
				{
					activeChar.sendPacket(SystemMessageId.YOU_ARE_NOT_A_CLAN_MEMBER);
					return;
				}

				activeChar.sendActionFailed();
				activeChar.setActiveWarehouse(activeChar.getClan().getWarehouse());
				if (activeChar.getClan().getLevel() == 0)
				{
					activeChar.sendPacket(SystemMessageId.ONLY_LEVEL_1_CLAN_OR_HIGHER_CAN_USE_WAREHOUSE);
					return;
				}
				activeChar.setActiveWarehouse(activeChar.getClan().getWarehouse());
				activeChar.setInventoryBlockingStatus(true);
				activeChar.sendPacket(new WareHouseDepositList(activeChar, WareHouseDepositList.CLAN));
			}
			else if (command.equals("_bbswarhouse:charwithdraw"))
			{
				activeChar.sendActionFailed();
				activeChar.setActiveWarehouse(activeChar.getWarehouse());

				if (activeChar.getActiveWarehouse().getSize() == 0)
				{
					activeChar.sendPacket(SystemMessageId.NO_ITEM_DEPOSITED_IN_WH);
					return;
				}
				activeChar.sendPacket(new WareHouseWithdrawList(1, activeChar, WareHouseWithdrawList.PRIVATE));
			}
			else if (command.equals("_bbswarhouse:charprivate"))
			{
				activeChar.sendActionFailed();
				activeChar.setActiveWarehouse(activeChar.getPrivateInventory());
				
				if (activeChar.getPrivateInventory().getSize() == 0)
				{
					activeChar.sendPacket(SystemMessageId.NO_ITEM_DEPOSITED_IN_WH);
					return;
				}
				activeChar.sendPacket(new WareHouseWithdrawList(1, activeChar, WareHouseWithdrawList.PRIVATE_INVENTORY));
			}
			else if (command.equals("_bbswarhouse:clanwithdraw"))
			{
				if (activeChar.isEnchanting())
				{
					return;
				}
				if (activeChar.getClan() == null)
				{
					activeChar.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_THE_RIGHT_TO_USE_CLAN_WAREHOUSE);
					return;
				}
				if (activeChar.getClan().getLevel() == 0)
				{
					activeChar.sendPacket(SystemMessageId.ONLY_LEVEL_1_CLAN_OR_HIGHER_CAN_USE_WAREHOUSE);
					return;
				}
				activeChar.sendActionFailed();

				if ((activeChar.getClanPrivileges() & Clan.CP_CL_VIEW_WAREHOUSE) != Clan.CP_CL_VIEW_WAREHOUSE)
				{
					activeChar.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_THE_RIGHT_TO_USE_CLAN_WAREHOUSE);
					return;
				}

				activeChar.setActiveWarehouse(activeChar.getClan().getWarehouse());

				if (activeChar.getActiveWarehouse().getSize() == 0)
				{
					activeChar.sendPacket(SystemMessageId.NO_ITEM_DEPOSITED_IN_WH);
					return;
				}
				activeChar.sendPacket(new WareHouseWithdrawList(1, activeChar, WareHouseWithdrawList.CLAN));
			}
		}
		else if (command.startsWith("_bbsAugment"))
		{
			if (command.equals("_bbsAugment;add"))
			{
				activeChar.sendPacket(SystemMessageId.SELECT_THE_ITEM_TO_BE_AUGMENTED);
				activeChar.sendPacket(new ExShowVariationMakeWindow());
				activeChar.cancelActiveTrade();
			}
			else if (command.equals("_bbsAugment;remove"))
			{
				activeChar.sendPacket(SystemMessageId.SELECT_THE_ITEM_FROM_WHICH_YOU_WISH_TO_REMOVE_AUGMENTATION);
				activeChar.sendPacket(new ExShowVariationCancelWindow());
				activeChar.cancelActiveTrade();
			}
		}
		else if (command.startsWith("_bbsdraw"))
		{
			if (command.equals("_bbsdraw:add"))
			{
				activeChar.sendPacket(new HennaEquipList(activeChar));
			}
			else if (command.equals("_bbsdraw:remove"))
			{
				for (final var henna : activeChar.getHennaList())
				{
					if (henna != null)
					{
						activeChar.sendPacket(new HennaUnequipList(activeChar));
						break;
					}
				}
			}
		}
		else if (command.startsWith("_bbsunban"))
		{
			String key = null;
			final var st = new StringTokenizer(command, " ");
			st.nextToken();
			final String affect = st.nextToken();

			try
			{
				key = st.nextToken();
			}
			catch (final Exception e)
			{}

			if (key != null)
			{
				if (activeChar.getInventory().getItemByItemId(Config.SERVICES_UNBAN_ITEM[0]) == null)
				{
					activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
					return;
				}
				if (activeChar.getInventory().getItemByItemId(Config.SERVICES_UNBAN_ITEM[0]).getCount() < (Config.SERVICES_UNBAN_ITEM[1]))
				{
					activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
					return;
				}

				if (unbanChar(activeChar, affect, key))
				{
					activeChar.destroyItemByItemId("UnbanBBS", Config.SERVICES_UNBAN_ITEM[0], Config.SERVICES_UNBAN_ITEM[1], activeChar, true);
					Util.addServiceLog(activeChar.getName(null) + " buy unban service!");
				}
				else
				{
					final var msg = new ServerMessage("ServiceBBS.UNBAN_NOTFIND", activeChar.getLang());
					msg.add(affect);
					msg.add(key);
					activeChar.sendMessage(msg.toString());
				}
			}
			else
			{
				activeChar.sendMessage((new ServerMessage("ServiceBBS.UNBAN_EMPTY", activeChar.getLang())).toString());
			}
		}
		else if (command.startsWith("_bbsmultisell;"))
		{
			final var st = new StringTokenizer(command, ";");
			st.nextToken();
			onBypassCommand("_bbspage:" + st.nextToken(), activeChar);
			final var listId = Integer.parseInt(st.nextToken());
			if (Config.AVALIABLE_COMMUNITY_MULTISELLS.contains(listId))
			{
				MultiSellParser.getInstance().separateAndSend(listId, activeChar, null, false);
				return;
			}
			else
			{
				Util.handleIllegalPlayerAction(activeChar, "" + activeChar.getName(null) + " try to cheat with Community MultiSell!");
				return;
			}
		}
		else if (command.startsWith("_bbsmsell;"))
		{
			final var st = new StringTokenizer(command, ";");
			st.nextToken();
			// onBypassCommand(st.nextToken(), activeChar);
			final var listId = Integer.parseInt(st.nextToken());
			if (Config.AVALIABLE_COMMUNITY_MULTISELLS.contains(listId))
			{
				MultiSellParser.getInstance().separateAndSend(listId, activeChar, null, false);
				return;
			}
			else
			{
				Util.handleIllegalPlayerAction(activeChar, "" + activeChar.getName(null) + " try to cheat with Community MultiSell!");
				return;
			}
		}
		else if (command.startsWith("_bbsExcMultisell;"))
		{
			final var st = new StringTokenizer(command, ";");
			st.nextToken();
			onBypassCommand("_bbspage:" + st.nextToken(), activeChar);
			final var listId = Integer.parseInt(st.nextToken());
			if (Config.AVALIABLE_COMMUNITY_MULTISELLS.contains(listId))
			{
				MultiSellParser.getInstance().separateAndSend(listId, activeChar, null, true);
				return;
			}
			else
			{
				Util.handleIllegalPlayerAction(activeChar, "" + activeChar.getName(null) + " try to cheat with Community MultiSell!");
				return;
			}
		}
		else if (command.startsWith("_bbsExcMsell;"))
		{
			final var st = new StringTokenizer(command, ";");
			st.nextToken();
			// onBypassCommand(st.nextToken(), activeChar);
			final int listId = Integer.parseInt(st.nextToken());
			if (Config.AVALIABLE_COMMUNITY_MULTISELLS.contains(listId))
			{
				MultiSellParser.getInstance().separateAndSend(listId, activeChar, null, true);
				return;
			}
			else
			{
				Util.handleIllegalPlayerAction(activeChar, "" + activeChar.getName(null) + " try to cheat with Community MultiSell!");
				return;
			}
		}
		else if (command.startsWith("_bbsservice"))
		{
			if (command.startsWith("_bbsservice:selectMainClass"))
			{
				if (activeChar.isSubClassActive())
				{
					activeChar.sendMessage((new ServerMessage("ServiceBBS.CHANGE_CLASS_ACTIVESUB", activeChar.getLang())).toString());
					return;
				}
				
				final Collection<SubClass> allSubs = activeChar.getSubClasses().values();
				if (allSubs.isEmpty())
				{
					activeChar.sendMessage((new ServerMessage("ServiceBBS.CHANGE_CLASS_HAVENT_SUBS", activeChar.getLang())).toString());
					return;
				}
				
				var html = HtmCache.getInstance().getHtm(activeChar, activeChar.getLang(), "data/html/community/donate/changeMainClass.htm");
				final var template = HtmCache.getInstance().getHtm(activeChar, activeChar.getLang(), "data/html/community/donate/changeMainClass-template.htm");
				var block = "";
				var list = "";
				for (final var subClass : allSubs)
				{
					if (subClass != null)
					{
						block = template;
						block = block.replace("{bypass}", "_bbsservice:changeMainClass " + subClass.getClassIndex());
						block = block.replace("{name}", Util.className(activeChar, subClass.getClassId()));
						list += block;
					}
				}
				html = html.replace("{list}", list);
				separateAndSend(html, activeChar);
			}
			else if (command.startsWith("_bbsservice:changeMainClass"))
			{
				String subId = null;
				final var st = new StringTokenizer(command, " ");
				st.nextToken();
				try
				{
					subId = st.nextToken();
				}
				catch (final Exception e)
				{
				}
				
				final var points = Olympiad.getInstance().getNoblePoints(activeChar.getObjectId());
				final var fights = Olympiad.getInstance().getNobleFights(activeChar.getObjectId());
				if ((points != Config.ALT_OLY_START_POINTS || fights > 0) && !Config.CHANGE_MAIN_CLASS_WITHOUT_OLY_CHECK)
				{
					activeChar.sendMessage((new ServerMessage("ServiceBBS.CHANGE_CLASS_WRONG_POINTS", activeChar.getLang())).toString());
					return;
				}
				
				if (activeChar.getClassId().level() != 3)
				{
					activeChar.sendMessage((new ServerMessage("ServiceBBS.CHANGE_CLASS_WRONG_CLASS", activeChar.getLang())).toString());
					return;
				}
				
				if (activeChar.isHero())
				{
					activeChar.sendMessage((new ServerMessage("ServiceBBS.CHANGE_CLASS_HERO", activeChar.getLang())).toString());
					return;
				}
				
				if (subId != null)
				{
					if (activeChar.isSubClassActive())
					{
						activeChar.sendMessage((new ServerMessage("ServiceBBS.CHANGE_CLASS_ACTIVESUB", activeChar.getLang())).toString());
						return;
					}
					
					final Collection<SubClass> allSubs = activeChar.getSubClasses().values();
					if (allSubs.isEmpty())
					{
						activeChar.sendMessage((new ServerMessage("ServiceBBS.CHANGE_CLASS_HAVENT_SUBS", activeChar.getLang())).toString());
						return;
					}
					
					final var subClass = activeChar.getSubClasses().get(Integer.parseInt(subId));
					if (subClass == null)
					{
						return;
					}
					
					if (activeChar.getInventory().getItemByItemId(Config.SERVICES_CHANGE_MAIN_CLASS[0]).getCount() < Config.SERVICES_CHANGE_MAIN_CLASS[1])
					{
						activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
						return;
					}
					
					final var msg = new ServerMessage("ServiceBBS.CHANGE_CLASS_TRY_INVITE", activeChar.getLang());
					msg.add(Util.className(activeChar, subClass.getClassId()));
					activeChar.sendConfirmDlg(new ChangeMainClassListener(activeChar, Integer.parseInt(subId)), 15000, msg.toString());
				}
			}
			else if (command.startsWith("_bbsservice:reloadRef"))
			{
				String id = null;
				String itemId = null;
				String itemAmount = null;
				String forPremium = null;
				final var st = new StringTokenizer(command, " ");
				st.nextToken();
				
				try
				{
					id = st.nextToken();
				}
				catch (final Exception e)
				{}
				
				try
				{
					itemId = st.nextToken();
				}
				catch (final Exception e)
				{}
				
				try
				{
					itemAmount = st.nextToken();
				}
				catch (final Exception e)
				{}
				
				try
				{
					forPremium = st.nextToken();
				}
				catch (final Exception e)
				{}
				
				if (id != null && itemId != null && itemAmount != null && forPremium != null)
				{
					final var isForPremium = Integer.parseInt(forPremium) == 1;
					if (isForPremium && !activeChar.hasPremiumBonus())
					{
						activeChar.sendMessage((new ServerMessage("ServiceBBS.ONLY_FOR_PREMIUM", activeChar.getLang())).toString());
						return;
					}
					
					if (activeChar.isInKrateisCube() || activeChar.getUCState() > 0 || activeChar.checkInTournament() || activeChar.isInFightEvent() || ((AerialCleftEvent.getInstance().isStarted() || AerialCleftEvent.getInstance().isRewarding()) && AerialCleftEvent.getInstance().isPlayerParticipant(activeChar.getObjectId())) || activeChar.getReflectionId() != 0)
					{
						activeChar.sendMessage((new ServerMessage("Community.ALL_DISABLE", activeChar.getLang())).toString());
						return;
					}
					
					if (System.currentTimeMillis() > ReflectionManager.getInstance().getReflectionTime(activeChar, Integer.parseInt(id)))
					{
						activeChar.sendMessage((new ServerMessage("ServiceBBS.REF_AVAILIABLE", activeChar.getLang())).toString());
						return;
					}
					
					if (Integer.parseInt(itemId) > 0)
					{
						if (activeChar.getInventory().getItemByItemId(Integer.parseInt(itemId)) == null)
						{
							activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
							return;
						}
						if (activeChar.getInventory().getItemByItemId(Integer.parseInt(itemId)).getCount() < Long.parseLong(itemAmount))
						{
							activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
							return;
						}
						activeChar.destroyItemByItemId("reloadReflection", Integer.parseInt(itemId), Long.parseLong(itemAmount), activeChar, true);
						Util.addServiceLog(activeChar.getName(null) + " buy refresh reflectionId " + Integer.parseInt(id));
					}
					ReflectionManager.getInstance().deleteReflectionTime(activeChar, Integer.parseInt(id));
				}
			}
			else if (command.equals("_bbsservice:sell"))
			{
				final var list = BuyListParser.getInstance().getBuyList(1);
				if (list != null)
				{
					activeChar.sendPacket(new BuyList(list, activeChar.getAdena(), 0));
					activeChar.sendPacket(new ExBuySellList(activeChar, false));
				}
			}
			else if (command.equals("_bbsservice:expandInventory"))
			{
				if (activeChar.getInventory().getItemByItemId(Config.SERVICES_EXPAND_INVENTORY[0]) == null)
				{
					activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
					return;
				}
				if (activeChar.getInventory().getItemByItemId(Config.SERVICES_EXPAND_INVENTORY[0]).getCount() < Config.SERVICES_EXPAND_INVENTORY[1])
				{
					activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
					return;
				}
				final var nextSlots = activeChar.getVarInt("expandInventory", 0) + Config.EXPAND_INVENTORY_STEP;
				if (nextSlots > Config.SERVICES_EXPAND_INVENTORY_LIMIT)
				{
					activeChar.sendMessage((new ServerMessage("ServiceBBS.LIMIT_EXCEEDED", activeChar.getLang())).toString());
					return;
				}
				activeChar.destroyItemByItemId("ExpandInventory", Config.SERVICES_EXPAND_INVENTORY[0], Config.SERVICES_EXPAND_INVENTORY[1], activeChar, true);
				Util.addServiceLog(activeChar.getName(null) + " buy expand Inventory!");
				activeChar.setVar("expandInventory", nextSlots);
				activeChar.sendPacket(new ExStorageMaxCount(activeChar));
				final ServerMessage msg = new ServerMessage("ServiceBBS.EXPAND_INV_INCREASE", activeChar.getLang());
				msg.add(Config.EXPAND_INVENTORY_STEP);
				activeChar.sendMessage(msg.toString());
			}
			else if (command.equals("_bbsservice:expandWareHouse"))
			{
				if (activeChar.getInventory().getItemByItemId(Config.SERVICES_EXPAND_WAREHOUSE[0]) == null)
				{
					activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
					return;
				}
				if (activeChar.getInventory().getItemByItemId(Config.SERVICES_EXPAND_WAREHOUSE[0]).getCount() < Config.SERVICES_EXPAND_WAREHOUSE[1])
				{
					activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
					return;
				}
				final var nextSlots = activeChar.getVarInt("expandWareHouse", 0) + Config.EXPAND_WAREHOUSE_STEP;
				if (nextSlots > Config.SERVICES_EXPAND_WAREHOUSE_LIMIT)
				{
					activeChar.sendMessage((new ServerMessage("ServiceBBS.LIMIT_EXCEEDED", activeChar.getLang())).toString());
					return;
				}
				activeChar.destroyItemByItemId("ExpandWareHouse", Config.SERVICES_EXPAND_WAREHOUSE[0], Config.SERVICES_EXPAND_WAREHOUSE[1], activeChar, true);
				Util.addServiceLog(activeChar.getName(null) + " buy expand WareHouse!");
				activeChar.setVar("expandWareHouse", nextSlots);
				activeChar.sendPacket(new ExStorageMaxCount(activeChar));
				final ServerMessage msg = new ServerMessage("ServiceBBS.EXPAND_WH_INCREASE", activeChar.getLang());
				msg.add(Config.EXPAND_WAREHOUSE_STEP);
				activeChar.sendMessage(msg.toString());
			}
			else if (command.equals("_bbsservice:expandSellStore"))
			{
				if (activeChar.getInventory().getItemByItemId(Config.SERVICES_EXPAND_SELLSTORE[0]) == null)
				{
					activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
					return;
				}
				if (activeChar.getInventory().getItemByItemId(Config.SERVICES_EXPAND_SELLSTORE[0]).getCount() < Config.SERVICES_EXPAND_SELLSTORE[1])
				{
					activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
					return;
				}
				final var nextSlots = activeChar.getVarInt("expandSellStore", 0) + Config.EXPAND_SELLSTORE_STEP;
				if (nextSlots > Config.SERVICES_EXPAND_SELLSTORE_LIMIT)
				{
					activeChar.sendMessage((new ServerMessage("ServiceBBS.LIMIT_EXCEEDED", activeChar.getLang())).toString());
					return;
				}
				activeChar.destroyItemByItemId("ExpandSellStore", Config.SERVICES_EXPAND_SELLSTORE[0], Config.SERVICES_EXPAND_SELLSTORE[1], activeChar, true);
				Util.addServiceLog(activeChar.getName(null) + " buy expand Sell Store!");
				activeChar.setVar("expandSellStore", nextSlots);
				activeChar.sendPacket(new ExStorageMaxCount(activeChar));
				final ServerMessage msg = new ServerMessage("ServiceBBS.EXPAND_SELLSTORE_INCREASE", activeChar.getLang());
				msg.add(Config.EXPAND_SELLSTORE_STEP);
				activeChar.sendMessage(msg.toString());
			}
			else if (command.equals("_bbsservice:expandBuyStore"))
			{
				if (activeChar.getInventory().getItemByItemId(Config.SERVICES_EXPAND_BUYSTORE[0]) == null)
				{
					activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
					return;
				}
				if (activeChar.getInventory().getItemByItemId(Config.SERVICES_EXPAND_BUYSTORE[0]).getCount() < Config.SERVICES_EXPAND_BUYSTORE[1])
				{
					activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
					return;
				}
				final var nextSlots = activeChar.getVarInt("expandBuyStore", 0) + Config.EXPAND_BUYSTORE_STEP;
				if (nextSlots > Config.SERVICES_EXPAND_BUYSTORE_LIMIT)
				{
					activeChar.sendMessage((new ServerMessage("ServiceBBS.LIMIT_EXCEEDED", activeChar.getLang())).toString());
					return;
				}
				activeChar.destroyItemByItemId("ExpandBuyStore", Config.SERVICES_EXPAND_BUYSTORE[0], Config.SERVICES_EXPAND_BUYSTORE[1], activeChar, true);
				Util.addServiceLog(activeChar.getName(null) + " buy expand Buy Store!");
				activeChar.setVar("expandBuyStore", nextSlots);
				activeChar.sendPacket(new ExStorageMaxCount(activeChar));
				final ServerMessage msg = new ServerMessage("ServiceBBS.EXPAND_BUYSTORE_INCREASE", activeChar.getLang());
				msg.add(Config.EXPAND_BUYSTORE_STEP);
				activeChar.sendMessage(msg.toString());
			}
			else if (command.equals("_bbsservice:expandDwarfRecipe"))
			{
				if (activeChar.getInventory().getItemByItemId(Config.SERVICES_EXPAND_DWARFRECIPE[0]) == null)
				{
					activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
					return;
				}
				if (activeChar.getInventory().getItemByItemId(Config.SERVICES_EXPAND_DWARFRECIPE[0]).getCount() < Config.SERVICES_EXPAND_DWARFRECIPE[1])
				{
					activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
					return;
				}
				if (!activeChar.hasDwarvenCraft())
				{
					return;
				}
				final var nextSlots = activeChar.getVarInt("expandDwarfRecipe", 0) + Config.EXPAND_DWARFRECIPE_STEP;
				if (nextSlots > Config.SERVICES_EXPAND_DWARFRECIPE_LIMIT)
				{
					activeChar.sendMessage((new ServerMessage("ServiceBBS.LIMIT_EXCEEDED", activeChar.getLang())).toString());
					return;
				}
				activeChar.destroyItemByItemId("ExpandDwarfRecipe", Config.SERVICES_EXPAND_DWARFRECIPE[0], Config.SERVICES_EXPAND_DWARFRECIPE[1], activeChar, true);
				Util.addServiceLog(activeChar.getName(null) + " buy expand Dwarf Recipe!");
				activeChar.setVar("expandDwarfRecipe", nextSlots);
				activeChar.sendPacket(new ExStorageMaxCount(activeChar));
				final ServerMessage msg = new ServerMessage("ServiceBBS.EXPAND_DWRECIPE_INCREASE", activeChar.getLang());
				msg.add(Config.EXPAND_DWARFRECIPE_STEP);
				activeChar.sendMessage(msg.toString());
			}
			else if (command.equals("_bbsservice:expandCommonRecipe"))
			{
				if (activeChar.getInventory().getItemByItemId(Config.SERVICES_EXPAND_COMMONRECIPE[0]) == null)
				{
					activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
					return;
				}
				if (activeChar.getInventory().getItemByItemId(Config.SERVICES_EXPAND_COMMONRECIPE[0]).getCount() < Config.SERVICES_EXPAND_COMMONRECIPE[1])
				{
					activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
					return;
				}
				final var nextSlots = activeChar.getVarInt("expandCommonRecipe", 0) + Config.EXPAND_COMMONRECIPE_STEP;
				if (nextSlots > Config.SERVICES_EXPAND_COMMONRECIPE_LIMIT)
				{
					activeChar.sendMessage((new ServerMessage("ServiceBBS.LIMIT_EXCEEDED", activeChar.getLang())).toString());
					return;
				}
				activeChar.destroyItemByItemId("ExpandCommonRecipe", Config.SERVICES_EXPAND_COMMONRECIPE[0], Config.SERVICES_EXPAND_COMMONRECIPE[1], activeChar, true);
				Util.addServiceLog(activeChar.getName(null) + " buy expand Common Recipe!");
				activeChar.setVar("expandCommonRecipe", nextSlots);
				activeChar.sendPacket(new ExStorageMaxCount(activeChar));
				final ServerMessage msg = new ServerMessage("ServiceBBS.EXPAND_COMRECIPE_INCREASE", activeChar.getLang());
				msg.add(Config.EXPAND_COMMONRECIPE_STEP);
				activeChar.sendMessage(msg.toString());
			}
			else if (command.equals("_bbsservice:subclass"))
			{
				if (activeChar.getInventory().getItemByItemId(Config.SERVICES_GIVESUBCLASS_ITEM[0]) == null)
				{
					activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
					return;
				}
				if (activeChar.getInventory().getItemByItemId(Config.SERVICES_GIVESUBCLASS_ITEM[0]).getCount() < Config.SERVICES_GIVESUBCLASS_ITEM[1])
				{
					activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
					return;
				}
				
				final var isActiveSub = activeChar.getVarInt("subclassBuy", 0);
				if (isActiveSub > 0)
				{
					activeChar.sendMessage((new ServerMessage("ServiceBBS.SUBCLASS_MSG_1", activeChar.getLang())).toString());
					return;
				}
				activeChar.destroyItemByItemId("SubclassBBS", Config.SERVICES_GIVESUBCLASS_ITEM[0], Config.SERVICES_GIVESUBCLASS_ITEM[1], activeChar, true);
				Util.addServiceLog(activeChar.getName(null) + " buy SubClass!");
				activeChar.setVar("subclassBuy", 1);
			}
			else if (command.equals("_bbsservice:noobles"))
			{
				if (activeChar.isNoble())
				{
					activeChar.sendMessage((new ServerMessage("ServiceBBS.NOOBLES_MSG_1", activeChar.getLang())).toString());
					return;
				}
				
				if (activeChar.getClassId().level() != 3)
				{
					activeChar.sendMessage((new ServerMessage("Community.ALL_DISABLE", activeChar.getLang())).toString());
				}
				final var msg = new ServerMessage("ServiceBBS.WANT_BUY_NOOBLESS", activeChar.getLang());
				msg.add(Util.formatPay(activeChar, Config.SERVICES_GIVENOOBLESS_ITEM[1], Config.SERVICES_GIVENOOBLESS_ITEM[0]));
				activeChar.sendConfirmDlg(new NooblesAnswerListener(activeChar), 60000, msg.toString());
			}
			else if (command.equals("_bbsservice:gender"))
			{
				if (activeChar.getRace().ordinal() == 5)
				{
					activeChar.sendMessage((new ServerMessage("ServiceBBS.GENDER_MSG", activeChar.getLang())).toString());
					return;
				}
				if (activeChar.getInventory().getItemByItemId(Config.SERVICES_CHANGEGENDER_ITEM[0]) == null)
				{
					activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
					return;
				}
				if (activeChar.getInventory().getItemByItemId(Config.SERVICES_CHANGEGENDER_ITEM[0]).getCount() < Config.SERVICES_CHANGEGENDER_ITEM[1])
				{
					activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
					return;
				}
				activeChar.destroyItemByItemId("ChangeGenderBBS", Config.SERVICES_CHANGEGENDER_ITEM[0], Config.SERVICES_CHANGEGENDER_ITEM[1], activeChar, true);
				Util.addServiceLog(activeChar.getName(null) + " buy change gender!");
				activeChar.getAppearance().setSex(activeChar.getAppearance().getSex() ? false : true);
				activeChar.broadcastUserInfo(true);
				activeChar.decayMe();
				activeChar.spawnMe(activeChar.getX(), activeChar.getY(), activeChar.getZ());
			}
			else if (command.equals("_bbsservice:level"))
			{
				if ((!Config.SERVICES_LEVELUP_ENABLE) && (!Config.SERVICES_DELEVEL_ENABLE))
				{
					activeChar.sendMessage((new ServerMessage("ServiceBBS.DISABLE", activeChar.getLang())).toString());
					return;
				}
				
				var html = HtmCache.getInstance().getHtm(activeChar, activeChar.getLang(), "data/html/community/levelcalc/index.htm");
				if (!Config.SERVICES_LEVELUP_ENABLE)
				{
					var up = HtmCache.getInstance().getHtm(activeChar, activeChar.getLang(), "data/html/community/levelcalc/up_off.htm");
					up = up.replace("{cost}", "<font color=\"CC3333\">" + ServerStorage.getInstance().getString(activeChar.getLang(), "ServiceBBS.DISABLE") + "</font>");
					html = html.replace("%up%", up);
				}
				else
				{
					var up = HtmCache.getInstance().getHtm(activeChar, activeChar.getLang(), "data/html/community/levelcalc/up.htm");
					up = up.replace("{cost}", Util.formatPay(activeChar, Config.SERVICES_LEVELUP_ITEM[1], Config.SERVICES_LEVELUP_ITEM[0]));
					html = html.replace("%up%", up);
				}
				if (!Config.SERVICES_DELEVEL_ENABLE)
				{
					var lower = HtmCache.getInstance().getHtm(activeChar, activeChar.getLang(), "data/html/community/levelcalc/lower_off.htm");
					lower = lower.replace("{cost}", "<font color=\"CC3333\">" + ServerStorage.getInstance().getString(activeChar.getLang(), "ServiceBBS.DISABLE") + "</font>");
					html = html.replace("%lower%", lower);
				}
				else
				{
					var lower = HtmCache.getInstance().getHtm(activeChar, activeChar.getLang(), "data/html/community/levelcalc/lower.htm");
					lower = lower.replace("{cost}", Util.formatPay(activeChar, Config.SERVICES_DELEVEL_ITEM[1], Config.SERVICES_DELEVEL_ITEM[0]));
					html = html.replace("%lower%", lower);
				}
				Util.setHtml(html, activeChar);
			}
			else if (command.startsWith("_bbsservice:levelcalc"))
			{
				final var st = new StringTokenizer(command, " ");
				st.nextToken();
				String lvl = null;
				try
				{
					lvl = st.nextToken();
				}
				catch (final Exception e)
				{}
				
				if (lvl != null)
				{
					final var level = Util.isNumber(lvl) ? Integer.parseInt(lvl) : activeChar.getLevel();
					if (level == activeChar.getLevel())
					{
						activeChar.sendMessage((new ServerMessage("ServiceBBS.LVL_EQUALS", activeChar.getLang())).toString());
						return;
					}
					
					final var delevel = level < activeChar.getLevel();
					if (delevel && !Config.SERVICES_DELEVEL_ENABLE)
					{
						activeChar.sendMessage((new ServerMessage("ServiceBBS.CANT_DELEVEL", activeChar.getLang())).toString());
						return;
					}
					
					if (!delevel && !Config.SERVICES_LEVELUP_ENABLE)
					{
						activeChar.sendMessage((new ServerMessage("ServiceBBS.CANT_LVLUP", activeChar.getLang())).toString());
						return;
					}
					
					final var item = level < activeChar.getLevel() ? Config.SERVICES_DELEVEL_ITEM[0] : Config.SERVICES_LEVELUP_ITEM[0];
					final var count = Config.LVLUP_SERVICE_STATIC_PRICE ? Config.SERVICES_LEVELUP_ITEM[1] : level < activeChar.getLevel() ? (activeChar.getLevel() - level) * Config.SERVICES_DELEVEL_ITEM[1] : (level - activeChar.getLevel()) * Config.SERVICES_LEVELUP_ITEM[1];
					final var msg = new ServerMessage("ServiceBBS.WANT_CHANGE_LVL", activeChar.getLang());
					msg.add(activeChar.getLevel());
					msg.add(level);
					msg.add(Util.formatPay(activeChar, count, item));
					activeChar.sendConfirmDlg(new LevelAnswerListener(activeChar, level), 60000, msg.toString());
				}
			}
			else if (command.equals("_bbsservice:setHero"))
			{
				if (activeChar.isHero())
				{
					return;
				}
				final var msg = new ServerMessage("ServiceBBS.WANT_BUY_HERO", activeChar.getLang());
				msg.add(Util.formatPay(activeChar, Config.SERVICES_GIVEHERO_ITEM[1], Config.SERVICES_GIVEHERO_ITEM[0]));
				activeChar.sendConfirmDlg(new HeroAnswerListener(activeChar), 60000, msg.toString());
			}
			else if (command.equals("_bbsservice:recoveryPK"))
			{
				if (activeChar.getPkKills() <= 0)
				{
					activeChar.sendMessage((new ServerMessage("ServiceBBS.PK_MSG", activeChar.getLang())).toString());
					return;
				}
				if (activeChar.getInventory().getItemByItemId(Config.SERVICES_RECOVERYPK_ITEM[0]) == null)
				{
					activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
					return;
				}
				if (activeChar.getInventory().getItemByItemId(Config.SERVICES_RECOVERYPK_ITEM[0]).getCount() < Config.SERVICES_RECOVERYPK_ITEM[1])
				{
					activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
					return;
				}
				final var msg = new ServerMessage("ServiceBBS.WANT_BUY_RECOVERY_PK", activeChar.getLang());
				msg.add(Util.formatPay(activeChar, Config.SERVICES_RECOVERYPK_ITEM[1], Config.SERVICES_RECOVERYPK_ITEM[0]));
				activeChar.sendConfirmDlg(new RecoveryPkAnswerListener(activeChar), 60000, msg.toString());
			}
			else if (command.equals("_bbsservice:recoveryKarma"))
			{
				if (activeChar.getKarma() <= 0)
				{
					activeChar.sendMessage((new ServerMessage("ServiceBBS.KARMA_MSG", activeChar.getLang())).toString());
					return;
				}
				final var msg = new ServerMessage("ServiceBBS.WANT_BUY_RECOVERY_KARMA", activeChar.getLang());
				msg.add(Util.formatPay(activeChar, Config.SERVICES_RECOVERYKARMA_ITEM[1], Config.SERVICES_RECOVERYKARMA_ITEM[0]));
				activeChar.sendConfirmDlg(new RecoveryKarmaAnswerListener(activeChar), 60000, msg.toString());
			}
			else if (command.equals("_bbsservice:recoveryVitality"))
			{
				final var msg = new ServerMessage("ServiceBBS.WANT_BUY_RECOVERY_VITALITY", activeChar.getLang());
				msg.add(Util.formatPay(activeChar, Config.SERVICES_RECOVERYVITALITY_ITEM[1], Config.SERVICES_RECOVERYVITALITY_ITEM[0]));
				activeChar.sendConfirmDlg(new RecoveryVitalityAnswerListener(activeChar), 60000, msg.toString());
			}
			else if (command.equals("_bbsservice:addSP"))
			{
				if (activeChar.getInventory().getItemByItemId(Config.SERVICES_GIVESP_ITEM[0]) == null)
				{
					activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
					return;
				}
				if (activeChar.getInventory().getItemByItemId(Config.SERVICES_GIVESP_ITEM[0]).getCount() < Config.SERVICES_GIVESP_ITEM[1])
				{
					activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
					return;
				}
				activeChar.destroyItemByItemId("AddSpBBS", Config.SERVICES_GIVESP_ITEM[0], Config.SERVICES_GIVESP_ITEM[1], activeChar, true);
				Util.addServiceLog(activeChar.getName(null) + " buy add SP service!");
				activeChar.setSp(activeChar.getSp() + 10000000);
				activeChar.sendMessage((new ServerMessage("ServiceBBS.ADDSP_MSG", activeChar.getLang())).toString());
				activeChar.broadcastPacket(new SocialAction(activeChar.getObjectId(), 16));
				activeChar.broadcastCharInfo();
			}
			else if (command.equals("_bbsservice:clanlvlup"))
			{
				if (activeChar.getClan() != null)
				{
					if (!ClanParser.getInstance().hasClanLevel(activeChar.getClan().getLevel() + 1))
					{
						activeChar.sendMessage(new ServerMessage("ServiceBBS.MAXLVL", activeChar.getLang()).toString());
						return;
					}
					
					if ((activeChar.getClanPrivileges() & Clan.CP_ALL) != Clan.CP_ALL)
					{
						activeChar.sendMessage((new ServerMessage("ServiceBBS.CLAN_LEADER", activeChar.getLang())).toString());
						return;
					}
					final var msg = new ServerMessage("ServiceBBS.WANT_BUY_CLAN_LEVEL", activeChar.getLang());
					msg.add(Util.formatPay(activeChar, Config.SERVICES_CLANLVL_ITEM[1], Config.SERVICES_CLANLVL_ITEM[0]));
					activeChar.sendConfirmDlg(new ClanLevelAnswerListener(activeChar), 60000, msg.toString());
				}
				else
				{
					activeChar.sendMessage(new ServerMessage("ServiceBBS.NEED_CREATE", activeChar.getLang()).toString());
				}
			}
			else if (command.equals("_bbsservice:clanCreatePenalty"))
			{
				if (activeChar.getInventory().getItemByItemId(Config.SERVICES_CLAN_CREATE_PENALTY_ITEM[0]) == null)
				{
					activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
					return;
				}
				if (activeChar.getInventory().getItemByItemId(Config.SERVICES_CLAN_CREATE_PENALTY_ITEM[0]).getCount() < Config.SERVICES_CLAN_CREATE_PENALTY_ITEM[1])
				{
					activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
					return;
				}
				
				if (activeChar.getClanCreateExpiryTime() <= 0)
				{
					activeChar.sendMessage((new ServerMessage("CommunityGeneral.HAVE_NO_CREATE_PENALTY", activeChar.getLang())).toString());
					return;
				}
				activeChar.destroyItemByItemId("CreatePenaltyBBS", Config.SERVICES_CLAN_CREATE_PENALTY_ITEM[0], Config.SERVICES_CLAN_CREATE_PENALTY_ITEM[1], activeChar, true);
				Util.addServiceLog(activeChar.getName(null) + " buy refresh clan create penalty service!");
				activeChar.setClanCreateExpiryTime(0);
				activeChar.sendMessage((new ServerMessage("CommunityGeneral.CREATE_PENALTY_REMOVED", activeChar.getLang())).toString());
			}
			else if (command.equals("_bbsservice:clanJoinPenalty"))
			{
				if (activeChar.getInventory().getItemByItemId(Config.SERVICES_CLAN_JOIN_PENALTY_ITEM[0]) == null)
				{
					activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
					return;
				}
				if (activeChar.getInventory().getItemByItemId(Config.SERVICES_CLAN_JOIN_PENALTY_ITEM[0]).getCount() < Config.SERVICES_CLAN_JOIN_PENALTY_ITEM[1])
				{
					activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
					return;
				}
				
				if (activeChar.getClan() == null && activeChar.getClanJoinExpiryTime() <= 0)
				{
					activeChar.sendMessage((new ServerMessage("CommunityGeneral.HAVE_NO_JOIN_PENALTY", activeChar.getLang())).toString());
					return;
				}
				
				if (activeChar.getClan() != null && activeChar.getClan().getCharPenaltyExpiryTime() <= 0)
				{
					activeChar.sendMessage((new ServerMessage("CommunityGeneral.HAVE_NO_JOIN_PENALTY", activeChar.getLang())).toString());
					return;
				}
				activeChar.destroyItemByItemId("JoinPenaltyBBS", Config.SERVICES_CLAN_JOIN_PENALTY_ITEM[0], Config.SERVICES_CLAN_JOIN_PENALTY_ITEM[1], activeChar, true);
				Util.addServiceLog(activeChar.getName(null) + " buy refresh clan join penalty service!");
				if (activeChar.getClan() == null)
				{
					activeChar.setClanJoinExpiryTime(0);
				}
				else
				{
					activeChar.getClan().setCharPenaltyExpiryTime(0);
				}
				activeChar.sendMessage((new ServerMessage("CommunityGeneral.CREATE_JOIN_REMOVE", activeChar.getLang())).toString());
			}
			else if (command.equals("_bbsservice:giverec"))
			{
				if (activeChar.getInventory().getItemByItemId(Config.SERVICES_GIVEREC_ITEM[0]) == null)
				{
					activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
					return;
				}
				if (activeChar.getInventory().getItemByItemId(Config.SERVICES_GIVEREC_ITEM[0]).getCount() < Config.SERVICES_GIVEREC_ITEM[1])
				{
					activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
					return;
				}
				if (activeChar.getRecommendation().getRecomHave() == 255)
				{
					activeChar.sendMessage((new ServerMessage("ServiceBBS.MAX_REC", activeChar.getLang())).toString());
					return;
				}
				activeChar.destroyItemByItemId("GiveRecBBS", Config.SERVICES_GIVEREC_ITEM[0], Config.SERVICES_GIVEREC_ITEM[1], activeChar, true);
				Util.addServiceLog(activeChar.getName(null) + " buy recommendations service!");
				final int recCanGive = 255 - activeChar.getRecommendation().getRecomHave();
				activeChar.getRecommendation().setRecomHave(activeChar.getRecommendation().getRecomHave() + recCanGive);
				final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.YOU_OBTAINED_S1_RECOMMENDATIONS);
				sm.addNumber(recCanGive);
				activeChar.sendPacket(sm);
				activeChar.sendUserInfo();
				activeChar.sendVoteSystemInfo();
			}
			else if (command.equals("_bbsservice:givereputation"))
			{
				if (activeChar.getClan() != null)
				{
					final var msg = new ServerMessage("ServiceBBS.WANT_BUY_CLAN_REPUTATION", activeChar.getLang());
					msg.add(Config.SERVICES_REP_COUNT);
					msg.add(Util.formatPay(activeChar, Config.SERVICES_GIVEREP_ITEM[1], Config.SERVICES_GIVEREP_ITEM[0]));
					activeChar.sendConfirmDlg(new ReputationAnswerListener(activeChar), 60000, msg.toString());
				}
				else
				{
					activeChar.sendMessage((new ServerMessage("ServiceBBS.NEED_CREATE", activeChar.getLang())).toString());
				}
			}
			else if (command.equals("_bbsservice:givefame"))
			{
				if (activeChar.getInventory().getItemByItemId(Config.SERVICES_GIVEFAME_ITEM[0]) == null)
				{
					activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
					return;
				}
				if (activeChar.getInventory().getItemByItemId(Config.SERVICES_GIVEFAME_ITEM[0]).getCount() < Config.SERVICES_GIVEFAME_ITEM[1])
				{
					activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
					return;
				}
				activeChar.destroyItemByItemId("GiveFameBBS", Config.SERVICES_GIVEFAME_ITEM[0], Config.SERVICES_GIVEFAME_ITEM[1], activeChar, true);
				Util.addServiceLog(activeChar.getName(null) + " buy fame service!");
				activeChar.setFame(activeChar.getFame() + Config.SERVICES_FAME_COUNT);
				final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.ACQUIRED_S1_REPUTATION_SCORE);
				sm.addNumber(Config.SERVICES_FAME_COUNT);
				activeChar.sendPacket(sm);
				activeChar.sendUserInfo();
			}
			else if (command.equals("_bbsservice:augmentation"))
			{
				showMainMenu(activeChar, 0, AugmentationFilter.NONE);
			}
			else if (command.startsWith("_bbsservice:augmentationSection"))
			{
				final var st = new StringTokenizer(command, " ");
				st.nextToken();
				final var select = Integer.parseInt(st.nextToken());

				var _filter = AugmentationFilter.NONE;
				try
				{
					switch (select)
					{
						case 1 :
							_filter = AugmentationFilter.NONE;
							break;
						case 2 :
							_filter = AugmentationFilter.ACTIVE_SKILL;
							break;
						case 3 :
							_filter = AugmentationFilter.PASSIVE_SKILL;
							break;
						case 4 :
							_filter = AugmentationFilter.CHANCE_SKILL;
							break;
						case 5 :
							_filter = AugmentationFilter.STATS;
							break;
					}
				}
				catch (final Exception e)
				{
					e.printStackTrace();
				}
				showMainMenu(activeChar, 1, _filter);
			}
			else if (command.startsWith("_bbsservice:augmentationPage"))
			{
				final var st = new StringTokenizer(command, " ");
				st.nextToken();
				
				final var filter = Integer.parseInt(st.nextToken());
				final var page = Integer.parseInt(st.nextToken());
				
				var _filter = AugmentationFilter.NONE;
				try
				{
					switch (filter)
					{
						case 1 :
							_filter = AugmentationFilter.NONE;
							break;
						case 2 :
							_filter = AugmentationFilter.ACTIVE_SKILL;
							break;
						case 3 :
							_filter = AugmentationFilter.PASSIVE_SKILL;
							break;
						case 4 :
							_filter = AugmentationFilter.CHANCE_SKILL;
							break;
						case 5 :
							_filter = AugmentationFilter.STATS;
							break;
					}
				}
				catch (final Exception e)
				{
					e.printStackTrace();
				}
				showMainMenu(activeChar, page, _filter);
			}
			else if (command.startsWith("_bbsservice:augmentationPut"))
			{
				final var st = new StringTokenizer(command, " ");
				st.nextToken();
				final var select = Integer.parseInt(st.nextToken());
				final var selectpage = Integer.parseInt(st.nextToken());
				final var page = Integer.parseInt(st.nextToken());

				if ((activeChar.isInStoreMode()) || (activeChar.isProcessingRequest()) || (activeChar.getActiveRequester() != null))
				{
					activeChar.sendMessage((new ServerMessage("ServiceBBS.AUGMENT_STOREMOD", activeChar.getLang())).toString());
					return;
				}

				final var targetItem = activeChar.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
				if (targetItem == null)
				{
					activeChar.sendMessage((new ServerMessage("ServiceBBS.AUGMENT_NOWEAPON", activeChar.getLang())).toString());
					return;
				}
				
				if (!checkItemType(targetItem))
				{
					return;
				}
				
				if (activeChar.getInventory().getItemByItemId(Config.SERVICES_AUGMENTATION_ITEM[0]) == null)
				{
					activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
					return;
				}
				
				if (activeChar.getInventory().getItemByItemId(Config.SERVICES_AUGMENTATION_ITEM[0]).getCount() < Config.SERVICES_AUGMENTATION_ITEM[1])
				{
					activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
					return;
				}
				
				final var augm = OptionsParser.getInstance().getOptions(select);
				if (augm != null)
				{
					Skill skill = null;
					var name = "";
					if (augm.hasActiveSkill())
					{
						skill = augm.getActiveSkill().getSkill();
					}
					else if (augm.hasPassiveSkill())
					{
						skill = augm.getPassiveSkill().getSkill();
					}
					else if (augm.hasActivationSkills())
					{
						skill = augm.getActivationsSkills().get(0).getSkill();
					}
					
					if (skill != null)
					{
						name = skill.getName(activeChar.getLang());
					}
					else
					{
						switch (augm.getId())
						{
							case 16341 :
								name = "+1 " + ServerStorage.getInstance().getString(activeChar.getLang(), "ServiceBBS.STR") + "";
								break;
							case 16342 :
								name = "+1 " + ServerStorage.getInstance().getString(activeChar.getLang(), "ServiceBBS.CON") + "";
								break;
							case 16343 :
								name = "+1 " + ServerStorage.getInstance().getString(activeChar.getLang(), "ServiceBBS.INT") + "";
								break;
							case 16344 :
								name = "+1 " + ServerStorage.getInstance().getString(activeChar.getLang(), "ServiceBBS.MEN") + "";
								break;
							default :
								name = "(Id:" + augm.getId() + ")";
						}
					}
					
					final var msg = new ServerMessage("ServiceBBS.WANT_AUGMENT", activeChar.getLang());
					msg.add(name);
					activeChar.sendConfirmDlg(new AugmentationAnswerListener(activeChar, select, selectpage, page), 60000, msg.toString());
				}
					
			}
			else if (command.startsWith("_bbsservice:exchangerPage"))
			{
				final var st = new StringTokenizer(command, " ");
				st.nextToken();
				final var upgrade = st.nextToken();
				final var pg = st.nextToken();
				
				final boolean isUpgrade = upgrade.equalsIgnoreCase("1");
				
				removeVars(activeChar, true);
				cleanAtt(activeChar, -1);
				final var html = new NpcHtmlMessage(5);
				html.setFile(activeChar, activeChar.getLang(), "data/html/community/exchanger/page.htm");
				final var template = HtmCache.getInstance().getHtm(activeChar, activeChar.getLang(), "data/html/community/exchanger/template.htm");
				var block = "";
				var list = "";
				final List<Change> _list = new ArrayList<>();

				for (final var item : activeChar.getInventory().getPaperdollItems())
				{
					if (item != null)
					{
						final var change = ExchangeItemParser.getInstance().getChanges(item.getId(), isUpgrade);
						if (change != null)
						{
							_list.add(change);
						}
					}
				}

				if (_list.isEmpty())
				{
					final var html2 = new NpcHtmlMessage(5);
					html2.setHtml(activeChar, "<html><title>" + (isUpgrade ? "" + ServerStorage.getInstance().getString(activeChar.getLang(), "ServiceBBS.UPGRADE_ITEMS") + "" : "" + ServerStorage.getInstance().getString(activeChar.getLang(), "ServiceBBS.EXCHANGE_ITEMS") + "") + "</title><body><center><br><br><font name=hs12>" + ServerStorage.getInstance().getString(activeChar.getLang(), "ServiceBBS.WEAR_LIST") + "</font></center></body></html>");
					activeChar.sendPacket(html2);
					return;
				}

				final var perpage = 6;
				final var page = pg.length() > 0 ? Integer.parseInt(pg) : 1;
				int counter = 0;

				final var isThereNextPage = _list.size() > perpage;

				for (int i = (page - 1) * perpage; i < _list.size(); i++)
				{
					final var pack = _list.get(i);
					block = template;
					block = block.replace("{bypass}", "bypass -h _bbsservice:exchangerList " + pack.getId() + " " + (isUpgrade ? 1 : 0) + " " + 1);
					block = block.replace("{name}", Util.getItemName(activeChar, pack.getId()));
					block = block.replace("{icon}", pack.getIcon());
					final var msg = new ServerMessage("ServiceBBS.EXCHANGE_COST", activeChar.getLang());
					msg.toString(Util.formatPay(activeChar, pack.getCostCount(), pack.getCostId()));
					block = block.replace("{cost}", msg.toString());
					list = list + block;

					counter++;
					if (counter >= perpage)
					{
						break;
					}
				}
				
				final var pages = (double) _list.size() / perpage;
				final var count = (int) Math.ceil(pages);
				html.replace("%list%", list);
				html.replace("%navigation%", Util.getNavigationBlock(count, page, _list.size(), perpage, isThereNextPage, "_bbsservice:exchangerPage " + (isUpgrade ? 1 : 0) + " %s"));
				activeChar.sendPacket(html);
			}
			else if (command.startsWith("_bbsservice:exchangerList"))
			{
				final var st = new StringTokenizer(command, " ");
				st.nextToken();
				final var packId = st.nextToken();
				final var upgrade = st.nextToken();
				final var pg = st.nextToken();

				if (activeChar == null)
				{
					return;
				}

				cleanAtt(activeChar, -1);
				removeVars(activeChar, true);
				if ((packId.isEmpty()) || (!Util.isNumber(packId)))
				{
					return;
				}
				final var id = Integer.parseInt(packId);
				final var isUpgrade = upgrade.equalsIgnoreCase("1");
				
				final var html = new NpcHtmlMessage(5);
				html.setFile(activeChar, activeChar.getLang(), "data/html/community/exchanger/list.htm");
				final var template = HtmCache.getInstance().getHtm(activeChar, activeChar.getLang(), "data/html/community/exchanger/template.htm");
				var block = "";
				var list = "";
				
				final var change = ExchangeItemParser.getInstance().getChanges(id, isUpgrade);
				if (change == null)
				{
					return;
				}
				activeChar.addQuickVar("exchange", Integer.valueOf(id));
				
				final List<Variant> _list = change.getList();
				
				final var perpage = 6;
				final var page = pg.length() > 0 ? Integer.parseInt(pg) : 1;
				var counter = 0;
				
				final var isThereNextPage = _list.size() > perpage;
				
				for (int i = (page - 1) * perpage; i < _list.size(); i++)
				{
					final var pack = _list.get(i);
					block = template;
					block = block.replace("{bypass}", "bypass -h _bbsservice:exchangerOpen " + pack.getNumber() + " " + (isUpgrade ? 1 : 0));
					block = block.replace("{name}", Util.getItemName(activeChar, pack.getId()));
					block = block.replace("{icon}", pack.getIcon());
					final var msg = new ServerMessage("ServiceBBS.EXCHANGE_COST", activeChar.getLang());
					msg.toString(Util.formatPay(activeChar, change.getCostCount(), change.getCostId()));
					block = block.replace("{cost}", msg.toString());
					list = list + block;
					
					counter++;
					if (counter >= perpage)
					{
						break;
					}
				}
				
				final var pages = (double) _list.size() / perpage;
				final var count = (int) Math.ceil(pages);
				html.replace("%list%", list);
				html.replace("%navigation%", Util.getNavigationBlock(count, page, _list.size(), perpage, isThereNextPage, "_bbsservice:exchangerList " + id + " " + (isUpgrade ? 1 : 0) + " %s"));
				html.replace("%choice%", Util.getItemName(activeChar, change.getId()));

				activeChar.sendPacket(html);
			}
			else if (command.startsWith("_bbsservice:exchangerOpen"))
			{
				final var st = new StringTokenizer(command, " ");
				st.nextToken();
				final var packId = st.nextToken();
				final var upgrade = st.nextToken();

				if (activeChar == null)
				{
					return;
				}
				
				final var id = activeChar.getQuickVarI("exchange", new int[]
				{
				        -1
				});
				if ((id == -1) || (packId.isEmpty()) || (!Util.isNumber(packId)))
				{
					return;
				}
				final var new_id = Integer.parseInt(packId);
				final var isUpgrade = upgrade.equalsIgnoreCase("1");
				ItemInstance item = null;
				Change change = null;
				for (final var inv : activeChar.getInventory().getPaperdollItems())
				{
					if (inv != null)
					{
						change = ExchangeItemParser.getInstance().getChanges(inv.getId(), isUpgrade);
						if ((change != null) && (change.getId() == id))
						{
							item = inv;
							break;
						}
					}
				}
				if (item == null)
				{
					return;
				}
				final var variant = change.getVariant(new_id);
				if (variant == null)
				{
					return;
				}
				removeVars(activeChar, false);
				activeChar.addQuickVar("exchange_obj", Integer.valueOf(item.getObjectId()));
				activeChar.addQuickVar("exchange_new", Integer.valueOf(variant.getId()));
				activeChar.addQuickVar("exchange_attribute", Boolean.valueOf(change.attChange()));
				if (change.attChange())
				{
					activeChar.addQuickVar("exchange_number", Integer.valueOf(variant.getNumber()));
				}
				final var html = new NpcHtmlMessage(5);
				html.setFile(activeChar, activeChar.getLang(), "data/html/community/exchanger/general.htm");
				html.replace("%my_name%", item.getItem().getName(activeChar.getLang()));
				html.replace("%my_ench%", "+" + item.getEnchantLevel());
				html.replace("%my_icon%", item.getItem().getIcon());
				final var att = item.getElementals();
				if ((!change.attChange()) || (att == null))
				{
					var att_info = HtmCache.getInstance().getHtm(activeChar, activeChar.getLang(), "data/html/community/exchanger/att_info.htm");
					
					att_info = att_info.replace("%Fire%", String.valueOf(0));
					att_info = att_info.replace("%Water%", String.valueOf(0));
					att_info = att_info.replace("%Wind%", String.valueOf(0));
					att_info = att_info.replace("%Earth%", String.valueOf(0));
					att_info = att_info.replace("%Holy%", String.valueOf(0));
					att_info = att_info.replace("%Unholy%", String.valueOf(0));
					html.replace("%att_info%", att_info);
				}
				else
				{
					var att_info = HtmCache.getInstance().getHtm(activeChar, activeChar.getLang(), "data/html/community/exchanger/att_change.htm");
					if (activeChar.getQuickVarI("ex_att", new int[]
					{
					        -1
					}) == -1)
					{
						for (final Elemental el : Elementals.Elemental.VALUES)
						{
							if (el != null)
							{
								int value = -1;
								for (final var e : att)
								{
									if (e != null && e.getElement() == el.getId())
									{
										value = e.getValue();
										break;
									}
								}
								
								if (value > 0)
								{
    								switch (el)
    								{
    									case FIRE:
    										att_info = att_info.replace("%Fire%", String.valueOf(value));
    										break;
    									case WATER:
    										att_info = att_info.replace("%Water%", String.valueOf(value));
    										break;
    									case WIND:
    										att_info = att_info.replace("%Wind%", String.valueOf(value));
    										break;
    									case EARTH:
    										att_info = att_info.replace("%Earth%", String.valueOf(value));
    										break;
    									case HOLY:
    										att_info = att_info.replace("%Holy%", String.valueOf(value));
    										break;
    									case UNHOLY:
    										att_info = att_info.replace("%Unholy%", String.valueOf(value));
    										break;
    								}
								}
								else
								{
    								switch (el)
    								{
    									case FIRE:
    										att_info = att_info.replace("%Fire%", String.valueOf(0));
    										break;
    									case WATER:
    										att_info = att_info.replace("%Water%", String.valueOf(0));
    										break;
    									case WIND:
    										att_info = att_info.replace("%Wind%", String.valueOf(0));
    										break;
    									case EARTH:
    										att_info = att_info.replace("%Earth%", String.valueOf(0));
    										break;
    									case HOLY:
    										att_info = att_info.replace("%Holy%", String.valueOf(0));
    										break;
    									case UNHOLY:
    										att_info = att_info.replace("%Unholy%", String.valueOf(0));
    										break;
    								}
								}
							}
						}
					}
					else
					{
						if (item.isArmor())
						{
							final var changeFire = activeChar.getQuickVarI("ex_att_0", new int[]
							{
							        0
							});
							final var changeWater = activeChar.getQuickVarI("ex_att_1", new int[]
							{
							        0
							});
							
							final var changeWind = activeChar.getQuickVarI("ex_att_2", new int[]
							{
							        0
							});
							final var changeEarth = activeChar.getQuickVarI("ex_att_3", new int[]
							{
							        0
							});
							final var changeHoly = activeChar.getQuickVarI("ex_att_4", new int[]
							{
							        0
							});
							final var changeUnholy = activeChar.getQuickVarI("ex_att_5", new int[]
							{
							        0
							});
									
    						for (final Elemental el : Elementals.Elemental.VALUES)
    						{
    							if (el != null)
    							{
    								int value = -10;
    								for (final var e : att)
    								{
    									if (e != null && e.getElement() == el.getId())
    									{
    										value = e.getValue();
    										break;
    									}
    								}
    								
    								if (value > 0)
    								{
    									switch (el)
    									{
    										case FIRE :
    											if (changeWater > 0 && changeFire == 0)
    											{
    												att_info = att_info.replace("%Water%", String.valueOf(changeWater));
    												att_info = att_info.replace("%Fire%", String.valueOf(0));
    											}
    											else
    											{
    												att_info = att_info.replace("%Water%", String.valueOf(0));
    												att_info = att_info.replace("%Fire%", String.valueOf(value));
    											}
    											break;
    										case WATER :
    											if (changeFire > 0 && changeWater == 0)
    											{
    												att_info = att_info.replace("%Fire%", String.valueOf(changeFire));
    												att_info = att_info.replace("%Water%", String.valueOf(0));
    											}
    											else
    											{
    												att_info = att_info.replace("%Fire%", String.valueOf(0));
    												att_info = att_info.replace("%Water%", String.valueOf(value));
    											}
    											break;
    										case WIND :
    											if (changeEarth > 0 && changeWind == 0)
    											{
    												att_info = att_info.replace("%Earth%", String.valueOf(changeEarth));
    												att_info = att_info.replace("%Wind%", String.valueOf(0));
    											}
    											else
    											{
    												att_info = att_info.replace("%Earth%", String.valueOf(0));
    												att_info = att_info.replace("%Wind%", String.valueOf(value));
    											}
    											break;
    										case EARTH :
    											if (changeWind > 0 && changeEarth == 0)
    											{
    												att_info = att_info.replace("%Wind%", String.valueOf(changeWind));
    												att_info = att_info.replace("%Earth%", String.valueOf(0));
    											}
    											else
    											{
    												att_info = att_info.replace("%Wind%", String.valueOf(0));
    												att_info = att_info.replace("%Earth%", String.valueOf(value));
    											}
    											break;
    										case HOLY :
    											if (changeUnholy > 0 && changeHoly == 0)
    											{
    												att_info = att_info.replace("%Unholy%", String.valueOf(changeUnholy));
    												att_info = att_info.replace("%Holy%", String.valueOf(0));
    											}
    											else
    											{
    												att_info = att_info.replace("%Unholy%", String.valueOf(0));
    												att_info = att_info.replace("%Holy%", String.valueOf(value));
    											}
    											break;
    										case UNHOLY :
    											if (changeHoly > 0 && changeUnholy == 0)
    											{
    												att_info = att_info.replace("%Holy%", String.valueOf(changeHoly));
    												att_info = att_info.replace("%Unholy%", String.valueOf(0));
    											}
    											else
    											{
    												att_info = att_info.replace("%Holy%", String.valueOf(0));
    												att_info = att_info.replace("%Unholy%", String.valueOf(value));
    											}
    											break;
    									}
    								}
    								else
    								{
    									switch (el)
    									{
    										case FIRE :
    											if (changeWater == 0 && changeFire == 0)
    											{
    												att_info = att_info.replace("%Fire%", String.valueOf(activeChar.getQuickVarI("ex_att_0", new int[]
    												{
    												        0
    												})));
    											}
    											break;
    										case WATER :
    											if (changeWater == 0 && changeFire == 0)
    											{
    												att_info = att_info.replace("%Water%", String.valueOf(activeChar.getQuickVarI("ex_att_1", new int[]
    												{
    												        0
    												})));
    											}
    											break;
    										case WIND :
    											if (changeWind == 0 && changeEarth == 0)
    											{
    												att_info = att_info.replace("%Wind%", String.valueOf(activeChar.getQuickVarI("ex_att_2", new int[]
    												{
    												        0
    												})));
    											}
    											break;
    										case EARTH :
    											if (changeWind == 0 && changeEarth == 0)
    											{
    												att_info = att_info.replace("%Earth%", String.valueOf(activeChar.getQuickVarI("ex_att_3", new int[]
    												{
    												        0
    												})));
    											}
    											break;
    										case HOLY :
    											if (changeHoly == 0 && changeUnholy == 0)
    											{
    												att_info = att_info.replace("%Holy%", String.valueOf(activeChar.getQuickVarI("ex_att_4", new int[]
    												{
    												        0
    												})));
    											}
    											break;
    										case UNHOLY :
    											if (changeHoly == 0 && changeUnholy == 0)
    											{
    												att_info = att_info.replace("%Unholy%", String.valueOf(activeChar.getQuickVarI("ex_att_5", new int[]
    												{
    												        0
    												})));
    											}
    											break;
    									}
    								}
    							}
    						}
						}
						else if (item.isWeapon())
						{
							Elemental itemElement = null;
							for (final Elemental el : Elementals.Elemental.VALUES)
							{
								if (el != null)
								{
									final var changeAtt = activeChar.getQuickVarI("ex_att_" + el.getId() + "", new int[]
									{
										     0
									});
									if (changeAtt > 0)
									{
										itemElement = el;
	    								switch (el)
	    								{
	    									case FIRE:
	    										att_info = att_info.replace("%Fire%", String.valueOf(changeAtt));
	    										break;
	    									case WATER:
	    										att_info = att_info.replace("%Water%", String.valueOf(changeAtt));
	    										break;
	    									case WIND:
	    										att_info = att_info.replace("%Wind%", String.valueOf(changeAtt));
	    										break;
	    									case EARTH:
	    										att_info = att_info.replace("%Earth%", String.valueOf(changeAtt));
	    										break;
	    									case HOLY:
	    										att_info = att_info.replace("%Holy%", String.valueOf(changeAtt));
	    										break;
	    									case UNHOLY:
	    										att_info = att_info.replace("%Unholy%", String.valueOf(changeAtt));
	    										break;
	    								}
										break;
									}
								}
							}
								
							if (itemElement != null)
							{
								for (final Elemental e : Elementals.Elemental.VALUES)
								{
									if (e != null && e != itemElement)
									{
										switch (e)
										{
											case FIRE :
												att_info = att_info.replace("%Fire%", String.valueOf(0));
												break;
											case WATER :
												att_info = att_info.replace("%Water%", String.valueOf(0));
												break;
											case WIND :
												att_info = att_info.replace("%Wind%", String.valueOf(0));
												break;
											case EARTH :
												att_info = att_info.replace("%Earth%", String.valueOf(0));
												break;
											case HOLY :
												att_info = att_info.replace("%Holy%", String.valueOf(0));
												break;
											case UNHOLY :
												att_info = att_info.replace("%Unholy%", String.valueOf(0));
												break;
										}
									}
								}
							}
							else
							{
								for (final Elemental ee : Elementals.Elemental.VALUES)
								{
									if (ee != null)
									{
										final var ell = item.getElemental((byte) ee.getId());
										final int value = ell != null ? ell.getValue() : 0;
										switch (ee)
										{
											case FIRE :
												att_info = att_info.replace("%Fire%", String.valueOf(value));
												break;
											case WATER :
												att_info = att_info.replace("%Water%", String.valueOf(value));
												break;
											case WIND :
												att_info = att_info.replace("%Wind%", String.valueOf(value));
												break;
											case EARTH :
												att_info = att_info.replace("%Earth%", String.valueOf(value));
												break;
											case HOLY :
												att_info = att_info.replace("%Holy%", String.valueOf(value));
												break;
											case UNHOLY :
												att_info = att_info.replace("%Unholy%", String.valueOf(value));
												break;
										}
									}
								}
							}
						}
					}
					html.replace("%att_info%", att_info);
				}
				html.replace("%cost%", Util.formatPay(activeChar, change.getCostCount(), change.getCostId()));
				html.replace("%new_name%", Util.getItemName(activeChar, variant.getId()));
				html.replace("%new_icon%", variant.getIcon());
				html.replace("%new_id%", String.valueOf(id));
				html.replace("%is_upgrade%", (isUpgrade ? 1 : 0));
				
				activeChar.sendPacket(html);
			}
			else if (command.startsWith("_bbsservice:exchange"))
			{
				final var st = new StringTokenizer(command, " ");
				st.nextToken();
				final var upgrade = st.nextToken();
				
				final var exchangeId = activeChar.getQuickVarI("exchange", new int[]
				{
				        -1
				});
				if (exchangeId == -1)
				{
					return;
				}
				final var obj_my = activeChar.getQuickVarI("exchange_obj", new int[]
				{
				        -1
				});
				if (obj_my == -1)
				{
					return;
				}
				final var id_new = activeChar.getQuickVarI("exchange_new", new int[]
				{
				        -1
				});
				if (id_new == -1)
				{
					return;
				}

				final var isUpgrade = upgrade.equalsIgnoreCase("1");
				final var att_change = activeChar.getQuickVarB("exchange_attribute", new boolean[]
				{
				        false
				});

				final var change = ExchangeItemParser.getInstance().getChanges(exchangeId, isUpgrade);
				if (change == null)
				{
					return;
				}

				final var item_my = activeChar.getInventory().getItemByObjectId(obj_my);
				if (item_my == null)
				{
					return;
				}

				if (activeChar.getInventory().getItemByItemId(change.getCostId()) == null)
				{
					activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
					return;
				}
				if (activeChar.getInventory().getItemByItemId(change.getCostId()).getCount() < change.getCostCount())
				{
					activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
					return;
				}
				
				final var EnchantLevel = item_my.getEnchantLevel();
				final var Augmentation = item_my.getAugmentation();
				final var elementals = item_my.getElementals() == null ? null : item_my.getElementals();

				final int new_att = activeChar.getQuickVarI("ex_att", new int[]
				{
				        -1
				});
				
				final Map<Byte, Integer> elements = new HashMap<>();
				if ((att_change) && ((new_att != -1) || elementals != null))
				{
					if (item_my.isArmor())
					{
						for (final Elemental el : Elementals.Elemental.VALUES)
						{
							if (el != null)
							{
								if (elements.containsKey(el.getId()))
								{
									continue;
								}
								
								for (final var e : elementals)
								{
									if (e != null && e.getElement() == el.getId())
									{
										final int opositeElement = Elementals.getOppositeElement(e.getElement());
										final var changeEl = activeChar.getQuickVarI("ex_att_" + opositeElement + "", new int[]
										{
										        0
										});
										
										if (changeEl > 0)
										{
											elements.put(e.getElement(), 0);
											elements.put((byte) opositeElement, changeEl);
										}
										else
										{
											elements.put((byte) opositeElement, 0);
											elements.put(e.getElement(), e.getValue());
										}
										break;
									}
								}
							}
						}
					}
					else if (item_my.isWeapon())
					{
						for (final Elemental el : Elementals.Elemental.VALUES)
						{
							if (el != null)
							{
								if (elements.containsKey(el.getId()) || elements.size() > 0)
								{
									continue;
								}
								
								final var changeEl = activeChar.getQuickVarI("ex_att_" + el.getId() + "", new int[]
								{
								        0
								});
								
								if (changeEl > 0)
								{
									elements.put((byte) el.getId(), changeEl);
									break;
								}
							}
						}
						
						if (elements.isEmpty() && elementals != null)
						{
							elements.put(elementals[0].getElement(), elementals[0].getValue());
						}
					}
				}
				
				if (activeChar.getInventory().destroyItemByObjectId(item_my.getObjectId(), item_my.getCount(), activeChar, true) != null)
				{
					final var itemInstance = activeChar.getInventory().addItem("ExchangersBBS", id_new, 1, activeChar, true);
					itemInstance.setEnchantLevel(EnchantLevel);
					itemInstance.setAugmentation(Augmentation);
					if (!elements.isEmpty())
					{
						for (final var e : elements.keySet())
						{
							itemInstance.setElementAttr(e, elements.get(e));
						}
					}
					activeChar.getInventory().equipItem(itemInstance);

					final var iu = new InventoryUpdate();
					iu.addModifiedItem(itemInstance);
					activeChar.sendPacket(iu);
					
					activeChar.destroyItemByItemId("ExchangersBBS", change.getCostId(), change.getCostCount(), activeChar, true);
					Util.addServiceLog(activeChar.getName(null) + " buy exchange item service!");
					final ServerMessage msg = new ServerMessage("ServiceBBS.YOU_EXCHANGE", activeChar.getLang());
					msg.add(item_my.getItem().getName(activeChar.getLang()));
					msg.add(itemInstance.getItem().getName(activeChar.getLang()));
					activeChar.sendMessage(msg.toString());
				}
				removeVars(activeChar, true);
				cleanAtt(activeChar, -1);
			}
			else if (command.startsWith("_bbsservice:changeAtt"))
			{
				final var st = new StringTokenizer(command, " ");
				st.nextToken();
				final var attId = st.nextToken();
				final var upgrade = st.nextToken();
				
				if ((attId.isEmpty()) || (!Util.isNumber(attId)))
				{
					return;
				}
				final var obj_my = activeChar.getQuickVarI("exchange_obj", new int[]
				{
				        -1
				});
				if (obj_my == -1)
				{
					return;
				}
				final var item = activeChar.getInventory().getItemByObjectId(obj_my);
				if (item == null)
				{
					return;
				}
				final var id_new = activeChar.getQuickVarI("exchange_number", new int[]
				{
				        -1
				});
				if (id_new == -1)
				{
					return;
				}
				final var att_id = Integer.parseInt(attId);
				final var isUpgrade = upgrade.equalsIgnoreCase("1");
				final var att = Elementals.getElementById(att_id);
				if (att != Elementals.NONE)
				{
					boolean found = false;
					var value = 0;
					if (item.isArmor())
					{
						final byte opositeElement = Elementals.getOppositeElement(att);
						if (item.getElementals() != null)
						{
							for (final Elementals elm : item.getElementals())
							{
								if (elm.getElement() == opositeElement)
								{
									found = true;
									value = elm.getValue();
									break;
								}
							}
						}
						
						if (found)
						{
							activeChar.addQuickVar("ex_att_" + att_id, Integer.valueOf(value));
							activeChar.addQuickVar("ex_att", Integer.valueOf(att_id));
							cleanAtt(activeChar, opositeElement);
						}
						else
						{
							cleanAtt(activeChar, opositeElement);
						}
					}
					else if (item.isWeapon())
					{
						cleanAtt(activeChar, -1);
						if (item.getElementals() != null && att != item.getElementals()[0].getElement())
						{
							value = item.getElementals()[0].getValue();
						}
						
						if (value > 0)
						{
							activeChar.addQuickVar("ex_att_" + att_id, Integer.valueOf(value));
							activeChar.addQuickVar("ex_att", Integer.valueOf(att_id));
						}
					}
				}
				onBypassCommand("_bbsservice:exchangerOpen " + String.valueOf(id_new) + " " + (isUpgrade ? "1" : "0"), activeChar);
			}
			else if (command.startsWith("_bbsservice:donateList"))
			{
				final var st = new StringTokenizer(command, " ");
				st.nextToken();
				final var groupId = st.nextToken();
				final var pageId = st.nextToken();

				if (!groupId.isEmpty() && Util.isNumber(groupId) && (pageId.isEmpty() || Util.isNumber(pageId)))
				{
					final var id = Integer.parseInt(groupId);
					removeVars(activeChar);
					final var html = new NpcHtmlMessage(5);
					html.setFile(activeChar, activeChar.getLang(), "data/html/community/donate/index.htm");
					final var template = HtmCache.getInstance().getHtm(activeChar, activeChar.getLang(), "data/html/community/donate/template.htm");
					var block = "";
					var list = "";
					final List<Donation> _donate = DonationParser.getInstance().getGroup(id);
					final var perpage = 6;
					final var page = pageId.length() > 0 ? Integer.parseInt(pageId) : 1;
					int counter = 0;

					final var isThereNextPage = _donate.size() > perpage;
					
					for (int i = (page - 1) * perpage; i < _donate.size(); i++)
					{
						final var pack = _donate.get(i);
						block = template.replace("{bypass}", "bypass -h _bbsservice:donateOpen " + pack.getId());
						block = block.replace("{name}", pack.getName(activeChar.getLang()));
						block = block.replace("{icon}", pack.getIcon());
						final var simple = pack.getSimple();
						block = block.replace("{cost}", "" + ServerStorage.getInstance().getString(activeChar.getLang(), "ServiceBBS.COST") + "" + Util.formatPay(activeChar, simple.getCount(), simple.getId()));
						list += block;
						counter++;
						if (counter >= perpage)
						{
							break;
						}
					}
					
					final var pages = (double) _donate.size() / perpage;
					final var count = (int) Math.ceil(pages);
					html.replace("%list%", list);
					html.replace("%navigation%", Util.getNavigationBlock(count, page, _donate.size(), perpage, isThereNextPage, "_bbsservice:donateList " + id + " %s"));
					activeChar.sendPacket(html);
				}
			}
			else if (command.startsWith("_bbsservice:donateOpen"))
			{
				final var st = new StringTokenizer(command, " ");
				st.nextToken();
				final var groupId = st.nextToken();

				if (!Util.isNumber(groupId))
				{
					return;
				}
				
				final var donate = DonationParser.getInstance().getDonate(Integer.parseInt(groupId));
				if (donate == null)
				{
					return;
				}
				
				final var html = new NpcHtmlMessage(0);
				html.setFile(activeChar, activeChar.getLang(), "data/html/community/donate/open.htm");
				var content = "";
				
				final Map<Integer, Long> price = new HashMap<>();
				
				html.replace("%name%", donate.getName(activeChar.getLang()));
				html.replace("%icon%", donate.getIcon());
				html.replace("%id%", String.valueOf(donate.getId()));
				html.replace("%group%", String.valueOf(donate.getGroup()));
				
				final var simple = donate.getSimple();
				html.replace("%cost%", "" + ServerStorage.getInstance().getString(activeChar.getLang(), "ServiceBBS.COST") + "" + Util.formatPay(activeChar, simple.getCount(), simple.getId()));
				price.put(Integer.valueOf(simple.getId()), Long.valueOf(simple.getCount()));
				if (donate.haveFound())
				{
					final var enchant = isVar(activeChar, _vars[0]);
					final var found = donate.getFound();
					var block = HtmCache.getInstance().getHtm(activeChar, activeChar.getLang(), "data/html/community/donate/foundation.htm");
					
					block = block.replace("{bypass}", "bypass -h _bbsservice:donateVar " + _vars[0] + " " + (enchant ? 0 : 1) + " " + donate.getId());
					block = block.replace("{status}", enchant ? "" + ServerStorage.getInstance().getString(activeChar.getLang(), "ServiceBBS.BUY_IT") + "" : "" + ServerStorage.getInstance().getString(activeChar.getLang(), "ServiceBBS.NOT_BUY") + "");
					block = block.replace("{cost}", "" + ServerStorage.getInstance().getString(activeChar.getLang(), "ServiceBBS.COST") + "" + Util.formatPay(activeChar, found.getCount(), found.getId()));
					block = block.replace("{action}", enchant ? "" + ServerStorage.getInstance().getString(activeChar.getLang(), "ServiceBBS.CANCEL") + "" : "" + ServerStorage.getInstance().getString(activeChar.getLang(), "ServiceBBS.BUY") + "");
					if (enchant)
					{
						updatePrice(price, found.getId(), found.getCount());
					}
					
					content += block;
				}
				
				final var enchant = donate.getEnchant();
				if (enchant != null)
				{
					final var is = isVar(activeChar, _vars[1]);
					var block = HtmCache.getInstance().getHtm(activeChar, activeChar.getLang(), "data/html/community/donate/enchant.htm");
					block = block.replace("{bypass}", "bypass -h _bbsservice:donateVar " + _vars[1] + " " + (is ? 0 : 1) + " " + donate.getId());
					block = block.replace("{status}", is ? "" + ServerStorage.getInstance().getString(activeChar.getLang(), "ServiceBBS.BUY_IT") + "" : "" + ServerStorage.getInstance().getString(activeChar.getLang(), "ServiceBBS.NOT_BUY") + "");
					block = block.replace("{ench}", "+" + enchant.getEnchant());
					block = block.replace("{cost}", "" + ServerStorage.getInstance().getString(activeChar.getLang(), "ServiceBBS.COST") + "" + Util.formatPay(activeChar, enchant.getCount(), enchant.getId()));
					block = block.replace("{action}", is ? "" + ServerStorage.getInstance().getString(activeChar.getLang(), "ServiceBBS.CANCEL") + "" : "" + ServerStorage.getInstance().getString(activeChar.getLang(), "ServiceBBS.BUY") + "");
					if (is)
					{
						updatePrice(price, enchant.getId(), enchant.getCount());
					}
					
					content += block;
				}
				final var att = donate.getAttribution();
				if (att != null && att.getSize() >= 1)
				{
					var is = isVar(activeChar, _vars[2]);
					if (is && checkAttVars(activeChar, att.getSize()))
					{
						is = false;
						activeChar.unsetVar(_vars[2]);
						onBypassCommand("_bbsservice:donateVar " + _vars[2] + " 0 " + donate.getId(), activeChar);
					}
					
					var block = HtmCache.getInstance().getHtm(activeChar, activeChar.getLang(), "data/html/community/donate/attribute.htm");
					block = block.replace("{bypass}", is ? "bypass -h _bbsservice:donateVar " + _vars[2] + " " + 0 + " " + donate.getId() : "bypass -h _bbsservice:donateAttr " + donate.getId());
					block = block.replace("{status}", is ? "" + ServerStorage.getInstance().getString(activeChar.getLang(), "ServiceBBS.BUY_IT") + "" : "" + ServerStorage.getInstance().getString(activeChar.getLang(), "ServiceBBS.NOT_BUY") + "");
					block = block.replace("{cost}", "" + ServerStorage.getInstance().getString(activeChar.getLang(), "ServiceBBS.COST") + "" + Util.formatPay(activeChar, att.getCount(), att.getId()));
					block = block.replace("{action}", is ? "" + ServerStorage.getInstance().getString(activeChar.getLang(), "ServiceBBS.CANCEL") + "" : "" + ServerStorage.getInstance().getString(activeChar.getLang(), "ServiceBBS.BUY") + "");
					if (is)
					{
						updatePrice(price, att.getId(), att.getCount());
					}
					
					content += block;
				}
				
				var total = "";
				
				for (final Entry<Integer, Long> map : price.entrySet())
				{
					total += Util.formatPay(activeChar, map.getValue(), map.getKey()) + "<br1>";
				}
				
				html.replace("%content%", content);
				html.replace("%total%", total);
				
				activeChar.sendPacket(html);
			}
			else if (command.startsWith("_bbsservice:donateVar"))
			{
				final var st = new StringTokenizer(command, " ");
				st.nextToken();
				final var arg0 = st.nextToken();
				final var arg1 = st.nextToken();
				final var arg2 = st.nextToken();

				if (!Util.isNumber(arg1) || !Util.isNumber(arg2))
				{
					return;
				}

				final var action = Integer.parseInt(arg1);
				final var var = arg0;
				activeChar.addQuickVar(var, action);

				if (action == 0)
				{
					activeChar.deleteQuickVar(var);
					if (var.equals(_vars[2]))
					{
						for (int i = 1; i <= 3; i++)
						{
							activeChar.deleteQuickVar("att_" + i);
						}
					}
				}
				onBypassCommand("_bbsservice:donateOpen " + arg2, activeChar);
			}
			else if (command.startsWith("_bbsservice:donateAttr"))
			{
				final var st = new StringTokenizer(command, " ");
				st.nextToken();
				final var attrId = st.nextToken();

				if (!Util.isNumber(attrId))
				{
					return;
				}
				
				final var id = Integer.parseInt(attrId);
				final var donate = DonationParser.getInstance().getDonate(id);
				if (donate == null)
				{
					return;
				}
				
				final var atribute = donate.getAttribution();
				if (atribute == null)
				{
					return;
				}
				
				if (atribute.getSize() < 1)
				{
					onBypassCommand("_bbsservice:donateOpen " + attrId, activeChar);
					return;
				}
				
				final var html = new NpcHtmlMessage(0);
				html.setFile(activeChar, activeChar.getLang(), "data/html/community/donate/attribute_choice.htm");
				html.replace("%name%", donate.getName(activeChar.getLang()));
				html.replace("%icon%", donate.getIcon());
				html.replace("%bypass%", "bypass -h _bbsservice:donateOpen " + donate.getId());
				html.replace("%value%", String.valueOf(atribute.getValue()));
				html.replace("%size%", String.valueOf(atribute.getSize()));
				html.replace("%id%", String.valueOf(donate.getId()));
				
				final var att_1 = activeChar.getQuickVarI("att_1", -1);
				final var att_2 = activeChar.getQuickVarI("att_2", -1);
				final var att_3 = activeChar.getQuickVarI("att_3", -1);
				html.replace("%att_1%", atribute.getSize() >= 1 ? (att_1 == -1 ? "..." : elementName(activeChar, att_1)) : "<font color=FF0000>" + ServerStorage.getInstance().getString(activeChar.getLang(), "CommunityGeneral.SLOT_BLOCK") + "</font>");
				html.replace("%att_2%", atribute.getSize() >= 2 ? (att_2 == -1 ? "..." : elementName(activeChar, att_2)) : "<font color=FF0000>" + ServerStorage.getInstance().getString(activeChar.getLang(), "CommunityGeneral.SLOT_BLOCK") + "</font>");
				html.replace("%att_3%", atribute.getSize() == 3 ? (att_3 == -1 ? "..." : elementName(activeChar, att_3)) : "<font color=FF0000>" + ServerStorage.getInstance().getString(activeChar.getLang(), "CommunityGeneral.SLOT_BLOCK") + "</font>");
				
				build(activeChar, html, donate, att_1, att_2, att_3);
				activeChar.sendPacket(html);
			}
			else if (command.startsWith("_bbsservice:donatePut"))
			{
				final var st = new StringTokenizer(command, " ");
				st.nextToken();
				final var arg0 = st.nextToken();
				final var arg1 = st.nextToken();
				
				if (!Util.isNumber(arg0) || !Util.isNumber(arg1))
				{
					return;
				}

				final var att = Integer.parseInt(arg1);
				if (activeChar.getQuickVarI("att_1", -1) == -1)
				{
					activeChar.addQuickVar("att_1", att);
				}
				else if (activeChar.getQuickVarI("att_2", -1) == -1)
				{
					activeChar.addQuickVar("att_2", att);
				}
				else if (activeChar.getQuickVarI("att_3", -1) == -1)
				{
					activeChar.addQuickVar("att_3", att);
				}
				activeChar.addQuickVar(_vars[2], 1);
				onBypassCommand("_bbsservice:donateAttr " + arg0, activeChar);
			}
			else if (command.startsWith("_bbsservice:donateClearAtt"))
			{
				final var st = new StringTokenizer(command, " ");
				st.nextToken();
				final var attId = st.nextToken();
				
				if (!Util.isNumber(attId))
				{
					return;
				}
				
				for (var i = 1; i <= 3; i++)
				{
					activeChar.deleteQuickVar("att_" + i);
				}
				onBypassCommand("_bbsservice:donateAttr " + attId, activeChar);
			}
			else if (command.startsWith("_bbsservice:donateBuy"))
			{
				final var st = new StringTokenizer(command, " ");
				st.nextToken();
				final var donateId = st.nextToken();

				if (!Util.isNumber(donateId))
				{
					return;
				}
				
				final var id = Integer.parseInt(donateId);
				final var donate = DonationParser.getInstance().getDonate(id);
				if (donate == null)
				{
					return;
				}
				
				if (!activeChar.isInventoryUnder90(false))
				{
					activeChar.sendPacket(SystemMessageId.INVENTORY_LESS_THAN_80_PERCENT);
					return;
				}
				
				final Map<Integer, Long> price = new HashMap<>();
				final var simple = donate.getSimple();
				
				price.put(simple.getId(), simple.getCount());
				
				final var foundation = donate.getFound();
				final var found_list = donate.haveFound() && foundation != null && activeChar.getQuickVarI(_vars[0], -1) != -1;
				if (found_list)
				{
					updatePrice(price, foundation.getId(), foundation.getCount());
				}
				
				final var enchant = donate.getEnchant();
				final var enchanted = enchant != null && activeChar.getQuickVarI(_vars[1], -1) != -1;
				if (enchanted)
				{
					updatePrice(price, enchant.getId(), enchant.getCount());
				}
				
				final var att = donate.getAttribution();
				final var attribution = att != null && activeChar.getQuickVarI(_vars[2], -1) != -1;
				if (attribution)
				{
					updatePrice(price, att.getId(), att.getCount());
				}
				
				for (final Entry<Integer, Long> map : price.entrySet())
				{
					final var _id = map.getKey();
					final var _count = map.getValue();
					
					if (activeChar.getInventory().getItemByItemId(_id) == null)
					{
						activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
						return;
					}
					if (activeChar.getInventory().getItemByItemId(_id).getCount() < _count)
					{
						activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
						return;
					}
					activeChar.destroyItemByItemId("DonateBBS", _id, _count, activeChar, true);
				}
				
				for (final var _donate : (found_list ? foundation.getList() : simple.getList()))
				{
					final var template = ItemsParser.getInstance().getTemplate(_donate.getId());
					if (template == null)
					{
						return;
					}
					
					if (template.isStackable())
					{
						final ItemInstance item = activeChar.getInventory().addItem("DonateBBS", _donate.getId(), _donate.getCount(), activeChar, true);
						Util.addServiceLog(activeChar.getName(null) + " buy donate item: " + item.getName(null) + " +" + item.getEnchantLevel());
						
						final var iu = new InventoryUpdate();
						iu.addModifiedItem(item);
						activeChar.sendPacket(iu);
					}
					else
					{
						for (int it = 1; it <= _donate.getCount(); it++)
						{
							final var item = activeChar.getInventory().addItem("DonateBBS", _donate.getId(), 1, activeChar, true);
							
							var enchant_level = 0;
							if (enchanted)
							{
								enchant_level = enchant.getEnchant();
							}
							else if (_donate.getEnchant() > 0)
							{
								enchant_level = _donate.getEnchant();
							}
							
							if (enchant_level > 0)
							{
								item.setEnchantLevel(enchant_level);
							}
							
							if ((item.isArmor() || item.isWeapon()) && attribution)
							{
								for (var i = 1; i <= att.getSize(); i++)
								{
									final var element_id = activeChar.getQuickVarI("att_" + i, -1);
									if (element_id != -1)
									{
										var element = Elementals.getElementById(element_id);
										
										if (item.isArmor())
										{
											element = Elementals.getReverseElement(element);
										}
										item.setElementAttr(element, att.getValue());
									}
								}
							}
							
							Util.addServiceLog(activeChar.getName(null) + " buy donate item: " + item.getName(null) + " +" + item.getEnchantLevel());
							
							final var iu = new InventoryUpdate();
							iu.addModifiedItem(item);
							activeChar.sendPacket(iu);
						}
					}
				}
				removeVars(activeChar);
				final var msg = new ServerMessage("ServiceBBS.YOU_BUY_ITEM", activeChar.getLang());
				msg.add(donate.getName(activeChar.getLang()));
				activeChar.sendMessage(msg.toString());
			}
			else if (command.equals("_bbsservice:cloak"))
			{
				final var i = activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_CLOAK);
				if (!isValidCloak(i))
				{
					activeChar.sendMessage((new ServerMessage("ServiceBBS.CLOAK_EQUIPED", activeChar.getLang())).toString());
					return;
				}
				final var html = new NpcHtmlMessage(0);
				html.setFile(activeChar, activeChar.getLang(), "data/html/community/donate/cloaks.htm");
				html.replace("%price%", Util.formatPay(activeChar, Config.SERVICES_SOUL_CLOAK_TRANSFER_ITEM[1], Config.SERVICES_SOUL_CLOAK_TRANSFER_ITEM[0]));
				activeChar.sendPacket(html);
			}
			else if (command.startsWith("_bbsservice:cloakSend"))
			{
				String str = null;
				final var st = new StringTokenizer(command, " ");
				st.nextToken();
				try
				{
					str = st.nextToken();
				}
				catch (final Exception e)
				{}
				
				if (str != null)
				{
					final var currentItem = activeChar.getInventory().getPaperdollItem(Inventory.PAPERDOLL_CLOAK);
					if (!isValidCloak(currentItem.getId()))
					{
						activeChar.sendMessage((new ServerMessage("ServiceBBS.CLOAK_EQUIPED", activeChar.getLang())).toString());
						return;
					}

					final var reciver = GameObjectsStorage.getPlayer(str);
					if (reciver == null)
					{
						final ServerMessage msg = new ServerMessage("ServiceBBS.PLAYER_OFF", activeChar.getLang());
						msg.add(str);
						activeChar.sendMessage(msg.toString());
						return;
					}
					
					if (!reciver.isInventoryUnder90(false))
					{
						activeChar.sendPacket(SystemMessageId.INVENTORY_LESS_THAN_80_PERCENT);
						return;
					}
					
					if (activeChar.getInventory().getItemByItemId(Config.SERVICES_SOUL_CLOAK_TRANSFER_ITEM[0]) == null)
					{
						activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
						return;
					}
					if (activeChar.getInventory().getItemByItemId(Config.SERVICES_SOUL_CLOAK_TRANSFER_ITEM[0]).getCount() < Config.SERVICES_SOUL_CLOAK_TRANSFER_ITEM[1])
					{
						activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
						return;
					}

					final var i = currentItem.getId();
					if (activeChar.getInventory().destroyItemByObjectId(currentItem.getObjectId(), currentItem.getCount(), activeChar, true) != null)
					{
						activeChar.destroyItemByItemId("TransferBBS", Config.SERVICES_SOUL_CLOAK_TRANSFER_ITEM[0], Config.SERVICES_SOUL_CLOAK_TRANSFER_ITEM[1], activeChar, true);
						Util.addServiceLog(activeChar.getName(null) + " buy transfer cloak service!");
						final ItemInstance newItem = reciver.getInventory().addItem("ExchangersBBS", i, 1, reciver, true);
						newItem.setEnchantLevel(currentItem.getEnchantLevel());

						final var iu = new InventoryUpdate();
						iu.addModifiedItem(newItem);
						reciver.sendPacket(iu);

						final var msg = new ServerMessage("ServiceBBS.TRANSFER_CLOAK", activeChar.getLang());
						msg.add(newItem.getItem().getName(activeChar.getLang()));
						msg.add(reciver.getName(null));
						activeChar.sendMessage(msg.toString());

						final var msg1 = new ServerMessage("ServiceBBS.SENDER_CLOAK", reciver.getLang());
						msg1.add(activeChar.getName(null));
						msg1.add(newItem.getItem().getName(activeChar.getLang()));
						reciver.sendMessage(msg1.toString());
					}
				}
			}
			else if (command.equals("_bbsservice:olfShirt"))
			{
				final var htm = new NpcHtmlMessage(0);
				htm.setFile(activeChar, activeChar.getLang(), "data/html/community/donate/olfstore.htm");
				activeChar.sendPacket(htm);
			}
			else if (command.startsWith("_bbsservice:olfShirtBuy"))
			{
				final var st = new StringTokenizer(command, " ");
				st.nextToken();
				final var olfId = st.nextToken();

				if (!Util.isNumber(olfId))
				{
					return;
				}
				
				final var enchant = Integer.parseInt(olfId);
				final var msg = new ServerMessage("ServiceBBS.WANT_BUY_OLF", activeChar.getLang());
				msg.add(enchant);
				activeChar.sendConfirmDlg(new EnchantOlfAnswerListner(activeChar, enchant), 60000, msg.toString());
			}
			else if (command.equals("_bbsservice:olfTransfer"))
			{
				final var i = activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_UNDER);
				final var j = 21580;
				if (i != j)
				{
					activeChar.sendMessage((new ServerMessage("ServiceBBS.OLF_EQUIPED", activeChar.getLang())).toString());
					return;
				}
				final var htm = new NpcHtmlMessage(0);
				htm.setFile(activeChar, activeChar.getLang(), "data/html/community/donate/olftransfer.htm");
				htm.replace("%price%", Util.formatPay(activeChar, Config.SERVICES_OLF_TRANSFER_ITEM[1], Config.SERVICES_OLF_TRANSFER_ITEM[0]));
				activeChar.sendPacket(htm);
			}
			else if (command.startsWith("_bbsservice:olfShirtTransfer"))
			{
				String str = null;
				final var st = new StringTokenizer(command, " ");
				st.nextToken();
				try
				{
					str = st.nextToken();
				}
				catch (final Exception e)
				{}
				if (str != null)
				{
					final var itemId = 21580;
					final var currentItem = activeChar.getInventory().getPaperdollItem(Inventory.PAPERDOLL_UNDER);
					if ((currentItem == null) || (currentItem.getId() != itemId))
					{
						activeChar.sendMessage((new ServerMessage("ServiceBBS.OLF_EQUIPED", activeChar.getLang())).toString());
						return;
					}
					final var reciver = GameObjectsStorage.getPlayer(str);
					if (reciver == null)
					{
						final ServerMessage msg = new ServerMessage("ServiceBBS.PLAYER_OFF", activeChar.getLang());
						msg.add(str);
						activeChar.sendMessage(msg.toString());
						return;
					}
					
					if (!reciver.isInventoryUnder90(false))
					{
						activeChar.sendPacket(SystemMessageId.INVENTORY_LESS_THAN_80_PERCENT);
						return;
					}
					
					if (activeChar.getInventory().getItemByItemId(Config.SERVICES_OLF_TRANSFER_ITEM[0]) == null)
					{
						activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
						return;
					}
					if (activeChar.getInventory().getItemByItemId(Config.SERVICES_OLF_TRANSFER_ITEM[0]).getCount() < Config.SERVICES_OLF_TRANSFER_ITEM[1])
					{
						activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
						return;
					}
					
					if (activeChar.getInventory().destroyItemByObjectId(currentItem.getObjectId(), currentItem.getCount(), activeChar, true) != null)
					{
						activeChar.destroyItemByItemId("TransferBBS", Config.SERVICES_OLF_TRANSFER_ITEM[0], Config.SERVICES_OLF_TRANSFER_ITEM[1], activeChar, true);
						Util.addServiceLog(activeChar.getName(null) + " buy OLF transfer service!");
						final var newItem = reciver.getInventory().addItem("ExchangersBBS", itemId, 1, reciver, true);
						newItem.setEnchantLevel(currentItem.getEnchantLevel());
						
						final var iu = new InventoryUpdate();
						iu.addModifiedItem(newItem);
						reciver.sendPacket(iu);
						
						final var msg = new ServerMessage("ServiceBBS.TRANSFER_CLOAK", activeChar.getLang());
						msg.add(newItem.getItem().getName(activeChar.getLang()));
						msg.add(reciver.getName(null));
						activeChar.sendMessage(msg.toString());
						
						final var msg1 = new ServerMessage("ServiceBBS.SENDER_CLOAK", reciver.getLang());
						msg1.add(activeChar.getName(null));
						msg1.add(newItem.getItem().getName(activeChar.getLang()));
						reciver.sendMessage(msg1.toString());
					}
				}
			}
			else if (command.startsWith("_bbsservice:newSubPage"))
			{
				String race = null;
				final var st = new StringTokenizer(command, " ");
				st.nextToken();
				try
				{
					race = st.nextToken();
				}
				catch (final Exception e)
				{}
				addNewSubPage(activeChar, race);
			}
			else if (command.startsWith("_bbsservice:addNewSub"))
			{
				final var st = new StringTokenizer(command, " ");
				st.nextToken();

				final var subclassId = Integer.parseInt(st.nextToken());
				
				if (activeChar.getQuickVarI("SubToRemove", 0) > 0)
				{
					final var msg = new ServerMessage("ServiceBBS.WANT_CHANGE_SUB", activeChar.getLang());
					activeChar.sendConfirmDlg(new ChangeSubClassListener(activeChar, subclassId), 60000, msg.toString());
				}
				else
				{
					addNewSub(activeChar, subclassId);
				}
			}
			else if (command.equals("_bbsservice:changeSubPage"))
			{
				changeSubPage(activeChar);
			}
			else if (command.startsWith("_bbsservice:changeSubTo"))
			{
				final var st = new StringTokenizer(command, " ");
				st.nextToken();
				
				final var subclassId = Integer.parseInt(st.nextToken());
				
				changeSub(activeChar, subclassId);
			}
			else if (command.equals("_bbsservice:cancelSubPage"))
			{
				cancelSubPage(activeChar);
			}
			else if (command.startsWith("_bbsservice:selectCancelSub"))
			{
				final var st = new StringTokenizer(command, " ");
				st.nextToken();
				
				final var subclassId = Integer.parseInt(st.nextToken());
				activeChar.addQuickVar("SubToRemove", subclassId);
				
				sendFileToPlayer(activeChar, "data/html/community/subclass/subclassChanger_add.htm");
			}
			else if (command.equals("_bbsservice:chooseCertificate"))
			{
				chooseCertificatePage(activeChar);
			}
			else if (command.startsWith("_bbsservice:giveCertificate"))
			{
				final var st = new StringTokenizer(command, " ");
				st.nextToken();
				
				final var certifications = st.nextToken();
				
				if (!activeChar.isSubClassActive())
				{
					activeChar.sendMessage((new ServerMessage("CommunityGeneral.SUB_NOT_ACTIVE", activeChar.getLang())).toString());
					return;
				}
				
				if (certifications.equals("CommunityCert65"))
				{
					CommunityCert65(activeChar);
				}
				else if (certifications.equals("CommunityCert70"))
				{
					CommunityCert70(activeChar);
				}
				else if (certifications.equals("CommunityCert75Class"))
				{
					CommunityCert75Class(activeChar);
				}
				else if (certifications.equals("CommunityCert75Master"))
				{
					CommunityCert75Master(activeChar);
				}
				else if (certifications.equals("CommunityCert80"))
				{
					CommunityCert80(activeChar);
				}
			}
		}
		else
		{
			final var sb = new ShowBoard("<html><body><br><br><center>the command: " + command + " is not implemented yet</center><br><br></body></html>", "101", activeChar);
			activeChar.sendPacket(sb);
			activeChar.sendPacket(new ShowBoard(null, "102", activeChar));
			activeChar.sendPacket(new ShowBoard(null, "103", activeChar));
		}
	}

	protected void getCertified(Player player, int itemId, String var)
	{
		final var st = player.getQuestState("SubClassSkills");
		final var qvar = st.getGlobalQuestVar(var);
		if (!qvar.equals("") && !qvar.equals("0"))
		{
			return;
		}
		
		if (!player.isInventoryUnder90(false))
		{
			player.sendPacket(SystemMessageId.INVENTORY_LESS_THAN_80_PERCENT);
			return;
		}
		
		final var item = player.getInventory().addItem("Quest", itemId, 1, player, player.getTarget());
		st.saveGlobalQuestVar(var, "" + item.getObjectId());
		player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.EARNED_ITEM_S1).addItemName(item));
	}

	protected void CommunityCert65(Player player)
	{
		if (!checkCertificationCondition(player, 65, "EmergentAbility65-"))
		{
			return;
		}
		getCertified(player, 10280, "EmergentAbility65-" + player.getClassIndex());
		onBypassCommand("_bbsservice:chooseCertificate", player);
	}
	
	protected void CommunityCert70(Player player)
	{
		if (!checkCertificationCondition(player, 70, "EmergentAbility70-"))
		{
			return;
		}
		getCertified(player, 10280, "EmergentAbility70-" + player.getClassIndex());
		onBypassCommand("_bbsservice:chooseCertificate", player);
	}

	protected void CommunityCert75Class(Player player)
	{
		if (!checkCertificationCondition(player, 75, "ClassAbility75-"))
		{
			return;
		}
		getCertified(player, CLASSITEMS[CertificationUtils.getClassIndex(player)], "ClassAbility75-" + player.getClassIndex());
		onBypassCommand("_bbsservice:chooseCertificate", player);
	}

	protected void CommunityCert75Master(Player player)
	{
		if (!checkCertificationCondition(player, 75, "ClassAbility75-"))
		{
			return;
		}
		getCertified(player, 10612, "ClassAbility75-" + player.getClassIndex());
		onBypassCommand("_bbsservice:chooseCertificate", player);
	}

	protected void CommunityCert80(Player player)
	{
		if (!checkCertificationCondition(player, 80, "ClassAbility80-"))
		{
			return;
		}
		getCertified(player, TRANSFORMITEMS[CertificationUtils.getClassIndex(player)], "ClassAbility80-" + player.getClassIndex());
		onBypassCommand("_bbsservice:chooseCertificate", player);
	}

	private boolean checkCertificationCondition(Player player, int requiredLevel, String index)
	{
		var failed = false;
		if (player.getLevel() < requiredLevel)
		{
			player.sendMessage((new ServerMessage("CommunityGeneral.YOU_LEVEL_LOW", player.getLang())).toString());
			failed = true;
		}

		var st = player.getQuestState("SubClassSkills");
		if (st == null)
		{
			final var subClassSkilllsQuest = QuestManager.getInstance().getQuest("SubClassSkills");
			if (subClassSkilllsQuest != null)
			{
				st = subClassSkilllsQuest.newQuestState(player);
			}
			else
			{
				_log.warn("Null SubClassSkills quest, for Certification level: " + requiredLevel + " for player " + player.getName(null) + "!");
				return false;
			}
		}
		
		final var CertificationIndex = st.getGlobalQuestVar(index + player.getClassIndex());
		if (!CertificationIndex.equals("") && !CertificationIndex.equals("0"))
		{
			player.sendMessage((new ServerMessage("CommunityGeneral.HAVE_CETRIFICATION", player.getLang())).toString());
			failed = true;
		}

		if (failed)
		{
			sendFileToPlayer(player, "data/html/community/subclass/subclassChanger.htm");
			return false;
		}
		return true;
	}

	private static void chooseCertificatePage(Player player)
	{
		if (!canChangeClass(player))
		{
			return;
		}
		
		if (player.getBaseClass() == player.getClassId().getId())
		{
			sendFileToPlayer(player, "data/html/community/subclass/subclassChanger_back.htm");
			return;
		}
		
		final String[][] certifications =
		{
		        {
		                ServerStorage.getInstance().getString(player.getLang(), "CommunityGeneral.CERT65_EMERGENT"), "CommunityCert65"
				},
				{
				        ServerStorage.getInstance().getString(player.getLang(), "CommunityGeneral.CERT70_EMERGENT"), "CommunityCert70"
				},
				{
				        ServerStorage.getInstance().getString(player.getLang(), "CommunityGeneral.CERT75_CLASS"), "CommunityCert75Class"
				},
				{
				        ServerStorage.getInstance().getString(player.getLang(), "CommunityGeneral.CERT75_MASTER"), "CommunityCert75Master"
				},
				{
				        ServerStorage.getInstance().getString(player.getLang(), "CommunityGeneral.CERT80_DIVINE"), "CommunityCert80"
				}
		};
		
		final String[] replacements = new String[11 * 2];
		for (int i = 0; i < 11; i++)
		{
			replacements[i * 2] = "%sub" + i + '%';
			if (certifications.length <= i)
			{
				replacements[i * 2 + 1] = "<br>";
			}
			else
			{
				final String[] button = certifications[i];
				replacements[i * 2 + 1] = "<button value=\"" + button[0] + "\" action=\"bypass -h _bbsservice:giveCertificate " + button[1] + "\" width=300 height=30 back=\"L2UI_CT1.ListCTRL_DF_Title_Down\" fore=\"L2UI_CT1.ListCTRL_DF_Title\">";
			}
		}
		sendFileToPlayer(player, "data/html/community/subclass/subclassChanger_select_cert.htm", replacements);
	}
	
	private static void cancelSubPage(Player player)
	{
		final List<SubClass> subToChoose = new ArrayList<>();
		for (final var sub : player.getSubClasses().values())
		{
			if (sub.getClassId() != player.getBaseClass())
			{
				subToChoose.add(sub);
			}
		}
		final String[] replacements = new String[11 * 2];
		for (int i = 0; i < 11; i++)
		{
			replacements[i * 2] = "%sub" + i + '%';
			if (subToChoose.size() <= i)
			{
				replacements[i * 2 + 1] = "<br>";
			}
			else
			{
				final var playerClass = subToChoose.get(i);
				replacements[i * 2 + 1] = "<button value=\"" + Util.className(player, playerClass.getClassId()) + "\" action=\"bypass -h _bbsservice:selectCancelSub " + playerClass.getClassIndex() + "\" width=200 height=30 back=\"L2UI_CT1.OlympiadWnd_DF_Fight1None_Down\" fore=\"L2UI_ct1.OlympiadWnd_DF_Fight1None\">";
			}
		}
		sendFileToPlayer(player, "data/html/community/subclass/subclassChanger_select_remove.htm", replacements);
	}

	private static void changeSub(Player player, int subId)
	{
		if (!canChangeClass(player))
		{
			return;
		}
		player.setActiveClass(subId);
		player.sendPacket(SystemMessageId.SUBCLASS_TRANSFER_COMPLETED);
		player.sendPacket(new ShowBoard());
	}

	private static void changeSubPage(Player player)
	{
		final Collection<SubClass> allSubs = player.getSubClasses().values();
		final List<Integer> classId = new ArrayList<>();
		final List<Integer> classIndex = new ArrayList<>();
		if (player.getActiveClass() != player.getBaseClass())
		{
			classId.add(player.getBaseClass());
			classIndex.add(0);
		}
		for (final var sub : allSubs)
		{
			if (sub.getClassId() != player.getActiveClass())
			{
				classId.add(sub.getClassId());
				classIndex.add(sub.getClassIndex());
			}
		}
		
		final String[] replacements = new String[11 * 2];
		for (int i = 0; i < 11; i++)
		{
			replacements[i * 2] = "%sub" + i + '%';
			
			if (classId.size() <= i)
			{
				replacements[i * 2 + 1] = "<br>";
			}
			else
			{
				final var playerClassId = classId.get(i);
				final var playerClassIndex = classIndex.get(i);
				replacements[i * 2 + 1] = "<button value=\"" + Util.className(player, playerClassId) + "\" action=\"bypass -h _bbsservice:changeSubTo " + playerClassIndex + "\" width=200 height=30 back=\"L2UI_CT1.OlympiadWnd_DF_Fight1None_Down\" fore=\"L2UI_ct1.OlympiadWnd_DF_Fight1None\">";
			}
		}
		sendFileToPlayer(player, "data/html/community/subclass/subclassChanger_select_change.htm", replacements);
	}

	public void addNewSub(Player player, int subclassId)
	{
		if (!canChangeClass(player))
		{
			return;
		}
		
		final var subToRemove = player.getQuickVarI("SubToRemove");
		boolean added;
		if (subToRemove > 0)
		{
			if (player.getQuickVarI("SubRemoveCert", 0) > 0)
			{
				checkCertificafication(player, subToRemove, false);
				player.deleteQuickVar("SubRemoveCert");
				
			}
			added = player.modifySubClass(subToRemove, subclassId);
			if (added)
			{
				player.abortCast();
				player.stopAllEffectsExceptThoseThatLastThroughDeath();
				player.stopAllEffectsNotStayOnSubclassChange();
				player.stopCubics(true);
				player.setActiveClass(subToRemove);
				InitialShortcutParser.getInstance().registerAllShortcuts(player);
				player.sendPacket(new ShortCutInit(player));
				player.deleteQuickVar("SubToRemove");
			}
			else
			{
				player.setActiveClass(0);
				player.sendMessage((new ServerMessage("CommunityGeneral.CANT_ADD_SUB", player.getLang())).toString());
				return;
			}
		}
		else
		{
			added = addNewSubclass(player, subclassId);
		}
		
		if (added)
		{
			player.sendPacket(SystemMessageId.ADD_NEW_SUBCLASS);
		}
		else
		{
			player.sendMessage((new ServerMessage("CommunityGeneral.CANT_ADD_SUB1", player.getLang())).toString());
		}
		player.sendPacket(new ShowBoard());
	}
	
	private void checkCertificafication(Player player, int classIndex, boolean restoreItems)
	{
		var st = player.getQuestState("SubClassSkills");
		if (st == null)
		{
			final var subClassSkilllsQuest = QuestManager.getInstance().getQuest("SubClassSkills");
			if (subClassSkilllsQuest != null)
			{
				st = subClassSkilllsQuest.newQuestState(player);
			}
			else
			{
				return;
			}
		}
		
		if (!player.isInventoryUnder90(false))
		{
			player.sendPacket(SystemMessageId.INVENTORY_LESS_THAN_80_PERCENT);
			return;
		}
		
		if (st != null)
		{
			var activeCertifications = 0;
			for (final var varName : SUB_VAR_NAMES)
			{
				final var qvar = st.getGlobalQuestVar(varName + classIndex);
				if (!qvar.isEmpty() && (qvar.endsWith(";") || !qvar.equals("0")))
				{
					activeCertifications++;
				}
			}
			
			if (activeCertifications > 0)
			{
				for (final var varName : SUB_VAR_NAMES)
				{
					final var qvarName = varName + classIndex;
					final var qvar = st.getGlobalQuestVar(qvarName);
					if (qvar.endsWith(";"))
					{
						final var skillIdVar = qvar.replace(";", "");
						if (Util.isDigit(skillIdVar))
						{
							final var sk = player.getKnownSkill(Integer.parseInt(skillIdVar));
							if (sk != null)
							{
								if (restoreItems)
								{
									for (int i = 1; i <= sk.getLevel(); i++)
    								{
										final var sl = selectSkill(sk.getId(), sk.getLevel());
										if (sl != null)
										{
											for (final var item : sl.getRequiredItems(AcquireSkillType.SUBCLASS))
											{
												if (item != null)
												{
													player.addItem("Return Book", item.getId(), item.getCount(), null, true);
												}
											}
										}
    								}
								}
								player.removeSkill(sk, true);
								st.saveGlobalQuestVar(qvarName, "0");
							}
							else
							{
								st.saveGlobalQuestVar(qvarName, "0");
							}
						}
					}
					else if (!qvar.isEmpty() && !qvar.equals("0"))
					{
						if (Util.isDigit(qvar))
						{
							final var itemObjId = Integer.parseInt(qvar);
							var itemInstance = player.getInventory().getItemByObjectId(itemObjId);
							if (itemInstance != null)
							{
								player.destroyItem("CancelCertification", itemObjId, 1, player, false);
							}
							else
							{
								itemInstance = player.getWarehouse().getItemByObjectId(itemObjId);
								if (itemInstance != null)
								{
									player.getWarehouse().destroyItem("CancelCertification", itemInstance, 1, player, false);
								}
							}
							st.saveGlobalQuestVar(qvarName, "0");
						}
					}
				}
			}
		}
	}

	private static boolean addNewSubclass(Player player, int classId)
	{
		if (player.getTotalSubClasses() >= Config.MAX_SUBCLASS)
		{
			return false;
		}

		if (player.getLevel() < 75)
		{
			return false;
		}

		if (!player.getSubClasses().isEmpty())
		{
			for (final Iterator<SubClass> subList = iterSubClasses(player); subList.hasNext();)
			{
				final var subClass = subList.next();

				if (subClass.getLevel() < 75)
				{
					return false;
				}
			}
		}

		if (!Config.ALT_GAME_SUBCLASS_WITHOUT_QUESTS)
		{
			checkQuests(player);
		}

		if (isValidNewSubClass(player, classId))
		{
			if (!player.addSubClass(classId, player.getTotalSubClasses() + 1))
			{
				return false;
			}
			else
			{
				player.setActiveClass(player.getTotalSubClasses());
				InitialShortcutParser.getInstance().registerAllShortcuts(player);
				player.sendPacket(new ShortCutInit(player));
			}
		}
		return true;
	}

	protected static boolean checkQuests(Player player)
	{
		if (player.isNoble() || player.getVarInt("subclassBuy", 0) > 0)
		{
			return true;
		}

		var qs = player.getQuestState("_234_FatesWhisper");
		if ((qs == null) || !qs.isCompleted())
		{
			return false;
		}

		qs = player.getQuestState("_235_MimirsElixir");
		if ((qs == null) || !qs.isCompleted())
		{
			return false;
		}
		return true;
	}

	private static boolean isValidNewSubClass(Player player, int classId)
	{
		final var cid = ClassId.values()[classId];
		SubClass sub;
		ClassId subClassId;
		for (final Iterator<SubClass> subList = iterSubClasses(player); subList.hasNext();)
		{
			sub = subList.next();
			subClassId = ClassId.values()[sub.getClassId()];

			if (subClassId.equalsOrChildOf(cid))
			{
				return false;
			}
		}
		final var currentBaseId = player.getBaseClass();
		final var baseCID = ClassId.getClassId(currentBaseId);
		final int baseClassId;

		if (baseCID.level() > 2)
		{
			baseClassId = baseCID.getParent().ordinal();
		}
		else
		{
			baseClassId = currentBaseId;
		}

		final Set<PlayerClass> availSubs = PlayerClass.values()[baseClassId].getAvailableSubclasses(player);
		if ((availSubs == null) || availSubs.isEmpty())
		{
			return false;
		}

		var found = false;
		for (final var pclass : availSubs)
		{
			if (pclass.ordinal() == classId)
			{
				found = true;
				break;
			}
		}
		return found;
	}

	private static boolean canChangeClass(Player player)
	{
		if (player.hasSummon())
		{
			player.sendPacket(SystemMessageId.CANT_SUBCLASS_WITH_SUMMONED_SERVITOR);
			return false;
		}
		
		if (player.isCastingNow() || player.isAllSkillsDisabled() || player.getTransformation() != null)
		{
			player.sendPacket(SystemMessageId.SUBCLASS_NO_CHANGE_OR_CREATE_WHILE_SKILL_IN_USE);
			return false;
		}
		
		if (!player.isInsideZone(ZoneId.PEACE) || player.getDuelState() != Duel.DUELSTATE_NODUEL)
		{
			player.sendMessage((new ServerMessage("CommunityGeneral.CANT_SUB", player.getLang())).toString());
			return false;
		}
		return true;
	}
	
	private static boolean canChangeMainClass(Player player)
	{
		if (player.hasSummon() || player.getPvpFlag() > 0 || player.isInCombat() || player.isCastingNow() || player.isAttackingNow() || player.isAllSkillsDisabled() || player.getTransformation() != null)
		{
			player.sendMessage((new ServerMessage("Community.ALL_DISABLE", player.getLang())).toString());
			return false;
		}
		
		if (player.checkInTournament() || player.isInFightEvent() || player.isInKrateisCube() || player.getUCState() > 0 || player.isBlocked() || player.isCursedWeaponEquipped() || player.isInDuel() || player.isFlying() || player.isJailed() || player.isInOlympiadMode() || player.inObserverMode() || player.isAlikeDead() || player.isDead())
		{
			player.sendMessage((new ServerMessage("Community.ALL_DISABLE", player.getLang())).toString());
			return false;
		}
		
		if (!player.isInsideZone(ZoneId.PEACE) || player.getDuelState() != Duel.DUELSTATE_NODUEL)
		{
			player.sendMessage((new ServerMessage("Community.ALL_DISABLE", player.getLang())).toString());
			return false;
		}
		return true;
	}
	
	private static void addNewSubPage(Player player, String raceName)
	{
		final var race = Race.valueOf(raceName);
		Set<PlayerClass> allSubs = getAvailableSubClasses(player);
		if (allSubs == null)
		{
			return;
		}
		allSubs = getSubsByRace(allSubs, race);
		
		PlayerClass[] arraySubs = new PlayerClass[allSubs.size()];
		arraySubs = allSubs.toArray(arraySubs);
		
		final String[] replacements = new String[11 * 2];
		for (int i = 0; i < 11; i++)
		{
			replacements[i * 2] = "%sub" + i + '%';
			if (arraySubs.length <= i)
			{
				replacements[i * 2 + 1] = "<br>";
			}
			else
			{
				final PlayerClass playerClass = arraySubs[i];
				replacements[i * 2 + 1] = "<button value=\"" + Util.className(player, playerClass.name()) + "\" action=\"bypass -h _bbsservice:addNewSub " + playerClass.ordinal() + "\" width=200 height=30 back=\"L2UI_CT1.OlympiadWnd_DF_Fight1None_Down\" fore=\"L2UI_ct1.OlympiadWnd_DF_Fight1None\">";
			}
		}
		sendFileToPlayer(player, "data/html/community/subclass/subclassChanger_select_add.htm", replacements);
	}
	
	private final static Set<PlayerClass> getAvailableSubClasses(Player player)
	{
		final var currentBaseId = player.getBaseClass();
		final var baseCID = ClassId.getClassId(currentBaseId);
		final int baseClassId;
		
		if (baseCID.level() > 2)
		{
			baseClassId = baseCID.getParent().ordinal();
		}
		else
		{
			baseClassId = currentBaseId;
		}
		
		final Set<PlayerClass> availSubs = PlayerClass.values()[baseClassId].getAvailableSubclasses(player);
		
		if ((availSubs != null) && !availSubs.isEmpty())
		{
			for (final Iterator<PlayerClass> availSub = availSubs.iterator(); availSub.hasNext();)
			{
				final var pclass = availSub.next();
				
				final var availClassId = pclass.ordinal();
				final var cid = ClassId.getClassId(availClassId);
				SubClass prevSubClass;
				ClassId subClassId;
				for (final Iterator<SubClass> subList = iterSubClasses(player); subList.hasNext();)
				{
					prevSubClass = subList.next();
					subClassId = ClassId.getClassId(prevSubClass.getClassId());
					
					if (subClassId.equalsOrChildOf(cid))
					{
						availSub.remove();
						break;
					}
				}
			}
		}
		return availSubs;
	}
	
	private static final Iterator<SubClass> iterSubClasses(Player player)
	{
		return player.getSubClasses().values().iterator();
	}

	private static Set<PlayerClass> getSubsByRace(Set<PlayerClass> allSubs, Race race)
	{
		for (final var sub : allSubs)
		{
			if (sub != null)
			{
				if (!sub.isOfRace(race))
				{
					allSubs.remove(sub);
				}
			}
		}
		return allSubs;
	}

	private boolean isValidCloak(int paramInt)
	{
		final int[] arrayOfInt1 =
		{
		        21719, 21720, 21721
		};
		for (final int k : arrayOfInt1)
		{
			if (paramInt == k)
			{
				return true;
			}
		}
		return false;
	}

	private NpcHtmlMessage build(Player player, NpcHtmlMessage html, Donation donate, int att_1, int att_2, int att_3)
	{
		final var slotclose = "<img src=\"L2UI_CT1.ItemWindow_DF_SlotBox_Disable\" width=\"32\" height=\"32\">";
		final var id = donate.getId();
		final var size = donate.getAttribution().getSize();
		var block = false;
		if (size == 1 && (att_1 != -1 || att_2 != -1 || att_3 != -1))
		{
			block = true;
		}
		else if (size == 2 && (att_1 != -1 || att_2 != -1) && (att_1 != -1 || att_3 != -1) && (att_2 != -1 || att_3 != -1))
		{
			block = true;
		}
		else if (size == 3 && att_1 != -1 && att_2 != -1 && att_3 != -1)
		{
			block = true;
		}
		
		final var one = block(player, 0, 1) || block;
		final var fire = one ? slotclose : button(0, id);
		final var water = one ? slotclose : button(1, id);
		final var two = block(player, 2, 3) || block;
		final var wind = two ? slotclose : button(2, id);
		final var earth = two ? slotclose : button(3, id);
		final var three = block(player, 4, 5) || block;
		final var holy = three ? slotclose : button(4, id);
		final var unholy = three ? slotclose : button(5, id);
		html.replace("%fire%", fire);
		html.replace("%water%", water);
		html.replace("%wind%", wind);
		html.replace("%earth%", earth);
		html.replace("%holy%", holy);
		html.replace("%unholy%", unholy);
		
		return html;
	}

	private String button(int att, int id)
	{
		return "<button action=\"bypass -h _bbsservice:donatePut " + id + " " + att + "\" width=34 height=34 back=\"L2UI_CT1.ItemWindow_DF_Frame_Down\" fore=\"L2UI_CT1.ItemWindow_DF_Frame\"/>";
	}

	private boolean block(Player player, int id, int id2)
	{
		for (int i = 1; i <= 3; i++)
		{
			final var var = player.getQuickVarI("att_" + i, -1);
			if (var == id || var == id2)
			{
				return true;
			}
		}
		return false;
	}

	private String elementName(Player player, int id)
	{
		var name = "";
		switch (id)
		{
			case 0 :
				name = "" + ServerStorage.getInstance().getString(player.getLang(), "ServiceBBS.ATTR_FIRE") + "";
				break;
			case 1 :
				name = "" + ServerStorage.getInstance().getString(player.getLang(), "ServiceBBS.ATTR_WATER") + "";
				break;
			case 2 :
				name = "" + ServerStorage.getInstance().getString(player.getLang(), "ServiceBBS.ATTR_WIND") + "";
				break;
			case 3 :
				name = "" + ServerStorage.getInstance().getString(player.getLang(), "ServiceBBS.ATTR_EARTH") + "";
				break;
			case 4 :
				name = "" + ServerStorage.getInstance().getString(player.getLang(), "ServiceBBS.ATTR_HOLY") + "";
				break;
			case 5 :
				name = "" + ServerStorage.getInstance().getString(player.getLang(), "ServiceBBS.ATTR_DARK") + "";
				break;
			default :
				name = "NONE";
				break;
		}
		return name;
	}
	
	private boolean isVar(Player player, String var)
	{
		return player.getQuickVarI(var, 0) != 0;
	}
	
	private boolean checkAttVars(Player player, int size)
	{
		var count = 0;

		for (int i = 1; i <= 3; i++)
		{
			final var var = player.getQuickVarI("att_" + i, -1);
			if (var != -1)
			{
				count++;
			}
		}
		return count != size;
	}

	private void updatePrice(Map<Integer, Long> price, int id, long count)
	{
		if (price.containsKey(id))
		{
			price.put(id, count + price.get(id));
		}
		else
		{
			price.put(id, count);
		}
	}
	
	private void removeVars(Player player)
	{
		for (final var var : _vars)
		{
			player.deleteQuickVar(var);
		}
		for (int i = 1; i <= 3; i++)
		{
			player.deleteQuickVar("att_" + i);
		}
	}

	private void cleanAtt(Player player, int exclude)
	{
		if (player == null)
		{
			return;
		}
		for (final var att : Elemental.VALUES)
		{
			if (att.getId() == exclude)
			{
				player.deleteQuickVar("ex_att_" + att.getId());
			}
		}
		
		if (exclude == -1)
		{
			for (final var att : Elemental.VALUES)
			{
				player.deleteQuickVar("ex_att_" + att.getId());
			}
			player.deleteQuickVar("ex_att");
		}
	}
	
	private void removeVars(Player player, boolean exchange)
	{
		if (player == null)
		{
			return;
		}

		if (exchange)
		{
			player.deleteQuickVar("exchange");
		}
		player.deleteQuickVar("exchange_obj");
		player.deleteQuickVar("exchange_new");
		player.deleteQuickVar("exchange_attribute");
	}
	
	protected void showMainMenu(Player player, int _page, AugmentationFilter _filter)
	{
		if (_page < 1)
		{
			final var adminReply = new NpcHtmlMessage(5);
			adminReply.setFile(player, player.getLang(), "data/html/community/augmentations/index.htm");
			player.sendPacket(adminReply);
			return;
		}

		final Map<Integer, Options> _augments = new HashMap<>();
		final Collection<Options> augmentations = Config.SERVICES_AUGMENTATION_FORMATE ? OptionsParser.getInstance().getUniqueAvailableOptions(_filter) : OptionsParser.getInstance().getUniqueOptions(_filter);
		int counts = 0;

		if (augmentations.isEmpty())
		{
			showMainMenu(player, 0, AugmentationFilter.NONE);
			player.sendMessage((new ServerMessage("ServiceBBS.AUGMENT_EMPTY", player.getLang())).toString());
			return;
		}

		final var adminReply = new NpcHtmlMessage(0);
		adminReply.setFile(player, player.getLang(), "data/html/community/augmentations/list.htm");
		final var template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/augmentations/template.htm");

		for (final var augm : augmentations)
		{
			if (augm != null)
			{
				counts++;
				_augments.put(counts, augm);
			}
		}
		var block = "";
		var list = "";

		final var perpage = 6;
		final var isThereNextPage = _augments.size() > perpage;
		var count = 0;
		var lastColor = true;

		for (int i = (_page - 1) * perpage; i < _augments.size(); i++)
		{
			final var augm = _augments.get(i + 1);
			if (augm != null)
			{
				Skill skill = null;
				if (augm.hasActiveSkill())
				{
					skill = augm.getActiveSkill().getSkill();
				}
				else if (augm.hasPassiveSkill())
				{
					skill = augm.getPassiveSkill().getSkill();
				}
				else if (augm.hasActivationSkills())
				{
					skill = augm.getActivationsSkills().get(0).getSkill();
				}
				
				block = template;
				block = block.replace("{bypass}", "bypass -h _bbsservice:augmentationPut " + augm.getId() + " " + _page + " " + (_filter.ordinal() + 1));
				var name = "";
				if (skill != null)
				{
					name = skill.getName(player.getLang()).length() > 30 ? skill.getName(player.getLang()).substring(0, 30) : skill.getName(player.getLang());
				}
				else
				{
					switch (augm.getId())
					{
						case 16341 :
							name = "+1 " + ServerStorage.getInstance().getString(player.getLang(), "ServiceBBS.STR") + "";
							break;
						case 16342 :
							name = "+1 " + ServerStorage.getInstance().getString(player.getLang(), "ServiceBBS.CON") + "";
							break;
						case 16343 :
							name = "+1 " + ServerStorage.getInstance().getString(player.getLang(), "ServiceBBS.INT") + "";
							break;
						case 16344 :
							name = "+1 " + ServerStorage.getInstance().getString(player.getLang(), "ServiceBBS.MEN") + "";
							break;
						default :
							name = "(Id:" + augm.getId() + ")";
					}
				}
				
				block = block.replace("{name}", name);
				block = block.replace("{icon}", skill != null ? skill.getIcon() : "icon.skill5041");
				block = block.replace("{color}", lastColor ? "222222" : "333333");
				block = block.replace("{price}", Util.formatAdena(Config.SERVICES_AUGMENTATION_ITEM[1]) + " " + ItemsParser.getInstance().getTemplate(Config.SERVICES_AUGMENTATION_ITEM[0]).getName(player.getLang()));
				list += block;

				lastColor = !lastColor;
				count++;
				if (count >= perpage)
				{
					break;
				}
			}
		}

		final var pages = (double) _augments.size() / perpage;
		final var countss = (int) Math.ceil(pages);
		adminReply.replace("%pages%", Util.getNavigationBlock(countss, _page, _augments.size(), perpage, isThereNextPage, "_bbsservice:augmentationPage " + (_filter.ordinal() + 1) + " %s"));
		adminReply.replace("%augs%", list);
		player.sendPacket(adminReply);
	}
	
	protected boolean checkItemType(ItemInstance item)
	{
		if (item.isHeroItem() || item.isShadowItem() || item.isCommonItem())
		{
			return false;
		}
		
		switch (item.getId())
		{
			case 13752 :
			case 13753 :
			case 13754 :
			case 13755 :
				return false;
		}
		
		if (item.isPvp() && !AugmentationParser.getInstance().getParams().getBool("allowAugmentationPvpItems"))
		{
			return false;
		}
		
		if (item.getItem().getCrystalType() < Item.CRYSTAL_C && !AugmentationParser.getInstance().getParams().getBool("allowAugmentationAllItemsGrade"))
		{
			return false;
		}
		
		switch (((Weapon) item.getItem()).getItemType())
		{
			case NONE :
			case FISHINGROD :
				return false;
			default :
				break;
		}
		return true;
	}

	protected void checkClanSkills(Player player, int page)
	{
		final Map<Integer, SkillLearn> _skills = new HashMap<>();
		final List<SkillLearn> skills = SkillTreesParser.getInstance().getAvailablePledgeSkills(player.getClan());
		int counts = 0;
		
		for (final var skill : skills)
		{
			if (skill != null)
			{
				counts++;
				_skills.put(counts, skill);
			}
		}

		final var perpage = 6;
		final var isThereNextPage = _skills.size() > perpage;

		final var html = new NpcHtmlMessage(5);
		html.setFile(player, player.getLang(), "data/html/community/donate/clanskills.htm");
		final String template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/donate/clanskills-template.htm");
		
		var block = "";
		var list = "";
		
		var countss = 0;

		for (int i = (page - 1) * perpage; i < _skills.size(); i++)
		{
			final var skill = _skills.get(i + 1);
			if (skill != null)
			{
				block = template;
				
				final var skillId = Integer.toString(skill.getId());
				String icon;
				if (skillId.length() == 3)
				{
					icon = 0 + skillId;
				}
				else
				{
					icon = skillId;
				}
				block = block.replace("%bypassBuy%", "bypass -h _bbslearnclanskills " + skill.getId() + " " + skill.getLvl() + " " + page);
				block = block.replace("%name%", SkillsParser.getInstance().getInfo(skill.getId(), skill.getLvl()).getName(player.getLang()));
				block = block.replace("%price%", Util.formatPay(player, Config.SERVICES_CLANSKILLS_ITEM[1], Config.SERVICES_CLANSKILLS_ITEM[0]));
				block = block.replace("%icon%", icon);
				list += block;
				countss++;

				if (countss >= perpage)
				{
					break;
				}
			}
		}

		final var pages = (double) _skills.size() / perpage;
		final var count = (int) Math.ceil(pages);
		if (counts == 0)
		{
			if (player.getClan().getLevel() < 8)
			{
				final var sm = SystemMessage.getSystemMessage(SystemMessageId.DO_NOT_HAVE_FURTHER_SKILLS_TO_LEARN_S1);
				if (player.getClan().getLevel() < 5)
				{
					sm.addNumber(5);
				}
				else
				{
					sm.addNumber(player.getClan().getLevel() + 1);
				}
				player.sendPacket(sm);
			}
			else
			{
				player.sendMessage((new ServerMessage("ServiceBBS.ALL_CLAN_SKILLS", player.getLang())).toString());
			}
			return;
		}

		html.replace("%list%", list);
		html.replace("%navigation%", Util.getNavigationBlock(count, page, _skills.size(), perpage, isThereNextPage, "_bbslistclanskills %s"));
		player.sendPacket(html);
	}

	protected boolean unbanChar(Player activeChar, String affect, String key)
	{
		var sucsess = false;
		final var af = PunishmentAffect.getByName(affect);

		switch (af)
		{
			case CHARACTER :
				sucsess = unbanCharById(activeChar, key);
				break;
			case ACCOUNT :
				sucsess = unbanCharByAcc(activeChar, key);
				break;
			case IP :
				sucsess = unbanCharByIP(activeChar, key);
				break;
		}
		return sucsess;
	}

	protected boolean unbanCharByIP(Player activeChar, String ip)
	{
		var bool = false;

		String ipAddress = null;

		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT `lastIP` FROM `accounts` WHERE `lastIP` = ?");
			statement.setString(1, ip);
			rset = statement.executeQuery();
			while (rset.next())
			{
				ipAddress = rset.getString("lastIP");
			}
		}
		catch (final Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
		
		if (ipAddress != null)
		{
			bool = finishUnban(activeChar, ipAddress, PunishmentAffect.IP);
		}
		return bool;
	}

	protected boolean unbanCharByAcc(Player activeChar, String key)
	{
		var bool = false;

		String accName = null;

		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT account_name FROM `characters` WHERE `account_name` = ?");
			statement.setString(1, key);
			rset = statement.executeQuery();
			while (rset.next())
			{
				accName = rset.getString("account_name");
			}
		}
		catch (final Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
		
		if (accName != null)
		{
			bool = finishUnban(activeChar, accName, PunishmentAffect.ACCOUNT);
		}
		return bool;
	}

	protected boolean unbanCharById(Player activeChar, String key)
	{
		var bool = false;

		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT charId FROM characters WHERE char_name = ?");
			statement.setString(1, key);
			rset = statement.executeQuery();
			while (rset.next())
			{
				final var charId = rset.getString("charId");

				bool = finishUnban(activeChar, charId, PunishmentAffect.CHARACTER);
			}
		}
		catch (final Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
		return bool;
	}

	protected boolean finishUnban(Player activeChar, String dbKey, PunishmentAffect param)
	{
		var sucsess = false;
		Connection con = null;
		Statement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.createStatement();
			rset = statement.executeQuery("SELECT * FROM punishments");
			while (rset.next())
			{
				final var key = rset.getString("key");
				final var affect = PunishmentAffect.getByName(rset.getString("affect"));
				final var type = PunishmentType.getByName(rset.getString("type"));
				final var reason = rset.getString("reason");
				if ((type != null) && (affect != null && affect == param))
				{
					if (key.equals(dbKey))
					{
						rset.getInt("id");
						if (PunishmentManager.getInstance().clearPunishment(key, type, affect))
						{
							ServerMessage msg = null;
							switch (affect)
							{
								case CHARACTER :
									msg = new ServerMessage("ServiceBBS.FIND_CHAR", activeChar.getLang());
									msg.add(key);
									msg.add(reason);
									sucsess = true;
									break;
								case ACCOUNT :
									msg = new ServerMessage("ServiceBBS.FIND_ACC", activeChar.getLang());
									msg.add(key);
									msg.add(reason);
									sucsess = true;
									break;
								case IP :
									msg = new ServerMessage("ServiceBBS.FIND_IP", activeChar.getLang());
									msg.add(key);
									msg.add(reason);
									sucsess = true;
									break;
							}
							activeChar.sendMessage(msg.toString());
						}
					}
				}
			}
		}
		catch (final Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
		return sucsess;
	}

	protected boolean deleteSupport(Player activeChar, String banChar, PunishmentAffect affect, int id)
	{
		var sucsess = false;
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("DELETE FROM punishments WHERE id=?");
			statement.setInt(1, id);
			statement.execute();
			
			ServerMessage msg = null;
			switch (affect)
			{
				case CHARACTER :
					msg = new ServerMessage("ServiceBBS.UNBAN_CHAR", activeChar.getLang());
				case ACCOUNT :
					msg = new ServerMessage("ServiceBBS.UNBAN_ACC", activeChar.getLang());
				case IP :
					msg = new ServerMessage("ServiceBBS.UNBAN_IP", activeChar.getLang());
			}
			msg.add(banChar);
			activeChar.sendMessage(msg.toString());
			sucsess = true;
		}
		catch (final SQLException e)
		{
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
		return sucsess;
	}

	protected void checkPremiumList(Player activeChar, int page)
	{
		final var html = new NpcHtmlMessage(5);
		html.setFile(activeChar, activeChar.getLang(), "data/html/community/donate/premiumList.htm");
		final var template = HtmCache.getInstance().getHtm(activeChar, activeChar.getLang(), "data/html/community/donate/premium-template.htm");
		var block = "";
		var list = "";
		
		final var perpage = 6;
		var counter = 0;
		final var isThereNextPage = Config.SERVICES_PREMIUM_VALID_ID.length > perpage;
		
		for (int i = (page - 1) * perpage; i < Config.SERVICES_PREMIUM_VALID_ID.length; i++)
		{
			final var tpl = PremiumAccountsParser.getInstance().getPremiumTemplate(Config.SERVICES_PREMIUM_VALID_ID[i]);
			if (tpl != null)
			{
				block = template;
				block = block.replace("%name%", tpl.getName(activeChar.getLang()));
				block = block.replace("%icon%", tpl.getIcon());
				block = block.replace("%time%", TimeUtils.formatTime(activeChar, (int) tpl.getTime()));
				var priceLine = "<font color=99CC66>Cost:</font> ";
				for (final var price : tpl.getPriceList())
				{
					if (price != null)
					{
						priceLine += "" + Util.formatPay(activeChar, price.getCount(), price.getId()) + " ";
					}
				}
				block = block.replace("%price%", priceLine);
				block = block.replace("%link%", "bypass -h _bbspremium " + tpl.getId() + " " + page + " " + 0);
				list += block;
				
				counter++;
				
				if (counter >= perpage)
				{
					break;
				}
			}
		}
		
		final var pages = (double) Config.SERVICES_PREMIUM_VALID_ID.length / perpage;
		final var count = (int) Math.ceil(pages);
		html.replace("%list%", list);
		html.replace("%navigation%", Util.getNavigationBlock(count, page, Config.SERVICES_PREMIUM_VALID_ID.length, perpage, isThereNextPage, "_bbspremiumList %s"));
		activeChar.sendPacket(html);
	}
	
	protected void checkFullPremiumList(Player activeChar, int page)
	{
		var html = HtmCache.getInstance().getHtm(activeChar, activeChar.getLang(), "data/html/community/donate/buyPremium.htm");
		final var template = HtmCache.getInstance().getHtm(activeChar, activeChar.getLang(), "data/html/community/donate/buyPremium-template.htm");
		var block = "";
		var list = "";
		
		final var perpage = 10;
		var counter = 0;
		final var isThereNextPage = Config.SERVICES_PREMIUM_VALID_ID.length > perpage;
		var countt = 0;
		for (int i = (page - 1) * perpage; i < Config.SERVICES_PREMIUM_VALID_ID.length; i++)
		{
			final var tpl = PremiumAccountsParser.getInstance().getPremiumTemplate(Config.SERVICES_PREMIUM_VALID_ID[i]);
			if (tpl != null)
			{
				block = template;
				block = block.replace("%name%", tpl.getName(activeChar.getLang()));
				block = block.replace("%icon%", tpl.getIcon());
				block = block.replace("%time%", TimeUtils.formatTime(activeChar, (int) tpl.getTime()));
				var priceLine = "<font color=99CC66>Cost:</font> ";
				for (final var price : tpl.getPriceList())
				{
					if (price != null)
					{
						priceLine += "" + Util.formatPay(activeChar, price.getCount(), price.getId()) + " ";
					}
				}
				block = block.replace("%price%", priceLine);
				block = block.replace("%link%", "bypass -h _bbspremium " + tpl.getId() + " " + page + " " + 1);
				
				countt++;
				if (countt == 2)
				{
					block += "</tr><tr><td><br></td></tr><tr>";
					countt = 0;
				}
				
				list += block;
				
				counter++;
				
				if (counter >= perpage)
				{
					break;
				}
			}
		}
		
		final var pages = (double) Config.SERVICES_PREMIUM_VALID_ID.length / perpage;
		final var count = (int) Math.ceil(pages);
		html = html.replace("%list%", list);
		html = html.replace("%navigation%", Util.getNavigationBlock(count, page, Config.SERVICES_PREMIUM_VALID_ID.length, perpage, isThereNextPage, "_bbspremiumPage %s"));
		separateAndSend(html, activeChar);
	}
	
	protected void checkFullPremiumListOnly(Player activeChar, int page)
	{
		var html = HtmCache.getInstance().getHtm(activeChar, activeChar.getLang(), "data/html/community/donate/buyPremium.htm");
		final var template = HtmCache.getInstance().getHtm(activeChar, activeChar.getLang(), "data/html/community/donate/buyPremium-template.htm");
		String block = "";
		String list = "";
		
		final var perpage = 10;
		var counter = 0;
		final var isThereNextPage = Config.SERVICES_PREMIUM_VALID_ID.length > perpage;
		var countt = 0;
		for (int i = (page - 1) * perpage; i < Config.SERVICES_PREMIUM_VALID_ID.length; i++)
		{
			final var tpl = PremiumAccountsParser.getInstance().getPremiumTemplate(Config.SERVICES_PREMIUM_VALID_ID[i]);
			if (tpl != null)
			{
				block = template;
				block = block.replace("%name%", tpl.getName(activeChar.getLang()));
				block = block.replace("%icon%", tpl.getIcon());
				block = block.replace("%time%", TimeUtils.formatTime(activeChar, (int) tpl.getTime()));
				var priceLine = "<font color=99CC66>Cost:</font> ";
				for (final var price : tpl.getPriceList())
				{
					if (price != null)
					{
						priceLine += "" + Util.formatPay(activeChar, price.getCount(), price.getId()) + " ";
					}
				}
				block = block.replace("%price%", priceLine);
				block = block.replace("%link%", "bypass -h _bbspremium " + tpl.getId() + " " + page + " " + 2);
				
				countt++;
				if (countt == 2)
				{
					block += "</tr><tr><td><br></td></tr><tr>";
					countt = 0;
				}
				
				list += block;
				
				counter++;
				
				if (counter >= perpage)
				{
					break;
				}
			}
		}
		
		final var pages = (double) Config.SERVICES_PREMIUM_VALID_ID.length / perpage;
		final var count = (int) Math.ceil(pages);
		html = html.replace("%list%", list);
		html = html.replace("%navigation%", Util.getNavigationBlock(count, page, Config.SERVICES_PREMIUM_VALID_ID.length, perpage, isThereNextPage, "_bbspremiumOnlyPage %s"));
		separateAndSend(html, activeChar);
	}

	protected void checkPremium(Player activeChar, int id, int page, int typeInfo)
	{
		if (!Config.USE_PREMIUMSERVICE)
		{
			activeChar.sendMessage((new ServerMessage("Community.ALL_DISABLE", activeChar.getLang())).toString());
			return;
		}
		
		if (activeChar.hasPremiumBonus() && !Config.PREMIUMSERVICE_DOUBLE)
		{
			activeChar.sendMessage((new ServerMessage("ServiceBBS.PREMIUM_MSG", activeChar.getLang())).toString());
			return;
		}
		
		final var template = PremiumAccountsParser.getInstance().getPremiumTemplate(id);
		if (template != null)
		{
			for (final var price : template.getPriceList())
			{
				if (price != null)
				{
					if (activeChar.getInventory().getItemByItemId(price.getId()) == null)
					{
						activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
						if (typeInfo == 0)
						{
							checkPremiumList(activeChar, page);
						}
						else if (typeInfo == 1)
						{
							onBypassCommand("_bbspremiumPage " + page + "", activeChar);
						}
						else
						{
							onBypassCommand("_bbspremiumOnlyPage " + page + "", activeChar);
						}
						return;
					}
					
					if (activeChar.getInventory().getItemByItemId(price.getId()).getCount() < (price.getCount()))
					{
						activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
						if (typeInfo == 0)
						{
							checkPremiumList(activeChar, page);
						}
						else if (typeInfo == 1)
						{
							onBypassCommand("_bbspremiumPage " + page + "", activeChar);
						}
						else
						{
							onBypassCommand("_bbspremiumOnlyPage " + page + "", activeChar);
						}
						return;
					}
				}
			}
			
			for (final var price : template.getPriceList())
			{
				if (price != null)
				{
					activeChar.destroyItemByItemId("PremiumBBS", price.getId(), price.getCount(), activeChar, true);
				}
			}
			
			Util.addServiceLog(activeChar.getName(null) + " buy premium account template id: " + template.getId());
				
			final var time = !template.isOnlineType() ? (System.currentTimeMillis() + (template.getTime() * 1000)) : 0;
			if (template.isPersonal())
			{
				CharacterPremiumDAO.getInstance().updatePersonal(activeChar, id, time, template.isOnlineType());
			}
			else
			{
				CharacterPremiumDAO.getInstance().update(activeChar, id, time, template.isOnlineType());
			}
			
			if (activeChar.isInParty())
			{
				activeChar.getParty().recalculatePartyData();
			}
		}
		
		if (typeInfo == 0)
		{
			checkPremiumList(activeChar, page);
		}
		else if (typeInfo == 1)
		{
			onBypassCommand("_bbspremiumPage " + page + "", activeChar);
		}
		else
		{
			onBypassCommand("_bbspremiumOnlyPage " + page + "", activeChar);
		}
	}

	private static String uptime()
	{
		final var dataDateFormat = new SimpleDateFormat("hh:mm dd.MM.yyyy");
		return dataDateFormat.format(GameServer.server_started);
	}

	private String online(boolean off)
	{
		var i = 0;
		var j = 0;
		for (final var player : GameObjectsStorage.getPlayers())
		{
			i++;
			if (player.isInOfflineMode() || player.getPrivateStoreType() != Player.STORE_PRIVATE_NONE)
			{
				j++;
			}
		}
		return Util.formatAdena((long) (!off ? (i * Config.FAKE_ONLINE) + Config.FAKE_ONLINE_MULTIPLIER : j));
	}

	private static final DateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm");

	public static String time()
	{
		return TIME_FORMAT.format(new Date(System.currentTimeMillis()));
	}

	private String getTimeInServer(Player player)
	{
		final var h = GameTimeController.getInstance().getGameHour();
		final var m = GameTimeController.getInstance().getGameMinute();
		String strH;
		if (h < 10)
		{
			strH = "0" + h;
		}
		else
		{
			strH = "" + h;
		}
		String strM;
		if (m < 10)
		{
			strM = "0" + m;
		}
		else
		{
			strM = "" + m;
		}
		final var time = strH + ":" + strM;
		return time;
	}

	private String getStatus(Player player)
	{
		return player.hasPremiumBonus() ? "<font color=\"LEVEL\">" + ServerStorage.getInstance().getString(player.getLang(), "AccountBBSManager.PREMIUM") + "</font>" : "" + ServerStorage.getInstance().getString(player.getLang(), "AccountBBSManager.SIMPLE") + "";
	}
	
	private String getPremiumStatus(Player player, boolean isButton)
	{
		var line = "";
		if (player.hasPremiumBonus())
		{
			if (player.getPremiumBonus().isOnlineType())
			{
				final var template = PremiumAccountsParser.getInstance().getPremiumTemplate(player.getPremiumBonus().getPremiumId());
				if (template != null)
				{
					final var lastTime = ((template.getTime() * 1000L - (player.getPremiumBonus().getOnlineTime() + (System.currentTimeMillis() - player.getPremiumOnlineTime()))) / 1000L);
					line = "<font color=\"E6D0AE\">" + TimeUtils.formatTime(player, (int) lastTime, false) + "</font>";
				}
			}
			else
			{
				final var lastTime = (player.getPremiumBonus().getOnlineTime() - System.currentTimeMillis()) / 1000L;
				line = "<font color=\"E6D0AE\">" + TimeUtils.formatTime(player, (int) lastTime, false) + "</font>";
			}
		}
		else
		{
			if (isButton)
			{
				line = "<a action=\"bypass -h _bbspremiumPage\"><font color=\"ff6755\">" + ServerStorage.getInstance().getString(player.getLang(), "AccountBBSManager.SIMPLEE") + "</a></font>";
			}
		}
		return line;
	}
	
	private static void sendFileToPlayer(Player player, String path, String... replacements)
	{
		var html = HtmCache.getInstance().getHtm(player, player.getLang(), path);
		for (int i = 0; i < replacements.length; i += 2)
		{
			var toReplace = replacements[i + 1];
			if (toReplace == null)
			{
				toReplace = "<br>";
			}
			html = html.replace(replacements[i], toReplace);
		}
		separateAndSend(html, player);
	}
	
	public static final boolean correct(int level, boolean base)
	{
		return level <= (base ? ExperienceParser.getInstance().getMaxLevel() : Config.MAX_SUBCLASS_LEVEL);
	}
	
	public boolean activateSubClassAsBase(Player player, int subClassIndex, SubClass subClass)
	{
		if (!canChangeMainClass(player))
		{
			return false;
		}
		
		if (!_changeClassLock.tryLock())
		{
			return false;
		}
		
		
		final var baseClassId = player.getBaseClass();
		final var baseClassIndex = 0;
		checkCertificafication(player, subClassIndex, true);
		
		try
		{
			Connection con = null;
			try
			{
				con = DatabaseFactory.getInstance().getConnection();
				con.setAutoCommit(false);
				try
				{
					updateChar(con, player, subClass);
					updateCharSubClasses(con, player, subClass, baseClassId, subClassIndex);
					updateCharSkills(con, player, subClassIndex, baseClassIndex);
					updateCharSkillsSave(con, player, subClassIndex, baseClassIndex);
					updateCharSummonSkillsSave(con, player, subClassIndex, baseClassIndex);
					updateCharShortcuts(con, player, subClassIndex, baseClassIndex);
					updateCharHennas(con, player, subClassIndex, baseClassIndex);
					updateCharQuests(con, player, subClassIndex, baseClassIndex);
					updateCharRecipebook(con, player, subClassIndex, baseClassIndex);
					updateOlympNubles(con, player, subClass.getClassId(), baseClassId);
					updateOlympNublesEOM(con, player, subClass.getClassId(), baseClassId);
					
					con.commit();
					con.setAutoCommit(true);
					return true;
				}
				catch (final Exception exc)
				{
					con.rollback();
					con.setAutoCommit(true);
					_log.warn("Could not modify sub class for " + player.getName(null) + ": " + exc.getMessage(), exc);
				}
				
			}
			catch (final Exception exc)
			{
				_log.warn("Could not modify sub class for " + player.getName(null) + ": " + exc.getMessage(), exc);
			}
			finally
			{
				DbUtils.closeQuietly(con);
			}
		}
		finally
		{
			_changeClassLock.unlock();
		}
		return false;
	}
	
	private void updateChar(Connection con, Player player, SubClass subClass) throws SQLException
	{
		final var statement = con.prepareStatement("UPDATE characters SET classid = ?, base_class = ?, level = ?, exp = ?, sp = ? WHERE charId = ?");
		statement.setInt(1, subClass.getClassId());
		statement.setInt(2, subClass.getClassId());
		statement.setInt(3, subClass.getLevel());
		statement.setLong(4, subClass.getExp());
		statement.setLong(5, subClass.getSp());
		statement.setInt(6, player.getObjectId());
		statement.execute();
		statement.close();
	}
	
	private void updateCharSubClasses(Connection con, Player player, SubClass subClass, int baseClassId, int subClassIndex) throws SQLException
	{
		final var statement = con.prepareStatement("UPDATE character_subclasses SET class_id = ?, exp = ?, sp = ?, level = ? WHERE charId = ? AND class_id = ? AND class_index = ?");
		statement.setInt(1, baseClassId);
		statement.setLong(2, player.getExp());
		statement.setLong(3, player.getSp());
		statement.setInt(4, player.getLevel());
		statement.setInt(5, player.getObjectId());
		statement.setInt(6, subClass.getClassId());
		statement.setInt(7, subClassIndex);
		statement.execute();
		statement.close();
	}
	
	private void updateCharSkills(Connection con, Player player, int subClassIndex, int baseClassIndex) throws SQLException
	{
		final var sql = "UPDATE character_skills SET class_index = ? WHERE  charId= ? AND class_index = ?";
		final var tempClassIndex = 100;
		var statement = con.prepareStatement(sql);
		statement.setInt(1, tempClassIndex);
		statement.setInt(2, player.getObjectId());
		statement.setInt(3, baseClassIndex);
		statement.execute();
		statement.close();
		
		statement = con.prepareStatement(sql);
		statement.setInt(1, baseClassIndex);
		statement.setInt(2, player.getObjectId());
		statement.setInt(3, subClassIndex);
		statement.execute();
		statement.close();
		
		statement = con.prepareStatement(sql);
		statement.setInt(1, subClassIndex);
		statement.setInt(2, player.getObjectId());
		statement.setInt(3, tempClassIndex);
		statement.execute();
		statement.close();
	}
	
	private void updateCharSkillsSave(Connection con, Player player, int subClassIndex, int baseClassIndex) throws SQLException
	{
		final var sql = "UPDATE character_skills_save SET class_index = ? WHERE charId = ? AND  class_index = ?";
		final var tempClassIndex = 100;
		
		var statement = con.prepareStatement(sql);
		statement.setInt(1, tempClassIndex);
		statement.setInt(2, player.getObjectId());
		statement.setInt(3, baseClassIndex);
		statement.execute();
		statement.close();
		
		statement = con.prepareStatement(sql);
		statement.setInt(1, baseClassIndex);
		statement.setInt(2, player.getObjectId());
		statement.setInt(3, subClassIndex);
		statement.execute();
		statement.close();
		
		statement = con.prepareStatement(sql);
		statement.setInt(1, subClassIndex);
		statement.setInt(2, player.getObjectId());
		statement.setInt(3, tempClassIndex);
		statement.execute();
		statement.close();
		
	}
	
	private void updateCharSummonSkillsSave(Connection con, Player player, int subClassIndex, int baseClassIndex) throws SQLException
	{
		final var sql = "UPDATE character_summon_skills_save SET ownerClassIndex = ? WHERE ownerId = ? AND ownerClassIndex = ?";
		final var tempClassIndex = 100;
		
		var statement = con.prepareStatement(sql);
		statement.setInt(1, tempClassIndex);
		statement.setInt(2, player.getObjectId());
		statement.setInt(3, baseClassIndex);
		statement.execute();
		statement.close();
		
		statement = con.prepareStatement(sql);
		statement.setInt(1, baseClassIndex);
		statement.setInt(2, player.getObjectId());
		statement.setInt(3, subClassIndex);
		statement.execute();
		statement.close();
		
		statement = con.prepareStatement(sql);
		statement.setInt(1, subClassIndex);
		statement.setInt(2, player.getObjectId());
		statement.setInt(3, tempClassIndex);
		statement.execute();
		statement.close();
		
	}
	
	private void updateCharShortcuts(Connection con, Player player, int subClassIndex, int baseClassIndex) throws SQLException
	{
		final var sql = "UPDATE character_shortcuts SET class_index = ? WHERE charId = ? AND class_index = ?";
		final var tempClassIndex = 100;
		
		var statement = con.prepareStatement(sql);
		statement.setInt(1, tempClassIndex);
		statement.setInt(2, player.getObjectId());
		statement.setInt(3, baseClassIndex);
		statement.execute();
		statement.close();
		
		statement = con.prepareStatement(sql);
		statement.setInt(1, baseClassIndex);
		statement.setInt(2, player.getObjectId());
		statement.setInt(3, subClassIndex);
		statement.execute();
		statement.close();
		
		statement = con.prepareStatement(sql);
		statement.setInt(1, subClassIndex);
		statement.setInt(2, player.getObjectId());
		statement.setInt(3, tempClassIndex);
		statement.execute();
		statement.close();
		
	}
	
	private void updateCharHennas(Connection con, Player player, int subClassIndex, int baseClassIndex) throws SQLException
	{
		final var sql = "UPDATE character_hennas SET class_index = ? WHERE charId = ? AND  class_index = ?";
		final var tempClassIndex = 100;
		
		var statement = con.prepareStatement(sql);
		statement.setInt(1, tempClassIndex);
		statement.setInt(2, player.getObjectId());
		statement.setInt(3, baseClassIndex);
		statement.execute();
		statement.close();
		
		statement = con.prepareStatement(sql);
		statement.setInt(1, baseClassIndex);
		statement.setInt(2, player.getObjectId());
		statement.setInt(3, subClassIndex);
		statement.execute();
		statement.close();
		
		statement = con.prepareStatement(sql);
		statement.setInt(1, subClassIndex);
		statement.setInt(2, player.getObjectId());
		statement.setInt(3, tempClassIndex);
		statement.execute();
		statement.close();
		
	}
	
	private void updateCharQuests(Connection con, Player player, int subClassIndex, int baseClassIndex) throws SQLException
	{
		final var sql = "UPDATE character_quests SET class_index = ? WHERE charId = ? AND  class_index = ?";
		final var tempClassIndex = 100;
		
		var statement = con.prepareStatement(sql);
		statement.setInt(1, tempClassIndex);
		statement.setInt(2, player.getObjectId());
		statement.setInt(3, baseClassIndex);
		statement.execute();
		statement.close();
		
		statement = con.prepareStatement(sql);
		statement.setInt(1, baseClassIndex);
		statement.setInt(2, player.getObjectId());
		statement.setInt(3, subClassIndex);
		statement.execute();
		statement.close();
		
		statement = con.prepareStatement(sql);
		statement.setInt(1, subClassIndex);
		statement.setInt(2, player.getObjectId());
		statement.setInt(3, tempClassIndex);
		statement.execute();
		statement.close();
		
	}
	
	private void updateCharRecipebook(Connection con, Player player, int subClassIndex, int baseClassIndex) throws SQLException
	{
		final var sql = "UPDATE character_recipebook SET classIndex = ? WHERE charId = ? AND  classIndex = ?";
		final var tempClassIndex = 100;
		
		var statement = con.prepareStatement(sql);
		statement.setInt(1, tempClassIndex);
		statement.setInt(2, player.getObjectId());
		statement.setInt(3, baseClassIndex);
		statement.execute();
		statement.close();
		
		statement = con.prepareStatement(sql);
		statement.setInt(1, baseClassIndex);
		statement.setInt(2, player.getObjectId());
		statement.setInt(3, subClassIndex);
		statement.execute();
		statement.close();
		
		statement = con.prepareStatement(sql);
		statement.setInt(1, subClassIndex);
		statement.setInt(2, player.getObjectId());
		statement.setInt(3, tempClassIndex);
		statement.execute();
		statement.close();
		
	}
	
	private void updateOlympNubles(Connection con, Player player, int subClassId, int baseClassId) throws SQLException
	{
		final var sql = "UPDATE olympiad_nobles SET class_id = ? WHERE charId = ? AND  class_id = ?";
		final var statement = con.prepareStatement(sql);
		statement.setInt(1, subClassId);
		statement.setInt(2, player.getObjectId());
		statement.setInt(3, baseClassId);
		statement.execute();
		statement.close();
	}
	
	private void updateOlympNublesEOM(Connection con, Player player, int subClassId, int baseClassId) throws SQLException
	{
		final var sql = "UPDATE olympiad_nobles_eom SET class_id = ? WHERE charId = ? AND  class_id = ?";
		final var statement = con.prepareStatement(sql);
		statement.setInt(1, subClassId);
		statement.setInt(2, player.getObjectId());
		statement.setInt(3, baseClassId);
		statement.execute();
		statement.close();
	}
	
	protected String getPlayerInfo(String html, Player activeChar)
	{
		html = html.replace("%nick%", String.valueOf(activeChar.getName(null).toString()));
		html = html.replace("%account_name%", activeChar.getAccountName());
		html = html.replace("%name_server%", Config.SERVER_NAME);
		html = html.replace("%clan_count%", String.valueOf(ClanHolder.getInstance().getClans().length));
		html = html.replace("%player_name%", String.valueOf(activeChar.getName(null)));
		html = html.replace("%player_class%", Util.className(activeChar, activeChar.getClassId().getId()));
		html = html.replace("%player_pvp%", String.valueOf(activeChar.getPvpKills()));
		html = html.replace("%player_pk%", String.valueOf(activeChar.getPkKills()));
		html = html.replace("%player_karma%", String.valueOf(activeChar.getKarma()));
		html = html.replace("%player_level%", String.valueOf(activeChar.getLevel()));
		html = html.replace("%player_clan%", String.valueOf(activeChar.getClan() != null ? activeChar.getClan().getName() : "<font color=\"FF0000\">" + ServerStorage.getInstance().getString(activeChar.getLang(), "Util.FALSE") + "</font>"));
		html = html.replace("%player_ally%", activeChar.getClan() != null && activeChar.getClan().getAllyId() > 0 ? activeChar.getClan().getAllyName() : "<font color=\"FF0000\">" + ServerStorage.getInstance().getString(activeChar.getLang(), "Util.FALSE") + "</font>");
		html = html.replace("%player_noobless%", String.valueOf(activeChar.isNoble() ? "<font color=\"18FF00\">" + ServerStorage.getInstance().getString(activeChar.getLang(), "Util.TRUE") + "</font>" : "<font color=\"ff6755\">" + ServerStorage.getInstance().getString(activeChar.getLang(), "Util.FALSE") + "</font>"));
		html = html.replace("%online_time%", TimeUtils.formatTime(activeChar, (int) activeChar.getTotalOnlineTime(), false));
		html = html.replace("%player_ip%", activeChar.getIPAddress());
		html = html.replace("%mytime%", getTimeInServer(activeChar));
		html = html.replace("%player_premium%", getStatus(activeChar));
		html = html.replace("%premium_status%", getPremiumStatus(activeChar, true));
		html = html.replace("%premium_time%", getPremiumStatus(activeChar, false));
		html = html.replace("%rate_xp%", String.valueOf("" + (int) Config.RATE_XP_BY_LVL[activeChar.getLevel()] * activeChar.getPremiumBonus().getRateXp() + ""));
		html = html.replace("%rate_sp%", String.valueOf("" + (int) Config.RATE_SP_BY_LVL[activeChar.getLevel()] * activeChar.getPremiumBonus().getRateSp() + ""));
		html = html.replace("%rate_adena%", String.valueOf("" + (int) Config.RATE_DROP_ADENA * activeChar.getPremiumBonus().getDropAdena() + ""));
		html = html.replace("%rate_items%", String.valueOf("" + (int) Config.RATE_DROP_ITEMS * activeChar.getPremiumBonus().getDropItems() + ""));
		html = html.replace("%rate_spoil%", String.valueOf("" + (int) Config.RATE_DROP_SPOIL * activeChar.getPremiumBonus().getDropSpoil() + ""));
		html = html.replace("%rate_quest%", String.valueOf("" + (int) Config.RATE_QUEST_REWARD * activeChar.getPremiumBonus().getQuestRewardRate() + ""));
		html = html.replace("%rate_siege%", String.valueOf("" + (int) Config.RATE_DROP_SIEGE_GUARD * activeChar.getPremiumBonus().getDropSiege() + ""));
		html = html.replace("%rate_manor%", String.valueOf("" + Config.RATE_DROP_MANOR + ""));
		html = html.replace("%rate_hellbound%", String.valueOf("" + Config.RATE_HB_TRUST_INCREASE + ""));
		html = html.replace("%rate_reputation%", String.valueOf("" + (double) Config.REPUTATION_SCORE_PER_KILL + ""));
		html = html.replace("%rate_fishing%", String.valueOf("" + (int) Config.RATE_DROP_FISHING * activeChar.getPremiumBonus().getFishingRate() + ""));
		html = html.replace("%rate_raidboss%", String.valueOf("" + (int) Config.RATE_DROP_RAIDBOSS * activeChar.getPremiumBonus().getDropRaids() + ""));
		html = html.replace("%rate_epicboss%", String.valueOf("" + (int) Config.RATE_DROP_EPICBOSS * activeChar.getPremiumBonus().getDropEpics() + ""));
		html = html.replace("%server_uptime%", String.valueOf(uptime()));
		
		html = html.replace("%time%", String.valueOf(time()));
		html = html.replace("%online%", online(false));
		html = html.replace("%offtrade%", online(true));
		return html;
	}
	
	protected String getServerInfo(String html, Player activeChar)
	{
		html = html.replace("%rate_xp%", String.valueOf(Config.RATE_XP_BY_LVL[activeChar.getLevel()]));
		html = html.replace("%rate_sp%", String.valueOf(Config.RATE_SP_BY_LVL[activeChar.getLevel()]));
		html = html.replace("%rate_adena%", String.valueOf(Config.RATE_DROP_ADENA));
		html = html.replace("%rate_items%", String.valueOf(Config.RATE_DROP_ITEMS));
		html = html.replace("%rate_spoil%", String.valueOf(Config.RATE_DROP_SPOIL));
		html = html.replace("%rate_quest%", String.valueOf(Config.RATE_QUEST_REWARD));
		html = html.replace("%rate_siege%", String.valueOf(Config.RATE_DROP_SIEGE_GUARD));
		html = html.replace("%rate_manor%", String.valueOf(Config.RATE_DROP_MANOR));
		html = html.replace("%rate_hellbound%", String.valueOf(Config.RATE_HB_TRUST_INCREASE));
		html = html.replace("%rate_reputation%", String.valueOf((double) Config.REPUTATION_SCORE_PER_KILL));
		html = html.replace("%rate_fishing%", String.valueOf(Config.RATE_DROP_FISHING));
		html = html.replace("%rate_raidboss%", String.valueOf(Config.RATE_DROP_RAIDBOSS));
		
		html = html.replace("%bonus_xp%", activeChar.getPremiumBonus().getRateXp() > 1.0 ? "<font color=\"E6D0AE\" name=\"hs11\">(+" + Math.abs((int) (100 - (activeChar.getPremiumBonus().getRateXp() * 100))) + "%)</font>" : "");
		html = html.replace("%bonus_sp%", activeChar.getPremiumBonus().getRateSp() > 1.0 ? "<font color=\"E6D0AE\" name=\"hs11\">(+" + Math.abs((int) (100 - (activeChar.getPremiumBonus().getRateSp() * 100))) + "%)</font>" : "");
		html = html.replace("%bonus_adena%", activeChar.getPremiumBonus().getDropAdena() > 1.0 ? "<font color=\"E6D0AE\" name=\"hs11\">(+" + Math.abs((int) (100 - (activeChar.getPremiumBonus().getDropAdena() * 100))) + "%)</font>" : "");
		html = html.replace("%bonus_items%", activeChar.getPremiumBonus().getDropItems() > 1.0 ? "<font color=\"E6D0AE\" name=\"hs11\">(+" + Math.abs((int) (100 - (activeChar.getPremiumBonus().getDropItems() * 100))) + "%)</font>" : "");
		html = html.replace("%bonus_spoil%", activeChar.getPremiumBonus().getDropSpoil() > 1.0 ? "<font color=\"E6D0AE\" name=\"hs11\">(+" + Math.abs((int) (100 - (activeChar.getPremiumBonus().getDropSpoil() * 100))) + "%)</font>" : "");
		html = html.replace("%bonus_quest%", activeChar.getPremiumBonus().getQuestRewardRate() > 1.0 ? "<font color=\"E6D0AE\" name=\"hs11\">(+" + Math.abs((int) (100 - (activeChar.getPremiumBonus().getQuestRewardRate() * 100))) + "%)</font>" : "");
		html = html.replace("%bonus_siege%", activeChar.getPremiumBonus().getDropSiege() > 1.0 ? "<font color=\"E6D0AE\" name=\"hs11\">(+" + Math.abs((int) (100 - (activeChar.getPremiumBonus().getDropSiege() * 100))) + "%)</font>" : "");
		html = html.replace("%bonus_fishing%", activeChar.getPremiumBonus().getFishingRate() > 1.0 ? "<font color=\"E6D0AE\" name=\"hs11\">(+" + Math.abs((int) (100 - (activeChar.getPremiumBonus().getFishingRate() * 100))) + "%)</font>" : "");
		html = html.replace("%bonus_raidboss%", activeChar.getPremiumBonus().getDropRaids() > 1.0 ? "<font color=\"E6D0AE\" name=\"hs11\">(+" + Math.abs((int) (100 - (activeChar.getPremiumBonus().getDropRaids() * 100))) + "%)</font>" : "");
		html = html.replace("%bonus_epicboss%", activeChar.getPremiumBonus().getDropEpics() > 1.0 ? "<font color=\"E6D0AE\" name=\"hs11\">(+" + Math.abs((int) (100 - (activeChar.getPremiumBonus().getDropEpics() * 100))) + "%)</font>" : "");
		
		html = html.replace("%server_uptime%", String.valueOf(uptime()));
		html = html.replace("%time%", String.valueOf(time()));
		html = html.replace("%online%", online(false));
		html = html.replace("%offtrade%", online(true));
		return html;
	}
	
	private String getFunc(Player player, Skill skill, int type)
	{
		final String html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/stat-template.htm");
		String block = "";
		String list = "";
		final Skill curSkill = player.getKnownSkill(skill.getId());
		if (skill.getFuncTemplates() != null)
		{
			for (final FuncTemplate func : skill.getFuncTemplates())
			{
				block = html;
				String name = new ServerMessage(func.stat.toString(), player.getLang()).toString();
				if (name.isEmpty())
				{
					name = func.stat.toString();
				}
				block = block.replace("%name%", name);
				final double currect = getCurrentValue(player, func.stat);
				final boolean isInt = isIntValue(func.stat);
				double currentBonus = 0;
				final double nextBonus = func.lambda.calc(null);
				if (curSkill != null && curSkill.getFuncTemplates() != null)
				{
					for (final FuncTemplate curFunc : curSkill.getFuncTemplates())
					{
						if (curFunc.stat == func.stat)
						{
							currentBonus = curFunc.lambda.calc(null);
							break;
						}
					}
				}
				double add = 0;
				double next = 0;
				double diff = 0;
				boolean idProc = false;
				switch (func.order)
				{
					case 16 :
					case 64 :
						next = (currect + (nextBonus - currentBonus));
						diff = ((currect + (nextBonus - currentBonus)) - currect);
						add = (nextBonus - currentBonus);
						break;
					default :
						idProc = true;
						next = (currect * (nextBonus - currentBonus));
						diff = (currect * (nextBonus - currentBonus) - currect);
						add = ((nextBonus - (currentBonus > 0 ? currentBonus : 1)) * 100);
						break;
					
				}
				if (type == 1)
				{
					block = block.replace("%add%", String.valueOf(!idProc || isInt ? (int) next : "" + convertDouble(String.format("%.2f", next)) + "%"));
				}
				else if (type == 2)
				{
					block = block.replace("%add%", String.valueOf(!idProc || isInt ? (int) diff : "" + convertDouble(String.format("%.2f", diff)) + "%"));
				}
				else
				{
					block = block.replace("%add%", String.valueOf(!idProc ? (int) add : "" + convertDouble(String.format("%.2f", add)) + "%"));
				}
				list += block;
			}
		}
		return list;
	}
	
	private String convertDouble(String string)
	{
		final String[] info = string.split(",");
		if (info.length > 1 && info[1].equals("00"))
		{
			return info[0];
		}
		return string;
	}
	
	private double getCurrentValue(Player player, Stats stats)
	{
		switch (stats)
		{
			case STAT_STR :
				return player.getStat().getSTR();
			case STAT_CON :
				return player.getStat().getCON();
			case STAT_INT :
				return player.getStat().getINT();
			case STAT_DEX :
				return player.getStat().getDEX();
			case STAT_WIT :
				return player.getStat().getWIT();
			case STAT_MEN :
				return player.getStat().getMEN();
			case PVE_PHYSICAL_DMG :
			case PVE_PHYS_SKILL_DMG :
			case PVE_BOW_DMG :
			case PVE_BOW_SKILL_DMG :
			case PVE_MAGICAL_DMG :
			case PVP_PHYSICAL_DMG :
			case PVP_MAGICAL_DMG :
			case PVP_PHYS_SKILL_DMG :
			case PVP_PHYSICAL_DEF :
			case PVP_MAGICAL_DEF :
			case PVP_PHYS_SKILL_DEF :
				return player.calcStat(stats, 1, null, null);
			case MAX_HP :
				return player.getStat().getMaxHp();
			case MAX_MP :
				return player.getStat().getMaxMp();
			case MAX_CP :
				return player.getStat().getMaxCp();
			case POWER_DEFENCE :
				return player.getStat().getPDef(null);
			case MAGIC_DEFENCE :
				return player.getStat().getMDef(null, null);
			case POWER_ATTACK :
				return player.getStat().getPAtk(null);
			case MAGIC_ATTACK :
				return player.getStat().getMAtk(null, null);
			case POWER_ATTACK_SPEED :
				return player.getStat().getPAtkSpd();
			case MAGIC_ATTACK_SPEED :
				return player.getStat().getMAtkSpd();
			case MOVE_SPEED :
				return player.getStat().getRunSpeed();
			case FIRE_RES :
				return player.getStat().calcStat(Stats.FIRE_RES, player.getTemplate().getBaseFireRes());
			case WIND_RES :
				return player.getStat().calcStat(Stats.FIRE_RES, player.getTemplate().getBaseWindRes());
			case WATER_RES :
				return player.getStat().calcStat(Stats.FIRE_RES, player.getTemplate().getBaseWaterRes());
			case EARTH_RES :
				return player.getStat().calcStat(Stats.FIRE_RES, player.getTemplate().getBaseEarthRes());
			case HOLY_RES :
				return player.getStat().calcStat(Stats.FIRE_RES, player.getTemplate().getBaseHolyRes());
			case DARK_RES :
				return player.getStat().calcStat(Stats.FIRE_RES, player.getTemplate().getBaseDarkRes());
			case ACCURACY_COMBAT :
				return player.getStat().getAccuracy();
		}
		return 0;
	}
	
	private boolean isIntValue(Stats stats)
	{
		switch (stats)
		{
			case PVE_PHYSICAL_DMG :
			case PVE_PHYS_SKILL_DMG :
			case PVE_BOW_DMG :
			case PVE_BOW_SKILL_DMG :
			case PVE_MAGICAL_DMG :
			case PVP_PHYSICAL_DMG :
			case PVP_MAGICAL_DMG :
			case PVP_PHYS_SKILL_DMG :
			case PVP_PHYSICAL_DEF :
			case PVP_MAGICAL_DEF :
			case PVP_PHYS_SKILL_DEF :
				return false;
		}
		return true;
	}
	
	private SkillLearn selectSkill(int skillId, int skillLvl)
	{
		for (final var sl : SkillTreesParser.getInstance().getSubClassSkillTree().values())
		{
			if (sl != null)
			{
				if (sl.getId() == skillId && sl.getLvl() == skillLvl)
				{
					return sl;
				}
			}
		}
		return null;
	}

	@Override
	public void onWriteCommand(String command, String ar1, String ar2, String ar3, String ar4, String ar5, Player activeChar)
	{
	}

	public static CommunityGeneral getInstance()
	{
		return SingletonHolder._instance;
	}

	private static class SingletonHolder
	{
		protected static final CommunityGeneral _instance = new CommunityGeneral();
	}
}