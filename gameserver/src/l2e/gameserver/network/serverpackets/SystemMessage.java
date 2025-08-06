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
package l2e.gameserver.network.serverpackets;

import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.components.AbstractMessage;

public final class SystemMessage extends AbstractMessage<SystemMessage>
{
	private SystemMessage(final SystemMessageId smId)
	{
		super(smId);
	}

	public static SystemMessage obtainItems(final ItemInstance item) {
		return obtainItems(item.getId(), item.getCount(), item.isEquipable() ? item.getEnchantLevel() : 0);
	}

	public static SystemMessage obtainItems(final int itemId, final long count, final int enchantLevel) {
		if (itemId == 57) {
			return new SystemMessage(SystemMessageId.EARNED_S1_ADENA).addLong(count);
		}
		if (count > 1L) {
			return new SystemMessage(SystemMessageId.EARNED_S2_S1_S).addItemName(itemId).addLong(count);
		}
		if (enchantLevel > 0) {
			return new SystemMessage(SystemMessageId.YOU_PICKED_UP_A_S1_S2).addNumber(enchantLevel).addItemName(itemId);
		}
		return new SystemMessage(SystemMessageId.EARNED_ITEM_S1).addItemName(itemId);
	}

	public static final SystemMessage sendString(final String text)
	{
		if (text == null)
		{
			throw new NullPointerException();
		}
		
		final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1);
		sm.addString(text);
		return sm;
	}
	
	public static final SystemMessage getSystemMessage(final SystemMessageId smId)
	{
		SystemMessage sm = smId.getStaticSystemMessage();
		if (sm != null)
		{
			return sm;
		}
		
		sm = new SystemMessage(smId);
		if (smId.getParamCount() == 0)
		{
			smId.setStaticSystemMessage(sm);
		}
		
		return sm;
	}
	
	public static SystemMessage getSystemMessage(int id)
	{
		return getSystemMessage(SystemMessageId.getSystemMessageId(id));
	}
	
	private SystemMessage(final int id)
	{
		this(SystemMessageId.getSystemMessageId(id));
	}
	
	@Override
	protected final void writeImpl()
	{
		writeInfo();
	}
}