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
package l2e.gameserver.handler.effecthandlers.impl;

import l2e.gameserver.Config;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.skills.effects.Effect;
import l2e.gameserver.model.skills.effects.EffectTemplate;
import l2e.gameserver.model.skills.effects.EffectType;
import l2e.gameserver.model.stats.Env;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.ExPCCafePointInfo;
import l2e.gameserver.network.serverpackets.SystemMessage;

public final class PcBangPointUp extends Effect
{
	private final int _amount;
	
	public PcBangPointUp(Env env, EffectTemplate template)
	{
		super(env, template);
		
		_amount = template.getParameters().getInteger("amount", 0);
	}
	
	@Override
	public EffectType getEffectType()
	{
		return EffectType.NONE;
	}
	
	@Override
	public boolean onStart()
	{
		final Player player = getEffected().isPlayer() ? (Player) getEffected() : null;
		if (player == null)
		{
			return false;
		}
		
		int points = _amount;
		if ((player.getPcBangPoints() + points) > Config.MAX_PC_BANG_POINTS)
		{
			points = Config.MAX_PC_BANG_POINTS - player.getPcBangPoints();
		}
		
		if (Config.PC_POINT_ID < 0)
		{
			player.setPcBangPoints(player.getPcBangPoints() + points);
		}
		else
		{
			player.setPcBangPoints(player.getPcBangPoints() + points);
			player.addItem("PcPoints", Config.PC_POINT_ID, points, player, true);
		}
		final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.YOU_HAVE_ACQUIRED_S1_PC_CAFE_POINTS);
		sm.addNumber(points);
		player.sendPacket(sm);
		player.sendPacket(new ExPCCafePointInfo(player.getPcBangPoints(), points, true, false, 1));
		return true;
	}
}
