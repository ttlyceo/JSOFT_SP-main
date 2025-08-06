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
package l2e.gameserver.network.clientpackets;

import l2e.commons.util.Util;
import l2e.gameserver.RecipeController;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.actor.Player;

public final class RequestRecipeShopMakeDo extends GameClientPacket
{
	private int _id;
	private int _recipeId;
	protected long _unknow;
	
	@Override
	protected void readImpl()
	{
		_id = readD();
		_recipeId = readD();
		_unknow = readQ();
	}

	@Override
	protected void runImpl()
	{
		final Player activeChar = getClient().getActiveChar();
		if (activeChar == null)
		{
			return;
		}

		if (activeChar.isMovementDisabled())
		{
			activeChar.sendActionFailed();
			return;
		}

		final Player manufacturer = GameObjectsStorage.getPlayer(_id);
		if (manufacturer == null)
		{
			return;
		}

		if ((manufacturer.getReflectionId() != activeChar.getReflectionId()) && (activeChar.getReflectionId() != -1))
		{
			return;
		}

		if (activeChar.getPrivateStoreType() != Player.STORE_PRIVATE_NONE)
		{
			activeChar.sendMessage("You cannot create items while trading.");
			return;
		}
		if (manufacturer.getPrivateStoreType() != Player.STORE_PRIVATE_MANUFACTURE)
		{
			return;
		}

		if (activeChar.isInCraftMode() || manufacturer.isInCraftMode())
		{
			activeChar.sendMessage("You are currently in Craft Mode.");
			return;
		}
		if (Util.checkIfInRange(150, activeChar, manufacturer, true))
		{
			RecipeController.getInstance().requestManufactureItem(manufacturer, _recipeId, activeChar);
		}
	}
}