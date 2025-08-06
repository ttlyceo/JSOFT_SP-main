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
package l2e.gameserver.instancemanager.mods;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import l2e.commons.dbutils.DbUtils;
import l2e.commons.log.LoggerObject;
import l2e.gameserver.Config;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.data.dao.TimeSkillsDAO;
import l2e.gameserver.data.holder.ClanHolder;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.DonateSkillTemplate;
import l2e.gameserver.model.actor.templates.TimeSkillTemplate;
import l2e.gameserver.model.holders.ItemHolder;

/**
 * Created by LordWinter
 */
public class TimeSkillsTaskManager extends LoggerObject
{
	private final Map<TimeSkillTemplate, Long> _skillList = new ConcurrentHashMap<>();
	private final List<DonateSkillTemplate> _donateSkillList = new ArrayList<>();
	private ScheduledFuture<?> _skillTask = null;
	private boolean _isRunTask = false;
	private boolean _isActive = false;
	
	public TimeSkillsTaskManager()
	{
		_skillList.clear();
		_donateSkillList.clear();
		templaterParser();
		
		if (_isActive)
		{
			TimeSkillsDAO.getInstance().deleteExpiredSkills();
			
			Connection con = null;
			PreparedStatement statement = null;
			ResultSet rset = null;
			try
			{
				con = DatabaseFactory.getInstance().getConnection();
				statement = con.prepareStatement("SELECT * FROM donate_time_skills ORDER BY objId");
				rset = statement.executeQuery();
				while (rset.next())
				{
					final long expireTime = rset.getLong("expire_time");
					if (expireTime > 0 && expireTime < System.currentTimeMillis())
					{
						continue;
					}
					final var tpl = new TimeSkillTemplate(rset.getInt("objId"), rset.getInt("skillId"), rset.getInt("skillLevel"), rset.getInt("clan_skill") == 1);
					tpl.setTime((int) ((expireTime - System.currentTimeMillis()) / 1000));
					_skillList.put(tpl, expireTime);
				}
			}
			catch (final Exception e)
			{
				warn("restore time skills", e);
				_isActive = false;
			}
			finally
			{
				DbUtils.closeQuietly(con, statement, rset);
			}
			
			if (_skillList.size() > 0 && !_isRunTask)
			{
				_isRunTask = true;
				if (recalcSpawnTime())
				{
					_isRunTask = false;
				}
			}
		}
	}
	
	private void templaterParser()
	{
		try
		{
			final File file = new File(Config.DATAPACK_ROOT + "/data/stats/services/donateTimeSkills.xml");
			if (!file.exists())
			{
				_isActive = false;
				return;
			}
			final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);
			final Document doc1 = factory.newDocumentBuilder().parse(file);
			
			for (Node n1 = doc1.getFirstChild(); n1 != null; n1 = n1.getNextSibling())
			{
				if ("list".equalsIgnoreCase(n1.getNodeName()))
				{
					for (Node d1 = n1.getFirstChild(); d1 != null; d1 = d1.getNextSibling())
					{
						if ("skill".equalsIgnoreCase(d1.getNodeName()))
						{
							final int id = Integer.parseInt(d1.getAttributes().getNamedItem("id").getNodeValue());
							final int level = Integer.parseInt(d1.getAttributes().getNamedItem("level").getNodeValue());
							final boolean clanSkill = Boolean.parseBoolean(d1.getAttributes().getNamedItem("isClanSkill").getNodeValue());
							
							final Map<Long, List<ItemHolder>> requestItems = new HashMap<>();
							final List<Long> times = new ArrayList<>();
							for (Node s1 = d1.getFirstChild(); s1 != null; s1 = s1.getNextSibling())
							{
								if ("requestItems".equalsIgnoreCase(s1.getNodeName()))
								{
									final long time = Long.parseLong(s1.getAttributes().getNamedItem("time").getNodeValue());
									times.add(time);
									
									final List<ItemHolder> items = new ArrayList<>();
									for (Node t1 = s1.getFirstChild(); t1 != null; t1 = t1.getNextSibling())
									{
										if ("item".equalsIgnoreCase(t1.getNodeName()))
										{
											final int itemId = Integer.parseInt(t1.getAttributes().getNamedItem("id").getNodeValue());
											final long itemAmount = Long.parseLong(t1.getAttributes().getNamedItem("amount").getNodeValue());
											items.add(new ItemHolder(itemId, itemAmount));
										}
									}
									requestItems.put(time, items);
								}
							}
							_donateSkillList.add(new DonateSkillTemplate(id, level, clanSkill, times, requestItems));
						}
					}
				}
			}
			info("Loaded " + _donateSkillList.size() + " donate time skill templates.");
			_isActive = true;
		}
		catch (NumberFormatException | DOMException | ParserConfigurationException | SAXException e)
		{
			warn("donateTimeSkills.xml could not be initialized.", e);
			_isActive = false;
		}
		catch (IOException | IllegalArgumentException e)
		{
			warn("IOException or IllegalArgumentException.", e);
			_isActive = false;
		}
	}
	
	public List<DonateSkillTemplate> getDonateSkills()
	{
		return _donateSkillList;
	}
	
	public DonateSkillTemplate getDonateSkill(int skillId, int skillLevel)
	{
		for (final var tpl : _donateSkillList)
		{
			if (tpl != null && tpl.getSkillId() == skillId && tpl.getSkillLevel() == skillLevel)
			{
				return tpl;
			}
		}
		return null;
	}
	
	public void checkPlayerSkills(Player player, boolean checkPersonalSkills, boolean checkClanSkills)
	{
		if (player == null || !_isActive)
		{
			return;
		}
		
		final List<TimeSkillTemplate> templates = getTemplates(player, checkPersonalSkills, checkClanSkills);
		if (!templates.isEmpty())
		{
			for (final var tpl : templates)
			{
				final var skill = SkillsParser.getInstance().getInfo(tpl.getSkillId(), tpl.getSkillLevel());
				if (skill != null)
				{
					player.addSkill(skill, false);
				}
			}
			player.sendSkillList(false);
		}
	}
	
	public List<TimeSkillTemplate> getTemplates(Player player, boolean checkPersonalSkills, boolean checkClanSkills)
	{
		final List<TimeSkillTemplate> templates = new ArrayList<>();
		if (player == null)
		{
			return templates;
		}
		
		for (final var tpl : _skillList.keySet())
		{
			if (tpl != null && ((tpl.getId() == player.getObjectId() && checkPersonalSkills) || (player.getClan() != null && player.getClan().getId() == tpl.getId() && tpl.isClanSkill() && checkClanSkills)))
			{
				templates.add(tpl);
			}
		}
		return templates;
	}
	
	private TimeSkillTemplate getTemplate(TimeSkillTemplate template)
	{
		for (final var tpl : _skillList.keySet())
		{
			if (tpl != null && tpl.getId() == template.getId() && tpl.getSkillId() == template.getSkillId())
			{
				return tpl;
			}
		}
		return null;
	}
	
	public void addTimeSkill(TimeSkillTemplate template, long newTime)
	{
		if (!_isActive)
		{
			return;
		}
		
		if (template != null)
		{
			final long time = System.currentTimeMillis() + newTime;
			final var skill = SkillsParser.getInstance().getInfo(template.getSkillId(), template.getSkillLevel());
			if (skill != null)
			{
				final var tpl = getTemplate(template);
				if (tpl != null)
				{
					_skillList.remove(tpl);
				}
				_skillList.put(template, time);
				
				template.setTime((int) ((time - System.currentTimeMillis()) / 1000));
				if (template.isClanSkill())
				{
					final var clan = ClanHolder.getInstance().getClan(template.getId());
					if (clan != null)
					{
						for (final var member : clan.getMembers())
						{
							if (member != null && member.getPlayerInstance() != null && member.getPlayerInstance().isOnline())
							{
								member.getPlayerInstance().addSkill(skill, false);
								member.getPlayerInstance().sendSkillList(false);
							}
						}
					}
				}
				else
				{
					final var player = GameObjectsStorage.getPlayer(template.getId());
					if (player != null && player.isOnline())
					{
						player.addSkill(skill, false);
						player.sendSkillList(false);
					}
				}
				
				TimeSkillsDAO.getInstance().insert(template, time);
				
				if (!_isRunTask)
				{
					_isRunTask = true;
					if (recalcSpawnTime())
					{
						_isRunTask = false;
					}
				}
			}
		}
	}
	
	public boolean recalcSpawnTime()
	{
		if (_skillTask != null)
		{
			_skillTask.cancel(false);
		}
		_skillTask = null;
		
		if (!_isActive)
		{
			return false;
		}
		
		if (_skillList.isEmpty())
		{
			return true;
		}
		
		final Map<TimeSkillTemplate, Long> sorted = _skillList.entrySet().stream().sorted(Map.Entry.comparingByValue()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
		if (!sorted.isEmpty())
		{
			long nextTime = sorted.entrySet().iterator().next().getValue() - System.currentTimeMillis();
			if (nextTime < 0)
			{
				nextTime = 0;
			}
			_skillTask = ThreadPoolManager.getInstance().schedule(new CheckTimeSkills(), nextTime);
			if (Config.DEBUG && nextTime > 0)
			{
				info("Next time skills task run at " + new Date(System.currentTimeMillis() + nextTime));
			}
		}
		return true;
	}
	
	private class CheckTimeSkills implements Runnable
	{
		@Override
		public void run()
		{
			final long time = System.currentTimeMillis();
			for (final Entry<TimeSkillTemplate, Long> entry : _skillList.entrySet())
			{
				final TimeSkillTemplate tpl = entry.getKey();
				if (time < entry.getValue())
				{
					continue;
				}
				
				if (tpl != null)
				{
					if (tpl.isClanSkill())
					{
						final var clan = ClanHolder.getInstance().getClan(tpl.getId());
						if (clan != null)
						{
							for (final var member : clan.getMembers())
							{
								if (member != null && member.getPlayerInstance() != null && member.getPlayerInstance().isOnline())
								{
									member.getPlayerInstance().removeSkill(tpl.getSkillId(), true);
									member.getPlayerInstance().sendSkillList(false);
								}
							}
						}
					}
					else
					{
						final var player = GameObjectsStorage.getPlayer(tpl.getId());
						if (player != null && player.isOnline())
						{
							player.removeSkill(tpl.getSkillId(), true);
							player.sendSkillList(false);
						}
					}
					TimeSkillsDAO.getInstance().delete(tpl.getId(), tpl.getSkillId(), tpl.getSkillLevel());
					_skillList.remove(tpl);
				}
			}
			checkTime();
		}
	}
	
	private void checkTime()
	{
		if (_isRunTask || !_isActive)
		{
			return;
		}
		_isRunTask = true;
		if (recalcSpawnTime())
		{
			_isRunTask = false;
		}
	}
	
	public boolean isActive()
	{
		return _isActive;
	}
	
	public static final TimeSkillsTaskManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final TimeSkillsTaskManager _instance = new TimeSkillsTaskManager();
	}
}