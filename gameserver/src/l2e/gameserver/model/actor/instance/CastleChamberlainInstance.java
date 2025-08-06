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
package l2e.gameserver.model.actor.instance;

import java.text.SimpleDateFormat;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import l2e.commons.util.StringUtil;
import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.SevenSigns;
import l2e.gameserver.data.holder.ClanHolder;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.data.parser.TeleLocationParser;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.model.Clan;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.TeleportTemplate;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.entity.Castle;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.items.itemcontainer.PcInventory;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.skills.SkillType;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.ExShowDominionRegistry;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;
import l2e.gameserver.network.serverpackets.SystemMessage;

public class CastleChamberlainInstance extends MerchantInstance
{
	protected static final int COND_ALL_FALSE = 0;
	protected static final int COND_BUSY_BECAUSE_OF_SIEGE = 1;
	protected static final int COND_OWNER = 2;
	
	public CastleChamberlainInstance(int objectId, NpcTemplate template)
	{
		super(objectId, template);
		setInstanceType(InstanceType.CastleChamberlainInstance);
	}

	private void sendHtmlMessage(Player player, NpcHtmlMessage html)
	{
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%npcId%", String.valueOf(getId()));
		player.sendPacket(html);
	}
	
	@Override
	public void onBypassFeedback(Player player, String command)
	{
		if (player.getLastFolkNPC().getObjectId() != getObjectId())
		{
			return;
		}
		
		final var castle = getCastle();
		if (castle == null)
		{
			return;
		}
		
		final var template = castle.getTemplate();
		if (template == null)
		{
			return;
		}
		
		final SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm");
		final int condition = validateCondition(player);
		if (condition <= COND_ALL_FALSE)
		{
			return;
		}
		else if (condition == COND_BUSY_BECAUSE_OF_SIEGE)
		{
			return;
		}
		else if (condition == COND_OWNER)
		{
			final StringTokenizer st = new StringTokenizer(command, " ");
			final String actualCommand = st.nextToken();
			
			String val = "";
			if (st.hasMoreTokens())
			{
				val = st.nextToken();
			}
			
			if (actualCommand.equalsIgnoreCase("banish_foreigner"))
			{
				if (!validatePrivileges(player, Clan.CP_CS_DISMISS))
				{
					return;
				}
				if (siegeBlocksFunction(player))
				{
					return;
				}
				castle.banishForeigners();
				final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile(player, player.getLang(), "data/html/chamberlain/chamberlain-banishafter.htm");
				html.replace("%objectId%", String.valueOf(getObjectId()));
				player.sendPacket(html);
				return;
			}
			else if (actualCommand.equalsIgnoreCase("banish_foreigner_show"))
			{
				if (!validatePrivileges(player, Clan.CP_CS_DISMISS))
				{
					return;
				}
				if (siegeBlocksFunction(player))
				{
					return;
				}
				final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile(player, player.getLang(), "data/html/chamberlain/chamberlain-banishfore.htm");
				html.replace("%objectId%", String.valueOf(getObjectId()));
				player.sendPacket(html);
				return;
			}
			else if (actualCommand.equalsIgnoreCase("list_siege_clans"))
			{
				if ((player.getClanPrivileges() & Clan.CP_CS_MANAGE_SIEGE) == Clan.CP_CS_MANAGE_SIEGE)
				{
					castle.getSiege().listRegisterClan(player);
				}
				else
				{
					final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					html.setFile(player, player.getLang(), "data/html/chamberlain/chamberlain-noprivs.htm");
					player.sendPacket(html);
				}
				return;
			}
			else if (actualCommand.equalsIgnoreCase("list_territory_clans"))
			{
				if ((player.getClanPrivileges() & Clan.CP_CS_MANAGE_SIEGE) == Clan.CP_CS_MANAGE_SIEGE)
				{
					player.sendPacket(new ExShowDominionRegistry(castle.getId(), player));
				}
				else
				{
					final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					html.setFile(player, player.getLang(), "data/html/chamberlain/chamberlain-noprivs.htm");
					player.sendPacket(html);
				}
				return;
			}
			else if (actualCommand.equalsIgnoreCase("receive_report"))
			{
				if (player.isClanLeader())
				{
					final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					html.setFile(player, player.getLang(), "data/html/chamberlain/chamberlain-report.htm");
					html.replace("%objectId%", String.valueOf(getObjectId()));
					final Clan clan = ClanHolder.getInstance().getClan(castle.getOwnerId());
					html.replace("%clanname%", clan.getName());
					html.replace("%clanleadername%", clan.getLeaderName());
					html.replace("%castlename%", castle.getName(player.getLang()));
					
					final int currentPeriod = SevenSigns.getInstance().getCurrentPeriod();
					switch (currentPeriod)
					{
						case SevenSigns.PERIOD_COMP_RECRUITING :
							html.replace("%ss_event%", "Quest Event Initialization");
							break;
						case SevenSigns.PERIOD_COMPETITION :
							html.replace("%ss_event%", "Competition (Quest Event)");
							break;
						case SevenSigns.PERIOD_COMP_RESULTS :
							html.replace("%ss_event%", "Quest Event Results");
							break;
						case SevenSigns.PERIOD_SEAL_VALIDATION :
							html.replace("%ss_event%", "Seal Validation");
							break;
					}
					final int sealOwner1 = SevenSigns.getInstance().getSealOwner(1);
					switch (sealOwner1)
					{
						case SevenSigns.CABAL_NULL :
							html.replace("%ss_avarice%", "Not in Possession");
							break;
						case SevenSigns.CABAL_DAWN :
							html.replace("%ss_avarice%", "Lords of Dawn");
							break;
						case SevenSigns.CABAL_DUSK :
							html.replace("%ss_avarice%", "Revolutionaries of Dusk");
							break;
					}
					final int sealOwner2 = SevenSigns.getInstance().getSealOwner(2);
					switch (sealOwner2)
					{
						case SevenSigns.CABAL_NULL :
							html.replace("%ss_gnosis%", "Not in Possession");
							break;
						case SevenSigns.CABAL_DAWN :
							html.replace("%ss_gnosis%", "Lords of Dawn");
							break;
						case SevenSigns.CABAL_DUSK :
							html.replace("%ss_gnosis%", "Revolutionaries of Dusk");
							break;
					}
					final int sealOwner3 = SevenSigns.getInstance().getSealOwner(3);
					switch (sealOwner3)
					{
						case SevenSigns.CABAL_NULL :
							html.replace("%ss_strife%", "Not in Possession");
							break;
						case SevenSigns.CABAL_DAWN :
							html.replace("%ss_strife%", "Lords of Dawn");
							break;
						case SevenSigns.CABAL_DUSK :
							html.replace("%ss_strife%", "Revolutionaries of Dusk");
							break;
					}
					player.sendPacket(html);
				}
				else
				{
					final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					html.setFile(player, player.getLang(), "data/html/chamberlain/chamberlain-noprivs.htm");
					player.sendPacket(html);
				}
				return;
			}
			else if (actualCommand.equalsIgnoreCase("items"))
			{
				if ((player.getClanPrivileges() & Clan.CP_CS_USE_FUNCTIONS) == Clan.CP_CS_USE_FUNCTIONS)
				{
					if (val.isEmpty())
					{
						return;
					}

					if (Config.DEBUG)
					{
						_log.info("Showing chamberlain buylist");
					}
					
					showBuyWindow(player, Integer.parseInt(val + "1"));
					player.sendActionFailed();
				}
				else
				{
					final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					html.setFile(player, player.getLang(), "data/html/chamberlain/chamberlain-noprivs.htm");
					player.sendPacket(html);
				}
				return;
			}
			else if (actualCommand.equalsIgnoreCase("manage_siege_defender"))
			{
				if ((player.getClanPrivileges() & Clan.CP_CS_MANAGE_SIEGE) == Clan.CP_CS_MANAGE_SIEGE)
				{
					castle.getSiege().listRegisterClan(player);
				}
				else
				{
					final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					html.setFile(player, player.getLang(), "data/html/chamberlain/chamberlain-noprivs.htm");
					player.sendPacket(html);
				}
				return;
			}
			else if (actualCommand.equalsIgnoreCase("manage_vault"))
			{
				if ((player.getClanPrivileges() & Clan.CP_CS_TAXES) == Clan.CP_CS_TAXES)
				{
					String filename = "data/html/chamberlain/chamberlain-vault.htm";
					long amount = 0;
					if (val.equalsIgnoreCase("deposit"))
					{
						try
						{
							amount = Long.parseLong(st.nextToken());
						}
						catch (final NoSuchElementException e)
						{}
						if ((amount > 0) && ((castle.getTreasury() + amount) < PcInventory.MAX_ADENA))
						{
							if (player.reduceAdena("Castle", amount, this, true))
							{
								castle.addToTreasuryNoTax(amount);
							}
							else
							{
								sendPacket(SystemMessageId.YOU_NOT_ENOUGH_ADENA);
							}
						}
					}
					else if (val.equalsIgnoreCase("withdraw"))
					{
						try
						{
							amount = Long.parseLong(st.nextToken());
						}
						catch (final NoSuchElementException e)
						{}
						if (amount > 0)
						{
							if (castle.getTreasury() < amount)
							{
								filename = "data/html/chamberlain/chamberlain-vault-no.htm";
							}
							else
							{
								if (castle.addToTreasuryNoTax((-1) * amount))
								{
									player.addAdena("Castle", amount, this, true);
								}
							}
						}
					}
					final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					html.setFile(player, player.getLang(), filename);
					html.replace("%objectId%", String.valueOf(getObjectId()));
					html.replace("%npcname%", getName(player.getLang()));
					html.replace("%tax_income%", Util.formatAdena(castle.getTreasury()));
					html.replace("%withdraw_amount%", Util.formatAdena(amount));
					player.sendPacket(html);
				}
				else
				{
					final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					html.setFile(player, player.getLang(), "data/html/chamberlain/chamberlain-noprivs.htm");
					player.sendPacket(html);
				}
				return;
			}
			else if (actualCommand.equalsIgnoreCase("operate_door"))
			{
				if ((player.getClanPrivileges() & Clan.CP_CS_OPEN_DOOR) == Clan.CP_CS_OPEN_DOOR)
				{
					if (!val.isEmpty())
					{
						final boolean open = (Integer.parseInt(val) == 1);
						while (st.hasMoreTokens())
						{
							castle.openCloseDoor(player, Integer.parseInt(st.nextToken()), open);
						}
						
						final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
						String file = "data/html/chamberlain/doors-close.htm";
						if (open)
						{
							file = "data/html/chamberlain/doors-open.htm";
						}
						html.setFile(player, player.getLang(), file);
						html.replace("%objectId%", String.valueOf(getObjectId()));
						player.sendPacket(html);
						return;
					}
					
					final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					html.setFile(player, player.getLang(), "data/html/chamberlain/" + getTemplate().getId() + "-d.htm");
					html.replace("%objectId%", String.valueOf(getObjectId()));
					html.replace("%npcname%", getName(player.getLang()));
					player.sendPacket(html);
				}
				else
				{
					final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					html.setFile(player, player.getLang(), "data/html/chamberlain/chamberlain-noprivs.htm");
					player.sendPacket(html);
				}
				return;
			}
			else if (actualCommand.equalsIgnoreCase("tax_set"))
			{
				if ((player.getClanPrivileges() & Clan.CP_CS_TAXES) == Clan.CP_CS_TAXES)
				{
					if (!val.isEmpty())
					{
						castle.setTaxPercent(player, Integer.parseInt(val));
					}
					
					final String msg = StringUtil.concat("<html><body>", getName(player.getLang()), ":<br>" + "Current tax rate: ", String.valueOf(castle.getTaxPercent()), "%<br>" + "<table>" + "<tr>" + "<td>Change tax rate to:</td>" + "<td><edit var=\"value\" width=40><br>" + "<button value=\"Adjust\" action=\"bypass -h npc_%objectId%_tax_set $value\" width=80 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>" + "</tr>" + "</table>" + "</center>" + "</body></html>");
					sendHtmlMessage(player, msg);
				}
				else
				{
					final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					html.setFile(player, player.getLang(), "data/html/chamberlain/chamberlain-tax.htm");
					html.replace("%objectId%", String.valueOf(getObjectId()));
					html.replace("%tax%", String.valueOf(castle.getTaxPercent()));
					player.sendPacket(html);
				}
				return;
			}
			else if (actualCommand.equalsIgnoreCase("manage_functions"))
			{
				final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile(player, player.getLang(), "data/html/chamberlain/chamberlain-manage.htm");
				html.replace("%objectId%", String.valueOf(getObjectId()));
				player.sendPacket(html);
				return;
			}
			else if (actualCommand.equalsIgnoreCase("products"))
			{
				final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile(player, player.getLang(), "data/html/chamberlain/chamberlain-products.htm");
				html.replace("%objectId%", String.valueOf(getObjectId()));
				html.replace("%npcId%", String.valueOf(getId()));
				player.sendPacket(html);
				return;
			}
			else if (actualCommand.equalsIgnoreCase("functions"))
			{
				if (val.equalsIgnoreCase("tele"))
				{
					final NpcHtmlMessage html = new NpcHtmlMessage(1);
					if (castle.getFunction(Castle.FUNC_TELEPORT) == null)
					{
						html.setFile(player, player.getLang(), "data/html/chamberlain/chamberlain-nac.htm");
					}
					else
					{
						html.setFile(player, player.getLang(), "data/html/chamberlain/" + getId() + "-t" + castle.getFunction(Castle.FUNC_TELEPORT).getLvl() + ".htm");
					}
					sendHtmlMessage(player, html);
				}
				else if (val.equalsIgnoreCase("support"))
				{
					final NpcHtmlMessage html = new NpcHtmlMessage(1);
					if (castle.getFunction(Castle.FUNC_SUPPORT) == null)
					{
						html.setFile(player, player.getLang(), "data/html/chamberlain/chamberlain-nac.htm");
					}
					else
					{
						html.setFile(player, player.getLang(), "data/html/chamberlain/support" + castle.getFunction(Castle.FUNC_SUPPORT).getLvl() + ".htm");
						html.replace("%mp%", String.valueOf((int) getCurrentMp()));
					}
					sendHtmlMessage(player, html);
				}
				else if (val.equalsIgnoreCase("back"))
				{
					showChatWindow(player);
				}
				else
				{
					final NpcHtmlMessage html = new NpcHtmlMessage(1);
					html.setFile(player, player.getLang(), "data/html/chamberlain/chamberlain-functions.htm");
					if (castle.getFunction(Castle.FUNC_RESTORE_EXP) != null)
					{
						html.replace("%xp_regen%", String.valueOf(castle.getFunction(Castle.FUNC_RESTORE_EXP).getLvl()));
					}
					else
					{
						html.replace("%xp_regen%", "0");
					}
					if (castle.getFunction(Castle.FUNC_RESTORE_HP) != null)
					{
						html.replace("%hp_regen%", String.valueOf(castle.getFunction(Castle.FUNC_RESTORE_HP).getLvl()));
					}
					else
					{
						html.replace("%hp_regen%", "0");
					}
					if (castle.getFunction(Castle.FUNC_RESTORE_MP) != null)
					{
						html.replace("%mp_regen%", String.valueOf(castle.getFunction(Castle.FUNC_RESTORE_MP).getLvl()));
					}
					else
					{
						html.replace("%mp_regen%", "0");
					}
					sendHtmlMessage(player, html);
				}
				return;
			}
			else if (actualCommand.equalsIgnoreCase("manage"))
			{
				if ((player.getClanPrivileges() & Clan.CP_CS_SET_FUNCTIONS) == Clan.CP_CS_SET_FUNCTIONS)
				{
					if (val.equalsIgnoreCase("recovery"))
					{
						if (st.countTokens() >= 1)
						{
							if (castle.getOwnerId() == 0)
							{
								player.sendMessage("This castle have no owner, you cannot change configuration");
								return;
							}
							val = st.nextToken();
							if (val.equalsIgnoreCase("hp_cancel"))
							{
								final NpcHtmlMessage html = new NpcHtmlMessage(1);
								html.setFile(player, player.getLang(), "data/html/chamberlain/functions-cancel.htm");
								html.replace("%apply%", "recovery hp 0");
								sendHtmlMessage(player, html);
								return;
							}
							else if (val.equalsIgnoreCase("mp_cancel"))
							{
								final NpcHtmlMessage html = new NpcHtmlMessage(1);
								html.setFile(player, player.getLang(), "data/html/chamberlain/functions-cancel.htm");
								html.replace("%apply%", "recovery mp 0");
								sendHtmlMessage(player, html);
								return;
							}
							else if (val.equalsIgnoreCase("exp_cancel"))
							{
								final NpcHtmlMessage html = new NpcHtmlMessage(1);
								html.setFile(player, player.getLang(), "data/html/chamberlain/functions-cancel.htm");
								html.replace("%apply%", "recovery exp 0");
								sendHtmlMessage(player, html);
								return;
							}
							else if (val.equalsIgnoreCase("edit_hp"))
							{
								val = st.nextToken();
								final NpcHtmlMessage html = new NpcHtmlMessage(1);
								html.setFile(player, player.getLang(), "data/html/chamberlain/functions-apply.htm");
								html.replace("%name%", "Fireplace (HP Recovery Device)");
								final int percent = Integer.parseInt(val);
								int cost;
								switch (percent)
								{
									case 80 :
										cost = template.getHpRegenFunction(1);
										break;
									case 120 :
										cost = template.getHpRegenFunction(2);
										break;
									case 180 :
										cost = template.getHpRegenFunction(3);
										break;
									case 240 :
										cost = template.getHpRegenFunction(4);
										break;
									default :
										cost = template.getHpRegenFunction(5);
										break;
								}
								
								html.replace("%cost%", String.valueOf(cost) + "</font>Adena /" + String.valueOf(template.getHpRegenFunctionTime()) + " Day</font>)");
								html.replace("%use%", "Provides additional HP recovery for clan members in the castle.<font color=\"00FFFF\">" + String.valueOf(percent) + "%</font>");
								html.replace("%apply%", "recovery hp " + String.valueOf(percent));
								sendHtmlMessage(player, html);
								return;
							}
							else if (val.equalsIgnoreCase("edit_mp"))
							{
								val = st.nextToken();
								final NpcHtmlMessage html = new NpcHtmlMessage(1);
								html.setFile(player, player.getLang(), "data/html/chamberlain/functions-apply.htm");
								html.replace("%name%", "Carpet (MP Recovery)");
								final int percent = Integer.parseInt(val);
								int cost;
								switch (percent)
								{
									case 5 :
										cost = template.getMpRegenFunction(1);
										break;
									case 15 :
										cost = template.getMpRegenFunction(2);
										break;
									case 30 :
										cost = template.getMpRegenFunction(3);
										break;
									default :
										cost = template.getMpRegenFunction(4);
										break;
								}
								html.replace("%cost%", String.valueOf(cost) + "</font>Adena /" + String.valueOf(template.getMpRegenFunctionTime()) + " Day</font>)");
								html.replace("%use%", "Provides additional MP recovery for clan members in the castle.<font color=\"00FFFF\">" + String.valueOf(percent) + "%</font>");
								html.replace("%apply%", "recovery mp " + String.valueOf(percent));
								sendHtmlMessage(player, html);
								return;
							}
							else if (val.equalsIgnoreCase("edit_exp"))
							{
								val = st.nextToken();
								final NpcHtmlMessage html = new NpcHtmlMessage(1);
								html.setFile(player, player.getLang(), "data/html/chamberlain/functions-apply.htm");
								html.replace("%name%", "Chandelier (EXP Recovery Device)");
								final int percent = Integer.parseInt(val);
								int cost;
								switch (percent)
								{
									case 15 :
										cost = template.getExpRegenFunction(1);
										break;
									case 25 :
										cost = template.getExpRegenFunction(2);
										break;
									case 35 :
										cost = template.getExpRegenFunction(3);
										break;
									default :
										cost = template.getExpRegenFunction(4);
										break;
								}
								html.replace("%cost%", String.valueOf(cost) + "</font>Adena /" + String.valueOf(template.getExpRegenFunctionTime()) + " Day</font>)");
								html.replace("%use%", "Restores the Exp of any clan member who is resurrected in the castle.<font color=\"00FFFF\">" + String.valueOf(percent) + "%</font>");
								html.replace("%apply%", "recovery exp " + String.valueOf(percent));
								sendHtmlMessage(player, html);
								return;
							}
							else if (val.equalsIgnoreCase("hp"))
							{
								if (st.countTokens() >= 1)
								{
									int fee;
									if (Config.DEBUG)
									{
										_log.warn("Hp editing invoked");
									}
									val = st.nextToken();
									final NpcHtmlMessage html = new NpcHtmlMessage(1);
									html.setFile(player, player.getLang(), "data/html/chamberlain/functions-apply_confirmed.htm");
									if (castle.getFunction(Castle.FUNC_RESTORE_HP) != null)
									{
										if (castle.getFunction(Castle.FUNC_RESTORE_HP).getLvl() == Integer.parseInt(val))
										{
											html.setFile(player, player.getLang(), "data/html/chamberlain/functions-used.htm");
											html.replace("%val%", String.valueOf(val) + "%");
											sendHtmlMessage(player, html);
											return;
										}
									}
									final int percent = Integer.parseInt(val);
									switch (percent)
									{
										case 0 :
											fee = 0;
											html.setFile(player, player.getLang(), "data/html/chamberlain/functions-cancel_confirmed.htm");
											break;
										case 80 :
											fee = template.getHpRegenFunction(1);
											break;
										case 120 :
											fee = template.getHpRegenFunction(2);
											break;
										case 180 :
											fee = template.getHpRegenFunction(3);
											break;
										case 240 :
											fee = template.getHpRegenFunction(4);
											break;
										default :
											fee = template.getHpRegenFunction(5);
											break;
									}
									if (!castle.updateFunctions(player, Castle.FUNC_RESTORE_HP, percent, fee, (template.getHpRegenFunctionTime() * 86400000L), (castle.getFunction(Castle.FUNC_RESTORE_HP) == null)))
									{
										html.setFile(player, player.getLang(), "data/html/chamberlain/low_adena.htm");
										sendHtmlMessage(player, html);
									}
									sendHtmlMessage(player, html);
								}
								return;
							}
							else if (val.equalsIgnoreCase("mp"))
							{
								if (st.countTokens() >= 1)
								{
									int fee;
									if (Config.DEBUG)
									{
										_log.warn("Mp editing invoked");
									}
									val = st.nextToken();
									final NpcHtmlMessage html = new NpcHtmlMessage(1);
									html.setFile(player, player.getLang(), "data/html/chamberlain/functions-apply_confirmed.htm");
									if (castle.getFunction(Castle.FUNC_RESTORE_MP) != null)
									{
										if (castle.getFunction(Castle.FUNC_RESTORE_MP).getLvl() == Integer.parseInt(val))
										{
											html.setFile(player, player.getLang(), "data/html/chamberlain/functions-used.htm");
											html.replace("%val%", String.valueOf(val) + "%");
											sendHtmlMessage(player, html);
											return;
										}
									}
									final int percent = Integer.parseInt(val);
									switch (percent)
									{
										case 0 :
											fee = 0;
											html.setFile(player, player.getLang(), "data/html/chamberlain/functions-cancel_confirmed.htm");
											break;
										case 5 :
											fee = template.getMpRegenFunction(1);
											break;
										case 15 :
											fee = template.getMpRegenFunction(2);
											break;
										case 30 :
											fee = template.getMpRegenFunction(3);
											break;
										default :
											fee = template.getMpRegenFunction(4);
											break;
									}
									if (!castle.updateFunctions(player, Castle.FUNC_RESTORE_MP, percent, fee, (template.getMpRegenFunctionTime() * 86400000L), (castle.getFunction(Castle.FUNC_RESTORE_MP) == null)))
									{
										html.setFile(player, player.getLang(), "data/html/chamberlain/low_adena.htm");
										sendHtmlMessage(player, html);
									}
									sendHtmlMessage(player, html);
								}
								return;
							}
							else if (val.equalsIgnoreCase("exp"))
							{
								if (st.countTokens() >= 1)
								{
									int fee;
									if (Config.DEBUG)
									{
										_log.warn("Exp editing invoked");
									}
									val = st.nextToken();
									final NpcHtmlMessage html = new NpcHtmlMessage(1);
									html.setFile(player, player.getLang(), "data/html/chamberlain/functions-apply_confirmed.htm");
									if (castle.getFunction(Castle.FUNC_RESTORE_EXP) != null)
									{
										if (castle.getFunction(Castle.FUNC_RESTORE_EXP).getLvl() == Integer.parseInt(val))
										{
											html.setFile(player, player.getLang(), "data/html/chamberlain/functions-used.htm");
											html.replace("%val%", String.valueOf(val) + "%");
											sendHtmlMessage(player, html);
											return;
										}
									}
									final int percent = Integer.parseInt(val);
									switch (percent)
									{
										case 0 :
											fee = 0;
											html.setFile(player, player.getLang(), "data/html/chamberlain/functions-cancel_confirmed.htm");
											break;
										case 15 :
											fee = template.getExpRegenFunction(1);
											break;
										case 25 :
											fee = template.getExpRegenFunction(2);
											break;
										case 35 :
											fee = template.getExpRegenFunction(3);
											break;
										default :
											fee = template.getExpRegenFunction(4);
											break;
									}
									if (!castle.updateFunctions(player, Castle.FUNC_RESTORE_EXP, percent, fee, (template.getExpRegenFunctionTime() * 86400000L), (castle.getFunction(Castle.FUNC_RESTORE_EXP) == null)))
									{
										html.setFile(player, player.getLang(), "data/html/chamberlain/low_adena.htm");
										sendHtmlMessage(player, html);
									}
									sendHtmlMessage(player, html);
								}
								return;
							}
						}
						final NpcHtmlMessage html = new NpcHtmlMessage(1);
						html.setFile(player, player.getLang(), "data/html/chamberlain/edit_recovery.htm");
						final String hp = "[<a action=\"bypass -h npc_%objectId%_manage recovery edit_hp 80\">80%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_hp 120\">120%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_hp 180\">180%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_hp 240\">240%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_hp 300\">300%</a>]";
						final String exp = "[<a action=\"bypass -h npc_%objectId%_manage recovery edit_exp 15\">15%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_exp 25\">25%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_exp 35\">35%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_exp 50\">50%</a>]";
						final String mp = "[<a action=\"bypass -h npc_%objectId%_manage recovery edit_mp 5\">5%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_mp 15\">15%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_mp 30\">30%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_mp 40\">40%</a>]";
						if (castle.getFunction(Castle.FUNC_RESTORE_HP) != null)
						{
							html.replace("%hp_recovery%", String.valueOf(castle.getFunction(Castle.FUNC_RESTORE_HP).getLvl()) + "%</font> (<font color=\"FFAABB\">" + String.valueOf(castle.getFunction(Castle.FUNC_RESTORE_HP).getLease()) + "</font>Adena /" + String.valueOf(template.getHpRegenFunctionTime()) + " Day)");
							html.replace("%hp_period%", "Withdraw the fee for the next time at " + format.format(castle.getFunction(Castle.FUNC_RESTORE_HP).getEndTime()));
							html.replace("%change_hp%", "[<a action=\"bypass -h npc_%objectId%_manage recovery hp_cancel\">Deactivate</a>]" + hp);
						}
						else
						{
							html.replace("%hp_recovery%", "none");
							html.replace("%hp_period%", "none");
							html.replace("%change_hp%", hp);
						}
						if (castle.getFunction(Castle.FUNC_RESTORE_EXP) != null)
						{
							html.replace("%exp_recovery%", String.valueOf(castle.getFunction(Castle.FUNC_RESTORE_EXP).getLvl()) + "%</font> (<font color=\"FFAABB\">" + String.valueOf(castle.getFunction(Castle.FUNC_RESTORE_EXP).getLease()) + "</font>Adena /" + String.valueOf(template.getExpRegenFunctionTime()) + " Day)");
							html.replace("%exp_period%", "Withdraw the fee for the next time at " + format.format(castle.getFunction(Castle.FUNC_RESTORE_EXP).getEndTime()));
							html.replace("%change_exp%", "[<a action=\"bypass -h npc_%objectId%_manage recovery exp_cancel\">Deactivate</a>]" + exp);
						}
						else
						{
							html.replace("%exp_recovery%", "none");
							html.replace("%exp_period%", "none");
							html.replace("%change_exp%", exp);
						}
						if (castle.getFunction(Castle.FUNC_RESTORE_MP) != null)
						{
							html.replace("%mp_recovery%", String.valueOf(castle.getFunction(Castle.FUNC_RESTORE_MP).getLvl()) + "%</font> (<font color=\"FFAABB\">" + String.valueOf(castle.getFunction(Castle.FUNC_RESTORE_MP).getLease()) + "</font>Adena /" + String.valueOf(template.getMpRegenFunctionTime()) + " Day)");
							html.replace("%mp_period%", "Withdraw the fee for the next time at " + format.format(castle.getFunction(Castle.FUNC_RESTORE_MP).getEndTime()));
							html.replace("%change_mp%", "[<a action=\"bypass -h npc_%objectId%_manage recovery mp_cancel\">Deactivate</a>]" + mp);
						}
						else
						{
							html.replace("%mp_recovery%", "none");
							html.replace("%mp_period%", "none");
							html.replace("%change_mp%", mp);
						}
						sendHtmlMessage(player, html);
					}
					else if (val.equalsIgnoreCase("other"))
					{
						if (st.countTokens() >= 1)
						{
							if (castle.getOwnerId() == 0)
							{
								player.sendMessage("This castle have no owner, you cannot change configuration");
								return;
							}
							val = st.nextToken();
							if (val.equalsIgnoreCase("tele_cancel"))
							{
								final NpcHtmlMessage html = new NpcHtmlMessage(1);
								html.setFile(player, player.getLang(), "data/html/chamberlain/functions-cancel.htm");
								html.replace("%apply%", "other tele 0");
								sendHtmlMessage(player, html);
								return;
							}
							else if (val.equalsIgnoreCase("support_cancel"))
							{
								final NpcHtmlMessage html = new NpcHtmlMessage(1);
								html.setFile(player, player.getLang(), "data/html/chamberlain/functions-cancel.htm");
								html.replace("%apply%", "other support 0");
								sendHtmlMessage(player, html);
								return;
							}
							else if (val.equalsIgnoreCase("edit_support"))
							{
								val = st.nextToken();
								final NpcHtmlMessage html = new NpcHtmlMessage(1);
								html.setFile(player, player.getLang(), "data/html/chamberlain/functions-apply.htm");
								html.replace("%name%", "Insignia (Supplementary Magic)");
								final int stage = Integer.parseInt(val);
								int cost;
								switch (stage)
								{
									case 1 :
										cost = template.getSupportFunction(1);
										break;
									case 2 :
										cost = template.getSupportFunction(2);
										break;
									case 3 :
										cost = template.getSupportFunction(3);
										break;
									default :
										cost = template.getSupportFunction(4);
										break;
								}
								html.replace("%cost%", String.valueOf(cost) + "</font>Adena /" + String.valueOf(template.getSupportFunctionTime()) + " Day</font>)");
								html.replace("%use%", "Enables the use of supplementary magic.");
								html.replace("%apply%", "other support " + String.valueOf(stage));
								sendHtmlMessage(player, html);
								return;
							}
							else if (val.equalsIgnoreCase("edit_tele"))
							{
								val = st.nextToken();
								final NpcHtmlMessage html = new NpcHtmlMessage(1);
								html.setFile(player, player.getLang(), "data/html/chamberlain/functions-apply.htm");
								html.replace("%name%", "Mirror (Teleportation Device)");
								final int stage = Integer.parseInt(val);
								int cost;
								switch (stage)
								{
									case 1 :
										cost = template.getTeleportFunction(1);
										break;
									default :
										cost = template.getTeleportFunction(2);
										break;
								}
								html.replace("%cost%", String.valueOf(cost) + "</font>Adena /" + String.valueOf(template.getTeleportFunctionTime()) + " Day</font>)");
								html.replace("%use%", "Teleports clan members in a castle to the target <font color=\"00FFFF\">Stage " + String.valueOf(stage) + "</font> staging area");
								html.replace("%apply%", "other tele " + String.valueOf(stage));
								sendHtmlMessage(player, html);
								return;
							}
							else if (val.equalsIgnoreCase("tele"))
							{
								if (st.countTokens() >= 1)
								{
									int fee;
									if (Config.DEBUG)
									{
										_log.warn("Tele editing invoked");
									}
									val = st.nextToken();
									final NpcHtmlMessage html = new NpcHtmlMessage(1);
									html.setFile(player, player.getLang(), "data/html/chamberlain/functions-apply_confirmed.htm");
									if (castle.getFunction(Castle.FUNC_TELEPORT) != null)
									{
										if (castle.getFunction(Castle.FUNC_TELEPORT).getLvl() == Integer.parseInt(val))
										{
											html.setFile(player, player.getLang(), "data/html/chamberlain/functions-used.htm");
											html.replace("%val%", "Stage " + String.valueOf(val));
											sendHtmlMessage(player, html);
											return;
										}
									}
									final int lvl = Integer.parseInt(val);
									switch (lvl)
									{
										case 0 :
											fee = 0;
											html.setFile(player, player.getLang(), "data/html/chamberlain/functions-cancel_confirmed.htm");
											break;
										case 1 :
											fee = template.getTeleportFunction(1);
											break;
										default :
											fee = template.getTeleportFunction(2);
											break;
									}
									if (!castle.updateFunctions(player, Castle.FUNC_TELEPORT, lvl, fee, (template.getTeleportFunctionTime() * 86400000L), (castle.getFunction(Castle.FUNC_TELEPORT) == null)))
									{
										html.setFile(player, player.getLang(), "data/html/chamberlain/low_adena.htm");
										sendHtmlMessage(player, html);
									}
									sendHtmlMessage(player, html);
								}
								return;
							}
							else if (val.equalsIgnoreCase("support"))
							{
								if (st.countTokens() >= 1)
								{
									int fee;
									if (Config.DEBUG)
									{
										_log.warn("Support editing invoked");
									}
									val = st.nextToken();
									final NpcHtmlMessage html = new NpcHtmlMessage(1);
									html.setFile(player, player.getLang(), "data/html/chamberlain/functions-apply_confirmed.htm");
									if (castle.getFunction(Castle.FUNC_SUPPORT) != null)
									{
										if (castle.getFunction(Castle.FUNC_SUPPORT).getLvl() == Integer.parseInt(val))
										{
											html.setFile(player, player.getLang(), "data/html/chamberlain/functions-used.htm");
											html.replace("%val%", "Stage " + String.valueOf(val));
											sendHtmlMessage(player, html);
											return;
										}
									}
									final int lvl = Integer.parseInt(val);
									switch (lvl)
									{
										case 0 :
											fee = 0;
											html.setFile(player, player.getLang(), "data/html/chamberlain/functions-cancel_confirmed.htm");
											break;
										case 1 :
											fee = template.getSupportFunction(1);
											break;
										case 2 :
											fee = template.getSupportFunction(2);
											break;
										case 3 :
											fee = template.getSupportFunction(3);
											break;
										default :
											fee = template.getSupportFunction(4);
											break;
									}
									if (!castle.updateFunctions(player, Castle.FUNC_SUPPORT, lvl, fee, (template.getSupportFunctionTime() * 86400000L), (castle.getFunction(Castle.FUNC_SUPPORT) == null)))
									{
										html.setFile(player, player.getLang(), "data/html/chamberlain/low_adena.htm");
										sendHtmlMessage(player, html);
									}
									else
									{
										sendHtmlMessage(player, html);
									}
								}
								return;
							}
						}
						final NpcHtmlMessage html = new NpcHtmlMessage(1);
						html.setFile(player, player.getLang(), "data/html/chamberlain/edit_other.htm");
						final String tele = "[<a action=\"bypass -h npc_%objectId%_manage other edit_tele 1\">Level 1</a>][<a action=\"bypass -h npc_%objectId%_manage other edit_tele 2\">Level 2</a>]";
						final String support = "[<a action=\"bypass -h npc_%objectId%_manage other edit_support 1\">Level 1</a>][<a action=\"bypass -h npc_%objectId%_manage other edit_support 2\">Level 2</a>][<a action=\"bypass -h npc_%objectId%_manage other edit_support 3\">Level 3</a>][<a action=\"bypass -h npc_%objectId%_manage other edit_support 4\">Level 4</a>]";
						if (castle.getFunction(Castle.FUNC_TELEPORT) != null)
						{
							html.replace("%tele%", "Stage " + String.valueOf(castle.getFunction(Castle.FUNC_TELEPORT).getLvl()) + "</font> (<font color=\"FFAABB\">" + String.valueOf(castle.getFunction(Castle.FUNC_TELEPORT).getLease()) + "</font>Adena /" + String.valueOf(template.getTeleportFunctionTime()) + " Day)");
							html.replace("%tele_period%", "Withdraw the fee for the next time at " + format.format(castle.getFunction(Castle.FUNC_TELEPORT).getEndTime()));
							html.replace("%change_tele%", "[<a action=\"bypass -h npc_%objectId%_manage other tele_cancel\">Deactivate</a>]" + tele);
						}
						else
						{
							html.replace("%tele%", "none");
							html.replace("%tele_period%", "none");
							html.replace("%change_tele%", tele);
						}
						if (castle.getFunction(Castle.FUNC_SUPPORT) != null)
						{
							html.replace("%support%", "Stage " + String.valueOf(castle.getFunction(Castle.FUNC_SUPPORT).getLvl()) + "</font> (<font color=\"FFAABB\">" + String.valueOf(castle.getFunction(Castle.FUNC_SUPPORT).getLease()) + "</font>Adena /" + String.valueOf(template.getSupportFunctionTime()) + " Day)");
							html.replace("%support_period%", "Withdraw the fee for the next time at " + format.format(castle.getFunction(Castle.FUNC_SUPPORT).getEndTime()));
							html.replace("%change_support%", "[<a action=\"bypass -h npc_%objectId%_manage other support_cancel\">Deactivate</a>]" + support);
						}
						else
						{
							html.replace("%support%", "none");
							html.replace("%support_period%", "none");
							html.replace("%change_support%", support);
						}
						sendHtmlMessage(player, html);
					}
					else if (val.equalsIgnoreCase("back"))
					{
						showChatWindow(player);
					}
					else
					{
						final NpcHtmlMessage html = new NpcHtmlMessage(1);
						html.setFile(player, player.getLang(), "data/html/chamberlain/manage.htm");
						sendHtmlMessage(player, html);
					}
				}
				else
				{
					final NpcHtmlMessage html = new NpcHtmlMessage(1);
					html.setFile(player, player.getLang(), "data/html/chamberlain/chamberlain-noprivs.htm");
					sendHtmlMessage(player, html);
				}
				return;
			}
			else if (actualCommand.equalsIgnoreCase("support"))
			{
				setTarget(player);
				Skill skill;
				if (val.isEmpty())
				{
					return;
				}
				
				try
				{
					final int skill_id = Integer.parseInt(val);
					try
					{
						if (castle.getFunction(Castle.FUNC_SUPPORT) == null)
						{
							return;
						}
						if (castle.getFunction(Castle.FUNC_SUPPORT).getLvl() == 0)
						{
							return;
						}
						final NpcHtmlMessage html = new NpcHtmlMessage(1);
						int skill_lvl = 0;
						if (st.countTokens() >= 1)
						{
							skill_lvl = Integer.parseInt(st.nextToken());
						}
						skill = SkillsParser.getInstance().getInfo(skill_id, skill_lvl);
						if (skill.getSkillType() == SkillType.SUMMON)
						{
							player.doSimultaneousCast(skill);
						}
						else
						{
							if (!((skill.getMpConsume() + skill.getMpInitialConsume()) > getCurrentMp()))
							{
								this.doCast(skill);
							}
							else
							{
								html.setFile(player, player.getLang(), "data/html/chamberlain/support-no_mana.htm");
								html.replace("%mp%", String.valueOf((int) getCurrentMp()));
								sendHtmlMessage(player, html);
								return;
							}
						}
						html.setFile(player, player.getLang(), "data/html/chamberlain/support-done.htm");
						html.replace("%mp%", String.valueOf((int) getCurrentMp()));
						sendHtmlMessage(player, html);
					}
					catch (final Exception e)
					{
						player.sendMessage("Invalid skill level, contact your admin!");
					}
				}
				catch (final Exception e)
				{
					player.sendMessage("Invalid skill level, contact your admin!");
				}
				return;
			}
			else if (actualCommand.equalsIgnoreCase("support_back"))
			{
				final NpcHtmlMessage html = new NpcHtmlMessage(1);
				if (castle.getFunction(Castle.FUNC_SUPPORT).getLvl() == 0)
				{
					return;
				}
				html.setFile(player, player.getLang(), "data/html/chamberlain/support" + castle.getFunction(Castle.FUNC_SUPPORT).getLvl() + ".htm");
				html.replace("%mp%", String.valueOf((int) getStatus().getCurrentMp()));
				sendHtmlMessage(player, html);
				return;
			}
			else if (actualCommand.equalsIgnoreCase("goto"))
			{
				final int whereTo = Integer.parseInt(val);
				doTeleport(player, whereTo);
				return;
			}
			else if (actualCommand.equals("give_crown"))
			{
				if (siegeBlocksFunction(player))
				{
					return;
				}
				
				final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				
				if (player.isClanLeader())
				{
					if (player.getInventory().getItemByItemId(6841) == null)
					{
						final ItemInstance crown = player.getInventory().addItem("Castle Crown", 6841, 1, player, this);
						
						final SystemMessage ms = SystemMessage.getSystemMessage(SystemMessageId.EARNED_ITEM_S1);
						ms.addItemName(crown);
						player.sendPacket(ms);
						
						html.setFile(player, player.getLang(), "data/html/chamberlain/chamberlain-gavecrown.htm");
						html.replace("%CharName%", String.valueOf(player.getName(null)));
						html.replace("%FeudName%", String.valueOf(castle.getName(player.getLang())));
					}
					else
					{
						html.setFile(player, player.getLang(), "data/html/chamberlain/chamberlain-hascrown.htm");
					}
				}
				else
				{
					html.setFile(player, player.getLang(), "data/html/chamberlain/chamberlain-noprivs.htm");
				}
				
				player.sendPacket(html);
				return;
			}
			else if (actualCommand.equalsIgnoreCase("manors_cert"))
			{
				final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				if (isMyLord(player) || (validatePrivileges(player, 5) && (validateCondition(player) == COND_OWNER)))
				{
					if (castle.getSiege().getIsInProgress())
					{
						html.setFile(player, player.getLang(), "data/html/chamberlain/chamberlain-busy.htm");
						html.replace("%npcname%", String.valueOf(getName(player.getLang())));
					}
					else
					{
						final int cabal = SevenSigns.getInstance().getPlayerCabal(player.getObjectId());
						if ((cabal == SevenSigns.CABAL_DAWN) && SevenSigns.getInstance().isCompetitionPeriod())
						{
							final int ticketCount = castle.getTicketBuyCount();
							if (ticketCount < (Config.SSQ_DAWN_TICKET_QUANTITY / Config.SSQ_DAWN_TICKET_BUNDLE))
							{
								html.setFile(player, player.getLang(), "data/html/chamberlain/ssq_selldawnticket.htm");
								html.replace("%DawnTicketLeft%", String.valueOf(Config.SSQ_DAWN_TICKET_QUANTITY - (ticketCount * Config.SSQ_DAWN_TICKET_BUNDLE)));
								html.replace("%DawnTicketBundle%", String.valueOf(Config.SSQ_DAWN_TICKET_BUNDLE));
								html.replace("%DawnTicketPrice%", String.valueOf(Config.SSQ_DAWN_TICKET_PRICE * Config.SSQ_DAWN_TICKET_BUNDLE));
							}
							else
							{
								html.setFile(player, player.getLang(), "data/html/chamberlain/ssq_notenoughticket.htm");
							}
						}
						else
						{
							html.setFile(player, player.getLang(), "data/html/chamberlain/ssq_notdawnorevent.htm");
						}
					}
				}
				else
				{
					html.setFile(player, player.getLang(), "data/html/chamberlain/chamberlain-noprivs.htm");
				}
				html.replace("%objectId%", String.valueOf(getObjectId()));
				player.sendPacket(html);
			}
			else if (actualCommand.equalsIgnoreCase("manors_cert_confirm"))
			{
				final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				if (isMyLord(player) || (validatePrivileges(player, 5) && (validateCondition(player) == COND_OWNER)))
				{
					if (castle.getSiege().getIsInProgress())
					{
						html.setFile(player, player.getLang(), "data/html/chamberlain/chamberlain-busy.htm");
						html.replace("%npcname%", String.valueOf(getName(player.getLang())));
					}
					else
					{
						final int cabal = SevenSigns.getInstance().getPlayerCabal(player.getObjectId());
						if ((cabal == SevenSigns.CABAL_DAWN) && SevenSigns.getInstance().isCompetitionPeriod())
						{
							final int ticketCount = castle.getTicketBuyCount();
							if (ticketCount < (Config.SSQ_DAWN_TICKET_QUANTITY / Config.SSQ_DAWN_TICKET_BUNDLE))
							{
								final long totalCost = Config.SSQ_DAWN_TICKET_PRICE * Config.SSQ_DAWN_TICKET_BUNDLE;
								if (player.getAdena() >= totalCost)
								{
									player.reduceAdena(actualCommand, totalCost, this, true);
									player.addItem(actualCommand, Config.SSQ_MANORS_AGREEMENT_ID, Config.SSQ_DAWN_TICKET_BUNDLE, this, true);
									castle.setTicketBuyCount(ticketCount + 1);
									return;
								}
								html.setFile(player, player.getLang(), "data/html/chamberlain/chamberlain_noadena.htm");
							}
							else
							{
								html.setFile(player, player.getLang(), "data/html/chamberlain/ssq_notenoughticket.htm");
							}
						}
						else
						{
							html.setFile(player, player.getLang(), "data/html/chamberlain/ssq_notdawnorevent.htm");
						}
					}
				}
				else
				{
					html.setFile(player, player.getLang(), "data/html/chamberlain/chamberlain-noprivs.htm");
				}
				html.replace("%objectId%", String.valueOf(getObjectId()));
				player.sendPacket(html);
			}
			else
			{
				super.onBypassFeedback(player, command);
			}
		}
	}
	
	private void sendHtmlMessage(Player player, String htmlMessage)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setHtml(player, htmlMessage);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%npcname%", getName(player.getLang()));
		player.sendPacket(html);
	}
	
	@Override
	public void showChatWindow(Player player)
	{
		player.sendActionFailed();
		String filename = "data/html/chamberlain/chamberlain-no.htm";
		
		final int condition = validateCondition(player);
		if (condition > COND_ALL_FALSE)
		{
			if (condition == COND_BUSY_BECAUSE_OF_SIEGE)
			{
				filename = "data/html/chamberlain/chamberlain-busy.htm";
			}
			else if (condition == COND_OWNER)
			{
				filename = "data/html/chamberlain/chamberlain.htm";
			}
		}
		
		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(player, player.getLang(), filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%npcname%", getName(player.getLang()));
		player.sendPacket(html);
	}
	
	private void doTeleport(Player player, int val)
	{
		if (Config.DEBUG)
		{
			_log.warn("doTeleport(Player player, int val) is called");
		}
		final TeleportTemplate list = TeleLocationParser.getInstance().getTemplate(val);
		if (list != null)
		{
			if (player.destroyItemByItemId("Teleport", list.getId(), list.getPrice(), this, true))
			{
				if (Config.DEBUG)
				{
					_log.warn("Teleporting player " + player.getName(null) + " for Castle to new location: " + list.getLocation().toString());
				}
				player.teleToLocation(list.getLocation(), true, ReflectionManager.DEFAULT);
			}
		}
		else
		{
			_log.warn("No teleport destination with id:" + val);
		}
		player.sendActionFailed();
	}
	
	protected int validateCondition(Player player)
	{
		final var castle = getCastle();
		if ((castle != null) && (castle.getId() > 0))
		{
			if (player.getClan() != null)
			{
				if (castle.getZone().isActive())
				{
					return COND_BUSY_BECAUSE_OF_SIEGE;
				}
				else if (castle.getOwnerId() == player.getClanId())
				{
					return COND_OWNER;
				}
			}
		}
		return COND_ALL_FALSE;
	}
	
	private boolean validatePrivileges(Player player, int privilege)
	{
		if ((player.getClanPrivileges() & privilege) != privilege)
		{
			final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			html.setFile(player, player.getLang(), "data/html/chamberlain/chamberlain-noprivs.htm");
			player.sendPacket(html);
			return false;
		}
		return true;
	}
	
	private boolean siegeBlocksFunction(Player player)
	{
		final var castle = getCastle();
		if (castle != null && castle.getSiege().getIsInProgress())
		{
			final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			html.setFile(player, player.getLang(), "data/html/chamberlain/chamberlain-busy.htm");
			html.replace("%npcname%", String.valueOf(getName(player.getLang())));
			player.sendPacket(html);
			return true;
		}
		return false;
	}
}