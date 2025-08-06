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
package l2e.scripts.hellbound;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import l2e.gameserver.data.parser.DoorParser;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.base.ClassId;
import l2e.gameserver.model.holders.SkillHolder;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.skills.effects.Effect;

public class BaseTower extends Quest
{
	private static final int GUZEN = 22362;
	private static final int KENDAL = 32301;
	private static final int BODY_DESTROYER = 22363;

	private static final Map<Integer, Player> BODY_DESTROYER_TARGET_LIST = new ConcurrentHashMap<>();

	private static final SkillHolder DEATH_WORD = new SkillHolder(5256, 1);

	public BaseTower(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addKillId(GUZEN);
		addKillId(BODY_DESTROYER);
		addFirstTalkId(KENDAL);
		addAggroRangeEnterId(BODY_DESTROYER);
	}

	@Override
	public final String onFirstTalk(Npc npc, Player player)
	{
		final ClassId classId = player.getClassId();
		if (classId.equalsOrChildOf(ClassId.hellKnight) || classId.equalsOrChildOf(ClassId.soultaker))
		{
			return "32301-02.htm";
		}
		return "32301-01.htm";
	}

	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		if (event.equalsIgnoreCase("close"))
		{
			DoorParser.getInstance().getDoor(20260004).closeMe();
		}
		return null;
	}

	@Override
	public String onAggroRangeEnter(Npc npc, Player player, boolean isSummon)
	{
		if (!BODY_DESTROYER_TARGET_LIST.containsKey(npc.getObjectId()))
		{
			BODY_DESTROYER_TARGET_LIST.put(npc.getObjectId(), player);
			npc.setTarget(player);
			npc.doSimultaneousCast(DEATH_WORD.getSkill());
		}
		return super.onAggroRangeEnter(npc, player, isSummon);
	}

	@Override
	public String onKill(Npc npc, Player killer, boolean isSummon)
	{
		switch (npc.getId())
		{
			case GUZEN:
				addSpawn(KENDAL, npc.getSpawn().getX(), npc.getSpawn().getY(), npc.getSpawn().getZ(), 0, false, npc.getSpawn().getRespawnDelay(), false);
				DoorParser.getInstance().getDoor(20260003).openMe();
				DoorParser.getInstance().getDoor(20260004).openMe();
				startQuestTimer("close", 60000, npc, null, false);
				break;
			case BODY_DESTROYER:
				if (BODY_DESTROYER_TARGET_LIST.containsKey(npc.getObjectId()))
				{
					final Player pl = BODY_DESTROYER_TARGET_LIST.get(npc.getObjectId());
					if ((pl != null) && pl.isOnline() && !pl.isDead())
					{
						final Effect e = pl.getFirstEffect(DEATH_WORD.getSkill());
						if (e != null)
						{
							e.exit();
						}
					}

					BODY_DESTROYER_TARGET_LIST.remove(npc.getObjectId());
				}
		}
		return super.onKill(npc, killer, isSummon);
	}

	public static void main(String[] args)
	{
		new BaseTower(-1, BaseTower.class.getSimpleName(), "hellbound");
	}
}
