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
import l2e.gameserver.model.base.AttackType;
import l2e.gameserver.model.holders.ClassBalanceHolder;

public class ClassBalanceParser extends DocumentParser
{
	private final Map<String, ClassBalanceHolder> _classes = new ConcurrentHashMap<>();
	
	private int _balanceSize;
	private int _olyBalanceSize;
	private boolean _hasModify = false;
	
	public ClassBalanceParser()
	{
		load();
	}
	
	@Override
	public synchronized void load()
	{
		_classes.clear();
		_balanceSize = 0;
		_olyBalanceSize = 0;
		parseDatapackFile("data/stats/services/balancer/classBalance.xml");
		info("Loaded " + _classes.size() + " balanced classe(s).");
	}
	
	@Override
	protected void reloadDocument()
	{
	}
	
	@Override
	protected void parseDocument()
	{
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
					final int classId = Integer.parseInt(d.getAttributes().getNamedItem("classId").getNodeValue());
					final int targetClassId = Integer.parseInt(d.getAttributes().getNamedItem("targetClassId").getNodeValue());
					final var cbh = new ClassBalanceHolder(classId, targetClassId);
					
					for (var set = d.getFirstChild(); set != null; set = set.getNextSibling())
					{
						if (set.getNodeName().equals("set"))
						{
							final double val = Double.parseDouble(set.getAttributes().getNamedItem("val").getNodeValue());
							final var atkType = AttackType.valueOf(set.getAttributes().getNamedItem("type").getNodeValue());
							cbh.addNormalBalance(atkType, val);
							
							_balanceSize += 1;
						}
						else if (set.getNodeName().equals("olyset"))
						{
							final double val = Double.parseDouble(set.getAttributes().getNamedItem("val").getNodeValue());
							final var atkType = AttackType.valueOf(set.getAttributes().getNamedItem("type").getNodeValue());
							cbh.addOlyBalance(atkType, val);
							
							_olyBalanceSize += 1;
						}
					}
					_classes.put(classId + ";" + targetClassId, cbh);
				}
			}
		}
	}
	
	public Map<String, ClassBalanceHolder> getAllBalances()
	{
		final Map<String, ClassBalanceHolder> map = new TreeMap<>(new ClassComparator());
		map.putAll(_classes);
		return map;
	}
	
	public List<ClassBalanceHolder> getClassBalances(int classId)
	{
		final List<ClassBalanceHolder> list = new ArrayList<>();
		for (final Map.Entry<String, ClassBalanceHolder> data : _classes.entrySet())
		{
			if (Integer.valueOf(data.getKey().split(";")[0]).intValue() == classId)
			{
				list.add(data.getValue());
			}
		}
		return list;
	}
	
	public int getClassBalanceSize(int classId, boolean olysize)
	{
		int size = 0;
		for (final ClassBalanceHolder data : getClassBalances(classId))
		{
			size += (!olysize ? data.getNormalBalance().size() : data.getOlyBalance().size());
		}
		return size;
	}
	
	public ClassBalanceHolder getBalanceHolder(String key)
	{
		return _classes.get(key);
	}
	
	private class ClassComparator implements Comparator<String>
	{
		public ClassComparator()
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
	
	public double getBalancedClass(AttackType type, Creature attacker, Creature victim)
	{
		if (Config.BALANCER_ALLOW)
		{
			if ((attacker != null && attacker.isPlayer()) && (victim != null && victim.isPlayer()))
			{
				final int classId = attacker.getActingPlayer().getClassId().getId();
				final int targetClassId = victim.getActingPlayer().getClassId().getId();
				if ((attacker.getActingPlayer().isInOlympiadMode()) && (victim.getActingPlayer().isInOlympiadMode()))
				{
					if (attacker.getActingPlayer().getOlympiadGameId() == victim.getActingPlayer().getOlympiadGameId())
					{
						if (_classes.containsKey(classId + ";" + targetClassId))
						{
							return _classes.get(classId + ";" + targetClassId).getOlyBalanceValue(type);
						}
						else if (_classes.containsKey(classId + ";-2"))
						{
							return _classes.get(classId + ";-2").getOlyBalanceValue(type);
						}
						else if (_classes.containsKey("-1;-2"))
						{
							return _classes.get("-1;-2").getOlyBalanceValue(type);
						}
					}
					return 1.0D;
				}
				
				if (_classes.containsKey(classId + ";" + targetClassId))
				{
					return _classes.get(classId + ";" + targetClassId).getBalanceValue(type);
				}
				else if (_classes.containsKey(classId + ";-2"))
				{
					return _classes.get(classId + ";-2").getBalanceValue(type);
				}
				else if (_classes.containsKey("-1;-2"))
				{
					return _classes.get("-1;-2").getBalanceValue(type);
				}
				return 1.0D;
			}
			else if (attacker.isPlayer() && victim.isAttackable())
			{
				final int classId = attacker.getActingPlayer().getClassId().getId();
				if (_classes.containsKey(classId + ";-1"))
				{
					return _classes.get(classId + ";-1").getBalanceValue(type);
				}
				else if (_classes.containsKey("-1;-1"))
				{
					return _classes.get("-1;-1").getBalanceValue(type);
				}
			}
		}
		return 1.0D;
	}
	
	public void removeClassBalance(String key, AttackType type, boolean isOly)
	{
		if (_classes.containsKey(key))
		{
			if (!_hasModify)
			{
				_hasModify = true;
			}
			
			if (isOly)
			{
				_classes.get(key).removeOlyBalance(type);
				_olyBalanceSize -= 1;
				return;
			}
			_classes.get(key).remove(type);
			_balanceSize -= 1;
		}
	}
	
	public void addClassBalance(String key, ClassBalanceHolder cbh, boolean isEdit)
	{
	    if (!_hasModify)
		{
			_hasModify = true;
		}
		_classes.put(key, cbh);
		
		if (!isEdit)
		{
			if (!cbh.getOlyBalance().isEmpty())
			{
				_olyBalanceSize += 1;
			}
			else
			{
				_balanceSize += 1;
			}
		}
	}
	
	public void store(Player player)
	{
		if (!_hasModify)
		{
			if (player != null)
			{
				player.sendMessage("ClassBalance: Nothing for saving!");
			}
			return;
		}
		
		try
		{
			var file = new File(Config.DATAPACK_ROOT, "data/stats/services/balancer/classBalance.xml");
			if (file.exists())
			{
				if (!file.renameTo(new File(Config.DATAPACK_ROOT, "data/stats/services/balancer/classBalance_Backup_[" + new SimpleDateFormat("YYYY-MM-dd_HH-mm-ss").format(Calendar.getInstance().getTimeInMillis()) + "].xml")))
				{
					if (player != null)
					{
						player.sendMessage("ClassBalance: can't save backup file!");
					}
				}
			}
			
			file = new File(Config.DATAPACK_ROOT, "data/stats/services/balancer/classBalance.xml");
			file.createNewFile();
			
			final FileWriter fstream = new FileWriter(file);
			final BufferedWriter out = new BufferedWriter(fstream);
			
			out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
			out.write("<list xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"../../../xsd/classBalance.xsd\">\n");
			
			for (final var cbh : _classes.values())
			{
				if (!cbh.getNormalBalance().isEmpty() || !cbh.getOlyBalance().isEmpty())
				{
					String xml = "	<balance classId=\"" + cbh.getActiveClass() + "\" targetClassId=\"" + cbh.getTargetClass() + "\">\n";
					
					for (final Map.Entry<AttackType, Double> info : cbh.getNormalBalance().entrySet())
					{
						xml += "		<set type=\"" + info.getKey().toString() + "\" val=\"" + info.getValue() + "\"/>\n";
					}
					
					for (final Map.Entry<AttackType, Double> info : cbh.getOlyBalance().entrySet())
					{
						xml += "		<olyset type=\"" + info.getKey().toString() + "\" val=\"" + info.getValue() + "\"/>\n";
					}
					
					xml = xml + "	</balance>\n";
					
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
			player.sendMessage("ClassBalance: Modified data was saved!");
		}
		_hasModify = false;
	}
	
	public int getSize(boolean olysize)
	{
		return olysize ? _olyBalanceSize : _balanceSize;
	}
	
	public static final ClassBalanceParser getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final ClassBalanceParser _instance = new ClassBalanceParser();
	}
}