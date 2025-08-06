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
package l2e.gameserver.handler.effecthandlers.impl;

import l2e.gameserver.Config;
import l2e.gameserver.model.skills.effects.Effect;
import l2e.gameserver.model.skills.effects.EffectTemplate;
import l2e.gameserver.model.skills.effects.EffectType;
import l2e.gameserver.model.stats.Env;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.ExPCCafePointInfo;
import l2e.gameserver.network.serverpackets.PetItemList;
import l2e.gameserver.network.serverpackets.SystemMessage;

public class Restoration extends Effect
{
	private final int _itemId;
	private long _itemCount;
	private final int _enchantLevel;

	public Restoration(Env env, EffectTemplate template)
	{
		super(env, template);
		_itemId = template.getParameters().getInteger("itemId", 0);
		_itemCount = template.getParameters().getLong("itemCount", 0L);
		_enchantLevel = template.getParameters().getInteger("enchantLevel", 0);
	}

	@Override
	public EffectType getEffectType()
	{
		return EffectType.NONE;
	}

	@Override
	public boolean onStart()
	{
		if ((getEffected() == null) || !getEffected().isPlayable())
		{
			return false;
		}

		if (_itemCount <= 0)
		{
			getEffected().sendPacket(SystemMessageId.NOTHING_INSIDE_THAT);
			_log.warn(Restoration.class.getSimpleName() + " effect with wrong item Id/count: " + _itemId + "/" + _itemCount + "!");
			return false;
		}

		if (getEffected().isPlayer())
		{
			final var activeChar = getEffected().getActingPlayer();
			switch (_itemId)
			{
				case -100 :
					
					if (activeChar.getPcBangPoints() < Config.MAX_PC_BANG_POINTS)
					{
						if ((activeChar.getPcBangPoints() + _itemCount) > Config.MAX_PC_BANG_POINTS)
						{
							_itemCount = Config.MAX_PC_BANG_POINTS - activeChar.getPcBangPoints();
						}
						
						activeChar.setPcBangPoints((int) (activeChar.getPcBangPoints() + _itemCount));
						final var smsg = SystemMessage.getSystemMessage(SystemMessageId.YOU_HAVE_ACQUIRED_S1_PC_CAFE_POINTS);
						smsg.addNumber((int) _itemCount);
						activeChar.sendPacket(smsg);
						activeChar.sendPacket(new ExPCCafePointInfo(activeChar.getPcBangPoints(), (int) _itemCount, false, false, 1));
					}
					break;
				case -200 :
					if (activeChar.getClan() != null)
					{
						activeChar.getClan().addReputationScore((int) _itemCount, true);
					}
					break;
				case -300 :
					activeChar.setFame((int) (activeChar.getFame() + _itemCount));
					activeChar.sendUserInfo();
					break;
				case -1 :
					activeChar.setGamePoints((activeChar.getGamePoints() + _itemCount));
					activeChar.sendMessage("Your game points count changed to " + activeChar.getGamePoints());
					break;
				default :
					final var newItem = activeChar.addItem("Extract", _itemId, _itemCount, activeChar, true);
					if (newItem != null && _enchantLevel > 0)
					{
						newItem.setEnchantLevel(_enchantLevel);
						newItem.updateDatabase();
					}
					break;
			}
		}
		else if (getEffected().isPet())
		{
			if (_itemId <= 0)
			{
				getEffected().sendPacket(SystemMessageId.NOTHING_INSIDE_THAT);
				_log.warn(Restoration.class.getSimpleName() + " effect with wrong item Id/count: " + _itemId + "/" + _itemCount + "!");
				return false;
			}
			final var newItem = getEffected().getInventory().addItem("Skill", _itemId, _itemCount, getEffected().getActingPlayer(), getEffector());
			if (newItem != null && _enchantLevel > 0)
			{
				newItem.setEnchantLevel(_enchantLevel);
				newItem.updateDatabase();
			}
			getEffected().getActingPlayer().sendPacket(new PetItemList(getEffected().getInventory().getItems()));
		}
		return true;
	}
}