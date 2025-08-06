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
package l2e.gameserver.model.actor;

import l2e.gameserver.data.parser.CategoryParser;
import l2e.gameserver.model.CategoryType;
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.templates.character.CharTemplate;
import l2e.gameserver.model.actor.templates.items.Weapon;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.CharInfo;
import l2e.gameserver.network.serverpackets.GameServerPacket;

public abstract class Decoy extends Creature
{
	private final Player _owner;

	public Decoy(int objectId, CharTemplate template, Player owner)
	{
		super(objectId, template);
		setInstanceType(InstanceType.Decoy);
		_owner = owner;
		setXYZInvisible(owner.getX(), owner.getY(), owner.getZ());
		setIsInvul(false);
	}
	
	@Override
	public void onSpawn()
	{
		super.onSpawn();
		sendPacket(new CharInfo(this));
	}

	@Override
	public void updateAbnormalEffect()
	{
		for (final Player player : World.getInstance().getAroundPlayers(this))
		{
			if (player != null)
			{
				player.sendPacket(new CharInfo(this));
			}
		}
	}

	@Override
	public void onDecay()
	{
		deleteMe(_owner);
	}

	@Override
	public boolean isAutoAttackable(Creature attacker, boolean isPoleAttack)
	{
		return _owner.isAutoAttackable(attacker, isPoleAttack);
	}

	@Override
	public ItemInstance getActiveWeaponInstance()
	{
		return null;
	}

	@Override
	public Weapon getActiveWeaponItem()
	{
		return null;
	}

	@Override
	public ItemInstance getSecondaryWeaponInstance()
	{
		return null;
	}

	@Override
	public Weapon getSecondaryWeaponItem()
	{
		return null;
	}

	@Override
	public final int getId()
	{
		return getTemplate().getId();
	}

	@Override
	public int getLevel()
	{
		return getTemplate().getLevel();
	}

	public void deleteMe(Player owner)
	{
		decayMe();
		owner.setDecoy(null);
	}

	public void unSummon(Player owner)
	{
		if (isVisible() && !isDead())
		{
			owner.setDecoy(null);
			decayMe();
		}
	}

	public final Player getOwner()
	{
		return _owner;
	}

	@Override
	public Player getActingPlayer()
	{
		return _owner;
	}

	@Override
	public NpcTemplate getTemplate()
	{
		return (NpcTemplate) super.getTemplate();
	}

	@Override
	public void sendInfo(Player activeChar)
	{
		activeChar.sendPacket(new CharInfo(this));
	}

	@Override
	public void sendPacket(GameServerPacket mov)
	{
		if (getOwner() != null)
		{
			getOwner().sendPacket(mov);
		}
	}

	@Override
	public void sendPacket(SystemMessageId id)
	{
		if (getOwner() != null)
		{
			getOwner().sendPacket(id);
		}
	}

	@Override
	public boolean isInCategory(CategoryType type)
	{
		return CategoryParser.getInstance().isInCategory(type, getId());
	}
}