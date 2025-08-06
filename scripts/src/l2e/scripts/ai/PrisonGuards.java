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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.quest.QuestState;
import l2e.scripts.custom.IOPRace;

public class PrisonGuards extends AbstractNpcAI
{
	final private static String[] GUARDVARS =
	{
	        "1st", "2nd", "3rd", "4th"
	};

	private final Map<Npc, Integer> _guards = new ConcurrentHashMap<>();

	private PrisonGuards(String name, String descr)
	{
		super(name, descr);

		final int[] mob =
		{
		        18367, 18368
		};
		registerMobs(mob);

		// place 1
		_guards.put(addSpawn(18368, 160704, 184704, -3704, 49152, false, 0), 0);
		_guards.put(addSpawn(18368, 160384, 184704, -3704, 49152, false, 0), 0);
		_guards.put(addSpawn(18367, 160528, 185216, -3704, 49152, false, 0), 0);
		// place 2
		_guards.put(addSpawn(18368, 135120, 171856, -3704, 49152, false, 0), 1);
		_guards.put(addSpawn(18368, 134768, 171856, -3704, 49152, false, 0), 1);
		_guards.put(addSpawn(18367, 134928, 172432, -3704, 49152, false, 0), 1);
		// place 3
		_guards.put(addSpawn(18368, 146880, 151504, -2872, 49152, false, 0), 2);
		_guards.put(addSpawn(18368, 146366, 151506, -2872, 49152, false, 0), 2);
		_guards.put(addSpawn(18367, 146592, 151888, -2872, 49152, false, 0), 2);
		// place 4
		_guards.put(addSpawn(18368, 155840, 160448, -3352, 0, false, 0), 3);
		_guards.put(addSpawn(18368, 155840, 159936, -3352, 0, false, 0), 3);
		_guards.put(addSpawn(18367, 155578, 160177, -3352, 0, false, 0), 3);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		if (event.equalsIgnoreCase("Respawn"))
		{
			final Npc newGuard = addSpawn(npc.getId(), npc.getSpawn().getX(), npc.getSpawn().getY(), npc.getSpawn().getZ(), npc.getSpawn().getHeading(), false, 0);
			final int place = _guards.get(npc);
			_guards.remove(npc);
			_guards.put(newGuard, place);
		}
		return null;
	}

	@Override
	public String onKill(Npc npc, Player player, boolean isSummon)
	{
		if (_guards.containsKey(npc))
		{
			startQuestTimer("Respawn", 20000, npc, null);
		}
		
		if (npc.getId() == 18367)
		{
			final QuestState qs = player.getQuestState(IOPRace.class.getSimpleName());
			if ((qs != null) && (qs.getInt(GUARDVARS[_guards.get(npc)]) != 1))
			{
				qs.set(GUARDVARS[_guards.get(npc)], "1");
				qs.giveItems(10013, 1);
			}
		}
		return super.onKill(npc, player, isSummon);
	}

	public static void main(String[] args)
	{
		new PrisonGuards(PrisonGuards.class.getSimpleName(), "ai");
	}
}
