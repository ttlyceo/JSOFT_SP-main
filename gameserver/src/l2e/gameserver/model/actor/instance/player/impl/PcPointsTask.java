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
package l2e.gameserver.model.actor.instance.player.impl;

import l2e.commons.util.Rnd;
import l2e.gameserver.Config;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.ExPCCafePointInfo;
import l2e.gameserver.network.serverpackets.MagicSkillUse;
import l2e.gameserver.network.serverpackets.SystemMessage;

public class PcPointsTask extends AbstractPlayerTask
{
	private final long _delay;

	public PcPointsTask(long delay)
	{
		_delay = delay;
	}
	
	@Override
	public boolean getTask(Player player)
	{
		if (player != null)
		{
			if (!Config.PC_BANG_ENABLED || (Config.PC_BANG_INTERVAL <= 0))
			{
				return false;
			}
			
			if (Config.PC_BANG_ONLY_FOR_PREMIUM && !player.hasPremiumBonus())
			{
				return false;
			}
			
			if ((player.getLevel() > Config.PC_BANG_MIN_LEVEL) && player.isOnline() && !player.isInOfflineMode())
			{
				if (player.getPcBangPoints() >= Config.MAX_PC_BANG_POINTS)
				{
					final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.THE_MAXMIMUM_ACCUMULATION_ALLOWED_OF_PC_CAFE_POINTS_HAS_BEEN_EXCEEDED);
					player.sendPacket(sm);
					return false;
				}
				
				int _points = player.hasPremiumBonus() ? Rnd.get(Config.PC_BANG_POINTS_PREMIUM_MIN, Config.PC_BANG_POINTS_PREMIUM_MAX) : Rnd.get(Config.PC_BANG_POINTS_MIN, Config.PC_BANG_POINTS_MAX);
				
				boolean doublepoint = false;
				SystemMessage sm = null;
				if (_points > 0)
				{
					if (Config.ENABLE_DOUBLE_PC_BANG_POINTS && (Rnd.get(100) < Config.DOUBLE_PC_BANG_POINTS_CHANCE))
					{
						_points *= 2;
						sm = SystemMessage.getSystemMessage(SystemMessageId.ACQUIRED_S1_PCPOINT_DOUBLE);
						player.broadcastPacket(new MagicSkillUse(player, player, 2023, 1, 100, 0));
						doublepoint = true;
					}
					else
					{
						sm = SystemMessage.getSystemMessage(SystemMessageId.YOU_HAVE_ACQUIRED_S1_PC_CAFE_POINTS);
					}
					
					if ((player.getPcBangPoints() + _points) > Config.MAX_PC_BANG_POINTS)
					{
						_points = Config.MAX_PC_BANG_POINTS - player.getPcBangPoints();
					}
					sm.addNumber(_points);
					player.sendPacket(sm);
					
					if (Config.PC_POINT_ID < 0)
					{
						player.setPcBangPoints(player.getPcBangPoints() + _points);
					}
					else
					{
						player.setPcBangPoints(player.getPcBangPoints() + _points);
						player.addItem("PcPoints", Config.PC_POINT_ID, _points, player, true);
					}
					player.sendPacket(new ExPCCafePointInfo(player.getPcBangPoints(), _points, true, doublepoint, 1));
				}
			}
		}
		return true;
	}
	
	@Override
	public int getId()
	{
		return 15;
	}
	
	@Override
	public boolean isOneUse()
	{
		return true;
	}
	
	@Override
	public boolean isSingleUse()
	{
		return false;
	}
	
	@Override
	public long getInterval()
	{
		return System.currentTimeMillis() + _delay;
	}
}