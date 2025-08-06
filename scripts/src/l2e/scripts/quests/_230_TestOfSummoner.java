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

import java.util.HashMap;
import java.util.Map;

import l2e.commons.util.Util;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.Summon;
import l2e.gameserver.model.base.ClassId;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.NpcSay;
import l2e.gameserver.network.serverpackets.SocialAction;

/**
 * Rework by LordWinter 31.03.2021
 */
public class _230_TestOfSummoner extends Quest
{
	private static final Map<Integer, MonsterData> _monsters = new HashMap<>();
	static
	{
		_monsters.put(27102, new MonsterData(3361, 3364, NpcStringId.IM_SORRY_LORD));
		_monsters.put(27103, new MonsterData(3371, 3374, NpcStringId.I_LOSE));
		_monsters.put(27104, new MonsterData(3381, 3384, NpcStringId.UGH_I_LOST));
		_monsters.put(27105, new MonsterData(3366, 3369, NpcStringId.LOST_SORRY_LORD));
		_monsters.put(27106, new MonsterData(3376, 3379, NpcStringId.I_LOSE));
		_monsters.put(27107, new MonsterData(3386, 3389, NpcStringId.UGH_CAN_THIS_BE_HAPPENING));
	}
	
	public _230_TestOfSummoner(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(30634);
		addTalkId(30634, 30063, 30635, 30636, 30637, 30638, 30639, 30640);
		
		addAttackId(27102, 27103, 27104, 27105, 27106, 27107);
		
		addKillId(20089, 20090, 20176, 20192, 20193, 20267, 20268, 20269, 20270, 20271, 20552, 20553, 20555, 20563, 20577, 20578, 20579, 20580, 20581, 20582, 20600);
		addKillId(_monsters.keySet());
		
		questItemIds = new int[]
		{
		        3337, 3338, 3339, 3340, 3341, 3342, 3343, 3344, 3345, 3346, 3347, 3348, 3349, 3350, 3351, 3352, 3353, 3354, 3355, 3356, 3357, 3358, 3359, 3360, 3361, 3362, 3363, 3364, 3365, 3366, 3367, 3368, 3369, 3370, 3371, 3372, 3373, 3374, 3375, 3376, 3377, 3378, 3379, 3380, 3381, 3382, 3383, 3384, 3385, 3386, 3387, 3388, 3389
		};
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "DESPAWN" :
			{
				npc.onDecay();
				break;
			}
			case "KILLED_ATTACKER" :
			{
				final Summon summon = npc.getVariables().getObject("ATTACKER", Summon.class);
				if ((summon != null) && summon.isDead())
				{
					npc.onDecay();
				}
				else
				{
					startQuestTimer("KILLED_ATTACKER", 5000, npc, null);
				}
				break;
			}
		}
		
		if (player == null)
		{
			return null;
		}
		
		final QuestState qs = getQuestState(player, false);
		if (qs == null || qs.isCompleted())
		{
			return null;
		}
		
		String htmltext = null;
		switch (event)
		{
			case "ACCEPT" :
			{
				if (qs.isCreated())
				{
					qs.startQuest();
					playSound(player, QuestSound.ITEMSOUND_QUEST_MIDDLE);
					giveItems(player, 3352, 1);
					if (player.getVarInt("2ND_CLASS_DIAMOND_REWARD", 0) == 0)
					{
						qs.calcReward(getId(), 1);
						player.setVar("2ND_CLASS_DIAMOND_REWARD", 1);
						htmltext = "30634-08a.htm";
					}
					else
					{
						htmltext = "30634-08.htm";
					}
				}
				break;
			}
			case "30634-04.htm" :
			case "30634-05.htm" :
			case "30634-06.htm" :
			case "30634-07.htm" :
			case "30634-11.htm" :
			case "30634-11a.htm" :
			case "30634-11b.htm" :
			case "30634-11c.htm" :
			case "30634-11d.htm" :
			{
				htmltext = event;
				break;
			}
			case "30063-02.htm" :
			{
				switch (getRandom(5))
				{
					case 0 :
					{
						giveItems(player, 3347, 1);
						break;
					}
					case 1 :
					{
						giveItems(player, 3348, 1);
						break;
					}
					case 2 :
					{
						giveItems(player, 3349, 1);
						break;
					}
					case 3 :
					{
						giveItems(player, 3350, 1);
						break;
					}
					case 4 :
					{
						giveItems(player, 3351, 1);
						break;
					}
				}
				qs.setCond(2, true);
				takeItems(player, 3352, 1);
				htmltext = event;
				break;
			}
			case "30063-04.htm" :
			{
				switch (getRandom(5))
				{
					case 0 :
					{
						giveItems(player, 3347, 1);
						break;
					}
					case 1 :
					{
						giveItems(player, 3348, 1);
						break;
					}
					case 2 :
					{
						giveItems(player, 3349, 1);
						break;
					}
					case 3 :
					{
						giveItems(player, 3350, 1);
						break;
					}
					case 4 :
					{
						giveItems(player, 3351, 1);
						break;
					}
				}
				htmltext = event;
				break;
			}
			case "30635-03.htm" :
			{
				if (hasQuestItems(player, 3353))
				{
					htmltext = event;
				}
				else
				{
					htmltext = "30635-02.htm";
				}
				break;
			}
			case "30635-04.htm" :
			{
				npc.setTarget(player);
				npc.doCast(SkillsParser.getInstance().getInfo(4126, 1));
				takeItems(player, 3353, 1);
				giveItems(player, 3360, 1);
				takeItems(player, 3362, 1);
				takeItems(player, 3363, 1);
				htmltext = event;
				break;
			}
			case "30636-03.htm" :
			{
				if (hasQuestItems(player, 3353))
				{
					htmltext = event;
				}
				else
				{
					htmltext = "30636-02.htm";
				}
				break;
			}
			case "30636-04.htm" :
			{
				npc.setTarget(player);
				npc.doCast(SkillsParser.getInstance().getInfo(4126, 1));
				takeItems(player, 3353, 1);
				giveItems(player, 3370, 1);
				takeItems(player, 3372, 1);
				takeItems(player, 3373, 1);
				htmltext = event;
				break;
			}
			case "30637-03.htm" :
			{
				if (hasQuestItems(player, 3353))
				{
					htmltext = event;
				}
				else
				{
					htmltext = "30637-02.htm";
				}
				break;
			}
			case "30637-04.htm" :
			{
				npc.setTarget(player);
				npc.doCast(SkillsParser.getInstance().getInfo(4126, 1));
				takeItems(player, 3353, 1);
				giveItems(player, 3380, 1);
				takeItems(player, 3382, 1);
				takeItems(player, 3383, 1);
				htmltext = event;
				break;
			}
			case "30638-03.htm" :
			{
				if (hasQuestItems(player, 3353))
				{
					htmltext = event;
				}
				else
				{
					htmltext = "30638-02.htm";
				}
				break;
			}
			case "30638-04.htm" :
			{
				npc.setTarget(player);
				npc.doCast(SkillsParser.getInstance().getInfo(4126, 1));
				takeItems(player, 3353, 1);
				giveItems(player, 3365, 1);
				takeItems(player, 3367, 1);
				takeItems(player, 3368, 1);
				htmltext = event;
				break;
			}
			case "30639-03.htm" :
			{
				if (hasQuestItems(player, 3353))
				{
					htmltext = event;
				}
				else
				{
					htmltext = "30639-02.htm";
				}
				break;
			}
			case "30639-04.htm" :
			{
				npc.setTarget(player);
				npc.doCast(SkillsParser.getInstance().getInfo(4126, 1));
				takeItems(player, 3353, 1);
				giveItems(player, 3375, 1);
				takeItems(player, 3377, 1);
				takeItems(player, 3378, 1);
				htmltext = event;
				break;
			}
			case "30640-03.htm" :
			{
				if (hasQuestItems(player, 3353))
				{
					htmltext = event;
				}
				else
				{
					htmltext = "30640-02.htm";
				}
				break;
			}
			case "30640-04.htm" :
			{
				npc.setTarget(player);
				npc.doCast(SkillsParser.getInstance().getInfo(4126, 1));
				takeItems(player, 3353, 1);
				giveItems(player, 3385, 1);
				takeItems(player, 3387, 1);
				takeItems(player, 3388, 1);
				htmltext = event;
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public String onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		switch (npc.getId())
		{
			case 27102 :
			{
				switch (npc.getScriptValue())
				{
					case 0 :
					{
						if (isSummon)
						{
							npc.getVariables().set("ATTACKER", attacker.getSummon());
							npc.setScriptValue(1);
							startQuestTimer("DESPAWN", 120000, npc, null);
							startQuestTimer("KILLED_ATTACKER", 5000, npc, null);
							
							final QuestState qs = getQuestState(attacker, false);
							if (hasQuestItems(attacker, 3360) && (qs != null) && qs.isStarted())
							{
								npc.broadcastPacketToOthers(2000, new NpcSay(npc.getObjectId(), Say2.NPC_ALL, npc.getId(), NpcStringId.WHHIISSHH));
								takeItems(attacker, 3360, -1);
								giveItems(attacker, 3361, 1);
								if (npc instanceof Attackable)
								{
									((Attackable) npc).addDamageHate(attacker.getSummon(), 0, 100000);
								}
							}
						}
						break;
					}
					case 1 :
					{
						if (!isSummon || (npc.getVariables().getObject("ATTACKER", Summon.class) != attacker.getSummon()))
						{
							final QuestState qs = getQuestState(attacker, false);
							if (!hasQuestItems(attacker, 3360) && hasQuestItems(attacker, 3361) && (qs != null) && qs.isStarted())
							{
								npc.setScriptValue(2);
								npc.broadcastPacketToOthers(2000, new NpcSay(npc.getObjectId(), Say2.NPC_ALL, npc.getId(), NpcStringId.RULE_VIOLATION));
								takeItems(attacker, 3361, -1);
								giveItems(attacker, 3362, 1);
								takeItems(attacker, 3360, -1);
							}
							npc.onDecay();
						}
						break;
					}
				}
				break;
			}
			case 27103 :
			{
				switch (npc.getScriptValue())
				{
					case 0 :
					{
						if (isSummon)
						{
							npc.getVariables().set("ATTACKER", attacker.getSummon());
							npc.setScriptValue(1);
							startQuestTimer("DESPAWN", 120000, npc, null);
							startQuestTimer("KILLED_ATTACKER", 5000, npc, null);
							
							final QuestState qs = getQuestState(attacker, false);
							if (hasQuestItems(attacker, 3370) && (qs != null) && qs.isStarted())
							{
								npc.broadcastPacketToOthers(2000, new NpcSay(npc.getObjectId(), Say2.NPC_ALL, npc.getId(), NpcStringId.START_DUEL));
								takeItems(attacker, 3370, -1);
								giveItems(attacker, 3371, 1);
								if (npc instanceof Attackable)
								{
									((Attackable) npc).addDamageHate(attacker.getSummon(), 0, 100000);
								}
							}
						}
						break;
					}
					case 1 :
					{
						if (!isSummon || (npc.getVariables().getObject("ATTACKER", Summon.class) != attacker.getSummon()))
						{
							final QuestState qs = getQuestState(attacker, false);
							if (!hasQuestItems(attacker, 3370) && hasQuestItems(attacker, 3371) && (qs != null) && qs.isStarted())
							{
								npc.setScriptValue(2);
								npc.broadcastPacketToOthers(2000, new NpcSay(npc.getObjectId(), Say2.NPC_ALL, npc.getId(), NpcStringId.RULE_VIOLATION));
								takeItems(attacker, 3371, -1);
								giveItems(attacker, 3372, 1);
								takeItems(attacker, 3370, -1);
							}
							npc.onDecay();
						}
						break;
					}
				}
			}
			case 27104 :
			{
				switch (npc.getScriptValue())
				{
					case 0 :
					{
						if (isSummon)
						{
							npc.getVariables().set("ATTACKER", attacker.getSummon());
							npc.setScriptValue(1);
							startQuestTimer("DESPAWN", 120000, npc, null);
							startQuestTimer("KILLED_ATTACKER", 5000, npc, null);
							
							final QuestState qs = getQuestState(attacker, false);
							if (hasQuestItems(attacker, 3380) && (qs != null) && qs.isStarted())
							{
								npc.broadcastPacketToOthers(2000, new NpcSay(npc.getObjectId(), Say2.NPC_ALL, npc.getId(), NpcStringId.SO_SHALL_WE_START));
								takeItems(attacker, 3380, -1);
								giveItems(attacker, 3381, 1);
								if (npc instanceof Attackable)
								{
									((Attackable) npc).addDamageHate(attacker.getSummon(), 0, 100000);
								}
							}
						}
						break;
					}
					case 1 :
					{
						if (!isSummon || (npc.getVariables().getObject("ATTACKER", Summon.class) != attacker.getSummon()))
						{
							final QuestState qs = getQuestState(attacker, false);
							if (!hasQuestItems(attacker, 3380) && hasQuestItems(attacker, 3381) && (qs != null) && qs.isStarted())
							{
								npc.setScriptValue(2);
								npc.broadcastPacketToOthers(2000, new NpcSay(npc.getObjectId(), Say2.NPC_ALL, npc.getId(), NpcStringId.RULE_VIOLATION));
								takeItems(attacker, 3381, -1);
								giveItems(attacker, 3382, 1);
								takeItems(attacker, 3380, -1);
							}
							npc.onDecay();
						}
						break;
					}
				}
			}
			case 27105 :
			{
				switch (npc.getScriptValue())
				{
					case 0 :
					{
						if (isSummon)
						{
							npc.getVariables().set("ATTACKER", attacker.getSummon());
							npc.setScriptValue(1);
							startQuestTimer("DESPAWN", 120000, npc, null);
							startQuestTimer("KILLED_ATTACKER", 5000, npc, null);
							
							final QuestState qs = getQuestState(attacker, false);
							if (hasQuestItems(attacker, 3365) && (qs != null) && qs.isStarted())
							{
								npc.broadcastPacketToOthers(2000, new NpcSay(npc.getObjectId(), Say2.NPC_ALL, npc.getId(), NpcStringId.WHISH_FIGHT));
								takeItems(attacker, 3365, -1);
								giveItems(attacker, 3366, 1);
								if (npc instanceof Attackable)
								{
									((Attackable) npc).addDamageHate(attacker.getSummon(), 0, 100000);
								}
							}
						}
						break;
					}
					case 1 :
					{
						if (!isSummon || (npc.getVariables().getObject("ATTACKER", Summon.class) != attacker.getSummon()))
						{
							final QuestState qs = getQuestState(attacker, false);
							if (!hasQuestItems(attacker, 3365) && hasQuestItems(attacker, 3366) && (qs != null) && qs.isStarted())
							{
								npc.setScriptValue(2);
								npc.broadcastPacketToOthers(2000, new NpcSay(npc.getObjectId(), Say2.NPC_ALL, npc.getId(), NpcStringId.RULE_VIOLATION));
								takeItems(attacker, 3366, -1);
								giveItems(attacker, 3367, 1);
								takeItems(attacker, 3365, -1);
							}
							npc.onDecay();
						}
						break;
					}
				}
			}
			case 27106 :
			{
				switch (npc.getScriptValue())
				{
					case 0 :
					{
						if (isSummon)
						{
							npc.getVariables().set("ATTACKER", attacker.getSummon());
							npc.setScriptValue(1);
							startQuestTimer("DESPAWN", 120000, npc, null);
							startQuestTimer("KILLED_ATTACKER", 5000, npc, null);
							
							final QuestState qs = getQuestState(attacker, false);
							if (hasQuestItems(attacker, 3375) && (qs != null) && qs.isStarted())
							{
								npc.broadcastPacketToOthers(2000, new NpcSay(npc.getObjectId(), Say2.NPC_ALL, npc.getId(), NpcStringId.START_DUEL));
								takeItems(attacker, 3375, -1);
								giveItems(attacker, 3376, 1);
								if (npc instanceof Attackable)
								{
									((Attackable) npc).addDamageHate(attacker.getSummon(), 0, 100000);
								}
							}
						}
						break;
					}
					case 1 :
					{
						if (!isSummon || (npc.getVariables().getObject("ATTACKER", Summon.class) != attacker.getSummon()))
						{
							final QuestState qs = getQuestState(attacker, false);
							if (!hasQuestItems(attacker, 3375) && hasQuestItems(attacker, 3376) && (qs != null) && qs.isStarted())
							{
								npc.setScriptValue(2);
								npc.broadcastPacketToOthers(2000, new NpcSay(npc.getObjectId(), Say2.NPC_ALL, npc.getId(), NpcStringId.RULE_VIOLATION));
								takeItems(attacker, 3376, -1);
								giveItems(attacker, 3377, 1);
								takeItems(attacker, 3375, -1);
							}
							npc.onDecay();
						}
						break;
					}
				}
			}
			case 27107 :
			{
				switch (npc.getScriptValue())
				{
					case 0 :
					{
						if (isSummon)
						{
							npc.getVariables().set("ATTACKER", attacker.getSummon());
							npc.setScriptValue(1);
							startQuestTimer("DESPAWN", 120000, npc, null);
							startQuestTimer("KILLED_ATTACKER", 5000, npc, null);
							
							final QuestState qs = getQuestState(attacker, false);
							if (hasQuestItems(attacker, 3385) && (qs != null) && qs.isStarted())
							{
								npc.broadcastPacketToOthers(2000, new NpcSay(npc.getObjectId(), Say2.NPC_ALL, npc.getId(), NpcStringId.ILL_WALK_ALL_OVER_YOU));
								takeItems(attacker, 3385, -1);
								giveItems(attacker, 3386, 1);
								if (npc instanceof Attackable)
								{
									((Attackable) npc).addDamageHate(attacker.getSummon(), 0, 100000);
								}
							}
						}
						break;
					}
					case 1 :
					{
						if (!isSummon || (npc.getVariables().getObject("ATTACKER", Summon.class) != attacker.getSummon()))
						{
							final QuestState qs = getQuestState(attacker, false);
							if (!hasQuestItems(attacker, 3385) && hasQuestItems(attacker, 3386) && (qs != null) && qs.isStarted())
							{
								npc.setScriptValue(2);
								npc.broadcastPacketToOthers(2000, new NpcSay(npc.getObjectId(), Say2.NPC_ALL, npc.getId(), NpcStringId.RULE_VIOLATION));
								takeItems(attacker, 3386, -1);
								giveItems(attacker, 3387, 1);
								takeItems(attacker, 3385, -1);
							}
							npc.onDecay();
						}
						break;
					}
				}
			}
		}
		return super.onAttack(npc, attacker, damage, isSummon);
	}
	
	@Override
	public String onKill(Npc npc, Player killer, boolean isSummon)
	{
		final QuestState qs = getQuestState(killer, false);
		if ((qs != null) && qs.isStarted() && Util.checkIfInRange(1500, npc, killer, true))
		{
			switch (npc.getId())
			{
				case 20089 :
				case 20090 :
				{
					if (!hasQuestItems(killer, 3352) && hasQuestItems(killer, 3351))
					{
						giveItemRandomly(killer, npc, 3344, 2, 30, 1.0, true);
					}
					break;
				}
				case 20176 :
				{
					if (!hasQuestItems(killer, 3352) && hasQuestItems(killer, 3351))
					{
						giveItemRandomly(killer, npc, 3346, 3, 30, 1.0, true);
					}
					break;
				}
				case 20192 :
				case 20193 :
				{
					if (!hasQuestItems(killer, 3352) && hasQuestItems(killer, 3350))
					{
						giveItemRandomly(killer, npc, 3343, 3, 30, 1.0, true);
					}
					break;
				}
				case 20267 :
				case 20268 :
				case 20271 :
				{
					if (!hasQuestItems(killer, 3352) && hasQuestItems(killer, 3349))
					{
						giveItemRandomly(killer, npc, 3341, 1, 30, 1.0, true);
					}
					break;
				}
				case 20269 :
				case 20270 :
				{
					if (!hasQuestItems(killer, 3352) && hasQuestItems(killer, 3349))
					{
						giveItemRandomly(killer, npc, 3341, 2, 30, 1.0, true);
					}
					break;
				}
				case 20552 :
				{
					if (!hasQuestItems(killer, 3352) && hasQuestItems(killer, 3349))
					{
						giveItemRandomly(killer, npc, 3342, 6, 30, 1.0, true);
					}
					break;
				}
				case 20553 :
				{
					if (!hasQuestItems(killer, 3352) && hasQuestItems(killer, 3350))
					{
						giveItemRandomly(killer, npc, 3345, 3, 30, 1.0, true);
					}
					break;
				}
				case 20555 :
				{
					if (!hasQuestItems(killer, 3352) && hasQuestItems(killer, 3347))
					{
						giveItemRandomly(killer, npc, 3338, 2, 30, 1.0, true);
					}
					break;
				}
				case 20563 :
				{
					if (!hasQuestItems(killer, 3352) && hasQuestItems(killer, 3348))
					{
						giveItemRandomly(killer, npc, 3340, 2, 30, 1.0, true);
					}
					break;
				}
				case 20577 :
				case 20578 :
				case 20579 :
				case 20580 :
				{
					if (!hasQuestItems(killer, 3352) && hasQuestItems(killer, 3347))
					{
						giveItemRandomly(killer, npc, 3337, 1, 30, 1.0, true);
					}
					break;
				}
				case 20581 :
				case 20582 :
				{
					if (!hasQuestItems(killer, 3352) && hasQuestItems(killer, 3347))
					{
						giveItemRandomly(killer, npc, 3337, 2, 30, 1.0, true);
					}
					break;
				}
				case 20600 :
				{
					if (!hasQuestItems(killer, 3352) && hasQuestItems(killer, 3348))
					{
						giveItemRandomly(killer, npc, 3339, 2, 30, 1.0, true);
					}
					break;
				}
				case 27107 :
				case 27106 :
				case 27105 :
				case 27104 :
				case 27103 :
				case 27102 :
				{
					final MonsterData data = _monsters.get(npc.getId());
					if (hasQuestItems(killer, data.getCrystalOfInprogress()))
					{
						npc.broadcastPacketToOthers(2000, new NpcSay(npc, Say2.NPC_ALL, data.getNpcStringId()));
						takeItems(killer, data.getCrystalOfInprogress(), 1);
						giveItems(killer, data.getCrystalOfVictory(), 1);
						playSound(killer, QuestSound.ITEMSOUND_QUEST_MIDDLE);
					}
					break;
				}
			}
		}
		return super.onKill(npc, killer, isSummon);
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, true);
		String htmltext = getNoQuestMsg(player);
		if (qs.isCreated())
		{
			if (npc.getId() == 30634)
			{
				if ((player.getClassId() == ClassId.wizard) || (player.getClassId() == ClassId.elvenWizard) || (player.getClassId() == ClassId.darkWizard))
				{
					if (player.getLevel() >= 39)
					{
						htmltext = "30634-03.htm";
					}
					else
					{
						htmltext = "30634-02.htm";
					}
				}
				else
				{
					htmltext = "30634-01.htm";
				}
			}
		}
		else if (qs.isStarted())
		{
			switch (npc.getId())
			{
				case 30634 :
				{
					if (hasQuestItems(player, 3352))
					{
						htmltext = "30634-09.htm";
					}
					else if (!hasQuestItems(player, 3352))
					{
						if (!hasQuestItems(player, 3354, 3357, 3355, 3358, 3356, 3359) && !hasQuestItems(player, 3353))
						{
							htmltext = "30634-10.htm";
						}
						else if (!hasQuestItems(player, 3354, 3357, 3355, 3358, 3356, 3359) && hasQuestItems(player, 3353))
						{
							htmltext = "30634-11.htm";
						}
						else if (hasQuestItems(player, 3354, 3357, 3355, 3358, 3356, 3359))
						{
							qs.calcExpAndSp(getId());
							qs.calcReward(getId(), 2);
							qs.exitQuest(false, true);
							player.sendPacket(new SocialAction(player.getObjectId(), 3));
							htmltext = "30634-12.htm";
						}
					}
					break;
				}
				case 30063 :
				{
					if (hasQuestItems(player, 3352))
					{
						htmltext = "30063-01.htm";
					}
					else if (!hasQuestItems(player, 3352))
					{
						if (!hasAtLeastOneQuestItem(player, 3347, 3348, 3349, 3350, 3351))
						{
							htmltext = "30063-03.htm";
						}
						else if (hasQuestItems(player, 3347))
						{
							if ((getQuestItemsCount(player, 3337) >= 30) && (getQuestItemsCount(player, 3338) >= 30))
							{
								takeItems(player, 3337, -1);
								takeItems(player, 3338, -1);
								takeItems(player, 3347, 1);
								giveItems(player, 3353, 2);
								qs.setCond(3, true);
								htmltext = "30063-06.htm";
							}
							else
							{
								htmltext = "30063-05.htm";
							}
						}
						else if (hasQuestItems(player, 3348))
						{
							if ((getQuestItemsCount(player, 3339) >= 30) && (getQuestItemsCount(player, 3340) >= 30))
							{
								takeItems(player, 3339, -1);
								takeItems(player, 3340, -1);
								takeItems(player, 3348, 1);
								giveItems(player, 3353, 2);
								qs.setCond(3, true);
								htmltext = "30063-08.htm";
							}
							else
							{
								htmltext = "30063-07.htm";
							}
						}
						else if (hasQuestItems(player, 3349))
						{
							if ((getQuestItemsCount(player, 3341) >= 30) && (getQuestItemsCount(player, 3342) >= 30))
							{
								takeItems(player, 3341, -1);
								takeItems(player, 3342, -1);
								takeItems(player, 3349, 1);
								giveItems(player, 3353, 2);
								qs.setCond(3, true);
								htmltext = "30063-10.htm";
							}
							else
							{
								htmltext = "30063-09.htm";
							}
						}
						else if (hasQuestItems(player, 3350))
						{
							if ((getQuestItemsCount(player, 3343) >= 30) && (getQuestItemsCount(player, 3345) >= 30))
							{
								takeItems(player, 3343, -1);
								takeItems(player, 3345, -1);
								takeItems(player, 3350, 1);
								giveItems(player, 3353, 2);
								qs.setCond(3, true);
								htmltext = "30063-12.htm";
							}
							else
							{
								htmltext = "30063-11.htm";
							}
						}
						else if (hasQuestItems(player, 3351))
						{
							if ((getQuestItemsCount(player, 3344) >= 30) && (getQuestItemsCount(player, 3346) >= 30))
							{
								takeItems(player, 3344, -1);
								takeItems(player, 3346, -1);
								takeItems(player, 3351, 1);
								giveItems(player, 3353, 2);
								qs.setCond(3, true);
								htmltext = "30063-14.htm";
							}
							else
							{
								htmltext = "30063-13.htm";
							}
						}
					}
					break;
				}
				case 30635 :
				{
					if (!hasQuestItems(player, 3354))
					{
						if (!hasAtLeastOneQuestItem(player, 3360, 3361, 3362, 3363, 3364))
						{
							htmltext = "30635-01.htm";
						}
						else if (!hasAtLeastOneQuestItem(player, 3360, 3361, 3362, 3364) && hasQuestItems(player, 3363))
						{
							htmltext = "30635-05.htm";
						}
						else if (!hasAtLeastOneQuestItem(player, 3360, 3361, 3363, 3364) && hasQuestItems(player, 3362))
						{
							htmltext = "30635-06.htm";
						}
						else if (!hasAtLeastOneQuestItem(player, 3360, 3361, 3362, 3363) && hasQuestItems(player, 3364))
						{
							giveItems(player, 3354, 1);
							takeItems(player, 3364, 1);
							if (hasQuestItems(player, 3357, 3355, 3358, 3356, 3359))
							{
								qs.setCond(4, true);
							}
							htmltext = "30635-07.htm";
						}
						else if (!hasAtLeastOneQuestItem(player, 3361, 3362, 3363, 3364) && hasQuestItems(player, 3360))
						{
							htmltext = "30635-08.htm";
						}
						else if (!hasAtLeastOneQuestItem(player, 3360, 3362, 3363, 3364) && hasQuestItems(player, 3361))
						{
							htmltext = "30635-09.htm";
						}
					}
					else
					{
						htmltext = "30635-10.htm";
					}
					break;
				}
				case 30636 :
				{
					if (!hasQuestItems(player, 3355))
					{
						if (!hasAtLeastOneQuestItem(player, 3370, 3371, 3372, 3373, 3374))
						{
							htmltext = "30636-01.htm";
						}
						else if (!hasAtLeastOneQuestItem(player, 3370, 3371, 3372, 3374) && hasQuestItems(player, 3373))
						{
							htmltext = "30636-05.htm";
						}
						else if (!hasAtLeastOneQuestItem(player, 3370, 3371, 3373, 3374) && hasQuestItems(player, 3372))
						{
							htmltext = "30636-06.htm";
						}
						else if (!hasAtLeastOneQuestItem(player, 3370, 3371, 3372, 3373) && hasQuestItems(player, 3374))
						{
							giveItems(player, 3355, 1);
							takeItems(player, 3374, 1);
							if (hasQuestItems(player, 3354, 3357, 3358, 3356, 3359))
							{
								qs.setCond(4, true);
							}
							htmltext = "30636-07.htm";
						}
						else if (!hasAtLeastOneQuestItem(player, 3371, 3372, 3373, 3374) && hasQuestItems(player, 3370))
						{
							htmltext = "30636-08.htm";
						}
						else if (!hasAtLeastOneQuestItem(player, 3370, 3372, 3373, 3374) && hasQuestItems(player, 3371))
						{
							htmltext = "30636-09.htm";
						}
					}
					else
					{
						htmltext = "30636-10.htm";
					}
					break;
				}
				case 30637 :
				{
					if (!hasQuestItems(player, 3356))
					{
						if (!hasAtLeastOneQuestItem(player, 3380, 3381, 3382, 3383, 3384))
						{
							htmltext = "30637-01.htm";
						}
						else if (!hasAtLeastOneQuestItem(player, 3380, 3381, 3382, 3384) && hasQuestItems(player, 3383))
						{
							htmltext = "30637-05.htm";
						}
						else if (!hasAtLeastOneQuestItem(player, 3380, 3381, 3383, 3384) && hasQuestItems(player, 3382))
						{
							htmltext = "30637-06.htm";
						}
						else if (!hasAtLeastOneQuestItem(player, 3380, 3381, 3382, 3383) && hasQuestItems(player, 3384))
						{
							giveItems(player, 3356, 1);
							takeItems(player, 3384, 1);
							if (hasQuestItems(player, 3354, 3357, 3355, 3358, 3359))
							{
								qs.setCond(4, true);
							}
							htmltext = "30637-07.htm";
						}
						else if (!hasAtLeastOneQuestItem(player, 3381, 3382, 3383, 3384) && hasQuestItems(player, 3380))
						{
							htmltext = "30637-08.htm";
						}
						else if (!hasAtLeastOneQuestItem(player, 3380, 3382, 3383, 3384) && hasQuestItems(player, 3381))
						{
							htmltext = "30637-09.htm";
						}
					}
					else
					{
						htmltext = "30637-10.htm";
					}
					break;
				}
				case 30638 :
				{
					if (!hasQuestItems(player, 3357))
					{
						if (!hasAtLeastOneQuestItem(player, 3365, 3366, 3367, 3368, 3369))
						{
							htmltext = "30638-01.htm";
						}
						else if (!hasAtLeastOneQuestItem(player, 3365, 3366, 3367, 3369) && hasQuestItems(player, 3368))
						{
							htmltext = "30638-05.htm";
						}
						else if (!hasAtLeastOneQuestItem(player, 3365, 3366, 3368, 3369) && hasQuestItems(player, 3367))
						{
							htmltext = "30638-06.htm";
						}
						else if (!hasAtLeastOneQuestItem(player, 3365, 3366, 3367, 3368) && hasQuestItems(player, 3369))
						{
							giveItems(player, 3357, 1);
							takeItems(player, 3369, 1);
							if (hasQuestItems(player, 3354, 3355, 3358, 3356, 3359))
							{
								qs.setCond(4, true);
							}
							htmltext = "30638-07.htm";
						}
						else if (!hasAtLeastOneQuestItem(player, 3366, 3367, 3368, 3369) && hasQuestItems(player, 3365))
						{
							htmltext = "30638-08.htm";
						}
						else if (!hasAtLeastOneQuestItem(player, 3365, 3367, 3368, 3369) && hasQuestItems(player, 3366))
						{
							htmltext = "30638-09.htm";
						}
					}
					else
					{
						htmltext = "30638-10.htm";
					}
					break;
				}
				case 30639 :
				{
					if (!hasQuestItems(player, 3358))
					{
						if (!hasAtLeastOneQuestItem(player, 3375, 3376, 3377, 3378, 3379))
						{
							htmltext = "30639-01.htm";
						}
						else if (!hasAtLeastOneQuestItem(player, 3375, 3376, 3377, 3379) && hasQuestItems(player, 3378))
						{
							htmltext = "30639-05.htm";
						}
						else if (!hasAtLeastOneQuestItem(player, 3375, 3376, 3378, 3379) && hasQuestItems(player, 3377))
						{
							htmltext = "30639-06.htm";
						}
						else if (!hasAtLeastOneQuestItem(player, 3375, 3376, 3377, 3378) && hasQuestItems(player, 3379))
						{
							giveItems(player, 3358, 1);
							takeItems(player, 3379, 1);
							if (hasQuestItems(player, 3354, 3357, 3355, 3356, 3359))
							{
								qs.setCond(4, true);
							}
							htmltext = "30639-07.htm";
						}
						else if (!hasAtLeastOneQuestItem(player, 3376, 3377, 3378, 3379) && hasQuestItems(player, 3375))
						{
							htmltext = "30639-08.htm";
						}
						else if (!hasAtLeastOneQuestItem(player, 3375, 3377, 3378, 3379) && hasQuestItems(player, 3376))
						{
							htmltext = "30639-09.htm";
						}
					}
					else
					{
						htmltext = "30639-10.htm";
					}
					break;
				}
				case 30640 :
				{
					if (!hasQuestItems(player, 3359))
					{
						if (!hasAtLeastOneQuestItem(player, 3385, 3386, 3387, 3388, 3389))
						{
							htmltext = "30640-01.htm";
						}
						else if (!hasAtLeastOneQuestItem(player, 3385, 3386, 3387, 3389) && hasQuestItems(player, 3388))
						{
							htmltext = "30640-05.htm";
						}
						else if (!hasAtLeastOneQuestItem(player, 3385, 3386, 3388, 3389) && hasQuestItems(player, 3387))
						{
							htmltext = "30640-06.htm";
						}
						else if (!hasAtLeastOneQuestItem(player, 3385, 3386, 3387, 3388) && hasQuestItems(player, 3389))
						{
							giveItems(player, 3359, 1);
							takeItems(player, 3389, 1);
							if (hasQuestItems(player, 3354, 3357, 3355, 3358, 3356))
							{
								qs.setCond(4, true);
							}
							htmltext = "30640-07.htm";
						}
						else if (!hasAtLeastOneQuestItem(player, 3386, 3387, 3388, 3389) && hasQuestItems(player, 3385))
						{
							htmltext = "30640-08.htm";
						}
						else if (!hasAtLeastOneQuestItem(player, 3385, 3387, 3388, 3389) && hasQuestItems(player, 3386))
						{
							htmltext = "30640-09.htm";
						}
					}
					else
					{
						htmltext = "30640-10.htm";
					}
					break;
				}
			}
		}
		else if (qs.isCompleted())
		{
			if (npc.getId() == 30634)
			{
				htmltext = getAlreadyCompletedMsg(player);
			}
		}
		return htmltext;
	}
	
	private static class MonsterData
	{
		private final int _crystalOfInprogress;
		private final int _crystalOfVictory;
		private final NpcStringId _npcStringId;
		
		protected MonsterData(int crystalOfInprogress, int crystalOfVictory, NpcStringId npcStringId)
		{
			_crystalOfInprogress = crystalOfInprogress;
			_crystalOfVictory = crystalOfVictory;
			_npcStringId = npcStringId;
		}
		
		protected int getCrystalOfInprogress()
		{
			return _crystalOfInprogress;
		}
		
		protected int getCrystalOfVictory()
		{
			return _crystalOfVictory;
		}
		
		protected NpcStringId getNpcStringId()
		{
			return _npcStringId;
		}
	}
	
	public static void main(String[] args)
	{
		new _230_TestOfSummoner(230, _230_TestOfSummoner.class.getSimpleName(), "");
	}
}