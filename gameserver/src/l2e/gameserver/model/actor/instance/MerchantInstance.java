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

import l2e.gameserver.data.parser.BuyListParser;
import l2e.gameserver.data.parser.MerchantPriceParser;
import l2e.gameserver.data.parser.MerchantPriceParser.MerchantPrice;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.items.buylist.ProductList;
import l2e.gameserver.network.serverpackets.BuyList;
import l2e.gameserver.network.serverpackets.ExBuySellList;

public class MerchantInstance extends NpcInstance
{
	private MerchantPrice _mpc;
	
	public MerchantInstance(int objectId, NpcTemplate template)
	{
		super(objectId, template);
		setInstanceType(InstanceType.MerchantInstance);
	}

	@Override
	public void onSpawn()
	{
		super.onSpawn();
		_mpc = MerchantPriceParser.getInstance().getMerchantPrice(this);
	}

	@Override
	public String getHtmlPath(int npcId, int val)
	{
		String pom = "";
		
		if (val == 0)
		{
			pom = "" + npcId;
		}
		else
		{
			pom = npcId + "-" + val;
		}
		
		return "data/html/merchant/" + pom + ".htm";
	}
	
	public MerchantPrice getMpc()
	{
		return _mpc;
	}
	
	public final void showBuyWindow(Player player, int val)
	{
		showBuyWindow(player, val, true);
	}
	
	public final void showBuyWindow(Player player, int val, boolean applyTax)
	{
		final ProductList buyList = BuyListParser.getInstance().getBuyList(val);
		if (buyList == null)
		{
			_log.warn("BuyList not found! BuyListId:" + val);
			player.sendActionFailed();
			return;
		}
		
		if (!buyList.isNpcAllowed(getId()))
		{
			_log.warn("Npc not allowed in BuyList! BuyListId:" + val + " NpcId:" + getId());
			player.sendActionFailed();
			return;
		}
		
		final double taxRate = (applyTax) ? getMpc().getTotalTaxRate() : 0;
		
		player.setInventoryBlockingStatus(true);
		player.sendPacket(new BuyList(buyList, player.getAdena(), taxRate));
		player.sendPacket(new ExBuySellList(player, false));
		player.sendActionFailed();
		if (player.isGM())
		{
			player.sendMessage("BuyList: " + val + ".xml " + "Tax: " + taxRate + "%");
		}
	}
}