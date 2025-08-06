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
package l2e.gameserver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2e.commons.util.Rnd;
import l2e.commons.util.Util;
import l2e.gameserver.data.parser.ItemsParser;
import l2e.gameserver.data.parser.RecipeParser;
import l2e.gameserver.model.RecipeList;
import l2e.gameserver.model.TempItem;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.RecipeStatTemplate;
import l2e.gameserver.model.actor.templates.RecipeTemplate;
import l2e.gameserver.model.actor.templates.items.Item;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.stats.Stats;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.MagicSkillUse;
import l2e.gameserver.network.serverpackets.RecipeBookItemList;
import l2e.gameserver.network.serverpackets.RecipeItemMakeInfo;
import l2e.gameserver.network.serverpackets.RecipeShopItemInfo;
import l2e.gameserver.network.serverpackets.SetupGauge;
import l2e.gameserver.network.serverpackets.StatusUpdate;
import l2e.gameserver.network.serverpackets.SystemMessage;

public class RecipeController
{
	protected static final Map<Integer, RecipeItemMaker> _activeMakers = new ConcurrentHashMap<>();

	protected RecipeController()
	{
	}
	
	public void requestBookOpen(Player player, boolean isDwarvenCraft)
	{
		if (!_activeMakers.containsKey(player.getObjectId()))
		{
			final RecipeBookItemList response = new RecipeBookItemList(isDwarvenCraft, (int) player.getMaxMp());
			response.addRecipes(isDwarvenCraft ? player.getDwarvenRecipeBook() : player.getCommonRecipeBook());
			player.sendPacket(response);
			return;
		}
		player.sendPacket(SystemMessageId.CANT_ALTER_RECIPEBOOK_WHILE_CRAFTING);
	}

	public void requestMakeItemAbort(Player player)
	{
		_activeMakers.remove(player.getObjectId());
	}

	public void requestManufactureItem(Player manufacturer, int recipeListId, Player player)
	{
		final var recipeList = RecipeParser.getInstance().getValidRecipeList(player, recipeListId);
		if (recipeList == null)
		{
			return;
		}

		final List<RecipeList> dwarfRecipes = Arrays.asList(manufacturer.getDwarvenRecipeBook());
		final List<RecipeList> commonRecipes = Arrays.asList(manufacturer.getCommonRecipeBook());

		if (!dwarfRecipes.contains(recipeList) && !commonRecipes.contains(recipeList))
		{
			Util.handleIllegalPlayerAction(player, "" + player.getName(null) + " of account " + player.getAccountName() + " sent a false recipe id.");
			return;
		}

		if (Config.ALT_GAME_CREATION && _activeMakers.containsKey(manufacturer.getObjectId()))
		{
			player.sendPacket(SystemMessageId.CLOSE_STORE_WINDOW_AND_TRY_AGAIN);
			return;
		}

		final var maker = new RecipeItemMaker(manufacturer, recipeList, player);
		if (maker._isValid)
		{
			if (Config.ALT_GAME_CREATION)
			{
				_activeMakers.put(manufacturer.getObjectId(), maker);
				ThreadPoolManager.getInstance().schedule(maker, 100);
			}
			else
			{
				maker.run();
			}
		}
	}

	public void requestMakeItem(Player player, int recipeListId)
	{
		if (player.isInDuel())
		{
			player.sendPacket(SystemMessageId.CANT_OPERATE_PRIVATE_STORE_DURING_COMBAT);
			return;
		}

		final var recipeList = RecipeParser.getInstance().getValidRecipeList(player, recipeListId);
		if (recipeList == null)
		{
			return;
		}

		final List<RecipeList> dwarfRecipes = Arrays.asList(player.getDwarvenRecipeBook());
		final List<RecipeList> commonRecipes = Arrays.asList(player.getCommonRecipeBook());

		if (!dwarfRecipes.contains(recipeList) && !commonRecipes.contains(recipeList))
		{
			Util.handleIllegalPlayerAction(player, "" + player.getName(null) + " of account " + player.getAccountName() + " sent a false recipe id.");
			return;
		}

		if (Config.ALT_GAME_CREATION && _activeMakers.containsKey(player.getObjectId()))
		{
			final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S2_S1);
			sm.addItemName(recipeList.getItemId());
			sm.addString("You are busy creating.");
			player.sendPacket(sm);
			return;
		}

		final var maker = new RecipeItemMaker(player, recipeList, player);
		if (maker._isValid)
		{
			if (Config.ALT_GAME_CREATION)
			{
				_activeMakers.put(player.getObjectId(), maker);
				ThreadPoolManager.getInstance().schedule(maker, 100);
			}
			else
			{
				maker.run();
			}
		}
	}

	private static class RecipeItemMaker implements Runnable
	{
		private static final Logger _log = LoggerFactory.getLogger(RecipeItemMaker.class);
		protected boolean _isValid;
		protected List<TempItem> _items = null;
		protected final RecipeList _recipeList;
		protected final Player _player;
		protected final Player _target;
		protected final Skill _skill;
		protected final int _skillId;
		protected final int _skillLevel;
		protected int _creationPasses = 1;
		protected int _itemGrab;
		protected int _exp = -1;
		protected int _sp = -1;
		protected long _price;
		protected int _totalItems;
		protected int _delay;

		public RecipeItemMaker(Player pPlayer, RecipeList pRecipeList, Player pTarget)
		{
			_player = pPlayer;
			_target = pTarget;
			_recipeList = pRecipeList;

			_isValid = false;
			_skillId = _recipeList.isDwarvenRecipe() ? Skill.SKILL_CREATE_DWARVEN : Skill.SKILL_CREATE_COMMON;
			_skillLevel = _player.getSkillLevel(_skillId);
			_skill = _player.getKnownSkill(_skillId);

			_player.isInCraftMode(true);

			if (_player.isAlikeDead())
			{
				_player.sendActionFailed();
				abort();
				return;
			}

			if (_target.isAlikeDead())
			{
				_target.sendActionFailed();
				abort();
				return;
			}

			if (_target.isProcessingTransaction())
			{
				_target.sendActionFailed();
				abort();
				return;
			}

			if (_player.isProcessingTransaction())
			{
				_player.sendActionFailed();
				abort();
				return;
			}

			if (_recipeList.getRecipes().length == 0)
			{
				_player.sendActionFailed();
				abort();
				return;
			}

			if (_recipeList.getLevel() > _skillLevel)
			{
				_player.sendActionFailed();
				abort();
				return;
			}

			if (_player != _target)
			{
				final var item = _player.getManufactureItems().get(_recipeList.getId());
				if (item != null)
				{
					_price = item.getCost();
					if (_target.getAdena() < _price)
					{
						_target.sendPacket(SystemMessageId.YOU_NOT_ENOUGH_ADENA);
						abort();
						return;
					}
				}
			}

			if ((_items = listItems(false)) == null)
			{
				abort();
				return;
			}

			for (final TempItem i : _items)
			{
				_totalItems += i.getQuantity();
			}

			if (!calculateStatUse(false, false))
			{
				abort();
				return;
			}

			if (Config.ALT_GAME_CREATION)
			{
				calculateAltStatChange();
			}
			updateMakeInfo(true);
			updateCurMp();
			updateCurLoad();

			_player.isInCraftMode(false);
			_isValid = true;
		}

		@Override
		public void run()
		{
			if (!Config.IS_CRAFTING_ENABLED)
			{
				_target.sendMessage("Item creation is currently disabled.");
				abort();
				return;
			}

			if ((_player == null) || (_target == null))
			{
				_log.warn("player or target == null (disconnected?), aborting" + _target + _player);
				abort();
				return;
			}

			if (!_player.isOnline() || !_target.isOnline())
			{
				_log.warn("player or target is not online, aborting " + _target + _player);
				abort();
				return;
			}

			if (Config.ALT_GAME_CREATION && !_activeMakers.containsKey(_player.getObjectId()))
			{
				if (_target != _player)
				{
					_target.sendMessage("Manufacture aborted");
					_player.sendMessage("Manufacture aborted");
				}
				else
				{
					_player.sendMessage("Item creation aborted");
				}

				abort();
				return;
			}

			if (Config.ALT_GAME_CREATION && !_items.isEmpty())
			{
				
				if (!calculateStatUse(true, true))
				{
					return;
				}
				updateCurMp();

				grabSomeItems();

				if (!_items.isEmpty())
				{
					_delay = (int) ((Config.ALT_GAME_CREATION_SPEED * _player.getMReuseRate(_skill) * GameTimeController.TICKS_PER_SECOND) / Config.RATE_CONSUMABLE_COST) * GameTimeController.MILLIS_IN_TICK;

					final MagicSkillUse msk = new MagicSkillUse(_player, _skillId, _skillLevel, _delay, 0);
					_player.broadcastPacket(msk);

					_player.sendPacket(new SetupGauge(_player, 0, _delay));
					ThreadPoolManager.getInstance().schedule(this, 100 + _delay);
				}
				else
				{
					_player.sendPacket(new SetupGauge(_player, 0, _delay));

					try
					{
						Thread.sleep(_delay);
					}
					catch (final InterruptedException e)
					{}
					finally
					{
						finishCrafting();
					}
				}
			}
			else
			{
				finishCrafting();
			}
		}

		private void finishCrafting()
		{
			if (!Config.ALT_GAME_CREATION)
			{
				calculateStatUse(false, true);
			}

			if ((_target != _player) && (_price > 0))
			{
				final var adenatransfer = _target.transferItem("PayManufacture", _target.getInventory().getAdenaInstance().getObjectId(), _price, _player.getInventory(), _player);
				if (adenatransfer == null)
				{
					_target.sendPacket(SystemMessageId.YOU_NOT_ENOUGH_ADENA);
					abort();
					return;
				}
			}
			
			if ((_items = listItems(true)) == null)
			{
			}
			else
			{
				int tryCount = 1;
				var canDouble = false;
				final var item = ItemsParser.getInstance().getTemplate(_recipeList.getItemId());
				if (item != null && !item.isKeyMatherial() && !item.isRecipe())
				{
					canDouble = true;
				}
				
				if (Rnd.chance(Config.CRAFT_DOUBLECRAFT_CHANCE) && canDouble)
				{
					tryCount++;
				}
				
				final var chance = _recipeList.getSuccessRate() + _player.getPremiumBonus().getCraftChance();
				for (int i = 0; i < tryCount; i++)
				{
					if (chance >= 100)
					{
						rewardPlayer();
						updateMakeInfo(true);
					}
					else if (Rnd.chance(chance))
					{
						rewardPlayer();
						updateMakeInfo(true);
						_target.getCounters().addAchivementInfo("recipesSucceeded", 0, -1, false, false, false);
					}
					else
					{
						if (_target != _player)
						{
							SystemMessage msg = SystemMessage.getSystemMessage(SystemMessageId.CREATION_OF_S2_FOR_C1_AT_S3_ADENA_FAILED);
							msg.addString(_target.getName(null));
							msg.addItemName(_recipeList.getItemId());
							msg.addItemNumber(_price);
							_player.sendPacket(msg);
							
							msg = SystemMessage.getSystemMessage(SystemMessageId.C1_FAILED_TO_CREATE_S2_FOR_S3_ADENA);
							msg.addString(_player.getName(null));
							msg.addItemName(_recipeList.getItemId());
							msg.addItemNumber(_price);
							_target.sendPacket(msg);
						}
						else
						{
							_target.sendPacket(SystemMessageId.ITEM_MIXING_FAILED);
							_target.getCounters().addAchivementInfo("recipesFailed", 0, -1, false, false, false);
						}
						updateMakeInfo(false);
					}
				}
			}
			updateCurMp();
			updateCurLoad();
			_activeMakers.remove(_player.getObjectId());
			_player.isInCraftMode(false);
			_target.sendItemList(false);
		}

		private void updateMakeInfo(boolean success)
		{
			if (_target == _player)
			{
				_target.sendPacket(new RecipeItemMakeInfo(_recipeList.getId(), _target, success));
			}
			else
			{
				_target.sendPacket(new RecipeShopItemInfo(_player, _recipeList.getId()));
			}
		}

		private void updateCurLoad()
		{
			_target.sendStatusUpdate(false, false, StatusUpdate.CUR_LOAD);
		}

		private void updateCurMp()
		{
			_target.sendStatusUpdate(false, false, StatusUpdate.CUR_MP);
		}

		private void grabSomeItems()
		{
			int grabItems = _itemGrab;
			while ((grabItems > 0) && !_items.isEmpty())
			{
				final TempItem item = _items.get(0);

				int count = item.getQuantity();
				if (count >= grabItems)
				{
					count = grabItems;
				}

				item.setQuantity(item.getQuantity() - count);
				if (item.getQuantity() <= 0)
				{
					_items.remove(0);
				}
				else
				{
					_items.set(0, item);
				}

				grabItems -= count;

				if (_target == _player)
				{
					final var sm = SystemMessage.getSystemMessage(SystemMessageId.S1_S2_EQUIPPED);
					sm.addItemNumber(count);
					sm.addItemName(item.getId());
					_player.sendPacket(sm);
				}
				else
				{
					_target.sendMessage("Manufacturer " + _player.getName(null) + " used " + count + " " + item.getItem().getName(_target.getLang()));
				}
			}
		}

		private void calculateAltStatChange()
		{
			_itemGrab = _skillLevel;

			for (final var altStatChange : _recipeList.getAltStatChange())
			{
				if (altStatChange.getType() == RecipeStatTemplate.StatType.XP)
				{
					_exp = altStatChange.getValue();
				}
				else if (altStatChange.getType() == RecipeStatTemplate.StatType.SP)
				{
					_sp = altStatChange.getValue();
				}
				else if (altStatChange.getType() == RecipeStatTemplate.StatType.GIM)
				{
					_itemGrab *= altStatChange.getValue();
				}
			}
			_creationPasses = (_totalItems / _itemGrab) + ((_totalItems % _itemGrab) != 0 ? 1 : 0);
			if (_creationPasses < 1)
			{
				_creationPasses = 1;
			}
		}

		private boolean calculateStatUse(boolean isWait, boolean isReduce)
		{
			var ret = true;
			for (final var statUse : _recipeList.getStatUse())
			{
				final double modifiedValue = statUse.getValue() / _creationPasses;
				if (statUse.getType() == RecipeStatTemplate.StatType.HP)
				{
					if (_player.getCurrentHp() <= modifiedValue)
					{
						if (Config.ALT_GAME_CREATION && isWait)
						{
							_player.sendPacket(new SetupGauge(_player, 0, _delay));
							ThreadPoolManager.getInstance().schedule(this, 100 + _delay);
						}
						else
						{
							_target.sendPacket(SystemMessageId.NOT_ENOUGH_HP);
							abort();
						}
						ret = false;
					}
					else if (isReduce)
					{
						_player.reduceCurrentHp(modifiedValue, _player, _skill);
					}
				}
				else if (statUse.getType() == RecipeStatTemplate.StatType.MP)
				{
					if (_player.getCurrentMp() < modifiedValue)
					{
						if (Config.ALT_GAME_CREATION && isWait)
						{
							_player.sendPacket(new SetupGauge(_player, 0, _delay));
							ThreadPoolManager.getInstance().schedule(this, 100 + _delay);
						}
						else
						{
							_target.sendPacket(SystemMessageId.NOT_ENOUGH_MP);
							abort();
						}
						ret = false;
					}
					else if (isReduce)
					{
						_player.reduceCurrentMp(modifiedValue);
					}
				}
				else
				{
					_target.sendMessage("Recipe error!!!, please tell this to your GM.");
					ret = false;
					abort();
				}
			}
			return ret;
		}

		private List<TempItem> listItems(boolean remove)
		{
			final RecipeTemplate[] recipes = _recipeList.getRecipes();
			final var inv = _target.getInventory();
			final List<TempItem> materials = new ArrayList<>();
			SystemMessage sm;

			for (final var recipe : recipes)
			{
				if (recipe.getQuantity() > 0)
				{
					final var item = inv.getItemByItemId(recipe.getId());
					final var itemQuantityAmount = item == null ? 0L : item.getCount();

					if (itemQuantityAmount < recipe.getQuantity())
					{
						sm = SystemMessage.getSystemMessage(SystemMessageId.MISSING_S2_S1_TO_CREATE);
						sm.addItemName(recipe.getId());
						sm.addItemNumber(recipe.getQuantity() - itemQuantityAmount);
						_target.sendPacket(sm);

						abort();
						return null;
					}
					final TempItem temp = new TempItem(item, recipe.getQuantity());
					materials.add(temp);
				}
			}

			if (remove)
			{
				for (final var tmp : materials)
				{
					inv.destroyItemByItemId("Manufacture", tmp.getId(), tmp.getQuantity(), _target, _player);

					if (tmp.getQuantity() > 1)
					{
						sm = SystemMessage.getSystemMessage(SystemMessageId.S2_S1_DISAPPEARED);
						sm.addItemName(tmp.getId());
						sm.addItemNumber(tmp.getQuantity());
						_target.sendPacket(sm);
					}
					else
					{
						sm = SystemMessage.getSystemMessage(SystemMessageId.S1_DISAPPEARED);
						sm.addItemName(tmp.getId());
						_target.sendPacket(sm);
					}
				}
			}
			return materials;
		}

		private void abort()
		{
			updateMakeInfo(false);
			_player.isInCraftMode(false);
			_activeMakers.remove(_player.getObjectId());
		}

		private void rewardPlayer()
		{
			final var rareProdId = _recipeList.getRareItemId();
			var itemId = _recipeList.getItemId();
			var itemCount = _recipeList.getCount();
			final Item template = ItemsParser.getInstance().getTemplate(itemId);
			
			if (rareProdId > 0 && (rareProdId == itemId || Config.CRAFT_MASTERWORK))
			{
				final int chance = _recipeList.getRarity() + _player.getPremiumBonus().getMasterWorkChance();
				if (chance >= 100)
				{
					itemId = rareProdId;
					itemCount = _recipeList.getRareCount();
				}
				else if (Rnd.chance(chance))
				{
					itemId = rareProdId;
					itemCount = _recipeList.getRareCount();
					_target.getCounters().addAchivementInfo("rareCreated", 0, -1, false, false, false);
				}
			}

			_target.getInventory().addItem("Manufacture", itemId, itemCount, _target, _player);

			SystemMessage sm = null;
			if (_target != _player)
			{
				if (itemCount == 1)
				{
					sm = SystemMessage.getSystemMessage(SystemMessageId.S2_CREATED_FOR_C1_FOR_S3_ADENA);
					sm.addString(_target.getName(null));
					sm.addItemName(itemId);
					sm.addItemNumber(_price);
					_player.sendPacket(sm);

					sm = SystemMessage.getSystemMessage(SystemMessageId.C1_CREATED_S2_FOR_S3_ADENA);
					sm.addString(_player.getName(null));
					sm.addItemName(itemId);
					sm.addItemNumber(_price);
					_target.sendPacket(sm);
				}
				else
				{
					sm = SystemMessage.getSystemMessage(SystemMessageId.S2_S3_S_CREATED_FOR_C1_FOR_S4_ADENA);
					sm.addString(_target.getName(null));
					sm.addNumber(itemCount);
					sm.addItemName(itemId);
					sm.addItemNumber(_price);
					_player.sendPacket(sm);

					sm = SystemMessage.getSystemMessage(SystemMessageId.C1_CREATED_S2_S3_S_FOR_S4_ADENA);
					sm.addString(_player.getName(null));
					sm.addNumber(itemCount);
					sm.addItemName(itemId);
					sm.addItemNumber(_price);
					_target.sendPacket(sm);
				}
			}

			if (itemCount > 1)
			{
				sm = SystemMessage.getSystemMessage(SystemMessageId.EARNED_S2_S1_S);
				sm.addItemName(itemId);
				sm.addItemNumber(itemCount);
				_target.sendPacket(sm);
			}
			else
			{
				sm = SystemMessage.getSystemMessage(SystemMessageId.EARNED_ITEM_S1);
				sm.addItemName(itemId);
				_target.sendPacket(sm);
			}

			if (Config.ALT_GAME_CREATION)
			{
				final var recipeLevel = _recipeList.getLevel();
				if (_exp < 0)
				{
					_exp = template.getReferencePrice() * itemCount;
					_exp /= recipeLevel;
				}
				if (_sp < 0)
				{
					_sp = _exp / 10;
				}
				if (itemId == rareProdId)
				{
					_exp *= Config.ALT_GAME_CREATION_RARE_XPSP_RATE;
					_sp *= Config.ALT_GAME_CREATION_RARE_XPSP_RATE;
				}

				if (_exp < 0)
				{
					_exp = 0;
				}
				if (_sp < 0)
				{
					_sp = 0;
				}

				for (int i = _skillLevel; i > recipeLevel; i--)
				{
					_exp /= 4;
					_sp /= 4;
				}
				_player.addExpAndSp((int) _player.calcStat(Stats.EXPSP_RATE, _exp * Config.ALT_GAME_CREATION_XP_RATE * Config.ALT_GAME_CREATION_SPEED, null, null), (int) _player.calcStat(Stats.EXPSP_RATE, _sp * Config.ALT_GAME_CREATION_SP_RATE * Config.ALT_GAME_CREATION_SPEED, null, null));
			}
			updateMakeInfo(true);
		}
	}

	public static RecipeController getInstance()
	{
		return SingletonHolder._instance;
	}

	private static class SingletonHolder
	{
		protected static final RecipeController _instance = new RecipeController();
	}
}