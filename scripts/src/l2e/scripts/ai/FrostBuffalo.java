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
package l2e.scripts.ai;

import l2e.commons.util.Rnd;
import l2e.gameserver.ai.model.CtrlEvent;
import l2e.gameserver.ai.npc.Fighter;
import l2e.gameserver.data.parser.NpcsParser;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.spawn.Spawner;

/**
 * Created by LordWinter 22.11.2018
 */
public class FrostBuffalo extends Fighter
{
	public FrostBuffalo(Attackable actor)
	{
		super(actor);
	}
	
	@Override
	protected void onEvtSeeSpell(Skill skill, Creature caster)
	{
		final var actor = getActiveChar();
		if (skill.isMagic() || actor == null || caster == null)
		{
			return;
		}

		if (actor.isScriptValue(0))
		{
			actor.setScriptValue(1);
			for (int i = 0; i < 4; i++)
			{
				try
				{
					final var tpl = NpcsParser.getInstance().getTemplate(22093);
					if (tpl != null)
					{
						final var sp = new Spawner(tpl);
						final var loc = Location.findPointToStay(actor, 100, 120, false);
						if (loc != null)
						{
							sp.setLocation(loc);
							final var npc = sp.doSpawn(true, 0);
							if (caster.isPet() || caster.isSummon())
							{
								npc.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, caster, Rnd.get(2, 100));
							}
							npc.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, caster.getActingPlayer(), Rnd.get(1, 100));
						}
					}
				}
				catch (final Exception e)
				{
				}
			}
		}
	}
}