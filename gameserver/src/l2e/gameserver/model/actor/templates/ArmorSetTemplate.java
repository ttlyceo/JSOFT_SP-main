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
package l2e.gameserver.model.actor.templates;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.holders.SkillHolder;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.items.itemcontainer.Inventory;

public final class ArmorSetTemplate
{
	private int _chestId;
	private final List<Integer> _legs;
	private final List<Integer> _head;
	private final List<Integer> _gloves;
	private final List<Integer> _feet;
	private final List<Integer> _shield;

	private final List<SkillHolder> _skills;
	private final List<SkillHolder> _shieldSkills;
	private final List<SkillHolder> _enchant6Skill;
	private final Map<Integer, SkillHolder> _enchantByLevel;
	
	private int _con;
	private int _dex;
	private int _str;
	private int _men;
	private int _wit;
	private int _int;
	
	public ArmorSetTemplate()
	{
		_legs = new ArrayList<>();
		_head = new ArrayList<>();
		_gloves = new ArrayList<>();
		_feet = new ArrayList<>();
		_shield = new ArrayList<>();
		
		_skills = new ArrayList<>();
		_shieldSkills = new ArrayList<>();
		_enchant6Skill = new ArrayList<>();
		_enchantByLevel = new HashMap<>();
	}
	
	public ArmorSetTemplate(ArmorSetTemplate tpl)
	{
		_legs = tpl.getLegs();
		_head = tpl.getHeads();
		_gloves = tpl.getGloves();
		_feet = tpl.getFeets();
		_shield = tpl.getShields();
		
		_skills = tpl.getSkills();
		_shieldSkills = tpl.getShieldSkillId();
		_enchant6Skill = tpl.getEnchant6skillId();
		_enchantByLevel = tpl.getEnchantByLevel();
		_con = tpl.getCON();
		_dex = tpl.getDEX();
		_str = tpl.getSTR();
		_men = tpl.getMEN();
		_wit = tpl.getWIT();
		_int = tpl.getINT();
	}
	
	public void addChest(int id)
	{
		_chestId = id;
	}
	
	public void addLegs(int id)
	{
		_legs.add(id);
	}
	
	public List<Integer> getLegs()
	{
		return _legs;
	}
	
	public void addHead(int id)
	{
		_head.add(id);
	}
	
	public List<Integer> getHeads()
	{
		return _head;
	}
	
	public void addGloves(int id)
	{
		_gloves.add(id);
	}
	
	public List<Integer> getGloves()
	{
		return _gloves;
	}
	
	public void addFeet(int id)
	{
		_feet.add(id);
	}
	
	public List<Integer> getFeets()
	{
		return _feet;
	}
	
	public void addShield(int id)
	{
		_shield.add(id);
	}
	
	public List<Integer> getShields()
	{
		return _shield;
	}
	
	public void addSkill(SkillHolder holder)
	{
		_skills.add(holder);
	}
	
	public void addShieldSkill(SkillHolder holder)
	{
		_shieldSkills.add(holder);
	}
	
	public void addEnchant6Skill(SkillHolder holder)
	{
		_enchant6Skill.add(holder);
	}
	
	public void addCon(int val)
	{
		_con = val;
	}
	
	public void addDex(int val)
	{
		_dex = val;
	}
	
	public void addStr(int val)
	{
		_str = val;
	}
	
	public void addMen(int val)
	{
		_men = val;
	}
	
	public void addWit(int val)
	{
		_wit = val;
	}
	
	public void addInt(int val)
	{
		_int = val;
	}
	
	public boolean containAll(Player player)
	{
		final Inventory inv = player.getInventory();
		
		final ItemInstance legsItem = inv.getPaperdollItem(Inventory.PAPERDOLL_LEGS);
		final ItemInstance headItem = inv.getPaperdollItem(Inventory.PAPERDOLL_HEAD);
		final ItemInstance glovesItem = inv.getPaperdollItem(Inventory.PAPERDOLL_GLOVES);
		final ItemInstance feetItem = inv.getPaperdollItem(Inventory.PAPERDOLL_FEET);
		
		int legs = 0;
		int head = 0;
		int gloves = 0;
		int feet = 0;
		
		if (legsItem != null)
		{
			legs = legsItem.getId();
		}
		if (headItem != null)
		{
			head = headItem.getId();
		}
		if (glovesItem != null)
		{
			gloves = glovesItem.getId();
		}
		if (feetItem != null)
		{
			feet = feetItem.getId();
		}
		
		return containAll(_chestId, legs, head, gloves, feet);
	}
	
	public boolean containAll(int chest, int legs, int head, int gloves, int feet)
	{
		if ((_chestId != 0) && (_chestId != chest))
		{
			return false;
		}
		if (!_legs.isEmpty() && !_legs.contains(legs))
		{
			return false;
		}
		if (!_head.isEmpty() && !_head.contains(head))
		{
			return false;
		}
		if (!_gloves.isEmpty() && !_gloves.contains(gloves))
		{
			return false;
		}
		if (!_feet.isEmpty() && !_feet.contains(feet))
		{
			return false;
		}
		
		return true;
	}
	
	public boolean containItem(int slot, int itemId)
	{
		switch (slot)
		{
			case Inventory.PAPERDOLL_CHEST :
				return _chestId == itemId;
			case Inventory.PAPERDOLL_LEGS :
				return _legs.contains(itemId);
			case Inventory.PAPERDOLL_HEAD :
				return _head.contains(itemId);
			case Inventory.PAPERDOLL_GLOVES :
				return _gloves.contains(itemId);
			case Inventory.PAPERDOLL_FEET :
				return _feet.contains(itemId);
			default :
				return false;
		}
	}
	
	public int getChestId()
	{
		return _chestId;
	}
	
	public List<SkillHolder> getSkills()
	{
		return _skills;
	}
	
	public boolean containShield(Player player)
	{
		final Inventory inv = player.getInventory();
		
		final ItemInstance shieldItem = inv.getPaperdollItem(Inventory.PAPERDOLL_LHAND);
		return ((shieldItem != null) && _shield.contains(Integer.valueOf(shieldItem.getId())));
	}
	
	public boolean containShield(int shield_id)
	{
		if (_shield.isEmpty())
		{
			return false;
		}
		
		return _shield.contains(Integer.valueOf(shield_id));
	}
	
	public List<SkillHolder> getShieldSkillId()
	{
		return _shieldSkills;
	}
	
	public List<SkillHolder> getEnchant6skillId()
	{
		return _enchant6Skill;
	}
	
	public boolean isEnchanted6(Player player)
	{
		if (!containAll(player))
		{
			return false;
		}
		
		final Inventory inv = player.getInventory();
		
		final ItemInstance chestItem = inv.getPaperdollItem(Inventory.PAPERDOLL_CHEST);
		final ItemInstance legsItem = inv.getPaperdollItem(Inventory.PAPERDOLL_LEGS);
		final ItemInstance headItem = inv.getPaperdollItem(Inventory.PAPERDOLL_HEAD);
		final ItemInstance glovesItem = inv.getPaperdollItem(Inventory.PAPERDOLL_GLOVES);
		final ItemInstance feetItem = inv.getPaperdollItem(Inventory.PAPERDOLL_FEET);
		
		if ((chestItem == null) || (chestItem.getEnchantLevel() < 6))
		{
			return false;
		}
		if (!_legs.isEmpty() && ((legsItem == null) || (legsItem.getEnchantLevel() < 6)))
		{
			return false;
		}
		if (!_gloves.isEmpty() && ((glovesItem == null) || (glovesItem.getEnchantLevel() < 6)))
		{
			return false;
		}
		if (!_head.isEmpty() && ((headItem == null) || (headItem.getEnchantLevel() < 6)))
		{
			return false;
		}
		if (!_feet.isEmpty() && ((feetItem == null) || (feetItem.getEnchantLevel() < 6)))
		{
			return false;
		}
		
		return true;
	}
	
	public int getCON()
	{
		return _con;
	}
	
	public int getDEX()
	{
		return _dex;
	}
	
	public int getSTR()
	{
		return _str;
	}
	
	public int getMEN()
	{
		return _men;
	}
	
	public int getWIT()
	{
		return _wit;
	}
	
	public int getINT()
	{
		return _int;
	}
	
	public void addEnchantByLevel(int enchLvl, SkillHolder holder)
	{
		_enchantByLevel.put(enchLvl, holder);
	}
	
	public Map<Integer, SkillHolder> getEnchantByLevel()
	{
		return _enchantByLevel;
	}
	
	public boolean isEnchantedByLevel(Player player, int enchLvl)
	{
		if (!containAll(player))
		{
			return false;
		}
		
		final Inventory inv = player.getInventory();
		
		final ItemInstance chestItem = inv.getPaperdollItem(Inventory.PAPERDOLL_CHEST);
		final ItemInstance legsItem = inv.getPaperdollItem(Inventory.PAPERDOLL_LEGS);
		final ItemInstance headItem = inv.getPaperdollItem(Inventory.PAPERDOLL_HEAD);
		final ItemInstance glovesItem = inv.getPaperdollItem(Inventory.PAPERDOLL_GLOVES);
		final ItemInstance feetItem = inv.getPaperdollItem(Inventory.PAPERDOLL_FEET);
		
		if ((chestItem == null) || (chestItem.getEnchantLevel() < enchLvl))
		{
			return false;
		}
		if (!_legs.isEmpty() && ((legsItem == null) || (legsItem.getEnchantLevel() < enchLvl)))
		{
			return false;
		}
		if (!_gloves.isEmpty() && ((glovesItem == null) || (glovesItem.getEnchantLevel() < enchLvl)))
		{
			return false;
		}
		if (!_head.isEmpty() && ((headItem == null) || (headItem.getEnchantLevel() < enchLvl)))
		{
			return false;
		}
		if (!_feet.isEmpty() && ((feetItem == null) || (feetItem.getEnchantLevel() < enchLvl)))
		{
			return false;
		}
		return true;
	}
}