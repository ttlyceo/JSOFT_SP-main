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

import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.quest.State;
import l2e.gameserver.model.strings.server.ServerStorage;

/**
 * Rework by LordWinter 25.05.2021
 */
public class _373_SupplierOfReagents extends Quest
{
	private static final int[][] DROPLIST_COND =
	{
	        {
	                20813, 6019
			},
			{
			        20813, 6014
			},
			{
			        20822, 6018
			},
			{
			        20822, 6007
			},
			{
			        21061, 6015
			},
			{
			        21061, 6013
			},
			{
			        20828, 6008
			},
			{
			        20828, 6019
			},
			{
			        21066, 6010
			},
			{
			        21111, 6011
			},
			{
			        21111, 6012
			},
			{
			        21115, 6009
			},
			{
			        21115, 6019
			}
	};

	private static final Object[][] ITEMS =
	{
	        {
	                4042, "etc_gem_red_i00", "Enria", ""
			},
			{
			        4043, "etc_gem_blue_i00", "Asofe", ""
			},
			{
			        4044, "etc_gem_clear_i00", "Thons", ""
			},
			{
			        2508, "etc_piece_bone_red_i00", "Cursed Bone", ""
			},
			{
			        735, "etc_reagent_green_i00", "Potion of Alacrity", ""
			},
			{
			        737, "etc_scroll_of_resurrection_i00", "Scroll of Resurrection", ""
			},
			{
			        4953, "etc_recipe_red_i00", "Recipe: Avadon Gloves (60%)", ""
			},
			{
			        4960, "etc_recipe_red_i00", "Recipe: Zubei's Gauntlets (60%)", ""
			},
			{
			        4959, "etc_recipe_red_i00", "Recipe: Avadon Boots (60%)", ""
			},
			{
			        4958, "etc_recipe_red_i00", "Recipe: Zubei's Boots (60%)", ""
			},
			{
			        4998, "etc_recipe_red_i00", "Recipe: Blue Wolf Gloves (60%)", ""
			},
			{
			        4992, "etc_recipe_red_i00", "Recipe: Blue Wolf Boots (60%)", ""
			},
			{
			        4993, "etc_recipe_red_i00", "Recipe: Doom Gloves (60%)", ""
			},
			{
			        4999, "etc_recipe_red_i00", "Recipe: Doom Boots (60%)", ""
			},
			{
			        5524, "etc_letter_red_i00", "Sealed Dark Crystal Gaiters Pattern", ""
			},
			{
			        5478, "etc_letter_red_i00", "Sealed Dark Crystal Leather Armor Pattern", ""
			},
			{
			        5520, "etc_letter_red_i00", "Sealed Dark Crystal Breastplate Pattern", ""
			},
			{
			        5479, "etc_letter_red_i00", "Sealled Tallum Leather Armor Pattern", ""
			},
			{
			        5521, "etc_letter_red_i00", "Sealed Tallum Plate Armor Pattern", ""
			},
			{
			        5480, "etc_leather_gray_i00", "Sealed Leather Armor of Nightmare Fabric", ""
			},
			{
			        5481, "etc_leather_gray_i00", "Sealed Majestic Leather Armor Fabric", ""
			},
			{
			        5522, "etc_letter_red_i00", "Sealed Armor of Nightmare Pattern", ""
			},
			{
			        5523, "etc_letter_red_i00", "Sealed Majestic Plate Armor Pattern", ""
			},
			{
			        103, "shield_tower_shield_i00", "Tower Shield", "Shield"
			},
			{
			        2437, "armor_t21_b_i00", "Drake Leather Boots", "Boots"
			},
			{
			        630, "shield_square_shield_i00", "Square Shield", "Shield"
			},
			{
			        612, "armor_t64_g_i00", "Zubei's Gauntlets", "Gloves"
			},
			{
			        2464, "armor_t66_g_i00", "Avadon Gloves", "Gloves"
			},
			{
			        554, "armor_t64_b_i00", "Zubei's Boots", "Boots"
			},
			{
			        600, "armor_t66_b_i00", "Avadon Boots", "Boots"
			},
			{
			        601, "armor_t68_b_i00", "Blue Wolf Boots", "Boots"
			},
			{
			        2439, "armor_t71_b_i00", "Boots of Doom", "Boots"
			},
			{
			        2475, "armor_t68_g_i00", "Blue Wolf Gloves", "Gloves"
			},
			{
			        2487, "armor_t71_g_i00", "Doom Gloves", "Gloves"
			},
			{
			        6011, "etc_reagent_red_i00", "Wyrm's Blood", ""
			},
			{
			        6012, "etc_inf_ore_high_i00", "Lava Stone", ""
			},
			{
			        6013, "etc_broken_crystal_silver_i00", "Moonstone Shard", ""
			},
			{
			        6014, "etc_piece_bone_black_i00", "Rotten Bone Piece", ""
			},
			{
			        6015, "etc_reagent_green_i00", "Demon's Blood", ""
			},
			{
			        6016, "etc_inf_ore_least_i00", "Infernium Ore", "Low Level Reagent"
			},
			{
			        6017, "etc_ginseng_red_i00", "Blood Root", ""
			},
			{
			        6018, "etc_powder_gray_i00", "Volcanic Ash", ""
			},
			{
			        6019, "etc_reagent_silver_i00", "Quicksilver", ""
			},
			{
			        6020, "etc_powder_orange_i00", "Sulfur", ""
			},
			{
			        6021, "etc_dragons_blood_i05", "Dracoplasm", "Low Level Reagent"
			},
			{
			        6022, "etc_powder_red_i00", "Magma Dust", ""
			},
			{
			        6023, "etc_powder_white_i00", "Moon Dust", "Low Level Reagent"
			},
			{
			        6024, "etc_potion_purpel_i00", "Necroplasm", "Low Level Reagent"
			},
			{
			        6025, "etc_potion_green_i00", "Demonplasm", "Low Level Reagent"
			},
			{
			        6026, "etc_powder_black_i00", "Inferno Dust", ""
			},
			{
			        6027, "etc_dragon_blood_i00", "Draconic Essence", "High Level Reagent"
			},
			{
			        6028, "etc_dragons_blood_i00", "Fire Essence", "High Level Reagent"
			},
			{
			        6029, "etc_mithril_ore_i00", "Lunargent", "High Level Reagent"
			},
			{
			        6030, "etc_dragons_blood_i02", "Midnight Oil", "High Level Reagent"
			},
			{
			        6031, "etc_dragons_blood_i05", "Demonic Essence", "High Level Reagent"
			},
			{
			        6032, "etc_dragons_blood_i04", "Abyss Oil", "High Level Reagent"
			},
			{
			        6033, "etc_luxury_wine_b_i00", "Hellfire Oil", "Highest Level Reagent"
			},
			{
			        6034, "etc_luxury_wine_c_i00", "Nightmare Oil", "Highest Level Reagent"
			},
			{
			        6320, "etc_broken_crystal_silver_i00", "Pure Silver", ""
			},
			{
			        6321, "etc_broken_crystal_gold_i00", "True Gold", ""
			},
	};

	private static final int[][] FORMULAS =
	{
	        {
	                6021, 6011, 10, 6017, 1
			},
			{
			        6022, 6012, 10, 6018, 1
			},
			{
			        6023, 6013, 10, 6018, 1
			},
			{
			        6024, 6014, 10, 6017, 1
			},
			{
			        6025, 6015, 10, 6017, 1
			},
			{
			        6026, 6016, 10, 6018, 1
			},
			{
			        6027, 6021, 10, 6019, 1
			},
			{
			        6028, 6022, 10, 6020, 1
			},
			{
			        6029, 6023, 10, 6019, 1
			},
			{
			        6030, 6024, 10, 6019, 1
			},
			{
			        6031, 6025, 10, 6020, 1
			},
			{
			        6032, 6026, 10, 6020, 1
			},
			{
			        6033, 6028, 1, 6031, 1
			},
			{
			        6034, 6029, 1, 6030, 1
			},
			{
			        6320, 6029, 1, 6019, 1
			},
			{
			        6319, 6320, 1, 6321, 1
			}
	};

	private static final int[][] TEMPERATURE =
	{
	        {
	                1, 100, 1
			},
			{
			        2, 45, 2
			},
			{
			        3, 15, 3
			}
	};

	public _373_SupplierOfReagents(int id, String name, String descr)
	{
		super(id, name, descr);

		addStartNpc(30166);
		addTalkId(30166, 31149);

		for (final int[] npc : DROPLIST_COND)
		{
			addKillId(npc[0]);
		}
		
		questItemIds = new int[]
		{
		        5904, 6317
		};
	}

	public String render_urn(QuestState st, String[] page)
	{
		String html;
		final int stone = st.getInt("mixing");
		final int ingredient = st.getInt("ingredient");
		final int catalyst = st.getInt("catalyst");
		if (page == null)
		{
			html = "<html>" + "<body>" + ServerStorage.getInstance().getString(st.getPlayer().getLang(), "373quest.URN") + ":" + "<br><table border=0 width=300><tr>" + "<tr><td width=50%>" + "<a action=\"bypass -h Quest _373_SupplierOfReagents U_M_MACT\">" + ServerStorage.getInstance().getString(st.getPlayer().getLang(), "373quest.MIXING_STONE") + "</a></td><td></td></tr>"
			                + "<tr><td><a action=\"bypass -h Quest _373_SupplierOfReagents U_I_IACT\">" + ServerStorage.getInstance().getString(st.getPlayer().getLang(), "373quest.INGREDIENTS") + "</a></td><td>" + ServerStorage.getInstance().getString(st.getPlayer().getLang(), "373quest.CURRENT_INGR") + "</td></tr>"
			                + "<tr><td><a action=\"bypass -h Quest _373_SupplierOfReagents U_C_CACT\">" + ServerStorage.getInstance().getString(st.getPlayer().getLang(), "373quest.CATALYST") + "</a></td><td>" + ServerStorage.getInstance().getString(st.getPlayer().getLang(), "373quest.CURRENT_CATA") + "</td></tr>"
			                + "<tr><td><a action=\"bypass -h Quest _373_SupplierOfReagents 31149-5.htm\">" + ServerStorage.getInstance().getString(st.getPlayer().getLang(), "373quest.SELECT_TEMPERATURE") + "</a></td>" + "<td>" + ServerStorage.getInstance().getString(st.getPlayer().getLang(), "373quest.CURRENT_TEMP")
			                + "</td></tr><tr><td><a action=\"bypass -h Quest _373_SupplierOfReagents 31149-6.htm\">" + ServerStorage.getInstance().getString(st.getPlayer().getLang(), "373quest.MIX_INGREDIENTS") + "</a></td><td></td></tr></table></body></html>";
			final int ingr = st.getInt("ingredient");
			final int cata = st.getInt("catalyst");
			final String temp = String.valueOf(st.get("temp"));
			String r_ingr = "";
			if (ingr != 0)
			{
				for (final Object[] ITEM : ITEMS)
				{
					if ((Integer) ITEM[0] == ingr)
					{
						r_ingr = ITEM[2] + "x" + st.get("i_qty");
					}
				}
			}
			else
			{
				r_ingr = "" + ServerStorage.getInstance().getString(st.getPlayer().getLang(), "373quest.NONE") + "";
			}
			String r_cata = "";
			if (cata != 0)
			{
				for (final Object[] ITEM : ITEMS)
				{
					if ((Integer) ITEM[0] == cata)
					{
						r_cata = ITEM[2] + "x" + st.get("c_qty");
					}
				}
			}
			else
			{
				r_cata = "" + ServerStorage.getInstance().getString(st.getPlayer().getLang(), "373quest.NONE") + "";
			}
			html = html.replace("INGR", r_ingr).replace("CATA", r_cata).replace("TEMP", temp);
			if (stone != 0)
			{
				html = html.replace("MACT", "Retrieve");
			}
			else
			{
				html = html.replace("MACT", "Insert");
			}
			if (ingredient != 0)
			{
				html = html.replace("IACT", "Retrieve");
			}
			else
			{
				html = html.replace("IACT", "Insert");
			}
			if (catalyst != 0)
			{
				html = html.replace("CACT", "Retrieve");
			}
			else
			{
				html = html.replace("CACT", "Insert");
			}
		}
		else
		{
			html = "<html><body>" + ServerStorage.getInstance().getString(st.getPlayer().getLang(), "373quest.INSERT") + ":<table border=0>";
			int amt = 0;
			int item;
			for (final Object[] ITEM : ITEMS)
			{
				item = (Integer) ITEM[0];
				if (((item >= 6011) && (item <= 6031)) || ((item >= 6320) && (item <= 6321)))
				{
					if (st.getQuestItemsCount(item) > 0)
					{
						amt += 1;
						html += "<tr><td height=45><img src=icon." + ITEM[1] + " height=32 width=32></td><td width=180>" + ITEM[2] + "</td><td><button value=X1 action=\"bypass -h Quest _373_SupplierOfReagents x_1_" + page[1] + "_" + item + "\" width=40 height=15 fore=sek.cbui92><button value=X10 action=\"bypass -h Quest _373_SupplierOfReagents x_2_" + page[1] + "_" + item
						                + "\" width=40 height=15 fore=sek.cbui92></td></tr>";
					}
				}
			}
			if (amt == 0)
			{
				html += "<tr><td>" + ServerStorage.getInstance().getString(st.getPlayer().getLang(), "373quest.DO_NOT_HAVE_MATERIALS") + "</td></tr>";
				html += "</table><center><a action=\"bypass -h Quest _373_SupplierOfReagents urn\">" + ServerStorage.getInstance().getString(st.getPlayer().getLang(), "373quest.BACK") + "</a></center></body></html>";
			}
		}
		return html;
	}

	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		final QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			return htmltext;
		}

		if (event.equalsIgnoreCase("30166-4.htm"))
		{
			st.set("ingredient", "0");
			st.set("catalyst", "0");
			st.set("i_qty", "0");
			st.set("c_qty", "0");
			st.set("temp", "0");
			st.set("mixing", "0");
			st.giveItems(6317, 1);
			st.giveItems(5904, 1);
			st.startQuest();
		}
		else if (event.equalsIgnoreCase("30166-5.htm"))
		{
			st.exitQuest(true);
			st.playSound("ItemSound.quest_finish");
		}
		else if (event.equalsIgnoreCase("urn"))
		{
			htmltext = render_urn(st, null);
		}
		else if (event.startsWith("U"))
		{
			final String[] s_event = event.split("_");
			if (s_event[1].equals("M"))
			{
				if (s_event[2].equals("Insert"))
				{
					if (st.getQuestItemsCount(5904) != 0)
					{
						st.takeItems(5904, -1);
						st.set("mixing", "1");
						htmltext = "31149-2.htm";
					}
					else
					{
						htmltext = "31149-2d.htm";
					}
				}
				else if (s_event[2].equals("Retrieve"))
				{
					if (st.getInt(("mixing")) != 0)
					{
						st.set("mixing", "0");
						st.set("temp", "0");
						st.giveItems(5904, 1);
						if ((st.getInt("ingredient") > 0) || (st.getInt("catalyst") > 0))
						{
							htmltext = "31149-2c.htm";
						}
						else
						{
							htmltext = "31149-2a.htm";
						}
					}
					else
					{
						htmltext = "31149-2b.htm";
					}
				}
			}
			else if (s_event[2].equals("Insert"))
			{
				htmltext = render_urn(st, s_event);
			}
			else if (s_event[2].equals("Retrieve"))
			{
				int item = 0;
				int qty = 0;
				if (s_event[1].equals("I"))
				{
					item = st.getInt("ingredient");
					qty = st.getInt("i_qty");
					st.set("ingredient", "0");
					st.set("i_qty", "0");
				}
				else if (s_event[1].equals("C"))
				{
					item = st.getInt("catalyst");
					qty = st.getInt("c_qty");
					st.set("catalyst", "0");
					st.set("c_qty", "0");
				}
				if ((item > 0) && (qty > 0))
				{
					st.giveItems(item, qty);
					htmltext = "31149-3a.htm";
				}
				else
				{
					htmltext = "31149-3b.htm";
				}
			}
		}
		else if (event.startsWith("x"))
		{
			final String[] s_event = event.split("_");
			int qty = Integer.valueOf(s_event[1]);
			final String dst = s_event[2];
			final int item = Integer.valueOf(s_event[3]);
			String dest;
			String count;
			if (qty == 2)
			{
				qty = 10;
			}
			else
			{
				qty = 1;
			}
			if (st.getQuestItemsCount(item) >= qty)
			{
				if (dst.equals("I"))
				{
					dest = "ingredient";
					count = "i_qty";
				}
				else
				{
					dest = "catalyst";
					count = "c_qty";
				}
				st.takeItems(item, qty);
				st.set(dest, String.valueOf(item));
				st.set(count, String.valueOf(qty));
				htmltext = "31149-4a.htm";
			}
			else
			{
				htmltext = "31149-4b.htm";
			}
		}
		else if (event.startsWith("tmp"))
		{
			st.set("temp", event.split("_")[1]);
			htmltext = "31149-5a.htm";
		}
		else if (event.equalsIgnoreCase("31149-6.htm"))
		{
			if (st.getInt("mixing") > 0)
			{
				final int temp = st.getInt(("temp"));
				if (temp != 0)
				{
					final int ingredient = st.getInt("ingredient");
					final int catalyst = st.getInt("catalyst");
					final int iq = st.getInt("i_qty");
					final int cq = st.getInt("c_qty");
					st.set("ingredient", "0");
					st.set("i_qty", "0");
					st.set("catalyst", "0");
					st.set("c_qty", "0");
					st.set("temp", "0");
					int item = 0;
					for (final int[] FORMULA : FORMULAS)
					{
						if (((ingredient == FORMULA[1]) && (catalyst == FORMULA[3]) && (iq == FORMULA[2]) && (cq == FORMULA[4])) || ((ingredient == FORMULA[3]) && (catalyst == FORMULA[1]) && (iq == FORMULA[4]) && (cq == FORMULA[2])))
						{
							item = FORMULA[0];
							break;
						}
					}
					if ((item == 6320) && (temp != 1))
					{
						return "31149-7c.htm";
					}
					if (item == 6319)
					{
						if (temp == 3)
						{
							if (st.getQuestItemsCount(6318) > 0)
							{
								st.takeItems(6318, 1);
							}
							else
							{
								return "31149-7a.htm";
							}
						}
						else
						{
							return "31149-7b.htm";
						}
					}
					if (item > 0)
					{
						int chance = 0;
						int qty = 0;
						for (final int[] aTEMPERATURE : TEMPERATURE)
						{
							if (aTEMPERATURE[0] == temp)
							{
								chance = aTEMPERATURE[1];
								qty = aTEMPERATURE[2];
							}
						}
						if (item == 6319)
						{
							final QuestState mimirs = player.getQuestState("_235_MimirsElixir");
							if (mimirs != null)
							{
								chance = 100;
								qty = 1;
								mimirs.set("cond", "8");
							}
							else
							{
								return "31149-7d.htm";
							}
						}
						if (getRandom(100) < chance)
						{
							st.giveItems(item, qty);
						}
						else
						{
							htmltext = "31149-6c.htm";
						}
					}
					else
					{
						htmltext = "31149-6d.htm";
					}
				}
				else
				{
					htmltext = "31149-6b.htm";
				}
			}
			else
			{
				htmltext = "31149-6a.htm";
			}
		}
		return htmltext;
	}

	@Override
	public String onTalk(Npc npc, Player player)
	{
		String htmltext = getNoQuestMsg(player);
		final QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			return htmltext;
		}

		final int npcId = npc.getId();
		final int cond = st.getCond();

		if (npcId == 30166)
		{
			if (cond == 0)
			{
				if (player.getLevel() < getMinLvl(getId()))
				{
					st.exitQuest(true);
					htmltext = "30166-2.htm";
				}
				else
				{
					htmltext = "30166-1.htm";
				}
			}
			else
			{
				htmltext = "30166-3.htm";
			}
		}
		else if ((npcId == 31149) && (cond == 1))
		{
			htmltext = render_urn(st, null);
		}
		return htmltext;
	}

	@Override
	public String onKill(Npc npc, Player player, boolean isSummon)
	{
		final Player partyMember = getRandomPartyMemberState(player, State.STARTED);
		if (partyMember == null)
		{
			return super.onKill(npc, player, isSummon);
		}
		
		final QuestState st = partyMember.getQuestState(getName());
		if (st != null)
		{
			for (final int[] i : DROPLIST_COND)
			{
				if (npc.getId() == i[0])
				{
					st.calcDropItems(getId(), i[1], npc.getId(), Integer.MAX_VALUE);
				}
			}
		}
		return super.onKill(npc, player, isSummon);
	}

	public static void main(String[] args)
	{
		new _373_SupplierOfReagents(373, _373_SupplierOfReagents.class.getSimpleName(), "");
	}
}
