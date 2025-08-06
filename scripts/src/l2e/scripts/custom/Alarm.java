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
package l2e.scripts.custom;

import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.scripts.ai.AbstractNpcAI;

public final class Alarm extends AbstractNpcAI
{
	public Alarm(String name, String descr)
	{
		super(name, descr);
		
		addStartNpc(32367);
		addTalkId(32367);
		addFirstTalkId(32367);
		addSpawnId(32367);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		final Player player0 = npc.getVariables().getObject("player0", Player.class);
		final Npc npc0 = npc.getVariables().getObject("npc0", Npc.class);
		switch (event)
		{
			case "SELF_DESTRUCT_IN_60":
			{
				startQuestTimer("SELF_DESTRUCT_IN_30", 30000, npc, null);
				broadcastNpcSay(npc, Say2.NPC_ALL, NpcStringId.THE_ALARM_WILL_SELF_DESTRUCT_IN_60_SECONDS_ENTER_PASSCODE_TO_OVERRIDE);
				break;
			}
			case "SELF_DESTRUCT_IN_30":
			{
				startQuestTimer("SELF_DESTRUCT_IN_10", 20000, npc, null);
				broadcastNpcSay(npc, Say2.NPC_ALL, NpcStringId.THE_ALARM_WILL_SELF_DESTRUCT_IN_30_SECONDS_ENTER_PASSCODE_TO_OVERRIDE);
				break;
			}
			case "SELF_DESTRUCT_IN_10":
			{
				startQuestTimer("RECORDER_CRUSHED", 10000, npc, null);
				broadcastNpcSay(npc, Say2.NPC_ALL, NpcStringId.THE_ALARM_WILL_SELF_DESTRUCT_IN_10_SECONDS_ENTER_PASSCODE_TO_OVERRIDE);
				break;
			}
			case "RECORDER_CRUSHED":
			{
				if (npc0 != null)
				{
					if (npc0.getVariables().getBool("SPAWNED"))
					{
						npc0.getVariables().set("SPAWNED", false);
						if (player0 != null)
						{
							broadcastNpcSay(npc, Say2.NPC_ALL, NpcStringId.RECORDER_CRUSHED);
							if (verifyMemoState(player0, 184, -1))
							{
								setMemoState(player0, 184, 5);
							}
							else if (verifyMemoState(player0, 185, -1))
							{
								setMemoState(player0, 185, 5);
							}
						}
					}
				}
				npc.deleteMe();
				break;
			}
			case "32367-184_04.htm" :
			case "32367-184_06.htm" :
			case "32367-184_08.htm" :
			{
				setUseSwitch(false);
				htmltext = event;
				break;
			}
			case "2":
			{
				if (player0 == player)
				{
					if (verifyMemoState(player, 184, 3))
					{
						setUseSwitch(false);
						htmltext = "32367-184_02.htm";
					}
					else if (verifyMemoState(player, 185, 3))
					{
						setUseSwitch(false);
						htmltext = "32367-185_02.htm";
					}
				}
				break;
			}
			case "3":
			{
				if (verifyMemoState(player, 184, 3))
				{
					setMemoStateEx(player, 184, 1, 1);
					setUseSwitch(false);
					htmltext = "32367-184_04.htm";
				}
				else if (verifyMemoState(player, 185, 3))
				{
					setMemoStateEx(player, 185, 1, 1);
					setUseSwitch(false);
					htmltext = "32367-185_04.htm";
				}
				break;
			}
			case "4":
			{
				if (verifyMemoState(player, 184, 3))
				{
					setMemoStateEx(player, 184, 1, getMemoStateEx(player, 184, 1) + 1);
					setUseSwitch(false);
					htmltext = "32367-184_06.htm";
				}
				else if (verifyMemoState(player, 185, 3))
				{
					setMemoStateEx(player, 185, 1, getMemoStateEx(player, 185, 1) + 1);
					setUseSwitch(false);
					htmltext = "32367-185_06.htm";
				}
				break;
			}
			case "5":
			{
				if (verifyMemoState(player, 184, 3))
				{
					setMemoStateEx(player, 184, 1, getMemoStateEx(player, 184, 1) + 1);
					setUseSwitch(false);
					htmltext = "32367-184_08.htm";
				}
				else if (verifyMemoState(player, 185, 3))
				{
					setMemoStateEx(player, 185, 1, getMemoStateEx(player, 185, 1) + 1);
					setUseSwitch(false);
					htmltext = "32367-185_08.htm";
				}
				break;
			}
			case "6":
			{
				if (verifyMemoState(player, 184, 3))
				{
					final int i0 = getMemoStateEx(player, 184, 1);
					if (i0 >= 3)
					{
						if ((npc0 != null) && npc0.getVariables().getBool("SPAWNED"))
						{
							npc0.getVariables().set("SPAWNED", false);
						}
						npc.deleteMe();
						setMemoState(player, 184, 4);
						htmltext = "32367-184_09.htm";
					}
					else
					{
						setMemoStateEx(player, 184, 1, 0);
						htmltext = "32367-184_10.htm";
					}
				}
				else if (verifyMemoState(player, 185, 3))
				{
					final int i0 = getMemoStateEx(player, 185, 1);
					if (i0 >= 3)
					{
						if ((npc0 != null) && npc0.getVariables().getBool("SPAWNED"))
						{
							npc0.getVariables().set("SPAWNED", false);
						}
						
						npc.deleteMe();
						setMemoState(player, 185, 4);
						htmltext = "32367-185_09.htm";
					}
					else
					{
						setMemoStateEx(player, 185, 1, 0);
						htmltext = "32367-185_10.htm";
					}
				}
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player talker)
	{
		String htmltext = getNoQuestMsg(talker);
		if (verifyMemoState(talker, 184, 3) || verifyMemoState(talker, 185, 3))
		{
			final Player player = npc.getVariables().getObject("player0", Player.class);
			if (player == talker)
			{
				htmltext = "32367-01.htm";
			}
			else
			{
				htmltext = "32367-02.htm";
			}
		}
		return htmltext;
	}
	
	@Override
	public String onSpawn(Npc npc)
	{
		startQuestTimer("SELF_DESTRUCT_IN_60", 60000, npc, null);
		broadcastNpcSay(npc, Say2.NPC_ALL, NpcStringId.INTRUDER_ALERT_THE_ALARM_WILL_SELF_DESTRUCT_IN_2_MINUTES);
		final Player player = npc.getVariables().getObject("player0", Player.class);
		if (player != null)
		{
			playSound(player, QuestSound.ITEMSOUND_SIREN);
		}
		return super.onSpawn(npc);
	}
	
	private static final boolean verifyMemoState(Player player, int questId, int memoState)
	{
		QuestState qs = null;
		switch (questId)
		{
			case 184 :
			{
				qs = player.getQuestState("_184_NikolasCooperationContract");
				break;
			}
			case 185 :
			{
				qs = player.getQuestState("_185_NikolasCooperationConsideration");
				break;
			}
		}
		return (qs != null) && ((memoState < 0) || qs.isMemoState(memoState));
	}
	
	private static final void setMemoState(Player player, int questId, int memoState)
	{
		QuestState qs = null;
		switch (questId)
		{
			case 184 :
			{
				qs = player.getQuestState("_184_NikolasCooperationContract");
				break;
			}
			case 185 :
			{
				qs = player.getQuestState("_185_NikolasCooperationConsideration");
				break;
			}
		}
		if (qs != null)
		{
			qs.setMemoState(memoState);
		}
	}
	
	private static final int getMemoStateEx(Player player, int questId, int slot)
	{
		QuestState qs = null;
		switch (questId)
		{
			case 184 :
			{
				qs = player.getQuestState("_184_NikolasCooperationContract");
				break;
			}
			case 185 :
			{
				qs = player.getQuestState("_185_NikolasCooperationConsideration");
				break;
			}
		}
		return (qs != null) ? qs.getMemoStateEx(slot) : -1;
	}
	
	private static final void setMemoStateEx(Player player, int questId, int slot, int memoStateEx)
	{
		QuestState qs = null;
		switch (questId)
		{
			case 184 :
			{
				qs = player.getQuestState("_184_NikolasCooperationContract");
				break;
			}
			case 185 :
			{
				qs = player.getQuestState("_185_NikolasCooperationConsideration");
				break;
			}
		}
		if (qs != null)
		{
			qs.setMemoStateEx(slot, memoStateEx);
		}
	}
	
	public static void main(String[] args)
	{
		new Alarm(Alarm.class.getSimpleName(), "custom");
	}
}
