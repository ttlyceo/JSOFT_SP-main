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

public class ExEnchantSkillResult extends GameServerPacket
{
	private static final ExEnchantSkillResult STATIC_PACKET_TRUE = new ExEnchantSkillResult(true);
	private static final ExEnchantSkillResult STATIC_PACKET_FALSE = new ExEnchantSkillResult(false);

	public static final ExEnchantSkillResult valueOf(boolean result)
	{
		return result ? STATIC_PACKET_TRUE : STATIC_PACKET_FALSE;
	}

	private final boolean _enchanted;

	public ExEnchantSkillResult(boolean enchanted)
	{
		_enchanted = enchanted;
	}

	@Override
	protected void writeImpl()
	{
		writeD(_enchanted ? 0x01 : 0x00);
	}
}