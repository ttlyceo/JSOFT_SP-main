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
import java.util.concurrent.ConcurrentHashMap;

import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.quest.State;
import l2e.gameserver.model.strings.server.ServerStorage;

/**
 * Rework by LordWinter 21.07.2023
 */
public class _662_AGameOfCards extends Quest
{
	private final static Map<Integer, CardGame> _games = new ConcurrentHashMap<>();

	public _662_AGameOfCards(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(30845);
		addTalkId(30845);

		addKillId(20677, 21109, 21112, 21116, 21114, 21004, 21002, 21006, 21008, 21010, 18001, 20672, 20673, 20674, 20955, 20962, 20961, 20959, 20958, 20966, 20965, 20968, 20973, 20972, 21278, 21279, 21280, 21286, 21287, 21288, 21520, 21526, 21530, 21535, 21508, 21510, 21513, 21515);

		questItemIds = new int[]
		{
		        8765
		};
	}

	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		final var st = player.getQuestState(getName());
		if (st == null)
		{
			return htmltext;
		}

		if (event.equalsIgnoreCase("30845-02.htm") && st.isCreated())
		{
			st.startQuest();
		}
		else if (event.equalsIgnoreCase("30845-07.htm") && st.isStarted())
		{
			st.exitQuest(true, true);
		}
		else if (event.equalsIgnoreCase("30845-03.htm") && st.isStarted() && (st.getQuestItemsCount(8765) >= 50))
		{
			htmltext = "30845-04.htm";
		}
		else if (event.equalsIgnoreCase("30845-10.htm") && st.isStarted())
		{
			if (st.getQuestItemsCount(8765) < 50)
			{
				return "30845-10a.htm";
			}
			st.takeItems(8765, 50);
			final int player_id = player.getObjectId();
			if (_games.containsKey(player_id))
			{
				_games.remove(player_id);
			}
			_games.put(player_id, new CardGame(player_id));
		}
		else if (event.equalsIgnoreCase("play") && st.isStarted())
		{
			final int player_id = player.getObjectId();
			if (!_games.containsKey(player_id))
			{
				return null;
			}
			return _games.get(player_id).playField(player);
		}
		else if (event.startsWith("card") && st.isStarted())
		{
			final int player_id = player.getObjectId();
			if (!_games.containsKey(player_id))
			{
				return null;
			}
			try
			{
				final int cardn = Integer.valueOf(event.replaceAll("card", ""));
				return _games.get(player_id).next(cardn, this, st, player);
			}
			catch (final Exception E)
			{
				return null;
			}
		}
		return htmltext;
	}

	@Override
	public String onTalk(Npc npc, Player player)
	{
		String htmltext = getNoQuestMsg(player);
		final var st = player.getQuestState(getName());
		if (st == null)
		{
			return htmltext;
		}

		switch (st.getState())
		{
			case State.CREATED :
				if (player.getLevel() < getMinLvl(getId()) || player.getLevel() > getMaxLvl(getId()))
				{
					st.exitQuest(true);
					htmltext = "30845-00.htm";
				}
				else
				{
					htmltext = "30845-01.htm";
				}
				break;
			case State.STARTED :
				htmltext = st.getQuestItemsCount(8765) < 50 ? "30845-03.htm" : "30845-04.htm";
				break;
		}
		return htmltext;
	}

	@Override
	public final String onKill(Npc npc, Player player, boolean isSummon)
	{
		final var partyMember = getRandomPartyMemberState(player, State.STARTED);
		if (partyMember == null)
		{
			return super.onKill(npc, player, isSummon);
		}
		
		final var st = partyMember.getQuestState(getName());
		if (st != null)
		{
			st.calcDropItems(getId(), 8765, npc.getId(), Integer.MAX_VALUE);
		}
		return super.onKill(npc, player, isSummon);
	}

	private static class CardGame
	{
		private final String[] cards = new String[5];
		private final int player_id;
		private final static String[] card_chars = new String[]
		{
		                "A",
		                "1",
		                "2",
		                "3",
		                "4",
		                "5",
		                "6",
		                "7",
		                "8",
		                "9",
		                "10",
		                "J",
		                "Q",
		                "K"
		};

		private final static String html_header = "<html><body>";
		private final static String html_footer = "</body></html>";
		private final static String table_header = "<table border=\"1\" cellpadding=\"3\"><tr>";
		private final static String table_footer = "</tr></table><br><br>";
		private final static String td_begin = "<center><td width=\"50\" align=\"center\"><br><br><br> ";
		private final static String td_end = " <br><br><br><br></td></center>";

		public CardGame(int _player_id)
		{
			player_id = _player_id;
			for (int i = 0; i < cards.length; i++)
			{
				cards[i] = "<a action=\"bypass -h Quest _662_AGameOfCards card" + i + "\">?</a>";
			}
		}

		public String next(int cardn, Quest q, QuestState st, Player player)
		{
			if ((cardn >= cards.length) || !cards[cardn].startsWith("<a"))
			{
				return null;
			}
			cards[cardn] = card_chars[getRandom(card_chars.length)];
			for (final String card : cards)
			{
				if (card.startsWith("<a"))
				{
					return playField(player);
				}
			}
			return finish(q, st, player);
		}

		private String finish(Quest q, QuestState st, Player player)
		{
			String result = html_header + table_header;
			final Map<String, Integer> matches = new HashMap<>();
			for (final String card : cards)
			{
				int count = matches.containsKey(card) ? matches.remove(card) : 0;
				count++;
				matches.put(card, count);
			}
			for (final String card : cards)
			{
				if (matches.get(card) < 2)
				{
					matches.remove(card);
				}
			}
			final var smatches = matches.keySet().toArray(new String[matches.size()]);
			final var cmatches = matches.values().toArray(new Integer[matches.size()]);
			String txt = "" + ServerStorage.getInstance().getString(player.getLang(), "662quest.NO_PAIRS") + "";
			if (cmatches.length == 1)
			{
				if (cmatches[0] == 5)
				{
					txt = "" + ServerStorage.getInstance().getString(player.getLang(), "662quest.5_PAIRS") + "";
					st.calcReward(q.getId(), 1);
				}
				else if (cmatches[0] == 4)
				{
					txt = "" + ServerStorage.getInstance().getString(player.getLang(), "662quest.4_PAIRS") + "";
					st.calcReward(q.getId(), 2);
				}
				else if (cmatches[0] == 3)
				{
					txt = "" + ServerStorage.getInstance().getString(player.getLang(), "662quest.3_PAIRS") + "";
					st.calcReward(q.getId(), 3);
				}
				else if (cmatches[0] == 2)
				{
					txt = "" + ServerStorage.getInstance().getString(player.getLang(), "662quest.1_PAIRS") + "";
					st.calcReward(q.getId(), 4);
				}
			}
			else if (cmatches.length == 2)
			{
				if ((cmatches[0] == 3) || (cmatches[1] == 3))
				{
					txt = "" + ServerStorage.getInstance().getString(player.getLang(), "662quest.FULL_HOUSE") + "";
					st.calcReward(q.getId(), 5);
				}
				else
				{
					txt = "" + ServerStorage.getInstance().getString(player.getLang(), "662quest.2_PAIRS") + "";
					st.calcReward(q.getId(), 6);
				}
			}

			for (final String card : cards)
			{
				if ((smatches.length > 0) && smatches[0].equalsIgnoreCase(card))
				{
					result += td_begin + "<font color=\"55FD44\">" + card + "</font>" + td_end;
				}
				else if ((smatches.length == 2) && smatches[1].equalsIgnoreCase(card))
				{
					result += td_begin + "<font color=\"FE6666\">" + card + "</font>" + td_end;
				}
				else
				{
					result += td_begin + card + td_end;
				}
			}

			result += table_footer + txt;
			if (st.getQuestItemsCount(8765) >= 50)
			{
				result += "<br><br><a action=\"bypass -h Quest _662_AGameOfCards 30845-10.htm\">" + ServerStorage.getInstance().getString(player.getLang(), "662quest.PLAY_AGAIN") + "</a>";
			}
			result += html_footer;
			_games.remove(player_id);
			return result;
		}

		public String playField(Player player)
		{
			String result = html_header + table_header;
			for (final String card : cards)
			{
				result += td_begin + card + td_end;
			}
			result += table_footer + "" + ServerStorage.getInstance().getString(player.getLang(), "662quest.NEXT_CARD") + "" + html_footer;
			return result;
		}
	}

	public static void main(String[] args)
	{
		new _662_AGameOfCards(662, _662_AGameOfCards.class.getSimpleName(), "");
	}
}
