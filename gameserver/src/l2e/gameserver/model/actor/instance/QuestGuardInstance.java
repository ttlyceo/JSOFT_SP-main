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

import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestEventType;
import l2e.gameserver.model.skills.Skill;

public final class QuestGuardInstance extends GuardInstance
{
	private boolean _isPassive = false;

	public QuestGuardInstance(int objectId, NpcTemplate template)
	{
		super(objectId, template);
		setInstanceType(InstanceType.QuestGuardInstance);
	}
	
	@Override
	public void addDamage(Creature attacker, int damage, Skill skill)
	{
		super.addDamage(attacker, damage, skill);
		
		if (attacker instanceof Attackable)
		{
			if (getTemplate().getEventQuests(QuestEventType.ON_ATTACK) != null)
			{
				for (final Quest quest : getTemplate().getEventQuests(QuestEventType.ON_ATTACK))
				{
					quest.notifyAttack(this, null, damage, false, skill);
				}
			}
		}
	}
	
	@Override
	public void addDamageHate(Creature attacker, int damage, int aggro)
	{
		if (!isPassive() && !(attacker.isPlayer()))
		{
			super.addDamageHate(attacker, damage, aggro);
		}
	}
	
	@Override
	public boolean isAutoAttackable(Creature attacker, boolean isPoleAttack)
	{
		return !attacker.isPlayer();
	}

	@Override
	public void returnHome()
	{
	}

	public void setPassive(boolean state)
	{
		_isPassive = state;
	}

	public boolean isPassive()
	{
		return _isPassive;
	}
}