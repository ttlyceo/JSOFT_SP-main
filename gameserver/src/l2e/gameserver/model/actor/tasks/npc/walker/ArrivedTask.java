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
package l2e.gameserver.model.actor.tasks.npc.walker;

import l2e.gameserver.instancemanager.WalkingManager;
import l2e.gameserver.model.WalkInfo;
import l2e.gameserver.model.actor.Npc;

public class ArrivedTask implements Runnable
{
	private final WalkInfo _walk;
	private final Npc _npc;
	
	public ArrivedTask(Npc npc, WalkInfo walk)
	{
		_npc = npc;
		_walk = walk;
	}
	
	@Override
	public void run()
	{
		_walk.setBlocked(false);
		WalkingManager.getInstance().startMoving(_npc, _walk.getRoute().getName());
	}
}