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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import l2e.commons.util.Rnd;
import l2e.gameserver.Config;
import l2e.gameserver.data.parser.ItemsParser;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.ExtractableProductItemTemplate;
import l2e.gameserver.model.actor.templates.ExtractableSkillTemplate;
import l2e.gameserver.model.actor.templates.items.Item;
import l2e.gameserver.model.holders.ItemHolder;
import l2e.gameserver.model.skills.effects.Effect;
import l2e.gameserver.model.skills.effects.EffectTemplate;
import l2e.gameserver.model.skills.effects.EffectType;
import l2e.gameserver.model.stats.Env;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.ExPCCafePointInfo;
import l2e.gameserver.network.serverpackets.SystemMessage;

public class RestorationRandom extends Effect
{
	public RestorationRandom(Env env, EffectTemplate template)
	{
		super(env, template);
	}
	
	@Override
	public boolean onStart()
	{
		if ((getEffector() == null) || (getEffected() == null) || !getEffector().isPlayer() || !getEffected().isPlayer())
		{
			return false;
		}
		final Player player = getEffected().getActingPlayer();
		final ExtractableSkillTemplate exSkill = getSkill().getExtractableSkill();
		if (exSkill == null)
		{
			return false;
		}

		final var items = exSkill.getProductItems();
		if (items.isEmpty())
		{
			_log.warn("Extractable Skill with no data, probably wrong/empty table in Skill Id: " + getSkill().getId());
			return false;
		}
		Collections.shuffle(items);
		
		final List<ItemHolder> creationList = new ArrayList<>();
		var chanceFull = 0.D;
		boolean doneAfterOneItem = false;
		for (final ExtractableProductItemTemplate expi : items)
		{
			chanceFull += expi.getChance();
		}
		final boolean mustGive = Math.ceil(chanceFull) >= 100 ? true : false;
		
		for (final ExtractableProductItemTemplate expi : items)
		{
			if (Rnd.chance(expi.getChance()) && !doneAfterOneItem)
			{
				creationList.addAll(expi.getItems());
				if (mustGive)
				{
					doneAfterOneItem = true;
				}
				break;
			}
		}
		
		if (mustGive && !doneAfterOneItem)
		{
			loopGive(player, creationList, exSkill);
		}

		if (creationList.isEmpty())
		{
			player.sendPacket(SystemMessageId.NOTHING_INSIDE_THAT);
			return false;
		}

		for (final ItemHolder item : creationList)
		{
			if (item.getCount() <= 0)
			{
				continue;
			}
			
			var amount = item.getCount();
			if (amount != item.getCountMax())
			{
				amount = Rnd.get(amount, item.getCountMax());
			}
			amount *= Config.RATE_EXTRACTABLE;
			
			switch (item.getId())
			{
				case -100 :
					if (player.getPcBangPoints() < Config.MAX_PC_BANG_POINTS)
					{
						if ((player.getPcBangPoints() + amount) > Config.MAX_PC_BANG_POINTS)
						{
							amount = Config.MAX_PC_BANG_POINTS - player.getPcBangPoints();
						}
						
						player.setPcBangPoints((int) (player.getPcBangPoints() + amount));
						final var smsg = SystemMessage.getSystemMessage(SystemMessageId.YOU_HAVE_ACQUIRED_S1_PC_CAFE_POINTS);
						smsg.addNumber((int) amount);
						player.sendPacket(smsg);
						player.sendPacket(new ExPCCafePointInfo(player.getPcBangPoints(), (int) amount, false, false, 1));
					}
					break;
				case -200 :
					if (player.getClan() != null)
					{
						player.getClan().addReputationScore((int) amount, true);
					}
					break;
				case -300 :
					player.setFame((int) (player.getFame() + amount));
					player.sendUserInfo();
					break;
				case -1 :
					player.setGamePoints(player.getGamePoints() + amount);
					player.sendMessage("Your game points count changed to " + player.getGamePoints());
					break;
				default :
					final Item template = ItemsParser.getInstance().getTemplate(item.getId());
					if (template != null)
					{
						if (template.isStackable())
						{
							player.addItem("Extract", item.getId(), amount, player, true);
						}
						else
						{
							final var count = amount;
							for (int it = 1; it <= count; it++)
							{
								final var newItem = player.addItem("Extract", item.getId(), 1, player, true);
								if (newItem != null && item.getEnchantLevel() > 0)
								{
									newItem.setEnchantLevel(item.getEnchantLevel());
									newItem.updateDatabase();
								}
							}
						}
					}
					break;
			}
		}
		return true;
	}
	
	private static void loopGive(Player player, List<ItemHolder> creationList, ExtractableSkillTemplate exSkill)
	{
		boolean doneAfterOneItem = false;
		for (final ExtractableProductItemTemplate expi : exSkill.getProductItems())
		{
			if (Rnd.chance(expi.getChance()) && !doneAfterOneItem)
			{
				creationList.addAll(expi.getItems());
				doneAfterOneItem = true;
				break;
			}
		}
		
		if (!doneAfterOneItem)
		{
			loopGive(player, creationList, exSkill);
		}
	}

	@Override
	public EffectType getEffectType()
	{
		return EffectType.RESTORATION_RANDOM;
	}
}