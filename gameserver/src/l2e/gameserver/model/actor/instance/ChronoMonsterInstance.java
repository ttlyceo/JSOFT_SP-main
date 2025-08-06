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

import static l2e.gameserver.ai.model.CtrlIntention.ACTIVE;
import static l2e.gameserver.ai.model.CtrlIntention.ATTACK;

import l2e.gameserver.ai.DefaultAI;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;

public class ChronoMonsterInstance extends MonsterInstance
{
	private Player _owner;
	private int _lvlUp;
	
	public ChronoMonsterInstance(int objectId, NpcTemplate template)
	{
		super(objectId, template);

		setInstanceType(InstanceType.ChronoMonsterInstance);
		setAI(new L2ChronoAI(this));
		_lvlUp = 0;
	}

	public final Player getOwner()
	{
		return _owner;
	}

	public void setOwner(Player newOwner)
	{
		_owner = newOwner;
	}

	public void setLevelUp(int lvl)
	{
		_lvlUp = lvl;
	}
	
	public int getLevelUp()
	{
		return _lvlUp;
	}

	class L2ChronoAI extends DefaultAI
	{
		public L2ChronoAI(Attackable accessor)
		{
			super(accessor);
		}

		@Override
		protected void onEvtThink()
		{
			if (_actor.isAllSkillsDisabled())
			{
				return;
			}

			if (getIntention() == ATTACK)
			{
				setIntention(ACTIVE);
			}
		}
	}
	
	@Override
	public boolean isMonster()
	{
		return false;
	}
}