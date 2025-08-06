/*
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
 * this program. If not, see <>.
 */
package services;

import l2e.commons.util.Rnd;
import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.data.htm.HtmCache;
import l2e.gameserver.data.parser.ItemsParser;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.handler.communityhandlers.CommunityBoardHandler;
import l2e.gameserver.handler.communityhandlers.ICommunityBoardHandler;
import l2e.gameserver.handler.communityhandlers.impl.AbstractCommunity;
import l2e.gameserver.model.Augmentation;
import l2e.gameserver.model.Elementals;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.items.Item;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.items.itemcontainer.Inventory;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.ExPCCafePointInfo;
import l2e.gameserver.network.serverpackets.SystemMessage;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.*;

/**
 * Created by LordWinter (Update Version)
 */
public class CommunityCraft extends AbstractCommunity implements ICommunityBoardHandler
{
	private static final int RECIPES_PER_PAGE = 12;
	
	private final List<GroupTemplate> _groups = new ArrayList<>();
	
	public CommunityCraft()
	{
		load();
		if (Config.DEBUG)
		{
			_log.info(getClass().getSimpleName() + ": Loading all functions.");
		}
	}

	protected void load()
	{
		_groups.clear();
		try
		{
			final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);
			
			final File file = new File(Config.DATAPACK_ROOT, "data/stats/services/communityCraft.xml");
			if (!file.exists())
			{
				_log.warn(getClass().getSimpleName() + ": Couldn't find data/stats/services/" + file.getName());
				return;
			}
			
			final Document doc = factory.newDocumentBuilder().parse(file);
			NamedNodeMap attrs;
			
			for (Node list = doc.getFirstChild(); list != null; list = list.getNextSibling())
			{
				if ("list".equalsIgnoreCase(list.getNodeName()))
				{
					for (Node recipe = list.getFirstChild(); recipe != null; recipe = recipe.getNextSibling())
					{
						if ("group".equalsIgnoreCase(recipe.getNodeName()))
						{
							attrs = recipe.getAttributes();
							final int gId = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
							final String gIcon = attrs.getNamedItem("icon").getNodeValue();
							final String gNameEn = attrs.getNamedItem("nameEn").getNodeValue();
							final String gNameRu = attrs.getNamedItem("nameRu").getNodeValue();
							final int groupId = Integer.parseInt(attrs.getNamedItem("groupId").getNodeValue());
							final List<RecipeTemplate> rList = new ArrayList<>();
							for (Node gd = recipe.getFirstChild(); gd != null; gd = gd.getNextSibling())
							{
								if ("recipe".equalsIgnoreCase(gd.getNodeName()))
								{
									attrs = gd.getAttributes();
									final int recipeId = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
									final String icon = attrs.getNamedItem("icon").getNodeValue();
									final String nameEn = attrs.getNamedItem("nameEn").getNodeValue();
									final String nameRu = attrs.getNamedItem("nameRu").getNodeValue();
									final int chance = Integer.parseInt(attrs.getNamedItem("chance").getNodeValue());
									final boolean canDouble = attrs.getNamedItem("canDouble") != null ? Boolean.parseBoolean(attrs.getNamedItem("canDouble").getNodeValue()) : false;
									final int doubleChance = attrs.getNamedItem("doubleChance") != null ? Integer.parseInt(attrs.getNamedItem("doubleChance").getNodeValue()) : 0;
									final boolean isRandomCraft = attrs.getNamedItem("isRandomCraft") != null ? Boolean.parseBoolean(attrs.getNamedItem("isRandomCraft").getNodeValue()) : false;
									final boolean isSaveEnhant = attrs.getNamedItem("saveEnchant") != null ? Boolean.parseBoolean(attrs.getNamedItem("saveEnchant").getNodeValue()) : false;
									final boolean isSaveAugment = attrs.getNamedItem("saveAugment") != null ? Boolean.parseBoolean(attrs.getNamedItem("saveAugment").getNodeValue()) : false;
									final boolean isSaveAtt = attrs.getNamedItem("saveAtt") != null ? Boolean.parseBoolean(attrs.getNamedItem("saveAtt").getNodeValue()) : false;
									
									final List<ItemTemplate> requestItems = new ArrayList<>();
									final List<ItemTemplate> rewardItems = new ArrayList<>();
									final List<Skill> rewardSkills = new ArrayList<>();
									
									String descrEn = null, descrRu = null;
									
									for (Node cd = gd.getFirstChild(); cd != null; cd = cd.getNextSibling())
									{
										if ("description".equalsIgnoreCase(cd.getNodeName()))
										{
											for (Node rt = cd.getFirstChild(); rt != null; rt = rt.getNextSibling())
											{
												if ("descrEn".equalsIgnoreCase(rt.getNodeName()))
												{
													descrEn = rt.getAttributes().getNamedItem("value").getNodeValue();
												}
												else if ("descrRu".equalsIgnoreCase(rt.getNodeName()))
												{
													descrRu = rt.getAttributes().getNamedItem("value").getNodeValue();
												}
											}
										}
										else if ("requestItems".equalsIgnoreCase(cd.getNodeName()))
										{
											for (Node rt = cd.getFirstChild(); rt != null; rt = rt.getNextSibling())
											{
												if ("item".equalsIgnoreCase(rt.getNodeName()))
												{
													attrs = rt.getAttributes();
													final int id = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
													final long min = Long.parseLong(attrs.getNamedItem("min").getNodeValue());
													final long max = attrs.getNamedItem("max") != null ? Long.parseLong(attrs.getNamedItem("max").getNodeValue()) : min;
													final boolean isTakeItem = attrs.getNamedItem("takeItem") != null ? Boolean.parseBoolean(attrs.getNamedItem("takeItem").getNodeValue()) : true;
													requestItems.add(new ItemTemplate(id, min, max, 100, isTakeItem));
												}
											}
										}
										else if ("rewardItems".equalsIgnoreCase(cd.getNodeName()))
										{
											for (Node rd = cd.getFirstChild(); rd != null; rd = rd.getNextSibling())
											{
												if ("item".equalsIgnoreCase(rd.getNodeName()))
												{
													attrs = rd.getAttributes();
													final int id = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
													final long min = Long.parseLong(attrs.getNamedItem("min").getNodeValue());
													final long max = attrs.getNamedItem("max") != null ? Long.parseLong(attrs.getNamedItem("max").getNodeValue()) : min;
													final double iChance = attrs.getNamedItem("chance") != null ? Double.parseDouble(attrs.getNamedItem("chance").getNodeValue()) : 100;
													rewardItems.add(new ItemTemplate(id, min, max, iChance, false));
												}
											}
										}
										else if ("rewardSkills".equalsIgnoreCase(cd.getNodeName()))
										{
											for (Node rd = cd.getFirstChild(); rd != null; rd = rd.getNextSibling())
											{
												if ("skill".equalsIgnoreCase(rd.getNodeName()))
												{
													attrs = rd.getAttributes();
													final int id = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
													final int level = Integer.parseInt(attrs.getNamedItem("level").getNodeValue());
													final Skill sk = SkillsParser.getInstance().getInfo(id, level);
													if (sk != null)
													{
														rewardSkills.add(sk);
													}
												}
											}
										}
									}
									rList.add(new RecipeTemplate(recipeId, icon, nameEn, nameRu, descrEn, descrRu, chance, requestItems, rewardItems, rewardSkills, canDouble, doubleChance, isRandomCraft, isSaveEnhant, isSaveAugment, isSaveAtt));
								}
							}
							final GroupTemplate group = new GroupTemplate(gId, groupId, gIcon, gNameEn, gNameRu, rList);
							_groups.add(group);
						}
					}
				}
			}
		}
		catch (final Exception e)
		{
			_log.warn(getClass().getSimpleName() + ": Error while loading recipes: " + e);
		}
	}

	@Override
	public String[] getBypassCommands()
	{
		return new String[]
		{
		        "_bbscraftMain", "_bbscraftInGroup", "_bbscraftGroup", "_bbscraftInfo", "_bbsdoCraft"
		};
	}
	
	@Override
	public void onBypassCommand(String command, Player player)
	{
		final StringTokenizer st = new StringTokenizer(command, "_");
		final String cmd = st.nextToken();

		if ("bbscraftMain".equals(cmd))
		{
			final String html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/craft/index.htm");
			separateAndSend(html, player);
		}
		else if ("bbscraftInGroup".equals(cmd))
		{
			final int id = Integer.parseInt(st.nextToken());
			final int page = Integer.parseInt(st.nextToken());
			int groupId = 1;
			String html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/craft/group.htm");
			final String template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/craft/group-template.htm");
			
			String block = "";
			String list = "";
			
			final List<GroupTemplate> groups = getGroups(id);
			if (groups == null || groups.isEmpty())
			{
				onBypassCommand("_bbscraftMain", player);
				return;
			}
			
			final int perpage = RECIPES_PER_PAGE;
			int counter = 0;
			final int totalSize = groups.size();
			final boolean isThereNextPage = totalSize > perpage;
			
			list += "<table width=500><tr>";
			int countt = 0;
			for (int i = (page - 1) * perpage; i < totalSize; i++)
			{
				final GroupTemplate tpl = groups.get(i);
				if (tpl != null)
				{
					block = template;
					groupId = tpl.getGroupId();
					block = block.replace("%name%", player.getLang().equalsIgnoreCase("en") ? tpl.getNameEn() : tpl.getNameRu());
					block = block.replace("%icon%", tpl.getIcon());
					block = block.replace("%bypass%", "_bbscraftGroup_" + tpl.getId() + "_" + tpl.getGroupId() + "_1");
					list += block;
					countt++;
					if (countt == 2)
					{
						list += "<tr></tr>";
						countt = 0;
					}
				}
				counter++;
				
				if (counter >= perpage)
				{
					break;
				}
			}
			list += "</tr></table>";
			
			final int count = (int) Math.ceil((double) totalSize / perpage);
			html = html.replace("%list%", list);
			html = html.replace("%navigation%", Util.getNavigationBlock(count, page, totalSize, perpage, isThereNextPage, "_bbscraftInGroup_" + id + "_%s"));
			separateAndSend(html, player);
		}
		else if ("bbscraftGroup".equals(cmd))
		{
			final int id = Integer.parseInt(st.nextToken());
			final int groupId = Integer.parseInt(st.nextToken());
			final int page = Integer.parseInt(st.nextToken());
			
			String html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/craft/recipes.htm");
			final String template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/craft/recipe-template.htm");
			
			String block = "";
			String list = "";
			
			final List<RecipeTemplate> recipes = getGroupRecipes(id);
			if (recipes == null || recipes.isEmpty())
			{
				onBypassCommand("_bbscraftInGroup_" + groupId + "_1", player);
				return;
			}
			
			final int perpage = RECIPES_PER_PAGE;
			int counter = 0;
			final int totalSize = recipes.size();
			final boolean isThereNextPage = totalSize > perpage;
			
			list += "<table width=500><tr>";
			int countt = 0;
			for (int i = (page - 1) * perpage; i < totalSize; i++)
			{
				final RecipeTemplate tpl = recipes.get(i);
				if (tpl != null)
				{
					block = template;
					
					String descr = player.getLang().equalsIgnoreCase("en") ? tpl.getDescrEn() : tpl.getDescrRu();
					if (descr.length() > 25)
					{
						descr = descr.substring(0, 25) + "..";
					}
					
					boolean found = false;
					if (!tpl.getRewardSkills().isEmpty() && tpl.getRewardItems().isEmpty())
					{
						for (final Skill skill : tpl.getRewardSkills())
						{
							if (skill != null)
							{
								final Skill sk = player.getKnownSkill(skill.getId());
								if (sk != null && sk.getLevel() >= skill.getLevel())
								{
									found = true;
									break;
								}
							}
						}
					}
					
					block = block.replace("%name%", player.getLang().equalsIgnoreCase("en") ? tpl.getNameEn() : tpl.getNameRu());
					block = block.replace("%icon%", tpl.getIcon());
					block = block.replace("%descr%", descr);
					block = block.replace("%chance%", String.valueOf(tpl.getChance()));
					block = block.replace("%bypass%", found ? "" : "_bbscraftInfo_" + tpl.getId() + "_" + id + "_" + groupId + "_" + page + "_1_1");
					list += block;
					countt++;
					if (countt == 2)
					{
						list += "<tr></tr>";
						countt = 0;
					}
				}
				counter++;
				
				if (counter >= perpage)
				{
					break;
				}
			}
			list += "</tr></table>";
			
			final int count = (int) Math.ceil((double) totalSize / perpage);
			html = html.replace("%list%", list);
			html = html.replace("%back%", "_bbscraftInGroup_" + groupId + "_1");
			html = html.replace("%navigation%", Util.getNavigationBlock(count, page, totalSize, perpage, isThereNextPage, "_bbscraftGroup_" + id + "_" + groupId + "_%s"));
			separateAndSend(html, player);
		}
		else if ("bbscraftInfo".equals(cmd))
		{
			final int id = Integer.parseInt(st.nextToken());
			final int groupId = Integer.parseInt(st.nextToken());
			final int inGroupId = Integer.parseInt(st.nextToken());
			final int groupPage = Integer.parseInt(st.nextToken());
			final int page = Integer.parseInt(st.nextToken());
			final int rewardPage = Integer.parseInt(st.nextToken());
			
			final RecipeTemplate tpl = getRecipeTemplate(groupId, id);
			if (tpl != null)
			{
				String html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/craft/info.htm");
				final String request = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/craft/request-template.htm");
				final String reward = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/craft/reward-template.htm");
				final String skillReward = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/craft/reward-skill-template.htm");
				
				String requestBlock = "";
				String rewardBlock = "";
				String requestList = "";
				String rewardList = "";
				
				html = html.replace("%name%", player.getLang().equalsIgnoreCase("en") ? tpl.getNameEn() : tpl.getNameRu());
				html = html.replace("%icon%", tpl.getIcon());
				html = html.replace("%descr%", player.getLang().equalsIgnoreCase("en") ? tpl.getDescrEn() : tpl.getDescrRu());
				html = html.replace("%chance%", String.valueOf(tpl.getChance()));
				html = html.replace("%doubleCraft%", tpl.isCanDouble() ? player.getLang().equalsIgnoreCase("en") ? "<font color=\"0099FF\">Possible Double Craft!</font>" : "<font color=\"0099FF\">Возможен Двойной Крафт!</font>" : "");
				html = html.replace("%isRandomCraft%", tpl.isRandomCraft() ? "<font color=0099FF>Random Craft</font>" : "");
				
				final int perpage = 7;
				int counter = 0;
				final int totalSize = tpl.getRequestItems().size();
				final boolean isThereNextPage = totalSize > perpage;
				
				final Map<Integer, Integer> itemStats = new HashMap<>();
				
				for (int i = (page - 1) * perpage; i < totalSize; i++)
				{
					final ItemTemplate rItem = tpl.getRequestItems().get(i);
					if (rItem != null)
					{
						requestBlock = request;
						requestBlock = requestBlock.replace("%name%", Util.getItemName(player, rItem.getId()));
						requestBlock = requestBlock.replace("%icon%", Util.getItemIcon(rItem.getId()));
						boolean isEnchanted = false;
						if (rItem.getId() == -300)
						{
							if (player.getFame() < rItem.getMinAmount())
							{
								requestBlock = requestBlock.replace("%amount%", "<font color=\"b02e31\">" + rItem.getMinAmount() + "</font>");
							}
							else
							{
								requestBlock = requestBlock.replace("%amount%", "<font color=\"259a30\">" + rItem.getMinAmount() + "</font>");
							}
						}
						else if (rItem.getId() == -200)
						{
							if (player.getClan() == null || player.getClan().getReputationScore() < rItem.getMinAmount())
							{
								requestBlock = requestBlock.replace("%amount%", "<font color=\"b02e31\">" + rItem.getMinAmount() + "</font>");
							}
							else
							{
								requestBlock = requestBlock.replace("%amount%", "<font color=\"259a30\">" + rItem.getMinAmount() + "</font>");
							}
						}
						else if (rItem.getId() == -100)
						{
							if (player.getPcBangPoints() < rItem.getMinAmount())
							{
								requestBlock = requestBlock.replace("%amount%", "<font color=\"b02e31\">" + rItem.getMinAmount() + "</font>");
							}
							else
							{
								requestBlock = requestBlock.replace("%amount%", "<font color=\"259a30\">" + rItem.getMinAmount() + "</font>");
							}
						}
						else
						{
							if (player.getInventory().getItemByItemId(rItem.getId()) == null || !player.getInventory().haveItemsCountNotEquip(rItem.getId(), rItem.getMinAmount()))
							{
								requestBlock = requestBlock.replace("%amount%", "<font color=\"b02e31\">" + rItem.getMinAmount() + "</font>");
							}
							else
							{
								requestBlock = requestBlock.replace("%amount%", "<font color=\"259a30\">" + rItem.getMinAmount() + "</font>");
								isEnchanted = true;
								final ItemInstance item = player.getInventory().getItemByItemId(rItem.getId());
								if (item.getEnchantLevel() > 0 && tpl.isSaveEnchant())
								{
									itemStats.put(Inventory.getPaperdollIndex(item.getItem().getBodyPart()), item.getEnchantLevel());
									requestBlock = requestBlock.replace("%enchant%", "<font color=\"DC143C\">+" + item.getEnchantLevel() + "</font>");
								}
								else
								{
									requestBlock = requestBlock.replace("%enchant%", "");
								}
							}
						}
						
						if (!isEnchanted)
						{
							requestBlock = requestBlock.replace("%enchant%", "");
						}
						requestList += requestBlock;
					}
					counter++;
					
					if (counter >= perpage)
					{
						break;
					}
				}
				
				final int count = (int) Math.ceil((double) totalSize / perpage);
				
				final int perpageReward = 7;
				int counterReward = 0;
				final int totalSizeReward = tpl.getRewardItems().size() + tpl.getRewardSkills().size();
				final boolean isThereNextPageReward = totalSizeReward > perpageReward;
				
				for (int i = (rewardPage - 1) * perpageReward; i < totalSizeReward; i++)
				{
					final int itemSize = tpl.getRewardItems().size() - 1;
					if (i <= itemSize)
					{
						final ItemTemplate rItem = tpl.getRewardItems().get(i);
						if (rItem != null)
						{
							rewardBlock = reward;
							rewardBlock = rewardBlock.replace("%name%", Util.getItemName(player, rItem.getId()));
							rewardBlock = rewardBlock.replace("%icon%", Util.getItemIcon(rItem.getId()));
							rewardBlock = rewardBlock.replace("%chance%", tpl.isRandomCraft() ? "" + (int) rItem.getChance() + "%" : "");
							if (rItem.getMinAmount() != rItem.getMaxAmount())
							{
								rewardBlock = rewardBlock.replace("%amount%", "<font color=\"LEVEL\">" + rItem.getMinAmount() + "</font> - <font color=\"LEVEL\">" + rItem.getMaxAmount() + "</font>");
							}
							else
							{
								rewardBlock = rewardBlock.replace("%amount%", "<font color=\"LEVEL\">" + rItem.getMinAmount() + "</font>");
							}
							
							final Item item = ItemsParser.getInstance().getTemplate(rItem.getId());
							final int slot = Inventory.getPaperdollIndex(item.getBodyPart());
							if (slot >= 0)
							{
								if (itemStats.containsKey(slot) && tpl.isSaveEnchant())
								{
									final int enchantLevel = itemStats.get(slot);
									rewardBlock = rewardBlock.replace("%enchant%", "<font color=\"DC143C\">+" + enchantLevel + "</font>");
								}
								else
								{
									rewardBlock = rewardBlock.replace("%enchant%", "");
								}
							}
							else
							{
								rewardBlock = rewardBlock.replace("%enchant%", "");
							}
							rewardList += rewardBlock;
						}
					}
					else
					{
						final int s = (i - tpl.getRewardItems().size());
						final Skill skill = tpl.getRewardSkills().get(s);
						if (skill != null)
						{
							rewardBlock = skillReward;
							rewardBlock = rewardBlock.replace("%name%", skill.getName(player.getLang()));
							rewardBlock = rewardBlock.replace("%icon%", skill.getIcon());
							rewardBlock = rewardBlock.replace("%level%", String.valueOf(skill.getLevel()));
							rewardBlock = rewardBlock.replace("%descr%", skill.getDescr(player.getLang()));
							rewardList += rewardBlock;
						}
					}
					counterReward++;
					
					if (counterReward >= perpageReward)
					{
						break;
					}
				}
				
				final int countReward = (int) Math.ceil((double) totalSizeReward / perpageReward);
				
				html = html.replace("{requestList}", requestList);
				html = html.replace("{rewardList}", rewardList);
				html = html.replace("{navigation}", Util.getNavigationBlock(count, page, totalSize, perpage, isThereNextPage, "_bbscraftInfo_" + tpl.getId() + "_" + groupId + "_" + inGroupId + "_" + groupPage + "_%s_" + rewardPage + ""));
				html = html.replace("{navigationReward}", Util.getNavigationBlock(countReward, rewardPage, totalSizeReward, perpageReward, isThereNextPageReward, "_bbscraftInfo_" + tpl.getId() + "_" + groupId + "_" + inGroupId + "_" + groupPage + "_" + page + "_%s"));
				html = html.replace("{bypass}", "_bbsdoCraft_" + id + "_" + groupId + "_" + inGroupId + "_" + groupPage + "_" + page + "");
				html = html.replace("{back}", "_bbscraftGroup_" + groupId + "_" + inGroupId + "_" + groupPage + "");
				separateAndSend(html, player);
			}
		}
		else if ("bbsdoCraft".equals(cmd))
		{
			final int id = Integer.parseInt(st.nextToken());
			final int groupId = Integer.parseInt(st.nextToken());
			final int inGroupId = Integer.parseInt(st.nextToken());
			final int groupPage = Integer.parseInt(st.nextToken());
			final int page = Integer.parseInt(st.nextToken());
			
			final RecipeTemplate tpl = getRecipeTemplate(groupId, id);
			if (tpl != null)
			{
				boolean found = false;
				if (!tpl.getRewardSkills().isEmpty() && tpl.getRewardItems().isEmpty())
				{
					for (final Skill skill : tpl.getRewardSkills())
					{
						if (skill != null)
						{
							final Skill sk = player.getKnownSkill(skill.getId());
							if (sk != null && sk.getLevel() >= skill.getLevel())
							{
								found = true;
								break;
							}
						}
					}
				}
				
				if (!checkRequestItems(tpl.getRequestItems(), player) || found)
				{
					onBypassCommand("_bbscraftInfo_" + id + "_" + groupId + "_" + inGroupId + "_" + groupPage + "_" + page + "_1", player);
					return;
				}
				
				final Map<Integer, ItemStatsTemplate> itemStats = new HashMap<>();
				
				final boolean isSucsess = Rnd.chance(tpl.getChance());
				
				for (final ItemTemplate rItem : tpl.getRequestItems())
				{
					if (rItem != null)
					{
						if (!isSucsess && !rItem.isTakeItem())
						{
							continue;
						}
						
						if (rItem.getId() == -300)
						{
							player.setFame((int) (player.getFame() - rItem.getMinAmount()));
							player.sendUserInfo();
						}
						else if (rItem.getId() == -200)
						{
							player.getClan().takeReputationScore((int) rItem.getMinAmount(), true);
							final SystemMessage smsg = SystemMessage.getSystemMessage(SystemMessageId.S1_DEDUCTED_FROM_CLAN_REP);
							smsg.addItemNumber(rItem.getMinAmount());
							player.sendPacket(smsg);
						}
						else if (rItem.getId() == -100)
						{
							player.setPcBangPoints((int) (player.getPcBangPoints() - rItem.getMinAmount()));
							final SystemMessage smsg = SystemMessage.getSystemMessage(SystemMessageId.USING_S1_PCPOINT);
							smsg.addNumber((int) rItem.getMinAmount());
							player.sendPacket(smsg);
							player.sendPacket(new ExPCCafePointInfo(player.getPcBangPoints(), (int) rItem.getMinAmount(), false, false, 1));
						}
						else
						{
							final ItemInstance item = player.getInventory().getItemByItemId(rItem.getId());
							if (item == null)
							{
								player.sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
								return;
							}
							
							if (item.isStackable())
							{
								if (!player.destroyItemByItemId("Craft", rItem.getId(), rItem.getMinAmount(), player, true))
								{
									player.sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
									return;
								}
							}
							else
							{
								final ItemInstance[] inventoryContents = player.getInventory().getAllItemsByItemId(rItem.getId(), false);
								for (int i = 0; i < rItem.getMinAmount(); i++)
								{
									final ItemInstance removeItem = inventoryContents[i];
									if (removeItem.getEnchantLevel() > 0 || removeItem.getElementals() != null || removeItem.isAugmented())
									{
										final ItemStatsTemplate stats = new ItemStatsTemplate();
										if (removeItem.getEnchantLevel() > 0 && tpl.isSaveEnchant())
										{
											stats.enchantLevel = removeItem.getEnchantLevel();
										}
										if (removeItem.getElementals() != null && tpl.isSaveAtt())
										{
											stats.elemental = removeItem.getElementals();
										}
										if (removeItem.isAugmented() && tpl.isSaveAugment())
										{
											stats.augmentation = removeItem.getAugmentation();
										}
										itemStats.put(Inventory.getPaperdollIndex(removeItem.getItem().getBodyPart()), stats);
									}
									
									if (!player.destroyItem("Craft", removeItem.getObjectId(), 1, player, true))
									{
										player.sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
										return;
									}
								}
							}
						}
					}
				}
				
				if (isSucsess)
				{
					if (tpl.isRandomCraft())
					{
						final ItemTemplate rItem = getRandomItem(tpl.getRewardItems());
						if (rItem != null)
						{
							final boolean doubleCraft = tpl.isCanDouble() ? Rnd.chance(tpl.getChance()) : false;
							long amount = 0;
							if (rItem.getMinAmount() != rItem.getMaxAmount())
							{
								amount = Rnd.get(rItem.getMinAmount(), rItem.getMaxAmount());
							}
							else
							{
								amount = rItem.getMinAmount();
							}
							
							if (doubleCraft)
							{
								amount *= 2;
							}
							
							final ItemInstance item = player.addItem("Craft", rItem.getId(), amount, player, true);
							if (!itemStats.isEmpty())
							{
								final int slot = Inventory.getPaperdollIndex(item.getItem().getBodyPart());
								if (slot >= 0)
								{
									if (itemStats.containsKey(slot))
									{
										final ItemStatsTemplate stats = itemStats.get(slot);
										if (stats.enchantLevel > 0 && tpl.isSaveEnchant())
										{
											item.setEnchantLevel(stats.enchantLevel);
										}
										
										if (stats.augmentation != null && tpl.isSaveAugment())
										{
											item.setAugmentation(stats.augmentation);
										}
										
										if (stats.elemental != null && tpl.isSaveAtt())
										{
											for (final Elementals elm : stats.elemental)
											{
												item.setElementAttr(elm.getElement(), elm.getValue());
											}
										}
										item.updateDatabase();
									}
								}
							}
						}
					}
					else
					{
						for (final ItemTemplate rItem : tpl.getRewardItems())
						{
							if (rItem != null)
							{
								final boolean doubleCraft = tpl.isCanDouble() ? Rnd.chance(tpl.getChance()) : false;
								long amount = 0;
								if (rItem.getMinAmount() != rItem.getMaxAmount())
								{
									amount = Rnd.get(rItem.getMinAmount(), rItem.getMaxAmount());
								}
								else
								{
									amount = rItem.getMinAmount();
								}
								
								if (doubleCraft)
								{
									amount *= 2;
								}
								
								final ItemInstance item = player.addItem("Craft", rItem.getId(), amount, player, true);
								if (!itemStats.isEmpty())
								{
									final int slot = Inventory.getPaperdollIndex(item.getItem().getBodyPart());
									if (slot >= 0)
									{
										if (itemStats.containsKey(slot))
										{
											final ItemStatsTemplate stats = itemStats.get(slot);
											if (stats.enchantLevel > 0 && tpl.isSaveEnchant())
											{
												item.setEnchantLevel(stats.enchantLevel);
											}
											
											if (stats.augmentation != null && tpl.isSaveAugment())
											{
												item.setAugmentation(stats.augmentation);
											}
											
											if (stats.elemental != null && tpl.isSaveAtt())
											{
												for (final Elementals elm : stats.elemental)
												{
													item.setElementAttr(elm.getElement(), elm.getValue());
												}
											}
											item.updateDatabase();
										}
									}
								}
							}
						}
					}
					
					if (!tpl.getRewardSkills().isEmpty())
					{
						for (final Skill skill : tpl.getRewardSkills())
						{
							if (skill != null)
							{
								final Skill sk = player.getKnownSkill(skill.getId());
								if (sk == null || (sk != null && sk.getLevel() < skill.getLevel()))
								{
									player.addSkill(skill, true);
								}
							}
						}
						player.sendSkillList(false);
					}
					itemStats.clear();
				}
			}
			onBypassCommand("_bbscraftInfo_" + id + "_" + groupId + "_" + inGroupId + "_" + groupPage + "_" + page + "_1", player);
		}
	}
	
	private boolean checkRequestItems(List<ItemTemplate> requestItems, Player player)
	{
		for (final ItemTemplate rItem : requestItems)
		{
			if (rItem != null)
			{
				if (rItem.getId() == -300)
				{
					if (player.getFame() < rItem.getMinAmount())
					{
						player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
						return false;
					}
				}
				else if (rItem.getId() == -200)
				{
					if (player.getClan() == null || player.getClan().getReputationScore() < rItem.getMinAmount())
					{
						player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
						return false;
					}
				}
				else if (rItem.getId() == -100)
				{
					if (player.getPcBangPoints() < rItem.getMinAmount())
					{
						player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
						return false;
					}
				}
				else
				{
					if (!player.getInventory().haveItemsCountNotEquip(rItem.getId(), rItem.getMinAmount()))
					{
						player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
						return false;
					}
				}
			}
		}
		return true;
	}
	
	public class ItemStatsTemplate
	{
		public Augmentation augmentation = null;
		public Elementals[] elemental = null;
		public int enchantLevel = 0;
	}
	
	public class RecipeTemplate
	{
		private final int _id;
		private final String _icon;
		private final String _nameEn;
		private final String _nameRu;
		private final String _descrEn;
		private final String _descrRu;
		private final int _chance;
		private final List<ItemTemplate> _requestItems;
		private final List<ItemTemplate> _rewardItems;
		private final List<Skill> _rewardSkills;
		private final boolean _canDouble;
		private final int _doubleChance;
		private final boolean _isRandomCraft;
		private final boolean _isSaveEnchant;
		private final boolean _isSaveAugment;
		private final boolean _isSaveAtt;
		
		public RecipeTemplate(int id, String icon, String nameEn, String nameRu, String descrEn, String descrRu, int chance, List<ItemTemplate> requestItems, List<ItemTemplate> rewardItems, List<Skill> rewardSkills, boolean canDouble, int doubleChance, boolean isRandomCraft, boolean isSaveEnchant, boolean isSaveAugment, boolean isSaveAtt)
		{
			_id = id;
			_icon = icon;
			_nameEn = nameEn;
			_nameRu = nameRu;
			_descrEn = descrEn;
			_descrRu = descrRu;
			_chance = chance;
			_requestItems = requestItems;
			_rewardItems = rewardItems;
			_rewardSkills = rewardSkills;
			_canDouble = canDouble;
			_doubleChance = doubleChance;
			_isRandomCraft = isRandomCraft;
			_isSaveEnchant = isSaveEnchant;
			_isSaveAugment = isSaveAugment;
			_isSaveAtt = isSaveAtt;
		}
		
		public int getId()
		{
			return _id;
		}
		
		public String getIcon()
		{
			return _icon;
		}
		
		public String getNameEn()
		{
			return _nameEn;
		}
		
		public String getNameRu()
		{
			return _nameRu;
		}
		
		public String getDescrEn()
		{
			return _descrEn;
		}
		
		public String getDescrRu()
		{
			return _descrRu;
		}
		
		public int getChance()
		{
			return _chance;
		}
		
		public List<ItemTemplate> getRequestItems()
		{
			return _requestItems;
		}
		
		public List<ItemTemplate> getRewardItems()
		{
			return _rewardItems;
		}
		
		public List<Skill> getRewardSkills()
		{
			return _rewardSkills;
		}
		
		public boolean isCanDouble()
		{
			return _canDouble;
		}
		
		public int getDoubleChance()
		{
			return _doubleChance;
		}
		
		public boolean isRandomCraft()
		{
			return _isRandomCraft;
		}
		
		public boolean isSaveEnchant()
		{
			return _isSaveEnchant;
		}
		
		public boolean isSaveAugment()
		{
			return _isSaveAugment;
		}
		
		public boolean isSaveAtt()
		{
			return _isSaveAtt;
		}
	}
	
	public class GroupTemplate
	{
		private final int _id;
		private final String _icon;
		private final String _nameEn;
		private final String _nameRu;
		private final int _groupId;
		private final List<RecipeTemplate> _recipes;
		
		public GroupTemplate(int id, int groupId, String icon, String nameEn, String nameRu, List<RecipeTemplate> recipes)
		{
			_id = id;
			_groupId = groupId;
			_icon = icon;
			_nameEn = nameEn;
			_nameRu = nameRu;
			_recipes = recipes;
		}
		
		public int getId()
		{
			return _id;
		}
		
		public int getGroupId()
		{
			return _groupId;
		}
		
		public String getIcon()
		{
			return _icon;
		}
		
		public String getNameEn()
		{
			return _nameEn;
		}
		
		public String getNameRu()
		{
			return _nameRu;
		}
		
		public List<RecipeTemplate> getRecipes()
		{
			return _recipes;
		}
	}
	
	public class ItemTemplate
	{
		private final int _itemId;
		private final long _min;
		private final long _max;
		private final double _chance;
		private final boolean _takeItem;
		
		public ItemTemplate(int itemId, long min, long max, double chance, boolean takeItem)
		{
			_itemId = itemId;
			_min = min;
			_max = max;
			_chance = chance;
			_takeItem = takeItem;
		}
		
		public int getId()
		{
			return _itemId;
		}
		
		public long getMinAmount()
		{
			return _min;
		}
		
		public long getMaxAmount()
		{
			return _max;
		}
		
		public double getChance()
		{
			return _chance;
		}
		
		public boolean isTakeItem()
		{
			return _takeItem;
		}
	}
	
	private List<GroupTemplate> getGroups(int groupId)
	{
		final List<GroupTemplate> list = new ArrayList<>();
		for (final GroupTemplate tpl : _groups)
		{
			if (tpl.getGroupId() == groupId)
			{
				list.add(tpl);
			}
		}
		return list;
	}
	
	private List<RecipeTemplate> getGroupRecipes(int groupId)
	{
		for (final GroupTemplate tpl : _groups)
		{
			if (tpl.getId() == groupId)
			{
				return tpl.getRecipes();
			}
		}
		return Collections.emptyList();
	}
	
	private RecipeTemplate getRecipeTemplate(int groupId, int id)
	{
		for (final GroupTemplate tpl : _groups)
		{
			if (tpl.getId() == groupId)
			{
				for (final RecipeTemplate template : tpl.getRecipes())
				{
					if (template != null)
					{
						if (template.getId() == id)
						{
							return template;
						}
					}
				}
			}
		}
		return null;
	}
	
	private ItemTemplate getRandomItem(List<ItemTemplate> holders)
	{
		double itemRandom = 100 * Rnd.nextDouble();
		for (final ItemTemplate holder : holders)
		{
			if (!Double.isNaN(holder.getChance()))
			{
				if (holder.getChance() > itemRandom)
				{
					return holder;
				}
				itemRandom -= holder.getChance();
			}
		}
		return null;
	}

	@Override
	public void onWriteCommand(String command, String s, String s1, String s2, String s3, String s4, Player Player)
	{
	}
	
	private static CommunityCraft _instance = new CommunityCraft();
	
	public static CommunityCraft getInstance()
	{
		if (_instance == null)
		{
			_instance = new CommunityCraft();
		}
		return _instance;
	}

	public static void main(String[] args)
	{
		if(CommunityBoardHandler.getInstance().getHandler("_bbscraftMain") == null)
			CommunityBoardHandler.getInstance().registerHandler(new CommunityCraft());
	}
}