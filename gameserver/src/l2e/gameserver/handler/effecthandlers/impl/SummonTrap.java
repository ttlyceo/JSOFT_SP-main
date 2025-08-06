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

import l2e.gameserver.data.parser.NpcsParser;
import l2e.gameserver.idfactory.IdFactory;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.TrapInstance;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.skills.effects.EffectTemplate;
import l2e.gameserver.model.skills.effects.Effect;
import l2e.gameserver.model.skills.effects.EffectType;
import l2e.gameserver.model.stats.Env;

public class SummonTrap extends Effect
{
	private final int _despawnTime;
	private final int _npcId;
	
	public SummonTrap(Env env, EffectTemplate template)
	{
		super(env, template);
		_despawnTime = template.getParameters().getInteger("despawnTime", 0);
		_npcId = template.getParameters().getInteger("npcId", 0);
	}

	@Override
	public EffectType getEffectType()
	{
		return EffectType.NONE;
	}

	@Override
	public boolean onStart()
	{
		if ((getEffected() == null) || !getEffected().isPlayer() || getEffected().isAlikeDead() || getEffected().getActingPlayer().inObserverMode())
		{
			return false;
		}
		
		if (_npcId <= 0)
		{
			_log.warn(SummonTrap.class.getSimpleName() + ": Invalid NPC Id:" + _npcId + " in skill Id: " + getSkill().getId());
			return false;
		}
		
		final Player player = getEffected().getActingPlayer();
		if (player.inObserverMode() || player.isMounted())
		{
			return false;
		}
		
		if (player.getTrap() != null)
		{
			player.getTrap().unSummon();
		}
		
		final NpcTemplate npcTemplate = NpcsParser.getInstance().getTemplate(_npcId);
		if (npcTemplate == null)
		{
			_log.warn(SummonTrap.class.getSimpleName() + ": Spawn of the non-existing Trap Id: " + _npcId + " in skill Id:" + getSkill().getId());
			return false;
		}
		
		final TrapInstance trap = new TrapInstance(IdFactory.getInstance().getNextId(), npcTemplate, player, _despawnTime);
		trap.setCurrentHp(trap.getMaxHp());
		trap.setCurrentMp(trap.getMaxMp());
		trap.setIsInvul(true);
		trap.setHeading(player.getHeading());
		trap.spawnMe(player.getX(), player.getY(), player.getZ());
		player.setTrap(trap);
		return true;
	}
}