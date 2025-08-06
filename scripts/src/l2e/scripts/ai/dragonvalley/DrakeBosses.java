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
package l2e.scripts.ai.dragonvalley;

import java.util.Map;

import l2e.commons.util.NpcUtils;
import l2e.gameserver.Config;
import l2e.gameserver.ai.npc.Fighter;
import l2e.gameserver.model.PlayerGroup;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.npc.aggro.AggroInfo;
import l2e.gameserver.model.quest.QuestState;

/**
 * Created by LordWinter 27.12.2019
 */
public class DrakeBosses extends Fighter
{
	public DrakeBosses(Attackable actor)
	{
		super(actor);
	}

	@Override
	protected void onEvtDead(Creature killer)
	{
		Npc corpse = null;
		switch (getActiveChar().getId())
		{
			case 25725:
				corpse = NpcUtils.spawnSingleNpc(32884, getActiveChar().getLocation(), 300000);
				break;
			case 25726:
				corpse = NpcUtils.spawnSingleNpc(32885, getActiveChar().getLocation(), 300000);
				break;
			case 25727:
				corpse = NpcUtils.spawnSingleNpc(32886, getActiveChar().getLocation(), 300000);
				break;
		}

		if (killer != null && corpse != null)
		{
			final boolean isForAll = getActiveChar().getTemplate().getParameter("456QuestbyAggroList", false);
			final Player player = killer.getActingPlayer();
			if (player != null)
			{
				if (isForAll)
				{
					QuestState st;
					for (final Creature creature : getActiveChar().getAggroList().getCharMap().keySet())
					{
						if (creature != null && creature.isPlayer())
						{
							final Player pl = creature.getActingPlayer();
							if (pl != null && !pl.isDead() && (getActiveChar().isInRangeZ(pl, Config.ALT_PARTY_RANGE) || getActiveChar().isInRangeZ(killer, Config.ALT_PARTY_RANGE)))
							{
								st = pl.getQuestState("_456_DontKnowDontCare");
								if (st != null && st.isCond(1))
								{
									st.set("RaidKilled", corpse.getObjectId());
								}
							}
						}
					}
				}
				else
				{
					final PlayerGroup group = player.getPlayerGroup();
					if (group != null)
					{
						QuestState st;
						final Map<Creature, AggroInfo> aggro = getActiveChar().getAggroList().getCharMap();
						for (final Player pl : group)
						{
							if (pl != null && !pl.isDead() && aggro.containsKey(pl) && (getActiveChar().isInRangeZ(pl, Config.ALT_PARTY_RANGE) || getActiveChar().isInRangeZ(killer, Config.ALT_PARTY_RANGE)))
							{
								st = pl.getQuestState("_456_DontKnowDontCare");
								if (st != null && st.isCond(1))
								{
									st.set("RaidKilled", corpse.getObjectId());
								}
							}
						}
					}
				}
			}
		}
		super.onEvtDead(killer);
		getActiveChar().endDecayTask();
	}
}