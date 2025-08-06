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

import java.util.Calendar;

import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.instancemanager.SoIManager;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.reflection.ReflectionWorld;
import l2e.gameserver.model.entity.Reflection;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.quest.State;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.SystemMessage;

/**
 * Rework by LordWinter 18.09.2020
 */
public final class _695_DefendTheHallOfSuffering extends Quest
{
	public _695_DefendTheHallOfSuffering(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(32603);
		addTalkId(32603);
		addTalkId(32530);
	}
	
	@Override
	public final String onAdvEvent(String event, Npc npc, Player player)
	{
		final String htmltext = event;
		
		final QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			return htmltext;
		}
		
		if (event.equalsIgnoreCase("32603-02.htm"))
		{
			if (st.isCreated())
			{
				st.startQuest();
			}
		}
		return htmltext;
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
		
		switch (st.getState())
		{
			case State.CREATED:
				if ((player.getLevel() >= getMinLvl(getId())) && (player.getLevel() <= getMaxLvl(getId())))
				{
					if (SoIManager.getInstance().getCurrentStage() == 4)
					{
						htmltext = "32603-01.htm";
					}
					else
					{
						htmltext = "32603-04.htm";
					}
				}
				else
				{
					htmltext = "32603-00.htm";
					st.exitQuest(true);
				}
				break;
			case State.STARTED:
				switch (npc.getId())
				{
					case 32603 :
						htmltext = "32603-01a.htm";
						break;
					case 32530 :
						final ReflectionWorld world = ReflectionManager.getInstance().getPlayerWorld(player);
						if ((world != null) && (world.getTemplateId() == 116))
						{
							if (world.getTag() == -1)
							{
								htmltext = "32530-11.htm";
							}
							else if ((player.getParty() != null) && (player.getParty().getLeaderObjectId() == player.getObjectId()))
							{
								for (final Player member : player.getParty().getMembers())
								{
									final QuestState st1 = member.getQuestState(getName());
									if (st1 != null)
									{
										if (world.getTag() == 13777)
										{
											st1.calcReward(getId(), 1);
											st1.exitQuest(true, true);
											htmltext = "32530-00.htm";
											finishInstance(player);
										}
										else if (world.getTag() == 13778)
										{
											st1.calcReward(getId(), 2);
											st1.exitQuest(true, true);
											htmltext = "32530-01.htm";
											finishInstance(player);
										}
										else if (world.getTag() == 13779)
										{
											st1.calcReward(getId(), 3);
											st1.exitQuest(true, true);
											htmltext = "32530-02.htm";
											finishInstance(player);
										}
										else if (world.getTag() == 13780)
										{
											st1.calcReward(getId(), 4);
											st1.exitQuest(true, true);
											htmltext = "32530-03.htm";
											finishInstance(player);
										}
										else if (world.getTag() == 13781)
										{
											st1.calcReward(getId(), 5);
											st1.exitQuest(true, true);
											htmltext = "32530-04.htm";
											finishInstance(player);
										}
										else if (world.getTag() == 13782)
										{
											st1.calcReward(getId(), 6);
											st1.exitQuest(true, true);
											htmltext = "32530-05.htm";
											finishInstance(player);
										}
										else if (world.getTag() == 13783)
										{
											st1.calcReward(getId(), 7);
											st1.exitQuest(true, true);
											htmltext = "32530-06.htm";
											finishInstance(player);
										}
										else if (world.getTag() == 13784)
										{
											st1.calcReward(getId(), 8);
											st1.exitQuest(true, true);
											htmltext = "32530-07.htm";
											finishInstance(player);
										}
										else if (world.getTag() == 13785)
										{
											st1.calcReward(getId(), 9);
											st1.exitQuest(true, true);
											htmltext = "32530-08.htm";
											finishInstance(player);
										}
										else if (world.getTag() == 13786)
										{
											st1.calcReward(getId(), 10);
											st1.exitQuest(true, true);
											htmltext = "32530-09.htm";
											finishInstance(player);
										}
										else
										{
											htmltext = "32530-11.htm";
										}
									}
								}
							}
							else
							{
								return "32530-10.htm";
							}
						}
						else
						{
							htmltext = "32530-11.htm";
						}
						break;
				}
				break;
		}
		return htmltext;
	}
	
	private static final void finishInstance(Player player)
	{
		final ReflectionWorld world = ReflectionManager.getInstance().getPlayerWorld(player);
		
		final Calendar reenter = Calendar.getInstance();
		reenter.set(Calendar.MINUTE, 30);
		
		if (reenter.get(Calendar.HOUR_OF_DAY) >= 6)
		{
			reenter.add(Calendar.DATE, 1);
		}
		reenter.set(Calendar.HOUR_OF_DAY, 6);
		
		final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.INSTANT_ZONE_S1_RESTRICTED);
		sm.addInstanceName(world.getTemplateId());
		
		final var instance = ReflectionManager.getInstance();
		for (final int objectId : world.getAllowed())
		{
			final Player obj = GameObjectsStorage.getPlayer(objectId);
			instance.setReflectionTime(obj, objectId, world.getTemplateId(), reenter.getTimeInMillis(), world.getReflection().isHwidCheck());
			if ((obj != null) && obj.isOnline())
			{
				obj.sendPacket(sm);
			}
		}
		final Reflection inst = ReflectionManager.getInstance().getReflection(world.getReflectionId());
		inst.setDuration(5 * 60000);
		inst.setEmptyDestroyTime(0);
	}
	
	public static void main(String[] args)
	{
		new _695_DefendTheHallOfSuffering(695, _695_DefendTheHallOfSuffering.class.getSimpleName(), "");
	}
}