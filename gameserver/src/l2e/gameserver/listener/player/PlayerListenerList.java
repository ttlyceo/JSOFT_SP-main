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
package l2e.gameserver.listener.player;

import l2e.commons.listener.Listener;
import l2e.gameserver.listener.actor.CharListenerList;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.Summon;
import l2e.gameserver.model.entity.Siege;
import l2e.gameserver.model.holders.ItemHolder;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.olympiad.CompetitionType;

public class PlayerListenerList extends CharListenerList
{
	public PlayerListenerList(Player actor)
	{
		super(actor);
	}
	
	@Override
	public Player getActor()
	{
		return (Player) _actor;
	}
	
	public void onEnter()
	{
		if (!_global.getListeners().isEmpty())
		{
			for (final Listener<Creature> listener : _global.getListeners())
			{
				if (OnPlayerEnterListener.class.isInstance(listener))
				{
					((OnPlayerEnterListener) listener).onPlayerEnter(getActor());
				}
			}
		}
		
		if (!getListeners().isEmpty())
		{
			for (final Listener<Creature> listener : getListeners())
			{
				if (OnPlayerEnterListener.class.isInstance(listener))
				{
					((OnPlayerEnterListener) listener).onPlayerEnter(getActor());
				}
			}
		}
	}
	
	public void onExit()
	{
		if (!_global.getListeners().isEmpty())
		{
			for (final Listener<Creature> listener : _global.getListeners())
			{
				if (OnPlayerExitListener.class.isInstance(listener))
				{
					((OnPlayerExitListener) listener).onPlayerExit(getActor());
				}
			}
		}
		
		if (!getListeners().isEmpty())
		{
			for (final Listener<Creature> listener : getListeners())
			{
				if (OnPlayerExitListener.class.isInstance(listener))
				{
					((OnPlayerExitListener) listener).onPlayerExit(getActor());
				}
			}
		}
	}
	
	public void onTeleport(int x, int y, int z)
	{
		if (!_global.getListeners().isEmpty())
		{
			for (final Listener<Creature> listener : _global.getListeners())
			{
				if (OnTeleportListener.class.isInstance(listener))
				{
					((OnTeleportListener) listener).onTeleport(getActor(), x, y, z);
				}
			}
		}
		
		if (!getListeners().isEmpty())
		{
			for (final Listener<Creature> listener : getListeners())
			{
				if (OnTeleportListener.class.isInstance(listener))
				{
					((OnTeleportListener) listener).onTeleport(getActor(), x, y, z);
				}
			}
		}
	}
	
	public void onPartyInvite()
	{
		if (!_global.getListeners().isEmpty())
		{
			for (final Listener<Creature> listener : _global.getListeners())
			{
				if (OnPlayerPartyInviteListener.class.isInstance(listener))
				{
					((OnPlayerPartyInviteListener) listener).onPartyInvite(getActor());
				}
			}
		}
		
		if (!getListeners().isEmpty())
		{
			for (final Listener<Creature> listener : getListeners())
			{
				if (OnPlayerPartyInviteListener.class.isInstance(listener))
				{
					((OnPlayerPartyInviteListener) listener).onPartyInvite(getActor());
				}
			}
		}
	}
	
	public void onLevelChange(int oldLvl, int newLvl)
	{
		if (!_global.getListeners().isEmpty())
		{
			for (final Listener<Creature> listener : _global.getListeners())
			{
				if (OnLevelChangeListener.class.isInstance(listener))
				{
					((OnLevelChangeListener) listener).onLevelChange(getActor(), oldLvl, newLvl);
				}
			}
		}
		
		if (!getListeners().isEmpty())
		{
			for (final Listener<Creature> listener : getListeners())
			{
				if (OnLevelChangeListener.class.isInstance(listener))
				{
					((OnLevelChangeListener) listener).onLevelChange(getActor(), oldLvl, newLvl);
				}
			}
		}
	}
	
	public void onPartyLeave()
	{
		if (!_global.getListeners().isEmpty())
		{
			for (final Listener<Creature> listener : _global.getListeners())
			{
				if (OnPlayerPartyLeaveListener.class.isInstance(listener))
				{
					((OnPlayerPartyLeaveListener) listener).onPartyLeave(getActor());
				}
			}
		}
		
		if (!getListeners().isEmpty())
		{
			for (final Listener<Creature> listener : getListeners())
			{
				if (OnPlayerPartyLeaveListener.class.isInstance(listener))
				{
					((OnPlayerPartyLeaveListener) listener).onPartyLeave(getActor());
				}
			}
		}
	}
	
	public void onSummonServitor(Summon summon)
	{
		if (!_global.getListeners().isEmpty())
		{
			for (final Listener<Creature> listener : _global.getListeners())
			{
				if (OnPlayerSummonServitorListener.class.isInstance(listener))
				{
					((OnPlayerSummonServitorListener) listener).onSummonServitor(getActor(), summon);
				}
			}
		}
		
		if (!getListeners().isEmpty())
		{
			for (final Listener<Creature> listener : getListeners())
			{
				if (OnPlayerSummonServitorListener.class.isInstance(listener))
				{
					((OnPlayerSummonServitorListener) listener).onSummonServitor(getActor(), summon);
				}
			}
		}
	}
	
	public void onChatMessageReceive(int type, String charName, String targetName, String text)
	{
		if (!_global.getListeners().isEmpty())
		{
			for (final Listener<Creature> listener : _global.getListeners())
			{
				if (OnPlayerChatMessageReceive.class.isInstance(listener))
				{
					((OnPlayerChatMessageReceive) listener).onChatMessageReceive(getActor(), type, charName, targetName, text);
				}
			}
		}
		
		if (!getListeners().isEmpty())
		{
			for (final Listener<Creature> listener : getListeners())
			{
				if (OnPlayerChatMessageReceive.class.isInstance(listener))
				{
					((OnPlayerChatMessageReceive) listener).onChatMessageReceive(getActor(), type, charName, targetName, text);
				}
			}
		}
	}
	
	public void onExperienceReceived(long exp)
	{
		if (!_global.getListeners().isEmpty())
		{
			for (final Listener<Creature> listener : _global.getListeners())
			{
				if (OnExperienceReceivedListener.class.isInstance(listener))
				{
					((OnExperienceReceivedListener) listener).onExperienceReceived(getActor(), exp);
				}
			}
		}
		
		if (!getListeners().isEmpty())
		{
			for (final Listener<Creature> listener : getListeners())
			{
				if (OnExperienceReceivedListener.class.isInstance(listener))
				{
					((OnExperienceReceivedListener) listener).onExperienceReceived(getActor(), exp);
				}
			}
		}
	}

	public void onQuestionMarkClicked(int questionMarkId)
	{
		if (!_global.getListeners().isEmpty())
		{
			for (final Listener<Creature> listener : _global.getListeners())
			{
				if (OnQuestionMarkListener.class.isInstance(listener))
				{
					((OnQuestionMarkListener) listener).onQuestionMarkClicked(getActor(), questionMarkId);
				}
			}
		}
		
		if (!getListeners().isEmpty())
		{
			for (final Listener<Creature> listener : getListeners())
			{
				if (OnQuestionMarkListener.class.isInstance(listener))
				{
					((OnQuestionMarkListener) listener).onQuestionMarkClicked(getActor(), questionMarkId);
				}
			}
		}
	}
	
	public void onFishing(ItemHolder item, boolean success)
	{
		if (!_global.getListeners().isEmpty())
		{
			for (final Listener<Creature> listener : _global.getListeners())
			{
				if (OnFishingListener.class.isInstance(listener))
				{
					((OnFishingListener) listener).onFishing(getActor(), item, success);
				}
			}
		}
		
		if (!getListeners().isEmpty())
		{
			for (final Listener<Creature> listener : getListeners())
			{
				if (OnFishingListener.class.isInstance(listener))
				{
					((OnFishingListener) listener).onFishing(getActor(), item, success);
				}
			}
		}
	}
	
	public void onOlympiadFinishBattle(CompetitionType type, boolean winner)
	{
		if (!_global.getListeners().isEmpty())
		{
			for (final Listener<Creature> listener : _global.getListeners())
			{
				if (OnOlympiadFinishBattleListener.class.isInstance(listener))
				{
					((OnOlympiadFinishBattleListener) listener).onOlympiadFinishBattle(getActor(), type, winner);
				}
			}
		}
		
		if (!getListeners().isEmpty())
		{
			for (final Listener<Creature> listener : getListeners())
			{
				if (OnOlympiadFinishBattleListener.class.isInstance(listener))
				{
					((OnOlympiadFinishBattleListener) listener).onOlympiadFinishBattle(getActor(), type, winner);
				}
			}
		}
	}
	
	public void onQuestFinish(int questId)
	{
		if (!_global.getListeners().isEmpty())
		{
			for (final Listener<Creature> listener : _global.getListeners())
			{
				if (OnQuestFinishListener.class.isInstance(listener))
				{
					((OnQuestFinishListener) listener).onQuestFinish(getActor(), questId);
				}
			}
		}
		
		if (!getListeners().isEmpty())
		{
			for (final Listener<Creature> listener : getListeners())
			{
				if (OnQuestFinishListener.class.isInstance(listener))
				{
					((OnQuestFinishListener) listener).onQuestFinish(getActor(), questId);
				}
			}
		}
	}
	
	public void onParticipateInCastleSiege(Siege siege)
	{
		if (!_global.getListeners().isEmpty())
		{
			for (final Listener<Creature> listener : _global.getListeners())
			{
				if (OnParticipateInCastleSiegeListener.class.isInstance(listener))
				{
					((OnParticipateInCastleSiegeListener) listener).onParticipateInCastleSiege(getActor(), siege);
				}
			}
		}
		
		if (!getListeners().isEmpty())
		{
			for (final Listener<Creature> listener : getListeners())
			{
				if (OnParticipateInCastleSiegeListener.class.isInstance(listener))
				{
					((OnParticipateInCastleSiegeListener) listener).onParticipateInCastleSiege(getActor(), siege);
				}
			}
		}
	}
	
	public long onItemDropListener(int itemId, long amount)
	{
		if (!_global.getListeners().isEmpty())
		{
			for (final Listener<Creature> listener : _global.getListeners())
			{
				if (OnItemDropListener.class.isInstance(listener))
				{
					return ((OnItemDropListener) listener).onItemDropListener(getActor(), itemId, amount);
				}
			}
		}
		
		if (!getListeners().isEmpty())
		{
			for (final Listener<Creature> listener : getListeners())
			{
				if (OnItemDropListener.class.isInstance(listener))
				{
					return ((OnItemDropListener) listener).onItemDropListener(getActor(), itemId, amount);
				}
			}
		}
		return amount;
	}
	
	public void onItemEquipListener(ItemInstance item)
	{
		if (!_global.getListeners().isEmpty())
		{
			for (final Listener<Creature> listener : _global.getListeners())
			{
				if (OnItemEquipListener.class.isInstance(listener))
				{
					((OnItemEquipListener) listener).onItemEquipListener(getActor(), item);
				}
			}
		}
		
		if (!getListeners().isEmpty())
		{
			for (final Listener<Creature> listener : getListeners())
			{
				if (OnItemEquipListener.class.isInstance(listener))
				{
					((OnItemEquipListener) listener).onItemEquipListener(getActor(), item);
				}
			}
		}
	}
	
	public void onItemUnEquipListener(ItemInstance item)
	{
		if (!_global.getListeners().isEmpty())
		{
			for (final Listener<Creature> listener : _global.getListeners())
			{
				if (OnItemUnEquipListener.class.isInstance(listener))
				{
					((OnItemUnEquipListener) listener).onItemUnEquipListener(getActor(), item);
				}
			}
		}
		
		if (!getListeners().isEmpty())
		{
			for (final Listener<Creature> listener : getListeners())
			{
				if (OnItemUnEquipListener.class.isInstance(listener))
				{
					((OnItemUnEquipListener) listener).onItemUnEquipListener(getActor(), item);
				}
			}
		}
	}
	
	public void onUseItem(final ItemInstance item)
	{
		if (!_global.getListeners().isEmpty())
		{
			for (final Listener<Creature> listener : _global.getListeners())
			{
				if (OnUseItemListener.class.isInstance(listener))
				{
					((OnUseItemListener) listener).onUseItem(getActor(), item);
				}
			}
		}
		
		if (!getListeners().isEmpty())
		{
			for (final Listener<Creature> listener : getListeners())
			{
				if (OnUseItemListener.class.isInstance(listener))
				{
					((OnUseItemListener) listener).onUseItem(getActor(), item);
				}
			}
		}
	}
	
	public void onEnchantItem(final ItemInstance item, int oldEnchant, int newEnchant, final boolean success, boolean destroy)
	{
		if (!_global.getListeners().isEmpty())
		{
			for (final Listener<Creature> listener : _global.getListeners())
			{
				if (OnEnchantItemListener.class.isInstance(listener))
				{
					((OnEnchantItemListener) listener).onEnchantItem(getActor(), item, oldEnchant, newEnchant, success, destroy);
				}
			}
		}
		
		if (!getListeners().isEmpty())
		{
			for (final Listener<Creature> listener : getListeners())
			{
				if (OnEnchantItemListener.class.isInstance(listener))
				{
					((OnEnchantItemListener) listener).onEnchantItem(getActor(), item, oldEnchant, newEnchant, success, destroy);
				}
			}
		}
	}
	
	public void onRaidPoints(long points)
	{
		if (!_global.getListeners().isEmpty())
		{
			for (final Listener<Creature> listener : _global.getListeners())
			{
				if (OnRaidPointListener.class.isInstance(listener))
				{
					((OnRaidPointListener) listener).onRaidPoints(getActor(), points);
				}
			}
		}
		
		if (!getListeners().isEmpty())
		{
			for (final Listener<Creature> listener : getListeners())
			{
				if (OnRaidPointListener.class.isInstance(listener))
				{
					((OnRaidPointListener) listener).onRaidPoints(getActor(), points);
				}
			}
		}
	}
	
	public void onPvp(Player target)
	{
		if (!_global.getListeners().isEmpty())
		{
			for (final Listener<Creature> listener : _global.getListeners())
			{
				if (OnPvpListener.class.isInstance(listener))
				{
					((OnPvpListener) listener).onPvp(getActor(), target);
				}
			}
		}
		
		if (!getListeners().isEmpty())
		{
			for (final Listener<Creature> listener : getListeners())
			{
				if (OnPvpListener.class.isInstance(listener))
				{
					((OnPvpListener) listener).onPvp(getActor(), target);
				}
			}
		}
	}
	
	public void onEventFinish(int eventId, boolean isWin)
	{
		if (!_global.getListeners().isEmpty())
		{
			for (final Listener<Creature> listener : _global.getListeners())
			{
				if (OnEventFinishListener.class.isInstance(listener))
				{
					((OnEventFinishListener) listener).onEventFinish(getActor(), eventId, isWin);
				}
			}
		}
		
		if (!getListeners().isEmpty())
		{
			for (final Listener<Creature> listener : getListeners())
			{
				if (OnEventFinishListener.class.isInstance(listener))
				{
					((OnEventFinishListener) listener).onEventFinish(getActor(), eventId, isWin);
				}
			}
		}
	}
	
	public void onReflectionFinish(int refId)
	{
		if (!_global.getListeners().isEmpty())
		{
			for (final Listener<Creature> listener : _global.getListeners())
			{
				if (OnReflectionFinishListener.class.isInstance(listener))
				{
					((OnReflectionFinishListener) listener).onReflectionFinish(getActor(), refId);
				}
			}
		}
		
		if (!getListeners().isEmpty())
		{
			for (final Listener<Creature> listener : getListeners())
			{
				if (OnReflectionFinishListener.class.isInstance(listener))
				{
					((OnReflectionFinishListener) listener).onReflectionFinish(getActor(), refId);
				}
			}
		}
	}
}