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
package l2e.scripts.quests;

import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.model.CategoryType;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.quest.State;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.NpcSay;
import l2e.gameserver.network.serverpackets.SocialAction;

/**
 * Rework by LordWinter 15.02.2020
 */
public final class _065_CertifiedSoulBreaker extends Quest
{
	private static boolean _isSpawnedAngel = false;
	private static boolean _isSpawnedKatenar = false;
	private Npc _angel;
	private Npc _katenar;

	public _065_CertifiedSoulBreaker(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(32213);
		addTalkId(30071, 30073, 30074, 30075, 30076, 30123, 30124, 30879, 32138, 32139, 32199, 32213, 32214, 32242, 32243, 32244);
		addFirstTalkId(32242);

		addSpawnId(32244);
		
		addKillId(20176, 27332);

		questItemIds = new int[]
		{
		        9803, 9804, 9805
		};
	}

	@Override
	public final String onAdvEvent(String event, Npc npc, Player player)
	{
		if ("DESPAWN_5".equals(event))
		{
			if (npc != null)
			{
				npc.deleteMe();
			}
			return super.onAdvEvent(event, npc, player);
		}
		
		final String htmltext = event;

		final QuestState st = player.getQuestState(getName());
		if (st == null || st.isCompleted())
		{
			return htmltext;
		}

		if (event.equalsIgnoreCase("32213-03.htm"))
		{
			st.startQuest();
			if (player.getVarInt("2ND_CLASS_DIAMOND_REWARD", 0) == 0)
			{
				giveItems(player, 7562, 47);
				player.setVar("2ND_CLASS_DIAMOND_REWARD", 1);
			}
		}
		else if (event.equalsIgnoreCase("32138-03.htm"))
		{
			st.setCond(2, true);
		}
		else if (event.equalsIgnoreCase("32139-02.htm"))
		{
			st.setCond(3, true);
		}
		else if (event.equalsIgnoreCase("32139-04.htm"))
		{
			st.setCond(4, true);
		}
		else if (event.equalsIgnoreCase("32199-02.htm"))
		{
			st.setCond(5, true);
			st.addSpawn(32244, 16489, 146249, -3112, 0);
		}
		else if (event.equalsIgnoreCase("30071-02.htm"))
		{
			st.setCond(8, true);
		}
		else if (event.equalsIgnoreCase("32214-02.htm"))
		{
			st.setCond(11, true);
		}
		else if (event.equalsIgnoreCase("30879-03.htm"))
		{
			st.set("angel", "0");
			st.setCond(12, true);
		}
		else if (event.equalsIgnoreCase("angel_cleanup"))
		{
			_isSpawnedAngel = false;
		}
		else if (event.equalsIgnoreCase("katenar_cleanup"))
		{
			_isSpawnedKatenar = false;
		}
		else if (event.equalsIgnoreCase("32139-08.htm"))
		{
			st.takeItems(9803, 1);
			st.setCond(14, true);
		}
		else if (event.equalsIgnoreCase("32138-06.htm"))
		{
			if (st.isCond(14))
			{
				st.setCond(15, true);
			}
		}
		else if (event.equalsIgnoreCase("32138-11.htm"))
		{
			if (st.isCond(16))
			{
				st.takeItems(9804, -1);
				st.giveItems(9805, 1);
				st.setCond(17, true);
			}
		}
		return htmltext;
	}
	
	@Override
	public String onSpawn(Npc npc)
	{
		if (npc.getId() == 32244)
		{
			startQuestTimer("DESPAWN_5", 5000, npc, null);
			npc.broadcastPacketToOthers(2000, new NpcSay(npc, Say2.NPC_ALL, NpcStringId.DRATS_HOW_COULD_I_BE_SO_WRONG));
			npc.setIsRunning(true);
			npc.getAI().setIntention(CtrlIntention.MOVING, new Location(16490, 145839, -3080), 0);
		}
		return super.onSpawn(npc);
	}

	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		final QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			return null;
		}

		if ((npc.getId() == 32242) && st.isCond(12))
		{
			st.unset("angel");
			st.setCond(13, true);
			_isSpawnedAngel = false;
			_isSpawnedKatenar = false;
			st.giveItems(9803, 1);
			return "32242-01.htm";
		}
		return null;
	}

	@Override
	public final String onTalk(Npc npc, Player player)
	{
		String htmltext = getNoQuestMsg(player);
		final QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			return htmltext;
		}

		final int npcId = npc.getId();
		final int cond = st.getCond();

		if (st.getState() == State.COMPLETED)
		{
			htmltext = getAlreadyCompletedMsg(player);
		}
		else if (npcId == 32213)
		{
			if (st.getState() == State.CREATED)
			{
				if ((player.getLevel() < getMinLvl(getId())) && !player.isInCategory(CategoryType.KAMAEL_SECOND_CLASS_GROUP))
				{
					htmltext = "32213-00.htm";
					st.exitQuest(true);
				}
				else
				{
					htmltext = "32213-01.htm";
				}
			}
			else if ((cond >= 1) && (cond <= 3))
			{
				htmltext = "32213-04.htm";
			}
			else if ((cond >= 4) && (cond < 17))
			{
				htmltext = "32213-05.htm";
			}
			else if ((cond == 17) && (st.getQuestItemsCount(9805) == 1))
			{
				htmltext = "32213-06.htm";
				st.takeItems(9805, -1);
				st.calcExpAndSp(getId());
				st.calcReward(getId());
				st.giveItems(9806, 1);
				player.sendPacket(new SocialAction(player.getObjectId(), 3));
				st.exitQuest(false, true);
			}
		}
		else if (npcId == 32138)
		{
			if (cond == 1)
			{
				htmltext = "32138-00.htm";
			}
			else if (cond == 2)
			{
				htmltext = "32138-04.htm";
			}
			else if (cond == 14)
			{
				htmltext = "32138-05.htm";
			}
			else if (cond == 15)
			{
				htmltext = "32138-07.htm";
			}
			else if (cond == 16)
			{
				htmltext = "32138-08.htm";
			}
			else if (cond == 17)
			{
				htmltext = "32138-12.htm";
			}
		}
		else if (npcId == 32139)
		{
			if (cond == 2)
			{
				htmltext = "32139-01.htm";
			}
			else if (cond == 3)
			{
				htmltext = "32139-03.htm";
			}
			else if (cond == 4)
			{
				htmltext = "32139-05.htm";
			}
			else if (cond == 13)
			{
				htmltext = "32139-06.htm";
			}
			else if (cond == 14)
			{
				htmltext = "32139-09.htm";
			}
		}
		else if (npcId == 32199)
		{
			if (cond == 4)
			{
				htmltext = "32199-01.htm";
			}
			else if (cond == 5)
			{
				htmltext = "32199-03.htm";
				st.setCond(6, true);
			}
			else if (cond == 6)
			{
				htmltext = "32199-04.htm";
			}
		}
		else if (npcId == 30074)
		{
			if (cond == 6)
			{
				htmltext = "30074-01.htm";
			}
			else if (cond == 7)
			{
				htmltext = "30074-02.htm";
			}
		}
		else if (npcId == 30073)
		{
			if (cond == 6)
			{
				htmltext = "30073-01.htm";
				st.setCond(7, true);
			}
			else if (cond == 7)
			{
				htmltext = "30073-02.htm";
			}
		}
		else if (npcId == 30071)
		{
			if (cond == 7)
			{
				htmltext = "30071-01.htm";
			}
			else if (cond == 8)
			{
				htmltext = "30071-03.htm";
			}
		}
		else if (npcId == 30075)
		{
			if (cond == 8)
			{
				htmltext = "30075-01.htm";
			}
			else if (cond == 9)
			{
				htmltext = "30075-02.htm";
			}
		}
		else if (npcId == 30076)
		{
			if (cond == 8)
			{
				htmltext = "30076-01.htm";
				st.setCond(9, true);
			}
			else if (cond == 9)
			{
				htmltext = "30076-02.htm";
			}
		}
		else if (npcId == 30124)
		{
			if (cond == 9)
			{
				htmltext = "30124-01.htm";
			}
			else if (cond == 10)
			{
				htmltext = "30124-02.htm";
			}
		}
		else if (npcId == 30123)
		{
			if (cond == 9)
			{
				htmltext = "30123-01.htm";
				st.setCond(10, true);
			}
			else if (cond == 10)
			{
				htmltext = "30123-02.htm";
			}
		}
		else if (npcId == 32214)
		{
			if (cond == 10)
			{
				htmltext = "32214-01.htm";
			}
			else if (cond == 11)
			{
				htmltext = "32214-03.htm";
			}
		}
		else if (npcId == 30879)
		{
			if (cond == 11)
			{
				htmltext = "30879-01.htm";
			}
			else if (cond == 12)
			{
				htmltext = "30879-04.htm";
			}
		}
		else if (npcId == 32243)
		{
			if (cond == 12)
			{
				htmltext = "32243-01.htm";
				if ((st.getInt("angel") == 0) && (_isSpawnedAngel == false))
				{
					_angel = st.addSpawn(27332, 36198, 191949, -3728, 180000);
					_angel.broadcastPacketToOthers(2000, new NpcSay(npc, Say2.NPC_ALL, NpcStringId.S1_STEP_BACK_FROM_THE_CONFOUNDED_BOX_I_WILL_TAKE_IT_MYSELF).addStringParameter(player.getName(null)));
					_angel.setRunning();
					((Attackable) _angel).addDamageHate(player, 0, 99999);
					_angel.getAI().setIntention(CtrlIntention.ATTACK, player, true);
					_isSpawnedAngel = true;
					startQuestTimer("angel_cleanup", 180000, _angel, player);
				}
				else if ((st.getInt("angel") == 1) && (_isSpawnedKatenar == false))
				{
					_katenar = st.addSpawn(32242, 36110, 191921, -3712, 60000);
					_isSpawnedKatenar = true;
					startQuestTimer("katenar_cleanup", 60000, _katenar, player);
					htmltext = "32243-02.htm";
				}
			}
			else if (cond == 13)
			{
				htmltext = "32243-03.htm";
			}
		}
		return htmltext;
	}

	@Override
	public String onKill(Npc npc, Player player, boolean isSummon)
	{
		final QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			return super.onKill(npc, player, isSummon);
		}

		if ((npc.getId() == 27332) && st.isCond(12))
		{
			st.set("angel", "1");
			_isSpawnedAngel = false;
			npc.broadcastPacketToOthers(2000, new NpcSay(npc, Say2.NPC_ALL, NpcStringId.GRR_IVE_BEEN_HIT));
			if (_isSpawnedKatenar == false)
			{
				_katenar = st.addSpawn(32242, 36110, 191921, -3712, 60000);
				_isSpawnedKatenar = true;
				startQuestTimer("katenar_cleanup", 60000, _katenar, player);
			}
		}
		if ((npc.getId() == 20176) && st.isCond(15))
		{
			if (st.calcDropItems(getId(), 9804, npc.getId(), 10))
			{
				st.setCond(16, true);
			}
		}
		return super.onKill(npc, player, isSummon);
	}

	public static void main(String[] args)
	{
		new _065_CertifiedSoulBreaker(65, _065_CertifiedSoulBreaker.class.getSimpleName(), "");
	}
}