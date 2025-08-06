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

import l2e.commons.util.Rnd;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.MonsterInstance;
import l2e.gameserver.model.holders.ItemHolder;
import l2e.gameserver.model.skills.effects.Effect;
import l2e.gameserver.model.skills.effects.EffectTemplate;
import l2e.gameserver.model.skills.effects.EffectType;
import l2e.gameserver.model.stats.Env;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.SystemMessage;

public class Harvesting extends Effect
{
	public Harvesting(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public boolean onStart()
	{
		if ((getEffector() == null) || (getEffected() == null) || !getEffector().isPlayer() || !getEffected().isNpc() || !getEffected().isDead())
		{
			return false;
		}
		
		final Player player = getEffector().getActingPlayer();
		final GameObject[] targets = getSkill().getTargetList(player, false, getEffected());
		if ((targets == null) || (targets.length == 0))
		{
			return false;
		}
		
		MonsterInstance monster;
		for (final GameObject target : targets)
		{
			if ((target == null) || !target.isMonster())
			{
				continue;
			}
			
			monster = (MonsterInstance) target;
			
			if (player.getObjectId() != monster.getSeederId())
			{
				final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_HARVEST);
				player.sendPacket(sm);
				continue;
			}
			
			if (monster.isSeeded())
			{
				if (calcSuccess(player, monster))
				{
					final ItemHolder[] items = monster.takeHarvest();
					if ((items != null) && (items.length > 0))
					{
						for (final ItemHolder reward : items)
						{
							if (reward == null)
							{
								continue;
							}
							
							player.getInventory().addItem("Harvesting", reward.getId(), reward.getCount(), player, monster);
							
							SystemMessage smsg = null;
							if (reward.getCount() > 1)
							{
								smsg = SystemMessage.getSystemMessage(SystemMessageId.YOU_PICKED_UP_S1_S2);
								smsg.addItemName(reward.getId());
								smsg.addLong(reward.getCount());
							}
							else
							{
								smsg = SystemMessage.getSystemMessage(SystemMessageId.YOU_PICKED_UP_S1);
								smsg.addItemName(reward.getId());
							}
							
							player.sendPacket(smsg);
							player.getCounters().addAchivementInfo("takeHarvests", 0, reward.getCount(), false, false, false);
							
							if (player.isInParty())
							{
								if (reward.getCount() > 1)
								{
									smsg = SystemMessage.getSystemMessage(SystemMessageId.C1_HARVESTED_S3_S2S);
									smsg.addString(player.getName(null));
									smsg.addLong(reward.getCount());
									smsg.addItemName(reward.getId());
								}
								else
								{
									smsg = SystemMessage.getSystemMessage(SystemMessageId.C1_HARVESTED_S2S);
									smsg.addString(player.getName(null));
									smsg.addItemName(reward.getId());
								}
								player.getParty().broadcastToPartyMembers(player, smsg);
							}
						}
						return true;
					}
				}
				else
				{
					player.sendPacket(SystemMessageId.THE_HARVEST_HAS_FAILED);
				}
			}
			else
			{
				player.sendPacket(SystemMessageId.THE_HARVEST_FAILED_BECAUSE_THE_SEED_WAS_NOT_SOWN);
			}
		}
		return false;
	}
	
	private boolean calcSuccess(Player activeChar, MonsterInstance target)
	{
		int basicSuccess = 100;
		final int levelPlayer = activeChar.getLevel();
		final int levelTarget = target.getLevel();
		
		int diff = (levelPlayer - levelTarget);
		if (diff < 0)
		{
			diff = -diff;
		}
		
		if (diff > 5)
		{
			basicSuccess -= (diff - 5) * 5;
		}
		
		if (basicSuccess < 1)
		{
			basicSuccess = 1;
		}
		return Rnd.nextInt(99) < basicSuccess;
	}
	
	@Override
	public EffectType getEffectType()
	{
		return EffectType.HARVESTING;
	}
}