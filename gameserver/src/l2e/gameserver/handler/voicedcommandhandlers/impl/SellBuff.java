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
package l2e.gameserver.handler.voicedcommandhandlers.impl;

import java.util.StringTokenizer;

import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.data.parser.ItemsParser;
import l2e.gameserver.data.parser.SellBuffsParser;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.handler.bypasshandlers.IBypassHandler;
import l2e.gameserver.handler.voicedcommandhandlers.IVoicedCommandHandler;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.items.Item;
import l2e.gameserver.model.entity.mods.SellBuffsManager;
import l2e.gameserver.model.holders.SellBuffHolder;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.skills.effects.Effect;
import l2e.gameserver.model.skills.effects.EffectTemplate;
import l2e.gameserver.model.stats.Env;
import l2e.gameserver.model.strings.server.ServerMessage;
import l2e.gameserver.model.zone.ZoneId;
import l2e.gameserver.network.serverpackets.MagicSkillUse;

public class SellBuff implements IVoicedCommandHandler, IBypassHandler
{
	private static final String[] VOICED_COMMANDS =
	{
	        "sellbuff", "sellbuffs",
	};

	private static final String[] BYPASS_COMMANDS =
	{
	        "sellbuffadd", "sellbuffaddskill", "sellbuffedit", "sellbuffchangeprice", "sellbuffremove", "sellbuffbuymenu", "sellbuffbuyskill", "sellbuffstart", "sellbuffstop",
	};
	
	@Override
	public boolean useBypass(String command, Player activeChar, Creature target)
	{
		if (!Config.ALLOW_SELLBUFFS_COMMAND || activeChar.isInsideZone(ZoneId.SIEGE))
		{
			return false;
		}
		
		if (Config.ALLOW_SELLBUFFS_IN_PEACE && !activeChar.isInZonePeace())
		{
			activeChar.sendMessage((new ServerMessage("SellBuff.ONLY_IN_PEACE", activeChar.getLang())).toString());
			return false;
		}
		
		String cmd = "";
		String params = "";
		final StringTokenizer st = new StringTokenizer(command, " ");

		if (st.hasMoreTokens())
		{
			cmd = st.nextToken();
		}

		while (st.hasMoreTokens())
		{
			params += st.nextToken() + (st.hasMoreTokens() ? " " : "");
		}

		if (cmd.isEmpty())
		{
			return false;
		}
		return useBypass(cmd, activeChar, params);
	}

	@Override
	public boolean useVoicedCommand(String command, Player activeChar, String params)
	{
		if (!Config.ALLOW_SELLBUFFS_COMMAND)
		{
			return false;
		}
		
		switch (command)
		{
			case "sellbuff" :
			case "sellbuffs" :
			{
				SellBuffsManager.sendSellMenu(activeChar);
				break;
			}
		}
		return true;
	}

	public boolean useBypass(String command, Player activeChar, String params)
	{
		switch (command)
		{
			case "sellbuffstart" :
			{
				if (activeChar.isSellingBuffs() || (params == null) || params.isEmpty())
				{
					return false;
				}
				else if (activeChar.getSellingBuffs().isEmpty())
				{
					activeChar.sendMessage((new ServerMessage("SellBuff.EMPTY_LIST", activeChar.getLang())).toString());
					return false;
				}
				else if (!activeChar.canOpenPrivateStore(false, true))
				{
					return false;
				}
				else
				{
					String title = "BUFF SELL: ";
					final StringTokenizer st = new StringTokenizer(params, " ");
					while (st.hasMoreTokens())
					{
						title += st.nextToken() + " ";
					}

					if (title.length() > 40)
					{
						activeChar.sendMessage((new ServerMessage("SellBuff.WRONG_TITLE", activeChar.getLang())).toString());
						return false;
					}

					SellBuffsManager.startSellBuffs(activeChar, title);
				}
				break;
			}
			case "sellbuffstop" :
			{
				if (activeChar.isSellingBuffs())
				{
					SellBuffsManager.stopSellBuffs(activeChar);
				}
				break;
			}
			case "sellbuffadd" :
			{
				if (!activeChar.isSellingBuffs())
				{
					int page = 1;
					if ((params != null) && !params.isEmpty() && Util.isDigit(params))
					{
						page = Integer.parseInt(params);
					}

					SellBuffsManager.sendBuffChoiceMenu(activeChar, page);
				}
				break;
			}
			case "sellbuffedit" :
			{
				if (!activeChar.isSellingBuffs())
				{
					final StringTokenizer st = new StringTokenizer(params, " ");
					int page = 1;
					if (st.hasMoreTokens())
					{
						page = Integer.parseInt(st.nextToken());
					}
					SellBuffsManager.sendBuffEditMenu(activeChar, page);
				}
				break;
			}
			case "sellbuffchangeprice" :
			{
				if (!activeChar.isSellingBuffs() && (params != null) && !params.isEmpty())
				{
					final StringTokenizer st = new StringTokenizer(params, " ");

					int skillId = -1;
					int price = -1;
					String currency = null;
					int page = 1;
					
					if (st.hasMoreTokens())
					{
						skillId = Integer.parseInt(st.nextToken());
					}

					if (st.hasMoreTokens())
					{
						try
						{
							price = Integer.parseInt(st.nextToken());
						}
						catch (final NumberFormatException e)
						{
							final ServerMessage msg = new ServerMessage("SellBuff.BIG_PRICE", activeChar.getLang());
							msg.add(Config.SELLBUFF_MAX_PRICE);
							activeChar.sendMessage(msg.toString());
							SellBuffsManager.sendBuffEditMenu(activeChar, page);
						}
					}
					
					if (st.hasMoreTokens())
					{
						try
						{
							currency = st.nextToken();
						}
						catch (final Exception e)
						{
							SellBuffsManager.sendBuffEditMenu(activeChar, page);
						}
					}
					
					if (st.hasMoreTokens())
					{
						page = Integer.parseInt(st.nextToken());
					}

					if ((skillId == -1) || (price == -1))
					{
						return false;
					}

					final Skill skillToChange = activeChar.getKnownSkill(skillId);
					if (skillToChange == null || currency == null)
					{
						return false;
					}

					for (final SellBuffHolder holder : activeChar.getSellingBuffs())
					{
						if (holder != null && holder.getId() == skillToChange.getId())
						{
							int itemId = 0;
							for (final String itemName : Config.SELLBUFF_CURRECY_LIST.keySet())
							{
								if (itemName.equals(currency))
								{
									itemId = Config.SELLBUFF_CURRECY_LIST.get(itemName);
									break;
								}
							}
							
							if (itemId == 0)
							{
								activeChar.sendMessage((new ServerMessage("SellBuff.WRONG_CURRENCY", activeChar.getLang())).toString());
								SellBuffsManager.sendBuffEditMenu(activeChar, page);
								return false;
							}
							final ServerMessage msg = new ServerMessage("SellBuff.CHANGE_PRICE", activeChar.getLang());
							msg.add(activeChar.getKnownSkill(holder.getId()).getName(activeChar.getLang()));
							msg.add(price);
							activeChar.sendMessage(msg.toString());
							holder.setItemId(itemId);
							holder.setPrice(price);
							SellBuffsManager.sendBuffEditMenu(activeChar, page);
						}
					}
				}
				break;
			}
			case "sellbuffremove" :
			{
				if (!activeChar.isSellingBuffs() && (params != null) && !params.isEmpty())
				{
					final StringTokenizer st = new StringTokenizer(params, " ");
					int page = 1;
					int skillId = -1;
					
					if (st.hasMoreTokens())
					{
						try
						{
							skillId = Integer.parseInt(st.nextToken());
						}
						catch (final NumberFormatException e)
						{
							activeChar.sendMessage((new ServerMessage("SellBuff.WRONG_SKILL", activeChar.getLang())).toString());
							SellBuffsManager.sendBuffEditMenu(activeChar, page);
						}
					}
					
					if (st.hasMoreTokens())
					{
						page = Integer.parseInt(st.nextToken());
					}

					if ((skillId == -1))
					{
						return false;
					}

					final Skill skillToRemove = activeChar.getKnownSkill(skillId);
					if (skillToRemove == null)
					{
						return false;
					}

					SellBuffHolder correctSkill = null;
					for (final SellBuffHolder holder : activeChar.getSellingBuffs())
					{
						if ((holder != null) && (holder.getId() == skillToRemove.getId()))
						{
							correctSkill = holder;
						}
					}

					if (correctSkill != null)
					{
						activeChar.getSellingBuffs().remove(correctSkill);
						final ServerMessage msg = new ServerMessage("SellBuff.SKILL_REMOVED", activeChar.getLang());
						msg.add(activeChar.getKnownSkill(correctSkill.getId()).getName(activeChar.getLang()));
						activeChar.sendMessage(msg.toString());
						SellBuffsManager.sendBuffEditMenu(activeChar, page);
					}
				}
				break;
			}
			case "sellbuffaddskill" :
			{
				if (!activeChar.isSellingBuffs() && (params != null) && !params.isEmpty())
				{
					final StringTokenizer st = new StringTokenizer(params, " ");

					int skillId = -1;
					long price = -1;
					String currency = null;
					int page = 1;

					if (st.hasMoreTokens())
					{
						skillId = Integer.parseInt(st.nextToken());
					}

					if (st.hasMoreTokens())
					{
						try
						{
							price = Integer.parseInt(st.nextToken());
						}
						catch (final NumberFormatException e)
						{
							final ServerMessage msg = new ServerMessage("SellBuff.BIG_PRICE", activeChar.getLang());
							msg.add(Config.SELLBUFF_MAX_PRICE);
							activeChar.sendMessage(msg.toString());
							SellBuffsManager.sendBuffChoiceMenu(activeChar, page);
						}
					}
					
					if (st.hasMoreTokens())
					{
						try
						{
							currency = st.nextToken();
						}
						catch (final Exception e)
						{
							SellBuffsManager.sendBuffChoiceMenu(activeChar, page);
						}
					}
					
					if (st.hasMoreTokens())
					{
						page = Integer.parseInt(st.nextToken());
					}

					if ((skillId == -1) || (price == -1))
					{
						return false;
					}

					final var skillToAdd = activeChar.getKnownSkill(skillId);
					if (skillToAdd == null || currency == null)
					{
						return false;
					}
					
					final var tpl = SellBuffsParser.getInstance().getSellBuff(skillId);
					if (tpl == null)
					{
						return false;
					}
					
					if (price < Config.SELLBUFF_MIN_PRICE)
					{
						final ServerMessage msg = new ServerMessage("SellBuff.SMALL_PRICE", activeChar.getLang());
						msg.add(Config.SELLBUFF_MIN_PRICE);
						activeChar.sendMessage(msg.toString());
						return false;
					}
					
					if (price > Config.SELLBUFF_MAX_PRICE)
					{
						final ServerMessage msg = new ServerMessage("SellBuff.BIG_PRICE", activeChar.getLang());
						msg.add(Config.SELLBUFF_MAX_PRICE);
						activeChar.sendMessage(msg.toString());
						return false;
					}
					
					if (activeChar.getSellingBuffs().size() >= Config.SELLBUFF_MAX_BUFFS)
					{
						final ServerMessage msg = new ServerMessage("SellBuff.MAX_BUFFS", activeChar.getLang());
						msg.add(Config.SELLBUFF_MAX_BUFFS);
						activeChar.sendMessage(msg.toString());
						return false;
					}
					
					if (!SellBuffsManager.isInSellList(activeChar, skillToAdd))
					{
						int itemId = 0;
						for (final String itemName : Config.SELLBUFF_CURRECY_LIST.keySet())
						{
							if (itemName.equals(currency))
							{
								itemId = Config.SELLBUFF_CURRECY_LIST.get(itemName);
								break;
							}
						}
						
						if (itemId == 0)
						{
							activeChar.sendMessage((new ServerMessage("SellBuff.WRONG_CURRENCY", activeChar.getLang())).toString());
							SellBuffsManager.sendBuffChoiceMenu(activeChar, page);
							return false;
						}
						activeChar.getSellingBuffs().add(new SellBuffHolder(skillToAdd.getId(), skillToAdd.getLevel(), itemId, price));
						final ServerMessage msg = new ServerMessage("SellBuff.SKILL_ADDED", activeChar.getLang());
						msg.add(skillToAdd.getName(activeChar.getLang()));
						activeChar.sendMessage(msg.toString());
						SellBuffsManager.sendBuffChoiceMenu(activeChar, page);
					}
				}
				break;
			}
			case "sellbuffbuymenu" :
			{
				if ((params != null) && !params.isEmpty())
				{
					final StringTokenizer st = new StringTokenizer(params, " ");

					int objId = -1;
					int page = 1;
					if (st.hasMoreTokens())
					{
						objId = Integer.parseInt(st.nextToken());
					}

					if (st.hasMoreTokens())
					{
						page = Integer.parseInt(st.nextToken());
					}

					final Player seller = GameObjectsStorage.getPlayer(objId);
					if (seller != null)
					{
						if (!seller.isSellingBuffs() || !activeChar.isInsideRadius(seller, Npc.INTERACTION_DISTANCE, true, true))
						{
							return false;
						}
						SellBuffsManager.sendBuffMenu(activeChar, seller, page);
					}
				}
				break;
			}
			case "sellbuffbuyskill" :
			{
				if ((params != null) && !params.isEmpty())
				{
					final StringTokenizer st = new StringTokenizer(params, " ");
					int objId = -1;
					int skillId = -1;
					int page = 1;

					if (st.hasMoreTokens())
					{
						objId = Integer.parseInt(st.nextToken());
					}

					if (st.hasMoreTokens())
					{
						skillId = Integer.parseInt(st.nextToken());
					}

					if (st.hasMoreTokens())
					{
						page = Integer.parseInt(st.nextToken());
					}

					if ((skillId == -1) || (objId == -1))
					{
						return false;
					}

					final Player seller = GameObjectsStorage.getPlayer(objId);
					if (seller == null)
					{
						return false;
					}

					final Skill skillToBuy = seller.getKnownSkill(skillId);
					if (!seller.isSellingBuffs() || !Util.checkIfInRange(Npc.INTERACTION_DISTANCE, activeChar, seller, true) || (skillToBuy == null))
					{
						return false;
					}
					
					if (!activeChar.checkFloodProtection("SINGLEBUFF", "singlebuff_delay"))
					{
						return false;
					}

					if (Config.SELLBUFF_USED_MP && seller.getCurrentMp() < (skillToBuy.getMpConsume()))
					{
						final ServerMessage msg = new ServerMessage("SellBuff.HAVENO_MP", activeChar.getLang());
						msg.add(seller.getName(null));
						msg.add(skillToBuy.getName(activeChar.getLang()));
						activeChar.sendMessage(msg.toString());
						SellBuffsManager.sendBuffMenu(activeChar, seller, page);
						return false;
					}
					
					final var isFreeBuff = Config.FREE_SELLBUFF_FOR_SAME_CLAN && seller.isInSameClan(activeChar);

					for (final SellBuffHolder holder : seller.getSellingBuffs())
					{
						if (holder != null && holder.getId() == skillToBuy.getId())
						{
							if (!isFreeBuff)
							{
								final Item item = ItemsParser.getInstance().getTemplate(holder.getItemId());
								if (activeChar.getInventory().getItemByItemId(holder.getItemId()) == null || activeChar.getInventory().getItemByItemId(holder.getItemId()).getCount() < holder.getPrice())
								{
									final ServerMessage msg = new ServerMessage("SellBuff.NO_ITEM", activeChar.getLang());
									msg.add(item.getName(activeChar.getLang()));
									activeChar.sendMessage(msg.toString());
									return false;
								}
								
								activeChar.destroyItemByItemId("SellBuff", holder.getItemId(), holder.getPrice(), activeChar, true);
								seller.getInventory().addItem("SellBuff", holder.getItemId(), holder.getPrice(), seller, true);
							}
							
							final Skill s = SkillsParser.getInstance().getInfo(holder.getId(), holder.getLvl());
							if (s != null)
							{
								final int buffTime = SellBuffsParser.getInstance().getBuffTime(seller, activeChar, s.getId());
								if (buffTime > 0 && s.hasEffects())
								{
									final Env env = new Env();
									env.setCharacter(seller);
									env.setTarget(activeChar);
									env.setSkill(s);
									
									Effect ef;
									for (final EffectTemplate et : s.getEffectTemplates())
									{
										ef = et.getEffect(env);
										if (ef != null)
										{
											ef.setAbnormalTime(buffTime * 60);
											ef.scheduleEffect(true);
										}
									}
									
									if (activeChar.hasSummon() && Config.ALLOW_SELLBUFFS_PETS)
									{
										final Env en = new Env();
										en.setCharacter(seller);
										en.setTarget(activeChar.getSummon());
										en.setSkill(s);
										
										Effect eff;
										for (final EffectTemplate et : s.getEffectTemplates())
										{
											eff = et.getEffect(en);
											if (eff != null)
											{
												eff.setAbnormalTime(buffTime * 60);
												eff.scheduleEffect(true);
											}
										}
									}
								}
								else
								{
									s.getEffects(seller, activeChar, false);
									if (activeChar.hasSummon() && Config.ALLOW_SELLBUFFS_PETS)
									{
										s.getEffects(seller, activeChar.getSummon(), false);
									}
								}
								activeChar.broadcastPacket(new MagicSkillUse(activeChar, activeChar, s.getId(), s.getLevel(), 2, 0));
								
								if (Config.SELLBUFF_USED_MP)
								{
									seller.reduceCurrentMp(skillToBuy.getMpConsume());
								}
							}
						}
					}
					SellBuffsManager.sendBuffMenu(activeChar, seller, page);
				}
				break;
			}
		}
		return true;
	}

	@Override
	public String[] getVoicedCommandList()
	{
		return VOICED_COMMANDS;
	}

	@Override
	public String[] getBypassList()
	{
		return BYPASS_COMMANDS;
	}
}