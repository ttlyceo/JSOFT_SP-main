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
package l2e.gameserver.model.actor.tasks.character;

import l2e.gameserver.ai.model.CtrlEvent;
import l2e.gameserver.model.actor.Creature;

public final class NotifyAITask implements Runnable
{
	private final Creature _character;
	private final CtrlEvent _event;
	private final Object _agr0;
	
	public NotifyAITask(Creature character, CtrlEvent event, Object agr0)
	{
		_character = character;
		_event = event;
		_agr0 = agr0;
	}

	public NotifyAITask(Creature character, CtrlEvent event)
	{
		_character = character;
		_event = event;
		_agr0 = null;
	}

	@Override
	public void run()
	{
		if (_character == null)
		{
			return;
		}
		
		final var ai = _character.getAI();
		if (ai != null)
		{
			ai.notifyEvent(_event, _agr0);
		}
	}
}