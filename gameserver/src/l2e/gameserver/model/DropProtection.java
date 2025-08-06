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

import l2e.gameserver.Config;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.PetInstance;

public class DropProtection
{
	private int _ownerId = 0;
	private long _protectTime = 0;

	public boolean isProtected()
	{
		return _protectTime > System.currentTimeMillis();
	}
	
	public int getOwnerId()
	{
		return _ownerId;
	}
	
	public synchronized boolean tryPickUp(Player actor)
	{
		if (_ownerId == 0 || _ownerId == actor.getObjectId())
		{
			return true;
		}
		
		final var owner = GameObjectsStorage.getPlayer(_ownerId);
		if (owner != null && owner.getParty() != null && owner.getParty() == actor.getParty())
		{
			return true;
		}
		return _protectTime < System.currentTimeMillis();
	}
	
	public boolean tryPickUp(PetInstance pet)
	{
		return tryPickUp(pet.getOwner());
	}
	
	public synchronized void unprotect()
	{
		_ownerId = 0;
		_protectTime = 0L;
	}
	
	public synchronized void protect(Player player, boolean isRaid)
	{
		unprotect();
		final long protectTime = (isRaid ? Config.RAID_DROP_PROTECTION : Config.NPC_DROP_PROTECTION) * 1000L;
		if (protectTime <= 0 || player == null)
		{
			return;
		}
		_ownerId = player.getObjectId();
		_protectTime = System.currentTimeMillis() + protectTime;
	}
}