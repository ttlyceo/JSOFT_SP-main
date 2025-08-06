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

import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.skills.effects.Effect;
import l2e.gameserver.model.skills.effects.EffectTemplate;
import l2e.gameserver.model.skills.effects.EffectType;
import l2e.gameserver.model.stats.Env;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.SystemMessage;

public class GiveRecommendation extends Effect
{
	private final int _amount;
	
	public GiveRecommendation(Env env, EffectTemplate template)
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
		final Player target = getEffected() instanceof Player ? (Player) getEffected() : null;
		if (target != null)
		{
			int recommendationsGiven = _amount;
			
			if ((target.getRecommendation().getRecomHave() + _amount) >= 255)
			{
				recommendationsGiven = 255 - target.getRecommendation().getRecomHave();
			}
			
			if (recommendationsGiven > 0)
			{
				target.getRecommendation().setRecomHave(target.getRecommendation().getRecomHave() + recommendationsGiven);
				
				final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.YOU_OBTAINED_S1_RECOMMENDATIONS);
				sm.addNumber(recommendationsGiven);
				target.sendPacket(sm);
				target.sendUserInfo();
				target.sendVoteSystemInfo();
			}
			else
			{
				final Player player = getEffector() instanceof Player ? (Player) getEffector() : null;
				if (player != null)
				{
					player.sendPacket(SystemMessageId.NOTHING_HAPPENED);
				}
			}
		}
		return true;
	}
}