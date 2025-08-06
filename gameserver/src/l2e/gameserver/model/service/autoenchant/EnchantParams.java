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
package l2e.gameserver.model.service.autoenchant;

import l2e.gameserver.Config;
import l2e.gameserver.model.items.instance.ItemInstance;

public class EnchantParams
{
    public ItemInstance targetItem;
    public ItemInstance upgradeItem;
    public boolean isUseCommonScrollWhenSafe = true;
	public int upgradeItemLimit = Config.enchantServiceDefaultLimit;
	public int maxEnchant = Config.enchantServiceDefaultEnchant;
	public int maxEnchantAtt = Config.enchantServiceDefaultAttribute;
    public boolean isChangingUpgradeItemLimit = false;
    public boolean isChangingMaxEnchant = false;

    public long lastEnchant;
    public long lastAbuse;
}
