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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import l2e.gameserver.model.skills.effects.Effect;
import l2e.gameserver.model.skills.effects.EffectTemplate;
import l2e.gameserver.model.skills.effects.EffectType;
import l2e.gameserver.model.stats.Env;

public final class BlockBuffSlot extends Effect
{
	private final Set<String> _blockBuffSlots;
	
	public BlockBuffSlot(Env env, EffectTemplate template)
	{
		super(env, template);
		
		final String blockBuffSlots = template.getParameters().getString("slot", null);
		if ((blockBuffSlots != null) && !blockBuffSlots.isEmpty())
		{
			_blockBuffSlots = new HashSet<>();
			for (final String slot : blockBuffSlots.split(";"))
			{
				_blockBuffSlots.add(slot);
			}
		}
		else
		{
			_blockBuffSlots = Collections.<String> emptySet();
		}
	}
	
	@Override
	public void onExit()
	{
		getEffected().getEffectList().removeBlockedBuffSlots(_blockBuffSlots);
	}
	
	@Override
	public boolean onStart()
	{
		getEffected().getEffectList().addBlockedBuffSlots(_blockBuffSlots);
		return true;
	}
	
	@Override
	public EffectType getEffectType()
	{
		return EffectType.NONE;
	}
}
