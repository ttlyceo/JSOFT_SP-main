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

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import l2e.commons.time.cron.SchedulingPattern;
import l2e.commons.time.cron.SchedulingPattern.InvalidPatternException;
import l2e.gameserver.Announcements;
import l2e.gameserver.Config;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.data.DocumentParser;
import l2e.gameserver.instancemanager.ServerVariables;
import l2e.gameserver.listener.player.OnPlayerEnterListener;
import l2e.gameserver.listener.player.PlayerListenerList;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.player.SpecialRateTempate;
import l2e.gameserver.model.base.BonusType;
import l2e.gameserver.model.stats.StatsSet;
import l2e.gameserver.model.strings.server.ServerMessage;

/**
 * Created by LordWinter
 */
public final class SpecialRatesParser extends DocumentParser implements OnPlayerEnterListener
{
	private final Map<SpecialRateTempate, Long> _templates = new ConcurrentHashMap<>();
	private Future<?> _timeTask = null;
	private boolean _isRecalc = false;
	private boolean _isActive = false;
	private SpecialRateTempate _activeTemplate = null;
	private long _time = 0L;

	private SpecialRatesParser()
	{
		_templates.clear();
		load();
		final long lastUpdate = ServerVariables.getLong("SpecialRate_Time", 0);
		if (System.currentTimeMillis() > lastUpdate)
		{
			calcNextTime();
		}
		else
		{
			final int id = ServerVariables.getInt("SpecialRate_ID", 0);
			if (id > 0)
			{
				final var tpl = getTemplate(id);
				if (tpl != null)
				{
					final var bonusList = tpl.getBonusList();
					if (!bonusList.isEmpty())
					{
						if (tpl.isAnnounceEnable())
						{
							Announcements.getInstance().announceToAll(tpl.getStartAnnounce());
						}
						_isActive = true;
						_activeTemplate = tpl;
						for (final var player : GameObjectsStorage.getPlayers())
						{
							if (player != null && player.isOnline())
							{
								final var bonus = player.getPremiumBonus();
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
					}
					_time = lastUpdate;
					_timeTask = ThreadPoolManager.getInstance().schedule(new DisableRateTask(tpl), (lastUpdate - System.currentTimeMillis()));
					if (Config.DEBUG)
					{
						info("Special Rates end at " + new Date(lastUpdate));
					}
				}
			}
			else
			{
				calcNextTime();
			}
		}
		
		if (!_templates.isEmpty())
		{
			PlayerListenerList.addGlobal(this);
		}
	}
	
	@Override
	public synchronized void load()
	{
		parseDatapackFile("data/stats/services/specialRates.xml");
		info("Loaded " + _templates.size() + " special rate templates.");
	}
	
	@Override
	protected void reloadDocument()
	{
		if (_timeTask != null)
		{
			_timeTask.cancel(false);
		}
		_timeTask = null;
		_templates.clear();
		parseDatapackFile("data/stats/services/specialRates.xml");
		info("Loaded " + _templates.size() + " special rate templates.");
		calcNextTime();
	}
	
	@Override
	protected void parseDocument()
	{
		for (var n = getCurrentDocument().getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				for (var d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("rates".equalsIgnoreCase(d.getNodeName()))
					{
						final StatsSet params = new StatsSet();
						final Map<BonusType, Double> bonusList = new ConcurrentHashMap<>();
						long startTime = 0L;
						ServerMessage startAnnounce = null;
						ServerMessage stopAnnounce = null;
						
						final int id = Integer.parseInt(d.getAttributes().getNamedItem("id").getNodeValue());
						final var startPattern = d.getAttributes().getNamedItem("startPattern").getNodeValue();
						params.set("startPattern", startPattern);
						params.set("stopPattern", d.getAttributes().getNamedItem("stopPattern").getNodeValue());
						params.set("allowAnnounce", Boolean.parseBoolean(d.getAttributes().getNamedItem("allowAnnounce").getNodeValue()));
						final var enable = Boolean.parseBoolean(d.getAttributes().getNamedItem("enable").getNodeValue());
						params.set("enable", enable);
						if (enable)
						{
							SchedulingPattern cronTime;
							try
							{
								cronTime = new SchedulingPattern(startPattern);
							}
							catch (final InvalidPatternException e)
							{
								return;
							}
							startTime = cronTime.next(System.currentTimeMillis());
						}
						
						for (var cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling())
						{
							if ("rate".equalsIgnoreCase(cd.getNodeName()))
							{
								final var type = BonusType.valueOf(cd.getAttributes().getNamedItem("type").getNodeValue());
								if (type != null)
								{
									final var value = cd.getAttributes().getNamedItem("value").getNodeValue();
									final String[] param = value.split("-");
									if (param.length == 2)
									{
										addBonusType(bonusList, type, Double.parseDouble(param[0]));
										addBonusType(bonusList, type, Double.parseDouble(param[1]));
									}
									else
									{
										addBonusType(bonusList, type, Double.parseDouble(value));
									}
								}
							}
							else if ("announce".equalsIgnoreCase(cd.getNodeName()))
							{
								startAnnounce = new ServerMessage(cd.getAttributes().getNamedItem("start").getNodeValue(), true);
								stopAnnounce = new ServerMessage(cd.getAttributes().getNamedItem("stop").getNodeValue(), true);
							}
						}
						
						if (startTime > 0)
						{
							_templates.put(new SpecialRateTempate(id, params, bonusList, startAnnounce, stopAnnounce), startTime);
						}
					}
				}
			}
		}
	}

	private void addBonusType(Map<BonusType, Double> bonusList, BonusType type, double value)
	{
		if (bonusList.containsKey(type))
		{
			final var val = bonusList.get(type);
			switch (type)
			{
				case CRAFT_CHANCE :
				case MASTER_WORK_CHANCE :
				case ENCHANT_CHANCE :
				case MAX_DROP_PER_ONE_GROUP :
				case MAX_SPOIL_PER_ONE_GROUP :
				case MAX_RAID_DROP_PER_ONE_GROUP :
					bonusList.put(type, (val + value));
					break;
				default :
					bonusList.put(type, (val + (value - 1)));
					break;
			}
			return;
		}
		bonusList.put(type, value);
	}
	
	public void calcNextTime()
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
		
		if (_templates.isEmpty())
		{
			_isRecalc = false;
			return;
		}
		
		final var sorted = _templates.entrySet().stream().sorted(Map.Entry.comparingByValue()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
		if (!sorted.isEmpty())
		{
			long nextTime = sorted.entrySet().iterator().next().getValue() - System.currentTimeMillis();
			if (nextTime < 0)
			{
				nextTime = 0;
			}
			_time = System.currentTimeMillis() + nextTime;
			final var tpl = sorted.entrySet().iterator().next().getKey();
			_activeTemplate = tpl;
			_timeTask = ThreadPoolManager.getInstance().schedule(new EnableRateTask(tpl), nextTime);
			if (Config.DEBUG && nextTime > 0)
			{
				info("Next Special Rates run at " + new Date(System.currentTimeMillis() + nextTime));
			}
		}
		_isRecalc = false;
	}
	
	private class EnableRateTask implements Runnable
	{
		private final SpecialRateTempate _tpl;
		
		public EnableRateTask(SpecialRateTempate tpl)
		{
			_tpl = tpl;
		}
		
		@Override
		public void run()
		{
			if (_tpl != null)
			{
				final var bonusList = _tpl.getBonusList();
				if (!bonusList.isEmpty())
				{
					if (_tpl.isAnnounceEnable())
					{
						Announcements.getInstance().announceToAll(_tpl.getStartAnnounce());
					}
					_isActive = true;
					_activeTemplate = _tpl;
					for (final var player : GameObjectsStorage.getPlayers())
					{
						if (player != null && player.isOnline())
						{
							final var bonus = player.getPremiumBonus();
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
					
					final var stopTime = _tpl.getDisableTime();
					_time = stopTime;
					ServerVariables.set("SpecialRate_ID", _tpl.getId());
					ServerVariables.set("SpecialRate_Time", stopTime);
					if (stopTime > 0)
					{
						_timeTask = ThreadPoolManager.getInstance().schedule(new DisableRateTask(_tpl), (stopTime - System.currentTimeMillis()));
						if (Config.DEBUG)
						{
							info("Special Rates end at " + new Date(stopTime));
						}
					}
				}
			}
		}
	}
	
	private class DisableRateTask implements Runnable
	{
		private final SpecialRateTempate _tpl;
		
		public DisableRateTask(SpecialRateTempate tpl)
		{
			_tpl = tpl;
		}
		
		@Override
		public void run()
		{
			if (_tpl != null)
			{
				final var bonusList = _tpl.getBonusList();
				if (!bonusList.isEmpty())
				{
					if (_tpl.isAnnounceEnable())
					{
						Announcements.getInstance().announceToAll(_tpl.getStopAnnounce());
					}
					_isActive = false;
					_activeTemplate = null;
					for (final var player : GameObjectsStorage.getPlayers())
					{
						if (player != null && player.isOnline())
						{
							final var bonus = player.getPremiumBonus();
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
					}
					
					if (_templates.containsKey(_tpl))
					{
						final var startTime = _tpl.getStartTime();
						if (startTime > 0)
						{
							_templates.put(_tpl, startTime);
						}
					}
					calcNextTime();
				}
			}
		}
	}
	
	private SpecialRateTempate getTemplate(int id)
	{
		for (final var tpl : _templates.keySet())
		{
			if (tpl != null && tpl.getId() == id)
			{
				return tpl;
			}
		}
		return null;
	}
	
	@Override
	public void onPlayerEnter(Player player)
	{
		if (_isActive && _activeTemplate != null)
		{
			final var bonusList = _activeTemplate.getBonusList();
			if (!bonusList.isEmpty())
			{
				final var bonus = player.getPremiumBonus();
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
	}
	
	public boolean isActive()
	{
		return _isActive;
	}
	
	public void reload()
	{
		reloadDocument();
	}
	
	public boolean enableRates()
	{
		if (_isActive || _activeTemplate == null)
		{
			return false;
		}
		
		if (_timeTask != null)
		{
			_timeTask.cancel(false);
		}
		_timeTask = null;
		
		_timeTask = ThreadPoolManager.getInstance().schedule(new EnableRateTask(_activeTemplate), 0L);
		return true;
	}
	
	public boolean disableRates()
	{
		if (!_isActive)
		{
			return false;
		}
		
		if (_timeTask != null)
		{
			_timeTask.cancel(false);
		}
		_timeTask = null;
		
		_timeTask = ThreadPoolManager.getInstance().schedule(new DisableRateTask(_activeTemplate), 0L);
		return false;
	}
	
	public long getTime()
	{
		return _time;
	}
	
	public static SpecialRatesParser getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final SpecialRatesParser _instance = new SpecialRatesParser();
	}
}