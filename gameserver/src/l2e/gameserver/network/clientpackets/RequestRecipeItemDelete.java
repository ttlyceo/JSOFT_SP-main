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

import l2e.gameserver.data.parser.RecipeParser;
import l2e.gameserver.model.RecipeList;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.network.serverpackets.RecipeBookItemList;

public final class RequestRecipeItemDelete extends GameClientPacket
{
	private int _recipeID;

	@Override
	protected void readImpl()
	{
		_recipeID = readD();
	}

	@Override
	protected void runImpl()
	{
		final Player activeChar = getClient().getActiveChar();
		if (activeChar == null)
		{
			return;
		}

		final RecipeList rp = RecipeParser.getInstance().getRecipeList(_recipeID);
		if (rp == null)
		{
			return;
		}
		activeChar.unregisterRecipeList(_recipeID);

		final RecipeBookItemList response = new RecipeBookItemList(rp.isDwarvenRecipe(), (int) activeChar.getMaxMp());
		if (rp.isDwarvenRecipe())
		{
			response.addRecipes(activeChar.getDwarvenRecipeBook());
		}
		else
		{
			response.addRecipes(activeChar.getCommonRecipeBook());
		}

		activeChar.sendPacket(response);
	}
}