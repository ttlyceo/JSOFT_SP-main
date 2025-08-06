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
package l2e.gameserver.model.actor.instance;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import l2e.commons.apache.ArrayUtils;
import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.data.htm.HtmCache;
import l2e.gameserver.data.parser.HennaParser;
import l2e.gameserver.data.parser.ItemsParser;
import l2e.gameserver.data.parser.RecipeParser;
import l2e.gameserver.model.Elementals;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.RecipeList;
import l2e.gameserver.model.TradeItem;
import l2e.gameserver.model.TradeList;
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.ManufactureItemTemplate;
import l2e.gameserver.model.actor.templates.items.Item;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.items.type.EtcItemType;
import l2e.gameserver.model.strings.server.ServerMessage;
import l2e.gameserver.model.strings.server.ServerStorage;
import l2e.gameserver.network.serverpackets.CharInfo;
import l2e.gameserver.network.serverpackets.ExPrivateStorePackageMsg;
import l2e.gameserver.network.serverpackets.PrivateStoreBuyMsg;
import l2e.gameserver.network.serverpackets.PrivateStoreSellMsg;
import l2e.gameserver.network.serverpackets.RadarControl;
import l2e.gameserver.network.serverpackets.RecipeShopMsg;

/**
 * Created by LordWinter
 */
public class ItemBrokerInstance extends NpcInstance
{
	private static Map<Integer, NpcInfo> _npcInfos = new ConcurrentHashMap<>();
	
	public int[] RARE_ITEMS =
	{
	        16255, 16256, 16257, 16258, 16259, 16260, 16261, 16262, 16263, 16264, 16265, 16266, 16267, 16268, 16269, 16270, 16271, 16272, 16273, 16274, 16275, 16276, 16277, 16278, 16279, 16280, 16281, 16282, 16283, 16284, 16285, 16286, 16287, 16288, 16357, 16358, 16359, 16360, 16361, 16362, 10119, 10120, 10121, 11349, 11350, 11351, 11352, 11353, 11354, 11355, 11356, 11357, 11358, 11359, 11360, 11361, 11363, 11364, 11365, 11366, 11367, 11368, 11369, 11370, 11371, 11372, 11373, 11375, 11376, 11377, 11378, 11379, 11380, 11381, 11382, 11383, 11384, 11386, 11387, 11388, 11389, 11390, 11391, 11392, 11393, 11394, 11395, 11396, 11397, 11398, 11399, 11400, 11401, 11402, 11403, 11404, 11405, 11406, 11407, 11408, 11409, 11410, 11411, 11412, 11413, 11414, 11415, 11417, 11418, 11419, 11420, 11421, 11422, 11423, 11424, 11426, 11427, 11428, 11429, 11430, 11431, 11432, 11433, 11434, 11435, 11436, 11437, 11438, 11439, 11440, 11441, 11442, 11443, 11444, 11445, 11446, 11447, 11448, 11449, 11450, 11451, 11452, 11453, 11454, 11455, 11456, 11457, 11458, 11459, 11460, 11461, 11462, 11463, 11464, 11465, 11466, 11467, 11468, 11470, 11471, 11472, 11473, 11474, 11475, 11476, 11477, 11478, 11479, 11481, 11482, 11483, 11484, 11485, 11486, 11487, 11488, 11489, 11490, 11491, 11492, 11493, 11494, 11495, 11496, 11497, 11498, 11499, 11500, 11501, 11503, 11504, 11505, 11506, 11507, 11509, 11510, 11511, 11512, 11513, 11514, 11515, 11516, 11517, 11518, 11519, 11520, 11521, 11522, 11523, 11524, 11525, 11526, 11527, 11528, 11529, 11530, 11531, 11533, 11534, 11535, 11536, 11537, 11538, 11539, 11540, 11541, 11542, 11543, 11544, 11545, 11546, 11547, 11548, 11549, 11550, 11551, 11552, 11553, 11554, 11555, 11556, 11557, 11558, 11559, 11560, 11561, 11562, 11563, 11564, 11565, 11566, 11567, 11568, 11570, 11571, 11572, 11573, 11574, 11575, 11576, 11577, 11578, 11579, 11580, 11581, 11582, 11583, 11584, 11585, 11586, 11587, 11588, 11589, 11590, 11591, 11592, 11593, 11594, 11595, 11596, 11597, 11598, 11599, 11600, 11601, 11602, 11603, 11604, 12978, 12979, 12980, 12981, 12982, 12983, 12984, 12985, 12986, 12987, 12988, 12989, 12990, 12991, 12992, 12993, 12994, 12995, 12996, 12997, 12998, 12999, 13000, 13001, 13078, 16289, 16290, 16291, 16292, 16293, 16294, 16295, 16296, 16297, 16298, 16299, 16300, 16301, 16302, 16303, 16305, 16306, 16307, 16308, 16309, 16310, 16311, 16312, 16313, 16314, 16315, 16316, 16317, 16318, 16319, 16320, 16322, 16323, 16324, 16325, 16326, 16327, 16328, 16329, 16330, 16331, 16332, 16333, 16334, 16335, 16336, 16337, 16339, 16340, 16341, 16342, 16343, 16344, 16345, 16346, 16347, 16348, 16349, 16350, 16351, 16352, 16353, 16354, 16356, 16369, 16370, 16371, 16372, 16373, 16374, 16375, 16376, 16377, 16378, 16379, 16380, 16837, 16838, 16839, 16840, 16841, 16842, 16843, 16844, 16845, 16846, 16847, 16848, 16849, 16850, 16851, 10870, 10871, 10872, 10873, 10874, 10875, 10876, 10877, 10878, 10879, 10880, 10881, 10882, 10883, 10884, 10885, 10886, 10887, 10888, 10889, 10890, 10891, 10892, 10893, 10894, 10895, 10896, 10897, 10898, 10899, 10900, 10901, 10902, 10903, 10904, 10905, 10906, 10907, 10908, 10909, 10910, 10911, 10912, 10913, 10914, 10915, 10916, 10917, 10918, 10919, 10920, 10921, 10922, 10923, 10924, 10925, 10926, 10927, 10928, 10929, 10930, 10931, 10932, 10933, 10934, 10935, 10936, 10937, 10938, 10939, 10940, 10941, 10942, 10943, 10944, 10945, 10946, 10947, 10948, 10949, 10950, 10951, 10952, 10953, 10954, 10955, 10956, 10957, 10958, 10959, 10960, 10961, 10962, 10963, 10964, 10965, 10966, 10967, 10968, 10969, 10970, 10971, 10972, 10973, 10974, 10975, 10976, 10977, 10978, 10979, 10980, 10981, 10982, 10983, 10984, 10985, 10986, 10987, 10988, 10989, 10990, 10991, 10992, 10993, 10994, 10995, 10996, 10997, 10998, 10999, 11000, 11001, 11002, 11003, 11004, 11005, 11006, 11007, 11008, 11009, 11010, 11011, 11012, 11013, 11014, 11015, 11016, 11017, 11018, 11019, 11020, 11021, 11022, 11023, 11024, 11025, 11026, 11027, 11028, 11029, 11030, 11031, 11032, 11033, 11034, 11035, 11036, 11037, 11038, 11039, 11040, 11041, 11042, 11043, 11044, 11045, 11046, 11047, 11048, 11049, 11050, 11051, 11052, 11053, 11054, 11055, 11056, 11057, 11058, 11059, 11060, 11061, 11062, 11063, 11064, 11065, 11066, 11067, 11068, 11069, 11070, 11071, 11072, 11073, 11074, 11075, 11076, 11077, 11078, 11079, 11080, 11081, 11082, 11083, 11084, 11085, 11086, 11087, 11088, 11089, 11090, 11091, 11092, 11093, 11094, 11095, 11096, 11097, 11098, 11099, 11100, 11101, 11102, 11103, 11104, 11105, 11106, 11107, 11108, 11109, 11110, 11111, 11112, 11113, 11114, 11115, 11116, 11117, 11118, 11119, 11120, 11121, 11122, 11123, 11124, 11125, 11126, 11127, 11128, 11129, 11130, 11131, 11132, 11133, 11134, 11135, 11136, 11137, 11138, 11139, 11140, 11141, 11142, 11143, 11144, 11145, 11146, 11147, 11148, 11149, 11150, 11151, 11152, 11153, 11154, 11155, 11156, 11157, 11158, 11159, 11160, 11161, 11162, 11163, 11164, 11165, 11166, 11167, 11168, 11169, 11170, 11171, 11172, 11173, 11174, 11175, 11176, 11177, 11178, 11179, 11180, 11181, 11182, 11183, 11184, 11185, 11186, 11187, 11188, 11189, 11190, 11191, 11192, 11193, 11194, 11195, 11196, 11197, 11198, 11199, 11200, 11201, 11202, 11203, 11204, 11205, 11206, 11207, 11208, 11209, 11210, 11211, 11212, 11213, 11214, 11215, 11216, 11217, 11218, 11219, 11220, 11221, 11222, 11223, 11224, 11225, 11226, 11227, 11228, 11229, 11230, 11231, 11232, 11233, 11234, 11235, 11236, 11237, 11238, 11239, 11240, 11241, 11242, 11243, 11244, 11245, 11246, 11247, 11248, 11249, 11250, 11251, 11252, 11253, 11254, 11255, 11256, 11257, 11258, 11259, 11260, 11261, 11262, 11263, 11264, 11265, 11266, 11267, 11268, 11269, 11270, 11271, 11272, 11273, 11274, 11275, 11276, 11277, 11278, 11279, 11280, 11281, 11282, 11283, 11284, 11285, 11286, 11287, 11288, 11289, 11290, 11291, 11292, 11293, 11294, 11295, 11296, 11297, 11298, 11299, 11300, 11301, 11302, 11303, 11304, 11305, 11306, 11307, 11308, 11309, 11310, 11311, 11312, 11313, 11314, 11315, 11316, 11317, 11318, 11319, 11320, 11321, 11322, 11323, 11324, 11325, 11326, 11327, 11328, 11329, 11330, 11331, 11332, 11333, 11334, 11335, 11336, 11337, 11338, 11339, 11340, 11341, 11342, 11343, 11344, 11345, 11346, 11347, 11348, 11362, 11374, 11385, 11416, 11425, 11469, 11480, 11502, 11508, 11532, 11569, 12852, 12853, 12854, 12855, 12856, 12857, 12858, 12859, 12860, 12861, 12862, 12863, 12864, 12865, 12866, 12867, 12868, 12869, 12870, 12871, 12872, 12873, 12874, 12875, 12876, 12877, 12878, 12879, 12880, 12881, 12882, 12883, 12884, 12885, 12886, 12887, 12888, 12889, 12890, 12891, 12892, 12893, 12894, 12895, 12896, 12897, 12898, 12899, 12900, 12901, 12902, 12903, 12904, 12905, 12906, 12907, 12908, 12909, 12910, 12911, 12912, 12913, 12914, 12915, 12916, 12917, 12918, 12919, 12920, 12921, 12922, 12923, 12924, 12925, 12926, 12927, 12928, 12929, 12930, 12931, 12932, 12933, 12934, 12935, 12936, 12937, 12938, 12939, 12940, 12941, 12942, 12943, 12944, 12945, 12946, 12947, 12948, 12949, 12950, 12951, 12952, 12953, 12954, 12955, 12956, 12957, 12958, 12959, 12960, 12961, 12962, 12963, 12964, 12965, 12966, 12967, 12968, 12969, 12970, 12971, 12972, 12973, 12974, 12975, 12976, 12977, 14412, 14413, 14414, 14415, 14416, 14417, 14418, 14419, 14420, 14421, 14422, 14423, 14424, 14425, 14426, 14427, 14428, 14429, 14430, 14431, 14432, 14433, 14434, 14435, 14436, 14437, 14438, 14439, 14440, 14441, 14442, 14443, 14444, 14445, 14446, 14447, 14448, 14449, 14450, 14451, 14452, 14453, 14454, 14455, 14456, 14457, 14458, 14459, 14460, 14526, 14527, 14528, 14529, 14560, 14561, 14562, 14563, 14564, 14565, 14566, 14567, 14568, 14569, 14570, 14571, 14572, 14573, 14574, 14575, 14576, 14577, 14578, 14579, 14580, 14581, 16042, 16043, 16044, 16045, 16046, 16047, 16048, 16049, 16050, 16051, 16052, 16053, 16054, 16055, 16056, 16057, 16058, 16059, 16060, 16061, 16062, 16063, 16064, 16065, 16066, 16067, 16068, 16069, 16070, 16071, 16072, 16073, 16074, 16075, 16076, 16077, 16078, 16079, 16080, 16081, 16082, 16083, 16084, 16085, 16086, 16087, 16088, 16089, 16090, 16091, 16092, 16093, 16094, 16095, 16096, 16097, 16134, 16135, 16136, 16137, 16138, 16139, 16140, 16141, 16142, 16143, 16144, 16145, 16146, 16147, 16148, 16149, 16150, 16151, 16179, 16180, 16181, 16182, 16183, 16184, 16185, 16186, 16187, 16188, 16189, 16190, 16191, 16192, 16193, 16194, 16195, 16196, 16197, 16198, 16199, 16200, 16201, 16202, 16203, 16204, 16205, 16206, 16207, 16208, 16209, 16210, 16211, 16212, 16213, 16214, 16215, 16216, 16217, 16218, 16219, 16220, 16304, 16321, 16338, 16355
	};
	
	public ItemBrokerInstance(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public void showChatWindow(Player player, int val)
	{
		String html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/itemBroker/main.htm");
		new StringBuilder();
		int type = 0;
		String typeNameEn = "";
		String list = "";
		switch (val)
		{
			case 0 :
				if (Config.ITEM_BROKER_ITEM_SEARCH)
				{
					html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/itemBroker/index-01.htm");
				}
				else
				{
					html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/itemBroker/index-00.htm");
				}
				final String template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/itemBroker/index-template.htm");
				list += template;
				
				list = list.replace("%objectId%", String.valueOf(getObjectId()));
				html = html.replace("{list}", list);
				html = html.replace("%objectId%", String.valueOf(getObjectId()));
				Util.setHtml(html, player);
				break;
			case 1 :
				html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/itemBroker/index-02.htm");
				html = html.replace("%objectId%", String.valueOf(getObjectId()));
				Util.setHtml(html, player);
				break;
			case 10 + Player.STORE_PRIVATE_SELL :
				type = Player.STORE_PRIVATE_SELL;
				typeNameEn = ServerStorage.getInstance().getString(player.getLang(), "ItemBroker.SELL");
				break;
			case 10 + Player.STORE_PRIVATE_BUY :
				type = Player.STORE_PRIVATE_BUY;
				typeNameEn = ServerStorage.getInstance().getString(player.getLang(), "ItemBroker.BUY");
				break;
			case 10 + Player.STORE_PRIVATE_MANUFACTURE :
				type = Player.STORE_PRIVATE_MANUFACTURE;
				typeNameEn = ServerStorage.getInstance().getString(player.getLang(), "ItemBroker.CRAFT");
				break;
			case 20 + Player.STORE_PRIVATE_SELL :
			case 20 + Player.STORE_PRIVATE_BUY :
			case 20 + Player.STORE_PRIVATE_MANUFACTURE :
				type = val - 20;
				
				final String template1 = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/itemBroker/list-template.htm");
				list += template1;
				
				list = list.replace("%type%", String.valueOf(type));
				list = list.replace("%back%", String.valueOf(10 + type));
				list = list.replace("%objectId%", String.valueOf(getObjectId()));
				html = html.replace("{list}", list);
				html = html.replace("%objectId%", String.valueOf(getObjectId()));
				Util.setHtml(html, player);
			case 30 + Player.STORE_PRIVATE_SELL :
			case 30 + Player.STORE_PRIVATE_BUY :
			case 30 + Player.STORE_PRIVATE_MANUFACTURE :
				type = val - 30;
				
				final String template2 = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/itemBroker/listEnchant-template.htm");
				list += template2;
				
				list = list.replace("%type%", String.valueOf(type));
				list = list.replace("%back%", String.valueOf(10 + type));
				list = list.replace("%objectId%", String.valueOf(getObjectId()));
				html = html.replace("{list}", list);
				html = html.replace("%objectId%", String.valueOf(getObjectId()));
				Util.setHtml(html, player);
		}
		
		if (type > 0)
		{
			final String template2 = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/itemBroker/listAll-template.htm");
			list += template2;
			
			String list1 = "";
			String list2 = "";
			String list3 = "";
			
			String block1 = "";
			String block2 = "";
			String block3 = "";
			
			if (type == Player.STORE_PRIVATE_SELL)
			{
				final String template3 = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/itemBroker/listAllEnchant-template.htm");
				block1 += template3;
				block1 = block1.replace("%typeEnch%", String.valueOf(type + 30));
				block1 = block1.replace("%objectId%", String.valueOf(getObjectId()));
				list1 += block1;
			}
			
			if (type != Player.STORE_PRIVATE_MANUFACTURE)
			{
				final String template4 = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/itemBroker/listAllRare-template.htm");
				block2 += template4;
				block2 = block2.replace("%type%", String.valueOf(type));
				block2 = block2.replace("%objectId%", String.valueOf(getObjectId()));
				list2 += template4;
			}
			
			if (type != Player.STORE_PRIVATE_MANUFACTURE)
			{
				final String template5 = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/itemBroker/listAllCommons-template.htm");
				block3 += template5;
				block3 = block3.replace("%type%", String.valueOf(type));
				block3 = block3.replace("%objectId%", String.valueOf(getObjectId()));
				list3 += template5;
			}
			
			if (list1 != null && !list1.isEmpty())
			{
				list = list.replace("%enchant%", list1);
			}
			else
			{
				list = list.replace("%enchant%", "");
			}
			
			if (list2 != null && !list2.isEmpty())
			{
				list = list.replace("%rare%", list2);
			}
			else
			{
				list = list.replace("%rare%", "");
			}
			
			if (list3 != null && !list3.isEmpty())
			{
				list = list.replace("%commons%", list3);
			}
			else
			{
				list = list.replace("%commons%", "");
			}
			
			list = list.replace("%name%", typeNameEn);
			list = list.replace("%type%", String.valueOf(type));
			list = list.replace("%typeE%", String.valueOf(type + 20));
			list = list.replace("%typeEnch%", String.valueOf(type + 30));
			list = list.replace("%objectId%", String.valueOf(getObjectId()));
		}
		
		html = html.replace("{list}", list);
		html = html.replace("%objectId%", String.valueOf(getObjectId()));
		Util.setHtml(html, player);
	}
	
	@Override
	public void onBypassFeedback(Player player, String command)
	{
		if (command.startsWith("Chat"))
		{
			final String[] var = command.split(" ");
			if (var.length != 2)
			{
				player.sendMessage((new ServerMessage("ItemBroker.INCORRECT_DATA_LENGHT", player.getLang())).toString());
				return;
			}
			showChatWindow(player, Integer.valueOf(var[1]));
		}
		else if (command.startsWith("list"))
		{
			if (!Config.ITEM_BROKER_ITEM_SEARCH)
			{
				return;
			}
			
			final String[] var = command.split(" ");
			if (var.length != 6)
			{
				player.sendMessage((new ServerMessage("ItemBroker.INCORRECT_DATA_LENGHT", player.getLang())).toString());
				return;
			}
			
			int type;
			int itemType;
			int currentPage;
			int minEnchant;
			int rare;
			
			try
			{
				type = Integer.valueOf(var[1]);
				itemType = Integer.valueOf(var[2]);
				currentPage = Integer.valueOf(var[3]);
				minEnchant = Integer.valueOf(var[4]);
				rare = Integer.valueOf(var[5]);
			}
			catch (final Exception e)
			{
				player.sendMessage((new ServerMessage("ItemBroker.INCORRECT_DATA", player.getLang())).toString());
				return;
			}
			
			final TreeMap<Integer, TreeMap<Long, ItemTemplate>> allItems = getItems(type, player);
			if (allItems == null)
			{
				player.sendMessage((new ServerMessage("ItemBroker.TYPE_NOT_FOUND", player.getLang())).toString());
				return;
			}
			
			final List<ItemTemplate> items = new ArrayList<>(allItems.size() * 10);
			for (final TreeMap<Long, ItemTemplate> tempItems : allItems.values())
			{
				final TreeMap<Long, ItemTemplate> tempItems2 = new TreeMap<>();
				for (final Entry<Long, ItemTemplate> entry : tempItems.entrySet())
				{
					final ItemTemplate tempItem = entry.getValue();
					if (tempItem == null)
					{
						continue;
					}
					if (tempItem._enchant < minEnchant)
					{
						continue;
					}
					final Item temp = tempItem._item != null ? tempItem._item.getItem() : ItemsParser.getInstance().getTemplate(tempItem._itemId);
					if (temp == null || (rare > 0 && !tempItem._rare))
					{
						continue;
					}
					if (itemType >= 13 ? !temp.isCommonItem() : temp.isCommonItem())
					{
						continue;
					}
					
					if (itemType < 13 && itemType != 0 && !getItemClassType(itemType, temp))
					{
						continue;
					}
					tempItems2.put(entry.getKey(), tempItem);
				}
				if (tempItems2.isEmpty())
				{
					continue;
				}
				
				final ItemTemplate item = type == Player.STORE_PRIVATE_BUY ? tempItems2.lastEntry().getValue() : tempItems2.firstEntry().getValue();
				if (item != null)
				{
					items.add(item);
				}
			}
			
			String html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/itemBroker/allList.htm");
			String template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/itemBroker/allList-template.htm");
			String block = "";
			String list = "";
			
			final StringBuilder out = new StringBuilder(200);
			
			int totalPages = items.size();
			totalPages = totalPages / Config.ITEM_BROKER_ITEMS_PER_PAGE + (totalPages % Config.ITEM_BROKER_ITEMS_PER_PAGE > 0 ? 1 : 0);
			totalPages = Math.max(1, totalPages);
			currentPage = Math.min(totalPages, Math.max(1, currentPage));
			
			if (totalPages > 1)
			{
				int page = Math.max(1, Math.min(totalPages - 1 + 1, currentPage - 1 / 2));
				
				if (currentPage > 1)
				{
					listPageNum(out, type, itemType, currentPage - 1, minEnchant, rare, "prev");
				}
				else
				{
					listPageNum(out, type, itemType, currentPage - 1, minEnchant, rare, "emptyLeft");
				}
				
				for (int count = 0; count < 1 && page <= totalPages; count++, page++)
				{
					if (page == currentPage)
					{
						listPageNum(out, type, itemType, page, minEnchant, rare, "currentPage");
					}
				}
				
				if (currentPage < totalPages)
				{
					listPageNum(out, type, itemType, currentPage + 1, minEnchant, rare, "next");
				}
				else
				{
					listPageNum(out, type, itemType, currentPage - 1, minEnchant, rare, "emptyRight");
				}
			}
			
			if (items.size() > 0)
			{
				int count = 0;
				final ListIterator<ItemTemplate> iter = items.listIterator((currentPage - 1) * Config.ITEM_BROKER_ITEMS_PER_PAGE);
				while (iter.hasNext() && count < Config.ITEM_BROKER_ITEMS_PER_PAGE)
				{
					final ItemTemplate item = iter.next();
					final Item temp = item._item != null ? item._item.getItem() : ItemsParser.getInstance().getTemplate(item._itemId);
					if (temp == null)
					{
						continue;
					}
					
					block = template;
					
					block = block.replace("%icon%", temp.getIcon());
					block = block.replace("%type%", String.valueOf(type));
					block = block.replace("%itemId%", String.valueOf(item._itemId));
					block = block.replace("%ench%", String.valueOf(minEnchant));
					block = block.replace("%rare%", String.valueOf(rare));
					block = block.replace("%itemType%", String.valueOf(itemType));
					block = block.replace("%page%", String.valueOf(currentPage));
					block = block.replace("%name%", getItemName(player, item));
					
					final String enchant = getItemEnchant(item);
					if (!enchant.isEmpty())
					{
						block = block.replace("%itemEnchant%", enchant);
					}
					else
					{
						block = block.replace("%itemEnchant%", "");
					}
					
					final String attribute = getItemAttribute(player, item);
					if (!attribute.isEmpty())
					{
						block = block.replace("%itemAtt%", "" + ServerStorage.getInstance().getString(player.getLang(), "ItemBroker.ATTRIBUTE") + "" + getItemAttribute(player, item));
					}
					else
					{
						block = block.replace("%itemAtt%", "");
					}
					
					block = block.replace("%price%", Util.formatAdena(item._price));
					if (item._isPackage)
					{
						block = block.replace("%package%", " " + ServerStorage.getInstance().getString(player.getLang(), "ItemBroker.PACKAGE") + "");
					}
					else
					{
						block = block.replace("%package%", "");
					}
					
					if (temp.isStackable())
					{
						block = block.replace("%count%", "" + ServerStorage.getInstance().getString(player.getLang(), "ItemBroker.COUNT") + " " + Util.formatAdena(item._count));
					}
					else
					{
						block = block.replace("%count%", "");
					}
					list += block;
					count++;
				}
			}
			else
			{
				template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/itemBroker/empty-template.htm");
				template = template.replace("%type%", String.valueOf(type));
				template = template.replace("%objectId%", String.valueOf(getObjectId()));
				list += template;
			}
			html = html.replace("%pages%", out.toString());
			html = html.replace("%list%", list);
			html = html.replace("%objectId%", String.valueOf(getObjectId()));
			Util.setHtml(html, player);
		}
		else if (command.startsWith("selectForItem"))
		{
			if (!Config.ITEM_BROKER_ITEM_SEARCH)
			{
				return;
			}
			
			final String[] var = command.split(" ");
			if (var.length < 8 || var.length > 13)
			{
				player.sendMessage((new ServerMessage("ItemBroker.INCORRECT_DATA_LENGHT", player.getLang())).toString());
				return;
			}
			
			int type;
			int itemId;
			int minEnchant;
			int rare;
			int itemType;
			int currentPage;
			int returnPage;
			String[] search = null;
			
			try
			{
				type = Integer.valueOf(var[1]);
				itemId = Integer.valueOf(var[2]);
				minEnchant = Integer.valueOf(var[3]);
				rare = Integer.valueOf(var[4]);
				itemType = Integer.valueOf(var[5]);
				currentPage = Integer.valueOf(var[6]);
				returnPage = Integer.valueOf(var[7]);
				if (var.length > 8)
				{
					search = new String[var.length - 8];
					System.arraycopy(var, 8, search, 0, search.length);
				}
			}
			catch (final Exception e)
			{
				player.sendMessage((new ServerMessage("ItemBroker.INCORRECT_DATA", player.getLang())).toString());
				return;
			}
			
			final Item template = ItemsParser.getInstance().getTemplate(itemId);
			if (template == null)
			{
				player.sendMessage((new ServerMessage("ItemBroker.NOT_SPEC", player.getLang())).toString());
				return;
			}
			
			final TreeMap<Integer, TreeMap<Long, ItemTemplate>> tmpItems = getItems(type, player);
			if (tmpItems == null)
			{
				player.sendMessage((new ServerMessage("ItemBroker.MATTER_NOT", player.getLang())).toString());
				return;
			}
			
			final TreeMap<Long, ItemTemplate> allItems = tmpItems.get(template.getId());
			if (allItems == null)
			{
				player.sendMessage((new ServerMessage("ItemBroker.SAME_NOT_FOUND", player.getLang())).toString());
				return;
			}
			
			final StringBuilder backOut = new StringBuilder(200);
			if (search == null)
			{
				listPageNum(backOut, type, itemType, returnPage, minEnchant, rare, ServerStorage.getInstance().getString(player.getLang(), "ItemBroker.BACK"));
			}
			else
			{
				findPageNum(backOut, type, returnPage, search, ServerStorage.getInstance().getString(player.getLang(), "ItemBroker.BACK"));
			}
			
			final NavigableMap<Long, ItemTemplate> sortedItems = type == Player.STORE_PRIVATE_BUY ? allItems.descendingMap() : allItems;
			if (sortedItems == null)
			{
				player.sendMessage((new ServerMessage("ItemBroker.NO_RESULTS", player.getLang())).toString());
				return;
			}
			
			final List<ItemTemplate> items = new ArrayList<>(sortedItems.size());
			for (final ItemTemplate item : sortedItems.values())
			{
				if (item == null || item._enchant < minEnchant || (rare > 0 && !item._rare))
				{
					continue;
				}
				
				items.add(item);
			}
			
			String html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/itemBroker/selectItem.htm");
			String tpl = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/itemBroker/selectItem-template.htm");
			String block = "";
			String list = "";
			
			int totalPages = items.size();
			totalPages = totalPages / Config.ITEM_BROKER_ITEMS_PER_PAGE + (totalPages % Config.ITEM_BROKER_ITEMS_PER_PAGE > 0 ? 1 : 0);
			totalPages = Math.max(1, totalPages);
			currentPage = Math.min(totalPages, Math.max(1, currentPage));
			
			final StringBuilder out = new StringBuilder(200);
			
			if (totalPages > 1)
			{
				int page = Math.max(1, Math.min(totalPages - 1 + 1, currentPage - 1 / 2));
				
				if (currentPage > 1)
				{
					listForItemPageNum(out, type, itemId, minEnchant, rare, itemType, currentPage - 1, returnPage, search, "prev");
				}
				else
				{
					listForItemPageNum(out, type, itemId, minEnchant, rare, itemType, currentPage - 1, returnPage, search, "emptyLeft");
				}
				
				for (int count = 0; count < 1 && page <= totalPages; count++, page++)
				{
					if (page == currentPage)
					{
						listForItemPageNum(out, type, itemId, minEnchant, rare, itemType, page, returnPage, search, "currentPage");
					}
				}
				
				if (currentPage < totalPages)
				{
					listForItemPageNum(out, type, itemId, minEnchant, rare, itemType, currentPage + 1, returnPage, search, "next");
				}
				else
				{
					listForItemPageNum(out, type, itemId, minEnchant, rare, itemType, currentPage - 1, returnPage, search, "emptyRight");
				}
			}
			
			if (items.size() > 0)
			{
				int count = 0;
				final ListIterator<ItemTemplate> iter = items.listIterator((currentPage - 1) * Config.ITEM_BROKER_ITEMS_PER_PAGE);
				while (iter.hasNext() && count < Config.ITEM_BROKER_ITEMS_PER_PAGE)
				{
					final ItemTemplate item = iter.next();
					final Item temp = item._item != null ? item._item.getItem() : ItemsParser.getInstance().getTemplate(item._itemId);
					if (temp == null)
					{
						continue;
					}
					
					block = tpl;
					
					block = block.replace("%icon%", temp.getIcon());
					block = block.replace("%type%", String.valueOf(type));
					block = block.replace("%itemId%", String.valueOf(item._itemId));
					block = block.replace("%itemObj%", String.valueOf(item._itemObjId));
					block = block.replace("%name%", getItemName(player, item));
					final String enchant = getItemEnchant(item);
					if (!enchant.isEmpty())
					{
						block = block.replace("%itemEnchant%", enchant);
					}
					else
					{
						block = block.replace("%itemEnchant%", "");
					}
					
					final String attribute = getItemAttribute(player, item);
					if (!attribute.isEmpty())
					{
						block = block.replace("%itemAtt%", "" + ServerStorage.getInstance().getString(player.getLang(), "ItemBroker.ATTRIBUTE") + "" + getItemAttribute(player, item));
					}
					else
					{
						block = block.replace("%itemAtt%", "");
					}
					block = block.replace("%price%", Util.formatAdena(item._price));
					if (item._isPackage)
					{
						block = block.replace("%package%", " " + ServerStorage.getInstance().getString(player.getLang(), "ItemBroker.PACKAGE") + "");
					}
					else
					{
						block = block.replace("%package%", "");
					}
					
					if (temp.isStackable())
					{
						block = block.replace("%count%", "" + ServerStorage.getInstance().getString(player.getLang(), "ItemBroker.COUNT") + " " + Util.formatAdena(item._count));
					}
					else
					{
						block = block.replace("%count%", "");
					}
					block = block.replace("%owner%", "" + ServerStorage.getInstance().getString(player.getLang(), "ItemBroker.OWNER") + " " + item._merchantName);
					list += block;
					
					count++;
				}
			}
			else
			{
				tpl = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/itemBroker/selectItem-empty.htm");
				tpl = tpl.replace("%type%", String.valueOf(type));
				tpl = tpl.replace("%objectId%", String.valueOf(getObjectId()));
				list += tpl;
			}
			html = html.replace("%back%", backOut.toString());
			html = html.replace("%pages%", out.toString());
			html = html.replace("%list%", list);
			html = html.replace("%objectId%", String.valueOf(getObjectId()));
			Util.setHtml(html, player);
		}
		else if (command.startsWith("path"))
		{
			if (!Config.ITEM_BROKER_ITEM_SEARCH)
			{
				return;
			}
			
			final String[] var = command.split(" ");
			if (var.length != 4)
			{
				player.sendMessage((new ServerMessage("ItemBroker.INCORRECT_DATA_LENGHT", player.getLang())).toString());
				return;
			}
			
			int type;
			int itemId;
			int itemObjId;
			
			try
			{
				type = Integer.valueOf(var[1]);
				itemId = Integer.valueOf(var[2]);
				itemObjId = Integer.valueOf(var[3]);
			}
			catch (final Exception e)
			{
				player.sendMessage((new ServerMessage("ItemBroker.INCORRECT_DATA", player.getLang())).toString());
				return;
			}
			
			final Item temp = ItemsParser.getInstance().getTemplate(itemId);
			if (temp == null)
			{
				player.sendMessage((new ServerMessage("ItemBroker.NOT_SPEC", player.getLang())).toString());
				return;
			}
			
			final TreeMap<Integer, TreeMap<Long, ItemTemplate>> allItems = getItems(type, player);
			if (allItems == null)
			{
				player.sendMessage((new ServerMessage("ItemBroker.TYPE_NOT_FOUND", player.getLang())).toString());
				return;
			}
			
			final TreeMap<Long, ItemTemplate> items = allItems.get(temp.getId());
			if (items == null)
			{
				player.sendMessage((new ServerMessage("ItemBroker.SAME_NOT_FOUND", player.getLang())).toString());
				return;
			}
			
			ItemTemplate item = null;
			for (final ItemTemplate i : items.values())
			{
				if (i._itemObjId == itemObjId)
				{
					item = i;
					break;
				}
			}
			
			if (item == null)
			{
				player.sendMessage((new ServerMessage("ItemBroker.OBJ_NOT_FOUNT", player.getLang())).toString());
				return;
			}
			
			boolean found = false;
			final Player trader = GameObjectsStorage.getPlayer(item._merchantObjectId);
			if (trader == null)
			{
				player.sendMessage((new ServerMessage("ItemBroker.MENCH_NOT_FOUND", player.getLang())).toString());
				return;
			}
			
			switch (type)
			{
				case Player.STORE_PRIVATE_SELL :
					if (trader.getSellList() != null)
					{
						if (trader.getPrivateStoreType() == Player.STORE_PRIVATE_PACKAGE_SELL)
						{
							if (item._isPackage)
							{
								long packagePrice = 0;
								for (final TradeItem tradeItem : trader.getSellList().getItems())
								{
									packagePrice += tradeItem.getPrice() * tradeItem.getCount();
									if (tradeItem.getItem().getId() == item._itemId)
									{
										found = true;
									}
								}
								
								if (packagePrice != item._price)
								{
									found = false;
								}
							}
						}
						else
						{
							if (!item._isPackage)
							{
								for (final TradeItem tradeItem : trader.getSellList().getItems())
								{
									if (tradeItem.getItem().getId() == item._itemId && tradeItem.getPrice() == item._price)
									{
										found = true;
										break;
									}
								}
							}
						}
					}
					break;
				case Player.STORE_PRIVATE_BUY :
					if (trader.getBuyList() != null)
					{
						for (final TradeItem tradeItem : trader.getBuyList().getItems())
						{
							if (tradeItem.getItem().getId() == item._itemId && tradeItem.getPrice() == item._price)
							{
								found = true;
								break;
							}
						}
					}
					break;
				case Player.STORE_PRIVATE_MANUFACTURE :
					found = true;
					break;
			}
			
			if (!found)
			{
				player.sendMessage((new ServerMessage("ItemBroker.PRICE_CHANGED", player.getLang())).toString());
			}
			
			final RadarControl rc = new RadarControl(0, 1, item._player.getX(), item._player.getY(), item._player.getZ());
			player.sendPacket(rc);
			
			if (player.getNotShowTraders())
			{
				player.sendPacket(new CharInfo(trader, player));
				if (trader.getPrivateStoreType() == Player.STORE_PRIVATE_BUY)
				{
					player.sendPacket(new PrivateStoreBuyMsg(trader));
				}
				else if (trader.getPrivateStoreType() == Player.STORE_PRIVATE_SELL)
				{
					player.sendPacket(new PrivateStoreSellMsg(trader));
				}
				else if (trader.getPrivateStoreType() == Player.STORE_PRIVATE_PACKAGE_SELL)
				{
					player.sendPacket(new ExPrivateStorePackageMsg(trader));
				}
				else if (trader.getPrivateStoreType() == Player.STORE_PRIVATE_MANUFACTURE)
				{
					player.sendPacket(new RecipeShopMsg(trader));
				}
			}
			player.setTarget(trader);
		}
		else if (command.startsWith("find"))
		{
			if (!Config.ITEM_BROKER_ITEM_SEARCH)
			{
				return;
			}
			
			final String[] var = command.split(" ");
			if (var.length < 4 || var.length > 8)
			{
				player.sendMessage((new ServerMessage("ItemBroker.ENTER_SYMBOLS", player.getLang())).toString());
				return;
			}
			
			int type;
			int currentPage;
			int minEnchant = 0;
			String[] search = null;
			
			try
			{
				type = Integer.valueOf(var[1]);
				currentPage = Integer.valueOf(var[2]);
				search = new String[var.length - 3];
				String line;
				for (int i = 0; i < search.length; i++)
				{
					line = var[i + 3].trim().toLowerCase();
					search[i] = line;
					if (line.length() > 1 && line.startsWith("+"))
					{
						minEnchant = Integer.valueOf(line.substring(1));
					}
				}
			}
			catch (final Exception e)
			{
				player.sendMessage((new ServerMessage("ItemBroker.INCORRECT_DATA", player.getLang())).toString());
				return;
			}
			
			final TreeMap<Integer, TreeMap<Long, ItemTemplate>> allItems = getItems(type, player);
			if (allItems == null)
			{
				player.sendMessage((new ServerMessage("ItemBroker.TYPE_NOT_FOUND", player.getLang())).toString());
				return;
			}
			
			final List<ItemTemplate> items = new ArrayList<>();
			String line;
			TreeMap<Long, ItemTemplate> itemMap;
			ItemTemplate item;
			mainLoop : for (final Entry<Integer, TreeMap<Long, ItemTemplate>> entry : allItems.entrySet())
			{
				for (int i = 0; i < search.length; i++)
				{
					line = search[i];
					if (line.startsWith("+"))
					{
						continue;
					}
					
					final Item searchItem = ItemsParser.getInstance().getTemplate(entry.getKey());
					boolean found = false;
					for (final String lang : Config.MULTILANG_ALLOWED)
					{
						if (lang != null && searchItem.getName(lang).toLowerCase().indexOf(line) != -1)
						{
							found = true;
						}
					}
					
					if (!found)
					{
						continue mainLoop;
					}
				}
				
				itemMap = entry.getValue();
				item = null;
				for (final ItemTemplate itm : itemMap.values())
				{
					if (itm != null && itm._enchant >= minEnchant)
					{
						item = itm;
						break;
					}
				}
				
				if (item != null)
				{
					items.add(item);
				}
			}
			
			String html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/itemBroker/findList.htm");
			String template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/itemBroker/findList-template.htm");
			String block = "";
			String list = "";
			
			final StringBuilder out = new StringBuilder(200);
			
			int totalPages = items.size();
			totalPages = totalPages / Config.ITEM_BROKER_ITEMS_PER_PAGE + (totalPages % Config.ITEM_BROKER_ITEMS_PER_PAGE > 0 ? 1 : 0);
			totalPages = Math.max(1, totalPages);
			currentPage = Math.min(totalPages, Math.max(1, currentPage));
			
			if (totalPages > 1)
			{
				int page = Math.max(1, Math.min(totalPages - 1 + 1, currentPage - 1 / 2));
				
				if (currentPage > 1)
				{
					findPageNum(out, type, currentPage - 1, search, "prev");
				}
				else
				{
					findPageNum(out, type, currentPage - 1, search, "emptyLeft");
				}
				
				for (int count = 0; count < 1 && page <= totalPages; count++, page++)
				{
					if (page == currentPage)
					{
						findPageNum(out, type, page, search, "currentPage");
					}
				}
				
				if (currentPage < totalPages)
				{
					findPageNum(out, type, currentPage + 1, search, "next");
				}
				else
				{
					findPageNum(out, type, currentPage + 1, search, "emptyRight");
				}
			}
			
			if (items.size() > 0)
			{
				int count = 0;
				final ListIterator<ItemTemplate> iter = items.listIterator((currentPage - 1) * Config.ITEM_BROKER_ITEMS_PER_PAGE);
				while (iter.hasNext() && count < Config.ITEM_BROKER_ITEMS_PER_PAGE)
				{
					item = iter.next();
					final Item temp = item._item != null ? item._item.getItem() : ItemsParser.getInstance().getTemplate(item._itemId);
					if (temp == null)
					{
						continue;
					}
					block = template;
					
					block = block.replace("%icon%", temp.getIcon());
					block = block.replace("%type%", String.valueOf(type));
					block = block.replace("%itemId%", String.valueOf(item._itemId));
					block = block.replace("%ench%", String.valueOf(minEnchant));
					block = block.replace("%page%", String.valueOf(currentPage));
					if (search != null)
					{
						for (int i = 0; i < search.length; i++)
						{
							block = block.replace("%search%", " " + search[i]);
						}
					}
					else
					{
						block = block.replace("%search%", "");
					}
					block = block.replace("%name%", "<font color=\"LEVEL\">" + getItemName(player, item) + "</font>");
					
					list += block;
					count++;
				}
			}
			else
			{
				template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/itemBroker/empty-template.htm");
				template = template.replace("%type%", String.valueOf(type));
				template = template.replace("%objectId%", String.valueOf(getObjectId()));
				list += template;
			}
			html = html.replace("%pages%", out.toString());
			html = html.replace("%list%", list);
			html = html.replace("%objectId%", String.valueOf(getObjectId()));
			Util.setHtml(html, player);
		}
		else
		{
			super.onBypassFeedback(player, command);
		}
	}
	
	private void listPageNum(StringBuilder out, int type, int itemType, int page, int minEnchant, int rare, String letter)
	{
		if (letter.equalsIgnoreCase("next"))
		{
			out.append("<td width=80 align=right valign=top><button action=\"bypass -h npc_%objectId%_list " + type + " " + itemType + " " + page + " " + minEnchant + " " + rare + "\" width=16 height=16 back=\"L2UI_CH3.shortcut_next_down\" fore=\"L2UI_CH3.shortcut_next\"></td>");
		}
		else if (letter.equalsIgnoreCase("prev"))
		{
			out.append("<td width=80 align=left valign=top><button action=\"bypass -h npc_%objectId%_list " + type + " " + itemType + " " + page + " " + minEnchant + " " + rare + "\" width=16 height=16 back=\"L2UI_CH3.shortcut_prev_down\" fore=\"L2UI_CH3.shortcut_prev\"></td>");
		}
		else if (letter.equalsIgnoreCase("emptyLeft"))
		{
			out.append("<td width=80 align=left valign=top></td>");
		}
		else if (letter.equalsIgnoreCase("currentPage"))
		{
			out.append("<td width=50 align=center valign=top>[ " + page + " ]</td>");
		}
		else if (letter.equalsIgnoreCase("emptyRight"))
		{
			out.append("<td width=80 align=right valign=top></td>");
		}
		else
		{
			out.append("<td width=50 align=center valign=top><a action=\"bypass -h npc_%objectId%_list ");
			out.append(type);
			out.append(" ");
			out.append(itemType);
			out.append(" ");
			out.append(page);
			out.append(" ");
			out.append(minEnchant);
			out.append(" ");
			out.append(rare);
			out.append("\">");
			out.append(letter);
			out.append("</a></td>");
		}
	}
	
	private void listForItemPageNum(StringBuilder out, int type, int itemId, int minEnchant, int rare, int itemType, int page, int returnPage, String[] search, String letter)
	{
		if (letter.equalsIgnoreCase("next"))
		{
			String search_w = "";
			if (search != null)
			{
				for (int i = 0; i < search.length; i++)
				{
					search_w += " " + search[i] + "";
				}
			}
			out.append("<td width=80 align=right valign=top><button action=\"bypass -h npc_%objectId%_selectForItem " + type + " " + itemType + " " + minEnchant + " " + rare + " " + itemType + " " + page + " " + returnPage + "" + search_w + "\" width=16 height=16 back=\"L2UI_CH3.shortcut_next_down\" fore=\"L2UI_CH3.shortcut_next\"></td>");
		}
		else if (letter.equalsIgnoreCase("prev"))
		{
			String search_w = "";
			if (search != null)
			{
				for (int i = 0; i < search.length; i++)
				{
					search_w += " " + search[i] + "";
				}
			}
			out.append("<td width=80 align=left valign=top><button action=\"bypass -h npc_%objectId%_selectForItem " + type + " " + itemType + " " + minEnchant + " " + rare + " " + itemType + " " + page + " " + returnPage + "" + search_w + "\" width=16 height=16 back=\"L2UI_CH3.shortcut_prev_down\" fore=\"L2UI_CH3.shortcut_prev\"></td>");
		}
		else if (letter.equalsIgnoreCase("emptyLeft"))
		{
			out.append("<td width=80 align=left valign=top></td>");
		}
		else if (letter.equalsIgnoreCase("emptyRight"))
		{
			out.append("<td width=80 align=right valign=top></td>");
		}
		else if (letter.equalsIgnoreCase("currentPage"))
		{
			out.append("<td width=50 align=center valign=top>[ " + page + " ]</td>");
		}
		else
		{
			out.append("<td width=50 align=center valign=top><a action=\"bypass -h npc_%objectId%_selectForItem ");
			out.append(type);
			out.append(" ");
			out.append(itemId);
			out.append(" ");
			out.append(minEnchant);
			out.append(" ");
			out.append(rare);
			out.append(" ");
			out.append(itemType);
			out.append(" ");
			out.append(page);
			out.append(" ");
			out.append(returnPage);
			if (search != null)
			{
				for (int i = 0; i < search.length; i++)
				{
					out.append(" ");
					out.append(search[i]);
				}
			}
			out.append("\">");
			out.append(letter);
			out.append("</a></td>");
		}
	}
	
	public void updateInfo(Player player, NpcInstance npc)
	{
		NpcInfo info = _npcInfos.get(npc.getObjectId());
		if (info == null || info.lastUpdate < (System.currentTimeMillis() - (Config.ITEM_BROKER_TIME_UPDATE * 1000L)))
		{
			info = new NpcInfo();
			info.lastUpdate = System.currentTimeMillis();
			info.bestBuyItems = new TreeMap<>();
			info.bestSellItems = new TreeMap<>();
			info.bestCraftItems = new TreeMap<>();
			
			int itemObjId = 0;
			
			final Collection<Player> knownPlayers = World.getInstance().getAroundTraders(this);
			if ((knownPlayers == null) || knownPlayers.isEmpty())
			{
				return;
			}
			
			for (final Player pl : knownPlayers)
			{
				TreeMap<Integer, TreeMap<Long, ItemTemplate>> items = null;
				TradeList tradeList = null;
				
				final int type = pl.getPrivateStoreType();
				switch (type)
				{
					case Player.STORE_PRIVATE_SELL :
						items = info.bestSellItems;
						tradeList = pl.getSellList();
						
						for (final TradeItem item : tradeList.getItems())
						{
							final Item temp = item.getItem();
							if (temp == null)
							{
								continue;
							}
							TreeMap<Long, ItemTemplate> oldItems = items.get(temp.getId());
							if (oldItems == null)
							{
								oldItems = new TreeMap<>();
								items.put(temp.getId(), oldItems);
							}
							final ItemTemplate newItem = new ItemTemplate(item.getItem().getId(), type, item.getPrice(), item.getCount(), item.getEnchant(), pl.getObjectId(), pl.getName(null), pl.getLocation(), item.getObjectId(), item, false);
							long key = newItem._price * 100;
							while (key < newItem._price * 100 + 100 && oldItems.containsKey(key))
							{
								key++;
							}
							oldItems.put(key, newItem);
						}
						break;
					case Player.STORE_PRIVATE_PACKAGE_SELL :
						items = info.bestSellItems;
						tradeList = pl.getSellList();
						
						long packagePrice = 0;
						for (final TradeItem item : tradeList.getItems())
						{
							packagePrice += item.getPrice() * item.getCount();
						}
						
						for (final TradeItem item : tradeList.getItems())
						{
							final Item temp = item.getItem();
							if (temp == null)
							{
								continue;
							}
							TreeMap<Long, ItemTemplate> oldItems = items.get(temp.getId());
							if (oldItems == null)
							{
								oldItems = new TreeMap<>();
								items.put(temp.getId(), oldItems);
							}
							final ItemTemplate newItem = new ItemTemplate(item.getItem().getId(), type, packagePrice, item.getCount(), item.getEnchant(), pl.getObjectId(), pl.getName(null), pl.getLocation(), item.getObjectId(), item, true);
							long key = newItem._price * 100;
							while (key < newItem._price * 100 + 100 && oldItems.containsKey(key))
							{
								key++;
							}
							oldItems.put(key, newItem);
						}
						break;
					case Player.STORE_PRIVATE_BUY :
						items = info.bestBuyItems;
						tradeList = pl.getBuyList();
						
						for (final TradeItem item : tradeList.getItems())
						{
							final Item temp = item.getItem();
							if (temp == null)
							{
								continue;
							}
							TreeMap<Long, ItemTemplate> oldItems = items.get(temp.getId());
							if (oldItems == null)
							{
								oldItems = new TreeMap<>();
								items.put(temp.getId(), oldItems);
							}
							final ItemTemplate newItem = new ItemTemplate(item.getItem().getId(), type, item.getPrice(), item.getCount(), item.getEnchant(), pl.getObjectId(), pl.getName(null), pl.getLocation(), itemObjId++, item, false);
							long key = newItem._price * 100;
							while (key < newItem._price * 100 + 100 && oldItems.containsKey(key))
							{
								key++;
							}
							oldItems.put(key, newItem);
						}
						break;
					case Player.STORE_PRIVATE_MANUFACTURE :
						items = info.bestCraftItems;
						final Map<Integer, ManufactureItemTemplate> createList = pl.getManufactureItems();
						if (createList == null)
						{
							continue;
						}
						
						for (final ManufactureItemTemplate mitem : createList.values())
						{
							final int recipeId = mitem.getRecipeId();
							final RecipeList recipe = RecipeParser.getInstance().getRecipeList(recipeId);
							if (recipe == null)
							{
								continue;
							}
							
							final Item temp = ItemsParser.getInstance().getTemplate(recipe.getItemId());
							if (temp == null)
							{
								continue;
							}
							TreeMap<Long, ItemTemplate> oldItems = items.get(temp.getId());
							if (oldItems == null)
							{
								oldItems = new TreeMap<>();
								items.put(temp.getId(), oldItems);
							}
							final ItemTemplate newItem = new ItemTemplate(recipe.getItemId(), type, mitem.getCost(), recipe.getCount(), 0, pl.getObjectId(), pl.getName(null), pl.getLocation(), itemObjId++, null, false);
							long key = newItem._price * 100;
							while (key < newItem._price * 100 + 100 && oldItems.containsKey(key))
							{
								key++;
							}
							oldItems.put(key, newItem);
						}
						break;
					default :
						continue;
				}
			}
			_npcInfos.put(npc.getObjectId(), info);
		}
	}
	
	private void findPageNum(StringBuilder out, int type, int page, String[] search, String letter)
	{
		if (letter.equalsIgnoreCase("next"))
		{
			String search_w = "";
			if (search != null)
			{
				for (int i = 0; i < search.length; i++)
				{
					search_w += " " + search[i] + "";
				}
			}
			out.append("<td width=80 align=right valign=top><button action=\"bypass -h npc_%objectId%_find " + type + " " + page + "" + search_w + "\" width=16 height=16 back=\"L2UI_CH3.shortcut_next_down\" fore=\"L2UI_CH3.shortcut_next\"></td>");
		}
		else if (letter.equalsIgnoreCase("prev"))
		{
			String search_w = "";
			if (search != null)
			{
				for (int i = 0; i < search.length; i++)
				{
					search_w += " " + search[i] + "";
				}
			}
			out.append("<td width=80 align=left valign=top><button action=\"bypass -h npc_%objectId%_find " + type + " " + page + "" + search_w + "\" width=16 height=16 back=\"L2UI_CH3.shortcut_prev_down\" fore=\"L2UI_CH3.shortcut_prev\"></td>");
		}
		else if (letter.equalsIgnoreCase("emptyLeft"))
		{
			out.append("<td width=80 align=left valign=top></td>");
		}
		else if (letter.equalsIgnoreCase("emptyRight"))
		{
			out.append("<td width=80 align=right valign=top></td>");
		}
		else if (letter.equalsIgnoreCase("currentPage"))
		{
			out.append("<td width=50 align=center valign=top>[ " + page + " ]</td>");
		}
		else
		{
			out.append("<td width=50 align=center valign=top><a action=\"bypass -h npc_%objectId%_find ");
			out.append(type);
			out.append(" ");
			out.append(page);
			if (search != null)
			{
				for (int i = 0; i < search.length; i++)
				{
					out.append(" ");
					out.append(search[i]);
				}
			}
			out.append("\">");
			out.append(letter);
			out.append("</a></td>");
		}
	}
	
	public class NpcInfo
	{
		public long lastUpdate;
		public TreeMap<Integer, TreeMap<Long, ItemTemplate>> bestSellItems;
		public TreeMap<Integer, TreeMap<Long, ItemTemplate>> bestBuyItems;
		public TreeMap<Integer, TreeMap<Long, ItemTemplate>> bestCraftItems;
	}
	
	protected static String getItemEnchant(ItemTemplate template)
	{
		String itemEnchant = "";
		if (template._enchant > 0)
		{
			if (template._rare)
			{
				itemEnchant += "<font color=\"FF0000\">+" + template._enchant + "</font> ";
			}
			else
			{
				itemEnchant += "<font color=\"b02e31\">+" + template._enchant + "</font> ";
			}
		}
		return itemEnchant;
	}
	
	protected static String getItemAttribute(Player player, ItemTemplate template)
	{
		String itemAtt = "";
		if (template._item != null)
		{
			if (template._item.getAttackElementType() >= 0)
			{
				itemAtt += " &nbsp;<font color=\"7CFC00\">+" + template._item.getAttackElementPower();
				switch (template._item.getAttackElementType())
				{
					case 0 :
						itemAtt += " " + ServerStorage.getInstance().getString(player.getLang(), "ItemBroker.FIRE") + "</font>";
						break;
					case 1 :
						itemAtt += " " + ServerStorage.getInstance().getString(player.getLang(), "ItemBroker.WATER") + "</font>";
						break;
					case 2 :
						itemAtt += " " + ServerStorage.getInstance().getString(player.getLang(), "ItemBroker.WIND") + "</font>";
						break;
					case 3 :
						itemAtt += " " + ServerStorage.getInstance().getString(player.getLang(), "ItemBroker.EARTH") + "</font>";
						break;
					case 4 :
						itemAtt += " " + ServerStorage.getInstance().getString(player.getLang(), "ItemBroker.HOLY") + "</font>";
						break;
					case 5 :
						itemAtt += " " + ServerStorage.getInstance().getString(player.getLang(), "ItemBroker.DARK") + "</font>";
						break;
				}
			}
			else
			{
				int fire = 0, water = 0, wind = 0, earth = 0, holy = 0, unholy = 0;
				if (template._item.getItem().getElementals() != null)
				{
					for (final Elementals elm : template._item.getItem().getElementals())
					{
						if (elm.getElement() == elm.getFire())
						{
							fire = elm.getValue();
						}
						if (elm.getElement() == elm.getWater())
						{
							water = elm.getValue();
						}
						if (elm.getElement() == elm.getWind())
						{
							wind = elm.getValue();
						}
						if (elm.getElement() == elm.getEarth())
						{
							earth = elm.getValue();
						}
						if (elm.getElement() == elm.getHoly())
						{
							holy = elm.getValue();
						}
						if (elm.getElement() == elm.getUnholy())
						{
							unholy = elm.getValue();
						}
						
					}
				}
				
				if (fire + water + wind + earth + holy + unholy > 0)
				{
					itemAtt += " &nbsp;<font color=\"7CFC00\">+";
					if (fire > 0)
					{
						itemAtt += "+" + fire + " " + ServerStorage.getInstance().getString(player.getLang(), "ItemBroker.FIRE") + " ";
					}
					if (water > 0)
					{
						itemAtt += "+" + water + " " + ServerStorage.getInstance().getString(player.getLang(), "ItemBroker.WATER") + " ";
					}
					if (wind > 0)
					{
						itemAtt += "+" + wind + " " + ServerStorage.getInstance().getString(player.getLang(), "ItemBroker.WIND") + " ";
					}
					if (earth > 0)
					{
						itemAtt += "+" + earth + " " + ServerStorage.getInstance().getString(player.getLang(), "ItemBroker.EARTH") + " ";
					}
					if (holy > 0)
					{
						itemAtt += "+" + holy + " " + ServerStorage.getInstance().getString(player.getLang(), "ItemBroker.HOLY") + " ";
					}
					if (unholy > 0)
					{
						itemAtt += "+" + unholy + " " + ServerStorage.getInstance().getString(player.getLang(), "ItemBroker.DARK") + " ";
					}
					itemAtt += "</font>";
				}
			}
		}
		return itemAtt;
	}
	
	protected static String getItemName(Player player, ItemTemplate template)
	{
		String itemName = "";
		if (template._rare)
		{
			itemName += "<font color=\"0000FF\">" + Util.getItemName(player, template._itemId) + "</font>";
		}
		else
		{
			itemName += "<font color=\"LEVEL\">" + Util.getItemName(player, template._itemId) + "</font>";
		}
		return itemName;
	}
	
	public class ItemTemplate
	{
		public final int _itemId;
		public final int _itemObjId;
		public final int _type;
		public final long _price;
		public final long _count;
		public final int _enchant;
		public final boolean _rare;
		public final int _merchantObjectId;
		public final String _merchantName;
		public final Location _player;
		public final TradeItem _item;
		public final boolean _isPackage;
		
		public ItemTemplate(int itemId, int type, long price, long count, int enchant, int mobjectId, String merchantName, Location player, int itemObjId, TradeItem item, boolean isPkg)
		{
			_itemId = itemId;
			_type = type;
			_price = price;
			_count = count;
			_enchant = enchant;
			_rare = ArrayUtils.contains(RARE_ITEMS, itemId);
			_merchantObjectId = mobjectId;
			_merchantName = merchantName;
			_player = player;
			_itemObjId = itemObjId;
			_item = item;
			_isPackage = isPkg;
		}
	}
	
	private TreeMap<Integer, TreeMap<Long, ItemTemplate>> getItems(int type, Player player)
	{
		if (player == null)
		{
			return null;
		}
		updateInfo(player, this);
		final NpcInfo info = _npcInfos.get(getObjectId());
		if (info == null)
		{
			return null;
		}
		switch (type)
		{
			case Player.STORE_PRIVATE_SELL :
				return info.bestSellItems;
			case Player.STORE_PRIVATE_BUY :
				return info.bestBuyItems;
			case Player.STORE_PRIVATE_MANUFACTURE :
				return info.bestCraftItems;
		}
		return null;
	}
	
	private static boolean getItemClassType(int type, Item item)
	{
		switch (type)
		{
			case 1 :
				return item.isWeapon();
			case 2 :
				return item.isArmor() || item.isShield();
			case 3 :
				return item.isJewel();
			case 4 :
				return item.getBodyPart() == Item.SLOT_HAIR || item.getBodyPart() == Item.SLOT_HAIRALL || item.getBodyPart() == Item.SLOT_HAIR2;
			case 5 :
				return item.isConsumable();
			case 6 :
				return item.getItemType() == EtcItemType.MATERIAL;
			case 7 :
				return item.isKeyMatherial();
			case 8 :
				return item.isRecipe();
			case 9 :
				return item.getItemType() == EtcItemType.SPELLBOOK;
			case 10 :
				return item.isLifeStone() || HennaParser.getInstance().isHenna(item.getId());
			case 11 :
				return item.getItemType() instanceof EtcItemType && (item.getItemType() != EtcItemType.MATERIAL && item.getItemType() != EtcItemType.SPELLBOOK && !item.isConsumable() && !item.isKeyMatherial() && !item.isRecipe());
			case 12 :
				return item.isExtractableItem();
		}
		return false;
	}
}