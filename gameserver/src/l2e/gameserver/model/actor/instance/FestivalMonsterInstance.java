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

import l2e.gameserver.SevenSignsFestival;
import l2e.gameserver.model.Party;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.network.serverpackets.InventoryUpdate;

public class FestivalMonsterInstance extends MonsterInstance
{
	protected int _bonusMultiplier = 1;
	
	public FestivalMonsterInstance(int objectId, NpcTemplate template)
	{
		super(objectId, template);
		setInstanceType(InstanceType.FestivalMonsterInstance);
	}

	public void setOfferingBonus(int bonusMultiplier)
	{
		_bonusMultiplier = bonusMultiplier;
	}
	
	@Override
	public boolean isAutoAttackable(Creature attacker, boolean isPoleAttack)
	{
		if (attacker instanceof FestivalMonsterInstance)
		{
			return false;
		}
		return true;
	}
	
	@Override
	public boolean isAggressive()
	{
		return true;
	}
	
	@Override
	public boolean hasRandomAnimation()
	{
		return false;
	}
	
	@Override
	public void doItemDrop(Creature lastAttacker, Creature mainDamageDealer)
	{
		Player killingChar = null;
		
		if (!(lastAttacker.isPlayer()))
		{
			return;
		}
		
		killingChar = (Player) lastAttacker;
		final Party associatedParty = killingChar.getParty();
		
		if (associatedParty == null || associatedParty.getMemberCount() < 2)
		{
			return;
		}
		
		final Player partyLeader = associatedParty.getLeader();
		final ItemInstance addedOfferings = partyLeader.getInventory().addItem("Sign", SevenSignsFestival.FESTIVAL_OFFERING_ID, _bonusMultiplier, partyLeader, this);
		
		final InventoryUpdate iu = new InventoryUpdate();
		
		if (addedOfferings.getCount() != _bonusMultiplier)
		{
			iu.addModifiedItem(addedOfferings);
		}
		else
		{
			iu.addNewItem(addedOfferings);
		}
		
		partyLeader.sendPacket(iu);
		
		super.doItemDrop(lastAttacker, mainDamageDealer);
	}
}