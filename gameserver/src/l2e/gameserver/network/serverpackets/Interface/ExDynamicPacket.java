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
package l2e.gameserver.network.serverpackets.Interface;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import l2e.gameserver.data.parser.ExperienceParser;
import l2e.gameserver.instancemanager.DailyTaskManager;
import l2e.gameserver.instancemanager.ServerVariables;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.player.PlayerTaskTemplate;
import l2e.gameserver.model.reward.CalculateRewardChances;
import l2e.gameserver.model.reward.CalculateRewardChances.DropInfoTemplate;
import l2e.gameserver.network.serverpackets.GameServerPacket;

public class ExDynamicPacket extends GameServerPacket
{
	private int _type = 0;
	private final Npc _npc;
	private final Player _player;
	private final List<DropInfoTemplate> _dropList;
	private final List<DropInfoTemplate> _spoilList;
    
	private final boolean _openVipWnd;
	private final int _vipLevel;
	private final long _vipCurPoints;
	private final long _vipTotalPoints;
	private final int _status;
	private final int _time;
    
	private Collection<PlayerTaskTemplate> _quests;
	
	public ExDynamicPacket(Player player, Npc npc)
	{
		_type = 6;
		_npc = npc;
		_player = player;
		final double penaltyMod = ExperienceParser.getInstance().penaltyModifier(npc.calculateLevelDiffForDrop(player.getLevel()), 9);
		_dropList = CalculateRewardChances.getAmountAndChance(player, npc.getTemplate(), penaltyMod, true, npc.getChampionTemplate());
		Comparator<DropInfoTemplate> statsComparator = new SortDropInfo();
		Collections.sort(_dropList, statsComparator);
		_spoilList = CalculateRewardChances.getAmountAndChance(player, npc.getTemplate(), penaltyMod, false, npc.getChampionTemplate());
		statsComparator = new SortDropInfo();
		Collections.sort(_spoilList, statsComparator);
		
		_openVipWnd = false;
		_vipLevel = 0;
		_vipCurPoints = 0;
		_vipTotalPoints = 0;
		_status = 0;
		_time = 0;
	}

	public ExDynamicPacket(final int level, final long curPoints, final long neededPoints, final boolean bShowWindow)
	{
		_type = 9;
		_vipLevel = level;
		_vipCurPoints = curPoints;
		_vipTotalPoints = neededPoints;
		_openVipWnd = bShowWindow;
		
		_dropList = null;
		_spoilList = null;
		_npc = null;
		_player = null;
		_status = 0;
		_time = 0;
	}
	
	public ExDynamicPacket(Player player)
	{
		_type = 10;
		_player = player;
		_quests = player.getActiveDailyTasks();
		
		_dropList = null;
		_spoilList = null;
		_npc = null;
		_openVipWnd = false;
		_vipLevel = 0;
		_vipCurPoints = 0;
		_vipTotalPoints = 0;
		_status = 0;
		_time = 0;
	}
	
	public ExDynamicPacket(int status, int min)
	{
		_type = 11;
		_status = status;
		_time = min;
		
		_dropList = null;
		_spoilList = null;
		_npc = null;
		_player = null;
		_openVipWnd = false;
		_vipLevel = 0;
		_vipCurPoints = 0;
		_vipTotalPoints = 0;
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(_type);
		switch (_type)
		{
			case 6 :
				writeD(_npc.getId());
				writeD(_npc.getLevel());
				writeQ(_npc.getExpReward(_player));
				writeD(_npc.getSpReward(_player));
				writeD((int) _npc.getCurrentHp());
				writeD((int) _npc.getCurrentMp());
				writeD((int) _npc.getPAtk(null));
				writeD((int) _npc.getMAtk(null, null));
				writeD((int) _npc.getPDef(null));
				writeD((int) _npc.getMDef(null, null));
				writeH(_dropList.size());
				for (final var d : _dropList)
				{
					writeD(d._item.getId());
					writeD(d._item.getItem().getType2());
					writeD(d._item.getItem().getBodyPart());
					writeQ(d._minCount);
					writeQ(d._minCount > d._maxCount ? d._minCount : d._maxCount);
					writeF(d._chance * 100D);
				}
				writeH(_spoilList.size());
				for (final var d : _spoilList)
				{
					writeD(d._item.getId());
					writeD(d._item.getItem().getType2());
					writeD(d._item.getItem().getBodyPart());
					writeQ(d._minCount);
					writeQ(d._minCount > d._maxCount ? d._minCount : d._maxCount);
					writeF(d._chance * 100D);
				}
				break;
			case 9 :
				writeC(_vipLevel);
				writeD((int) _vipCurPoints);
				writeD((int) _vipTotalPoints);
				writeC(_openVipWnd ? 0x01 : 0x00);
				break;
			case 10 :
				final var dailyTime = ServerVariables.getLong("Daily_Tasks", 0);
				final var weeklyTime = ServerVariables.getLong("Weekly_Tasks", 0);
				final var monthTime = ServerVariables.getLong("Month_Tasks", 0);
				final var now = System.currentTimeMillis();
				writeD(dailyTime > 0 && dailyTime > now ? (int) ((dailyTime - now) / 1000) : 0x00);
				writeD(weeklyTime > 0 && weeklyTime > now ? (int) ((weeklyTime - now) / 1000) : 0x00);
				writeD(monthTime > 0 && monthTime > now ? (int) ((monthTime - now) / 1000) : 0x00);
				writeH(_quests.size());
				for (final var q : _quests)
				{
					writeH(q.getId());
					switch (q.getSort())
					{
						case "daily" :
							writeC(0x01);
							writeC(0x00); // color
							break;
						case "weekly" :
							writeC(0x02);
							writeC(0x01); // color
							break;
						case "month" :
							writeC(0x03);
							writeC(0x02); // color
							break;
						default :
							writeC(0x01);
							writeC(0x00); // color
							break;
					}
					writeC(q.isComplete() ? 0x02 : 0x01); // 0 -locked
					writeC(!q.isComplete() ? 0x00 : q.isComplete() && !q.isRewarded() ? 0x01 : 0x02);
					final var task = DailyTaskManager.getInstance().getDailyTask(q.getId());
					switch (task.getType())
					{
						case "Farm" :
							writeH(q.getCurrentNpcCount());
							writeH(task.getNpcCount());
							break;
						case "Quest" :
							writeH(q.isComplete() ? 0x01 : 0x00);
							writeH(0x01);
							break;
						case "Reflection" :
							writeH(q.isComplete() ? 0x01 : 0x00);
							writeH(0x01);
							break;
						case "Pvp" :
							writeH(q.getCurrentPvpCount());
							writeH(task.getPvpCount());
							break;
						case "Pk" :
							writeH(q.getCurrentPkCount());
							writeH(task.getPkCount());
							break;
						case "Olympiad" :
							writeH(q.getCurrentOlyMatchCount());
							writeH(task.getOlyMatchCount());
							break;
						case "Event" :
							writeH(q.getCurrentEventsCount());
							writeH(task.getEventsCount());
							break;
						case "Siege" :
							writeH(q.isComplete() ? 0x01 : 0x00);
							writeH(0x01);
							break;
						default :
							writeH(q.isComplete() ? 0x01 : 0x00);
							writeH(0x01);
							break;
					}
					writeD(0x00);// X minimap
					writeD(0x00);// Y minimap
					writeD(0x00);// Z minimap
					writeC(0x00);// flag town map (1) or global map (0)
					final var rewards = task.getRewards();
					writeC(rewards.size());
					for (final var r : rewards.keySet())
					{
						final long count = rewards.get(r);
						writeD(r);
						writeD((int) count);
					}
				}
				break;
			case 11 :
				writeC(_status);
				writeD(_time);// time in seconds for using bot
				break;
		}
	}
	
	private static class SortDropInfo implements Comparator<DropInfoTemplate>, Serializable
	{
		private static final long serialVersionUID = 7691414259610932752L;
		
		@Override
		public int compare(DropInfoTemplate o1, DropInfoTemplate o2)
		{
			return Double.compare(o2._chance, o1._chance);
		}
	}
}
