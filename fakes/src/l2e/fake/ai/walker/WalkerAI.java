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
package l2e.fake.ai.walker;

import java.util.LinkedList;
import java.util.Queue;

import l2e.commons.util.Rnd;
import l2e.fake.FakePlayer;
import l2e.fake.ai.FakePlayerAI;
import l2e.fake.model.WalkNode;
import l2e.fake.model.WalkerType;
import l2e.gameserver.ai.model.CtrlIntention;

public abstract class WalkerAI extends FakePlayerAI
{
	protected Queue<WalkNode> _walkNodes;
	private WalkNode _currentWalkNode;
	private int currentStayIterations = 0;
	protected boolean isWalking = false;
	
	public WalkerAI(FakePlayer character)
	{
		super(character, false);
	}
	
	public Queue<WalkNode> getWalkNodes()
	{
		return _walkNodes;
	}
	
	protected void addWalkNode(WalkNode walkNode)
	{
		_walkNodes.add(walkNode);
	}
	
	@Override
	public void setup()
	{
		super.setup();
		_walkNodes = new LinkedList<>();
		setWalkNodes();
	}
	
	@Override
	public void thinkAndAct()
	{
		setBusyThinking(true);
		handleDeath();
		
		if (_walkNodes.isEmpty())
		{
			return;
		}
		
		if (isWalking)
		{
			if (userReachedDestination(_currentWalkNode))
			{
				if (currentStayIterations < _currentWalkNode.getStayIterations())
				{
					currentStayIterations++;
					setBusyThinking(false);
					return;
				}
				_currentWalkNode = null;
				currentStayIterations = 0;
				isWalking = false;
			}
		}
		
		if (!isWalking && _currentWalkNode == null)
		{
			switch (getWalkerType())
			{
				case RANDOM :
					_currentWalkNode = (WalkNode) getWalkNodes().toArray()[Rnd.get(0, getWalkNodes().size() - 1)];
					break;
				case LINEAR :
					_currentWalkNode = getWalkNodes().poll();
					_walkNodes.add(_currentWalkNode);
					break;
			}
			_fakePlayer.getAI().setIntention(CtrlIntention.MOVING, _currentWalkNode.getLocation(), 0);
			isWalking = true;
		}
		
		setBusyThinking(false);
	}

	@Override
	protected int[][] getBuffs()
	{
		return new int[0][0];
	}

	protected boolean userReachedDestination(WalkNode targetWalkNode)
	{
		if (_fakePlayer.getX() == targetWalkNode.getX() && _fakePlayer.getY() == targetWalkNode.getY() && _fakePlayer.getZ() == targetWalkNode.getZ())
		{
			return true;
		}
		return false;
	}
	
	protected abstract WalkerType getWalkerType();
	protected abstract void setWalkNodes();
}
