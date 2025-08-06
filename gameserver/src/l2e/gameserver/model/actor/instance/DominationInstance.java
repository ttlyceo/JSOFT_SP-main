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

import l2e.gameserver.ai.character.CharacterAI;
import l2e.gameserver.ai.character.DominationAI;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.entity.events.model.impl.DominationEvent;
import l2e.gameserver.model.entity.events.model.template.FightEventTeam;

public class DominationInstance extends Npc
{
	private DominationEvent _event;
	private FightEventTeam _team;
	
	public DominationInstance(int objectId, NpcTemplate template)
	{
		super(objectId, template);
		
		setInstanceType(InstanceType.DominationInstance);
		setIsInvul(true);
		setIsImmobilized(true);
	}

	@Override
	public void onAction(Player player, boolean interact, boolean shift)
	{
		player.sendActionFailed();
	}

	@Override
	public void onActionShift(Player player)
	{
		if (player == null)
		{
			return;
		}
		player.sendActionFailed();
	}
	
	@Override
	protected CharacterAI initAI()
	{
		return new DominationAI(this);
	}
	
	public void setIsInEvent(DominationEvent event)
	{
		_event = event;
	}
	
	public DominationEvent getEvent()
	{
		return _event;
	}
	
	public void setEventTeam(FightEventTeam team)
	{
		_team = team;
	}
	
	public FightEventTeam getEventTeam()
	{
		return _team;
	}
}