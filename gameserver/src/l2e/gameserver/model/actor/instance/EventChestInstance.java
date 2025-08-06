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
package l2e.gameserver.model.actor.instance;

import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.network.serverpackets.GameServerPacket;
import l2e.gameserver.network.serverpackets.NpcInfo.Info;

public final class EventChestInstance extends EventMonsterInstance
{
	private boolean _isTriggered = false;

	public EventChestInstance(int objectId, NpcTemplate template)
	{
		super(objectId, template);
		
		setIsNoRndWalk(true);
		disableCoreAI(true);
		setInvisible(true);
		eventSetDropOnGround(true);
	}
	
	public boolean canSee(Creature cha)
	{
		return !isInvisible();
	}
	
	public void trigger(Creature cha)
	{
		_isTriggered = true;
		setInvisible(false);
		if (!_isTriggered)
		{
			broadcastPacket(new Info(this, cha));
		}
	}
	
	@Override
	public void sendInfo(Player activeChar)
	{
		if (_isTriggered || canSee(activeChar))
		{
			activeChar.sendPacket(new Info(this, activeChar));
		}
	}
	
	@Override
	public void broadcastPacket(GameServerPacket... packets)
	{
		World.getInstance().getAroundPlayers(this).stream().filter(p -> p != null && (_isTriggered || canSee(p))).forEach(p -> p.sendPacket(packets));
	}
	
	@Override
	public void broadcastPacket(int range, GameServerPacket... packets)
	{
		World.getInstance().getAroundPlayers(this, range, 300).stream().filter(p -> p != null && (_isTriggered || canSee(p))).forEach(p -> p.sendPacket(packets));
	}
	
	@Override
	public boolean isAutoAttackable(Creature attacker, boolean isPoleAttack)
	{
		return !canSee(attacker);
	}
	
	@Override
	public boolean canBeAttacked()
	{
		return false;
	}
	
	@Override
	public boolean isAttackable()
	{
		return false;
	}
}