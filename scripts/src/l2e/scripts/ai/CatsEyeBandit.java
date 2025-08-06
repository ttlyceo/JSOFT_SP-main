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

import l2e.gameserver.ai.npc.Fighter;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.items.itemcontainer.Inventory;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.NpcSay;
import l2e.scripts.quests._403_PathToRogue;

/**
 * Created by LordWinter 19.09.2018
 */
public final class CatsEyeBandit extends Fighter
{
	public CatsEyeBandit(Attackable actor)
	{
		super(actor);
	}
	
	@Override
	protected void onEvtAttacked(Creature attacker, int damage)
	{
		final Attackable npc = getActiveChar();
		if (attacker != null && npc.isScriptValue(0) && attacker.isPlayer())
		{
			final QuestState qs = attacker.getActingPlayer().getQuestState(_403_PathToRogue.class.getSimpleName());
			if ((qs != null) && ((qs.getItemEquipped(Inventory.PAPERDOLL_RHAND) == 1181) || (qs.getItemEquipped(Inventory.PAPERDOLL_RHAND) == 1182)))
			{
				npc.broadcastPacketToOthers(1000, new NpcSay(npc, Say2.NPC_ALL, NpcStringId.YOU_CHILDISH_FOOL_DO_YOU_THINK_YOU_CAN_CATCH_ME));
				npc.setScriptValue(1);
			}
		}
		super.onEvtAttacked(attacker, damage);
	}
	
	@Override
	protected void onEvtDead(Creature killer)
	{
		final Attackable npc = getActiveChar();
		if (killer != null && killer.isPlayer())
		{
			final QuestState qs = killer.getActingPlayer().getQuestState(_403_PathToRogue.class.getSimpleName());
			if (qs != null)
			{
				npc.broadcastPacketToOthers(1000, new NpcSay(npc, Say2.NPC_ALL, NpcStringId.I_MUST_DO_SOMETHING_ABOUT_THIS_SHAMEFUL_INCIDENT));
			}
		}
		super.onEvtDead(killer);
	}
}
