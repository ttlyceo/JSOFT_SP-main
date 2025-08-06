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

import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.instancemanager.FortSiegeManager;
import l2e.gameserver.instancemanager.MercTicketManager;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.actor.instance.PetInstance;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.network.SystemMessageId;

public final class RequestPetGetItem extends GameClientPacket
{
	private int _objectId;
	
	@Override
	protected void readImpl()
	{
		_objectId = readD();
	}
	
	@Override
	protected void runImpl()
	{
		final ItemInstance item = GameObjectsStorage.getItem(_objectId);
		if ((item == null) || (getActiveChar() == null) || !getActiveChar().hasPet())
		{
			sendActionFailed();
			return;
		}
		
		final int castleId = MercTicketManager.getInstance().getTicketCastleId(item.getId());
		if (castleId > 0)
		{
			sendActionFailed();
			return;
		}
		
		if (FortSiegeManager.getInstance().isCombat(item.getId()))
		{
			sendActionFailed();
			return;
		}
		
		final PetInstance pet = (PetInstance) getClient().getActiveChar().getSummon();
		if (pet.isDead() || pet.isOutOfControl() || pet.isActionsDisabled())
		{
			sendActionFailed();
			return;
		}
		
		if (pet.isUncontrollable())
		{
			sendPacket(SystemMessageId.WHEN_YOUR_PET_S_HUNGER_GAUGE_IS_AT_0_YOU_CANNOT_USE_YOUR_PET);
			return;
		}
		pet.getAI().setIntention(CtrlIntention.PICK_UP, item);
	}
}