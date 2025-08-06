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
package l2e.gameserver.model.actor.instance.player;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import l2e.commons.log.LoggerObject;
import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.data.dao.DonateRatesDAO;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.player.DonateRateTempate;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.ExPCCafePointInfo;
import l2e.gameserver.network.serverpackets.ExShowScreenMessage;
import l2e.gameserver.network.serverpackets.SystemMessage;
import l2e.gameserver.network.serverpackets.updatetype.UserInfoType;

/**
 * Created by LordWinter
 */
public class DonateRates extends LoggerObject
{
	private final Player _owner;
	private final Map<DonateRateTempate, Long> _donateList = new ConcurrentHashMap<>();
	private Future<?> _timeTask = null;
	private boolean _isRecalc = false;
	private long _nextTimeCalc = 0L;
	
	public DonateRates(Player player)
	{
		_owner = player;
		_donateList.clear();
	}
	
	public Set<DonateRateTempate> getActiveRates()
	{
		return _donateList.keySet();
	}
	
	public Map<DonateRateTempate, Long> getActiveAllRates()
	{
		return _donateList;
	}
	
	public boolean addBonusRates(Map<DonateRateTempate, Long> templates)
	{
		if (templates.isEmpty())
		{
			return false;
		}
		
		for (final var template : templates.keySet())
		{
			final var time = templates.get(template);
			_donateList.put(template, time);
			
			final var bonusList = template.getBonusList();
			if (!bonusList.isEmpty())
			{
				final var bonus = _owner.getPremiumBonus();
				for (final var type : bonusList.keySet())
				{
					if (type != null)
					{
						final double value = bonusList.get(type);
						if (value > 0)
						{
							bonus.addBonusType(type, value);
						}
					}
				}
			}
		}
		recalcTime();
		return true;
	}
	
	public boolean addBonusRate(DonateRateTempate template)
	{
		if (_donateList.get(template) != null || !checkValidItems(template.getPrice(), _owner))
		{
			return false;
		}
		removeRequestItems(template.getPrice(), _owner);
		final var timeCalc = System.currentTimeMillis() + (template.getTime() * 1000L);
		_donateList.put(template, timeCalc);
		DonateRatesDAO.getInstance().insert(_owner, template, timeCalc);
		Util.addServiceLog(_owner.getName(null) + " buy donate rate template id: " + template.getId());
		final var bonusList = template.getBonusList();
		if (!bonusList.isEmpty())
		{
			final var bonus = _owner.getPremiumBonus();
			for (final var type : bonusList.keySet())
			{
				if (type != null)
				{
					final double value = bonusList.get(type);
					if (value > 0)
					{
						bonus.addBonusType(type, value);
					}
				}
			}
		}
		
		if (_nextTimeCalc <= 0)
		{
			recalcTime();
		}
		else
		{
			if (_nextTimeCalc > timeCalc)
			{
				recalcTime();
			}
		}
		return true;
	}
	
	public void recalcTime()
	{
		if (_isRecalc)
		{
			return;
		}
		_isRecalc = true;
		
		if (_timeTask != null)
		{
			_timeTask.cancel(false);
		}
		_timeTask = null;
		
		if (_donateList.isEmpty())
		{
			_nextTimeCalc = 0;
			_isRecalc = false;
			return;
		}
		
		final var sorted = _donateList.entrySet().stream().sorted(Map.Entry.comparingByValue()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
		if (!sorted.isEmpty())
		{
			long nextTime = sorted.entrySet().iterator().next().getValue() - System.currentTimeMillis();
			if (nextTime < 0)
			{
				nextTime = 0;
			}
			_timeTask = ThreadPoolManager.getInstance().schedule(new BonusTask(), nextTime);
			_nextTimeCalc = System.currentTimeMillis() + nextTime;
			if (Config.DEBUG && nextTime > 0)
			{
				info("Next task run at " + new Date(System.currentTimeMillis() + nextTime));
			}
		}
		_isRecalc = false;
	}
	
	private class BonusTask implements Runnable
	{
		@Override
		public void run()
		{
			if (_owner == null)
			{
				return;
			}
			_isRecalc = true;
			final long time = System.currentTimeMillis();
			for (final var entry : _donateList.entrySet())
			{
				final var tpl = entry.getKey();
				if (time < entry.getValue())
				{
					continue;
				}
				
				if (tpl != null)
				{
					final var bonus = _owner.getPremiumBonus();
					final var bonusList = tpl.getBonusList();
					if (!bonusList.isEmpty())
					{
						for (final var type : bonusList.keySet())
						{
							if (type != null)
							{
								final double value = bonusList.get(type);
								if (value > 0)
								{
									bonus.removeBonusType(type, value);
								}
							}
						}
					}
					DonateRatesDAO.getInstance().delete(_owner, tpl.getId());
					_owner.sendPacket(new ExShowScreenMessage(_owner.getLang().equalsIgnoreCase("en") ? "" + tpl.getName(_owner.getLang()) + " is over!" : "" + tpl.getName(_owner.getLang()) + " закончился!", 5000));
					_donateList.remove(tpl);
				}
			}
			_isRecalc = false;
			recalcTime();
		}
	}
	
	public void cleanUp()
	{
		final var task = _timeTask;
		if (task != null)
		{
			task.cancel(false);
		}
		_timeTask = null;
		_nextTimeCalc = 0L;
		_donateList.clear();
	}
	
	private boolean checkValidItems(Map<Integer, Long> requestItems, Player player)
	{
		if (requestItems.isEmpty())
		{
			return true;
		}
		
		for (final var id : requestItems.keySet())
		{
			final long amount = requestItems.get(id);
			switch (id)
			{
				case -300 :
					if (player.getFame() < amount)
					{
						player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
						return false;
					}
					break;
				case -200 :
					if (player.getClan() == null || player.getClan().getReputationScore() < amount)
					{
						player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
						return false;
					}
					break;
				case -100 :
					if (player.getPcBangPoints() < amount)
					{
						player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
						return false;
					}
					break;
				default :
					if (player.getInventory().getItemByItemId(id) == null || player.getInventory().getItemByItemId(id).getCount() < amount)
					{
						player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
						return false;
					}
					break;
			}
		}
		return true;
	}
	
	private void removeRequestItems(Map<Integer, Long> requestItems, Player player)
	{
		for (final var id : requestItems.keySet())
		{
			final long amount = requestItems.get(id);
			switch (id)
			{
				case -300 :
					player.setFame((int) (player.getFame() - amount));
					player.sendUserInfo(UserInfoType.VITA_FAME);
					break;
				case -200 :
					player.getClan().takeReputationScore((int) amount, true);
					final SystemMessage msg = SystemMessage.getSystemMessage(SystemMessageId.S1_DEDUCTED_FROM_CLAN_REP);
					msg.addItemNumber(amount);
					player.sendPacket(msg);
					break;
				case -100 :
					player.setPcBangPoints((int) (player.getPcBangPoints() - amount));
					final SystemMessage smsg = SystemMessage.getSystemMessage(SystemMessageId.USING_S1_PCPOINT);
					smsg.addNumber((int) amount);
					player.sendPacket(smsg);
					player.sendPacket(new ExPCCafePointInfo(player.getPcBangPoints(), (int) amount, false, false, 1));
					break;
				default :
					player.destroyItemByItemId("Donate Rate", id, amount, player, true);
					break;
			}
		}
	}
}