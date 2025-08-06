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
package l2e.fake.ai;

import java.util.List;

import l2e.commons.util.Rnd;
import l2e.fake.FakePlayer;
import l2e.gameserver.Config;
import l2e.gameserver.data.parser.FakeArmorParser;
import l2e.gameserver.data.parser.ItemsParser;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.items.Item;
import l2e.gameserver.model.actor.templates.items.Weapon;
import l2e.gameserver.model.items.PcItemTemplate;
import l2e.gameserver.model.items.instance.ItemInstance;

public class EnchanterAI extends FakePlayerAI
{
	private int _enchantIterations = 0;
	private final int _maxEnchant = Config.ENCHANTERS_MAX_LVL;
	private final int iterationsForAction = Rnd.get(3, 5);

	public EnchanterAI(FakePlayer character)
	{
		super(character, false);
	}

	@Override
	public void setup()
	{
		super.setup();
		ItemInstance weapon = _fakePlayer.getActiveWeaponInstance();
		weapon = checkIfWeaponIsExistsEquipped(weapon);
		weapon.setEnchantLevel(0);
		_fakePlayer.broadcastCharInfo();
	}
	
	@Override
	public void thinkAndAct()
	{
		
		handleDeath();
		setBusyThinking(true);
		if (_enchantIterations % iterationsForAction == 0)
		{
			ItemInstance weapon = _fakePlayer.getActiveWeaponInstance();
			weapon = checkIfWeaponIsExistsEquipped(weapon);
			final double chance = getSuccessChance(weapon);
			
			final int currentEnchantLevel = weapon.getEnchantLevel();
			if (currentEnchantLevel < _maxEnchant || serverHasUnlimitedMax())
			{
				if (Rnd.nextDouble() < chance || weapon.getEnchantLevel() < 4)
				{
					weapon.setEnchantLevel(currentEnchantLevel + 1);
					_fakePlayer.broadcastCharInfo();
				}
				else
				{
					destroyFailedItem(weapon);
				}
			}
		}
		_enchantIterations++;
		setBusyThinking(false);
	}

	private void destroyFailedItem(ItemInstance weapon)
	{
		_fakePlayer.getInventory().destroyItem("Enchant", weapon, _fakePlayer, null);
		_fakePlayer.broadcastCharInfo();
		_fakePlayer.setActiveEnchantItemId(Player.ID_NONE);
	}

	private double getSuccessChance(ItemInstance weapon)
	{
		double chance = 0d;
		if (((Weapon) weapon.getItem()).isMagicWeapon())
		{
			chance = (weapon.getEnchantLevel() > 14) ? 60 : 70;
		}
		else
		{
			chance = (weapon.getEnchantLevel() > 14) ? 65 : 75;
		}
		return chance;
	}

	private boolean serverHasUnlimitedMax()
	{
		return _maxEnchant == 0;
	}

	private ItemInstance checkIfWeaponIsExistsEquipped(ItemInstance weapon)
	{
		if (weapon == null)
		{
			final ItemInstance newItem = checkRndItems(_fakePlayer);
			if (newItem != null)
			{
				weapon = newItem;
			}
		}
		return weapon;
	}
	
	private ItemInstance checkRndItems(FakePlayer player)
	{
		ItemInstance weapon = null;
		List<PcItemTemplate> items = null;
		
		if (player.getLevel() < 20)
		{
			items = FakeArmorParser.getInstance().getNgGradeList(player.getClassId());
		}
		else if (player.getLevel() > 19 && player.getLevel() < 40)
		{
			items = FakeArmorParser.getInstance().getDGradeList(player.getClassId());
		}
		else if (player.getLevel() > 40 && player.getLevel() < 52)
		{
			items = FakeArmorParser.getInstance().getCGradeList(player.getClassId());
		}
		else if (player.getLevel() > 52 && player.getLevel() < 61)
		{
			items = FakeArmorParser.getInstance().getBGradeList(player.getClassId());
		}
		else if (player.getLevel() > 60 && player.getLevel() < 76)
		{
			items = FakeArmorParser.getInstance().getAGradeList(player.getClassId());
		}
		else if (player.getLevel() > 75 && player.getLevel() < 80)
		{
			items = FakeArmorParser.getInstance().getSGradeList(player.getClassId());
		}
		else
		{
			if (Rnd.get(100) <= 50)
			{
				items = FakeArmorParser.getInstance().getS80GradeList(player.getClassId());
			}
			else
			{
				items = FakeArmorParser.getInstance().getS84GradeList(player.getClassId());
			}
		}
		
		if (items != null)
		{
			for (final PcItemTemplate ie : items)
			{
				if (ie != null)
				{
					final Item item = ItemsParser.getInstance().getTemplate(ie.getId());
					if (item != null && item.isWeapon())
					{
						final ItemInstance it = player.getInventory().addItem("Items", ie.getId(), ie.getCount(), player, null);
						player.getInventory().equipItem(it);
						weapon = it;
					}
				}
			}
		}
		return weapon;
	}
	
	@Override
	protected int[][] getBuffs()
	{
		return new int[0][0];
	}
}
