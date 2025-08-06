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

import l2e.gameserver.model.skills.effects.Effect;
import l2e.gameserver.model.skills.effects.EffectTemplate;
import l2e.gameserver.model.skills.effects.EffectType;
import l2e.gameserver.model.stats.Env;
import l2e.gameserver.network.serverpackets.MagicSkillUse;

public class VisualSkin extends Effect
{
	private final int _skinId;
	private final boolean _isWithEffect;
	
	public VisualSkin(Env env, EffectTemplate template)
	{
		super(env, template);
		
		_skinId = template.getParameters().getInteger("skinId");
		_isWithEffect = template.getParameters().getBool("withEffect");
	}

	@Override
	public EffectType getEffectType()
	{
		return EffectType.VISUAL_SKIN;
	}

	@Override
	public boolean onStart()
	{
		if (getEffected().isPlayer() && _skinId > 0)
		{
			getEffected().getActingPlayer().setVar("visualBuff", _skinId);
			if (_isWithEffect)
			{
				getEffected().broadcastPacket(new MagicSkillUse(getEffected(), getEffected(), 22217, 1, 0, 0));
			}
			getEffected().getActingPlayer().broadcastUserInfo(true);
			getEffected().getActingPlayer().sendUserInfo(true);
			return true;
		}
		return false;
	}

	@Override
	public void onExit()
	{
		if (getEffected().isPlayer())
		{
			getEffected().getActingPlayer().setVar("visualBuff", 0);
			if (_isWithEffect)
			{
				getEffected().broadcastPacket(new MagicSkillUse(getEffected(), getEffected(), 22217, 1, 0, 0));
			}
			getEffected().getActingPlayer().broadcastUserInfo(true);
			getEffected().getActingPlayer().sendUserInfo(true);
		}
	}
}