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

import java.util.ArrayList;
import java.util.List;

import l2e.gameserver.Config;
import l2e.gameserver.data.parser.MultiSellParser;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.quest.State;
import l2e.gameserver.network.NpcStringId;

/**
 * Created by LordWinter 08.12.2010 Based on L2J Eternity-World
 */
public class NewbieGuideSystem extends Quest
{
	private static final List<Integer> guides = new ArrayList<>();

	private static final int SS_NOVICE = 5789;
	private static final int SPS_NOVICE = 5790;
	private static final int SCROLL_ID = 8594;
	private static final int COUPON_ONE = 7832;
	private static final int COUPON_TWO = 7833;

	@Override
	public final String onAdvEvent(String event, Npc npc, Player player)
	{
		QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			st = newQuestState(player);
		}

		final int reward = st.getInt("reward");
		final int level = player.getLevel();
		if (event.equalsIgnoreCase("newbie_give_weapon_coupon"))
		{
			if ((level < 6) || (level > 19) || (player.getPkKills() > 0))
			{
				return "no-weapcoups-1.htm";
			}
			if (st.getInt("reward") >= 2)
			{
				return "no-weapcoups.htm";
			}
			showOnScreenMsg(player, NpcStringId.ACQUISITION_OF_WEAPON_EXCHANGE_COUPON_FOR_BEGINNERS_COMPLETE_N_GO_SPEAK_WITH_THE_NEWBIE_GUIDE, 2, 5000);
			st.giveItems(COUPON_ONE, 1);
			st.giveItems(COUPON_ONE, 1);
			st.giveItems(COUPON_ONE, 1);
			st.giveItems(COUPON_ONE, 1);
			st.giveItems(COUPON_ONE, 1);
			st.set("reward", "2");
			return "coupons-list.htm";
		}
		else if (event.equalsIgnoreCase("newbie_show_weapon"))
		{
			if ((level < 6) || (level > 19) || (player.getPkKills() > 0))
			{
				return "no-weapon-warehouse.htm";
			}
			MultiSellParser.getInstance().separateAndSend(305986001, player, npc, false);
			return player.isHFClient() ? "newbieitems-list.htm" : "";
		}
		else if (event.equalsIgnoreCase("newbie_give_armor_coupon"))
		{
			if (reward == 3)
			{
				return "no-armorcoups.htm";
			}
			if ((level > 19) && (level < 40) && (player.getPkKills() < 5) && (player.getClassId().level() == 1))
			{
				st.giveItems(COUPON_TWO, 1);
				st.set("reward", "3");
				return "armorcoups.htm";
			}
			else if ((level < 20) || (player.getPkKills() > 0) || (player.getClassId().level() != 1))
			{
				return "no-armorcoups-1.htm";
			}
		}
		else if (event.equalsIgnoreCase("newbie_show_armor"))
		{
			if ((level < 20) || (player.getPkKills() > 0) || (player.getClassId().level() != 1))
			{
				return "no-armor-warehouse.htm";
			}
			MultiSellParser.getInstance().separateAndSend(305986002, player, npc, false);
			return player.isHFClient() ? "newbieitems-list.htm" : "";
		}
		npc.showChatWindow(player);
		return "";
	}

	@Override
	public final String onFirstTalk(Npc npc, Player player)
	{
		if (Config.DISABLE_TUTORIAL)
		{
			npc.showChatWindow(player);
			return null;
		}
		
		QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			st = newQuestState(player);
		}

		final int npcId = npc.getId();
		final int step = st.getInt("step");
		final int reward = st.getInt("reward");
		final int level = player.getLevel();
		final boolean isMage = player.getClassId().isMage();
		switch (npcId)
		{
			case 30598: // Human
				if (reward == 0)
				{
					if (isMage)
					{
						st.playTutorialVoice("tutorial_voice_027");
						st.giveItems(SPS_NOVICE, 100);
					}
					else
					{
						st.playTutorialVoice("tutorial_voice_026");
						st.giveItems(SS_NOVICE, 200);
					}
					st.giveItems(SCROLL_ID, 2);
					st.addExpAndSp(68, 50);
					st.set("reward", "1");
					st.set("step", "1");
					st.setState(State.STARTED);
					st.addRadar(-84436, 242793, -3720);
					return "Human01.htm";
				}
				if ((level < 6) && (reward >= 1))
				{
					final var qs = player.getQuestState("_001_LettersOfLove");
					if (qs == null || qs.isStarted())
					{
						st.addRadar(-84436, 242793, -3720);
						return "Human011.htm";
					}
					if (qs.isCompleted())
					{
						if (step == 1)
						{
							st.rewardItems(57, 695);
							st.addExpAndSp(3154, 127);
							st.set("step", "2");
							st.addRadar(-82236, 241573, -3728);
							return "Human02.htm";
						}
						st.addRadar(-82236, 241573, -3728);
						return "Human022.htm";
					}
				}
				if ((level > 5) && (level < 40) && (player.getPkKills() == 0) && (player.getClassId().level() == 0) && (reward == 1))
				{
					return "Level6.htm";
				}
				if ((level > 5) && (level < 10) && (reward >= 1) && (step < 3))
				{
					if ((st.getQuestItemsCount(SS_NOVICE) >= 5000) || (st.getQuestItemsCount(SPS_NOVICE) >= 2000))
					{
						st.rewardItems(57, 11567);
						st.addExpAndSp(36942, 1541);
						st.set("step", "3");
						if (isMage)
						{
							st.addRadar(-91008, 248016, -3560);
							return "Human03m.htm";
						}
						st.addRadar(-71384, 258304, -3104);
						return "Human03.htm";
					}
					
					final var qs = player.getQuestState("_257_TheGuardIsBusy");
					if (qs == null || qs.isStarted())
					{
						st.addRadar(-82236, 241573, -3728);
						return "Human022.htm";
					}
				}
				if ((level > 9) && (level < 15) && (reward >= 1) && (step < 4))
				{
					if (isMage)
					{
						final var qs = player.getQuestState("_104_SpiritOfMirrors");
						if (qs == null || qs.isStarted())
						{
							st.addRadar(-91008, 248016, -3560);
							return "Human033m.htm";
						}
						
						if (qs.isCompleted())
						{
							st.rewardItems(57, 31752);
							st.addExpAndSp(152653, 6914);
							st.set("step", "4");
							st.addRadar(-84057, 242832, -3728);
							return "Human04.htm";
						}
					}
					
					final var qs = player.getQuestState("_101_SwordOfSolidarity");
					if (qs == null || qs.isStarted())
					{
						st.addRadar(-71384, 258304, -3104);
						return "Human033.htm";
					}
					
					if (qs.isCompleted())
					{
						st.rewardItems(57, 31752);
						st.addExpAndSp(152653, 6914);
						st.set("step", "4");
						st.addRadar(-84057, 242832, -3728);
						return "Human04.htm";
					}
				}
				if ((level > 14) && (level < 18) && (reward >= 1) && (step < 5))
				{
					final var qs = player.getQuestState("_151_CureForFeverDisease");
					if (qs == null || qs.isStarted())
					{
						st.addRadar(-84057, 242832, -3728);
						return "Human04.htm";
					}
					
					if (qs.isCompleted())
					{
						st.rewardItems(57, 13648);
						st.addExpAndSp(285670, 58155);
						st.set("step", "5");
						return "Human05.htm";
					}
				}
				if ((level > 17) || (step > 4))
				{
					return "Human05.htm";
				}
				break;
			case 30599: // Elven
				if (reward == 0)
				{
					if (isMage)
					{
						st.playTutorialVoice("tutorial_voice_027");
						st.giveItems(SPS_NOVICE, 100);
					}
					else
					{
						st.playTutorialVoice("tutorial_voice_026");
						st.giveItems(SS_NOVICE, 200);
					}
					st.giveItems(SCROLL_ID, 2);
					st.addExpAndSp(68, 50);
					st.setState(State.STARTED);
					st.set("reward", "1");
					st.set("step", "1");
					st.addRadar(42978, 49115, -2992);
					return "Elven01.htm";
				}
				if ((level < 6) && (reward >= 1))
				{
					final var qs = player.getQuestState("_002_WhatWomenWant");
					if (qs == null || qs.isStarted())
					{
						st.addRadar(42978, 49115, -2992);
						return "Elven011.htm";
					}
					
					if (qs.isCompleted())
					{
						if (step == 1)
						{
							st.rewardItems(57, 695);
							st.addExpAndSp(3154, 127);
							st.set("step", "2");
							st.addRadar(42812, 51138, -2992);
							return "Elven02.htm";
						}
						st.addRadar(42812, 51138, -2992);
						return "Elven022.htm";
					}
				}
				if ((level > 5) && (level < 40) && (player.getPkKills() > 0) && (player.getClassId().level() == 0) && (reward == 1))
				{
					return "Level6.htm";
				}
				if ((level > 5) && (level < 10) && (reward >= 1) && (step < 3))
				{
					if ((st.getQuestItemsCount(SS_NOVICE) >= 5000) || (st.getQuestItemsCount(SPS_NOVICE) >= 2000))
					{
						st.rewardItems(57, 11567);
						st.addExpAndSp(36942, 1541);
						st.set("step", "3");
						st.addRadar(47595, 51569, -2992);
						return "Elven03.htm";
					}
					
					final var qs = player.getQuestState("_260_HuntTheOrcs");
					if (qs == null || qs.isStarted())
					{
						st.addRadar(42812, 51138, -2992);
						return "Elven022.htm";
					}
				}
				if ((level > 9) && (level < 15) && (reward >= 1) && (step < 4))
				{
					final var qs = player.getQuestState("_105_SkirmishWithOrcs");
					if (qs == null || qs.isStarted())
					{
						st.addRadar(47595, 51569, -2992);
						return "Elven03.htm";
					}
					
					if (qs.isCompleted())
					{
						st.rewardItems(57, 31752);
						st.addExpAndSp(152653, 6914);
						st.set("step", "4");
						st.addRadar(45859, 50827, -3056);
						return "Elven04.htm";
					}
				}
				if ((level > 14) && (level < 18) && (reward >= 1) && (step < 5))
				{
					final int finalStep = st.getInt("finalStep");
					switch (finalStep)
					{
						case 0 :
							st.addRadar(45859, 50827, -3056);
							return "Elven04.htm";
						case 1 :
							st.rewardItems(57, 13648);
							st.addExpAndSp(285670, 58155);
							st.set("step", "5");
							return "Elven05.htm";
					}
				}
				if ((level > 17) || (step > 4))
				{
					return "Elven05.htm";
				}
				break;
			case 30600: // Dark Elven
				if (reward == 0)
				{
					if (isMage)
					{
						st.playTutorialVoice("tutorial_voice_027");
						st.giveItems(SPS_NOVICE, 100);
					}
					else
					{
						st.playTutorialVoice("tutorial_voice_026");
						st.giveItems(SS_NOVICE, 200);
					}
					st.giveItems(SCROLL_ID, 2);
					st.addExpAndSp(68, 50);
					st.setState(State.STARTED);
					st.set("reward", "1");
					st.set("step", "1");
					st.addRadar(25856, 10832, -3736);
					return "Delf01.htm";
				}
				if ((level < 6) && (reward >= 1))
				{
					final var qs = player.getQuestState("_166_MassOfDarkness");
					if (qs == null || qs.isStarted())
					{
						st.addRadar(25856, 10832, -3736);
						return "Delf011.htm";
					}
					
					if (qs.isCompleted())
					{
						if (step == 1)
						{
							st.rewardItems(57, 695);
							st.addExpAndSp(3154, 127);
							st.set("step", "2");
							st.addRadar(7644, 18048, -4392);
							return "Delf02.htm";
						}
						st.addRadar(7644, 18048, -4392);
						return "Delf022.htm";
					}
				}
				if ((level > 5) && (level < 40) && (player.getPkKills() > 0) && (player.getClassId().level() == 0) && (reward == 1))
				{
					return "Level6.htm";
				}
				if ((level > 5) && (level < 10) && (reward >= 1) && (step < 3))
				{
					if ((st.getQuestItemsCount(SS_NOVICE) >= 5000) || (st.getQuestItemsCount(SPS_NOVICE) >= 2000))
					{
						st.rewardItems(57, 11567);
						st.addExpAndSp(36942, 1541);
						st.set("step", "3");
						if (isMage)
						{
							st.addRadar(10775, 14190, -4256);
							return "Delf03m.htm";
						}
						st.addRadar(10584, 17581, -4568);
						return "Delf03.htm";
					}
					
					final var qs = player.getQuestState("_265_ChainsOfSlavery");
					if (qs == null || qs.isStarted())
					{
						st.addRadar(7644, 18048, -4392);
						return "Delf022.htm";
					}
				}
				if ((level > 9) && (level < 15) && (reward >= 1) && (step < 4))
				{
					if (isMage)
					{
						final var qs = player.getQuestState("_106_ForgottenTruth");
						if (qs == null || qs.isStarted())
						{
							st.addRadar(10775, 14190, -4256);
							return "Delf033m.htm";
						}
						
						if (qs.isCompleted())
						{
							st.rewardItems(57, 31752);
							st.addExpAndSp(152653, 6914);
							st.set("step", "4");
							st.addRadar(11258, 14431, -4256);
							return "Delf04.htm";
						}
					}
					
					final var qs = player.getQuestState("_103_SpiritOfCraftsman");
					if (qs == null || qs.isStarted())
					{
						st.addRadar(10584, 17581, -4568);
						return "Delf033.htm";
					}
					
					if (qs.isCompleted())
					{
						st.rewardItems(57, 31752);
						st.addExpAndSp(152653, 6914);
						st.set("step", "4");
						st.addRadar(11258, 14431, -4256);
						return "Delf04.htm";
					}
				}
				if ((level > 14) && (level < 18) && (reward >= 1) && (step < 5))
				{
					final var qs = player.getQuestState("_169_NightmareChildren");
					if (qs == null || qs.isStarted())
					{
						st.addRadar(11258, 14431, -4256);
						return "Delf04.htm";
					}
					
					if (qs.isCompleted())
					{
						st.rewardItems(57, 13648);
						st.addExpAndSp(285670, 58155);
						st.set("step", "5");
						return "Delf05.htm";
					}
				}
				if ((level > 17) || (step > 4))
				{
					return "Delf05.htm";
				}
				break;
			case 30601: // Dwarf
				if (reward == 0)
				{
					if (isMage)
					{
						st.playTutorialVoice("tutorial_voice_027");
						st.giveItems(SPS_NOVICE, 100);
					}
					else
					{
						st.playTutorialVoice("tutorial_voice_026");
						st.giveItems(SS_NOVICE, 200);
					}
					st.giveItems(SCROLL_ID, 2);
					st.addExpAndSp(68, 50);
					st.setState(State.STARTED);
					st.set("reward", "1");
					st.set("step", "1");
					st.addRadar(112656, -174864, -608);
					return "Dwarf01.htm";
				}
				if ((level < 6) && (reward >= 1))
				{
					final var qs = player.getQuestState("_005_MinersFavor");
					if (qs == null || qs.isStarted())
					{
						st.addRadar(112656, -174864, -608);
						return "Dwarf011.htm";
					}
					
					if (qs.isCompleted())
					{
						if (step == 1)
						{
							st.rewardItems(57, 695);
							st.addExpAndSp(3154, 127);
							st.set("step", "2");
							st.addRadar(116103, -178407, -944);
							return "Dwarf02.htm";
						}
						st.addRadar(116103, -178407, -944);
						return "Dwarf022.htm";
					}
				}
				if ((level > 5) && (level < 40) && (player.getPkKills() > 0) && (player.getClassId().level() == 0) && (reward == 1))
				{
					return "Level6.htm";
				}
				if ((level > 5) && (level < 10) && (reward >= 1) && (step < 3))
				{
					if ((st.getQuestItemsCount(SS_NOVICE) >= 5000) || (st.getQuestItemsCount(SPS_NOVICE) >= 2000))
					{
						st.rewardItems(57, 11567);
						st.addExpAndSp(36942, 1541);
						st.set("step", "3");
						st.addRadar(115717, -183488, -1472);
						return "Dwarf03.htm";
					}
					
					final var qs = player.getQuestState("_293_HiddenVein");
					if (qs == null || qs.isStarted())
					{
						st.addRadar(116103, -178407, -944);
						return "Dwarf022.htm";
					}
				}
				if ((level > 9) && (level < 15) && (reward >= 1) && (step < 4))
				{
					final var qs = player.getQuestState("_108_JumbleTumbleDiamondFuss");
					if (qs == null || qs.isStarted())
					{
						st.addRadar(115717, -183488, -1472);
						return "Dwarf033.htm";
					}
					
					if (qs.isCompleted())
					{
						st.rewardItems(57, 31752);
						st.addExpAndSp(152653, 6914);
						st.set("step", "4");
						st.addRadar(116268, -177524, -880);
						return "Dwarf04.htm";
					}
				}
				if ((level > 14) && (level < 18) && (reward >= 1) && (step < 5))
				{
					final int finalStep = st.getInt("finalStep");
					switch (finalStep)
					{
						case 0 :
							st.addRadar(116268, -177524, -880);
							return "Dwarf04.htm";
						case 1 :
							st.rewardItems(57, 13648);
							st.addExpAndSp(285670, 58155);
							st.set("step", "5");
							return "Dwarf05.htm";
					}
				}
				if ((level > 17) || (step > 4))
				{
					return "Dwarf05.htm";
				}
				break;
			case 30602: // Orc
				if (reward == 0)
				{
					if (isMage)
					{
						st.playTutorialVoice("tutorial_voice_027");
						st.giveItems(SPS_NOVICE, 100);
					}
					else
					{
						st.playTutorialVoice("tutorial_voice_026");
						st.giveItems(SS_NOVICE, 200);
					}
					st.giveItems(SCROLL_ID, 2);
					st.addExpAndSp(68, 50);
					st.setState(State.STARTED);
					st.set("reward", "1");
					st.set("step", "1");
					st.addRadar(-47360, -113791, -224);
					return "Orc01.htm";
				}
				if ((level < 6) && (reward >= 1))
				{
					final var qs = player.getQuestState("_004_LongLiveThePaagrioLord");
					if (qs == null || qs.isStarted())
					{
						st.addRadar(-47360, -113791, -224);
						return "Orc011.htm";
					}
					
					if (qs.isCompleted())
					{
						if (step == 1)
						{
							st.rewardItems(57, 695);
							st.addExpAndSp(3154, 127);
							st.set("step", "2");
							st.addRadar(-46802, -114011, -112);
							return "Orc02.htm";
						}
						st.addRadar(-46802, -114011, -112);
						return "Orc022.htm";
					}
				}
				if ((level > 5) && (level < 40) && (player.getPkKills() > 0) && (player.getClassId().level() == 0) && (reward == 1))
				{
					return "Level6.htm";
				}
				if ((level > 5) && (level < 10) && (reward >= 1) && (step < 3))
				{
					if ((st.getQuestItemsCount(SS_NOVICE) >= 5000) || (st.getQuestItemsCount(SPS_NOVICE) >= 2000))
					{
						st.rewardItems(57, 11567);
						st.addExpAndSp(36942, 1541);
						st.set("step", "3");
						st.addRadar(-46808, -113184, -112);
						return "Orc03.htm";
					}
					
					final var qs = player.getQuestState("_273_InvadersOfHolyland");
					if (qs == null || qs.isStarted())
					{
						st.addRadar(-46802, -114011, -112);
						return "Orc022.htm";
					}
				}
				if ((level > 9) && (level < 15) && (reward >= 1) && (step < 4))
				{
					final var qs = player.getQuestState("_107_MercilessPunishment");
					if (qs == null || qs.isStarted())
					{
						st.addRadar(-46808, -113184, -112);
						return "Orc033.htm";
					}
					
					if (qs.isCompleted())
					{
						st.rewardItems(57, 31752);
						st.addExpAndSp(152653, 6914);
						st.set("step", "4");
						st.addRadar(-45863, -112621, -200);
						return "Orc04.htm";
					}
				}
				if ((level > 14) && (level < 18) && (reward >= 1) && (step < 5))
				{
					final int finalStep = st.getInt("finalStep");
					switch (finalStep)
					{
						case 0 :
							st.addRadar(-45863, -112621, -200);
							return "Orc04.htm";
						case 1 :
							st.rewardItems(57, 13648);
							st.addExpAndSp(285670, 58155);
							st.set("step", "5");
							return "Orc05.htm";
					}
				}
				if ((level > 17) || (step > 4))
				{
					return "Orc05.htm";
				}
				break;
			case 32135: // Kamael
				if (reward == 0)
				{
					if (isMage)
					{
						st.playTutorialVoice("tutorial_voice_027");
						st.giveItems(SPS_NOVICE, 100);
					}
					else
					{
						st.playTutorialVoice("tutorial_voice_026");
						st.giveItems(SS_NOVICE, 200);
					}
					st.giveItems(SCROLL_ID, 2);
					st.addExpAndSp(68, 50);
					st.setState(State.STARTED);
					st.set("reward", "1");
					st.set("step", "1");
					st.addRadar(-119378, 49242, 8);
					return "Kamael01.htm";
				}
				if ((level < 6) && (reward >= 1))
				{
					final var qs = player.getQuestState("_174_SupplyCheck");
					if (qs == null || qs.isStarted())
					{
						st.addRadar(-119378, 49242, 8);
						return "Kamael011.htm";
					}
					
					if (qs.isCompleted())
					{
						if (step == 1)
						{
							st.rewardItems(57, 695);
							st.addExpAndSp(3154, 127);
							st.set("step", "2");
							st.addRadar(-119378, 49242, 8);
							return "Kamael02.htm";
						}
						st.addRadar(-119378, 49242, 8);
						return "Kamael022.htm";
					}
				}
				if ((level > 5) && (level < 40) && (player.getPkKills() > 0) && (player.getClassId().level() == 0) && (reward == 1))
				{
					return "Level6.htm";
				}
				if ((level > 5) && (level < 10) && (reward >= 1) && (step < 3))
				{
					if ((st.getQuestItemsCount(SS_NOVICE) >= 5000) || (st.getQuestItemsCount(SPS_NOVICE) >= 2000))
					{
						st.rewardItems(57, 11567);
						st.addExpAndSp(36942, 1541);
						st.set("step", "3");
						st.addRadar(-118080, 42835, 712);
						return "Kamael03.htm";
					}
					
					final var qs = player.getQuestState("_281_HeadForTheHills");
					if (qs == null || qs.isStarted())
					{
						st.addRadar(-119378, 49242, 8);
						return "Kamael022.htm";
					}
				}
				if ((level > 9) && (level < 15) && (reward >= 1) && (step < 4))
				{
					final var qs = player.getQuestState("_175_TheWayOfTheWarrior");
					if (qs == null || qs.isStarted())
					{
						st.addRadar(-118080, 42835, 712);
						return "Kamael033.htm";
					}
					
					if (qs.isCompleted())
					{
						st.rewardItems(57, 31752);
						st.addExpAndSp(152653, 6914);
						st.set("step", "4");
						st.addRadar(-125872, 38208, 1232);
						return "Kamael04.htm";
					}
				}
				if ((level > 14) && (level < 18) && (reward >= 1) && (step < 5))
				{
					final int finalStep = st.getInt("finalStep");
					switch (finalStep)
					{
						case 0 :
							st.addRadar(-125872, 38208, 1232);
							return "Kamael04.htm";
						case 1 :
							st.rewardItems(57, 13648);
							st.addExpAndSp(285670, 58155);
							st.set("step", "5");
							return "Kamael05.htm";
					}
				}
				if ((level > 17) || (step > 4))
				{
					return "Kamael05.htm";
				}
				break;
			case 31076:
			case 31077:
				npc.showChatWindow(player);
				return "";

		}
		npc.showChatWindow(player);
		return "";
	}

	@Override
	public final String onTalk(Npc npc, Player player)
	{
		QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			st = newQuestState(player);
		}

		if (guides.contains(npc.getId()))
		{
			return "newbieitems-list.htm";
		}
		npc.showChatWindow(player);
		return "";
	}

	public NewbieGuideSystem(int id, String name, String desc)
	{
		super(id, name, desc);
		final int[] ids =
		{
		                30598,
		                30599,
		                30600,
		                30601,
		                30602,
		                31076,
		                31077,
		                32135
		};
		for (final int i : ids)
		{
			addStartNpc(i);
			addFirstTalkId(i);
			addTalkId(i);
			guides.add(i);
		}
	}

	public static void main(String[] args)
	{
		new NewbieGuideSystem(-1, NewbieGuideSystem.class.getSimpleName(), "custom");
	}
}