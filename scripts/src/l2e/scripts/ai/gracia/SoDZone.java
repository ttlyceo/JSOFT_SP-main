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
package l2e.scripts.ai.gracia;

import l2e.gameserver.instancemanager.SoDManager;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.zone.ZoneType;
import l2e.scripts.ai.AbstractNpcAI;

public class SoDZone extends AbstractNpcAI
{
	private SoDZone(String name, String descr)
	{
		super(name, descr);

		addEnterZoneId(60009);
	}

	@Override
	public String onEnterZone(Creature character, ZoneType zone)
	{
		if (character.getReflectionId() != 0)
		{
			return super.onEnterZone(character, zone);
		}

		if (character.isPlayer() && zone.getId() == 60009)
		{
			if (!SoDManager.getInstance().isOpened())
			{
				character.teleToLocation(-248717, 250260, 4337, true, character.getReflection());
			}
		}
		return super.onEnterZone(character, zone);
	}

	public static void main(String[] args)
	{
		new SoDZone(SoDZone.class.getSimpleName(), "ai");
	}
}
