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

import l2e.gameserver.data.parser.AdminParser;
import l2e.gameserver.model.stats.StatsSet;

public class AccessLevel
{
	AccessLevel _childsAccessLevel = null;
	private int _child = 0;
	private int _nameColor = 0;
	private int _titleColor = 0;
	private final StatsSet _set;
	
	public AccessLevel(StatsSet set)
	{
		_nameColor = Integer.decode("0x" + set.getString("nameColor", "FFFFFF"));
		_titleColor = Integer.decode("0x" + set.getString("titleColor", "FFFFFF"));
		_child = set.getInteger("childAccess", 0);
		_set = set;
	}
	
	public AccessLevel()
	{
		_nameColor = Integer.decode("0xFFFFFF");
		_titleColor = Integer.decode("0xFFFFFF");
		_child = 0;
		_set = new StatsSet();
	}
	
	public int getLevel()
	{
		return _set.getInteger("level", 0);
	}
	
	public String getName()
	{
		return _set.getString("name", "User");
	}
	
	public int getNameColor()
	{
		return _nameColor;
	}
	
	public int getTitleColor()
	{
		return _titleColor;
	}
	
	public boolean isGm()
	{
		return _set.getBool("isGM", false);
	}
	
	public boolean allowPeaceAttack()
	{
		return _set.getBool("allowPeaceAttack", false);
	}
	
	public boolean allowFixedRes()
	{
		return _set.getBool("allowFixedRes", false);
	}
	
	public boolean allowTransaction()
	{
		return _set.getBool("allowTransaction", false);
	}
	
	public boolean allowAltG()
	{
		return _set.getBool("allowAltG", false);
	}
	
	public boolean canGiveDamage()
	{
		return _set.getBool("giveDamage", false);
	}
	
	public boolean canTakeAggro()
	{
		return _set.getBool("takeAggro", false);
	}
	
	public boolean canGainExp()
	{
		return _set.getBool("gainExp", true);
	}
	
	public boolean isGmSpeedAccess()
	{
		return _set.getBool("gmSpeedAccess", false);
	}
	
	public boolean allowDoorActionShift()
	{
		return _set.getBool("allowDoorActionShift", false);
	}
	
	public boolean allowItemActionShift()
	{
		return _set.getBool("allowItemActionShift", false);
	}
	
	public boolean allowNpcActionShift()
	{
		return _set.getBool("allowNpcActionShift", false);
	}
	
	public boolean allowPlayerActionShift()
	{
		return _set.getBool("allowPlayerActionShift", false);
	}
	
	public boolean allowStaticObjectActionShift()
	{
		return _set.getBool("allowStaticObjectActionShift", false);
	}
	
	public boolean allowSummonActionShift()
	{
		return _set.getBool("allowSummonActionShift", false);
	}
	
	public boolean allowBalancer()
	{
		return _set.getBool("allowBalancer", false);
	}
	
	public boolean allowPunishment()
	{
		return _set.getBool("allowPunishment", false);
	}
	
	public boolean hasChildAccess(AccessLevel accessLevel)
	{
		if (_childsAccessLevel == null)
		{
			if (_child <= 0)
			{
				return false;
			}
			_childsAccessLevel = AdminParser.getInstance().getAccessLevel(_child);
		}
		return ((_childsAccessLevel.getLevel() == accessLevel.getLevel()) || _childsAccessLevel.hasChildAccess(accessLevel));
	}
	
	public boolean allowPunishmentChat()
	{
		return _set.getBool("allowPunishmentChat", false);
	}
	
	public boolean allowHeroAura()
	{
		return _set.getBool("allowHeroAura", false);
	}
	
	public boolean allowStartupInvulnerable()
	{
		return _set.getBool("allowStartupInvulnerable", false);
	}
	
	public boolean allowStartupInvisible()
	{
		return _set.getBool("allowStartupInvisible", false);
	}
	
	public boolean allowStartupSilence()
	{
		return _set.getBool("allowStartupSilence", false);
	}
	
	public boolean allowStartupAutoList()
	{
		return _set.getBool("allowStartupAutoList", false);
	}
	
	public boolean allowStartupDietMode()
	{
		return _set.getBool("allowStartupDietMode", false);
	}
	
	public boolean allowItemRestriction()
	{
		return _set.getBool("allowItemRestriction", false);
	}
	
	public boolean allowSkillRestriction()
	{
		return _set.getBool("allowSkillRestriction", false);
	}
	
	public boolean allowTradeRestrictedItems()
	{
		return _set.getBool("allowTradeRestrictedItems", false);
	}
	
	public boolean allowRestartFighting()
	{
		return _set.getBool("allowRestartFighting", false);
	}
	
	public boolean allowShowAnnouncerName()
	{
		return _set.getBool("allowShowAnnouncerName", false);
	}
	
	public boolean allowShowCritAnnouncerName()
	{
		return _set.getBool("allowShowCritAnnouncerName", false);
	}
	
	public boolean allowGiveSpecialSkills()
	{
		return _set.getBool("allowGiveSpecialSkills", false);
	}
	
	public boolean allowGiveSpecialAuraSkills()
	{
		return _set.getBool("allowGiveSpecialAuraSkills", false);
	}
	
	public boolean allowEnterAnnounce()
	{
		return _set.getBool("allowEnterAnnounce", false);
	}
}