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
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.NpcSay;

/**
 * Created by LordWinter 22.11.2018
 */
public class CaveAntLarva extends Fighter
{
	private static final NpcStringId[] MSG1 =
	{
	        NpcStringId.ENOUGH_FOOLING_AROUND_GET_READY_TO_DIE, NpcStringId.YOU_IDIOT_IVE_JUST_BEEN_TOYING_WITH_YOU, NpcStringId.NOW_THE_FUN_STARTS
	};

	private static final NpcStringId[] MSG2 =
	{
	        NpcStringId.I_MUST_ADMIT_NO_ONE_MAKES_MY_BLOOD_BOIL_QUITE_LIKE_YOU_DO, NpcStringId.NOW_THE_BATTLE_BEGINS, NpcStringId.WITNESS_MY_TRUE_POWER
	};

	private static final NpcStringId[] MSG3 =
	{
	        NpcStringId.PREPARE_TO_DIE, NpcStringId.ILL_DOUBLE_MY_STRENGTH, NpcStringId.YOU_HAVE_MORE_SKILL_THAN_I_THOUGHT
	};

	public CaveAntLarva(Attackable actor)
	{
		super(actor);
	}

	@Override
	protected void onEvtAttacked(Creature attacker, int damage)
	{
		final Attackable actor = getActiveChar();

		int transformId = 0;
		int chance = 0;
		int hp = 0;
		NpcStringId msg = null;
		if (actor.getId() == 21265)
		{
			transformId = 21271;
			chance = 30;
			hp = 100;
			msg = MSG1[Rnd.get(MSG1.length)];
		}
		else if (actor.getId() == 21271)
		{
			transformId = 21272;
			chance = 10;
			hp = 60;
			msg = MSG2[Rnd.get(MSG2.length)];
		}
		else
		{
			transformId = 21273;
			chance = 5;
			hp = 30;
			msg = MSG2[Rnd.get(MSG3.length)];
		}

		if (attacker != null && actor.isScriptValue(0) && (actor.getCurrentHp() <= ((actor.getMaxHp() * hp) / 100.0)) && Rnd.chance(chance))
		{
			actor.setScriptValue(1);
			actor.broadcastPacketToOthers(2000, new NpcSay(actor.getObjectId(), Say2.NPC_ALL, actor.getId(), msg));
			actor.decayMe();
			final Attackable npc = (Attackable) Quest.addSpawn(transformId, actor.getLocation(), actor.getReflection());
			npc.setRunning();
			npc.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, attacker, 100);
			attacker.setTarget(npc);
		}
		super.onEvtAttacked(attacker, damage);
	}
}
