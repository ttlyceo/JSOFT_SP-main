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
package l2e.gameserver.data.parser;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.DOMException;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import l2e.commons.math.SafeMath;
import l2e.commons.util.Util;
import l2e.commons.util.file.filter.NumericNameFilter;
import l2e.gameserver.Config;
import l2e.gameserver.data.DocumentParser;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.items.Item;
import l2e.gameserver.model.items.multisell.Entry;
import l2e.gameserver.model.items.multisell.Ingredient;
import l2e.gameserver.model.items.multisell.ListContainer;
import l2e.gameserver.model.items.multisell.PreparedListContainer;
import l2e.gameserver.model.stats.StatsSet;
import l2e.gameserver.model.strings.server.ServerMessage;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.ExPCCafePointInfo;
import l2e.gameserver.network.serverpackets.MultiSellList;
import l2e.gameserver.network.serverpackets.SystemMessage;

public class MultiSellParser extends DocumentParser
{
	public static final int PAGE_SIZE = 40;
	public static final int PC_BANG_POINTS = Config.PC_POINT_ID;
	public static final int CLAN_REPUTATION = -200;
	public static final int FAME = -300;
	public static final int EXP = Config.EXP_ID;
	public static final int SP = Config.SP_ID;

	private final Map<Integer, ListContainer> _entries = new HashMap<>();
	
	protected MultiSellParser()
	{
		setCurrentFileFilter(new NumericNameFilter());
		load();
	}

	@Override
	public final void load()
	{
		_entries.clear();
		parseDirectory("data/stats/npcs/multisell", false);
		if (Config.CUSTOM_MULTISELLS)
		{
			parseDirectory("data/stats/npcs/multisell/custom", false);
		}

		verify();
		info("Loaded " + _entries.size() + " lists.");
	}
	
	@Override
	protected void reloadDocument()
	{
	}
	
	@Override
	protected final void parseDocument()
	{
		try
		{
			final int id = Integer.parseInt(getCurrentFile().getName().replaceAll(".xml", ""));
			int entryId = 1;
			Node att;
			final var list = new ListContainer(id);

			for (Node n = getCurrentDocument().getFirstChild(); n != null; n = n.getNextSibling())
			{
				if ("list".equalsIgnoreCase(n.getNodeName()))
				{
					att = n.getAttributes().getNamedItem("applyTaxes");
					list.setApplyTaxes((att != null) && Boolean.parseBoolean(att.getNodeValue()));

					att = n.getAttributes().getNamedItem("useRate");
					if (att != null)
					{
						try
						{
							
							list.setUseRate(Double.valueOf(att.getNodeValue()));
							if (list.getUseRate() <= 1e-6)
							{
								throw new NumberFormatException("The value cannot be 0");
							}
						}
						catch (final NumberFormatException e)
						{
							
							try
							{
								list.setUseRate(Config.class.getField(att.getNodeValue()).getDouble(Config.class));
							}
							catch (final Exception e1)
							{
								warn(e1.getMessage() + getCurrentDocument().getLocalName());
								list.setUseRate(1.0);
							}

						}
						catch (final DOMException e)
						{
							warn(e.getMessage() + getCurrentDocument().getLocalName());
						}
					}

					att = n.getAttributes().getNamedItem("maintainEnchantment");
					list.setMaintainEnchantment((att != null) && Boolean.parseBoolean(att.getNodeValue()));
					att = n.getAttributes().getNamedItem("isAllowCheckEquipItems");
					list.setIsAllowCheckEquipItems((att != null) && Boolean.parseBoolean(att.getNodeValue()));

					for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
					{
						if ("item".equalsIgnoreCase(d.getNodeName()))
						{
							final Entry e = parseEntry(id, d, entryId++, list);
							list.getEntries().add(e);
						}
						else if ("npcs".equalsIgnoreCase(d.getNodeName()))
						{
							for (Node b = d.getFirstChild(); b != null; b = b.getNextSibling())
							{
								if ("npc".equalsIgnoreCase(b.getNodeName()))
								{
									if (Util.isDigit(b.getTextContent()))
									{
										list.allowNpc(Integer.parseInt(b.getTextContent()));
									}
								}
							}
						}
						else if ("items".equalsIgnoreCase(d.getNodeName()))
						{
							for (Node b = d.getFirstChild(); b != null; b = b.getNextSibling())
							{
								if ("item".equalsIgnoreCase(b.getNodeName()))
								{
									if (Util.isDigit(b.getTextContent()))
									{
										list.allowItem(Integer.parseInt(b.getTextContent()));
									}
								}
							}
						}
					}
				}
			}
			_entries.put(id, list);
		}
		catch (final Exception e)
		{
			warn("Error in file " + getCurrentFile(), e);
		}
	}

	private final Entry parseEntry(int id, Node n, int entryId, ListContainer list)
	{
		final var first = n.getFirstChild();
		final var entry = new Entry(entryId);

		NamedNodeMap attrs;
		Node att;
		StatsSet set;

		for (n = first; n != null; n = n.getNextSibling())
		{
			if ("ingredient".equalsIgnoreCase(n.getNodeName()))
			{
				attrs = n.getAttributes();
				set = new StatsSet();
				for (int i = 0; i < attrs.getLength(); i++)
				{
					att = attrs.item(i);
					set.set(att.getNodeName(), att.getNodeValue());
				}
				entry.addIngredient(new Ingredient(set));
			}
			else if ("production".equalsIgnoreCase(n.getNodeName()))
			{
				attrs = n.getAttributes();
				set = new StatsSet();
				for (int i = 0; i < attrs.getLength(); i++)
				{
					att = attrs.item(i);
					set.set(att.getNodeName(), att.getNodeValue());
				}
				entry.addProduct(new Ingredient(set));
			}
		}

		if ((entry.getIngredients().size() == 1) && (entry.getIngredients().get(0).getId() == 57))
		{
			long count = 0L;
			Item item;
			for (final var product : entry.getProducts())
			{
				if (product.getId() < 0)
				{
					continue;
				}
				
				item = ItemsParser.getInstance().getTemplate(product.getId());
				if (item == null)
				{
					warn("MultiSell [" + id + "] Production [" + entry.getProducts().get(0).getId() + "] not found!");
					return null;
				}
				count = SafeMath.addAndCheck(count, SafeMath.mulAndCheck(entry.getProducts().get(0).getCount(), item.getReferencePrice()));
			}
			if ((count > entry.getIngredients().get(0).getCount()) && (Config.ALLOW_MULTISELL_DEBUG))
			{
				warn("MultiSell [" + id + "] Production [" + entry.getEntryId() + "] [" + entry.getProducts().get(0).getId() + "] price is lower than referenced | " + count + " > " + entry.getIngredients().get(0).getCount());
			}
		}
		return entry;
	}

	public final void separateAndSend(int listId, Player player, Npc npc, boolean inventoryOnly, double productMultiplier, double ingredientMultiplier)
	{
		if (player.isProcessingTransaction())
		{
			return;
		}
		
		final var template = _entries.get(listId);
		if (template == null)
		{
			warn("can't find list id: " + listId + " requested by player: " + player.getName(null) + ", npcId:" + (npc != null ? npc.getId() : 0));
			return;
		}

		if (((npc != null) && !template.isNpcAllowed(npc.getId())) || ((npc == null) && template.isNpcOnly()))
		{
			warn("player " + player + " attempted to open multisell " + listId + " from npc " + npc + " which is not allowed!");
			return;
		}
		
		if (template.isItemOnly())
		{
			for (final var item : template.getItemsAllowed())
			{
				if (player.getInventory().getItemByItemId(item) == null)
				{
					return;
				}
			}
		}

		final var list = new PreparedListContainer(template, inventoryOnly, player, npc);

		if ((productMultiplier != 1) || (ingredientMultiplier != 1))
		{
			for (final Entry entry : list.getEntries())
			{
				for (final var product : entry.getProducts())
				{
					product.setCount((long) Math.max(product.getCount() * productMultiplier, 1));
				}
				for (final var ingredient : entry.getIngredients())
				{
					ingredient.setCount((long) Math.max(ingredient.getCount() * ingredientMultiplier, 1));
				}
			}
		}
		int index = 0;
		do
		{
			player.sendPacket(new MultiSellList(list, index));
			index += PAGE_SIZE;
		}
		while (index < list.getEntries().size());

		if (player.isGM())
		{
			player.sendMessage("MutliSell: " + listId + ".xml");
		}
		
		player.setMultiSell(list);
	}

	public final void separateAndSend(int listId, Player player, Npc npc, boolean inventoryOnly)
	{
		separateAndSend(listId, player, npc, inventoryOnly, 1, 1);
	}

	public static final boolean checkSpecialIngredient(int id, long amount, Player player)
	{
		if (id == PC_BANG_POINTS)
		{
			if (player.getPcBangPoints() < amount)
			{
				player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
				return false;
			}
		}
		else if (id == CLAN_REPUTATION)
		{
			if (player.getClan() == null)
			{
				player.sendPacket(SystemMessageId.YOU_ARE_NOT_A_CLAN_MEMBER);
				return false;
			}

			if (!player.isClanLeader())
			{
				player.sendPacket(SystemMessageId.ONLY_THE_CLAN_LEADER_IS_ENABLED);
				return false;
			}

			if (player.getClan().getReputationScore() < amount)
			{
				player.sendPacket(SystemMessageId.THE_CLAN_REPUTATION_SCORE_IS_TOO_LOW);
				return false;
			}
		}
		else if (id == FAME)
		{
			if (player.getFame() < amount)
			{
				player.sendPacket(SystemMessageId.NOT_ENOUGH_FAME_POINTS);
				return false;
			}
		}
		else if (id == EXP)
		{
			if (player.getExp() < amount)
			{
				player.sendMessage((new ServerMessage("Util.HAVE_NO_EXP", player.getLang())).toString());
				return false;
			}
		}
		else if (id == SP)
		{
			if (player.getSp() < amount)
			{
				player.sendPacket(SystemMessageId.NOT_ENOUGH_SP);
				return false;
			}
		}
		return true;
	}

	public static final boolean getSpecialIngredient(int id, long amount, Player player)
	{
		if (id == PC_BANG_POINTS)
		{
			final int cost = player.getPcBangPoints() - (int) (amount);
			player.setPcBangPoints(cost);
			final var smsgpc = SystemMessage.getSystemMessage(SystemMessageId.USING_S1_PCPOINT);
			smsgpc.addNumber((int) amount);
			player.sendPacket(smsgpc);
			player.sendPacket(new ExPCCafePointInfo(player.getPcBangPoints(), (int) amount, false, false, 1));
			return true;
		}
		else if (id == CLAN_REPUTATION && player.getClan() != null)
		{
			player.getClan().takeReputationScore((int) amount, true);
			final var smsg = SystemMessage.getSystemMessage(SystemMessageId.S1_DEDUCTED_FROM_CLAN_REP);
			smsg.addNumber((int) amount);
			player.sendPacket(smsg);
			return true;
		}
		else if (id == FAME)
		{
			player.setFame(player.getFame() - (int) amount);
			player.sendUserInfo();
			return true;
		}
		else if (id == EXP)
		{
			player.removeExpAndSp(amount, 0);
			player.sendUserInfo();
			return true;
		}
		else if (id == SP)
		{
			player.removeExpAndSp(0, (int) amount);
			player.sendUserInfo();
			return true;
		}
		return false;
	}

	public static final void addSpecialProduct(int id, long amount, Player player)
	{
		if (id == EXP)
		{
			player.addExpAndSp((int) amount, 0);
			player.sendUserInfo();
		}
		
		if (id == SP)
		{
			player.addExpAndSp(0, (int) amount);
			player.sendUserInfo();
		}
		
		switch (id)
		{
			case CLAN_REPUTATION :
				if (player.getClan() != null)
				{
					player.getClan().addReputationScore((int) amount, true);
				}
				break;
			case FAME :
				player.setFame((int) (player.getFame() + amount));
				player.sendUserInfo();
				break;
		}
	}

	private final void verify()
	{
		ListContainer list;
		final var iter = _entries.values().iterator();
		while (iter.hasNext())
		{
			list = iter.next();

			for (final var ent : list.getEntries())
			{
				for (final var ing : ent.getIngredients())
				{
					if (!verifyIngredient(ing))
					{
						warn("can't find ingredient with itemId: " + ing.getId() + " in list: " + list.getListId());
					}
				}
				for (final var ing : ent.getProducts())
				{
					if (!verifyIngredient(ing))
					{
						warn("can't find product with itemId: " + ing.getId() + " in list: " + list.getListId());
					}
				}
			}
		}
	}

	private final boolean verifyIngredient(Ingredient ing)
	{
		switch (ing.getId())
		{
			case CLAN_REPUTATION :
			case FAME :
				return true;
			default :
				if (ing.getId() == PC_BANG_POINTS || ing.getId() == EXP || ing.getId() == SP)
				{
					return true;
				}
				return ing.getTemplate() != null;
		}
	}
	
	public Map<Integer, ListContainer> getEntries()
	{
		return _entries;
	}

	public static MultiSellParser getInstance()
	{
		return SingletonHolder._instance;
	}

	private static class SingletonHolder
	{
		protected static final MultiSellParser _instance = new MultiSellParser();
	}
}