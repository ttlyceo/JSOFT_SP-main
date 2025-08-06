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

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.skills.effects.Effect;
import l2e.gameserver.network.serverpackets.GameServerPacket;

public class ExAbnormalStatusUpdateFromTarget extends GameServerPacket
{
	private final Creature _creature;
	private final List<Effect> _effects;

	public ExAbnormalStatusUpdateFromTarget(Creature creature)
	{
		_creature = creature;
		if (creature.isRaid())
		{
			_effects = creature.getEffectList().getEffects().stream().filter(Objects::nonNull).filter(Effect::isInUse).filter(b -> b.getAbnormalTime() > 1 || b.getTotalTickCount() > 1).collect(Collectors.toList());
		}
		else
		{
			_effects = creature.getEffectList().getEffects().stream().filter(Objects::nonNull).filter(Effect::isInUse).filter(b -> b.getSkill().hasDebuffEffects() && (b.getAbnormalTime() > 1 || b.getTotalTickCount() > 1)).collect(Collectors.toList());
		}
	}
	
	public ExAbnormalStatusUpdateFromTarget(Creature creature, List<Effect> effects)
	{
		_creature = creature;
		if (creature.isRaid())
		{
			_effects = effects.stream().filter(b -> b.getAbnormalTime() > 1 || b.getTotalTickCount() > 1).collect(Collectors.toList());
		}
		else
		{
			_effects = effects.stream().filter(b -> b.getSkill().hasDebuffEffects() && (b.getAbnormalTime() > 1 || b.getTotalTickCount() > 1)).collect(Collectors.toList());
		}
	}

	@Override
	protected final void writeImpl()
	{
		if (_creature.isDead())
		{
			writeH(0x00);
			return;
		}
		
		writeH(_effects.size());
		for (final Effect info : _effects)
		{
			writeD(info.getSkill().getDisplayId());
			writeH(info.getSkill().getDisplayLevel());
			writeD(info.getTimeLeft());
		}
	}
}
