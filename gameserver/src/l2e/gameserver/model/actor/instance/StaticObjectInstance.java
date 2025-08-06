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

import l2e.gameserver.ai.character.CharacterAI;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.stat.StaticObjStat;
import l2e.gameserver.model.actor.status.StaticObjStatus;
import l2e.gameserver.model.actor.templates.character.CharTemplate;
import l2e.gameserver.model.actor.templates.items.Weapon;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.network.serverpackets.ShowTownMap;
import l2e.gameserver.network.serverpackets.StaticObject;

public final class StaticObjectInstance extends Creature
{
	public static final int INTERACTION_DISTANCE = 150;

	private final int _staticObjectId;
	private int _meshIndex = 0;
	private int _type = -1;
	private ShowTownMap _map;

	@Override
	public CharacterAI initAI()
	{
		return null;
	}

	@Override
	public int getId()
	{
		return _staticObjectId;
	}

	public StaticObjectInstance(int objectId, CharTemplate template, int staticId)
	{
		super(objectId, template);
		setInstanceType(InstanceType.StaticObjectInstance);
		_staticObjectId = staticId;
	}
	
	@Override
	public final StaticObjStat getStat()
	{
		return (StaticObjStat) super.getStat();
	}

	@Override
	public void initCharStat()
	{
		setStat(new StaticObjStat(this));
	}

	@Override
	public final StaticObjStatus getStatus()
	{
		return (StaticObjStatus) super.getStatus();
	}

	@Override
	public void initCharStatus()
	{
		setStatus(new StaticObjStatus(this));
	}

	public int getType()
	{
		return _type;
	}

	public void setType(int type)
	{
		_type = type;
	}

	public void setMap(String texture, int x, int y)
	{
		_map = new ShowTownMap("town_map." + texture, x, y);
	}

	public ShowTownMap getMap()
	{
		return _map;
	}

	@Override
	public final int getLevel()
	{
		return 1;
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
	public boolean isAutoAttackable(Creature attacker, boolean isPoleAttack)
	{
		return false;
	}

	public void setMeshIndex(int meshIndex)
	{
		_meshIndex = meshIndex;
		this.broadcastPacket(new StaticObject(this));
	}

	public int getMeshIndex()
	{
		return _meshIndex;
	}

	@Override
	public void updateAbnormalEffect()
	{
	}

	@Override
	public void sendInfo(Player activeChar)
	{
		activeChar.sendPacket(new StaticObject(this));
	}

	@Override
	public void doAttack(Creature target)
	{
	}

	@Override
	public void doCast(Skill skill)
	{
	}
	
	@Override
	public void moveToLocation(int x, int y, int z, int offset)
	{
	}
	
	@Override
	public void stopMove(Location loc)
	{
	}
	
	@Override
	public int getGeoZ(int x, int y, int z)
	{
		return z;
	}
}