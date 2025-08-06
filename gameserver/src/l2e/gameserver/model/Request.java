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
package l2e.gameserver.model;

import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.clientpackets.GameClientPacket;
import l2e.gameserver.network.serverpackets.SystemMessage;

public class Request
{
	private static final int REQUEST_TIMEOUT = 15;

	protected Player _player;
	protected Player _partner;
	protected boolean _isRequestor;
	protected boolean _isAnswerer;
	protected GameClientPacket _requestPacket;
	
	public Request(Player player)
	{
		_player = player;
	}

	protected void clear()
	{
		_partner = null;
		_requestPacket = null;
		_isRequestor = false;
		_isAnswerer = false;
	}

	private synchronized void setPartner(Player partner)
	{
		_partner = partner;
	}

	public Player getPartner()
	{
		return _partner;
	}

	private synchronized void setRequestPacket(GameClientPacket packet)
	{
		_requestPacket = packet;
	}

	public GameClientPacket getRequestPacket()
	{
		return _requestPacket;
	}

	public synchronized boolean setRequest(Player partner, GameClientPacket packet)
	{
		if (partner == null)
		{
			_player.sendPacket(SystemMessageId.YOU_HAVE_INVITED_THE_WRONG_TARGET);
			return false;
		}
		if (partner.getRequest().isProcessingRequest())
		{
			SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_IS_BUSY_TRY_LATER);
			sm.addString(partner.getName(null));
			_player.sendPacket(sm);
			sm = null;
			return false;
		}
		if (isProcessingRequest())
		{
			_player.sendPacket(SystemMessageId.WAITING_FOR_ANOTHER_REPLY);
			return false;
		}
		_partner = partner;
		_requestPacket = packet;
		setOnRequestTimer(true);
		_partner.getRequest().setPartner(_player);
		_partner.getRequest().setRequestPacket(packet);
		_partner.getRequest().setOnRequestTimer(false);
		return true;
	}

	private void setOnRequestTimer(boolean isRequestor)
	{
		_isRequestor = isRequestor ? true : false;
		_isAnswerer = isRequestor ? false : true;
		ThreadPoolManager.getInstance().schedule(new Runnable()
		{
			@Override
			public void run()
			{
				clear();
			}
		}, REQUEST_TIMEOUT * 1000);
	}

	public void onRequestResponse()
	{
		if (_partner != null)
		{
			_partner.getRequest().clear();
		}
		clear();
	}

	public boolean isProcessingRequest()
	{
		return _partner != null;
	}
}