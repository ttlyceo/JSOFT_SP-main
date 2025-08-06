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
package l2e.scripts.ai.hellbound;

import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.instancemanager.HellboundManager;
import l2e.gameserver.model.MinionList;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.MonsterInstance;
import l2e.gameserver.model.holders.SkillHolder;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.skills.effects.Effect;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.NpcSay;
import l2e.scripts.ai.AbstractNpcAI;

public class Amaskari extends AbstractNpcAI
{
	private static final int AMASKARI = 22449;
	private static final int AMASKARI_PRISONER = 22450;

	private static final int BUFF_ID = 4632;
	private static SkillHolder[] BUFF =
	{
	                new SkillHolder(BUFF_ID, 1), new SkillHolder(BUFF_ID, 2), new SkillHolder(BUFF_ID, 3)
	};

	private static final NpcStringId[] AMASKARI_NPCSTRING_ID =
	{
	                NpcStringId.ILL_MAKE_EVERYONE_FEEL_THE_SAME_SUFFERING_AS_ME, NpcStringId.HA_HA_YES_DIE_SLOWLY_WRITHING_IN_PAIN_AND_AGONY, NpcStringId.MORE_NEED_MORE_SEVERE_PAIN, NpcStringId.SOMETHING_IS_BURNING_INSIDE_MY_BODY
	};

	private static final NpcStringId[] MINIONS_NPCSTRING_ID =
	{
	                NpcStringId.AHH_MY_LIFE_IS_BEING_DRAINED_OUT, NpcStringId.THANK_YOU_FOR_SAVING_ME, NpcStringId.IT_WILL_KILL_EVERYONE, NpcStringId.EEEK_I_FEEL_SICKYOW
	};

	private Amaskari(String name, String descr)
	{
		super(name, descr);

		addKillId(AMASKARI, AMASKARI_PRISONER);
		addAttackId(AMASKARI);
		addSpawnId(AMASKARI_PRISONER);
	}

	@Override
	public final String onAdvEvent(String event, Npc npc, Player player)
	{
		if (event.equalsIgnoreCase("stop_toggle"))
		{
			npc.broadcastPacketToOthers(2000, new NpcSay(npc.getObjectId(), Say2.NPC_ALL, npc.getId(), AMASKARI_NPCSTRING_ID[2]));
			((MonsterInstance) npc).clearAggroList(false);
			((MonsterInstance) npc).getAI().setIntention(CtrlIntention.ACTIVE);
			npc.setIsInvul(false);
		}
		else if (event.equalsIgnoreCase("onspawn_msg") && (npc != null) && !npc.isDead())
		{
			if (getRandom(100) > 20)
			{
				npc.broadcastPacketToOthers(2000, new NpcSay(npc.getObjectId(), Say2.NPC_ALL, npc.getId(), MINIONS_NPCSTRING_ID[2]));
			}
			else if (getRandom(100) > 40)
			{
				npc.broadcastPacketToOthers(2000, new NpcSay(npc.getObjectId(), Say2.NPC_ALL, npc.getId(), MINIONS_NPCSTRING_ID[3]));
			}
			startQuestTimer("onspawn_msg", (getRandom(8) + 1) * 30000, npc, null);
		}
		return null;
	}

	@Override
	public String onAttack(Npc npc, Player attacker, int damage, boolean isSummon, Skill skill)
	{
		if ((npc.getId() == AMASKARI) && (getRandom(1000) < 25))
		{
			npc.broadcastPacketToOthers(2000, new NpcSay(npc.getObjectId(), Say2.NPC_ALL, npc.getId(), AMASKARI_NPCSTRING_ID[0]));
			final MinionList ml = npc.getMinionList();
			if (ml != null && ml.hasAliveMinions())
			{
				for (final MonsterInstance minion : ml.getAliveMinions())
				{
					if ((minion != null) && !minion.isDead() && (getRandom(10) == 0))
					{
						minion.broadcastPacketToOthers(2000, new NpcSay(minion.getObjectId(), Say2.NPC_ALL, minion.getId(), MINIONS_NPCSTRING_ID[0]));
						minion.setCurrentHp(minion.getCurrentHp() - (minion.getCurrentHp() / 5));
					}
				}
			}
		}
		return super.onAttack(npc, attacker, damage, isSummon, skill);
	}

	@Override
	public String onKill(Npc npc, Player killer, boolean isSummon)
	{
		if (npc.getId() == AMASKARI_PRISONER)
		{
			final Npc master = npc.getLeader();
			if ((master != null) && !master.isDead())
			{
				master.broadcastPacketToOthers(2000, new NpcSay(master.getObjectId(), Say2.NPC_ALL, master.getId(), AMASKARI_NPCSTRING_ID[1]));
				final Effect e = master.getFirstEffect(BUFF_ID);
				if ((e != null) && (e.getSkill().getAbnormalLvl() == 3) && master.isInvul())
				{
					master.setCurrentHp(master.getCurrentHp() + (master.getCurrentHp() / 5));
				}
				else
				{
					if (master instanceof Attackable)
					{
						((Attackable) master).clearAggroList(false);
					}
					master.getAI().setIntention(CtrlIntention.ACTIVE);
					if (e == null)
					{
						master.doCast(BUFF[0].getSkill());
					}
					else if (e != null && e.getSkill().getAbnormalLvl() > 0 && e.getSkill().getAbnormalLvl() < 3)
					{
						master.doCast(BUFF[e.getSkill().getAbnormalLvl() + 1].getSkill());
					}
					else
					{
						master.broadcastPacketToOthers(2000, new NpcSay(master.getObjectId(), Say2.NPC_ALL, master.getId(), AMASKARI_NPCSTRING_ID[3]));
						master.setIsInvul(true);
						startQuestTimer("stop_toggle", 10000, master, null);
					}
				}
			}
		}
		else if (npc.getId() == AMASKARI)
		{
			final MinionList ml = npc.getMinionList();
			if (ml != null && ml.hasAliveMinions())
			{
				for (final MonsterInstance minion : ml.getAliveMinions())
				{
					if ((minion != null) && !minion.isDead())
					{
						if (getRandom(1000) > 300)
						{
							minion.broadcastPacketToOthers(2000, new NpcSay(minion.getObjectId(), Say2.NPC_ALL, minion.getId(), MINIONS_NPCSTRING_ID[1]));
						}
						HellboundManager.getInstance().updateTrust(30, true);
						minion.deleteMe();
					}
				}
			}
		}
		return super.onKill(npc, killer, isSummon);
	}

	@Override
	public final String onSpawn(Npc npc)
	{
		if (!npc.isTeleporting())
		{
			startQuestTimer("onspawn_msg", (getRandom(3) + 1) * 30000, npc, null);
		}
		return super.onSpawn(npc);
	}

	public static void main(String[] args)
	{
		new Amaskari(Amaskari.class.getSimpleName(), "ai");
	}
}
