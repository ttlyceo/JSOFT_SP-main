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

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import l2e.commons.util.Rnd;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.ai.npc.Minions;
import l2e.gameserver.data.parser.SpawnParser;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.ControllableMobInstance;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.spawn.SpawnGroup;

public final class MobGroup
{
	private final NpcTemplate _npcTemplate;
	private final int _groupId;
	private final int _maxMobCount;
	
	private List<ControllableMobInstance> _mobs;
	
	public MobGroup(int groupId, NpcTemplate npcTemplate, int maxMobCount)
	{
		_groupId = groupId;
		_npcTemplate = npcTemplate;
		_maxMobCount = maxMobCount;
	}

	public int getActiveMobCount()
	{
		return getMobs().size();
	}

	public int getGroupId()
	{
		return _groupId;
	}
	
	public int getMaxMobCount()
	{
		return _maxMobCount;
	}

	public List<ControllableMobInstance> getMobs()
	{
		if (_mobs == null)
		{
			_mobs = new CopyOnWriteArrayList<>();
		}
		return _mobs;
	}

	public String getStatus()
	{
		try
		{
			final Minions mobGroupAI = (Minions) getMobs().get(0).getAI();

			switch (mobGroupAI.getAlternateAI())
			{
				case Minions.AI_NORMAL :
					return "Idle";
				case Minions.AI_FORCEATTACK :
					return "Force Attacking";
				case Minions.AI_FOLLOW :
					return "Following";
				case Minions.AI_CAST :
					return "Casting";
				case Minions.AI_ATTACK_GROUP :
					return "Attacking Group";
				default :
					return "Idle";
			}
		}
		catch (final Exception e)
		{
			return "Unspawned";
		}
	}

	public NpcTemplate getTemplate()
	{
		return _npcTemplate;
	}

	public boolean isGroupMember(ControllableMobInstance mobInst)
	{
		for (final ControllableMobInstance groupMember : getMobs())
		{
			if (groupMember == null)
			{
				continue;
			}

			if (groupMember.getObjectId() == mobInst.getObjectId())
			{
				return true;
			}
		}

		return false;
	}

	public void spawnGroup(int x, int y, int z)
	{
		if (getActiveMobCount() > 0)
		{
			return;
		}

		try
		{
			for (int i = 0; i < getMaxMobCount(); i++)
			{
				final SpawnGroup spawn = new SpawnGroup(getTemplate());

				final int signX = (Rnd.nextInt(2) == 0) ? -1 : 1;
				final int signY = (Rnd.nextInt(2) == 0) ? -1 : 1;
				final int randX = Rnd.nextInt(MobGroupData.RANDOM_RANGE);
				final int randY = Rnd.nextInt(MobGroupData.RANDOM_RANGE);

				spawn.setX(x + (signX * randX));
				spawn.setY(y + (signY * randY));
				spawn.setZ(z);
				spawn.stopRespawn();

				SpawnParser.getInstance().addNewSpawn(spawn);
				getMobs().add((ControllableMobInstance) spawn.doGroupSpawn());
			}
		}
		catch (final ClassNotFoundException e)
		{}
		catch (final NoSuchMethodException e2)
		{}
	}

	public void spawnGroup(Player activeChar)
	{
		spawnGroup(activeChar.getX(), activeChar.getY(), activeChar.getZ());
	}

	public void teleportGroup(Player player)
	{
		removeDead();

		for (final ControllableMobInstance mobInst : getMobs())
		{
			if (mobInst == null)
			{
				continue;
			}

			if (!mobInst.isDead())
			{
				final int x = player.getX() + Rnd.nextInt(50);
				final int y = player.getY() + Rnd.nextInt(50);

				mobInst.teleToLocation(x, y, player.getZ(), true, player.getReflection());
				final Minions ai = (Minions) mobInst.getAI();
				ai.follow(player);
			}
		}
	}

	public ControllableMobInstance getRandomMob()
	{
		removeDead();

		if (getActiveMobCount() == 0)
		{
			return null;
		}

		final int choice = Rnd.nextInt(getActiveMobCount());
		return getMobs().get(choice);
	}

	public void unspawnGroup()
	{
		removeDead();

		if (getActiveMobCount() == 0)
		{
			return;
		}

		for (final ControllableMobInstance mobInst : getMobs())
		{
			if (mobInst == null)
			{
				continue;
			}

			if (!mobInst.isDead())
			{
				mobInst.deleteMe();
			}

			SpawnParser.getInstance().deleteSpawn(mobInst.getSpawn());
		}

		getMobs().clear();
	}

	public void killGroup(Player activeChar)
	{
		removeDead();

		for (final ControllableMobInstance mobInst : getMobs())
		{
			if (mobInst == null)
			{
				continue;
			}

			if (!mobInst.isDead())
			{
				mobInst.reduceCurrentHp(mobInst.getMaxHp() + 1, activeChar, null);
			}

			SpawnParser.getInstance().deleteSpawn(mobInst.getSpawn());
		}
		getMobs().clear();
	}

	public void setAttackRandom()
	{
		removeDead();

		for (final ControllableMobInstance mobInst : getMobs())
		{
			if (mobInst == null)
			{
				continue;
			}

			final Minions ai = (Minions) mobInst.getAI();
			ai.setAlternateAI(Minions.AI_NORMAL);
			ai.setIntention(CtrlIntention.ACTIVE);
		}
	}

	public void setAttackTarget(Creature target)
	{
		removeDead();

		for (final ControllableMobInstance mobInst : getMobs())
		{
			if (mobInst == null)
			{
				continue;
			}

			final Minions ai = (Minions) mobInst.getAI();
			ai.forceAttack(target);
		}
	}

	public void setIdleMode()
	{
		removeDead();

		for (final ControllableMobInstance mobInst : getMobs())
		{
			if (mobInst == null)
			{
				continue;
			}

			final Minions ai = (Minions) mobInst.getAI();
			ai.stop();
		}
	}

	public void returnGroup(Creature activeChar)
	{
		setIdleMode();

		for (final ControllableMobInstance mobInst : getMobs())
		{
			if (mobInst == null)
			{
				continue;
			}

			final int signX = (Rnd.nextInt(2) == 0) ? -1 : 1;
			final int signY = (Rnd.nextInt(2) == 0) ? -1 : 1;
			final int randX = Rnd.nextInt(MobGroupData.RANDOM_RANGE);
			final int randY = Rnd.nextInt(MobGroupData.RANDOM_RANGE);

			final Minions ai = (Minions) mobInst.getAI();
			ai.move(activeChar.getX() + (signX * randX), activeChar.getY() + (signY * randY), activeChar.getZ());
		}
	}

	public void setFollowMode(Creature character)
	{
		removeDead();

		for (final ControllableMobInstance mobInst : getMobs())
		{
			if (mobInst == null)
			{
				continue;
			}

			final Minions ai = (Minions) mobInst.getAI();
			ai.follow(character);
		}
	}

	public void setCastMode()
	{
		removeDead();

		for (final ControllableMobInstance mobInst : getMobs())
		{
			if (mobInst == null)
			{
				continue;
			}

			final Minions ai = (Minions) mobInst.getAI();
			ai.setAlternateAI(Minions.AI_CAST);
		}
	}

	public void setNoMoveMode(boolean enabled)
	{
		removeDead();

		for (final ControllableMobInstance mobInst : getMobs())
		{
			if (mobInst == null)
			{
				continue;
			}

			final Minions ai = (Minions) mobInst.getAI();
			ai.setNotMoving(enabled);
		}
	}

	protected void removeDead()
	{
		final List<ControllableMobInstance> deadMobs = new LinkedList<>();

		for (final ControllableMobInstance mobInst : getMobs())
		{
			if ((mobInst != null) && mobInst.isDead())
			{
				deadMobs.add(mobInst);
			}
		}
		getMobs().removeAll(deadMobs);
	}

	public void setInvul(boolean invulState)
	{
		removeDead();

		for (final ControllableMobInstance mobInst : getMobs())
		{
			if (mobInst != null)
			{
				mobInst.setInvul(invulState);
			}
		}
	}

	public void setAttackGroup(MobGroup otherGrp)
	{
		removeDead();

		for (final ControllableMobInstance mobInst : getMobs())
		{
			if (mobInst == null)
			{
				continue;
			}

			final Minions ai = (Minions) mobInst.getAI();
			ai.forceAttackGroup(otherGrp);
			ai.setIntention(CtrlIntention.ACTIVE);
		}
	}
}