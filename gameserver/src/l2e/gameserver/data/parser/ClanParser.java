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
package l2e.gameserver.data.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Node;

import l2e.commons.util.Rnd;
import l2e.gameserver.data.DocumentParser;
import l2e.gameserver.instancemanager.TerritoryWarManager;
import l2e.gameserver.instancemanager.TerritoryWarManager.Territory;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.player.ClanTemplate;
import l2e.gameserver.model.holders.ItemHolder;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.strings.server.ServerMessage;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.ExPCCafePointInfo;
import l2e.gameserver.network.serverpackets.StatusUpdate;
import l2e.gameserver.network.serverpackets.SystemMessage;

public final class ClanParser extends DocumentParser
{
	private final Map<Integer, ClanTemplate> _templates = new HashMap<>();
	private final Map<Integer, Skill> _skills = new HashMap<>();
	
	protected ClanParser()
	{
		load();
	}
	
	public void reload()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_templates.clear();
		_skills.clear();
		parseDatapackFile("data/stats/services/clanTemplates.xml");
		info("Loaded " + _templates.size() + " clan templates.");
	}
	
	@Override
	protected void reloadDocument()
	{
	}
	
	@Override
	protected void parseDocument()
	{
		for (Node n = getCurrentDocument().getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equals(n.getNodeName()))
			{
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("level".equals(d.getNodeName()))
					{
						final int level = Integer.parseInt(d.getAttributes().getNamedItem("val").getNodeValue());
						final int sp = d.getAttributes().getNamedItem("sp") != null ? Integer.parseInt(d.getAttributes().getNamedItem("sp").getNodeValue()) : -1;
						final int members = d.getAttributes().getNamedItem("members") != null ? Integer.parseInt(d.getAttributes().getNamedItem("members").getNodeValue()) : -1;
						final int membersLimit = d.getAttributes().getNamedItem("mainPledgeLimit") != null ? Integer.parseInt(d.getAttributes().getNamedItem("mainPledgeLimit").getNodeValue()) : 0;
						final int academyLimit = d.getAttributes().getNamedItem("academyLimit") != null ? Integer.parseInt(d.getAttributes().getNamedItem("academyLimit").getNodeValue()) : 0;
						final int royalLimit = d.getAttributes().getNamedItem("royalsLimit") != null ? Integer.parseInt(d.getAttributes().getNamedItem("royalsLimit").getNodeValue()) : 0;
						final int knightLimit = d.getAttributes().getNamedItem("knightsLimit") != null ? Integer.parseInt(d.getAttributes().getNamedItem("knightsLimit").getNodeValue()) : 0;
						final boolean haveTerritory = d.getAttributes().getNamedItem("haveTerritory") != null ? Boolean.parseBoolean(d.getAttributes().getNamedItem("haveTerritory").getNodeValue()) : false;
						final List<ItemHolder> requestItems = new ArrayList<>();
						final List<ItemHolder> rewardItems = new ArrayList<>();
						final List<Skill> rewardSkills = new ArrayList<>();
						for (Node c = d.getFirstChild(); c != null; c = c.getNextSibling())
						{
							if ("requestItems".equals(c.getNodeName()))
							{
								for (Node i = c.getFirstChild(); i != null; i = i.getNextSibling())
								{
									if ("item".equals(i.getNodeName()))
									{
										final int id = Integer.parseInt(i.getAttributes().getNamedItem("id").getNodeValue());
										final long count = Long.parseLong(i.getAttributes().getNamedItem("count").getNodeValue());
										requestItems.add(new ItemHolder(id, count));
									}
								}
							}
							else if ("rewardItems".equals(c.getNodeName()))
							{
								for (Node i = c.getFirstChild(); i != null; i = i.getNextSibling())
								{
									if ("item".equals(i.getNodeName()))
									{
										final int id = Integer.parseInt(i.getAttributes().getNamedItem("id").getNodeValue());
										final long count = Long.parseLong(i.getAttributes().getNamedItem("count").getNodeValue());
										final double chance = i.getAttributes().getNamedItem("chance") != null ? Double.parseDouble(i.getAttributes().getNamedItem("chance").getNodeValue()) : 100;
										rewardItems.add(new ItemHolder(id, count, chance));
									}
								}
							}
							else if ("rewardSkills".equals(c.getNodeName()))
							{
								for (Node i = c.getFirstChild(); i != null; i = i.getNextSibling())
								{
									if ("skill".equals(i.getNodeName()))
									{
										final int id = Integer.parseInt(i.getAttributes().getNamedItem("id").getNodeValue());
										final int skLvl = Integer.parseInt(i.getAttributes().getNamedItem("level").getNodeValue());
										final var skill = SkillsParser.getInstance().getInfo(id, skLvl);
										if (skill != null)
										{
											rewardSkills.add(skill);
											_skills.put(id, skill);
										}
									}
								}
							}
						}
						_templates.put(level, new ClanTemplate(level, sp, requestItems, rewardItems, rewardSkills, members, membersLimit, academyLimit, royalLimit, knightLimit, haveTerritory));
					}
				}
			}
		}
	}
	
	public ClanTemplate getClanTemplate(int level)
	{
		if (_templates.containsKey(level))
		{
			return _templates.get(level);
		}
		return null;
	}
	
	public int getLevelRequirement(int level)
	{
		if (_templates.containsKey(level))
		{
			final var tpl = _templates.get(level);
			if (tpl != null)
			{
				return tpl.getSp();
			}
		}
		return 0;
	}
	
	public boolean hasClanLevel(int level)
	{
		return _templates.containsKey(level);
	}
	
	public boolean canIncreaseClanLevel(Player player, int newLevel)
	{
		final var clan = player.getClan();
		if (clan == null || !player.isClanLeader())
		{
			player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
			return false;
		}
		if (System.currentTimeMillis() < clan.getDissolvingExpiryTime())
		{
			player.sendPacket(SystemMessageId.CANNOT_RISE_LEVEL_WHILE_DISSOLUTION_IN_PROGRESS);
			return false;
		}
		
		final var template = getClanTemplate(newLevel);
		if (template == null)
		{
			return false;
		}
		
		if (template.getSp() > 0 && player.getSp() < template.getSp())
		{
			return false;
		}
		
		if (!template.getRequestItems().isEmpty())
		{
			for (final var item : template.getRequestItems())
			{
				if (item != null)
				{
					if (item.getId() == -300)
					{
						if (player.getFame() < item.getCount())
						{
							return false;
						}
					}
					else if (item.getId() == -200)
					{
						if (clan.getReputationScore() < item.getCount())
						{
							return false;
						}
					}
					else if (item.getId() == -100)
					{
						if (player.getPcBangPoints() < item.getCount())
						{
							return false;
						}
					}
					else
					{
						if (player.getInventory().getItemByItemId(item.getId()) == null || player.getInventory().getItemByItemId(item.getId()).getCount() < item.getCount())
						{
							return false;
						}
					}
				}
			}
		}
		
		if (template.getRequestMembers() > 0 && clan.getMembersCount() < template.getRequestMembers())
		{
			return false;
		}
		
		if (template.hasTerritory())
		{
			var found = false;
			for (final Territory terr : TerritoryWarManager.getInstance().getAllTerritories())
			{
				if (terr.getLordObjectId() == clan.getLeaderId())
				{
					found = true;
					break;
				}
			}
			
			if (!found)
			{
				return false;
			}
		}
		return true;
	}
	
	public boolean isIncreaseClanLevel(Player player, int newLevel)
	{
		final var clan = player.getClan();
		if (clan == null || !player.isClanLeader())
		{
			player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
			return false;
		}
		if (System.currentTimeMillis() < clan.getDissolvingExpiryTime())
		{
			player.sendPacket(SystemMessageId.CANNOT_RISE_LEVEL_WHILE_DISSOLUTION_IN_PROGRESS);
			return false;
		}
		
		final var template = getClanTemplate(newLevel);
		if (template == null)
		{
			return false;
		}
		
		final var sp = template.getSp();
		if (sp > 0)
		{
			player.setSp(player.getSp() - sp);
			SystemMessage msg = SystemMessage.getSystemMessage(SystemMessageId.SP_DECREASED_S1);
			msg.addNumber(sp);
			player.sendPacket(msg);
			msg = null;
		}
		
		if (!template.getRequestItems().isEmpty())
		{
			for (final var item : template.getRequestItems())
			{
				if (item != null)
				{
					if (item.getId() == -300)
					{
						player.setFame((int) (player.getFame() - item.getCount()));
						player.sendUserInfo();
					}
					else if (item.getId() == -200)
					{
						clan.takeReputationScore((int) item.getCount(), true);
						SystemMessage msg = SystemMessage.getSystemMessage(SystemMessageId.S1_DEDUCTED_FROM_CLAN_REP);
						msg.addItemNumber(item.getCount());
						player.sendPacket(msg);
						msg = null;
					}
					else if (item.getId() == -100)
					{
						player.setPcBangPoints((int) (player.getPcBangPoints() - item.getCount()));
						SystemMessage msg = SystemMessage.getSystemMessage(SystemMessageId.USING_S1_PCPOINT);
						msg.addNumber((int) item.getCount());
						player.sendPacket(msg);
						player.sendPacket(new ExPCCafePointInfo(player.getPcBangPoints(), (int) item.getCount(), false, false, 1));
						msg = null;
					}
					else
					{
						if (!player.destroyItemByItemId("ClanLvl", item.getId(), item.getCount(), player.getTarget(), false))
						{
							return false;
						}
					}
				}
			}
		}
		
		if (sp > 0)
		{
			player.sendStatusUpdate(false, false, StatusUpdate.SP);
		}
		
		if (!template.getRewardItems().isEmpty())
		{
			for (final var item : template.getRewardItems())
			{
				if (item != null)
				{
					if (item.getChance() < 100 && !Rnd.chance(item.getChance()))
					{
						continue;
					}
					
					if (item.getId() == -300)
					{
						player.setFame((int) (player.getFame() + item.getCount()));
						final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.ACQUIRED_S1_REPUTATION_SCORE);
						sm.addNumber((int) item.getCount());
						player.sendPacket(sm);
						player.sendUserInfo();
					}
					else if (item.getId() == -200)
					{
						player.getClan().addReputationScore((int) item.getCount(), true);
						final var msg = new ServerMessage("ServiceBBS.ADD_REP", player.getLang());
						msg.add(String.valueOf((int) item.getCount()));
						player.sendMessage(msg.toString());
					}
					else if (item.getId() == -100)
					{
						player.setPcBangPoints((int) (player.getPcBangPoints() + item.getCount()));
						final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.YOU_HAVE_ACQUIRED_S1_PC_CAFE_POINTS);
						sm.addNumber((int) item.getCount());
						player.sendPacket(sm);
						player.sendPacket(new ExPCCafePointInfo(player.getPcBangPoints(), (int) item.getCount(), false, false, 1));
					}
					else
					{
						player.addItem("Clan Level", item.getId(), item.getCount(), player, true);
					}
				}
			}
		}
		
		if (!template.getRewardSkills().isEmpty())
		{
			for (final var skill : template.getRewardSkills())
			{
				if (skill != null)
				{
					clan.addNewSkill(skill);
				}
			}
		}
		return true;
	}
	
	public boolean isClanSkill(int id)
	{
		return _skills.containsKey(id);
	}
	
	public int getMaxLevel()
	{
		var lvl = 0;
		for (final var level : _templates.keySet())
		{
			if (level > lvl)
			{
				lvl = level;
			}
		}
		return lvl;
	}
	
	public static ClanParser getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final ClanParser _instance = new ClanParser();
	}
}