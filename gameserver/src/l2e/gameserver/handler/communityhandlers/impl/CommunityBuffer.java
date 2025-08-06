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

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.NamedNodeMap;

import l2e.commons.dbutils.DbUtils;
import l2e.commons.util.Rnd;
import l2e.commons.util.StringUtil;
import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.data.holder.CharSchemesHolder;
import l2e.gameserver.data.htm.HtmCache;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.handler.communityhandlers.ICommunityBoardHandler;
import l2e.gameserver.listener.player.impl.AskQuestionAnswerListener;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.Summon;
import l2e.gameserver.model.entity.events.AbstractFightEvent;
import l2e.gameserver.model.service.BotFunctions;
import l2e.gameserver.model.service.buffer.PlayerScheme;
import l2e.gameserver.model.service.buffer.RequestItems;
import l2e.gameserver.model.service.buffer.SchemeBuff;
import l2e.gameserver.model.service.buffer.SingleBuff;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.skills.effects.Effect;
import l2e.gameserver.model.stats.Env;
import l2e.gameserver.model.strings.server.ServerMessage;
import l2e.gameserver.model.strings.server.ServerStorage;
import l2e.gameserver.model.zone.ZoneId;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.CreatureSay;
import l2e.gameserver.network.serverpackets.MagicSkillUse;
import l2e.gameserver.network.serverpackets.SystemMessage;

/**
 * Rework by LordWinter 18.05.2020
 */
public class CommunityBuffer extends AbstractCommunity implements ICommunityBoardHandler
{
	public static final String BYPASS_BUFFER = "_bbsbuffer";
	private static final char[] FINE_CHARS =
	{
	        '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', 'q', 'w', 'e', 'r', 't', 'y', 'u', 'i', 'o', 'p', 'a', 's', 'd', 'f', 'g', 'h', 'j', 'k', 'l', 'z', 'x', 'c', 'v', 'b', 'n', 'm', 'Q', 'W', 'E', 'R', 'T', 'Y', 'U', 'I', 'O', 'P', 'A', 'S', 'D', 'F', 'G', 'H', 'J', 'K', 'L', 'Z', 'X', 'C', 'V', 'B', 'N', 'M', ' '
	};
	
	private static final String[] SCHEME_ICONS = new String[]
	{
	        "Icon.skill1331", "Icon.skill1332", "Icon.skill1316", "Icon.skill1264", "Icon.skill1254", "Icon.skill1178", "Icon.skill1085", "Icon.skill957", "Icon.skill0928", "Icon.skill0793", "Icon.skill0787", "Icon.skill0490", "Icon.skill0487", "Icon.skill0452", "Icon.skill0453", "Icon.skill0440", "Icon.skill0409", "Icon.skill0405", "Icon.skill0061", "Icon.skill0072", "Icon.skill0219", "Icon.skill0208", "Icon.skill0210", "Icon.skill0254", "Icon.skill0228", "Icon.skill0222", "Icon.skill0181", "Icon.skill0078", "Icon.skill0091", "Icon.skill0076", "Icon.skill0025", "Icon.skill0018", "Icon.skill0019", "Icon.skill0007", "Icon.skill1391", "Icon.skill1373", "Icon.skill1388", "Icon.skill1409", "Icon.skill1457", "Icon.skill1501", "Icon.skill1520", "Icon.skill1506", "Icon.skill1527", "Icon.skill5016", "Icon.skill5860", "Icon.skill5661", "Icon.skill6302", "Icon.skill6171", "Icon.skill6286", "Icon.skill4106", "Icon.skill4270_3"
	};
	
	private final List<SingleBuff> _allSingleBuffs = new LinkedList<>();
	private final Map<Integer, ArrayList<SingleBuff>> _buffs = new HashMap<>();
	private final Map<String, ArrayList<SingleBuff>> _typeBuffs = new HashMap<>();
	private final Map<Integer, RequestItems> _setPrices = new HashMap<>();
	private final Map<Integer, List<Integer>> _classes = new HashMap<>();
	
	public CommunityBuffer()
	{
		load();
		if (Config.DEBUG)
		{
			_log.info(getClass().getSimpleName() + ": Loading all functions.");
		}
	}
	
	public void load()
	{
		_buffs.clear();
		_allSingleBuffs.clear();
		_typeBuffs.clear();
		try
		{
			final var factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);
			
			final var file = new File(Config.DATAPACK_ROOT, "data/stats/services/communityBuffer.xml");
			if (!file.exists())
			{
				_log.warn(getClass().getSimpleName() + ": Couldn't find data/stats/services/" + file.getName());
				return;
			}
			
			final var doc = factory.newDocumentBuilder().parse(file);
			NamedNodeMap attrs;
			int id, groupId;
			
			for (var list = doc.getFirstChild(); list != null; list = list.getNextSibling())
			{
				if ("list".equalsIgnoreCase(list.getNodeName()))
				{
					for (var groups = list.getFirstChild(); groups != null; groups = groups.getNextSibling())
					{
						if ("skill".equalsIgnoreCase(groups.getNodeName()))
						{
							final var skillId = Integer.parseInt(groups.getAttributes().getNamedItem("id").getNodeValue());
							final var skillLvl = Integer.parseInt(groups.getAttributes().getNamedItem("level").getNodeValue());
							final var premiumLvl = groups.getAttributes().getNamedItem("premiumLvl") != null ? Integer.parseInt(groups.getAttributes().getNamedItem("premiumLvl").getNodeValue()) : skillLvl;
							final var buffTime = groups.getAttributes().getNamedItem("buffTime") != null ? Integer.parseInt(groups.getAttributes().getNamedItem("buffTime").getNodeValue()) : 0;
							final var premiumBuffTime = groups.getAttributes().getNamedItem("premiumBuffTime") != null ? Integer.parseInt(groups.getAttributes().getNamedItem("premiumBuffTime").getNodeValue()) : buffTime;
							final var type = groups.getAttributes().getNamedItem("type").getNodeValue();
							final var isDanceSlot = groups.getAttributes().getNamedItem("isDanceSlot") != null ? Boolean.parseBoolean(groups.getAttributes().getNamedItem("isDanceSlot").getNodeValue()) : false;
							final var requestItems = groups.getAttributes().getNamedItem("requestItems") != null ? parseItemsList(groups.getAttributes().getNamedItem("requestItems").getNodeValue()) : null;
							final var needAllItems = groups.getAttributes().getNamedItem("needAllItems") != null ? Boolean.parseBoolean(groups.getAttributes().getNamedItem("needAllItems").getNodeValue()) : false;
							final var removeItems = groups.getAttributes().getNamedItem("removeItems") != null ? Boolean.parseBoolean(groups.getAttributes().getNamedItem("removeItems").getNodeValue()) : false;
							final var single = new SingleBuff(type, skillId, skillLvl, premiumLvl, buffTime, premiumBuffTime, isDanceSlot, requestItems, needAllItems, removeItems);
							_allSingleBuffs.add(single);
							if (!_typeBuffs.containsKey(type))
							{
								_typeBuffs.put(type, new ArrayList<>());
							}
							_typeBuffs.get(type).add(single);
						}
						else if ("groups".equalsIgnoreCase(groups.getNodeName()))
						{
							attrs = groups.getAttributes();
							groupId = Integer.parseInt(attrs.getNamedItem("groupId").getNodeValue());
							final var requestItems = groups.getAttributes().getNamedItem("requestItems") != null ? parseItemsList(groups.getAttributes().getNamedItem("requestItems").getNodeValue()) : null;
							final var needAllItems = groups.getAttributes().getNamedItem("needAllItems") != null ? Boolean.parseBoolean(groups.getAttributes().getNamedItem("needAllItems").getNodeValue()) : false;
							final var removeItems = groups.getAttributes().getNamedItem("removeItems") != null ? Boolean.parseBoolean(groups.getAttributes().getNamedItem("removeItems").getNodeValue()) : false;
							final var requestTemplate = new RequestItems(requestItems, needAllItems, removeItems);
							final ArrayList<SingleBuff> groupSkills = new ArrayList<>();
							final List<Integer> classes = new ArrayList<>();
							for (var skills = groups.getFirstChild(); skills != null; skills = skills.getNextSibling())
							{
								if ("class".equalsIgnoreCase(skills.getNodeName()))
								{
									attrs = skills.getAttributes();
									id = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
									classes.add(id);
								}
								else if ("skill".equalsIgnoreCase(skills.getNodeName()))
								{
									attrs = skills.getAttributes();
									id = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
									for (final SingleBuff singleBuff : _allSingleBuffs)
									{
										if (singleBuff != null && singleBuff.getSkillId() == id)
										{
											groupSkills.add(singleBuff);
										}
									}
								}
							}
							_classes.put(groupId, classes);
							_buffs.put(groupId, groupSkills);
							_setPrices.put(groupId, requestTemplate);
						}
					}
				}
			}
		}
		catch (final Exception e)
		{
			_log.warn(getClass().getSimpleName() + ": Error while loading buffs: " + e);
		}
	}
	
	@Override
	public String[] getBypassCommands()
	{
		return new String[]
		{
		        "_bbsbuffer", "_bbsbufferbypass", "_bbsbuffs", "_bbsbuffpage"
		};
	}
	
	@Override
	public void onBypassCommand(String command, Player player)
	{
		final var st = new StringTokenizer(command, "_");
		final var cmd = st.nextToken();
		
		if (!checkCondition(player, cmd, true, false))
		{
			return;
		}
		
		if (player.isInSiege() && !Config.ALLOW_COMMUNITY_BUFF_IN_SIEGE)
		{
			player.sendMessage((new ServerMessage("Community.ALL_DISABLE", player.getLang())).toString());
			return;
		}
		
		if (Config.ALLOW_BUFF_PEACE_ZONE)
		{
			if (!player.isInsideZone(ZoneId.PEACE) && !player.isInFightEvent() && !player.isInPartyTournament())
			{
				var canUse = false;
				if (Config.ALLOW_BUFF_WITHOUT_PEACE_FOR_PREMIUM && player.hasPremiumBonus())
				{
					canUse = true;
				}
				
				if (!canUse)
				{
					player.sendMessage((new ServerMessage("Community.ALL_DISABLE", player.getLang())).toString());
					return;
				}
			}
		}
		
		if ("bbsbuffer".equals(cmd))
		{
			showWindow(player);
			return;
		}
		
		if ("bbsbuffs".equals(cmd))
		{
			castSkill(command.substring("_bbsbuffs_".length()), player);
			return;
		}
		
		if ("bbsbuffpage".equals(cmd))
		{
			generatePage(command.substring("_bbsbuffpage_".length()), player);
			return;
		}
		
		if ("bbsbufferbypass".equals(cmd))
		{
			onBypass(command.substring("_bbsbufferbypass_".length()), player);
			return;
		}
	}
	
	private void generatePage(String command, Player player)
	{
		final var eventSplit = command.split(":");
		if (eventSplit.length < 2)
		{
			return;
		}
		final var eventParam0 = eventSplit[0];
		final var eventParam1 = eventSplit[1];
		setPetBuff(player, eventParam0);
		var html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/buffer/" + eventParam1 + ".htm");
		var button = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/buffer/buffer_main_button.htm");
		html = html.replace("%schemePart%", generateScheme(player));
		if (isPetBuff(player))
		{
			button = button.replace("%name%", (player.hasSummon() ? player.getSummon().getSummonName(player.getLang()) : ServerStorage.getInstance().getString(player.getLang(), "CommunityBuffer.DONT_HAVE_PET")));
			button = button.replace("%bypass%", "_bbsbuffpage_0:" + eventParam1 + "");
		}
		else
		{
			button = button.replace("%name%", player.getName(null));
			button = button.replace("%bypass%", "_bbsbuffpage_1:" + eventParam1 + "");
		}
		html = html.replace("%topbtn%", button);
		separateAndSend(html, player);
	}
	
	private void castSkill(String command, Player player)
	{
		final var eventSplit = command.split(" ");
		if (eventSplit.length < 5)
		{
			return;
		}
		
		final var eventParam0 = eventSplit[0];
		final var eventParam1 = eventSplit[1];
		final var eventParam2 = eventSplit[2];
		final var eventParam3 = eventSplit[3];
		final var eventParam4 = eventSplit[4];
		
		var html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/" + eventParam4 + ".htm");
		html = html.replace("%schemePart%", generateScheme(player));
		if (eventParam0.equalsIgnoreCase("skill"))
		{
			if (!player.checkFloodProtection("SINGLEBUFF", "singlebuff_delay"))
			{
				return;
			}
			
			if (!isValidSkill(player, Integer.parseInt(eventParam1), Integer.parseInt(eventParam2)))
			{
				Util.handleIllegalPlayerAction(player, "" + player.getName(null) + " try to cheat with Community Buffer!");
				return;
			}
			
			if (!Config.FREE_ALL_BUFFS && (player.getLevel() > Config.COMMUNITY_FREE_BUFF_LVL))
			{
				if (player.getInventory().getItemByItemId(Config.BUFF_ID_ITEM) == null)
				{
					player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
					separateAndSend(html, player);
					return;
				}
				
				if (player.getInventory().getItemByItemId(Config.BUFF_ID_ITEM).getCount() < Config.BUFF_AMOUNT)
				{
					final var message = new ServerMessage("CommunityBuffer.NECESSARY_ITEMS", player.getLang());
					message.add(Config.BUFF_AMOUNT);
					message.add(Util.getItemName(player, Config.BUFF_ID_ITEM));
					sendErrorMessageToPlayer(player, message.toString());
					separateAndSend(html, player);
					return;
				}
				player.destroyItemByItemId("SchemeBuffer", Config.BUFF_ID_ITEM, Config.BUFF_AMOUNT, player, true);
			}
			
			if (player.isBlocked())
			{
				return;
			}
			
			if (isItemRemoveSkill(player, Integer.parseInt(eventParam1), Integer.parseInt(eventParam2)))
			{
				if (!checkItemsForSkill(player, Integer.parseInt(eventParam1), Integer.parseInt(eventParam2), true))
				{
					separateAndSend(html, player);
					return;
				}
			}
			
			final var getpetbuff = Integer.parseInt(eventParam3) == 1;
			if (!getpetbuff)
			{
				final var skill = SkillsParser.getInstance().getInfo(Integer.parseInt(eventParam1), Integer.parseInt(eventParam2));
				if (skill != null)
				{
					double hp = 0.;
					double mp = 0.;
					double cp = 0.;
					final var canHeal = canHeal(player);
					if (!canHeal)
					{
						hp = player.getCurrentHp();
						mp = player.getCurrentMp();
						cp = player.getCurrentCp();
					}
					
					final var buffTime = getBuffTime(player, skill.getId(), skill.getLevel());
					if (buffTime > 0 && skill.hasEffects())
					{
						final var env = new Env();
						env.setCharacter(player);
						env.setTarget(player);
						env.setSkill(skill);
						
						Effect ef;
						for (final var et : skill.getEffectTemplates())
						{
							ef = et.getEffect(env);
							if (ef != null)
							{
								ef.setAbnormalTime(buffTime * 60);
								ef.scheduleEffect(true);
							}
						}
					}
					else
					{
						skill.getEffects(player, player, false);
					}
					player.broadcastPacket(new MagicSkillUse(player, player, Integer.parseInt(eventParam1), Integer.parseInt(eventParam2), 2, 0));
					
					if (!canHeal)
					{
						player.setCurrentHpMp(hp, mp);
						player.setCurrentCp(cp);
					}
					
					if (Config.ALLOW_SUMMON_AUTO_BUFF && player.hasSummon())
					{
						if (!canHeal)
						{
							hp = player.getSummon().getCurrentHp();
							mp = player.getSummon().getCurrentMp();
						}
						
						if (buffTime > 0 && skill.hasEffects())
						{
							final var env = new Env();
							env.setCharacter(player);
							env.setTarget(player.getSummon());
							env.setSkill(skill);
							
							Effect ef;
							for (final var et : skill.getEffectTemplates())
							{
								ef = et.getEffect(env);
								if (ef != null)
								{
									ef.setAbnormalTime(buffTime * 60);
									ef.scheduleEffect(true);
								}
							}
						}
						else
						{
							skill.getEffects(player, player.getSummon(), false);
						}
						player.broadcastPacket(new MagicSkillUse(player, player.getSummon(), Integer.parseInt(eventParam1), Integer.parseInt(eventParam2), 0, 0));
						
						if (!canHeal)
						{
							player.getSummon().setCurrentHpMp(hp, mp);
						}
					}
				}
			}
			else
			{
				if (player.hasSummon())
				{
					final var skill = SkillsParser.getInstance().getInfo(Integer.parseInt(eventParam1), Integer.parseInt(eventParam2));
					if (skill != null)
					{
						double hp = 0.;
						double mp = 0.;
						final var canHeal = canHeal(player);
						if (!canHeal)
						{
							hp = player.getSummon().getCurrentHp();
							mp = player.getSummon().getCurrentMp();
						}
						
						final var buffTime = getBuffTime(player, skill.getId(), skill.getLevel());
						if (buffTime > 0 && skill.hasEffects())
						{
							final var env = new Env();
							env.setCharacter(player);
							env.setTarget(player.getSummon());
							env.setSkill(skill);
							
							Effect ef;
							for (final var et : skill.getEffectTemplates())
							{
								ef = et.getEffect(env);
								if (ef != null)
								{
									ef.setAbnormalTime(buffTime * 60);
									ef.scheduleEffect(true);
								}
							}
						}
						else
						{
							skill.getEffects(player, player.getSummon(), false);
						}
						player.broadcastPacket(new MagicSkillUse(player, player.getSummon(), Integer.parseInt(eventParam1), Integer.parseInt(eventParam2), 0, 0));
						
						if (!canHeal)
						{
							player.getSummon().setCurrentHpMp(hp, mp);
						}
					}
				}
				else
				{
					sendErrorMessageToPlayer(player, new ServerMessage("CommunityBuffer.NOT_HAVE_SERVITOR", player.getLang()).toString());
					separateAndSend(html, player);
					return;
				}
			}
			separateAndSend(html, player);
		}
	}
	
	private void onBypass(String command, Player player)
	{
		String msg = null;
		
		final var eventSplit = command.split(" ");
		if (eventSplit.length < 4)
		{
			return;
		}
		
		final var eventParam0 = eventSplit[0];
		final var eventParam1 = eventSplit[1];
		final var eventParam2 = eventSplit[2];
		final var eventParam3 = eventSplit[3];
		String eventParam4 = null;
		if (eventSplit.length > 4)
		{
			eventParam4 = eventSplit[4];
		}
		
		if (eventParam0.equalsIgnoreCase("buffpet"))
		{
			setPetBuff(player, eventParam1);
			msg = main(player);
		}
		else if (eventParam0.equals("redirect"))
		{
			if (eventParam1.equals("main"))
			{
				msg = main(player);
			}
			else
			{
				final var type = eventParam1.split("_")[1];
				if (type == null)
				{
					return;
				}
				msg = buildHtml(type, player, Integer.parseInt(eventParam2));
			}
		}
		else if (eventParam0.equalsIgnoreCase("giveBuffs"))
		{
			if (eventParam4 == null)
			{
				return;
			}
			
			if (!player.checkFloodProtection("SINGLEBUFF", "singlebuff_delay"))
			{
				return;
			}
			
			if (!isValidTypeSkill(player, Integer.parseInt(eventParam1), Integer.parseInt(eventParam2), eventParam3))
			{
				Util.handleIllegalPlayerAction(player, "" + player.getName(null) + " try to cheat with Community Buffer!");
				return;
			}
			
			if (!Config.FREE_ALL_BUFFS && (player.getLevel() > Config.COMMUNITY_FREE_BUFF_LVL))
			{
				if (player.getInventory().getItemByItemId(Config.BUFF_ID_ITEM) == null)
				{
					player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
					showCommunity(player, main(player));
					return;
				}
				
				if (player.getInventory().getItemByItemId(Config.BUFF_ID_ITEM).getCount() < Config.BUFF_AMOUNT)
				{
					final var message = new ServerMessage("CommunityBuffer.NECESSARY_ITEMS", player.getLang());
					message.add(Config.BUFF_AMOUNT);
					message.add(Util.getItemName(player, Config.BUFF_ID_ITEM));
					sendErrorMessageToPlayer(player, message.toString());
					showCommunity(player, main(player));
					return;
				}
				player.destroyItemByItemId("SchemeBuffer", Config.BUFF_ID_ITEM, Config.BUFF_AMOUNT, player, true);
			}
			
			if (player.isBlocked())
			{
				return;
			}
			
			if (isItemRemoveTypeSkill(player, Integer.parseInt(eventParam1), eventParam3))
			{
				if (!checkItemsForTypeSkill(player, Integer.parseInt(eventParam1), true, eventParam3))
				{
					showCommunity(player, main(player));
					return;
				}
			}
			
			final var getpetbuff = isPetBuff(player);
			if (!getpetbuff)
			{
				final var skill = SkillsParser.getInstance().getInfo(Integer.parseInt(eventParam1), Integer.parseInt(eventParam2));
				if (skill != null)
				{
					double hp = 0.;
					double mp = 0.;
					double cp = 0.;
					final var canHeal = canHeal(player);
					if (!canHeal)
					{
						hp = player.getCurrentHp();
						mp = player.getCurrentMp();
						cp = player.getCurrentCp();
					}
					
					final var buffTime = getBuffTime(player, skill.getId(), eventParam3);
					if (buffTime > 0 && skill.hasEffects())
					{
						final var env = new Env();
						env.setCharacter(player);
						env.setTarget(player);
						env.setSkill(skill);
						
						Effect ef;
						for (final var et : skill.getEffectTemplates())
						{
							ef = et.getEffect(env);
							if (ef != null)
							{
								ef.setAbnormalTime(buffTime * 60);
								ef.scheduleEffect(true);
							}
						}
					}
					else
					{
						skill.getEffects(player, player, false);
					}
					player.broadcastPacket(new MagicSkillUse(player, player, Integer.parseInt(eventParam1), Integer.parseInt(eventParam2), 2, 0));
					if (!canHeal)
					{
						player.setCurrentHpMp(hp, mp);
						player.setCurrentCp(cp);
					}
					
					if (Config.ALLOW_SUMMON_AUTO_BUFF && player.hasSummon())
					{
						if (!canHeal)
						{
							hp = player.getSummon().getCurrentHp();
							mp = player.getSummon().getCurrentMp();
						}
						
						if (buffTime > 0 && skill.hasEffects())
						{
							final var env = new Env();
							env.setCharacter(player);
							env.setTarget(player.getSummon());
							env.setSkill(skill);
							
							Effect ef;
							for (final var et : skill.getEffectTemplates())
							{
								ef = et.getEffect(env);
								if (ef != null)
								{
									ef.setAbnormalTime(buffTime * 60);
									ef.scheduleEffect(true);
								}
							}
						}
						else
						{
							skill.getEffects(player, player.getSummon(), false);
						}
						player.broadcastPacket(new MagicSkillUse(player, player.getSummon(), Integer.parseInt(eventParam1), Integer.parseInt(eventParam2), 0, 0));
						
						if (!canHeal)
						{
							player.getSummon().setCurrentHpMp(hp, mp);
						}
					}
				}
			}
			else
			{
				if (player.hasSummon())
				{
					final var skill = SkillsParser.getInstance().getInfo(Integer.parseInt(eventParam1), Integer.parseInt(eventParam2));
					if (skill != null)
					{
						double hp = 0.;
						double mp = 0.;
						final var canHeal = canHeal(player);
						if (!canHeal)
						{
							hp = player.getSummon().getCurrentHp();
							mp = player.getSummon().getCurrentMp();
						}
						
						final var buffTime = getBuffTime(player, skill.getId(), eventParam3);
						if (buffTime > 0 && skill.hasEffects())
						{
							final var env = new Env();
							env.setCharacter(player);
							env.setTarget(player.getSummon());
							env.setSkill(skill);
							
							Effect ef;
							for (final var et : skill.getEffectTemplates())
							{
								ef = et.getEffect(env);
								if (ef != null)
								{
									ef.setAbnormalTime(buffTime * 60);
									ef.scheduleEffect(true);
								}
							}
						}
						else
						{
							skill.getEffects(player, player.getSummon(), false);
						}
						player.broadcastPacket(new MagicSkillUse(player, player.getSummon(), Integer.parseInt(eventParam1), Integer.parseInt(eventParam2), 0, 0));
						if (!canHeal)
						{
							player.getSummon().setCurrentHpMp(hp, mp);
						}
					}
				}
				else
				{
					sendErrorMessageToPlayer(player, new ServerMessage("CommunityBuffer.NOT_HAVE_SERVITOR", player.getLang()).toString());
					showCommunity(player, main(player));
					return;
				}
			}
			msg = buildHtml(eventParam3, player, Integer.parseInt(eventParam4));
		}
		else if (eventParam0.equalsIgnoreCase("heal"))
		{
			if (!player.checkFloodProtection("SINGLEBUFF", "singlebuff_delay"))
			{
				return;
			}
			
			if (!Config.FREE_ALL_BUFFS && (player.getLevel() > Config.COMMUNITY_FREE_BUFF_LVL))
			{
				if (player.getInventory().getItemByItemId(Config.BUFF_ID_ITEM) == null)
				{
					player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
					showCommunity(player, main(player));
					return;
				}
				
				if (player.getInventory().getItemByItemId(Config.BUFF_ID_ITEM).getCount() < Config.HPMPCP_BUFF_AMOUNT)
				{
					final var message = new ServerMessage("CommunityBuffer.NECESSARY_ITEMS", player.getLang());
					message.add(Config.HPMPCP_BUFF_AMOUNT);
					message.add(Util.getItemName(player, Config.BUFF_ID_ITEM));
					sendErrorMessageToPlayer(player, message.toString());
					showCommunity(player, main(player));
					return;
				}
				player.destroyItemByItemId("SchemeBuffer", Config.BUFF_ID_ITEM, Config.HPMPCP_BUFF_AMOUNT, player, true);
			}
			
			if (!canHeal(player))
			{
				sendErrorMessageToPlayer(player, new ServerMessage("CommunityBuffer.CANT_HEAL", player.getLang()).toString());
				showCommunity(player, main(player));
				return;
			}
			final var getpetbuff = isPetBuff(player);
			if (getpetbuff)
			{
				if (player.hasSummon())
				{
					heal(player, getpetbuff);
				}
				else
				{
					sendErrorMessageToPlayer(player, new ServerMessage("CommunityBuffer.NOT_HAVE_SERVITOR", player.getLang()).toString());
					showCommunity(player, main(player));
					return;
				}
			}
			else
			{
				heal(player, getpetbuff);
			}
			msg = main(player);
		}
		else if (eventParam0.equalsIgnoreCase("removeBuffs"))
		{
			if (!player.checkFloodProtection("BUFFSDELAY", "buffs_delay"))
			{
				return;
			}
			
			if (!Config.FREE_ALL_BUFFS && (player.getLevel() > Config.COMMUNITY_FREE_BUFF_LVL))
			{
				if (player.getInventory().getItemByItemId(Config.BUFF_ID_ITEM) == null)
				{
					player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
					showCommunity(player, main(player));
					return;
				}
				
				if (player.getInventory().getItemByItemId(Config.BUFF_ID_ITEM).getCount() < Config.CANCEL_BUFF_AMOUNT)
				{
					final var message = new ServerMessage("CommunityBuffer.NECESSARY_ITEMS", player.getLang());
					message.add(Config.CANCEL_BUFF_AMOUNT);
					message.add(Util.getItemName(player, Config.BUFF_ID_ITEM));
					sendErrorMessageToPlayer(player, message.toString());
					showCommunity(player, main(player));
					return;
				}
				player.destroyItemByItemId("SchemeBuffer", Config.BUFF_ID_ITEM, Config.CANCEL_BUFF_AMOUNT, player, true);
			}
			
			final var getpetbuff = isPetBuff(player);
			if (getpetbuff)
			{
				if (player.hasSummon())
				{
					player.getSummon().getEffectList().stopAllBuffs();
				}
				else
				{
					sendErrorMessageToPlayer(player, new ServerMessage("CommunityBuffer.NOT_HAVE_SERVITOR", player.getLang()).toString());
					showCommunity(player, main(player));
					return;
				}
			}
			else
			{
				player.getEffectList().stopAllBuffs();
				player.stopCubics(true);
			}
			msg = main(player);
		}
		else if (eventParam0.equalsIgnoreCase("removeDebuffs"))
		{
			if (!player.checkFloodProtection("BUFFSDELAY", "buffs_delay"))
			{
				return;
			}
			
			if (!Config.FREE_ALL_BUFFS && (player.getLevel() > Config.COMMUNITY_FREE_BUFF_LVL))
			{
				if (player.getInventory().getItemByItemId(Config.BUFF_ID_ITEM) == null)
				{
					player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
					showCommunity(player, main(player));
					return;
				}
				
				if (player.getInventory().getItemByItemId(Config.BUFF_ID_ITEM).getCount() < Config.CANCEL_BUFF_AMOUNT)
				{
					final ServerMessage message = new ServerMessage("CommunityBuffer.NECESSARY_ITEMS", player.getLang());
					message.add(Config.CANCEL_BUFF_AMOUNT);
					message.add(Util.getItemName(player, Config.BUFF_ID_ITEM));
					sendErrorMessageToPlayer(player, message.toString());
					showCommunity(player, main(player));
					return;
				}
				player.destroyItemByItemId("SchemeBuffer", Config.BUFF_ID_ITEM, Config.CANCEL_BUFF_AMOUNT, player, true);
			}
			
			final boolean getpetbuff = isPetBuff(player);
			if (getpetbuff)
			{
				if (player.hasSummon())
				{
					player.getSummon().getEffectList().stopAllDebuffs();
				}
				else
				{
					sendErrorMessageToPlayer(player, new ServerMessage("CommunityBuffer.NOT_HAVE_SERVITOR", player.getLang()).toString());
					showCommunity(player, main(player));
					return;
				}
			}
			else
			{
				player.getEffectList().stopAllDebuffs();
			}
			msg = main(player);
		}
		else if (eventParam0.equalsIgnoreCase("cast"))
		{
			if (Config.ALLOW_SCHEMES_FOR_PREMIUMS && !player.hasPremiumBonus())
			{
				sendErrorMessageToPlayer(player, new ServerMessage("CommunityBuffer.ONLY_FOR_PREMIUM", player.getLang()).toString());
				return;
			}
			
			if (!player.checkFloodProtection("BUFFSDELAY", "buffs_delay"))
			{
				return;
			}
			
			final var schemeId = Integer.parseInt(eventParam1);
			if (player.getBuffSchemeById(schemeId) == null || player.getBuffSchemeById(schemeId).getBuffs() == null)
			{
				player.sendMessage(new ServerMessage("CommunityBuffer.NEED_CREATE_SCHEME", player.getLang()).toString());
				return;
			}
			
			final List<Integer> buffs = new ArrayList<>();
			final List<Integer> levels = new ArrayList<>();
			
			for (final var buff : player.getBuffSchemeById(schemeId).getBuffs())
			{
				final var id = buff.getSkillId();
				final var level = player.hasPremiumBonus() ? buff.getPremiumLevel() : buff.getLevel();
				
				if (!isValidSkill(player, id, level))
				{
					Util.handleIllegalPlayerAction(player, "" + player.getName(null) + " try to cheat with Community Buffer!");
					return;
				}
				
				final var type = getBuffType(player, id);
				if (type.equals("none"))
				{
					continue;
				}
				buffs.add(id);
				levels.add(level);
			}
			
			final var getpetbuff = isPetBuff(player);
			
			if (player.isBlocked())
			{
				return;
			}
			
			if (buffs.size() == 0)
			{
				msg = viewAllSchemeBuffs(player, eventParam1, "0");
			}
			else if (getpetbuff && !player.hasSummon())
			{
				sendErrorMessageToPlayer(player, new ServerMessage("CommunityBuffer.NOT_HAVE_SERVITOR", player.getLang()).toString());
				showCommunity(player, main(player));
				return;
			}
			else
			{
				if (!Config.FREE_ALL_BUFFS && (player.getLevel() > Config.COMMUNITY_FREE_BUFF_LVL))
				{
					final var price = buffs.size() * Config.BUFF_AMOUNT;
					if (player.getInventory().getItemByItemId(Config.BUFF_ID_ITEM) == null)
					{
						player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
						showCommunity(player, main(player));
						return;
					}
					
					if (player.getInventory().getItemByItemId(Config.BUFF_ID_ITEM).getCount() < price)
					{
						final ServerMessage message = new ServerMessage("CommunityBuffer.NECESSARY_ITEMS", player.getLang());
						message.add(price);
						message.add(Util.getItemName(player, Config.BUFF_ID_ITEM));
						sendErrorMessageToPlayer(player, message.toString());
						showCommunity(player, main(player));
						return;
					}
					
					player.destroyItemByItemId("SchemeBuffer", Config.BUFF_ID_ITEM, price, player, true);
				}
				
				double hp = 0.;
				double mp = 0.;
				double cp = 0.;
				double s_hp = 0.;
				double s_mp = 0.;
				final var canHeal = canHeal(player);
				if (!canHeal)
				{
					hp = player.getCurrentHp();
					mp = player.getCurrentMp();
					cp = player.getCurrentCp();
					if (player.hasSummon())
					{
						
						s_hp = player.getSummon().getCurrentHp();
						s_mp = player.getSummon().getCurrentMp();
					}
				}
				
				for (int i = 0; i < buffs.size(); ++i)
				{
					if (isItemRemoveSkill(player, buffs.get(i), levels.get(i)))
					{
						if (!checkItemsForSkill(player, buffs.get(i), levels.get(i), false))
						{
							continue;
						}
					}
					
					if (!getpetbuff)
					{
						final var skill = SkillsParser.getInstance().getInfo(buffs.get(i), levels.get(i));
						if (skill != null)
						{
							final var buffTime = getBuffTime(player, skill.getId(), skill.getLevel());
							if (buffTime > 0 && skill.hasEffects())
							{
								final var env = new Env();
								env.setCharacter(player);
								env.setTarget(player);
								env.setSkill(skill);
								
								Effect ef;
								for (final var et : skill.getEffectTemplates())
								{
									ef = et.getEffect(env);
									if (ef != null)
									{
										ef.setAbnormalTime(buffTime * 60);
										ef.scheduleEffect(true);
									}
								}
							}
							else
							{
								skill.getEffects(player, player, false);
							}
							
							if (Config.ALLOW_SUMMON_AUTO_BUFF && player.hasSummon())
							{
								if (buffTime > 0 && skill.hasEffects())
								{
									final var env = new Env();
									env.setCharacter(player);
									env.setTarget(player.getSummon());
									env.setSkill(skill);
									
									Effect ef;
									for (final var et : skill.getEffectTemplates())
									{
										ef = et.getEffect(env);
										if (ef != null)
										{
											ef.setAbnormalTime(buffTime * 60);
											ef.scheduleEffect(true);
										}
									}
								}
								else
								{
									skill.getEffects(player, player.getSummon(), false);
								}
							}
						}
					}
					else
					{
						if (player.hasSummon())
						{
							final var skill = SkillsParser.getInstance().getInfo(buffs.get(i), levels.get(i));
							if (skill != null)
							{
								final var buffTime = getBuffTime(player, skill.getId(), skill.getLevel());
								if (buffTime > 0 && skill.hasEffects())
								{
									final var env = new Env();
									env.setCharacter(player);
									env.setTarget(player.getSummon());
									env.setSkill(skill);
									
									Effect ef;
									for (final var et : skill.getEffectTemplates())
									{
										ef = et.getEffect(env);
										if (ef != null)
										{
											ef.setAbnormalTime(buffTime * 60);
											ef.scheduleEffect(true);
										}
									}
								}
								else
								{
									skill.getEffects(player, player.getSummon(), false);
								}
							}
						}
					}
				}
				
				if (!canHeal)
				{
					player.setCurrentHpMp(hp, mp);
					player.setCurrentCp(cp);
					if (player.hasSummon())
					{
						
						player.getSummon().setCurrentHpMp(s_hp, s_mp);
					}
				}
			}
			msg = main(player);
		}
		else if (eventParam0.equalsIgnoreCase("manage_scheme_1"))
		{
			if (Config.ALLOW_SCHEMES_FOR_PREMIUMS && !player.hasPremiumBonus())
			{
				sendErrorMessageToPlayer(player, new ServerMessage("CommunityBuffer.ONLY_FOR_PREMIUM", player.getLang()).toString());
				return;
			}
			msg = viewAllSchemeBuffs(player, eventParam1, eventParam2);
		}
		else if (eventParam0.equalsIgnoreCase("remove_buff"))
		{
			if (Config.ALLOW_SCHEMES_FOR_PREMIUMS && !player.hasPremiumBonus())
			{
				sendErrorMessageToPlayer(player, new ServerMessage("CommunityBuffer.ONLY_FOR_PREMIUM", player.getLang()).toString());
				return;
			}
			final var split = eventParam1.split("_");
			final var scheme = split[0];
			final var skill = split[1];
			final var level = split[2];
			
			CharSchemesHolder.getInstance().removeBuff(scheme, skill, level);
			
			final var skillId = Integer.parseInt(skill);
			for (final var buff : player.getBuffSchemeById(Integer.parseInt(scheme)).getBuffs())
			{
				if (buff.getSkillId() == skillId)
				{
					player.getBuffSchemeById(Integer.parseInt(scheme)).getBuffs().remove(buff);
					break;
				}
			}
			
			msg = viewAllSchemeBuffs(player, scheme, eventParam2);
		}
		else if (eventParam0.equalsIgnoreCase("add_buff"))
		{
			if (Config.ALLOW_SCHEMES_FOR_PREMIUMS && !player.hasPremiumBonus())
			{
				sendErrorMessageToPlayer(player, new ServerMessage("CommunityBuffer.ONLY_FOR_PREMIUM", player.getLang()).toString());
				return;
			}
			final var split = eventParam1.split("_");
			final var scheme = split[0];
			final var skill = split[1];
			final var level = split[2];
			final var premiumLvl = split[3];
			final var isDanceSlot = isDanceSlotBuff(player, Integer.parseInt(skill));
			
			if (!isValidSkill(player, Integer.parseInt(skill), Integer.parseInt(level)))
			{
				_log.warn("Player " + player.getName(null) + " try to cheat with Community Buffer bypass!");
				Util.handleIllegalPlayerAction(player, "" + player.getName(null) + " try to cheat with Community Buffer!");
				return;
			}
			
			CharSchemesHolder.getInstance().addBuff(scheme, skill, level, premiumLvl, isDanceSlot);
			player.getBuffSchemeById(Integer.parseInt(scheme)).getBuffs().add(new SchemeBuff(Integer.parseInt(skill), Integer.parseInt(level), Integer.parseInt(premiumLvl), isDanceSlot));
			
			msg = viewAllSchemeBuffs(player, scheme, eventParam2);
		}
		else if (eventParam0.equalsIgnoreCase("create"))
		{
			if (Config.ALLOW_SCHEMES_FOR_PREMIUMS && !player.hasPremiumBonus())
			{
				sendErrorMessageToPlayer(player, new ServerMessage("CommunityBuffer.ONLY_FOR_PREMIUM", player.getLang()).toString());
				return;
			}
			
			if (player.getBuffSchemes().size() >= Config.BUFF_MAX_SCHEMES)
			{
				sendErrorMessageToPlayer(player, new ServerMessage("CommunityBuffer.MAX_SCHEMES", player.getLang()).toString());
				return;
			}
			
			final var name = getCorrectName(eventParam2 + (eventParam3.equalsIgnoreCase("x") ? "" : " " + eventParam3));
			if (name.isEmpty() || name.equals("no_name"))
			{
				player.sendPacket(SystemMessageId.INCORRECT_NAME_TRY_AGAIN);
				sendErrorMessageToPlayer(player, new ServerMessage("CommunityBuffer.ENTER_NAME", player.getLang()).toString());
				return;
			}
			var printMain = false;
			var iconId = 0;
			try
			{
				iconId = Integer.parseInt(eventParam1);
				if (iconId == -1)
				{
					printMain = true;
					iconId = Rnd.get(SCHEME_ICONS.length - 1);
				}
				
				if (iconId < 0 || iconId > SCHEME_ICONS.length - 1)
				{
					throw new Exception();
				}
			}
			catch (final Exception e)
			{
				sendErrorMessageToPlayer(player, new ServerMessage("CommunityBuffer.WRONG_ICON", player.getLang()).toString());
				showCommunity(player, main(player));
				return;
			}
			
			Connection con = null;
			PreparedStatement statement = null;
			ResultSet rset = null;
			try
			{
				con = DatabaseFactory.getInstance().getConnection();
				statement = con.prepareStatement("INSERT INTO character_scheme_list (charId,scheme_name,icon) VALUES (?,?,?)", Statement.RETURN_GENERATED_KEYS);
				statement.setInt(1, player.getObjectId());
				statement.setString(2, name);
				statement.setInt(3, iconId);
				statement.executeUpdate();
				rset = statement.getGeneratedKeys();
				if (rset.next())
				{
					final var id = rset.getInt(1);
					player.getBuffSchemes().add(new PlayerScheme(id, name, iconId));
					addAllBuffsToScheme(player, id);
					if (!printMain)
					{
						msg = getOptionList(player, id);
					}
					else
					{
						msg = main(player);
					}
				}
				else
				{
					_log.warn("Couldn't get Generated Key while creating scheme!");
				}
			}
			catch (final SQLException e)
			{
				_log.warn("Error while inserting Scheme List", e);
				msg = main(player);
			}
			finally
			{
				DbUtils.closeQuietly(con, statement, rset);
			}
		}
		else if (eventParam0.equalsIgnoreCase("delete"))
		{
			if (Config.ALLOW_SCHEMES_FOR_PREMIUMS && !player.hasPremiumBonus())
			{
				sendErrorMessageToPlayer(player, new ServerMessage("CommunityBuffer.ONLY_FOR_PREMIUM", player.getLang()).toString());
				return;
			}
			final var schemeId = Integer.parseInt(eventParam1);
			final var scheme = player.getBuffSchemeById(schemeId);
			if (scheme == null)
			{
				sendErrorMessageToPlayer(player, new ServerMessage("CommunityBuffer.INVALID_SCHEME", player.getLang()).toString());
				showCommunity(player, main(player));
				return;
			}
			
			askQuestion(player, schemeId, scheme.getName());
			
			msg = main(player);
		}
		else if (eventParam0.equalsIgnoreCase("create_1"))
		{
			if (Config.ALLOW_SCHEMES_FOR_PREMIUMS && !player.hasPremiumBonus())
			{
				sendErrorMessageToPlayer(player, new ServerMessage("CommunityBuffer.ONLY_FOR_PREMIUM", player.getLang()).toString());
				return;
			}
			
			if (player.getBuffSchemes().size() >= Config.BUFF_MAX_SCHEMES)
			{
				sendErrorMessageToPlayer(player, new ServerMessage("CommunityBuffer.MAX_SCHEMES", player.getLang()).toString());
				showCommunity(player, main(player));
				return;
			}
			
			msg = createScheme(player, Integer.parseInt(eventParam1));
		}
		else if (eventParam0.equalsIgnoreCase("edit_1"))
		{
			if (Config.ALLOW_SCHEMES_FOR_PREMIUMS && !player.hasPremiumBonus())
			{
				sendErrorMessageToPlayer(player, new ServerMessage("CommunityBuffer.ONLY_FOR_PREMIUM", player.getLang()).toString());
				return;
			}
			msg = getEditSchemePage(player);
		}
		else if (eventParam0.equalsIgnoreCase("delete_1"))
		{
			if (Config.ALLOW_SCHEMES_FOR_PREMIUMS && !player.hasPremiumBonus())
			{
				sendErrorMessageToPlayer(player, new ServerMessage("CommunityBuffer.ONLY_FOR_PREMIUM", player.getLang()).toString());
				return;
			}
			msg = getDeleteSchemePage(player);
		}
		else if (eventParam0.equalsIgnoreCase("manage_scheme_select"))
		{
			if (Config.ALLOW_SCHEMES_FOR_PREMIUMS && !player.hasPremiumBonus())
			{
				sendErrorMessageToPlayer(player, new ServerMessage("CommunityBuffer.ONLY_FOR_PREMIUM", player.getLang()).toString());
				return;
			}
			msg = getOptionList(player, Integer.parseInt(eventParam1));
		}
		else if (eventParam0.equalsIgnoreCase("giveBuffSet"))
		{
			if (player.isBlocked())
			{
				return;
			}
			
			if (!player.checkFloodProtection("BUFFSDELAY", "buffs_delay"))
			{
				return;
			}
			
			final int groupId = Math.max(1, getGroupId(eventParam1));
			final List<SingleBuff> buff_sets = getSetBuffs(groupId);
			final RequestItems requestTemplate = getRequestTemplate(groupId);
			
			final var getpetbuff = isPetBuff(player);
			
			if (buff_sets != null && !buff_sets.isEmpty())
			{
				if (!Config.FREE_ALL_BUFFS && (player.getLevel() > Config.COMMUNITY_FREE_BUFF_LVL))
				{
					if (requestTemplate != null && requestTemplate.isBuffForItems())
					{
						if (requestTemplate.needAllItems())
						{
							for (final int it[] : requestTemplate.getRequestItems())
							{
								if ((it == null) || (it.length != 2))
								{
									continue;
								}
								
								if (player.getInventory().getItemByItemId(it[0]) == null || player.getInventory().getItemByItemId(it[0]).getCount() < it[1])
								{
									final var message = new ServerMessage("CommunityBuffer.NECESSARY_ITEMS", player.getLang());
									message.add(it[1]);
									message.add(Util.getItemName(player, it[0]));
									player.sendPacket(new CreatureSay(player.getObjectId(), Say2.CRITICAL_ANNOUNCE, ServerStorage.getInstance().getString(player.getLang(), "CommunityBuffer.ERROR"), message.toString()));
									return;
								}
							}
							
							if (requestTemplate.isRemoveItems())
							{
								for (final int it[] : requestTemplate.getRequestItems())
								{
									if ((it == null) || (it.length != 2))
									{
										continue;
									}
									player.destroyItemByItemId("RequestItemsBuff", it[0], it[1], player, true);
								}
							}
						}
						else
						{
							var foundItem = false;
							final var needRemove = requestTemplate.isRemoveItems();
							for (final int it[] : requestTemplate.getRequestItems())
							{
								if ((it == null) || (it.length != 2))
								{
									continue;
								}
								
								if (player.getInventory().getItemByItemId(it[0]) != null && player.getInventory().getItemByItemId(it[0]).getCount() >= it[1])
								{
									foundItem = true;
									if (needRemove)
									{
										player.destroyItemByItemId("RequestItemsBuff", it[0], it[1], player, true);
									}
									break;
								}
							}
							
							if (!foundItem)
							{
								player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
								return;
							}
						}
					}
				}
				
				if (!getpetbuff)
				{
					if (BotFunctions.getInstance().isAutoBuffEnable(player))
					{
						BotFunctions.getInstance().getAutoBuffSet(player);
						return;
					}
					
					double hp = 0.;
					double mp = 0.;
					double cp = 0.;
					double s_hp = 0.;
					double s_mp = 0.;
					final var canHeal = canHeal(player);
					if (!canHeal)
					{
						hp = player.getCurrentHp();
						mp = player.getCurrentMp();
						cp = player.getCurrentCp();
						if (player.hasSummon() && Config.ALLOW_SUMMON_AUTO_BUFF)
						{
							
							s_hp = player.getSummon().getCurrentHp();
							s_mp = player.getSummon().getCurrentMp();
						}
					}
					
					for (final var singleBuff : buff_sets)
					{
						final var skillLvl = player.hasPremiumBonus() ? singleBuff.getPremiumLevel() : singleBuff.getLevel();
						final var skill = SkillsParser.getInstance().getInfo(singleBuff.getSkillId(), skillLvl);
						if (skill != null)
						{
							final var buffTime = getBuffTime(player, skill.getId(), skill.getLevel());
							if (buffTime > 0 && skill.hasEffects())
							{
								final var env = new Env();
								env.setCharacter(player);
								env.setTarget(player);
								env.setSkill(skill);
								
								Effect ef;
								for (final var et : skill.getEffectTemplates())
								{
									ef = et.getEffect(env);
									if (ef != null)
									{
										ef.setAbnormalTime(buffTime * 60);
										ef.scheduleEffect(true);
									}
								}
							}
							else
							{
								skill.getEffects(player, player, false);
							}
							
							if (Config.ALLOW_SUMMON_AUTO_BUFF && player.hasSummon())
							{
								if (buffTime > 0 && skill.hasEffects())
								{
									final var env = new Env();
									env.setCharacter(player);
									env.setTarget(player.getSummon());
									env.setSkill(skill);
									
									Effect ef;
									for (final var et : skill.getEffectTemplates())
									{
										ef = et.getEffect(env);
										if (ef != null)
										{
											ef.setAbnormalTime(buffTime * 60);
											ef.scheduleEffect(true);
										}
									}
								}
								else
								{
									skill.getEffects(player, player.getSummon(), false);
								}
							}
						}
					}
					
					if (!canHeal)
					{
						player.setCurrentHpMp(hp, mp);
						player.setCurrentCp(cp);
						if (player.hasSummon() && Config.ALLOW_SUMMON_AUTO_BUFF)
						{
							
							player.getSummon().setCurrentHpMp(s_hp, s_mp);
						}
					}
				}
				else
				{
					if (player.hasSummon())
					{
						double hp = 0.;
						double mp = 0.;
						final var canHeal = canHeal(player);
						if (!canHeal)
						{
							hp = player.getSummon().getCurrentHp();
							mp = player.getSummon().getCurrentMp();
						}
						
						for (final var singleBuff : buff_sets)
						{
							final var skillLvl = player.hasPremiumBonus() ? singleBuff.getPremiumLevel() : singleBuff.getLevel();
							final var skill = SkillsParser.getInstance().getInfo(singleBuff.getSkillId(), skillLvl);
							if (skill != null)
							{
								final var buffTime = getBuffTime(player, skill.getId(), skill.getLevel());
								if (buffTime > 0 && skill.hasEffects())
								{
									final var env = new Env();
									env.setCharacter(player);
									env.setTarget(player.getSummon());
									env.setSkill(skill);
									
									Effect ef;
									for (final var et : skill.getEffectTemplates())
									{
										ef = et.getEffect(env);
										if (ef != null)
										{
											ef.setAbnormalTime(buffTime * 60);
											ef.scheduleEffect(true);
										}
									}
								}
								else
								{
									skill.getEffects(player, player.getSummon(), false);
								}
							}
						}
						
						if (!canHeal)
						{
							player.getSummon().setCurrentHpMp(hp, mp);
						}
					}
					else
					{
						sendErrorMessageToPlayer(player, new ServerMessage("CommunityBuffer.NOT_HAVE_SERVITOR", player.getLang()).toString());
						showCommunity(player, main(player));
						return;
					}
				}
			}
			msg = main(player);
		}
		else if (eventParam0.equalsIgnoreCase("changeName_1"))
		{
			var dialog = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/buffer/buffer_scheme_change_name.htm");
			
			if (isPetBuff(player))
			{
				dialog = dialog.replace("%topbtn%", (player.hasSummon() ? player.getSummon().getSummonName(player.getLang()) : ServerStorage.getInstance().getString(player.getLang(), "CommunityBuffer.DONT_HAVE_PET")));
			}
			else
			{
				dialog = dialog.replace("%topbtn%", player.getName(null));
			}
			
			dialog = dialog.replace("%schemeId%", eventParam1);
			dialog = dialog.replace("%schemePart%", generateScheme(player));
			msg = dialog;
		}
		else if (eventParam0.equalsIgnoreCase("changeName"))
		{
			final var schemeId = Integer.parseInt(eventParam1);
			final var scheme = player.getBuffSchemeById(schemeId);
			if (scheme == null)
			{
				sendErrorMessageToPlayer(player, new ServerMessage("CommunityBuffer.INVALID_SCHEME", player.getLang()).toString());
				showCommunity(player, main(player));
				return;
			}
			
			final var name = getCorrectName(eventParam2 + (eventParam3.equalsIgnoreCase("x") ? "" : " " + eventParam3));
			if (name.isEmpty() || name.equals("no_name"))
			{
				player.sendPacket(SystemMessageId.INCORRECT_NAME_TRY_AGAIN);
				sendErrorMessageToPlayer(player, new ServerMessage("CommunityBuffer.ENTER_NAME", player.getLang()).toString());
				showCommunity(player, getOptionList(player, schemeId));
				return;
			}
			
			scheme.setName(name);
			CharSchemesHolder.getInstance().updateScheme(name, schemeId);
			sendErrorMessageToPlayer(player, new ServerMessage("CommunityBuffer.NAME_CHANGE_SUCCESS", player.getLang()).toString());
			
			msg = getOptionList(player, schemeId);
		}
		else if (eventParam0.equalsIgnoreCase("changeIcon_1"))
		{
			msg = changeSchemeIcon(player, Integer.parseInt(eventParam1));
		}
		else if (eventParam0.equalsIgnoreCase("changeIcon"))
		{
			final var schemeId = Integer.parseInt(eventParam1);
			final var scheme = player.getBuffSchemeById(schemeId);
			if (scheme == null)
			{
				sendErrorMessageToPlayer(player, new ServerMessage("CommunityBuffer.INVALID_SCHEME", player.getLang()).toString());
				showCommunity(player, main(player));
				return;
			}
			
			var iconId = 0;
			try
			{
				iconId = Integer.parseInt(eventParam2);
				if (iconId < 0 || iconId > SCHEME_ICONS.length - 1)
				{
					throw new Exception();
				}
			}
			catch (final Exception e)
			{
				sendErrorMessageToPlayer(player, new ServerMessage("CommunityBuffer.WRONG_ICON", player.getLang()).toString());
				showCommunity(player, getOptionList(player, schemeId));
				return;
			}
			
			scheme.setIcon(iconId);
			CharSchemesHolder.getInstance().updateIcon(iconId, schemeId);
			sendErrorMessageToPlayer(player, new ServerMessage("CommunityBuffer.ICON_CHANGE_SUCCESS", player.getLang()).toString());
			msg = getOptionList(player, schemeId);
		}
		showCommunity(player, msg);
	}
	
	private void setPetBuff(Player player, String eventParam1)
	{
		player.addQuickVar("SchemeBufferPet", Integer.valueOf(eventParam1));
	}
	
	private static boolean isPetBuff(Player player)
	{
		final int value = player.getQuickVarI("SchemeBufferPet");
		return value > 0;
	}
	
	public void showWindow(Player player)
	{
		showCommunity(player, main(player));
	}
	
	public static String main(final Player player)
	{
		var dialog = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/buffer/buffer_main.htm");
		var button = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/buffer/buffer_main_button.htm");
		
		if (isPetBuff(player))
		{
			button = button.replace("%name%", (player.hasSummon() ? player.getSummon().getSummonName(player.getLang()) : ServerStorage.getInstance().getString(player.getLang(), "CommunityBuffer.DONT_HAVE_PET")));
			button = button.replace("%bypass%", "_bbsbufferbypass_buffpet 0 0 0");
		}
		else
		{
			button = button.replace("%name%", player.getName(null));
			button = button.replace("%bypass%", "_bbsbufferbypass_buffpet 1 0 0");
		}
		dialog = dialog.replace("%topbtn%", button);
		dialog = dialog.replace("%schemePart%", generateScheme(player));
		dialog = dialog.replace("\r\n", "");
		dialog = dialog.replace("\t", "");
		
		return dialog;
	}
	
	private String viewAllSchemeBuffs(Player player, String scheme, String page)
	{
		final var pageN = Integer.parseInt(page);
		final var schemeId = Integer.parseInt(scheme);
		var dialog = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/buffer/buffer_scheme_buffs.htm");
		final var button = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/buffer/scheme_button.htm");
		
		final var buffCount = getBuffCount(player, schemeId);
		final var TOTAL_BUFF = buffCount[0];
		final var BUFF_COUNT = buffCount[1];
		final var DANCE_SONG = buffCount[2];
		
		if (isPetBuff(player))
		{
			dialog = dialog.replace("%topbtn%", (player.hasSummon() ? player.getSummon().getSummonName(player.getLang()) : ServerStorage.getInstance().getString(player.getLang(), "CommunityBuffer.DONT_HAVE_PET")));
		}
		else
		{
			dialog = dialog.replace("%topbtn%", player.getName(null));
		}
		
		dialog = dialog.replace("%bcount%", String.valueOf(player.getMaxBuffCount() - BUFF_COUNT));
		dialog = dialog.replace("%dscount%", String.valueOf(Config.DANCES_MAX_AMOUNT - DANCE_SONG));
		
		final List<SchemeBuff> schemeBuffs = new ArrayList<>();
		final List<SchemeBuff> schemeDances = new ArrayList<>();
		for (final SchemeBuff buff : player.getBuffSchemeById(schemeId).getBuffs())
		{
			switch (getBuffType(player, buff.getSkillId()))
			{
				case "song" :
				case "dance" :
					schemeDances.add(buff);
					break;
				case "none" :
					break;
				default :
					schemeBuffs.add(buff);
					break;
			}
		}
		
		final var MAX_ROW_SIZE = 16;
		final var maxBuffs = player.getMaxBuffCount();
		final int rowsAmount = (int) Math.floor(maxBuffs / MAX_ROW_SIZE);
		final var lastBuffs = (maxBuffs - (MAX_ROW_SIZE * rowsAmount));
		final var danceAmount = Config.DANCES_MAX_AMOUNT;
		final var rowsDanceAmount = (int) Math.floor(danceAmount / MAX_ROW_SIZE);
		final var lastDance = (danceAmount - (MAX_ROW_SIZE * rowsDanceAmount));
		final var isChangeAmount = rowsAmount > 2 || (rowsAmount >= 2 && rowsDanceAmount > 0);
		final int[] ROW_SIZES = new int[rowsAmount + (lastBuffs > 0 ? 1 : 0) + (rowsDanceAmount > 0 ? rowsDanceAmount + 1 : 1)];
		
		int i = -1;
		for (int b = 0; b < rowsAmount; b++)
		{
			i++;
			ROW_SIZES[i] = i > 0 ? ROW_SIZES[i - 1] + MAX_ROW_SIZE : MAX_ROW_SIZE;
		}
		
		if (lastBuffs > 0)
		{
			i++;
			ROW_SIZES[i] = ROW_SIZES[i - 1] + lastBuffs;
		}
		
		if (danceAmount > 0)
		{
			if (rowsDanceAmount > 0)
			{
				for (int d = 0; d < rowsDanceAmount; d++)
				{
					i++;
					ROW_SIZES[i] = ROW_SIZES[i - 1] + MAX_ROW_SIZE;
				}
				
				if (lastDance > 0)
				{
					i++;
					ROW_SIZES[i] = ROW_SIZES[i - 1] + lastDance;
				}
			}
			else
			{
				i++;
				ROW_SIZES[i] = ROW_SIZES[i - 1] + danceAmount;
			}
		}
		
		final var addedBuffs = new StringBuilder();
		var row = 0;
		final var rowAll = ROW_SIZES.length - 1;
		final var rowBuffs = rowsAmount + (lastBuffs > 0 ? 1 : 0);
		for (i = 0; i < ROW_SIZES[rowAll]; i++)
		{
			if (i == 0 || (i + 1) - ROW_SIZES[Math.max(row - 1, 0)] == 1)
			{
				addedBuffs.append("<tr>");
			}
			
			var info = button;
			
			if (row < rowBuffs && schemeBuffs.size() > i)
			{
				final var skill = SkillsParser.getInstance().getInfo(schemeBuffs.get(i).getSkillId(), schemeBuffs.get(i).getLevel());
				info = info.replace("%icon%", skill.getIcon());
				info = info.replace("%bypass%", "bypass -h _bbsbufferbypass_remove_buff " + schemeId + "_" + skill.getId() + "_" + skill.getLevel() + " " + pageN + " x");
			}
			else if (row >= rowBuffs && (i - maxBuffs) < schemeDances.size())
			{
				final int pos = (i - maxBuffs);
				final var skill = SkillsParser.getInstance().getInfo(schemeDances.get(pos).getSkillId(), schemeDances.get(pos).getLevel());
				info = info.replace("%icon%", skill.getIcon());
				info = info.replace("%bypass%", "bypass -h _bbsbufferbypass_remove_buff " + schemeId + "_" + skill.getId() + "_" + skill.getLevel() + " " + pageN + " x");
			}
			else
			{
				info = info.replace("%icon%", "L2UI_CH3.multisell_plusicon");
				info = info.replace("%bypass%", "");
			}
			
			addedBuffs.append(info);
			
			if (ROW_SIZES[row] < MAX_ROW_SIZE * (row + 1) && i + 1 > ROW_SIZES[row])
			{
				for (int z = ROW_SIZES[row]; z < MAX_ROW_SIZE * (row + 1); z++)
				{
					addedBuffs.append("<td width=1>");
					addedBuffs.append("&nbsp;");
					addedBuffs.append("</td>");
				}
			}
			
			if ((i + 1) - ROW_SIZES[row] == 0)
			{
				addedBuffs.append("</tr>");
				row++;
			}
		}
		
		final List<Skill> availableSkills = new ArrayList<>();
		for (final var singleBuff : _allSingleBuffs)
		{
			boolean hasAddedThisBuff = false;
			for (final var buff : schemeBuffs)
			{
				if (buff.getSkillId() == singleBuff.getSkillId())
				{
					hasAddedThisBuff = true;
					break;
				}
			}
			for (final var buff : schemeDances)
			{
				if (buff.getSkillId() == singleBuff.getSkillId())
				{
					hasAddedThisBuff = true;
					break;
				}
			}
			if (hasAddedThisBuff)
			{
				continue;
			}
			
			switch (singleBuff.getBuffType())
			{
				case "song" :
				case "dance" :
					if (DANCE_SONG >= danceAmount)
					{
						continue;
					}
					break;
				case "none" :
					break;
				default :
					if (BUFF_COUNT >= player.getMaxBuffCount())
					{
						continue;
					}
					break;
			}
			availableSkills.add(SkillsParser.getInstance().getInfo(singleBuff.getSkillId(), singleBuff.getLevel()));
		}
		
		final var SKILLS_PER_ROW = 4;
		final var MAX_SKILLS_ROWS = isChangeAmount ? 2 : 3;
		
		final var availableBuffs = new StringBuilder();
		final var template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/buffer/scheme_buff_template.htm");
		final var templateEmpty = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/buffer/scheme_buff_template_empty.htm");
		final var maxPage = (int) Math.ceil((double) availableSkills.size() / (SKILLS_PER_ROW * MAX_SKILLS_ROWS) - 1);
		final var currentPage = Math.max(Math.min(maxPage, pageN), 0);
		final var startIndex = currentPage * SKILLS_PER_ROW * MAX_SKILLS_ROWS;
		for (i = startIndex; i < startIndex + SKILLS_PER_ROW * MAX_SKILLS_ROWS; i++)
		{
			if (i == 0 || i % SKILLS_PER_ROW == 0)
			{
				availableBuffs.append("<tr>");
			}
			
			if (availableSkills.size() > i)
			{
				final var skill = availableSkills.get(i);
				final var skillName = skill.getName(player.getLang());
				
				var info = template;
				info = info.replace("%icon%", skill.getIcon());
				info = info.replace("%skillName%", skillName);
				info = info.replace("%scheme%", scheme);
				info = info.replace("%skillId%", String.valueOf(skill.getId()));
				info = info.replace("%skillLevel%", String.valueOf(skill.getLevel()));
				info = info.replace("%premLevel%", String.valueOf(getPremiumLvl(skill.getId(), skill.getLevel())));
				info = info.replace("%curPage%", String.valueOf(currentPage));
				info = info.replace("%total%", String.valueOf(TOTAL_BUFF));
				availableBuffs.append(info);
			}
			else
			{
				final var info = templateEmpty;
				availableBuffs.append(info);
			}
			
			if ((i + 1) % SKILLS_PER_ROW == 0 || (i - startIndex) >= SKILLS_PER_ROW * MAX_SKILLS_ROWS)
			{
				availableBuffs.append("</tr>");
			}
		}
		
		dialog = dialog.replace("%scheme%", scheme);
		dialog = dialog.replace("%addedBuffs%", addedBuffs.toString());
		dialog = dialog.replace("%availableBuffs%", availableBuffs.toString());
		dialog = dialog.replace("%prevPage%", (currentPage > 0 ? "bypass -h _bbsbufferbypass_manage_scheme_1 " + scheme + " " + (currentPage - 1) + " x" : ""));
		dialog = dialog.replace("%nextPage%", (currentPage < maxPage ? "bypass -h _bbsbufferbypass_manage_scheme_1 " + scheme + " " + (currentPage + 1) + " x" : ""));
		dialog = dialog.replace("\r\n", "");
		dialog = dialog.replace("\t", "");
		return dialog;
	}
	
	public boolean canHeal(Player player)
	{
		if (player.isInPartyTournament() && !player.getPartyTournament().isPrepare())
		{
			return false;
		}
		if (player.isInFightEvent() && player.getFightEvent().getState() != AbstractFightEvent.EVENT_STATE.PREPARATION)
		{
			return false;
		}
		if (!player.isInPartyTournament() && !player.isInFightEvent() && (Config.ALLOW_HEAL_ONLY_PEACE && !player.isInsideZone(ZoneId.PEACE) && !player.isInSiege()))
		{
			return false;
		}
		return true;
	}
	
	private void heal(Player player, boolean isPet)
	{
		if (!canHeal(player))
		{
			return;
		}
		if (!isPet)
		{
			player.setCurrentHp(player.getMaxHp());
			player.setCurrentMp(player.getMaxMp());
			player.setCurrentCp(player.getMaxCp());
			player.broadcastPacket(new MagicSkillUse(player, player, 22217, 1, 0, 0));
			if (Config.ALLOW_SUMMON_AUTO_BUFF && player.hasSummon())
			{
				final Summon pet = player.getSummon();
				pet.setCurrentHp(pet.getMaxHp());
				pet.setCurrentMp(pet.getMaxMp());
				pet.setCurrentCp(pet.getMaxCp());
				pet.broadcastPacket(new MagicSkillUse(pet, 22217, 1, 0, 0));
			}
		}
		else if (player.hasSummon())
		{
			final Summon pet = player.getSummon();
			pet.setCurrentHp(pet.getMaxHp());
			pet.setCurrentMp(pet.getMaxMp());
			pet.setCurrentCp(pet.getMaxCp());
			pet.broadcastPacket(new MagicSkillUse(pet, 22217, 1, 0, 0));
		}
	}
	
	private String getDeleteSchemePage(Player player)
	{
		final var builder = new StringBuilder();
		var dialog = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/buffer/buffer_scheme_delete.htm");
		final var button = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/buffer/buffer_scheme_button.htm");
		
		for (final var scheme : player.getBuffSchemes())
		{
			var info = button;
			info = info.replace("%name%", scheme.getName());
			info = info.replace("%id%", String.valueOf(scheme.getSchemeId()));
			builder.append(info);
		}
		dialog = dialog.replace("%schemes%", builder.toString());
		return dialog;
	}
	
	private String buildHtml(String buffType, Player player, int curPage)
	{
		var dialog = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/buffer/buffer_scheme_indbuffs.htm");
		final var template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/buffer/indbuffs-template.htm");
		var block = "";
		var list = "";
		
		final List<String> availableBuffs = new ArrayList<>();
		final var buffList = _typeBuffs.get(buffType);
		if (buffList != null)
		{
			for (final var buff : buffList)
			{
				if (buff.getBuffType().equals(buffType))
				{
					var bName = SkillsParser.getInstance().getInfo(buff.getSkillId(), buff.getLevel()).getName(player.getLang());
					bName = bName.replace(" ", "+");
					final var skillLvl = player.hasPremiumBonus() ? buff.getPremiumLevel() : buff.getLevel();
					availableBuffs.add(bName + "_" + buff.getSkillId() + "_" + skillLvl);
				}
			}
		}
		
		final var perpage = Config.BUFFS_PER_PAGE;
		final var isThereNextPage = availableBuffs.size() > perpage;
		var counter = 0;
		final var page = curPage > 0 ? curPage : 1;
		
		if (availableBuffs.isEmpty())
		{
			list += ServerStorage.getInstance().getString(player.getLang(), "CommunityBuffer.NO_BUFFS");
		}
		else
		{
			var index = 0;
			for (int i = (page - 1) * perpage; i < availableBuffs.size(); i++)
			{
				block = template;
				
				final var buff = availableBuffs.get(i).replace("_", " ");
				final var buffSplit = buff.split(" ");
				var name = buffSplit[0];
				final var id = Integer.parseInt(buffSplit[1]);
				final var level = Integer.parseInt(buffSplit[2]);
				name = name.replace("+", " ");
				final var skill = SkillsParser.getInstance().getInfo(id, level);
				block = block.replace("%icon%", "<img width=32 height=32 src=\"" + skill.getIcon() + "\">");
				block = block.replace("%iconImg%", skill.getIcon());
				block = block.replace("%name%", name);
				block = block.replace("%id%", String.valueOf(id));
				block = block.replace("%lvl%", String.valueOf(level));
				block = block.replace("%type%", buffType);
				block = block.replace("%page%", String.valueOf(page));
				
				index++;
				if (index % 2 == 0)
				{
					if (index > 0)
					{
						block += "</tr>";
					}
					block += "<tr>";
				}
				list += block;
				
				counter++;
				if (counter >= perpage)
				{
					break;
				}
			}
		}
		
		final var pages = (double) availableBuffs.size() / perpage;
		final var count = (int) Math.ceil(pages);
		dialog = dialog.replace("%navigation%", Util.getNavigationBlock(count, page, availableBuffs.size(), perpage, isThereNextPage, "_bbsbufferbypass_redirect view_" + buffType + " %s 0"));
		dialog = dialog.replace("%buffs%", list);
		dialog = dialog.replace("%schemePart%", generateScheme(player));
		return dialog;
	}
	
	private String getEditSchemePage(Player player)
	{
		final var builder = new StringBuilder();
		var dialog = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/buffer/buffer_scheme_menu.htm");
		final var button = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/buffer/buffer_scheme_menu_button.htm");
		
		for (final var scheme : player.getBuffSchemes())
		{
			var info = button;
			info = info.replace("%name%", scheme.getName());
			info = info.replace("%id%", String.valueOf(scheme.getSchemeId()));
			builder.append(info);
		}
		
		dialog = dialog.replace("%schemes%", builder.toString());
		return dialog;
	}
	
	private int[] getBuffCount(Player player, int schemeId)
	{
		var count = 0;
		var dances = 0;
		var buffs = 0;
		
		for (final var buff : player.getBuffSchemeById(schemeId).getBuffs())
		{
			++count;
			if (buff.isDanceSlot())
			{
				++dances;
			}
			else
			{
				++buffs;
			}
		}
		
		return new int[]
		{
		        count, buffs, dances
		};
	}
	
	private String getOptionList(Player player, int schemeId)
	{
		final var scheme = player.getBuffSchemeById(schemeId);
		final var buffCount = getBuffCount(player, schemeId);
		var dialog = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/buffer/buffer_scheme_options.htm");
		
		if (isPetBuff(player))
		{
			dialog = dialog.replace("%topbtn%", (player.hasSummon() ? player.getSummon().getSummonName(player.getLang()) : ServerStorage.getInstance().getString(player.getLang(), "CommunityBuffer.DONT_HAVE_PET")));
		}
		else
		{
			dialog = dialog.replace("%topbtn%", player.getName(null));
		}
		
		dialog = dialog.replace("%name%", (scheme != null ? scheme.getName() : ""));
		dialog = dialog.replace("%bcount%", String.valueOf(buffCount[1]));
		dialog = dialog.replace("%dscount%", String.valueOf(buffCount[2]));
		
		dialog = dialog.replace("%manageBuffs%", "bypass -h _bbsbufferbypass_manage_scheme_1 " + schemeId + " 0 x");
		dialog = dialog.replace("%changeName%", "bypass -h _bbsbufferbypass_changeName_1 " + schemeId + " x x");
		dialog = dialog.replace("%changeIcon%", "bypass -h _bbsbufferbypass_changeIcon_1 " + schemeId + " x x");
		dialog = dialog.replace("%deleteScheme%", "bypass -h _bbsbufferbypass_delete " + schemeId + " x x");
		dialog = dialog.replace("%schemePart%", generateScheme(player));
		return dialog;
	}
	
	private String createScheme(Player player, int iconId)
	{
		var dialog = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/buffer/buffer_scheme_create.htm");
		
		if (isPetBuff(player))
		{
			dialog = dialog.replace("%topbtn%", (player.hasSummon() ? player.getSummon().getSummonName(player.getLang()) : ServerStorage.getInstance().getString(player.getLang(), "CommunityBuffer.DONT_HAVE_PET")));
		}
		else
		{
			dialog = dialog.replace("%topbtn%", player.getName(null));
		}
		
		final var icons = new StringBuilder();
		final var MAX_ICONS_PER_ROW = 17;
		
		for (int i = 0; i < SCHEME_ICONS.length; i++)
		{
			if (i == 0 || (i + 1) % MAX_ICONS_PER_ROW == 1)
			{
				icons.append("<tr>");
			}
			
			icons.append("<td width=60 align=center valign=top>");
			icons.append("<table border=0 cellspacing=0 cellpadding=0 width=32 height=32 background=" + SCHEME_ICONS[i] + ">");
			icons.append("<tr>");
			icons.append("<td width=32 height=32 align=center valign=top>");
			if (iconId == i)
			{
				icons.append("<table cellspacing=0 cellpadding=0 width=34 height=34 background=L2UI_CT1.ItemWindow_DF_Frame_Over>");
				icons.append("<tr><td align=left>");
				icons.append("&nbsp;");
				icons.append("</td></tr>");
				icons.append("</table>");
			}
			else
			{
				icons.append("<button action=\"bypass -h _bbsbufferbypass_create_1 " + i + " x x\" width=34 height=34 back=L2UI_CT1.ItemWindow_DF_Frame_Down fore=L2UI_CT1.ItemWindow_DF_Frame />");
			}
			icons.append("</td>");
			icons.append("</tr>");
			icons.append("</table>");
			icons.append("</td>");
			
			if ((i + 1) == SCHEME_ICONS.length || (i + 1) % MAX_ICONS_PER_ROW == 0)
			{
				icons.append("</tr>");
			}
		}
		dialog = dialog.replace("%iconList%", icons.toString());
		dialog = dialog.replace("%iconId%", String.valueOf(iconId));
		
		return dialog;
	}
	
	private String changeSchemeIcon(Player player, int schemeId)
	{
		var dialog = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/buffer/buffer_scheme_change_icon.htm");
		final var button = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/buffer/buffer_scheme_change_icon_button.htm");
		
		if (isPetBuff(player))
		{
			dialog = dialog.replace("%topbtn%", (player.hasSummon() ? player.getSummon().getSummonName(player.getLang()) : ServerStorage.getInstance().getString(player.getLang(), "CommunityBuffer.DONT_HAVE_PET")));
		}
		else
		{
			dialog = dialog.replace("%topbtn%", player.getName(null));
		}
		
		final var icons = new StringBuilder();
		final var MAX_ICONS_PER_ROW = 17;
		
		for (int i = 0; i < SCHEME_ICONS.length; i++)
		{
			if (i == 0 || (i + 1) % MAX_ICONS_PER_ROW == 1)
			{
				icons.append("<tr>");
			}
			
			var info = button;
			info = info.replace("%icon%", SCHEME_ICONS[i]);
			info = info.replace("%id%", String.valueOf(schemeId));
			info = info.replace("%is%", String.valueOf(i));
			icons.append(info);
			
			if ((i + 1) == SCHEME_ICONS.length || (i + 1) % MAX_ICONS_PER_ROW == 0)
			{
				icons.append("</tr>");
			}
		}
		dialog = dialog.replace("%iconList%", icons.toString());
		dialog = dialog.replace("%schemeId%", String.valueOf(schemeId));
		return dialog;
	}
	
	private static String generateScheme(Player player)
	{
		var html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/buffer/scheme_main.htm");
		final var template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/buffer/scheme_template.htm");
		final var space = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/buffer/scheme_space.htm");
		var block = "";
		var list = "";
		
		final Iterator<PlayerScheme> it = player.getBuffSchemes().iterator();
		for (int i = 0; i < Config.BUFF_MAX_SCHEMES; i++)
		{
			if (it.hasNext())
			{
				final var scheme = it.next();
				var buffCount = 0;
				for (final var buff : scheme.getBuffs())
				{
					if (buff != null)
					{
						buffCount++;
					}
				}
				final var price = buffCount * Config.BUFF_AMOUNT;
				block = template;
				block = block.replace("%icon%", SCHEME_ICONS[scheme.getIconId()]);
				block = block.replace("%schemeId%", String.valueOf(scheme.getSchemeId()));
				block = block.replace("%schemeName%", scheme.getName());
				block = block.replace("%price%", String.valueOf(price));
				list += block;
			}
			else
			{
				list += space;
			}
		}
		html = html.replace("%list%", list);
		return html;
	}
	
	public String getBuffType(Player player, int id)
	{
		for (final var singleBuff : _allSingleBuffs)
		{
			if (singleBuff.getSkillId() == id)
			{
				return singleBuff.getBuffType();
			}
		}
		return "none";
	}
	
	private boolean isDanceSlotBuff(Player player, int id)
	{
		for (final var singleBuff : _allSingleBuffs)
		{
			if (singleBuff.getSkillId() == id)
			{
				return singleBuff.isDanceSlot();
			}
		}
		return false;
	}
	
	public void sendErrorMessageToPlayer(Player player, String msg)
	{
		player.sendPacket(new CreatureSay(player.getObjectId(), Say2.CRITICAL_ANNOUNCE, ServerStorage.getInstance().getString(player.getLang(), "CommunityBuffer.ERROR"), msg));
	}
	
	public void showCommunity(Player player, String text)
	{
		if (text != null)
		{
			separateAndSend(text, player);
		}
	}
	
	private String getCorrectName(String currentName)
	{
		final var newNameBuilder = new StringBuilder();
		final var chars = currentName.toCharArray();
		for (final var c : chars)
		{
			if (isCharFine(c))
			{
				newNameBuilder.append(c);
			}
		}
		return newNameBuilder.toString();
	}
	
	private static boolean isCharFine(char c)
	{
		for (final var fineChar : FINE_CHARS)
		{
			if (fineChar == c)
			{
				return true;
			}
		}
		return false;
	}
	
	private void askQuestion(Player player, int id, String name)
	{
		final var message = new ServerMessage("CommunityBuffer.WANT_DELETE", player.getLang());
		message.add(name);
		player.sendConfirmDlg(new AskQuestionAnswerListener(player), 60000, message.toString());
		player.addQuickVar("schemeToDel", id);
	}
	
	public void deleteScheme(int eventParam1, Player player)
	{
		CharSchemesHolder.getInstance().deleteScheme(eventParam1);
		final var realId = eventParam1;
		for (final var scheme : player.getBuffSchemes())
		{
			if (scheme.getSchemeId() == realId)
			{
				player.getBuffSchemes().remove(scheme);
				break;
			}
		}
	}
	
	public boolean isValidSkill(Player player, int skillId, int level)
	{
		for (final var singleBuff : _allSingleBuffs)
		{
			if (singleBuff.getSkillId() == skillId)
			{
				if (player.hasPremiumBonus())
				{
					if (singleBuff.getPremiumLevel() == level || singleBuff.getLevel() == level)
					{
						return true;
					}
				}
				
				if (singleBuff.getLevel() == level)
				{
					return true;
				}
			}
		}
		return false;
	}
	
	public boolean isValidTypeSkill(Player player, int skillId, int level, String type)
	{
		final var buffList = _typeBuffs.get(type);
		if (buffList == null)
		{
			return false;
		}
		
		for (final var singleBuff : buffList)
		{
			if (singleBuff.getSkillId() == skillId)
			{
				if (player.hasPremiumBonus())
				{
					return singleBuff.getPremiumLevel() == level || singleBuff.getLevel() == level;
				}
				return singleBuff.getLevel() == level;
			}
		}
		return false;
	}
	
	public boolean isItemRemoveSkill(Player player, int skillId, int level)
	{
		for (final var singleBuff : _allSingleBuffs)
		{
			if (singleBuff.getSkillId() == skillId && singleBuff.getLevel() == level && singleBuff.isBuffForItems() && singleBuff.getRequestItems() != null)
			{
				return true;
			}
		}
		return false;
	}
	
	public boolean isItemRemoveTypeSkill(Player player, int skillId, String type)
	{
		final var buffList = _typeBuffs.get(type);
		if (buffList == null)
		{
			return false;
		}
		
		for (final var singleBuff : buffList)
		{
			if (singleBuff.getSkillId() == skillId && singleBuff.isBuffForItems() && singleBuff.getRequestItems() != null)
			{
				return true;
			}
		}
		return false;
	}
	
	public boolean checkItemsForSkill(Player player, int skillId, int level, boolean printMsg)
	{
		for (final var singleBuff : _allSingleBuffs)
		{
			if (singleBuff.getSkillId() == skillId && singleBuff.getLevel() == level && singleBuff.getRequestItems() != null)
			{
				if (singleBuff.needAllItems())
				{
					for (final int it[] : singleBuff.getRequestItems())
					{
						if ((it == null) || (it.length != 2))
						{
							continue;
						}
						
						if (player.getInventory().getItemByItemId(it[0]) == null || player.getInventory().getItemByItemId(it[0]).getCount() < it[1])
						{
							if (printMsg)
							{
								final var message = new ServerMessage("CommunityBuffer.NECESSARY_ITEMS", player.getLang());
								message.add(it[1]);
								message.add(Util.getItemName(player, it[0]));
								player.sendPacket(new CreatureSay(player.getObjectId(), Say2.CRITICAL_ANNOUNCE, ServerStorage.getInstance().getString(player.getLang(), "CommunityBuffer.ERROR"), message.toString()));
							}
							return false;
						}
					}
					
					if (singleBuff.isRemoveItems())
					{
						for (final int it[] : singleBuff.getRequestItems())
						{
							if ((it == null) || (it.length != 2))
							{
								continue;
							}
							player.destroyItemByItemId("RequestItemsBuff", it[0], it[1], player, true);
						}
					}
					return true;
				}
				else
				{
					var foundItem = false;
					final var needRemove = singleBuff.isRemoveItems();
					for (final int it[] : singleBuff.getRequestItems())
					{
						if ((it == null) || (it.length != 2))
						{
							continue;
						}
						
						if (player.getInventory().getItemByItemId(it[0]) != null && player.getInventory().getItemByItemId(it[0]).getCount() >= it[1])
						{
							foundItem = true;
							if (needRemove)
							{
								player.destroyItemByItemId("RequestItemsBuff", it[0], it[1], player, true);
							}
							break;
						}
					}
					
					if (!foundItem)
					{
						if (printMsg)
						{
							player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
						}
						return false;
					}
					return true;
				}
			}
		}
		return false;
	}
	
	public boolean checkItemsForTypeSkill(Player player, int skillId, boolean printMsg, String type)
	{
		final var buffList = _typeBuffs.get(type);
		if (buffList == null)
		{
			return false;
		}
		
		for (final var singleBuff : buffList)
		{
			if (singleBuff.getSkillId() == skillId && singleBuff.getRequestItems() != null)
			{
				if (singleBuff.needAllItems())
				{
					for (final int it[] : singleBuff.getRequestItems())
					{
						if ((it == null) || (it.length != 2))
						{
							continue;
						}
						
						if (player.getInventory().getItemByItemId(it[0]) == null || player.getInventory().getItemByItemId(it[0]).getCount() < it[1])
						{
							if (printMsg)
							{
								final var message = new ServerMessage("CommunityBuffer.NECESSARY_ITEMS", player.getLang());
								message.add(it[1]);
								message.add(Util.getItemName(player, it[0]));
								player.sendPacket(new CreatureSay(player.getObjectId(), Say2.CRITICAL_ANNOUNCE, ServerStorage.getInstance().getString(player.getLang(), "CommunityBuffer.ERROR"), message.toString()));
							}
							return false;
						}
					}
					
					if (singleBuff.isRemoveItems())
					{
						for (final int it[] : singleBuff.getRequestItems())
						{
							if ((it == null) || (it.length != 2))
							{
								continue;
							}
							player.destroyItemByItemId("RequestItemsBuff", it[0], it[1], player, true);
						}
					}
					return true;
				}
				else
				{
					var foundItem = false;
					final var needRemove = singleBuff.isRemoveItems();
					for (final int it[] : singleBuff.getRequestItems())
					{
						if ((it == null) || (it.length != 2))
						{
							continue;
						}
						
						if (player.getInventory().getItemByItemId(it[0]) != null && player.getInventory().getItemByItemId(it[0]).getCount() >= it[1])
						{
							foundItem = true;
							if (needRemove)
							{
								player.destroyItemByItemId("RequestItemsBuff", it[0], it[1], player, true);
							}
							break;
						}
					}
					
					if (!foundItem)
					{
						if (printMsg)
						{
							player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
						}
						return false;
					}
					return true;
				}
			}
		}
		return false;
	}
	
	private int getPremiumLvl(int skillId, int level)
	{
		for (final var singleBuff : _allSingleBuffs)
		{
			if (singleBuff.getSkillId() == skillId && singleBuff.getLevel() == level)
			{
				return singleBuff.getPremiumLevel();
			}
		}
		return 1;
	}
	
	public int getBuffTime(Player player, int skillId, int level)
	{
		for (final var singleBuff : _allSingleBuffs)
		{
			if (singleBuff.getSkillId() == skillId && singleBuff.getLevel() == level)
			{
				return player.hasPremiumBonus() ? singleBuff.getPremiumBuffTime() : singleBuff.getBuffTime();
			}
		}
		return 0;
	}
	
	public int getBuffTime(Player player, int skillId, String type)
	{
		final var buffList = _typeBuffs.get(type);
		if (buffList == null)
		{
			return 0;
		}
		
		for (final var singleBuff : buffList)
		{
			if (singleBuff.getSkillId() == skillId)
			{
				return player.hasPremiumBonus() ? singleBuff.getPremiumBuffTime() : singleBuff.getBuffTime();
			}
		}
		return 0;
	}
	
	private void addAllBuffsToScheme(Player player, int schemeId)
	{
		for (final var ef : player.getAllEffects())
		{
			if (ef != null)
			{
				final var skillId = ef.getSkill().getId();
				if (!haveSkillInScheme(player, schemeId, skillId))
				{
					final var level = ef.getSkill().getLevel();
					final var premiumLvl = getPremiumLvl(skillId, level);
					final var isDanceSlot = isDanceSlotBuff(player, skillId);
					if (isValidSkill(player, skillId, level))
					{
						CharSchemesHolder.getInstance().addBuff(String.valueOf(schemeId), String.valueOf(skillId), String.valueOf(level), String.valueOf(premiumLvl), isDanceSlot);
						player.getBuffSchemeById(schemeId).getBuffs().add(new SchemeBuff(skillId, level, premiumLvl, isDanceSlot));
					}
				}
			}
		}
	}
	
	private static boolean haveSkillInScheme(Player player, int schemeId, int skillId)
	{
		for (final var buff : player.getBuffSchemeById(schemeId).getBuffs())
		{
			if (buff != null && buff.getSkillId() == skillId)
			{
				return true;
			}
		}
		return false;
	}
	
	public Map<Integer, List<Integer>> getBuffClasses()
	{
		return _classes;
	}

	public int getGroupId(String presetName)
	{
		switch (presetName)
		{
			case "fighter" : return 1;
			case "mage" : return 2;
			case "support" : return 3;
			case "tank" : return 4;
			case "dagger" : return 5;
			case "archer" : return 6;
			default : return -1;
		}
	}
	
	public ArrayList<SingleBuff> getSetBuffs(int groupId)
	{
		return _buffs.get(groupId);
	}

	public RequestItems getRequestTemplate(int groupId)
	{
		return _setPrices.get(groupId);
	}

	
	private int[][] parseItemsList(String line)
	{
		if (line == null || line.isEmpty())
		{
			return null;
		}
		
		final var propertySplit = line.split(";");
		if (propertySplit.length == 0)
		{
			return null;
		}
		
		var i = 0;
		String[] valueSplit;
		final var result = new int[propertySplit.length][];
		for (final var value : propertySplit)
		{
			valueSplit = value.split(",");
			if (valueSplit.length != 2)
			{
				_log.warn(StringUtil.concat("CommunityBuffer: parseItemsList invalid entry -> \"", valueSplit[0], "\", should be itemId,itemNumber"));
				return null;
			}
			
			result[i] = new int[2];
			try
			{
				result[i][0] = Integer.parseInt(valueSplit[0]);
			}
			catch (final NumberFormatException e)
			{
				_log.warn(StringUtil.concat("CommunityBuffer: parseItemsList invalid itemId -> \"", valueSplit[0], "\""));
				return null;
			}
			try
			{
				result[i][1] = Integer.parseInt(valueSplit[1]);
			}
			catch (final NumberFormatException e)
			{
				_log.warn(StringUtil.concat("CommunityBuffer: parseItemsList invalid item number -> \"", valueSplit[1], "\""));
				return null;
			}
			i++;
		}
		return result;
	}

	@Override
	public void onWriteCommand(String command, String s, String s1, String s2, String s3, String s4, Player Player)
	{
	}
	
	public static CommunityBuffer getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final CommunityBuffer _instance = new CommunityBuffer();
	}
}