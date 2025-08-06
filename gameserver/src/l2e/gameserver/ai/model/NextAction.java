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
package l2e.gameserver.ai.model;

import java.util.ArrayList;
import java.util.List;

import l2e.gameserver.geodata.GeoEngine;
import l2e.gameserver.model.actor.Creature;

public class NextAction
{
	private List<CtrlEvent> _events;
	private List<CtrlIntention> _intentions;
	private List<NextActionCallback> _callback;
	private Creature _actor = null;
	private Creature _target = null;
	
	public interface NextActionCallback
	{
		public void doWork();
	}
	
	public NextAction(ArrayList<CtrlEvent> events, ArrayList<CtrlIntention> intentions, NextActionCallback callback)
	{
		_events = events;
		_intentions = intentions;
		addCallback(callback);
	}
	
	public NextAction(Creature actor, Creature target, CtrlEvent event, CtrlIntention intention, NextActionCallback callback)
	{
		_actor = actor;
		_target = target;
		if (_events == null)
		{
			_events = new ArrayList<>();
		}
		
		if (_intentions == null)
		{
			_intentions = new ArrayList<>();
		}
		
		if (event != null)
		{
			_events.add(event);
		}
		
		if (intention != null)
		{
			_intentions.add(intention);
		}
		addCallback(callback);
	}
	
	public NextAction(CtrlEvent event, CtrlIntention intention, NextActionCallback callback)
	{
		if (_events == null)
		{
			_events = new ArrayList<>();
		}
		
		if (_intentions == null)
		{
			_intentions = new ArrayList<>();
		}
		
		if (event != null)
		{
			_events.add(event);
		}
		
		if (intention != null)
		{
			_intentions.add(intention);
		}
		addCallback(callback);
	}
	
	public boolean isAttackAction()
	{
		return _target != null;
	}
	
	public boolean isCanAtivate()
	{
		return _target != null && GeoEngine.getInstance().canSeeTarget(_actor, _target);
	}
	
	public void doAction()
	{
		if (_callback != null && !_callback.isEmpty())
		{
			for (final var action : _callback)
			{
				action.doWork();
			}
		}
	}
	
	public List<CtrlEvent> getEvents()
	{
		if (_events == null)
		{
			_events = new ArrayList<>();
		}
		return _events;
	}
	
	public void setEvents(ArrayList<CtrlEvent> event)
	{
		_events = event;
	}
	
	public void addEvent(CtrlEvent event)
	{
		if (_events == null)
		{
			_events = new ArrayList<>();
		}
		
		if (event != null)
		{
			_events.add(event);
		}
	}
	
	public void removeEvent(CtrlEvent event)
	{
		if (_events == null)
		{
			return;
		}
		_events.remove(event);
	}
	
	public List<NextActionCallback> getCallback()
	{
		return _callback;
	}
	
	public void addCallback(NextActionCallback callback)
	{
		if (_callback == null)
		{
			_callback = new ArrayList<>();
		}
		_callback.add(callback);
	}
	
	public List<CtrlIntention> getIntentions()
	{
		if (_intentions == null)
		{
			_intentions = new ArrayList<>();
		}
		return _intentions;
	}
	
	public void setIntentions(ArrayList<CtrlIntention> intentions)
	{
		_intentions = intentions;
	}
	
	public void addIntention(CtrlIntention intention)
	{
		if (_intentions == null)
		{
			_intentions = new ArrayList<>();
		}
		
		if (intention != null)
		{
			_intentions.add(intention);
		}
	}
	
	public void removeIntention(CtrlIntention intention)
	{
		if (_intentions == null)
		{
			return;
		}
		_intentions.remove(intention);
	}
}