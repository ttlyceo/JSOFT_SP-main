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
package l2e.gameserver.model.entity.events.model;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import l2e.gameserver.model.entity.events.AbstractFightEvent;

/**
 * Created by LordWinter
 */
public abstract class FightEventOwner
{
	private final Set<AbstractFightEvent> _events = new CopyOnWriteArraySet<>();

	@SuppressWarnings("unchecked")
	public <E extends AbstractFightEvent> E getEvent(Class<E> eventClass)
	{
		for (final AbstractFightEvent e : _events)
		{
			if (e.getClass() == eventClass)
			{
				return (E) e;
			}
			if (eventClass.isAssignableFrom(e.getClass()))
			{
				return (E) e;
			}
		}
		return null;
	}

	public void addEvent(AbstractFightEvent event)
	{
		_events.add(event);
	}

	public void removeEvent(AbstractFightEvent event)
	{
		_events.remove(event);
	}

	public Set<AbstractFightEvent> getFightEvents()
	{
		return _events;
	}
}