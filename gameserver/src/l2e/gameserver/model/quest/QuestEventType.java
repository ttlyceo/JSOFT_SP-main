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
package l2e.gameserver.model.quest;

public enum QuestEventType
{
	ON_FIRST_TALK(false),
	QUEST_START(true),
	ON_TALK(true),
	ON_ATTACK(true),
	ON_KILL(true),
	ON_SPAWN(true),
	ON_SKILL_SEE(true),
	ON_FACTION_CALL(true),
	ON_AGGRO_RANGE_ENTER(true),
	ON_SPELL_FINISHED(true),
	ON_SKILL_LEARN(false),
	ON_ENTER_ZONE(true),
	ON_EXIT_ZONE(true),
	ON_TRAP_ACTION(true),
	ON_ITEM_USE(true),
	ON_NODE_ARRIVED(true),
	ON_EVENT_RECEIVED(true),
	ON_MOVE_FINISHED(true),
	ON_SEE_CREATURE(true),
	ON_ROUTE_FINISHED(true),
	ON_CAN_SEE_ME(false);
		
	private boolean _allowMultipleRegistration;
		
	private QuestEventType(boolean allowMultipleRegistration)
	{
		_allowMultipleRegistration = allowMultipleRegistration;
	}
		
	public boolean isMultipleRegistrationAllowed()
	{
		return _allowMultipleRegistration;
	}
}