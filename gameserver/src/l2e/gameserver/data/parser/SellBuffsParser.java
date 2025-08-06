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
import java.util.List;

import org.w3c.dom.Node;

import l2e.gameserver.data.DocumentParser;
import l2e.gameserver.model.actor.Player;

public final class SellBuffsParser extends DocumentParser
{
	private final List<SellBuffTemplate> _sellBuffs = new ArrayList<>();
	private boolean _isSellerCheck = false;
	
	protected SellBuffsParser()
	{
		_sellBuffs.clear();
		load();
	}
	
	@Override
	public synchronized void load()
	{
		parseDatapackFile("data/stats/services/sellBuffs.xml");
		info("Loaded " + _sellBuffs.size() + " available skills.");
	}
	
	@Override
	protected void reloadDocument()
	{
	}
	
	@Override
	protected void parseDocument()
	{
		for (Node node = getCurrentDocument().getFirstChild(); node != null; node = node.getNextSibling())
		{
			if ("list".equalsIgnoreCase(node.getNodeName()))
			{
				_isSellerCheck = node.getAttributes().getNamedItem("isSellerCheck") != null ? Boolean.parseBoolean(node.getAttributes().getNamedItem("isSellerCheck").getNodeValue()) : false;
				for (Node cd = node.getFirstChild(); cd != null; cd = cd.getNextSibling())
				{
					if ("skill".equalsIgnoreCase(cd.getNodeName()))
					{
						final var skillId = Integer.parseInt(cd.getAttributes().getNamedItem("id").getNodeValue());
						final var buffTime = cd.getAttributes().getNamedItem("buffTime") != null ? Integer.parseInt(cd.getAttributes().getNamedItem("buffTime").getNodeValue()) : 0;
						final var premiumBuffTime = cd.getAttributes().getNamedItem("premiumBuffTime") != null ? Integer.parseInt(cd.getAttributes().getNamedItem("premiumBuffTime").getNodeValue()) : 0;
						if (!_sellBuffs.contains(skillId))
						{
							_sellBuffs.add(new SellBuffTemplate(skillId, buffTime, premiumBuffTime));
						}
					}
				}
			}
		}
	}

	public List<SellBuffTemplate> getSellBuffs()
	{
		return _sellBuffs;
	}
	
	public SellBuffTemplate getSellBuff(int id)
	{
		for (final SellBuffTemplate buff : getSellBuffs())
		{
			if (buff != null && buff.getId() == id)
			{
				return buff;
			}
		}
		return null;
	}
	
	public int getBuffTime(Player player, Player buyer, int skillId)
	{
		for (final SellBuffTemplate singleBuff : getSellBuffs())
		{
			if (singleBuff != null && singleBuff.getId() == skillId)
			{
				return (_isSellerCheck ? player.hasPremiumBonus() : buyer.hasPremiumBonus()) ? singleBuff.getPremiumBuffTime() : singleBuff.getBuffTime();
			}
		}
		return 0;
	}
	
	public static class SellBuffTemplate
	{
		private final int _buffId;
		private final int _buffTime;
		private final int _premiumBuffTime;
		
		public SellBuffTemplate(int buffId, int buffTime, int premiumBuffTime)
		{
			_buffId = buffId;
			_buffTime = buffTime;
			_premiumBuffTime = premiumBuffTime;
		}
		
		public int getId()
		{
			return _buffId;
		}
		
		public int getBuffTime()
		{
			return _buffTime;
		}
		
		public int getPremiumBuffTime()
		{
			return _premiumBuffTime;
		}
	}
	
	public static SellBuffsParser getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected final static SellBuffsParser _instance = new SellBuffsParser();
	}
}