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
package l2e.scripts.instances.KamalokaSolo;

import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;

/**
 * Rework by LordWinter 13.02.2020
 */
public class Kamaloka20_30 extends KamalokaSolo
{
	private final KamaParam param = new KamaParam();

	public Kamaloka20_30(String name, String descr)
	{
		super(name, descr);

		param.qn = "Kamaloka20_30";
		param.instanceId = 46;
		param.rewPosition = new Location(9261, -219862, -8021);
		addStartNpc(32484);
		addTalkId(32484, 32485);
		
		addKillId(22452, 22453, 22454);
	}

	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		if (event.equalsIgnoreCase("enter"))
		{
			return onEnterTo(npc, player, param);
		}
		return onAdvEventTo(event, npc, player, param.qn);
	}

	@Override
	public String onTalk(Npc npc, Player player)
	{
		if (npc.getId() == 32484)
		{
			return onEnterTo(npc, player, param);
		}
		return onTalkTo(npc, player, param.qn);
	}

	@Override
	public String onKill(Npc npc, Player player, boolean isPet)
	{
		return onKillTo(npc, player, isPet, param.qn, 22452, new int[]
		{
		        22453, 22454
		});
	}

	public static void main(String[] args)
	{
		new Kamaloka20_30(Kamaloka20_30.class.getSimpleName(), "Kamaloka20_30");
	}
}