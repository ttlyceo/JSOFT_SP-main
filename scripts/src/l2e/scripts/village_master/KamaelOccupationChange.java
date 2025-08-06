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
package l2e.scripts.village_master;

import java.util.HashMap;
import java.util.Map;

import l2e.commons.apache.ArrayUtils;
import l2e.gameserver.data.parser.ClassMasterParser;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.base.ClassId;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.network.serverpackets.updatetype.UserInfoType;

/**
 * Create by LordWinter 28.14.2013 Based on L2J Eternity-World
 */
public final class KamaelOccupationChange extends Quest
{
	private final static String qn = "KamaelOccupationChange";
	
	private final String preffix = "32139";
	
	private static final int GWAINS_RECOMMENTADION = 9753;
	private static final int ORKURUS_RECOMMENDATION = 9760;
	private static final int STEELRAZOR_EVALUATION = 9772;
	private static final int KAMAEL_INQUISITOR_MARK = 9782;
	private static final int SOUL_BREAKER_CERTIFICATE = 9806;
	private static final int SHADOW_WEAPON_COUPON_DGRADE = 8869;
	private static final int SHADOW_WEAPON_COUPON_CGRADE = 8870;
	
	private static final Map<String, int[]> CLASSES = new HashMap<>();
	
	private static final int[] NPCS_MALE1 =
	{
		32139,
		32196,
		32199
	};
	
	private static final int[] NPCS_FEMALE1 =
	{
		32140,
		32193,
		32202
	};
	
	private static final int[] NPCS_MALE2 =
	{
		32146,
		32205,
		32209,
		32213,
		32217,
		32221,
		32225,
		32229,
		32233
	};
	
	private static final int[] NPCS_FEMALE2 =
	{
		32145,
		32206,
		32210,
		32214,
		32218,
		32222,
		32226,
		32230,
		32234
	};
	
	private static final int[] NPCS_ALL =
	{
		32139,
		32196,
		32199,
		32140,
		32193,
		32202,
		32146,
		32205,
		32209,
		32213,
		32217,
		32221,
		32225,
		32229,
		32233,
		32145,
		32206,
		32210,
		32214,
		32218,
		32222,
		32226,
		32230,
		32234
	};
	
	static
	{
		CLASSES.put("DR", new int[]
		{
			125,
			123,
			5,
			20,
			16,
			17,
			18,
			19,
			GWAINS_RECOMMENTADION,
			SHADOW_WEAPON_COUPON_DGRADE
		});
		
		CLASSES.put("WA", new int[]
		{
			126,
			124,
			5,
			20,
			20,
			21,
			22,
			23,
			STEELRAZOR_EVALUATION,
			SHADOW_WEAPON_COUPON_DGRADE
		});
		
		CLASSES.put("BE", new int[]
		{
			127,
			125,
			5,
			40,
			24,
			25,
			26,
			27,
			ORKURUS_RECOMMENDATION,
			SHADOW_WEAPON_COUPON_CGRADE
		});
		
		CLASSES.put("AR", new int[]
		{
			130,
			126,
			5,
			40,
			28,
			29,
			30,
			31,
			KAMAEL_INQUISITOR_MARK,
			SHADOW_WEAPON_COUPON_CGRADE
		});
		
		CLASSES.put("SBF", new int[]
		{
			129,
			126,
			5,
			40,
			40,
			41,
			42,
			43,
			SOUL_BREAKER_CERTIFICATE,
			SHADOW_WEAPON_COUPON_CGRADE
		});
		
		CLASSES.put("SBM", new int[]
		{
			128,
			125,
			5,
			40,
			40,
			41,
			42,
			43,
			SOUL_BREAKER_CERTIFICATE,
			SHADOW_WEAPON_COUPON_CGRADE
		});
	}
	
	public KamaelOccupationChange(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		for (final int id : NPCS_ALL)
		{
			addStartNpc(id);
			addTalkId(id);
		}
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		String htmltext = getNoQuestMsg(player);
		String suffix = "";
		final QuestState st = player.getQuestState(qn);
		if (st == null)
		{
			return null;
		}
		
		if (!CLASSES.containsKey(event))
		{
			return event;
		}
		
		final int req_class = CLASSES.get(event)[1];
		final int req_race = CLASSES.get(event)[2];
		final int req_level = CLASSES.get(event)[3];
		final int low_ni = CLASSES.get(event)[4];
		final int low_i = CLASSES.get(event)[5];
		final int ok_ni = CLASSES.get(event)[6];
		final int ok_i = CLASSES.get(event)[7];
		final int req_item = CLASSES.get(event)[8];
		final boolean item = st.hasQuestItems(req_item);
		if ((player.getRace().ordinal() == req_race) && (player.getClassId().getId() == req_class))
		{
			if (player.getLevel() < req_level)
			{
				suffix = "" + low_i;
				if (!item)
				{
					suffix = "" + low_ni;
				}
			}
			else
			{
				if (!item)
				{
					suffix = "" + ok_ni;
				}
				else
				{
					suffix = "" + ok_i;
					changeClass(st, player, event, req_item);
				}
			}
		}
		st.exitQuest(true);
		htmltext = preffix + "-" + suffix + ".htm";
		return htmltext;
	}
	
	@Override
	public String onTalk(final Npc npc, final Player player)
	{
		String htmltext = getNoQuestMsg(player);
		QuestState st = player.getQuestState(qn);
		if (st == null)
		{
			st = newQuestState(player);
		}
		
		if (player.isSubClassActive())
		{
			return htmltext;
		}
		
		final int race = player.getRace().ordinal();
		htmltext = preffix;
		if (race == 5)
		{
			final ClassId classId = player.getClassId();
			final int id = classId.getId();
			final int npcId = npc.getId();
			if (classId.level() >= 2)
			{
				htmltext += "-32.htm";
			}
			else if (ArrayUtils.contains(NPCS_MALE1, npcId) || ArrayUtils.contains(NPCS_FEMALE1, npcId))
			{
				if (id == 123)
				{
					htmltext = htmltext + "-01.htm";
				}
				else if (id == 124)
				{
					htmltext = htmltext + "-05.htm";
				}
			}
			else if (ArrayUtils.contains(NPCS_MALE2, npcId) || ArrayUtils.contains(NPCS_FEMALE2, npcId))
			{
				if (id == 125)
				{
					htmltext = htmltext + "-09.htm";
				}
				else if (id == 126)
				{
					htmltext = htmltext + "-35.htm";
				}
			}
		}
		else
		{
			htmltext += "-33.htm";
		}
		st.exitQuest(true);
		return htmltext;
	}
	
	private void changeClass(final QuestState st, final Player player, final String event, final int req_item)
	{
		final int newclass = CLASSES.get(event)[0];
		st.takeItems(req_item, 1);
		final var rewards = ClassMasterParser.getInstance().getGrandMasterRewards(CLASSES.get(event)[9] == 8869 ? 1 : 2);
		if (rewards != null && !rewards.isEmpty())
		{
			rewards.stream().filter(r -> r != null).forEach(it -> st.giveItems(it.getId(), it.getCountMax()));
		}
		st.playSound("ItemSound.quest_fanfare_2");
		player.setClassId(newclass);
		player.setBaseClass(newclass);
		player.broadcastCharInfo(UserInfoType.BASIC_INFO, UserInfoType.BASE_STATS, UserInfoType.MAX_HPCPMP, UserInfoType.STATS, UserInfoType.SPEED);
	}
	
	public static void main(String[] args)
	{
		new KamaelOccupationChange(-1, qn, "village_master");
	}
}