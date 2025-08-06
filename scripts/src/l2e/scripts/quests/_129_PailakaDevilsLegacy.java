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

import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.MonsterInstance;
import l2e.gameserver.model.entity.Reflection;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.quest.State;
import l2e.gameserver.model.zone.ZoneType;

/**
 * Rework by LordWinter 13.12.2020
 */
public class _129_PailakaDevilsLegacy extends Quest
{
	private static boolean _allowTeleToRaid;
	
	private static final int[][] DROPLIST =
	{
	        {
	                32495,
	                13033,
	                20
			},
			{
			        32495,
			        13049,
			        40
			},
			{
			        32495,
			        13059,
			        60
			},
			{
			        32495,
			        13150,
			        80
			},
			{
			        32495,
			        13048,
			        100
			}
	};

	public _129_PailakaDevilsLegacy(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(32498);
		addFirstTalkId(32498, 32501, 32508, 32511);
		addTalkId(32498, 32501, 32508, 32511);
		
		addAttackId(32495);
		addKillId(18629, 18630, 18631, 18632, 32495, 18622, 18623, 18624, 18625, 18626, 18627);
		
		_allowTeleToRaid = getQuestParams(questId).getBool("allowTeleToRaid");
		
		addEnterZoneId(20113);
		
		questItemIds = new int[]
		{
		        13042, 13043, 13044, 13046, 13047, 13033, 13048, 13049, 13059, 13150
		};
	}

	private static final void dropItem(Npc mob, Player player)
	{
		final int npcId = mob.getId();
		final int chance = getRandom(100);
		for (final int[] drop : DROPLIST)
		{
			if (npcId == drop[0])
			{
				if (chance < drop[2])
				{
					((MonsterInstance) mob).dropSingleItem(player, drop[1], getRandom(1, 6));
					return;
				}
			}
			if (npcId < drop[0])
			{
				return;
			}
		}
	}

	@Override
	public final String onAdvEvent(String event, Npc npc, Player player)
	{
		final QuestState st = player.getQuestState(getName());
		if (st == null || st.isCompleted())
		{
			return getNoQuestMsg(player);
		}

		final int cond = st.getCond();
		if (event.equalsIgnoreCase("32498-05.htm"))
		{
			if (cond == 0)
			{
				st.startQuest();
			}
		}
		else if (event.equalsIgnoreCase("32501-03.htm"))
		{
			if (cond == 2)
			{
				st.giveItems(13042, 1);
				st.setCond(3, true);
			}
		}
		else if ((npc.getId() == 32508) && event.equalsIgnoreCase("teleport_to_raid") && _allowTeleToRaid)
		{
			if (st.getQuestItemsCount(13044) > 0)
			{
				player.teleToLocation(87041, -209212, -3744, true, player.getReflection());
			}
		}
		return event;
	}

	@Override
	public final String onFirstTalk(Npc npc, Player player)
	{
		final QuestState st = player.getQuestState(getName());
		if ((st != null) && (npc.getId() == 32511) && (st.getState() == State.COMPLETED))
		{
			return "32511-03.htm";
		}

		return npc.getId() + ".htm";
	}

	@Override
	public final String onTalk(Npc npc, Player player)
	{
		final QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			return getNoQuestMsg(player);
		}

		final int cond = st.getCond();
		switch (npc.getId())
		{
			case 32498 :
				switch (st.getState())
				{
					case State.CREATED:
						if (player.getLevel() < getMinLvl(getId()))
						{
							return "32498-11.htm";
						}
						if (player.getLevel() > getMaxLvl(getId()))
						{
							return "32498-12.htm";
						}
						return "32498-01.htm";
					case State.STARTED:
						if (cond > 1)
						{
							return "32498-08.htm";
						}
						return "32498-06.htm";
					case State.COMPLETED:
						return "32498-10.htm";
					default:
						return "32498-01.htm";
				}
			case 32501 :
				if (st.getInt("cond") > 2)
				{
					return "32501-04.htm";
				}
				return "32501-01.htm";
			case 32508 :
				if (!player.hasSummon())
				{
					if (st.getQuestItemsCount(13042) > 0)
					{
						if (st.getQuestItemsCount(13046) > 0)
						{
							st.takeItems(13042, -1);
							st.takeItems(13046, -1);
							st.giveItems(13043, 1);
							return "32508-03.htm";
						}
						return "32508-02.htm";
					}
					else if (st.getQuestItemsCount(13043) > 0)
					{
						if (st.getQuestItemsCount(13047) > 0)
						{
							st.takeItems(13043, -1);
							st.takeItems(13047, -1);
							st.giveItems(13044, 1);
							if (_allowTeleToRaid)
							{
								startQuestTimer("teleport_to_raid", 1500, npc, player);
							}
							return "32508-05.htm";
						}
						return "32508-04.htm";
					}
					else if (st.getQuestItemsCount(13044) > 0)
					{
						return "32508-06.htm";
					}
					else
					{
						return "32508-00.htm";
					}
				}
				return "32508-07.htm";
			case 32511 :
				if (!player.hasSummon())
				{
					st.exitQuest(false, true);
					final Reflection inst = ReflectionManager.getInstance().getReflection(npc.getReflectionId());
					if (inst != null)
					{
						inst.setDuration(300000);
						inst.setEmptyDestroyTime(0);
						if (inst.containsPlayer(player.getObjectId()))
						{
							player.setVitalityPoints(20000, true);
							st.calcExpAndSp(getId());
							st.calcReward(getId());
						}
					}
					return "32511-01.htm";
				}
				return "32511-02.htm";
		}
		return getNoQuestMsg(player);
	}

	@Override
	public final String onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		if (npc.getId() == 32495)
		{
			dropItem(npc, attacker);
			npc.doDie(attacker);
		}
		return super.onAttack(npc, attacker, damage, isSummon);
	}
	
	@Override
	public String onEnterZone(Creature character, ZoneType zone)
	{
		if ((character.isPlayer()) && !character.isDead() && !character.isTeleporting())
		{
			final var world = ReflectionManager.getInstance().getWorld(character.getReflectionId());
			if ((world != null) && (world.getTemplateId() == 44) && _allowTeleToRaid)
			{
				character.teleToLocation(87041, -209212, -3744, true, world.getReflection());
			}
		}
		return super.onEnterZone(character, zone);
	}

	@Override
	public final String onKill(Npc npc, Player player, boolean isSummon)
	{
		final QuestState st = player.getQuestState(getName());
		if ((st == null) || (st.getState() != State.STARTED))
		{
			return null;
		}

		switch (npc.getId())
		{
			case 18629 :
				if (st.getQuestItemsCount(13042) > 0)
				{
					st.playSound("ItemSound.quest_itemget");
					st.giveItems(13046, 1);
				}
				break;
			case 18631 :
				if (st.getQuestItemsCount(13043) > 0)
				{
					st.playSound("ItemSound.quest_itemget");
					st.giveItems(13047, 1);
				}
				break;
			default:
				break;
		}
		return super.onKill(npc, player, isSummon);
	}

	public static void main(String[] args)
	{
		new _129_PailakaDevilsLegacy(129, _129_PailakaDevilsLegacy.class.getSimpleName(), "");
	}
}
