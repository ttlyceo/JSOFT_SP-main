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
package l2e.gameserver.model.actor.tasks.npc;

import java.util.List;

import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.quest.Quest;

public class OnKillNotifyTask implements Runnable
{
	private final Attackable _attackable;
	private final List<Quest> _quests;
	private final Player _killer;
	private final boolean _isSummon;
	
	public OnKillNotifyTask(Attackable attackable, List<Quest> quests, Player killer, boolean isSummon)
	{
		_attackable = attackable;
		_quests = quests;
		_killer = killer;
		_isSummon = isSummon;
	}
	
	@Override
	public void run()
	{
		if ((_quests != null && !_quests.isEmpty()) && (_attackable != null) && (_killer != null))
		{
			for (final Quest quest : _quests)
			{
				quest.notifyKill(_attackable, _killer, _isSummon);
			}
		}
	}
}