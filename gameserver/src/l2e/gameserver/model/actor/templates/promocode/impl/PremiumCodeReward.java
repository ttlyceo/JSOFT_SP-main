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
package l2e.gameserver.model.actor.templates.promocode.impl;

import org.w3c.dom.NamedNodeMap;

import l2e.gameserver.Config;
import l2e.gameserver.data.dao.CharacterPremiumDAO;
import l2e.gameserver.data.parser.PremiumAccountsParser;
import l2e.gameserver.listener.player.OnAnswerListener;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.service.premium.PremiumTemplate;
import l2e.gameserver.model.strings.server.ServerMessage;

public class PremiumCodeReward extends AbstractCodeReward
{
	private final int _premiumId;
	private final String _icon;
	
	public PremiumCodeReward(NamedNodeMap attr)
	{
		_premiumId = Integer.parseInt(attr.getNamedItem("id").getNodeValue());
		_icon = attr.getNamedItem("icon") != null ? attr.getNamedItem("icon").getNodeValue() : "";
	}
	
	@Override
	public void giveReward(Player player)
	{
		final PremiumTemplate template = PremiumAccountsParser.getInstance().getPremiumTemplate(_premiumId);
		if (template != null)
		{
			if (!player.hasPremiumBonus())
			{
				getGivePremium(template, player);
			}
			else
			{
				if (Config.PREMIUMSERVICE_DOUBLE)
				{
					player.sendConfirmDlg(new PremiumAnswerListener(player, template), 60000, new ServerMessage("PromoCode.WANT_CHANGE_PREMIUM", player.getLang()).toString());
				}
			}
		}
	}
	
	private class PremiumAnswerListener implements OnAnswerListener
	{
		private final Player _player;
		private final PremiumTemplate _template;
		
		protected PremiumAnswerListener(Player player, PremiumTemplate template)
		{
			_player = player;
			_template = template;
		}
		
		@Override
		public void sayYes()
		{
			getGivePremium(_template, _player);
		}
		
		@Override
		public void sayNo()
		{
		}
	}
	
	@Override
	public String getIcon()
	{
		return _icon;
	}
	
	private void getGivePremium(PremiumTemplate template, Player player)
	{
		if (Config.PREMIUMSERVICE_DOUBLE && player.hasPremiumBonus())
		{
			player.removePremiumAccount(true);
		}
		
		final long time = !template.isOnlineType() ? (System.currentTimeMillis() + (template.getTime() * 1000)) : 0;
		if (template.isPersonal())
		{
			CharacterPremiumDAO.getInstance().updatePersonal(player, _premiumId, time, template.isOnlineType());
		}
		else
		{
			CharacterPremiumDAO.getInstance().update(player, _premiumId, time, template.isOnlineType());
		}
		
		if (player.isInParty())
		{
			player.getParty().recalculatePartyData();
		}
	}
	
	public int getPremiumId()
	{
		return _premiumId;
	}
}