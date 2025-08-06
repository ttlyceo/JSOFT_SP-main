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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import l2e.gameserver.Config;
import l2e.gameserver.data.DocumentParser;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.base.SkillChangeType;
import l2e.gameserver.model.holders.SkillBalanceHolder;

public class SkillBalanceParser extends DocumentParser
{
	private final Map<String, SkillBalanceHolder> _skills = new ConcurrentHashMap<>();
	
	private int _balanceSize = 0;
	private int _olyBalanceSize = 0;
	private boolean _hasModify = false;
	
	public SkillBalanceParser()
	{
		load();
	}
	
	@Override
	public synchronized void load()
	{
		parseDatapackFile("data/stats/services/balancer/skillBalance.xml");
		info("Loaded " + _skills.size() + " balanced skill(s).");
	}
	
	@Override
	protected void reloadDocument()
	{
	}
	
	@Override
	protected void parseDocument()
	{
		_balanceSize = 0;
		_olyBalanceSize = 0;
		_hasModify = false;
		
		for (var o = getCurrentDocument().getFirstChild(); o != null; o = o.getNextSibling())
		{
			if (!"list".equalsIgnoreCase(o.getNodeName()))
			{
				continue;
			}
			
			for (var d = o.getFirstChild(); d != null; d = d.getNextSibling())
			{
				if (d.getNodeName().equals("balance"))
				{
					final int skillId = Integer.parseInt(d.getAttributes().getNamedItem("skillId").getNodeValue());
					final int target = Integer.parseInt(d.getAttributes().getNamedItem("target").getNodeValue());
					final var cbh = new SkillBalanceHolder(skillId, target);
					
					for (var set = d.getFirstChild(); set != null; set = set.getNextSibling())
					{
						if (set.getNodeName().equals("set"))
						{
							final double val = Double.parseDouble(set.getAttributes().getNamedItem("val").getNodeValue());
							final var atkType = SkillChangeType.valueOf(set.getAttributes().getNamedItem("type").getNodeValue());
							cbh.addSkillBalance(atkType, val);
							
							_balanceSize += 1;
						}
						else if (set.getNodeName().equals("olyset"))
						{
							final double val = Double.parseDouble(set.getAttributes().getNamedItem("val").getNodeValue());
							final var atkType = SkillChangeType.valueOf(set.getAttributes().getNamedItem("type").getNodeValue());
							cbh.addOlySkillBalance(atkType, val);
							
							_olyBalanceSize += 1;
						}
					}
					_skills.put(skillId + ";" + target, cbh);
				}
			}
		}
	}
	
	public void removeSkillBalance(String key, SkillChangeType type, boolean isOly)
	{
		if (!_hasModify)
		{
			_hasModify = true;
		}
		
		if (_skills.containsKey(key))
		{
			if (isOly)
			{
				_skills.get(key).removeOly(type);
				_olyBalanceSize -= 1;
				return;
			}
			_skills.get(key).remove(type);
			_balanceSize -= 1;
		}
	}
	
	public void addSkillBalance(String skill, SkillBalanceHolder sbh, boolean isEdit)
	{
		if (!_hasModify)
		{
			_hasModify = true;
		}
		_skills.put(skill, sbh);
		
		if (!isEdit)
		{
			if (!sbh.getOlyBalance().isEmpty())
			{
				_olyBalanceSize += 1;
			}
			else
			{
				_balanceSize += 1;
			}
		}
	}
	
	public Map<String, SkillBalanceHolder> getAllBalances()
	{
		final Map<String, SkillBalanceHolder> map = new TreeMap<>(new SkillComparator());
		map.putAll(_skills);
		return map;
	}
	
	public List<SkillBalanceHolder> getSkillBalances(int skillId)
	{
		final List<SkillBalanceHolder> list = new ArrayList<>();
		for (final Map.Entry<String, SkillBalanceHolder> data : _skills.entrySet())
		{
			if (Integer.valueOf(data.getKey().split(";")[0]).intValue() == skillId)
			{
				list.add(data.getValue());
			}
		}
		return list;
	}
	
	public int getSkillBalanceSize(int skillId, boolean olysize)
	{
		int size = 0;
		for (final var data : getSkillBalances(skillId))
		{
			size += (!olysize ? data.getNormalBalance().size() : data.getOlyBalance().size());
		}
		return size;
	}
	
	public double getSkillValue(String sk, SkillChangeType sct, Creature victim)
	{
		if (Config.BALANCER_ALLOW)
		{
			if (_skills.containsKey(sk) || _skills.containsKey(sk.split(";")[0] + ";-2"))
			{
				if (!sk.split(";")[1].equals("-2") && !_skills.containsKey(sk))
				{
					sk = sk.split(";")[0] + ";-2";
				}
				
				if (victim != null || sct.isForceCheck())
				{
					if (victim != null && victim.isPlayer())
					{
						if (victim.getActingPlayer().isOlympiadStart() && victim.getActingPlayer().getOlympiadGameId() != -1)
						{
							if (_skills.containsKey(sk))
							{
								return _skills.get(sk).getOlyBalanceValue(sct);
							}
						}
					}
					return _skills.get(sk).getValue(sct);
				}
			}
		}
		return 1.0D;
	}
	
	public int getSize(boolean olysize)
	{
		return olysize ? _olyBalanceSize : _balanceSize;
	}
	
	public SkillBalanceHolder getSkillHolder(String key)
	{
		return _skills.get(key);
	}
	
	public void store(Player player)
	{
		if (!_hasModify)
		{
			if (player != null)
			{
				player.sendMessage("SkillBalance: Nothing for saving!");
			}
			return;
		}
		
		try
		{
			var file = new File(Config.DATAPACK_ROOT, "data/stats/services/balancer/skillBalance.xml");
			if (file.exists())
			{
				if (!file.renameTo(new File(Config.DATAPACK_ROOT, "data/stats/services/balancer/skillBalance_Backup_[" + new SimpleDateFormat("YYYY-MM-dd_HH-mm-ss").format(Calendar.getInstance().getTimeInMillis()) + "].xml")))
				{
					if (player != null)
					{
						player.sendMessage("SkillBalance: can't save backup file!");
					}
				}
			}
			
			file = new File(Config.DATAPACK_ROOT, "data/stats/services/balancer/skillBalance.xml");
			file.createNewFile();
			
			final var fstream = new FileWriter(file);
			final var out = new BufferedWriter(fstream);
			
			out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
			out.write("<list xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"../../../xsd/skillBalance.xsd\">\n");
			
			for (final SkillBalanceHolder cbh : _skills.values())
			{
				if ((!cbh.getNormalBalance().isEmpty()) || (!cbh.getOlyBalance().isEmpty()))
				{
					String xml = "	<balance skillId=\"" + cbh.getSkillId() + "\" target=\"" + cbh.getTarget() + "\">\n";
					
					for (final Map.Entry<SkillChangeType, Double> info : cbh.getNormalBalance().entrySet())
					{
						xml += "		<set type=\"" + info.getKey().toString() + "\" val=\"" + info.getValue() + "\"/>\n";
					}
					
					for (final Map.Entry<SkillChangeType, Double> info : cbh.getOlyBalance().entrySet())
					{
						xml += "		<olyset type=\"" + info.getKey().toString() + "\" val=\"" + info.getValue() + "\"/>\n";
					}
					
					xml += "	</balance>\n";
					
					out.write(xml);
				}
			}
			out.write("</list>");
			out.close();
		}
		catch (final Exception e)
		{
			e.printStackTrace();
		}
		
		if (player != null)
		{
			player.sendMessage("SkillBalance: Modified data was saved!");
		}
		_hasModify = false;
	}
	
	private class SkillComparator implements Comparator<String>
	{
		public SkillComparator()
		{
		}
		
		@Override
		public int compare(String l, String r)
		{
			final int left = Integer.valueOf(l.split(";")[0]).intValue();
			final int right = Integer.valueOf(r.split(";")[0]).intValue();
			
			if (left > right)
			{
				return 1;
			}
			
			if (left < right)
			{
				return -1;
			}
			
			if (Integer.valueOf(l.split(";")[1]).intValue() > Integer.valueOf(r.split(";")[1]).intValue())
			{
				return 1;
			}
			
			if (Integer.valueOf(r.split(";")[1]).intValue() > Integer.valueOf(l.split(";")[1]).intValue())
			{
				return -1;
			}
			
			final Random x = new Random();
			return x.nextInt(2) == 1 ? 1 : 1;
		}
	}
	
	public static final SkillBalanceParser getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final SkillBalanceParser _instance = new SkillBalanceParser();
	}
}