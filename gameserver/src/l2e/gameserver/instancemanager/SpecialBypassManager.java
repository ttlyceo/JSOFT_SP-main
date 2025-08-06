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
package l2e.gameserver.instancemanager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.napile.primitive.maps.IntObjectMap;
import org.napile.primitive.maps.impl.HashIntObjectMap;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import l2e.commons.log.LoggerObject;
import l2e.commons.util.Rnd;
import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.instancemanager.SpecialBypassManager.BypassTemplate.BypassQuestType;
import l2e.gameserver.model.CommandChannel;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.Party;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.Summon;
import l2e.gameserver.model.actor.instance.GrandBossInstance;
import l2e.gameserver.model.actor.templates.reflection.ReflectionItemTemplate;
import l2e.gameserver.model.actor.templates.reflection.ReflectionTemplate.ReflectionRemoveType;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.strings.server.ServerMessage;
import l2e.gameserver.model.strings.server.ServerStorage;
import l2e.gameserver.model.zone.type.BossZone;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.ExPCCafePointInfo;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;
import l2e.gameserver.network.serverpackets.SystemMessage;
import l2e.gameserver.network.serverpackets.updatetype.UserInfoType;

/**
 * Created by LordWinter
 */
public final class SpecialBypassManager extends LoggerObject
{
	private final IntObjectMap<BypassTemplate> _npcTemplates = new HashIntObjectMap<>();
	
	protected SpecialBypassManager()
	{
		_npcTemplates.clear();
		load();
		if (_npcTemplates.size() > 0)
		{
			info("Loaded " + _npcTemplates.size() + " special bypasses.");
			new SpecialBypass(-1, "SpecialBypass", "teleports");
		}
	}

	public void load()
	{
		try
		{
			final File file = new File(Config.DATAPACK_ROOT + "/data/stats/services/speacialBypasses.xml");
			final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);
			final Document doc1 = factory.newDocumentBuilder().parse(file);
			
			for (Node n = doc1.getFirstChild(); n != null; n = n.getNextSibling())
			{
				if ("list".equalsIgnoreCase(n.getNodeName()))
				{
					for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
					{
						if ("npc".equalsIgnoreCase(d.getNodeName()))
						{
							NamedNodeMap ref = d.getAttributes();

							String requiredQuest = null;
							boolean isForPremium = false;
							int hwidsLimit = 0, minRebirth = 0;
							int minLevel = 0, maxLevel = 0, minParty = 1, maxParty = 9;
							List<Location> teleportLocs = Collections.emptyList();
							
							BypassQuestType questType = null;
							
							final List<ReflectionItemTemplate> requestItems = new ArrayList<>();
							final List<ReflectionItemTemplate> rewardItems = new ArrayList<>();
							
							final int id = Integer.parseInt(ref.getNamedItem("id").getNodeValue());
							final boolean dispelBuffs = ref.getNamedItem("dispelBuffs") != null ? Boolean.parseBoolean(ref.getNamedItem("dispelBuffs").getNodeValue()) : false;
							boolean needAllItems = true;
							for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling())
							{
								ref = cd.getAttributes();
								
								if ("level".equalsIgnoreCase(cd.getNodeName()))
								{
									minLevel = ref.getNamedItem("min") == null ? 1 : Integer.parseInt(ref.getNamedItem("min").getNodeValue());
									maxLevel = ref.getNamedItem("max") == null ? Integer.MAX_VALUE : Integer.parseInt(ref.getNamedItem("max").getNodeValue());
								}
								else if ("party".equalsIgnoreCase(cd.getNodeName()))
								{
									minParty = Integer.parseInt(ref.getNamedItem("min").getNodeValue());
									maxParty = Integer.parseInt(ref.getNamedItem("max").getNodeValue());
								}
								
								else if ("rebirth".equalsIgnoreCase(cd.getNodeName()))
								{
									minRebirth = Integer.parseInt(ref.getNamedItem("val").getNodeValue());
								}
								else if ("hwidsLimit".equalsIgnoreCase(cd.getNodeName()))
								{
									hwidsLimit = Integer.parseInt(ref.getNamedItem("val").getNodeValue());
								}
								else if ("teleport".equalsIgnoreCase(cd.getNodeName()))
								{
									if (teleportLocs.isEmpty())
									{
										teleportLocs = new ArrayList<>(1);
									}
									final int x = Integer.parseInt(ref.getNamedItem("x").getNodeValue());
									final int y = Integer.parseInt(ref.getNamedItem("y").getNodeValue());
									final int z = Integer.parseInt(ref.getNamedItem("z").getNodeValue());
									teleportLocs.add(new Location(x, y, z));
								}
								else if ("remove".equalsIgnoreCase(cd.getNodeName()))
								{
									needAllItems = ref.getNamedItem("needAllItems") != null ? Boolean.parseBoolean(ref.getNamedItem("needAllItems").getNodeValue()) : true;
									for (Node item = cd.getFirstChild(); item != null; item = item.getNextSibling())
									{
										if ("item".equalsIgnoreCase(item.getNodeName()))
										{
											final int itemId = Integer.parseInt(item.getAttributes().getNamedItem("id").getNodeValue());
											final long amount = Long.parseLong(item.getAttributes().getNamedItem("count").getNodeValue());
											final boolean necessary = Boolean.parseBoolean(item.getAttributes().getNamedItem("necessary").getNodeValue());
											final ReflectionRemoveType type = item.getAttributes().getNamedItem("type") != null ? ReflectionRemoveType.valueOf(item.getAttributes().getNamedItem("type").getNodeValue()) : ReflectionRemoveType.NONE;
											requestItems.add(new ReflectionItemTemplate(itemId, amount, necessary, type));
										}
									}
								}
								else if ("give".equalsIgnoreCase(cd.getNodeName()))
								{
									for (Node item = cd.getFirstChild(); item != null; item = item.getNextSibling())
									{
										if ("item".equalsIgnoreCase(item.getNodeName()))
										{
											final int itemId = Integer.parseInt(item.getAttributes().getNamedItem("id").getNodeValue());
											final long amount = Long.parseLong(item.getAttributes().getNamedItem("count").getNodeValue());
											rewardItems.add(new ReflectionItemTemplate(itemId, amount, false, ReflectionRemoveType.NONE));
										}
									}
								}
								else if ("quest".equalsIgnoreCase(cd.getNodeName()))
								{
									requiredQuest = ref.getNamedItem("name") != null ? ref.getNamedItem("name").getNodeValue() : null;
									questType = ref.getNamedItem("type") != null ? BypassQuestType.valueOf(ref.getNamedItem("type").getNodeValue()) : BypassQuestType.STARTED;
								}
								else if ("isForPremium".equalsIgnoreCase(cd.getNodeName()))
								{
									isForPremium = ref.getNamedItem("val") != null ? Boolean.parseBoolean(ref.getNamedItem("val").getNodeValue()) : false;
								}
							}
							addNpcBypass(new BypassTemplate(id, dispelBuffs, minLevel, maxLevel, minParty, maxParty, minRebirth, hwidsLimit, teleportLocs, requestItems, needAllItems, rewardItems, requiredQuest, questType, isForPremium));
						}
					}
				}
			}
		}
		catch (
		    NumberFormatException | DOMException | ParserConfigurationException | SAXException e)
		{
			warn("speacialBypasses.xml could not be initialized.", e);
		}
		catch (
		    IOException | IllegalArgumentException e)
		{
			warn("IOException or IllegalArgumentException.", e);
		}
	}
	
	public class SpecialBypass extends Quest
	{
		public SpecialBypass(int id, String name, String descr)
		{
			super(id, name, descr);

			for (final int npcId : _npcTemplates.keys())
			{
				addStartNpc(npcId);
				addTalkId(npcId);
			}
		}
		
		@Override
		public String onAdvEvent(String event, Npc npc, Player player)
		{
			if (event.equalsIgnoreCase("teleport"))
			{
				final BypassTemplate template = getNpcBypass(npc.getId());
				if (template != null)
				{
					final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
					switch (npc.getId())
					{
						case 31862 :
							if (EpicBossManager.getInstance().getBossStatus(29020) == 3)
							{
								return "<html><body>" + ServerStorage.getInstance().getString(player.getLang(), "Baium.DEAD") + "</body></html>";
							}
							
							if (EpicBossManager.getInstance().getBossStatus(29020) == 2)
							{
								return "<html><body>" + ServerStorage.getInstance().getString(player.getLang(), "Baium.IN_FIGHT") + "</body></html>";
							}
							break;
						case 13001 :
							if (EpicBossManager.getInstance().getBossStatus(29068) == 3)
							{
								html.setFile(player, player.getLang(), "data/html/scripts/teleports/GrandBossTeleporters/13001-01.htm");
								player.sendPacket(html);
								return null;
							}
							
							if (EpicBossManager.getInstance().getBossStatus(29068) == 2)
							{
								html.setFile(player, player.getLang(), "data/html/scripts/teleports/GrandBossTeleporters/13001-02.htm");
								player.sendPacket(html);
								return null;
							}
							break;
						case 31385 :
							if (EpicBossManager.getInstance().getBossStatus(29028) == 2 || EpicBossManager.getInstance().getBossStatus(29028) == 3)
							{
								html.setFile(player, player.getLang(), "data/html/scripts/teleports/GrandBossTeleporters/31385-04.htm");
								player.sendPacket(html);
								return null;
							}
							break;
						case 32376 :
							if (EpicBossManager.getInstance().getBossStatus(29118) == 3)
							{
								html.setFile(player, player.getLang(), "data/html/scripts/teleports/SteelCitadelTeleport/32376-02.htm");
								player.sendPacket(html);
								return null;
							}
							break;
					}
					
					if (checkConditions(player, template))
					{
						onTeleport(player, template);
						switch (npc.getId())
						{
							case 13001 :
								if (EpicBossManager.getInstance().getBossStatus(29068) == 0)
								{
									final var antharas = EpicBossManager.getInstance().getBoss(29068);
									final var antharasAi = QuestManager.getInstance().getQuest("AntharasManager");
									if (antharasAi != null)
									{
										antharasAi.notifyEvent("waiting", antharas, player);
									}
								}
								break;
							case 31385 :
								if (EpicBossManager.getInstance().getBossStatus(29028) == 0)
								{
									final var valakas = EpicBossManager.getInstance().getBoss(29028);
									final var valakasAi = QuestManager.getInstance().getQuest("ValakasManager");
									if (valakasAi != null)
									{
										valakasAi.notifyEvent("waiting", valakas, player);
									}
								}
								break;
						}
					}
				}
			}
			return null;
		}
	}
	
	private boolean checkConditions(Player player, BypassTemplate template)
	{
		if (player.isCursedWeaponEquipped() || player.isFlying())
		{
			player.sendPacket(SystemMessageId.CANNOT_ENTER_CAUSE_DONT_MATCH_REQUIREMENTS);
			return false;
		}
		
		switch (template.getEntryType())
		{
			case SOLO :
				return checkSoloType(player, template);
			case SOLO_PARTY :
				if (player.getParty() == null)
				{
					return checkSoloType(player, template);
				}
				return checkPartyType(player, template);
			case PARTY :
				return checkPartyType(player, template);
			case PARTY_COMMAND_CHANNEL :
				if (player.getParty() != null && player.getParty().getCommandChannel() != null)
				{
					return checkCommandChannelType(player, template);
				}
				return checkPartyType(player, template);
			case COMMAND_CHANNEL :
				return checkCommandChannelType(player, template);
		}
		return true;
	}
	
	private void onTeleport(Player player, BypassTemplate template)
	{
		switch (template.getEntryType())
		{
			case SOLO :
				doSoloEnter(player, template);
				break;
			case SOLO_PARTY :
				if (player.getParty() == null)
				{
					doSoloEnter(player, template);
				}
				else
				{
					doPartyEnter(player, template);
				}
				break;
			case PARTY :
				doPartyEnter(player, template);
				break;
			case PARTY_COMMAND_CHANNEL :
				if (player.getParty() != null && player.getParty().getCommandChannel() != null)
				{
					doCommandChannelEnter(player, template);
				}
				else
				{
					doPartyEnter(player, template);
				}
				break;
			case COMMAND_CHANNEL :
				doCommandChannelEnter(player, template);
				break;
		}
	}
	
	protected boolean checkSoloType(Player player, BypassTemplate template)
	{
		if (player.getLevel() < template.getMinLevel() || player.getLevel() > template.getMaxLevel())
		{
			final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_LEVEL_REQUIREMENT_NOT_SUFFICIENT);
			sm.addPcName(player);
			player.sendPacket(sm);
			return false;
		}
		
		if (player.isInParty())
		{
			player.sendPacket(SystemMessageId.PARTY_EXCEEDED_THE_LIMIT_CANT_ENTER);
			return false;
		}
		
		if (template.getRebirth() > 0 && (player.getVarInt("rebirth", 0) < template.getRebirth()))
		{
			final var msg = new ServerMessage("Reflection.LOW_REBIRTH", player.getLang());
			msg.add(template.getRebirth());
			player.sendMessage(msg.toString());
			return false;
		}
		
		if (template.isForPremium() && !player.hasPremiumBonus())
		{
			player.sendMessage(new ServerMessage("Reflection.ONLY_FOR_PREMIUM", player.getLang()).toString());
			return false;
		}
		
		if (!template.getRequestItems().isEmpty())
		{
			final var needAllItems = template.isNeedAllItems();
			var notFound = 0;
			for (final var tpl : template.getRequestItems())
			{
				if (tpl.getId() == -100 && player.getPcBangPoints() < tpl.getCount())
				{
					notFound++;
					if (needAllItems)
					{
						final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_ITEM_REQUIREMENT_NOT_SUFFICIENT);
						sm.addPcName(player);
						player.sendPacket(sm);
						return false;
					}
				}
				else if (tpl.getId() == -200 && (player.getClan() == null || player.getClan().getReputationScore() < tpl.getCount()))
				{
					notFound++;
					if (needAllItems)
					{
						final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_ITEM_REQUIREMENT_NOT_SUFFICIENT);
						sm.addPcName(player);
						player.sendPacket(sm);
						return false;
					}
				}
				else if (tpl.getId() == -300 && player.getFame() < tpl.getCount())
				{
					notFound++;
					if (needAllItems)
					{
						final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_ITEM_REQUIREMENT_NOT_SUFFICIENT);
						sm.addPcName(player);
						player.sendPacket(sm);
						return false;
					}
				}
				else if (tpl.getId() > 0 && (player.getInventory().getItemByItemId(tpl.getId()) == null || player.getInventory().getItemByItemId(tpl.getId()).getCount() < tpl.getCount()))
				{
					notFound++;
					if (needAllItems)
					{
						final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_ITEM_REQUIREMENT_NOT_SUFFICIENT);
						sm.addPcName(player);
						player.sendPacket(sm);
						return false;
					}
				}
			}
			
			if (!needAllItems && notFound == template.getRequestItems().size())
			{
				final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_ITEM_REQUIREMENT_NOT_SUFFICIENT);
				sm.addPcName(player);
				player.sendPacket(sm);
				return false;
			}
		}
		
		if (template.getRequiredQuest() != null)
		{
			final QuestState qs = player.getQuestState(template.getRequiredQuest());
			boolean cannot = false;
			if (template.getQuestType() == BypassQuestType.STARTED)
			{
				if (qs == null || !qs.isStarted())
				{
					cannot = true;
				}
			}
			else
			{
				if (qs == null || !qs.isCompleted())
				{
					cannot = true;
				}
			}
			
			if (cannot)
			{
				final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_QUEST_REQUIREMENT_NOT_SUFFICIENT);
				sm.addPcName(player);
				player.sendPacket(sm);
				return false;
			}
		}
		return true;
	}
	
	protected boolean checkPartyType(Player player, BypassTemplate template)
	{
		final Party party = player.getParty();
		if (party == null || party.getMemberCount() < 2)
		{
			player.sendPacket(SystemMessageId.NOT_IN_PARTY_CANT_ENTER);
			return false;
		}
		if (party.getLeader() != player)
		{
			player.sendPacket(SystemMessageId.ONLY_PARTY_LEADER_CAN_ENTER);
			return false;
		}
		
		if (party.getMemberCount() < template.getMinParty())
		{
			final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.YOU_MUST_HAVE_MINIMUM_OF_S1_PEOPLE_TO_ENTER);
			sm.addNumber(template.getMinParty());
			player.sendPacket(sm);
			return false;
		}
		
		if (party.getMemberCount() > template.getMaxParty())
		{
			player.sendPacket(SystemMessageId.PARTY_EXCEEDED_THE_LIMIT_CANT_ENTER);
			return false;
		}
		
		if (!template.getRequestItems().isEmpty())
		{
			final var needAllItems = template.isNeedAllItems();
			var notFound = 0;
			for (final var tpl : template.getRequestItems())
			{
				if (tpl.getType() == ReflectionRemoveType.LEADER)
				{
					if (tpl.getId() == -100 && player.getPcBangPoints() < tpl.getCount())
					{
						notFound++;
						if (needAllItems)
						{
							final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_ITEM_REQUIREMENT_NOT_SUFFICIENT);
							sm.addPcName(player);
							player.sendPacket(sm);
							return false;
						}
					}
					else if (tpl.getId() == -200 && (player.getClan() == null || player.getClan().getReputationScore() < tpl.getCount()))
					{
						notFound++;
						if (needAllItems)
						{
							final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_ITEM_REQUIREMENT_NOT_SUFFICIENT);
							sm.addPcName(player);
							player.sendPacket(sm);
							return false;
						}
					}
					else if (tpl.getId() == -300 && player.getFame() < tpl.getCount())
					{
						notFound++;
						if (needAllItems)
						{
							final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_ITEM_REQUIREMENT_NOT_SUFFICIENT);
							sm.addPcName(player);
							player.sendPacket(sm);
							return false;
						}
					}
					else if (tpl.getId() > 0 && (player.getInventory().getItemByItemId(tpl.getId()) == null || player.getInventory().getItemByItemId(tpl.getId()).getCount() < tpl.getCount()))
					{
						notFound++;
						if (needAllItems)
						{
							final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_ITEM_REQUIREMENT_NOT_SUFFICIENT);
							sm.addPcName(player);
							player.sendPacket(sm);
							return false;
						}
					}
				}
			}
			
			if (!needAllItems && notFound == template.getRequestItems().size())
			{
				final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_ITEM_REQUIREMENT_NOT_SUFFICIENT);
				sm.addPcName(player);
				player.sendPacket(sm);
				return false;
			}
		}
		
		if (template.getHwidsLimit() > 0)
		{
			if (!isAvalibleHwids(party.getMembers(), template.getHwidsLimit()))
			{
				final var msg = new ServerMessage("Reflection.HWIDS_LIMIT", true);
				msg.add(template.getHwidsLimit());
				party.broadCastMessage(msg);
				return false;
			}
		}
		
		for (final Player partyMember : party.getMembers())
		{
			if (partyMember.getLevel() < template.getMinLevel() || partyMember.getLevel() > template.getMaxLevel())
			{
				final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_LEVEL_REQUIREMENT_NOT_SUFFICIENT);
				sm.addPcName(partyMember);
				party.broadCast(sm);
				return false;
			}
			
			if (!partyMember.isInsideRadius(player, 500, true, true))
			{
				final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_IS_IN_LOCATION_THAT_CANNOT_BE_ENTERED);
				sm.addPcName(partyMember);
				party.broadCast(sm);
				return false;
			}
			
			if (template.getRebirth() > 0 && (partyMember.getVarInt("rebirth", 0) < template.getRebirth()))
			{
				final var msg = new ServerMessage("Reflection.LOW_PLAYER_REBIRTH", true);
				msg.add(partyMember);
				msg.add(template.getRebirth());
				party.broadCastMessage(msg);
				return false;
			}
			
			if (template.isForPremium() && !partyMember.hasPremiumBonus())
			{
				final var msg = new ServerMessage("Reflection.PLAYER_WITHOUT_PREMIUM", true);
				msg.add(partyMember);
				party.broadCastMessage(msg);
				return false;
			}
			
			if (!template.getRequestItems().isEmpty())
			{
				final var needAllItems = template.isNeedAllItems();
				var notFound = 0;
				for (final var tpl : template.getRequestItems())
				{
					if (tpl.getType() == ReflectionRemoveType.ALL)
					{
						if (tpl.getId() == -100 && partyMember.getPcBangPoints() < tpl.getCount())
						{
							notFound++;
							if (needAllItems)
							{
								final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_ITEM_REQUIREMENT_NOT_SUFFICIENT);
								sm.addPcName(partyMember);
								party.broadCast(sm);
								return false;
							}
						}
						else if (tpl.getId() == -200 && (partyMember.getClan() == null || partyMember.getClan().getReputationScore() < tpl.getCount()))
						{
							notFound++;
							if (needAllItems)
							{
								final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_ITEM_REQUIREMENT_NOT_SUFFICIENT);
								sm.addPcName(partyMember);
								party.broadCast(sm);
								return false;
							}
						}
						else if (tpl.getId() == -300 && partyMember.getFame() < tpl.getCount())
						{
							notFound++;
							if (needAllItems)
							{
								final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_ITEM_REQUIREMENT_NOT_SUFFICIENT);
								sm.addPcName(partyMember);
								party.broadCast(sm);
								return false;
							}
						}
						else if (tpl.getId() > 0 && (partyMember.getInventory().getItemByItemId(tpl.getId()) == null || partyMember.getInventory().getItemByItemId(tpl.getId()).getCount() < tpl.getCount()))
						{
							notFound++;
							if (needAllItems)
							{
								final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_ITEM_REQUIREMENT_NOT_SUFFICIENT);
								sm.addPcName(partyMember);
								party.broadCast(sm);
								return false;
							}
						}
					}
				}
				
				if (!needAllItems && notFound == template.getRequestItems().size())
				{
					final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_ITEM_REQUIREMENT_NOT_SUFFICIENT);
					sm.addPcName(partyMember);
					party.broadCast(sm);
					return false;
				}
			}
			
			if (template.getRequiredQuest() != null)
			{
				final QuestState qs = partyMember.getQuestState(template.getRequiredQuest());
				boolean cannot = false;
				if (template.getQuestType() == BypassQuestType.STARTED)
				{
					if (qs == null || !qs.isStarted())
					{
						cannot = true;
					}
				}
				else
				{
					if (qs == null || !qs.isCompleted())
					{
						cannot = true;
					}
				}
				
				if (cannot)
				{
					final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_QUEST_REQUIREMENT_NOT_SUFFICIENT);
					sm.addPcName(partyMember);
					party.broadCast(sm);
					return false;
				}
			}
		}
		return true;
	}
	
	protected boolean checkCommandChannelType(Player player, BypassTemplate template)
	{
		final Party pt = player.getParty();
		if (pt == null || pt.getCommandChannel() == null)
		{
			player.sendPacket(SystemMessageId.NOT_IN_COMMAND_CHANNEL_CANT_ENTER);
			return false;
		}
		
		final CommandChannel cc = pt.getCommandChannel();
		if (cc.getMemberCount() < template.getMinParty())
		{
			final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.YOU_MUST_HAVE_MINIMUM_OF_S1_PEOPLE_TO_ENTER);
			sm.addNumber(template.getMinParty());
			player.sendPacket(sm);
			return false;
		}
		
		if (cc.getMemberCount() > template.getMaxParty())
		{
			player.sendPacket(SystemMessageId.PARTY_EXCEEDED_THE_LIMIT_CANT_ENTER);
			return false;
		}
		
		if (!template.getRequestItems().isEmpty())
		{
			final var needAllItems = template.isNeedAllItems();
			var notFound = 0;
			for (final var tpl : template.getRequestItems())
			{
				if (tpl.getType() == ReflectionRemoveType.LEADER)
				{
					if (tpl.getId() == -100 && player.getPcBangPoints() < tpl.getCount())
					{
						notFound++;
						if (needAllItems)
						{
							final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_ITEM_REQUIREMENT_NOT_SUFFICIENT);
							sm.addPcName(player);
							player.sendPacket(sm);
							return false;
						}
					}
					else if (tpl.getId() == -200 && (player.getClan() == null || player.getClan().getReputationScore() < tpl.getCount()))
					{
						notFound++;
						if (needAllItems)
						{
							final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_ITEM_REQUIREMENT_NOT_SUFFICIENT);
							sm.addPcName(player);
							player.sendPacket(sm);
							return false;
						}
					}
					else if (tpl.getId() == -300 && player.getFame() < tpl.getCount())
					{
						notFound++;
						if (needAllItems)
						{
							final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_ITEM_REQUIREMENT_NOT_SUFFICIENT);
							sm.addPcName(player);
							player.sendPacket(sm);
							return false;
						}
					}
					else if (tpl.getId() > 0 && (player.getInventory().getItemByItemId(tpl.getId()) == null || player.getInventory().getItemByItemId(tpl.getId()).getCount() < tpl.getCount()))
					{
						notFound++;
						if (needAllItems)
						{
							final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_ITEM_REQUIREMENT_NOT_SUFFICIENT);
							sm.addPcName(player);
							player.sendPacket(sm);
							return false;
						}
					}
				}
			}
			
			if (!needAllItems && notFound == template.getRequestItems().size())
			{
				final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_ITEM_REQUIREMENT_NOT_SUFFICIENT);
				sm.addPcName(player);
				player.sendPacket(sm);
				return false;
			}
		}
		
		if (template.getHwidsLimit() > 0)
		{
			if (!isAvalibleHwids(cc.getMembers(), template.getHwidsLimit()))
			{
				final var msg = new ServerMessage("Reflection.HWIDS_LIMIT", true);
				msg.add(template.getHwidsLimit());
				cc.broadCastMessage(msg);
				return false;
			}
		}
		
		for (final Player channelMember : cc.getMembers())
		{
			if (channelMember.getLevel() < template.getMinLevel() || channelMember.getLevel() > template.getMaxLevel())
			{
				final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_LEVEL_REQUIREMENT_NOT_SUFFICIENT);
				sm.addPcName(channelMember);
				cc.broadCast(sm);
				return false;
			}
			
			if (!Util.checkIfInRange(1000, player, channelMember, true))
			{
				final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_IS_IN_LOCATION_THAT_CANNOT_BE_ENTERED);
				sm.addPcName(channelMember);
				cc.broadCast(sm);
				return false;
			}
			
			if (template.getRebirth() > 0 && (channelMember.getVarInt("rebirth", 0) < template.getRebirth()))
			{
				final var msg = new ServerMessage("Reflection.LOW_PLAYER_REBIRTH", true);
				msg.add(channelMember);
				msg.add(template.getRebirth());
				cc.broadCastMessage(msg);
				return false;
			}
			
			if (template.isForPremium() && !channelMember.hasPremiumBonus())
			{
				final var msg = new ServerMessage("Reflection.PLAYER_WITHOUT_PREMIUM", true);
				msg.add(channelMember);
				cc.broadCastMessage(msg);
				return false;
			}
			
			if (!template.getRequestItems().isEmpty())
			{
				final var needAllItems = template.isNeedAllItems();
				var notFound = 0;
				for (final var tpl : template.getRequestItems())
				{
					if (tpl.getType() == ReflectionRemoveType.ALL)
					{
						if (tpl.getId() == -100 && channelMember.getPcBangPoints() < tpl.getCount())
						{
							notFound++;
							if (needAllItems)
							{
								final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_ITEM_REQUIREMENT_NOT_SUFFICIENT);
								sm.addPcName(channelMember);
								cc.broadCast(sm);
								return false;
							}
						}
						else if (tpl.getId() == -200 && (channelMember.getClan() == null || channelMember.getClan().getReputationScore() < tpl.getCount()))
						{
							notFound++;
							if (needAllItems)
							{
								final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_ITEM_REQUIREMENT_NOT_SUFFICIENT);
								sm.addPcName(channelMember);
								cc.broadCast(sm);
								return false;
							}
						}
						else if (tpl.getId() == -300 && channelMember.getFame() < tpl.getCount())
						{
							notFound++;
							if (needAllItems)
							{
								final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_ITEM_REQUIREMENT_NOT_SUFFICIENT);
								sm.addPcName(channelMember);
								cc.broadCast(sm);
								return false;
							}
						}
						else if (tpl.getId() > 0 && (channelMember.getInventory().getItemByItemId(tpl.getId()) == null || channelMember.getInventory().getItemByItemId(tpl.getId()).getCount() < tpl.getCount()))
						{
							notFound++;
							if (needAllItems)
							{
								final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_ITEM_REQUIREMENT_NOT_SUFFICIENT);
								sm.addPcName(channelMember);
								cc.broadCast(sm);
								return false;
							}
						}
					}
				}
				
				if (!needAllItems && notFound == template.getRequestItems().size())
				{
					final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_ITEM_REQUIREMENT_NOT_SUFFICIENT);
					sm.addPcName(channelMember);
					cc.broadCast(sm);
					return false;
				}
			}
			
			if (template.getRequiredQuest() != null)
			{
				final QuestState qs = channelMember.getQuestState(template.getRequiredQuest());
				boolean cannot = false;
				if (template.getQuestType() == BypassQuestType.STARTED)
				{
					if (qs == null || !qs.isStarted())
					{
						cannot = true;
					}
				}
				else
				{
					if (qs == null || !qs.isCompleted())
					{
						cannot = true;
					}
				}
				
				if (cannot)
				{
					final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_QUEST_REQUIREMENT_NOT_SUFFICIENT);
					sm.addPcName(channelMember);
					cc.broadCast(sm);
					return false;
				}
			}
		}
		return true;
	}
	
	private void doSoloEnter(Player player, BypassTemplate template)
	{
		if (!template.getRequestItems().isEmpty())
		{
			for (final var tpl : template.getRequestItems())
			{
				if (tpl.isNecessary())
				{
					if (tpl.getId() == -100)
					{
						player.setPcBangPoints((int) (player.getPcBangPoints() - tpl.getCount()));
						final SystemMessage smsg = SystemMessage.getSystemMessage(SystemMessageId.USING_S1_PCPOINT);
						smsg.addNumber((int) tpl.getCount());
						player.sendPacket(smsg);
						player.sendPacket(new ExPCCafePointInfo(player.getPcBangPoints(), (int) tpl.getCount(), false, false, 1));
					}
					else if (tpl.getId() == -200)
					{
						player.getClan().takeReputationScore((int) tpl.getCount(), true);
						final SystemMessage smsg = SystemMessage.getSystemMessage(SystemMessageId.S1_DEDUCTED_FROM_CLAN_REP);
						smsg.addItemNumber(tpl.getCount());
						player.sendPacket(smsg);
					}
					else if (tpl.getId() == -300)
					{
						player.setFame((int) (player.getFame() - tpl.getCount()));
						player.sendUserInfo(UserInfoType.VITA_FAME);
					}
					else if (tpl.getId() > 0)
					{
						player.destroyItemByItemId("Instance Check", tpl.getId(), tpl.getCount(), player, true);
					}
				}
			}
		}
		
		if (!template.getRewardItems().isEmpty())
		{
			for (final var tpl : template.getRewardItems())
			{
				player.addItem("Instance reward", tpl.getId(), tpl.getCount(), null, true);
			}
		}
		onTeleportEnter(player, template);
	}
	
	private void doPartyEnter(Player player, BypassTemplate template)
	{
		if (!template.getRequestItems().isEmpty())
		{
			for (final var tpl : template.getRequestItems())
			{
				if (tpl.isNecessary() && tpl.getType() == ReflectionRemoveType.LEADER)
				{
					if (tpl.getId() == -100)
					{
						player.setPcBangPoints((int) (player.getPcBangPoints() - tpl.getCount()));
						final SystemMessage smsg = SystemMessage.getSystemMessage(SystemMessageId.USING_S1_PCPOINT);
						smsg.addNumber((int) tpl.getCount());
						player.sendPacket(smsg);
						player.sendPacket(new ExPCCafePointInfo(player.getPcBangPoints(), (int) tpl.getCount(), false, false, 1));
					}
					else if (tpl.getId() == -200)
					{
						player.getClan().takeReputationScore((int) tpl.getCount(), true);
						final SystemMessage smsg = SystemMessage.getSystemMessage(SystemMessageId.S1_DEDUCTED_FROM_CLAN_REP);
						smsg.addItemNumber(tpl.getCount());
						player.sendPacket(smsg);
					}
					else if (tpl.getId() == -300)
					{
						player.setFame((int) (player.getFame() - tpl.getCount()));
						player.sendUserInfo(UserInfoType.VITA_FAME);
					}
					else if (tpl.getId() > 0)
					{
						player.destroyItemByItemId("Instance Check", tpl.getId(), tpl.getCount(), player, true);
					}
				}
			}
		}
		
		for (final Player partyMember : player.getParty().getMembers())
		{
			if (partyMember != null)
			{
				if (!template.getRequestItems().isEmpty())
				{
					for (final var tpl : template.getRequestItems())
					{
						if (tpl.isNecessary() && tpl.getType() == ReflectionRemoveType.ALL)
						{
							if (tpl.getId() == -100)
							{
								partyMember.setPcBangPoints((int) (partyMember.getPcBangPoints() - tpl.getCount()));
								final SystemMessage smsg = SystemMessage.getSystemMessage(SystemMessageId.USING_S1_PCPOINT);
								smsg.addNumber((int) tpl.getCount());
								partyMember.sendPacket(smsg);
								partyMember.sendPacket(new ExPCCafePointInfo(partyMember.getPcBangPoints(), (int) tpl.getCount(), false, false, 1));
							}
							else if (tpl.getId() == -200)
							{
								partyMember.getClan().takeReputationScore((int) tpl.getCount(), true);
								final SystemMessage smsg = SystemMessage.getSystemMessage(SystemMessageId.S1_DEDUCTED_FROM_CLAN_REP);
								smsg.addItemNumber(tpl.getCount());
								partyMember.sendPacket(smsg);
							}
							else if (tpl.getId() == -300)
							{
								partyMember.setFame((int) (partyMember.getFame() - tpl.getCount()));
								partyMember.sendUserInfo(UserInfoType.VITA_FAME);
							}
							else if (tpl.getId() > 0)
							{
								partyMember.destroyItemByItemId("Instance Check", tpl.getId(), tpl.getCount(), partyMember, true);
							}
						}
					}
				}
				
				if (!template.getRewardItems().isEmpty())
				{
					for (final var tpl : template.getRewardItems())
					{
						partyMember.addItem("Instance reward", tpl.getId(), tpl.getCount(), null, true);
					}
				}
				onTeleportEnter(partyMember, template);
			}
		}
	}
	
	private void doCommandChannelEnter(Player player, BypassTemplate template)
	{
		if (!template.getRequestItems().isEmpty())
		{
			for (final var tpl : template.getRequestItems())
			{
				if (tpl.isNecessary() && tpl.getType() == ReflectionRemoveType.LEADER)
				{
					if (tpl.getId() == -100)
					{
						player.setPcBangPoints((int) (player.getPcBangPoints() - tpl.getCount()));
						final SystemMessage smsg = SystemMessage.getSystemMessage(SystemMessageId.USING_S1_PCPOINT);
						smsg.addNumber((int) tpl.getCount());
						player.sendPacket(smsg);
						player.sendPacket(new ExPCCafePointInfo(player.getPcBangPoints(), (int) tpl.getCount(), false, false, 1));
					}
					else if (tpl.getId() == -200)
					{
						player.getClan().takeReputationScore((int) tpl.getCount(), true);
						final SystemMessage smsg = SystemMessage.getSystemMessage(SystemMessageId.S1_DEDUCTED_FROM_CLAN_REP);
						smsg.addItemNumber(tpl.getCount());
						player.sendPacket(smsg);
					}
					else if (tpl.getId() == -300)
					{
						player.setFame((int) (player.getFame() - tpl.getCount()));
						player.sendUserInfo(UserInfoType.VITA_FAME);
					}
					else if (tpl.getId() > 0)
					{
						player.destroyItemByItemId("Instance Check", tpl.getId(), tpl.getCount(), player, true);
					}
				}
			}
		}
		
		for (final Player ccMember : player.getParty().getCommandChannel().getMembers())
		{
			if (ccMember != null)
			{
				if (!template.getRequestItems().isEmpty())
				{
					for (final var tpl : template.getRequestItems())
					{
						if (tpl.isNecessary() && tpl.getType() == ReflectionRemoveType.ALL)
						{
							if (tpl.getId() == -100)
							{
								ccMember.setPcBangPoints((int) (ccMember.getPcBangPoints() - tpl.getCount()));
								final SystemMessage smsg = SystemMessage.getSystemMessage(SystemMessageId.USING_S1_PCPOINT);
								smsg.addNumber((int) tpl.getCount());
								ccMember.sendPacket(smsg);
								ccMember.sendPacket(new ExPCCafePointInfo(ccMember.getPcBangPoints(), (int) tpl.getCount(), false, false, 1));
							}
							else if (tpl.getId() == -200)
							{
								ccMember.getClan().takeReputationScore((int) tpl.getCount(), true);
								final SystemMessage smsg = SystemMessage.getSystemMessage(SystemMessageId.S1_DEDUCTED_FROM_CLAN_REP);
								smsg.addItemNumber(tpl.getCount());
								ccMember.sendPacket(smsg);
							}
							else if (tpl.getId() == -300)
							{
								ccMember.setFame((int) (ccMember.getFame() - tpl.getCount()));
								ccMember.sendUserInfo(UserInfoType.VITA_FAME);
							}
							else if (tpl.getId() > 0)
							{
								ccMember.destroyItemByItemId("Instance Check", tpl.getId(), tpl.getCount(), ccMember, true);
							}
						}
					}
				}
				
				if (!template.getRewardItems().isEmpty())
				{
					for (final var tpl : template.getRewardItems())
					{
						ccMember.addItem("Instance reward", tpl.getId(), tpl.getCount(), null, true);
					}
				}
				onTeleportEnter(ccMember, template);
			}
		}
	}
	
	private void onTeleportEnter(Player player, BypassTemplate template)
	{
		switch (template.getId())
		{
			case 32376 :
				final BossZone zone = (BossZone) ZoneManager.getInstance().getZoneById(12018);
				final var belethAI = QuestManager.getInstance().getQuest("Beleth");
				if (belethAI != null && zone != null)
				{
					final int status = EpicBossManager.getInstance().getBossStatus(29118);
					if (status == 0)
					{
						EpicBossManager.getInstance().setBossStatus(29118, 1, true);
						final GrandBossInstance beleth = EpicBossManager.getInstance().getBoss(29118);
						belethAI.notifyEvent("waiting", beleth, player);
					}
					zone.allowPlayerEntry(player, 30);
				}
				break;
		}
		player.getAI().setIntention(CtrlIntention.IDLE);
		final Location teleLoc = template.getTeleportCoord();
		player.teleToLocation(teleLoc, true, ReflectionManager.DEFAULT);
		if (player.hasSummon())
		{
			player.getSummon().getAI().setIntention(CtrlIntention.IDLE);
			player.getSummon().teleToLocation(teleLoc, true, ReflectionManager.DEFAULT);
		}
		
		if (template.isDispelBuffs())
		{
			player.stopAllEffectsExceptThoseThatLastThroughDeath();
			final Summon summon = player.getSummon();
			if (summon != null)
			{
				summon.stopAllEffectsExceptThoseThatLastThroughDeath();
			}
		}
	}
	
	public void addNpcBypass(BypassTemplate tpl)
	{
		_npcTemplates.put(tpl.getId(), tpl);
	}
	
	public BypassTemplate getNpcBypass(int id)
	{
		return _npcTemplates.get(id);
	}
	
	public static class BypassTemplate
	{
		private final int _id;
		private final boolean _dispelBuffs;
		private final int _minLevel;
		private final int _maxLevel;
		private final int _minParty;
		private final int _maxParty;
		private final int _minRebirth;
		private final int _hwidsLimit;
		private final List<Location> _teleportCoords;
		private BypassEntryType _entryType = null;
		private final String _requiredQuest;
		private final BypassQuestType _questType;
		final List<ReflectionItemTemplate> _requestItems;
		private final boolean _needAllItems;
		final List<ReflectionItemTemplate> _rewardItems;
		private final boolean _isForPremium;
		
		public enum BypassQuestType
		{
			STARTED, COMPLETED;
		}
		
		public enum BypassEntryType
		{
			SOLO, SOLO_PARTY, PARTY, EVENT, PARTY_COMMAND_CHANNEL, COMMAND_CHANNEL;
		}
		
		public BypassTemplate(int id, boolean dispelBuffs, int minLevel, int maxLevel, int minParty, int maxParty, int minRebirth, int hwidsLimit, List<Location> tele, List<ReflectionItemTemplate> requestItems, boolean needAllItems, List<ReflectionItemTemplate> rewardItems, String requiredQuest, BypassQuestType questType, boolean isForPremium)
		{
			_id = id;
			_dispelBuffs = dispelBuffs;
			_minLevel = minLevel;
			_maxLevel = maxLevel;
			_teleportCoords = tele;
			_minParty = minParty;
			_maxParty = maxParty;
			_minRebirth = minRebirth;
			_hwidsLimit = hwidsLimit;
			_requestItems = requestItems;
			_rewardItems = rewardItems;
			_requiredQuest = requiredQuest;
			_questType = questType;
			_isForPremium = isForPremium;
			_needAllItems = needAllItems;
			if (getMinParty() == 1 && getMaxParty() == 1)
			{
				_entryType = BypassEntryType.SOLO;
			}
			else if (getMinParty() == 1 && getMaxParty() <= Config.PARTY_LIMIT)
			{
				_entryType = BypassEntryType.SOLO_PARTY;
			}
			else if (getMinParty() > 1 && getMaxParty() <= Config.PARTY_LIMIT)
			{
				_entryType = BypassEntryType.PARTY;
			}
			else if (getMinParty() < Config.PARTY_LIMIT && getMaxParty() > Config.PARTY_LIMIT)
			{
				_entryType = BypassEntryType.PARTY_COMMAND_CHANNEL;
			}
			else if (getMinParty() >= Config.PARTY_LIMIT && getMaxParty() > Config.PARTY_LIMIT)
			{
				_entryType = BypassEntryType.COMMAND_CHANNEL;
			}
			
			if (_entryType == null)
			{
				throw new IllegalArgumentException("Invalid type for special bypass: " + _id);
			}
		}
		
		public int getId()
		{
			return _id;
		}
		
		public boolean isDispelBuffs()
		{
			return _dispelBuffs;
		}
		
		public int getMinLevel()
		{
			return _minLevel;
		}
		
		public int getMaxLevel()
		{
			return _maxLevel;
		}
		
		public int getMinParty()
		{
			return _minParty;
		}
		
		public int getMaxParty()
		{
			return _maxParty;
		}
		
		public Location getTeleportCoord()
		{
			if (_teleportCoords == null || _teleportCoords.isEmpty())
			{
				return null;
			}
			if (_teleportCoords.size() == 1)
			{
				return _teleportCoords.get(0);
			}
			return _teleportCoords.get(Rnd.get(_teleportCoords.size()));
		}
		
		public void setNewTeleportCoords(Location loc)
		{
			_teleportCoords.clear();
			_teleportCoords.add(loc);
		}
		
		public List<ReflectionItemTemplate> getRequestItems()
		{
			return _requestItems;
		}
		
		public List<ReflectionItemTemplate> getRewardItems()
		{
			return _rewardItems;
		}
		
		public boolean isForPremium()
		{
			return _isForPremium;
		}
		
		public int getHwidsLimit()
		{
			return _hwidsLimit;
		}
		
		public int getRebirth()
		{
			return _minRebirth;
		}
		
		public BypassEntryType getEntryType()
		{
			return _entryType;
		}
		
		public List<Location> getTeleportCoords()
		{
			return _teleportCoords;
		}
		
		public String getRequiredQuest()
		{
			return _requiredQuest;
		}
		
		public BypassQuestType getQuestType()
		{
			return _questType;
		}
		
		public boolean isNeedAllItems()
		{
			return _needAllItems;
		}
	}
	
	private boolean isAvalibleHwids(List<Player> players, int hwidLimit)
	{
		final Map<String, Integer> hwids = new HashMap<>();
		final var isHwidCheck = !Config.PROTECTION.equalsIgnoreCase("NONE");
		for (final var player : players)
		{
			if (player != null && player.getClient() != null)
			{
				final String info = isHwidCheck ? player.getHWID() : player.getIPAddress();
				if (hwids.containsKey(info))
				{
					final int value = hwids.get(info) + 1;
					if (value > hwidLimit)
					{
						hwids.clear();
						return false;
					}
				}
				else
				{
					hwids.put(info, 1);
				}
			}
		}
		hwids.clear();
		return true;
	}
	
	public static SpecialBypassManager getInstance()
	{
		return SingletonHolder._instance;
	}

	private static class SingletonHolder
	{
		protected static final SpecialBypassManager _instance = new SpecialBypassManager();
	}
}