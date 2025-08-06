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

import l2e.gameserver.instancemanager.MapRegionManager;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.TeleportWhereType;
import l2e.gameserver.model.holders.SummonRequestHolder;
import l2e.gameserver.model.skills.effects.Effect;
import l2e.gameserver.model.skills.effects.EffectTemplate;
import l2e.gameserver.model.skills.effects.EffectType;
import l2e.gameserver.model.stats.Env;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.ConfirmDlg;

public class Escape extends Effect
{
	private final TeleportWhereType _escapeType;

	public Escape(Env env, EffectTemplate template)
	{
		super(env, template);
		
		_escapeType = template.getParameters().getEnum("escapeType", TeleportWhereType.class, null);
	}

	@Override
	public EffectType getEffectType()
	{
		return EffectType.TELEPORT;
	}
	
	@Override
	public boolean calcSuccess()
	{
		return true;
	}

	@Override
	public boolean onStart()
	{
		if ((_escapeType == null && getSkill().getId() != 2588) || (getSkill().getId() == 2588 && getEffected().isPlayer() && getEffected().getActingPlayer().getBookmarkLocation() == null))
		{
			return false;
		}
		
		if (getSkill().getId() == 1255 && getEffected().isPlayer() && getEffected().getActingPlayer().getBlockPartyRecall())
		{
			getEffected().addScript(new SummonRequestHolder(getEffector().getActingPlayer(), getSkill(), false));
			final ConfirmDlg confirm = new ConfirmDlg(SystemMessageId.C1_WISHES_TO_SUMMON_YOU_FROM_S2_DO_YOU_ACCEPT.getId());
			confirm.addCharName(getEffector());
			final Location loc = MapRegionManager.getInstance().getTeleToLocation(getEffected(), _escapeType);
			confirm.addZoneName(loc.getX(), loc.getY(), loc.getZ());
			confirm.addTime(30000);
			confirm.addRequesterId(getEffector().getObjectId());
			getEffected().sendPacket(confirm);
		}
		else
		{
			if (getEffected().isPlayer() && getEffected().getActingPlayer().isInKrateisCube())
			{
				getEffected().getActingPlayer().getArena().removePlayer(getEffected().getActingPlayer());
			}
			
			getEffected().getActingPlayer().setIsIn7sDungeon(false);
			
			if (getSkill().getId() == 2588 && getEffected().isPlayer())
			{
				getEffected().teleToLocation(getEffected().getActingPlayer().getBookmarkLocation(), false, ReflectionManager.DEFAULT);
				getEffected().getActingPlayer().setBookmarkLocation(null);
			}
			else
			{
				getEffected().teleToLocation(MapRegionManager.getInstance().getTeleToLocation(getEffected(), _escapeType), true, ReflectionManager.DEFAULT);
			}
		}
		return true;
	}
}