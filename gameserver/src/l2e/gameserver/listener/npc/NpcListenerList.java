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
package l2e.gameserver.listener.npc;

import l2e.commons.listener.Listener;
import l2e.gameserver.listener.actor.CharListenerList;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;

public class NpcListenerList extends CharListenerList
{
	public NpcListenerList(Npc actor)
	{
		super(actor);
	}

	@Override
	public Npc getActor()
	{
		return (Npc) _actor;
	}

	public void onSpawn()
	{
		if (!_global.getListeners().isEmpty())
		{
			for (final Listener<Creature> listener : _global.getListeners())
			{
				if(OnSpawnListener.class.isInstance(listener))
				{
					((OnSpawnListener) listener).onSpawn(getActor());
				}
			}
		}

		if(!getListeners().isEmpty())
		{
			for(final Listener<Creature> listener : getListeners())
			{
				if(OnSpawnListener.class.isInstance(listener))
				{
					((OnSpawnListener) listener).onSpawn(getActor());
				}
			}
		}
	}

	public void onShowChat()
	{
		if (!_global.getListeners().isEmpty())
		{
			for (final Listener<Creature> listener : _global.getListeners())
			{
				if(OnShowChatListener.class.isInstance(listener))
				{
					((OnShowChatListener) listener).onShowChat(getActor());
				}
			}
		}

		if( !getListeners().isEmpty())
		{
			for(final Listener<Creature> listener : getListeners())
			{
				if(OnShowChatListener.class.isInstance(listener))
				{
					((OnShowChatListener) listener).onShowChat(getActor());
				}
			}
		}
	}
	
	public void onDecay()
	{
		if (!_global.getListeners().isEmpty())
		{
			for (final Listener<Creature> listener : _global.getListeners())
			{
				if(OnDecayListener.class.isInstance(listener))
				{
					((OnDecayListener) listener).onDecay(getActor());
				}
			}
		}

		if(!getListeners().isEmpty())
		{
			for(final Listener<Creature> listener : getListeners())
			{
				if(OnDecayListener.class.isInstance(listener))
				{
					((OnDecayListener) listener).onDecay(getActor());
				}
			}
		}
	}
}
