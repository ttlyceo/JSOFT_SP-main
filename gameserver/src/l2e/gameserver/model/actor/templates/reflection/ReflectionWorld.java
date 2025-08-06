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
package l2e.gameserver.model.actor.templates.reflection;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.entity.Reflection;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.SystemMessage;

public class ReflectionWorld
{
	private Reflection _reflection = null;
	private int _templateId = -1;
	private final List<Integer> _allowed = new CopyOnWriteArrayList<>();
	private final AtomicInteger _status = new AtomicInteger();
	private boolean _isLocked = false;
	private int _tag = -1;

	public List<Integer> getAllowed()
	{
		return _allowed;
	}
		
	public void removeAllowed(int id)
	{
		_allowed.remove(_allowed.indexOf(Integer.valueOf(id)));
	}
		
	public void addAllowed(int id)
	{
		_allowed.add(id);
	}
		
	public boolean isAllowed(int id)
	{
		return _allowed.contains(id);
	}
		
	public void setReflection(Reflection reflection)
	{
		_reflection = reflection;
	}
		
	public Reflection getReflection()
	{
		return _reflection;
	}
		
	public int getReflectionId()
	{
		return _reflection.getId();
	}
		
	public void setTemplateId(int templateId)
	{
		_templateId = templateId;
	}
		
	public int getTemplateId()
	{
		return _templateId;
	}
		
	public int getStatus()
	{
		return _status.get();
	}
		
	public boolean isStatus(int status)
	{
		return _status.get() == status;
	}
		
	public void setStatus(int status)
	{
		_status.set(status);
	}
		
	public void incStatus()
	{
		_status.incrementAndGet();
	}
		
	public void setTag(int tag)
	{
		_tag = tag;
	}
		
	public int getTag()
	{
		return _tag;
	}
		
	public void setIsLocked(boolean isLocked)
	{
		_isLocked = isLocked;
	}
		
	public boolean isLocked()
	{
		return _isLocked;
	}
		
	public void onDeath(Creature killer, Creature victim)
	{
		if ((victim != null) && victim.isPlayer())
		{
			if (_reflection != null)
			{
				final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.YOU_WILL_BE_EXPELLED_IN_S1);
				sm.addNumber((_reflection.getEjectTime() / 1000));
				victim.getActingPlayer().sendPacket(sm);
				_reflection.addEjectDeadTask(victim.getActingPlayer());
			}
		}
	}
}