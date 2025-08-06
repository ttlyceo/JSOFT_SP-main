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
package l2e.scripts.ai.isle_of_prayer;

import l2e.commons.util.Rnd;
import l2e.gameserver.ai.model.CtrlEvent;
import l2e.gameserver.ai.npc.Fighter;
import l2e.gameserver.data.parser.NpcsParser;
import l2e.gameserver.idfactory.IdFactory;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.Party;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.MonsterInstance;

/**
 * Created by LordWinter 21.09.2018
 */
public class IsleOfPrayerFighter extends Fighter
{
	private static final int PENALTY_MOBS[] =
	{
	        18364, 18365, 18366
	};

	public IsleOfPrayerFighter(Attackable actor)
	{
		super(actor);
	}

	@Override
	protected void onEvtAttacked(Creature attacker, int damage)
	{
		final Attackable actor = getActiveChar();
		if (attacker != null && actor.isScriptValue(0) && attacker.isPlayer())
		{
			final Party party = attacker.getActingPlayer().getParty();
			if ((party != null) && (party.getMemberCount() > 2))
			{
				actor.setScriptValue(1);
				for (int i = 0; i < 2; i++)
				{
					final MonsterInstance npc = new MonsterInstance(IdFactory.getInstance().getNextId(), NpcsParser.getInstance().getTemplate(PENALTY_MOBS[Rnd.get(PENALTY_MOBS.length)]));
					final Location loc = ((MonsterInstance) actor).getMinionPosition();
					npc.setReflection(actor.getReflection());
					npc.setHeading(actor.getHeading());
					npc.setCurrentHpMp(npc.getMaxHp(), npc.getMaxMp());
					npc.spawnMe(loc.getX(), loc.getY(), loc.getZ());
					npc.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, attacker, Rnd.get(1, 100));
				}
			}
		}

		super.onEvtAttacked(attacker, damage);
	}

	@Override
	protected void onEvtDead(Creature killer)
	{
		if (killer != null)
		{
			final Player player = killer.getActingPlayer();
			if (player != null)
			{
				final Attackable actor = getActiveChar();
				switch (actor.getId())
				{
					case 22259 :
						if (Rnd.chance(26))
						{
							actor.dropSingleItem(player, 9593, 1);
						}
						break;
					case 22263 :
						if (Rnd.chance(14))
						{
							actor.dropSingleItem(player, 9594, 1);
						}
						break;
				}
			}
		}
		super.onEvtDead(killer);
	}
}
