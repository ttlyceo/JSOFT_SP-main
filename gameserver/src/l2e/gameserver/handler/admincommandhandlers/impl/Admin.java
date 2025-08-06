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
package l2e.gameserver.handler.admincommandhandlers.impl;

import java.util.Objects;
import java.util.StringTokenizer;

import l2e.gameserver.Config;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.data.htm.HtmCache;
import l2e.gameserver.data.parser.*;
import l2e.gameserver.handler.admincommandhandlers.IAdminCommandHandler;
import l2e.gameserver.handler.communityhandlers.impl.CommunityBuffer;
import l2e.gameserver.instancemanager.NpcStatManager;
import l2e.gameserver.instancemanager.QuestManager;
import l2e.gameserver.instancemanager.RewardManager;
import l2e.gameserver.instancemanager.WalkingManager;
import l2e.gameserver.instancemanager.ZoneManager;
import l2e.gameserver.listener.ScriptListenerLoader;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.entity.events.AbstractFightEvent;
import l2e.gameserver.model.entity.events.AbstractWorldEvent;
import l2e.gameserver.model.entity.events.model.FightEventManager;
import l2e.gameserver.model.entity.events.model.FightEventNpcManager;
import l2e.gameserver.model.entity.events.tournaments.Tournament;
import l2e.gameserver.model.entity.events.tournaments.TournamentData;
import l2e.gameserver.model.entity.events.tournaments.data.parser.TournamentsParser;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;
import top.jsoft.phaseevent.PhaseEventConfig;

public class Admin implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
	        "admin_admin", "admin_admin1", "admin_admin2", "admin_admin3", "admin_admin4", "admin_admin5", "admin_admin6", "admin_admin7", "admin_html", "admin_gmliston", "admin_gmlistoff", "admin_silence", "admin_diet", "admin_tradeoff", "admin_reload", "admin_pool_info",
	};
	
	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		final NpcHtmlMessage adminhtm = new NpcHtmlMessage(5);
		
		if (command.startsWith("admin_admin"))
		{
			showMainPage(activeChar, command);
		}
		else if (command.startsWith("admin_pool_info"))
		{
			ThreadPoolManager.getInstance().getInfo();
		}
		else if (command.startsWith("admin_html"))
		{
			final StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken();
			if (!st.hasMoreTokens())
			{
				activeChar.sendMessage("Usage: //html path");
				return false;
			}
			
			final String path = st.nextToken();
			if (path != null)
			{
				adminhtm.setFile(activeChar, activeChar.getLang(), "data/html/admin/" + path);
				activeChar.sendPacket(adminhtm);
			}
		}
		else if (command.startsWith("admin_gmliston"))
		{
			AdminParser.getInstance().showGm(activeChar);
			activeChar.sendMessage("Registered into gm list");
			adminhtm.setFile(activeChar, activeChar.getLang(), "data/html/admin/gm_menu.htm");
			activeChar.sendPacket(adminhtm);
		}
		else if (command.startsWith("admin_gmlistoff"))
		{
			AdminParser.getInstance().hideGm(activeChar);
			activeChar.sendMessage("Removed from gm list");
			adminhtm.setFile(activeChar, activeChar.getLang(), "data/html/admin/gm_menu.htm");
			activeChar.sendPacket(adminhtm);
		}
		else if (command.startsWith("admin_silence"))
		{
			if (activeChar.isSilenceMode())
			{
				activeChar.setSilenceMode(false);
				activeChar.sendPacket(SystemMessageId.MESSAGE_ACCEPTANCE_MODE);
			}
			else
			{
				activeChar.setSilenceMode(true);
				activeChar.sendPacket(SystemMessageId.MESSAGE_REFUSAL_MODE);
			}
			adminhtm.setFile(activeChar, activeChar.getLang(), "data/html/admin/gm_menu.htm");
			activeChar.sendPacket(adminhtm);
		}
		else if (command.startsWith("admin_diet"))
		{
			try
			{
				final StringTokenizer st = new StringTokenizer(command);
				st.nextToken();
				if (st.nextToken().equalsIgnoreCase("on"))
				{
					activeChar.setDietMode(true);
					activeChar.sendMessage("Diet mode on");
				}
				else if (st.nextToken().equalsIgnoreCase("off"))
				{
					activeChar.setDietMode(false);
					activeChar.sendMessage("Diet mode off");
				}
			}
			catch (final Exception ex)
			{
				if (activeChar.getDietMode())
				{
					activeChar.setDietMode(false);
					activeChar.sendMessage("Diet mode off");
				}
				else
				{
					activeChar.setDietMode(true);
					activeChar.sendMessage("Diet mode on");
				}
			}
			finally
			{
				activeChar.refreshOverloaded();
			}
			adminhtm.setFile(activeChar, activeChar.getLang(), "data/html/admin/gm_menu.htm");
			activeChar.sendPacket(adminhtm);
		}
		else if (command.startsWith("admin_tradeoff"))
		{
			try
			{
				final String mode = command.substring(15);
				if (mode.equalsIgnoreCase("on"))
				{
					activeChar.setVar("useBlockTrade@", "1");
					activeChar.sendMessage("Trade refusal enabled");
				}
				else if (mode.equalsIgnoreCase("off"))
				{
					activeChar.setVar("useBlockTrade@", "0");
					activeChar.sendMessage("Trade refusal disabled");
				}
			}
			catch (final Exception ex)
			{
				if (activeChar.getTradeRefusal())
				{
					activeChar.setVar("useBlockTrade@", "0");
					activeChar.sendMessage("Trade refusal disabled");
				}
				else
				{
					activeChar.setVar("useBlockTrade@", "1");
					activeChar.sendMessage("Trade refusal enabled");
				}
			}
			adminhtm.setFile(activeChar, activeChar.getLang(), "data/html/admin/gm_menu.htm");
			activeChar.sendPacket(adminhtm);
		}
		else if (command.startsWith("admin_reload"))
		{
			final StringTokenizer st = new StringTokenizer(command);
			st.nextToken();
			if (!st.hasMoreTokens())
			{
				activeChar.sendMessage("You need to specify a type to reload!");
				activeChar.sendMessage("Usage: //reload <multisell|buylist|teleport|skill|npc|htm|item|config|access|quests|door|walker|handler>");
				HelpPage.showHelpPage(activeChar, "reloads.htm");
				return false;
			}
			
			final String type = st.nextToken();
			try
			{
				if (type.equals("multisells"))
				{
					MultiSellParser.getInstance().load();
					activeChar.sendMessage("All Multisells have been reloaded");
				}
				else if (type.startsWith("buylists"))
				{
					BuyListParser.getInstance().load();
					activeChar.sendMessage("All BuyLists have been reloaded");
				}
				else if (type.startsWith("teleports"))
				{
					TeleLocationParser.getInstance().load();
					activeChar.sendMessage("Teleport Locations have been reloaded");
				}
				else if (type.startsWith("skills"))
				{
					SkillsParser.getInstance().reload();
					activeChar.sendMessage("All Skills have been reloaded");
				}
				else if (type.startsWith("skillTrees"))
				{
					SkillTreesParser.getInstance().load();
					activeChar.sendMessage("All skill trees have been reloaded");
				}
				else if (type.equals("npcs"))
				{
					NpcsParser.getInstance();
					QuestManager.getInstance().reloadAllQuests();
					activeChar.sendMessage("All NPCs have been reloaded");
				}
				else if (type.startsWith("drop"))
				{
					NpcsParser.getInstance().reloadAllDropAndSkills();
					activeChar.sendMessage("All NPCs drop have been reloaded");
				}
				else if (type.equals("npcStats"))
				{
					NpcStatManager.getInstance().reload();
					activeChar.sendMessage("All NPCs drop have been reloaded");
				}
				else if (type.startsWith("htmls"))
				{
					HtmCache.getInstance().reload();
					activeChar.sendMessage("Cache[HTML]: " + HtmCache.getInstance().getMemoryUsage() + " megabytes on " + HtmCache.getInstance().getLoadedFiles() + " files loaded");
				}
				else if (type.startsWith("items"))
				{
					ItemsParser.getInstance().reload();
					activeChar.sendMessage("Item Templates have been reloaded");
				}
				else if (type.startsWith("configs"))
				{
					Config.load();
					ClassMasterParser.getInstance().reload();
					RewardManager.getInstance().reload();
					PhaseEventConfig.getInstance().reload();
					activeChar.sendMessage("All Config Settings have been reloaded");
				}
				else if (type.startsWith("access"))
				{
					AdminParser.getInstance().load();
					activeChar.sendMessage("Access Rights have been reloaded");
				}
				else if (type.startsWith("quests"))
				{
					QuestManager.getInstance().reloadAllQuests();
					activeChar.sendMessage("All Quests have been reloaded");
				}
				else if (type.startsWith("doors"))
				{
					DoorParser.getInstance().load();
					activeChar.sendMessage("All Doors have been reloaded.");
				}
				else if (type.startsWith("walkers"))
				{
					WalkingManager.getInstance().load();
					activeChar.sendMessage("All Walkers have been reloaded");
				}
				else if (type.startsWith("reflections"))
				{
					ReflectionParser.getInstance().load();
					activeChar.sendMessage("All Reflections have been reloaded.");
				}
				else if (type.startsWith("enchant"))
				{
					EnchantItemGroupsParser.getInstance().load();
					EnchantItemParser.getInstance().load();
					activeChar.sendMessage(activeChar.getName(null) + ": Reloaded item enchanting data.");
				}
				else if (type.startsWith("transforms"))
				{
					TransformParser.getInstance().load();
					activeChar.sendMessage(activeChar.getName(null) + ": Reloaded transform data.");
				}
				else if (type.startsWith("scripts"))
				{
					ScriptListenerLoader.getInstance().executeScriptList();
					activeChar.sendMessage(activeChar.getName(null) + ": Reloaded all scripts.");
				}
				else if (type.startsWith("worldevents"))
				{
					WorldEventParser.getInstance().reload();
					for (final Quest event : QuestManager.getInstance().getQuests())
					{
						if (event instanceof AbstractWorldEvent)
						{
							final AbstractWorldEvent _event = (AbstractWorldEvent) QuestManager.getInstance().getQuest(event.getName());
							if (_event != null && !_event.isEventActive())
							{
								if (_event.isReloaded())
								{
									activeChar.sendMessage(_event.getEventTemplate().getName(activeChar.getLang()) + ": Reloaded!");
								}
							}
						}
					}
				}
				else if (type.startsWith("fightevents"))
				{
					for (final AbstractFightEvent event : FightEventParser.getInstance().getEvents().valueCollection())
					{
						if (event != null && FightEventManager.getInstance().getActiveEventTask(event.getId()) || event.getState() == AbstractFightEvent.EVENT_STATE.STARTED)
						{
							event.stopEvent();
						}
						FightEventManager.getInstance().clearEventIdTask(event.getId());
					}
					activeChar.sendMessage(activeChar.getName(null) + ": Disable all fight events.");
					FightEventParser.getInstance().reload();
					FightEventMapParser.getInstance().reload();
					FightEventNpcManager.getInstance().reload();
					FightEventManager.getInstance().reload();
					activeChar.sendMessage(activeChar.getName(null) + ": Reloaded all fight events.");
				}
				else if (type.startsWith("tournaments") || type.startsWith("tour"))
				{
					TournamentData.getInstance().getTournaments().stream().filter(Objects::nonNull).forEach(Tournament::stop);
					TournamentsParser.getInstance().reload();
					TournamentData.getInstance().reload();
					activeChar.sendMessage(activeChar.getName(null) + ": Reloaded all Tournaments events.");
				}
				else if (type.startsWith("promocodes"))
				{
					PromoCodeParser.getInstance().reload();
					activeChar.sendMessage(activeChar.getName(null) + ": Reloaded all promocodes.");
				}
				else if (type.startsWith("zones"))
				{
					ZoneManager.getInstance().reload();
					ZoneManager.getInstance().createZoneReflections();
					activeChar.sendMessage("All Zones have been reloaded");
				}
				else if (type.startsWith("armorSets"))
				{
					ArmorSetsParser.getInstance().load();
					activeChar.sendMessage("All armor sets have been reloaded");
				}
				else if (type.startsWith("category"))
				{
					CategoryParser.getInstance().load();
					activeChar.sendMessage("All categoryes have been reloaded");
				}
				else if (type.startsWith("charTemplates"))
				{
					CharTemplateParser.getInstance().load();
					activeChar.sendMessage("All char templates have been reloaded");
				}
				else if (type.startsWith("clans"))
				{
					ClanParser.getInstance().load();
					activeChar.sendMessage("All clans have been reloaded");
				}
				else if (type.startsWith("balancer"))
				{
					ClassBalanceParser.getInstance().load();
					SkillBalanceParser.getInstance().load();
					activeChar.sendMessage("All balancer settings have been reloaded");
				}
				else if (type.startsWith("classList"))
				{
					ClassListParser.getInstance().load();
					activeChar.sendMessage("All classes have been reloaded");
				}
				else if (type.startsWith("colosseumFences"))
				{
					ColosseumFenceParser.getInstance().load();
					activeChar.sendMessage("All colosseum fences have been reloaded");
				}
				else if (type.startsWith("communityTeleports"))
				{
					CommunityTeleportsParser.getInstance().load();
					activeChar.sendMessage("All community teleports have been reloaded");
				}
				else if (type.startsWith("communityBuffer"))
				{
					CommunityBuffer.getInstance().load();
					SchemesParser.getInstance().load();
					activeChar.sendMessage("Community buffer have been reloaded");
				}
				else if (type.startsWith("sellBuffs"))
				{
					SellBuffsParser.getInstance().load();
					activeChar.sendMessage("All sell buffs have been reloaded");
				}
				else if (type.startsWith("damageLimits"))
				{
					DamageLimitParser.getInstance().load();
					activeChar.sendMessage("All damage limits have been reloaded");
				}
				else if (type.startsWith("donations"))
				{
					DonationParser.getInstance().load();
					ExchangeItemParser.getInstance().load();
					FoundationParser.getInstance().load();
					activeChar.sendMessage("All donations have been reloaded");
				}
				else if (type.startsWith("dressMe"))
				{
					DressArmorParser.getInstance().load();
					DressCloakParser.getInstance().load();
					DressHatParser.getInstance().load();
					DressShieldParser.getInstance().load();
					DressWeaponParser.getInstance().load();
					activeChar.sendMessage("All dressme have been reloaded");
				}
				else if (type.startsWith("enchantItems"))
				{
					EnchantItemGroupsParser.getInstance().load();
					EnchantItemHPBonusParser.getInstance().load();
					EnchantItemOptionsParser.getInstance().load();
					EnchantItemParser.getInstance().load();
					EnchantSkillGroupsParser.getInstance().load();
					activeChar.sendMessage("All enchant item groups have been reloaded");
				}
				else if (type.startsWith("enchantSkills"))
				{
					EnchantSkillGroupsParser.getInstance().load();
					activeChar.sendMessage("All enchant skill groups have been reloaded");
				}
				else if (type.startsWith("experience"))
				{
					ExperienceParser.getInstance().load();
					ExpPercentLostParser.getInstance().load();
					activeChar.sendMessage("Experience table have been reloaded");
				}
				else if (type.startsWith("fishing"))
				{
					FishMonstersParser.getInstance().load();
					FishParser.getInstance().load();
					activeChar.sendMessage("Fishing have been reloaded");
				}
				else if (type.startsWith("hennas"))
				{
					HennaParser.getInstance().load();
					activeChar.sendMessage("Hennas have been reloaded");
				}
				else if (type.startsWith("initials"))
				{
					InitialBuffParser.getInstance().load();
					InitialEquipmentParser.getInstance().load();
					InitialShortcutParser.getInstance().load();
					activeChar.sendMessage("Initial buffs & items & shortcuts have been reloaded");
				}
				else if (type.startsWith("limits"))
				{
					LimitStatParser.getInstance().load();
					activeChar.sendMessage("All limits have been reloaded");
				}
				else if (type.startsWith("premiums"))
				{
					PremiumAccountsParser.getInstance().load();
					activeChar.sendMessage("All premium accounts have been reloaded");
				}
				else if (type.startsWith("itemmall"))
				{
					ProductItemParser.getInstance().load();
					activeChar.sendMessage("All itemmall items have been reloaded");
				}
				else if (type.startsWith("recipes"))
				{
					RecipeParser.getInstance().load();
					activeChar.sendMessage("All recipes have been reloaded");
				}
				else if (type.startsWith("specialRates"))
				{
					if (!SpecialRatesParser.getInstance().isActive())
					{
						SpecialRatesParser.getInstance().reload();
						activeChar.sendMessage("Special rate templates have been reloaded");
					}
					else
					{
						activeChar.sendMessage("Special rate is active now. Cant be reload!");
					}
				}
				HelpPage.showHelpPage(activeChar, "reloads.htm");
			}
			catch (final Exception e)
			{
				activeChar.sendMessage("An error occured while reloading " + type + " !");
				activeChar.sendMessage("Usage: //reload <multisell|buylist|teleport|skill|npc|htm|item|config|access|quests|door|walker|handler>");
				HelpPage.showHelpPage(activeChar, "reloads.htm");
				_log.warn("An error occured while reloading " + type + ": " + e, e);
			}
		}
		return true;
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
	
	private void showMainPage(Player activeChar, String command)
	{
		final NpcHtmlMessage adminhtm = new NpcHtmlMessage(5);
		
		int mode = 0;
		try
		{
			mode = Integer.parseInt(command.substring(11));
		}
		catch (final Exception e)
		{}
		switch (mode)
		{
			case 1 :
				adminhtm.setFile(activeChar, activeChar.getLang(), "data/html/admin/main_menu.htm");
				activeChar.sendPacket(adminhtm);
				break;
			case 2 :
				adminhtm.setFile(activeChar, activeChar.getLang(), "data/html/admin/game_menu.htm");
				activeChar.sendPacket(adminhtm);
				break;
			case 3 :
				adminhtm.setFile(activeChar, activeChar.getLang(), "data/html/admin/effects_menu.htm");
				activeChar.sendPacket(adminhtm);
				break;
			case 4 :
				adminhtm.setFile(activeChar, activeChar.getLang(), "data/html/admin/server_menu.htm");
				activeChar.sendPacket(adminhtm);
				break;
			case 5 :
				adminhtm.setFile(activeChar, activeChar.getLang(), "data/html/admin/mods_menu.htm");
				activeChar.sendPacket(adminhtm);
				break;
			case 6 :
				adminhtm.setFile(activeChar, activeChar.getLang(), "data/html/admin/char_menu.htm");
				activeChar.sendPacket(adminhtm);
				break;
			case 7 :
				adminhtm.setFile(activeChar, activeChar.getLang(), "data/html/admin/gm_menu.htm");
				activeChar.sendPacket(adminhtm);
				break;
			default :
				adminhtm.setFile(activeChar, activeChar.getLang(), "data/html/admin/main_menu.htm");
				activeChar.sendPacket(adminhtm);
				break;
		}
	}
}